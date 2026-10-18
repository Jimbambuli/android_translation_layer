#include <gio/gunixinputstream.h>
#include <gtk/gtk.h>
#include <webkit/webkit.h>

#include <androidfw/androidfw_c_api.h>

#include "../defines.h"
#include "../util.h"

#include "../AssetInputStream.h"
#include "WrapperWidget.h"

#include "../generated_headers/android_view_View.h"
#include "../generated_headers/android_webkit_CookieManager.h"
#include "../generated_headers/android_webkit_WebView.h"

extern GThread *main_thread_id;

static void asset_uri_scheme_request_cb(WebKitURISchemeRequest *request, gpointer user_data)
{
	const gchar *path = webkit_uri_scheme_request_get_path(request);
	if (path == NULL)
		return;
	path += 1; // remove the leading '/'
	JNIEnv *env = get_jni_env();
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(webkit_uri_scheme_request_get_web_view(request))));
	jobject asset_manager_obj = J__WebView__internalGetAssetManager(env, wrapper->jobj);
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(asset_manager_obj, "mObject"));
	struct Asset *asset = AssetManager_openNonAsset(asset_manager, path, ACCESS_STREAMING);
	GInputStream *stream = asset_input_stream_new(asset);
	webkit_uri_scheme_request_finish(request, stream, Asset_getLength(asset), NULL);
	g_object_unref(stream);
}

static void web_view_load_changed(WebKitWebView *web_view, WebKitLoadEvent load_event, gpointer user_data)
{
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(web_view)));
	JNIEnv *env = get_jni_env();
	J__WebView__internalLoadChanged(env, wrapper->jobj, load_event, _JSTRING(webkit_web_view_get_uri(web_view)));
}

/* same-document navigations (e.g. only the fragment changed) don't emit load-changed, but Android still calls onPageFinished */
static void web_view_uri_changed(WebKitWebView *web_view, GParamSpec *pspec, gpointer user_data)
{
	if (!webkit_web_view_is_loading(web_view))
		web_view_load_changed(web_view, WEBKIT_LOAD_FINISHED, user_data);
}

static gboolean decide_policy(WebKitWebView *web_view, WebKitPolicyDecision *decision, WebKitPolicyDecisionType type, WrapperWidget *wrapper)
{
	if (type == WEBKIT_POLICY_DECISION_TYPE_NAVIGATION_ACTION) {
		WebKitNavigationPolicyDecision *nav = WEBKIT_NAVIGATION_POLICY_DECISION(decision);
		WebKitNavigationAction *action = webkit_navigation_policy_decision_get_navigation_action(nav);
		WebKitURIRequest *request = webkit_navigation_action_get_request(action);
		const gchar *uri = webkit_uri_request_get_uri(request);

		JNIEnv *env = get_jni_env();
		jboolean result = J__WebView__internalShouldOverrideUrlLoading(env, wrapper->jobj, _JSTRING(uri));
		if (result)
			webkit_policy_decision_ignore(decision);
		else
			webkit_policy_decision_use(decision);
		return result;
	}
	return FALSE;
}

/* messages from the JavaScript side of addJavascriptInterface() and from the console forwarding script */
static void script_message_cb(WebKitUserContentManager *manager, JSCValue *value, WrapperWidget *wrapper)
{
	char *message = jsc_value_to_string(value);
	JNIEnv *env = get_jni_env();
	J__WebView__internalJsMessage(env, wrapper->jobj, _JSTRING(message));
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionDescribe(env);
	g_free(message);
}

struct evaluate_js_data {
	WrapperWidget *wrapper;
	jobject callback;
};

static void evaluate_js_cb(GObject *source, GAsyncResult *result, gpointer user_data)
{
	struct evaluate_js_data *data = user_data;
	GError *error = NULL;
	char *json = NULL;
	JSCValue *value = webkit_web_view_evaluate_javascript_finish(WEBKIT_WEB_VIEW(source), result, &error);
	if (error) {
		fprintf(stderr, "WebView.evaluateJavascript(): %s\n", error->message);
		g_error_free(error);
	} else if (value) {
		json = jsc_value_to_json(value, 0);
		g_object_unref(value);
	}
	if (data->callback) {
		JNIEnv *env = get_jni_env();
		J__WebView__internalJsResult(env, data->wrapper->jobj, data->callback, _JSTRING(json ? json : "null"));
		if ((*env)->ExceptionCheck(env))
			(*env)->ExceptionDescribe(env);
		(*env)->DeleteGlobalRef(env, data->callback);
	}
	g_free(json);
	g_free(data);
}

