package android.hardware.camera2.params;

import static com.android.internal.util.Preconditions.*;

import android.graphics.PointF;
import android.hardware.camera2.utils.HashCodeHelpers;

import java.util.Arrays;

/* AOSP core/java/android/hardware/camera2/params/TonemapCurve.java */
public final class TonemapCurve {
	
	public static final float LEVEL_BLACK = 0.0f;

	public static final float LEVEL_WHITE = 1.0f;

	public static final int POINT_SIZE = 2;

	public static final int CHANNEL_RED = 0;
	
	public static final int CHANNEL_GREEN = 1;
	
	public static final int CHANNEL_BLUE = 2;

	public TonemapCurve(float[] red, float[] green, float[] blue) {

		checkNotNull(red, "red must not be null");
		checkNotNull(green, "green must not be null");
		checkNotNull(blue, "blue must not be null");

		checkArgumentArrayLengthDivisibleBy(red, POINT_SIZE, "red");
		checkArgumentArrayLengthDivisibleBy(green, POINT_SIZE, "green");
		checkArgumentArrayLengthDivisibleBy(blue, POINT_SIZE, "blue");

		checkArgumentArrayLengthNoLessThan(red, MIN_CURVE_LENGTH, "red");
		checkArgumentArrayLengthNoLessThan(green, MIN_CURVE_LENGTH, "green");
		checkArgumentArrayLengthNoLessThan(blue, MIN_CURVE_LENGTH, "blue");

		checkArrayElementsInRange(red, LEVEL_BLACK, LEVEL_WHITE, "red");
		checkArrayElementsInRange(green, LEVEL_BLACK, LEVEL_WHITE, "green");
		checkArrayElementsInRange(blue, LEVEL_BLACK, LEVEL_WHITE, "blue");

		mRed = Arrays.copyOf(red, red.length);
		mGreen = Arrays.copyOf(green, green.length);
		mBlue = Arrays.copyOf(blue, blue.length);
	}

	private static void checkArgumentArrayLengthDivisibleBy(float[] array,
			int divisible, String arrayName) {
		if (array.length % divisible != 0) {
			throw new IllegalArgumentException(arrayName + " size must be divisible by "
					+ divisible);
		}
	}

	private static int checkArgumentColorChannel(int colorChannel) {
		switch (colorChannel) {
			case CHANNEL_RED:
			case CHANNEL_GREEN:
			case CHANNEL_BLUE:
				break;
			default:
				throw new IllegalArgumentException("colorChannel out of range");
		}

		return colorChannel;
	}

	private static void checkArgumentArrayLengthNoLessThan(float[] array, int minLength,
			String arrayName) {
		if (array.length < minLength) {
			throw new IllegalArgumentException(arrayName + " size must be at least "
					+ minLength);
		}
	}

	public int getPointCount(int colorChannel) {
		checkArgumentColorChannel(colorChannel);

		return getCurve(colorChannel).length / POINT_SIZE;
	}

	public PointF getPoint(int colorChannel, int index) {
		checkArgumentColorChannel(colorChannel);
		if (index < 0 || index >= getPointCount(colorChannel)) {
			throw new IllegalArgumentException("index out of range");
		}

		final float[] curve = getCurve(colorChannel);

		final float pIn = curve[index * POINT_SIZE + OFFSET_POINT_IN];
		final float pOut = curve[index * POINT_SIZE + OFFSET_POINT_OUT];

		return new PointF(pIn, pOut);
	}

	public void copyColorCurve(int colorChannel, float[] destination,
			int offset) {
		checkArgumentNonnegative(offset, "offset must not be negative");
		checkNotNull(destination, "destination must not be null");

		if (destination.length + offset < getPointCount(colorChannel) * POINT_SIZE) {
			throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");
		}

		float[] curve = getCurve(colorChannel);
		System.arraycopy(curve, 0, destination, offset, curve.length);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj instanceof TonemapCurve) {
			final TonemapCurve other = (TonemapCurve) obj;
			return Arrays.equals(mRed, other.mRed) &&
					Arrays.equals(mGreen, other.mGreen) &&
					Arrays.equals(mBlue, other.mBlue);
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (mHashCalculated) {
			return mHashCode;
		}

		mHashCode = HashCodeHelpers.hashCodeGeneric(mRed, mGreen, mBlue);
		mHashCalculated = true;

		return mHashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("TonemapCurve{");
		sb.append("R:");
		sb.append(curveToString(CHANNEL_RED));
		sb.append(", G:");
		sb.append(curveToString(CHANNEL_GREEN));
		sb.append(", B:");
		sb.append(curveToString(CHANNEL_BLUE));
		sb.append("}");
		return sb.toString();
	}

	private String curveToString(int colorChannel) {
		checkArgumentColorChannel(colorChannel);
		StringBuilder sb = new StringBuilder("[");
		float[] curve = getCurve(colorChannel);
		int pointCount = curve.length / POINT_SIZE;
		for (int i = 0, j = 0; i < pointCount; i++, j += 2) {
			sb.append("(");
			sb.append(curve[j]);
			sb.append(", ");
			sb.append(curve[j+1]);
			sb.append("), ");
		}
		sb.setLength(sb.length() - 2);
		sb.append("]");
		return sb.toString();
	}

	private float[] getCurve(int colorChannel) {
		switch (colorChannel) {
			case CHANNEL_RED:
				return mRed;
			case CHANNEL_GREEN:
				return mGreen;
			case CHANNEL_BLUE:
				return mBlue;
			default:
				throw new AssertionError("colorChannel out of range");
		}
	}

	private final static int OFFSET_POINT_IN = 0;
	private final static int OFFSET_POINT_OUT = 1;
	private final static int TONEMAP_MIN_CURVE_POINTS = 2;
	private final static int MIN_CURVE_LENGTH = TONEMAP_MIN_CURVE_POINTS * POINT_SIZE;

	private final float[] mRed;
	private final float[] mGreen;
	private final float[] mBlue;

	private int mHashCode;
	private boolean mHashCalculated = false;
}
