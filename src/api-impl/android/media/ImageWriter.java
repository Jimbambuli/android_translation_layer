package android.media;

import android.annotation.NonNull;
import android.graphics.ImageFormat;
import android.hardware.DataSpace;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* AOSP frameworks/base/core/java/android/media/ImageWriter.java: the argument checks, the formats and the state. ATL has no
 * BufferQueue behind a Surface that a consumer reads, so dequeueInputImage() finds no buffer: AOSP's native
 * dequeue then fails with an IllegalStateException, and no image can be queued. */
public class ImageWriter implements AutoCloseable {
	private final Object mListenerLock = new Object();
	private OnImageReleasedListener mListener;
	private final Object mCloseLock = new Object();
	private boolean mIsWriterValid = false;
	private int mWidth;
	private int mHeight;
	private final int mMaxImages;
	private long mUsage = HardwareBuffer.USAGE_CPU_WRITE_OFTEN;
	private int mHardwareBufferFormat;
	private int mDataSpace;
	private int mWriterFormat;
	private List<Image> mDequeuedImages = new CopyOnWriteArrayList<>();

	public static @NonNull ImageWriter newInstance(@NonNull Surface surface, int maxImages) {
		return new ImageWriter(surface, maxImages, true, ImageFormat.UNKNOWN, -1, -1, HardwareBuffer.USAGE_CPU_WRITE_OFTEN);
	}

	public static @NonNull ImageWriter newInstance(@NonNull Surface surface, int maxImages, int format) {
		checkFormat(format);
		return new ImageWriter(surface, maxImages, false, format, -1, -1, HardwareBuffer.USAGE_CPU_WRITE_OFTEN);
	}

	public static @NonNull ImageWriter newInstance(@NonNull Surface surface, int maxImages, int format, int width, int height) {
		checkFormat(format);
		return new ImageWriter(surface, maxImages, false, format, width, height, HardwareBuffer.USAGE_CPU_WRITE_OFTEN);
	}

	/* AOSP: ImageFormat.isPublicFormat() or PixelFormat.isPublicFormat() */
	private static void checkFormat(int format) {
		boolean pixelFormat = format >= 1 && format <= 4 || format == 0x16 || format == 0x2b;
		if (!ImageFormat.isPublicFormat(format) && !pixelFormat) {
			throw new IllegalArgumentException("Invalid format is specified: " + format);
		}
	}

	private ImageWriter(Surface surface, int maxImages, boolean useSurfaceImageFormatInfo, int imageFormat, int width, int height, long usage) {
		mMaxImages = maxImages;
		mUsage = usage;
		if (!useSurfaceImageFormatInfo) {
			mWriterFormat = imageFormat;
			/* AOSP PublicFormatUtils.getHalFormat() / getHalDataspace() as in ImageReader */
			mHardwareBufferFormat = imageFormat == ImageFormat.JPEG ? HardwareBuffer.BLOB : imageFormat == ImageFormat.YUV_420_888 ? 0x23 : imageFormat;
			mDataSpace = imageFormat == ImageFormat.JPEG ? DataSpace.DATASPACE_JFIF : DataSpace.DATASPACE_UNKNOWN;
		}
		if (surface == null || maxImages < 1) {
			throw new IllegalArgumentException("Illegal input argument: surface " + surface + ", maxImages: " + maxImages);
		}
		/* the size of the surface is not known in ATL: the given one, else none */
		mWidth = width == -1 ? 0 : width;
		mHeight = height == -1 ? 0 : height;
		mIsWriterValid = true;
	}

