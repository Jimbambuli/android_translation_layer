package android.bluetooth;

import android.os.IBinder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/* AOSP BluetoothGatt (packages/modules/Bluetooth, android16-release) in the state it has without a Gatt service
 * (mService == null, mClientIf == 0): every operation that needs the service returns false / does nothing, the
 * service list is empty. BluetoothDevice.connectGatt() returns null in ATL (iGatt == null: "BLE is not supported"),
 * so apps normally never get an object; the class exists for apps that name it in fields, callbacks and casts. */
public final class BluetoothGatt implements BluetoothProfile {
	public static final int GATT_SUCCESS = 0;
	public static final int GATT_READ_NOT_PERMITTED = 0x2;
	public static final int GATT_WRITE_NOT_PERMITTED = 0x3;
	public static final int GATT_INSUFFICIENT_AUTHENTICATION = 0x5;
	public static final int GATT_REQUEST_NOT_SUPPORTED = 0x6;
	public static final int GATT_INSUFFICIENT_ENCRYPTION = 0xf;
	public static final int GATT_INVALID_OFFSET = 0x7;
	public static final int GATT_INSUFFICIENT_AUTHORIZATION = 0x8;
	public static final int GATT_INVALID_ATTRIBUTE_LENGTH = 0xd;
	public static final int GATT_CONNECTION_CONGESTED = 0x8f;
	public static final int GATT_CONNECTION_TIMEOUT = 0x93;
	public static final int GATT_FAILURE = 0x101;
	public static final int CONNECTION_PRIORITY_BALANCED = 0;
	public static final int CONNECTION_PRIORITY_HIGH = 1;
	public static final int CONNECTION_PRIORITY_LOW_POWER = 2;
	public static final int CONNECTION_PRIORITY_DCK = 3;

	private static final int GATT_MAX_ATTR_LEN = 600;

	private final BluetoothDevice mDevice;

	BluetoothGatt(BluetoothDevice device) {
		mDevice = device;
	}

	@Override
	public void onServiceConnected(IBinder service) {}

	@Override
	public void onServiceDisconnected() {}

	@Override
	public BluetoothAdapter getAdapter() {
		return BluetoothAdapter.getDefaultAdapter();
	}

	public void close() {}

	public void disconnect() {}

	public boolean connect() {
		return false;
	}

	public void setPreferredPhy(int txPhy, int rxPhy, int phyOptions) {}

	public void readPhy() {}

	public BluetoothDevice getDevice() {
		return mDevice;
	}

	public boolean discoverServices() {
		return false;
	}

	public boolean discoverServiceByUuid(UUID uuid) {
		return false;
	}

	public List<BluetoothGattService> getServices() {
		return new ArrayList<BluetoothGattService>();
	}

	public BluetoothGattService getService(UUID uuid) {
		return null;
	}

	public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
		return false;
	}

	public boolean readUsingCharacteristicUuid(UUID uuid, int startHandle, int endHandle) {
		return false;
	}

	public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		return false;
	}

	public int writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value, int writeType) {
		if (characteristic == null) {
			throw new IllegalArgumentException("characteristic must not be null");
		}
		if (value == null) {
			throw new IllegalArgumentException("value must not be null");
		}
		if (value.length > GATT_MAX_ATTR_LEN) {
			throw new IllegalArgumentException("value should not be longer than max length of an attribute value");
		}
		if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
		    && (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
			return 200; // BluetoothStatusCodes.ERROR_GATT_WRITE_NOT_ALLOWED
		}
		return 9; // BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND
	}

	public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
		return false;
	}

	public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
		return false;
	}

	/* AOSP BluetoothGatt.writeDescriptor(descriptor, value): argument checks, then ERROR_PROFILE_SERVICE_NOT_BOUND
	 * since the GATT client is never registered (no Bluetooth service) */
	public int writeDescriptor(BluetoothGattDescriptor descriptor, byte[] value) {
		if (descriptor == null) {
			throw new IllegalArgumentException("descriptor must not be null");
		}
		if (value == null) {
			throw new IllegalArgumentException("value must not be null");
		}
		return 9; // BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND
	}

	public boolean beginReliableWrite() {
		return false;
	}

	public boolean executeReliableWrite() {
		return false;
	}

	public void abortReliableWrite() {}

	public void abortReliableWrite(BluetoothDevice mDevice) {}

	public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
		return false;
	}

	public boolean refresh() {
		return false;
	}

	public boolean readRemoteRssi() {
		return false;
	}

	public boolean requestMtu(int mtu) {
		return false;
	}

	public boolean requestConnectionPriority(int connectionPriority) {
		if (connectionPriority < CONNECTION_PRIORITY_BALANCED || connectionPriority > CONNECTION_PRIORITY_DCK) {
			throw new IllegalArgumentException("connectionPriority not within valid range");
		}
		return false;
	}

	@Override
	public int getConnectionState(BluetoothDevice device) {
		throw new UnsupportedOperationException("Use BluetoothManager#getConnectionState instead.");
	}

	@Override
	public List<BluetoothDevice> getConnectedDevices() {
		throw new UnsupportedOperationException("Use BluetoothManager#getConnectedDevices instead.");
	}

	@Override
	public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
		throw new UnsupportedOperationException("Use BluetoothManager#getDevicesMatchingConnectionStates instead.");
	}
}
