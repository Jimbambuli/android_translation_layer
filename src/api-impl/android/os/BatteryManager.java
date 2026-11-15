package android.os;

import android.content.Intent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/* The values of AOSP's BatteryManager and the state behind ACTION_BATTERY_CHANGED, read from
 * /sys/class/power_supply like healthd does. Without a battery (desktop): present=false, status and
 * health unknown as in healthd, plugged = AC; level 100 of 100 (healthd would report 0 there, an app
 * that reads it would think the battery is empty). */
public class BatteryManager {
	public static final String EXTRA_STATUS = "status";
	public static final String EXTRA_HEALTH = "health";
	public static final String EXTRA_PRESENT = "present";
	public static final String EXTRA_LEVEL = "level";
	public static final String EXTRA_SCALE = "scale";
	public static final String EXTRA_ICON_SMALL = "icon-small";
	public static final String EXTRA_PLUGGED = "plugged";
	public static final String EXTRA_VOLTAGE = "voltage";
	public static final String EXTRA_TEMPERATURE = "temperature";
	public static final String EXTRA_TECHNOLOGY = "technology";
	public static final String EXTRA_INVALID_CHARGER = "invalid_charger";
	public static final String EXTRA_BATTERY_LOW = "battery_low";
	public static final String EXTRA_CYCLE_COUNT = "android.os.extra.CYCLE_COUNT";
	public static final String EXTRA_CHARGING_STATUS = "android.os.extra.CHARGING_STATUS";

	public static final int BATTERY_STATUS_UNKNOWN = 1;
	public static final int BATTERY_STATUS_CHARGING = 2;
	public static final int BATTERY_STATUS_DISCHARGING = 3;
	public static final int BATTERY_STATUS_NOT_CHARGING = 4;
	public static final int BATTERY_STATUS_FULL = 5;

	public static final int BATTERY_HEALTH_UNKNOWN = 1;
	public static final int BATTERY_HEALTH_GOOD = 2;
	public static final int BATTERY_HEALTH_OVERHEAT = 3;
	public static final int BATTERY_HEALTH_DEAD = 4;
	public static final int BATTERY_HEALTH_OVER_VOLTAGE = 5;
	public static final int BATTERY_HEALTH_UNSPECIFIED_FAILURE = 6;
	public static final int BATTERY_HEALTH_COLD = 7;

	public static final int BATTERY_PLUGGED_AC = 1;
	public static final int BATTERY_PLUGGED_USB = 2;
	public static final int BATTERY_PLUGGED_WIRELESS = 4;
	public static final int BATTERY_PLUGGED_ANY = BATTERY_PLUGGED_AC | BATTERY_PLUGGED_USB | BATTERY_PLUGGED_WIRELESS;

	public static final int BATTERY_PROPERTY_CHARGE_COUNTER = 1;
	public static final int BATTERY_PROPERTY_CURRENT_NOW = 2;
	public static final int BATTERY_PROPERTY_CURRENT_AVERAGE = 3;
	public static final int BATTERY_PROPERTY_CAPACITY = 4;
	public static final int BATTERY_PROPERTY_ENERGY_COUNTER = 5;
	public static final int BATTERY_PROPERTY_STATUS = 6;

	private static final File POWER_SUPPLY = new File("/sys/class/power_supply");
	private static final int LOW_LEVEL = 15; // config_lowBatteryWarningLevel of AOSP

	/* the state of the supply at this moment */
	private static final class State {
		boolean present;
		int status = BATTERY_STATUS_UNKNOWN;
		int health = BATTERY_HEALTH_UNKNOWN;
		int level = 100;
		int plugged;
		int voltage; // mV
		int temperature; // tenths of a degree Celsius
		String technology = "";
	}

