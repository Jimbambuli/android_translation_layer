#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"

#include "WrapperWidget.h"
#include "android_widget_PopupWindow.h"

#include "../generated_headers/android_widget_PopupWindow.h"

JNIEXPORT jlong JNICALL Java_android_widget_PopupWindow_native_1constructor(JNIEnv *env, jobject this)
{
	GtkWidget *popover = gtk_popover_new();
	gtk_widget_set_name(popover, "PopupWindow");
	/* autohiding works by the widget grabbing events, which is not something apps expect */
	gtk_popover_set_autohide(GTK_POPOVER(popover), false);
	atl_popover_count_grab(GTK_POPOVER(popover));
	/* to dismiss it on a press outside of it, as AOSP's PopupDecorView does on ACTION_OUTSIDE */
	g_object_set_data(G_OBJECT(popover), "popup-window", _WEAK_REF(this));
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
		    "popover.%1$s modelbutton:hover, popover.%1$s modelbutton:selected { background-color: rgba(%6$d,%7$d,%8$d,0.12); }\n",
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

static void on_map_count_grab(GtkWidget *popover)
{
	if (gtk_popover_get_autohide(GTK_POPOVER(popover)))
		grabbing_popovers++;
}

static void on_unmap_count_grab(GtkWidget *popover)
{
	if (gtk_popover_get_autohide(GTK_POPOVER(popover)))
		grabbing_popovers--;
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

	if (!gtk_widget_get_visible(GTK_WIDGET(popover)))
		gtk_popover_set_autohide(popover, touch_modal && !(parent && gtk_widget_get_ancestor(parent, GTK_TYPE_POPOVER)) && may_grab());
}

static gboolean is_fading_out(GtkWidget *popover);

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
		    !(pressed && (GTK_WIDGET(pressed) == child || gtk_widget_is_ancestor(GTK_WIDGET(pressed), child)))) {
			JNIEnv *env = get_jni_env();
			jobject popup_window = (*env)->NewLocalRef(env, weak);
			if (popup_window) {
				(*env)->CallVoidMethod(env, popup_window, _METHOD(_CLASS(popup_window), "dismiss", "()V"));
				if ((*env)->ExceptionCheck(env))
					(*env)->ExceptionDescribe(env);
				(*env)->DeleteLocalRef(env, popup_window);
			}
		}
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

static void popup(GtkPopover *popover)
{
	watch_outer_popup(popover);
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
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionDescribe(env);
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

/* a popup that fades out after dismiss() is no longer showing and takes no input */
static gboolean is_fading_out(GtkWidget *popover)
{
	return GPOINTER_TO_INT(g_object_get_data(G_OBJECT(popover), "fading-out"));
}

static void reset_fade(GtkWidget *popover)
{
	atl_fade_widget_cancel(popover);
	g_object_set_data(G_OBJECT(popover), "fading-out", NULL);
	g_object_set_data(G_OBJECT(popover), "dismiss-notified", NULL);
	gtk_widget_set_can_target(popover, TRUE);
	gtk_widget_set_opacity(popover, 1);
}

static void fade_out_done(GtkWidget *popover)
{
	g_object_set_data(G_OBJECT(popover), "fading-out", NULL);
	gtk_popover_popdown(GTK_POPOVER(popover));
	gtk_widget_set_can_target(popover, TRUE);
	gtk_widget_set_opacity(popover, 1);
}

JNIEXPORT jboolean JNICALL Java_android_widget_PopupWindow_native_1isShowing(JNIEnv *env, jobject this, jlong popover_ptr)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	return gtk_widget_get_visible(popover) && !is_fading_out(popover);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1dismiss(JNIEnv *env, jobject this, jlong popover_ptr, jint fade_ms)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	if (fade_ms <= 0 || !gtk_widget_get_mapped(popover)) {
		reset_fade(popover);
		gtk_popover_popdown(GTK_POPOVER(popover));
		return;
	}
	if (is_fading_out(popover))
		return;
	g_object_set_data(G_OBJECT(popover), "fading-out", GINT_TO_POINTER(1));
	gtk_widget_set_can_target(popover, FALSE);
	g_object_set_data(G_OBJECT(popover), "dismiss-notified", GINT_TO_POINTER(1));
	atl_fade_widget(popover, 0, fade_ms, fade_out_done);
	notify_dismissed(popover);
}

/* called after the popover was shown: fade in like the popupEnterTransition (from 0.1) */
JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1fadeIn(JNIEnv *env, jobject this, jlong popover_ptr, jint fade_ms)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	reset_fade(popover);
	if (fade_ms <= 0)
		return;
	gtk_widget_set_opacity(popover, 0.1);
	atl_fade_widget(popover, 1, fade_ms, NULL);
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
