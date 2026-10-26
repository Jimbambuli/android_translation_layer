#include <assert.h>
#include <stdio.h>

#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"

#include "../views/AndroidLayout.h"
#include "../widgets/WrapperWidget.h"

#include "../generated_headers/android_view_MotionEvent.h"
#include "../generated_headers/android_view_View.h"

#define JAVA_ENUM_CLASS android_view_MotionEvent
enum {
	JAVA_ENUM(ACTION_DOWN),
	JAVA_ENUM(ACTION_UP),
	JAVA_ENUM(ACTION_MOVE),
	JAVA_ENUM(ACTION_CANCEL),

	JAVA_ENUM(ACTION_POINTER_DOWN),
	JAVA_ENUM(ACTION_POINTER_UP),

	JAVA_ENUM(ACTION_HOVER_MOVE),
	JAVA_ENUM(ACTION_SCROLL),
	JAVA_ENUM(ACTION_HOVER_ENTER),
	JAVA_ENUM(ACTION_HOVER_EXIT),
	JAVA_ENUM(ACTION_BUTTON_PRESS),
	JAVA_ENUM(ACTION_BUTTON_RELEASE),

	JAVA_ENUM(BUTTON_PRIMARY),
	JAVA_ENUM(BUTTON_SECONDARY),
	JAVA_ENUM(BUTTON_TERTIARY),
};
#undef JAVA_ENUM_CLASS

#define SOURCE_TOUCHSCREEN   0x1002
#define SOURCE_CLASS_POINTER 0x2
#define SOURCE_MOUSE         0x2002

#define MAX_POINTERS         50

struct pointer {
	int id;
	int index;
	float coord_x;
	float coord_y;
	float raw_x;
	float raw_y;
};

GPtrArray *pointer_indices = NULL;

gpointer canceled_event = NULL;
WrapperWidget *cancel_triggerer = NULL;
int canceled_action = 0;

static struct pointer pointers[MAX_POINTERS] = {};

static GdkEvent *synthetic_propagation_failed = NULL;

/** JavaWidget:
 * Minimal gtk widget class which does nothing.
 * Drawing will be overwritten by WrapperWidget.
 * If it holds children, they will be laid out by AndroidLayout
 */
struct _JavaWidget {
	GtkWidget parent_instance;
};
G_DECLARE_FINAL_TYPE(JavaWidget, java_widget, JAVA, WIDGET, GtkWidget)
G_DEFINE_TYPE(JavaWidget, java_widget, GTK_TYPE_WIDGET)
static void java_widget_dispose(GObject *java_widget)
{
	GtkWidget *widget = GTK_WIDGET(java_widget);
	GtkWidget *child = gtk_widget_get_first_child(widget);
	while (child) {
		GtkWidget *_child = gtk_widget_get_next_sibling(child);
		gtk_widget_unparent(child);
		child = _child;
	}
	/* GtkWidget's dispose releases the layout manager (with the view's weak reference) and the controllers */
	G_OBJECT_CLASS(java_widget_parent_class)->dispose(java_widget);
}
/* like GtkWidget's default, with the elevation shadow of each child below it */
static void java_widget_snapshot(GtkWidget *widget, GdkSnapshot *snapshot)
{
	for (GtkWidget *child = gtk_widget_get_first_child(widget); child; child = gtk_widget_get_next_sibling(child)) {
		wrapper_widget_snapshot_shadow(widget, child, snapshot);
		gtk_widget_snapshot_child(widget, child, snapshot);
	}
}
static void java_widget_init(JavaWidget *java_widget) {}
static void java_widget_class_init(JavaWidgetClass *class)
{
	G_OBJECT_CLASS(class)->dispose = java_widget_dispose;
	GTK_WIDGET_CLASS(class)->snapshot = java_widget_snapshot;
}

JNIEXPORT void JNICALL Java_android_view_View_native_1drawThroughJava(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *wrapper = gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr)));
	if (WRAPPER_IS_WIDGET(wrapper))
		wrapper_widget_draw_through_java(WRAPPER_WIDGET(wrapper), env);
}

/* whether GTK lays the widget out through layoutInternal() (a JavaWidget), or by itself (a native widget) */
JNIEXPORT jboolean JNICALL Java_android_view_View_native_1hasAndroidLayout(JNIEnv *env, jclass class, jlong widget_ptr)
{
	return ATL_IS_ANDROID_LAYOUT(gtk_widget_get_layout_manager(GTK_WIDGET(_PTR(widget_ptr))));
}

