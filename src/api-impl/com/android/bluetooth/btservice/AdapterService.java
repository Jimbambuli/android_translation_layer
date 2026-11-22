package com.android.bluetooth.btservice;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.bluetooth.bluez.Bluez;
import com.android.bluetooth.bluez.BluezBus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/* ATL: the part of AOSP's Bluetooth app (android/app/src/com/android/bluetooth/btservice: AdapterService,
 * AdapterServiceBinder, AdapterProperties, RemoteDevices, BondStateMachine) and of BluetoothManagerService that the
 * framework classes of one app need, running in the app's process on top of BlueZ (Adapter1/Device1 of the adapter
 * Bluez.adapterPath() picks). The permission checks are AdapterServiceBinder's; the broadcasts are the ones AOSP sends
 * to apps, sent to this app only when it holds the permissions AOSP sends them with.
 *
 * BlueZ keeps one discovery session and one discovery filter per D-Bus client; AOSP's classic discovery (inquiry plus
 * LE, startDiscovery) and the LE scanners of the app therefore share one session here, see updateDiscovery(). */
public final class AdapterService implements Bluez.Observer {
	private static final String TAG = "BluetoothAdapterService";

	/* IBluetoothManager.DEFAULT_MAC_ADDRESS */
	public static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";
	/* btm_inq.cc: BTIF_DM_DEFAULT_INQ_MAX_DURATION (10) * 1280 ms */
	private static final long INQUIRY_DURATION_MS = 10 * 1280;

	private static AdapterService instance;

	private final Bluez bluez;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private int lastState = BluetoothAdapter.STATE_OFF;
	private String lastName;
	private int lastScanMode = BluetoothAdapter.SCAN_MODE_NONE;

	/* discovery (main thread) */
	private boolean classicDiscovery;      /* startDiscovery() until cancelDiscovery() or the end of the inquiry */
	private String discoveryPermission;    /* the location permission ACTION_FOUND is sent with, null if disavowed */
	private final HashSet<String> foundInDiscovery = new HashSet<>();
	private int leScanners;                /* ScanController: number of running LE scans */
	private boolean leDuplicates;
	private boolean sessionStarted;        /* StartDiscovery sent (and not stopped) */
	private String sessionFilter;
	private final Runnable inquiryEnd = new Runnable() {
		public void run() {
			endClassicDiscovery();
		}
	};

	/* bonding started by this app (BondStateMachine) */
	private final HashSet<String> bonding = new HashSet<>();

	private AdapterService() {
		bluez = Bluez.get();
		lastState = getState();
		lastName = bluez.isPresent() ? getNameInternal() : null;
		lastScanMode = getScanModeInternal();
		bluez.addObserver(this);
	}

	public static synchronized AdapterService get() {
		if (instance == null)
			instance = new AdapterService();
		return instance;
	}

	/* true if the desktop has a Bluetooth adapter ATL can use (BluetoothManager/getDefaultAdapter() not null) */
	public static boolean isAvailable() {
		Bluez b = Bluez.get();
		return b.isPresent() && b.adapterPath() != null;
	}

	public Bluez bluez() {
		return bluez;
	}

	public String adapterPath() {
		return bluez.adapterPath();
	}

	public String devicePath(String address) {
		String adapter = adapterPath();
		return adapter == null ? null : Bluez.devicePath(adapter, address);
	}

	/* ---- permissions (Utils, BtPermissionUtils) ---- */

	public static Context context() {
		return android.atl.ATLLoadedApp.getPrimaryApplication().getApplication();
	}

	public static boolean hasPermission(String permission) {
		return context().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
	}

	/* Utils.checkPermissionForDataDelivery(): granted, else a hard denial (ATL has no app-op soft denial) */
	public static boolean checkPermissionForDataDelivery(String permission, String message) {
		if (hasPermission(permission))
			return true;
		throw new SecurityException("Need " + permission + " permission for AttributionSource { uid = " + android.os.Process.myUid() + ", packageName = " + context().getPackageName() + " }: " + message);
	}

	public static boolean checkConnectPermission(String tag, String method) {
		return checkPermissionForDataDelivery(Manifest.permission.BLUETOOTH_CONNECT, tag + "." + method + "()");
	}

	public static boolean checkScanPermission(String tag, String method) {
		return checkPermissionForDataDelivery(Manifest.permission.BLUETOOTH_SCAN, tag + "." + method + "()");
	}

