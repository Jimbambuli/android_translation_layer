package android.hardware.usb;

/* AOSP core/java/android/hardware/usb/UsbInterface.java without the Parcelable part (see UsbEndpoint) */
public class UsbInterface {
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
}
