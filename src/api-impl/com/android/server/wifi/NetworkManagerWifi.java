package com.android.server.wifi;

import android.atl.annotation.Export;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiSsid;

import java.util.ArrayList;
import java.util.List;

/*
 * ATL's Wi-Fi hardware: the desktop's NetworkManager on the system bus (org.freedesktop.NetworkManager, in a Flatpak
 * it needs --system-talk-name for it). It takes the place of AOSP's wificond/WifiScanner below ScanRequestProxy and of
 * ClientModeImpl's WifiInfo. Without NetworkManager or without a Wi-Fi device ATL has Wi-Fi that is switched off
 * (WIFI_STATE_DISABLED, no scan results).
 *
 * NetworkManager gives what wpa_supplicant reports, a little reduced: the signal only as quality in percent
 * (nm_wifi_utils_level_to_quality(): -100..-40 dBm linearly to 0..100 %, converted back here), the security as flags
 * without the order of the elements, no information elements.
 */
public final class NetworkManagerWifi {

	/* NetworkManager's nm-dbus-interface.h (1.58) */
	private static final int NM_802_11_AP_FLAGS_PRIVACY = 0x1;
	private static final int NM_802_11_AP_FLAGS_WPS = 0x2;
	private static final int NM_802_11_AP_SEC_PAIR_TKIP = 0x4;
	private static final int NM_802_11_AP_SEC_PAIR_CCMP = 0x8;
	private static final int NM_802_11_AP_SEC_KEY_MGMT_PSK = 0x100;
	private static final int NM_802_11_AP_SEC_KEY_MGMT_802_1X = 0x200;
	private static final int NM_802_11_AP_SEC_KEY_MGMT_SAE = 0x400;
	private static final int NM_802_11_AP_SEC_KEY_MGMT_OWE = 0x800;
	private static final int NM_802_11_AP_SEC_KEY_MGMT_OWE_TM = 0x1000;
	private static final int NM_802_11_AP_SEC_KEY_MGMT_EAP_SUITE_B_192 = 0x2000;
	private static final int NM_802_11_MODE_ADHOC = 1;
	private static final int NM_802_11_MODE_INFRA = 2;
	public static final int NM_WIFI_DEVICE_CAP_FREQ_VALID = 0x100;
	public static final int NM_WIFI_DEVICE_CAP_FREQ_2GHZ = 0x200;
	public static final int NM_WIFI_DEVICE_CAP_FREQ_5GHZ = 0x400;
	public static final int NM_WIFI_DEVICE_CAP_FREQ_6GHZ = 0x800;
	public static final int NM_DEVICE_STATE_DISCONNECTED = 30;
	private static final int NM_DEVICE_STATE_PREPARE = 40;
	private static final int NM_DEVICE_STATE_NEED_AUTH = 60;
	private static final int NM_DEVICE_STATE_IP_CONFIG = 70;
	private static final int NM_DEVICE_STATE_ACTIVATED = 100;
	private static final int NM_METERED_YES = 1;
	private static final int NM_METERED_GUESS_YES = 3;

	/* the values of nativeGetDevice() */
	public static final int DEVICE_WIRELESS_ENABLED = 0;
	public static final int DEVICE_STATE = 1;
	public static final int DEVICE_CAPABILITIES = 2;
	private static final int DEVICE_BITRATE = 3;
	private static final int DEVICE_METERED = 4;
	private static final int DEVICE_IPV4 = 5;
	private static final int DEVICE_COUNT = 6;

	/* the values of an access point record */
	private static final int AP_STRENGTH = 0;
	private static final int AP_FREQUENCY = 1;
	private static final int AP_FLAGS = 2;
	private static final int AP_WPA_FLAGS = 3;
	private static final int AP_RSN_FLAGS = 4;
	private static final int AP_MODE = 5;
	private static final int AP_LAST_SEEN = 6;
	private static final int AP_BANDWIDTH = 7;

	private static Boolean present;

	private NetworkManagerWifi() {}

	/** whether NetworkManager runs and has a Wi-Fi device (AOSP: FEATURE_WIFI) */
	public static synchronized boolean isPresent() {
		if (present == null)
			present = nativeInit();
		return present;
	}

	/** the device's values (DEVICE_*), all 0 when it went away */
	public static int[] getDevice() {
		int[] values = isPresent() ? nativeGetDevice() : null;
		return values != null && values.length == DEVICE_COUNT ? values : new int[DEVICE_COUNT];
	}

