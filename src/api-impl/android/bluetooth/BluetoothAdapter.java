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
}
