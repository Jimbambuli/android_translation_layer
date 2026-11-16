package android.media;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.ImageFormat;
import android.hardware.DataSpace;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/* AOSP frameworks/base/core/java/android/media/ImageReader.java: the argument checks, the formats and the state. ATL has no
 * BufferQueue whose producer could fill the images (camera, MediaProjection and VirtualDisplay are not backed), so like
 * the native reader of a Surface nothing writes to, acquireNextImage() has no buffer (null) and the listener is never
 * called. */
public class ImageReader implements AutoCloseable {
	private final int mWidth;
	private final int mHeight;
	private final int mFormat;
	private final long mUsage;
	private final int mMaxImages;
	private final int mHardwareBufferFormat;
	private final int mDataSpace;
	private Surface mSurface;
	private boolean mIsReaderValid;
	private OnImageAvailableListener mListener;
	private Executor mListenerExecutor;
	private final CopyOnWriteArrayList<Image> mAcquiredImages = new CopyOnWriteArrayList<>();
	private final Object mListenerLock = new Object();
	private final Object mCloseLock = new Object();

	public static @NonNull ImageReader newInstance(int width, int height, int format, int maxImages) {
		return new ImageReader(width, height, format, maxImages, format == ImageFormat.PRIVATE ? 0 : HardwareBuffer.USAGE_CPU_READ_OFTEN);
	}

	public static @NonNull ImageReader newInstance(int width, int height, int format, int maxImages, long usage) {
		return new ImageReader(width, height, format, maxImages, usage);
	}

	/* AOSP PublicFormatUtils.getHalFormat() */
	private static int getHalFormat(int imageFormat) {
		switch (imageFormat) {
			case ImageFormat.JPEG:
			case ImageFormat.DEPTH_POINT_CLOUD:
			case ImageFormat.DEPTH_JPEG:
			case ImageFormat.HEIC:
			case ImageFormat.JPEG_R:
				return HardwareBuffer.BLOB;
			case ImageFormat.YUV_420_888:
				return 0x23;
			case ImageFormat.RAW_PRIVATE:
				return 0x24;
			default:
				return imageFormat;
		}
	}

	/* AOSP PublicFormatUtils.getHalDataspace() */
	private static int getHalDataspace(int imageFormat) {
		switch (imageFormat) {
			case ImageFormat.JPEG:
				return DataSpace.DATASPACE_JFIF;
			case ImageFormat.DEPTH_POINT_CLOUD:
			case ImageFormat.DEPTH16:
			case ImageFormat.RAW_DEPTH:
			case ImageFormat.RAW_DEPTH10:
				return DataSpace.DATASPACE_DEPTH;
			case ImageFormat.DEPTH_JPEG:
				return DataSpace.DATASPACE_DYNAMIC_DEPTH;
			case ImageFormat.HEIC:
				return DataSpace.DATASPACE_HEIF;
			default:
				return DataSpace.DATASPACE_UNKNOWN;
		}
	}

	/* AOSP PublicFormatUtils.getPublicFormat() */
	private static int getPublicFormat(int hardwareBufferFormat, int dataSpace) {
		switch (hardwareBufferFormat) {
			case HardwareBuffer.BLOB:
				switch (dataSpace) {
					case DataSpace.DATASPACE_JFIF:
						return ImageFormat.JPEG;
					case DataSpace.DATASPACE_DEPTH:
						return ImageFormat.DEPTH_POINT_CLOUD;
					case DataSpace.DATASPACE_DYNAMIC_DEPTH:
						return ImageFormat.DEPTH_JPEG;
					case DataSpace.DATASPACE_HEIF:
						return ImageFormat.HEIC;
					default:
						return hardwareBufferFormat;
				}
			case 0x23:
				return ImageFormat.YUV_420_888;
			case 0x24:
				return ImageFormat.RAW_PRIVATE;
			default:
				return hardwareBufferFormat;
		}
	}

	private void initializeImageReader(int width, int height, int imageFormat, int maxImages) {
		if (width < 1 || height < 1) {
			throw new IllegalArgumentException("The image dimensions must be positive");
		}
		if (maxImages < 1) {
			throw new IllegalArgumentException("Maximum outstanding image count must be at least 1");
		}
		if (imageFormat == ImageFormat.NV21) {
			throw new IllegalArgumentException("NV21 format is not supported");
		}
		mIsReaderValid = true;
		mSurface = new Surface();
	}

	private ImageReader(int width, int height, int imageFormat, int maxImages, long usage) {
		mWidth = width;
		mHeight = height;
		mFormat = imageFormat;
		mUsage = usage;
		mMaxImages = maxImages;
		mHardwareBufferFormat = getHalFormat(mFormat);
		mDataSpace = getHalDataspace(mFormat);
		initializeImageReader(width, height, imageFormat, maxImages);
	}

