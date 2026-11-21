/* AOSP (Android 16) android.hardware.camera2.params.StreamConfigurationDuration, comments and annotations stripped; no other differences. */
package android.hardware.camera2.params;

import static com.android.internal.util.Preconditions.*;
import static android.hardware.camera2.params.StreamConfigurationMap.checkArgumentFormatInternal;

import android.hardware.camera2.utils.HashCodeHelpers;
import android.util.Size;

public final class StreamConfigurationDuration {
	public StreamConfigurationDuration(
			final int format, final int width, final int height, final long durationNs) {
		mFormat =  checkArgumentFormatInternal(format);
		mWidth = checkArgumentPositive(width, "width must be positive");
		mHeight = checkArgumentPositive(height, "height must be positive");
		mDurationNs = checkArgumentNonnegative(durationNs, "durationNs must be non-negative");
	}

	public final int getFormat() {
		return mFormat;
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public Size getSize() {
		return new Size(mWidth, mHeight);
	}

	public long getDuration() {
		return mDurationNs;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj instanceof StreamConfigurationDuration) {
			final StreamConfigurationDuration other = (StreamConfigurationDuration) obj;
			return mFormat == other.mFormat &&
					mWidth == other.mWidth &&
					mHeight == other.mHeight &&
					mDurationNs == other.mDurationNs;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return HashCodeHelpers.hashCode(mFormat, mWidth, mHeight,
				(int) mDurationNs, (int)(mDurationNs >>> Integer.SIZE));
	}

	private final int mFormat;
	private final int mWidth;
	private final int mHeight;
	private final long mDurationNs;
}
