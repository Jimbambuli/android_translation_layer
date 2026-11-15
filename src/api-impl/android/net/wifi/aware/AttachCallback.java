package android.net.wifi.aware;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/aware/AttachCallback.java */
public class AttachCallback {
	public void onAttached(WifiAwareSession session) {
	}

	public void onAttachFailed() {
	}

	public void onAwareSessionTerminated() {
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public AttachCallback() {}
}
