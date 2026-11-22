package com.android.server.input;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/* The part of AOSP's InputDispatcher, PhoneWindowManager and ViewRootImpl that the events of InputReader go through
 * before and after the app's focused window gets them (the window part is activity_dispatch_device_key() and
 * activity_dispatch_device_motion() in android_app_Activity.c):
 * - InputDispatcher::dispatchKeyLocked(), synthesizeKeyRepeatLocked(): a key that stays down repeats after
 *   ViewConfiguration.getKeyRepeatTimeout(), then every getKeyRepeatDelay(), the first repeat with FLAG_LONG_PRESS
 * - InputDispatcher::afterKeyEventLockedInterruptable() with PhoneWindowManager.dispatchUnhandledKey(): a key the
 *   window did not handle on its first down gets the fallback key of the key character map (Generic.kcm:
 *   BUTTON_A -> DPAD_CENTER, BUTTON_B -> BACK, ...) with FLAG_FALLBACK, for the rest of the key; HOME, which the
 *   policy keeps for itself on Android, goes nowhere (ATL has no home screen)
 * - ViewRootImpl.SyntheticJoystickHandler: joystick motion the window did not handle moves the focus as DPAD keys
 * Without a focused window of the app nothing is delivered (the user works in another window). */
final class InputDispatcher {
	private static final int RESULT_NO_FOCUSED_WINDOW = -1;
	private static final int RESULT_HANDLED = 1;

	private static Handler sHandler;
	/* mKeyRepeatState */
	private static KeyEvent sLastKeyEntry;
	private static final Runnable sKeyRepeat = InputDispatcher::synthesizeKeyRepeat;
	/* InputState's fallback keys of the focused window: original key code -> fallback key code (UNKNOWN: none) */
	private static final SparseArray<Integer> sFallbackKeys = new SparseArray<>();
	/* PhoneWindowManager.mFallbackActions */
	private static final SparseArray<KeyCharacterMap.FallbackAction> sFallbackActions = new SparseArray<>();
	private static final SyntheticJoystickHandler sJoystick = new SyntheticJoystickHandler();

	private InputDispatcher() {}

	private static Handler getHandler() {
		if (sHandler == null)
			sHandler = new Handler(Looper.getMainLooper());
		return sHandler;
	}

	/* InputDispatcher::notifyKey() and dispatchKeyLocked(): the key repeat. HOME never reaches the app: the policy
	 * intercepts it before dispatching (PhoneWindowManager.interceptKeyBeforeDispatching() -> handleHomeButton(),
	 * which goes home on the up key that was not canceled) */
	static void notifyKey(KeyEvent entry) {
		if (entry.getKeyCode() == KeyEvent.KEYCODE_HOME) {
			if (entry.getAction() == KeyEvent.ACTION_UP && !entry.isCanceled())
				nativeGoHome();
			return;
		}
		if (entry.getAction() == KeyEvent.ACTION_DOWN) {
			if (sLastKeyEntry != null && sLastKeyEntry.getKeyCode() == entry.getKeyCode()
			    && sLastKeyEntry.getDeviceId() == entry.getDeviceId()) {
				/* a repeat of the device itself: Android stops repeating */
				entry = KeyEvent.changeTimeRepeat(entry, entry.getEventTime(), sLastKeyEntry.getRepeatCount() + 1);
				resetKeyRepeat();
			} else {
				resetKeyRepeat();
				getHandler().postAtTime(sKeyRepeat, entry.getEventTime() + ViewConfiguration.getKeyRepeatTimeout());
			}
			sLastKeyEntry = entry;
		} else if (sLastKeyEntry == null || sLastKeyEntry.getDeviceId() == entry.getDeviceId()) {
			resetKeyRepeat();
		}
		dispatchKey(entry);
	}

	/* InputDispatcher::notifyDeviceReset(): the repeat of the device's key ends */
	static void notifyDeviceReset(int deviceId) {
		if (sLastKeyEntry != null && sLastKeyEntry.getDeviceId() == deviceId)
			resetKeyRepeat();
	}

	private static void resetKeyRepeat() {
		getHandler().removeCallbacks(sKeyRepeat);
		sLastKeyEntry = null;
	}

	private static void synthesizeKeyRepeat() {
		KeyEvent entry = sLastKeyEntry;
		if (entry == null)
			return;
		long now = SystemClock.uptimeMillis();
		sLastKeyEntry = KeyEvent.changeTimeRepeat(entry, now, entry.getRepeatCount() + 1);
		getHandler().postAtTime(sKeyRepeat, now + ViewConfiguration.getKeyRepeatDelay());
		dispatchKey(sLastKeyEntry);
	}

