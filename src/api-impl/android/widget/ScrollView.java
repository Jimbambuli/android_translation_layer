package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;

public class ScrollView extends FrameLayout {
	private boolean mFillViewport;

	public ScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScrollView, R.attr.scrollViewStyle, 0);
		setFillViewport(a.getBoolean(R.styleable.ScrollView_fillViewport, false));
		a.recycle();
	}

	public ScrollView(Context context) {
		super(context);
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	@Override
	protected native void native_addView(long widget, long child, int index, ViewGroup.LayoutParams params);
	@Override
	protected native void native_removeView(long widget, long child);
	private native void native_scrollTo(long widget, int x, int y, boolean relative);

	/** AOSP smoothScrollBy(): scroll by (dx, dy) within the scroll range; GTK holds the position */
	public final void smoothScrollBy(int dx, int dy) {
		if (getChildCount() == 0) {
			// Nothing to do.
			return;
		}
		native_scrollTo(widget, dx, dy, true);
	}

	/** AOSP smoothScrollTo() = smoothScrollBy(x - scrollX, y - scrollY), with the position taken from GTK */
	public final void smoothScrollTo(int x, int y) {
		if (getChildCount() == 0)
			return;
		native_scrollTo(widget, x, y, false);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		return native_gtkEventPropagation();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return native_gtkEventPropagation();
	}

	/* the children's positions in Java do not follow the GTK scrolling: as with the touches
	 * (onInterceptTouchEvent()), the wheel goes on along GTK's path to the children and the ScrollView */
	@Override
	protected boolean dispatchGenericPointerEvent(MotionEvent event) {
		return native_gtkEventPropagation();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = 0;
		int height = 0;
		if (getChildCount() > 0) {
			View child = getChildAt(0);
			ViewGroup.LayoutParams lp = child.getLayoutParams();
			int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
			int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
			child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
			width = child.getMeasuredWidth();
			height = child.getMeasuredHeight();
		}
		setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));

		/* AOSP ScrollView.onMeasure(): with fillViewport, a child smaller than the viewport is measured again
		 * to fill it */
		if (!mFillViewport)
			return;
		if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED)
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
			final int desiredHeight = getMeasuredHeight() - heightPadding;
			if (child.getMeasuredHeight() < desiredHeight) {
				final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, widthPadding, lp.width);
				final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(desiredHeight, MeasureSpec.EXACTLY);
				child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
			}
		}
	}

	public boolean isFillViewport() {
		return mFillViewport;
	}

	public void setFillViewport(boolean fillViewport) {
		if (fillViewport != mFillViewport) {
			mFillViewport = fillViewport;
			requestLayout();
		}
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

	/* AOSP: a child's pressed state is delayed, as the touch may start a scroll */
	@Override
	public boolean shouldDelayChildPressedState() {
		return true;
	}

	/* AOSP setTopEdgeEffectColor() and friends: the colors are kept, ATL draws no edge glow */
	private int mEdgeGlowTopColor = 0xff000000;
	private int mEdgeGlowBottomColor = 0xff000000;

	public void setEdgeEffectColor(int color) {
		setTopEdgeEffectColor(color);
		setBottomEdgeEffectColor(color);
	}

	public void setBottomEdgeEffectColor(int color) {
		mEdgeGlowBottomColor = color;
		invalidate();
	}

	public void setTopEdgeEffectColor(int color) {
		mEdgeGlowTopColor = color;
		invalidate();
	}

	public int getTopEdgeEffectColor() {
		return mEdgeGlowTopColor;
	}

	public int getBottomEdgeEffectColor() {
		return mEdgeGlowBottomColor;
	}

	/** AOSP computeScrollDeltaToGetChildRectOnScreen(): how far to scroll to show rect (child coordinates); the fading edge
	 * length is 0 as ATL draws no fading edges (AOSP getVerticalFadingEdgeLength() is 0 unless one is enabled) */
	protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
		if (getChildCount() == 0)
			return 0;

		int height = getHeight();
		int screenTop = getScrollY();
		int screenBottom = screenTop + height;

		int scrollYDelta = 0;

		if (rect.bottom > screenBottom && rect.top > screenTop) {
			// need to move down to get it in view: move down just enough so that the entire rectangle is in view (or at least
			// the first screen size chunk of it).
			if (rect.height() > height) {
				// just enough to get screen size chunk on
				scrollYDelta += (rect.top - screenTop);
			} else {
				// get entire rect at bottom of screen
				scrollYDelta += (rect.bottom - screenBottom);
			}

			// make sure we aren't scrolling beyond the end of our content
			int bottom = getChildAt(0).getBottom();
			int distanceToBottom = bottom - screenBottom;
			scrollYDelta = Math.min(scrollYDelta, distanceToBottom);

		} else if (rect.top < screenTop && rect.bottom < screenBottom) {
			// need to move up to get it in view: move up just enough so that entire rectangle is in view (or at least the first
			// screen size chunk)
			if (rect.height() > height) {
				// screen size chunk
				scrollYDelta -= (screenBottom - rect.bottom);
			} else {
				// entire rect at top
				scrollYDelta -= (screenTop - rect.top);
			}

			// make sure we aren't scrolling any further than the top our content
			scrollYDelta = Math.max(scrollYDelta, -getScrollY());
		}
		return scrollYDelta;
	}
}
