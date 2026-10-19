package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class Space extends View {

	public Space(Context context) {
		super(context);
	}

	public Space(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}

	/* as in AOSP: unlike View, a Space doesn't fill an AT_MOST spec but keeps its minimum size,
	 * so it can serve as a weighted spacer in a wrapping LinearLayout */
	private static int getDefaultSize2(int size, int measureSpec) {
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		switch (specMode) {
			case MeasureSpec.AT_MOST:
				return Math.min(size, specSize);
			case MeasureSpec.EXACTLY:
				return specSize;
			default:
				return size;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(getDefaultSize2(getSuggestedMinimumWidth(), widthMeasureSpec),
		                     getDefaultSize2(getSuggestedMinimumHeight(), heightMeasureSpec));
	}
}
