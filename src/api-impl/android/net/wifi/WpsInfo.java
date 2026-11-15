package android.net.wifi;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/WpsInfo.java (deprecated data holder) */
public class WpsInfo implements android.os.Parcelable {
	public static final int PBC = 0;
	public static final int DISPLAY = 1;
	public static final int KEYPAD = 2;
	public static final int LABEL = 3;
	public static final int INVALID = 4;

	public int setup;
	public String BSSID;
	public String pin;

	/* AOSP: setup = INVALID */
	public WpsInfo() {
		setup = INVALID;
	}

	/* AOSP WpsInfo(WpsInfo) copy constructor */
	public WpsInfo(WpsInfo source) {
		if (source != null) {
			setup = source.setup;
			BSSID = source.BSSID;
			pin = source.pin;
		}
	}

	/* AOSP WpsInfo.describeContents()/writeToParcel()/CREATOR */
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeInt(setup);
		dest.writeString(BSSID);
		dest.writeString(pin);
	}

	public static final android.os.Parcelable.Creator<WpsInfo> CREATOR = new android.os.Parcelable.Creator<WpsInfo>() {
		public WpsInfo createFromParcel(android.os.Parcel in) {
			WpsInfo config = new WpsInfo();
			config.setup = in.readInt();
			config.BSSID = in.readString();
			config.pin = in.readString();
			return config;
		}

		public WpsInfo[] newArray(int size) {
			return new WpsInfo[size];
		}
	};
}
