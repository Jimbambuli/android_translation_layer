/*
 * The fingerprint sensor of ATL: fprintd (net.reactivated.Fprint) on the system bus.
 *
 * fprintd D-Bus API (fprintd 1.94, src/device.xml, src/manager.xml): Manager.GetDefaultDevice() -> o
 * (net.reactivated.Fprint.Error.NoSuchDevice without a reader), Device.ListEnrolledFingers(s user),
 * Device.Claim(s user), Device.VerifyStart(s finger), signal Device.VerifyStatus(s result, b done),
 * Device.VerifyStop(), Device.Release(). An empty user name is the caller.
 *
 * The verify session runs on the default main context (the main looper of ATL); the results go to
 * FprintdSensor.Session.onStatus(String result, boolean done), D-Bus errors as onStatus("error:<name>", true).
 */

#include <gio/gio.h>
#include <jni.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/com_android_server_biometrics_FprintdSensor.h"

#define FPRINT_NAME "net.reactivated.Fprint"
#define FPRINT_MANAGER_PATH "/net/reactivated/Fprint/Manager"
#define FPRINT_MANAGER "net.reactivated.Fprint.Manager"
#define FPRINT_DEVICE "net.reactivated.Fprint.Device"

/* states, as in FprintdSensor.java */
#define STATE_NO_HARDWARE 0
#define STATE_NOT_ENROLLED 1
#define STATE_ENROLLED 2

static GDBusConnection *system_bus(void)
{
	static GDBusConnection *bus = NULL;
	if (!bus)
		bus = g_bus_get_sync(G_BUS_TYPE_SYSTEM, NULL, NULL);
	return bus;
}

static gboolean error_is(GError *error, const char *name)
{
	gchar *remote = g_dbus_error_get_remote_error(error);
	gboolean ret = !g_strcmp0(remote, name);
	g_free(remote);
	return ret;
}

static char *default_device(GDBusConnection *bus, GError **error)
{
	GVariant *ret = g_dbus_connection_call_sync(bus, FPRINT_NAME, FPRINT_MANAGER_PATH, FPRINT_MANAGER, "GetDefaultDevice",
	                                            NULL, G_VARIANT_TYPE("(o)"), G_DBUS_CALL_FLAGS_NONE, 5000, NULL, error);
	if (!ret)
		return NULL;
	char *path;
	g_variant_get(ret, "(o)", &path);
	g_variant_unref(ret);
	return path;
}

JNIEXPORT jint JNICALL Java_com_android_server_biometrics_FprintdSensor_nativeGetState(JNIEnv *env, jclass clazz)
{
	GDBusConnection *bus = system_bus();
	GError *error = NULL;
	if (!bus)
		return STATE_NO_HARDWARE;
	char *path = default_device(bus, &error);
	if (!path) {
		g_debug("fprintd: no device (%s)", error->message);
		g_error_free(error);
		return STATE_NO_HARDWARE;
	}
	GVariant *ret = g_dbus_connection_call_sync(bus, FPRINT_NAME, path, FPRINT_DEVICE, "ListEnrolledFingers",
	                                            g_variant_new("(s)", ""), G_VARIANT_TYPE("(as)"), G_DBUS_CALL_FLAGS_NONE,
	                                            5000, NULL, &error);
	g_free(path);
	if (!ret) {
		/* NoEnrolledPrints when the user has none; any other error leaves the reader unusable for this user */
		jint state = error_is(error, FPRINT_NAME ".Error.NoEnrolledPrints") ? STATE_NOT_ENROLLED : STATE_NO_HARDWARE;
		g_debug("fprintd: ListEnrolledFingers: %s", error->message);
		g_error_free(error);
		return state;
	}
	GVariant *fingers = g_variant_get_child_value(ret, 0);
	jint state = g_variant_n_children(fingers) > 0 ? STATE_ENROLLED : STATE_NOT_ENROLLED;
	g_variant_unref(fingers);
	g_variant_unref(ret);
	return state;
}

struct session {
	gint refs;
	jobject listener; /* global ref to the FprintdSensor.Session */
	GDBusConnection *bus;
	char *device;
	guint subscription;
	gboolean claimed;
	gboolean verifying;
	gboolean stopped;
};

static struct session *session_ref(struct session *s)
{
	g_atomic_int_inc(&s->refs);
	return s;
}

static void session_unref(struct session *s)
{
	if (!g_atomic_int_dec_and_test(&s->refs))
		return;
	(*get_jni_env())->DeleteGlobalRef(get_jni_env(), s->listener);
	g_free(s->device);
	g_free(s);
}

static void report(struct session *s, const char *result, gboolean done)
{
	if (s->stopped)
		return;
	JNIEnv *env = get_jni_env();
	jclass class = (*env)->GetObjectClass(env, s->listener);
	jmethodID method = (*env)->GetMethodID(env, class, "onStatus", "(Ljava/lang/String;Z)V");
	jstring result_jstr = (*env)->NewStringUTF(env, result);
	(*env)->CallVoidMethod(env, s->listener, method, result_jstr, done);
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionDescribe(env);
	(*env)->DeleteLocalRef(env, result_jstr);
	(*env)->DeleteLocalRef(env, class);
}

static void report_error(struct session *s, GError *error)
{
	gchar *remote = g_dbus_error_get_remote_error(error);
	gchar *result = g_strdup_printf("error:%s", remote ?: "");
	g_debug("fprintd: %s", error->message);
	report(s, result, TRUE);
	g_free(result);
	g_free(remote);
}

static void call(struct session *s, const char *method, GVariant *args, GAsyncReadyCallback callback)
{
	g_dbus_connection_call(s->bus, FPRINT_NAME, s->device, FPRINT_DEVICE, method, args, NULL, G_DBUS_CALL_FLAGS_NONE,
	                       -1, NULL, callback, session_ref(s));
}

