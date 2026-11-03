#include <gtk/gtk.h>

#include "defines.h"
#include "util.h"

#include "generated_headers/android_view_Window.h"

JNIEXPORT void JNICALL Java_android_view_Window_set_1title(JNIEnv *env, jobject this, jlong window, jstring title_jstr)
{
	GtkWindow *gtk_window = GTK_WINDOW(_PTR(window));
	const char *title = (*env)->GetStringUTFChars(env, title_jstr, NULL);
	gtk_window_set_title(gtk_window, title);
	(*env)->ReleaseStringUTFChars(env, title_jstr, title);
}

// FIXME put this in a header file
struct input_queue {
	int fd;
	GtkEventController *controller;
};

JNIEXPORT void JNICALL Java_android_view_Window_take_1input_1queue(JNIEnv *env, jobject this, jlong native_window, jobject callback, jobject queue)
{
	GtkWidget *window = _PTR(native_window);
	printf("in Java_android_view_Window_take_1input_1queue\n");

	GtkEventController *controller = GTK_EVENT_CONTROLLER(gtk_event_controller_legacy_new());
	gtk_widget_add_controller(window, controller);

	struct input_queue *input_queue = malloc(sizeof(struct input_queue));
	input_queue->fd = -1;
	input_queue->controller = controller;

	_SET_LONG_FIELD(queue, "native_ptr", _INTPTR(input_queue));

	// we need to keep these for later, so they can be called after OnCreate finishes
	g_object_set_data(G_OBJECT(window), "input_queue_callback", (gpointer)_REF(callback));
	g_object_set_data(G_OBJECT(window), "input_queue", (gpointer)_REF(queue));
}

JNIEXPORT void JNICALL Java_android_view_Window_set_1layout(JNIEnv *env, jobject this, jlong window, jint width, jint height)
{
	GtkWindow *gtk_window = GTK_WINDOW(_PTR(window));
	if (width > 0 && height > 0)
		gtk_window_set_default_size(gtk_window, width, height);
}

JNIEXPORT void JNICALL Java_android_view_Window_set_1jobject(JNIEnv *env, jclass this, jlong window, jobject window_jobj)
{
	g_object_set_data(G_OBJECT(window), "jobject", _WEAK_REF(window_jobj));
}

JNIEXPORT void JNICALL Java_android_view_Window_remove_1gtk_1background(JNIEnv *env, jobject this, jlong window)
{
	gtk_widget_add_css_class(GTK_WIDGET(_PTR(window)), "ATL-no-background");
}

#define FLOAT_TO_POINTER(f) GINT_TO_POINTER(*(uint32_t *)(&f))
#define POINTER_TO_FLOAT(p) (*((float *)(&p)))

void set_brightness_done(GObject *source_object, GAsyncResult *res, gpointer data)
{
	float brightness = POINTER_TO_FLOAT(data);
	GVariant *result = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source_object), res, NULL);
	if (result) {
		g_variant_unref(result);
	} else { // GNOME settings daemon not available. Try fallback to sysfs
		GDir *dir = g_dir_open("/sys/class/backlight", 0, NULL);
		if (!dir)
			return;

		const gchar *name;
		gchar *path;
		FILE *fp;
		while ((name = g_dir_read_name(dir))) {
			path = g_build_filename("/sys/class/backlight/", name, "/max_brightness", NULL);
			fp = fopen(path, "r");
			if (!fp) {
				g_printerr("Failed to read %s: %s\n", path, g_strerror(errno));
				g_free(path);
				continue;
			}
			g_free(path);
			int max = 0;
			fscanf(fp, "%d", &max);
			fclose(fp);

			path = g_build_filename("/sys/class/backlight/", name, "/brightness", NULL);
			fp = fopen(path, "w");
			if (!fp) {
				g_printerr("Failed to write %s: %s\n", path, g_strerror(errno));
				g_free(path);
				continue;
			}
			g_free(path);
			fprintf(fp, "%d", (int)(brightness * max));
			fclose(fp);
		}

		g_dir_close(dir);
	}
}

JNIEXPORT void JNICALL Java_android_view_Window_set_1screen_1brightness(JNIEnv *env, jobject this, jfloat brightness)
{
	GDBusConnection *connection = g_bus_get_sync(G_BUS_TYPE_SESSION, NULL, NULL);
	if (!connection)
		return;

	g_dbus_connection_call(connection, "org.gnome.SettingsDaemon.Power", "/org/gnome/SettingsDaemon/Power", "org.freedesktop.DBus.Properties", "Set",
	                       g_variant_new("(ssv)", "org.gnome.SettingsDaemon.Power.Screen", "Brightness", g_variant_new_int32(brightness * 100)),
	                       NULL, G_DBUS_CALL_FLAGS_NONE, -1, NULL, set_brightness_done, FLOAT_TO_POINTER(brightness));

	g_object_unref(connection);
}

/* Files dragged from the desktop onto the window, as Android drag events (Window.handleDragEvent(), which
 * plays AOSP's ViewRootImpl and window manager). The drop target loads the file list while the drag is over
 * the window (preload), so ACTION_DRAG_STARTED knows the files' types like a drag from AOSP's Files app. In
 * the Flatpak, GTK passes the files through the document portal. */
