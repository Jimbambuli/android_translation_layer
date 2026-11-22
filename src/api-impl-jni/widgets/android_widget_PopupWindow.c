#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"

#include "WrapperWidget.h"
#include "android_widget_PopupWindow.h"

#include "../generated_headers/android_widget_PopupWindow.h"

#define KEYCODE_ESCAPE 111
#define ACTION_DOWN 0
#define ACTION_UP 1

/* On AOSP the popup's decor view gets the keys first (PopupWindow.PopupDecorView.dispatchKeyEvent())
 * and dismisses the popup on Back or Escape: here in the capture phase, before the content and
 * before GtkPopover's own key handler, which would pop the popup down on Escape by itself. */
static gboolean dispatch_decor_key(GtkEventControllerKey *controller, guint keyval, int action)
{
	if (keyval != GDK_KEY_Escape)
		return FALSE;
	GtkWidget *popover = gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(controller));
	JNIEnv *env = get_jni_env();
	jobject popup_window = (*env)->NewLocalRef(env, g_object_get_data(G_OBJECT(popover), "popup-window"));
	if (!popup_window)
		return FALSE;
	jobject key_event = J_new__KeyEvent(env, (jlong)0, (jlong)0, action, KEYCODE_ESCAPE, 0, 0);
	gboolean ret = J__PopupWindow__dispatchDecorKeyEvent(env, popup_window, key_event);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, key_event);
	(*env)->DeleteLocalRef(env, popup_window);
	return ret;
}

static gboolean on_decor_key_pressed(GtkEventControllerKey *controller, guint keyval, guint keycode, GdkModifierType state, gpointer user_data)
{
	return dispatch_decor_key(controller, keyval, ACTION_DOWN);
}

static gboolean on_decor_key_released(GtkEventControllerKey *controller, guint keyval, guint keycode, GdkModifierType state, gpointer user_data)
{
	return dispatch_decor_key(controller, keyval, ACTION_UP);
}

JNIEXPORT jlong JNICALL Java_android_widget_PopupWindow_native_1constructor(JNIEnv *env, jobject this)
{
	GtkWidget *popover = gtk_popover_new();
	gtk_widget_set_name(popover, "PopupWindow");
	/* autohiding works by the widget grabbing events, which is not something apps expect */
	gtk_popover_set_autohide(GTK_POPOVER(popover), false);
	atl_popover_count_grab(GTK_POPOVER(popover));
	/* to dismiss it on a press outside of it, as AOSP's PopupDecorView does on ACTION_OUTSIDE */
	g_object_set_data(G_OBJECT(popover), "popup-window", _WEAK_REF(this));
	GtkEventController *key_controller = gtk_event_controller_key_new();
	gtk_event_controller_set_propagation_phase(key_controller, GTK_PHASE_CAPTURE);
	g_signal_connect(key_controller, "key-pressed", G_CALLBACK(on_decor_key_pressed), NULL);
	g_signal_connect(key_controller, "key-released", G_CALLBACK(on_decor_key_released), NULL);
	gtk_widget_add_controller(popover, key_controller);
	return _INTPTR(popover);
}

/* The size an app sets is the size of its content view (with the content's own background padding),
 * so it is requested for the content; the popover adds the frame of the GTK theme around it. */
static void apply_content_size(GtkWidget *popover)
{
	GtkWidget *child = gtk_popover_get_child(GTK_POPOVER(popover));
	if (child)
		gtk_widget_set_size_request(child, GPOINTER_TO_INT(g_object_get_data(G_OBJECT(popover), "content-width")) - 1,
		                            GPOINTER_TO_INT(g_object_get_data(G_OBJECT(popover), "content-height")) - 1);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setContentView(JNIEnv *env, jobject this, jlong popover_ptr, jlong content_ptr)
{
	GtkWidget *old_child = gtk_popover_get_child(GTK_POPOVER(_PTR(popover_ptr)));
	if (old_child)
		gtk_widget_set_size_request(old_child, -1, -1);
	if (!content_ptr) {
		gtk_popover_set_child(GTK_POPOVER(_PTR(popover_ptr)), NULL);
		return;
	}
	WrapperWidget *content = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(content_ptr))));
	gtk_popover_set_child(GTK_POPOVER(_PTR(popover_ptr)), GTK_WIDGET(content));
	apply_content_size(GTK_WIDGET(_PTR(popover_ptr)));
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setTransparentBackground(JNIEnv *env, jobject this, jlong popover_ptr, jboolean transparent)
{
	if (transparent)
		gtk_widget_add_css_class(GTK_WIDGET(_PTR(popover_ptr)), "atl-transparent-background");
	else
		gtk_widget_remove_css_class(GTK_WIDGET(_PTR(popover_ptr)), "atl-transparent-background");
}

