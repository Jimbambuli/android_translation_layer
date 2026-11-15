package android.net.wifi.p2p;

import android.net.MacAddress;
import android.net.wifi.WpsInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pConfig.java: fields, getters, the Builder
 * with the validations of its setters and of build(), the copy constructor and the Parcelable code. Not ported:
 * the supplicant-event parser, the vendor data (system API). The WpsInfo is copied and parcelled field by field
 * (setup, BSSID, pin, as WpsInfo does). ATL reports Android 16, so the SDK-level checks (B, T) all pass. */
public class WifiP2pConfig implements Parcelable {
	static final int PSK_PASSWORD_MIN_LEN = 8;
	static final int PSK_PASSWORD_MAX_LEN = 63;
	static final int SAE_PASSWORD_MAX_LEN = 128;

	public static final int GROUP_OWNER_BAND_AUTO = 0;
	public static final int GROUP_OWNER_BAND_2GHZ = 1;
	public static final int GROUP_OWNER_BAND_5GHZ = 2;
	public static final int GROUP_OWNER_BAND_6GHZ = 3;

	public static final int GROUP_OWNER_INTENT_MIN = 0;
	public static final int GROUP_OWNER_INTENT_MAX = 15;
	public static final int GROUP_OWNER_INTENT_AUTO = -1;

	public static final int GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP = 0;
	public static final int GROUP_CLIENT_IP_PROVISIONING_MODE_IPV6_LINK_LOCAL = 1;

	/* AOSP: PCC_MODE_DEFAULT_CONNECTION_TYPE_LEGACY_ONLY (hidden) */
	private static final int PCC_MODE_DEFAULT_CONNECTION_TYPE_LEGACY_ONLY = 0;
	public static final int PCC_MODE_CONNECTION_TYPE_LEGACY_ONLY = PCC_MODE_DEFAULT_CONNECTION_TYPE_LEGACY_ONLY;
	public static final int PCC_MODE_CONNECTION_TYPE_LEGACY_OR_R2 = 1;
	public static final int PCC_MODE_CONNECTION_TYPE_R2_ONLY = 2;

	/* AOSP: P2P_DEFAULT_VERSION_1 (hidden) */
	private static final int P2P_DEFAULT_VERSION_1 = 0;
	public static final int P2P_VERSION_1 = P2P_DEFAULT_VERSION_1;
	public static final int P2P_VERSION_2 = 1;

	public String deviceAddress = "";
	public WpsInfo wps;
	public int groupOwnerIntent = GROUP_OWNER_INTENT_AUTO;
	public int netId = WifiP2pGroup.NETWORK_ID_PERSISTENT;
	public String networkName = "";
	public String passphrase = "";
	public int groupOwnerBand = GROUP_OWNER_BAND_AUTO;

	private int mGroupClientIpProvisioningMode = GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP;
	private boolean mJoinExistingGroup;
	private int mPccModeConnectionType = PCC_MODE_DEFAULT_CONNECTION_TYPE_LEGACY_ONLY;
	private int mGroupOwnerVersion = P2P_DEFAULT_VERSION_1;
	private WifiP2pPairingBootstrappingConfig mPairingBootstrappingConfig;
	private boolean mIsAuthorizeConnectionFromPeerEnabled = false;

	public WifiP2pConfig() {
		wps = new WpsInfo();
		wps.setup = WpsInfo.PBC;
	}

	public WifiP2pConfig(WifiP2pConfig source) {
		if (source != null) {
			deviceAddress = source.deviceAddress;
			wps = copyWps(source.wps);
			groupOwnerIntent = source.groupOwnerIntent;
			netId = source.netId;
			networkName = source.networkName;
			passphrase = source.passphrase;
			groupOwnerBand = source.groupOwnerBand;
			mGroupClientIpProvisioningMode = source.mGroupClientIpProvisioningMode;
			mJoinExistingGroup = source.mJoinExistingGroup;
			mGroupOwnerVersion = source.mGroupOwnerVersion;
			mIsAuthorizeConnectionFromPeerEnabled = source.mIsAuthorizeConnectionFromPeerEnabled;
			mPccModeConnectionType = source.mPccModeConnectionType;
			mPairingBootstrappingConfig = source.mPairingBootstrappingConfig;
		}
	}

	/* AOSP WpsInfo(WpsInfo source): copies setup, BSSID and pin if source is not null */
	private static WpsInfo copyWps(WpsInfo source) {
		WpsInfo wps = new WpsInfo();
		if (source != null) {
			wps.setup = source.setup;
			wps.BSSID = source.BSSID;
			wps.pin = source.pin;
		}
		return wps;
	}

	public int getGroupClientIpProvisioningMode() {
		return mGroupClientIpProvisioningMode;
	}

	public int getPccModeConnectionType() {
		return mPccModeConnectionType;
	}

	public int getGroupOwnerVersion() {
		return mGroupOwnerVersion;
	}

	public void setGroupOwnerVersion(int version) {
		mGroupOwnerVersion = version;
	}

	public WifiP2pPairingBootstrappingConfig getPairingBootstrappingConfig() {
		return mPairingBootstrappingConfig;
	}

