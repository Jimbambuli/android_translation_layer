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

/* AOSP Shader.TileMode.nativeInt */
enum { TILE_CLAMP, TILE_REPEAT, TILE_MIRROR, TILE_DECAL };

/* a part [start, end) of the filled area along one axis, which repeats the part [child_start, child_start + child_len)
 * of the tile */
struct tile_segment {
	float start, end, child_start, child_len;
};

/* the parts of [a0, a1) along an axis of the bitmap of the given size (AOSP SkBitmapProcShader): CLAMP extends the edge
 * pixels, REPEAT repeats the bitmap, MIRROR repeats it together with its mirror image, DECAL leaves the outside empty */
static int tile_segments(int mode, float size, float a0, float a1, struct tile_segment *seg)
{
	struct tile_segment all[3];
	int n = 0;
	switch (mode) {
		case TILE_CLAMP:
			all[n++] = (struct tile_segment){a0, 0, 0, 1};
			all[n++] = (struct tile_segment){0, size, 0, size};
			all[n++] = (struct tile_segment){size, a1, size - 1, 1};
			break;
		case TILE_REPEAT:
			all[n++] = (struct tile_segment){a0, a1, 0, size};
			break;
		case TILE_MIRROR:
			all[n++] = (struct tile_segment){a0, a1, 0, 2 * size};
			break;
		default:
			all[n++] = (struct tile_segment){0, size, 0, size};
			break;
	}
	int count = 0;
	for (int i = 0; i < n; i++) {
		all[i].start = MAX(all[i].start, a0);
		all[i].end = MIN(all[i].end, a1);
		if (all[i].end > all[i].start)
			seg[count++] = all[i];
	}
	return count;
}

/* the source coordinate of v along an axis of the given size: REPEAT wraps, MIRROR wraps with every
 * second copy mirrored, the other modes stay inside the bitmap */
static int32_t tile_source(int32_t mode, int32_t size, int64_t v)
{
	if (mode == TILE_REPEAT)
		return ((v % size) + size) % size;
	if (mode == TILE_MIRROR) {
		int64_t m = ((v % (2 * size)) + 2 * size) % (2 * size);
		return m < size ? m : 2 * size - 1 - m;
	}
	return v;
}

struct tiled_texture {
	int32_t tile_x, tile_y;
	int64_t x0, y0;
	int32_t w, h;
	GdkTexture *texture;
};

static void tiled_texture_free(struct tiled_texture *tiled)
{
	g_object_unref(tiled->texture);
	g_free(tiled);
}

/* the bitmap repeated along each REPEAT/MIRROR axis over [x0, x0 + w) x [y0, y0 + h), as one texture; the
 * other axes keep the bitmap's size. Kept on the bitmap's texture for the next frame. NULL if too large */
