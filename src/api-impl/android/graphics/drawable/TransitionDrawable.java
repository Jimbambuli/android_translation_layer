package android.graphics.drawable;

import android.graphics.Canvas;
import android.os.SystemClock;

/* as in AOSP: cross-fades from the first layer to the second (startTransition) and back (reverseTransition);
 * shows the first layer until a transition is started */
public class TransitionDrawable extends LayerDrawable {
	private static final int TRANSITION_STARTING = 0;
	private static final int TRANSITION_RUNNING = 1;
	private static final int TRANSITION_NONE = 2;

	private int mTransitionState = TRANSITION_NONE;
	private boolean mReverse;
	private long mStartTimeMillis;
	private int mFrom;
	private int mTo;
	private int mDuration;
	private int mOriginalDuration;
	private int mAlpha = 0;
	private boolean mCrossFade;

	public TransitionDrawable(Drawable[] layers) {
		super(layers);
	}

	/* for <transition> in XML; the layers come from inflate() */
	TransitionDrawable() {
		super();
	}

	public void startTransition(int durationMillis) {
		mFrom = 0;
		mTo = 255;
		mAlpha = 0;
		mDuration = mOriginalDuration = durationMillis;
		mReverse = false;
		mTransitionState = TRANSITION_STARTING;
		invalidateSelf();
	}

	public void showSecondLayer() {
		mAlpha = 255;
		mReverse = false;
		mTransitionState = TRANSITION_NONE;
		invalidateSelf();
	}

	public void resetTransition() {
		mAlpha = 0;
		mTransitionState = TRANSITION_NONE;
		invalidateSelf();
	}

	public void reverseTransition(int duration) {
		final long time = SystemClock.uptimeMillis();
		// animation is over
		if (time - mStartTimeMillis > mDuration) {
			if (mTo == 0) {
				mFrom = 0;
				mTo = 255;
				mAlpha = 0;
				mReverse = false;
			} else {
				mFrom = 255;
				mTo = 0;
				mAlpha = 255;
				mReverse = true;
			}
			mDuration = mOriginalDuration = duration;
			mTransitionState = TRANSITION_STARTING;
			invalidateSelf();
			return;
		}
		mReverse = !mReverse;
		mFrom = mAlpha;
		mTo = mReverse ? 0 : 255;
		mDuration = (int)(mReverse ? time - mStartTimeMillis : mOriginalDuration - (time - mStartTimeMillis));
		mTransitionState = TRANSITION_STARTING;
	}

	@Override
	public void draw(Canvas canvas) {
		boolean done = true;
		switch (mTransitionState) {
			case TRANSITION_STARTING:
				mStartTimeMillis = SystemClock.uptimeMillis();
				done = false;
				mTransitionState = TRANSITION_RUNNING;
				break;
			case TRANSITION_RUNNING:
				if (mStartTimeMillis >= 0) {
					float normalized = mDuration > 0 ? (float)(SystemClock.uptimeMillis() - mStartTimeMillis) / mDuration : 1.0f;
					done = normalized >= 1.0f;
					normalized = Math.min(normalized, 1.0f);
					mAlpha = (int)(mFrom + (mTo - mFrom) * normalized);
				}
				break;
		}

		final int alpha = mAlpha;
		final boolean crossFade = mCrossFade;
		final ChildDrawable[] array = mLayerState.mChildren;

		if (done) {
			// the transition is over: draw only what is visible
			if (!crossFade || alpha == 0)
				array[0].mDrawable.draw(canvas);
			if (alpha == 0xFF)
				array[1].mDrawable.draw(canvas);
			return;
		}

		Drawable d = array[0].mDrawable;
		if (crossFade)
			d.setAlpha(255 - alpha);
		d.draw(canvas);
		if (crossFade)
			d.setAlpha(0xFF);

		if (alpha > 0) {
			d = array[1].mDrawable;
			d.setAlpha(alpha);
			d.draw(canvas);
			d.setAlpha(0xFF);
		}

		if (!done)
			invalidateSelf();
	}

	public void setCrossFadeEnabled(boolean enabled) {
		mCrossFade = enabled;
	}

	public boolean isCrossFadeEnabled() {
		return mCrossFade;
	}
}
