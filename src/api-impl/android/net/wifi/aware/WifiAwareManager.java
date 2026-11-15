package android.net.wifi.aware;

/* AOSP frameworks/base/net/wifi/aware/WifiAwareManager.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public class WifiAwareManager {
    public void attach(android.net.wifi.aware.AttachCallback arg0, android.os.Handler arg1) {  }
    public void attach(android.net.wifi.aware.AttachCallback arg0, android.net.wifi.aware.IdentityChangedListener arg1, android.os.Handler arg2) {  }
    public android.net.wifi.aware.AwareResources getAvailableAwareResources() { return null; }
    public android.net.wifi.aware.Characteristics getCharacteristics() { return null; }
    public void getPairedDevices(java.util.concurrent.Executor arg0, java.util.function.Consumer<java.util.List<java.lang.String>> arg1) {  }
    public boolean isAvailable() { return false; }
    public boolean isDeviceAttached() { return false; }
    public boolean isInstantCommunicationModeEnabled() { return false; }
    public void isOpportunisticModeEnabled(java.util.concurrent.Executor arg0, java.util.function.Consumer<java.lang.Boolean> arg1) {  }
    public boolean isSetChannelOnDataPathSupported() { return false; }
    public void removePairedDevice(String arg0) {  }
    public void resetPairedDevices() {  }
    public void setOpportunisticModeEnabled(boolean arg0) {  }
    public static final String ACTION_WIFI_AWARE_RESOURCE_CHANGED = "android.net.wifi.aware.action.WIFI_AWARE_RESOURCE_CHANGED";
    public static final String ACTION_WIFI_AWARE_STATE_CHANGED = "android.net.wifi.aware.action.WIFI_AWARE_STATE_CHANGED";
    public static final String EXTRA_AWARE_RESOURCES = "android.net.wifi.aware.extra.AWARE_RESOURCES";
    public static final int WIFI_AWARE_DATA_PATH_ROLE_INITIATOR = 0; // 0x0;
    public static final int WIFI_AWARE_DATA_PATH_ROLE_RESPONDER = 1; // 0x1;
    public static final int WIFI_AWARE_DISCOVERY_LOST_REASON_PEER_NOT_VISIBLE = 1; // 0x1;
    public static final int WIFI_AWARE_DISCOVERY_LOST_REASON_UNKNOWN = 0; // 0x0;
}
