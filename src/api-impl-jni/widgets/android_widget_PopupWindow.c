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
	return _INTPTR(popover);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setContentView(JNIEnv *env, jobject this, jlong popover_ptr, jlong content_ptr)
{
	WrapperWidget *content = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(content_ptr))));
	gtk_popover_set_child(GTK_POPOVER(_PTR(popover_ptr)), GTK_WIDGET(content));
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
	int request_width, request_height;

	*width = *height = 0;
	gtk_widget_get_size_request(GTK_WIDGET(popover), &request_width, &request_height);
	if (contents) {
		gtk_widget_measure(contents, GTK_ORIENTATION_HORIZONTAL, -1, NULL, width, NULL, NULL);
		gtk_widget_measure(contents, GTK_ORIENTATION_VERTICAL, MAX(*width, request_width), NULL, height, NULL, NULL);
	}
	/* PopupWindow.setWidth()/setHeight() are size requests on the popover itself */
	*width = MAX(*width, request_width);
	*height = MAX(*height, request_height);
}

/* Show the popover with its top left corner at (x, y) in frame coordinates. We compute the final
 * position ourselves, like Android does, and point GTK at a 1x1 rectangle there: on Wayland the
 * rectangle becomes the xdg_positioner anchor rectangle, which is relative to the parent surface
 * and must not be empty, so pointing at an anchor with an empty allocation put the popup at the
 * window's origin. */
static void place_popover(GtkPopover *popover, GtkWidget *frame, int x, int y)
{
	GtkWidget *parent = gtk_widget_get_parent(GTK_WIDGET(popover));
	graphene_point_t p;

	if (!gtk_widget_compute_point(frame, parent, &GRAPHENE_POINT_INIT(x, y), &p))
		p = GRAPHENE_POINT_INIT(x, y);
	gtk_popover_set_has_arrow(popover, FALSE);
	gtk_popover_set_position(popover, GTK_POS_BOTTOM);
	gtk_widget_set_halign(GTK_WIDGET(popover), GTK_ALIGN_START);
	gtk_popover_set_offset(popover, 0, 0);
	gtk_popover_set_pointing_to(popover, &(GdkRectangle){.x = p.x, .y = p.y - 1, .width = 1, .height = 1});
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

	if (!gtk_widget_compute_point(anchor, frame, &GRAPHENE_POINT_INIT(0, 0), &a))
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

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1showAsDropDown(JNIEnv *env, jobject this, jlong popover_ptr, jlong anchor_ptr, jint anchor_width, jint anchor_height, jint x, jint y, jint gravity, jboolean overlap_anchor)
{
	GtkPopover *popover = GTK_POPOVER(_PTR(popover_ptr));
	WrapperWidget *anchor = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(anchor_ptr))));

	gtk_widget_insert_before(GTK_WIDGET(popover), GTK_WIDGET(anchor), NULL);
	atl_popover_place_as_dropdown(popover, GTK_WIDGET(anchor), anchor_width, anchor_height, x, y, gravity, overlap_anchor);
	gtk_popover_present(GTK_POPOVER(popover));
	gtk_popover_popup(popover);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1showAtLocation(JNIEnv *env, jobject this, jlong popover_ptr, jlong parent_ptr, jint gravity, jint x, jint y)
{
	GtkPopover *popover = GTK_POPOVER(_PTR(popover_ptr));
	WrapperWidget *parent = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(parent_ptr))));

	gtk_widget_insert_before(GTK_WIDGET(popover), GTK_WIDGET(parent), NULL);
	atl_popover_place_at_location(popover, gravity, x, y);
	gtk_popover_present(GTK_POPOVER(popover));
	gtk_popover_popup(popover);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setWidth(JNIEnv *env, jobject this, jlong popover_ptr, jint width)
{
	int height;
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	gtk_widget_get_size_request(popover, NULL, &height);
	gtk_widget_set_size_request(popover, width, height);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1setHeight(JNIEnv *env, jobject this, jlong popover_ptr, jint height)
{
	int width;
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	gtk_widget_get_size_request(popover, &width, NULL);
	gtk_widget_set_size_request(popover, width, height);
}

JNIEXPORT jint JNICALL Java_android_widget_PopupWindow_native_1getWidth(JNIEnv *env, jobject this, jlong popover_ptr)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	GtkRequisition natural_size;
	gtk_widget_get_preferred_size(popover, &natural_size, NULL);
	return natural_size.width;
}

JNIEXPORT jint JNICALL Java_android_widget_PopupWindow_native_1getHeight(JNIEnv *env, jobject this, jlong popover_ptr)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(popover_ptr));
	GtkRequisition natural_size;
	gtk_widget_get_preferred_size(popover, &natural_size, NULL);
	return natural_size.height;
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
	gtk_popover_set_autohide(popover, touch_modal);
}

static void on_closed_cb(GtkPopover *popover, jobject listener)
{
	JNIEnv *env = get_jni_env();
	J__PopupWindow__OnDismissListener__onDismiss(env, listener);
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_setOnDismissListener(JNIEnv *env, jobject this, jobject listener)
{
	GtkWidget *popover = GTK_WIDGET(_PTR(_GET_LONG_FIELD(this, "popover")));
	if (listener)
		g_signal_connect(popover, "closed", G_CALLBACK(on_closed_cb), _REF(listener));
}

JNIEXPORT jboolean JNICALL Java_android_widget_PopupWindow_native_1isShowing(JNIEnv *env, jobject this, jlong popover_ptr)
{
	return gtk_widget_get_visible(GTK_WIDGET(_PTR(popover_ptr)));
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1dismiss(JNIEnv *env, jobject this, jlong popover_ptr)
{
	gtk_popover_popdown(GTK_POPOVER(_PTR(popover_ptr)));
}

JNIEXPORT void JNICALL Java_android_widget_PopupWindow_native_1update(JNIEnv *env, jobject this, jlong popover_ptr, jlong anchor_ptr, jint anchor_width, jint anchor_height, jint x, jint y, jint width, jint height, jint gravity, jboolean overlap_anchor)
{
	GtkPopover *popover = GTK_POPOVER(_PTR(popover_ptr));
	WrapperWidget *anchor = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(_PTR(anchor_ptr))));
	gtk_widget_set_size_request(GTK_WIDGET(popover), width, height);
	gtk_widget_insert_before(GTK_WIDGET(popover), GTK_WIDGET(anchor), NULL);
	atl_popover_place_as_dropdown(popover, GTK_WIDGET(anchor), anchor_width, anchor_height, x, y, gravity, overlap_anchor);
	gtk_popover_present(GTK_POPOVER(popover));
	gtk_popover_popup(popover);
}
