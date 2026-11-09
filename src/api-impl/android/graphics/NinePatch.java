package android.graphics;

import android.graphics.drawable.NinePatchDrawable;

/**
 * The NinePatch class permits drawing a bitmap in nine or more sections. Essentially, it allows the image to
 * specify which sections of the bitmap can be stretched and which sections cannot, as AOSP's NinePatch. The
 * drawing is done by ATL's NinePatchDrawable (the chunk of the bitmap goes to the native 9-patch paintable).
 */
public class NinePatch {
	private final Bitmap mBitmap;
	private final byte[] mChunk;
	private Paint mPaint;
	private String mSrcName;
	private NinePatchDrawable mDrawable;

	public NinePatch(Bitmap bitmap, byte[] chunk) {
		this(bitmap, chunk, null);
	}

	public NinePatch(Bitmap bitmap, byte[] chunk, String srcName) {
		mBitmap = bitmap;
		mChunk = chunk;
		mSrcName = srcName;
	}

	public NinePatch(NinePatch patch) {
		mBitmap = patch.mBitmap;
		mChunk = patch.mChunk;
		mSrcName = patch.mSrcName;
		if (patch.mPaint != null)
			mPaint = new Paint(patch.mPaint);
	}

	/** Validates the 9-patch chunk (the serialized Res_png_9patch header, 32 bytes, followed by its arrays) */
	public static boolean isNinePatchChunk(byte[] chunk) {
		if (chunk == null || chunk.length < 32)
			return false;
		int divs = (chunk[1] & 0xff) + (chunk[2] & 0xff);
		int colors = chunk[3] & 0xff;
		return chunk[0] != 0 && chunk.length >= 32 + 4 * (divs + colors);
	}

	public void setPaint(Paint p) {
		mPaint = p;
	}

	public Paint getPaint() {
		return mPaint;
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}

	public void draw(Canvas canvas, RectF location) {
		canvas.drawPatch(this, location, mPaint);
	}

	public void draw(Canvas canvas, Rect location) {
		canvas.drawPatch(this, location, mPaint);
	}

	public void draw(Canvas canvas, Rect location, Paint paint) {
		canvas.drawPatch(this, location, paint);
	}

	public int getDensity() {
		return mBitmap.getDensity();
	}

	public int getWidth() {
		return mBitmap.getWidth();
	}

	public int getHeight() {
		return mBitmap.getHeight();
	}

	public boolean hasAlpha() {
		return mBitmap.hasAlpha();
	}

	/** the Drawable that shows this patch (internal) */
	NinePatchDrawable getDrawable() {
		if (mDrawable == null)
			mDrawable = new NinePatchDrawable(null, mBitmap, mChunk, null, mSrcName);
		return mDrawable;
	}
}
