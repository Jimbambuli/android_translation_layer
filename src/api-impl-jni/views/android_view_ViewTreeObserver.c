#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_view_ViewTreeObserver.h"

static void on_global_layout_callback(GdkFrameClock *clock, jobject view_tree_observer)
{
	JNIEnv *env = get_jni_env();
	J__ViewTreeObserver__dispatchOnGlobalLayout(env, view_tree_observer);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

static void on_pre_draw_callback(GdkFrameClock *clock, jobject view_tree_observer)
{
	JNIEnv *env = get_jni_env();
	J__ViewTreeObserver__dispatchOnPreDraw(env, view_tree_observer);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

extern GtkWidget *window;

/* A window has a frame clock only once it is realized. Listeners added before that (the views of a
 * dialog, for instance) are connected to the clock when the window gets realized; until then the
 * observer's handle field holds the window's "realize" handler. The connection owns the global
 * reference to the observer and releases it when it is disconnected, so that an observer which got
 * no more listeners, or was killed with its window, does not stay alive with everything it holds. */
struct deferred_hook {
	jobject observer;
	const char *field;
	const char *signal;
	GCallback callback;
};

static void release_observer(gpointer observer, GClosure *closure)
{
	JNIEnv *env = get_jni_env();
	_UNREF(observer);
}

static void release_deferred_hook(gpointer data, GClosure *closure)
{
	struct deferred_hook *hook = data;
	if (hook->observer)
		release_observer(hook->observer, closure);
	g_free(hook);
}

static void on_window_realize(GtkWidget *window, struct deferred_hook *hook)
{
	JNIEnv *env = get_jni_env();
	jobject observer = hook->observer;
	const char *field = hook->field;
	const char *signal = hook->signal;
	GCallback callback = hook->callback;

	hook->observer = NULL; // now owned by the frame clock connection
	g_signal_handler_disconnect(window, _GET_LONG_FIELD(observer, field)); // frees hook
	gulong signal_handle = g_signal_connect_data(G_OBJECT(gtk_widget_get_frame_clock(window)), signal, callback, observer, release_observer, 0);
	_SET_LONG_FIELD(observer, field, signal_handle);
}

static gulong connect_to_frame_clock(GtkWidget *window, jobject observer, const char *field, const char *signal, GCallback callback)
{
	GdkFrameClock *clock = gtk_widget_get_frame_clock(window);
	if (clock)
		return g_signal_connect_data(G_OBJECT(clock), signal, callback, observer, release_observer, 0);

	struct deferred_hook *hook = g_new(struct deferred_hook, 1);
	*hook = (struct deferred_hook){observer, field, signal, callback};
	return g_signal_connect_data(window, "realize", G_CALLBACK(on_window_realize), hook, release_deferred_hook, 0);
}

static void disconnect_from_frame_clock(GtkWidget *window, gulong signal_handle)
{
	if (g_signal_handler_is_connected(window, signal_handle))
		g_signal_handler_disconnect(window, signal_handle);
	else
		g_signal_handler_disconnect(G_OBJECT(gtk_widget_get_frame_clock(window)), signal_handle);
}

void _gdb_force_java_stack_trace(void);

JNIEXPORT void JNICALL Java_android_view_ViewTreeObserver_native_1set_1have_1global_1layout_1listeners(JNIEnv *env, jobject this, jboolean have_listeners)
{
	GtkWidget *window = _PTR(_GET_LONG_FIELD(this, "window"));

	if (!window) {
		fprintf(stderr, "Java_android_view_ViewTreeObserver_native_1set_1have_1global_1layout_1listeners: no window\n");
		return;
	}

	gulong signal_handle = _GET_LONG_FIELD(this, "onGlobalLayout_signal_handle");

	if (have_listeners && !signal_handle) {
		/* GTKs own handler is always the first, so our handler gets called after all widgets have been measured and allocated */
		signal_handle = connect_to_frame_clock(window, _REF(this), "onGlobalLayout_signal_handle", "layout", G_CALLBACK(on_global_layout_callback));
		_SET_LONG_FIELD(this, "onGlobalLayout_signal_handle", signal_handle);
	} else if (!have_listeners && signal_handle) {
		disconnect_from_frame_clock(window, signal_handle);
		_SET_LONG_FIELD(this, "onGlobalLayout_signal_handle", 0);
	} else {
		fprintf(stderr, "Java_android_view_ViewTreeObserver_native_1set_1have_1global_1layout_1listeners: invalid state: have_listeners: %d, signal_handle: 0x%016w64x\n", have_listeners, signal_handle);
		exit(1);
	}
}

JNIEXPORT void JNICALL Java_android_view_ViewTreeObserver_native_1set_1have_1predraw_1listeners(JNIEnv *env, jobject this, jboolean have_listeners)
{
	GtkWidget *window = _PTR(_GET_LONG_FIELD(this, "window"));

	if (!window) {
		fprintf(stderr, "Java_android_view_ViewTreeObserver_native_1set_1have_1predraw_1listeners: no window\n");
		return;
	}

	gulong signal_handle = _GET_LONG_FIELD(this, "onPreDraw_signal_handle");

	if (have_listeners && !signal_handle) {
		/* Unlike AOSP, our hook gets invoked after all widgets have been drawn. */
		/* Switching to ::layout would fix the timing, but we would miss frames where no layout was requested. */
		/* TODO: Hook into the DecorView snapshot function to get in before widget drawing. */
		signal_handle = connect_to_frame_clock(window, _REF(this), "onPreDraw_signal_handle", "paint", G_CALLBACK(on_pre_draw_callback));
		_SET_LONG_FIELD(this, "onPreDraw_signal_handle", signal_handle);
	} else if (!have_listeners && signal_handle) {
		disconnect_from_frame_clock(window, signal_handle);
		_SET_LONG_FIELD(this, "onPreDraw_signal_handle", 0);
	} else {
		fprintf(stderr, "Java_android_view_ViewTreeObserver_native_1set_1have_1predraw_1listeners: invalid state: have_listeners: %d, signal_handle: 0x%016w64x\n", have_listeners, signal_handle);
		exit(1);
	}
}
