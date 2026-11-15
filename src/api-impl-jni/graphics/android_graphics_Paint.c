#include <glib.h>
#include <string.h>
#include <gsk/gsk.h>
#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"
#include "AndroidPaint.h"
#include "pango/pango-font.h"
#include <pango/pangocairo.h>

#include "../generated_headers/android_graphics_Paint.h"

JNIEXPORT jlong JNICALL Java_android_graphics_Paint_native_1create(JNIEnv *env, jclass clazz)
{
	struct AndroidPaint *paint = g_new0(struct AndroidPaint, 1);
	paint->color.alpha = 1.f;
	paint->gsk_stroke = gsk_stroke_new(1);
	paint->font = pango_font_description_new();
	paint->is_fill = true;
	return _INTPTR(paint);
}

JNIEXPORT jlong JNICALL Java_android_graphics_Paint_native_1clone(JNIEnv *env, jclass clazz, jlong paint_ptr)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	struct AndroidPaint *clone = g_memdup2(paint, sizeof(struct AndroidPaint));
	clone->gsk_stroke = gsk_stroke_copy(paint->gsk_stroke);
	clone->font = pango_font_description_copy(paint->font);
	return _INTPTR(clone);
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1recycle(JNIEnv *env, jclass clazz, jlong paint_ptr)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	gsk_stroke_free(paint->gsk_stroke);
	pango_font_description_free(paint->font);
	g_free(paint);
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1color(JNIEnv *env, jclass clazz, jlong paint_ptr, jint color)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	paint->color.red = ((color >> 16) & 0xFF) / 255.f;
	paint->color.green = ((color >> 8) & 0xFF) / 255.f;
	paint->color.blue = ((color >> 0) & 0xFF) / 255.f;
	paint->color.alpha = ((color >> 24) & 0xFF) / 255.f;
}

JNIEXPORT jint JNICALL Java_android_graphics_Paint_native_1get_1color(JNIEnv *env, jclass clazz, jlong paint_ptr)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	// unsigned, so that the alpha does not shift into the sign bit
	return (jint)(((uint32_t)(paint->color.red * 0xFF) << 16) + ((uint32_t)(paint->color.green * 0xFF) << 8) + ((uint32_t)(paint->color.blue * 0xFF) << 0) + ((uint32_t)(paint->color.alpha * 0xFF) << 24));
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1alpha(JNIEnv *env, jclass clazz, jlong paint_ptr, jint alpha)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	paint->color.alpha = (alpha & 0xFF) / 255.f;
}

JNIEXPORT int JNICALL Java_android_graphics_Paint_native_1get_1alpha(JNIEnv *env, jclass clazz, jlong paint_ptr)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	return (int)(paint->color.alpha * 0xFF);
}

#define STYLE_FILL            0
#define STYLE_STROKE          1
#define STYLE_FILL_AND_STROKE 2

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1style(JNIEnv *env, jclass clazz, jlong paint_ptr, jint style)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	paint->is_fill = style == STYLE_FILL || style == STYLE_FILL_AND_STROKE;
	paint->is_stroke = style == STYLE_STROKE || style == STYLE_FILL_AND_STROKE;
}

JNIEXPORT jint JNICALL Java_android_graphics_Paint_native_1get_1style(JNIEnv *env, jclass clazz, jlong paint_ptr)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	if (paint->is_fill && paint->is_stroke)
		return STYLE_FILL_AND_STROKE;
	else if (paint->is_fill)
		return STYLE_FILL;
	else
		return STYLE_STROKE;
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1dash(JNIEnv *env, jclass clazz, jlong paint_ptr, jfloatArray intervals_arr, jfloat phase)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	if (!intervals_arr) {
		gsk_stroke_set_dash(paint->gsk_stroke, NULL, 0);
		return;
	}
	jsize n = (*env)->GetArrayLength(env, intervals_arr);
	jfloat *intervals = (*env)->GetFloatArrayElements(env, intervals_arr, NULL);
	/* like Skia, an odd last interval is ignored */
	gsk_stroke_set_dash(paint->gsk_stroke, intervals, n & ~1);
	gsk_stroke_set_dash_offset(paint->gsk_stroke, phase);
	(*env)->ReleaseFloatArrayElements(env, intervals_arr, intervals, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1stroke_1width(JNIEnv *env, jclass clazz, jlong paint_ptr, jfloat width)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	/* TODO: width of 0 means "single pixel width, no matter what",
	 * meanwile width of 1 should care about scaling;
	 * The problem is, 0 is not a valid value for gsk_stroke_set_line_width,
	 * so at least change it to 1 for now */
	if (width == 0)
		width = 1;
	gsk_stroke_set_line_width(paint->gsk_stroke, width);
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1stroke_1cap(JNIEnv *env, jclass clazz, jlong paint_ptr, jint cap)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	gsk_stroke_set_line_cap(paint->gsk_stroke, cap);
}

