#include <gtk/gtk.h>
#include <pango/pango.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_text_Layout.h"
#include "../graphics/AndroidPaint.h"

extern GtkWidget *window;

JNIEXPORT jlong JNICALL Java_android_text_Layout_native_1constructor(JNIEnv *env, jobject object, jstring text, jlong paint, jint width)
{
	struct AndroidPaint *android_paint = _PTR(paint);
	PangoLayout *layout = pango_layout_new(gtk_widget_get_pango_context(window));
	pango_layout_set_font_description(layout, android_paint->font);
	android_paint_apply_letter_spacing(layout, android_paint);
	/* real UTF-8: GetStringUTFChars would give surrogate pairs of emoji as two 3-byte sequences (CESU-8), which Pango rejects */
	const jchar *chars = (*env)->GetStringChars(env, text, NULL);
	gchar *str = g_utf16_to_utf8((const gunichar2 *)chars, (*env)->GetStringLength(env, text), NULL, NULL, NULL);
	(*env)->ReleaseStringChars(env, text, chars);
	pango_layout_set_text(layout, str ? str : "", -1);
	g_free(str);
	pango_layout_set_width(layout, width == -1 ? -1 : width * PANGO_SCALE);
	return _INTPTR(layout);
}

/* the byte offset in the UTF-8 text of an offset in UTF-16 units */
static guint utf16_to_byte_offset(const char *text, int offset)
{
	const char *p = text;
	while (*p && offset > 0) {
		offset -= g_utf8_get_char(p) > 0xFFFF ? 2 : 1;
		p = g_utf8_next_char(p);
	}
	return p - text;
}

/* the offset in UTF-16 units of a byte offset in the UTF-8 text (Pango indexes in bytes, Java in UTF-16 units) */
static jint byte_to_utf16_offset(const char *text, int byte_index)
{
	const char *p = text, *end = text + byte_index;
	jint offset = 0;
	while (*p && p < end) {
		offset += g_utf8_get_char(p) > 0xFFFF ? 2 : 1;
		p = g_utf8_next_char(p);
	}
	return offset;
}

static guint16 color_channel(jint argb, int shift)
{
	return ((argb >> shift) & 0xFF) * 0x101;
}

JNIEXPORT void JNICALL Java_android_text_Layout_native_1add_1attribute(JNIEnv *env, jclass class, jlong layout, jint type, jint start, jint end, jint color, jfloat value)
{
	PangoLayout *pango_layout = PANGO_LAYOUT(_PTR(layout));
	const char *text = pango_layout_get_text(pango_layout);
	PangoAttrList *attrs = pango_layout_get_attributes(pango_layout);
	attrs = attrs ? pango_attr_list_ref(attrs) : pango_attr_list_new();
	PangoAttribute *attr = NULL, *alpha = NULL;
	switch (type) {
		case 0: // AbsoluteSizeSpan
			attr = pango_attr_size_new_absolute(value * PANGO_SCALE);
			break;
		case 1: // RelativeSizeSpan
			attr = pango_attr_scale_new(value);
			break;
		case 2: // ForegroundColorSpan
			attr = pango_attr_foreground_new(color_channel(color, 16), color_channel(color, 8), color_channel(color, 0));
			alpha = pango_attr_foreground_alpha_new(color_channel(color, 24));
			break;
		case 3: // BackgroundColorSpan
			attr = pango_attr_background_new(color_channel(color, 16), color_channel(color, 8), color_channel(color, 0));
			alpha = pango_attr_background_alpha_new(color_channel(color, 24));
			break;
		case 4: // StyleSpan BOLD
			attr = pango_attr_weight_new(PANGO_WEIGHT_BOLD);
			break;
		case 5: // StyleSpan ITALIC
			attr = pango_attr_style_new(PANGO_STYLE_ITALIC);
			break;
		case 6: // UnderlineSpan
			attr = pango_attr_underline_new(PANGO_UNDERLINE_SINGLE);
			break;
		case 7: // StrikethroughSpan
			attr = pango_attr_strikethrough_new(TRUE);
			break;
		case 8: // StaticLayout.Builder.setHyphenationFrequency(): a hyphen where a word is broken
			attr = pango_attr_insert_hyphens_new(value != 0);
			break;
	}
	guint start_index = utf16_to_byte_offset(text, start);
	guint end_index = start_index + utf16_to_byte_offset(text + start_index, end - start);
	for (PangoAttribute *a = attr; a; a = (a == attr ? alpha : NULL)) {
		a->start_index = start_index;
		a->end_index = end_index;
		pango_attr_list_insert(attrs, a);
	}
	pango_layout_set_attributes(pango_layout, attrs);
	pango_attr_list_unref(attrs);
}

