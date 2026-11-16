package android.text.style;

import android.graphics.Paint;
import android.text.TextPaint;

public interface LineHeightSpan extends ParagraphStyle, WrapTogetherSpan {
	/* AOSP: how the span changes the height of the lines it covers */
	public void chooseHeight(CharSequence text, int start, int end,
	                         int spanstartv, int lineHeight,
	                         Paint.FontMetricsInt fm);

	/**
	 * The classes that affect the line height of paragraph with respect to density,
	 * should implement this interface.
	 */
	public interface WithDensity extends LineHeightSpan {

		/**
		 * Classes that implement this should define how the height is being calculated.
		 *
		 * @param text       the text
		 * @param start      the start of the line
		 * @param end        the end of the line
		 * @param spanstartv the start of the span
		 * @param lineHeight the line height
		 * @param paint      the paint
		 */
		public void chooseHeight(CharSequence text, int start, int end,
		                         int spanstartv, int lineHeight,
		                         Paint.FontMetricsInt fm, TextPaint paint);
	}

	/* AOSP core/java/android/text/style/LineHeightSpan.java, Standard: sets the line height in pixels by scaling
	 * descent and ascent (without the Parcel constructor and writeToParcel) */
	class Standard implements LineHeightSpan {
		private final int mHeight;

		public Standard(int height) {
			if (height <= 0)
				throw new IllegalArgumentException("Height: " + height + " must be positive");
			mHeight = height;
		}

		/* AOSP: the height is read as an int */
		public Standard(android.os.Parcel src) {
			mHeight = src.readInt();
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
