package android.bluetooth;

import android.os.RemoteException;
import java.util.List;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.IAdvertisingSetCallback;
import android.bluetooth.le.IPeriodicAdvertisingCallback;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.bluetooth.IBluetoothGattServerCallback;
import android.content.AttributionSource;

/* ATL: the binder interface IBluetoothAdvertise.aidl of AOSP 16 (packages/modules/Bluetooth android/app/aidl) as a plain Java
 * interface; its implementation runs in the app's process (com.android.bluetooth), there is no Stub/Proxy. */
public interface IBluetoothAdvertise {
	void startAdvertisingSet(AdvertisingSetParameters parameters, AdvertiseData advertiseData, AdvertiseData scanResponse, PeriodicAdvertisingParameters periodicParameters, AdvertiseData periodicData, int duration, int maxExtAdvEvents, IBluetoothGattServerCallback gattServerCallback, IAdvertisingSetCallback callback, AttributionSource attributionSource) throws RemoteException;
	void stopAdvertisingSet(IAdvertisingSetCallback callback, AttributionSource attributionSource) throws RemoteException;
	void getOwnAddress(int advertiserId, AttributionSource attributionSource) throws RemoteException;
	void enableAdvertisingSet(int advertiserId, boolean enable, int duration, int maxExtAdvEvents, AttributionSource attributionSource) throws RemoteException;
	void setAdvertisingData(int advertiserId, AdvertiseData data, AttributionSource attributionSource) throws RemoteException;
	void setScanResponseData(int advertiserId, AdvertiseData data, AttributionSource attributionSource) throws RemoteException;
	void setAdvertisingParameters(int advertiserId, AdvertisingSetParameters parameters, AttributionSource attributionSource) throws RemoteException;
	void setPeriodicAdvertisingParameters(int advertiserId, PeriodicAdvertisingParameters parameters, AttributionSource attributionSource) throws RemoteException;
	void setPeriodicAdvertisingData(int advertiserId, AdvertiseData data, AttributionSource attributionSource) throws RemoteException;
	void setPeriodicAdvertisingEnable(int advertiserId, boolean enable, AttributionSource attributionSource) throws RemoteException;
}
