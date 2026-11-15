package android.hardware.camera2.params;

import java.util.HashMap;
import java.util.Objects;

/* AOSP DeviceStateSensorOrientationMap. ATL's CameraManager lists no camera, so no
 * CameraCharacteristics.INFO_DEVICE_STATE_SENSOR_ORIENTATION_MAP value ever arises; the type keeps the pairs
 * apps may build themselves. */
public final class DeviceStateSensorOrientationMap {
	public static final long NORMAL = 0;
	public static final long FOLDED = 1 << 2;

	private final HashMap<Long, Integer> mDeviceStateOrientationMap;

	/* AOSP constructor: elements are (deviceState, orientation) pairs, orientation a multiple of 90 */
	public DeviceStateSensorOrientationMap(final long[] elements) {
		Objects.requireNonNull(elements, "elements must not be null");
		mDeviceStateOrientationMap = new HashMap<>();
		if ((elements.length % 2) != 0) {
			throw new IllegalArgumentException("Device state sensor orientation map length " + elements.length + " is not even!");
		}
		for (int i = 0; i < elements.length; i += 2) {
			if ((elements[i + 1] % 90) != 0) {
				throw new IllegalArgumentException("Sensor orientation not divisible by 90: " + elements[i + 1]);
			}
			mDeviceStateOrientationMap.put(elements[i], Math.toIntExact(elements[i + 1]));
		}
	}

	/* AOSP getSensorOrientation(): IllegalArgumentException for a device state not in the map */
	public int getSensorOrientation(long deviceState) {
		if (!mDeviceStateOrientationMap.containsKey(deviceState)) {
			throw new IllegalArgumentException("Invalid device state: " + deviceState);
		}
		return mDeviceStateOrientationMap.get(deviceState);
	}
}
