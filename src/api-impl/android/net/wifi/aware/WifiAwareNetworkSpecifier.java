package android.net.wifi.aware;

/* AOSP frameworks/base/net/wifi/aware/WifiAwareNetworkSpecifier.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class WifiAwareNetworkSpecifier extends android.net.NetworkSpecifier implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public int getChannelFrequencyMhz() { return 0; }
    public android.net.wifi.aware.WifiAwareDataPathSecurityConfig getWifiAwareDataPathSecurityConfig() { return null; }
    public boolean isChannelRequired() { return false; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }

    public static final class Builder {
        public Builder(android.net.wifi.aware.DiscoverySession arg0, android.net.wifi.aware.PeerHandle arg1)  {}
        public Builder(android.net.wifi.aware.PublishDiscoverySession arg0)  {}
        public android.net.wifi.aware.WifiAwareNetworkSpecifier build() { return null; }
        public android.net.wifi.aware.WifiAwareNetworkSpecifier.Builder setChannelFrequencyMhz(int arg0, boolean arg1) { return this; }
        public android.net.wifi.aware.WifiAwareNetworkSpecifier.Builder setDataPathSecurityConfig(android.net.wifi.aware.WifiAwareDataPathSecurityConfig arg0) { return this; }
        public android.net.wifi.aware.WifiAwareNetworkSpecifier.Builder setPmk(byte[] arg0) { return this; }
        public android.net.wifi.aware.WifiAwareNetworkSpecifier.Builder setPort(int arg0) { return this; }
        public android.net.wifi.aware.WifiAwareNetworkSpecifier.Builder setPskPassphrase(String arg0) { return this; }
        public android.net.wifi.aware.WifiAwareNetworkSpecifier.Builder setTransportProtocol(int arg0) { return this; }
    }
}
