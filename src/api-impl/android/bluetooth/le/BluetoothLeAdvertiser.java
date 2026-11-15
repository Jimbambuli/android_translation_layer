package android.bluetooth.le;

/* BluetoothLeAdvertiser is only handed out by BluetoothAdapter when getLeAccess() holds (adapter on or BLE-only). ATL's adapter stays
 * off, so getBluetoothLe*() returns null and no instance exists; the class is here for the return type. */
public final class BluetoothLeAdvertiser {
	BluetoothLeAdvertiser() {}
}
