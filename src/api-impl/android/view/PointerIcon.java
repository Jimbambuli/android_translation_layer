package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.android.internal.util.XmlUtils;

/**
 * Represents an icon that can be used as a mouse pointer (AOSP android.view.PointerIcon). A system icon is
 * known by its type; ATL does not draw custom icons, the desktop's cursor of that type stands for it. Custom icons
 * (create(), load()) keep their bitmap and hot spot as in AOSP android-16.0.0_r4 PointerIcon.
 */
public final class PointerIcon implements android.os.Parcelable {
	public static final int TYPE_CUSTOM = -1;
	public static final int TYPE_NOT_SPECIFIED = 1;
	public static final int TYPE_NULL = 0;
	public static final int TYPE_ARROW = 1000;
	public static final int TYPE_CONTEXT_MENU = 1001;
	public static final int TYPE_HAND = 1002;
	public static final int TYPE_HELP = 1003;
	public static final int TYPE_WAIT = 1004;
	public static final int TYPE_CELL = 1006;
	public static final int TYPE_CROSSHAIR = 1007;
	public static final int TYPE_TEXT = 1008;
	public static final int TYPE_VERTICAL_TEXT = 1009;
	public static final int TYPE_ALIAS = 1010;
	public static final int TYPE_COPY = 1011;
	public static final int TYPE_NO_DROP = 1012;
	public static final int TYPE_ALL_SCROLL = 1013;
	public static final int TYPE_HORIZONTAL_DOUBLE_ARROW = 1014;
	public static final int TYPE_VERTICAL_DOUBLE_ARROW = 1015;
	public static final int TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW = 1016;
	public static final int TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW = 1017;
	public static final int TYPE_ZOOM_IN = 1018;
	public static final int TYPE_ZOOM_OUT = 1019;
	public static final int TYPE_GRAB = 1020;
	public static final int TYPE_GRABBING = 1021;
	public static final int TYPE_DEFAULT = TYPE_ARROW;

	private final int mType;
	private Bitmap mBitmap;
	private float mHotSpotX;
	private float mHotSpotY;
	private boolean mDrawNativeDropShadow;

	private PointerIcon(int type) {
		mType = type;
	}

	/** AOSP PointerIcon.create(): a custom icon from a bitmap; the hot spot must lie inside the bitmap */
	public static PointerIcon create(Bitmap bitmap, float hotSpotX, float hotSpotY) {
		if (bitmap == null) {
			throw new IllegalArgumentException("bitmap must not be null");
		}
		validateHotSpot(bitmap, hotSpotX, hotSpotY);
		PointerIcon icon = new PointerIcon(TYPE_CUSTOM);
		icon.mBitmap = bitmap;
		icon.mHotSpotX = hotSpotX;
		icon.mHotSpotY = hotSpotY;
		return icon;
	}

	/** AOSP PointerIcon.load(): a custom icon from a <pointer-icon> XML resource (bitmap, hotSpotX, hotSpotY);
	 * AOSP's animated icons keep only their first frame here */
	public static PointerIcon load(Resources resources, int resourceId) {
		if (resources == null) {
			throw new IllegalArgumentException("resources must not be null");
		}
		final XmlResourceParser parser = resources.getXml(resourceId);
		final int bitmapRes;
		final float hotSpotX;
		final float hotSpotY;
		try {
			XmlUtils.beginDocument(parser, "pointer-icon");
			final TypedArray a = resources.obtainAttributes(parser, com.android.internal.R.styleable.PointerIcon);
			bitmapRes = a.getResourceId(com.android.internal.R.styleable.PointerIcon_bitmap, 0);
			hotSpotX = (int)a.getDimension(com.android.internal.R.styleable.PointerIcon_hotSpotX, 0);
			hotSpotY = (int)a.getDimension(com.android.internal.R.styleable.PointerIcon_hotSpotY, 0);
			a.recycle();
		} catch (Exception ex) {
			throw new IllegalArgumentException("Exception parsing pointer icon resource.", ex);
		} finally {
			parser.close();
		}
		if (bitmapRes == 0) {
			throw new IllegalArgumentException("<pointer-icon> is missing bitmap attribute.");
		}
		Drawable drawable = resources.getDrawable(bitmapRes, null);
		if (drawable instanceof android.graphics.drawable.AnimationDrawable) {
			drawable = ((android.graphics.drawable.AnimationDrawable)drawable).getFrame(0);
		}
		final Bitmap bitmap;
		if (drawable instanceof BitmapDrawable) {
			bitmap = ((BitmapDrawable)drawable).getBitmap();
		} else {
			/* AOSP draws a VectorDrawable into a bitmap of its intrinsic size */
			final int w = drawable.getIntrinsicWidth();
			final int h = drawable.getIntrinsicHeight();
			if (w <= 0 || h <= 0) {
				throw new IllegalArgumentException("<pointer-icon> bitmap attribute must refer to a bitmap drawable.");
			}
			bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			drawable.setBounds(0, 0, w, h);
			drawable.draw(new Canvas(bitmap));
		}
		validateHotSpot(bitmap, hotSpotX, hotSpotY);
		PointerIcon icon = new PointerIcon(TYPE_CUSTOM);
		icon.mBitmap = bitmap;
		icon.mHotSpotX = hotSpotX;
		icon.mHotSpotY = hotSpotY;
		return icon;
	}

