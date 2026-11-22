package android.bluetooth;

import android.os.RemoteException;
import java.util.List;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.IBluetoothGattCallback;
import android.bluetooth.IBluetoothGattServerCallback;
import android.content.AttributionSource;
import android.os.ParcelUuid;

/* ATL: the binder interface IBluetoothGatt.aidl of AOSP 16 (packages/modules/Bluetooth android/app/aidl) as a plain Java
 * interface; its implementation runs in the app's process (com.android.bluetooth), there is no Stub/Proxy. */
public interface IBluetoothGatt {
	List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states, AttributionSource attributionSource) throws RemoteException;
	void registerClient(ParcelUuid appId, IBluetoothGattCallback callback, boolean eatt_support, int transport, AttributionSource attributionSource) throws RemoteException;
	void unregisterClient(IBluetoothGattCallback callback, AttributionSource attributionSource) throws RemoteException;
	void clientConnect(IBluetoothGattCallback callback, BluetoothDevice device, int addressType, boolean isDirect, int transport, boolean opportunistic, int phy, AttributionSource attributionSource) throws RemoteException;
	void clientDisconnect(IBluetoothGattCallback callback, BluetoothDevice device, AttributionSource attributionSource) throws RemoteException;
	void clientSetPreferredPhy(IBluetoothGattCallback callback, BluetoothDevice device, int txPhy, int rxPhy, int phyOptions, AttributionSource attributionSource) throws RemoteException;
	void clientReadPhy(IBluetoothGattCallback callback, BluetoothDevice device, AttributionSource attributionSources) throws RemoteException;
	void refreshDevice(IBluetoothGattCallback callback, BluetoothDevice device, AttributionSource attributionSource) throws RemoteException;
	void discoverServices(IBluetoothGattCallback callback, BluetoothDevice device, AttributionSource attributionSource) throws RemoteException;
	void discoverServiceByUuid(IBluetoothGattCallback callback, BluetoothDevice device, ParcelUuid uuid, AttributionSource attributionSource) throws RemoteException;
	void readCharacteristic(IBluetoothGattCallback callback, BluetoothDevice device, int handle, int authReq, AttributionSource attributionSource) throws RemoteException;
	void readUsingCharacteristicUuid(IBluetoothGattCallback callback, BluetoothDevice device, ParcelUuid uuid, int startHandle, int endHandle, int authReq, AttributionSource attributionSource) throws RemoteException;
	int writeCharacteristic(IBluetoothGattCallback callback, BluetoothDevice device, int handle, int writeType, int authReq, byte[] value, AttributionSource attributionSource) throws RemoteException;
	void readDescriptor(IBluetoothGattCallback callback, BluetoothDevice device, int handle, int authReq, AttributionSource attributionSource) throws RemoteException;
	int writeDescriptor(IBluetoothGattCallback callback, BluetoothDevice device, int handle, int authReq, byte[] value, AttributionSource attributionSource) throws RemoteException;
	void registerForNotification(IBluetoothGattCallback callback, BluetoothDevice device, int handle, boolean enable, AttributionSource attributionSource) throws RemoteException;
	void beginReliableWrite(BluetoothDevice device, AttributionSource attributionSource) throws RemoteException;
	void endReliableWrite(IBluetoothGattCallback callback, BluetoothDevice device, boolean execute, AttributionSource attributionSource) throws RemoteException;
	void readRemoteRssi(IBluetoothGattCallback callback, BluetoothDevice device, AttributionSource attributionSource) throws RemoteException;
	void configureMTU(IBluetoothGattCallback callback, BluetoothDevice device, int mtu, AttributionSource attributionSource) throws RemoteException;
	void connectionParameterUpdate(IBluetoothGattCallback callback, BluetoothDevice device, int connectionPriority, AttributionSource attributionSource) throws RemoteException;
	void leConnectionUpdate(IBluetoothGattCallback callback, BluetoothDevice device, int minInterval, int maxInterval, int peripheralLatency, int supervisionTimeout, int minConnectionEventLen, int maxConnectionEventLen, AttributionSource attributionSource) throws RemoteException;
	void registerServer(ParcelUuid appId, IBluetoothGattServerCallback callback, boolean eatt_support, int transport, AttributionSource attributionSource) throws RemoteException;
	void unregisterServer(IBluetoothGattServerCallback callback, AttributionSource attributionSource) throws RemoteException;
	void serverConnect(IBluetoothGattServerCallback callback, BluetoothDevice device, int addressType, boolean isDirect, int transport, AttributionSource attributionSource) throws RemoteException;
	void serverDisconnect(IBluetoothGattServerCallback callback, BluetoothDevice device, AttributionSource attributionSource) throws RemoteException;
	void serverSetPreferredPhy(IBluetoothGattServerCallback callback, BluetoothDevice device, int txPhy, int rxPhy, int phyOptions, AttributionSource attributionSource) throws RemoteException;
	void serverReadPhy(IBluetoothGattServerCallback callback, BluetoothDevice device, AttributionSource attributionSource) throws RemoteException;
	void addService(IBluetoothGattServerCallback callback, BluetoothGattService service, AttributionSource attributionSource) throws RemoteException;
	void removeService(IBluetoothGattServerCallback callback, int handle, AttributionSource attributionSource) throws RemoteException;
	void clearServices(IBluetoothGattServerCallback callback, AttributionSource attributionSource) throws RemoteException;
	void sendResponse(IBluetoothGattServerCallback callback, BluetoothDevice device, int requestId, int status, int offset, byte[] value, AttributionSource attributionSource) throws RemoteException;
	int sendNotification(IBluetoothGattServerCallback callback, BluetoothDevice device, int handle, boolean confirm, byte[] value, AttributionSource attributionSource) throws RemoteException;
	void disconnectAll(AttributionSource attributionSource) throws RemoteException;
	int subrateModeRequest(IBluetoothGattCallback callback, BluetoothDevice device, int subrateMode, AttributionSource attributionSource) throws RemoteException;
}
