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
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int describeContents() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.util.List<android.net.wifi.MloLink> getAffiliatedMloLinks() { return new java.util.ArrayList<>(); }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.net.MacAddress getApMldMacAddress() { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int getApMloLinkId() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public long getApplicableRedactions() { return 0L; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.util.List<android.net.wifi.MloLink> getAssociatedMloLinks() { return new java.util.ArrayList<>(); }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static android.net.NetworkInfo.DetailedState getDetailedStateOf(android.net.wifi.SupplicantState arg0) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean getHiddenSSID() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.util.List<android.net.wifi.ScanResult.InformationElement> getInformationElements() { return new java.util.ArrayList<>(); }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public String getPasspointFqdn() { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public String getPasspointProviderFriendlyName() { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public String getPasspointUniqueId() { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int getSubscriptionId() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean isRestricted() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.net.wifi.WifiInfo makeCopy(long arg0) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }

    /* AOSP 16 API; no backing hardware/service in ATL */
    public static final class Builder {
        public Builder()  {}
        public android.net.wifi.WifiInfo build() { return null; }
        public android.net.wifi.WifiInfo.Builder setBssid(String arg0) { return this; }
        public android.net.wifi.WifiInfo.Builder setCurrentSecurityType(int arg0) { return this; }
        public android.net.wifi.WifiInfo.Builder setNetworkId(int arg0) { return this; }
        public android.net.wifi.WifiInfo.Builder setRssi(int arg0) { return this; }
        public android.net.wifi.WifiInfo.Builder setSsid(byte[] arg0) { return this; }
        public android.net.wifi.WifiInfo.Builder setSubscriptionId(int arg0) { return this; }
    }
}