static struct tiled_texture *get_tiled_texture(GdkTexture *texture, int32_t tile_x, int32_t tile_y, int64_t x0, int64_t y0, int32_t w, int32_t h)
{
	static GQuark quark;
	if (!quark)
		quark = g_quark_from_static_string("atl-tiled-texture");
	struct tiled_texture *tiled = g_object_get_qdata(G_OBJECT(texture), quark);
	if (tiled && tiled->tile_x == tile_x && tiled->tile_y == tile_y && tiled->x0 == x0 && tiled->y0 == y0 && tiled->w == w && tiled->h == h)
		return tiled;
	int32_t width = gdk_texture_get_width(texture);
	int32_t height = gdk_texture_get_height(texture);
	/* an ALPHA_8 bitmap stays a mask (the color filter colors it), all others become 8 bit RGBA */
	GdkMemoryFormat format = gdk_texture_get_format(texture) == GDK_MEMORY_A8 ? GDK_MEMORY_A8 : GDK_MEMORY_R8G8B8A8_PREMULTIPLIED;
	gsize bpp = format == GDK_MEMORY_A8 ? 1 : 4;
	gsize stride = width * bpp;
	if ((gsize)w * h * bpp > 64 * 1024 * 1024)
		return NULL;
	guchar *src = g_malloc(stride * height);
	GdkTextureDownloader *downloader = gdk_texture_downloader_new(texture);
	gdk_texture_downloader_set_format(downloader, format);
	gdk_texture_downloader_download_into(downloader, src, stride);
	gdk_texture_downloader_free(downloader);
	guchar *dst = g_malloc((gsize)w * h * bpp);
	for (int32_t y = 0; y < h; y++) {
		const guchar *src_row = src + tile_source(tile_y, height, y0 + y) * stride;
		guchar *dst_row = dst + (gsize)y * w * bpp;
		for (int32_t x = 0; x < w; x++)
			memcpy(dst_row + x * bpp, src_row + tile_source(tile_x, width, x0 + x) * bpp, bpp);
	}
	g_free(src);
	GBytes *tiled_bytes = g_bytes_new_take(dst, (gsize)w * h * bpp);
	tiled = g_new(struct tiled_texture, 1);
	*tiled = (struct tiled_texture){tile_x, tile_y, x0, y0, w, h,
	                                gdk_memory_texture_new(w, h, format, tiled_bytes, w * bpp)};
	g_bytes_unref(tiled_bytes);
	g_object_set_qdata_full(G_OBJECT(texture), quark, tiled, (GDestroyNotify)tiled_texture_free);
	return tiled;
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1fillBitmapShader(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jlong texture_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom, jint tile_x, jint tile_y, jlong paint_ptr)
{
	GdkSnapshot *snapshot = (GdkSnapshot *)_PTR(snapshot_ptr);
	GdkTexture *texture = GDK_TEXTURE(_PTR(texture_ptr));
	struct AndroidPaint *paint = _PTR(paint_ptr);
	float width = gdk_texture_get_width(texture);
	float height = gdk_texture_get_height(texture);
	struct tile_segment xs[3], ys[3];
	int nx = tile_segments(tile_x, width, left, right, xs);
	int ny = tile_segments(tile_y, height, top, bottom, ys);
	/* Skia samples the shader per device pixel. GSK draws a repeat node whose visible part holds no whole
	 * tile in bands, one per row or column of tiles, each with antialiased edges
	 * (gsk_gpu_node_processor_add_repeat_node()); where a band ends inside a pixel (a tile scaled to a
	 * fractional size) the two bands leave a lighter line. So the repeating axes are tiled into one texture
	 * that covers the whole area, and GSK draws it in one piece */
	gboolean repeat_x = tile_x == TILE_REPEAT || tile_x == TILE_MIRROR;
	gboolean repeat_y = tile_y == TILE_REPEAT || tile_y == TILE_MIRROR;
	struct tiled_texture *tiled = NULL;
	if (repeat_x || repeat_y) {
		/* one pixel more on each side, so that the filtering at the edges of the area sees the next tile */
		int64_t x0 = repeat_x ? (int64_t)floorf(left) - 1 : 0, y0 = repeat_y ? (int64_t)floorf(top) - 1 : 0;
		int64_t x1 = repeat_x ? (int64_t)ceilf(right) + 1 : (int64_t)width, y1 = repeat_y ? (int64_t)ceilf(bottom) + 1 : (int64_t)height;
		if (x1 - x0 <= G_MAXINT && y1 - y0 <= G_MAXINT)
			tiled = get_tiled_texture(texture, tile_x, tile_y, x0, y0, x1 - x0, y1 - y0);
		if (tiled) {
			for (int32_t i = 0; i < nx && repeat_x; i++)
				xs[i].child_start = x0, xs[i].child_len = x1 - x0;
			for (int32_t j = 0; j < ny && repeat_y; j++)
				ys[j].child_start = y0, ys[j].child_len = y1 - y0;
		}
	}
	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	push_paint_alpha(snapshot, paint);
	for (int j = 0; j < ny; j++) {
		for (int i = 0; i < nx; i++) {
			gtk_snapshot_push_repeat(snapshot, &GRAPHENE_RECT_INIT(xs[i].start, ys[j].start, xs[i].end - xs[i].start, ys[j].end - ys[j].start),
			                         &GRAPHENE_RECT_INIT(xs[i].child_start, ys[j].child_start, xs[i].child_len, ys[j].child_len));
			if (tiled) {
				gtk_snapshot_append_texture(snapshot, tiled->texture, &GRAPHENE_RECT_INIT(tiled->x0, tiled->y0, tiled->w, tiled->h));
				gtk_snapshot_pop(snapshot);
				continue;
			}
			/* the bitmap, for MIRROR followed by its mirror image right of and below it */
			for (int my = 0; my <= (tile_y == TILE_MIRROR); my++) {
				for (int mx = 0; mx <= (tile_x == TILE_MIRROR); mx++) {
					gtk_snapshot_save(snapshot);
					gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(mx ? 2 * width : 0, my ? 2 * height : 0));
					gtk_snapshot_scale(snapshot, mx ? -1 : 1, my ? -1 : 1);
					gtk_snapshot_append_texture(snapshot, texture, &GRAPHENE_RECT_INIT(0, 0, width, height));
					gtk_snapshot_restore(snapshot);
				}
			}
			gtk_snapshot_pop(snapshot);
		}
	}
	pop_paint_alpha(snapshot, paint);
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
	bool shadow = android_paint_push_shadow(snapshot, paint);
	gtk_snapshot_append_color(snapshot, &paint->color, &bounds);
	if (shadow)
		gtk_snapshot_pop(snapshot);
	if (paint->use_color_filter)
		gtk_snapshot_pop(snapshot);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawLinearGradient(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom, jfloat x0, jfloat y0, jfloat x1, jfloat y1, jintArray colors_arr, jfloatArray positions_arr, jint tile_mode, jlong paint_ptr)
{
	/* TileMode.MIRROR is not supported by GSK and falls back to CLAMP */
	bool repeat = tile_mode == TILE_REPEAT;
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

/* extent of an inverse fill, as Canvas.drawPaint() */
#define INVERSE_FILL_EXTENT 100000.f

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawPath(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jlong path_ptr, jint fill_rule, jboolean inverse, jlong paint_ptr)
{
	GtkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	GskPath *path = _PTR(path_ptr);
	struct AndroidPaint *paint = _PTR(paint_ptr);
	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	bool shadow = android_paint_push_shadow(snapshot, paint);
	if (inverse) {
		/* Skia: an inverse fill type covers everything outside the path; a stroke keeps the inverse bit
		 * (SkStroke), so it covers everything outside the stroke */
		const GdkRGBA opaque = {1, 1, 1, 1};
		gtk_snapshot_push_mask(snapshot, GSK_MASK_MODE_INVERTED_ALPHA);
		if (paint->is_stroke)
			gtk_snapshot_append_stroke(snapshot, path, paint->gsk_stroke, &opaque);
		if (paint->is_fill)
			gtk_snapshot_append_fill(snapshot, path, fill_rule, &opaque);
		gtk_snapshot_pop(snapshot);
		gtk_snapshot_append_color(snapshot, &paint->color, &GRAPHENE_RECT_INIT(-INVERSE_FILL_EXTENT, -INVERSE_FILL_EXTENT, 2 * INVERSE_FILL_EXTENT, 2 * INVERSE_FILL_EXTENT));
		gtk_snapshot_pop(snapshot);
	} else {
		if (paint->is_stroke) {
			gtk_snapshot_append_stroke(snapshot, path, paint->gsk_stroke, &paint->color);
		}
		if (paint->is_fill) {
			gtk_snapshot_append_fill(snapshot, path, fill_rule, &paint->color);
		}
	}
	if (shadow)
		gtk_snapshot_pop(snapshot);
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

static void draw_line(GdkSnapshot *snapshot, struct AndroidPaint *paint, jfloat x0, jfloat y0, jfloat x1, jfloat y1)
{
	if (!isfinite(x0) || !isfinite(y0) || !isfinite(x1) || !isfinite(y1)) {
		return;
	}
	gtk_snapshot_save(snapshot);
	gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(x0, y0));
	float rotation = atan2(y1 - y0, x1 - x0);
	gtk_snapshot_rotate(snapshot, rotation * 180 / M_PI);
	float length = sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
	float stroke_width = gsk_stroke_get_line_width(paint->gsk_stroke);
	gtk_snapshot_append_color(snapshot, &paint->color, &GRAPHENE_RECT_INIT(0, -stroke_width / 2, length, stroke_width));
	gtk_snapshot_restore(snapshot);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawLine(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloat x0, jfloat y0, jfloat x1, jfloat y1, jlong paint_ptr)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	struct AndroidPaint *paint = _PTR(paint_ptr);
	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	bool shadow = android_paint_push_shadow(snapshot, paint);
	draw_line(snapshot, paint, x0, y0, x1, y1);
	if (shadow)
		gtk_snapshot_pop(snapshot);
	if (paint->use_color_filter)
		gtk_snapshot_pop(snapshot);
}

/* like hwui's drawLines(), one draw (one shadow) for all lines */
JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawLines(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jfloatArray points_arr, jint offset, jint count, jlong paint_ptr)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	struct AndroidPaint *paint = _PTR(paint_ptr);
	jfloat *points = (*env)->GetFloatArrayElements(env, points_arr, NULL);
	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	bool shadow = android_paint_push_shadow(snapshot, paint);
	/* count is the number of values in points; four make one line */
	for (int i = offset; i + 4 <= offset + count; i += 4)
		draw_line(snapshot, paint, points[i + 0], points[i + 1], points[i + 2], points[i + 3]);
	if (shadow)
		gtk_snapshot_pop(snapshot);
	if (paint->use_color_filter)
		gtk_snapshot_pop(snapshot);
	(*env)->ReleaseFloatArrayElements(env, points_arr, points, 0);
}

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1drawText(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jstring text, jfloat x, jfloat y, jlong paint_ptr)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
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
	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(x, y));
	android_paint_append_layout(snapshot, layout, paint);
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
	PangoFont *font = pango_context_load_font(android_paint_pango_context(paint, desc), desc);
	pango_font_description_free(desc);
	if (!font)
		return;
	jint *ids = (*env)->GetIntArrayElements(env, ids_arr, NULL);
	jfloat *pos = (*env)->GetFloatArrayElements(env, pos_arr, NULL);
	if (paint->use_color_filter)
		gtk_snapshot_push_color_matrix(snapshot, &paint->color_matrix, &paint->color_offset);
	bool shadow = android_paint_push_shadow(snapshot, paint);
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
		android_paint_append_glyphs(snapshot, font, glyphs, &GRAPHENE_POINT_INIT(pos[pos_offset + i * 2], pos[pos_offset + i * 2 + 1]), paint);
	}
	pango_glyph_string_free(glyphs);
	if (shadow)
		gtk_snapshot_pop(snapshot);
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
	bool shadow = android_paint_push_shadow(snapshot, paint);
	if (paint->is_fill) {
		gtk_snapshot_push_rounded_clip(snapshot, &round_rect);
		gtk_snapshot_append_color(snapshot, gdk_color, &round_rect.bounds);
		gtk_snapshot_pop(snapshot);
	}
	if (paint->is_stroke) {
		gtk_snapshot_append_border(snapshot, &round_rect, widths, gdk_color);
	}
	if (shadow)
		gtk_snapshot_pop(snapshot);
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

