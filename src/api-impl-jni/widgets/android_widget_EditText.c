#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"

#include "WrapperWidget.h"

#include "../generated_headers/android_widget_EditText.h"

static void changed_cb(GtkEditable *self, gpointer user_data);
static void selection_changed_cb(GObject *self, GParamSpec *pspec, gpointer user_data);

JNIEXPORT jlong JNICALL Java_android_widget_EditText_native_1constructor(JNIEnv *env, jobject this, jobject context, jobject attrs)
{
	GtkWidget *wrapper = g_object_ref_sink(wrapper_widget_new());
	GtkWidget *gtk_text = gtk_text_new();
	wrapper_widget_set_child(WRAPPER_WIDGET(wrapper), gtk_text);
	wrapper_widget_set_jobject(WRAPPER_WIDGET(wrapper), env, this);
	/* InputTrace: the text the user types, whether or not the app has a TextWatcher */
	g_signal_connect(GTK_EDITABLE(gtk_text), "changed", G_CALLBACK(changed_cb), NULL);
	/* TextView.onSelectionChanged(): the cursor or the selection moved */
	g_signal_connect(gtk_text, "notify::cursor-position", G_CALLBACK(selection_changed_cb), NULL);
	g_signal_connect(gtk_text, "notify::selection-bound", G_CALLBACK(selection_changed_cb), NULL);
	wrapper_widget_register_invalidation_listener(WRAPPER_WIDGET(wrapper));
	return _INTPTR(gtk_text);
}

JNIEXPORT jstring JNICALL Java_android_widget_EditText_native_1getText(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkText *gtk_text = GTK_TEXT(_PTR(widget_ptr));
	const char *text = gtk_entry_buffer_get_text(gtk_text_get_buffer(gtk_text));
	return _JSTRING(text);
}

/* Like on AOSP, the EditText keeps its TextWatchers and its OnEditorActionListener; the GtkText only
 * knows the EditText, through the weak reference of its WrapperWidget. A global reference from the
 * widget to a listener would keep the listener's view tree alive for good: the widget is released
 * only when the EditText is finalized, which that reference prevents. */
static jobject get_edit_text(JNIEnv *env, GtkWidget *gtk_text)
{
	return (*env)->NewLocalRef(env, WRAPPER_WIDGET(gtk_widget_get_parent(gtk_text))->jobj);
}

static void changed_cb(GtkEditable *self, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	jobject this = get_edit_text(env, GTK_WIDGET(self));
	if (!this)
		return;
	J__EditText__sendTextChanged(env, this, _JSTRING(gtk_editable_get_text(self)));
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, this);
}

static void selection_changed_cb(GObject *self, GParamSpec *pspec, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	jobject this = get_edit_text(env, GTK_WIDGET(self));
	if (!this)
		return;
	J__EditText__sendSelectionChanged(env, this);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, this);
}

/* the selection as {start, end} in characters (not UTF-16 units); both are the cursor if nothing is selected */
JNIEXPORT jintArray JNICALL Java_android_widget_EditText_native_1getSelectionBounds(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkEditable *editable = GTK_EDITABLE(_PTR(widget_ptr));
	int start, end;
	if (!gtk_editable_get_selection_bounds(editable, &start, &end))
		start = end = gtk_editable_get_position(editable);
	jint bounds[2] = {start, end};
	jintArray result = (*env)->NewIntArray(env, 2);
	(*env)->SetIntArrayRegion(env, result, 0, 2, bounds);
	return result;
}

JNIEXPORT void JNICALL Java_android_widget_EditText_native_1setSelectionBounds(JNIEnv *env, jobject this, jlong widget_ptr, jint start, jint end)
{
	GtkEditable *editable = GTK_EDITABLE(_PTR(widget_ptr));
	if (start == end)
		gtk_editable_set_position(editable, start);
	else
		gtk_editable_select_region(editable, start, end);
}

JNIEXPORT void JNICALL Java_android_widget_EditText_native_1addTextChangedListener(JNIEnv *env, jobject this, jlong widget_ptr, jobject listener)
{
	GtkText *gtk_text = GTK_TEXT(_PTR(widget_ptr));
	if (!g_signal_handler_find(gtk_text, G_SIGNAL_MATCH_FUNC, 0, 0, NULL, changed_cb, NULL))
		g_signal_connect(GTK_EDITABLE(gtk_text), "changed", G_CALLBACK(changed_cb), NULL);
}

/* InputFilters: the EditText filters what is about to be inserted; if that changes the text, insert
 * the filtered text instead (the pattern from the GtkEditable documentation) */
static void insert_text_cb(GtkEditable *editable, const char *text, int length, int *position, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	jobject this = get_edit_text(env, GTK_WIDGET(editable));
	if (!this)
		return;
	char *source = length < 0 ? g_strdup(text) : g_strndup(text, length);
	jstring source_jstr = _JSTRING(source);
	g_free(source);
	jstring filtered = J__EditText__filterInsertion(env, this, source_jstr, *position);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	if (filtered) {
		const char *str = (*env)->GetStringUTFChars(env, filtered, NULL);
		g_signal_handlers_block_by_func(editable, insert_text_cb, user_data);
		gtk_editable_insert_text(editable, str, -1, position);
		g_signal_handlers_unblock_by_func(editable, insert_text_cb, user_data);
		g_signal_stop_emission_by_name(editable, "insert-text");
		(*env)->ReleaseStringUTFChars(env, filtered, str);
		(*env)->DeleteLocalRef(env, filtered);
	}
	(*env)->DeleteLocalRef(env, source_jstr);
	(*env)->DeleteLocalRef(env, this);
}

