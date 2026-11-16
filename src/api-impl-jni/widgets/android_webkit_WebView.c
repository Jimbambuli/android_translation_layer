#include <math.h>
#include <string.h>
#include <gio/gunixinputstream.h>
#include <gio/gunixsocketaddress.h>
#include <glib/gstdio.h>
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

/* The Java bridge (addJavascriptInterface()): a call from the page is a synchronous XMLHttpRequest to this
 * scheme, which is how a page can wait for a native answer in WebKitGTK (script message handlers only
 * reply with a promise). The WebView calls the Java method on a background thread (Chromium's bridge does
 * as well) and hands the answer back through native_finishJavaBridgeCall(), so the main loop keeps running
 * while the page waits. */
#define JAVA_BRIDGE_SCHEME "atl-javabridge"

static void java_bridge_reply(WebKitURISchemeRequest *request, const char *answer)
{
	gsize length = strlen(answer);
	GInputStream *stream = g_memory_input_stream_new_from_data(g_strdup(answer), length, g_free);
	WebKitURISchemeResponse *response = webkit_uri_scheme_response_new(stream, length);
	SoupMessageHeaders *headers = soup_message_headers_new(SOUP_MESSAGE_HEADERS_RESPONSE);
	soup_message_headers_append(headers, "Access-Control-Allow-Origin", "*");
	webkit_uri_scheme_response_set_content_type(response, "text/plain; charset=utf-8");
	webkit_uri_scheme_response_set_http_headers(response, headers);
	webkit_uri_scheme_request_finish_with_response(request, response);
	g_object_unref(response);
	g_object_unref(stream);
}

static void java_bridge_request_cb(WebKitURISchemeRequest *request, gpointer user_data)
{
	WebKitWebView *web_view = webkit_uri_scheme_request_get_web_view(request);
	GtkWidget *parent = web_view ? gtk_widget_get_parent(GTK_WIDGET(web_view)) : NULL;
	JNIEnv *env = get_jni_env();
	jobject this = WRAPPER_IS_WIDGET(parent) ? webview_jobject(env, WRAPPER_WIDGET(parent)) : NULL;
	GInputStream *body = webkit_uri_scheme_request_get_http_body(request);
	if (!this || !body) {
		java_bridge_reply(request, "eJava object is gone");
		return;
	}
	GString *message = g_string_new(NULL);
	char buffer[4096];
	gssize count;
	while ((count = g_input_stream_read(body, buffer, sizeof(buffer), NULL, NULL)) > 0)
		g_string_append_len(message, buffer, count);
	J__WebView__internalJavaBridgeCall(env, this, _INTPTR(g_object_ref(request)), _JSTRING(message->str));
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, this);
	g_string_free(message, TRUE);
}

struct java_bridge_answer {
	WebKitURISchemeRequest *request;
	char *answer;
};

