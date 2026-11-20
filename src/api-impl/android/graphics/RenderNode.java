package android.graphics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The public RenderNode of API 29, which Compose's GraphicsLayerV29 records each layer into.
 *
 * ATL's display list is the GSK render node of android.view.RenderNode, which this class extends.
 * As in hwui, the node is drawn at its position (left, top) with the property transforms, clipped
 * to its bounds and to a rectangular or rounded outline, with its alpha, its blur and offset render
 * effects and the elevation shadow of its outline (light over the node, see createShadowNode()).
 * Not drawn: the 3D rotations and camera distance, the other render effects, the compositing layer
 * paint and the animation matrix; their values are kept and returned.
 */
public final class RenderNode extends android.view.RenderNode {
	private static final AtomicLong sNextUniqueId = new AtomicLong(1);

	private final long uniqueId = sNextUniqueId.getAndIncrement();
	private int left, top, right, bottom;
	private boolean pivotExplicitlySet;
	private boolean clipToBounds = true;
	private boolean clipToOutline;
	private Outline outline;
	private final Rect outlineRect = new Rect();
	private Rect clipRect;
	private float translationZ;
	private int ambientShadowColor = Color.BLACK;
	private int spotShadowColor = Color.BLACK;
	private boolean useCompositingLayer;
	private Paint layerPaint;
	private boolean overlappingRendering = true;
	private RenderEffect renderEffect;
	private Matrix animationMatrix;
	private boolean forceDarkAllowed = true;
	private boolean projectBackwards;
	private boolean projectionReceiver;
	private RecordingCanvas recordingCanvas;
	private boolean hasDisplayList;

	/**
	 * Creates a new RenderNode that can be used to record batches of
	 * drawing operations, and store / apply render properties when drawn.
	 *
	 * @param name The name of the RenderNode, used for debugging purpose. May be null.
	 */
	public RenderNode(String name) {
	}

	/**
	 * Starts recording a display list for the render node. All
	 * operations performed on the returned canvas are recorded and
	 * stored in this display list.
	 *
	 * @param width  The width of the recording viewport
	 * @param height The height of the recording viewport
	 * @return A canvas to record drawing operations.
	 * @throws IllegalStateException If a recording is already in progress.
	 */
	public RecordingCanvas beginRecording(int width, int height) {
		if (recordingCanvas != null) {
			throw new IllegalStateException(
			    "Recording currently in progress - missing #endRecording() call?");
		}
		prepareRecording(width, height);
		recordingCanvas = new RecordingCanvas(this, createRecordingSnapshot());
		recordingCanvas.width = width;
		recordingCanvas.height = height;
		return recordingCanvas;
	}

	/**
	 * Same as {@link #beginRecording(int, int)} with the width & height set
	 * to the RenderNode's own width & height.
	 */
	public RecordingCanvas beginRecording() {
		return beginRecording(right - left, bottom - top);
	}

	/**
	 * Ends the recording for this display list. Calling this method marks
	 * the display list valid and {@link #hasDisplayList()} will return true.
	 */
	public void endRecording() {
		if (recordingCanvas == null) {
			throw new IllegalStateException(
			    "No recording in progress, forgot to call #beginRecording()?");
		}
		end(recordingCanvas);
		recordingCanvas = null;
		hasDisplayList = true;
	}

	/* RecordingCanvas: a node drawn while recording becomes a child */
	void addChild(long snapshot, android.view.RenderNode child) {
		recordChild(snapshot, child);
	}

	/**
	 * Reset native resources. This is called when cleaning up the state of display lists
	 * during destruction of hardware resources, to ensure that we do not hold onto
	 * obsolete resources after related resources are gone.
	 */
	@Override
	public void discardDisplayList() {
		super.discardDisplayList();
		hasDisplayList = false;
	}

	/**
	 * Returns whether the RenderNode has a display list. If this returns false, the RenderNode
	 * should be re-recorded with {@link #beginRecording()} and {@link #endRecording()}.
	 */
	public boolean hasDisplayList() {
		return hasDisplayList;
	}

