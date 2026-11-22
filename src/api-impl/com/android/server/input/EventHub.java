package com.android.server.input;

import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/* AOSP EventHub (frameworks/native/services/inputflinger/reader/EventHub.cpp) for the input devices that ATL reads
 * itself: the evdev nodes of /dev/input are opened and classified as on Android, and the game controllers among
 * them (JOYSTICK or GAMEPAD, without the classes of keyboards, mice and touch screens, which come through GTK) are
 * kept and read; the others are closed again. The kernel side (open, ioctls, inotify, reading, force feedback) is
 * in com_android_server_input_EventHub.c. Everything runs on the main thread. */
public final class EventHub {
	private static final String TAG = "EventHub";

	/* InputDeviceClass (EventHub.h) */
	static final int CLASS_KEYBOARD = 0x00000001;
	static final int CLASS_ALPHAKEY = 0x00000002;
	static final int CLASS_TOUCH = 0x00000004;
	static final int CLASS_CURSOR = 0x00000008;
	static final int CLASS_TOUCH_MT = 0x00000010;
	static final int CLASS_DPAD = 0x00000020;
	static final int CLASS_GAMEPAD = 0x00000040;
	static final int CLASS_SWITCH = 0x00000080;
	static final int CLASS_JOYSTICK = 0x00000100;
	static final int CLASS_VIBRATOR = 0x00000200;
	static final int CLASS_MIC = 0x00000400;
	static final int CLASS_EXTERNAL_STYLUS = 0x00000800;
	static final int CLASS_ROTARY_ENCODER = 0x00001000;
	static final int CLASS_SENSOR = 0x00002000;
	static final int CLASS_TOUCHPAD = 0x00010000;
	static final int CLASS_EXTERNAL = 0x80000000;

	/* linux/input-event-codes.h */
	static final int EV_SYN = 0x00, EV_KEY = 0x01, EV_ABS = 0x03, EV_MSC = 0x04;
	static final int SYN_REPORT = 0, SYN_DROPPED = 3, MSC_SCAN = 4;
	static final int BTN_MISC = 0x100, BTN_MOUSE = 0x110, BTN_JOYSTICK = 0x120, BTN_DIGI = 0x140, BTN_WHEEL = 0x150;
	static final int BTN_TOUCH = 0x14a, BTN_STYLUS = 0x14b, BTN_STYLUS2 = 0x14c, BTN_STYLUS3 = 0x149;
	static final int BTN_TOOL_PEN = 0x140, BTN_TOOL_FINGER = 0x145;
	static final int KEY_MAX = 0x2ff, ABS_MAX = 0x3f, ABS_CNT = ABS_MAX + 1;
	static final int ABS_X = 0x00, ABS_Y = 0x01, ABS_Z = 0x02, ABS_RX = 0x03, ABS_RY = 0x04, ABS_RZ = 0x05;
	static final int ABS_PRESSURE = 0x18, ABS_DISTANCE = 0x19, ABS_TILT_X = 0x1a, ABS_TILT_Y = 0x1b, ABS_TOOL_WIDTH = 0x1c;
	static final int ABS_MT_SLOT = 0x2f, ABS_MT_TOUCH_MAJOR = 0x30, ABS_MT_POSITION_X = 0x35, ABS_MT_POSITION_Y = 0x36;
	static final int ABS_MT_DISTANCE = 0x3b;
	static final int REL_X = 0x00, REL_Y = 0x01, REL_HWHEEL = 0x06, REL_WHEEL = 0x08;
	static final int FF_RUMBLE = 0x50;
	static final int INPUT_PROP_POINTER = 0x00, INPUT_PROP_ACCELEROMETER = 0x06;
	static final int BUS_USB = 0x03, BUS_BLUETOOTH = 0x05;

	/* KeyCodeClassifications.h */
	private static final int[] DPAD_REQUIRED_KEYCODES = {KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
		KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER};
	private static final int[] GAMEPAD_KEYCODES = {KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B,
		KeyEvent.KEYCODE_BUTTON_C, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_Z,
		KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
		KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR, KeyEvent.KEYCODE_BUTTON_START,
		KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_MODE};
	private static final int[] STYLUS_BUTTON_KEYCODES = {KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY,
		KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, KeyEvent.KEYCODE_STYLUS_BUTTON_TERTIARY,
		KeyEvent.KEYCODE_STYLUS_BUTTON_TAIL};

