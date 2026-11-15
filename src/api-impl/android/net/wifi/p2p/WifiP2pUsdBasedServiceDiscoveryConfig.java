package android.net.wifi.p2p;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pUsdBasedServiceDiscoveryConfig.java; the
 * ScanResult.WIFI_BAND_* values (WifiScanner.WIFI_BAND_*) and ScanResult.UNSPECIFIED are inlined, ATL's
 * ScanResult does not have them */
public final class WifiP2pUsdBasedServiceDiscoveryConfig implements Parcelable {
	private static final int USD_DEFAULT_DISCOVERY_CHANNEL_MHZ = 2437;
	private static final int UNSPECIFIED = -1;
	private static final int WIFI_BAND_24_GHZ = 1 << 0;
	private static final int WIFI_BAND_5_GHZ = 1 << 1;
	private static final int WIFI_BAND_6_GHZ = 1 << 2;

	private int mBand;
	private int[] mFrequenciesMhz;

	private WifiP2pUsdBasedServiceDiscoveryConfig(int band, int[] frequencies) {
		mBand = band;
		mFrequenciesMhz = frequencies;
	}

	public int getBand() {
		return mBand;
	}

	public int[] getFrequenciesMhz() {
		return mFrequenciesMhz;
	}

	public String toString() {
		StringBuilder sbuf = new StringBuilder("WifiP2pUsdBasedServiceDiscoveryConfig:");
		sbuf.append("\n Band: ").append(mBand);
		sbuf.append("\n Frequencies: ").append((mFrequenciesMhz == null) ? "<null>" : Arrays.toString(mFrequenciesMhz));
		return sbuf.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mBand);
		dest.writeIntArray(mFrequenciesMhz);
	}

	private WifiP2pUsdBasedServiceDiscoveryConfig(Parcel in) {
		this.mBand = in.readInt();
		this.mFrequenciesMhz = in.createIntArray();
	}

	private static boolean isBandValid(int band) {
		int bandAny = WIFI_BAND_24_GHZ | WIFI_BAND_5_GHZ | WIFI_BAND_6_GHZ;
		return ((band != 0) && ((band & ~bandAny) == 0));
	}

	public static final Creator<WifiP2pUsdBasedServiceDiscoveryConfig> CREATOR = new Creator<WifiP2pUsdBasedServiceDiscoveryConfig>() {
		public WifiP2pUsdBasedServiceDiscoveryConfig createFromParcel(Parcel in) {
			return new WifiP2pUsdBasedServiceDiscoveryConfig(in);
		}

		public WifiP2pUsdBasedServiceDiscoveryConfig[] newArray(int size) {
			return new WifiP2pUsdBasedServiceDiscoveryConfig[size];
		}
	};

	public static final class Builder {
		private static final int MAXIMUM_CHANNEL_FREQUENCIES = 48;
		private int mBand;
		private int[] mFrequenciesMhz;

		public Builder() {
			mBand = UNSPECIFIED;
			mFrequenciesMhz = new int[] { USD_DEFAULT_DISCOVERY_CHANNEL_MHZ };
		}

		public Builder setBand(int band) {
			if (!isBandValid(band)) {
				throw new IllegalArgumentException("Invalid band: " + band);
			}
			mBand = band;
			return this;
		}

		public Builder setFrequenciesMhz(int[] frequenciesMhz) {
			Objects.requireNonNull(frequenciesMhz, "Frequencies cannot be null");
			if (frequenciesMhz.length < 1 || frequenciesMhz.length > MAXIMUM_CHANNEL_FREQUENCIES) {
				throw new IllegalArgumentException("Number of frequencies: " + frequenciesMhz.length + " must be between 1 and " + MAXIMUM_CHANNEL_FREQUENCIES);
			}
			mFrequenciesMhz = frequenciesMhz;
			return this;
		}

		public WifiP2pUsdBasedServiceDiscoveryConfig build() {
			if (mBand != UNSPECIFIED && mFrequenciesMhz != null) {
				throw new IllegalStateException("Frequencies and band are mutually exclusive.");
			}
			return new WifiP2pUsdBasedServiceDiscoveryConfig(mBand, mFrequenciesMhz);
		}
	}
}