/* android.view.Gravity */
#define GRAVITY_CENTER_HORIZONTAL 0x01
#define GRAVITY_RIGHT 0x05
#define GRAVITY_HORIZONTAL_MASK 0x07
#define GRAVITY_CENTER_VERTICAL 0x10
#define GRAVITY_BOTTOM 0x50
#define GRAVITY_VERTICAL_MASK 0x70

/* the widget that corresponds to the Android window (the decor view's container) */
static GtkWidget *window_frame(GtkWidget *widget)
{
	GtkRoot *root = gtk_widget_get_root(widget);
	GtkWidget *child = GTK_IS_WINDOW(root) ? gtk_window_get_child(GTK_WINDOW(root)) : NULL;
	return child ? child : GTK_WIDGET(root);
}

/* a hidden popover measures as 0x0 (GTK skips invisible widgets), so measure its first child, the
 * box that holds the content and draws the background; its size is the visible size of the popup */
static void measure_popover(GtkPopover *popover, int *width, int *height)
{
	GtkWidget *contents = gtk_widget_get_first_child(GTK_WIDGET(popover));

	*width = *height = 0;
	if (contents) {
		gtk_widget_measure(contents, GTK_ORIENTATION_HORIZONTAL, -1, NULL, width, NULL, NULL);
		gtk_widget_measure(contents, GTK_ORIENTATION_VERTICAL, *width, NULL, height, NULL, NULL);
	}
}

/* Show the popover with its top left corner at (x, y) in frame coordinates. We compute the final
 * position ourselves, like Android does, and point GTK at a 1x1 rectangle there: on Wayland the
 * rectangle becomes the xdg_positioner anchor rectangle, which is relative to the parent surface
 * and must not be empty, so pointing at an anchor with an empty allocation put the popup at the
 * window's origin. */
static void place_popover(GtkPopover *popover, GtkWidget *frame, int x, int y)
{
	GtkWidget *parent = gtk_widget_get_parent(GTK_WIDGET(popover));
	graphene_point_t p = GRAPHENE_POINT_INIT(x, y);
	graphene_point_t frame_origin, parent_origin;

	/* a submenu's parent is in another popup: go through the window's surface */
	if (atl_compute_point_in_window(frame, &GRAPHENE_POINT_INIT(0, 0), &frame_origin) &&
	    atl_compute_point_in_window(parent, &GRAPHENE_POINT_INIT(0, 0), &parent_origin))
		p = GRAPHENE_POINT_INIT(x + frame_origin.x - parent_origin.x, y + frame_origin.y - parent_origin.y);
	gtk_popover_set_has_arrow(popover, FALSE);
	gtk_popover_set_position(popover, GTK_POS_BOTTOM);
	gtk_widget_set_halign(GTK_WIDGET(popover), GTK_ALIGN_START);
	/* GTK moves the rectangle into the parent's bounds, which a submenu next to its menu is not in:
	 * point at the nearest point inside and move the popup the rest of the way by its offset */
	int px = CLAMP((int)p.x, 0, MAX(gtk_widget_get_width(parent) - 1, 0));
	int py = CLAMP((int)p.y, 1, MAX(gtk_widget_get_height(parent), 1));
	gtk_popover_set_offset(popover, (int)p.x - px, (int)p.y - py);
	gtk_popover_set_pointing_to(popover, &(GdkRectangle){.x = px, .y = py - 1, .width = 1, .height = 1});
}

static int clamp_to_frame(int pos, int size, int frame_size)
{
	return MAX(0, MIN(pos, frame_size - size));
}