	/** switch the radio (NetworkManager's WirelessEnabled); the change comes back through stateChanged() */
	public static void setWirelessEnabled(boolean enabled) {
		nativeSetWirelessEnabled(enabled);
	}

	/** ask NetworkManager to scan; the answer comes through scanDone() */
	public static void requestScan() {
		if (isPresent())
			nativeRequestScan();
	}

	/** the access points NetworkManager has seen in its recent scans, as AOSP's scan results */
	public static List<ScanResult> getScanResults() {
		List<ScanResult> results = new ArrayList<>();
		Object[] records = isPresent() ? nativeGetAccessPoints() : null;
		if (records == null)
			return results;
		for (Object record : records) {
			ScanResult result = toScanResult((Object[])record);
			if (result != null)
				results.add(result);
		}
		return results;
	}

	/** the connection as ClientModeImpl's WifiInfo (before the redactions for the caller) */
	public static WifiInfo getConnectionInfo(int[] device) {
		WifiInfo info = new WifiInfo();
		String mac = nativeGetMacAddress();
		if (mac != null)
			info.setMacAddress(mac.toLowerCase(java.util.Locale.ROOT));
		int state = device[DEVICE_STATE];
		info.setSupplicantState(supplicantState(state));
		Object[] ap = state >= NM_DEVICE_STATE_PREPARE && state <= NM_DEVICE_STATE_ACTIVATED ? nativeGetActiveAccessPoint() : null;
		if (ap == null)
			return info;
		int[] values = (int[])ap[2];
		info.setSSID(WifiSsid.fromBytes((byte[])ap[0]));
		info.setBSSID(((String)ap[1]).toLowerCase(java.util.Locale.ROOT));
		info.setRssi(rssiFromStrength(values[AP_STRENGTH]));
		info.setFrequency(values[AP_FREQUENCY]);
		/* NetworkManager's Bitrate is the transmit rate in kbit/s; it does not report the receive rate */
		int bitrate = device[DEVICE_BITRATE] / 1000;
		info.setLinkSpeed(bitrate);
		info.setTxLinkSpeedMbps(bitrate);
		info.setCurrentSecurityType(securityType(values));
		if (state >= NM_DEVICE_STATE_IP_CONFIG) {
			/* ATL has no WifiConfigManager: the one network it knows is the active connection */
			info.setNetworkId(0);
			int metered = device[DEVICE_METERED];
			info.setMeteredHint(metered == NM_METERED_YES || metered == NM_METERED_GUESS_YES);
			int ipv4 = device[DEVICE_IPV4];
			if (ipv4 != 0) {
				try {
					info.setInetAddress(java.net.InetAddress.getByAddress(new byte[] {
					    (byte)(ipv4 >>> 24), (byte)(ipv4 >>> 16), (byte)(ipv4 >>> 8), (byte)ipv4 }));
				} catch (java.net.UnknownHostException e) {
				}
			}
		}
		return info;
	}

	/* NetworkManager's device state -> wpa_supplicant's state as ClientModeImpl reports it */
	private static android.net.wifi.SupplicantState supplicantState(int state) {
		if (state >= NM_DEVICE_STATE_IP_CONFIG && state <= NM_DEVICE_STATE_ACTIVATED)
			return android.net.wifi.SupplicantState.COMPLETED;
		if (state == NM_DEVICE_STATE_NEED_AUTH)
			return android.net.wifi.SupplicantState.FOUR_WAY_HANDSHAKE;
		if (state >= NM_DEVICE_STATE_PREPARE && state < NM_DEVICE_STATE_NEED_AUTH)
			return android.net.wifi.SupplicantState.ASSOCIATING;
		if (state == NM_DEVICE_STATE_DISCONNECTED)
			return android.net.wifi.SupplicantState.DISCONNECTED;
		return android.net.wifi.SupplicantState.INTERFACE_DISABLED;
	}

	/* the inverse of nm_wifi_utils_level_to_quality() (NetworkManager src/core/nm-core-utils.c): quality =
	 * 100 - (-40 - dBm) * 100 / 60 for -100..-40 dBm */
	static int rssiFromStrength(int strength) {
		return -40 - (100 - strength) * 60 / 100;
	}

