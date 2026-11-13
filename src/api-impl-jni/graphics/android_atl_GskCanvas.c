#include <graphene.h>
#include <gtk/gtk.h>
#include <pango/pango.h>

#include "../defines.h"
#include "../util.h"
#include "AndroidPaint.h"

#include "../generated_headers/android_atl_GskCanvas.h"

/* like Skia, the paint's alpha modulates the bitmap before the color filter (RippleDrawable draws its
 * ALPHA_8 mask with an opaque SRC_IN filter and puts the ripple's alpha into the paint) */
static void push_paint_alpha(GdkSnapshot *snapshot, struct AndroidPaint *paint)
{
	if (paint->color.alpha < 1.f)
		gtk_snapshot_push_opacity(snapshot, paint->color.alpha);
}

static void pop_paint_alpha(GdkSnapshot *snapshot, struct AndroidPaint *paint)
{
	if (paint->color.alpha < 1.f)
		gtk_snapshot_pop(snapshot);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawBitmap__JJIIIIJ(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jlong texture_ptr, jint x, jint y, jint width, jint height, jlong paint_ptr)
{
	GdkSnapshot *snapshot = (GdkSnapshot *)_PTR(snapshot_ptr);
	GdkTexture *texture = GDK_TEXTURE(_PTR(texture_ptr));
	struct AndroidPaint *paint = _PTR(paint_ptr);
	if (paint->use_color_filter) {
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	}
	push_paint_alpha(snapshot, paint);
	gtk_snapshot_append_texture(snapshot, texture, &GRAPHENE_RECT_INIT(x, y, width, height));
	pop_paint_alpha(snapshot, paint);
	if (paint->use_color_filter)
		gtk_snapshot_pop(snapshot);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawBitmap__JJIIIIIIIIJ(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jlong texture_ptr, jint x, jint y, jint width, jint height, jint src_x, jint src_y, jint src_width, jint src_height, jlong paint_ptr)
{
	GdkSnapshot *snapshot = (GdkSnapshot *)_PTR(snapshot_ptr);
	GdkTexture *texture = GDK_TEXTURE(_PTR(texture_ptr));
	struct AndroidPaint *paint = _PTR(paint_ptr);
	if (paint->use_color_filter) {
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	}
	gtk_snapshot_push_clip(snapshot, &GRAPHENE_RECT_INIT(x, y, width, height));
	float xscale = width / (float)src_width;
	float yscale = height / (float)src_height;
	float scaled_width = gdk_texture_get_width(texture) * xscale;
	float scaled_height = gdk_texture_get_height(texture) * yscale;
	push_paint_alpha(snapshot, paint);
	gtk_snapshot_append_texture(snapshot, texture, &GRAPHENE_RECT_INIT(x - src_x * xscale, y - src_y * yscale, scaled_width, scaled_height));
	pop_paint_alpha(snapshot, paint);
	gtk_snapshot_pop(snapshot);
	if (paint->use_color_filter)
		gtk_snapshot_pop(snapshot);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawRect(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom, jlong paint_ptr)
{
	GdkSnapshot *snapshot = (GdkSnapshot *)_PTR(snapshot_ptr);
	struct AndroidPaint *paint = _PTR(paint_ptr);
	graphene_rect_t bounds = GRAPHENE_RECT_INIT(left, top, right - left, bottom - top);
	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	gtk_snapshot_append_color(snapshot, &paint->color, &bounds);
	if (paint->use_color_filter)
		gtk_snapshot_pop(snapshot);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawLinearGradient(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom, jfloat x0, jfloat y0, jfloat x1, jfloat y1, jintArray colors_arr, jfloatArray positions_arr, jboolean repeat, jlong paint_ptr)
{
	GdkSnapshot *snapshot = (GdkSnapshot *)_PTR(snapshot_ptr);
	struct AndroidPaint *paint = _PTR(paint_ptr);
	graphene_rect_t bounds = GRAPHENE_RECT_INIT(left, top, right - left, bottom - top);
	jsize n_stops = (*env)->GetArrayLength(env, colors_arr);
	jint *colors = (*env)->GetIntArrayElements(env, colors_arr, NULL);
	jfloat *positions = positions_arr ? (*env)->GetFloatArrayElements(env, positions_arr, NULL) : NULL;
	GskColorStop *stops = g_new(GskColorStop, n_stops);
	for (int i = 0; i < n_stops; i++) {
		float offset = positions ? positions[i] : i / (float)(n_stops - 1);
		/* GSK requires increasing offsets in [0, 1] */
		offset = CLAMP(offset, i > 0 ? stops[i - 1].offset : 0.f, 1.f);
		stops[i].offset = offset;
		stops[i].color = (GdkRGBA){
			.red = ((colors[i] >> 16) & 0xFF) / 255.f,
			.green = ((colors[i] >> 8) & 0xFF) / 255.f,
			.blue = (colors[i] & 0xFF) / 255.f,
			.alpha = ((colors[i] >> 24) & 0xFF) / 255.f,
		};
	}
	(*env)->ReleaseIntArrayElements(env, colors_arr, colors, JNI_ABORT);
	if (positions)
		(*env)->ReleaseFloatArrayElements(env, positions_arr, positions, JNI_ABORT);

	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	/* the shader output is modulated by the paint's alpha */
	gtk_snapshot_push_opacity(snapshot, paint->color.alpha);
	if (x0 == x1 && y0 == y1) /* degenerate gradient: use the last color, like Skia does for CLAMP */
		gtk_snapshot_append_color(snapshot, &stops[n_stops - 1].color, &bounds);
	else if (repeat)
		gtk_snapshot_append_repeating_linear_gradient(snapshot, &bounds, &GRAPHENE_POINT_INIT(x0, y0), &GRAPHENE_POINT_INIT(x1, y1), stops, n_stops);
	else
		gtk_snapshot_append_linear_gradient(snapshot, &bounds, &GRAPHENE_POINT_INIT(x0, y0), &GRAPHENE_POINT_INIT(x1, y1), stops, n_stops);
	gtk_snapshot_pop(snapshot);
	if (paint->use_color_filter)
		gtk_snapshot_pop(snapshot);
	g_free(stops);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawPath(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jlong path_ptr, jint fill_rule, jlong paint_ptr)
{
	GtkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	GskPath *path = _PTR(path_ptr);
	struct AndroidPaint *paint = _PTR(paint_ptr);
	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	if (paint->is_stroke) {
		gtk_snapshot_append_stroke(snapshot, path, paint->gsk_stroke, &paint->color);
	}
	if (paint->is_fill) {
		gtk_snapshot_append_fill(snapshot, path, fill_rule, &paint->color);
	}
	if (paint->use_color_filter)
		gtk_snapshot_pop(snapshot);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1translate(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloat dx, jfloat dy)
{
	GtkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(dx, dy));
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1rotate(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloat angle)
{
	if (!isfinite(angle)) // GSK asserts on it
		return;
	GtkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	gtk_snapshot_rotate(snapshot, angle);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1save(JNIEnv *env, jclass this_class, jlong snapshot_ptr)
{
	GtkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	gtk_snapshot_save(snapshot);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1restore(JNIEnv *env, jclass this_class, jlong snapshot_ptr)
{
	GtkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	gtk_snapshot_restore(snapshot);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawLine(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloat x0, jfloat y0, jfloat x1, jfloat y1, jlong paint_ptr)
{
	if (!isfinite(x0) || !isfinite(y0) || !isfinite(x1) || !isfinite(y1)) {
		return;
	}
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	struct AndroidPaint *paint = _PTR(paint_ptr);
	gtk_snapshot_save(snapshot);
	gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(x0, y0));
	float rotation = atan2(y1 - y0, x1 - x0);
	gtk_snapshot_rotate(snapshot, rotation * 180 / M_PI);
	float length = sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
	float stroke_width = gsk_stroke_get_line_width(paint->gsk_stroke);
	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	gtk_snapshot_append_color(snapshot, &paint->color, &GRAPHENE_RECT_INIT(0, -stroke_width / 2, length, stroke_width));
	if (paint->use_color_filter)
		gtk_snapshot_pop(snapshot);
	gtk_snapshot_restore(snapshot);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawLines(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloatArray points_arr, jint offset, jint count, jlong paint_ptr)
{
	jfloat *points = (*env)->GetFloatArrayElements(env, points_arr, NULL);
	for (int i = offset; i < count; i++)
		Java_android_atl_GskCanvas_native_1drawLine(env, this_class, snapshot_ptr, points[i + 0], points[i + 1], points[i + 2], points[i + 3], paint_ptr);
	(*env)->ReleaseFloatArrayElements(env, points_arr, points, 0);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawText(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jstring text, jfloat x, jfloat y, jlong paint_ptr)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	struct AndroidPaint *paint = _PTR(paint_ptr);
	PangoLayout *layout = pango_layout_new(atl_pango_context());
	pango_layout_set_font_description(layout, paint->font);
	android_paint_apply_letter_spacing(layout, paint);
	const char *str = (*env)->GetStringUTFChars(env, text, NULL);
	pango_layout_set_text(layout, str, -1);
	(*env)->ReleaseStringUTFChars(env, text, str);
	PangoRectangle rect;
	pango_layout_get_pixel_extents(layout, NULL, &rect);
	y -= (float)pango_layout_get_baseline(layout) / PANGO_SCALE;
	if (paint->alignment == PANGO_ALIGN_CENTER)
		x -= rect.width / 2.f;
	else if (paint->alignment == PANGO_ALIGN_RIGHT)
		x -= rect.width;
	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(x, y));
	gtk_snapshot_append_layout(snapshot, layout, &paint->color);
	gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(-x, -y));
	if (paint->use_color_filter)
		gtk_snapshot_pop(snapshot);
	g_object_unref(layout);
}

/* Canvas.drawGlyphs(): the glyphs of the font (a family registered with fontconfig by Font.Builder) by glyph id,
 * each at its own position, at the size and with the color of the paint. */
JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawGlyphs(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jintArray ids_arr, jint id_offset, jfloatArray pos_arr, jint pos_offset, jint count, jstring family, jint weight, jint slant, jlong paint_ptr)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
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
	PangoFont *font = pango_context_load_font(atl_pango_context(), desc);
	pango_font_description_free(desc);
	if (!font)
		return;
	jint *ids = (*env)->GetIntArrayElements(env, ids_arr, NULL);
	jfloat *pos = (*env)->GetFloatArrayElements(env, pos_arr, NULL);
	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
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
		GskRenderNode *node = gsk_text_node_new(font, glyphs, &paint->color, &GRAPHENE_POINT_INIT(pos[pos_offset + i * 2], pos[pos_offset + i * 2 + 1]));
		if (node) {
			gtk_snapshot_append_node(snapshot, node);
			gsk_render_node_unref(node);
		}
	}
	pango_glyph_string_free(glyphs);
	if (paint->use_color_filter)
		gtk_snapshot_pop(snapshot);
	(*env)->ReleaseIntArrayElements(env, ids_arr, ids, JNI_ABORT);
	(*env)->ReleaseFloatArrayElements(env, pos_arr, pos, JNI_ABORT);
	g_object_unref(font);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawRoundRect(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom, jfloat rx, jfloat ry, jlong paint_ptr)
{
	GdkSnapshot *snapshot = (GdkSnapshot *)_PTR(snapshot_ptr);
	struct AndroidPaint *paint = _PTR(paint_ptr);
	GdkRGBA gdk_color[4];
	for (int i = 0; i < 4; i++) {
		gdk_color[i].red = paint->color.red;
		gdk_color[i].green = paint->color.green;
		gdk_color[i].blue = paint->color.blue;
		gdk_color[i].alpha = paint->color.alpha;
	}
	GskRoundedRect round_rect = {
		.bounds = GRAPHENE_RECT_INIT(left, top, right - left, bottom - top),
		.corner = {{rx, ry}, {rx, ry}, {rx, ry}, {rx, ry}},
	};
	float width = gsk_stroke_get_line_width(paint->gsk_stroke);
	const float widths[4] = {width, width, width, width};
	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	if (paint->is_fill) {
		gtk_snapshot_push_rounded_clip(snapshot, &round_rect);
		gtk_snapshot_append_color(snapshot, gdk_color, &round_rect.bounds);
		gtk_snapshot_pop(snapshot);
	}
	if (paint->is_stroke) {
		gtk_snapshot_append_border(snapshot, &round_rect, widths, gdk_color);
	}
	if (paint->use_color_filter)
		gtk_snapshot_pop(snapshot);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1scale(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloat x, jfloat y)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	gtk_snapshot_scale(snapshot, x, y);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1concat(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jlong matrix_ptr)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	graphene_matrix_t *matrix = (graphene_matrix_t *)_PTR(matrix_ptr);
	atl_snapshot_concat_matrix(snapshot, matrix);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1clipRect(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	gtk_snapshot_push_clip(snapshot, &GRAPHENE_RECT_INIT(left, top, right - left, bottom - top));
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1pushOpacity(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloat opacity)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	gtk_snapshot_push_opacity(snapshot, opacity);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1pop(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jint count)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	while (count--)
		gtk_snapshot_pop(snapshot);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawRenderNode(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jlong node_ptr)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	GskRenderNode *node = _PTR(node_ptr);
	gtk_snapshot_append_node(snapshot, node);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1clipPath(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jlong path_ptr, jint fill_rule)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	GskPath *path = _PTR(path_ptr);
	gtk_snapshot_push_mask(snapshot, GSK_MASK_MODE_ALPHA);
	gtk_snapshot_append_fill(snapshot, path, fill_rule, &(GdkRGBA){1, 1, 1, 1});
	gtk_snapshot_pop(snapshot);
}
