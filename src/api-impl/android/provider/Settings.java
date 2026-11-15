package android.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.AndroidException;
import android.util.Slog;

public class Settings {
	public static class NameValueTable implements BaseColumns {}

	public static final class Secure extends NameValueTable {

		/* AOSP getLong(): the value as long, SettingNotFoundException when the setting does not exist */
		public static long getLong(ContentResolver cr, String key) throws SettingNotFoundException {
			Integer output = getIntOrNull(cr, key);
			if (output == null)
				throw new SettingNotFoundException(key);
			return output.longValue();
		}

		public static long getLong(ContentResolver cr, String key, long def) {
			Integer output = getIntOrNull(cr, key);
			return output != null ? output.longValue() : def;
		}

		/* AOSP putInt() -> putString(): the write needs android.permission.WRITE_SECURE_SETTINGS, which an app does not hold here (ATL has no
		 * settings provider to write to): SecurityException as for an app without it. The message text is not from
		 * the SettingsProvider source (not available). */
		public static boolean putInt(ContentResolver cr, String key, int value) {
			return putString(cr, key, Integer.toString(value));
		}

		public static boolean putString(ContentResolver cr, String key, String value) {
			throw new SecurityException("Permission denial: writing to settings requires " + "android.permission.WRITE_SECURE_SETTINGS");
		}
		private final static String TAG = "Settings$Secure";
		public static final Uri CONTENT_URI = Uri.parse("content://settings/secure");

		public static Uri getUriFor(String name) {
			return Uri.withAppendedPath(CONTENT_URI, name);
		}

		public static String getString(ContentResolver content_resolver, String key) {
			switch (key) {
				case "android_id":
					return "_totally_an_androidID"; // TODO: is this a good ID? :P
				case "advertising_id":
					return "";
				default:
					Slog.w(TAG, "!!!! getString: unknown key: >" + key + "<");
					return "NOTICEME";
			}
		}

		protected static Integer getIntOrNull(ContentResolver content_resolver, String key) {
			switch (key) {
				case "limit_ad_tracking":
					return 1; // obviously, duh
				case "user_setup_complete":
					return 1;
				default:
					Slog.w(TAG, "!!!! getInt: unknown key: >" + key + "<");
					return null;
			}
		}

		public static int getInt(ContentResolver content_resolver, String key) {
			Integer output = getIntOrNull(content_resolver, key);
			if (output != null) {
				return output.intValue();
			} else {
				return -1; // NOTE: should actually throw a Settings$SettingNotFoundException
			}
		}

		public static int getInt(ContentResolver content_resolver, String key, int def) {
			Integer output = getIntOrNull(content_resolver, key);
			if (output != null) {
				return output.intValue();
			} else {
				return def;
			}
		}

		protected static Float getFloatOrNull(ContentResolver cr, String key) {
			switch (key) {
				default:
					Slog.w(TAG, "!!!! getFloat: unknown key: >" + key + "<");
					return null;
			}
		}

		public static float getFloat(ContentResolver cr, String key) {
			Float output = getFloatOrNull(cr, key);
			if (output != null) {
				return output.floatValue();
			} else {
				return 0.0f; // NOTE: should actually throw a Settings$SettingNotFoundException
			}
		}

		public static float getFloat(ContentResolver cr, String key, float def) {
			Float output = getFloatOrNull(cr, key);
			if (output != null) {
				return output.floatValue();
			} else {
				return def;
			}
		}
	}

	public static final class System extends NameValueTable {

		/* AOSP getLong(): the value as long, SettingNotFoundException when the setting does not exist */
		public static long getLong(ContentResolver cr, String key) throws SettingNotFoundException {
			Integer output = getIntOrNull(cr, key);
			if (output == null)
				throw new SettingNotFoundException(key);
			return output.longValue();
		}

		public static long getLong(ContentResolver cr, String key, long def) {
			Integer output = getIntOrNull(cr, key);
			return output != null ? output.longValue() : def;
		}

		/* AOSP putInt() -> putString(): the write needs android.permission.WRITE_SETTINGS, which an app does not hold here (ATL has no
		 * settings provider to write to): SecurityException as for an app without it. The message text is not from
		 * the SettingsProvider source (not available). */
		public static boolean putInt(ContentResolver cr, String key, int value) {
			return putString(cr, key, Integer.toString(value));
		}

		public static boolean putString(ContentResolver cr, String key, String value) {
			throw new SecurityException("Permission denial: writing to settings requires " + "android.permission.WRITE_SETTINGS");
		}
		/* AOSP: null when the setting is not set, which none is here */
		public static String getString(ContentResolver cr, String key) {
			return null;
		}

		private final static String TAG = "Settings$System";

		/* AOSP Settings.System.canWrite -> isCallingPackageAllowedToWriteSettings: true only once the user
		 * granted WRITE_SETTINGS (app op). ATL has no such grant and no Settings.System.put*, so the app
		 * sees the state of a fresh install: not allowed. */
		public static boolean canWrite(Context context) {
			return false;
		}
		public static final Uri CONTENT_URI = Uri.parse("content://settings/system");

		public static final Uri DEFAULT_NOTIFICATION_URI = getUriFor("notification_sound");

		public static final Uri DEFAULT_RINGTONE_URI = getUriFor("ringtone");
		public static final String ALARM_ALERT = "alarm_alert";
		public static final Uri DEFAULT_ALARM_ALERT_URI = getUriFor(ALARM_ALERT);

