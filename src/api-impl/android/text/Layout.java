package android.text;

import android.atl.GskCanvas;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.UnderlineSpan;

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

	long layout; // native PangoLayout
	private CharSequence text;
	private TextPaint paint;
	private float spacing_mult;
	private float spacing_add;
	private Alignment align;
	private int maxLines = Integer.MAX_VALUE;

	protected Layout(CharSequence text, TextPaint paint, int width, Layout.Alignment align, float spacingMult, float spacingAdd) {
		if (text instanceof Spanned) {
			Spanned spanned = (Spanned)text;
			// call every ReplacementSpan.getSize method to prevent "PlaceholderSpan is not laid out yet." errors in ComposeUI
			for (ReplacementSpan span : spanned.getSpans(0, spanned.length(), ReplacementSpan.class))
				span.getSize(paint, text, spanned.getSpanStart(span), spanned.getSpanEnd(span), paint.getFontMetricsInt());
		}
		String str = text != null ? text.toString() : "";
		this.text = str;
		this.paint = paint;
		this.spacing_mult = spacingMult;
		this.spacing_add = spacingAdd;
		layout = native_constructor(str, paint.paint, width);
		if (text instanceof Spanned)
			applySpans((Spanned)text);
		setAlignment(align);
	}

	/* the character spans that change how the text looks become Pango attributes */
	private void applySpans(Spanned text) {
		for (CharacterStyle span : text.getSpans(0, text.length(), CharacterStyle.class)) {
			int start = text.getSpanStart(span);
			int end = text.getSpanEnd(span);
			if (start < 0 || end <= start)
				continue;
			if (span instanceof AbsoluteSizeSpan) {
				AbsoluteSizeSpan size = (AbsoluteSizeSpan)span;
				float px = size.getDip() ? size.getSize() * paint.density : size.getSize();
				native_add_attribute(layout, ATTR_SIZE, start, end, 0, px);
			} else if (span instanceof RelativeSizeSpan) {
				native_add_attribute(layout, ATTR_SCALE, start, end, 0, ((RelativeSizeSpan)span).getSizeChange());
			} else if (span instanceof ForegroundColorSpan) {
				native_add_attribute(layout, ATTR_FOREGROUND, start, end, ((ForegroundColorSpan)span).getForegroundColor(), 0);
			} else if (span instanceof BackgroundColorSpan) {
				native_add_attribute(layout, ATTR_BACKGROUND, start, end, ((BackgroundColorSpan)span).getBackgroundColor(), 0);
			} else if (span instanceof StyleSpan) {
				int style = ((StyleSpan)span).getStyle();
				if ((style & Typeface.BOLD) != 0)
					native_add_attribute(layout, ATTR_BOLD, start, end, 0, 0);
				if ((style & Typeface.ITALIC) != 0)
					native_add_attribute(layout, ATTR_ITALIC, start, end, 0, 0);
			} else if (span instanceof TextAppearanceSpan) {
				/* AOSP TextAppearanceSpan.updateDrawState(): colour, size and style of the appearance */
				TextAppearanceSpan appearance = (TextAppearanceSpan)span;
				if (appearance.getTextColor() != null)
					native_add_attribute(layout, ATTR_FOREGROUND, start, end, appearance.getTextColor().getDefaultColor(), 0);
				if (appearance.getTextSize() > 0)
					native_add_attribute(layout, ATTR_SIZE, start, end, 0, appearance.getTextSize());
				if ((appearance.getTextStyle() & Typeface.BOLD) != 0)
					native_add_attribute(layout, ATTR_BOLD, start, end, 0, 0);
				if ((appearance.getTextStyle() & Typeface.ITALIC) != 0)
					native_add_attribute(layout, ATTR_ITALIC, start, end, 0, 0);
			} else if (span instanceof UnderlineSpan) {
				native_add_attribute(layout, ATTR_UNDERLINE, start, end, 0, 0);
			} else if (span instanceof StrikethroughSpan) {
				native_add_attribute(layout, ATTR_STRIKETHROUGH, start, end, 0, 0);
			}
		}
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

	/* StaticLayout.Builder.setHyphenationFrequency(): HYPHENATION_FREQUENCY_NONE (0) breaks a word that
	 * does not fit without a hyphen; Pango has no hyphenation dictionary, so normal and full both leave
	 * the hyphen it inserts where it has to break a word */
	void setHyphenation(boolean hyphens) {
		if (text.length() > 0)
			native_add_attribute(layout, ATTR_HYPHENS, 0, text.length(), 0, hyphens ? 1 : 0);
	}

	/* for StaticLayout.Builder, which builds the layout before its setters are called */
	void setAlignment(Alignment align) {
		this.align = align;
		if (align == Alignment.ALIGN_CENTER)
			native_set_alignment(layout, 1);
		else if (align == Alignment.ALIGN_OPPOSITE || align == Alignment.ALIGN_RIGHT)
			native_set_alignment(layout, 2);
		else
			native_set_alignment(layout, 0);
	}

	/* StaticLayout.Builder: line spacing as AOSP (extra space between lines, not after the last),
	 * JUSTIFICATION_MODE_INTER_WORD, first-line or hanging indent */
	void setParagraphStyle(float spacingMult, float spacingAdd, boolean justify, int firstIndent, int restIndent) {
		spacing_mult = spacingMult;
		spacing_add = spacingAdd;
		native_set_paragraph_style(layout, spacingMult, spacingAdd, justify, firstIndent, restIndent);
	}

	/* StaticLayout.Builder: like AOSP only the first maxLines lines exist; with ellipsize the last of
	 * them ends in an ellipsis (Pango does that natively), without it the rest is cut off */
	void setMaxLines(int maxLines, TextUtils.TruncateAt ellipsize) {
		this.maxLines = maxLines;
		if (ellipsize != null && ellipsize != TextUtils.TruncateAt.MARQUEE)
			native_set_max_lines(layout, maxLines, ellipsize.ordinal() + 1);
	}

	/* StaticLayout.Builder.setUseBoundsForWidth() */
	boolean mUseBoundsForWidth = false;

	public boolean getUseBoundsForWidth() {
		return mUseBoundsForWidth;
	}

	public int getLineCount() {
		return Math.min(native_get_line_count(layout), maxLines);
	}

	public float getLineWidth(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
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
		return native_get_ellipsis_count(layout, line);
	}

	public CharSequence getText() { return text; }

	public int getWidth() {
		return native_get_width(layout);
	}

	public int getHeight() {
		if (native_get_line_count(layout) > maxLines)
			return getLineBottom(maxLines - 1);
		return native_get_height(layout);
	}

	public void draw(Canvas canvas) {
		if (native_get_line_count(layout) > maxLines) {
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
	}

	public void draw(Canvas canvas, Path selectionHighlight, Paint paint, int selectionOffset) {
		draw(canvas);
	}

	public int getParagraphDirection(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return /*DIR_LEFT_TO_RIGHT*/ 1;
	}

	public static float getDesiredWidth(CharSequence source, int start, int end, TextPaint paint) {
		return getDesiredWidth(source.subSequence(start, end), paint);
	}

	public static float getDesiredWidth(CharSequence source, TextPaint paint) {
		long layout = native_constructor(source != null ? source.toString() : "", paint.paint, -1);
		float width = native_get_desired_width(layout);
		native_free(layout);
		return width;
	}

	public int getLineBaseline(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return native_get_line_baseline(layout, line);
	}

	public int getLineAscent(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return native_get_line_ascent(layout, line);
	}

	public int getLineDescent(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return native_get_line_descent(layout, line);
	}

	public int getTopPadding() { return -5; }

	public int getBottomPadding() { return 5; }

	public boolean isRtlCharAt(int offset) { return false; }

	public float getSecondaryHorizontal(int offset) {
		if (getLineDirections(0) == null)
			throw new NullPointerException();
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
		return native_get_line_for_vertical(layout, y);
	}

	public int getOffsetForHorizontal(int line, float x) {
		if (getLineDirections(0) == null)
			throw new NullPointerException();
		return native_get_offset_for_horizontal(layout, line, x);
	}

	public float getPrimaryHorizontal(int offset) {
		if (getLineDirections(0) == null)
			throw new NullPointerException();
		return native_get_primary_horizontal(layout, offset);
	}

	public int getLineForOffset(int offset) {
		return native_get_line_for_offset(layout, offset);
	}

	public int getLineTop(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return native_get_line_top(layout, line);
	}

	public int getLineBottom(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return native_get_line_bottom(layout, line);
	}

	public float getLineLeft(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return native_get_line_left(layout, line);
	}

	public float getLineRight(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return native_get_line_right(layout, line);
	}

	public int getLineStart(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return native_get_line_start(layout, line);
	}

	public int getLineEnd(int line) {
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return native_get_line_end(layout, line);
	}

	public boolean isSpanned() {
		return text instanceof Spanned;
	}

	public void increaseWidthTo(int width) {
		if (width < getWidth())
			throw new RuntimeException("cannot decrease width");
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
