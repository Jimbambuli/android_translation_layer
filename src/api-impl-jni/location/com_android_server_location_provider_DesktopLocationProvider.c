#include <gio/gio.h>
#include <libportal/portal.h>
#include <string.h>
#include <time.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/com_android_server_location_provider_DesktopLocationProvider.h"

/*
 * The desktop's position for DesktopLocationProvider: in a Flatpak the location portal (libportal: CreateSession,
 * Start, LocationUpdated), outside GeoClue2 on the system bus (Manager.GetClient, Start, LocationUpdated). The portal
 * itself talks to GeoClue; both deliver GeoClue's Location properties. One session at a time: a new accuracy while a
 * start is under way waits for its answer.
 */

#define GEOCLUE_NAME "org.freedesktop.GeoClue2"

static gboolean in_flatpak;
static gboolean starting;   /* a start was sent and has not answered yet */
static gboolean running;    /* a session is open */
static int wanted = -1;     /* the accuracy Java wants, -1 none */
static int session_accuracy = -1;
static int failed = -1;      /* the accuracy whose start failed: no new try until the request changes */

static XdpPortal *portal;

static GDBusConnection *system_bus;
static char *client_path;
static guint client_signal;

static void apply(void);

/* GeoClue: seconds and microseconds of the fix */
static void deliver(double latitude, double longitude, double altitude, double accuracy, double speed, double heading,
                    guint64 timestamp_s, guint64 timestamp_us)
{
	struct timespec now; /* SystemClock.elapsedRealtimeNanos() */
	clock_gettime(CLOCK_BOOTTIME, &now);
	J__DesktopLocationProvider__locationUpdated(get_jni_env(), latitude, longitude, altitude, accuracy, speed, heading,
	                                            (jlong)(timestamp_s * 1000 + timestamp_us / 1000),
	                                            now.tv_sec * 1000000000LL + now.tv_nsec);
}

static void denied(void)
{
	wanted = -1;
	J__DesktopLocationProvider__locationDenied(get_jni_env());
}

static void started(gboolean ok)
{
	starting = FALSE;
	running = ok;
	failed = ok ? -1 : session_accuracy;
	apply();
}

/* ---- portal ---- */

static void portal_location_updated(XdpPortal *self, gdouble latitude, gdouble longitude, gdouble altitude,
                                    gdouble accuracy, gdouble speed, gdouble heading, gchar *description,
                                    gint64 timestamp_s, gint64 timestamp_us, gpointer data)
{
	if (running)
		deliver(latitude, longitude, altitude, accuracy, speed, heading, timestamp_s, timestamp_us);
}

static void portal_started(GObject *source, GAsyncResult *result, gpointer data)
{
	g_autoptr(GError) error = NULL;
	if (xdp_portal_location_monitor_start_finish(XDP_PORTAL(source), result, &error)) {
		started(TRUE);
		return;
	}
	printf("LocationManager: the location portal gives no position: %s\n", error->message);
	xdp_portal_location_monitor_stop(portal);
	if (g_error_matches(error, G_IO_ERROR, G_IO_ERROR_CANCELLED)) /* response 1: the user said no */
		denied();
	started(FALSE);
}

/* ---- GeoClue ---- */

/* GClueAccuracyLevel for the portal's levels 0..5 (what xdg-desktop-portal's location.c maps them to) */
static const guint geoclue_accuracy[] = {0, 1, 4, 5, 6, 8};

static void geoclue_location_properties(GObject *source, GAsyncResult *result, gpointer data)
{
	g_autoptr(GError) error = NULL;
	g_autoptr(GVariant) ret = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source), result, &error);
	if (!ret) {
		printf("LocationManager: GeoClue location: %s\n", error->message);
		return;
	}
	g_autoptr(GVariant) dict = NULL;
	double latitude = 0, longitude = 0, accuracy = 0, altitude = -G_MAXDOUBLE, speed = -1, heading = -1;
	guint64 timestamp_s = 0, timestamp_us = 0;
	g_variant_get(ret, "(@a{sv})", &dict);
	g_variant_lookup(dict, "Latitude", "d", &latitude);
	g_variant_lookup(dict, "Longitude", "d", &longitude);
	g_variant_lookup(dict, "Accuracy", "d", &accuracy);
	g_variant_lookup(dict, "Altitude", "d", &altitude);
	g_variant_lookup(dict, "Speed", "d", &speed);
	g_variant_lookup(dict, "Heading", "d", &heading);
	g_variant_lookup(dict, "Timestamp", "(tt)", &timestamp_s, &timestamp_us);
	if (running)
		deliver(latitude, longitude, altitude, accuracy, speed, heading, timestamp_s, timestamp_us);
}

