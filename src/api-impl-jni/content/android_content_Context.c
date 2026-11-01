#define _GNU_SOURCE

#include <gio/gunixinputstream.h>
#include <gtk/gtk.h>
#include <libportal/portal.h>
#include <math.h>
#include <string.h>
#ifdef XDP_TYPE_INPUT_CAPTURE_SESSION // libportal >= 0.8
	#include <libportal/settings.h>
#endif

#include "portal-email.h"
#include "portal-openuri.h"
#include "unifiedpush-connector.h"
#include "unifiedpush-distributor.h"

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_content_Context.h"

#include "android_content_Context.h"
#include "../app/android_app_Activity.h"

static void monitor_changed_cb(GdkSurface *surface, GdkMonitor *monitor, jobject configuration)
{
	JNIEnv *env = get_jni_env();
	GdkRectangle geometry;
	gdk_monitor_get_geometry(monitor, &geometry);
	if (!configuration)
		configuration = _GET_STATIC_OBJ_FIELD(_CACHED_CLASS(Context), "sys_config", "Landroid/content/res/Configuration;");

	/* like Android, the screen size the app sees is the size of its window (which the window size
	 * option sets, and which DisplayMetrics reports as well), at most the monitor's size; without
	 * it, the w<N>dp resources of the monitor width were used in a narrow window */
	jclass display = (*env)->FindClass(env, "android/view/Display");
	int window_width = (*env)->GetStaticIntField(env, display, _STATIC_FIELD_ID(display, "window_width", "I"));
	int window_height = (*env)->GetStaticIntField(env, display, _STATIC_FIELD_ID(display, "window_height", "I"));
	_SET_INT_FIELD(configuration, "screenWidthDp", window_width > 0 ? MIN(window_width, geometry.width) : geometry.width);
	_SET_INT_FIELD(configuration, "screenHeightDp", window_height > 0 ? MIN(window_height, geometry.height) : geometry.height);
	/* without it, no resources with an sw<N>dp qualifier match, which tablet layouts rely on */
	_SET_INT_FIELD(configuration, "smallestScreenWidthDp", MIN(geometry.width, geometry.height));
	/* the layout direction bits come from the locale (Configuration.setLayoutDirection()) */
	jint layout_dir = _GET_INT_FIELD(configuration, "screenLayout") & /*SCREENLAYOUT_LAYOUTDIR_MASK*/ 0xC0;
	if (geometry.width >= 800 && geometry.height >= 800)
		_SET_INT_FIELD(configuration, "screenLayout", /*SCREENLAYOUT_SIZE_LARGE*/ 0x03 | layout_dir);
	else
		_SET_INT_FIELD(configuration, "screenLayout", /*SCREENLAYOUT_SIZE_NORMAL*/ 0x02 | layout_dir);
}

/* The appearance of the desktop, from the XDG settings portal (namespace org.freedesktop.appearance) and
 * followed while the app runs, like the system settings on Android: color-scheme is the night mode
 * (UiModeManagerService: UI_MODE_NIGHT_YES for dark, otherwise NO), contrast the contrast level
 * (UiModeManager.getContrast(): 1 for high contrast) and accent-color the color the Material You palettes
 * are made from (android.atl.ATLThemeOverlay; out of range: none). Without the portal, GTK's
 * gtk-application-prefer-dark-theme decides, otherwise it is light. */
static guint32 color_scheme = 0; // 0: no preference, 1: prefer dark, 2: prefer light
static guint32 contrast = 0;     // 0: no preference, 1: high contrast
static guint32 accent_color = 0; // ARGB, 0: none
static gboolean appearance_from_portal = FALSE;

static jint appearance_ui_mode_night(void)
{
	gboolean dark = color_scheme == 1;
	if (!appearance_from_portal)
		g_object_get(gtk_settings_get_default(), "gtk-application-prefer-dark-theme", &dark, NULL);
	return dark ? /*UI_MODE_NIGHT_YES*/ 0x20 : /*UI_MODE_NIGHT_NO*/ 0x10;
}

static jfloat appearance_contrast(void)
{
	return contrast == 1 ? 1.0f : 0.0f;
}

