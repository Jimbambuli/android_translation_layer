// for dladdr
#define _GNU_SOURCE

#include <libintl.h>
#include <locale.h>
#include <math.h>
#include <dlfcn.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include <gtk/gtk.h>
#include <pango/pangocairo.h>
#ifdef GDK_WINDOWING_WAYLAND
#include <gdk/wayland/gdkwayland.h>
#endif
#ifdef GDK_WINDOWING_X11
/* from gdk/x11/gdkx.h, which would bring Xlib's names into this file */
GType gdk_x11_display_get_type(void);
void *gdk_x11_display_get_xdisplay(GdkDisplay *display);
#endif

#include "src/api-impl-jni/defines.h"
#include "util.h"
#include "app/android_app_Activity.h"
#include "widgets/WrapperWidget.h"
#include "input/VelocityControl.h"

const char *attribute_set_get_string(JNIEnv *env, jobject attrs, char *attribute, char *schema)
{
	if (!attrs)
		return NULL;

	if (!schema)
		schema = "http://schemas.android.com/apk/res/android";

	jstring string = J__AttributeSet__getAttributeValue(env, attrs, _JSTRING(schema), _JSTRING(attribute));
	return string ? _CSTRING(string) : NULL;
}

int attribute_set_get_int(JNIEnv *env, jobject attrs, char *attribute, char *schema, int default_value)
{
	if (!attrs)
		return default_value;

	if (!schema)
		schema = "http://schemas.android.com/apk/res/android";

	return J__AttributeSet__getAttributeIntValue(env, attrs, _JSTRING(schema), _JSTRING(attribute), default_value);
}

JavaVM *jvm;

// TODO: use this everywhere, not just for gdb helper functions
JNIEnv *get_jni_env(void)
{
	JNIEnv *env;
	(*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6);
	return env;
}

JNIEnv *_gdb_get_jni_env(void)
{
	return get_jni_env();
}

void _gdb_get_java_stack_trace(void)
{
	JNIEnv *env = get_jni_env();
	(*env)->ExceptionDescribe(env);
}

void _gdb_force_java_stack_trace(void)
{
	JNIEnv *env = get_jni_env();
	(*env)->ThrowNew(env, _CACHED_CLASS(Exception), "forced stack trace");
	(*env)->ExceptionDescribe(env);
	(*env)->ExceptionClear(env);
}

extern char *apk_path;
void extract_from_apk(const char *path, const char *target)
{
	JNIEnv *env = get_jni_env();
	J__AssetManager__extractFromAPK(env, _JSTRING(apk_path), _JSTRING(path), _JSTRING(target));
}

/* logging with fallback to stderr */

typedef int __android_log_vprint_type(int prio, const char *tag, const char *fmt, va_list ap);

static int fallback_verbose_log(int prio, const char *tag, const char *fmt, va_list ap)
{
	int ret;

	static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
	pthread_mutex_lock(&mutex);
	static char buf[1024];
	ret = vsnprintf(buf, sizeof(buf), fmt, ap);
	fprintf(stderr, "%w64u: %s\n", (uint64_t)pthread_self(), buf);
	pthread_mutex_unlock(&mutex);

	return ret;
}

/* liblog (fake_log_device) drops verbose and debug lines unless ANDROID_LOG_TAGS asks for them (the global
 * minimum is info), after a __android_log_is_loggable check per line (Log.println_native converts both strings
 * before that). Without ANDROID_LOG_TAGS (read once) they are dropped here; with it (apk-starter's debug sets
 * it) liblog filters as before. */
gboolean atl_log_below_info_wanted(void)
{
	static int wanted = -1;
	int w = __atomic_load_n(&wanted, __ATOMIC_RELAXED);
	if (w < 0) {
		w = getenv("ANDROID_LOG_TAGS") != NULL;
		__atomic_store_n(&wanted, w, __ATOMIC_RELAXED);
	}
	return w;
}

static int android_log_vprintf(int prio, const char *tag, const char *fmt, va_list ap)
{
	if (prio < ANDROID_LOG_INFO && !atl_log_below_info_wanted())
		return 0;

	static __android_log_vprint_type *_android_log_vprintf = NULL;
	if (!_android_log_vprintf) {
		_android_log_vprintf = dlsym(RTLD_DEFAULT, "__android_log_vprint");

		if (!_android_log_vprintf) {
			_android_log_vprintf = &fallback_verbose_log;
		}
	}

	return _android_log_vprintf(prio, tag, fmt, ap);
}

int android_log_printf(android_LogPriority prio, const char *tag, const char *fmt, ...)
{
	int ret;

	va_list ap;
	va_start(ap, fmt);

	ret = android_log_vprintf(prio, tag, fmt, ap);

	va_end(ap);

	return ret;
}

void *get_nio_buffer(JNIEnv *env, jobject buffer, jarray *array_ref, jbyte **array)
{
	jclass class;
	void *pointer;
	int elementSizeShift, position;

	if (!buffer) {
		*array_ref = NULL;
		return NULL;
	}
	class = _CLASS(buffer);
	pointer = _PTR((*env)->GetLongField(env, buffer, _FIELD_ID(class, "address", "J")));
	elementSizeShift = (*env)->GetIntField(env, buffer, _FIELD_ID(class, "_elementSizeShift", "I"));
	position = (*env)->GetIntField(env, buffer, _FIELD_ID(class, "position", "I"));
	if (pointer) { // buffer is direct
		*array_ref = NULL;
		pointer += position << elementSizeShift;
	} else { // buffer is indirect
		*array_ref = J__Buffer__array(env, buffer);
		jint offset = J__Buffer__arrayOffset(env, buffer);
		pointer = *array = (*env)->GetPrimitiveArrayCritical(env, *array_ref, NULL);
		pointer += (offset + position) << elementSizeShift;
	}
	return pointer;
}

void release_nio_buffer(JNIEnv *env, jarray array_ref, jbyte *array)
{
	if (array_ref)
		(*env)->ReleasePrimitiveArrayCritical(env, array_ref, array, 0);
}

int get_nio_buffer_size(JNIEnv *env, jobject buffer)
{
	jclass class = _CLASS(buffer);

	int limit = (*env)->GetIntField(env, buffer, _FIELD_ID(class, "limit", "I"));
	int position = (*env)->GetIntField(env, buffer, _FIELD_ID(class, "position", "I"));

	return limit - position;
}