bool view_dispatch_motionevent(JNIEnv *env, WrapperWidget *wrapper, GtkPropagationPhase phase, jobject motion_event, gpointer event, int action)
{
	int ret;

	jobject this = wrapper->jobj;
	bool is_synthetic = event == motion_event;

	if (cancel_triggerer == wrapper) { // cancel done
		canceled_event = NULL;
		cancel_triggerer = NULL;
		if (is_synthetic)
			_SET_INT_FIELD(motion_event, "action", canceled_action);
	}

	if (wrapper->custom_dispatch_touch) {
		if (synthetic_propagation_failed != event) { // New event. Try to handle again.
			synthetic_propagation_failed = NULL;
			ret = J__View__dispatchTouchEvent(env, this, motion_event);
			if ((*env)->ExceptionCheck(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
			}
		}
		if (synthetic_propagation_failed) {
			// The app tried to synthetically propagate the event to a GTK widget, but that is not possible in GTK4,
			// so just treat it as unhandled and let GTK propagate it the normal way.
			synthetic_propagation_failed = event;
			ret = false;
		}
	} else if (phase == GTK_PHASE_CAPTURE && _GET_BOOL_FIELD(this, "disallowIntercept")) {
		if (action == ACTION_UP || action == ACTION_CANCEL)
			_SET_BOOL_FIELD(this, "disallowIntercept", false);
		ret = false;
	} else if (phase == GTK_PHASE_CAPTURE && !wrapper->intercepting_touch) {
		GdkEvent *outer = synthetic_propagation_failed;
		synthetic_propagation_failed = NULL;
		wrapper->intercepting_touch = J__View__onInterceptTouchEvent(env, this, motion_event) && !synthetic_propagation_failed;
		synthetic_propagation_failed = outer;
		if (wrapper->intercepting_touch) {
			// store the event that was canceled and let it propagate to the child widgets
			canceled_event = event;
			cancel_triggerer = wrapper;
			if (is_synthetic) {
				canceled_action = _GET_INT_FIELD(motion_event, "action");
				_SET_INT_FIELD(motion_event, "action", ACTION_CANCEL);
			}
		}
		ret = false;
	} else {
		/* a native GTK widget (ScrollView, Button, ...) answers native_gtkEventPropagation(): on this GTK
		 * path that means it doesn't handle the event, GTK propagates it on */
		GdkEvent *outer = synthetic_propagation_failed;
		synthetic_propagation_failed = NULL;
		ret = J__View__onTouchEventInternal(env, this, motion_event, (jboolean)is_synthetic) && !synthetic_propagation_failed;
		synthetic_propagation_failed = outer;
	}

	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	if (action == ACTION_UP || action == ACTION_CANCEL)
		wrapper->intercepting_touch = false;

	if (action == ACTION_CANCEL)
		ret = false;

	return ret;
}

static bool call_ontouch_callback(WrapperWidget *wrapper, int action, struct pointer pointers[MAX_POINTERS], GPtrArray *pointer_indices, GtkPropagationPhase phase, guint32 timestamp, GdkEvent *event, int32_t source, int32_t button_state)
{
	bool ret;
	JNIEnv *env = get_jni_env();

	int num_pointers = pointer_indices->len;
	jintArray ids = (*env)->NewIntArray(env, num_pointers);
	jfloatArray coords = (*env)->NewFloatArray(env, num_pointers * 4);
	for (int i = 0; i < num_pointers; i++) {
		struct pointer *pointer = (struct pointer *)g_ptr_array_index(pointer_indices, i);
		(*env)->SetIntArrayRegion(env, ids, i, 1, &pointer->id);
		/* put in all four float values starting at coord_x */
		(*env)->SetFloatArrayRegion(env, coords, 4 * i, 4, &pointer->coord_x);
	}

	jobject motion_event = J_new__MotionEvent(env, source, action, timestamp, ids, coords);
	_SET_INT_FIELD(motion_event, "buttonState", button_state);

	ret = view_dispatch_motionevent(env, wrapper, phase, motion_event, event, action);

	(*env)->DeleteLocalRef(env, motion_event);

	return ret;
}

static void call_hover_callback(jobject this, int action, float x, float y, float raw_x, float raw_y)
{
	JNIEnv *env = get_jni_env();
	/* the pointer id of the mouse's presses (see on_pointer_event()): hover and press are one pointer, as on
	 * Android, so that the hovering pointer turns into the pressed one instead of staying behind */
	jintArray ids = (*env)->NewIntArray(env, 1);
	(*env)->SetIntArrayRegion(env, ids, 0, 1, &(jint){1});
	jfloatArray coords = (*env)->NewFloatArray(env, 4);
	(*env)->SetFloatArrayRegion(env, coords, 0, 4, (jfloat[]){x, y, raw_x, raw_y});
	jobject motion_event = J_new__MotionEvent(env, SOURCE_MOUSE, action, (jlong)0, ids, coords);
	J__View__dispatchHoverEvent(env, this, motion_event);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

static void transform_coords_to_widget_relative(GtkWidget *widget, double *x, double *y)
{
	int ret;

	graphene_point_t p;
	double off_x;
	double off_y;

	GtkWidget *window = GTK_WIDGET(gtk_widget_get_native(widget));
	gtk_native_get_surface_transform(GTK_NATIVE(window), &off_x, &off_y);
	ret = gtk_widget_compute_point(window, widget, &GRAPHENE_POINT_INIT(*x - off_x, *y - off_y), &p);
	assert(ret);

	*x = p.x;
	*y = p.y;
}

void remove_pointer_fast(GPtrArray *pointer_indices, struct pointer *pointer)
{
	g_ptr_array_remove_index_fast(pointer_indices, pointer->index);
	if (pointer->index != pointer_indices->len) {
		/* update the index field of the pointer that was moved in to fill the empty space */
		struct pointer *replacement_pointer = (struct pointer *)g_ptr_array_index(pointer_indices, pointer->index);
		replacement_pointer->index = pointer->index;
	}
	/* mark as empty, other fields don't matter */
	pointer->id = 0;
}

/* the release event that completed a click whose gesture could not be claimed */
static GdkEvent *action_up_already_handled = NULL;

/* the MotionEvent.BUTTON_* bit of a GDK button number */
static int32_t mouse_button(guint button)
{
	switch (button) {
		case GDK_BUTTON_PRIMARY:
			return BUTTON_PRIMARY;
		case GDK_BUTTON_SECONDARY:
			return BUTTON_SECONDARY;
		case GDK_BUTTON_MIDDLE:
			return BUTTON_TERTIARY;
		default:
			return 0;
	}
}

/* the buttons held before the event, as MotionEvent.getButtonState() reports them */
static int32_t mouse_button_state(GdkEvent *event)
{
	GdkModifierType state = gdk_event_get_modifier_state(event);
	return (state & GDK_BUTTON1_MASK ? BUTTON_PRIMARY : 0) | (state & GDK_BUTTON3_MASK ? BUTTON_SECONDARY : 0) |
	       (state & GDK_BUTTON2_MASK ? BUTTON_TERTIARY : 0);
}

/* Android sends ACTION_BUTTON_PRESS after ACTION_DOWN and ACTION_BUTTON_RELEASE before ACTION_UP, and only
 * these when another button is pressed or released while one is held. They go through
 * dispatchGenericMotionEvent() like the wheel, so only to views that handle generic motion themselves. */
static gboolean call_button_callback(WrapperWidget *wrapper, int action, int32_t action_button, int32_t button_state, struct pointer *pointer, guint32 timestamp)
{
	if (!wrapper->custom_generic_motion)
		return false;
	JNIEnv *env = get_jni_env();
	jobject motion_event = J_new__MotionEvent__single(env, SOURCE_MOUSE, action, (jlong)timestamp, pointer->coord_x, pointer->coord_y, pointer->raw_x, pointer->raw_y);
	_SET_INT_FIELD(motion_event, "buttonState", button_state);
	_SET_INT_FIELD(motion_event, "actionButton", action_button);
	gboolean ret = J__View__dispatchGenericMotionEvent(env, wrapper->jobj, motion_event);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, motion_event);
	return ret;
}

// TODO: find a way to reconcile this with libandroid/input.c?
static gboolean on_pointer_event(GtkEventControllerLegacy *event_controller, GdkEvent *event, gpointer user_data)
{
	double x;
	double y;

	if (event == action_up_already_handled)
		return true;

	uintptr_t id = (uintptr_t)gdk_event_get_event_sequence(event);
	if (id > MAX_POINTERS - 1) // sequence id is a real pointer for drag and drop events
		return false;

	/* FIXME: this will clash with touchscreen */
	if (id == 0)
		id = 1;

	int pointer_index;

	if (pointers[id].id) {
		pointer_index = pointers[id].index;
	} else {
		/* index of a hypothetical next appended element */
		pointer_index = pointer_indices->len;
	}

	int action;
	GdkEventType event_type = gdk_event_get_event_type(event);
	GtkWidget *widget = gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(event_controller));
	WrapperWidget *wrapper = WRAPPER_WIDGET(widget);
	GtkPropagationPhase phase = gtk_event_controller_get_propagation_phase(GTK_EVENT_CONTROLLER(event_controller));
	guint32 timestamp = gdk_event_get_time(event);
	/* A mouse is a mouse on Android too (SOURCE_MOUSE, TOOL_TYPE_MOUSE, getButtonState()): apps and web
	 * content treat it differently from a finger, e.g. a drag selects text instead of turning the page.
	 * Touchscreens send GDK_TOUCH_* events; pens stay touch events as before. */
	GdkInputSource input_source = gdk_device_get_source(gdk_event_get_device(event));
	bool is_mouse = event_type != GDK_TOUCH_BEGIN && event_type != GDK_TOUCH_UPDATE && event_type != GDK_TOUCH_END &&
	                input_source != GDK_SOURCE_PEN && input_source != GDK_SOURCE_TOUCHSCREEN;
	int32_t button_state = is_mouse ? mouse_button_state(event) : 0;
	int32_t action_button = 0;
	switch (event_type) {
		case GDK_BUTTON_PRESS:
			if (is_mouse) {
				action_button = mouse_button(gdk_button_event_get_button(event));
				if (!action_button)
					return false;
				if (button_state) // another button is held
					return phase == GTK_PHASE_BUBBLE && call_button_callback(wrapper, ACTION_BUTTON_PRESS, action_button, button_state | action_button, &pointers[id], timestamp);
				button_state |= action_button;
			}
		case GDK_TOUCH_BEGIN:
			action = pointer_index > 0 ? (ACTION_POINTER_DOWN | (pointer_index << 8)) : ACTION_DOWN;
			break;
		case GDK_BUTTON_RELEASE:
			if (is_mouse) {
				action_button = mouse_button(gdk_button_event_get_button(event));
				button_state &= ~action_button;
				if (!action_button)
					return false;
				if (button_state) // another button is still held
					return phase == GTK_PHASE_BUBBLE && call_button_callback(wrapper, ACTION_BUTTON_RELEASE, action_button, button_state, &pointers[id], timestamp);
			}
		case GDK_TOUCH_END:
			action = pointer_index > 0 ? (ACTION_POINTER_UP | (pointer_index << 8)) : ACTION_UP;
			break;
		case GDK_MOTION_NOTIFY:
			if (!(gdk_event_get_modifier_state(event) & (is_mouse ? GDK_BUTTON1_MASK | GDK_BUTTON2_MASK | GDK_BUTTON3_MASK : GDK_BUTTON1_MASK)))
				return false;
		case GDK_TOUCH_UPDATE:
			action = ACTION_MOVE;
			break;
		default: // not a touch or mouse event, nothing to do here
			return false;
	}

	if (id >= MAX_POINTERS) {
		fprintf(stderr, "exiting - event sequence %w64d larger than %d, did Gtk implementation change?", id, MAX_POINTERS);
		exit(1);
	}

	double raw_x;
	double raw_y;
	gdk_event_get_position(event, &raw_x, &raw_y);
	x = raw_x, y = raw_y;
	transform_coords_to_widget_relative(widget, &x, &y);

	if (!pointers[id].id) {
		/* if this is a new sequence, add a slot for it */
		g_ptr_array_add(pointer_indices, &pointers[id]);
		/* we appended a single element, so it must be at pointer_index */
		pointers[id].index = pointer_index;
		pointers[id].id = id;
	}

	pointers[id].coord_x = x;
	pointers[id].coord_y = y;
	pointers[id].raw_x = raw_x;
	pointers[id].raw_y = raw_y;

	if (event == canceled_event && cancel_triggerer != wrapper) {
		action = ACTION_CANCEL;
	}

	gboolean ret;
	if (is_mouse && action == ACTION_UP && phase == GTK_PHASE_BUBBLE)
		call_button_callback(wrapper, ACTION_BUTTON_RELEASE, action_button, button_state, &pointers[id], timestamp);
	ret = call_ontouch_callback(wrapper, action, pointers, pointer_indices, phase, timestamp, event, is_mouse ? SOURCE_MOUSE : SOURCE_TOUCHSCREEN, button_state);
	if (is_mouse && action == ACTION_DOWN && phase == GTK_PHASE_BUBBLE)
		call_button_callback(wrapper, ACTION_BUTTON_PRESS, action_button, button_state, &pointers[id], timestamp);
	if (event_type == GDK_BUTTON_RELEASE || event_type == GDK_TOUCH_END) {
		remove_pointer_fast(pointer_indices, &pointers[id]);
	}
	if (pointer_indices->len == 0 && wrapper->hover_exit_pending) {
		wrapper->hover_exit_pending = FALSE;
		call_hover_callback(wrapper->jobj, ACTION_HOVER_EXIT, x, y, raw_x, raw_y);
	}
	return ret;
}

