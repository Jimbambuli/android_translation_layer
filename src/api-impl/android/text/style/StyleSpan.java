package android.text.style;

import android.text.ParcelableSpan;

public class StyleSpan extends MetricAffectingSpan implements ParcelableSpan {

	private int style;

	private final int mFontWeightAdjustment;

	public StyleSpan(int style) {
		this(style, android.content.res.Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED);
	}

	public StyleSpan(int style, int fontWeightAdjustment) {
		this.style = style;
		mFontWeightAdjustment = fontWeightAdjustment;
	}

	public int getFontWeightAdjustment() {
		return mFontWeightAdjustment;
	}

	public int getStyle() {
		return style;
	}
}