#ifdef XDP_TYPE_INPUT_CAPTURE_SESSION // libportal >= 0.8
static gboolean appearance_setting(const char *namespace, const char *key, GVariant *value)
{
	if (strcmp(namespace, "org.freedesktop.appearance"))
		return FALSE;
	if (!strcmp(key, "color-scheme") && g_variant_is_of_type(value, G_VARIANT_TYPE_UINT32)) {
		color_scheme = g_variant_get_uint32(value);
		g_object_set(gtk_settings_get_default(), "gtk-application-prefer-dark-theme", color_scheme == 1, NULL);
	} else if (!strcmp(key, "contrast") && g_variant_is_of_type(value, G_VARIANT_TYPE_UINT32)) {
		contrast = g_variant_get_uint32(value);
	} else if (!strcmp(key, "accent-color") && g_variant_is_of_type(value, G_VARIANT_TYPE("(ddd)"))) {
		double r, g, b;
		g_variant_get(value, "(ddd)", &r, &g, &b);
		if (r >= 0 && r <= 1 && g >= 0 && g <= 1 && b >= 0 && b <= 1)
			accent_color = 0xff000000 | (guint32)lround(r * 255) << 16 | (guint32)lround(g * 255) << 8 | (guint32)lround(b * 255);
		else
			accent_color = 0;
	} else {
		return FALSE;
	}
	appearance_from_portal = TRUE;
	return TRUE;
}

/* a setting changed while the app runs: a change of the global configuration, as on Android */
static void settings_changed_cb(XdpSettings *xdp_settings, gchar *namespace, gchar *key, GVariant *value, gpointer user_data)
{
	if (!appearance_setting(namespace, key, value))
		return;
	JNIEnv *env = get_jni_env();
	J__ActivityThread__setNightMode(env, appearance_ui_mode_night(), appearance_contrast());
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	/* the dynamic colors depend on the contrast as well */
	if (!strcmp(key, "accent-color") || !strcmp(key, "contrast")) {
		J__ATLThemeOverlay__setAccentColor(env, accent_color);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
	}
	activity_ensure_configuration();
}

static XdpSettings *xdp_settings = NULL;

static void appearance_read(const char *key)
{
	GVariant *value = xdp_settings_read_value(xdp_settings, "org.freedesktop.appearance", key, NULL, NULL);
	if (value) {
		appearance_setting("org.freedesktop.appearance", key, value);
		g_variant_unref(value);
	}
}
#endif

/* the window was resized: take the screen size of the configuration (and the window size that
 * DisplayMetrics reports) from its new size, like at the start from the window size option */
JNIEXPORT void JNICALL Java_android_content_Context_native_1updateConfigFromWindow(JNIEnv *env, jclass this, jlong window_ptr, jobject config)
{
	GtkWidget *window = _PTR(window_ptr);
	jclass display = (*env)->FindClass(env, "android/view/Display");
	(*env)->SetStaticIntField(env, display, _STATIC_FIELD_ID(display, "window_width", "I"), gtk_widget_get_width(window));
	(*env)->SetStaticIntField(env, display, _STATIC_FIELD_ID(display, "window_height", "I"), gtk_widget_get_height(window));
	GdkSurface *surface = gtk_native_get_surface(GTK_NATIVE(window));
	GdkMonitor *monitor = surface ? gdk_display_get_monitor_at_surface(gtk_widget_get_display(window), surface) : NULL;
	if (!monitor)
		monitor = g_list_model_get_item(gdk_display_get_monitors(gtk_widget_get_display(window)), 0);
	else
		g_object_ref(monitor);
	monitor_changed_cb(NULL, monitor, config);
	g_object_unref(monitor);
}

/* the highest scale of the monitors, since the window can be moved to any of them */
JNIEXPORT jdouble JNICALL Java_android_content_Context_native_1getMonitorScale(JNIEnv *env, jclass this)
{
	GListModel *monitors = gdk_display_get_monitors(gdk_display_get_default());
	double scale = 1;
	for (guint i = 0; i < g_list_model_get_n_items(monitors); i++) {
		GdkMonitor *m = g_list_model_get_item(monitors, i);
		scale = MAX(scale, gdk_monitor_get_scale(m));
		g_object_unref(m);
	}
	return scale;
}

