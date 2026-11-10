package android.text.format;

import java.util.Calendar;
import java.util.GregorianCalendar;
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

	public static String getCurrentTimezone() {
		return TimeZone.getDefault().getID();
	}

	/* AOSP delegates to a ZoneInfo-based calculator; java.util.GregorianCalendar gives the same result */
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
}
