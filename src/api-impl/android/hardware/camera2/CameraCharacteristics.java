package android.hardware.camera2;

import android.graphics.Rect;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.util.SizeF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* AOSP core/java/android/hardware/camera2/CameraCharacteristics.java, reduced to the members the corpus
 * references. ATL has no camera service: CameraManager lists no camera and getCameraCharacteristics()
 * throws, so no instance is created and get() answers from an empty property map. The keys keep AOSP's
 * names and value types (keys whose value type is still missing in ATL are typed Object); Key follows
 * AOSP Key/CameraMetadataNative.Key (equality by name and type). */
@SuppressWarnings("unchecked")
public final class CameraCharacteristics {

	public static final class Key<T> {
		private final String mName;
		private final Class<T> mType;
		private final long mVendorId;

		public Key(String name, Class<T> type, long vendorId) {
			if (name == null)
				throw new NullPointerException("Key needs a valid name");
			if (type == null)
				throw new NullPointerException("Type needs to be non-null");
			mName = name;
			mType = type;
			mVendorId = vendorId;
		}

		public Key(String name, Class<T> type) {
			this(name, type, Long.MAX_VALUE);
		}

		public String getName() {
			return mName;
		}

		public long getVendorId() {
			return mVendorId;
		}

		@Override
		public final int hashCode() {
			return mName.hashCode() ^ mType.hashCode();
		}

