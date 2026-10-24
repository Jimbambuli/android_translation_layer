#include <gtk/gtk.h>
#include <stdio.h>

#include "../defines.h"
#include "../util.h"

#include "WrapperWidget.h"

#include "../generated_headers/android_widget_SeekBar.h"
#include "jni.h"

JNIEXPORT jlong JNICALL Java_android_widget_SeekBar_native_1constructor(JNIEnv *env, jobject this, jobject context, jobject attrs)
{
	GtkWidget *wrapper = g_object_ref_sink(wrapper_widget_new());
	GtkWidget *scale = gtk_scale_new(GTK_ORIENTATION_HORIZONTAL, NULL);
	gtk_range_set_range(GTK_RANGE(scale), 0, 100);
	wrapper_widget_set_child(WRAPPER_WIDGET(wrapper), scale);
	wrapper_widget_set_jobject(WRAPPER_WIDGET(wrapper), env, this);
	gtk_widget_set_name(scale, "SeekBar");
	return _INTPTR(scale);
}

JNIEXPORT void JNICALL Java_android_widget_SeekBar_native_1setMax(JNIEnv *env, jobject this, jlong widget_ptr, jint max)
{
	GtkRange *range = GTK_RANGE(_PTR(widget_ptr));
	gtk_range_set_range(range, 0, max);
}

JNIEXPORT void JNICALL Java_android_widget_SeekBar_native_1setProgress(JNIEnv *env, jobject this, jlong widget_ptr, jfloat progress)
{
	GtkRange *range = GTK_RANGE(_PTR(widget_ptr));
	gtk_range_set_value(range, progress);
}

static void on_change_value(GtkRange *self, GtkScrollType *scroll, gdouble value, jobject listener)
{
	JNIEnv *env = get_jni_env();
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(self)));
	J__SeekBar__OnSeekBarChangeListener__onStartTrackingTouch(env, listener, wrapper->jobj);
	J__SeekBar__OnSeekBarChangeListener__onProgressChanged(env, listener, wrapper->jobj, (int)value, TRUE);
	J__SeekBar__OnSeekBarChangeListener__onStopTrackingTouch(env, listener, wrapper->jobj);
}

JNIEXPORT void JNICALL Java_android_widget_SeekBar_setOnSeekBarChangeListener(JNIEnv *env, jobject this, jobject listener)
{
	GtkRange *range = GTK_RANGE(_PTR(_GET_LONG_FIELD(this, "widget")));
	g_signal_handlers_disconnect_matched(range, G_SIGNAL_MATCH_FUNC, 0, 0, NULL, on_change_value, NULL);
	if (listener) {
		g_signal_connect(range, "change_value", G_CALLBACK(on_change_value), _REF(listener));
	}
}

JNIEXPORT jint JNICALL Java_android_widget_SeekBar_native_1getProgress(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkRange *range = GTK_RANGE(_PTR(widget_ptr));
	return gtk_range_get_value(range);
}

#define CSS_RGBA(color) ((color) >> 16) & 0xFF, ((color) >> 8) & 0xFF, (color) & 0xFF, (((color) >> 24) & 0xFF) / 255.0

/* The scale takes the colors of Android's Material seekbar from the theme. Trough, highlight and
 * slider are child nodes of the scale, which a provider on the scale's own style context does not
 * reach, so the rules go to the display, once per pair of colors, selected by a CSS class. */
JNIEXPORT void JNICALL Java_android_widget_SeekBar_native_1setColors(JNIEnv *env, jobject this, jlong widget_ptr, jint activated, jint track)
{
	static GHashTable *classes = NULL;
	GtkWidget *scale = GTK_WIDGET(_PTR(widget_ptr));
	char *css_class = g_strdup_printf("atl-seekbar-%08x-%08x", activated, track);

	if (!classes)
		classes = g_hash_table_new_full(g_str_hash, g_str_equal, g_free, NULL);
	if (!g_hash_table_contains(classes, css_class)) {
		GtkCssProvider *css_provider = gtk_css_provider_new();
		char *css = g_strdup_printf(
		    "scale.%1$s trough { background-color: rgba(%6$d,%7$d,%8$d,%9$.3f); }\n"
		    "scale.%1$s highlight, scale.%1$s slider { background-color: rgba(%2$d,%3$d,%4$d,%5$.3f); background-image: none; border-color: rgba(%2$d,%3$d,%4$d,%5$.3f); }\n"
		    /* Material's thumb is a small filled dot */
		    "scale.%1$s slider { min-width: 12px; min-height: 12px; margin: -4px; box-shadow: none; }\n",
		    css_class, CSS_RGBA(activated), CSS_RGBA(track));
		gtk_css_provider_load_from_string(css_provider, css);
		g_free(css);
		gtk_style_context_add_provider_for_display(gtk_widget_get_display(scale), GTK_STYLE_PROVIDER(css_provider), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
		g_object_unref(css_provider);
		g_hash_table_add(classes, g_strdup(css_class));
	}
	gtk_widget_add_css_class(scale, css_class);
	g_free(css_class);
}
