package android.transition;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

/* AOSP's Fade without the animators (see Transition): onAppear() fades the view from alpha 0 to 1,
 * onDisappear() from its alpha to 0, with getDuration() and getInterpolator() */
public class Fade extends Visibility {
	public static final int IN = Visibility.MODE_IN;
	public static final int OUT = Visibility.MODE_OUT;

	public Fade() {}

	public Fade(int fadingMode) {
		setMode(fadingMode);
	}

	public Fade(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.Fade);
		int fadingMode = a.getInt(com.android.internal.R.styleable.Fade_fadingMode, getMode());
		a.recycle();
		setMode(fadingMode);
	}
}