	/* InputDeviceIdentifier (include/input/InputDevice.h) */
	static final class Identifier {
		String name = "";
		String location = "";
		String uniqueId = "";
		int bus, vendor, product, version, nonce;
		String descriptor = "";

		/* InputDeviceIdentifier::getCanonicalName() */
		String getCanonicalName() {
			StringBuilder name = new StringBuilder(this.name);
			for (int i = 0; i < name.length(); i++) {
				char ch = name.charAt(i);
				if (!(ch < 128 && (Character.isLetterOrDigit(ch) || ch == '-' || ch == '_')))
					name.setCharAt(i, '_');
			}
			return name.toString();
		}
	}

	static final class Device {
		final int id;
		final String path;
		final int fd;
		final Identifier identifier;
		Map<String, String> configuration;
		final byte[][] bits = new byte[8][];
		final int[] absInfo = new int[ABS_CNT * 6];
		int classes;
		boolean haveKeyboardKeys;
		KeyLayoutMap keyLayoutMap;
		boolean haveKeyCharacterMap;
		int controllerNumber;

		Device(int id, String path, int fd, Identifier identifier) {
			this.id = id;
			this.path = path;
			this.fd = fd;
			this.identifier = identifier;
		}

		private static boolean test(byte[] bitmask, int bit) {
			return bit >= 0 && bit / 8 < bitmask.length && (bitmask[bit / 8] & (1 << (bit % 8))) != 0;
		}

		/* BitArray::any(first, last): a bit in [first, last) */
		private static boolean any(byte[] bitmask, int first, int last) {
			for (int bit = first; bit < last; bit++)
				if (test(bitmask, bit))
					return true;
			return false;
		}

		boolean hasKeyBit(int code) { return test(bits[0], code); }
		boolean hasAbsBit(int code) { return test(bits[1], code); }
		boolean hasRelBit(int code) { return test(bits[2], code); }
		boolean hasSwitches() { return any(bits[3], 0, bits[3].length * 8); }
		boolean hasFfBit(int code) { return test(bits[5], code); }
		boolean hasMscBit(int code) { return test(bits[6], code); }
		boolean hasProp(int code) { return test(bits[7], code); }

		/* EventHub::getAbsoluteAxisInfo(): {min, max, flat, fuzz, resolution}, null if the device has no such axis */
		int[] getAbsoluteAxisInfo(int axis) {
			if (axis < 0 || axis >= ABS_CNT || absInfo[axis * 6] == 0)
				return null;
			return new int[] {absInfo[axis * 6 + 1], absInfo[axis * 6 + 2], absInfo[axis * 6 + 3],
					  absInfo[axis * 6 + 4], absInfo[axis * 6 + 5]};
		}

		/* Device::hasKeycodeLocked(): a scan code or usage of the key layout maps to the key code (ATL's key
		 * character map, Generic.kcm, remaps no keys) */
		boolean hasKeycode(int keyCode) {
			if (keyLayoutMap == null)
				return false;
			for (int scanCode : keyLayoutMap.findScanCodesForKey(keyCode))
				if (scanCode >= 0 && scanCode <= KEY_MAX && hasKeyBit(scanCode))
					return true;
			return !keyLayoutMap.findUsageCodesForKey(keyCode).isEmpty() && hasMscBit(MSC_SCAN);
		}

		/* Device::isExternalDeviceLocked() */
		boolean isExternal() {
			String internal = configuration != null ? configuration.get("device.internal") : null;
			if (internal != null)
				return !parseBool(internal);
			return identifier.bus == BUS_USB || identifier.bus == BUS_BLUETOOTH;
		}
	}

	private static String sSystemUsrDir;
	private static final SparseArray<Device> sDevices = new SparseArray<>();
	private static final Map<String, Device> sDevicesByPath = new HashMap<>();
	/* InputReader's ids start after the reserved ones; ATL's own device is 0, and the pointer events of GTK carry
	 * the id 1 (MotionEvent.getDeviceId()) */
	private static int sNextDeviceId = 2;
	private static int sControllerNumbers; /* BitSet32 of the controller numbers in use */
	private static boolean sStarted;

	private EventHub() {}

	/* the directory of AOSP's /system/usr in ATL's installation (keylayout, keychars, idc) */
	public static synchronized String getSystemUsrDir() {
		if (sSystemUsrDir == null)
			sSystemUsrDir = nativeGetSystemUsrDir();
		return sSystemUsrDir;
	}

