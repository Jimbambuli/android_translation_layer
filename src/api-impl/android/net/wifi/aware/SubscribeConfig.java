package android.net.wifi.aware;

/* AOSP frameworks/base/net/wifi/aware/SubscribeConfig.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class SubscribeConfig implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public int getInstantCommunicationBand() { return 0; }
    public android.net.wifi.aware.AwarePairingConfig getPairingConfig() { return null; }
    public boolean isInstantCommunicationModeEnabled() { return false; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int SUBSCRIBE_TYPE_ACTIVE = 1; // 0x1;
    public static final int SUBSCRIBE_TYPE_PASSIVE = 0; // 0x0;

    public static final class Builder {
        public Builder()  {}
        public android.net.wifi.aware.SubscribeConfig build() { return null; }
        public android.net.wifi.aware.SubscribeConfig.Builder setInstantCommunicationModeEnabled(boolean arg0, int arg1) { return this; }
        public android.net.wifi.aware.SubscribeConfig.Builder setMatchFilter(java.util.List<byte[]> arg0) { return this; }
        public android.net.wifi.aware.SubscribeConfig.Builder setMaxDistanceMm(int arg0) { return this; }
        public android.net.wifi.aware.SubscribeConfig.Builder setMinDistanceMm(int arg0) { return this; }
        public android.net.wifi.aware.SubscribeConfig.Builder setPairingConfig(android.net.wifi.aware.AwarePairingConfig arg0) { return this; }
        public android.net.wifi.aware.SubscribeConfig.Builder setServiceName(String arg0) { return this; }
        public android.net.wifi.aware.SubscribeConfig.Builder setServiceSpecificInfo(byte[] arg0) { return this; }
        public android.net.wifi.aware.SubscribeConfig.Builder setSubscribeType(int arg0) { return this; }
        public android.net.wifi.aware.SubscribeConfig.Builder setTerminateNotificationEnabled(boolean arg0) { return this; }
        public android.net.wifi.aware.SubscribeConfig.Builder setTtlSec(int arg0) { return this; }
    }
}
