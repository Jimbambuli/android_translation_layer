package android.graphics;

import android.atl.annotation.Export;
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

	private void copyJavaState(Paint paint) {
		flags = paint.flags;
		typeface = paint.typeface;
		text_scale_x = paint.text_scale_x;
		stroke_miter = paint.stroke_miter;
		hinting = paint.hinting;
		stroke_width = paint.stroke_width;
		path_effect = paint.path_effect;
	}

	private void setFlag(int flag, boolean on) {
		flags = on ? flags | flag : flags & ~flag;
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
		return true;
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
		if (isFakeBoldText() && weight < 700)
			weight = 700;
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
	public int getTextWidths(String text, int start, int end, float[] widths) {
		Rect bounds = new Rect();
		native_get_text_bounds(paint, text.substring(start, end), bounds);
		return bounds.width();
	}
	public void setFilterBitmap(boolean filter) {
		setFlag(FILTER_BITMAP_FLAG, filter);
	}

	public void setFlags(int flags) {
		boolean fakeBoldChanged = ((this.flags ^ flags) & FAKE_BOLD_TEXT_FLAG) != 0;
		this.flags = flags;
		if (fakeBoldChanged)
			applyTypeface();
	}

	public void setStyle(Style style) {
		native_set_style(paint, style.ordinal());
	}

	public float ascent() {
		return -getTextSize();
	}

	public float measureText(char[] text, int index, int count) { return 10; }
	public float measureText(String text, int start, int end) {
		return (end - start) * getTextSize() * .6f;
	}
	public float measureText(String text) {
		return measureText(text, 0, text.length());
	}
	public float measureText(CharSequence text, int start, int end) {
		return measureText(text.toString(), start, end);
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

		@Override
		public String toString() {
			return "FontMetricsInt: top=" + top + " ascent=" + ascent + " descent=" + descent + " bottom=" + bottom + " leading=" + leading;
		}
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

	public /*native*/ void setLinearText(boolean linearText) {}
	public /*native*/ void setSubpixelText(boolean subpixelText) {}
	public /*native*/ void setUnderlineText(boolean underlineText) {}
	public /*native*/ void setStrikeThruText(boolean strikeThruText) {}
	/* Pango has no synthetic emboldening of its own, so fake bold asks for a bold weight */
	public void setFakeBoldText(boolean fakeBoldText) {
		flags = fakeBoldText ? (flags | FAKE_BOLD_TEXT_FLAG) : (flags & ~FAKE_BOLD_TEXT_FLAG);
		applyTypeface();
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
	public /*native*/ float getTextSkewX() { return 0; }
	public /*native*/ void setTextSkewX(float skewX) {}

	public /*native*/ float descent() { return 0; }
	public float getFontMetrics(FontMetrics metrics) {
		return native_get_font_metrics(paint, metrics);
	}
	public int getFontMetricsInt(FontMetricsInt fmi) {
		return native_get_font_metrics_int(paint, fmi);
	}

	public void setShadowLayer(float radius, float dx, float dy, int color) {}

	public void setShadowLayer(float radius, float dx, float dy, long shadowColor) {
		setShadowLayer(radius, dx, dy, Color.toArgb(shadowColor));
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

	public int breakText(char[] text, int index, int count, float maxWidth, float[] measuredWidth) { return 10; }
	public int breakText(String text, boolean measureForwards, float maxWidth, float[] measuredWidth) { return 10; }
	public int breakText(CharSequence text, int start, int end, boolean measureForwards, float maxWidth, float[] measuredWidth) { return 10; }

	public void clearShadowLayer() {}

	public boolean hasShadowLayer() {
		return false;
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

	public Locale getTextLocale() { return Locale.getDefault(); }

	public float getLetterSpacing() {
		return native_get_letter_spacing(paint);
	}

	public Style getStyle() {
		return Style.values[native_get_style(paint)];
	}

	public Align getTextAlign() {
		return align;
	}

	public boolean hasGlyph(String text) { return false; }

	public MaskFilter setMaskFilter(MaskFilter filter) { return filter; }

	public boolean isFakeBoldText() {
		return (flags & FAKE_BOLD_TEXT_FLAG) != 0;
	}

	public void setFontFeatureSettings(String settings) {}

	public boolean getFillPath(Path src, Path dst) {
		return false;
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
	private static native void native_set_text_align(long paint, int align);
	private static native float native_get_font_metrics(long paint, FontMetrics metrics);
	private static native int native_get_font_metrics_int(long paint, FontMetricsInt metrics);
}