	private static ScanResult toScanResult(Object[] record) {
		int[] values = (int[])record[2];
		int lastSeen = values[AP_LAST_SEEN];
		if (lastSeen < 0) // never in a scan result
			return null;
		ScanResult result = new ScanResult();
		result.setWifiSsid(WifiSsid.fromBytes((byte[])record[0]));
		result.BSSID = ((String)record[1]).toLowerCase(java.util.Locale.ROOT);
		result.capabilities = capabilities(values);
		result.level = rssiFromStrength(values[AP_STRENGTH]);
		result.frequency = values[AP_FREQUENCY];
		/* LastSeen is in CLOCK_BOOTTIME seconds, ScanResult.timestamp in microseconds since boot */
		result.timestamp = lastSeen * 1000000L;
		result.channelWidth = channelWidth(values[AP_BANDWIDTH]);
		/* ScanDetail always sets the elements parsed from the frame (getInformationElements() needs the array);
		 * NetworkManager does not pass them on */
		result.informationElements = new ScanResult.InformationElement[0];
		return result;
	}

	/* ScanResult.CHANNEL_WIDTH_* for NetworkManager's Bandwidth in MHz (0: not reported) */
	private static int channelWidth(int mhz) {
		switch (mhz) {
			case 40:
				return ScanResult.CHANNEL_WIDTH_40MHZ;
			case 80:
				return ScanResult.CHANNEL_WIDTH_80MHZ;
			case 160:
				return ScanResult.CHANNEL_WIDTH_160MHZ;
			case 320:
				return ScanResult.CHANNEL_WIDTH_320MHZ;
			default:
				return ScanResult.CHANNEL_WIDTH_20MHZ;
		}
	}

	/* AOSP InformationElementUtil.Capabilities.generateCapabilitiesString() from NetworkManager's flags. The order
	 * of the elements is lost: RSN before WPA (the order in a beacon), key managements and ciphers by suite number
	 * (802.1X 1, PSK 2, SAE 8, Suite B 12, OWE 18; TKIP 2, CCMP 4) */
	static String capabilities(int[] values) {
		StringBuilder capabilities = new StringBuilder();
		int rsn = values[AP_RSN_FLAGS];
		int wpa = values[AP_WPA_FLAGS];
		List<String> protocols = new ArrayList<>();
		List<List<String>> akms = new ArrayList<>();
		List<List<String>> ciphers = new ArrayList<>();
		int rsnAkm = rsn & (NM_802_11_AP_SEC_KEY_MGMT_802_1X | NM_802_11_AP_SEC_KEY_MGMT_PSK | NM_802_11_AP_SEC_KEY_MGMT_SAE
		                    | NM_802_11_AP_SEC_KEY_MGMT_EAP_SUITE_B_192 | NM_802_11_AP_SEC_KEY_MGMT_OWE);
		if (rsnAkm != 0 || (rsn & (NM_802_11_AP_SEC_PAIR_TKIP | NM_802_11_AP_SEC_PAIR_CCMP)) != 0) {
			protocols.add("RSN");
			akms.add(keyManagements(rsn));
			ciphers.add(pairwiseCiphers(rsn));
		}
		if ((rsn & NM_802_11_AP_SEC_KEY_MGMT_OWE_TM) != 0) { // the OWE transition element
			protocols.add("RSN");
			akms.add(java.util.Collections.singletonList("OWE_TRANSITION"));
			ciphers.add(java.util.Collections.singletonList("CCMP-128"));
		}
		if (wpa != 0) {
			protocols.add("WPA");
			akms.add(keyManagements(wpa));
			ciphers.add(pairwiseCiphers(wpa));
		}
		if (protocols.isEmpty() && (values[AP_FLAGS] & NM_802_11_AP_FLAGS_PRIVACY) != 0)
			capabilities.append("[WEP]");
		for (int i = 0; i < protocols.size(); i++) {
			String capability = perProtocol(protocols.get(i), akms.get(i), ciphers.get(i));
			if (!capability.contains("EAP_SUITE_B_192") && (capability.contains("RSN-EAP") || capability.contains("RSN-FT/EAP")
			                                                || capability.contains("RSN-PSK") || capability.contains("RSN-FT/PSK"))) {
				StringBuilder wpa2 = new StringBuilder("[WPA2");
				for (int j = 0; j < akms.get(i).size(); j++) {
					wpa2.append(j == 0 ? "-" : "+").append(akms.get(i).get(j));
					if (capability.contains("SAE")) // WPA3/WPA2 transition mode
						break;
				}
				for (int j = 0; j < ciphers.get(i).size(); j++)
					wpa2.append(j == 0 ? "-" : "+").append(ciphers.get(i).get(j));
				capabilities.append(wpa2).append("]");
			}
			capabilities.append(capability);
		}
		int mode = values[AP_MODE];
		if (mode == NM_802_11_MODE_INFRA)
			capabilities.append("[ESS]");
		if (mode == NM_802_11_MODE_ADHOC)
			capabilities.append("[IBSS]");
		if ((values[AP_FLAGS] & NM_802_11_AP_FLAGS_WPS) != 0)
			capabilities.append("[WPS]");
		return capabilities.toString();
	}

