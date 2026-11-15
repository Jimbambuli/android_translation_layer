package android.os;

/* AOSP android.os.PowerMonitor (API 36): a source of energy readings (an on-device power rail or a consumer) */
public final class PowerMonitor implements Parcelable {
	public static final int POWER_MONITOR_TYPE_CONSUMER = 0;
	public static final int POWER_MONITOR_TYPE_MEASUREMENT = 1;

	/** @hide */
	public final int index;
	private final int mType;
	private final String mName;

	/** @hide */
	public PowerMonitor(int index, int type, String name) {
		this.index = index;
		this.mType = type;
		this.mName = name;
	}

	public int getType() {
		return mType;
	}

	public String getName() {
		return mName;
	}

	private PowerMonitor(Parcel in) {
		index = in.readInt();
		mType = in.readInt();
		mName = in.readString8();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(index);
		dest.writeInt(mType);
		dest.writeString8(mName);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<PowerMonitor> CREATOR = new Creator<>() {
		@Override
		public PowerMonitor createFromParcel(Parcel in) {
			return new PowerMonitor(in);
		}

		@Override
		public PowerMonitor[] newArray(int size) {
			return new PowerMonitor[size];
		}
	};
}