	private ImageReader(int width, int height, int maxImages, long usage, int hardwareBufferFormat, int dataSpace) {
		mWidth = width;
		mHeight = height;
		mUsage = usage;
		mMaxImages = maxImages;
		mHardwareBufferFormat = hardwareBufferFormat;
		mDataSpace = dataSpace;
		mFormat = getPublicFormat(hardwareBufferFormat, dataSpace);
		initializeImageReader(width, height, mFormat, maxImages);
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public int getImageFormat() {
		return mFormat;
	}

	public int getHardwareBufferFormat() {
		return mHardwareBufferFormat;
	}

	public int getDataSpace() {
		return mDataSpace;
	}

	public int getMaxImages() {
		return mMaxImages;
	}

	public long getUsage() {
		return mUsage;
	}

	public Surface getSurface() {
		return mSurface;
	}

	public Image acquireLatestImage() {
		return acquireNextImage();
	}

	/* AOSP: nativeImageSetup() reports ACQUIRE_NO_BUFS when the producer queued nothing (null), ACQUIRE_MAX_IMAGES
	 * when maxImages are acquired (IllegalStateException); ATL's reader never gets a buffer */
	public Image acquireNextImage() {
		return null;
	}

	public Image acquireNextImageNoThrowISE() {
		return null;
	}

	public void setOnImageAvailableListener(OnImageAvailableListener listener, Handler handler) {
		synchronized (mListenerLock) {
			if (listener != null) {
				Looper looper = handler != null ? handler.getLooper() : Looper.myLooper();
				if (looper == null) {
					throw new IllegalArgumentException("handler is null but the current thread is not a looper");
				}
				Handler h = handler != null ? handler : new Handler(looper);
				mListenerExecutor = h::post;
			} else {
				mListenerExecutor = null;
			}
			mListener = listener;
		}
	}

	public void setOnImageAvailableListenerWithExecutor(@NonNull OnImageAvailableListener listener, @NonNull Executor executor) {
		if (executor == null) {
			throw new IllegalArgumentException("executor must not be null");
		}
		synchronized (mListenerLock) {
			mListenerExecutor = executor;
			mListener = listener;
		}
	}

	public void close() {
		setOnImageAvailableListener(null, null);
		synchronized (mCloseLock) {
			mIsReaderValid = false;
			for (Image image : mAcquiredImages) {
				image.close();
			}
			mAcquiredImages.clear();
		}
	}

	public void discardFreeBuffers() {
		synchronized (mCloseLock) {
			/* AOSP: nativeDiscardFreeBuffers(); the reader has no free buffers */
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

	public interface OnImageAvailableListener {
		void onImageAvailable(ImageReader reader);
	}

	public static final class Builder {
		private int mWidth;
		private int mHeight;
		private int mMaxImages = 1;
		private int mImageFormat = ImageFormat.UNKNOWN;
		private int mHardwareBufferFormat = HardwareBuffer.RGBA_8888;
		private int mDataSpace = DataSpace.DATASPACE_UNKNOWN;
		private long mUsage = HardwareBuffer.USAGE_CPU_READ_OFTEN;
		private boolean mUseLegacyImageFormat = false;

		public Builder(int width, int height) {
			mWidth = width;
			mHeight = height;
		}

		public @NonNull Builder setMaxImages(int maxImages) {
			mMaxImages = maxImages;
			return this;
		}

		public @NonNull Builder setUsage(long usage) {
			mUsage = usage;
			return this;
		}

		public @NonNull Builder setImageFormat(int imageFormat) {
			mImageFormat = imageFormat;
			mUseLegacyImageFormat = true;
			mHardwareBufferFormat = HardwareBuffer.RGBA_8888;
			mDataSpace = DataSpace.DATASPACE_UNKNOWN;
			return this;
		}

		public @NonNull Builder setDefaultHardwareBufferFormat(int hardwareBufferFormat) {
			mHardwareBufferFormat = hardwareBufferFormat;
			mUseLegacyImageFormat = false;
			mImageFormat = ImageFormat.UNKNOWN;
			return this;
		}

		public @NonNull Builder setDefaultDataSpace(int dataSpace) {
			mDataSpace = dataSpace;
			mUseLegacyImageFormat = false;
			mImageFormat = ImageFormat.UNKNOWN;
			return this;
		}

		public @NonNull ImageReader build() {
			if (mUseLegacyImageFormat) {
				return new ImageReader(mWidth, mHeight, mImageFormat, mMaxImages, mUsage);
			} else {
				return new ImageReader(mWidth, mHeight, mMaxImages, mUsage, mHardwareBufferFormat, mDataSpace);
			}
		}
	}
}
