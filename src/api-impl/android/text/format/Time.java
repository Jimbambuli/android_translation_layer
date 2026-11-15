package android.text.format;

import java.util.Calendar;
import java.util.GregorianCalendar;
import android.util.TimeFormatException;
import java.util.TimeZone;

public class Time {

	public static final int EPOCH_JULIAN_DAY = 2440588;

	public boolean allDay;
	public int second;
	public int minute;
	public int hour;
	public int monthDay;
	public int month;
	public int year;
	public int weekDay;
	public int yearDay;
	public int isDst;
	public long gmtoff = 0;
	public String timezone = getCurrentTimezone();

	// days from the closest Thursday, indexed by weekDay (AOSP sThursdayOffset)
	private static final int[] sThursdayOffset = {-3, 3, 2, 1, 0, -1, -2};

	public Time() {}

	public Time(String timezoneId) {
		if (timezoneId == null)
			throw new NullPointerException("timezoneId is null!");
		timezone = timezoneId;
	}

	public Time(Time other) {
		set(other);
	}

	public void setToNow() {
		set(System.currentTimeMillis());
	}

	public void set(Time that) {
		timezone = that.timezone;
		allDay = that.allDay;
		second = that.second;
		minute = that.minute;
		hour = that.hour;
		monthDay = that.monthDay;
		month = that.month;
		year = that.year;
		weekDay = that.weekDay;
		yearDay = that.yearDay;
		isDst = that.isDst;
		gmtoff = that.gmtoff;
	}

	public void set(int second, int minute, int hour, int monthDay, int month, int year) {
		allDay = false;
		this.second = second;
		this.minute = minute;
		this.hour = hour;
		this.monthDay = monthDay;
		this.month = month;
		this.year = year;
		weekDay = 0;
		yearDay = 0;
		isDst = -1;
		gmtoff = 0;
	}

	/* AOSP: a date without a time of day (allDay) */
	public void set(int monthDay, int month, int year) {
		set(0, 0, 0, monthDay, month, year);
		allDay = true;
	}

	/* AOSP: the same moment, seen in another time zone */
	public void switchTimezone(String timezone) {
		long millis = toMillis(true);
		this.timezone = timezone;
		set(millis);
	}

	public static int compare(Time a, Time b) {
		if (a == null || b == null)
			throw new NullPointerException("a or b is null");
		return Long.compare(a.toMillis(true), b.toMillis(true));
	}

	public boolean after(Time that) {
		return compare(this, that) > 0;
	}

	public boolean before(Time that) {
		return compare(this, that) < 0;
	}

	public static String getCurrentTimezone() {
		return TimeZone.getDefault().getID();
	}

	/* AOSP delegates to a ZoneInfo-based calculator; java.util.GregorianCalendar gives the same result */
	/* AOSP Time.clear(): all fields 0, isDst unknown, the given zone */
	public void clear(String timezoneId) {
		if (timezoneId == null) {
			throw new NullPointerException("timezone is null!");
		}
		this.timezone = timezoneId;
		this.allDay = false;
		this.second = 0;
		this.minute = 0;
		this.hour = 0;
		this.monthDay = 0;
		this.month = 0;
		this.year = 0;
		this.weekDay = 0;
		this.yearDay = 0;
		this.gmtoff = 0;
		this.isDst = -1;
	}

