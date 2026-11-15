package android.text;

import android.text.style.MetricAffectingSpan;
import java.util.Objects;

/*
 * AOSP PrecomputedText's API without the precomputation: ATL measures text with Pango when the view is
 * measured, so the text is only kept with the Params it was created for (TextView.getTextMetricsParams(),
 * PrecomputedText.create(), PrecomputedTextCompat). The paragraph measurements of AOSP do not exist here:
 * the whole text is one paragraph.
 */
public class PrecomputedText implements Spannable {
	public static final class Params {
		private final TextPaint mPaint;
		private final TextDirectionHeuristic mTextDir;
		private final int mBreakStrategy;
		private final int mHyphenationFrequency;

		public static class Builder {
			private final TextPaint mPaint;
			private TextDirectionHeuristic mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
			private int mBreakStrategy = 1 /* Layout.BREAK_STRATEGY_HIGH_QUALITY */;
			private int mHyphenationFrequency = 1 /* Layout.HYPHENATION_FREQUENCY_NORMAL */;

			public Builder(TextPaint paint) {
				mPaint = paint;
			}

			public Builder setBreakStrategy(int strategy) {
				mBreakStrategy = strategy;
				return this;
			}

			public Builder setHyphenationFrequency(int frequency) {
				mHyphenationFrequency = frequency;
				return this;
			}

			public Builder setTextDirection(TextDirectionHeuristic textDir) {
				mTextDir = textDir;
				return this;
			}

			public Params build() {
				return new Params(mPaint, mTextDir, mBreakStrategy, mHyphenationFrequency);
			}
		}

		Params(TextPaint paint, TextDirectionHeuristic textDir, int strategy, int frequency) {
			mPaint = paint;
			mTextDir = textDir;
			mBreakStrategy = strategy;
			mHyphenationFrequency = frequency;
		}

		public TextPaint getTextPaint() { return mPaint; }
		public TextDirectionHeuristic getTextDirection() { return mTextDir; }
		public int getBreakStrategy() { return mBreakStrategy; }
		public int getHyphenationFrequency() { return mHyphenationFrequency; }

		/* AOSP: equal when the text would be measured the same way (the paint's metrics-relevant values) */
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof Params))
				return false;
			Params p = (Params)o;
			return mPaint.getTextSize() == p.mPaint.getTextSize() && mPaint.getTypeface() == p.mPaint.getTypeface()
			    && mBreakStrategy == p.mBreakStrategy && mHyphenationFrequency == p.mHyphenationFrequency
			    && Objects.equals(mTextDir, p.mTextDir);
		}

		public int hashCode() {
			return Objects.hash(mPaint.getTextSize(), mPaint.getTypeface(), mBreakStrategy, mHyphenationFrequency, mTextDir);
		}

		public String toString() {
			return "{textSize=" + mPaint.getTextSize() + ", breakStrategy=" + mBreakStrategy + ", hyphenationFrequency=" + mHyphenationFrequency + "}";
		}
	}

	private final SpannableString mText;
	private final Params mParams;
	/* AOSP ParagraphInfo.paragraphEnd of each paragraph */
	private final int[] mParagraphEnds;

	public static PrecomputedText create(CharSequence text, Params params) {
		return new PrecomputedText(text, params);
	}

	private PrecomputedText(CharSequence text, Params params) {
		mText = new SpannableString(text);
		mParams = params;
		mParagraphEnds = paragraphEnds(mText);
	}

	/* AOSP PrecomputedText.createMeasuredParagraphs(): a paragraph ends after each LINE_FEED or at the
	 * end of the text; an empty text has no paragraph */
	private static int[] paragraphEnds(CharSequence text) {
		final int end = text.length();
		int count = 0;
		for (int paraStart = 0, paraEnd; paraStart < end; paraStart = paraEnd, count++) {
			paraEnd = TextUtils.indexOf(text, '\n', paraStart, end);
			paraEnd = paraEnd < 0 ? end : paraEnd + 1;
		}
		int[] ends = new int[count];
		int i = 0;
		for (int paraStart = 0, paraEnd; paraStart < end; paraStart = paraEnd) {
			paraEnd = TextUtils.indexOf(text, '\n', paraStart, end);
			paraEnd = paraEnd < 0 ? end : paraEnd + 1;
			ends[i++] = paraEnd;
		}
		return ends;
	}

	/* AOSP Preconditions.checkArgumentInRange(paraIndex, 0, getParagraphCount(), "paraIndex") */
	private void checkParaIndex(int paraIndex) {
		if (paraIndex < 0 || paraIndex > mParagraphEnds.length)
			throw new IllegalArgumentException(String.format(java.util.Locale.US,
			    "%s is out of range of [%d, %d] (too %s)", "paraIndex", 0, mParagraphEnds.length,
			    paraIndex < 0 ? "low" : "high"));
	}

	public Params getParams() { return mParams; }
	public int getParagraphCount() { return mParagraphEnds.length; }

	public int getParagraphStart(int paraIndex) {
		checkParaIndex(paraIndex);
		return paraIndex == 0 ? 0 : getParagraphEnd(paraIndex - 1);
	}

	public int getParagraphEnd(int paraIndex) {
		checkParaIndex(paraIndex);
		return mParagraphEnds[paraIndex];
	}

	public int getSpanStart(Object tag) { return mText.getSpanStart(tag); }
	public int getSpanEnd(Object tag) { return mText.getSpanEnd(tag); }
	public int getSpanFlags(Object tag) { return mText.getSpanFlags(tag); }
	public <T> T[] getSpans(int start, int end, Class<T> type) { return mText.getSpans(start, end, type); }
	public int nextSpanTransition(int start, int limit, Class type) { return mText.nextSpanTransition(start, limit, type); }
	public void setSpan(Object what, int start, int end, int flags) { mText.setSpan(what, start, end, flags); }
	public void removeSpan(Object what) { mText.removeSpan(what); }
	public char charAt(int index) { return mText.charAt(index); }
	public int length() { return mText.length(); }
	public CharSequence subSequence(int start, int end) { return mText.subSequence(start, end); }
	public String toString() { return mText.toString(); }
}