JNIEXPORT jint JNICALL Java_android_graphics_Paint_native_1get_1stroke_1cap(JNIEnv *env, jclass clazz, jlong paint_ptr)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	return gsk_stroke_get_line_cap(paint->gsk_stroke);
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1stroke_1join(JNIEnv *env, jclass clazz, jlong paint_ptr, jint join)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	gsk_stroke_set_line_join(paint->gsk_stroke, join);
}

JNIEXPORT jint JNICALL Java_android_graphics_Paint_native_1get_1stroke_1join(JNIEnv *env, jclass clazz, jlong paint_ptr)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	return gsk_stroke_get_line_join(paint->gsk_stroke);
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1text_1size(JNIEnv *env, jclass clazz, jlong paint_ptr, jfloat size)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	pango_font_description_set_absolute_size(paint->font, roundf(size * PANGO_SCALE));
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1typeface(JNIEnv *env, jclass clazz, jlong paint_ptr, jstring family, jint weight, jboolean italic)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	if (family) {
		const char *family_str = (*env)->GetStringUTFChars(env, family, NULL);
		pango_font_description_set_family(paint->font, family_str);
		(*env)->ReleaseStringUTFChars(env, family, family_str);
	} else {
		pango_font_description_unset_fields(paint->font, PANGO_FONT_MASK_FAMILY);
	}
	pango_font_description_set_weight(paint->font, CLAMP(weight, 100, 1000));
	pango_font_description_set_style(paint->font, italic ? PANGO_STYLE_ITALIC : PANGO_STYLE_NORMAL);
}

JNIEXPORT jfloat JNICALL Java_android_graphics_Paint_native_1get_1text_1size(JNIEnv *env, jclass clazz, jlong paint_ptr)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	return (jfloat)pango_font_description_get_size(paint->font) / PANGO_SCALE;
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1letter_1spacing(JNIEnv *env, jclass clazz, jlong paint_ptr, jfloat spacing)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	paint->letter_spacing = spacing;
}

JNIEXPORT jfloat JNICALL Java_android_graphics_Paint_native_1get_1letter_1spacing(JNIEnv *env, jclass clazz, jlong paint_ptr)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	return paint->letter_spacing;
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1color_1filter(JNIEnv *env, jclass clazz, jlong paint_ptr, jint mode, jint color)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	/* clang-format off */
	graphene_matrix_init_from_float(&paint->color_matrix, (float[]){
		0, 0, 0, 0,
		0, 0, 0, 0,
		0, 0, 0, 0,
		0, 0, 0, ((color >> 24) & 0xFF) / 255.f,
	});
	/* clang-format on */
	graphene_vec4_init(&paint->color_offset, ((color >> 16) & 0xFF) / 255.f, ((color >> 8) & 0xFF) / 255.f, ((color >> 0) & 0xFF) / 255.f, 0);
	paint->use_color_filter = mode != -1;
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1get_1text_1bounds(JNIEnv *env, jclass clazz, jlong paint_ptr, jstring text_ptr, jobject bounds)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	PangoLayout *layout = pango_layout_new(atl_pango_context());
	pango_layout_set_font_description(layout, paint->font);
	android_paint_apply_letter_spacing(layout, paint);
	const char *str = (*env)->GetStringUTFChars(env, text_ptr, NULL);
	pango_layout_set_text(layout, str, -1);
	(*env)->ReleaseStringUTFChars(env, text_ptr, str);
	PangoRectangle rect;
	pango_layout_get_pixel_extents(layout, NULL, &rect);
	rect.y -= (float)pango_layout_get_baseline(layout) / PANGO_SCALE;
	if (paint->alignment == PANGO_ALIGN_CENTER)
		rect.x -= rect.width / 2.f;
	else if (paint->alignment == PANGO_ALIGN_RIGHT)
		rect.x -= rect.width;
	_SET_INT_FIELD(bounds, "left", rect.x);
	_SET_INT_FIELD(bounds, "top", rect.y);
	_SET_INT_FIELD(bounds, "right", rect.x + rect.width);
	_SET_INT_FIELD(bounds, "bottom", rect.y + rect.height);
	g_object_unref(layout);
}

