package android.widget;

import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import java.util.Objects;

/**
 * android-14 frameworks/base core/java/android/widget/Magnifier.java: parameters, Builder defaults
 * (core/res/res/values/dimens.xml default_magnifier_*) and argument checks as in AOSP.
 * Not ported: the magnifier window. AOSP draws a copy of the view's surface into its own
 * InternalPopupWindow (SurfaceControl + ThreadedRenderer), which ATL does not have; show() therefore
 * creates no window, and getPosition()/getSourcePosition() return null as AOSP does while no window
 * exists. Apps keep working, the magnifier is just not drawn.
 */
public final class Magnifier {

	public static final int SOURCE_BOUND_MAX_IN_SURFACE = 0;
	public static final int SOURCE_BOUND_MAX_VISIBLE = 1;

	private final View mView;
	private final int mWindowWidth;
	private final int mWindowHeight;
	private float mZoom;
	private int mSourceWidth;
	private int mSourceHeight;
	private final float mWindowElevation;
	private final float mWindowCornerRadius;
	private final Drawable mOverlay;
	private final int mDefaultHorizontalSourceToMagnifierOffset;
	private final int mDefaultVerticalSourceToMagnifierOffset;
	private final boolean mClippingEnabled;

	@Deprecated
	public Magnifier(View view) {
		this(createBuilderWithOldMagnifierDefaults(view));
	}

	/* AOSP createBuilderWithOldMagnifierDefaults(): Widget.Magnifier (core/res/res/values/styles.xml) has the
	 * Builder's sizes, zoom and offsets, overlay #0EFFFFFF, top/bottom bound MAX_IN_SURFACE; the corner radius
	 * (AOSP: the theme's dialogCornerRadius) stays at the Builder default */
	static Builder createBuilderWithOldMagnifierDefaults(final View view) {
		final Builder params = new Builder(view);
		params.mOverlay = new ColorDrawable(0x0EFFFFFF);
		params.mTopContentBound = SOURCE_BOUND_MAX_IN_SURFACE;
		params.mBottomContentBound = SOURCE_BOUND_MAX_IN_SURFACE;
		return params;
	}

	private Magnifier(Builder params) {
		mView = params.mView;
		mWindowWidth = params.mWidth;
		mWindowHeight = params.mHeight;
		mZoom = params.mZoom;
		mSourceWidth = Math.round(mWindowWidth / mZoom);
		mSourceHeight = Math.round(mWindowHeight / mZoom);
		mWindowElevation = params.mElevation;
		mWindowCornerRadius = params.mCornerRadius;
		mOverlay = params.mOverlay;
		mDefaultHorizontalSourceToMagnifierOffset = params.mHorizontalDefaultSourceToMagnifierOffset;
		mDefaultVerticalSourceToMagnifierOffset = params.mVerticalDefaultSourceToMagnifierOffset;
		mClippingEnabled = params.mClippingEnabled;
	}

	public void show(float sourceCenterX, float sourceCenterY) {
		show(sourceCenterX, sourceCenterY,
		     sourceCenterX + mDefaultHorizontalSourceToMagnifierOffset,
		     sourceCenterY + mDefaultVerticalSourceToMagnifierOffset);
	}

	public void show(float sourceCenterX, float sourceCenterY, float magnifierCenterX, float magnifierCenterY) {
		/* no InternalPopupWindow in ATL (see class comment) */
	}

	public void dismiss() {}

	public void update() {}

	public int getWidth() {
		return mWindowWidth;
	}

	public int getHeight() {
		return mWindowHeight;
	}

	public int getSourceWidth() {
		return mSourceWidth;
	}

	public int getSourceHeight() {
		return mSourceHeight;
	}

	public void setZoom(float zoom) {
		checkArgumentPositive(zoom, "Zoom should be positive");
		mZoom = zoom;
		mSourceWidth = Math.round(mWindowWidth / mZoom);
		mSourceHeight = Math.round(mWindowHeight / mZoom);
	}

	public float getZoom() {
		return mZoom;
	}

	public float getElevation() {
		return mWindowElevation;
	}

	public float getCornerRadius() {
		return mWindowCornerRadius;
	}

	public int getDefaultHorizontalSourceToMagnifierOffset() {
		return mDefaultHorizontalSourceToMagnifierOffset;
	}

