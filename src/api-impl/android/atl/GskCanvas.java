package android.atl;

import android.atl.annotation.Export;
import android.graphics.*;
import android.view.DisplayListCanvas;
import android.view.RenderNode;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Stack;

/**
 * GskCanvas:
 *   - implements Canvas for onscreen rendering inside GTKs snapshot function
 */
public class GskCanvas extends DisplayListCanvas {
	public long snapshot;
	private int[] push_history = null;
	private Stack<Matrix> state_stack = new Stack<>();
	/* the save levels that saveLayer() opened as an isolated GSK layer, and how many are open */
	private BitSet layer_levels = new BitSet();
	private int layers = 0;
	protected boolean compositing = false;
	/* the snapshot is drawn into a bitmap (android.graphics.Canvas): it starts transparent, like a layer */
	public boolean offscreen = false;
	/* AOSP RecordingCanvas: the size of what is recorded (the view or render node), set before drawing */
	public int width = 0;
	public int height = 0;
	private static Boolean can_composite;

	/* GTK >= 4.22: Porter-Duff modes are drawn with GSK composite nodes */
	public static boolean canComposite() {
		if (can_composite == null)
			can_composite = native_canComposite();
		return can_composite;
	}

	private static Paint default_paint = new Paint();

	@Export
	public GskCanvas(long snapshot) {
		this.snapshot = snapshot;
		state_stack.push(new Matrix());
		/* the unit of a GTK snapshot is a logical pixel, which ATL gives the device density */
		setDensity(android.util.DisplayMetrics.DENSITY_DEVICE);
	}

	/* ATL_SNAPSHOT_CHECK set: finishRecording() reports the first draw of each class that left saves or clips open.
	 * Once is enough: apps leave them open on every draw (e.g. Material CollapsingTextHelper.draw() returns between
	 * save() and restoreToCount() without text), and AOSP closes them silently */
	private static final boolean check_balance = System.getenv("ATL_SNAPSHOT_CHECK") != null;
	private static final java.util.Set<String> check_reported = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

