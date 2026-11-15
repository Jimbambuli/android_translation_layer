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
}
