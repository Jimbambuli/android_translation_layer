#include <gdk/wayland/gdkwayland.h>
#include <gtk/gtk.h>
#include <string.h>
#include <wayland-client.h>

#include "../defines.h"
#include "../util.h"

#include "android_view_Display.h"

/* The rotation of the display (Display.getRotation()) is the transform the compositor gives the monitor the
 * window is on: when a phone shell turns the screen (Phosh with iio-sensor-proxy, the rotation button), the
 * output's wl_output transform changes. GDK does not tell the transform, so ATL binds the wl_outputs itself
 * and matches them to GDK's monitors by their name (the connector). */

struct output {
	struct wl_output *wl_output;
	uint32_t global_name;
	char *name;
	int32_t transform;
	int32_t pending_transform;
};

static GPtrArray *outputs = NULL;
static GdkSurface *window_surface = NULL;
static int current_rotation = 0;

/* wl_output.transform is the rotation counterclockwise the compositor applies to the content; Android's
 * rotation (Surface.ROTATION_*) is the rotation of the drawn content clockwise ("if the device is rotated 90
 * degrees counter-clockwise, to compensate rendering will be rotated by 90 degrees clockwise and thus the
 * returned value here will be Surface.ROTATION_90") */
static int rotation_from_transform(int32_t transform)
{
	switch (transform & 3) { // the flipped transforms rotate like the plain ones
		case WL_OUTPUT_TRANSFORM_90:
			return 3; // ROTATION_270
		case WL_OUTPUT_TRANSFORM_180:
			return 2; // ROTATION_180
		case WL_OUTPUT_TRANSFORM_270:
			return 1; // ROTATION_90
		default:
			return 0; // ROTATION_0
	}
}

static struct output *output_for_monitor(GdkMonitor *monitor)
{
	if (!outputs || !outputs->len)
		return NULL;
	const char *connector = monitor ? gdk_monitor_get_connector(monitor) : NULL;
	for (guint i = 0; connector && i < outputs->len; i++) {
		struct output *o = g_ptr_array_index(outputs, i);
		if (o->name && !strcmp(o->name, connector))
			return o;
	}
	return outputs->len == 1 ? g_ptr_array_index(outputs, 0) : NULL;
}

static void update_rotation(void)
{
	GdkMonitor *monitor = NULL;
	if (window_surface)
		monitor = gdk_display_get_monitor_at_surface(gdk_surface_get_display(window_surface), window_surface);
	if (!monitor)
		monitor = g_list_model_get_item(gdk_display_get_monitors(gdk_display_get_default()), 0);
	else
		g_object_ref(monitor);
	struct output *o = output_for_monitor(monitor);
	if (monitor)
		g_object_unref(monitor);
	int rotation = o ? rotation_from_transform(o->transform) : 0;
	if (rotation == current_rotation)
		return;
	current_rotation = rotation;
	JNIEnv *env = get_jni_env();
	J__Display__setRotation(env, rotation);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

static void output_geometry(void *data, struct wl_output *wl_output, int32_t x, int32_t y, int32_t physical_width, int32_t physical_height,
                            int32_t subpixel, const char *make, const char *model, int32_t transform)
{
	((struct output *)data)->pending_transform = transform;
}

static void output_mode(void *data, struct wl_output *wl_output, uint32_t flags, int32_t width, int32_t height, int32_t refresh) {}

static void output_done(void *data, struct wl_output *wl_output)
{
	struct output *o = data;
	o->transform = o->pending_transform;
	update_rotation();
}

static void output_scale(void *data, struct wl_output *wl_output, int32_t factor) {}

static void output_name(void *data, struct wl_output *wl_output, const char *name)
{
	struct output *o = data;
	g_free(o->name);
	o->name = g_strdup(name);
}

static void output_description(void *data, struct wl_output *wl_output, const char *description) {}

static const struct wl_output_listener output_listener = {
	.geometry = output_geometry,
	.mode = output_mode,
	.done = output_done,
	.scale = output_scale,
	.name = output_name,
	.description = output_description,
};

static void registry_global(void *data, struct wl_registry *registry, uint32_t name, const char *interface, uint32_t version)
{
	if (strcmp(interface, wl_output_interface.name))
		return;
	struct output *o = g_new0(struct output, 1);
	o->global_name = name;
	o->wl_output = wl_registry_bind(registry, name, &wl_output_interface, MIN(version, 4));
	wl_output_add_listener(o->wl_output, &output_listener, o);
	g_ptr_array_add(outputs, o);
}

static void registry_global_remove(void *data, struct wl_registry *registry, uint32_t name)
{
	for (guint i = 0; i < outputs->len; i++) {
		struct output *o = g_ptr_array_index(outputs, i);
		if (o->global_name == name) {
			if (wl_output_get_version(o->wl_output) >= WL_OUTPUT_RELEASE_SINCE_VERSION)
				wl_output_release(o->wl_output);
			else
				wl_output_destroy(o->wl_output);
			g_free(o->name);
			g_ptr_array_remove_index(outputs, i);
			g_free(o);
			update_rotation();
			return;
		}
	}
}

static const struct wl_registry_listener registry_listener = {
	.global = registry_global,
	.global_remove = registry_global_remove,
};

static void enter_monitor_cb(GdkSurface *surface, GdkMonitor *monitor, gpointer user_data)
{
	update_rotation();
}

/* follow the rotation of the monitor of the app's window */
void display_track_rotation(GtkWindow *window)
{
	GdkDisplay *display = gtk_widget_get_display(GTK_WIDGET(window));
	if (!GDK_IS_WAYLAND_DISPLAY(display))
		return;
	GdkSurface *surface = gtk_native_get_surface(GTK_NATIVE(window));
	if (surface && surface != window_surface) {
		window_surface = surface;
		g_signal_connect(surface, "enter-monitor", G_CALLBACK(enter_monitor_cb), NULL);
	}
	if (!outputs) {
		outputs = g_ptr_array_new();
		struct wl_registry *registry = wl_display_get_registry(gdk_wayland_display_get_wl_display(display));
		wl_registry_add_listener(registry, &registry_listener, NULL);
		/* the outputs and their transforms before the app asks */
		wl_display_roundtrip(gdk_wayland_display_get_wl_display(display));
		wl_display_roundtrip(gdk_wayland_display_get_wl_display(display));
	}
	update_rotation();
}