		public static Uri getUriFor(String name) {
			return Uri.withAppendedPath(CONTENT_URI, name);
		}

		protected static Integer getIntOrNull(ContentResolver cr, String key) {
			switch (key) {
				case "accelerometer_rotation":
					return 0; // degrees? no clue
				case "always_finish_activities":
					return 0; // we certainly don't aggressively kill activities :P
				default:
					Slog.w(TAG, "!!!! getInt: unknown key: >" + key + "<");
					return null;
			}
		}

		public static int getInt(ContentResolver content_resolver, String key) {
			Integer output = getIntOrNull(content_resolver, key);
			if (output != null) {
				return output.intValue();
			} else {
				return -1; // NOTE: should actually throw a Settings$SettingNotFoundException
			}
		}

		public static int getInt(ContentResolver content_resolver, String key, int def) {
			Integer output = getIntOrNull(content_resolver, key);
			if (output != null) {
				return output.intValue();
			} else {
				return def;
			}
		}

		protected static Float getFloatOrNull(ContentResolver cr, String key) {
			switch (key) {
				case "font_scale":
					return 1f;
				default:
					Slog.w(TAG, "!!!! getFloat: unknown key: >" + key + "<");
					return null;
			}
		}

		public static float getFloat(ContentResolver cr, String key) {
			Float output = getFloatOrNull(cr, key);
			if (output != null) {
				return output.floatValue();
			} else {
				return 0.0f; // NOTE: should actually throw a Settings$SettingNotFoundException
			}
		}

		public static float getFloat(ContentResolver cr, String key, float def) {
			Float output = getFloatOrNull(cr, key);
			if (output != null) {
				return output.floatValue();
			} else {
				return def;
			}
		}
	}

	public static final class Global extends NameValueTable {

		/* AOSP getLong(): the value as long, SettingNotFoundException when the setting does not exist */
		public static long getLong(ContentResolver cr, String key) throws SettingNotFoundException {
			Integer output = getIntOrNull(cr, key);
			if (output == null)
				throw new SettingNotFoundException(key);
			return output.longValue();
		}

		public static long getLong(ContentResolver cr, String key, long def) {
			Integer output = getIntOrNull(cr, key);
			return output != null ? output.longValue() : def;
		}

		/* AOSP putInt() -> putString(): the write needs android.permission.WRITE_SECURE_SETTINGS, which an app does not hold here (ATL has no
		 * settings provider to write to): SecurityException as for an app without it. The message text is not from
		 * the SettingsProvider source (not available). */
		public static boolean putInt(ContentResolver cr, String key, int value) {
			return putString(cr, key, Integer.toString(value));
		}

		public static boolean putString(ContentResolver cr, String key, String value) {
			throw new SecurityException("Permission denial: writing to settings requires " + "android.permission.WRITE_SECURE_SETTINGS");
		}
		private final static String TAG = "Settings$Global";
		public static final Uri CONTENT_URI = Uri.parse("content://settings/global");
		public static final String ADB_WIFI_ENABLED = "adb_wifi_enabled";

		public static Uri getUriFor(String name) {
			return Uri.withAppendedPath(CONTENT_URI, name);
		}

		public static String getString(ContentResolver cr, String key) {
			switch (key) {
				default:
					Slog.w(TAG, "!!!! getString: unknown key: >" + key + "<");
					return "STRING_FROM_SETTINGS_GLOBAL_WITH_KEY_" + key;
			}
		}

		protected static Integer getIntOrNull(ContentResolver content_resolver, String key) {
			switch (key) {
				default:
					Slog.w(TAG, "!!!! getInt: unknown key: >" + key + "<");
					return null;
			}
		}

		public static int getInt(ContentResolver content_resolver, String key) {
			Integer output = getIntOrNull(content_resolver, key);
			if (output != null) {
				return output.intValue();
			} else {
				return -1; // NOTE: should actually throw a Settings$SettingNotFoundException
			}
		}

		public static int getInt(ContentResolver content_resolver, String key, int def) {
			Integer output = getIntOrNull(content_resolver, key);
			if (output != null) {
				return output.intValue();
			} else {
				return def;
			}
		}

		protected static Float getFloatOrNull(ContentResolver cr, String key) {
			switch (key) {
				case "animator_duration_scale":
					return 1.f;
				default:
					Slog.w(TAG, "!!!! getFloat: unknown key: >" + key + "<");
					return null;
			}
		}

		public static float getFloat(ContentResolver cr, String key) {
			Float output = getFloatOrNull(cr, key);
			if (output != null) {
				return output.floatValue();
			} else {
				return 0.0f; // NOTE: should actually throw a Settings$SettingNotFoundException
			}
		}

		public static float getFloat(ContentResolver cr, String key, float def) {
			Float output = getFloatOrNull(cr, key);
			if (output != null) {
				return output.floatValue();
			} else {
				return def;
			}
		}
	}

	public static class SettingNotFoundException extends AndroidException {
		public SettingNotFoundException() {}

		public SettingNotFoundException(String msg) {
			super(msg);
		}
	}

	public static boolean canDrawOverlays(Context context) {
		return true;
	}

	/* AOSP Settings.ACTION_APPLICATION_DETAILS_SETTINGS */
	public static final String ACTION_APPLICATION_DETAILS_SETTINGS = "android.settings.APPLICATION_DETAILS_SETTINGS";
}
