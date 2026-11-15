package android.bluetooth;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/* BluetoothManager.openGattServer() returns null (BluetoothAdapter.getBluetoothGatt() is null without a Bluetooth
 * service), so no instance exists; the methods are here for linking and follow AOSP BluetoothGattServer for the
 * state "mService == null": no registered server, nothing connected, no services. */
public final class BluetoothGattServer implements BluetoothProfile {
	private static final String TAG = "BluetoothGattServer";

	BluetoothGattServer() {}

	/* AOSP BluetoothGattServer: the BluetoothProfile proxy hooks are empty, getAdapter() is the adapter that opened
	 * the server (as BluetoothGatt here: the default adapter) */
	@Override
	public void onServiceConnected(android.os.IBinder service) {}

	@Override
	public void onServiceDisconnected() {}

	@Override
	public BluetoothAdapter getAdapter() {
		return BluetoothAdapter.getDefaultAdapter();
	}

	public void close() {
		Log.d(TAG, "close()");
	}

	public boolean connect(BluetoothDevice device, boolean autoConnect) {
		Log.d(TAG, "connect() - device: " + device + ", auto: " + autoConnect);
		return false;
	}

	public void cancelConnection(BluetoothDevice device) {
		Log.d(TAG, "cancelConnection() - device: " + device);
	}

	public void setPreferredPhy(BluetoothDevice device, int txPhy, int rxPhy, int phyOptions) {}

	public void readPhy(BluetoothDevice device) {}

	public boolean sendResponse(BluetoothDevice device, int requestId, int status, int offset, byte[] value) {
		return false;
	}

	public boolean addService(BluetoothGattService service) {
		return false;
	}

	public boolean removeService(BluetoothGattService service) {
		return false;
	}

	public void clearServices() {}

	public List<BluetoothGattService> getServices() {
		return new ArrayList<BluetoothGattService>();
	}

	public BluetoothGattService getService(UUID uuid) {
		return null;
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
