package android.hardware.input;

import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import android.view.Display;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.VerifiedInputEvent;

/* AOSP InputManager: the public face of InputManagerGlobal (the input devices: ATL's own device and the game
 * controllers of com.android.server.input.InputReader) */
public final class InputManager {
	private final InputManagerGlobal mGlobal = InputManagerGlobal.getInstance();
	private final Context mContext;
	/* mIsStylusPointerIconEnabled: read once, as AOSP */
	private Boolean mIsStylusPointerIconEnabled = null;

	/** @hide */
	public InputManager(Context context) {
		mContext = context;
	}

	public static final String ACTION_QUERY_KEYBOARD_LAYOUTS = "android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS";
	public static final String META_DATA_KEYBOARD_LAYOUTS = "android.hardware.input.metadata.KEYBOARD_LAYOUTS";

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

	/* AOSP getMaximumObscuringOpacityForTouch() -> InputSettings: Settings.Global
	 * maximum_obscuring_opacity_for_touch, DEFAULT_MAXIMUM_OBSCURING_OPACITY_FOR_TOUCH (.8f) when unset */
	public float getMaximumObscuringOpacityForTouch() {
		return Settings.Global.getFloat(mContext.getContentResolver(), "maximum_obscuring_opacity_for_touch", .8f);
	}

	/* AOSP isStylusPointerIconEnabled() -> InputSettings.isStylusPointerIconEnabled(): no sysprop override here
	 * (persist.input.force_enable_stylus_pointer_icon), then config_enableStylusPointerIcon and
	 * Settings.Secure stylus_pointer_icon_enabled (DEFAULT_STYLUS_POINTER_ICON_ENABLED = 1) */
	public boolean isStylusPointerIconEnabled() {
		if (mIsStylusPointerIconEnabled == null) {
			mIsStylusPointerIconEnabled = mContext.getResources().getBoolean(com.android.internal.R.bool.config_enableStylusPointerIcon)
			    && Settings.Secure.getInt(mContext.getContentResolver(), "stylus_pointer_icon_enabled", 1) != 0;
		}
		return mIsStylusPointerIconEnabled;
	}

	/* AOSP verifyInputEvent() -> InputDispatcher::verifyInputEvent(): the event is verified when its HMAC is the
	 * one the dispatcher signed it with. ATL dispatches its events without signing them (MotionEvent and KeyEvent
	 * carry no HMAC, the parcel writes none), so no event verifies, as on Android for an event the system did not
	 * dispatch: null */
	public VerifiedInputEvent verifyInputEvent(InputEvent event) {
		return null;
	}

	/* AOSP getHostUsiVersion(Display): InputManagerGlobal */
	public HostUsiVersion getHostUsiVersion(Display display) {
		return mGlobal.getHostUsiVersion(display);
	}

	public InputDevice.ViewBehavior getInputDeviceViewBehavior(int deviceId) {
		InputDevice device = getInputDevice(deviceId);
		return device == null ? null : device.getViewBehavior();
	}
}
