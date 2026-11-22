#include <arpa/inet.h>
#include <gio/gio.h>
#include <string.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/com_android_server_wifi_NetworkManagerWifi.h"

/*
 * NetworkManagerWifi: the desktop's Wi-Fi through NetworkManager on the system bus (in a Flatpak it needs
 * --system-talk-name=org.freedesktop.NetworkManager). Java reads with synchronous calls from its own threads; the
 * scan request and the PropertiesChanged signals (LastScan: a scan finished; radio, device state, connection) live in
 * the main context and come back through the @Export methods. Only reads and RequestScan, which polkit allows as
 * org.freedesktop.NetworkManager.wifi.scan.
 */

#define NM_NAME "org.freedesktop.NetworkManager"
#define NM_PATH "/org/freedesktop/NetworkManager"
#define NM_DEVICE NM_NAME ".Device"
#define NM_WIRELESS NM_NAME ".Device.Wireless"
#define NM_AP NM_NAME ".AccessPoint"
#define NM_IP4CONFIG NM_NAME ".IP4Config"
#define NM_DEVICE_TYPE_WIFI 2

static GDBusConnection *bus;
static char *device_path; /* the first Wi-Fi device */
static gint64 last_scan = -1; /* main context */

/* the value of one property, NULL when it cannot be read */
static GVariant *get_property(const char *path, const char *iface, const char *name)
{
	g_autoptr(GVariant) ret = g_dbus_connection_call_sync(bus, NM_NAME, path, "org.freedesktop.DBus.Properties", "Get",
	                                                      g_variant_new("(ss)", iface, name), G_VARIANT_TYPE("(v)"),
	                                                      G_DBUS_CALL_FLAGS_NONE, -1, NULL, NULL);
	if (!ret)
		return NULL;
	GVariant *value;
	g_variant_get(ret, "(v)", &value);
	return value;
}

static guint32 get_uint(const char *path, const char *iface, const char *name)
{
	g_autoptr(GVariant) value = get_property(path, iface, name);
	if (value && g_variant_is_of_type(value, G_VARIANT_TYPE_UINT32))
		return g_variant_get_uint32(value);
	if (value && g_variant_is_of_type(value, G_VARIANT_TYPE_BOOLEAN))
		return g_variant_get_boolean(value);
	return 0;
}

static char *get_object_path(const char *path, const char *iface, const char *name)
{
	g_autoptr(GVariant) value = get_property(path, iface, name);
	if (!value || !g_variant_is_of_type(value, G_VARIANT_TYPE_OBJECT_PATH) || strcmp(g_variant_get_string(value, NULL), "/") == 0)
		return NULL;
	return g_variant_dup_string(value, NULL);
}

/* ---- signals (main context) ---- */

static gboolean has_key(GVariant *dict, const char *key)
{
	g_autoptr(GVariant) value = g_variant_lookup_value(dict, key, NULL);
	return value != NULL;
}

static void changed(GDBusConnection *connection, const char *sender, const char *path, const char *iface,
                    const char *signal, GVariant *parameters, gpointer data)
{
	const char *changed_iface;
	g_autoptr(GVariant) dict = NULL;
	g_autoptr(GVariant) invalidated = NULL;
	g_variant_get(parameters, "(&s@a{sv}@as)", &changed_iface, &dict, &invalidated);
	JNIEnv *env = get_jni_env();
	if (strcmp(changed_iface, NM_WIRELESS) == 0) {
		gint64 scan;
		if (g_variant_lookup(dict, "LastScan", "x", &scan) && scan != last_scan) {
			last_scan = scan;
			if (scan > 0)
				J__NetworkManagerWifi__scanDone(env, JNI_TRUE);
		}
		if (has_key(dict, "ActiveAccessPoint"))
			J__NetworkManagerWifi__stateChanged(env);
	} else if ((strcmp(changed_iface, NM_DEVICE) == 0 && has_key(dict, "State")) ||
	           (strcmp(changed_iface, NM_NAME) == 0 && (has_key(dict, "WirelessEnabled") ||
	                                                    has_key(dict, "WirelessHardwareEnabled")))) {
		J__NetworkManagerWifi__stateChanged(env);
	}
}

static gboolean subscribe_idle(gpointer data)
{
	g_autoptr(GVariant) scan = get_property(device_path, NM_WIRELESS, "LastScan");
	if (scan && g_variant_is_of_type(scan, G_VARIANT_TYPE_INT64))
		last_scan = g_variant_get_int64(scan);
	g_dbus_connection_signal_subscribe(bus, NM_NAME, "org.freedesktop.DBus.Properties", "PropertiesChanged", device_path,
	                                   NULL, G_DBUS_SIGNAL_FLAGS_NONE, changed, NULL, NULL);
	g_dbus_connection_signal_subscribe(bus, NM_NAME, "org.freedesktop.DBus.Properties", "PropertiesChanged", NM_PATH,
	                                   NULL, G_DBUS_SIGNAL_FLAGS_NONE, changed, NULL, NULL);
	return G_SOURCE_REMOVE;
}