void atl_popover_place_as_dropdown(GtkPopover *popover, GtkWidget *anchor, int anchor_width, int anchor_height, int xoff, int yoff, int gravity, gboolean overlap_anchor)
{
	GtkWidget *frame = window_frame(anchor);
	int frame_width = gtk_widget_get_width(frame);
	int frame_height = gtk_widget_get_height(frame);
	graphene_point_t a;
	int width, height, x, y;

	/* a hidden (GONE) anchor keeps the place of its last layout on Android, where update() puts a
	 * showing popup again (ListPopupWindow.show() does on every change of the menu); GTK no longer
	 * places an unmapped widget, so a popup that is showing stays where it is */
	if (gtk_widget_get_visible(GTK_WIDGET(popover)) && !gtk_widget_get_mapped(anchor))
		return;

	graphene_point_t frame_origin;
	if (atl_compute_point_in_window(anchor, &GRAPHENE_POINT_INIT(0, 0), &a) &&
	    atl_compute_point_in_window(frame, &GRAPHENE_POINT_INIT(0, 0), &frame_origin))
		a = GRAPHENE_POINT_INIT(a.x - frame_origin.x, a.y - frame_origin.y);
	else
		a = GRAPHENE_POINT_INIT(0, 0);
	measure_popover(popover, &width, &height);

	x = a.x + xoff;
	if ((gravity & GRAVITY_HORIZONTAL_MASK) == GRAVITY_RIGHT)
		x -= width - anchor_width;
	/* like PopupWindow.findDropDownPosition()/tryFitVertical(): below the anchor (or over it),
	 * moved above it if it only fits there */
	y = a.y + (overlap_anchor ? 0 : anchor_height) + yoff;
	if (y + height > frame_height) {
		int above = a.y + (overlap_anchor ? anchor_height : 0) - height + yoff;
		if (above >= 0)
			y = above;
	}
	/* PopupWindow.isAboveAnchor(): AOSP keeps the result of findDropDownPosition() in mAboveAnchor */
	g_object_set_data(G_OBJECT(popover), "atl-above-anchor", GINT_TO_POINTER(y < a.y + (overlap_anchor ? 0 : anchor_height) + yoff));

	place_popover(popover, frame, clamp_to_frame(x, width, frame_width), clamp_to_frame(y, height, frame_height));
}

void atl_popover_place_at_location(GtkPopover *popover, int gravity, int x, int y)
{
	GtkWidget *frame = window_frame(GTK_WIDGET(popover));
	int frame_width = gtk_widget_get_width(frame);
	int frame_height = gtk_widget_get_height(frame);
	int width, height;

	measure_popover(popover, &width, &height);
	/* like Gravity.apply() */
	switch (gravity & GRAVITY_HORIZONTAL_MASK) {
		case GRAVITY_CENTER_HORIZONTAL:
			x += (frame_width - width) / 2;
			break;
		case GRAVITY_RIGHT:
			x = frame_width - width - x;
			break;
	}
	switch (gravity & GRAVITY_VERTICAL_MASK) {
		case GRAVITY_CENTER_VERTICAL:
			y += (frame_height - height) / 2;
			break;
		case GRAVITY_BOTTOM:
			y = frame_height - height - y;
			break;
	}
	place_popover(popover, frame, x, y);
}

#define CSS_RGBA(color) ((color) >> 16) & 0xFF, ((color) >> 8) & 0xFF, (color) & 0xFF, (((color) >> 24) & 0xFF) / 255.0

/* The contents node is internal to the popover, which a provider on the popover's own style context
 * does not reach, so the rules go to the display, once per pair of colors, selected by a CSS class. */