/* like AOSP's hasGlyph() (hwui Paint.cpp): lay the string out with the paint's font and its fallbacks; true if
 * there is no missing glyph (.notdef) and, for more than one character, they made exactly one glyph */
JNIEXPORT jboolean JNICALL Java_android_graphics_Paint_native_1has_1glyph(JNIEnv *env, jclass clazz, jlong paint_ptr, jstring text_ptr)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	const jchar *chars = (*env)->GetStringChars(env, text_ptr, NULL);
	gchar *str = g_utf16_to_utf8((const gunichar2 *)chars, (*env)->GetStringLength(env, text_ptr), NULL, NULL, NULL);
	(*env)->ReleaseStringChars(env, text_ptr, chars);
	if (!str) // unpaired surrogate
		return false;
	glong n_chars = g_utf8_strlen(str, -1);
	PangoLayout *layout = pango_layout_new(atl_pango_context());
	pango_layout_set_font_description(layout, paint->font);
	pango_layout_set_text(layout, str, -1);
	g_free(str);
	int n_glyphs = 0;
	gboolean notdef = pango_layout_get_unknown_glyphs_count(layout) > 0;
	PangoLayoutIter *iter = pango_layout_get_iter(layout);
	do {
		PangoLayoutRun *run = pango_layout_iter_get_run_readonly(iter);
		if (!run)
			continue;
		for (int i = 0; i < run->glyphs->num_glyphs; i++) {
			PangoGlyph glyph = run->glyphs->glyphs[i].glyph;
			if (glyph & PANGO_GLYPH_UNKNOWN_FLAG)
				notdef = TRUE;
			else if (glyph != PANGO_GLYPH_EMPTY) // what AOSP's countNonSpaceGlyphs() leaves out
				n_glyphs++;
		}
	} while (pango_layout_iter_next_run(iter));
	pango_layout_iter_free(iter);
	g_object_unref(layout);
	if (n_glyphs != 1 && n_chars > 1)
		return false;
	return n_glyphs > 0 && !notdef;
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1text_1align(JNIEnv *env, jclass clazz, jlong paint_ptr, jint align)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	paint->alignment = align;
}

JNIEXPORT jfloat JNICALL Java_android_graphics_Paint_native_1get_1font_1metrics(JNIEnv *env, jclass clazz, jlong paint_ptr, jobject metrics)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	PangoContext *context = atl_pango_context();
	PangoFontMetrics *font_metrics = pango_context_get_metrics(context, paint->font, NULL);
	float height = (float)pango_font_metrics_get_height(font_metrics) / PANGO_SCALE;
	if (metrics) {
		float ascent = (float)pango_font_metrics_get_ascent(font_metrics) / PANGO_SCALE;
		float descent = (float)pango_font_metrics_get_descent(font_metrics) / PANGO_SCALE;
		_SET_FLOAT_FIELD(metrics, "top", -ascent);
		_SET_FLOAT_FIELD(metrics, "ascent", -ascent);
		_SET_FLOAT_FIELD(metrics, "descent", descent);
		_SET_FLOAT_FIELD(metrics, "bottom", descent);
		_SET_FLOAT_FIELD(metrics, "leading", height - ascent - descent);
	}
	pango_font_metrics_unref(font_metrics);
	return height;
}