enum {
	DRAG_STARTED = 1, DRAG_LOCATION = 2, DROP = 3, DRAG_ENDED = 4, DRAG_EXITED = 6,
};

struct drop_state {
	jobject window;		/* android.view.Window */
	gboolean inside;	/* the drag is over the window */
	gboolean started;	/* ACTION_DRAG_STARTED went out */
	double x, y;
};

static jboolean send_drag_event(struct drop_state *state, int action, double x, double y, const GValue *value, gboolean drop_result)
{
	JNIEnv *env = get_jni_env();
	jobjectArray paths = NULL;
	if (value && G_VALUE_HOLDS(value, GDK_TYPE_FILE_LIST)) {
		GSList *files = gdk_file_list_get_files(g_value_get_boxed(value));
		paths = (*env)->NewObjectArray(env, g_slist_length(files), (*env)->FindClass(env, "java/lang/String"), NULL);
		int i = 0;
		for (GSList *l = files; l; l = l->next) {
			char *path = g_file_get_path(l->data);	/* NULL for a file that is not local */
			if (path)
				(*env)->SetObjectArrayElement(env, paths, i++, _JSTRING(path));
			g_free(path);
		}
		g_slist_free(files);
		if (i == 0)
			return JNI_FALSE;
		if (i < (*env)->GetArrayLength(env, paths)) { /* only the local files */
			jobjectArray all = paths;
			paths = (*env)->NewObjectArray(env, i, (*env)->FindClass(env, "java/lang/String"), NULL);
			for (int k = 0; k < i; k++)
				(*env)->SetObjectArrayElement(env, paths, k, (*env)->GetObjectArrayElement(env, all, k));
		}
	}
	jboolean ret = (*env)->CallBooleanMethod(env, state->window, _METHOD(_CLASS(state->window), "handleDragEvent", "(IFF[Ljava/lang/String;Z)Z"),
	                                         action, (jfloat)x, (jfloat)y, paths, drop_result);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		ret = JNI_FALSE;
	}
	return ret;
}

/* ACTION_DRAG_STARTED once the files are known, then the location */
static void start_drag(GtkDropTarget *target, struct drop_state *state)
{
	const GValue *value = gtk_drop_target_get_value(target);
	if (state->started || !state->inside || !value)
		return;
	state->started = TRUE;
	send_drag_event(state, DRAG_STARTED, state->x, state->y, value, FALSE);
	send_drag_event(state, DRAG_LOCATION, state->x, state->y, NULL, FALSE);
}

static void on_drop_value(GtkDropTarget *target, GParamSpec *pspec, struct drop_state *state)
{
	start_drag(target, state);
}

static GdkDragAction on_drop_enter(GtkDropTarget *target, double x, double y, struct drop_state *state)
{
	state->inside = TRUE;
	state->started = FALSE;
	state->x = x;
	state->y = y;
	start_drag(target, state);
	return GDK_ACTION_COPY;
}

static GdkDragAction on_drop_motion(GtkDropTarget *target, double x, double y, struct drop_state *state)
{
	state->x = x;
	state->y = y;
	if (state->started)
		send_drag_event(state, DRAG_LOCATION, x, y, NULL, FALSE);
	return GDK_ACTION_COPY;
}

/* the drag left the window: GTK does not tell whether it ends elsewhere, so it ends for the app */
static void on_drop_leave(GtkDropTarget *target, struct drop_state *state)
{
	state->inside = FALSE;
	if (!state->started)
		return;
	state->started = FALSE;
	send_drag_event(state, DRAG_EXITED, 0, 0, NULL, FALSE);
	send_drag_event(state, DRAG_ENDED, 0, 0, NULL, FALSE);
}

static gboolean on_drop(GtkDropTarget *target, const GValue *value, double x, double y, struct drop_state *state)
{
	state->x = x;
	state->y = y;
	start_drag(target, state);
	if (!state->started)
		return FALSE;
	gboolean result = send_drag_event(state, DROP, x, y, value, FALSE);
	state->started = FALSE;
	state->inside = FALSE;
	send_drag_event(state, DRAG_ENDED, 0, 0, NULL, result);
	return result;
}

JNIEXPORT void JNICALL Java_android_view_Window_native_1set_1drop_1target(JNIEnv *env, jobject this, jlong decor_widget)
{
	GtkWidget *wrapper = gtk_widget_get_parent(GTK_WIDGET(_PTR(decor_widget)));
	struct drop_state *state = g_new0(struct drop_state, 1);
	state->window = _REF(this);
	GtkDropTarget *target = gtk_drop_target_new(GDK_TYPE_FILE_LIST, GDK_ACTION_COPY);
	gtk_drop_target_set_preload(target, TRUE);
	g_signal_connect(target, "notify::value", G_CALLBACK(on_drop_value), state);
	g_signal_connect(target, "enter", G_CALLBACK(on_drop_enter), state);
	g_signal_connect(target, "motion", G_CALLBACK(on_drop_motion), state);
	g_signal_connect(target, "leave", G_CALLBACK(on_drop_leave), state);
	g_signal_connect(target, "drop", G_CALLBACK(on_drop), state);
	gtk_widget_add_controller(wrapper, GTK_EVENT_CONTROLLER(target));
}
