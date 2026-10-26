#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"

#include "WrapperWidget.h"

#include "../generated_headers/android_widget_Button.h"

static GtkLabel *box_get_label(JNIEnv *env, GtkWidget *box)
{
	GtkWidget *label = gtk_widget_get_last_child(GTK_WIDGET(box));
	if (!GTK_IS_LABEL(label))
		label = gtk_widget_get_prev_sibling(label);
	return GTK_LABEL(label);
}

JNIEXPORT jlong JNICALL Java_android_widget_Button_native_1constructor(JNIEnv *env, jobject this, jobject context, jobject attrs)
{
	const char *text = attribute_set_get_string(env, attrs, "text", NULL);

	GtkWidget *wrapper = g_object_ref_sink(wrapper_widget_new());
	GtkWidget *button = gtk_button_new();
	GtkWidget *box = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 0);
	gtk_button_set_child(GTK_BUTTON(button), box);
	GtkWidget *label = gtk_label_new(text);
	/* the text is centered in the space next to the compound drawable, as on Android */
	gtk_widget_set_hexpand(label, TRUE);
	gtk_box_append(GTK_BOX(box), label);
	wrapper_widget_set_child(WRAPPER_WIDGET(wrapper), button);
	wrapper_widget_set_jobject(WRAPPER_WIDGET(wrapper), env, this);

	return _INTPTR(button);
}

JNIEXPORT void JNICALL Java_android_widget_Button_native_1setText(JNIEnv *env, jobject this, jlong widget_ptr, jobject text)
{
	GtkButton *button = GTK_BUTTON(_PTR(widget_ptr));

	const char *nativeText = ((*env)->GetStringUTFChars(env, text, NULL));
	atl_safe_gtk_label_set_text(box_get_label(env, gtk_button_get_child(button)), nativeText);
	((*env)->ReleaseStringUTFChars(env, text, nativeText));
}

static void clicked_cb(GtkWidget *button, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(button));

	J__View__performClick(env, wrapper->jobj);

	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

JNIEXPORT void JNICALL Java_android_widget_Button_nativeSetOnClickListener(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *button = GTK_WIDGET(_PTR(widget_ptr));
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(button));
	wrapper_widget_consume_touch_events(wrapper); // Android button consumes touch events
	g_signal_handlers_disconnect_matched(button, G_SIGNAL_MATCH_FUNC, 0, 0, NULL, clicked_cb, NULL);

	g_signal_connect(button, "clicked", G_CALLBACK(clicked_cb), NULL);
}

JNIEXPORT jobject JNICALL Java_android_widget_Button_getText(JNIEnv *env, jobject this)
{
	GtkButton *button = GTK_BUTTON(_PTR(_GET_LONG_FIELD(this, "widget")));
	return (*env)->NewStringUTF(env, gtk_label_get_text(box_get_label(env, gtk_button_get_child(button))));
}

/* The start drawable of a button, drawn like TextView does on Android: in the drawable's bounds,
 * taken when it is set and after each layout (MaterialButton moves its icon next to the centered
 * text with setBounds() in onLayout()), since drawing the paintable sets the bounds to its size.
 * It takes the width and height of the bounds, is centered vertically, and is drawn moved by the
 * bounds' left and top. */
#define ATL_TYPE_COMPOUND_DRAWABLE (atl_compound_drawable_get_type())
G_DECLARE_FINAL_TYPE(AtlCompoundDrawable, atl_compound_drawable, ATL, COMPOUND_DRAWABLE, GtkWidget)

struct _AtlCompoundDrawable {
	GtkWidget parent_instance;
	GdkPaintable *paintable;
	jobject drawable; // global reference to the android.graphics.drawable.Drawable
	GdkRectangle bounds;
};

G_DEFINE_TYPE(AtlCompoundDrawable, atl_compound_drawable, GTK_TYPE_WIDGET)

static GdkRectangle drawable_bounds(AtlCompoundDrawable *self)
{
	JNIEnv *env = get_jni_env();
	if (!self->drawable)
		return (GdkRectangle){0};
	(*env)->PushLocalFrame(env, 8); // called from GTK, which never returns to Java to free them
	jobject bounds = _GET_OBJ_FIELD(self->drawable, "mBounds", "Landroid/graphics/Rect;");
	GdkRectangle rect = {
	    _GET_INT_FIELD(bounds, "left"),
	    _GET_INT_FIELD(bounds, "top"),
	    _GET_INT_FIELD(bounds, "right") - _GET_INT_FIELD(bounds, "left"),
	    _GET_INT_FIELD(bounds, "bottom") - _GET_INT_FIELD(bounds, "top"),
	};
	(*env)->PopLocalFrame(env, NULL);
	return rect;
}