JNIEXPORT void JNICALL Java_android_content_Context_native_1updateConfig(JNIEnv *env, jclass this, jobject config)
{
	GdkDisplay *display = gdk_display_get_default();
	GdkMonitor *monitor = g_list_model_get_item(gdk_display_get_monitors(display), 0);
	monitor_changed_cb(NULL, monitor, config);

#ifdef XDP_TYPE_INPUT_CAPTURE_SESSION // libportal >= 0.8
	if (!xdp_settings) {
		GError *error = NULL;
		XdpPortal *portal = xdp_portal_initable_new(&error);
		if (portal) {
			xdp_settings = xdp_portal_get_settings(portal);
			g_object_unref(portal);
			g_signal_connect(xdp_settings, "changed", G_CALLBACK(settings_changed_cb), NULL);
		} else {
			printf("xdp_portal_initable_new failed: %s\n", error->message);
			g_error_free(error);
		}
	}
	if (xdp_settings) {
		appearance_read("color-scheme");
		appearance_read("contrast");
		appearance_read("accent-color");
	}
#endif
	_SET_INT_FIELD(config, "uiMode", (_GET_INT_FIELD(config, "uiMode") & ~/*UI_MODE_NIGHT_MASK*/ 0x30) | appearance_ui_mode_night());
	jclass ui_mode_manager = (*env)->FindClass(env, "android/app/UiModeManager");
	(*env)->SetStaticFloatField(env, ui_mode_manager, _STATIC_FIELD_ID(ui_mode_manager, "sContrast", "F"), appearance_contrast());
	jclass theme_overlay = (*env)->FindClass(env, "android/atl/ATLThemeOverlay");
	_SET_STATIC_INT_FIELD(theme_overlay, "sAccentColor", accent_color);
}

void update_config_for_window(GtkWindow *window)
{
	GdkSurface *surface = gtk_native_get_surface(GTK_NATIVE(window));
	monitor_changed_cb(surface, gdk_display_get_monitor_at_surface(gdk_display_get_default(), surface), NULL);
	g_signal_connect(surface, "enter-monitor", G_CALLBACK(monitor_changed_cb), NULL);
}

JNIEXPORT void JNICALL Java_android_content_Context_nativeOpenFile(JNIEnv *env, jclass class, jint fd)
{
	GDBusConnection *connection = g_bus_get_sync(G_BUS_TYPE_SESSION, NULL, NULL);
	OpenURI *openuri = open_uri_proxy_new_sync(connection, 0, "org.freedesktop.portal.Desktop", "/org/freedesktop/portal/desktop", NULL, NULL);
	GVariantBuilder opt_builder;
	g_variant_builder_init(&opt_builder, G_VARIANT_TYPE_VARDICT);
	GUnixFDList *fd_list = g_unix_fd_list_new_from_array(&fd, 1);
	open_uri_call_open_file_sync(openuri, "", g_variant_new("h", 0), g_variant_builder_end(&opt_builder), fd_list, NULL, NULL, NULL, NULL);
	g_object_unref(fd_list);
	g_object_unref(openuri);
	g_object_unref(connection);
}

char *fd_get_path(int fd)
{
	char *fdlink = g_strdup_printf("/proc/self/fd/%d", fd);
	char *buf = g_malloc(PATH_MAX);

	ssize_t len = readlink(fdlink, buf, PATH_MAX - 1);
	g_free(fdlink);
	if (len < 0)
		return NULL;

	buf[len] = '\0';
	return buf;
}

extern GtkWindow *window;

static void share_save_callback(GObject *dialog, GAsyncResult *result, gpointer user_data)
{
	int fd = GPOINTER_TO_INT(user_data);
	GFile *file = gtk_file_dialog_save_finish(GTK_FILE_DIALOG(dialog), result, NULL);
	if (file) {
		GError *error = NULL;
		lseek(fd, 0, SEEK_SET);
		GInputStream *in = g_unix_input_stream_new(fd, TRUE);
		GFileOutputStream *out = g_file_replace(file, NULL, FALSE, G_FILE_CREATE_NONE, NULL, &error);
		if (out) {
			g_output_stream_splice(G_OUTPUT_STREAM(out), in, G_OUTPUT_STREAM_SPLICE_CLOSE_TARGET, NULL, &error);
			g_object_unref(out);
		}
		if (error) {
			fprintf(stderr, "share: saving the file failed: %s\n", error->message);
			g_error_free(error);
		}
		g_object_unref(in);
		g_object_unref(file);
	} else {
		close(fd);
	}
	g_object_unref(dialog);
}

