package android.hardware.usb;

/* AOSP core/java/android/hardware/usb/UsbDevice.java without the serial-number reader binder (the
 * serial number travels as a string) and the capability flags: ATL lists no USB devices, so no instance is ever handed out. */
public class UsbDevice implements android.os.Parcelable {
	private final String mName;
	private final String mManufacturerName;
	private final String mProductName;
	private final String mVersion;
	private final UsbConfiguration[] mConfigurations;
	private final String mSerialNumber;
	private final int mVendorId;
	private final int mProductId;
	private final int mClass;
	private final int mSubclass;
	private final int mProtocol;
	private UsbInterface[] mInterfaces;

	private UsbDevice(String name, int vendorId, int productId, int Class, int subClass, int protocol,
	                  String manufacturerName, String productName, String version, UsbConfiguration[] configurations,
	                  String serialNumber) {
		mName = name;
		mVendorId = vendorId;
		mProductId = productId;
		mClass = Class;
		mSubclass = subClass;
		mProtocol = protocol;
		mManufacturerName = manufacturerName;
		mProductName = productName;
		mVersion = version;
		mConfigurations = configurations;
		mSerialNumber = serialNumber;
	}

	public String getDeviceName() { return mName; }

	public int getDeviceId() { return getDeviceId(mName); }

	/* AOSP UsbDevice.getDeviceId(String): native_get_device_id() = usb_device_get_unique_id_from_name(), bus * 1000 + device
	 * of "/dev/bus/usb/BBB/DDD", -1 for any other name */
	public static int getDeviceId(String name) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("^/dev/bus/usb/(\\d+)/(\\d+)$").matcher(name);
		if (!m.matches())
			return -1;
		return Integer.parseInt(m.group(1)) * 1000 + Integer.parseInt(m.group(2));
	}

	/* AOSP UsbDevice.getDeviceName(int id): native_get_device_name(), "/dev/bus/usb/BBB/DDD" from id = bus * 1000 + device */
	public static String getDeviceName(int id) {
		return String.format("/dev/bus/usb/%03d/%03d", id / 1000, id % 1000);
	}

	public String getManufacturerName() { return mManufacturerName; }

	public String getProductName() { return mProductName; }

	public String getVersion() { return mVersion; }

	public String getSerialNumber() { return mSerialNumber; }

	public int getVendorId() { return mVendorId; }

	public int getProductId() { return mProductId; }

	public int getDeviceClass() { return mClass; }

	public int getDeviceSubclass() { return mSubclass; }

	public int getDeviceProtocol() { return mProtocol; }

	public int getConfigurationCount() { return mConfigurations.length; }

	public UsbConfiguration getConfiguration(int index) { return mConfigurations[index]; }

	private UsbInterface[] getInterfaceList() {
		if (mInterfaces == null) {
			int interfaceCount = 0;
			for (UsbConfiguration configuration : mConfigurations)
				interfaceCount += configuration.getInterfaceCount();
			mInterfaces = new UsbInterface[interfaceCount];
			int offset = 0;
			for (UsbConfiguration configuration : mConfigurations) {
				for (int j = 0; j < configuration.getInterfaceCount(); j++)
					mInterfaces[offset++] = configuration.getInterface(j);
			}
		}
		return mInterfaces;
	}

	public int getInterfaceCount() { return getInterfaceList().length; }

	public UsbInterface getInterface(int index) { return getInterfaceList()[index]; }

	@Override
	public boolean equals(Object o) {
		if (o instanceof UsbDevice)
			return ((UsbDevice)o).mName.equals(mName);
		else if (o instanceof String)
			return ((String)o).equals(mName);
		return false;
	}

	@Override
	public int hashCode() {
		return mName.hashCode();
	}

	@Override
	public String toString() {
		return "UsbDevice[mName=" + mName + ",mVendorId=" + mVendorId + ",mProductId=" + mProductId +
		    ",mClass=" + mClass + ",mSubclass=" + mSubclass + ",mProtocol=" + mProtocol +
		    ",mManufacturerName=" + mManufacturerName + ",mProductName=" + mProductName +
		    ",mVersion=" + mVersion + "]";
	}

	public static final android.os.Parcelable.Creator<UsbDevice> CREATOR = new android.os.Parcelable.Creator<UsbDevice>() {
		public UsbDevice createFromParcel(android.os.Parcel in) {
			String name = in.readString();
			int vendorId = in.readInt();
			int productId = in.readInt();
			int clasz = in.readInt();
			int subClass = in.readInt();
			int protocol = in.readInt();
			String manufacturerName = in.readString();
			String productName = in.readString();
			String version = in.readString();
			String serialNumber = in.readString();
			android.os.Parcelable[] p = UsbEndpoint.readArray(in, UsbConfiguration.class);
			UsbConfiguration[] configurations = new UsbConfiguration[p.length];
			System.arraycopy(p, 0, configurations, 0, p.length);
			return new UsbDevice(name, vendorId, productId, clasz, subClass, protocol, manufacturerName, productName, version,
			                     configurations, serialNumber);
		}

		public UsbDevice[] newArray(int size) {
			return new UsbDevice[size];
		}
	};

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel parcel, int flags) {
		parcel.writeString(mName);
		parcel.writeInt(mVendorId);
		parcel.writeInt(mProductId);
		parcel.writeInt(mClass);
		parcel.writeInt(mSubclass);
		parcel.writeInt(mProtocol);
		parcel.writeString(mManufacturerName);
		parcel.writeString(mProductName);
		parcel.writeString(mVersion);
		parcel.writeString(mSerialNumber);
		parcel.writeParcelableArray(mConfigurations, 0);
	}
}
