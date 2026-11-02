#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_content_ClipboardManager.h"

extern GtkWindow *window; // TODO: get this in a better way

static jclass clipboard_manager_class;

static void read_text_cb(GObject *source, GAsyncResult *result, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	char *text = gdk_clipboard_read_text_finish(GDK_CLIPBOARD(source), result, NULL);
	/* unless the app has set its own clip in the meantime */
	if (!gdk_clipboard_is_local(GDK_CLIPBOARD(source)))
		(*env)->CallStaticVoidMethod(env, clipboard_manager_class, _STATIC_METHOD(clipboard_manager_class, "onSystemClipboardText", "(Ljava/lang/String;)V"), text ? _JSTRING(text) : NULL);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	g_free(text);
}

/* another application owns the clipboard: read its text for ClipboardManager.getPrimaryClip(), which is synchronous */
static void clipboard_changed_cb(GdkClipboard *clipboard, gpointer user_data)
{
	if (gdk_clipboard_is_local(clipboard))
		return;
	gdk_clipboard_read_text_async(clipboard, NULL, read_text_cb, NULL);
}

JNIEXPORT void JNICALL Java_android_content_ClipboardManager_native_1init(JNIEnv *env, jclass class)
{
	GdkClipboard *clipboard = gdk_display_get_clipboard(gdk_display_get_default());
	clipboard_manager_class = _REF(class);
	g_signal_connect(clipboard, "changed", G_CALLBACK(clipboard_changed_cb), NULL);
	clipboard_changed_cb(clipboard, NULL);
}

JNIEXPORT void JNICALL Java_android_content_ClipboardManager_native_1set_1clipboard(JNIEnv *env, jclass class, jstring text_jstring)
{
	GdkClipboard *clipboard = gdk_display_get_clipboard(gtk_root_get_display(GTK_ROOT(window)));
	if (!text_jstring) {
		gdk_clipboard_set_content(clipboard, NULL);
		return;
	}
	const char *text = (*env)->GetStringUTFChars(env, text_jstring, NULL);
	gdk_clipboard_set_text(clipboard, text);
	(*env)->ReleaseStringUTFChars(env, text_jstring, text);
}