enum {
	SHARE_CANCEL,
	SHARE_COPY,
	SHARE_EMAIL,
	SHARE_SAVE,
};

/* runs the share target the user picked in the share sheet, after it was closed */
static void share_sheet_destroyed(GtkWindow *sheet, gpointer text_jstr)
{
	JNIEnv *env = get_jni_env();
	int button_id = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(sheet), "target"));
	const char *text = NULL;
	if (text_jstr)
		text = (*env)->GetStringUTFChars(env, text_jstr, NULL);
	int fd = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(sheet), "fd"));
	printf("share_sheet_destroyed: button_id=%d, text=%s, fd=%d\n", button_id, text, fd);
	if (button_id == SHARE_COPY) {
		printf("Copy\n");
		GdkClipboard *clipboard = gdk_display_get_clipboard(gtk_root_get_display(GTK_ROOT(window)));
		if (fd) {
			char *path = fd_get_path(fd);
			GFile *file = g_file_new_for_path(path);
			gdk_clipboard_set(clipboard, G_TYPE_FILE, file);
			g_object_unref(file);
			g_free(path);
		}
		if (text) {
			gdk_clipboard_set_text(clipboard, text);
		}
	} else if (button_id == SHARE_EMAIL) {
		printf("Email\n");
		GDBusConnection *connection = g_bus_get_sync(G_BUS_TYPE_SESSION, NULL, NULL);
		Email *email = email_proxy_new_sync(connection, 0, "org.freedesktop.portal.Desktop", "/org/freedesktop/portal/desktop", NULL, NULL);
		GUnixFDList *fd_list = g_unix_fd_list_new();
		GVariantBuilder opt_builder;
		g_variant_builder_init(&opt_builder, G_VARIANT_TYPE_VARDICT);
		if (text_jstr) {
			const char *text = (*env)->GetStringUTFChars(env, text_jstr, NULL);
			g_variant_builder_add(&opt_builder, "{sv}", "body", g_variant_new_string(text));
			(*env)->ReleaseStringUTFChars(env, text_jstr, text);
		}
		if (fd) {
			int fd_handle = g_unix_fd_list_append(fd_list, fd, NULL);
			GVariantBuilder fd_array_builder;
			g_variant_builder_init(&fd_array_builder, G_VARIANT_TYPE("ah"));
			g_variant_builder_add(&fd_array_builder, "h", fd_handle);
			g_variant_builder_add(&opt_builder, "{sv}", "attachment_fds", g_variant_builder_end(&fd_array_builder));
			/* The `attachment_fds` option is ignored by most email applications, so we also set the file path as subject */
			char *path = fd_get_path(fd);
			g_variant_builder_add(&opt_builder, "{sv}", "subject", g_variant_new_string(path));
			g_free(path);
		}
		email_call_compose_email_sync(email, "", g_variant_builder_end(&opt_builder), fd_list, NULL, NULL, NULL, NULL);
		g_object_unref(fd_list);
		g_object_unref(email);
		g_object_unref(connection);
	} else if (button_id == SHARE_SAVE) {
		GtkFileDialog *save_dialog = gtk_file_dialog_new();
		char *path = fd_get_path(fd);
		if (path) {
			char *name = g_path_get_basename(path);
			gtk_file_dialog_set_initial_name(save_dialog, name);
			g_free(name);
			g_free(path);
		}
		gtk_file_dialog_save(save_dialog, window, NULL, share_save_callback, GINT_TO_POINTER(dup(fd)));
	}
	if (text_jstr) {
		(*env)->ReleaseStringUTFChars(env, text_jstr, text);
		_UNREF(text_jstr);
	}
	if (fd)
		close(fd);
}

static void share_target_clicked(GtkButton *button, GtkWindow *sheet)
{
	g_object_set_data(G_OBJECT(sheet), "target", g_object_get_data(G_OBJECT(button), "target"));
	gtk_window_close(sheet);
}