		@Override
		public final boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Key))
				return false;
			Key<?> lhs = (Key<?>)o;
			return mName.equals(lhs.mName) && mType.equals(lhs.mType);
		}

		@Override
		public String toString() {
			return String.format("CameraCharacteristics.Key(%s)", mName);
		}
	}

	private final Map<Key<?>, Object> mProperties = new HashMap<Key<?>, Object>();

	CameraCharacteristics() {
	}

	public <T> T get(Key<T> key) {
		return (T) mProperties.get(key);
	}

	public List<Key<?>> getKeys() {
		return Collections.unmodifiableList(new ArrayList<Key<?>>(mProperties.keySet()));
	}

	/* AOSP getAvailableSessionCharacteristicsKeys(): AVAILABLE_SESSION_CHARACTERISTICS_KEYS_MAP has the
	 * one entry 35 -> {CONTROL_ZOOM_RATIO_RANGE, SCALER_AVAILABLE_MAX_DIGITAL_ZOOM} */
	public List<Key<?>> getAvailableSessionCharacteristicsKeys() {
		Integer queryVersion = get(INFO_SESSION_CONFIGURATION_QUERY_VERSION);
		if (queryVersion == null || queryVersion < 35)
			return List.of();
		return List.of(CONTROL_ZOOM_RATIO_RANGE, SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
	}

	public static final Key<Integer> INFO_SESSION_CONFIGURATION_QUERY_VERSION = new Key<Integer>("android.info.sessionConfigurationQueryVersion", (Class<Integer>) (Class<?>) int.class);

	public Set<String> getPhysicalCameraIds() {
		return Collections.unmodifiableSet(new HashSet<String>());
	}

	public static final Key<int[]> CONTROL_AE_AVAILABLE_MODES = new Key<int[]>("android.control.aeAvailableModes", int[].class);
	public static final Key<Range<Integer>[]> CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES = new Key<Range<Integer>[]>("android.control.aeAvailableTargetFpsRanges", (Class<Range<Integer>[]>) (Class<?>) Range[].class);
	public static final Key<Range<Integer>> CONTROL_AE_COMPENSATION_RANGE = new Key<Range<Integer>>("android.control.aeCompensationRange", (Class<Range<Integer>>) (Class<?>) Range.class);
	public static final Key<Rational> CONTROL_AE_COMPENSATION_STEP = new Key<Rational>("android.control.aeCompensationStep", Rational.class);
	public static final Key<int[]> CONTROL_AF_AVAILABLE_MODES = new Key<int[]>("android.control.afAvailableModes", int[].class);
	public static final Key<int[]> CONTROL_AVAILABLE_SETTINGS_OVERRIDES = new Key<int[]>("android.control.availableSettingsOverrides", int[].class);
	public static final Key<int[]> CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES = new Key<int[]>("android.control.availableVideoStabilizationModes", int[].class);
	public static final Key<int[]> CONTROL_AWB_AVAILABLE_MODES = new Key<int[]>("android.control.awbAvailableModes", int[].class);
	public static final Key<Integer> CONTROL_MAX_REGIONS_AE = new Key<Integer>("android.control.maxRegionsAe", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> CONTROL_MAX_REGIONS_AF = new Key<Integer>("android.control.maxRegionsAf", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> CONTROL_MAX_REGIONS_AWB = new Key<Integer>("android.control.maxRegionsAwb", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Range<Float>> CONTROL_ZOOM_RATIO_RANGE = new Key<Range<Float>>("android.control.zoomRatioRange", (Class<Range<Float>>) (Class<?>) Range.class);
	public static final Key<Boolean> FLASH_INFO_AVAILABLE = new Key<Boolean>("android.flash.info.available", (Class<Boolean>) (Class<?>) boolean.class);
	// FLASH_TORCH_STRENGTH_DEFAULT_LEVEL: not in the AOSP source at hand (newer API level), left out
	// FLASH_TORCH_STRENGTH_MAX_LEVEL: not in the AOSP source at hand (newer API level), left out
	/* value type DeviceStateSensorOrientationMap does not exist in ATL yet */
	public static final Key<Object> INFO_DEVICE_STATE_SENSOR_ORIENTATION_MAP = new Key<Object>("android.info.deviceStateSensorOrientationMap", Object.class);
	// INFO_SESSION_CONFIGURATION_QUERY_VERSION: not in the AOSP source at hand (newer API level), left out
	public static final Key<Integer> INFO_SUPPORTED_HARDWARE_LEVEL = new Key<Integer>("android.info.supportedHardwareLevel", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> LENS_FACING = new Key<Integer>("android.lens.facing", (Class<Integer>) (Class<?>) int.class);
	public static final Key<float[]> LENS_INFO_AVAILABLE_FOCAL_LENGTHS = new Key<float[]>("android.lens.info.availableFocalLengths", float[].class);
	public static final Key<int[]> LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION = new Key<int[]>("android.lens.info.availableOpticalStabilization", int[].class);
	public static final Key<Float> LENS_INFO_MINIMUM_FOCUS_DISTANCE = new Key<Float>("android.lens.info.minimumFocusDistance", (Class<Float>) (Class<?>) float.class);
	public static final Key<int[]> REQUEST_AVAILABLE_CAPABILITIES = new Key<int[]>("android.request.availableCapabilities", int[].class);
	/* value type DynamicRangeProfiles does not exist in ATL yet */
	public static final Key<Object> REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES = new Key<Object>("android.request.availableDynamicRangeProfiles", Object.class);
	public static final Key<Long> REQUEST_RECOMMENDED_TEN_BIT_DYNAMIC_RANGE_PROFILE = new Key<Long>("android.request.recommendedTenBitDynamicRangeProfile", (Class<Long>) (Class<?>) long.class);
	public static final Key<Float> SCALER_AVAILABLE_MAX_DIGITAL_ZOOM = new Key<Float>("android.scaler.availableMaxDigitalZoom", (Class<Float>) (Class<?>) float.class);
	public static final Key<long[]> SCALER_AVAILABLE_STREAM_USE_CASES = new Key<long[]>("android.scaler.availableStreamUseCases", long[].class);
	/* value type StreamConfigurationMap does not exist in ATL yet */
	public static final Key<Object> SCALER_STREAM_CONFIGURATION_MAP = new Key<Object>("android.scaler.streamConfigurationMap", Object.class);
	/* value type StreamConfigurationMap does not exist in ATL yet */
	public static final Key<Object> SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION = new Key<Object>("android.scaler.streamConfigurationMapMaximumResolution", Object.class);
	public static final Key<Rect> SENSOR_INFO_ACTIVE_ARRAY_SIZE = new Key<Rect>("android.sensor.info.activeArraySize", Rect.class);
	public static final Key<SizeF> SENSOR_INFO_PHYSICAL_SIZE = new Key<SizeF>("android.sensor.info.physicalSize", SizeF.class);
	public static final Key<Size> SENSOR_INFO_PIXEL_ARRAY_SIZE = new Key<Size>("android.sensor.info.pixelArraySize", Size.class);
	public static final Key<Integer> SENSOR_INFO_TIMESTAMP_SOURCE = new Key<Integer>("android.sensor.info.timestampSource", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> SENSOR_ORIENTATION = new Key<Integer>("android.sensor.orientation", (Class<Integer>) (Class<?>) int.class);

	/* AOSP getAvailable*Keys(): read from the request keys of the property map, which is empty in ATL (no
	 * instance is ever created): null for the optional lists, AOSP's AssertionError for the mandatory ones */
	public List<CaptureRequest.Key<?>> getAvailableSessionKeys() {
		return null;
	}

	public List<CaptureRequest.Key<?>> getAvailablePhysicalCameraRequestKeys() {
		return null;
	}

	public List<CaptureRequest.Key<?>> getAvailableCaptureRequestKeys() {
		throw new AssertionError("android.request.availableRequestKeys must be non-null in the characteristics");
	}

	public List<CaptureResult.Key<?>> getAvailableCaptureResultKeys() {
		throw new AssertionError("android.request.availableResultKeys must be non-null in the characteristics");
	}

	public static final Key<int[]> COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES = new Key<int[]>("android.colorCorrection.availableAberrationModes", int[].class);
	public static final Key<int[]> CONTROL_AE_AVAILABLE_ANTIBANDING_MODES = new Key<int[]>("android.control.aeAvailableAntibandingModes", int[].class);
	public static final Key<int[]> CONTROL_AVAILABLE_EFFECTS = new Key<int[]>("android.control.availableEffects", int[].class);
	public static final Key<int[]> CONTROL_AVAILABLE_SCENE_MODES = new Key<int[]>("android.control.availableSceneModes", int[].class);
	public static final Key<Boolean> CONTROL_AE_LOCK_AVAILABLE = new Key<Boolean>("android.control.aeLockAvailable", (Class<Boolean>) (Class<?>) boolean.class);
	public static final Key<Boolean> CONTROL_AWB_LOCK_AVAILABLE = new Key<Boolean>("android.control.awbLockAvailable", (Class<Boolean>) (Class<?>) boolean.class);
	public static final Key<int[]> CONTROL_AVAILABLE_MODES = new Key<int[]>("android.control.availableModes", int[].class);
	public static final Key<android.util.Range<Integer>> CONTROL_POST_RAW_SENSITIVITY_BOOST_RANGE = new Key<android.util.Range<Integer>>("android.control.postRawSensitivityBoostRange", new android.hardware.camera2.utils.TypeReference<android.util.Range<Integer>>() {{ }});
	/* value type Capability does not exist in ATL yet */
	public static final Key<Object> CONTROL_AVAILABLE_EXTENDED_SCENE_MODE_CAPABILITIES = new Key<Object>("android.control.availableExtendedSceneModeCapabilities", Object.class);
	public static final Key<Boolean> CONTROL_AUTOFRAMING_AVAILABLE = new Key<Boolean>("android.control.autoframingAvailable", (Class<Boolean>) (Class<?>) boolean.class);
	public static final Key<int[]> EDGE_AVAILABLE_EDGE_MODES = new Key<int[]>("android.edge.availableEdgeModes", int[].class);
	public static final Key<Integer> FLASH_INFO_STRENGTH_MAXIMUM_LEVEL = new Key<Integer>("android.flash.info.strengthMaximumLevel", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> FLASH_INFO_STRENGTH_DEFAULT_LEVEL = new Key<Integer>("android.flash.info.strengthDefaultLevel", (Class<Integer>) (Class<?>) int.class);
	public static final Key<int[]> HOT_PIXEL_AVAILABLE_HOT_PIXEL_MODES = new Key<int[]>("android.hotPixel.availableHotPixelModes", int[].class);
	public static final Key<android.util.Size[]> JPEG_AVAILABLE_THUMBNAIL_SIZES = new Key<android.util.Size[]>("android.jpeg.availableThumbnailSizes", android.util.Size[].class);
	public static final Key<float[]> LENS_INFO_AVAILABLE_APERTURES = new Key<float[]>("android.lens.info.availableApertures", float[].class);
	public static final Key<float[]> LENS_INFO_AVAILABLE_FILTER_DENSITIES = new Key<float[]>("android.lens.info.availableFilterDensities", float[].class);
	public static final Key<Float> LENS_INFO_HYPERFOCAL_DISTANCE = new Key<Float>("android.lens.info.hyperfocalDistance", (Class<Float>) (Class<?>) float.class);
	public static final Key<Integer> LENS_INFO_FOCUS_DISTANCE_CALIBRATION = new Key<Integer>("android.lens.info.focusDistanceCalibration", (Class<Integer>) (Class<?>) int.class);
	public static final Key<float[]> LENS_POSE_ROTATION = new Key<float[]>("android.lens.poseRotation", float[].class);
	public static final Key<float[]> LENS_POSE_TRANSLATION = new Key<float[]>("android.lens.poseTranslation", float[].class);
	public static final Key<float[]> LENS_INTRINSIC_CALIBRATION = new Key<float[]>("android.lens.intrinsicCalibration", float[].class);
	public static final Key<float[]> LENS_RADIAL_DISTORTION = new Key<float[]>("android.lens.radialDistortion", float[].class);
	public static final Key<Integer> LENS_POSE_REFERENCE = new Key<Integer>("android.lens.poseReference", (Class<Integer>) (Class<?>) int.class);
	public static final Key<float[]> LENS_DISTORTION = new Key<float[]>("android.lens.distortion", float[].class);
	public static final Key<float[]> LENS_DISTORTION_MAXIMUM_RESOLUTION = new Key<float[]>("android.lens.distortionMaximumResolution", float[].class);
	public static final Key<float[]> LENS_INTRINSIC_CALIBRATION_MAXIMUM_RESOLUTION = new Key<float[]>("android.lens.intrinsicCalibrationMaximumResolution", float[].class);
	public static final Key<int[]> NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES = new Key<int[]>("android.noiseReduction.availableNoiseReductionModes", int[].class);
	public static final Key<Integer> REQUEST_MAX_NUM_OUTPUT_RAW = new Key<Integer>("android.request.maxNumOutputRaw", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> REQUEST_MAX_NUM_OUTPUT_PROC = new Key<Integer>("android.request.maxNumOutputProc", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> REQUEST_MAX_NUM_OUTPUT_PROC_STALLING = new Key<Integer>("android.request.maxNumOutputProcStalling", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> REQUEST_MAX_NUM_INPUT_STREAMS = new Key<Integer>("android.request.maxNumInputStreams", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Byte> REQUEST_PIPELINE_MAX_DEPTH = new Key<Byte>("android.request.pipelineMaxDepth", (Class<Byte>) (Class<?>) byte.class);
	public static final Key<Integer> REQUEST_PARTIAL_RESULT_COUNT = new Key<Integer>("android.request.partialResultCount", (Class<Integer>) (Class<?>) int.class);
	/* value type ColorSpaceProfiles does not exist in ATL yet */
	public static final Key<Object> REQUEST_AVAILABLE_COLOR_SPACE_PROFILES = new Key<Object>("android.request.availableColorSpaceProfiles", Object.class);
	public static final Key<Integer> SCALER_CROPPING_TYPE = new Key<Integer>("android.scaler.croppingType", (Class<Integer>) (Class<?>) int.class);
	/* value type MandatoryStreamCombination does not exist in ATL yet */
	public static final Key<Object> SCALER_MANDATORY_STREAM_COMBINATIONS = new Key<Object>("android.scaler.mandatoryStreamCombinations", Object.class);
	/* value type MandatoryStreamCombination does not exist in ATL yet */
	public static final Key<Object> SCALER_MANDATORY_CONCURRENT_STREAM_COMBINATIONS = new Key<Object>("android.scaler.mandatoryConcurrentStreamCombinations", Object.class);
	public static final Key<int[]> SCALER_AVAILABLE_ROTATE_AND_CROP_MODES = new Key<int[]>("android.scaler.availableRotateAndCropModes", int[].class);
	public static final Key<android.util.Size> SCALER_DEFAULT_SECURE_IMAGE_SIZE = new Key<android.util.Size>("android.scaler.defaultSecureImageSize", android.util.Size.class);
	/* value type MultiResolutionStreamConfigurationMap does not exist in ATL yet */
	public static final Key<Object> SCALER_MULTI_RESOLUTION_STREAM_CONFIGURATION_MAP = new Key<Object>("android.scaler.multiResolutionStreamConfigurationMap", Object.class);
	/* value type MandatoryStreamCombination does not exist in ATL yet */
	public static final Key<Object> SCALER_MANDATORY_MAXIMUM_RESOLUTION_STREAM_COMBINATIONS = new Key<Object>("android.scaler.mandatoryMaximumResolutionStreamCombinations", Object.class);
	/* value type MandatoryStreamCombination does not exist in ATL yet */
	public static final Key<Object> SCALER_MANDATORY_TEN_BIT_OUTPUT_STREAM_COMBINATIONS = new Key<Object>("android.scaler.mandatoryTenBitOutputStreamCombinations", Object.class);
	/* value type MandatoryStreamCombination does not exist in ATL yet */
	public static final Key<Object> SCALER_MANDATORY_PREVIEW_STABILIZATION_OUTPUT_STREAM_COMBINATIONS = new Key<Object>("android.scaler.mandatoryPreviewStabilizationOutputStreamCombinations", Object.class);
	/* value type MandatoryStreamCombination does not exist in ATL yet */
	public static final Key<Object> SCALER_MANDATORY_USE_CASE_STREAM_COMBINATIONS = new Key<Object>("android.scaler.mandatoryUseCaseStreamCombinations", Object.class);
	public static final Key<android.util.Range<Integer>> SENSOR_INFO_SENSITIVITY_RANGE = new Key<android.util.Range<Integer>>("android.sensor.info.sensitivityRange", new android.hardware.camera2.utils.TypeReference<android.util.Range<Integer>>() {{ }});
	public static final Key<Integer> SENSOR_INFO_COLOR_FILTER_ARRANGEMENT = new Key<Integer>("android.sensor.info.colorFilterArrangement", (Class<Integer>) (Class<?>) int.class);
	public static final Key<android.util.Range<Long>> SENSOR_INFO_EXPOSURE_TIME_RANGE = new Key<android.util.Range<Long>>("android.sensor.info.exposureTimeRange", new android.hardware.camera2.utils.TypeReference<android.util.Range<Long>>() {{ }});
	public static final Key<Long> SENSOR_INFO_MAX_FRAME_DURATION = new Key<Long>("android.sensor.info.maxFrameDuration", (Class<Long>) (Class<?>) long.class);
	public static final Key<Integer> SENSOR_INFO_WHITE_LEVEL = new Key<Integer>("android.sensor.info.whiteLevel", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Boolean> SENSOR_INFO_LENS_SHADING_APPLIED = new Key<Boolean>("android.sensor.info.lensShadingApplied", (Class<Boolean>) (Class<?>) boolean.class);
	public static final Key<android.graphics.Rect> SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE = new Key<android.graphics.Rect>("android.sensor.info.preCorrectionActiveArraySize", android.graphics.Rect.class);
	public static final Key<android.graphics.Rect> SENSOR_INFO_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION = new Key<android.graphics.Rect>("android.sensor.info.activeArraySizeMaximumResolution", android.graphics.Rect.class);
	public static final Key<android.util.Size> SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION = new Key<android.util.Size>("android.sensor.info.pixelArraySizeMaximumResolution", android.util.Size.class);
	public static final Key<android.graphics.Rect> SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION = new Key<android.graphics.Rect>("android.sensor.info.preCorrectionActiveArraySizeMaximumResolution", android.graphics.Rect.class);
	public static final Key<android.util.Size> SENSOR_INFO_BINNING_FACTOR = new Key<android.util.Size>("android.sensor.info.binningFactor", android.util.Size.class);
	public static final Key<Integer> SENSOR_REFERENCE_ILLUMINANT1 = new Key<Integer>("android.sensor.referenceIlluminant1", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Byte> SENSOR_REFERENCE_ILLUMINANT2 = new Key<Byte>("android.sensor.referenceIlluminant2", (Class<Byte>) (Class<?>) byte.class);
	/* value type ColorSpaceTransform does not exist in ATL yet */
	public static final Key<Object> SENSOR_CALIBRATION_TRANSFORM1 = new Key<Object>("android.sensor.calibrationTransform1", Object.class);
	/* value type ColorSpaceTransform does not exist in ATL yet */
	public static final Key<Object> SENSOR_CALIBRATION_TRANSFORM2 = new Key<Object>("android.sensor.calibrationTransform2", Object.class);
	/* value type ColorSpaceTransform does not exist in ATL yet */
	public static final Key<Object> SENSOR_COLOR_TRANSFORM1 = new Key<Object>("android.sensor.colorTransform1", Object.class);
	/* value type ColorSpaceTransform does not exist in ATL yet */
	public static final Key<Object> SENSOR_COLOR_TRANSFORM2 = new Key<Object>("android.sensor.colorTransform2", Object.class);
	/* value type ColorSpaceTransform does not exist in ATL yet */
	public static final Key<Object> SENSOR_FORWARD_MATRIX1 = new Key<Object>("android.sensor.forwardMatrix1", Object.class);
	/* value type ColorSpaceTransform does not exist in ATL yet */
	public static final Key<Object> SENSOR_FORWARD_MATRIX2 = new Key<Object>("android.sensor.forwardMatrix2", Object.class);
	/* value type BlackLevelPattern does not exist in ATL yet */
	public static final Key<Object> SENSOR_BLACK_LEVEL_PATTERN = new Key<Object>("android.sensor.blackLevelPattern", Object.class);
	public static final Key<Integer> SENSOR_MAX_ANALOG_SENSITIVITY = new Key<Integer>("android.sensor.maxAnalogSensitivity", (Class<Integer>) (Class<?>) int.class);
	public static final Key<int[]> SENSOR_AVAILABLE_TEST_PATTERN_MODES = new Key<int[]>("android.sensor.availableTestPatternModes", int[].class);
	public static final Key<android.graphics.Rect[]> SENSOR_OPTICAL_BLACK_REGIONS = new Key<android.graphics.Rect[]>("android.sensor.opticalBlackRegions", android.graphics.Rect[].class);
	public static final Key<Integer> SENSOR_READOUT_TIMESTAMP = new Key<Integer>("android.sensor.readoutTimestamp", (Class<Integer>) (Class<?>) int.class);
	public static final Key<int[]> SHADING_AVAILABLE_MODES = new Key<int[]>("android.shading.availableModes", int[].class);
	public static final Key<int[]> STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES = new Key<int[]>("android.statistics.info.availableFaceDetectModes", int[].class);
	public static final Key<Integer> STATISTICS_INFO_MAX_FACE_COUNT = new Key<Integer>("android.statistics.info.maxFaceCount", (Class<Integer>) (Class<?>) int.class);
	public static final Key<boolean[]> STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES = new Key<boolean[]>("android.statistics.info.availableHotPixelMapModes", boolean[].class);
	public static final Key<int[]> STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES = new Key<int[]>("android.statistics.info.availableLensShadingMapModes", int[].class);
	public static final Key<int[]> STATISTICS_INFO_AVAILABLE_OIS_DATA_MODES = new Key<int[]>("android.statistics.info.availableOisDataModes", int[].class);
	public static final Key<Integer> TONEMAP_MAX_CURVE_POINTS = new Key<Integer>("android.tonemap.maxCurvePoints", (Class<Integer>) (Class<?>) int.class);
	public static final Key<int[]> TONEMAP_AVAILABLE_TONE_MAP_MODES = new Key<int[]>("android.tonemap.availableToneMapModes", int[].class);
	public static final Key<String> INFO_VERSION = new Key<String>("android.info.version", String.class);
	public static final Key<Integer> SYNC_MAX_LATENCY = new Key<Integer>("android.sync.maxLatency", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> REPROCESS_MAX_CAPTURE_STALL = new Key<Integer>("android.reprocess.maxCaptureStall", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Boolean> DEPTH_DEPTH_IS_EXCLUSIVE = new Key<Boolean>("android.depth.depthIsExclusive", (Class<Boolean>) (Class<?>) boolean.class);
	public static final Key<Integer> LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE = new Key<Integer>("android.logicalMultiCamera.sensorSyncType", (Class<Integer>) (Class<?>) int.class);
	public static final Key<int[]> DISTORTION_CORRECTION_AVAILABLE_MODES = new Key<int[]>("android.distortionCorrection.availableModes", int[].class);
	public static final Key<int[]> AUTOMOTIVE_LENS_FACING = new Key<int[]>("android.automotive.lens.facing", int[].class);
	public static final Key<Integer> AUTOMOTIVE_LOCATION = new Key<Integer>("android.automotive.location", (Class<Integer>) (Class<?>) int.class);
}