JNIEXPORT jint JNICALL Java_android_graphics_Paint_native_1get_1font_1metrics_1int(JNIEnv *env, jclass clazz, jlong paint_ptr, jobject metrics)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	PangoContext *context = atl_pango_context();
	PangoFontMetrics *font_metrics = pango_context_get_metrics(context, paint->font, NULL);
	int height = ceil((float)pango_font_metrics_get_height(font_metrics) / PANGO_SCALE);
	if (metrics) {
		int ascent = ceil((float)pango_font_metrics_get_ascent(font_metrics) / PANGO_SCALE);
		int descent = ceil((float)pango_font_metrics_get_descent(font_metrics) / PANGO_SCALE);
		_SET_INT_FIELD(metrics, "top", -ascent);
		_SET_INT_FIELD(metrics, "ascent", -ascent);
		_SET_INT_FIELD(metrics, "descent", descent);
		_SET_INT_FIELD(metrics, "bottom", descent);
		_SET_INT_FIELD(metrics, "leading", height - ascent - descent);
	}
	pango_font_metrics_unref(font_metrics);
	return height;
}

/* the bits of Paint.getFlags() that change how text is drawn */
#define UNDERLINE_TEXT_FLAG (1 << 3)
#define STRIKE_THRU_TEXT_FLAG (1 << 4)
#define FAKE_BOLD_TEXT_FLAG (1 << 5)

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1text_1flags(JNIEnv *env, jclass clazz, jlong paint_ptr, jint flags)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	paint->underline = (flags & UNDERLINE_TEXT_FLAG) != 0;
	paint->strike_thru = (flags & STRIKE_THRU_TEXT_FLAG) != 0;
	paint->fake_bold = (flags & FAKE_BOLD_TEXT_FLAG) != 0;
}

JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1text_1skew_1x(JNIEnv *env, jclass clazz, jlong paint_ptr, jfloat skew_x)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	paint->text_skew_x = skew_x;
}

/* AOSP Paint.setShadowLayer() -> nSetShadowLayer: radius <= 0 removes the shadow */
JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1set_1shadow_1layer(JNIEnv *env, jclass clazz, jlong paint_ptr, jfloat radius, jfloat dx, jfloat dy, jint color)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	paint->shadow_radius = radius > 0 ? radius : 0;
	paint->shadow_dx = dx;
	paint->shadow_dy = dy;
	paint->shadow_color.red = ((color >> 16) & 0xFF) / 255.f;
	paint->shadow_color.green = ((color >> 8) & 0xFF) / 255.f;
	paint->shadow_color.blue = ((color >> 0) & 0xFF) / 255.f;
	paint->shadow_color.alpha = ((color >> 24) & 0xFF) / 255.f;
}

/* UNDERLINE_TEXT_FLAG and STRIKE_THRU_TEXT_FLAG as Pango attributes over the whole text, added to the attributes
 * the layout already has (Pango places and sizes the lines from the font's metrics, as Skia/minikin do) */
void android_paint_apply_text_decoration(PangoLayout *layout, struct AndroidPaint *paint)
{
	if (!paint->underline && !paint->strike_thru)
		return;
	PangoAttrList *attrs = pango_layout_get_attributes(layout);
	attrs = attrs ? pango_attr_list_copy(attrs) : pango_attr_list_new();
	if (paint->underline)
		pango_attr_list_insert_before(attrs, pango_attr_underline_new(PANGO_UNDERLINE_SINGLE));
	if (paint->strike_thru)
		pango_attr_list_insert_before(attrs, pango_attr_strikethrough_new(TRUE));
	pango_layout_set_attributes(layout, attrs);
	pango_attr_list_unref(attrs);
}

/* Skia's fake bold (SkScalerContext, kStdFakeBoldInterpKeys {9, 36} and kStdFakeBoldInterpValues {1/24, 1/32}):
 * the outline is stroked with an extra width of the text size times a factor interpolated over the text size */
static float fake_bold_extra(struct AndroidPaint *paint)
{
	float size = (float)pango_font_description_get_size(paint->font) / PANGO_SCALE;
	float factor;
	if (size <= 9)
		factor = 1 / 24.f;
	else if (size >= 36)
		factor = 1 / 32.f;
	else
		factor = 1 / 24.f + (size - 9) / (36 - 9) * (1 / 32.f - 1 / 24.f);
	return size * factor;
}

/* the stroke for the glyph outlines, NULL if they are only filled: Style.STROKE and FILL_AND_STROKE stroke with
 * the paint's stroke, fake bold adds its extra width to it (or, for a plain fill, is a stroke of that width) */