	/* AOSP RenderNode.endRecording(): SkiaRecordingCanvas::finishRecording() restores the canvas to save count 1,
	 * and hwui draws each display list in a save of its own, so what a view's draw left saved or clipped (also when
	 * an exception ended it) ends with its display list. Here the view draws into the GTK snapshot of its widget,
	 * which its parent and the window share: what it left open is closed when it has drawn. Otherwise the snapshot
	 * stays unbalanced and GTK keeps only the innermost node of the frame (gtk_snapshot_free_to_node(): "Too many
	 * gtk_snapshot_push() calls"), without the window's offset. Called when the view, drawable or render node
	 * that draws with this canvas has drawn. */
	@Export
	public void finishRecording(Object owner) {
		int saves = getSaveCount() - 1;
		int clips = (push_history != null && push_history.length > 1) ? push_history[1] : 0;
		if (saves == 0 && clips == 0)
			return;
		if (check_balance) {
			String name = owner != null ? owner.getClass().getName() : "?";
			if (check_reported.add(name))
				android.util.Log.w("GskCanvas", "ATL_SNAPSHOT_CHECK: " + name + " left " + saves + " saves and " + clips + " clips of the base level open (reported once per class)");
		}
		while (getSaveCount() > 1)
			restore();
		if (clips > 0) {
			native_pop(snapshot, clips);
			push_history[1] = 0;
		}
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public int save() {
		native_save(snapshot);
		state_stack.push(new Matrix(state_stack.peek()));
		return getSaveCount() - 1;
	}

	@Override
	public void restore() {
		int save_count = getSaveCount();
		if (save_count <= 1)
			throw new IllegalStateException("No more saves to restore");
		if (push_history != null && push_history.length > save_count && push_history[save_count] > 0) {
			native_pop(snapshot, push_history[save_count]);
			push_history[save_count] = 0;
		}
		if (layer_levels.get(save_count)) {
			layer_levels.clear(save_count);
			layers--;
		}
		state_stack.pop();
		native_restore(snapshot);
	}

	/* AOSP saveLayer(): what is drawn until restore() is composed as a group, clipped to the bounds, with the
	 * paint's alpha (ATL has no blend modes or color filters for a layer). Like the offscreen of AOSP, the group
	 * starts transparent (GSK isolation, GTK >= 4.22), so a Porter-Duff mode inside sees only the layer */
	private int saveLayerOf(float left, float top, float right, float bottom, boolean clip, Paint paint) {
		int count = save();
		if (clip)
			clipRect(left, top, right, bottom);
		int alpha = paint != null ? paint.getAlpha() : 255;
		if (alpha != 255) {
			native_pushOpacity(snapshot, alpha / 255f);
			int save_count = getSaveCount();
			if (push_history == null)
				push_history = new int[save_count + 1];
			else if (push_history.length <= save_count)
				push_history = Arrays.copyOf(push_history, save_count + 1);
			push_history[save_count]++;
		}
		if (native_pushIsolation(snapshot)) {
			int save_count = getSaveCount();
			if (push_history == null)
				push_history = new int[save_count + 1];
			else if (push_history.length <= save_count)
				push_history = Arrays.copyOf(push_history, save_count + 1);
			push_history[save_count]++;
			layer_levels.set(save_count);
			layers++;
		}
		return count;
	}

	protected interface Drawing {
		void draw(Paint paint);
	}

	/* the composite operation of the paint's mode for native_pushComposite(), -1 if the mode is drawn without one:
	 * here the GskPorterDuff of a Porter-Duff mode, -1 for SRC_OVER, no mode or a separable blend mode */
	protected int compositeOp(Paint paint) {
		BlendMode mode = paint != null ? paint.getBlendMode() : null;
		if (mode == null)
			return -1;
		switch (mode) {
			case SRC: return 0;
			case DST: return 1;
			case DST_OVER: return 3;
			case SRC_IN: return 4;
			case DST_IN: return 5;
			case SRC_OUT: return 6;
			case DST_OUT: return 7;
			case SRC_ATOP: return 8;
			case DST_ATOP: return 9;
			case XOR: return 10;
			case CLEAR: return 11;
			default: return -1;
		}
	}

	/* AOSP Paint.setXfermode(): the Porter-Duff mode combines the drawing with what the layer holds, and like
	 * Skia only where the drawing covers (for a bitmap its destination rectangle). GSK composite node: mask =
	 * the coverage, drawn opaque, child = the drawing. Only inside saveLayer() or a bitmap; elsewhere the GSK
	 * background is the whole window, not the surface of the view, and the drawing stays SRC_OVER as before.
	 * The separable blend modes (MULTIPLY, SCREEN, ...) stay SRC_OVER as well. */
	protected boolean drawComposited(Paint paint, Drawing coverage, Drawing drawing) {
		int op;
		if (compositing || (layers == 0 && !offscreen) || (op = compositeOp(paint)) < 0 || !native_pushComposite(snapshot, op))
			return false;
		Paint coverage_paint = new Paint(paint);
		coverage_paint.setShader(null);
		coverage_paint.setColorFilter(null);
		coverage_paint.setXfermode(null);
		coverage_paint.setColor(Color.WHITE);
		compositing = true;
		try {
			coverage.draw(coverage_paint);
			native_pop(snapshot, 1);
			drawing.draw(paint);
			native_pop(snapshot, 1);
		} finally {
			compositing = false;
		}
		return true;
	}

	@Override
	public int saveLayer(RectF bounds, Paint paint, int flags) {
		return bounds != null ? saveLayerOf(bounds.left, bounds.top, bounds.right, bounds.bottom, true, paint) : saveLayerOf(0, 0, 0, 0, false, paint);
	}

	@Override
	public int saveLayer(RectF bounds, Paint paint) {
		return saveLayer(bounds, paint, ALL_SAVE_FLAG);
	}

	@Override
	public int saveLayer(float left, float top, float right, float bottom, Paint paint) {
		return saveLayerOf(left, top, right, bottom, true, paint);
	}

	@Override
	public int getSaveCount() {
		return state_stack.size();
	}

	@Override
	public void translate(float dx, float dy) {
		native_translate(snapshot, dx, dy);
		state_stack.peek().preTranslate(dx, dy);
	}

	@Override
	public void rotate(float degrees) {
		native_rotate(snapshot, degrees);
		state_stack.peek().preRotate(degrees);
	}

	@Override
	public void scale(float sx, float sy) {
		native_scale(snapshot, sx, sy);
		state_stack.peek().preScale(sx, sy);
	}

	@Override
	public void concat(Matrix matrix) {
		if (matrix != null) {
			native_concat(snapshot, matrix.native_instance);
			state_stack.peek().preConcat(matrix);
		}
	}

	@Override
	public void getMatrix(Matrix matrix) {
		matrix.set(state_stack.peek());
	}

	@Override
	public boolean clipRect(float left, float top, float right, float bottom) {
		native_clipRect(snapshot, left, top, right, bottom);
		int save_count = getSaveCount();
		if (push_history == null)
			push_history = new int[save_count + 1];
		else if (push_history.length <= save_count)
			push_history = Arrays.copyOf(push_history, save_count + 1);
		push_history[save_count]++;
		return right > left && bottom > top;
	}

	@Override
	public boolean clipPath(Path path) {
		native_clipPath(snapshot, path.getGskPath(), path.getFillType().fill_rule, path.isInverseFillType());
		int save_count = getSaveCount();
		if (push_history == null)
			push_history = new int[save_count + 1];
		else if (push_history.length <= save_count)
			push_history = Arrays.copyOf(push_history, save_count + 1);
		push_history[save_count]++;
		RectF bounds = new RectF();
		path.computeBounds(bounds, true);
		return path.isInverseFillType() || !bounds.isEmpty();
	}

	@Override
	public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, Paint paint) {
		/* AOSP BaseCanvas.throwIfCannotDraw() (its recycled check is left out: ATL's own code may still draw
		 * bitmaps it has recycled internally) */
		if (!bitmap.isPremultiplied() && bitmap.getConfig() == Bitmap.Config.ARGB_8888 && bitmap.hasAlpha())
			throw new RuntimeException("Canvas: trying to use a non-premultiplied bitmap " + bitmap);
		if (drawComposited(paint, p -> native_drawRect(snapshot, dst.left, dst.top, dst.right, dst.bottom, p.paint), p -> drawBitmap(bitmap, src, dst, p)))
			return;
		/* under the bitmap's lock: another thread may change its pixels meanwhile (see Bitmap) */
		synchronized (bitmap) {
			if (src == null)
				native_drawBitmap(snapshot, imageOf(bitmap), dst.left, dst.top, dst.width(), dst.height(), paint != null ? paint.paint : default_paint.paint);
			else
				native_drawBitmap(snapshot, imageOf(bitmap), dst.left, dst.top, dst.width(), dst.height(), src.left, src.top, src.width(), src.height(), paint != null ? paint.paint : default_paint.paint);
		}
	}

