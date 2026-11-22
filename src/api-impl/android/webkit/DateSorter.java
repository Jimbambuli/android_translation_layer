package android.webkit;

import android.content.Context;
import android.content.res.Resources;
import android.icu.text.BreakIterator;
import android.icu.text.CaseMap;
import android.icu.text.MessageFormat;
import android.icu.text.RelativeDateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/* AOSP frameworks/base core/java/android/webkit/DateSorter.java (Android 16). The labels "today"/"yesterday" come
 * from ICU's fields/day/relative data as in libcore's DateSorterBridge (title-cased the same way); that bridge
 * is not part of ATL's ICU, the public RelativeDateTimeFormatter reads the same data. The other labels are the
 * framework strings last_num_days (PluralsMessageFormatter), last_month and older. */
public class DateSorter {
	private static final String LOGTAG = "webkit";

	/** must be >= 3 */
	public static final int DAY_COUNT = 5;

	private long[] mBins = new long[DAY_COUNT - 1];
	private String[] mLabels = new String[DAY_COUNT];

	private static final int NUM_DAYS_AGO = 7;

	public DateSorter(Context context) {
		Resources resources = context.getResources();

		Calendar c = Calendar.getInstance();
		beginningOfDay(c);

		// Create the bins
		mBins[0] = c.getTimeInMillis(); // Today
		c.add(Calendar.DAY_OF_YEAR, -1);
		mBins[1] = c.getTimeInMillis(); // Yesterday
		c.add(Calendar.DAY_OF_YEAR, -(NUM_DAYS_AGO - 1));
		mBins[2] = c.getTimeInMillis(); // Five days ago
		c.add(Calendar.DAY_OF_YEAR, NUM_DAYS_AGO); // move back to today
		c.add(Calendar.MONTH, -1);
		mBins[3] = c.getTimeInMillis(); // One month ago

		// build labels
		Locale locale = resources.getConfiguration().locale;
		if (locale == null) {
			locale = Locale.getDefault();
		}
		RelativeDateTimeFormatter relative = RelativeDateTimeFormatter.getInstance(locale);
		BreakIterator breakIterator = BreakIterator.getSentenceInstance(locale);
		CaseMap.Title caseMap = CaseMap.toTitle().noLowercase().noBreakAdjustment();
		mLabels[0] = caseMap.apply(locale, breakIterator, relative.format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.DAY));
		mLabels[1] = caseMap.apply(locale, breakIterator, relative.format(RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.AbsoluteUnit.DAY));
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("count", NUM_DAYS_AGO);
		mLabels[2] = new MessageFormat(resources.getString(com.android.internal.R.string.last_num_days)).format(arguments);
		mLabels[3] = context.getString(com.android.internal.R.string.last_month);
		mLabels[4] = context.getString(com.android.internal.R.string.older);
	}

	public int getIndex(long time) {
		int lastDay = DAY_COUNT - 1;
		for (int i = 0; i < lastDay; i++) {
			if (time > mBins[i])
				return i;
		}
		return lastDay;
	}

	public String getLabel(int index) {
		if (index < 0 || index >= DAY_COUNT)
			return "";
		return mLabels[index];
	}

	public long getBoundary(int index) {
		int lastDay = DAY_COUNT - 1;
		// Error case
		if (index < 0 || index > lastDay)
			index = 0;
		// Since this provides a lower boundary on dates that will be included
		// in the given bin, provide the smallest value
		if (index == lastDay)
			return Long.MIN_VALUE;
		return mBins[index];
	}

	private void beginningOfDay(Calendar c) {
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
	}
}