static gboolean on_event(GtkEventControllerLegacy *event_controller, GdkEvent *event, gpointer user_data)
{
	GdkEventType event_type = gdk_event_get_event_type(event);
	switch (event_type) {
		case GDK_BUTTON_PRESS:
		case GDK_TOUCH_BEGIN:
		case GDK_BUTTON_RELEASE:
		case GDK_TOUCH_END:
		case GDK_MOTION_NOTIFY:
		case GDK_TOUCH_UPDATE:
			return on_pointer_event(event_controller, event, user_data);
		default: // not a touch or mouse event, nothing to do here
			return false;
	}
}

static void on_click(GtkGestureClick *gesture, int n_press, double x, double y, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	GtkWidget *widget = gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(gesture));
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(widget));

	if (!J__View__isClickable(env, wrapper->jobj))
		return;
	jboolean handled = J__View__performClick(env, wrapper->jobj);

	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	if (handled) {
		gtk_gesture_set_state(GTK_GESTURE(gesture), GTK_EVENT_SEQUENCE_CLAIMED);
		// claiming fails if there is no last event, set a flag in that case
		GdkEvent *event = gtk_gesture_get_last_event(GTK_GESTURE(gesture), gtk_gesture_get_last_updated_sequence(GTK_GESTURE(gesture)));
		if (!event) {
			/* only the click's own release must not reach the other touch listeners; a flag that
			 * is only reset by the next release would swallow the release of the next click
			 * whenever this release does not reach another touch listener (e.g. because the
			 * click removed the view) */
			g_clear_pointer(&action_up_already_handled, gdk_event_unref);
			event = gtk_event_controller_get_current_event(GTK_EVENT_CONTROLLER(gesture));
			if (event)
				action_up_already_handled = gdk_event_ref(event);
		}
	}
}

#define MAGIC_SCROLL_FACTOR 32

/* Gtk4 for whatever reason doesn't report cursor position for GtkEventControllerScroll, thankfully
 * it's not too annoying to get it ourselves */
gboolean hacky_get_cursor_position(GtkWidget *widget, double *x, double *y)
{
	GdkSeat *seat;
	GdkSurface *surface;
	GdkDevice *device_pointer;

	seat = gdk_display_get_default_seat(gtk_widget_get_display(widget));

	device_pointer = gdk_seat_get_pointer(seat);
	surface = gtk_native_get_surface(gtk_widget_get_native(widget));

	return gdk_surface_get_device_position(surface,
	                                       device_pointer,
	                                       x, y, NULL);
}

static gboolean scroll_cb(GtkEventControllerScroll *controller, gdouble dx, gdouble dy, WrapperWidget *wrapper)
{
	double x;
	double y;
	double raw_x;
	double raw_y;

	JNIEnv *env = get_jni_env();
	GdkScrollUnit scroll_unit = gtk_event_controller_scroll_get_unit(controller);
	GdkEvent *event = gtk_event_controller_get_current_event(GTK_EVENT_CONTROLLER(controller));
	GtkWidget *widget = gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(controller));
	jobject this = (*env)->NewLocalRef(env, wrapper->jobj);
	if (!this)
		return FALSE;
	hacky_get_cursor_position(widget, &raw_x, &raw_y);
	x = raw_x, y = raw_y;
	transform_coords_to_widget_relative(widget, &x, &y);

	if (scroll_unit == GDK_SCROLL_UNIT_SURFACE) {
		dx /= MAGIC_SCROLL_FACTOR;
		dy /= MAGIC_SCROLL_FACTOR;
	}
	jobject motion_event = J_new__MotionEvent__scroll(env, SOURCE_MOUSE, ACTION_SCROLL,
	                                                  gdk_event_get_time(event), x, y, raw_x, raw_y, dx, -dy);

	gboolean ret = J__View__dispatchGenericMotionEvent(env, this, motion_event);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	// HACK: replay last hover event to work around scrolling issues in composeUI apps
	if (wrapper->hover_x && wrapper->hover_y)
		call_hover_callback(this, ACTION_HOVER_MOVE, wrapper->hover_x, wrapper->hover_y, wrapper->hover_x, wrapper->hover_y);

	(*env)->DeleteLocalRef(env, motion_event);
	(*env)->DeleteLocalRef(env, this);
	return ret;
}

/* the pointer position in surface coordinates, like gdk_event_get_position(); GTK also emits
 * crossing signals without an event (when a widget appears under the pointer, for instance) */
static void get_raw_position(GtkEventControllerMotion *controller, WrapperWidget *wrapper, double x, double y, double *raw_x, double *raw_y)
{
	GdkEvent *event = gtk_event_controller_get_current_event(GTK_EVENT_CONTROLLER(controller));
	if (event) {
		gdk_event_get_position(event, raw_x, raw_y);
		return;
	}
	GtkNative *native = gtk_widget_get_native(GTK_WIDGET(wrapper));
	graphene_point_t p = GRAPHENE_POINT_INIT(x, y);
	double off_x = 0, off_y = 0;
	if (native) {
		gtk_widget_compute_point(GTK_WIDGET(wrapper), GTK_WIDGET(native), &GRAPHENE_POINT_INIT(x, y), &p);
		gtk_native_get_surface_transform(native, &off_x, &off_y);
	}
	*raw_x = p.x + off_x;
	*raw_y = p.y + off_y;
}

