/*
 * The NFC controller of ATL: neard (org.neard) on the system bus.
 *
 * neard D-Bus API (neard 0.19, doc/adapter-api.txt, doc/tag-api.txt, src/ndef.c record_properties):
 * org.freedesktop.DBus.ObjectManager at "/"; adapters /org/neard/nfcN with org.neard.Adapter (Powered, Polling,
 * Mode, Protocols; StartPollLoop(s mode), StopPollLoop()); a tag in range is /org/neard/nfcN/tagM with org.neard.Tag
 * (Type, Protocol, ReadOnly, Uid) and its NDEF records below it with org.neard.Record (Type, Encoding, Language,
 * Representation, URI, Action, MIMEType, Size, MIME, AAR, MIMEPayload).
 *
 * The tags go to NeardAdapter.onTagFound(path, uid, type, protocol, readOnly, records, payloads) and onTagLost(path)
 * on the main context. A record is "Key=Value" entries joined by \x1f, its MIMEPayload is in payloads.
 */

#include <gio/gio.h>
#include <jni.h>
#include <string.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/com_android_server_nfc_NeardAdapter.h"

#define NEARD "org.neard"
#define NEARD_ADAPTER "org.neard.Adapter"
#define NEARD_TAG "org.neard.Tag"
#define NEARD_RECORD "org.neard.Record"
#define OBJECT_MANAGER "org.freedesktop.DBus.ObjectManager"

static GDBusConnection *system_bus(void)
{
	static GDBusConnection *bus = NULL;
	if (!bus)
		bus = g_bus_get_sync(G_BUS_TYPE_SYSTEM, NULL, NULL);
	return bus;
}

/* a{oa{sa{sv}}} of GetManagedObjects, NULL without neard */
static GVariant *managed_objects(void)
{
	GDBusConnection *bus = system_bus();
	if (!bus)
		return NULL;
	GError *error = NULL;
	GVariant *ret = g_dbus_connection_call_sync(bus, NEARD, "/", OBJECT_MANAGER, "GetManagedObjects", NULL,
	                                            G_VARIANT_TYPE("(a{oa{sa{sv}}})"), G_DBUS_CALL_FLAGS_NONE, 5000, NULL, &error);
	if (!ret) {
		g_debug("neard: %s", error->message);
		g_error_free(error);
		return NULL;
	}
	GVariant *objects = g_variant_get_child_value(ret, 0);
	g_variant_unref(ret);
	return objects;
}

JNIEXPORT jstring JNICALL Java_com_android_server_nfc_NeardAdapter_nativeGetAdapter(JNIEnv *env, jclass clazz)
{
	GVariant *objects = managed_objects();
	if (!objects)
		return NULL;
	GVariantIter iter;
	gchar *path;
	GVariant *interfaces;
	char *found = NULL;
	g_variant_iter_init(&iter, objects);
	while (g_variant_iter_next(&iter, "{o@a{sa{sv}}}", &path, &interfaces)) {
		GVariant *adapter = g_variant_lookup_value(interfaces, NEARD_ADAPTER, NULL);
		if (adapter && (!found || strcmp(path, found) < 0)) {
			g_free(found);
			found = g_strdup(path);
		}
		if (adapter)
			g_variant_unref(adapter);
		g_variant_unref(interfaces);
		g_free(path);
	}
	g_variant_unref(objects);
	jstring ret = found ? (*env)->NewStringUTF(env, found) : NULL;
	g_free(found);
	return ret;
}

static GVariant *get_property(const char *path, const char *interface, const char *name)
{
	GDBusConnection *bus = system_bus();
	if (!bus)
		return NULL;
	GVariant *ret = g_dbus_connection_call_sync(bus, NEARD, path, "org.freedesktop.DBus.Properties", "Get",
	                                            g_variant_new("(ss)", interface, name), G_VARIANT_TYPE("(v)"),
	                                            G_DBUS_CALL_FLAGS_NONE, 5000, NULL, NULL);
	if (!ret)
		return NULL;
	GVariant *value;
	g_variant_get(ret, "(v)", &value);
	g_variant_unref(ret);
	return value;
}

