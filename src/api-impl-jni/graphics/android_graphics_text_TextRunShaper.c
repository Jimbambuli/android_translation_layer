#include <fontconfig/fontconfig.h>
#include <glib.h>
#include <pango/pangofc-font.h>

#include "../defines.h"
#include "../util.h"
#include "AndroidPaint.h"

#include "../generated_headers/android_graphics_text_TextRunShaper.h"

/* TextRunShaper.nativeShapeTextRun(): AOSP returns a minikin Layout that PositionedGlyphs reads glyph by glyph.
 * Here the run is shaped once (android_paint_shape_run, Pango/HarfBuzz) and everything PositionedGlyphs needs is
 * handed back in one Object[]:
 *   [0] int[]    glyph ids (in the glyph's font)
 *   [1] float[]  x, y of each glyph relative to the run's origin
 *   [2] float[]  total advance, ascent (negative), descent of the run
 *   [3] int[]    index of each glyph's font in the font arrays
 *   [4] String[] font family name (as Pango/fontconfig know it)
 *   [5] String[] font file (null if unknown)
 *   [6] int[]    per font: weight, slant (0 upright, 1 italic), ttc index, fake bold, fake italic
 */
static void font_info(PangoFont *font, char **file, int *ttc_index, bool *fake_bold, bool *fake_italic)
{
	*file = NULL;
	*ttc_index = 0;
	*fake_bold = false;
	*fake_italic = false;
	if (!PANGO_IS_FC_FONT(font))
		return;
	FcPattern *pattern = pango_fc_font_get_pattern(PANGO_FC_FONT(font));
	FcChar8 *path;
	if (FcPatternGetString(pattern, FC_FILE, 0, &path) == FcResultMatch)
		*file = (char *)path;
	int index;
	if (FcPatternGetInteger(pattern, FC_INDEX, 0, &index) == FcResultMatch)
		*ttc_index = index & 0xffff; /* the upper bits select a named instance of a variable font */
	FcBool embolden;
	if (FcPatternGetBool(pattern, FC_EMBOLDEN, 0, &embolden) == FcResultMatch)
		*fake_bold = embolden;
	FcMatrix *matrix;
	if (FcPatternGetMatrix(pattern, FC_MATRIX, 0, &matrix) == FcResultMatch)
		*fake_italic = matrix->xy != 0; /* the oblique shear fontconfig adds for a missing italic */
}

