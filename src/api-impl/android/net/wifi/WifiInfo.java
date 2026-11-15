package android.net.wifi;

public class WifiInfo {
	public String getMacAddress() {
		return "";
	}

	public String getBSSID() {
		return "";
	}

	public int getIpAddress() {
		return -1;
	}

	/* not connected, as AOSP reports it: WifiManager.UNKNOWN_SSID, no network, LINK_SPEED_UNKNOWN */
	public String getSSID() {
		return WifiManager.UNKNOWN_SSID;
	}

	public int getNetworkId() {
		return -1;
	}

	public int getLinkSpeed() {
		return -1;
	}

	public SupplicantState getSupplicantState() {
		return SupplicantState.DISCONNECTED;
	}

	/* AOSP reset(): LINK_SPEED_UNKNOWN; mWifiStandard stays at WIFI_STANDARD_UNKNOWN; mSecurityType = -1 */
	public int getMaxSupportedTxLinkSpeedMbps() {
		return -1;
	}

	public int getMaxSupportedRxLinkSpeedMbps() {
		return -1;
	}

	public int getWifiStandard() {
		return ScanResult.WIFI_STANDARD_UNKNOWN;
	}

	public int getCurrentSecurityType() {
		return -1;
	}
}
