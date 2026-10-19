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

	long layout; // native PangoLayout
	private CharSequence text;
	private TextPaint paint;
	private float spacing_mult;
	private float spacing_add;
	private Alignment align;

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

	public int getLineCount() {
		return native_get_line_count(layout);
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
		if (line < 0 || line >= getLineCount())
			throw new ArrayIndexOutOfBoundsException();
		return native_get_ellipsis_count(layout, line);
	}

	public CharSequence getText() { return text; }

	public int getWidth() {
		return native_get_width(layout);
	}

	public int getHeight() {
		return native_get_height(layout);
	}

	public void draw(Canvas canvas) {
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

	public void getSelectionPath(int start, int end, Path path) {
		if (start != end && getLineDirections(0) == null)
			throw new NullPointerException();
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
			throw new ArrayIndexOutOfBoundsException();
		return 0;
	}

	protected static native long native_constructor(String text, long paint, int width);
	/* start and end in UTF-16 units of the text; color as ARGB, size in pixels, scale as factor */
	private static native void native_add_attribute(long layout, int type, int start, int end, int color, float value);
	protected native void native_set_width(long layout, int width);
	/* 0 left, 1 center, 2 right (the layout direction is always LTR) */
	private static native void native_set_alignment(long layout, int alignment);
	protected native int native_get_width(long layout);
	protected native int native_get_height(long layout);
	protected native int native_get_line_count(long layout);
	protected native int native_get_line_start(long layout, int line);
	protected native int native_get_line_end(long layout, int line);
	protected native int native_get_line_top(long layout, int line);
	protected native int native_get_line_bottom(long layout, int line);
	protected native int native_get_line_left(long layout, int line);
	protected native int native_get_line_right(long layout, int line);
	protected native int native_get_line_width(long layout, int line);
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
