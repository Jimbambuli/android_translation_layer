#include <gio/gdesktopappinfo.h>
#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"

#include "android_app_Activity.h"
#include "mpris-dbus.h"

#include "../generated_headers/android_app_NotificationManager.h"

#define MPRIS_BUS_NAME_PREFIX "org.mpris.MediaPlayer2."
#define MPRIS_OBJECT_NAME     "/org/mpris/MediaPlayer2"

/* the apps notifications on the desktop: like AOSPs NotificationManagerService, which cancels all
   notifications of a package that is force-stopped (ACTION_PACKAGE_RESTARTED), they go when the app ends */
static GHashTable *posted_notifications = NULL;

/* We queue up notification updates in pending_notifications to make sure that there is at least 200ms
   delay between consecutive updates. This prevents dynamic notification updated from arriving in wrong
   order at the desktop environment.
   Normally 20ms should be enough to prevent notification update order issues, but we use a 10x larger value
   to be safe and 200ms should be more than enough as notification update interval. */
static GHashTable *pending_notifications = NULL;
static GMutex pending_notifications_mutex = {0};
static GSource *send_notifcation_timer = NULL;

/* The notification's id at the desktop: the portal keeps one list per Flatpak, which every app of the
   APK Starter shares, so the id names the app (its GApplication id) besides Android's id. */
static char *desktop_id(int id)
{
	return g_strdup_printf("%s.%d", g_application_get_application_id(g_application_get_default()), id);
}

/* GIO's portal backend sends notifications but leaves the clicks (ActionInvoked) to the app: activate the
   action of the button or of the notification here, as GIO's other backends do. Without an action of the
   app, the click brings its window to the front. */
static void portal_action_invoked(GDBusConnection *connection, const char *sender, const char *path, const char *iface,
                                  const char *signal, GVariant *parameters, gpointer user_data)
{
	GApplication *app = g_application_get_default();
	const char *id, *action;
	g_autoptr(GVariant) args = NULL;
	g_autofree char *prefix = g_strdup_printf("%s.", g_application_get_application_id(app));
	g_variant_get(parameters, "(&s&s@av)", &id, &action, &args);
	if (!g_str_has_prefix(id, prefix))
		return;
	g_autoptr(GVariant) target = NULL;
	for (gsize i = 0; i < g_variant_n_children(args); i++) {
		g_autoptr(GVariant) arg = g_variant_get_child_value(args, i);
		g_autoptr(GVariant) value = g_variant_get_variant(arg);
		const char *token;
		if (g_variant_is_of_type(value, G_VARIANT_TYPE_VARDICT)) {
			/* platform data: the activation token lets the window come to the front on Wayland */
			GtkWindow *active = gtk_application_get_active_window(GTK_APPLICATION(app));
			if (active && g_variant_lookup(value, "activation-token", "&s", &token))
				gtk_window_set_startup_id(active, token);
		} else if (!target) {
			target = g_steal_pointer(&value);
		}
	}
	if (g_str_has_prefix(action, "app.") && g_action_group_has_action(G_ACTION_GROUP(app), action + 4))
		g_action_group_activate_action(G_ACTION_GROUP(app), action + 4, target);
	else
		activity_window_present();
}

/* GNOME's and KDE's portal backends activate a notification's action with org.freedesktop.Application
   ActivateAction on the bus name of the Flatpak, not of the app (each app of the APK Starter has its own
   GApplication id under it). One running app owns the Flatpak's name and passes the call on to the app the
   action's intent names (its last field), which answers ActivateAction on its own name. */
static const char application_xml[] =
    "<node><interface name='org.freedesktop.Application'>"
    "<method name='Activate'><arg type='a{sv}' direction='in'/></method>"
    "<method name='Open'><arg type='as' direction='in'/><arg type='a{sv}' direction='in'/></method>"
    "<method name='ActivateAction'><arg type='s' direction='in'/><arg type='av' direction='in'/><arg type='a{sv}' direction='in'/></method>"
    "</interface></node>";

static char *object_path_for(const char *bus_name)
{
	char *path = g_strconcat("/", bus_name, NULL);
	g_strdelimit(path, ".", '/');
	g_strdelimit(path, "-", '_');
	return path;
}

