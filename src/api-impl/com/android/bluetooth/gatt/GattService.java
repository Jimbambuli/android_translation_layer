package com.android.bluetooth.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothGattCallback;
import android.bluetooth.IBluetoothGattServerCallback;
import android.content.AttributionSource;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import com.android.bluetooth.bluez.Bluez;
import com.android.bluetooth.bluez.BluezBus;
import com.android.bluetooth.btservice.AdapterService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/* ATL: the GATT client of AOSP's gatt/GattService (android/app, android-16.0.0_r4) as the IBluetoothGatt of the
 * app's process, on top of BlueZ's GATT client (Device1.Connect, GattService1/GattCharacteristic1/GattDescriptor1).
 *
 * - Handles: BlueZ names its attribute objects after the attribute handle (service0010/char0011/desc0013); that
 *   number is the instance id the app sees.
 * - BlueZ discovers the services itself after connecting (ServicesResolved); discoverServices() answers once they are
 *   resolved.
 * - The client characteristic configuration descriptor (0x2902) cannot be written over BlueZ's D-Bus API: a write
 *   of it is StartNotify/StopNotify of its characteristic, the call BlueZ writes the descriptor with.
 * - Notifications reach the app for characteristics it registered (setCharacteristicNotification), as AOSP's
 *   BTA_GATTC_RegisterForNotifications.
 * - The link: a connection this client made is closed on disconnect; a link the desktop already had stays.
 * - Not in BlueZ's D-Bus API, and so answered without the radio: PHY (read/set report LE 1M with
 *   GATT_REQUEST_NOT_SUPPORTED), connection priority/parameters (accepted, no onConnectionUpdated), the RSSI of a
 *   connection (the last advertisement's RSSI while discovery runs, otherwise GATT_FAILURE). The MTU is the one BlueZ
 *   exchanged when it connected (GattCharacteristic1.MTU).
 * - GATT server: not provided (registerServer reports failure). */
public final class GattService implements IBluetoothGatt, Bluez.Observer {
	private static final String TAG = "BtGattService";

	/* stack/include/gatt_api.h GATT_ERROR (0x85, 133): what apps see for failed connections */
	private static final int GATT_ERROR = 0x85;
	/* hci_error_code.h: HCI_ERR_CONNECTION_TOUT, HCI_ERR_PEER_USER, HCI_ERR_CONN_CAUSE_LOCAL_HOST */
	private static final int CONN_TIMEOUT = 0x08, CONN_TERMINATE_PEER_USER = 0x13, CONN_TERMINATE_LOCAL_HOST = 0x16;
	private static final long DIRECT_CONNECT_TIMEOUT_MS = 30000; /* AOSP: direct connections time out after 30 s */
	private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	private static GattService instance;

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final AdapterService adapterService;
	private final Bluez bluez;

	private final class Client {
		final IBluetoothGattCallback callback;
		String address;
		boolean wantConnected;   /* clientConnect() until clientDisconnect() */
		boolean direct;
		boolean connected;       /* reported to the app */
		boolean weConnected;     /* the link is ours to close */
		boolean searchPending;   /* discoverServices() waits for ServicesResolved */
		boolean reliable;
		final HashSet<Integer> notifications = new HashSet<>();
		Runnable timeout;

		Client(IBluetoothGattCallback callback) {
			this.callback = callback;
		}
	}

	private final ArrayList<Client> clients = new ArrayList<>();

	private GattService() {
		adapterService = AdapterService.get();
		bluez = adapterService.bluez();
		bluez.addObserver(this);
	}

	public static synchronized GattService get() {
		if (instance == null)
			instance = new GattService();
		return instance;
	}

	private Client client(IBluetoothGattCallback callback) {
		synchronized (clients) {
			for (Client c : clients)
				if (c.callback == callback)
					return c;
		}
		return null;
	}

	private Client[] clientsFor(String address) {
		ArrayList<Client> list = new ArrayList<>();
		synchronized (clients) {
			for (Client c : clients)
				if (address.equalsIgnoreCase(c.address))
					list.add(c);
		}
		return list.toArray(new Client[0]);
	}

