package android.hardware.camera2.params;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/* AOSP StreamConfigurationMap, reduced to the color stream configurations. ATL's CameraManager lists no camera,
 * so no CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP value ever arises (AOSP's public constructor takes
 * StreamConfiguration/StreamConfigurationDuration objects that ATL does not have; this one takes the flat
 * int/long form of the camera metadata). Not carried, and answered as AOSP answers a map without them: depth,
 * dynamic depth, HEIC and JPEG/R streams, stall durations (0, AOSP's default), the reprocess map (no valid
 * output formats for an input) and high speed video configurations (none). */
public final class StreamConfigurationMap {
	private static final long DURATION_20FPS_NS = 50000000L;
	private static final int HAL_PIXEL_FORMAT_BLOB = 0x21;
	private static final int HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED = 0x22;
	private static final int HAL_PIXEL_FORMAT_RAW16 = 0x20;
	private static final int HAL_PIXEL_FORMAT_RAW10 = 0x25;
	private static final int HAL_PIXEL_FORMAT_Y16 = 0x20363159;
	/* ImageFormat.JPEG */
	private static final int IMAGE_FORMAT_JPEG = 0x100;
	private static final int DURATION_MIN_FRAME = 0;
	private static final int DURATION_STALL = 1;

	private final int[] mConfigurations;      /* {format, width, height, isInput} per stream, as in the metadata */
	private final long[] mMinFrameDurations;  /* {format, width, height, durationNs} */
	private final boolean mListHighResolution;
	private final SparseIntArray mAllOutputFormats = new SparseIntArray();
	private final SparseIntArray mInputFormats = new SparseIntArray();

	StreamConfigurationMap(int[] configurations, long[] minFrameDurations, boolean listHighResolution) {
		if (configurations == null || minFrameDurations == null) {
			throw new NullPointerException("configurations must not be null");
		}
		mConfigurations = configurations;
		mMinFrameDurations = minFrameDurations;
		mListHighResolution = listHighResolution;
		/* AOSP constructor: for each format, count the output sizes and the input sizes */
		for (int i = 0; i + 3 < configurations.length; i += 4) {
			int fmt = configurations[i];
			if (configurations[i + 3] == 0) {
				mAllOutputFormats.put(fmt, mAllOutputFormats.get(fmt) + 1);
			} else {
				mInputFormats.put(fmt, mInputFormats.get(fmt) + 1);
			}
		}
	}

	/* AOSP getOutputFormats() -> getPublicFormats(true): the internal BLOB format is reported as ImageFormat.JPEG */
	public int[] getOutputFormats() {
		return getPublicFormats(mAllOutputFormats);
	}

	/* AOSP: without a reprocess map (mInputOutputFormatsMap == null) no output format is valid for an input */
	public int[] getValidOutputFormatsForInput(int inputFormat) {
		return new int[0];
	}

	public int[] getInputFormats() {
		return getPublicFormats(mInputFormats);
	}

	public Size[] getInputSizes(final int format) {
		return getPublicFormatSizes(format, false, false);
	}

	public boolean isOutputSupportedFor(int format) {
		checkArgumentFormat(format);
		if (hasOwnDataspace(format))
			return false;
		return mAllOutputFormats.indexOfKey(imageFormatToInternal(format)) >= 0;
	}

	/* AOSP compares against ImageReader, MediaRecorder, MediaCodec, Allocation, SurfaceHolder and SurfaceTexture */
	public static <T> boolean isOutputSupportedFor(Class<T> klass) {
		Objects.requireNonNull(klass, "klass must not be null");
		switch (klass.getName()) {
			case "android.media.ImageReader":
			case "android.media.MediaRecorder":
			case "android.media.MediaCodec":
			case "android.renderscript.Allocation":
			case "android.view.SurfaceHolder":
			case "android.graphics.SurfaceTexture":
				return true;
			default:
				return false;
		}
	}

	/* AOSP matches the size/format/dataspace of the surface (SurfaceUtils, from its native window) against the output
	 * configurations; an ATL Surface has no such native window, so none matches. */
	public boolean isOutputSupportedFor(Surface surface) {
		Objects.requireNonNull(surface, "surface must not be null");
		if (!surface.isValid())
			throw new IllegalArgumentException("Surface is not valid");
		return false;
	}

	/* AOSP getOutputSizes(Class) -> isOutputSupportedFor(Class), else null; then the IMPLEMENTATION_DEFINED sizes. */
	public <T> Size[] getOutputSizes(Class<T> klass) {
		if (!isOutputSupportedFor(klass)) {
			return null;
		}
		return getInternalFormatSizes(HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED, true, false);
	}

	/* AOSP getOutputSizes(int) -> getPublicFormatSizes(format, output, highRes false) */
	public Size[] getOutputSizes(int format) {
		return getPublicFormatSizes(format, true, false);
	}

	/* AOSP: the maps of high speed video sizes and fps ranges are empty without such configurations */
	public Size[] getHighSpeedVideoSizes() {
		return new Size[0];
	}

