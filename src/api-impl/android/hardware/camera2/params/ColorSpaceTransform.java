package android.hardware.camera2.params;

import static com.android.internal.util.Preconditions.*;

import android.hardware.camera2.utils.HashCodeHelpers;
import android.util.Rational;

import java.util.Arrays;

/* AOSP core/java/android/hardware/camera2/params/ColorSpaceTransform.java */
public final class ColorSpaceTransform {

	private static final int ROWS = 3;

	private static final int COLUMNS = 3;

	private static final int COUNT = ROWS * COLUMNS;

	private static final int RATIONAL_SIZE = 2;

	private static final int OFFSET_NUMERATOR = 0;

	private static final int OFFSET_DENOMINATOR = 1;

	private static final int COUNT_INT = ROWS * COLUMNS * RATIONAL_SIZE;

	public ColorSpaceTransform(Rational[] elements) {

		checkNotNull(elements, "elements must not be null");
		if (elements.length != COUNT) {
			throw new IllegalArgumentException("elements must be " + COUNT + " length");
		}

		mElements = new int[COUNT_INT];

		for (int i = 0; i < elements.length; ++i) {
			checkNotNull(elements, "element[%d] must not be null", i);
			mElements[i * RATIONAL_SIZE + OFFSET_NUMERATOR] = elements[i].getNumerator();
			mElements[i * RATIONAL_SIZE + OFFSET_DENOMINATOR] = elements[i].getDenominator();
		}
	}

	public ColorSpaceTransform(int[] elements) {
		checkNotNull(elements, "elements must not be null");
		if (elements.length != COUNT_INT) {
			throw new IllegalArgumentException("elements must be " + COUNT_INT + " length");
		}

		for (int i = 0; i < elements.length; ++i) {
			checkNotNull(elements, "element %d must not be null", i);
		}

		mElements = Arrays.copyOf(elements, elements.length);
	}

	public Rational getElement(int column, int row) {
		if (column < 0 || column >= COLUMNS) {
			throw new IllegalArgumentException("column out of range");
		} else if (row < 0 || row >= ROWS) {
			throw new IllegalArgumentException("row out of range");
		}

		int numerator = mElements[(row * COLUMNS + column) * RATIONAL_SIZE + OFFSET_NUMERATOR];
		int denominator = mElements[(row * COLUMNS + column) * RATIONAL_SIZE + OFFSET_DENOMINATOR];

		return new Rational(numerator, denominator);
	}

	public void copyElements(Rational[] destination, int offset) {
		checkArgumentNonnegative(offset, "offset must not be negative");
		checkNotNull(destination, "destination must not be null");
		if (destination.length - offset < COUNT) {
			throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");
		}

		for (int i = 0, j = 0; i < COUNT; ++i, j += RATIONAL_SIZE) {
			int numerator = mElements[j + OFFSET_NUMERATOR];
			int denominator = mElements[j + OFFSET_DENOMINATOR];

			destination[i + offset] = new Rational(numerator, denominator);
		}
	}

	public void copyElements(int[] destination, int offset) {
		checkArgumentNonnegative(offset, "offset must not be negative");
		checkNotNull(destination, "destination must not be null");
		if (destination.length - offset < COUNT_INT) {
			throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");
		}
		for (int i = 0; i < COUNT_INT; ++i) {
			destination[i + offset] = mElements[i];
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj instanceof ColorSpaceTransform) {
			final ColorSpaceTransform other = (ColorSpaceTransform) obj;
			for (int i = 0, j = 0; i < COUNT; ++i, j += RATIONAL_SIZE) {
				int numerator = mElements[j + OFFSET_NUMERATOR];
				int denominator = mElements[j + OFFSET_DENOMINATOR];
				int numeratorOther = other.mElements[j + OFFSET_NUMERATOR];
				int denominatorOther = other.mElements[j + OFFSET_DENOMINATOR];
				Rational r = new Rational(numerator, denominator);
				Rational rOther = new Rational(numeratorOther, denominatorOther);
				if (!r.equals(rOther)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return HashCodeHelpers.hashCode(mElements);
	}

	@Override
	public String toString() {
		return String.format("ColorSpaceTransform%s", toShortString());
	}

	private String toShortString() {
		StringBuilder sb = new StringBuilder("(");
		for (int row = 0, i = 0; row < ROWS; row++) {
			sb.append("[");
			for (int col = 0; col < COLUMNS; col++, i += RATIONAL_SIZE) {
				int numerator = mElements[i + OFFSET_NUMERATOR];
				int denominator = mElements[i + OFFSET_DENOMINATOR];
				sb.append(numerator);
				sb.append("/");
				sb.append(denominator);
				if (col < COLUMNS - 1) {
					sb.append(", ");
				}
			}
			sb.append("]");
			if (row < ROWS - 1) {
				sb.append(", ");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	private final int[] mElements;
}
