package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.widget.TimePicker;
import com.android.internal.R;

public class TimePickerDialog extends AlertDialog implements DialogInterface.OnClickListener, TimePicker.OnTimeChangedListener {

	private TimePicker time_picker;
	private OnTimeSetListener time_set_listener = null;

	public TimePickerDialog(Context context, OnTimeSetListener listener, int hourOfDay, int minute, boolean is24HourView) {
		this(context, 0, listener, hourOfDay, minute, is24HourView);
	}

	public TimePickerDialog(Context context, int themeResId, OnTimeSetListener listener, int hourOfDay, int minute, boolean is24HourView) {
		super(context, resolveDialogTheme(context, themeResId));

		time_set_listener = listener;

		/* AOSP: the picker with OK and Cancel beside it; only OK reports the time (onClick()). ATL's TimePicker is
		 * GTK's instead of AOSP's layout time_picker_dialog */
		final Context themeContext = getContext();
		time_picker = new TimePicker(themeContext);
		setView(time_picker);
		setButton(BUTTON_POSITIVE, themeContext.getString(R.string.ok), this);
		setButton(BUTTON_NEGATIVE, themeContext.getString(R.string.cancel), this);
		setButtonPanelLayoutHint(LAYOUT_HINT_SIDE);

		time_picker.setIs24HourView(is24HourView);
		time_picker.setCurrentHour(hourOfDay);
		time_picker.setCurrentMinute(minute);
		time_picker.setOnTimeChangedListener(this);
	}

	/* AOSP TimePickerDialog.onClick(): the positive button reports the last valid time, the negative one cancels */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
			case BUTTON_POSITIVE:
				if (time_set_listener != null) {
					time_set_listener.onTimeSet(time_picker, time_picker.getCurrentHour(), time_picker.getCurrentMinute());
				}
				break;
			case BUTTON_NEGATIVE:
				cancel();
				break;
		}
	}

	public void updateTime(int hourOfDay, int minuteOfHour) {
		time_picker.setHour(hourOfDay);
		time_picker.setMinute(minuteOfHour);
	}

	/* AOSP: does nothing, the listener hears of the time with OK */
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
	}

	public static interface OnTimeSetListener {
		void onTimeSet(TimePicker view, int hourOfDay, int minute);
	}

	/* function ported from AOSP - Copyright 2007, The Android Open Source Project */
	static int resolveDialogTheme(Context context, int resId) {
		if (resId == 0) {
			final TypedValue outValue = new TypedValue();
			context.getTheme().resolveAttribute(R.attr.timePickerDialogTheme, outValue, true);
			return outValue.resourceId;
		} else {
			return resId;
		}
	}
}
