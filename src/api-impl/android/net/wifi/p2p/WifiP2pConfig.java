package android.net.wifi.p2p;

import android.net.wifi.WpsInfo;
import android.text.TextUtils;
import java.nio.charset.StandardCharsets;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pConfig.java: fields and the Builder for
 * network name, passphrase, band and frequency, with the validations of those setters and of build() (default PCC
 * mode LEGACY_ONLY). Setting the peer by MacAddress is not ported, ATL has no android.net.MacAddress. */
public class WifiP2pConfig {
	static final int PSK_PASSWORD_MIN_LEN = 8;
	static final int PSK_PASSWORD_MAX_LEN = 63;
	static final int SAE_PASSWORD_MAX_LEN = 128;
	private static final int MAX_SSID_BYTES = 32;
	private static final String MAC_ANY_ADDRESS = "02:00:00:00:00:00";

	public static final int GROUP_OWNER_BAND_AUTO = 0;
	public static final int GROUP_OWNER_BAND_2GHZ = 1;
	public static final int GROUP_OWNER_BAND_5GHZ = 2;
	public static final int GROUP_OWNER_BAND_6GHZ = 3;

	public static final int GROUP_OWNER_INTENT_MIN = 0;
	public static final int GROUP_OWNER_INTENT_MAX = 15;
	public static final int GROUP_OWNER_INTENT_AUTO = -1;

	public String deviceAddress = "";
	public WpsInfo wps;
	public int groupOwnerIntent = GROUP_OWNER_INTENT_AUTO;
	public int netId = WifiP2pGroup.NETWORK_ID_PERSISTENT;
	public String networkName = "";
	public String passphrase = "";
	public int groupOwnerBand = GROUP_OWNER_BAND_AUTO;

	public WifiP2pConfig() {
		wps = new WpsInfo();
		wps.setup = WpsInfo.PBC;
	}

	public static final class Builder {
		private String mNetworkName = "";
		private String mPassphrase = "";
		private int mGroupOperatingBand = GROUP_OWNER_BAND_AUTO;
		private int mGroupOperatingFrequency = GROUP_OWNER_BAND_AUTO;
		private int mNetId = WifiP2pGroup.NETWORK_ID_TEMPORARY;

		public Builder() {}

		public Builder setNetworkName(String networkName) {
			if (TextUtils.isEmpty(networkName)) {
				throw new IllegalArgumentException("network name must be non-empty.");
			}
			if (networkName.getBytes(StandardCharsets.UTF_8).length > MAX_SSID_BYTES) {
				throw new IllegalArgumentException("network name exceeds " + MAX_SSID_BYTES + " bytes.");
			}
			if (!networkName.matches("^DIRECT-[a-zA-Z0-9]{2}.*")) {
				throw new IllegalArgumentException("network name must starts with the prefix DIRECT-xy.");
			}
			mNetworkName = networkName;
			return this;
		}

		public Builder setPassphrase(String passphrase) {
			java.util.Objects.requireNonNull(passphrase, "passphrase cannot be null");
			if (TextUtils.isEmpty(passphrase)) {
				throw new IllegalArgumentException("passphrase must be non-empty.");
			}
			if (passphrase.length() > SAE_PASSWORD_MAX_LEN) {
				throw new IllegalArgumentException("The length of a passphrase must be less than 128");
			}
			mPassphrase = passphrase;
			return this;
		}

		public Builder setGroupOperatingBand(int band) {
			if (GROUP_OWNER_BAND_AUTO == band || GROUP_OWNER_BAND_2GHZ == band || GROUP_OWNER_BAND_5GHZ == band) {
				mGroupOperatingBand = band;
			} else {
				throw new IllegalArgumentException("Invalid constant for the group operating band!");
			}
			return this;
		}

		public Builder setGroupOperatingFrequency(int frequency) {
			if (frequency < 0) {
				throw new IllegalArgumentException("Invalid group operating frequency!");
			}
			mGroupOperatingFrequency = frequency;
			return this;
		}

		public WifiP2pConfig build() {
			if ((TextUtils.isEmpty(mNetworkName) && !TextUtils.isEmpty(mPassphrase)) || (!TextUtils.isEmpty(mNetworkName) && TextUtils.isEmpty(mPassphrase))) {
				throw new IllegalStateException("network name and passphrase must be non-empty or empty both.");
			}
			/* the device address is always the MAC_ANY_ADDRESS here, see the class comment */
			if (TextUtils.isEmpty(mNetworkName)) {
				throw new IllegalStateException("peer address must be set if network name and passphrase are not set.");
			}
			if (mPassphrase.length() < PSK_PASSWORD_MIN_LEN || mPassphrase.length() > PSK_PASSWORD_MAX_LEN) {
				throw new IllegalArgumentException("The length of a passphrase must be between " + PSK_PASSWORD_MIN_LEN + " and " + PSK_PASSWORD_MAX_LEN + " for legacy connection type");
			}
			if (mGroupOperatingFrequency > 0 && mGroupOperatingBand > 0) {
				throw new IllegalStateException("Preferred frequency and band are mutually exclusive.");
			}
			WifiP2pConfig config = new WifiP2pConfig();
			config.deviceAddress = MAC_ANY_ADDRESS;
			config.networkName = mNetworkName;
			config.passphrase = mPassphrase;
			config.groupOwnerBand = GROUP_OWNER_BAND_AUTO;
			if (mGroupOperatingFrequency > 0) {
				config.groupOwnerBand = mGroupOperatingFrequency;
			} else if (mGroupOperatingBand > 0) {
				config.groupOwnerBand = mGroupOperatingBand;
			}
			config.netId = mNetId;
			return config;
		}
	}
}
