package android.os;

/* AOSP android.os.VibrationAttributes (API 30): what a vibration is for. ATL's vibrator treats every
 * vibration the same; usage, original audio usage and flags are kept */
public final class VibrationAttributes implements Parcelable {
	public static final int USAGE_FILTER_MATCH_ALL = -1;
	public static final int USAGE_CLASS_UNKNOWN = 0x0;
	public static final int USAGE_CLASS_ALARM = 0x1;
	public static final int USAGE_CLASS_FEEDBACK = 0x2;
	public static final int USAGE_CLASS_MEDIA = 0x3;
	public static final int USAGE_CLASS_MASK = 0xF;

	public static final int USAGE_UNKNOWN = 0x0 | USAGE_CLASS_UNKNOWN;
	public static final int USAGE_ALARM = 0x10 | USAGE_CLASS_ALARM;
	public static final int USAGE_RINGTONE = 0x20 | USAGE_CLASS_ALARM;
	public static final int USAGE_NOTIFICATION = 0x30 | USAGE_CLASS_ALARM;
	public static final int USAGE_COMMUNICATION_REQUEST = 0x40 | USAGE_CLASS_ALARM;
	public static final int USAGE_TOUCH = 0x10 | USAGE_CLASS_FEEDBACK;
	public static final int USAGE_PHYSICAL_EMULATION = 0x20 | USAGE_CLASS_FEEDBACK;
	public static final int USAGE_HARDWARE_FEEDBACK = 0x30 | USAGE_CLASS_FEEDBACK;
	public static final int USAGE_ACCESSIBILITY = 0x40 | USAGE_CLASS_FEEDBACK;
	public static final int USAGE_IME_FEEDBACK = 0x50 | USAGE_CLASS_FEEDBACK;
	public static final int USAGE_GESTURE_INPUT = 0x60 | USAGE_CLASS_FEEDBACK;
	public static final int USAGE_MEDIA = 0x10 | USAGE_CLASS_MEDIA;

	public static final int FLAG_BYPASS_INTERRUPTION_POLICY = 1;
	public static final int FLAG_BYPASS_USER_VIBRATION_INTENSITY_OFF = 1 << 1;
	public static final int FLAG_INVALIDATE_SETTINGS_CACHE = 1 << 2;
	public static final int FLAG_PIPELINED_EFFECT = 1 << 3;
	public static final int FLAG_ALL_SUPPORTED = FLAG_BYPASS_INTERRUPTION_POLICY
			| FLAG_BYPASS_USER_VIBRATION_INTENSITY_OFF | FLAG_INVALIDATE_SETTINGS_CACHE | FLAG_PIPELINED_EFFECT;

	private final int mUsage;
	private final int mOriginalAudioUsage;
	private final int mFlags;

	private VibrationAttributes(int usage, int audioUsage, int flags) {
		mUsage = usage;
		mOriginalAudioUsage = audioUsage;
		mFlags = flags & FLAG_ALL_SUPPORTED;
	}

	public int getUsageClass() {
		return mUsage & USAGE_CLASS_MASK;
	}

	public int getUsage() {
		return mUsage;
	}

	/** @hide */
	public int getOriginalAudioUsage() {
		return mOriginalAudioUsage;
	}

	public int getFlags() {
		return mFlags;
	}

	public boolean isFlagSet(int flag) {
		return (mFlags & flag) > 0;
	}

