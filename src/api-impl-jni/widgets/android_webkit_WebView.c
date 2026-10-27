#include <math.h>
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

/* A local reference to the Java WebView, or NULL if it has been garbage collected: the WrapperWidget only
 * holds a weak reference, and WebKit can still deliver events for the page until the wrapper is released
 * on the main thread after View.finalize(). */
static jobject webview_jobject(JNIEnv *env, WrapperWidget *wrapper)
{
	return (*env)->NewLocalRef(env, wrapper->jobj);
}

static void asset_uri_scheme_request_cb(WebKitURISchemeRequest *request, gpointer user_data)
{
	const gchar *path = webkit_uri_scheme_request_get_path(request);
	if (path == NULL)
		return;
	/* android-asset:/// stands in for file:/// (see WebView.loadUrl()). Only file:///android_asset/
	 * is backed by the APK. Anything else, e.g. an absolute path resolved against an asset base URL,
	 * is a local file, as on Android. */
	if (!g_str_has_prefix(path, "/assets/")) {
		g_autofree gchar *file_uri = g_strconcat("file://", path, NULL);
		g_autoptr(GFile) file = g_file_new_for_uri(file_uri);
		g_autoptr(GError) error = NULL;
		g_autoptr(GFileInputStream) stream = g_file_read(file, NULL, &error);
		g_autoptr(GFileInfo) info = stream ? g_file_input_stream_query_info(stream, G_FILE_ATTRIBUTE_STANDARD_SIZE, NULL, &error) : NULL;
		if (info)
			webkit_uri_scheme_request_finish(request, G_INPUT_STREAM(stream), g_file_info_get_size(info), NULL);
		else
			webkit_uri_scheme_request_finish_error(request, error);
		return;
	}
	path += 1; // remove the leading '/'
	GtkWidget *parent = gtk_widget_get_parent(GTK_WIDGET(webkit_uri_scheme_request_get_web_view(request)));
	if (!WRAPPER_IS_WIDGET(parent)) { // the WebView was destroyed
		GError *error = g_error_new(G_IO_ERROR, G_IO_ERROR_CANCELLED, "WebView destroyed");
		webkit_uri_scheme_request_finish_error(request, error);
		g_error_free(error);
		return;
	}
	JNIEnv *env = get_jni_env();
	jobject this = webview_jobject(env, WRAPPER_WIDGET(parent));
	if (!this) {
		GError *error = g_error_new(G_IO_ERROR, G_IO_ERROR_CANCELLED, "WebView garbage collected");
		webkit_uri_scheme_request_finish_error(request, error);
		g_error_free(error);
		return;
	}
	jobject asset_manager_obj = J__WebView__internalGetAssetManager(env, this);
	(*env)->DeleteLocalRef(env, this);
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(asset_manager_obj, "mObject"));
	struct Asset *asset = AssetManager_openNonAsset(asset_manager, path, ACCESS_STREAMING);
	if (asset == NULL) {
		// like the Android WebView: a missing asset is a failed load, not a crash
		android_log_printf(ANDROID_LOG_WARN, "WebView", "asset not found: %s\n", path);
		GError *error = g_error_new(G_IO_ERROR, G_IO_ERROR_NOT_FOUND, "asset not found: %s", path);
		webkit_uri_scheme_request_finish_error(request, error);
		g_error_free(error);
		return;
	}
	GInputStream *stream = asset_input_stream_new(asset);
	webkit_uri_scheme_request_finish(request, stream, Asset_getLength(asset), NULL);
	g_object_unref(stream);
}

static void web_view_load_changed(WebKitWebView *web_view, WebKitLoadEvent load_event, gpointer user_data)
{
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(web_view)));
	JNIEnv *env = get_jni_env();
	jobject this = webview_jobject(env, wrapper);
	if (!this)
		return;
	J__WebView__internalLoadChanged(env, this, load_event, _JSTRING(webkit_web_view_get_uri(web_view)));
	(*env)->DeleteLocalRef(env, this);
}