	/* the property transforms, with hwui's order: the render effect, the clip in local coordinates (a
	 * rounded outline clips round), the elevation shadow outside of it, then translate by the position and
	 * translation and transform around the pivot */
	@Override
	protected long applyProperties(long node) {
		long effected = 0;
		if (renderEffect != null) {
			float[] effect = new float[3];
			renderEffect.accumulate(effect);
			effected = nativeApplyEffect(node, (float)Math.sqrt(effect[0]), effect[1], effect[2]);
			node = effected;
		}
		boolean clipToRect = false;
		Rect clip = null;
		float clipRadius = 0;
		if (clipToOutline && outline != null && outline.getRect(outlineRect)) {
			clip = outlineRect;
			clipRadius = Math.max(outline.getRadius(), 0);
		}
		if (clipRect != null) {
			clip = clip == null ? clipRect : new Rect(clip);
			if (clip != clipRect && !clip.intersect(clipRect))
				clip.setEmpty();
			if (clip != outlineRect)
				clipRadius = 0;
		}
		long shadow = createShadowNode();
		long transformed = nativeApplyProperties(node, right - left, bottom - top, clipToBounds,
		                                         clip != null, clip != null ? clip.left : 0, clip != null ? clip.top : 0,
		                                         clip != null ? clip.right : 0, clip != null ? clip.bottom : 0, clipRadius, shadow,
		                                         getScaleX(), getScaleY(), left + getTranslationX(), top + getTranslationY(),
		                                         getRotationZ(), getPivotX(), getPivotY(), getAlpha());
		if (shadow != 0)
			nativeUnref(shadow);
		if (effected != 0)
			nativeUnref(effected);
		return transformed;
	}

	/* hwui's LightingInfo with the light source of the Material theme (light_z, light_radius,
	 * ambient_shadow_alpha and spot_shadow_alpha of the framework); a render node knows no window */
	private static final float LIGHT_Z_DP = 500;
	private static final float LIGHT_RADIUS_DP = 800;
	private static final float AMBIENT_SHADOW_ALPHA = 0.039f;
	private static final float SPOT_SHADOW_ALPHA = 0.19f;

	/* the shadow of the outline at the node's Z, as hwui draws it for a node with elevation (0 for none) */
	private long createShadowNode() {
		float z = getElevation() + translationZ;
		if (z <= 0 || outline == null || outline.isEmpty() || outline.getAlpha() <= 0)
			return 0;
		RectF bounds = new RectF();
		float radius = 0;
		if (outline.getRect(outlineRect)) {
			bounds.set(outlineRect);
			radius = Math.max(outline.getRadius(), 0);
		} else if (outline.mPath != null) { // convex path: its bounds
			outline.mPath.computeBounds(bounds, true);
		} else {
			return 0;
		}
		float casterAlpha = getAlpha() * outline.getAlpha();
		float density = android.content.res.Resources.getSystem().getDisplayMetrics().density;
		return nativeCreateShadowNode(bounds.left, bounds.top, bounds.right, bounds.bottom, radius, z,
		                              shadowColor(ambientShadowColor, AMBIENT_SHADOW_ALPHA * casterAlpha),
		                              shadowColor(spotShadowColor, SPOT_SHADOW_ALPHA * casterAlpha),
		                              LIGHT_Z_DP * density, LIGHT_RADIUS_DP * density, casterAlpha < 1);
	}

	/* hwui multiplies the shadow color's alpha by the light's alpha and the caster's */
	private static int shadowColor(int color, float alpha) {
		int a = Math.round(Color.alpha(color) * Math.min(Math.max(alpha, 0), 1));
		return (a << 24) | (color & 0xffffff);
	}

	/**
	 * Gets the current transform matrix
	 *
	 * @param outMatrix The matrix to store the transform of the RenderNode
	 */
	@Override
	public void getMatrix(Matrix outMatrix) {
		outMatrix.reset();
		outMatrix.setTranslate(getTranslationX(), getTranslationY());
		outMatrix.preRotate(getRotationZ(), getPivotX(), getPivotY());
		outMatrix.preScale(getScaleX(), getScaleY(), getPivotX(), getPivotY());
	}

	/**
	 * Gets the current transform inverted. This is a faster way to do the otherwise
	 * equivalent {@link #getMatrix(Matrix)} followed by {@link Matrix#invert(Matrix)}
	 *
	 * @param outMatrix The matrix to store the inverse transform of the RenderNode
	 */
	public void getInverseMatrix(Matrix outMatrix) {
		Matrix matrix = new Matrix();
		getMatrix(matrix);
		matrix.invert(outMatrix);
	}

