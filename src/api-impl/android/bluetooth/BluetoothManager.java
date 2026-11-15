package android.bluetooth;

public class BluetoothManager {
	public BluetoothAdapter getAdapter() {
		return new BluetoothAdapter();
	}

	/* BluetoothManager.getDevicesMatchingConnectionStates(): GATT and GATT_SERVER only; no IBluetoothGatt, no devices */
	public java.util.List<BluetoothDevice> getDevicesMatchingConnectionStates(int profile, int[] states) {
		if (profile != BluetoothProfile.GATT && profile != BluetoothProfile.GATT_SERVER) {
			throw new IllegalArgumentException("Profile not supported: " + profile);
		}
		return new java.util.ArrayList<BluetoothDevice>();
	}

	public java.util.List<BluetoothDevice> getConnectedDevices(int profile) {
		return getDevicesMatchingConnectionStates(profile, new int[] {BluetoothProfile.STATE_CONNECTED});
	}

	public int getConnectionState(BluetoothDevice device, int profile) {
		for (BluetoothDevice connectedDevice : getConnectedDevices(profile)) {
			if (device.equals(connectedDevice)) {
				return BluetoothProfile.STATE_CONNECTED;
			}
		}
		return BluetoothProfile.STATE_DISCONNECTED;
	}
}
