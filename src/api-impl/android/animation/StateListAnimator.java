/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.animation;

import android.util.StateSet;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Lets you define a number of Animators that will run on the attached View depending on the View's
 * drawable state (shortened from AOSP, without the constant state cache).
 */
public class StateListAnimator implements Cloneable {

	private final ArrayList<Tuple> mTuples = new ArrayList<Tuple>();
	private Tuple mLastMatch = null;
	private Animator mRunningAnimator = null;
	private WeakReference<View> mViewRef;

	private final AnimatorListenerAdapter mAnimatorListener = new AnimatorListenerAdapter() {
		@Override
		public void onAnimationEnd(Animator animation) {
			animation.setTarget(null);
			if (mRunningAnimator == animation)
				mRunningAnimator = null;
		}
	};

	/**
	 * Associates the given animator with the provided drawable state specs so that it will be run
	 * when the View's drawable state matches the specs.
	 */
	public void addState(int[] specs, Animator animator) {
		Tuple tuple = new Tuple(specs, animator);
		tuple.mAnimator.addListener(mAnimatorListener);
		mTuples.add(tuple);
	}

	/** @hide */
	public Animator getRunningAnimator() {
		return mRunningAnimator;
	}

	/** @hide */
	public View getTarget() {
		return mViewRef == null ? null : mViewRef.get();
	}

	/** @hide called by View.setStateListAnimator() */
	public void setTarget(View view) {
		final View current = getTarget();
		if (current == view)
			return;
		if (current != null)
			clearTarget();
		if (view != null)
			mViewRef = new WeakReference<View>(view);
	}

	private void clearTarget() {
		for (Tuple tuple : mTuples)
			tuple.mAnimator.setTarget(null);
		mViewRef = null;
		mLastMatch = null;
		mRunningAnimator = null;
	}

	/** @hide called by View when its drawable state changes */
	public void setState(int[] state) {
		Tuple match = null;
		for (Tuple tuple : mTuples) {
			if (StateSet.stateSetMatches(tuple.mSpecs, state)) {
				match = tuple;
				break;
			}
		}
		if (match == mLastMatch)
			return;
		if (mLastMatch != null)
			cancel();
		mLastMatch = match;
		if (match != null)
			start(match);
	}

	private void start(Tuple match) {
		match.mAnimator.setTarget(getTarget());
		mRunningAnimator = match.mAnimator;
		mRunningAnimator.start();
	}

	private void cancel() {
		if (mRunningAnimator != null) {
			mRunningAnimator.cancel();
			mRunningAnimator = null;
		}
	}

	/** If there is an animation running for a recent state change, ends it. */
	/* AOSP StateListAnimator.clone(): fresh state (no target, no last match, no running animator), every state's
	 * animator cloned with the original's listener removed; a new instance instead of Object.clone() because ATL's
	 * fields are final */
	@Override
	public StateListAnimator clone() {
		StateListAnimator clone = new StateListAnimator();
		final int tupleSize = mTuples.size();
		for (int i = 0; i < tupleSize; i++) {
			final Tuple tuple = mTuples.get(i);
			final Animator animatorClone = tuple.mAnimator.clone();
			animatorClone.removeListener(mAnimatorListener);
			clone.addState(tuple.mSpecs, animatorClone);
		}
		return clone;
	}

	public void jumpToCurrentState() {
		if (mRunningAnimator != null)
			mRunningAnimator.end();
	}

	private static class Tuple {
		final int[] mSpecs;
		final Animator mAnimator;

		private Tuple(int[] specs, Animator animator) {
			mSpecs = specs;
			mAnimator = animator;
		}
	}
}
