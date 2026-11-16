package android.graphics;

import android.atl.annotation.Export;
import android.os.LocaleList;
import java.util.Locale;

public class Paint {
	public static final int ANTI_ALIAS_FLAG = (1 << 0);
	public static final int FILTER_BITMAP_FLAG = (1 << 1);
	public static final int DITHER_FLAG = (1 << 2);
	public static final int UNDERLINE_TEXT_FLAG = (1 << 3);
	public static final int STRIKE_THRU_TEXT_FLAG = (1 << 4);
	public static final int FAKE_BOLD_TEXT_FLAG = (1 << 5);
	public static final int LINEAR_TEXT_FLAG = (1 << 6);
	public static final int SUBPIXEL_TEXT_FLAG = (1 << 7);
	public static final int DEV_KERN_TEXT_FLAG = (1 << 8);
	public static final int LCD_RENDER_TEXT_FLAG = (1 << 9);
	public static final int EMBEDDED_BITMAP_TEXT_FLAG = (1 << 10);
	public static final int AUTO_HINTING_TEXT_FLAG = (1 << 11);
	public static final int VERTICAL_TEXT_FLAG = (1 << 12);

	public long paint; // native paint
	private Xfermode xfermode;
	private Shader shader;
	private Align align = Align.CENTER;
	private ColorFilter color_filter;
	/* Kept for the getters only: GSK always draws antialiased and filtered, Pango hints text
	 * itself. ATL always antialiases, so that flag is set by default, like in AOSP since API 30. */
	private int flags = ANTI_ALIAS_FLAG;
	private float text_scale_x = 1;
	private float stroke_miter = 4;
	private int hinting = HINTING_ON;
	/* as set by the app: GSK can't stroke with width 0 (hairline), so the native stroke is 1 then */
	private float stroke_width = 0;
	private PathEffect path_effect;
	/* AOSP keeps these in Java fields too (mShadowLayer*); the native paint has them for drawing text */
	private float shadow_layer_radius;
	private float shadow_layer_dx;
	private float shadow_layer_dy;
	private long shadow_layer_color;
	private float text_skew_x;

	public static final int HINTING_OFF = 0;
	public static final int HINTING_ON = 1;

	public Paint() {
		paint = native_create();
	}

	public Paint(int flags) {
		this();
		setFlags(flags);
	}

	public Paint(Paint paint) {
		this.paint = native_clone(paint.paint);
		copyJavaState(paint);
	}

	/* AOSP hands them to the text shaper; ATL keeps them for the getters (Pango uses the default locale) */
	private LocaleList text_locales;
	private String font_feature_settings;
	private String font_variation_settings;
	private boolean elegant_text_height;

	private void copyJavaState(Paint paint) {
		flags = paint.flags;
		text_locales = paint.text_locales;
		font_feature_settings = paint.font_feature_settings;
		font_variation_settings = paint.font_variation_settings;
		elegant_text_height = paint.elegant_text_height;
		mWordSpacing = paint.mWordSpacing;
		typeface = paint.typeface;
		text_scale_x = paint.text_scale_x;
		stroke_miter = paint.stroke_miter;
		hinting = paint.hinting;
		stroke_width = paint.stroke_width;
		path_effect = paint.path_effect;
		shadow_layer_radius = paint.shadow_layer_radius;
		shadow_layer_dx = paint.shadow_layer_dx;
		shadow_layer_dy = paint.shadow_layer_dy;
		shadow_layer_color = paint.shadow_layer_color;
		text_skew_x = paint.text_skew_x;
	}

	private void setFlag(int flag, boolean on) {
		flags = on ? flags | flag : flags & ~flag;
		native_set_text_flags(paint, flags);
	}

	public void setColor(int color) {
		native_set_color(paint, color);
	}

	/**
	 * Set the paint's color with a {@code ColorLong}. The color is converted to sRGB, as ATL
	 * draws in sRGB only.
	 *
	 * @throws IllegalArgumentException if the color space encoded in the {@code ColorLong}
	 *      is invalid or unknown.
	 */
	public void setColor(long color) {
		setColor(Color.toArgb(color));
	}

	public void setARGB(int a, int r, int g, int b) {
		setColor(Color.argb(a, r, g, b));
	}

	@Export
	public int getColor() {
		return native_get_color(paint);
	}

	/**
	 * Return the paint's color as a {@code ColorLong} in sRGB.
	 */
	public long getColorLong() {
		return Color.pack(getColor());
	}

	public void setAlpha(int a) {
		native_set_alpha(paint, a);
	}

	public int getAlpha() {
		return native_get_alpha(paint);
	}

	public void setAntiAlias(boolean aa) {
		setFlag(ANTI_ALIAS_FLAG, aa);
	}

	public boolean isAntiAlias() {
		return (flags & ANTI_ALIAS_FLAG) != 0;
	}

	public boolean setFontVariationSettings(String fvs) {
		font_variation_settings = fvs;
		return true;
	}

	/**
	 * Returns the font variation settings.
	 */
	public String getFontVariationSettings() {
		return font_variation_settings;
	}

	public void setStrokeWidth(float width) {
		stroke_width = width;
		native_set_stroke_width(paint, width);
	}
	public void setTextSize(float size) {
		native_set_text_size(paint, size);
	}

	private Typeface typeface;

	public Typeface setTypeface(Typeface typeface) {
		this.typeface = typeface;
		applyTypeface();
		return typeface;
	}

	private void applyTypeface() {
		int weight = typeface != null ? typeface.getWeight() : 400;
		native_set_typeface(paint, typeface != null ? typeface.family : null, weight, typeface != null && typeface.isItalic());
	}
	public void getTextBounds(String text, int start, int end, Rect bounds) {
		if (end > text.length())
			end = text.length();
		native_get_text_bounds(paint, text.substring(start, end), bounds);
	}
	public void getTextBounds(CharSequence text, int start, int end, Rect bounds) {
		getTextBounds(text.toString(), start, end, bounds);
	}

