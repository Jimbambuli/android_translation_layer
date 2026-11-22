package android.hardware.camera2;

import android.view.Surface;
import android.hardware.camera2.impl.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/* AOSP core/java/android/hardware/camera2/CaptureRequest.java. CameraDevice.createCaptureRequest() hands out a
 * Builder over the template of ATL's camera HAL; the request keeps the settings of the logical and the physical
 * cameras in maps of the public keys where AOSP has CameraMetadataNative, and parcels them with
 * impl.MetadataParcel. The public keys keep AOSP's names and value types (types still missing in
 * ATL are typed Object); Key follows AOSP Key/CameraMetadataNative.Key. */
@SuppressWarnings("unchecked")
public final class CaptureRequest extends CameraMetadata<CaptureRequest.Key<?>> implements android.os.Parcelable {

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

	/* AOSP: the settings of the logical camera and of each requested physical camera (AOSP CameraMetadataNative,
	 * here maps of the public keys) */
	private String mLogicalCameraId;
	private Map<Key<?>, Object> mSettings;
	private final java.util.LinkedHashMap<String, Map<Key<?>, Object>> mPhysicalCameraSettings = new java.util.LinkedHashMap<String, Map<Key<?>, Object>>();
	private final HashSet<Surface> mSurfaceSet = new HashSet<Surface>();
	private boolean mIsReprocess;
	private int mReprocessableSessionId;
	private Object mUserTag;
	private boolean mReleaseSurfaces = false;

	private CaptureRequest() {
		mIsReprocess = false;
		mReprocessableSessionId = CameraCaptureSession.SESSION_ID_NONE;
	}

	private CaptureRequest(CaptureRequest source) {
		mLogicalCameraId = source.mLogicalCameraId;
		for (Map.Entry<String, Map<Key<?>, Object>> entry : source.mPhysicalCameraSettings.entrySet())
			mPhysicalCameraSettings.put(entry.getKey(), new HashMap<Key<?>, Object>(entry.getValue()));
		mSettings = mPhysicalCameraSettings.get(mLogicalCameraId);
		mSurfaceSet.addAll(source.mSurfaceSet);
		mIsReprocess = source.mIsReprocess;
		mReprocessableSessionId = source.mReprocessableSessionId;
		mUserTag = source.mUserTag;
	}

	/* AOSP CaptureRequest(CameraMetadataNative settings, isReprocess, reprocessableSessionId, logicalCameraId,
	 * physicalCameraIdSet) */
	private CaptureRequest(Map<Key<?>, Object> settings, boolean isReprocess, int reprocessableSessionId, String logicalCameraId, java.util.Set<String> physicalCameraIdSet) {
		if ((physicalCameraIdSet != null) && isReprocess) {
			throw new IllegalArgumentException("Create a reprocess capture request with with more than one physical camera is not supported!");
		}
		mLogicalCameraId = logicalCameraId;
		mSettings = new HashMap<Key<?>, Object>(settings);
		mPhysicalCameraSettings.put(mLogicalCameraId, mSettings);
		if (physicalCameraIdSet != null) {
			for (String physicalId : physicalCameraIdSet) {
				mPhysicalCameraSettings.put(physicalId, new HashMap<Key<?>, Object>(mSettings));
			}
		}
		mIsReprocess = isReprocess;
		if (isReprocess) {
			if (reprocessableSessionId == CameraCaptureSession.SESSION_ID_NONE) {
				throw new IllegalArgumentException("Create a reprocess capture request with an invalid session ID: " + reprocessableSessionId);
			}
			mReprocessableSessionId = reprocessableSessionId;
		} else {
			mReprocessableSessionId = CameraCaptureSession.SESSION_ID_NONE;
		}
	}

	@Override
	public List<Key<?>> getKeys() {
		return super.getKeys();
	}

	public <T> T get(Key<T> key) {
		return (T) mSettings.get(key);
	}

	/** @hide AOSP getProtected(): the value of a key */
	@Override
	protected <T> T getProtected(Key<?> key) {
		return (T) mSettings.get(key);
	}

	/** @hide */
	@Override
	protected Class<Key<?>> getKeyClass() {
		Object thisClass = Key.class;
		return (Class<Key<?>>)thisClass;
	}

	public Object getTag() {
		return mUserTag;
	}

	/** @hide AOSP getTargets(): the Surfaces the request sends its frames to */
	public java.util.Collection<Surface> getTargets() {
		return Collections.unmodifiableCollection(mSurfaceSet);
	}

	/** @hide the settings of the physical cameras the request was built for (AOSP getPhysicalCameraSettings()) */
	public Map<String, Map<Key<?>, Object>> getPhysicalCameraSettings() {
		return Collections.unmodifiableMap(mPhysicalCameraSettings);
	}

