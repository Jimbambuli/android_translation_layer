package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class BitmapDrawable extends Drawable {

	private Bitmap bitmap;
	private ColorStateList tint;
	private PorterDuff.Mode tint_mode = DEFAULT_TINT_MODE;
	private ColorFilter color_filter;
	private final Paint paint = new Paint();
	private int target_density = DisplayMetrics.DENSITY_DEFAULT;

	public BitmapDrawable() {
	}

	public BitmapDrawable(Resources res, Bitmap bitmap) {
		this.bitmap = bitmap;
		setTargetDensity(res);
	}

	public BitmapDrawable(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme)
	    throws XmlPullParserException, IOException {
		final TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.BitmapDrawable);
		if (a.hasValue(R.styleable.BitmapDrawable_src)) {
			BitmapDrawable dr = null;
			try {
				// TODO: getDrawable() might pick up another <bitmap> XML,
				// but we should reject anything other than image files.
				dr = (BitmapDrawable)a.getDrawable(R.styleable.BitmapDrawable_src);
			} catch (java.lang.Exception e) {
				e.printStackTrace();
			}
			if (dr != null)
				bitmap = dr.bitmap;
		}
		setTargetDensity(r);
		a.recycle();
		if (bitmap == null)
			throw new XmlPullParserException("<bitmap> needs a valid `src' attribute");
	}

	public void setTargetDensity(Resources res) {
		if (res != null)
			setTargetDensity(res.getDisplayMetrics());
	}

	public void setTargetDensity(DisplayMetrics metrics) {
		setTargetDensity(metrics.densityDpi);
	}

	public void setTargetDensity(int density) {
		target_density = density == 0 ? DisplayMetrics.DENSITY_DEFAULT : density;
		invalidateSelf();
	}

	/* the size at the target density, while drawing keeps all of the bitmap's pixels */
	/* AOSP BitmapDrawable.setBitmap() (@hide) */
	public void setBitmap(Bitmap bitmap) {
		if (this.bitmap != bitmap) {
			this.bitmap = bitmap;
			invalidateSelf();
		}
	}

	@Override
	public int getIntrinsicWidth() {
		if (bitmap == null)
			return 0;
		return bitmap.getScaledWidth(target_density);
	}

	@Override
	public int getIntrinsicHeight() {
		if (bitmap == null)
			return 0;
		return bitmap.getScaledHeight(target_density);
	}

	/* overriding draw() makes Drawable use a paintable that calls it, so tint and
	 * color filter changes are picked up by every widget showing this drawable */
	@Override
	public void draw(Canvas canvas) {
		if (bitmap == null)
			return;
		ColorFilter filter = color_filter;
		if (filter == null && tint != null)
			filter = new PorterDuffColorFilter(tint.getColorForState(getState(), tint.getDefaultColor()), tint_mode);
		paint.setColorFilter(filter);
		canvas.drawBitmap(bitmap, null, getBounds(), paint);
	}

	@Override
	public void setTintList(ColorStateList tint) {
		this.tint = tint;
		invalidateSelf();
	}

	@Override
	public void setTintMode(PorterDuff.Mode tintMode) {
		tint_mode = tintMode != null ? tintMode : DEFAULT_TINT_MODE;
		invalidateSelf();
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
		color_filter = colorFilter;
		invalidateSelf();
	}

	@Override
	public ColorFilter getColorFilter() {
		return color_filter;
	}

	@Override
	public boolean isStateful() {
		return tint != null && tint.isStateful();
	}

	@Override
	protected boolean onStateChange(int[] stateSet) {
		if (!isStateful())
			return false;
		invalidateSelf();
		return true;
	}

	public void setTileModeX(Shader.TileMode mode) {}

	/* ATL draws the bitmap through its own filter */
	public void setAntiAlias(boolean aa) {}

	/* ATL draws a bitmap once, stretched to the bounds: tiling is not implemented (ProgressBar's tileify()
	 * asks for it on the bitmaps of old progress drawables) */
	public void setTileModeXY(Shader.TileMode xmode, Shader.TileMode ymode) {}

	public Paint getPaint() {
		return new Paint();
	}

	public void setGravity(int g) {}

	/* like AOSP's BitmapState: what a new BitmapDrawable needs, taken when asked for */
	@Override
	public ConstantState getConstantState() {
		return new BitmapState(this);
	}

	final static class BitmapState extends ConstantState {
		Bitmap mBitmap;
		ColorStateList mTint;
		PorterDuff.Mode mTintMode;
		int mTargetDensity;

		BitmapState(BitmapDrawable drawable) {
			mBitmap = drawable.bitmap;
			mTint = drawable.tint;
			mTintMode = drawable.tint_mode;
			mTargetDensity = drawable.target_density;
		}

		@Override
		public Drawable newDrawable() {
			return new BitmapDrawable(this);
		}

		@Override
		public Drawable newDrawable(Resources res) {
			return new BitmapDrawable(this);
		}

		@Override
		public int getChangingConfigurations() {
			return 0;
		}
	}

	private BitmapDrawable(BitmapState state) {
		bitmap = state.mBitmap;
		tint = state.mTint;
		tint_mode = state.mTintMode;
		target_density = state.mTargetDensity;
	}
}
