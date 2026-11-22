package android.net.wifi;

import static android.net.wifi.ScanResult.UNSPECIFIED;

import static com.android.internal.util.Preconditions.checkNotNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.MacAddress;
import android.net.NetworkSpecifier;
import android.net.wifi.ScanResult.WifiBand;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PatternMatcher;
import android.text.TextUtils;
import android.util.Pair;

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/WifiNetworkSpecifier.java */
public final class WifiNetworkSpecifier extends NetworkSpecifier implements Parcelable {

	private static final String TAG = "WifiNetworkSpecifier";

	@WifiBand public static int getBand(final int freqMHz) {
		if (ScanResult.is24GHz(freqMHz)) {
			return ScanResult.WIFI_BAND_24_GHZ;
		} else if (ScanResult.is5GHz(freqMHz)) {
			return ScanResult.WIFI_BAND_5_GHZ;
		} else if (ScanResult.is6GHz(freqMHz)) {
			return ScanResult.WIFI_BAND_6_GHZ;
		} else if (ScanResult.is60GHz(freqMHz)) {
			return ScanResult.WIFI_BAND_60_GHZ;
		}
		return UNSPECIFIED;
	}

	public static boolean validateChannelFrequencyInMhz(@NonNull int[] channels) {
		if (channels == null) {
			return false;
		}
		for (int channel : channels) {
			if (ScanResult.convertFrequencyMhzToChannelIfSupported(channel) == UNSPECIFIED) {
				return false;
			}
		}
		return true;
	}

	public static boolean validateBand(@WifiBand int band) {
		switch (band) {
			case UNSPECIFIED:
			case ScanResult.WIFI_BAND_24_GHZ:
			case ScanResult.WIFI_BAND_5_GHZ:
			case ScanResult.WIFI_BAND_5_GHZ_LOW:
			case ScanResult.WIFI_BAND_5_GHZ_HIGH:
			case ScanResult.WIFI_BAND_6_GHZ:
			case ScanResult.WIFI_BAND_60_GHZ:
				return true;
			default:
				return false;
		}
	}

	public static final class Builder {
		private static final String MATCH_ALL_SSID_PATTERN_PATH = ".*";
		private static final String MATCH_EMPTY_SSID_PATTERN_PATH = "";
		private static final Pair<MacAddress, MacAddress> MATCH_NO_BSSID_PATTERN1 =
				new Pair<>(MacAddress.BROADCAST_ADDRESS, MacAddress.BROADCAST_ADDRESS);
		private static final Pair<MacAddress, MacAddress> MATCH_NO_BSSID_PATTERN2 =
				new Pair<>(WifiManager.ALL_ZEROS_MAC_ADDRESS, MacAddress.BROADCAST_ADDRESS);
		private static final Pair<MacAddress, MacAddress> MATCH_ALL_BSSID_PATTERN =
				new Pair<>(WifiManager.ALL_ZEROS_MAC_ADDRESS, WifiManager.ALL_ZEROS_MAC_ADDRESS);
		private static final MacAddress MATCH_EXACT_BSSID_PATTERN_MASK =
				MacAddress.BROADCAST_ADDRESS;

		private static final int WPA3_ENTERPRISE_AUTO = 0;

		private static final int WPA3_ENTERPRISE_STANDARD = 1;

		private static final int WPA3_ENTERPRISE_192_BIT = 2;

		private @Nullable PatternMatcher mSsidPatternMatcher;

		private @Nullable Pair<MacAddress, MacAddress> mBssidPatternMatcher;

		private boolean mIsEnhancedOpen;

		private @Nullable String mWpa2PskPassphrase;

		private @Nullable String mWpa3SaePassphrase;

		private @Nullable WifiEnterpriseConfig mWpa2EnterpriseConfig;

		private @Nullable WifiEnterpriseConfig mWpa3EnterpriseConfig;

		private int mWpa3EnterpriseType = WPA3_ENTERPRISE_AUTO;

		private boolean mIsHiddenSSID;

		@WifiBand private int mBand;

		private int[] mChannels;

		private boolean mPreferSecondarySta;