/* same-document navigations (e.g. only the fragment changed) don't emit load-changed, but Android still calls onPageFinished */
static void web_view_uri_changed(WebKitWebView *web_view, GParamSpec *pspec, gpointer user_data)
{
	if (!webkit_web_view_is_loading(web_view))
		web_view_load_changed(web_view, WEBKIT_LOAD_FINISHED, user_data);
}

/* Android's WebView has no context menu of its own: a right click only reaches the page (contextmenu
 * event). Keep WebKit's menu where Android shows its text actions instead (editable text, a selection). */
static gboolean context_menu_cb(WebKitWebView *web_view, WebKitContextMenu *context_menu, WebKitHitTestResult *hit_test_result, gpointer user_data)
{
	return !webkit_hit_test_result_context_is_editable(hit_test_result) && !webkit_hit_test_result_context_is_selection(hit_test_result);
}

static gboolean decide_policy(WebKitWebView *web_view, WebKitPolicyDecision *decision, WebKitPolicyDecisionType type, WrapperWidget *wrapper)
{
	if (type == WEBKIT_POLICY_DECISION_TYPE_NAVIGATION_ACTION) {
		WebKitNavigationPolicyDecision *nav = WEBKIT_NAVIGATION_POLICY_DECISION(decision);
		WebKitNavigationAction *action = webkit_navigation_policy_decision_get_navigation_action(nav);
		WebKitURIRequest *request = webkit_navigation_action_get_request(action);
		const gchar *uri = webkit_uri_request_get_uri(request);

		JNIEnv *env = get_jni_env();
		jobject this = webview_jobject(env, wrapper);
		// a page without its WebView doesn't navigate anywhere
		jboolean result = this ? J__WebView__internalShouldOverrideUrlLoading(env, this, _JSTRING(uri)) : JNI_TRUE;
		if (this)
			(*env)->DeleteLocalRef(env, this);
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
	JNIEnv *env = get_jni_env();
	jobject this = webview_jobject(env, wrapper);
	if (!this)
		return;
	char *message = jsc_value_to_string(value);
	J__WebView__internalJsMessage(env, this, _JSTRING(message));
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, this);
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
		// the WebView was destroyed meanwhile: like on Android, the callback is dropped
		jobject this = webview_jobject(env, data->wrapper);
		if (this && gtk_widget_get_parent(GTK_WIDGET(source)) == GTK_WIDGET(data->wrapper)) {
			J__WebView__internalJsResult(env, this, data->callback, _JSTRING(json ? json : "null"));
			if ((*env)->ExceptionCheck(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
			}
		}
		if (this)
			(*env)->DeleteLocalRef(env, this);
		(*env)->DeleteGlobalRef(env, data->callback);
	}
	g_free(json);
	g_object_unref(data->wrapper);
	g_free(data);
}

/* A WebView that is destroyed or garbage collected gives up its page, but WebKit does not close it: WebKitGTK
 * 2.54 aborts the web process when it closes a page whose compositing thread stops before it has dropped its
 * buffer release timer (AcceleratedSurface::willDestroyCompositingRunLoop(), fixed in WebKit main), and with
 * it every page of the app goes blank. So the WebKitWebView stays alive, here, and its page is emptied: the
 * document, its style rules and scripts go, which is what made every page an app has shown cost time and
 * memory. Nothing calls back into the View any more, and a new WebView takes it over. */
static void park_web_view(WebKitWebView *webview, gpointer wrapper)
{
	if (g_object_get_data(G_OBJECT(webview), "atl-parked"))
		return;
	g_object_set_data(G_OBJECT(webview), "atl-parked", GINT_TO_POINTER(1));
	webkit_web_view_stop_loading(webview);
	WebKitUserContentManager *content_manager = webkit_web_view_get_user_content_manager(webview);
	g_signal_handlers_disconnect_by_data(content_manager, wrapper);
	webkit_user_content_manager_unregister_script_message_handler(content_manager, "atlbridge", NULL);
	webkit_user_content_manager_remove_all_scripts(content_manager);
	g_signal_handlers_disconnect_by_func(webview, web_view_load_changed, NULL);
	g_signal_handlers_disconnect_by_func(webview, context_menu_cb, NULL);
	g_signal_handlers_disconnect_by_data(webview, wrapper);
	// own settings without the page cache, which would keep the old document
	WebKitSettings *settings = webkit_settings_new();
	webkit_settings_set_enable_page_cache(settings, FALSE);
	webkit_web_view_set_settings(webview, settings);
	g_object_unref(settings);
	webkit_web_view_load_uri(webview, "about:blank");
}

