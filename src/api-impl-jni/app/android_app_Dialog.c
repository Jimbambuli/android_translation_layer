#include <gtk/gtk.h>
#include <jni.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_app_Dialog.h"

/* main app window */
extern GtkWindow *window;

static gboolean on_close_request(GtkWidget *dialog, jobject jobj)
{
	printf("on_close_request\n");
	JNIEnv *env = get_jni_env();
	J__DialogInterface__dismiss(env, jobj);
	return FALSE;
}

#define KEYCODE_BACK 4
#define ACTION_DOWN 0
#define ACTION_UP 1

/* Key events that no view consumed go to the dialog, like Window.Callback.dispatchKeyEvent() on
 * Android. Only Escape is forwarded, as Back, which is what the fallback in AOSP's Generic.kcm
 * turns an unhandled Escape into: a cancelable dialog closes, Compose's dialogs call their
 * onDismissRequest. */
static gboolean dispatch_back_key(jobject jobj, guint keyval, int action)
{
	if (keyval != GDK_KEY_Escape)
		return FALSE;

	JNIEnv *env = get_jni_env();
	jobject key_event = J_new__KeyEvent(env, (jlong)0, (jlong)0, action, KEYCODE_BACK, 0, 0);
	jmethodID dispatch = _METHOD(_CLASS(jobj), "dispatchKeyEvent", "(Landroid/view/KeyEvent;)Z");
	gboolean ret = (*env)->CallBooleanMethod(env, jobj, dispatch, key_event);
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionDescribe(env);
	(*env)->DeleteLocalRef(env, key_event);
	return ret;
}

static gboolean on_key_pressed(GtkEventControllerKey *controller, guint keyval, guint keycode, GdkModifierType state, jobject jobj)
{
	return dispatch_back_key(jobj, keyval, ACTION_DOWN);
}

static gboolean on_key_released(GtkEventControllerKey *controller, guint keyval, guint keycode, GdkModifierType state, jobject jobj)
{
	return dispatch_back_key(jobj, keyval, ACTION_UP);
}

/* state for Dialog.setCanceledOnTouchOutside() */
struct touch_outside {
	GtkWindow *dialog;
	GtkEventController *controller;
	jobject jobj;
};

static gboolean on_touch_outside(GtkEventControllerLegacy *controller, GdkEvent *event, struct touch_outside *outside)
{
	GdkEventType event_type = gdk_event_get_event_type(event);
	if ((event_type == GDK_BUTTON_PRESS || event_type == GDK_TOUCH_BEGIN) && gtk_widget_get_visible(GTK_WIDGET(outside->dialog))) {
		JNIEnv *env = get_jni_env();
		jmethodID dismiss = _METHOD(_CLASS(outside->jobj), "dismiss", "()V");
		(*env)->CallVoidMethod(env, outside->jobj, dismiss);
		if ((*env)->ExceptionCheck(env))
			(*env)->ExceptionDescribe(env);
		return TRUE;
	} else {
		return FALSE;
	}
}

static void remove_touch_outside(GtkWindow *dialog)
{
	struct touch_outside *outside = g_object_get_data(G_OBJECT(dialog), "touch-outside");
	if (!outside)
		return;

	JNIEnv *env = get_jni_env();
	gtk_widget_remove_controller(GTK_WIDGET(window), outside->controller);
	_UNREF(outside->jobj);
	g_free(outside);
	g_object_set_data(G_OBJECT(dialog), "touch-outside", NULL);
}

JNIEXPORT jlong JNICALL Java_android_app_Dialog_nativeInit(JNIEnv *env, jobject this, jlong decor_view_ptr)
{
	GtkWidget *decor_view = GTK_WIDGET(_PTR(decor_view_ptr));
	GtkWidget *dialog = gtk_window_new();
	gtk_window_set_transient_for(GTK_WINDOW(dialog), window);
	/* an Android dialog is part of the app's window: modal, without decorations of its own (the
	 * content draws its title), so the compositor keeps it centred on the app window instead of
	 * showing a separate "android-translation-layer" window */
	gtk_window_set_modal(GTK_WINDOW(dialog), TRUE);
	gtk_window_set_decorated(GTK_WINDOW(dialog), FALSE);
	gtk_window_set_child(GTK_WINDOW(dialog), gtk_widget_get_parent(decor_view));
	jobject ref = _REF(this);
	g_signal_connect(GTK_WINDOW(dialog), "close-request", G_CALLBACK(on_close_request), ref);
	GtkEventController *controller = gtk_event_controller_key_new();
	g_signal_connect(controller, "key-pressed", G_CALLBACK(on_key_pressed), ref);
	g_signal_connect(controller, "key-released", G_CALLBACK(on_key_released), ref);
	gtk_widget_add_controller(dialog, controller);
	return _INTPTR(g_object_ref(dialog));
}

JNIEXPORT void JNICALL Java_android_app_Dialog_nativeSetTitle(JNIEnv *env, jobject this, jlong ptr, jstring title)
{
	GtkWindow *dialog = GTK_WINDOW(_PTR(ptr));
	const char *nativeTitle = (*env)->GetStringUTFChars(env, title, NULL);
	gtk_window_set_title(dialog, nativeTitle);
	(*env)->ReleaseStringUTFChars(env, title, nativeTitle);
}

JNIEXPORT void JNICALL Java_android_app_Dialog_nativeSetContentView(JNIEnv *env, jobject this, jlong ptr, jlong widget_ptr)
{
	GtkWindow *dialog = GTK_WINDOW(_PTR(ptr));
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));

	gtk_window_set_child(dialog, gtk_widget_get_parent(widget));
}

JNIEXPORT void JNICALL Java_android_app_Dialog_nativeShow(JNIEnv *env, jobject this, jlong ptr)
{
	GtkWindow *dialog = GTK_WINDOW(_PTR(ptr));
	gtk_window_present(dialog);
}

JNIEXPORT void JNICALL Java_android_app_Dialog_nativeClose(JNIEnv *env, jobject this, jlong ptr)
{
	GtkWindow *dialog = GTK_WINDOW(_PTR(ptr));
	remove_touch_outside(dialog);
	/* not gtk_window_close(): it emits close-request, which dismisses the dialog again */
	gtk_widget_set_visible(GTK_WIDGET(dialog), FALSE);
}

JNIEXPORT jboolean JNICALL Java_android_app_Dialog_nativeIsShowing(JNIEnv *env, jobject this, jlong ptr)
{
	GtkWindow *dialog = GTK_WINDOW(_PTR(ptr));
	return gtk_widget_is_visible(GTK_WIDGET(dialog));
}

JNIEXPORT void JNICALL Java_android_app_Dialog_nativeSetCanceledOnTouchOutside(JNIEnv *env, jobject this, jlong ptr, jboolean cancel)
{
	GtkWindow *dialog = GTK_WINDOW(_PTR(ptr));
	remove_touch_outside(dialog);
	if (!cancel)
		return;

	struct touch_outside *outside = g_new0(struct touch_outside, 1);
	outside->dialog = dialog;
	outside->jobj = _REF(this);

	GtkEventController *controller = GTK_EVENT_CONTROLLER(gtk_event_controller_legacy_new());
	/* capture phase on the toplevel runs before the view tree's own touch controllers */
	gtk_event_controller_set_propagation_phase(controller, GTK_PHASE_CAPTURE);
	g_signal_connect(controller, "event", G_CALLBACK(on_touch_outside), outside);
	gtk_widget_add_controller(GTK_WIDGET(window), controller);
	outside->controller = controller;

	g_object_set_data(G_OBJECT(dialog), "touch-outside", outside);
}