	private static String read(File dir, String name) {
		try {
			return new String(Files.readAllBytes(new File(dir, name).toPath()), StandardCharsets.UTF_8).trim();
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}

	private static int readInt(File dir, String name, int def) {
		String s = read(dir, name);
		try {
			return s != null ? Integer.parseInt(s) : def;
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static State state() {
		State s = new State();
		File[] supplies = POWER_SUPPLY.listFiles();
		boolean mains = false;
		if (supplies != null) {
			for (File d : supplies) {
				String type = read(d, "type");
				if ("Battery".equals(type) && !"System".equals(read(d, "scope"))) {
					if (s.present || readInt(d, "present", 1) == 0)
						continue;
					s.present = true;
					s.level = Math.max(0, Math.min(100, readInt(d, "capacity", 100)));
					String st = read(d, "status");
					s.status = "Charging".equals(st) ? BATTERY_STATUS_CHARGING : "Discharging".equals(st) ? BATTERY_STATUS_DISCHARGING : "Not charging".equals(st) ? BATTERY_STATUS_NOT_CHARGING : "Full".equals(st) ? BATTERY_STATUS_FULL : BATTERY_STATUS_UNKNOWN;
					String h = read(d, "health");
					s.health = "Good".equals(h) ? BATTERY_HEALTH_GOOD : "Overheat".equals(h) ? BATTERY_HEALTH_OVERHEAT : "Dead".equals(h) ? BATTERY_HEALTH_DEAD : "Over voltage".equals(h) ? BATTERY_HEALTH_OVER_VOLTAGE : "Cold".equals(h) ? BATTERY_HEALTH_COLD : "Unspecified failure".equals(h) ? BATTERY_HEALTH_UNSPECIFIED_FAILURE : BATTERY_HEALTH_UNKNOWN;
					s.voltage = readInt(d, "voltage_now", 0) / 1000;
					s.temperature = readInt(d, "temp", 0);
					String tech = read(d, "technology");
					s.technology = tech != null && !"Unknown".equals(tech) ? tech : "";
				} else if ("Mains".equals(type) || "USB".equals(type) || "Wireless".equals(type) || (type != null && type.startsWith("USB"))) {
					if (readInt(d, "online", 0) != 0)
						s.plugged |= "Wireless".equals(type) ? BATTERY_PLUGGED_WIRELESS : "Mains".equals(type) ? BATTERY_PLUGGED_AC : BATTERY_PLUGGED_USB;
				}
			}
		}
		if (!s.present)
			s.plugged = BATTERY_PLUGGED_AC; // a desktop without battery runs on the mains
		return s;
	}

	/* the sticky ACTION_BATTERY_CHANGED of BatteryService.sendBatteryChangedIntentLocked() */
	public static Intent batteryChangedIntent() {
		State s = state();
		Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
		intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | Intent.FLAG_RECEIVER_REPLACE_PENDING | Intent.FLAG_RECEIVER_FOREGROUND);
		intent.putExtra(EXTRA_STATUS, s.status);
		intent.putExtra(EXTRA_HEALTH, s.health);
		intent.putExtra(EXTRA_PRESENT, s.present);
		intent.putExtra(EXTRA_LEVEL, s.level);
		intent.putExtra(EXTRA_BATTERY_LOW, s.present && s.level <= LOW_LEVEL && s.plugged == 0);
		intent.putExtra(EXTRA_SCALE, 100);
		intent.putExtra(EXTRA_ICON_SMALL, 0);
		intent.putExtra(EXTRA_PLUGGED, s.plugged);
		intent.putExtra(EXTRA_VOLTAGE, s.voltage);
		intent.putExtra(EXTRA_TEMPERATURE, s.temperature);
		intent.putExtra(EXTRA_TECHNOLOGY, s.technology);
		intent.putExtra(EXTRA_INVALID_CHARGER, 0);
		return intent;
	}

	public int getIntProperty(int id) {
		State s = state();
		switch (id) {
			case BATTERY_PROPERTY_CAPACITY:
				return s.level;
			case BATTERY_PROPERTY_STATUS:
				return s.status;
			default:
				return Integer.MIN_VALUE; // AOSP: a property the battery does not report
		}
	}

	public long getLongProperty(int id) {
		return Long.MIN_VALUE;
	}

	/* AOSP getStringProperty(): queryStringProperty() yields null for a property the battery does not report; ATL
	 * reports no string properties */
	public String getStringProperty(int id) {
		return null;
	}

	/* AOSP computeChargeTimeRemaining(): BatteryStatsImpl returns -1 when no estimate is available (ATL keeps no
	 * charge history) */
	public long computeChargeTimeRemaining() {
		return -1;
	}

	public boolean isCharging() {
		State s = state();
		return s.plugged != 0;
	}
}
