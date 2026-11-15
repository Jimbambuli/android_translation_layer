package android.os.vibrator;

import java.util.Objects;

/** android-16 frameworks/base core/java/android/os/vibrator/HapticFeedbackRequest.java. */
public final class HapticFeedbackRequest {

	private final int mFeedbackConstant;
	private final int mUsage;
	private final int mFlags;

	private HapticFeedbackRequest(int feedbackConstant, int usage, int flags) {
		mFeedbackConstant = feedbackConstant;
		mUsage = usage;
		mFlags = flags;
	}

	public int getFeedbackConstant() {
		return mFeedbackConstant;
	}

	public int getUsage() {
		return mUsage;
	}

	public int getFlags() {
		return mFlags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		HapticFeedbackRequest rhs = (HapticFeedbackRequest)o;
		return mFeedbackConstant == rhs.mFeedbackConstant && mUsage == rhs.mUsage && mFlags == rhs.mFlags;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mFeedbackConstant, mUsage, mFlags);
	}

	public static final class Builder {

		private final int mFeedbackConstant;
		private int mUsage = 0; // VibrationAttributes.USAGE_UNKNOWN
		private int mFlags = 0x0;

		public Builder(int constant) {
			mFeedbackConstant = constant;
		}

		public Builder(HapticFeedbackRequest request) {
			Objects.requireNonNull(request);
			mFeedbackConstant = request.mFeedbackConstant;
			mUsage = request.mUsage;
			mFlags = request.mFlags;
		}

		public Builder setUsage(int usage) {
			mUsage = usage;
			return this;
		}

		public Builder setFlags(int flags) {
			mFlags = flags;
			return this;
		}

		public HapticFeedbackRequest build() {
			return new HapticFeedbackRequest(mFeedbackConstant, mUsage, mFlags);
		}
	}
}
