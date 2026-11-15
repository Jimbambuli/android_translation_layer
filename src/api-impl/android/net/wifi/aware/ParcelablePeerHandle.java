package android.net.wifi.aware;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/aware/ParcelablePeerHandle.java */
public final class ParcelablePeerHandle extends PeerHandle implements Parcelable {
	public ParcelablePeerHandle(@NonNull PeerHandle peerHandle) {
		super(peerHandle.peerId);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(peerId);
	}

	@NonNull
	public static final Creator<ParcelablePeerHandle> CREATOR = new Creator<ParcelablePeerHandle>() {
		@Override
		public ParcelablePeerHandle[] newArray(int size) {
			return new ParcelablePeerHandle[size];
		}

		@Override
		public ParcelablePeerHandle createFromParcel(Parcel in) {
			return new ParcelablePeerHandle(new PeerHandle(in.readInt()));
		}
	};
}