	/* dispatchKeyLocked(): FLAG_LONG_PRESS on the first repeat, then to the focused window and
	 * afterKeyEventLockedInterruptable() */
	private static void dispatchKey(KeyEvent entry) {
		int flags = entry.getRepeatCount() == 1 ? entry.getFlags() | KeyEvent.FLAG_LONG_PRESS : entry.getFlags() & ~KeyEvent.FLAG_LONG_PRESS;
		KeyEvent event = KeyEvent.changeFlags(entry, flags);
		int result = nativeDispatchKey(event);
		if (result == RESULT_NO_FOCUSED_WINDOW)
			return;
		afterKeyEvent(event, result == RESULT_HANDLED);
	}

	private static KeyEvent copyWithFlags(KeyEvent event, int keyCode, int metaState, int flags) {
		return new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(), keyCode, event.getRepeatCount(),
				    metaState, event.getDeviceId(), event.getScanCode(), flags, event.getSource());
	}

	/* InputDispatcher::afterKeyEventLockedInterruptable() */
	private static void afterKeyEvent(KeyEvent keyEntry, boolean handled) {
		if ((keyEntry.getFlags() & KeyEvent.FLAG_FALLBACK) != 0)
			return;
		int originalKeyCode = keyEntry.getKeyCode();
		Integer fallbackKeyCode = sFallbackKeys.get(originalKeyCode);
		if (keyEntry.getAction() == KeyEvent.ACTION_UP)
			sFallbackKeys.remove(originalKeyCode);

		if (handled) {
			if (fallbackKeyCode != null) {
				/* the application handled the original key: the policy forgets the fallback, and a
				 * fallback key that was dispatched is canceled */
				dispatchUnhandledKey(copyWithFlags(keyEntry, originalKeyCode, keyEntry.getMetaState(), keyEntry.getFlags() | KeyEvent.FLAG_CANCELED));
				if (fallbackKeyCode != KeyEvent.KEYCODE_UNKNOWN)
					cancelFallbackKey(keyEntry, fallbackKeyCode);
				sFallbackKeys.remove(originalKeyCode);
			}
			return;
		}

		boolean initialDown = keyEntry.getAction() == KeyEvent.ACTION_DOWN && keyEntry.getRepeatCount() == 0;
		if (fallbackKeyCode == null && !initialDown)
			return;
		KeyEvent fallbackEvent = dispatchUnhandledKey(keyEntry);
		if (initialDown) {
			fallbackKeyCode = fallbackEvent != null ? fallbackEvent.getKeyCode() : KeyEvent.KEYCODE_UNKNOWN;
			sFallbackKeys.put(originalKeyCode, fallbackKeyCode);
		}
		if (fallbackKeyCode != KeyEvent.KEYCODE_UNKNOWN && (fallbackEvent == null || fallbackKeyCode != fallbackEvent.getKeyCode())) {
			/* the policy no longer wants the fallback */
			cancelFallbackKey(keyEntry, fallbackKeyCode);
			fallbackEvent = null;
			if (keyEntry.getAction() != KeyEvent.ACTION_UP)
				sFallbackKeys.put(originalKeyCode, KeyEvent.KEYCODE_UNKNOWN);
		}
		if (fallbackEvent != null) {
			int result = nativeDispatchKey(fallbackEvent);
			if (result != RESULT_NO_FOCUSED_WINDOW)
				afterKeyEvent(fallbackEvent, result == RESULT_HANDLED);
		}
	}

	/* synthesizeCancelationEventsForWindowLocked(CANCEL_FALLBACK_EVENTS): the fallback key goes up, canceled */
	private static void cancelFallbackKey(KeyEvent keyEntry, int fallbackKeyCode) {
		KeyEvent cancel = new KeyEvent(keyEntry.getDownTime(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, fallbackKeyCode, 0,
					       keyEntry.getMetaState(), keyEntry.getDeviceId(), keyEntry.getScanCode(),
					       KeyEvent.FLAG_FALLBACK | KeyEvent.FLAG_CANCELED, keyEntry.getSource());
		nativeDispatchKey(cancel);
	}

	/* PhoneWindowManager.dispatchUnhandledKey(): the fallback action of the device's key character map, kept from
	 * the initial down to the up */
	private static KeyEvent dispatchUnhandledKey(KeyEvent event) {
		if ((event.getFlags() & KeyEvent.FLAG_FALLBACK) != 0)
			return null;
		int keyCode = event.getKeyCode();
		boolean initialDown = event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0;
		KeyCharacterMap.FallbackAction fallbackAction = initialDown
			? event.getKeyCharacterMap().getFallbackAction(keyCode, event.getMetaState())
			: sFallbackActions.get(keyCode);
		if (fallbackAction == null)
			return null;
		KeyEvent fallbackEvent = copyWithFlags(event, fallbackAction.keyCode, fallbackAction.metaState, event.getFlags() | KeyEvent.FLAG_FALLBACK);
		/* interceptFallback(): the policy keeps HOME for itself and goes home on its up key (handleHomeButton()) */
		if (fallbackAction.keyCode == KeyEvent.KEYCODE_HOME) {
			if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled())
				nativeGoHome();
			fallbackEvent = null;
		}
		if (initialDown) {
			sFallbackActions.put(keyCode, fallbackAction);
		} else if (event.getAction() == KeyEvent.ACTION_UP) {
			sFallbackActions.remove(keyCode);
			fallbackAction.recycle();
		}
		return fallbackEvent;
	}

	/* InputDispatcher::notifyMotion() to the focused window; ViewRootImpl's SyntheticInputStage turns joystick
	 * motion that no view handled into DPAD keys, and handled motion cancels them (onDeliverToNext()) */
	static void notifyMotion(MotionEvent event) {
		int result = nativeDispatchMotion(event);
		if (result == RESULT_NO_FOCUSED_WINDOW)
			return;
		if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
			if (result == RESULT_HANDLED)
				sJoystick.cancel();
			else
				sJoystick.process(event);
		}
	}

	/* ViewRootImpl.enqueueInputEvent() of a synthesized key: through the window's input stages, without
	 * InputDispatcher (no key repeat, no fallback) */
	private static void enqueueInputEvent(KeyEvent event) {
		nativeDispatchKey(event);
	}

	/* AOSP ViewRootImpl.SyntheticJoystickHandler */
	private static final class SyntheticJoystickHandler extends Handler {
		private static final int MSG_ENQUEUE_X_AXIS_KEY_REPEAT = 1;
		private static final int MSG_ENQUEUE_Y_AXIS_KEY_REPEAT = 2;

		private final JoystickAxesState mJoystickAxesState = new JoystickAxesState();
		private final SparseArray<KeyEvent> mDeviceKeyEvents = new SparseArray<>();

		SyntheticJoystickHandler() {
			super(Looper.getMainLooper());
		}

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case MSG_ENQUEUE_X_AXIS_KEY_REPEAT:
				case MSG_ENQUEUE_Y_AXIS_KEY_REPEAT: {
					KeyEvent oldEvent = (KeyEvent)msg.obj;
					KeyEvent e = KeyEvent.changeTimeRepeat(oldEvent, SystemClock.uptimeMillis(), oldEvent.getRepeatCount() + 1);
					enqueueInputEvent(e);
					sendMessageDelayed(obtainMessage(msg.what, e), ViewConfiguration.getKeyRepeatDelay());
				} break;
			}
		}

		void process(MotionEvent event) {
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_CANCEL:
					cancel();
					break;
				case MotionEvent.ACTION_MOVE:
					update(event);
					break;
			}
		}

		void cancel() {
			removeMessages(MSG_ENQUEUE_X_AXIS_KEY_REPEAT);
			removeMessages(MSG_ENQUEUE_Y_AXIS_KEY_REPEAT);
			for (int i = 0; i < mDeviceKeyEvents.size(); i++) {
				final KeyEvent keyEvent = mDeviceKeyEvents.valueAt(i);
				if (keyEvent != null)
					enqueueInputEvent(KeyEvent.changeTimeRepeat(keyEvent, SystemClock.uptimeMillis(), 0));
			}
			mDeviceKeyEvents.clear();
			mJoystickAxesState.resetState();
		}

		private void update(MotionEvent event) {
			final long time = event.getEventTime();
			mJoystickAxesState.updateStateForAxis(event, time, MotionEvent.AXIS_X, event.getAxisValue(MotionEvent.AXIS_X));
			mJoystickAxesState.updateStateForAxis(event, time, MotionEvent.AXIS_Y, event.getAxisValue(MotionEvent.AXIS_Y));
			mJoystickAxesState.updateStateForAxis(event, time, MotionEvent.AXIS_HAT_X, event.getAxisValue(MotionEvent.AXIS_HAT_X));
			mJoystickAxesState.updateStateForAxis(event, time, MotionEvent.AXIS_HAT_Y, event.getAxisValue(MotionEvent.AXIS_HAT_Y));
		}

		final class JoystickAxesState {
			private static final int STATE_UP_OR_LEFT = -1;
			private static final int STATE_NEUTRAL = 0;
			private static final int STATE_DOWN_OR_RIGHT = 1;

			final int[] mAxisStatesHat = {STATE_NEUTRAL, STATE_NEUTRAL}; // {AXIS_HAT_X, AXIS_HAT_Y}
			final int[] mAxisStatesStick = {STATE_NEUTRAL, STATE_NEUTRAL}; // {AXIS_X, AXIS_Y}

			void resetState() {
				mAxisStatesHat[0] = STATE_NEUTRAL;
				mAxisStatesHat[1] = STATE_NEUTRAL;
				mAxisStatesStick[0] = STATE_NEUTRAL;
				mAxisStatesStick[1] = STATE_NEUTRAL;
			}

			void updateStateForAxis(MotionEvent event, long time, int axis, float value) {
				final int axisStateIndex;
				final int repeatMessage;
				if (isXAxis(axis)) {
					axisStateIndex = 0;
					repeatMessage = MSG_ENQUEUE_X_AXIS_KEY_REPEAT;
				} else if (isYAxis(axis)) {
					axisStateIndex = 1;
					repeatMessage = MSG_ENQUEUE_Y_AXIS_KEY_REPEAT;
				} else {
					return;
				}
				final int newState = joystickAxisValueToState(value);
				final int currentState = (axis == MotionEvent.AXIS_X || axis == MotionEvent.AXIS_Y)
					? mAxisStatesStick[axisStateIndex] : mAxisStatesHat[axisStateIndex];
				if (currentState == newState)
					return;

				final int metaState = event.getMetaState();
				final int deviceId = event.getDeviceId();
				final int source = event.getSource();

				if (currentState == STATE_DOWN_OR_RIGHT || currentState == STATE_UP_OR_LEFT) {
					final int keyCode = joystickAxisAndStateToKeycode(axis, currentState);
					if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
						enqueueInputEvent(new KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0, metaState, deviceId, 0,
									       KeyEvent.FLAG_FALLBACK, source));
						mDeviceKeyEvents.put(deviceId, null);
					}
					removeMessages(repeatMessage);
				}

				if (newState == STATE_DOWN_OR_RIGHT || newState == STATE_UP_OR_LEFT) {
					final int keyCode = joystickAxisAndStateToKeycode(axis, newState);
					if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
						KeyEvent keyEvent = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0, metaState, deviceId, 0,
										 KeyEvent.FLAG_FALLBACK, source);
						enqueueInputEvent(keyEvent);
						sendMessageDelayed(obtainMessage(repeatMessage, keyEvent), ViewConfiguration.getKeyRepeatTimeout());
						mDeviceKeyEvents.put(deviceId, new KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0, metaState, deviceId, 0,
											   KeyEvent.FLAG_FALLBACK | KeyEvent.FLAG_CANCELED, source));
					}
				}
				if (axis == MotionEvent.AXIS_X || axis == MotionEvent.AXIS_Y)
					mAxisStatesStick[axisStateIndex] = newState;
				else
					mAxisStatesHat[axisStateIndex] = newState;
			}

			private boolean isXAxis(int axis) {
				return axis == MotionEvent.AXIS_X || axis == MotionEvent.AXIS_HAT_X;
			}

			private boolean isYAxis(int axis) {
				return axis == MotionEvent.AXIS_Y || axis == MotionEvent.AXIS_HAT_Y;
			}

			private int joystickAxisAndStateToKeycode(int axis, int state) {
				if (isXAxis(axis) && state == STATE_UP_OR_LEFT)
					return KeyEvent.KEYCODE_DPAD_LEFT;
				if (isXAxis(axis) && state == STATE_DOWN_OR_RIGHT)
					return KeyEvent.KEYCODE_DPAD_RIGHT;
				if (isYAxis(axis) && state == STATE_UP_OR_LEFT)
					return KeyEvent.KEYCODE_DPAD_UP;
				if (isYAxis(axis) && state == STATE_DOWN_OR_RIGHT)
					return KeyEvent.KEYCODE_DPAD_DOWN;
				return KeyEvent.KEYCODE_UNKNOWN;
			}

			private int joystickAxisValueToState(float value) {
				if (value >= 0.5f)
					return STATE_DOWN_OR_RIGHT;
				else if (value <= -0.5f)
					return STATE_UP_OR_LEFT;
				else
					return STATE_NEUTRAL;
			}
		}
	}

	private static native int nativeDispatchKey(KeyEvent event);
	private static native int nativeDispatchMotion(MotionEvent event);
	private static native void nativeGoHome();
}
