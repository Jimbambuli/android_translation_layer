package android.hardware.usb;

/* AOSP core/java/android/hardware/usb/UsbConfiguration.java without the Parcelable part (see UsbEndpoint) */
public class UsbConfiguration {
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
}
