package android.graphics;

public class BitmapShader extends Shader {

	/* AOSP's hidden mBitmap; android.atl.GskCanvas draws it */
	public Bitmap bitmap;
	/* AOSP's private mTileX/mTileY; android.atl.GskCanvas repeats, mirrors or extends the bitmap with them */
	public final TileMode tileX;
	public final TileMode tileY;

	/* AOSP BitmapShader.java: filter mode and anisotropy are kept; android.atl.GskCanvas draws with the paint's filter */
	public static final int FILTER_MODE_DEFAULT = 0;
	public static final int FILTER_MODE_NEAREST = 1;
	public static final int FILTER_MODE_LINEAR = 2;

	private int mFilterMode = FILTER_MODE_DEFAULT;
	private int mMaxAniso = 0;

	public int getFilterMode() {
		return mFilterMode;
	}

	public void setFilterMode(int mode) {
		mFilterMode = mode;
	}

	public void setMaxAnisotropy(int maxAnisotropy) {
		if (mMaxAniso != maxAnisotropy && maxAnisotropy > 0) {
			mMaxAniso = maxAnisotropy;
			mFilterMode = FILTER_MODE_DEFAULT;
		}
	}

	public int getMaxAnisotropy() {
		return mMaxAniso;
	}

	public BitmapShader(Bitmap bitmap, TileMode tileX, TileMode tileY) {
		if (bitmap == null) {
			throw new IllegalArgumentException("Bitmap must be non-null");
		}
		this.bitmap = bitmap;
		this.tileX = tileX;
		this.tileY = tileY;
	}
}
