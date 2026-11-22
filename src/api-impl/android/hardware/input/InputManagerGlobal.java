package android.hardware.input;

import android.hardware.BatteryState;
import android.hardware.input.InputManager.InputDeviceListener;
import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.hardware.lights.LightsManager;
import android.hardware.lights.LightsRequest;
import android.os.CombinedVibration;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.SparseArray;
import android.view.InputDevice;
import com.android.server.input.InputReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* AOSP InputManagerGlobal (@hide, core/java/android/hardware/input/InputManagerGlobal.java): the process' view of
 * the input devices and its listeners. The input manager service runs in the app's process here: the devices are
 * ATL's own input device (touch screen, mouse and keyboard of GTK, InputDevice.getDevice()) and the game
 * controllers of com.android.server.input.InputReader, which tells about added and removed ones. */
public final class InputManagerGlobal {
	private static final InputManagerGlobal sInstance = new InputManagerGlobal();
	/* InputManagerService.DEFAULT_VIBRATION_MAGNITUDE */
	private static final int DEFAULT_VIBRATION_MAGNITUDE = 192;

	private final ArrayList<InputDeviceListenerDelegate> mInputDeviceListeners = new ArrayList<>();
	private int mNextVibratorToken = 1;

	private InputManagerGlobal() {}

	public static InputManagerGlobal getInstance() {
		return sInstance;
	}

	private static final class InputDeviceListenerDelegate extends Handler {
		public final InputDeviceListener mListener;
		static final int MSG_DEVICE_ADDED = 1;
		static final int MSG_DEVICE_REMOVED = 2;
		static final int MSG_DEVICE_CHANGED = 3;

