package android.net.wifi;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/ScanResult.java: the public fields, constants and
 * information elements. WifiManager.getScanResults() is empty without Wi-Fi hardware, so apps only build these
 * themselves. */
public final class ScanResult {
	public String SSID;
	public String BSSID;
	public String capabilities;
	public int level;
	public int frequency;

	public static final int CHANNEL_WIDTH_20MHZ = 0;
	public static final int CHANNEL_WIDTH_40MHZ = 1;
	public static final int CHANNEL_WIDTH_80MHZ = 2;
	public static final int CHANNEL_WIDTH_160MHZ = 3;
	public static final int CHANNEL_WIDTH_80MHZ_PLUS_MHZ = 4;
	public static final int CHANNEL_WIDTH_320MHZ = 5;

	public int channelWidth;

	public static final int WIFI_STANDARD_UNKNOWN = 0;
	public static final int WIFI_STANDARD_LEGACY = 1;
	public static final int WIFI_STANDARD_11N = 4;
	public static final int WIFI_STANDARD_11AC = 5;
	public static final int WIFI_STANDARD_11AX = 6;
	public static final int WIFI_STANDARD_11AD = 7;
	public static final int WIFI_STANDARD_11BE = 8;

	public static class InformationElement {
		public int id;
		public int idExt;
		public byte[] bytes;

		public InformationElement() {}

		public InformationElement(int id, int idExt, byte[] bytes) {
			this.id = id;
			this.idExt = idExt;
			this.bytes = bytes.clone();
		}

		public InformationElement(InformationElement rhs) {
			this.id = rhs.id;
			this.idExt = rhs.idExt;
			this.bytes = rhs.bytes.clone();
		}

		public int getId() {
			return id;
		}

		public int getIdExt() {
			return idExt;
		}

		public ByteBuffer getBytes() {
			return ByteBuffer.wrap(bytes).asReadOnlyBuffer();
		}
	}

	public InformationElement[] informationElements;

	public List<InformationElement> getInformationElements() {
		return Collections.unmodifiableList(Arrays.asList(informationElements));
	}
}