/* Calling these functions while snapshotting will cause Gtk to not snapshot the affected widgets.
 * Below are "safe" wrappers which will postpone the calls if inside a snapshot.
 * Specifically, gtk_widget_add_tick_callback will make sure the calls are made in the next
 * Update phase. */

/* callbacks */
static gboolean queue_set_text(GtkWidget *label, GdkFrameClock *frame_clock, gpointer str)
{
	gtk_label_set_text(GTK_LABEL(label), str);
	/* we always call strdup so we always want to free */
	free(str);
	return G_SOURCE_REMOVE;
}

static gboolean queue_queue_allocate(GtkWidget *widget, GdkFrameClock *frame_clock, gpointer user_data)
{
	gtk_widget_queue_allocate(widget);
	return G_SOURCE_REMOVE;
}

static gboolean queue_queue_resize(GtkWidget *widget, GdkFrameClock *frame_clock, gpointer user_data)
{
	gtk_widget_queue_resize(widget);
	return G_SOURCE_REMOVE;
}

/* Some functions call gtk_widget_queue_allocate or similar internally.
 * To prevent that from breaking the snapshotting process, when called at the wrong time,
 * we have to follow those functions with this pile of hacks that will unset the problematic flags. */
extern int snapshot_in_progress;
void atl_ensure_widget_snapshotability(GtkWidget *widget)
{
	if (snapshot_in_progress) {
		GtkAllocation allocation;
		G_GNUC_BEGIN_IGNORE_DEPRECATIONS
		/* we probably don't need to use this deprecated function but it sure is convenient */
		gtk_widget_get_allocation(widget, &allocation);
		G_GNUC_END_IGNORE_DEPRECATIONS
		/* this clears resize request, which seems to be necessary in some cases */
		gtk_widget_get_request_mode(widget);
		gtk_widget_size_allocate(widget, &allocation, gtk_widget_get_baseline(widget));
		// maybe we should schedule a call to queue_queue_allocate, but that causes problems in composeUI apps.

		/* the problematic flags get set all the way up the hierarchy */
		GtkWidget *parent = gtk_widget_get_parent(widget);
		if (parent) {
			atl_ensure_widget_snapshotability(parent);
		}
	}
}

void atl_safe_gtk_label_set_text(GtkLabel *label, const char *str)
{
	if (!snapshot_in_progress) {
		gtk_label_set_text(label, str);
	} else {
		/* strdup since the string may not exist by the time the callback runs */
		gtk_widget_add_tick_callback(GTK_WIDGET(label), queue_set_text, (gpointer)strdup(str), NULL);
	}
}

void atl_safe_gtk_widget_set_visible(GtkWidget *widget, gboolean visible)
{
	gtk_widget_set_visible(widget, visible);
	GtkWidget *parent = gtk_widget_get_parent(widget);
	if (parent) {
		atl_ensure_widget_snapshotability(parent);
	}
}

void atl_safe_gtk_widget_queue_allocate(GtkWidget *widget)
{
	if (!snapshot_in_progress) {
		gtk_widget_queue_allocate(widget);
	} else {
		gtk_widget_add_tick_callback(widget, queue_queue_allocate, NULL, NULL);
	}
}

void atl_safe_gtk_widget_queue_resize(GtkWidget *widget)
{
	if (!snapshot_in_progress) {
		gtk_widget_queue_resize(widget);
	} else {
		gtk_widget_add_tick_callback(widget, queue_queue_resize, NULL, NULL);
	}
}

static gboolean unref_idle(gpointer object)
{
	g_object_unref(object);
	return G_SOURCE_REMOVE;
}

/* For finalizers, which run on the FinalizerDaemon thread: GTK objects may only be disposed of on the
 * main thread, so the last reference is dropped from the main loop. */
void atl_unref_on_main_thread(gpointer object)
{
	g_idle_add(unref_idle, object);
}

extern GtkWidget *window;
extern GThread *main_thread_id;

static GPrivate thread_pango_context = G_PRIVATE_INIT(g_object_unref);

struct atl_display_info atl_display_info;

void atl_display_info_init(GtkWidget *window)
{
	GdkDisplay *display = gtk_widget_get_display(window);
	PangoContext *context = gtk_widget_get_pango_context(window);
	atl_display_info.pango_resolution = pango_cairo_context_get_resolution(context);
	const cairo_font_options_t *options = pango_cairo_context_get_font_options(context);
	atl_display_info.pango_font_options = options ? cairo_font_options_copy(options) : NULL;
#ifdef GDK_WINDOWING_WAYLAND
	if (GDK_IS_WAYLAND_DISPLAY(display)) {
		atl_display_info.is_wayland = TRUE;
		atl_display_info.wl_display = gdk_wayland_display_get_wl_display(display);
		atl_display_info.wl_compositor = gdk_wayland_display_get_wl_compositor(display);
	}
#endif
#ifdef GDK_WINDOWING_X11
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
	if (G_TYPE_CHECK_INSTANCE_TYPE(display, gdk_x11_display_get_type())) {
		atl_display_info.is_x11 = TRUE;
		atl_display_info.x11_display = gdk_x11_display_get_xdisplay(display);
	}
#pragma GCC diagnostic pop
#endif
}

/* The PangoContext for text in the current thread. Pango font maps are not thread-safe: only the main thread uses
 * the window's context; other threads (apps drawing text into a Bitmap from worker threads) get a context of their
 * own on the thread's own default font map, with the resolution and font options of the window's context, as taken
 * on the main thread (atl_display_info) */
PangoContext *atl_pango_context(void)
{
	if (!main_thread_id || g_thread_self() == main_thread_id)
		return gtk_widget_get_pango_context(window);
	PangoContext *context = g_private_get(&thread_pango_context);
	if (!context) {
		context = pango_font_map_create_context(pango_cairo_font_map_get_default());
		if (atl_display_info.pango_resolution)
			pango_cairo_context_set_resolution(context, atl_display_info.pango_resolution);
		pango_cairo_context_set_font_options(context, atl_display_info.pango_font_options);
		g_private_set(&thread_pango_context, context);
	}
	return context;
}

static GPrivate thread_hinted_pango_context = G_PRIVATE_INIT(g_object_unref);