static void geoclue_location_updated(GDBusConnection *bus, const char *sender, const char *path, const char *iface,
                                     const char *signal, GVariant *parameters, gpointer data)
{
	const char *old_location, *new_location;
	g_variant_get(parameters, "(&o&o)", &old_location, &new_location);
	if (strcmp(new_location, "/") == 0)
		return;
	g_dbus_connection_call(bus, GEOCLUE_NAME, new_location, "org.freedesktop.DBus.Properties", "GetAll",
	                       g_variant_new("(s)", "org.freedesktop.GeoClue2.Location"), G_VARIANT_TYPE("(a{sv})"),
	                       G_DBUS_CALL_FLAGS_NONE, -1, NULL, geoclue_location_properties, NULL);
}

static void geoclue_stop(void)
{
	if (client_signal) {
		g_dbus_connection_signal_unsubscribe(system_bus, client_signal);
		client_signal = 0;
	}
	if (client_path) {
		g_dbus_connection_call(system_bus, GEOCLUE_NAME, client_path, "org.freedesktop.GeoClue2.Client", "Stop",
		                       NULL, NULL, G_DBUS_CALL_FLAGS_NONE, -1, NULL, NULL, NULL);
		g_clear_pointer(&client_path, g_free);
	}
}

static void geoclue_started(GObject *source, GAsyncResult *result, gpointer data)
{
	g_autoptr(GError) error = NULL;
	g_autoptr(GVariant) ret = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source), result, &error);
	if (ret) {
		started(TRUE);
		return;
	}
	printf("LocationManager: GeoClue gives no position: %s\n", error->message);
	g_autofree char *remote = g_dbus_error_get_remote_error(error);
	geoclue_stop();
	if (remote && strcmp(remote, "org.freedesktop.DBus.Error.AccessDenied") == 0) /* the agent said no */
		denied();
	started(FALSE);
}

static void geoclue_set(const char *name, GVariant *value)
{
	g_dbus_connection_call(system_bus, GEOCLUE_NAME, client_path, "org.freedesktop.DBus.Properties", "Set",
	                       g_variant_new("(ssv)", "org.freedesktop.GeoClue2.Client", name, value), NULL,
	                       G_DBUS_CALL_FLAGS_NONE, -1, NULL, NULL, NULL);
}

static void geoclue_got_client(GObject *source, GAsyncResult *result, gpointer data)
{
	g_autoptr(GError) error = NULL;
	g_autoptr(GVariant) ret = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source), result, &error);
	if (!ret) {
		printf("LocationManager: GeoClue gives no client: %s\n", error->message);
		started(FALSE);
		return;
	}
	g_variant_get(ret, "(o)", &client_path);
	GApplication *app = g_application_get_default();
	const char *app_id = app ? g_application_get_application_id(app) : NULL;
	/* GeoClue asks its agent for the desktop id; the calls are ordered on the connection, so all are set before
	 * Start */
	geoclue_set("DesktopId", g_variant_new_string(app_id ? app_id : "android_translation_layer"));
	geoclue_set("RequestedAccuracyLevel", g_variant_new_uint32(geoclue_accuracy[session_accuracy]));
	geoclue_set("DistanceThreshold", g_variant_new_uint32(0));
	geoclue_set("TimeThreshold", g_variant_new_uint32(0));
	client_signal = g_dbus_connection_signal_subscribe(system_bus, GEOCLUE_NAME, "org.freedesktop.GeoClue2.Client",
	                                                   "LocationUpdated", client_path, NULL, G_DBUS_SIGNAL_FLAGS_NONE,
	                                                   geoclue_location_updated, NULL, NULL);
	g_dbus_connection_call(system_bus, GEOCLUE_NAME, client_path, "org.freedesktop.GeoClue2.Client", "Start", NULL,
	                       NULL, G_DBUS_CALL_FLAGS_NONE, -1, NULL, geoclue_started, NULL);
}