static gboolean java_bridge_answer_cb(gpointer user_data)
{
	struct java_bridge_answer *answer = user_data;
	java_bridge_reply(answer->request, answer->answer);
	g_object_unref(answer->request);
	g_free(answer->answer);
	g_free(answer);
	return G_SOURCE_REMOVE;
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1finishJavaBridgeCall(JNIEnv *env, jobject this, jlong request, jstring answer)
{
	struct java_bridge_answer *data = g_new(struct java_bridge_answer, 1);
	const char *text = _CSTRING(answer);
	data->request = _PTR(request);
	data->answer = g_strdup(text);
	(*env)->ReleaseStringUTFChars(env, answer, text);
	g_idle_add(java_bridge_answer_cb, data); // the answer is given on the main thread
}

/* WebViewClient.shouldInterceptRequest(): ATL's web process extension (src/webext/atl_webext.c) asks over a socket
 * before each subresource request; the answer is WebView.internalShouldInterceptRequest(), called on the socket's
 * thread (Android calls shouldInterceptRequest() off the UI thread too). A response is kept until WebKit fetches
 * it from the atl-intercept: scheme */
#define INTERCEPT_SCHEME "atl-intercept"
static GMutex intercept_lock;
static GHashTable *intercept_pages;     // page id -> weak global reference to the Java WebView
static GHashTable *intercept_responses; // response number -> GBytes, as internalShouldInterceptRequest() returned it
static guint intercept_next;
static char *intercept_socket_path;

static void intercept_set_page(JNIEnv *env, WebKitWebView *webview, jobject this)
{
	g_mutex_lock(&intercept_lock);
	jweak old = g_hash_table_lookup(intercept_pages, GUINT_TO_POINTER(webkit_web_view_get_page_id(webview)));
	if (old)
		(*env)->DeleteWeakGlobalRef(env, old);
	g_hash_table_insert(intercept_pages, GUINT_TO_POINTER(webkit_web_view_get_page_id(webview)), (*env)->NewWeakGlobalRef(env, this));
	g_mutex_unlock(&intercept_lock);
}

static gboolean intercept_connection_cb(GThreadedSocketService *service, GSocketConnection *connection, GObject *source, gpointer user_data)
{
	JNIEnv *env;
	(*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);
	g_autoptr(GDataInputStream) in = g_data_input_stream_new(g_io_stream_get_input_stream(G_IO_STREAM(connection)));
	GOutputStream *out = g_io_stream_get_output_stream(G_IO_STREAM(connection));
	char *line;
	while ((line = g_data_input_stream_read_line(in, NULL, NULL, NULL))) {
		char reply[64] = "";
		(*env)->PushLocalFrame(env, 16); // the thread stays attached: its local references go with each request
		char **field = g_strsplit(line, "\t", 4);
		if (g_strv_length(field) == 4) {
			g_mutex_lock(&intercept_lock);
			jweak weak = g_hash_table_lookup(intercept_pages, GUINT_TO_POINTER(g_ascii_strtoull(field[0], NULL, 10)));
			jobject this = weak ? (*env)->NewLocalRef(env, weak) : NULL;
			g_mutex_unlock(&intercept_lock);
			jbyteArray response = this ? J__WebView__internalShouldInterceptRequest(env, this, _JSTRING(field[2]), _JSTRING(field[1]), _JSTRING(field[3])) : NULL;
			if ((*env)->ExceptionCheck(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
				response = NULL;
			}
			if (response) {
				jsize length = (*env)->GetArrayLength(env, response);
				jbyte *bytes = (*env)->GetByteArrayElements(env, response, NULL);
				g_mutex_lock(&intercept_lock);
				guint number = ++intercept_next;
				g_hash_table_insert(intercept_responses, GUINT_TO_POINTER(number), g_bytes_new(bytes, length));
				g_mutex_unlock(&intercept_lock);
				(*env)->ReleaseByteArrayElements(env, response, bytes, JNI_ABORT);
				snprintf(reply, sizeof(reply), INTERCEPT_SCHEME ":///%u", number);
			}
		}
		g_strfreev(field);
		g_free(line);
		(*env)->PopLocalFrame(env, NULL);
		strcat(reply, "\n");
		if (!g_output_stream_write_all(out, reply, strlen(reply), NULL, NULL, NULL))
			break;
	}
	(*jvm)->DetachCurrentThread(jvm);
	return TRUE;
}

/* the response, as WebView.internalShouldInterceptRequest() wrote it: status code, reason phrase, content type
 * and "Name: value" header lines, an empty line, the data */
static void intercept_request_cb(WebKitURISchemeRequest *request, gpointer user_data)
{
	const char *number = strrchr(webkit_uri_scheme_request_get_uri(request), '/');
	g_mutex_lock(&intercept_lock);
	GBytes *bytes = number ? g_hash_table_lookup(intercept_responses, GUINT_TO_POINTER(strtoul(number + 1, NULL, 10))) : NULL;
	if (bytes)
		g_hash_table_steal(intercept_responses, GUINT_TO_POINTER(strtoul(number + 1, NULL, 10)));
	g_mutex_unlock(&intercept_lock);
	if (!bytes) {
		g_autoptr(GError) error = g_error_new(G_IO_ERROR, G_IO_ERROR_NOT_FOUND, "no intercepted response");
		webkit_uri_scheme_request_finish_error(request, error);
		return;
	}
	gsize size;
	const char *data = g_bytes_get_data(bytes, &size);
	// the end of the head: the empty line after the three fixed lines (whose third, the type, may be empty)
	const char *end = data;
	for (int i = 0; i < 3 && end; i++)
		end = memchr(end, '\n', size - (end - data)), end = end ? end + 1 : NULL;
	end = end ? g_strstr_len(end - 1, size - (end - 1 - data), "\n\n") : NULL;
	gsize head_length = end ? end - data : 0;
	g_autofree char *head = g_strndup(data, head_length);
	char **line = g_strsplit(head, "\n", -1);
	g_autoptr(GBytes) body = g_bytes_new_from_bytes(bytes, end ? head_length + 2 : size, end ? size - head_length - 2 : 0);
	g_bytes_unref(bytes);
	g_autoptr(GInputStream) stream = g_memory_input_stream_new_from_bytes(body);
	g_autoptr(WebKitURISchemeResponse) response = webkit_uri_scheme_response_new(stream, g_bytes_get_size(body));
	if (g_strv_length(line) >= 3) {
		webkit_uri_scheme_response_set_status(response, atoi(line[0]), line[1]);
		if (line[2][0])
			webkit_uri_scheme_response_set_content_type(response, line[2]);
		SoupMessageHeaders *headers = soup_message_headers_new(SOUP_MESSAGE_HEADERS_RESPONSE);
		for (char **header = line + 3; *header; header++) {
			char *colon = strchr(*header, ':');
			if (colon) {
				*colon = '\0';
				soup_message_headers_append(headers, *header, g_strstrip(colon + 1));
			}
		}
		/* on Android the response belongs to the URL the page asked for; here the scheme is another origin, which
		 * the page's fetch() and XMLHttpRequest may read only with this */
		if (!soup_message_headers_get_one(headers, "Access-Control-Allow-Origin"))
			soup_message_headers_append(headers, "Access-Control-Allow-Origin", "*");
		webkit_uri_scheme_response_set_http_headers(response, headers);
	}
	g_strfreev(line);
	webkit_uri_scheme_request_finish_with_response(request, response);
}

static void intercept_remove_socket(void)
{
	g_unlink(intercept_socket_path);
	g_autofree char *dir = g_path_get_dirname(intercept_socket_path);
	g_rmdir(dir);
}

/* before the web process starts: it loads the extension with the socket's path */
static void intercept_setup(WebKitWebContext *web_context)
{
	intercept_pages = g_hash_table_new(NULL, NULL);
	intercept_responses = g_hash_table_new_full(NULL, NULL, NULL, (GDestroyNotify)g_bytes_unref);
	/* in the app's own cache directory: the Flatpak portal exposes only the app's directories to the web
	 * process's sandbox */
	g_autofree char *dir = g_strdup_printf("%s/atl-webext-%d", g_get_user_cache_dir(), getpid());
	g_mkdir_with_parents(dir, 0700);
	intercept_socket_path = g_build_filename(dir, "s", NULL);
	g_unlink(intercept_socket_path);
	g_autoptr(GSocketAddress) address = g_unix_socket_address_new(intercept_socket_path);
	GSocketService *service = g_threaded_socket_service_new(-1);
	g_autoptr(GError) error = NULL;
	if (!g_socket_listener_add_address(G_SOCKET_LISTENER(service), address, G_SOCKET_TYPE_STREAM, G_SOCKET_PROTOCOL_DEFAULT, NULL, NULL, &error)) {
		g_warning("WebView: no shouldInterceptRequest(): %s", error->message);
		g_object_unref(service);
		return;
	}
	atexit(intercept_remove_socket);
	g_signal_connect(service, "run", G_CALLBACK(intercept_connection_cb), NULL);
	g_socket_service_start(service);
	webkit_web_context_add_path_to_sandbox(web_context, dir, FALSE);
	webkit_web_context_set_web_process_extensions_directory(web_context, ATL_WEBEXT_DIR);
	webkit_web_context_set_web_process_extensions_initialization_user_data(web_context, g_variant_new_string(intercept_socket_path));
	webkit_web_context_register_uri_scheme(web_context, INTERCEPT_SCHEME, intercept_request_cb, NULL, NULL);
	webkit_security_manager_register_uri_scheme_as_cors_enabled(webkit_web_context_get_security_manager(web_context), INTERCEPT_SCHEME);
	webkit_security_manager_register_uri_scheme_as_secure(webkit_web_context_get_security_manager(web_context), INTERCEPT_SCHEME);
}

/* The part of WebKit's back-forward list that is this WebView's history (WebView.canGoBackOrForward()): a parked
 * WebKitWebView (park_web_view()) still has the pages of the View it served before, and WebKit can't remove entries
 * for WebView.clearHistory(). "atl-history-floor" is the oldest entry the View may go back to (with
 * "atl-history-floor-excl" set: the first one after it), "atl-history-ceiling" the newest one it may go forward to,
 * NULL for no limit. */
static void history_set_floor(WebKitWebView *web_view, WebKitBackForwardListItem *item, gboolean exclusive)
{
	g_object_set_data_full(G_OBJECT(web_view), "atl-history-floor", item ? g_object_ref(item) : NULL, item ? g_object_unref : NULL);
	g_object_set_data(G_OBJECT(web_view), "atl-history-floor-excl", GINT_TO_POINTER(exclusive));
}

static void history_set_ceiling(WebKitWebView *web_view, WebKitBackForwardListItem *item)
{
	g_object_set_data_full(G_OBJECT(web_view), "atl-history-ceiling", item ? g_object_ref(item) : NULL, item ? g_object_unref : NULL);
}

static gboolean history_can_go(WebKitWebView *web_view, gint steps)
{
	WebKitBackForwardList *list = webkit_web_view_get_back_forward_list(web_view);
	WebKitBackForwardListItem *target = webkit_back_forward_list_get_nth_item(list, steps);
	if (!target)
		return FALSE;
	WebKitBackForwardListItem *limit = g_object_get_data(G_OBJECT(web_view), steps < 0 ? "atl-history-floor" : "atl-history-ceiling");
	if (!limit)
		return TRUE;
	// the limit must not lie on the way to the target, the target itself may be it
	for (gint i = 0; i != steps; i += steps < 0 ? -1 : 1) {
		if (webkit_back_forward_list_get_nth_item(list, i) == limit)
			return FALSE;
	}
	return !(steps < 0 && target == limit && g_object_get_data(G_OBJECT(web_view), "atl-history-floor-excl"));
}

/* WebView.postUrl(): "atl-post" is how far the POST got, "atl-post-script" the script that submits the form */
enum { POST_NONE, POST_REQUESTED, POST_FORM_PAGE, POST_SUBMITTED };

static void post_set_stage(WebKitWebView *web_view, int stage)
{
	WebKitUserScript *script = g_object_get_data(G_OBJECT(web_view), "atl-post-script");
	if (stage != POST_REQUESTED && stage != POST_FORM_PAGE && script) {
		webkit_user_content_manager_remove_script(webkit_web_view_get_user_content_manager(web_view), script);
		g_object_set_data(G_OBJECT(web_view), "atl-post-script", NULL);
	}
	g_object_set_data(G_OBJECT(web_view), "atl-post", GINT_TO_POINTER(stage));
}

static void web_view_load_changed(WebKitWebView *web_view, WebKitLoadEvent load_event, gpointer user_data)
{
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(web_view)));
	JNIEnv *env = get_jni_env();
	jobject this = webview_jobject(env, wrapper);
	if (!this)
		return;
	// a new web process (process swap) may have given the page another id
	if (load_event == WEBKIT_LOAD_COMMITTED)
		intercept_set_page(env, web_view, this);
	/* a new entry replaced the forward entries that clearHistory() left in WebKit's list (NavigationController
	 * PruneAllButLastCommitted() drops them on Android) */
	if (load_event == WEBKIT_LOAD_COMMITTED && webkit_back_forward_list_get_current_item(webkit_web_view_get_back_forward_list(web_view)) != g_object_get_data(G_OBJECT(web_view), "atl-history-ceiling"))
		history_set_ceiling(web_view, NULL);
	/* WebView.postUrl(): one load for the app (onPageStarted() of the form page, the rest from the POST) */
	int post = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(web_view), "atl-post"));
	gboolean hidden = (post == POST_REQUESTED || post == POST_FORM_PAGE) ? load_event != WEBKIT_LOAD_STARTED : post == POST_SUBMITTED && load_event == WEBKIT_LOAD_STARTED;
	if (post == POST_SUBMITTED && load_event != WEBKIT_LOAD_STARTED)
		post_set_stage(web_view, POST_NONE);
	if (!hidden)
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