void atl_popover_set_colors(GtkPopover *popover, int background, int foreground)
{
	static GHashTable *classes = NULL;
	char *css_class = g_strdup_printf("atl-popup-%08x-%08x", background, foreground);

	if (!classes)
		classes = g_hash_table_new_full(g_str_hash, g_str_equal, g_free, NULL);
	if (!g_hash_table_contains(classes, css_class)) {
		GtkCssProvider *css_provider = gtk_css_provider_new();
		/* menu items are highlighted like Android's colorControlHighlight, the text color at 12 % */
		char *css = g_strdup_printf(
		    "popover.%1$s > contents { background-color: rgba(%2$d,%3$d,%4$d,%5$.3f); color: rgba(%6$d,%7$d,%8$d,%9$.3f); }\n"
		    "popover.%1$s modelbutton:hover, popover.%1$s modelbutton:selected { background-color: rgba(%6$d,%7$d,%8$d,0.12); }\n"
		    /* a window without background (see default-stylesheet.css): a provider added later wins over the
		     * stylesheet for the same priority, so the rule is repeated here */
		    "popover.%1$s.atl-transparent-background > contents { background: none; border: none; box-shadow: none; }\n",
		    css_class, CSS_RGBA(background), CSS_RGBA(foreground));
		gtk_css_provider_load_from_string(css_provider, css);
		g_free(css);
		gtk_style_context_add_provider_for_display(gtk_widget_get_display(GTK_WIDGET(popover)), GTK_STYLE_PROVIDER(css_provider), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
		g_object_unref(css_provider);
		g_hash_table_add(classes, g_strdup(css_class));
	}
	gtk_widget_add_css_class(GTK_WIDGET(popover), css_class);
	g_free(css_class);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setColors(JNIEnv *env, jobject this, jlong popover_ptr, jint background, jint foreground)
{
	atl_popover_set_colors(GTK_POPOVER(_PTR(popover_ptr)), background, foreground);
}

/* Android shows a popup in a window of its own, which stays while its anchor is hidden or
 * recycled (video controls that hide themselves, list items). A GtkPopover is a child widget and
 * is closed together with its parent, so it goes into the widget of the anchor's window instead,
 * or for a popup anchored in another popup (a submenu) into the content of that popup, which
 * keeps it a child of the outer popup for GDK. */
static GtkWidget *popover_parent_for(GtkWidget *anchor)
{
	GtkWidget *outer = gtk_widget_get_ancestor(anchor, GTK_TYPE_POPOVER);
	GtkWidget *content = outer ? gtk_popover_get_child(GTK_POPOVER(outer)) : NULL;
	return content ? content : window_frame(anchor);
}

/* the grabbing (autohide) popovers that are mapped */
static int grabbing_popovers = 0;

static void free_weak_ref(gpointer ref)
{
	g_weak_ref_clear(ref);
	g_free(ref);
}

/* the popovers that are mapped, see on_window_press() */
static GList *mapped_popovers = NULL;

static void on_map_count_grab(GtkWidget *popover)
{
	mapped_popovers = g_list_prepend(mapped_popovers, popover);
	if (!gtk_popover_get_autohide(GTK_POPOVER(popover)))
		return;
	grabbing_popovers++;
	/* the widget that has the focus before GtkPopover moves it into the popup */
	GtkRoot *root = gtk_widget_get_root(popover);
	GWeakRef *focus = g_new0(GWeakRef, 1);
	g_weak_ref_init(focus, root ? gtk_root_get_focus(root) : NULL);
	g_object_set_data_full(G_OBJECT(popover), "focus-before", focus, free_weak_ref);
}

/* GtkPopover moves the keyboard focus into a grabbing popup, but does not give it back when the popup
 * closes: GtkWindow then focuses the first widget it finds from the top, a button in the header bar,
 * where the keys no longer reach the app (Tab, Escape). Like on Android, where the app's window gets
 * the focus back with the view that had it, the focus goes back to that widget, or else into the
 * content of the window. Where no widget there takes it, the window keeps the focus itself, as an
 * Android window with no focused view still gets the keys.
 * The focus is also given back from a widget that was taken out of the popup while it had the focus
 * (ListPopupWindow.dismiss() drops the content view during the exit fade): GtkWindow would move such
 * a focus on, but GtkPopover's focus() claims to keep it while the popup grabs, so it stayed on the
 * removed widget, and no key reached the window any more. */
static void give_focus_back(GtkWidget *popover)
{
	GtkRoot *root = gtk_widget_get_root(popover);
	GtkWidget *focus = root ? gtk_root_get_focus(root) : NULL;
	if (!GTK_IS_WINDOW(root) || (focus && gtk_widget_get_root(focus) == root && !gtk_widget_is_ancestor(focus, popover)))
		return;
	GWeakRef *focus_before = g_object_get_data(G_OBJECT(popover), "focus-before");
	GtkWidget *before = focus_before ? g_weak_ref_get(focus_before) : NULL;
	if (!(before && gtk_widget_get_mapped(before) && !gtk_widget_is_ancestor(before, popover) && gtk_widget_grab_focus(before))) {
		GtkWidget *content = gtk_window_get_child(GTK_WINDOW(root));
		if (!(content && gtk_widget_child_focus(content, GTK_DIR_TAB_FORWARD)))
			gtk_root_set_focus(root, NULL);
	}
	g_clear_object(&before);
}

static void on_unmap_count_grab(GtkWidget *popover)
{
	mapped_popovers = g_list_remove(mapped_popovers, popover);
	if (!gtk_popover_get_autohide(GTK_POPOVER(popover)))
		return;
	grabbing_popovers--;
	give_focus_back(popover);
}

void atl_popover_count_grab(GtkPopover *popover)
{
	g_signal_connect(popover, "map", G_CALLBACK(on_map_count_grab), NULL);
	g_signal_connect(popover, "unmap", G_CALLBACK(on_unmap_count_grab), NULL);
}

/* GDK maps a grabbing popup only as the child of the topmost grabbing popup. Any other one it
 * refuses to map, but it keeps the seat grabbed for it (GTK 4 has no way to end that grab), and
 * the app takes no more input. So a popup that is shown while another popup grabs does not grab. */
static gboolean may_grab(void)
{
	return grabbing_popovers == 0;
}

/* GtkPopover unrealizes, i.e. closes, itself when autohide changes, so the touch modality set with
 * setTouchModal()/setOutsideTouchable() is applied only while the popup is hidden. A submenu
 * never grabs: GDK hides a grabbing popup on a press in its parent popup, and the outer popup's
 * grab already ends both on outside presses. A press in the outer popup dismisses it instead
 * (see on_outer_popup_event). */
static void apply_touch_modal(GtkPopover *popover)
{
	GtkWidget *parent = gtk_widget_get_parent(GTK_WIDGET(popover));
	gboolean touch_modal = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(popover), "touch-modal"));
	gboolean focusable = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(popover), "focusable"));

	if (!gtk_widget_get_visible(GTK_WIDGET(popover))) {
		/* AOSP's popup window that is not focusable (FLAG_NOT_FOCUSABLE) is never touch-modal and
		 * leaves the keyboard focus in the app's window, e.g. with the view that types into a
		 * drop-down (AutoCompleteTextView); GtkPopover moves the focus into a grabbing popup, and
		 * the focus must not get into the popup's widgets on a click either */
		gtk_popover_set_autohide(popover, touch_modal && focusable && !(parent && gtk_widget_get_ancestor(parent, GTK_TYPE_POPOVER)) && may_grab());
		gtk_widget_set_can_focus(GTK_WIDGET(popover), focusable);
	}
}