/* ---- scan request (main context) ---- */

static void scan_requested(GObject *source, GAsyncResult *result, gpointer data)
{
	g_autoptr(GError) error = NULL;
	g_autoptr(GVariant) ret = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source), result, &error);
	if (ret)
		return; /* the results come with LastScan */
	printf("WifiManager: NetworkManager does not scan: %s\n", error->message);
	J__NetworkManagerWifi__scanDone(get_jni_env(), JNI_FALSE);
}

static gboolean request_scan_idle(gpointer data)
{
	g_dbus_connection_call(bus, NM_NAME, device_path, NM_WIRELESS, "RequestScan", g_variant_new("(a{sv})", NULL),
	                       NULL, G_DBUS_CALL_FLAGS_NONE, -1, NULL, scan_requested, NULL);
	return G_SOURCE_REMOVE;
}

/* ---- JNI ---- */

JNIEXPORT jboolean JNICALL Java_com_android_server_wifi_NetworkManagerWifi_nativeInit(JNIEnv *env, jclass)
{
	bus = g_bus_get_sync(G_BUS_TYPE_SYSTEM, NULL, NULL);
	if (!bus)
		return JNI_FALSE;
	g_autoptr(GVariant) devices = g_dbus_connection_call_sync(bus, NM_NAME, NM_PATH, NM_NAME, "GetDevices", NULL,
	                                                          G_VARIANT_TYPE("(ao)"), G_DBUS_CALL_FLAGS_NONE, -1, NULL, NULL);
	if (!devices)
		return JNI_FALSE;
	g_autoptr(GVariantIter) iter = NULL;
	const char *path;
	g_variant_get(devices, "(ao)", &iter);
	while (!device_path && g_variant_iter_next(iter, "&o", &path))
		if (get_uint(path, NM_DEVICE, "DeviceType") == NM_DEVICE_TYPE_WIFI)
			device_path = g_strdup(path);
	if (!device_path)
		return JNI_FALSE;
	g_idle_add(subscribe_idle, NULL);
	return JNI_TRUE;
}

/* {WirelessEnabled && WirelessHardwareEnabled, State, WirelessCapabilities, Bitrate, Metered, IPv4 address} */
JNIEXPORT jintArray JNICALL Java_com_android_server_wifi_NetworkManagerWifi_nativeGetDevice(JNIEnv *env, jclass)
{
	if (!device_path)
		return NULL;
	jint values[6] = {0};
	values[0] = get_uint(NM_PATH, NM_NAME, "WirelessEnabled") && get_uint(NM_PATH, NM_NAME, "WirelessHardwareEnabled");
	values[1] = get_uint(device_path, NM_DEVICE, "State");
	values[2] = get_uint(device_path, NM_WIRELESS, "WirelessCapabilities");
	values[3] = get_uint(device_path, NM_WIRELESS, "Bitrate");
	values[4] = get_uint(device_path, NM_DEVICE, "Metered");
	g_autofree char *ip4config = get_object_path(device_path, NM_DEVICE, "Ip4Config");
	if (ip4config) {
		g_autoptr(GVariant) addresses = get_property(ip4config, NM_IP4CONFIG, "AddressData");
		if (addresses && g_variant_is_of_type(addresses, G_VARIANT_TYPE("aa{sv}")) && g_variant_n_children(addresses) > 0) {
			g_autoptr(GVariant) first = g_variant_get_child_value(addresses, 0);
			const char *address;
			struct in_addr in;
			if (g_variant_lookup(first, "address", "&s", &address) && inet_pton(AF_INET, address, &in) == 1)
				values[5] = ntohl(in.s_addr);
		}
	}
	jintArray array = (*env)->NewIntArray(env, 6);
	(*env)->SetIntArrayRegion(env, array, 0, 6, values);
	return array;
}

JNIEXPORT jstring JNICALL Java_com_android_server_wifi_NetworkManagerWifi_nativeGetMacAddress(JNIEnv *env, jclass)
{
	if (!device_path)
		return NULL;
	g_autoptr(GVariant) value = get_property(device_path, NM_WIRELESS, "HwAddress");
	if (!value || !g_variant_is_of_type(value, G_VARIANT_TYPE_STRING))
		return NULL;
	return _JSTRING(g_variant_get_string(value, NULL));
}

