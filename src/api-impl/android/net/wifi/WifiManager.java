package android.net.wifi;

import android.net.DhcpInfo;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.WorkSource;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class WifiManager {

	public static final int WIFI_STATE_DISABLED = 1;
	public static final int WIFI_STATE_DISABLING = 0;
	public static final int WIFI_STATE_ENABLED = 3;
	public static final int WIFI_STATE_ENABLING = 2;
	public static final int WIFI_STATE_UNKNOWN = 4;

	public static final String UNKNOWN_SSID = "<unknown ssid>";

	/* constants of WifiManager in modules_Wifi/framework/api/current.txt (values as there) */
	public static final String ACTION_PICK_WIFI_NETWORK = "android.net.wifi.PICK_WIFI_NETWORK";
	public static final int ACTION_REMOVE_SUGGESTION_DISCONNECT = 2;
	public static final int ACTION_REMOVE_SUGGESTION_LINGER = 1;
	public static final String ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE = "android.net.wifi.action.REQUEST_SCAN_ALWAYS_AVAILABLE";
	public static final String ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION = "android.net.wifi.action.WIFI_NETWORK_SUGGESTION_POST_CONNECTION";
	public static final String ACTION_WIFI_SCAN_AVAILABILITY_CHANGED = "android.net.wifi.action.WIFI_SCAN_AVAILABILITY_CHANGED";
	public static final String CHANNEL_DATA_KEY_FREQUENCY_MHZ = "CHANNEL_DATA_KEY_FREQUENCY_MHZ";
	public static final String CHANNEL_DATA_KEY_NUM_AP = "CHANNEL_DATA_KEY_NUM_AP";
	public static final int ERROR_AUTHENTICATING = 1;
	public static final String EXTRA_BSSID = "bssid";
	public static final String EXTRA_NETWORK_INFO = "networkInfo";
	public static final String EXTRA_NETWORK_SUGGESTION = "android.net.wifi.extra.NETWORK_SUGGESTION";
	public static final String EXTRA_NEW_RSSI = "newRssi";
	public static final String EXTRA_NEW_STATE = "newState";
	public static final String EXTRA_PREVIOUS_WIFI_STATE = "previous_wifi_state";
	public static final String EXTRA_RESULTS_UPDATED = "resultsUpdated";
	public static final String EXTRA_SCAN_AVAILABLE = "android.net.wifi.extra.SCAN_AVAILABLE";
	public static final String EXTRA_SUPPLICANT_CONNECTED = "connected";
	public static final String EXTRA_SUPPLICANT_ERROR = "supplicantError";
	public static final String EXTRA_WIFI_INFO = "wifiInfo";
	public static final String EXTRA_WIFI_STATE = "wifi_state";
	public static final int FLAG_SEND_DHCP_HOSTNAME_RESTRICTION_OPEN = 1;
	public static final int FLAG_SEND_DHCP_HOSTNAME_RESTRICTION_SECURE = 2;
	public static final String NETWORK_IDS_CHANGED_ACTION = "android.net.wifi.NETWORK_IDS_CHANGED";
	public static final String NETWORK_STATE_CHANGED_ACTION = "android.net.wifi.STATE_CHANGE";
	public static final int ROAMING_MODE_AGGRESSIVE = 2;
	public static final int ROAMING_MODE_NONE = 0;
	public static final int ROAMING_MODE_NORMAL = 1;
	public static final String RSSI_CHANGED_ACTION = "android.net.wifi.RSSI_CHANGED";
	public static final String SCAN_RESULTS_AVAILABLE_ACTION = "android.net.wifi.SCAN_RESULTS";
	public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_ASSOCIATION = 1;
	public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_AUTHENTICATION = 2;
	public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_IP_PROVISIONING = 3;
	public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_NOT_FOUND = 4;
	public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_NO_RESPONSE = 5;
	public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_UNKNOWN = 0;
	public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_USER_REJECT = 6;
	public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE = 3;
	public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP = 4;
	public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID = 7;
	public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED = 6;
	public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED = 2;
	public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL = 1;
	public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID = 5;
	public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_RESTRICTED_BY_ADMIN = 8;
	public static final int STATUS_NETWORK_SUGGESTIONS_SUCCESS = 0;
	public static final int STATUS_SUGGESTION_APPROVAL_APPROVED_BY_CARRIER_PRIVILEGE = 4;
	public static final int STATUS_SUGGESTION_APPROVAL_APPROVED_BY_USER = 2;
	public static final int STATUS_SUGGESTION_APPROVAL_PENDING = 1;
	public static final int STATUS_SUGGESTION_APPROVAL_REJECTED_BY_USER = 3;
	public static final int STATUS_SUGGESTION_APPROVAL_UNKNOWN = 0;
	public static final int STATUS_SUGGESTION_CONNECTION_FAILURE_ASSOCIATION = 1;
	public static final int STATUS_SUGGESTION_CONNECTION_FAILURE_AUTHENTICATION = 2;
	public static final int STATUS_SUGGESTION_CONNECTION_FAILURE_IP_PROVISIONING = 3;
	public static final int STATUS_SUGGESTION_CONNECTION_FAILURE_UNKNOWN = 0;
	public static final String SUPPLICANT_CONNECTION_CHANGE_ACTION = "android.net.wifi.supplicant.CONNECTION_CHANGE";
	public static final String SUPPLICANT_STATE_CHANGED_ACTION = "android.net.wifi.supplicant.STATE_CHANGE";
	public static final int WIFI_INTERFACE_TYPE_AP = 1;
	public static final int WIFI_INTERFACE_TYPE_AWARE = 2;
	public static final int WIFI_INTERFACE_TYPE_DIRECT = 3;
	public static final int WIFI_INTERFACE_TYPE_STA = 0;
	public static final int WIFI_MODE_FULL = 1;
	public static final int WIFI_MODE_FULL_HIGH_PERF = 3;
	public static final int WIFI_MODE_FULL_LOW_LATENCY = 4;
	public static final int WIFI_MODE_SCAN_ONLY = 2;
	public static final int WIFI_MULTI_INTERNET_MODE_DBS_AP = 1;
	public static final int WIFI_MULTI_INTERNET_MODE_DISABLED = 0;
	public static final int WIFI_MULTI_INTERNET_MODE_MULTI_AP = 2;
	public static final String WIFI_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_STATE_CHANGED";
	public static final int WPS_AUTH_FAILURE = 6;
	public static final int WPS_OVERLAP_ERROR = 3;
	public static final int WPS_TIMED_OUT = 7;
	public static final int WPS_TKIP_ONLY_PROHIBITED = 5;
	public static final int WPS_WEP_PROHIBITED = 4;

	/* AOSP WifiManager.WifiLock: the reference counting of acquire()/release(); there is no Wi-Fi power saving
	 * on the desktop for the lock to hold off */
	public class WifiLock {
		private int mRefCount;
		private boolean mRefCounted = true;
		private boolean mHeld;

		public void setReferenceCounted(boolean refCounted) {
			mRefCounted = refCounted;
		}

		public void acquire() {
			synchronized (this) {
				if (mRefCounted ? (++mRefCount == 1) : (!mHeld))
					mHeld = true;
			}
		}

		public void release() {
			synchronized (this) {
				if (mRefCounted ? (--mRefCount == 0) : (mHeld))
					mHeld = false;
				if (mRefCount < 0)
					throw new RuntimeException("WifiLock under-locked");
			}
		}

		public boolean isHeld() {
			synchronized (this) {
				return mHeld;
			}
		}

		/* AOSP WifiLock.setWorkSource(): only attributes the lock for battery statistics, no observable effect
		 * without a power-saving Wi-Fi to hold */
		public void setWorkSource(WorkSource ws) {}
	}

	/* AOSP WifiManager.MulticastLock: the same reference counting as WifiLock; ATL does not filter multicast */
	public class MulticastLock {
		private int mRefCount;
		private boolean mRefCounted = true;
		private boolean mHeld;

		public void setReferenceCounted(boolean refCounted) {
			mRefCounted = refCounted;
		}

		public void acquire() {
			synchronized (this) {
				if (mRefCounted ? (++mRefCount == 1) : (!mHeld))
					mHeld = true;
			}
		}

		public void release() {
			synchronized (this) {
				if (mRefCounted ? (--mRefCount == 0) : (mHeld))
					mHeld = false;
				if (mRefCount < 0)
					throw new RuntimeException("MulticastLock under-locked");
			}
		}

		public boolean isHeld() {
			synchronized (this) {
				return mHeld;
			}
		}
	}

	/* AOSP WifiManager.AddNetworkResult */
	public static final class AddNetworkResult implements Parcelable {
		public static final int STATUS_SUCCESS = 0;
		public static final int STATUS_FAILURE_UNKNOWN = 1;
		public static final int STATUS_NO_PERMISSION = 2;
		public static final int STATUS_ADD_PASSPOINT_FAILURE = 3;
		public static final int STATUS_ADD_WIFI_CONFIG_FAILURE = 4;
		public static final int STATUS_INVALID_CONFIGURATION = 5;
		public static final int STATUS_NO_PERMISSION_MODIFY_CONFIG = 6;
		public static final int STATUS_NO_PERMISSION_MODIFY_PROXY_SETTING = 7;
		public static final int STATUS_NO_PERMISSION_MODIFY_MAC_RANDOMIZATION = 8;
		public static final int STATUS_FAILURE_UPDATE_NETWORK_KEYS = 9;
		public static final int STATUS_INVALID_CONFIGURATION_ENTERPRISE = 10;

		public final int statusCode;
		public final int networkId;

		public AddNetworkResult(int statusCode, int networkId) {
			this.statusCode = statusCode;
			this.networkId = networkId;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(statusCode);
			dest.writeInt(networkId);
		}

		public static final Creator<AddNetworkResult> CREATOR = new Creator<AddNetworkResult>() {
			public AddNetworkResult createFromParcel(Parcel in) {
				return new AddNetworkResult(in.readInt(), in.readInt());
			}

			public AddNetworkResult[] newArray(int size) {
				return new AddNetworkResult[size];
			}
		};
	}

	/* AOSP WifiManager.InterfaceCreationImpact */
	public static class InterfaceCreationImpact {
		private final int mInterfaceType;
		private final Set<String> mPackages;

		public InterfaceCreationImpact(int interfaceType, Set<String> packages) {
			mInterfaceType = interfaceType;
			mPackages = packages;
		}

		public int getInterfaceType() {
			return mInterfaceType;
		}

		public Set<String> getPackages() {
			return mPackages;
		}

		@Override
		public int hashCode() {
			return Objects.hash(mInterfaceType, mPackages);
		}

		@Override
		public boolean equals(Object that) {
			if (this == that)
				return true;
			if (!(that instanceof InterfaceCreationImpact))
				return false;
			InterfaceCreationImpact other = (InterfaceCreationImpact) that;
			return this.mInterfaceType == other.mInterfaceType && Objects.equals(this.mPackages, other.mPackages);
		}
	}

	/* AOSP WifiManager.LocalOnlyConnectionFailureListener */
	public static interface LocalOnlyConnectionFailureListener {
		void onConnectionFailed(WifiNetworkSpecifier specifier, int failureReason);
	}

	/* AOSP WifiManager.SuggestionConnectionStatusListener */
	public static interface SuggestionConnectionStatusListener {
		void onConnectionStatus(WifiNetworkSuggestion wifiNetworkSuggestion, int failureReason);
	}

	/* AOSP WifiManager.SuggestionUserApprovalStatusListener */
	public static interface SuggestionUserApprovalStatusListener {
		void onUserApprovalStatusChange(int status);
	}

	/* AOSP WifiManager.WifiStateChangedListener */
	public static interface WifiStateChangedListener {
		void onWifiStateChanged();
	}

	/* AOSP WifiManager.ScanResultsCallback (the binder proxy of AOSP is left out: no scan results ever arrive) */
	public abstract static class ScanResultsCallback {
		public ScanResultsCallback() {}

		public abstract void onScanResultsAvailable();
	}

	/* AOSP WifiManager.SubsystemRestartTrackingCallback (binder proxy left out: the subsystem never restarts) */
	public abstract static class SubsystemRestartTrackingCallback {
		public SubsystemRestartTrackingCallback() {}

		public abstract void onSubsystemRestarting();

		public abstract void onSubsystemRestarted();
	}

	/* AOSP WifiManager.WpsCallback */
	@Deprecated
	public abstract static class WpsCallback {
		@Deprecated
		public WpsCallback() {}

		@Deprecated
		public abstract void onStarted(String pin);

		@Deprecated
		public abstract void onSucceeded();

		@Deprecated
		public abstract void onFailed(int reason);
	}

	/* AOSP WifiManager.LocalOnlyHotspotCallback; REQUEST_REGISTERED is the hidden constant of AOSP */
	public static class LocalOnlyHotspotCallback {
		public static final int REQUEST_REGISTERED = 0;
		public static final int ERROR_NO_CHANNEL = 1;
		public static final int ERROR_GENERIC = 2;
		public static final int ERROR_INCOMPATIBLE_MODE = 3;
		public static final int ERROR_TETHERING_DISALLOWED = 4;

		public LocalOnlyHotspotCallback() {}

		public void onStarted(LocalOnlyHotspotReservation reservation) {}

		public void onStopped() {}

		public void onFailed(int reason) {}
	}

	/* AOSP WifiManager.LocalOnlyHotspotReservation: close() ends the request (stopLocalOnlyHotspot(), nothing to
	 * stop here) */
	public class LocalOnlyHotspotReservation implements AutoCloseable {
		private final SoftApConfiguration mSoftApConfig;
		private final WifiConfiguration mWifiConfig;
		private boolean mClosed = false;

		public LocalOnlyHotspotReservation(SoftApConfiguration config) {
			mSoftApConfig = config;
			mWifiConfig = config.toWifiConfiguration();
		}

		@Deprecated
		public WifiConfiguration getWifiConfiguration() {
			return mWifiConfig;
		}

		public SoftApConfiguration getSoftApConfiguration() {
			return mSoftApConfig;
		}

		@Override
		public void close() {
			mClosed = true;
		}
	}

	public MulticastLock createMulticastLock(String tag) {
		return new MulticastLock();
	}

	public WifiLock createWifiLock(int lockType, String tag) {
		return new WifiLock();
	}

	public WifiLock createWifiLock(String tag) {
		return new WifiLock();
	}

	public WifiInfo getConnectionInfo() {
		return new WifiInfo();
	}

	/* like WifiServiceImpl without a DHCP lease: all fields 0 */
	@Deprecated
	public DhcpInfo getDhcpInfo() {
		return new DhcpInfo();
	}

	public int getWifiState() {
		return WIFI_STATE_UNKNOWN;
	}

	public boolean isWifiEnabled() {
		return false;
	}

	/* no Wi-Fi control from inside the sandbox: AOSP returns false when the request is not carried out */
	public boolean setWifiEnabled(boolean enabled) {
		return false;
	}

	public boolean startScan() {
		return false;
	}

	public java.util.List getScanResults() {
		return new java.util.ArrayList();
	}

	public boolean is5GHzBandSupported() {
		return false;
	}

	public boolean is6GHzBandSupported() {
		return false;
	}

	public boolean isScanThrottleEnabled() {
		return false;
	}

	/* hidden constants of AOSP WifiManager.java */
	public static final int NETWORK_SUGGESTIONS_MAX_PER_APP_LOW_RAM = 256;
	public static final int NETWORK_SUGGESTIONS_MAX_PER_APP_HIGH_RAM = 1024;

	private static final int MAX_RSSI = -55;

	private static final int MIN_RSSI = -100;

	/* AOSP WifiManager.calculateSignalLevel(int, int): MIN_RSSI is the private WifiManager.MIN_RSSI = -100 there
	 * (not WifiInfo.MIN_RSSI) */
	@Deprecated
	public static int calculateSignalLevel(int rssi, int numLevels) {
		if (rssi <= MIN_RSSI) {
			return 0;
		} else if (rssi >= MAX_RSSI) {
			return numLevels - 1;
		} else {
			float inputRange = (MAX_RSSI - MIN_RSSI);
			float outputRange = (numLevels - 1);
			return (int) ((float) (rssi - MIN_RSSI) * outputRange / inputRange);
		}
	}

	/* AOSP WifiManager.calculateSignalLevel(int) -> WifiServiceImpl -> RssiUtil.calculateSignalLevel(): level =
	 * number of config_wifiRssiLevelThresholds above the rssi. The thresholds -88, -77, -66, -55 are the AOSP
	 * default of that overlay (the resource file is not in the source tree here: from memory, not verified) */
	private static final int[] RSSI_LEVEL_THRESHOLDS = { -88, -77, -66, -55 };

	public int calculateSignalLevel(int rssi) {
		for (int level = 0; level < RSSI_LEVEL_THRESHOLDS.length; level++) {
			if (rssi < RSSI_LEVEL_THRESHOLDS[level]) {
				return level;
			}
		}
		return RSSI_LEVEL_THRESHOLDS.length;
	}

	/* AOSP WifiManager.getMaxSignalLevel(): calculateSignalLevel(Integer.MAX_VALUE) */
	public int getMaxSignalLevel() {
		return calculateSignalLevel(Integer.MAX_VALUE);
	}

	/* AOSP WifiManager.compareSignalLevel(int, int) */
	public static int compareSignalLevel(int rssiA, int rssiB) {
		return rssiA - rssiB;
	}

	/* no Wi-Fi control from inside the sandbox (see setWifiEnabled): the request is not carried out */
	@Deprecated
	public boolean enableNetwork(int netId, boolean attemptConnect) {
		return false;
	}

	@Deprecated
	public boolean disconnect() {
		return false;
	}

	@Deprecated
	public boolean reconnect() {
		return false;
	}

	/* AOSP WifiServiceImpl.addOrUpdateNetwork(): -1 for apps targeting Android 10 or later without privileges
	 * (and, in WifiManager.addNetwork(), for a null config) */
	@Deprecated
	public int addNetwork(WifiConfiguration config) {
		return -1;
	}

	/* AOSP WifiServiceImpl.removeNetwork(): false for apps targeting Android 10 or later without privileges */
	@Deprecated
	public boolean removeNetwork(int netId) {
		return false;
	}

	/* AOSP WifiManager.saveConfiguration(): always false */
	@Deprecated
	public boolean saveConfiguration() {
		return false;
	}

	/* AOSP: needs FEATURE_WIFI_DIRECT, which a device without Wi-Fi hardware does not declare */
	public boolean isP2pSupported() {
		return false;
	}

	/* Capability queries. AOSP answers them from WifiServiceImpl.isFeatureSupported() (the feature bitset of the
	 * Wi-Fi chip) and from the band/standard queries of the chip: a device without Wi-Fi hardware has no feature set
	 * and no band, so everything is false (as is5GHzBandSupported() above). */

	public boolean is24GHzBandSupported() {
		return false;
	}

	public boolean is60GHzBandSupported() {
		return false;
	}

	public boolean isAggressiveRoamingModeSupported() {
		return false;
	}

	public boolean isBridgedApConcurrencySupported() {
		return false;
	}

	public boolean isD2dSupportedWhenInfraStaDisabled() {
		return false;
	}

	public boolean isDecoratedIdentitySupported() {
		return false;
	}

	@Deprecated
	public boolean isDeviceToApRttSupported() {
		return false;
	}

	public boolean isDualBandSimultaneousSupported() {
		return false;
	}

	public boolean isEasyConnectDppAkmSupported() {
		return false;
	}

	public boolean isEasyConnectEnrolleeResponderModeSupported() {
		return false;
	}

	public boolean isEasyConnectSupported() {
		return false;
	}

	public boolean isEnhancedOpenSupported() {
		return false;
	}

	public boolean isEnhancedPowerReportingSupported() {
		return false;
	}

	public boolean isMakeBeforeBreakWifiSwitchingSupported() {
		return false;
	}

	public boolean isPasspointTermsAndConditionsSupported() {
		return false;
	}

	public boolean isPreferredNetworkOffloadSupported() {
		return false;
	}

	public boolean isStaApConcurrencySupported() {
		return false;
	}

	public boolean isStaBridgedApConcurrencySupported() {
		return false;
	}

	public boolean isStaConcurrencyForLocalOnlyConnectionsSupported() {
		return false;
	}

	public boolean isStaConcurrencyForMultiInternetSupported() {
		return false;
	}

	public boolean isTdlsSupported() {
		return false;
	}

	public boolean isTidToLinkMappingNegotiationSupported() {
		return false;
	}

	public boolean isTlsMinimumVersionSupported() {
		return false;
	}

	public boolean isTlsV13Supported() {
		return false;
	}

	public boolean isTrustOnFirstUseSupported() {
		return false;
	}

	public boolean isWapiSupported() {
		return false;
	}

	public boolean isWepSupported() {
		return false;
	}

	public boolean isWifiDisplayR2Supported() {
		return false;
	}

	public boolean isWifiStandardSupported(int standard) {
		return false;
	}

	public boolean isWpa3SaeH2eSupported() {
		return false;
	}

	/* AOSP WifiManager.isWpa3SaePublicKeySupported(): returns false on every device */
	public boolean isWpa3SaePublicKeySupported() {
		return false;
	}

	public boolean isWpa3SaeSupported() {
		return false;
	}

	public boolean isWpa3SuiteBSupported() {
		return false;
	}

	public boolean isWpaPersonalSupported() {
		return false;
	}

	/* AOSP WifiServiceImpl: settings of the Wi-Fi service, which is off (isWifiEnabled() == false) here */
	@Deprecated
	public boolean isScanAlwaysAvailable() {
		return false;
	}

	public boolean isAutoWakeupEnabled() {
		return false;
	}

	public boolean isWifiPasspointEnabled() {
		return false;
	}

	/* AOSP WifiCarrierInfoManager.isCarrierNetworkOffloadEnabled(): no carrier subscription, no offload */
	public boolean isCarrierNetworkOffloadEnabled(int subscriptionId, boolean merged) {
		return false;
	}

	/* AOSP WifiServiceImpl.getStaConcurrencyForMultiInternetMode(): multi-internet is a STA feature */
	public int getStaConcurrencyForMultiInternetMode() {
		return WIFI_MULTI_INTERNET_MODE_DISABLED;
	}

	/* AOSP WifiManager.pingSupplicant(): isWifiEnabled() */
	@Deprecated
	public boolean pingSupplicant() {
		return isWifiEnabled();
	}

	/* AOSP WifiServiceImpl.reassociate() / disableNetwork(): false for apps targeting Android 10 or later without
	 * privileges (as removeNetwork() above) */
	@Deprecated
	public boolean reassociate() {
		return false;
	}

	@Deprecated
	public boolean disableNetwork(int netId) {
		return false;
	}

	/* AOSP WifiManager.updateNetwork(): -1 for null or networkId < 0, otherwise addOrUpdateNetwork(), which is -1
	 * for apps targeting Android 10 or later without privileges (see addNetwork()) */
	@Deprecated
	public int updateNetwork(WifiConfiguration config) {
		return -1;
	}

	/* AOSP WifiManager.addNetworkPrivileged(): null is an IllegalArgumentException; WifiServiceImpl
	 * .addOrUpdateNetworkPrivileged() throws SecurityException unless the caller is DO, PO, system or privileged */
	public AddNetworkResult addNetworkPrivileged(WifiConfiguration config) {
		if (config == null)
			throw new IllegalArgumentException("config cannot be null");
		throw new SecurityException("Caller is not a device owner, profile owner, system app, or privileged app");
	}

	/* AOSP WifiServiceImpl.getConfiguredNetworks(): empty list for apps targeting Android 10 or later without
	 * privileges */
	@Deprecated
	public List<WifiConfiguration> getConfiguredNetworks() {
		return Collections.emptyList();
	}

	/* AOSP WifiServiceImpl.getConfiguredNetworks(callerNetworksOnly = true): only DO, PO, carrier or privileged apps */
	public List<WifiConfiguration> getCallerConfiguredNetworks() {
		throw new SecurityException("Not a DO, PO, carrier or privileged app");
	}

	/* AOSP WifiServiceImpl.removeNonCallerConfiguredNetworks(): only for the DO/PO of an organization owned device */
	public boolean removeNonCallerConfiguredNetworks() {
		throw new SecurityException("Caller is not device owner or profile owner of an organization owned device");
	}

	/* Network suggestions: AOSP WifiServiceImpl.addNetworkSuggestions()/removeNetworkSuggestions() answer
	 * STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED when the app may not change Wi-Fi state; without any Wi-Fi
	 * the request is not carried out in the same way. (AOSP has no "service not there" case here: not unambiguous.) */
	public int addNetworkSuggestions(List<WifiNetworkSuggestion> networkSuggestions) {
		return STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED;
	}

	public int removeNetworkSuggestions(List<WifiNetworkSuggestion> networkSuggestions) {
		return removeNetworkSuggestions(networkSuggestions, ACTION_REMOVE_SUGGESTION_DISCONNECT);
	}

	public int removeNetworkSuggestions(List<WifiNetworkSuggestion> networkSuggestions, int action) {
		return STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED;
	}

	/* AOSP WifiNetworkSuggestionsManager.get(): the suggestions of the calling app, none were added */
	public List<WifiNetworkSuggestion> getNetworkSuggestions() {
		return Collections.emptyList();
	}

	/* AOSP WifiManager.getMaxNumberOfNetworkSuggestionsPerApp(): the high-RAM value (a desktop is no low-RAM device) */
	public int getMaxNumberOfNetworkSuggestionsPerApp() {
		return getMaxNumberOfNetworkSuggestionsPerApp(false);
	}

	/* AOSP WifiManager.getMaxNumberOfNetworkSuggestionsPerApp(boolean) */
	public static int getMaxNumberOfNetworkSuggestionsPerApp(boolean isLowRamDevice) {
		return isLowRamDevice ? NETWORK_SUGGESTIONS_MAX_PER_APP_LOW_RAM : NETWORK_SUGGESTIONS_MAX_PER_APP_HIGH_RAM;
	}

	/* AOSP WifiServiceImpl.getMaxNumberOfChannelsPerRequest(): overlay config_wifiNetworkSpecifierMaxPreferredChannels,
	 * AOSP default 5 (the resource file is not in the source tree here: from memory, not verified) */
	public int getMaxNumberOfChannelsPerNetworkSpecifierRequest() {
		return 5;
	}

	/* Passpoint. AOSP WifiManager.addOrUpdatePasspointConfiguration()/removePasspointConfiguration() throw
	 * IllegalArgumentException when the service returns false (not allowed for apps targeting Android 11 or later
	 * without privileges; no such profile to remove); WifiServiceImpl.getPasspointConfigurations() is the list of
	 * the profiles the app may see (none); flushPasspointAnqpCache() needs DO/PO or NETWORK_SETTINGS and the like */
	public void addOrUpdatePasspointConfiguration(PasspointConfiguration config) {
		throw new IllegalArgumentException();
	}

	@Deprecated
	public void removePasspointConfiguration(String fqdn) {
		throw new IllegalArgumentException();
	}

	@Deprecated
	public List<PasspointConfiguration> getPasspointConfigurations() {
		return Collections.emptyList();
	}

	public void flushPasspointAnqpCache() {
		throw new SecurityException("Caller is not a device or profile owner and holds none of NETWORK_SETTINGS, "
				+ "NETWORK_MANAGED_PROVISIONING, NETWORK_CARRIER_PROVISIONING");
	}

	/* WPS: AOSP WifiManager.startWps()/cancelWps() report ActionListener.FAILURE_INTERNAL_ERROR (= 0), WPS is gone */
	@Deprecated
	public void startWps(WpsInfo config, WpsCallback listener) {
		if (listener != null)
			listener.onFailed(0);
	}

	@Deprecated
	public void cancelWps(WpsCallback listener) {
		if (listener != null)
			listener.onFailed(0);
	}
}
