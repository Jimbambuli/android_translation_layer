package com.android.server.input;

import android.hardware.input.InputManagerGlobal;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.TreeMap;

/* AOSP InputReader (frameworks/native/services/inputflinger/reader) for the game controllers of EventHub: per device
 * the mappers of InputDevice::createMappers() that a game controller has (VibratorInputMapper, KeyboardInputMapper,
 * JoystickInputMapper), their InputDeviceInfo as android.view.InputDevice, and the key and motion events they make,
 * which go to InputDispatcher. On the main thread. */
public final class InputReader {
	private static final String TAG = "InputReader";

	private static final class ReaderDevice {
		final EventHub.Device device;
		int sources;
		KeyboardInputMapper keyboard;
		JoystickInputMapper joystick;
		VibratorInputMapper vibrator;
		InputDevice info;
		boolean dropUntilNextSync;

		ReaderDevice(EventHub.Device device) {
			this.device = device;
		}
	}

	private static final SparseArray<ReaderDevice> sDevices = new SparseArray<>();
	private static int sGeneration = 1;
	private static Handler sHandler;

	private InputReader() {}

	private static Handler getHandler() {
		if (sHandler == null)
			sHandler = new Handler(Looper.getMainLooper());
		return sHandler;
	}

	/* InputReader::addDeviceLocked(), InputDevice::addEventHubDevice() and createMappers() */
	static void addDevice(EventHub.Device device) {
		ReaderDevice readerDevice = new ReaderDevice(device);
		if ((device.classes & EventHub.CLASS_VIBRATOR) != 0)
			readerDevice.vibrator = new VibratorInputMapper(readerDevice);
		int keyboardSource = 0;
		if ((device.classes & EventHub.CLASS_KEYBOARD) != 0)
			keyboardSource |= InputDevice.SOURCE_KEYBOARD;
		if ((device.classes & EventHub.CLASS_DPAD) != 0)
			keyboardSource |= InputDevice.SOURCE_DPAD;
		if ((device.classes & EventHub.CLASS_GAMEPAD) != 0)
			keyboardSource |= InputDevice.SOURCE_GAMEPAD;
		if (keyboardSource != 0)
			readerDevice.keyboard = new KeyboardInputMapper(readerDevice, keyboardSource);
		if ((device.classes & EventHub.CLASS_JOYSTICK) != 0)
			readerDevice.joystick = new JoystickInputMapper(readerDevice);
		readerDevice.sources = keyboardSource | (readerDevice.joystick != null ? InputDevice.SOURCE_JOYSTICK : 0);
		readerDevice.info = buildInputDevice(readerDevice);
		sDevices.put(device.id, readerDevice);
		InputManagerGlobal.onInputDeviceAdded(device.id);
	}

	/* InputReader::removeDeviceLocked(): the device is reset (the keys that are down are canceled) */
	static void removeDevice(EventHub.Device device) {
		ReaderDevice readerDevice = sDevices.get(device.id);
		if (readerDevice == null)
			return;
		reset(readerDevice, SystemClock.uptimeMillis());
		if (readerDevice.vibrator != null)
			readerDevice.vibrator.stopVibrating();
		sDevices.remove(device.id);
		InputDispatcher.notifyDeviceReset(device.id);
		InputManagerGlobal.onInputDeviceRemoved(device.id);
	}

	private static void reset(ReaderDevice readerDevice, long when) {
		if (readerDevice.keyboard != null)
			readerDevice.keyboard.cancelAllDownKeys(when);
		if (readerDevice.joystick != null)
			readerDevice.joystick.reset();
	}

