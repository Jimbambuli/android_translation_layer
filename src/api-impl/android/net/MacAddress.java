package android.net;

/* AOSP frameworks/base/net/MacAddress.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class MacAddress implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public static android.net.MacAddress fromBytes(byte[] arg0) { return null; }
    public static android.net.MacAddress fromString(String arg0) { return null; }
    public int getAddressType() { return 0; }
    public java.net.Inet6Address getLinkLocalIpv6FromEui48Mac() { return null; }
    public boolean isLocallyAssigned() { return false; }
    public boolean matches(android.net.MacAddress arg0, android.net.MacAddress arg1) { return false; }
    public byte[] toByteArray() { return null; }
    public String toOuiString() { return null; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int TYPE_BROADCAST = 3; // 0x3;
    public static final int TYPE_MULTICAST = 2; // 0x2;
    public static final int TYPE_UNICAST = 1; // 0x1;
    /* AOSP Parcelable.Creator; no service hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.net.MacAddress> CREATOR = new android.os.Parcelable.Creator<android.net.MacAddress>() {
        public android.net.MacAddress createFromParcel(android.os.Parcel source) { return null; }
        public android.net.MacAddress[] newArray(int size) { return new android.net.MacAddress[size]; }
    };
}
