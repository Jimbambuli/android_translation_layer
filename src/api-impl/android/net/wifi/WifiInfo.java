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

	public static final int INVALID_RSSI = -127;
	public static final int UNKNOWN_FREQUENCY = -1;
	public static final int LINK_SPEED_UNKNOWN = -1;

	/* the fields after WifiInfo.reset(): INVALID_RSSI, frequency -1, link speeds LINK_SPEED_UNKNOWN */
	public int getRssi() {
		return INVALID_RSSI;
	}

	public int getFrequency() {
		return UNKNOWN_FREQUENCY;
	}

	public int getTxLinkSpeedMbps() {
		return LINK_SPEED_UNKNOWN;
	}

	public int getRxLinkSpeedMbps() {
		return LINK_SPEED_UNKNOWN;
	}

	public SupplicantState getSupplicantState() {
		return SupplicantState.DISCONNECTED;
	}
}
