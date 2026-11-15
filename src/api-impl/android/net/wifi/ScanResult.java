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

	public static class InformationElement implements android.os.Parcelable {
		public static final int EID_SSID = 0;
		public static final int EID_SUPPORTED_RATES = 1;
		public static final int EID_TIM = 5;
		public static final int EID_COUNTRY = 7;
		public static final int EID_BSS_LOAD = 11;
		public static final int EID_ERP = 42;
		public static final int EID_HT_CAPABILITIES = 45;
		public static final int EID_RSN = 48;
		public static final int EID_RSN_EXTENSION = 244;
		public static final int EID_EXTENDED_SUPPORTED_RATES = 50;
		public static final int EID_HT_OPERATION = 61;
		public static final int EID_INTERWORKING = 107;
		public static final int EID_ROAMING_CONSORTIUM = 111;
		public static final int EID_EXTENDED_CAPS = 127;
		public static final int EID_VHT_CAPABILITIES = 191;
		public static final int EID_VHT_OPERATION = 192;
		public static final int EID_RNR = 201;
		public static final int EID_VSA = 221;
		public static final int EID_EXTENSION_PRESENT = 255;
		public static final int EID_EXT_HE_CAPABILITIES = 35;
		public static final int EID_EXT_HE_OPERATION = 36;
		public static final int EID_EXT_EHT_OPERATION = 106;
		public static final int EID_EXT_MULTI_LINK = 107;
		public static final int EID_FRAGMENT_SUB_ELEMENT_MULTI_LINK = 254;
		public static final int EID_EXT_EHT_CAPABILITIES = 108;
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
	
		/* AOSP ScanResult.InformationElement: Parcelable */
		public int describeContents() {
			return 0;
		}

		public void writeToParcel(android.os.Parcel dest, int flags) {
			dest.writeInt(id);
			dest.writeInt(idExt);
			dest.writeByteArray(bytes);
		}

		public static final android.os.Parcelable.Creator<InformationElement> CREATOR = new android.os.Parcelable.Creator<InformationElement>() {
			public InformationElement createFromParcel(android.os.Parcel in) {
				InformationElement informationElement = new InformationElement();
				informationElement.id = in.readInt();
				informationElement.idExt = in.readInt();
				informationElement.bytes = in.createByteArray();
				return informationElement;
			}

			public InformationElement[] newArray(int size) {
				return new InformationElement[size];
			}
		};
	}

	public InformationElement[] informationElements;

	public List<InformationElement> getInformationElements() {
		return Collections.unmodifiableList(Arrays.asList(informationElements));
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int[] getSecurityTypes() { return new int[0]; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.net.wifi.WifiSsid getWifiSsid() { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int getWifiStandard() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean is80211mcResponder() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public ScanResult(android.net.wifi.ScanResult arg0) {}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static int convertChannelToFrequencyMhzIfSupported(int arg0, int arg1) { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static int convertFrequencyMhzToChannelIfSupported(int arg0) { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int describeContents() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.util.List<android.net.wifi.MloLink> getAffiliatedMloLinks() { return new java.util.ArrayList<>(); }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.net.MacAddress getApMldMacAddress() { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int getApMloLinkId() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean is80211azNtbResponder() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean isPasspointNetwork() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean isRangingFrameProtectionRequired() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean isSecureHeLtfSupported() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean isTwtResponder() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
}
