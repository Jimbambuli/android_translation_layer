package android.view;

/**
 * Constants to be used to play sound effects via View.playSoundEffect(int)
 * (AOSP android-16.0.0_r4 SoundEffectConstants, without the hidden repeat-sound randomizer).
 */
public class SoundEffectConstants {
	private SoundEffectConstants() {}

	public static final int CLICK = 0;
	public static final int NAVIGATION_LEFT = 1;
	public static final int NAVIGATION_UP = 2;
	public static final int NAVIGATION_RIGHT = 3;
	public static final int NAVIGATION_DOWN = 4;
	public static final int NAVIGATION_REPEAT_LEFT = 5;
	public static final int NAVIGATION_REPEAT_UP = 6;
	public static final int NAVIGATION_REPEAT_RIGHT = 7;
	public static final int NAVIGATION_REPEAT_DOWN = 8;

	// the typo is part of the API
	public static int getContantForFocusDirection(int direction) {
		switch (direction) {
			case View.FOCUS_RIGHT:
				return SoundEffectConstants.NAVIGATION_RIGHT;
			case View.FOCUS_FORWARD:
			case View.FOCUS_DOWN:
				return SoundEffectConstants.NAVIGATION_DOWN;
			case View.FOCUS_LEFT:
				return SoundEffectConstants.NAVIGATION_LEFT;
			case View.FOCUS_BACKWARD:
			case View.FOCUS_UP:
				return SoundEffectConstants.NAVIGATION_UP;
		}
		throw new IllegalArgumentException("direction must be one of "
		                                   + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, FOCUS_FORWARD, FOCUS_BACKWARD}.");
	}

	public static int getConstantForFocusDirection(int direction, boolean repeating) {
		if (repeating) {
			switch (direction) {
				case View.FOCUS_RIGHT:
					return SoundEffectConstants.NAVIGATION_REPEAT_RIGHT;
				case View.FOCUS_FORWARD:
				case View.FOCUS_DOWN:
					return SoundEffectConstants.NAVIGATION_REPEAT_DOWN;
				case View.FOCUS_LEFT:
					return SoundEffectConstants.NAVIGATION_REPEAT_LEFT;
				case View.FOCUS_BACKWARD:
				case View.FOCUS_UP:
					return SoundEffectConstants.NAVIGATION_REPEAT_UP;
			}
			throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, "
			                                   + "FOCUS_LEFT, FOCUS_RIGHT, FOCUS_FORWARD, FOCUS_BACKWARD}.");
		} else {
			return getContantForFocusDirection(direction);
		}
	}

	/** @hide */
	public static boolean isNavigationRepeat(int effectId) {
		return effectId == SoundEffectConstants.NAVIGATION_REPEAT_DOWN
		    || effectId == SoundEffectConstants.NAVIGATION_REPEAT_LEFT
		    || effectId == SoundEffectConstants.NAVIGATION_REPEAT_RIGHT
		    || effectId == SoundEffectConstants.NAVIGATION_REPEAT_UP;
	}
}
