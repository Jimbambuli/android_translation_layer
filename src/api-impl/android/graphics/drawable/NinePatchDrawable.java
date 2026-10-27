package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class NinePatchDrawable extends Drawable {

	public NinePatchDrawable(Resources res, Bitmap bitmap, byte[] data, Rect padding, String name) {
		if (bitmap != null && data != null)
			setPaintable(nativeCreate(data, bitmap.getTexture()));
		if (paintable != 0 && res != null)
			setScale(bitmap.getDensity(), res.getDisplayMetrics().densityDpi);
	}

	NinePatchDrawable(String path) {
		setPaintable(nativeCreate(path));
	}

	/* a 9-patch made for density, shown at targetDensity */
	NinePatchDrawable(String path, int density, int targetDensity) {
		this(path);
		if (paintable != 0)
			setScale(density, targetDensity);
	}

	private void setScale(int density, int targetDensity) {
		if (density != Bitmap.DENSITY_NONE && targetDensity != 0 && density != targetDensity)
			nativeSetScale(paintable, density / (float)targetDensity);
	}

	private ColorStateList tintList;
	private PorterDuff.Mode tintMode = DEFAULT_TINT_MODE;
	private ColorFilter colorFilter;

	/* Like AOSP's draw(), a colour filter set with setColorFilter() overrides the filter made from the
	 * tint (updateTintFilter(), with the default mode SRC_IN). The native paintable can only colour the
	 * picture the way a PorterDuff SRC_IN filter does; other colour filters are not drawn. */
	private void updateNativeTint() {
		ColorFilter filter = colorFilter;
		if (filter == null && tintList != null && tintMode != null)
			filter = new PorterDuffColorFilter(tintList.getColorForState(getState(), Color.TRANSPARENT), tintMode);
		int color = 0;
		if (filter instanceof PorterDuffColorFilter && ((PorterDuffColorFilter)filter).getMode() == PorterDuff.Mode.SRC_IN)
			color = ((PorterDuffColorFilter)filter).getColor();
		nativeSetTint(paintable, color);
	}

	/* <nine-patch android:src="..." android:tint="..."/> */
	@Override
	public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme)
	    throws XmlPullParserException, IOException {
		final TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.NinePatchDrawable);
		Drawable src = a.getDrawable(R.styleable.NinePatchDrawable_src);
		if (!(src instanceof NinePatchDrawable)) {
			a.recycle();
			throw new XmlPullParserException("<nine-patch> requires a valid 9-patch source image");
		}
		setPaintable(src.paintable);
		if (a.hasValue(R.styleable.NinePatchDrawable_tint))
			setTintList(a.getColorStateList(R.styleable.NinePatchDrawable_tint));
		a.recycle();
	}

	@Override
	public void setTintList(ColorStateList tint) {
		tintList = tint;
		updateNativeTint();
		invalidateSelf();
	}

	@Override
	public void setTintMode(PorterDuff.Mode tintMode) {
		this.tintMode = tintMode;
		updateNativeTint();
		invalidateSelf();
	}

	@Override
	public boolean isStateful() {
		return tintList != null && tintList.isStateful();
	}

	@Override
	protected boolean onStateChange(int[] stateSet) {
		if (tintList != null && tintMode != null) {
			updateNativeTint();
			return true;
		}
		return false;
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
		this.colorFilter = colorFilter;
		updateNativeTint();
		invalidateSelf();
	}

	@Override
	public ColorFilter getColorFilter() {
		return colorFilter;
	}

	private native long nativeCreate(byte[] data, long texture);
	private native long nativeCreate(String path);
	private native void nativeSetTint(long paintable, int tint);
	private native void nativeSetScale(long paintable, float scale);
}
