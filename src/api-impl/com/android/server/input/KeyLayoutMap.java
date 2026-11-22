package com.android.server.input;

import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* AOSP KeyLayoutMap (frameworks/native/libs/input/KeyLayoutMap.cpp): a key layout file (.kl) maps the scan codes and
 * HID usages of a device to key codes and its absolute axes to MotionEvent axes. A file with an error is not used
 * at all. The leds and sensors of a file are checked like AOSP's parser does but not used (ATL reads no leds and
 * no sensors of input devices); "requires_kernel_config" is met off Android, as in AOSP's host build. */
final class KeyLayoutMap {
	static final class Key {
		final int keyCode;
		final int flags;
		Key(int keyCode, int flags) {
			this.keyCode = keyCode;
			this.flags = flags;
		}
	}

	/* AOSP AxisInfo (include/input/KeyLayoutMap.h) */
	static final class AxisInfo {
		static final int MODE_NORMAL = 0;
		static final int MODE_INVERT = 1;
		static final int MODE_SPLIT = 2;
		int mode = MODE_NORMAL;
		int axis = -1;
		int highAxis = -1;
		int splitValue = 0;
		int flatOverride = -1;
	}

	/* InputEventLabels.cpp: FLAGS_SEQUENCE (POLICY_FLAG_*) */
	static final int POLICY_FLAG_WAKE = 0x00000001;
	static final int POLICY_FLAG_VIRTUAL = 0x00000002;
	static final int POLICY_FLAG_FUNCTION = 0x00000004;
	static final int POLICY_FLAG_GESTURE = 0x00000008;
	static final int POLICY_FLAG_FALLBACK_USAGE_MAPPING = 0x00000010;
	private static final List<String> FLAG_LABELS = Arrays.asList("WAKE", "VIRTUAL", "FUNCTION", "GESTURE", "FALLBACK_USAGE_MAPPING");
	private static final List<String> LED_LABELS = Arrays.asList("NUM_LOCK", "CAPS_LOCK", "SCROLL_LOCK", "COMPOSE",
		"KANA", "SLEEP", "SUSPEND", "MUTE", "MISC", "MAIL", "CHARGING", "CONTROLLER_1", "CONTROLLER_2", "CONTROLLER_3",
		"CONTROLLER_4");
	private static final List<String> SENSOR_LABELS = Arrays.asList("ACCELEROMETER", "MAGNETIC_FIELD", "ORIENTATION",
		"GYROSCOPE", "LIGHT", "PRESSURE", "TEMPERATURE", "PROXIMITY", "GRAVITY", "LINEAR_ACCELERATION",
		"ROTATION_VECTOR", "RELATIVE_HUMIDITY", "AMBIENT_TEMPERATURE", "MAGNETIC_FIELD_UNCALIBRATED",
		"GAME_ROTATION_VECTOR", "GYROSCOPE_UNCALIBRATED", "SIGNIFICANT_MOTION");

	final SparseArray<Key> keysByScanCode = new SparseArray<>();
	final SparseArray<Key> keysByUsageCode = new SparseArray<>();
	final SparseArray<AxisInfo> axes = new SparseArray<>();
	String loadFileName;

