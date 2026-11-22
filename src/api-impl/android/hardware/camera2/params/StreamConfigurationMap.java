/* AOSP (Android 16) android.hardware.camera2.params.StreamConfigurationMap, comments and annotations stripped.
 * Differences: the cameraHeifGainmap flag is the constant CAMERA_HEIF_GAINMAP (true); isOutputSupportedFor(Surface)
 * cannot query a native window. */
package android.hardware.camera2.params;

import static com.android.internal.util.Preconditions.checkArrayElementsNotNull;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.DataSpace;
import android.hardware.camera2.utils.HashCodeHelpers;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public final class StreamConfigurationMap {
	/* AOSP's com.android.internal.camera.flags.Flags.cameraHeifGainmap(); treated as enabled (not verified for AOSP 16) */
	private static final boolean CAMERA_HEIF_GAINMAP = true;

	private static final String TAG = "StreamConfigurationMap";

	private static final int MAX_DIMEN_FOR_ROUNDING = 1920; // maximum allowed width for rounding

	public StreamConfigurationMap(
			StreamConfiguration[] configurations,
			StreamConfigurationDuration[] minFrameDurations,
			StreamConfigurationDuration[] stallDurations,
			StreamConfiguration[] depthConfigurations,
			StreamConfigurationDuration[] depthMinFrameDurations,
			StreamConfigurationDuration[] depthStallDurations,
			StreamConfiguration[] dynamicDepthConfigurations,
			StreamConfigurationDuration[] dynamicDepthMinFrameDurations,
			StreamConfigurationDuration[] dynamicDepthStallDurations,
			StreamConfiguration[] heicConfigurations,
			StreamConfigurationDuration[] heicMinFrameDurations,
			StreamConfigurationDuration[] heicStallDurations,
			StreamConfiguration[] jpegRConfigurations,
			StreamConfigurationDuration[] jpegRMinFrameDurations,
			StreamConfigurationDuration[] jpegRStallDurations,
			StreamConfiguration[] heicUltraHDRConfigurations,
			StreamConfigurationDuration[] heicUltraHDRMinFrameDurations,
			StreamConfigurationDuration[] heicUltraHDRStallDurations,
			HighSpeedVideoConfiguration[] highSpeedVideoConfigurations,
			ReprocessFormatsMap inputOutputFormatsMap,
			boolean listHighResolution) {
		this(configurations, minFrameDurations, stallDurations,
					depthConfigurations, depthMinFrameDurations, depthStallDurations,
					dynamicDepthConfigurations, dynamicDepthMinFrameDurations,
					dynamicDepthStallDurations,
					heicConfigurations, heicMinFrameDurations, heicStallDurations,
					jpegRConfigurations, jpegRMinFrameDurations, jpegRStallDurations,
					heicUltraHDRConfigurations, heicUltraHDRMinFrameDurations,
					heicUltraHDRStallDurations, highSpeedVideoConfigurations, inputOutputFormatsMap,
					listHighResolution, /*enforceImplementationDefined*/ true);
	}

	public StreamConfigurationMap(
			StreamConfiguration[] configurations,
			StreamConfigurationDuration[] minFrameDurations,
			StreamConfigurationDuration[] stallDurations,
			StreamConfiguration[] depthConfigurations,
			StreamConfigurationDuration[] depthMinFrameDurations,
			StreamConfigurationDuration[] depthStallDurations,
			StreamConfiguration[] dynamicDepthConfigurations,
			StreamConfigurationDuration[] dynamicDepthMinFrameDurations,
			StreamConfigurationDuration[] dynamicDepthStallDurations,
			StreamConfiguration[] heicConfigurations,
			StreamConfigurationDuration[] heicMinFrameDurations,
			StreamConfigurationDuration[] heicStallDurations,
			StreamConfiguration[] jpegRConfigurations,
			StreamConfigurationDuration[] jpegRMinFrameDurations,
			StreamConfigurationDuration[] jpegRStallDurations,
			StreamConfiguration[] heicUltraHDRConfigurations,
			StreamConfigurationDuration[] heicUltraHDRMinFrameDurations,
			StreamConfigurationDuration[] heicUltraHDRStallDurations,
			HighSpeedVideoConfiguration[] highSpeedVideoConfigurations,
			ReprocessFormatsMap inputOutputFormatsMap,
			boolean listHighResolution,
			boolean enforceImplementationDefined) {
		if (configurations == null &&
				depthConfigurations == null &&
				heicConfigurations == null) {
			throw new NullPointerException("At least one of color/depth/heic configurations " +
					"must not be null");
		}

		if (configurations == null) {
			mConfigurations = new StreamConfiguration[0];
			mMinFrameDurations = new StreamConfigurationDuration[0];
			mStallDurations = new StreamConfigurationDuration[0];
		} else {
			mConfigurations = checkArrayElementsNotNull(configurations, "configurations");
			mMinFrameDurations = checkArrayElementsNotNull(minFrameDurations, "minFrameDurations");
			mStallDurations = checkArrayElementsNotNull(stallDurations, "stallDurations");
		}

		mListHighResolution = listHighResolution;

		if (depthConfigurations == null) {
			mDepthConfigurations = new StreamConfiguration[0];
			mDepthMinFrameDurations = new StreamConfigurationDuration[0];
			mDepthStallDurations = new StreamConfigurationDuration[0];
		} else {
			mDepthConfigurations = checkArrayElementsNotNull(depthConfigurations,
					"depthConfigurations");
			mDepthMinFrameDurations = checkArrayElementsNotNull(depthMinFrameDurations,
					"depthMinFrameDurations");
			mDepthStallDurations = checkArrayElementsNotNull(depthStallDurations,
					"depthStallDurations");
		}

		if (dynamicDepthConfigurations == null) {
			mDynamicDepthConfigurations = new StreamConfiguration[0];
			mDynamicDepthMinFrameDurations = new StreamConfigurationDuration[0];
			mDynamicDepthStallDurations = new StreamConfigurationDuration[0];
		} else {
			mDynamicDepthConfigurations = checkArrayElementsNotNull(dynamicDepthConfigurations,
					"dynamicDepthConfigurations");
			mDynamicDepthMinFrameDurations = checkArrayElementsNotNull(
					dynamicDepthMinFrameDurations, "dynamicDepthMinFrameDurations");
			mDynamicDepthStallDurations = checkArrayElementsNotNull(dynamicDepthStallDurations,
					"dynamicDepthStallDurations");
		}

		if (heicConfigurations == null) {
			mHeicConfigurations = new StreamConfiguration[0];
			mHeicMinFrameDurations = new StreamConfigurationDuration[0];
			mHeicStallDurations = new StreamConfigurationDuration[0];
		} else {
			mHeicConfigurations = checkArrayElementsNotNull(heicConfigurations,
					"heicConfigurations");
			mHeicMinFrameDurations = checkArrayElementsNotNull(heicMinFrameDurations,
					"heicMinFrameDurations");
			mHeicStallDurations = checkArrayElementsNotNull(heicStallDurations,
					"heicStallDurations");
		}

		if (heicUltraHDRConfigurations == null || (!CAMERA_HEIF_GAINMAP)) {
			mHeicUltraHDRConfigurations = new StreamConfiguration[0];
			mHeicUltraHDRMinFrameDurations = new StreamConfigurationDuration[0];
			mHeicUltraHDRStallDurations = new StreamConfigurationDuration[0];
		} else {
			mHeicUltraHDRConfigurations = checkArrayElementsNotNull(heicUltraHDRConfigurations,
					"heicUltraHDRConfigurations");
			mHeicUltraHDRMinFrameDurations = checkArrayElementsNotNull(
					heicUltraHDRMinFrameDurations, "heicUltraHDRMinFrameDurations");
			mHeicUltraHDRStallDurations = checkArrayElementsNotNull(heicUltraHDRStallDurations,
					"heicUltraHDRStallDurations");
		}

		if (jpegRConfigurations == null) {
			mJpegRConfigurations = new StreamConfiguration[0];
			mJpegRMinFrameDurations = new StreamConfigurationDuration[0];
			mJpegRStallDurations = new StreamConfigurationDuration[0];
		} else {
			mJpegRConfigurations = checkArrayElementsNotNull(jpegRConfigurations,
					"jpegRConfigurations");
			mJpegRMinFrameDurations = checkArrayElementsNotNull(jpegRMinFrameDurations,
					"jpegRFrameDurations");
			mJpegRStallDurations = checkArrayElementsNotNull(jpegRStallDurations,
					"jpegRStallDurations");
		}

		if (highSpeedVideoConfigurations == null) {
			mHighSpeedVideoConfigurations = new HighSpeedVideoConfiguration[0];
		} else {
			mHighSpeedVideoConfigurations = checkArrayElementsNotNull(
					highSpeedVideoConfigurations, "highSpeedVideoConfigurations");
		}

		for (StreamConfiguration config : mConfigurations) {
			int fmt = config.getFormat();
			SparseIntArray map = null;
			if (config.isOutput()) {
				mAllOutputFormats.put(fmt, mAllOutputFormats.get(fmt) + 1);
				long duration = 0;
				if (mListHighResolution) {
					for (StreamConfigurationDuration configurationDuration : mMinFrameDurations) {
						if (configurationDuration.getFormat() == fmt &&
								configurationDuration.getWidth() == config.getSize().getWidth() &&
								configurationDuration.getHeight() == config.getSize().getHeight()) {
							duration = configurationDuration.getDuration();
							break;
						}
					}
				}
				map = duration <= DURATION_20FPS_NS ?
						mOutputFormats : mHighResOutputFormats;
			} else {
				map = mInputFormats;
			}
			map.put(fmt, map.get(fmt) + 1);
		}

		for (StreamConfiguration config : mDepthConfigurations) {
			if (!config.isOutput()) {
				continue;
			}

			mDepthOutputFormats.put(config.getFormat(),
					mDepthOutputFormats.get(config.getFormat()) + 1);
		}
		for (StreamConfiguration config : mDynamicDepthConfigurations) {
			if (!config.isOutput()) {
				continue;
			}

			mDynamicDepthOutputFormats.put(config.getFormat(),
					mDynamicDepthOutputFormats.get(config.getFormat()) + 1);
		}

		for (StreamConfiguration config : mHeicConfigurations) {
			if (!config.isOutput()) {
				continue;
			}

			mHeicOutputFormats.put(config.getFormat(),
					mHeicOutputFormats.get(config.getFormat()) + 1);
		}

		if (CAMERA_HEIF_GAINMAP) {
			for (StreamConfiguration config : mHeicUltraHDRConfigurations) {
				if (!config.isOutput()) {
					continue;
				}

				mHeicUltraHDROutputFormats.put(config.getFormat(),
						mHeicUltraHDROutputFormats.get(config.getFormat()) + 1);
			}
		}

		for (StreamConfiguration config : mJpegRConfigurations) {
			if (!config.isOutput()) {
				continue;
			}

			mJpegROutputFormats.put(config.getFormat(),
					mJpegROutputFormats.get(config.getFormat()) + 1);
		}

		if (configurations != null && enforceImplementationDefined &&
				mOutputFormats.indexOfKey(HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED) < 0) {
			throw new AssertionError(
					"At least one stream configuration for IMPLEMENTATION_DEFINED must exist");
		}

		for (HighSpeedVideoConfiguration config : mHighSpeedVideoConfigurations) {
			Size size = config.getSize();
			Range<Integer> fpsRange = config.getFpsRange();
			Integer fpsRangeCount = mHighSpeedVideoSizeMap.get(size);
			if (fpsRangeCount == null) {
				fpsRangeCount = 0;
			}
			mHighSpeedVideoSizeMap.put(size, fpsRangeCount + 1);
			Integer sizeCount = mHighSpeedVideoFpsRangeMap.get(fpsRange);
			if (sizeCount == null) {
				sizeCount = 0;
			}
			mHighSpeedVideoFpsRangeMap.put(fpsRange, sizeCount + 1);
		}

		mInputOutputFormatsMap = inputOutputFormatsMap;
	}

	public int[] getOutputFormats() {
		return getPublicFormats(/*output*/true);
	}

	public int[] getValidOutputFormatsForInput(int inputFormat) {
		if (mInputOutputFormatsMap == null) {
			return new int[0];
		}

		int[] outputs = mInputOutputFormatsMap.getOutputs(inputFormat);
		if (mHeicOutputFormats.size() > 0) {
			int[] outputsWithHeic = Arrays.copyOf(outputs, outputs.length+1);
			outputsWithHeic[outputs.length] = ImageFormat.HEIC;
			return outputsWithHeic;
		} else {
			return outputs;
		}
	}

	public int[] getInputFormats() {
		return getPublicFormats(/*output*/false);
	}

	public Size[] getInputSizes(final int format) {
		return getPublicFormatSizes(format, /*output*/false, /*highRes*/false);
	}

	public boolean isOutputSupportedFor(int format) {
		checkArgumentFormat(format);

		int internalFormat = imageFormatToInternal(format);
		int dataspace = imageFormatToDataspace(format);
		if (CAMERA_HEIF_GAINMAP) {
			if (dataspace == DataSpace.DATASPACE_HEIF_ULTRAHDR) {
				return mHeicUltraHDROutputFormats.indexOfKey(internalFormat) >= 0;
			}
		}
		if (dataspace == HAL_DATASPACE_DEPTH) {
			return mDepthOutputFormats.indexOfKey(internalFormat) >= 0;
		} else if (dataspace == HAL_DATASPACE_DYNAMIC_DEPTH) {
			return mDynamicDepthOutputFormats.indexOfKey(internalFormat) >= 0;
		} else if (dataspace == HAL_DATASPACE_HEIF) {
			return mHeicOutputFormats.indexOfKey(internalFormat) >= 0;
		} else if (dataspace == HAL_DATASPACE_JPEG_R) {
			return mJpegROutputFormats.indexOfKey(internalFormat) >= 0;
		} else {
			return getFormatsMap(/*output*/true).indexOfKey(internalFormat) >= 0;
		}
	}

	public static <T> boolean isOutputSupportedFor(Class<T> klass) {
		Objects.requireNonNull(klass, "klass must not be null");

		if (klass == android.media.ImageReader.class) {
			return true;
		} else if (klass == android.media.MediaRecorder.class) {
			return true;
		} else if (klass == android.media.MediaCodec.class) {
			return true;
		} else if (klass == android.renderscript.Allocation.class) {
			return true;
		} else if (klass == android.view.SurfaceHolder.class) {
			return true;
		} else if (klass == android.graphics.SurfaceTexture.class) {
			return true;
		}

		return false;
	}

	public boolean isOutputSupportedFor(Surface surface) {
		Objects.requireNonNull(surface, "surface must not be null");

		/* AOSP matches size/format/dataspace of the surface's native window (SurfaceUtils) against the output
		 * configurations; an ATL Surface has no native window to query, so none matches. */
		if (!surface.isValid())
			throw new IllegalArgumentException("Surface is not valid");
		return false;
	}

	public boolean isOutputSupportedFor(Size size, int format) {
		int internalFormat = imageFormatToInternal(format);
		int dataspace = imageFormatToDataspace(format);

		StreamConfiguration[] configs =
				dataspace == HAL_DATASPACE_DEPTH ? mDepthConfigurations :
				dataspace == HAL_DATASPACE_DYNAMIC_DEPTH ? mDynamicDepthConfigurations :
				dataspace == HAL_DATASPACE_HEIF ? mHeicConfigurations :
				dataspace == HAL_DATASPACE_JPEG_R ? mJpegRConfigurations :
				mConfigurations;
		if (CAMERA_HEIF_GAINMAP) {
			if (dataspace == DataSpace.DATASPACE_HEIF_ULTRAHDR ) {
				configs = mHeicUltraHDRConfigurations;
			}
		}
		for (StreamConfiguration config : configs) {
			if ((config.getFormat() == internalFormat) && config.isOutput() &&
					config.getSize().equals(size)) {
				return true;
			}
		}

		return false;
	}

	public <T> Size[] getOutputSizes(Class<T> klass) {
		if (isOutputSupportedFor(klass) == false) {
			return null;
		}

		return getInternalFormatSizes(HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED,
				HAL_DATASPACE_UNKNOWN,/*output*/true, /*highRes*/false);
	}

	public Size[] getOutputSizes(int format) {
		return getPublicFormatSizes(format, /*output*/true, /*highRes*/ false);
	}

	public Size[] getHighSpeedVideoSizes() {
		Set<Size> keySet = mHighSpeedVideoSizeMap.keySet();
		return keySet.toArray(new Size[keySet.size()]);
	}

	public Range<Integer>[] getHighSpeedVideoFpsRangesFor(Size size) {
		Integer fpsRangeCount = mHighSpeedVideoSizeMap.get(size);
		if (fpsRangeCount == null || fpsRangeCount == 0) {
			throw new IllegalArgumentException(String.format(
					"Size %s does not support high speed video recording", size));
		}

		@SuppressWarnings("unchecked")
		Range<Integer>[] fpsRanges = new Range[fpsRangeCount];
		int i = 0;
		for (HighSpeedVideoConfiguration config : mHighSpeedVideoConfigurations) {
			if (size.equals(config.getSize())) {
				fpsRanges[i++] = config.getFpsRange();
			}
		}
		return fpsRanges;
	}

	@SuppressWarnings("unchecked")
	public Range<Integer>[] getHighSpeedVideoFpsRanges() {
		Set<Range<Integer>> keySet = mHighSpeedVideoFpsRangeMap.keySet();
		return keySet.toArray(new Range[keySet.size()]);
	}

	public Size[] getHighSpeedVideoSizesFor(Range<Integer> fpsRange) {
		Integer sizeCount = mHighSpeedVideoFpsRangeMap.get(fpsRange);
		if (sizeCount == null || sizeCount == 0) {
			throw new IllegalArgumentException(String.format(
					"FpsRange %s does not support high speed video recording", fpsRange));
		}

		Size[] sizes = new Size[sizeCount];
		int i = 0;
		for (HighSpeedVideoConfiguration config : mHighSpeedVideoConfigurations) {
			if (fpsRange.equals(config.getFpsRange())) {
				sizes[i++] = config.getSize();
			}
		}
		return sizes;
	}

	public Size[] getHighResolutionOutputSizes(int format) {
		if (!mListHighResolution) return null;

		return getPublicFormatSizes(format, /*output*/true, /*highRes*/ true);
	}

	public long getOutputMinFrameDuration(int format, Size size) {
		Objects.requireNonNull(size, "size must not be null");
		checkArgumentFormatSupported(format, /*output*/true);

		return getInternalFormatDuration(imageFormatToInternal(format),
				imageFormatToDataspace(format),
				size,
				DURATION_MIN_FRAME);
	}

	public <T> long getOutputMinFrameDuration(final Class<T> klass, final Size size) {
		if (!isOutputSupportedFor(klass)) {
			throw new IllegalArgumentException("klass was not supported");
		}

		return getInternalFormatDuration(HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED,
				HAL_DATASPACE_UNKNOWN,
				size, DURATION_MIN_FRAME);
	}

	public long getOutputStallDuration(int format, Size size) {
		checkArgumentFormatSupported(format, /*output*/true);

		return getInternalFormatDuration(imageFormatToInternal(format),
				imageFormatToDataspace(format),
				size,
				DURATION_STALL);
	}

	public <T> long getOutputStallDuration(final Class<T> klass, final Size size) {
		if (!isOutputSupportedFor(klass)) {
			throw new IllegalArgumentException("klass was not supported");
		}

		return getInternalFormatDuration(HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED,
				HAL_DATASPACE_UNKNOWN, size, DURATION_STALL);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj instanceof StreamConfigurationMap) {
			final StreamConfigurationMap other = (StreamConfigurationMap) obj;
			return Arrays.equals(mConfigurations, other.mConfigurations) &&
					Arrays.equals(mMinFrameDurations, other.mMinFrameDurations) &&
					Arrays.equals(mStallDurations, other.mStallDurations) &&
					Arrays.equals(mDepthConfigurations, other.mDepthConfigurations) &&
					Arrays.equals(mDepthMinFrameDurations, other.mDepthMinFrameDurations) &&
					Arrays.equals(mDepthStallDurations, other.mDepthStallDurations) &&
					Arrays.equals(mDynamicDepthConfigurations, other.mDynamicDepthConfigurations) &&
					Arrays.equals(mDynamicDepthMinFrameDurations,
							other.mDynamicDepthMinFrameDurations) &&
					Arrays.equals(mDynamicDepthStallDurations, other.mDynamicDepthStallDurations) &&
					Arrays.equals(mHeicConfigurations, other.mHeicConfigurations) &&
					Arrays.equals(mHeicMinFrameDurations, other.mHeicMinFrameDurations) &&
					Arrays.equals(mHeicStallDurations, other.mHeicStallDurations) &&
					Arrays.equals(mHeicUltraHDRConfigurations, other.mHeicUltraHDRConfigurations) &&
					Arrays.equals(mHeicUltraHDRMinFrameDurations,
							other.mHeicUltraHDRMinFrameDurations) &&
					Arrays.equals(mHeicUltraHDRStallDurations, other.mHeicUltraHDRStallDurations) &&
					Arrays.equals(mJpegRConfigurations, other.mJpegRConfigurations) &&
					Arrays.equals(mJpegRMinFrameDurations, other.mJpegRMinFrameDurations) &&
					Arrays.equals(mJpegRStallDurations, other.mJpegRStallDurations) &&
					Arrays.equals(mHighSpeedVideoConfigurations,
							other.mHighSpeedVideoConfigurations);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return HashCodeHelpers.hashCodeGeneric(
				mConfigurations, mMinFrameDurations, mStallDurations,
				mDepthConfigurations, mDepthMinFrameDurations, mDepthStallDurations,
				mDynamicDepthConfigurations, mDynamicDepthMinFrameDurations,
				mDynamicDepthStallDurations, mHeicConfigurations,
				mHeicMinFrameDurations, mHeicStallDurations,
				mHeicUltraHDRConfigurations, mHeicUltraHDRMinFrameDurations,
				mHeicUltraHDRStallDurations, mJpegRConfigurations, mJpegRMinFrameDurations,
				mJpegRStallDurations, mHighSpeedVideoConfigurations);
	}

	private int checkArgumentFormatSupported(int format, boolean output) {
		checkArgumentFormat(format);

		int internalFormat = imageFormatToInternal(format);
		int internalDataspace = imageFormatToDataspace(format);

		if (output) {
			if (CAMERA_HEIF_GAINMAP) {
				if (internalDataspace == DataSpace.DATASPACE_HEIF_ULTRAHDR) {
					if (mHeicUltraHDROutputFormats.indexOfKey(internalFormat) >= 0) {
						return format;
					}
				}
			}
			if (internalDataspace == HAL_DATASPACE_DEPTH) {
				if (mDepthOutputFormats.indexOfKey(internalFormat) >= 0) {
					return format;
				}
			} else if (internalDataspace == HAL_DATASPACE_DYNAMIC_DEPTH) {
				if (mDynamicDepthOutputFormats.indexOfKey(internalFormat) >= 0) {
					return format;
				}
			} else if (internalDataspace == HAL_DATASPACE_HEIF) {
				if (mHeicOutputFormats.indexOfKey(internalFormat) >= 0) {
					return format;
				}
			} else if (internalDataspace == HAL_DATASPACE_JPEG_R) {
				if (mJpegROutputFormats.indexOfKey(internalFormat) >= 0) {
					return format;
				}
			} else {
				if (mAllOutputFormats.indexOfKey(internalFormat) >= 0) {
					return format;
				}
			}
		} else {
			if (mInputFormats.indexOfKey(internalFormat) >= 0) {
				return format;
			}
		}

		throw new IllegalArgumentException(String.format(
				"format %x is not supported by this stream configuration map", format));
	}

	static int checkArgumentFormatInternal(int format) {
		switch (format) {
			case HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED:
			case HAL_PIXEL_FORMAT_BLOB:
			case HAL_PIXEL_FORMAT_RAW_OPAQUE:
			case HAL_PIXEL_FORMAT_Y16:
				return format;
			case ImageFormat.JPEG:
			case ImageFormat.HEIC:
				throw new IllegalArgumentException(
						"An unknown internal format: " + format);
			default:
				return checkArgumentFormat(format);
		}
	}

	static int checkArgumentFormat(int format) {
		if (!ImageFormat.isPublicFormat(format) && !PixelFormat.isPublicFormat(format)) {
			throw new IllegalArgumentException(String.format(
					"format 0x%x was not defined in either ImageFormat or PixelFormat", format));
		}

		return format;
	}

	public static int imageFormatToPublic(int format) {
		switch (format) {
			case HAL_PIXEL_FORMAT_BLOB:
				return ImageFormat.JPEG;
			case ImageFormat.JPEG:
				throw new IllegalArgumentException(
						"ImageFormat.JPEG is an unknown internal format");
			default:
				return format;
		}
	}

	public static int depthFormatToPublic(int format) {
		switch (format) {
			case HAL_PIXEL_FORMAT_BLOB:
				return ImageFormat.DEPTH_POINT_CLOUD;
			case HAL_PIXEL_FORMAT_Y16:
				return ImageFormat.DEPTH16;
			case HAL_PIXEL_FORMAT_RAW16:
				return ImageFormat.RAW_DEPTH;
			case HAL_PIXEL_FORMAT_RAW10:
				return ImageFormat.RAW_DEPTH10;
			case ImageFormat.JPEG:
				throw new IllegalArgumentException(
						"ImageFormat.JPEG is an unknown internal format");
			case HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED:
				throw new IllegalArgumentException(
						"IMPLEMENTATION_DEFINED must not leak to public API");
			default:
				throw new IllegalArgumentException(
						"Unknown DATASPACE_DEPTH format " + format);
		}
	}

	static int[] imageFormatToPublic(int[] formats) {
		if (formats == null) {
			return null;
		}

		for (int i = 0; i < formats.length; ++i) {
			formats[i] = imageFormatToPublic(formats[i]);
		}

		return formats;
	}

	static int imageFormatToInternal(int format) {
		if (CAMERA_HEIF_GAINMAP) {
			if (format == ImageFormat.HEIC_ULTRAHDR) {
				return HAL_PIXEL_FORMAT_BLOB;
			}
		}
		switch (format) {
			case ImageFormat.JPEG:
			case ImageFormat.DEPTH_POINT_CLOUD:
			case ImageFormat.DEPTH_JPEG:
			case ImageFormat.HEIC:
			case ImageFormat.JPEG_R:
				return HAL_PIXEL_FORMAT_BLOB;
			case ImageFormat.DEPTH16:
				return HAL_PIXEL_FORMAT_Y16;
			case ImageFormat.RAW_DEPTH:
				return HAL_PIXEL_FORMAT_RAW16;
			case ImageFormat.RAW_DEPTH10:
				return HAL_PIXEL_FORMAT_RAW10;
			default:
				return format;
		}
	}

	static int imageFormatToDataspace(int format) {
		if (CAMERA_HEIF_GAINMAP) {
			if (format == ImageFormat.HEIC_ULTRAHDR) {
				return DataSpace.DATASPACE_HEIF_ULTRAHDR;
			}
		}
		switch (format) {
			case ImageFormat.JPEG:
				return HAL_DATASPACE_V0_JFIF;
			case ImageFormat.DEPTH_POINT_CLOUD:
			case ImageFormat.DEPTH16:
			case ImageFormat.RAW_DEPTH:
			case ImageFormat.RAW_DEPTH10:
				return HAL_DATASPACE_DEPTH;
			case ImageFormat.DEPTH_JPEG:
				return HAL_DATASPACE_DYNAMIC_DEPTH;
			case ImageFormat.HEIC:
				return HAL_DATASPACE_HEIF;
			case ImageFormat.JPEG_R:
				return HAL_DATASPACE_JPEG_R;
			case ImageFormat.YUV_420_888:
				return HAL_DATASPACE_JFIF;
			case ImageFormat.RAW_SENSOR:
			case ImageFormat.RAW_PRIVATE:
			case ImageFormat.RAW10:
			case ImageFormat.RAW12:
				return HAL_DATASPACE_ARBITRARY;
			default:
				return HAL_DATASPACE_UNKNOWN;
		}
	}

	static int internalFormatAndDataspaceToImageFormat(int internalFormat, int dataSpace) {
		if (dataSpace == HAL_DATASPACE_DEPTH) {
			return depthFormatToPublic(internalFormat);
		}

		if (internalFormat == HAL_PIXEL_FORMAT_BLOB) {
			if (CAMERA_HEIF_GAINMAP) {
				if (dataSpace == DataSpace.DATASPACE_HEIF_ULTRAHDR) {
					return ImageFormat.HEIC_ULTRAHDR;
				}
			}

			switch (dataSpace) {
				case HAL_DATASPACE_DYNAMIC_DEPTH:
					return ImageFormat.DEPTH_JPEG;
				case HAL_DATASPACE_HEIF:
					return ImageFormat.HEIC;
				case HAL_DATASPACE_JPEG_R:
					return ImageFormat.JPEG_R;
				case HAL_DATASPACE_V0_JFIF:
				default:
					return ImageFormat.JPEG;
			}
		}

		switch (internalFormat) {
			case HAL_PIXEL_FORMAT_RGBA_8888:
			case HAL_PIXEL_FORMAT_RGBX_8888:
			case HAL_PIXEL_FORMAT_RGB_888:
			case HAL_PIXEL_FORMAT_RGB_565:
			case HAL_PIXEL_FORMAT_YCBCR_422_SP:
			case HAL_PIXEL_FORMAT_YCRCB_420_SP:
			case HAL_PIXEL_FORMAT_YCBCR_422_I:
			case HAL_PIXEL_FORMAT_RAW16:
			case HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED:
			case HAL_PIXEL_FORMAT_YCBCR_420_888:
			case HAL_PIXEL_FORMAT_RAW_OPAQUE:
			case HAL_PIXEL_FORMAT_RAW10:
			case HAL_PIXEL_FORMAT_RAW12:
			case HAL_PIXEL_FORMAT_YCBCR_P010:
			case HAL_PIXEL_FORMAT_YCBCR_P210:
			case HAL_PIXEL_FORMAT_Y8:
			case HAL_PIXEL_FORMAT_Y16:
			case HAL_PIXEL_FORMAT_YV12:
				return internalFormat;
			default:
				return ImageFormat.UNKNOWN;
		}
	}

	public static int[] imageFormatToInternal(int[] formats) {
		if (formats == null) {
			return null;
		}

		for (int i = 0; i < formats.length; ++i) {
			formats[i] = imageFormatToInternal(formats[i]);
		}

		return formats;
	}

	private Size[] getPublicFormatSizes(int format, boolean output, boolean highRes) {
		try {
			checkArgumentFormatSupported(format, output);
		} catch (IllegalArgumentException e) {
			return null;
		}

		int internalFormat = imageFormatToInternal(format);
		int dataspace = imageFormatToDataspace(format);

		return getInternalFormatSizes(internalFormat, dataspace, output, highRes);
	}

	private Size[] getInternalFormatSizes(int format, int dataspace,
			boolean output, boolean highRes) {
		if (dataspace == HAL_DATASPACE_DEPTH && highRes) {
			return new Size[0];
		}

		SparseIntArray formatsMap =
				!output ? mInputFormats :
				dataspace == HAL_DATASPACE_DEPTH ? mDepthOutputFormats :
				dataspace == HAL_DATASPACE_DYNAMIC_DEPTH ? mDynamicDepthOutputFormats :
				dataspace == HAL_DATASPACE_HEIF ? mHeicOutputFormats :
				dataspace == HAL_DATASPACE_JPEG_R ? mJpegROutputFormats :
				highRes ? mHighResOutputFormats :
				mOutputFormats;
		boolean isDataSpaceHeifUltraHDR = false;
		if (CAMERA_HEIF_GAINMAP) {
			if (dataspace == DataSpace.DATASPACE_HEIF_ULTRAHDR) {
				formatsMap = mHeicUltraHDROutputFormats;
				isDataSpaceHeifUltraHDR = true;
			}
		}
		int sizesCount = formatsMap.get(format);
		if ( ((!output || (dataspace == HAL_DATASPACE_DEPTH || dataspace == HAL_DATASPACE_JPEG_R ||
							dataspace == HAL_DATASPACE_DYNAMIC_DEPTH ||
							dataspace == HAL_DATASPACE_HEIF ||
							isDataSpaceHeifUltraHDR)) && sizesCount == 0) ||
				(output && (dataspace != HAL_DATASPACE_DEPTH && dataspace != HAL_DATASPACE_JPEG_R &&
							dataspace != HAL_DATASPACE_DYNAMIC_DEPTH &&
							!isDataSpaceHeifUltraHDR &&
							dataspace != HAL_DATASPACE_HEIF) &&
				 mAllOutputFormats.get(format) == 0)) {
			return null;
		}

		Size[] sizes = new Size[sizesCount];
		int sizeIndex = 0;

		StreamConfiguration[] configurations =
				(dataspace == HAL_DATASPACE_DEPTH) ? mDepthConfigurations :
				(dataspace == HAL_DATASPACE_DYNAMIC_DEPTH) ? mDynamicDepthConfigurations :
				(dataspace == HAL_DATASPACE_HEIF) ? mHeicConfigurations :
				(dataspace == HAL_DATASPACE_JPEG_R) ? mJpegRConfigurations :
				(isDataSpaceHeifUltraHDR) ? mHeicUltraHDRConfigurations :
				mConfigurations;
		StreamConfigurationDuration[] minFrameDurations =
				(dataspace == HAL_DATASPACE_DEPTH) ? mDepthMinFrameDurations :
				(dataspace == HAL_DATASPACE_DYNAMIC_DEPTH) ? mDynamicDepthMinFrameDurations :
				(dataspace == HAL_DATASPACE_HEIF) ? mHeicMinFrameDurations :
				(dataspace == HAL_DATASPACE_JPEG_R) ? mJpegRMinFrameDurations :
				(isDataSpaceHeifUltraHDR) ? mHeicUltraHDRMinFrameDurations :
				mMinFrameDurations;

		for (StreamConfiguration config : configurations) {
			int fmt = config.getFormat();
			if (fmt == format && config.isOutput() == output) {
				if (output && mListHighResolution) {
					long duration = 0;
					for (int i = 0; i < minFrameDurations.length; i++) {
						StreamConfigurationDuration d = minFrameDurations[i];
						if (d.getFormat() == fmt &&
								d.getWidth() == config.getSize().getWidth() &&
								d.getHeight() == config.getSize().getHeight()) {
							duration = d.getDuration();
							break;
						}
					}
					if (dataspace != HAL_DATASPACE_DEPTH &&
							highRes != (duration > DURATION_20FPS_NS)) {
						continue;
					}
				}
				sizes[sizeIndex++] = config.getSize();
			}
		}

		if ((sizeIndex != sizesCount) && (dataspace == HAL_DATASPACE_DYNAMIC_DEPTH ||
				dataspace == HAL_DATASPACE_HEIF) || (dataspace == HAL_DATASPACE_JPEG_R) ||
				isDataSpaceHeifUltraHDR) {
			if (sizeIndex > sizesCount) {
				throw new AssertionError(
						"Too many dynamic depth sizes (expected " + sizesCount + ", actual " +
						sizeIndex + ")");
			}

			if (sizeIndex <= 0) {
				sizes = new Size[0];
			} else {
				sizes = Arrays.copyOf(sizes, sizeIndex);
			}
		} else if (sizeIndex != sizesCount) {
			throw new AssertionError(
					"Too few sizes (expected " + sizesCount + ", actual " + sizeIndex + ")");
		}

		return sizes;
	}

	private int[] getPublicFormats(boolean output) {
		int[] formats = new int[getPublicFormatCount(output)];

		int i = 0;

		SparseIntArray map = getFormatsMap(output);
		for (int j = 0; j < map.size(); j++) {
			int format = map.keyAt(j);
			formats[i++] = imageFormatToPublic(format);
		}
		if (output) {
			for (int j = 0; j < mDepthOutputFormats.size(); j++) {
				formats[i++] = depthFormatToPublic(mDepthOutputFormats.keyAt(j));
			}
			if (mDynamicDepthOutputFormats.size() > 0) {
				formats[i++] = ImageFormat.DEPTH_JPEG;
			}
			if (mHeicOutputFormats.size() > 0) {
				formats[i++] = ImageFormat.HEIC;
			}
			if (CAMERA_HEIF_GAINMAP) {
				if (mHeicUltraHDROutputFormats.size() > 0) {
					formats[i++] = ImageFormat.HEIC_ULTRAHDR;
				}
			}
			if (mJpegROutputFormats.size() > 0) {
				formats[i++] = ImageFormat.JPEG_R;
			}
		}
		if (formats.length != i) {
			throw new AssertionError("Too few formats " + i + ", expected " + formats.length);
		}

		return formats;
	}

	private SparseIntArray getFormatsMap(boolean output) {
		return output ? mAllOutputFormats : mInputFormats;
	}

	private long getInternalFormatDuration(int format, int dataspace, Size size, int duration) {
		if (!isSupportedInternalConfiguration(format, dataspace, size)) {
			throw new IllegalArgumentException("size was not supported");
		}

		StreamConfigurationDuration[] durations = getDurations(duration, dataspace);

		for (StreamConfigurationDuration configurationDuration : durations) {
			if (configurationDuration.getFormat() == format &&
					configurationDuration.getWidth() == size.getWidth() &&
					configurationDuration.getHeight() == size.getHeight()) {
				return configurationDuration.getDuration();
			}
		}
		return 0;
	}

	private StreamConfigurationDuration[] getDurations(int duration, int dataspace) {
		boolean isDataSpaceHeifUltraHDR = false;
		if (CAMERA_HEIF_GAINMAP) {
			if (dataspace == DataSpace.DATASPACE_HEIF_ULTRAHDR) {
				isDataSpaceHeifUltraHDR = true;
			}
		}
		switch (duration) {
			case DURATION_MIN_FRAME:
				return (dataspace == HAL_DATASPACE_DEPTH) ? mDepthMinFrameDurations :
						(dataspace == HAL_DATASPACE_DYNAMIC_DEPTH) ?
						mDynamicDepthMinFrameDurations :
						(dataspace == HAL_DATASPACE_HEIF) ? mHeicMinFrameDurations :
						isDataSpaceHeifUltraHDR ? mHeicUltraHDRMinFrameDurations :
						(dataspace == HAL_DATASPACE_JPEG_R) ? mJpegRMinFrameDurations :
						mMinFrameDurations;

			case DURATION_STALL:
				return (dataspace == HAL_DATASPACE_DEPTH) ? mDepthStallDurations :
						(dataspace == HAL_DATASPACE_DYNAMIC_DEPTH) ? mDynamicDepthStallDurations :
						(dataspace == HAL_DATASPACE_HEIF) ? mHeicStallDurations :
						isDataSpaceHeifUltraHDR ? mHeicUltraHDRStallDurations :
						(dataspace == HAL_DATASPACE_JPEG_R) ? mJpegRStallDurations :
						mStallDurations;
			default:
				throw new IllegalArgumentException("duration was invalid");
		}
	}

	private int getPublicFormatCount(boolean output) {
		SparseIntArray formatsMap = getFormatsMap(output);
		int size = formatsMap.size();
		if (output) {
			size += mDepthOutputFormats.size();
			size += mDynamicDepthOutputFormats.size();
			size += mHeicOutputFormats.size();
			size += mJpegROutputFormats.size();
			size += mHeicUltraHDROutputFormats.size();
		}

		return size;
	}

	private static <T> boolean arrayContains(T[] array, T element) {
		if (array == null) {
			return false;
		}

		for (T el : array) {
			if (Objects.equals(el, element)) {
				return true;
			}
		}

		return false;
	}

	private boolean isSupportedInternalConfiguration(int format, int dataspace, Size size) {
		boolean isDataSpaceHeifUltraHDR = false;
		if (CAMERA_HEIF_GAINMAP) {
			if (dataspace == DataSpace.DATASPACE_HEIF_ULTRAHDR) {
				isDataSpaceHeifUltraHDR = true;
			}
		}
		StreamConfiguration[] configurations =
				(dataspace == HAL_DATASPACE_DEPTH) ? mDepthConfigurations :
				(dataspace == HAL_DATASPACE_DYNAMIC_DEPTH) ? mDynamicDepthConfigurations :
				(dataspace == HAL_DATASPACE_HEIF) ? mHeicConfigurations :
				(dataspace == HAL_DATASPACE_JPEG_R) ? mJpegRConfigurations :
				isDataSpaceHeifUltraHDR ? mHeicUltraHDRConfigurations :
				mConfigurations;

		for (int i = 0; i < configurations.length; i++) {
			if (configurations[i].getFormat() == format &&
					configurations[i].getSize().equals(size)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("StreamConfiguration(");
		appendOutputsString(sb);
		sb.append(", ");
		appendHighResOutputsString(sb);
		sb.append(", ");
		appendInputsString(sb);
		sb.append(", ");
		appendValidOutputFormatsForInputString(sb);
		sb.append(", ");
		appendHighSpeedVideoConfigurationsString(sb);
		sb.append(")");

		return sb.toString();
	}

	public static int compareSizes(int widthA, int heightA, int widthB, int heightB) {
		long left = widthA * (long) heightA;
		long right = widthB * (long) heightB;
		if (left == right) {
			left = widthA;
			right = widthB;
		}
		return (left < right) ? -1 : (left > right ? 1 : 0);
	}

	private void appendOutputsString(StringBuilder sb) {
		sb.append("Outputs(");
		int[] formats = getOutputFormats();
		for (int format : formats) {
			Size[] sizes = getOutputSizes(format);
			for (Size size : sizes) {
				long minFrameDuration = getOutputMinFrameDuration(format, size);
				long stallDuration = getOutputStallDuration(format, size);
				sb.append(String.format("[w:%d, h:%d, format:%s(%d), min_duration:%d, " +
						"stall:%d], ", size.getWidth(), size.getHeight(), formatToString(format),
						format, minFrameDuration, stallDuration));
			}
		}
		if (sb.charAt(sb.length() - 1) == ' ') {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(")");
	}

	private void appendHighResOutputsString(StringBuilder sb) {
		sb.append("HighResolutionOutputs(");
		int[] formats = getOutputFormats();
		for (int format : formats) {
			Size[] sizes = getHighResolutionOutputSizes(format);
			if (sizes == null) continue;
			for (Size size : sizes) {
				long minFrameDuration = getOutputMinFrameDuration(format, size);
				long stallDuration = getOutputStallDuration(format, size);
				sb.append(String.format("[w:%d, h:%d, format:%s(%d), min_duration:%d, " +
						"stall:%d], ", size.getWidth(), size.getHeight(), formatToString(format),
						format, minFrameDuration, stallDuration));
			}
		}
		if (sb.charAt(sb.length() - 1) == ' ') {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(")");
	}

	private void appendInputsString(StringBuilder sb) {
		sb.append("Inputs(");
		int[] formats = getInputFormats();
		for (int format : formats) {
			Size[] sizes = getInputSizes(format);
			for (Size size : sizes) {
				sb.append(String.format("[w:%d, h:%d, format:%s(%d)], ", size.getWidth(),
						size.getHeight(), formatToString(format), format));
			}
		}
		if (sb.charAt(sb.length() - 1) == ' ') {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(")");
	}

	private void appendValidOutputFormatsForInputString(StringBuilder sb) {
		sb.append("ValidOutputFormatsForInput(");
		int[] inputFormats = getInputFormats();
		for (int inputFormat : inputFormats) {
			sb.append(String.format("[in:%s(%d), out:", formatToString(inputFormat), inputFormat));
			int[] outputFormats = getValidOutputFormatsForInput(inputFormat);
			for (int i = 0; i < outputFormats.length; i++) {
				sb.append(String.format("%s(%d)", formatToString(outputFormats[i]),
						outputFormats[i]));
				if (i < outputFormats.length - 1) {
					sb.append(", ");
				}
			}
			sb.append("], ");
		}
		if (sb.charAt(sb.length() - 1) == ' ') {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(")");
	}

	private void appendHighSpeedVideoConfigurationsString(StringBuilder sb) {
		sb.append("HighSpeedVideoConfigurations(");
		Size[] sizes = getHighSpeedVideoSizes();
		for (Size size : sizes) {
			Range<Integer>[] ranges = getHighSpeedVideoFpsRangesFor(size);
			for (Range<Integer> range : ranges) {
				sb.append(String.format("[w:%d, h:%d, min_fps:%d, max_fps:%d], ", size.getWidth(),
						size.getHeight(), range.getLower(), range.getUpper()));
			}
		}
		if (sb.charAt(sb.length() - 1) == ' ') {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(")");
	}

	public static String formatToString(int format) {
		if (CAMERA_HEIF_GAINMAP) {
			if (format == ImageFormat.HEIC_ULTRAHDR) {
				return "HEIC_ULTRAHDR";
			}
		}
		switch (format) {
			case ImageFormat.YV12:
				return "YV12";
			case ImageFormat.YUV_420_888:
				return "YUV_420_888";
			case ImageFormat.NV21:
				return "NV21";
			case ImageFormat.NV16:
				return "NV16";
			case PixelFormat.RGB_565:
				return "RGB_565";
			case PixelFormat.RGBA_8888:
				return "RGBA_8888";
			case PixelFormat.RGBX_8888:
				return "RGBX_8888";
			case PixelFormat.RGB_888:
				return "RGB_888";
			case ImageFormat.JPEG:
				return "JPEG";
			case ImageFormat.YUY2:
				return "YUY2";
			case ImageFormat.Y8:
				return "Y8";
			case ImageFormat.Y16:
				return "Y16";
			case ImageFormat.RAW_SENSOR:
				return "RAW_SENSOR";
			case ImageFormat.RAW_PRIVATE:
				return "RAW_PRIVATE";
			case ImageFormat.RAW10:
				return "RAW10";
			case ImageFormat.DEPTH16:
				return "DEPTH16";
			case ImageFormat.DEPTH_POINT_CLOUD:
				return "DEPTH_POINT_CLOUD";
			case ImageFormat.DEPTH_JPEG:
				return "DEPTH_JPEG";
			case ImageFormat.RAW_DEPTH:
				return "RAW_DEPTH";
			case ImageFormat.RAW_DEPTH10:
				return "RAW_DEPTH10";
			case ImageFormat.PRIVATE:
				return "PRIVATE";
			case ImageFormat.HEIC:
				return "HEIC";
			case ImageFormat.JPEG_R:
				return "JPEG/R";
			default:
				return "UNKNOWN";
		}
	}

	private static final int HAL_PIXEL_FORMAT_RGBA_8888 = 0x01;
	private static final int HAL_PIXEL_FORMAT_RGBX_8888 = 0x02;
	private static final int HAL_PIXEL_FORMAT_RGB_888 = 0x03;
	private static final int HAL_PIXEL_FORMAT_RGB_565 = 0x04;
	private static final int HAL_PIXEL_FORMAT_YCBCR_422_SP = 0x10;
	private static final int HAL_PIXEL_FORMAT_YCRCB_420_SP = 0x11;
	private static final int HAL_PIXEL_FORMAT_YCBCR_422_I = 0x14;
	private static final int HAL_PIXEL_FORMAT_RAW16 = 0x20;
	public static final int HAL_PIXEL_FORMAT_BLOB = 0x21;
	private static final int HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED = 0x22;
	private static final int HAL_PIXEL_FORMAT_YCBCR_420_888 = 0x23;
	private static final int HAL_PIXEL_FORMAT_RAW_OPAQUE = 0x24;
	private static final int HAL_PIXEL_FORMAT_RAW10 = 0x25;
	private static final int HAL_PIXEL_FORMAT_RAW12 = 0x26;
	private static final int HAL_PIXEL_FORMAT_YCBCR_P010 = 0x36;
	private static final int HAL_PIXEL_FORMAT_YCBCR_P210 = 0x3c;
	private static final int HAL_PIXEL_FORMAT_Y8 = 0x20203859;
	private static final int HAL_PIXEL_FORMAT_Y16 = 0x20363159;
	private static final int HAL_PIXEL_FORMAT_YV12 = 0x32315659;

	private static final int HAL_DATASPACE_STANDARD_SHIFT = 16;
	private static final int HAL_DATASPACE_TRANSFER_SHIFT = 22;
	private static final int HAL_DATASPACE_RANGE_SHIFT = 27;

	private static final int HAL_DATASPACE_UNKNOWN = 0x0;

	public static final int HAL_DATASPACE_ARBITRARY = 0x1;

	public static final int HAL_DATASPACE_V0_JFIF =
			(2 << HAL_DATASPACE_STANDARD_SHIFT) |
			(3 << HAL_DATASPACE_TRANSFER_SHIFT) |
			(1 << HAL_DATASPACE_RANGE_SHIFT);

	public static final int HAL_DATASPACE_DEPTH = 0x1000;
	public static final int HAL_DATASPACE_DYNAMIC_DEPTH = 0x1002;
	public static final int HAL_DATASPACE_HEIF = 0x1004;
	public static final int HAL_DATASPACE_JPEG_R = 0x1005;
	public static final int HAL_DATASPACE_JFIF = 0x8C20000;
	private static final long DURATION_20FPS_NS = 50000000L;
	private static final int DURATION_MIN_FRAME = 0;
	private static final int DURATION_STALL = 1;

	private final StreamConfiguration[] mConfigurations;
	private final StreamConfigurationDuration[] mMinFrameDurations;
	private final StreamConfigurationDuration[] mStallDurations;

	private final StreamConfiguration[] mDepthConfigurations;
	private final StreamConfigurationDuration[] mDepthMinFrameDurations;
	private final StreamConfigurationDuration[] mDepthStallDurations;

	private final StreamConfiguration[] mDynamicDepthConfigurations;
	private final StreamConfigurationDuration[] mDynamicDepthMinFrameDurations;
	private final StreamConfigurationDuration[] mDynamicDepthStallDurations;

	private final StreamConfiguration[] mHeicConfigurations;
	private final StreamConfigurationDuration[] mHeicMinFrameDurations;
	private final StreamConfigurationDuration[] mHeicStallDurations;

	private final StreamConfiguration[] mHeicUltraHDRConfigurations;
	private final StreamConfigurationDuration[] mHeicUltraHDRMinFrameDurations;
	private final StreamConfigurationDuration[] mHeicUltraHDRStallDurations;

	private final StreamConfiguration[] mJpegRConfigurations;
	private final StreamConfigurationDuration[] mJpegRMinFrameDurations;
	private final StreamConfigurationDuration[] mJpegRStallDurations;

	private final HighSpeedVideoConfiguration[] mHighSpeedVideoConfigurations;
	private final ReprocessFormatsMap mInputOutputFormatsMap;

	private final boolean mListHighResolution;

	private final SparseIntArray mOutputFormats = new SparseIntArray();
	private final SparseIntArray mHighResOutputFormats = new SparseIntArray();
	private final SparseIntArray mAllOutputFormats = new SparseIntArray();
	private final SparseIntArray mInputFormats = new SparseIntArray();
	private final SparseIntArray mDepthOutputFormats = new SparseIntArray();
	private final SparseIntArray mDynamicDepthOutputFormats = new SparseIntArray();
	private final SparseIntArray mHeicOutputFormats = new SparseIntArray();
	private final SparseIntArray mHeicUltraHDROutputFormats = new SparseIntArray();
	private final SparseIntArray mJpegROutputFormats = new SparseIntArray();

	private final HashMap</*HighSpeedVideoSize*/Size, /*Count*/Integer> mHighSpeedVideoSizeMap =
			new HashMap<Size, Integer>();
	private final HashMap</*HighSpeedVideoFpsRange*/Range<Integer>, /*Count*/Integer>
			mHighSpeedVideoFpsRangeMap = new HashMap<Range<Integer>, Integer>();

}
