package android.net;

/* AOSP frameworks/base/net/IpConfiguration.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class IpConfiguration implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public android.net.ProxyInfo getHttpProxy() { return null; }
    public android.net.StaticIpConfiguration getStaticIpConfiguration() { return null; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }

    public static final class Builder {
        public Builder()  {}
        public android.net.IpConfiguration build() { return null; }
        public android.net.IpConfiguration.Builder setHttpProxy(android.net.ProxyInfo arg0) { return this; }
        public android.net.IpConfiguration.Builder setStaticIpConfiguration(android.net.StaticIpConfiguration arg0) { return this; }
    }
}
