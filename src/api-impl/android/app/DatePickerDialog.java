package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.widget.DatePicker;
import java.util.Calendar;
import com.android.internal.R;

public class DatePickerDialog extends AlertDialog implements DialogInterface.OnClickListener, DatePicker.OnDateChangedListener {

	private DatePicker date_picker;
	private OnDateSetListener date_set_listener = null;

	public DatePickerDialog(Context context) {
		this(context, 0);
	}

	public DatePickerDialog(Context context, int themeResId) {
		this(context, themeResId, null, Calendar.getInstance(), -1, -1, -1);
	}

	public DatePickerDialog(Context context, DatePickerDialog.OnDateSetListener listener, int year, int month, int dayOfMonth) {
		this(context, 0, listener, year, month, dayOfMonth);
	}

	public DatePickerDialog(Context context, int themeResId, DatePickerDialog.OnDateSetListener listener, int year, int monthOfYear, int dayOfMonth) {
		this(context, themeResId, listener, null, year, monthOfYear, dayOfMonth);
	}

	/* AOSP: the picker with OK and Cancel beside it; only OK reports the date (onClick()). ATL's DatePicker is GTK's
	 * calendar instead of AOSP's layout date_picker_dialog */
	private DatePickerDialog(Context context, int themeResId, DatePickerDialog.OnDateSetListener listener, Calendar calendar, int year, int monthOfYear, int dayOfMonth) {
		super(context, resolveDialogTheme(context, themeResId));

		final Context themeContext = getContext();
		date_picker = new DatePicker(themeContext);
		setView(date_picker);

		setButton(BUTTON_POSITIVE, themeContext.getString(R.string.ok), this);
		setButton(BUTTON_NEGATIVE, themeContext.getString(R.string.cancel), this);
		setButtonPanelLayoutHint(LAYOUT_HINT_SIDE);

		if (calendar != null) {
			year = calendar.get(Calendar.YEAR);
			monthOfYear = calendar.get(Calendar.MONTH);
			dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
		}

		date_picker.init(year, monthOfYear, dayOfMonth, this);

		date_set_listener = listener;
	}

	public DatePicker getDatePicker() {
		return date_picker;
	}

	/* AOSP DatePickerDialog.onClick(): the positive button reports the picked date, the negative one cancels */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
			case BUTTON_POSITIVE:
				if (date_set_listener != null) {
					date_picker.clearFocus();
					date_set_listener.onDateSet(date_picker, date_picker.getYear(), date_picker.getMonth(), date_picker.getDayOfMonth());
				}
				break;
			case BUTTON_NEGATIVE:
				cancel();
				break;
		}
	}

	public void updateDate(int year, int month, int dayOfMonth) {
		date_picker.updateDate(year, month, dayOfMonth);
	}

	public interface OnDateSetListener {
		public abstract void onDateSet(DatePicker view, int year, int month, int dayOfMonth);
	}

	/* AOSP: only puts the date into the picker again (mDatePicker.init()), the listener hears of it with OK; ATL's
	 * GTK calendar shows the date already */
	public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
	}

	public void setOnDateSetListener(DatePickerDialog.OnDateSetListener listener) {
		date_set_listener = listener;
	}

	/* function ported from AOSP - Copyright 2007, The Android Open Source Project */
	static int resolveDialogTheme(Context context, int resId) {
		if (resId == 0) {
			final TypedValue outValue = new TypedValue();
			context.getTheme().resolveAttribute(R.attr.datePickerDialogTheme, outValue, true);
			return outValue.resourceId;
		} else {
			return resId;
		}
	}
}