static jobject shape(JNIEnv *env, const jchar *text, jint len, jint start, jint end, jboolean is_rtl, struct AndroidPaint *paint)
{
	GArray *glyphs = g_array_new(FALSE, FALSE, sizeof(struct android_shaped_glyph));
	float total;
	android_paint_shape_run(paint, text, len, start, end, is_rtl, NULL, glyphs, &total);

	GPtrArray *fonts = g_ptr_array_new();
	jint *ids = g_new(jint, glyphs->len + 1);
	jint *font_index = g_new(jint, glyphs->len + 1);
	jfloat *pos = g_new(jfloat, glyphs->len * 2 + 1);
	for (guint i = 0; i < glyphs->len; i++) {
		struct android_shaped_glyph *g = &g_array_index(glyphs, struct android_shaped_glyph, i);
		guint index;
		if (!g_ptr_array_find(fonts, g->font, &index)) {
			index = fonts->len;
			g_ptr_array_add(fonts, g->font);
		}
		ids[i] = g->id;
		font_index[i] = index;
		pos[i * 2] = g->x;
		pos[i * 2 + 1] = g->y;
	}

	/* the run's extent: the highest ascent and lowest descent of its fonts (the paint's font for no glyphs) */
	float ascent = 0, descent = 0;
	PangoFont *paint_font = fonts->len ? NULL : pango_context_load_font(atl_pango_context(), paint->font);
	for (guint i = 0; i < (fonts->len ? fonts->len : (paint_font ? 1 : 0)); i++) {
		PangoFontMetrics *metrics = pango_font_get_metrics(fonts->len ? fonts->pdata[i] : paint_font, NULL);
		ascent = MIN(ascent, -(float)pango_font_metrics_get_ascent(metrics) / PANGO_SCALE);
		descent = MAX(descent, (float)pango_font_metrics_get_descent(metrics) / PANGO_SCALE);
		pango_font_metrics_unref(metrics);
	}
	if (paint_font)
		g_object_unref(paint_font);

	jclass string_class = (*env)->FindClass(env, "java/lang/String");
	jobjectArray families = (*env)->NewObjectArray(env, fonts->len, string_class, NULL);
	jobjectArray files = (*env)->NewObjectArray(env, fonts->len, string_class, NULL);
	jint *styles = g_new(jint, fonts->len * 5 + 1);
	for (guint i = 0; i < fonts->len; i++) {
		PangoFontDescription *desc = pango_font_describe(fonts->pdata[i]);
		const char *family = pango_font_description_get_family(desc);
		if (family) {
			jstring family_str = (*env)->NewStringUTF(env, family);
			(*env)->SetObjectArrayElement(env, families, i, family_str);
			(*env)->DeleteLocalRef(env, family_str);
		}
		char *file;
		int ttc_index;
		bool fake_bold, fake_italic;
		font_info(fonts->pdata[i], &file, &ttc_index, &fake_bold, &fake_italic);
		if (file) {
			jstring file_str = (*env)->NewStringUTF(env, file);
			(*env)->SetObjectArrayElement(env, files, i, file_str);
			(*env)->DeleteLocalRef(env, file_str);
		}
		styles[i * 5 + 0] = pango_font_description_get_weight(desc);
		styles[i * 5 + 1] = pango_font_description_get_style(desc) != PANGO_STYLE_NORMAL;
		styles[i * 5 + 2] = ttc_index;
		styles[i * 5 + 3] = fake_bold;
		styles[i * 5 + 4] = fake_italic;
		pango_font_description_free(desc);
	}

	jintArray ids_arr = (*env)->NewIntArray(env, glyphs->len);
	(*env)->SetIntArrayRegion(env, ids_arr, 0, glyphs->len, ids);
	jfloatArray pos_arr = (*env)->NewFloatArray(env, glyphs->len * 2);
	(*env)->SetFloatArrayRegion(env, pos_arr, 0, glyphs->len * 2, pos);
	jfloatArray metrics_arr = (*env)->NewFloatArray(env, 3);
	(*env)->SetFloatArrayRegion(env, metrics_arr, 0, 3, (jfloat[]){total, ascent, descent});
	jintArray font_index_arr = (*env)->NewIntArray(env, glyphs->len);
	(*env)->SetIntArrayRegion(env, font_index_arr, 0, glyphs->len, font_index);
	jintArray styles_arr = (*env)->NewIntArray(env, fonts->len * 5);
	(*env)->SetIntArrayRegion(env, styles_arr, 0, fonts->len * 5, styles);

	jobjectArray result = (*env)->NewObjectArray(env, 7, (*env)->FindClass(env, "java/lang/Object"), NULL);
	(*env)->SetObjectArrayElement(env, result, 0, ids_arr);
	(*env)->SetObjectArrayElement(env, result, 1, pos_arr);
	(*env)->SetObjectArrayElement(env, result, 2, metrics_arr);
	(*env)->SetObjectArrayElement(env, result, 3, font_index_arr);
	(*env)->SetObjectArrayElement(env, result, 4, families);
	(*env)->SetObjectArrayElement(env, result, 5, files);
	(*env)->SetObjectArrayElement(env, result, 6, styles_arr);

	for (guint i = 0; i < glyphs->len; i++)
		g_object_unref(g_array_index(glyphs, struct android_shaped_glyph, i).font);
	g_array_free(glyphs, TRUE);
	g_ptr_array_free(fonts, TRUE);
	g_free(ids);
	g_free(font_index);
	g_free(pos);
	g_free(styles);
	return result;
}

JNIEXPORT jobjectArray JNICALL Java_android_graphics_text_TextRunShaper_nativeShapeTextRun(JNIEnv *env, jclass clazz, jcharArray text_arr, jint start, jint count, jint context_start, jint context_count, jboolean is_rtl, jlong paint_ptr)
{
	if (context_count < 0 || count < 0)
		return shape(env, NULL, 0, 0, 0, is_rtl, _PTR(paint_ptr));
	jchar *text = g_new(jchar, context_count + 1);
	(*env)->GetCharArrayRegion(env, text_arr, context_start, context_count, text);
	jobjectArray result = shape(env, text, context_count, start - context_start, start - context_start + count, is_rtl, _PTR(paint_ptr));
	g_free(text);
	return result;
}
