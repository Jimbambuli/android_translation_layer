package android.net.wifi.p2p.nsd;

import java.util.Locale;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/nsd/WifiP2pUpnpServiceRequest.java */
public class WifiP2pUpnpServiceRequest extends WifiP2pServiceRequest {
	protected WifiP2pUpnpServiceRequest(String query) {
		super(WifiP2pServiceInfo.SERVICE_TYPE_UPNP, query);
	}

	protected WifiP2pUpnpServiceRequest() {
		super(WifiP2pServiceInfo.SERVICE_TYPE_UPNP, null);
	}

	public static WifiP2pUpnpServiceRequest newInstance() {
		return new WifiP2pUpnpServiceRequest();
	}

	public static WifiP2pUpnpServiceRequest newInstance(String st) {
		if (st == null) {
			throw new IllegalArgumentException("search target cannot be null");
		}
		StringBuffer sb = new StringBuffer();
		sb.append(String.format(Locale.US, "%02x", WifiP2pUpnpServiceInfo.VERSION_1_0));
		sb.append(WifiP2pServiceInfo.bin2HexStr(st.getBytes()));
		return new WifiP2pUpnpServiceRequest(sb.toString());
	}
}
