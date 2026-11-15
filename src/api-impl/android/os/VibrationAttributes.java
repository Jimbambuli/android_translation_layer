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

		/* AOSP Builder(VibrationAttributes): null leaves the defaults */
		public Builder(VibrationAttributes vib) {
			if (vib != null) {
				mUsage = vib.mUsage;
			}
		}

		/* AOSP Builder(AudioAttributes): setUsage(AudioAttributes), the flags are not kept by ATL */
		public Builder(android.media.AudioAttributes audio) {
			switch (audio.getUsage()) {
				case android.media.AudioAttributes.USAGE_NOTIFICATION:
				case android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT:
				case android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_DELAYED:
				case android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT:
				case android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST:
					mUsage = USAGE_NOTIFICATION;
					break;
				case android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION:
				case android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING:
				case android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
				case android.media.AudioAttributes.USAGE_ASSISTANT:
					mUsage = USAGE_COMMUNICATION_REQUEST;
					break;
				case android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE:
					mUsage = USAGE_RINGTONE;
					break;
				case android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY:
					mUsage = USAGE_ACCESSIBILITY;
					break;
				case android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION:
					mUsage = USAGE_TOUCH;
					break;
				case android.media.AudioAttributes.USAGE_ALARM:
					mUsage = USAGE_ALARM;
					break;
				case android.media.AudioAttributes.USAGE_MEDIA:
				case android.media.AudioAttributes.USAGE_GAME:
					mUsage = USAGE_MEDIA;
					break;
				default:
					mUsage = USAGE_UNKNOWN;
			}
		}

		public Builder setUsage(int usage) {
			mUsage = usage;
			return this;
		}

		public VibrationAttributes build() {
			return new VibrationAttributes(mUsage);
		}
	}
}
