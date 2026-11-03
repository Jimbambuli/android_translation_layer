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

static void read_texture_cb(GObject *source, GAsyncResult *result, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	GdkTexture *texture = gdk_clipboard_read_texture_finish(GDK_CLIPBOARD(source), result, NULL);
	if (gdk_clipboard_is_local(GDK_CLIPBOARD(source))) {
		g_clear_object(&texture);
		return;
	}
	if (!texture) {
		(*env)->CallStaticVoidMethod(env, clipboard_manager_class, _STATIC_METHOD(clipboard_manager_class, "onSystemClipboardText", "(Ljava/lang/String;)V"), NULL);
	} else {
		GBytes *png = gdk_texture_save_to_png_bytes(texture);
		jbyteArray array = (*env)->NewByteArray(env, g_bytes_get_size(png));
		(*env)->SetByteArrayRegion(env, array, 0, g_bytes_get_size(png), g_bytes_get_data(png, NULL));
		(*env)->CallStaticVoidMethod(env, clipboard_manager_class, _STATIC_METHOD(clipboard_manager_class, "onSystemClipboardImage", "([B)V"), array);
		(*env)->DeleteLocalRef(env, array);
		g_bytes_unref(png);
		g_object_unref(texture);
	}
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

/* another application owns the clipboard: read its image or else its text for ClipboardManager.getPrimaryClip(),
 * which is synchronous */
static void clipboard_changed_cb(GdkClipboard *clipboard, gpointer user_data)
{
	if (gdk_clipboard_is_local(clipboard))
		return;
	if (gdk_content_formats_contain_gtype(gdk_clipboard_get_formats(clipboard), GDK_TYPE_TEXTURE))
		gdk_clipboard_read_texture_async(clipboard, NULL, read_texture_cb, NULL);
	else
		gdk_clipboard_read_text_async(clipboard, NULL, read_text_cb, NULL);
}

JNIEXPORT void JNICALL Java_android_content_ClipboardManager_native_1init(JNIEnv *env, jclass class)
{
	GdkClipboard *clipboard = gdk_display_get_clipboard(gdk_display_get_default());
	clipboard_manager_class = _REF(class);
	g_signal_connect(clipboard, "changed", G_CALLBACK(clipboard_changed_cb), NULL);
	clipboard_changed_cb(clipboard, NULL);
}

/* the app's clip for the desktop: an image in its own type (the desktop converts it) and the text, as far as
 * there are */
JNIEXPORT void JNICALL Java_android_content_ClipboardManager_native_1set_1clipboard(JNIEnv *env, jclass class, jstring text_jstring, jstring mime_type_jstring, jbyteArray data)
{
	GdkClipboard *clipboard = gdk_display_get_clipboard(gtk_root_get_display(GTK_ROOT(window)));
	GdkContentProvider *providers[2];
	int n = 0;
	if (mime_type_jstring && data) {
		const char *mime_type = (*env)->GetStringUTFChars(env, mime_type_jstring, NULL);
		jbyte *bytes = (*env)->GetByteArrayElements(env, data, NULL);
		GBytes *gbytes = g_bytes_new(bytes, (*env)->GetArrayLength(env, data));
		(*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
		providers[n++] = gdk_content_provider_new_for_bytes(mime_type, gbytes);
		g_bytes_unref(gbytes);
		(*env)->ReleaseStringUTFChars(env, mime_type_jstring, mime_type);
	}
	if (text_jstring) {
		const char *text = (*env)->GetStringUTFChars(env, text_jstring, NULL);
		providers[n++] = gdk_content_provider_new_typed(G_TYPE_STRING, text);
		(*env)->ReleaseStringUTFChars(env, text_jstring, text);
	}
	if (n == 0) {
		gdk_clipboard_set_content(clipboard, NULL);
		return;
	}
	GdkContentProvider *content = n == 1 ? providers[0] : gdk_content_provider_new_union(providers, n);
	gdk_clipboard_set_content(clipboard, content);
	g_object_unref(content);
}
