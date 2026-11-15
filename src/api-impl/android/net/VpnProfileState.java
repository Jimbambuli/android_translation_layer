package android.net;

/* AOSP frameworks/base/net/VpnProfileState.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class VpnProfileState implements android.os.Parcelable {
    public VpnProfileState(int arg0, String arg1, boolean arg2, boolean arg3)  {}
    public int describeContents() { return 0; }
    public String getSessionId() { return null; }
    public int getState() { return 0; }
    public boolean isAlwaysOn() { return false; }
    public boolean isLockdownEnabled() { return false; }
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
    public static final int STATE_CONNECTED = 2; // 0x2;
    public static final int STATE_CONNECTING = 1; // 0x1;
    public static final int STATE_DISCONNECTED = 0; // 0x0;
    public static final int STATE_FAILED = 3; // 0x3;
}
