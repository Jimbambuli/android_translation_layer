#include <gtk/gtk.h>
#include <libportal/portal.h>

#include <jni.h>
#include <math.h>
#include <string.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_app_Activity.h"
#include "../../main-executable/back_button.h"
#include "../widgets/WrapperWidget.h"
#include "android_app_Activity.h"

extern GtkWindow *window;

static GList *activity_backlog = NULL;
static jobject activity_current = NULL;

/* the activity change that plays window animations: from which activity, and whether the new one
 * opens (otherwise one is closed, or it is recreated if transition_from is NULL) */
static jobject transition_from = NULL;
static gboolean transition_opening = FALSE;

/* The window's child is a GtkOverlay: the activities are the pages of its GtkStack, and the window
 * that an activity change animates out is shown by a snapshot (AtlWindowGhost) in one of the two
 * layers around the stack - below it for an opening change (the new window slides in over the old
 * one), above it for a closing one (the old window slides away over the revealed one). The stack
 * itself switches pages at once. */
static GtkWidget *pages_stack;
static GtkWidget *pages_below;
static GtkWidget *pages_above;

GtkWidget *activity_pages_new(void)
{
	GtkWidget *overlay = gtk_overlay_new();
	pages_below = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
	pages_stack = gtk_stack_new();
	pages_above = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
	gtk_stack_set_transition_type(GTK_STACK(pages_stack), GTK_STACK_TRANSITION_TYPE_NONE);
	gtk_widget_set_can_target(pages_above, FALSE);
	gtk_overlay_set_child(GTK_OVERLAY(overlay), pages_below);
	gtk_overlay_add_overlay(GTK_OVERLAY(overlay), pages_stack);
	gtk_overlay_set_measure_overlay(GTK_OVERLAY(overlay), pages_stack, TRUE);
	gtk_overlay_add_overlay(GTK_OVERLAY(overlay), pages_above);
	return overlay;
}

GtkWidget *activity_pages_stack(void)
{
	return pages_stack;
}

static GtkStack *get_stack(void)
{
	return GTK_STACK(pages_stack);
}

/* ---- AtlWindowGhost: the snapshot of a window that an activity change animates out ---- */

#define ATL_TYPE_WINDOW_GHOST (atl_window_ghost_get_type())
G_DECLARE_FINAL_TYPE(AtlWindowGhost, atl_window_ghost, ATL, WINDOW_GHOST, GtkWidget)

struct _AtlWindowGhost {
	GtkWidget parent_instance;
	GskRenderNode *image; /* what the window showed, as it was when the change started */
	float alpha;
	float matrix[9]; /* android.graphics.Matrix values of the window animation */
	gint64 start;
	gint64 duration; /* ms */
};

G_DEFINE_FINAL_TYPE(AtlWindowGhost, atl_window_ghost, GTK_TYPE_WIDGET)

static void atl_window_ghost_snapshot(GtkWidget *widget, GtkSnapshot *snapshot)
{
	AtlWindowGhost *ghost = ATL_WINDOW_GHOST(widget);
	graphene_matrix_t matrix;
	/* a 2D affine transformation: x' = xx * x + xy * y + x_0, y' = yx * x + yy * y + y_0 */
	graphene_matrix_init_from_2d(&matrix, ghost->matrix[0], ghost->matrix[3], ghost->matrix[1], ghost->matrix[4], ghost->matrix[2], ghost->matrix[5]);
	GskTransform *transform = gsk_transform_matrix(NULL, &matrix);
	gtk_snapshot_save(snapshot);
	gtk_snapshot_transform(snapshot, transform);
	gtk_snapshot_push_opacity(snapshot, ghost->alpha);
	if (ghost->image)
		gtk_snapshot_append_node(snapshot, ghost->image);
	gtk_snapshot_pop(snapshot);
	gtk_snapshot_restore(snapshot);
	gsk_transform_unref(transform);
}

static void atl_window_ghost_dispose(GObject *object)
{
	g_clear_pointer(&ATL_WINDOW_GHOST(object)->image, gsk_render_node_unref);
	G_OBJECT_CLASS(atl_window_ghost_parent_class)->dispose(object);
}

static void atl_window_ghost_class_init(AtlWindowGhostClass *class)
{
	G_OBJECT_CLASS(class)->dispose = atl_window_ghost_dispose;
	GTK_WIDGET_CLASS(class)->snapshot = atl_window_ghost_snapshot;
}

static void atl_window_ghost_init(AtlWindowGhost *ghost)
{
	ghost->alpha = 1;
	ghost->matrix[0] = ghost->matrix[4] = ghost->matrix[8] = 1;
	gtk_widget_set_can_target(GTK_WIDGET(ghost), FALSE);
	gtk_widget_set_overflow(GTK_WIDGET(ghost), GTK_OVERFLOW_HIDDEN);
	gtk_widget_set_hexpand(GTK_WIDGET(ghost), TRUE);
	gtk_widget_set_vexpand(GTK_WIDGET(ghost), TRUE);
}

/* each frame: Activity.sampleWindowTransition() gives the transformation of the window animation */
static gboolean ghost_tick(GtkWidget *widget, GdkFrameClock *clock, gpointer user_data)
{
	AtlWindowGhost *ghost = ATL_WINDOW_GHOST(widget);
	gint64 now = gdk_frame_clock_get_frame_time(clock);
	if (!ghost->start)
		ghost->start = now;
	JNIEnv *env = get_jni_env();
	jfloatArray values = (*env)->NewFloatArray(env, 10);
	jboolean more = J__Activity__sampleWindowTransition(env, (now - ghost->start) / 1000, values);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		more = FALSE;
	}
	if (more) {
		float out[10];
		(*env)->GetFloatArrayRegion(env, values, 0, 10, out);
		ghost->alpha = out[0];
		memcpy(ghost->matrix, out + 1, sizeof(ghost->matrix));
		gtk_widget_queue_draw(widget);
	}
	(*env)->DeleteLocalRef(env, values);
	if (!more)
		gtk_box_remove(GTK_BOX(gtk_widget_get_parent(widget)), widget);
	return more ? G_SOURCE_CONTINUE : G_SOURCE_REMOVE;
}

/* shows a snapshot of the page that is about to be left, for the time the window animations need */
static void ghost_start(GtkWidget *page, gboolean below)
{
	AtlWindowGhost *ghost = g_object_new(ATL_TYPE_WINDOW_GHOST, NULL);
	GtkSnapshot *snapshot = gtk_snapshot_new();
	gtk_widget_snapshot_child(gtk_widget_get_parent(page), page, snapshot); /* as GtkStack does for its transitions */
	ghost->image = gtk_snapshot_free_to_node(snapshot);
	gtk_box_append(GTK_BOX(below ? pages_below : pages_above), GTK_WIDGET(ghost));
	gtk_widget_add_tick_callback(GTK_WIDGET(ghost), ghost_tick, NULL, NULL);
}

static GtkWidget *get_decor_view(JNIEnv *env, jobject activity)
{
	jobject window_jobj = _GET_OBJ_FIELD(activity, "window", "Landroid/view/Window;");
	jobject decor_view_jobj = _GET_OBJ_FIELD(window_jobj, "decorView", "Landroid/view/ViewGroup;");
	GtkWidget *decor_view = GTK_WIDGET(_PTR(_GET_LONG_FIELD(decor_view_jobj, "widget")));
	/* called from signal handlers and idles, outside of a native method: these would stay */
	(*env)->DeleteLocalRef(env, decor_view_jobj);
	(*env)->DeleteLocalRef(env, window_jobj);
	return gtk_widget_get_parent(decor_view);
}

/* like ViewRootImpl: the window's views get the focus change, its decor view hands it to the
 * Window.Callback (AppCompat wraps the activity in its own) */
static void window_focus_changed(JNIEnv *env, jobject activity, jboolean has_focus)
{
	jobject window_jobj = _GET_OBJ_FIELD(activity, "window", "Landroid/view/Window;");
	J__Window__dispatchWindowFocusChanged(env, window_jobj, has_focus);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, window_jobj);
}

/* the activity whose window has the focus, like the focused window of the window manager */
static jobject focused_activity = NULL;

/* like the input dispatcher, the window that had the focus loses it before the next one gains it; an
 * activity that is paused keeps the focus until the next one is resumed, and one that is destroyed
 * meanwhile (finished or recreated) is not told, its window is gone */
static void activity_take_focus(JNIEnv *env, jobject activity)
{
	if (focused_activity) {
		if (!(*env)->IsSameObject(env, focused_activity, activity))
			window_focus_changed(env, focused_activity, false);
		_UNREF(focused_activity);
	}
	focused_activity = _REF(activity);
	window_focus_changed(env, activity, true);
}

