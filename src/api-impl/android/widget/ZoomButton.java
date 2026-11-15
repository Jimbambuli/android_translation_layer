package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;

/** AOSP ZoomButton (android16 core/java/android/widget/ZoomButton.java): an ImageButton that repeats its click while pressed long */
public class ZoomButton extends ImageButton implements OnLongClickListener {

	private final Runnable mRunnable = new Runnable() {
		public void run() {
			if (hasOnClickListeners() && mIsInLongpress && isEnabled()) {
				callOnClick();
				postDelayed(this, mZoomSpeed);
			}
		}
	};

	private long mZoomSpeed = 1000;
	private boolean mIsInLongpress;

	public ZoomButton(Context context) {
		this(context, null);
	}

	public ZoomButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ZoomButton(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public ZoomButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		setOnLongClickListener(this);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if ((event.getAction() == MotionEvent.ACTION_CANCEL) || (event.getAction() == MotionEvent.ACTION_UP)) {
			mIsInLongpress = false;
		}
		return super.onTouchEvent(event);
	}

	/** Sets the delay between calls to the click listener, in milliseconds */
	public void setZoomSpeed(long speed) {
		mZoomSpeed = speed;
	}

	@Override
	public boolean onLongClick(View v) {
		mIsInLongpress = true;
		post(mRunnable);
		return true;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		mIsInLongpress = false;
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) {
			/* a disabled view gets no up event, so reset the pressed state here */
			setPressed(false);
		}
		super.setEnabled(enabled);
	}

	@Override
	public boolean dispatchUnhandledMove(View focused, int direction) {
		clearFocus();
		return super.dispatchUnhandledMove(focused, direction);
	}

	@Override
	public CharSequence getAccessibilityClassName() {
		return ZoomButton.class.getName();
	}
}
