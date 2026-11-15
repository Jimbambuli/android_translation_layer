package android.text.style;

import android.graphics.Paint;

public interface LineHeightSpan extends ParagraphStyle, WrapTogetherSpan {
	/* AOSP: how the span changes the height of the lines it covers */
	public void chooseHeight(CharSequence text, int start, int end,
	                         int spanstartv, int lineHeight,
	                         Paint.FontMetricsInt fm);

	/* AOSP core/java/android/text/style/LineHeightSpan.java, Standard: sets the line height in pixels by scaling
	 * descent and ascent (without the Parcel constructor and writeToParcel) */
	class Standard implements LineHeightSpan {
		private final int mHeight;

		public Standard(int height) {
			if (height <= 0)
				throw new IllegalArgumentException("Height: " + height + " must be positive");
			mHeight = height;
		}

		public int getHeight() {
			return mHeight;
		}

		@Override
		public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int lineHeight,
		                         Paint.FontMetricsInt fm) {
			final int originHeight = fm.descent - fm.ascent;
			// If original height is not positive, do nothing.
			if (originHeight <= 0) {
				return;
			}
			final float ratio = mHeight * 1.0f / originHeight;
			fm.descent = Math.round(fm.descent * ratio);
			fm.ascent = fm.descent - mHeight;
		}
	}
}
