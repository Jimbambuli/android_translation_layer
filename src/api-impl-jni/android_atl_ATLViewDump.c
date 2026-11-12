#include <glib/gstdio.h>
#include <gtk/gtk.h>

#include "defines.h"
#include "util.h"

#include "generated_headers/android_atl_ATLViewDump.h"

/* The toplevels of the app: the activity's window first, then the other mapped GtkWindows (dialogs). A
 * popup is no toplevel: a GtkPopover has a surface of its own, but it is a widget of the toplevel it is
 * shown on. */
static GPtrArray *toplevels(GtkWidget *window)
{
	GPtrArray *all = g_ptr_array_new();
	g_ptr_array_add(all, window);
	GList *list = gtk_window_list_toplevels();
	for (GList *l = list; l; l = l->next)
		if (l->data != window && gtk_widget_get_mapped(GTK_WIDGET(l->data)))
			g_ptr_array_add(all, l->data);
	g_list_free(list);
	return all;
}

/* the mapped GtkPopovers below the widget, a popover before the popovers shown from it */
static void collect_popovers(GtkWidget *widget, GPtrArray *out)
{
	for (GtkWidget *child = gtk_widget_get_first_child(widget); child; child = gtk_widget_get_next_sibling(child)) {
		if (!gtk_widget_get_mapped(child))
			continue;
		if (GTK_IS_POPOVER(child))
			g_ptr_array_add(out, child);
		collect_popovers(child, out);
	}
}

static void paint(GtkSnapshot *snapshot, GtkWidget *widget, float x, float y)
{
	GdkPaintable *paintable = gtk_widget_paintable_new(widget);
	gtk_snapshot_save(snapshot);
	gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(x, y));
	gdk_paintable_snapshot(paintable, snapshot, gtk_widget_get_width(widget), gtk_widget_get_height(widget));
	gtk_snapshot_restore(snapshot);
	g_object_unref(paintable);
}

/* Renders a toplevel (only it, not the screen) and the popups shown on it to a PNG. A popup is placed where
 * its surface is on the toplevel's surface; what of it lies outside the toplevel is cut off. */
static gboolean render(GtkWidget *toplevel, const char *path)
{
	int width = gtk_widget_get_width(toplevel);
	int height = gtk_widget_get_height(toplevel);
	GskRenderer *renderer = gtk_native_get_renderer(GTK_NATIVE(toplevel));
	if (!renderer || width <= 0 || height <= 0)
		return FALSE;

	GtkSnapshot *snapshot = gtk_snapshot_new();
	paint(snapshot, toplevel, 0, 0);
	double tx, ty;
	gtk_native_get_surface_transform(GTK_NATIVE(toplevel), &tx, &ty);
	GPtrArray *popovers = g_ptr_array_new();
	collect_popovers(toplevel, popovers);
	for (guint i = 0; i < popovers->len; i++) {
		GtkWidget *popover = g_ptr_array_index(popovers, i);
		graphene_point_t origin;
		if (atl_compute_point_in_window(popover, &GRAPHENE_POINT_INIT(0, 0), &origin))
			paint(snapshot, popover, origin.x - tx, origin.y - ty);
	}
	g_ptr_array_free(popovers, TRUE);
	GskRenderNode *node = gtk_snapshot_free_to_node(snapshot);
	if (!node)
		return FALSE;

	graphene_rect_t bounds = GRAPHENE_RECT_INIT(0, 0, width, height);
	GdkTexture *texture = gsk_renderer_render_texture(renderer, node, &bounds);
	gsk_render_node_unref(node);
	if (!texture)
		return FALSE;

	char *tmp = g_strconcat(path, ".tmp", NULL);
	gboolean ok = gdk_texture_save_to_png(texture, tmp) && g_rename(tmp, path) == 0;
	g_free(tmp);
	g_object_unref(texture);
	return ok;
}

/* Renders each toplevel with its popups: the window to <prefix>.png, toplevel N to <prefix>.N.png; must run
 * on the main thread. Returns how many images were written. */