	public Range<Integer>[] getHighSpeedVideoFpsRangesFor(Size size) {
		throw new IllegalArgumentException(String.format("Size %s does not support high speed video recording", size));
	}

	@SuppressWarnings("unchecked")
	public Range<Integer>[] getHighSpeedVideoFpsRanges() {
		return new Range[0];
	}

	public Size[] getHighSpeedVideoSizesFor(Range<Integer> fpsRange) {
		throw new IllegalArgumentException(String.format("FpsRange %s does not support high speed video recording", fpsRange));
	}

	/* AOSP getHighResolutionOutputSizes(): null unless the map lists high resolution streams separately */
	public Size[] getHighResolutionOutputSizes(int format) {
		if (!mListHighResolution)
			return null;
		return getPublicFormatSizes(format, true, true);
	}

	public long getOutputMinFrameDuration(int format, Size size) {
		Objects.requireNonNull(size, "size must not be null");
		checkArgumentFormatSupported(format, true);
		return getInternalFormatDuration(imageFormatToInternal(format), size, DURATION_MIN_FRAME);
	}

	public <T> long getOutputMinFrameDuration(final Class<T> klass, final Size size) {
		if (!isOutputSupportedFor(klass)) {
			throw new IllegalArgumentException("klass was not supported");
		}
		return getInternalFormatDuration(HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED, size, DURATION_MIN_FRAME);
	}

	public long getOutputStallDuration(int format, Size size) {
		checkArgumentFormatSupported(format, true);
		return getInternalFormatDuration(imageFormatToInternal(format), size, DURATION_STALL);
	}

	public <T> long getOutputStallDuration(final Class<T> klass, final Size size) {
		if (!isOutputSupportedFor(klass)) {
			throw new IllegalArgumentException("klass was not supported");
		}
		return getInternalFormatDuration(HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED, size, DURATION_STALL);
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
			final StreamConfigurationMap other = (StreamConfigurationMap)obj;
			return Arrays.equals(mConfigurations, other.mConfigurations) && Arrays.equals(mMinFrameDurations, other.mMinFrameDurations);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(mConfigurations) * 31 + Arrays.hashCode(mMinFrameDurations);
	}

	/* AOSP checkArgumentFormat(): the format has to be an ImageFormat or a PixelFormat public format */
	private static int checkArgumentFormat(int format) {
		if (!ImageFormat.isPublicFormat(format) && !PixelFormat.isPublicFormat(format)) {
			throw new IllegalArgumentException(String.format("format 0x%x was not defined in either ImageFormat or PixelFormat", format));
		}
		return format;
	}

	/* AOSP checkArgumentFormatSupported(): the format has to be one of getOutputFormats()/getInputFormats() */
	private int checkArgumentFormatSupported(int format, boolean output) {
		checkArgumentFormat(format);
		int internalFormat = imageFormatToInternal(format);
		boolean supported;
		if (output)
			supported = !hasOwnDataspace(format) && mAllOutputFormats.indexOfKey(internalFormat) >= 0;
		else
			supported = mInputFormats.indexOfKey(internalFormat) >= 0;
		if (!supported) {
			throw new IllegalArgumentException(String.format("format %x is not supported by this stream configuration map", format));
		}
		return format;
	}

	/* AOSP imageFormatToDataspace(): these public formats are streams of their own dataspace (depth, dynamic depth,
	 * HEIC, JPEG/R: DEPTH16 0x44363159, DEPTH_POINT_CLOUD 0x101, RAW_DEPTH 0x1002, RAW_DEPTH10 0x1003,
	 * DEPTH_JPEG 0x69656963, HEIC 0x48454946, JPEG_R 0x1005), none of which this map carries. */
	private static boolean hasOwnDataspace(int format) {
		switch (format) {
			case 0x44363159:
			case 0x101:
			case 0x1002:
			case 0x1003:
			case 0x69656963:
			case 0x48454946:
			case 0x1005:
				return true;
			default:
				return false;
		}
	}

	/* AOSP imageFormatToInternal() */
	private static int imageFormatToInternal(int format) {
		switch (format) {
			case IMAGE_FORMAT_JPEG:
			case 0x101:
			case 0x69656963:
			case 0x48454946:
			case 0x1005:
				return HAL_PIXEL_FORMAT_BLOB;
			case 0x44363159:
				return HAL_PIXEL_FORMAT_Y16;
			case 0x1002:
				return HAL_PIXEL_FORMAT_RAW16;
			case 0x1003:
				return HAL_PIXEL_FORMAT_RAW10;
			default:
				return format;
		}
	}

	/* AOSP getPublicFormats(): the internal BLOB format is reported as ImageFormat.JPEG */
	private static int[] getPublicFormats(SparseIntArray map) {
		int[] formats = new int[map.size()];
		for (int j = 0; j < formats.length; j++) {
			int format = map.keyAt(j);
			formats[j] = format == HAL_PIXEL_FORMAT_BLOB ? IMAGE_FORMAT_JPEG : format;
		}
		return formats;
	}