static void hover_enter_cb(GtkEventControllerMotion *controller, double x, double y, WrapperWidget *wrapper)
{
	double raw_x;
	double raw_y;
	get_raw_position(controller, wrapper, x, y, &raw_x, &raw_y);
	wrapper->hover_exit_pending = FALSE;
	wrapper->hover_x = x;
	wrapper->hover_y = y;
	/* like Android's InputDispatcher, enter on the first motion: GTK also sends crossings without one, e.g.
	 * when a popup closes on a press outside of it, and with that hover Compose lost the next mouse press */
	wrapper->hover_enter_pending = TRUE;
}

static void hover_leave_cb(GtkEventControllerMotion *controller, WrapperWidget *wrapper)
{
	if (wrapper->hover_enter_pending) { // it never entered
		wrapper->hover_enter_pending = FALSE;
		return;
	}
	if (pointer_indices->len) // composeUI apps don't like ACTION_HOVER_EXIT events while a pointer is down
		wrapper->hover_exit_pending = TRUE;
	else
		call_hover_callback(wrapper->jobj, ACTION_HOVER_EXIT, 0, 0, 0, 0);
}

static void hover_motion_cb(GtkEventControllerMotion *controller, double x, double y, WrapperWidget *wrapper)
{
	double raw_x;
	double raw_y;
	if (pointer_indices && pointer_indices->len) // like Android: the pointer is down, it doesn't hover
		return;
	get_raw_position(controller, wrapper, x, y, &raw_x, &raw_y);
	wrapper->hover_x = x;
	wrapper->hover_y = y;
	if (wrapper->hover_enter_pending) {
		wrapper->hover_enter_pending = FALSE;
		call_hover_callback(wrapper->jobj, ACTION_HOVER_ENTER, x, y, raw_x, raw_y);
	}
	call_hover_callback(wrapper->jobj, ACTION_HOVER_MOVE, x, y, raw_x, raw_y);
}

void _setOnTouchListener(JNIEnv *env, jobject this, GtkWidget *widget)
{
	GtkEventController *old_controller = g_object_get_data(G_OBJECT(widget), "on_touch_listener");
	if (old_controller)
		return;

	if (!pointer_indices)
		pointer_indices = g_ptr_array_new_full(20, NULL);

	GtkEventController *controller = GTK_EVENT_CONTROLLER(gtk_event_controller_legacy_new());
	gtk_event_controller_set_propagation_phase(controller, GTK_PHASE_BUBBLE);

	g_signal_connect(controller, "event", G_CALLBACK(on_event), NULL);
	gtk_widget_add_controller(widget, controller);
	g_object_set_data(G_OBJECT(widget), "on_touch_listener", controller);

	WrapperWidget *wrapper = WRAPPER_WIDGET(widget);
	jmethodID onintercepttouchevent_method = _METHOD(_CLASS(this), "onInterceptTouchEvent", "(Landroid/view/MotionEvent;)Z");
	if (onintercepttouchevent_method != _CACHED_METHOD(View__onInterceptTouchEvent) || wrapper->custom_dispatch_touch) {
		GtkEventController *old_controller = g_object_get_data(G_OBJECT(widget), "on_intercept_touch_listener");
		if (old_controller)
			gtk_widget_remove_controller(widget, old_controller);

		GtkEventController *controller = GTK_EVENT_CONTROLLER(gtk_event_controller_legacy_new());
		gtk_event_controller_set_propagation_phase(controller, GTK_PHASE_CAPTURE);

		g_signal_connect(controller, "event", G_CALLBACK(on_event), NULL);
		gtk_widget_add_controller(widget, controller);
		g_object_set_data(G_OBJECT(widget), "on_intercept_touch_listener", controller);
	}

	if (!wrapper->needs_allocation && (wrapper->real_width || wrapper->real_height))
		gtk_widget_size_allocate(GTK_WIDGET(wrapper), &(GtkAllocation){.x = 0, .y = 0, .width = wrapper->real_width, .height = wrapper->real_height}, 0);

	wrapper->needs_allocation = true;
	gtk_widget_set_overflow(GTK_WIDGET(wrapper), GTK_OVERFLOW_HIDDEN);
}

JNIEXPORT void JNICALL Java_android_view_View_native_1setCanTarget(JNIEnv *env, jobject this, jlong widget_ptr, jboolean can_target)
{
	gtk_widget_set_can_target(gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr))), can_target);
}

JNIEXPORT void JNICALL Java_android_view_View_nativeSetOnTouchListener(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *widget = gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr)));

	_setOnTouchListener(env, this, widget);
}

JNIEXPORT void JNICALL Java_android_view_View_nativeSetOnClickListener(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));

	GtkEventController *old_controller = g_object_get_data(G_OBJECT(widget), "on_click_listener");
	if (old_controller)
		return;

	GtkEventController *controller = GTK_EVENT_CONTROLLER(gtk_gesture_click_new());

	g_signal_connect(controller, "released", G_CALLBACK(on_click), NULL); // the release completes the click, I guess?
	gtk_widget_add_controller(widget, controller);
	g_object_set_data(G_OBJECT(widget), "on_click_listener", controller);
	widget_set_needs_allocation(widget);
}

JNIEXPORT jint JNICALL Java_android_view_View_getWidth(JNIEnv *env, jobject this)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(_GET_LONG_FIELD(this, "widget")));

	if (ATL_IS_ANDROID_LAYOUT(gtk_widget_get_layout_manager(widget))) {
		AndroidLayout *layout = ATL_ANDROID_LAYOUT(gtk_widget_get_layout_manager(widget));
		return layout->real_width;
	}
	return gtk_widget_get_width(widget);
}

JNIEXPORT jint JNICALL Java_android_view_View_getHeight(JNIEnv *env, jobject this)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(_GET_LONG_FIELD(this, "widget")));

	if (ATL_IS_ANDROID_LAYOUT(gtk_widget_get_layout_manager(widget))) {
		AndroidLayout *layout = ATL_ANDROID_LAYOUT(gtk_widget_get_layout_manager(widget));
		return layout->real_height;
	}
	return gtk_widget_get_height(widget);
}

#define GRAVITY_TOP               (1 << 5) //0x30
#define GRAVITY_BOTTOM            (1 << 6) //0x50
#define GRAVITY_LEFT              (1 << 1) //0x3
#define GRAVITY_RIGHT             (1 << 2) //0x5

#define GRAVITY_CENTER_VERTICAL   0x10
#define GRAVITY_CENTER_HORIZONTAL 0x01

#define GRAVITY_CENTER            (GRAVITY_CENTER_VERTICAL | GRAVITY_CENTER_HORIZONTAL)

#define MATCH_PARENT              (-1)