static void on_ignored_reply(GObject *source, GAsyncResult *res, gpointer user_data)
{
	GVariant *ret = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source), res, NULL);
	if (ret)
		g_variant_unref(ret);
	session_unref(user_data);
}

static void on_verify_started(GObject *source, GAsyncResult *res, gpointer user_data)
{
	struct session *s = user_data;
	GError *error = NULL;
	GVariant *ret = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source), res, &error);
	if (ret) {
		g_variant_unref(ret);
		s->verifying = TRUE;
		if (s->stopped) {
			s->verifying = FALSE;
			call(s, "VerifyStop", NULL, on_ignored_reply);
		}
	} else {
		report_error(s, error);
		g_error_free(error);
	}
	session_unref(s);
}

static void on_claimed(GObject *source, GAsyncResult *res, gpointer user_data)
{
	struct session *s = user_data;
	GError *error = NULL;
	GVariant *ret = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source), res, &error);
	if (ret) {
		g_variant_unref(ret);
		s->claimed = TRUE;
		if (s->stopped) {
			s->claimed = FALSE;
			call(s, "Release", NULL, on_ignored_reply);
		} else {
			call(s, "VerifyStart", g_variant_new("(s)", "any"), on_verify_started);
		}
	} else {
		report_error(s, error);
		g_error_free(error);
	}
	session_unref(s);
}

static void on_verify_status(GDBusConnection *bus, const char *sender, const char *path, const char *interface,
                             const char *signal, GVariant *params, gpointer user_data)
{
	struct session *s = user_data;
	const char *result;
	gboolean done;
	g_variant_get(params, "(&sb)", &result, &done);
	if (done)
		s->verifying = FALSE;
	report(s, result, done);
}

static void on_device(GObject *source, GAsyncResult *res, gpointer user_data)
{
	struct session *s = user_data;
	GError *error = NULL;
	GVariant *ret = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source), res, &error);
	if (ret) {
		g_variant_get(ret, "(o)", &s->device);
		g_variant_unref(ret);
		if (!s->stopped) {
			s->subscription = g_dbus_connection_signal_subscribe(s->bus, FPRINT_NAME, FPRINT_DEVICE, "VerifyStatus", s->device,
			                                                     NULL, G_DBUS_SIGNAL_FLAGS_NONE, on_verify_status, s, NULL);
			call(s, "Claim", g_variant_new("(s)", ""), on_claimed);
		}
	} else {
		report_error(s, error);
		g_error_free(error);
	}
	session_unref(s);
}

static gboolean start_in_main(gpointer user_data)
{
	struct session *s = user_data;
	g_dbus_connection_call(s->bus, FPRINT_NAME, FPRINT_MANAGER_PATH, FPRINT_MANAGER, "GetDefaultDevice", NULL,
	                       G_VARIANT_TYPE("(o)"), G_DBUS_CALL_FLAGS_NONE, -1, NULL, on_device, session_ref(s));
	session_unref(s);
	return G_SOURCE_REMOVE;
}

static void on_verify_stopped(GObject *source, GAsyncResult *res, gpointer user_data)
{
	struct session *s = user_data;
	GVariant *ret = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source), res, NULL);
	if (ret)
		g_variant_unref(ret);
	if (!s->stopped && s->claimed)
		call(s, "VerifyStart", g_variant_new("(s)", "any"), on_verify_started);
	session_unref(s);
}

static gboolean restart_in_main(gpointer user_data)
{
	struct session *s = user_data;
	/* after a "done" status fprintd wants VerifyStop before the next VerifyStart */
	if (!s->stopped && s->claimed)
		call(s, "VerifyStop", NULL, on_verify_stopped);
	session_unref(s);
	return G_SOURCE_REMOVE;
}

static gboolean stop_in_main(gpointer user_data)
{
	struct session *s = user_data;
	s->stopped = TRUE;
	if (s->subscription) {
		g_dbus_connection_signal_unsubscribe(s->bus, s->subscription);
		s->subscription = 0;
	}
	if (s->verifying) {
		s->verifying = FALSE;
		call(s, "VerifyStop", NULL, on_ignored_reply);
	}
	if (s->claimed) {
		s->claimed = FALSE;
		call(s, "Release", NULL, on_ignored_reply);
	}
	session_unref(s); /* the reference of nativeStart */
	session_unref(s); /* the reference of this call */
	return G_SOURCE_REMOVE;
}

JNIEXPORT jlong JNICALL Java_com_android_server_biometrics_FprintdSensor_nativeStart(JNIEnv *env, jclass clazz, jobject listener)
{
	GDBusConnection *bus = system_bus();
	if (!bus)
		return 0;
	struct session *s = g_new0(struct session, 1);
	s->refs = 2; /* one for the caller (released by nativeStop), one for start_in_main */
	s->listener = (*env)->NewGlobalRef(env, listener);
	s->bus = bus;
	g_main_context_invoke(NULL, start_in_main, s);
	return _INTPTR(s);
}

JNIEXPORT void JNICALL Java_com_android_server_biometrics_FprintdSensor_nativeRestart(JNIEnv *env, jclass clazz, jlong ptr)
{
	struct session *s = _PTR(ptr);
	g_main_context_invoke(NULL, restart_in_main, session_ref(s));
}

JNIEXPORT void JNICALL Java_com_android_server_biometrics_FprintdSensor_nativeStop(JNIEnv *env, jclass clazz, jlong ptr)
{
	struct session *s = _PTR(ptr);
	g_main_context_invoke(NULL, stop_in_main, session_ref(s));
}
