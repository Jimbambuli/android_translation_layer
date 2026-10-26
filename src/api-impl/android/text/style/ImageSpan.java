package android.text.style;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class ImageSpan extends DynamicDrawableSpan {

	public ImageSpan(Context context, int resId) {}

	private Drawable drawable;

	public ImageSpan(Drawable d) {
		drawable = d;
	}

	private String mSource;

	public ImageSpan(Drawable d, String source) {
		drawable = d;
		mSource = source;
	}

	public String getSource() {
		return mSource;
	}

	public ImageSpan(Drawable d, int verticalAlignment) {
		drawable = d;
	}

	public Drawable getDrawable() {
		return drawable;
	}

	@Override
	public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
		return drawable.getIntrinsicWidth();
	}
}
