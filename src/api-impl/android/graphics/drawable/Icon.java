package android.graphics.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.InputStream;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/* The resource and bitmap icons of AOSP's Icon (graphics/java/android/graphics/drawable/Icon.java):
 * createWithResource(), createWithBitmap() and loadDrawable() (loadDrawableInner()). An icon from a
 * resource of another package or of "android" is looked up in the app's or the system's resources,
 * the only ones ATL has; no tint, no URIs, no data. Parceled as type and payload (bitmap, or package and id). */
public class Icon implements Parcelable {
	private static final String TAG = "Icon";

	public static final int TYPE_BITMAP = 1;
	public static final int TYPE_RESOURCE = 2;
	public static final int TYPE_DATA = 3;
	public static final int TYPE_URI = 4;
	public static final int TYPE_ADAPTIVE_BITMAP = 5;
	public static final int TYPE_URI_ADAPTIVE_BITMAP = 6;

	private final int mType;
	private Object mObj1; // Bitmap or Resources
	private String mString1; // resource package
	private int mInt1; // resource id, or the length of the data
	private int mInt2; // the offset of the data
	private ColorStateList mTintList;
	private PorterDuff.Mode mTintMode = PorterDuff.Mode.SRC_IN; // as AOSP's DEFAULT_TINT_BLEND_MODE

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

	/** AOSP Icon.createWithData(): an icon from the bytes of an image file (PNG, JPEG, ...) at an offset */
	public static Icon createWithData(byte[] data, int offset, int length) {
		if (data == null)
			throw new IllegalArgumentException("Data must not be null.");
		final Icon rep = new Icon(TYPE_DATA);
		rep.mObj1 = data;
		rep.mInt1 = length;
		rep.mInt2 = offset;
		return rep;
	}

	/** AOSP Icon.createWithContentUri(): an icon from the image a content URI points to, read when it is loaded */
	public static Icon createWithContentUri(String uri) {
		if (uri == null)
			throw new IllegalArgumentException("Uri must not be null.");
		final Icon rep = new Icon(TYPE_URI);
		rep.mString1 = uri;
		return rep;
	}

	public static Icon createWithContentUri(Uri uri) {
		if (uri == null)
			throw new IllegalArgumentException("Uri must not be null.");
		return createWithContentUri(uri.toString());
	}

	public static Icon createWithAdaptiveBitmapContentUri(String uri) {
		if (uri == null)
			throw new IllegalArgumentException("Uri must not be null.");
		final Icon rep = new Icon(TYPE_URI_ADAPTIVE_BITMAP);
		rep.mString1 = uri;
		return rep;
	}

	public static Icon createWithAdaptiveBitmapContentUri(Uri uri) {
		if (uri == null)
			throw new IllegalArgumentException("Uri must not be null.");
		return createWithAdaptiveBitmapContentUri(uri.toString());
	}