	/* AOSP getPublicFormatSizes(): a format that is not supported (checkArgumentFormatSupported throws) gives null */
	private Size[] getPublicFormatSizes(int format, boolean output, boolean highRes) {
		try {
			checkArgumentFormatSupported(format, output);
		} catch (IllegalArgumentException e) {
			return null;
		}
		return getInternalFormatSizes(imageFormatToInternal(format), output, highRes);
	}

	/* AOSP getInternalFormatSizes() for color streams: null if the format has no stream, else the sizes (for outputs
	 * with listHighResolution: those whose min frame duration is <= 50 ms, or > 50 ms for highRes) */
	private Size[] getInternalFormatSizes(int format, boolean output, boolean highRes) {
		if ((output ? mAllOutputFormats : mInputFormats).get(format) == 0)
			return null;
		ArrayList<Size> sizes = new ArrayList<Size>();
		for (int i = 0; i + 3 < mConfigurations.length; i += 4) {
			if (mConfigurations[i] != format || (mConfigurations[i + 3] == 0) != output)
				continue;
			int width = mConfigurations[i + 1];
			int height = mConfigurations[i + 2];
			if (output && mListHighResolution) {
				if (highRes != (minFrameDuration(format, width, height) > DURATION_20FPS_NS))
					continue;
			}
			sizes.add(new Size(width, height));
		}
		return sizes.toArray(new Size[sizes.size()]);
	}

	private long minFrameDuration(int format, int width, int height) {
		for (int d = 0; d + 3 < mMinFrameDurations.length; d += 4) {
			if (mMinFrameDurations[d] == format && mMinFrameDurations[d + 1] == width && mMinFrameDurations[d + 2] == height)
				return mMinFrameDurations[d + 3];
		}
		return 0;
	}

	/* AOSP getInternalFormatDuration(): the size has to be a configured one; a stream without a duration entry has the
	 * default '0' (unsupported/no extra stall), and stall durations are not carried at all */
	private long getInternalFormatDuration(int format, Size size, int duration) {
		boolean supported = false;
		for (int i = 0; i + 3 < mConfigurations.length; i += 4) {
			if (mConfigurations[i] == format && mConfigurations[i + 1] == size.getWidth() && mConfigurations[i + 2] == size.getHeight()) {
				supported = true;
				break;
			}
		}
		if (!supported) {
			throw new IllegalArgumentException("size was not supported");
		}
		if (duration == DURATION_STALL)
			return 0;
		return minFrameDuration(format, size.getWidth(), size.getHeight());
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

	private void appendOutputsString(StringBuilder sb) {
		sb.append("Outputs(");
		int[] formats = getOutputFormats();
		for (int format : formats) {
			Size[] sizes = getOutputSizes(format);
			for (Size size : sizes) {
				long minFrameDuration = getOutputMinFrameDuration(format, size);
				long stallDuration = getOutputStallDuration(format, size);
				sb.append(String.format("[w:%d, h:%d, format:%s(%d), min_duration:%d, stall:%d], ", size.getWidth(), size.getHeight(),
				                        formatToString(format), format, minFrameDuration, stallDuration));
			}
		}
		// Remove the pending ", "
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
			if (sizes == null)
				continue;
			for (Size size : sizes) {
				long minFrameDuration = getOutputMinFrameDuration(format, size);
				long stallDuration = getOutputStallDuration(format, size);
				sb.append(String.format("[w:%d, h:%d, format:%s(%d), min_duration:%d, stall:%d], ", size.getWidth(), size.getHeight(),
				                        formatToString(format), format, minFrameDuration, stallDuration));
			}
		}
		// Remove the pending ", "
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
				sb.append(String.format("[w:%d, h:%d, format:%s(%d)], ", size.getWidth(), size.getHeight(), formatToString(format), format));
			}
		}
		// Remove the pending ", "
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
				sb.append(String.format("%s(%d)", formatToString(outputFormats[i]), outputFormats[i]));
				if (i < outputFormats.length - 1) {
					sb.append(", ");
				}
			}
			sb.append("], ");
		}
		// Remove the pending ", "
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
				sb.append(String.format("[w:%d, h:%d, min_fps:%d, max_fps:%d], ", size.getWidth(), size.getHeight(), range.getLower(),
				                        range.getUpper()));
			}
		}
		// Remove the pending ", "
		if (sb.charAt(sb.length() - 1) == ' ') {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(")");
	}

	/* AOSP formatToString() (public in the source, @hide in the API) */
	public static String formatToString(int format) {
		switch (format) {
			case ImageFormat.YV12:
				return "YV12";
			case ImageFormat.YUV_420_888:
				return "YUV_420_888";
			case ImageFormat.NV21:
				return "NV21";
			case ImageFormat.NV16:
				return "NV16";
			case 4: /* PixelFormat.RGB_565 */
				return "RGB_565";
			case 1: /* PixelFormat.RGBA_8888 */
				return "RGBA_8888";
			case 2: /* PixelFormat.RGBX_8888 */
				return "RGBX_8888";
			case 3: /* PixelFormat.RGB_888 */
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
}
