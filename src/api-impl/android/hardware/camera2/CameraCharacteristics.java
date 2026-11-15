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
}
