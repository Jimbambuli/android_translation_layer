#include <gio/gio.h>
#include <gio/gunixfdlist.h>
#include <string.h>
#include <unistd.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/com_android_bluetooth_bluez_BluezBus.h"

/*
 * The system bus connection of BluezBus.java to BlueZ (org.bluez): method calls, the signals of org.bluez, its name
 * owner, and objects ATL exports for BlueZ to call (advertisements, profiles). Java values <-> GVariant as described
 * in BluezBus.java. Asynchronous calls and exported objects run in the GLib default main context, which is the
 * thread of Android's main looper; their results go to Java there.
 */

#define BLUEZ "org.bluez"

static GDBusConnection *bus;
static jclass bus_class;
static jmethodID on_reply, on_signal, on_owner, on_method_call, on_get_property;
static jclass object_class, string_class, boolean_class, integer_class, long_class, double_class, number_class,
    map_class, hashmap_class;
static jmethodID boolean_value_of, integer_value_of, long_value_of, double_value_of, boolean_value, number_int_value,
    number_long_value, number_double_value, hashmap_init, map_put;

/* ---- GVariant -> Java ---- */

static jobject to_java(JNIEnv *env, GVariant *value, GUnixFDList *fds);

static jobjectArray children_to_java(JNIEnv *env, GVariant *value, GUnixFDList *fds)
{
	gsize n = g_variant_n_children(value);
	jobjectArray array = (*env)->NewObjectArray(env, n, object_class, NULL);
	for (gsize i = 0; i < n; i++) {
		GVariant *child = g_variant_get_child_value(value, i);
		jobject object = to_java(env, child, fds);
		(*env)->SetObjectArrayElement(env, array, i, object);
		if (object)
			(*env)->DeleteLocalRef(env, object);
		g_variant_unref(child);
	}
	return array;
}

static jobject to_java(JNIEnv *env, GVariant *value, GUnixFDList *fds)
{
	const GVariantType *type = g_variant_get_type(value);
	switch (g_variant_type_peek_string(type)[0]) {
		case 'b':
			return (*env)->CallStaticObjectMethod(env, boolean_class, boolean_value_of, (jboolean)g_variant_get_boolean(value));
		case 'y':
			return (*env)->CallStaticObjectMethod(env, integer_class, integer_value_of, (jint)g_variant_get_byte(value));
		case 'n':
			return (*env)->CallStaticObjectMethod(env, integer_class, integer_value_of, (jint)g_variant_get_int16(value));
		case 'q':
			return (*env)->CallStaticObjectMethod(env, integer_class, integer_value_of, (jint)g_variant_get_uint16(value));
		case 'i':
			return (*env)->CallStaticObjectMethod(env, integer_class, integer_value_of, (jint)g_variant_get_int32(value));
		case 'h': {
			int fd = -1;
			gint32 index = g_variant_get_handle(value);
			if (fds && index >= 0 && index < g_unix_fd_list_get_length(fds))
				fd = g_unix_fd_list_get(fds, index, NULL); /* a dup */
			return (*env)->CallStaticObjectMethod(env, integer_class, integer_value_of, (jint)fd);
		}
		case 'u':
			return (*env)->CallStaticObjectMethod(env, long_class, long_value_of, (jlong)g_variant_get_uint32(value));
		case 'x':
			return (*env)->CallStaticObjectMethod(env, long_class, long_value_of, (jlong)g_variant_get_int64(value));
		case 't':
			return (*env)->CallStaticObjectMethod(env, long_class, long_value_of, (jlong)g_variant_get_uint64(value));
		case 'd':
			return (*env)->CallStaticObjectMethod(env, double_class, double_value_of, (jdouble)g_variant_get_double(value));
		case 's':
		case 'o':
		case 'g':
			return _JSTRING(g_variant_get_string(value, NULL));
		case 'v': {
			GVariant *inner = g_variant_get_variant(value);
			jobject object = to_java(env, inner, fds);
			g_variant_unref(inner);
			return object;
		}
		case 'a':
			if (g_variant_type_equal(type, G_VARIANT_TYPE_BYTESTRING)) {
				gsize n;
				const guint8 *data = g_variant_get_fixed_array(value, &n, 1);
				jbyteArray array = (*env)->NewByteArray(env, n);
				(*env)->SetByteArrayRegion(env, array, 0, n, (const jbyte *)data);
				return array;
			}
			if (g_variant_type_is_dict_entry(g_variant_type_element(type))) {
				jobject map = (*env)->NewObject(env, hashmap_class, hashmap_init);
				gsize n = g_variant_n_children(value);
				for (gsize i = 0; i < n; i++) {
					GVariant *entry = g_variant_get_child_value(value, i);
					GVariant *k = g_variant_get_child_value(entry, 0);
					GVariant *v = g_variant_get_child_value(entry, 1);
					jobject jk = to_java(env, k, fds);
					jobject jv = to_java(env, v, fds);
					jobject old = (*env)->CallObjectMethod(env, map, map_put, jk, jv);
					if (old)
						(*env)->DeleteLocalRef(env, old);
					if (jk)
						(*env)->DeleteLocalRef(env, jk);
					if (jv)
						(*env)->DeleteLocalRef(env, jv);
					g_variant_unref(k);
					g_variant_unref(v);
					g_variant_unref(entry);
				}
				return map;
			}
			return children_to_java(env, value, fds);
		case '(':
			return children_to_java(env, value, fds);
		default:
			return NULL;
	}
}

