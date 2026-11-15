package android.hardware.usb;

/* AOSP core/java/android/hardware/usb/UsbEndpoint.java . ATL lists no USB
 * devices (UsbManager.getDeviceList() is empty), so no endpoint is ever handed out. */
public class UsbEndpoint implements android.os.Parcelable {
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

	public static final android.os.Parcelable.Creator<UsbEndpoint> CREATOR = new android.os.Parcelable.Creator<UsbEndpoint>() {
		public UsbEndpoint createFromParcel(android.os.Parcel in) {
			return new UsbEndpoint(in.readInt(), in.readInt(), in.readInt(), in.readInt());
		}

		public UsbEndpoint[] newArray(int size) {
			return new UsbEndpoint[size];
		}
	};

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel parcel, int flags) {
		parcel.writeInt(mAddress);
		parcel.writeInt(mAttributes);
		parcel.writeInt(mMaxPacketSize);
		parcel.writeInt(mInterval);
	}

	/* Parcel.readParcelableArray() throws a checked exception, which a Creator cannot */
	static android.os.Parcelable[] readArray(android.os.Parcel in, Class<?> type) {
		try {
			return in.readParcelableArray(type.getClassLoader());
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
