package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;

public class HorizontalScrollView extends FrameLayout {
	private boolean mFillViewport;

	public HorizontalScrollView(Context context) {
		super(context);
	}

	public HorizontalScrollView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		final TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.HorizontalScrollView, R.attr.horizontalScrollViewStyle, 0);
		setFillViewport(a.getBoolean(R.styleable.HorizontalScrollView_fillViewport, false));
		a.recycle();
	}

	public void setHorizontalScrollBarEnabled(boolean enabled) {}

	/** AOSP HorizontalScrollView.smoothScrollBy(): dx is clamped to 0..child width - viewport width; ATL scrolls
	 * at once (AOSP does so too within ANIMATED_SCROLL_GAP), without the OverScroller animation */
	public final void smoothScrollBy(int dx, int dy) {
		if (getChildCount() == 0)
			return;
		final int width = getWidth() - getPaddingRight() - getPaddingLeft();
		final int right = getChildAt(0).getWidth();
		final int maxX = Math.max(0, right - width);
		final int scrollX = getScrollX();
		dx = Math.max(0, Math.min(scrollX + dx, maxX)) - scrollX;
		scrollBy(dx, 0);
	}

	/** AOSP HorizontalScrollView.smoothScrollTo() */
	public final void smoothScrollTo(int x, int y) {
		smoothScrollBy(x - getScrollX(), y - getScrollY());
	}

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

	public boolean isFillViewport() {
		return mFillViewport;
	}

	/** AOSP HorizontalScrollView.setFillViewport() */
	public void setFillViewport(boolean fillViewport) {
		if (fillViewport != mFillViewport) {
			mFillViewport = fillViewport;
			requestLayout();
		}
	}

	/** AOSP HorizontalScrollView.onMeasure(): with fillViewport, a child narrower than the viewport is measured
	 * again to fill it */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (!mFillViewport)
			return;
		if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED)
			return;
		if (getChildCount() > 0) {
			final View child = getChildAt(0);
			final int widthPadding;
			final int heightPadding;
			final ViewGroup.LayoutParams lp = child.getLayoutParams();
			final ViewGroup.MarginLayoutParams mlp = lp instanceof ViewGroup.MarginLayoutParams ? (ViewGroup.MarginLayoutParams)lp : new ViewGroup.MarginLayoutParams(lp);
			if (getContext().getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.M) {
				widthPadding = getPaddingLeft() + getPaddingRight() + mlp.leftMargin + mlp.rightMargin;
				heightPadding = getPaddingTop() + getPaddingBottom() + mlp.topMargin + mlp.bottomMargin;
			} else {
				widthPadding = getPaddingLeft() + getPaddingRight();
				heightPadding = getPaddingTop() + getPaddingBottom();
			}
			final int desiredWidth = getMeasuredWidth() - widthPadding;
			if (child.getMeasuredWidth() < desiredWidth) {
				final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(desiredWidth, MeasureSpec.EXACTLY);
				final int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, heightPadding, lp.height);
				child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
			}
		}
	}

	/** AOSP HorizontalScrollView.measureChild(): the child may be as wide as it wants (UNSPECIFIED width) */
	@Override
	protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
		ViewGroup.LayoutParams lp = child.getLayoutParams();
		final int horizontalPadding = getPaddingLeft() + getPaddingRight();
		final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
		    Math.max(0, MeasureSpec.getSize(parentWidthMeasureSpec) - horizontalPadding), MeasureSpec.UNSPECIFIED);
		final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
		    getPaddingTop() + getPaddingBottom(), lp.height);
		child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
	}

	/** AOSP HorizontalScrollView.measureChildWithMargins() (makeSafeMeasureSpec = makeMeasureSpec for targetSdk >= M) */
	@Override
	protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
	                                       int parentHeightMeasureSpec, int heightUsed) {
		final MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
		final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
		    getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin + heightUsed, lp.height);
		final int usedTotal = getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin + widthUsed;
		final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
		    Math.max(0, MeasureSpec.getSize(parentWidthMeasureSpec) - usedTotal), MeasureSpec.UNSPECIFIED);
		child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
	}

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
