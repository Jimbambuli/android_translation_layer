package android.text;

import android.atl.GskCanvas;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.LineHeightSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.UnderlineSpan;
import java.util.ArrayList;

public class Layout {

	public enum Alignment {
		ALIGN_NORMAL,
		ALIGN_OPPOSITE,
		ALIGN_CENTER,
		ALIGN_LEFT,
		ALIGN_RIGHT,
	}

	public class Directions {}

	/* AOSP Layout.TextInclusionStrategy and the three strategies (android-16 Layout.java) */
	public interface TextInclusionStrategy {
		boolean isSegmentInside(android.graphics.RectF segmentBounds, android.graphics.RectF area);
	}

	public static final TextInclusionStrategy INCLUSION_STRATEGY_ANY_OVERLAP =
	    android.graphics.RectF::intersects;

	public static final TextInclusionStrategy INCLUSION_STRATEGY_CONTAINS_CENTER =
	    (segmentBounds, area) -> area.contains(segmentBounds.centerX(), segmentBounds.centerY());

	public static final TextInclusionStrategy INCLUSION_STRATEGY_CONTAINS_ALL =
	    (segmentBounds, area) -> area.contains(segmentBounds);

	long layout; // native PangoLayout; in paragraph mode the one of the first paragraph
	private CharSequence text;
	private TextPaint paint;
	private float spacing_mult;
	private float spacing_add;
	private Alignment align;
	private int maxLines = Integer.MAX_VALUE;
	private int width;

	protected Layout(CharSequence text, TextPaint paint, int width, Layout.Alignment align, float spacingMult, float spacingAdd) {
		this.paint = paint;
		this.spacing_mult = spacingMult;
		this.spacing_add = spacingAdd;
		this.align = align;
		build(text, 0, text != null ? text.length() : 0, width);
	}

	/* the text [start, end) of source at width (-1: no wrapping), for getDesiredWidth() */
	private Layout(CharSequence source, int start, int end, TextPaint paint) {
		measureOnly = true;
		this.paint = paint;
		this.spacing_mult = 1;
		this.align = Alignment.ALIGN_NORMAL;
		build(source, start, end, -1);
	}

	/* the Pango layout(s) of the text [start, end) of source; the spans keep their offsets in source, as AOSP hands
	 * source and its offsets to the spans */
	private void build(CharSequence source, int start, int end, int width) {
		this.text = source != null ? source.subSequence(start, end).toString() : "";
		this.width = width;
		replacementText = source instanceof Spanned ? (Spanned)source : null;
		findReplacementRuns(start, end);
		textStart = start;
		paras = null;
		if (replacementText != null && hasParagraphSpans(replacementText, start, end)) {
			buildParagraphs(start, end);
			return;
		}
		layout = native_constructor(pangoText(source, start, end), paint.paint, width);
		if (replacementText != null)
			applySpans(layout, start, start, end);
		setAlignment(align);
	}

	/* --- ReplacementSpan ---
	 * AOSP MeasuredParagraph.applyMetricsAffectingSpan() and TextLine.handleRun(): a paragraph is split at the
	 * transitions of its MetricAffectingSpans; a run that a ReplacementSpan covers is replaced, by the last such
	 * span (the other spans of the run change its paint), and getSize() and draw() are called for that run only.
	 * A span that crosses a transition is so measured and drawn once per run. */
	private Spanned replacementText;
	private ReplacementSpan[] runSpans;
	private int[] runStarts;
	private int[] runEnds;
	private int textStart; // the offset in replacementText of the Pango text

	private void findReplacementRuns(int start, int end) {
		ArrayList<ReplacementSpan> spans = new ArrayList<>();
		ArrayList<int[]> runs = new ArrayList<>();
		Spanned sp = replacementText;
		if (sp != null && sp.getSpans(start, end, ReplacementSpan.class).length > 0) {
			for (int p = start, pend; p < end; p = pend) {
				pend = TextUtils.indexOf(sp, '\n', p, end);
				pend = pend < 0 ? end : pend + 1;
				for (int s = p, next; s < pend; s = next) {
					next = sp.nextSpanTransition(s, pend, MetricAffectingSpan.class);
					ReplacementSpan replacement = null;
					for (MetricAffectingSpan span : sp.getSpans(s, next, MetricAffectingSpan.class)) {
						if (span instanceof ReplacementSpan && sp.getSpanStart(span) != sp.getSpanEnd(span))
							replacement = (ReplacementSpan)span;
					}
					if (replacement != null) {
						spans.add(replacement);
						runs.add(new int[] {s, next});
					}
				}
			}
		}
		runSpans = spans.toArray(new ReplacementSpan[0]);
		runStarts = new int[runs.size()];
		runEnds = new int[runs.size()];
		for (int i = 0; i < runs.size(); i++) {
			runStarts[i] = runs.get(i)[0];
			runEnds[i] = runs.get(i)[1];
		}
	}

	/* AOSP TextLine.set(): the first character of a replaced run is U+FFFC, the rest U+FEFF (zero width), so the
	 * text keeps its offsets and each run gets one placeholder; a line break stays one */
	private String pangoText(CharSequence source, int start, int end) {
		if (source == null)
			return "";
		char[] chars = source.subSequence(start, end).toString().toCharArray();
		for (int i = 0; i < runStarts.length; i++) {
			if (runStarts[i] < start || runStarts[i] >= end)
				continue;
			chars[runStarts[i] - start] = '￼';
			for (int j = runStarts[i] + 1; j < Math.min(runEnds[i], end); j++) {
				if (chars[j - start] != '\n')
					chars[j - start] = '﻿';
			}
		}
		return new String(chars);
	}

	/* AOSP: the paint of a replaced run with the other MetricAffectingSpans of the run applied, their measure state
	 * for getSize() (MeasuredParagraph.applyMetricsAffectingSpan()), their draw state for draw() (TextLine.handleRun()) */
	private TextPaint replacementPaint(int run, boolean draw) {
		TextPaint wp = new TextPaint();
		wp.set(paint);
		Spanned sp = replacementText;
		for (MetricAffectingSpan span : sp.getSpans(runStarts[run], runEnds[run], MetricAffectingSpan.class)) {
			if (span instanceof ReplacementSpan || sp.getSpanStart(span) == sp.getSpanEnd(span))
				continue;
			if (draw)
				span.updateDrawState(wp);
			else
				span.updateMeasureState(wp);
		}
		return wp;
	}

	/* each replaced run takes the width and the font metrics getSize() gives (AOSP
	 * MeasuredParagraph.applyReplacementRun(): the line grows to them); Pango leaves a gap of that size */
	private void applyReplacements(long pango, int base, int from, int to) {
		for (int i = 0; i < runSpans.length; i++) {
			if (runStarts[i] < from || runStarts[i] >= to)
				continue;
			TextPaint wp = replacementPaint(i, false);
			Paint.FontMetricsInt fm = wp.getFontMetricsInt();
			int size = runSpans[i].getSize(wp, replacementText, runStarts[i], runEnds[i], fm);
			native_add_placeholder(pango, runStarts[i] - base, size, fm.ascent, fm.descent);
		}
	}