/* WebView.getHitTestResult(): Chromium keeps the data of the last hit test (AwContents.mPossiblyStaleHitTestData),
 * here that is the element under the pointer */
static void mouse_target_changed_cb(WebKitWebView *web_view, WebKitHitTestResult *hit_test_result, guint modifiers, WrapperWidget *wrapper)
{
	JNIEnv *env = get_jni_env();
	jobject this = webview_jobject(env, wrapper);
	if (!this)
		return;
	const char *link = webkit_hit_test_result_context_is_link(hit_test_result) ? webkit_hit_test_result_get_link_uri(hit_test_result) : NULL;
	const char *label = link ? webkit_hit_test_result_get_link_label(hit_test_result) : NULL;
	const char *image = webkit_hit_test_result_context_is_image(hit_test_result) ? webkit_hit_test_result_get_image_uri(hit_test_result) : NULL;
	jstring jlink = link ? _JSTRING(link) : NULL;
	jstring jlabel = label ? _JSTRING(label) : NULL;
	jstring jimage = image ? _JSTRING(image) : NULL;
	J__WebView__internalHitTest(env, this, jlink, jlabel, jimage, webkit_hit_test_result_context_is_editable(hit_test_result));
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	if (jlink)
		(*env)->DeleteLocalRef(env, jlink);
	if (jlabel)
		(*env)->DeleteLocalRef(env, jlabel);
	if (jimage)
		(*env)->DeleteLocalRef(env, jimage);
	(*env)->DeleteLocalRef(env, this);
}

// WebView.FindListener: the result of findAllAsync()/findNext()
static void find_result(WrapperWidget *wrapper, guint match_count)
{
	JNIEnv *env = get_jni_env();
	jobject this = webview_jobject(env, wrapper);
	if (!this)
		return;
	J__WebView__internalFindResult(env, this, match_count);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, this);
}

static void found_text_cb(WebKitFindController *find_controller, guint match_count, WrapperWidget *wrapper)
{
	find_result(wrapper, match_count);
}

static void failed_to_find_text_cb(WebKitFindController *find_controller, WrapperWidget *wrapper)
{
	find_result(wrapper, 0);
}

