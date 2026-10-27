package android.transition;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

public abstract class Visibility extends Transition {
	public static final int MODE_IN = 0x1;
	public static final int MODE_OUT = 0x2;
	private int mMode = MODE_IN | MODE_OUT;

	public Visibility() {}

	public Visibility(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.VisibilityTransition);
		int mode = a.getInt(com.android.internal.R.styleable.VisibilityTransition_transitionVisibilityMode, 0);
		a.recycle();
		if (mode != 0) {
			setMode(mode);
		}
	}

	public void setMode(int mode) {
		if ((mode & ~(MODE_IN | MODE_OUT)) != 0) {
			throw new IllegalArgumentException("Only MODE_IN and MODE_OUT flags are allowed");
		}
		mMode = mode;
	}

	public int getMode() {
		return mMode;
	}
}
