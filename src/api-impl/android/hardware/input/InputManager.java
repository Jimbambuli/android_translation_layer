package android.hardware.input;

import android.os.Handler;
import android.view.InputDevice;

/* AOSP InputManager: the public face of InputManagerGlobal (the input devices: ATL's own device and the game
 * controllers of com.android.server.input.InputReader) */
public final class InputManager {
	private final InputManagerGlobal mGlobal = InputManagerGlobal.getInstance();

	public static interface InputDeviceListener {
		abstract void onInputDeviceAdded(int deviceId);
		abstract void onInputDeviceChanged(int deviceId);
		abstract void onInputDeviceRemoved(int deviceId);
	}

	/* AOSP: a listener is called on the handler's looper, or on that of the registering thread */
	public void registerInputDeviceListener(InputManager.InputDeviceListener listener, Handler handler) {
		mGlobal.registerInputDeviceListener(listener, handler);
	}

	/* AOSP InputManager.unregisterInputDeviceListener(): a null listener is rejected */
	public void unregisterInputDeviceListener(InputDeviceListener listener) {
		mGlobal.unregisterInputDeviceListener(listener);
	}

	public int[] getInputDeviceIds() {
		return mGlobal.getInputDeviceIds();
	}

	public InputDevice getInputDevice(int id) {
		return mGlobal.getInputDevice(id);
	}
}
