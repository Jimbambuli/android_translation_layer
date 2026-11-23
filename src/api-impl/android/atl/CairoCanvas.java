package android.atl;

import android.graphics.BlendMode;
import android.graphics.Bitmap;
import android.graphics.Paint;

/**
 * CairoCanvas:
 *   - the canvas of a Bitmap: draws with cairo into the bitmap's surface (Bitmap.getSurface()), in any thread,
 *     like Android's software canvas. GskCanvas does the drawing logic; the native methods draw with cairo.
 */
public class CairoCanvas extends GskCanvas {

	public CairoCanvas(long surface) {
		super(native_create(surface));
		offscreen = true;
	}

	@Override
	protected long imageOf(Bitmap bitmap) {
		return bitmap.getSurface();
	}

	@Override
	protected boolean drawsBlendMode() {
		return true;
	}

	/* the modes cairo has no operator for that blends like Skia only where the drawing covers (see
	 * android_atl_CairoCanvas.c): composited by native_pop(), with the BlendMode ordinal as op */
	@Override
	protected int compositeOp(Paint paint) {
		BlendMode mode = paint != null ? paint.getBlendMode() : null;
		if (mode == null)
			return -1;
		switch (mode) {
			case CLEAR:
			case SRC:
			case SRC_IN:
			case DST_IN:
			case SRC_OUT:
			case DST_ATOP:
			case MODULATE:
				return mode.ordinal();
			default:
				return -1;
		}
	}

	/* every draw passes here first: the native canvas takes the paint's blend mode (as a cairo operator; inside a
	 * composite the drawing is plain SRC_OVER), antialiasing and bitmap filtering (hwui Paint::sampling(); a null
	 * paint is Skia's default SkPaint: neither) */
	@Override
	protected boolean drawComposited(Paint paint, Drawing coverage, Drawing drawing) {
		BlendMode mode = paint != null && !compositing ? paint.getBlendMode() : null;
		native_setPaint(snapshot, mode != null ? mode.ordinal() : -1, paint != null && paint.isAntiAlias(), paint != null && paint.isFilterBitmap());
		return super.drawComposited(paint, coverage, drawing);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void finalize() throws Throwable {
		try {
			if (snapshot != 0)
				native_destroy(snapshot);
			snapshot = 0;
		} finally {
			super.finalize();
		}
	}

	private static native long native_create(long surface);
	private static native void native_destroy(long canvas);
	private static native void native_setPaint(long canvas, int mode, boolean antialias, boolean filter);

	@Override protected native void native_drawBitmap(long canvas, long surface, int x, int y, int width, int height, long paint);
	@Override protected native void native_drawBitmap(long canvas, long surface, int x, int y, int width, int height, int src_x, int src_y, int src_width, int src_height, long paint);
	@Override protected native void native_drawRect(long canvas, float left, float top, float right, float bottom, long paint);
	@Override protected native void native_drawPath(long canvas, long path, int fill_rule, boolean inverse, long paint);
	@Override protected native void native_fillBitmapShader(long canvas, long surface, float left, float top, float right, float bottom, int tile_x, int tile_y, long paint);
	@Override protected native void native_drawLinearGradient(long canvas, float left, float top, float right, float bottom, float x0, float y0, float x1, float y1, int[] colors, float[] positions, int tile_mode, long paint);
	@Override protected native void native_translate(long canvas, float dx, float dy);
	@Override protected native void native_rotate(long canvas, float degrees);
	@Override protected native void native_save(long canvas);
	@Override protected native void native_restore(long canvas);
	@Override protected native void native_drawLine(long canvas, float startX, float startY, float stopX, float stopY, long paint);
	@Override protected native void native_drawLines(long canvas, float[] points, int offset, int count, long paint);
	@Override protected native void native_drawText(long canvas, String text, float x, float y, long paint);
	@Override protected native void native_drawGlyphs(long canvas, int[] glyphIds, int glyphIdOffset, float[] positions, int positionOffset, int glyphCount, String family, int weight, int slant, long paint);
	@Override protected native void native_drawRoundRect(long canvas, float left, float top, float right, float bottom, float rx, float ry, long paint);
	@Override protected native void native_scale(long canvas, float sx, float sy);
	@Override protected native void native_concat(long canvas, long matrix);
	@Override protected native void native_clipRect(long canvas, float left, float top, float right, float bottom);
	@Override protected native void native_clipPath(long canvas, long path, int fill_rule, boolean inverse);
	@Override protected native void native_pop(long canvas, int pop_count);
	@Override protected native void native_pushOpacity(long canvas, float opacity);
	@Override protected native boolean native_pushIsolation(long canvas);
	@Override protected native boolean native_pushComposite(long canvas, int op);
	@Override protected native void native_drawRenderNode(long canvas, long render_node);
}