static void flatpak_application_call(GDBusConnection *connection, const char *sender, const char *path, const char *iface,
                                     const char *method, GVariant *parameters, GDBusMethodInvocation *invocation, gpointer user_data)
{
	const char *target = NULL;
	if (!strcmp(method, "ActivateAction")) {
		g_autoptr(GVariant) args = g_variant_get_child_value(parameters, 1);
		if (g_variant_n_children(args) > 0) {
			g_autoptr(GVariant) arg = g_variant_get_child_value(args, 0);
			g_autoptr(GVariant) intent = g_variant_get_variant(arg);
			if (g_variant_is_of_type(intent, G_VARIANT_TYPE(INTENT_G_VARIANT_TYPE_STRING)))
				g_variant_get_child(intent, 4, "&s", &target);
		}
	}
	if (target && *target) {
		g_autofree char *target_path = object_path_for(target);
		g_dbus_connection_call(connection, target, target_path, iface, method, parameters, NULL,
		                       G_DBUS_CALL_FLAGS_NONE, -1, NULL, NULL, NULL);
	} else {
		activity_window_present();
	}
	g_dbus_method_invocation_return_value(invocation, NULL);
}

static void own_flatpak_name(GDBusConnection *connection)
{
	const char *flatpak_id = g_getenv("FLATPAK_ID");
	if (!flatpak_id)
		return;
	static const GDBusInterfaceVTable vtable = {flatpak_application_call};
	g_autoptr(GDBusNodeInfo) info = g_dbus_node_info_new_for_xml(application_xml, NULL);
	g_autofree char *path = object_path_for(flatpak_id);
	g_dbus_connection_register_object(connection, path, info->interfaces[0], &vtable, NULL, NULL, NULL);
	/* queued: when the owning app ends, the next running one takes over */
	g_bus_own_name_on_connection(connection, flatpak_id, G_BUS_NAME_OWNER_FLAGS_NONE, NULL, NULL, NULL, NULL);
}

static gboolean send_notifcation_func(GSource *send_notifcation_timer, GSourceFunc callback, gpointer user_data)
{
	GApplication *app = g_application_get_default();
	GHashTableIter iter;
	gpointer key, value;
	gboolean notification_sent = FALSE;

	g_mutex_lock(&pending_notifications_mutex);
	g_hash_table_iter_init(&iter, pending_notifications);
	while (g_hash_table_iter_next(&iter, &key, &value)) {
		char *id_string = desktop_id(GPOINTER_TO_INT(key));
		if (value)
			g_application_send_notification(app, id_string, value);
		else
			g_application_withdraw_notification(app, id_string);
		g_free(id_string);
		g_hash_table_iter_remove(&iter);
		notification_sent = TRUE;
	}
	g_mutex_unlock(&pending_notifications_mutex);

	if (notification_sent)
		g_source_set_ready_time(send_notifcation_timer, g_source_get_time(send_notifcation_timer) + 200000L); // 200ms
	else
		g_source_set_ready_time(send_notifcation_timer, -1);

	return G_SOURCE_CONTINUE;
}
static GSourceFuncs send_notifcation_funcs = {
	.dispatch = send_notifcation_func,
};
static void unref_nullsafe(void *data)
{
	if (data)
		g_object_unref(data);
}

void remove_app_notifications(void);

static void queue_notification(int id, GNotification *notification)
{
	g_mutex_lock(&pending_notifications_mutex);
	if (!pending_notifications) {
		pending_notifications = g_hash_table_new_full(NULL, NULL, NULL, unref_nullsafe);
		send_notifcation_timer = g_source_new(&send_notifcation_funcs, sizeof(GSource));
		g_source_attach(send_notifcation_timer, NULL);
		GApplication *app = g_application_get_default();
		gchar *desktop_id = g_strdup_printf("%s.desktop", g_application_get_application_id(app));
		GDesktopAppInfo *info = g_desktop_app_info_new(desktop_id);
		/* Some desktop environments don't allow XDG-portal notifications without a desktop file.
		   There is no public API to force a specific backend, so we have to set the environment variable.
		   The GNOTIFICATION_BACKEND variable will be read by GIO the first time the notification backend is used.
		   This method should be future proof unless the freedesktop backend is removed. */
		if (g_file_test("/.flatpak-info", G_FILE_TEST_EXISTS)) {
			/* in a Flatpak only the portal may show notifications: GIO picks it */
			GDBusConnection *bus = g_application_get_dbus_connection(app) ?: g_bus_get_sync(G_BUS_TYPE_SESSION, NULL, NULL);
			g_dbus_connection_signal_subscribe(bus, "org.freedesktop.portal.Desktop", "org.freedesktop.portal.Notification",
			                                   "ActionInvoked", "/org/freedesktop/portal/desktop", NULL,
			                                   G_DBUS_SIGNAL_FLAGS_NONE, portal_action_invoked, NULL, NULL);
			own_flatpak_name(bus);
		} else if (!info) {
			setenv("GNOTIFICATION_BACKEND", "freedesktop", 0);
		}
		if (info)
			g_object_unref(info);
		g_free(desktop_id);
		posted_notifications = g_hash_table_new(NULL, NULL);
		/* also when the app ends with System.exit() */
		atexit(remove_app_notifications);
	}
	g_hash_table_insert(pending_notifications, GINT_TO_POINTER(id), notification);
	if (notification)
		g_hash_table_add(posted_notifications, GINT_TO_POINTER(id));
	else
		g_hash_table_remove(posted_notifications, GINT_TO_POINTER(id));
	g_mutex_unlock(&pending_notifications_mutex);
	if (g_source_get_ready_time(send_notifcation_timer) == -1)
		g_source_set_ready_time(send_notifcation_timer, 0); // immediately
}