static GskStroke *text_outline_stroke(struct AndroidPaint *paint)
{
	float extra = paint->fake_bold ? fake_bold_extra(paint) : 0;
	if (paint->is_stroke) {
		GskStroke *stroke = gsk_stroke_copy(paint->gsk_stroke);
		gsk_stroke_set_line_width(stroke, gsk_stroke_get_line_width(stroke) + extra);
		return stroke;
	}
	if (extra > 0)
		return gsk_stroke_new(extra); // miter join with limit 4, like SkStrokeRec's default
	return NULL;
}

/* the outlines of the glyphs as cairo has them (GTK draws text with cairo, too), added to the GskPathBuilder */
static void add_cairo_path(GskPathBuilder *builder, cairo_t *cr)
{
	cairo_path_t *path = cairo_copy_path(cr);
	for (int i = 0; i < path->num_data; i += path->data[i].header.length) {
		cairo_path_data_t *d = &path->data[i];
		switch (d->header.type) {
			case CAIRO_PATH_MOVE_TO:
				gsk_path_builder_move_to(builder, d[1].point.x, d[1].point.y);
				break;
			case CAIRO_PATH_LINE_TO:
				gsk_path_builder_line_to(builder, d[1].point.x, d[1].point.y);
				break;
			case CAIRO_PATH_CURVE_TO:
				gsk_path_builder_cubic_to(builder, d[1].point.x, d[1].point.y, d[2].point.x, d[2].point.y, d[3].point.x, d[3].point.y);
				break;
			case CAIRO_PATH_CLOSE_PATH:
				gsk_path_builder_close(builder);
				break;
		}
	}
	cairo_path_destroy(path);
}

static cairo_t *outline_cairo_new(void)
{
	cairo_surface_t *surface = cairo_recording_surface_create(CAIRO_CONTENT_ALPHA, NULL);
	cairo_t *cr = cairo_create(surface);
	cairo_surface_destroy(surface);
	return cr;
}

/* the glyph outlines of the layout, its top left corner at x, y */
void android_paint_add_layout_outline(GskPathBuilder *builder, PangoLayout *layout, float x, float y)
{
	cairo_t *cr = outline_cairo_new();
	cairo_move_to(cr, x, y);
	pango_cairo_layout_path(cr, layout);
	add_cairo_path(builder, cr);
	cairo_destroy(cr);
}

/* the glyph outlines of the glyph string, its origin on the baseline at x, y */
void android_paint_add_glyphs_outline(GskPathBuilder *builder, PangoFont *font, PangoGlyphString *glyphs, float x, float y)
{
	cairo_t *cr = outline_cairo_new();
	cairo_move_to(cr, x, y);
	pango_cairo_glyph_string_path(cr, font, glyphs);
	add_cairo_path(builder, cr);
	cairo_destroy(cr);
}

/* the text itself, at the origin: filled (with the colors of the layout's attributes) and/or the outlines stroked.
 * Skia draws fill and stroke as one shape, so with a translucent paint the overlap must not be blended twice. */
static void append_text(GtkSnapshot *snapshot, struct AndroidPaint *paint, PangoLayout *layout, PangoFont *font, PangoGlyphString *glyphs)
{
	GskStroke *stroke = text_outline_stroke(paint);
	GdkRGBA color = paint->color;
	bool group = paint->is_fill && stroke && color.alpha < 1.f;
	if (group) {
		gtk_snapshot_push_opacity(snapshot, color.alpha);
		color.alpha = 1.f;
	}
	if (paint->is_fill) {
		if (layout) {
			gtk_snapshot_append_layout(snapshot, layout, &color);
		} else {
			GskRenderNode *node = gsk_text_node_new(font, glyphs, &color, &GRAPHENE_POINT_INIT(0, 0));
			if (node) {
				gtk_snapshot_append_node(snapshot, node);
				gsk_render_node_unref(node);
			}
		}
	}
	if (stroke) {
		GskPathBuilder *builder = gsk_path_builder_new();
		if (layout)
			android_paint_add_layout_outline(builder, layout, 0, 0);
		else
			android_paint_add_glyphs_outline(builder, font, glyphs, 0, 0);
		GskPath *path = gsk_path_builder_free_to_path(builder);
		gtk_snapshot_append_stroke(snapshot, path, stroke, &color);
		gsk_path_unref(path);
		gsk_stroke_free(stroke);
	}
	if (group)
		gtk_snapshot_pop(snapshot);
}