	private void drawReplacements(Canvas canvas) {
		float[] place = new float[4];
		for (int i = 0; i < runSpans.length; i++) {
			int start = runStarts[i];
			if (paras == null) {
				if (!native_get_placeholder(layout, start - textStart, place))
					continue;
			} else {
				Para p = paraOfOffset(start);
				if (!native_get_placeholder(p.layout, start - p.start, place))
					continue;
				int line = getLineForOffset(start);
				place[0] += p.x;
				place[1] = getLineTop(line);
				place[2] = getLineBaseline(line);
				place[3] = getLineTop(line + 1);
			}
			runSpans[i].draw(canvas, replacementText, start, runEnds[i], place[0], Math.round(place[1]),
			                 Math.round(place[2]), Math.round(place[3]), replacementPaint(i, true));
		}
	}

	/* the character spans that change how the text looks become Pango attributes of the text [from, to) of
	 * replacementText, which starts at base in the Pango layout; then the placeholders of the replaced runs */
	private void applySpans(long pango, int base, int from, int to) {
		Spanned text = replacementText;
		for (CharacterStyle span : text.getSpans(from, to, CharacterStyle.class)) {
			int start = Math.max(text.getSpanStart(span), from) - base;
			int end = Math.min(text.getSpanEnd(span), to) - base;
			if (start < 0 || end <= start)
				continue;
			if (span instanceof AbsoluteSizeSpan) {
				AbsoluteSizeSpan size = (AbsoluteSizeSpan)span;
				float px = size.getDip() ? size.getSize() * paint.density : size.getSize();
				native_add_attribute(pango, ATTR_SIZE, start, end, 0, px);
			} else if (span instanceof RelativeSizeSpan) {
				native_add_attribute(pango, ATTR_SCALE, start, end, 0, ((RelativeSizeSpan)span).getSizeChange());
			} else if (span instanceof ForegroundColorSpan) {
				native_add_attribute(pango, ATTR_FOREGROUND, start, end, ((ForegroundColorSpan)span).getForegroundColor(), 0);
			} else if (span instanceof BackgroundColorSpan) {
				native_add_attribute(pango, ATTR_BACKGROUND, start, end, ((BackgroundColorSpan)span).getBackgroundColor(), 0);
			} else if (span instanceof StyleSpan) {
				int style = ((StyleSpan)span).getStyle();
				if ((style & Typeface.BOLD) != 0)
					native_add_attribute(pango, ATTR_BOLD, start, end, 0, 0);
				if ((style & Typeface.ITALIC) != 0)
					native_add_attribute(pango, ATTR_ITALIC, start, end, 0, 0);
			} else if (span instanceof TextAppearanceSpan) {
				/* AOSP TextAppearanceSpan.updateDrawState(): colour, size and style of the appearance */
				TextAppearanceSpan appearance = (TextAppearanceSpan)span;
				if (appearance.getTextColor() != null)
					native_add_attribute(pango, ATTR_FOREGROUND, start, end, appearance.getTextColor().getDefaultColor(), 0);
				if (appearance.getTextSize() > 0)
					native_add_attribute(pango, ATTR_SIZE, start, end, 0, appearance.getTextSize());
				if ((appearance.getTextStyle() & Typeface.BOLD) != 0)
					native_add_attribute(pango, ATTR_BOLD, start, end, 0, 0);
				if ((appearance.getTextStyle() & Typeface.ITALIC) != 0)
					native_add_attribute(pango, ATTR_ITALIC, start, end, 0, 0);
			} else if (span instanceof UnderlineSpan) {
				native_add_attribute(pango, ATTR_UNDERLINE, start, end, 0, 0);
			} else if (span instanceof StrikethroughSpan) {
				native_add_attribute(pango, ATTR_STRIKETHROUGH, start, end, 0, 0);
			} else if (!(span instanceof ReplacementSpan)) {
				/* any other span draws through updateDrawState() as in AOSP's TextLine (a ClickableSpan: the link
				 * colour of the paint and an underline); what it changes of the paint becomes an attribute */
				TextPaint wp = new TextPaint();
				wp.set(paint);
				span.updateDrawState(wp);
				if (wp.getColor() != paint.getColor())
					native_add_attribute(pango, ATTR_FOREGROUND, start, end, wp.getColor(), 0);
				if (wp.bgColor != 0)
					native_add_attribute(pango, ATTR_BACKGROUND, start, end, wp.bgColor, 0);
				if (wp.isUnderlineText() && !paint.isUnderlineText())
					native_add_attribute(pango, ATTR_UNDERLINE, start, end, 0, 0);
				if (wp.isStrikeThruText() && !paint.isStrikeThruText())
					native_add_attribute(pango, ATTR_STRIKETHROUGH, start, end, 0, 0);
			}
		}
		applyReplacements(pango, base, from, to);
	}

	private static final int ATTR_SIZE = 0;
	private static final int ATTR_SCALE = 1;
	private static final int ATTR_FOREGROUND = 2;
	private static final int ATTR_BACKGROUND = 3;
	private static final int ATTR_BOLD = 4;
	private static final int ATTR_ITALIC = 5;
	private static final int ATTR_UNDERLINE = 6;
	private static final int ATTR_STRIKETHROUGH = 7;
	private static final int ATTR_HYPHENS = 8;

	/* --- paragraph mode ---
	 * AOSP StaticLayout lays out each paragraph on its own: its LeadingMarginSpans narrow it and Layout.drawText()
	 * has them draw their margin per line, its LineHeightSpans choose the height of each line (StaticLayout.out()),
	 * its last AlignmentSpan aligns it, and LineBackgroundSpans draw behind each line (Layout.drawBackground()).
	 * Pango has one width, indent and alignment per layout, so a text with such spans gets one Pango layout per
	 * paragraph, placed by the line table below, which StaticLayout.out() fills the same way. */
	private static final class Para {
		long layout;
		int start; // offset of the paragraph in the text
		int end;   // after its line break, if it has one
		int x;     // the leading margin both of its first and its other lines have (Pango indents the rest)
		int firstIndent, restIndent; // the rest of the margin of the first and of the other lines
		int firstLine;
		int lineCount;
		Alignment align;
	}

	private Para[] paras; // null: one Pango layout for the whole text
	private boolean measureOnly; // getDesiredWidth(): no line table
	private int lineCount;
	private int[] lineTops;     // lineCount + 1 entries, the last is the height
	private int[] lineAscents;  // negative, as getLineAscent()
	private int[] lineDescents; // with the extra space
	private int[] lineExtras;
	private int[] lineShifts;   // how far each line is moved down from where Pango put it in its paragraph
	private float[] paraSpacing = {1, 0};
	private boolean justify;

	/* AOSP Layout.getParagraphSpans(): the spans of the paragraph, none for an empty range but at the start */
	private static <T> T[] getParagraphSpans(Spanned text, int start, int end, Class<T> type) {
		if (start == end && start > 0)
			return (T[])java.lang.reflect.Array.newInstance(type, 0);
		return text.getSpans(start, end, type);
	}