/* fields of android.app.Activity itself, which an app's subclass may hide with fields of the same name */
static jboolean activity_get_bool(JNIEnv *env, jobject activity, const char *name)
{
	return (*env)->GetBooleanField(env, activity, _FIELD_ID(_CACHED_CLASS(Activity), name, "Z"));
}

/* like AOSP, the activity that is left is paused before the next one comes up, loses the focus when that
 * one is resumed (see activity_take_focus) and is stopped only once it is idle (see stop_when_idle) */
static void activity_pause(JNIEnv *env, jobject activity, bool user_leaving)
{
	if (!_GET_BOOL_FIELD(activity, "paused")) {
		J__Activity__onTopResumedActivityChanged(env, activity, false);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
		if (user_leaving) {
			J__Activity__performUserLeaving(env, activity);
			if ((*env)->ExceptionCheck(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
			}
		}
		J__Activity__performPause(env, activity);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
	}
}

/* onStop(), then (unless it is finishing or save_state is false) onSaveInstanceState() */
static void activity_stop(JNIEnv *env, jobject activity, bool save_state)
{
	if (!activity_get_bool(env, activity, "started"))
		return;
	J__Activity__performStop(env, activity, save_state);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

/* AOSP stops the activity that was left once the next one is idle, not while the app is still in the
 * call that started or finished an activity: that call may still change the fragments of the activity
 * it left, which must not have saved its state yet */
static jobject pinned_activity;

static gboolean stop_when_idle(gpointer activity)
{
	JNIEnv *env = get_jni_env();
	/* unless it came back meanwhile, or went into picture-in-picture: that one stays paused and visible */
	if (_GET_BOOL_FIELD(activity, "paused") && !(pinned_activity && (*env)->IsSameObject(env, activity, pinned_activity)))
		activity_stop(env, activity, true);
	_UNREF(activity);
	return G_SOURCE_REMOVE;
}

static void stop_later(JNIEnv *env, jobject activity)
{
	g_idle_add(stop_when_idle, _REF(activity));
}

static void activity_close(JNIEnv *env, jobject activity)
{
	// in case some exception was left unhandled in native code, print it here so we don't confuse it with an exception thrown by onDestroy
	if ((*env)->ExceptionCheck(env)) {
		fprintf(stderr, "activity.onDestroy: seems there was a pending exception... :");
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	activity_stop(env, activity, false);

	if (focused_activity && (*env)->IsSameObject(env, focused_activity, activity)) {
		_UNREF(focused_activity);
		focused_activity = NULL;
	}

	GtkWidget *decor_view = get_decor_view(env, activity);
	GtkStack *stack = get_stack();
	if (gtk_widget_get_parent(decor_view) == GTK_WIDGET(stack))
		gtk_stack_remove(stack, decor_view);

	/* -- run the activity's onDestroy -- */
	J__Activity__performDestroy(env, activity);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

static void weak_ref_free(gpointer ref)
{
	g_weak_ref_clear(ref);
	g_free(ref);
}

/* gives the focus back to a widget without the viewports around it scrolling to it */
static void grab_focus_in_place(GtkWidget *widget)
{
	GSList *viewports = NULL;
	for (GtkWidget *w = widget; w; w = gtk_widget_get_parent(w)) {
		if (GTK_IS_VIEWPORT(w) && gtk_viewport_get_scroll_to_focus(GTK_VIEWPORT(w))) {
			gtk_viewport_set_scroll_to_focus(GTK_VIEWPORT(w), FALSE);
			viewports = g_slist_prepend(viewports, w);
		}
	}
	gtk_widget_grab_focus(widget);
	for (GSList *l = viewports; l; l = l->next)
		gtk_viewport_set_scroll_to_focus(GTK_VIEWPORT(l->data), TRUE);
	g_slist_free(viewports);
}

/* On Android every activity has a window of its own, which keeps its focused view while another
 * activity is in front; a window that gets the focus back assigns no new focus and does not scroll.
 * Here all activities are pages of one GtkStack, which would move the focus to the widget focused
 * in the page before, or else to the first focusable one, and the viewports of the scroll views
 * would then scroll to it (e.g. back to the top of a page that the user had scrolled down). So
 * remember the focus of the page that is left, and only give it back, in place, when the page is
 * shown again; a page shown for the first time gets the first focusable widget, as from GtkStack. */
static void stack_show_page(GtkStack *stack, GtkWidget *page)
{
	GtkRoot *root = gtk_widget_get_root(GTK_WIDGET(stack));
	GtkWidget *old_page = gtk_stack_get_visible_child(stack);
	GtkWidget *focus = root ? gtk_root_get_focus(root) : NULL;
	gboolean had_focus = old_page && focus && gtk_widget_is_ancestor(focus, old_page);

	if (old_page && old_page != page) {
		GWeakRef *ref = g_new(GWeakRef, 1);
		g_weak_ref_init(ref, had_focus ? focus : NULL);
		g_object_set_data_full(G_OBJECT(old_page), "atl-last-focus", ref, weak_ref_free);
		if (had_focus)
			gtk_root_set_focus(root, NULL);
	}

	gtk_stack_set_visible_child(stack, page);

	if (!had_focus || old_page == page)
		return;
	GWeakRef *ref = g_object_get_data(G_OBJECT(page), "atl-last-focus");
	if (!ref) {
		gtk_widget_child_focus(page, GTK_DIR_TAB_FORWARD);
		return;
	}
	GtkWidget *last_focus = g_weak_ref_get(ref);
	if (last_focus) {
		if (gtk_widget_is_ancestor(last_focus, page))
			grab_focus_in_place(last_focus);
		g_object_unref(last_focus);
	}
}

static void activity_focus(JNIEnv *env, jobject activity)
{
	if (_GET_BOOL_FIELD(activity, "finishing"))
		return;

	/* like ActivityThread.handleSendResult(): the results of the activities that finished come before it
	 * is restarted or resumed */
	J__Activity__performDeliverResults(env, activity);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	if (_GET_BOOL_FIELD(activity, "finishing"))
		return;

	if (!activity_get_bool(env, activity, "started")) { // (one that was only paused is just resumed)
		if (activity_get_bool(env, activity, "stopped"))
			J__Activity__performRestart(env, activity);
		else
			J__Activity__performStart(env, activity);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
		if (_GET_BOOL_FIELD(activity, "finishing"))
			return;
	}

	GtkWidget *decor_view = get_decor_view(env, activity);
	GtkStack *stack = get_stack();
	GtkWidget *old_page = gtk_stack_get_visible_child(stack);
	if (old_page && old_page != decor_view && transition_from) {
		jint duration = J__Activity__startWindowTransition(env, activity, transition_from, transition_opening,
		                                                   gtk_widget_get_width(GTK_WIDGET(stack)), gtk_widget_get_height(GTK_WIDGET(stack)));
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
			duration = 0;
		}
		if (duration > 0)
			ghost_start(old_page, transition_opening);
	}
	transition_from = NULL;
	stack_show_page(stack, decor_view);

	J__Activity__performResume(env, activity);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	if (_GET_BOOL_FIELD(activity, "finishing"))
		return;

	J__Activity__onPostResume(env, activity);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	if (_GET_BOOL_FIELD(activity, "finishing"))
		return;

	J__Activity__onTopResumedActivityChanged(env, activity, true);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	activity_take_focus(env, activity);
}

static void activity_update_current(JNIEnv *env)
{
	jobject activity_new = activity_backlog ? g_list_first(activity_backlog)->data : NULL;

	if (activity_current != activity_new) {
		jobject activity_old = activity_current;
		if (activity_old)
			activity_pause(env, activity_old, false);
		activity_current = NULL;

		if (activity_new)
			activity_focus(env, activity_new);
		if (activity_old)
			stop_later(env, activity_old);
		if (activity_new && _GET_BOOL_FIELD(activity_new, "finishing"))
			return;

		activity_current = activity_new;
	}

	if (activity_current != NULL) {
		jclass current_activity_class = (*env)->GetObjectClass(env, activity_current);
		jmethodID current_activity_on_back_pressed_method_id = (*env)->GetMethodID(env, current_activity_class, "onBackPressed", "()V");
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}

		if (g_list_length(activity_backlog) > 1 || _CACHED_METHOD(Activity__onBackPressed) != current_activity_on_back_pressed_method_id) {
			back_button_set_sensitive(true);
		} else {
			back_button_set_sensitive(false);
		}
	}
}

/* The app's window was minimized or suspended (hidden, KWin: minimized), or came back: like leaving the app with
 * the Home button and returning to its task on Android. The shown activity is paused with the user leaving
 * (onUserLeaveHint()) and stopped, unless its params let it enter picture-in-picture by itself
 * (PictureInPictureParams.isAutoEnterEnabled(); TaskFragment.startPausing() with shouldAutoPip). When the
 * window comes back, the activity on top of the back stack is resumed. */
static gboolean window_hidden = FALSE;

static void window_set_hidden(gboolean hidden)
{
	JNIEnv *env = get_jni_env();
	if (hidden == window_hidden)
		return;
	window_hidden = hidden;
	if (hidden && activity_current) {
		jobject activity = activity_current;
		gboolean auto_pip = J__Activity__autoEnterPictureInPicture(env, activity);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
			auto_pip = FALSE;
		}
		if (!auto_pip) {
			activity_pause(env, activity, true);
			activity_stop(env, activity, true);
		}
	} else if (!hidden) {
		if (activity_current && activity_current == (activity_backlog ? activity_backlog->data : NULL))
			activity_focus(env, activity_current);
		else
			activity_update_current(env);
	}
}

/* the window closed while a foreground service kept the app running (see activity_window_close_request()) */
static gboolean window_closed = FALSE;
static gboolean app_quitting = FALSE;
static int foreground_services = 0;

/* GTK applies a new toplevel state only when it paints the next frame (before-paint), and it does not paint
 * while it waits for the compositor's frame callback for the last one. A compositor does not send frame
 * callbacks to a window it does not show (xdg_toplevel "suspended"): minimized while a frame was on its way,
 * e.g. during a video, GTK never learns that it is hidden. So when the window has not painted for a while,
 * ATL asks for a frame; a window that cannot paint it within WINDOW_STALL_MS waits for the compositor and
 * counts as hidden, until it paints again. */
#define WINDOW_IDLE_MS  500
#define WINDOW_STALL_MS 1000
static gboolean window_stalled = FALSE;
static gboolean window_probing = FALSE;
static guint window_stall_check = 0;
static gint64 window_last_paint = 0;

static void update_window_hidden(GdkToplevel *toplevel)
{
	GdkToplevelState state = gdk_toplevel_get_state(toplevel);
	if (!window_closed)
		window_set_hidden(window_stalled || !!(state & (GDK_TOPLEVEL_STATE_MINIMIZED | GDK_TOPLEVEL_STATE_SUSPENDED)));
}

static void on_window_state_changed(GdkToplevel *toplevel, GParamSpec *pspec, gpointer user_data)
{
	update_window_hidden(toplevel);
}

/* low priority: a frame callback that is already there is handled first */
static gboolean check_window_stalled(gpointer user_data)
{
	GdkSurface *surface = gtk_native_get_surface(GTK_NATIVE(window));
	GdkFrameClock *clock = surface ? gdk_surface_get_frame_clock(surface) : NULL;

	window_stall_check = 0;
	if (!clock)
		return G_SOURCE_REMOVE;
	if (window_probing) {
		window_stalled = TRUE;
		update_window_hidden(GDK_TOPLEVEL(surface));
		return G_SOURCE_REMOVE;
	}
	gint64 idle_ms = (g_get_monotonic_time() - window_last_paint) / 1000;
	if (idle_ms < WINDOW_IDLE_MS) {
		window_stall_check = g_timeout_add_full(G_PRIORITY_LOW, WINDOW_IDLE_MS - idle_ms + 1, check_window_stalled, NULL, NULL);
		return G_SOURCE_REMOVE;
	}
	window_probing = TRUE;
	gdk_frame_clock_request_phase(clock, GDK_FRAME_CLOCK_PHASE_AFTER_PAINT);
	window_stall_check = g_timeout_add_full(G_PRIORITY_LOW, WINDOW_STALL_MS, check_window_stalled, NULL, NULL);
	return G_SOURCE_REMOVE;
}

static void on_frame_clock_after_paint(GdkFrameClock *clock, GdkSurface *surface)
{
	window_last_paint = g_get_monotonic_time();
	if (window_stalled) {
		window_stalled = FALSE;
		update_window_hidden(GDK_TOPLEVEL(surface));
	}
	if (window_probing) { // the window can paint: check again once it paints by itself
		window_probing = FALSE;
		g_clear_handle_id(&window_stall_check, g_source_remove);
	} else if (!window_stall_check) {
		window_stall_check = g_timeout_add_full(G_PRIORITY_LOW, WINDOW_IDLE_MS, check_window_stalled, NULL, NULL);
	}
}

/* The user closed the app's window. Like leaving an Android app whose foreground service runs on (its
 * process keeps running, e.g. for the music it plays, see Service.startForeground()), the window only hides
 * then and the shown activity is stopped as with the Home button; the window comes back with
 * activity_window_present() (MPRIS Raise), and the app ends once no foreground service is left. */
gboolean activity_window_close_request(void)
{
	if (app_quitting || foreground_services <= 0)
		return FALSE;
	window_closed = TRUE;
	window_set_hidden(TRUE);
	gtk_widget_set_visible(GTK_WIDGET(window), FALSE);
	return TRUE;
}

void activity_window_present(void)
{
	gtk_window_present(window);
	if (window_closed) {
		window_closed = FALSE;
		window_set_hidden(FALSE);
	}
}

void activity_app_quit(void)
{
	app_quitting = TRUE;
	gtk_window_close(window);
}

static void close_hidden_window(gpointer user_data)
{
	if (window_closed && foreground_services <= 0)
		gtk_window_close(window);
}

/* Service.startForeground()/stopForeground(): the number of foreground services of the app */
JNIEXPORT void JNICALL Java_android_app_Service_nativeSetForegroundServices(JNIEnv *env, jclass clazz, jint count)
{
	foreground_services = count;
	if (count <= 0 && window_closed)
		g_idle_add_once(close_hidden_window, NULL);
}

/* GTK drops the compositor's close request (GDK_DELETE) for the app's window while a modal dialog of
 * it holds the grab, e.g. the file chooser of ACTION_OPEN_DOCUMENT; only then the event is left
 * unhandled and reaches this handler. On Android, removing the task also removes the picker activity
 * on top, so the request closes the app's window all the same. */
static gboolean on_surface_event(GdkSurface *surface, GdkEvent *event, gpointer user_data)
{
	if (gdk_event_get_event_type(event) != GDK_DELETE)
		return FALSE;
	gtk_window_close(GTK_WINDOW(window));
	return TRUE;
}

void activity_window_ready(void)
{
	JNIEnv *env = get_jni_env();

	for (GList *l = activity_backlog; l != NULL; l = l->next)
		window_focus_changed(env, l->data, true);

	GdkSurface *surface = gtk_native_get_surface(GTK_NATIVE(window));
	if (surface && GDK_IS_TOPLEVEL(surface) && !g_object_get_data(G_OBJECT(surface), "atl-watches-state")) {
		g_signal_connect(surface, "notify::state", G_CALLBACK(on_window_state_changed), NULL);
		g_signal_connect(gdk_surface_get_frame_clock(surface), "after-paint", G_CALLBACK(on_frame_clock_after_paint), surface);
		g_signal_connect(surface, "event", G_CALLBACK(on_surface_event), NULL);
		g_object_set_data(G_OBJECT(surface), "atl-watches-state", GINT_TO_POINTER(1));
	}
}

void activity_ensure_configuration(void)
{
	JNIEnv *env = get_jni_env();
	if (!activity_current)
		return;
	J__Activity__ensureActivityConfiguration(env, activity_current);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

/* What Back does once the activity let it go back (Activity.onKeyUp()): the callbacks of the
 * OnBackInvokedDispatcher first, then onBackPressed(), unless the activity is the only one and has
 * Activity's own onBackPressed() (Android 12+ would move its task to the back, it stays here) */
static void activity_back_invoked(void)
{
	JNIEnv *env = get_jni_env();

	bool handled = J__Activity__dispatchOnBackInvoked(env, activity_current);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	if (handled)
		return;

	jclass current_activity_class = (*env)->GetObjectClass(env, activity_current);
	jmethodID current_activity_on_back_pressed_method_id = (*env)->GetMethodID(env, current_activity_class, "onBackPressed", "()V");
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	// Either a new activity was added to the backlog or the current activity's onBackPressed method was changed
	if (g_list_length(activity_backlog) > 1 || _CACHED_METHOD(Activity__onBackPressed) != current_activity_on_back_pressed_method_id) {
		J__Activity__onBackPressed(env, activity_current);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
	} else {
		back_button_set_sensitive(false);
	}
}

JNIEXPORT void JNICALL Java_android_app_Activity_nativeBackInvoked(JNIEnv *env, jobject this)
{
	if (activity_current && (*env)->IsSameObject(env, this, activity_current))
		activity_back_invoked();
}

/* ---- keys at the app's window: the part of InputDispatcher and ViewRootImpl ----
 * GTK hands a key to the focused widget, the views get it on its way up (WrapperWidget.c). A key that no view
 * consumed reaches the window and goes to the shown activity's Window (Window.dispatchKeyEventFromNative():
 * DecorView, Window.Callback, PhoneWindow, fallback handler) with the repeat count, the long press flag of the
 * first repeat and the time of the down key, as InputDispatcher sends it. Like InputDispatcher, a down key that
 * nobody handled gets the fallback key of the key character map, which then gets the rest of the key with
 * FLAG_FALLBACK: AOSP's Generic.kcm has Ctrl+ESCAPE fall back to MENU (Alt/Meta+ESCAPE to HOME, which ATL
 * has not), and ATL adds the desktop's Escape as BACK, as its title bar promises (the back arrow, Esc and
 * the mouse's back button); a Compose popup that takes only BACK keeps Escape, its window stops it. */
#define ACTION_DOWN           0
#define ACTION_UP             1
#define FLAG_FROM_SYSTEM      0x8
#define FLAG_VIRTUAL_HARD_KEY 0x40
#define FLAG_LONG_PRESS       0x80
#define FLAG_FALLBACK         0x400
#define SOURCE_KEYBOARD       0x101
#define VIRTUAL_KEYBOARD      -1
#define KEYCODE_BACK          4
#define KEYCODE_MENU          82
#define KEYCODE_ESCAPE        111
#define META_ALT_ON           (1 << 1)
#define META_CTRL_ON          (1 << 12)
#define META_META_ON          (1 << 16)

static gboolean dispatch_window_key(JNIEnv *env, int action, int code, int repeat, int meta, int scancode, int flags, int unicode, jlong down_time)
{
	if (!activity_current)
		return FALSE;
	jobject window_jobj = _GET_OBJ_FIELD(activity_current, "window", "Landroid/view/Window;");
	jlong now = g_get_monotonic_time() / 1000;
	J__InputTrace__key(env, code, action);
	jobject event = J_new__KeyEvent__full(env, down_time ? down_time : now, now, action, code, repeat, meta,
	                                      VIRTUAL_KEYBOARD, scancode, flags, SOURCE_KEYBOARD);
	_SET_INT_FIELD(event, "unicodeValue", unicode);
	gboolean ret = J__Window__dispatchKeyEventFromNative(env, window_jobj, event);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		ret = FALSE;
	}
	(*env)->DeleteLocalRef(env, event);
	(*env)->DeleteLocalRef(env, window_jobj);
	return ret;
}

/* the key that is down: GDK key code, repeat count, down time, fallback key code (0: none) */
static guint key_down = 0;
static int key_repeat = 0;
static jlong key_down_time = 0;
static int key_fallback = 0;

static gboolean on_window_key_pressed(GtkEventControllerKey *controller, guint keyval, guint keycode, GdkModifierType state, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	int code = atl_android_key_code(controller, keyval, keycode);
	int meta = atl_android_meta_state(state);
	int unicode = atl_android_unicode_char(keyval);
	if (keycode == key_down) {
		key_repeat++;
	} else {
		key_down = keycode;
		key_repeat = 0;
		key_down_time = g_get_monotonic_time() / 1000;
		key_fallback = 0;
	}
	int flags = FLAG_FROM_SYSTEM | (key_repeat == 1 ? FLAG_LONG_PRESS : 0);
	if (key_fallback)
		return dispatch_window_key(env, ACTION_DOWN, key_fallback, key_repeat, meta, keycode - 8, flags | FLAG_FALLBACK, 0, key_down_time);
	gboolean handled = dispatch_window_key(env, ACTION_DOWN, code, key_repeat, meta, keycode - 8, flags, unicode, key_down_time);
	if (!handled && key_repeat == 0 && code == KEYCODE_ESCAPE && !(meta & (META_ALT_ON | META_META_ON))) {
		key_fallback = (meta & META_CTRL_ON) ? KEYCODE_MENU : KEYCODE_BACK;
		handled = dispatch_window_key(env, ACTION_DOWN, key_fallback, 0, meta, keycode - 8, flags | FLAG_FALLBACK, 0, key_down_time);
	}
	return handled;
}

static void on_window_key_released(GtkEventControllerKey *controller, guint keyval, guint keycode, GdkModifierType state, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	int meta = atl_android_meta_state(state);
	int fallback = keycode == key_down ? key_fallback : 0;
	jlong down_time = keycode == key_down ? key_down_time : 0;
	if (keycode == key_down)
		key_down = 0;
	if (fallback)
		dispatch_window_key(env, ACTION_UP, fallback, 0, meta, keycode - 8, FLAG_FROM_SYSTEM | FLAG_FALLBACK, 0, down_time);
	else
		dispatch_window_key(env, ACTION_UP, atl_android_key_code(controller, keyval, keycode), 0, meta, keycode - 8,
		                    FLAG_FROM_SYSTEM, atl_android_unicode_char(keyval), down_time);
}

void activity_window_add_key_controller(GtkWindow *window)
{
	GtkEventController *controller = gtk_event_controller_key_new();
	g_signal_connect(controller, "key-pressed", G_CALLBACK(on_window_key_pressed), NULL);
	g_signal_connect(controller, "key-released", G_CALLBACK(on_window_key_released), NULL);
	gtk_widget_add_controller(GTK_WIDGET(window), controller);
}

/* The back button of the title bar, the navigation bar and the mouse: like the back button of SystemUI's
 * navigation bar (KeyButtonView), a KEYCODE_BACK key from the system, down and up */
void current_activity_back_pressed(void)
{
	JNIEnv *env = get_jni_env();
	jlong now = g_get_monotonic_time() / 1000;
	dispatch_window_key(env, ACTION_DOWN, KEYCODE_BACK, 0, 0, 0, FLAG_FROM_SYSTEM | FLAG_VIRTUAL_HARD_KEY, 0, now);
	dispatch_window_key(env, ACTION_UP, KEYCODE_BACK, 0, 0, 0, FLAG_FROM_SYSTEM | FLAG_VIRTUAL_HARD_KEY, 0, now);
}

int activity_count(void)
{
	return g_list_length(activity_backlog);
}

static char *java_string(JNIEnv *env, jobject object)
{
	if (!object)
		return NULL;
	jmethodID to_string = (*env)->GetMethodID(env, _CLASS(object), "toString", "()Ljava/lang/String;");
	jstring string = (*env)->CallObjectMethod(env, object, to_string);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		return NULL;
	}
	if (!string)
		return NULL;
	const char *chars = (*env)->GetStringUTFChars(env, string, NULL);
	char *copy = g_strdup(chars);
	(*env)->ReleaseStringUTFChars(env, string, chars);
	(*env)->DeleteLocalRef(env, string);
	return copy;
}

