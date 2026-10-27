package android.text.style;

import android.content.Context;
import android.text.TextPaint;

public class TextAppearanceSpan extends MetricAffectingSpan {

	public TextAppearanceSpan(Context context, int resId) {}

	// ATL: the appearance itself is not applied (stub)
	@Override
	public void updateDrawState(TextPaint ds) {}

	@Override
	public void updateMeasureState(TextPaint ds) {}
}