/* Like atl_pango_context(), with the same font map, resolution and font options, but full hinting: for the text of
 * a Paint with HINTING_ON in a font that hints itself (android_paint_pango_context()) */
PangoContext *atl_pango_context_hinted(void)
{
	PangoContext *context = g_private_get(&thread_hinted_pango_context);
	if (!context) {
		PangoContext *base = atl_pango_context();
		const cairo_font_options_t *base_options = pango_cairo_context_get_font_options(base);
		cairo_font_options_t *options = base_options ? cairo_font_options_copy(base_options) : cairo_font_options_create();
		cairo_font_options_set_hint_style(options, CAIRO_HINT_STYLE_FULL);
		context = pango_font_map_create_context(pango_context_get_font_map(base));
		pango_context_set_font_description(context, pango_context_get_font_description(base));
		pango_context_set_language(context, pango_context_get_language(base));
		pango_cairo_context_set_resolution(context, pango_cairo_context_get_resolution(base));
		pango_cairo_context_set_font_options(context, options);
		cairo_font_options_destroy(options);
		g_private_set(&thread_hinted_pango_context, context);
	}
	return context;
}

/* the placeholder of a ReplacementSpan at byte index in the layout: x of its left edge, top, baseline and bottom of
 * its line, in Pango units of the layout (the arguments of AOSP ReplacementSpan.draw()); FALSE when an ellipsis
 * has taken its place (AOSP TextLine draws no replacement inside the ellipsis) */
gboolean atl_pango_layout_placeholder(PangoLayout *layout, guint index, float place[4])
{
	PangoLayoutIter *iter = pango_layout_get_iter(layout);
	gboolean shown = FALSE;
	do {
		PangoLayoutRun *run = pango_layout_iter_get_run_readonly(iter);
		if (!run || index < (guint)run->item->offset || index >= (guint)(run->item->offset + run->item->length))
			continue;
		shown = !(run->item->analysis.flags & PANGO_ANALYSIS_FLAG_IS_ELLIPSIS);
		break;
	} while (pango_layout_iter_next_run(iter));
	int top, bottom;
	pango_layout_iter_get_line_yrange(iter, &top, &bottom);
	int baseline = pango_layout_iter_get_baseline(iter);
	pango_layout_iter_free(iter);
	if (!shown)
		return FALSE;
	PangoRectangle pos;
	pango_layout_index_to_pos(layout, index, &pos);
	place[0] = (float)MIN(pos.x, pos.x + pos.width) / PANGO_SCALE;
	place[1] = (float)top / PANGO_SCALE;
	place[2] = (float)baseline / PANGO_SCALE;
	place[3] = (float)bottom / PANGO_SCALE;
	return TRUE;
}

/* Fades the opacity of a widget from its current opacity to `to`, with the default interpolator of
 * Android's animators (AccelerateDecelerate), then calls done (unless cancelled or replaced). Used for
 * what Android animates as a whole window: popups, and the dim behind dialogs. */
struct fade {
	double from;
	double to;
	gint64 start;
	gint64 duration;
	AtlFadeDone done;
	guint tick_id;
};

static gboolean fade_tick(GtkWidget *widget, GdkFrameClock *clock, gpointer user_data)
{
	struct fade *fade = user_data;
	gint64 now = gdk_frame_clock_get_frame_time(clock);
	if (!fade->start)
		fade->start = now;
	double t = fade->duration > 0 ? (double)(now - fade->start) / fade->duration : 1;
	if (t > 1)
		t = 1;
	double v = cos((t + 1) * G_PI) / 2 + 0.5;
	gtk_widget_set_opacity(widget, fade->from + (fade->to - fade->from) * v);
	if (t < 1)
		return G_SOURCE_CONTINUE;
	AtlFadeDone done = fade->done;
	fade->tick_id = 0;
	g_object_set_data(G_OBJECT(widget), "atl-fade", NULL);
	if (done)
		done(widget);
	return G_SOURCE_REMOVE;
}

static void fade_free(gpointer data)
{
	g_free(data);
}

void atl_fade_widget_cancel(GtkWidget *widget)
{
	struct fade *fade = g_object_get_data(G_OBJECT(widget), "atl-fade");
	if (!fade)
		return;
	if (fade->tick_id)
		gtk_widget_remove_tick_callback(widget, fade->tick_id);
	g_object_set_data(G_OBJECT(widget), "atl-fade", NULL);
}

void atl_fade_widget(GtkWidget *widget, double to, int duration_ms, AtlFadeDone done)
{
	atl_fade_widget_cancel(widget);
	if (duration_ms <= 0) {
		gtk_widget_set_opacity(widget, to);
		if (done)
			done(widget);
		return;
	}
	struct fade *fade = g_new0(struct fade, 1);
	fade->from = gtk_widget_get_opacity(widget);
	fade->to = to;
	fade->duration = (gint64)duration_ms * 1000;
	fade->done = done;
	g_object_set_data_full(G_OBJECT(widget), "atl-fade", fade, fade_free);
	fade->tick_id = gtk_widget_add_tick_callback(widget, fade_tick, fade, NULL);
}

/* GtkWindow's move-focus (Tab, Shift+Tab, when the focused widget did not use the key): the focus
 * moves as Android's ViewRootImpl moves it, by the focus search of the window's views
 * (Window.moveFocusOnTab(), FocusFinder), not in the order of the GTK widgets; the title bar is not
 * part of the Android window. The root of the views is the window's content, or the activity's page
 * that is shown in the main window. */
static void on_move_focus(GtkWidget *widget, GtkDirectionType direction, gpointer user_data)
{
	GtkWindow *window = GTK_WINDOW(widget);
	if (direction != GTK_DIR_TAB_FORWARD && direction != GTK_DIR_TAB_BACKWARD)
		return;
	g_signal_stop_emission_by_name(widget, "move-focus");
	/* a key leaves touch mode before it is handled (ViewRootImpl.ensureTouchMode(false)): the focus search
	 * of the views then finds the views that are not focusable in touch mode, too */
	gtk_window_set_focus_visible(window, TRUE);
	GtkWidget *root = gtk_window_get_child(window);
	GtkWidget *stack = activity_pages_stack();
	if (stack && gtk_widget_get_root(stack) == GTK_ROOT(window))
		root = gtk_stack_get_visible_child(GTK_STACK(stack));
	if (root && WRAPPER_IS_WIDGET(root) && WRAPPER_WIDGET(root)->jobj) {
		JNIEnv *env = get_jni_env();
		J__Window__moveFocusOnTab(env, WRAPPER_WIDGET(root)->jobj, direction == GTK_DIR_TAB_FORWARD);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
	}
	gtk_window_set_focus_visible(window, TRUE);
}

