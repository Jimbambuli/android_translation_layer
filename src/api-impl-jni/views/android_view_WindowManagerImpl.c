#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_view_WindowManagerImpl.h"
#include "../widgets/android_widget_PopupWindow.h"
#include "../app/android_app_Activity.h"
#include "gdk/gdk.h"
#include "glib-object.h"

#define FIRST_SUB_WINDOW 1000
#define LAST_SUB_WINDOW  1999

extern GtkWindow *window;

static jclass window_manager_class;

/* touch-modal windows that are added, and whether the rest of a press is swallowed */
static int touch_modal_windows = 0;
static gboolean swallowing_press = FALSE;

/* GTK closed a touch-modal window (press outside of it, Escape): tell the view, as AOSP would by
 * giving it the press, so that the app removes the window */
static void on_closed(GtkPopover *popover, jobject view)
{
	/* the compositor takes the press that closed it and passes on only the rest */
	swallowing_press = TRUE;
	JNIEnv *env = get_jni_env();
	(*env)->CallStaticVoidMethod(env, window_manager_class, _STATIC_METHOD(window_manager_class, "dispatchOutsideTouch", "(Landroid/view/View;)V"), view);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

static void release_view(gpointer view, GClosure *closure)
{
	JNIEnv *env = get_jni_env();
	_UNREF(view);
}

/* A touch-modal window takes the presses outside of it, so the app's window must not see them
 * (on AOSP, tapping another button while a menu is open only closes the menu): neither the rest
 * of the press that made GTK close the popover nor one while the popover is still shown. */
static gboolean on_window_event(GtkEventControllerLegacy *controller, GdkEvent *event, gpointer user_data)
{
	GdkEventType type = gdk_event_get_event_type(event);
	if (type == GDK_BUTTON_PRESS || type == GDK_TOUCH_BEGIN) {
		swallowing_press = touch_modal_windows > 0 && gdk_event_get_surface(event) == gtk_native_get_surface(GTK_NATIVE(window));
		return swallowing_press;
	}
	if (!swallowing_press)
		return FALSE;
	/* no button is held (anymore), so there is no rest of a press to swallow */
	if (type == GDK_MOTION_NOTIFY && !(gdk_event_get_modifier_state(event) & GDK_BUTTON1_MASK)) {
		swallowing_press = FALSE;
		return FALSE;
	}
	if (type == GDK_BUTTON_RELEASE || type == GDK_TOUCH_END || type == GDK_TOUCH_CANCEL)
		swallowing_press = FALSE;
	return type == GDK_MOTION_NOTIFY || type == GDK_TOUCH_UPDATE || type == GDK_BUTTON_RELEASE || type == GDK_TOUCH_END || type == GDK_TOUCH_CANCEL;
}

/* watch the presses in a window of the app, in the capture phase, before its views see them */
static void watch_window(GtkWidget *window_widget)
{
	if (g_object_get_data(G_OBJECT(window_widget), "watches-presses"))
		return;
	GtkEventController *controller = GTK_EVENT_CONTROLLER(gtk_event_controller_legacy_new());
	g_signal_connect(controller, "event", G_CALLBACK(on_window_event), NULL);
	gtk_event_controller_set_propagation_phase(controller, GTK_PHASE_CAPTURE);
	gtk_widget_add_controller(window_widget, controller);
	g_object_set_data(G_OBJECT(window_widget), "watches-presses", GINT_TO_POINTER(1));
}

/* A grabbing popup that closes on a press outside of it takes the press like AOSP's touch-modal
 * popups, but the compositor passes the rest of it (motion, release) on to the window below,
 * whose views would take it for a swipe from where their last press was. */
void atl_swallow_rest_of_press(GtkWidget *widget)
{
	GtkRoot *root = gtk_widget_get_root(widget);
	if (!root)
		return;
	watch_window(GTK_WIDGET(root));
	swallowing_press = TRUE;
}

/* A window added with WindowManager.addView() gets Escape as KEYCODE_ESCAPE through its views,
 * which decide what it does (Compose's popups close on Back only); unlike a GtkPopover, an Android
 * window has no default action for it. So an Escape that the views did not consume stops here,
 * after them and before GtkPopover's key handler, which would pop the window down. */
static gboolean on_window_key_pressed(GtkEventControllerKey *controller, guint keyval, guint keycode, GdkModifierType state, gpointer user_data)
{
	return keyval == GDK_KEY_Escape;
}

/* the window shows only its view, which draws its own background and shadow (Compose's menus),
 * without the frame and padding of the GTK theme's popover */
static void add_window_style(GtkPopover *popover)
{
	static gboolean provider_added = FALSE;
	if (!provider_added) {
		GtkCssProvider *css_provider = gtk_css_provider_new();
		gtk_css_provider_load_from_string(css_provider, "popover.atl-window > contents { background: none; border: none; box-shadow: none; padding: 0; }");
		gtk_style_context_add_provider_for_display(gtk_widget_get_display(GTK_WIDGET(popover)), GTK_STYLE_PROVIDER(css_provider), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
		g_object_unref(css_provider);
		provider_added = TRUE;
	}
	gtk_widget_add_css_class(GTK_WIDGET(popover), "atl-window");
}

/* An added window is a popover in the widget of the app's window, like the popups of
 * PopupWindow: not in the current activity's page, which removes it while its views are detached
 * from the window (Compose removes its popups then), and with its top left corner at (x, y) as the
 * gravity measures them. A touch-modal window closes on a press outside of it (GTK's autohide),
 * which the view learns as an outside touch. */
JNIEXPORT void JNICALL Java_android_view_WindowManagerImpl_native_1addView(JNIEnv *env, jclass clazz, jlong widget_ptr, jobject view, jint type, jboolean touch_modal, jint gravity, jint x, jint y, jint width, jint height)
{
	GtkWidget *widget = _PTR(widget_ptr);
	if (type < FIRST_SUB_WINDOW || type > LAST_SUB_WINDOW) {
		// TODO: handle toplevel windows properly
		printf("WARNING: non subwindow types not implemented properly in WindowManagerImpl\n");
	}
	if (!window_manager_class)
		window_manager_class = _REF(clazz);
	GtkPopover *popover = GTK_POPOVER(g_object_ref(gtk_popover_new()));
	gtk_popover_set_child(popover, gtk_widget_get_parent(widget));
	add_window_style(popover);
	gtk_popover_set_autohide(popover, touch_modal);
	atl_popover_count_grab(popover);
	if (touch_modal) {
		watch_window(GTK_WIDGET(window));
		touch_modal_windows++;
		g_object_set_data(G_OBJECT(popover), "touch-modal", GINT_TO_POINTER(1));
		g_signal_connect_data(popover, "closed", G_CALLBACK(on_closed), _REF(view), release_view, 0);
	}
	GtkEventController *key_controller = GTK_EVENT_CONTROLLER(gtk_event_controller_key_new());
	g_signal_connect(key_controller, "key-pressed", G_CALLBACK(on_window_key_pressed), NULL);
	gtk_widget_add_controller(gtk_widget_get_parent(widget), key_controller);
	gtk_widget_insert_before(GTK_WIDGET(popover), activity_pages_stack(), NULL);
	atl_popover_place_at_location(popover, gravity, x, y);
	gtk_popover_present(popover);
	gtk_popover_popup(popover);
}

JNIEXPORT void JNICALL Java_android_view_WindowManagerImpl_native_1updateViewLayout(JNIEnv *env, jclass clazz, jlong widget_ptr, jint gravity, jint x, jint y, jint width, jint height)
{
	GtkPopover *popover = GTK_POPOVER(gtk_widget_get_parent(gtk_widget_get_parent(gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr))))));
	atl_popover_place_at_location(popover, gravity, x, y);
	gtk_popover_present(popover);
}

JNIEXPORT void JNICALL Java_android_view_WindowManagerImpl_native_1removeView(JNIEnv *env, jclass clazz, jlong widget_ptr)
{
	GtkPopover *popover = GTK_POPOVER(gtk_widget_get_parent(gtk_widget_get_parent(gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr))))));
	if (g_object_get_data(G_OBJECT(popover), "touch-modal"))
		touch_modal_windows--;
	g_signal_handlers_disconnect_matched(popover, G_SIGNAL_MATCH_FUNC, 0, 0, NULL, on_closed, NULL);
	gtk_popover_popdown(popover);
	gtk_widget_unparent(GTK_WIDGET(popover));
	gtk_popover_set_child(popover, NULL);
	g_object_unref(popover);
}
