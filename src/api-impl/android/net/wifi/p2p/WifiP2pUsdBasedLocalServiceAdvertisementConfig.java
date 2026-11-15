package android.net.wifi.p2p;

import android.os.Parcel;
import android.os.Parcelable;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pUsdBasedLocalServiceAdvertisementConfig.java */
public final class WifiP2pUsdBasedLocalServiceAdvertisementConfig implements Parcelable {
	private static final int USD_DEFAULT_DISCOVERY_CHANNEL_MHZ = 2437;
	private int mFrequencyMhz;

	private WifiP2pUsdBasedLocalServiceAdvertisementConfig(int frequencyMhz) {
		mFrequencyMhz = frequencyMhz;
	}

	public int getFrequencyMhz() {
		return mFrequencyMhz;
	}

	public String toString() {
		StringBuilder sbuf = new StringBuilder("WifiP2pUsdBasedLocalServiceAdvertisementConfig:");
		sbuf.append("\n Frequency: ").append(mFrequencyMhz);
		return sbuf.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mFrequencyMhz);
	}

	private WifiP2pUsdBasedLocalServiceAdvertisementConfig(Parcel in) {
		this.mFrequencyMhz = in.readInt();
	}

	public static final Creator<WifiP2pUsdBasedLocalServiceAdvertisementConfig> CREATOR = new Creator<WifiP2pUsdBasedLocalServiceAdvertisementConfig>() {
		public WifiP2pUsdBasedLocalServiceAdvertisementConfig createFromParcel(Parcel in) {
			return new WifiP2pUsdBasedLocalServiceAdvertisementConfig(in);
		}

		public WifiP2pUsdBasedLocalServiceAdvertisementConfig[] newArray(int size) {
			return new WifiP2pUsdBasedLocalServiceAdvertisementConfig[size];
		}
	};

	public static final class Builder {
		private int mFrequencyMhz;

		public Builder() {
			mFrequencyMhz = USD_DEFAULT_DISCOVERY_CHANNEL_MHZ;
		}

		public Builder setFrequencyMhz(int frequencyMhz) {
			if (frequencyMhz <= 0) {
				throw new IllegalArgumentException("Frequency must be greater than 0");
			}
			mFrequencyMhz = frequencyMhz;
			return this;
		}

		public WifiP2pUsdBasedLocalServiceAdvertisementConfig build() {
			return new WifiP2pUsdBasedLocalServiceAdvertisementConfig(mFrequencyMhz);
		}
	}
}
