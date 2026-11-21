/* AOSP LensIntrinsicsSample.java (Android 16). Difference: toString uses String.format
 * (ATL's TextUtils has no formatSimple; same output). */
package android.hardware.camera2.params;

import android.hardware.camera2.utils.HashCodeHelpers;

import com.android.internal.util.Preconditions;

import java.util.Arrays;

public final class LensIntrinsicsSample {
	public LensIntrinsicsSample(final long timestampNs, final float[] lensIntrinsics) {
		mTimestampNs = timestampNs;
		Preconditions.checkArgument(lensIntrinsics.length == 5);
		mLensIntrinsics = lensIntrinsics;
	}

	public long getTimestampNanos() {
		return mTimestampNs;
	}

	public float[] getLensIntrinsics() {
		return mLensIntrinsics;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		} else if (this == obj) {
			return true;
		} else if (obj instanceof LensIntrinsicsSample) {
			final LensIntrinsicsSample other = (LensIntrinsicsSample) obj;
			return mTimestampNs == other.mTimestampNs && Arrays.equals(mLensIntrinsics, other.getLensIntrinsics());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int timestampHash = HashCodeHelpers.hashCode(((float) mTimestampNs));
		return HashCodeHelpers.hashCode(Arrays.hashCode(mLensIntrinsics), timestampHash);
	}

	@Override
	public String toString() {
		return String.format("LensIntrinsicsSample{timestamp:%d, sample:%s}", mTimestampNs, Arrays.toString(mLensIntrinsics));
	}

	private final long mTimestampNs;
	private final float[] mLensIntrinsics;
}
