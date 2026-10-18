package android.graphics;

public class LinearGradient extends Shader {

	public final float x0, y0, x1, y1;
	public final int[] colors;
	public final float[] positions;
	public final TileMode tileMode;

	public LinearGradient(float x0, float y0, float x1, float y1, int[] colors, float[] positions, TileMode mode) {
		if (colors.length < 2)
			throw new IllegalArgumentException("needs >= 2 number of colors");
		if (positions != null && colors.length != positions.length)
			throw new IllegalArgumentException("color and position arrays must be of equal length");
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		this.colors = colors.clone();
		this.positions = positions != null ? positions.clone() : null;
		this.tileMode = mode;
	}

	public LinearGradient(float x0, float y0, float x1, float y1, long[] colors, float[] positions, TileMode tile) {
		this(x0, y0, x1, y1, toArgb(colors), positions, tile);
	}

	public LinearGradient(float x0, float y0, float x1, float y1, int color0, int color1, TileMode tile) {
		this(x0, y0, x1, y1, new int[] {color0, color1}, null, tile);
	}

	/* only sRGB color longs are supported, their upper 32 bits are the ARGB color int */
	private static int[] toArgb(long[] colors) {
		int[] argb = new int[colors.length];
		for (int i = 0; i < colors.length; i++)
			argb[i] = (int)(colors[i] >>> 32);
		return argb;
	}
}
