package android.graphics;

import android.atl.GskCanvas;
import android.atl.annotation.Export;
import android.content.res.Resources;
import android.util.Log;

public class Canvas {

	public static enum EdgeType {
		BW,
		AA;
	}
	public static final int HAS_ALPHA_LAYER_SAVE_FLAG = (1 << 2);
	public static final int CLIP_SAVE_FLAG = 0x02;

	private Bitmap bitmap;
	private GskCanvas gsk_canvas;
	private int density = Bitmap.DENSITY_NONE;

	public Canvas() {
		if (!(this instanceof GskCanvas)) {
			gsk_canvas = new GskCanvas(0);
			gsk_canvas.offscreen = true;
		}
	}

	public Canvas(Bitmap bmp) {
		this.bitmap = bmp;
		gsk_canvas = new GskCanvas(bmp.getSnapshot());
		gsk_canvas.offscreen = true;
		density = bmp.getDensity();
	}

	public int getDensity() {
		return density;
	}

	public void setDensity(int density) {
		this.density = density;
	}

	/* AOSP's deprecated save(int); the GSK canvas always saves the matrix together with the clip */
	public int save(int saveFlags) {
		return save();
	}

	public int save() {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			return gsk_canvas.save();
		}
	}
	public void restore() {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.restore();
		}
	}

	public int getSaveCount() {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			return gsk_canvas.getSaveCount();
		}
	}

	// ---

	/**
	 * Draw the specified Rect using the specified paint. The rectangle will
	 * be filled or framed based on the Style in the paint.
	 *
	 * @param rect  The rect to be drawn
	 * @param paint The paint used to draw the rect
	 */
	public void drawRect(RectF r, Paint paint) {
		drawRect(r.left, r.top, r.right, r.bottom, paint);
	}

	/**
	 * Draw the specified Rect using the specified Paint. The rectangle
	 * will be filled or framed based on the Style in the paint.
	 *
	 * @param r        The rectangle to be drawn.
	 * @param paint    The paint used to draw the rectangle
	 */
	public void drawRect(Rect r, Paint paint) {
		drawRect(r.left, r.top, r.right, r.bottom, paint);
	}

	/**
	 * Draw the specified Rect using the specified paint. The rectangle will
	 * be filled or framed based on the Style in the paint.
	 *
	 * @param left   The left side of the rectangle to be drawn
	 * @param top    The top side of the rectangle to be drawn
	 * @param right  The right side of the rectangle to be drawn
	 * @param bottom The bottom side of the rectangle to be drawn
	 * @param paint  The paint used to draw the rect
	 */
	public void drawRect(float left, float top, float right, float bottom, Paint paint) {
		synchronized (bitmap) {
			/* without GSK composite nodes (GTK < 4.22) CLEAR erases the whole bitmap */
			if (!GskCanvas.canComposite() && paint != null && paint.getXfermode() instanceof PorterDuffXfermode && ((PorterDuffXfermode)paint.getXfermode()).porterDuffMode == PorterDuff.Mode.CLEAR.nativeInt) {
				int oldSaveCount = gsk_canvas.getSaveCount();
				gsk_canvas.restoreToCount(1);
				bitmap.eraseColor(0);
				gsk_canvas.snapshot = bitmap.getSnapshot();
				while (gsk_canvas.getSaveCount() < oldSaveCount)
					gsk_canvas.save();
				return;
			}
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.drawRect(left, top, right, bottom, paint);
		}
	}

	// ---
	/**
	 * Preconcat the current matrix with the specified rotation.
	 *
	 * @param degrees The amount to rotate, in degrees
	 */
	public void rotate(float degrees) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.rotate(degrees);
		}
	}

	/**
	 * Preconcat the current matrix with the specified rotation.
	 *
	 * @param degrees The amount to rotate, in degrees
	 * @param px The x-coord for the pivot point (unchanged by the rotation)
	 * @param py The y-coord for the pivot point (unchanged by the rotation)
	 */
	public void rotate(float degrees, float px, float py) {
		translate(px, py);
		rotate(degrees);
		translate(-px, -py);
	}
	// ---
	/**
	 * Draw the text, with origin at (x,y), using the specified paint. The
	 * origin is interpreted based on the Align setting in the paint.
	 *
	 * @param text  The text to be drawn
	 * @param x     The x-coordinate of the origin of the text being drawn
	 * @param y     The y-coordinate of the origin of the text being drawn
	 * @param paint The paint used for the text (e.g. color, size, style)
	 */
	public void drawText(String text, float x, float y, Paint paint) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.drawText(text, x, y, paint);
		}
	}

	/**
	 * Draw the text, with origin at (x,y), using the specified paint.
	 * The origin is interpreted based on the Align setting in the paint.
	 *
	 * @param text  The text to be drawn
	 * @param start The index of the first character in text to draw
	 * @param end   (end - 1) is the index of the last character in text to draw
	 * @param x     The x-coordinate of the origin of the text being drawn
	 * @param y     The y-coordinate of the origin of the text being drawn
	 * @param paint The paint used for the text (e.g. color, size, style)
	 */
	public void drawText(String text, int start, int end, float x, float y, Paint paint) {
		drawText(text.substring(start, end), x, y, paint);
	}

	/**
	 * Draw the specified range of text, specified by start/end, with its
	 * origin at (x,y), in the specified Paint. The origin is interpreted
	 * based on the Align setting in the Paint.
	 *
	 * @param text     The text to be drawn
	 * @param start    The index of the first character in text to draw
	 * @param end      (end - 1) is the index of the last character in text
	 *                 to draw
	 * @param x        The x-coordinate of origin for where to draw the text
	 * @param y        The y-coordinate of origin for where to draw the text
	 * @param paint The paint used for the text (e.g. color, size, style)
	 */
	@Export
	public void drawText(CharSequence text, int start, int end, float x, float y, Paint paint) {
		drawText(text.toString().substring(start, end), x, y, paint);
		/*if (text instanceof String || text instanceof SpannedString
		      || text instanceof SpannableString) {
		    native_drawText(mNativeCanvas, text.toString(), start, end, x, y,
			    paint.mBidiFlags, paint.mNativePaint);
		} else if (text instanceof GraphicsOperations) {
		    ((GraphicsOperations) text).drawText(this, start, end, x, y,
			    paint);
		} else {
		    char[] buf = TemporaryBuffer.obtain(end - start);
		    TextUtils.getChars(text, start, end, buf, 0);
		    native_drawText(mNativeCanvas, buf, 0, end - start, x, y,
			    paint.mBidiFlags, paint.mNativePaint);
		    TemporaryBuffer.recycle(buf);
		}*/
	}

	public void drawText(char text[], int start, int end, float x, float y, Paint paint) {
		drawText(new String(text), start, end, x, y, paint);
	}

	/**
	 * Draw the specified NinePatch into the canvas at dst, with the color filter of the paint (the pieces
	 * that stretch fill the space, the others keep their size).
	 */
	public void drawPatch(NinePatch patch, Rect dst, Paint paint) {
		drawPatchImpl(patch, dst, paint);
	}

	public void drawPatch(NinePatch patch, RectF dst, Paint paint) {
		drawPatchImpl(patch, new Rect(Math.round(dst.left), Math.round(dst.top), Math.round(dst.right), Math.round(dst.bottom)), paint);
	}

	private void drawPatchImpl(NinePatch patch, Rect dst, Paint paint) {
		android.graphics.drawable.NinePatchDrawable d = patch.getDrawable();
		d.setBounds(dst);
		d.setColorFilter(paint != null ? paint.getColorFilter() : null);
		d.draw(this);
	}

	/** The checks of AOSP BaseCanvas.drawGlyphs() */
	protected static void checkGlyphs(int[] glyphIds, int glyphIdOffset, float[] positions, int positionOffset, int glyphCount, android.graphics.fonts.Font font, Paint paint) {
		java.util.Objects.requireNonNull(glyphIds, "glyphIds must not be null.");
		java.util.Objects.requireNonNull(positions, "positions must not be null.");
		java.util.Objects.requireNonNull(font, "font must not be null.");
		java.util.Objects.requireNonNull(paint, "paint must not be null.");
		if (glyphCount < 0)
			throw new IllegalArgumentException("glyphCount must not be negative");
		if (glyphIdOffset < 0 || glyphIdOffset + glyphCount > glyphIds.length)
			throw new IndexOutOfBoundsException("glyphIds must have at least " + (glyphIdOffset + glyphCount) + " of elements");
		if (positionOffset < 0 || positionOffset + glyphCount * 2 > positions.length)
			throw new IndexOutOfBoundsException("positions must have at least " + (positionOffset + glyphCount * 2) + " of elements");
	}

	/**
	 * Draw glyphs of the font by glyph id, each at its x, y in positions (Android 12). The typeface of the paint is
	 * ignored, size and color come from the paint.
	 */
	public void drawGlyphs(int[] glyphIds, int glyphIdOffset, float[] positions, int positionOffset, int glyphCount, android.graphics.fonts.Font font, Paint paint) {
		checkGlyphs(glyphIds, glyphIdOffset, positions, positionOffset, glyphCount, font, paint);
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.drawGlyphs(glyphIds, glyphIdOffset, positions, positionOffset, glyphCount, font, paint);
		}
	}

	/**
	 * Draw the text, with origin at (x,y), along the specified path: each glyph is put where the path is at the
	 * middle of its advance, turned by the tangent there (as Skia's text on a path); glyphs beyond the ends of the
	 * path are not drawn. hOffset moves along the path, vOffset away from it.
	 */
	public void drawTextOnPath(String text, Path path, float hOffset, float vOffset, Paint paint) {
		PathMeasure measure = new PathMeasure(path, false);
		float length = measure.getLength();
		float[] pos = new float[2];
		float[] tan = new float[2];
		float h = hOffset;
		for (int i = 0; i < text.length();) {
			int count = Character.charCount(text.codePointAt(i));
			String glyph = text.substring(i, i + count);
			float width = paint.measureText(glyph);
			float middle = h + width / 2;
			if (middle >= 0 && middle <= length && measure.getPosTan(middle, pos, tan)) {
				save();
				translate(pos[0], pos[1]);
				rotate((float)Math.toDegrees(Math.atan2(tan[1], tan[0])));
				drawText(glyph, -width / 2, vOffset, paint);
				restore();
			}
			h += width;
			i += count;
		}
	}

	public void drawTextOnPath(char[] text, int index, int count, Path path, float hOffset, float vOffset, Paint paint) {
		if ((index | count) < 0 || index + count > text.length)
			throw new ArrayIndexOutOfBoundsException();
		drawTextOnPath(new String(text, index, count), path, hOffset, vOffset, paint);
	}

	/** Draw each character at its own position: pos is x, y per character (AOSP: deprecated drawPosText()) */
	@Deprecated
	public void drawPosText(char[] text, int index, int count, float[] pos, Paint paint) {
		if (index < 0 || index + count > text.length || count * 2 > pos.length)
			throw new IndexOutOfBoundsException();
		for (int i = 0; i < count; i++)
			drawText(text, index + i, 1, pos[i * 2], pos[i * 2 + 1], paint);
	}

	@Deprecated
	public void drawPosText(String text, float[] pos, Paint paint) {
		drawPosText(text.toCharArray(), 0, text.length(), pos, paint);
	}

	/**
	 * Draw a run of text, all in one direction. The context is what shapes the run's ends (Pango takes it from the
	 * text itself), the direction comes from the text, isRtl is not applied.
	 */
	public void drawTextRun(CharSequence text, int start, int end, int contextStart, int contextEnd, float x, float y, boolean isRtl, Paint paint) {
		if (text == null)
			throw new NullPointerException("text is null");
		if (paint == null)
			throw new NullPointerException("paint is null");
		if ((start | end | (end - start) | (text.length() - end) | contextStart | contextEnd | (start - contextStart) | (contextEnd - end) | (text.length() - contextEnd)) < 0)
			throw new IndexOutOfBoundsException();
		drawText(text, start, end, x, y, paint);
	}

	/** Draw a run of the measured text: the characters of the range, with the paint */
	public void drawTextRun(android.graphics.text.MeasuredText text, int start, int end, int contextStart, int contextEnd, float x, float y, boolean isRtl, Paint paint) {
		if (text == null)
			throw new NullPointerException("text is null");
		if (paint == null)
			throw new NullPointerException("paint is null");
		char[] chars = text.getChars();
		if ((start | end | (end - start) | (chars.length - end) | contextStart | contextEnd | (start - contextStart) | (contextEnd - end) | (chars.length - contextEnd)) < 0)
			throw new IndexOutOfBoundsException();
		drawText(chars, start, end - start, x, y, paint);
	}

	public void drawTextRun(char[] text, int index, int count, int contextIndex, int contextCount, float x, float y, boolean isRtl, Paint paint) {
		if (text == null)
			throw new NullPointerException("text is null");
		if (paint == null)
			throw new NullPointerException("paint is null");
		if ((index | count | text.length - index - count | contextIndex | contextCount | index - contextIndex | (contextIndex + contextCount) - (index + count) | text.length - (contextIndex + contextCount)) < 0)
			throw new IndexOutOfBoundsException();
		drawText(text, index, count, x, y, paint);
	}

	// ---
	/**
	 * <p>Draw the specified arc, which will be scaled to fit inside the
	 * specified oval.</p>
	 *
	 * <p>If the start angle is negative or >= 360, the start angle is treated
	 * as start angle modulo 360.</p>
	 *
	 * <p>If the sweep angle is >= 360, then the oval is drawn
	 * completely. Note that this differs slightly from SkPath::arcTo, which
	 * treats the sweep angle modulo 360. If the sweep angle is negative,
	 * the sweep angle is treated as sweep angle modulo 360</p>
	 *
	 * <p>The arc is drawn clockwise. An angle of 0 degrees correspond to the
	 * geometric angle of 0 degrees (3 o'clock on a watch.)</p>
	 *
	 * @param oval       The bounds of oval used to define the shape and size
	 *                   of the arc
	 * @param startAngle Starting angle (in degrees) where the arc begins
	 * @param sweepAngle Sweep angle (in degrees) measured clockwise
	 * @param useCenter If true, include the center of the oval in the arc, and
			    close it if it is being stroked. This will draw a wedge
	 * @param paint      The paint used to draw the arc
	 */
	public void drawArc(RectF oval, float startAngle, float sweepAngle, boolean useCenter,
	                    Paint paint) {
		if (oval == null) {
			throw new NullPointerException();
		}
		drawArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, useCenter, paint);
	}
	// ---
	/**
	 * Preconcat the current matrix with the specified scale.
	 *
	 * @param sx The amount to scale in X
	 * @param sy The amount to scale in Y
	 */
	public /*native*/ void scale(float sx, float sy) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.scale(sx, sy);
		}
	}

	/**
	 * Preconcat the current matrix with the specified scale.
	 *
	 * @param sx The amount to scale in X
	 * @param sy The amount to scale in Y
	 * @param px The x-coord for the pivot point (unchanged by the scale)
	 * @param py The y-coord for the pivot point (unchanged by the scale)
	 */
	public final void scale(float sx, float sy, float px, float py) {
		translate(px, py);
		scale(sx, sy);
		translate(-px, -py);
	}
	// ---
	/**
	 * Draw the specified bitmap, with its top/left corner at (x,y), using
	 * the specified paint, transformed by the current matrix.
	 *
	 * <p>Note: if the paint contains a maskfilter that generates a mask which
	 * extends beyond the bitmap's original width/height (e.g. BlurMaskFilter),
	 * then the bitmap will be drawn as if it were in a Shader with CLAMP mode.
	 * Thus the color outside of the original width/height will be the edge
	 * color replicated.
	 *
	 * <p>If the bitmap and canvas have different densities, this function
	 * will take care of automatically scaling the bitmap to draw at the
	 * same density as the canvas.
	 *
	 * @param bitmap The bitmap to be drawn
	 * @param left   The position of the left side of the bitmap being drawn
	 * @param top    The position of the top side of the bitmap being drawn
	 * @param paint  The paint used to draw the bitmap (may be null)
	 */
	public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
		Rect dst = new Rect((int)left, (int)top, (int)left + bitmap.getScaledWidth(this), (int)top + bitmap.getScaledHeight(this));
		drawBitmap(bitmap, null, dst, paint);
	}

	/**
	 * Draw the specified bitmap, scaling/translating automatically to fill
	 * the destination rectangle. If the source rectangle is not null, it
	 * specifies the subset of the bitmap to draw.
	 *
	 * <p>Note: if the paint contains a maskfilter that generates a mask which
	 * extends beyond the bitmap's original width/height (e.g. BlurMaskFilter),
	 * then the bitmap will be drawn as if it were in a Shader with CLAMP mode.
	 * Thus the color outside of the original width/height will be the edge
	 * color replicated.
	 *
	 * <p>This function <em>ignores the density associated with the bitmap</em>.
	 * This is because the source and destination rectangle coordinate
	 * spaces are in their respective densities, so must already have the
	 * appropriate scaling factor applied.
	 *
	 * @param bitmap The bitmap to be drawn
	 * @param src    May be null. The subset of the bitmap to be drawn
	 * @param dst    The rectangle that the bitmap will be scaled/translated
	 *               to fit into
	 * @param paint  May be null. The paint used to draw the bitmap
	 */
	public void drawBitmap(Bitmap bitmap, Rect src, RectF dst, Paint paint) {
		drawBitmap(bitmap, src, new Rect((int)dst.left, (int)dst.top, (int)dst.right, (int)dst.bottom), paint);
	}

	/**
	 * Draw the specified bitmap, scaling/translating automatically to fill
	 * the destination rectangle. If the source rectangle is not null, it
	 * specifies the subset of the bitmap to draw.
	 *
	 * <p>Note: if the paint contains a maskfilter that generates a mask which
	 * extends beyond the bitmap's original width/height (e.g. BlurMaskFilter),
	 * then the bitmap will be drawn as if it were in a Shader with CLAMP mode.
	 * Thus the color outside of the original width/height will be the edge
	 * color replicated.
	 *
	 * <p>This function <em>ignores the density associated with the bitmap</em>.
	 * This is because the source and destination rectangle coordinate
	 * spaces are in their respective densities, so must already have the
	 * appropriate scaling factor applied.
	 *
	 * @param bitmap The bitmap to be drawn
	 * @param src    May be null. The subset of the bitmap to be drawn
	 * @param dst    The rectangle that the bitmap will be scaled/translated
	 *               to fit into
	 * @param paint  May be null. The paint used to draw the bitmap
	 */
	public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, Paint paint) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = this.bitmap.getSnapshot();
			gsk_canvas.drawBitmap(bitmap, src, dst, paint);
		}
	}

	/**
	 * Treat the specified array of colors as a bitmap, and draw it. This gives
	 * the same result as first creating a bitmap from the array, and then
	 * drawing it, but this method avoids explicitly creating a bitmap object
	 * which can be more efficient if the colors are changing often.
	 *
	 * @param colors Array of colors representing the pixels of the bitmap
	 * @param offset Offset into the array of colors for the first pixel
	 * @param stride The number of colors in the array between rows (must be
	 *               >= width or <= -width).
	 * @param x The X coordinate for where to draw the bitmap
	 * @param y The Y coordinate for where to draw the bitmap
	 * @param width The width of the bitmap
	 * @param height The height of the bitmap
	 * @param hasAlpha True if the alpha channel of the colors contains valid
	 *                 values. If false, the alpha byte is ignored (assumed to
	 *                 be 0xFF for every pixel).
	 * @param paint  May be null. The paint used to draw the bitmap
	 */
	public void drawBitmap(int[] colors, int offset, int stride, float x, float y,
	                       int width, int height, boolean hasAlpha, Paint paint) {
		/* AOSP BaseCanvas.drawBitmap(int[], ...) with the checks; the native part (CanvasJNI DrawBitmapArray in
		 * libs/hwui/jni/android_graphics_Canvas.cpp: an N32 bitmap from the colors, opaque without hasAlpha,
		 * drawn at x, y) as a temporary Bitmap. The rows are copied here, which also covers a negative stride */
		if (width < 0) {
			throw new IllegalArgumentException("width must be >= 0");
		}
		if (height < 0) {
			throw new IllegalArgumentException("height must be >= 0");
		}
		if (Math.abs(stride) < width) {
			throw new IllegalArgumentException("abs(stride) must be >= width");
		}
		int lastScanline = offset + (height - 1) * stride;
		int length = colors.length;
		if (offset < 0 || (offset + width > length) || lastScanline < 0
		    || (lastScanline + width > length)) {
			throw new ArrayIndexOutOfBoundsException();
		}
		// quick escape if there's nothing to draw
		if (width == 0 || height == 0) {
			return;
		}
		int[] packed = new int[width * height];
		for (int row = 0; row < height; row++) {
			System.arraycopy(colors, offset + row * stride, packed, row * width, width);
		}
		if (!hasAlpha) {
			for (int i = 0; i < packed.length; i++)
				packed[i] |= 0xFF000000;
		}
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setHasAlpha(hasAlpha);
		bitmap.setPixels(packed, 0, width, 0, 0, width, height);
		drawBitmap(bitmap, x, y, paint);
	}

	/**
	 * Legacy version of drawBitmap(int[] colors, ...) that took ints for x,y
	 */
	public void drawBitmap(int[] colors, int offset, int stride, int x, int y,
	                       int width, int height, boolean hasAlpha, Paint paint) {
		// call through to the common float version
		drawBitmap(colors, offset, stride, (float)x, (float)y, width, height,
		           hasAlpha, paint);
	}

	/**
	 * Draw the bitmap using the specified matrix.
	 *
	 * @param bitmap The bitmap to draw
	 * @param matrix The matrix used to transform the bitmap when it is drawn
	 * @param paint  May be null. The paint used to draw the bitmap
	 */
	public void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint) {
		save();
		concat(matrix);
		drawBitmap(bitmap, 0, 0, paint);
		restore();
	}
	// ---
	/**
	 * Draw a line segment with the specified start and stop x,y coordinates,
	 * using the specified paint.
	 *
	 * <p>Note that since a line is always "framed", the Style is ignored in the paint.</p>
	 *
	 * <p>Degenerate lines (length is 0) will not be drawn.</p>
	 *
	 * @param startX The x-coordinate of the start point of the line
	 * @param startY The y-coordinate of the start point of the line
	 * @param paint  The paint used to draw the line
	 */
	public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.drawLine(startX, startY, stopX, stopY, paint);
		}
	}

	public void drawLines(float[] points, Paint paint) {
		drawLines(points, 0, points.length, paint);
	}

	public void drawLines(float[] points, int offset, int count, Paint paint) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.drawLines(points, offset, count, paint);
		}
	}

	public void setBitmap(Bitmap bitmap) {
		if (bitmap != null && !bitmap.isMutable()) {
			throw new IllegalStateException("Bitmap must be mutable");
		}
		this.bitmap = bitmap;
		gsk_canvas.snapshot = bitmap == null ? 0 : bitmap.getSnapshot();
	}

	public void drawPath(Path path, Paint paint) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.drawPath(path, paint);
		}
	}

	public void restoreToCount(int count) {
		if (count < 1)
			throw new IllegalArgumentException("count must be >= 1");
		while (getSaveCount() > count)
			restore();
	}

	public void drawRoundRect(float left, float top, float right, float bottom, float rx, float ry, Paint paint) {
		synchronized (bitmap) {
			if (paint.getShader() instanceof BitmapShader) {
				BitmapShader shader = (BitmapShader)paint.getShader();
				Matrix localMatrix = new Matrix();
				if (shader.getLocalMatrix(localMatrix)) {
					// the shader maps the bitmap with its local matrix (AOSP Shader.setLocalMatrix)
					int saveCount = save();
					concat(localMatrix);
					drawBitmap(shader.bitmap, 0, 0, paint);
					restoreToCount(saveCount);
				} else {
					drawBitmap(shader.bitmap, 0, 0, paint);
				}
			} else if (!GskCanvas.canComposite() && paint.getXfermode() instanceof PorterDuffXfermode && ((PorterDuffXfermode)paint.getXfermode()).porterDuffMode == PorterDuff.Mode.CLEAR.nativeInt) {
				int oldSaveCount = gsk_canvas.getSaveCount();
				gsk_canvas.restoreToCount(1);
				bitmap.eraseColor(0);
				gsk_canvas.snapshot = bitmap.getSnapshot();
				while (gsk_canvas.getSaveCount() < oldSaveCount)
					gsk_canvas.save();
			} else {
				gsk_canvas.snapshot = bitmap.getSnapshot();
				gsk_canvas.drawRoundRect(left, top, right, bottom, rx, ry, paint);
			}
		}
	}

	public void drawRoundRect(RectF rect, float rx, float ry, Paint paint) {
		drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, paint);
	}

	public void getMatrix(Matrix matrix) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.getMatrix(matrix);
		}
	}

	public Matrix getMatrix() {
		Matrix matrix = new Matrix();
		getMatrix(matrix);
		return matrix;
	}

	public void translate(float dx, float dy) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.translate(dx, dy);
		}
	}

	public void drawCircle(float cx, float cy, float radius, Paint paint) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.drawCircle(cx, cy, radius, paint);
		}
	}

	public void drawOval(float left, float top, float right, float bottom, Paint paint) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.drawOval(left, top, right, bottom, paint);
		}
	}

	public Rect getClipBounds() {
		Rect rect = new Rect();
		getClipBounds(rect);
		return rect;
	}

	public void concat(Matrix matrix) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			gsk_canvas.concat(matrix);
		}
	}

	/* AOSP concat(Matrix44) preconcats the 4x4 matrix; the 2D canvas maps x, y through the rows/columns of x, y and w
	 * (the z row and column only matter for 3D drawing) */
	public void concat(Matrix44 m) {
		if (m == null)
			return;
		Matrix matrix = new Matrix();
		matrix.setValues(new float[] {m.get(0, 0), m.get(0, 1), m.get(0, 3),
		                              m.get(1, 0), m.get(1, 1), m.get(1, 3),
		                              m.get(3, 0), m.get(3, 1), m.get(3, 3)});
		concat(matrix);
	}

	public void setMatrix(Matrix matrix) {
		Matrix transform = getMatrix();
		if (transform.isIdentity()) {
			transform = matrix;
		} else {
			getMatrix().invert(transform); // revert the current matrix
			transform.preConcat(matrix);   // apply the new matrix
		}
		concat(transform);
	}

	public int getWidth() {
		return (bitmap == null) ? 0 : bitmap.getWidth();
	}

	public int getHeight() {
		return (bitmap == null) ? 0 : bitmap.getHeight();
	}

	public void drawColor(int color) {
		Paint paint = new Paint();
		paint.setColor(color);
		drawPaint(paint);
	}

	public void drawARGB(int a, int r, int g, int b) {
		drawColor(Color.argb(a, r, g, b));
	}

	public static final int MATRIX_SAVE_FLAG = 0x01;
	public static final int FULL_COLOR_LAYER_SAVE_FLAG = 0x08;
	public static final int CLIP_TO_LAYER_SAVE_FLAG = 0x10;
	public static final int ALL_SAVE_FLAG = 0x1F;

	/* AOSP: a layer of its own (GskCanvas.saveLayer()), also when drawing into a bitmap */
	public int saveLayer(RectF bounds, Paint paint, int flags) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			return gsk_canvas.saveLayer(bounds, paint, flags);
		}
	}

	public int saveLayer(RectF bounds, Paint paint) {
		return saveLayer(bounds, paint, ALL_SAVE_FLAG);
	}

	public int saveLayer(float left, float top, float right, float bottom, Paint paint) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			return gsk_canvas.saveLayer(left, top, right, bottom, paint);
		}
	}

	public int saveLayer(float left, float top, float right, float bottom, Paint paint, int flags) {
		return saveLayer(left, top, right, bottom, paint);
	}

	/* AOSP Canvas.saveLayerAlpha(): saveLayer() with a paint that only has the alpha */
	public int saveLayerAlpha(RectF bounds, int alpha, int saveFlags) {
		if (bounds == null)
			bounds = new RectF(getClipBounds());
		return saveLayerAlpha(bounds.left, bounds.top, bounds.right, bounds.bottom, alpha, saveFlags);
	}

	public int saveLayerAlpha(RectF bounds, int alpha) {
		return saveLayerAlpha(bounds, alpha, ALL_SAVE_FLAG);
	}

	public int saveLayerAlpha(float left, float top, float right, float bottom, int alpha, int saveFlags) {
		alpha = Math.min(255, Math.max(0, alpha));
		Paint paint = new Paint();
		paint.setAlpha(alpha);
		return saveLayer(left, top, right, bottom, paint, saveFlags);
	}

	public int saveLayerAlpha(float left, float top, float right, float bottom, int alpha) {
		return saveLayerAlpha(left, top, right, bottom, alpha, ALL_SAVE_FLAG);
	}

	public void drawOval(RectF oval, Paint paint) {
		drawRoundRect(oval, oval.width() / 2, oval.height() / 2, paint);
	}

	public void drawColor(int color, PorterDuff.Mode mode) {
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setXfermode(new PorterDuffXfermode(mode));
		drawPaint(paint);
	}

	public boolean clipRect(int left, int top, int right, int bottom) {
		return clipRect((float)left, top, right, bottom);
	}

	public boolean clipRect(float left, float top, float right, float bottom) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			return gsk_canvas.clipRect(left, top, right, bottom);
		}
	}

	public boolean clipRect(Rect rect) {
		return clipRect((float)rect.left, rect.top, rect.right, rect.bottom);
	}

	public boolean clipRect(Rect rect, Region.Op op) {
		return clipRect((float)rect.left, rect.top, rect.right, rect.bottom, op);
	}

	public boolean clipPath(Path path) {
		synchronized (bitmap) {
			gsk_canvas.snapshot = bitmap.getSnapshot();
			return gsk_canvas.clipPath(path);
		}
	}

	public boolean clipPath(Path path, Region.Op op) {
		if (op == Region.Op.DIFFERENCE)
			return clipOutPath(path);
		return clipPath(path);
	}

	/* AOSP clipOutPath() = DIFFERENCE: what the path does not fill, i.e. the path with the inverse bit of its
	 * fill type flipped (keeps WINDING/EVEN_ODD and an already inverse path) */
	public boolean clipOutPath(Path path) {
		Path outside = new Path(path);
		outside.toggleInverseFillType();
		return clipPath(outside);
	}

	public boolean isHardwareAccelerated() {
		return false;
	}

	public boolean clipRect(RectF rect) {
		return clipRect(rect.left, rect.top, rect.right, rect.bottom);
	}

	/* Region.Op.DIFFERENCE (AOSP clipOutRect(), e.g. AbsSeekBar.drawTrack() with splitTrack) has no
	 * GSK clip node: clip to an even-odd path of a very large rectangle with the rectangle cut out.
	 * The other ops still intersect, as before (AOSP only allows INTERSECT and DIFFERENCE since P). */
	private static final float CLIP_OUT_EXTENT = 1e6f;

	public boolean clipRect(float left, float top, float right, float bottom, Region.Op op) {
		if (op == Region.Op.DIFFERENCE) {
			Path path = new Path();
			path.setFillType(Path.FillType.EVEN_ODD);
			path.addRect(-CLIP_OUT_EXTENT, -CLIP_OUT_EXTENT, CLIP_OUT_EXTENT, CLIP_OUT_EXTENT, Path.Direction.CW);
			path.addRect(left, top, right, bottom, Path.Direction.CW);
			clipPath(path);
			return true;
		}
		return clipRect(left, top, right, bottom);
	}

	public boolean clipRect(RectF rect, Region.Op op) {
		return clipRect(rect.left, rect.top, rect.right, rect.bottom, op);
	}

	public boolean clipOutRect(RectF rect) {
		return clipRect(rect.left, rect.top, rect.right, rect.bottom, Region.Op.DIFFERENCE);
	}

	public boolean clipOutRect(Rect rect) {
		return clipRect((float)rect.left, rect.top, rect.right, rect.bottom, Region.Op.DIFFERENCE);
	}

	public boolean clipOutRect(float left, float top, float right, float bottom) {
		return clipRect(left, top, right, bottom, Region.Op.DIFFERENCE);
	}

	public boolean clipOutRect(int left, int top, int right, int bottom) {
		return clipRect((float)left, top, right, bottom, Region.Op.DIFFERENCE);
	}

	/* hwui SkiaCanvas::drawArc: a sweep of 360 degrees or more is the whole oval. Otherwise SkCanvas::drawArc and
	 * SkPathPriv::CreateDrawArcPath: nothing for an empty oval or no sweep, a wedge (center, arc, close) with useCenter. */
	public void drawArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean useCenter, Paint paint) {
		if (Math.abs(sweepAngle) >= 360.0f) {
			drawOval(left, top, right, bottom, paint);
			return;
		}
		if (!(left < right && top < bottom) || sweepAngle == 0)
			return;
		Path path = new Path();
		if (useCenter)
			path.moveTo((left + right) / 2, (top + bottom) / 2);
		path.arcTo(left, top, right, bottom, startAngle, sweepAngle, !useCenter);
		if (useCenter)
			path.close();
		drawPath(path, paint);
	}

	public boolean getClipBounds(Rect outRect) {
		/* UGLY HACK */
		outRect.set(0, 0, Resources.getSystem().getDisplayMetrics().widthPixels, Resources.getSystem().getDisplayMetrics().heightPixels);
		return true;
	}

	public void drawPaint(Paint paint) {
		// GskColorNode needs a fixed size. Draw a large rectangle to fill the whole viewport.
		// Values should not be near MAXINT to avoid overflow in the renderer.
		drawRect(-100000, -100000, 100000, 100000, paint);
	}

	public void drawPicture(Picture picture) {
		picture.draw(this);
	}

	/* AOSP: the picture scaled to fill dst */
	public void drawPicture(Picture picture, RectF dst) {
		save();
		translate(dst.left, dst.top);
		if (picture.getWidth() > 0 && picture.getHeight() > 0)
			scale(dst.width() / picture.getWidth(), dst.height() / picture.getHeight());
		drawPicture(picture);
		restore();
	}

	public void drawPicture(Picture picture, Rect dst) {
		drawPicture(picture, new RectF(dst));
	}

	/* a point is a square of the stroke width (at least one pixel) around x, y; round caps are
	 * not drawn round */
	public void drawPoint(float x, float y, Paint paint) {
		float half = Math.max(paint.getStrokeWidth(), 1) / 2;
		Paint fill = new Paint(paint);
		fill.setStyle(Paint.Style.FILL);
		drawRect(x - half, y - half, x + half, y + half, fill);
	}

	public void drawPoints(float[] pts, Paint paint) {
		drawPoints(pts, 0, pts.length, paint);
	}

	public void drawPoints(float[] pts, int offset, int count, Paint paint) {
		/* count is the number of values; two make one point */
		for (int i = offset; i + 2 <= offset + count; i += 2) {
			drawPoint(pts[i], pts[i + 1], paint);
		}
	}

	public boolean quickReject(float left, float top, float right, float bottom, EdgeType edgeType) {
		return false;
	}

	/* API 29/30 */

	/* there are no render nodes to reorder by elevation, so Z reordering has no effect */
	public void enableZ() {}

	/**
	 * Draws the given RenderNode. This is only supported in hardware rendering, which is
	 * {@link #isHardwareAccelerated()} returning true; otherwise an IllegalArgumentException
	 * will be thrown.
	 *
	 * @param renderNode The RenderNode to draw.
	 */
	public void drawRenderNode(RenderNode renderNode) {
		throw new IllegalArgumentException("Software rendering doesn't support drawRenderNode");
	}

	public void disableZ() {}

	public void drawColor(long color) {
		drawColor(Color.toArgb(color));
	}

	public void drawColor(int color, BlendMode mode) {
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setBlendMode(mode);
		drawPaint(paint);
	}

	public void drawColor(long color, BlendMode mode) {
		drawColor(Color.toArgb(color), mode);
	}

	public void drawDoubleRoundRect(RectF outer, float outerRx, float outerRy, RectF inner, float innerRx, float innerRy, Paint paint) {
		Path path = new Path();
		path.setFillType(Path.FillType.EVEN_ODD);
		path.addRoundRect(outer, outerRx, outerRy, Path.Direction.CW);
		path.addRoundRect(inner, innerRx, innerRy, Path.Direction.CW);
		drawPath(path, paint);
	}

	public void drawDoubleRoundRect(RectF outer, float[] outerRadii, RectF inner, float[] innerRadii, Paint paint) {
		Path path = new Path();
		path.setFillType(Path.FillType.EVEN_ODD);
		path.addRoundRect(outer, outerRadii, Path.Direction.CW);
		path.addRoundRect(inner, innerRadii, Path.Direction.CW);
		drawPath(path, paint);
	}

	/* never rejecting is always correct, it only skips an optimization */
	public boolean quickReject(float left, float top, float right, float bottom) {
		return false;
	}

	public boolean quickReject(RectF rect) {
		return false;
	}

	public boolean quickReject(RectF rect, EdgeType type) {
		return false;
	}

	public boolean quickReject(Path path, EdgeType type) {
		return false;
	}

	public boolean quickReject(Path path) {
		return false;
	}

	public void drawRGB(int r, int g, int b) {
		drawColor(Color.rgb(r, g, b));
	}

	/** Preconcat the current matrix with the specified skew. */
	public void skew(float sx, float sy) {
		Matrix m = new Matrix();
		m.setSkew(sx, sy);
		concat(m);
	}

	private DrawFilter mDrawFilter;

	/** The filter is kept; ATL does not apply its paint flags to what is drawn. */
	public void setDrawFilter(DrawFilter filter) {
		mDrawFilter = filter;
	}

	public DrawFilter getDrawFilter() {
		return mDrawFilter;
	}

	/* AOSP: the software canvas has no limit but the size of a bitmap (32766); a hardware canvas the size of a GPU texture */
	private static final int MAXIMUM_BITMAP_SIZE = 32766;

	public int getMaximumBitmapWidth() {
		return MAXIMUM_BITMAP_SIZE;
	}

	public int getMaximumBitmapHeight() {
		return MAXIMUM_BITMAP_SIZE;
	}

	/** Returns true if the canvas has an opaque bitmap, as AOSP's software canvas does */
	public boolean isOpaque() {
		return bitmap != null && !bitmap.hasAlpha();
	}

	public enum VertexMode {
		TRIANGLES(0),
		TRIANGLE_STRIP(1),
		TRIANGLE_FAN(2);

		VertexMode(int nativeInt) {
			this.nativeInt = nativeInt;
		}

		final int nativeInt;
	}

	/* the triangle of the source points s (three x,y pairs) drawn as the triangle of the points d with the bitmap */
	private void drawBitmapTriangle(Bitmap bitmap, float[] s, float[] d, Paint paint) {
		Matrix m = new Matrix();
		if (!m.setPolyToPoly(s, 0, d, 0, 3))
			return;
		save();
		Path triangle = new Path();
		triangle.moveTo(d[0], d[1]);
		triangle.lineTo(d[2], d[3]);
		triangle.lineTo(d[4], d[5]);
		triangle.close();
		clipPath(triangle);
		concat(m);
		drawBitmap(bitmap, 0, 0, paint);
		restore();
	}

	/**
	 * Draw the bitmap through a mesh with meshWidth+1 points across and meshHeight+1 down, verts is x, y per point.
	 * Each cell is two triangles, each mapped from the bitmap by the affine transformation of its three points;
	 * colors (per point) are not applied.
	 */
	public void drawBitmapMesh(Bitmap bitmap, int meshWidth, int meshHeight, float[] verts, int vertOffset, int[] colors, int colorOffset, Paint paint) {
		if ((meshWidth | meshHeight | vertOffset | colorOffset) < 0)
			throw new ArrayIndexOutOfBoundsException();
		if (meshWidth == 0 || meshHeight == 0)
			return;
		int count = (meshWidth + 1) * (meshHeight + 1);
		if (verts.length < vertOffset + count * 2)
			throw new ArrayIndexOutOfBoundsException();
		if (colors != null && colors.length < colorOffset + count)
			throw new ArrayIndexOutOfBoundsException();
		float w = bitmap.getWidth(), h = bitmap.getHeight();
		for (int y = 0; y < meshHeight; y++) {
			for (int x = 0; x < meshWidth; x++) {
				int[] corner = {y * (meshWidth + 1) + x, y * (meshWidth + 1) + x + 1, (y + 1) * (meshWidth + 1) + x, (y + 1) * (meshWidth + 1) + x + 1};
				float[][] src = new float[4][];
				float[][] dst = new float[4][];
				for (int c = 0; c < 4; c++) {
					src[c] = new float[] {w * ((c & 1) + x) / meshWidth, h * ((c >> 1) + y) / meshHeight};
					dst[c] = new float[] {verts[vertOffset + corner[c] * 2], verts[vertOffset + corner[c] * 2 + 1]};
				}
				drawBitmapTriangle(bitmap, new float[] {src[0][0], src[0][1], src[1][0], src[1][1], src[2][0], src[2][1]},
					new float[] {dst[0][0], dst[0][1], dst[1][0], dst[1][1], dst[2][0], dst[2][1]}, paint);
				drawBitmapTriangle(bitmap, new float[] {src[1][0], src[1][1], src[3][0], src[3][1], src[2][0], src[2][1]},
					new float[] {dst[1][0], dst[1][1], dst[3][0], dst[3][1], dst[2][0], dst[2][1]}, paint);
			}
		}
	}

	/**
	 * Draw the triangles of the vertices: with texs and a BitmapShader in the paint the bitmap is mapped onto
	 * each triangle by texs (in pixels of the bitmap); otherwise a triangle is filled with the paint, or with the
	 * average of the colors of its vertices if there are colors.
	 */
	public void drawVertices(VertexMode mode, int vertexCount, float[] verts, int vertOffset, float[] texs, int texOffset, int[] colors, int colorOffset, short[] indices, int indexOffset, int indexCount, Paint paint) {
		if ((vertexCount | vertOffset | texOffset | colorOffset | indexOffset | indexCount) < 0 || vertOffset + vertexCount > verts.length || (vertexCount & 1) != 0)
			throw new ArrayIndexOutOfBoundsException();
		int points = vertexCount / 2;
		/* the order of the points of the triangles */
		int[] order;
		if (indices != null) {
			order = new int[indexCount];
			for (int i = 0; i < indexCount; i++)
				order[i] = indices[indexOffset + i] & 0xFFFF;
		} else {
			int triangles = mode == VertexMode.TRIANGLES ? points / 3 : Math.max(points - 2, 0);
			order = new int[triangles * 3];
			for (int t = 0; t < triangles; t++) {
				order[t * 3] = mode == VertexMode.TRIANGLE_FAN ? 0 : mode == VertexMode.TRIANGLES ? t * 3 : t;
				order[t * 3 + 1] = mode == VertexMode.TRIANGLE_FAN ? t + 1 : mode == VertexMode.TRIANGLES ? t * 3 + 1 : t + 1;
				order[t * 3 + 2] = mode == VertexMode.TRIANGLE_FAN ? t + 2 : mode == VertexMode.TRIANGLES ? t * 3 + 2 : t + 2;
			}
		}
		/* an index list of TRIANGLE_STRIP / TRIANGLE_FAN is a strip/fan of the indexed points */
		if (indices != null && mode != VertexMode.TRIANGLES) {
			int triangles = Math.max(indexCount - 2, 0);
			int[] strip = new int[triangles * 3];
			for (int t = 0; t < triangles; t++) {
				strip[t * 3] = mode == VertexMode.TRIANGLE_FAN ? order[0] : order[t];
				strip[t * 3 + 1] = order[t + 1];
				strip[t * 3 + 2] = order[t + 2];
			}
			order = strip;
		}
		Bitmap bitmap = null;
		if (texs != null && paint != null && paint.getShader() instanceof BitmapShader)
			bitmap = ((BitmapShader)paint.getShader()).bitmap;
		for (int t = 0; t + 2 < order.length; t += 3) {
			float[] d = new float[6];
			float[] s = new float[6];
			int r = 0, g = 0, b = 0, a = 0;
			boolean valid = true;
			for (int c = 0; c < 3; c++) {
				int index = order[t + c];
				if (index >= points) {
					valid = false;
					break;
				}
				d[c * 2] = verts[vertOffset + index * 2];
				d[c * 2 + 1] = verts[vertOffset + index * 2 + 1];
				if (texs != null) {
					s[c * 2] = texs[texOffset + index * 2];
					s[c * 2 + 1] = texs[texOffset + index * 2 + 1];
				}
				if (colors != null) {
					int color = colors[colorOffset + index];
					a += color >>> 24;
					r += (color >> 16) & 0xFF;
					g += (color >> 8) & 0xFF;
					b += color & 0xFF;
				}
			}
			if (!valid)
				continue;
			if (bitmap != null) {
				drawBitmapTriangle(bitmap, s, d, null);
			} else {
				Path triangle = new Path();
				triangle.moveTo(d[0], d[1]);
				triangle.lineTo(d[2], d[3]);
				triangle.lineTo(d[4], d[5]);
				triangle.close();
				if (colors != null) {
					Paint fill = new Paint(paint);
					fill.setColor(Color.argb(a / 3, r / 3, g / 3, b / 3));
					drawPath(triangle, fill);
				} else {
					drawPath(triangle, paint);
				}
			}
		}
	}
}
