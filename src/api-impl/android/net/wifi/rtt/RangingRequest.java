package android.net.wifi.rtt;

/* AOSP frameworks/base/net/wifi/rtt/RangingRequest.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class RangingRequest implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public static int getDefaultRttBurstSize() { return 0; }
    public static int getMaxPeers() { return 0; }
    public static int getMaxRttBurstSize() { return 0; }
    public static int getMinRttBurstSize() { return 0; }
    public int getRttBurstSize() { return 0; }
    public int getSecurityMode() { return 0; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int SECURITY_MODE_OPEN = 0; // 0x0;
    public static final int SECURITY_MODE_OPPORTUNISTIC = 1; // 0x1;
    public static final int SECURITY_MODE_SECURE_AUTH = 2; // 0x2;

    public static final class Builder {
        public Builder()  {}
        public android.net.wifi.rtt.RangingRequest.Builder addAccessPoint(android.net.wifi.ScanResult arg0) { return this; }
        public android.net.wifi.rtt.RangingRequest.Builder addAccessPoints(java.util.List<android.net.wifi.ScanResult> arg0) { return this; }
        public android.net.wifi.rtt.RangingRequest.Builder addNon80211mcCapableAccessPoint(android.net.wifi.ScanResult arg0) { return null; }
        public android.net.wifi.rtt.RangingRequest.Builder addNon80211mcCapableAccessPoints(java.util.List<android.net.wifi.ScanResult> arg0) { return null; }
        public android.net.wifi.rtt.RangingRequest.Builder addResponder(android.net.wifi.rtt.ResponderConfig arg0) { return this; }
        public android.net.wifi.rtt.RangingRequest.Builder addResponders(java.util.List<android.net.wifi.rtt.ResponderConfig> arg0) { return this; }
        public android.net.wifi.rtt.RangingRequest.Builder addWifiAwarePeer(android.net.MacAddress arg0) { return this; }
        public android.net.wifi.rtt.RangingRequest.Builder addWifiAwarePeer(android.net.wifi.aware.PeerHandle arg0) { return this; }
        public android.net.wifi.rtt.RangingRequest build() { return null; }
        public android.net.wifi.rtt.RangingRequest.Builder setRttBurstSize(int arg0) { return this; }
        public android.net.wifi.rtt.RangingRequest.Builder setSecurityMode(int arg0) { return this; }
    }
}