	public int getDefaultVerticalSourceToMagnifierOffset() {
		return mDefaultVerticalSourceToMagnifierOffset;
	}

	public Drawable getOverlay() {
		return mOverlay;
	}

	public boolean isClippingEnabled() {
		return mClippingEnabled;
	}

	/** AOSP: null while the magnifier has no window */
	public Point getPosition() {
		return null;
	}

	/** AOSP: null while the magnifier has no window */
	public Point getSourcePosition() {
		return null;
	}

	private static void checkArgumentPositive(float value, String message) {
		if (value <= 0)
			throw new IllegalArgumentException(message);
	}

	private static void checkArgumentNonNegative(float value, String message) {
		if (value < 0)
			throw new IllegalArgumentException(message);
	}

	/** AOSP Magnifier.Builder */
	public static final class Builder {
		private View mView;
		private int mWidth;
		private int mHeight;
		private float mZoom;
		private float mElevation;
		private float mCornerRadius;
		private Drawable mOverlay;
		private int mHorizontalDefaultSourceToMagnifierOffset;
		private int mVerticalDefaultSourceToMagnifierOffset;
		private boolean mClippingEnabled;
		private int mLeftContentBound;
		private int mTopContentBound;
		private int mRightContentBound;
		private int mBottomContentBound;

		public Builder(View view) {
			mView = Objects.requireNonNull(view);
			applyDefaults();
		}

		/* AOSP applyDefaults(); values of core/res/res/values/dimens.xml and colors.xml */
		private void applyDefaults() {
			android.util.DisplayMetrics dm = mView.getContext().getResources().getDisplayMetrics();
			mWidth = dpPixelSize(100, dm);
			mHeight = dpPixelSize(48, dm);
			mElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, dm);
			mCornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm);
			mZoom = 1.25f;
			mHorizontalDefaultSourceToMagnifierOffset = 0;
			mVerticalDefaultSourceToMagnifierOffset = dpPixelSize(-42, dm);
			mOverlay = new ColorDrawable(0x00FFFFFF);
			mClippingEnabled = true;
			mLeftContentBound = SOURCE_BOUND_MAX_VISIBLE;
			mTopContentBound = SOURCE_BOUND_MAX_VISIBLE;
			mRightContentBound = SOURCE_BOUND_MAX_VISIBLE;
			mBottomContentBound = SOURCE_BOUND_MAX_VISIBLE;
		}

		/* Resources.getDimensionPixelSize() rounding */
		private static int dpPixelSize(float dp, android.util.DisplayMetrics dm) {
			float f = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
			int res = (int)((f >= 0) ? (f + 0.5f) : (f - 0.5f));
			return res != 0 ? res : (dp > 0 ? 1 : (dp < 0 ? -1 : 0));
		}

		public Builder setSize(int width, int height) {
			checkArgumentPositive(width, "Width should be positive");
			checkArgumentPositive(height, "Height should be positive");
			mWidth = width;
			mHeight = height;
			return this;
		}

		public Builder setInitialZoom(float zoom) {
			checkArgumentPositive(zoom, "Zoom should be positive");
			mZoom = zoom;
			return this;
		}

		public Builder setElevation(float elevation) {
			checkArgumentNonNegative(elevation, "Elevation should be non-negative");
			mElevation = elevation;
			return this;
		}

		public Builder setCornerRadius(float cornerRadius) {
			checkArgumentNonNegative(cornerRadius, "Corner radius should be non-negative");
			mCornerRadius = cornerRadius;
			return this;
		}

		public Builder setOverlay(Drawable overlay) {
			mOverlay = overlay;
			return this;
		}

		public Builder setDefaultSourceToMagnifierOffset(int horizontalOffset, int verticalOffset) {
			mHorizontalDefaultSourceToMagnifierOffset = horizontalOffset;
			mVerticalDefaultSourceToMagnifierOffset = verticalOffset;
			return this;
		}

		public Builder setClippingEnabled(boolean clip) {
			mClippingEnabled = clip;
			return this;
		}

		public Builder setSourceBounds(int left, int top, int right, int bottom) {
			mLeftContentBound = left;
			mTopContentBound = top;
			mRightContentBound = right;
			mBottomContentBound = bottom;
			return this;
		}

		public Magnifier build() {
			return new Magnifier(this);
		}
	}
}
