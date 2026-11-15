package android.net.wifi.aware;

/* AOSP frameworks/base/net/wifi/aware/WifiAwareNetworkInfo.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class WifiAwareNetworkInfo implements android.os.Parcelable android.net.TransportInfo {
    public int describeContents() { return 0; }
    public java.util.List<android.net.wifi.aware.WifiAwareChannelInfo> getChannelInfoList() { return null; }
    public java.net.Inet6Address getPeerIpv6Addr() { return null; }
    public int getPort() { return 0; }
    public int getTransportProtocol() { return 0; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
}