	public void getTextBounds(char[] text, int index, int count, Rect bounds) {
		native_get_text_bounds(paint, new String(text, index, count), bounds);
	}
	/* AOSP Paint.getTextWidths(): nGetTextAdvances() over the range with the range as context, one advance per
	 * char (a cluster's advance on its first char, 0 for the others), returns the char count */
	public int getTextWidths(String text, int start, int end, float[] widths) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((start | end | (end - start) | (text.length() - end)) < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (end - start > widths.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (text.length() == 0 || start == end) {
			return 0;
		}
		native_get_text_advances(paint, text.toCharArray(), start, end - start, start, end - start, false, widths, 0);
		return end - start;
	}

	public int getTextWidths(CharSequence text, int start, int end, float[] widths) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((start | end | (end - start) | (text.length() - end)) < 0) {
			throw new IndexOutOfBoundsException();
		}
		return getTextWidths(text.toString(), start, end, widths);
	}

	public int getTextWidths(char[] text, int index, int count, float[] widths) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((index | count) < 0 || index + count > text.length || count > widths.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (text.length == 0 || count == 0) {
			return 0;
		}
		native_get_text_advances(paint, text, index, count, index, count, false, widths, 0);
		return count;
	}

	public int getTextWidths(String text, float[] widths) {
		return getTextWidths(text, 0, text.length(), widths);
	}
	public void setFilterBitmap(boolean filter) {
		setFlag(FILTER_BITMAP_FLAG, filter);
	}

	public void setFlags(int flags) {
		this.flags = flags;
		native_set_text_flags(paint, flags);
	}

	public void setStyle(Style style) {
		native_set_style(paint, style.ordinal());
	}

	/* AOSP nAscent: the ascent of the font metrics */
	public float ascent() {
		FontMetrics metrics = new FontMetrics();
		getFontMetrics(metrics);
		return metrics.ascent;
	}

	/* AOSP Paint.measureText(): ceil of nGetTextAdvances() over the range, the range being its own context */
	public float measureText(char[] text, int index, int count) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((index | count) < 0 || index + count > text.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (text.length == 0 || count == 0) {
			return 0f;
		}
		return (float)Math.ceil(native_get_text_advances(paint, text, index, count, index, count, false, null, 0));
	}

	public float measureText(String text, int start, int end) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((start | end | (end - start) | (text.length() - end)) < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (text.length() == 0 || start == end) {
			return 0f;
		}
		char[] buf = new char[end - start];
		text.getChars(start, end, buf, 0);
		return measureText(buf, 0, buf.length);
	}

