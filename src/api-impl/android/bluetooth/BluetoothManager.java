package android.bluetooth;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/* AOSP BluetoothManager (packages/modules/Bluetooth framework, android-16.0.0_r4). getAdapter() is null without a
 * Bluetooth adapter on the desktop. GATT servers: ATL's GattService has none (see com.android.bluetooth.gatt), so
 * openGattServer() takes AOSP's branch for a missing Gatt service. */
public final class BluetoothManager {
	private static final String TAG = "BluetoothManager";

	public BluetoothManager() {}

	/** @hide */
	public BluetoothManager(Context context) {}

	public BluetoothAdapter getAdapter() {
		return BluetoothAdapter.getDefaultAdapter();
	}

	public int getConnectionState(BluetoothDevice device, int profile) {
		List<BluetoothDevice> connectedDevices = getConnectedDevices(profile);
		for (BluetoothDevice connectedDevice : connectedDevices) {
			if (device.equals(connectedDevice)) {
				return BluetoothProfile.STATE_CONNECTED;
			}
		}
		return BluetoothProfile.STATE_DISCONNECTED;
	}

	public List<BluetoothDevice> getConnectedDevices(int profile) {
		return getDevicesMatchingConnectionStates(profile, new int[] {BluetoothProfile.STATE_CONNECTED});
	}

	public List<BluetoothDevice> getDevicesMatchingConnectionStates(int profile, int[] states) {
		if (profile != BluetoothProfile.GATT && profile != BluetoothProfile.GATT_SERVER) {
			throw new IllegalArgumentException("Profile not supported: " + profile);
		}
		List<BluetoothDevice> devices = new ArrayList<>();
		BluetoothAdapter adapter = getAdapter();
		IBluetoothGatt iGatt = adapter == null ? null : adapter.getBluetoothGatt();
		if (iGatt == null) {
			return devices;
		}
		try {
			devices = iGatt.getDevicesMatchingConnectionStates(states, null);
		} catch (RemoteException e) {
			Log.e(TAG, "", e);
		}
		return devices;
	}

	public BluetoothGattServer openGattServer(Context context, BluetoothGattServerCallback callback) {
		return openGattServer(context, callback, BluetoothDevice.TRANSPORT_AUTO);
	}

	/** @hide */
	public BluetoothGattServer openGattServer(Context context, BluetoothGattServerCallback callback, boolean eattSupport) {
		return openGattServer(context, callback, BluetoothDevice.TRANSPORT_AUTO, eattSupport);
	}

	/** @hide */
	public BluetoothGattServer openGattServer(Context context, BluetoothGattServerCallback callback, int transport) {
		return openGattServer(context, callback, transport, false);
	}

	/** @hide */
	public BluetoothGattServer openGattServer(Context context, BluetoothGattServerCallback callback, int transport, boolean eattSupport) {
		if (context == null || callback == null) {
			throw new IllegalArgumentException("null parameter: " + context + " " + callback);
		}
		Log.e(TAG, "Fail to get GATT Server connection");
		return null;
	}
}
