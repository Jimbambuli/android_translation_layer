package android.text.format;

import android.content.Context;
import android.icu.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

public class DateUtils {
	public static final long SECOND_IN_MILLIS = 1000;
	public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
	public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
	public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
	public static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;
	public static final long YEAR_IN_MILLIS = WEEK_IN_MILLIS * 52;
	public static final int FORMAT_SHOW_YEAR = 0x00004;
	public static final int FORMAT_SHOW_DATE = 0x00010;
	public static final int FORMAT_ABBREV_MONTH = 0x10000;

	/* AOSP core/java/android/text/format/DateUtils.java: abbrev values and the deprecated name getters */
	public static final int LENGTH_LONG = 10;
	public static final int LENGTH_MEDIUM = 20;
	public static final int LENGTH_SHORT = 30;
	public static final int LENGTH_SHORTER = 40;
	public static final int LENGTH_SHORTEST = 50;

	@Deprecated
	public static String getDayOfWeekString(int dayOfWeek, int abbrev) {
		DateFormatSymbols dfs = DateFormatSymbols.getInstance();
		final int width;
		switch (abbrev) {
			case LENGTH_LONG:
				width = DateFormatSymbols.WIDE;
				break;
			case LENGTH_SHORTEST:
				width = DateFormatSymbols.NARROW;
				break;
			case LENGTH_MEDIUM:
			case LENGTH_SHORT:
			case LENGTH_SHORTER:
			default:
				width = DateFormatSymbols.ABBREVIATED;
				break;
		}
		return dfs.getWeekdays(DateFormatSymbols.FORMAT, width)[dayOfWeek];
	}

	@Deprecated
	public static String getAMPMString(int ampm) {
		String[] amPm = DateFormat.getIcuDateFormatSymbols(Locale.getDefault()).getAmPmStrings();
		return amPm[ampm - Calendar.AM];
	}

	public static CharSequence getRelativeTimeSpanString(Context c, long millis) {
		return getRelativeTimeSpanString(c, millis, false /* no preposition */);
	}

	public static CharSequence getRelativeTimeSpanString(Context context, long millis, boolean withPreposition) {
		return new Date(millis).toString();
	}

	public static CharSequence getRelativeDateTimeString(Context context, long millis, long minResolution, long transitionResolution, int flags) {
		return new Date(millis).toString();
	}

	public static boolean isToday(long millis) {
		Date d1 = new Date(millis);
		Date d2 = new Date();

		return d1.getYear() == d2.getYear() && d1.getMonth() == d2.getMonth() && d1.getDate() == d2.getDate();
	}

	/* AOSP DateUtils.formatElapsedTime(): MM:SS below an hour, H:MM:SS from an hour (no days; the strings
	 * elapsed_time_short_format_mm_ss / _h_mm_ss of AOSP's values/strings.xml) */
	public static String formatElapsedTime(long elapsedSeconds) {
		return formatElapsedTime(null, elapsedSeconds);
	}

	public static String formatElapsedTime(StringBuilder recycle, long elapsedSeconds) {
		// Break the elapsed seconds into hours, minutes, and seconds.
		long hours = 0;
		long minutes = 0;
		long seconds = 0;
		if (elapsedSeconds >= 3600) {
			hours = elapsedSeconds / 3600;
			elapsedSeconds -= hours * 3600;
		}
		if (elapsedSeconds >= 60) {
			minutes = elapsedSeconds / 60;
			elapsedSeconds -= minutes * 60;
		}
		seconds = elapsedSeconds;

		// Create a StringBuilder if we weren't given one to recycle.
		StringBuilder sb = recycle;
		if (sb == null) {
			sb = new StringBuilder(8);
		} else {
			sb.setLength(0);
		}

		Formatter f = new Formatter(sb, Locale.getDefault());
		if (hours > 0) {
			return f.format("%1$d:%2$02d:%3$02d", hours, minutes, seconds).toString();
		} else {
			return f.format("%1$02d:%2$02d", minutes, seconds).toString();
		}
	}

	public static String formatDateTime(Context context, long millis, int flags) {
		return new Date(millis).toString();
	}

	public static String formatDateRange(Context context, long fromMillis, long toMillis, int flags) {
		if (fromMillis == toMillis)
			return formatDateTime(context, fromMillis, flags);
		else
			return formatDateTime(context, fromMillis, flags) + " - " + formatDateTime(context, toMillis, flags);
	}

	public static Formatter formatDateRange(Context context, Formatter formatter, long startMillis, long endMillis, int flags) {
		formatter.format("%s", formatDateRange(context, startMillis, endMillis, flags));
		return formatter;
	}

	/* AOSP's formatDateRange(..., String timeZone) formats in that zone; ATL's formatDateRange() above is the plain
	 * formatDateTime() form, which has no zone parameter */
	public static Formatter formatDateRange(Context context, Formatter formatter, long startMillis, long endMillis, int flags, String timeZone) {
		return formatDateRange(context, formatter, startMillis, endMillis, flags);
	}

	/* AOSP DateUtils.getRelativeTimeSpanString(long) */
	public static CharSequence getRelativeTimeSpanString(long startTime) {
		return getRelativeTimeSpanString(startTime, System.currentTimeMillis(), MINUTE_IN_MILLIS);
	}

	public static CharSequence getRelativeTimeSpanString(long time, long now, long minResolution) {
		int flags = FORMAT_SHOW_DATE | FORMAT_SHOW_YEAR | FORMAT_ABBREV_MONTH;
		return getRelativeTimeSpanString(time, now, minResolution, flags);
	}

	public static CharSequence getRelativeTimeSpanString(long time, long now, long minResolutionMillis, int flags) {
		return new Date(time).toString();
	}
}
