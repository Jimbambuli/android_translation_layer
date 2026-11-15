package android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class Toolbar extends View {

	/* the title is not rendered; only stored so the getters return what was set */
	private CharSequence titleText;
	private int titleMarginStart;
	private int titleMarginTop;
	private int titleMarginEnd;
	private int titleMarginBottom;

	public Toolbar(Context context) {
		super(context);
	}

	public Toolbar(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}

	public Toolbar(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public Toolbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public CharSequence getTitle() {
		return titleText;
	}

	/* AOSP getSubtitle()/setSubtitle(): kept like the title, not rendered */
	private CharSequence subtitleText;

	public CharSequence getSubtitle() {
		return subtitleText;
	}

	public void setSubtitle(int resId) {
		setSubtitle(getContext().getText(resId));
	}

	public void setSubtitle(CharSequence subtitle) {
		subtitleText = subtitle;
	}

	public int getTitleMarginStart() {
		return titleMarginStart;
	}

	public int getTitleMarginTop() {
		return titleMarginTop;
	}

	public int getTitleMarginEnd() {
		return titleMarginEnd;
	}

	public int getTitleMarginBottom() {
		return titleMarginBottom;
	}

	/* AOSP setTitle(): stored like the subtitle, not rendered */
	public void setTitle(int resId) {
		setTitle(getContext().getText(resId));
	}

	public void setTitle(CharSequence title) {
		titleText = title;
	}

	/* AOSP navigation icon, overflow icon and navigation content description: stored (there is no navigation
	 * button or overflow menu in this Toolbar) so the getters return what was set */
	private Drawable navigationIcon;
	private Drawable overflowIcon;
	private CharSequence navigationContentDescription;

	public void setNavigationIcon(int resId) {
		setNavigationIcon(resId != 0 ? getContext().getDrawable(resId) : null);
	}

	public void setNavigationIcon(Drawable icon) {
		navigationIcon = icon;
	}

	public Drawable getNavigationIcon() {
		return navigationIcon;
	}

	public void setOverflowIcon(Drawable icon) {
		overflowIcon = icon;
	}

	public Drawable getOverflowIcon() {
		return overflowIcon;
	}

	public void setNavigationContentDescription(int resId) {
		setNavigationContentDescription(resId != 0 ? getContext().getText(resId) : null);
	}

	public void setNavigationContentDescription(CharSequence description) {
		navigationContentDescription = description;
	}

	public CharSequence getNavigationContentDescription() {
		return navigationContentDescription;
	}

	public void setNavigationOnClickListener(OnClickListener listener) {}

	/* AOSP content insets: kept (ATL lays out no toolbar content), the getters return what was set; AOSP's
	 * RtlSpacingHelper.UNDEFINED (Integer.MIN_VALUE) stands for "not set" and reads as 0 */
	private int mContentInsetStart;
	private int mContentInsetEnd;
	private int mContentInsetStartWithNavigation;
	private int mContentInsetEndWithActions;

	public void setContentInsetsRelative(int contentInsetStart, int contentInsetEnd) {
		mContentInsetStart = contentInsetStart;
		mContentInsetEnd = contentInsetEnd;
	}

	public int getContentInsetStart() {
		return mContentInsetStart;
	}

	public int getContentInsetEnd() {
		return mContentInsetEnd;
	}

	public void setContentInsetStartWithNavigation(int insetStartWithNavigation) {
		if (insetStartWithNavigation < 0) {
			insetStartWithNavigation = 0;
		}
		mContentInsetStartWithNavigation = insetStartWithNavigation;
	}

	public int getContentInsetStartWithNavigation() {
		return mContentInsetStartWithNavigation;
	}

	public void setContentInsetEndWithActions(int insetEndWithActions) {
		if (insetEndWithActions < 0) {
			insetEndWithActions = 0;
		}
		mContentInsetEndWithActions = insetEndWithActions;
	}

	public int getContentInsetEndWithActions() {
		return mContentInsetEndWithActions;
	}

	/* AOSP Toolbar.LayoutParams */
	public static class LayoutParams extends android.app.ActionBar.LayoutParams {
		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);
		}

		public LayoutParams(int width, int height) {
			super(width, height);
			this.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START;
		}

		public LayoutParams(int width, int height, int gravity) {
			super(width, height);
			this.gravity = gravity;
		}

		public LayoutParams(int gravity) {
			this(WRAP_CONTENT, MATCH_PARENT, gravity);
		}

		public LayoutParams(LayoutParams source) {
			super(source);
		}

		public LayoutParams(android.app.ActionBar.LayoutParams source) {
			super(source);
		}

		public LayoutParams(MarginLayoutParams source) {
			super(source);
			// ActionBar.LayoutParams has no MarginLayoutParams constructor (as in AOSP): copy the margins
			copyMarginsFrom(source);
		}

		public LayoutParams(android.view.ViewGroup.LayoutParams source) {
			super(source);
		}
	}
}
