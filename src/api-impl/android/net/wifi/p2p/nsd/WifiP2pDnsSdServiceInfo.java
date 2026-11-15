package android.net.wifi.p2p.nsd;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/nsd/WifiP2pDnsSdServiceInfo.java: newInstance()
 * with its argument check, and createRequest() that WifiP2pDnsSdServiceRequest needs. The PTR/TXT queries of
 * newInstance() are not built (AOSP needs DnsSdTxtRecord for them, ATL has none), only P2pManager would consume
 * them and ATL has none. */
public class WifiP2pDnsSdServiceInfo extends WifiP2pServiceInfo {
	/* AOSP: hidden constants */
	static final int VERSION_1 = 0x01;
	static final int DNS_TYPE_PTR = 12;
	static final int DNS_TYPE_TXT = 16;

	private static final Map<String, String> sVmPacket;
	static {
		sVmPacket = new HashMap<String, String>();
		sVmPacket.put("_tcp.local.", "c00c");
		sVmPacket.put("local.", "c011");
		sVmPacket.put("_udp.local.", "c01c");
	}

	private WifiP2pDnsSdServiceInfo(List<String> queryList) {
		super(queryList);
	}

	public static WifiP2pDnsSdServiceInfo newInstance(String instanceName, String serviceType, Map<String, String> txtMap) {
		if (TextUtils.isEmpty(instanceName) || TextUtils.isEmpty(serviceType)) {
			throw new IllegalArgumentException("instance name or service type cannot be empty");
		}
		return new WifiP2pDnsSdServiceInfo(new ArrayList<String>());
	}

	static String createRequest(String dnsName, int dnsType, int version) {
		StringBuffer sb = new StringBuffer();
		if (dnsType == WifiP2pDnsSdServiceInfo.DNS_TYPE_TXT) {
			dnsName = dnsName.toLowerCase(Locale.ROOT);
		}
		sb.append(compressDnsName(dnsName));
		sb.append(String.format(Locale.US, "%04x", dnsType));
		sb.append(String.format(Locale.US, "%02x", version));
		return sb.toString();
	}

	private static String compressDnsName(String dnsName) {
		StringBuffer sb = new StringBuffer();
		while (true) {
			String data = sVmPacket.get(dnsName);
			if (data != null) {
				sb.append(data);
				break;
			}
			int i = dnsName.indexOf('.');
			if (i == -1) {
				if (dnsName.length() > 0) {
					sb.append(String.format(Locale.US, "%02x", dnsName.length()));
					sb.append(WifiP2pServiceInfo.bin2HexStr(dnsName.getBytes()));
				}
				sb.append("00");
				break;
			}
			String name = dnsName.substring(0, i);
			dnsName = dnsName.substring(i + 1);
			sb.append(String.format(Locale.US, "%02x", name.length()));
			sb.append(WifiP2pServiceInfo.bin2HexStr(name.getBytes()));
		}
		return sb.toString();
	}
}
