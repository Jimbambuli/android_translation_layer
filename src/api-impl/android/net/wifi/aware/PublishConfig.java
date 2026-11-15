package android.net.wifi.aware;

/* AOSP frameworks/base/net/wifi/aware/PublishConfig.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class PublishConfig implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public int getInstantCommunicationBand() { return 0; }
    public android.net.wifi.aware.AwarePairingConfig getPairingConfig() { return null; }
    public android.net.wifi.aware.WifiAwareDataPathSecurityConfig getSecurityConfig() { return null; }
    public boolean isInstantCommunicationModeEnabled() { return false; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int PUBLISH_TYPE_SOLICITED = 1; // 0x1;
    public static final int PUBLISH_TYPE_UNSOLICITED = 0; // 0x0;

    public static final class Builder {
        public Builder()  {}
        public android.net.wifi.aware.PublishConfig build() { return null; }
        public android.net.wifi.aware.PublishConfig.Builder setDataPathSecurityConfig(android.net.wifi.aware.WifiAwareDataPathSecurityConfig arg0) { return this; }
        public android.net.wifi.aware.PublishConfig.Builder setInstantCommunicationModeEnabled(boolean arg0, int arg1) { return this; }
        public android.net.wifi.aware.PublishConfig.Builder setMatchFilter(java.util.List<byte[]> arg0) { return this; }
        public android.net.wifi.aware.PublishConfig.Builder setPairingConfig(android.net.wifi.aware.AwarePairingConfig arg0) { return this; }
        public android.net.wifi.aware.PublishConfig.Builder setPublishType(int arg0) { return this; }
        public android.net.wifi.aware.PublishConfig.Builder setRangingEnabled(boolean arg0) { return this; }
        public android.net.wifi.aware.PublishConfig.Builder setServiceName(String arg0) { return this; }
        public android.net.wifi.aware.PublishConfig.Builder setServiceSpecificInfo(byte[] arg0) { return this; }
        public android.net.wifi.aware.PublishConfig.Builder setTerminateNotificationEnabled(boolean arg0) { return this; }
        public android.net.wifi.aware.PublishConfig.Builder setTtlSec(int arg0) { return this; }
    }
    /* AOSP Parcelable.Creator; no service hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.net.wifi.aware.PublishConfig> CREATOR = new android.os.Parcelable.Creator<android.net.wifi.aware.PublishConfig>() {
        public android.net.wifi.aware.PublishConfig createFromParcel(android.os.Parcel source) { return null; }
        public android.net.wifi.aware.PublishConfig[] newArray(int size) { return new android.net.wifi.aware.PublishConfig[size]; }
    };
}