	public float measureText(String text) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		return measureText(text, 0, text.length());
	}

	public float measureText(CharSequence text, int start, int end) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((start | end | (end - start) | (text.length() - end)) < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (text.length() == 0 || start == end) {
			return 0f;
		}
		char[] buf = new char[end - start];
		android.text.TextUtils.getChars(text, start, end, buf, 0);
		return measureText(buf, 0, buf.length);
	}

	/* AOSP Paint.getTextRunAdvances(): nGetTextAdvances() of the run shaped in its context, forced LTR or RTL */
	public float getTextRunAdvances(char[] chars, int index, int count, int contextIndex, int contextCount, boolean isRtl, float[] advances, int advancesIndex) {
		if (chars == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((index | count | contextIndex | contextCount | advancesIndex
		     | (index - contextIndex) | (contextCount - count)
		     | ((contextIndex + contextCount) - (index + count))
		     | (chars.length - (contextIndex + contextCount))
		     | (advances == null ? 0 : (advances.length - (advancesIndex + count)))) < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (chars.length == 0 || count == 0) {
			return 0f;
		}
		return native_get_text_advances(paint, chars, index, count, contextIndex, contextCount, isRtl, advances, advancesIndex);
	}

	/* AOSP Paint.getRunAdvance(): the advance from start to offset of the run [start, end) shaped in its context
	 * (minikin sums the char advances; a cluster's advance lies on its first char) */
	public float getRunAdvance(char[] text, int start, int end, int contextStart, int contextEnd, boolean isRtl, int offset) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((contextStart | start | offset | end | contextEnd
		     | start - contextStart | offset - start | end - offset
		     | contextEnd - end | text.length - contextEnd) < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (end == start) {
			return 0.0f;
		}
		float[] advances = new float[end - start];
		native_get_text_advances(paint, text, start, end - start, contextStart, contextEnd - contextStart, isRtl, advances, 0);
		float result = 0;
		for (int i = 0; i < offset - start; i++)
			result += advances[i];
		return result;
	}

	public float getRunAdvance(CharSequence text, int start, int end, int contextStart, int contextEnd, boolean isRtl, int offset) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((contextStart | start | offset | end | contextEnd
		     | start - contextStart | offset - start | end - offset
		     | contextEnd - end | text.length() - contextEnd) < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (end == start) {
			return 0.0f;
		}
		char[] buf = new char[contextEnd - contextStart];
		android.text.TextUtils.getChars(text, contextStart, contextEnd, buf, 0);
		return getRunAdvance(buf, start - contextStart, end - contextStart, 0, contextEnd - contextStart, isRtl, offset - contextStart);
	}

	/* AOSP Paint.getRunCharacterAdvance(): the char advances of [start, end) into advances[advancesIndex...] and
	 * the run advance at offset, as getRunAdvance() */
	public float getRunCharacterAdvance(char[] text, int start, int end, int contextStart, int contextEnd, boolean isRtl, int offset, float[] advances, int advancesIndex) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if (contextStart < 0 || contextEnd > text.length) {
			throw new IndexOutOfBoundsException("Invalid Context Range: " + contextStart + ", " + contextEnd + " must be in 0, " + text.length);
		}
		if (start < contextStart || contextEnd < end) {
			throw new IndexOutOfBoundsException("Invalid start/end range: " + start + ", " + end + " must be in " + contextStart + ", " + contextEnd);
		}
		if (offset < start || end < offset) {
			throw new IndexOutOfBoundsException("Invalid offset position: " + offset + " must be in " + start + ", " + end);
		}
		if (advances != null && advances.length < advancesIndex - start + end) {
			throw new IndexOutOfBoundsException("Given array doesn't have enough space to receive the result, advances.length: "
			                                    + advances.length + " advanceIndex: " + advancesIndex + " needed space: " + (offset - start));
		}
		if (end == start) {
			return 0.0f;
		}
		float[] run = new float[end - start];
		native_get_text_advances(paint, text, start, end - start, contextStart, contextEnd - contextStart, isRtl, run, 0);
		if (advances != null)
			System.arraycopy(run, 0, advances, advancesIndex, end - start);
		float result = 0;
		for (int i = 0; i < offset - start; i++)
			result += run[i];
		return result;
	}

	public float getRunCharacterAdvance(CharSequence text, int start, int end, int contextStart, int contextEnd, boolean isRtl, int offset, float[] advances, int advancesIndex) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if (contextStart < 0 || contextEnd > text.length()) {
			throw new IndexOutOfBoundsException("Invalid Context Range: " + contextStart + ", " + contextEnd + " must be in 0, " + text.length());
		}
		if (start < contextStart || contextEnd < end) {
			throw new IndexOutOfBoundsException("Invalid start/end range: " + start + ", " + end + " must be in " + contextStart + ", " + contextEnd);
		}
		if (offset < start || end < offset) {
			throw new IndexOutOfBoundsException("Invalid offset position: " + offset + " must be in " + start + ", " + end);
		}
		if (advances != null && advances.length < advancesIndex - start + end) {
			throw new IndexOutOfBoundsException("Given array doesn't have enough space to receive the result, advances.length: "
			                                    + advances.length + " advanceIndex: " + advancesIndex + " needed space: " + (offset - start));
		}
		if (end == start) {
			return 0.0f;
		}
		char[] buf = new char[contextEnd - contextStart];
		android.text.TextUtils.getChars(text, contextStart, contextEnd, buf, 0);
		return getRunCharacterAdvance(buf, start - contextStart, end - contextStart, 0, contextEnd - contextStart, isRtl, offset - contextStart, advances, advancesIndex);
	}

	/* AOSP Paint.getTextPath(): the glyph outlines of the text at its origin x, y (moved by the alignment as in
	 * Canvas.drawText()), the path receives them in place of its previous contents */
	public void getTextPath(char[] text, int index, int count, float x, float y, Path path) {
		if ((index | count) < 0 || index + count > text.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		path.reset();
		native_get_text_path(paint, text, index, count, x, y, path.getBuilder());
	}

	public void getTextPath(String text, int start, int end, float x, float y, Path path) {
		if ((start | end | (end - start) | (text.length() - end)) < 0) {
			throw new IndexOutOfBoundsException();
		}
		path.reset();
		native_get_text_path(paint, text.toCharArray(), start, end - start, x, y, path.getBuilder());
	}

	public ColorFilter setColorFilter(ColorFilter colorFilter) {
		if (colorFilter instanceof PorterDuffColorFilter) {
			PorterDuffColorFilter porterDuff = (PorterDuffColorFilter)colorFilter;
			native_set_color_filter(paint, porterDuff.getMode().ordinal(), porterDuff.getColor());
		} else if (colorFilter instanceof BlendModeColorFilter && BlendMode.blendModeToPorterDuffMode(((BlendModeColorFilter)colorFilter).getMode()) != null) {
			/* AOSP's blend-mode filter: the Porter-Duff blend modes map one to one */
			BlendModeColorFilter blend = (BlendModeColorFilter)colorFilter;
			native_set_color_filter(paint, BlendMode.blendModeToPorterDuffMode(blend.getMode()).ordinal(), blend.getColor());
		} else {
			native_set_color_filter(paint, -1, 0);
		}
		color_filter = colorFilter;
		return colorFilter;
	}

	public ColorFilter getColorFilter() {
		return color_filter;
	}

	public Shader setShader(Shader shader) {
		this.shader = shader;
		return shader;
	}

	public enum Style {
		/**
		 * Geometry and text drawn with this style will be filled, ignoring all
		 * stroke-related settings in the paint.
		 */
		FILL,
		/**
		 * Geometry and text drawn with this style will be stroked, respecting
		 * the stroke-related fields on the paint.
		 */
		STROKE,
		/**
		 * Geometry and text drawn with this style will be both filled and
		 * stroked at the same time, respecting the stroke-related fields on
		 * the paint. This mode can give unexpected results if the geometry
		 * is oriented counter-clockwise. This restriction does not apply to
		 * either FILL or STROKE.
		 */
		FILL_AND_STROKE;

		public static final Style values[] = Style.values();
	}

	public static class FontMetrics {
		/**
		 * The maximum distance above the baseline for the tallest glyph in
		 * the font at a given text size.
		 */
		public float top;
		/**
		 * The recommended distance above the baseline for singled spaced text.
		 */
		public float ascent;
		/**
		 * The recommended distance below the baseline for singled spaced text.
		 */
		public float descent;
		/**
		 * The maximum distance below the baseline for the lowest glyph in
		 * the font at a given text size.
		 */
		public float bottom;
		/**
		 * The recommended additional space to add between lines of text.
		 */
		public float leading;
	}

	public static class FontMetricsInt {
		public int top;
		public int ascent;
		public int descent;
		public int bottom;
		public int leading;

		/* AOSP FontMetricsInt.set(): copy of the fields; from FontMetrics as GraphicsJNI::set_metrics_int rounds */
		public void set(FontMetricsInt fontMetricsInt) {
			top = fontMetricsInt.top;
			ascent = fontMetricsInt.ascent;
			descent = fontMetricsInt.descent;
			bottom = fontMetricsInt.bottom;
			leading = fontMetricsInt.leading;
		}

		public void set(FontMetrics fontMetrics) {
			top = (int)Math.floor(fontMetrics.top);
			ascent = Math.round(fontMetrics.ascent);
			descent = Math.round(fontMetrics.descent);
			bottom = (int)Math.ceil(fontMetrics.bottom);
			leading = Math.round(fontMetrics.leading);
		}

		@Override
		public String toString() {
			return "FontMetricsInt: top=" + top + " ascent=" + ascent + " descent=" + descent + " bottom=" + bottom + " leading=" + leading;
		}
	}

	/* AOSP Paint.RunInfo */
	public static final class RunInfo {
		private int mClusterCount = 0;

		public int getClusterCount() { return mClusterCount; }
		public void setClusterCount(int clusterCount) { mClusterCount = clusterCount; }
	}

	public int getFlags() { return flags; }

	public int getHinting() { return hinting; }
	public void setHinting(int mode) { hinting = mode; }

	public void setDither(boolean dither) {
		setFlag(DITHER_FLAG, dither);
	}
	public boolean isDither() {
		return (flags & DITHER_FLAG) != 0;
	}

	/* AOSP sets these flag bits natively; ATL keeps them for getFlags() and the getters, the native paint
	 * gets the ones that change how text is drawn (underline, strike-through, fake bold) */
	public void setLinearText(boolean linearText) {
		setFlag(LINEAR_TEXT_FLAG, linearText);
	}
	public void setSubpixelText(boolean subpixelText) {
		setFlag(SUBPIXEL_TEXT_FLAG, subpixelText);
	}
	public void setUnderlineText(boolean underlineText) {
		setFlag(UNDERLINE_TEXT_FLAG, underlineText);
	}
	public void setStrikeThruText(boolean strikeThruText) {
		setFlag(STRIKE_THRU_TEXT_FLAG, strikeThruText);
	}
	public final boolean isLinearText() {
		return (flags & LINEAR_TEXT_FLAG) != 0;
	}
	public final boolean isSubpixelText() {
		return (flags & SUBPIXEL_TEXT_FLAG) != 0;
	}
	public final boolean isUnderlineText() {
		return (flags & UNDERLINE_TEXT_FLAG) != 0;
	}
	public final boolean isStrikeThruText() {
		return (flags & STRIKE_THRU_TEXT_FLAG) != 0;
	}
	/* like Skia, fake bold strokes the glyph outlines of the same font (natively), it does not pick a bold font */
	public void setFakeBoldText(boolean fakeBoldText) {
		setFlag(FAKE_BOLD_TEXT_FLAG, fakeBoldText);
	}

	public float getStrokeWidth() {
		return stroke_width;
	}

	public float getStrokeMiter() { return stroke_miter; }
	public void setStrokeMiter(float miter) { stroke_miter = miter; }
	public /*native*/ float getTextSize() {
		return native_get_text_size(paint);
	}

	public float getTextScaleX() { return text_scale_x; }
	public void setTextScaleX(float scaleX) { text_scale_x = scaleX; }
	public float getTextSkewX() { return text_skew_x; }
	public void setTextSkewX(float skewX) {
		text_skew_x = skewX;
		native_set_text_skew_x(paint, skewX);
	}

	/* AOSP Paint.getUnderlinePosition()/getUnderlineThickness() -> hwui jni/Paint.cpp (not in the local sources):
	 * the font's post table values when Skia has them, else Paint::kStdUnderline_Top (1/9 - 1/36) and
	 * Paint::kStdUnderline_Thickness (1/18) times the text size. ATL's native paint does not expose the font's
	 * values, so the fallback is used. */
	public float getUnderlinePosition() {
		return (1.0f / 9.0f - 0.5f * (1.0f / 18.0f)) * getTextSize();
	}

	public float getUnderlineThickness() {
		return (1.0f / 18.0f) * getTextSize();
	}

	/* AOSP Paint.equalsForTextMeasurement() -> nEqualsForTextMeasurement(): the same typeface, size, scale, skew,
	 * spacing, text flags, hinting, locales and font settings. Compared on the values ATL keeps. */
	public boolean equalsForTextMeasurement(Paint other) {
		return java.util.Objects.equals(typeface, other.typeface)
		    && getTextSize() == other.getTextSize()
		    && text_scale_x == other.text_scale_x
		    && text_skew_x == other.text_skew_x
		    && getLetterSpacing() == other.getLetterSpacing()
		    && mWordSpacing == other.mWordSpacing
		    && flags == other.flags
		    && hinting == other.hinting
		    && java.util.Objects.equals(text_locales, other.text_locales)
		    && java.util.Objects.equals(font_feature_settings, other.font_feature_settings)
		    && java.util.Objects.equals(font_variation_settings, other.font_variation_settings)
		    && elegant_text_height == other.elegant_text_height;
	}

	/* AOSP Paint.getOffsetForAdvance(): the offset in [start, end) on a grapheme cluster boundary whose run advance
	 * (getRunAdvance()) is closest to advance; minikin's getOffsetForAdvance() also only considers cluster
	 * boundaries */
	public int getOffsetForAdvance(char[] text, int start, int end, int contextStart, int contextEnd, boolean isRtl, float advance) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((contextStart | start | end | contextEnd
		     | start - contextStart | end - start | contextEnd - end
		     | text.length - contextEnd) < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (end == start) {
			return start;
		}
		float[] advances = new float[end - start];
		native_get_text_advances(paint, text, start, end - start, contextStart, contextEnd - contextStart, isRtl, advances, 0);
		java.text.BreakIterator it = java.text.BreakIterator.getCharacterInstance();
		it.setText(new String(text, contextStart, contextEnd - contextStart));
		int best = start;
		float bestDistance = Math.abs(advance);
		float pos = 0;
		for (int i = start + 1; i <= end; i++) {
			pos += advances[i - 1 - start];
			if (i < end && !it.isBoundary(i - contextStart))
				continue;
			float distance = Math.abs(pos - advance);
			if (distance < bestDistance) {
				best = i;
				bestDistance = distance;
			}
		}
		return best;
	}

	public int getOffsetForAdvance(CharSequence text, int start, int end, int contextStart, int contextEnd, boolean isRtl, float advance) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((contextStart | start | end | contextEnd
		     | start - contextStart | end - start | contextEnd - end
		     | text.length() - contextEnd) < 0) {
			throw new IndexOutOfBoundsException();
		}
		char[] buf = new char[contextEnd - contextStart];
		android.text.TextUtils.getChars(text, contextStart, contextEnd, buf, 0);
		return getOffsetForAdvance(buf, start - contextStart, end - contextStart, 0, contextEnd - contextStart, isRtl, advance) + contextStart;
	}

	/* AOSP Paint.getStrikeThruPosition()/getStrikeThruThickness() -> hwui jni/Paint.cpp: the font's OS/2 table
	 * values when Skia has them, else Paint::kStdStrikeThru_Top (-6/21) and Paint::kStdStrikeThru_Thickness (1/18)
	 * times the text size. ATL's native paint does not expose the font's values, so the fallback is used. */
	public float getStrikeThruPosition() {
		return (-6.0f / 21.0f) * getTextSize();
	}

	public float getStrikeThruThickness() {
		return (1.0f / 18.0f) * getTextSize();
	}

	/* AOSP Paint.get/setStartHyphenEdit(), get/setEndHyphenEdit(): kept in the native paint (hyphen edit bits),
	 * ATL keeps the values in fields. */
	private int start_hyphen_edit;
	private int end_hyphen_edit;
	public int getStartHyphenEdit() { return start_hyphen_edit; }
	public int getEndHyphenEdit() { return end_hyphen_edit; }
	public void setStartHyphenEdit(int startHyphen) { start_hyphen_edit = startHyphen; }
	public void setEndHyphenEdit(int endHyphen) { end_hyphen_edit = endHyphen; }

	/* AOSP Paint.setFontVariationOverride(): the string is kept (mFontVariationOverride), same string = no-op */
	private String font_variation_override;
	public void setFontVariationOverride(String fontVariationOverride) {
		if (java.util.Objects.equals(fontVariationOverride, font_variation_override))
			return;
		font_variation_override = fontVariationOverride;
	}
	public String getFontVariationOverride() { return font_variation_override; }

	/* AOSP Paint.getFontMetricsForLocale()/getFontMetricsIntForLocale(): the metrics with the locale's line height
	 * adjustment; ATL's native font has no per-locale adjustment, so these are the plain metrics. */
	public void getFontMetricsForLocale(FontMetrics metrics) { getFontMetrics(metrics); }
	public void getFontMetricsIntForLocale(FontMetricsInt metrics) { getFontMetricsInt(metrics); }

	/* AOSP Paint.getFontMetricsInt(CharSequence/char[], ...): argument checks, then the metrics of the run; the
	 * native side widens them by the fallback fonts used for the run, ATL has no per-run fonts. */
	public void getFontMetricsInt(CharSequence text, int start, int count, int contextStart, int contextCount,
	                              boolean isRtl, FontMetricsInt outMetrics) {
		if (text == null) {
			throw new IllegalArgumentException("text must not be null");
		}
		check_font_metrics_args(text.length(), start, count, contextStart, contextCount, outMetrics);
		getFontMetricsInt(outMetrics);
	}

	public void getFontMetricsInt(char[] text, int start, int count, int contextStart, int contextCount,
	                              boolean isRtl, FontMetricsInt outMetrics) {
		if (text == null) {
			throw new IllegalArgumentException("text must not be null");
		}
		check_font_metrics_args(text.length, start, count, contextStart, contextCount, outMetrics);
		getFontMetricsInt(outMetrics);
	}

	private static void check_font_metrics_args(int length, int start, int count, int contextStart, int contextCount,
	                                            FontMetricsInt outMetrics) {
		if (start < 0 || start >= length) {
			throw new IllegalArgumentException("start argument is out of bounds.");
		}
		if (count < 0 || start + count > length) {
			throw new IllegalArgumentException("count argument is out of bounds.");
		}
		if (contextStart < 0 || contextStart >= length) {
			throw new IllegalArgumentException("ctxStart argument is out of bounds.");
		}
		if (contextCount < 0 || contextStart + contextCount > length) {
			throw new IllegalArgumentException("ctxCount argument is out of bounds.");
		}
		if (outMetrics == null) {
			throw new IllegalArgumentException("outMetrics must not be null.");
		}
	}

	public float getTextRunAdvances(CharSequence text, int start, int end, int contextStart, int contextEnd,
	                                boolean isRtl, float[] advances, int advancesIndex) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((start | end | contextStart | contextEnd | advancesIndex | (end - start)
		     | (start - contextStart) | (contextEnd - end)
		     | (text.length() - contextEnd)
		     | (advances == null ? 0 : (advances.length - (advancesIndex + (end - start))))) < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (text.length() == 0 || start == end) {
			return 0f;
		}
		/* AOSP: copy the context into a char[] and measure there (native shaping as the char[] variant) */
		char[] buf = new char[contextEnd - contextStart];
		android.text.TextUtils.getChars(text, contextStart, contextEnd, buf, 0);
		return getTextRunAdvances(buf, start - contextStart, end - start, 0, contextEnd - contextStart, isRtl, advances, advancesIndex);
	}

	public float getRunCharacterAdvance(char[] text, int start, int end, int contextStart, int contextEnd,
	                                    boolean isRtl, int offset, float[] advances, int advancesIndex,
	                                    RectF drawBounds, RunInfo runInfo) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		return run_character_advance(text.length, start, end, contextStart, contextEnd, offset, advances, advancesIndex,
		                             drawBounds, runInfo, () -> getRunCharacterAdvance(text, start, end, contextStart, contextEnd, isRtl, offset, advances, advancesIndex));
	}

	public float getRunCharacterAdvance(CharSequence text, int start, int end, int contextStart, int contextEnd,
	                                    boolean isRtl, int offset, float[] advances, int advancesIndex,
	                                    RectF drawBounds, RunInfo runInfo) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		return run_character_advance(text.length(), start, end, contextStart, contextEnd, offset, advances, advancesIndex,
		                             drawBounds, runInfo, () -> getRunCharacterAdvance(text, start, end, contextStart, contextEnd, isRtl, offset, advances, advancesIndex));
	}

	private float run_character_advance(int length, int start, int end, int contextStart, int contextEnd, int offset,
	                                    float[] advances, int advancesIndex, RectF drawBounds, RunInfo runInfo,
	                                    java.util.function.Supplier<Float> measure) {
		if (contextStart < 0 || contextEnd > length) {
			throw new IndexOutOfBoundsException("Invalid Context Range: " + contextStart + ", " + contextEnd
			                                    + " must be in 0, " + length);
		}
		if (start < contextStart || contextEnd < end) {
			throw new IndexOutOfBoundsException("Invalid start/end range: " + start + ", " + end
			                                    + " must be in " + contextStart + ", " + contextEnd);
		}
		if (offset < start || end < offset) {
			throw new IndexOutOfBoundsException("Invalid offset position: " + offset + " must be in " + start + ", " + end);
		}
		if (advances != null && advances.length < advancesIndex - start + end) {
			throw new IndexOutOfBoundsException("Given array doesn't have enough space to receive the result, advances.length: "
			                                    + advances.length + " advanceIndex: " + advancesIndex + " needed space: "
			                                    + (offset - start));
		}
		if (end == start) {
			if (runInfo != null)
				runInfo.setClusterCount(0);
			return 0.0f;
		}
		float sum = measure.get();
		if (drawBounds != null) {
			FontMetrics metrics = new FontMetrics();
			getFontMetrics(metrics);
			drawBounds.set(0, metrics.top, sum, metrics.bottom);
		}
		if (runInfo != null)
			runInfo.setClusterCount(offset - start);
		return sum;
	}

	/* AOSP nDescent: the descent of the font metrics */
	public float descent() {
		FontMetrics metrics = new FontMetrics();
		getFontMetrics(metrics);
		return metrics.descent;
	}
	public float getFontMetrics(FontMetrics metrics) {
		return native_get_font_metrics(paint, metrics);
	}
	/* AOSP: getFontMetrics(null), i.e. descent - ascent + leading */
	public float getFontSpacing() {
		return getFontMetrics(null);
	}
	public int getFontMetricsInt(FontMetricsInt fmi) {
		return native_get_font_metrics_int(paint, fmi);
	}

	public void setShadowLayer(float radius, float dx, float dy, int shadowColor) {
		setShadowLayer(radius, dx, dy, Color.pack(shadowColor));
	}

	/* AOSP Paint.setShadowLayer; nSetShadowLayer clears the shadow for radius <= 0 */
	public void setShadowLayer(float radius, float dx, float dy, long shadowColor) {
		shadow_layer_radius = radius;
		shadow_layer_dx = dx;
		shadow_layer_dy = dy;
		shadow_layer_color = shadowColor;
		native_set_shadow_layer(paint, radius, dx, dy, Color.toArgb(shadowColor));
	}

	/* AOSP Paint.getShadowLayerColor() (int) and getShadowLayerColorLong() */
	public int getShadowLayerColor() {
		return Color.toArgb(shadow_layer_color);
	}

	public long getShadowLayerColorLong() {
		return shadow_layer_color;
	}

	public float getShadowLayerRadius() {
		return shadow_layer_radius;
	}

	public float getShadowLayerDx() {
		return shadow_layer_dx;
	}

	public float getShadowLayerDy() {
		return shadow_layer_dy;
	}

	public Xfermode setXfermode(Xfermode xfermode) {
		this.xfermode = xfermode;
		return xfermode;
	}

	public Xfermode getXfermode() {
		return xfermode;
	}

	public void setBlendMode(BlendMode blendmode) {
		setXfermode(blendmode == null ? null : blendmode.getXfermode());
	}

	public BlendMode getBlendMode() {
		if (this.xfermode instanceof PorterDuffXfermode) {
			return BlendMode.fromValue(((PorterDuffXfermode)this.xfermode).porterDuffMode);
		}
		return null;
	}

	public void setLetterSpacing(float spacing) {
		native_set_letter_spacing(paint, spacing);
	}

	public enum Cap {
		/**
		 * The stroke ends with the path, and does not project beyond it.
		 */
		BUTT,
		/**
		 * The stroke projects out as a semicircle, with the center at the
		 * end of the path.
		 */
		ROUND,
		/**
		 * The stroke projects out as a square, with the center at the end
		 * of the path.
		 */
		SQUARE;

		public static final Cap values[] = Cap.values();
	}

	public enum Join {
		/**
		 * The outer edges of a join meet at a sharp angle
		 */
		MITER,
		/**
		 * The outer edges of a join meet in a circular arc.
		 */
		ROUND,
		/**
		 * The outer edges of a join meet with a straight line
		 */
		BEVEL;

		public static final Join values[] = Join.values();
	}

	public enum Align {
		LEFT,
		CENTER,
		RIGHT,
	}

	public void setStrokeCap(Cap cap) {
		native_set_stroke_cap(paint, cap.ordinal());
	}

	public void setStrokeJoin(Join join) {
		native_set_stroke_join(paint, join.ordinal());
	}

	public Typeface getTypeface() {
		return typeface;
	}

	public void setTextAlign(Align align) {
		this.align = align;
		native_set_text_align(paint, align.ordinal());
	}

	public Shader getShader() {
		return shader;
	}

	public PathEffect getPathEffect() {
		return path_effect;
	}

	/* of the path effects, only dashes are drawn (as the dash of the GskStroke) */
	public PathEffect setPathEffect(PathEffect effect) {
		path_effect = effect;
		if (effect instanceof DashPathEffect)
			native_set_dash(paint, ((DashPathEffect)effect).intervals, ((DashPathEffect)effect).phase);
		else
			native_set_dash(paint, null, 0);
		return effect;
	}

	/* AOSP Paint.breakText(): nBreakText() measures the chars of the range (count < 0: backwards from its end) and
	 * stops before the char whose advance would exceed maxWidth; a surrogate pair is not split. Returns the number
	 * of chars measured, measuredWidth[0] receives their width. */
	public int breakText(char[] text, int index, int count, float maxWidth, float[] measuredWidth) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if (index < 0 || text.length - index < Math.abs(count)) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (text.length == 0 || count == 0) {
			return 0;
		}
		boolean forwards = count > 0;
		int n = Math.abs(count);
		float[] advances = new float[n];
		native_get_text_advances(paint, text, index, n, index, n, false, advances, 0);
		float width = 0;
		int i = 0;
		while (i < n) {
			int pos = forwards ? i : n - 1 - i;
			int step = 1;
			float w = advances[pos];
			if (forwards && pos + 1 < n && Character.isHighSurrogate(text[index + pos]) && Character.isLowSurrogate(text[index + pos + 1])) {
				step = 2;
				w += advances[pos + 1];
			} else if (!forwards && pos > 0 && Character.isLowSurrogate(text[index + pos]) && Character.isHighSurrogate(text[index + pos - 1])) {
				step = 2;
				w += advances[pos - 1];
			}
			if (width + w > maxWidth)
				break;
			width += w;
			i += step;
		}
		if (measuredWidth != null)
			measuredWidth[0] = width;
		return i;
	}

	public int breakText(String text, boolean measureForwards, float maxWidth, float[] measuredWidth) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if (text.length() == 0) {
			return 0;
		}
		char[] buf = text.toCharArray();
		return breakText(buf, 0, measureForwards ? buf.length : -buf.length, maxWidth, measuredWidth);
	}

	public int breakText(CharSequence text, int start, int end, boolean measureForwards, float maxWidth, float[] measuredWidth) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((start | end | (end - start) | (text.length() - end)) < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (text.length() == 0 || start == end) {
			return 0;
		}
		char[] buf = new char[end - start];
		android.text.TextUtils.getChars(text, start, end, buf, 0);
		return breakText(buf, 0, measureForwards ? end - start : -(end - start), maxWidth, measuredWidth);
	}

	public void clearShadowLayer() {
		setShadowLayer(0, 0, 0, 0);
	}

	/* AOSP nHasShadowLayer: SkPaint has a blur looper, set only for radius > 0 */
	public boolean hasShadowLayer() {
		return shadow_layer_radius > 0;
	}

	public FontMetrics getFontMetrics() {
		FontMetrics metrics = new FontMetrics();
		native_get_font_metrics(paint, metrics);
		return metrics;
	}

	public void setFontMetricsInt(FontMetricsInt fmi) {}

	public FontMetricsInt getFontMetricsInt() {
		FontMetricsInt fmi = new FontMetricsInt();
		native_get_font_metrics_int(paint, fmi);
		return fmi;
	}

	public void set(Paint paint) {
		native_recycle(this.paint);
		this.paint = native_clone(paint.paint);
		copyJavaState(paint);
	}

	public boolean isFilterBitmap() { return (flags & FILTER_BITMAP_FLAG) != 0; }

	public Cap getStrokeCap() {
		return Cap.values[native_get_stroke_cap(paint)];
	}

	public Join getStrokeJoin() {
		return Join.values[native_get_stroke_join(paint)];
	}

	public Locale getTextLocale() { return text_locales != null ? text_locales.get(0) : Locale.getDefault(); }

	/**
	 * Set the text locale, i.e. a list of just that locale.
	 */
	public void setTextLocale(Locale locale) {
		if (locale == null) {
			throw new IllegalArgumentException("locale cannot be null");
		}
		setTextLocales(new LocaleList(locale));
	}

	/**
	 * Get the text locale list.
	 */
	public LocaleList getTextLocales() {
		return text_locales != null ? text_locales : new LocaleList(Locale.getDefault());
	}

	/**
	 * Set the text locale list.
	 */
	public void setTextLocales(LocaleList locales) {
		if (locales == null || locales.isEmpty()) {
			throw new IllegalArgumentException("locales cannot be null or empty");
		}
		text_locales = locales;
	}

	/**
	 * Get the elegant metrics flag.
	 */
	public boolean isElegantTextHeight() {
		return elegant_text_height;
	}

	/**
	 * Set the paint's elegant height metrics flag.
	 */
	public void setElegantTextHeight(boolean elegant) {
		elegant_text_height = elegant;
	}

	/* AOSP Paint.setWordSpacing()/getWordSpacing(): extra advance added at each space; kept, not applied by
	 * the Pango text path */
	private float mWordSpacing;

	public void setWordSpacing(float wordSpacing) {
		mWordSpacing = wordSpacing;
	}

	public float getWordSpacing() {
		return mWordSpacing;
	}

	public float getLetterSpacing() {
		return native_get_letter_spacing(paint);
	}

	public Style getStyle() {
		return Style.values[native_get_style(paint)];
	}

	public Align getTextAlign() {
		return align;
	}

	/* AOSP Paint.hasGlyph(): whether the font (with its fallbacks) has a glyph for the string - for more than
	 * one character only if they make a single glyph (a ligature, an emoji sequence) */
	public boolean hasGlyph(String text) {
		return native_has_glyph(paint, text);
	}

	private MaskFilter mask_filter;
	/* AOSP Paint.setMaskFilter()/getMaskFilter(): the filter is kept in a Java field (mMaskFilter) */
	public MaskFilter setMaskFilter(MaskFilter filter) { mask_filter = filter; return filter; }
	public MaskFilter getMaskFilter() { return mask_filter; }

	public boolean isFakeBoldText() {
		return (flags & FAKE_BOLD_TEXT_FLAG) != 0;
	}

	public void setFontFeatureSettings(String settings) {
		if (settings != null && settings.equals("")) {
			settings = null;
		}
		font_feature_settings = settings;
	}

	/**
	 * Returns the font feature settings.
	 */
	public String getFontFeatureSettings() {
		return font_feature_settings;
	}

	/* AOSP getFillPath() -> SkPaint::getFillPath(): FILL leaves the path as it is; a stroke becomes the outline of the
	 * stroke (to fill with WINDING, inverse like src); a hairline (width 0) stays the path, to draw as a line (false).
	 * Of the path effects ATL has the dash of DashPathEffect (only for strokes, like Skia), the others are not applied
	 * here either, as when drawing. */
	public boolean getFillPath(Path src, Path dst) {
		Style style = getStyle();
		boolean hairline = style != Style.FILL && stroke_width <= 0;
		if (style == Style.FILL || (hairline && !(path_effect instanceof DashPathEffect))) {
			if (dst != src)
				dst.set(src);
			return !hairline;
		}
		if (dst == src)
			src = new Path(src);
		boolean inverse = src.isInverseFillType();
		dst.reset();
		native_get_fill_path(paint, src.getGskPath(), dst.getBuilder(), hairline ? 0 : stroke_width, stroke_miter);
		dst.setFillType(inverse ? Path.FillType.INVERSE_WINDING : Path.FillType.WINDING);
		return !hairline;
	}

	public void reset() {
		native_recycle(paint);
		paint = native_create();
		xfermode = null;
		shader = null;
		align = Align.CENTER;
		color_filter = null;
		stroke_width = 0;
		path_effect = null;
		mWordSpacing = 0;
		shadow_layer_radius = 0;
		shadow_layer_dx = 0;
		shadow_layer_dy = 0;
		shadow_layer_color = 0;
		text_skew_x = 0;
		native_set_text_flags(paint, flags);
	}

	private static native long native_create();
	private static native long native_clone(long paint);
	private static native void native_recycle(long paint);
	private static native void native_set_color(long paint, int color);
	private static native int native_get_color(long paint);
	private static native void native_set_alpha(long paint, int alpha);
	private static native int native_get_alpha(long paint);
	private static native void native_set_style(long paint, int style);
	private static native int native_get_style(long paint);
	private static native void native_set_stroke_width(long paint, float width);
	private static native void native_get_fill_path(long paint, long src, long dst, float width, float miter);
	private static native void native_set_dash(long paint, float[] intervals, float phase);
	private static native void native_set_stroke_cap(long paint, int cap);
	private static native int native_get_stroke_cap(long paint);
	private static native void native_set_stroke_join(long paint, int join);
	private static native int native_get_stroke_join(long paint);
	private static native void native_set_text_size(long paint, float size);
	private static native void native_set_typeface(long paint, String family, int weight, boolean italic);
	private static native float native_get_text_size(long paint);
	private static native void native_set_letter_spacing(long paint, float spacing);
	private static native float native_get_letter_spacing(long paint);
	private static native void native_set_color_filter(long paint, int mode, int color);
	private static native void native_get_text_bounds(long paint, String text, Rect bounds);
	private static native boolean native_has_glyph(long paint, String text);
	private static native void native_set_text_align(long paint, int align);
	private static native float native_get_font_metrics(long paint, FontMetrics metrics);
	private static native int native_get_font_metrics_int(long paint, FontMetricsInt metrics);
	private static native void native_set_text_flags(long paint, int flags);
	private static native void native_set_text_skew_x(long paint, float skewX);
	private static native void native_set_shadow_layer(long paint, float radius, float dx, float dy, int color);
	private static native float native_get_text_advances(long paint, char[] text, int index, int count, int contextIndex, int contextCount, boolean isRtl, float[] advances, int advancesIndex);
	private static native void native_get_text_path(long paint, char[] text, int index, int count, float x, float y, long builder);

	/** Option for getTextRunCursor: the next cursor position after the offset */
	public static final int CURSOR_AFTER = 0;
	public static final int CURSOR_AT_OR_AFTER = 1;
	public static final int CURSOR_BEFORE = 2;
	public static final int CURSOR_AT_OR_BEFORE = 3;
	public static final int CURSOR_AT = 4;

	/**
	 * Returns the next cursor position in the run, as AOSP's native getTextRunCursor() does: positions are the
	 * grapheme cluster boundaries (the run's direction does not change which they are), -1 if there is none.
	 */
	public int getTextRunCursor(char[] text, int contextStart, int contextLength, boolean isRtl, int offset, int cursorOpt) {
		int contextEnd = contextStart + contextLength;
		if ((contextStart | contextEnd | offset | (contextEnd - contextStart) | (offset - contextStart) | (contextEnd - offset)
				| (text.length - contextEnd) | cursorOpt) < 0 || cursorOpt > CURSOR_AT) {
			throw new IndexOutOfBoundsException();
		}
		return getTextRunCursor(new String(text), contextStart, contextEnd, isRtl, offset, cursorOpt);
	}

	public int getTextRunCursor(CharSequence text, int contextStart, int contextEnd, boolean isRtl, int offset, int cursorOpt) {
		if (text instanceof String || text instanceof android.text.SpannedString || text instanceof android.text.SpannableString) {
			return getTextRunCursor(text.toString(), contextStart, contextEnd, isRtl, offset, cursorOpt);
		}
		if (text instanceof android.text.GetChars || text instanceof StringBuffer || text instanceof StringBuilder) {
			return getTextRunCursor(text.toString(), contextStart, contextEnd, isRtl, offset, cursorOpt);
		}
		return getTextRunCursor(text.toString(), contextStart, contextEnd, isRtl, offset, cursorOpt);
	}

	public int getTextRunCursor(String text, int contextStart, int contextEnd, boolean isRtl, int offset, int cursorOpt) {
		if (((contextStart | contextEnd | offset | (contextEnd - contextStart) | (offset - contextStart) | (contextEnd - offset)
				| (text.length() - contextEnd) | cursorOpt) < 0) || cursorOpt > CURSOR_AT) {
			throw new IndexOutOfBoundsException();
		}
		java.text.BreakIterator it = java.text.BreakIterator.getCharacterInstance();
		it.setText(text.substring(contextStart, contextEnd));
		int o = offset - contextStart;
		int result;
		switch (cursorOpt) {
			case CURSOR_AFTER:
				result = it.following(o);
				break;
			case CURSOR_AT_OR_AFTER:
				result = it.isBoundary(o) ? o : it.following(o);
				break;
			case CURSOR_BEFORE:
				result = it.preceding(o);
				break;
			case CURSOR_AT_OR_BEFORE:
				result = it.isBoundary(o) ? o : it.preceding(o);
				break;
			default:
				result = it.isBoundary(o) ? o : -1;
				break;
		}
		return result == java.text.BreakIterator.DONE ? -1 : result + contextStart;
	}
}
