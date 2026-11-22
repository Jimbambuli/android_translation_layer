/* ATL's WebKit web process extension: WebViewClient.shouldInterceptRequest()
 *
 * WebKitGTK lets no one but the web process see (and change) a subresource request before it is sent: here,
 * each http(s) request of a page is passed to the app process (android_webkit_WebView.c, intercept_*), which asks
 * the app's WebViewClient. When the app answers with a WebResourceResponse, the request goes to the
 * atl-intercept: scheme instead, whose handler in the app process serves that response.
 * The line protocol: "<page id>\t<method>\t<url>\t<header>: <value>\x1f...\n", answered by "\n" (load as usual)
 * or "<atl-intercept URI>\n".
 *
 * androidx.webkit's WebMessageListener objects and document-start scripts: when a frame's window object is set up,
 * before the page's scripts run, "S\t<page id>\t<origin>\t<1 main frame/0>\n" asks for what belongs into this
 * frame, answered by "<count>\n" and count times "<length>\n<bytes>": first a function expression that installs the
 * listener objects (it gets the function that posts a message and returns the one that delivers a reply), then the
 * scripts, each run as a script of its own. */

#include <string.h>
#include <gio/gio.h>
#include <gio/gunixsocketaddress.h>
#include <webkit/webkit-web-process-extension.h>

static GSocketConnection *connection; // to the app process; NULL if it is not (or no longer) reachable
static GDataInputStream *replies;

static void append_header(const char *name, const char *value, gpointer line)
{
	g_string_append_printf(line, "%s: %s\x1f", name, value);
}

static void lost_connection(GError *error)
{
	g_warning("ATL web process extension: no shouldInterceptRequest() any more: %s", error ? error->message : "connection closed");
	g_clear_object(&replies);
	g_clear_object(&connection);
}

static gboolean send_request_cb(WebKitWebPage *page, WebKitURIRequest *request, WebKitURIResponse *redirected_response, gpointer user_data)
{
	const char *uri = webkit_uri_request_get_uri(request);
	if (!connection || redirected_response || !(g_str_has_prefix(uri, "http:") || g_str_has_prefix(uri, "https:")))
		return FALSE;
	/* the page's own document keeps its URL, and with it its origin: only subresources are asked */
	if (g_strcmp0(uri, webkit_web_page_get_uri(page)) == 0)
		return FALSE;
	const char *method = webkit_uri_request_get_http_method(request);
	g_autoptr(GString) line = g_string_new(NULL);
	g_string_append_printf(line, "%" G_GUINT64_FORMAT "\t%s\t%s\t", webkit_web_page_get_id(page), method ? method : "GET", uri);
	SoupMessageHeaders *headers = webkit_uri_request_get_http_headers(request);
	if (headers)
		soup_message_headers_foreach(headers, append_header, line);
	g_strdelimit(line->str, "\r\n", ' ');
	g_string_append_c(line, '\n');

	g_autoptr(GError) error = NULL;
	GOutputStream *out = g_io_stream_get_output_stream(G_IO_STREAM(connection));
	if (!g_output_stream_write_all(out, line->str, line->len, NULL, NULL, &error)) {
		lost_connection(error);
		return FALSE;
	}
	g_autofree char *reply = g_data_input_stream_read_line(replies, NULL, NULL, &error);
	if (!reply) {
		lost_connection(error);
		return FALSE;
	}
	if (reply[0])
		webkit_uri_request_set_uri(request, reply);
	return FALSE;
}

static char *read_record(GError **error)
{
	g_autofree char *length_line = g_data_input_stream_read_line(replies, NULL, NULL, error);
	if (!length_line)
		return NULL;
	gsize length = g_ascii_strtoull(length_line, NULL, 10);
	char *bytes = g_malloc(length + 1);
	if (length && !g_input_stream_read_all(G_INPUT_STREAM(replies), bytes, length, NULL, NULL, error)) {
		g_free(bytes);
		return NULL;
	}
	bytes[length] = '\0';
	return bytes;
}

static GHashTable *frames; // frame id -> WebKitFrame (not referenced, removed when it goes)
static guint64 next_document;

static void frame_gone(gpointer id, GObject *frame)
{
	g_hash_table_remove(frames, id);
}

struct post_target {
	WebKitWebPage *page;
	char *origin;
	gboolean main_frame;
	guint64 frame, document;
};

static void post_target_free(struct post_target *target)
{
	g_free(target->origin);
	g_free(target);
}

// a listener object's postMessage(): the origin is the frame's own, read before the page could run anything
static void post_message_cb(const char *name, const char *data, struct post_target *target)
{
	webkit_web_page_send_message_to_view(target->page, webkit_user_message_new("atl-wml", g_variant_new("(sssbtt)", name ? name : "", data ? data : "", target->origin, target->main_frame, target->frame, target->document)), NULL, NULL, NULL);
}