static void share_sheet_add_target(GtkWidget *targets, GtkWindow *sheet, int target, GIcon *icon, const char *label)
{
	GtkWidget *box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 6);
	GtkWidget *image = gtk_image_new_from_gicon(icon);
	gtk_image_set_pixel_size(GTK_IMAGE(image), 40);
	gtk_box_append(GTK_BOX(box), image);
	GtkWidget *text = gtk_label_new(label);
	gtk_label_set_wrap(GTK_LABEL(text), TRUE);
	gtk_label_set_justify(GTK_LABEL(text), GTK_JUSTIFY_CENTER);
	gtk_label_set_max_width_chars(GTK_LABEL(text), 12);
	gtk_box_append(GTK_BOX(box), text);
	GtkWidget *button = gtk_button_new();
	gtk_button_set_child(GTK_BUTTON(button), box);
	gtk_widget_add_css_class(button, "flat");
	gtk_widget_set_size_request(button, 96, -1);
	g_object_set_data(G_OBJECT(button), "target", GINT_TO_POINTER(target));
	g_signal_connect(button, "clicked", G_CALLBACK(share_target_clicked), sheet);
	gtk_box_append(GTK_BOX(targets), button);
}

static const char *jstring_or(JNIEnv *env, jobjectArray labels, int index, const char *fallback, char **to_free)
{
	jstring label = labels ? (*env)->GetObjectArrayElement(env, labels, index) : NULL;
	if (!label)
		return fallback;
	const char *utf = (*env)->GetStringUTFChars(env, label, NULL);
	*to_free = g_strdup(utf);
	(*env)->ReleaseStringUTFChars(env, label, utf);
	return *to_free;
}

/* The XDG specification does not provide anything comparable to the Android share API yet. Therefore, we provide
 * a share sheet like Android's chooser allowing the user to copy to clipboard, send per mail using the
 * composeEmail portal, or save a shared file. The labels (title, copy, save) come from the framework
 * resources, so they are in the app's language; the mail target carries the name of the mail application.
 */