	/* EventHub(): watch /dev/input and open what is there (scanDevicesLocked()); called once, before the
	 * application starts */
	public static void start() {
		if (sStarted)
			return;
		sStarted = true;
		for (String path : nativeInit())
			openDevice(path);
	}

	/* from the inotify watch: a node was created (or got its permissions from udev) or was deleted */
	private static void onDevicePath(String path, boolean added) {
		if (added) {
			if (!sDevicesByPath.containsKey(path))
				openDevice(path);
		} else {
			Device device = sDevicesByPath.get(path);
			if (device != null)
				closeDevice(device);
		}
	}

	/* the device could not be read any more (ENODEV) */
	private static void onDeviceGone(int id) {
		Device device = sDevices.get(id);
		if (device != null)
			closeDevice(device);
	}

	/* the raw events of a device: per event the time (CLOCK_MONOTONIC, ns) and type, code, value */
	private static void onEvents(int id, long[] when, int[] values) {
		Device device = sDevices.get(id);
		if (device != null)
			InputReader.processEvents(device, when, values);
	}

	static Device getDevice(int id) {
		return sDevices.get(id);
	}

	/* EventHub::openDeviceLocked() */
	private static void openDevice(String path) {
		int fd = nativeOpen(path);
		if (fd < 0)
			return;
		String[] strings = new String[3];
		int[] ids = new int[5];
		Identifier identifier = new Identifier();
		Device device = new Device(sNextDeviceId, path, fd, identifier);
		if (!nativeGetDeviceInfo(fd, strings, ids, device.bits, device.absInfo)) {
			nativeClose(fd);
			return;
		}
		identifier.name = strings[0];
		identifier.location = strings[1] != null ? strings[1] : "";
		identifier.uniqueId = strings[2] != null ? strings[2] : "";
		identifier.bus = ids[0];
		identifier.vendor = ids[1];
		identifier.product = ids[2];
		identifier.version = ids[3];
		assignDescriptor(identifier);
		device.configuration = loadConfiguration(identifier);

		classify(device);
		if (!isGameController(device)) {
			nativeClose(fd);
			return;
		}
		if ((device.classes & (CLASS_JOYSTICK | CLASS_DPAD)) != 0 && (device.classes & CLASS_GAMEPAD) != 0)
			device.controllerNumber = getNextControllerNumber();

		sNextDeviceId++;
		sDevices.put(device.id, device);
		sDevicesByPath.put(path, device);
		Log.i(TAG, "New device: id=" + device.id + ", path='" + path + "', name='" + identifier.name
			  + "', classes=0x" + Integer.toHexString(device.classes) + ", keyLayout='"
			  + (device.keyLayoutMap != null ? device.keyLayoutMap.loadFileName : "") + "', vendor=0x"
			  + Integer.toHexString(identifier.vendor) + ", product=0x" + Integer.toHexString(identifier.product)
			  + ", descriptor=" + identifier.descriptor);
		nativeStartReading(device.id, fd);
		InputReader.addDevice(device);
	}

	/* ATL keeps the game controllers: the devices with gamepad buttons (GAMEPAD) and the joysticks without keyboard
	 * keys. The keys of keyboards, mice, touch screens and touch pads reach ATL through GTK already: a device of the
	 * desktop with such keys stays GTK's, even where AOSP's classes would make it a joystick too (the "Consumer
	 * Control" device of a keyboard receiver, with media keys, BTN_0 and ABS_VOLUME) */
	private static boolean isGameController(Device device) {
		if ((device.classes & (CLASS_ALPHAKEY | CLASS_CURSOR | CLASS_TOUCH)) != 0)
			return false;
		return (device.classes & CLASS_GAMEPAD) != 0 || ((device.classes & CLASS_JOYSTICK) != 0 && !device.haveKeyboardKeys);
	}

