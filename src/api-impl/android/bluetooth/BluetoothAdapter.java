package android.bluetooth;

import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.AttributionSource;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.gatt.AdvertiseManager;
import com.android.bluetooth.gatt.GattService;
import com.android.bluetooth.le_scan.ScanController;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/* AOSP BluetoothAdapter (packages/modules/Bluetooth framework, android-16.0.0_r4). ATL's Bluetooth service is
 * com.android.bluetooth.btservice.AdapterService in the app's process on top of BlueZ; getDefaultAdapter() is null
 * on a desktop without BlueZ or without an adapter, as on a device without Bluetooth. The calls into the service are
 * the ones the AOSP methods make (IBluetooth, IBluetoothManager, IBluetoothScan, IBluetoothGatt), the permission
 * checks happen there. */
public final class BluetoothAdapter {
	private static final String TAG = "BluetoothAdapter";

	public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED";
	public static final String ACTION_DISCOVERY_FINISHED = "android.bluetooth.adapter.action.DISCOVERY_FINISHED";
	public static final String ACTION_DISCOVERY_STARTED = "android.bluetooth.adapter.action.DISCOVERY_STARTED";
	public static final String ACTION_LOCAL_NAME_CHANGED = "android.bluetooth.adapter.action.LOCAL_NAME_CHANGED";
	public static final String ACTION_REQUEST_DISCOVERABLE = "android.bluetooth.adapter.action.REQUEST_DISCOVERABLE";
	public static final String ACTION_REQUEST_ENABLE = "android.bluetooth.adapter.action.REQUEST_ENABLE";
	public static final String ACTION_SCAN_MODE_CHANGED = "android.bluetooth.adapter.action.SCAN_MODE_CHANGED";
	public static final String ACTION_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED";
	public static final int ERROR = Integer.MIN_VALUE;
	public static final String EXTRA_CONNECTION_STATE = "android.bluetooth.adapter.extra.CONNECTION_STATE";
	public static final String EXTRA_DISCOVERABLE_DURATION = "android.bluetooth.adapter.extra.DISCOVERABLE_DURATION";
	public static final String EXTRA_LOCAL_NAME = "android.bluetooth.adapter.extra.LOCAL_NAME";
	public static final String EXTRA_PREVIOUS_CONNECTION_STATE = "android.bluetooth.adapter.extra.PREVIOUS_CONNECTION_STATE";
	public static final String EXTRA_PREVIOUS_SCAN_MODE = "android.bluetooth.adapter.extra.PREVIOUS_SCAN_MODE";
	public static final String EXTRA_PREVIOUS_STATE = "android.bluetooth.adapter.extra.PREVIOUS_STATE";
	public static final String EXTRA_SCAN_MODE = "android.bluetooth.adapter.extra.SCAN_MODE";
	public static final String EXTRA_STATE = "android.bluetooth.adapter.extra.STATE";
	public static final int SCAN_MODE_CONNECTABLE = 21;
	public static final int SCAN_MODE_CONNECTABLE_DISCOVERABLE = 23;
	public static final int SCAN_MODE_NONE = 20;
	public static final int STATE_CONNECTED = 2;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_DISCONNECTED = 0;
	public static final int STATE_DISCONNECTING = 3;
	public static final int STATE_OFF = 10;
	public static final int STATE_ON = 12;
	public static final int STATE_TURNING_OFF = 13;
	public static final int STATE_TURNING_ON = 11;
	/** @hide */
	public static final int STATE_BLE_TURNING_ON = 14;
	/** @hide */
	public static final int STATE_BLE_ON = 15;
	/** @hide */
	public static final int STATE_BLE_TURNING_OFF = 16;
	/** @hide */
	public static final String BLUETOOTH_MANAGER_SERVICE = "bluetooth_manager";

	private static final int ADDRESS_LENGTH = 17;

	private static BluetoothAdapter sAdapter;

	private final AttributionSource mAttributionSource;
	private final Object mLock = new Object();
	private BluetoothLeScanner mBluetoothLeScanner;
	private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
	private final Map<LeScanCallback, ScanCallback> mLeScanClients = new HashMap<>();
	private final Handler mMainHandler = new Handler(Looper.getMainLooper());

	BluetoothAdapter() {
		/* ATL: Context.getAttributionSource() is null; the in-process services do not read it */
		mAttributionSource = AdapterService.context().getAttributionSource();
	}

	/* null without Bluetooth (BluetoothManager service missing in AOSP) */
	public static synchronized BluetoothAdapter getDefaultAdapter() {
		if (sAdapter == null) {
			if (!AdapterService.isAvailable())
				return null;
			sAdapter = new BluetoothAdapter();
		}
		return sAdapter;
	}

