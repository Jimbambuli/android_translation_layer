package android.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import java.util.Calendar;
import java.util.TimeZone;

/* AOSP TextClock: shows the current time in the format for the 12 or 24 hour mode of the system and keeps it
 * current while it is attached (every minute, every second when the format has seconds) */
public class TextClock extends TextView {
	private CharSequence mFormat12Hour = "h:mm a";
	private CharSequence mFormat24Hour = "H:mm";
	private String mTimeZone = "";
	private final Handler mHandler = new Handler(Looper.getMainLooper());
	private boolean mTicking;

	private final Runnable mTicker = new Runnable() {
		@Override
		public void run() {
			refreshTime();
			long now = System.currentTimeMillis();
			long step = hasSeconds() ? 1000 : 60000;
			mHandler.postAtTime(this, SystemClock.uptimeMillis() + step - now % step);
		}
	};

	public TextClock(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TextClock(Context context) {
		super(context);
	}

	public TextClock(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public TextClock(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public CharSequence getFormat12Hour() {
		return mFormat12Hour;
	}

	public CharSequence getFormat24Hour() {
		return mFormat24Hour;
	}

	public String getTimeZone() {
		return mTimeZone;
	}

	public boolean is24HourModeEnabled() {
		return DateFormat.is24HourFormat(getContext());
	}

	public void onVisibilityAggregated(boolean isVisible) {}

	public void refreshTime() {
		Calendar now = mTimeZone == null || mTimeZone.isEmpty() ? Calendar.getInstance() : Calendar.getInstance(TimeZone.getTimeZone(mTimeZone));
		CharSequence format = is24HourModeEnabled() ? mFormat24Hour : mFormat12Hour;
		if (format != null)
			setText(DateFormat.format(format, now));
	}

	private boolean hasSeconds() {
		CharSequence format = is24HourModeEnabled() ? mFormat24Hour : mFormat12Hour;
		return format != null && format.toString().indexOf('s') >= 0;
	}

	public void setFormat12Hour(CharSequence format) {
		mFormat12Hour = format;
		refreshTime();
	}

	public void setFormat24Hour(CharSequence format) {
		mFormat24Hour = format;
		refreshTime();
	}

	public void setTimeZone(String timeZone) {
		mTimeZone = timeZone;
		refreshTime();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (!mTicking) {
			mTicking = true;
			mTicker.run();
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mTicking) {
			mTicking = false;
			mHandler.removeCallbacks(mTicker);
		}
	}
}