	/** AOSP Icon.getUri(): the URI of an icon made with createWithContentUri() */
	public Uri getUri() {
		if (mType != TYPE_URI && mType != TYPE_URI_ADAPTIVE_BITMAP)
			throw new IllegalStateException("called getUri() on " + this);
		return Uri.parse(mString1);
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

	/** Store a color to use as a tint for this icon; it is applied by loadDrawable(), not parceled. */
	public Icon setTint(int tint) {
		return setTintList(ColorStateList.valueOf(tint));
	}

	/** Store a color state list to use as a tint for this icon. */
	public Icon setTintList(ColorStateList tintList) {
		mTintList = tintList;
		return this;
	}

	/** Store a blending mode to use for the tint of this icon (default SRC_IN). */
	public Icon setTintMode(PorterDuff.Mode mode) {
		mTintMode = mode;
		return this;
	}

	/** Loads the drawable and, as AOSP's loadDrawable(), applies the tint. */
	public Drawable loadDrawable(Context context) {
		final Drawable result = loadDrawableInner(context);
		if (result != null && mTintList != null) {
			result.mutate();
			result.setTintList(mTintList);
			result.setTintMode(mTintMode);
		}
		return result;
	}

	/**
	 * AOSP Icon.OnDrawableLoadedListener: called on the handler's thread with the loaded drawable
	 */
	public interface OnDrawableLoadedListener {
		void onDrawableLoaded(Drawable d);
	}

	/**
	 * AOSP Icon.loadDrawableAsync(Context, Message): loads the drawable on a worker thread and sends the message
	 * with the drawable as its obj to its target handler.
	 */
	public void loadDrawableAsync(Context context, Message andThen) {
		if (andThen.getTarget() == null) {
			throw new IllegalArgumentException("callback message must have a target handler");
		}
		AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
			@Override
			public void run() {
				andThen.obj = loadDrawable(context);
				andThen.sendToTarget();
			}
		});
	}

	/**
	 * AOSP Icon.loadDrawableAsync(Context, OnDrawableLoadedListener, Handler): loads the drawable on a worker thread
	 * and calls the listener on the handler's thread.
	 */
	public void loadDrawableAsync(Context context, final OnDrawableLoadedListener listener, Handler handler) {
		AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
			@Override
			public void run() {
				final Drawable result = loadDrawable(context);
				handler.post(new Runnable() {
					@Override
					public void run() {
						listener.onDrawableLoaded(result);
					}
				});
			}
		});
	}

	private Drawable loadDrawableInner(Context context) {
		switch (mType) {
			case TYPE_BITMAP:
				return new BitmapDrawable(context.getResources(), getBitmap());
			case TYPE_ADAPTIVE_BITMAP:
				return new AdaptiveIconDrawable(null, new BitmapDrawable(context.getResources(), getBitmap()));
			case TYPE_DATA:
				return new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray((byte[])mObj1, mInt2, mInt1));
			case TYPE_URI:
			case TYPE_URI_ADAPTIVE_BITMAP:
				try (InputStream is = context.getContentResolver().openInputStream(getUri())) {
					if (is == null)
						return null;
					Bitmap bitmap = BitmapFactory.decodeStream(is);
					return mType == TYPE_URI ? new BitmapDrawable(context.getResources(), bitmap)
					                         : new AdaptiveIconDrawable(null, new BitmapDrawable(context.getResources(), bitmap));
				} catch (Exception e) {
					Log.w(TAG, "Unable to load image from URI: " + getUri(), e);
				}
				break;
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
	public int describeContents() {
		return mType == TYPE_BITMAP || mType == TYPE_ADAPTIVE_BITMAP ? Parcelable.CONTENTS_FILE_DESCRIPTOR : 0;
	}

	/* AOSP Icon.writeToParcel() without the tint fields: the type, then the bitmap or the package and id */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mType);
		switch (mType) {
			case TYPE_BITMAP:
			case TYPE_ADAPTIVE_BITMAP:
				getBitmap().writeToParcel(dest, flags);
				break;
			case TYPE_RESOURCE:
				dest.writeString(mString1);
				dest.writeInt(mInt1);
				break;
			case TYPE_DATA:
				dest.writeInt(mInt1);
				dest.writeByteArray((byte[])mObj1, mInt2, mInt1);
				break;
			case TYPE_URI:
			case TYPE_URI_ADAPTIVE_BITMAP:
				dest.writeString(mString1);
				break;
		}
	}

	private Icon(Parcel in) {
		this(in.readInt());
		switch (mType) {
			case TYPE_BITMAP:
			case TYPE_ADAPTIVE_BITMAP:
				mObj1 = Bitmap.CREATOR.createFromParcel(in);
				break;
			case TYPE_RESOURCE:
				mString1 = in.readString();
				mInt1 = in.readInt();
				break;
			case TYPE_DATA:
				mInt1 = in.readInt();
				mObj1 = in.createByteArray();
				mInt2 = 0;
				break;
			case TYPE_URI:
			case TYPE_URI_ADAPTIVE_BITMAP:
				mString1 = in.readString();
				break;
			default:
				throw new RuntimeException("invalid icon type " + mType + " in parcel");
		}
	}

	public static final Parcelable.Creator<Icon> CREATOR = new Parcelable.Creator<Icon>() {
		public Icon createFromParcel(Parcel in) {
			return new Icon(in);
		}

		public Icon[] newArray(int size) {
			return new Icon[size];
		}
	};

	@Override
	public String toString() {
		return "Icon(typ=" + (mType == TYPE_BITMAP ? "BITMAP" : mType == TYPE_ADAPTIVE_BITMAP ? "BITMAP_MASKABLE" : mType == TYPE_DATA ? "DATA" : mType == TYPE_URI ? "URI" : mType == TYPE_URI_ADAPTIVE_BITMAP ? "URI_MASKABLE" : "RESOURCE")
		    + (mType == TYPE_RESOURCE ? " pkg=" + mString1 + " id=" + String.format("0x%08x", mInt1) : "")
		    + ((mType == TYPE_URI || mType == TYPE_URI_ADAPTIVE_BITMAP) ? " uri=" + mString1 : "") + ")";
	}
}
