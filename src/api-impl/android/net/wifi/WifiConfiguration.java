package android.net.wifi;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/WifiConfiguration.java: the credential fields apps fill
 * in. Without Wi-Fi hardware nothing ever reads them. */
public class WifiConfiguration {
	public int networkId;
	public String SSID;
	public String BSSID;
	public String preSharedKey;
	public boolean hiddenSSID;

	/* AOSP: networkId = INVALID_NETWORK_ID (-1), the other fields null/false */
	public WifiConfiguration() {
		networkId = -1;
	}
}
