/* AOSP BlackLevelPattern.java (Android 16) */
package android.hardware.camera2.params;

import java.util.Arrays;
import java.util.Objects;

public final class BlackLevelPattern {
	public static final int COUNT = 4;

	public BlackLevelPattern(int[] offsets) {
		if (offsets == null) {
			throw new NullPointerException("Null offsets array passed to constructor");
		}
		if (offsets.length < COUNT) {
			throw new IllegalArgumentException("Invalid offsets array length");
		}
		mCfaOffsets = Arrays.copyOf(offsets, COUNT);
	}

	public int getOffsetForIndex(int column, int row) {
		if (row < 0 || column < 0) {
			throw new IllegalArgumentException("column, row arguments must be positive");
		}
		return mCfaOffsets[((row & 1) << 1) | (column & 1)];
	}

	public void copyTo(int[] destination, int offset) {
		Objects.requireNonNull(destination, "destination must not be null");
		if (offset < 0) {
			throw new IllegalArgumentException("Null offset passed to copyTo");
		}
		if (destination.length - offset < COUNT) {
			throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");
		}
		for (int i = 0; i < COUNT; ++i) {
			destination[offset + i] = mCfaOffsets[i];
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (this == obj) {
			return true;
		} else if (obj instanceof BlackLevelPattern) {
			final BlackLevelPattern other = (BlackLevelPattern) obj;
			return Arrays.equals(other.mCfaOffsets, mCfaOffsets);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(mCfaOffsets);
	}

	@Override
	public String toString() {
		return String.format("BlackLevelPattern([%d, %d], [%d, %d])", mCfaOffsets[0], mCfaOffsets[1], mCfaOffsets[2], mCfaOffsets[3]);
	}

	private final int[] mCfaOffsets;
}
