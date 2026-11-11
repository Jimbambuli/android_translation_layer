package android.util;

import java.util.Arrays;

/* AOSP android.util.EventLog: binary events for the "events" log buffer. ATL has no logd, so the events go to
 * the log as text (tag number and values), and there is no event-log-tags file to name the tags. */
public class EventLog {
	private static final String TAG = "EventLog";

	private EventLog() {}

	private static int write(int tag, String value) {
		String line = tag + " " + value;
		Log.i(TAG, line);
		return line.length();
	}

	public static int writeEvent(int tag, int value) {
		return write(tag, String.valueOf(value));
	}

	public static int writeEvent(int tag, long value) {
		return write(tag, String.valueOf(value));
	}

	public static int writeEvent(int tag, float value) {
		return write(tag, String.valueOf(value));
	}

	public static int writeEvent(int tag, String str) {
		return write(tag, String.valueOf(str));
	}

	public static int writeEvent(int tag, Object... list) {
		return write(tag, Arrays.deepToString(list));
	}

	/* the name of an event tag, from /system/etc/event-log-tags on Android: none here */
	public static String getTagName(int tag) {
		return null;
	}

	public static int getTagCode(String name) {
		return -1;
	}
}
