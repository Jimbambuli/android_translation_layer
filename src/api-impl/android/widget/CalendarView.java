package android.widget;

import android.content.Context;
import android.util.AttributeSet;

/**
 * AOSP CalendarView (android16 core/java/android/widget/CalendarView.java) delegates to a drawing delegate
 * (CalendarViewLegacyDelegate / CalendarViewMaterialDelegate). ATL draws no calendar: the shown date and the listener are
 * kept, so apps can set and read the date; day clicks are not generated.
 */
public class CalendarView extends FrameLayout {
	public interface OnDateChangeListener {
		void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth);
	}

	private long mDate = System.currentTimeMillis();
	private OnDateChangeListener mOnDateChangeListener;

	public CalendarView(Context context) {
		this(context, null);
	}

	public CalendarView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/** AOSP setDate(): the legacy delegate does not call the listener for a programmatic change */
	public void setDate(long date) {
		mDate = date;
	}

	public void setDate(long date, boolean animate, boolean center) {
		mDate = date;
	}

	public long getDate() {
		return mDate;
	}

	public void setOnDateChangeListener(OnDateChangeListener listener) {
		mOnDateChangeListener = listener;
	}
}