	/* KeyLayoutMap::load(): null when the file is missing or has an error */
	static KeyLayoutMap load(String path) {
		KeyLayoutMap map = new KeyLayoutMap();
		try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
			String line;
			int lineNumber = 0;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				int comment = line.indexOf('#');
				String[] tokens = (comment >= 0 ? line.substring(0, comment) : line).trim().split("[ \t\r]+");
				if (tokens.length == 0 || tokens[0].isEmpty())
					continue;
				if (!map.parseLine(tokens))
					throw new IOException(path + ":" + lineNumber + ": cannot parse '" + line.trim() + "'");
			}
		} catch (IOException e) {
			android.util.Log.w("KeyLayoutMap", "not loading key layout: " + e.getMessage());
			return null;
		}
		map.loadFileName = path;
		return map;
	}

	private boolean parseLine(String[] tokens) {
		switch (tokens[0]) {
			case "key":
				return parseKey(tokens);
			case "axis":
				return parseAxis(tokens);
			case "led":
				return parseLed(tokens);
			case "sensor":
				return tokens.length == 4 && parseInt(tokens[1]) != null && SENSOR_LABELS.contains(tokens[2])
					&& Arrays.asList("X", "Y", "Z").contains(tokens[3]);
			case "requires_kernel_config":
				return tokens.length == 2;
			default:
				return false;
		}
	}

	private boolean parseKey(String[] tokens) {
		int i = 1;
		boolean mapUsage = i < tokens.length && tokens[i].equals("usage");
		if (mapUsage)
			i++;
		if (i + 1 >= tokens.length)
			return false;
		Integer code = parseInt(tokens[i++]);
		SparseArray<Key> map = mapUsage ? keysByUsageCode : keysByScanCode;
		if (code == null || map.indexOfKey(code) >= 0)
			return false;
		int keyCode = keyCodeByLabel(tokens[i++]);
		if (keyCode == KeyEvent.KEYCODE_UNKNOWN)
			return false;
		int flags = 0;
		for (; i < tokens.length; i++) {
			int index = FLAG_LABELS.indexOf(tokens[i]);
			if (index < 0 || (flags & (1 << index)) != 0)
				return false;
			flags |= 1 << index;
		}
		map.put(code, new Key(keyCode, flags));
		return true;
	}

	private boolean parseAxis(String[] tokens) {
		if (tokens.length < 3)
			return false;
		Integer scanCode = parseInt(tokens[1]);
		if (scanCode == null || axes.indexOfKey(scanCode) >= 0)
			return false;
		AxisInfo axisInfo = new AxisInfo();
		int i = 2;
		if (tokens[i].equals("invert")) {
			axisInfo.mode = AxisInfo.MODE_INVERT;
			if (++i >= tokens.length || (axisInfo.axis = axisByLabel(tokens[i++])) < 0)
				return false;
		} else if (tokens[i].equals("split")) {
			axisInfo.mode = AxisInfo.MODE_SPLIT;
			if (i + 3 >= tokens.length)
				return false;
			Integer splitValue = parseInt(tokens[i + 1]);
			if (splitValue == null)
				return false;
			axisInfo.splitValue = splitValue;
			if ((axisInfo.axis = axisByLabel(tokens[i + 2])) < 0 || (axisInfo.highAxis = axisByLabel(tokens[i + 3])) < 0)
				return false;
			i += 4;
		} else {
			if ((axisInfo.axis = axisByLabel(tokens[i++])) < 0)
				return false;
		}
		while (i < tokens.length) {
			if (!tokens[i].equals("flat") || i + 1 >= tokens.length)
				return false;
			Integer flatOverride = parseInt(tokens[i + 1]);
			if (flatOverride == null)
				return false;
			axisInfo.flatOverride = flatOverride;
			i += 2;
		}
		axes.put(scanCode, axisInfo);
		return true;
	}

	private static boolean parseLed(String[] tokens) {
		int i = 1;
		if (i < tokens.length && tokens[i].equals("usage"))
			i++;
		return i + 2 == tokens.length && parseInt(tokens[i]) != null && LED_LABELS.contains(tokens[i + 1]);
	}

	/* InputEventLookup::getKeyCodeByLabel(); no key layout of AOSP maps to UNKNOWN, which stands for "no label" here */
	private static int keyCodeByLabel(String label) {
		return KeyEvent.keyCodeFromString("KEYCODE_" + label);
	}

	/* InputEventLookup::getAxisByLabel() */
	private static int axisByLabel(String label) {
		return MotionEvent.axisFromString("AXIS_" + label);
	}

	/* parseInt() of KeyLayoutMap.cpp: strtol() with base 0, the number at the start of the token */
	static Integer parseInt(String token) {
		int i = 0;
		boolean negative = false;
		if (i < token.length() && (token.charAt(i) == '-' || token.charAt(i) == '+'))
			negative = token.charAt(i++) == '-';
		int radix = 10;
		if (token.startsWith("0x", i) || token.startsWith("0X", i)) {
			radix = 16;
			i += 2;
		} else if (token.startsWith("0", i) && token.length() > i + 1) {
			radix = 8;
			i++;
		}
		int start = i;
		while (i < token.length() && Character.digit(token.charAt(i), radix) >= 0)
			i++;
		if (i == start)
			return radix == 8 ? Integer.valueOf(0) : null;
		try {
			long value = Long.parseLong(token.substring(start, i), radix);
			value = negative ? -value : value;
			if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
				return null;
			return (int)value;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/* KeyLayoutMap::mapKey(), getKey(): the usage first, then the scan code */
	Key getKey(int scanCode, int usageCode) {
		if (usageCode != 0) {
			Key key = keysByUsageCode.get(usageCode);
			if (key != null)
				return key;
		}
		if (scanCode != 0)
			return keysByScanCode.get(scanCode);
		return null;
	}

	/* KeyLayoutMap::findScanCodesForKey(), findUsageCodesForKey() */
	List<Integer> findScanCodesForKey(int keyCode) {
		return findCodes(keysByScanCode, keyCode);
	}

	List<Integer> findUsageCodesForKey(int keyCode) {
		return findCodes(keysByUsageCode, keyCode);
	}

	private static List<Integer> findCodes(SparseArray<Key> map, int keyCode) {
		List<Integer> codes = new ArrayList<>();
		for (int i = 0; i < map.size(); i++)
			if (map.valueAt(i).keyCode == keyCode)
				codes.add(map.keyAt(i));
		return codes;
	}

	AxisInfo mapAxis(int scanCode) {
		return axes.get(scanCode);
	}
}