/* the title of the activity at index (0 is the shown one), else the simple name of its class */
char *activity_title(int index)
{
	JNIEnv *env = get_jni_env();
	jobject activity = g_list_nth_data(activity_backlog, index);
	if (!activity)
		return NULL;
	jmethodID get_title = (*env)->GetMethodID(env, _CLASS(activity), "getTitle", "()Ljava/lang/CharSequence;");
	jobject title_object = (*env)->CallObjectMethod(env, activity, get_title);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		title_object = NULL;
	}
	char *title = java_string(env, title_object);
	if (title_object)
		(*env)->DeleteLocalRef(env, title_object);
	if (title && *title)
		return title;
	g_free(title);
	return activity_class_name(index);
}

/* the simple name of the class of the activity at index (0 is the shown one) */
char *activity_class_name(int index)
{
	JNIEnv *env = get_jni_env();
	jobject activity = g_list_nth_data(activity_backlog, index);
	if (!activity)
		return NULL;
	jclass class_class = (*env)->FindClass(env, "java/lang/Class");
	jmethodID simple_name = (*env)->GetMethodID(env, class_class, "getSimpleName", "()Ljava/lang/String;");
	jobject name = (*env)->CallObjectMethod(env, _CLASS(activity), simple_name);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		return NULL;
	}
	char *class_name = java_string(env, name);
	(*env)->DeleteLocalRef(env, name);
	return class_name;
}

