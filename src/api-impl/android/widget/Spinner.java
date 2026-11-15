package android.widget;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

public class Spinner extends AbsSpinner {
	/** AOSP Spinner.MODE_DIALOG: the choices are shown in a dialog; ATL's GTK drop-down popup is used in both modes */
	public static final int MODE_DIALOG = 0;
	/** AOSP Spinner.MODE_DROPDOWN */
	public static final int MODE_DROPDOWN = 1;
	/* AOSP: take the mode from the spinnerMode attribute */
	private static final int MODE_THEME = -1;

	private Observer observer;
	private Drawable popupBackground;
	private Context mPopupContext;

	/** AOSP getPopupContext(): the context of the popup theme (constructor argument), else the spinner's context */
	public Context getPopupContext() {
		return mPopupContext;
	}

	public Spinner(Context context) {
		this(context, null, 0);
		haveCustomMeasure = false;
	}

	/** AOSP Spinner(Context, int mode): the mode is not used, the GTK drop-down is the only popup */
	public Spinner(Context context, int mode) {
		this(context, null, R.attr.spinnerStyle, mode);
	}

	public Spinner(Context context, AttributeSet attributeSet) {
		this(context, attributeSet, 0);
		haveCustomMeasure = false;
	}

	public Spinner(Context context, AttributeSet attributeSet, int defStyle) {
		this(context, attributeSet, defStyle, 0, MODE_THEME);
	}

	public Spinner(Context context, AttributeSet attrs, int defStyleAttr, int mode) {
		this(context, attrs, defStyleAttr, 0, mode);
	}

	public Spinner(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, int mode) {
		this(context, attrs, defStyleAttr, defStyleRes, mode, null);
	}

	/** AOSP Spinner: the popup context wraps the popup theme; the mode only selects AOSP's dialog or drop-down popup,
	 * which ATL does not have (the GTK drop-down is used for both) */
	public Spinner(Context context, AttributeSet attributeSet, int defStyle, int defStyleRes, int mode,
	               android.content.res.Resources.Theme popupTheme) {
		super(context, attributeSet, defStyle, defStyleRes);
		haveCustomMeasure = false;

		TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.Spinner, defStyle, defStyleRes);
		/* the android:popupTheme attribute is not in ATL's android.R.styleable.Spinner, only the argument counts */
		mPopupContext = popupTheme != null ? new android.view.ContextThemeWrapper(context, popupTheme) : context;
		popupBackground = a.getDrawable(R.styleable.Spinner_popupBackground);
		mDropDownWidth = a.getLayoutDimension(R.styleable.Spinner_dropDownWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
		a.recycle();
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	protected native void native_setAdapter(long widget, SpinnerAdapter adapter);
	@Override
	protected native void native_setBackgroundDrawable(long widget, long paintable);
	@Override
	protected native void native_setBackgroundColor(long widget, int color);

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return native_gtkEventPropagation();
	}

	public void setAdapter(SpinnerAdapter adapter) {
		if (observer == null)
			observer = new Observer();
		SpinnerAdapter oldAdapter = getAdapter();
		if (oldAdapter != null)
			oldAdapter.unregisterDataSetObserver(observer);
		super.setAdapter(adapter);
		if (adapter != null)
			adapter.registerDataSetObserver(observer);
		native_setAdapter(this.widget, adapter);
	}

	public SpinnerAdapter getAdapter() {
		return (SpinnerAdapter)super.getAdapter();
	}

	@Override
	public native void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener);

	private class Observer extends DataSetObserver {

		@Override
		public void onChanged() {
			Spinner.this.native_setAdapter(widget, getAdapter());
		}
		@Override
		public void onInvalidated() {
			Spinner.this.native_setAdapter(widget, getAdapter());
		}
	}

	@Override
	void layout(int delta, boolean animate) {}

	/* AOSP Spinner: mDropDownWidth from the dropDownWidth attribute, default WRAP_CONTENT; the GTK drop-down
	 * sizes its popup itself, so the value is only kept for the getter */
	private int mDropDownWidth = ViewGroup.LayoutParams.WRAP_CONTENT;

	public int getDropDownWidth() { return mDropDownWidth; }

	/* the GTK drop-down places and sizes its popup itself; the values are only kept for the getters */
	private int dropDownHorizontalOffset;
	private int dropDownVerticalOffset;
	private CharSequence prompt;

	public void setDropDownWidth(int pixels) { mDropDownWidth = pixels; }

	public void setDropDownHorizontalOffset(int pixels) {
		dropDownHorizontalOffset = pixels;
	}

	public int getDropDownHorizontalOffset() {
		return dropDownHorizontalOffset;
	}

	public void setDropDownVerticalOffset(int pixels) {
		dropDownVerticalOffset = pixels;
	}

	public int getDropDownVerticalOffset() {
		return dropDownVerticalOffset;
	}

	public void setPopupBackgroundDrawable(Drawable background) {
		popupBackground = background;
	}

	public void setPopupBackgroundResource(int resId) {
		setPopupBackgroundDrawable(getContext().getDrawable(resId));
	}

	public Drawable getPopupBackground() {
		return popupBackground;
	}

	public void setPrompt(CharSequence prompt) {
		this.prompt = prompt;
	}

	public void setPromptId(int promptId) {
		setPrompt(getContext().getText(promptId));
	}

	public CharSequence getPrompt() {
		return prompt;
	}
}
