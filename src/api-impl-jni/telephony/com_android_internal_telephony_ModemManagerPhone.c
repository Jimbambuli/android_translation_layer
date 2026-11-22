#include <gio/gio.h>
#include <string.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/com_android_internal_telephony_ModemManagerPhone.h"

/*
 * ModemManager on the system bus for ModemManagerPhone (ATL's phone process): the properties of the modems (object
 * manager), of their SIM and SMS objects, creating and sending SMS. Every change (InterfacesAdded/Removed,
 * PropertiesChanged below /org/freedesktop/ModemManager1, the Messaging signal Added) goes to Java in the main
 * context, which then reads the state again. In a Flatpak the system bus needs --system-talk-name=org.freedesktop.ModemManager1.
 */

#define MM_NAME "org.freedesktop.ModemManager1"
#define MM_PATH "/org/freedesktop/ModemManager1"

static GDBusConnection *system_bus;

/* a property value as Java reads it: strings and object paths as they are, numbers in decimal, booleans
 * true/false, string and path arrays one per line, bytes in hex, a struct its first member (SignalQuality (ub)),
 * else GVariant text */
static char *value_string(GVariant *value)
{
	if (g_variant_is_of_type(value, G_VARIANT_TYPE_STRING) || g_variant_is_of_type(value, G_VARIANT_TYPE_OBJECT_PATH))
		return g_variant_dup_string(value, NULL);
	if (g_variant_is_of_type(value, G_VARIANT_TYPE_BOOLEAN))
		return g_strdup(g_variant_get_boolean(value) ? "true" : "false");
	if (g_variant_is_of_type(value, G_VARIANT_TYPE_INT32))
		return g_strdup_printf("%d", g_variant_get_int32(value));
	if (g_variant_is_of_type(value, G_VARIANT_TYPE_UINT32))
		return g_strdup_printf("%u", g_variant_get_uint32(value));
	if (g_variant_is_of_type(value, G_VARIANT_TYPE_INT64))
		return g_strdup_printf("%" G_GINT64_FORMAT, g_variant_get_int64(value));
	if (g_variant_is_of_type(value, G_VARIANT_TYPE_UINT64))
		return g_strdup_printf("%" G_GUINT64_FORMAT, g_variant_get_uint64(value));
	if (g_variant_is_of_type(value, G_VARIANT_TYPE_BYTE))
		return g_strdup_printf("%u", g_variant_get_byte(value));
	if (g_variant_is_of_type(value, G_VARIANT_TYPE_STRING_ARRAY) || g_variant_is_of_type(value, G_VARIANT_TYPE_OBJECT_PATH_ARRAY)) {
		g_autofree const char **list = g_variant_is_of_type(value, G_VARIANT_TYPE_STRING_ARRAY) ? g_variant_get_strv(value, NULL) : g_variant_get_objv(value, NULL);
		return g_strjoinv("\n", (char **)list);
	}
	if (g_variant_is_of_type(value, G_VARIANT_TYPE_BYTESTRING)) { /* SMS Data: hex */
		gsize len;
		const guint8 *data = g_variant_get_fixed_array(value, &len, 1);
		GString *hex = g_string_new(NULL);
		for (gsize i = 0; i < len; i++)
			g_string_append_printf(hex, "%02x", data[i]);
		return g_string_free(hex, FALSE);
	}
	if (g_variant_is_container(value) && g_variant_is_of_type(value, G_VARIANT_TYPE_TUPLE) && g_variant_n_children(value) > 0) {
		g_autoptr(GVariant) first = g_variant_get_child_value(value, 0);
		return value_string(first);
	}
	return g_variant_print(value, FALSE);
}

/* rows of four strings: object path, interface, property, value */
static void add_properties(GPtrArray *rows, const char *path, const char *iface, GVariant *props)
{
	GVariantIter iter;
	const char *name;
	GVariant *value;
	g_variant_iter_init(&iter, props);
	while (g_variant_iter_next(&iter, "{&sv}", &name, &value)) {
		g_ptr_array_add(rows, g_strdup(path));
		g_ptr_array_add(rows, g_strdup(iface));
		g_ptr_array_add(rows, g_strdup(name));
		g_ptr_array_add(rows, value_string(value));
		g_variant_unref(value);
	}
}

static jobjectArray to_java(JNIEnv *env, GPtrArray *rows)
{
	jobjectArray array = (*env)->NewObjectArray(env, rows->len, (*env)->FindClass(env, "java/lang/String"), NULL);
	for (guint i = 0; i < rows->len; i++) {
		jstring s = (*env)->NewStringUTF(env, g_ptr_array_index(rows, i));
		(*env)->SetObjectArrayElement(env, array, i, s);
		(*env)->DeleteLocalRef(env, s);
	}
	return array;
}

static gboolean changed_idle(gpointer data)
{
	J__ModemManagerPhone__changed(get_jni_env());
	return G_SOURCE_REMOVE;
}

static guint changed_pending;