JNIEXPORT void JNICALL Java_android_text_Layout_native_1set_1alignment(JNIEnv *env, jclass class, jlong layout, jint alignment)
{
	static const PangoAlignment pango_alignments[] = {PANGO_ALIGN_LEFT, PANGO_ALIGN_CENTER, PANGO_ALIGN_RIGHT};
	pango_layout_set_alignment(PANGO_LAYOUT(_PTR(layout)), pango_alignments[alignment]);
}

/* the extents of a line in layout coordinates, which include the offset from the alignment
 * (pango_layout_line_get_extents() is relative to the line's own origin) */
static void line_extents(PangoLayout *pango_layout, int line, PangoRectangle *logical_rect)
{
	PangoLayoutIter *pango_iter = pango_layout_get_iter(pango_layout);
	for (int i = 0; i < line && pango_layout_iter_next_line(pango_iter); i++)
		;
	pango_layout_iter_get_line_extents(pango_iter, NULL, logical_rect);
	pango_layout_iter_free(pango_iter);
}

/* AOSP adds (mult - 1) * line height + add between lines, not after the last one; Pango's spacing
 * does the same (with one line height for the whole layout). Indents: Pango knows a first-line
 * indent and a hanging indent of all other lines, not both at once. */
JNIEXPORT void JNICALL Java_android_text_Layout_native_1set_1paragraph_1style(JNIEnv *env, jclass class, jlong layout, jfloat spacing_mult, jfloat spacing_add, jboolean justify, jint first_indent, jint rest_indent)
{
	PangoLayout *pango_layout = PANGO_LAYOUT(_PTR(layout));
	float spacing = spacing_add * PANGO_SCALE;
	if (spacing_mult != 1.0f) {
		PangoRectangle logical_rect;
		line_extents(pango_layout, 0, &logical_rect);
		spacing += logical_rect.height * (spacing_mult - 1.0f);
	}
	pango_layout_set_spacing(pango_layout, spacing);
	pango_layout_set_justify(pango_layout, justify);
	if (rest_indent == 0)
		pango_layout_set_indent(pango_layout, first_indent * PANGO_SCALE);
	else if (first_indent == 0)
		pango_layout_set_indent(pango_layout, -rest_indent * PANGO_SCALE);
}

/* AOSP's StaticLayout stops after max_lines lines and ellipsizes the last; a positive height makes
 * Pango do the same (a negative one would count lines per paragraph) */
JNIEXPORT void JNICALL Java_android_text_Layout_native_1set_1max_1lines(JNIEnv *env, jclass class, jlong layout, jint max_lines, jint ellipsize_mode)
{
	PangoLayout *pango_layout = PANGO_LAYOUT(_PTR(layout));
	if (max_lines < 1 || pango_layout_get_line_count(pango_layout) <= max_lines)
		return;
	PangoRectangle logical_rect;
	line_extents(pango_layout, max_lines - 1, &logical_rect);
	pango_layout_set_ellipsize(pango_layout, (PangoEllipsizeMode)ellipsize_mode);
	pango_layout_set_height(pango_layout, logical_rect.y + logical_rect.height);
}

