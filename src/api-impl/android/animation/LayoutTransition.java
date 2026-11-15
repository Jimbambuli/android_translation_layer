package android.animation;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import java.util.ArrayList;
import java.util.List;

/* After AOSP core/java/android/animation/LayoutTransition.java: the settings (transition types, animators,
 * durations, start delays) are kept as in AOSP; ATL's ViewGroup does not run the transitions */
public class LayoutTransition {

	public static final int CHANGE_APPEARING = 0;
	public static final int CHANGE_DISAPPEARING = 1;
	public static final int APPEARING = 2;
	public static final int DISAPPEARING = 3;
	public static final int CHANGING = 4;

	private static final int FLAG_APPEARING = 0x01;
	private static final int FLAG_DISAPPEARING = 0x02;
	private static final int FLAG_CHANGE_APPEARING = 0x04;
	private static final int FLAG_CHANGE_DISAPPEARING = 0x08;
	private static final int FLAG_CHANGING = 0x10;

	private static final long DEFAULT_DURATION = 300;

	/* AOSP: by default appearing/disappearing and the change animations related to them are enabled (not CHANGING) */
	private int mTransitionTypes = FLAG_CHANGE_APPEARING | FLAG_CHANGE_DISAPPEARING | FLAG_APPEARING | FLAG_DISAPPEARING;

	/* indexed by transition type; AOSP sets default animators in the constructor, ATL keeps a plain
	 * ObjectAnimator in their place as before */
	private final Animator[] mAnimators = new Animator[5];
	private final long[] mDurations = {DEFAULT_DURATION, DEFAULT_DURATION, DEFAULT_DURATION, DEFAULT_DURATION, DEFAULT_DURATION};
	/* AOSP: mChangingAppearingDelay 0, mChangingDisappearingDelay DEFAULT_DURATION, mAppearingDelay DEFAULT_DURATION,
	 * mDisappearingDelay 0, mChangingDelay 0 */
	private final long[] mStartDelays = {0, DEFAULT_DURATION, DEFAULT_DURATION, 0, 0};

	public LayoutTransition() {
		for (int i = 0; i < mAnimators.length; i++)
			mAnimators[i] = new ObjectAnimator();
	}

	private static int flag(int transitionType) {
		switch (transitionType) {
			case APPEARING:
				return FLAG_APPEARING;
			case DISAPPEARING:
				return FLAG_DISAPPEARING;
			case CHANGE_APPEARING:
				return FLAG_CHANGE_APPEARING;
			case CHANGE_DISAPPEARING:
				return FLAG_CHANGE_DISAPPEARING;
			case CHANGING:
				return FLAG_CHANGING;
		}
		return 0;
	}

	private static boolean valid(int transitionType) {
		return transitionType >= CHANGE_APPEARING && transitionType <= CHANGING;
	}

	public void enableTransitionType(int transitionType) {
		mTransitionTypes |= flag(transitionType);
	}

	public void disableTransitionType(int transitionType) {
		mTransitionTypes &= ~flag(transitionType);
	}

	public boolean isTransitionTypeEnabled(int transitionType) {
		int flag = flag(transitionType);
		return flag != 0 && (mTransitionTypes & flag) == flag;
	}

	public void setStartDelay(int transitionType, long delay) {
		if (valid(transitionType))
			mStartDelays[transitionType] = delay;
	}

	/* AOSP getStartDelay(): 0 for an unknown type */
	public long getStartDelay(int transitionType) {
		return valid(transitionType) ? mStartDelays[transitionType] : 0;
	}

	public void setAnimator(int transitionType, Animator animator) {
		if (valid(transitionType))
			mAnimators[transitionType] = animator;
	}

	/* AOSP getAnimator(): null for an unknown type */
	public Animator getAnimator(int transitionType) {
		return valid(transitionType) ? mAnimators[transitionType] : null;
	}

	/* AOSP setDuration(long): all five durations */
	public void setDuration(long duration) {
		for (int i = 0; i < mDurations.length; i++)
			mDurations[i] = duration;
	}

	public void setDuration(int transitionType, long duration) {
		if (valid(transitionType))
			mDurations[transitionType] = duration;
	}

	/* AOSP getDuration(): 0 for an unknown type */
	public long getDuration(int transitionType) {
		return valid(transitionType) ? mDurations[transitionType] : 0;
	}

