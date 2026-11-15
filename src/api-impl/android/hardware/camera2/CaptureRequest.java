package android.hardware.camera2;

import android.view.Surface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/* AOSP core/java/android/hardware/camera2/CaptureRequest.java, reduced. ATL has no camera service, so
 * CameraDevice.createCaptureRequest() is never reachable and no request is built for a camera; the
 * request and its Builder keep their settings, targets and tag in plain maps as AOSP's
 * CameraMetadataNative does. The public keys keep AOSP's names and value types (types still missing in
 * ATL are typed Object); Key follows AOSP Key/CameraMetadataNative.Key. */
@SuppressWarnings("unchecked")
public final class CaptureRequest {

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
			return String.format("CaptureRequest.Key(%s)", mName);
		}
	}

	private final Map<Key<?>, Object> mSettings;
	private final HashSet<Surface> mSurfaceSet;
	private final boolean mIsReprocess;
	private Object mUserTag;

	private CaptureRequest(boolean reprocess) {
		mSettings = new HashMap<Key<?>, Object>();
		mSurfaceSet = new HashSet<Surface>();
		mIsReprocess = reprocess;
	}

	private CaptureRequest(CaptureRequest source) {
		mSettings = new HashMap<Key<?>, Object>(source.mSettings);
		mSurfaceSet = new HashSet<Surface>(source.mSurfaceSet);
		mIsReprocess = source.mIsReprocess;
		mUserTag = source.mUserTag;
	}

	public <T> T get(Key<T> key) {
		return (T) mSettings.get(key);
	}

	public List<Key<?>> getKeys() {
		return Collections.unmodifiableList(new ArrayList<Key<?>>(mSettings.keySet()));
	}

	public Object getTag() {
		return mUserTag;
	}

	public boolean isReprocess() {
		return mIsReprocess;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof CaptureRequest))
			return false;
		CaptureRequest o = (CaptureRequest)other;
		return mSettings.equals(o.mSettings) && mSurfaceSet.equals(o.mSurfaceSet) && mIsReprocess == o.mIsReprocess;
	}

	@Override
	public int hashCode() {
		return mSettings.hashCode() ^ mSurfaceSet.hashCode();
	}

	/* AOSP CaptureRequest.Builder */
	public static final class Builder {
		private final CaptureRequest mRequest;

		Builder(boolean reprocess) {
			mRequest = new CaptureRequest(reprocess);
		}

		public void addTarget(Surface outputTarget) {
			mRequest.mSurfaceSet.add(outputTarget);
		}

		public void removeTarget(Surface outputTarget) {
			mRequest.mSurfaceSet.remove(outputTarget);
		}

		public <T> void set(Key<T> key, T value) {
			if (value == null)
				mRequest.mSettings.remove(key);
			else
				mRequest.mSettings.put(key, value);
		}

		public <T> T get(Key<T> key) {
			return (T) mRequest.mSettings.get(key);
		}

		public void setTag(Object tag) {
			mRequest.mUserTag = tag;
		}

		public CaptureRequest build() {
			return new CaptureRequest(mRequest);
		}
	}

	public static final Key<Integer> COLOR_CORRECTION_MODE = new Key<Integer>("android.colorCorrection.mode", (Class<Integer>) (Class<?>) int.class);
	/* value type ColorSpaceTransform does not exist in ATL yet */
	public static final Key<Object> COLOR_CORRECTION_TRANSFORM = new Key<Object>("android.colorCorrection.transform", Object.class);
	public static final Key<android.hardware.camera2.params.RggbChannelVector> COLOR_CORRECTION_GAINS = new Key<android.hardware.camera2.params.RggbChannelVector>("android.colorCorrection.gains", android.hardware.camera2.params.RggbChannelVector.class);
	public static final Key<Integer> COLOR_CORRECTION_ABERRATION_MODE = new Key<Integer>("android.colorCorrection.aberrationMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> CONTROL_AE_ANTIBANDING_MODE = new Key<Integer>("android.control.aeAntibandingMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> CONTROL_AE_EXPOSURE_COMPENSATION = new Key<Integer>("android.control.aeExposureCompensation", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Boolean> CONTROL_AE_LOCK = new Key<Boolean>("android.control.aeLock", (Class<Boolean>) (Class<?>) boolean.class);
	public static final Key<Integer> CONTROL_AE_MODE = new Key<Integer>("android.control.aeMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<android.hardware.camera2.params.MeteringRectangle[]> CONTROL_AE_REGIONS = new Key<android.hardware.camera2.params.MeteringRectangle[]>("android.control.aeRegions", android.hardware.camera2.params.MeteringRectangle[].class);
	public static final Key<android.util.Range<Integer>> CONTROL_AE_TARGET_FPS_RANGE = new Key<android.util.Range<Integer>>("android.control.aeTargetFpsRange", new android.hardware.camera2.utils.TypeReference<android.util.Range<Integer>>() {{ }});
	public static final Key<Integer> CONTROL_AE_PRECAPTURE_TRIGGER = new Key<Integer>("android.control.aePrecaptureTrigger", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> CONTROL_AF_MODE = new Key<Integer>("android.control.afMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<android.hardware.camera2.params.MeteringRectangle[]> CONTROL_AF_REGIONS = new Key<android.hardware.camera2.params.MeteringRectangle[]>("android.control.afRegions", android.hardware.camera2.params.MeteringRectangle[].class);
	public static final Key<Integer> CONTROL_AF_TRIGGER = new Key<Integer>("android.control.afTrigger", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Boolean> CONTROL_AWB_LOCK = new Key<Boolean>("android.control.awbLock", (Class<Boolean>) (Class<?>) boolean.class);
	public static final Key<Integer> CONTROL_AWB_MODE = new Key<Integer>("android.control.awbMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<android.hardware.camera2.params.MeteringRectangle[]> CONTROL_AWB_REGIONS = new Key<android.hardware.camera2.params.MeteringRectangle[]>("android.control.awbRegions", android.hardware.camera2.params.MeteringRectangle[].class);
	public static final Key<Integer> CONTROL_CAPTURE_INTENT = new Key<Integer>("android.control.captureIntent", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> CONTROL_EFFECT_MODE = new Key<Integer>("android.control.effectMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> CONTROL_MODE = new Key<Integer>("android.control.mode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> CONTROL_SCENE_MODE = new Key<Integer>("android.control.sceneMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> CONTROL_VIDEO_STABILIZATION_MODE = new Key<Integer>("android.control.videoStabilizationMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> CONTROL_POST_RAW_SENSITIVITY_BOOST = new Key<Integer>("android.control.postRawSensitivityBoost", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Boolean> CONTROL_ENABLE_ZSL = new Key<Boolean>("android.control.enableZsl", (Class<Boolean>) (Class<?>) boolean.class);
	public static final Key<Integer> CONTROL_EXTENDED_SCENE_MODE = new Key<Integer>("android.control.extendedSceneMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Float> CONTROL_ZOOM_RATIO = new Key<Float>("android.control.zoomRatio", (Class<Float>) (Class<?>) float.class);
	public static final Key<Integer> CONTROL_SETTINGS_OVERRIDE = new Key<Integer>("android.control.settingsOverride", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> CONTROL_AUTOFRAMING = new Key<Integer>("android.control.autoframing", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> EDGE_MODE = new Key<Integer>("android.edge.mode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> FLASH_MODE = new Key<Integer>("android.flash.mode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> FLASH_STRENGTH_LEVEL = new Key<Integer>("android.flash.strengthLevel", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> HOT_PIXEL_MODE = new Key<Integer>("android.hotPixel.mode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<android.location.Location> JPEG_GPS_LOCATION = new Key<android.location.Location>("android.jpeg.gpsLocation", android.location.Location.class);
	public static final Key<Integer> JPEG_ORIENTATION = new Key<Integer>("android.jpeg.orientation", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Byte> JPEG_QUALITY = new Key<Byte>("android.jpeg.quality", (Class<Byte>) (Class<?>) byte.class);
	public static final Key<Byte> JPEG_THUMBNAIL_QUALITY = new Key<Byte>("android.jpeg.thumbnailQuality", (Class<Byte>) (Class<?>) byte.class);
	public static final Key<android.util.Size> JPEG_THUMBNAIL_SIZE = new Key<android.util.Size>("android.jpeg.thumbnailSize", android.util.Size.class);
	public static final Key<Float> LENS_APERTURE = new Key<Float>("android.lens.aperture", (Class<Float>) (Class<?>) float.class);
	public static final Key<Float> LENS_FILTER_DENSITY = new Key<Float>("android.lens.filterDensity", (Class<Float>) (Class<?>) float.class);
	public static final Key<Float> LENS_FOCAL_LENGTH = new Key<Float>("android.lens.focalLength", (Class<Float>) (Class<?>) float.class);
	public static final Key<Float> LENS_FOCUS_DISTANCE = new Key<Float>("android.lens.focusDistance", (Class<Float>) (Class<?>) float.class);
	public static final Key<Integer> LENS_OPTICAL_STABILIZATION_MODE = new Key<Integer>("android.lens.opticalStabilizationMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> NOISE_REDUCTION_MODE = new Key<Integer>("android.noiseReduction.mode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<android.graphics.Rect> SCALER_CROP_REGION = new Key<android.graphics.Rect>("android.scaler.cropRegion", android.graphics.Rect.class);
	public static final Key<Integer> SCALER_ROTATE_AND_CROP = new Key<Integer>("android.scaler.rotateAndCrop", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Long> SENSOR_EXPOSURE_TIME = new Key<Long>("android.sensor.exposureTime", (Class<Long>) (Class<?>) long.class);
	public static final Key<Long> SENSOR_FRAME_DURATION = new Key<Long>("android.sensor.frameDuration", (Class<Long>) (Class<?>) long.class);
	public static final Key<Integer> SENSOR_SENSITIVITY = new Key<Integer>("android.sensor.sensitivity", (Class<Integer>) (Class<?>) int.class);
	public static final Key<int[]> SENSOR_TEST_PATTERN_DATA = new Key<int[]>("android.sensor.testPatternData", int[].class);
	public static final Key<Integer> SENSOR_TEST_PATTERN_MODE = new Key<Integer>("android.sensor.testPatternMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> SENSOR_PIXEL_MODE = new Key<Integer>("android.sensor.pixelMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> SHADING_MODE = new Key<Integer>("android.shading.mode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> STATISTICS_FACE_DETECT_MODE = new Key<Integer>("android.statistics.faceDetectMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Boolean> STATISTICS_HOT_PIXEL_MAP_MODE = new Key<Boolean>("android.statistics.hotPixelMapMode", (Class<Boolean>) (Class<?>) boolean.class);
	public static final Key<Integer> STATISTICS_LENS_SHADING_MAP_MODE = new Key<Integer>("android.statistics.lensShadingMapMode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> STATISTICS_OIS_DATA_MODE = new Key<Integer>("android.statistics.oisDataMode", (Class<Integer>) (Class<?>) int.class);
	/* value type TonemapCurve does not exist in ATL yet */
	public static final Key<Object> TONEMAP_CURVE = new Key<Object>("android.tonemap.curve", Object.class);
	public static final Key<Integer> TONEMAP_MODE = new Key<Integer>("android.tonemap.mode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Float> TONEMAP_GAMMA = new Key<Float>("android.tonemap.gamma", (Class<Float>) (Class<?>) float.class);
	public static final Key<Integer> TONEMAP_PRESET_CURVE = new Key<Integer>("android.tonemap.presetCurve", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Boolean> BLACK_LEVEL_LOCK = new Key<Boolean>("android.blackLevel.lock", (Class<Boolean>) (Class<?>) boolean.class);
	public static final Key<Float> REPROCESS_EFFECTIVE_EXPOSURE_FACTOR = new Key<Float>("android.reprocess.effectiveExposureFactor", (Class<Float>) (Class<?>) float.class);
	public static final Key<Integer> DISTORTION_CORRECTION_MODE = new Key<Integer>("android.distortionCorrection.mode", (Class<Integer>) (Class<?>) int.class);
	public static final Key<Integer> EXTENSION_STRENGTH = new Key<Integer>("android.extension.strength", (Class<Integer>) (Class<?>) int.class);
}
