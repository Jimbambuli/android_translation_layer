package android.bluetooth.le;

import android.os.RemoteException;
import java.util.List;
import android.bluetooth.le.ScanResult;

/* ATL: the binder interface IScannerCallback.aidl of AOSP 16 (packages/modules/Bluetooth android/app/aidl) as a plain Java
 * interface; its implementation runs in the app's process (com.android.bluetooth), there is no Stub/Proxy. */
public interface IScannerCallback {
	void onScannerRegistered(int status, int scannerId) throws RemoteException;
	void onScanResult(ScanResult scanResult) throws RemoteException;
	void onBatchScanResults(List<ScanResult> batchResults) throws RemoteException;
	void onFoundOrLost(boolean onFound, ScanResult scanResult) throws RemoteException;
	void onScanManagerErrorCallback(int errorCode) throws RemoteException;
}
