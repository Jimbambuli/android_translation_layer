package android.bluetooth;

import android.os.RemoteException;
import java.util.List;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanResult;
import android.content.AttributionSource;
import android.os.WorkSource;
import android.bluetooth.le.IScannerCallback;
import android.bluetooth.le.IPeriodicAdvertisingCallback;

/* ATL: the binder interface IBluetoothScan.aidl of AOSP 16 (packages/modules/Bluetooth android/app/aidl) as a plain Java
 * interface; its implementation runs in the app's process (com.android.bluetooth), there is no Stub/Proxy. */
public interface IBluetoothScan {
	void registerScanner(IScannerCallback callback, WorkSource workSource, AttributionSource attributionSource) throws RemoteException;
	void unregisterScanner(int scannerId, AttributionSource attributionSource) throws RemoteException;
	void startScan(int scannerId, ScanSettings settings, List<ScanFilter> filters, AttributionSource attributionSource) throws RemoteException;
	void registerPiAndStartScan(PendingIntent intent, ScanSettings settings, List<ScanFilter> filters, AttributionSource attributionSource) throws RemoteException;
	void stopScan(int scannerId, AttributionSource attributionSource) throws RemoteException;
	void stopScanForIntent(PendingIntent intent, AttributionSource attributionSource) throws RemoteException;
	void flushPendingBatchResults(int scannerId, AttributionSource attributionSource) throws RemoteException;
	void registerSync(ScanResult scanResult, int skip, int timeout, IPeriodicAdvertisingCallback callback, AttributionSource attributionSource) throws RemoteException;
	void unregisterSync(IPeriodicAdvertisingCallback callback, AttributionSource attributionSource) throws RemoteException;
	void transferSync(BluetoothDevice bda, int serviceData, int syncHandle, AttributionSource attributionSource) throws RemoteException;
	void transferSetInfo(BluetoothDevice bda, int serviceData, int advertisingHandle, IPeriodicAdvertisingCallback callback, AttributionSource attributionSource) throws RemoteException;
	int numHwTrackFiltersAvailable(AttributionSource attributionSource) throws RemoteException;
}
