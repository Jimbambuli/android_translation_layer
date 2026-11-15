package android.hardware.usb;

/* AOSP core/java/android/hardware/usb/UsbDevice.java without the Parcelable part and the serial-number
 * reader (see UsbEndpoint): ATL lists no USB devices, so no instance is ever handed out. */
public class UsbDevice {
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
}
