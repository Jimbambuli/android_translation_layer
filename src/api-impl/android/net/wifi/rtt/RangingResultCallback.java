package android.net.wifi.rtt;

/* AOSP frameworks/base/net/wifi/rtt/RangingResultCallback.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public abstract class RangingResultCallback {
    public RangingResultCallback()  {}
    public abstract void onRangingFailure(int arg0);
    public abstract void onRangingResults(java.util.List<android.net.wifi.rtt.RangingResult> arg0);
    public static final int STATUS_CODE_FAIL = 1; // 0x1;
    public static final int STATUS_CODE_FAIL_RTT_NOT_AVAILABLE = 2; // 0x2;
}
