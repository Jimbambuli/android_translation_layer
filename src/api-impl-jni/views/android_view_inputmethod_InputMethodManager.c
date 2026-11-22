#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"
#include "jni.h"

#include "../generated_headers/android_view_inputmethod_InputMethodManager.h"

static jobject connection;

static void commit_cb(GtkIMContext *context, gchar *str, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	J__InputConnection__commitText(env, connection, _JSTRING(str), 1);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

/* GTK's preedit is Android's composing text: setComposingText() replaces it, the commit that follows replaces it with
 * the final text (an empty preedit at the end removes it) */
static void preedit_changed_cb(GtkIMContext *context, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	gchar *str;
	gtk_im_context_get_preedit_string(context, &str, NULL, NULL);
	J__InputConnection__setComposingText(env, connection, _JSTRING(str), 1);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	g_free(str);
}

static void retrieve_surrounding_cb(GtkIMContext *context, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	jobject before = J__InputConnection__getTextBeforeCursor(env, connection, 100, 0);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	jobject after = J__InputConnection__getTextAfterCursor(env, connection, 100, 0);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	jstring before_str = before ? J__CharSequence__toString(env, before) : NULL;
	jstring after_str = after ? J__CharSequence__toString(env, after) : NULL;
	const char *before_cstr = before_str ? (*env)->GetStringUTFChars(env, before_str, NULL) : "";
	const char *after_cstr = after_str ? (*env)->GetStringUTFChars(env, after_str, NULL) : "";
	char *text = g_strconcat(before_cstr, after_cstr, NULL);
	int cursor_index = strlen(before_cstr);
	gtk_im_context_set_surrounding_with_selection(context, text, -1, cursor_index, cursor_index);
	g_free(text);
	if (before_str)
		(*env)->ReleaseStringUTFChars(env, before_str, before_cstr);
	if (after_str)
		(*env)->ReleaseStringUTFChars(env, after_str, after_cstr);
}

static void delete_surrounding_cb(GtkIMContext *context, gint offset, gint n_chars, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	J__InputConnection__deleteSurroundingText(env, connection, -offset, offset + n_chars);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

/* The on-screen keyboard of a phone shell: Phosh's keyboards (squeekboard, stevia) tell on the session bus
 * whether they are shown (sm.puri.OSK0, property Visible). While it is shown, the compositor makes the windows
 * smaller; Android keeps the configuration and reports the IME as visible (WindowInsets). */
static void osk_visible_changed(GDBusProxy *proxy)
{
	GVariant *v = g_dbus_proxy_get_cached_property(proxy, "Visible");
	gboolean visible = v && g_variant_is_of_type(v, G_VARIANT_TYPE_BOOLEAN) && g_variant_get_boolean(v);
	if (v)
		g_variant_unref(v);
	JNIEnv *env = get_jni_env();
	J__InputMethodManager__oskVisibleChanged(env, visible);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

static void osk_properties_changed_cb(GDBusProxy *proxy, GVariant *changed, GStrv invalidated, gpointer user_data)
{
	osk_visible_changed(proxy);
}

static void osk_proxy_ready_cb(GObject *source, GAsyncResult *res, gpointer user_data)
{
	GDBusProxy *proxy = g_dbus_proxy_new_for_bus_finish(res, NULL);
	if (!proxy)
		return;
	g_signal_connect(proxy, "g-properties-changed", G_CALLBACK(osk_properties_changed_cb), NULL);
	osk_visible_changed(proxy);
}

JNIEXPORT jlong JNICALL Java_android_view_inputmethod_InputMethodManager_nativeInit(JNIEnv *env, jclass class)
{
	g_dbus_proxy_new_for_bus(G_BUS_TYPE_SESSION, G_DBUS_PROXY_FLAGS_DO_NOT_AUTO_START, NULL, "sm.puri.OSK0", "/sm/puri/OSK0", "sm.puri.OSK0",
	                         NULL, osk_proxy_ready_cb, NULL);
	GtkIMContext *context = gtk_im_multicontext_new();
	g_signal_connect(context, "commit", G_CALLBACK(commit_cb), NULL);
	g_signal_connect(context, "preedit-changed", G_CALLBACK(preedit_changed_cb), NULL);
	g_signal_connect(context, "retrieve-surrounding", G_CALLBACK(retrieve_surrounding_cb), NULL);
	g_signal_connect(context, "delete-surrounding", G_CALLBACK(delete_surrounding_cb), NULL);
	return _INTPTR(context);
}

static gboolean activate_osk(gpointer user_data)
{
	GtkIMContext *context = GTK_IM_CONTEXT(user_data);
	gtk_im_context_focus_in(context);
	gtk_im_context_activate_osk(context, NULL);
	return G_SOURCE_REMOVE;
}

#define TYPE_MASK_CLASS                       0x0000000f
#define TYPE_MASK_VARIATION                   0x00000ff0
#define TYPE_MASK_FLAGS                       0x00fff000

#define TYPE_CLASS_TEXT                       0x00000001
#define TYPE_CLASS_NUMBER                     0x00000002
#define TYPE_CLASS_PHONE                      0x00000003

#define TYPE_NUMBER_VARIATION_PASSWORD        0x00000010

#define TYPE_TEXT_VARIATION_URI               0x00000010
#define TYPE_TEXT_VARIATION_EMAIL_ADDRESS     0x00000020
#define TYPE_TEXT_VARIATION_PERSON_NAME       0x00000060
#define TYPE_TEXT_VARIATION_PASSWORD          0x00000080
#define TYPE_TEXT_VARIATION_VISIBLE_PASSWORD  0x00000090
#define TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS 0x000000d0
#define TYPE_TEXT_VARIATION_WEB_PASSWORD      0x000000e0

#define TYPE_TEXT_FLAG_AUTO_CORRECT           0x00008000
#define TYPE_TEXT_FLAG_AUTO_COMPLETE          0x00010000

static void set_input_type(GtkIMContext *context, jint input_type)
{
	GtkInputPurpose purpose = GTK_INPUT_PURPOSE_FREE_FORM;
	GtkInputHints hints = GTK_INPUT_HINT_NONE;
	switch (input_type & TYPE_MASK_CLASS) {
		case TYPE_CLASS_TEXT:
			switch (input_type & TYPE_MASK_VARIATION) {
				case TYPE_TEXT_VARIATION_URI:
					purpose = GTK_INPUT_PURPOSE_URL;
					break;
				case TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
				case TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
					purpose = GTK_INPUT_PURPOSE_EMAIL;
					break;
				case TYPE_TEXT_VARIATION_PERSON_NAME:
					purpose = GTK_INPUT_PURPOSE_NAME;
					break;
				case TYPE_TEXT_VARIATION_PASSWORD:
				case TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
				case TYPE_TEXT_VARIATION_WEB_PASSWORD:
					purpose = GTK_INPUT_PURPOSE_PASSWORD;
					break;
				default:
					purpose = GTK_INPUT_PURPOSE_FREE_FORM;
					break;
			}
			switch (input_type & TYPE_MASK_FLAGS) {
				case TYPE_TEXT_FLAG_AUTO_CORRECT:
					hints |= GTK_INPUT_HINT_SPELLCHECK;
					break;
				case TYPE_TEXT_FLAG_AUTO_COMPLETE:
					hints |= GTK_INPUT_HINT_WORD_COMPLETION;
					break;
			}
			break;
		case TYPE_CLASS_NUMBER:
			switch (input_type & TYPE_MASK_VARIATION) {
				case TYPE_NUMBER_VARIATION_PASSWORD:
					purpose = GTK_INPUT_PURPOSE_PASSWORD;
					break;
				default:
					purpose = GTK_INPUT_PURPOSE_NUMBER;
					break;
			}
			break;
		case TYPE_CLASS_PHONE:
			purpose = GTK_INPUT_PURPOSE_PHONE;
			break;
		default:
			purpose = GTK_INPUT_PURPOSE_FREE_FORM;
			break;
	}
	g_object_set(G_OBJECT(context), "input-purpose", purpose, "input-hints", hints, NULL);
}

JNIEXPORT jboolean JNICALL Java_android_view_inputmethod_InputMethodManager_nativeShowSoftInput(JNIEnv *env, jobject this, jlong context_ptr, jlong widget_ptr, jobject new_connection, jint input_type)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	GtkIMContext *context = GTK_IM_CONTEXT(_PTR(context_ptr));
	gtk_im_context_set_client_widget(context, widget);
	if (new_connection) {
		set_input_type(context, input_type);
		if (connection) {
			/* a new connection (restartInput) starts without the old one's preedit */
			gtk_im_context_reset(context);
			_UNREF(connection);
		} else // GtkIMContext needs a few Wayland cycles to become functional after setting the initial client widget, so schedule a retry after 10ms.
			g_timeout_add(10, activate_osk, context);
		connection = _REF(new_connection);
	}
	gtk_im_context_focus_in(context);
	return gtk_im_context_activate_osk(context, NULL);
}

JNIEXPORT void JNICALL Java_android_view_inputmethod_InputMethodManager_nativeHideSoftInput(JNIEnv *env, jobject this, jlong context_ptr)
{
	GtkIMContext *context = GTK_IM_CONTEXT(_PTR(context_ptr));
	gtk_im_context_focus_out(context);
}