static gboolean decide_policy(WebKitWebView *web_view, WebKitPolicyDecision *decision, WebKitPolicyDecisionType type, WrapperWidget *wrapper)
{
	if (type == WEBKIT_POLICY_DECISION_TYPE_NAVIGATION_ACTION) {
		WebKitNavigationPolicyDecision *nav = WEBKIT_NAVIGATION_POLICY_DECISION(decision);
		WebKitNavigationAction *action = webkit_navigation_policy_decision_get_navigation_action(nav);
		WebKitURIRequest *request = webkit_navigation_action_get_request(action);
		const gchar *uri = webkit_uri_request_get_uri(request);
		/* goBack()/goForward() of the app: Chromium does not ask shouldOverrideUrlLoading() for history
		 * navigations (PAGE_TRANSITION_FORWARD_BACK) */
		if (webkit_navigation_action_get_navigation_type(action) == WEBKIT_NAVIGATION_TYPE_BACK_FORWARD) {
			webkit_policy_decision_use(decision);
			return TRUE;
		}
		/* WebView.postUrl(): the form page was the app's load (asked like loadUrl()), its submission is the POST of
		 * that same load; any other navigation ends the postUrl() */
		int post = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(web_view), "atl-post"));
		if (post && webkit_navigation_action_get_navigation_type(action) == WEBKIT_NAVIGATION_TYPE_FORM_SUBMITTED) {
			post_set_stage(web_view, POST_SUBMITTED);
			webkit_policy_decision_use(decision);
			return TRUE;
		}
		post_set_stage(web_view, post == POST_REQUESTED ? POST_FORM_PAGE : POST_NONE);

		JNIEnv *env = get_jni_env();
		jobject this = webview_jobject(env, wrapper);
		// a page without its WebView doesn't navigate anywhere
		jboolean result = this ? J__WebView__internalShouldOverrideUrlLoading(env, this, _JSTRING(uri)) : JNI_TRUE;
		if (this)
			(*env)->DeleteLocalRef(env, this);
		if (result) {
			post_set_stage(web_view, POST_NONE);
			webkit_policy_decision_ignore(decision);
		} else
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
	webkit_user_content_manager_remove_all_style_sheets(content_manager);
	g_signal_handlers_disconnect_by_func(webview, web_view_load_changed, NULL);
	g_signal_handlers_disconnect_by_func(webview, web_view_uri_changed, NULL);
	g_signal_handlers_disconnect_by_func(webview, context_menu_cb, NULL);
	g_signal_handlers_disconnect_by_data(webview, wrapper);
	g_signal_handlers_disconnect_by_data(webkit_web_view_get_find_controller(webview), wrapper);
	webkit_find_controller_search_finish(webkit_web_view_get_find_controller(webview));
	history_set_floor(webview, NULL, FALSE);
	history_set_ceiling(webview, NULL);
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
	/* WebKit animates each mouse wheel step with an ease-out curve that starts over at every further
	 * step, so a spinning wheel scrolls in lurches; the page animates the steps like Chromium instead
	 * (wheel_script below), and WebKit's animation stays off. The setting has to be
	 * there when the view is created: the page may set up its threaded scrolling tree right away, and
	 * that keeps the value it saw (WebKitGTK 2.54 ThreadedScrollingTree::m_scrollAnimatorEnabled). */
	static gboolean intercept_set_up = FALSE;
	if (!intercept_set_up) {
		intercept_setup(webkit_web_context_get_default());
		intercept_set_up = TRUE;
	}
	WebKitSettings *settings = webkit_settings_new();
	webkit_settings_set_enable_smooth_scrolling(settings, FALSE);
	GtkWidget *webview = GTK_WIDGET(take_parked_web_view());
	if (webview) {
		// what a new WebKitWebView has: default settings and background
		webkit_web_view_set_settings(WEBKIT_WEB_VIEW(webview), settings);
		webkit_web_view_set_background_color(WEBKIT_WEB_VIEW(webview), &(GdkRGBA){1.f, 1.f, 1.f, 1.f});
		// what a new WebKitWebView has: no history, the about:blank of parking is not part of it
		history_set_floor(WEBKIT_WEB_VIEW(webview), webkit_back_forward_list_get_current_item(webkit_web_view_get_back_forward_list(WEBKIT_WEB_VIEW(webview))), TRUE);
	} else {
		WebKitWebView *related = g_weak_ref_get(&related_view);
		WebKitUserContentManager *own_content_manager = webkit_user_content_manager_new();
		webview = g_object_new(WEBKIT_TYPE_WEB_VIEW, "related-view", related, "user-content-manager", own_content_manager, "settings", settings, NULL);
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
	g_object_unref(settings);
	/* a WebView that follows its page (wrap_content in a scrolling list) is laid out with height 0 until the page
	 * reports its size, and WebKitGTK (2.54) did not draw a page whose view was first allocated a height of 0, also not
	 * after it grew; the wrapper allocates the view at least its minimum, the View keeps the size from the layout */
	gtk_widget_set_size_request(webview, 1, 1);
	g_object_weak_ref(G_OBJECT(wrapper), wrapper_finalized, webview);
	undo_font_dpi_zoom(gtk_settings_get_default(), NULL, WEBKIT_WEB_VIEW(webview));
	g_signal_connect_object(gtk_settings_get_default(), "notify::gtk-xft-dpi", G_CALLBACK(undo_font_dpi_zoom), webview, 0);
	wrapper_widget_set_child(WRAPPER_WIDGET(wrapper), webview);
	wrapper_widget_set_jobject(WRAPPER_WIDGET(wrapper), env, this);
	intercept_set_page(env, WEBKIT_WEB_VIEW(webview), this);
	wrapper_widget_register_invalidation_listener(WRAPPER_WIDGET(wrapper));
	// all WebViews share the default web context, and registering a scheme twice is an error
	static gboolean asset_scheme_registered = FALSE;
	if (!asset_scheme_registered) {
		webkit_web_context_register_uri_scheme(webkit_web_view_get_context(WEBKIT_WEB_VIEW(webview)), "android-asset", asset_uri_scheme_request_cb, NULL, NULL);
		webkit_security_manager_register_uri_scheme_as_local(webkit_web_context_get_security_manager(webkit_web_view_get_context(WEBKIT_WEB_VIEW(webview))), "android-asset");
		WebKitWebContext *web_context = webkit_web_view_get_context(WEBKIT_WEB_VIEW(webview));
		webkit_web_context_register_uri_scheme(web_context, JAVA_BRIDGE_SCHEME, java_bridge_request_cb, NULL, NULL);
		webkit_security_manager_register_uri_scheme_as_cors_enabled(webkit_web_context_get_security_manager(web_context), JAVA_BRIDGE_SCHEME);
		webkit_security_manager_register_uri_scheme_as_secure(webkit_web_context_get_security_manager(web_context), JAVA_BRIDGE_SCHEME);
		asset_scheme_registered = TRUE;
	}
	WebKitUserContentManager *content_manager = webkit_web_view_get_user_content_manager(WEBKIT_WEB_VIEW(webview));
	webkit_user_content_manager_register_script_message_handler(content_manager, "atlbridge", NULL);
	g_signal_connect(content_manager, "script-message-received::atlbridge", G_CALLBACK(script_message_cb), wrapper);
	/* forward console messages to WebChromeClient.onConsoleMessage() */
	WebKitUserScript *console_script = webkit_user_script_new(
	    "(function(){['log','info','warn','error','debug'].forEach(function(level){var orig=console[level];"
	    "console[level]=function(){var msg=Array.prototype.map.call(arguments,String).join(' ');"
	    "try{window.webkit.messageHandlers.atlbridge.postMessage('\001console\001'+level+'\001'+msg);}catch(e){}"
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
	/* Chromium's WebView mirrors the page's scroll offset in View.getScrollX/Y() (AwScrollOffsetManager), apps
	 * follow it with onScrollChanged() and read getContentHeight(). Report the offset and the size of the page
	 * when it scrolls or changes its size; WebView.internalScrollChanged() takes them. */
	WebKitUserScript *scroll_script = webkit_user_script_new(
	    "(function(){var last='';function report(){var e=document.scrollingElement||document.documentElement;if(!e)return;"
	    "var m='\001scroll\001'+scrollX+'\001'+scrollY+'\001'+e.scrollWidth+'\001'+e.scrollHeight;"
	    "if(m===last)return;last=m;try{window.webkit.messageHandlers.atlbridge.postMessage(m);}catch(x){}}"
	    "addEventListener('scroll',report,{passive:true});addEventListener('resize',report);"
	    "addEventListener('DOMContentLoaded',function(){report();new ResizeObserver(report).observe(document.documentElement);});})();",
	    WEBKIT_USER_CONTENT_INJECT_TOP_FRAME, WEBKIT_USER_SCRIPT_INJECT_AT_DOCUMENT_START, NULL, NULL);
	webkit_user_content_manager_add_script(content_manager, scroll_script);
	webkit_user_script_unref(scroll_script);
	/* Android's WebView animates a mouse wheel step (Chromium: enable_scroll_animator is on where
	 * gfx::Animation::ScrollAnimationsEnabledBySystem(), always on Android), and a further step changes the
	 * target of the running animation and keeps its speed. WebKit's own animation starts over at every step
	 * and scrolls in lurches (see above), so the page scrolls with Chromium's curve instead:
	 * cc/animation/scroll_offset_animation_curve.cc, kEaseInOut with DurationBehavior::kInverseDelta
	 * (EaseInOutSegmentDuration()), UpdateTarget() with EaseInOutWithInitialSlope() and
	 * VelocityBasedDurationBound(). A step the page cancels, or none of its boxes can take, stays WebKit's. */
	WebKitUserScript *wheel_script = webkit_user_script_new(
	    "(function(){var E=0.01,act=new Map();"
	    "function bez(x1,y1,x2,y2){var cx=3*x1,bx=3*(x2-x1)-cx,ax=1-cx-bx,cy=3*y1,by=3*(y2-y1)-cy,ay=1-cy-by;"
	    "function sx(t){return((ax*t+bx)*t+cx)*t;}function dx(t){return(3*ax*t+2*bx)*t+cx;}"
	    "function sy(t){return((ay*t+by)*t+cy)*t;}function dy(t){return(3*ay*t+2*by)*t+cy;}"
	    "function solve(x){var t=x,i,e,d,lo=0,hi=1;for(i=0;i<8;i++){e=sx(t)-x;if(Math.abs(e)<1e-7)return t;d=dx(t);"
	    "if(Math.abs(d)<1e-6)break;t-=e/d;}t=x;for(i=0;i<60;i++){e=sx(t);if(Math.abs(e-x)<1e-7)break;if(x>e)lo=t;else hi=t;t=(lo+hi)/2;}return t;}"
	    "return{y:function(x){return sy(solve(x));},v:function(x){var t=solve(x),d=dx(t);return d?dy(t)/d:0;}};}"
	    "function md(x,y){return Math.abs(x)>Math.abs(y)?x:y;}"
	    /* EaseInOutSegmentDuration(), kInverseDelta: (14 - |delta|/60) frames of 1/60 s, between 6 and 12 */
	    "function segdur(x,y){return Math.min(12,Math.max(6,14-Math.abs(md(x,y))/60))/60;}"
	    "function at(a,now){var x=(now-a.t0)/1000/a.d;if(x>=1)return[a.tx,a.ty,1];var p=a.f.y(x);"
	    "return[a.ix+(a.tx-a.ix)*p,a.iy+(a.ty-a.iy)*p,0];}"
	    "function vel(a,now){var x=Math.min(1,(now-a.t0)/1000/a.d);return a.f.v(x)*md(a.tx-a.ix,a.ty-a.iy)/a.d;}"
	    "function retarget(a,now,tx,ty){if(Math.abs(md(a.tx-tx,a.ty-ty))<E)return;var c=at(a,now),nx=tx-c[0],ny=ty-c[1],m=md(nx,ny);"
	    "a.tx=tx;a.ty=ty;if(Math.abs(m)<E){a.d=0;return;}var v=vel(a,now),d=segdur(nx,ny);"
	    "if(Math.abs(v)>=E){var b=m/v*2.5;if(b>=0)d=Math.min(d,b);}if(d<E){a.d=0;return;}"
	    "var s=Math.max(-1000,Math.min(1000,v*d/m));a.f=bez(0.42,0.42*s,0.58,1);a.ix=c[0];a.iy=c[1];a.t0=now;a.d=d;}"
	    "function root(){return document.scrollingElement||document.documentElement;}"
	    "function room(el,x,y){var a=act.get(el),l=a?a.tx:el.scrollLeft,t=a?a.ty:el.scrollTop;"
	    "return(y>0&&t<el.scrollHeight-el.clientHeight-0.5)||(y<0&&t>0.5)||(x>0&&l<el.scrollWidth-el.clientWidth-0.5)||(x<0&&l>0.5);}"
	    "function box(n,x,y){for(var el=n&&n.nodeType===1?n:n&&n.parentElement;el&&el!==root();el=el.parentElement||(el.getRootNode().host||null)){"
	    "var s=getComputedStyle(el);if(/(auto|scroll|overlay)/.test(s.overflowX+s.overflowY)&&room(el,x,y))return el;}"
	    "var r=root();return r&&room(r,x,y)?r:null;}"
	    "function step(now){act.forEach(function(a,el){var p=a.d?at(a,now):[a.tx,a.ty,1];"
	    "el.scrollTo({left:p[0],top:p[1],behavior:'instant'});if(p[2])act.delete(el);});if(act.size)requestAnimationFrame(step);}"
	    "addEventListener('wheel',function(e){if(e.defaultPrevented||!e.cancelable||e.ctrlKey||e.deltaMode!==0||(!e.deltaX&&!e.deltaY))return;"
	    "var el=box(e.target,e.deltaX,e.deltaY);if(!el)return;e.preventDefault();var now=performance.now(),a=act.get(el);"
	    "function cl(v,m){return Math.max(0,Math.min(m,v));}var mx=el.scrollWidth-el.clientWidth,my=el.scrollHeight-el.clientHeight;"
	    "if(a){retarget(a,now,cl(a.tx+e.deltaX,mx),cl(a.ty+e.deltaY,my));return;}"
	    "a={ix:el.scrollLeft,iy:el.scrollTop,t0:now,f:bez(0.42,0,0.58,1)};a.tx=cl(a.ix+e.deltaX,mx);a.ty=cl(a.iy+e.deltaY,my);"
	    "a.d=segdur(a.tx-a.ix,a.ty-a.iy);act.set(el,a);if(act.size===1)requestAnimationFrame(step);},{passive:false});})();",
	    WEBKIT_USER_CONTENT_INJECT_ALL_FRAMES, WEBKIT_USER_SCRIPT_INJECT_AT_DOCUMENT_START, NULL, NULL);
	webkit_user_content_manager_add_script(content_manager, wheel_script);
	webkit_user_script_unref(wheel_script);
	g_signal_connect(G_OBJECT(webview), "load-changed", G_CALLBACK(web_view_load_changed), NULL);
	g_signal_connect(G_OBJECT(webview), "notify::uri", G_CALLBACK(web_view_uri_changed), NULL);
	g_signal_connect(webview, "decide-policy", G_CALLBACK(decide_policy), wrapper);
	g_signal_connect(webview, "context-menu", G_CALLBACK(context_menu_cb), NULL);
	g_signal_connect(webview, "mouse-target-changed", G_CALLBACK(mouse_target_changed_cb), wrapper);
	WebKitFindController *find_controller = webkit_web_view_get_find_controller(WEBKIT_WEB_VIEW(webview));
	g_signal_connect(find_controller, "found-text", G_CALLBACK(found_text_cb), wrapper);
	g_signal_connect(find_controller, "failed-to-find-text", G_CALLBACK(failed_to_find_text_cb), wrapper);
	return _INTPTR(webview);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1loadUrl(JNIEnv *env, jobject this, jlong widget_ptr, jstring url)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;

	WebKitWebView *webview = _PTR(widget_ptr);
	webkit_web_view_load_uri(webview, _CSTRING(url));
}

/* WebView.postUrl(): the form page (from Java) is loaded with the URL as its own; a script in a world of its own
 * (so it also runs with the page's scripts turned off, enable-javascript-markup) submits the form while the page
 * loads, WebKit's navigation then replaces the form page's history entry */
JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1postUrl(JNIEnv *env, jobject this, jlong widget_ptr, jstring url, jbyteArray form, jstring form_id)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;

	WebKitWebView *webview = _PTR(widget_ptr);
	post_set_stage(webview, POST_NONE);
	const char *id = _CSTRING(form_id);
	g_autofree char *source = g_strdup_printf("var form = document.getElementById('%s'); if (form) form.submit();", id);
	(*env)->ReleaseStringUTFChars(env, form_id, id);
	WebKitUserScript *script = webkit_user_script_new_for_world(source, WEBKIT_USER_CONTENT_INJECT_TOP_FRAME, WEBKIT_USER_SCRIPT_INJECT_AT_DOCUMENT_END, "atl-post", NULL, NULL);
	webkit_user_content_manager_add_script(webkit_web_view_get_user_content_manager(webview), script);
	g_object_set_data_full(G_OBJECT(webview), "atl-post-script", script, (GDestroyNotify)webkit_user_script_unref);
	post_set_stage(webview, POST_REQUESTED);
	jsize length = (*env)->GetArrayLength(env, form);
	jbyte *bytes = (*env)->GetByteArrayElements(env, form, NULL);
	g_autoptr(GBytes) data = g_bytes_new(bytes, length);
	(*env)->ReleaseByteArrayElements(env, form, bytes, JNI_ABORT);
	const char *uri = _CSTRING(url);
	webkit_web_view_load_bytes(webview, data, "text/html", "UTF-8", uri);
	(*env)->ReleaseStringUTFChars(env, url, uri);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1reload(JNIEnv *env, jobject this, jlong widget_ptr)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;
	WebKitWebView *webview = _PTR(widget_ptr);
	webkit_web_view_reload(webview);
}

