package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public class CheckBox extends CompoundButton {

	public CheckBox(Context context) {
		this(context, null);
	}

	public CheckBox(Context context, AttributeSet attrs) {
		this(context, attrs, com.android.internal.R.attr.checkboxStyle);
	}

	public CheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public CheckBox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public void setLines(int lines) {}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);

	@Override
	public native void setChecked(boolean checked);

	@Override
	public native boolean isChecked();

	// following methods are overridden to prevent calling incompatible methods from superclasses
	@Override
	public void setOnClickListener(final OnClickListener l) {}
	@Override
	public void setTextColor(int color) {}
	@Override
	protected void applyNativeTextSize(float size) {}

	@Override
	public native void native_setText(long widget, String text);
}