JNIEXPORT jint JNICALL Java_android_atl_ATLViewDump_native_1screenshot(JNIEnv *env, jclass this, jlong window, jstring prefix_jstr)
{
	const char *prefix = (*env)->GetStringUTFChars(env, prefix_jstr, NULL);
	GPtrArray *all = toplevels(GTK_WIDGET(_PTR(window)));
	jint written = 0;
	for (guint i = 0; i < all->len; i++) {
		char *path = i ? g_strdup_printf("%s.%u.png", prefix, i) : g_strconcat(prefix, ".png", NULL);
		written += render(g_ptr_array_index(all, i), path);
		g_free(path);
	}
	g_ptr_array_free(all, TRUE);
	(*env)->ReleaseStringUTFChars(env, prefix_jstr, prefix);
	return written;
}

static int toplevel_index(GtkWidget *window, GtkWidget *widget)
{
	GPtrArray *all = toplevels(window);
	guint index;
	gboolean found = g_ptr_array_find(all, gtk_widget_get_root(widget), &index);
	g_ptr_array_free(all, TRUE);
	return found ? (int)index : -1;
}

JNIEXPORT jint JNICALL Java_android_atl_ATLViewDump_native_1toplevel(JNIEnv *env, jclass this, jlong window, jlong widget)
{
	return toplevel_index(GTK_WIDGET(_PTR(window)), GTK_WIDGET(_PTR(widget)));
}

/* the size of the toplevel's surface and where the toplevel's widget (and its image) begins in it */
static void surface_attributes(GString *out, GtkWidget *window, int index)
{
	GPtrArray *all = toplevels(window);
	if (index >= 0 && (guint)index < all->len) {
		GtkNative *native = GTK_NATIVE(g_ptr_array_index(all, index));
		GdkSurface *surface = gtk_native_get_surface(native);
		double tx, ty;
		gtk_native_get_surface_transform(native, &tx, &ty);
		if (surface)
			g_string_append_printf(out, " atl-surface-size=\"%dx%d\" atl-surface-offset=\"%g,%g\"",
			                       gdk_surface_get_width(surface), gdk_surface_get_height(surface), tx, ty);
	}
	g_ptr_array_free(all, TRUE);
}

JNIEXPORT jstring JNICALL Java_android_atl_ATLViewDump_native_1surface(JNIEnv *env, jclass this, jlong window, jint toplevel)
{
	GString *out = g_string_new("");
	surface_attributes(out, GTK_WIDGET(_PTR(window)), toplevel);
	jstring ret = _JSTRING(out->str);
	g_string_free(out, TRUE);
	return ret;
}

static gboolean clickable_role(GtkAccessibleRole role)
{
	switch (role) {
		case GTK_ACCESSIBLE_ROLE_BUTTON:
		case GTK_ACCESSIBLE_ROLE_CHECKBOX:
		case GTK_ACCESSIBLE_ROLE_LIST_ITEM:
		case GTK_ACCESSIBLE_ROLE_MENU_ITEM:
		case GTK_ACCESSIBLE_ROLE_MENU_ITEM_CHECKBOX:
		case GTK_ACCESSIBLE_ROLE_MENU_ITEM_RADIO:
		case GTK_ACCESSIBLE_ROLE_RADIO:
		case GTK_ACCESSIBLE_ROLE_SWITCH:
			return TRUE;
		default:
			return FALSE;
	}
}

static void append_escaped(GString *out, const char *s)
{
	char *e = g_markup_escape_text(s ? s : "", -1);
	g_string_append(out, e);
	g_free(e);
}

/* a GTK widget as a node in the form of ATLViewDump.node(): bounds in the toplevel's surface, the GTK type
 * as class, a label's text; popups shown from it are windows of their own */