/* Paint.setShadowLayer() below the text. hwui (jni/Paint.cpp, utils/Blur.cpp) makes a blur of
 * sigma = 0.57735 * radius + 0.5 from the radius; GSK's blur radius is twice the sigma. Paint.java: "The alpha of
 * the shadow will be the paint's alpha if the shadow color is opaque, or the alpha from the shadow color if not."
 * GSK colors the shadow with the alpha of what is drawn (the paint's), so that is divided out. */
bool android_paint_push_text_shadow(GtkSnapshot *snapshot, struct AndroidPaint *paint)
{
	if (paint->shadow_radius <= 0)
		return false;
	GskShadow shadow = {
		.color = paint->shadow_color,
		.dx = paint->shadow_dx,
		.dy = paint->shadow_dy,
		.radius = 2 * (0.57735f * paint->shadow_radius + 0.5f),
	};
	if (shadow.color.alpha < 1.f && paint->color.alpha > 0.f)
		shadow.color.alpha = MIN(1.f, shadow.color.alpha / paint->color.alpha);
	gtk_snapshot_push_shadow(snapshot, &shadow, 1);
	return true;
}

/* Paint.setTextSkewX(): Skia skews each glyph about its origin on the baseline (x' = x + skewX * y) */
static void transform_skew(GtkSnapshot *snapshot, struct AndroidPaint *paint)
{
	GskTransform *skew = gsk_transform_skew(NULL, atanf(paint->text_skew_x) * 180.f / G_PI, 0);
	gtk_snapshot_transform(snapshot, skew);
	gsk_transform_unref(skew);
}

/* the layout with the paint's text effects (shadow, skew, Style, fake bold), its top left corner at the origin;
 * the caller has pushed the color filter */
void android_paint_append_layout(GtkSnapshot *snapshot, PangoLayout *layout, struct AndroidPaint *paint)
{
	bool shadow = android_paint_push_text_shadow(snapshot, paint);
	if (paint->text_skew_x == 0.f) {
		append_text(snapshot, paint, layout, NULL, NULL);
	} else {
		/* each line skewed about its own baseline, cut to its band (the first and last band reach to the ink) */
		int n_lines = pango_layout_get_line_count(layout);
		PangoRectangle ink;
		pango_layout_get_pixel_extents(layout, &ink, NULL);
		float pad = fabsf(paint->text_skew_x) * ink.height + 1;
		int line = 0;
		PangoLayoutIter *iter = pango_layout_get_iter(layout);
		do {
			float baseline = (float)pango_layout_iter_get_baseline(iter) / PANGO_SCALE;
			if (n_lines > 1) {
				int y0, y1;
				pango_layout_iter_get_line_yrange(iter, &y0, &y1);
				float top = line == 0 ? MIN(ink.y, (float)y0 / PANGO_SCALE) : (float)y0 / PANGO_SCALE;
				float bottom = line == n_lines - 1 ? MAX(ink.y + ink.height, (float)y1 / PANGO_SCALE) : (float)y1 / PANGO_SCALE;
				gtk_snapshot_push_clip(snapshot, &GRAPHENE_RECT_INIT(ink.x - pad, top, ink.width + 2 * pad, bottom - top));
			}
			gtk_snapshot_save(snapshot);
			gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(0, baseline));
			transform_skew(snapshot, paint);
			gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(0, -baseline));
			append_text(snapshot, paint, layout, NULL, NULL);
			gtk_snapshot_restore(snapshot);
			if (n_lines > 1)
				gtk_snapshot_pop(snapshot);
			line++;
		} while (pango_layout_iter_next_line(iter));
		pango_layout_iter_free(iter);
	}
	if (shadow)
		gtk_snapshot_pop(snapshot);
}

/* glyphs with the paint's text effects but the shadow (one android_paint_push_text_shadow() is meant to cover all
 * glyphs of a draw), their origin on the baseline at origin; the caller has pushed the color filter */