	/**
	 * Whether or not the RenderNode has an identity transform. This is a faster
	 * way to do the otherwise equivalent {@link #getMatrix(Matrix)} {@link Matrix#isIdentity()}
	 * as it doesn't require copying the Matrix first, thus minimizing overhead.
	 */
	public boolean hasIdentityMatrix() {
		return getTranslationX() == 0 && getTranslationY() == 0 && getRotationZ() == 0
		    && getScaleX() == 1 && getScaleY() == 1;
	}

	/**
	 * Sets the position of the RenderNode.
	 *
	 * @return true if the value changed, false otherwise
	 */
	public boolean setPosition(int left, int top, int right, int bottom) {
		if (this.left == left && this.top == top && this.right == right && this.bottom == bottom)
			return false;
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
		invalidateGskNode();
		return true;
	}

	/**
	 * Sets the position of the RenderNode.
	 *
	 * @param position The position rectangle in pixels
	 * @return true if the value changed, false otherwise
	 */
	public boolean setPosition(Rect position) {
		return setPosition(position.left, position.top, position.right, position.bottom);
	}

	@Override
	public boolean setLeftTopRightBottom(int left, int top, int right, int bottom) {
		return setPosition(left, top, right, bottom);
	}

	/**
	 * Offsets the left and right positions for the RenderNode
	 *
	 * @param offset The amount that the left and right positions are offset in pixels
	 * @return true if any values changed, false otherwise.
	 */
	@Override
	public boolean offsetLeftAndRight(int offset) {
		return offset != 0 && setPosition(left + offset, top, right + offset, bottom);
	}

	/**
	 * Offsets the top and bottom values for the RenderNode
	 *
	 * @param offset The amount that the left and right positions are offset in pixels
	 * @return true if any values changed, false otherwise.
	 */
	@Override
	public boolean offsetTopAndBottom(int offset) {
		return offset != 0 && setPosition(left, top + offset, right, bottom + offset);
	}

	public int getLeft() {
		return left;
	}

	public int getTop() {
		return top;
	}

	public int getRight() {
		return right;
	}

	public int getBottom() {
		return bottom;
	}

	public int getWidth() {
		return right - left;
	}

	public int getHeight() {
		return bottom - top;
	}

	/**
	 * Sets the translucency level for the display list.
	 *
	 * @param alpha The translucency of the display list, must be a value between 0.0f and 1.0f
	 */
	@Override
	public boolean setAlpha(float alpha) {
		if (!super.setAlpha(alpha))
			return false;
		invalidateGskNode();
		return true;
	}

	public boolean setRotationZ(float rotation) {
		return setRotation(rotation);
	}

	public float getRotationZ() {
		return getRotation();
	}

	@Override
	public boolean setPivotX(float pivotX) {
		pivotExplicitlySet = true;
		return super.setPivotX(pivotX);
	}

	@Override
	public boolean setPivotY(float pivotY) {
		pivotExplicitlySet = true;
		return super.setPivotY(pivotY);
	}

	/* like hwui: the pivot is the center unless it was set */
	@Override
	public float getPivotX() {
		return pivotExplicitlySet ? super.getPivotX() : (right - left) / 2.f;
	}

	@Override
	public float getPivotY() {
		return pivotExplicitlySet ? super.getPivotY() : (bottom - top) / 2.f;
	}

	/**
	 * Clears any pivot previously set by a call to  {@link #setPivotX(float)} or
	 * {@link #setPivotY(float)}. After calling this {@link #isPivotExplicitlySet()} will be false
	 * and the pivot used for rotation will return to default of being centered on the view.
	 *
	 * @return true if the value changed, false otherwise
	 */
	public boolean resetPivot() {
		if (!pivotExplicitlySet)
			return false;
		pivotExplicitlySet = false;
		invalidateGskNode();
		return true;
	}

	public boolean isPivotExplicitlySet() {
		return pivotExplicitlySet;
	}

	@Override
	public boolean setClipToBounds(boolean clipToBounds) {
		if (this.clipToBounds == clipToBounds)
			return false;
		this.clipToBounds = clipToBounds;
		invalidateGskNode();
		return true;
	}

	public boolean getClipToBounds() {
		return clipToBounds;
	}