	/** @hide */
	public static BluetoothAdapter createAdapter(AttributionSource attributionSource) {
		return getDefaultAdapter();
	}

	private static AdapterService service() {
		return AdapterService.get();
	}

	/** @hide */
	public AttributionSource getAttributionSource() {
		return mAttributionSource;
	}

	/** @hide */
	public IBluetoothScan getBluetoothScan() {
		return ScanController.get();
	}

	/** @hide */
	public IBluetoothGatt getBluetoothGatt() {
		return GattService.get();
	}

	/** @hide */
	public IBluetoothAdvertise getBluetoothAdvertise() {
		return AdvertiseManager.get();
	}

	public BluetoothDevice getRemoteDevice(String address) {
		final BluetoothDevice res = new BluetoothDevice(address);
		res.setAttributionSource(mAttributionSource);
		return res;
	}

	public BluetoothDevice getRemoteLeDevice(String address, int addressType) {
		final BluetoothDevice res = new BluetoothDevice(address, addressType);
		res.setAttributionSource(mAttributionSource);
		return res;
	}

	public BluetoothDevice getRemoteDevice(byte[] address) {
		if (address == null || address.length != 6) {
			throw new IllegalArgumentException("Bluetooth address must have 6 bytes");
		}
		final BluetoothDevice res = new BluetoothDevice(String.format(java.util.Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X", address[0], address[1],
		                                                              address[2], address[3], address[4], address[5]));
		res.setAttributionSource(mAttributionSource);
		return res;
	}

	public BluetoothLeAdvertiser getBluetoothLeAdvertiser() {
		if (!getLeAccess()) {
			return null;
		}
		synchronized (mLock) {
			if (mBluetoothLeAdvertiser == null) {
				mBluetoothLeAdvertiser = new BluetoothLeAdvertiser(this);
			}
			return mBluetoothLeAdvertiser;
		}
	}

	public BluetoothLeScanner getBluetoothLeScanner() {
		if (!getLeAccess()) {
			return null;
		}
		synchronized (mLock) {
			if (mBluetoothLeScanner == null) {
				mBluetoothLeScanner = new BluetoothLeScanner(this);
			}
			return mBluetoothLeScanner;
		}
	}

	public boolean isEnabled() {
		return getState() == STATE_ON;
	}

	/* BlueZ has no LE-only state: the LE state is the adapter state */
	public boolean isLeEnabled() {
		final int state = getLeState();
		return state == STATE_ON || state == STATE_BLE_ON || state == STATE_TURNING_ON || state == STATE_TURNING_OFF;
	}

	/** @hide */
	public int getLeState() {
		return getState();
	}

	boolean getLeAccess() {
		int state = getLeState();
		return state == STATE_ON || state == STATE_BLE_ON || state == STATE_TURNING_OFF || state == STATE_TURNING_ON;
	}

	public int getState() {
		return service().getState();
	}

	/* BluetoothManagerService.enable(): refused for apps targeting T (RESTRICT_ENABLE_DISABLE) */
	public boolean enable() {
		if (isEnabled()) {
			Log.d(TAG, "enable(): BT already enabled!");
			return true;
		}
		return service().setPowered(true, "enable");
	}

	public boolean disable() {
		return service().setPowered(false, "disable");
	}

	public String getAddress() {
		return service().getAddress();
	}

	public String getName() {
		return service().getName();
	}

	/** @hide */
	public int getNameLengthForAdvertise() {
		if (getState() != STATE_ON)
			return -1;
		String name = service().getName();
		return name == null ? -1 : name.length();
	}

	public boolean setName(String name) {
		if (getState() != STATE_ON)
			return false;
		return service().setName(name);
	}

	public int getScanMode() {
		if (getState() != STATE_ON)
			return SCAN_MODE_NONE;
		return service().getScanMode();
	}

	public Duration getDiscoverableTimeout() {
		if (getState() != STATE_ON)
			return null;
		return Duration.ofSeconds(service().getDiscoverableTimeoutSeconds());
	}

	public boolean startDiscovery() {
		if (getState() != STATE_ON)
			return false;
		return service().startDiscovery();
	}

	public boolean cancelDiscovery() {
		if (getState() != STATE_ON)
			return false;
		return service().cancelDiscovery();
	}

	public boolean isDiscovering() {
		if (getState() != STATE_ON)
			return false;
		return service().isDiscovering();
	}

