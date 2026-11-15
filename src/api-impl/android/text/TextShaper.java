package android.text;

import android.graphics.text.PositionedGlyphs;
import android.graphics.text.TextRunShaper;
import android.text.style.MetricAffectingSpan;
import java.text.Bidi;

/**
 * Provides text shaping for multi-styled text (AOSP android.text.TextShaper).
 *
 * AOSP walks the line with TextLine: bidi runs in visual order, each split at MetricAffectingSpan
 * boundaries, shaped with TextRunShaper and handed to the consumer with the running x offset.
 * ATL has no TextLine/MeasuredParagraph, so the same walk is done here with java.text.Bidi.
 * Tab stops and ellipsis are not supported, as in AOSP.
 */
public class TextShaper {
	private TextShaper() {}

	/**
	 * A consumer interface for accepting text shape result.
	 */
	public interface GlyphsConsumer {
		void accept(int start, int count, PositionedGlyphs glyphs, TextPaint paint);
	}

	public static void shapeText(CharSequence text, int start, int count, TextDirectionHeuristic dir,
	                             TextPaint paint, GlyphsConsumer consumer) {
		final int end = start + count;
		final boolean paraRtl = dir.isRtl(text, start, count);
		char[] chars = new char[count];
		TextUtils.getChars(text, start, end, chars, 0);
		Bidi bidi = new Bidi(chars, 0, null, 0, count,
		                     paraRtl ? Bidi.DIRECTION_RIGHT_TO_LEFT : Bidi.DIRECTION_LEFT_TO_RIGHT);
		final int runCount = bidi.getRunCount();
		byte[] levels = new byte[runCount];
		Integer[] runs = new Integer[runCount];
		for (int i = 0; i < runCount; i++) {
			levels[i] = (byte)bidi.getRunLevel(i);
			runs[i] = i;
		}
		Bidi.reorderVisually(levels, 0, runs, 0, runCount);

		float x = 0;
		for (int v = 0; v < runCount; v++) {
			final int run = runs[v];
			final int runStart = start + bidi.getRunStart(run);
			final int runEnd = start + bidi.getRunLimit(run);
			final boolean runRtl = (bidi.getRunLevel(run) & 1) != 0;
			x += shapeRun(text, runStart, runEnd, start, end, runRtl, x, paint, consumer);
		}
	}

	/** Shapes one bidi run split at MetricAffectingSpan boundaries; returns its advance. */
	private static float shapeRun(CharSequence text, int runStart, int runEnd, int ctxStart, int ctxEnd,
	                              boolean runRtl, float x, TextPaint paint, GlyphsConsumer consumer) {
		Spanned spanned = text instanceof Spanned ? (Spanned)text : null;
		java.util.ArrayList<int[]> pieces = new java.util.ArrayList<>();
		for (int s = runStart; s < runEnd;) {
			int e = spanned == null ? runEnd
			                        : spanned.nextSpanTransition(s, runEnd, MetricAffectingSpan.class);
			pieces.add(new int[] {s, e});
			s = e;
		}
		float advance = 0;
		for (int i = 0; i < pieces.size(); i++) {
			int[] p = pieces.get(runRtl ? pieces.size() - 1 - i : i);
			TextPaint wp = paint;
			if (spanned != null) {
				MetricAffectingSpan[] spans = spanned.getSpans(p[0], p[1], MetricAffectingSpan.class);
				if (spans.length > 0) {
					wp = new TextPaint();
					wp.set(paint);
					for (MetricAffectingSpan span : spans)
						span.updateMeasureState(wp);
				}
			}
			PositionedGlyphs glyphs = TextRunShaper.shapeTextRun(text, p[0], p[1] - p[0], ctxStart,
			                                                     ctxEnd - ctxStart, x + advance, 0, runRtl, wp);
			consumer.accept(p[0], p[1] - p[0], glyphs, wp);
			advance += glyphs.getAdvance();
		}
		return advance;
	}
}