JNIEXPORT void JNICALL Java_android_content_Context_nativeShareFile(JNIEnv *env, jclass class, jstring text_jstr, jint fd, jobjectArray labels)
{
	char *title_free = NULL, *copy_free = NULL, *save_free = NULL;
	const char *title = jstring_or(env, labels, 0, "Share", &title_free);
	const char *copy = jstring_or(env, labels, 1, "Copy", &copy_free);
	const char *save = jstring_or(env, labels, 2, "Save", &save_free);

	GtkWidget *sheet = gtk_window_new();
	gtk_window_set_title(GTK_WINDOW(sheet), title);
	gtk_window_set_transient_for(GTK_WINDOW(sheet), window);
	gtk_window_set_modal(GTK_WINDOW(sheet), TRUE);
	gtk_window_set_resizable(GTK_WINDOW(sheet), FALSE);
	GtkWidget *content = gtk_box_new(GTK_ORIENTATION_VERTICAL, 12);
	gtk_widget_set_margin_start(content, 24);
	gtk_widget_set_margin_end(content, 24);
	gtk_widget_set_margin_bottom(content, 20);

	/* the heading is the title bar: the sheet is a card with rounded corners and a shadow, like
	 * Android's chooser, and can be moved by it */
	GtkWidget *header = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 0);
	gtk_widget_set_margin_start(header, 24);
	gtk_widget_set_margin_end(header, 12);
	gtk_widget_set_margin_top(header, 12);
	gtk_widget_set_margin_bottom(header, 4);
	GtkWidget *heading = gtk_label_new(title);
	gtk_widget_add_css_class(heading, "title-3");
	gtk_label_set_xalign(GTK_LABEL(heading), 0);
	gtk_widget_set_hexpand(heading, TRUE);
	gtk_box_append(GTK_BOX(header), heading);
	GtkWidget *close = gtk_button_new_from_icon_name("window-close-symbolic");
	gtk_widget_add_css_class(close, "flat");
	gtk_widget_add_css_class(close, "circular");
	gtk_widget_set_valign(close, GTK_ALIGN_CENTER);
	g_signal_connect_swapped(close, "clicked", G_CALLBACK(gtk_window_close), sheet);
	gtk_box_append(GTK_BOX(header), close);
	gtk_window_set_titlebar(GTK_WINDOW(sheet), header);

	/* what is shared, like the preview at the top of Android's chooser */
	char *preview = NULL;
	if (fd != -1) {
		char *path = fd_get_path(fd);
		preview = path ? g_path_get_basename(path) : NULL;
		g_free(path);
		g_object_set_data(G_OBJECT(sheet), "fd", GINT_TO_POINTER(dup(fd)));
	} else if (text_jstr) {
		const char *text = (*env)->GetStringUTFChars(env, text_jstr, NULL);
		preview = g_strdup(text);
		(*env)->ReleaseStringUTFChars(env, text_jstr, text);
	}
	if (preview) {
		GtkWidget *label = gtk_label_new(preview);
		gtk_label_set_wrap(GTK_LABEL(label), TRUE);
		gtk_label_set_lines(GTK_LABEL(label), 3);
		gtk_label_set_ellipsize(GTK_LABEL(label), PANGO_ELLIPSIZE_END);
		gtk_label_set_max_width_chars(GTK_LABEL(label), 48);
		gtk_label_set_width_chars(GTK_LABEL(label), 36);
		gtk_label_set_xalign(GTK_LABEL(label), 0);
		gtk_widget_set_margin_start(label, 12);
		gtk_widget_set_margin_end(label, 12);
		gtk_widget_set_margin_top(label, 10);
		gtk_widget_set_margin_bottom(label, 10);
		GtkWidget *card = gtk_frame_new(NULL);
		gtk_widget_add_css_class(card, "view");
		gtk_frame_set_child(GTK_FRAME(card), label);
		gtk_box_append(GTK_BOX(content), card);
		g_free(preview);
	}

	GtkWidget *targets = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 8);
	GIcon *icon = g_themed_icon_new("edit-copy");
	share_sheet_add_target(targets, GTK_WINDOW(sheet), SHARE_COPY, icon, copy);
	g_object_unref(icon);
	GAppInfo *mail = g_app_info_get_default_for_uri_scheme("mailto");
	if (mail) {
		icon = g_app_info_get_icon(mail);
		icon = icon ? g_object_ref(icon) : g_themed_icon_new("mail-send");
		share_sheet_add_target(targets, GTK_WINDOW(sheet), SHARE_EMAIL, icon, g_app_info_get_display_name(mail));
		g_object_unref(icon);
		g_object_unref(mail);
	}
	if (fd != -1) {
		icon = g_themed_icon_new("document-save");
		share_sheet_add_target(targets, GTK_WINDOW(sheet), SHARE_SAVE, icon, save);
		g_object_unref(icon);
	}
	gtk_box_append(GTK_BOX(content), targets);
	gtk_window_set_child(GTK_WINDOW(sheet), content);

	GtkEventController *shortcuts = gtk_shortcut_controller_new();
	gtk_shortcut_controller_add_shortcut(GTK_SHORTCUT_CONTROLLER(shortcuts),
	                                     gtk_shortcut_new(gtk_keyval_trigger_new(GDK_KEY_Escape, 0), gtk_named_action_new("window.close")));
	gtk_widget_add_controller(sheet, shortcuts);

	g_signal_connect(sheet, "destroy", G_CALLBACK(share_sheet_destroyed), _REF(text_jstr));
	gtk_window_present(GTK_WINDOW(sheet));
	g_free(title_free);
	g_free(copy_free);
	g_free(save_free);
}

static void on_bus_acquired(GDBusConnection *connection, const char *name, gpointer user_data)
{
	Connector1 *connector1 = user_data;
	g_dbus_interface_skeleton_export(G_DBUS_INTERFACE_SKELETON(connector1),
	                                 connection, "/org/unifiedpush/Connector", NULL);
}

static gboolean on_new_endpoint(Connector1 *connector, GDBusMethodInvocation *invocation, gpointer user_data)
{
	GVariant *parameters = g_dbus_method_invocation_get_parameters(invocation);
	const char *token;
	g_variant_get_child(parameters, 0, "s", &token);
	const char *endpoint;
	g_variant_get_child(parameters, 1, "s", &endpoint);
	connector1_complete_new_endpoint(connector, invocation);

	JNIEnv *env = get_jni_env();
	jobject intent = J_new__Intent(env);
	_SET_OBJ_FIELD(intent, "action", "Ljava/lang/String;", _JSTRING("org.unifiedpush.android.connector.NEW_ENDPOINT"));
	J__Intent__putExtraCharSequence(env, intent, _JSTRING("token"), _JSTRING(token));
	J__Intent__putExtraCharSequence(env, intent, _JSTRING("endpoint"), _JSTRING(endpoint));

	jobject context = _GET_STATIC_OBJ_FIELD(_CACHED_CLASS(Context), "this_application", "Landroid/app/Application;");
	J__Context__sendBroadcast(env, context, intent);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	return TRUE;
}