/* whether the back stack holds the same activity at index and at other */
gboolean activity_same(int index, int other)
{
	JNIEnv *env = get_jni_env();
	jobject a = g_list_nth_data(activity_backlog, index);
	jobject b = g_list_nth_data(activity_backlog, other);
	return a && b && (*env)->IsSameObject(env, a, b);
}

/* the activity at index comes to the front, the others stay below it in their order (like
 * FLAG_ACTIVITY_REORDER_TO_FRONT): the shown one is paused and stopped, the chosen one restarted and resumed */
void activity_bring_to_front(int index)
{
	JNIEnv *env = get_jni_env();
	GList *l = g_list_nth(activity_backlog, index);
	if (!l || l == activity_backlog)
		return;
	activity_backlog = g_list_remove_link(activity_backlog, l);
	activity_backlog = g_list_concat(l, activity_backlog);
	transition_from = activity_current;
	transition_opening = TRUE;
	activity_update_current(env);
	transition_from = NULL;
}

void activity_go_to_start(void)
{
	JNIEnv *env = get_jni_env();
	if (!activity_current)
		return;
	J__Activity__navigateToStart(env, activity_current);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

/* ---- picture-in-picture: the pinned task of ActivityTaskManagerService ----
 *
 * Activity.enterPictureInPictureMode() moves the activity out of the back stack into a pinned task, which is a
 * small undecorated window of its own here (AOSP: a floating window above all others, which a Wayland client
 * cannot ask for; xdg-toplevel has no "keep above"). Its size is the default size of AOSP's WM Shell
 * (PipSizeSpecHandler.SizeSpecDefaultImpl.getDefaultSize()) for the aspect ratio of the params; the activity
 * below comes back in the app's window. Like AOSP's PiP menu (PipMenuView), the pinned window takes all
 * pointer input itself: a click shows the menu with the actions of the params (RemoteAction, sent with their
 * PendingIntent), expand and close, a drag moves the window. Expand (also Escape, the back button of a mouse
 * and the back key: they go back to the app) returns the activity to the top of the back stack, close
 * (also the window's close request) finishes it, as the dismissed pinned task finishes its activity. */
static GtkWidget *pinned_window;
static GtkWidget *pinned_menu;
static int pinned_width, pinned_height;

/* PipSizeSpecHandler.SizeSpecDefaultImpl.getDefaultSize() with the values of AOSP's config.xml (WM Shell):
 * config_pictureInPictureDefaultSizePercent 0.23, config_pictureInPictureAspectRatioLimitForMinSize 1.777778
 * and default_minimal_size_pip_resizable_task 108dp (a dp is a GTK pixel in ATL) */
static void pinned_task_default_size(float aspect_ratio, int *width, int *height)
{
	const float default_size_percent = 0.23f;
	const float max_aspect_ratio_for_min_size = 1.777778f;
	const float min_aspect_ratio_for_min_size = 1.f / max_aspect_ratio_for_min_size;
	const int min_edge_size = 108;
	GdkRectangle display = {0, 0, 1280, 800};
	GdkSurface *surface = gtk_native_get_surface(GTK_NATIVE(window));
	GdkMonitor *monitor = surface ? gdk_display_get_monitor_at_surface(gdk_surface_get_display(surface), surface) : NULL;
	if (monitor)
		gdk_monitor_get_geometry(monitor, &display);
	const int smallest_display_size = MIN(display.width, display.height);
	const int min_size = (int)MAX(min_edge_size, smallest_display_size * default_size_percent);
	if (aspect_ratio <= min_aspect_ratio_for_min_size || aspect_ratio > max_aspect_ratio_for_min_size) {
		// Beyond these points, we can just use the min size as the shorter edge
		if (aspect_ratio <= 1) {
			*width = min_size;
			*height = lroundf(*width / aspect_ratio);
		} else {
			*height = min_size;
			*width = lroundf(*height * aspect_ratio);
		}
	} else {
		// Within these points, ensure that the bounds fit within the radius of the limits at the points
		const float width_at_max_aspect_ratio_for_min_size = max_aspect_ratio_for_min_size * min_size;
		const float radius = hypotf(width_at_max_aspect_ratio_for_min_size, min_size);
		*height = (int)lround(sqrt((radius * radius) / (aspect_ratio * aspect_ratio + 1)));
		*width = lroundf(*height * aspect_ratio);
	}
}

/* the activity leaves the pinned task: its page (returned with a reference) leaves the pinned window, which goes away */
static jobject pinned_task_take(GtkWidget **page_out)
{
	jobject activity = pinned_activity;
	pinned_activity = NULL;
	GtkWidget *overlay = gtk_window_get_child(GTK_WINDOW(pinned_window));
	GtkWidget *page = g_object_ref(gtk_overlay_get_child(GTK_OVERLAY(overlay)));
	gtk_overlay_set_child(GTK_OVERLAY(overlay), NULL);
	gtk_widget_set_can_target(page, TRUE);
	gtk_window_destroy(GTK_WINDOW(pinned_window));
	pinned_window = NULL;
	pinned_menu = NULL;
	pinned_width = pinned_height = 0;
	*page_out = page;
	return activity;
}

static void pinned_task_changed(JNIEnv *env, jobject activity, int width, int height)
{
	J__Activity__pinnedTaskChanged(env, activity, width, height);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

/* PipTaskOrganizer.exitPip(): back to the top of the back stack, in the app's window */
static void pinned_task_expand(void)
{
	JNIEnv *env = get_jni_env();
	if (!pinned_activity)
		return;
	GtkWidget *page;
	jobject activity = pinned_task_take(&page);
	gtk_stack_add_child(get_stack(), page);
	g_object_unref(page);
	pinned_task_changed(env, activity, 0, 0);
	activity_backlog = g_list_prepend(activity_backlog, activity);
	activity_window_present();
	activity_update_current(env);
}

/* the activity of the pinned task finished: it is stopped and destroyed with the pinned window */
static void pinned_task_finish(JNIEnv *env)
{
	GtkWidget *page;
	jobject activity = pinned_task_take(&page);
	activity_close(env, activity);
	g_object_unref(page);
	_UNREF(activity);
}

/* PipTaskOrganizer.removePip(): the activity leaves picture-in-picture, then the removed pinned task
 * finishes it (it is stopped and destroyed, see pinned_task_finish()) */
static void pinned_task_dismiss(void)
{
	JNIEnv *env = get_jni_env();
	if (!pinned_activity)
		return;
	jobject activity = (*env)->NewLocalRef(env, pinned_activity);
	pinned_task_changed(env, activity, 0, 0);
	J__Activity__finish(env, activity);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, activity);
}

static gboolean on_pinned_close_request(GtkWindow *self, gpointer user_data)
{
	pinned_task_dismiss();
	return TRUE;
}

static gboolean on_pinned_key_released(GtkEventControllerKey *controller, guint keyval, guint keycode, GdkModifierType state, gpointer user_data)
{
	if (keyval == GDK_KEY_Escape || keyval == GDK_KEY_Back)
		g_idle_add_once((GSourceOnceFunc)pinned_task_expand, NULL);
	return FALSE;
}

static void on_pinned_mouse_back(gpointer user_data)
{
	g_idle_add_once((GSourceOnceFunc)pinned_task_expand, NULL);
}

static void on_pinned_action_clicked(GtkButton *button, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	if (!pinned_activity)
		return;
	J__Activity__performPictureInPictureAction(env, pinned_activity, GPOINTER_TO_INT(user_data));
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

/* PipMenuView.updateActionViews(): a button for each action of the params, then expand and close */
static void pinned_menu_update(JNIEnv *env)
{
	GtkWidget *child;
	while ((child = gtk_widget_get_first_child(pinned_menu)))
		gtk_box_remove(GTK_BOX(pinned_menu), child);
	jobjectArray titles = J__Activity__getPictureInPictureActionTitles(env, pinned_activity);
	jlongArray icons = J__Activity__getPictureInPictureActionIcons(env, pinned_activity);
	jbooleanArray enabled = J__Activity__getPictureInPictureActionsEnabled(env, pinned_activity);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		return;
	}
	jsize count = titles ? (*env)->GetArrayLength(env, titles) : 0;
	jlong *icon_ptrs = count ? (*env)->GetLongArrayElements(env, icons, NULL) : NULL;
	jboolean *enabled_flags = count ? (*env)->GetBooleanArrayElements(env, enabled, NULL) : NULL;
	for (jsize i = 0; i < count; i++) {
		jstring title_jstr = (*env)->GetObjectArrayElement(env, titles, i);
		const char *title = title_jstr ? (*env)->GetStringUTFChars(env, title_jstr, NULL) : NULL;
		GtkWidget *button = gtk_button_new();
		if (icon_ptrs[i]) {
			GtkWidget *image = gtk_image_new_from_paintable(GDK_PAINTABLE(_PTR(icon_ptrs[i])));
			gtk_image_set_pixel_size(GTK_IMAGE(image), 20); // pip_action_inner_size
			gtk_button_set_child(GTK_BUTTON(button), image);
		} else if (title) {
			gtk_button_set_label(GTK_BUTTON(button), title);
		}
		if (title) {
			gtk_widget_set_tooltip_text(button, title);
			(*env)->ReleaseStringUTFChars(env, title_jstr, title);
		}
		gtk_widget_set_sensitive(button, enabled_flags[i]);
		gtk_widget_set_hexpand(button, TRUE);
		g_signal_connect(button, "clicked", G_CALLBACK(on_pinned_action_clicked), GINT_TO_POINTER(i));
		gtk_box_append(GTK_BOX(pinned_menu), button);
		if (title_jstr)
			(*env)->DeleteLocalRef(env, title_jstr);
	}
	if (icon_ptrs)
		(*env)->ReleaseLongArrayElements(env, icons, icon_ptrs, JNI_ABORT);
	if (enabled_flags)
		(*env)->ReleaseBooleanArrayElements(env, enabled, enabled_flags, JNI_ABORT);
	GtkWidget *expand = gtk_button_new_from_icon_name("window-maximize-symbolic");
	gtk_widget_set_tooltip_text(expand, "Expand");
	gtk_widget_set_hexpand(expand, TRUE);
	g_signal_connect_swapped(expand, "clicked", G_CALLBACK(on_pinned_mouse_back), NULL);
	gtk_box_append(GTK_BOX(pinned_menu), expand);
	GtkWidget *close = gtk_button_new_from_icon_name("window-close-symbolic");
	gtk_widget_set_tooltip_text(close, "Close");
	g_signal_connect_swapped(close, "clicked", G_CALLBACK(pinned_task_dismiss), NULL);
	gtk_box_append(GTK_BOX(pinned_menu), close);
}

/* a click shows or hides the menu (PipTouchHandler: a tap shows it), a drag moves the window */
static void on_pinned_pressed(GtkGestureClick *gesture, int n_press, double x, double y, gpointer user_data)
{
	gtk_widget_set_visible(pinned_menu, !gtk_widget_get_visible(pinned_menu));
}

/* a drag beyond the drag threshold moves the window (the compositor moves it), a click shows the menu */
static void on_pinned_drag_update(GtkGestureDrag *gesture, double offset_x, double offset_y, gpointer user_data)
{
	double x, y;
	if (!gtk_drag_check_threshold(pinned_window, 0, 0, (int)offset_x, (int)offset_y))
		return;
	GdkSurface *surface = gtk_native_get_surface(GTK_NATIVE(pinned_window));
	GdkEvent *event = gtk_event_controller_get_current_event(GTK_EVENT_CONTROLLER(gesture));
	if (!surface || !event || !GDK_IS_TOPLEVEL(surface) || !gtk_gesture_drag_get_start_point(gesture, &x, &y))
		return;
	gtk_gesture_set_state(GTK_GESTURE(gesture), GTK_EVENT_SEQUENCE_CLAIMED);
	gdk_toplevel_begin_move(GDK_TOPLEVEL(surface), gdk_event_get_device(event),
	                        gtk_gesture_single_get_current_button(GTK_GESTURE_SINGLE(gesture)), x + offset_x, y + offset_y,
	                        gdk_event_get_time(event));
	gtk_event_controller_reset(GTK_EVENT_CONTROLLER(gesture));
}

static void pinned_window_new(JNIEnv *env, GtkWidget *page)
{
	pinned_window = gtk_window_new();
	gtk_window_set_application(GTK_WINDOW(pinned_window), gtk_window_get_application(window));
	gtk_window_set_title(GTK_WINDOW(pinned_window), gtk_window_get_title(window));
	gtk_window_set_decorated(GTK_WINDOW(pinned_window), FALSE);
	gtk_window_set_default_size(GTK_WINDOW(pinned_window), pinned_width, pinned_height);
	g_signal_connect(pinned_window, "close-request", G_CALLBACK(on_pinned_close_request), NULL);
	atl_window_android_mouse_back(GTK_WINDOW(pinned_window), G_CALLBACK(on_pinned_mouse_back), NULL);
	GtkEventController *keys = gtk_event_controller_key_new();
	g_signal_connect(keys, "key-released", G_CALLBACK(on_pinned_key_released), NULL);
	gtk_widget_add_controller(pinned_window, keys);

	GtkWidget *overlay = gtk_overlay_new();
	/* the app gets no input in the pinned task, the menu takes it */
	gtk_widget_set_can_target(page, FALSE);
	gtk_overlay_set_child(GTK_OVERLAY(overlay), page);
	GtkGesture *click = gtk_gesture_click_new();
	g_signal_connect(click, "released", G_CALLBACK(on_pinned_pressed), NULL);
	gtk_widget_add_controller(overlay, GTK_EVENT_CONTROLLER(click));
	GtkGesture *drag = gtk_gesture_drag_new();
	g_signal_connect(drag, "drag-update", G_CALLBACK(on_pinned_drag_update), NULL);
	gtk_widget_add_controller(overlay, GTK_EVENT_CONTROLLER(drag));
	/* like PipMenuView: a dark scrim over the video (pip_menu_background 54 % black) with white icons */
	static gboolean css_added = FALSE;
	if (!css_added) {
		GtkCssProvider *css = gtk_css_provider_new();
		gtk_css_provider_load_from_string(css, ".atl-pip-menu { background: rgba(0, 0, 0, 0.54); padding: 4px; }"
		                                       ".atl-pip-menu button { background: none; border: none; box-shadow: none;"
		                                       " color: white; min-width: 40px; min-height: 40px; }"
		                                       ".atl-pip-menu button:hover { background: rgba(255, 255, 255, 0.12); }");
		gtk_style_context_add_provider_for_display(gtk_widget_get_display(pinned_window), GTK_STYLE_PROVIDER(css), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
		g_object_unref(css);
		css_added = TRUE;
	}
	pinned_menu = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 0);
	gtk_widget_add_css_class(pinned_menu, "atl-pip-menu");
	gtk_widget_set_halign(pinned_menu, GTK_ALIGN_FILL);
	gtk_widget_set_valign(pinned_menu, GTK_ALIGN_END);
	gtk_widget_set_visible(pinned_menu, FALSE);
	gtk_overlay_add_overlay(GTK_OVERLAY(overlay), pinned_menu);
	gtk_window_set_child(GTK_WINDOW(pinned_window), overlay);
	pinned_menu_update(env);
	gtk_window_present(GTK_WINDOW(pinned_window));
}

/* the transition into the pinned task, after enterPictureInPictureMode() returned (as from the system server) */
static void pinned_task_entered(gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	if (!pinned_activity)
		return;
	pinned_task_changed(env, pinned_activity, pinned_width, pinned_height);
	if (!window_hidden) {
		activity_update_current(env);
	} else if (activity_current && (*env)->IsSameObject(env, activity_current, pinned_activity)) {
		/* entered by itself when the user left the app: the app's window stays hidden, the pinned activity is paused */
		activity_pause(env, activity_current, false);
		activity_current = NULL;
	}
}

JNIEXPORT jboolean JNICALL Java_android_app_Activity_nativeEnterPictureInPicture(JNIEnv *env, jobject this, jfloat aspect_ratio)
{
	if (pinned_activity && (*env)->IsSameObject(env, this, pinned_activity))
		return JNI_TRUE;
	GList *l;
	for (l = activity_backlog; l != NULL; l = l->next)
		if ((*env)->IsSameObject(env, this, l->data))
			break;
	if (!l)
		return JNI_FALSE;
	/* there is one pinned task: the activity that was in it before is removed with it */
	if (pinned_activity)
		pinned_task_dismiss();
	pinned_activity = l->data;
	activity_backlog = g_list_delete_link(activity_backlog, l);
	GtkWidget *page = get_decor_view(env, this);
	g_object_ref(page);
	gtk_stack_remove(get_stack(), page);
	pinned_task_default_size(aspect_ratio, &pinned_width, &pinned_height);
	pinned_window_new(env, page);
	g_object_unref(page);
	g_idle_add_once(pinned_task_entered, NULL);
	return JNI_TRUE;
}

/* setPictureInPictureParams() while pinned: new size and actions */
JNIEXPORT void JNICALL Java_android_app_Activity_nativeUpdatePictureInPicture(JNIEnv *env, jobject this, jfloat aspect_ratio)
{
	if (!pinned_activity || !(*env)->IsSameObject(env, this, pinned_activity))
		return;
	pinned_menu_update(env);
	int width, height;
	pinned_task_default_size(aspect_ratio, &width, &height);
	if (width == pinned_width && height == pinned_height)
		return;
	pinned_width = width;
	pinned_height = height;
	gtk_window_set_default_size(GTK_WINDOW(pinned_window), width, height);
	pinned_task_changed(env, this, width, height);
}

void activity_close_all(void)
{
	GList *activities, *l;
	JNIEnv *env = get_jni_env();
	// local backup of the backlog
	activities = activity_backlog;
	// deactivate all activities
	activity_backlog = NULL;
	if (activity_current) {
		activity_pause(env, activity_current, false);
		activity_current = NULL;
	}
	if (pinned_activity)
		pinned_task_finish(env);
	// destroy all activities
	for (l = activities; l != NULL; l = l->next) {
		activity_close(env, l->data);
		_UNREF(l->data);
	}
	g_list_free(activities);
}

static gboolean delayed_close_window(gpointer window)
{
	if (activity_backlog == NULL && GTK_IS_WINDOW(window))
		gtk_window_close(GTK_WINDOW(window));
	return G_SOURCE_REMOVE;
}

#define FLAG_ACTIVITY_NO_USER_ACTION 0x00040000

static bool is_user_leaving(JNIEnv *env, jobject intent)
{
	return !intent || !((*env)->CallIntMethod(env, intent, _METHOD(_CLASS(intent), "getFlags", "()I")) & FLAG_ACTIVITY_NO_USER_ACTION);
}

void activity_start(JNIEnv *env, jobject activity_object)
{
	jobject activity_old = activity_current;
	transition_from = activity_current;
	transition_opening = TRUE;
	if (activity_old) { // (unless it was paused before the new activity was created)
		jobject intent = _GET_OBJ_FIELD(activity_object, "intent", "Landroid/content/Intent;");
		activity_pause(env, activity_old, is_user_leaving(env, intent));
		stop_later(env, activity_old);
	}
	activity_current = NULL;
	/* -- run the activity's onCreate -- */
	J__Activity__performCreate(env, activity_object, NULL);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	if (_GET_BOOL_FIELD(activity_object, "finishing")) { // finish() was called before the activity was created
		transition_from = NULL;
		return;
	}

	activity_backlog = g_list_prepend(activity_backlog, _REF(activity_object));

	GtkWidget *decor_view = get_decor_view(env, activity_object);
	GtkStack *stack = get_stack();
	gtk_stack_add_child(stack, decor_view);

	activity_update_current(env);
	transition_from = NULL;
}

JNIEXPORT void JNICALL Java_android_app_Activity_nativeFinish(JNIEnv *env, jobject this, jlong window)
{
	GList *l;
	jobject removed_activity = NULL;
	if (pinned_activity && (*env)->IsSameObject(env, this, pinned_activity)) {
		pinned_task_finish(env);
		return;
	}
	for (l = activity_backlog; l != NULL; l = l->next) {
		if ((*env)->IsSameObject(env, this, l->data)) {
			removed_activity = l->data;
			activity_backlog = g_list_delete_link(activity_backlog, l);
			break;
		}
	}
	transition_from = removed_activity;
	transition_opening = FALSE;
	activity_update_current(env);
	transition_from = NULL;
	if (removed_activity) {
		activity_close(env, removed_activity);
		_UNREF(removed_activity);
	}
	if (activity_backlog == NULL && window)
		g_idle_add(delayed_close_window, _PTR(window));
}

JNIEXPORT void JNICALL Java_android_app_Activity_nativeStartActivity(JNIEnv *env, jclass class, jobject activity)
{
	activity_start(env, activity);
}

/* like AOSP, the activity that is left is paused before the next one is created */
JNIEXPORT void JNICALL Java_android_app_Activity_nativePauseCurrentActivity(JNIEnv *env, jclass class, jobject intent)
{
	if (activity_current)
		activity_pause(env, activity_current, is_user_leaving(env, intent));
}

/* the next activity could not be created: the one that was paused for it comes back */
JNIEXPORT void JNICALL Java_android_app_Activity_nativeResumeCurrentActivity(JNIEnv *env, jclass class)
{
	if (activity_current && _GET_BOOL_FIELD(activity_current, "paused"))
		activity_focus(env, activity_current);
}

/* Activity.recreate(): the new instance takes the place of this one in the back stack and is
 * created from the state this one saved after being stopped, and gets what it retained with
 * retainNonConfigurationInstances() (AndroidX keeps its ViewModels there). The start follows when it
 * is focused, then onRestoreInstanceState() and onPostCreate() (see Activity.performStart()). */
JNIEXPORT void JNICALL Java_android_app_Activity_nativeRecreate(JNIEnv *env, jobject this)
{
	GList *l;
	for (l = activity_backlog; l != NULL; l = l->next) {
		if ((*env)->IsSameObject(env, this, l->data))
			break;
	}
	if (!l || _GET_BOOL_FIELD(this, "finishing"))
		return;
	jobject old_activity = l->data;

	if (activity_current && (*env)->IsSameObject(env, activity_current, old_activity)) {
		activity_pause(env, activity_current, false);
		activity_current = NULL;
	}
	activity_stop(env, old_activity, true);

	jobject saved_state = (*env)->GetObjectField(env, old_activity, _FIELD_ID(_CACHED_CLASS(Activity), "savedState", "Landroid/os/Bundle;"));
	jobject retained = J__Activity__retainNonConfigurationInstances(env, old_activity);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		retained = NULL;
	}
	/* as on AOSP, the old instance is destroyed before the new one is created: apps that keep their
	 * pages outside the activity (JW Library) tear them down in onDestroy() and set them up again in
	 * onCreate() */
	activity_close(env, old_activity);

	jobject activity = J__Activity__createRelaunched(env, old_activity, retained);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		activity = NULL;
	}
	if (!activity) {
		activity_backlog = g_list_delete_link(activity_backlog, l);
		_UNREF(old_activity);
		activity_update_current(env);
		return;
	}

	J__Activity__performCreate(env, activity, saved_state);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	if (_GET_BOOL_FIELD(activity, "finishing")) { // finish() was called while it was created
		activity_backlog = g_list_delete_link(activity_backlog, l);
	} else {
		GtkStack *stack = get_stack();
		gtk_stack_add_child(stack, get_decor_view(env, activity));
		l->data = _REF(activity);
	}
	_UNREF(old_activity);
	activity_update_current(env);
}