/* WebView.loadUrl(String, Map): the additional headers go with the request of the page */
JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1loadUrlWithHeaders(JNIEnv *env, jobject this, jlong widget_ptr, jstring url, jobjectArray headers)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview)) // WebViews are disabled, this is a plain View
		return;
	const char *uri = (*env)->GetStringUTFChars(env, url, NULL);
	WebKitURIRequest *request = webkit_uri_request_new(uri);
	(*env)->ReleaseStringUTFChars(env, url, uri);
	SoupMessageHeaders *http_headers = webkit_uri_request_get_http_headers(request); // NULL if not HTTP
	jsize count = headers ? (*env)->GetArrayLength(env, headers) : 0;
	for (jsize i = 0; http_headers && i + 1 < count; i += 2) {
		jstring jname = (*env)->GetObjectArrayElement(env, headers, i);
		jstring jvalue = (*env)->GetObjectArrayElement(env, headers, i + 1);
		if (jname && jvalue) {
			const char *name = (*env)->GetStringUTFChars(env, jname, NULL);
			const char *value = (*env)->GetStringUTFChars(env, jvalue, NULL);
			soup_message_headers_replace(http_headers, name, value);
			(*env)->ReleaseStringUTFChars(env, jname, name);
			(*env)->ReleaseStringUTFChars(env, jvalue, value);
		}
		if (jname)
			(*env)->DeleteLocalRef(env, jname);
		if (jvalue)
			(*env)->DeleteLocalRef(env, jvalue);
	}
	webkit_web_view_load_request(WEBKIT_WEB_VIEW(webview), request);
	g_object_unref(request);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1stopLoading(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (WEBKIT_IS_WEB_VIEW(webview))
		webkit_web_view_stop_loading(WEBKIT_WEB_VIEW(webview));
}

