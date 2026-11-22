package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class Button extends TextView {

	public Button(Context context) {
		this(context, null);
	}

	/* AOSP: this(context, attrs, getButtonDefaultStyleAttr(context), getButtonDefaultStyleRes()), which are
	 * R.attr.buttonStyle and 0 except for the Material 3 style of Wear watches */
	public Button(Context context, AttributeSet attrs) {
		this(context, attrs, com.android.internal.R.attr.buttonStyle, 0);
	}

	public Button(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public Button(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.TextView, defStyleAttr, defStyleRes);
		if (a.hasValue(com.android.internal.R.styleable.TextView_text)) {
			setText(a.getText(com.android.internal.R.styleable.TextView_text));
		}

		a.recycle();

		/* AOSP's Button draws only its background drawable and nothing without one (background="@null"): the
		 * GtkButton never draws a frame, shadow or focus ring of its own, nor its own background */
		native_addClass(widget, "ATL-no-border");
		if (getBackground() == null)
			native_setBackgroundDrawable(widget, 0);
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	public native void native_setText(long widget, String text);
	@Override
	protected native void nativeSetOnClickListener(long widget);

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return native_gtkEventPropagation();
	}

	@Override
	public void setText(CharSequence text) {
		/* AOSP TextView.setText(): a null text is the empty text (icon-only buttons set null) */
		if (text == null)
			text = "";
		native_setText(widget, text.toString());
	}

	@Override
	public native CharSequence getText();

	/* the label of the GtkButton takes the size as an absolute Pango size in pixels; without it GTK's default font
	 * (in points) follows gtk-xft-dpi, which Android's sp do not */
	@Override
	protected void applyNativeTextSize(float size) {
		native_setTextSize(widget, size);
	}

	private native void native_setTextSize(long widget, float size);
}