	private void post(Runnable r) {
		handler.post(r);
	}

	private interface Call {
		void run() throws RemoteException;
	}

	/* the callbacks of IBluetoothGattCallback are oneway: they never run inside the app's call */
	private void callback(final Call call) {
		post(new Runnable() {
			public void run() {
				try {
					call.run();
				} catch (RemoteException e) {
				}
			}
		});
	}

	private String devicePath(BluetoothDevice device) {
		return adapterService.devicePath(device.getAddress());
	}

	/* ---- client registration and connection ---- */

	@Override
	public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "getDevicesMatchingConnectionStates");
		ArrayList<BluetoothDevice> list = new ArrayList<>();
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null)
			return list;
		for (String address : adapterService.deviceAddresses()) {
			int type = adapterService.getRemoteType(address);
			if (type == BluetoothDevice.DEVICE_TYPE_CLASSIC)
				continue;
			int state = adapterService.isConnected(address) ? BluetoothProfile.STATE_CONNECTED : BluetoothProfile.STATE_DISCONNECTED;
			for (int s : states)
				if (s == state) {
					list.add(adapter.getRemoteDevice(address));
					break;
				}
		}
		return list;
	}

	@Override
	public void registerClient(ParcelUuid appId, final IBluetoothGattCallback callback, boolean eattSupport, int transport, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "registerClient");
		synchronized (clients) {
			if (client(callback) == null)
				clients.add(new Client(callback));
		}
		callback(new Call() {
			public void run() throws RemoteException {
				callback.onClientRegistered(BluetoothGatt.GATT_SUCCESS);
			}
		});
	}

	@Override
	public void unregisterClient(final IBluetoothGattCallback callback, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "unregisterClient");
		post(new Runnable() {
			public void run() {
				Client c = client(callback);
				if (c == null)
					return;
				dropLink(c);
				synchronized (clients) {
					clients.remove(c);
				}
			}
		});
	}

	@Override
	public void clientConnect(final IBluetoothGattCallback callback, final BluetoothDevice device, int addressType, final boolean isDirect,
	                          int transport, boolean opportunistic, int phy, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "clientConnect");
		post(new Runnable() {
			public void run() {
				final Client c = client(callback);
				if (c == null)
					return;
				c.address = device.getAddress();
				c.wantConnected = true;
				c.direct = isDirect;
				if (c.timeout != null)
					handler.removeCallbacks(c.timeout);
				if (isDirect) {
					c.timeout = new Runnable() {
						public void run() {
							if (c.wantConnected && !c.connected) {
								c.wantConnected = false;
								c.timeout = null;
								stopSearching(c);
								reportConnection(c, GATT_ERROR, false);
							}
						}
					};
					handler.postDelayed(c.timeout, DIRECT_CONNECT_TIMEOUT_MS);
				}
				tryConnect(c);
			}
		});
	}

	private final HashSet<Client> searching = new HashSet<>();

	/* a device BlueZ does not know yet has no object: discover (LE) until it advertises */
	private void startSearching(Client c) {
		if (searching.add(c))
			updateSearch();
	}

	private void stopSearching(Client c) {
		if (searching.remove(c))
			updateSearch();
	}

	/* the LE discovery of AdapterService: a search for a connection counts as one scan */
	private void updateSearch() {
		adapterService.setLeScanning("gatt", searching.isEmpty() ? 0 : 1, true);
	}

	private void tryConnect(final Client c) {
		if (!c.wantConnected || c.connected)
			return;
		final String path = adapterService.devicePath(c.address);
		if (path == null)
			return;
		if (!bluez.has(path, Bluez.DEVICE)) {
			startSearching(c);
			return;
		}
		stopSearching(c);
		if (bluez.booleanProperty(path, Bluez.DEVICE, "Connected")) {
			/* the link is up (the desktop's, or another client's): the GATT client is connected */
			onLinkUp(c, path);
			return;
		}
		c.weConnected = true;
		BluezBus.call(path, Bluez.DEVICE, "Connect", null, null, new BluezBus.Reply() {
			public void onReply(Object[] result, String error, String message) {
				if (error == null) {
					if (bluez.booleanProperty(path, Bluez.DEVICE, "Connected"))
						onLinkUp(c, path);
					return;
				}
				Log.i(TAG, "Connect " + c.address + ": " + error + " " + message);
				if (!c.wantConnected || c.connected)
					return;
				if ("AlreadyConnected".equals(Bluez.shortError(error))) {
					onLinkUp(c, path);
				} else if (c.direct) {
					c.wantConnected = false;
					c.weConnected = false;
					if (c.timeout != null)
						handler.removeCallbacks(c.timeout);
					c.timeout = null;
					reportConnection(c, GATT_ERROR, false);
				} else {
					/* background connection: try again */
					handler.postDelayed(new Runnable() {
						public void run() {
							tryConnect(c);
						}
					}, 2000);
				}
			}
		});
	}

	private void onLinkUp(Client c, String path) {
		if (!c.wantConnected || c.connected)
			return;
		if (c.timeout != null)
			handler.removeCallbacks(c.timeout);
		c.timeout = null;
		reportConnection(c, BluetoothGatt.GATT_SUCCESS, true);
		if (c.searchPending && bluez.booleanProperty(path, Bluez.DEVICE, "ServicesResolved"))
			searchComplete(c, path);
	}

	private void reportConnection(final Client c, final int status, final boolean connected) {
		c.connected = connected;
		if (!connected) {
			c.searchPending = false;
			c.notifications.clear();
		}
		final BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(c.address);
		try {
			c.callback.onClientConnectionState(status, connected, device);
		} catch (RemoteException e) {
		}
	}

	private void dropLink(Client c) {
		c.wantConnected = false;
		stopSearching(c);
		if (c.timeout != null)
			handler.removeCallbacks(c.timeout);
		c.timeout = null;
		if (c.address == null)
			return;
		boolean others = false;
		for (Client o : clientsFor(c.address))
			if (o != c && o.wantConnected)
				others = true;
		String path = adapterService.devicePath(c.address);
		if (c.weConnected && !others && path != null && bluez.has(path, Bluez.DEVICE))
			BluezBus.call(path, Bluez.DEVICE, "Disconnect", null, null, null);
		c.weConnected = false;
	}

	@Override
	public void clientDisconnect(final IBluetoothGattCallback callback, BluetoothDevice device, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "clientDisconnect");
		post(new Runnable() {
			public void run() {
				Client c = client(callback);
				if (c == null)
					return;
				boolean was = c.connected;
				dropLink(c);
				/* AOSP: the app's disconnect ends with onConnectionStateChange(GATT_SUCCESS, DISCONNECTED) */
				if (was)
					reportConnection(c, BluetoothGatt.GATT_SUCCESS, false);
			}
		});
	}

	@Override
	public void clientSetPreferredPhy(final IBluetoothGattCallback callback, final BluetoothDevice device, int txPhy, int rxPhy, int phyOptions, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "clientSetPreferredPhy");
		callback(new Call() {
			public void run() throws RemoteException {
				callback.onPhyUpdate(device, BluetoothDevice.PHY_LE_1M, BluetoothDevice.PHY_LE_1M, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED);
			}
		});
	}

	@Override
	public void clientReadPhy(final IBluetoothGattCallback callback, final BluetoothDevice device, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "clientReadPhy");
		callback(new Call() {
			public void run() throws RemoteException {
				callback.onPhyRead(device, BluetoothDevice.PHY_LE_1M, BluetoothDevice.PHY_LE_1M, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED);
			}
		});
	}

	@Override
	public void refreshDevice(IBluetoothGattCallback callback, BluetoothDevice device, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "refreshDevice");
	}

	/* ---- services ---- */

	@Override
	public void discoverServices(final IBluetoothGattCallback callback, final BluetoothDevice device, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "discoverServices");
		post(new Runnable() {
			public void run() {
				Client c = client(callback);
				if (c == null || !c.connected)
					return;
				String path = devicePath(device);
				c.searchPending = true;
				if (bluez.booleanProperty(path, Bluez.DEVICE, "ServicesResolved"))
					searchComplete(c, path);
			}
		});
	}

	@Override
	public void discoverServiceByUuid(IBluetoothGattCallback callback, BluetoothDevice device, ParcelUuid uuid, AttributionSource source) {
		discoverServices(callback, device, source);
	}

	/* service0010, char0011, desc0013: the handle in hex */
	private static int handleOf(String path) {
		try {
			return Integer.parseInt(path.substring(path.length() - 4), 16);
		} catch (RuntimeException e) {
			return -1;
		}
	}

	private static int properties(Object[] flags) {
		int p = 0;
		if (flags == null)
			return p;
		for (Object o : flags) {
			switch ((String)o) {
				case "broadcast": p |= BluetoothGattCharacteristic.PROPERTY_BROADCAST; break;
				case "read": p |= BluetoothGattCharacteristic.PROPERTY_READ; break;
				case "write-without-response": p |= BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE; break;
				case "write": p |= BluetoothGattCharacteristic.PROPERTY_WRITE; break;
				case "notify": p |= BluetoothGattCharacteristic.PROPERTY_NOTIFY; break;
				case "indicate": p |= BluetoothGattCharacteristic.PROPERTY_INDICATE; break;
				case "authenticated-signed-writes": p |= BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE; break;
				case "extended-properties": p |= BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS; break;
				default: break;
			}
		}
		return p;
	}

	/* the attribute database of the device as AOSP's onSearchComplete() gets it */
	private List<BluetoothGattService> database(String devicePath) {
		ArrayList<BluetoothGattService> services = new ArrayList<>();
		HashMap<String, BluetoothGattService> byPath = new HashMap<>();
		for (String sp : bluez.paths(Bluez.GATT_SERVICE, devicePath)) {
			Map<String, Object> p = bluez.properties(sp, Bluez.GATT_SERVICE);
			if (p == null || p.get("UUID") == null)
				continue;
			BluetoothGattService s = new BluetoothGattService(UUID.fromString((String)p.get("UUID")), handleOf(sp),
			                                                  Boolean.FALSE.equals(p.get("Primary")) ? BluetoothGattService.SERVICE_TYPE_SECONDARY
			                                                                                         : BluetoothGattService.SERVICE_TYPE_PRIMARY);
			services.add(s);
			byPath.put(sp, s);
		}
		for (String sp : byPath.keySet()) {
			Object[] includes = (Object[])bluez.property(sp, Bluez.GATT_SERVICE, "Includes");
			if (includes != null)
				for (Object inc : includes) {
					BluetoothGattService included = byPath.get((String)inc);
					if (included != null)
						byPath.get(sp).addIncludedService(included);
				}
		}
		HashMap<String, BluetoothGattCharacteristic> chars = new HashMap<>();
		for (String cp : bluez.paths(Bluez.GATT_CHARACTERISTIC, devicePath)) {
			Map<String, Object> p = bluez.properties(cp, Bluez.GATT_CHARACTERISTIC);
			BluetoothGattService s = p == null ? null : byPath.get((String)p.get("Service"));
			if (s == null || p.get("UUID") == null)
				continue;
			BluetoothGattCharacteristic c = new BluetoothGattCharacteristic(UUID.fromString((String)p.get("UUID")), handleOf(cp),
			                                                                properties((Object[])p.get("Flags")), 0);
			s.addCharacteristic(c);
			chars.put(cp, c);
		}
		for (String dp : bluez.paths(Bluez.GATT_DESCRIPTOR, devicePath)) {
			Map<String, Object> p = bluez.properties(dp, Bluez.GATT_DESCRIPTOR);
			BluetoothGattCharacteristic c = p == null ? null : chars.get((String)p.get("Characteristic"));
			if (c == null || p.get("UUID") == null)
				continue;
			c.addDescriptor(new BluetoothGattDescriptor(UUID.fromString((String)p.get("UUID")), handleOf(dp), 0));
		}
		java.util.Collections.sort(services, new java.util.Comparator<BluetoothGattService>() {
			public int compare(BluetoothGattService a, BluetoothGattService b) {
				return Integer.compare(a.getInstanceId(), b.getInstanceId());
			}
		});
		return services;
	}

	private void searchComplete(Client c, String devicePath) {
		c.searchPending = false;
		BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(c.address);
		try {
			c.callback.onSearchComplete(device, database(devicePath), BluetoothGatt.GATT_SUCCESS);
		} catch (RemoteException e) {
		}
	}

	/* object path of the attribute with this handle under the device, iface GattCharacteristic1/GattDescriptor1 */
	private String attributePath(BluetoothDevice device, String iface, int handle) {
		String dp = devicePath(device);
		if (dp == null)
			return null;
		for (String p : bluez.paths(iface, dp))
			if (handleOf(p) == handle)
				return p;
		return null;
	}

	/* BlueZ's D-Bus errors of an ATT request (src/gatt-client.c create_gatt_dbus_error) as ATT/GATT status */
	private static int status(String error, String message, boolean write) {
		if (error == null)
			return BluetoothGatt.GATT_SUCCESS;
		if (message != null) {
			int i = message.indexOf("ATT error: 0x");
			if (i >= 0) {
				try {
					return Integer.parseInt(message.substring(i + 13, Math.min(message.length(), i + 15)), 16);
				} catch (NumberFormatException e) {
				}
			}
		}
		switch (Bluez.shortError(error)) {
			case "NotPermitted":
				return write ? BluetoothGatt.GATT_WRITE_NOT_PERMITTED : BluetoothGatt.GATT_READ_NOT_PERMITTED;
			case "NotAuthorized":
				return BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION;
			case "InvalidOffset":
				return BluetoothGatt.GATT_INVALID_OFFSET;
			case "InvalidValueLength":
				return BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
			case "NotSupported":
				return BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
			default:
				return GATT_ERROR;
		}
	}

	private static final Object[] NO_OPTIONS = new Object[0];

	/* ---- reads and writes ---- */

	@Override
	public void readCharacteristic(final IBluetoothGattCallback callback, final BluetoothDevice device, final int handle, int authReq, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "readCharacteristic");
		final String path = attributePath(device, Bluez.GATT_CHARACTERISTIC, handle);
		if (path == null) {
			Log.e(TAG, "readCharacteristic() - No connection for " + device);
			return;
		}
		BluezBus.call(path, Bluez.GATT_CHARACTERISTIC, "ReadValue", "(a{sv})", new Object[] {NO_OPTIONS}, new BluezBus.Reply() {
			public void onReply(Object[] result, String error, String message) {
				try {
					callback.onCharacteristicRead(device, status(error, message, false), handle, result != null ? (byte[])result[0] : new byte[0]);
				} catch (RemoteException e) {
				}
			}
		});
	}

	@Override
	public void readUsingCharacteristicUuid(IBluetoothGattCallback callback, BluetoothDevice device, ParcelUuid uuid, int startHandle, int endHandle, int authReq,
	                                        AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "readUsingCharacteristicUuid");
		String dp = devicePath(device);
		if (dp == null)
			return;
		for (String p : bluez.paths(Bluez.GATT_CHARACTERISTIC, dp)) {
			int h = handleOf(p);
			if (h >= startHandle && h <= endHandle && uuid.getUuid().toString().equalsIgnoreCase((String)bluez.property(p, Bluez.GATT_CHARACTERISTIC, "UUID"))) {
				readCharacteristic(callback, device, h, authReq, source);
				return;
			}
		}
	}

	private static String writeType(int writeType, boolean reliable) {
		if (reliable)
			return "reliable";
		return writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT ? "request" : "command";
	}

	@Override
	public int writeCharacteristic(final IBluetoothGattCallback callback, final BluetoothDevice device, final int handle, int writeType, int authReq, final byte[] value,
	                               AttributionSource source) {
		if (!AdapterService.checkConnectPermission("GattService", "writeCharacteristic"))
			return BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION;
		final String path = attributePath(device, Bluez.GATT_CHARACTERISTIC, handle);
		Client c = client(callback);
		if (path == null || c == null || !c.connected) {
			Log.e(TAG, "writeCharacteristic() - No connection for " + device);
			return BluetoothStatusCodes.ERROR_DEVICE_NOT_CONNECTED;
		}
		Object[] options = Bluez.dict("type", Bluez.variant("s", writeType(writeType, c.reliable)));
		BluezBus.call(path, Bluez.GATT_CHARACTERISTIC, "WriteValue", "(aya{sv})", new Object[] {value, options}, new BluezBus.Reply() {
			public void onReply(Object[] result, String error, String message) {
				try {
					callback.onCharacteristicWrite(device, status(error, message, true), handle, value);
				} catch (RemoteException e) {
				}
			}
		});
		return BluetoothStatusCodes.SUCCESS;
	}

	@Override
	public void readDescriptor(final IBluetoothGattCallback callback, final BluetoothDevice device, final int handle, int authReq, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "readDescriptor");
		final String path = attributePath(device, Bluez.GATT_DESCRIPTOR, handle);
		if (path == null) {
			Log.e(TAG, "readDescriptor() - No connection for " + device);
			return;
		}
		BluezBus.call(path, Bluez.GATT_DESCRIPTOR, "ReadValue", "(a{sv})", new Object[] {NO_OPTIONS}, new BluezBus.Reply() {
			public void onReply(Object[] result, String error, String message) {
				try {
					callback.onDescriptorRead(device, status(error, message, false), handle, result != null ? (byte[])result[0] : new byte[0]);
				} catch (RemoteException e) {
				}
			}
		});
	}

	@Override
	public int writeDescriptor(final IBluetoothGattCallback callback, final BluetoothDevice device, final int handle, int authReq, final byte[] value,
	                           AttributionSource source) {
		if (!AdapterService.checkConnectPermission("GattService", "writeDescriptor"))
			return BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION;
		final String path = attributePath(device, Bluez.GATT_DESCRIPTOR, handle);
		Client c = client(callback);
		if (path == null || c == null || !c.connected) {
			Log.e(TAG, "writeDescriptor() - No connection for " + device);
			return BluetoothStatusCodes.ERROR_DEVICE_NOT_CONNECTED;
		}
		BluezBus.Reply reply = new BluezBus.Reply() {
			public void onReply(Object[] result, String error, String message) {
				try {
					callback.onDescriptorWrite(device, status(error, message, true), handle, value);
				} catch (RemoteException e) {
				}
			}
		};
		Object uuid = bluez.property(path, Bluez.GATT_DESCRIPTOR, "UUID");
		if (CCCD.toString().equalsIgnoreCase((String)uuid) && value != null && value.length == 2) {
			String characteristic = (String)bluez.property(path, Bluez.GATT_DESCRIPTOR, "Characteristic");
			boolean enable = (value[0] & 0x03) != 0;
			BluezBus.call(characteristic, Bluez.GATT_CHARACTERISTIC, enable ? "StartNotify" : "StopNotify", null, null, reply);
		} else {
			BluezBus.call(path, Bluez.GATT_DESCRIPTOR, "WriteValue", "(aya{sv})", new Object[] {value, NO_OPTIONS}, reply);
		}
		return BluetoothStatusCodes.SUCCESS;
	}

	@Override
	public void registerForNotification(final IBluetoothGattCallback callback, BluetoothDevice device, final int handle, final boolean enable, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "registerForNotification");
		post(new Runnable() {
			public void run() {
				Client c = client(callback);
				if (c == null || !c.connected) {
					Log.e(TAG, "registerForNotification() - No connection");
					return;
				}
				if (enable)
					c.notifications.add(handle);
				else
					c.notifications.remove(handle);
			}
		});
	}

	@Override
	public void beginReliableWrite(BluetoothDevice device, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "beginReliableWrite");
		for (Client c : clientsFor(device.getAddress()))
			c.reliable = true;
	}

	/* BlueZ executes each "reliable" write at once (prepare + execute): nothing is left to execute or cancel */
	@Override
	public void endReliableWrite(final IBluetoothGattCallback callback, final BluetoothDevice device, boolean execute, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "endReliableWrite");
		Client c = client(callback);
		if (c != null)
			c.reliable = false;
		callback(new Call() {
			public void run() throws RemoteException {
				callback.onExecuteWrite(device, BluetoothGatt.GATT_SUCCESS);
			}
		});
	}

	@Override
	public void readRemoteRssi(final IBluetoothGattCallback callback, final BluetoothDevice device, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "readRemoteRssi");
		final short rssi = adapterService.getRemoteRssi(device.getAddress());
		callback(new Call() {
			public void run() throws RemoteException {
				if (rssi != Short.MIN_VALUE)
					callback.onReadRemoteRssi(device, rssi, BluetoothGatt.GATT_SUCCESS);
				else
					callback.onReadRemoteRssi(device, 0, BluetoothGatt.GATT_FAILURE);
			}
		});
	}

	@Override
	public void configureMTU(final IBluetoothGattCallback callback, final BluetoothDevice device, int mtu, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "configureMTU");
		String dp = devicePath(device);
		int linkMtu = 23;
		if (dp != null)
			for (String p : bluez.paths(Bluez.GATT_CHARACTERISTIC, dp)) {
				Object m = bluez.property(p, Bluez.GATT_CHARACTERISTIC, "MTU");
				if (m instanceof Number) {
					linkMtu = ((Number)m).intValue();
					break;
				}
			}
		final int result = linkMtu;
		callback(new Call() {
			public void run() throws RemoteException {
				callback.onConfigureMTU(device, result, BluetoothGatt.GATT_SUCCESS);
			}
		});
	}

	@Override
	public void connectionParameterUpdate(IBluetoothGattCallback callback, BluetoothDevice device, int connectionPriority, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "connectionParameterUpdate");
	}

	@Override
	public void leConnectionUpdate(IBluetoothGattCallback callback, BluetoothDevice device, int minInterval, int maxInterval, int peripheralLatency,
	                               int supervisionTimeout, int minConnectionEventLen, int maxConnectionEventLen, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "leConnectionUpdate");
	}

	@Override
	public int subrateModeRequest(IBluetoothGattCallback callback, BluetoothDevice device, int subrateMode, AttributionSource source) {
		return BluetoothStatusCodes.FEATURE_NOT_SUPPORTED;
	}

	/* ---- GATT server: not provided ---- */

	@Override
	public void registerServer(ParcelUuid appId, final IBluetoothGattServerCallback callback, boolean eattSupport, int transport, AttributionSource source) {
		AdapterService.checkConnectPermission("GattService", "registerServer");
		callback(new Call() {
			public void run() throws RemoteException {
				callback.onServerRegistered(BluetoothGatt.GATT_FAILURE);
			}
		});
	}

	@Override
	public void unregisterServer(IBluetoothGattServerCallback callback, AttributionSource source) {}

	@Override
	public void serverConnect(IBluetoothGattServerCallback callback, BluetoothDevice device, int addressType, boolean isDirect, int transport, AttributionSource source) {}

	@Override
	public void serverDisconnect(IBluetoothGattServerCallback callback, BluetoothDevice device, AttributionSource source) {}

	@Override
	public void serverSetPreferredPhy(IBluetoothGattServerCallback callback, BluetoothDevice device, int txPhy, int rxPhy, int phyOptions, AttributionSource source) {}

	@Override
	public void serverReadPhy(IBluetoothGattServerCallback callback, BluetoothDevice device, AttributionSource source) {}

	@Override
	public void addService(IBluetoothGattServerCallback callback, BluetoothGattService service, AttributionSource source) {}

	@Override
	public void removeService(IBluetoothGattServerCallback callback, int handle, AttributionSource source) {}

	@Override
	public void clearServices(IBluetoothGattServerCallback callback, AttributionSource source) {}

	@Override
	public void sendResponse(IBluetoothGattServerCallback callback, BluetoothDevice device, int requestId, int status, int offset, byte[] value, AttributionSource source) {}

	@Override
	public int sendNotification(IBluetoothGattServerCallback callback, BluetoothDevice device, int handle, boolean confirm, byte[] value, AttributionSource source) {
		return BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND;
	}

	@Override
	public void disconnectAll(AttributionSource source) {}

	/* ---- Bluez.Observer (main thread) ---- */

	private String addressOfDevicePath(String path) {
		Object a = bluez.property(path, Bluez.DEVICE, "Address");
		return a instanceof String ? (String)a : null;
	}

	@Override
	public void onInterfaceAdded(String path, String iface, Map<String, Object> properties) {
		if (!Bluez.DEVICE.equals(iface) || searching.isEmpty())
			return;
		String address = (String)properties.get("Address");
		if (address == null)
			return;
		for (Client c : clientsFor(address))
			if (searching.contains(c))
				tryConnect(c);
	}

	@Override
	public void onInterfaceRemoved(String path, String iface, Map<String, Object> lastProperties) {
		if (!Bluez.DEVICE.equals(iface))
			return;
		String address = (String)lastProperties.get("Address");
		if (address != null)
			linkDown(address, CONN_TIMEOUT);
	}

	private void linkDown(String address, int status) {
		for (Client c : clientsFor(address)) {
			if (!c.connected)
				continue;
			reportConnection(c, status, false);
			c.weConnected = false;
			if (c.wantConnected && !c.direct)
				tryConnect(c); /* autoConnect: AOSP reconnects in the background */
			else
				c.wantConnected = false;
		}
	}

	@Override
	public void onPropertiesChanged(String path, String iface, Map<String, Object> changed, String[] invalidated) {
		if (Bluez.DEVICE.equals(iface)) {
			String address = addressOfDevicePath(path);
			if (address == null)
				return;
			if (Boolean.TRUE.equals(changed.get("Connected"))) {
				for (Client c : clientsFor(address))
					onLinkUp(c, path);
			} else if (Boolean.FALSE.equals(changed.get("Connected"))) {
				linkDown(address, lastDisconnectReason(address));
			}
			if (Boolean.TRUE.equals(changed.get("ServicesResolved"))) {
				for (Client c : clientsFor(address))
					if (c.connected && c.searchPending)
						searchComplete(c, path);
			}
			if (changed.containsKey("RSSI") && !searching.isEmpty()) {
				for (Client c : clientsFor(address))
					if (searching.contains(c))
						tryConnect(c);
			}
			return;
		}
		if (Bluez.GATT_CHARACTERISTIC.equals(iface) && changed.get("Value") instanceof byte[]) {
			/* a notification or indication (BlueZ sets Value; a read reply also does, only notifying ones count) */
			if (!bluez.booleanProperty(path, Bluez.GATT_CHARACTERISTIC, "Notifying"))
				return;
			int slash = path.indexOf("/dev_");
			int end = slash < 0 ? -1 : path.indexOf('/', slash + 1);
			if (end < 0)
				return;
			String address = addressOfDevicePath(path.substring(0, end));
			if (address == null)
				return;
			int handle = handleOf(path);
			byte[] value = (byte[])changed.get("Value");
			BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
			for (Client c : clientsFor(address)) {
				if (c.connected && c.notifications.contains(handle)) {
					try {
						c.callback.onNotify(device, handle, value);
					} catch (RemoteException e) {
					}
				}
			}
		}
	}

	/* BlueZ's Device1.Disconnected signal (with the reason) is not read: a lost link is reported as terminated by
	 * the peer */
	private int lastDisconnectReason(String address) {
		return CONN_TERMINATE_PEER_USER;
	}

	@Override
	public void onReset(boolean present) {
		if (present)
			return;
		synchronized (clients) {
			for (Client c : clients.toArray(new Client[0]))
				if (c.connected)
					reportConnection(c, CONN_TERMINATE_LOCAL_HOST, false);
		}
	}
}