	private static void validateHotSpot(Bitmap bitmap, float hotSpotX, float hotSpotY) {
		if (hotSpotX < 0 || hotSpotX >= bitmap.getWidth()) {
			throw new IllegalArgumentException("x hotspot lies outside of the bitmap area");
		}
		if (hotSpotY < 0 || hotSpotY >= bitmap.getHeight()) {
			throw new IllegalArgumentException("y hotspot lies outside of the bitmap area");
		}
	}

	/** AOSP PointerIcon.getSystemIcon(): the icon of a type; TYPE_NOT_SPECIFIED is a mistake, invalid types throw */
	public static PointerIcon getSystemIcon(Context context, int type) {
		if (context == null) {
			throw new IllegalArgumentException("context must not be null");
		}
		if (type == TYPE_NOT_SPECIFIED || (type != TYPE_NULL && (type < TYPE_ARROW || type > TYPE_GRABBING || type == 1005))) {
			throw new IllegalArgumentException("Invalid pointer icon type: " + type);
		}
		return new PointerIcon(type);
	}

	public int getType() {
		return mType;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || !(other instanceof PointerIcon)) {
			return false;
		}
		PointerIcon otherIcon = (PointerIcon)other;
		if (mType != otherIcon.mType) {
			return false;
		}
		/* AOSP: custom icons are equal only with the same bitmap and hot spot */
		if (mType == TYPE_CUSTOM) {
			return mBitmap == otherIcon.mBitmap && mHotSpotX == otherIcon.mHotSpotX && mHotSpotY == otherIcon.mHotSpotY;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return mType;
	}

	public static final android.os.Parcelable.Creator<PointerIcon> CREATOR = new android.os.Parcelable.Creator<PointerIcon>() {
		@Override
		public PointerIcon createFromParcel(android.os.Parcel in) {
			final int type = in.readInt();
			if (type != TYPE_CUSTOM) {
				return new PointerIcon(type);
			}
			final PointerIcon icon = PointerIcon.create(Bitmap.CREATOR.createFromParcel(in), in.readFloat(), in.readFloat());
			icon.mDrawNativeDropShadow = in.readBoolean();
			return icon;
		}

		@Override
		public PointerIcon[] newArray(int size) {
			return new PointerIcon[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel out, int flags) {
		out.writeInt(mType);
		if (mType != TYPE_CUSTOM) {
			// a non-custom icon is loaded again from its type after un-parceling
			return;
		}
		if (mBitmap == null) {
			throw new IllegalStateException("Custom icon should be loaded upon creation");
		}
		mBitmap.writeToParcel(out, flags);
		out.writeFloat(mHotSpotX);
		out.writeFloat(mHotSpotY);
		out.writeBoolean(mDrawNativeDropShadow);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PointerIcon)) {
			return false;
		}
		PointerIcon otherIcon = (PointerIcon)other;
		if (mType != otherIcon.mType) {
			return false;
		}
		return mBitmap == otherIcon.mBitmap && mHotSpotX == otherIcon.mHotSpotX && mHotSpotY == otherIcon.mHotSpotY;
	}
}
