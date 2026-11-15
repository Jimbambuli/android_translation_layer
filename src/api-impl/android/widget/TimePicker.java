package android.widget;

import android.R;
import android.atl.annotation.Export;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class TimePicker extends FrameLayout {

	// pointers
	private long hour_spin_widget;
	private long minute_spin_widget;
	private long btn_widget;

	private OnTimeChangedListener on_time_changed_listener = null;

	public TimePicker(Context context) {
		this(context, null);
	}

	public TimePicker(Context context, AttributeSet attributeSet) {
		this(context, attributeSet, 0);
	}

	public TimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public TimePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	/* GTK draws the picker (the native widget of this view), FrameLayout has no children to measure it
	 * by: measure the GTK widget as View.onMeasure() does for a native view, or the picker gets size 0 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		native_measure(widget, widthMeasureSpec, heightMeasureSpec);
	}

	public Integer getCurrentHour() {
		return getHour();
	}

	public Integer getCurrentMinute() {
		return getMinute();
	}

	public int getHour() {
		return nativeGetSpinBtnValue(hour_spin_widget);
	}

	public int getMinute() {
		return nativeGetSpinBtnValue(minute_spin_widget);
	}

	/* AOSP setIs24HourView()/is24HourView(); the GTK spin buttons always show 0-23, as getHour() returns */
	private boolean mIs24HourView = true;

	public void setIs24HourView(Boolean is24HourView) {
		if (is24HourView == null) {
			return;
		}
		mIs24HourView = is24HourView;
	}

	public boolean is24HourView() {
		return mIs24HourView;
	}

	public boolean isEnabled() {
		return true;
	}

	public void setEnabled(boolean enabled) {
	}

	public void setCurrentHour(Integer currentHour) {
		setHour(currentHour);
	}

	public void setCurrentMinute(Integer currentMinute) {
		setMinute(currentMinute);
	}

	public void setHour(int hour) {
		nativeSetSpinBtnValue(hour_spin_widget, hour);
	}

	public void setMinute(int minute) {
		nativeSetSpinBtnValue(minute_spin_widget, minute);
	}

	public void setOnTimeChangedListener(OnTimeChangedListener onTimeChangedListener) {
		nativeSetOnTimeChangedListener(btn_widget);
		on_time_changed_listener = onTimeChangedListener;
	}

	@Export
	private void onTimeChange() {
		if (on_time_changed_listener != null) {
			on_time_changed_listener.onTimeChanged(this, getCurrentHour(), getCurrentMinute());
		}
	}

	protected native void nativeSetOnTimeChangedListener(long widget);

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);

	private native void nativeSetSpinBtnValue(long widget, int value);
	private native int nativeGetSpinBtnValue(long widget);

	public static interface OnTimeChangedListener {
		void onTimeChanged(TimePicker view, int hourOfDay, int minute);
	}
}