void android_paint_append_glyphs(GtkSnapshot *snapshot, PangoFont *font, PangoGlyphString *glyphs, const graphene_point_t *origin, struct AndroidPaint *paint)
{
	gtk_snapshot_save(snapshot);
	gtk_snapshot_translate(snapshot, origin);
	if (paint->text_skew_x != 0.f)
		transform_skew(snapshot, paint);
	append_text(snapshot, paint, NULL, font, glyphs);
	gtk_snapshot_restore(snapshot);
}

/* ---- text shaping for the measuring methods, TextRunShaper and getTextPath ----
 *
 * AOSP shapes with minikin (Layout over the context, BIDI_FORCE_LTR/RTL for a run); here Pango itemizes and
 * shapes with HarfBuzz. A forced direction overrides the bidi level of every item, as minikin lays out the
 * whole run in that direction. Advances follow Paint.getTextRunAdvances(): the advance of a cluster belongs to
 * its first char, the other chars of the cluster get 0. Letter spacing is added per cluster, half of it before
 * and half after the cluster (minikin's Layout does the same for Paint.setLetterSpacing). */

/* a copy of the UTF-16 text with unpaired surrogates replaced by U+FFFD (same length, so indices stay valid) */
static gunichar2 *utf16_sanitized(const jchar *text, int len)
{
	gunichar2 *buf = g_new(gunichar2, len + 1);
	for (int i = 0; i < len; i++) {
		jchar c = text[i];
		if (c >= 0xD800 && c <= 0xDBFF) {
			if (i + 1 < len && text[i + 1] >= 0xDC00 && text[i + 1] <= 0xDFFF) {
				buf[i] = c;
				buf[i + 1] = text[i + 1];
				i++;
				continue;
			}
			c = 0xFFFD;
		} else if (c >= 0xDC00 && c <= 0xDFFF) {
			c = 0xFFFD;
		}
		buf[i] = c;
	}
	buf[len] = 0;
	return buf;
}

void android_paint_shape_run(struct AndroidPaint *paint, const jchar *text, int len, int start, int end, bool rtl,
                             float *advances, GArray *glyphs_out, float *total)
{
	*total = 0;
	if (advances)
		memset(advances, 0, sizeof(float) * len);
	if (len <= 0 || start >= end)
		return;
	gunichar2 *buf = utf16_sanitized(text, len);
	glong bytes = 0;
	gchar *utf8 = g_utf16_to_utf8(buf, len, NULL, &bytes, NULL);
	g_free(buf);
	if (!utf8)
		return;
	/* the UTF-16 index of every UTF-8 byte offset */
	int *u16_at = g_new0(int, bytes + 1);
	int u16 = 0;
	for (const gchar *p = utf8; *p; p = g_utf8_next_char(p)) {
		const gchar *next = g_utf8_next_char(p);
		for (const gchar *q = p; q < next; q++)
			u16_at[q - utf8] = u16;
		u16 += g_utf8_get_char(p) >= 0x10000 ? 2 : 1;
	}
	u16_at[bytes] = u16;

	float spacing = paint->letter_spacing * pango_font_description_get_size(paint->font) / PANGO_SCALE;
	PangoAttrList *attrs = pango_attr_list_new();
	pango_attr_list_insert(attrs, pango_attr_font_desc_new(paint->font));
	GList *items = pango_itemize_with_base_dir(atl_pango_context(), rtl ? PANGO_DIRECTION_RTL : PANGO_DIRECTION_LTR,
	                                           utf8, 0, bytes, attrs, NULL);
	pango_attr_list_unref(attrs);
	if (rtl) /* forced RTL: the items (in logical order) are laid out from right to left */
		items = g_list_reverse(items);

	float pen = 0;
	PangoGlyphString *glyphs = pango_glyph_string_new();
	for (GList *l = items; l; l = l->next) {
		PangoItem *item = l->data;
		item->analysis.level = rtl ? 1 : 0;
		pango_shape_item(item, utf8, bytes, NULL, glyphs, PANGO_SHAPE_NONE);
		for (int i = 0; i < glyphs->num_glyphs; i++) {
			PangoGlyphInfo *info = &glyphs->glyphs[i];
			int index = u16_at[item->offset + glyphs->log_clusters[i]];
			bool cluster_start = info->attr.is_cluster_start;
			float width = (float)info->geometry.width / PANGO_SCALE;
			if (advances)
				advances[index] += width + (cluster_start ? spacing : 0);
			if (index < start || index >= end)
				continue;
			if (cluster_start)
				pen += spacing / 2;
			if (glyphs_out && info->glyph != PANGO_GLYPH_EMPTY) {
				struct android_shaped_glyph g = {
					.id = (info->glyph & PANGO_GLYPH_UNKNOWN_FLAG) ? 0 : info->glyph,
					.x = pen + (float)info->geometry.x_offset / PANGO_SCALE,
					.y = (float)info->geometry.y_offset / PANGO_SCALE,
					.font = g_object_ref(item->analysis.font),
				};
				g_array_append_val(glyphs_out, g);
			}
			pen += width;
			if (cluster_start)
				pen += spacing / 2;
		}
	}
	pango_glyph_string_free(glyphs);
	g_list_free_full(items, (GDestroyNotify)pango_item_free);
	g_free(u16_at);
	g_free(utf8);
	*total = pen;
}