	private static boolean hasParagraphSpans(Spanned text, int start, int end) {
		return text.getSpans(start, end, LeadingMarginSpan.class).length > 0 || text.getSpans(start, end, LineHeightSpan.class).length > 0
		    || text.getSpans(start, end, LineBackgroundSpan.class).length > 0 || text.getSpans(start, end, AlignmentSpan.class).length > 0;
	}

	private void buildParagraphs(int start, int end) {
		Spanned sp = replacementText;
		ArrayList<Para> list = new ArrayList<>();
		for (int p = start, pend; p <= end; p = pend) {
			int nl = TextUtils.indexOf(sp, '\n', p, end);
			pend = nl < 0 ? end : nl + 1;
			Para para = new Para();
			para.start = p;
			para.end = pend;
			int textEnd = nl < 0 ? end : nl;
			int first = 0, rest = 0;
			for (LeadingMarginSpan margin : getParagraphSpans(sp, p, pend, LeadingMarginSpan.class)) {
				first += margin.getLeadingMargin(true);
				rest += margin.getLeadingMargin(false);
			}
			para.x = Math.min(first, rest);
			para.layout = native_constructor(pangoText(sp, p, textEnd), paint.paint, width == -1 ? -1 : Math.max(0, width - para.x));
			applySpans(para.layout, p, p, textEnd);
			para.firstIndent = first - para.x;
			para.restIndent = rest - para.x;
			native_set_paragraph_style(para.layout, 1, 0, justify, para.firstIndent, para.restIndent);
			list.add(para);
			if (nl < 0)
				break; // a text that ends in a line break has an empty last paragraph, as in AOSP
		}
		paras = list.toArray(new Para[0]);
		layout = paras[0].layout;
		setAlignment(align);
		if (!measureOnly)
			buildLines();
	}

	/* StaticLayout.out(): line by line the height Pango gives the line, changed by the LineHeightSpans of the
	 * paragraph, then the line spacing below every line but the last */
	private void buildLines() {
		Spanned sp = replacementText;
		int n = 0;
		for (Para p : paras) {
			p.firstLine = n;
			p.lineCount = native_get_line_count(p.layout);
			n += p.lineCount;
		}
		lineCount = n;
		lineTops = new int[n + 1];
		lineAscents = new int[n];
		lineDescents = new int[n];
		lineExtras = new int[n];
		lineShifts = new int[n];
		float mult = paraSpacing[0], add = paraSpacing[1];
		boolean needMultiply = mult != 1 || add != 0;
		Paint.FontMetricsInt fm = new Paint.FontMetricsInt();
		int[] metrics = new int[3];
		int v = 0;
		for (Para p : paras) {
			LineHeightSpan[] chooseHt = getParagraphSpans(sp, p.start, p.end, LineHeightSpan.class);
			int[] chooseHtv = new int[chooseHt.length];
			for (int i = 0; i < chooseHt.length; i++) {
				int o = sp.getSpanStart(chooseHt[i]);
				chooseHtv[i] = o < p.start ? lineTops[getLineForOffset(o)] : v;
			}
			for (int l = 0; l < p.lineCount; l++) {
				int line = p.firstLine + l;
				native_get_line_metrics(p.layout, l, metrics);
				int above = metrics[0];
				int below = metrics[1];
				if (chooseHt.length > 0) {
					fm.ascent = fm.top = above;
					fm.descent = fm.bottom = below;
					int start = lineStart(p, l), end = lineEnd(p, l);
					for (int i = 0; i < chooseHt.length; i++) {
						if (chooseHt[i] instanceof LineHeightSpan.WithDensity)
							((LineHeightSpan.WithDensity)chooseHt[i]).chooseHeight(sp, start, end, chooseHtv[i], v, fm, paint);
						else
							chooseHt[i].chooseHeight(sp, start, end, chooseHtv[i], v, fm);
					}
					above = fm.ascent;
					below = fm.descent;
				}
				int extra = 0;
				if (needMultiply && line != n - 1) {
					double ex = (below - above) * (mult - 1) + add;
					extra = ex >= 0 ? (int)(ex + 0.5) : -(int)(-ex + 0.5);
				}
				lineTops[line] = v;
				lineAscents[line] = above;
				lineDescents[line] = below + extra;
				lineExtras[line] = extra;
				lineShifts[line] = v - above - metrics[2];
				v += below - above + extra;
			}
		}
		lineTops[n] = v;
	}

	/* the offsets in the text of the start and the end of line l of the paragraph (the last ends after the line break) */
	private int lineStart(Para p, int l) {
		return p.start + native_get_line_start(p.layout, l);
	}

	private int lineEnd(Para p, int l) {
		return l == p.lineCount - 1 ? p.end : p.start + native_get_line_end(p.layout, l);
	}

	/* AOSP Layout.getLineVisibleEnd(line, start, end, true): without the line break and the spaces at the end of the
	 * line, but the last line */
	private int visibleEnd(int line, int start, int end) {
		if (line == getLineCount() - 1)
			return end;
		for (; end > start; end--) {
			char ch = replacementText.charAt(end - 1);
			if (ch == '\n')
				return end - 1;
			if (ch != ' ' && ch != '\t' && ch != 0x1680 && (ch < 0x2000 || ch > 0x200A || ch == 0x2007) && ch != 0x205F && ch != 0x3000)
				break;
		}
		return end;
	}

	private Para paraOfLine(int line) {
		for (int i = paras.length - 1; i > 0; i--) {
			if (paras[i].firstLine <= line)
				return paras[i];
		}
		return paras[0];
	}

	private Para paraOfOffset(int offset) {
		for (int i = paras.length - 1; i > 0; i--) {
			if (paras[i].start <= offset)
				return paras[i];
		}
		return paras[0];
	}