static void window_object_cleared_cb(WebKitScriptWorld *world, WebKitWebPage *page, WebKitFrame *frame, gpointer user_data)
{
	// a new document: what the frame's last one had is gone
	g_object_set_data(G_OBJECT(frame), "atl-deliver", NULL);
	if (!connection)
		return;
	g_autoptr(JSCContext) context = webkit_frame_get_js_context_for_script_world(frame, world);
	g_autoptr(JSCValue) origin_value = jsc_context_evaluate(context, "location.origin", -1);
	g_autofree char *origin = jsc_value_to_string(origin_value);
	gboolean main_frame = webkit_frame_is_main_frame(frame);
	g_autofree char *line = g_strdup_printf("S\t%" G_GUINT64_FORMAT "\t%s\t%d\n", webkit_web_page_get_id(page), origin ? origin : "null", main_frame);
	g_strdelimit(line + 2, "\r", ' ');
	g_autoptr(GError) error = NULL;
	GOutputStream *out = g_io_stream_get_output_stream(G_IO_STREAM(connection));
	if (!g_output_stream_write_all(out, line, strlen(line), NULL, NULL, &error)) {
		lost_connection(error);
		return;
	}
	g_autofree char *count_line = g_data_input_stream_read_line(replies, NULL, NULL, &error);
	if (!count_line) {
		lost_connection(error);
		return;
	}
	guint count = g_ascii_strtoull(count_line, NULL, 10);
	for (guint i = 0; i < count; i++) {
		g_autofree char *script = read_record(&error);
		if (!script) {
			lost_connection(error);
			return;
		}
		if (!script[0])
			continue;
		g_autoptr(JSCValue) result = jsc_context_evaluate(context, script, -1);
		g_autoptr(JSCException) exception = jsc_context_get_exception(context);
		if (exception) {
			g_autofree char *report = jsc_exception_report(exception);
			g_message("ATL web process extension: %s", report);
			jsc_context_clear_exception(context);
			continue;
		}
		if (i == 0 && jsc_value_is_function(result)) {
			guint64 id = webkit_frame_get_id(frame);
			struct post_target *target = g_new0(struct post_target, 1);
			target->page = page;
			target->origin = g_strdup(origin ? origin : "null");
			target->main_frame = main_frame;
			target->frame = id;
			target->document = ++next_document;
			g_autoptr(JSCValue) post = jsc_value_new_function(context, NULL, G_CALLBACK(post_message_cb), target, (GDestroyNotify)post_target_free, G_TYPE_NONE, 2, G_TYPE_STRING, G_TYPE_STRING);
			JSCValue *deliver = jsc_value_function_call(result, JSC_TYPE_VALUE, post, G_TYPE_NONE);
			g_object_set_data_full(G_OBJECT(frame), "atl-deliver", deliver, g_object_unref);
			g_object_set_data(G_OBJECT(frame), "atl-document", GUINT_TO_POINTER((guint)target->document));
			if (!g_hash_table_contains(frames, GSIZE_TO_POINTER(id))) {
				g_hash_table_insert(frames, GSIZE_TO_POINTER(id), frame);
				g_object_weak_ref(G_OBJECT(frame), frame_gone, GSIZE_TO_POINTER(id));
			}
		}
	}
}

// JavaScriptReplyProxy.postMessage(): to the frame, if it still shows the document whose object posted
static gboolean user_message_received_cb(WebKitWebPage *page, WebKitUserMessage *message, gpointer user_data)
{
	if (g_strcmp0(webkit_user_message_get_name(message), "atl-wml-reply"))
		return FALSE;
	GVariant *parameters = webkit_user_message_get_parameters(message);
	if (!parameters || !g_variant_is_of_type(parameters, G_VARIANT_TYPE("(ttss)")))
		return TRUE;
	guint64 frame_id, document;
	const char *name, *data;
	g_variant_get(parameters, "(tt&s&s)", &frame_id, &document, &name, &data);
	WebKitFrame *frame = g_hash_table_lookup(frames, GSIZE_TO_POINTER(frame_id));
	JSCValue *deliver = frame ? g_object_get_data(G_OBJECT(frame), "atl-deliver") : NULL;
	if (!deliver || GPOINTER_TO_UINT(g_object_get_data(G_OBJECT(frame), "atl-document")) != (guint)document)
		return TRUE;
	g_autoptr(JSCValue) result = jsc_value_function_call(deliver, G_TYPE_STRING, name, G_TYPE_STRING, data, G_TYPE_NONE);
	return TRUE;
}

static void page_created_cb(WebKitWebProcessExtension *extension, WebKitWebPage *page, gpointer user_data)
{
	g_signal_connect(page, "send-request", G_CALLBACK(send_request_cb), NULL);
	g_signal_connect(page, "user-message-received", G_CALLBACK(user_message_received_cb), NULL);
}

G_MODULE_EXPORT void webkit_web_process_extension_initialize_with_user_data(WebKitWebProcessExtension *extension, GVariant *user_data)
{
	g_autoptr(GSocketClient) client = g_socket_client_new();
	g_autoptr(GSocketAddress) address = g_unix_socket_address_new(g_variant_get_string(user_data, NULL));
	g_autoptr(GError) error = NULL;
	connection = g_socket_client_connect(client, G_SOCKET_CONNECTABLE(address), NULL, &error);
	if (!connection) {
		lost_connection(error);
		return;
	}
	/* a page waits for each answer: an app that does not answer at all costs it this long once */
	g_socket_set_timeout(g_socket_connection_get_socket(connection), 10);
	replies = g_data_input_stream_new(g_io_stream_get_input_stream(G_IO_STREAM(connection)));
	g_signal_connect(extension, "page-created", G_CALLBACK(page_created_cb), NULL);
	frames = g_hash_table_new(NULL, NULL);
	g_signal_connect(webkit_script_world_get_default(), "window-object-cleared", G_CALLBACK(window_object_cleared_cb), NULL);
}