/* a press of the pointer (which ATL hands to the app as touch) enters touch mode, which hides the
 * focus (Android: ViewRootImpl.ensureTouchMode()); Tab leaves it again (on_move_focus) */
static gboolean on_pointer_event(GtkEventControllerLegacy *controller, GdkEvent *event, GtkWindow *window)
{
	GdkEventType type = gdk_event_get_event_type(event);
	if (type == GDK_BUTTON_PRESS || type == GDK_TOUCH_BEGIN)
		gtk_window_set_focus_visible(window, FALSE);
	return FALSE;
}

void atl_window_android_tab_focus(GtkWindow *window)
{
	g_signal_connect(window, "move-focus", G_CALLBACK(on_move_focus), NULL);
	GtkEventController *controller = gtk_event_controller_legacy_new();
	gtk_event_controller_set_propagation_phase(controller, GTK_PHASE_CAPTURE);
	g_signal_connect(controller, "event", G_CALLBACK(on_pointer_event), window);
	gtk_widget_add_controller(GTK_WIDGET(window), controller);
}

/* Mouse wheel acceleration as in AOSP's InputReader: CursorInputMapper runs the wheel deltas through
 * a VelocityControl per axis (input/VelocityControl.c, a port of libs/input/VelocityControl.cpp), with
 * InputReaderConfiguration's wheelVelocityControlParameters (scale 1, low threshold 15, high threshold 50,
 * acceleration 4): a wheel spun faster than 15 notches/s scrolls up to 4 times as far per notch (full at
 * 50/s). GTK delivers the notches unaccelerated, and a GdkEvent cannot be built with other deltas, so the
 * event is delivered once more for every whole notch the acceleration adds (the remainder is carried to
 * the next event). This happens before any widget sees the event, so views (ACTION_SCROLL) and WebViews
 * (WebKit scrolls on its own) are accelerated alike. */
static struct {
	struct velocity_control x, y; // mWheelXVelocityControl, mWheelYVelocityControl
	gboolean initialized;
	nsecs_t last_event_time;
	float pending; // accelerated notches not yet delivered (fraction of an event)
	gboolean redelivering;
} wheel;

static gboolean on_wheel_event(GtkEventControllerLegacy *controller, GdkEvent *event, gpointer user_data)
{
	if (wheel.redelivering || gdk_event_get_event_type(event) != GDK_SCROLL || gdk_scroll_event_get_unit(event) != GDK_SCROLL_UNIT_WHEEL)
		return FALSE;
	double dx, dy;
	gdk_scroll_event_get_deltas(event, &dx, &dy);
	if (!dx && !dy) {
		GdkScrollDirection direction = gdk_scroll_event_get_direction(event);
		dy = direction == GDK_SCROLL_DOWN ? 1 : direction == GDK_SCROLL_UP ? -1 : 0;
	}
	if (!dx && !dy)
		return FALSE;

	if (!wheel.initialized) {
		struct velocity_control_parameters parameters = {1.0f, 15.0f, 50.0f, 4.0f};
		velocity_control_init(&wheel.x);
		velocity_control_init(&wheel.y);
		velocity_control_set_parameters(&wheel.x, &parameters);
		velocity_control_set_parameters(&wheel.y, &parameters);
		wheel.initialized = TRUE;
	}

	nsecs_t when = (nsecs_t)gdk_event_get_time(event) * 1000000;
	/* InputReader accelerates each wheel event once, when it comes from the device. A WebView gets the
	 * events WebCore did not handle (at the end of the page) back from WebKitGTK with gdk_display_put_event(),
	 * so they pass this window again: they are not new input, and accelerating them again would deliver
	 * them again, back to WebKit, which hands them back once more (a loop that floods WebKit's wheel event
	 * queue, and the wheel turned the other way then waits behind it or is coalesced with it). */
	if (wheel.last_event_time && when <= wheel.last_event_time)
		return FALSE;
	if (wheel.last_event_time && when - wheel.last_event_time >= 500 * 1000000) // VelocityControl::STOP_TIME: the controls reset themselves
		wheel.pending = 0;
	wheel.last_event_time = when;

	float hscroll = dx, vscroll = dy;
	velocity_control_move(&wheel.y, when, NULL, &vscroll);
	velocity_control_move(&wheel.x, when, &hscroll, NULL);
	float scale = dy ? vscroll / dy : hscroll / dx;

	// the event itself is one delivery; deliver it again for each further whole multiple
	wheel.pending += scale - 1;
	GdkSurface *surface = gdk_event_get_surface(event);
	wheel.redelivering = TRUE;
	for (; wheel.pending >= 1; wheel.pending -= 1) {
		gboolean handled;
		g_signal_emit_by_name(surface, "event", event, &handled);
	}
	wheel.redelivering = FALSE;
	return FALSE;
}

void atl_window_android_wheel_acceleration(GtkWindow *window)
{
	GtkEventController *controller = gtk_event_controller_legacy_new();
	gtk_event_controller_set_propagation_phase(controller, GTK_PHASE_CAPTURE);
	g_signal_connect(controller, "event", G_CALLBACK(on_wheel_event), NULL);
	gtk_widget_add_controller(GTK_WIDGET(window), controller);
}

/* The back and forward buttons of a mouse (BTN_SIDE/BTN_EXTRA, GDK buttons 8 and 9) are keys on
 * Android, not presses: InputReader turns them into KEYCODE_BACK and KEYCODE_FORWARD
 * (synthesizeButtonKeys). So no view or WebView gets the press, the release of back goes back like
 * the back key, and forward has no default action. */
