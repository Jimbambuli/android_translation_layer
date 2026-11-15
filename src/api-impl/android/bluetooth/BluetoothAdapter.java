package android.bluetooth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/* AOSP BluetoothAdapter (packages/modules/Bluetooth, android16-release). ATL has no Bluetooth stack: an adapter
 * that BluetoothManager hands out is one that stays off (getState() is AOSP's STATE_OFF for mService == null). */
public class BluetoothAdapter {
	public static final int ERROR = Integer.MIN_VALUE;
	public static final String ACTION_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED";
	public static final String EXTRA_STATE = "android.bluetooth.adapter.extra.STATE";
	public static final String EXTRA_PREVIOUS_STATE = "android.bluetooth.adapter.extra.PREVIOUS_STATE";
	public static final String ACTION_REQUEST_ENABLE = "android.bluetooth.adapter.action.REQUEST_ENABLE";
	public static final int STATE_OFF = 10;
	public static final int STATE_TURNING_ON = 11;
	public static final int STATE_ON = 12;
	public static final int STATE_TURNING_OFF = 13;
	private static final int ADDRESS_LENGTH = 17;

	public static BluetoothAdapter getDefaultAdapter() {
		return null;
	}

	public int getState() {
		return STATE_OFF;
	}

	public boolean isEnabled() {
		return getState() == STATE_ON;
	}

	public Set<BluetoothDevice> getBondedDevices() {
		if (getState() != STATE_ON) {
			return Collections.unmodifiableSet(new HashSet<BluetoothDevice>()); // toDeviceSet(Arrays.asList())
		}
		return null;
	}

	public BluetoothDevice getRemoteDevice(String address) {
		return new BluetoothDevice(address);
	}

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
						// hex character, OK
						break;
					}
					return false;
				case 2:
					if (c == ':') {
						break; // OK
					}
					return false;
			}
		}
		return true;
	}

	public static final int SCAN_MODE_NONE = 20;
	public static final int SCAN_MODE_CONNECTABLE = 21;
	public static final int SCAN_MODE_CONNECTABLE_DISCOVERABLE = 23;

	/* ATL's adapter has no BluetoothManagerService behind it: the manager's answers for a device whose Bluetooth
	 * cannot be switched on (no address or name known, enable()/disable() refused). */
	public boolean enable() {
		return isEnabled();
	}

	public boolean disable() {
		return false;
	}

	public String getAddress() {
		return null;
	}

	public String getName() {
		return null;
	}

	public BluetoothDevice getRemoteLeDevice(String address, int addressType) {
		return new BluetoothDevice(address, addressType);
	}

	/* the branches of the AOSP methods for getState() != STATE_ON */
	public boolean startDiscovery() {
		return false;
	}

	public boolean cancelDiscovery() {
		return false;
	}

	public boolean isDiscovering() {
		return false;
	}

	public boolean setName(String name) {
		return false;
	}

	public int getScanMode() {
		return SCAN_MODE_NONE;
	}

	public int getProfileConnectionState(int profile) {
		return BluetoothProfile.STATE_DISCONNECTED;
	}

	/* getLeAccess() is false with the adapter off */
	public android.bluetooth.le.BluetoothLeScanner getBluetoothLeScanner() {
		return null;
	}

	public android.bluetooth.le.BluetoothLeAdvertiser getBluetoothLeAdvertiser() {
		return null;
	}

	public boolean isOffloadedFilteringSupported() {
		return false;
	}

	public boolean isOffloadedScanBatchingSupported() {
		return false;
	}

	public interface LeScanCallback {
		void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);
	}

	/* getBluetoothLeScanner() == null -> false (null callback -> false first) */
	@Deprecated
	public boolean startLeScan(LeScanCallback callback) {
		return false;
	}

	@Deprecated
	public boolean startLeScan(java.util.UUID[] serviceUuids, LeScanCallback callback) {
		return false;
	}

	@Deprecated
	public void stopLeScan(LeScanCallback callback) {}

	/* with the service down the proxy is created but never connected: true for a profile in AOSP's
	 * PROFILE_CONSTRUCTORS (HEALTH, an unknown profile and an unsupported hearing aid give false) */
	public boolean getProfileProxy(android.content.Context context, BluetoothProfile.ServiceListener listener, int profile) {
		if (context == null || listener == null) {
			return false;
		}
		switch (profile) {
			case BluetoothProfile.HEADSET:
			case BluetoothProfile.A2DP:
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
				return false;
		}
	}

	public void closeProfileProxy(int unusedProfile, BluetoothProfile proxy) {
		if (proxy == null) {
			return;
		}
		closeProfileProxy(proxy);
	}

	public void closeProfileProxy(BluetoothProfile proxy) {
		if (proxy instanceof BluetoothGatt) {
			((BluetoothGatt) proxy).close();
		}
	}

	/* bindListen() fails with -1 while the Bluetooth service is off */
	public BluetoothServerSocket listenUsingRfcommWithServiceRecord(String name, java.util.UUID uuid) throws java.io.IOException {
		throw new java.io.IOException("Error: -1");
	}

	public BluetoothServerSocket listenUsingInsecureRfcommWithServiceRecord(String name, java.util.UUID uuid)
	    throws java.io.IOException {
		throw new java.io.IOException("Error: -1");
	}
}