	public Set<BluetoothDevice> getBondedDevices() {
		if (getState() != STATE_ON) {
			return Collections.unmodifiableSet(new HashSet<BluetoothDevice>());
		}
		if (!AdapterService.checkConnectPermission(TAG, "getBondedDevices"))
			return Collections.unmodifiableSet(new HashSet<BluetoothDevice>());
		HashSet<BluetoothDevice> set = new HashSet<>();
		for (String address : service().bondedAddresses())
			set.add(getRemoteDevice(address));
		return Collections.unmodifiableSet(set);
	}

	/* AdapterService.getProfileConnectionState(): the most connected state over the devices that have the profile */
	public int getProfileConnectionState(int profile) {
		if (getState() != STATE_ON)
			return STATE_DISCONNECTED;
		if (!AdapterService.checkConnectPermission(TAG, "getProfileConnectionState"))
			return STATE_DISCONNECTED;
		return service().getProfileConnectionState(profile);
	}

	/* ---- capabilities (AdapterProperties from the controller; here BlueZ's LEAdvertisingManager1) ---- */

	public boolean isMultipleAdvertisementSupported() {
		if (getState() != STATE_ON)
			return false;
		/* AdapterServiceBinder: MIN_ADVT_INSTANCES_FOR_MA = 5 */
		return AdvertiseManager.get().numOfAdvertisementInstances() >= 5;
	}

	/* BlueZ does not filter in the controller: ScanController filters in software, as AOSP without offload */
	public boolean isOffloadedFilteringSupported() {
		return false;
	}

	public boolean isOffloadedScanBatchingSupported() {
		return false;
	}

	/** @hide */
	public boolean isHardwareTrackingFiltersAvailable() {
		if (!getLeAccess()) {
			return false;
		}
		try {
			IBluetoothScan scan = getBluetoothScan();
			if (scan == null) {
				return false;
			}
			return scan.numHwTrackFiltersAvailable(mAttributionSource) != 0;
		} catch (android.os.RemoteException e) {
			return false;
		}
	}

	public boolean isLe2MPhySupported() {
		return getState() == STATE_ON && AdvertiseManager.get().secondaryChannels().contains("2M");
	}

	public boolean isLeCodedPhySupported() {
		return getState() == STATE_ON && AdvertiseManager.get().secondaryChannels().contains("Coded");
	}

	/* a secondary channel exists only with extended advertising */
	public boolean isLeExtendedAdvertisingSupported() {
		return getState() == STATE_ON && !AdvertiseManager.get().secondaryChannels().isEmpty();
	}

	/* BlueZ has no periodic advertising over D-Bus */
	public boolean isLePeriodicAdvertisingSupported() {
		return false;
	}

	public int getLeMaximumAdvertisingDataLength() {
		if (getState() != STATE_ON)
			return 0;
		return AdvertiseManager.get().maxAdvertisingDataLength();
	}

	/* LE Audio needs isochronous channels; BlueZ's BAP is not reachable for apps */
	public int isLeAudioSupported() {
		return getState() == STATE_ON ? BluetoothStatusCodes.FEATURE_NOT_SUPPORTED : BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
	}

	public int isLeAudioBroadcastSourceSupported() {
		return getState() == STATE_ON ? BluetoothStatusCodes.FEATURE_NOT_SUPPORTED : BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
	}

	public int isLeAudioBroadcastAssistantSupported() {
		return getState() == STATE_ON ? BluetoothStatusCodes.FEATURE_NOT_SUPPORTED : BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
	}

	/* AdapterProperties: config_bluetooth_max_connected_audio_devices (5) */
	public int getMaxConnectedAudioDevices() {
		if (getState() != STATE_ON)
			return -1;
		if (!AdapterService.checkConnectPermission(TAG, "getMaxConnectedAudioDevices"))
			return -1;
		return 5;
	}

	public int getOffloadedTransportDiscoveryDataScanSupported() {
		if (!getLeAccess())
			return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
		return BluetoothStatusCodes.FEATURE_NOT_SUPPORTED;
	}

	/* ---- deprecated LE scan (AOSP code) ---- */

