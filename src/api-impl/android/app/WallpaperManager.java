package android.app;

import android.content.Context;
import android.graphics.Bitmap;

public class WallpaperManager {
	public static final int FLAG_SYSTEM = 1 << 0;
	public static final int FLAG_LOCK = 1 << 1;

	public static WallpaperManager getInstance(Context context) {
		return new WallpaperManager();
	}

	public void setBitmap(Bitmap bitmap) throws java.io.IOException {
		setBitmap(bitmap, null, true);
	}

	public int setBitmap(Bitmap fullImage, android.graphics.Rect visibleCropHint, boolean allowBackup) throws java.io.IOException {
		return setBitmap(fullImage, visibleCropHint, allowBackup, FLAG_SYSTEM | FLAG_LOCK);
	}

	/* AOSP returns the id of the new wallpaper (0 on failure); ATL has the one wallpaper of the desktop and no lock
	 * screen, so which is not looked at, and the crop hint is not applied by the native side */
	public int setBitmap(Bitmap fullImage, android.graphics.Rect visibleCropHint, boolean allowBackup, int which) throws java.io.IOException {
		if (fullImage == null)
			throw new IllegalArgumentException("Bitmap cannot be null");
		set_bitmap(fullImage.getTexture());
		return 1;
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

	/* AOSP WallpaperManager.OnColorsChangedListener: told when the colors of the wallpaper change */
	public interface OnColorsChangedListener {
		void onColorsChanged(WallpaperColors colors, int which);
	}

	/* AOSP keeps the listeners and calls them when WallpaperManagerService reports new colors; ATL has no such
	 * service, getWallpaperColors() stays null and the colors never change */
	public void addOnColorsChangedListener(OnColorsChangedListener listener, android.os.Handler handler) {}

	public void removeOnColorsChangedListener(OnColorsChangedListener callback) {}

	/* AOSP DisabledWallpaperManager (what SystemServiceRegistry hands out on a device without a wallpaper service):
	 * nothing can be read back, desired size is -1, clear() does nothing. Unlike DisabledWallpaperManager,
	 * isWallpaperSupported()/isSetWallpaperAllowed() say true because ATL's setBitmap() does set the wallpaper. */
	public android.graphics.drawable.Drawable getDrawable() {
		return getDrawable(FLAG_SYSTEM);
	}

	public android.graphics.drawable.Drawable getDrawable(int which) {
		return null;
	}

	public int getDesiredMinimumWidth() {
		return -1;
	}

	public int getDesiredMinimumHeight() {
		return -1;
	}

	public boolean isWallpaperSupported() {
		return true;
	}

	public boolean isSetWallpaperAllowed() {
		return true;
	}

	public void clear() throws java.io.IOException {}

	public void clear(int which) throws java.io.IOException {}

	public void setStream(java.io.InputStream bitmapData) throws java.io.IOException {
		setStream(bitmapData, null, true);
	}

	public int setStream(java.io.InputStream bitmapData, android.graphics.Rect visibleCropHint, boolean allowBackup)
	    throws java.io.IOException {
		return setStream(bitmapData, visibleCropHint, allowBackup, FLAG_SYSTEM | FLAG_LOCK);
	}

	/* DisabledWallpaperManager.setStream(): unsupported(), returns 0 */
	public int setStream(java.io.InputStream bitmapData, android.graphics.Rect visibleCropHint, boolean allowBackup, int which)
	    throws java.io.IOException {
		return 0;
	}

	private static native void set_bitmap(long texture);
}
