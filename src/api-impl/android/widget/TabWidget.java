package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;

/**
 * AOSP TabWidget (android16 core/java/android/widget/TabWidget.java): the LinearLayout holding the tab indicators of a
 * TabHost, with the selected tab, the click listener per indicator and the bottom strips. Not ported: the accessibility
 * event fields (onInitializeAccessibilityEventInternal) and the hand pointer icon set in addView().
 */
public class TabWidget extends LinearLayout implements android.view.View.OnFocusChangeListener {
	private final Rect mBounds = new Rect();

	private OnTabSelectionChanged mSelectionChangedListener;

	// This value will be set to 0 as soon as the first tab is added to TabHost.
	private int mSelectedTab = -1;

	private Drawable mLeftStrip;
	private Drawable mRightStrip;

	private boolean mDrawBottomStrips = true;
	private boolean mStripMoved;

	// When positive, the widths and heights of tabs will be imposed so that
	// they fit in parent.
	private int mImposedTabsHeight = -1;
	private int[] mImposedTabWidths;

	public TabWidget(Context context) {
		this(context, null);
	}

	public TabWidget(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.tabWidgetStyle);
	}

	public TabWidget(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public TabWidget(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		final TypedArray a = context.obtainStyledAttributes(
		    attrs, R.styleable.TabWidget, defStyleAttr, defStyleRes);

		mDrawBottomStrips = a.getBoolean(R.styleable.TabWidget_tabStripEnabled, mDrawBottomStrips);

		/* AOSP takes tab_bottom_left_v4/tab_bottom_right_v4 for apps targeting DONUT or lower; ATL's framework-res
		 * does not have them, so those apps get the current strips */
		if (a.hasValueOrEmpty(R.styleable.TabWidget_tabStripLeft)) {
			mLeftStrip = a.getDrawable(R.styleable.TabWidget_tabStripLeft);
		} else {
			mLeftStrip = context.getDrawable(R.drawable.tab_bottom_left);
		}

		if (a.hasValueOrEmpty(R.styleable.TabWidget_tabStripRight)) {
			mRightStrip = a.getDrawable(R.styleable.TabWidget_tabStripRight);
		} else {
			mRightStrip = context.getDrawable(R.drawable.tab_bottom_right);
		}

		a.recycle();

		setChildrenDrawingOrderEnabled(true);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mStripMoved = true;

		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected int getChildDrawingOrder(int childCount, int i) {
		if (mSelectedTab == -1) {
			return i;
		} else {
			// Always draw the selected tab last, so that drop shadows are drawn
			// in the correct z-order.
			if (i == childCount - 1) {
				return mSelectedTab;
			} else if (i >= mSelectedTab) {
				return i + 1;
			} else {
				return i;
			}
		}
	}

	@Override
	void measureChildBeforeLayout(View child, int childIndex, int widthMeasureSpec, int totalWidth,
	                              int heightMeasureSpec, int totalHeight) {
		if (!isMeasureWithLargestChildEnabled() && mImposedTabsHeight >= 0) {
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(
			    totalWidth + mImposedTabWidths[childIndex], MeasureSpec.EXACTLY);
			heightMeasureSpec = MeasureSpec.makeMeasureSpec(mImposedTabsHeight,
			                                                MeasureSpec.EXACTLY);
		}

		super.measureChildBeforeLayout(child, childIndex,
		                               widthMeasureSpec, totalWidth, heightMeasureSpec, totalHeight);
	}

	@Override
	void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
		if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
			super.measureHorizontal(widthMeasureSpec, heightMeasureSpec);
			return;
		}

		// First, measure with no constraint
		final int width = MeasureSpec.getSize(widthMeasureSpec);
		/* AOSP MeasureSpec.makeSafeMeasureSpec(), which is makeMeasureSpec() for targetSdk >= M */
		final int unspecifiedWidth = MeasureSpec.makeMeasureSpec(width, MeasureSpec.UNSPECIFIED);
		mImposedTabsHeight = -1;
		super.measureHorizontal(unspecifiedWidth, heightMeasureSpec);

		int extraWidth = getMeasuredWidth() - width;
		if (extraWidth > 0) {
			final int count = getChildCount();

			int childCount = 0;
			for (int i = 0; i < count; i++) {
				final View child = getChildAt(i);
				if (child.getVisibility() == GONE)
					continue;
				childCount++;
			}

			if (childCount > 0) {
				if (mImposedTabWidths == null || mImposedTabWidths.length != count) {
					mImposedTabWidths = new int[count];
				}
				for (int i = 0; i < count; i++) {
					final View child = getChildAt(i);
					if (child.getVisibility() == GONE)
						continue;
					final int childWidth = child.getMeasuredWidth();
					final int delta = extraWidth / childCount;
					final int newWidth = Math.max(0, childWidth - delta);
					mImposedTabWidths[i] = newWidth;
					// Make sure the extra width is evenly distributed, no int division remainder
					extraWidth -= childWidth - newWidth; // delta may have been clamped
					childCount--;
					mImposedTabsHeight = Math.max(mImposedTabsHeight, child.getMeasuredHeight());
				}
			}
		}

		// Measure again, this time with imposed tab widths and respecting
		// initial spec request.
		super.measureHorizontal(widthMeasureSpec, heightMeasureSpec);
	}

	/** AOSP getChildTabViewAt(): the indicator of tab index (the children are the tab indicators) */
	public View getChildTabViewAt(int index) {
		return getChildAt(index);
	}

	/** AOSP getTabCount() */
	public int getTabCount() {
		return getChildCount();
	}

	@Override
	public void setDividerDrawable(Drawable drawable) {
		super.setDividerDrawable(drawable);
	}

	/** AOSP setDividerDrawable(int resId) */
	public void setDividerDrawable(int resId) {
		setDividerDrawable(getContext().getDrawable(resId));
	}

	public void setLeftStripDrawable(Drawable drawable) {
		mLeftStrip = drawable;
		requestLayout();
		invalidate();
	}

	public void setLeftStripDrawable(int resId) {
		setLeftStripDrawable(getContext().getDrawable(resId));
	}

	public Drawable getLeftStripDrawable() {
		return mLeftStrip;
	}

	public void setRightStripDrawable(Drawable drawable) {
		mRightStrip = drawable;
		requestLayout();
		invalidate();
	}

	public void setRightStripDrawable(int resId) {
		setRightStripDrawable(getContext().getDrawable(resId));
	}

	public Drawable getRightStripDrawable() {
		return mRightStrip;
	}

	public void setStripEnabled(boolean stripEnabled) {
		mDrawBottomStrips = stripEnabled;
		invalidate();
	}

	public boolean isStripEnabled() {
		return mDrawBottomStrips;
	}

	@Override
	public void childDrawableStateChanged(View child) {
		if (getTabCount() > 0 && child == getChildTabViewAt(mSelectedTab)) {
			// To make sure that the bottom strip is redrawn
			invalidate();
		}
		super.childDrawableStateChanged(child);
	}

	@Override
	public void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		// Do nothing if there are no tabs.
		if (getTabCount() == 0)
			return;

		// If the user specified a custom view for the tab indicators, then
		// do not draw the bottom strips.
		if (!mDrawBottomStrips) {
			// Skip drawing the bottom strips.
			return;
		}

		final View selectedChild = getChildTabViewAt(mSelectedTab);

		final Drawable leftStrip = mLeftStrip;
		final Drawable rightStrip = mRightStrip;

		if (leftStrip != null) {
			leftStrip.setState(selectedChild.getDrawableState());
		}
		if (rightStrip != null) {
			rightStrip.setState(selectedChild.getDrawableState());
		}

		if (mStripMoved) {
			final Rect bounds = mBounds;
			bounds.left = selectedChild.getLeft();
			bounds.right = selectedChild.getRight();
			final int myHeight = getHeight();
			if (leftStrip != null) {
				leftStrip.setBounds(Math.min(0, bounds.left - leftStrip.getIntrinsicWidth()),
				                    myHeight - leftStrip.getIntrinsicHeight(), bounds.left, myHeight);
			}
			if (rightStrip != null) {
				rightStrip.setBounds(bounds.right, myHeight - rightStrip.getIntrinsicHeight(),
				                     Math.max(getWidth(), bounds.right + rightStrip.getIntrinsicWidth()),
				                     myHeight);
			}
			mStripMoved = false;
		}

		if (leftStrip != null) {
			leftStrip.draw(canvas);
		}
		if (rightStrip != null) {
			rightStrip.draw(canvas);
		}
	}

	/** AOSP setCurrentTab(): marks the indicator of the tab as selected, without giving it the focus */
	public void setCurrentTab(int index) {
		if (index < 0 || index >= getTabCount() || index == mSelectedTab) {
			return;
		}

		if (mSelectedTab != -1) {
			getChildTabViewAt(mSelectedTab).setSelected(false);
		}
		mSelectedTab = index;
		getChildTabViewAt(mSelectedTab).setSelected(true);
		mStripMoved = true;
	}

	@Override
	public CharSequence getAccessibilityClassName() {
		return TabWidget.class.getName();
	}

	/** AOSP focusCurrentTab(): setCurrentTab() and the focus to the indicator if the tab changed */
	public void focusCurrentTab(int index) {
		final int oldTab = mSelectedTab;

		// set the tab
		setCurrentTab(index);

		// change the focus if applicable.
		if (oldTab != index) {
			getChildTabViewAt(index).requestFocus();
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		final int count = getTabCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildTabViewAt(i);
			child.setEnabled(enabled);
		}
	}

	@Override
	public void addView(View child) {
		if (child.getLayoutParams() == null) {
			final LinearLayout.LayoutParams lp = new LayoutParams(
			    0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
			lp.setMargins(0, 0, 0, 0);
			child.setLayoutParams(lp);
		}

		// Ensure you can navigate to the tab with the keyboard, and you can touch it
		child.setFocusable(true);
		child.setClickable(true);

		super.addView(child);

		// TODO: detect this via geometry with a tabwidget listener rather
		// than potentially interfere with the view's listener
		child.setOnClickListener(new TabClickListener(getTabCount() - 1));
	}

	@Override
	public void removeAllViews() {
		super.removeAllViews();
		mSelectedTab = -1;
	}

	/** AOSP setTabSelectionListener() (package-private): TabHost's listener for clicks on an indicator */
	void setTabSelectionListener(OnTabSelectionChanged listener) {
		mSelectionChangedListener = listener;
	}

	/* AOSP TabWidget.onFocusChange() (View.OnFocusChangeListener): "No-op. Tab selection is separate from
	 * keyboard focus." */
	@Override
	public void onFocusChange(View v, boolean hasFocus) {
	}

	// registered with each tab indicator so we can notify tab host
	private class TabClickListener implements OnClickListener {
		private final int mTabIndex;

		private TabClickListener(int tabIndex) {
			mTabIndex = tabIndex;
		}

		public void onClick(View v) {
			mSelectionChangedListener.onTabSelectionChanged(mTabIndex, true);
		}
	}

	/**
	 * Lets {@link TabHost} know that the user clicked on a tab indicator.
	 */
	interface OnTabSelectionChanged {
		/**
		 * Informs the TabHost which tab was selected. It also indicates
		 * if the tab was clicked/pressed or just focused into.
		 *
		 * @param tabIndex index of the tab that was selected
		 * @param clicked whether the selection changed due to a touch/click or
		 *                due to focus entering the tab through navigation.
		 */
		void onTabSelectionChanged(int tabIndex, boolean clicked);
	}
}
