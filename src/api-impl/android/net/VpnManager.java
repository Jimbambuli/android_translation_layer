package android.net;

/* AOSP frameworks/base/net/VpnManager.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public class VpnManager {
    public void deleteProvisionedVpnProfile() {  }
    public android.net.VpnProfileState getProvisionedVpnProfileState() { return null; }
    public android.content.Intent provisionVpnProfile(android.net.PlatformVpnProfile arg0) { return null; }
    @Deprecated public void startProvisionedVpnProfile() {  }
    public String startProvisionedVpnProfileSession() { return null; }
    public void stopProvisionedVpnProfile() {  }
    public static final String ACTION_VPN_MANAGER_EVENT = "android.net.action.VPN_MANAGER_EVENT";
    public static final String CATEGORY_EVENT_ALWAYS_ON_STATE_CHANGED = "android.net.category.EVENT_ALWAYS_ON_STATE_CHANGED";
    public static final String CATEGORY_EVENT_DEACTIVATED_BY_USER = "android.net.category.EVENT_DEACTIVATED_BY_USER";
    public static final String CATEGORY_EVENT_IKE_ERROR = "android.net.category.EVENT_IKE_ERROR";
    public static final String CATEGORY_EVENT_NETWORK_ERROR = "android.net.category.EVENT_NETWORK_ERROR";
    public static final int ERROR_CLASS_NOT_RECOVERABLE = 1; // 0x1;
    public static final int ERROR_CLASS_RECOVERABLE = 2; // 0x2;
    public static final int ERROR_CODE_NETWORK_IO = 3; // 0x3;
    public static final int ERROR_CODE_NETWORK_LOST = 2; // 0x2;
    public static final int ERROR_CODE_NETWORK_PROTOCOL_TIMEOUT = 1; // 0x1;
    public static final int ERROR_CODE_NETWORK_UNKNOWN_HOST = 0; // 0x0;
    public static final String EXTRA_ERROR_CLASS = "android.net.extra.ERROR_CLASS";
    public static final String EXTRA_ERROR_CODE = "android.net.extra.ERROR_CODE";
    public static final String EXTRA_SESSION_KEY = "android.net.extra.SESSION_KEY";
    public static final String EXTRA_TIMESTAMP_MILLIS = "android.net.extra.TIMESTAMP_MILLIS";
    public static final String EXTRA_UNDERLYING_LINK_PROPERTIES = "android.net.extra.UNDERLYING_LINK_PROPERTIES";
    public static final String EXTRA_UNDERLYING_NETWORK = "android.net.extra.UNDERLYING_NETWORK";
    public static final String EXTRA_UNDERLYING_NETWORK_CAPABILITIES = "android.net.extra.UNDERLYING_NETWORK_CAPABILITIES";
    public static final String EXTRA_VPN_PROFILE_STATE = "android.net.extra.VPN_PROFILE_STATE";
}
