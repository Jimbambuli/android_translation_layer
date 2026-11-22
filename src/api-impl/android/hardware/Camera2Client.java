package android.hardware;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.impl.ExternalCameraDevice;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* AOSP's API 1 camera client, frameworks/av services/camera/libcameraservice/api1/Camera2Client.cpp with
 * client2/Parameters.cpp (android16-release), over ATL's camera HAL as AOSP's runs over Camera3Device: the
 * parameters are built from the static metadata as Parameters::initialize() builds them and checked as
 * Parameters::set() checks preview size, frame rate, preview and picture size; the preview states and the
 * preview window as in Camera2Client; preview callbacks as its CallbackProcessor delivers them (NV21 or YV12
 * of the preview size).
 *
 * One difference to AOSP: Parameters::initialize() bounds the video sizes by the encoders of
 * media_profiles.xml, and ATL has no video encoder (AOSP's default profiles would allow 176x144 to 352x288
 * only and leave most cameras without an initial preview size); the video sizes are the preview sizes. */
final class Camera2Client implements ExternalCameraDevice.FrameListener {
	/* client2/Parameters.h */
	private static final int MAX_PREVIEW_WIDTH = 1920;
	private static final int MAX_PREVIEW_HEIGHT = 1920;
	private static final int MAX_INITIAL_PREVIEW_WIDTH = 1920;
	private static final int MAX_INITIAL_PREVIEW_HEIGHT = 1080;
	private static final float ASPECT_RATIO_TOLERANCE = 0.01f;
	private static final long SLOW_JPEG_MODE_THRESHOLD = 33400000L;
	private static final int FPS_MARGIN = 1;
	private static final int MAX_DEFAULT_FPS = 30;
	private static final long MAX_PREVIEW_RECORD_DURATION_NS = (long)(1e9 / 29.97);
	private static final int FPS_TO_API_SCALE = 1000;
	/* system/core graphics-base */
	private static final int HAL_PIXEL_FORMAT_YCRCB_420_SP = 0x11;
	private static final int HAL_PIXEL_FORMAT_YV12 = 0x32315659;
	private static final int HAL_PIXEL_FORMAT_YCBCR_420_888 = 0x23;
	private static final int HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED = 0x22;
	/* system/core camera.h */
	static final int CAMERA_FRAME_CALLBACK_FLAG_ENABLE_MASK = 0x01;
	static final int CAMERA_FRAME_CALLBACK_FLAG_ONE_SHOT_MASK = 0x02;
	static final int CAMERA_FRAME_CALLBACK_FLAG_COPY_OUT_MASK = 0x04;

	/* Parameters::State */
	private static final int DISCONNECTED = 0;
	private static final int STOPPED = 1;
	private static final int WAITING_FOR_PREVIEW_WINDOW = 2;
	private static final int PREVIEW = 3;
	private static final int STILL_CAPTURE = 5;

	/** Camera's side (AOSP's JNICameraContext): a preview frame to copy and post, on the stream's thread */
	interface Callbacks {
		void onPreviewFrame(Camera2Client client, int size);

		void onError(int error);

		/** CaptureSequencer: the shutter (CAMERA_MSG_SHUTTER and CAMERA_MSG_RAW_IMAGE_NOTIFY), then the JPEG
		 * (CAMERA_MSG_COMPRESSED_IMAGE; null if it could not be encoded) */
		void onShutter();

		void onPictureTaken(byte[] jpeg);
	}

	private final ExternalCameraDevice mHal;
	private final CameraCharacteristics mInfo;
	private final Callbacks mCallbacks;
	private final Object mLock = new Object();

	private final List<Size> mAvailablePreviewSizes = new ArrayList<Size>();
	private final List<Size> mAvailableVideoSizes = new ArrayList<Size>();
	private String mParamsFlattened;
	private int mState = DISCONNECTED;
	private int mPreviewWidth;
	private int mPreviewHeight;
	private int[] mPreviewFpsRange = new int[2];
	private int mPreviewFormat;
	private int mPictureWidth;
	private int mPictureHeight;
	private int mPreviewCallbackFlags;
	private Surface mPreviewSurface;
	private boolean mPlayShutterSound = true;
	private int mPreviewTransform;
	private boolean mFocusFixed;
	/* API 1 facing FRONT (an external camera is reported so): the preview transform mirrors */
	private final boolean mMirror = ExternalCameraDevice.getLensFacing() != CameraMetadata.LENS_FACING_BACK;
	/* takePicture(): a still capture waits for the next frame of the picture size */
	private boolean mCapturePending;
	private boolean mCaptureRestartsStream;
	private int mJpegQuality = 90;
	private final java.util.concurrent.Executor mWorker = java.util.concurrent.Executors.newSingleThreadExecutor();
	/* the HAL is called outside mLock (its stream thread takes mLock in onFrame() while it holds the native
	 * loop's lock), serialized by mHalLock */
	private final Object mHalLock = new Object();

	Camera2Client(ExternalCameraDevice hal, Callbacks callbacks) {
		mHal = hal;
		mInfo = hal.getCharacteristics();
		mCallbacks = callbacks;
	}

