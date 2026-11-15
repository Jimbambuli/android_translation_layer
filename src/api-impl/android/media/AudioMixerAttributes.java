package android.media;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/* AOSP media/java/android/media/AudioMixerAttributes.java */
public final class AudioMixerAttributes implements Parcelable {
	public static final int MIXER_BEHAVIOR_DEFAULT = 0;
	public static final int MIXER_BEHAVIOR_BIT_PERFECT = 1;

	@IntDef(flag = false, prefix = "MIXER_BEHAVIOR_", value = {MIXER_BEHAVIOR_DEFAULT, MIXER_BEHAVIOR_BIT_PERFECT})
	@Retention(RetentionPolicy.SOURCE)
	public @interface MixerBehavior {}

	private final AudioFormat mFormat;
	private final int mMixerBehavior;

	AudioMixerAttributes(AudioFormat format, int mixerBehavior) {
		mFormat = format;
		mMixerBehavior = mixerBehavior;
	}

	@NonNull
	public AudioFormat getFormat() {
		return mFormat;
	}

	public int getMixerBehavior() {
		return mMixerBehavior;
	}

	public static final class Builder {
		private final AudioFormat mFormat;
		private int mMixerBehavior = MIXER_BEHAVIOR_DEFAULT;

		public Builder(@NonNull AudioFormat format) {
			Objects.requireNonNull(format);
			mFormat = format;
		}

		@NonNull
		public AudioMixerAttributes build() {
			return new AudioMixerAttributes(mFormat, mMixerBehavior);
		}

		@NonNull
		public Builder setMixerBehavior(int mixerBehavior) {
			switch (mixerBehavior) {
				case MIXER_BEHAVIOR_DEFAULT:
				case MIXER_BEHAVIOR_BIT_PERFECT:
					mMixerBehavior = mixerBehavior;
					break;
				default:
					throw new IllegalArgumentException("Invalid mixer behavior " + mixerBehavior);
			}
			return this;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(mFormat, mMixerBehavior);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AudioMixerAttributes that = (AudioMixerAttributes) o;
		return (mFormat.equals(that.mFormat) && (mMixerBehavior == that.mMixerBehavior));
	}

	private String mixerBehaviorToString(int mixerBehavior) {
		switch (mixerBehavior) {
			case MIXER_BEHAVIOR_DEFAULT:
				return "default";
			case MIXER_BEHAVIOR_BIT_PERFECT:
				return "bit-perfect";
			default:
				return "unknown";
		}
	}

	@Override
	public String toString() {
		return new String("AudioMixerAttributes:" + " format:" + mFormat.toString() + " mixer behavior:" + mixerBehaviorToString(mMixerBehavior));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeParcelable(mFormat, flags);
		dest.writeInt(mMixerBehavior);
	}

	private AudioMixerAttributes(@NonNull Parcel in) {
		mFormat = in.readParcelable(AudioFormat.class.getClassLoader(), AudioFormat.class);
		mMixerBehavior = in.readInt();
	}

	@NonNull
	public static final Parcelable.Creator<AudioMixerAttributes> CREATOR = new Parcelable.Creator<AudioMixerAttributes>() {
		public AudioMixerAttributes createFromParcel(Parcel p) {
			return new AudioMixerAttributes(p);
		}

		public AudioMixerAttributes[] newArray(int size) {
			return new AudioMixerAttributes[size];
		}
	};
}