JNIEXPORT void JNICALL Java_android_view_View_native_1setLayoutParams(JNIEnv *env, jobject this, jlong widget_ptr, jint width, jint height, jint gravity, jfloat weight,
                                                                      jint leftMargin, jint topMargin, jint rightMargin, jint bottomMargin)
{
	GtkWidget *widget = gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr)));

	GtkAlign halign = GTK_ALIGN_FILL;
	GtkAlign valign = GTK_ALIGN_FILL;
	gboolean hexpand = FALSE;
	gboolean vexpand = FALSE;

	if (gravity != -1) {
		if (gravity & GRAVITY_BOTTOM)
			valign = GTK_ALIGN_END;
		else if (gravity & GRAVITY_TOP)
			valign = GTK_ALIGN_START;
		else
			valign = GTK_ALIGN_FILL;

		if (gravity & GRAVITY_RIGHT)
			halign = GTK_ALIGN_END;
		else if (gravity & GRAVITY_LEFT)
			halign = GTK_ALIGN_START;
		else
			halign = GTK_ALIGN_FILL;

		if (gravity == GRAVITY_CENTER) {
			valign = GTK_ALIGN_CENTER; // GTK_ALIGN_CENTER doesn't seem to be the right one?
			halign = GTK_ALIGN_CENTER; // ditto (GTK_ALIGN_CENTER)
			hexpand = TRUE;            // haxx or not?
			vexpand = TRUE;            // seems to be the deciding factor for whether to expand, guess I should try on android
		}
	}

	if (weight > 0.f) {
		hexpand = TRUE;
		vexpand = TRUE;
	}

	if (width == MATCH_PARENT) {
		hexpand = true;
		halign = GTK_ALIGN_FILL;
	}
	if (height == MATCH_PARENT) {
		vexpand = true;
		valign = GTK_ALIGN_FILL;
	}

	gtk_widget_set_hexpand(widget, hexpand);
	gtk_widget_set_vexpand(widget, vexpand);
	gtk_widget_set_halign(widget, halign);
	gtk_widget_set_valign(widget, valign);

	if (width > 0)
		g_object_set(G_OBJECT(widget), "width-request", width, NULL);
	if (height > 0)
		g_object_set(G_OBJECT(widget), "height-request", height, NULL);

	GtkWidget *parent = gtk_widget_get_parent(widget);
	// if parent is Java widget, it will handle the margins by itself
	if (parent && !ATL_IS_ANDROID_LAYOUT(gtk_widget_get_layout_manager(parent))) {
		gtk_widget_set_margin_start(widget, leftMargin);
		gtk_widget_set_margin_top(widget, topMargin);
		gtk_widget_set_margin_end(widget, rightMargin);
		gtk_widget_set_margin_bottom(widget, bottomMargin);
	}

	GtkLayoutManager *layout_manager = gtk_widget_get_layout_manager(WRAPPER_WIDGET(widget)->child);
	if (ATL_IS_ANDROID_LAYOUT(layout_manager))
		android_layout_set_params(ATL_ANDROID_LAYOUT(layout_manager), width, height);

	wrapper_widget_set_layout_params(WRAPPER_WIDGET(widget), width, height);

	atl_ensure_widget_snapshotability(widget);
}

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
JNIEXPORT void JNICALL Java_android_view_View_native_1setPadding(JNIEnv *env, jobject this, jlong widget_ptr, jint left, jint top, jint right, jint bottom)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	if (ATL_IS_ANDROID_LAYOUT(gtk_widget_get_layout_manager(widget))) {
		return;
	}

	GtkStyleContext *style_context = gtk_widget_get_style_context(widget);
	GtkCssProvider *old_provider = g_object_get_data(G_OBJECT(widget), "padding_style_provider");
	if (old_provider)
		gtk_style_context_remove_provider(style_context, GTK_STYLE_PROVIDER(old_provider));
	GtkCssProvider *css_provider = gtk_css_provider_new();

	char *css_string = g_markup_printf_escaped("*{ padding-left: %dpx; padding-top: %dpx; padding-right: %dpx; padding-bottom: %dpx; }", left, top, right, bottom);
	gtk_css_provider_load_from_string(css_provider, css_string);
	g_free(css_string);

	gtk_style_context_add_provider(style_context, GTK_STYLE_PROVIDER(css_provider), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
	g_object_set_data(G_OBJECT(widget), "padding_style_provider", css_provider);
}

JNIEXPORT void JNICALL Java_android_view_View_native_1setVisibility(JNIEnv *env, jobject this, jlong widget_ptr, jint visibility, jfloat alpha, jboolean sensitive)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	GtkWidget *wrapper = gtk_widget_get_parent(widget);

	atl_safe_gtk_widget_set_visible(wrapper, visibility != android_view_View_GONE);
	gtk_widget_set_opacity(wrapper, (visibility != android_view_View_INVISIBLE) * alpha);
	if (!JAVA_IS_WIDGET(widget))
		gtk_widget_set_sensitive(wrapper, sensitive && visibility != android_view_View_INVISIBLE && alpha != 0.0f);
}

JNIEXPORT jlong JNICALL Java_android_view_View_native_1constructor(JNIEnv *env, jobject this, jobject context, jobject attrs)
{
	WrapperWidget *wrapper = g_object_ref_sink(WRAPPER_WIDGET(wrapper_widget_new()));
	GtkWidget *widget = g_object_new(java_widget_get_type(), NULL);
	gtk_widget_set_layout_manager(widget, android_layout_new());
	wrapper_widget_set_child(WRAPPER_WIDGET(wrapper), widget);
	wrapper_widget_set_jobject(WRAPPER_WIDGET(wrapper), env, this);

	jclass class = _CLASS(this);
	jstring name_jstr = J__Class__getName(env, class);
	const char *name = (*env)->GetStringUTFChars(env, name_jstr, NULL);
	gtk_widget_set_name(widget, name);
	(*env)->ReleaseStringUTFChars(env, name_jstr, name);

	if (_METHOD(class, "onGenericMotionEvent", "(Landroid/view/MotionEvent;)Z") != _CACHED_METHOD(View__onGenericMotionEvent)
	    || _METHOD(class, "dispatchGenericMotionEvent", "(Landroid/view/MotionEvent;)Z") != _CACHED_METHOD(View__dispatchGenericMotionEvent)) {
		GtkEventController *controller = gtk_event_controller_scroll_new(GTK_EVENT_CONTROLLER_SCROLL_VERTICAL);

		g_signal_connect(controller, "scroll", G_CALLBACK(scroll_cb), wrapper);
		gtk_widget_add_controller(GTK_WIDGET(wrapper), controller);
		wrapper->custom_generic_motion = TRUE;
	}
	if (_METHOD(class, "dispatchHoverEvent", "(Landroid/view/MotionEvent;)Z") != _CACHED_METHOD(View__dispatchHoverEvent)) {
		GtkEventController *controller = gtk_event_controller_motion_new();

		g_signal_connect(controller, "enter", G_CALLBACK(hover_enter_cb), wrapper);
		g_signal_connect(controller, "leave", G_CALLBACK(hover_leave_cb), wrapper);
		g_signal_connect(controller, "motion", G_CALLBACK(hover_motion_cb), wrapper);
		gtk_widget_add_controller(GTK_WIDGET(wrapper), controller);
	}

	return _INTPTR(widget);
}

JNIEXPORT void JNICALL Java_android_view_View_nativeInvalidate(JNIEnv *env, jclass class, jlong widget_ptr)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));

	wrapper_widget_queue_draw(WRAPPER_WIDGET(gtk_widget_get_parent(widget)));
}

JNIEXPORT void JNICALL Java_android_view_View_native_1setAnimating(JNIEnv *env, jobject this, jlong widget_ptr, jboolean animating)
{
	wrapper_widget_set_animating(WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr)))), animating);
}

/* called from View.finalize(), on the FinalizerDaemon thread */
static gboolean release_wrapper(gpointer data)
{
	WrapperWidget *wrapper = data;
	JNIEnv *env = get_jni_env();
	if (wrapper->jobj)
		_WEAK_UNREF(wrapper->jobj);
	wrapper->jobj = NULL;
	g_object_unref(wrapper);
	return G_SOURCE_REMOVE;
}

/* called from the finalizer: the view is gone, so drop its weak reference on the main thread along
 * with the unref, instead of only once GTK lets go of the widget (a parent may keep it for long) */
JNIEXPORT void JNICALL Java_android_view_View_native_1destructor(JNIEnv *env, jobject this, jlong widget_ptr)
{
	g_idle_add(release_wrapper, WRAPPER_WIDGET(gtk_widget_get_parent(_PTR(widget_ptr))));
}

