package android.os;

import android.system.ErrnoException;
import android.system.Os;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import libcore.io.IoUtils;

public class SystemProperties {
	private static final Properties properties = new Properties();
	/* fillers of board makers for fields they leave unset (before the static block, which uses it) */
	private static final java.util.Set<String> DMI_PLACEHOLDERS = new java.util.HashSet<>(java.util.Arrays.asList(
		"to be filled by o.e.m.", "system manufacturer", "system product name", "default string", "not specified",
		"not applicable", "o.e.m.", "none"));

	static {
		String SDK_INT_str = System.getProperty("Build.VERSION.SDK_INT");
		if (SDK_INT_str == null)
			SDK_INT_str = "" + Build.VERSION_CODES.GINGERBREAD;
		properties.put("ro.build.version.sdk", SDK_INT_str);
		properties.put("ro.product.brand", "google");
		properties.put("ro.build.tags", "release-keys");
		properties.put("ro.build.type", "user");
		// AOSP sets the ABI lists from the zygotes the device runs. ATL runs the app in one process of the
		// host architecture (os.arch of this VM) and can load native code only of that ABI, so it lists that
		// one alone, like a 64-bit-only device (no ro.product.cpu.abilist32, no ro.product.cpu.abi2)
		String abi = null;
		if (System.getProperty("os.arch").equals("x86_64"))
			abi = "x86_64";
		else if (System.getProperty("os.arch").equals("aarch64"))
			abi = "arm64-v8a";
		if (abi != null) {
			properties.put("ro.product.cpu.abi", abi);
			properties.put("ro.product.cpu.abilist", abi);
			properties.put("ro.product.cpu.abilist64", abi);
		}
		// with a native bridge (ATL passes ro.dalvik.vm.native.bridge and the ABI list it makes possible),
		// the list goes on with the ABIs the bridge translates, as on an Android device with a bridge
		String nativeBridge = System.getProperty("ro.dalvik.vm.native.bridge");
		String abiList = System.getProperty("ro.product.cpu.abilist");
		if (nativeBridge != null && abiList != null) {
			properties.put("ro.dalvik.vm.native.bridge", nativeBridge);
			properties.put("ro.product.cpu.abilist", abiList);
			properties.put("ro.product.cpu.abilist64", abiList);
		}
		// a phone names itself (ro.product.manufacturer/model, set by the vendor); the device ATL runs on is
		// this computer: its firmware names it (DMI, else the device tree); unnamed stays "unknown" as in AOSP
		putIfNamed("ro.product.manufacturer", "/sys/class/dmi/id/sys_vendor", "/sys/class/dmi/id/board_vendor");
		putIfNamed("ro.product.model", "/sys/class/dmi/id/product_name", "/sys/class/dmi/id/board_name",
		           "/sys/firmware/devicetree/base/model");
		// on Android the time zone service keeps persist.sys.timezone; here it follows the host
		String timezone = hostTimeZoneId();
		if (timezone != null)
			properties.put("persist.sys.timezone", timezone);
	}

	private static void putIfNamed(String property, String... files) {
		for (String file : files) {
			String name = firmwareName(file);
			if (name != null) {
				properties.put(property, name);
				return;
			}
		}
	}

	static String firmwareName(String file) {
		String name;
		try {
			name = IoUtils.readFileAsString(file);
		} catch (IOException e) {
			return null;
		}
		name = name.trim(); // also drops the NUL that ends device tree strings
		return name.isEmpty() || DMI_PLACEHOLDERS.contains(name.toLowerCase(java.util.Locale.ROOT)) ? null : name;
	}

	private static String hostTimeZoneId() {
		String etcTimezone = null;
		try {
			etcTimezone = IoUtils.readFileAsString("/etc/timezone");
		} catch (IOException e) {
		}
		String localtimeLink = null;
		try {
			localtimeLink = Os.readlink("/etc/localtime");
		} catch (ErrnoException e) { // missing, or a copy instead of a symlink: leave it to libcore
		}
		return hostTimeZoneId(System.getenv("TZ"), etcTimezone, localtimeLink);
	}

	/* the host's zone id, found like glibc does: TZ when it names a zone file (":Area/City", "Area/City"
	 * or a zoneinfo path; POSIX rules such as "CET-1CEST" name no zone), else /etc/timezone (Debian),
	 * else the zoneinfo/Area/City target of the /etc/localtime symlink; null leaves it to libcore */
	static String hostTimeZoneId(String tz, String etcTimezone, String localtimeLink) {
		String id = zoneIdOf(tz);
		if (id != null && new File(ZONEINFO_DIR, id).isFile())
			return id;
		if (etcTimezone != null && !etcTimezone.trim().isEmpty())
			return etcTimezone.trim();
		return zoneIdOf(localtimeLink);
	}

	private static final String ZONEINFO_DIR = "/usr/share/zoneinfo";

	private static String zoneIdOf(String path) {
		if (path == null)
			return null;
		if (path.startsWith(":"))
			path = path.substring(1);
		int i = path.indexOf("zoneinfo/");
		if (i >= 0)
			path = path.substring(i + "zoneinfo/".length());
		return path.isEmpty() ? null : path;
	}

	public static String get(String prop) {
		android.util.Log.i("SystemProperties", "Grabbing String prop " + prop);
		return properties.getProperty(prop);
	}

	public static String get(String prop, String def) {
		android.util.Log.i("SystemProperties", "Grabbing String prop " + prop + ", default " + def);
		return properties.getProperty(prop, def);
	}

	public static boolean getBoolean(String prop, boolean def) {
		android.util.Log.i("SystemProperties", "Grabbing boolean prop " + prop + ", default " + def);
		String val = properties.getProperty(prop);
		return val == null ? def : Boolean.parseBoolean(val);
	}

	public static int getInt(String prop, int def) {
		android.util.Log.i("SystemProperties", "Grabbing int prop " + prop + ", default " + def);
		String val = properties.getProperty(prop);
		return val == null ? def : Integer.parseInt(val);
	}

	public static long getLong(String prop, long def) {
		android.util.Log.i("SystemProperties", "Grabbing long prop " + prop + ", default " + def);
		String val = properties.getProperty(prop);
		return val == null ? def : Long.parseLong(val);
	}
}
