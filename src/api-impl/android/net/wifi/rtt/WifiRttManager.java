package android.net.wifi.rtt;

/* AOSP frameworks/base/net/wifi/rtt/WifiRttManager.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public class WifiRttManager {
    public android.os.Bundle getRttCharacteristics() { return null; }
    public boolean isAvailable() { return false; }
    public void startRanging(android.net.wifi.rtt.RangingRequest arg0, java.util.concurrent.Executor arg1, android.net.wifi.rtt.RangingResultCallback arg2) {  }
    public static final String ACTION_WIFI_RTT_STATE_CHANGED = "android.net.wifi.rtt.action.WIFI_RTT_STATE_CHANGED";
    public static final String CHARACTERISTICS_KEY_BOOLEAN_LCI = "key_lci";
    public static final String CHARACTERISTICS_KEY_BOOLEAN_LCR = "key_lcr";
    public static final String CHARACTERISTICS_KEY_BOOLEAN_NTB_INITIATOR = "key_ntb_initiator";
    public static final String CHARACTERISTICS_KEY_BOOLEAN_ONE_SIDED_RTT = "key_one_sided_rtt";
    public static final String CHARACTERISTICS_KEY_BOOLEAN_RANGING_FRAME_PROTECTION_SUPPORTED = "key_rnm_mfp_supported";
    public static final String CHARACTERISTICS_KEY_BOOLEAN_SECURE_HE_LTF_SUPPORTED = "key_secure_he_ltf_supported";
    public static final String CHARACTERISTICS_KEY_BOOLEAN_STA_RESPONDER = "key_sta_responder";
    public static final String CHARACTERISTICS_KEY_INT_MAX_SUPPORTED_SECURE_HE_LTF_PROTO_VERSION = "key_max_supported_secure_he_ltf_proto_ver";
}
