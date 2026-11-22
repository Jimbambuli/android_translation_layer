package com.android.bluetooth.le_scan;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothScan;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.IPeriodicAdvertisingCallback;
import android.bluetooth.le.IScannerCallback;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.AttributionSource;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.WorkSource;
import android.util.Log;
import com.android.bluetooth.bluez.Bluez;
import com.android.bluetooth.btservice.AdapterService;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/* ATL: AOSP's le_scan/ScanController (android/app, android-16.0.0_r4) as the IBluetoothScan of the app's process,
 * on top of BlueZ discovery (Transport le, shared with classic discovery in AdapterService.updateDiscovery()).
 *
 * BlueZ gives no advertising reports, only the device properties it parsed from them; every update that comes from
 * an advertisement (a Device1 appearing with RSSI, a change of RSSI, ManufacturerData, ServiceData, AdvertisingData,
 * TxPower, AdvertisingFlags) is one result, its scan record rebuilt from those properties (ScanRecord.getBytes()
 * therefore holds the same AD structures, not the radio bytes). The event type is that of AOSP's ScanResult
 * compatibility constructor (legacy, connectable), primary PHY 1M.
 *
 * BlueZ filters nothing in the controller: no hardware tracking (FIRST_MATCH/MATCH_LOST) and no batching, as AOSP on
 * a controller without these offloads (BluetoothLeScanner then refuses those settings itself). Scan modes cannot be
 * set in BlueZ; an opportunistic scan only sees what other scans of the app find. */
public final class ScanController implements IBluetoothScan, Bluez.Observer {
	private static final String TAG = "BtGattService";

	private static ScanController instance;

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final AdapterService adapterService;

	private static final class Client {
		final int id;
		final IScannerCallback callback; /* null for a PendingIntent scan */
		final PendingIntent intent;
		ScanSettings settings;
		List<ScanFilter> filters;
		boolean started;
		boolean hasPermission; /* ScanController.hasScanResultPermission() */

		Client(int id, IScannerCallback callback, PendingIntent intent) {
			this.id = id;
			this.callback = callback;
			this.intent = intent;
		}
	}

	private final HashMap<Integer, Client> clients = new HashMap<>();
	private int nextId = 1;

	private ScanController() {
		adapterService = AdapterService.get();
		adapterService.bluez().addObserver(this);
	}

	public static synchronized ScanController get() {
		if (instance == null)
			instance = new ScanController();
		return instance;
	}

	/* ---- IBluetoothScan ---- */

	@Override
	public void registerScanner(final IScannerCallback callback, WorkSource workSource, AttributionSource source) {
		AdapterService.checkScanPermission("ScanController", "registerScanner");
		final int id;
		synchronized (this) {
			id = nextId++;
			clients.put(id, new Client(id, callback, null));
		}
		com.android.bluetooth.bluez.BinderThread.post(new Runnable() {
			public void run() {
				try {
					callback.onScannerRegistered(ScanCallback.NO_ERROR, id);
				} catch (RemoteException e) {
				}
			}
		});
	}

	@Override
	public void unregisterScanner(final int scannerId, AttributionSource source) {
		AdapterService.checkScanPermission("ScanController", "unregisterScanner");
		handler.post(new Runnable() {
			public void run() {
				synchronized (ScanController.this) {
					clients.remove(scannerId);
				}
				update();
			}
		});
	}

	/* ScanController.startScan(): permissions decided at start, as in AOSP */
	@Override
	public void startScan(final int scannerId, final ScanSettings settings, final List<ScanFilter> filters, AttributionSource source) {
		AdapterService.checkScanPermission("ScanController", "startScan");
		final boolean permission = scanResultPermission();
		handler.post(new Runnable() {
			public void run() {
				Client c;
				synchronized (ScanController.this) {
					c = clients.get(scannerId);
				}
				if (c == null)
					return;
				c.settings = settings;
				c.filters = filters != null ? new ArrayList<>(filters) : new ArrayList<ScanFilter>();
				c.hasPermission = permission;
				c.started = true;
				update();
				deliverKnown(c);
			}
		});
	}

