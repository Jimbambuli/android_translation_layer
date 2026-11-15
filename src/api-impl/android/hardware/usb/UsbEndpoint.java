package android.hardware.usb;

/* AOSP core/java/android/hardware/usb/UsbEndpoint.java without the Parcelable part. ATL lists no USB
 * devices (UsbManager.getDeviceList() is empty), so no endpoint is ever handed out. */
public class UsbEndpoint {
	private final int mAddress;
	private final int mAttributes;
	private final int mMaxPacketSize;
	private final int mInterval;

	public UsbEndpoint(int address, int attributes, int maxPacketSize, int interval) {
		mAddress = address;
		mAttributes = attributes;
		mMaxPacketSize = maxPacketSize;
		mInterval = interval;
	}

	public int getAddress() { return mAddress; }

	public int getEndpointNumber() { return mAddress & UsbConstants.USB_ENDPOINT_NUMBER_MASK; }

	public int getDirection() { return mAddress & UsbConstants.USB_ENDPOINT_DIR_MASK; }

	public int getAttributes() { return mAttributes; }

	public int getType() { return mAttributes & UsbConstants.USB_ENDPOINT_XFERTYPE_MASK; }

	public int getMaxPacketSize() { return mMaxPacketSize; }

	public int getInterval() { return mInterval; }

	@Override
	public String toString() {
		return "UsbEndpoint[mAddress=" + mAddress + ",mAttributes=" + mAttributes +
		    ",mMaxPacketSize=" + mMaxPacketSize + ",mInterval=" + mInterval + "]";
	}
}