	/* AOSP: appearing/disappearing use AccelerateDecelerateInterpolator, the change animations DecelerateInterpolator */
	private TimeInterpolator mAppearingInterpolator = new AccelerateDecelerateInterpolator();
	private TimeInterpolator mDisappearingInterpolator = new AccelerateDecelerateInterpolator();
	private TimeInterpolator mChangingAppearingInterpolator = new DecelerateInterpolator();
	private TimeInterpolator mChangingDisappearingInterpolator = new DecelerateInterpolator();
	private TimeInterpolator mChangingInterpolator = new DecelerateInterpolator();

	public void setInterpolator(int transitionType, TimeInterpolator interpolator) {
		switch (transitionType) {
			case CHANGE_APPEARING:
				mChangingAppearingInterpolator = interpolator;
				break;
			case CHANGE_DISAPPEARING:
				mChangingDisappearingInterpolator = interpolator;
				break;
			case CHANGING:
				mChangingInterpolator = interpolator;
				break;
			case APPEARING:
				mAppearingInterpolator = interpolator;
				break;
			case DISAPPEARING:
				mDisappearingInterpolator = interpolator;
				break;
		}
	}

	public TimeInterpolator getInterpolator(int transitionType) {
		switch (transitionType) {
			case CHANGE_APPEARING:
				return mChangingAppearingInterpolator;
			case CHANGE_DISAPPEARING:
				return mChangingDisappearingInterpolator;
			case CHANGING:
				return mChangingInterpolator;
			case APPEARING:
				return mAppearingInterpolator;
			case DISAPPEARING:
				return mDisappearingInterpolator;
		}
		// shouldn't reach here
		return null;
	}

	/* AOSP TransitionListener: ATL runs no transitions, so the listeners are kept (addTransitionListener() and friends)
	 * but never called */
	public interface TransitionListener {
		public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType);

		public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType);
	}

	private ArrayList<TransitionListener> mListeners;
	private boolean mAnimateParentHierarchy = true;

	public void addTransitionListener(TransitionListener listener) {
		if (mListeners == null) {
			mListeners = new ArrayList<TransitionListener>();
		}
		mListeners.add(listener);
	}

	public void removeTransitionListener(TransitionListener listener) {
		if (mListeners == null) {
			return;
		}
		mListeners.remove(listener);
	}

	public List<TransitionListener> getTransitionListeners() {
		return mListeners;
	}

	public void setAnimateParentHierarchy(boolean animateParentHierarchy) {
		mAnimateParentHierarchy = animateParentHierarchy;
	}

	/** AOSP isChangingLayout(): true while CHANGE_APPEARING/CHANGE_DISAPPEARING animations run; ATL runs none */
	public boolean isChangingLayout() {
		return false;
	}

	/** AOSP isRunning(): true while change/appear/disappear animations run; ATL starts none of them */
	public boolean isRunning() {
		return false;
	}

	/* AOSP: the stagger of the change animations (CHANGE_APPEARING, CHANGE_DISAPPEARING, CHANGING), default 0 */
	private long mChangingAppearingStagger = 0;
	private long mChangingDisappearingStagger = 0;
	private long mChangingStagger = 0;

	public void setStagger(int transitionType, long duration) {
		switch (transitionType) {
			case CHANGE_APPEARING:
				mChangingAppearingStagger = duration;
				break;
			case CHANGE_DISAPPEARING:
				mChangingDisappearingStagger = duration;
				break;
			case CHANGING:
				mChangingStagger = duration;
				break;
			// noop other cases
		}
	}

	public long getStagger(int transitionType) {
		switch (transitionType) {
			case CHANGE_APPEARING:
				return mChangingAppearingStagger;
			case CHANGE_DISAPPEARING:
				return mChangingDisappearingStagger;
			case CHANGING:
				return mChangingStagger;
		}
		// shouldn't reach here
		return 0;
	}

	/* AOSP addChild()/showChild()/removeChild()/hideChild() only start the transition animations for a child that the
	 * caller has already added/removed/shown/hidden; ATL runs no transitions, so there is nothing to start */
	public void addChild(ViewGroup parent, View child) {}

	@Deprecated
	public void showChild(ViewGroup parent, View child) {}

	public void showChild(ViewGroup parent, View child, int oldVisibility) {}

	public void removeChild(ViewGroup parent, View child) {}

	@Deprecated
	public void hideChild(ViewGroup parent, View child) {}

	public void hideChild(ViewGroup parent, View child, int newVisibility) {}
}