	@Override
	public void registerPiAndStartScan(PendingIntent intent, ScanSettings settings, List<ScanFilter> filters, AttributionSource source) {
		AdapterService.checkScanPermission("ScanController", "registerPiAndStartScan");
		final int id;
		synchronized (this) {
			for (Client c : clients.values())
				if (c.intent != null && c.intent.equals(intent))
					return; /* AOSP: already scanning for this intent */
			id = nextId++;
			clients.put(id, new Client(id, null, intent));
		}
		startScan(id, settings, filters, source);
	}

	@Override
	public void stopScan(final int scannerId, AttributionSource source) {
		AdapterService.checkScanPermission("ScanController", "stopScan");
		handler.post(new Runnable() {
			public void run() {
				Client c;
				synchronized (ScanController.this) {
					c = clients.get(scannerId);
				}
				if (c != null)
					c.started = false;
				update();
			}
		});
	}

	@Override
	public void stopScanForIntent(final PendingIntent intent, AttributionSource source) {
		AdapterService.checkScanPermission("ScanController", "stopScanForIntent");
		handler.post(new Runnable() {
			public void run() {
				synchronized (ScanController.this) {
					java.util.Iterator<Client> it = clients.values().iterator();
					while (it.hasNext())
						if (intent.equals(it.next().intent))
							it.remove();
				}
				update();
			}
		});
	}

	/* no batching: nothing pending */
	@Override
	public void flushPendingBatchResults(int scannerId, AttributionSource source) {
		AdapterService.checkScanPermission("ScanController", "flushPendingBatchResults");
	}

	/* periodic advertising sync: BlueZ has no D-Bus API; AOSP reports a failed sync to the callback */
	@Override
	public void registerSync(ScanResult scanResult, int skip, int timeout, final IPeriodicAdvertisingCallback callback, AttributionSource source) {
		AdapterService.checkScanPermission("ScanController", "registerSync");
		handler.post(new Runnable() {
			public void run() {
				try {
					/* PeriodicAdvertisingCallback.SYNC_NO_RESOURCES */
					callback.onSyncEstablished(0, null, 0, 0, 0, 2);
				} catch (RemoteException e) {
				}
			}
		});
	}

	@Override
	public void unregisterSync(IPeriodicAdvertisingCallback callback, AttributionSource source) {}

	@Override
	public void transferSync(BluetoothDevice bda, int serviceData, int syncHandle, AttributionSource source) {}

	@Override
	public void transferSetInfo(BluetoothDevice bda, int serviceData, int advertisingHandle, IPeriodicAdvertisingCallback callback, AttributionSource source) {}

	@Override
	public int numHwTrackFiltersAvailable(AttributionSource source) {
		return 0;
	}

	/* ---- permissions (ScanController.startScan + hasScanResultPermission) ---- */

	private static boolean scanResultPermission() {
		if (AdapterService.hasDisavowedLocationForScan())
			return true;
		if (AdapterService.targetsAtLeast(Build.VERSION_CODES.Q))
			return AdapterService.hasFineLocation();
		return AdapterService.hasCoarseOrFineLocation();
	}

	/* ---- the BlueZ session ---- */

	private void update() {
		int scanners = 0;
		synchronized (this) {
			for (Client c : clients.values())
				if (c.started && c.settings != null && c.settings.getScanMode() != ScanSettings.SCAN_MODE_OPPORTUNISTIC)
					scanners++;
		}
		/* every advertisement makes a result (CALLBACK_TYPE_ALL_MATCHES) */
		adapterService.setLeScanning("scan", scanners, true);
	}

	private synchronized Client[] startedClients() {
		ArrayList<Client> list = new ArrayList<>();
		for (Client c : clients.values())
			if (c.started)
				list.add(c);
		return list.toArray(new Client[0]);
	}

	/* devices BlueZ sees in the running session when a scan starts */
	private void deliverKnown(Client c) {
		String adapter = adapterService.adapterPath();
		if (adapter == null)
			return;
		for (String path : adapterService.bluez().paths(Bluez.DEVICE, adapter)) {
			Map<String, Object> p = adapterService.bluez().properties(path, Bluez.DEVICE);
			if (p != null && p.get("RSSI") != null)
				deliver(new Client[] {c}, p);
		}
	}

	private static final String[] ADVERTISEMENT_PROPERTIES = {"RSSI", "ManufacturerData", "ServiceData", "AdvertisingData", "TxPower", "AdvertisingFlags"};