	/* Camera2Client::initialize(): parameters, then the device */
	boolean initialize() {
		Camera.Parameters params = Camera.getEmptyParameters();
		if (!initializeParameters(params))
			return false;
		if (!mHal.open(this))
			return false;
		synchronized (mLock) {
			mParamsFlattened = params.flatten();
			mState = STOPPED;
		}
		/* previewTransform = degToTransform(0, cameraFacing == CAMERA_FACING_FRONT): the external camera is front facing
		 * to API 1, so its preview is mirrored */
		mHal.setDisplayTransform(this, 0, mMirror);
		return true;
	}

	/* --- Parameters::initialize() --- */

	private StreamConfigurationMap streamMap() {
		return mInfo.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
	}

	/* Parameters::getFilteredSizes(): IMPLEMENTATION_DEFINED outputs within the area bounds, fast enough to preview */
	private List<Size> getFilteredSizes(int lowerArea, int upperArea) {
		List<Size> sizes = new ArrayList<Size>();
		StreamConfigurationMap map = streamMap();
		for (Size size : map.getOutputSizes(android.graphics.SurfaceTexture.class)) {
			int area = size.getWidth() * size.getHeight();
			if (area < lowerArea || area > upperArea)
				continue;
			if (map.getOutputMinFrameDuration(android.graphics.SurfaceTexture.class, size) > MAX_PREVIEW_RECORD_DURATION_NS)
				continue;
			sizes.add(size);
		}
		return sizes;
	}

	/* Parameters::isFpsSupported() */
	private boolean isFpsSupported(List<Size> sizes, int fps) {
		for (Size size : sizes) {
			long minFrameDuration = streamMap().getOutputMinFrameDuration(android.graphics.SurfaceTexture.class, size);
			if (minFrameDuration <= 0)
				return false;
			int maxSupportedFps = (int)(1e9 / minFrameDuration) + FPS_MARGIN;
			if (fps > maxSupportedFps)
				return false;
		}
		return true;
	}

	private Size[] jpegSizes() {
		return streamMap().getOutputSizes(ImageFormat.JPEG);
	}

	private static String join(List<String> values) {
		StringBuilder s = new StringBuilder();
		for (String value : values) {
			if (s.length() > 0)
				s.append(',');
			s.append(value);
		}
		return s.toString();
	}

	private static String sizes(List<Size> sizes) {
		List<String> values = new ArrayList<String>();
		for (Size size : sizes)
			values.add(size.getWidth() + "x" + size.getHeight());
		return join(values);
	}

	/* CameraParameters::setFloat(): printf %g */
	private static String formatFloat(float value) {
		return new BigDecimal(Float.toString(value)).stripTrailingZeros().toPlainString();
	}