static gboolean changed_later(gpointer data)
{
	changed_pending = 0;
	return changed_idle(data);
}

/* many PropertiesChanged come together (a modem that registers): Java reads the state once after them */
static void on_changed(GDBusConnection *bus, const char *sender, const char *path, const char *iface,
                       const char *signal, GVariant *parameters, gpointer data)
{
	if (!changed_pending)
		changed_pending = g_timeout_add(50, changed_later, NULL);
}

static void on_sms_added(GDBusConnection *bus, const char *sender, const char *path, const char *iface,
                         const char *signal, GVariant *parameters, gpointer data)
{
	const char *sms_path;
	gboolean received;
	g_variant_get(parameters, "(&ob)", &sms_path, &received);
	JNIEnv *env = get_jni_env();
	jstring jpath = (*env)->NewStringUTF(env, sms_path);
	J__ModemManagerPhone__smsAdded(env, jpath, received);
	(*env)->DeleteLocalRef(env, jpath);
}

static void on_owner_changed(GDBusConnection *bus, const char *sender, const char *path, const char *iface,
                             const char *signal, GVariant *parameters, gpointer data)
{
	on_changed(bus, sender, path, iface, signal, parameters, data);
}

static gboolean subscribe_idle(gpointer data)
{
	/* in the main context: the signals arrive there */
	g_dbus_connection_signal_subscribe(system_bus, MM_NAME, "org.freedesktop.DBus.ObjectManager", NULL, MM_PATH, NULL,
	                                   G_DBUS_SIGNAL_FLAGS_NONE, on_changed, NULL, NULL);
	g_dbus_connection_signal_subscribe(system_bus, MM_NAME, "org.freedesktop.DBus.Properties", "PropertiesChanged",
	                                   MM_PATH, NULL, G_DBUS_SIGNAL_FLAGS_MATCH_ARG0_PATH, on_changed, NULL, NULL);
	g_dbus_connection_signal_subscribe(system_bus, MM_NAME, MM_NAME ".Modem.Messaging", "Added", NULL, NULL,
	                                   G_DBUS_SIGNAL_FLAGS_NONE, on_sms_added, NULL, NULL);
	g_dbus_connection_signal_subscribe(system_bus, "org.freedesktop.DBus", "org.freedesktop.DBus", "NameOwnerChanged",
	                                   "/org/freedesktop/DBus", MM_NAME, G_DBUS_SIGNAL_FLAGS_NONE, on_owner_changed, NULL, NULL);
	return G_SOURCE_REMOVE;
}

/* false without a system bus (then there is no modem) */
JNIEXPORT jboolean JNICALL Java_com_android_internal_telephony_ModemManagerPhone_nativeConnect(JNIEnv *env, jclass)
{
	g_autoptr(GError) error = NULL;
	system_bus = g_bus_get_sync(G_BUS_TYPE_SYSTEM, NULL, &error);
	if (!system_bus) {
		printf("ModemManagerPhone: no system bus: %s\n", error->message);
		return JNI_FALSE;
	}
	g_idle_add(subscribe_idle, NULL);
	return JNI_TRUE;
}

static GVariant *call(const char *path, const char *iface, const char *method, GVariant *args, const GVariantType *type, GError **error)
{
	return g_dbus_connection_call_sync(system_bus, MM_NAME, path, iface, method, args, type,
	                                   G_DBUS_CALL_FLAGS_NO_AUTO_START, 10000, NULL, error);
}

/* the modems with all their interfaces (ObjectManager.GetManagedObjects), empty when ModemManager does not run */
JNIEXPORT jobjectArray JNICALL Java_com_android_internal_telephony_ModemManagerPhone_nativeGetModems(JNIEnv *env, jclass)
{
	g_autoptr(GPtrArray) rows = g_ptr_array_new_with_free_func(g_free);
	g_autoptr(GError) error = NULL;
	g_autoptr(GVariant) ret = call(MM_PATH, "org.freedesktop.DBus.ObjectManager", "GetManagedObjects", NULL,
	                               G_VARIANT_TYPE("(a{oa{sa{sv}}})"), &error);
	if (ret) {
		g_autoptr(GVariant) objects = g_variant_get_child_value(ret, 0);
		GVariantIter iter;
		const char *path;
		GVariant *ifaces;
		g_variant_iter_init(&iter, objects);
		while (g_variant_iter_next(&iter, "{&o@a{sa{sv}}}", &path, &ifaces)) {
			GVariantIter iiter;
			const char *iface;
			GVariant *props;
			g_variant_iter_init(&iiter, ifaces);
			while (g_variant_iter_next(&iiter, "{&s@a{sv}}", &iface, &props)) {
				add_properties(rows, path, iface, props);
				g_variant_unref(props);
			}
			g_variant_unref(ifaces);
		}
	} else if (!g_error_matches(error, G_DBUS_ERROR, G_DBUS_ERROR_SERVICE_UNKNOWN) &&
	           !g_error_matches(error, G_DBUS_ERROR, G_DBUS_ERROR_NAME_HAS_NO_OWNER)) {
		printf("ModemManagerPhone: GetManagedObjects: %s\n", error->message);
	}
	return to_java(env, rows);
}

