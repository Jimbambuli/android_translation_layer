package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.widget.DatePicker;
import com.android.internal.R;

public class DatePickerDialog extends AlertDialog implements DialogInterface.OnClickListener, DatePicker.OnDateChangedListener {

	private DatePicker date_picker;
	private OnDateSetListener date_set_listener = null;

	public DatePickerDialog(Context context) {
		this(context, 0);
	}

	public DatePickerDialog(Context context, int themeResId) {
		this(context, themeResId, null, -1, -1, -1);
	}

	public DatePickerDialog(Context context, DatePickerDialog.OnDateSetListener listener, int year, int month, int dayOfMonth) {
		this(context, 0, listener, year, month, dayOfMonth);
	}

	public DatePickerDialog(Context context, int themeResId, DatePickerDialog.OnDateSetListener listener, int year, int monthOfYear, int dayOfMonth) {
		super(context, resolveDialogTheme(context, themeResId));

		date_set_listener = listener;
		setTitle(R.string.date_picker_dialog_title);
		date_picker = new DatePicker(context);
		date_picker.setOnDateChangedListener(this);
		date_picker.updateDate(year, monthOfYear, dayOfMonth);
		setView(date_picker);
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

	public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		if (date_set_listener != null) {
			date_set_listener.onDateSet(date_picker, year, monthOfYear, dayOfMonth);
		}
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