	/* the classes of EventHub::openDeviceLocked(), from the event bits and the key layout */
	private static void classify(Device device) {
		boolean haveKeyboardKeys = Device.any(device.bits[0], 0, BTN_MISC) || Device.any(device.bits[0], BTN_WHEEL, KEY_MAX + 1);
		device.haveKeyboardKeys = haveKeyboardKeys;
		boolean haveGamepadButtons = Device.any(device.bits[0], BTN_MISC, BTN_MOUSE) || Device.any(device.bits[0], BTN_JOYSTICK, BTN_DIGI);
		boolean haveStylusButtons = device.hasKeyBit(BTN_STYLUS) || device.hasKeyBit(BTN_STYLUS2) || device.hasKeyBit(BTN_STYLUS3);
		if (haveKeyboardKeys || haveGamepadButtons || haveStylusButtons)
			device.classes |= CLASS_KEYBOARD;

		if (device.hasKeyBit(BTN_MOUSE) && device.hasRelBit(REL_X) && device.hasRelBit(REL_Y))
			device.classes |= CLASS_CURSOR;

		String deviceType = device.configuration != null ? device.configuration.get("device.type") : null;
		if ("rotaryEncoder".equals(deviceType))
			device.classes |= CLASS_ROTARY_ENCODER;
		else if ("externalStylus".equals(deviceType))
			device.classes |= CLASS_EXTERNAL_STYLUS;

		if (device.hasAbsBit(ABS_MT_POSITION_X) && device.hasAbsBit(ABS_MT_POSITION_Y)) {
			/* some joysticks such as the PS3 controller report axes that conflict with the ABS_MT range */
			if (device.hasKeyBit(BTN_TOUCH) || !haveGamepadButtons) {
				device.classes |= CLASS_TOUCH | CLASS_TOUCH_MT;
				if (device.hasProp(INPUT_PROP_POINTER) && !Device.any(device.bits[0], BTN_TOOL_PEN, BTN_TOOL_FINGER) && !haveStylusButtons)
					device.classes |= CLASS_TOUCHPAD;
			}
		} else if (device.hasKeyBit(BTN_TOUCH) && device.hasAbsBit(ABS_X) && device.hasAbsBit(ABS_Y)) {
			device.classes |= CLASS_TOUCH;
		} else if ((device.hasAbsBit(ABS_PRESSURE) || device.hasKeyBit(BTN_TOUCH)) && !device.hasAbsBit(ABS_X) && !device.hasAbsBit(ABS_Y)) {
			device.classes |= CLASS_EXTERNAL_STYLUS;
		}

		/* joysticks always have gamepad buttons, which tells them from other devices with absolute axes */
		if (haveGamepadButtons) {
			int assumedClasses = device.classes | CLASS_JOYSTICK;
			for (int i = 0; i <= ABS_MAX; i++) {
				if (device.hasAbsBit(i) && (getAbsAxisUsage(i, assumedClasses) & CLASS_JOYSTICK) != 0) {
					device.classes = assumedClasses;
					break;
				}
			}
		}

		if (device.hasProp(INPUT_PROP_ACCELEROMETER))
			device.classes |= CLASS_SENSOR;
		if (device.hasSwitches())
			device.classes |= CLASS_SWITCH;
		if (device.hasFfBit(FF_RUMBLE))
			device.classes |= CLASS_VIBRATOR;

		if ((device.classes & (CLASS_KEYBOARD | CLASS_JOYSTICK | CLASS_SENSOR)) != 0)
			loadKeyMap(device);

		if ((device.classes & CLASS_KEYBOARD) != 0) {
			if (device.hasKeycode(KeyEvent.KEYCODE_Q))
				device.classes |= CLASS_ALPHAKEY;
			boolean dpad = true;
			for (int keyCode : DPAD_REQUIRED_KEYCODES)
				dpad &= device.hasKeycode(keyCode);
			if (dpad)
				device.classes |= CLASS_DPAD;
			for (int keyCode : GAMEPAD_KEYCODES) {
				if (device.hasKeycode(keyCode)) {
					device.classes |= CLASS_GAMEPAD;
					break;
				}
			}
			if ((device.classes & (CLASS_TOUCH | CLASS_TOUCH_MT | CLASS_ALPHAKEY)) == 0) {
				for (int keyCode : STYLUS_BUTTON_KEYCODES) {
					if (device.hasKeycode(keyCode)) {
						device.classes |= CLASS_EXTERNAL_STYLUS;
						break;
					}
				}
			}
		}

		String mic = device.configuration != null ? device.configuration.get("audio.mic") : null;
		if (mic != null && parseBool(mic))
			device.classes |= CLASS_MIC;
		if (device.isExternal())
			device.classes |= CLASS_EXTERNAL;
	}