JNIEXPORT jlong JNICALL Java_android_app_NotificationManager_nativeInitBuilder(JNIEnv *env, jobject this)
{
	return _INTPTR(g_notification_new(""));
}

JNIEXPORT void JNICALL Java_android_app_NotificationManager_nativeAddAction(JNIEnv *env, jobject this, jlong builder_ptr, jstring name_jstr, jint type, jobject intent)
{
	GNotification *notification = _PTR(builder_ptr);
	const char *name = "";
	if (name_jstr)
		name = (*env)->GetStringUTFChars(env, name_jstr, NULL);
	const char *action = intent_actionname_from_type(type);
	if (action)
		g_notification_add_button_with_target_value(notification, name, action, intent_serialize(env, intent));
	if (name_jstr)
		(*env)->ReleaseStringUTFChars(env, name_jstr, name);
}

JNIEXPORT void JNICALL Java_android_app_NotificationManager_nativeShowNotification(JNIEnv *env, jobject this, jlong builder_ptr, jint id, jstring title_jstr, jstring text_jstr, jstring icon_jstr, jboolean ongoing, jint priority, jint type, jobject intent)
{
	GNotification *notification = _PTR(builder_ptr);

	g_notification_set_priority(notification, priority);

	if (title_jstr) {
		const char *title = (*env)->GetStringUTFChars(env, title_jstr, NULL);
		g_notification_set_title(notification, title);
		(*env)->ReleaseStringUTFChars(env, title_jstr, title);
	}
	if (text_jstr) {
		const char *text = (*env)->GetStringUTFChars(env, text_jstr, NULL);
		g_notification_set_body(notification, text);
		(*env)->ReleaseStringUTFChars(env, text_jstr, text);
	}
	if (icon_jstr) {
		const char *icon_path = (*env)->GetStringUTFChars(env, icon_jstr, NULL);
		extract_from_apk(icon_path, icon_path);
		char *icon_path_full = g_strdup_printf("%s/%s", get_app_data_dir(), icon_path);
		GMappedFile *icon_file = g_mapped_file_new(icon_path_full, FALSE, NULL);
		if (icon_file) {
			GBytes *icon_bytes = g_mapped_file_get_bytes(icon_file);
			GIcon *icon = g_bytes_icon_new(icon_bytes);
			g_notification_set_icon(notification, icon);
			g_object_unref(icon);
			g_bytes_unref(icon_bytes);
			g_mapped_file_unref(icon_file);
		}
		g_free(icon_path_full);
		(*env)->ReleaseStringUTFChars(env, icon_jstr, icon_path);
	}
	const char *action = intent_actionname_from_type(type);
	if (action)
		g_notification_set_default_action_and_target_value(notification, action, intent_serialize(env, intent));
	queue_notification(id, notification);
}

JNIEXPORT void JNICALL Java_android_app_NotificationManager_nativeCancel(JNIEnv *env, jobject this, jint id)
{
	queue_notification(id, NULL);
}

static void remove_app_notification(gpointer key, gpointer value, gpointer user_data)
{
	char *id_string = desktop_id(GPOINTER_TO_INT(key));
	g_application_withdraw_notification(user_data, id_string);
	g_free(id_string);
}

