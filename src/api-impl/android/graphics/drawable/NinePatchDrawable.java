package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
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

	@Override
	public void setTint(int tint) {
		nativeSetTint(paintable, tint);
	}

	private ColorStateList tintList;

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
		nativeSetTint(paintable, tint != null ? tint.getColorForState(getState(), tint.getDefaultColor()) : 0);
	}

	@Override
	public boolean isStateful() {
		return tintList != null && tintList.isStateful();
	}

	@Override
	protected boolean onStateChange(int[] stateSet) {
		if (tintList == null || !tintList.isStateful())
			return false;
		nativeSetTint(paintable, tintList.getColorForState(stateSet, tintList.getDefaultColor()));
		return true;
	}

	/* the native tint colours the picture like a PorterDuff SRC_IN filter, which is how
	 * AppCompat tints the nine-patches of its widgets */
	@Override
	public void setColorFilter(ColorFilter colorFilter) {
		nativeSetTint(paintable, colorFilter instanceof PorterDuffColorFilter ? ((PorterDuffColorFilter)colorFilter).getColor() : 0);
	}

	private native long nativeCreate(byte[] data, long texture);
	private native long nativeCreate(String path);
	private native void nativeSetTint(long paintable, int tint);
	private native void nativeSetScale(long paintable, float scale);
}
