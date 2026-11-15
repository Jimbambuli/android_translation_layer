package android.renderscript;

import android.graphics.Bitmap;

/* AOSP Allocation, reduced to the 1D/2D unsigned 8 bit memory of the Elements here, kept in a byte array (AOSP:
 * driver memory). Bitmaps are copied in and out: an ARGB_8888 bitmap is stored as the bytes R, G, B, A (AOSP's
 * RGBA_8888 memory order), an ALPHA_8 bitmap as one byte per pixel. */
public class Allocation extends BaseObj {
	public static final int USAGE_SCRIPT = 0x0001;
	public static final int USAGE_GRAPHICS_TEXTURE = 0x0002;
	public static final int USAGE_SHARED = 0x0020;

	public enum MipmapControl {
		MIPMAP_NONE(0),
		MIPMAP_FULL(1),
		MIPMAP_ON_SYNC_TO_TEXTURE(2);

		int mID;

		MipmapControl(int id) {
			mID = id;
		}
	}

	private final Type mType;
	final byte[] mData;

	private Allocation(RenderScript rs, Type type) {
		super(rs);
		mType = type;
		mData = new byte[type.getCount() * type.getElement().getBytesSize()];
	}

	public Type getType() {
		return mType;
	}

	public static Allocation createTyped(RenderScript rs, Type type, int usage) {
		if (type.getElement() == null) {
			throw new RSInvalidStateException("Bad Type");
		}
		return new Allocation(rs, type);
	}

	public static Allocation createTyped(RenderScript rs, Type type) {
		return createTyped(rs, type, USAGE_SCRIPT);
	}

	public static Allocation createFromBitmap(RenderScript rs, Bitmap b, MipmapControl mips, int usage) {
		Element e = b.getConfig() == Bitmap.Config.ALPHA_8 ? Element.U8(rs) : Element.RGBA_8888(rs);
		Type t = new Type.Builder(rs, e).setX(b.getWidth()).setY(b.getHeight()).create();
		Allocation a = new Allocation(rs, t);
		a.copyFrom(b);
		return a;
	}

	public static Allocation createFromBitmap(RenderScript rs, Bitmap b) {
		return createFromBitmap(rs, b, MipmapControl.MIPMAP_NONE, USAGE_GRAPHICS_TEXTURE);
	}

	private void validateBitmap(Bitmap b) {
		int bpp = b.getConfig() == Bitmap.Config.ALPHA_8 ? 1 : 4;
		if (b.getConfig() != Bitmap.Config.ALPHA_8 && b.getConfig() != Bitmap.Config.ARGB_8888) {
			throw new RSIllegalArgumentException("Unsupported bitmap format " + b.getConfig());
		}
		if (mType.getX() != b.getWidth() || mType.getY() != b.getHeight()
		    || mType.getElement().getBytesSize() != bpp) {
			throw new RSIllegalArgumentException("Cannot copy a " + b.getWidth() + "x" + b.getHeight()
			                                     + " bitmap to a " + mType.getX() + "x" + mType.getY() + " Allocation.");
		}
	}

	public void copyFrom(Bitmap b) {
		checkValid();
		validateBitmap(b);
		final int n = b.getWidth() * b.getHeight();
		final int[] px = new int[n];
		b.getPixels(px, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
		if (b.getConfig() == Bitmap.Config.ALPHA_8) {
			for (int i = 0; i < n; i++)
				mData[i] = (byte)(px[i] >>> 24);
		} else {
			for (int i = 0; i < n; i++) {
				mData[4 * i] = (byte)(px[i] >> 16);
				mData[4 * i + 1] = (byte)(px[i] >> 8);
				mData[4 * i + 2] = (byte)px[i];
				mData[4 * i + 3] = (byte)(px[i] >>> 24);
			}
		}
	}

	public void copyTo(Bitmap b) {
		checkValid();
		validateBitmap(b);
		final int n = b.getWidth() * b.getHeight();
		final int[] px = new int[n];
		if (b.getConfig() == Bitmap.Config.ALPHA_8) {
			for (int i = 0; i < n; i++)
				px[i] = (mData[i] & 0xff) << 24;
		} else {
			for (int i = 0; i < n; i++) {
				px[i] = ((mData[4 * i + 3] & 0xff) << 24) | ((mData[4 * i] & 0xff) << 16)
				        | ((mData[4 * i + 1] & 0xff) << 8) | (mData[4 * i + 2] & 0xff);
			}
		}
		b.setPixels(px, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
	}

	public void copyFrom(byte[] d) {
		checkValid();
		if (d.length != mData.length) {
			throw new RSIllegalArgumentException("Array too small for allocation type.");
		}
		System.arraycopy(d, 0, mData, 0, d.length);
	}

	public void copyTo(byte[] d) {
		checkValid();
		if (d.length < mData.length) {
			throw new RSIllegalArgumentException("Array too small for allocation type.");
		}
		System.arraycopy(mData, 0, d, 0, mData.length);
	}
}
