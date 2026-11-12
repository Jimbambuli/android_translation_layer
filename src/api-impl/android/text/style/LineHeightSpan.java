package android.text.style;

import android.graphics.Paint;

public interface LineHeightSpan extends ParagraphStyle, WrapTogetherSpan {
	/* AOSP: how the span changes the height of the lines it covers */
	public void chooseHeight(CharSequence text, int start, int end,
	                         int spanstartv, int lineHeight,
	                         Paint.FontMetricsInt fm);
}
