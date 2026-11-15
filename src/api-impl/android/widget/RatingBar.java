package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import com.android.internal.R;

/* AOSP RatingBar (frameworks/base/core/java/android/widget/RatingBar.java); the accessibility node info is left out */
public class RatingBar extends AbsSeekBar {

	public interface OnRatingBarChangeListener {
		void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser);
	}

	private int mNumStars = 5;
	private int mProgressOnStartTracking;
	private OnRatingBarChangeListener mOnRatingBarChangeListener;

	public RatingBar(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public RatingBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RatingBar, defStyleAttr, defStyleRes);
		final int numStars = a.getInt(R.styleable.RatingBar_numStars, mNumStars);
		setIsIndicator(a.getBoolean(R.styleable.RatingBar_isIndicator, !mIsUserSeekable));
		final float rating = a.getFloat(R.styleable.RatingBar_rating, -1);
		final float stepSize = a.getFloat(R.styleable.RatingBar_stepSize, -1);
		a.recycle();

		if (numStars > 0 && numStars != mNumStars) {
			setNumStars(numStars);
		}
		if (stepSize >= 0) {
			setStepSize(stepSize);
		} else {
			setStepSize(0.5f);
		}
		if (rating >= 0) {
			setRating(rating);
		}
		// A touch inside a star fill up to that fractional area (slightly more than 1/2)
		mTouchProgressOffset = 0.6f;
	}

	public RatingBar(Context context, AttributeSet attrs) {
		this(context, attrs, com.android.internal.R.attr.ratingBarStyle);
	}

	public RatingBar(Context context) {
		this(context, null);
	}

	public void setOnRatingBarChangeListener(OnRatingBarChangeListener listener) {
		mOnRatingBarChangeListener = listener;
	}

	public OnRatingBarChangeListener getOnRatingBarChangeListener() {
		return mOnRatingBarChangeListener;
	}

	public void setIsIndicator(boolean isIndicator) {
		mIsUserSeekable = !isIndicator;
		if (isIndicator) {
			setFocusable(FOCUSABLE_AUTO);
		} else {
			setFocusable(FOCUSABLE);
		}
	}

	public boolean isIndicator() {
		return !mIsUserSeekable;
	}

	/** Sets the number of stars to show; as in AOSP a value of zero or less is ignored. */
	public void setNumStars(final int numStars) {
		if (numStars <= 0) {
			return;
		}
		mNumStars = numStars;
		requestLayout();
	}

	public int getNumStars() {
		return mNumStars;
	}

	public void setRating(float rating) {
		setProgress(Math.round(rating * getProgressPerStar()));
	}

	public float getRating() {
		return getProgress() / getProgressPerStar();
	}

	public void setStepSize(float stepSize) {
		if (stepSize <= 0) {
			return;
		}
		final float newMax = mNumStars / stepSize;
		final int newProgress = (int) (newMax / getMax() * getProgress());
		setMax((int) newMax);
		setProgress(newProgress);
	}

	public float getStepSize() {
		return (float) getNumStars() / getMax();
	}

	private float getProgressPerStar() {
		if (mNumStars > 0) {
			return 1f * getMax() / mNumStars;
		} else {
			return 1;
		}
	}

	@Override
	Shape getDrawableShape() {
		// TODO: Once ProgressBar's TODOs are fixed, this won't be needed
		return new RectShape();
	}

	@Override
	void onProgressRefresh(float scale, boolean fromUser, int progress) {
		super.onProgressRefresh(scale, fromUser, progress);

		// Keep secondary progress in sync with primary
		updateSecondaryProgress(progress);

		if (!fromUser) {
			// Callback for non-user updates
			dispatchRatingChange(false);
		}
	}

	private void updateSecondaryProgress(int progress) {
		final float ratio = getProgressPerStar();
		if (ratio > 0) {
			final float progressInStars = progress / ratio;
			final int secondaryProgress = (int) (Math.ceil(progressInStars) * ratio);
			setSecondaryProgress(secondaryProgress);
		}
	}

	@Override
	protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (mSampleWidth > 0) {
			final int width = mSampleWidth * mNumStars;
			setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0), getMeasuredHeight());
		}
	}

	@Override
	void onStartTrackingTouch() {
		mProgressOnStartTracking = getProgress();

		super.onStartTrackingTouch();
	}

	@Override
	void onStopTrackingTouch() {
		super.onStopTrackingTouch();

		if (getProgress() != mProgressOnStartTracking) {
			dispatchRatingChange(true);
		}
	}

	@Override
	void onKeyChange() {
		super.onKeyChange();
		dispatchRatingChange(true);
	}

	void dispatchRatingChange(boolean fromUser) {
		if (mOnRatingBarChangeListener != null) {
			mOnRatingBarChangeListener.onRatingChanged(this, getRating(), fromUser);
		}
	}

	@Override
	public synchronized void setMax(int max) {
		// Disallow max progress = 0
		if (max <= 0) {
			return;
		}

		super.setMax(max);
	}

	@Override
	boolean canUserSetProgress() {
		return super.canUserSetProgress() && !isIndicator();
	}
}
