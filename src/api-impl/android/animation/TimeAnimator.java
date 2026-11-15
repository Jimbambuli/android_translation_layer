package android.animation;

import android.view.animation.AnimationUtils;

/* AOSP TimeAnimator (frameworks/base/core/java/android/animation/TimeAnimator.java): a ValueAnimator that calls the
 * TimeListener on every animation frame with the total and the delta time */
public class TimeAnimator extends ValueAnimator {

	private TimeListener mListener;
	private long mPreviousTime = -1;

	@Override
	public void start() {
		mPreviousTime = -1;
		super.start();
	}

	@Override
	boolean animateBasedOnTime(long currentTime) {
		if (mListener != null) {
			long totalTime = currentTime - mStartTime;
			long deltaTime = (mPreviousTime < 0) ? 0 : (currentTime - mPreviousTime);
			mPreviousTime = currentTime;
			mListener.onTimeUpdate(this, totalTime, deltaTime);
		}
		return false;
	}

	@Override
	public void setCurrentPlayTime(long playTime) {
		long currentTime = AnimationUtils.currentAnimationTimeMillis();
		mStartTime = Math.max(mStartTime, currentTime - playTime);
		mStartTimeCommitted = true; // do not allow start time to be compensated for jank
		animateBasedOnTime(currentTime);
	}

	public void setTimeListener(TimeListener listener) {
		mListener = listener;
	}

	@Override
	void animateValue(float fraction) {
		// Noop
	}

	@Override
	void initAnimation() {
		// noop
	}

	public static interface TimeListener {
		void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime);
	}
}