JNIEXPORT jboolean JNICALL Java_android_webkit_WebView_native_1canGoBackOrForward(JNIEnv *env, jobject this, jlong widget_ptr, jint steps)
{
	GtkWidget *webview = _PTR(widget_ptr);
	return WEBKIT_IS_WEB_VIEW(webview) && history_can_go(WEBKIT_WEB_VIEW(webview), steps);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1goBackOrForward(JNIEnv *env, jobject this, jlong widget_ptr, jint steps)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview) || !history_can_go(WEBKIT_WEB_VIEW(webview), steps))
		return;
	WebKitBackForwardList *list = webkit_web_view_get_back_forward_list(WEBKIT_WEB_VIEW(webview));
	webkit_web_view_go_to_back_forward_list_item(WEBKIT_WEB_VIEW(webview), webkit_back_forward_list_get_nth_item(list, steps));
}

/* AwContents.clearHistory() keeps only the current entry (NavigationController PruneAllButLastCommitted()) */
JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1clearHistory(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview))
		return;
	WebKitBackForwardListItem *current = webkit_back_forward_list_get_current_item(webkit_web_view_get_back_forward_list(WEBKIT_WEB_VIEW(webview)));
	if (!current)
		return;
	history_set_floor(WEBKIT_WEB_VIEW(webview), current, FALSE);
	history_set_ceiling(WEBKIT_WEB_VIEW(webview), current);
}

JNIEXPORT jstring JNICALL Java_android_webkit_WebView_native_1getTitle(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *webview = _PTR(widget_ptr);
	const char *title = WEBKIT_IS_WEB_VIEW(webview) ? webkit_web_view_get_title(WEBKIT_WEB_VIEW(webview)) : NULL;
	return title ? _JSTRING(title) : NULL;
}

// the URL first asked for the current entry, before redirects
JNIEXPORT jstring JNICALL Java_android_webkit_WebView_native_1getOriginalUrl(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview))
		return NULL;
	WebKitBackForwardListItem *current = webkit_back_forward_list_get_current_item(webkit_web_view_get_back_forward_list(WEBKIT_WEB_VIEW(webview)));
	const char *uri = current ? webkit_back_forward_list_item_get_original_uri(current) : NULL;
	return uri ? _JSTRING(uri) : NULL;
}

