/* ATL's WebKit web process extension: WebViewClient.shouldInterceptRequest()
 *
 * WebKitGTK lets no one but the web process see (and change) a subresource request before it is sent: here,
 * each http(s) request of a page is passed to the app process (android_webkit_WebView.c, intercept_*), which asks
 * the app's WebViewClient. When the app answers with a WebResourceResponse, the request goes to the
 * atl-intercept: scheme instead, whose handler in the app process serves that response.
 * The line protocol: "<page id>\t<method>\t<url>\t<header>: <value>\x1f...\n", answered by "\n" (load as usual)
 * or "<atl-intercept URI>\n". */

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

static void page_created_cb(WebKitWebProcessExtension *extension, WebKitWebPage *page, gpointer user_data)
{
	g_signal_connect(page, "send-request", G_CALLBACK(send_request_cb), NULL);
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
}
