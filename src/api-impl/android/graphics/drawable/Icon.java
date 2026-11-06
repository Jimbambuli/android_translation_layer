package android.graphics.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;

/* The resource and bitmap icons of AOSP's Icon (graphics/java/android/graphics/drawable/Icon.java):
 * createWithResource(), createWithBitmap() and loadDrawable() (loadDrawableInner()). An icon from a
 * resource of another package or of "android" is looked up in the app's or the system's resources,
 * the only ones ATL has; no tint, no URIs, no data, no parceling. */
public class Icon {
	private static final String TAG = "Icon";

	public static final int TYPE_BITMAP = 1;
	public static final int TYPE_RESOURCE = 2;
	public static final int TYPE_ADAPTIVE_BITMAP = 5;

	private final int mType;
	private Object mObj1; // Bitmap or Resources
	private String mString1; // resource package
	private int mInt1; // resource id

	private Icon(int type) {
		mType = type;
	}

	public int getType() {
		return mType;
	}

	public Bitmap getBitmap() {
		if (mType != TYPE_BITMAP && mType != TYPE_ADAPTIVE_BITMAP)
			throw new IllegalStateException("called getBitmap() on " + this);
		return (Bitmap)mObj1;
	}

	public String getResPackage() {
		if (mType != TYPE_RESOURCE)
			throw new IllegalStateException("called getResPackage() on " + this);
		return mString1;
	}

	public int getResId() {
		if (mType != TYPE_RESOURCE)
			throw new IllegalStateException("called getResId() on " + this);
		return mInt1;
	}

	public static Icon createWithResource(Context context, int resId) {
		if (context == null)
			throw new IllegalArgumentException("Context must not be null.");
		final Icon rep = new Icon(TYPE_RESOURCE);
		rep.mInt1 = resId;
		rep.mString1 = context.getPackageName();
		return rep;
	}

	public static Icon createWithResource(Resources res, int resId) {
		if (res == null)
			throw new IllegalArgumentException("Resource must not be null.");
		final Icon rep = new Icon(TYPE_RESOURCE);
		rep.mInt1 = resId;
		rep.mObj1 = res;
		return rep;
	}

	public static Icon createWithResource(String resPackage, int resId) {
		if (resPackage == null)
			throw new IllegalArgumentException("Resource package name must not be null.");
		final Icon rep = new Icon(TYPE_RESOURCE);
		rep.mInt1 = resId;
		rep.mString1 = resPackage;
		return rep;
	}

	public static Icon createWithBitmap(Bitmap bits) {
		if (bits == null)
			throw new IllegalArgumentException("Bitmap must not be null.");
		final Icon rep = new Icon(TYPE_BITMAP);
		rep.mObj1 = bits;
		return rep;
	}

	public static Icon createWithAdaptiveBitmap(Bitmap bits) {
		if (bits == null)
			throw new IllegalArgumentException("Bitmap must not be null.");
		final Icon rep = new Icon(TYPE_ADAPTIVE_BITMAP);
		rep.mObj1 = bits;
		return rep;
	}

	public Drawable loadDrawable(Context context) {
		switch (mType) {
			case TYPE_BITMAP:
				return new BitmapDrawable(context.getResources(), getBitmap());
			case TYPE_ADAPTIVE_BITMAP:
				return new AdaptiveIconDrawable(null, new BitmapDrawable(context.getResources(), getBitmap()));
			case TYPE_RESOURCE:
				Resources res = mObj1 instanceof Resources ? (Resources)mObj1 : null;
				if (res == null) {
					String resPackage = getResPackage();
					res = "android".equals(resPackage) && !"android".equals(context.getPackageName())
					    ? Resources.getSystem() : context.getResources();
				}
				try {
					return res.getDrawable(getResId(), context.getTheme());
				} catch (RuntimeException e) {
					Log.e(TAG, String.format("Unable to load resource 0x%08x from pkg=%s", getResId(), mString1), e);
				}
				break;
		}
		return null;
	}

	@Override
	public String toString() {
		return "Icon(typ=" + (mType == TYPE_BITMAP ? "BITMAP" : mType == TYPE_ADAPTIVE_BITMAP ? "BITMAP_MASKABLE" : "RESOURCE")
		    + (mType == TYPE_RESOURCE ? " pkg=" + mString1 + " id=" + String.format("0x%08x", mInt1) : "") + ")";
	}
}
