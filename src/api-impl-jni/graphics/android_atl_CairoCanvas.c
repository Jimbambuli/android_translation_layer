/* CairoCanvas: the canvas of a Bitmap (Canvas(Bitmap), Surface.lockHardwareCanvas() of an ImageReader). It draws with
 * cairo into the bitmap's image surface, in whatever thread the app draws (Android's software canvas works in any
 * thread); GTK is not used, except gsk_render_node_draw() for a render node on the main thread. The Java side is
 * GskCanvas, whose native methods CairoCanvas overrides; "snapshot" there is a struct canvas here. */
#include <cairo.h>
#include <gtk/gtk.h>
#include <math.h>
#include <pango/pangocairo.h>

#include "../defines.h"
#include "../util.h"
#include "AndroidPaint.h"

#include "../generated_headers/android_atl_CairoCanvas.h"

extern GThread *main_thread_id;

/* what GskCanvas pushed and pops (native_pop()): clips end with the cairo_restore() of their save level */
enum push_kind { PUSH_CLIP, PUSH_OPACITY, PUSH_ISOLATION, PUSH_COMPOSITE_MASK, PUSH_COMPOSITE_CHILD };

struct push {
	enum push_kind kind;
	float opacity;
	int op;
	cairo_pattern_t *mask;
};

struct canvas {
	cairo_t *cr;
	GArray *pushes;
	/* the paint of the current draw (CairoCanvas.drawComposited()) */
	cairo_operator_t op;
	bool antialias;
	bool filter;
};

/* android.graphics.BlendMode ordinals */
enum {
	MODE_CLEAR, MODE_SRC, MODE_DST, MODE_SRC_OVER, MODE_DST_OVER, MODE_SRC_IN, MODE_DST_IN, MODE_SRC_OUT, MODE_DST_OUT,
	MODE_SRC_ATOP, MODE_DST_ATOP, MODE_XOR, MODE_PLUS, MODE_MODULATE, MODE_SCREEN, MODE_OVERLAY, MODE_DARKEN, MODE_LIGHTEN,
	MODE_COLOR_DODGE, MODE_COLOR_BURN, MODE_HARD_LIGHT, MODE_SOFT_LIGHT, MODE_DIFFERENCE, MODE_EXCLUSION, MODE_MULTIPLY,
	MODE_HUE, MODE_SATURATION, MODE_COLOR, MODE_LUMINOSITY,
};

/* The cairo operator that draws a blend mode like Skia. Skia blends where the drawing covers and interpolates by the
 * coverage: dst' = lerp(dst, src MODE dst, coverage). cairo computes (src IN coverage) OP dst, which is the same for
 * every mode that is linear in src and keeps dst where src is 0 (all separable and non-separable blend modes, OVER,
 * DST_OVER, SRC_ATOP, DST_OUT, XOR, PLUS). CLEAR, SRC, SRC_IN, DST_IN, SRC_OUT, DST_ATOP and MODULATE are composited
 * (CairoCanvas.compositeOp(), composite() below); they draw OVER into their group. */
static cairo_operator_t operator_of(int mode)
{
	static const cairo_operator_t ops[] = {
		[MODE_DST] = CAIRO_OPERATOR_DEST, [MODE_SRC_OVER] = CAIRO_OPERATOR_OVER, [MODE_DST_OVER] = CAIRO_OPERATOR_DEST_OVER,
		[MODE_DST_OUT] = CAIRO_OPERATOR_DEST_OUT, [MODE_SRC_ATOP] = CAIRO_OPERATOR_ATOP, [MODE_XOR] = CAIRO_OPERATOR_XOR,
		[MODE_PLUS] = CAIRO_OPERATOR_ADD, [MODE_SCREEN] = CAIRO_OPERATOR_SCREEN, [MODE_OVERLAY] = CAIRO_OPERATOR_OVERLAY,
		[MODE_DARKEN] = CAIRO_OPERATOR_DARKEN, [MODE_LIGHTEN] = CAIRO_OPERATOR_LIGHTEN,
		[MODE_COLOR_DODGE] = CAIRO_OPERATOR_COLOR_DODGE, [MODE_COLOR_BURN] = CAIRO_OPERATOR_COLOR_BURN,
		[MODE_HARD_LIGHT] = CAIRO_OPERATOR_HARD_LIGHT, [MODE_SOFT_LIGHT] = CAIRO_OPERATOR_SOFT_LIGHT,
		[MODE_DIFFERENCE] = CAIRO_OPERATOR_DIFFERENCE, [MODE_EXCLUSION] = CAIRO_OPERATOR_EXCLUSION,
		[MODE_MULTIPLY] = CAIRO_OPERATOR_MULTIPLY, [MODE_HUE] = CAIRO_OPERATOR_HSL_HUE,
		[MODE_SATURATION] = CAIRO_OPERATOR_HSL_SATURATION, [MODE_COLOR] = CAIRO_OPERATOR_HSL_COLOR,
		[MODE_LUMINOSITY] = CAIRO_OPERATOR_HSL_LUMINOSITY,
	};
	if (mode < 0 || mode >= (int)G_N_ELEMENTS(ops) || (mode != MODE_CLEAR && ops[mode] == CAIRO_OPERATOR_CLEAR))
		return CAIRO_OPERATOR_OVER;
	return ops[mode];
}

