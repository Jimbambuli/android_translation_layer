package android.net.wifi.p2p;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pDevice.java: the public fields */
public class WifiP2pDevice {
	public String deviceName = "";
	public String deviceAddress = "";
	public String primaryDeviceType;
	public String secondaryDeviceType;
	public int wpsConfigMethodsSupported;
	public int deviceCapability;
	public int groupCapability;
	public int status = UNAVAILABLE;

	public static final int CONNECTED = 0;
	public static final int INVITED = 1;
	public static final int FAILED = 2;
	public static final int AVAILABLE = 3;
	public static final int UNAVAILABLE = 4;

	public WifiP2pDevice() {}
}