static gboolean is_fading_out(GtkWidget *popover);

static void dismiss_popup_window(GtkWidget *popover)
{
	JNIEnv *env = get_jni_env();
	jobject popup_window = (*env)->NewLocalRef(env, g_object_get_data(G_OBJECT(popover), "popup-window"));
	if (popup_window) {
		(*env)->CallVoidMethod(env, popup_window, _METHOD(_CLASS(popup_window), "dismiss", "()V"));
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
		(*env)->DeleteLocalRef(env, popup_window);
	}
}

/* AOSP gives a press outside of an outside-touchable popup to the popup as ACTION_OUTSIDE, and
 * the popup dismisses itself; a submenu (which CascadingMenuPopup makes not touch-modal) also
 * lets the press through to the menu behind it. A submenu here is a popover in the content of
 * its menu, so presses in the menu pass this menu's capture phase: dismiss its submenus that the
 * press is not in, before the menu sees the press. Otherwise the press on another submenu item
 * opens that submenu while the first is still showing, as if it were a submenu of the first. */
static gboolean on_outer_popup_event(GtkEventControllerLegacy *controller, GdkEvent *event, gpointer user_data)
{
	GdkEventType type = gdk_event_get_event_type(event);
	if (type != GDK_BUTTON_PRESS && type != GDK_TOUCH_BEGIN)
		return FALSE;
	GtkWidget *content = gtk_popover_get_child(GTK_POPOVER(gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(controller))));
	GtkNative *pressed = gtk_native_get_for_surface(gdk_event_get_surface(event));
	GtkWidget *child = content ? gtk_widget_get_first_child(content) : NULL;
	while (child) {
		GtkWidget *next = gtk_widget_get_next_sibling(child);
		jobject weak = g_object_get_data(G_OBJECT(child), "popup-window");
		if (GTK_IS_POPOVER(child) && weak && gtk_widget_get_visible(child) && !is_fading_out(child) &&
		    GPOINTER_TO_INT(g_object_get_data(G_OBJECT(child), "touch-modal")) &&
		    !(pressed && (GTK_WIDGET(pressed) == child || gtk_widget_is_ancestor(GTK_WIDGET(pressed), child))))
			dismiss_popup_window(child);
		child = next;
	}
	return FALSE;
}

static void watch_outer_popup(GtkPopover *popover)
{
	GtkWidget *parent = gtk_widget_get_parent(GTK_WIDGET(popover));
	GtkWidget *outer = parent ? gtk_widget_get_ancestor(parent, GTK_TYPE_POPOVER) : NULL;
	if (!outer || g_object_get_data(G_OBJECT(outer), "watches-submenus"))
		return;
	GtkEventController *controller = gtk_event_controller_legacy_new();
	gtk_event_controller_set_propagation_phase(controller, GTK_PHASE_CAPTURE);
	g_signal_connect(controller, "event", G_CALLBACK(on_outer_popup_event), NULL);
	gtk_widget_add_controller(outer, controller);
	g_object_set_data(G_OBJECT(outer), "watches-submenus", GINT_TO_POINTER(1));
}

