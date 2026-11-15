package android.hardware.camera2.params;

import android.util.Size;
import android.util.SparseIntArray;
import java.util.ArrayList;

/* AOSP StreamConfigurationMap, reduced to the color stream configurations and the output size queries. ATL's
 * CameraManager lists no camera, so no CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP value ever arises
 * (AOSP's public constructor takes StreamConfiguration/StreamConfigurationDuration objects that ATL does not have;
 * this one takes the flat int/long form of the camera metadata). Not carried: depth, dynamic depth, HEIC, JPEG/R
 * and high speed video configurations, input/reprocess maps, other duration queries. */
public final class StreamConfigurationMap {
	private static final long DURATION_20FPS_NS = 50000000L;
	private static final int HAL_PIXEL_FORMAT_BLOB = 0x21;
	private static final int HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED = 0x22;
	/* ImageFormat.JPEG */
	private static final int IMAGE_FORMAT_JPEG = 0x100;

	private final int[] mConfigurations;      /* {format, width, height, isInput} per stream, as in the metadata */
	private final long[] mMinFrameDurations;  /* {format, width, height, durationNs} */
	private final boolean mListHighResolution;
	private final SparseIntArray mAllOutputFormats = new SparseIntArray();

	StreamConfigurationMap(int[] configurations, long[] minFrameDurations, boolean listHighResolution) {
		if (configurations == null || minFrameDurations == null) {
			throw new NullPointerException("configurations must not be null");
		}
		mConfigurations = configurations;
		mMinFrameDurations = minFrameDurations;
		mListHighResolution = listHighResolution;
		/* AOSP constructor: for each format, count the output sizes */
		for (int i = 0; i + 3 < configurations.length; i += 4) {
			if (configurations[i + 3] == 0) {
				int fmt = configurations[i];
				mAllOutputFormats.put(fmt, mAllOutputFormats.get(fmt) + 1);
			}
		}
	}

	/* AOSP getOutputFormats() -> getPublicFormats(true): the internal BLOB format is reported as ImageFormat.JPEG */
	public int[] getOutputFormats() {
		int[] formats = new int[mAllOutputFormats.size()];
		for (int j = 0; j < formats.length; j++) {
			int format = mAllOutputFormats.keyAt(j);
			formats[j] = format == HAL_PIXEL_FORMAT_BLOB ? IMAGE_FORMAT_JPEG : format;
		}
		return formats;
	}

	/* AOSP getOutputSizes(Class) -> isOutputSupportedFor(Class), else null; then the IMPLEMENTATION_DEFINED sizes.
	 * AOSP compares against ImageReader, MediaRecorder, MediaCodec, Allocation, SurfaceHolder and SurfaceTexture. */
	public <T> Size[] getOutputSizes(Class<T> klass) {
		switch (klass.getName()) {
			case "android.media.ImageReader":
			case "android.media.MediaRecorder":
			case "android.media.MediaCodec":
			case "android.renderscript.Allocation":
			case "android.view.SurfaceHolder":
			case "android.graphics.SurfaceTexture":
				return getInternalFormatSizes(HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED, false);
			default:
				return null;
		}
	}

	/* AOSP getOutputSizes(int) -> getPublicFormatSizes(format, output, highRes false) */
	public Size[] getOutputSizes(int format) {
		return getPublicFormatSizes(format, false);
	}

	/* AOSP getHighResolutionOutputSizes(): null unless the map lists high resolution streams separately */
	public Size[] getHighResolutionOutputSizes(int format) {
		if (!mListHighResolution)
			return null;
		return getPublicFormatSizes(format, true);
	}

	/* AOSP getPublicFormatSizes(): an unsupported or non-public format (checkArgumentFormatSupported throws) gives null.
	 * Only color streams exist here, so the public formats backed by depth/HEIC/JPEG-R streams (DEPTH16 0x44363159,
	 * DEPTH_POINT_CLOUD 0x101, RAW_DEPTH 0x1002, RAW_DEPTH10 0x1003, HEIC 0x48454946, DEPTH_JPEG 0x69656963,
	 * JPEG_R 0x1005, HEIC_ULTRAHDR 0x1006) have no sizes, and the internal BLOB (0x21) is no public format. */
	private Size[] getPublicFormatSizes(int format, boolean highRes) {
		switch (format) {
			case 0x44363159:
			case 0x101:
			case 0x1002:
			case 0x1003:
			case 0x48454946:
			case 0x69656963:
			case 0x1005:
			case 0x1006:
			case HAL_PIXEL_FORMAT_BLOB:
				return null;
		}
		return getInternalFormatSizes(format == IMAGE_FORMAT_JPEG ? HAL_PIXEL_FORMAT_BLOB : format, highRes);
	}

	/* AOSP getInternalFormatSizes() for output color streams: null if the format has no output stream, else the sizes
	 * (with listHighResolution: those whose min frame duration is <= 50 ms, or > 50 ms for highRes) */
	private Size[] getInternalFormatSizes(int format, boolean highRes) {
		if (mAllOutputFormats.get(format) == 0)
			return null;
		ArrayList<Size> sizes = new ArrayList<Size>();
		for (int i = 0; i + 3 < mConfigurations.length; i += 4) {
			if (mConfigurations[i] != format || mConfigurations[i + 3] != 0)
				continue;
			int width = mConfigurations[i + 1];
			int height = mConfigurations[i + 2];
			if (mListHighResolution) {
				long duration = 0;
				for (int d = 0; d + 3 < mMinFrameDurations.length; d += 4) {
					if (mMinFrameDurations[d] == format && mMinFrameDurations[d + 1] == width && mMinFrameDurations[d + 2] == height) {
						duration = mMinFrameDurations[d + 3];
						break;
					}
				}
				if (highRes != (duration > DURATION_20FPS_NS))
					continue;
			}
			sizes.add(new Size(width, height));
		}
		return sizes.toArray(new Size[sizes.size()]);
	}
}