	private static String perProtocol(String protocol, List<String> akms, List<String> ciphers) {
		StringBuilder capability = new StringBuilder("[").append(protocol);
		for (int j = 0; j < akms.size(); j++)
			capability.append(j == 0 ? "-" : "+").append(akms.get(j));
		for (int j = 0; j < ciphers.size(); j++)
			capability.append(j == 0 ? "-" : "+").append(ciphers.get(j));
		return capability.append("]").toString();
	}

	/* InformationElementUtil.Capabilities.keyManagementToString() */
	private static List<String> keyManagements(int flags) {
		List<String> list = new ArrayList<>();
		if ((flags & NM_802_11_AP_SEC_KEY_MGMT_802_1X) != 0)
			list.add("EAP/SHA1");
		if ((flags & NM_802_11_AP_SEC_KEY_MGMT_PSK) != 0)
			list.add("PSK");
		if ((flags & NM_802_11_AP_SEC_KEY_MGMT_SAE) != 0)
			list.add("SAE");
		if ((flags & NM_802_11_AP_SEC_KEY_MGMT_EAP_SUITE_B_192) != 0)
			list.add("EAP_SUITE_B_192");
		if ((flags & NM_802_11_AP_SEC_KEY_MGMT_OWE) != 0)
			list.add("OWE");
		return list;
	}

	/* InformationElementUtil.Capabilities.cipherToString() */
	private static List<String> pairwiseCiphers(int flags) {
		List<String> list = new ArrayList<>();
		if ((flags & NM_802_11_AP_SEC_PAIR_TKIP) != 0)
			list.add("TKIP");
		if ((flags & NM_802_11_AP_SEC_PAIR_CCMP) != 0)
			list.add("CCMP-128");
		return list;
	}

	/* WifiInfo.SECURITY_TYPE_* of the connected access point (ClientModeImpl takes it from the network it
	 * connected with; NetworkManager gives the access point's flags) */
	private static int securityType(int[] values) {
		int sec = values[AP_RSN_FLAGS] | values[AP_WPA_FLAGS];
		if ((sec & NM_802_11_AP_SEC_KEY_MGMT_EAP_SUITE_B_192) != 0)
			return WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT;
		if ((sec & NM_802_11_AP_SEC_KEY_MGMT_SAE) != 0)
			return WifiInfo.SECURITY_TYPE_SAE;
		if ((sec & NM_802_11_AP_SEC_KEY_MGMT_802_1X) != 0)
			return WifiInfo.SECURITY_TYPE_EAP;
		if ((sec & NM_802_11_AP_SEC_KEY_MGMT_PSK) != 0)
			return WifiInfo.SECURITY_TYPE_PSK;
		if ((sec & (NM_802_11_AP_SEC_KEY_MGMT_OWE | NM_802_11_AP_SEC_KEY_MGMT_OWE_TM)) != 0)
			return WifiInfo.SECURITY_TYPE_OWE;
		if ((values[AP_FLAGS] & NM_802_11_AP_FLAGS_PRIVACY) != 0)
			return WifiInfo.SECURITY_TYPE_WEP;
		return WifiInfo.SECURITY_TYPE_OPEN;
	}

	private static native boolean nativeInit();
	private static native int[] nativeGetDevice();
	private static native String nativeGetMacAddress();
	/* records {byte[] ssid, String bssid, int[] AP_* values} */
	private static native Object[] nativeGetAccessPoints();
	private static native Object[] nativeGetActiveAccessPoint();
	private static native void nativeRequestScan();
	private static native void nativeSetWirelessEnabled(boolean enabled);

	/* NetworkManager finished a scan (LastScan changed: success) or refused the request (failure) */
	@Export
	static void scanDone(boolean success) {
		WifiServiceImpl.get().onScanDone(success);
	}

	/* the radio, the device state or the connection changed */
	@Export
	static void stateChanged() {
		WifiServiceImpl.get().onStateChanged();
	}
}
