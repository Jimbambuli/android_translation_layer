package android.net.wifi.aware;

/* AOSP frameworks/base/net/wifi/aware/WifiAwareDataPathSecurityConfig.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class WifiAwareDataPathSecurityConfig implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public int getCipherSuite() { return 0; }
    public byte[] getPmk() { return null; }
    public byte[] getPmkId() { return null; }
    public String getPskPassphrase() { return null; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }

    public static final class Builder {
        public Builder(int arg0)  {}
        public android.net.wifi.aware.WifiAwareDataPathSecurityConfig build() { return null; }
        public android.net.wifi.aware.WifiAwareDataPathSecurityConfig.Builder setPmk(byte[] arg0) { return this; }
        public android.net.wifi.aware.WifiAwareDataPathSecurityConfig.Builder setPmkId(byte[] arg0) { return this; }
        public android.net.wifi.aware.WifiAwareDataPathSecurityConfig.Builder setPskPassphrase(String arg0) { return this; }
    }
}