JNIEXPORT jboolean JNICALL Java_com_android_server_nfc_NeardAdapter_nativeIsPowered(JNIEnv *env, jclass clazz, jstring adapter_jstr)
{
	const char *adapter = (*env)->GetStringUTFChars(env, adapter_jstr, NULL);
	GVariant *value = get_property(adapter, NEARD_ADAPTER, "Powered");
	(*env)->ReleaseStringUTFChars(env, adapter_jstr, adapter);
	jboolean ret = value && g_variant_is_of_type(value, G_VARIANT_TYPE_BOOLEAN) && g_variant_get_boolean(value);
	if (value)
		g_variant_unref(value);
	return ret;
}

JNIEXPORT jboolean JNICALL Java_com_android_server_nfc_NeardAdapter_nativeIsTagPresent(JNIEnv *env, jclass clazz, jstring tag_jstr)
{
	const char *tag = (*env)->GetStringUTFChars(env, tag_jstr, NULL);
	GVariant *value = get_property(tag, NEARD_TAG, "Type");
	(*env)->ReleaseStringUTFChars(env, tag_jstr, tag);
	if (value)
		g_variant_unref(value);
	return value != NULL;
}

static int compare_strings(gconstpointer a, gconstpointer b)
{
	return strcmp(*(char **)a, *(char **)b);
}

/* reports the tag at path from the objects of GetManagedObjects */
static void report_tag(JNIEnv *env, jobject listener, GVariant *objects, const char *tag_path, GVariant *tag)
{
	/* the records: children of the tag with org.neard.Record, in path order */
	GPtrArray *paths = g_ptr_array_new_with_free_func(g_free);
	GVariantIter iter;
	gchar *path;
	GVariant *interfaces;
	gchar *prefix = g_strdup_printf("%s/", tag_path);
	g_variant_iter_init(&iter, objects);
	while (g_variant_iter_next(&iter, "{o@a{sa{sv}}}", &path, &interfaces)) {
		GVariant *record = g_variant_lookup_value(interfaces, NEARD_RECORD, NULL);
		if (record && g_str_has_prefix(path, prefix))
			g_ptr_array_add(paths, path);
		else
			g_free(path);
		if (record)
			g_variant_unref(record);
		g_variant_unref(interfaces);
	}
	g_free(prefix);
	g_ptr_array_sort(paths, compare_strings);

	jclass string_class = (*env)->FindClass(env, "java/lang/String");
	jclass bytes_class = (*env)->FindClass(env, "[B");
	jobjectArray records = (*env)->NewObjectArray(env, paths->len, string_class, NULL);
	jobjectArray payloads = (*env)->NewObjectArray(env, paths->len, bytes_class, NULL);
	for (guint i = 0; i < paths->len; i++) {
		GVariant *interfaces_of = NULL;
		g_variant_lookup(objects, paths->pdata[i], "@a{sa{sv}}", &interfaces_of);
		GVariant *props = interfaces_of ? g_variant_lookup_value(interfaces_of, NEARD_RECORD, NULL) : NULL;
		GString *text = g_string_new(NULL);
		if (props) {
			GVariantIter piter;
			const char *key;
			GVariant *value;
			g_variant_iter_init(&piter, props);
			while (g_variant_iter_next(&piter, "{&sv}", &key, &value)) {
				if (g_variant_is_of_type(value, G_VARIANT_TYPE_STRING)) {
					g_string_append_printf(text, "%s%s=%s", text->len ? "\x1f" : "", key, g_variant_get_string(value, NULL));
				} else if (g_variant_is_of_type(value, G_VARIANT_TYPE_UINT32)) {
					g_string_append_printf(text, "%s%s=%u", text->len ? "\x1f" : "", key, g_variant_get_uint32(value));
				} else if (!strcmp(key, "MIMEPayload") && g_variant_is_of_type(value, G_VARIANT_TYPE_BYTESTRING)) {
					gsize len;
					const guint8 *data = g_variant_get_fixed_array(value, &len, 1);
					jbyteArray payload = (*env)->NewByteArray(env, len);
					(*env)->SetByteArrayRegion(env, payload, 0, len, (const jbyte *)data);
					(*env)->SetObjectArrayElement(env, payloads, i, payload);
					(*env)->DeleteLocalRef(env, payload);
				}
				g_variant_unref(value);
			}
			g_variant_unref(props);
		}
		if (interfaces_of)
			g_variant_unref(interfaces_of);
		jstring record = (*env)->NewStringUTF(env, text->str);
		(*env)->SetObjectArrayElement(env, records, i, record);
		(*env)->DeleteLocalRef(env, record);
		g_string_free(text, TRUE);
	}
	g_ptr_array_unref(paths);

	gchar *type = NULL, *protocol = NULL;
	gboolean read_only = FALSE;
	GVariant *uid_v = g_variant_lookup_value(tag, "Uid", G_VARIANT_TYPE_BYTESTRING);
	g_variant_lookup(tag, "Type", "s", &type);
	g_variant_lookup(tag, "Protocol", "s", &protocol);
	g_variant_lookup(tag, "ReadOnly", "b", &read_only);
	gsize uid_len = 0;
	const guint8 *uid_data = uid_v ? g_variant_get_fixed_array(uid_v, &uid_len, 1) : NULL;
	jbyteArray uid = (*env)->NewByteArray(env, uid_len);
	if (uid_len)
		(*env)->SetByteArrayRegion(env, uid, 0, uid_len, (const jbyte *)uid_data);
	if (uid_v)
		g_variant_unref(uid_v);

	jclass class = (*env)->GetObjectClass(env, listener);
	jmethodID method = (*env)->GetMethodID(env, class, "onTagFound", "(Ljava/lang/String;[BLjava/lang/String;Ljava/lang/String;Z[Ljava/lang/String;[[B)V");
	jstring path_jstr = (*env)->NewStringUTF(env, tag_path);
	jstring type_jstr = (*env)->NewStringUTF(env, type ?: "");
	jstring protocol_jstr = (*env)->NewStringUTF(env, protocol ?: "");
	g_free(type);
	g_free(protocol);
	(*env)->CallVoidMethod(env, listener, method, path_jstr, uid, type_jstr, protocol_jstr, read_only, records, payloads);
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionDescribe(env);
	(*env)->DeleteLocalRef(env, path_jstr);
	(*env)->DeleteLocalRef(env, type_jstr);
	(*env)->DeleteLocalRef(env, protocol_jstr);
	(*env)->DeleteLocalRef(env, uid);
	(*env)->DeleteLocalRef(env, records);
	(*env)->DeleteLocalRef(env, payloads);
	(*env)->DeleteLocalRef(env, class);
	(*env)->DeleteLocalRef(env, string_class);
	(*env)->DeleteLocalRef(env, bytes_class);
}

