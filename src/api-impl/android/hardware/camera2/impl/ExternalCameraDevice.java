package android.hardware.camera2.impl;

import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfiguration;
import android.hardware.camera2.params.StreamConfigurationDuration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** @hide ATL's camera HAL: a PipeWire camera (native code in api-impl-jni/hardware) presented as AOSP's external
 * (USB) camera HAL presents a V4L2 camera, hardware/interfaces/camera/device/default ExternalCameraDevice and
 * ExternalCameraDeviceSession (android16-release): the same static metadata, request templates and capture
 * results. CameraDeviceImpl (camera2) and android.hardware.Camera (API 1, AOSP's Camera2Client) use it as AOSP's
 * camera service uses the HAL.
 *
 * Differences to the HAL, as the HAL's device configuration (external_camera_config.xml) is ATL's to choose:
 * no frame rate limits per size (the HAL's built-in defaults stop at 1920x1080), and every format ATL can read
 * (MJPEG and raw YUV/RGB) counts, where the HAL reads MJPEG only. Camera ids are the HAL's: the id offset 100
 * plus the camera's number (its place among the usable cameras in PipeWire's order, in place of the V4L2 node
 * number).
 * A camera whose sizes are all larger than 1280x720 also gets its smallest size halved until it fits, streamed at
 * that smallest size and scaled like the HAL's outputs: apps such as Open Camera drop preview sizes larger than
 * the window (and 1280x720), and an ATL window is smaller than a phone's display.
 *
 * Inside a Flatpak the cameras come through the camera portal, which asks the user. Without the user's answer ATL
 * knows only the portal's IsCameraPresent property: then it reports one camera (id 100, with the HAL's fixed facing
 * and orientation) and asks the portal when the app needs the camera itself, i.e. its characteristics (the stream
 * configurations come only through the portal's PipeWire remote) or a connection (openCamera(), Camera.open()). */
public final class ExternalCameraDevice {
	/* ExternalCameraUtils.cpp kDefaultCameraIdOffset, kDefaultJpegBufSize, kDefaultOrientation */
	private static final int CAMERA_ID_OFFSET = 100;
	private static final int MAX_JPEG_BUF_SIZE = 5 << 20;
	private static final int ORIENTATION = 0;
	/* ExternalCameraDeviceSession.h kMaxProcessedStream, kMaxStallStream */
	private static final int MAX_PROCESSED_STREAM = 2;
	private static final int MAX_STALL_STREAM = 1;
	/* system/core graphics-base: HAL_PIXEL_FORMAT_BLOB, _IMPLEMENTATION_DEFINED, _YCBCR_420_888 */
	private static final int HAL_PIXEL_FORMAT_BLOB = 0x21;
	private static final int HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED = 0x22;
	private static final int HAL_PIXEL_FORMAT_YCBCR_420_888 = 0x23;
	private static final int[] HAL_FORMATS = {HAL_PIXEL_FORMAT_BLOB, HAL_PIXEL_FORMAT_YCBCR_420_888, HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED};

	/** the client of an open camera: frames of a started stream, on the stream's thread (the frame is readable with
	 * copyFrame() meanwhile; the native loop's lock is held, so the client must not wait for a thread that calls
	 * this class); the end of the stream; and the eviction by a newer client (AOSP's camera service evicts the
	 * older client of the same process) */
	public interface FrameListener {
		void onFrame(long timestampNs);

		void onStreamError(String message);

		void onEvicted();
	}

	/* a size with its frame rates, ExternalCameraUtils SupportedV4L2Format */
	private static final class SupportedFormat {
		final int width;
		final int height;
		final List<int[]> frameRates = new ArrayList<int[]>(); /* {numerator, denominator} of frames per second */
		final SupportedFormat source; /* the camera's size this one is scaled from, or null */

		SupportedFormat(int width, int height) {
			this(width, height, null);
		}

		SupportedFormat(int width, int height, SupportedFormat source) {
			this.width = width;
			this.height = height;
			this.source = source;
			if (source != null)
				frameRates.addAll(source.frameRates);
		}

		float aspectRatio() {
			return (float)width / height;
		}
	}

	private static List<ExternalCameraDevice> sDevices; /* null until PipeWire is connected */
	private static Boolean sPresent;                    /* the portal's IsCameraPresent, read once */
	private static boolean sDenied;                     /* the user refused the camera portal */
	private static boolean sTriedSilently;

	private final int mIndex;
	private final String mId;
	private final List<SupportedFormat> mSupportedFormats;
	private final CameraCharacteristics mCharacteristics;
	private final Map<Integer, Map<CaptureRequest.Key<?>, Object>> mDefaultRequests = new HashMap<>();
	private volatile long mHandle;
	private FrameListener mListener;

	private ExternalCameraDevice(int index, int number, List<SupportedFormat> formats) {
		mIndex = index;
		mId = String.valueOf(CAMERA_ID_OFFSET + number);
		mSupportedFormats = formats;
		mCharacteristics = new CameraCharacteristics(initCameraCharacteristics());
		initDefaultRequests();
	}

	/** the cameras with a color stream, in id order (AOSP ExternalCameraProvider skips a camera whose
	 * characteristics fail, i.e. one without supported formats); with requestAccess, inside a Flatpak the camera
	 * portal asks the user if needed. Empty without a connection */
	public static synchronized List<ExternalCameraDevice> getDevices(boolean requestAccess) {
		/* without access: one silent try (the portal's remote opens without a dialog once the user allowed the app) */
		if (sDevices == null && !(sDenied && requestAccess) && (requestAccess || !sTriedSilently)) {
			sTriedSilently = true;
			int count = nativeInit(requestAccess);
			if (count == -2)
				sDenied = true;
			if (count == -1 && !new java.io.File("/.flatpak-info").exists())
				count = 0; /* no PipeWire to connect to: no camera (asked once) */
			if (count >= 0) {
				sDevices = new ArrayList<ExternalCameraDevice>();
				for (int i = 0; i < count; i++) {
					List<SupportedFormat> formats = initSupportedFormats(nativeGetModes(i));
					if (!formats.isEmpty())
						sDevices.add(new ExternalCameraDevice(i, sDevices.size(), formats));
				}
			}
		}
		return sDevices != null ? sDevices : java.util.Collections.<ExternalCameraDevice>emptyList();
	}

	/** the camera ids without asking the user: the cameras if PipeWire is connected, else, inside a Flatpak, one
	 * camera (the first id) if the portal's IsCameraPresent says there is one and the user did not refuse */
	public static synchronized String[] getCameraIds() {
		List<ExternalCameraDevice> devices = getDevices(false);
		if (sDevices == null && !sDenied && new java.io.File("/.flatpak-info").exists()) {
			if (sPresent == null)
				sPresent = nativeIsCameraPresent();
			if (sPresent)
				return new String[] {String.valueOf(CAMERA_ID_OFFSET)};
		}
		String[] ids = new String[devices.size()];
		for (int i = 0; i < ids.length; i++)
			ids[i] = devices.get(i).mId;
		return ids;
	}

	/** the user refused the camera portal (inside a Flatpak) */
	public static synchronized boolean accessDenied() {
		return sDenied;
	}

	/** a camera can be used: one is present and the user did not switch the camera off for the app */
	public static boolean available() {
		return !android.content.pm.PackageManager.deniedByUser("android.permission.CAMERA") && getCameraIds().length > 0;
	}

	/** the camera with this id (null if there is none); asks the camera portal if needed */
	public static ExternalCameraDevice get(String cameraId) {
		for (ExternalCameraDevice device : getDevices(true))
			if (device.mId.equals(cameraId))
				return device;
		return null;
	}

	/** android.lens.facing and android.sensor.orientation of every camera of the HAL (initDefaultCharsKeys()) */
	public static int getLensFacing() {
		return CameraMetadata.LENS_FACING_EXTERNAL;
	}

	public static int getSensorOrientation() {
		return ORIENTATION;
	}

	public String getId() {
		return mId;
	}

	public String getName() {
		return nativeGetName(mIndex);
	}

	public CameraCharacteristics getCharacteristics() {
		return mCharacteristics;
	}

	/* ExternalCameraDevice::initSupportedFormatsLocked(): the formats of the cropping type that keeps the
	 * largest size (without the HAL's fps limits, see the class comment) */
	private static List<SupportedFormat> initSupportedFormats(int[] modes) {
		Map<Long, SupportedFormat> bySize = new HashMap<Long, SupportedFormat>();
		for (int i = 0; i + 3 < modes.length; i += 4) {
			int width = modes[i], height = modes[i + 1], num = modes[i + 2], den = modes[i + 3];
			/* getCandidateSupportedFormatsLocked(): no h > w sizes; a size needs a frame rate */
			if (height > width || num <= 0 || den <= 0)
				continue;
			long key = ((long)width << 32) | height;
			SupportedFormat format = bySize.get(key);
			if (format == null)
				bySize.put(key, format = new SupportedFormat(width, height));
			boolean known = false;
			for (int[] rate : format.frameRates)
				known |= (long)rate[0] * den == (long)num * rate[1];
			if (!known)
				format.frameRates.add(new int[] {num, den});
		}
		return addScaledFormats(chooseSupportedFormats(bySize.values()));
	}

	/* see the class comment: the smallest size halved (even sizes) until it fits into 1280x720, if none fits */
	private static List<SupportedFormat> addScaledFormats(List<SupportedFormat> formats) {
		if (formats.isEmpty())
			return formats;
		for (SupportedFormat format : formats)
			if (format.width <= 1280 && format.height <= 720)
				return formats;
		SupportedFormat smallest = formats.get(0);
		List<SupportedFormat> scaled = new ArrayList<SupportedFormat>();
		int width = smallest.width, height = smallest.height;
		while (width > 1280 || height > 720) {
			width = (width / 2) & ~1;
			height = (height / 2) & ~1;
			if (width <= 0 || height <= 0)
				return formats;
			scaled.add(0, new SupportedFormat(width, height, smallest));
		}
		scaled.addAll(formats);
		return scaled;
	}

	private static List<SupportedFormat> chooseSupportedFormats(Collection<SupportedFormat> sizes) {
		List<SupportedFormat> horizontal = trimSupportedFormats(false, sizes);
		List<SupportedFormat> vertical = trimSupportedFormats(true, sizes);
		if (horizontal.isEmpty())
			return vertical;
		if (vertical.isEmpty())
			return horizontal;
		SupportedFormat maxHori = horizontal.get(horizontal.size() - 1);
		SupportedFormat maxVert = vertical.get(vertical.size() - 1);
		if (maxHori.width == maxVert.width && maxHori.height == maxVert.height)
			return horizontal.size() > vertical.size() ? horizontal : vertical;
		if (maxHori.width >= maxVert.width && maxHori.height >= maxVert.height)
			return horizontal;
		if (maxHori.width <= maxVert.width && maxHori.height <= maxVert.height)
			return vertical;
		return horizontal.size() > vertical.size() ? horizontal : vertical;
	}

	/* ExternalCameraDevice::trimSupportedFormats(): sorted, without sizes that cannot be cropped from the largest */
	private static List<SupportedFormat> trimSupportedFormats(boolean vertical, Collection<SupportedFormat> formats) {
		List<SupportedFormat> sorted = new ArrayList<SupportedFormat>(formats);
		if (vertical)
			sorted.sort((a, b) -> a.width == b.width ? Integer.compare(a.height, b.height) : Integer.compare(a.width, b.width));
		else
			sorted.sort((a, b) -> a.height == b.height ? Integer.compare(a.width, b.width) : Integer.compare(a.height, b.height));
		if (sorted.isEmpty())
			return sorted;
		float maxSizeAr = sorted.get(sorted.size() - 1).aspectRatio();
		List<SupportedFormat> out = new ArrayList<SupportedFormat>();
		for (SupportedFormat fmt : sorted) {
			float ar = fmt.aspectRatio();
			/* ExternalCameraUtils isAspectRatioClose(): kAspectRatioMatchThres 0.01 */
			if (Math.abs(ar - maxSizeAr) < 0.01f || (!vertical && ar < maxSizeAr) || (vertical && ar > maxSizeAr))
				out.add(fmt);
		}
		return out;
	}

	private static double fps(int[] rate) {
		return (double)rate[0] / rate[1];
	}

	private static String[] names(CaptureRequest.Key<?>... keys) {
		String[] names = new String[keys.length];
		for (int i = 0; i < keys.length; i++)
			names[i] = keys[i].getName();
		return names;
	}

	private static String[] names(CaptureResult.Key<?>... keys) {
		String[] names = new String[keys.length];
		for (int i = 0; i < keys.length; i++)
			names[i] = keys[i].getName();
		return names;
	}

	/* ExternalCameraDevice::initCameraCharacteristics(): initDefaultCharsKeys(), initCameraControlsCharsKeys(),
	 * initOutputCharsKeys(), initAvailableCapabilities(); as the public keys AOSP's CameraMetadataNative makes of
	 * them (e.g. CONTROL_MAX_REGIONS split into CONTROL_MAX_REGIONS_AE/AWB/AF) */
	private Map<CameraCharacteristics.Key<?>, Object> initCameraCharacteristics() {
		Map<CameraCharacteristics.Key<?>, Object> md = new HashMap<CameraCharacteristics.Key<?>, Object>();
		/* initDefaultCharsKeys() */
		md.put(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL);
		md.put(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES, new int[] {CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_OFF});
		md.put(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES, new int[] {CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO});
		md.put(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 0);
		md.put(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 0);
		md.put(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 0);
		md.put(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES, new int[] {CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF});
		md.put(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES, new int[] {CameraMetadata.CONTROL_AWB_MODE_AUTO});
		md.put(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, new int[] {CameraMetadata.CONTROL_AE_MODE_ON});
		md.put(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS, new int[] {CameraMetadata.CONTROL_EFFECT_MODE_OFF});
		md.put(CameraCharacteristics.CONTROL_AVAILABLE_MODES, new int[] {CameraMetadata.CONTROL_MODE_OFF, CameraMetadata.CONTROL_MODE_AUTO});
		md.put(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES, new int[] {CameraMetadata.EDGE_MODE_OFF});
		md.put(CameraCharacteristics.FLASH_INFO_AVAILABLE, false);
		md.put(CameraCharacteristics.HOT_PIXEL_AVAILABLE_HOT_PIXEL_MODES, new int[] {CameraMetadata.HOT_PIXEL_MODE_OFF});
		md.put(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES, new Size[] {new Size(0, 0), new Size(176, 144), new Size(240, 144), new Size(256, 144), new Size(240, 160), new Size(256, 154), new Size(240, 180)});
		md.put(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION, CameraMetadata.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_UNCALIBRATED);
		md.put(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION, new int[] {CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF});
		md.put(CameraCharacteristics.LENS_FACING, getLensFacing());
		md.put(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES, new int[] {CameraMetadata.NOISE_REDUCTION_MODE_OFF});
		md.put(CameraCharacteristics.REQUEST_PARTIAL_RESULT_COUNT, 1);
		md.put(CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH, (byte)4);
		md.put(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_RAW, 0);
		md.put(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC, MAX_PROCESSED_STREAM);
		md.put(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC_STALLING, MAX_STALL_STREAM);
		md.put(CameraCharacteristics.REQUEST_MAX_NUM_INPUT_STREAMS, 0);
		md.put(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM, 1f);
		md.put(CameraCharacteristics.SCALER_CROPPING_TYPE, CameraMetadata.SCALER_CROPPING_TYPE_CENTER_ONLY);
		md.put(CameraCharacteristics.SENSOR_AVAILABLE_TEST_PATTERN_MODES, new int[] {CameraMetadata.SENSOR_TEST_PATTERN_MODE_OFF, CameraMetadata.SENSOR_TEST_PATTERN_MODE_SOLID_COLOR});
		md.put(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE, CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN);
		md.put(CameraCharacteristics.SENSOR_ORIENTATION, ORIENTATION);
		md.put(CameraCharacteristics.SHADING_AVAILABLE_MODES, new int[] {CameraMetadata.SHADING_MODE_OFF});
		md.put(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES, new int[] {CameraMetadata.STATISTICS_FACE_DETECT_MODE_OFF});
		md.put(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT, 0);
		md.put(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES, new boolean[] {false});
		md.put(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES, new int[] {CameraMetadata.STATISTICS_LENS_SHADING_MAP_MODE_OFF});
		md.put(CameraCharacteristics.SYNC_MAX_LATENCY, CameraMetadata.SYNC_MAX_LATENCY_UNKNOWN);
		md.put(CameraCharacteristics.SENSOR_READOUT_TIMESTAMP, CameraMetadata.SENSOR_READOUT_TIMESTAMP_NOT_SUPPORTED);
		md.put(CameraCharacteristics.REQUEST_AVAILABLE_REQUEST_KEYS, names(
				CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
				CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, CaptureRequest.CONTROL_AE_LOCK, CaptureRequest.CONTROL_AE_MODE,
				CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, CaptureRequest.CONTROL_AF_MODE,
				CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AWB_LOCK, CaptureRequest.CONTROL_AWB_MODE,
				CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_MODE,
				CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.FLASH_MODE,
				CaptureRequest.JPEG_ORIENTATION, CaptureRequest.JPEG_QUALITY, CaptureRequest.JPEG_THUMBNAIL_QUALITY,
				CaptureRequest.JPEG_THUMBNAIL_SIZE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.NOISE_REDUCTION_MODE,
				CaptureRequest.SCALER_CROP_REGION, CaptureRequest.SENSOR_TEST_PATTERN_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE,
				CaptureRequest.STATISTICS_HOT_PIXEL_MAP_MODE));
		md.put(CameraCharacteristics.REQUEST_AVAILABLE_RESULT_KEYS, names(
				CaptureResult.COLOR_CORRECTION_ABERRATION_MODE, CaptureResult.CONTROL_AE_ANTIBANDING_MODE,
				CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION, CaptureResult.CONTROL_AE_LOCK, CaptureResult.CONTROL_AE_MODE,
				CaptureResult.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureResult.CONTROL_AE_STATE, CaptureResult.CONTROL_AE_TARGET_FPS_RANGE,
				CaptureResult.CONTROL_AF_MODE, CaptureResult.CONTROL_AF_STATE, CaptureResult.CONTROL_AF_TRIGGER, CaptureResult.CONTROL_AWB_LOCK,
				CaptureResult.CONTROL_AWB_MODE, CaptureResult.CONTROL_AWB_STATE, CaptureResult.CONTROL_CAPTURE_INTENT,
				CaptureResult.CONTROL_EFFECT_MODE, CaptureResult.CONTROL_MODE, CaptureResult.CONTROL_SCENE_MODE,
				CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE, CaptureResult.FLASH_MODE, CaptureResult.FLASH_STATE,
				CaptureResult.JPEG_ORIENTATION, CaptureResult.JPEG_QUALITY, CaptureResult.JPEG_THUMBNAIL_QUALITY,
				CaptureResult.JPEG_THUMBNAIL_SIZE, CaptureResult.LENS_OPTICAL_STABILIZATION_MODE, CaptureResult.NOISE_REDUCTION_MODE,
				CaptureResult.REQUEST_PIPELINE_DEPTH, CaptureResult.SCALER_CROP_REGION, CaptureResult.SENSOR_TIMESTAMP,
				CaptureResult.STATISTICS_FACE_DETECT_MODE, CaptureResult.STATISTICS_HOT_PIXEL_MAP_MODE,
				CaptureResult.STATISTICS_LENS_SHADING_MAP_MODE, CaptureResult.STATISTICS_SCENE_FLICKER));

		/* initCameraControlsCharsKeys() */
		md.put(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE, new Range<Integer>(0, 0));
		md.put(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP, new Rational(0, 1));
		md.put(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, new int[] {CameraMetadata.CONTROL_AF_MODE_AUTO, CameraMetadata.CONTROL_AF_MODE_OFF});
		md.put(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES, new int[] {CameraMetadata.CONTROL_SCENE_MODE_DISABLED});
		md.put(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE, false);
		md.put(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE, false);

		/* initOutputCharsKeys() -> initOutputCharsKeysByFormat(): every size in the three HAL formats */
		List<StreamConfiguration> configurations = new ArrayList<StreamConfiguration>();
		List<StreamConfigurationDuration> minFrameDurations = new ArrayList<StreamConfigurationDuration>();
		List<StreamConfigurationDuration> stallDurations = new ArrayList<StreamConfigurationDuration>();
		for (SupportedFormat format : mSupportedFormats) {
			long minFrameDuration = Long.MAX_VALUE;
			for (int[] rate : format.frameRates)
				minFrameDuration = Math.min(minFrameDuration, 1000000000L * rate[1] / rate[0]);
			for (int halFormat : HAL_FORMATS) {
				configurations.add(new StreamConfiguration(halFormat, format.width, format.height, false));
				minFrameDurations.add(new StreamConfigurationDuration(halFormat, format.width, format.height, minFrameDuration));
				stallDurations.add(new StreamConfigurationDuration(halFormat, format.width, format.height, halFormat == HAL_PIXEL_FORMAT_BLOB ? 1000000000L : 0));
			}
		}
		/* CameraMetadataNative.getStreamConfigurationMap(): high resolution sizes are listed separately only with
		 * BURST_CAPTURE, which the HAL does not report */
		md.put(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, new StreamConfigurationMap(
				configurations.toArray(new StreamConfiguration[0]), minFrameDurations.toArray(new StreamConfigurationDuration[0]),
				stallDurations.toArray(new StreamConfigurationDuration[0]), null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, false));
		/* calculateMinFps(): {fps / 2, fps} for each whole frame rate */
		TreeSet<Integer> framerates = new TreeSet<Integer>();
		int minFps = Integer.MAX_VALUE;
		for (SupportedFormat format : mSupportedFormats) {
			for (int[] rate : format.frameRates) {
				int frameRateInt = (int)fps(rate);
				minFps = Math.min(minFps, frameRateInt);
				framerates.add(frameRateInt);
			}
		}
		@SuppressWarnings("unchecked")
		Range<Integer>[] fpsRanges = new Range[framerates.size()];
		int i = 0;
		for (int framerate : framerates)
			fpsRanges[i++] = new Range<Integer>(framerate / 2, framerate);
		md.put(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES, fpsRanges);
		minFps /= 2;
		md.put(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION, minFps > 0 ? 1000000000L / minFps : Long.MAX_VALUE);
		/* the largest size is the sensor */
		SupportedFormat maximum = null;
		for (SupportedFormat format : mSupportedFormats)
			if (maximum == null || (format.width >= maximum.width && format.height >= maximum.height))
				maximum = format;
		md.put(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE, new Rect(0, 0, maximum.width, maximum.height));
		md.put(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, new Rect(0, 0, maximum.width, maximum.height));
		md.put(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE, new Size(maximum.width, maximum.height));

		/* initAvailableCapabilities(): a color camera */
		md.put(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, new int[] {CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE});
		return md;
	}

	/* ExternalCameraDeviceSession::initDefaultRequests() */
	private void initDefaultRequests() {
		Map<CaptureRequest.Key<?>, Object> md = new HashMap<CaptureRequest.Key<?>, Object>();
		md.put(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_OFF);
		md.put(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
		md.put(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
		md.put(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
		md.put(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
		md.put(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
		md.put(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
		md.put(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
		md.put(CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.CONTROL_SCENE_MODE_DISABLED);
		md.put(CaptureRequest.CONTROL_EFFECT_MODE, CameraMetadata.CONTROL_EFFECT_MODE_OFF);
		md.put(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
		md.put(CaptureRequest.JPEG_THUMBNAIL_SIZE, new Size(240, 180));
		md.put(CaptureRequest.JPEG_QUALITY, (byte)90);
		md.put(CaptureRequest.JPEG_THUMBNAIL_QUALITY, (byte)90);
		md.put(CaptureRequest.JPEG_ORIENTATION, 0);
		md.put(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF);
		md.put(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_OFF);
		md.put(CaptureRequest.SENSOR_TEST_PATTERN_MODE, CameraMetadata.SENSOR_TEST_PATTERN_MODE_OFF);
		md.put(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CameraMetadata.STATISTICS_FACE_DETECT_MODE_OFF);
		md.put(CaptureRequest.STATISTICS_HOT_PIXEL_MAP_MODE, false);
		/* 30 fps if a format has it, else the highest rate */
		boolean support30Fps = false;
		int maxFps = Integer.MIN_VALUE;
		for (SupportedFormat format : mSupportedFormats) {
			for (int[] rate : format.frameRates) {
				int framerateInt = (int)fps(rate);
				maxFps = Math.max(maxFps, framerateInt);
				support30Fps |= framerateInt == 30;
			}
		}
		int defaultFramerate = support30Fps ? 30 : maxFps;
		md.put(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(defaultFramerate / 2, defaultFramerate));
		md.put(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO);
		md.put(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
		int[][] templates = {
			{android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW, CameraMetadata.CONTROL_CAPTURE_INTENT_PREVIEW},
			{android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE, CameraMetadata.CONTROL_CAPTURE_INTENT_STILL_CAPTURE},
			{android.hardware.camera2.CameraDevice.TEMPLATE_RECORD, CameraMetadata.CONTROL_CAPTURE_INTENT_VIDEO_RECORD},
			{android.hardware.camera2.CameraDevice.TEMPLATE_VIDEO_SNAPSHOT, CameraMetadata.CONTROL_CAPTURE_INTENT_VIDEO_SNAPSHOT},
		};
		for (int[] template : templates) {
			Map<CaptureRequest.Key<?>, Object> copy = new HashMap<CaptureRequest.Key<?>, Object>(md);
			copy.put(CaptureRequest.CONTROL_CAPTURE_INTENT, template[1]);
			mDefaultRequests.put(template[0], copy);
		}
	}

	/** ExternalCameraDeviceSession::constructDefaultRequestSettings(): null for MANUAL and ZERO_SHUTTER_LAG (the
	 * HAL's ILLEGAL_ARGUMENT) */
	public Map<CaptureRequest.Key<?>, Object> getDefaultRequest(int templateType) {
		return mDefaultRequests.get(templateType);
	}

	/* the public result keys by name (class initialization, no lock shared with the stream thread) */
	private static final class ResultKeys {
		static final Map<String, CaptureResult.Key<?>> BY_NAME = new HashMap<String, CaptureResult.Key<?>>();
		static {
			for (java.lang.reflect.Field field : CaptureResult.class.getDeclaredFields()) {
				if (field.getType() != CaptureResult.Key.class || !java.lang.reflect.Modifier.isStatic(field.getModifiers()) || !java.lang.reflect.Modifier.isPublic(field.getModifiers()))
					continue;
				try {
					CaptureResult.Key<?> key = (CaptureResult.Key<?>)field.get(null);
					BY_NAME.put(key.getName(), key);
				} catch (IllegalAccessException e) {
				}
			}
		}
	}

	/* the public result key of a request key's name */
	private static CaptureResult.Key<?> resultKey(String name) {
		return ResultKeys.BY_NAME.get(name);
	}

	/** ExternalCameraDeviceSession::fillCaptureResult(): the request's settings plus the faked 3A state of a USB
	 * camera (AF locked after a trigger until a cancel; AE/AWB converged) */
	private final Object mAfLock = new Object();
	private boolean mAfTrigger;

	public Map<CaptureResult.Key<?>, Object> fillCaptureResult(CaptureRequest request, long timestampNs) {
		Map<CaptureResult.Key<?>, Object> md = new HashMap<CaptureResult.Key<?>, Object>();
		for (CaptureRequest.Key<?> key : request.getKeys()) {
			CaptureResult.Key<?> resultKey = resultKey(key.getName());
			if (resultKey != null)
				md.put(resultKey, request.get(key));
		}
		Integer afTrigger = request.get(CaptureRequest.CONTROL_AF_TRIGGER);
		synchronized (mAfLock) {
			if (afTrigger != null && afTrigger == CameraMetadata.CONTROL_AF_TRIGGER_START)
				mAfTrigger = true;
			else if (afTrigger != null && afTrigger == CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
				mAfTrigger = false;
			md.put(CaptureResult.CONTROL_AF_STATE, mAfTrigger ? CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED : CameraMetadata.CONTROL_AF_STATE_INACTIVE);
		}
		/* fillCaptureResultCommon() */
		md.put(CaptureResult.CONTROL_AE_STATE, CameraMetadata.CONTROL_AE_STATE_CONVERGED);
		md.put(CaptureResult.CONTROL_AE_LOCK, false);
		md.put(CaptureResult.CONTROL_AWB_STATE, CameraMetadata.CONTROL_AWB_STATE_CONVERGED);
		md.put(CaptureResult.CONTROL_AWB_LOCK, false);
		md.put(CaptureResult.FLASH_STATE, CameraMetadata.FLASH_STATE_UNAVAILABLE);
		md.put(CaptureResult.REQUEST_PIPELINE_DEPTH, (byte)4);
		md.put(CaptureResult.SCALER_CROP_REGION, new Rect(mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)));
		md.put(CaptureResult.SENSOR_TIMESTAMP, timestampNs);
		md.put(CaptureResult.STATISTICS_LENS_SHADING_MAP_MODE, CameraMetadata.STATISTICS_LENS_SHADING_MAP_MODE_OFF);
		md.put(CaptureResult.STATISTICS_SCENE_FLICKER, CameraMetadata.STATISTICS_SCENE_FLICKER_NONE);
		return md;
	}

	/* --- the stream --- */

	/* the native calls happen outside this object's monitor: the stream thread holds the native loop's lock while
	 * it calls the client */

	/** opens the camera for this client, evicting the one that has it open; false if the camera is gone */
	public boolean open(FrameListener client) {
		FrameListener evicted;
		long oldHandle;
		synchronized (this) {
			evicted = mListener;
			oldHandle = mHandle;
			mHandle = 0;
			mListener = null;
		}
		if (oldHandle != 0)
			nativeClose(oldHandle);
		if (evicted != null)
			evicted.onEvicted();
		long handle = nativeOpen(mIndex);
		if (handle == 0)
			return false;
		synchronized (this) {
			mHandle = handle;
			mListener = client;
		}
		return true;
	}

	public synchronized boolean isOpenBy(FrameListener client) {
		return mHandle != 0 && mListener == client;
	}

	private synchronized long handleOf(FrameListener client) {
		return mListener == client ? mHandle : 0;
	}

	/** ExternalCameraDeviceSession::configureV4l2StreamLocked(): the size and the highest frame rate of that size
	 * within the requested maximum (else the lowest); false if the camera cannot stream it */
	public boolean startStream(FrameListener client, int width, int height, int maxFps) {
		int[] best = null;   /* the highest rate within maxFps */
		int[] lowest = null; /* else the lowest rate */
		for (SupportedFormat format : mSupportedFormats) {
			if (format.width != width || format.height != height)
				continue;
			if (format.source != null) { /* streamed at the camera's size, the targets are scaled */
				width = format.source.width;
				height = format.source.height;
			}
			for (int[] rate : format.frameRates) {
				if (fps(rate) <= maxFps + 0.01 && (best == null || fps(rate) > fps(best)))
					best = rate;
				if (lowest == null || fps(rate) < fps(lowest))
					lowest = rate;
			}
		}
		if (best == null)
			best = lowest;
		long handle = handleOf(client);
		return best != null && handle != 0 && nativeStart(handle, width, height, best[0], best[1]);
	}

	/** the Surfaces that show the frames (SurfaceView, Surface(SurfaceTexture), a Surface of another process) */
	public void setTargets(FrameListener client, Collection<Surface> surfaces) {
		long handle = handleOf(client);
		if (handle != 0)
			nativeSetTargets(handle, surfaces.toArray(new Surface[0]));
	}

	public void stopStream(FrameListener client) {
		long handle = handleOf(client);
		if (handle != 0)
			nativeStop(handle);
	}

	public void close(FrameListener client) {
		long handle;
		synchronized (this) {
			if (mListener != client)
				return;
			handle = mHandle;
			mHandle = 0;
			mListener = null;
		}
		if (handle != 0)
			nativeClose(handle);
	}

	/** @hide during onFrame(): the frame as a YUV_420_888 image (I420 planes) of the reader's size, queued to the
	 * reader (an ImageReader target, an encoder's input surface); false if the reader takes no image now */
	public boolean queueYuvImage(android.media.ImageReader reader, long timestampNs) {
		int w = reader.getWidth(), h = reader.getHeight();
		int ySize = w * h, cSize = (w / 2) * (h / 2);
		byte[] i420 = new byte[ySize + 2 * cSize];
		if (!copyFrame(i420, android.graphics.ImageFormat.YUV_420_888, w, h))
			return false;
		java.nio.ByteBuffer[] planes = {
			java.nio.ByteBuffer.allocateDirect(ySize).put(i420, 0, ySize),
			java.nio.ByteBuffer.allocateDirect(cSize).put(i420, ySize, cSize),
			java.nio.ByteBuffer.allocateDirect(cSize).put(i420, ySize + cSize, cSize)};
		for (java.nio.ByteBuffer plane : planes)
			plane.rewind();
		return reader.queueImage(timestampNs, planes, new int[] {w, w / 2, w / 2}, new int[] {1, 1, 1});
	}

	/** during FrameListener.onFrame(): the frame scaled to width x height as ImageFormat.NV21 or YV12 (the layouts
	 * of Camera's preview callbacks) or YUV_420_888 (I420, packed) in dst; false if it does not fit */
	public boolean copyFrame(byte[] dst, int imageFormat, int width, int height) {
		long handle = mHandle;
		return handle != 0 && nativeCopyFrame(handle, dst, imageFormat, width, height);
	}

	/** during FrameListener.onFrame(): the frame scaled to width x height as a JPEG (the HAL's BLOB output; AOSP's
	 * external HAL encodes it from the frame as well); null if it cannot be encoded */
	public byte[] encodeJpeg(int width, int height, int quality) {
		long handle = mHandle;
		return handle != 0 ? nativeEncodeJpeg(handle, width, height, quality) : null;
	}

	/** the display transform of the stream's Surfaces: horizontal flip first, then turned clockwise */
	public void setDisplayTransform(FrameListener client, int rotation, boolean flipH) {
		long handle = handleOf(client);
		if (handle != 0)
			nativeSetDisplayTransform(handle, rotation, flipH);
	}

	/* called by the native stream thread */
	private void onFrame(long timestampNs) {
		FrameListener listener = mListener;
		if (listener != null)
			listener.onFrame(timestampNs);
	}

	private void onStreamError(String message) {
		FrameListener listener = mListener;
		if (listener != null)
			listener.onStreamError(message);
	}

	private static native boolean nativeIsCameraPresent();
	private static native int nativeInit(boolean requestAccess);
	private static native String nativeGetName(int index);
	private static native int[] nativeGetModes(int index);
	private native long nativeOpen(int index);
	private native boolean nativeStart(long handle, int width, int height, int fpsNum, int fpsDen);
	private static native void nativeSetTargets(long handle, Surface[] targets);
	private static native void nativeStop(long handle);
	private static native void nativeClose(long handle);
	private static native void nativeSetDisplayTransform(long handle, int rotation, boolean flipH);
	private static native boolean nativeCopyFrame(long handle, byte[] dst, int format, int width, int height);
	private static native byte[] nativeEncodeJpeg(long handle, int width, int height, int quality);
}
