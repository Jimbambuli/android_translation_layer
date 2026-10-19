#include <fontconfig/fontconfig.h>
#include <fontconfig/fcfreetype.h>
#include <gtk/gtk.h>
#include <pango/pangofc-fontmap.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_graphics_Typeface.h"

extern GtkWidget *window;

static gboolean font_map_config_changed(gpointer user_data)
{
	PangoFontMap *font_map = pango_cairo_font_map_get_default();
	if (PANGO_IS_FC_FONT_MAP(font_map))
		pango_fc_font_map_config_changed(PANGO_FC_FONT_MAP(font_map));
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
		PangoFontMap *font_map = pango_cairo_font_map_get_default();
		FcConfig *config = PANGO_IS_FC_FONT_MAP(font_map) ? pango_fc_font_map_get_config(PANGO_FC_FONT_MAP(font_map)) : NULL;
		if (FcConfigAppFontAddFile(config, (const FcChar8 *)path)) {
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
			/* the font map belongs to the GTK main thread, fonts may be loaded on any thread */
			g_main_context_invoke(NULL, font_map_config_changed, NULL);
		}
		FcPatternDestroy(pattern);
	}
	(*env)->ReleaseStringUTFChars(env, path_jstr, path);
	return family;
}