	/**
	 * Sets an additional clip on the RenderNode. If null, the extra clip is removed from the
	 * RenderNode. If non-null, the RenderNode will be clipped to this rect.
	 */
	public boolean setClipRect(Rect rect) {
		if (rect == null ? clipRect == null : rect.equals(clipRect))
			return false;
		clipRect = rect == null ? null : new Rect(rect);
		invalidateGskNode();
		return true;
	}

	@Override
	public boolean setClipToOutline(boolean clipToOutline) {
		if (this.clipToOutline == clipToOutline)
			return false;
		this.clipToOutline = clipToOutline;
		invalidateGskNode();
		return true;
	}

	@Override
	public boolean getClipToOutline() {
		return clipToOutline;
	}

	/**
	 * Sets the outline, defining the shape that casts a shadow, and the path to
	 * be clipped if setClipToOutline is set.
	 */
	@Override
	public boolean setOutline(Outline outline) {
		this.outline = outline == null ? null : new Outline(outline);
		invalidateGskNode();
		return true;
	}

	public boolean hasShadow() {
		return getElevation() + translationZ > 0 && outline != null && !outline.isEmpty() && outline.getAlpha() > 0;
	}

	@Override
	public boolean setElevation(float elevation) {
		boolean changed = super.setElevation(elevation);
		if (changed)
			invalidateGskNode();
		return changed;
	}

	public boolean setTranslationZ(float translationZ) {
		boolean changed = this.translationZ != translationZ;
		this.translationZ = translationZ;
		if (changed)
			invalidateGskNode();
		return changed;
	}

	public float getTranslationZ() {
		return translationZ;
	}

	public boolean setAmbientShadowColor(int color) {
		boolean changed = ambientShadowColor != color;
		ambientShadowColor = color;
		if (changed)
			invalidateGskNode();
		return changed;
	}

	public int getAmbientShadowColor() {
		return ambientShadowColor;
	}

	public boolean setSpotShadowColor(int color) {
		boolean changed = spotShadowColor != color;
		spotShadowColor = color;
		if (changed)
			invalidateGskNode();
		return changed;
	}

	public int getSpotShadowColor() {
		return spotShadowColor;
	}

	/**
	 * Controls whether or not to force this RenderNode to render to an intermediate buffer.
	 */
	public boolean setUseCompositingLayer(boolean forceToLayer, Paint paint) {
		boolean changed = useCompositingLayer != forceToLayer || layerPaint != paint;
		useCompositingLayer = forceToLayer;
		layerPaint = paint;
		return changed;
	}

	public boolean getUseCompositingLayer() {
		return useCompositingLayer;
	}

	@Override
	public boolean setHasOverlappingRendering(boolean hasOverlappingRendering) {
		boolean changed = overlappingRendering != hasOverlappingRendering;
		overlappingRendering = hasOverlappingRendering;
		return changed;
	}

	@Override
	public boolean hasOverlappingRendering() {
		return overlappingRendering;
	}

	public boolean setRenderEffect(RenderEffect renderEffect) {
		boolean changed = this.renderEffect != renderEffect;
		this.renderEffect = renderEffect;
		if (changed)
			invalidateGskNode();
		return changed;
	}

	public boolean setAnimationMatrix(Matrix matrix) {
		animationMatrix = matrix == null ? null : new Matrix(matrix);
		return true;
	}

	public Matrix getAnimationMatrix() {
		return animationMatrix;
	}

	public boolean setForceDarkAllowed(boolean allow) {
		boolean changed = forceDarkAllowed != allow;
		forceDarkAllowed = allow;
		return changed;
	}

	public boolean isForceDarkAllowed() {
		return forceDarkAllowed;
	}

	public boolean setProjectBackwards(boolean shouldProject) {
		boolean changed = projectBackwards != shouldProject;
		projectBackwards = shouldProject;
		return changed;
	}

	public boolean setProjectionReceiver(boolean shouldRecieve) {
		boolean changed = projectionReceiver != shouldRecieve;
		projectionReceiver = shouldRecieve;
		return changed;
	}

	/**
	 * Returns the unique ID that identifies this RenderNode. This ID is unique for the
	 * lifetime of the process.
	 */
	public long getUniqueId() {
		return uniqueId;
	}

	public long computeApproximateMemoryUsage() {
		return 0;
	}

	public long computeApproximateMemoryAllocated() {
		return 0;
	}
}