JNIEXPORT jlong JNICALL Java_android_atl_CairoCanvas_native_1create(JNIEnv *env, jclass class, jlong surface_ptr)
{
	struct canvas *c = g_new0(struct canvas, 1);
	c->cr = cairo_create(_PTR(surface_ptr));
	c->pushes = g_array_new(FALSE, FALSE, sizeof(struct push));
	c->op = CAIRO_OPERATOR_OVER;
	c->antialias = true;
	return _INTPTR(c);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1destroy(JNIEnv *env, jclass class, jlong canvas_ptr)
{
	struct canvas *c = _PTR(canvas_ptr);
	for (guint i = 0; i < c->pushes->len; i++)
		if (g_array_index(c->pushes, struct push, i).mask)
			cairo_pattern_destroy(g_array_index(c->pushes, struct push, i).mask);
	g_array_free(c->pushes, TRUE);
	cairo_destroy(c->cr);
	g_free(c);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1setPaint(JNIEnv *env, jclass class, jlong canvas_ptr, jint mode, jboolean antialias, jboolean filter)
{
	struct canvas *c = _PTR(canvas_ptr);
	c->op = operator_of(mode);
	c->antialias = antialias;
	c->filter = filter;
}

static void set_color(cairo_t *cr, const GdkRGBA *color, float alpha)
{
	cairo_set_source_rgba(cr, color->red, color->green, color->blue, alpha);
}

/* ---- the paint's shadow layer and color filter around a draw ---- */

/* a gaussian blur of the alpha of an image surface, as an A8 surface with room for the blur, in the same device
 * space (device offset) */
static cairo_surface_t *blurred_alpha(cairo_surface_t *src, double sigma)
{
	int radius = (int)ceil(3 * sigma);
	int w = cairo_image_surface_get_width(src), h = cairo_image_surface_get_height(src);
	int W = w + 2 * radius, H = h + 2 * radius;
	cairo_surface_t *dst = cairo_image_surface_create(CAIRO_FORMAT_A8, W, H);
	if (cairo_surface_status(dst) != CAIRO_STATUS_SUCCESS)
		return dst;
	float *kernel = g_new(float, 2 * radius + 1);
	float sum = 0;
	for (int i = -radius; i <= radius; i++)
		sum += kernel[i + radius] = sigma > 0 ? expf(-(float)(i * i) / (2 * sigma * sigma)) : 1;
	for (int i = 0; i <= 2 * radius; i++)
		kernel[i] /= sum;
	float *a = g_new0(float, (size_t)W * H), *b = g_new0(float, (size_t)W * H);
	cairo_surface_flush(src);
	const unsigned char *data = cairo_image_surface_get_data(src);
	int stride = cairo_image_surface_get_stride(src);
	cairo_format_t format = cairo_image_surface_get_format(src);
	for (int y = 0; y < h; y++)
		for (int x = 0; x < w; x++)
			a[(size_t)(y + radius) * W + x + radius] = (format == CAIRO_FORMAT_A8 ? data[(size_t)y * stride + x] : ((const uint32_t *)(data + (size_t)y * stride))[x] >> 24) / 255.f;
	for (int y = 0; y < H; y++)
		for (int x = 0; x < W; x++) {
			float v = 0;
			for (int k = -radius; k <= radius; k++)
				if (x + k >= 0 && x + k < W)
					v += kernel[k + radius] * a[(size_t)y * W + x + k];
			b[(size_t)y * W + x] = v;
		}
	unsigned char *out = cairo_image_surface_get_data(dst);
	int out_stride = cairo_image_surface_get_stride(dst);
	for (int y = 0; y < H; y++)
		for (int x = 0; x < W; x++) {
			float v = 0;
			for (int k = -radius; k <= radius; k++)
				if (y + k >= 0 && y + k < H)
					v += kernel[k + radius] * b[(size_t)(y + k) * W + x];
			out[(size_t)y * out_stride + x] = (unsigned char)CLAMP(lrintf(v * 255), 0, 255);
		}
	g_free(a);
	g_free(b);
	g_free(kernel);
	cairo_surface_mark_dirty(dst);
	double ox, oy;
	cairo_surface_get_device_offset(src, &ox, &oy);
	cairo_surface_set_device_offset(dst, ox + radius, oy + radius);
	return dst;
}

/* Paint.setShadowLayer() below the drawing (hwui SkiaCanvas::applyLooper(), BlurDrawLooper): the drawing's alpha,
 * blurred with sigma = 0.57735 * radius + 0.5 (scaled like the canvas), moved by dx, dy, in the shadow color. "The
 * alpha of the shadow will be the paint's alpha if the shadow color is opaque, or the alpha from the shadow color if
 * not"; the drawing's alpha already has the paint's, so that is divided out. */
static void draw_shadow(cairo_t *cr, cairo_pattern_t *content, struct AndroidPaint *paint)
{
	cairo_surface_t *surface;
	if (cairo_pattern_get_surface(content, &surface) != CAIRO_STATUS_SUCCESS || cairo_surface_get_type(surface) != CAIRO_SURFACE_TYPE_IMAGE)
		return;
	cairo_matrix_t ctm;
	cairo_get_matrix(cr, &ctm);
	double scale = sqrt(fabs(ctm.xx * ctm.yy - ctm.xy * ctm.yx));
	double dx = paint->shadow_dx, dy = paint->shadow_dy;
	cairo_user_to_device_distance(cr, &dx, &dy);
	cairo_surface_t *blurred = blurred_alpha(surface, (0.57735 * paint->shadow_radius + 0.5) * scale);
	GdkRGBA color = paint->shadow_color;
	if (color.alpha < 1.f && paint->color.alpha > 0.f)
		color.alpha = MIN(1.f, color.alpha / paint->color.alpha);
	cairo_save(cr);
	cairo_identity_matrix(cr);
	set_color(cr, &color, color.alpha);
	cairo_mask_surface(cr, blurred, dx, dy);
	cairo_restore(cr);
	cairo_surface_destroy(blurred);
}

/* sets the operator and antialiasing of the draw; with a color filter or a shadow the drawing goes into a group,
 * which end_draw() puts onto the canvas */
static bool begin_draw(struct canvas *c, struct AndroidPaint *paint, bool shadow)
{
	cairo_set_operator(c->cr, c->op);
	cairo_set_antialias(c->cr, c->antialias ? CAIRO_ANTIALIAS_DEFAULT : CAIRO_ANTIALIAS_NONE);
	if (!paint->use_color_filter && !(shadow && paint->shadow_radius > 0))
		return false;
	cairo_push_group(c->cr);
	cairo_set_operator(c->cr, CAIRO_OPERATOR_OVER);
	return true;
}

/* The color filter ATL has (Paint.c native_set_color_filter(): a PorterDuffColorFilter that keeps the alpha of
 * the drawing and gives it the filter's color) is the filter's color masked by the drawing. */
static void end_draw(struct canvas *c, struct AndroidPaint *paint, bool group, bool shadow)
{
	if (!group)
		return;
	cairo_t *cr = c->cr;
	cairo_pattern_t *content = cairo_pop_group(cr);
	if (shadow && paint->shadow_radius > 0) {
		cairo_push_group(cr);
		cairo_set_operator(cr, CAIRO_OPERATOR_OVER);
		draw_shadow(cr, content, paint);
		cairo_set_source(cr, content);
		cairo_paint(cr);
		cairo_pattern_destroy(content);
		content = cairo_pop_group(cr);
	}
	if (paint->use_color_filter) {
		cairo_set_source_rgba(cr, graphene_vec4_get_x(&paint->color_offset), graphene_vec4_get_y(&paint->color_offset),
		                      graphene_vec4_get_z(&paint->color_offset), graphene_matrix_get_value(&paint->color_matrix, 3, 3));
		cairo_mask(cr, content);
	} else {
		cairo_set_source(cr, content);
		cairo_paint(cr);
	}
	cairo_pattern_destroy(content);
}

/* ---- shapes ---- */

/* the current path stroked with the paint's stroke; a hairline (width 0) is one device pixel wide, like Skia's */
static void stroke_path(cairo_t *cr, struct AndroidPaint *paint)
{
	gsk_stroke_to_cairo(paint->gsk_stroke, cr);
	if (paint->hairline) {
		cairo_save(cr);
		cairo_identity_matrix(cr);
		cairo_set_line_width(cr, 1);
		cairo_stroke(cr);
		cairo_restore(cr);
	} else {
		cairo_stroke(cr);
	}
}

/* the current path filled and/or stroked in the paint's color; Skia draws fill and stroke as one shape, so both
 * are drawn into a group that is composed once */
static void paint_path(cairo_t *cr, struct AndroidPaint *paint, cairo_fill_rule_t fill_rule)
{
	bool group = paint->is_fill && paint->is_stroke;
	if (group) {
		cairo_push_group(cr);
		cairo_set_operator(cr, CAIRO_OPERATOR_OVER);
	}
	set_color(cr, &paint->color, group ? 1.f : paint->color.alpha);
	if (paint->is_fill) {
		cairo_set_fill_rule(cr, fill_rule);
		if (paint->is_stroke)
			cairo_fill_preserve(cr);
		else
			cairo_fill(cr);
	}
	if (paint->is_stroke)
		stroke_path(cr, paint);
	if (group) {
		cairo_pop_group_to_source(cr);
		cairo_paint_with_alpha(cr, paint->color.alpha);
	}
}

/* extent of Canvas.drawPaint() and of an inverse fill, see GskCanvas */
#define INVERSE_FILL_EXTENT 100000.f

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1drawRect(JNIEnv *env, jobject this, jlong canvas_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom, jlong paint_ptr)
{
	struct canvas *c = _PTR(canvas_ptr);
	struct AndroidPaint *paint = _PTR(paint_ptr);
	bool group = begin_draw(c, paint, true);
	set_color(c->cr, &paint->color, paint->color.alpha);
	/* drawPaint(): the whole clip (a rectangle that large would overflow cairo's fixed point numbers when scaled) */
	if (left <= -INVERSE_FILL_EXTENT && top <= -INVERSE_FILL_EXTENT && right >= INVERSE_FILL_EXTENT && bottom >= INVERSE_FILL_EXTENT) {
		cairo_paint(c->cr);
	} else {
		cairo_rectangle(c->cr, left, top, right - left, bottom - top);
		cairo_fill(c->cr);
	}
	end_draw(c, paint, group, true);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1drawPath(JNIEnv *env, jobject this, jlong canvas_ptr, jlong path_ptr, jint fill_rule, jboolean inverse, jlong paint_ptr)
{
	struct canvas *c = _PTR(canvas_ptr);
	cairo_t *cr = c->cr;
	struct AndroidPaint *paint = _PTR(paint_ptr);
	bool group = begin_draw(c, paint, true);
	if (inverse) {
		/* Skia: an inverse fill type covers everything outside the path; a stroke keeps the inverse bit (SkStroke),
		 * so it covers everything outside the stroke */
		cairo_push_group_with_content(cr, CAIRO_CONTENT_ALPHA);
		cairo_set_operator(cr, CAIRO_OPERATOR_SOURCE);
		cairo_set_source_rgba(cr, 0, 0, 0, 1);
		cairo_paint(cr);
		cairo_set_operator(cr, CAIRO_OPERATOR_DEST_OUT);
		gsk_path_to_cairo(_PTR(path_ptr), cr);
		struct AndroidPaint opaque = *paint;
		opaque.color.alpha = 1.f;
		paint_path(cr, &opaque, fill_rule);
		cairo_pattern_t *mask = cairo_pop_group(cr);
		set_color(cr, &paint->color, paint->color.alpha);
		cairo_mask(cr, mask);
		cairo_pattern_destroy(mask);
	} else {
		gsk_path_to_cairo(_PTR(path_ptr), cr);
		paint_path(cr, paint, fill_rule);
	}
	end_draw(c, paint, group, true);
}

/* a rounded rectangle with the radii of SkRRect::setRectXY(): too large radii are scaled down together */
static void rounded_rect_path(cairo_t *cr, float left, float top, float right, float bottom, float rx, float ry)
{
	float width = right - left, height = bottom - top;
	if (rx <= 0 || ry <= 0) {
		cairo_rectangle(cr, left, top, width, height);
		return;
	}
	float scale = 1;
	if (rx + rx > width)
		scale = width / (rx + rx);
	if (ry + ry > height)
		scale = MIN(scale, height / (ry + ry));
	rx *= scale;
	ry *= scale;
	cairo_new_sub_path(cr);
	const float corners[4][3] = {{right - rx, top + ry, -G_PI / 2}, {right - rx, bottom - ry, 0}, {left + rx, bottom - ry, G_PI / 2}, {left + rx, top + ry, G_PI}};
	for (int i = 0; i < 4; i++) {
		cairo_save(cr);
		cairo_translate(cr, corners[i][0], corners[i][1]);
		cairo_scale(cr, rx, ry);
		cairo_arc(cr, 0, 0, 1, corners[i][2], corners[i][2] + G_PI / 2);
		cairo_restore(cr);
	}
	cairo_close_path(cr);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1drawRoundRect(JNIEnv *env, jobject this, jlong canvas_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom, jfloat rx, jfloat ry, jlong paint_ptr)
{
	struct canvas *c = _PTR(canvas_ptr);
	struct AndroidPaint *paint = _PTR(paint_ptr);
	bool group = begin_draw(c, paint, true);
	rounded_rect_path(c->cr, left, top, right, bottom, rx, ry);
	paint_path(c->cr, paint, CAIRO_FILL_RULE_WINDING);
	end_draw(c, paint, group, true);
}

/* Skia's drawLine()/drawLines(): each segment stroked with the paint's width and cap, whatever its style */
static void stroke_line(cairo_t *cr, struct AndroidPaint *paint, float x0, float y0, float x1, float y1)
{
	if (!isfinite(x0) || !isfinite(y0) || !isfinite(x1) || !isfinite(y1))
		return;
	cairo_move_to(cr, x0, y0);
	cairo_line_to(cr, x1, y1);
	stroke_path(cr, paint);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1drawLine(JNIEnv *env, jobject this, jlong canvas_ptr, jfloat x0, jfloat y0, jfloat x1, jfloat y1, jlong paint_ptr)
{
	struct canvas *c = _PTR(canvas_ptr);
	struct AndroidPaint *paint = _PTR(paint_ptr);
	bool group = begin_draw(c, paint, true);
	set_color(c->cr, &paint->color, paint->color.alpha);
	stroke_line(c->cr, paint, x0, y0, x1, y1);
	end_draw(c, paint, group, true);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1drawLines(JNIEnv *env, jobject this, jlong canvas_ptr, jfloatArray points_arr, jint offset, jint count, jlong paint_ptr)
{
	struct canvas *c = _PTR(canvas_ptr);
	struct AndroidPaint *paint = _PTR(paint_ptr);
	jfloat *points = (*env)->GetFloatArrayElements(env, points_arr, NULL);
	bool group = begin_draw(c, paint, true);
	set_color(c->cr, &paint->color, paint->color.alpha);
	/* count is the number of values in points; four make one line */
	for (int i = offset; i + 4 <= offset + count; i += 4)
		stroke_line(c->cr, paint, points[i + 0], points[i + 1], points[i + 2], points[i + 3]);
	end_draw(c, paint, group, true);
	(*env)->ReleaseFloatArrayElements(env, points_arr, points, JNI_ABORT);
}

/* ---- text ---- */

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1drawText(JNIEnv *env, jobject this, jlong canvas_ptr, jstring text, jfloat x, jfloat y, jlong paint_ptr)
{
	struct canvas *c = _PTR(canvas_ptr);
	struct AndroidPaint *paint = _PTR(paint_ptr);
	PangoLayout *layout = pango_layout_new(android_paint_pango_context(paint, paint->font));
	pango_layout_set_font_description(layout, paint->font);
	android_paint_apply_letter_spacing(layout, paint);
	const char *str = (*env)->GetStringUTFChars(env, text, NULL);
	pango_layout_set_text(layout, str, -1);
	(*env)->ReleaseStringUTFChars(env, text, str);
	android_paint_apply_text_decoration(layout, paint);
	PangoRectangle rect;
	pango_layout_get_pixel_extents(layout, NULL, &rect);
	y -= (float)pango_layout_get_baseline(layout) / PANGO_SCALE;
	if (paint->alignment == PANGO_ALIGN_CENTER)
		x -= rect.width / 2.f;
	else if (paint->alignment == PANGO_ALIGN_RIGHT)
		x -= rect.width;
	bool group = begin_draw(c, paint, true);
	cairo_save(c->cr);
	cairo_translate(c->cr, x, y);
	android_paint_draw_layout_cairo(c->cr, layout, paint);
	cairo_restore(c->cr);
	end_draw(c, paint, group, true);
	g_object_unref(layout);
}

/* Canvas.drawGlyphs(), as GskCanvas */
JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1drawGlyphs(JNIEnv *env, jobject this, jlong canvas_ptr, jintArray ids_arr, jint id_offset, jfloatArray pos_arr, jint pos_offset, jint count, jstring family, jint weight, jint slant, jlong paint_ptr)
{
	struct canvas *c = _PTR(canvas_ptr);
	struct AndroidPaint *paint = _PTR(paint_ptr);
	if (count <= 0)
		return;
	PangoFontDescription *desc = pango_font_description_copy(paint->font);
	if (family) {
		const char *family_str = (*env)->GetStringUTFChars(env, family, NULL);
		pango_font_description_set_family(desc, family_str);
		(*env)->ReleaseStringUTFChars(env, family, family_str);
	}
	pango_font_description_set_weight(desc, CLAMP(weight, 100, 1000));
	pango_font_description_set_style(desc, slant ? PANGO_STYLE_ITALIC : PANGO_STYLE_NORMAL);
	PangoFont *font = pango_context_load_font(android_paint_pango_context(paint, desc), desc);
	pango_font_description_free(desc);
	if (!font)
		return;
	jint *ids = (*env)->GetIntArrayElements(env, ids_arr, NULL);
	jfloat *pos = (*env)->GetFloatArrayElements(env, pos_arr, NULL);
	bool group = begin_draw(c, paint, true);
	PangoGlyphString *glyphs = pango_glyph_string_new();
	pango_glyph_string_set_size(glyphs, 1);
	for (jint i = 0; i < count; i++) {
		PangoRectangle logical;
		pango_font_get_glyph_extents(font, ids[id_offset + i], NULL, &logical);
		glyphs->glyphs[0].glyph = ids[id_offset + i];
		glyphs->glyphs[0].geometry.width = logical.width;
		glyphs->glyphs[0].geometry.x_offset = 0;
		glyphs->glyphs[0].geometry.y_offset = 0;
		glyphs->glyphs[0].attr.is_cluster_start = 1;
		glyphs->log_clusters[0] = 0;
		android_paint_draw_glyphs_cairo(c->cr, font, glyphs, pos[pos_offset + i * 2], pos[pos_offset + i * 2 + 1], paint);
	}
	pango_glyph_string_free(glyphs);
	end_draw(c, paint, group, true);
	(*env)->ReleaseIntArrayElements(env, ids_arr, ids, JNI_ABORT);
	(*env)->ReleaseFloatArrayElements(env, pos_arr, pos, JNI_ABORT);
	g_object_unref(font);
}

/* ---- bitmaps and shaders ---- */

/* the image to draw from; the canvas's own surface is copied first (cairo cannot read and write it at once) */
static cairo_surface_t *source_surface(cairo_t *cr, cairo_surface_t *image)
{
	if (image != cairo_get_target(cr))
		return cairo_surface_reference(image);
	cairo_surface_t *copy = cairo_image_surface_create(cairo_image_surface_get_format(image), cairo_image_surface_get_width(image), cairo_image_surface_get_height(image));
	cairo_t *copy_cr = cairo_create(copy);
	cairo_set_operator(copy_cr, CAIRO_OPERATOR_SOURCE);
	cairo_set_source_surface(copy_cr, image, 0, 0);
	cairo_paint(copy_cr);
	cairo_destroy(copy_cr);
	return copy;
}

/* paints the pattern (in the clip) with the paint's alpha; an ALPHA_8 bitmap is a mask for the paint's color, as
 * Skia draws an alpha-only image */
static void paint_image(cairo_t *cr, cairo_pattern_t *pattern, cairo_surface_t *image, struct AndroidPaint *paint)
{
	if (cairo_image_surface_get_format(image) == CAIRO_FORMAT_A8) {
		set_color(cr, &paint->color, paint->color.alpha);
		cairo_mask(cr, pattern);
	} else {
		cairo_set_source(cr, pattern);
		cairo_paint_with_alpha(cr, paint->color.alpha);
	}
}

/* drawBitmap(): the source rectangle of the image scaled into the destination rectangle, sampled like hwui
 * (Paint.sampling(): bilinear with FILTER_BITMAP_FLAG, else nearest; SkCanvas::kFast_SrcRectConstraint, so the
 * filter may read pixels around the source rectangle) */
static void draw_image(struct canvas *c, cairo_surface_t *image, int x, int y, int width, int height, int src_x, int src_y, int src_width, int src_height, struct AndroidPaint *paint)
{
	cairo_t *cr = c->cr;
	if (src_width <= 0 || src_height <= 0 || width <= 0 || height <= 0)
		return;
	bool group = begin_draw(c, paint, false);
	cairo_surface_t *source = source_surface(cr, image);
	cairo_save(cr);
	cairo_rectangle(cr, x, y, width, height);
	cairo_clip(cr);
	cairo_translate(cr, x, y);
	cairo_scale(cr, width / (double)src_width, height / (double)src_height);
	cairo_pattern_t *pattern = cairo_pattern_create_for_surface(source);
	cairo_matrix_t matrix;
	cairo_matrix_init_translate(&matrix, src_x, src_y);
	cairo_pattern_set_matrix(pattern, &matrix);
	cairo_pattern_set_filter(pattern, c->filter ? CAIRO_FILTER_BILINEAR : CAIRO_FILTER_NEAREST);
	cairo_pattern_set_extend(pattern, CAIRO_EXTEND_PAD);
	paint_image(cr, pattern, source, paint);
	cairo_pattern_destroy(pattern);
	cairo_restore(cr);
	cairo_surface_destroy(source);
	end_draw(c, paint, group, false);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1drawBitmap__JJIIIIJ(JNIEnv *env, jobject this, jlong canvas_ptr, jlong surface_ptr, jint x, jint y, jint width, jint height, jlong paint_ptr)
{
	cairo_surface_t *image = _PTR(surface_ptr);
	draw_image(_PTR(canvas_ptr), image, x, y, width, height, 0, 0, cairo_image_surface_get_width(image), cairo_image_surface_get_height(image), _PTR(paint_ptr));
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1drawBitmap__JJIIIIIIIIJ(JNIEnv *env, jobject this, jlong canvas_ptr, jlong surface_ptr, jint x, jint y, jint width, jint height, jint src_x, jint src_y, jint src_width, jint src_height, jlong paint_ptr)
{
	draw_image(_PTR(canvas_ptr), _PTR(surface_ptr), x, y, width, height, src_x, src_y, src_width, src_height, _PTR(paint_ptr));
}

/* AOSP Shader.TileMode.nativeInt */
enum { TILE_CLAMP, TILE_REPEAT, TILE_MIRROR, TILE_DECAL };

static cairo_extend_t extend_of(int tile)
{
	switch (tile) {
		case TILE_REPEAT:
			return CAIRO_EXTEND_REPEAT;
		case TILE_MIRROR:
			return CAIRO_EXTEND_REFLECT;
		case TILE_DECAL:
			return CAIRO_EXTEND_NONE;
		default:
			return CAIRO_EXTEND_PAD;
	}
}

/* the source coordinate of v along an axis of the given size: REPEAT wraps, MIRROR wraps with every second copy
 * mirrored */
static int32_t tile_source(int32_t mode, int32_t size, int64_t v)
{
	if (mode == TILE_REPEAT)
		return ((v % size) + size) % size;
	int64_t m = ((v % (2 * size)) + 2 * size) % (2 * size);
	return m < size ? m : 2 * size - 1 - m;
}

/* The pattern of a BitmapShader over the area (in the shader's coordinates). cairo extends a pattern alike along
 * both axes; with different tile modes the repeating axes (REPEAT, MIRROR) are tiled into an image that covers the
 * area (and a pixel more for the filter), whose other axis is extended by its own mode. NULL if that is too large. */
static cairo_pattern_t *shader_pattern(cairo_surface_t *image, int tile_x, int tile_y, float left, float top, float right, float bottom)
{
	if (tile_x == tile_y) {
		cairo_pattern_t *pattern = cairo_pattern_create_for_surface(image);
		cairo_pattern_set_extend(pattern, extend_of(tile_x));
		return pattern;
	}
	int32_t width = cairo_image_surface_get_width(image), height = cairo_image_surface_get_height(image);
	bool repeat_x = tile_x == TILE_REPEAT || tile_x == TILE_MIRROR;
	bool repeat_y = tile_y == TILE_REPEAT || tile_y == TILE_MIRROR;
	int64_t x0 = repeat_x ? (int64_t)floorf(left) - 1 : 0, y0 = repeat_y ? (int64_t)floorf(top) - 1 : 0;
	int64_t x1 = repeat_x ? (int64_t)ceilf(right) + 1 : width, y1 = repeat_y ? (int64_t)ceilf(bottom) + 1 : height;
	cairo_format_t format = cairo_image_surface_get_format(image);
	int bpp = format == CAIRO_FORMAT_A8 ? 1 : 4;
	if (x1 - x0 > 32767 || y1 - y0 > 32767 || (x1 - x0) * (y1 - y0) * bpp > 64 * 1024 * 1024)
		return NULL;
	cairo_surface_t *tiled = cairo_image_surface_create(format, x1 - x0, y1 - y0);
	cairo_surface_flush(image);
	const unsigned char *src = cairo_image_surface_get_data(image);
	unsigned char *dst = cairo_image_surface_get_data(tiled);
	int src_stride = cairo_image_surface_get_stride(image), dst_stride = cairo_image_surface_get_stride(tiled);
	for (int64_t y = 0; y < y1 - y0; y++) {
		const unsigned char *src_row = src + (size_t)(repeat_y ? tile_source(tile_y, height, y0 + y) : y) * src_stride;
		for (int64_t x = 0; x < x1 - x0; x++)
			memcpy(dst + y * dst_stride + x * bpp, src_row + (size_t)(repeat_x ? tile_source(tile_x, width, x0 + x) : x) * bpp, bpp);
	}
	cairo_surface_mark_dirty(tiled);
	cairo_pattern_t *pattern = cairo_pattern_create_for_surface(tiled);
	cairo_surface_destroy(tiled);
	cairo_matrix_t matrix;
	cairo_matrix_init_translate(&matrix, -x0, -y0);
	cairo_pattern_set_matrix(pattern, &matrix);
	/* the extend only matters for the axis that does not repeat */
	cairo_pattern_set_extend(pattern, extend_of(repeat_x ? (repeat_y ? TILE_DECAL : tile_y) : tile_x));
	return pattern;
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1fillBitmapShader(JNIEnv *env, jobject this, jlong canvas_ptr, jlong surface_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom, jint tile_x, jint tile_y, jlong paint_ptr)
{
	struct canvas *c = _PTR(canvas_ptr);
	cairo_t *cr = c->cr;
	struct AndroidPaint *paint = _PTR(paint_ptr);
	cairo_surface_t *image = source_surface(cr, _PTR(surface_ptr));
	cairo_pattern_t *pattern = shader_pattern(image, tile_x, tile_y, left, top, right, bottom);
	if (!pattern) {
		pattern = cairo_pattern_create_for_surface(image);
		cairo_pattern_set_extend(pattern, extend_of(tile_x));
	}
	cairo_pattern_set_filter(pattern, c->filter ? CAIRO_FILTER_BILINEAR : CAIRO_FILTER_NEAREST);
	bool group = begin_draw(c, paint, false);
	cairo_save(cr);
	cairo_rectangle(cr, left, top, right - left, bottom - top);
	cairo_clip(cr);
	paint_image(cr, pattern, image, paint);
	cairo_restore(cr);
	end_draw(c, paint, group, false);
	cairo_pattern_destroy(pattern);
	cairo_surface_destroy(image);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1drawLinearGradient(JNIEnv *env, jobject this, jlong canvas_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom, jfloat x0, jfloat y0, jfloat x1, jfloat y1, jintArray colors_arr, jfloatArray positions_arr, jint tile_mode, jlong paint_ptr)
{
	struct canvas *c = _PTR(canvas_ptr);
	cairo_t *cr = c->cr;
	struct AndroidPaint *paint = _PTR(paint_ptr);
	jsize n_stops = (*env)->GetArrayLength(env, colors_arr);
	jint *colors = (*env)->GetIntArrayElements(env, colors_arr, NULL);
	jfloat *positions = positions_arr ? (*env)->GetFloatArrayElements(env, positions_arr, NULL) : NULL;
	cairo_pattern_t *pattern;
	if (x0 == x1 && y0 == y1) { /* degenerate gradient: the last color, like Skia does for CLAMP */
		uint32_t last = colors[n_stops - 1];
		pattern = cairo_pattern_create_rgba(((last >> 16) & 0xff) / 255., ((last >> 8) & 0xff) / 255., (last & 0xff) / 255., (last >> 24) / 255.);
	} else {
		pattern = cairo_pattern_create_linear(x0, y0, x1, y1);
		float previous = 0;
		for (int i = 0; i < n_stops; i++) {
			float offset = positions ? positions[i] : i / (float)(n_stops - 1);
			offset = CLAMP(offset, previous, 1.f); /* increasing offsets in [0, 1] */
			previous = offset;
			uint32_t color = colors[i];
			cairo_pattern_add_color_stop_rgba(pattern, offset, ((color >> 16) & 0xff) / 255., ((color >> 8) & 0xff) / 255., (color & 0xff) / 255., (color >> 24) / 255.);
		}
		cairo_pattern_set_extend(pattern, extend_of(tile_mode));
	}
	(*env)->ReleaseIntArrayElements(env, colors_arr, colors, JNI_ABORT);
	if (positions)
		(*env)->ReleaseFloatArrayElements(env, positions_arr, positions, JNI_ABORT);
	bool group = begin_draw(c, paint, false);
	cairo_save(cr);
	cairo_rectangle(cr, left, top, right - left, bottom - top);
	cairo_clip(cr);
	/* the shader output is modulated by the paint's alpha */
	cairo_set_source(cr, pattern);
	cairo_paint_with_alpha(cr, paint->color.alpha);
	cairo_restore(cr);
	end_draw(c, paint, group, false);
	cairo_pattern_destroy(pattern);
}

/* ---- matrix, clip, save/restore, layers ---- */

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1translate(JNIEnv *env, jobject this, jlong canvas_ptr, jfloat dx, jfloat dy)
{
	cairo_translate(((struct canvas *)_PTR(canvas_ptr))->cr, dx, dy);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1rotate(JNIEnv *env, jobject this, jlong canvas_ptr, jfloat degrees)
{
	if (isfinite(degrees))
		cairo_rotate(((struct canvas *)_PTR(canvas_ptr))->cr, degrees * G_PI / 180);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1scale(JNIEnv *env, jobject this, jlong canvas_ptr, jfloat sx, jfloat sy)
{
	/* a non-invertible matrix puts cairo into an error state for good; Skia then draws nothing, ATL draws on */
	if (sx != 0 && sy != 0 && isfinite(sx) && isfinite(sy))
		cairo_scale(((struct canvas *)_PTR(canvas_ptr))->cr, sx, sy);
}

/* the 2D part of the android Matrix (a graphene matrix: rows x, y, w); cairo has no perspective */
JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1concat(JNIEnv *env, jobject this, jlong canvas_ptr, jlong matrix_ptr)
{
	const graphene_matrix_t *m = _PTR(matrix_ptr);
	cairo_matrix_t matrix;
	cairo_matrix_init(&matrix, graphene_matrix_get_value(m, 0, 0), graphene_matrix_get_value(m, 0, 1), graphene_matrix_get_value(m, 1, 0),
	                  graphene_matrix_get_value(m, 1, 1), graphene_matrix_get_value(m, 3, 0), graphene_matrix_get_value(m, 3, 1));
	cairo_matrix_t inverse = matrix;
	if (cairo_matrix_invert(&inverse) == CAIRO_STATUS_SUCCESS) /* see native_scale() */
		cairo_transform(((struct canvas *)_PTR(canvas_ptr))->cr, &matrix);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1save(JNIEnv *env, jobject this, jlong canvas_ptr)
{
	cairo_save(((struct canvas *)_PTR(canvas_ptr))->cr);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1restore(JNIEnv *env, jobject this, jlong canvas_ptr)
{
	cairo_restore(((struct canvas *)_PTR(canvas_ptr))->cr);
}

static void push(struct canvas *c, enum push_kind kind, float opacity, int op)
{
	struct push p = {kind, opacity, op, NULL};
	g_array_append_val(c->pushes, p);
}

/* hwui clips antialiased (SkiaCanvas::clipRect()/clipPath() with doAntiAlias true) */
JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1clipRect(JNIEnv *env, jobject this, jlong canvas_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom)
{
	struct canvas *c = _PTR(canvas_ptr);
	cairo_set_antialias(c->cr, CAIRO_ANTIALIAS_DEFAULT);
	cairo_rectangle(c->cr, left, top, right - left, bottom - top);
	cairo_clip(c->cr);
	push(c, PUSH_CLIP, 1, 0);
}

/* An inverse fill type clips to everything outside the path: a large rectangle around the path with the even-odd
 * rule. For a path that covers parts several times with the winding rule (overlapping contours) that clips out
 * less than Skia does. */
JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1clipPath(JNIEnv *env, jobject this, jlong canvas_ptr, jlong path_ptr, jint fill_rule, jboolean inverse)
{
	struct canvas *c = _PTR(canvas_ptr);
	cairo_t *cr = c->cr;
	cairo_set_antialias(cr, CAIRO_ANTIALIAS_DEFAULT);
	cairo_new_path(cr);
	if (inverse) {
		double x0, y0, x1, y1;
		cairo_clip_extents(cr, &x0, &y0, &x1, &y1);
		cairo_rectangle(cr, x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2);
		cairo_set_fill_rule(cr, CAIRO_FILL_RULE_EVEN_ODD);
	} else {
		cairo_set_fill_rule(cr, fill_rule);
	}
	gsk_path_to_cairo(_PTR(path_ptr), cr);
	cairo_clip(cr);
	push(c, PUSH_CLIP, 1, 0);
}

/* saveLayer() with alpha: a group, composed with that alpha by native_pop() */
JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1pushOpacity(JNIEnv *env, jobject this, jlong canvas_ptr, jfloat opacity)
{
	struct canvas *c = _PTR(canvas_ptr);
	cairo_push_group(c->cr);
	push(c, PUSH_OPACITY, opacity, 0);
}

/* saveLayer(): a transparent offscreen like AOSP's */
JNIEXPORT jboolean JNICALL Java_android_atl_CairoCanvas_native_1pushIsolation(JNIEnv *env, jobject this, jlong canvas_ptr)
{
	struct canvas *c = _PTR(canvas_ptr);
	cairo_push_group(c->cr);
	push(c, PUSH_ISOLATION, 1, 0);
	return JNI_TRUE;
}

/* GskCanvas.drawComposited(): the coverage of the drawing goes into one group, the drawing into the next; the
 * second pop composes them (composite()) */
JNIEXPORT jboolean JNICALL Java_android_atl_CairoCanvas_native_1pushComposite(JNIEnv *env, jobject this, jlong canvas_ptr, jint op)
{
	struct canvas *c = _PTR(canvas_ptr);
	cairo_push_group(c->cr);
	push(c, PUSH_COMPOSITE_MASK, 1, op);
	return JNI_TRUE;
}

struct pixel {
	float r, g, b, a;
};

static struct pixel read_pixel(const unsigned char *row, int x, cairo_format_t format)
{
	if (format == CAIRO_FORMAT_A8)
		return (struct pixel){0, 0, 0, row[x] / 255.f};
	uint32_t p = ((const uint32_t *)row)[x];
	return (struct pixel){((p >> 16) & 0xff) / 255.f, ((p >> 8) & 0xff) / 255.f, (p & 0xff) / 255.f, format == CAIRO_FORMAT_RGB24 ? 1.f : (p >> 24) / 255.f};
}

static inline uint32_t to_byte(float v)
{
	return (uint32_t)lrintf(CLAMP(v, 0.f, 1.f) * 255);
}

static void write_pixel(unsigned char *row, int x, cairo_format_t format, struct pixel p)
{
	if (format == CAIRO_FORMAT_A8) {
		row[x] = to_byte(p.a);
		return;
	}
	uint32_t a = format == CAIRO_FORMAT_RGB24 ? 255 : to_byte(p.a);
	((uint32_t *)row)[x] = a << 24 | MIN(to_byte(p.r), a) << 16 | MIN(to_byte(p.g), a) << 8 | MIN(to_byte(p.b), a);
}

/* Skia's dst' = lerp(dst, src MODE dst, coverage) for the modes cairo has no bounded operator for. The drawing in
 * the group is c = coverage * src; these modes are linear in src (MODULATE too), so with m = coverage:
 * dst' = (1 - m) * dst + c MODE' dst, where the dst term of MODE scales with m (DST_IN: dst * src_a becomes
 * dst * c_a). The groups are drawn under the canvas's clip, so outside it m and c are 0 and dst stays. */
static void composite(cairo_t *cr, int mode, cairo_pattern_t *mask_pattern, cairo_pattern_t *child_pattern)
{
	cairo_surface_t *mask, *child, *target = cairo_get_group_target(cr);
	if (cairo_pattern_get_surface(mask_pattern, &mask) != CAIRO_STATUS_SUCCESS || cairo_pattern_get_surface(child_pattern, &child) != CAIRO_STATUS_SUCCESS ||
	    cairo_surface_get_type(mask) != CAIRO_SURFACE_TYPE_IMAGE || cairo_surface_get_type(child) != CAIRO_SURFACE_TYPE_IMAGE || cairo_surface_get_type(target) != CAIRO_SURFACE_TYPE_IMAGE)
		return;
	cairo_surface_flush(mask);
	cairo_surface_flush(child);
	cairo_surface_flush(target);
	double cox, coy, mox, moy, tox, toy;
	cairo_surface_get_device_offset(child, &cox, &coy);
	cairo_surface_get_device_offset(mask, &mox, &moy);
	cairo_surface_get_device_offset(target, &tox, &toy);
	int cw = cairo_image_surface_get_width(child), ch = cairo_image_surface_get_height(child);
	int mw = cairo_image_surface_get_width(mask), mh = cairo_image_surface_get_height(mask);
	int tw = cairo_image_surface_get_width(target), th = cairo_image_surface_get_height(target);
	cairo_format_t cf = cairo_image_surface_get_format(child), mf = cairo_image_surface_get_format(mask), tf = cairo_image_surface_get_format(target);
	for (int cy = 0; cy < ch; cy++) {
		int ty = cy - (int)coy + (int)toy, my = cy - (int)coy + (int)moy;
		if (ty < 0 || ty >= th)
			continue;
		const unsigned char *crow = cairo_image_surface_get_data(child) + (size_t)cy * cairo_image_surface_get_stride(child);
		const unsigned char *mrow = my >= 0 && my < mh ? cairo_image_surface_get_data(mask) + (size_t)my * cairo_image_surface_get_stride(mask) : NULL;
		unsigned char *trow = cairo_image_surface_get_data(target) + (size_t)ty * cairo_image_surface_get_stride(target);
		for (int cx = 0; cx < cw; cx++) {
			int tx = cx - (int)cox + (int)tox, mx = cx - (int)cox + (int)mox;
			if (tx < 0 || tx >= tw)
				continue;
			float m = mrow && mx >= 0 && mx < mw ? read_pixel(mrow, mx, mf).a : 0;
			struct pixel s = read_pixel(crow, cx, cf);
			if (m == 0 && s.a == 0)
				continue;
			struct pixel d = read_pixel(trow, tx, tf);
			float k = 1 - m;
			struct pixel r = {k * d.r, k * d.g, k * d.b, k * d.a};
			float fa = 0, fb = 0; /* r += s * fa + d * fb */
			switch (mode) {
				case MODE_SRC: fa = 1; break;
				case MODE_SRC_IN: fa = d.a; break;
				case MODE_DST_IN: fb = s.a; break;
				case MODE_SRC_OUT: fa = 1 - d.a; break;
				case MODE_DST_ATOP: fa = 1 - d.a; fb = s.a; break;
				case MODE_MODULATE:
					r.r += s.r * d.r, r.g += s.g * d.g, r.b += s.b * d.b, r.a += s.a * d.a;
					break;
				default: /* MODE_CLEAR */ break;
			}
			r.r += s.r * fa + d.r * fb, r.g += s.g * fa + d.g * fb, r.b += s.b * fa + d.b * fb, r.a += s.a * fa + d.a * fb;
			write_pixel(trow, tx, tf, r);
		}
	}
	cairo_surface_mark_dirty(target);
}

JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1pop(JNIEnv *env, jobject this, jlong canvas_ptr, jint count)
{
	struct canvas *c = _PTR(canvas_ptr);
	cairo_t *cr = c->cr;
	while (count-- > 0 && c->pushes->len > 0) {
		struct push *p = &g_array_index(c->pushes, struct push, c->pushes->len - 1);
		switch (p->kind) {
			case PUSH_CLIP:
				break;
			case PUSH_OPACITY:
			case PUSH_ISOLATION:
				cairo_pop_group_to_source(cr);
				cairo_set_operator(cr, CAIRO_OPERATOR_OVER);
				cairo_paint_with_alpha(cr, p->opacity);
				break;
			case PUSH_COMPOSITE_MASK:
				/* the drawing follows in a group of its own; this push stays as its record */
				p->mask = cairo_pop_group(cr);
				p->kind = PUSH_COMPOSITE_CHILD;
				cairo_push_group(cr);
				continue;
			case PUSH_COMPOSITE_CHILD: {
				cairo_pattern_t *child = cairo_pop_group(cr);
				composite(cr, p->op, p->mask, child);
				cairo_pattern_destroy(child);
				cairo_pattern_destroy(p->mask);
				break;
			}
		}
		g_array_set_size(c->pushes, c->pushes->len - 1);
	}
}

/* a view's render node (Surface.lockHardwareCanvas() of an ImageReader). GSK draws it with cairo; a node can hold
 * GL textures, which GDK downloads on the main thread and waits for (gdk_gl_texture_run()), so off the main thread
 * that could deadlock: there the node is not drawn. */
JNIEXPORT void JNICALL Java_android_atl_CairoCanvas_native_1drawRenderNode(JNIEnv *env, jobject this, jlong canvas_ptr, jlong node_ptr)
{
	struct canvas *c = _PTR(canvas_ptr);
	if (!node_ptr)
		return;
	if (main_thread_id && g_thread_self() != main_thread_id) {
		static gint warned;
		if (g_atomic_int_compare_and_exchange(&warned, 0, 1))
			fprintf(stderr, "CairoCanvas: render nodes are only drawn into a bitmap on the main thread\n");
		return;
	}
	cairo_save(c->cr);
	cairo_set_operator(c->cr, CAIRO_OPERATOR_OVER);
	gsk_render_node_draw(_PTR(node_ptr), c->cr);
	cairo_restore(c->cr);
}
