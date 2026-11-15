
package android.media.midi;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
/* AOSP media/java/android/media/midi/MidiDeviceInfo.java. The @hide constructor and PROPERTY_ALSA_*, PROPERTY_SERVICE_INFO are
 * package-private: ATL has no MidiService that would create instances. */
public final class MidiDeviceInfo implements Parcelable {
	private static final String TAG = "MidiDeviceInfo";
	public static final int TYPE_USB = 1;
	public static final int TYPE_VIRTUAL = 2;
	public static final int TYPE_BLUETOOTH = 3;
	public static final int PROTOCOL_UMP_USE_MIDI_CI = 0;
	public static final int PROTOCOL_UMP_MIDI_1_0_UP_TO_64_BITS = 1;
	public static final int PROTOCOL_UMP_MIDI_1_0_UP_TO_64_BITS_AND_JRTS = 2;
	public static final int PROTOCOL_UMP_MIDI_1_0_UP_TO_128_BITS = 3;
	public static final int PROTOCOL_UMP_MIDI_1_0_UP_TO_128_BITS_AND_JRTS = 4;
	public static final int PROTOCOL_UMP_MIDI_2_0 = 17;
	public static final int PROTOCOL_UMP_MIDI_2_0_AND_JRTS = 18;
	public static final int PROTOCOL_UNKNOWN = -1;
	public static final String PROPERTY_NAME = "name";
	public static final String PROPERTY_MANUFACTURER = "manufacturer";
	public static final String PROPERTY_PRODUCT = "product";
	public static final String PROPERTY_VERSION = "version";
	public static final String PROPERTY_SERIAL_NUMBER = "serial_number";
	public static final String PROPERTY_USB_DEVICE = "usb_device";
	public static final String PROPERTY_BLUETOOTH_DEVICE = "bluetooth_device";
	static final String PROPERTY_ALSA_CARD = "alsa_card";
	static final String PROPERTY_ALSA_DEVICE = "alsa_device";
	static final String PROPERTY_SERVICE_INFO = "service_info";
	public static final class PortInfo {
		public static final int TYPE_INPUT = 1;
		public static final int TYPE_OUTPUT = 2;
		private final int mPortType;
		private final int mPortNumber;
		private final String mName;
		PortInfo(int type, int portNumber, String name) {
			mPortType = type;
			mPortNumber = portNumber;
			mName = (name == null ? "" : name);
		}
		public int getType() {
			return mPortType;
		}
		public int getPortNumber() {
			return mPortNumber;
		}
		public String getName() {
			return mName;
		}
	}
	private final int mType;
	private final int mId;
	private final int mInputPortCount;
	private final int mOutputPortCount;
	private final String[] mInputPortNames;
	private final String[] mOutputPortNames;
	private final Bundle mProperties;
	private final boolean mIsPrivate;
	private final int mDefaultProtocol;
	MidiDeviceInfo(int type, int id, int numInputPorts, int numOutputPorts,
			String[] inputPortNames, String[] outputPortNames, Bundle properties,
			boolean isPrivate, int defaultProtocol) {
		final int maxPorts = 256;
		if (numInputPorts < 0 || numInputPorts > maxPorts) {
			throw new IllegalArgumentException("numInputPorts out of range = "
					+ numInputPorts);
		}
		if (numOutputPorts < 0 || numOutputPorts > maxPorts) {
			throw new IllegalArgumentException("numOutputPorts out of range = "
					+ numOutputPorts);
		}
		mType = type;
		mId = id;
		mInputPortCount = numInputPorts;
		mOutputPortCount = numOutputPorts;
		if (inputPortNames == null) {
			mInputPortNames = new String[numInputPorts];
		} else {
			mInputPortNames = inputPortNames;
		}
		if (outputPortNames == null) {
			mOutputPortNames = new String[numOutputPorts];
		} else {
			mOutputPortNames = outputPortNames;
		}
		mProperties = properties;
		mIsPrivate = isPrivate;
		mDefaultProtocol = defaultProtocol;
	}
	public int getType() {
		return mType;
	}
	public int getId() {
		return mId;
	}
	public int getInputPortCount() {
		return mInputPortCount;
	}
	public int getOutputPortCount() {
		return mOutputPortCount;
	}
	public PortInfo[] getPorts() {
		PortInfo[] ports = new PortInfo[mInputPortCount + mOutputPortCount];
		int index = 0;
		for (int i = 0; i < mInputPortCount; i++) {
			ports[index++] = new PortInfo(PortInfo.TYPE_INPUT, i, mInputPortNames[i]);
		}
		for (int i = 0; i < mOutputPortCount; i++) {
			ports[index++] = new PortInfo(PortInfo.TYPE_OUTPUT, i, mOutputPortNames[i]);
		}
		return ports;
	}
	public Bundle getProperties() {
		return mProperties;
	}
	public boolean isPrivate() {
		return mIsPrivate;
	}
	public int getDefaultProtocol() {
		return mDefaultProtocol;
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof MidiDeviceInfo) {
			return (((MidiDeviceInfo)o).mId == mId);
		} else {
			return false;
		}
	}
	@Override
	public int hashCode() {
		return mId;
	}
	@Override
	public String toString() {
		mProperties.getString(PROPERTY_NAME);
		return ("MidiDeviceInfo[mType=" + mType
				+ ",mInputPortCount=" + mInputPortCount
				+ ",mOutputPortCount=" + mOutputPortCount
				+ ",mProperties=" + mProperties
				+ ",mIsPrivate=" + mIsPrivate
				+ ",mDefaultProtocol=" + mDefaultProtocol);
	}
	public static final @android.annotation.NonNull Parcelable.Creator<MidiDeviceInfo> CREATOR =
		new Parcelable.Creator<MidiDeviceInfo>() {
		public MidiDeviceInfo createFromParcel(Parcel in) {
			int type = in.readInt();
			int id = in.readInt();
			int inputPortCount = in.readInt();
			int outputPortCount = in.readInt();
			String[] inputPortNames = in.createStringArray();
			String[] outputPortNames = in.createStringArray();
			boolean isPrivate = (in.readInt() == 1);
			int defaultProtocol = in.readInt();
			Bundle basicPropertiesIgnored = in.readBundle();
			Bundle properties = in.readBundle();
			return new MidiDeviceInfo(type, id, inputPortCount, outputPortCount,
					inputPortNames, outputPortNames, properties, isPrivate,
					defaultProtocol);
		}
		public MidiDeviceInfo[] newArray(int size) {
			return new MidiDeviceInfo[size];
		}
	};
	public int describeContents() {
		return 0;
	}
	private Bundle getBasicProperties(String[] keys) {
		Bundle basicProperties = new Bundle();
		for (String key : keys) {
			Object val = mProperties.get(key);
			if (val != null) {
				if (val instanceof String) {
					basicProperties.putString(key, (String) val);
				} else if (val instanceof Integer) {
					basicProperties.putInt(key, (Integer) val);
				} else {
					Log.w(TAG, "Unsupported property type: " + val.getClass().getName());
				}
			}
		}
		return basicProperties;
	}
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(mType);
		parcel.writeInt(mId);
		parcel.writeInt(mInputPortCount);
		parcel.writeInt(mOutputPortCount);
		parcel.writeStringArray(mInputPortNames);
		parcel.writeStringArray(mOutputPortNames);
		parcel.writeInt(mIsPrivate ? 1 : 0);
		parcel.writeInt(mDefaultProtocol);
		parcel.writeBundle(getBasicProperties(new String[] {
			PROPERTY_NAME, PROPERTY_MANUFACTURER, PROPERTY_PRODUCT, PROPERTY_VERSION,
			PROPERTY_SERIAL_NUMBER, PROPERTY_ALSA_CARD, PROPERTY_ALSA_DEVICE
		}));
		parcel.writeBundle(mProperties);
   }
}
