package android.graphics;

public class RadialGradient extends Shader {

	public RadialGradient(float x, float y, float radius, int[] colors, float[] positions, android.graphics.Shader.TileMode tileMode) {}

	public RadialGradient(float x, float y, float radius, long[] colors, float[] positions, android.graphics.Shader.TileMode tileMode) {
		this(x, y, radius, toArgb(colors), positions, tileMode);
	}

	public RadialGradient(float centerX, float centerY, float radius, int centerColor, int edgeColor, android.graphics.Shader.TileMode tileMode) {
		this(centerX, centerY, radius, new int[] {centerColor, edgeColor}, null, tileMode);
	}

	public RadialGradient(float centerX, float centerY, float radius, long centerColor, long edgeColor, android.graphics.Shader.TileMode tileMode) {
		this(centerX, centerY, radius, new long[] {centerColor, edgeColor}, null, tileMode);
	}

	private static int[] toArgb(long[] colors) {
		int[] argb = new int[colors.length];
		for (int i = 0; i < colors.length; i++)
			argb[i] = Color.toArgb(colors[i]);
		return argb;
	}
}