	public boolean isAuthorizeConnectionFromPeerEnabled() {
		return mIsAuthorizeConnectionFromPeerEnabled;
	}

	public String getNetworkName() {
		return networkName;
	}

	public String getPassphrase() {
		return passphrase;
	}

	public int getGroupOwnerBand() {
		return groupOwnerBand;
	}

	public int getNetworkId() {
		return netId;
	}

	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("\n address: ").append(deviceAddress);
		sbuf.append("\n wps: ").append(wps);
		sbuf.append("\n groupOwnerIntent: ").append(groupOwnerIntent);
		sbuf.append("\n persist: ").append(netId);
		sbuf.append("\n networkName: ").append(networkName);
		sbuf.append("\n passphrase: ").append(TextUtils.isEmpty(passphrase) ? "<empty>" : "<non-empty>");
		sbuf.append("\n groupOwnerBand: ").append(groupOwnerBand);
		sbuf.append("\n groupClientIpProvisioningMode: ").append(mGroupClientIpProvisioningMode);
		sbuf.append("\n joinExistingGroup: ").append(mJoinExistingGroup);
		sbuf.append("\n Group Owner Version: ").append(mGroupOwnerVersion);
		sbuf.append("\n authorizeConnectionFromPeerEnabled: ").append(mIsAuthorizeConnectionFromPeerEnabled);
		sbuf.append("\n pccModeConnectionType: ").append(mPccModeConnectionType);
		sbuf.append("\n Pairing bootstrapping config : ").append((mPairingBootstrappingConfig == null) ? "<null>" : mPairingBootstrappingConfig.toString());
		return sbuf.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(deviceAddress);
		dest.writeInt(wps.setup);
		dest.writeString(wps.BSSID);
		dest.writeString(wps.pin);
		dest.writeInt(groupOwnerIntent);
		dest.writeInt(netId);
		dest.writeString(networkName);
		dest.writeString(passphrase);
		dest.writeInt(groupOwnerBand);
		dest.writeInt(mGroupClientIpProvisioningMode);
		dest.writeBoolean(mJoinExistingGroup);
		dest.writeInt(mGroupOwnerVersion);
		dest.writeBoolean(mIsAuthorizeConnectionFromPeerEnabled);
		dest.writeInt(mPccModeConnectionType);
		dest.writeParcelable(mPairingBootstrappingConfig, flags);
	}

	public static final Creator<WifiP2pConfig> CREATOR = new Creator<WifiP2pConfig>() {
		public WifiP2pConfig createFromParcel(Parcel in) {
			WifiP2pConfig config = new WifiP2pConfig();
			config.deviceAddress = in.readString();
			config.wps = new WpsInfo();
			config.wps.setup = in.readInt();
			config.wps.BSSID = in.readString();
			config.wps.pin = in.readString();
			config.groupOwnerIntent = in.readInt();
			config.netId = in.readInt();
			config.networkName = in.readString();
			config.passphrase = in.readString();
			config.groupOwnerBand = in.readInt();
			config.mGroupClientIpProvisioningMode = in.readInt();
			config.mJoinExistingGroup = in.readBoolean();
			config.mGroupOwnerVersion = in.readInt();
			config.mIsAuthorizeConnectionFromPeerEnabled = in.readBoolean();
			config.mPccModeConnectionType = in.readInt();
			config.mPairingBootstrappingConfig = (WifiP2pPairingBootstrappingConfig) in.readParcelable(WifiP2pPairingBootstrappingConfig.class.getClassLoader());
			return config;
		}

		public WifiP2pConfig[] newArray(int size) {
			return new WifiP2pConfig[size];
		}
	};

	public static final class Builder {
		private static final MacAddress MAC_ANY_ADDRESS = MacAddress.fromString("02:00:00:00:00:00");
		private static final int MAX_SSID_BYTES = 32;

		private MacAddress mDeviceAddress = MAC_ANY_ADDRESS;
		private String mNetworkName = "";
		private String mPassphrase = "";
		private int mGroupOperatingBand = GROUP_OWNER_BAND_AUTO;
		private int mGroupOperatingFrequency = GROUP_OWNER_BAND_AUTO;
		private int mNetId = WifiP2pGroup.NETWORK_ID_TEMPORARY;
		private int mGroupClientIpProvisioningMode = GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP;
		private boolean mJoinExistingGroup = false;
		private int mPccModeConnectionType = PCC_MODE_DEFAULT_CONNECTION_TYPE_LEGACY_ONLY;
		private WifiP2pPairingBootstrappingConfig mPairingBootstrappingConfig;
		private boolean mIsAuthorizeConnectionFromPeerEnabled = false;

		public Builder() {}

		public Builder setDeviceAddress(MacAddress deviceAddress) {
			if (deviceAddress == null) {
				mDeviceAddress = MAC_ANY_ADDRESS;
			} else {
				mDeviceAddress = deviceAddress;
			}
			return this;
		}

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
			Objects.requireNonNull(passphrase, "passphrase cannot be null");
			if (TextUtils.isEmpty(passphrase)) {
				throw new IllegalArgumentException("passphrase must be non-empty.");
			}
			if (passphrase.length() > SAE_PASSWORD_MAX_LEN) {
				throw new IllegalArgumentException("The length of a passphrase must be less than 128");
			}
			mPassphrase = passphrase;
			return this;
		}

		public Builder setPccModeConnectionType(int connectionType) {
			switch (connectionType) {
				case PCC_MODE_CONNECTION_TYPE_LEGACY_ONLY:
				case PCC_MODE_CONNECTION_TYPE_LEGACY_OR_R2:
				case PCC_MODE_CONNECTION_TYPE_R2_ONLY:
					mPccModeConnectionType = connectionType;
					break;
				default:
					throw new IllegalArgumentException("Invalid constant for the PCC Mode connection type!");
			}
			return this;
		}

		public Builder setGroupOperatingBand(int band) {
			if (GROUP_OWNER_BAND_AUTO == band || GROUP_OWNER_BAND_2GHZ == band || GROUP_OWNER_BAND_5GHZ == band || GROUP_OWNER_BAND_6GHZ == band) {
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

		public Builder enablePersistentMode(boolean persistent) {
			if (persistent) {
				mNetId = WifiP2pGroup.NETWORK_ID_PERSISTENT;
			} else {
				mNetId = WifiP2pGroup.NETWORK_ID_TEMPORARY;
			}
			return this;
		}

		public Builder setGroupClientIpProvisioningMode(int groupClientIpProvisioningMode) {
			switch (groupClientIpProvisioningMode) {
				case GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP:
				case GROUP_CLIENT_IP_PROVISIONING_MODE_IPV6_LINK_LOCAL:
					mGroupClientIpProvisioningMode = groupClientIpProvisioningMode;
					break;
				default:
					throw new IllegalArgumentException("Invalid constant for the group client IP provisioning mode!");
			}
			return this;
		}

		public Builder setPairingBootstrappingConfig(WifiP2pPairingBootstrappingConfig config) {
			Objects.requireNonNull(config, "config cannot be null");
			mPairingBootstrappingConfig = config;
			return this;
		}

		public Builder setAuthorizeConnectionFromPeerEnabled(boolean enabled) {
			mIsAuthorizeConnectionFromPeerEnabled = enabled;
			return this;
		}

		public WifiP2pConfig build() {
			if ((TextUtils.isEmpty(mNetworkName) && !TextUtils.isEmpty(mPassphrase)) || (!TextUtils.isEmpty(mNetworkName) && TextUtils.isEmpty(mPassphrase))) {
				throw new IllegalStateException("network name and passphrase must be non-empty or empty both.");
			}
			if (TextUtils.isEmpty(mNetworkName) && mDeviceAddress.equals(MAC_ANY_ADDRESS)) {
				throw new IllegalStateException("peer address must be set if network name and passphrase are not set.");
			}
			if (!TextUtils.isEmpty(mNetworkName) && !TextUtils.isEmpty(mPassphrase)) {
				if (mPccModeConnectionType == PCC_MODE_CONNECTION_TYPE_LEGACY_ONLY || mPccModeConnectionType == PCC_MODE_CONNECTION_TYPE_LEGACY_OR_R2) {
					if (mPassphrase.length() < PSK_PASSWORD_MIN_LEN || mPassphrase.length() > PSK_PASSWORD_MAX_LEN) {
						throw new IllegalArgumentException("The length of a passphrase must be between " + PSK_PASSWORD_MIN_LEN + " and " + PSK_PASSWORD_MAX_LEN + " for legacy connection type");
					}
				}
			}
			if (mGroupOperatingFrequency > 0 && mGroupOperatingBand > 0) {
				throw new IllegalStateException("Preferred frequency and band are mutually exclusive.");
			}
			WifiP2pConfig config = new WifiP2pConfig();
			config.deviceAddress = mDeviceAddress.toString();
			config.networkName = mNetworkName;
			config.passphrase = mPassphrase;
			config.groupOwnerBand = GROUP_OWNER_BAND_AUTO;
			if (mGroupOperatingFrequency > 0) {
				config.groupOwnerBand = mGroupOperatingFrequency;
			} else if (mGroupOperatingBand > 0) {
				config.groupOwnerBand = mGroupOperatingBand;
			}
			config.netId = mNetId;
			config.mGroupClientIpProvisioningMode = mGroupClientIpProvisioningMode;
			config.mJoinExistingGroup = mJoinExistingGroup;
			config.mIsAuthorizeConnectionFromPeerEnabled = mIsAuthorizeConnectionFromPeerEnabled;
			config.mPccModeConnectionType = mPccModeConnectionType;
			if (mPairingBootstrappingConfig != null) {
				config.mPairingBootstrappingConfig = mPairingBootstrappingConfig;
				config.mGroupClientIpProvisioningMode = GROUP_CLIENT_IP_PROVISIONING_MODE_IPV6_LINK_LOCAL;
				config.wps.setup = WpsInfo.INVALID;
			}
			return config;
		}
	}
}
