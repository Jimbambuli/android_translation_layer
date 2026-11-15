package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/* AOSP core/java/android/hardware/usb/UsbAccessory.java. The serial number is kept as a string instead of an
 * IUsbSerialReader binder (no system process in ATL); the Parcel carries it as a string. */
public class UsbAccessory implements Parcelable {
	public static final int MANUFACTURER_STRING = 0;
	public static final int MODEL_STRING = 1;
	public static final int DESCRIPTION_STRING = 2;
	public static final int VERSION_STRING = 3;
	public static final int URI_STRING = 4;
	public static final int SERIAL_STRING = 5;

	private final String mManufacturer;
	private final String mModel;
	private final String mDescription;
	private final String mVersion;
	private final String mUri;
	private final String mSerial;

	@Deprecated
	public UsbAccessory(String manufacturer, String model, String description, String version, String uri, String serialNumber) {
		mManufacturer = Objects.requireNonNull(manufacturer);
		mModel = Objects.requireNonNull(model);
		mDescription = description;
		mVersion = version;
		mUri = uri;
		mSerial = serialNumber;
	}

	public String getManufacturer() { return mManufacturer; }

	public String getModel() { return mModel; }

	public String getDescription() { return mDescription; }

	public String getVersion() { return mVersion; }

	public String getUri() { return mUri; }

	public String getSerial() { return mSerial; }

	private static boolean compare(String s1, String s2) {
		if (s1 == null)
			return (s2 == null);
		return s1.equals(s2);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof UsbAccessory) {
			UsbAccessory accessory = (UsbAccessory)obj;
			return compare(mManufacturer, accessory.getManufacturer()) && compare(mModel, accessory.getModel()) &&
			    compare(mDescription, accessory.getDescription()) && compare(mVersion, accessory.getVersion()) &&
			    compare(mUri, accessory.getUri()) && compare(getSerial(), accessory.getSerial());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return mManufacturer.hashCode() ^ mModel.hashCode() ^ (mDescription == null ? 0 : mDescription.hashCode()) ^
		    (mVersion == null ? 0 : mVersion.hashCode()) ^ (mUri == null ? 0 : mUri.hashCode());
	}

	@Override
	public String toString() {
		return "UsbAccessory[mManufacturer=" + mManufacturer + ", mModel=" + mModel + ", mDescription=" + mDescription +
		    ", mVersion=" + mVersion + ", mUri=" + mUri + ", mSerial=" + mSerial + "]";
	}

	public static final Parcelable.Creator<UsbAccessory> CREATOR = new Parcelable.Creator<UsbAccessory>() {
		public UsbAccessory createFromParcel(Parcel in) {
			return new UsbAccessory(in.readString(), in.readString(), in.readString(), in.readString(), in.readString(),
			                        in.readString());
		}

		public UsbAccessory[] newArray(int size) {
			return new UsbAccessory[size];
		}
	};

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(mManufacturer);
		parcel.writeString(mModel);
		parcel.writeString(mDescription);
		parcel.writeString(mVersion);
		parcel.writeString(mUri);
		parcel.writeString(mSerial);
	}
}
