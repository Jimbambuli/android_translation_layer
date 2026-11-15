package android.net.wifi.p2p.nsd;

import android.text.TextUtils;
import java.util.Map;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/nsd/WifiP2pDnsSdServiceInfo.java: newInstance()
 * with its argument check. The DNS-SD queries it would encode are not built, only P2pManager would consume them
 * and ATL has none. */
public class WifiP2pDnsSdServiceInfo extends WifiP2pServiceInfo {
	private WifiP2pDnsSdServiceInfo() {}

	public static WifiP2pDnsSdServiceInfo newInstance(String instanceName, String serviceType, Map<String, String> txtMap) {
		if (TextUtils.isEmpty(instanceName) || TextUtils.isEmpty(serviceType)) {
			throw new IllegalArgumentException("instance name or service type cannot be empty");
		}
		return new WifiP2pDnsSdServiceInfo();
	}
}