// the DER form of the page's server certificate, NULL without one (not loaded over TLS)
JNIEXPORT jbyteArray JNICALL Java_android_webkit_WebView_native_1getCertificate(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *webview = _PTR(widget_ptr);
	GTlsCertificate *certificate = NULL;
	if (!WEBKIT_IS_WEB_VIEW(webview) || !webkit_web_view_get_tls_info(WEBKIT_WEB_VIEW(webview), &certificate, NULL) || !certificate)
		return NULL;
	GByteArray *der = NULL;
	g_object_get(certificate, "certificate", &der, NULL);
	if (!der)
		return NULL;
	jbyteArray result = (*env)->NewByteArray(env, der->len);
	(*env)->SetByteArrayRegion(env, result, 0, der->len, (const jbyte *)der->data);
	g_byte_array_unref(der);
	return result;
}

/* findAllAsync(): Chromium's find in page is case insensitive and wraps around; the result comes to
 * internalFindResult() */
JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1findAll(JNIEnv *env, jobject this, jlong widget_ptr, jstring text)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview))
		return;
	WebKitFindController *find_controller = webkit_web_view_get_find_controller(WEBKIT_WEB_VIEW(webview));
	const char *search_text = (*env)->GetStringUTFChars(env, text, NULL);
	if (search_text[0])
		webkit_find_controller_search(find_controller, search_text, WEBKIT_FIND_OPTIONS_CASE_INSENSITIVE | WEBKIT_FIND_OPTIONS_WRAP_AROUND, G_MAXUINT);
	else
		webkit_find_controller_search_finish(find_controller);
	(*env)->ReleaseStringUTFChars(env, text, search_text);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1findNext(JNIEnv *env, jobject this, jlong widget_ptr, jboolean forward)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview))
		return;
	WebKitFindController *find_controller = webkit_web_view_get_find_controller(WEBKIT_WEB_VIEW(webview));
	if (forward)
		webkit_find_controller_search_next(find_controller);
	else
		webkit_find_controller_search_previous(find_controller);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1clearMatches(JNIEnv *env, jobject this, jlong widget_ptr)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (WEBKIT_IS_WEB_VIEW(webview))
		webkit_find_controller_search_finish(webkit_web_view_get_find_controller(WEBKIT_WEB_VIEW(webview)));
}

/* AwContents.clearCache(): the memory cache, and with includeDiskFiles the disk cache, of all WebViews */
JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1clearCache(JNIEnv *env, jobject this, jlong widget_ptr, jboolean include_disk_files)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview))
		return;
	WebKitWebsiteDataManager *manager = webkit_network_session_get_website_data_manager(webkit_web_view_get_network_session(WEBKIT_WEB_VIEW(webview)));
	WebKitWebsiteDataTypes types = WEBKIT_WEBSITE_DATA_MEMORY_CACHE | (include_disk_files ? WEBKIT_WEBSITE_DATA_DISK_CACHE : 0);
	webkit_website_data_manager_clear(manager, types, 0, NULL, NULL, NULL);
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

/* WebKit's cookie store belongs to the main thread; CookieManager may be used from any thread, so the change
 * runs there (at once when called on it) */
static gboolean add_cookie_on_main(gpointer cookie)
{
	WebKitCookieManager *cookie_manager = webkit_network_session_get_cookie_manager(webkit_network_session_get_default());
	webkit_cookie_manager_add_cookie(cookie_manager, cookie, NULL, NULL, NULL);
	soup_cookie_free(cookie);
	return G_SOURCE_REMOVE;
}

/* CookieManager.setCookie(): the value is a Set-Cookie header for the URL; FALSE if it is not a valid cookie
 * for it (e.g. a Domain the URL's host is not in) */
JNIEXPORT jboolean JNICALL Java_android_webkit_CookieManager_native_1setCookie(JNIEnv *env, jclass class, jstring url, jstring value)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return JNI_FALSE;
	const char *uri = (*env)->GetStringUTFChars(env, url, NULL);
	GUri *origin = g_uri_parse(uri, G_URI_FLAGS_ENCODED, NULL);
	(*env)->ReleaseStringUTFChars(env, url, uri);
	if (!origin)
		return JNI_FALSE;
	const char *header = (*env)->GetStringUTFChars(env, value, NULL);
	SoupCookie *cookie = soup_cookie_parse(header, origin);
	(*env)->ReleaseStringUTFChars(env, value, header);
	g_uri_unref(origin);
	if (!cookie)
		return JNI_FALSE;
	g_main_context_invoke(NULL, add_cookie_on_main, cookie);
	return JNI_TRUE;
}

static gboolean remove_all_cookies_on_main(gpointer user_data)
{
	WebKitWebsiteDataManager *manager = webkit_network_session_get_website_data_manager(webkit_network_session_get_default());
	webkit_website_data_manager_clear(manager, WEBKIT_WEBSITE_DATA_COOKIES, 0, NULL, NULL, NULL);
	return G_SOURCE_REMOVE;
}

JNIEXPORT void JNICALL Java_android_webkit_CookieManager_native_1removeAllCookies(JNIEnv *env, jclass class)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;
	g_main_context_invoke(NULL, remove_all_cookies_on_main, NULL);
}

/* CookieManager.removeSessionCookies(): the cookies without an expiry date go, on the main thread; the calling
 * thread waits for the result */
struct remove_session_cookies {
	GMutex lock;
	GCond cond;
	guint pending; // cookie list + deletions not yet answered
	gboolean removed;
};

static void remove_session_cookies_step(struct remove_session_cookies *data)
{
	g_mutex_lock(&data->lock);
	data->pending--;
	g_cond_signal(&data->cond);
	g_mutex_unlock(&data->lock);
}

static void session_cookie_deleted_cb(GObject *cookie_manager, GAsyncResult *res, gpointer user_data)
{
	webkit_cookie_manager_delete_cookie_finish(WEBKIT_COOKIE_MANAGER(cookie_manager), res, NULL);
	remove_session_cookies_step(user_data);
}

static void session_cookies_listed_cb(GObject *cookie_manager, GAsyncResult *res, gpointer user_data)
{
	struct remove_session_cookies *data = user_data;
	GList *cookies = webkit_cookie_manager_get_all_cookies_finish(WEBKIT_COOKIE_MANAGER(cookie_manager), res, NULL);
	for (GList *l = cookies; l; l = l->next) {
		if (soup_cookie_get_expires(l->data))
			continue;
		g_mutex_lock(&data->lock);
		data->pending++;
		data->removed = TRUE;
		g_mutex_unlock(&data->lock);
		webkit_cookie_manager_delete_cookie(WEBKIT_COOKIE_MANAGER(cookie_manager), l->data, NULL, session_cookie_deleted_cb, data);
	}
	g_list_free_full(cookies, (GDestroyNotify)soup_cookie_free);
	remove_session_cookies_step(data);
}

static gboolean remove_session_cookies_on_main(gpointer data)
{
	webkit_cookie_manager_get_all_cookies(webkit_network_session_get_cookie_manager(webkit_network_session_get_default()), NULL, session_cookies_listed_cb, data);
	return G_SOURCE_REMOVE;
}

