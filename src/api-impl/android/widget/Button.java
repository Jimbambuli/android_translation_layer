package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class Button extends TextView {

	private Drawable compoundDrawableLeft;

	public Button(Context context) {
		this(context, null);
	}

	public Button(Context context, AttributeSet attributeSet) {
		this(context, attributeSet, 0);
	}

	public Button(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public Button(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.TextView, 0, 0);
		if (a.hasValue(com.android.internal.R.styleable.TextView_text)) {
			setText(a.getText(com.android.internal.R.styleable.TextView_text));
		}

		a.recycle();
	}

	/* a button with a background of its own draws no GTK frame; MaterialButton, for one,
	 * sets its background only after the constructor */
	@Override
	public void setBackgroundDrawable(Drawable backgroundDrawable) {
		super.setBackgroundDrawable(backgroundDrawable);
		if (backgroundDrawable != null)
			native_addClass(widget, "ATL-no-border");
		else
			native_removeClass(widget, "ATL-no-border");
	}

	@Override
	public void setBackgroundColor(int color) {
		super.setBackgroundColor(color);
		native_addClass(widget, "ATL-no-border");
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	public native void native_setText(long widget, String text);
	@Override
	protected native void nativeSetOnClickListener(long widget);
	/* the native side draws the drawable in its bounds at the time it is drawn */
	protected native void native_setCompoundDrawables(long widget, Drawable drawable);

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return native_gtkEventPropagation();
	}

	@Override
	public void setText(CharSequence text) {
		native_setText(widget, String.valueOf(text));
	}

	@Override
	public native CharSequence getText();

	@Override
	public void setTextSize(float size) {}

	/* the native Button draws only the left drawable (TextView keeps all of them) */
	@Override
	protected void updateNativeCompoundDrawables() {
		Drawable left = getCompoundDrawables()[0];
		if (left == null && compoundDrawableLeft == null)
			return;
		compoundDrawableLeft = left;
		native_setCompoundDrawables(widget, compoundDrawableLeft);
	}

	/* the drawable is drawn in the bounds it has then: MaterialButton sets them in onLayout() */
	@Override
	public void layout(int l, int t, int r, int b) {
		super.layout(l, t, r, b);
		if (compoundDrawableLeft != null)
			native_updateCompoundDrawable(widget);
	}

	private native void native_updateCompoundDrawable(long widget);
}