/* A popup that does not grab (not focusable) and is outside-touchable gets a press outside of it
 * as ACTION_OUTSIDE on AOSP and dismisses itself, while the press also goes to the window below.
 * The window sees the presses in its popovers too, so the press is outside of the popups whose
 * surface it is not on. */
static gboolean on_window_press(GtkEventControllerLegacy *controller, GdkEvent *event, gpointer user_data)
{
	GdkEventType type = gdk_event_get_event_type(event);
	if (type != GDK_BUTTON_PRESS && type != GDK_TOUCH_BEGIN)
		return FALSE;
	GtkWidget *window = gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(controller));
	GtkNative *pressed = gtk_native_get_for_surface(gdk_event_get_surface(event));
	GPtrArray *outside = g_ptr_array_new_with_free_func(g_object_unref);
	for (GList *l = mapped_popovers; l; l = l->next) {
		GtkWidget *popover = l->data;
		if (g_object_get_data(G_OBJECT(popover), "popup-window") && GTK_WIDGET(gtk_widget_get_root(popover)) == window &&
		    !gtk_popover_get_autohide(GTK_POPOVER(popover)) &&
		    !GPOINTER_TO_INT(g_object_get_data(G_OBJECT(popover), "focusable")) &&
		    GPOINTER_TO_INT(g_object_get_data(G_OBJECT(popover), "touch-modal")) && !is_fading_out(popover) &&
		    !(pressed && (GTK_WIDGET(pressed) == popover || gtk_widget_is_ancestor(GTK_WIDGET(pressed), popover))))
			g_ptr_array_add(outside, g_object_ref(popover));
	}
	for (guint i = 0; i < outside->len; i++)
		dismiss_popup_window(g_ptr_array_index(outside, i));
	g_ptr_array_unref(outside);
	return FALSE;
}

static void watch_window(GtkPopover *popover)
{
	GtkRoot *root = gtk_widget_get_root(GTK_WIDGET(popover));
	if (!root || g_object_get_data(G_OBJECT(root), "watches-popups"))
		return;
	GtkEventController *controller = gtk_event_controller_legacy_new();
	gtk_event_controller_set_propagation_phase(controller, GTK_PHASE_CAPTURE);
	g_signal_connect(controller, "event", G_CALLBACK(on_window_press), NULL);
	gtk_widget_add_controller(GTK_WIDGET(root), controller);
	g_object_set_data(G_OBJECT(root), "watches-popups", GINT_TO_POINTER(1));
}

static void popup(GtkPopover *popover)
{
	watch_outer_popup(popover);
	watch_window(popover);
	gtk_popover_present(popover);
	gtk_popover_popup(popover);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1showAsDropDown(JNIEnv *env, jobject this, jlong popover_ptr, jlong anchor_ptr, jint anchor_width, jint anchor_height, jint x, jint y, jint gravity, jboolean overlap_anchor)
{
	GtkPopover *popover = GTK_POPOVER(_PTR(popover_ptr));
	WrapperWidget *anchor = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(anchor_ptr))));

	gtk_widget_insert_before(GTK_WIDGET(popover), popover_parent_for(GTK_WIDGET(anchor)), NULL);
	apply_touch_modal(popover);
	atl_popover_place_as_dropdown(popover, GTK_WIDGET(anchor), anchor_width, anchor_height, x, y, gravity, overlap_anchor);
	popup(popover);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1showAtLocation(JNIEnv *env, jobject this, jlong popover_ptr, jlong parent_ptr, jint gravity, jint x, jint y)
{
	GtkPopover *popover = GTK_POPOVER(_PTR(popover_ptr));
	WrapperWidget *parent = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(parent_ptr))));

	gtk_widget_insert_before(GTK_WIDGET(popover), GTK_WIDGET(parent), NULL);
	apply_touch_modal(popover);
	atl_popover_place_at_location(popover, gravity, x, y);
	popup(popover);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setWidth(JNIEnv *env, jobject this, jlong popover_ptr, jint width)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	g_object_set_data(G_OBJECT(popover), "content-width", GINT_TO_POINTER(width + 1));
	apply_content_size(popover);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setHeight(JNIEnv *env, jobject this, jlong popover_ptr, jint height)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	g_object_set_data(G_OBJECT(popover), "content-height", GINT_TO_POINTER(height + 1));
	apply_content_size(popover);
}

