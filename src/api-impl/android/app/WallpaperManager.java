package android.app;

import android.content.Context;
import android.graphics.Bitmap;

public class WallpaperManager {
	public static final int FLAG_SYSTEM = 1 << 0;
	public static final int FLAG_LOCK = 1 << 1;

	public static WallpaperManager getInstance(Context context) {
		return new WallpaperManager();
	}

	public void setBitmap(Bitmap bitmap) {
		set_bitmap(bitmap.getTexture());
	}

	/**
	 * Get the primary colors of a wallpaper.
	 *
	 * @param which wallpaper type. Must be either {@link #FLAG_SYSTEM} or
	 *     {@link #FLAG_LOCK}
	 * @return Current {@link WallpaperColors} or null if colors are unknown.
	 */
	public WallpaperColors getWallpaperColors(int which) {
		if (which != FLAG_SYSTEM && which != FLAG_LOCK)
			throw new IllegalArgumentException("Must specify exactly one kind of wallpaper");
		/* like AOSP when the wallpaper service can't tell */
		return null;
	}

	private static native void set_bitmap(long texture);
}