	/* getAbsAxisUsage(): touch devices get the touch axes, sensors the motion axes, external styluses the
	 * pressure, joysticks the rest */
	static int getAbsAxisUsage(int axis, int deviceClasses) {
		if ((deviceClasses & CLASS_TOUCH) != 0) {
			if (axis == ABS_X || axis == ABS_Y || axis == ABS_PRESSURE || axis == ABS_TOOL_WIDTH || axis == ABS_DISTANCE
			    || axis == ABS_TILT_X || axis == ABS_TILT_Y || (axis >= ABS_MT_SLOT && axis <= ABS_MT_DISTANCE))
				return CLASS_TOUCH;
		}
		if ((deviceClasses & CLASS_SENSOR) != 0) {
			if (axis >= ABS_X && axis <= ABS_RZ)
				return CLASS_SENSOR;
		}
		if ((deviceClasses & CLASS_EXTERNAL_STYLUS) != 0 && axis == ABS_PRESSURE)
			return CLASS_EXTERNAL_STYLUS;
		return deviceClasses & CLASS_JOYSTICK;
	}

	/* EventHub::generateDescriptor(), assignDescriptorLocked(): SHA-1 of vendor, product and unique id (name or
	 * location for built-in devices), unique among the connected devices */
	private static void assignDescriptor(Identifier identifier) {
		identifier.nonce = 0;
		generateDescriptor(identifier);
		while (hasDeviceWithDescriptor(identifier.descriptor)) {
			identifier.nonce++;
			generateDescriptor(identifier);
		}
	}

