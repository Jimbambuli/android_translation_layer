package android.net.wifi.rtt;

/* AOSP frameworks/base/net/wifi/rtt/ResponderConfig.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class ResponderConfig implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public static android.net.wifi.rtt.ResponderConfig fromScanResult(android.net.wifi.ScanResult arg0) { return null; }
    public int getCenterFreq0Mhz() { return 0; }
    public int getCenterFreq1Mhz() { return 0; }
    public int getChannelWidth() { return 0; }
    public int getFrequencyMhz() { return 0; }
    public android.net.MacAddress getMacAddress() { return null; }
    public android.net.wifi.aware.PeerHandle getPeerHandle() { return null; }
    public int getPreamble() { return 0; }
    public int getResponderType() { return 0; }
    public android.net.wifi.rtt.SecureRangingConfig getSecureRangingConfig() { return null; }
    public boolean is80211azNtbSupported() { return false; }
    public boolean is80211mcSupported() { return false; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int RESPONDER_AP = 0; // 0x0;
    public static final int RESPONDER_STA = 1; // 0x1;

    public static final class Builder {
        public Builder()  {}
        public Builder(android.net.wifi.rtt.ResponderConfig arg0)  {}
        public android.net.wifi.rtt.ResponderConfig build() { return null; }
        public android.net.wifi.rtt.ResponderConfig.Builder set80211azNtbSupported(boolean arg0) { return null; }
        public android.net.wifi.rtt.ResponderConfig.Builder set80211mcSupported(boolean arg0) { return null; }
        public android.net.wifi.rtt.ResponderConfig.Builder setCenterFreq0Mhz(int arg0) { return null; }
        public android.net.wifi.rtt.ResponderConfig.Builder setCenterFreq1Mhz(int arg0) { return null; }
        public android.net.wifi.rtt.ResponderConfig.Builder setChannelWidth(int arg0) { return this; }
        public android.net.wifi.rtt.ResponderConfig.Builder setFrequencyMhz(int arg0) { return this; }
        public android.net.wifi.rtt.ResponderConfig.Builder setMacAddress(android.net.MacAddress arg0) { return this; }
        public android.net.wifi.rtt.ResponderConfig.Builder setPeerHandle(android.net.wifi.aware.PeerHandle arg0) { return this; }
        public android.net.wifi.rtt.ResponderConfig.Builder setPreamble(int arg0) { return this; }
        public android.net.wifi.rtt.ResponderConfig.Builder setResponderType(int arg0) { return this; }
        public android.net.wifi.rtt.ResponderConfig.Builder setSecureRangingConfig(android.net.wifi.rtt.SecureRangingConfig arg0) { return this; }
    }
}