JNIEXPORT void JNICALL Java_android_text_Layout_native_1set_1width(JNIEnv *env, jobject object, jlong layout, jint width)
{
	PangoLayout *pango_layout = _PTR(layout);
	pango_layout_set_width(pango_layout, width * PANGO_SCALE);
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1width(JNIEnv *env, jobject object, jlong layout)
{
	PangoLayout *pango_layout = _PTR(layout);
	return pango_layout_get_width(pango_layout) / PANGO_SCALE;
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1height(JNIEnv *env, jobject object, jlong layout)
{
	PangoLayout *pango_layout = _PTR(layout);
	PangoRectangle logical_rect;
	pango_layout_get_extents(pango_layout, NULL, &logical_rect);
	return logical_rect.height / PANGO_SCALE;
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1line_1count(JNIEnv *env, jobject object, jlong layout)
{
	PangoLayout *pango_layout = _PTR(layout);
	return pango_layout_get_line_count(pango_layout);
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1line_1start(JNIEnv *env, jobject object, jlong layout, jint line)
{
	PangoLayout *pango_layout = _PTR(layout);
	PangoLayoutLine *pango_line = pango_layout_get_line_readonly(pango_layout, line);
	int byte_index = pango_layout_line_get_start_index(pango_line);
	return byte_to_utf16_offset(pango_layout_get_text(pango_layout), byte_index);
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1line_1end(JNIEnv *env, jobject object, jlong layout, jint line)
{
	PangoLayout *pango_layout = _PTR(layout);
	PangoLayoutLine *pango_line = pango_layout_get_line_readonly(pango_layout, line);
	int byte_index = pango_layout_line_get_start_index(pango_line) + pango_layout_line_get_length(pango_line);
	return byte_to_utf16_offset(pango_layout_get_text(pango_layout), byte_index);
}

static void get_line_metrics(PangoLayout *pango_layout, jint line, int *baseline, PangoRectangle *logical_rect)
{
	PangoLayoutIter *pango_iter = pango_layout_get_iter(pango_layout);
	while (line--)
		pango_layout_iter_next_line(pango_iter);
	*baseline = pango_layout_iter_get_baseline(pango_iter) / PANGO_SCALE;
	pango_layout_line_get_extents(pango_layout_iter_get_line_readonly(pango_iter), NULL, logical_rect);
	pango_layout_iter_free(pango_iter);
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1line_1top(JNIEnv *env, jobject object, jlong layout, jint line)
{
	int baseline;
	PangoRectangle logical_rect;
	get_line_metrics(_PTR(layout), line, &baseline, &logical_rect);
	return baseline - PANGO_ASCENT(logical_rect) / PANGO_SCALE;
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1line_1bottom(JNIEnv *env, jobject object, jlong layout, jint line)
{
	int baseline;
	PangoRectangle logical_rect;
	get_line_metrics(_PTR(layout), line, &baseline, &logical_rect);
	return baseline + PANGO_DESCENT(logical_rect) / PANGO_SCALE;
}

JNIEXPORT jfloat JNICALL Java_android_text_Layout_native_1get_1line_1left(JNIEnv *env, jobject object, jlong layout, jint line)
{
	PangoRectangle logical_rect;
	line_extents(PANGO_LAYOUT(_PTR(layout)), line, &logical_rect);
	return (jfloat)logical_rect.x / PANGO_SCALE;
}

JNIEXPORT jfloat JNICALL Java_android_text_Layout_native_1get_1line_1right(JNIEnv *env, jobject object, jlong layout, jint line)
{
	PangoRectangle logical_rect;
	line_extents(PANGO_LAYOUT(_PTR(layout)), line, &logical_rect);
	return (jfloat)(logical_rect.x + logical_rect.width) / PANGO_SCALE;
}

/* Layout.getLineWidth() is a float: callers that make a layout as wide as its line (Compose rounds it up) wrap
 * the line when the width was cut down to whole pixels */
JNIEXPORT jfloat JNICALL Java_android_text_Layout_native_1get_1line_1width(JNIEnv *env, jobject object, jlong layout, jint line)
{
	PangoLayoutLine *pango_line = pango_layout_get_line_readonly(PANGO_LAYOUT(_PTR(layout)), line);
	PangoRectangle logical_rect;
	pango_layout_line_get_extents(pango_line, NULL, &logical_rect);
	return (jfloat)logical_rect.width / PANGO_SCALE;
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1line_1baseline(JNIEnv *env, jobject object, jlong layout, jint line)
{
	int baseline;
	PangoRectangle logical_rect;
	get_line_metrics(_PTR(layout), line, &baseline, &logical_rect);
	return baseline;
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1line_1ascent(JNIEnv *env, jobject object, jlong layout, jint line)
{
	PangoLayout *pango_layout = _PTR(layout);
	PangoLayoutLine *pango_line = pango_layout_get_line_readonly(pango_layout, line);
	PangoRectangle logical_rect;
	pango_layout_line_get_extents(pango_line, NULL, &logical_rect);
	return -PANGO_ASCENT(logical_rect) / PANGO_SCALE;
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1line_1descent(JNIEnv *env, jobject object, jlong layout, jint line)
{
	PangoLayout *pango_layout = _PTR(layout);
	PangoLayoutLine *pango_line = pango_layout_get_line_readonly(pango_layout, line);
	PangoRectangle logical_rect;
	pango_layout_line_get_extents(pango_line, NULL, &logical_rect);
	return PANGO_DESCENT(logical_rect) / PANGO_SCALE;
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1line_1for_1vertical(JNIEnv *env, jobject object, jlong layout, jint y)
{
	PangoLayout *pango_layout = _PTR(layout);
	int index_, trailing;
	pango_layout_xy_to_index(pango_layout, 0, y * PANGO_SCALE, &index_, &trailing);
	int line, x_pos;
	pango_layout_index_to_line_x(pango_layout, index_, trailing, &line, &x_pos);
	return line;
}

JNIEXPORT void JNICALL Java_android_text_Layout_native_1set_1ellipsize(JNIEnv *env, jobject object, jlong layout, jint ellipsize_mode, jfloat ellipsize_width)
{
	PangoLayout *pango_layout = _PTR(layout);
	pango_layout_set_ellipsize(pango_layout, (PangoEllipsizeMode)ellipsize_mode);
	pango_layout_set_width(pango_layout, ellipsize_width * PANGO_SCALE);
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1ellipsis_1count(JNIEnv *env, jobject object, jlong layout, jint line)
{
	PangoLayout *pango_layout = _PTR(layout);
	return pango_layout_is_ellipsized(pango_layout);
}

JNIEXPORT void JNICALL Java_android_text_Layout_native_1draw(JNIEnv *env, jobject object, jlong layout, jlong snapshot_ptr, jlong paint_ptr)
{
	PangoLayout *pango_layout = PANGO_LAYOUT(_PTR(layout));
	GtkSnapshot *snapshot = GTK_SNAPSHOT(_PTR(snapshot_ptr));
	struct AndroidPaint *android_paint = _PTR(paint_ptr);

	gtk_snapshot_append_layout(snapshot, pango_layout, &android_paint->color);
}

JNIEXPORT void JNICALL Java_android_text_Layout_native_1draw_1custom_1canvas(JNIEnv *env, jobject object, jlong layout, jobject canvas, jobject paint)
{
	PangoLayout *pango_layout = PANGO_LAYOUT(_PTR(layout));

	const gchar *text = pango_layout_get_text(pango_layout);
	PangoLayoutIter *pango_iter = pango_layout_get_iter(pango_layout);
	do {
		PangoLayoutLine *pango_line = pango_layout_iter_get_line_readonly(pango_iter);

		/* the line's length is in bytes of UTF-8, the Java string counts UTF-16 units: hand over
		 * exactly the line's text, so lines with non-ASCII characters do not run into the next */
		gchar *line_text = g_strndup(text + pango_line->start_index, pango_line->length);
		jstring text_jstr = (*env)->NewStringUTF(env, line_text);
		g_free(line_text);
		jint end = (*env)->GetStringLength(env, text_jstr);
		jfloat y = (float)pango_layout_iter_get_baseline(pango_iter) / PANGO_SCALE;
		PangoRectangle logical_rect;
		pango_layout_iter_get_line_extents(pango_iter, NULL, &logical_rect);
		jfloat x = (float)logical_rect.x / PANGO_SCALE;
		J__Canvas__drawText(env, canvas, text_jstr, (jint)0, end, x, y, paint);
		(*env)->DeleteLocalRef(env, text_jstr);
	} while (pango_layout_iter_next_line(pango_iter));

	pango_layout_iter_free(pango_iter);
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1line_1for_1offset(JNIEnv *env, jclass class, jlong layout, jint offset)
{
	PangoLayout *pango_layout = _PTR(layout);
	int line;
	pango_layout_index_to_line_x(pango_layout, utf16_to_byte_offset(pango_layout_get_text(pango_layout), offset), FALSE, &line, NULL);
	return line;
}

JNIEXPORT jfloat JNICALL Java_android_text_Layout_native_1get_1primary_1horizontal(JNIEnv *env, jclass class, jlong layout, jint offset)
{
	PangoLayout *pango_layout = _PTR(layout);
	PangoRectangle cursor_rect;
	pango_layout_get_cursor_pos(pango_layout, utf16_to_byte_offset(pango_layout_get_text(pango_layout), offset), &cursor_rect, NULL);
	return (jfloat)cursor_rect.x / PANGO_SCALE;
}

JNIEXPORT jfloat JNICALL Java_android_text_Layout_native_1get_1secondary_1horizontal(JNIEnv *env, jclass class, jlong layout, jint offset)
{
	PangoLayout *pango_layout = _PTR(layout);
	PangoRectangle cursor_rect;
	pango_layout_get_cursor_pos(pango_layout, utf16_to_byte_offset(pango_layout_get_text(pango_layout), offset), NULL, &cursor_rect);
	return (jfloat)cursor_rect.x / PANGO_SCALE;
}

JNIEXPORT jint JNICALL Java_android_text_Layout_native_1get_1offset_1for_1horizontal(JNIEnv *env, jclass class, jlong layout, jint line, jfloat x)
{
	PangoLayout *pango_layout = _PTR(layout);
	PangoLayoutLine *pango_line = pango_layout_get_line_readonly(pango_layout, line);
	int index;
	pango_layout_line_x_to_index(pango_line, x * PANGO_SCALE, &index, NULL);
	/* a byte index of Pango is not an offset in UTF-16 units */
	return byte_to_utf16_offset(pango_layout_get_text(pango_layout), index);
}

JNIEXPORT jfloat JNICALL Java_android_text_Layout_native_1get_1desired_1width(JNIEnv *env, jclass class, jlong layout)
{
	PangoLayout *pango_layout = _PTR(layout);
	int width;
	pango_layout_get_size(pango_layout, &width, NULL);
	return (jfloat)width / PANGO_SCALE;
}

JNIEXPORT void JNICALL Java_android_text_Layout_native_1free(JNIEnv *env, jclass class, jlong layout)
{
	PangoLayout *pango_layout = _PTR(layout);
	g_object_unref(pango_layout);
}