static gboolean task_clearing_has(JNIEnv *env, jobject activity);

JNIEXPORT jboolean JNICALL Java_android_app_Activity_nativeResumeActivity(JNIEnv *env, jclass class, jclass activity_class, jobject intent)
{
	GList *l;
	GList *activities_to_close = NULL;
	jboolean found = JNI_FALSE;
	for (l = activity_backlog; l != NULL; l = l->next) {
		if ((*env)->IsSameObject(env, activity_class, _CLASS(l->data)) && !task_clearing_has(env, l->data)) {
			if (l != activity_backlog) {
				activities_to_close = activity_backlog;
				activity_backlog = l;
				l->prev->next = NULL;
				l->prev = NULL;
			}

			/* -- run the activity's onNewIntent -- */
			J__Activity__performNewIntent(env, l->data, intent);
			if ((*env)->ExceptionCheck(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
			}
			found = JNI_TRUE;
			break;
		}
	}
	transition_from = activity_current;
	transition_opening = FALSE;
	activity_update_current(env);
	transition_from = NULL;

	for (l = activities_to_close; l != NULL; l = l->next) {
		activity_close(env, l->data);
		_UNREF(l->data);
	}
	g_list_free(activities_to_close);

	return found;
}

/* FLAG_ACTIVITY_CLEAR_TASK: the activities of the task before the start (nativeClearTaskMark()) finish once the
 * new root is up (nativeClearTaskFinish(), posted after the start: a launcher activity that is a trampoline
 * starts the real one from its onCreate() and finishes); meanwhile none of them is reused for singleTask or
 * FLAG_ACTIVITY_CLEAR_TOP, and the back stack is never empty (that would close the window) */
static GList *task_clearing = NULL;

static gboolean task_clearing_has(JNIEnv *env, jobject activity)
{
	for (GList *l = task_clearing; l != NULL; l = l->next)
		if ((*env)->IsSameObject(env, l->data, activity))
			return TRUE;
	return FALSE;
}

JNIEXPORT void JNICALL Java_android_app_Activity_nativeClearTaskMark(JNIEnv *env, jclass class)
{
	for (GList *l = activity_backlog; l != NULL; l = l->next)
		if (!task_clearing_has(env, l->data))
			task_clearing = g_list_append(task_clearing, _REF(l->data));
}

JNIEXPORT void JNICALL Java_android_app_Activity_nativeClearTaskFinish(JNIEnv *env, jclass class, jboolean finish)
{
	GList *clearing = task_clearing;
	task_clearing = NULL;
	for (GList *l = clearing; l != NULL; l = l->next) {
		if (finish)
			(*env)->CallVoidMethod(env, l->data, (*env)->GetMethodID(env, _CLASS(l->data), "finish", "()V"));
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
		_UNREF(l->data);
	}
	g_list_free(clearing);
}

/* Activity.finishAndRemoveTask(): every activity of the task finishes, the one that asked last; when the back
 * stack is empty, the window closes (nativeFinish()). An activity in picture-in-picture is the only one of
 * its pinned task */
JNIEXPORT void JNICALL Java_android_app_Activity_nativeFinishTask(JNIEnv *env, jobject this)
{
	jmethodID finish = (*env)->GetMethodID(env, _CLASS(this), "finish", "()V");
	GList *others = NULL;
	if (!(pinned_activity && (*env)->IsSameObject(env, this, pinned_activity))) {
		for (GList *l = activity_backlog; l != NULL; l = l->next)
			if (!(*env)->IsSameObject(env, this, l->data))
				others = g_list_prepend(others, _REF(l->data));
	}
	for (GList *l = others; l != NULL; l = l->next) {
		(*env)->CallVoidMethod(env, l->data, (*env)->GetMethodID(env, _CLASS(l->data), "finish", "()V"));
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
		_UNREF(l->data);
	}
	g_list_free(others);
	(*env)->CallVoidMethod(env, this, finish);
}

JNIEXPORT void JNICALL Java_android_app_Activity_nativeOpenURI(JNIEnv *env, jclass class, jstring uriString)
{
	static XdpPortal *portal = NULL;
	static gboolean have_portal = TRUE;
	if (!portal && have_portal) {
		/* xdp_portal_new() aborts the process when there is no session bus or portal */
		GError *error = NULL;
		portal = xdp_portal_initable_new(&error);
		if (!portal) {
			fprintf(stderr, "nativeOpenURI: no desktop portal (%s), opening links with the default application\n", error->message);
			g_error_free(error);
			have_portal = FALSE;
		}
	}

	const char *uri = (*env)->GetStringUTFChars(env, uriString, NULL);
	if (portal) {
		xdp_portal_open_uri(portal, NULL, uri, XDP_OPEN_URI_FLAG_NONE, NULL, NULL, NULL);
	} else {
		GError *error = NULL;
		if (!g_app_info_launch_default_for_uri(uri, NULL, &error)) {
			fprintf(stderr, "nativeOpenURI: cannot open %s: %s\n", uri, error->message);
			g_error_free(error);
		}
	}
	(*env)->ReleaseStringUTFChars(env, uriString, uri);
}

extern GtkWindow *window; // TODO: get this in a better way

struct filechooser_callback_data {
	jobject activity;
	jint request_code;
	jint action;
};

#define RESULT_OK       -1
#define RESULT_CANCELED 0

static void file_dialog_callback(GObject *source_object, GAsyncResult *res, gpointer data)
{
	struct filechooser_callback_data *d = data;
	GtkFileDialog *dialog = GTK_FILE_DIALOG(source_object);
	GFile *(*const finish_functions[])(GtkFileDialog *, GAsyncResult *, GError **) = {
		gtk_file_dialog_open_finish,
		gtk_file_dialog_save_finish,
		gtk_file_dialog_select_folder_finish,
	};

	GFile *file = finish_functions[d->action](dialog, res, NULL);
	JNIEnv *env = get_jni_env();

	if (file) {
		char *uri = g_file_get_uri(file);

		J__Activity__fileChooserResultCallback(env, d->activity, d->request_code, RESULT_OK, d->action, _JSTRING(uri));
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}

		g_free(uri);
		g_object_unref(file);
	} else {
		J__Activity__fileChooserResultCallback(env, d->activity, d->request_code, RESULT_CANCELED, d->action, NULL);
	}
	free(d);
}

