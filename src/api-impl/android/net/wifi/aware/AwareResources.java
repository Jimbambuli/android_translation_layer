package android.net.wifi.aware;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/aware/AwareResources.java */
public final class AwareResources implements Parcelable {
	private int mNumOfAvailableNdps;
	private int mNumOfAvailablePublishSessions;
	private int mNumOfAvailableSubscribeSessions;

	public AwareResources(int availableDataPathsCount, int availablePublishSessionsCount,
			int availableSubscribeSessionsCount) {
		mNumOfAvailableNdps = availableDataPathsCount;
		mNumOfAvailablePublishSessions = availablePublishSessionsCount;
		mNumOfAvailableSubscribeSessions = availableSubscribeSessionsCount;
	}

	public int getAvailableDataPathsCount() {
		return mNumOfAvailableNdps;
	}

	public int getAvailablePublishSessionsCount() {
		return mNumOfAvailablePublishSessions;
	}

	public int getAvailableSubscribeSessionsCount() {
		return mNumOfAvailableSubscribeSessions;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeInt(mNumOfAvailableNdps);
		dest.writeInt(mNumOfAvailablePublishSessions);
		dest.writeInt(mNumOfAvailableSubscribeSessions);
	}

	@NonNull
	public static final Creator<AwareResources> CREATOR = new Creator<AwareResources>() {
		@Override
		public AwareResources createFromParcel(Parcel in) {
			return new AwareResources(in.readInt(), in.readInt(), in.readInt());
		}

		@Override
		public AwareResources[] newArray(int size) {
			return new AwareResources[size];
		}
	};
}
