package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.android.internal.R;
import java.util.Formatter;
import java.util.IllegalFormatException;
import java.util.Locale;

/* AOSP Chronometer (frameworks/base/core/java/android/widget/Chronometer.java): a TextView that shows the time
 * elapsed since (or left until) a base in SystemClock.elapsedRealtime(), ticking once a second while shown.
 * Differences: the negative duration uses the U+2212 sign directly (R.string.negative_duration), and
 * getContentDescription() is not overridden (the ICU MeasureFormat of AOSP is not available). */
public class Chronometer extends TextView {
	private static final String TAG = "Chronometer";

	public interface OnChronometerTickListener {
		void onChronometerTick(Chronometer chronometer);
	}

	private long mBase;
	private boolean mVisible;
	private boolean mStarted;
	private boolean mRunning;
	private boolean mLogged;
	private String mFormat;
	private Formatter mFormatter;
	private Locale mFormatterLocale;
	private Object[] mFormatterArgs = new Object[1];
	private StringBuilder mFormatBuilder;
	private OnChronometerTickListener mOnChronometerTickListener;
	private boolean mCountDown;

	public Chronometer(Context context) {
		this(context, null, 0);
	}

	public Chronometer(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public Chronometer(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public Chronometer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Chronometer, defStyleAttr, defStyleRes);
		setFormat(a.getString(R.styleable.Chronometer_format));
		setCountDown(a.getBoolean(R.styleable.Chronometer_countDown, false));
		a.recycle();

		mBase = SystemClock.elapsedRealtime();
		updateText(mBase);
	}

	public void setCountDown(boolean countDown) {
		mCountDown = countDown;
		updateText(SystemClock.elapsedRealtime());
	}

	public boolean isCountDown() {
		return mCountDown;
	}

	public void setBase(long base) {
		mBase = base;
		dispatchChronometerTick();
		updateText(SystemClock.elapsedRealtime());
	}

	public long getBase() {
		return mBase;
	}

	public void setFormat(String format) {
		mFormat = format;
		if (format != null && mFormatBuilder == null) {
			mFormatBuilder = new StringBuilder(format.length() * 2);
		}
	}

	public String getFormat() {
		return mFormat;
	}

	public void setOnChronometerTickListener(OnChronometerTickListener listener) {
		mOnChronometerTickListener = listener;
	}

	public OnChronometerTickListener getOnChronometerTickListener() {
		return mOnChronometerTickListener;
	}

	public void start() {
		mStarted = true;
		updateRunning();
	}

	public void stop() {
		mStarted = false;
		updateRunning();
	}

	public void setStarted(boolean started) {
		mStarted = started;
		updateRunning();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mVisible = false;
		updateRunning();
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
		mVisible = visibility == VISIBLE;
		updateRunning();
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		updateRunning();
	}

	private synchronized void updateText(long now) {
		long seconds = mCountDown ? mBase - now : now - mBase;
		seconds /= 1000;
		boolean negative = false;
		if (seconds < 0) {
			seconds = -seconds;
			negative = true;
		}
		String text = DateUtils.formatElapsedTime(seconds);
		if (negative) {
			text = "−" + text;
		}

		if (mFormat != null) {
			Locale loc = Locale.getDefault();
			if (mFormatter == null || !loc.equals(mFormatterLocale)) {
				mFormatterLocale = loc;
				mFormatter = new Formatter(mFormatBuilder, loc);
			}
			mFormatBuilder.setLength(0);
			mFormatterArgs[0] = text;
			try {
				mFormatter.format(mFormat, mFormatterArgs);
				text = mFormatBuilder.toString();
			} catch (IllegalFormatException ex) {
				if (!mLogged) {
					Log.w(TAG, "Illegal format string: " + mFormat);
					mLogged = true;
				}
			}
		}
		setText(text);
	}

	private void updateRunning() {
		boolean running = mVisible && mStarted && isShown();
		if (running != mRunning) {
			if (running) {
				updateText(SystemClock.elapsedRealtime());
				dispatchChronometerTick();
				postDelayed(mTickRunnable, 1000);
			} else {
				removeCallbacks(mTickRunnable);
			}
			mRunning = running;
		}
	}

	private final Runnable mTickRunnable = new Runnable() {
		@Override
		public void run() {
			if (mRunning) {
				updateText(SystemClock.elapsedRealtime());
				dispatchChronometerTick();
				postDelayed(mTickRunnable, 1000);
			}
		}
	};

	void dispatchChronometerTick() {
		if (mOnChronometerTickListener != null) {
			mOnChronometerTickListener.onChronometerTick(this);
		}
	}
}
