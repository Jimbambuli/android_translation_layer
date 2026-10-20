#include <gtk/gtk.h>

#include "../defines.h"
#include "../generated_headers/android_view_WindowManagerImpl.h"
#include "../widgets/android_widget_PopupWindow.h"
#include "gdk/gdk.h"
#include "glib-object.h"

#define FIRST_SUB_WINDOW 1000
#define LAST_SUB_WINDOW  1999

extern GtkWindow *window;

/* the window shows only its view, which draws its own background and shadow (Compose's menus),
 * without the frame and padding of the GTK theme's popover */
static void add_window_style(GtkPopover *popover)
{
	static gboolean provider_added = FALSE;
	if (!provider_added) {
		GtkCssProvider *css_provider = gtk_css_provider_new();
		gtk_css_provider_load_from_string(css_provider, "popover.atl-window > contents { background: none; border: none; box-shadow: none; padding: 0; }");
		gtk_style_context_add_provider_for_display(gtk_widget_get_display(GTK_WIDGET(popover)), GTK_STYLE_PROVIDER(css_provider), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
		g_object_unref(css_provider);
		provider_added = TRUE;
	}
	gtk_widget_add_css_class(GTK_WIDGET(popover), "atl-window");
}

/* An added window is a popover in the widget of the app's window, like the popups of
 * PopupWindow: not in the current activity's page, which removes it while its views are detached
 * from the window (Compose removes its popups then), and with its top left corner at (x, y) as the
 * gravity measures them. */
JNIEXPORT void JNICALL Java_android_view_WindowManagerImpl_native_1addView(JNIEnv *env, jclass clazz, jlong widget_ptr, jint type, jint gravity, jint x, jint y, jint width, jint height)
{
	GtkWidget *widget = _PTR(widget_ptr);
	if (type < FIRST_SUB_WINDOW || type > LAST_SUB_WINDOW) {
		// TODO: handle toplevel windows properly
		printf("WARNING: non subwindow types not implemented properly in WindowManagerImpl\n");
	}
	GtkPopover *popover = GTK_POPOVER(g_object_ref(gtk_popover_new()));
	gtk_popover_set_child(popover, gtk_widget_get_parent(widget));
	add_window_style(popover);
	gtk_popover_set_autohide(popover, FALSE);
	gtk_widget_insert_before(GTK_WIDGET(popover), gtk_window_get_child(window), NULL);
	atl_popover_place_at_location(popover, gravity, x, y);
	gtk_popover_present(popover);
	gtk_popover_popup(popover);
}

JNIEXPORT void JNICALL Java_android_view_WindowManagerImpl_native_1updateViewLayout(JNIEnv *env, jclass clazz, jlong widget_ptr, jint gravity, jint x, jint y, jint width, jint height)
{
	GtkPopover *popover = GTK_POPOVER(gtk_widget_get_parent(gtk_widget_get_parent(gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr))))));
	atl_popover_place_at_location(popover, gravity, x, y);
	gtk_popover_present(popover);
}

JNIEXPORT void JNICALL Java_android_view_WindowManagerImpl_native_1removeView(JNIEnv *env, jclass clazz, jlong widget_ptr)
{
	GtkPopover *popover = GTK_POPOVER(gtk_widget_get_parent(gtk_widget_get_parent(gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr))))));
	gtk_popover_popdown(popover);
	gtk_widget_unparent(GTK_WIDGET(popover));
	gtk_popover_set_child(popover, NULL);
	g_object_unref(popover);
}
