package android.graphics;

import android.atl.annotation.Export;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;

/*
 * Bitmap is implemented as GdkTexture or GtkSnapshot. It can only be one of the two at a time.
 * The methods getTexture() and getSnapshot() automatically convert between the two as needed.
 */
public final class Bitmap implements Parcelable {

	public static final int DENSITY_NONE = 0;

	public enum Config {
		RGB_565(2, -1, /*ANDROID_BITMAP_FORMAT_RGB_565*/ 4),
		ARGB_8888(4, /*GDK_MEMORY_R8G8B8A8*/ 5, /**ANDROID_BITMAP_FORMAT_RGBA_8888*/ 1),
		ARGB_4444(2, -1, /*ANDROID_BITMAP_FORMAT_RGBA_4444*/ 7),
		ALPHA_8(1, /*GDK_MEMORY_A8*/ 24, /*ANDROID_BITMAP_FORMAT_A_8*/ 8),
		RGBA_F16(8, /*GDK_MEMORY_R16G16B16A16_FLOAT*/ 14, /*ANDROID_BITMAP_FORMAT_RGBA_F16*/ 9),
		HARDWARE(4, /*GDK_MEMORY_R8G8B8A8*/ 5, /*ANDROID_BITMAP_FORMAT_RGBA_8888*/ 1);

		private int bytes_per_pixel;
		private int gdk_memory_format;
		int android_memory_format; // used by native function AndroidBitmap_getInfo()

		private Config(int bytes_per_pixel, int gdk_memory_format, int android_memory_format) {
			this.bytes_per_pixel = bytes_per_pixel;
			this.gdk_memory_format = gdk_memory_format;
			this.android_memory_format = android_memory_format;
		}
	}

	/**
	 * Specifies the known formats a bitmap can be compressed into
	 */
	public enum CompressFormat {
		/**
		 * Compress to the JPEG format. {@code quality} of {@code 0} means
		 * compress for the smallest size. {@code 100} means compress for max
		 * visual quality.
		 */
		JPEG          (0),
		/**
		 * Compress to the PNG format. PNG is lossless, so {@code quality} is
		 * ignored.
		 */
		PNG           (1),
		/**
		 * Compress to the WEBP format. {@code quality} of {@code 0} means
		 * compress for the smallest size. {@code 100} means compress for max
		 * visual quality. As of {@link android.os.Build.VERSION_CODES#Q}, a
		 * value of {@code 100} results in a file in the lossless WEBP format.
		 * Otherwise the file will be in the lossy WEBP format.
		 *
		 * @deprecated in favor of the more explicit
		 *             {@link CompressFormat#WEBP_LOSSY} and
		 *             {@link CompressFormat#WEBP_LOSSLESS}.
		 */
		@Deprecated
		WEBP          (2),
		/**
		 * Compress to the WEBP lossy format. {@code quality} of {@code 0} means
		 * compress for the smallest size. {@code 100} means compress for max
		 * visual quality.
		 */
		WEBP_LOSSY    (3),
		/**
		 * Compress to the WEBP lossless format. {@code quality} refers to how
		 * much effort to put into compression. A value of {@code 0} means to
		 * compress quickly, resulting in a relatively large file size.
		 * {@code 100} means to spend more time compressing, resulting in a
		 * smaller file.
		 */
		WEBP_LOSSLESS (4);

		CompressFormat(int nativeInt) {
			this.nativeInt = nativeInt;
		}
		final int nativeInt;
	}

	private int width;
	private int height;
	private int stride;
	private long texture;
	private long snapshot;
	private Config config = Config.ARGB_8888;
	private boolean hasAlpha = true;
	long bytes = 0; // used by native function AndroidBitmap_lockPixels()
	private boolean recycled = false;
	boolean mutable = true;
	private int density = DisplayMetrics.DENSITY_DEVICE;

	Bitmap(long texture) {
		this(native_get_width(texture), native_get_height(texture), Config.ARGB_8888);
		this.texture = texture;
	}

	private Bitmap(int width, int height, Config config) {
		this.config = config;
		this.width = width;
		this.height = height;
		int stride = width * config.bytes_per_pixel;
		this.stride = (stride + 3) & ~3; // 4-byte alignment
	}

