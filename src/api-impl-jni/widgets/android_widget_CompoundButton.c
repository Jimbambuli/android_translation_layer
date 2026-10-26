#include <gtk/gtk.h>

#include "../util.h"
#include "WrapperWidget.h"

#include "../generated_headers/android_widget_CompoundButton.h"
#include "jni.h"

static gboolean on_state_set(GtkSwitch *self, gboolean state, gpointer user_data);

JNIEXPORT jlong JNICALL Java_android_widget_CompoundButton_native_1constructor(JNIEnv *env, jobject this, jobject context, jobject attrs)
{
	GtkWidget *wrapper = g_object_ref_sink(wrapper_widget_new());
	GtkWidget *box = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 0);
	GtkWidget *label = gtk_label_new("");
	gtk_widget_set_hexpand(label, TRUE);
	gtk_widget_set_halign(label, GTK_ALIGN_START);
	gtk_box_append(GTK_BOX(box), label);
	GtkWidget *switcher = gtk_switch_new();
	g_signal_connect(switcher, "state-set", G_CALLBACK(on_state_set), NULL);
	gtk_box_append(GTK_BOX(box), switcher);
	wrapper_widget_set_child(WRAPPER_WIDGET(wrapper), box);
	wrapper_widget_set_jobject(WRAPPER_WIDGET(wrapper), env, this);
	return _INTPTR(box);
}

JNIEXPORT void JNICALL Java_android_widget_CompoundButton_setChecked(JNIEnv *env, jobject this, jboolean checked)
{
	gtk_switch_set_active(GTK_SWITCH(gtk_widget_get_last_child(_PTR(_GET_LONG_FIELD(this, "widget")))), checked);
}

JNIEXPORT jboolean JNICALL Java_android_widget_CompoundButton_isChecked(JNIEnv *env, jobject this)
{
	return gtk_switch_get_active(GTK_SWITCH(gtk_widget_get_last_child(_PTR(_GET_LONG_FIELD(this, "widget")))));
}

/* the listener is looked up in the view, as the native side holds no reference to it (also for
 * CheckBox and RadioButton) */
void compound_button_checked_changed(WrapperWidget *wrapper, gboolean checked)
{
	JNIEnv *env = get_jni_env();
	jobject view = (*env)->NewLocalRef(env, wrapper->jobj);
	if (!view) // garbage collected
		return;
	jobject listener = _GET_OBJ_FIELD(view, "mOnCheckedChangeListener", "Landroid/widget/CompoundButton$OnCheckedChangeListener;");
	if (listener) {
		J__CompoundButton__OnCheckedChangeListener__onCheckedChanged(env, listener, view, checked);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
		(*env)->DeleteLocalRef(env, listener);
	}
	(*env)->DeleteLocalRef(env, view);
}

static gboolean on_state_set(GtkSwitch *self, gboolean state, gpointer user_data)
{
	compound_button_checked_changed(WRAPPER_WIDGET(gtk_widget_get_parent(gtk_widget_get_parent(GTK_WIDGET(self)))), state);
	return FALSE;
}

JNIEXPORT void JNICALL Java_android_widget_CompoundButton_native_1setText(JNIEnv *env, jobject this, jlong widget_ptr, jstring text)
{
	GtkLabel *label = GTK_LABEL(gtk_widget_get_first_child(_PTR(widget_ptr)));
	const char *nativeText = (*env)->GetStringUTFChars(env, text, NULL);
	gtk_label_set_text(label, nativeText);
	(*env)->ReleaseStringUTFChars(env, text, nativeText);
}

#define CSS_RGBA(color) ((color) >> 16) & 0xFF, ((color) >> 8) & 0xFF, (color) & 0xFF, (((color) >> 24) & 0xFF) / 255.0

/* The switch takes the colors of Android's Material switch from the theme. The slider is a child
 * node of the switch, which a provider on the switch's own style context does not reach, so the
 * rules go to the display, once per set of colors, selected by a CSS class. */
JNIEXPORT void JNICALL Java_android_widget_CompoundButton_native_1setSwitchColors(JNIEnv *env, jobject this, jlong widget_ptr, jint thumb, jint thumb_checked, jint track, jint track_checked)
{
	static GHashTable *classes = NULL;
	GtkWidget *switcher = gtk_widget_get_last_child(GTK_WIDGET(_PTR(widget_ptr)));
	char *css_class = g_strdup_printf("atl-switch-%08x-%08x-%08x-%08x", thumb, thumb_checked, track, track_checked);

	if (!classes)
		classes = g_hash_table_new_full(g_str_hash, g_str_equal, g_free, NULL);
	if (!g_hash_table_contains(classes, css_class)) {
		GtkCssProvider *css_provider = gtk_css_provider_new();
		/* no positional arguments: musl's printf supports at most 9 (NL_ARGMAX) */
		char *css = g_strdup_printf(
		    "switch.%s { background-color: rgba(%d,%d,%d,%.3f); }\n"
		    "switch.%s:checked { background-color: rgba(%d,%d,%d,%.3f); }\n"
		    "switch.%s > slider { background-color: rgba(%d,%d,%d,%.3f); }\n"
		    "switch.%s:checked > slider { background-color: rgba(%d,%d,%d,%.3f); }\n",
		    css_class, CSS_RGBA(track), css_class, CSS_RGBA(track_checked),
		    css_class, CSS_RGBA(thumb), css_class, CSS_RGBA(thumb_checked));
		gtk_css_provider_load_from_string(css_provider, css);
		g_free(css);
		gtk_style_context_add_provider_for_display(gtk_widget_get_display(switcher), GTK_STYLE_PROVIDER(css_provider), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
		g_object_unref(css_provider);
		g_hash_table_add(classes, g_strdup(css_class));
	}
	gtk_widget_add_css_class(switcher, css_class);
	g_free(css_class);
}