		InputDeviceListenerDelegate(InputDeviceListener listener, Handler handler) {
			super(handler != null ? handler.getLooper() : Looper.myLooper());
			mListener = listener;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_DEVICE_ADDED:
					mListener.onInputDeviceAdded(msg.arg1);
					break;
				case MSG_DEVICE_REMOVED:
					mListener.onInputDeviceRemoved(msg.arg1);
					break;
				case MSG_DEVICE_CHANGED:
					mListener.onInputDeviceChanged(msg.arg1);
					break;
			}
		}
	}

	/* onInputDevicesChanged() for a device of InputReader */
	public static void onInputDeviceAdded(int deviceId) {
		sInstance.sendMessageToInputDeviceListeners(InputDeviceListenerDelegate.MSG_DEVICE_ADDED, deviceId);
	}

	public static void onInputDeviceRemoved(int deviceId) {
		sInstance.sendMessageToInputDeviceListeners(InputDeviceListenerDelegate.MSG_DEVICE_REMOVED, deviceId);
	}

	private void sendMessageToInputDeviceListeners(int what, int deviceId) {
		synchronized (mInputDeviceListeners) {
			for (InputDeviceListenerDelegate listener : mInputDeviceListeners)
				listener.sendMessage(listener.obtainMessage(what, deviceId, 0));
		}
	}

	public InputDevice getInputDevice(int id) {
		InputDevice device = InputReader.getInputDevice(id);
		return device != null ? device : InputDevice.getAtlDevice(id);
	}

	public int[] getInputDeviceIds() {
		int[] gameControllers = InputReader.getInputDeviceIds();
		int[] ids = new int[gameControllers.length + 1];
		ids[0] = InputDevice.getAtlDevice(0).getId();
		System.arraycopy(gameControllers, 0, ids, 1, gameControllers.length);
		return ids;
	}

	/* InputManagerService.hasKeys(): the keys that the key layout of a game controller maps from its scan codes;
	 * ATL's own device (GTK's keyboard) is taken to have every key */
	public boolean[] deviceHasKeys(int id, int[] keyCodes) {
		boolean[] keyExists = new boolean[keyCodes.length];
		if (InputReader.getInputDevice(id) != null)
			return InputReader.hasKeys(id, keyCodes);
		java.util.Arrays.fill(keyExists, true);
		return keyExists;
	}

	public void registerInputDeviceListener(InputDeviceListener listener, Handler handler) {
		Objects.requireNonNull(listener, "listener must not be null");
		synchronized (mInputDeviceListeners) {
			if (findInputDeviceListenerLocked(listener) < 0)
				mInputDeviceListeners.add(new InputDeviceListenerDelegate(listener, handler));
		}
	}

	public void unregisterInputDeviceListener(InputDeviceListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener must not be null");
		synchronized (mInputDeviceListeners) {
			int index = findInputDeviceListenerLocked(listener);
			if (index >= 0) {
				mInputDeviceListeners.get(index).removeCallbacksAndMessages(null);
				mInputDeviceListeners.remove(index);
			}
		}
	}

	private int findInputDeviceListenerLocked(InputDeviceListener listener) {
		for (int i = 0; i < mInputDeviceListeners.size(); i++)
			if (mInputDeviceListeners.get(i).mListener == listener)
				return i;
		return -1;
	}

	/* the lights of an input device: AOSP's PeripheralController finds them in the sysfs of the associated device
	 * (/sys/class/leds: player LEDs, RGB lights); ATL does not read them, so a device has no lights (like an input
	 * device without any on Android) and a request has nothing to set */
	public LightsManager getInputDeviceLightsManager(int deviceId) {
		return new InputDeviceLightsManager(deviceId);
	}

	List<Light> getLights(int deviceId) {
		return new ArrayList<>();
	}

	/* InputManagerService.getLightState(): color and player id of the light (0 for a light the device has not) */
	LightState getLightState(int deviceId, Light light) {
		return new LightState(0, 0);
	}

	/* InputManagerService.setLightStates() of the session's request */
	void requestLights(int deviceId, LightsRequest request) {
		Objects.requireNonNull(request, "request must not be null");
	}

	/* the battery of an input device: AOSP reads it from the power supply in the sysfs of the associated device;
	 * ATL does not (the game controllers have hasBattery() false), so the state is that of a device without one */
	public BatteryState getInputDeviceBatteryState(int deviceId, boolean hasBattery) {
		return new LocalBatteryState();
	}

	private static final class LocalBatteryState extends BatteryState {
		private final boolean mIsPresent;
		private final int mStatus;
		private final float mCapacity;

		LocalBatteryState() {
			this(false /*isPresent*/, BatteryState.STATUS_UNKNOWN, Float.NaN /*capacity*/);
		}

		LocalBatteryState(boolean isPresent, int status, float capacity) {
			mIsPresent = isPresent;
			mStatus = status;
			mCapacity = capacity;
		}

		@Override
		public boolean isPresent() {
			return mIsPresent;
		}

		@Override
		public int getStatus() {
			return mStatus;
		}

		@Override
		public float getCapacity() {
			return mCapacity;
		}
	}

	public Vibrator getInputDeviceVibrator(int deviceId, int vibratorId) {
		return new InputDeviceVibrator(deviceId, vibratorId);
	}

	public VibratorManager getInputDeviceVibratorManager(int deviceId) {
		return new InputDeviceVibratorManager(deviceId);
	}

	int[] getVibratorIds(int deviceId) {
		return InputReader.getVibratorIds(deviceId);
	}

	boolean isVibrating(int deviceId) {
		return InputReader.isVibrating(deviceId);
	}

	int newVibratorToken() {
		return mNextVibratorToken++;
	}

	/* InputManagerService.vibrate() with its VibrationInfo: the steps of the effect, DEFAULT_AMPLITUDE as 192, on
	 * both channels */
	void vibrate(int deviceId, VibrationEffect effect, int token) {
		long[] pattern = effect.getAtlTimings();
		int[] amplitudes = effect.getAtlAmplitudes(DEFAULT_VIBRATION_MAGNITUDE);
		SparseArray<int[]> channels = new SparseArray<>();
		for (int vibratorId : getVibratorIds(deviceId))
			channels.put(vibratorId, amplitudes);
		InputReader.vibrate(deviceId, pattern, channels, effect.getAtlRepeatIndex(), token);
	}

	/* InputManagerService.vibrateCombined(): Mono like vibrate(), Stereo with the effect of each vibrator (the
	 * pattern and repeat of the first) */
	void vibrate(int deviceId, CombinedVibration effect, int token) {
		if (effect instanceof CombinedVibration.Mono) {
			vibrate(deviceId, ((CombinedVibration.Mono)effect).getEffect(), token);
		} else if (effect instanceof CombinedVibration.Stereo) {
			SparseArray<VibrationEffect> effects = ((CombinedVibration.Stereo)effect).getEffects();
			long[] pattern = new long[0];
			int repeat = Integer.MIN_VALUE;
			SparseArray<int[]> amplitudes = new SparseArray<>(effects.size());
			for (int i = 0; i < effects.size(); i++) {
				VibrationEffect channelEffect = effects.valueAt(i);
				if (pattern.length == 0)
					pattern = channelEffect.getAtlTimings();
				if (repeat == Integer.MIN_VALUE)
					repeat = channelEffect.getAtlRepeatIndex();
				amplitudes.put(effects.keyAt(i), channelEffect.getAtlAmplitudes(DEFAULT_VIBRATION_MAGNITUDE));
			}
			InputReader.vibrate(deviceId, pattern, amplitudes, repeat, token);
		} else {
			android.util.Log.e("InputManagerGlobal", "Only Mono and Stereo effects are supported");
		}
	}

	void cancelVibrate(int deviceId, int token) {
		InputReader.cancelVibrate(deviceId, token);
	}
}
