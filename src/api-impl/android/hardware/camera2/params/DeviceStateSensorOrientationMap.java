/* AOSP DeviceStateSensorOrientationMap.java (Android 16). ATL's CameraManager lists no camera, so no
 * CameraCharacteristics.INFO_DEVICE_STATE_SENSOR_ORIENTATION_MAP value ever arises; the type keeps the pairs
 * apps build themselves. Difference: the @DeviceState annotation type is omitted. */
package android.hardware.camera2.params;

import android.hardware.camera2.utils.HashCodeHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public final class DeviceStateSensorOrientationMap {
	public static final long NORMAL = 0;
	public static final long FOLDED = 1 << 2;

	private final HashMap<Long, Integer> mDeviceStateOrientationMap;

	public DeviceStateSensorOrientationMap(final long[] elements) {
		mElements = Objects.requireNonNull(elements, "elements must not be null");
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

	private DeviceStateSensorOrientationMap(final ArrayList<Long> elements, final HashMap<Long, Integer> deviceStateOrientationMap) {
		mElements = new long[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			mElements[i] = elements.get(i);
		}
		mDeviceStateOrientationMap = deviceStateOrientationMap;
	}

	public int getSensorOrientation(long deviceState) {
		if (!mDeviceStateOrientationMap.containsKey(deviceState)) {
			throw new IllegalArgumentException("Invalid device state: " + deviceState);
		}

		return mDeviceStateOrientationMap.get(deviceState);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj instanceof DeviceStateSensorOrientationMap) {
			final DeviceStateSensorOrientationMap other = (DeviceStateSensorOrientationMap) obj;
			return Arrays.equals(mElements, other.mElements);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return HashCodeHelpers.hashCodeGeneric(mElements);
	}

	private final long[] mElements;

	public static final class Builder {
		public Builder() {
			// Empty
		}

		public Builder addOrientationForState(long deviceState, long angle) {
			if (angle % 90 != 0) {
				throw new IllegalArgumentException("Sensor orientation not divisible by 90: " + angle);
			}
			mDeviceStateOrientationMap.put(deviceState, Math.toIntExact(angle));
			mElements.add(deviceState);
			mElements.add(angle);
			return this;
		}

		public DeviceStateSensorOrientationMap build() {
			if (mElements.size() == 0) {
				throw new IllegalStateException("Cannot build a DeviceStateSensorOrientationMap with zero elements.");
			}
			return new DeviceStateSensorOrientationMap(mElements, mDeviceStateOrientationMap);
		}

		private final ArrayList<Long> mElements = new ArrayList<>();
		private final HashMap<Long, Integer> mDeviceStateOrientationMap = new HashMap<>();
	}
}
