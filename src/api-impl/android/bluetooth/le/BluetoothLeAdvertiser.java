package android.bluetooth.le;

/* BluetoothLeAdvertiser is only handed out by BluetoothAdapter when getLeAccess() holds (adapter on or BLE-only). ATL's adapter stays
 * off, so getBluetoothLe*() returns null and no instance exists; the class is here for the return type. */
public final class BluetoothLeAdvertiser {
	BluetoothLeAdvertiser() {}

	/* AOSP stopAdvertising(): no advertiser was ever started here (no instance exists), so the callback is
	 * always unregistered. */
	public void stopAdvertising(final AdvertiseCallback callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback cannot be null");
		}
		android.util.Log.e("BluetoothLeAdvertiser", "Callback not registered or already removed");
	}
}
