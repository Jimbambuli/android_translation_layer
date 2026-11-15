package android.hardware.usb;

/* AOSP core/java/android/hardware/usb/UsbInterface.java */
public class UsbInterface implements android.os.Parcelable {
	private final int mId;
	private final int mAlternateSetting;
	private final String mName;
	private final int mClass;
	private final int mSubclass;
	private final int mProtocol;
	private UsbEndpoint[] mEndpoints = new UsbEndpoint[0];

	public UsbInterface(int id, int alternateSetting, String name, int Class, int subClass, int protocol) {
		mId = id;
		mAlternateSetting = alternateSetting;
		mName = name;
		mClass = Class;
		mSubclass = subClass;
		mProtocol = protocol;
	}

	public int getId() { return mId; }

	public int getAlternateSetting() { return mAlternateSetting; }

	public String getName() { return mName; }

	public int getInterfaceClass() { return mClass; }

	public int getInterfaceSubclass() { return mSubclass; }

	public int getInterfaceProtocol() { return mProtocol; }

	public int getEndpointCount() { return mEndpoints.length; }

	public UsbEndpoint getEndpoint(int index) { return mEndpoints[index]; }

	public void setEndpoints(UsbEndpoint[] endpoints) {
		mEndpoints = com.android.internal.util.Preconditions.checkArrayElementsNotNull(endpoints, "endpoints");
	}

	public static final android.os.Parcelable.Creator<UsbInterface> CREATOR = new android.os.Parcelable.Creator<UsbInterface>() {
		public UsbInterface createFromParcel(android.os.Parcel in) {
			UsbInterface intf = new UsbInterface(in.readInt(), in.readInt(), in.readString(), in.readInt(), in.readInt(), in.readInt());
			android.os.Parcelable[] p = UsbEndpoint.readArray(in, UsbEndpoint.class);
			UsbEndpoint[] endpoints = new UsbEndpoint[p.length];
			System.arraycopy(p, 0, endpoints, 0, p.length);
			intf.setEndpoints(endpoints);
			return intf;
		}

		public UsbInterface[] newArray(int size) {
			return new UsbInterface[size];
		}
	};

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel parcel, int flags) {
		parcel.writeInt(mId);
		parcel.writeInt(mAlternateSetting);
		parcel.writeString(mName);
		parcel.writeInt(mClass);
		parcel.writeInt(mSubclass);
		parcel.writeInt(mProtocol);
		parcel.writeParcelableArray(mEndpoints, 0);
	}
}
