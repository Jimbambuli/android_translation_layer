package android.net;

/* AOSP frameworks/base/net/L2capNetworkSpecifier.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class L2capNetworkSpecifier extends android.net.NetworkSpecifier implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public int getHeaderCompression() { return 0; }
    public int getPsm() { return 0; }
    public android.net.MacAddress getRemoteAddress() { return null; }
    public int getRole() { return 0; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int HEADER_COMPRESSION_6LOWPAN = 2; // 0x2;
    public static final int HEADER_COMPRESSION_ANY = 0; // 0x0;
    public static final int HEADER_COMPRESSION_NONE = 1; // 0x1;
    public static final int PSM_ANY = 0; // 0x0;
    public static final int ROLE_ANY = 0; // 0x0;
    public static final int ROLE_CLIENT = 1; // 0x1;
    public static final int ROLE_SERVER = 2; // 0x2;

    public static final class Builder {
        public Builder()  {}
        public android.net.L2capNetworkSpecifier build() { return null; }
        public android.net.L2capNetworkSpecifier.Builder setHeaderCompression(int arg0) { return this; }
        public android.net.L2capNetworkSpecifier.Builder setPsm(int arg0) { return this; }
        public android.net.L2capNetworkSpecifier.Builder setRemoteAddress(android.net.MacAddress arg0) { return this; }
        public android.net.L2capNetworkSpecifier.Builder setRole(int arg0) { return this; }
    }
}
