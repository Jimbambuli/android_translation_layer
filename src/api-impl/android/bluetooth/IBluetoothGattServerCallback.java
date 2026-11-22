package android.bluetooth;

import android.os.RemoteException;
import java.util.List;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;

/* ATL: the binder interface IBluetoothGattServerCallback.aidl of AOSP 16 (packages/modules/Bluetooth android/app/aidl) as a plain Java
 * interface; its implementation runs in the app's process (com.android.bluetooth), there is no Stub/Proxy. */
public interface IBluetoothGattServerCallback {
	void onServerRegistered(int status) throws RemoteException;
	void onServerConnectionState(int status, boolean connected, BluetoothDevice device) throws RemoteException;
	void onServiceAdded(int status, BluetoothGattService service) throws RemoteException;
	void onCharacteristicReadRequest(BluetoothDevice device, int transId, int offset, boolean isLong, int handle) throws RemoteException;
	void onDescriptorReadRequest(BluetoothDevice device, int transId, int offset, boolean isLong, int handle) throws RemoteException;
	void onCharacteristicWriteRequest(BluetoothDevice device, int transId, int offset, int length, boolean isPrep, boolean needRsp, int handle, byte[] value) throws RemoteException;
	void onDescriptorWriteRequest(BluetoothDevice device, int transId, int offset, int length, boolean isPrep, boolean needRsp, int handle, byte[] value) throws RemoteException;
	void onExecuteWrite(BluetoothDevice device, int transId, boolean execWrite) throws RemoteException;
	void onNotificationSent(BluetoothDevice device, int status) throws RemoteException;
	void onMtuChanged(BluetoothDevice device, int mtu) throws RemoteException;
	void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) throws RemoteException;
	void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) throws RemoteException;
	void onConnectionUpdated(BluetoothDevice device, int interval, int latency, int timeout, int status) throws RemoteException;
	void onSubrateChange(BluetoothDevice device, int subrateMode, int status) throws RemoteException;
}