static gboolean on_mouse_back_event(GtkEventControllerLegacy *controller, GdkEvent *event, GCallback back)
{
	GdkEventType type = gdk_event_get_event_type(event);
	if (type != GDK_BUTTON_PRESS && type != GDK_BUTTON_RELEASE)
		return FALSE;
	guint button = gdk_button_event_get_button(event);
	if (button != 8 && button != 9)
		return FALSE;
	if (type == GDK_BUTTON_RELEASE && button == 8) {
		/* a key goes to the window with the input focus, not to the one under the pointer: while a dialog
		 * is shown that is the dialog (atl_window_set_key_window()), even if the pointer is outside of it */
		GtkWidget *window = gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(controller));
		GtkWidget *key_window = g_object_get_data(G_OBJECT(window), "key-window");
		GObject *target = key_window ? g_object_get_data(G_OBJECT(key_window), "mouse-back") : G_OBJECT(controller);
		if (target)
			((void (*)(gpointer))g_object_get_data(target, "back-fn"))(g_object_get_data(target, "back-data"));
	}
	return TRUE;
}

void atl_window_android_mouse_back(GtkWindow *window, GCallback back, gpointer data)
{
	GtkEventController *controller = gtk_event_controller_legacy_new();
	gtk_event_controller_set_propagation_phase(controller, GTK_PHASE_CAPTURE);
	g_object_set_data(G_OBJECT(controller), "back-data", data);
	g_object_set_data(G_OBJECT(controller), "back-fn", back);
	g_object_set_data(G_OBJECT(window), "mouse-back", controller);
	g_signal_connect(controller, "event", G_CALLBACK(on_mouse_back_event), back);
	gtk_widget_add_controller(GTK_WIDGET(window), controller);
}

/* The window that takes the keys of an app window, while it has the input focus above it (a dialog),
 * or NULL again (see atl_window_android_mouse_back()). */
void atl_window_set_key_window(GtkWindow *window, GtkWindow *key_window)
{
	g_object_set_data(G_OBJECT(window), "key-window", key_window);
}

GVariant *intent_serialize(JNIEnv *env, jobject intent)
{
	if (!intent)
		return NULL;
	jstring action_jstr = _GET_OBJ_FIELD(intent, "action", "Ljava/lang/String;");
	jobject component = _GET_OBJ_FIELD(intent, "component", "Landroid/content/ComponentName;");
	jstring className_jstr = component ? _GET_OBJ_FIELD(component, "mClass", "Ljava/lang/String;") : NULL;
	jstring data_jstr = J__Intent__getDataString(env, intent);

	GVariantBuilder extras_builder;
	g_variant_builder_init(&extras_builder, G_VARIANT_TYPE_VARDICT);
	jobject extras = _GET_OBJ_FIELD(intent, "extras", "Landroid/os/Bundle;");
	jobject extras_key_set = J__BaseBundle__keySet(env, extras);
	jobjectArray extras_keys = J__Set__toArray(env, extras_key_set);
	jsize extras_keys_length = (*env)->GetArrayLength(env, extras_keys);
	jclass parcelable_class = _CACHED_CLASS(Parcelable);
	for (jint i = 0; i < extras_keys_length; i++) {
		jstring key_jstr = (*env)->GetObjectArrayElement(env, extras_keys, i);
		jobject value_jobj = J__BaseBundle__get(env, extras, key_jstr);
		if (!key_jstr || !value_jobj)
			continue;
		const char *key = (*env)->GetStringUTFChars(env, key_jstr, NULL);
		if ((*env)->IsInstanceOf(env, value_jobj, _CLASS(key_jstr))) {
			const char *value = (*env)->GetStringUTFChars(env, value_jobj, NULL);
			g_variant_builder_add(&extras_builder, "{sv}", key, g_variant_new_string(value));
			(*env)->ReleaseStringUTFChars(env, value_jobj, value);
		} else if ((*env)->IsInstanceOf(env, value_jobj, parcelable_class)) {
			GVariantBuilder parcel_builder;
			g_variant_builder_init(&parcel_builder, G_VARIANT_TYPE_TUPLE);
			jobject parcel = J_new__GVariantBuilderParcel(env, _INTPTR(&parcel_builder));
			J__Parcel__writeParcelable(env, parcel, value_jobj, 0);
			GVariant *parcel_variant = g_variant_builder_end(&parcel_builder);
			g_variant_builder_add(&extras_builder, "{sv}", key, parcel_variant);
			(*env)->DeleteLocalRef(env, parcel);
		} else {
			printf("intent_serialize: skipping non-string, non-parcelable extra: %s\n", key);
		}
		(*env)->ReleaseStringUTFChars(env, key_jstr, key);
		(*env)->DeleteLocalRef(env, key_jstr);
		(*env)->DeleteLocalRef(env, value_jobj);
	}

	const char *action = action_jstr ? (*env)->GetStringUTFChars(env, action_jstr, NULL) : NULL;
	const char *className = className_jstr ? (*env)->GetStringUTFChars(env, className_jstr, NULL) : NULL;
	const char *data = data_jstr ? (*env)->GetStringUTFChars(env, data_jstr, NULL) : NULL;
	const char *dbus_name = g_application_get_application_id(g_application_get_default());
	GVariant *variant = g_variant_new(INTENT_G_VARIANT_TYPE_STRING, action ?: "", className ?: "", data ?: "", &extras_builder, dbus_name);
	if (action_jstr)
		(*env)->ReleaseStringUTFChars(env, action_jstr, action);
	if (className_jstr)
		(*env)->ReleaseStringUTFChars(env, className_jstr, className);
	if (data_jstr)
		(*env)->ReleaseStringUTFChars(env, data_jstr, data);
	return variant;
}