	public int getMaxImages() {
		return mMaxImages;
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public Image dequeueInputImage() {
		if (mDequeuedImages.size() >= mMaxImages) {
			throw new IllegalStateException("Already dequeued max number of Images " + mMaxImages);
		}
		throw new IllegalStateException("Failed to dequeue an input image, the surface has no buffer queue");
	}

	public void queueInputImage(Image image) {
		if (image == null) {
			throw new IllegalArgumentException("image shouldn't be null");
		}
		throw new IllegalArgumentException("Only images from ImageReader can be queued to ImageWriter, other image source is not supported yet!");
	}

	public int getFormat() {
		return mWriterFormat;
	}

	public long getUsage() {
		return mUsage;
	}

	public int getHardwareBufferFormat() {
		return mHardwareBufferFormat;
	}

	public int getDataSpace() {
		return mDataSpace;
	}

	public interface OnImageReleasedListener {
		void onImageReleased(ImageWriter writer);
	}

	public void setOnImageReleasedListener(OnImageReleasedListener listener, Handler handler) {
		synchronized (mListenerLock) {
			if (listener != null) {
				Looper looper = handler != null ? handler.getLooper() : Looper.myLooper();
				if (looper == null) {
					throw new IllegalArgumentException("handler is null but the current thread is not a looper");
				}
				mListener = listener;
			} else {
				mListener = null;
			}
		}
	}

	@Override
	public void close() {
		setOnImageReleasedListener(null, null);
		synchronized (mCloseLock) {
			if (!mIsWriterValid) {
				return;
			}
			for (Image image : mDequeuedImages) {
				image.close();
			}
			mDequeuedImages.clear();
			mIsWriterValid = false;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}

	public static final class Builder {
		private Surface mSurface;
		private int mWidth = -1;
		private int mHeight = -1;
		private int mMaxImages = 1;
		private int mImageFormat = ImageFormat.UNKNOWN;
		private long mUsage = -1;
		private int mHardwareBufferFormat = HardwareBuffer.RGBA_8888;
		private int mDataSpace = DataSpace.DATASPACE_UNKNOWN;
		private boolean mUseSurfaceImageFormatInfo = true;
		private boolean mUseLegacyImageFormat = false;

		public Builder(@NonNull Surface surface) {
			mSurface = surface;
		}

		public @NonNull Builder setWidthAndHeight(int width, int height) {
			mWidth = width;
			mHeight = height;
			return this;
		}

		public @NonNull Builder setMaxImages(int maxImages) {
			mMaxImages = maxImages;
			return this;
		}

		public @NonNull Builder setImageFormat(int imageFormat) {
			checkFormat(imageFormat);
			mImageFormat = imageFormat;
			mUseLegacyImageFormat = true;
			mHardwareBufferFormat = HardwareBuffer.RGBA_8888;
			mDataSpace = DataSpace.DATASPACE_UNKNOWN;
			mUseSurfaceImageFormatInfo = false;
			return this;
		}

		public @NonNull Builder setHardwareBufferFormat(int hardwareBufferFormat) {
			mHardwareBufferFormat = hardwareBufferFormat;
			mImageFormat = ImageFormat.UNKNOWN;
			mUseLegacyImageFormat = false;
			mUseSurfaceImageFormatInfo = false;
			return this;
		}

		public @NonNull Builder setDataSpace(int dataSpace) {
			mDataSpace = dataSpace;
			mImageFormat = ImageFormat.UNKNOWN;
			mUseLegacyImageFormat = false;
			mUseSurfaceImageFormatInfo = false;
			return this;
		}

		public @NonNull Builder setUsage(long usage) {
			mUsage = usage;
			return this;
		}

		public @NonNull ImageWriter build() {
			long usage = mUsage == -1 ? HardwareBuffer.USAGE_CPU_WRITE_OFTEN : mUsage;
			ImageWriter writer = new ImageWriter(mSurface, mMaxImages, mUseSurfaceImageFormatInfo, mImageFormat, mWidth, mHeight, usage);
			if (!mUseLegacyImageFormat && !mUseSurfaceImageFormatInfo) {
				writer.mHardwareBufferFormat = mHardwareBufferFormat;
				writer.mDataSpace = mDataSpace;
			}
			return writer;
		}
	}
}