	/* InputDevice::process(): after SYN_DROPPED the events up to the next SYN_REPORT are dropped and the device is
	 * reset */
	static void processEvents(EventHub.Device device, long[] when, int[] values) {
		ReaderDevice readerDevice = sDevices.get(device.id);
		if (readerDevice == null)
			return;
		for (int i = 0; i < when.length; i++) {
			int type = values[i * 3], code = values[i * 3 + 1], value = values[i * 3 + 2];
			long whenMillis = when[i] / 1000000;
			if (readerDevice.dropUntilNextSync) {
				if (type == EventHub.EV_SYN && code == EventHub.SYN_REPORT)
					readerDevice.dropUntilNextSync = false;
				continue;
			}
			if (type == EventHub.EV_SYN && code == EventHub.SYN_DROPPED) {
				readerDevice.dropUntilNextSync = true;
				reset(readerDevice, whenMillis);
				continue;
			}
			if (readerDevice.keyboard != null)
				readerDevice.keyboard.process(whenMillis, type, code, value);
			if (readerDevice.joystick != null)
				readerDevice.joystick.process(whenMillis, type, code, value);
		}
	}

	/* InputDevice::getDeviceInfo() with the mappers' populateDeviceInfo(), as the JNI of android.view.InputDevice
	 * turns it into a Java InputDevice */
	private static InputDevice buildInputDevice(ReaderDevice readerDevice) {
		EventHub.Device device = readerDevice.device;
		InputDevice.Builder builder = new InputDevice.Builder()
			.setId(device.id)
			.setGeneration(sGeneration++)
			.setControllerNumber(device.controllerNumber)
			.setName(device.identifier.name)
			.setVendorId(device.identifier.vendor)
			.setProductId(device.identifier.product)
			.setDeviceBus(device.identifier.bus)
			.setDescriptor(device.identifier.descriptor)
			.setExternal((device.classes & EventHub.CLASS_EXTERNAL) != 0)
			.setHasMicrophone((device.classes & EventHub.CLASS_MIC) != 0)
			.setSources(readerDevice.sources)
			.setKeyboardType(readerDevice.keyboard == null ? InputDevice.KEYBOARD_TYPE_NONE
					 : (device.classes & EventHub.CLASS_ALPHAKEY) != 0 ? InputDevice.KEYBOARD_TYPE_ALPHABETIC
					 : InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC)
			.setKeyCharacterMap(KeyCharacterMap.load(device.id))
			.setHasVibrator(readerDevice.vibrator != null)
			.setEnabled(true);
		if (readerDevice.joystick != null)
			readerDevice.joystick.populateDeviceInfo(builder);
		return builder.build();
	}

	/* InputManagerService.getInputDeviceIds(), getInputDevice() */
	public static int[] getInputDeviceIds() {
		int[] ids = new int[sDevices.size()];
		for (int i = 0; i < ids.length; i++)
			ids[i] = sDevices.keyAt(i);
		return ids;
	}

	public static InputDevice getInputDevice(int id) {
		ReaderDevice readerDevice = sDevices.get(id);
		return readerDevice != null ? readerDevice.info : null;
	}

	/* InputReader::hasKeys() with KeyboardInputMapper::markSupportedKeyCodes(): the device's key layout maps a
	 * scan code (or usage) that the device has to the key */
	public static boolean[] hasKeys(int deviceId, int[] keyCodes) {
		boolean[] keyExists = new boolean[keyCodes.length];
		ReaderDevice readerDevice = sDevices.get(deviceId);
		if (readerDevice == null || readerDevice.keyboard == null)
			return keyExists;
		for (int i = 0; i < keyCodes.length; i++)
			keyExists[i] = readerDevice.device.hasKeycode(keyCodes[i]);
		return keyExists;
	}

	/* InputReader::getVibratorIds(): the two channels of FF_RUMBLE (strong, weak) */
	public static int[] getVibratorIds(int deviceId) {
		ReaderDevice readerDevice = sDevices.get(deviceId);
		return readerDevice != null && readerDevice.vibrator != null ? new int[] {0, 1} : new int[0];
	}

	public static boolean isVibrating(int deviceId) {
		ReaderDevice readerDevice = sDevices.get(deviceId);
		return readerDevice != null && readerDevice.vibrator != null && readerDevice.vibrator.vibrating;
	}