/* Parked WebKitWebViews, for the next WebViews of the app: a new WebKitWebView makes the web process create a
 * page, and that rebuilds the style of every page the process shows (WebPage::WebPage() sets the accent color,
 * see web_view_css_changed()), which got slower with each page the app keeps. */
static GQueue parked_web_views = G_QUEUE_INIT;

// the wrapper goes when the WebView is garbage collected, and has let go of the WebKitWebView by now
static void wrapper_finalized(gpointer webview, GObject *wrapper)
{
	park_web_view(webview, wrapper);
	g_queue_push_tail(&parked_web_views, webview);
}

// a parked WebKitWebView that is done loading about:blank, or NULL
static WebKitWebView *take_parked_web_view(void)
{
	for (GList *l = parked_web_views.head; l; l = l->next) {
		WebKitWebView *webview = l->data;
		if (!webkit_web_view_is_loading(webview) && !gtk_widget_get_parent(GTK_WIDGET(webview))) {
			g_queue_delete_link(&parked_web_views, l);
			g_object_set_data(G_OBJECT(webview), "atl-parked", NULL);
			return webview;
		}
	}
	return NULL;
}

/* the theme's accent color, looked up like WebKitGTK does (PageClientImpl::accentColor()) */
static GdkRGBA web_view_accent_color(GtkWidget *webview)
{
	static const char *const names[] = {"accent_bg_color", "accent_color", "accentColor", "theme_selected_bg_color"};
	GdkRGBA color = {52 / 255.f, 132 / 255.f, 228 / 255.f, 1.f};
	G_GNUC_BEGIN_IGNORE_DEPRECATIONS
	GtkStyleContext *context = gtk_widget_get_style_context(webview);
	for (int i = 0; i < G_N_ELEMENTS(names); i++)
		if (gtk_style_context_lookup_color(context, names[i], &color))
			break;
	G_GNUC_END_IGNORE_DEPRECATIONS
	return color;
}

static void (*webkit_css_changed)(GtkWidget *widget, GtkCssStyleChange *change);

/* WebKitGTK hands the accent color to the web process whenever the CSS of a WebKitWebView changes, e.g. when the
 * view is added to or removed from a window, and the web process then rebuilds the style of every page it shows
 * (RenderThemeAdwaita::setAccentColor() does not check whether the color changed). With the WebViews an app keeps,
 * that made each new page cost more. Only pass real changes on. */
static void web_view_css_changed(GtkWidget *webview, GtkCssStyleChange *change)
{
	GdkRGBA color = web_view_accent_color(webview);
	GdkRGBA *sent = g_object_get_data(G_OBJECT(webview), "atl-accent-color");
	if (sent && gdk_rgba_equal(sent, &color)) {
		GTK_WIDGET_CLASS(g_type_class_peek(GTK_TYPE_WIDGET))->css_changed(webview, change);
		return;
	}
	g_object_set_data_full(G_OBJECT(webview), "atl-accent-color", gdk_rgba_copy(&color), (GDestroyNotify)gdk_rgba_free);
	webkit_css_changed(webview, change);
}

/* An Android WebView scales its content by the display density alone: a CSS px is a dp. ATL's dp is GTK's
 * logical pixel, which GTK already draws at the monitor scale. WebKitGTK additionally zooms every page by
 * the font resolution, gtk-xft-dpi / 96 (refreshInternalScaling() in WebKitWebViewBase.cpp, which leaves
 * out changes of up to 2 %), so a desktop text scaling such as 144 dpi with a 150 % monitor would scale
 * web content twice. Undo that zoom; this follows WebKit when gtk-xft-dpi changes, whichever handler runs
 * first. Without gtk-xft-dpi WebKit takes the monitor's physical resolution, which GTK does not tell. */