JNIEXPORT void JNICALL Java_android_app_Activity_nativeFileChooser(JNIEnv *env, jobject this, jint action, jstring type_jstring, jstring filename_jstring, jint request_code)
{
	const char *chooser_title = ((char *[]){"Open File", "Save File", "Select Folder"})[action];
	GtkFileDialog *dialog = gtk_file_dialog_new();
	gtk_file_dialog_set_title(GTK_FILE_DIALOG(dialog), chooser_title);

	const char *type = type_jstring ? (*env)->GetStringUTFChars(env, type_jstring, NULL) : NULL;
	if (type && !strchr(type, '*')) {
		GtkFileFilter *filter = gtk_file_filter_new();
		gtk_file_filter_add_mime_type(filter, type);
		gtk_file_filter_set_name(filter, type);
		gtk_file_dialog_set_default_filter(GTK_FILE_DIALOG(dialog), filter);
		(*env)->ReleaseStringUTFChars(env, type_jstring, type);
	}
	const char *filename = filename_jstring ? (*env)->GetStringUTFChars(env, filename_jstring, NULL) : NULL;
	if (filename) {
		gtk_file_dialog_set_initial_name(GTK_FILE_DIALOG(dialog), filename);
		(*env)->ReleaseStringUTFChars(env, filename_jstring, filename);
	}

	struct filechooser_callback_data *callback_data = malloc(sizeof(struct filechooser_callback_data));
	callback_data->activity = _REF(this);
	callback_data->request_code = request_code;
	callback_data->action = action;
	void (*const file_dialog_functions[])(GtkFileDialog *, GtkWindow *, GCancellable *, GAsyncReadyCallback, gpointer) = {
		gtk_file_dialog_open,
		gtk_file_dialog_save,
		gtk_file_dialog_select_folder,
	};
	file_dialog_functions[action](dialog, window, NULL, file_dialog_callback, callback_data);
}