	public void set(long millis) {
		GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone(timezone));
		c.setTimeInMillis(millis);
		copyFrom(c);
	}

	public long normalize(boolean ignoreDst) {
		long millis = toMillis(ignoreDst);
		if (millis != -1)
			set(millis);
		return millis;
	}

	public long toMillis(boolean ignoreDst) {
		GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone(timezone));
		c.clear();
		c.set(year, month, monthDay, hour, minute, second);
		return c.getTimeInMillis();
	}

	public static int getJulianDay(long millis, long gmtoffSeconds) {
		long offsetMillis = gmtoffSeconds * 1000;
		long adjustedMillis = millis + offsetMillis;
		long julianDay = adjustedMillis / DateUtils.DAY_IN_MILLIS;
		// Negative adjustedMillis values must round towards Integer.MIN_VALUE.
		if (adjustedMillis < 0 && adjustedMillis % DateUtils.DAY_IN_MILLIS != 0) {
			julianDay--;
		}
		return (int) (julianDay + EPOCH_JULIAN_DAY);
	}

	public int getWeekNumber() {
		// Get the year day for the closest Thursday
		int closestThursday = yearDay + sThursdayOffset[weekDay];

		// Year days start at 0
		if (closestThursday >= 0 && closestThursday <= 364) {
			return closestThursday / 7 + 1;
		}

		// The week crosses a year boundary.
		Time temp = new Time();
		temp.timezone = timezone;
		temp.year = year;
		temp.month = month;
		temp.monthDay = monthDay + sThursdayOffset[weekDay];
		temp.hour = hour;
		temp.minute = minute;
		temp.second = second;
		temp.normalize(true);
		return temp.yearDay / 7 + 1;
	}

	/* Added from AOSP core/java/android/text/format/Time.java: parse, parse3339, format2445, format3339.
	 * AOSP's format2445()/format3339() format the fields through TimeCalculator/format(); here directly. */
	public static final String TIMEZONE_UTC = "UTC";

	public boolean parse(String s) {
		if (s == null) {
			throw new NullPointerException("time string is null");
		}
		if (parseInternal(s)) {
			timezone = TIMEZONE_UTC;
			return true;
		}
		return false;
	}

	/**
	 * Parse a time in the current zone in YYYYMMDDTHHMMSS format.
	 */
	private boolean parseInternal(String s) {
		int len = s.length();
		if (len < 8) {
			throw new TimeFormatException("String is too short: \"" + s +
					"\" Expected at least 8 characters.");
		}

		boolean inUtc = false;

		// year
		int n = getChar(s, 0, 1000);
		n += getChar(s, 1, 100);
		n += getChar(s, 2, 10);
		n += getChar(s, 3, 1);
		year = n;

		// month
		n = getChar(s, 4, 10);
		n += getChar(s, 5, 1);
		n--;
		month = n;

		// day of month
		n = getChar(s, 6, 10);
		n += getChar(s, 7, 1);
		monthDay = n;

		if (len > 8) {
			if (len < 15) {
				throw new TimeFormatException(
						"String is too short: \"" + s
								+ "\" If there are more than 8 characters there must be at least"
								+ " 15.");
			}
			checkChar(s, 8, 'T');
			allDay = false;

			// hour
			n = getChar(s, 9, 10);
			n += getChar(s, 10, 1);
			hour = n;

			// min
			n = getChar(s, 11, 10);
			n += getChar(s, 12, 1);
			minute = n;

			// sec
			n = getChar(s, 13, 10);
			n += getChar(s, 14, 1);
			second = n;

			if (len > 15) {
				// Z
				checkChar(s, 15, 'Z');
				inUtc = true;
			}
		} else {
			allDay = true;
			hour = 0;
			minute = 0;
			second = 0;
		}

		weekDay = 0;
		yearDay = 0;
		isDst = -1;
		gmtoff = 0;
		return inUtc;
	}

	private void checkChar(String s, int spos, char expected) {
		char c = s.charAt(spos);
		if (c != expected) {
			throw new TimeFormatException(String.format(
					"Unexpected character 0x%02d at pos=%d.  Expected 0x%02d (\'%c\').",
					(int) c, spos, (int) expected, expected));
		}
	}

	private static int getChar(String s, int spos, int mul) {
		char c = s.charAt(spos);
		if (Character.isDigit(c)) {
			return Character.getNumericValue(c) * mul;
		} else {
			throw new TimeFormatException("Parse error at pos=" + spos);
		}
	}


	public boolean parse3339(String s) {
		if (s == null) {
		   throw new NullPointerException("time string is null");
		}
		if (parse3339Internal(s)) {
		   timezone = TIMEZONE_UTC;
		   return true;
		}
		return false;
	}

	private boolean parse3339Internal(String s) {
		int len = s.length();
		if (len < 10) {
		   throw new TimeFormatException("String too short --- expected at least 10 characters.");
		}
		boolean inUtc = false;

		// year
		int n = getChar(s, 0, 1000);
		n += getChar(s, 1, 100);
		n += getChar(s, 2, 10);
		n += getChar(s, 3, 1);
		year = n;

		checkChar(s, 4, '-');

		// month
		n = getChar(s, 5, 10);
		n += getChar(s, 6, 1);
		--n;
		month = n;

		checkChar(s, 7, '-');

		// day
		n = getChar(s, 8, 10);
		n += getChar(s, 9, 1);
		monthDay = n;

		if (len >= 19) {
		   // T
		   checkChar(s, 10, 'T');
		   allDay = false;

		   // hour
		   n = getChar(s, 11, 10);
		   n += getChar(s, 12, 1);

		   // Note that this.hour is not set here. It is set later.
		   int hour = n;

		   checkChar(s, 13, ':');

		   // minute
		   n = getChar(s, 14, 10);
		   n += getChar(s, 15, 1);
		   // Note that this.minute is not set here. It is set later.
		   int minute = n;

		   checkChar(s, 16, ':');

		   // second
		   n = getChar(s, 17, 10);
		   n += getChar(s, 18, 1);
		   second = n;

		   // skip the '.XYZ' -- we don't care about subsecond precision.

		   int tzIndex = 19;
		   if (tzIndex < len && s.charAt(tzIndex) == '.') {
			  do {
				 tzIndex++;
			  } while (tzIndex < len && Character.isDigit(s.charAt(tzIndex)));
		   }

		   int offset = 0;
		   if (len > tzIndex) {
			  char c = s.charAt(tzIndex);
			  // NOTE: the offset is meant to be subtracted to get from local time
			  // to UTC.  we therefore use 1 for '-' and -1 for '+'.
			  switch (c) {
				 case 'Z':
					// Zulu time -- UTC
					offset = 0;
					break;
				 case '-':
					offset = 1;
					break;
				 case '+':
					offset = -1;
					break;
				 default:
					throw new TimeFormatException(String.format(
						   "Unexpected character 0x%02d at position %d.  Expected + or -",
						   (int) c, tzIndex));
			  }
			  inUtc = true;

			  if (offset != 0) {
				 if (len < tzIndex + 6) {
					throw new TimeFormatException(
						   String.format("Unexpected length; should be %d characters",
								 tzIndex + 6));
				 }

				 // hour
				 n = getChar(s, tzIndex + 1, 10);
				 n += getChar(s, tzIndex + 2, 1);
				 n *= offset;
				 hour += n;

				 // minute
				 n = getChar(s, tzIndex + 4, 10);
				 n += getChar(s, tzIndex + 5, 1);
				 n *= offset;
				 minute += n;
			  }
		   }
		   this.hour = hour;
		   this.minute = minute;

		   if (offset != 0) {
			  normalize(false);
		   }
		} else {
		   allDay = true;
		   this.hour = 0;
		   this.minute = 0;
		   this.second = 0;
		}

		this.weekDay = 0;
		this.yearDay = 0;
		this.isDst = -1;
		this.gmtoff = 0;
		return inUtc;
	}

	public String format2445() {
		String d = String.format(java.util.Locale.US, "%04d%02d%02d", year, month + 1, monthDay);
		if (allDay)
			return d;
		d += String.format(java.util.Locale.US, "T%02d%02d%02d", hour, minute, second);
		return TIMEZONE_UTC.equals(timezone) ? d + "Z" : d;
	}

	public String format3339(boolean allDay) {
		String ymd = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, monthDay);
		if (allDay)
			return ymd;
		String base = ymd + String.format(java.util.Locale.US, "T%02d:%02d:%02d.000", hour, minute, second);
		if (TIMEZONE_UTC.equals(timezone))
			return base + "Z";
		String sign = (gmtoff < 0) ? "-" : "+";
		int offset = (int)Math.abs(gmtoff);
		int minutes = (offset % 3600) / 60;
		int hours = offset / 3600;
		return String.format(java.util.Locale.US, "%s%s%02d:%02d", base, sign, hours, minutes);
	}

	private void copyFrom(Calendar c) {
		year = c.get(Calendar.YEAR);
		month = c.get(Calendar.MONTH);
		monthDay = c.get(Calendar.DAY_OF_MONTH);
		hour = c.get(Calendar.HOUR_OF_DAY);
		minute = c.get(Calendar.MINUTE);
		second = c.get(Calendar.SECOND);
		weekDay = c.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
		yearDay = c.get(Calendar.DAY_OF_YEAR) - 1;
		isDst = c.getTimeZone().inDaylightTime(c.getTime()) ? 1 : 0;
		gmtoff = (c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET)) / 1000;
	}

	/* AOSP Time.format(String) -> TimeFormatter.format(): strftime with the locale's names; the field values are
	 * those of this Time (normalize() is not called) */
	public String format(String format) {
		java.util.Locale locale = java.util.Locale.getDefault();
		java.text.DateFormatSymbols sym = java.text.DateFormatSymbols.getInstance(locale);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < format.length(); i++) {
			char c = format.charAt(i);
			if (c != '%' || i + 1 >= format.length()) {
				sb.append(c);
				continue;
			}
			char f = format.charAt(++i);
			if ((f == 'E' || f == 'O' || f == '_' || f == '-' || f == '0' || f == '^' || f == '#') && i + 1 < format.length()) {
				if (f == 'E' || f == 'O')
					f = format.charAt(++i);
			}
			sb.append(formatField(f, sym));
		}
		return sb.toString();
	}

	private String formatField(char f, java.text.DateFormatSymbols sym) {
		int h12 = hour % 12 == 0 ? 12 : hour % 12;
		java.util.Locale us = java.util.Locale.US;
		switch (f) {
		case 'A': return sym.getWeekdays()[weekDay + 1];
		case 'a': return sym.getShortWeekdays()[weekDay + 1];
		case 'B': return sym.getMonths()[month];
		case 'b': case 'h': return sym.getShortMonths()[month];
		case 'C': return String.format(us, "%02d", year / 100);
		case 'c': return format("%a %b %e %H:%M:%S %Y");
		case 'D': return format("%m/%d/%y");
		case 'd': return String.format(us, "%02d", monthDay);
		case 'e': return String.format(us, "%2d", monthDay);
		case 'F': return format("%Y-%m-%d");
		case 'H': return String.format(us, "%02d", hour);
		case 'I': return String.format(us, "%02d", h12);
		case 'j': return String.format(us, "%03d", yearDay + 1);
		case 'k': return String.format(us, "%2d", hour);
		case 'l': return String.format(us, "%2d", h12);
		case 'M': return String.format(us, "%02d", minute);
		case 'm': return String.format(us, "%02d", month + 1);
		case 'n': return "\n";
		case 'p': return hour >= 12 ? sym.getAmPmStrings()[1] : sym.getAmPmStrings()[0];
		case 'P': return (hour >= 12 ? sym.getAmPmStrings()[1] : sym.getAmPmStrings()[0]).toLowerCase(java.util.Locale.getDefault());
		case 'R': return format("%H:%M");
		case 'r': return format("%I:%M:%S %p");
		case 'S': return String.format(us, "%02d", second);
		case 's': return Long.toString(toMillis(false) / 1000);
		case 'T': case 'X': return format("%H:%M:%S");
		case 't': return "\t";
		case 'u': return Integer.toString(weekDay == 0 ? 7 : weekDay);
		case 'w': return Integer.toString(weekDay);
		case 'x': return format("%m/%d/%y");
		case 'U': return String.format(us, "%02d", (yearDay + 7 - weekDay) / 7);
		case 'W': return String.format(us, "%02d", (yearDay + 7 - (weekDay + 6) % 7) / 7);
		case 'V': return String.format(us, "%02d", java.time.LocalDate.of(year, month + 1, monthDay).get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
		case 'G': return Integer.toString(java.time.LocalDate.of(year, month + 1, monthDay).get(java.time.temporal.IsoFields.WEEK_BASED_YEAR));
		case 'g': return String.format(us, "%02d", java.time.LocalDate.of(year, month + 1, monthDay).get(java.time.temporal.IsoFields.WEEK_BASED_YEAR) % 100);
		case 'y': return String.format(us, "%02d", year % 100);
		case 'Y': return Integer.toString(year);
		case 'Z': return isDst > 0 ? java.util.TimeZone.getTimeZone(timezone).getDisplayName(true, java.util.TimeZone.SHORT) : java.util.TimeZone.getTimeZone(timezone).getDisplayName(false, java.util.TimeZone.SHORT);
		case 'z': {
			long off = gmtoff / 60;
			return String.format(us, "%c%02d%02d", off < 0 ? '-' : '+', Math.abs(off) / 60, Math.abs(off) % 60);
		}
		case '+': return format("%a %b %e %H:%M:%S %Z %Y");
		case '%': return "%";
		default: return "%" + f;
		}
	}
}
