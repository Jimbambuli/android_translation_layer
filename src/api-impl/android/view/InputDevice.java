package android.view;

import android.os.NullVibrator;
import android.os.Vibrator;
import java.util.ArrayList;
import java.util.List;

public class InputDevice {

	public static final int SOURCE_CLASS_NONE = 0x00000000;
	public static final int SOURCE_CLASS_BUTTON = 0x00000001;
	public static final int SOURCE_CLASS_POINTER = 0x00000002;
	public static final int SOURCE_CLASS_JOYSTICK = 0x00000010;
	public static final int SOURCE_CLASS_GAMEPAD = 0x00000400 | SOURCE_CLASS_BUTTON;
	public static final int SOURCE_KEYBOARD = 0x00000100 | SOURCE_CLASS_BUTTON;
	public static final int SOURCE_TOUCHSCREEN = 0x00001000 | SOURCE_CLASS_POINTER;
	public static final int SOURCE_MOUSE = 0x00002000 | SOURCE_CLASS_POINTER;
	public static final int SOURCE_ROTARY_ENCODER = 0x00400000 | SOURCE_CLASS_NONE;

	/* AOSP: the keyboard type of the device; ATL's one device includes the computer's keyboard */
	public static final int KEYBOARD_TYPE_NONE = 0;
	public static final int KEYBOARD_TYPE_NON_ALPHABETIC = 1;
	public static final int KEYBOARD_TYPE_ALPHABETIC = 2;

	public static int[] getDeviceIds() {
		return new int[] {0}; // might work?
	}

	public static InputDevice getDevice(int id) {
		return new InputDevice();
	}

	public int getId() {
		return 0; // might work?
	}

	/* AOSP: the vibrator of the input device (e.g. a game controller); ATL's input device has none */
	public Vibrator getVibrator() {
		return new NullVibrator();
	}

	public int getProductId() {
		return 0x69;
	}

	public int getVendorId() {
		return 0x420;
	}

	public String getName() {
		return "FIXME-name";
	}

	public String getDescriptor() {
		return "FIXME-descriptor";
	}

	public int getKeyboardType() {
		return KEYBOARD_TYPE_ALPHABETIC;
	}

	public boolean isVirtual() {
		return false;
	}

	/* AOSP 16 InputDevice.isExternal(): mIsExternal. ATL's single device presents itself as the
	 * built-in touchscreen (getSources(), isVirtual() false), which is not external */
	public boolean isExternal() {
		return false;
	}

	public int getSources() {
		return 2 | 4098 /* SOURCE_CLASS_POINTER | SOURCE_TOUCHSCREEN */; // FIXME
	}

	/*
	 * FIXME: We pretend we can do literally everything here...
	 */

	public boolean[] hasKeys(int... keys) {
		boolean[] ret = new boolean[keys.length];
		for (int i = 0; i < keys.length; i++) {
			ret[i] = true;
		}
		return ret;
	}

	public List<InputDevice.MotionRange> getMotionRanges() {
		MotionRange[] ranges = new MotionRange[32];

		for (int i = 0; i < ranges.length; i++) {
			ranges[i] = new MotionRange(i);
		}

		return new ArrayList<InputDevice.MotionRange>();
	}

	public boolean supportsSource(int source) {
		return true;
	}

	public MotionRange getMotionRange(int axis, int source) {
		return new MotionRange(axis, source);
	}

	public MotionRange getMotionRange(int axis) {
		return new MotionRange(axis);
	}

	/* AOSP InputDevice.MotionRange; ATL reports no real ranges, so min == max == 0 */
	public class MotionRange {
		int axis;
		int source;
		float min;
		float max;

		public MotionRange(int axis) {
			this(axis, InputDevice.this.getSources());
		}

		MotionRange(int axis, int source) {
			this.axis = axis;
			this.source = source;
		}

		public int getAxis() {
			return this.axis;
		}

		public int getSource() {
			return source;
		}

		/* AOSP MotionRange.isFromSource() */
		public boolean isFromSource(int source) {
			return (getSource() & source) == source;
		}

		public float getMin() {
			return min;
		}

		public float getMax() {
			return max;
		}

		public float getRange() {
			return max - min;
		}

		/* AOSP MotionRange.getFlat()/getFuzz(): like min and max, ATL's ranges have none */
		public float getFlat() {
			return 0;
		}

		public float getFuzz() {
			return 0;
		}
	};
}
