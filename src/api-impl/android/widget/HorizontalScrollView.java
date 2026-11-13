package android.widget;

import android.content.Context;
import android.util.AttributeSet;

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

	/* AOSP: a child's pressed state is delayed, as the touch may start a scroll */
	@Override
	public boolean shouldDelayChildPressedState() {
		return true;
	}
}
