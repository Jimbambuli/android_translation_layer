#include <gtk/gtk.h>
#include <libportal/portal.h>

#include <jni.h>
#include <string.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_app_Activity.h"
#include "../../main-executable/back_button.h"
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
static gboolean stop_when_idle(gpointer activity)
{
	JNIEnv *env = get_jni_env();
	if (_GET_BOOL_FIELD(activity, "paused")) // unless it came back meanwhile
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

void activity_window_ready(void)
{
	JNIEnv *env = get_jni_env();

	for (GList *l = activity_backlog; l != NULL; l = l->next)
		window_focus_changed(env, l->data, true);
}

void current_activity_back_pressed(void)
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
 * onRetainNonConfigurationInstance() (AndroidX keeps its ViewModels there). The start follows when it
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
	jobject retained = J__Activity__onRetainNonConfigurationInstance(env, old_activity);
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

JNIEXPORT jboolean JNICALL Java_android_app_Activity_nativeResumeActivity(JNIEnv *env, jclass class, jclass activity_class, jobject intent)
{
	GList *l;
	GList *activities_to_close = NULL;
	jboolean found = JNI_FALSE;
	for (l = activity_backlog; l != NULL; l = l->next) {
		if ((*env)->IsSameObject(env, activity_class, _CLASS(l->data))) {
			if (l != activity_backlog) {
				activities_to_close = activity_backlog;
				activity_backlog = l;
				l->prev->next = NULL;
				l->prev = NULL;
			}

			/* -- run the activity's onNewIntent -- */
			J__Activity__onNewIntent(env, l->data, intent);
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