/* Paint.nGetTextAdvances(): the total advance of text[index, index + count) shaped in its context, the advance
 * of every char into advances[advances_index...] if given */
JNIEXPORT jfloat JNICALL Java_android_graphics_Paint_native_1get_1text_1advances(JNIEnv *env, jclass clazz, jlong paint_ptr, jcharArray text_arr, jint index, jint count, jint context_index, jint context_count, jboolean is_rtl, jfloatArray advances_arr, jint advances_index)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	if (count <= 0 || context_count <= 0)
		return 0;
	jchar *text = g_new(jchar, context_count);
	(*env)->GetCharArrayRegion(env, text_arr, context_index, context_count, text);
	float *advances = g_new(float, context_count);
	float total;
	android_paint_shape_run(paint, text, context_count, index - context_index, index - context_index + count, is_rtl, advances, NULL, &total);
	if (advances_arr)
		(*env)->SetFloatArrayRegion(env, advances_arr, advances_index, count, advances + (index - context_index));
	g_free(advances);
	g_free(text);
	return total;
}

/* Paint.nGetTextPath(): the glyph outlines of the text, its origin (left end of the baseline, moved by the
 * paint's alignment as Canvas.drawText() does) at x, y, appended to the path builder */
JNIEXPORT void JNICALL Java_android_graphics_Paint_native_1get_1text_1path(JNIEnv *env, jclass clazz, jlong paint_ptr, jcharArray text_arr, jint index, jint count, jfloat x, jfloat y, jlong builder_ptr)
{
	struct AndroidPaint *paint = _PTR(paint_ptr);
	GskPathBuilder *builder = _PTR(builder_ptr);
	if (count <= 0)
		return;
	jchar *text = g_new(jchar, count);
	(*env)->GetCharArrayRegion(env, text_arr, index, count, text);
	GArray *glyphs = g_array_new(FALSE, FALSE, sizeof(struct android_shaped_glyph));
	float total;
	android_paint_shape_run(paint, text, count, 0, count, false, NULL, glyphs, &total);
	g_free(text);
	if (paint->alignment == PANGO_ALIGN_CENTER)
		x -= total / 2.f;
	else if (paint->alignment == PANGO_ALIGN_RIGHT)
		x -= total;
	PangoGlyphString *one = pango_glyph_string_new();
	pango_glyph_string_set_size(one, 1);
	for (guint i = 0; i < glyphs->len; i++) {
		struct android_shaped_glyph *g = &g_array_index(glyphs, struct android_shaped_glyph, i);
		one->glyphs[0].glyph = g->id;
		one->glyphs[0].geometry.width = 0;
		one->glyphs[0].geometry.x_offset = 0;
		one->glyphs[0].geometry.y_offset = 0;
		one->glyphs[0].attr.is_cluster_start = 1;
		one->log_clusters[0] = 0;
		android_paint_add_glyphs_outline(builder, g->font, one, x + g->x, y + g->y);
		g_object_unref(g->font);
	}
	pango_glyph_string_free(one);
	g_array_free(glyphs, TRUE);
}
