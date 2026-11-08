#include <gtk/gtk.h>

#include "defines.h"
#include "util.h"

#include "generated_headers/android_atl_ATLViewDump.h"

/* Renders the GtkWindow of the app (and only it) to a PNG; must run on the main thread. */
JNIEXPORT jboolean JNICALL Java_android_atl_ATLViewDump_native_1screenshot(JNIEnv *env, jclass this, jlong window, jstring path_jstr)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(window));
	int width = gtk_widget_get_width(widget);
	int height = gtk_widget_get_height(widget);
	GskRenderer *renderer = GTK_IS_NATIVE(widget) ? gtk_native_get_renderer(GTK_NATIVE(widget)) : NULL;
	if (!renderer || width <= 0 || height <= 0)
		return JNI_FALSE;

	GdkPaintable *paintable = gtk_widget_paintable_new(widget);
	GtkSnapshot *snapshot = gtk_snapshot_new();
	gdk_paintable_snapshot(paintable, snapshot, width, height);
	GskRenderNode *node = gtk_snapshot_free_to_node(snapshot);
	g_object_unref(paintable);
	if (!node)
		return JNI_FALSE;

	graphene_rect_t bounds = GRAPHENE_RECT_INIT(0, 0, width, height);
	GdkTexture *texture = gsk_renderer_render_texture(renderer, node, &bounds);
	gsk_render_node_unref(node);
	if (!texture)
		return JNI_FALSE;

	const char *path = (*env)->GetStringUTFChars(env, path_jstr, NULL);
	gboolean ok = gdk_texture_save_to_png(texture, path);
	(*env)->ReleaseStringUTFChars(env, path_jstr, path);
	g_object_unref(texture);
	return ok;
}
