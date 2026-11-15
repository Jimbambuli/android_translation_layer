
package android.media.midi;
import android.os.Parcel;
import android.os.Parcelable;
/* AOSP media/java/android/media/midi/MidiDeviceStatus.java */
public final class MidiDeviceStatus implements Parcelable {
	private static final String TAG = "MidiDeviceStatus";
	private final MidiDeviceInfo mDeviceInfo;
	private final boolean mInputPortOpen[];
	private final int mOutputPortOpenCount[];
	public MidiDeviceStatus(MidiDeviceInfo deviceInfo, boolean inputPortOpen[],
			int outputPortOpenCount[]) {
		mDeviceInfo = deviceInfo;
		mInputPortOpen = new boolean[inputPortOpen.length];
		System.arraycopy(inputPortOpen, 0, mInputPortOpen, 0, inputPortOpen.length);
		mOutputPortOpenCount = new int[outputPortOpenCount.length];
		System.arraycopy(outputPortOpenCount, 0, mOutputPortOpenCount, 0,
				outputPortOpenCount.length);
	}
	public MidiDeviceStatus(MidiDeviceInfo deviceInfo) {
		mDeviceInfo = deviceInfo;
		mInputPortOpen = new boolean[deviceInfo.getInputPortCount()];
		mOutputPortOpenCount = new int[deviceInfo.getOutputPortCount()];
	}
	public MidiDeviceInfo getDeviceInfo() {
		return mDeviceInfo;
	}
	public boolean isInputPortOpen(int portNumber) {
		return mInputPortOpen[portNumber];
	}
	public int getOutputPortOpenCount(int portNumber) {
		return mOutputPortOpenCount[portNumber];
	}
	@Override
	public String toString() {
		int inputPortCount = mDeviceInfo.getInputPortCount();
		int outputPortCount = mDeviceInfo.getOutputPortCount();
		StringBuilder builder = new StringBuilder("mInputPortOpen=[");
		for (int i = 0; i < inputPortCount; i++) {
			builder.append(mInputPortOpen[i]);
			if (i < inputPortCount -1) {
				builder.append(",");
			}
		}
		builder.append("] mOutputPortOpenCount=[");
		for (int i = 0; i < outputPortCount; i++) {
			builder.append(mOutputPortOpenCount[i]);
			if (i < outputPortCount -1) {
				builder.append(",");
			}
		}
		builder.append("]");
		return builder.toString();
	}
	public static final @android.annotation.NonNull Parcelable.Creator<MidiDeviceStatus> CREATOR =
		new Parcelable.Creator<MidiDeviceStatus>() {
		public MidiDeviceStatus createFromParcel(Parcel in) {
			ClassLoader classLoader = MidiDeviceInfo.class.getClassLoader();
			MidiDeviceInfo deviceInfo = in.readParcelable(classLoader, android.media.midi.MidiDeviceInfo.class);
			boolean[] inputPortOpen = in.createBooleanArray();
			int[] outputPortOpenCount = in.createIntArray();
			return new MidiDeviceStatus(deviceInfo, inputPortOpen, outputPortOpenCount);
		}
		public MidiDeviceStatus[] newArray(int size) {
			return new MidiDeviceStatus[size];
		}
	};
	public int describeContents() {
		return 0;
	}
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeParcelable(mDeviceInfo, flags);
		parcel.writeBooleanArray(mInputPortOpen);
		parcel.writeIntArray(mOutputPortOpenCount);
   }
}