#define MEASURE_SPEC_UNSPECIFIED ((int)(0u << 30))
#define MEASURE_SPEC_EXACTLY     ((int)(1u << 30))
#define MEASURE_SPEC_AT_MOST     ((int)(2u << 30)) /* unsigned: no shift into the sign bit */
#define MEASURE_SPEC_MASK        ((int)(0x3u << 30))

JNIEXPORT void JNICALL Java_android_view_View_native_1measure(JNIEnv *env, jobject this, jlong widget_ptr, jint width_spec, jint height_spec)
{
	int width = -1;
	int height = -1;
	GtkWidget *widget = gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr)));
	int width_spec_size = width_spec & ~MEASURE_SPEC_MASK;
	int height_spec_size = height_spec & ~MEASURE_SPEC_MASK;
	int width_spec_mode = width_spec & MEASURE_SPEC_MASK;
	int height_spec_mode = height_spec & MEASURE_SPEC_MASK;
	GtkSizeRequestMode request_mode;

	if (width_spec_mode == MEASURE_SPEC_EXACTLY) {
		width = width_spec_size;
		request_mode = GTK_SIZE_REQUEST_HEIGHT_FOR_WIDTH;
	}
	if (height_spec_mode == MEASURE_SPEC_EXACTLY) {
		height = height_spec_size;
		request_mode = GTK_SIZE_REQUEST_WIDTH_FOR_HEIGHT;
	}
	if (width == -1 && height == -1)
		request_mode = gtk_widget_get_request_mode(widget);
	if (width == -1 && request_mode == GTK_SIZE_REQUEST_HEIGHT_FOR_WIDTH) {
		gtk_widget_measure(widget, GTK_ORIENTATION_HORIZONTAL, height, NULL, &width, NULL, NULL);
		if (width_spec_mode == MEASURE_SPEC_AT_MOST && width > width_spec_size)
			width = width_spec_size;
	}
	if (height == -1) {
		gtk_widget_measure(widget, GTK_ORIENTATION_VERTICAL, width, NULL, &height, NULL, NULL);
		if (height_spec_mode == MEASURE_SPEC_AT_MOST && height > height_spec_size)
			height = height_spec_size;
	}
	if (width == -1) {
		gtk_widget_measure(widget, GTK_ORIENTATION_HORIZONTAL, height, NULL, &width, NULL, NULL);
		if (width_spec_mode == MEASURE_SPEC_AT_MOST && width > width_spec_size)
			width = width_spec_size;
	}

	J__View__setMeasuredDimension(env, this, width, height);
}

JNIEXPORT void JNICALL Java_android_view_View_native_1layout(JNIEnv *env, jobject this, jlong widget_ptr, jint l, jint t, jint r, jint b)
{
	GtkWidget *widget = gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr)));

	GtkAllocation allocation = {
		.x = l,
		.y = t,
	};
	int width = r - l;
	int height = b - t;
	WrapperWidget *wrapper = WRAPPER_WIDGET(widget);
	if (wrapper->real_width != width || wrapper->real_height != height) {
		wrapper->real_width = width;
		wrapper->real_height = height;
		if (!wrapper->needs_allocation)
			gtk_widget_queue_allocate(widget);
	}
	if (wrapper->needs_allocation) {
		allocation.width = width;
		allocation.height = height;
	}

	if (wrapper->jobj) {
		allocation.x += _GET_FLOAT_FIELD(wrapper->jobj, "translationX");
		allocation.y += _GET_FLOAT_FIELD(wrapper->jobj, "translationY");
	}

	/* scale and rotation about the pivot like AOSP's RenderNode (View.getMatrix()); GTK draws and
	 * picks the widget through this transform */
	GskTransform *transform = gsk_transform_translate(NULL, &GRAPHENE_POINT_INIT(allocation.x, allocation.y));
	if (wrapper->jobj && _GET_BOOL_FIELD(wrapper->jobj, "hasTransform")) {
		graphene_point_t pivot = GRAPHENE_POINT_INIT(width / 2.f, height / 2.f);
		if (_GET_BOOL_FIELD(wrapper->jobj, "pivotExplicit"))
			pivot = GRAPHENE_POINT_INIT(_GET_FLOAT_FIELD(wrapper->jobj, "pivotX"), _GET_FLOAT_FIELD(wrapper->jobj, "pivotY"));
		transform = gsk_transform_translate(transform, &pivot);
		transform = gsk_transform_rotate(transform, _GET_FLOAT_FIELD(wrapper->jobj, "rotation"));
		transform = gsk_transform_scale(transform, _GET_FLOAT_FIELD(wrapper->jobj, "scaleX"), _GET_FLOAT_FIELD(wrapper->jobj, "scaleY"));
		transform = gsk_transform_translate(transform, &GRAPHENE_POINT_INIT(-pivot.x, -pivot.y));
	}
	gtk_widget_allocate(widget, allocation.width, allocation.height, -1, transform);
}

JNIEXPORT void JNICALL Java_android_view_View_native_1requestLayout(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));

	atl_safe_gtk_widget_queue_resize(widget);
}

/* we kinda need per-widget css */
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
JNIEXPORT void JNICALL Java_android_view_View_native_1setBackgroundColor(JNIEnv *env, jobject this, jlong widget_ptr, jint color)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(widget));
	wrapper_widget_set_background(wrapper, NULL);

	GtkStyleContext *style_context = gtk_widget_get_style_context(widget);

	GtkCssProvider *old_provider = g_object_get_data(G_OBJECT(widget), "background_style_provider");
	if (old_provider)
		gtk_style_context_remove_provider(style_context, GTK_STYLE_PROVIDER(old_provider));

	GtkCssProvider *css_provider = gtk_css_provider_new();

	char *css_string = g_markup_printf_escaped("* { background-image: none; background-color: #%06x%02x; }", color & 0xFFFFFF, (color >> 24) & 0xFF);
	gtk_css_provider_load_from_string(css_provider, css_string);
	g_free(css_string);

	gtk_style_context_add_provider(style_context, GTK_STYLE_PROVIDER(css_provider), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
	g_object_set_data(G_OBJECT(widget), "background_style_provider", css_provider);
	if (((color >> 24) & 0xFF) != 0)
		widget_set_needs_allocation(widget);
}

JNIEXPORT void JNICALL Java_android_view_View_native_1setShadow(JNIEnv *env, jobject this, jlong widget_ptr, jint left, jint top, jint right, jint bottom, jfloat radius, jfloat z, jfloat alpha)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	wrapper_widget_set_shadow(WRAPPER_WIDGET(gtk_widget_get_parent(widget)), &GRAPHENE_RECT_INIT(left, top, right - left, bottom - top), radius, z, alpha);
}

JNIEXPORT void JNICALL Java_android_view_View_native_1setClipBounds(JNIEnv *env, jobject this, jlong widget_ptr, jboolean enabled, jint left, jint top, jint right, jint bottom)
{
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr))));
	wrapper->has_clip_bounds = enabled;
	wrapper->clip_bounds = GRAPHENE_RECT_INIT(left, top, right - left, bottom - top);
	gtk_widget_queue_draw(GTK_WIDGET(wrapper));
}

JNIEXPORT void JNICALL Java_android_view_View_native_1setOutlineClip(JNIEnv *env, jobject this, jlong widget_ptr, jboolean enabled, jint left, jint top, jint right, jint bottom, jfloat radius, jlong path_ptr)
{
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr))));
	wrapper_widget_set_outline_clip(wrapper, enabled, &GRAPHENE_RECT_INIT(left, top, right - left, bottom - top), radius, _PTR(path_ptr));
}