	/* nativeVibrateCombined() of InputManagerService's JNI: a step pattern with an amplitude (0-255) per channel
	 * and step; nativeVibrate() gives both channels the same amplitudes */
	public static void vibrate(int deviceId, long[] pattern, SparseArray<int[]> amplitudes, int repeat, int token) {
		ReaderDevice readerDevice = sDevices.get(deviceId);
		if (readerDevice == null || readerDevice.vibrator == null)
			return;
		if (pattern.length > VibratorInputMapper.MAX_VIBRATE_PATTERN_SIZE) {
			android.util.Log.i(TAG, "Skipped requested vibration because the pattern size is " + pattern.length
					   + " which is more than the maximum supported size of " + VibratorInputMapper.MAX_VIBRATE_PATTERN_SIZE + ".");
			return;
		}
		readerDevice.vibrator.vibrate(pattern, amplitudes, repeat, token);
	}

	public static void cancelVibrate(int deviceId, int token) {
		ReaderDevice readerDevice = sDevices.get(deviceId);
		if (readerDevice != null && readerDevice.vibrator != null && readerDevice.vibrator.vibrating
		    && readerDevice.vibrator.token == token)
			readerDevice.vibrator.stopVibrating();
	}

	/* AOSP KeyboardInputMapper for the buttons of a game controller. The meta state stays 0: a game controller has
	 * no modifier keys, and the modifiers of the keyboard are GTK's. The device is not orientation aware. */
	private static final class KeyboardInputMapper {
		private final ReaderDevice mDevice;
		private final int mSource;
		/* the keys that are down: {keyCode, scanCode, flags} and their down times */
		private final ArrayList<int[]> mKeyDowns = new ArrayList<>();
		private final ArrayList<Long> mKeyDownTimes = new ArrayList<>();
		private int mCurrentHidUsage; /* HidUsageAccumulator */

		KeyboardInputMapper(ReaderDevice device, int source) {
			mDevice = device;
			mSource = source;
		}

		/* isSupportedScanCode(): keys of keyboards, gamepads and styluses */
		private static boolean isSupportedScanCode(int scanCode) {
			return scanCode < EventHub.BTN_MOUSE || (scanCode >= EventHub.BTN_JOYSTICK && scanCode < EventHub.BTN_DIGI)
				|| scanCode == EventHub.BTN_STYLUS || scanCode == EventHub.BTN_STYLUS2 || scanCode == EventHub.BTN_STYLUS3
				|| scanCode >= EventHub.BTN_WHEEL;
		}

		void process(long when, int type, int code, int value) {
			if (type == EventHub.EV_MSC && code == EventHub.MSC_SCAN) {
				mCurrentHidUsage = value;
			} else if (type == EventHub.EV_SYN && code == EventHub.SYN_REPORT) {
				mCurrentHidUsage = 0;
			} else if (type == EventHub.EV_KEY) {
				/* the kernel's repeats (2) are dropped, Android repeats keys itself */
				if (value == 2)
					return;
				if (isSupportedScanCode(code)) {
					int usage = mCurrentHidUsage;
					mCurrentHidUsage = 0;
					processKey(when, value != 0, code, usage);
				}
			}
		}

		private int findKeyDownIndex(int scanCode) {
			for (int i = 0; i < mKeyDowns.size(); i++)
				if (mKeyDowns.get(i)[1] == scanCode)
					return i;
			return -1;
		}

