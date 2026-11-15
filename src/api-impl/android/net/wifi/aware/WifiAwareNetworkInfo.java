package android.net.wifi.aware;

/* AOSP frameworks/base/net/wifi/aware/WifiAwareNetworkInfo.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class WifiAwareNetworkInfo implements android.os.Parcelable, android.net.TransportInfo {
    public int describeContents() { return 0; }
    public java.util.List<android.net.wifi.aware.WifiAwareChannelInfo> getChannelInfoList() { return null; }
    public java.net.Inet6Address getPeerIpv6Addr() { return null; }
    public int getPort() { return 0; }
    public int getTransportProtocol() { return 0; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    /* AOSP Parcelable.Creator; no service hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.net.wifi.aware.WifiAwareNetworkInfo> CREATOR = new android.os.Parcelable.Creator<android.net.wifi.aware.WifiAwareNetworkInfo>() {
        public android.net.wifi.aware.WifiAwareNetworkInfo createFromParcel(android.os.Parcel source) { return null; }
        public android.net.wifi.aware.WifiAwareNetworkInfo[] newArray(int size) { return new android.net.wifi.aware.WifiAwareNetworkInfo[size]; }
    };
}
