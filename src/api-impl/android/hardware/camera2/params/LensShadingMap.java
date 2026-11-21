/* AOSP LensShadingMap.java (Android 16) */
package android.hardware.camera2.params;

import static android.hardware.camera2.params.RggbChannelVector.BLUE;
import static android.hardware.camera2.params.RggbChannelVector.COUNT;
import static android.hardware.camera2.params.RggbChannelVector.GREEN_EVEN;
import static android.hardware.camera2.params.RggbChannelVector.GREEN_ODD;
import static android.hardware.camera2.params.RggbChannelVector.RED;
import static com.android.internal.util.Preconditions.checkArgumentNonnegative;
import static com.android.internal.util.Preconditions.checkArgumentPositive;
import static com.android.internal.util.Preconditions.checkArrayElementsInRange;

import android.hardware.camera2.utils.HashCodeHelpers;

import java.util.Arrays;
import java.util.Objects;

public final class LensShadingMap {
	public static final float MINIMUM_GAIN_FACTOR = 1.0f;

	public LensShadingMap(final float[] elements, final int rows, final int columns) {
		mRows = checkArgumentPositive(rows, "rows must be positive");
		mColumns = checkArgumentPositive(columns, "columns must be positive");
		mElements = Objects.requireNonNull(elements, "elements must not be null");

		if (elements.length != getGainFactorCount()) {
			throw new IllegalArgumentException("elements must be " + getGainFactorCount() + " length, received " + elements.length);
		}

		// Every element must be finite and >= 1.0f
		checkArrayElementsInRange(elements, MINIMUM_GAIN_FACTOR, Float.MAX_VALUE, "elements");
	}

	public int getRowCount() {
		return mRows;
	}

	public int getColumnCount() {
		return mColumns;
	}

	public int getGainFactorCount() {
		return mRows * mColumns * COUNT;
	}

	public float getGainFactor(final int colorChannel, final int column, final int row) {
		if (colorChannel < 0 || colorChannel > COUNT) {
			throw new IllegalArgumentException("colorChannel out of range");
		} else if (column < 0 || column >= mColumns) {
			throw new IllegalArgumentException("column out of range");
		} else if (row < 0 || row >= mRows) {
			throw new IllegalArgumentException("row out of range");
		}

		return mElements[colorChannel + (row * mColumns + column) * COUNT];
	}

	public RggbChannelVector getGainFactorVector(final int column, final int row) {
		if (column < 0 || column >= mColumns) {
			throw new IllegalArgumentException("column out of range");
		} else if (row < 0 || row >= mRows) {
			throw new IllegalArgumentException("row out of range");
		}

		final int offset = (row * mColumns + column) * COUNT;

		final float red = mElements[RED + offset];
		final float greenEven = mElements[GREEN_EVEN + offset];
		final float greenOdd = mElements[GREEN_ODD + offset];
		final float blue = mElements[BLUE + offset];

		return new RggbChannelVector(red, greenEven, greenOdd, blue);
	}

	public void copyGainFactors(final float[] destination, final int offset) {
		checkArgumentNonnegative(offset, "offset must not be negative");
		Objects.requireNonNull(destination, "destination must not be null");

		if (destination.length + offset < getGainFactorCount()) {
			throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");
		}

		System.arraycopy(mElements, /*srcPos*/ 0, destination, offset, getGainFactorCount());
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj instanceof LensShadingMap) {
			final LensShadingMap other = (LensShadingMap) obj;
			return mRows == other.mRows && mColumns == other.mColumns && Arrays.equals(mElements, other.mElements);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int elemsHash = HashCodeHelpers.hashCode(mElements);
		return HashCodeHelpers.hashCode(mRows, mColumns, elemsHash);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("LensShadingMap{");

		final String channelPrefix[] = {"R:(", "G_even:(", "G_odd:(", "B:("};

		for (int ch = 0; ch < COUNT; ch++) {
			str.append(channelPrefix[ch]);

			for (int r = 0; r < mRows; r++) {
				str.append("[");
				for (int c = 0; c < mColumns; c++) {
					float gain = getGainFactor(ch, c, r);
					str.append(gain);
					if (c < mColumns - 1) {
						str.append(", ");
					}
				}
				str.append("]");
				if (r < mRows - 1) {
					str.append(", ");
				}
			}

			str.append(")");
			if (ch < COUNT - 1) {
				str.append(", ");
			}
		}

		str.append("}");
		return str.toString();
	}

	private final int mRows;
	private final int mColumns;
	private final float[] mElements;
}