/* MediaStore.ACTION_PICK_IMAGES: the GTK file dialog stands in for the photo picker of MediaProvider */
struct pick_images_callback_data {
	jobject activity;
	jint request_code;
	jint max_items;
};

static void pick_images_callback(GObject *source_object, GAsyncResult *res, gpointer data)
{
	struct pick_images_callback_data *d = data;
	GtkFileDialog *dialog = GTK_FILE_DIALOG(source_object);
	JNIEnv *env = get_jni_env();
	GListModel *files = NULL;
	jobjectArray uris = NULL;

	if (d->max_items > 1) {
		files = gtk_file_dialog_open_multiple_finish(dialog, res, NULL);
	} else {
		GFile *file = gtk_file_dialog_open_finish(dialog, res, NULL);
		if (file) {
			GListStore *store = g_list_store_new(G_TYPE_FILE);
			g_list_store_append(store, file);
			g_object_unref(file);
			files = G_LIST_MODEL(store);
		}
	}
	if (files) {
		guint n = g_list_model_get_n_items(files);
		uris = (*env)->NewObjectArray(env, n, (*env)->FindClass(env, "java/lang/String"), NULL);
		for (guint i = 0; i < n; i++) {
			GFile *file = g_list_model_get_item(files, i);
			char *uri = g_file_get_uri(file);
			(*env)->SetObjectArrayElement(env, uris, i, _JSTRING(uri));
			g_free(uri);
			g_object_unref(file);
		}
		g_object_unref(files);
	}
	J__Activity__pickImagesResultCallback(env, d->activity, d->request_code, d->max_items, uris);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	_UNREF(d->activity);
	free(d);
}