jobject intent_deserialize(JNIEnv *env, GVariant *variant)
{
	const char *action;
	const char *className;
	const char *data;
	GVariantIter *extras;
	g_variant_get(variant, INTENT_G_VARIANT_TYPE_STRING, &action, &className, &data, &extras, NULL);
	if (action && action[0] == '\0')
		action = NULL;
	if (className && className[0] == '\0')
		className = NULL;
	if (data && data[0] == '\0')
		data = NULL;

	jobject intent = J_new__Intent(env);
	_SET_OBJ_FIELD(intent, "action", "Ljava/lang/String;", _JSTRING(action));
	if (className)
		J__Intent__setClassName(env, intent, _GET_STATIC_OBJ_FIELD(_CACHED_CLASS(Context), "this_application", "Landroid/app/Application;"), _JSTRING(className));
	if (data)
		_SET_OBJ_FIELD(intent, "data", "Landroid/net/Uri;", J__Uri__parse(env, _JSTRING(data)));
	const char *key;
	GVariant *value;
	while (g_variant_iter_loop(extras, "{sv}", &key, &value)) {
		if (g_variant_is_of_type(value, G_VARIANT_TYPE_STRING)) {
			J__Intent__putExtraCharSequence(env, intent, _JSTRING(key), _JSTRING(g_variant_get_string(value, NULL)));
		} else if (g_variant_is_of_type(value, G_VARIANT_TYPE_INT32)) {
			J__Intent__putExtraInt(env, intent, _JSTRING(key), g_variant_get_int32(value));
		} else if (g_variant_is_of_type(value, G_VARIANT_TYPE_INT64)) {
			J__Intent__putExtraLong(env, intent, _JSTRING(key), g_variant_get_int64(value));
		} else if (g_variant_is_of_type(value, G_VARIANT_TYPE_BYTESTRING)) {
			gsize size;
			const int8_t *message = g_variant_get_fixed_array(value, &size, 1);
			jbyteArray bytesMessage = (*env)->NewByteArray(env, size);
			(*env)->SetByteArrayRegion(env, bytesMessage, 0, size, message);
			J__Intent__putExtraByteArray(env, intent, _JSTRING(key), bytesMessage);
		} else if (g_variant_is_of_type(value, G_VARIANT_TYPE_TUPLE)) {
			GVariantIter parcel_iter;
			g_variant_iter_init(&parcel_iter, value);
			jobject class_loader = J__Context__getClassLoader(env, _GET_STATIC_OBJ_FIELD(_CACHED_CLASS(Context), "this_application", "Landroid/app/Application;"));
			jobject parcel = J_new__GVariantIterParcel(env, _INTPTR(&parcel_iter));
			jobject parcelable = J__Parcel__readParcelable(env, parcel, class_loader);
			if ((*env)->ExceptionCheck(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
			}
			J__Intent__putExtraParcelable(env, intent, _JSTRING(key), parcelable);
			(*env)->DeleteLocalRef(env, parcelable);
			(*env)->DeleteLocalRef(env, parcel);
		}
	}
	g_variant_iter_free(extras);
	return intent;
}

/* a point of the widget in the surface of its window; a widget in a popup (a popover has a surface
 * of its own) is placed by the popup's position on its parent's surface, like a view in a popup
 * window on Android, whose location on screen includes the position of the popup */
gboolean atl_compute_point_in_window(GtkWidget *widget, const graphene_point_t *point_in, graphene_point_t *point_out)
{
	GtkNative *native = gtk_widget_get_native(widget);
	double off_x, off_y;
	if (!native || !gtk_widget_compute_point(widget, GTK_WIDGET(native), point_in, point_out))
		return FALSE;
	gtk_native_get_surface_transform(native, &off_x, &off_y);
	point_out->x += off_x;
	point_out->y += off_y;
	while (GTK_IS_POPOVER(native)) {
		GdkSurface *surface = gtk_native_get_surface(native);
		GtkWidget *parent = gtk_widget_get_parent(GTK_WIDGET(native));
		native = parent ? gtk_widget_get_native(parent) : NULL;
		if (!native || !surface || !GDK_IS_POPUP(surface))
			return FALSE;
		point_out->x += gdk_popup_get_position_x(GDK_POPUP(surface));
		point_out->y += gdk_popup_get_position_y(GDK_POPUP(surface));
	}
	return TRUE;
}

/*
 * Concatenate an android.graphics.Matrix to the snapshot like Skia's SkCanvas::concat(const SkMatrix&):
 * an identity matrix changes nothing, and translate/scale matrices (SkMatrix::getType() without
 * kAffine/kPerspective) stay translate/scale transforms. GSK can't classify a raw matrix, so every
 * gtk_snapshot_transform_matrix() is an "unknown" transform for it and goes through its generic
 * offscreen path, which breaks below views that are scaled almost to zero (e.g. the SwipeRefreshLayout
 * spinner at the end of its scale-down animation, ImageView's identity draw matrix inside it): the
 * stroke mask comes out NaN-sized and the renderer crashes.
 */
void atl_snapshot_concat_matrix(GtkSnapshot *snapshot, const graphene_matrix_t *matrix)
{
	if (graphene_matrix_is_identity(matrix))
		return;
	if (graphene_matrix_is_2d(matrix) && graphene_matrix_get_value(matrix, 0, 1) == 0.f && graphene_matrix_get_value(matrix, 1, 0) == 0.f) {
		gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(graphene_matrix_get_value(matrix, 3, 0), graphene_matrix_get_value(matrix, 3, 1)));
		gtk_snapshot_scale(snapshot, graphene_matrix_get_value(matrix, 0, 0), graphene_matrix_get_value(matrix, 1, 1));
		return;
	}
	/* a GskTransform made from a raw matrix has an unknown category, and GSK draws such a
	 * transform node as a pink fallback; so build other 2D affine matrices (rotations) from
	 * translate * rotate * skew * scale, which keeps the 2D category */
	/* graphene_matrix_is_2d() wants exact zeros, which rotations do not leave; an android Matrix
	 * is 2D unless it has perspective */
	if (fabsf(graphene_matrix_get_value(matrix, 0, 3)) < 1e-6f && fabsf(graphene_matrix_get_value(matrix, 1, 3)) < 1e-6f &&
	    fabsf(graphene_matrix_get_value(matrix, 3, 3) - 1.f) < 1e-6f) {
		double xx = graphene_matrix_get_value(matrix, 0, 0), yx = graphene_matrix_get_value(matrix, 0, 1);
		double xy = graphene_matrix_get_value(matrix, 1, 0), yy = graphene_matrix_get_value(matrix, 1, 1);
		double x0 = graphene_matrix_get_value(matrix, 3, 0), y0 = graphene_matrix_get_value(matrix, 3, 1);
		double sx = hypot(xx, yx);
		double sy = sx ? (xx * yy - yx * xy) / sx : 0;
		if (sx && sy) {
			double shear = (xx * xy + yx * yy) / sx;
			GskTransform *transform = gsk_transform_translate(NULL, &GRAPHENE_POINT_INIT(x0, y0));
			transform = gsk_transform_rotate(transform, atan2(yx, xx) * 180 / G_PI);
			transform = gsk_transform_skew(transform, atan(shear / sy) * 180 / G_PI, 0);
			transform = gsk_transform_scale(transform, sx, sy);
			gtk_snapshot_transform(snapshot, transform);
			gsk_transform_unref(transform);
			return;
		}
	}
	gtk_snapshot_transform_matrix(snapshot, matrix);
}

