package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ImageView extends View {

	private Bitmap bitmap = null;
	private ScaleType scaleType = ScaleType.FIT_CENTER;
	private Drawable drawable = null;
	private ColorFilter colorFilter = null;
	private boolean adjustViewBounds = false;
	private int maxWidth = Integer.MAX_VALUE;
	private int maxHeight = Integer.MAX_VALUE;

	public ImageView(Context context) {
		this(context, null);
	}

	public ImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public ImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		haveCustomMeasure = false;
		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.ImageView, defStyleAttr, defStyleRes);
		if (a.hasValue(com.android.internal.R.styleable.ImageView_tint))
			colorFilter = new PorterDuffColorFilter(a.getColor(com.android.internal.R.styleable.ImageView_tint, 0),
			                                        PorterDuff.Mode.values()[a.getInt(com.android.internal.R.styleable.ImageView_tintMode, PorterDuff.Mode.SRC_IN.nativeInt)]);
		drawable = a.getDrawable(com.android.internal.R.styleable.ImageView_src);
		if (drawable != null) {
			drawable.setCallback(this);
			if (colorFilter != null)
				drawable.setColorFilter(colorFilter);
			native_setDrawable(widget, drawable.paintable);
		}
		adjustViewBounds = a.getBoolean(com.android.internal.R.styleable.ImageView_adjustViewBounds, false);
		maxWidth = a.getDimensionPixelSize(com.android.internal.R.styleable.ImageView_maxWidth, Integer.MAX_VALUE);
		maxHeight = a.getDimensionPixelSize(com.android.internal.R.styleable.ImageView_maxHeight, Integer.MAX_VALUE);
		if (a.hasValue(com.android.internal.R.styleable.ImageView_scaleType))
			setScaleType(scaletype_from_int[a.getInt(com.android.internal.R.styleable.ImageView_scaleType, 3 /*CENTER*/)]);
		a.recycle();
		updateTouchTarget();
	}

	/* a plain ImageView consumes touches only when clickable, unlike buttons or views with their own onTouchEvent() */
	@Override
	protected boolean passesUnconsumedTouches() {
		if (this instanceof ImageButton)
			return false;
		try {
			Class<?> c = getClass();
			return c.getMethod("onTouchEvent", MotionEvent.class).getDeclaringClass() == View.class
			    && c.getMethod("dispatchTouchEvent", MotionEvent.class).getDeclaringClass() == View.class;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	public void setImageResource(final int resid) {
		if (this.getResources().getString(resid).endsWith(".xml")) {
			setImageDrawable(getContext().getDrawable(resid));
			return;
		}
		bitmap = BitmapFactory.decodeResource(this.getResources(), resid);
		native_setDrawable(widget, bitmap != null ? bitmap.getTexture() : 0);
	}
	public void setAdjustViewBounds(boolean adjustViewBounds) {
		this.adjustViewBounds = adjustViewBounds;
		if (adjustViewBounds)
			setScaleType(ScaleType.FIT_CENTER);
		requestLayout();
	}

	public boolean getAdjustViewBounds() {
		return adjustViewBounds;
	}

	public void setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
		requestLayout();
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	public void setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
		requestLayout();
	}

	public int getMaxHeight() {
		return maxHeight;
	}

	/* AOSP's measurement from the drawable's intrinsic size, so that an image is not scaled up to the
	 * height (or width) it is given; GtkPicture would keep the aspect ratio instead. Images set as a
	 * bitmap have no Drawable here and are still measured by GTK. */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (drawable == null) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			return;
		}
		int w = Math.max(drawable.getIntrinsicWidth(), 1);
		int h = Math.max(drawable.getIntrinsicHeight(), 1);
		float desiredAspect = 0.0f;
		boolean resizeWidth = false;
		boolean resizeHeight = false;
		if (adjustViewBounds) {
			resizeWidth = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY;
			resizeHeight = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
			desiredAspect = (float)w / (float)h;
		}
		final int pleft = getPaddingLeft();
		final int pright = getPaddingRight();
		final int ptop = getPaddingTop();
		final int pbottom = getPaddingBottom();
		int widthSize;
		int heightSize;
		if (resizeWidth || resizeHeight) {
			widthSize = resolveAdjustedSize(w + pleft + pright, maxWidth, widthMeasureSpec);
			heightSize = resolveAdjustedSize(h + ptop + pbottom, maxHeight, heightMeasureSpec);
			final float actualAspect = (float)(widthSize - pleft - pright) / (heightSize - ptop - pbottom);
			if (Math.abs(actualAspect - desiredAspect) > 0.0000001) {
				boolean done = false;
				if (resizeWidth) {
					int newWidth = (int)(desiredAspect * (heightSize - ptop - pbottom)) + pleft + pright;
					if (!resizeHeight)
						widthSize = resolveAdjustedSize(newWidth, maxWidth, widthMeasureSpec);
					if (newWidth <= widthSize) {
						widthSize = newWidth;
						done = true;
					}
				}
				if (!done && resizeHeight) {
					int newHeight = (int)((widthSize - pleft - pright) / desiredAspect) + ptop + pbottom;
					if (!resizeWidth)
						heightSize = resolveAdjustedSize(newHeight, maxHeight, heightMeasureSpec);
					if (newHeight <= heightSize)
						heightSize = newHeight;
				}
			}
		} else {
			w = Math.max(w + pleft + pright, getSuggestedMinimumWidth());
			h = Math.max(h + ptop + pbottom, getSuggestedMinimumHeight());
			widthSize = resolveSizeAndState(w, widthMeasureSpec, 0);
			heightSize = resolveSizeAndState(h, heightMeasureSpec, 0);
		}
		setMeasuredDimension(widthSize, heightSize);
	}

	private static int resolveAdjustedSize(int desiredSize, int maxSize, int measureSpec) {
		final int specSize = MeasureSpec.getSize(measureSpec);
		switch (MeasureSpec.getMode(measureSpec)) {
			case MeasureSpec.UNSPECIFIED:
				return Math.min(desiredSize, maxSize);
			case MeasureSpec.AT_MOST:
				return Math.min(Math.min(desiredSize, specSize), maxSize);
			default: // EXACTLY
				return specSize;
		}
	}

	public void setScaleType(ScaleType scaleType) {
		this.scaleType = scaleType;
		native_setScaleType(widget, scaleType.nativeInt);
	}

	public ScaleType getScaleType() {
		return scaleType;
	}

	public Drawable getDrawable() {
		return drawable;
	}

	public void setImageDrawable(Drawable drawable) {
		if (drawable != null && colorFilter != null) {
			drawable = drawable.mutate();
			drawable.setColorFilter(colorFilter);
		}
		if (this.drawable != null) {
			this.drawable.setCallback(null);
		}
		this.drawable = drawable;
		if (drawable != null) {
			drawable.setCallback(this);
			if (drawable.isStateful())
				drawable.setState(getDrawableState());
		}
		native_setDrawable(widget, drawable != null ? drawable.paintable : 0);
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (drawable != null && drawable.isStateful())
			drawable.setState(getDrawableState());
	}

	public void setImageMatrix(Matrix matrix) {}

	public void setImageLevel(int level) {
		if (drawable != null)
			drawable.setLevel(level);
	}

	public void setImageURI(android.net.Uri uri) {
		if (uri == null) {
			setImageDrawable(null);
			return;
		}
		try {
			java.io.InputStream in = getContext().getContentResolver().openInputStream(uri);
			Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(in);
			in.close();
			setImageBitmap(bitmap);
		} catch (Exception e) {
			System.err.println("ImageView.setImageURI(" + uri + "): " + e);
		}
	}

	public void setImageBitmap(Bitmap bitmap) {
		native_setDrawable(widget, bitmap != null ? bitmap.getTexture() : 0);
	}

	/**
	 * Options for scaling the bounds of an image to the bounds of this view.
	 */
	public enum ScaleType {
		/**
		 * Scale using the image matrix when drawing. The image matrix can be set using
		 * {@link ImageView#setImageMatrix(Matrix)}. From XML, use this syntax:
		 * <code>android:scaleType="matrix"</code>.
		 */
		MATRIX(0),
		/**
		 * Scale the image using {@link Matrix.ScaleToFit#FILL}.
		 * From XML, use this syntax: <code>android:scaleType="fitXY"</code>.
		 */
		FIT_XY(1),
		/**
		 * Scale the image using {@link Matrix.ScaleToFit#START}.
		 * From XML, use this syntax: <code>android:scaleType="fitStart"</code>.
		 */
		FIT_START(2),
		/**
		 * Scale the image using {@link Matrix.ScaleToFit#CENTER}.
		 * From XML, use this syntax:
		 * <code>android:scaleType="fitCenter"</code>.
		 */
		FIT_CENTER(3),
		/**
		 * Scale the image using {@link Matrix.ScaleToFit#END}.
		 * From XML, use this syntax: <code>android:scaleType="fitEnd"</code>.
		 */
		FIT_END(4),
		/**
		 * Center the image in the view, but perform no scaling.
		 * From XML, use this syntax: <code>android:scaleType="center"</code>.
		 */
		CENTER(5),
		/**
		 * Scale the image uniformly (maintain the image's aspect ratio) so
		 * that both dimensions (width and height) of the image will be equal
		 * to or larger than the corresponding dimension of the view
		 * (minus padding). The image is then centered in the view.
		 * From XML, use this syntax: <code>android:scaleType="centerCrop"</code>.
		 */
		CENTER_CROP(6),
		/**
		 * Scale the image uniformly (maintain the image's aspect ratio) so
		 * that both dimensions (width and height) of the image will be equal
		 * to or less than the corresponding dimension of the view
		 * (minus padding). The image is then centered in the view.
		 * From XML, use this syntax: <code>android:scaleType="centerInside"</code>.
		 */
		CENTER_INSIDE(7);

		ScaleType(int ni) {
			nativeInt = ni;
		}
		final int nativeInt;
	}
	private final ScaleType[] scaletype_from_int = {
		ScaleType.MATRIX,
		ScaleType.FIT_XY,
		ScaleType.FIT_START,
		ScaleType.FIT_CENTER,
		ScaleType.FIT_END,
		ScaleType.CENTER,
		ScaleType.CENTER_CROP,
		ScaleType.CENTER_INSIDE,
	};

	public final void setColorFilter(int color, PorterDuff.Mode mode) {}

	public void setImageTintList(ColorStateList tint) {
		if (tint == null)
			colorFilter = null;
		else
			colorFilter = new PorterDuffColorFilter(tint.getDefaultColor(), PorterDuff.Mode.SRC_IN);
		setImageDrawable(drawable);
	}

	public void setImageAlpha(int alpha) {}



	public void setImageState(int[] state, boolean merge) {}

	public ColorStateList getImageTintList() {
		return null;
	}

	public PorterDuff.Mode getImageTintMode() {
		return null;
	}

	public void setImageTintMode(PorterDuff.Mode tintMode) {}

	public void setCropToPadding(boolean crop) {}

	public void setColorFilter(int color) {}

	public void setColorFilter(ColorFilter cf) {}

	public Matrix getImageMatrix() {
		return Matrix.IDENTITY_MATRIX;
	}

	public void clearColorFilter() {
		colorFilter = null;
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	protected native void native_setDrawable(long widget, long paintable);
	protected native void native_setScaleType(long widget, int scale_type);
}