static void atl_compound_drawable_measure(GtkWidget *widget, GtkOrientation orientation, int for_size, int *minimum, int *natural, int *minimum_baseline, int *natural_baseline)
{
	GdkRectangle bounds = ATL_COMPOUND_DRAWABLE(widget)->bounds;
	*minimum = *natural = orientation == GTK_ORIENTATION_HORIZONTAL ? bounds.width : bounds.height;
}

static void atl_compound_drawable_snapshot(GtkWidget *widget, GtkSnapshot *snapshot)
{
	AtlCompoundDrawable *self = ATL_COMPOUND_DRAWABLE(widget);
	if (!self->paintable)
		return;
	GdkRectangle bounds = self->bounds;
	gtk_snapshot_save(snapshot);
	gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(bounds.x, (gtk_widget_get_height(widget) - bounds.height) / 2 + bounds.y));
	gdk_paintable_snapshot(self->paintable, snapshot, bounds.width, bounds.height);
	gtk_snapshot_restore(snapshot);
}

static void compound_drawable_set(AtlCompoundDrawable *self, GdkPaintable *paintable, jobject drawable);

static void atl_compound_drawable_dispose(GObject *object)
{
	compound_drawable_set(ATL_COMPOUND_DRAWABLE(object), NULL, NULL);
	G_OBJECT_CLASS(atl_compound_drawable_parent_class)->dispose(object);
}

static void atl_compound_drawable_class_init(AtlCompoundDrawableClass *class)
{
	G_OBJECT_CLASS(class)->dispose = atl_compound_drawable_dispose;
	GTK_WIDGET_CLASS(class)->measure = atl_compound_drawable_measure;
	GTK_WIDGET_CLASS(class)->snapshot = atl_compound_drawable_snapshot;
}

static void atl_compound_drawable_init(AtlCompoundDrawable *self)
{
}

static void invalidate_contents_cb(GdkPaintable *paintable, GtkWidget *widget)
{
	gtk_widget_queue_draw(widget);
}

static void compound_drawable_set(AtlCompoundDrawable *self, GdkPaintable *paintable, jobject drawable)
{
	JNIEnv *env = get_jni_env();
	if (self->paintable)
		g_signal_handlers_disconnect_by_func(self->paintable, invalidate_contents_cb, self);
	g_set_object(&self->paintable, paintable);
	if (paintable)
		g_signal_connect_object(paintable, "invalidate-contents", G_CALLBACK(invalidate_contents_cb), self, 0);
	if (self->drawable)
		(*env)->DeleteGlobalRef(env, self->drawable);
	self->drawable = drawable ? (*env)->NewGlobalRef(env, drawable) : NULL;
	self->bounds = drawable_bounds(self);
}

JNIEXPORT void JNICALL Java_android_widget_Button_native_1setCompoundDrawables(JNIEnv *env, jobject this, jlong widget_ptr, jobject drawable)
{
	GtkButton *button = GTK_BUTTON(_PTR(widget_ptr));
	GdkPaintable *paintable = drawable ? GDK_PAINTABLE(_PTR(_GET_LONG_FIELD(drawable, "paintable"))) : NULL;
	GtkWidget *box = gtk_button_get_child(button);
	GtkWidget *child = gtk_widget_get_first_child(box);
	AtlCompoundDrawable *compound;
	if (ATL_IS_COMPOUND_DRAWABLE(child)) {
		compound = ATL_COMPOUND_DRAWABLE(child);
	} else if (paintable) {
		compound = g_object_new(ATL_TYPE_COMPOUND_DRAWABLE, NULL);
		gtk_widget_insert_after(GTK_WIDGET(compound), box, NULL);
	} else {
		return;
	}
	compound_drawable_set(compound, paintable, drawable);
	gtk_widget_set_visible(GTK_WIDGET(compound), paintable != NULL);
	gtk_widget_queue_resize(GTK_WIDGET(compound));
}

JNIEXPORT void JNICALL Java_android_widget_Button_native_1updateCompoundDrawable(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *child = gtk_widget_get_first_child(gtk_button_get_child(GTK_BUTTON(_PTR(widget_ptr))));
	if (ATL_IS_COMPOUND_DRAWABLE(child)) {
		ATL_COMPOUND_DRAWABLE(child)->bounds = drawable_bounds(ATL_COMPOUND_DRAWABLE(child));
		atl_safe_gtk_widget_queue_resize(child);
	}
}
