package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class HorizontalScrollView extends FrameLayout {

	public HorizontalScrollView(Context context) {
		super(context);
	}

	public HorizontalScrollView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}

	public void setHorizontalScrollBarEnabled(boolean enabled) {}

	public void smoothScrollTo(int x, int y) {}

	public boolean fullScroll(int direction) {
		return true;
	}

	/* AOSP: whether arrowScroll() scrolls smoothly; ATL keeps the setting, GTK scrolls on its own */
	private boolean mSmoothScrollingEnabled = true;

	public boolean isSmoothScrollingEnabled() {
		return mSmoothScrollingEnabled;
	}

	public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled) {
		mSmoothScrollingEnabled = smoothScrollingEnabled;
	}

	public void setFillViewport(boolean fillViewport) {}

	/* AOSP: the scroll position stays within the child; TabLayout scrolls to a negative x to center its first tab,
	 * which without the clamp shifted all touches off the tabs */
	@Override
	public void scrollTo(int x, int y) {
		if (getChildCount() > 0) {
			View child = getChildAt(0);
			x = clamp(x, getWidth() - getPaddingRight() - getPaddingLeft(), child.getWidth());
			y = clamp(y, getHeight() - getPaddingBottom() - getPaddingTop(), child.getHeight());
			if (x != getScrollX() || y != getScrollY())
				super.scrollTo(x, y);
		}
	}

	private static int clamp(int n, int my, int child) {
		if (my >= child || n < 0)
			return 0;
		if ((my + n) > child)
			return child - my;
		return n;
	}

	/* AOSP: a child's pressed state is delayed, as the touch may start a scroll */
	@Override
	public boolean shouldDelayChildPressedState() {
		return true;
	}
}