JNIEXPORT jlong JNICALL Java_android_webkit_WebView_native_1constructor(JNIEnv *env, jobject this, jobject context, jobject attrs)
{
	/*
	 * many apps use webview just for fingerprinting or displaying ads, which seems like
	 * a waste of resources even if we deal with fingerprinting and ads in some other way
	 * in the future.
	 */
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return Java_android_view_View_native_1constructor(env, this, context, attrs);

	GtkWidget *wrapper = g_object_ref(wrapper_widget_new());
	GtkWidget *webview = webkit_web_view_new();
	wrapper_widget_set_child(WRAPPER_WIDGET(wrapper), webview);
	wrapper_widget_set_jobject(WRAPPER_WIDGET(wrapper), env, this);
	wrapper_widget_register_invalidation_listener(WRAPPER_WIDGET(wrapper));
	// all WebViews share the default web context, and registering a scheme twice is an error
	static gboolean asset_scheme_registered = FALSE;
	if (!asset_scheme_registered) {
		webkit_web_context_register_uri_scheme(webkit_web_view_get_context(WEBKIT_WEB_VIEW(webview)), "android-asset", asset_uri_scheme_request_cb, NULL, NULL);
		asset_scheme_registered = TRUE;
	}
	WebKitUserContentManager *content_manager = webkit_web_view_get_user_content_manager(WEBKIT_WEB_VIEW(webview));
	webkit_user_content_manager_register_script_message_handler(content_manager, "atlbridge", NULL);
	g_signal_connect(content_manager, "script-message-received::atlbridge", G_CALLBACK(script_message_cb), wrapper);
	/* forward console messages to WebChromeClient.onConsoleMessage() */
	WebKitUserScript *console_script = webkit_user_script_new(
	    "(function(){['log','info','warn','error','debug'].forEach(function(level){var orig=console[level];"
	    "console[level]=function(){var msg=Array.prototype.map.call(arguments,String).join(' ');"
	    "try{window.webkit.messageHandlers.atlbridge.postMessage('\u0001console\u0001'+level+'\u0001'+msg);}catch(e){}"
	    "if(orig)orig.apply(console,arguments);};});})();",
	    WEBKIT_USER_CONTENT_INJECT_ALL_FRAMES, WEBKIT_USER_SCRIPT_INJECT_AT_DOCUMENT_START, NULL, NULL);
	webkit_user_content_manager_add_script(content_manager, console_script);
	webkit_user_script_unref(console_script);
	/* ATL presents pointer input to apps as a touchscreen (MotionEvents use SOURCE_TOUCHSCREEN), but WebKit
	 * reports mouse input as pointerType "mouse" to web content. Web apps written for Android WebView then
	 * take their desktop code paths, which don't cooperate with the surrounding Android views (e.g. no
	 * long press, or a horizontal text selection drag is taken over by a ViewPager). Report "touch". */
	WebKitUserScript *pointer_script = webkit_user_script_new(
	    "(function(){var d=window.PointerEvent&&Object.getOwnPropertyDescriptor(PointerEvent.prototype,'pointerType');"
	    "if(!d||!d.get)return;Object.defineProperty(PointerEvent.prototype,'pointerType',{configurable:true,enumerable:true,"
	    "get:function(){var t=d.get.call(this);return t==='mouse'?'touch':t;}});})();",
	    WEBKIT_USER_CONTENT_INJECT_ALL_FRAMES, WEBKIT_USER_SCRIPT_INJECT_AT_DOCUMENT_START, NULL, NULL);
	webkit_user_content_manager_add_script(content_manager, pointer_script);
	webkit_user_script_unref(pointer_script);
	g_signal_connect(G_OBJECT(webview), "load-changed", G_CALLBACK(web_view_load_changed), NULL);
	g_signal_connect(G_OBJECT(webview), "notify::uri", G_CALLBACK(web_view_uri_changed), NULL);
	g_signal_connect(webview, "decide-policy", G_CALLBACK(decide_policy), wrapper);
	return _INTPTR(webview);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1loadUrl(JNIEnv *env, jobject this, jlong widget_ptr, jstring url)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;

	WebKitWebView *webview = _PTR(widget_ptr);
	webkit_web_view_load_uri(webview, _CSTRING(url));
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1reload(JNIEnv *env, jobject this, jlong widget_ptr)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;
	WebKitWebView *webview = _PTR(widget_ptr);
	webkit_web_view_reload(webview);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1loadDataWithBaseURL(JNIEnv *env, jobject this, jlong widget_ptr, jstring base_url, jstring data_jstr, jstring mime_type, jstring encoding)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;

	WebKitWebView *webview = _PTR(widget_ptr);
	jsize data_len = (*env)->GetStringUTFLength(env, data_jstr);
	jsize data_jlen = (*env)->GetStringLength(env, data_jstr);
	char *data = malloc(data_len + 1); // + 1 for NUL
	(*env)->GetStringUTFRegion(env, data_jstr, 0, data_jlen, data);
	webkit_web_view_load_bytes(webview, g_bytes_new(data, data_len), mime_type ? _CSTRING(mime_type) : "text/html", encoding ? _CSTRING(encoding) : NULL, base_url ? _CSTRING(base_url) : NULL);
}

