package android.net;

/* AOSP frameworks/base/net/PlatformVpnProfile.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public abstract class PlatformVpnProfile {
    public final boolean areLocalRoutesExcluded() { return false; }
    public final int getType() { return 0; }
    public final String getTypeString() { return null; }
    public final boolean isInternetValidationRequired() { return false; }
    public static final int TYPE_IKEV2_IPSEC_PSK = 7; // 0x7;
    public static final int TYPE_IKEV2_IPSEC_RSA = 8; // 0x8;
    public static final int TYPE_IKEV2_IPSEC_USER_PASS = 6; // 0x6;
}