	private static void generateDescriptor(Identifier identifier) {
		StringBuilder raw = new StringBuilder(String.format(":%04x:%04x:", identifier.vendor, identifier.product));
		if (!identifier.uniqueId.isEmpty())
			raw.append("uniqueId:").append(identifier.uniqueId);
		if (identifier.nonce != 0)
			raw.append(String.format("nonce:%04x", identifier.nonce));
		if (identifier.vendor == 0 && identifier.product == 0) {
			if (!identifier.name.isEmpty())
				raw.append("name:").append(identifier.name);
			else if (!identifier.location.isEmpty())
				raw.append("location:").append(identifier.location);
		}
		try {
			byte[] digest = MessageDigest.getInstance("SHA-1").digest(raw.toString().getBytes("UTF-8"));
			StringBuilder hex = new StringBuilder();
			for (byte b : digest)
				hex.append(String.format("%02x", b & 0xff));
			identifier.descriptor = hex.toString();
		} catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean hasDeviceWithDescriptor(String descriptor) {
		for (int i = 0; i < sDevices.size(); i++)
			if (sDevices.valueAt(i).identifier.descriptor.equals(descriptor))
				return true;
		return false;
	}

	/* getInputDeviceConfigurationFilePathByName(): ATL's installation is the one system repository */
	private static String getPathByName(String name, String type) {
		String dir = type.equals(".kl") ? "keylayout/" : type.equals(".kcm") ? "keychars/" : "idc/";
		String path = getSystemUsrDir() + dir + name + type;
		return new File(path).canRead() ? path : null;
	}

	/* getInputDeviceConfigurationFilePathByDeviceIdentifier() */
	private static String getPathByIdentifier(Identifier identifier, String type, String suffix) {
		if (identifier.vendor != 0 && identifier.product != 0) {
			if (identifier.version != 0) {
				String path = getPathByName(String.format("Vendor_%04x_Product_%04x_Version_%04x%s", identifier.vendor,
									  identifier.product, identifier.version, suffix), type);
				if (path != null)
					return path;
			}
			String path = getPathByName(String.format("Vendor_%04x_Product_%04x%s", identifier.vendor, identifier.product, suffix), type);
			if (path != null)
				return path;
		}
		return getPathByName(identifier.getCanonicalName() + suffix, type);
	}

	/* EventHub::loadConfiguration(): the .idc file of the device, a PropertyMap of "key = value" lines */
	private static Map<String, String> loadConfiguration(Identifier identifier) {
		String path = getPathByIdentifier(identifier, ".idc", "");
		if (path == null)
			return null;
		Map<String, String> properties = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#"))
					continue;
				int equals = line.indexOf('=');
				if (equals < 0)
					throw new IOException(path + ": expected '=' in '" + line + "'");
				properties.put(line.substring(0, equals).trim(), line.substring(equals + 1).trim());
			}
		} catch (IOException e) {
			Log.e(TAG, "Error loading input device configuration file for device '" + identifier.name + "': " + e.getMessage());
			return null;
		}
		return properties;
	}

	/* PropertyMap::getBool(): an integer, not 0 */
	private static boolean parseBool(String value) {
		Integer number = KeyLayoutMap.parseInt(value);
		return number != null && number != 0;
	}

	/* KeyMap::load(): a key layout named by the configuration, else by the identifier, else "Generic", else
	 * "Virtual"; the key character map likewise (ATL uses only whether there is one) */
	private static void loadKeyMap(Device device) {
		if (device.configuration != null) {
			String layout = device.configuration.get("keyboard.layout");
			if (layout != null)
				loadKeyLayout(device, layout);
			String characterMap = device.configuration.get("keyboard.characterMap");
			if (characterMap != null)
				device.haveKeyCharacterMap = getPathByName(characterMap, ".kcm") != null;
			if (device.keyLayoutMap != null && device.haveKeyCharacterMap)
				return;
		}
		for (String name : new String[] {"", "Generic", "Virtual"}) {
			if (device.keyLayoutMap == null)
				loadKeyLayout(device, name);
			if (!device.haveKeyCharacterMap)
				device.haveKeyCharacterMap = (name.isEmpty() ? getPathByIdentifier(device.identifier, ".kcm", "") : getPathByName(name, ".kcm")) != null;
			if (device.keyLayoutMap != null && device.haveKeyCharacterMap)
				return;
		}
		Log.e(TAG, "Could not determine key map for device '" + device.identifier.name + "' and no default key maps were found!");
	}

	/* KeyMap::loadKeyLayout(): the file, or else its "_fallback" variant */
	private static void loadKeyLayout(Device device, String name) {
		String path = name.isEmpty() ? getPathByIdentifier(device.identifier, ".kl", "") : getPathByName(name, ".kl");
		if (path == null)
			return;
		device.keyLayoutMap = KeyLayoutMap.load(path);
		if (device.keyLayoutMap == null) {
			String fallback = getPathByIdentifier(device.identifier, ".kl", "_fallback");
			if (fallback != null)
				device.keyLayoutMap = KeyLayoutMap.load(fallback);
		}
	}

	/* EventHub::getNextControllerNumberLocked(), releaseControllerNumberLocked(): 1 to 32, 0 when all are taken */
	private static int getNextControllerNumber() {
		for (int i = 0; i < 32; i++) {
			if ((sControllerNumbers & (1 << i)) == 0) {
				sControllerNumbers |= 1 << i;
				return i + 1;
			}
		}
		return 0;
	}

	/* EventHub::closeDeviceLocked() */
	private static void closeDevice(Device device) {
		Log.i(TAG, "Removed device: path=" + device.path + " name=" + device.identifier.name + " id=" + device.id);
		if (device.controllerNumber > 0)
			sControllerNumbers &= ~(1 << (device.controllerNumber - 1));
		sDevices.remove(device.id);
		sDevicesByPath.remove(device.path);
		nativeStopReading(device.id);
		InputReader.removeDevice(device);
	}

	/* EventHub::mapKey(): the key code of a scan code or usage, KEYCODE_UNKNOWN if the layout has none;
	 * {keyCode, policyFlags} */
	static int[] mapKey(Device device, int scanCode, int usageCode) {
		KeyLayoutMap.Key key = device.keyLayoutMap != null ? device.keyLayoutMap.getKey(scanCode, usageCode) : null;
		return key != null ? new int[] {key.keyCode, key.flags} : null;
	}

	/* EventHub::vibrate(), cancelVibrate() */
	static void vibrate(Device device, int strongMagnitude, int weakMagnitude, int durationMillis) {
		nativeVibrate(device.id, strongMagnitude, weakMagnitude, durationMillis);
	}

	static void cancelVibrate(Device device) {
		nativeCancelVibrate(device.id);
	}

	private static native String[] nativeInit();
	private static native int nativeOpen(String path);
	private static native boolean nativeGetDeviceInfo(int fd, String[] strings, int[] ids, byte[][] bits, int[] absInfo);
	private static native void nativeClose(int fd);
	private static native void nativeStartReading(int id, int fd);
	private static native void nativeStopReading(int id);
	private static native void nativeVibrate(int id, int strongMagnitude, int weakMagnitude, int durationMillis);
	private static native void nativeCancelVibrate(int id);
	private static native String nativeGetSystemUsrDir();
}
