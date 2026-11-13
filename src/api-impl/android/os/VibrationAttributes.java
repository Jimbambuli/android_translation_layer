package android.os;

/* AOSP android.os.VibrationAttributes (API 30): what a vibration is for. ATL's vibrator treats every
 * vibration the same, so only the usage is kept */
public final class VibrationAttributes {
	public static final int USAGE_UNKNOWN = 0;
	public static final int USAGE_ALARM = 0x11;
	public static final int USAGE_RINGTONE = 0x21;
	public static final int USAGE_NOTIFICATION = 0x31;
	public static final int USAGE_COMMUNICATION_REQUEST = 0x41;
	public static final int USAGE_TOUCH = 0x12;
	public static final int USAGE_PHYSICAL_EMULATION = 0x22;
	public static final int USAGE_HARDWARE_FEEDBACK = 0x32;
	public static final int USAGE_ACCESSIBILITY = 0x42;
	public static final int USAGE_MEDIA = 0x13;

	private final int mUsage;

	private VibrationAttributes(int usage) {
		mUsage = usage;
	}

	public int getUsage() {
		return mUsage;
	}

	public static VibrationAttributes createForUsage(int usage) {
		return new VibrationAttributes(usage);
	}

	public static final class Builder {
		private int mUsage = USAGE_UNKNOWN;

		public Builder() {}

		public Builder setUsage(int usage) {
			mUsage = usage;
			return this;
		}

		public VibrationAttributes build() {
			return new VibrationAttributes(mUsage);
		}
	}
}
