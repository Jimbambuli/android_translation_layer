package android.bluetooth.le;

import android.os.IBinder;
import android.os.RemoteException;
import java.util.List;

/* ATL: the binder interface IAdvertisingSetCallback.aidl of AOSP 16 (packages/modules/Bluetooth android/app/aidl) as a plain Java
 * interface; its implementation runs in the app's process (com.android.bluetooth), there is no Stub/Proxy. */
public interface IAdvertisingSetCallback {
	void onAdvertisingSetStarted(IBinder advertiseBinder, int advertiserId, int tx_power, int status) throws RemoteException;
	void onOwnAddressRead(int advertiserId, int addressType, String address) throws RemoteException;
	void onAdvertisingSetStopped(int advertiserId) throws RemoteException;
	void onAdvertisingEnabled(int advertiserId, boolean enable, int status) throws RemoteException;
	void onAdvertisingDataSet(int advertiserId, int status) throws RemoteException;
	void onScanResponseDataSet(int advertiserId, int status) throws RemoteException;
	void onAdvertisingParametersUpdated(int advertiserId, int tx_power, int status) throws RemoteException;
	void onPeriodicAdvertisingParametersUpdated(int advertiserId, int status) throws RemoteException;
	void onPeriodicAdvertisingDataSet(int advertiserId, int status) throws RemoteException;
	void onPeriodicAdvertisingEnabled(int advertiserId, boolean enable, int status) throws RemoteException;
}