	/* AOSP Layout.drawBackground() and the leading margins of Layout.drawText(), then each paragraph's text moved to
	 * its margin and its lines moved to where the line table has them */
	private void drawParagraphs(Canvas canvas) {
		Spanned sp = replacementText;
		TextPaint wp = new TextPaint();
		wp.set(paint);
		LineBackgroundSpan[] backgrounds = sp.getSpans(0, sp.length(), LineBackgroundSpan.class);
		int count = getLineCount();
		for (int line = 0; line < count; line++) {
			Para p = paraOfLine(line);
			int start = lineStart(p, line - p.firstLine), end = lineEnd(p, line - p.firstLine);
			int top = lineTops[line], bottom = lineTops[line + 1], baseline = bottom - lineDescents[line];
			if (start != end || start == 0) {
				for (LineBackgroundSpan background : backgrounds) {
					if (sp.getSpanStart(background) < end && sp.getSpanEnd(background) > start)
						background.drawBackground(canvas, paint, 0, getWidth(), top, baseline, bottom, sp, start, end, line);
				}
			}
		}
		for (Para p : paras) {
			LeadingMarginSpan[] margins = getParagraphSpans(sp, p.start, p.end, LeadingMarginSpan.class);
			int lines = Math.min(p.lineCount, count - p.firstLine);
			if (lines <= 0)
				break;
			for (int l = 0; l < lines && margins.length > 0; l++) {
				int line = p.firstLine + l;
				boolean useFirstLineMargin = l == 0;
				for (LeadingMarginSpan margin : margins) {
					if (margin instanceof LeadingMarginSpan.LeadingMarginSpan2 && line < getLineForOffset(sp.getSpanStart(margin)) + ((LeadingMarginSpan.LeadingMarginSpan2)margin).getLeadingMarginLineCount()) {
						useFirstLineMargin = true;
						break;
					}
				}
				int left = 0;
				int bottom = lineTops[line + 1];
				for (LeadingMarginSpan margin : margins) {
					margin.drawLeadingMargin(canvas, wp, left, DIR_LEFT_TO_RIGHT, lineTops[line], bottom - lineDescents[line], bottom,
					                         sp, lineStart(p, l), visibleEnd(line, lineStart(p, l), lineEnd(p, l)), l == 0, this);
					left += margin.getLeadingMargin(useFirstLineMargin);
				}
			}
			boolean moved = false;
			for (int l = 1; l < lines; l++)
				moved |= lineShifts[p.firstLine + l] != lineShifts[p.firstLine];
			if (!moved) {
				drawPango(canvas, p.layout, p.x, lineShifts[p.firstLine], lines < p.lineCount ? native_get_line_bottom(p.layout, lines - 1) : -1);
			} else {
				for (int l = 0; l < lines; l++) {
					/* each line on its own: cut to its band, the first from the top and the last to the bottom */
					canvas.save();
					canvas.clipRect(-1e6f, l == 0 ? -1e6f : native_get_line_top(p.layout, l) + lineShifts[p.firstLine + l],
					                1e6f, l == p.lineCount - 1 ? 1e6f : native_get_line_bottom(p.layout, l) + lineShifts[p.firstLine + l]);
					drawPango(canvas, p.layout, p.x, lineShifts[p.firstLine + l], -1);
					canvas.restore();
				}
			}
		}
	}

	/* the Pango layout at (x, y); with a bottom >= 0 only the lines above it (maxLines) */
	private void drawPango(Canvas canvas, long pango, int x, int y, int bottom) {
		canvas.save();
		canvas.translate(x, y);
		if (bottom >= 0)
			canvas.clipRect(-1e6f, -1e6f, 1e6f, bottom);
		if (canvas instanceof GskCanvas)
			native_draw(pango, ((GskCanvas)canvas).snapshot, paint.paint);
		else
			native_draw_custom_canvas(pango, canvas, paint);
		canvas.restore();
	}

	/* for DynamicLayout (AOSP reflow()): the text changed; build the Pango layout again at the same width with
	 * the alignment and the line spacing this layout has, and the spans of the new text */
	void reflow(CharSequence source) {
		long old = layout;
		Para[] oldParas = paras;
		build(source, 0, source != null ? source.length() : 0, width);
		if (paras != null)
			setParagraphStyle(spacing_mult, spacing_add, justify, 0, 0);
		else if (spacing_mult != 1 || spacing_add != 0)
			native_set_paragraph_style(layout, spacing_mult, spacing_add, false, 0, 0);
		if (oldParas == null) {
			native_free(old);
		} else {
			for (Para p : oldParas)
				native_free(p.layout);
		}
	}

	/* StaticLayout.Builder.setHyphenationFrequency(): HYPHENATION_FREQUENCY_NONE (0) breaks a word that
	 * does not fit without a hyphen; Pango has no hyphenation dictionary, so normal and full both leave
	 * the hyphen it inserts where it has to break a word */
	void setHyphenation(boolean hyphens) {
		if (paras == null) {
			if (text.length() > 0)
				native_add_attribute(layout, ATTR_HYPHENS, 0, text.length(), 0, hyphens ? 1 : 0);
			return;
		}
		for (Para p : paras) {
			if (p.end > p.start)
				native_add_attribute(p.layout, ATTR_HYPHENS, 0, p.end - p.start, 0, hyphens ? 1 : 0);
		}
		buildLines();
	}

	/* for StaticLayout.Builder, which builds the layout before its setters are called */
	void setAlignment(Alignment align) {
		this.align = align;
		if (paras == null) {
			native_set_alignment(layout, pangoAlignment(align));
			return;
		}
		/* AOSP Layout.drawText(): the last AlignmentSpan of a paragraph aligns it */
		for (Para p : paras) {
			AlignmentSpan[] spans = getParagraphSpans(replacementText, p.start, p.end, AlignmentSpan.class);
			p.align = spans.length > 0 ? spans[spans.length - 1].getAlignment() : align;
			native_set_alignment(p.layout, pangoAlignment(p.align));
		}
	}

	private static int pangoAlignment(Alignment align) {
		if (align == Alignment.ALIGN_CENTER)
			return 1;
		else if (align == Alignment.ALIGN_OPPOSITE || align == Alignment.ALIGN_RIGHT)
			return 2;
		else
			return 0;
	}

	/* StaticLayout.Builder: line spacing as AOSP (extra space between lines, not after the last),
	 * JUSTIFICATION_MODE_INTER_WORD, first-line or hanging indent */
	void setParagraphStyle(float spacingMult, float spacingAdd, boolean justify, int firstIndent, int restIndent) {
		spacing_mult = spacingMult;
		spacing_add = spacingAdd;
		this.justify = justify;
		if (paras == null) {
			native_set_paragraph_style(layout, spacingMult, spacingAdd, justify, firstIndent, restIndent);
			return;
		}
		/* paragraph mode: the spacing is in the line table; the indents of the leading margins stay (the
		 * Builder's indents are not applied, Pango has one indent per layout) */
		paraSpacing = new float[] {spacingMult, spacingAdd};
		for (Para p : paras)
			native_set_paragraph_style(p.layout, 1, 0, justify, p.firstIndent, p.restIndent);
		buildLines();
	}

	/* StaticLayout.Builder: like AOSP only the first maxLines lines exist; with ellipsize the last of
	 * them ends in an ellipsis (Pango does that natively), without it the rest is cut off */
	void setMaxLines(int maxLines, TextUtils.TruncateAt ellipsize) {
		this.maxLines = maxLines;
		if (ellipsize == null || ellipsize == TextUtils.TruncateAt.MARQUEE)
			return;
		if (paras == null) {
			native_set_max_lines(layout, maxLines, ellipsize.ordinal() + 1);
		} else if (maxLines > 0 && lineCount > maxLines) {
			/* the paragraph with the last line ends in the ellipsis */
			Para p = paraOfLine(maxLines - 1);
			native_set_max_lines(p.layout, maxLines - p.firstLine, ellipsize.ordinal() + 1);
			buildLines();
		}
	}

	/* StaticLayout.Builder.setUseBoundsForWidth() */
	boolean mUseBoundsForWidth = false;

	public boolean getUseBoundsForWidth() {
		return mUseBoundsForWidth;
	}

	public int getLineCount() {
		return Math.min(paras == null ? native_get_line_count(layout) : lineCount, maxLines);
	}

	public float getLineWidth(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		if (paras != null) {
			Para p = paraOfLine(line);
			return native_get_line_width(p.layout, line - p.firstLine);
		}
		return native_get_line_width(layout, line);
	}

	public TextPaint getPaint() {
		return paint;
	}

