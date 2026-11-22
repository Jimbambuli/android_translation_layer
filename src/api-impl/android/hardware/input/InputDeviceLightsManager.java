package android.hardware.input;

import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.hardware.lights.LightsManager;
import android.hardware.lights.LightsRequest;
import com.android.internal.util.Preconditions;
import java.util.List;

/* AOSP InputDeviceLightsManager (core/java/android/hardware/input/InputDeviceLightsManager.java): the lights of an
 * input device, through InputManagerGlobal (InputManagerService); openSession(int) is not supported, as in AOSP */
class InputDeviceLightsManager extends LightsManager {
	private final InputManagerGlobal mGlobal;
	private final int mDeviceId;

	InputDeviceLightsManager(int deviceId) {
		mGlobal = InputManagerGlobal.getInstance();
		mDeviceId = deviceId;
	}

	@Override
	public List<Light> getLights() {
		return mGlobal.getLights(mDeviceId);
	}

	@Override
	public LightState getLightState(Light light) {
		Preconditions.checkNotNull(light);
		return mGlobal.getLightState(mDeviceId, light);
	}

	@Override
	public LightsSession openSession() {
		return new InputDeviceLightsSession();
	}

	@Override
	public LightsSession openSession(int priority) {
		throw new UnsupportedOperationException();
	}

	public final class InputDeviceLightsSession extends LightsManager.LightsSession implements AutoCloseable {
		private boolean mClosed = false;

		private InputDeviceLightsSession() {
		}

		@Override
		public void requestLights(LightsRequest request) {
			Preconditions.checkNotNull(request);
			Preconditions.checkArgument(!mClosed);
			mGlobal.requestLights(mDeviceId, request);
		}

		@Override
		public void close() {
			mClosed = true;
		}
	}
}
