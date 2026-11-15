package android.net.wifi.aware;

/* AOSP frameworks/base/net/wifi/aware/AwarePairingConfig.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class AwarePairingConfig implements android.os.Parcelable {
    public int describeContents() { return 0; }
    public int getBootstrappingMethods() { return 0; }
    public int getSupportedCipherSuites() { return 0; }
    public boolean isPairingCacheEnabled() { return false; }
    public boolean isPairingSetupEnabled() { return false; }
    public boolean isPairingVerificationEnabled() { return false; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int PAIRING_BOOTSTRAPPING_NFC_READER = 256; // 0x100;
    public static final int PAIRING_BOOTSTRAPPING_NFC_TAG = 16; // 0x10;
    public static final int PAIRING_BOOTSTRAPPING_OPPORTUNISTIC = 1; // 0x1;
    public static final int PAIRING_BOOTSTRAPPING_PASSPHRASE_DISPLAY = 4; // 0x4;
    public static final int PAIRING_BOOTSTRAPPING_PASSPHRASE_KEYPAD = 64; // 0x40;
    public static final int PAIRING_BOOTSTRAPPING_PIN_CODE_DISPLAY = 2; // 0x2;
    public static final int PAIRING_BOOTSTRAPPING_PIN_CODE_KEYPAD = 32; // 0x20;
    public static final int PAIRING_BOOTSTRAPPING_QR_DISPLAY = 8; // 0x8;
    public static final int PAIRING_BOOTSTRAPPING_QR_SCAN = 128; // 0x80;

    public static final class Builder {
        public Builder()  {}
        public android.net.wifi.aware.AwarePairingConfig build() { return null; }
        public android.net.wifi.aware.AwarePairingConfig.Builder setBootstrappingMethods(int arg0) { return this; }
        public android.net.wifi.aware.AwarePairingConfig.Builder setPairingCacheEnabled(boolean arg0) { return this; }
        public android.net.wifi.aware.AwarePairingConfig.Builder setPairingSetupEnabled(boolean arg0) { return this; }
        public android.net.wifi.aware.AwarePairingConfig.Builder setPairingVerificationEnabled(boolean arg0) { return this; }
        public android.net.wifi.aware.AwarePairingConfig.Builder setSupportedCipherSuites(int arg0) { return this; }
    }
}
