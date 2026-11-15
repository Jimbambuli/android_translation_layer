package android.widget;

import android.content.Context;
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

	public void setNavigationIcon(int resId) {}

	public void setNavigationOnClickListener(OnClickListener listener) {}
}