	private boolean initializeParameters(Camera.Parameters params) {
		/* buildFastInfo(): the preview fps ranges up to 30 fps, the widest (then lowest) one for stills */
		Range<Integer>[] availableFpsRanges = mInfo.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
		if (availableFpsRanges == null || availableFpsRanges.length == 0)
			return false;
		List<Size> previewBoundSizes = getFilteredSizes(0, MAX_PREVIEW_WIDTH * MAX_PREVIEW_HEIGHT);
		if (previewBoundSizes.isEmpty())
			return false;
		List<Range<Integer>> supportedPreviewFpsRanges = new ArrayList<Range<Integer>>();
		for (Range<Integer> range : availableFpsRanges) {
			if (!isFpsSupported(previewBoundSizes, range.getUpper()) || range.getUpper() > MAX_DEFAULT_FPS)
				continue;
			supportedPreviewFpsRanges.add(range);
		}
		if (supportedPreviewFpsRanges.isEmpty())
			return false;
		int[] bestStillCaptureFpsRange = {supportedPreviewFpsRanges.get(0).getLower(), supportedPreviewFpsRanges.get(0).getUpper()};
		int curRange = bestStillCaptureFpsRange[1] - bestStillCaptureFpsRange[0];
		for (int i = 1; i < supportedPreviewFpsRanges.size(); i++) {
			Range<Integer> r = supportedPreviewFpsRanges.get(i);
			int nextRange = r.getUpper() - r.getLower();
			if (nextRange > curRange || (nextRange == curRange && bestStillCaptureFpsRange[0] > r.getLower())) {
				bestStillCaptureFpsRange[0] = r.getLower();
				bestStillCaptureFpsRange[1] = r.getUpper();
				curRange = nextRange;
			}
		}

		/* preview and video sizes (video: see the class comment) */
		mAvailablePreviewSizes.addAll(previewBoundSizes);
		mAvailableVideoSizes.addAll(previewBoundSizes);
		int previewWidth = 0, previewHeight = 0;
		for (Size size : mAvailablePreviewSizes) {
			int newWidth = size.getWidth(), newHeight = size.getHeight();
			if (newWidth >= previewWidth && newHeight >= previewHeight && newWidth <= MAX_INITIAL_PREVIEW_WIDTH && newHeight <= MAX_INITIAL_PREVIEW_HEIGHT) {
				if (mAvailableVideoSizes.contains(size)) {
					previewWidth = newWidth;
					previewHeight = newHeight;
				}
			}
		}
		if (previewWidth == 0)
			return false;
		mPreviewWidth = previewWidth;
		mPreviewHeight = previewHeight;
		params.setPreviewSize(previewWidth, previewHeight);
		params.set("video-size", previewWidth + "x" + previewHeight);
		params.set("preferred-preview-size-for-video", previewWidth + "x" + previewHeight);
		params.set("preview-size-values", sizes(mAvailablePreviewSizes));
		params.set("video-size-values", sizes(mAvailableVideoSizes));

		mPreviewFormat = HAL_PIXEL_FORMAT_YCRCB_420_SP;
		params.set("preview-format", "yuv420sp");
		/* getAvailableOutputFormats(): the HAL's output formats, sorted (the map reports BLOB as JPEG) */
		List<Integer> outputFormats = new ArrayList<Integer>();
		for (int format : streamMap().getOutputFormats())
			outputFormats.add(format == ImageFormat.JPEG ? 0x21 : format);
		Collections.sort(outputFormats);
		List<String> previewFormats = new ArrayList<String>();
		for (int format : outputFormats) {
			switch (format) {
				case 0x10: /* HAL_PIXEL_FORMAT_YCbCr_422_SP */
					previewFormats.add("yuv422sp");
					break;
				case HAL_PIXEL_FORMAT_YCRCB_420_SP:
					previewFormats.add("yuv420sp");
					break;
				case 0x14: /* HAL_PIXEL_FORMAT_YCbCr_422_I */
					previewFormats.add("yuv422i-yuyv");
					break;
				case HAL_PIXEL_FORMAT_YV12:
					previewFormats.add("yuv420p");
					break;
				case 4: /* HAL_PIXEL_FORMAT_RGB_565 */
					previewFormats.add("rgb565");
					break;
				case 1: /* HAL_PIXEL_FORMAT_RGBA_8888 */
					previewFormats.add("rgba8888");
					break;
				case HAL_PIXEL_FORMAT_YCBCR_420_888:
					previewFormats.add("yuv420p");
					previewFormats.add("yuv420sp");
					break;
				default: /* IMPLEMENTATION_DEFINED, RAW16, BLOB and unknown formats are no preview formats */
					break;
			}
		}
		params.set("preview-format-values", join(previewFormats));

		mPreviewFpsRange[0] = bestStillCaptureFpsRange[0];
		mPreviewFpsRange[1] = bestStillCaptureFpsRange[1];
		params.set("preview-frame-rate", mPreviewFpsRange[1]); /* fpsFromRange(): the maximum */
		params.set("preview-fps-range", (mPreviewFpsRange[0] * FPS_TO_API_SCALE) + "," + (mPreviewFpsRange[1] * FPS_TO_API_SCALE));
		List<String> fpsRanges = new ArrayList<String>();
		List<Integer> frameRates = new ArrayList<Integer>();
		for (Range<Integer> range : availableFpsRanges) {
			if (!isFpsSupported(mAvailablePreviewSizes, range.getUpper()))
				continue;
			fpsRanges.add("(" + (range.getLower() * FPS_TO_API_SCALE) + "," + (range.getUpper() * FPS_TO_API_SCALE) + ")");
			if (!frameRates.contains(range.getUpper()))
				frameRates.add(range.getUpper());
		}
		params.set("preview-fps-range-values", join(fpsRanges));
		Collections.sort(frameRates); /* a SortedVector, listed as it grows */
		List<String> rates = new ArrayList<String>();
		for (int rate : frameRates)
			rates.add(Integer.toString(rate));
		params.set("preview-frame-rate-values", join(rates));

		Size[] jpegSizes = jpegSizes();
		if (jpegSizes == null || jpegSizes.length == 0)
			return false;
		mPictureWidth = jpegSizes[0].getWidth();
		mPictureHeight = jpegSizes[0].getHeight();
		params.setPictureSize(mPictureWidth, mPictureHeight);
		List<Size> jpegSizeList = new ArrayList<Size>();
		Collections.addAll(jpegSizeList, jpegSizes);
		params.set("picture-size-values", sizes(jpegSizeList));
		params.set("picture-format", "jpeg");
		params.set("picture-format-values", "jpeg");

		/* the largest thumbnail of the picture's aspect ratio, else the first one */
		Size[] thumbnailSizes = mInfo.get(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES);
		if (thumbnailSizes == null || thumbnailSizes.length == 0)
			return false;
		float picAspectRatio = (float)mPictureWidth / mPictureHeight;
		Size thumbnail = null;
		for (Size size : thumbnailSizes) {
			if (size.getWidth() > 0 && size.getHeight() > 0) {
				float ratio = (float)size.getWidth() / size.getHeight();
				if (Math.abs(ratio - picAspectRatio) < ASPECT_RATIO_TOLERANCE && (thumbnail == null || thumbnail.getWidth() < size.getWidth()))
					thumbnail = size;
			}
		}
		if (thumbnail == null)
			thumbnail = thumbnailSizes[0];
		params.set("jpeg-thumbnail-width", thumbnail.getWidth());
		params.set("jpeg-thumbnail-height", thumbnail.getHeight());
		List<Size> thumbnailList = new ArrayList<Size>();
		Collections.addAll(thumbnailList, thumbnailSizes);
		params.set("jpeg-thumbnail-size-values", sizes(thumbnailList));
		params.set("jpeg-thumbnail-quality", 90);
		params.set("jpeg-quality", 90);
		params.set("rotation", 0);

		/* white balance, effects, antibanding (the external HAL lists one mode each) */
		params.set("whitebalance", "auto");
		params.set("whitebalance-values", modeNames(mInfo.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES), new int[] {CameraMetadata.CONTROL_AWB_MODE_AUTO, CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT, CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT, CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT, CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT, CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT, CameraMetadata.CONTROL_AWB_MODE_TWILIGHT, CameraMetadata.CONTROL_AWB_MODE_SHADE}, new String[] {"auto", "incandescent", "fluorescent", "warm-fluorescent", "daylight", "cloudy-daylight", "twilight", "shade"}, "auto"));
		params.set("effect", "none");
		params.set("effect-values", modeNames(mInfo.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS), new int[] {CameraMetadata.CONTROL_EFFECT_MODE_OFF, CameraMetadata.CONTROL_EFFECT_MODE_MONO, CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE, CameraMetadata.CONTROL_EFFECT_MODE_SOLARIZE, CameraMetadata.CONTROL_EFFECT_MODE_SEPIA, CameraMetadata.CONTROL_EFFECT_MODE_POSTERIZE, CameraMetadata.CONTROL_EFFECT_MODE_WHITEBOARD, CameraMetadata.CONTROL_EFFECT_MODE_BLACKBOARD, CameraMetadata.CONTROL_EFFECT_MODE_AQUA}, new String[] {"none", "mono", "negative", "solarize", "sepia", "posterize", "whiteboard", "blackboard", "aqua"}, "none"));
		params.set("antibanding", "auto");
		params.set("antibanding-values", modeNames(mInfo.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES), new int[] {CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_OFF, CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_50HZ, CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_60HZ, CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO}, new String[] {"off", "50hz", "60hz", "auto"}, "off"));

		/* scene modes: "auto" plus the listed ones; none if DISABLED is listed (the external HAL's case) */
		params.set("scene-mode", "auto");
		int[] sceneModes = mInfo.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
		if (sceneModes == null || sceneModes.length == 0) {
			params.remove("scene-mode");
		} else {
			boolean noSceneModes = false;
			List<String> supportedSceneModes = new ArrayList<String>();
			supportedSceneModes.add("auto");
			int[] modes = {CameraMetadata.CONTROL_SCENE_MODE_ACTION, CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT, CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE, CameraMetadata.CONTROL_SCENE_MODE_NIGHT, CameraMetadata.CONTROL_SCENE_MODE_NIGHT_PORTRAIT, CameraMetadata.CONTROL_SCENE_MODE_THEATRE, CameraMetadata.CONTROL_SCENE_MODE_BEACH, CameraMetadata.CONTROL_SCENE_MODE_SNOW, CameraMetadata.CONTROL_SCENE_MODE_SUNSET, CameraMetadata.CONTROL_SCENE_MODE_STEADYPHOTO, CameraMetadata.CONTROL_SCENE_MODE_FIREWORKS, CameraMetadata.CONTROL_SCENE_MODE_SPORTS, CameraMetadata.CONTROL_SCENE_MODE_PARTY, CameraMetadata.CONTROL_SCENE_MODE_CANDLELIGHT, CameraMetadata.CONTROL_SCENE_MODE_BARCODE, CameraMetadata.CONTROL_SCENE_MODE_HDR};
			String[] names = {"action", "portrait", "landscape", "night", "night-portrait", "theatre", "beach", "snow", "sunset", "steadyphoto", "fireworks", "sports", "party", "candlelight", "barcode", "hdr"};
			for (int mode : sceneModes) {
				if (mode == CameraMetadata.CONTROL_SCENE_MODE_DISABLED)
					noSceneModes = true;
				for (int i = 0; i < modes.length; i++)
					if (modes[i] == mode)
						supportedSceneModes.add(names[i]);
			}
			if (!noSceneModes)
				params.set("scene-mode-values", join(supportedSceneModes));
			else
				params.remove("scene-mode");
		}

		/* flash modes with a flash unit, else no flash keys */
		Boolean flash = mInfo.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
		if (flash != null && flash) {
			params.set("flash-mode", "off");
			String supportedFlashModes = "off,auto,on,torch";
			int[] aeModes = mInfo.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
			for (int i = 0; aeModes != null && i < aeModes.length; i++) {
				if (aeModes[i] == CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE) {
					supportedFlashModes += ",red-eye";
					break;
				}
			}
			params.set("flash-mode-values", supportedFlashModes);
		} else {
			params.remove("flash-mode");
			params.remove("flash-mode-values");
		}

		/* focus: fixed without a minimum focus distance (the external HAL's case), else the AF modes */
		Float minFocusDistance = mInfo.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
		mFocusFixed = minFocusDistance == null || minFocusDistance == 0;
		if (mFocusFixed) {
			params.set("focus-mode", "fixed");
			params.set("focus-mode-values", "fixed");
		} else {
			params.set("focus-mode", "auto");
			List<String> supportedFocusModes = new ArrayList<String>();
			Integer calibration = mInfo.get(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION);
			if (calibration != null && calibration != CameraMetadata.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_UNCALIBRATED)
				supportedFocusModes.add("infinity");
			int[] afModes = mInfo.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
			int[] modes = {CameraMetadata.CONTROL_AF_MODE_AUTO, CameraMetadata.CONTROL_AF_MODE_MACRO, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE, CameraMetadata.CONTROL_AF_MODE_EDOF};
			String[] names = {"auto", "macro", "continuous-video", "continuous-picture", "edof"};
			for (int i = 0; afModes != null && i < afModes.length; i++)
				for (int j = 0; j < modes.length; j++)
					if (modes[j] == afModes[i])
						supportedFocusModes.add(names[j]);
			params.set("focus-mode-values", join(supportedFocusModes));
		}
		params.set("max-num-focus-areas", mFocusFixed ? 0 : mInfo.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF));
		params.set("focus-areas", "(0,0,0,0,0)");
		/* buildFastInfo(): no LENS_INFO_AVAILABLE_FOCAL_LENGTHS; calculatePictureFovs(): -1 for an external camera */
		params.set("focal-length", formatFloat(-1f));
		params.set("horizontal-view-angle", formatFloat(-1f));
		params.set("vertical-view-angle", formatFloat(-1f));

		params.set("exposure-compensation", 0);
		Range<Integer> compensationRange = mInfo.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
		android.util.Rational compensationStep = mInfo.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
		if (compensationRange == null || compensationStep == null)
			return false;
		params.set("max-exposure-compensation", compensationRange.getUpper());
		params.set("min-exposure-compensation", compensationRange.getLower());
		params.set("exposure-compensation-step", formatFloat((float)compensationStep.getNumerator() / compensationStep.getDenominator()));
		Boolean aeLock = mInfo.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE);
		if (aeLock != null && aeLock) {
			params.set("auto-exposure-lock", "false");
			params.set("auto-exposure-lock-supported", "true");
		} else {
			params.set("auto-exposure-lock-supported", "false");
		}
		Boolean awbLock = mInfo.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE);
		if (awbLock != null && awbLock) {
			params.set("auto-whitebalance-lock", "false");
			params.set("auto-whitebalance-lock-supported", "true");
		} else {
			params.set("auto-whitebalance-lock-supported", "false");
		}
		params.set("max-num-metering-areas", mInfo.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE));
		params.set("metering-areas", "(0,0,0,0,0)");

		/* zoom: NUM_ZOOM_STEPS (100) ratios from 1 to the maximum digital zoom; none at 1 (the external HAL's case) */
		Float maxDigitalZoom = mInfo.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
		if (maxDigitalZoom == null)
			return false;
		if (Math.abs(maxDigitalZoom - 1f) > 0.00001f) {
			params.set("zoom", 0);
			List<String> zoomRatios = new ArrayList<String>();
			float zoom = 1f;
			float zoomIncrement = (maxDigitalZoom - zoom) / (100 - 1);
			int previousZoom = -1;
			for (int i = 0; i < 100; i++) {
				int currentZoom = (int)(zoom * 100);
				zoom += zoomIncrement;
				if (previousZoom == currentZoom)
					continue;
				zoomRatios.add(Integer.toString(currentZoom));
				previousZoom = currentZoom;
			}
			if (!zoomRatios.isEmpty()) {
				params.set("zoom-ratios", join(zoomRatios));
				params.set("zoom-supported", "true");
				params.set("max-zoom", zoomRatios.size() - 1);
			} else {
				params.set("zoom-supported", "false");
			}
		} else {
			params.set("zoom-supported", "false");
		}
		params.set("smooth-zoom-supported", "false");
		params.set("focus-distances", "Infinity,Infinity,Infinity");
		params.set("max-num-detected-faces-hw", mInfo.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT));
		params.set("max-num-detected-faces-sw", 0);
		params.set("video-frame-format", "android-opaque");
		params.set("recording-hint", "false");
		params.set("video-snapshot-supported", "true");
		params.set("video-stabilization", "false");
		int[] stabilizationModes = mInfo.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
		params.set("video-stabilization-supported", stabilizationModes != null && stabilizationModes.length > 1 ? "true" : "false");

		/* slowJpegMode: the largest JPEG size streams slower than 33.4 ms a frame */
		Size maxJpegSize = jpegSizes[0];
		for (Size size : jpegSizes)
			if (size.getWidth() > maxJpegSize.getWidth() || (size.getWidth() == maxJpegSize.getWidth() && size.getHeight() > maxJpegSize.getHeight()))
				maxJpegSize = size;
		if (streamMap().getOutputMinFrameDuration(ImageFormat.JPEG, maxJpegSize) > SLOW_JPEG_MODE_THRESHOLD)
			params.set("video-snapshot-supported", "false");
		return true;
	}

	/* the names of the listed modes known to Parameters.cpp, in the listed order; the default if none is listed */
	private static String modeNames(int[] available, int[] modes, String[] names, String none) {
		if (available == null || available.length == 0)
			return none;
		List<String> values = new ArrayList<String>();
		for (int mode : available)
			for (int i = 0; i < modes.length; i++)
				if (modes[i] == mode)
					values.add(names[i]);
		return join(values);
	}

	/* --- client calls --- */

	String getParameters() {
		synchronized (mLock) {
			return mParamsFlattened;
		}
	}

	/* Parameters::set(): the checks of the preview size, frame rate, preview format and picture size; false is
	 * BAD_VALUE */
	boolean setParameters(String flattened) {
		Camera.Parameters newParams = Camera.getEmptyParameters();
		newParams.unflatten(flattened);
		synchronized (mLock) {
			Camera.Size previewSize = newParams.getPreviewSize();
			if (previewSize == null)
				{
					if (Camera.DEBUG)
						android.util.Log.i("Camera", "setParameters: check at line 507 fails");
					return false;
				}
			if (previewSize.width != mPreviewWidth || previewSize.height != mPreviewHeight) {
				if (mState >= PREVIEW)
					{
						if (Camera.DEBUG)
							android.util.Log.i("Camera", "setParameters: check at line 510 fails");
						return false;
					}
				if (!mAvailablePreviewSizes.contains(new Size(previewSize.width, previewSize.height)))
					{
						if (Camera.DEBUG)
							android.util.Log.i("Camera", "setParameters: check at line 512 fails");
						return false;
					}
			}
			boolean recordingHint = "true".equals(newParams.get("recording-hint"));
			String fpsSingle = newParams.get("preview-frame-rate");
			String fpsRange = newParams.get("preview-fps-range");
			if (fpsRange == null && fpsSingle == null)
				{
					if (Camera.DEBUG)
						android.util.Log.i("Camera", "setParameters: check at line 518 fails");
					return false;
				}
			/* CameraParameters2::compareSetOrder(): the key set later comes later in the string */
			boolean fpsUseSingleValue = fpsRange == null || (fpsSingle != null && flattened.indexOf("preview-frame-rate=") > flattened.indexOf("preview-fps-range="));
			int[] range = new int[2];
			Range<Integer>[] available = mInfo.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
			if (!fpsUseSingleValue) {
				newParams.getPreviewFpsRange(range);
				range[0] /= FPS_TO_API_SCALE;
				range[1] /= FPS_TO_API_SCALE;
				boolean found = false;
				for (Range<Integer> r : available)
					found |= r.getLower() == range[0] && r.getUpper() == range[1];
				if (!found)
					{
						if (Camera.DEBUG)
							android.util.Log.i("Camera", "setParameters: check at line 531 fails");
						return false;
					}
			} else {
				int previewFps = newParams.getPreviewFrameRate();
				List<Range<Integer>> candidates = new ArrayList<Range<Integer>>();
				List<Range<Integer>> fastCandidates = new ArrayList<Range<Integer>>();
				for (Range<Integer> r : available) {
					if (!isFpsSupported(mAvailablePreviewSizes, r.getUpper()))
						continue;
					if (r.getLower() <= previewFps && previewFps <= r.getUpper())
						(r.getUpper() <= MAX_DEFAULT_FPS ? candidates : fastCandidates).add(r);
				}
				if (candidates.isEmpty() && fastCandidates.isEmpty())
					{
						if (Camera.DEBUG)
							android.util.Log.i("Camera", "setParameters: check at line 543 fails");
						return false;
					}
				List<Range<Integer>> ranges = candidates.isEmpty() ? fastCandidates : candidates;
				Range<Integer> best = ranges.get(0);
				for (Range<Integer> r : ranges) {
					if (recordingHint ? r.getLower() > best.getLower() : r.getLower() < best.getLower())
						best = r;
					else if (r.getLower().equals(best.getLower()) && r.getUpper() < best.getUpper())
						best = r;
				}
				range[0] = best.getLower();
				range[1] = best.getUpper();
			}
			/* the API-visible frame rate values follow what was set, the later key last */
			if (!fpsUseSingleValue) {
				newParams.setPreviewFrameRate(range[1]);
				newParams.setPreviewFpsRange(range[0] * FPS_TO_API_SCALE, range[1] * FPS_TO_API_SCALE);
			} else {
				newParams.setPreviewFpsRange(range[0] * FPS_TO_API_SCALE, range[1] * FPS_TO_API_SCALE);
				newParams.setPreviewFrameRate(newParams.getPreviewFrameRate());
			}

			String formatName = newParams.get("preview-format");
			int previewFormat = "yuv420sp".equals(formatName) ? HAL_PIXEL_FORMAT_YCRCB_420_SP : "yuv420p".equals(formatName) ? HAL_PIXEL_FORMAT_YV12 : -1;
			if (previewFormat != mPreviewFormat) {
				if (mState >= PREVIEW)
					{
						if (Camera.DEBUG)
							android.util.Log.i("Camera", "setParameters: check at line 568 fails");
						return false;
					}
				/* useFlexibleYuv: NV21 and YV12 from YCbCr_420_888; nothing else */
				if (previewFormat == -1)
					{
						if (Camera.DEBUG)
							android.util.Log.i("Camera", "setParameters: check at line 571 fails");
						return false;
					}
			}

			Camera.Size pictureSize = newParams.getPictureSize();
			if (pictureSize == null)
				{
					if (Camera.DEBUG)
						android.util.Log.i("Camera", "setParameters: check at line 576 fails");
					return false;
				}
			if (pictureSize.width != mPictureWidth || pictureSize.height != mPictureHeight) {
				boolean found = false;
				for (Size size : jpegSizes())
					found |= size.getWidth() == pictureSize.width && size.getHeight() == pictureSize.height;
				if (!found)
					{
						if (Camera.DEBUG)
							android.util.Log.i("Camera", "setParameters: check at line 582 fails");
						return false;
					}
			}

			mPreviewWidth = previewSize.width;
			mPreviewHeight = previewSize.height;
			mPreviewFpsRange = range;
			mPreviewFormat = previewFormat;
			/* JPEG_QUALITY: 1..100, else BAD_VALUE */
			String quality = newParams.get("jpeg-quality");
			int jpegQuality = quality != null ? Integer.parseInt(quality) : mJpegQuality;
			if (jpegQuality < 1 || jpegQuality > 100)
				{
					if (Camera.DEBUG)
						android.util.Log.i("Camera", "setParameters: check at line 593 fails");
					return false;
				}

			mPictureWidth = pictureSize.width;
			mPictureHeight = pictureSize.height;
			mJpegQuality = jpegQuality;
			mParamsFlattened = newParams.flatten();
			return true;
		}
	}

	/* Camera2Client::setPreviewTarget() -> setPreviewWindowL(); false is INVALID_OPERATION */
	boolean setPreviewTarget(Surface surface) {
		synchronized (mHalLock) {
			boolean stop;
			synchronized (mLock) {
				if (surface == mPreviewSurface)
					return true;
				if (mState == DISCONNECTED)
					return false;
				stop = mState == PREVIEW;
				if (stop)
					mState = WAITING_FOR_PREVIEW_WINDOW;
				mPreviewSurface = surface;
			}
			if (stop)
				stopStream();
			synchronized (mLock) {
				if (mState != WAITING_FOR_PREVIEW_WINDOW)
					return true;
			}
			return startPreview();
		}
	}

	/* Camera2Client::startPreviewL(): without a window the preview waits for one */
	boolean startPreview() {
		synchronized (mHalLock) {
			Surface target;
			int width, height, maxFps;
			synchronized (mLock) {
				if (mState == DISCONNECTED)
					return false;
				if (mState == PREVIEW)
					return true;
				if (mPreviewSurface == null) {
					if (Camera.DEBUG)
						android.util.Log.i("Camera", "startPreview: waiting for a preview window");
					mState = WAITING_FOR_PREVIEW_WINDOW;
					return true;
				}
				target = mPreviewSurface;
				width = mPreviewWidth;
				height = mPreviewHeight;
				maxFps = mPreviewFpsRange[1];
			}
			mHal.stopStream(this); /* a still capture's stream (picture size) may still run */
			mHal.setTargets(this, Collections.singletonList(target));
			boolean started = mHal.startStream(this, width, height, maxFps);
			if (Camera.DEBUG)
				android.util.Log.i("Camera", "startPreview " + width + "x" + height + " @" + maxFps + " target " + target + ": " + started);
			if (!started)
				mHal.setTargets(this, Collections.<Surface>emptyList());
			synchronized (mLock) {
				mState = started ? PREVIEW : STOPPED;
			}
			return started;
		}
	}

	/* with mHalLock, without mLock */
	private void stopStream() {
		mHal.setTargets(this, Collections.<Surface>emptyList());
		mHal.stopStream(this);
	}

	/* Camera2Client::stopPreviewL() */
	void stopPreview() {
		synchronized (mHalLock) {
			boolean stop;
			synchronized (mLock) {
				stop = mState == PREVIEW || mState == STILL_CAPTURE;
				mCapturePending = false;
				if (mState != DISCONNECTED)
					mState = STOPPED;
			}
			if (stop)
				stopStream();
		}
	}

	boolean previewEnabled() {
		synchronized (mLock) {
			return mState == PREVIEW;
		}
	}

	/* Camera2Client::setPreviewCallbackFlagL() */
	void setPreviewCallbackFlag(int flag) {
		synchronized (mLock) {
			mPreviewCallbackFlags = flag;
		}
	}

	/* Camera2Client::autoFocus(): INVALID_OPERATION (-38) without preview; with FOCUS_MODE_FIXED success at once
	 * (1); a camera with autofocus is not reported by the HAL (0: no immediate answer) */
	int autoFocus() {
		synchronized (mLock) {
			if (mState < PREVIEW)
				return -38;
			return mFocusFixed ? 1 : 0;
		}
	}

	boolean isConnected() {
		synchronized (mLock) {
			return mState != DISCONNECTED;
		}
	}

	/* commandSetDisplayOrientationL() -> Parameters::degToTransform(degrees, mirror): false is BAD_VALUE; the stream
	 * turns the frames for the preview Surface (horizontal flip of a front camera first, then clockwise) */
	boolean setDisplayOrientation(int degrees) {
		if (degrees != 0 && degrees != 90 && degrees != 180 && degrees != 270)
			return false;
		synchronized (mHalLock) {
			synchronized (mLock) {
				mPreviewTransform = degrees;
			}
			mHal.setDisplayTransform(this, degrees, mMirror);
		}
		return true;
	}

	/* Camera2Client::takePicture() with CaptureSequencer's standard capture (the HAL has no ZSL/reprocessing): only
	 * with preview running (else INVALID_OPERATION, -38); the JPEG has the picture size (the stream switches to it
	 * when the preview has another size), afterwards the state is STOPPED as after AOSP's manageDone() */
	int takePicture() {
		synchronized (mHalLock) {
			int width, height, maxFps;
			boolean restart;
			synchronized (mLock) {
				if (mState != PREVIEW)
					return -38;
				mState = STILL_CAPTURE;
				restart = mPictureWidth != mPreviewWidth || mPictureHeight != mPreviewHeight;
				mCaptureRestartsStream = restart;
				width = mPictureWidth;
				height = mPictureHeight;
				maxFps = mPreviewFpsRange[1];
				mCapturePending = true;
			}
			if (restart) {
				mHal.stopStream(this);
				if (!mHal.startStream(this, width, height, maxFps)) {
					synchronized (mLock) {
						mCapturePending = false;
						mState = STOPPED;
					}
					mHal.setTargets(this, Collections.<Surface>emptyList());
					return -38;
				}
			}
			return 0;
		}
	}

	void enableShutterSound(boolean enable) {
		synchronized (mLock) {
			mPlayShutterSound = enable;
		}
	}

	/* commandStartFaceDetectionL(): INVALID_OPERATION without preview, else BAD_VALUE (no face detection) */
	int startFaceDetection() {
		synchronized (mLock) {
			return mState >= PREVIEW ? -22 /* BAD_VALUE */ : -38 /* INVALID_OPERATION */;
		}
	}

	/* Camera2Client::disconnect() */
	void disconnect() {
		synchronized (mHalLock) {
			synchronized (mLock) {
				if (mState == DISCONNECTED)
					return;
				mState = DISCONNECTED;
				mPreviewSurface = null;
			}
			mHal.close(this);
		}
	}

	/** the frame in the preview format, during onPreviewFrame() */
	boolean copyFrame(byte[] dst) {
		int format, width, height;
		synchronized (mLock) {
			format = mPreviewFormat == HAL_PIXEL_FORMAT_YV12 ? ImageFormat.YV12 : ImageFormat.NV21;
			width = mPreviewWidth;
			height = mPreviewHeight;
		}
		return mHal.copyFrame(dst, format, width, height);
	}

	/* the size of a preview callback frame: NV21, or YV12 with 16-byte aligned strides */
	private static int frameSize(int format, int width, int height) {
		if (format == HAL_PIXEL_FORMAT_YV12) {
			int yStride = (width + 15) & ~15;
			int uvStride = (yStride / 2 + 15) & ~15;
			return yStride * height + uvStride * (height / 2) * 2;
		}
		return width * height * 3 / 2;
	}

	/* CallbackProcessor::onFrameAvailable(): a frame while preview callbacks are on; a one-shot callback turns
	 * them off (the FrameListener runs on the stream's thread) */
	@Override
	public void onFrame(long timestampNs) {
		boolean capture;
		int pictureWidth, pictureHeight, quality;
		synchronized (mLock) {
			capture = mCapturePending && mState == STILL_CAPTURE;
			pictureWidth = mPictureWidth;
			pictureHeight = mPictureHeight;
			quality = mJpegQuality;
			if (capture) {
				mCapturePending = false;
				mState = STOPPED;
			}
		}
		if (capture) {
			mCallbacks.onShutter();
			byte[] jpeg = mHal.encodeJpeg(pictureWidth, pictureHeight, quality);
			/* the preview stays stopped after the capture; the stream cannot end on its own thread */
			mWorker.execute(() -> {
				synchronized (mHalLock) {
					synchronized (mLock) {
						if (mState != STOPPED)
							return;
					}
					stopStream();
				}
			});
			mCallbacks.onPictureTaken(jpeg);
			return;
		}
		int size;
		synchronized (mLock) {
			if (mState != PREVIEW || (mPreviewCallbackFlags & CAMERA_FRAME_CALLBACK_FLAG_ENABLE_MASK) == 0)
				return;
			if ((mPreviewCallbackFlags & CAMERA_FRAME_CALLBACK_FLAG_ONE_SHOT_MASK) != 0)
				mPreviewCallbackFlags = 0;
			size = frameSize(mPreviewFormat, mPreviewWidth, mPreviewHeight);
		}
		mCallbacks.onPreviewFrame(this, size);
	}

	/* the camera is gone, or another client opened it (AOSP's service evicts the older client): the client
	 * hears ERROR_CAMERA_DISCONNECTED, which Camera2Client::notifyError() reports as CAMERA_ERROR_RELEASED
	 * (2, Camera.CAMERA_ERROR_EVICTED) */
	@Override
	public void onStreamError(String message) {
		synchronized (mLock) {
			if (mState == DISCONNECTED)
				return;
		}
		mCallbacks.onError(Camera.CAMERA_ERROR_EVICTED);
	}

	@Override
	public void onEvicted() {
		synchronized (mLock) {
			mState = DISCONNECTED;
			mPreviewSurface = null;
		}
		mCallbacks.onError(Camera.CAMERA_ERROR_EVICTED);
	}
}
