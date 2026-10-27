package android.transition;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import java.util.ArrayList;

public class TransitionSet extends Transition {
	public static final int ORDERING_TOGETHER = 0;
	public static final int ORDERING_SEQUENTIAL = 1;

	ArrayList<Transition> mTransitions = new ArrayList<Transition>();
	private int mOrdering = ORDERING_TOGETHER;

	public TransitionSet() {}

	public TransitionSet(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.TransitionSet);
		int ordering = a.getInt(com.android.internal.R.styleable.TransitionSet_transitionOrdering, TransitionSet.ORDERING_TOGETHER);
		setOrdering(ordering);
		a.recycle();
	}

	public TransitionSet setOrdering(int ordering) {
		switch (ordering) {
			case ORDERING_SEQUENTIAL:
			case ORDERING_TOGETHER:
				mOrdering = ordering;
				break;
			default:
				throw new android.util.AndroidRuntimeException("Invalid parameter for TransitionSet ordering: " + ordering);
		}
		return this;
	}

	public int getOrdering() {
		return mOrdering;
	}

	public TransitionSet addTransition(Transition transition) {
		if (transition != null) {
			mTransitions.add(transition);
			if (mDuration >= 0) {
				transition.setDuration(mDuration);
			}
			if (mInterpolator != null) {
				transition.setInterpolator(mInterpolator);
			}
		}
		return this;
	}

	public int getTransitionCount() {
		return mTransitions.size();
	}

	public Transition getTransitionAt(int index) {
		if (index < 0 || index >= mTransitions.size()) {
			return null;
		}
		return mTransitions.get(index);
	}

	@Override
	public TransitionSet setDuration(long duration) {
		super.setDuration(duration);
		if (mDuration >= 0 && mTransitions != null) {
			for (Transition t : mTransitions) {
				t.setDuration(duration);
			}
		}
		return this;
	}

	@Override
	public TransitionSet setInterpolator(TimeInterpolator interpolator) {
		super.setInterpolator(interpolator);
		if (mInterpolator != null && mTransitions != null) {
			for (Transition t : mTransitions) {
				t.setInterpolator(interpolator);
			}
		}
		return this;
	}

	@Override
	public TransitionSet clone() {
		TransitionSet clone = (TransitionSet)super.clone();
		clone.mTransitions = new ArrayList<Transition>();
		for (Transition t : mTransitions) {
			clone.mTransitions.add(t.clone());
		}
		return clone;
	}
}
