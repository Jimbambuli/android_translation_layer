/* AOSP OisSample.java (Android 16) */
package android.hardware.camera2.params;

import android.hardware.camera2.utils.HashCodeHelpers;

import com.android.internal.util.Preconditions;

public final class OisSample {
	public OisSample(final long timestamp, final float xShift, final float yShift) {
		mTimestampNs = timestamp;
		mXShift = Preconditions.checkArgumentFinite(xShift, "xShift must be finite");
		mYShift = Preconditions.checkArgumentFinite(yShift, "yShift must be finite");
	}

	public long getTimestamp() {
		return mTimestampNs;
	}

	public float getXshift() {
		return mXShift;
	}

	public float getYshift() {
		return mYShift;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		} else if (this == obj) {
			return true;
		} else if (obj instanceof OisSample) {
			final OisSample other = (OisSample) obj;
			return mTimestampNs == other.mTimestampNs && mXShift == other.mXShift && mYShift == other.mYShift;
		}
		return false;
	}

	@Override
	public int hashCode() {
		int timestampHash = HashCodeHelpers.hashCode(mTimestampNs);
		return HashCodeHelpers.hashCode(mXShift, mYShift, timestampHash);
	}

	@Override
	public String toString() {
		return String.format("OisSample{timestamp:%d, shift_x:%f, shift_y:%f}", mTimestampNs, mXShift, mYShift);
	}

	private final long mTimestampNs;
	private final float mXShift;
	private final float mYShift;
}
