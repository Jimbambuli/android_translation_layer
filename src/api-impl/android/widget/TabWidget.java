package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * AOSP TabWidget is a LinearLayout holding the tab indicators (android16 core/java/android/widget/TabWidget.java).
 * ATL keeps the container and the child lookup; the selection state and the strip drawing are not done (TabHost has no
 * widget connection either).
 */
public class TabWidget extends LinearLayout {
	public TabWidget(Context context) {
		this(context, null);
	}

	public TabWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFocusable(true);
	}

	public TabWidget(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public TabWidget(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		setFocusable(true);
	}

	/** AOSP getChildTabViewAt(): the indicator of tab index (the children are the tab indicators) */
	public View getChildTabViewAt(int index) {
		return getChildAt(index);
	}

	/** AOSP getTabCount() */
	public int getTabCount() {
		return getChildCount();
	}

	public void setDividerDrawable(android.graphics.drawable.Drawable drawable) {}

	/** AOSP setDividerDrawable(int resId) */
	public void setDividerDrawable(int resId) {
		setDividerDrawable(getContext().getDrawable(resId));
	}

	public void setStripEnabled(boolean stripEnabled) {}
}