	/**
	 * Fills the given area with the paint's shader. Returns false if the paint
	 * has no shader or the shader is not supported.
	 */
	private boolean fillWithShader(float left, float top, float right, float bottom, Paint paint) {
		if (paint != null && paint.getStyle() == Paint.Style.FILL && paint.getShader() instanceof BitmapShader) {
			/* the bitmap, mapped by the shader's local matrix (AOSP SkBitmapProcShader); outside the bitmap
			 * each axis follows its TileMode. The area is mapped back into the shader's coordinates */
			BitmapShader shader = (BitmapShader)paint.getShader();
			if (shader.bitmap == null)
				return false;
			Matrix local = new Matrix();
			RectF area = new RectF(left, top, right, bottom);
			save();
			clipRect(left, top, right, bottom);
			if (shader.getLocalMatrix(local)) {
				Matrix inverse = new Matrix();
				if (!local.invert(inverse)) {
					restore();
					return true;
				}
				inverse.mapRect(area);
				concat(local);
			}
			synchronized (shader.bitmap) {
				native_fillBitmapShader(snapshot, imageOf(shader.bitmap), area.left, area.top, area.right, area.bottom,
				                        tileModeInt(shader.tileX), tileModeInt(shader.tileY), paint.paint);
			}
			restore();
			return true;
		}
		if (paint == null || paint.getStyle() != Paint.Style.FILL || !(paint.getShader() instanceof LinearGradient))
			return false;
		LinearGradient gradient = (LinearGradient)paint.getShader();
		/* AOSP Shader.setLocalMatrix(): the gradient is defined in the coordinates the local matrix maps
		 * to the canvas; the area is mapped back into them. A local matrix that cannot be inverted draws nothing */
		Matrix local = new Matrix();
		boolean has_local = gradient.getLocalMatrix(local);
		RectF area = new RectF(left, top, right, bottom);
		if (has_local) {
			Matrix inverse = new Matrix();
			if (!local.invert(inverse))
				return true;
			inverse.mapRect(area);
			save();
			concat(local);
		}
		native_drawLinearGradient(snapshot, area.left, area.top, area.right, area.bottom, gradient.x0, gradient.y0, gradient.x1, gradient.y1,
		                          gradient.colors, gradient.positions, tileModeInt(gradient.tileMode), paint.paint);
		if (has_local)
			restore();
		return true;
	}