		/* KeyboardInputMapper::processKey() */
		private void processKey(long when, boolean down, int scanCode, int usageCode) {
			int[] mapped = EventHub.mapKey(mDevice.device, scanCode, usageCode);
			int keyCode = mapped != null ? mapped[0] : KeyEvent.KEYCODE_UNKNOWN;
			int policyFlags = mapped != null ? mapped[1] : 0;
			int flags = KeyEvent.FLAG_FROM_SYSTEM;
			long downTime = when;
			int keyDownIndex = findKeyDownIndex(scanCode);
			if (down) {
				if (keyDownIndex >= 0) {
					keyCode = mKeyDowns.get(keyDownIndex)[0];
					flags = mKeyDowns.get(keyDownIndex)[2];
					downTime = mKeyDownTimes.get(keyDownIndex);
				} else {
					if ((policyFlags & KeyLayoutMap.POLICY_FLAG_GESTURE) != 0)
						flags |= KeyEvent.FLAG_KEEP_TOUCH_MODE;
					mKeyDowns.add(new int[] {keyCode, scanCode, flags});
					mKeyDownTimes.add(when);
				}
			} else {
				if (keyDownIndex < 0)
					return; /* the key was not down */
				keyCode = mKeyDowns.get(keyDownIndex)[0];
				flags = mKeyDowns.get(keyDownIndex)[2];
				downTime = mKeyDownTimes.get(keyDownIndex);
				mKeyDowns.remove(keyDownIndex);
				mKeyDownTimes.remove(keyDownIndex);
			}
			KeyEvent event = new KeyEvent(downTime, when, down ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP, keyCode, 0, 0,
						      mDevice.device.id, scanCode, flags, mDevice.sources & mSource);
			InputDispatcher.notifyKey(event);
		}

		/* KeyboardInputMapper::cancelAllDownKeys(): up with FLAG_CANCELED for every key that is down */
		void cancelAllDownKeys(long when) {
			for (int i = 0; i < mKeyDowns.size(); i++) {
				int[] keyDown = mKeyDowns.get(i);
				InputDispatcher.notifyKey(new KeyEvent(mKeyDownTimes.get(i), when, KeyEvent.ACTION_UP, keyDown[0], 0, 0,
								       mDevice.device.id, keyDown[1], keyDown[2] | KeyEvent.FLAG_CANCELED,
								       mDevice.sources & mSource));
			}
			mKeyDowns.clear();
			mKeyDownTimes.clear();
		}
	}

	/* AOSP JoystickInputMapper: the absolute axes of the device as MotionEvent axes in -1..1 (centered) or 0..1,
	 * with the mapping of the key layout, a MotionEvent (SOURCE_JOYSTICK, ACTION_MOVE) per SYN_REPORT if a value
	 * changed by more than the axis' filter */
	private static final class JoystickInputMapper {
		private static final int MAX_AXES = 30; /* PointerCoords::MAX_AXES */

		private static final class Axis {
			int rawMin, rawMax, rawFlat, rawFuzz, rawResolution;
			KeyLayoutMap.AxisInfo axisInfo;
			boolean explicitlyMapped;
			float scale, offset, highScale, highOffset, min, max, flat, fuzz, resolution, filter;
			float currentValue, newValue, highCurrentValue, highNewValue;
		}

		private final ReaderDevice mDevice;
		private final TreeMap<Integer, Axis> mAxes = new TreeMap<>();

		/* JoystickInputMapper::reconfigure(), first time */
		JoystickInputMapper(ReaderDevice device) {
			mDevice = device;
			EventHub.Device hubDevice = device.device;
			for (int abs = 0; abs <= EventHub.ABS_MAX; abs++) {
				if ((EventHub.getAbsAxisUsage(abs, hubDevice.classes) & EventHub.CLASS_JOYSTICK) == 0)
					continue; /* the axis belongs to another kind of device */
				int[] rawAxisInfo = hubDevice.getAbsoluteAxisInfo(abs);
				if (rawAxisInfo == null)
					continue;
				KeyLayoutMap.AxisInfo axisInfo = hubDevice.keyLayoutMap != null ? hubDevice.keyLayoutMap.mapAxis(abs) : null;
				boolean explicitlyMapped = axisInfo != null;
				if (!explicitlyMapped)
					axisInfo = new KeyLayoutMap.AxisInfo();
				mAxes.put(abs, createAxis(axisInfo, rawAxisInfo, explicitlyMapped));
			}
			if (mAxes.size() > MAX_AXES) {
				pruneAxes(true);
				pruneAxes(false);
			}
			int nextGenericAxisId = MotionEvent.AXIS_GENERIC_1;
			for (java.util.Iterator<Axis> it = mAxes.values().iterator(); it.hasNext();) {
				Axis axis = it.next();
				if (axis.axisInfo.axis < 0) {
					while (nextGenericAxisId <= MotionEvent.AXIS_GENERIC_16 && haveAxis(nextGenericAxisId))
						nextGenericAxisId++;
					if (nextGenericAxisId <= MotionEvent.AXIS_GENERIC_16) {
						axis.axisInfo.axis = nextGenericAxisId++;
					} else {
						it.remove(); /* all generic axes are taken */
					}
				}
			}
		}