/* mime_types: null for all images and videos (MimeFilterUtils.getMimeTypeFilters()) */
JNIEXPORT void JNICALL Java_android_app_Activity_nativePickImages(JNIEnv *env, jobject this, jobjectArray mime_types, jint max_items, jint request_code)
{
	GtkFileDialog *dialog = gtk_file_dialog_new();
	gtk_file_dialog_set_title(dialog, max_items > 1 ? "Select Photos and Videos" : "Select a Photo or Video");

	GtkFileFilter *filter = gtk_file_filter_new();
	if (mime_types) {
		jsize n = (*env)->GetArrayLength(env, mime_types);
		for (jsize i = 0; i < n; i++) {
			jstring type_jstring = (*env)->GetObjectArrayElement(env, mime_types, i);
			const char *type = (*env)->GetStringUTFChars(env, type_jstring, NULL);
			gtk_file_filter_add_mime_type(filter, type);
			(*env)->ReleaseStringUTFChars(env, type_jstring, type);
			(*env)->DeleteLocalRef(env, type_jstring);
		}
	} else {
		gtk_file_filter_add_mime_type(filter, "image/*");
		gtk_file_filter_add_mime_type(filter, "video/*");
	}
	gtk_file_filter_set_name(filter, "Photos and videos");
	gtk_file_dialog_set_default_filter(dialog, filter);
	g_object_unref(filter);

	struct pick_images_callback_data *callback_data = malloc(sizeof(struct pick_images_callback_data));
	callback_data->activity = _REF(this);
	callback_data->request_code = request_code;
	callback_data->max_items = max_items;
	if (max_items > 1)
		gtk_file_dialog_open_multiple(dialog, window, NULL, pick_images_callback, callback_data);
	else
		gtk_file_dialog_open(dialog, window, NULL, pick_images_callback, callback_data);
	g_object_unref(dialog);
}

JNIEXPORT jboolean JNICALL Java_android_app_Activity_isInMultiWindowMode(JNIEnv *env, jobject this)
{
	return !gtk_window_is_maximized(window);
}

JNIEXPORT jboolean JNICALL Java_android_app_Activity_isTaskRoot(JNIEnv *env, jobject this)
{
	jobject root_activity = activity_backlog ? g_list_last(activity_backlog)->data : NULL;
	// NULL means that we are currently creating the root activity, so no other activity can exist yet
	return root_activity == NULL || (*env)->IsSameObject(env, this, root_activity);
}
