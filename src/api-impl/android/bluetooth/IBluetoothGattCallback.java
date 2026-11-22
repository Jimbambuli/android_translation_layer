package android.bluetooth;

import android.os.RemoteException;
import java.util.List;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;

/* ATL: the binder interface IBluetoothGattCallback.aidl of AOSP 16 (packages/modules/Bluetooth android/app/aidl) as a plain Java
 * interface; its implementation runs in the app's process (com.android.bluetooth), there is no Stub/Proxy. */
public interface IBluetoothGattCallback {
	void onClientRegistered(int status) throws RemoteException;
	void onClientConnectionState(int status, boolean connected, BluetoothDevice device) throws RemoteException;
	void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) throws RemoteException;
	void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) throws RemoteException;
	void onSearchComplete(BluetoothDevice device, List<BluetoothGattService> services, int status) throws RemoteException;
	void onCharacteristicRead(BluetoothDevice device, int status, int handle, byte[] value) throws RemoteException;
	void onCharacteristicWrite(BluetoothDevice device, int status, int handle, byte[] value) throws RemoteException;
	void onExecuteWrite(BluetoothDevice device, int status) throws RemoteException;
	void onDescriptorRead(BluetoothDevice device, int status, int handle, byte[] value) throws RemoteException;
	void onDescriptorWrite(BluetoothDevice device, int status, int handle, byte[] value) throws RemoteException;
	void onNotify(BluetoothDevice device, int handle, byte[] value) throws RemoteException;
	void onReadRemoteRssi(BluetoothDevice device, int rssi, int status) throws RemoteException;
	void onConfigureMTU(BluetoothDevice device, int mtu, int status) throws RemoteException;
	void onConnectionUpdated(BluetoothDevice device, int interval, int latency, int timeout, int status) throws RemoteException;
	void onServiceChanged(BluetoothDevice device) throws RemoteException;
	void onSubrateChange(BluetoothDevice device, int subrateMode, int status) throws RemoteException;
}