JNIEXPORT jboolean JNICALL Java_android_atl_GskCanvas_native_1canComposite(JNIEnv *env, jclass this_class)
{
	return GTK_CHECK_VERSION(4, 22, 0) ? JNI_TRUE : JNI_FALSE;
}

/* AOSP saveLayer() draws into a transparent offscreen; GSK isolates the group from the background */
JNIEXPORT jboolean JNICALL Java_android_atl_GskCanvas_native_1pushIsolation(JNIEnv *env, jclass this_class, jlong snapshot_ptr)
{
#if GTK_CHECK_VERSION(4, 22, 0)
	gtk_snapshot_push_isolation(GTK_SNAPSHOT(_PTR(snapshot_ptr)), GSK_ISOLATION_BACKGROUND);
	return JNI_TRUE;
#else
	return JNI_FALSE;
#endif
}

/* Porter-Duff composite of the drawing onto the background; the caller records the mask, pops, records the
 * drawing and pops again. op is a GskPorterDuff value */
JNIEXPORT jboolean JNICALL Java_android_atl_GskCanvas_native_1pushComposite(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jint op)
{
#if GTK_CHECK_VERSION(4, 22, 0)
	gtk_snapshot_push_composite(GTK_SNAPSHOT(_PTR(snapshot_ptr)), (GskPorterDuff)op);
	return JNI_TRUE;
#else
	return JNI_FALSE;
#endif
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

JNIEXPORT void JNICALL Java_android_atl_GskCanvas_native_1clipPath(JNIEnv *env, jclass this_class, jlong snapshot_ptr, jlong path_ptr, jint fill_rule, jboolean inverse)
{
	GdkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	GskPath *path = _PTR(path_ptr);
	/* an inverse fill type clips to everything outside the path */
	gtk_snapshot_push_mask(snapshot, inverse ? GSK_MASK_MODE_INVERTED_ALPHA : GSK_MASK_MODE_ALPHA);
	gtk_snapshot_append_fill(snapshot, path, fill_rule, &(GdkRGBA){1, 1, 1, 1});
	gtk_snapshot_pop(snapshot);
}
