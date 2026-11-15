package android.net.wifi.p2p;

import android.os.Parcel;
import android.os.Parcelable;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pDiscoveryConfig.java (without the vendor
 * data, a system API) */
public final class WifiP2pDiscoveryConfig implements Parcelable {
	private final int mScanType;
	private final int mFrequencyMhz;

	private WifiP2pDiscoveryConfig(int scanType, int frequencyMhz) {
		mScanType = scanType;
		mFrequencyMhz = frequencyMhz;
	}

	public int getScanType() {
		return mScanType;
	}

	public int getFrequencyMhz() {
		return mFrequencyMhz;
	}

	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("\n scanType: ").append(mScanType);
		sbuf.append("\n frequencyMhz: ").append(mFrequencyMhz);
		return sbuf.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mScanType);
		dest.writeInt(mFrequencyMhz);
	}

	WifiP2pDiscoveryConfig(Parcel in) {
		this.mScanType = in.readInt();
		this.mFrequencyMhz = in.readInt();
	}

	public static final Creator<WifiP2pDiscoveryConfig> CREATOR = new Creator<WifiP2pDiscoveryConfig>() {
		public WifiP2pDiscoveryConfig createFromParcel(Parcel in) {
			return new WifiP2pDiscoveryConfig(in);
		}

		public WifiP2pDiscoveryConfig[] newArray(int size) {
			return new WifiP2pDiscoveryConfig[size];
		}
	};

	public static final class Builder {
		private final int mScanType;
		private int mFrequencyMhz = 0;

		public Builder(int scanType) {
			mScanType = scanType;
		}

		public Builder setFrequencyMhz(int frequencyMhz) {
			if (frequencyMhz <= 0) {
				throw new IllegalArgumentException("Frequency must be greater than 0");
			}
			mFrequencyMhz = frequencyMhz;
			return this;
		}

		public WifiP2pDiscoveryConfig build() {
			if (mScanType < WifiP2pManager.WIFI_P2P_SCAN_FULL || mScanType > WifiP2pManager.WIFI_P2P_SCAN_SINGLE_FREQ) {
				throw new IllegalArgumentException("Invalid scan type " + mScanType);
			}
			if (mScanType == WifiP2pManager.WIFI_P2P_SCAN_SINGLE_FREQ && mFrequencyMhz == 0) {
				throw new IllegalArgumentException("Scan type is single frequency, but no frequency was provided");
			}
			return new WifiP2pDiscoveryConfig(mScanType, mFrequencyMhz);
		}
	}
}