JNIEXPORT jboolean JNICALL Java_android_webkit_CookieManager_native_1removeSessionCookies(JNIEnv *env, jclass class)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return JNI_FALSE;
	struct remove_session_cookies data = {.pending = 1};
	g_mutex_init(&data.lock);
	g_cond_init(&data.cond);
	gboolean main_thread = g_thread_self() == main_thread_id;
	if (main_thread)
		remove_session_cookies_on_main(&data);
	else
		g_main_context_invoke(NULL, remove_session_cookies_on_main, &data);
	g_mutex_lock(&data.lock);
	while (data.pending) {
		if (main_thread) {
			g_mutex_unlock(&data.lock);
			g_main_context_iteration(g_main_context_default(), TRUE);
			g_mutex_lock(&data.lock);
		} else
			g_cond_wait(&data.cond, &data.lock);
	}
	g_mutex_unlock(&data.lock);
	g_mutex_clear(&data.lock);
	g_cond_clear(&data.cond);
	return data.removed;
}

static gboolean set_accept_policy_on_main(gpointer policy)
{
	WebKitCookieManager *cookie_manager = webkit_network_session_get_cookie_manager(webkit_network_session_get_default());
	webkit_cookie_manager_set_accept_policy(cookie_manager, GPOINTER_TO_INT(policy));
	return G_SOURCE_REMOVE;
}

// policy: 0 accept always, 1 never, 2 not from third parties (WebKitCookieAcceptPolicy)
JNIEXPORT void JNICALL Java_android_webkit_CookieManager_native_1setAcceptPolicy(JNIEnv *env, jclass class, jint policy)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;
	g_main_context_invoke(NULL, set_accept_policy_on_main, GINT_TO_POINTER(policy));
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1addJavascriptInterface(JNIEnv *env, jobject this, jlong widget_ptr, jstring name, jstring method_names)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;

	WebKitWebView *webview = _PTR(widget_ptr);
	/* window.<name>.<method>(args...) asks the Java bridge (see java_bridge_request_cb()) with a synchronous request:
	 * "<name>\1<method>\1<typeof>:<value>..." (null as "null:"), answered by "u" (undefined), "n" (null), "b<bool>",
	 * "d<number>", "s<string>" or "e<error>", which throws like Chromium's bridge does. */
	char *source = g_strdup_printf(
	    "(function(){var o={};'%s'.split(',').forEach(function(m){o[m]=function(){"
	    "var a=['%s',m];for(var i=0;i<arguments.length;i++){var x=arguments[i],t=typeof x;"
	    "a.push(x===null?'null:':t+':'+(t==='number'||t==='boolean'||t==='string'?String(x):''));}"
	    "var r=new XMLHttpRequest();r.open('POST','" JAVA_BRIDGE_SCHEME "://call',false);r.send(a.join('\\u0001'));"
	    "var t=r.responseText,v=t.substring(1);"
	    "switch(t.charAt(0)){case 'e':throw new Error('Error invoking '+m+': '+v);case 'b':return v==='true';"
	    "case 'd':return Number(v);case 's':return v;case 'n':return null;}};});"
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

/* WebView.onScrollChanged(): the app scrolled the WebView, scroll the page (AwScrollOffsetManager.scrollNativeTo()) */
JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1scrollTo(JNIEnv *env, jobject this, jlong widget_ptr, jfloat x, jfloat y)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview)) // destroyed, or WebViews are disabled
		return;

	char x_str[G_ASCII_DTOSTR_BUF_SIZE], y_str[G_ASCII_DTOSTR_BUF_SIZE];
	char *script = g_strdup_printf("scrollTo(%s,%s)", g_ascii_dtostr(x_str, sizeof(x_str), x), g_ascii_dtostr(y_str, sizeof(y_str), y));
	webkit_web_view_evaluate_javascript(WEBKIT_WEB_VIEW(webview), script, -1, NULL, NULL, NULL, NULL, NULL);
	g_free(script);
}

/* WebView.setVerticalScrollBarEnabled(): Android's WebView then draws no vertical scrollbar for the page.
 * WebKitGTK has no setting for its own scrollbars, and its ::-webkit-scrollbar:vertical does not hide one axis
 * alone (tested with 2.54), so a user style sheet hides both; pages rarely scroll sideways. The sheet belongs to
 * this view's own user content manager, so only its page restyles, and apps turn the scrollbar off before they
 * load the page. */
JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1setVerticalScrollBarEnabled(JNIEnv *env, jobject this, jlong widget_ptr, jboolean enabled)
{
	GtkWidget *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview)) // destroyed, or WebViews are disabled
		return;

	WebKitUserContentManager *content_manager = webkit_web_view_get_user_content_manager(WEBKIT_WEB_VIEW(webview));
	webkit_user_content_manager_remove_all_style_sheets(content_manager);
	if (enabled)
		return;
	WebKitUserStyleSheet *style_sheet = webkit_user_style_sheet_new("html{scrollbar-width:none!important}",
	    WEBKIT_USER_CONTENT_INJECT_TOP_FRAME, WEBKIT_USER_STYLE_LEVEL_USER, NULL, NULL);
	webkit_user_content_manager_add_style_sheet(content_manager, style_sheet);
	webkit_user_style_sheet_unref(style_sheet);
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

/* WebSettings: a property of the view's WebKitSettings */
JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1setBooleanSetting(JNIEnv *env, jobject this, jlong widget_ptr, jstring name, jboolean value)
{
	WebKitWebView *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview)) // destroyed, or WebViews are disabled
		return;
	const char *name_str = (*env)->GetStringUTFChars(env, name, NULL);
	g_object_set(webkit_web_view_get_settings(webview), name_str, (gboolean)value, NULL);
	(*env)->ReleaseStringUTFChars(env, name, name_str);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1setStringSetting(JNIEnv *env, jobject this, jlong widget_ptr, jstring name, jstring value)
{
	WebKitWebView *webview = _PTR(widget_ptr);
	if (!WEBKIT_IS_WEB_VIEW(webview) || !value)
		return;
	const char *name_str = (*env)->GetStringUTFChars(env, name, NULL);
	const char *value_str = (*env)->GetStringUTFChars(env, value, NULL);
	g_object_set(webkit_web_view_get_settings(webview), name_str, value_str, NULL);
	(*env)->ReleaseStringUTFChars(env, value, value_str);
	(*env)->ReleaseStringUTFChars(env, name, name_str);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1setPreferredLanguage(JNIEnv *env, jclass class, jstring language)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW")) // no WebKit, see native_constructor()
		return;
	const char *language_str = (*env)->GetStringUTFChars(env, language, NULL);
	const char *languages[] = {language_str, NULL};
	webkit_web_context_set_preferred_languages(webkit_web_context_get_default(), languages);
	(*env)->ReleaseStringUTFChars(env, language, language_str);
}
