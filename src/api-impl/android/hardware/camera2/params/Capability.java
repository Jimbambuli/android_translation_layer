/* AOSP Capability.java (Android 16) */
package android.hardware.camera2.params;

import static com.android.internal.util.Preconditions.checkArgumentNonnegative;
import static com.android.internal.util.Preconditions.checkArgumentPositive;

import android.hardware.camera2.utils.HashCodeHelpers;
import android.util.Range;
import android.util.Size;

public final class Capability {
	public static final int COUNT = 3;

	private final int mMode;
	private final Size mMaxStreamingSize;
	private final Range<Float> mZoomRatioRange;

	public Capability(int mode, Size maxStreamingSize, Range<Float> zoomRatioRange) {
		mMode = mode;

		checkArgumentNonnegative(maxStreamingSize.getWidth(), "maxStreamingSize.getWidth() must be nonnegative");
		checkArgumentNonnegative(maxStreamingSize.getHeight(), "maxStreamingSize.getHeight() must be nonnegative");
		mMaxStreamingSize = maxStreamingSize;

		if (zoomRatioRange.getLower() > zoomRatioRange.getUpper()) {
			throw new IllegalArgumentException("zoomRatioRange.getLower() " + zoomRatioRange.getLower() + " is greater than zoomRatioRange.getUpper() " + zoomRatioRange.getUpper());
		}
		checkArgumentPositive(zoomRatioRange.getLower(), "zoomRatioRange.getLower() must be positive");
		checkArgumentPositive(zoomRatioRange.getUpper(), "zoomRatioRange.getUpper() must be positive");
		mZoomRatioRange = zoomRatioRange;
	}

	public int getMode() {
		return mMode;
	}

	public Size getMaxStreamingSize() {
		return mMaxStreamingSize;
	}

	public Range<Float> getZoomRatioRange() {
		return mZoomRatioRange;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj instanceof Capability) {
			final Capability other = (Capability) obj;
			return (mMode == other.mMode && mMaxStreamingSize.equals(other.mMaxStreamingSize) && mZoomRatioRange.equals(other.mZoomRatioRange));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return HashCodeHelpers.hashCode(mMode, mMaxStreamingSize.getWidth(), mMaxStreamingSize.getHeight(), mZoomRatioRange.getLower(), mZoomRatioRange.getUpper());
	}

	@Override
	public String toString() {
		return String.format("(mode:%d, maxStreamingSize:%d x %d, zoomRatio: %f-%f)", mMode, mMaxStreamingSize.getWidth(), mMaxStreamingSize.getHeight(), mZoomRatioRange.getLower(), mZoomRatioRange.getUpper());
	}
}