JNIEXPORT void JNICALL Java_android_widget_EditText_native_1setFiltered(JNIEnv *env, jobject this, jlong widget_ptr, jboolean filtered)
{
	GtkEditable *editable = GTK_EDITABLE(_PTR(widget_ptr));
	gboolean connected = g_signal_handler_find(editable, G_SIGNAL_MATCH_FUNC, 0, 0, NULL, insert_text_cb, NULL);
	if (filtered && !connected)
		g_signal_connect(editable, "insert-text", G_CALLBACK(insert_text_cb), NULL);
	else if (!filtered && connected)
		g_signal_handlers_disconnect_by_func(editable, insert_text_cb, NULL);
}

#define IME_ACTION_SEARCH 3
#define KEYCODE_ENTER     66

static void on_activate(GtkText *gtk_text, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	jobject this = get_edit_text(env, GTK_WIDGET(gtk_text));
	if (!this)
		return;
	jobject key_event = J_new__KeyEvent(env, (jlong)0, (jlong)0, IME_ACTION_SEARCH, KEYCODE_ENTER, 0, 0);
	J__EditText__sendEditorAction(env, this, key_event);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, this);
}

JNIEXPORT void JNICALL Java_android_widget_EditText_native_1setOnEditorActionListener(JNIEnv *env, jobject this, jlong widget_ptr, jobject listener)
{
	GtkText *gtk_text = GTK_TEXT(_PTR(widget_ptr));
	if (!g_signal_handler_find(gtk_text, G_SIGNAL_MATCH_FUNC, 0, 0, NULL, on_activate, NULL))
		g_signal_connect(gtk_text, "activate", G_CALLBACK(on_activate), NULL);
}

JNIEXPORT void JNICALL Java_android_widget_EditText_native_1setText(JNIEnv *env, jobject this, jlong widget_ptr, jstring text_jstr)
{
	const char *gtk_text = (*env)->GetStringUTFChars(env, text_jstr, NULL);
	jsize length = (*env)->GetStringUTFLength(env, text_jstr);
	gtk_entry_buffer_set_text(gtk_text_get_buffer(GTK_TEXT(_PTR(widget_ptr))), gtk_text, length);
	(*env)->ReleaseStringUTFChars(env, text_jstr, gtk_text);
}

JNIEXPORT void JNICALL Java_android_widget_EditText_native_1setHint(JNIEnv *env, jobject this, jlong widget_ptr, jstring text_jstr)
{
	const char *text = (*env)->GetStringUTFChars(env, text_jstr, NULL);
	gtk_text_set_placeholder_text(GTK_TEXT(_PTR(widget_ptr)), text);
	(*env)->ReleaseStringUTFChars(env, text_jstr, text);
}

JNIEXPORT jstring JNICALL Java_android_widget_EditText_native_1getHint(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkText *gtk_text = GTK_TEXT(_PTR(widget_ptr));
	const char *text = gtk_text_get_placeholder_text(gtk_text);
	return _JSTRING(text);
}

/* like TextView, the size goes into the text's Pango attributes */
JNIEXPORT void JNICALL Java_android_widget_EditText_native_1setTextSize(JNIEnv *env, jobject this, jlong widget_ptr, jfloat size)
{
	GtkText *text = GTK_TEXT(_PTR(widget_ptr));
	PangoAttrList *old_attrs = gtk_text_get_attributes(text);
	PangoAttrList *attrs = old_attrs ? pango_attr_list_copy(old_attrs) : pango_attr_list_new();

	pango_attr_list_change(attrs, pango_attr_size_new_absolute(size * PANGO_SCALE));
	gtk_text_set_attributes(text, attrs);
	pango_attr_list_unref(attrs);
}

/* the placeholder is an internal label of the GtkText, which neither a style provider nor the
 * attributes of the entry reach, so the hint color and the text's attributes go to the label */
JNIEXPORT void JNICALL Java_android_widget_EditText_native_1setHintColor(JNIEnv *env, jobject this, jlong widget_ptr, jint color)
{
	for (GtkWidget *child = gtk_widget_get_first_child(GTK_WIDGET(_PTR(widget_ptr))); child; child = gtk_widget_get_next_sibling(child)) {
		if (!GTK_IS_LABEL(child))
			continue;
		/* the text's attributes (its size) and the hint color */
		PangoAttrList *text_attrs = gtk_text_get_attributes(GTK_TEXT(_PTR(widget_ptr)));
		PangoAttrList *attrs = text_attrs ? pango_attr_list_copy(text_attrs) : pango_attr_list_new();
		pango_attr_list_change(attrs, pango_attr_foreground_new(((color >> 16) & 0xff) * 257, ((color >> 8) & 0xff) * 257, (color & 0xff) * 257));
		pango_attr_list_change(attrs, pango_attr_foreground_alpha_new(MAX(1, ((color >> 24) & 0xff) * 257)));
		gtk_label_set_attributes(GTK_LABEL(child), attrs);
		pango_attr_list_unref(attrs);
	}
}
