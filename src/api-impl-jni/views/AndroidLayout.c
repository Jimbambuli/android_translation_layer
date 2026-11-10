#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"
#include "AndroidLayout.h"
#include "../widgets/WrapperWidget.h"

static int make_measure_spec(int layout_size, int for_size)
{
	if (layout_size >= 0)
		return layout_size | MEASURE_SPEC_EXACTLY;
	else if (for_size >= 0 && layout_size == MATCH_PARENT)
		return for_size | MEASURE_SPEC_EXACTLY;
	else if (for_size >= 0 && layout_size == WRAP_CONTENT)
		return for_size | MEASURE_SPEC_AT_MOST;
	else if (layout_size == WRAP_CONTENT)
		return MEASURE_SPEC_UNSPECIFIED;
	else
		return -1;
}

extern int snapshot_in_progress;

/* the view of the WrapperWidget around the widget: a view costs one weak reference, not two */
static jobject get_view(JNIEnv *env, GtkWidget *widget)
{
	GtkWidget *wrapper = gtk_widget_get_parent(widget);
	return WRAPPER_IS_WIDGET(wrapper) ? (*env)->NewLocalRef(env, WRAPPER_WIDGET(wrapper)->jobj) : NULL;
}

static void android_layout_measure(GtkLayoutManager *layout_manager, GtkWidget *widget, GtkOrientation orientation, int for_size, int *minimum, int *natural, int *minimum_baseline, int *natural_baseline)
{
	int widthMeasureSpec = 0;
	int heightMeasureSpec = 0;
	AndroidLayout *layout = ATL_ANDROID_LAYOUT(layout_manager);
	JNIEnv *env = get_jni_env();

	/* if we're inside a shanpshot, this must be getting called purely to make Gtk call gtk_widget_clear_resize_queued */
	if (snapshot_in_progress)
		return;

	jobject view = get_view(env, widget);
	if (!view) {
		*minimum = *natural = 0;
		*minimum_baseline = *natural_baseline = -1;
		return;
	}

	// If the parent widget is also an AndroidLayout, the measurement will already have happened in Java.
	// The same holds for the child of a ScrollView (in its GtkViewport): ScrollView.onMeasure() measures it,
	// with fillViewport a second time at the height of the viewport, which must not be measured over again
	GtkWidget *parent = gtk_widget_get_parent(gtk_widget_get_parent(widget));
	if (!ATL_IS_ANDROID_LAYOUT(gtk_widget_get_layout_manager(parent)) && !(GTK_IS_VIEWPORT(parent) && GTK_IS_SCROLLED_WINDOW(gtk_widget_get_parent(parent)))) {
		widthMeasureSpec = make_measure_spec(layout->width, orientation == GTK_ORIENTATION_VERTICAL ? for_size : -1);
		heightMeasureSpec = make_measure_spec(layout->height, orientation == GTK_ORIENTATION_HORIZONTAL ? for_size : -1);

		// if layout params say match_parent, but GTK doesn't specify the dimension, fall back to old specification if available
		if (widthMeasureSpec == -1)
			widthMeasureSpec = _GET_INT_FIELD(view, "oldWidthMeasureSpec");
		if (heightMeasureSpec == -1)
			heightMeasureSpec = _GET_INT_FIELD(view, "oldHeightMeasureSpec");
		if (widthMeasureSpec != -1 && heightMeasureSpec != -1) {
			J__View__measure(env, view, widthMeasureSpec, heightMeasureSpec);
			if ((*env)->ExceptionCheck(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
			}
		}
	}

	if (orientation == GTK_ORIENTATION_HORIZONTAL) {
		*natural = J__View__getMeasuredWidth(env, view);
		*minimum = heightMeasureSpec && !widthMeasureSpec ? *natural
		                                                  : J__View__getSuggestedMinimumWidth(env, view);
	} else if (orientation == GTK_ORIENTATION_VERTICAL) {
		*natural = J__View__getMeasuredHeight(env, view);
		*minimum = widthMeasureSpec && !heightMeasureSpec ? *natural
		                                                  : J__View__getSuggestedMinimumHeight(env, view);
	}
	if (*natural < *minimum)
		*natural = *minimum;

	*minimum_baseline = -1;
	*natural_baseline = -1;
	(*env)->DeleteLocalRef(env, view);
}

static void android_layout_allocate(GtkLayoutManager *layout_manager, GtkWidget *widget, int width, int height, int baseline)
{
	/* if we're inside a shanpshot, this must be getting called purely to make Gtk call gtk_widget_clear_resize_queued */
	if (snapshot_in_progress)
		return;

	AndroidLayout *layout = ATL_ANDROID_LAYOUT(layout_manager);
	JNIEnv *env = get_jni_env();
	if (!width && !height) {
		width = layout->real_width;
		height = layout->real_height;
	}

	jobject view = get_view(env, widget);
	if (!view)
		return;
	J__View__layoutInternal(env, view, width, height);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, view);
}

static GtkSizeRequestMode android_layout_get_request_mode(GtkLayoutManager *layout_manager, GtkWidget *widget)
{
	AndroidLayout *layout = ATL_ANDROID_LAYOUT(layout_manager);

	if (layout->height == WRAP_CONTENT) {
		return GTK_SIZE_REQUEST_HEIGHT_FOR_WIDTH;
	} else if (layout->width == WRAP_CONTENT) {
		return GTK_SIZE_REQUEST_WIDTH_FOR_HEIGHT;
	} else {
		return GTK_SIZE_REQUEST_CONSTANT_SIZE;
	}
}

static void android_layout_class_init(AndroidLayoutClass *klass)
{
	klass->parent_class.measure = android_layout_measure;
	klass->parent_class.allocate = android_layout_allocate;
	klass->parent_class.get_request_mode = android_layout_get_request_mode;
}

static void android_layout_init(AndroidLayout *self) {}

G_DEFINE_TYPE(AndroidLayout, android_layout, GTK_TYPE_LAYOUT_MANAGER)

GtkLayoutManager *android_layout_new(void)
{
	AndroidLayout *layout = g_object_new(android_layout_get_type(), NULL);
	layout->width = MATCH_PARENT;
	layout->height = MATCH_PARENT;
	return &layout->parent_instance;
}

void android_layout_set_params(AndroidLayout *layout, int width, int height)
{
	layout->width = width;
	layout->height = height;
}

void widget_set_needs_allocation(GtkWidget *widget)
{
	if (ATL_IS_ANDROID_LAYOUT(gtk_widget_get_layout_manager(widget))) {
		AndroidLayout *layout = ATL_ANDROID_LAYOUT(gtk_widget_get_layout_manager(widget));
		if (!layout->needs_allocation && (layout->real_width || layout->real_height))
			gtk_widget_size_allocate(widget, &(GtkAllocation){.x = 0, .y = 0, .width = layout->real_width, .height = layout->real_height}, 0);
		layout->needs_allocation = true;
	}
}