/* ---- Java -> GVariant (floating), NULL with an exception thrown if the object does not fit the type ---- */

static GVariant *to_variant(JNIEnv *env, jobject object, const GVariantType *type);

static jboolean is_a(JNIEnv *env, jobject object, jclass class)
{
	return object && (*env)->IsInstanceOf(env, object, class);
}

static GVariant *mismatch(JNIEnv *env, const GVariantType *type)
{
	if (!(*env)->ExceptionCheck(env)) {
		gchar *text = g_strdup_printf("value does not fit D-Bus type %.*s", (int)g_variant_type_get_string_length(type),
		                              g_variant_type_peek_string(type));
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"), text);
		g_free(text);
	}
	return NULL;
}

/* elements of a tuple or an array from an Object[] */
static GVariant *array_to_variant(JNIEnv *env, jobjectArray array, const GVariantType *type)
{
	gboolean tuple = g_variant_type_is_tuple(type);
	gboolean dict = !tuple && g_variant_type_is_dict_entry(g_variant_type_element(type));
	jsize n = (*env)->GetArrayLength(env, array);
	GVariantBuilder builder;
	g_variant_builder_init(&builder, type);
	const GVariantType *element = tuple ? g_variant_type_first(type) : g_variant_type_element(type);
	if (dict) {
		const GVariantType *key_type = g_variant_type_key(element);
		const GVariantType *value_type = g_variant_type_value(element);
		for (jsize i = 0; i + 1 < n; i += 2) {
			jobject k = (*env)->GetObjectArrayElement(env, array, i);
			jobject v = (*env)->GetObjectArrayElement(env, array, i + 1);
			GVariant *vk = to_variant(env, k, key_type);
			GVariant *vv = vk ? to_variant(env, v, value_type) : NULL;
			if (k)
				(*env)->DeleteLocalRef(env, k);
			if (v)
				(*env)->DeleteLocalRef(env, v);
			if (!vv) {
				if (vk)
					g_variant_unref(g_variant_ref_sink(vk));
				g_variant_builder_clear(&builder);
				return NULL;
			}
			g_variant_builder_add_value(&builder, g_variant_new_dict_entry(vk, vv));
		}
		return g_variant_builder_end(&builder);
	}
	for (jsize i = 0; i < n; i++) {
		if (tuple && !element) {
			g_variant_builder_clear(&builder);
			return mismatch(env, type);
		}
		jobject o = (*env)->GetObjectArrayElement(env, array, i);
		GVariant *v = to_variant(env, o, element);
		if (o)
			(*env)->DeleteLocalRef(env, o);
		if (!v) {
			g_variant_builder_clear(&builder);
			return NULL;
		}
		g_variant_builder_add_value(&builder, v);
		if (tuple)
			element = g_variant_type_next(element);
	}
	if (tuple && element) {
		g_variant_builder_clear(&builder);
		return mismatch(env, type);
	}
	return g_variant_builder_end(&builder);
}

