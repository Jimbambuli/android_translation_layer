package android.net.wifi.aware;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/aware/WifiAwareChannelInfo.java */
public final class WifiAwareChannelInfo implements Parcelable {
	private final int mChannelFreq;
	private final int mChannelBandwidth;
	private final int mNumSpatialStreams;

	public WifiAwareChannelInfo(int channelFreq, int channelBandwidth, int numSpatialStreams) {
		mChannelFreq = channelFreq;
		mChannelBandwidth = channelBandwidth;
		mNumSpatialStreams = numSpatialStreams;
	}

	public int getChannelFrequencyMhz() {
		return mChannelFreq;
	}

	public int getChannelBandwidth() {
		return mChannelBandwidth;
	}

	public int getSpatialStreamCount() {
		return mNumSpatialStreams;
	}

	private WifiAwareChannelInfo(Parcel in) {
		mChannelFreq = in.readInt();
		mChannelBandwidth = in.readInt();
		mNumSpatialStreams = in.readInt();
	}

	@NonNull
	public static final Creator<WifiAwareChannelInfo> CREATOR = new Creator<WifiAwareChannelInfo>() {
		@Override
		public WifiAwareChannelInfo createFromParcel(Parcel in) {
			return new WifiAwareChannelInfo(in);
		}

		@Override
		public WifiAwareChannelInfo[] newArray(int size) {
			return new WifiAwareChannelInfo[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeInt(mChannelFreq);
		dest.writeInt(mChannelBandwidth);
		dest.writeInt(mNumSpatialStreams);
	}

	@Override
	public String toString() {
		return "{.channelFreq = " + mChannelFreq + ", .channelBandwidth = " + mChannelBandwidth
				+ ", .numSpatialStreams = " + mNumSpatialStreams + "}";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof WifiAwareChannelInfo)) {
			return false;
		}
		WifiAwareChannelInfo lhs = (WifiAwareChannelInfo) obj;
		return mChannelFreq == lhs.mChannelFreq && mChannelBandwidth == lhs.mChannelBandwidth
				&& mNumSpatialStreams == lhs.mNumSpatialStreams;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mChannelFreq, mChannelBandwidth, mNumSpatialStreams);
	}
}