static void gtk_node(GString *out, GtkWidget *widget, int index, int depth)
{
	graphene_point_t a, b;
	if (!atl_compute_point_in_window(widget, &GRAPHENE_POINT_INIT(0, 0), &a) ||
	    !atl_compute_point_in_window(widget, &GRAPHENE_POINT_INIT(gtk_widget_get_width(widget), gtk_widget_get_height(widget)), &b))
		return;
	char *bounds = g_strdup_printf("[%d,%d][%d,%d]", (int)a.x, (int)a.y, (int)b.x, (int)b.y);
	const char *type = G_OBJECT_TYPE_NAME(widget);
	gboolean clickable = clickable_role(gtk_accessible_get_accessible_role(GTK_ACCESSIBLE(widget)));
	g_string_append_printf(out, "%*s<node index=\"%d\" text=\"", depth * 2, "", index);
	append_escaped(out, GTK_IS_LABEL(widget) ? gtk_label_get_text(GTK_LABEL(widget)) : NULL);
	g_string_append_printf(out, "\" resource-id=\"\" class=\"%s\" content-desc=\"\" checkable=\"false\" checked=\"false\""
	                            " clickable=\"%s\" enabled=\"%s\" focusable=\"%s\" selected=\"false\" bounds=\"%s\""
	                            " atl-class=\"%s\" atl-visibility=\"visible\" atl-alpha=\"%g\" atl-shown=\"true\""
	                            " atl-gtk-rect=\"true\" atl-jbounds=\"%s\"",
	                       type, clickable ? "true" : "false", gtk_widget_is_sensitive(widget) ? "true" : "false",
	                       gtk_widget_get_focusable(widget) ? "true" : "false", bounds, type,
	                       gtk_widget_get_opacity(widget), bounds);
	g_free(bounds);
	int n = 0;
	for (GtkWidget *child = gtk_widget_get_first_child(widget); child; child = gtk_widget_get_next_sibling(child)) {
		if (!gtk_widget_get_mapped(child) || GTK_IS_NATIVE(child))
			continue;
		g_string_append(out, n ? "" : ">\n");
		gtk_node(out, child, n++, depth + 1);
	}
	if (n)
		g_string_append_printf(out, "%*s</node>\n", depth * 2, "");
	else
		g_string_append(out, " />\n");
}

static void gtk_window(GString *out, GtkWidget *window, GtkWidget *native, guint toplevel)
{
	g_string_append_printf(out, "<window atl-what=\"gtk %s\" atl-toplevel=\"%u\"", G_OBJECT_TYPE_NAME(native), toplevel);
	surface_attributes(out, window, toplevel);
	g_string_append(out, ">\n");
	gtk_node(out, native, 0, 1);
	g_string_append(out, "</window>\n");
}

/* <window atl-what="gtk TYPE"> for each mapped GtkPopover of the app that is not the native of one of the
 * given widgets (whose views ATLViewDump writes itself): PopupMenu's GtkPopoverMenu, a Spinner's list; and
 * for each further toplevel that none of the widgets is in (ATL's AlertDialog shows GTK widgets) */
JNIEXPORT jstring JNICALL Java_android_atl_ATLViewDump_native_1gtkPopups(JNIEnv *env, jclass this, jlong window_ptr, jlongArray roots_jarr)
{
	GtkWidget *window = GTK_WIDGET(_PTR(window_ptr));
	jsize n_roots = (*env)->GetArrayLength(env, roots_jarr);
	jlong *roots = (*env)->GetLongArrayElements(env, roots_jarr, NULL);
	GPtrArray *known = g_ptr_array_new();
	GPtrArray *covered = g_ptr_array_new();
	for (jsize i = 0; i < n_roots; i++) {
		g_ptr_array_add(known, gtk_widget_get_native(GTK_WIDGET(_PTR(roots[i]))));
		g_ptr_array_add(covered, gtk_widget_get_root(GTK_WIDGET(_PTR(roots[i]))));
	}
	(*env)->ReleaseLongArrayElements(env, roots_jarr, roots, JNI_ABORT);

	GString *out = g_string_new("");
	GPtrArray *all = toplevels(window);
	for (guint t = 0; t < all->len; t++) {
		if (t > 0 && !g_ptr_array_find(covered, g_ptr_array_index(all, t), NULL))
			gtk_window(out, window, g_ptr_array_index(all, t), t);
		GPtrArray *popovers = g_ptr_array_new();
		collect_popovers(g_ptr_array_index(all, t), popovers);
		for (guint i = 0; i < popovers->len; i++) {
			GtkWidget *popover = g_ptr_array_index(popovers, i);
			if (!g_ptr_array_find(known, popover, NULL))
				gtk_window(out, window, popover, t);
		}
		g_ptr_array_free(popovers, TRUE);
	}
	g_ptr_array_free(all, TRUE);
	g_ptr_array_free(known, TRUE);
	g_ptr_array_free(covered, TRUE);
	jstring ret = _JSTRING(out->str);
	g_string_free(out, TRUE);
	return ret;
}
