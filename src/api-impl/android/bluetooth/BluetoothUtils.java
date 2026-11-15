package android.bluetooth;

import android.os.Parcel;

/* AOSP modules/Bluetooth BluetoothUtils (@hide), the part ATL needs */
public final class BluetoothUtils {
	private BluetoothUtils() {}

	/* AOSP BluetoothUtils.writeStringToParcel() */
	public static void writeStringToParcel(Parcel out, String str) {
		out.writeString(str);
	}
}