/* the size of the content, like the size set with setWidth()/setHeight() */
static GtkRequisition content_size(GtkWidget *popover)
{
	GtkWidget *child = gtk_popover_get_child(GTK_POPOVER(popover));
	GtkRequisition natural_size;
	gtk_widget_get_preferred_size(child ? child : popover, &natural_size, NULL);
	return natural_size;
}

JNIEXPORT jint JNICALL Java_android_widget_PopupWindow_native_1getWidth(JNIEnv *env, jobject this, jlong popover_ptr)
{
	return content_size(GTK_WIDGET(_PTR(popover_ptr))).width;
}

JNIEXPORT jint JNICALL Java_android_widget_PopupWindow_native_1getHeight(JNIEnv *env, jobject this, jlong popover_ptr)
{
	return content_size(GTK_WIDGET(_PTR(popover_ptr))).height;
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setTouchable(JNIEnv *env, jobject this, jlong popover_ptr, jboolean touchable)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	gtk_widget_set_sensitive(popover, touchable);
}

JNIEXPORT jboolean JNICALL Java_android_widget_PopupWindow_native_1isTouchable(JNIEnv *env, jobject this, jlong popover_ptr)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	return gtk_widget_is_sensitive(popover);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setFocusable(JNIEnv *env, jobject this, jlong popover_ptr, jboolean focusable)
{
	GtkPopover *popover = GTK_POPOVER(_PTR(popover_ptr));
	g_object_set_data(G_OBJECT(popover), "focusable", GINT_TO_POINTER(focusable));
	apply_touch_modal(popover);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setTouchModal(JNIEnv *env, jobject this, jlong popover_ptr, jboolean touch_modal)
{
	GtkPopover *popover = GTK_POPOVER(_PTR(popover_ptr));
	/* FIXME: we should only add grab (not autohide), however we need to remove it again in umap;
	 * GtkPopover is not final, so we should subclass it and check whether it's modal in map/unmap
	 * to add/remove grab, which is the desired part of what GtkPopover does with autohide enabled */
	g_object_set_data(G_OBJECT(popover), "touch-modal", GINT_TO_POINTER(touch_modal));
	apply_touch_modal(popover);
}

/* AOSP calls the OnDismissListener from dismiss(), before the exit transition has played (a
 * CascadingMenuPopup learns from it which submenu is gone), so a popup that fades out is reported
 * at dismiss() and not again when the popover closes at the end of the fade */
static void notify_dismissed(GtkWidget *popover)
{
	JNIEnv *env = get_jni_env();
	/* like AOSP, the PopupWindow keeps its listener; a global reference to it from the popover
	 * would keep the listener's views (and their activity) alive for good */
	jobject this = (*env)->NewLocalRef(env, g_object_get_data(G_OBJECT(popover), "popup-window"));
	if (!this)
		return;
	J__PopupWindow__dispatchDismiss(env, this);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, this);
}

static void on_closed_cb(GtkPopover *popover, gpointer user_data)
{
	if (g_object_get_data(G_OBJECT(popover), "dismiss-notified")) {
		g_object_set_data(G_OBJECT(popover), "dismiss-notified", NULL);
		return;
	}
	notify_dismissed(GTK_WIDGET(popover));
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setOnDismissListener(JNIEnv *env, jobject this, jlong popover_ptr)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	if (!g_signal_handler_find(popover, G_SIGNAL_MATCH_FUNC, 0, 0, NULL, on_closed_cb, NULL))
		g_signal_connect(popover, "closed", G_CALLBACK(on_closed_cb), NULL);
}

/* PopupWindow.java plays the popup's transitions: it sets the opacity of the popover, keeps it from
 * taking input while it fades out, and pops it down when the fade ends */
JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setOpacity(JNIEnv *env, jobject this, jlong popover_ptr, jfloat opacity)
{
	gtk_widget_set_opacity(GTK_WIDGET(_PTR(popover_ptr)), opacity);
}

/* a popup that fades out after dismiss() is no longer showing and takes no input */
static gboolean is_fading_out(GtkWidget *popover)
{
	return GPOINTER_TO_INT(g_object_get_data(G_OBJECT(popover), "fading-out"));
}

static void notify_dismissed(GtkWidget *popover);

