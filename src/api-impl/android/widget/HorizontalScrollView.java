package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;

public class HorizontalScrollView extends FrameLayout {
	private boolean mFillViewport;

	public HorizontalScrollView(Context context) {
		super(context);
	}

	public HorizontalScrollView(Context context, AttributeSet attributeSet) {
		this(context, attributeSet, R.attr.horizontalScrollViewStyle);
	}

	public HorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public HorizontalScrollView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
		super(context, attributeSet, defStyleAttr, defStyleRes);
		final TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.HorizontalScrollView, defStyleAttr, defStyleRes);
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

	/** AOSP pageScroll(): one page (the view width) to the left or right, limited to the child; no focus change */
	public boolean pageScroll(int direction) {
		if (getChildCount() == 0)
			return false;
		final int width = getWidth();
		final int scrollX = getScrollX();
		final int target;
		if (direction == View.FOCUS_RIGHT)
			target = Math.min(scrollX + width, getChildAt(getChildCount() - 1).getRight() - width);
		else
			target = Math.max(scrollX - width, 0);
		smoothScrollBy(target - scrollX, 0);
		return getScrollX() != scrollX;
	}

	/** AOSP getMaxScrollAmount() with MAX_SCROLL_FACTOR 0.5 */
	public int getMaxScrollAmount() {
		return (int)(0.5f * (getRight() - getLeft()));
	}

	/** AOSP fling(): the distance of an OverScroller fling over 0..child width - viewport, from the current position;
	 * ATL scrolls there at once instead of animated and draws no edge effects */
	public void fling(int velocityX) {
		if (getChildCount() > 0) {
			final int width = getWidth() - getPaddingRight() - getPaddingLeft();
			final int right = getChildAt(0).getRight() - getPaddingLeft();
			OverScroller scroller = new OverScroller(getContext());
			scroller.fling(getScrollX(), 0, velocityX, 0, 0, Math.max(0, right - width), 0, 0, 0, 0);
			smoothScrollBy(scroller.getFinalX() - getScrollX(), 0);
		}
	}

	/** AOSP executeKeyEvent(): keys that scroll; arrowScroll() scrolls by getMaxScrollAmount() without the focus search,
	 * as AOSP does when no next focus is found. */
	public boolean executeKeyEvent(KeyEvent event) {
		if (getChildCount() == 0)
			return false;
		boolean handled = false;
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_DPAD_LEFT:
					if (!event.isAltPressed()) {
						smoothScrollBy(-getMaxScrollAmount(), 0);
						handled = true;
					} else {
						handled = fullScroll(View.FOCUS_LEFT);
					}
					break;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					if (!event.isAltPressed()) {
						smoothScrollBy(getMaxScrollAmount(), 0);
						handled = true;
					} else {
						handled = fullScroll(View.FOCUS_RIGHT);
					}
					break;
				case KeyEvent.KEYCODE_SPACE:
					pageScroll(event.isShiftPressed() ? View.FOCUS_LEFT : View.FOCUS_RIGHT);
					break;
			}
		}
		return handled;
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

	/* AOSP setLeftEdgeEffectColor() and friends: the colors are kept, ATL draws no edge glow */
	private int mEdgeGlowLeftColor = 0xff000000;
	private int mEdgeGlowRightColor = 0xff000000;

	public void setEdgeEffectColor(int color) {
		setLeftEdgeEffectColor(color);
		setRightEdgeEffectColor(color);
	}

	public void setRightEdgeEffectColor(int color) {
		mEdgeGlowRightColor = color;
		invalidate();
	}

	public void setLeftEdgeEffectColor(int color) {
		mEdgeGlowLeftColor = color;
		invalidate();
	}

	public int getLeftEdgeEffectColor() {
		return mEdgeGlowLeftColor;
	}

	public int getRightEdgeEffectColor() {
		return mEdgeGlowRightColor;
	}

	/** AOSP computeScrollDeltaToGetChildRectOnScreen(): how far to scroll to show rect (child coordinates); the fading edge
	 * length is 0 as ATL draws no fading edges (AOSP getHorizontalFadingEdgeLength() is 0 unless one is enabled) */
	protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
		if (getChildCount() == 0)
			return 0;

		int width = getWidth();
		int screenLeft = getScrollX();
		int screenRight = screenLeft + width;

		int scrollXDelta = 0;

		if (rect.right > screenRight && rect.left > screenLeft) {
			// need to move right to get it in view: move right just enough so that the entire rectangle is in view (or at least
			// the first screen size chunk).
			if (rect.width() > width) {
				// just enough to get screen size chunk on
				scrollXDelta += (rect.left - screenLeft);
			} else {
				// get entire rect at right of screen
				scrollXDelta += (rect.right - screenRight);
			}

			// make sure we aren't scrolling beyond the end of our content
			int right = getChildAt(0).getRight();
			int distanceToRight = right - screenRight;
			scrollXDelta = Math.min(scrollXDelta, distanceToRight);

		} else if (rect.left < screenLeft && rect.right < screenRight) {
			// need to move right to get it in view: move right just enough so that entire rectangle is in view (or at least the
			// first screen size chunk of it).
			if (rect.width() > width) {
				// screen size chunk
				scrollXDelta -= (screenRight - rect.right);
			} else {
				// entire rect at left
				scrollXDelta -= (screenLeft - rect.left);
			}

			// make sure we aren't scrolling any further than the left our content
			scrollXDelta = Math.max(scrollXDelta, -getScrollX());
		}
		return scrollXDelta;
	}
}
