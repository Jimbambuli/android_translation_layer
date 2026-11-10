package android.graphics;

public class ColorMatrixColorFilter extends ColorFilter {
	private final ColorMatrix mMatrix;

	public ColorMatrixColorFilter(ColorMatrix matrix) {
		super();
		mMatrix = new ColorMatrix(matrix);
	}

	public ColorMatrixColorFilter(float[] array) {
		super();
		mMatrix = new ColorMatrix(array);
	}

	public void getColorMatrix(ColorMatrix colorMatrix) {
		colorMatrix.set(mMatrix);
	}
}