	public static VibrationAttributes createForUsage(int usage) {
		return new Builder().setUsage(usage).build();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mUsage);
		dest.writeInt(mOriginalAudioUsage);
		dest.writeInt(mFlags);
	}

	private VibrationAttributes(Parcel src) {
		mUsage = src.readInt();
		mOriginalAudioUsage = src.readInt();
		mFlags = src.readInt();
	}

	public static final Parcelable.Creator<VibrationAttributes> CREATOR = new Parcelable.Creator<VibrationAttributes>() {
		public VibrationAttributes createFromParcel(Parcel p) {
			return new VibrationAttributes(p);
		}

		public VibrationAttributes[] newArray(int size) {
			return new VibrationAttributes[size];
		}
	};

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		VibrationAttributes rhs = (VibrationAttributes)o;
		return mUsage == rhs.mUsage && mOriginalAudioUsage == rhs.mOriginalAudioUsage && mFlags == rhs.mFlags;
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(mUsage, mOriginalAudioUsage, mFlags);
	}

	@Override
	public String toString() {
		return "VibrationAttributes{" + "mUsage=" + usageToString() + ", mAudioUsage= "
		    + android.media.AudioAttributes.usageToString(mOriginalAudioUsage) + ", mFlags=" + mFlags + '}';
	}

	/** @hide */
	public String usageToString() {
		return usageToString(mUsage);
	}

	/** @hide */
	public static String usageToString(int usage) {
		switch (usage) {
			case USAGE_UNKNOWN:
				return "UNKNOWN";
			case USAGE_ALARM:
				return "ALARM";
			case USAGE_ACCESSIBILITY:
				return "ACCESSIBILITY";
			case USAGE_RINGTONE:
				return "RINGTONE";
			case USAGE_NOTIFICATION:
				return "NOTIFICATION";
			case USAGE_COMMUNICATION_REQUEST:
				return "COMMUNICATION_REQUEST";
			case USAGE_MEDIA:
				return "MEDIA";
			case USAGE_TOUCH:
				return "TOUCH";
			case USAGE_PHYSICAL_EMULATION:
				return "PHYSICAL_EMULATION";
			case USAGE_HARDWARE_FEEDBACK:
				return "HARDWARE_FEEDBACK";
			case USAGE_IME_FEEDBACK:
				return "IME";
			case USAGE_GESTURE_INPUT:
				return "GESTURE_INPUT";
			default:
				return "unknown usage " + usage;
		}
	}

	public static final class Builder {
		private int mUsage = USAGE_UNKNOWN;
		private int mOriginalAudioUsage = android.media.AudioAttributes.USAGE_UNKNOWN;
		private int mFlags = 0x0;

		public Builder() {}

		/* AOSP Builder(VibrationAttributes): null leaves the defaults */
		public Builder(VibrationAttributes vib) {
			if (vib != null) {
				mUsage = vib.mUsage;
				mOriginalAudioUsage = vib.mOriginalAudioUsage;
				mFlags = vib.mFlags;
			}
		}

		/* AOSP Builder(AudioAttributes): setUsage(AudioAttributes) and setFlags(AudioAttributes) */
		public Builder(android.media.AudioAttributes audio) {
			setUsage(audio);
			setFlags(audio);
		}

		private void setUsage(android.media.AudioAttributes audio) {
			mOriginalAudioUsage = audio.getUsage();
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

		private void setFlags(android.media.AudioAttributes audio) {
			if ((audio.getAllFlags() & android.media.AudioAttributes.FLAG_BYPASS_INTERRUPTION_POLICY) != 0) {
				mFlags |= FLAG_BYPASS_INTERRUPTION_POLICY;
			}
			if ((audio.getAllFlags() & android.media.AudioAttributes.FLAG_BYPASS_MUTE) != 0) {
				mFlags |= FLAG_BYPASS_USER_VIBRATION_INTENSITY_OFF;
			}
		}

		public Builder setUsage(int usage) {
			mOriginalAudioUsage = android.media.AudioAttributes.USAGE_UNKNOWN;
			mUsage = usage;
			return this;
		}

		public Builder setFlags(int flags, int mask) {
			mask &= FLAG_ALL_SUPPORTED;
			mFlags = (mFlags & ~mask) | (flags & mask);
			return this;
		}

		public Builder setFlags(int flags) {
			return setFlags(flags, FLAG_ALL_SUPPORTED);
		}

		public VibrationAttributes build() {
			return new VibrationAttributes(mUsage, mOriginalAudioUsage, mFlags);
		}
	}
}