	public int getEllipsisCount(int line) {
		/* like AOSP StaticLayout for a layout without ellipsis: 0, no range check
		 * (VLC asks for line -1 of an empty text) */
		if (line < 0 || line >= getLineCount())
			return 0;
		if (paras != null) {
			Para p = paraOfLine(line);
			return native_get_ellipsis_count(p.layout, line - p.firstLine);
		}
		return native_get_ellipsis_count(layout, line);
	}

	public CharSequence getText() { return text; }

	public int getWidth() {
		return paras == null ? native_get_width(layout) : width;
	}

	public int getHeight() {
		if (paras != null)
			return lineTops[getLineCount()];
		if (native_get_line_count(layout) > maxLines)
			return getLineBottom(maxLines - 1);
		return native_get_height(layout);
	}

	public void draw(Canvas canvas) {
		if (paras != null) {
			drawParagraphs(canvas);
			drawReplacements(canvas);
		} else if (native_get_line_count(layout) > maxLines) {
			canvas.save();
			canvas.clipRect(-1e6f, 0, 1e6f, getHeight()); // only the lines below the last are cut
			drawLayout(canvas);
			canvas.restore();
		} else {
			drawLayout(canvas);
		}
	}

	private void drawLayout(Canvas canvas) {
		if (canvas instanceof GskCanvas)
			native_draw(layout, ((GskCanvas)canvas).snapshot, paint.paint);
		else
			native_draw_custom_canvas(layout, canvas, paint);
		drawReplacements(canvas);
	}

	public void draw(Canvas canvas, Path selectionHighlight, Paint paint, int selectionOffset) {
		draw(canvas);
	}

	/* AOSP Layout.draw(Canvas, List<Path>, List<Paint>, Path, Paint, int) (API 34): drawHighlights() (the highlight
	 * paths with their paints, then the selection path, moved down by cursorOffsetVertical) before the text. ATL has
	 * no high contrast text, so highlights are not drawn on top. */
	public void draw(Canvas canvas, java.util.List<Path> highlightPaths, java.util.List<Paint> highlightPaints,
	                 Path selectionPath, Paint selectionPaint, int cursorOffsetVertical) {
		if (highlightPaths != null || highlightPaints != null) {
			if (cursorOffsetVertical != 0)
				canvas.translate(0, cursorOffsetVertical);
			try {
				if (highlightPaths != null) {
					if (highlightPaints == null) {
						throw new IllegalArgumentException("if highlight is specified, highlightPaint must be specified.");
					}
					if (highlightPaints.size() != highlightPaths.size()) {
						throw new IllegalArgumentException("The highlight path size is different from the size of highlight paints");
					}
					for (int i = 0; i < highlightPaths.size(); ++i) {
						final Path highlight = highlightPaths.get(i);
						if (highlight != null) {
							canvas.drawPath(highlight, highlightPaints.get(i));
						}
					}
				}
				if (selectionPath != null) {
					canvas.drawPath(selectionPath, selectionPaint);
				}
			} finally {
				if (cursorOffsetVertical != 0)
					canvas.translate(0, -cursorOffsetVertical);
			}
		}
		draw(canvas);
	}

	public int getParagraphDirection(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return DIR_LEFT_TO_RIGHT;
	}

	public static final int DIR_LEFT_TO_RIGHT = 1;
	public static final int DIR_RIGHT_TO_LEFT = -1;

	public static float getDesiredWidth(CharSequence source, TextPaint paint) {
		return getDesiredWidth(source, 0, source.length(), paint);
	}

	/* AOSP getDesiredWidthWithLimit(): the widest paragraph, each measured with its spans (TextLine.metrics()) and
	 * with its leading margin; unwrapped each paragraph is one line, which starts at the margin */
	public static float getDesiredWidth(CharSequence source, int start, int end, TextPaint paint) {
		Layout measure = new Layout(source, start, end, paint);
		float width = 0;
		if (measure.paras == null) {
			width = native_get_desired_width(measure.layout);
			native_free(measure.layout);
			return width;
		}
		for (Para p : measure.paras) {
			width = Math.max(width, p.x + p.firstIndent + native_get_desired_width(p.layout)); // Pango ignores the indent without a width
			native_free(p.layout);
		}
		return width;
	}

	public int getLineBaseline(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		if (paras != null)
			return lineTops[line + 1] - lineDescents[line];
		return native_get_line_baseline(layout, line);
	}

	public int getLineAscent(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		if (paras != null)
			return lineAscents[line];
		return native_get_line_ascent(layout, line);
	}

	public int getLineDescent(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		if (paras != null)
			return lineDescents[line];
		return native_get_line_descent(layout, line);
	}

	public int getTopPadding() { return -5; }

	public int getBottomPadding() { return 5; }

	public boolean isRtlCharAt(int offset) { return false; }

	public float getSecondaryHorizontal(int offset) {
		if (getLineDirections(0) == null)
			throw new NullPointerException();
		if (paras != null) {
			Para p = paraOfOffset(offset);
			return p.x + native_get_secondary_horizontal(p.layout, offset - p.start);
		}
		return native_get_secondary_horizontal(layout, offset);
	}

	/* AOSP Layout.getLineBounds(): the bounds of the line (the whole width of the layout) and its baseline */
	public int getLineBounds(int line, android.graphics.Rect bounds) {
		if (bounds != null) {
			bounds.left = 0;
			bounds.top = getLineTop(line);
			bounds.right = getWidth();
			bounds.bottom = getLineTop(line + 1);
		}
		return getLineBaseline(line);
	}

	public int getLineForVertical(int y) {
		if (paras != null) {
			int line = 0;
			while (line < getLineCount() - 1 && lineTops[line + 1] <= y)
				line++;
			return line;
		}
		return native_get_line_for_vertical(layout, y);
	}

	public int getOffsetForHorizontal(int line, float x) {
		if (getLineDirections(0) == null)
			throw new NullPointerException();
		if (paras != null) {
			Para p = paraOfLine(line);
			return p.start + native_get_offset_for_horizontal(p.layout, line - p.firstLine, x - p.x);
		}
		return native_get_offset_for_horizontal(layout, line, x);
	}

	public float getPrimaryHorizontal(int offset) {
		if (getLineDirections(0) == null)
			throw new NullPointerException();
		if (paras != null) {
			Para p = paraOfOffset(offset);
			return p.x + native_get_primary_horizontal(p.layout, offset - p.start);
		}
		return native_get_primary_horizontal(layout, offset);
	}

	public int getLineForOffset(int offset) {
		if (paras != null) {
			Para p = paraOfOffset(offset);
			return p.firstLine + native_get_line_for_offset(p.layout, Math.min(offset, p.end) - p.start);
		}
		return native_get_line_for_offset(layout, offset);
	}

