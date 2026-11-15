package android.net.wifi.p2p.nsd;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/nsd/WifiP2pDnsSdServiceRequest.java: newInstance()
 * for any DNS-SD service */
public class WifiP2pDnsSdServiceRequest extends WifiP2pServiceRequest {
	private WifiP2pDnsSdServiceRequest() {}

	public static WifiP2pDnsSdServiceRequest newInstance() {
		return new WifiP2pDnsSdServiceRequest();
	}
}
