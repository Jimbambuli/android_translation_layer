package android.bluetooth.le;

import android.os.RemoteException;
import java.util.List;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.PeriodicAdvertisingReport;

/* ATL: the binder interface IPeriodicAdvertisingCallback.aidl of AOSP 16 (packages/modules/Bluetooth android/app/aidl) as a plain Java
 * interface; its implementation runs in the app's process (com.android.bluetooth), there is no Stub/Proxy. */
public interface IPeriodicAdvertisingCallback {
	void onSyncEstablished(int syncHandle, BluetoothDevice device, int advertisingSid, int skip, int timeout, int status) throws RemoteException;
	void onPeriodicAdvertisingReport(PeriodicAdvertisingReport report) throws RemoteException;
	void onSyncLost(int syncHandle) throws RemoteException;
	void onSyncTransferred(BluetoothDevice device, int status) throws RemoteException;
	void onBigInfoAdvertisingReport(int syncHandle, boolean encrypted) throws RemoteException;
}
