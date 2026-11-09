package android.atl;

import android.atl.annotation.Export;
import android.graphics.*;
import android.view.DisplayListCanvas;
import android.view.RenderNode;
import java.util.Arrays;
import java.util.Stack;

/**
 * GskCanvas:
 *   - implements Canvas for onscreen rendering inside GTKs snapshot function
 */
public class GskCanvas extends DisplayListCanvas {
	public long snapshot;
	private int[] push_history = null;
	private Stack<Matrix> state_stack = new Stack<>();

	private static Paint default_paint = new Paint();

	@Export
	public GskCanvas(long snapshot) {
		this.snapshot = snapshot;
		state_stack.push(new Matrix());
		/* the unit of a GTK snapshot is a logical pixel, which ATL gives the device density */
		setDensity(android.util.DisplayMetrics.DENSITY_DEVICE);
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
		state_stack.pop();
		native_restore(snapshot);
	}

	/* AOSP saveLayer(): what is drawn until restore() is composed as a group, clipped to the bounds, with the
	 * paint's alpha (ATL has no blend modes or color filters for a layer) */
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
		return count;
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
		native_clipPath(snapshot, path.getGskPath(), path.getFillType().fill_rule);
		int save_count = getSaveCount();
		if (push_history == null)
			push_history = new int[save_count + 1];
		else if (push_history.length <= save_count)
			push_history = Arrays.copyOf(push_history, save_count + 1);
		push_history[save_count]++;
		RectF bounds = new RectF();
		path.computeBounds(bounds, true);
		return !bounds.isEmpty();
	}

	@Override
	public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, Paint paint) {
		if (src == null)
			native_drawBitmap(snapshot, bitmap.getTexture(), dst.left, dst.top, dst.width(), dst.height(), paint != null ? paint.paint : default_paint.paint);
		else
			native_drawBitmap(snapshot, bitmap.getTexture(), dst.left, dst.top, dst.width(), dst.height(), src.left, src.top, src.width(), src.height(), paint != null ? paint.paint : default_paint.paint);
	}

	/**
	 * Fills the given area with the paint's shader. Returns false if the paint
	 * has no shader or the shader is not supported.
	 */
	private boolean fillWithShader(float left, float top, float right, float bottom, Paint paint) {
		if (paint != null && paint.getStyle() == Paint.Style.FILL && paint.getShader() instanceof BitmapShader) {
			/* the bitmap, mapped by the shader's local matrix (AOSP SkBitmapProcShader); the edges are not
			 * extended (TileMode.CLAMP) or repeated: what is outside the bitmap stays empty */
			BitmapShader shader = (BitmapShader)paint.getShader();
			if (shader.bitmap == null)
				return false;
			Matrix local = new Matrix();
			save();
			clipRect(left, top, right, bottom);
			if (shader.getLocalMatrix(local))
				concat(local);
			drawBitmap(shader.bitmap, null, new Rect(0, 0, shader.bitmap.getWidth(), shader.bitmap.getHeight()), paint);
			restore();
			return true;
		}
		if (paint == null || paint.getStyle() != Paint.Style.FILL || !(paint.getShader() instanceof LinearGradient))
			return false;
		LinearGradient gradient = (LinearGradient)paint.getShader();
		// TileMode.MIRROR is not supported by GSK and falls back to CLAMP
		native_drawLinearGradient(snapshot, left, top, right, bottom, gradient.x0, gradient.y0, gradient.x1, gradient.y1,
		                          gradient.colors, gradient.positions, gradient.tileMode == Shader.TileMode.REPEAT, paint.paint);
		return true;
	}

	@Override
	public void drawPath(Path path, Paint paint) {
		if (path != null && paint != null && paint.getShader() != null) {
			RectF bounds = new RectF();
			path.computeBounds(bounds, true);
			save();
			clipPath(path);
			boolean drawn = fillWithShader(bounds.left, bounds.top, bounds.right, bounds.bottom, paint);
			restore();
			if (drawn)
				return;
		}
		if (path != null)
			native_drawPath(snapshot, path.getGskPath(), path.getFillType().fill_rule, paint != null ? paint.paint : default_paint.paint);
	}

	@Override
	public void drawRect(float left, float top, float right, float bottom, Paint paint) {
		// TODO: implement xfermode using gtk_snapshot_push_composite in GTK >= 4.22
		if (paint != null && paint.getXfermode() != null && paint.getBlendMode() != BlendMode.SRC_OVER)
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
		native_drawText(snapshot, text, x, y, paint != null ? paint.paint : default_paint.paint);
	}

	@Override
	public void drawGlyphs(int[] glyphIds, int glyphIdOffset, float[] positions, int positionOffset, int glyphCount, android.graphics.fonts.Font font, Paint paint) {
		checkGlyphs(glyphIds, glyphIdOffset, positions, positionOffset, glyphCount, font, paint);
		native_drawGlyphs(snapshot, glyphIds, glyphIdOffset, positions, positionOffset, glyphCount, font.getFamilyName(), font.getStyle().getWeight(), font.getStyle().getSlant(), paint.paint);
	}

	@Override
	public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
		native_drawLine(snapshot, startX, startY, stopX, stopY, paint != null ? paint.paint : default_paint.paint);
	}

	@Override
	public void drawLines(float[] points, int offset, int count, Paint paint) {
		if (offset + count < 0 /* overflow */ || offset + count > points.length)
			throw new ArrayIndexOutOfBoundsException();
		native_drawLines(snapshot, points, offset, count, paint != null ? paint.paint : default_paint.paint);
	}

	@Override
	public void drawRoundRect(float left, float top, float right, float bottom, float rx, float ry, Paint paint) {
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
	protected native void native_drawPath(long snapshot, long path, int fill_rule, long paint);
	protected native void native_drawLinearGradient(long snapshot, float left, float top, float right, float bottom, float x0, float y0, float x1, float y1, int[] colors, float[] positions, boolean repeat, long paint);
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
	protected native void native_clipPath(long snapshot, long path, int fill_rule);
	protected native void native_pop(long snapshot, int pop_count);

	protected native void native_pushOpacity(long snapshot, float opacity);
	protected native void native_drawRenderNode(long snapshot, long render_node);
}