static GVariant *to_variant(JNIEnv *env, jobject object, const GVariantType *type)
{
	switch (g_variant_type_peek_string(type)[0]) {
		case 'b':
			if (!is_a(env, object, boolean_class))
				return mismatch(env, type);
			return g_variant_new_boolean((*env)->CallBooleanMethod(env, object, boolean_value));
		case 'y':
		case 'n':
		case 'q':
		case 'i':
		case 'h':
		case 'u':
		case 'x':
		case 't': {
			if (!is_a(env, object, number_class))
				return mismatch(env, type);
			jlong l = (*env)->CallLongMethod(env, object, number_long_value);
			switch (g_variant_type_peek_string(type)[0]) {
				case 'y': return g_variant_new_byte(l);
				case 'n': return g_variant_new_int16(l);
				case 'q': return g_variant_new_uint16(l);
				case 'i': return g_variant_new_int32(l);
				case 'h': return g_variant_new_handle(l);
				case 'u': return g_variant_new_uint32(l);
				case 'x': return g_variant_new_int64(l);
				default: return g_variant_new_uint64(l);
			}
		}
		case 'd':
			if (!is_a(env, object, number_class))
				return mismatch(env, type);
			return g_variant_new_double((*env)->CallDoubleMethod(env, object, number_double_value));
		case 's':
		case 'o':
		case 'g': {
			if (!is_a(env, object, string_class))
				return mismatch(env, type);
			const char *s = _CSTRING((jstring)object);
			GVariant *v = NULL;
			char c = g_variant_type_peek_string(type)[0];
			if (c == 's')
				v = g_variant_new_string(s);
			else if (c == 'o' && g_variant_is_object_path(s))
				v = g_variant_new_object_path(s);
			else if (c == 'g' && g_variant_is_signature(s))
				v = g_variant_new_signature(s);
			(*env)->ReleaseStringUTFChars(env, (jstring)object, s);
			return v ? v : mismatch(env, type);
		}
		case 'v': {
			/* Object[]{String signature, Object value} */
			if (!object || !(*env)->IsInstanceOf(env, object, (*env)->FindClass(env, "[Ljava/lang/Object;")) ||
			    (*env)->GetArrayLength(env, object) != 2)
				return mismatch(env, type);
			jstring jsig = (*env)->GetObjectArrayElement(env, object, 0);
			jobject inner = (*env)->GetObjectArrayElement(env, object, 1);
			if (!is_a(env, jsig, string_class))
				return mismatch(env, type);
			const char *sig = _CSTRING(jsig);
			GVariant *v = NULL;
			if (g_variant_type_string_is_valid(sig))
				v = to_variant(env, inner, G_VARIANT_TYPE(sig));
			else
				mismatch(env, type);
			(*env)->ReleaseStringUTFChars(env, jsig, sig);
			return v ? g_variant_new_variant(v) : NULL;
		}
		case 'a':
			if (g_variant_type_equal(type, G_VARIANT_TYPE_BYTESTRING)) {
				if (!object || !(*env)->IsInstanceOf(env, object, (*env)->FindClass(env, "[B")))
					return mismatch(env, type);
				jsize n = (*env)->GetArrayLength(env, object);
				jbyte *data = (*env)->GetByteArrayElements(env, object, NULL);
				GVariant *v = g_variant_new_fixed_array(G_VARIANT_TYPE_BYTE, data, n, 1);
				(*env)->ReleaseByteArrayElements(env, object, data, JNI_ABORT);
				return v;
			}
			/* fall through */
		case '(':
			if (!object)
				return g_variant_type_is_tuple(type) ? mismatch(env, type) : g_variant_new_array(g_variant_type_element(type), NULL, 0);
			if (!(*env)->IsInstanceOf(env, object, (*env)->FindClass(env, "[Ljava/lang/Object;")))
				return mismatch(env, type);
			return array_to_variant(env, object, type);
		default:
			return mismatch(env, type);
	}
}

static GVariant *args_variant(JNIEnv *env, jstring jsig, jobjectArray args)
{
	if (!jsig)
		return NULL;
	const char *sig = _CSTRING(jsig);
	GVariant *v = NULL;
	if (g_variant_type_string_is_valid(sig) && g_variant_type_is_tuple(G_VARIANT_TYPE(sig))) {
		v = to_variant(env, args ? args : (*env)->NewObjectArray(env, 0, object_class, NULL), G_VARIANT_TYPE(sig));
	} else {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"), "signature must be a tuple");
	}
	(*env)->ReleaseStringUTFChars(env, jsig, sig);
	return v;
}

/* ---- signals and name owner ---- */