	/* the image the native drawing draws a bitmap from: here its GdkTexture */
	protected long imageOf(Bitmap bitmap) {
		return bitmap.getTexture();
	}

	/* whether the native drawing applies the paint's blend mode itself (else a mode that is not composited is
	 * left out of drawRect()) */
	protected boolean drawsBlendMode() {
		return false;
	}

	/* AOSP Shader.TileMode.nativeInt */
	private static int tileModeInt(Shader.TileMode mode) {
		switch (mode) {
			case REPEAT:
				return 1;
			case MIRROR:
				return 2;
			case DECAL:
				return 3;
			default:
				return 0;
		}
	}

	@Override
	public void drawPath(Path path, Paint paint) {
		if (path != null && drawComposited(paint, p -> drawPath(path, p), p -> drawPath(path, p)))
			return;
		if (path != null && paint != null && paint.getShader() != null) {
			RectF bounds = new RectF();
			path.computeBounds(bounds, true);
			/* an inverse fill covers everything outside the path (same extent as Canvas.drawPaint()) */
			if (path.isInverseFillType())
				bounds.set(-100000, -100000, 100000, 100000);
			save();
			clipPath(path);
			boolean drawn = fillWithShader(bounds.left, bounds.top, bounds.right, bounds.bottom, paint);
			restore();
			if (drawn)
				return;
		}
		if (path != null)
			native_drawPath(snapshot, path.getGskPath(), path.getFillType().fill_rule, path.isInverseFillType(), paint != null ? paint.paint : default_paint.paint);
	}

	@Override
	public void drawRect(float left, float top, float right, float bottom, Paint paint) {
		if (drawComposited(paint, p -> drawRect(left, top, right, bottom, p), p -> drawRect(left, top, right, bottom, p)))
			return;
		/* without a layer (or GTK < 4.22) a Porter-Duff mode is left out, see drawComposited() */
		if (!compositing && !drawsBlendMode() && paint != null && paint.getXfermode() != null && paint.getBlendMode() != BlendMode.SRC_OVER)
			return;
		if (fillWithShader(left, top, right, bottom, paint))
			return;
		/* the native rectangle is filled; an outline is a path */
		if (paint != null && paint.getStyle() != Paint.Style.FILL) {
			Path path = new Path();
			path.addRect(left, top, right, bottom, Path.Direction.CW);
			drawPath(path, paint);
			return;
		}
		native_drawRect(snapshot, left, top, right, bottom, paint != null ? paint.paint : default_paint.paint);
	}

	@Override
	public void drawText(String text, float x, float y, Paint paint) {
		if (text == null) {
			new Exception("drawText: text is null; stack trace:").printStackTrace();
			return;
		}
		if (drawComposited(paint, p -> drawText(text, x, y, p), p -> drawText(text, x, y, p)))
			return;
		native_drawText(snapshot, text, x, y, paint != null ? paint.paint : default_paint.paint);
	}

