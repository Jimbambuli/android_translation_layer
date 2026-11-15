package android.net.wifi;

import android.net.DhcpInfo;

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
}
