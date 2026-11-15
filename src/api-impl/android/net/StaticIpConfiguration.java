package android.net;

/* AOSP frameworks/base/net/StaticIpConfiguration.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class StaticIpConfiguration implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public java.util.List<java.net.InetAddress> getDnsServers() { return null; }
    public String getDomains() { return null; }
    public java.net.InetAddress getGateway() { return null; }
    public android.net.LinkAddress getIpAddress() { return null; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }

    public static final class Builder {
        public Builder()  {}
        public android.net.StaticIpConfiguration build() { return null; }
        public android.net.StaticIpConfiguration.Builder setDnsServers(Iterable<java.net.InetAddress> arg0) { return this; }
        public android.net.StaticIpConfiguration.Builder setDomains(String arg0) { return this; }
        public android.net.StaticIpConfiguration.Builder setGateway(java.net.InetAddress arg0) { return this; }
        public android.net.StaticIpConfiguration.Builder setIpAddress(android.net.LinkAddress arg0) { return this; }
    }
}
