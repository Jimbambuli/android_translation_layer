package android.graphics;

public class BitmapShader extends Shader {

	/* AOSP's hidden mBitmap; android.atl.GskCanvas draws it */
	public Bitmap bitmap;

	public BitmapShader(Bitmap bitmap, TileMode tileX, TileMode tileY) {
		this.bitmap = bitmap;
	}
}
