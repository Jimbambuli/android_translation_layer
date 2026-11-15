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

	/* BluetoothManager.openGattServer(): argument check, then BluetoothAdapter.getBluetoothGatt() == null (no Bluetooth
	 * service) -> "Fail to get GATT Server connection", null */
	public BluetoothGattServer openGattServer(android.content.Context context, BluetoothGattServerCallback callback) {
		return openGattServer(context, callback, BluetoothDevice.TRANSPORT_AUTO);
	}

	public BluetoothGattServer openGattServer(android.content.Context context, BluetoothGattServerCallback callback,
	                                          boolean eattSupport) {
		return openGattServer(context, callback, BluetoothDevice.TRANSPORT_AUTO, eattSupport);
	}

	public BluetoothGattServer openGattServer(android.content.Context context, BluetoothGattServerCallback callback,
	                                          int transport) {
		return openGattServer(context, callback, transport, false);
	}

	public BluetoothGattServer openGattServer(android.content.Context context, BluetoothGattServerCallback callback,
	                                          int transport, boolean eattSupport) {
		if (context == null || callback == null) {
			throw new IllegalArgumentException("null parameter: " + context + " " + callback);
		}
		android.util.Log.e("BluetoothManager", "Fail to get GATT Server connection");
		return null;
	}
}