	/* AOSP: line may be getLineCount(), then it is the bottom of the last line (getLineBounds() asks for line + 1) */
	public int getLineTop(int line) {
		if (line < 0 || line > getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		if (paras != null)
			return lineTops[line];
		if (line == getLineCount())
			return line > 0 ? native_get_line_bottom(layout, line - 1) : 0;
		return native_get_line_top(layout, line);
	}

	/* AOSP Layout.getLineExtra(): the line spacing added below the line; the base class has none */
	public int getLineExtra(int line) {
		return paras != null && line >= 0 && line < lineCount ? lineExtras[line] : 0;
	}

	public int getLineBottom(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		if (paras != null)
			return lineTops[line + 1];
		return native_get_line_bottom(layout, line);
	}

	/* AOSP Layout.getLineBottom(int, boolean) (API 34): getLineTop(line + 1), less getLineExtra(line) without the
	 * line spacing; getLineBottom(line) is getLineTop(line + 1) */
	public int getLineBottom(int line, boolean includeLineSpacing) {
		if (includeLineSpacing) {
			return getLineBottom(line);
		} else {
			return getLineBottom(line) - getLineExtra(line);
		}
	}

	/* AOSP Layout.getLineMax(): the width of the line without trailing whitespace plus the paragraph's leading
	 * margin. ATL counts trailing whitespace as visible (getLineVisibleEnd() == getLineEnd()), so this is the Pango
	 * line width and, in paragraph mode, the margin of the line */
	public float getLineMax(int line) {
		if (paras != null) {
			Para p = paraOfLine(line);
			return p.x + (line == p.firstLine ? p.firstIndent : p.restIndent) + getLineWidth(line);
		}
		return getLineWidth(line);
	}

	public float getLineLeft(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		if (paras != null) {
			/* AOSP: 0 for a left aligned line, otherwise where the alignment puts it after the margin */
			Para p = paraOfLine(line);
			if (p.align == null || p.align == Alignment.ALIGN_NORMAL || p.align == Alignment.ALIGN_LEFT)
				return 0;
			return p.x + native_get_line_left(p.layout, line - p.firstLine);
		}
		return native_get_line_left(layout, line);
	}

	public float getLineRight(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		if (paras != null) {
			/* AOSP: getLineMax() (margin and text) for a left aligned line */
			Para p = paraOfLine(line);
			if (p.align == null || p.align == Alignment.ALIGN_NORMAL || p.align == Alignment.ALIGN_LEFT)
				return getLineMax(line);
			return p.x + native_get_line_right(p.layout, line - p.firstLine);
		}
		return native_get_line_right(layout, line);
	}

	public int getLineStart(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		if (paras != null) {
			Para p = paraOfLine(line);
			return lineStart(p, line - p.firstLine);
		}
		return native_get_line_start(layout, line);
	}

	public int getLineEnd(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		if (paras != null) {
			Para p = paraOfLine(line);
			return lineEnd(p, line - p.firstLine);
		}
		return native_get_line_end(layout, line);
	}

	public boolean isSpanned() {
		return text instanceof Spanned;
	}

	public void increaseWidthTo(int width) {
		if (width < getWidth())
			throw new RuntimeException("cannot decrease width");
		if (paras != null) {
			this.width = width;
			for (Para p : paras)
				native_set_width(p.layout, width - p.x);
			buildLines();
			return;
		}
		native_set_width(layout, width);
	}

	public float getSpacingMultiplier() {
		return spacing_mult;
	}

	public float getSpacingAdd() {
		return spacing_add;
	}

	public float getSpacing_add() {
		return spacing_add;
	}

	/* AOSP's getSelection() for left-to-right lines (isRtlCharAt() is false here): the range on its first and
	 * last line, the first line filled to the right edge and the lines between over the whole width */
	public void getSelectionPath(int start, int end, Path path) {
		path.reset();
		if (start == end)
			return;
		if (end < start) {
			int temp = end;
			end = start;
			start = temp;
		}
		int startLine = getLineForOffset(start);
		int endLine = getLineForOffset(end);
		int top = getLineTop(startLine);
		int bottom = getLineBottom(endLine);
		if (startLine == endLine) {
			path.addRect(getPrimaryHorizontal(start), top, getPrimaryHorizontal(end), bottom, Path.Direction.CW);
			return;
		}
		int width = getWidth();
		path.addRect(getPrimaryHorizontal(start), top, width, getLineBottom(startLine), Path.Direction.CW);
		for (int i = startLine + 1; i < endLine; i++)
			path.addRect(0, getLineTop(i), width, getLineBottom(i), Path.Direction.CW);
		path.addRect(getLineLeft(endLine), getLineTop(endLine), getPrimaryHorizontal(end), bottom, Path.Direction.CW);
	}

	/* AOSP Layout.getRangeForRect() (android-16). ATL's lines are one left-to-right run (getLineDirections()), so
	 * the run loop of getStartOrEndOffsetForAreaWithinLine() has one pass; the character bounds that AOSP's
	 * fillHorizontalBoundsForLine() measures with TextLine come from Pango's cursor positions
	 * (getPrimaryHorizontal(), already including the line's alignment offset, so lineStartPos is 0). The
	 * line bottom includes the line spacing (ATL has no getLineBottom(line, false)). */
	public int[] getRangeForRect(android.graphics.RectF area, SegmentFinder segmentFinder,
	                             TextInclusionStrategy inclusionStrategy) {
		// Find the first line whose bottom (without line spacing) is below the top of the area.
		int startLine = getLineForVertical((int)area.top);
		if (area.top > getLineBottom(startLine)) {
			startLine++;
			if (startLine >= getLineCount()) {
				// The entire area is below the last line, so it does not contain any text.
				return null;
			}
		}

		// Find the last line whose top is above the bottom of the area.
		int endLine = getLineForVertical((int)area.bottom);
		if (endLine == 0 && area.bottom < getLineTop(0)) {
			// The entire area is above the first line, so it does not contain any text.
			return null;
		}
		if (endLine < startLine) {
			// The entire area is between two lines, so it does not contain any text.
			return null;
		}

		int start = getStartOrEndOffsetForAreaWithinLine(
		    startLine, area, segmentFinder, inclusionStrategy, /* getStart= */ true);
		// If the area does not contain any text on this line, keep trying subsequent lines until
		// the end line is reached.
		while (start == -1 && startLine < endLine) {
			startLine++;
			start = getStartOrEndOffsetForAreaWithinLine(
			    startLine, area, segmentFinder, inclusionStrategy, /* getStart= */ true);
		}
		if (start == -1) {
			// All lines were checked, the area does not contain any text.
			return null;
		}

		int end = getStartOrEndOffsetForAreaWithinLine(
		    endLine, area, segmentFinder, inclusionStrategy, /* getStart= */ false);
		// If the area does not contain any text on this line, keep trying previous lines until
		// the start line is reached.
		while (end == -1 && startLine < endLine) {
			endLine--;
			end = getStartOrEndOffsetForAreaWithinLine(
			    endLine, area, segmentFinder, inclusionStrategy, /* getStart= */ false);
		}
		if (end == -1) {
			// All lines were checked, the area does not contain any text.
			return null;
		}

		// If a text segment spans multiple lines or multiple directional runs (e.g. a hyphenated
		// word), then getStartOrEndOffsetForAreaWithinLine() can return an offset in the middle of
		// a text segment. Adjust the range to include the rest of any partial text segments. If
		// start is already the start boundary of a text segment, then this is a no-op.
		start = segmentFinder.previousStartBoundary(start + 1);
		end = segmentFinder.nextEndBoundary(end - 1);

		return new int[] {start, end};
	}

	/* the left and right edge of each character of the line (AOSP fillHorizontalBoundsForLine()), from the
	 * cursor positions: a character ends where the next one starts, the last one at the line's right (or
	 * where it starts, if Pango's line ends before it, as for a trailing space) */
	private float[] horizontalBoundsForLine(int line) {
		final int lineStart = getLineStart(line);
		final int lineEnd = getLineEnd(line);
		float[] bounds = new float[2 * (lineEnd - lineStart)];
		float lineRight = getLineRight(line);
		for (int i = 0; i < lineEnd - lineStart; i++) {
			bounds[2 * i] = getPrimaryHorizontal(lineStart + i);
			bounds[2 * i + 1] = lineStart + i + 1 < lineEnd ? getPrimaryHorizontal(lineStart + i + 1) : Math.max(lineRight, bounds[2 * i]);
		}
		return bounds;
	}

	private int getStartOrEndOffsetForAreaWithinLine(int line, android.graphics.RectF area,
	                                                 SegmentFinder segmentFinder, TextInclusionStrategy inclusionStrategy,
	                                                 boolean getStart) {
		int lineTop = getLineTop(line);
		int lineBottom = getLineBottom(line);

		int lineStartOffset = getLineStart(line);
		int lineEndOffset = getLineEnd(line);
		if (lineStartOffset == lineEndOffset) {
			return -1;
		}

		float[] horizontalBounds = horizontalBoundsForLine(line);
		int lineStartPos = 0;

		// one left-to-right run over the line
		int runStartOffset = 0;
		int runEndOffset = lineEndOffset - lineStartOffset;
		boolean isRtl = false;
		float runLeft = lineStartPos + horizontalBounds[2 * runStartOffset];
		float runRight = lineStartPos + horizontalBounds[2 * (runEndOffset - 1) + 1];

		return getStart
		    ? getStartOffsetForAreaWithinRun(
		          area, lineTop, lineBottom,
		          lineStartOffset, lineStartPos, horizontalBounds,
		          runStartOffset, runEndOffset, runLeft, runRight, isRtl,
		          segmentFinder, inclusionStrategy)
		    : getEndOffsetForAreaWithinRun(
		          area, lineTop, lineBottom,
		          lineStartOffset, lineStartPos, horizontalBounds,
		          runStartOffset, runEndOffset, runLeft, runRight, isRtl,
		          segmentFinder, inclusionStrategy);
	}

	private static int getStartOffsetForAreaWithinRun(
	    android.graphics.RectF area,
	    int lineTop, int lineBottom,
	    int lineStartOffset,
	    int lineStartPos,
	    float[] horizontalBounds,
	    int runStartOffset, int runEndOffset,
	    float runLeft, float runRight,
	    boolean isRtl,
	    SegmentFinder segmentFinder,
	    TextInclusionStrategy inclusionStrategy) {
		if (runRight < area.left || runLeft > area.right) {
			// The run does not overlap the area.
			return -1;
		}

		// Find the first character in the run whose bounds overlap with the area.
		// firstCharOffset is an offset index within the line.
		int firstCharOffset;
		if ((!isRtl && area.left <= runLeft) || (isRtl && area.right >= runRight)) {
			firstCharOffset = runStartOffset;
		} else {
			int low = runStartOffset;
			int high = runEndOffset;
			int guess;
			while (high - low > 1) {
				guess = (high + low) / 2;
				// Left edge of the character at guess
				float pos = lineStartPos + horizontalBounds[2 * guess];
				if ((!isRtl && pos > area.left) || (isRtl && pos < area.right)) {
					high = guess;
				} else {
					low = guess;
				}
			}
			// The area edge is between the left edge of the character at low and the left edge of
			// the character at high. For LTR text, this is within the character at low. For RTL
			// text, this is within the character at high.
			firstCharOffset = isRtl ? high : low;
		}

		// Find the first text segment containing this character (or, if no text segment contains
		// this character, the first text segment after this character). All previous text segments
		// in this run are to the left (for LTR) of the area.
		int segmentEndOffset =
		    segmentFinder.nextEndBoundary(lineStartOffset + firstCharOffset);
		if (segmentEndOffset == SegmentFinder.DONE) {
			// There are no text segments containing or after firstCharOffset, so no text segments
			// in this run overlap the area.
			return -1;
		}
		int segmentStartOffset = segmentFinder.previousStartBoundary(segmentEndOffset);
		if (segmentStartOffset >= lineStartOffset + runEndOffset) {
			// The text segment is after the end of this run, so no text segments in this run
			// overlap the area.
			return -1;
		}
		// If the segment extends outside of this run, only consider the piece of the segment within
		// this run.
		segmentStartOffset = Math.max(segmentStartOffset, lineStartOffset + runStartOffset);
		segmentEndOffset = Math.min(segmentEndOffset, lineStartOffset + runEndOffset);

		android.graphics.RectF segmentBounds = new android.graphics.RectF(0, lineTop, 0, lineBottom);
		while (true) {
			// Start (left for LTR, right for RTL) edge of the character at segmentStartOffset.
			float segmentStart = lineStartPos + horizontalBounds[2 * (segmentStartOffset - lineStartOffset) + (isRtl ? 1 : 0)];
			if ((!isRtl && segmentStart > area.right) || (isRtl && segmentStart < area.left)) {
				// The entire area is to the left (for LTR) of the text segment. So the area does
				// not contain any text segments within this run.
				return -1;
			}
			// End (right for LTR, left for RTL) edge of the character at (segmentStartOffset - 1).
			float segmentEnd = lineStartPos + horizontalBounds[2 * (segmentEndOffset - lineStartOffset - 1) + (isRtl ? 0 : 1)];
			segmentBounds.left = isRtl ? segmentEnd : segmentStart;
			segmentBounds.right = isRtl ? segmentStart : segmentEnd;
			if (inclusionStrategy.isSegmentInside(segmentBounds, area)) {
				return segmentStartOffset;
			}
			// Try the next text segment.
			segmentStartOffset = segmentFinder.nextStartBoundary(segmentStartOffset);
			if (segmentStartOffset == SegmentFinder.DONE || segmentStartOffset >= lineStartOffset + runEndOffset) {
				// No more text segments within this run.
				return -1;
			}
			segmentEndOffset = segmentFinder.nextEndBoundary(segmentStartOffset);
			// If the segment extends past the end of this run, only consider the piece of the
			// segment within this run.
			segmentEndOffset = Math.min(segmentEndOffset, lineStartOffset + runEndOffset);
		}
	}

	private static int getEndOffsetForAreaWithinRun(
	    android.graphics.RectF area,
	    int lineTop, int lineBottom,
	    int lineStartOffset,
	    int lineStartPos,
	    float[] horizontalBounds,
	    int runStartOffset, int runEndOffset,
	    float runLeft, float runRight,
	    boolean isRtl,
	    SegmentFinder segmentFinder,
	    TextInclusionStrategy inclusionStrategy) {
		if (runRight < area.left || runLeft > area.right) {
			// The run does not overlap the area.
			return -1;
		}

		// Find the last character in the run whose bounds overlap with the area.
		// firstCharOffset is an offset index within the line.
		int lastCharOffset;
		if ((!isRtl && area.right >= runRight) || (isRtl && area.left <= runLeft)) {
			lastCharOffset = runEndOffset - 1;
		} else {
			int low = runStartOffset;
			int high = runEndOffset;
			int guess;
			while (high - low > 1) {
				guess = (high + low) / 2;
				// Left edge of the character at guess
				float pos = lineStartPos + horizontalBounds[2 * guess];
				if ((!isRtl && pos > area.right) || (isRtl && pos < area.left)) {
					high = guess;
				} else {
					low = guess;
				}
			}
			// The area edge is between the left edge of the character at low and the left edge of
			// the character at high. For LTR text, this is within the character at low. For RTL
			// text, this is within the character at high.
			lastCharOffset = isRtl ? high : low;
		}

		// Find the last text segment containing this character (or, if no text segment contains
		// this character, the first text segment before this character). All following text
		// segments in this run are to the right (for LTR) of the area.
		// + 1 to allow segmentStartOffset = lineStartOffset + lastCharOffset
		int segmentStartOffset =
		    segmentFinder.previousStartBoundary(lineStartOffset + lastCharOffset + 1);
		if (segmentStartOffset == SegmentFinder.DONE) {
			// There are no text segments containing or before lastCharOffset, so no text segments
			// in this run overlap the area.
			return -1;
		}
		int segmentEndOffset = segmentFinder.nextEndBoundary(segmentStartOffset);
		if (segmentEndOffset <= lineStartOffset + runStartOffset) {
			// The text segment is before the start of this run, so no text segments in this run
			// overlap the area.
			return -1;
		}
		// If the segment extends outside of this run, only consider the piece of the segment within
		// this run.
		segmentStartOffset = Math.max(segmentStartOffset, lineStartOffset + runStartOffset);
		segmentEndOffset = Math.min(segmentEndOffset, lineStartOffset + runEndOffset);

		android.graphics.RectF segmentBounds = new android.graphics.RectF(0, lineTop, 0, lineBottom);
		while (true) {
			// End (right for LTR, left for RTL) edge of the character at (segmentStartOffset - 1).
			float segmentEnd = lineStartPos + horizontalBounds[2 * (segmentEndOffset - lineStartOffset - 1) + (isRtl ? 0 : 1)];
			if ((!isRtl && segmentEnd < area.left) || (isRtl && segmentEnd > area.right)) {
				// The entire area is to the right (for LTR) of the text segment. So the
				// area does not contain any text segments within this run.
				return -1;
			}
			// Start (left for LTR, right for RTL) edge of the character at segmentStartOffset.
			float segmentStart = lineStartPos + horizontalBounds[2 * (segmentStartOffset - lineStartOffset) + (isRtl ? 1 : 0)];
			segmentBounds.left = isRtl ? segmentEnd : segmentStart;
			segmentBounds.right = isRtl ? segmentStart : segmentEnd;
			if (inclusionStrategy.isSegmentInside(segmentBounds, area)) {
				return segmentEndOffset;
			}
			// Try the previous text segment.
			segmentEndOffset = segmentFinder.previousEndBoundary(segmentEndOffset);
			if (segmentEndOffset == SegmentFinder.DONE || segmentEndOffset <= lineStartOffset + runStartOffset) {
				// No more text segments within this run.
				return -1;
			}
			segmentStartOffset = segmentFinder.previousStartBoundary(segmentEndOffset);
			// If the segment extends past the start of this run, only consider the piece of the
			// segment within this run.
			segmentStartOffset = Math.max(segmentStartOffset, lineStartOffset + runStartOffset);
		}
	}

	public int getParagraphRight(int line) {
		return getWidth();
	}

	public int getParagraphLeft(int line) {
		return 0;
	}

	public Alignment getParagraphAlignment(int line) {
		return align;
	}

	public Alignment getAlignment() {
		return align;
	}

	public int getOffsetToRightOf(int offset) {
		if (getLineDirections(0) == null)
			throw new NullPointerException();
		return offset;
	}

	public int getOffsetToLeftOf(int offset) {
		if (getLineDirections(0) == null)
			throw new NullPointerException();
		return offset;
	}

	public Directions getLineDirections(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return new Directions();
	}

	public int getLineVisibleEnd(int line) {
		return getLineEnd(line);
	}

	public boolean getLineContainsTab(int line) { return text.toString().split("\n")[line].contains("\t"); }

	public int getEllipsizedWidth() {
		return getWidth();
	}

	public int getEllipsisStart(int line) {
		if (line < 0 || line >= getLineCount())
			return 0;
		return 0;
	}

	protected static native long native_constructor(String text, long paint, int width);
	/* start and end in UTF-16 units of the text; color as ARGB, size in pixels, scale as factor */
	private static native void native_add_attribute(long layout, int type, int start, int end, int color, float value);
	private static native void native_add_placeholder(long layout, int offset, int width, int ascent, int descent);
	private static native boolean native_get_placeholder(long layout, int offset, float[] place);
	private static native void native_get_line_metrics(long layout, int line, int[] metrics);
	protected native void native_set_width(long layout, int width);
	/* 0 left, 1 center, 2 right (the layout direction is always LTR) */
	private static native void native_set_alignment(long layout, int alignment);
	private static native void native_set_paragraph_style(long layout, float spacing_mult, float spacing_add, boolean justify, int first_indent, int rest_indent);
	/* ellipsize_mode as PangoEllipsizeMode */
	private static native void native_set_max_lines(long layout, int max_lines, int ellipsize_mode);
	protected native int native_get_width(long layout);
	protected native int native_get_height(long layout);
	protected native int native_get_line_count(long layout);
	protected native int native_get_line_start(long layout, int line);
	protected native int native_get_line_end(long layout, int line);
	protected native int native_get_line_top(long layout, int line);
	protected native int native_get_line_bottom(long layout, int line);
	protected native float native_get_line_left(long layout, int line);
	protected native float native_get_line_right(long layout, int line);
	protected native float native_get_line_width(long layout, int line);
	protected native int native_get_line_baseline(long layout, int line);
	protected native int native_get_line_ascent(long layout, int line);
	protected native int native_get_line_descent(long layout, int line);
	protected native int native_get_line_for_vertical(long layout, int y);
	protected native void native_set_ellipsize(long layout, int ellipsize_mode, float ellipsize_width);
	protected native int native_get_ellipsis_count(long layout, int line);
	protected native void native_draw(long layout, long snapshot, long paint);
	protected native void native_draw_custom_canvas(long layout, Canvas canvas, Paint paint);
	protected native int native_get_line_for_offset(long layout, int offset);
	protected native float native_get_primary_horizontal(long layout, int offset);
	protected native float native_get_secondary_horizontal(long layout, int offset);
	protected native int native_get_offset_for_horizontal(long layout, int line, float x);
	protected static native float native_get_desired_width(long layout);
	protected static native void native_free(long layout);
}