void remove_app_notifications(void)
{
	GApplication *app = g_application_get_default();
	if (!posted_notifications || !app)
		return;
	g_mutex_lock(&pending_notifications_mutex);
	g_hash_table_remove_all(pending_notifications);
	g_hash_table_foreach(posted_notifications, remove_app_notification, app);
	g_hash_table_remove_all(posted_notifications);
	g_mutex_unlock(&pending_notifications_mutex);
	/* the withdrawals are D-Bus calls: send them before the process ends */
	GDBusConnection *bus = g_application_get_dbus_connection(app) ?: g_bus_get_sync(G_BUS_TYPE_SESSION, NULL, NULL);
	if (bus)
		g_dbus_connection_flush_sync(bus, NULL, NULL);
}

static MediaPlayer2 *mpris = NULL;
static int dbus_name_id = 0;
MediaPlayer2Player *mpris_player_get(void);
extern GtkWindow *window;

/* Raise brings the app's window back, also when it was closed while a foreground service kept the app running */
static gboolean on_media_player_handle_raise(MediaPlayer2 *mpris, GDBusMethodInvocation *invocation, gpointer user_data)
{
	activity_window_present();
	media_player2_complete_raise(mpris, invocation);
	return TRUE;
}

/* Quit ends the app, even while a foreground service runs */
static gboolean on_media_player_handle_quit(MediaPlayer2 *mpris, GDBusMethodInvocation *invocation, gpointer user_data)
{
	media_player2_complete_quit(mpris, invocation);
	activity_app_quit();
	return TRUE;
}

static void on_bus_acquired(GDBusConnection *connection, const char *name, gpointer user_data)
{
	g_dbus_interface_skeleton_export(G_DBUS_INTERFACE_SKELETON(mpris),
	                                 connection, MPRIS_OBJECT_NAME, NULL);

	g_dbus_interface_skeleton_export(G_DBUS_INTERFACE_SKELETON(mpris_player_get()),
	                                 connection, MPRIS_OBJECT_NAME, NULL);
}

JNIEXPORT void JNICALL Java_android_app_NotificationManager_nativeShowMPRIS(JNIEnv *env, jobject this, jstring package_name_jstr, jstring identity_jstr)
{
	if (!mpris) {
		mpris = media_player2_skeleton_new();
		g_signal_connect(mpris, "handle-raise", G_CALLBACK(on_media_player_handle_raise), NULL);
		g_signal_connect(mpris, "handle-quit", G_CALLBACK(on_media_player_handle_quit), NULL);
	}
	const char *package_name = NULL;
	const char *app_id = g_application_get_application_id(G_APPLICATION(gtk_window_get_application(window)));
	if ((app_id == NULL || strcmp(app_id, "com.example.demo_application") == 0) && package_name_jstr) {
		// fall back to package name
		app_id = package_name = (*env)->GetStringUTFChars(env, package_name_jstr, NULL);
	}
	if (!dbus_name_id) {
		gchar *bus_name = g_strdup_printf("%s%s", MPRIS_BUS_NAME_PREFIX, app_id);
		dbus_name_id = g_bus_own_name(G_BUS_TYPE_SESSION, bus_name, G_BUS_NAME_OWNER_FLAGS_NONE,
		                              on_bus_acquired, NULL, NULL, mpris, NULL);
		g_free(bus_name);
	}
	media_player2_set_can_raise(mpris, TRUE);
	media_player2_set_can_quit(mpris, TRUE);
	static const char *const none[] = {NULL};
	media_player2_set_supported_uri_schemes(mpris, none);
	media_player2_set_supported_mime_types(mpris, none);
	media_player2_set_desktop_entry(mpris, app_id);
	if (package_name) {
		(*env)->ReleaseStringUTFChars(env, package_name_jstr, package_name);
	}
	if (identity_jstr) {
		const char *identity = (*env)->GetStringUTFChars(env, identity_jstr, NULL);
		media_player2_set_identity(mpris, identity);
		(*env)->ReleaseStringUTFChars(env, identity_jstr, identity);
	}
}

JNIEXPORT void JNICALL Java_android_app_NotificationManager_nativeCancelMPRIS(JNIEnv *env, jobject this)
{
	if (dbus_name_id) {
		g_dbus_interface_skeleton_unexport(G_DBUS_INTERFACE_SKELETON(mpris));
		g_dbus_interface_skeleton_unexport(G_DBUS_INTERFACE_SKELETON(mpris_player_get()));
		g_clear_handle_id(&dbus_name_id, g_bus_unown_name);
	}
}
