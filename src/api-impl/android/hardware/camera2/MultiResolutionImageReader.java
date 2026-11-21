/* AOSP frameworks/base/core/java/android/hardware/camera2/MultiResolutionImageReader.java (Android 16)
 * Differences: javadoc, @FlaggedApi (MULTIRESOLUTION_IMAGEREADER_USAGE_PUBLIC, enabled in Android 16: the usage constructor
 * is kept) and @SuppressLint removed. */
package android.hardware.camera2;

import static com.android.internal.util.Preconditions.checkNotNull;

import android.annotation.CallbackExecutor;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.ImageFormat;
import android.graphics.ImageFormat.Format;
import android.hardware.HardwareBuffer;
import android.hardware.HardwareBuffer.Usage;
import android.hardware.camera2.params.MultiResolutionStreamInfo;
import android.media.Image;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;

import java.util.Collection;
import java.util.concurrent.Executor;

public class MultiResolutionImageReader implements AutoCloseable {

	private static final String TAG = "MultiResolutionImageReader";

		public MultiResolutionImageReader(
			@NonNull Collection<MultiResolutionStreamInfo> streams,
			@Format			 int format,
			@IntRange(from = 1) int maxImages) {
		mFormat = format;
		mMaxImages = maxImages;

		if (streams == null || streams.size() <= 1) {
			throw new IllegalArgumentException(
				"The streams info collection must contain at least 2 entries");
		}
		if (mMaxImages < 1) {
			throw new IllegalArgumentException(
				"Maximum outstanding image count must be at least 1");
		}

		if (format == ImageFormat.NV21) {
			throw new IllegalArgumentException(
					"NV21 format is not supported");
		}

		int numImageReaders = streams.size();
		mReaders = new ImageReader[numImageReaders];
		mStreamInfo = new MultiResolutionStreamInfo[numImageReaders];
		int index = 0;
		for (MultiResolutionStreamInfo streamInfo : streams) {
			mReaders[index] = ImageReader.newInstance(streamInfo.getWidth(),
					streamInfo.getHeight(), format, maxImages);
			mStreamInfo[index] = streamInfo;
			index++;
		}
	}
	public MultiResolutionImageReader(
			@NonNull Collection<MultiResolutionStreamInfo> streams,
			@Format			 int format,
			@IntRange(from = 1) int maxImages,
			@Usage			  long usage) {
		mFormat = format;
		mMaxImages = maxImages;

		if (streams == null || streams.size() <= 1) {
			throw new IllegalArgumentException(
				"The streams info collection must contain at least 2 entries");
		}
		if (mMaxImages < 1) {
			throw new IllegalArgumentException(
				"Maximum outstanding image count must be at least 1");
		}

		if (format == ImageFormat.NV21) {
			throw new IllegalArgumentException(
					"NV21 format is not supported");
		}

		int numImageReaders = streams.size();
		mReaders = new ImageReader[numImageReaders];
		mStreamInfo = new MultiResolutionStreamInfo[numImageReaders];
		int index = 0;
		for (MultiResolutionStreamInfo streamInfo : streams) {
			mReaders[index] = ImageReader.newInstance(streamInfo.getWidth(),
					streamInfo.getHeight(), format, maxImages, usage);
			mStreamInfo[index] = streamInfo;
			index++;
		}
	}
	public void setOnImageAvailableListener(
			@Nullable ImageReader.OnImageAvailableListener listener,
			@Nullable @CallbackExecutor Executor executor) {
		for (int i = 0; i < mReaders.length; i++) {
			mReaders[i].setOnImageAvailableListenerWithExecutor(listener, executor);
		}
	}

	@Override
	public void close() {
		flush();

		for (int i = 0; i < mReaders.length; i++) {
			mReaders[i].close();
		}
	}

	@Override
	protected void finalize() {
		close();
	}

		public void flush() {
		flushOther(null);
	}

		public void flushOther(ImageReader reader) {
		for (int i = 0; i < mReaders.length; i++) {
			if (reader != null && reader == mReaders[i]) {
				continue;
			}

			while (true) {
				Image image = mReaders[i].acquireNextImageNoThrowISE();
				if (image == null) {
					break;
				} else {
					image.close();
				}
			}
		}
	}

		public @NonNull ImageReader[] getReaders() {
		return mReaders;
	}

		public @NonNull Surface getSurface(@NonNull Size configuredSize,
			@NonNull String physicalCameraId) {
		checkNotNull(configuredSize, "configuredSize must not be null");
		checkNotNull(physicalCameraId, "physicalCameraId must not be null");

		for (int i = 0; i < mStreamInfo.length; i++) {
			if (mStreamInfo[i].getWidth() == configuredSize.getWidth()
					&& mStreamInfo[i].getHeight() == configuredSize.getHeight()
					&& physicalCameraId.equals(mStreamInfo[i].getPhysicalCameraId())) {
				return mReaders[i].getSurface();
			}
		}
		throw new IllegalArgumentException("configuredSize and physicalCameraId don't match with "
				+ "this MultiResolutionImageReader");
	}

		public @NonNull Surface getSurface() {
		// Pick the surface of smallest size. This is necessary for an ultra high resolution
		// camera not to default to maximum resolution pixel mode.
		int minReaderSize = mReaders[0].getWidth() * mReaders[0].getHeight();
		Surface candidateSurface = mReaders[0].getSurface();
		for (int i = 1; i < mReaders.length; i++) {
			int readerSize =  mReaders[i].getWidth() * mReaders[i].getHeight();
			if (readerSize < minReaderSize) {
				minReaderSize = readerSize;
				candidateSurface = mReaders[i].getSurface();
			}
		}
		return candidateSurface;
	}

		public @NonNull MultiResolutionStreamInfo getStreamInfoForImageReader(
			@NonNull ImageReader reader) {
		for (int i = 0; i < mReaders.length; i++) {
			if (reader == mReaders[i]) {
				return mStreamInfo[i];
			}
		}

		throw new IllegalArgumentException("ImageReader doesn't belong to this multi-resolution "
				+ "imagereader");
	}

	// mReaders and mStreamInfo has the same length, and their entries are 1:1 mapped.
	private final ImageReader[] mReaders;
	private final MultiResolutionStreamInfo[] mStreamInfo;

	private final int mFormat;
	private final int mMaxImages;
}