		public Builder() {
			mSsidPatternMatcher = null;
			mBssidPatternMatcher = null;
			mIsEnhancedOpen = false;
			mWpa2PskPassphrase = null;
			mWpa3SaePassphrase = null;
			mWpa2EnterpriseConfig = null;
			mWpa3EnterpriseConfig = null;
			mIsHiddenSSID = false;
			mBand = UNSPECIFIED;
			mChannels = new int[0];
		}

		public @NonNull Builder setSsidPattern(@NonNull PatternMatcher ssidPattern) {
			checkNotNull(ssidPattern);
			mSsidPatternMatcher = ssidPattern;
			return this;
		}

		public @NonNull Builder setSsid(@NonNull String ssid) {
			checkNotNull(ssid);
			final CharsetEncoder unicodeEncoder = StandardCharsets.UTF_8.newEncoder();
			if (!unicodeEncoder.canEncode(ssid)) {
				throw new IllegalArgumentException("SSID is not a valid unicode string");
			}
			mSsidPatternMatcher = new PatternMatcher(ssid, PatternMatcher.PATTERN_LITERAL);
			return this;
		}

		public @NonNull Builder setBssidPattern(
				@NonNull MacAddress baseAddress, @NonNull MacAddress mask) {
			checkNotNull(baseAddress);
			checkNotNull(mask);
			mBssidPatternMatcher = Pair.create(baseAddress, mask);
			return this;
		}

		public @NonNull Builder setBssid(@NonNull MacAddress bssid) {
			checkNotNull(bssid);
			mBssidPatternMatcher = Pair.create(bssid, MATCH_EXACT_BSSID_PATTERN_MASK);
			return this;
		}

		public @NonNull Builder setIsEnhancedOpen(boolean isEnhancedOpen) {
			mIsEnhancedOpen = isEnhancedOpen;
			return this;
		}

		public @NonNull Builder setWpa2Passphrase(@NonNull String passphrase) {
			checkNotNull(passphrase);
			final CharsetEncoder asciiEncoder = StandardCharsets.US_ASCII.newEncoder();
			if (!asciiEncoder.canEncode(passphrase)) {
				throw new IllegalArgumentException("passphrase not ASCII encodable");
			}
			mWpa2PskPassphrase = passphrase;
			return this;
		}

		public @NonNull Builder setWpa3Passphrase(@NonNull String passphrase) {
			checkNotNull(passphrase);
			final CharsetEncoder asciiEncoder = StandardCharsets.US_ASCII.newEncoder();
			if (!asciiEncoder.canEncode(passphrase)) {
				throw new IllegalArgumentException("passphrase not ASCII encodable");
			}
			mWpa3SaePassphrase = passphrase;
			return this;
		}

		public @NonNull Builder setWpa2EnterpriseConfig(
				@NonNull WifiEnterpriseConfig enterpriseConfig) {
			checkNotNull(enterpriseConfig);
			mWpa2EnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
			return this;
		}

		@Deprecated
		public @NonNull Builder setWpa3EnterpriseConfig(
				@NonNull WifiEnterpriseConfig enterpriseConfig) {
			checkNotNull(enterpriseConfig);
			mWpa3EnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
			return this;
		}

		public @NonNull Builder setWpa3EnterpriseStandardModeConfig(
				@NonNull WifiEnterpriseConfig enterpriseConfig) {
			checkNotNull(enterpriseConfig);
			mWpa3EnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
			mWpa3EnterpriseType = WPA3_ENTERPRISE_STANDARD;
			return this;
		}

		public @NonNull Builder setWpa3Enterprise192BitModeConfig(
				@NonNull WifiEnterpriseConfig enterpriseConfig) {
			checkNotNull(enterpriseConfig);
			if (enterpriseConfig.getEapMethod() != WifiEnterpriseConfig.Eap.TLS) {
				throw new IllegalArgumentException("The 192-bit mode network type must be TLS");
			}
			if (!WifiEnterpriseConfig.isSuiteBCipherCert(
					enterpriseConfig.getClientCertificate())) {
				throw new IllegalArgumentException(
					"The client certificate does not meet 192-bit mode requirements.");
			}
			if (!WifiEnterpriseConfig.isSuiteBCipherCert(
					enterpriseConfig.getCaCertificate())) {
				throw new IllegalArgumentException(
					"The CA certificate does not meet 192-bit mode requirements.");
			}

			mWpa3EnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
			mWpa3EnterpriseType = WPA3_ENTERPRISE_192_BIT;
			return this;
		}