/* ---- one session at a time ---- */

static void apply(void)
{
	if (starting || (running && wanted == session_accuracy) || (!running && wanted >= 0 && wanted == failed))
		return;
	if (running) {
		if (in_flatpak)
			xdp_portal_location_monitor_stop(portal);
		else
			geoclue_stop();
		running = FALSE;
	}
	if (wanted < 0)
		return;
	session_accuracy = wanted;
	starting = TRUE;
	if (in_flatpak) {
		if (!portal) { /* in the main context, not on the Java thread that asked whether location is available */
			portal = xdp_portal_new();
			g_signal_connect(portal, "location-updated", G_CALLBACK(portal_location_updated), NULL);
		}
		xdp_portal_location_monitor_start(portal, NULL, 0, 0, session_accuracy, XDP_LOCATION_MONITOR_FLAG_NONE, NULL,
		                                  portal_started, NULL);
	} else {
		g_dbus_connection_call(system_bus, GEOCLUE_NAME, "/org/freedesktop/GeoClue2/Manager",
		                       "org.freedesktop.GeoClue2.Manager", "GetClient", NULL, G_VARIANT_TYPE("(o)"),
		                       G_DBUS_CALL_FLAGS_NONE, -1, NULL, geoclue_got_client, NULL);
	}
}

static gboolean apply_idle(gpointer data)
{
	wanted = GPOINTER_TO_INT(data);
	if (wanted < 0)
		failed = -1;
	apply();
	return G_SOURCE_REMOVE;
}

/* the portal in a Flatpak (it asks the user itself); outside, GeoClue running or D-Bus activatable */
JNIEXPORT jboolean JNICALL Java_com_android_server_location_provider_DesktopLocationProvider_nativeIsAvailable(JNIEnv *env, jclass)
{
	in_flatpak = g_file_test("/.flatpak-info", G_FILE_TEST_EXISTS);
	if (in_flatpak)
		return JNI_TRUE;
	system_bus = g_bus_get_sync(G_BUS_TYPE_SYSTEM, NULL, NULL);
	if (!system_bus)
		return JNI_FALSE;
	g_autoptr(GVariant) owner = g_dbus_connection_call_sync(system_bus, "org.freedesktop.DBus", "/org/freedesktop/DBus",
	                                                        "org.freedesktop.DBus", "NameHasOwner",
	                                                        g_variant_new("(s)", GEOCLUE_NAME), G_VARIANT_TYPE("(b)"),
	                                                        G_DBUS_CALL_FLAGS_NONE, -1, NULL, NULL);
	gboolean has_owner = FALSE;
	if (owner)
		g_variant_get(owner, "(b)", &has_owner);
	if (has_owner)
		return JNI_TRUE;
	g_autoptr(GVariant) names = g_dbus_connection_call_sync(system_bus, "org.freedesktop.DBus", "/org/freedesktop/DBus",
	                                                        "org.freedesktop.DBus", "ListActivatableNames", NULL,
	                                                        G_VARIANT_TYPE("(as)"), G_DBUS_CALL_FLAGS_NONE, -1, NULL, NULL);
	if (names) {
		g_autofree const char **list = NULL;
		g_variant_get(names, "(^a&s)", &list);
		for (int i = 0; list && list[i]; i++)
			if (strcmp(list[i], GEOCLUE_NAME) == 0)
				return JNI_TRUE;
	}
	return JNI_FALSE;
}

/* Java calls these from any thread; the session lives in the main context */
JNIEXPORT void JNICALL Java_com_android_server_location_provider_DesktopLocationProvider_nativeStart(JNIEnv *env, jclass, jint accuracy)
{
	g_idle_add(apply_idle, GINT_TO_POINTER(accuracy));
}

JNIEXPORT void JNICALL Java_com_android_server_location_provider_DesktopLocationProvider_nativeStop(JNIEnv *env, jclass)
{
	g_idle_add(apply_idle, GINT_TO_POINTER(-1));
}