/* AOSP's PopupWindow.dismiss() starts the exit transition, calls the OnDismissListener at once and
 * drops the window when the transition ends: the popover is then no longer showing, takes no input,
 * and its closing is not reported again (on_closed_cb) */
JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1startFadeOut(JNIEnv *env, jobject this, jlong popover_ptr)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	g_object_set_data(G_OBJECT(popover), "fading-out", GINT_TO_POINTER(1));
	gtk_widget_set_can_target(popover, FALSE);
	/* AOSP's dismiss() makes the popup window FLAG_NOT_FOCUSABLE for the exit transition, so the
	 * window below gets the keyboard focus back at once, not only when the popup is gone */
	gtk_widget_set_can_focus(popover, FALSE);
	give_focus_back(popover);
	g_object_set_data(G_OBJECT(popover), "dismiss-notified", GINT_TO_POINTER(1));
	notify_dismissed(popover);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1fadeOutDone(JNIEnv *env, jobject this, jlong popover_ptr)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	g_object_set_data(G_OBJECT(popover), "fading-out", NULL);
	gtk_popover_popdown(GTK_POPOVER(popover));
	gtk_widget_set_can_target(popover, TRUE);
	gtk_widget_set_can_focus(popover, GPOINTER_TO_INT(g_object_get_data(G_OBJECT(popover), "focusable")));
	gtk_widget_set_opacity(popover, 1);
}

/* the popup is shown again, or dismissed at once */
JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1resetFade(JNIEnv *env, jobject this, jlong popover_ptr)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	g_object_set_data(G_OBJECT(popover), "fading-out", NULL);
	g_object_set_data(G_OBJECT(popover), "dismiss-notified", NULL);
	gtk_widget_set_can_target(popover, TRUE);
	gtk_widget_set_can_focus(popover, GPOINTER_TO_INT(g_object_get_data(G_OBJECT(popover), "focusable")));
	gtk_widget_set_opacity(popover, 1);
}

JNIEXPORT jboolean JNICALL Java_android_widget_PopupWindow_native_1isShowing(JNIEnv *env, jobject this, jlong popover_ptr)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	return gtk_widget_get_visible(popover) && !is_fading_out(popover);
}

JNIEXPORT jboolean JNICALL Java_android_widget_PopupWindow_native_1isMapped(JNIEnv *env, jobject this, jlong popover_ptr)
{
	return gtk_widget_get_mapped(GTK_WIDGET(_PTR(popover_ptr)));
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1dismiss(JNIEnv *env, jobject this, jlong popover_ptr)
{
	gtk_popover_popdown(GTK_POPOVER(_PTR(popover_ptr)));
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1update(JNIEnv *env, jobject this, jlong popover_ptr, jlong anchor_ptr, jint anchor_width, jint anchor_height, jint x, jint y, jint width, jint height, jint gravity, jboolean overlap_anchor)
{
	GtkPopover *popover = GTK_POPOVER(_PTR(popover_ptr));
	WrapperWidget *anchor = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(anchor_ptr))));
	/* like AOSP, -1 keeps the current size */
	if (width >= 0)
		g_object_set_data(G_OBJECT(popover), "content-width", GINT_TO_POINTER(width + 1));
	if (height >= 0)
		g_object_set_data(G_OBJECT(popover), "content-height", GINT_TO_POINTER(height + 1));
	apply_content_size(GTK_WIDGET(popover));
	gtk_widget_insert_before(GTK_WIDGET(popover), popover_parent_for(GTK_WIDGET(anchor)), NULL);
	apply_touch_modal(popover);
	atl_popover_place_as_dropdown(popover, GTK_WIDGET(anchor), anchor_width, anchor_height, x, y, gravity, overlap_anchor);
	popup(popover);
}

JNIEXPORT jboolean JNICALL Java_android_widget_PopupWindow_native_1isAboveAnchor(JNIEnv *env, jobject this, jlong popover_ptr)
{
	return GPOINTER_TO_INT(g_object_get_data(G_OBJECT(_PTR(popover_ptr)), "atl-above-anchor")) != 0;
}

/* PopupWindow.update(x, y, width, height): a shown popup moves to (x, y) in the window, as AOSP sets
 * WindowManager.LayoutParams.x/y */
JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1moveTo(JNIEnv *env, jobject this, jlong popover_ptr, jint gravity, jint x, jint y)
{
	atl_popover_place_at_location(GTK_POPOVER(_PTR(popover_ptr)), gravity, x, y);
}
