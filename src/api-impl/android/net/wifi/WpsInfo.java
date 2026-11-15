package android.net.wifi;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/WpsInfo.java (deprecated data holder) */
public class WpsInfo {
	public static final int PBC = 0;
	public static final int DISPLAY = 1;
	public static final int KEYPAD = 2;
	public static final int LABEL = 3;
	public static final int INVALID = 4;

	public int setup;
	public String BSSID;
	public String pin;

	/* AOSP: setup = INVALID */
	public WpsInfo() {
		setup = INVALID;
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public WpsInfo(android.net.wifi.WpsInfo arg0) {}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int describeContents() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
}