		private static boolean isCenteredAxis(int axis) {
			switch (axis) {
				case MotionEvent.AXIS_X:
				case MotionEvent.AXIS_Y:
				case MotionEvent.AXIS_Z:
				case MotionEvent.AXIS_RX:
				case MotionEvent.AXIS_RY:
				case MotionEvent.AXIS_RZ:
				case MotionEvent.AXIS_HAT_X:
				case MotionEvent.AXIS_HAT_Y:
				case MotionEvent.AXIS_ORIENTATION:
				case MotionEvent.AXIS_RUDDER:
				case MotionEvent.AXIS_WHEEL:
					return true;
				default:
					return false;
			}
		}

		/* JoystickInputMapper::createAxis(); raw: {min, max, flat, fuzz, resolution} */
		private static Axis createAxis(KeyLayoutMap.AxisInfo axisInfo, int[] raw, boolean explicitlyMapped) {
			Axis axis = new Axis();
			axis.rawMin = raw[0];
			axis.rawMax = raw[1];
			axis.rawFlat = raw[2];
			axis.rawFuzz = raw[3];
			axis.rawResolution = raw[4];
			axis.axisInfo = axisInfo;
			axis.explicitlyMapped = explicitlyMapped;
			int rawFlat = axisInfo.flatOverride < 0 ? axis.rawFlat : axisInfo.flatOverride;
			if (axisInfo.mode == KeyLayoutMap.AxisInfo.MODE_SPLIT) {
				axis.scale = 1.0f / (axisInfo.splitValue - axis.rawMin);
				axis.highScale = 1.0f / (axis.rawMax - axisInfo.splitValue);
			} else if (isCenteredAxis(axisInfo.axis)) {
				axis.scale = 2.0f / (axis.rawMax - axis.rawMin);
				axis.offset = ((axis.rawMin + axis.rawMax) * 0.5f) * -axis.scale;
				axis.highOffset = axis.offset;
				axis.highScale = axis.scale;
				axis.min = -1.0f;
			} else {
				axis.scale = 1.0f / (axis.rawMax - axis.rawMin);
				axis.highScale = axis.scale;
			}
			axis.max = 1.0f;
			axis.flat = rawFlat * axis.scale;
			axis.fuzz = axis.rawFuzz * axis.scale;
			axis.resolution = axis.rawResolution * axis.scale;
			axis.filter = axis.fuzz != 0 ? axis.fuzz : axis.flat * 0.25f;
			return axis;
		}

		private boolean haveAxis(int axisId) {
			for (Axis axis : mAxes.values())
				if (axis.axisInfo.axis == axisId || (axis.axisInfo.mode == KeyLayoutMap.AxisInfo.MODE_SPLIT && axis.axisInfo.highAxis == axisId))
					return true;
			return false;
		}

		/* JoystickInputMapper::pruneAxes(): the axes with the lowest codes go first */
		private void pruneAxes(boolean ignoreExplicitlyMappedAxes) {
			for (java.util.Iterator<Axis> it = mAxes.values().iterator(); mAxes.size() > MAX_AXES && it.hasNext();) {
				Axis axis = it.next();
				if (ignoreExplicitlyMappedAxes && axis.explicitlyMapped)
					continue;
				it.remove();
			}
		}

		/* getCompatAxis(): the old axes BRAKE and GAS mirror LTRIGGER and RTRIGGER */
		private static int getCompatAxis(int axis) {
			switch (axis) {
				case MotionEvent.AXIS_LTRIGGER:
					return MotionEvent.AXIS_BRAKE;
				case MotionEvent.AXIS_RTRIGGER:
					return MotionEvent.AXIS_GAS;
			}
			return -1;
		}

