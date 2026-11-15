package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * AOSP AnalogClock (android16 core/java/android/widget/AnalogClock.java) draws dial and hands from drawables. ATL draws
 * nothing here: the time zone is kept, as AOSP does with setTimeZone().
 */
public class AnalogClock extends View {
	private String mTimeZone;

	public AnalogClock(Context context) {
		this(context, null);
	}

	public AnalogClock(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AnalogClock(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public AnalogClock(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	/** Sets the specified time zone id (e.g. "America/Los_Angeles"); null uses the default zone */
	public void setTimeZone(String timeZone) {
		mTimeZone = timeZone;
	}
}