JNIEXPORT void JNICALL Java_android_view_View_native_1setBackgroundDrawable(JNIEnv *env, jobject this, jlong widget_ptr, jlong paintable_ptr)
{
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	GdkPaintable *paintable = GDK_PAINTABLE(_PTR(paintable_ptr));
	wrapper_widget_set_background(WRAPPER_WIDGET(gtk_widget_get_parent(widget)), paintable);
	GtkStyleContext *style_context = gtk_widget_get_style_context(widget);

	GtkCssProvider *old_provider = g_object_get_data(G_OBJECT(widget), "background_style_provider");
	if (old_provider)
		gtk_style_context_remove_provider(style_context, GTK_STYLE_PROVIDER(old_provider));

	GtkCssProvider *css_provider = gtk_css_provider_new();

	char *css_string = g_markup_printf_escaped("* { background-image: none; background-color: #00000000; }");
	gtk_css_provider_load_from_string(css_provider, css_string);
	g_free(css_string);

	gtk_style_context_add_provider(style_context, GTK_STYLE_PROVIDER(css_provider), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
	g_object_set_data(G_OBJECT(widget), "background_style_provider", css_provider);
	if (paintable)
		widget_set_needs_allocation(widget);
}
#pragma GCC diagnostic pop

JNIEXPORT jboolean JNICALL Java_android_view_View_native_1getGlobalVisibleRect(JNIEnv *env, jobject this, jlong widget_ptr, jobject rect)
{
	GtkWidget *widget = gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr)));
	graphene_point_t point_in = {0, 0};
	graphene_point_t point_out;
	gboolean ret;
	ret = atl_compute_point_in_window(widget, &point_in, &point_out);
	if (!ret)
		return false;
	_SET_INT_FIELD(rect, "left", point_out.x);
	_SET_INT_FIELD(rect, "top", point_out.y);
	point_in = (graphene_point_t){Java_android_view_View_getWidth(env, this), Java_android_view_View_getHeight(env, this)};
	ret = atl_compute_point_in_window(widget, &point_in, &point_out);
	if (!ret)
		return false;
	_SET_INT_FIELD(rect, "right", point_out.x);
	_SET_INT_FIELD(rect, "bottom", point_out.y);
	return true;
}

static void on_long_click(GtkGestureLongPress *gesture, double x, double y, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	GtkWidget *widget = gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(gesture));
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(widget));

	bool ret = J__View__performLongClick(env, wrapper->jobj, x, y);

	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	if (ret)
		gtk_gesture_set_state(GTK_GESTURE(gesture), GTK_EVENT_SEQUENCE_CLAIMED);
}

static void on_long_click_update(GtkGesture *gesture, GdkEventSequence *sequence, gpointer user_data)
{
	GdkEvent *event = gtk_gesture_get_last_event(gesture, sequence);
	if (event == canceled_event) {
		gtk_event_controller_reset(GTK_EVENT_CONTROLLER(gesture));
	}
}

JNIEXPORT void JNICALL Java_android_view_View_nativeSetOnLongClickListener(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	GtkEventController *old_controller = g_object_get_data(G_OBJECT(widget), "on_long_click_listener");
	if (old_controller)
		return;

	GtkEventController *controller = GTK_EVENT_CONTROLLER(gtk_gesture_long_press_new());

	g_signal_connect(controller, "pressed", G_CALLBACK(on_long_click), NULL);
	g_signal_connect(controller, "update", G_CALLBACK(on_long_click_update), NULL);
	gtk_widget_add_controller(widget, controller);
	g_object_set_data(G_OBJECT(widget), "on_long_click_listener", controller);
}

JNIEXPORT jboolean JNICALL Java_android_view_View_native_1getMatrix(JNIEnv *env, jobject this, jlong widget_ptr, jlong matrix_ptr)
{
	GtkWidget *widget = gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr)));
	return gtk_widget_compute_transform(gtk_widget_get_parent(widget), widget, _PTR(matrix_ptr));
}

JNIEXPORT void JNICALL Java_android_view_View_native_1queueAllocate(JNIEnv *env, jobject this, jlong widget_ptr)
{
	atl_safe_gtk_widget_queue_allocate(GTK_WIDGET(_PTR(widget_ptr)));
}

JNIEXPORT void JNICALL Java_android_view_View_native_1drawBackground(JNIEnv *env, jobject this, jlong widget_ptr, jlong snapshot_ptr)
{
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr))));
	GdkSnapshot *snapshot = GDK_SNAPSHOT(_PTR(snapshot_ptr));
	if (wrapper->background)
		gtk_widget_snapshot_child(&wrapper->parent_instance, wrapper->background, snapshot);
}

JNIEXPORT void JNICALL Java_android_view_View_native_1drawContent(JNIEnv *env, jobject this, jlong widget_ptr, jlong snapshot_ptr)
{
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr))));
	GdkSnapshot *snapshot = GDK_SNAPSHOT(_PTR(snapshot_ptr));
	gtk_widget_snapshot_child(&wrapper->parent_instance, wrapper->child, snapshot);
}

JNIEXPORT void JNICALL Java_android_view_View_nativeSetFullscreen(JNIEnv *env, jobject this, jlong widget_ptr, jboolean fullscreen)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	GtkWindow *window = GTK_WINDOW(gtk_widget_get_native(widget));
	if (!window) // not attached to a window
		return;
	if (fullscreen) {
		if (gtk_window_is_maximized(window)) {
			gtk_window_fullscreen(window);
			// Android apps expect the fullscreen to happen immediately, so we need to mock the fullscreened window size
			if (ATL_IS_ANDROID_LAYOUT(gtk_widget_get_layout_manager(widget))) {
				AndroidLayout *layout = ATL_ANDROID_LAYOUT(gtk_widget_get_layout_manager(widget));
				GdkSurface *surface = gtk_native_get_surface(GTK_NATIVE(window));
				GdkDisplay *display = gdk_surface_get_display(surface);
				GdkMonitor *monitor = gdk_display_get_monitor_at_surface(display, surface);
				GdkRectangle geometry;
				gdk_monitor_get_geometry(monitor, &geometry);
				// check if fullscreen is slightly larger than the window, if so, we can safely mock the new size
				if (layout->real_width <= geometry.width && layout->real_width > geometry.width - 200 && layout->real_height <= geometry.height && layout->real_height > geometry.height - 200) {
					layout->real_width = geometry.width;
					layout->real_height = geometry.height;
					_SET_INT_FIELD(this, "right", geometry.width);
					_SET_INT_FIELD(this, "bottom", geometry.height);
				}
			}
		} else {
			printf("blocking fullscreen request, because window is not maximized\n");
		}
	} else {
		gtk_window_unfullscreen(window);
	}
}

JNIEXPORT void JNICALL Java_android_view_View_native_1setTooltipText(JNIEnv *env, jobject this, jlong widget_ptr, jstring tooltip_jstr)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	const char *tooltip = tooltip_jstr ? (*env)->GetStringUTFChars(env, tooltip_jstr, NULL) : NULL;
	gtk_widget_set_tooltip_text(widget, tooltip);
	if (tooltip_jstr)
		(*env)->ReleaseStringUTFChars(env, tooltip_jstr, tooltip);
}

JNIEXPORT void JNICALL Java_android_view_View_native_1addClass(JNIEnv *env, jobject this, jlong widget_ptr, jstring class_name_jstr)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));

	const char *class_name = (*env)->GetStringUTFChars(env, class_name_jstr, NULL);
	gtk_widget_add_css_class(widget, class_name);

	(*env)->ReleaseStringUTFChars(env, class_name_jstr, class_name);
}

JNIEXPORT void JNICALL Java_android_view_View_native_1removeClass(JNIEnv *env, jobject this, jlong widget_ptr, jstring class_name_jstr)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));

	const char *class_name = (*env)->GetStringUTFChars(env, class_name_jstr, NULL);
	gtk_widget_remove_css_class(widget, class_name);

	(*env)->ReleaseStringUTFChars(env, class_name_jstr, class_name);
}