		public @NonNull Builder setIsHiddenSsid(boolean isHiddenSsid) {
			mIsHiddenSSID = isHiddenSsid;
			return this;
		}

		public @NonNull Builder setBand(@WifiBand int band) {
			if (!validateBand(band)) {
				throw new IllegalArgumentException("Unexpected band in setBand : " + band);
			}
			mBand = band;
			return this;
		}

		@NonNull public Builder setPreferredChannelsFrequenciesMhz(@NonNull int[] channelFreqs) {
			Objects.requireNonNull(channelFreqs);
			if (!validateChannelFrequencyInMhz(channelFreqs)) {
				throw new IllegalArgumentException("Invalid channel frequency in the input array");
			}
			mChannels = channelFreqs.clone();
			return this;
		}

		@NonNull public Builder setPreferSecondarySta(boolean value) {
			mPreferSecondarySta = value;
			return this;
		}

		private void setSecurityParamsInWifiConfiguration(
				@NonNull WifiConfiguration configuration) {
			if (!TextUtils.isEmpty(mWpa2PskPassphrase)) { // WPA-PSK network.
				configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_PSK);
				configuration.preSharedKey = "\"" + mWpa2PskPassphrase + "\"";
			} else if (!TextUtils.isEmpty(mWpa3SaePassphrase)) { // WPA3-SAE network.
				configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_SAE);
				configuration.preSharedKey = "\"" + mWpa3SaePassphrase + "\"";
			} else if (mWpa2EnterpriseConfig != null) { // WPA-EAP network
				configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_EAP);
				configuration.enterpriseConfig = mWpa2EnterpriseConfig;
			} else if (mWpa3EnterpriseConfig != null) { // WPA3-Enterprise
				if (mWpa3EnterpriseType == WPA3_ENTERPRISE_AUTO
						&& mWpa3EnterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.TLS
						&& WifiEnterpriseConfig.isSuiteBCipherCert(
						mWpa3EnterpriseConfig.getClientCertificate())
						&& WifiEnterpriseConfig.isSuiteBCipherCert(
						mWpa3EnterpriseConfig.getCaCertificate())) {
					configuration.setSecurityParams(
							WifiConfiguration.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT);
				} else if (mWpa3EnterpriseType == WPA3_ENTERPRISE_192_BIT) {
					configuration.setSecurityParams(
							WifiConfiguration.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT);
				} else {
					configuration.setSecurityParams(
							WifiConfiguration.SECURITY_TYPE_EAP_WPA3_ENTERPRISE);
				}
				configuration.enterpriseConfig = mWpa3EnterpriseConfig;
			} else if (mIsEnhancedOpen) { // OWE network
				configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OWE);
			} else { // Open network
				configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OPEN);
			}
		}

		private WifiConfiguration buildWifiConfiguration() {
			final WifiConfiguration wifiConfiguration = new WifiConfiguration();
			if (mSsidPatternMatcher.getType() == PatternMatcher.PATTERN_LITERAL) {
				wifiConfiguration.SSID = "\"" + mSsidPatternMatcher.getPath() + "\"";
			}
			if (mBssidPatternMatcher.second == MATCH_EXACT_BSSID_PATTERN_MASK) {
				wifiConfiguration.BSSID = mBssidPatternMatcher.first.toString();
			}
			setSecurityParamsInWifiConfiguration(wifiConfiguration);
			wifiConfiguration.hiddenSSID = mIsHiddenSSID;
			return wifiConfiguration;
		}

		private boolean hasSetAnyPattern() {
			return mSsidPatternMatcher != null || mBssidPatternMatcher != null;
		}

		private void setMatchAnyPatternIfUnset() {
			if (mSsidPatternMatcher == null) {
				mSsidPatternMatcher = new PatternMatcher(MATCH_ALL_SSID_PATTERN_PATH,
						PatternMatcher.PATTERN_SIMPLE_GLOB);
			}
			if (mBssidPatternMatcher == null) {
				mBssidPatternMatcher = MATCH_ALL_BSSID_PATTERN;
			}
		}

		private boolean hasSetMatchNonePattern() {
			if (mSsidPatternMatcher.getType() != PatternMatcher.PATTERN_PREFIX
					&& mSsidPatternMatcher.getPath().equals(MATCH_EMPTY_SSID_PATTERN_PATH)) {
				return true;
			}
			if (mBssidPatternMatcher.equals(MATCH_NO_BSSID_PATTERN1)) {
				return true;
			}
			if (mBssidPatternMatcher.equals(MATCH_NO_BSSID_PATTERN2)) {
				return true;
			}
			return false;
		}

		private boolean hasSetMatchAllPattern() {
			if ((mSsidPatternMatcher.match(MATCH_EMPTY_SSID_PATTERN_PATH))
					&& mBssidPatternMatcher.equals(MATCH_ALL_BSSID_PATTERN)) {
				return true;
			}
			return false;
		}

		private void validateSecurityParams() {
			int numSecurityTypes = 0;
			numSecurityTypes += mIsEnhancedOpen ? 1 : 0;
			numSecurityTypes += !TextUtils.isEmpty(mWpa2PskPassphrase) ? 1 : 0;
			numSecurityTypes += !TextUtils.isEmpty(mWpa3SaePassphrase) ? 1 : 0;
			numSecurityTypes += mWpa2EnterpriseConfig != null ? 1 : 0;
			numSecurityTypes += mWpa3EnterpriseConfig != null ? 1 : 0;
			if (numSecurityTypes > 1) {
				throw new IllegalStateException("only one of setIsEnhancedOpen, setWpa2Passphrase,"
						+ "setWpa3Passphrase, setWpa2EnterpriseConfig or setWpa3EnterpriseConfig"
						+ " can be invoked for network specifier");
			}
		}

		public @NonNull WifiNetworkSpecifier build() {
			if (!hasSetAnyPattern() && mBand == UNSPECIFIED) {
				throw new IllegalStateException("one of setSsidPattern/setSsid/setBssidPattern/"
						+ "setBssid/setBand should be invoked for specifier");
			}
			setMatchAnyPatternIfUnset();
			if (hasSetMatchNonePattern()) {
				throw new IllegalStateException("cannot set match-none pattern for specifier");
			}
			if (hasSetMatchAllPattern() && mBand == UNSPECIFIED) {
				throw new IllegalStateException("cannot set match-all pattern for specifier");
			}
			if (mIsHiddenSSID && mSsidPatternMatcher.getType() != PatternMatcher.PATTERN_LITERAL) {
				throw new IllegalStateException("setSsid should also be invoked when "
						+ "setIsHiddenSsid is invoked for network specifier");
			}
			if (mChannels.length != 0 && mBand != UNSPECIFIED) {
				throw new IllegalStateException("cannot setPreferredChannelsFrequencyInMhz with "
						+ "setBand together");
			}
			validateSecurityParams();

			return new WifiNetworkSpecifier(
					mSsidPatternMatcher,
					mBssidPatternMatcher,
					mBand,
					buildWifiConfiguration(),
					mChannels,
					mPreferSecondarySta);
		}
	}

	public final PatternMatcher ssidPatternMatcher;

	public final Pair<MacAddress, MacAddress> bssidPatternMatcher;

	@WifiBand private final int mBand;

	private final int[] mChannelFreqs;
	private boolean mPreferSecondarySta;

	public final WifiConfiguration wifiConfiguration;

	public WifiNetworkSpecifier() throws IllegalAccessException {
		throw new IllegalAccessException("Use the builder to create an instance");
	}

	public WifiNetworkSpecifier(@NonNull PatternMatcher ssidPatternMatcher,
			@NonNull Pair<MacAddress, MacAddress> bssidPatternMatcher,
			@WifiBand int band,
			@NonNull WifiConfiguration wifiConfiguration,
			@NonNull int[] channelFreqs,
			boolean preferSecondarySta) {
		checkNotNull(ssidPatternMatcher);
		checkNotNull(bssidPatternMatcher);
		checkNotNull(wifiConfiguration);

		this.ssidPatternMatcher = ssidPatternMatcher;
		this.bssidPatternMatcher = bssidPatternMatcher;
		this.mBand = band;
		this.wifiConfiguration = wifiConfiguration;
		this.mChannelFreqs = channelFreqs;
		this.mPreferSecondarySta = preferSecondarySta;
	}

	@WifiBand public int getBand() {
		return mBand;
	}

	@NonNull public int[] getPreferredChannelFrequenciesMhz() {
		return mChannelFreqs.clone();
	}

	public boolean isPreferSecondarySta() {
		return mPreferSecondarySta;
	}

	public static final @NonNull Creator<WifiNetworkSpecifier> CREATOR =
			new Creator<WifiNetworkSpecifier>() {
				@Override
				public WifiNetworkSpecifier createFromParcel(Parcel in) {
					PatternMatcher ssidPatternMatcher = in.readParcelable(null);
					MacAddress baseAddress = in.readParcelable(null);
					MacAddress mask = in.readParcelable(null);
					Pair<MacAddress, MacAddress> bssidPatternMatcher =
							Pair.create(baseAddress, mask);
					int band = in.readInt();
					WifiConfiguration wifiConfiguration = in.readParcelable(null);
					int[] channels = in.createIntArray();
					boolean preferSecondarySta = in.readBoolean();
					return new WifiNetworkSpecifier(ssidPatternMatcher, bssidPatternMatcher, band,
							wifiConfiguration, channels, preferSecondarySta);
				}

				@Override
				public WifiNetworkSpecifier[] newArray(int size) {
					return new WifiNetworkSpecifier[size];
				}
			};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(ssidPatternMatcher, flags);
		dest.writeParcelable(bssidPatternMatcher.first, flags);
		dest.writeParcelable(bssidPatternMatcher.second, flags);
		dest.writeInt(mBand);
		dest.writeParcelable(wifiConfiguration, flags);
		dest.writeIntArray(mChannelFreqs);
		dest.writeBoolean(mPreferSecondarySta);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				ssidPatternMatcher.getPath(), ssidPatternMatcher.getType(), bssidPatternMatcher,
				mBand, wifiConfiguration.allowedKeyManagement, Arrays.hashCode(mChannelFreqs),
				mPreferSecondarySta);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof WifiNetworkSpecifier)) {
			return false;
		}
		WifiNetworkSpecifier lhs = (WifiNetworkSpecifier) obj;
		return Objects.equals(this.ssidPatternMatcher.getPath(),
					lhs.ssidPatternMatcher.getPath())
				&& Objects.equals(this.ssidPatternMatcher.getType(),
					lhs.ssidPatternMatcher.getType())
				&& Objects.equals(this.bssidPatternMatcher,
					lhs.bssidPatternMatcher)
				&& this.mBand == lhs.mBand
				&& Objects.equals(this.wifiConfiguration.allowedKeyManagement,
					lhs.wifiConfiguration.allowedKeyManagement)
				&& Arrays.equals(mChannelFreqs, lhs.mChannelFreqs)
				&& mPreferSecondarySta == lhs.mPreferSecondarySta;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append("WifiNetworkSpecifier [")
				.append(", SSID Match pattern=").append(ssidPatternMatcher)
				.append(", BSSID Match pattern=").append(bssidPatternMatcher)
				.append(", SSID=").append(wifiConfiguration.SSID)
				.append(", BSSID=").append(wifiConfiguration.BSSID)
				.append(", channels=").append(Arrays.toString(mChannelFreqs))
				.append(", band=").append(mBand)
				.append(", preferSecondarySta=").append(mPreferSecondarySta)
				.append("]")
				.toString();
	}

	/* AOSP additionally defers to the @hide WifiNetworkAgentSpecifier, which ATL does not have */
	@Override
	public boolean canBeSatisfiedBy(NetworkSpecifier other) {
		return equals(other);
	}

	@Override
	@Nullable
	public NetworkSpecifier redact() {
		return new Builder().setBand(mBand).build();
	}
}
