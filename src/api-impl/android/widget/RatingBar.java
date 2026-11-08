package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public class RatingBar extends AbsSeekBar {

	public RatingBar(Context context) {
		this(context, null);
	}

	public RatingBar(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}

	private int mNumStars = 5;

	/** Sets the number of stars to show; as in AOSP a value of zero or less is ignored. */
	public void setNumStars(final int numStars) {
		if (numStars <= 0) {
			return;
		}
		mNumStars = numStars;
		requestLayout();
	}

	/** Returns the number of stars shown (5 by default). */
	public int getNumStars() {
		return mNumStars;
	}

	public void setRating(float rating) {
		setProgress((int)rating);
	}
}