struct poll {
	jobject listener; /* global ref to the NeardAdapter */
	char *adapter;
	guint added;
	guint removed;
};

/* InterfacesAdded fires for the tag before neard has published its records (they come with their own
 * InterfacesAdded right after), so a tag is reported from GetManagedObjects once the signals have settled */
static gboolean report_tags_later(gpointer user_data)
{
	struct poll *p = user_data;
	GVariant *objects = managed_objects();
	if (objects) {
		JNIEnv *env = get_jni_env();
		gchar *prefix = g_strdup_printf("%s/", p->adapter);
		GVariantIter iter;
		gchar *path;
		GVariant *interfaces;
		g_variant_iter_init(&iter, objects);
		while (g_variant_iter_next(&iter, "{o@a{sa{sv}}}", &path, &interfaces)) {
			GVariant *tag = g_variant_lookup_value(interfaces, NEARD_TAG, NULL);
			if (tag && g_str_has_prefix(path, prefix))
				report_tag(env, p->listener, objects, path, tag);
			if (tag)
				g_variant_unref(tag);
			g_variant_unref(interfaces);
			g_free(path);
		}
		g_free(prefix);
		g_variant_unref(objects);
	}
	return G_SOURCE_REMOVE;
}

static void on_interfaces_added(GDBusConnection *bus, const char *sender, const char *path, const char *interface,
                                const char *signal, GVariant *params, gpointer user_data)
{
	struct poll *p = user_data;
	const char *object;
	GVariant *interfaces;
	g_variant_get(params, "(&o@a{sa{sv}})", &object, &interfaces);
	GVariant *tag = g_variant_lookup_value(interfaces, NEARD_TAG, NULL);
	if (tag) {
		g_timeout_add(200, report_tags_later, p);
		g_variant_unref(tag);
	}
	g_variant_unref(interfaces);
}

