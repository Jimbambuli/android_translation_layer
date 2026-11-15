package android.widget;

import android.atl.annotation.Export;
import android.content.Context;
import android.util.AttributeSet;
import java.util.Calendar;

public class DatePicker extends FrameLayout {

	private OnDateChangedListener on_date_changed_listener = null;

	/* AOSP DatePickerCalendarDelegate: DEFAULT_START_YEAR 1900-01-01 .. DEFAULT_END_YEAR 2100-12-31 */
	private final Calendar mMinDate = Calendar.getInstance();
	private final Calendar mMaxDate = Calendar.getInstance();
	{
		mMinDate.set(1900, Calendar.JANUARY, 1);
		mMaxDate.set(2100, Calendar.DECEMBER, 31);
	}

	public interface OnDateChangedListener {
		void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth);
	}

	public DatePicker(Context context) {
		this(context, null);
	}

	public DatePicker(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DatePicker(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public DatePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	/* GTK draws the picker (the native widget of this view), FrameLayout has no children to measure it
	 * by: measure the GTK widget as View.onMeasure() does for a native view, or the picker gets size 0 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		native_measure(widget, widthMeasureSpec, heightMeasureSpec);
	}

	private Calendar currentDate() {
		Calendar c = Calendar.getInstance();
		c.set(getYear(), getMonth(), getDayOfMonth());
		return c;
	}

	private static boolean sameDay(Calendar a, Calendar b) {
		return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
	}

	/* AOSP DatePickerCalendarDelegate.setMinDate(): a current date before the new minimum moves to it
	 * (AOSP also tells the listener; here that is left to updateDate() as for any other update); the GTK
	 * calendar itself has no range, so earlier days stay selectable */
	public void setMinDate(long minDate) {
		Calendar temp = Calendar.getInstance();
		temp.setTimeInMillis(minDate);
		if (sameDay(temp, mMinDate))
			return;
		if (currentDate().before(temp)) {
			updateDate(temp.get(Calendar.YEAR), temp.get(Calendar.MONTH), temp.get(Calendar.DAY_OF_MONTH));
		}
		mMinDate.setTimeInMillis(minDate);
	}

	public long getMinDate() {
		return mMinDate.getTimeInMillis();
	}

	/* AOSP DatePickerCalendarDelegate.setMaxDate(), as setMinDate() */
	public void setMaxDate(long maxDate) {
		Calendar temp = Calendar.getInstance();
		temp.setTimeInMillis(maxDate);
		if (sameDay(temp, mMaxDate))
			return;
		if (currentDate().after(temp)) {
			updateDate(temp.get(Calendar.YEAR), temp.get(Calendar.MONTH), temp.get(Calendar.DAY_OF_MONTH));
		}
		mMaxDate.setTimeInMillis(maxDate);
	}

	public long getMaxDate() {
		return mMaxDate.getTimeInMillis();
	}

	public void init(int year, int monthOfYear, int dayOfMonth, OnDateChangedListener listener) {
		updateDate(year, monthOfYear, dayOfMonth);
		setOnDateChangedListener(listener);
	}

	/* AOSP DatePickerCalendarDelegate.setFirstDayOfWeek()/getFirstDayOfWeek(): 0 means the first day of the locale; an invalid day
	 * of week is ignored. ATL's calendar does not draw the week header from it. */
	private int mFirstDayOfWeek = 0;

	public void setFirstDayOfWeek(int firstDayOfWeek) {
		if (firstDayOfWeek != 0 && (firstDayOfWeek < Calendar.SUNDAY || firstDayOfWeek > Calendar.SATURDAY)) {
			return;
		}
		mFirstDayOfWeek = firstDayOfWeek;
	}

	public int getFirstDayOfWeek() {
		if (mFirstDayOfWeek != 0) {
			return mFirstDayOfWeek;
		}
		return Calendar.getInstance(java.util.Locale.getDefault()).getFirstDayOfWeek();
	}

	public void setOnDateChangedListener(OnDateChangedListener onDateChangedListener) {
		nativeSetOnDateChangedListener(widget);
		on_date_changed_listener = onDateChangedListener;
	}

	@Export
	private void onDateChange() {
		if (on_date_changed_listener != null) {
			on_date_changed_listener.onDateChanged(this, getYear(), getMonth(), getDayOfMonth());
		}
	}

	public int getYear() {
		return nativeGetYear(widget);
	}

	public int getMonth() {
		return nativeGetMonth(widget);
	}

	public int getDayOfMonth() {
		return nativeGetDay(widget);
	}

	public void updateDate(int year, int month, int dayOfMonth) {
		nativeUpdateDate(widget, year, month, dayOfMonth);
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	protected native void nativeSetOnDateChangedListener(long widget);

	protected native int nativeGetYear(long widget);
	protected native int nativeGetMonth(long widget);
	protected native int nativeGetDay(long widget);

	protected native void nativeUpdateDate(long widget, int year, int month, int dayOfMonth);

	/* AOSP DatePickerCalendarDelegate (the default mode of DatePicker): the spinners and the calendar view are not shown
	 * separately, setCalendarViewShown()/setSpinnersShown() do nothing and the getters return false */
	public void setCalendarViewShown(boolean shown) {}

	public boolean getCalendarViewShown() {
		return false;
	}

	public void setSpinnersShown(boolean shown) {}

	public boolean getSpinnersShown() {
		return false;
	}
}