JNIEXPORT void JNICALL Java_android_view_View_native_1addClasses(JNIEnv *env, jobject this, jlong widget_ptr, jobjectArray class_names_jarray)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));

	int length = (*env)->GetArrayLength(env, class_names_jarray);

	for (int i = 0; i < length; i++) {
		jstring class_name_jstr = (jstring)((*env)->GetObjectArrayElement(env, class_names_jarray, i));

		const char *class_name = (*env)->GetStringUTFChars(env, class_name_jstr, NULL);
		gtk_widget_add_css_class(widget, class_name);

		(*env)->ReleaseStringUTFChars(env, class_name_jstr, class_name);
	}
}

JNIEXPORT void JNICALL Java_android_view_View_native_1removeClasses(JNIEnv *env, jobject this, jlong widget_ptr, jobjectArray class_names_jarray)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));

	int length = (*env)->GetArrayLength(env, class_names_jarray);

	for (int i = 0; i < length; i++) {
		jstring class_name_jstr = (jstring)((*env)->GetObjectArrayElement(env, class_names_jarray, i));

		const char *class_name = (*env)->GetStringUTFChars(env, class_name_jstr, NULL);
		gtk_widget_remove_css_class(widget, class_name);

		(*env)->ReleaseStringUTFChars(env, class_name_jstr, class_name);
	}
}

/* like View.onKeyUp(): Enter and Space click a focused clickable view */
static gboolean on_activate_key_released(GtkEventControllerKey *controller, guint keyval, guint keycode, GdkModifierType state, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(controller)));

	if (keyval != GDK_KEY_Return && keyval != GDK_KEY_KP_Enter && keyval != GDK_KEY_space)
		return FALSE;
	if (!gtk_widget_has_focus(GTK_WIDGET(wrapper)) || !J__View__isClickable(env, wrapper->jobj))
		return FALSE;
	J__View__performClick(env, wrapper->jobj);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	return TRUE;
}

/* Widgets that take the focus themselves (buttons, entries) are left alone, and so is a wrapper
 * that is focusable because the view handles keys itself. */
/* A view the app makes not focusable (android:focusable="false", like the switch in a preference
 * row, whose row takes the focus) is skipped by Tab also when GTK's widget takes the focus itself. */
JNIEXPORT void JNICALL Java_android_view_View_native_1setNativeFocusable(JNIEnv *env, jobject this, jlong widget_ptr, jboolean focusable)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	gboolean ours = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(widget), "atl-not-focusable"));

	/* can-focus covers the widget and its descendants (GtkSwitch inside the box of a Switch) */
	if (!focusable && !ours && gtk_widget_get_can_focus(widget)) {
		gtk_widget_set_can_focus(widget, FALSE);
		g_object_set_data(G_OBJECT(widget), "atl-not-focusable", GINT_TO_POINTER(1));
	} else if (focusable && ours) {
		gtk_widget_set_can_focus(widget, TRUE);
		g_object_set_data(G_OBJECT(widget), "atl-not-focusable", NULL);
	}
}

JNIEXPORT void JNICALL Java_android_view_View_native_1setKeyboardFocusable(JNIEnv *env, jobject this, jlong widget_ptr, jboolean focusable)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	GtkWidget *wrapper = gtk_widget_get_parent(widget);
	gboolean ours = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(wrapper), "keyboard-focusable"));

	if (!WRAPPER_IS_WIDGET(wrapper) || gtk_widget_get_focusable(widget) || (gtk_widget_get_focusable(wrapper) && !ours))
		return;
	if (focusable && !g_object_get_data(G_OBJECT(wrapper), "activate-key-controller")) {
		GtkEventController *controller = gtk_event_controller_key_new();
		g_signal_connect(controller, "key-released", G_CALLBACK(on_activate_key_released), NULL);
		gtk_widget_add_controller(wrapper, controller);
		g_object_set_data(G_OBJECT(wrapper), "activate-key-controller", controller);
	}
	gtk_widget_set_focusable(wrapper, focusable);
	gtk_widget_set_focus_on_click(wrapper, FALSE);
	g_object_set_data(G_OBJECT(wrapper), "keyboard-focusable", GINT_TO_POINTER(focusable));
}

JNIEXPORT void JNICALL Java_android_view_View_nativeRequestFocus(JNIEnv *env, jobject this, jlong widget_ptr, jint direction)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	GtkWidget *wrapper = gtk_widget_get_parent(widget);
	if (gtk_widget_get_focusable(widget))
		gtk_widget_grab_focus(widget);
	else
		gtk_widget_grab_focus(wrapper);
}

JNIEXPORT jboolean JNICALL Java_android_view_View_nativeIsFocused(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	GtkWidget *wrapper = gtk_widget_get_parent(widget);
	return gtk_widget_has_focus(widget) || gtk_widget_has_focus(wrapper);
}

/* the GTK focus is in the view's widget tree: the view itself, an internal GTK child or (for a
 * ViewGroup) a descendant view */
JNIEXPORT jboolean JNICALL Java_android_view_View_nativeHasFocus(JNIEnv *env, jclass class, jlong widget_ptr)
{
	GtkWidget *wrapper = gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr)));
	return wrapper && (gtk_widget_get_state_flags(wrapper) & (GTK_STATE_FLAG_FOCUSED | GTK_STATE_FLAG_FOCUS_WITHIN));
}

JNIEXPORT void JNICALL Java_android_view_View_native_1keep_1screen_1on(JNIEnv *env, jobject this, jlong widget_ptr, jboolean enable)
{
	GtkApplication *application = GTK_APPLICATION(g_application_get_default());
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	guint cookie = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(widget), "keep-screen-on-cookie"));
	if (cookie && !enable) {
		gtk_application_uninhibit(application, cookie);
		g_object_set_data(G_OBJECT(widget), "keep-screen-on-cookie", NULL);
	} else if (!cookie && enable) {
		GtkWindow *window = GTK_WINDOW(gtk_widget_get_native(widget));
		GtkApplicationInhibitFlags flags = GTK_APPLICATION_INHIBIT_SUSPEND | GTK_APPLICATION_INHIBIT_IDLE;
		cookie = gtk_application_inhibit(application, window, flags, "keep-screen-on");
		g_object_set_data(G_OBJECT(widget), "keep-screen-on-cookie", GINT_TO_POINTER(cookie));
	}
}

JNIEXPORT jboolean JNICALL Java_android_view_View_nativeIsAttachedToWindow(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	return currently_unmapping == widget || gtk_widget_get_root(widget);
}

JNIEXPORT jobject JNICALL Java_android_view_View_native_1get_1window(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	GtkRoot *root = gtk_widget_get_root(widget);
	if (!root && widget == currently_unmapping) // in onDetachedFromWindow()
		root = currently_unmapping_root;
	return root ? g_object_get_data(G_OBJECT(root), "jobject") : NULL;
}

extern GtkWindow *window;

JNIEXPORT void JNICALL Java_android_view_View_getWindowVisibleDisplayFrame(JNIEnv *env, jobject this, jobject rect)
{
	int width;
	int height;
	gtk_window_get_default_size(window, &width, &height);
	_SET_INT_FIELD(rect, "left", 0);
	_SET_INT_FIELD(rect, "top", 0);
	_SET_INT_FIELD(rect, "right", width);
	_SET_INT_FIELD(rect, "bottom", height);
}

JNIEXPORT jboolean JNICALL Java_android_view_View_native_1gtkEventPropagation(JNIEnv *env, jobject this)
{
	synthetic_propagation_failed = GINT_TO_POINTER(1);
	return true;
}

JNIEXPORT void JNICALL Java_android_view_View_native_1registerTouchEventPropagation(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *wrapper = gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr)));
	WRAPPER_WIDGET(wrapper)->custom_dispatch_touch = TRUE;
	_setOnTouchListener(env, this, wrapper);
}