static void undo_font_dpi_zoom(GtkSettings *settings, GParamSpec *pspec, WebKitWebView *webview)
{
	int xft_dpi;
	g_object_get(settings, "gtk-xft-dpi", &xft_dpi, NULL);
	double page_scale = xft_dpi > 0 ? xft_dpi / (96 * 1024.0) : 1;
	webkit_web_view_set_zoom_level(webview, fabs(page_scale - 1) > 0.02 ? 1 / page_scale : 1);
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

	if (!webkit_css_changed) {
		GtkWidgetClass *web_view_class = g_type_class_ref(WEBKIT_TYPE_WEB_VIEW);
		webkit_css_changed = web_view_class->css_changed;
		web_view_class->css_changed = web_view_css_changed;
	}

	GtkWidget *wrapper = g_object_ref_sink(wrapper_widget_new());
	/* Like Android, which renders the WebViews of an app in one renderer process, the WebViews share
	 * one web process (as related views); WebKitGTK would start one per view (~150 MB each), and those
	 * of an activity that the app leaks would stay. Each view keeps its own user content manager: the
	 * atlbridge handler below belongs to the view. */
	static GWeakRef related_view;
	GtkWidget *webview = GTK_WIDGET(take_parked_web_view());
	if (webview) {
		// what a new WebKitWebView has: default settings and background
		WebKitSettings *settings = webkit_settings_new();
		webkit_web_view_set_settings(WEBKIT_WEB_VIEW(webview), settings);
		g_object_unref(settings);
		webkit_web_view_set_background_color(WEBKIT_WEB_VIEW(webview), &(GdkRGBA){1.f, 1.f, 1.f, 1.f});
	} else {
		WebKitWebView *related = g_weak_ref_get(&related_view);
		WebKitUserContentManager *own_content_manager = webkit_user_content_manager_new();
		webview = g_object_new(WEBKIT_TYPE_WEB_VIEW, "related-view", related, "user-content-manager", own_content_manager, NULL);
		g_object_unref(own_content_manager);
		if (related)
			g_object_unref(related);
		g_weak_ref_set(&related_view, webview);
		// the page gets this color when it is created, see web_view_css_changed()
		GdkRGBA accent_color = web_view_accent_color(webview);
		g_object_set_data_full(G_OBJECT(webview), "atl-accent-color", gdk_rgba_copy(&accent_color), (GDestroyNotify)gdk_rgba_free);
		// never finalized, see park_web_view()
		g_object_ref(webview);
	}
	g_object_weak_ref(G_OBJECT(wrapper), wrapper_finalized, webview);
	/* Android's WebView applies a mouse wheel step at once (one frame per notch, the size already
	 * accelerated by the InputReader). WebKit animates each step instead, with an ease-out curve that
	 * starts over at every further step, so a spinning wheel scrolls in lurches. */
	webkit_settings_set_enable_smooth_scrolling(webkit_web_view_get_settings(WEBKIT_WEB_VIEW(webview)), FALSE);
	undo_font_dpi_zoom(gtk_settings_get_default(), NULL, WEBKIT_WEB_VIEW(webview));
	g_signal_connect_object(gtk_settings_get_default(), "notify::gtk-xft-dpi", G_CALLBACK(undo_font_dpi_zoom), webview, 0);
	wrapper_widget_set_child(WRAPPER_WIDGET(wrapper), webview);
	wrapper_widget_set_jobject(WRAPPER_WIDGET(wrapper), env, this);
	wrapper_widget_register_invalidation_listener(WRAPPER_WIDGET(wrapper));
	// all WebViews share the default web context, and registering a scheme twice is an error
	static gboolean asset_scheme_registered = FALSE;
	if (!asset_scheme_registered) {
		webkit_web_context_register_uri_scheme(webkit_web_view_get_context(WEBKIT_WEB_VIEW(webview)), "android-asset", asset_uri_scheme_request_cb, NULL, NULL);
		webkit_security_manager_register_uri_scheme_as_local(webkit_web_context_get_security_manager(webkit_web_view_get_context(WEBKIT_WEB_VIEW(webview))), "android-asset");
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
	/* see native_firePointerCancel(): fire the pointercancel that Android's WebView fires on ACTION_CANCEL. Mouse input
	 * keeps its pointerType "mouse", as on Android with a mouse: the MotionEvents are mouse events too. */
	WebKitUserScript *pointer_script = webkit_user_script_new(
	    "(function(){var down=null;addEventListener('pointerdown',function(e){down=e;},true);"
	    "addEventListener('pointerup',function(){down=null;},true);addEventListener('pointercancel',function(){down=null;},true);"
	    "Object.defineProperty(window,'__atl_pointercancel',{value:function(){var e=down;down=null;if(e)"
	    "e.target.dispatchEvent(new PointerEvent('pointercancel',{bubbles:true,pointerId:e.pointerId,pointerType:e.pointerType,"
	    "isPrimary:e.isPrimary,clientX:e.clientX,clientY:e.clientY,screenX:e.screenX,screenY:e.screenY}));}});})();",
	    WEBKIT_USER_CONTENT_INJECT_ALL_FRAMES, WEBKIT_USER_SCRIPT_INJECT_AT_DOCUMENT_START, NULL, NULL);
	webkit_user_content_manager_add_script(content_manager, pointer_script);
	webkit_user_script_unref(pointer_script);
	g_signal_connect(G_OBJECT(webview), "load-changed", G_CALLBACK(web_view_load_changed), NULL);
	g_signal_connect(G_OBJECT(webview), "notify::uri", G_CALLBACK(web_view_uri_changed), NULL);
	g_signal_connect(webview, "decide-policy", G_CALLBACK(decide_policy), wrapper);
	g_signal_connect(webview, "context-menu", G_CALLBACK(context_menu_cb), NULL);
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
	data->wrapper = g_object_ref(WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(webview))));
	data->callback = callback ? (*env)->NewGlobalRef(env, callback) : NULL;
	webkit_web_view_evaluate_javascript(webview, _CSTRING(script), -1, NULL, NULL, NULL, evaluate_js_cb, data);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1setBackgroundColor(JNIEnv *env, jobject this, jlong widget_ptr, jint color)
{
	WebKitWebView *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview)) // destroyed, or WebViews are disabled
		return;
	GdkRGBA rgba = {
		.red = ((color >> 16) & 0xFF) / 255.f,
		.green = ((color >> 8) & 0xFF) / 255.f,
		.blue = (color & 0xFF) / 255.f,
		.alpha = ((color >> 24) & 0xFF) / 255.f,
	};
	webkit_web_view_set_background_color(webview, &rgba);
}