/* {byte[] Ssid, String HwAddress, int[] {Strength, Frequency, Flags, WpaFlags, RsnFlags, Mode, LastSeen, Bandwidth}} */
static jobjectArray access_point(JNIEnv *env, const char *path)
{
	g_autoptr(GVariant) ret = g_dbus_connection_call_sync(bus, NM_NAME, path, "org.freedesktop.DBus.Properties", "GetAll",
	                                                      g_variant_new("(s)", NM_AP), G_VARIANT_TYPE("(a{sv})"),
	                                                      G_DBUS_CALL_FLAGS_NONE, -1, NULL, NULL);
	if (!ret)
		return NULL;
	g_autoptr(GVariant) dict = g_variant_get_child_value(ret, 0);
	g_autoptr(GVariant) ssid = g_variant_lookup_value(dict, "Ssid", G_VARIANT_TYPE_BYTESTRING);
	const char *bssid = NULL;
	if (!g_variant_lookup(dict, "HwAddress", "&s", &bssid))
		return NULL;
	guint8 strength = 0;
	guint32 frequency = 0, flags = 0, wpa = 0, rsn = 0, mode = 0, bandwidth = 0;
	gint32 last_seen = -1;
	g_variant_lookup(dict, "Strength", "y", &strength);
	g_variant_lookup(dict, "Frequency", "u", &frequency);
	g_variant_lookup(dict, "Flags", "u", &flags);
	g_variant_lookup(dict, "WpaFlags", "u", &wpa);
	g_variant_lookup(dict, "RsnFlags", "u", &rsn);
	g_variant_lookup(dict, "Mode", "u", &mode);
	g_variant_lookup(dict, "LastSeen", "i", &last_seen);
	g_variant_lookup(dict, "Bandwidth", "u", &bandwidth); /* NetworkManager 1.46 */
	jint values[8] = {strength, frequency, flags, wpa, rsn, mode, last_seen, bandwidth};

	gsize ssid_len = 0;
	const guint8 *ssid_bytes = ssid ? g_variant_get_fixed_array(ssid, &ssid_len, 1) : NULL;
	jbyteArray ssid_array = (*env)->NewByteArray(env, ssid_len);
	if (ssid_len)
		(*env)->SetByteArrayRegion(env, ssid_array, 0, ssid_len, (const jbyte *)ssid_bytes);
	jintArray value_array = (*env)->NewIntArray(env, 8);
	(*env)->SetIntArrayRegion(env, value_array, 0, 8, values);
	jclass object_class = (*env)->FindClass(env, "java/lang/Object");
	jobjectArray record = (*env)->NewObjectArray(env, 3, object_class, NULL);
	(*env)->SetObjectArrayElement(env, record, 0, ssid_array);
	(*env)->SetObjectArrayElement(env, record, 1, _JSTRING(bssid));
	(*env)->SetObjectArrayElement(env, record, 2, value_array);
	(*env)->DeleteLocalRef(env, ssid_array);
	(*env)->DeleteLocalRef(env, value_array);
	(*env)->DeleteLocalRef(env, object_class);
	return record;
}

JNIEXPORT jobjectArray JNICALL Java_com_android_server_wifi_NetworkManagerWifi_nativeGetAccessPoints(JNIEnv *env, jclass)
{
	if (!device_path)
		return NULL;
	g_autoptr(GVariant) ret = g_dbus_connection_call_sync(bus, NM_NAME, device_path, NM_WIRELESS, "GetAllAccessPoints", NULL,
	                                                      G_VARIANT_TYPE("(ao)"), G_DBUS_CALL_FLAGS_NONE, -1, NULL, NULL);
	if (!ret)
		return NULL;
	g_autoptr(GVariant) paths = g_variant_get_child_value(ret, 0);
	gsize n = g_variant_n_children(paths);
	jclass object_class = (*env)->FindClass(env, "java/lang/Object");
	jobjectArray records = (*env)->NewObjectArray(env, n, object_class, NULL);
	for (gsize i = 0; i < n; i++) {
		const char *path;
		g_variant_get_child(paths, i, "&o", &path);
		jobjectArray record = access_point(env, path);
		if (!record) { /* gone between the two calls */
			jobjectArray empty = (*env)->NewObjectArray(env, 3, object_class, NULL);
			jintArray never = (*env)->NewIntArray(env, 8);
			jint values[8] = {0, 0, 0, 0, 0, 0, -1, 0};
			(*env)->SetIntArrayRegion(env, never, 0, 8, values);
			(*env)->SetObjectArrayElement(env, empty, 2, never);
			(*env)->DeleteLocalRef(env, never);
			record = empty;
		}
		(*env)->SetObjectArrayElement(env, records, i, record);
		(*env)->DeleteLocalRef(env, record);
	}
	(*env)->DeleteLocalRef(env, object_class);
	return records;
}

JNIEXPORT jobjectArray JNICALL Java_com_android_server_wifi_NetworkManagerWifi_nativeGetActiveAccessPoint(JNIEnv *env, jclass)
{
	if (!device_path)
		return NULL;
	g_autofree char *path = get_object_path(device_path, NM_WIRELESS, "ActiveAccessPoint");
	return path ? access_point(env, path) : NULL;
}

JNIEXPORT void JNICALL Java_com_android_server_wifi_NetworkManagerWifi_nativeRequestScan(JNIEnv *env, jclass)
{
	if (device_path)
		g_idle_add(request_scan_idle, NULL);
}