	private void onAdvertisement(String path, Map<String, Object> changed) {
		boolean fromAdvertisement = false;
		for (String name : ADVERTISEMENT_PROPERTIES)
			fromAdvertisement |= changed.containsKey(name);
		if (!fromAdvertisement)
			return;
		Client[] started = startedClients();
		if (started.length == 0)
			return;
		Map<String, Object> p = adapterService.bluez().properties(path, Bluez.DEVICE);
		if (p == null || p.get("RSSI") == null)
			return;
		deliver(started, p);
	}

	private void deliver(Client[] targets, Map<String, Object> p) {
		String address = (String)p.get("Address");
		if (address == null)
			return;
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null)
			return;
		int addressType = "random".equals(p.get("AddressType")) ? BluetoothDevice.ADDRESS_TYPE_RANDOM : BluetoothDevice.ADDRESS_TYPE_PUBLIC;
		BluetoothDevice device = adapter.getRemoteLeDevice(address, addressType);
		int rssi = ((Number)p.get("RSSI")).intValue();
		int txPower = p.get("TxPower") instanceof Number ? ((Number)p.get("TxPower")).intValue() : ScanResult.TX_POWER_NOT_PRESENT;
		ScanRecord record = ScanRecord.parseFromBytes(advertisingData(p));
		/* ScanResult(BluetoothDevice, ScanRecord, int, long): legacy connectable, 1M, no SID */
		ScanResult result = new ScanResult(device, (ScanResult.DATA_COMPLETE << 5) | 0x10 | 0x01, BluetoothDevice.PHY_LE_1M, ScanResult.PHY_UNUSED,
		                                   ScanResult.SID_NOT_PRESENT, txPower, rssi, ScanResult.PERIODIC_INTERVAL_NOT_PRESENT, record,
		                                   SystemClock.elapsedRealtimeNanos());
		for (Client c : targets) {
			if (!c.hasPermission) {
				Log.v(TAG, "Skipping client: No permission");
				continue;
			}
			if (!matchesFilters(c, result)) {
				continue;
			}
			int callbackType = c.settings.getCallbackType();
			if (!(callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES || callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES_AUTO_BATCH)) {
				continue;
			}
			try {
				if (c.callback != null) {
					c.callback.onScanResult(result);
				} else {
					Intent extras = new Intent();
					extras.putParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT, new ArrayList<>(Arrays.asList(result)));
					extras.putExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
					c.intent.send(AdapterService.context(), 0, extras);
				}
			} catch (RemoteException | PendingIntent.CanceledException e) {
				Log.e(TAG, "Exception: " + e);
				synchronized (this) {
					clients.remove(c.id);
				}
				update();
			}
		}
	}

	/* ScanController.matchesFilters() */
	private static boolean matchesFilters(Client client, ScanResult scanResult) {
		if (scanResult.getRssi() < client.settings.getRssiThreshold()) {
			return false;
		}
		if (client.filters.isEmpty()) {
			return true;
		}
		for (ScanFilter filter : client.filters) {
			if (filter.matches(scanResult)) {
				return true;
			}
		}
		return false;
	}

	/* the AD structures of the advertisement, from what BlueZ parsed */
	@SuppressWarnings("unchecked")
	static byte[] advertisingData(Map<String, Object> p) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Object flags = p.get("AdvertisingFlags");
		if (flags instanceof byte[])
			ad(out, ScanRecord.DATA_TYPE_FLAGS, (byte[])flags);
		Object[] uuids = (Object[])p.get("UUIDs");
		Map<String, Object> serviceData = (Map<String, Object>)p.get("ServiceData");
		if (uuids != null) {
			ByteArrayOutputStream u16 = new ByteArrayOutputStream(), u32 = new ByteArrayOutputStream(), u128 = new ByteArrayOutputStream();
			for (Object o : uuids) {
				UUID uuid = UUID.fromString((String)o);
				byte[] b = shortest(uuid);
				(b.length == 2 ? u16 : b.length == 4 ? u32 : u128).write(b, 0, b.length);
			}
			if (u16.size() > 0)
				ad(out, ScanRecord.DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE, u16.toByteArray());
			if (u32.size() > 0)
				ad(out, ScanRecord.DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE, u32.toByteArray());
			if (u128.size() > 0)
				ad(out, ScanRecord.DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE, u128.toByteArray());
		}
		Object name = p.get("Name");
		if (name instanceof String) {
			byte[] b = ((String)name).getBytes(java.nio.charset.StandardCharsets.UTF_8);
			ad(out, ScanRecord.DATA_TYPE_LOCAL_NAME_COMPLETE, b.length > 248 ? Arrays.copyOf(b, 248) : b);
		}
		Object tx = p.get("TxPower");
		if (tx instanceof Number)
			ad(out, ScanRecord.DATA_TYPE_TX_POWER_LEVEL, new byte[] {(byte)((Number)tx).intValue()});
		Object appearance = p.get("Appearance");
		if (appearance instanceof Number) {
			int a = ((Number)appearance).intValue();
			ad(out, 0x19, new byte[] {(byte)a, (byte)(a >> 8)});
		}
		if (serviceData != null) {
			for (Map.Entry<String, Object> e : serviceData.entrySet()) {
				byte[] u = shortest(UUID.fromString(e.getKey()));
				byte[] d = (byte[])e.getValue();
				byte[] b = new byte[u.length + d.length];
				System.arraycopy(u, 0, b, 0, u.length);
				System.arraycopy(d, 0, b, u.length, d.length);
				ad(out, u.length == 2 ? ScanRecord.DATA_TYPE_SERVICE_DATA_16_BIT : u.length == 4 ? 0x20 : 0x21, b);
			}
		}
		Map<Object, Object> manufacturer = (Map<Object, Object>)p.get("ManufacturerData");
		if (manufacturer != null) {
			for (Map.Entry<Object, Object> e : manufacturer.entrySet()) {
				int company = ((Number)e.getKey()).intValue();
				byte[] d = (byte[])e.getValue();
				byte[] b = new byte[2 + d.length];
				b[0] = (byte)company;
				b[1] = (byte)(company >> 8);
				System.arraycopy(d, 0, b, 2, d.length);
				ad(out, ScanRecord.DATA_TYPE_MANUFACTURER_SPECIFIC_DATA, b);
			}
		}
		Map<Object, Object> other = (Map<Object, Object>)p.get("AdvertisingData");
		if (other != null) {
			for (Map.Entry<Object, Object> e : other.entrySet())
				ad(out, ((Number)e.getKey()).intValue(), (byte[])e.getValue());
		}
		return out.toByteArray();
	}

	private static void ad(ByteArrayOutputStream out, int type, byte[] data) {
		if (data.length > 254)
			data = Arrays.copyOf(data, 254);
		out.write(data.length + 1);
		out.write(type);
		out.write(data, 0, data.length);
	}

	/* little endian, 16 or 32 bit if it is a Bluetooth base UUID (BluetoothUuid.uuidToBytes) */
	private static byte[] shortest(UUID uuid) {
		long msb = uuid.getMostSignificantBits(), lsb = uuid.getLeastSignificantBits();
		boolean base = (msb & 0xFFFFFFFFL) == 0x1000L && lsb == 0x800000805F9B34FBL;
		if (base) {
			long v = msb >>> 32;
			if (v <= 0xFFFF)
				return new byte[] {(byte)v, (byte)(v >> 8)};
			return new byte[] {(byte)v, (byte)(v >> 8), (byte)(v >> 16), (byte)(v >> 24)};
		}
		byte[] b = new byte[16];
		for (int i = 0; i < 8; i++) {
			b[i] = (byte)(lsb >>> (8 * i));
			b[8 + i] = (byte)(msb >>> (8 * i));
		}
		return b;
	}

	/* ---- Bluez.Observer (main thread) ---- */

	@Override
	public void onInterfaceAdded(String path, String iface, Map<String, Object> properties) {
		if (Bluez.DEVICE.equals(iface))
			onAdvertisement(path, properties);
	}

	@Override
	public void onInterfaceRemoved(String path, String iface, Map<String, Object> lastProperties) {}

	@Override
	public void onPropertiesChanged(String path, String iface, Map<String, Object> changed, String[] invalidated) {
		if (Bluez.DEVICE.equals(iface))
			onAdvertisement(path, changed);
	}

	/* the adapter went away: AOSP stops all scans when Bluetooth goes off (onScanManagerErrorCallback is for
	 * scans that cannot start) */
	@Override
	public void onReset(boolean present) {
		if (!present) {
			synchronized (this) {
				for (Client c : clients.values())
					c.started = false;
			}
		}
		update();
	}
}
