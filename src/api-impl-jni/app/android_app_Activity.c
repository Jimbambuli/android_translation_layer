#include <gtk/gtk.h>
#include <libportal/portal.h>

#include <jni.h>
#include <string.h>

#include "../defines.h"
#include "../util.h"
#include "../widgets/WrapperWidget.h"
#include "../generated_headers/android_app_Activity.h"
#include "../../main-executable/back_button.h"
#include "android_app_Activity.h"

extern GtkWindow *window;

static GList *activity_backlog = NULL;
static jobject activity_current = NULL;

/* the activity change for which the GtkStack shows a transition: from which activity, and whether the
 * new one opens (otherwise one is closed, or it is recreated if transition_from is NULL) */
static jobject transition_from = NULL;
static gboolean transition_opening = FALSE;

static GtkStack *get_stack(void)
{
	return GTK_STACK(gtk_window_get_child(window));
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

/* fields of android.app.Activity itself, which an app's subclass may hide with fields of the same name */
static jboolean activity_get_bool(JNIEnv *env, jobject activity, const char *name)
{
	return (*env)->GetBooleanField(env, activity, _FIELD_ID(_CACHED_CLASS(Activity), name, "Z"));
}

/* like AOSP, the activity that is left is paused (and loses the focus) before the next one comes up,
 * and stopped only once that one is resumed (see stop_when_idle) */
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

	window_focus_changed(env, activity, false);
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

	GtkWidget *decor_view = get_decor_view(env, activity);
	GtkStack *stack = GTK_STACK(gtk_window_get_child(window));
	gtk_stack_remove(stack, decor_view);

	/* -- run the activity's onDestroy -- */
	J__Activity__performDestroy(env, activity);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

struct scrim_anim {
	struct atl_scrim scrim;
	GtkWidget *page;
	float from;
	float to;
	gint64 start;
	gint64 duration;
};

/* the tick callback owns the animation and holds a reference on the page; the page's "atl-scrim"
 * points at it until it ends or the next change replaces it */
static void scrim_anim_free(gpointer data)
{
	struct scrim_anim *anim = data;
	if (g_object_get_data(G_OBJECT(anim->page), "atl-scrim") == anim)
		g_object_set_data(G_OBJECT(anim->page), "atl-scrim", NULL);
	g_object_unref(anim->page);
	g_free(anim);
}

static gboolean scrim_tick(GtkWidget *stack, GdkFrameClock *clock, gpointer user_data)
{
	struct scrim_anim *anim = user_data;
	if (g_object_get_data(G_OBJECT(anim->page), "atl-scrim") != anim)
		return G_SOURCE_REMOVE; // replaced by the next change
	gint64 now = gdk_frame_clock_get_frame_time(clock);
	if (!anim->start)
		anim->start = now;
	double t = MIN(1.0, (double)(now - anim->start) / anim->duration);
	anim->scrim.alpha = anim->from + (anim->to - anim->from) * t;
	gtk_widget_queue_draw(anim->page);
	if (t < 1 && gtk_stack_get_transition_running(GTK_STACK(stack)))
		return G_SOURCE_CONTINUE;
	gtk_widget_queue_draw(anim->page);
	return G_SOURCE_REMOVE; // scrim_anim_free() takes the scrim off the page
}

/* The activity that does not move in a change fades over the black window (see
 * Activity.getStackTransition). GtkStack draws the leaving page from a snapshot taken when the change
 * starts, so the page that it shows live (the entering one) draws a black scrim of the matching alpha:
 * over itself if it is the one that fades, else around itself, where the leaving page shows. */
static void start_scrim(JNIEnv *env, GtkStack *stack, GtkWidget *page, int duration)
{
	jint scrim = (*env)->GetStaticIntField(env, _CACHED_CLASS(Activity),
	                                       (*env)->GetStaticFieldID(env, _CACHED_CLASS(Activity), "stackScrim", "I"));
	if (!(scrim & (1 << 17)) || duration <= 0 || !WRAPPER_IS_WIDGET(page))
		return;
	struct scrim_anim *anim = g_new0(struct scrim_anim, 1);
	anim->page = g_object_ref(page);
	anim->scrim.outside = !(scrim & (1 << 16));
	anim->from = (scrim & 0xff) / 255.f;
	anim->to = ((scrim >> 8) & 0xff) / 255.f;
	anim->scrim.alpha = anim->from;
	anim->duration = (gint64)duration * 1000;
	g_object_set_data(G_OBJECT(page), "atl-scrim", anim);
	gtk_widget_add_tick_callback(GTK_WIDGET(stack), scrim_tick, anim, scrim_anim_free);
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
	GtkStack *stack = GTK_STACK(gtk_window_get_child(window));
	jint transition = J__Activity__getStackTransition(env, transition_opening ? activity : transition_from, transition_opening ? transition_from : activity,
	                                                  transition_opening, gtk_widget_get_width(GTK_WIDGET(stack)), gtk_widget_get_height(GTK_WIDGET(stack)));
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		transition = GTK_STACK_TRANSITION_TYPE_NONE;
	}
	transition_from = NULL;
	gtk_stack_set_transition_type(stack, transition & 0xff);
	gtk_stack_set_transition_duration(stack, transition >> 8);
	stack_show_page(stack, decor_view);
	start_scrim(env, stack, decor_view, transition >> 8);

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

	window_focus_changed(env, activity, true);
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

void activity_start(JNIEnv *env, jobject activity_object)
{
	jobject activity_old = activity_current;
	transition_from = activity_current;
	transition_opening = TRUE;
	if (activity_old) {
		jobject intent = _GET_OBJ_FIELD(activity_object, "intent", "Landroid/content/Intent;");
		bool user_leaving = !intent || !((*env)->CallIntMethod(env, intent, _METHOD(_CLASS(intent), "getFlags", "()I")) & FLAG_ACTIVITY_NO_USER_ACTION);
		activity_pause(env, activity_old, user_leaving);
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

	J__Activity__onPostCreate(env, activity_object, NULL);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	activity_backlog = g_list_prepend(activity_backlog, _REF(activity_object));

	GtkWidget *decor_view = get_decor_view(env, activity_object);
	GtkStack *stack = GTK_STACK(gtk_window_get_child(window));
	gtk_stack_add_child(stack, decor_view);

	activity_update_current(env);
	transition_from = NULL;
}

static void close_after_transition(GtkStack *stack, GParamSpec *pspec, jobject activity)
{
	if (gtk_stack_get_transition_running(stack))
		return;
	g_signal_handlers_disconnect_by_func(stack, close_after_transition, activity);
	JNIEnv *env = get_jni_env();
	activity_close(env, activity);
	_UNREF(activity);
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
		GtkStack *stack = get_stack();
		if (gtk_stack_get_transition_running(stack)) {
			/* as on AOSP, the activity is destroyed after the transition, which still shows it */
			g_signal_connect(stack, "notify::transition-running", G_CALLBACK(close_after_transition), removed_activity);
		} else {
			activity_close(env, removed_activity);
			_UNREF(removed_activity);
		}
	}
	if (activity_backlog == NULL && window)
		g_idle_add(delayed_close_window, _PTR(window));
}

JNIEXPORT void JNICALL Java_android_app_Activity_nativeStartActivity(JNIEnv *env, jclass class, jobject activity)
{
	activity_start(env, activity);
}

/* Activity.recreate(): the new instance takes the place of this one in the back stack and is
 * created from the state this one saved after being stopped, and gets what it retained with
 * onRetainNonConfigurationInstance() (AndroidX keeps its ViewModels there). As for activity_start(),
 * onRestoreInstanceState() comes before onPostCreate(), the start follows when it is focused. */
JNIEXPORT void JNICALL Java_android_app_Activity_nativeRecreate(JNIEnv *env, jobject this, jobject activity)
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
	J__Activity__retainNonConfigurationInstanceFor(env, old_activity, activity);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	/* as on AOSP, the old instance is destroyed before the new one is created: apps that keep their
	 * pages outside the activity (JW Library) tear them down in onDestroy() and set them up again in
	 * onCreate() */
	activity_close(env, old_activity);

	J__Activity__performCreate(env, activity, saved_state);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	if (!_GET_BOOL_FIELD(activity, "finishing")) {
		if (saved_state) {
			J__Activity__onRestoreInstanceState(env, activity, saved_state);
			if ((*env)->ExceptionCheck(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
			}
		}
		J__Activity__onPostCreate(env, activity, saved_state);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
	}

	if (_GET_BOOL_FIELD(activity, "finishing")) { // finish() was called while it was created
		activity_backlog = g_list_delete_link(activity_backlog, l);
	} else {
		GtkStack *stack = GTK_STACK(gtk_window_get_child(window));
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