/* Touch targets per window, as AOSP's InputDispatcher keeps them: a pointer's gesture belongs to the
 * window that got its ACTION_DOWN, and a window gets the moves and the up of a gesture only if it got
 * the down (InputDispatcher::findTouchedWindowTargetsLocked(); the same holds for the next ViewGroup in
 * ViewGroup.dispatchTouchEvent(), which drops them without a touch target). GTK passes the rest of a
 * press on to the window below once the popup that took the press (a closing grabbing popover, a
 * touch-modal window, a dialog) is gone, so the window drops the moves and the release it got no
 * press for. The pointer moving without a button is a hover and always passes. */
#define BUTTONS_MASK (GDK_BUTTON1_MASK | GDK_BUTTON2_MASK | GDK_BUTTON3_MASK | GDK_BUTTON4_MASK | GDK_BUTTON5_MASK)

static gpointer press_key(GtkWidget *window, GdkEvent *event)
{
	GdkEventSequence *sequence = gdk_event_get_event_sequence(event);
	return sequence ? (gpointer)sequence : (gpointer)window; /* the mouse has no sequence */
}

static gboolean on_gesture_event(GtkEventControllerLegacy *controller, GdkEvent *event, GHashTable *pressed)
{
	GtkWidget *window = gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(controller));
	GdkEventType type = gdk_event_get_event_type(event);
	gpointer key = press_key(window, event);
	GdkModifierType buttons = gdk_event_get_modifier_state(event) & BUTTONS_MASK;
	switch (type) {
	case GDK_BUTTON_PRESS:
	case GDK_TOUCH_BEGIN:
		g_hash_table_add(pressed, key);
		return FALSE;
	case GDK_MOTION_NOTIFY:
		if (!buttons) { /* a hover; a gesture whose release this window never got is over, too */
			g_hash_table_remove(pressed, key);
			return FALSE;
		}
		return !g_hash_table_contains(pressed, key);
	case GDK_TOUCH_UPDATE:
		return !g_hash_table_contains(pressed, key);
	case GDK_BUTTON_RELEASE: {
		gboolean got_press = g_hash_table_contains(pressed, key);
		if (!(buttons & ~(GDK_BUTTON1_MASK << (gdk_button_event_get_button(event) - 1))))
			g_hash_table_remove(pressed, key);
		return !got_press;
	}
	case GDK_TOUCH_END:
	case GDK_TOUCH_CANCEL:
		return !g_hash_table_remove(pressed, key);
	default:
		return FALSE;
	}
}

/* AOSP's InputDispatcher finds the window and view under the pointer anew for every ACTION_DOWN. GTK
 * delivers a press to the widget it picked at the last motion of the pointer, so after the views moved
 * under a pointer that stands still (a pager turned the page, a new screen came up) the press would go
 * to the view that was there before: the first click after it was lost. So after each frame, if the
 * pointer is over another widget than the one GTK picked, GTK picks again: it does that when the old
 * widget's sensitivity changes (gtk_window_update_pointer_focus_on_state_change()). */
struct pointer_pick {
	GtkWidget *window;
	GWeakRef target; /* the widget GTK picked at the last pointer event */
	double x, y;
	gboolean inside;
	guint settle; /* timeout that picks again once the views stopped moving */
};

static void pointer_pick_free(struct pointer_pick *p)
{
	g_clear_handle_id(&p->settle, g_source_remove);
	g_weak_ref_clear(&p->target);
	g_free(p);
}

static void pointer_pick_update(GdkEvent *event, struct pointer_pick *p)
{
	switch (gdk_event_get_event_type(event)) {
	case GDK_LEAVE_NOTIFY:
		p->inside = FALSE;
		g_weak_ref_set(&p->target, NULL);
		return;
	case GDK_ENTER_NOTIFY:
	case GDK_MOTION_NOTIFY:
	case GDK_BUTTON_RELEASE:
		if (gdk_device_get_source(gdk_event_get_device(event)) == GDK_SOURCE_TOUCHSCREEN)
			return;
		if (gdk_event_get_surface(event) != gtk_native_get_surface(GTK_NATIVE(p->window)))
			return;
		double x, y;
		if (!gdk_event_get_position(event, &x, &y))
			return;
		graphene_point_t point;
		if (!gtk_widget_compute_point(GTK_WIDGET(gtk_widget_get_native(p->window)), p->window, &GRAPHENE_POINT_INIT(x, y), &point))
			return;
		p->x = point.x;
		p->y = point.y;
		p->inside = TRUE;
		/* as GTK picks for a motion; after a release it picks anew, too (gtkmain.c) */
		g_weak_ref_set(&p->target, gtk_widget_pick(p->window, p->x, p->y, GTK_PICK_DEFAULT));
		return;
	default:
		return;
	}
}

static gboolean pointer_pick_update_cb(GtkEventControllerLegacy *controller, GdkEvent *event, struct pointer_pick *p)
{
	pointer_pick_update(event, p);
	return FALSE;
}

/* once the views under the pointer stopped moving: an animation passes widgets on its way (the gap
 * between two pages) which may contain the one that ends up under the pointer */
static gboolean pointer_pick_settled(struct pointer_pick *p)
{
	p->settle = 0;
	GtkWidget *target = g_weak_ref_get(&p->target);
	if (!target)
		return G_SOURCE_REMOVE;
	GtkWidget *now = gtk_widget_pick(p->window, p->x, p->y, GTK_PICK_DEFAULT);
	GdkSeat *seat = gdk_display_get_default_seat(gtk_widget_get_display(p->window));
	gboolean button_down = seat && (gdk_device_get_modifier_state(gdk_seat_get_pointer(seat)) & BUTTONS_MASK);
	/* not while a button is held (the gesture stays with its widget), and not when the new widget is
	 * inside the old one: GTK picks while the old one is insensitive and would stop above it */
	if (p->inside && now && now != target && !button_down && !gtk_widget_is_ancestor(now, target) && gtk_widget_get_sensitive(target)) {
		gtk_widget_set_sensitive(target, FALSE);
		GtkWidget *picked = gtk_widget_pick(p->window, p->x, p->y, GTK_PICK_DEFAULT); /* what GTK picked */
		gtk_widget_set_sensitive(target, TRUE);
		g_weak_ref_set(&p->target, picked);
	}
	g_object_unref(target);
	return G_SOURCE_REMOVE;
}

