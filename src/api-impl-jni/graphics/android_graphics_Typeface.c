#include <fontconfig/fontconfig.h>
#include <fontconfig/fcfreetype.h>
#include <gtk/gtk.h>
#include <pango/pangofc-fontmap.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_graphics_Typeface.h"

extern GtkWidget *window;

/* fontconfig is not thread-safe for changes: the configuration is shared by every font map, and Pango
 * matches in threads of its own ("[pango] fontconfig") while the main thread prepares its work. An app font
 * is therefore added on the main thread only, once per file, and the main thread's font map told then. */
static GMutex app_fonts_lock;
static GHashTable *app_fonts; /* paths of the font files added (or about to be) */

static gboolean add_app_font(gpointer path)
{
	FcConfigAppFontAddFile(FcConfigGetCurrent(), path);
	PangoFontMap *font_map = pango_cairo_font_map_get_default();
	if (PANGO_IS_FC_FONT_MAP(font_map))
		pango_fc_font_map_config_changed(PANGO_FC_FONT_MAP(font_map));
	g_free(path);
	return G_SOURCE_REMOVE;
}

/* Makes a font file of the app known to fontconfig, and so to Pango, and returns its family name.
 * style[0] gets the OpenType weight and style[1] 1 for an italic font. */
JNIEXPORT jstring JNICALL Java_android_graphics_Typeface_native_1register_1font(JNIEnv *env, jclass clazz, jstring path_jstr, jintArray style)
{
	const char *path = (*env)->GetStringUTFChars(env, path_jstr, NULL);
	int count = 0;
	FcPattern *pattern = FcFreeTypeQuery((const FcChar8 *)path, 0, NULL, &count);
	jstring family = NULL;
	if (pattern) {
		FcChar8 *name = NULL;
		int fc_weight = FC_WEIGHT_REGULAR;
		int fc_slant = FC_SLANT_ROMAN;
		FcPatternGetString(pattern, FC_FAMILY, 0, &name);
		FcPatternGetInteger(pattern, FC_WEIGHT, 0, &fc_weight);
		FcPatternGetInteger(pattern, FC_SLANT, 0, &fc_slant);
		jint values[2] = {FcWeightToOpenType(fc_weight), fc_slant != FC_SLANT_ROMAN};
		(*env)->SetIntArrayRegion(env, style, 0, 2, values);
		if (name)
			family = (*env)->NewStringUTF(env, (const char *)name);
		g_mutex_lock(&app_fonts_lock);
		if (!app_fonts)
			app_fonts = g_hash_table_new(g_str_hash, g_str_equal);
		gboolean added = g_hash_table_contains(app_fonts, path);
		if (!added)
			g_hash_table_add(app_fonts, g_strdup(path));
		g_mutex_unlock(&app_fonts_lock);
		if (!added)
			g_main_context_invoke(NULL, add_app_font, g_strdup(path));
		FcPatternDestroy(pattern);
	}
	(*env)->ReleaseStringUTFChars(env, path_jstr, path);
	return family;
}