	public static Bitmap createBitmap(int width, int height, Config config) {
		return new Bitmap(width, height, config);
	}

	public static Bitmap createBitmap(DisplayMetrics metrics, int width, int height, Config config) {
		return new Bitmap(width, height, config);
	}

	public static Bitmap createBitmap(DisplayMetrics metrics, int width, int height, Config config, boolean hasAlpha, ColorSpace colorSpace) {
		Bitmap bitmap = new Bitmap(width, height, config);
		bitmap.hasAlpha = hasAlpha;
		return bitmap;
	}

	public static Bitmap createBitmap(Bitmap src, int x, int y, int width, int height) {
		Bitmap dest = new Bitmap(width, height, src.getConfig());
		new Canvas(dest).drawBitmap(src, new Rect(x, y, x + width, y + height), new Rect(0, 0, width, height), null);
		return dest;
	}

	public static Bitmap createBitmap(Bitmap src, int x, int y, int width, int height, Matrix matrix, boolean filter) {
		Bitmap dest = new Bitmap(width, height, src.getConfig());
		Canvas canvas = new Canvas(dest);
		canvas.concat(matrix);
		canvas.drawBitmap(src, new Rect(x, y, x + width, y + height), new Rect(0, 0, width, height), null);
		return dest;
	}

	public static Bitmap createBitmap(int[] colors, int width, int height, Config config) {
		return createBitmap(width, height, config);
	}

	public static Bitmap createBitmap(Bitmap src) {
		return new Bitmap(native_ref_texture(src.getTexture()));
	}

	public static Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
		Bitmap dest = new Bitmap(dstWidth, dstHeight, src.getConfig());
		new Canvas(dest).drawBitmap(src, new Rect(0, 0, src.getWidth(), src.getHeight()), new Rect(0, 0, dstWidth, dstHeight), null);
		return dest;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Config getConfig() {
		return config;
	}

	@Export
	public synchronized long getTexture() {
		if (texture == 0) {
			texture = native_create_texture(snapshot, width, height, stride, config.gdk_memory_format);
			snapshot = 0;
		}
		return texture;
	}

	synchronized long getSnapshot() {
		if (snapshot == 0) {
			snapshot = native_create_snapshot(texture);
			texture = 0;
		}
		return snapshot;
	}

	public void eraseColor(int color) {
		if (color == Color.TRANSPARENT) {
			native_recycle(texture, snapshot);
			snapshot = native_erase_color(color, width, height);
			texture = 0;
		} else {
			Paint paint = new Paint();
			paint.setColor(color);
			new Canvas(this).drawRect(0, 0, width, height, paint);
		}
	}

	@Export
	public void recycle() {
		native_recycle(texture, snapshot);
		texture = 0;
		snapshot = 0;
		recycled = true;
	}

	public int getRowBytes() {
		return stride;
	}

	// used by BitmapFactory to satify Options.inBitmap if set
	void transferFrom(Bitmap src) {
		native_recycle(this.texture, this.snapshot);
		this.texture = src.texture;
		this.snapshot = src.snapshot;
		this.width = src.width;
		this.height = src.height;
		this.stride = src.stride;
		this.config = src.config;
		this.hasAlpha = src.hasAlpha;
		this.mutable = src.mutable;
		this.recycled = false;
		src.texture = 0;
		src.snapshot = 0;
		src.recycled = true;
	}

	public int getAllocationByteCount() {
		return height * getRowBytes();
	}

	public void prepareToDraw() {
		getTexture();
	}

	/* the density the pixels were made for; drawing scales the bitmap by it as in AOSP */
	public int getDensity() {
		return density;
	}

	public void setDensity(int density) {
		this.density = density;
	}

	public int getScaledWidth(int targetDensity) {
		return scaleFromDensity(width, density, targetDensity);
	}

	public int getScaledHeight(int targetDensity) {
		return scaleFromDensity(height, density, targetDensity);
	}

	public int getScaledWidth(DisplayMetrics metrics) {
		return scaleFromDensity(width, density, metrics.densityDpi);
	}

	public int getScaledHeight(DisplayMetrics metrics) {
		return scaleFromDensity(height, density, metrics.densityDpi);
	}