static void pointer_pick_after_paint(GdkFrameClock *clock, struct pointer_pick *p)
{
	if (!p->inside)
		return;
	GtkWidget *target = g_weak_ref_get(&p->target);
	if (!target)
		return;
	if (gtk_widget_pick(p->window, p->x, p->y, GTK_PICK_DEFAULT) != target) {
		g_clear_handle_id(&p->settle, g_source_remove);
		p->settle = g_timeout_add(100, G_SOURCE_FUNC(pointer_pick_settled), p);
	}
	g_object_unref(target);
}

static void pointer_pick_realize(GtkWidget *window, struct pointer_pick *p)
{
	g_signal_connect(gtk_widget_get_frame_clock(window), "after-paint", G_CALLBACK(pointer_pick_after_paint), p);
}

static void pointer_pick_unrealize(GtkWidget *window, struct pointer_pick *p)
{
	g_signal_handlers_disconnect_by_func(gtk_widget_get_frame_clock(window), pointer_pick_after_paint, p);
}

void atl_window_android_touch_targets(GtkWindow *window)
{
	GHashTable *pressed = g_hash_table_new(g_direct_hash, g_direct_equal);
	g_object_set_data_full(G_OBJECT(window), "touch-targets", pressed, (GDestroyNotify)g_hash_table_unref);
	GtkEventController *controller = gtk_event_controller_legacy_new();
	gtk_event_controller_set_propagation_phase(controller, GTK_PHASE_CAPTURE);
	g_signal_connect(controller, "event", G_CALLBACK(on_gesture_event), pressed);
	gtk_widget_add_controller(GTK_WIDGET(window), controller);

	struct pointer_pick *p = g_new0(struct pointer_pick, 1);
	p->window = GTK_WIDGET(window);
	g_weak_ref_init(&p->target, NULL);
	g_object_set_data_full(G_OBJECT(window), "pointer-pick", p, (GDestroyNotify)pointer_pick_free);
	controller = gtk_event_controller_legacy_new();
	gtk_event_controller_set_propagation_phase(controller, GTK_PHASE_CAPTURE);
	g_signal_connect(controller, "event", G_CALLBACK(pointer_pick_update_cb), p);
	gtk_widget_add_controller(GTK_WIDGET(window), controller);
	g_signal_connect(window, "realize", G_CALLBACK(pointer_pick_realize), p);
	g_signal_connect(window, "unrealize", G_CALLBACK(pointer_pick_unrealize), p);
	if (gtk_widget_get_realized(GTK_WIDGET(window)))
		pointer_pick_realize(GTK_WIDGET(window), p);
}

void atl_window_press_taken(GtkWidget *widget, GdkEvent *event)
{
	GtkRoot *root = gtk_widget_get_root(widget);
	GHashTable *pressed = root ? g_object_get_data(G_OBJECT(root), "touch-targets") : NULL;
	if (pressed)
		g_hash_table_remove(pressed, press_key(GTK_WIDGET(root), event));
}

/* ATL sets LC_ALL=C.UTF-8 for the apps (main.c), so GTK's own texts are English. GTK's dialogs stand in for
 * system UI (DocumentsUI, the chooser), which on Android speaks the user's language, not the app's C locale:
 * keep the user's message locale from before the override for them, C.UTF-8 for every other category. */
static locale_t user_messages_locale;

void atl_remember_user_locale(void)
{
	static const char *const variables[] = {"LC_ALL", "LC_MESSAGES", "LANG"};
	for (int i = 0; i < 3; i++) {
		const char *name = getenv(variables[i]);
		if (name && *name) {
			locale_t base = newlocale(LC_ALL_MASK, "C.UTF-8", (locale_t)0);
			if (base && !(user_messages_locale = newlocale(LC_MESSAGES_MASK, name, base)))
				freelocale(base);
			return;
		}
	}
}

/* the same for GTK's widgets in the app's windows, such as the context menu of text fields (Android's
 * floating toolbar): the GTK thread takes its messages from the user's locale. Numbers, time and character
 * types stay C.UTF-8 for the app's native code; so do the C library's own messages (strerror(), which
 * bionic has only in English): its text domain gets no catalog. */
void atl_use_user_messages_locale(void)
{
	if (!user_messages_locale)
		return;
	bindtextdomain("libc", "/dev/null");
	uselocale(user_messages_locale);
}

/* GTK's translation of one of its own texts (msgid as in GTK's sources) in the user's language */
const char *atl_gtk_text(const char *msgid)
{
	if (!user_messages_locale)
		return msgid;
	locale_t previous = uselocale(user_messages_locale);
	const char *text = dgettext("gtk40", msgid);
	uselocale(previous);
	return text;
}

/* a string of framework-res (com.android.internal.R.string.<name>) in the context's language, to be freed */
char *atl_framework_string(JNIEnv *env, jobject context, const char *name)
{
	jclass r_string = (*env)->FindClass(env, "com/android/internal/R$string");
	jint id = (*env)->GetStaticIntField(env, r_string, _STATIC_FIELD_ID(r_string, name, "I"));
	jstring text = (*env)->CallObjectMethod(env, context, _METHOD(_CLASS(context), "getString", "(I)Ljava/lang/String;"), id);
	const char *utf = _CSTRING(text);
	char *result = g_strdup(utf);
	(*env)->ReleaseStringUTFChars(env, text, utf);
	(*env)->DeleteLocalRef(env, text);
	(*env)->DeleteLocalRef(env, r_string);
	return result;
}

char *atl_install_path(const char *rel)
{
	Dl_info info;
	if (g_path_is_absolute(rel) || !dladdr(atl_install_path, &info) || !info.dli_fname)
		return g_strdup(rel);
	char *dir = g_path_get_dirname(info.dli_fname);
	char *path = g_build_filename(dir, rel, NULL);
	g_free(dir);
	return path;
}