	/** @hide the public keys with these names (AOSP CameraCharacteristics.getAvailableKeyList() reflects over the
	 * key fields the same way) */
	public static List<Key<?>> keysNamed(String[] names) {
		java.util.Set<String> wanted = new HashSet<String>(java.util.Arrays.asList(names));
		List<Key<?>> keys = new ArrayList<Key<?>>();
		for (java.lang.reflect.Field field : CaptureRequest.class.getDeclaredFields()) {
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

	public boolean isReprocess() {
		return mIsReprocess;
	}

	/** @hide */
	public int getReprocessableSessionId() {
		return mReprocessableSessionId;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CaptureRequest && equals((CaptureRequest)other);
	}

	private boolean equals(CaptureRequest other) {
		return other != null && java.util.Objects.equals(mUserTag, other.mUserTag) && mSurfaceSet.equals(other.mSurfaceSet) && mPhysicalCameraSettings.equals(other.mPhysicalCameraSettings) && mLogicalCameraId.equals(other.mLogicalCameraId) && mSettings.equals(other.mSettings) && mIsReprocess == other.mIsReprocess && mReprocessableSessionId == other.mReprocessableSessionId;
	}

	@Override
	public int hashCode() {
		return android.hardware.camera2.utils.HashCodeHelpers.hashCodeGeneric(mPhysicalCameraSettings, mSurfaceSet, mUserTag);
	}

	/* --- Parcelable: AOSP's layout, with the settings written by impl.MetadataParcel in place of the native
	 * metadata --- */
	private static final android.hardware.camera2.impl.MetadataParcel.KeyNames<Key<?>> KEY_NAMES = new android.hardware.camera2.impl.MetadataParcel.KeyNames<Key<?>>() {
		@Override
		public String nameOf(Key<?> key) {
			return key.getName();
		}

		@Override
		public Key<?> keyNamed(String name) {
			List<Key<?>> keys = keysNamed(new String[] {name});
			return keys.isEmpty() ? null : keys.get(0);
		}
	};

	public static final android.os.Parcelable.Creator<CaptureRequest> CREATOR = new android.os.Parcelable.Creator<CaptureRequest>() {
		@Override
		public CaptureRequest createFromParcel(android.os.Parcel in) {
			CaptureRequest request = new CaptureRequest();
			request.readFromParcel(in);
			return request;
		}

		@Override
		public CaptureRequest[] newArray(int size) {
			return new CaptureRequest[size];
		}
	};

	private void readFromParcel(android.os.Parcel in) {
		int physicalCameraCount = in.readInt();
		if (physicalCameraCount <= 0) {
			throw new RuntimeException("Physical camera count" + physicalCameraCount + " should always be positive");
		}
		mLogicalCameraId = in.readString();
		mSettings = new HashMap<Key<?>, Object>();
		android.hardware.camera2.impl.MetadataParcel.read(in, mSettings, KEY_NAMES);
		mPhysicalCameraSettings.put(mLogicalCameraId, mSettings);
		for (int i = 1; i < physicalCameraCount; i++) {
			String physicalId = in.readString();
			Map<Key<?>, Object> physicalCameraSettings = new HashMap<Key<?>, Object>();
			android.hardware.camera2.impl.MetadataParcel.read(in, physicalCameraSettings, KEY_NAMES);
			mPhysicalCameraSettings.put(physicalId, physicalCameraSettings);
		}
		mIsReprocess = (in.readInt() == 0) ? false : true;
		mReprocessableSessionId = CameraCaptureSession.SESSION_ID_NONE;
		mSurfaceSet.clear();
		Surface[] parcelableArray = in.readParcelableArray(Surface.class.getClassLoader(), Surface.class);
		if (parcelableArray != null) {
			for (Surface p : parcelableArray) {
				mSurfaceSet.add(p);
			}
		}
		/* AOSP: the stream/surface index arrays of a converted request (count 0 here) */
		int streamSurfaceSize = in.readInt();
		for (int i = 0; i < streamSurfaceSize * 2; i++)
			in.readInt();
		boolean hasUserTagStr = (in.readInt() == 1) ? true : false;
		if (hasUserTagStr) {
			mUserTag = in.readString();
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int flags) {
		if (!mPhysicalCameraSettings.containsKey(mLogicalCameraId)) {
			throw new IllegalStateException("Physical camera settings map must contain a key for the logical camera id.");
		}
		int physicalCameraCount = mPhysicalCameraSettings.size();
		dest.writeInt(physicalCameraCount);
		dest.writeString(mLogicalCameraId);
		android.hardware.camera2.impl.MetadataParcel.write(dest, mSettings, KEY_NAMES);
		for (Map.Entry<String, Map<Key<?>, Object>> entry : mPhysicalCameraSettings.entrySet()) {
			if (entry.getKey().equals(mLogicalCameraId)) {
				continue;
			}
			dest.writeString(entry.getKey());
			android.hardware.camera2.impl.MetadataParcel.write(dest, entry.getValue(), KEY_NAMES);
		}
		dest.writeInt(mIsReprocess ? 1 : 0);
		dest.writeParcelableArray(mSurfaceSet.toArray(new Surface[mSurfaceSet.size()]), flags);
		dest.writeInt(0); /* no stream/surface index arrays */
		/* AOSP: a String tag only */
		if (mUserTag != null && mUserTag instanceof String) {
			dest.writeInt(1);
			dest.writeString((String)mUserTag);
		} else {
			dest.writeInt(0);
		}
	}

	@SuppressWarnings("Finalize")
	@Override
	protected void finalize() {
		if (mReleaseSurfaces) {
			for (Surface s : mSurfaceSet) {
				s.release();
			}
		}
	}

	/* AOSP CaptureRequest.Builder */
	public static final class Builder {
		private final CaptureRequest mRequest;

		/** @hide AOSP Builder(CameraMetadataNative template, reprocess, reprocessableSessionId, logicalCameraId,
		 * physicalCameraIdSet) */
		public Builder(Map<Key<?>, Object> template, boolean reprocess, int reprocessableSessionId, String logicalCameraId, java.util.Set<String> physicalCameraIdSet) {
			mRequest = new CaptureRequest(template, reprocess, reprocessableSessionId, logicalCameraId, physicalCameraIdSet);
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

		public <T> Builder setPhysicalCameraKey(Key<T> key, T value, String physicalCameraId) {
			if (!mRequest.mPhysicalCameraSettings.containsKey(physicalCameraId)) {
				throw new IllegalArgumentException("Physical camera id: " + physicalCameraId + " is not valid!");
			}
			Map<Key<?>, Object> settings = mRequest.mPhysicalCameraSettings.get(physicalCameraId);
			if (value == null)
				settings.remove(key);
			else
				settings.put(key, value);
			return this;
		}

		public <T> T getPhysicalCameraKey(Key<T> key, String physicalCameraId) {
			if (!mRequest.mPhysicalCameraSettings.containsKey(physicalCameraId)) {
				throw new IllegalArgumentException("Physical camera id: " + physicalCameraId + " is not valid!");
			}
			return (T) mRequest.mPhysicalCameraSettings.get(physicalCameraId).get(key);
		}

		public void setTag(Object tag) {
			mRequest.mUserTag = tag;
		}

		public CaptureRequest build() {
			return new CaptureRequest(mRequest);
		}

		/** @hide */
		public boolean isEmpty() {
			return mRequest.mSettings.isEmpty();
		}
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
	@PublicKey public static final Key<Integer> CONTROL_AF_MODE = new Key<Integer>("android.control.afMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.hardware.camera2.params.MeteringRectangle[]> CONTROL_AF_REGIONS = new Key<android.hardware.camera2.params.MeteringRectangle[]>("android.control.afRegions", android.hardware.camera2.params.MeteringRectangle[].class);
	@PublicKey public static final Key<Integer> CONTROL_AF_TRIGGER = new Key<Integer>("android.control.afTrigger", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Boolean> CONTROL_AWB_LOCK = new Key<Boolean>("android.control.awbLock", (Class<Boolean>) (Class<?>) boolean.class);
	@PublicKey public static final Key<Integer> CONTROL_AWB_MODE = new Key<Integer>("android.control.awbMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.hardware.camera2.params.MeteringRectangle[]> CONTROL_AWB_REGIONS = new Key<android.hardware.camera2.params.MeteringRectangle[]>("android.control.awbRegions", android.hardware.camera2.params.MeteringRectangle[].class);
	@PublicKey public static final Key<Integer> CONTROL_CAPTURE_INTENT = new Key<Integer>("android.control.captureIntent", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_EFFECT_MODE = new Key<Integer>("android.control.effectMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_MODE = new Key<Integer>("android.control.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_SCENE_MODE = new Key<Integer>("android.control.sceneMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_VIDEO_STABILIZATION_MODE = new Key<Integer>("android.control.videoStabilizationMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_POST_RAW_SENSITIVITY_BOOST = new Key<Integer>("android.control.postRawSensitivityBoost", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Boolean> CONTROL_ENABLE_ZSL = new Key<Boolean>("android.control.enableZsl", (Class<Boolean>) (Class<?>) boolean.class);
	@PublicKey public static final Key<Integer> CONTROL_EXTENDED_SCENE_MODE = new Key<Integer>("android.control.extendedSceneMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Float> CONTROL_ZOOM_RATIO = new Key<Float>("android.control.zoomRatio", (Class<Float>) (Class<?>) float.class);
	@PublicKey public static final Key<Integer> CONTROL_SETTINGS_OVERRIDE = new Key<Integer>("android.control.settingsOverride", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_AUTOFRAMING = new Key<Integer>("android.control.autoframing", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> EDGE_MODE = new Key<Integer>("android.edge.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> FLASH_MODE = new Key<Integer>("android.flash.mode", (Class<Integer>) (Class<?>) int.class);
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
	@PublicKey public static final Key<Integer> LENS_OPTICAL_STABILIZATION_MODE = new Key<Integer>("android.lens.opticalStabilizationMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> NOISE_REDUCTION_MODE = new Key<Integer>("android.noiseReduction.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.graphics.Rect> SCALER_CROP_REGION = new Key<android.graphics.Rect>("android.scaler.cropRegion", android.graphics.Rect.class);
	@PublicKey public static final Key<Integer> SCALER_ROTATE_AND_CROP = new Key<Integer>("android.scaler.rotateAndCrop", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Long> SENSOR_EXPOSURE_TIME = new Key<Long>("android.sensor.exposureTime", (Class<Long>) (Class<?>) long.class);
	@PublicKey public static final Key<Long> SENSOR_FRAME_DURATION = new Key<Long>("android.sensor.frameDuration", (Class<Long>) (Class<?>) long.class);
	@PublicKey public static final Key<Integer> SENSOR_SENSITIVITY = new Key<Integer>("android.sensor.sensitivity", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<int[]> SENSOR_TEST_PATTERN_DATA = new Key<int[]>("android.sensor.testPatternData", int[].class);
	@PublicKey public static final Key<Integer> SENSOR_TEST_PATTERN_MODE = new Key<Integer>("android.sensor.testPatternMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> SENSOR_PIXEL_MODE = new Key<Integer>("android.sensor.pixelMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> SHADING_MODE = new Key<Integer>("android.shading.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> STATISTICS_FACE_DETECT_MODE = new Key<Integer>("android.statistics.faceDetectMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Boolean> STATISTICS_HOT_PIXEL_MAP_MODE = new Key<Boolean>("android.statistics.hotPixelMapMode", (Class<Boolean>) (Class<?>) boolean.class);
	@PublicKey public static final Key<Integer> STATISTICS_LENS_SHADING_MAP_MODE = new Key<Integer>("android.statistics.lensShadingMapMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> STATISTICS_OIS_DATA_MODE = new Key<Integer>("android.statistics.oisDataMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<android.hardware.camera2.params.TonemapCurve> TONEMAP_CURVE = new Key<android.hardware.camera2.params.TonemapCurve>("android.tonemap.curve", android.hardware.camera2.params.TonemapCurve.class);
	@PublicKey public static final Key<Integer> TONEMAP_MODE = new Key<Integer>("android.tonemap.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Float> TONEMAP_GAMMA = new Key<Float>("android.tonemap.gamma", (Class<Float>) (Class<?>) float.class);
	@PublicKey public static final Key<Integer> TONEMAP_PRESET_CURVE = new Key<Integer>("android.tonemap.presetCurve", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Boolean> BLACK_LEVEL_LOCK = new Key<Boolean>("android.blackLevel.lock", (Class<Boolean>) (Class<?>) boolean.class);
	@PublicKey public static final Key<Float> REPROCESS_EFFECTIVE_EXPOSURE_FACTOR = new Key<Float>("android.reprocess.effectiveExposureFactor", (Class<Float>) (Class<?>) float.class);
	@PublicKey public static final Key<Integer> DISTORTION_CORRECTION_MODE = new Key<Integer>("android.distortionCorrection.mode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> EXTENSION_STRENGTH = new Key<Integer>("android.extension.strength", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> COLOR_CORRECTION_COLOR_TEMPERATURE = new Key<Integer>("android.colorCorrection.colorTemperature", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> COLOR_CORRECTION_COLOR_TINT = new Key<Integer>("android.colorCorrection.colorTint", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_AE_PRIORITY_MODE = new Key<Integer>("android.control.aePriorityMode", (Class<Integer>) (Class<?>) int.class);
	@PublicKey public static final Key<Integer> CONTROL_ZOOM_METHOD = new Key<Integer>("android.control.zoomMethod", (Class<Integer>) (Class<?>) int.class);
}
