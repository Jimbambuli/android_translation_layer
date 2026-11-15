package android.net.wifi.aware;

/* AOSP frameworks/base/net/wifi/aware/Characteristics.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class Characteristics implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public int getMaxMatchFilterLength() { return 0; }
    public int getMaxServiceNameLength() { return 0; }
    public int getMaxServiceSpecificInfoLength() { return 0; }
    public int getNumberOfSupportedDataInterfaces() { return 0; }
    public int getNumberOfSupportedDataPaths() { return 0; }
    public int getNumberOfSupportedPublishSessions() { return 0; }
    public int getNumberOfSupportedSubscribeSessions() { return 0; }
    public int getSupportedCipherSuites() { return 0; }
    public int getSupportedPairingCipherSuites() { return 0; }
    public boolean isAwarePairingSupported() { return false; }
    public boolean isInstantCommunicationModeSupported() { return false; }
    public boolean isSuspensionSupported() { return false; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int WIFI_AWARE_CIPHER_SUITE_NCS_PK_128 = 4; // 0x4;
    public static final int WIFI_AWARE_CIPHER_SUITE_NCS_PK_256 = 8; // 0x8;
    public static final int WIFI_AWARE_CIPHER_SUITE_NCS_PK_PASN_128 = 16; // 0x10;
    public static final int WIFI_AWARE_CIPHER_SUITE_NCS_PK_PASN_256 = 32; // 0x20;
    public static final int WIFI_AWARE_CIPHER_SUITE_NCS_SK_128 = 1; // 0x1;
    public static final int WIFI_AWARE_CIPHER_SUITE_NCS_SK_256 = 2; // 0x2;
    public static final int WIFI_AWARE_CIPHER_SUITE_NONE = 0; // 0x0;
    /* AOSP Parcelable.Creator; no service hands ATL a parcel of this type */
    public static final android.os.Parcelable.Creator<android.net.wifi.aware.Characteristics> CREATOR = new android.os.Parcelable.Creator<android.net.wifi.aware.Characteristics>() {
        public android.net.wifi.aware.Characteristics createFromParcel(android.os.Parcel source) { return null; }
        public android.net.wifi.aware.Characteristics[] newArray(int size) { return new android.net.wifi.aware.Characteristics[size]; }
    };
}
