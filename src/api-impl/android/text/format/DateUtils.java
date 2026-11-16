package android.text.format;

import android.content.Context;
import android.icu.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
	public static final long SECOND_IN_MILLIS = 1000;
	public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
	public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
	public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
	public static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;
	public static final long YEAR_IN_MILLIS = WEEK_IN_MILLIS * 52;
	public static final int FORMAT_SHOW_TIME = 0x00001;
	public static final int FORMAT_SHOW_WEEKDAY = 0x00002;
	public static final int FORMAT_SHOW_YEAR = 0x00004;
	public static final int FORMAT_NO_YEAR = 0x00008;
	public static final int FORMAT_SHOW_DATE = 0x00010;
	public static final int FORMAT_NO_MONTH_DAY = 0x00020;
	public static final int FORMAT_12HOUR = 0x00040;
	public static final int FORMAT_24HOUR = 0x00080;
	public static final int FORMAT_CAP_AMPM = 0x00100;
	public static final int FORMAT_NO_NOON = 0x00200;
	public static final int FORMAT_CAP_NOON = 0x00400;
	public static final int FORMAT_NO_MIDNIGHT = 0x00800;
	public static final int FORMAT_CAP_MIDNIGHT = 0x01000;
	public static final int FORMAT_UTC = 0x02000;
	public static final int FORMAT_ABBREV_TIME = 0x04000;
	public static final int FORMAT_ABBREV_WEEKDAY = 0x08000;
	public static final int FORMAT_ABBREV_MONTH = 0x10000;
	public static final int FORMAT_NUMERIC_DATE = 0x20000;
	public static final int FORMAT_ABBREV_RELATIVE = 0x40000;
	public static final int FORMAT_ABBREV_ALL = 0x80000;
	public static final int FORMAT_CAP_NOON_MIDNIGHT = (FORMAT_CAP_NOON | FORMAT_CAP_MIDNIGHT);
	public static final int FORMAT_NO_NOON_MIDNIGHT = (FORMAT_NO_NOON | FORMAT_NO_MIDNIGHT);

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

	/* AOSP DateUtils.formatDateRange() family: the work is done by DateIntervalFormat (ICU) */
	public static String formatDateTime(Context context, long millis, int flags) {
		return formatDateRange(context, millis, millis, flags);
	}

	public static String formatDateRange(Context context, long startMillis, long endMillis, int flags) {
		Formatter f = new Formatter(new StringBuilder(50), Locale.getDefault());
		return formatDateRange(context, f, startMillis, endMillis, flags).toString();
	}

	public static Formatter formatDateRange(Context context, Formatter formatter, long startMillis, long endMillis, int flags) {
		return formatDateRange(context, formatter, startMillis, endMillis, flags, null);
	}

	public static Formatter formatDateRange(Context context, Formatter formatter, long startMillis, long endMillis, int flags, String timeZone) {
		// If we're being asked to format a time without being explicitly told whether to use
		// the 12- or 24-hour clock, icu4c will fall back to the locale's preferred 12/24 format,
		// but we want to fall back to the user's preference.
		if ((flags & (FORMAT_SHOW_TIME | FORMAT_12HOUR | FORMAT_24HOUR)) == FORMAT_SHOW_TIME) {
			flags |= DateFormat.is24HourFormat(context) ? FORMAT_24HOUR : FORMAT_12HOUR;
		}

		String range = DateIntervalFormat.formatDateRange(startMillis, endMillis, flags, timeZone);
		try {
			formatter.out().append(range);
		} catch (IOException impossible) {
			throw new AssertionError(impossible);
		}
		return formatter;
	}

	/* AOSP DateUtils.getRelativeTimeSpanString() family: RelativeDateTimeFormatter (ICU) */
	public static CharSequence getRelativeTimeSpanString(long startTime) {
		return getRelativeTimeSpanString(startTime, System.currentTimeMillis(), MINUTE_IN_MILLIS);
	}

	public static CharSequence getRelativeTimeSpanString(long time, long now, long minResolution) {
		int flags = FORMAT_SHOW_DATE | FORMAT_SHOW_YEAR | FORMAT_ABBREV_MONTH;
		return getRelativeTimeSpanString(time, now, minResolution, flags);
	}

	public static CharSequence getRelativeTimeSpanString(long time, long now, long minResolution, int flags) {
		return RelativeDateTimeFormatter.getRelativeTimeSpanString(Locale.getDefault(), TimeZone.getDefault(), time, now, minResolution, flags);
	}

	public static CharSequence getRelativeDateTimeString(Context c, long time, long minResolution, long transitionResolution, int flags) {
		// Same reason as in formatDateRange() to explicitly indicate 12- or 24-hour format.
		if ((flags & (FORMAT_SHOW_TIME | FORMAT_12HOUR | FORMAT_24HOUR)) == FORMAT_SHOW_TIME) {
			flags |= DateFormat.is24HourFormat(c) ? FORMAT_24HOUR : FORMAT_12HOUR;
		}

		return RelativeDateTimeFormatter.getRelativeDateTimeString(Locale.getDefault(), TimeZone.getDefault(), time, System.currentTimeMillis(), minResolution, transitionResolution, flags);
	}

	public static CharSequence getRelativeTimeSpanString(Context c, long millis, boolean withPreposition) {
		String result;
		long now = System.currentTimeMillis();
		long span = Math.abs(now - millis);

		synchronized (DateUtils.class) {
			if (sNowTime == null) {
				sNowTime = new Time();
			}

			if (sThenTime == null) {
				sThenTime = new Time();
			}

			sNowTime.set(now);
			sThenTime.set(millis);

			int prepositionId;
			if (span < DAY_IN_MILLIS && sNowTime.weekDay == sThenTime.weekDay) {
				// Same day
				int flags = FORMAT_SHOW_TIME;
				result = formatDateRange(c, millis, millis, flags);
				prepositionId = com.android.internal.R.string.preposition_for_time;
			} else if (sNowTime.year != sThenTime.year) {
				// Different years
				int flags = FORMAT_SHOW_DATE | FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE;
				result = formatDateRange(c, millis, millis, flags);

				// This is a date (like "10/31/2008" so use the date preposition)
				prepositionId = com.android.internal.R.string.preposition_for_date;
			} else {
				// Default
				int flags = FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH;
				result = formatDateRange(c, millis, millis, flags);
				prepositionId = com.android.internal.R.string.preposition_for_date;
			}
			if (withPreposition) {
				result = c.getResources().getString(prepositionId, result);
			}
		}
		return result;
	}

	public static CharSequence getRelativeTimeSpanString(Context c, long millis) {
		return getRelativeTimeSpanString(c, millis, false /* no preposition */);
	}

	private static Time sNowTime;
	private static Time sThenTime;
}