static void signal_cb(GDBusConnection *connection, const gchar *sender, const gchar *path, const gchar *iface,
                      const gchar *member, GVariant *parameters, gpointer data)
{
	JNIEnv *env = get_jni_env();
	jobjectArray args = children_to_java(env, parameters, NULL);
	jstring jpath = _JSTRING(path), jiface = _JSTRING(iface), jmember = _JSTRING(member);
	(*env)->CallStaticVoidMethod(env, bus_class, on_signal, jpath, jiface, jmember, args);
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionDescribe(env);
	(*env)->DeleteLocalRef(env, args);
	(*env)->DeleteLocalRef(env, jpath);
	(*env)->DeleteLocalRef(env, jiface);
	(*env)->DeleteLocalRef(env, jmember);
}

static void owner_cb(GDBusConnection *connection, const gchar *name, const gchar *owner, gpointer data)
{
	JNIEnv *env = get_jni_env();
	(*env)->CallStaticVoidMethod(env, bus_class, on_owner, (jboolean)(owner != NULL));
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionDescribe(env);
}

static void name_appeared(GDBusConnection *connection, const gchar *name, const gchar *owner, gpointer data)
{
	owner_cb(connection, name, owner, data);
}

static void name_vanished(GDBusConnection *connection, const gchar *name, gpointer data)
{
	owner_cb(connection, name, NULL, data);
}

/* GDBus delivers signals and calls to exported objects in the thread-default context of the subscribing or
 * registering thread: subscribe and register in the default context, on the main thread (waiting for it when an app
 * thread asks) */
struct on_main {
	void (*func)(void *data);
	void *data;
	GMutex mutex;
	GCond cond;
	gboolean done;
};

static gboolean on_main_run(gpointer p)
{
	struct on_main *m = p;
	m->func(m->data);
	g_mutex_lock(&m->mutex);
	m->done = TRUE;
	g_cond_signal(&m->cond);
	g_mutex_unlock(&m->mutex);
	return G_SOURCE_REMOVE;
}

static void run_on_main(void (*func)(void *data), void *data)
{
	GMainContext *context = g_main_context_default();
	if (g_main_context_acquire(context)) {
		func(data);
		g_main_context_release(context);
		return;
	}
	struct on_main m = {func, data};
	g_mutex_init(&m.mutex);
	g_cond_init(&m.cond);
	g_main_context_invoke(context, on_main_run, &m);
	g_mutex_lock(&m.mutex);
	while (!m.done)
		g_cond_wait(&m.cond, &m.mutex);
	g_mutex_unlock(&m.mutex);
	g_mutex_clear(&m.mutex);
	g_cond_clear(&m.cond);
}

static void subscribe(void *data)
{
	g_dbus_connection_signal_subscribe(bus, BLUEZ, NULL, NULL, NULL, NULL, G_DBUS_SIGNAL_FLAGS_NONE, signal_cb, NULL, NULL);
	g_bus_watch_name_on_connection(bus, BLUEZ, G_BUS_NAME_WATCHER_FLAGS_NONE, name_appeared, name_vanished, NULL, NULL);
}

static jclass global_class(JNIEnv *env, const char *name)
{
	jclass local = (*env)->FindClass(env, name);
	jclass global = _REF(local);
	(*env)->DeleteLocalRef(env, local);
	return global;
}

