package android.hardware.usb;

/* AOSP core/java/android/hardware/usb/UsbConfiguration.java */
public class UsbConfiguration implements android.os.Parcelable {
	private final int mId;
	private final String mName;
	private final int mAttributes;
	private final int mMaxPower;
	private UsbInterface[] mInterfaces = new UsbInterface[0];

	private static final int ATTR_SELF_POWERED = 1 << 6;
	private static final int ATTR_REMOTE_WAKEUP = 1 << 5;

	public UsbConfiguration(int id, String name, int attributes, int maxPower) {
		mId = id;
		mName = name;
		mAttributes = attributes;
		mMaxPower = maxPower;
	}

	public int getId() { return mId; }

	public String getName() { return mName; }

	public boolean isSelfPowered() { return (mAttributes & ATTR_SELF_POWERED) != 0; }

	public boolean isRemoteWakeup() { return (mAttributes & ATTR_REMOTE_WAKEUP) != 0; }

	public int getAttributes() { return mAttributes; }

	public int getMaxPower() { return mMaxPower * 2; }

	public int getInterfaceCount() { return mInterfaces.length; }

	public UsbInterface getInterface(int index) { return mInterfaces[index]; }

	public void setInterfaces(UsbInterface[] interfaces) {
		mInterfaces = com.android.internal.util.Preconditions.checkArrayElementsNotNull(interfaces, "interfaces");
	}

	public static final android.os.Parcelable.Creator<UsbConfiguration> CREATOR = new android.os.Parcelable.Creator<UsbConfiguration>() {
		public UsbConfiguration createFromParcel(android.os.Parcel in) {
			UsbConfiguration configuration = new UsbConfiguration(in.readInt(), in.readString(), in.readInt(), in.readInt());
			android.os.Parcelable[] p = UsbEndpoint.readArray(in, UsbInterface.class);
			UsbInterface[] interfaces = new UsbInterface[p.length];
			System.arraycopy(p, 0, interfaces, 0, p.length);
			configuration.setInterfaces(interfaces);
			return configuration;
		}

		public UsbConfiguration[] newArray(int size) {
			return new UsbConfiguration[size];
		}
	};

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel parcel, int flags) {
		parcel.writeInt(mId);
		parcel.writeString(mName);
		parcel.writeInt(mAttributes);
		parcel.writeInt(mMaxPower);
		parcel.writeParcelableArray(mInterfaces, 0);
	}
}
