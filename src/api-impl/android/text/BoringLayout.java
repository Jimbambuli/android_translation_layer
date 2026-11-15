package android.text;

import android.graphics.Paint;
import android.text.style.ParagraphStyle;

/**
 * A BoringLayout is a very simple Layout implementation for text that fits on a single line and is all
 * left-to-right characters. As AOSP's, it knows its width from isBoring() and makes a Layout from the Metrics.
 * ATL's Layout (Pango) does the drawing; AOSP's own lays out the single line with TextLine.
 */
public class BoringLayout extends Layout implements TextUtils.EllipsizeCallback {
	private static final char FIRST_RIGHT_TO_LEFT = '֐';

	private boolean mUseFallbackLineSpacing;
	private int mEllipsizedWidth;

	public static BoringLayout make(CharSequence source, TextPaint paint, int outerWidth, Layout.Alignment align, float spacingmult, float spacingadd, BoringLayout.Metrics metrics, boolean includePad) {
		return new BoringLayout(source, paint, outerWidth, align, spacingmult, spacingadd, metrics, includePad);
	}

	public static BoringLayout make(CharSequence source, TextPaint paint, int outerWidth, Layout.Alignment align, float spacingmult, float spacingadd, BoringLayout.Metrics metrics, boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
		return new BoringLayout(source, paint, outerWidth, align, spacingmult, spacingadd, metrics, includePad, ellipsize, ellipsizedWidth);
	}

	/* API 33 */
	public static BoringLayout make(CharSequence source, TextPaint paint, int outerWidth, Layout.Alignment align, BoringLayout.Metrics metrics, boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth, boolean useFallbackLineSpacing) {
		return new BoringLayout(source, paint, outerWidth, align, 1f, 0f, metrics, includePad, ellipsize, ellipsizedWidth, useFallbackLineSpacing);
	}

	/* AOSP re-uses this layout; here a new one is the same thing */
	public BoringLayout replaceOrMake(CharSequence source, TextPaint paint, int outerwidth, Layout.Alignment align, float spacingmult, float spacingadd, BoringLayout.Metrics metrics, boolean includePad) {
		return make(source, paint, outerwidth, align, spacingmult, spacingadd, metrics, includePad);
	}

	public BoringLayout replaceOrMake(CharSequence source, TextPaint paint, int outerWidth, Layout.Alignment align, float spacingMult, float spacingAdd, BoringLayout.Metrics metrics, boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
		return make(source, paint, outerWidth, align, spacingMult, spacingAdd, metrics, includePad, ellipsize, ellipsizedWidth);
	}

	/* API 33 */
	public BoringLayout replaceOrMake(CharSequence source, TextPaint paint, int outerWidth, Layout.Alignment align, BoringLayout.Metrics metrics, boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth, boolean useFallbackLineSpacing) {
		return make(source, paint, outerWidth, align, metrics, includePad, ellipsize, ellipsizedWidth, useFallbackLineSpacing);
	}

	public BoringLayout(CharSequence source, TextPaint paint, int outerwidth, Layout.Alignment align, float spacingMult, float spacingAdd, BoringLayout.Metrics metrics, boolean includePad) {
		super(source, paint, outerwidth, align, spacingMult, spacingAdd);
		mEllipsizedWidth = outerwidth;
	}

	public BoringLayout(CharSequence source, TextPaint paint, int outerWidth, Layout.Alignment align, float spacingMult, float spacingAdd, BoringLayout.Metrics metrics, boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
		this(source, paint, outerWidth, align, spacingMult, spacingAdd, metrics, includePad);
		/* AOSP: the single line is ellipsized to ellipsizedWidth when it is wider */
		if (ellipsize != null && metrics != null && metrics.width > ellipsizedWidth) {
			setMaxLines(1, ellipsize);
			mEllipsizedWidth = ellipsizedWidth;
		}
	}

	/* API 33 */
	public BoringLayout(CharSequence source, TextPaint paint, int outerWidth, Layout.Alignment align, float spacingMult, float spacingAdd, BoringLayout.Metrics metrics, boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth, boolean useFallbackLineSpacing) {
		this(source, paint, outerWidth, align, spacingMult, spacingAdd, metrics, includePad, ellipsize, ellipsizedWidth);
		mUseFallbackLineSpacing = useFallbackLineSpacing;
	}

	/* AOSP BoringLayout.ellipsized(int, int): TextUtils.EllipsizeCallback, stores the ellipsized range
	 * (ATL's getEllipsisStart()/getEllipsisCount() come from the Pango layout of Layout) */
	private int mEllipsizedStart;
	private int mEllipsizedCount;

	public void ellipsized(int start, int end) {
		mEllipsizedStart = start;
		mEllipsizedCount = end - start;
	}

	@Override
	public int getEllipsizedWidth() {
		return mEllipsizedWidth;
	}

	public boolean isFallbackLineSpacingEnabled() {
		return mUseFallbackLineSpacing;
	}

	/**
	 * Metrics of a boring text: the font metrics of the paint and the width of the line.
	 */
	public static class Metrics extends Paint.FontMetricsInt {
		public int width;

		@Override
		public String toString() {
			return super.toString() + " width=" + width;
		}

		private void reset() {
			top = 0;
			bottom = 0;
			ascent = 0;
			descent = 0;
			width = 0;
			leading = 0;
		}
	}

	public static Metrics isBoring(CharSequence text, TextPaint paint) {
		return isBoring(text, paint, TextDirectionHeuristics.FIRSTSTRONG_LTR, null);
	}

	public static Metrics isBoring(CharSequence text, TextPaint paint, Metrics metrics) {
		return isBoring(text, paint, TextDirectionHeuristics.FIRSTSTRONG_LTR, metrics);
	}

	/* API 33 */
	public static Metrics isBoring(CharSequence text, TextPaint paint, TextDirectionHeuristic textDir, boolean useFallbackLineSpacing, Metrics metrics) {
		return isBoring(text, paint, textDir, metrics);
	}

	/**
	 * Returns null if not boring; the width, ascent, and descent in the provided Metrics object (or a new one if the
	 * provided one was null) if boring: one line (no newline, no tab), no right-to-left characters and no ParagraphStyle.
	 */
	public static Metrics isBoring(CharSequence text, TextPaint paint, TextDirectionHeuristic textDir, Metrics metrics) {
		final int textLength = text.length();
		for (int i = 0; i < textLength; i++) {
			char c = text.charAt(i);
			if (c == '\n' || c == '\t' || c >= FIRST_RIGHT_TO_LEFT)
				return null;
		}
		if (textDir != null && textDir.isRtl(text, 0, textLength))
			return null;
		if (text instanceof Spanned) {
			Spanned sp = (Spanned)text;
			Object[] styles = sp.getSpans(0, textLength, ParagraphStyle.class);
			if (styles.length > 0)
				return null;
		}
		Metrics fm = metrics;
		if (fm == null)
			fm = new Metrics();
		else
			fm.reset();
		paint.getFontMetricsInt(fm);
		/* the width with the character spans applied, as the Layout draws the line */
		if (text instanceof Spanned) {
			Layout line = new Layout(text, paint, -1, Layout.Alignment.ALIGN_NORMAL, 1f, 0f) {};
			fm.width = (int)Math.ceil(line.getLineWidth(0));
		} else {
			fm.width = (int)Math.ceil(Layout.getDesiredWidth(text, paint));
		}
		return fm;
	}
}
