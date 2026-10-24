#ifndef WRAPPER_WIDGET_H
#define WRAPPER_WIDGET_H

#include <gtk/gtk.h>
#include <jni.h>

G_DECLARE_FINAL_TYPE(WrapperWidget, wrapper_widget, WRAPPER, WIDGET, GtkWidget)

struct _WrapperWidget {
	GtkWidget parent_instance;
	GtkWidget *child;
	GtkWidget *background;
	JavaVM *jvm;
	jobject jobj;
	jobject canvas;
	int layout_width;
	int layout_height;
	int real_width;
	int real_height;
	gboolean needs_allocation;
	gboolean intercepting_touch;
	gboolean custom_dispatch_touch;
	gboolean hover_exit_pending;
	gboolean has_focus;
	gboolean rtl_resolved;
	gboolean animating; /* a view animation (android.view.animation) is set on the view */
	guint animation_tick; /* redraws the view in every frame while the animation runs */
	int hover_x;
	int hover_y;
	bool have_custom_draw_method;
	bool have_custom_computeScroll;
	/* elevation shadow around the view's outline, drawn when shadow_z > 0 */
	graphene_rect_t shadow_rect;
	float shadow_radius;
	float shadow_z;
	float shadow_alpha;
};

/* object data "atl-scrim" of a WrapperWidget: a black layer drawn over the widget (or around it,
 * outside its bounds) with the given alpha, for the fade of an activity change */
struct atl_scrim {
	float alpha;
	gboolean outside;
};

struct _WrapperWidgetClass {
	GtkWidgetClass parent_class;
};

extern GtkWidget *currently_unmapping;
/* the window that currently_unmapping was attached to */
extern GtkRoot *currently_unmapping_root;

GtkWidget *wrapper_widget_new(void);
void wrapper_widget_set_child(WrapperWidget *parent, GtkWidget *child);
void wrapper_widget_set_jobject(WrapperWidget *wrapper, JNIEnv *env, jobject jobj);
void wrapper_widget_draw_through_java(WrapperWidget *wrapper, JNIEnv *env);
void wrapper_widget_queue_draw(WrapperWidget *wrapper);
void wrapper_widget_set_layout_params(WrapperWidget *wrapper, int width, int height);
void wrapper_widget_set_background(WrapperWidget *wrapper, GdkPaintable *paintable);
void wrapper_widget_set_shadow(WrapperWidget *wrapper, graphene_rect_t *rect, float radius, float z, float alpha);
void wrapper_widget_snapshot_shadow(GtkWidget *parent, GtkWidget *child, GdkSnapshot *snapshot);
void wrapper_widget_consume_touch_events(WrapperWidget *wrapper);
void wrapper_widget_register_invalidation_listener(WrapperWidget *wrapper);
void wrapper_widget_set_animating(WrapperWidget *wrapper, gboolean animating);

void _setOnTouchListener(JNIEnv *env, jobject this, GtkWidget *widget);

#endif