static gboolean on_message(Connector1 *connector, GDBusMethodInvocation *invocation, gpointer user_data)
{
	GVariant *parameters = g_dbus_method_invocation_get_parameters(invocation);
	const char *token;
	g_variant_get_child(parameters, 0, "s", &token);
	gsize size;
	const int8_t *message = g_variant_get_fixed_array(g_variant_get_child_value(parameters, 1), &size, 1);
	connector1_complete_message(connector, invocation);

	JNIEnv *env = get_jni_env();
	jobject intent = J_new__Intent(env);
	_SET_OBJ_FIELD(intent, "action", "Ljava/lang/String;", _JSTRING("org.unifiedpush.android.connector.MESSAGE"));
	J__Intent__putExtraCharSequence(env, intent, _JSTRING("token"), _JSTRING(token));
	jbyteArray bytesMessage = (*env)->NewByteArray(env, size);
	(*env)->SetByteArrayRegion(env, bytesMessage, 0, size, message);
	J__Intent__putExtraByteArray(env, intent, _JSTRING("bytesMessage"), bytesMessage);

	jobject context = _GET_STATIC_OBJ_FIELD(_CACHED_CLASS(Context), "this_application", "Landroid/app/Application;");
	J__Context__sendBroadcast(env, context, intent);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	return TRUE;
}

JNIEXPORT void JNICALL Java_android_content_Context_nativeExportUnifiedPush(JNIEnv *env, jclass this, jstring application_jstr)
{
	const char *application = (*env)->GetStringUTFChars(env, application_jstr, NULL);

	Connector1 *connector1 = connector1_skeleton_new();
	g_signal_connect(connector1, "handle-new-endpoint", G_CALLBACK(on_new_endpoint), NULL);
	g_signal_connect(connector1, "handle-message", G_CALLBACK(on_message), NULL);
	g_bus_own_name(G_BUS_TYPE_SESSION, application, G_BUS_NAME_OWNER_FLAGS_NONE,
	               on_bus_acquired, NULL, NULL, connector1, NULL);
	(*env)->ReleaseStringUTFChars(env, application_jstr, application);
}

JNIEXPORT void JNICALL Java_android_content_Context_nativeRegisterUnifiedPush(JNIEnv *env, jclass this, jstring token_jstr, jstring application_jstr)
{
	const char *token = (*env)->GetStringUTFChars(env, token_jstr, NULL);
	const char *application = (*env)->GetStringUTFChars(env, application_jstr, NULL);

	GDBusConnection *connection = g_bus_get_sync(G_BUS_TYPE_SESSION, NULL, NULL);
	Distributor1 *distributor1 = distributor1_proxy_new_sync(connection, 0, "org.unifiedpush.Distributor.kde", "/org/unifiedpush/Distributor", NULL, NULL);
	GError *error = NULL;
	distributor1_call_register(distributor1, application, token, "", NULL, NULL, &error);
	if (error) {
		printf("nativeRegisterUnifiedPush: error=%s\n", error->message);
		g_error_free(error);
	}
	g_object_unref(distributor1);
	g_object_unref(connection);

	(*env)->ReleaseStringUTFChars(env, token_jstr, token);
	(*env)->ReleaseStringUTFChars(env, application_jstr, application);
}

JNIEXPORT void JNICALL Java_android_content_Context_nativeStartExternalService(JNIEnv *env, jclass this, jobject intent)
{
	GVariant *variant = intent_serialize(env, intent);
	jstring package_jstr = _GET_OBJ_FIELD(intent, "packageName", "Ljava/lang/String;");
	const char *package = (*env)->GetStringUTFChars(env, package_jstr, NULL);
	char *object_path = g_strdup_printf("/%s", package);
	g_strdelimit(object_path, ".", '/');
	GDBusConnection *connection = g_bus_get_sync(G_BUS_TYPE_SESSION, NULL, NULL);
	GActionGroup *action_group = G_ACTION_GROUP(g_dbus_action_group_get(connection, package, object_path));
	g_action_group_activate_action(action_group, "startService", variant);
	g_object_unref(action_group);
	g_object_unref(connection);
	g_free(object_path);
	(*env)->ReleaseStringUTFChars(env, package_jstr, package);
}