/* WebView.onTouchEvent() got ACTION_CANCEL: an ancestor ViewGroup intercepted the touch sequence. Android's
 * WebView passes the cancel on to the page (pointercancel). WebKitGTK only knows GDK events and gets none for
 * the rest of the sequence, so gesture code in the page would wait for a pointerup that never comes. */
JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1firePointerCancel(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview)) // destroyed, or WebViews are disabled
		return;

	webkit_web_view_evaluate_javascript(WEBKIT_WEB_VIEW(webview), "window.__atl_pointercancel&&__atl_pointercancel()", -1, NULL, NULL, NULL, NULL, NULL);
}

/* WebView.destroy(): release the page in the (shared) web process (see park_web_view()). Until then every page an
 * app has shown stays loaded and keeps working (styles, scripts, timers). The Java object stays a View that the
 * app may still remove from its parent, so an empty widget takes the WebKitWebView's place in the wrapper; the
 * WebView methods ignore further calls, like on Android. */
JNIEXPORT jlong JNICALL Java_android_webkit_WebView_native_1destroy(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview)) // WebViews are disabled, this is a plain View
		return widget_ptr;

	GtkWidget *wrapper = gtk_widget_get_parent(webview);
	park_web_view(WEBKIT_WEB_VIEW(webview), wrapper);
	gtk_widget_unparent(webview);
	g_object_weak_unref(G_OBJECT(wrapper), wrapper_finalized, webview);
	g_queue_push_tail(&parked_web_views, webview);

	GtkWidget *placeholder = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
	wrapper_widget_set_child(WRAPPER_WIDGET(wrapper), placeholder);
	return _INTPTR(placeholder);
}