	public int getScaledWidth(Canvas canvas) {
		return scaleFromDensity(width, density, canvas.getDensity());
	}

	public int getScaledHeight(Canvas canvas) {
		return scaleFromDensity(height, density, canvas.getDensity());
	}

	public static int scaleFromDensity(int size, int sdensity, int tdensity) {
		if (sdensity == DENSITY_NONE || tdensity == DENSITY_NONE || sdensity == tdensity)
			return size;
		// scale by tdensity / sdensity, rounding up
		return ((size * tdensity) + (sdensity >> 1)) / sdensity;
	}

	public boolean isRecycled() {
		return recycled;
	}

	public void setHasAlpha(boolean hasAlpha) {
		this.hasAlpha = hasAlpha;
	}

	public boolean hasAlpha() {
		return hasAlpha;
	}

	/* AOSP keeps the flag in the native bitmap; nothing here uses mip maps, but BitmapDrawable reads and sets it */
	private boolean hasMipMap;

	public final boolean hasMipMap() {
		return hasMipMap;
	}

	public final void setHasMipMap(boolean hasMipMap) {
		this.hasMipMap = hasMipMap;
	}

	public Bitmap copy(Bitmap.Config config, boolean isMutable) {
		Bitmap bitmap = new Bitmap(width, height, config);
		bitmap.texture = native_ref_texture(getTexture());
		return bitmap;
	}

	public void getPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {
		native_get_pixels(getTexture(), pixels, offset, stride, x, y, width, height);
	}

	public int getPixel(int x, int y) {
		int[] pixels = new int[1];
		native_get_pixels(getTexture(), pixels, 0, 1, x, y, 1, 1);
		return pixels[0];
	}

	public void copyPixelsToBuffer(Buffer buffer) {
		if (config.gdk_memory_format == -1) {
			System.out.println("copyPixelsToBuffer: format " + config.name() + " not implemented");
			System.exit(1);
		}
		native_copy_to_buffer(getTexture(), buffer, config.gdk_memory_format, getRowBytes());
		buffer.position(buffer.position() + getAllocationByteCount());
	}

	public int getByteCount() {
		return getAllocationByteCount();
	}

	public boolean isMutable() {
		return mutable;
	}

	public boolean compress(Bitmap.CompressFormat format, int quality, OutputStream stream) {
		if (recycled) {
			throw new IllegalStateException("Can't compress a recycled bitmap");
		}
		// do explicit check before calling the native method
		if (stream == null) {
			throw new NullPointerException();
		}
		if (quality < 0 || quality > 100) {
			throw new IllegalArgumentException("quality must be 0..100");
		}
		/* AOSP's nativeCompress() writes to the stream and returns false when that fails */
		byte[] data = native_compress(getTexture(), format.nativeInt, quality);
		if (data == null)
			return false;
		try {
			stream.write(data);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public void setPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {
		native_set_pixels(getSnapshot(), pixels, offset, stride, x, y, width, height);
	}

	public void setPixel(int x, int y, int color) {
		int[] pixels = new int[] {color};
		native_set_pixels(getSnapshot(), pixels, 0, 1, x, y, 1, 1);
	}

	public void reconfigure(int width, int height, Bitmap.Config config) {}

	public void setPremultiplied(boolean premultiplied) {}

	public Bitmap extractAlpha() {
		return this.copy(config, mutable);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void finalize() throws Throwable {
		try {
			recycle();
		} finally {
			super.finalize();
		}
	}

	private static native long native_create_snapshot(long texture);
	private static native long native_create_texture(long snapshot, int width, int height, int stride, int format);
	private static native int native_get_width(long texture);
	private static native int native_get_height(long texture);
	private static native long native_erase_color(int color, int width, int height);
	private static native void native_recycle(long texture, long snapshot);
	private static native long native_ref_texture(long texture);
	private static native void native_get_pixels(long texture, int[] pixels, int offset, int stride, int x, int y, int width, int height);
	private static native void native_copy_to_buffer(long texture, Buffer buffer, int memory_format, int stride);
	private static native byte[] native_compress(long texture, int format, int quality);
	private static native void native_set_pixels(long snapshot, int[] pixels, int offset, int stride, int x, int y, int width, int height);
}