static void on_interfaces_removed(GDBusConnection *bus, const char *sender, const char *path, const char *interface,
                                  const char *signal, GVariant *params, gpointer user_data)
{
	struct poll *p = user_data;
	const char *object;
	GVariantIter *interfaces;
	const char *name;
	g_variant_get(params, "(&oas)", &object, &interfaces);
	while (g_variant_iter_next(interfaces, "&s", &name)) {
		if (!strcmp(name, NEARD_TAG)) {
			JNIEnv *env = get_jni_env();
			jclass class = (*env)->GetObjectClass(env, p->listener);
			jmethodID method = (*env)->GetMethodID(env, class, "onTagLost", "(Ljava/lang/String;)V");
			jstring object_jstr = (*env)->NewStringUTF(env, object);
			(*env)->CallVoidMethod(env, p->listener, method, object_jstr);
			if ((*env)->ExceptionCheck(env))
				(*env)->ExceptionDescribe(env);
			(*env)->DeleteLocalRef(env, object_jstr);
			(*env)->DeleteLocalRef(env, class);
		}
	}
	g_variant_iter_free(interfaces);
}

static void on_poll_started(GObject *source, GAsyncResult *res, gpointer user_data)
{
	GError *error = NULL;
	GVariant *ret = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source), res, &error);
	if (ret)
		g_variant_unref(ret);
	else {
		/* org.neard.Error.Failed "Already polling" while the loop runs for someone else */
		g_debug("neard: StartPollLoop: %s", error->message);
		g_error_free(error);
	}
}

static gboolean start_poll_in_main(gpointer user_data)
{
	struct poll *p = user_data;
	GDBusConnection *bus = system_bus();
	if (!p->added) {
		p->added = g_dbus_connection_signal_subscribe(bus, NEARD, OBJECT_MANAGER, "InterfacesAdded", "/", NULL,
		                                              G_DBUS_SIGNAL_FLAGS_NONE, on_interfaces_added, p, NULL);
		p->removed = g_dbus_connection_signal_subscribe(bus, NEARD, OBJECT_MANAGER, "InterfacesRemoved", "/", NULL,
		                                                G_DBUS_SIGNAL_FLAGS_NONE, on_interfaces_removed, p, NULL);
	}
	g_dbus_connection_call(bus, NEARD, p->adapter, NEARD_ADAPTER, "StartPollLoop", g_variant_new("(s)", "Initiator"), NULL,
	                       G_DBUS_CALL_FLAGS_NONE, -1, NULL, on_poll_started, NULL);
	return G_SOURCE_REMOVE;
}

static gboolean stop_poll_in_main(gpointer user_data)
{
	struct poll *p = user_data;
	GDBusConnection *bus = system_bus();
	g_dbus_connection_signal_unsubscribe(bus, p->added);
	g_dbus_connection_signal_unsubscribe(bus, p->removed);
	g_dbus_connection_call(bus, NEARD, p->adapter, NEARD_ADAPTER, "StopPollLoop", NULL, NULL, G_DBUS_CALL_FLAGS_NONE,
	                       -1, NULL, NULL, NULL);
	(*get_jni_env())->DeleteGlobalRef(get_jni_env(), p->listener);
	g_free(p->adapter);
	g_free(p);
	return G_SOURCE_REMOVE;
}

/* starts the poll loop (again: neard ends it when a tag is found); returns the poll handle */
JNIEXPORT jlong JNICALL Java_com_android_server_nfc_NeardAdapter_nativeStartPoll(JNIEnv *env, jobject this, jlong handle, jstring adapter_jstr)
{
	struct poll *p = _PTR(handle);
	if (!p) {
		if (!system_bus())
			return 0;
		p = g_new0(struct poll, 1);
		p->listener = (*env)->NewGlobalRef(env, this);
		const char *adapter = (*env)->GetStringUTFChars(env, adapter_jstr, NULL);
		p->adapter = g_strdup(adapter);
		(*env)->ReleaseStringUTFChars(env, adapter_jstr, adapter);
	}
	g_main_context_invoke(NULL, start_poll_in_main, p);
	return _INTPTR(p);
}

JNIEXPORT void JNICALL Java_com_android_server_nfc_NeardAdapter_nativeStopPoll(JNIEnv *env, jobject this, jlong handle)
{
	g_main_context_invoke(NULL, stop_poll_in_main, _PTR(handle));
}
