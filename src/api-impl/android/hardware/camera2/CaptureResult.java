package android.hardware.camera2;

import android.view.Surface;
import android.hardware.camera2.impl.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/* AOSP core/java/android/hardware/camera2/CaptureResult.java, reduced. A capture of ATL's camera HAL gives its
 * result as a map of the public keys (AOSP: CameraMetadataNative); the class keeps AOSP's keys (types still
 * missing in ATL are typed Object). */
@SuppressWarnings("unchecked")
public class CaptureResult extends CameraMetadata<CaptureResult.Key<?>> {

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
			return String.format("CaptureResult.Key(%s)", mName);
		}
	}

	private final Map<Key<?>, Object> mResults = new HashMap<Key<?>, Object>();
	private final String mCameraId;
	private final CaptureRequest mRequest;
	private final int mSequenceId;
	private final long mFrameNumber;

	/** @hide AOSP CaptureResult(cameraId, CameraMetadataNative results, parent, extras): the extras are the
	 * sequence id and the frame number */
	public CaptureResult(String cameraId, Map<Key<?>, Object> results, CaptureRequest parent, int sequenceId, long frameNumber) {
		if (results == null) {
			throw new IllegalArgumentException("results was null");
		}
		if (parent == null) {
			throw new IllegalArgumentException("parent was null");
		}
		mCameraId = cameraId;
		mResults.putAll(results);
		mRequest = parent;
		mSequenceId = sequenceId;
		mFrameNumber = frameNumber;
	}

	public String getCameraId() {
		return mCameraId;
	}

	@Override
	public List<Key<?>> getKeys() {
		return super.getKeys();
	}

	public <T> T get(Key<T> key) {
		return (T) mResults.get(key);
	}

	/** @hide AOSP getProtected() */
	@Override
	protected <T> T getProtected(Key<?> key) {
		return (T) mResults.get(key);
	}

	/** @hide */
	@Override
	protected Class<Key<?>> getKeyClass() {
		Object thisClass = Key.class;
		return (Class<Key<?>>)thisClass;
	}

	public CaptureRequest getRequest() {
		return mRequest;
	}

	public long getFrameNumber() {
		return mFrameNumber;
	}

	public int getSequenceId() {
		return mSequenceId;
	}

	/** @hide the public keys with these names (AOSP CameraCharacteristics.getAvailableKeyList() reflects over the
	 * key fields the same way) */
	public static List<Key<?>> keysNamed(String[] names) {
		java.util.Set<String> wanted = new HashSet<String>(java.util.Arrays.asList(names));
		List<Key<?>> keys = new ArrayList<Key<?>>();
		for (java.lang.reflect.Field field : CaptureResult.class.getDeclaredFields()) {
			if (field.getType() != Key.class || !java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.getAnnotation(PublicKey.class) == null)
				continue;
			try {
				Key<?> key = (Key<?>)field.get(null);
				if (wanted.contains(key.getName()))
					keys.add(key);
			} catch (IllegalAccessException e) {
			}
		}
		return Collections.unmodifiableList(keys);
	}

	@PublicKey public static final Key<Integer> COLOR_CORRECTION_MODE = new Key<Integer>("android.colorCorrection.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.hardware.camera2.params.ColorSpaceTransform> COLOR_CORRECTION_TRANSFORM = new Key<android.hardware.camera2.params.ColorSpaceTransform>("android.colorCorrection.transform", android.hardware.camera2.params.ColorSpaceTransform.class);
	@PublicKey public static final Key<android.hardware.camera2.params.RggbChannelVector> COLOR_CORRECTION_GAINS = new Key<android.hardware.camera2.params.RggbChannelVector>("android.colorCorrection.gains", android.hardware.camera2.params.RggbChannelVector.class);
	@PublicKey public static final Key<Integer> COLOR_CORRECTION_ABERRATION_MODE = new Key<Integer>("android.colorCorrection.aberrationMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_AE_ANTIBANDING_MODE = new Key<Integer>("android.control.aeAntibandingMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_AE_EXPOSURE_COMPENSATION = new Key<Integer>("android.control.aeExposureCompensation", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Boolean> CONTROL_AE_LOCK = new Key<Boolean>("android.control.aeLock", (Class<Boolean>) (Class<?>) boolean.class);
	@PublicKey public static final Key<Integer> CONTROL_AE_MODE = new Key<Integer>("android.control.aeMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.hardware.camera2.params.MeteringRectangle[]> CONTROL_AE_REGIONS = new Key<android.hardware.camera2.params.MeteringRectangle[]>("android.control.aeRegions", android.hardware.camera2.params.MeteringRectangle[].class);
	@PublicKey public static final Key<android.util.Range<Integer>> CONTROL_AE_TARGET_FPS_RANGE = new Key<android.util.Range<Integer>>("android.control.aeTargetFpsRange", (Class<android.util.Range<Integer>>) (Class<?>) android.util.Range.class);
	@PublicKey public static final Key<Integer> CONTROL_AE_PRECAPTURE_TRIGGER = new Key<Integer>("android.control.aePrecaptureTrigger", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_AE_STATE = new Key<Integer>("android.control.aeState", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_AF_MODE = new Key<Integer>("android.control.afMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.hardware.camera2.params.MeteringRectangle[]> CONTROL_AF_REGIONS = new Key<android.hardware.camera2.params.MeteringRectangle[]>("android.control.afRegions", android.hardware.camera2.params.MeteringRectangle[].class);
	@PublicKey public static final Key<Integer> CONTROL_AF_TRIGGER = new Key<Integer>("android.control.afTrigger", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_AF_STATE = new Key<Integer>("android.control.afState", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Boolean> CONTROL_AWB_LOCK = new Key<Boolean>("android.control.awbLock", (Class<Boolean>) (Class<?>) boolean.class);
	@PublicKey public static final Key<Integer> CONTROL_AWB_MODE = new Key<Integer>("android.control.awbMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.hardware.camera2.params.MeteringRectangle[]> CONTROL_AWB_REGIONS = new Key<android.hardware.camera2.params.MeteringRectangle[]>("android.control.awbRegions", android.hardware.camera2.params.MeteringRectangle[].class);
	@PublicKey public static final Key<Integer> CONTROL_CAPTURE_INTENT = new Key<Integer>("android.control.captureIntent", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_AWB_STATE = new Key<Integer>("android.control.awbState", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_EFFECT_MODE = new Key<Integer>("android.control.effectMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_MODE = new Key<Integer>("android.control.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_SCENE_MODE = new Key<Integer>("android.control.sceneMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_VIDEO_STABILIZATION_MODE = new Key<Integer>("android.control.videoStabilizationMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_POST_RAW_SENSITIVITY_BOOST = new Key<Integer>("android.control.postRawSensitivityBoost", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Boolean> CONTROL_ENABLE_ZSL = new Key<Boolean>("android.control.enableZsl", (Class<Boolean>) (Class<?>) boolean.class);
	@PublicKey public static final Key<Integer> CONTROL_AF_SCENE_CHANGE = new Key<Integer>("android.control.afSceneChange", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_EXTENDED_SCENE_MODE = new Key<Integer>("android.control.extendedSceneMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Float> CONTROL_ZOOM_RATIO = new Key<Float>("android.control.zoomRatio", (Class<Float>) (Class<?>) float.class);
	@PublicKey public static final Key<Integer> CONTROL_SETTINGS_OVERRIDE = new Key<Integer>("android.control.settingsOverride", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_AUTOFRAMING = new Key<Integer>("android.control.autoframing", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_AUTOFRAMING_STATE = new Key<Integer>("android.control.autoframingState", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_LOW_LIGHT_BOOST_STATE = new Key<Integer>("android.control.lowLightBoostState", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> EDGE_MODE = new Key<Integer>("android.edge.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> FLASH_MODE = new Key<Integer>("android.flash.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> FLASH_STATE = new Key<Integer>("android.flash.state", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> FLASH_STRENGTH_LEVEL = new Key<Integer>("android.flash.strengthLevel", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> HOT_PIXEL_MODE = new Key<Integer>("android.hotPixel.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.location.Location> JPEG_GPS_LOCATION = new Key<android.location.Location>("android.jpeg.gpsLocation", android.location.Location.class);
	@PublicKey public static final Key<Integer> JPEG_ORIENTATION = new Key<Integer>("android.jpeg.orientation", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Byte> JPEG_QUALITY = new Key<Byte>("android.jpeg.quality", (Class<Byte>) (Class<?>) byte.class);
	@PublicKey public static final Key<Byte> JPEG_THUMBNAIL_QUALITY = new Key<Byte>("android.jpeg.thumbnailQuality", (Class<Byte>) (Class<?>) byte.class);
	@PublicKey public static final Key<android.util.Size> JPEG_THUMBNAIL_SIZE = new Key<android.util.Size>("android.jpeg.thumbnailSize", android.util.Size.class);
	@PublicKey public static final Key<Float> LENS_APERTURE = new Key<Float>("android.lens.aperture", (Class<Float>) (Class<?>) float.class);
	@PublicKey public static final Key<Float> LENS_FILTER_DENSITY = new Key<Float>("android.lens.filterDensity", (Class<Float>) (Class<?>) float.class);
	@PublicKey public static final Key<Float> LENS_FOCAL_LENGTH = new Key<Float>("android.lens.focalLength", (Class<Float>) (Class<?>) float.class);
	@PublicKey public static final Key<Float> LENS_FOCUS_DISTANCE = new Key<Float>("android.lens.focusDistance", (Class<Float>) (Class<?>) float.class);
	@PublicKey public static final Key<android.util.Pair<Float,Float>> LENS_FOCUS_RANGE = new Key<android.util.Pair<Float,Float>>("android.lens.focusRange", (Class<android.util.Pair<Float,Float>>) (Class<?>) android.util.Pair.class);
	@PublicKey public static final Key<Integer> LENS_OPTICAL_STABILIZATION_MODE = new Key<Integer>("android.lens.opticalStabilizationMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> LENS_STATE = new Key<Integer>("android.lens.state", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<float[]> LENS_POSE_ROTATION = new Key<float[]>("android.lens.poseRotation", float[].class);
	@PublicKey public static final Key<float[]> LENS_POSE_TRANSLATION = new Key<float[]>("android.lens.poseTranslation", float[].class);
	@PublicKey public static final Key<float[]> LENS_INTRINSIC_CALIBRATION = new Key<float[]>("android.lens.intrinsicCalibration", float[].class);
	@PublicKey public static final Key<float[]> LENS_RADIAL_DISTORTION = new Key<float[]>("android.lens.radialDistortion", float[].class);
	@PublicKey public static final Key<float[]> LENS_DISTORTION = new Key<float[]>("android.lens.distortion", float[].class);
	@PublicKey public static final Key<Integer> NOISE_REDUCTION_MODE = new Key<Integer>("android.noiseReduction.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Byte> REQUEST_PIPELINE_DEPTH = new Key<Byte>("android.request.pipelineDepth", (Class<Byte>) (Class<?>) byte.class);
	@PublicKey public static final Key<android.graphics.Rect> SCALER_CROP_REGION = new Key<android.graphics.Rect>("android.scaler.cropRegion", android.graphics.Rect.class);
	@PublicKey public static final Key<Integer> SCALER_ROTATE_AND_CROP = new Key<Integer>("android.scaler.rotateAndCrop", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.graphics.Rect> SCALER_RAW_CROP_REGION = new Key<android.graphics.Rect>("android.scaler.rawCropRegion", android.graphics.Rect.class);
	@PublicKey public static final Key<Long> SENSOR_EXPOSURE_TIME = new Key<Long>("android.sensor.exposureTime", (Class<Long>) (Class<?>) long.class);
	@PublicKey public static final Key<Long> SENSOR_FRAME_DURATION = new Key<Long>("android.sensor.frameDuration", (Class<Long>) (Class<?>) long.class);
	@PublicKey public static final Key<Integer> SENSOR_SENSITIVITY = new Key<Integer>("android.sensor.sensitivity", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Long> SENSOR_TIMESTAMP = new Key<Long>("android.sensor.timestamp", (Class<Long>) (Class<?>) long.class);
	@PublicKey public static final Key<android.util.Rational[]> SENSOR_NEUTRAL_COLOR_POINT = new Key<android.util.Rational[]>("android.sensor.neutralColorPoint", android.util.Rational[].class);
	@PublicKey public static final Key<android.util.Pair<Double,Double>[]> SENSOR_NOISE_PROFILE = new Key<android.util.Pair<Double,Double>[]>("android.sensor.noiseProfile", (Class<android.util.Pair<Double,Double>[]>) (Class<?>) android.util.Pair[].class);
	@PublicKey public static final Key<Float> SENSOR_GREEN_SPLIT = new Key<Float>("android.sensor.greenSplit", (Class<Float>) (Class<?>) float.class);
	@PublicKey public static final Key<int[]> SENSOR_TEST_PATTERN_DATA = new Key<int[]>("android.sensor.testPatternData", int[].class);
	@PublicKey public static final Key<Integer> SENSOR_TEST_PATTERN_MODE = new Key<Integer>("android.sensor.testPatternMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Long> SENSOR_ROLLING_SHUTTER_SKEW = new Key<Long>("android.sensor.rollingShutterSkew", (Class<Long>) (Class<?>) long.class);
	@PublicKey public static final Key<float[]> SENSOR_DYNAMIC_BLACK_LEVEL = new Key<float[]>("android.sensor.dynamicBlackLevel", float[].class);
	@PublicKey public static final Key<Integer> SENSOR_DYNAMIC_WHITE_LEVEL = new Key<Integer>("android.sensor.dynamicWhiteLevel", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> SENSOR_PIXEL_MODE = new Key<Integer>("android.sensor.pixelMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Boolean> SENSOR_RAW_BINNING_FACTOR_USED = new Key<Boolean>("android.sensor.rawBinningFactorUsed", (Class<Boolean>) (Class<?>) boolean.class);
	@PublicKey public static final Key<Integer> SHADING_MODE = new Key<Integer>("android.shading.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> STATISTICS_FACE_DETECT_MODE = new Key<Integer>("android.statistics.faceDetectMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.hardware.camera2.params.Face[]> STATISTICS_FACES = new Key<android.hardware.camera2.params.Face[]>("android.statistics.faces", android.hardware.camera2.params.Face[].class);
	@PublicKey public static final Key<android.hardware.camera2.params.LensShadingMap> STATISTICS_LENS_SHADING_CORRECTION_MAP = new Key<android.hardware.camera2.params.LensShadingMap>("android.statistics.lensShadingCorrectionMap", android.hardware.camera2.params.LensShadingMap.class);
	@PublicKey public static final Key<Integer> STATISTICS_SCENE_FLICKER = new Key<Integer>("android.statistics.sceneFlicker", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Boolean> STATISTICS_HOT_PIXEL_MAP_MODE = new Key<Boolean>("android.statistics.hotPixelMapMode", (Class<Boolean>) (Class<?>) boolean.class);
	@PublicKey public static final Key<android.graphics.Point[]> STATISTICS_HOT_PIXEL_MAP = new Key<android.graphics.Point[]>("android.statistics.hotPixelMap", android.graphics.Point[].class);
	@PublicKey public static final Key<Integer> STATISTICS_LENS_SHADING_MAP_MODE = new Key<Integer>("android.statistics.lensShadingMapMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> STATISTICS_OIS_DATA_MODE = new Key<Integer>("android.statistics.oisDataMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.hardware.camera2.params.OisSample[]> STATISTICS_OIS_SAMPLES = new Key<android.hardware.camera2.params.OisSample[]>("android.statistics.oisSamples", android.hardware.camera2.params.OisSample[].class);
	@PublicKey public static final Key<android.hardware.camera2.params.TonemapCurve> TONEMAP_CURVE = new Key<android.hardware.camera2.params.TonemapCurve>("android.tonemap.curve", android.hardware.camera2.params.TonemapCurve.class);
	@PublicKey public static final Key<Integer> TONEMAP_MODE = new Key<Integer>("android.tonemap.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Float> TONEMAP_GAMMA = new Key<Float>("android.tonemap.gamma", (Class<Float>) (Class<?>) float.class);
	@PublicKey public static final Key<Integer> TONEMAP_PRESET_CURVE = new Key<Integer>("android.tonemap.presetCurve", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Boolean> BLACK_LEVEL_LOCK = new Key<Boolean>("android.blackLevel.lock", (Class<Boolean>) (Class<?>) boolean.class);
	@PublicKey public static final Key<Float> REPROCESS_EFFECTIVE_EXPOSURE_FACTOR = new Key<Float>("android.reprocess.effectiveExposureFactor", (Class<Float>) (Class<?>) float.class);
	@PublicKey public static final Key<String> LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID = new Key<String>("android.logicalMultiCamera.activePhysicalId", String.class);
	@PublicKey public static final Key<Integer> DISTORTION_CORRECTION_MODE = new Key<Integer>("android.distortionCorrection.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> EXTENSION_CURRENT_TYPE = new Key<Integer>("android.extension.currentType", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> EXTENSION_STRENGTH = new Key<Integer>("android.extension.strength", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> COLOR_CORRECTION_COLOR_TEMPERATURE = new Key<Integer>("android.colorCorrection.colorTemperature", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> COLOR_CORRECTION_COLOR_TINT = new Key<Integer>("android.colorCorrection.colorTint", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_AE_PRIORITY_MODE = new Key<Integer>("android.control.aePriorityMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_ZOOM_METHOD = new Key<Integer>("android.control.zoomMethod", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> EXTENSION_NIGHT_MODE_INDICATOR = new Key<Integer>("android.extension.nightModeIndicator", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.graphics.Rect> LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_SENSOR_CROP_REGION = new Key<android.graphics.Rect>("android.logicalMultiCamera.activePhysicalSensorCropRegion", android.graphics.Rect.class);
	@PublicKey public static final Key<android.hardware.camera2.params.LensIntrinsicsSample[]> STATISTICS_LENS_INTRINSICS_SAMPLES = new Key<android.hardware.camera2.params.LensIntrinsicsSample[]>("android.statistics.lensIntrinsicsSamples", android.hardware.camera2.params.LensIntrinsicsSample[].class);
}
