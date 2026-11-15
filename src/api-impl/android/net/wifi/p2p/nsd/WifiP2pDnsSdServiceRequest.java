package android.net.wifi.p2p.nsd;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/nsd/WifiP2pDnsSdServiceRequest.java: the three
 * newInstance() variants */
public class WifiP2pDnsSdServiceRequest extends WifiP2pServiceRequest {
	private WifiP2pDnsSdServiceRequest() {
		super(WifiP2pServiceInfo.SERVICE_TYPE_BONJOUR, null);
	}

	private WifiP2pDnsSdServiceRequest(String dnsQuery, int dnsType, int version) {
		super(WifiP2pServiceInfo.SERVICE_TYPE_BONJOUR, WifiP2pDnsSdServiceInfo.createRequest(dnsQuery, dnsType, version));
	}

	public static WifiP2pDnsSdServiceRequest newInstance() {
		return new WifiP2pDnsSdServiceRequest();
	}

	public static WifiP2pDnsSdServiceRequest newInstance(String serviceType) {
		if (serviceType == null) {
			throw new IllegalArgumentException("service type cannot be null");
		}
		return new WifiP2pDnsSdServiceRequest(serviceType + ".local.", WifiP2pDnsSdServiceInfo.DNS_TYPE_PTR, WifiP2pDnsSdServiceInfo.VERSION_1);
	}

	public static WifiP2pDnsSdServiceRequest newInstance(String instanceName, String serviceType) {
		if (instanceName == null || serviceType == null) {
			throw new IllegalArgumentException("instance name or service type cannot be null");
		}
		String fullDomainName = instanceName + "." + serviceType + ".local.";
		return new WifiP2pDnsSdServiceRequest(fullDomainName, WifiP2pDnsSdServiceInfo.DNS_TYPE_TXT, WifiP2pDnsSdServiceInfo.VERSION_1);
	}
}
