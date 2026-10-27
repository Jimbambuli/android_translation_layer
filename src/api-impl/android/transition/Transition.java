package android.transition;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

/* The properties of AOSP's Transition that its XML sets (duration, startDelay, interpolator), read as
 * in Transition(Context, AttributeSet). ATL has no TransitionManager, so transitions are not run by
 * the framework: PopupWindow plays the Fade of its popupEnter/ExitTransition itself. */
public class Transition implements Cloneable {
	long mStartDelay = -1;
	long mDuration = -1;
	TimeInterpolator mInterpolator = null;

	public Transition() {}

	public Transition(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.Transition);
		long duration = a.getInt(com.android.internal.R.styleable.Transition_duration, -1);
		if (duration >= 0) {
			setDuration(duration);
		}
		long startDelay = a.getInt(com.android.internal.R.styleable.Transition_startDelay, -1);
		if (startDelay > 0) {
			setStartDelay(startDelay);
		}
		final int resId = a.getResourceId(com.android.internal.R.styleable.Transition_interpolator, 0);
		if (resId > 0) {
			setInterpolator(AnimationUtils.loadInterpolator(context, resId));
		}
		a.recycle();
	}

	public long getDuration() {
		return mDuration;
	}

	public long getStartDelay() {
		return mStartDelay;
	}

	public TimeInterpolator getInterpolator() {
		return mInterpolator;
	}

	public Transition setStartDelay(long startDelay) {
		mStartDelay = startDelay;
		return this;
	}

	public interface TransitionListener {}

	@Override
	public Transition clone() {
		try {
			return (Transition)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	public Transition addListener(TransitionListener listener) {
		return this;
	}

	public Transition excludeTarget(int targetId, boolean exclude) {
		return this;
	}

	public Transition addTarget(View target) {
		return this;
	}

	public Transition addTarget(String targetName) {
		return this;
	}

	public Transition setDuration(long duration) {
		mDuration = duration;
		return this;
	}

	public Transition setInterpolator(TimeInterpolator interpolator) {
		mInterpolator = interpolator;
		return this;
	}
}