struct get_cookies_data {
	gboolean done;
	GList *cookies;
};

static void get_cookies_cb(GObject *cookie_manager, GAsyncResult *res, gpointer user_data)
{
	struct get_cookies_data *data = user_data;
	data->cookies = webkit_cookie_manager_get_cookies_finish(WEBKIT_COOKIE_MANAGER(cookie_manager), res, NULL);
	data->done = TRUE;
}

JNIEXPORT jstring JNICALL Java_android_webkit_CookieManager_native_1getCookie(JNIEnv *env, jobject this, jstring url)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return _JSTRING("");

	// Android's getCookie is synchronous, so we need to iterate the main loop until WebKit replies
	if (g_thread_self() != main_thread_id) {
		fprintf(stderr, "CookieManager.getCookie: not supported off the main thread\n");
		return _JSTRING("");
	}

	struct get_cookies_data data = {0};
	const char *uri = _CSTRING(url);
	WebKitCookieManager *cookie_manager = webkit_network_session_get_cookie_manager(webkit_network_session_get_default());
	webkit_cookie_manager_get_cookies(cookie_manager, uri, NULL, get_cookies_cb, &data);
	(*env)->ReleaseStringUTFChars(env, url, uri);
	while (!data.done)
		g_main_context_iteration(g_main_context_default(), TRUE);

	if (!data.cookies)
		return NULL;

	GString *cookie_string = g_string_new(NULL);
	for (GList *l = data.cookies; l; l = l->next) {
		SoupCookie *cookie = l->data;
		g_string_append_printf(cookie_string, "%s%s=%s", l == data.cookies ? "" : "; ", soup_cookie_get_name(cookie), soup_cookie_get_value(cookie));
	}
	g_list_free_full(data.cookies, (GDestroyNotify)soup_cookie_free);

	jstring ret = _JSTRING(cookie_string->str);
	g_string_free(cookie_string, TRUE);
	return ret;
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1addJavascriptInterface(JNIEnv *env, jobject this, jlong widget_ptr, jstring name, jstring method_names)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;

	WebKitWebView *webview = _PTR(widget_ptr);
	/* window.<name>.<method>(args...) posts "<name>\1<method>\1<type>:<value>..." to script_message_cb;
	 * the call is asynchronous, so methods can't return values to JavaScript */
	char *source = g_strdup_printf(
	    "(function(){var o={};'%s'.split(',').forEach(function(m){o[m]=function(){"
	    "var a=Array.prototype.map.call(arguments,function(x){return (typeof x)+':'+String(x);});"
	    "window.webkit.messageHandlers.atlbridge.postMessage(['%s',m].concat(a).join('\\u0001'));};});"
	    "window['%s']=o;})();",
	    _CSTRING(method_names), _CSTRING(name), _CSTRING(name));
	WebKitUserScript *script = webkit_user_script_new(source, WEBKIT_USER_CONTENT_INJECT_ALL_FRAMES, WEBKIT_USER_SCRIPT_INJECT_AT_DOCUMENT_START, NULL, NULL);
	webkit_user_content_manager_add_script(webkit_web_view_get_user_content_manager(webview), script);
	webkit_user_script_unref(script);
	g_free(source);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1evaluateJavascript(JNIEnv *env, jobject this, jlong widget_ptr, jstring script, jobject callback)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;

	WebKitWebView *webview = _PTR(widget_ptr);
	struct evaluate_js_data *data = g_new(struct evaluate_js_data, 1);
	data->wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(webview)));
	data->callback = callback ? (*env)->NewGlobalRef(env, callback) : NULL;
	webkit_web_view_evaluate_javascript(webview, _CSTRING(script), -1, NULL, NULL, NULL, evaluate_js_cb, data);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1setBackgroundColor(JNIEnv *env, jobject this, jlong widget_ptr, jint color)
{
	WebKitWebView *webview = _PTR(widget_ptr);
	GdkRGBA rgba = {
		.red = ((color >> 16) & 0xFF) / 255.f,
		.green = ((color >> 8) & 0xFF) / 255.f,
		.blue = (color & 0xFF) / 255.f,
		.alpha = ((color >> 24) & 0xFF) / 255.f,
	};
	webkit_web_view_set_background_color(webview, &rgba);
}