	public interface LeScanCallback {
		void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);
	}

	@Deprecated
	public boolean startLeScan(LeScanCallback callback) {
		return startLeScan(null, callback);
	}

	@Deprecated
	public boolean startLeScan(final UUID[] serviceUuids, final LeScanCallback callback) {
		Log.d(TAG, "startLeScan(): " + Arrays.toString(serviceUuids));
		if (callback == null) {
			Log.e(TAG, "startLeScan: null callback");
			return false;
		}
		BluetoothLeScanner scanner = getBluetoothLeScanner();
		if (scanner == null) {
			Log.e(TAG, "startLeScan: cannot get BluetoothLeScanner");
			return false;
		}

		synchronized (mLeScanClients) {
			if (mLeScanClients.containsKey(callback)) {
				Log.e(TAG, "LE Scan has already started");
				return false;
			}

			ScanCallback scanCallback = new ScanCallback() {
				@Override
				public void onScanResult(int callbackType, ScanResult result) {
					if (callbackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
						Log.e(TAG, "LE Scan has already started");
						return;
					}
					ScanRecord scanRecord = result.getScanRecord();
					if (scanRecord == null) {
						return;
					}
					if (serviceUuids != null) {
						List<ParcelUuid> uuids = new ArrayList<ParcelUuid>();
						for (UUID uuid : serviceUuids) {
							uuids.add(new ParcelUuid(uuid));
						}
						List<ParcelUuid> scanServiceUuids = scanRecord.getServiceUuids();
						if (scanServiceUuids == null || !scanServiceUuids.containsAll(uuids)) {
							Log.d(TAG, "uuids does not match");
							return;
						}
					}
					callback.onLeScan(result.getDevice(), result.getRssi(), scanRecord.getBytes());
				}
			};
			ScanSettings settings = new ScanSettings.Builder()
			                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
			                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
			                            .build();

			List<ScanFilter> filters = new ArrayList<ScanFilter>();
			if (serviceUuids != null && serviceUuids.length > 0) {
				ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUuids[0])).build();
				filters.add(filter);
			}
			scanner.startScan(filters, settings, scanCallback);

			mLeScanClients.put(callback, scanCallback);
			return true;
		}
	}

	@Deprecated
	public void stopLeScan(LeScanCallback callback) {
		Log.d(TAG, "stopLeScan()");
		BluetoothLeScanner scanner = getBluetoothLeScanner();
		if (scanner == null) {
			return;
		}
		synchronized (mLeScanClients) {
			ScanCallback scanCallback = mLeScanClients.remove(callback);
			if (scanCallback == null) {
				Log.e(TAG, "scan not started yet");
				return;
			}
			scanner.stopScan(scanCallback);
		}
	}

	/* ---- profiles ---- */

	/* AOSP: a proxy for every profile in PROFILE_CONSTRUCTORS; it connects when the profile's service binds. With
	 * BlueZ, HEADSET and A2DP connect (their state comes from BlueZ's devices); the other profiles have no service
	 * on the desktop, their proxy stays unconnected like a disabled profile in AOSP. */
	public boolean getProfileProxy(final Context context, final BluetoothProfile.ServiceListener listener, final int profile) {
		if (context == null || listener == null) {
			return false;
		}
		if (context.getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.S && context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
			throw new SecurityException("Need BLUETOOTH permission");
		}
		if (profile == BluetoothProfile.HEALTH) {
			Log.e(TAG, "getProfileProxy(): BluetoothHealth is deprecated");
			return false;
		}
		if (profile == BluetoothProfile.HEARING_AID) {
			Log.e(TAG, "getProfileProxy(): BluetoothHearingAid is not supported");
			return false;
		}
		final BluetoothProfile proxy;
		switch (profile) {
			case BluetoothProfile.HEADSET:
				proxy = new BluetoothHeadset(context, this);
				break;
			case BluetoothProfile.A2DP:
				proxy = new BluetoothA2dp(context, this);
				break;
			case BluetoothProfile.A2DP_SINK:
			case BluetoothProfile.AVRCP_CONTROLLER:
			case BluetoothProfile.HID_HOST:
			case BluetoothProfile.PAN:
			case BluetoothProfile.PBAP:
			case BluetoothProfile.MAP:
			case BluetoothProfile.HEADSET_CLIENT:
			case BluetoothProfile.SAP:
			case BluetoothProfile.PBAP_CLIENT:
			case BluetoothProfile.MAP_CLIENT:
			case BluetoothProfile.HID_DEVICE:
			case BluetoothProfile.HAP_CLIENT:
			case BluetoothProfile.LE_AUDIO:
			case BluetoothProfile.LE_AUDIO_BROADCAST:
			case BluetoothProfile.VOLUME_CONTROL:
			case BluetoothProfile.CSIP_SET_COORDINATOR:
			case BluetoothProfile.LE_CALL_CONTROL:
			case BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT:
				return true;
			default:
				Log.e(TAG, "getProfileProxy(): Unknown profile " + profile);
				return false;
		}
		/* ProfileConnection.connect(): onServiceConnected on the main handler once the service is bound */
		if (getState() == STATE_ON) {
			mMainHandler.post(new Runnable() {
				public void run() {
					listener.onServiceConnected(profile, proxy);
				}
			});
		}
		return true;
	}

	public void closeProfileProxy(int unusedProfile, BluetoothProfile proxy) {
		if (proxy == null) {
			return;
		}
		closeProfileProxy(proxy);
	}

	/** @hide */
	public void closeProfileProxy(BluetoothProfile proxy) {
		if (proxy instanceof BluetoothGatt) {
			((BluetoothGatt)proxy).close();
		} else if (proxy instanceof BluetoothGattServer) {
			((BluetoothGattServer)proxy).close();
		}
	}

	/* ---- sockets ---- */

	public BluetoothServerSocket listenUsingRfcommWithServiceRecord(String name, UUID uuid) throws java.io.IOException {
		return BluetoothServerSocket.listenRfcomm(name, uuid, true);
	}

	public BluetoothServerSocket listenUsingInsecureRfcommWithServiceRecord(String name, UUID uuid) throws java.io.IOException {
		return BluetoothServerSocket.listenRfcomm(name, uuid, false);
	}

	/* LE credit-based channels need a PSM from the kernel's L2CAP socket, which ATL does not open */
	public BluetoothServerSocket listenUsingL2capChannel() throws java.io.IOException {
		throw new java.io.IOException("Error: -1");
	}

	public BluetoothServerSocket listenUsingInsecureL2capChannel() throws java.io.IOException {
		throw new java.io.IOException("Error: -1");
	}

	public BluetoothServerSocket listenUsingSocketSettings(BluetoothSocketSettings settings) throws java.io.IOException {
		if (settings.getSocketType() == BluetoothSocket.TYPE_RFCOMM) {
			return BluetoothServerSocket.listenRfcomm(settings.getRfcommServiceName(), settings.getRfcommUuid(),
			                                          settings.isAuthenticationRequired() || settings.isEncryptionRequired());
		}
		throw new java.io.IOException("Error: -1");
	}

	/* ---- ACTION_REQUEST_ENABLE / ACTION_REQUEST_DISCOVERABLE (Settings' RequestPermissionActivity) ---- */

	/** @hide ATL: Activity.startActivityForResult() hands these intents here. Already enabled: RESULT_OK at once, as
	 * RequestPermissionActivity does; otherwise the user did not agree (ATL has no dialog that switches the desktop's
	 * Bluetooth on): RESULT_CANCELED. Discoverable: the desktop decides, RESULT_CANCELED. */
	public static boolean handleRequestIntent(android.content.Intent intent, final java.util.function.IntConsumer result) {
		if (intent == null || intent.getComponent() != null)
			return false;
		String action = intent.getAction();
		if (!ACTION_REQUEST_ENABLE.equals(action) && !ACTION_REQUEST_DISCOVERABLE.equals(action))
			return false;
		BluetoothAdapter adapter = getDefaultAdapter();
		final boolean ok = ACTION_REQUEST_ENABLE.equals(action) && adapter != null && adapter.isEnabled();
		/* like a finished activity's result: after the caller has returned */
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				result.accept(ok ? android.app.Activity.RESULT_OK : android.app.Activity.RESULT_CANCELED);
			}
		});
		return true;
	}

	/* ---- static helpers (AOSP) ---- */

	public static boolean checkBluetoothAddress(String address) {
		if (address == null || address.length() != ADDRESS_LENGTH) {
			return false;
		}
		for (int i = 0; i < ADDRESS_LENGTH; i++) {
			char c = address.charAt(i);
			switch (i % 3) {
				case 0:
				case 1:
					if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) {
						break;
					}
					return false;
				case 2:
					if (c == ':') {
						break;
					}
					return false;
			}
		}
		return true;
	}

	/** @hide */
	public static boolean isAddressRandomStatic(String address) {
		java.util.Objects.requireNonNull(address);
		return checkBluetoothAddress(address) && (Integer.parseInt(address.split(":")[0], 16) & 0xC0) == 0xC0;
	}

	/** @hide */
	public static String nameForState(int state) {
		switch (state) {
			case STATE_OFF: return "OFF";
			case STATE_TURNING_ON: return "TURNING_ON";
			case STATE_ON: return "ON";
			case STATE_TURNING_OFF: return "TURNING_OFF";
			case STATE_BLE_TURNING_ON: return "BLE_TURNING_ON";
			case STATE_BLE_ON: return "BLE_ON";
			case STATE_BLE_TURNING_OFF: return "BLE_TURNING_OFF";
			default: return "?!?!? (" + state + ")";
		}
	}
}
