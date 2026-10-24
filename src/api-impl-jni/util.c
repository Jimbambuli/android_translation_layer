#include <math.h>
#include <dlfcn.h>
#include <pthread.h>
#include <stdint.h>
#include <string.h>

#include <gtk/gtk.h>

#include "src/api-impl-jni/defines.h"
#include "util.h"

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

static int android_log_vprintf(int prio, const char *tag, const char *fmt, va_list ap)
{

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

/* where Tab may put the focus: in the window's content (not the title bar, which is not part of
 * the Android window) and not inside a ViewGroup with touchscreenBlocksFocus */
static gboolean tab_focus_allowed(GtkWindow *window, GtkWidget *focus)
{
	GtkWidget *content = gtk_window_get_child(window);
	if (!focus || !content || !gtk_widget_is_ancestor(focus, content))
		return FALSE;
	for (GtkWidget *w = focus; w && w != content; w = gtk_widget_get_parent(w)) {
		if (g_object_get_data(G_OBJECT(w), "atl-blocks-tab-focus"))
			return FALSE;
	}
	return TRUE;
}

/* GtkWindow's move-focus (Tab, Shift+Tab, when the focused widget did not use the key): move like
 * GTK does, but on to the next widget as long as the focus lands where Android's focus search
 * would not go. Like Android, the focus wraps around at the end. */
static void on_move_focus(GtkWidget *widget, GtkDirectionType direction, gpointer user_data)
{
	GtkWindow *window = GTK_WINDOW(widget);
	if (direction != GTK_DIR_TAB_FORWARD && direction != GTK_DIR_TAB_BACKWARD)
		return;
	g_signal_stop_emission_by_name(widget, "move-focus");
	GtkWidget *start = gtk_root_get_focus(GTK_ROOT(window));
	if (start)
		g_object_ref(start);
	for (int i = 0; i < 1000; i++) {
		if (!gtk_widget_child_focus(widget, direction)) {
			gtk_window_set_focus(window, NULL);
			if (!gtk_widget_child_focus(widget, direction))
				break;
		}
		GtkWidget *focus = gtk_root_get_focus(GTK_ROOT(window));
		if (tab_focus_allowed(window, focus))
			break;
		if (focus == start) // went all the way round
			break;
	}
	if (start)
		g_object_unref(start);
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
 * a VelocityControl with wheelVelocityControlParameters (scale 1, low 15, high 50, acceleration 4),
 * so a wheel spun faster than 15 notches/s scrolls up to 4 times as far per notch (full at 50/s).
 * The speed comes from VelocityTracker's default LSQ2 estimator: the last 20 positions within
 * 100 ms, history cleared after 40 ms without movement. GTK delivers the notches unaccelerated,
 * and a GdkEvent cannot be built with other deltas, so the event is delivered once more for
 * every whole notch the acceleration adds. This happens before any widget sees the event, so
 * views (ACTION_SCROLL) and WebViews (WebKit scrolls on its own) are accelerated alike. */
#define WHEEL_HISTORY 20
static struct {
	guint32 time[WHEEL_HISTORY];
	float position[WHEEL_HISTORY];
	int32_t count;
	float raw_position;
	float pending; // accelerated notches not yet delivered (fraction of an event)
	gboolean redelivering;
} wheel;

static float wheel_velocity(void)
{
	// times in seconds relative to the newest sample, as in LeastSquaresVelocityTrackerStrategy
	guint32 newest = wheel.time[wheel.count - 1];
	float sx = 0, sx2 = 0, sx3 = 0, sx4 = 0, sy = 0, sxy = 0, sx2y = 0;
	int32_t m = 0;
	for (int32_t i = wheel.count - 1; i >= 0 && newest - wheel.time[i] <= 100; i--, m++) {
		float x = -(float)(newest - wheel.time[i]) / 1000, y = wheel.position[i];
		sx += x, sx2 += x * x, sx3 += x * x * x, sx4 += x * x * x * x;
		sy += y, sxy += x * y, sx2y += x * x * y;
	}
	if (m < 2)
		return 0;
	float sxx = sx2 - sx * sx / m, sxy_ = sxy - sx * sy / m;
	if (m == 2) // degree reduced to m - 1
		return sxx ? sxy_ / sxx : 0;
	float sxx2 = sx3 - sx * sx2 / m, sx2y_ = sx2y - sx2 * sy / m, sx2x2 = sx4 - sx2 * sx2 / m;
	float denominator = sxx * sx2x2 - sxx2 * sxx2;
	return denominator ? (sxy_ * sx2x2 - sx2y_ * sxx2) / denominator : 0;
}

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
	float delta = dy ? dy : dx;
	if (!delta)
		return FALSE;

	guint32 time = gdk_event_get_time(event);
	if (wheel.count && time - wheel.time[wheel.count - 1] > 40) // pointer stopped
		wheel.count = 0;
	if (!wheel.count)
		wheel.pending = 0;
	if (wheel.count == WHEEL_HISTORY) {
		memmove(wheel.time, wheel.time + 1, sizeof(wheel.time[0]) * (WHEEL_HISTORY - 1));
		memmove(wheel.position, wheel.position + 1, sizeof(wheel.position[0]) * (WHEEL_HISTORY - 1));
		wheel.count--;
	}
	wheel.raw_position += delta;
	wheel.time[wheel.count] = time;
	wheel.position[wheel.count] = wheel.raw_position;
	wheel.count++;

	float speed = fabsf(wheel_velocity()), scale = 1;
	if (speed >= 50)
		scale = 4;
	else if (speed > 15)
		scale = 1 + (speed - 15) / (50 - 15) * (4 - 1);

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

const char *intent_actionname_from_type(int type)
{
	switch (type) {
		case 0:
			return "app.startActivity";
		case 1:
			return "app.startService";
		case 2:
			return "app.sendBroadcast";
		default:
			return NULL;
	}
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