	public static boolean checkAdvertisePermission(String message) {
		return checkPermissionForDataDelivery(Manifest.permission.BLUETOOTH_ADVERTISE, message);
	}

	public static int targetSdk() {
		return context().getApplicationInfo().targetSdkVersion;
	}

	/* Utils.checkCallerTargetSdk(): the app targets at least sdk */
	public static boolean targetsAtLeast(int sdk) {
		return targetSdk() >= sdk;
	}

	/* Utils.hasDisavowedLocationForScan(): BLUETOOTH_SCAN requested with usesPermissionFlags="neverForLocation" */
	public static boolean hasDisavowedLocationForScan() {
		try {
			PackageInfo info = context().getPackageManager().getPackageInfo(context().getPackageName(), PackageManager.GET_PERMISSIONS);
			if (info.requestedPermissions == null)
				return false;
			for (int i = 0; i < info.requestedPermissions.length; i++) {
				if (info.requestedPermissions[i].equals(Manifest.permission.BLUETOOTH_SCAN))
					return (info.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_NEVER_FOR_LOCATION) != 0;
			}
		} catch (PackageManager.NameNotFoundException e) {
			Log.w(TAG, "Could not find package for disavowal check");
		}
		return false;
	}

	/* Utils.checkCallerHasFineLocation()/checkCallerHasCoarseOrFineLocation() (location mode on: the desktop has one) */
	public static boolean hasFineLocation() {
		if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))
			return true;
		Log.e(TAG, "Need ACCESS_FINE_LOCATION permission");
		return false;
	}

	public static boolean hasCoarseOrFineLocation() {
		if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION))
			return true;
		Log.e(TAG, "Need ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission");
		return false;
	}

	private void sendBroadcast(Intent intent, String... permissions) {
		for (String p : permissions)
			if (p != null && !hasPermission(p))
				return;
		Context c = context();
		intent.setPackage(c.getPackageName());
		c.sendBroadcast(intent);
	}

	/* ---- adapter state ---- */

	public int getState() {
		String adapter = adapterPath();
		if (adapter == null)
			return BluetoothAdapter.STATE_OFF;
		Object powerState = bluez.property(adapter, Bluez.ADAPTER, "PowerState");
		if ("off-enabling".equals(powerState))
			return BluetoothAdapter.STATE_TURNING_ON;
		if ("on-disabling".equals(powerState))
			return BluetoothAdapter.STATE_TURNING_OFF;
		return bluez.booleanProperty(adapter, Bluez.ADAPTER, "Powered") ? BluetoothAdapter.STATE_ON : BluetoothAdapter.STATE_OFF;
	}

	/* BluetoothServiceBinder.enable()/disable(): BtPermissionUtils.callerCanToggle(); RESTRICT_ENABLE_DISABLE (T) */
	public boolean setPowered(boolean on, String method) {
		if (!checkConnectPermission("BluetoothAdapter", method))
			return false;
		if (targetsAtLeast(Build.VERSION_CODES.TIRAMISU)) {
			Log.d(TAG, method + "(): FAILED: Caller does not match restriction criteria");
			return false;
		}
		String adapter = adapterPath();
		if (adapter == null)
			return false;
		BluezBus.setProperty(adapter, Bluez.ADAPTER, "Powered", "b", on, null);
		return true;
	}

	public String getAddress() {
		if (!checkConnectPermission("BluetoothAdapter", "getAddress"))
			return null;
		if (!hasPermission("android.permission.LOCAL_MAC_ADDRESS")) {
			Log.w(TAG, "getAddress(): Client does not have LOCAL_MAC_ADDRESS permission");
			return DEFAULT_MAC_ADDRESS;
		}
		String adapter = adapterPath();
		return adapter == null ? null : (String)bluez.property(adapter, Bluez.ADAPTER, "Address");
	}

	private String getNameInternal() {
		String adapter = adapterPath();
		if (adapter == null)
			return null;
		Object alias = bluez.property(adapter, Bluez.ADAPTER, "Alias");
		return alias != null ? (String)alias : (String)bluez.property(adapter, Bluez.ADAPTER, "Name");
	}

	public String getName() {
		if (!checkConnectPermission("BluetoothAdapter", "getName"))
			return null;
		return getNameInternal();
	}

	/* AdapterServiceBinder.setName(): the name is the adapter's alias in BlueZ */
	public boolean setName(String name) {
		if (!checkConnectPermission("BluetoothAdapter", "setName"))
			return false;
		String adapter = adapterPath();
		if (adapter == null || getState() != BluetoothAdapter.STATE_ON)
			return false;
		BluezBus.setProperty(adapter, Bluez.ADAPTER, "Alias", "s", name, null);
		return true;
	}

	private int getScanModeInternal() {
		String adapter = adapterPath();
		if (adapter == null || getState() != BluetoothAdapter.STATE_ON)
			return BluetoothAdapter.SCAN_MODE_NONE;
		if (bluez.booleanProperty(adapter, Bluez.ADAPTER, "Discoverable"))
			return BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
		Object connectable = bluez.property(adapter, Bluez.ADAPTER, "Connectable");
		return Boolean.FALSE.equals(connectable) ? BluetoothAdapter.SCAN_MODE_NONE : BluetoothAdapter.SCAN_MODE_CONNECTABLE;
	}

	public int getScanMode() {
		if (!checkScanPermission("BluetoothAdapter", "getScanMode"))
			return BluetoothAdapter.SCAN_MODE_NONE;
		return getScanModeInternal();
	}

	public long getDiscoverableTimeoutSeconds() {
		String adapter = adapterPath();
		Object t = adapter == null ? null : bluez.property(adapter, Bluez.ADAPTER, "DiscoverableTimeout");
		return t instanceof Number ? ((Number)t).longValue() : 0;
	}

	public List<String> deviceAddresses() {
		ArrayList<String> list = new ArrayList<>();
		String adapter = adapterPath();
		if (adapter == null)
			return list;
		for (String p : bluez.paths(Bluez.DEVICE, adapter)) {
			Object a = bluez.property(p, Bluez.DEVICE, "Address");
			if (a != null)
				list.add((String)a);
		}
		return list;
	}

	public List<String> bondedAddresses() {
		ArrayList<String> list = new ArrayList<>();
		for (String address : deviceAddresses())
			if (isBonded(devicePath(address)))
				list.add(address);
		return list;
	}

	private boolean isBonded(String path) {
		Object bonded = bluez.property(path, Bluez.DEVICE, "Bonded"); /* BlueZ 5.72+ */
		if (bonded instanceof Boolean)
			return (Boolean)bonded;
		return bluez.booleanProperty(path, Bluez.DEVICE, "Paired");
	}

	/* ---- remote devices (RemoteDevices.DeviceProperties) ---- */

	public Map<String, Object> deviceProperties(String address) {
		String path = devicePath(address);
		return path == null ? null : bluez.properties(path, Bluez.DEVICE);
	}

	public String getRemoteName(String address) {
		Map<String, Object> p = deviceProperties(address);
		return p == null ? null : (String)p.get("Name");
	}

	public String getRemoteAlias(String address) {
		Map<String, Object> p = deviceProperties(address);
		if (p == null)
			return null;
		/* BlueZ's Alias falls back to the address with dashes when there is no name; AOSP has no alias then */
		return p.get("Name") == null && p.get("Alias") != null && ((String)p.get("Alias")).replace('-', ':').equalsIgnoreCase(address) ? null : (String)p.get("Alias");
	}

	public boolean setRemoteAlias(String address, String alias) {
		String path = devicePath(address);
		if (path == null || !bluez.has(path, Bluez.DEVICE))
			return false;
		BluezBus.setProperty(path, Bluez.DEVICE, "Alias", "s", alias == null ? "" : alias, null);
		return true;
	}

	public int getRemoteClass(String address) {
		Map<String, Object> p = deviceProperties(address);
		Object c = p == null ? null : p.get("Class");
		return c instanceof Number ? ((Number)c).intValue() : 0;
	}

	/* BlueZ has no device type: BR/EDR devices have a class of device (from inquiry or the link), LE ones advertise
	 * (advertising flags) or have a random address */
	public int getRemoteType(String address) {
		Map<String, Object> p = deviceProperties(address);
		if (p == null)
			return BluetoothDevice.DEVICE_TYPE_UNKNOWN;
		boolean classic = p.get("Class") != null;
		boolean le = p.get("AdvertisingFlags") != null || "random".equals(p.get("AddressType")) || p.get("Appearance") != null;
		return classic && le ? BluetoothDevice.DEVICE_TYPE_DUAL : classic ? BluetoothDevice.DEVICE_TYPE_CLASSIC : le ? BluetoothDevice.DEVICE_TYPE_LE : BluetoothDevice.DEVICE_TYPE_UNKNOWN;
	}

	public int getRemoteAddressType(String address) {
		Map<String, Object> p = deviceProperties(address);
		if (p == null)
			return BluetoothDevice.ADDRESS_TYPE_UNKNOWN;
		return "random".equals(p.get("AddressType")) ? BluetoothDevice.ADDRESS_TYPE_RANDOM : BluetoothDevice.ADDRESS_TYPE_PUBLIC;
	}

	public int getBondState(String address) {
		String path = devicePath(address);
		if (path == null)
			return BluetoothDevice.BOND_NONE;
		synchronized (bonding) {
			if (bonding.contains(address))
				return BluetoothDevice.BOND_BONDING;
		}
		return bluez.has(path, Bluez.DEVICE) && isBonded(path) ? BluetoothDevice.BOND_BONDED : BluetoothDevice.BOND_NONE;
	}

	public boolean isConnected(String address) {
		String path = devicePath(address);
		return path != null && bluez.booleanProperty(path, Bluez.DEVICE, "Connected");
	}

	public ParcelUuid[] getRemoteUuids(String address) {
		Map<String, Object> p = deviceProperties(address);
		Object[] uuids = p == null ? null : (Object[])p.get("UUIDs");
		if (uuids == null)
			return null;
		ParcelUuid[] result = new ParcelUuid[uuids.length];
		for (int i = 0; i < uuids.length; i++)
			result[i] = ParcelUuid.fromString((String)uuids[i]);
		return result;
	}

	public short getRemoteRssi(String address) {
		Map<String, Object> p = deviceProperties(address);
		Object r = p == null ? null : p.get("RSSI");
		return r instanceof Number ? ((Number)r).shortValue() : Short.MIN_VALUE;
	}

	/* BondStateMachine: createBond() is Device1.Pair; BlueZ's agent (the desktop's) does the user interaction */
	public boolean createBond(final String address) {
		if (!checkConnectPermission("BluetoothDevice", "createBond"))
			return false;
		final String path = devicePath(address);
		if (path == null || getState() != BluetoothAdapter.STATE_ON || !bluez.has(path, Bluez.DEVICE))
			return false;
		if (getBondState(address) != BluetoothDevice.BOND_NONE)
			return false;
		synchronized (bonding) {
			bonding.add(address);
		}
		sendBondState(address, BluetoothDevice.BOND_BONDING, BluetoothDevice.BOND_NONE, -1);
		BluezBus.call(path, Bluez.DEVICE, "Pair", null, null, 60000, new BluezBus.Reply() {
			public void onReply(Object[] result, String error, String message) {
				synchronized (bonding) {
					bonding.remove(address);
				}
				if (error != null) {
					Log.i(TAG, "pairing failed: " + error + " " + message);
					/* BluetoothDevice.UNBOND_REASON_AUTH_FAILED / _AUTH_CANCELED / _REMOTE_DEVICE_DOWN */
					String e = Bluez.shortError(error);
					int reason = "AuthenticationCanceled".equals(e) ? 3 : "ConnectionAttemptFailed".equals(e) || "AuthenticationTimeout".equals(e) ? 4 : 1;
					sendBondState(address, BluetoothDevice.BOND_NONE, BluetoothDevice.BOND_BONDING, reason);
				} else if (!isBonded(path)) {
					/* Paired may follow the reply */
					if (bluez.booleanProperty(path, Bluez.DEVICE, "Paired"))
						sendBondState(address, BluetoothDevice.BOND_BONDED, BluetoothDevice.BOND_BONDING, -1);
				}
			}
		});
		return true;
	}

	public boolean cancelBond(String address) {
		if (!checkConnectPermission("BluetoothDevice", "cancelBondProcess"))
			return false;
		String path = devicePath(address);
		if (path == null)
			return false;
		synchronized (bonding) {
			if (!bonding.contains(address))
				return false;
		}
		BluezBus.call(path, Bluez.DEVICE, "CancelPairing", null, null, null);
		return true;
	}

	public boolean removeBond(String address) {
		if (!checkConnectPermission("BluetoothDevice", "removeBond"))
			return false;
		String adapter = adapterPath();
		String path = devicePath(address);
		if (adapter == null || !bluez.has(path, Bluez.DEVICE) || !isBonded(path))
			return false;
		BluezBus.call(adapter, Bluez.ADAPTER, "RemoveDevice", "(o)", new Object[] {path}, null);
		return true;
	}

	private void sendBondState(String address, int state, int previous, int reason) {
		Intent intent = new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		intent.putExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address));
		intent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, state);
		intent.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, previous);
		if (state == BluetoothDevice.BOND_NONE && reason >= 0)
			intent.putExtra("android.bluetooth.device.extra.REASON", reason);
		sendBroadcast(intent, Manifest.permission.BLUETOOTH_CONNECT);
	}

	/* fetchUuidsWithSdp(): BlueZ resolves the services when it connects; the answer is ACTION_UUID with the UUIDs it has */
	public boolean fetchUuidsWithSdp(final String address) {
		if (!checkConnectPermission("BluetoothDevice", "fetchUuidsWithSdp"))
			return false;
		if (getState() != BluetoothAdapter.STATE_ON)
			return false;
		handler.post(new Runnable() {
			public void run() {
				sendUuidIntent(address);
			}
		});
		return true;
	}

	private void sendUuidIntent(String address) {
		Intent intent = new Intent(BluetoothDevice.ACTION_UUID);
		intent.putExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address));
		intent.putExtra(BluetoothDevice.EXTRA_UUID, getRemoteUuids(address));
		sendBroadcast(intent, Manifest.permission.BLUETOOTH_CONNECT);
	}

	/* ---- discovery (AdapterService.startDiscovery/cancelDiscovery, ScanController) ---- */

	public boolean startDiscovery() {
		if (!checkScanPermission("BluetoothAdapter", "startDiscovery"))
			return false;
		if (getState() != BluetoothAdapter.STATE_ON)
			return false;
		boolean isQApp = targetsAtLeast(Build.VERSION_CODES.Q);
		boolean disavowed = hasDisavowedLocationForScan();
		String permission = null;
		if (!disavowed) {
			if (isQApp) {
				if (!hasFineLocation())
					return false;
				permission = Manifest.permission.ACCESS_FINE_LOCATION;
			} else {
				if (!hasCoarseOrFineLocation())
					return false;
				permission = Manifest.permission.ACCESS_COARSE_LOCATION;
			}
		}
		final String p = permission;
		handler.post(new Runnable() {
			public void run() {
				discoveryPermission = p;
				foundInDiscovery.clear();
				boolean was = classicDiscovery;
				classicDiscovery = true;
				handler.removeCallbacks(inquiryEnd);
				handler.postDelayed(inquiryEnd, INQUIRY_DURATION_MS);
				updateDiscovery();
				if (!was) {
					sendBroadcast(new Intent(BluetoothAdapter.ACTION_DISCOVERY_STARTED), Manifest.permission.BLUETOOTH_SCAN);
				}
				/* devices BlueZ already sees in this session count as found now */
				for (String address : deviceAddresses())
					if (getRemoteRssi(address) != Short.MIN_VALUE)
						deviceFound(address);
			}
		});
		synchronized (this) {
			pendingDiscovery = true;
		}
		return true;
	}

	private boolean pendingDiscovery; /* between startDiscovery() and its start on the main thread */

	public boolean cancelDiscovery() {
		if (!checkScanPermission("BluetoothAdapter", "cancelDiscovery"))
			return false;
		if (getState() != BluetoothAdapter.STATE_ON)
			return false;
		handler.post(new Runnable() {
			public void run() {
				endClassicDiscovery();
			}
		});
		return true;
	}

	public boolean isDiscovering() {
		if (!checkScanPermission("BluetoothAdapter", "isDiscovering"))
			return false;
		synchronized (this) {
			if (pendingDiscovery)
				return true;
		}
		return classicDiscovery;
	}

	private void endClassicDiscovery() {
		synchronized (this) {
			pendingDiscovery = false;
		}
		handler.removeCallbacks(inquiryEnd);
		if (!classicDiscovery)
			return;
		classicDiscovery = false;
		updateDiscovery();
		sendBroadcast(new Intent(BluetoothAdapter.ACTION_DISCOVERY_FINISHED), Manifest.permission.BLUETOOTH_SCAN);
	}

	private final HashMap<String, int[]> leScanOwners = new HashMap<>();

	/* ScanController, GattService: LE scans running (main thread); duplicates: a scan wants every advertisement */
	public void setLeScanning(String owner, int scanners, boolean duplicates) {
		leScanOwners.put(owner, new int[] {scanners, duplicates ? 1 : 0});
		leScanners = 0;
		leDuplicates = false;
		for (int[] v : leScanOwners.values()) {
			leScanners += v[0];
			leDuplicates |= v[0] > 0 && v[1] != 0;
		}
		updateDiscovery();
	}

	/* one BlueZ discovery session for classic discovery and LE scans: Transport auto (inquiry and LE, like AOSP's
	 * general discovery) while startDiscovery() runs, else le */
	private void updateDiscovery() {
		synchronized (this) {
			pendingDiscovery = false;
		}
		final String adapter = adapterPath();
		if (adapter == null)
			return;
		boolean wanted = classicDiscovery || leScanners > 0;
		if (!wanted) {
			if (sessionStarted) {
				sessionStarted = false;
				sessionFilter = null;
				BluezBus.call(adapter, Bluez.ADAPTER, "StopDiscovery", null, null, logError("StopDiscovery"));
			}
			return;
		}
		String transport = classicDiscovery ? "auto" : "le";
		boolean duplicates = leScanners > 0 && leDuplicates;
		String filter = transport + duplicates;
		if (!filter.equals(sessionFilter)) {
			sessionFilter = filter;
			BluezBus.call(adapter, Bluez.ADAPTER, "SetDiscoveryFilter", "(a{sv})",
			              new Object[] {Bluez.dict("Transport", Bluez.variant("s", transport), "DuplicateData", Bluez.variant("b", duplicates))},
			              logError("SetDiscoveryFilter"));
		}
		if (!sessionStarted) {
			sessionStarted = true;
			BluezBus.call(adapter, Bluez.ADAPTER, "StartDiscovery", null, null, new BluezBus.Reply() {
				public void onReply(Object[] result, String error, String message) {
					if (error != null && !"InProgress".equals(Bluez.shortError(error))) {
						Log.w(TAG, "StartDiscovery: " + error + " " + message);
						sessionStarted = false;
						sessionFilter = null;
					}
				}
			});
		}
	}

	private static BluezBus.Reply logError(final String what) {
		return new BluezBus.Reply() {
			public void onReply(Object[] result, String error, String message) {
				if (error != null)
					Log.w(TAG, what + ": " + error + " " + message);
			}
		};
	}

	/* RemoteDevices.deviceFoundCallback() */
	private void deviceFound(String address) {
		if (!classicDiscovery)
			return;
		Map<String, Object> p = deviceProperties(address);
		if (p == null)
			return;
		BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
		Intent intent = new Intent(BluetoothDevice.ACTION_FOUND);
		intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
		intent.putExtra(BluetoothDevice.EXTRA_CLASS, new BluetoothClass(getRemoteClass(address)));
		intent.putExtra(BluetoothDevice.EXTRA_RSSI, getRemoteRssi(address));
		intent.putExtra(BluetoothDevice.EXTRA_NAME, (String)p.get("Name"));
		intent.putExtra(BluetoothDevice.EXTRA_IS_COORDINATED_SET_MEMBER, false);
		if (discoveryPermission != null)
			sendBroadcast(intent, Manifest.permission.BLUETOOTH_SCAN, discoveryPermission);
		else
			sendBroadcast(intent, Manifest.permission.BLUETOOTH_SCAN);
	}

	private static String addressOf(Map<String, Object> props) {
		Object a = props == null ? null : props.get("Address");
		return a instanceof String ? (String)a : null;
	}

	/* ---- profiles: BlueZ connects the profiles itself; a device counts as connected for a profile when its link is
	 * up and it has the profile's remote role (an approximation: BlueZ does not tell which profile carries the link) */

	private static String[] profileUuids(int profile) {
		switch (profile) {
			case BluetoothProfile.HEADSET: /* the remote headset/hands-free unit: HSP HS, HFP HF */
				return new String[] {"00001108-0000-1000-8000-00805f9b34fb", "0000111e-0000-1000-8000-00805f9b34fb"};
			case BluetoothProfile.A2DP: /* the remote audio sink */
				return new String[] {"0000110b-0000-1000-8000-00805f9b34fb"};
			case BluetoothProfile.HID_HOST: /* HID, HID over GATT */
				return new String[] {"00001124-0000-1000-8000-00805f9b34fb", "00001812-0000-1000-8000-00805f9b34fb"};
			case BluetoothProfile.PAN: /* PANU, NAP */
				return new String[] {"00001115-0000-1000-8000-00805f9b34fb", "00001116-0000-1000-8000-00805f9b34fb"};
			default:
				return new String[0];
		}
	}

	public int profileConnectionState(int profile, String address) {
		AdapterService.checkConnectPermission("BluetoothProfile", "getConnectionState");
		if (!isConnected(address))
			return BluetoothProfile.STATE_DISCONNECTED;
		ParcelUuid[] uuids = getRemoteUuids(address);
		if (uuids != null)
			for (ParcelUuid u : uuids)
				for (String p : profileUuids(profile))
					if (p.equalsIgnoreCase(u.toString()))
						return BluetoothProfile.STATE_CONNECTED;
		return BluetoothProfile.STATE_DISCONNECTED;
	}

	public List<BluetoothDevice> profileDevices(int profile, int[] states) {
		AdapterService.checkConnectPermission("BluetoothProfile", "getDevicesMatchingConnectionStates");
		ArrayList<BluetoothDevice> list = new ArrayList<>();
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null || states == null)
			return list;
		for (String address : deviceAddresses()) {
			int state = profileConnectionState(profile, address);
			for (int s : states)
				if (s == state) {
					list.add(adapter.getRemoteDevice(address));
					break;
				}
		}
		return list;
	}

	/* AdapterService.getProfileConnectionState() */
	public int getProfileConnectionState(int profile) {
		int best = BluetoothProfile.STATE_DISCONNECTED;
		for (String address : deviceAddresses())
			if (profileConnectionState(profile, address) == BluetoothProfile.STATE_CONNECTED)
				best = BluetoothProfile.STATE_CONNECTED;
		return best;
	}

	/* ---- Bluez.Observer (main thread) ---- */

	private void checkAdapterChanges() {
		int state = getState();
		if (state != lastState) {
			int previous = lastState;
			lastState = state;
			if (state == BluetoothAdapter.STATE_OFF) {
				classicDiscovery = false;
				sessionStarted = false;
				sessionFilter = null;
				handler.removeCallbacks(inquiryEnd);
			}
			/* AOSP goes OFF -> TURNING_ON -> ON; BlueZ without PowerState reports only the end */
			if (previous == BluetoothAdapter.STATE_OFF && state == BluetoothAdapter.STATE_ON)
				sendStateChange(BluetoothAdapter.STATE_TURNING_ON, previous);
			else if (previous == BluetoothAdapter.STATE_ON && state == BluetoothAdapter.STATE_OFF)
				sendStateChange(BluetoothAdapter.STATE_TURNING_OFF, previous);
			sendStateChange(state, previous == BluetoothAdapter.STATE_OFF && state == BluetoothAdapter.STATE_ON ? BluetoothAdapter.STATE_TURNING_ON
			                       : previous == BluetoothAdapter.STATE_ON && state == BluetoothAdapter.STATE_OFF ? BluetoothAdapter.STATE_TURNING_OFF
			                       : previous);
			for (StateListener l : stateListeners())
				l.onStateChanged(state);
		}
		String name = getNameInternal();
		if (name != null && !name.equals(lastName)) {
			lastName = name;
			Intent intent = new Intent(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
			intent.putExtra(BluetoothAdapter.EXTRA_LOCAL_NAME, name);
			sendBroadcast(intent, Manifest.permission.BLUETOOTH_CONNECT);
		}
		int scanMode = getScanModeInternal();
		if (scanMode != lastScanMode) {
			int previous = lastScanMode;
			lastScanMode = scanMode;
			Intent intent = new Intent(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
			intent.putExtra(BluetoothAdapter.EXTRA_SCAN_MODE, scanMode);
			intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, previous);
			sendBroadcast(intent, Manifest.permission.BLUETOOTH_SCAN);
		}
	}

	private void sendStateChange(int state, int previous) {
		Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
		intent.putExtra(BluetoothAdapter.EXTRA_STATE, state);
		intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, previous);
		sendBroadcast(intent);
	}

	public interface StateListener {
		void onStateChanged(int state);
	}

	private final ArrayList<StateListener> stateListeners = new ArrayList<>();

	public synchronized void addStateListener(StateListener l) {
		stateListeners.add(l);
	}

	private synchronized StateListener[] stateListeners() {
		return stateListeners.toArray(new StateListener[0]);
	}

	private boolean isOurDevice(String path) {
		String adapter = adapterPath();
		return adapter != null && path.startsWith(adapter + "/dev_") && path.indexOf('/', adapter.length() + 5) < 0;
	}

	@Override
	public void onInterfaceAdded(String path, String iface, Map<String, Object> properties) {
		if (Bluez.ADAPTER.equals(iface))
			checkAdapterChanges();
		else if (Bluez.DEVICE.equals(iface) && isOurDevice(path)) {
			String address = addressOf(properties);
			if (address != null && properties.get("RSSI") != null)
				deviceFound(address);
		}
	}

	@Override
	public void onInterfaceRemoved(String path, String iface, Map<String, Object> lastProperties) {
		if (Bluez.ADAPTER.equals(iface))
			checkAdapterChanges();
		else if (Bluez.DEVICE.equals(iface) && isOurDevice(path)) {
			String address = addressOf(lastProperties);
			if (address == null)
				return;
			if (Boolean.TRUE.equals(lastProperties.get("Connected")))
				sendAcl(address, false);
			if (Boolean.TRUE.equals(lastProperties.get("Paired")) || Boolean.TRUE.equals(lastProperties.get("Bonded")))
				sendBondState(address, BluetoothDevice.BOND_NONE, BluetoothDevice.BOND_BONDED, -1);
		}
	}

	@Override
	public void onPropertiesChanged(String path, String iface, Map<String, Object> changed, String[] invalidated) {
		if (Bluez.ADAPTER.equals(iface)) {
			checkAdapterChanges();
			return;
		}
		if (!Bluez.DEVICE.equals(iface) || !isOurDevice(path))
			return;
		String address = addressOf(bluez.properties(path, Bluez.DEVICE));
		if (address == null)
			return;
		if (changed.containsKey("RSSI"))
			deviceFound(address);
		if (changed.containsKey("Name")) {
			Intent intent = new Intent(BluetoothDevice.ACTION_NAME_CHANGED);
			intent.putExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address));
			intent.putExtra(BluetoothDevice.EXTRA_NAME, (String)changed.get("Name"));
			sendBroadcast(intent, Manifest.permission.BLUETOOTH_CONNECT);
		}
		if (changed.containsKey("Class")) {
			Intent intent = new Intent(BluetoothDevice.ACTION_CLASS_CHANGED);
			intent.putExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address));
			intent.putExtra(BluetoothDevice.EXTRA_CLASS, new BluetoothClass(((Number)changed.get("Class")).intValue()));
			sendBroadcast(intent, Manifest.permission.BLUETOOTH_CONNECT);
		}
		if (changed.containsKey("Connected"))
			sendAcl(address, Boolean.TRUE.equals(changed.get("Connected")));
		if (changed.containsKey("UUIDs") && getState() == BluetoothAdapter.STATE_ON)
			sendUuidIntent(address);
		Object paired = changed.containsKey("Bonded") ? changed.get("Bonded") : changed.get("Paired");
		if (paired instanceof Boolean) {
			boolean wasBonding;
			synchronized (bonding) {
				wasBonding = bonding.remove(address);
			}
			if ((Boolean)paired)
				sendBondState(address, BluetoothDevice.BOND_BONDED, wasBonding ? BluetoothDevice.BOND_BONDING : BluetoothDevice.BOND_NONE, -1);
			else
				sendBondState(address, BluetoothDevice.BOND_NONE, BluetoothDevice.BOND_BONDED, -1);
		}
	}

	private void sendAcl(String address, boolean connected) {
		Intent intent = new Intent(connected ? BluetoothDevice.ACTION_ACL_CONNECTED : BluetoothDevice.ACTION_ACL_DISCONNECTED);
		intent.putExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address));
		int type = getRemoteType(address);
		intent.putExtra(BluetoothDevice.EXTRA_TRANSPORT, type == BluetoothDevice.DEVICE_TYPE_LE ? BluetoothDevice.TRANSPORT_LE : BluetoothDevice.TRANSPORT_BREDR);
		sendBroadcast(intent, Manifest.permission.BLUETOOTH_CONNECT);
	}

	@Override
	public void onReset(boolean present) {
		sessionStarted = false;
		sessionFilter = null;
		checkAdapterChanges();
		if (present)
			updateDiscovery();
	}
}