		/* populateDeviceInfo(), addMotionRange() */
		void populateDeviceInfo(InputDevice.Builder builder) {
			for (Axis axis : mAxes.values()) {
				addMotionRange(builder, axis.axisInfo.axis, axis);
				if (axis.axisInfo.mode == KeyLayoutMap.AxisInfo.MODE_SPLIT)
					addMotionRange(builder, axis.axisInfo.highAxis, axis);
			}
		}

		private static void addMotionRange(InputDevice.Builder builder, int axisId, Axis axis) {
			builder.addMotionRange(axisId, InputDevice.SOURCE_JOYSTICK, axis.min, axis.max, axis.flat, axis.fuzz, axis.resolution);
			int compatAxis = getCompatAxis(axisId);
			if (compatAxis >= 0)
				builder.addMotionRange(compatAxis, InputDevice.SOURCE_JOYSTICK, axis.min, axis.max, axis.flat, axis.fuzz, axis.resolution);
		}

		/* JoystickInputMapper::reset() */
		void reset() {
			for (Axis axis : mAxes.values()) {
				axis.currentValue = axis.newValue = 0;
				axis.highCurrentValue = axis.highNewValue = 0;
			}
		}

		/* JoystickInputMapper::process() */
		void process(long when, int type, int code, int value) {
			if (type == EventHub.EV_ABS) {
				Axis axis = mAxes.get(code);
				if (axis == null)
					return;
				float newValue, highNewValue;
				switch (axis.axisInfo.mode) {
					case KeyLayoutMap.AxisInfo.MODE_INVERT:
						newValue = (axis.rawMax - value) * axis.scale + axis.offset;
						highNewValue = 0.0f;
						break;
					case KeyLayoutMap.AxisInfo.MODE_SPLIT:
						if (value < axis.axisInfo.splitValue) {
							newValue = (axis.axisInfo.splitValue - value) * axis.scale + axis.offset;
							highNewValue = 0.0f;
						} else if (value > axis.axisInfo.splitValue) {
							newValue = 0.0f;
							highNewValue = (value - axis.axisInfo.splitValue) * axis.highScale + axis.highOffset;
						} else {
							newValue = 0.0f;
							highNewValue = 0.0f;
						}
						break;
					default:
						newValue = value * axis.scale + axis.offset;
						highNewValue = 0.0f;
						break;
				}
				axis.newValue = newValue;
				axis.highNewValue = highNewValue;
			} else if (type == EventHub.EV_SYN && code == EventHub.SYN_REPORT) {
				sync(when, false);
			}
		}

		/* JoystickInputMapper::sync() */
		private void sync(long when, boolean force) {
			if (!filterAxes(force))
				return;
			MotionEvent.PointerProperties pointerProperties = new MotionEvent.PointerProperties();
			pointerProperties.id = 0;
			pointerProperties.toolType = MotionEvent.TOOL_TYPE_UNKNOWN;
			MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
			for (Axis axis : mAxes.values()) {
				setPointerCoordsAxisValue(pointerCoords, axis.axisInfo.axis, axis.currentValue);
				if (axis.axisInfo.mode == KeyLayoutMap.AxisInfo.MODE_SPLIT)
					setPointerCoordsAxisValue(pointerCoords, axis.axisInfo.highAxis, axis.highCurrentValue);
			}
			MotionEvent event = MotionEvent.obtain(0, when, MotionEvent.ACTION_MOVE, 1,
							       new MotionEvent.PointerProperties[] {pointerProperties},
							       new MotionEvent.PointerCoords[] {pointerCoords}, 0, 0, 0, 0,
							       mDevice.device.id, 0, InputDevice.SOURCE_JOYSTICK, 0);
			InputDispatcher.notifyMotion(event);
		}

		private static void setPointerCoordsAxisValue(MotionEvent.PointerCoords pointerCoords, int axis, float value) {
			pointerCoords.setAxisValue(axis, value);
			int compatAxis = getCompatAxis(axis);
			if (compatAxis >= 0)
				pointerCoords.setAxisValue(compatAxis, value);
		}

