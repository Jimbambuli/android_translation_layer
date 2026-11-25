#ifndef _UTILS_H_
#define _UTILS_H_

#include <gtk/gtk.h>

#include <jni.h>

#include "defines.h"
#include "upcalls.h"

extern JavaVM *jvm;

JNIEnv *get_jni_env(void);

const char *attribute_set_get_string(JNIEnv *env, jobject attrs, char *attribute, char *schema);
int attribute_set_get_int(JNIEnv *env, jobject attrs, char *attribute, char *schema, int default_value);
void extract_from_apk(const char *path, const char *target);
char *get_app_data_dir();

void prepare_main_looper(JNIEnv *env);

/* we don't (currently?) install the headers for liblog */
typedef enum {
	LOG_ID_MAIN = 0,
	LOG_ID_RADIO = 1,
	LOG_ID_EVENTS = 2,
	LOG_ID_SYSTEM = 3,

	LOG_ID_MAX
} log_id_t;

typedef enum android_LogPriority {
	ANDROID_LOG_UNKNOWN = 0,
	ANDROID_LOG_DEFAULT, /* only for SetMinPriority() */
	ANDROID_LOG_VERBOSE,
	ANDROID_LOG_DEBUG,
	ANDROID_LOG_INFO,
	ANDROID_LOG_WARN,
	ANDROID_LOG_ERROR,
	ANDROID_LOG_FATAL,
	ANDROID_LOG_SILENT, /* only for SetMinPriority(); must be last */
} android_LogPriority;

/* TODO: do we really need the bufID, or can we use our function below which has a stderr fallback */
int __android_log_buf_write(int bufID, int prio, const char *tag, const char *text);

/* defined in util.c */
int android_log_printf(android_LogPriority prio, const char *tag, const char *fmt, ...);
gboolean atl_log_below_info_wanted(void);

void *get_nio_buffer(JNIEnv *env, jobject buffer, jarray *array_ref, jbyte **array);
void release_nio_buffer(JNIEnv *env, jarray array_ref, jbyte *array);
int get_nio_buffer_size(JNIEnv *env, jobject buffer);

void atl_remember_user_locale(void);
void atl_use_user_messages_locale(void);
const char *atl_gtk_text(const char *msgid);
char *atl_framework_string(JNIEnv *env, jobject context, const char *name);

void atl_ensure_widget_snapshotability(GtkWidget *widget);
void atl_safe_gtk_label_set_text(GtkLabel *label, const char *str);
void atl_safe_gtk_widget_set_visible(GtkWidget *widget, gboolean visible);
void atl_safe_gtk_widget_queue_allocate(GtkWidget *widget);
void atl_safe_gtk_widget_queue_resize(GtkWidget *widget);
void atl_unref_on_main_thread(gpointer object);
void atl_paintable_invalidate(GdkPaintable *paintable, gboolean size);
PangoContext *atl_pango_context(void);

/* What render and worker threads need of GTK's display (doc/threading.md): taken on the main thread once the window
 * exists (atl_display_info_init()), unchanged afterwards, so any thread may read it without a GTK call. */
struct wl_display;
struct wl_compositor;
struct atl_display_info {
	gboolean is_wayland;
	gboolean is_x11;
	struct wl_display *wl_display;
	struct wl_compositor *wl_compositor;
	void *x11_display;
	double pango_resolution; /* of the window's PangoContext, for the PangoContexts of other threads */
	cairo_font_options_t *pango_font_options;
};
extern struct atl_display_info atl_display_info;
void atl_display_info_init(GtkWidget *window);
PangoContext *atl_pango_context_hinted(void);
gboolean atl_pango_layout_placeholder(PangoLayout *layout, guint index, float place[4]);
gboolean atl_compute_point_in_window(GtkWidget *widget, const graphene_point_t *point_in, graphene_point_t *point_out);
void atl_snapshot_concat_matrix(GtkSnapshot *snapshot, const graphene_matrix_t *matrix);

typedef void (*AtlFadeDone)(GtkWidget *widget);
void atl_fade_widget(GtkWidget *widget, double to, int duration_ms, AtlFadeDone done);
void atl_fade_widget_cancel(GtkWidget *widget);
void atl_window_android_tab_focus(GtkWindow *window);
void atl_window_android_wheel_acceleration(GtkWindow *window);
void atl_window_android_mouse_back(GtkWindow *window, GCallback back, gpointer data);
void atl_window_set_key_window(GtkWindow *window, GtkWindow *key_window);
/* the window gets the moves and the release of a gesture only if it got its press (InputDispatcher touch targets) */
void atl_window_android_touch_targets(GtkWindow *window);
/* another window took the press that this one has seen: the rest of the gesture is not for this window */
void atl_window_press_taken(GtkWidget *widget, GdkEvent *event);

/* a file of ATL's installation, REL relative to the directory of libtranslation_layer_main.so (meson.build makes the
 * relative paths from the install directories), so that an installation moved as a whole finds its files; an absolute
 * REL is returned unchanged. Free with g_free(). */
char *atl_install_path(const char *rel);

#define INTENT_G_VARIANT_TYPE_STRING "(sssa{sv}s)" // (action, className, data, extras, sender_dbus_name)
GVariant *intent_serialize(JNIEnv *env, jobject intent);
jobject intent_deserialize(JNIEnv *env, GVariant *variant);

#endif