/* the properties of one object (SIM, SMS), empty when it is gone */
JNIEXPORT jobjectArray JNICALL Java_com_android_internal_telephony_ModemManagerPhone_nativeGetAll(JNIEnv *env, jclass, jstring jpath, jstring jiface)
{
	const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
	const char *iface = (*env)->GetStringUTFChars(env, jiface, NULL);
	g_autoptr(GPtrArray) rows = g_ptr_array_new_with_free_func(g_free);
	g_autoptr(GVariant) ret = NULL;
	if (g_variant_is_object_path(path) && strcmp(path, "/") != 0)
		ret = call(path, "org.freedesktop.DBus.Properties", "GetAll", g_variant_new("(s)", iface), G_VARIANT_TYPE("(a{sv})"), NULL);
	if (ret) {
		g_autoptr(GVariant) props = g_variant_get_child_value(ret, 0);
		add_properties(rows, path, iface, props);
	}
	(*env)->ReleaseStringUTFChars(env, jpath, path);
	(*env)->ReleaseStringUTFChars(env, jiface, iface);
	return to_java(env, rows);
}

/* Messaging.Create: a text (text != null) or data SMS; the path of the new SMS object, or "!" and the error */
JNIEXPORT jstring JNICALL Java_com_android_internal_telephony_ModemManagerPhone_nativeCreateSms(JNIEnv *env, jclass, jstring jmodem, jstring jnumber, jstring jtext, jbyteArray jdata, jboolean delivery_report)
{
	const char *modem = (*env)->GetStringUTFChars(env, jmodem, NULL);
	const char *number = (*env)->GetStringUTFChars(env, jnumber, NULL);
	GVariantBuilder props;
	g_variant_builder_init(&props, G_VARIANT_TYPE("a{sv}"));
	g_variant_builder_add(&props, "{sv}", "number", g_variant_new_string(number));
	if (jtext) {
		const char *text = (*env)->GetStringUTFChars(env, jtext, NULL);
		g_variant_builder_add(&props, "{sv}", "text", g_variant_new_string(text));
		(*env)->ReleaseStringUTFChars(env, jtext, text);
	} else {
		jsize len = (*env)->GetArrayLength(env, jdata);
		jbyte *data = (*env)->GetByteArrayElements(env, jdata, NULL);
		g_variant_builder_add(&props, "{sv}", "data", g_variant_new_fixed_array(G_VARIANT_TYPE_BYTE, data, len, 1));
		(*env)->ReleaseByteArrayElements(env, jdata, data, JNI_ABORT);
	}
	if (delivery_report)
		g_variant_builder_add(&props, "{sv}", "delivery-report-request", g_variant_new_boolean(TRUE));
	g_autoptr(GError) error = NULL;
	g_autoptr(GVariant) ret = call(modem, MM_NAME ".Modem.Messaging", "Create", g_variant_new("(a{sv})", &props),
	                               G_VARIANT_TYPE("(o)"), &error);
	(*env)->ReleaseStringUTFChars(env, jmodem, modem);
	(*env)->ReleaseStringUTFChars(env, jnumber, number);
	g_autofree char *result = NULL;
	if (ret)
		g_variant_get(ret, "(o)", &result);
	else
		result = g_strconcat("!", error->message, NULL);
	return (*env)->NewStringUTF(env, result);
}

static void sms_sent(GObject *source, GAsyncResult *res, gpointer data)
{
	g_autoptr(GError) error = NULL;
	g_autoptr(GVariant) ret = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source), res, &error);
	JNIEnv *env = get_jni_env();
	jstring message = ret ? NULL : (*env)->NewStringUTF(env, error->message);
	J__ModemManagerPhone__smsSent(env, GPOINTER_TO_INT(data), message);
	if (message)
		(*env)->DeleteLocalRef(env, message);
}

struct send_request {
	char *path;
	int token;
};

static gboolean send_idle(gpointer data)
{
	struct send_request *r = data;
	/* sending can take long (network): no timeout of our own */
	g_dbus_connection_call(system_bus, MM_NAME, r->path, MM_NAME ".Sms", "Send", NULL, NULL,
	                       G_DBUS_CALL_FLAGS_NO_AUTO_START, G_MAXINT, NULL, sms_sent, GINT_TO_POINTER(r->token));
	g_free(r->path);
	g_free(r);
	return G_SOURCE_REMOVE;
}

/* Sms.Send; the answer comes as smsSent(token, error or null) in the main context */
JNIEXPORT void JNICALL Java_com_android_internal_telephony_ModemManagerPhone_nativeSendSms(JNIEnv *env, jclass, jstring jpath, jint token)
{
	const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
	struct send_request *r = g_new(struct send_request, 1);
	r->path = g_strdup(path);
	r->token = token;
	(*env)->ReleaseStringUTFChars(env, jpath, path);
	g_idle_add(send_idle, r);
}