JNIEXPORT jboolean JNICALL Java_com_android_bluetooth_bluez_BluezBus_nativeInit(JNIEnv *env, jclass class)
{
	g_autoptr(GError) error = NULL;
	bus_class = _REF(class);
	on_reply = _STATIC_METHOD(class, "onReply", "(I[Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V");
	on_signal = _STATIC_METHOD(class, "onSignal", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V");
	on_owner = _STATIC_METHOD(class, "onOwner", "(Z)V");
	on_method_call = _STATIC_METHOD(class, "onMethodCall", "(IJLjava/lang/String;[Ljava/lang/Object;)V");
	on_get_property = _STATIC_METHOD(class, "onGetProperty", "(ILjava/lang/String;)Ljava/lang/Object;");
	object_class = global_class(env, "java/lang/Object");
	string_class = global_class(env, "java/lang/String");
	boolean_class = global_class(env, "java/lang/Boolean");
	integer_class = global_class(env, "java/lang/Integer");
	long_class = global_class(env, "java/lang/Long");
	double_class = global_class(env, "java/lang/Double");
	number_class = global_class(env, "java/lang/Number");
	map_class = global_class(env, "java/util/Map");
	hashmap_class = global_class(env, "java/util/HashMap");
	boolean_value_of = _STATIC_METHOD(boolean_class, "valueOf", "(Z)Ljava/lang/Boolean;");
	integer_value_of = _STATIC_METHOD(integer_class, "valueOf", "(I)Ljava/lang/Integer;");
	long_value_of = _STATIC_METHOD(long_class, "valueOf", "(J)Ljava/lang/Long;");
	double_value_of = _STATIC_METHOD(double_class, "valueOf", "(D)Ljava/lang/Double;");
	boolean_value = _METHOD(boolean_class, "booleanValue", "()Z");
	number_int_value = _METHOD(number_class, "intValue", "()I");
	number_long_value = _METHOD(number_class, "longValue", "()J");
	number_double_value = _METHOD(number_class, "doubleValue", "()D");
	hashmap_init = _METHOD(hashmap_class, "<init>", "()V");
	map_put = _METHOD(map_class, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

	bus = g_bus_get_sync(G_BUS_TYPE_SYSTEM, NULL, &error);
	if (!bus) {
		android_log_printf(ANDROID_LOG_INFO, "BluezBus", "no system bus: %s", error->message);
		return JNI_FALSE;
	}
	run_on_main(subscribe, NULL);
	return JNI_TRUE;
}

/* ---- calls ---- */

static jobjectArray reply_to_java(JNIEnv *env, GVariant *reply, GUnixFDList *fds)
{
	return reply ? children_to_java(env, reply, fds) : (*env)->NewObjectArray(env, 0, object_class, NULL);
}

static void error_parts(GError *error, gchar **name, gchar **message)
{
	*name = g_dbus_error_get_remote_error(error);
	if (!*name)
		*name = g_strdup(error->domain == G_IO_ERROR && error->code == G_IO_ERROR_TIMED_OUT ? "org.freedesktop.DBus.Error.NoReply"
		                                                                                    : "org.freedesktop.DBus.Error.Failed");
	g_dbus_error_strip_remote_error(error);
	*message = g_strdup(error->message);
}

JNIEXPORT jobject JNICALL Java_com_android_bluetooth_bluez_BluezBus_nativeCallSync(JNIEnv *env, jclass class, jstring jpath, jstring jiface,
                                                                                   jstring jmethod, jstring jsig, jobjectArray args, jint timeout)
{
	GVariant *parameters = args_variant(env, jsig, args);
	if ((*env)->ExceptionCheck(env))
		return NULL;
	const char *path = _CSTRING(jpath), *iface = _CSTRING(jiface), *method = _CSTRING(jmethod);
	g_autoptr(GError) error = NULL;
	GVariant *reply = g_dbus_connection_call_sync(bus, BLUEZ, path, iface, method, parameters, NULL, G_DBUS_CALL_FLAGS_NONE,
	                                              timeout, NULL, &error);
	(*env)->ReleaseStringUTFChars(env, jpath, path);
	(*env)->ReleaseStringUTFChars(env, jiface, iface);
	(*env)->ReleaseStringUTFChars(env, jmethod, method);
	if (!reply) {
		gchar *name, *message;
		error_parts(error, &name, &message);
		gchar *text = g_strdup_printf("%s: %s", name, message);
		jstring result = _JSTRING(text);
		g_free(text);
		g_free(name);
		g_free(message);
		return result;
	}
	jobject result = reply_to_java(env, reply, NULL);
	g_variant_unref(reply);
	return result;
}

struct call {
	int token;
	char *path, *iface, *method;
	GVariant *parameters;
	int timeout;
};

static void call_done(GObject *source, GAsyncResult *result, gpointer data)
{
	int token = GPOINTER_TO_INT(data);
	g_autoptr(GError) error = NULL;
	GUnixFDList *fds = NULL;
	GVariant *reply = g_dbus_connection_call_with_unix_fd_list_finish(G_DBUS_CONNECTION(source), &fds, result, &error);
	if (!token) {
		if (reply)
			g_variant_unref(reply);
		g_clear_object(&fds);
		return;
	}
	JNIEnv *env = get_jni_env();
	if (reply) {
		jobjectArray values = reply_to_java(env, reply, fds);
		(*env)->CallStaticVoidMethod(env, bus_class, on_reply, token, values, NULL, NULL);
		(*env)->DeleteLocalRef(env, values);
		g_variant_unref(reply);
	} else {
		gchar *name, *message;
		error_parts(error, &name, &message);
		jstring jname = _JSTRING(name), jmessage = _JSTRING(message);
		(*env)->CallStaticVoidMethod(env, bus_class, on_reply, token, NULL, jname, jmessage);
		(*env)->DeleteLocalRef(env, jname);
		(*env)->DeleteLocalRef(env, jmessage);
		g_free(name);
		g_free(message);
	}
	g_clear_object(&fds);
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionDescribe(env);
}

static gboolean start_call(gpointer data)
{
	struct call *call = data;
	g_dbus_connection_call_with_unix_fd_list(bus, BLUEZ, call->path, call->iface, call->method, call->parameters, NULL,
	                                         G_DBUS_CALL_FLAGS_NONE, call->timeout, NULL, NULL, call_done, GINT_TO_POINTER(call->token));
	if (call->parameters)
		g_variant_unref(call->parameters); /* GDBus took its own reference */
	g_free(call->path);
	g_free(call->iface);
	g_free(call->method);
	g_free(call);
	return G_SOURCE_REMOVE;
}

JNIEXPORT void JNICALL Java_com_android_bluetooth_bluez_BluezBus_nativeCall(JNIEnv *env, jclass class, jint token, jstring jpath, jstring jiface,
                                                                            jstring jmethod, jstring jsig, jobjectArray args, jint timeout)
{
	GVariant *parameters = args_variant(env, jsig, args);
	if ((*env)->ExceptionCheck(env))
		return;
	struct call *call = g_new0(struct call, 1);
	const char *s;
	call->token = token;
	call->timeout = timeout;
	call->parameters = parameters ? g_variant_ref_sink(parameters) : NULL;
	s = _CSTRING(jpath);
	call->path = g_strdup(s);
	(*env)->ReleaseStringUTFChars(env, jpath, s);
	s = _CSTRING(jiface);
	call->iface = g_strdup(s);
	(*env)->ReleaseStringUTFChars(env, jiface, s);
	s = _CSTRING(jmethod);
	call->method = g_strdup(s);
	(*env)->ReleaseStringUTFChars(env, jmethod, s);
	/* the reply arrives in the context the call starts in: start it in the default context (the main looper's) */
	g_main_context_invoke_full(g_main_context_default(), G_PRIORITY_DEFAULT, start_call, call, NULL);
}

/* ---- exported objects ---- */

struct export {
	int id;
	guint registration;
	GDBusNodeInfo *node;
};

static GHashTable *exports; /* id -> struct export */
static int next_export = 1;

static void exported_method_call(GDBusConnection *connection, const gchar *sender, const gchar *path, const gchar *iface,
                                 const gchar *method, GVariant *parameters, GDBusMethodInvocation *invocation, gpointer data)
{
	JNIEnv *env = get_jni_env();
	GUnixFDList *fds = g_dbus_message_get_unix_fd_list(g_dbus_method_invocation_get_message(invocation));
	jobjectArray args = children_to_java(env, parameters, fds);
	jstring jmethod = _JSTRING(method);
	/* Java answers with nativeReturn/nativeReturnError, which take this reference */
	(*env)->CallStaticVoidMethod(env, bus_class, on_method_call, GPOINTER_TO_INT(data), _INTPTR(g_object_ref(invocation)), jmethod, args);
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionDescribe(env);
	(*env)->DeleteLocalRef(env, args);
	(*env)->DeleteLocalRef(env, jmethod);
	g_object_unref(invocation); /* the reference GDBus handed over */
}

static GVariant *exported_get_property(GDBusConnection *connection, const gchar *sender, const gchar *path, const gchar *iface,
                                       const gchar *property, GError **error, gpointer data)
{
	JNIEnv *env = get_jni_env();
	struct export *export = g_hash_table_lookup(exports, data);
	GDBusPropertyInfo *info = export ? g_dbus_interface_info_lookup_property(export->node->interfaces[0], property) : NULL;
	jstring jname = _JSTRING(property);
	jobject value = (*env)->CallStaticObjectMethod(env, bus_class, on_get_property, GPOINTER_TO_INT(data), jname);
	(*env)->DeleteLocalRef(env, jname);
	GVariant *v = NULL;
	if (!(*env)->ExceptionCheck(env) && value && info)
		v = to_variant(env, value, G_VARIANT_TYPE(info->signature));
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		v = NULL;
	}
	if (value)
		(*env)->DeleteLocalRef(env, value);
	if (!v)
		g_set_error(error, G_DBUS_ERROR, G_DBUS_ERROR_INVALID_ARGS, "no property %s", property);
	return v;
}

static const GDBusInterfaceVTable vtable = {exported_method_call, exported_get_property, NULL};

struct registration {
	const char *path;
	GDBusInterfaceInfo *info;
	gpointer id;
	guint registration;
	GError *error;
};

static void register_object(void *data)
{
	struct registration *r = data;
	r->registration = g_dbus_connection_register_object(bus, r->path, r->info, &vtable, r->id, NULL, &r->error);
}

JNIEXPORT jint JNICALL Java_com_android_bluetooth_bluez_BluezBus_nativeExport(JNIEnv *env, jclass class, jstring jpath, jstring jxml)
{
	g_autoptr(GError) error = NULL;
	const char *xml = _CSTRING(jxml);
	GDBusNodeInfo *node = g_dbus_node_info_new_for_xml(xml, &error);
	(*env)->ReleaseStringUTFChars(env, jxml, xml);
	if (!node || !node->interfaces || !node->interfaces[0]) {
		android_log_printf(ANDROID_LOG_ERROR, "BluezBus", "bad introspection data: %s", error ? error->message : "no interface");
		if (node)
			g_dbus_node_info_unref(node);
		return 0;
	}
	if (!exports)
		exports = g_hash_table_new(NULL, NULL);
	int id = next_export++;
	const char *path = _CSTRING(jpath);
	struct registration r = {path, node->interfaces[0], GINT_TO_POINTER(id), 0, NULL};
	run_on_main(register_object, &r);
	guint registration = r.registration;
	if (r.error)
		g_propagate_error(&error, r.error);
	(*env)->ReleaseStringUTFChars(env, jpath, path);
	if (!registration) {
		android_log_printf(ANDROID_LOG_ERROR, "BluezBus", "cannot export object: %s", error->message);
		g_dbus_node_info_unref(node);
		return 0;
	}
	struct export *export = g_new0(struct export, 1);
	export->id = id;
	export->registration = registration;
	export->node = node;
	g_hash_table_insert(exports, GINT_TO_POINTER(id), export);
	return id;
}

JNIEXPORT void JNICALL Java_com_android_bluetooth_bluez_BluezBus_nativeUnexport(JNIEnv *env, jclass class, jint id)
{
	struct export *export = exports ? g_hash_table_lookup(exports, GINT_TO_POINTER(id)) : NULL;
	if (!export)
		return;
	g_hash_table_remove(exports, GINT_TO_POINTER(id));
	g_dbus_connection_unregister_object(bus, export->registration);
	g_dbus_node_info_unref(export->node);
	g_free(export);
}

JNIEXPORT void JNICALL Java_com_android_bluetooth_bluez_BluezBus_nativeReturn(JNIEnv *env, jclass class, jlong jinvocation, jobjectArray values)
{
	GDBusMethodInvocation *invocation = _PTR(jinvocation);
	const GDBusMethodInfo *info = g_dbus_method_invocation_get_method_info(invocation);
	GString *sig = g_string_new("(");
	for (int i = 0; info && info->out_args && info->out_args[i]; i++)
		g_string_append(sig, info->out_args[i]->signature);
	g_string_append_c(sig, ')');
	GVariant *v = to_variant(env, values ? values : (*env)->NewObjectArray(env, 0, object_class, NULL), G_VARIANT_TYPE(sig->str));
	g_string_free(sig, TRUE);
	if (!v) {
		g_dbus_method_invocation_return_dbus_error(invocation, "org.freedesktop.DBus.Error.Failed", "bad reply");
		return; /* the Java exception stays pending */
	}
	g_dbus_method_invocation_return_value(invocation, v); /* takes the reference */
}

JNIEXPORT void JNICALL Java_com_android_bluetooth_bluez_BluezBus_nativeReturnError(JNIEnv *env, jclass class, jlong jinvocation, jstring jname,
                                                                                   jstring jmessage)
{
	GDBusMethodInvocation *invocation = _PTR(jinvocation);
	const char *name = _CSTRING(jname), *message = _CSTRING(jmessage);
	g_dbus_method_invocation_return_dbus_error(invocation, name, message); /* takes the reference */
	(*env)->ReleaseStringUTFChars(env, jname, name);
	(*env)->ReleaseStringUTFChars(env, jmessage, message);
}
