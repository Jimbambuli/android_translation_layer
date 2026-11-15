package android.graphics.drawable;

import android.atl.GskCanvas;
import android.atl.annotation.Export;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.LayoutDirection;
import android.util.TypedValue;
import android.util.Xml;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class Drawable {
	public static interface Callback {
		public void invalidateDrawable(Drawable drawable);
		public void scheduleDrawable(Drawable drawable, Runnable runnable, long time);
		public void unscheduleDrawable(Drawable drawable, Runnable runnable);
	}

	static final BlendMode DEFAULT_BLEND_MODE = BlendMode.SRC_IN;
	static final PorterDuff.Mode DEFAULT_TINT_MODE = PorterDuff.Mode.SRC_IN;

	private Rect mBounds = new Rect();
	private int[] mStateSet = new int[0];
	public long paintable;

	private Callback callback = null;

	public Drawable() {
		this.paintable = native_constructor();
	}

	public Drawable(long paintable) {
		native_ref(paintable);
		this.paintable = paintable;
	}

	protected void setPaintable(long paintable) {
		if (this.paintable != 0)
			native_unref(this.paintable);
		if (paintable != 0)
			native_ref(paintable);
		this.paintable = paintable;
	}

	/* AOSP Drawable.setChangingConfigurations / getChangingConfigurations */
	private int mChangingConfigurations = 0;

	/* AOSP Drawable.mLayoutDirection (View.LAYOUT_DIRECTION_LTR until set) */
	private int mLayoutDirection;

	public int getChangingConfigurations() {
		return mChangingConfigurations;
	}

	public void setChangingConfigurations(int configs) {
		mChangingConfigurations = configs;
	}

	/* like AOSP: a drawable that cannot make copies of itself has no constant state */
	public ConstantState getConstantState() {
		return null;
	}

	public static abstract class ConstantState {

		public abstract Drawable newDrawable();

		/* as in AOSP not abstract: a ConstantState of an app (or of a library built against an old SDK) may
		 * implement only newDrawable() */
		public Drawable newDrawable(Resources res) {
			return newDrawable();
		}

		public Drawable newDrawable(Resources res, Theme theme) {
			return newDrawable(res);
		}

		public abstract int getChangingConfigurations();

		public boolean canApplyTheme() {
			return false;
		}
	}

	@Export
	public void setBounds(int left, int top, int right, int bottom) {
		boolean changed = left != mBounds.left || top != mBounds.top || right != mBounds.right || bottom != mBounds.bottom;
		mBounds.set(left, top, right, bottom);
		if (changed)
			onBoundsChange(mBounds);
	}

	public void setBounds(Rect bounds) {
		setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
	}

	public final Rect getBounds() {
		return mBounds;
	}

	@Export
	public void draw(Canvas canvas) {
		if (canvas instanceof GskCanvas) {
			if (mBounds.left != 0 || mBounds.top != 0)
				canvas.translate(mBounds.left, mBounds.top);
			native_draw(paintable, ((GskCanvas)canvas).snapshot, mBounds.width(), mBounds.height());
			if (mBounds.left != 0 || mBounds.top != 0)
				canvas.translate(-mBounds.left, -mBounds.top);
		}
	}

	public boolean setState(int[] stateSet) {
		if (!Arrays.equals(this.mStateSet, stateSet)) {
			this.mStateSet = stateSet;
			return onStateChange(stateSet);
		}
		return false;
	}

	public int[] getState() {
		return mStateSet;
	}

	public void setCallback(Callback callback) {
		this.callback = callback;
	}

	public Callback getCallback() {
		return this.callback;
	}

	public void invalidateSelf() {
		native_invalidate(paintable);

		/* this shouldn't ever be needed with Gtk, but let's play it safe for now */
		if (this.callback != null) {
			callback.invalidateDrawable(this);
		}
	}

	public void scheduleSelf(Runnable runnable, long time) {
		if (this.callback != null) {
			callback.scheduleDrawable(this, runnable, time);
		}
	}

	public void unscheduleSelf(Runnable runnable) {
		if (this.callback != null) {
			callback.unscheduleDrawable(this, runnable);
		}
	}

	private boolean mVisible = true;

	public final boolean isVisible() {
		return mVisible;
	}

	/**
	 * Set whether this Drawable is visible.  This generally does not impact
	 * the Drawable's behavior, but is a hint that can be used by some
	 * Drawables, for example, to decide whether run animations.
	 *
	 * @param visible Set to true if visible, false if not.
	 * @param restart You can supply true here to force the drawable to behave
	 *                as if it has just become visible, even if it had last
	 *                been set visible.  Used for example to force animations
	 *                to restart.
	 *
	 * @return boolean Returns true if the new visibility is different than
	 *         its previous state.
	 */
	public boolean setVisible(boolean visible, boolean restart) {
		boolean changed = mVisible != visible;
		if (changed) {
			mVisible = visible;
			invalidateSelf();
		}
		return changed;
	}

	/**
	 * Clears the mutated state, allowing this drawable to be cached and
	 * mutated again.
	 * <p>
	 * This is hidden because only framework drawables can be cached, so
	 * custom drawables don't need to support constant state, mutate(), or
	 * clearMutated().
	 *
	 * @hide
	 */
	public void clearMutated() {
		// Default implementation is no-op.
	}

	/**
	 * Inflate a Drawable from an XML resource.
	 *
	 * @hide
	 */
	void inflateWithAttributes(Resources r, XmlPullParser parser, TypedArray attrs, int visibleAttr)
	    throws XmlPullParserException, IOException {
		mVisible = attrs.getBoolean(visibleAttr, mVisible);
	}

	protected static TypedArray obtainAttributes(Resources r, Theme theme, AttributeSet set, int[] attrs) {
		if (theme != null)
			return theme.obtainStyledAttributes(set, attrs, 0, 0);
		else
			return r.obtainAttributes(set, attrs);
	}

	public void clearColorFilter() {
		setColorFilter(null);
	}

	private int level = 0;

	public final int getLevel() {
		return level;
	}

	/**
	 * Sets the level (0..10000) and calls onLevelChange() if it changed, like AOSP.
	 *
	 * @return the result of onLevelChange(), i.e. whether the drawable needs to be redrawn
	 */
	public final boolean setLevel(int level) {
		if (this.level != level) {
			this.level = level;
			return onLevelChange(level);
		}
		return false;
	}

	public void setColorFilter(int color, PorterDuff.Mode mode) {
		setColorFilter(new PorterDuffColorFilter(color, mode));
	}
	public void setColorFilter(ColorFilter filter) {}

	public ColorFilter getColorFilter() { return null; }

	public Drawable mutate() {
		return this;
	}

	@Export
	public int getIntrinsicWidth() { return -1; }

	@Export
	public int getIntrinsicHeight() { return -1; }

	public void setTintList(ColorStateList tint) {}

	public void setTint(int tint) {
		setTintList(ColorStateList.valueOf(tint));
	}

	/**
	 * Parses a {@link android.graphics.PorterDuff.Mode} from a tintMode
	 * attribute's enum value.
	 *
	 * @hide
	 */
	public static PorterDuff.Mode parseTintMode(int value, PorterDuff.Mode defaultMode) {
		switch (value) {
			case 3: return PorterDuff.Mode.SRC_OVER;
			case 5: return PorterDuff.Mode.SRC_IN;
			case 9: return PorterDuff.Mode.SRC_ATOP;
			case 14: return PorterDuff.Mode.MULTIPLY;
			case 15: return PorterDuff.Mode.SCREEN;
			case 16: return PorterDuff.Mode.ADD;
			default: return defaultMode;
		}
	}

	/* Copyright (C) 2006 The Android Open Source Project */
	public static BlendMode parseBlendMode(int value, BlendMode defaultMode) {
		switch (value) {
			case 3: return BlendMode.SRC_OVER;
			case 5: return BlendMode.SRC_IN;
			case 9: return BlendMode.SRC_ATOP;
			// b/73224934 PorterDuff Multiply maps to Skia Modulate so actually
			// return BlendMode.MODULATE here
			case 14: return BlendMode.MODULATE;
			case 15: return BlendMode.SCREEN;
			case 16: return BlendMode.PLUS;
			default: return defaultMode;
		}
	}

	/* Copyright (C) 2006 The Android Open Source Project */
	BlendModeColorFilter updateBlendModeFilter(BlendModeColorFilter blendFilter, ColorStateList tint, BlendMode blendMode) {
		if (tint == null || blendMode == null) {
			return null;
		}

		final int color = tint.getColorForState(getState(), Color.TRANSPARENT);
		if (blendFilter == null || blendFilter.getColor() != color
		    || blendFilter.getMode() != blendMode) {
			return new BlendModeColorFilter(color, blendMode);
		}
		return blendFilter;
	}

	/* Copyright (C) 2006 The Android Open Source Project */
	PorterDuffColorFilter updateTintFilter(PorterDuffColorFilter tintFilter, ColorStateList tint, PorterDuff.Mode tintMode) {
		System.out.println("updateTintFilter(" + tintFilter + ", " + tint + ", " + tintMode + ")");
		if (tint == null || tintMode == null) {
			return null;
		}
		final int color = tint.getColorForState(getState(), Color.TRANSPARENT);
		if (tintFilter == null || tintFilter.getColor() != color || tintFilter.getMode() != tintMode) {
			return new PorterDuffColorFilter(color, tintMode);
		}
		return tintFilter;
	}

	public boolean isStateful() {
		return false;
	}

	public boolean hasFocusStateSpecified() {
		return false;
	}

	/* AOSP Drawable.setTintMode: forwards to setTintBlendMode, which subclasses override */
	private boolean mSetTintModeInvoked;

	public void setTintMode(PorterDuff.Mode tintMode) {
		if (!mSetTintModeInvoked) {
			mSetTintModeInvoked = true;
			BlendMode mode = tintMode != null ? BlendMode.fromValue(tintMode.nativeInt) : null;
			setTintBlendMode(mode != null ? mode : Drawable.DEFAULT_BLEND_MODE);
			mSetTintModeInvoked = false;
		}
	}

	/* AOSP Drawable.setTintBlendMode */
	private boolean mSetBlendModeInvoked;

	public void setTintBlendMode(BlendMode blendMode) {
		if (!mSetBlendModeInvoked) {
			mSetBlendModeInvoked = true;
			PorterDuff.Mode mode = BlendMode.blendModeToPorterDuffMode(blendMode);
			setTintMode(mode != null ? mode : Drawable.DEFAULT_TINT_MODE);
			mSetBlendModeInvoked = false;
		}
	}

	/* AOSP Drawable.getDirtyBounds */
	public Rect getDirtyBounds() {
		return getBounds();
	}

	/* AOSP Drawable.setXfermode */
	public void setXfermode(Xfermode mode) {
		// Base implementation drops it on the floor for compatibility. Whee!
	}

	/* AOSP Drawable.getHotspotBounds */
	public void getHotspotBounds(Rect outRect) {
		outRect.set(getBounds());
	}

	public boolean isProjected() { return false; }

	public static Drawable createFromXml(Resources resources, XmlPullParser parser) throws XmlPullParserException, IOException {
		return createFromXml(resources, parser, null);
	}

	public static Drawable createFromXml(Resources resources, XmlPullParser parser, Theme theme) throws XmlPullParserException, IOException {
		AttributeSet attrs = Xml.asAttributeSet(parser);
		int type;
		while ((type = parser.next()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT)
			;
		if (type != XmlPullParser.START_TAG)
			throw new XmlPullParserException("No start tag found");

		return createFromXmlInner(resources, parser, attrs, theme);
	}

	public static Drawable createFromXml(Resources resources, XmlResourceParser parser) throws XmlPullParserException, IOException {
		return createFromXml(resources, parser, null);
	}

	public static Drawable createFromXml(Resources resources, XmlResourceParser parser, Theme theme) throws XmlPullParserException, IOException {
		int type;
		while ((type = parser.next()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT)
			;
		if (type != XmlPullParser.START_TAG)
			throw new XmlPullParserException("No start tag found");

		return createFromXmlInner(resources, parser, parser, theme);
	}

	public static Drawable createFromXmlInner(Resources resources, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
		return createFromXmlInner(resources, parser, attrs, null);
	}

	public static Drawable createFromXmlInner(Resources resources, XmlPullParser parser, AttributeSet attrs, Theme theme) throws XmlPullParserException, IOException {
		switch (parser.getName()) {
			case "selector": {
				StateListDrawable drawable = new StateListDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "animated-selector": {
				AnimatedStateListDrawable drawable = new AnimatedStateListDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "shape": {
				GradientDrawable drawable = new GradientDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "bitmap": {
				BitmapDrawable drawable = new BitmapDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "transition": {
				TransitionDrawable drawable = new TransitionDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "ripple": {
				RippleDrawable drawable = new RippleDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "vector": {
				VectorDrawable drawable = new VectorDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "layer-list": {
				LayerDrawable drawable = new LayerDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "nine-patch": {
				NinePatchDrawable drawable = new NinePatchDrawable(resources, null, null, null, null);
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "animation-list": {
				AnimationDrawable drawable = new AnimationDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "adaptive-icon": {
				AdaptiveIconDrawable drawable = new AdaptiveIconDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "rotate": {
				RotateDrawable drawable = new RotateDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "inset": {
				InsetDrawable drawable = new InsetDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "color": {
				ColorDrawable drawable = new ColorDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "level-list": {
				LevelListDrawable drawable = new LevelListDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "scale": {
				ScaleDrawable drawable = new ScaleDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "clip": {
				ClipDrawable drawable = new ClipDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "animated-rotate": {
				AnimatedRotateDrawable drawable = new AnimatedRotateDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
			case "animated-vector": {
				AnimatedVectorDrawable drawable = new AnimatedVectorDrawable();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			}
		}

		/* like AOSP's DrawableInflater.inflateFromClass(): a tag with dots names a Drawable class
		 * (AnimationScaleListDrawable in the framework's progress drawables) */
		String name = parser.getName();
		if (name.indexOf('.') > 0) {
			try {
				Drawable drawable = (Drawable)Class.forName(name).getConstructor().newInstance();
				drawable.inflate(resources, parser, attrs, theme);
				return drawable;
			} catch (ReflectiveOperationException e) {
				throw new XmlPullParserException("Error inflating class " + name + ": " + e, parser, e);
			}
		}

		return null;
	}

	public static Drawable createFromResourceStream(Resources resources, TypedValue value, InputStream is, String file) {
		return createFromResourceStream(resources, value, is, file, (BitmapFactory.Options) null);
	}

	public static Drawable createFromResourceStream(Resources resources, TypedValue value, InputStream is, String file,
	                                                Object object) {
		return createFromResourceStream(resources, value, is, file, object instanceof BitmapFactory.Options ? (BitmapFactory.Options) object : null);
	}

	/* AOSP createFromResourceStream(): the options (may be null) are those of the bitmap decode */
	public static Drawable createFromResourceStream(Resources resources, TypedValue value, InputStream is, String file,
	                                                BitmapFactory.Options opts) {
		if (is == null)
			return null;
		/* the pixels are kept as they are, so the bitmap is as sharp as the resource on a
		 * monitor with a higher density, and the density sets the size (as AOSP's
		 * BitmapFactory.decodeResourceStream() does for the scaling) */
		int density = DisplayMetrics.DENSITY_DEFAULT;
		if (value != null && value.density == TypedValue.DENSITY_NONE)
			density = Bitmap.DENSITY_NONE;
		else if (value != null && value.density != TypedValue.DENSITY_DEFAULT)
			density = value.density;
		/* like AOSP's createFromResourceStream(): decode the stream, and when the bitmap carries a 9-patch
		 * chunk (BitmapFactory takes it from the PNG) make a NinePatchDrawable of it; nothing is written */
		final Rect pad = new Rect();
		final Bitmap bm = BitmapFactory.decodeStream(is, pad, opts);
		if (bm == null)
			return null;
		bm.setDensity(density);
		final byte[] np = bm.getNinePatchChunk();
		if (np != null)
			return new NinePatchDrawable(resources, bm, np, pad, file);
		return new BitmapDrawable(resources, bm);
	}

	public static Drawable createFromPath(String path) {
		if (path == null)
			return null;

		if (path.endsWith(".9.png"))
			return new NinePatchDrawable(path);
		long paintable = native_paintable_from_path(path);
		return new Drawable(paintable);
	}

	protected boolean onStateChange(int[] stateSet) {
		return false;
	}

	public void setAlpha(int alpha) {}

	public int getAlpha() {
		return 0xFF;
	}

	/* AOSP has no default; subclasses that know better override it */
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	/* AOSP Drawable.resolveOpacity */
	public static int resolveOpacity(int op1, int op2) {
		if (op1 == op2) {
			return op1;
		}
		if (op1 == PixelFormat.UNKNOWN || op2 == PixelFormat.UNKNOWN) {
			return PixelFormat.UNKNOWN;
		}
		if (op1 == PixelFormat.TRANSLUCENT || op2 == PixelFormat.TRANSLUCENT) {
			return PixelFormat.TRANSLUCENT;
		}
		if (op1 == PixelFormat.TRANSPARENT || op2 == PixelFormat.TRANSPARENT) {
			return PixelFormat.TRANSPARENT;
		}
		return PixelFormat.OPAQUE;
	}

	public Region getTransparentRegion() {
		return null;
	}

	public void getOutline(Outline outline) {
		outline.setRect(getBounds());
		outline.setAlpha(0);
	}

	protected boolean onLevelChange(int level) {
		return false;
	}

	public boolean onLayoutDirectionChanged(int layoutDirection) {
		return false;
	}

	/* themes are applied at inflation, never afterwards */
	public boolean canApplyTheme() {
		return false;
	}

	public void applyTheme(Theme t) {}

	public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
		inflate(r, parser, attrs, null);
	}

	/* AOSP Drawable.inflate(): the base class reads android:visible */
	public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme) throws XmlPullParserException, IOException {
		final TypedArray a = obtainAttributes(r, theme, attrs, com.android.internal.R.styleable.Drawable);
		mVisible = a.getBoolean(com.android.internal.R.styleable.Drawable_visible, mVisible);
		a.recycle();
	}

	public static Drawable createFromStream(InputStream is, String srcName) {
		final Bitmap bm = BitmapFactory.decodeStream(is);
		return bm != null ? new BitmapDrawable(bm) : null;
	}

	/* AOSP Drawable.getPadding(): no padding is all zero */
	public boolean getPadding(Rect padding) {
		padding.set(0, 0, 0, 0);
		return false;
	}

	public Insets getOpticalInsets() {
		return Insets.NONE;
	}

	public void copyBounds(Rect bounds) {
		bounds.set(mBounds);
	}

	/* AOSP Drawable.getMinimumWidth / getMinimumHeight */
	public int getMinimumWidth() {
		final int intrinsicWidth = getIntrinsicWidth();
		return intrinsicWidth > 0 ? intrinsicWidth : 0;
	}

	public int getMinimumHeight() {
		final int intrinsicHeight = getIntrinsicHeight();
		return intrinsicHeight > 0 ? intrinsicHeight : 0;
	}

	protected void onBoundsChange(Rect bounds) {}

	public void setDither(boolean dither) {}

	public void setAutoMirrored(boolean mirrored) {}

	public boolean isAutoMirrored() {
		return false;
	}

	public void jumpToCurrentState() {}

	/* AOSP Drawable.setLayoutDirection */
	public final boolean setLayoutDirection(int layoutDirection) {
		if (mLayoutDirection != layoutDirection) {
			mLayoutDirection = layoutDirection;
			return onLayoutDirectionChanged(layoutDirection);
		}
		return false;
	}

	public void setHotspot(float x, float y) {}

	/* AOSP Drawable.getLayoutDirection */
	public int getLayoutDirection() {
		return mLayoutDirection;
	}

	/* AOSP Drawable.scaleFromDensity */
	static float scaleFromDensity(float pixels, int sourceDensity, int targetDensity) {
		return pixels * targetDensity / sourceDensity;
	}

	static int scaleFromDensity(int pixels, int sourceDensity, int targetDensity, boolean isSize) {
		if (pixels == 0 || sourceDensity == targetDensity) {
			return pixels;
		}

		final float result = pixels * targetDensity / (float)sourceDensity;
		if (!isSize) {
			return (int)result;
		}

		final int rounded = Math.round(result);
		if (rounded != 0) {
			return rounded;
		} else if (pixels > 0) {
			return 1;
		} else {
			return -1;
		}
	}

	/* AOSP Drawable.rethrowAsRuntimeException */
	static void rethrowAsRuntimeException(Exception cause) throws RuntimeException {
		final RuntimeException e = new RuntimeException(cause);
		e.setStackTrace(new StackTraceElement[0]);
		throw e;
	}

	/* AOSP Drawable.mSrcDensityOverride: the density BitmapDrawable.inflate() asks the resource for */
	protected int mSrcDensityOverride = 0;

	static int resolveDensity(Resources r, int parentDensity) {
		final int densityDpi = r == null ? parentDensity : r.getDisplayMetrics().densityDpi;
		return densityDpi == 0 ? DisplayMetrics.DENSITY_DEFAULT : densityDpi;
	}

	public void setFilterBitmap(boolean filter) {}

	public boolean isFilterBitmap() {
		return false;
	}

	public void setHotspotBounds(int left, int top, int right, int bottom) {}

	public Drawable getCurrent() {
		return this;
	}

	public Rect copyBounds() {
		return new Rect(mBounds);
	}

	@SuppressWarnings("removal")
	protected void finalize() throws Throwable {
		try {
			if (paintable != 0)
				native_unref(paintable);
			paintable = 0;
		} finally {
			super.finalize();
		}
	}

	protected static native long native_paintable_from_path(String path);
	protected native long native_constructor();
	protected native void native_invalidate(long paintable);
	protected native void native_draw(long paintable, long snapshot, int width, int height);
	protected native void native_ref(long paintable);
	protected native void native_unref(long paintable);
}
