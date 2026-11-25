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

/* AOSP frameworks/base/core/java/android/media/ImageReader.java: the argument checks, the formats and the state. In place
 * of the BufferQueue, the reader's Surface (Surface.atl_imageReader) takes the images a producer queues: the camera
 * (camera2.impl.CameraDeviceImpl), with YUV_420_888 (three planes, I420), JPEG (one plane) and PRIVATE (no planes)
 * images, Surface.lockCanvas() (RGBA), and MediaCodec, whose frames are AHardwareBuffers (queueHardwareBuffer()).
 * As a BufferQueue whose consumer holds all buffers, a full reader (maxImages acquired or queued) takes no more
 * images; the producer drops them. The NDK's AImageReader (libandroid image_reader.c) is this reader. */
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
		mSurface.atl_imageReader = this;
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

	/* AOSP acquireLatestImage(): the newest image, the older ones closed */
	public Image acquireLatestImage() {
		Image image = acquireNextImage();
		if (image == null) {
			return null;
		}
		try {
			for (;;) {
				Image next = acquireNextImageNoThrowISE();
				if (next == null) {
					Image result = image;
					image = null;
					return result;
				}
				image.close();
				image = next;
			}
		} finally {
			if (image != null) {
				image.close();
			}
		}
	}

	private static final int ACQUIRE_SUCCESS = 0;
	private static final int ACQUIRE_NO_BUFS = 1;
	private static final int ACQUIRE_MAX_IMAGES = 2;

	private final java.util.ArrayDeque<SurfaceImage> mQueuedImages = new java.util.ArrayDeque<SurfaceImage>();

	/* AOSP acquireNextSurfaceImage() -> nativeImageSetup(): the oldest queued image, ACQUIRE_NO_BUFS without one,
	 * ACQUIRE_MAX_IMAGES when maxImages are acquired */
	private int acquireNextSurfaceImage(SurfaceImage[] out) {
		synchronized (mCloseLock) {
			if (!mIsReaderValid)
				return ACQUIRE_NO_BUFS;
			if (mAcquiredImages.size() >= mMaxImages)
				return ACQUIRE_MAX_IMAGES;
			SurfaceImage si = mQueuedImages.poll();
			if (si == null)
				return ACQUIRE_NO_BUFS;
			si.mIsImageValid = true;
			mAcquiredImages.add(si);
			out[0] = si;
			return ACQUIRE_SUCCESS;
		}
	}

	public Image acquireNextImage() {
		SurfaceImage[] si = new SurfaceImage[1];
		int status = acquireNextSurfaceImage(si);
		switch (status) {
			case ACQUIRE_SUCCESS:
				return si[0];
			case ACQUIRE_NO_BUFS:
				return null;
			case ACQUIRE_MAX_IMAGES:
				throw new IllegalStateException(String.format("maxImages (%d) has already been acquired, call #close before acquiring more.", mMaxImages));
			default:
				throw new AssertionError("Unknown nativeImageSetup return code " + status);
		}
	}

	/** @hide the consumer of an encoder's input surface (MediaCodec.createInputSurface(), MediaRecorder.getSurface()):
	 * images of any producer's format and size, read on the CPU (AOSP: the encoder's GraphicBufferSource takes the
	 * buffers its producer queues). surface: the Surface to consume (a PersistentSurface), else a new one */
	static ImageReader atl_newEncoderInput(Surface surface, int width, int height) {
		ImageReader reader = new ImageReader(width, height, ImageFormat.PRIVATE, 4, HardwareBuffer.USAGE_CPU_READ_OFTEN | HardwareBuffer.USAGE_VIDEO_ENCODE);
		if (surface != null) {
			reader.mSurface = surface;
			surface.atl_imageReader = reader;
		}
		return reader;
	}

	/** @hide an image's AHardwareBuffer, 0 for an image of ByteBuffers */
	static long atl_hardwareBuffer(Image image) {
		return ((SurfaceImage)image).mBuffer;
	}

	/** @hide the ByteBuffers an image was queued with (ImageReader.queueImage()), null for a hardware buffer */
	static Image.Plane[] atl_queuedPlanes(Image image) {
		SurfaceImage si = (SurfaceImage)image;
		return si.mBuffer != 0 ? null : si.mPlanes;
	}

	/** @hide */
	public Image acquireNextImageNoThrowISE() {
		SurfaceImage[] si = new SurfaceImage[1];
		return acquireNextSurfaceImage(si) == ACQUIRE_SUCCESS ? si[0] : null;
	}

	/** @hide the reader is open */
	public boolean isValidReader() {
		synchronized (mCloseLock) {
			return mIsReaderValid;
		}
	}

	/** @hide the producer side: an image of the reader's format and size (planes as {buffer, row stride, pixel
	 * stride}); false, and the image dropped, while the reader is full or closed. The listener hears of it on its
	 * executor, as AOSP's ListenerHandler posts onImageAvailable(). */
	public boolean queueImage(long timestampNs, java.nio.ByteBuffer[] buffers, int[] rowStrides, int[] pixelStrides) {
		synchronized (mCloseLock) {
			if (!mIsReaderValid || mAcquiredImages.size() + mQueuedImages.size() >= mMaxImages)
				return false;
			mQueuedImages.add(new SurfaceImage(timestampNs, buffers, rowStrides, pixelStrides));
		}
		notifyImageAvailable();
		return true;
	}

	/** @hide the producer side for a hardware buffer (an AHardwareBuffer, of which the reader takes over one
	 * reference) of a producer such as MediaCodec, its size being the buffer's (AOSP: the consumer's default size
	 * only applies to a producer that sets none); false, and the buffer released, while the reader is full or
	 * closed */
	public boolean queueHardwareBuffer(long timestampNs, long buffer, int width, int height) {
		synchronized (mCloseLock) {
			if (!mIsReaderValid || mAcquiredImages.size() + mQueuedImages.size() >= mMaxImages) {
				nativeReleaseBuffer(buffer);
				return false;
			}
			mQueuedImages.add(new SurfaceImage(timestampNs, buffer, width, height));
		}
		notifyImageAvailable();
		return true;
	}

	/* AOSP ListenerHandler: onImageAvailable() on the listener's executor */
	private void notifyImageAvailable() {
		OnImageAvailableListener listener;
		Executor executor;
		synchronized (mListenerLock) {
			listener = mListener;
			executor = mListenerExecutor;
		}
		if (listener != null && executor != null) {
			executor.execute(() -> {
				synchronized (mCloseLock) {
					if (!mIsReaderValid)
						return;
				}
				listener.onImageAvailable(ImageReader.this);
			});
		}
	}

	/* AOSP BufferRemovedListener of the NDK (AImageReader_setBufferRemovedListener()): a buffer leaves the reader;
	 * the callback runs on the reader's callback thread, as AOSP's handler message holds the buffer until then */
	private long mNativeRemovedCallback;
	private long mNativeRemovedContext;
	private long mNativeReader;
	private java.util.concurrent.ExecutorService mNativeExecutor;

	/** @hide AImageReader: the thread of its callbacks (AOSP: the ALooper of the reader) */
	public Executor atl_nativeExecutor(long nativeReader) {
		synchronized (mListenerLock) {
			mNativeReader = nativeReader;
			if (mNativeExecutor == null)
				mNativeExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
			return mNativeExecutor;
		}
	}

	/** @hide AImageReader_setImageListener(): the callback on the reader's callback thread, none for 0 */
	public void atl_setImageListener(long callback, long context) {
		if (callback == 0) {
			setOnImageAvailableListener(null, null);
			return;
		}
		long reader;
		synchronized (mListenerLock) {
			reader = mNativeReader;
		}
		setOnImageAvailableListenerWithExecutor(r -> nativeImageAvailable(callback, context, reader), atl_nativeExecutor(reader));
	}

	/** @hide AImageReader_setBufferRemovedListener() */
	public void atl_setBufferRemovedListener(long callback, long context) {
		synchronized (mListenerLock) {
			mNativeRemovedCallback = callback;
			mNativeRemovedContext = context;
		}
	}

	private void releaseBuffer(long buffer) {
		long callback, context, reader;
		Executor executor;
		synchronized (mListenerLock) {
			callback = mNativeRemovedCallback;
			context = mNativeRemovedContext;
			reader = mNativeReader;
			executor = mNativeExecutor;
		}
		if (callback == 0 || executor == null) {
			nativeReleaseBuffer(buffer);
			return;
		}
		try {
			executor.execute(() -> {
				nativeBufferRemoved(callback, context, reader, buffer);
				nativeReleaseBuffer(buffer);
			});
		} catch (java.util.concurrent.RejectedExecutionException e) {
			nativeReleaseBuffer(buffer);
		}
	}

	/** @hide AImageReader: the result of AOSP's acquireNextSurfaceImage() (ACQUIRE_*) and the image */
	public int atl_acquireNextImage(Image[] out) {
		SurfaceImage[] si = new SurfaceImage[1];
		int status = acquireNextSurfaceImage(si);
		out[0] = si[0];
		return status;
	}

	private static native void nativeReleaseBuffer(long buffer);
	private static native void nativeImageAvailable(long callback, long context, long reader);
	private static native void nativeBufferRemoved(long callback, long context, long reader, long buffer);
	private static native int nativeLockPlanes(long buffer, java.nio.ByteBuffer[] buffers, int[] rowStrides, int[] pixelStrides);
	private static native void nativeUnlock(long buffer);

	/* AOSP ImageReader.SurfaceImage: an image of this reader */
	private class SurfaceImage extends Image {
		private final long mTimestamp;
		private SurfacePlane[] mPlanes;
		private final long mBuffer; /* the AHardwareBuffer of a hardware producer, else 0 */
		private final int mBufferWidth;
		private final int mBufferHeight;
		private boolean mLocked;

		SurfaceImage(long timestampNs, java.nio.ByteBuffer[] buffers, int[] rowStrides, int[] pixelStrides) {
			mTimestamp = timestampNs;
			mPlanes = new SurfacePlane[buffers.length];
			for (int i = 0; i < buffers.length; i++)
				mPlanes[i] = new SurfacePlane(buffers[i], rowStrides[i], pixelStrides[i]);
			mBuffer = 0;
			mBufferWidth = mWidth;
			mBufferHeight = mHeight;
		}

		SurfaceImage(long timestampNs, long buffer, int width, int height) {
			mTimestamp = timestampNs;
			mBuffer = buffer;
			mBufferWidth = width;
			mBufferHeight = height;
		}

		/* AOSP nativeCreatePlanes(): a PRIVATE image has none; the buffer is locked for the CPU, which fails
		 * without CPU read usage (UnsupportedOperationException as AOSP's Image_createSurfacePlanes()) */
		private SurfacePlane[] createPlanes() {
			if (mFormat == ImageFormat.PRIVATE)
				return new SurfacePlane[0];
			java.nio.ByteBuffer[] buffers = new java.nio.ByteBuffer[3];
			int[] rowStrides = new int[3];
			int[] pixelStrides = new int[3];
			int n = (mUsage & HardwareBuffer.USAGE_CPU_READ_OFTEN) != 0 ? nativeLockPlanes(mBuffer, buffers, rowStrides, pixelStrides) : -1;
			if (n < 0)
				throw new UnsupportedOperationException("Failed to lock the image buffer for CPU reading");
			mLocked = true;
			SurfacePlane[] planes = new SurfacePlane[n];
			for (int i = 0; i < n; i++)
				planes[i] = new SurfacePlane(buffers[i], rowStrides[i], pixelStrides[i]);
			return planes;
		}

		@Override
		public int getFormat() {
			throwISEIfImageIsInvalid();
			return mFormat;
		}

		@Override
		public int getWidth() {
			throwISEIfImageIsInvalid();
			return mBufferWidth;
		}

		@Override
		public int getHeight() {
			throwISEIfImageIsInvalid();
			return mBufferHeight;
		}

		@Override
		public long getTimestamp() {
			throwISEIfImageIsInvalid();
			return mTimestamp;
		}

		@Override
		public Plane[] getPlanes() {
			throwISEIfImageIsInvalid();
			if (mPlanes == null)
				mPlanes = createPlanes();
			return mPlanes.clone();
		}

		/* AImage_getHardwareBuffer() */
		private long atl_getHardwareBuffer() {
			return mIsImageValid ? mBuffer : 0;
		}

		/* AOSP SurfaceImage.close() -> ImageReader.releaseImage() */
		@Override
		public void close() {
			synchronized (mCloseLock) {
				if (!mIsImageValid)
					return;
				mIsImageValid = false;
				mAcquiredImages.remove(this);
			}
			freeBuffer();
		}

		/* the buffer goes back (AOSP: releaseBuffer() to the BufferQueue) */
		void freeBuffer() {
			if (mBuffer == 0)
				return;
			if (mLocked)
				nativeUnlock(mBuffer);
			mLocked = false;
			releaseBuffer(mBuffer);
		}

		private class SurfacePlane extends Image.Plane {
			private final java.nio.ByteBuffer mBuffer;
			private final int mRowStride;
			private final int mPixelStride;

			SurfacePlane(java.nio.ByteBuffer buffer, int rowStride, int pixelStride) {
				mBuffer = buffer;
				mRowStride = rowStride;
				mPixelStride = pixelStride;
			}

			@Override
			public int getRowStride() {
				throwISEIfImageIsInvalid();
				return mRowStride;
			}

			@Override
			public int getPixelStride() {
				throwISEIfImageIsInvalid();
				return mPixelStride;
			}

			@Override
			public java.nio.ByteBuffer getBuffer() {
				throwISEIfImageIsInvalid();
				return mBuffer;
			}
		}
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
			for (SurfaceImage image : mQueuedImages)
				image.freeBuffer();
			mQueuedImages.clear();
		}
		synchronized (mListenerLock) {
			if (mNativeExecutor != null)
				mNativeExecutor.shutdown();
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