		private boolean filterAxes(boolean force) {
			boolean atLeastOneSignificantChange = force;
			for (Axis axis : mAxes.values()) {
				if (force || hasValueChangedSignificantly(axis.filter, axis.newValue, axis.currentValue, axis.min, axis.max)) {
					axis.currentValue = axis.newValue;
					atLeastOneSignificantChange = true;
				}
				if (axis.axisInfo.mode == KeyLayoutMap.AxisInfo.MODE_SPLIT) {
					if (force || hasValueChangedSignificantly(axis.filter, axis.highNewValue, axis.highCurrentValue, axis.min, axis.max)) {
						axis.highCurrentValue = axis.highNewValue;
						atLeastOneSignificantChange = true;
					}
				}
			}
			return atLeastOneSignificantChange;
		}

		private static boolean hasValueChangedSignificantly(float filter, float newValue, float currentValue, float min, float max) {
			if (newValue != currentValue) {
				if (Math.abs(newValue - currentValue) > filter
				    || hasMovedNearerToValueWithinFilteredRange(filter, newValue, currentValue, min)
				    || hasMovedNearerToValueWithinFilteredRange(filter, newValue, currentValue, max)
				    || hasMovedNearerToValueWithinFilteredRange(filter, newValue, currentValue, 0))
					return true;
			}
			return false;
		}

		private static boolean hasMovedNearerToValueWithinFilteredRange(float filter, float newValue, float currentValue, float thresholdValue) {
			float newDistance = Math.abs(newValue - thresholdValue);
			if (newDistance < filter) {
				float oldDistance = Math.abs(currentValue - thresholdValue);
				return newDistance < oldDistance;
			}
			return false;
		}
	}

	/* AOSP VibratorInputMapper with VibrationElement: the steps of the pattern, each an FF_RUMBLE effect with the
	 * amplitudes of the strong (0) and weak (1) channel times 256, or off; the next step after its duration (at
	 * most 100 s, MAX_VIBRATE_PATTERN_DELAY) */
	private static final class VibratorInputMapper {
		static final int MAX_VIBRATE_PATTERN_SIZE = 100;
		private static final long MAX_VIBRATE_PATTERN_DELAY_MILLIS = 100000;

		private final ReaderDevice mDevice;
		boolean vibrating;
		int token;
		private long[] mPattern;
		private SparseArray<int[]> mAmplitudes;
		private int mRepeat;
		private int mIndex;
		private final Runnable mNextStep = this::nextStep;

		VibratorInputMapper(ReaderDevice device) {
			mDevice = device;
		}

		void vibrate(long[] pattern, SparseArray<int[]> amplitudes, int repeat, int token) {
			getHandler().removeCallbacks(mNextStep);
			vibrating = true;
			mPattern = pattern;
			mAmplitudes = amplitudes;
			mRepeat = repeat;
			this.token = token;
			mIndex = -1;
			nextStep();
		}

		private int amplitude(int vibratorId, int index) {
			int[] amplitudes = mAmplitudes.get(vibratorId);
			return amplitudes != null ? amplitudes[index] & 0xff : 0;
		}

		private void nextStep() {
			mIndex += 1;
			if (mIndex >= mPattern.length) {
				if (mRepeat < 0) {
					stopVibrating();
					return;
				}
				mIndex = mRepeat;
			}
			long duration = Math.min(mPattern[mIndex], MAX_VIBRATE_PATTERN_DELAY_MILLIS);
			int strong = amplitude(0, mIndex), weak = amplitude(1, mIndex);
			if (strong != 0 || weak != 0)
				EventHub.vibrate(mDevice.device, strong << 8, weak << 8, (int)duration);
			else
				EventHub.cancelVibrate(mDevice.device);
			getHandler().postDelayed(mNextStep, duration);
		}

		void stopVibrating() {
			getHandler().removeCallbacks(mNextStep);
			vibrating = false;
			EventHub.cancelVibrate(mDevice.device);
		}
	}
}
