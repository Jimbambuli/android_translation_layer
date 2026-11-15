package android.bluetooth;

import android.os.IBinder;
import java.util.ArrayList;
import java.util.List;

/* AOSP BluetoothHeadset (modules/Bluetooth). ATL has no Bluetooth adapter and no IBluetoothHeadset service
 * (BluetoothAdapter.getProfileProxy() never delivers a proxy), so no instance can arise; the type carries
 * what AOSP does with getService() == null: empty list / false / STATE_DISCONNECTED. */
public final class BluetoothHeadset implements BluetoothProfile {
	private final BluetoothAdapter mAdapter;

	BluetoothHeadset(BluetoothAdapter adapter) {
		mAdapter = adapter;
	}

	/* AOSP getConnectedDevices(): no service -> Collections.emptyList() */
	@Override
	public List<BluetoothDevice> getConnectedDevices() {
		return new ArrayList<BluetoothDevice>();
	}

	/* AOSP getDevicesMatchingConnectionStates(): no service -> empty list */
	@Override
	public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
		return new ArrayList<BluetoothDevice>();
	}

	/* AOSP getConnectionState(): no service -> STATE_DISCONNECTED */
	@Override
	public int getConnectionState(BluetoothDevice device) {
		return STATE_DISCONNECTED;
	}

	/* AOSP isAudioConnected(): no service -> false */
	public boolean isAudioConnected(BluetoothDevice device) {
		return false;
	}

	/* AOSP onServiceConnected()/onServiceDisconnected(): there is no service binder to attach or detach */
	@Override
	public void onServiceConnected(IBinder service) {
	}

	@Override
	public void onServiceDisconnected() {
	}

	@Override
	public BluetoothAdapter getAdapter() {
		return mAdapter;
	}
}