	@Override
	public void drawGlyphs(int[] glyphIds, int glyphIdOffset, float[] positions, int positionOffset, int glyphCount, android.graphics.fonts.Font font, Paint paint) {
		checkGlyphs(glyphIds, glyphIdOffset, positions, positionOffset, glyphCount, font, paint);
		if (drawComposited(paint, p -> drawGlyphs(glyphIds, glyphIdOffset, positions, positionOffset, glyphCount, font, p), p -> drawGlyphs(glyphIds, glyphIdOffset, positions, positionOffset, glyphCount, font, p)))
			return;
		native_drawGlyphs(snapshot, glyphIds, glyphIdOffset, positions, positionOffset, glyphCount, font.getFamilyName(), font.getStyle().getWeight(), font.getStyle().getSlant(), paint.paint);
	}

	@Override
	public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
		if (drawComposited(paint, p -> drawLine(startX, startY, stopX, stopY, p), p -> drawLine(startX, startY, stopX, stopY, p)))
			return;
		native_drawLine(snapshot, startX, startY, stopX, stopY, paint != null ? paint.paint : default_paint.paint);
	}

	@Override
	public void drawLines(float[] points, int offset, int count, Paint paint) {
		if (offset + count < 0 /* overflow */ || offset + count > points.length)
			throw new ArrayIndexOutOfBoundsException();
		if (drawComposited(paint, p -> drawLines(points, offset, count, p), p -> drawLines(points, offset, count, p)))
			return;
		native_drawLines(snapshot, points, offset, count, paint != null ? paint.paint : default_paint.paint);
	}

	@Override
	public void drawRoundRect(float left, float top, float right, float bottom, float rx, float ry, Paint paint) {
		if (drawComposited(paint, p -> drawRoundRect(left, top, right, bottom, rx, ry, p), p -> drawRoundRect(left, top, right, bottom, rx, ry, p)))
			return;
		/* the native rounded rectangle has no dashed outline */
		if (paint != null && (paint.getShader() != null || (paint.getPathEffect() instanceof DashPathEffect && paint.getStyle() != Paint.Style.FILL))) {
			Path path = new Path();
			path.addRoundRect(left, top, right, bottom, rx, ry, Path.Direction.CW);
			drawPath(path, paint);
			return;
		}
		native_drawRoundRect(snapshot, left, top, right, bottom, rx, ry, paint != null ? paint.paint : default_paint.paint);
	}

	@Override
	public void drawCircle(float cx, float cy, float radius, Paint paint) {
		drawRoundRect(cx - radius, cy - radius, cx + radius, cy + radius, radius, radius, paint);
	}

	@Override
	public void drawOval(float left, float top, float right, float bottom, Paint paint) {
		drawRoundRect(left, top, right, bottom, (right - left) / 2, (bottom - top) / 2, paint);
	}

	@Override
	public void drawRenderNode(RenderNode node) {
		drawRenderNodeOfView(node);
	}

	@Override
	public void drawRenderNode(android.graphics.RenderNode node) {
		drawRenderNodeOfView(node);
	}

	/* The canvas of a view: hwui renders a display list after the view has drawn, so a render node that is
	 * recorded again later in the draw (Compose updates its dirty layers after drawing the root) shows in the
	 * same frame. For a view that draws render nodes the WrapperWidget sets defer_render_nodes: the nodes are
	 * stubs then, which deferred_children replaces when the view has drawn. */
	public boolean defer_render_nodes;
	public RenderNode deferred_children;
	public boolean drew_render_node;

	private void drawRenderNodeOfView(RenderNode node) {
		drew_render_node = true;
		if (defer_render_nodes) {
			if (deferred_children == null)
				deferred_children = new RenderNode();
			deferred_children.recordDeferredChild(snapshot, node);
		} else
			native_drawRenderNode(snapshot, node.getGskNode());
	}

	/* Stretch of the view this canvas records, set by EdgeEffect.draw(). In AOSP this is the
	 * StretchEffect of the view's RenderNode (RenderNode.stretch(), hwui StretchEffect::mergeWith());
	 * the WrapperWidget clears it before View.draw() and applies it to what the view drew. */
	public float stretch_x;
	public float stretch_y;
	private float max_stretch_x;
	private float max_stretch_y;

	/* hwui StretchEffect::NON_ZERO_EPSILON */
	private static boolean isZero(float value) {
		return Math.abs(value) <= 0.00004f;
	}

	/* AOSP RenderNode.stretch() with hwui StretchEffect::mergeWith() */
	public boolean stretch(float vecX, float vecY, float maxStretchAmountX, float maxStretchAmountY) {
		if (Float.isInfinite(vecX) || Float.isNaN(vecX)) {
			throw new IllegalArgumentException("vecX must be a finite, non-NaN value " + vecX);
		}
		if (Float.isInfinite(vecY) || Float.isNaN(vecY)) {
			throw new IllegalArgumentException("vecY must be a finite, non-NaN value " + vecY);
		}
		if (maxStretchAmountX <= 0.0f) {
			throw new IllegalArgumentException(
					"The max horizontal stretch amount must be >0, got " + maxStretchAmountX);
		}
		if (maxStretchAmountY <= 0.0f) {
			throw new IllegalArgumentException(
					"The max vertical stretch amount must be >0, got " + maxStretchAmountY);
		}
		if (isZero(vecX) && isZero(vecY))
			return true;
		if (isZero(stretch_x) && isZero(stretch_y)) {
			stretch_x = vecX;
			stretch_y = vecY;
			max_stretch_x = maxStretchAmountX;
			max_stretch_y = maxStretchAmountY;
			return true;
		}
		stretch_x += vecX;
		stretch_y += vecY;
		if (isZero(stretch_x) && isZero(stretch_y)) {
			stretch_x = stretch_y = max_stretch_x = max_stretch_y = 0;
			return true;
		}
		max_stretch_x = Math.max(max_stretch_x, maxStretchAmountX);
		max_stretch_y = Math.max(max_stretch_y, maxStretchAmountY);
		return true;
	}

	protected native void native_drawBitmap(long snapshot, long texture, int x, int y, int width, int height, long paint);
	protected native void native_drawBitmap(long snapshot, long texture, int x, int y, int width, int height, int src_x, int src_y, int src_width, int src_height, long paint);
	protected native void native_drawRect(long snapshot, float left, float top, float right, float bottom, long paint);
	protected native void native_drawPath(long snapshot, long path, int fill_rule, boolean inverse, long paint);
	protected native void native_fillBitmapShader(long snapshot, long texture, float left, float top, float right, float bottom, int tile_x, int tile_y, long paint);
	protected native void native_drawLinearGradient(long snapshot, float left, float top, float right, float bottom, float x0, float y0, float x1, float y1, int[] colors, float[] positions, int tile_mode, long paint);
	protected native void native_translate(long snapshot, float dx, float dy);
	protected native void native_rotate(long snapshot, float degrees);
	protected native void native_save(long snapshot);
	protected native void native_restore(long snapshot);
	protected native void native_drawLine(long snapshot, float startX, float startY, float stopX, float stopY, long paint);
	protected native void native_drawLines(long snapshot, float[] points, int offset, int count, long paint);
	protected native void native_drawText(long snapshot, String text, float x, float y, long paint);
	protected native void native_drawGlyphs(long snapshot, int[] glyphIds, int glyphIdOffset, float[] positions, int positionOffset, int glyphCount, String family, int weight, int slant, long paint);
	protected native void native_drawRoundRect(long snapshot, float left, float top, float right, float bottom, float rx, float ry, long paint);
	protected native void native_scale(long snapshot, float sx, float sy);
	protected native void native_concat(long snapshot, long matrix);
	protected native void native_clipRect(long snapshot, float left, float top, float right, float bottom);
	protected native void native_clipPath(long snapshot, long path, int fill_rule, boolean inverse);
	protected native void native_pop(long snapshot, int pop_count);

	protected native void native_pushOpacity(long snapshot, float opacity);
	protected native boolean native_pushIsolation(long snapshot);
	private static native boolean native_canComposite();
	protected native boolean native_pushComposite(long snapshot, int op);
	protected native void native_drawRenderNode(long snapshot, long render_node);
}
