#include <gio/gio.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_net_ConnectivityManager.h"

/*
 * GNetworkMonitor lives on its own thread with its own GMainContext.
 *
 * The portal implementation used inside Flatpak (GNetworkMonitorPortal) starts
 * with network-available = FALSE and only learns the real state from an
 * asynchronous GetStatus reply, which is dispatched in the main context that
 * created the monitor. An app asking right away (microG's LoginActivity checks
 * getActiveNetworkInfo().isConnected() once) would see "no network". With a
 * context of our own, the first query can wait for that reply without needing
 * the UI thread's main loop, and NetworkCallbacks are not tied to it either.
 */

#define INITIAL_STATE_TIMEOUT_US (1 * G_TIME_SPAN_SECOND)

static GMutex state_mutex;
static GCond state_cond;
static gboolean state_known;
static GNetworkMonitor *monitor;

static gboolean mark_state_known(gpointer user_data)
{
	g_mutex_lock(&state_mutex);
	state_known = TRUE;
	g_cond_broadcast(&state_cond);
	g_mutex_unlock(&state_mutex);
	return G_SOURCE_REMOVE;
}

/* the reply handler may still be updating other properties: mark known once it has returned */
static void mark_state_known_later(void)
{
	GSource *source = g_idle_source_new();
	g_source_set_callback(source, mark_state_known, NULL, NULL);
	g_source_attach(source, g_main_context_get_thread_default());
	g_source_unref(source);
}

static void on_network_changed(GNetworkMonitor *self, gboolean network_available, JavaVM *jvm)
{
	mark_state_known_later();

	JNIEnv *env;
	if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6) != JNI_OK)
		return;
	J__ConnectivityManager__onNetworkChanged(env, network_available);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

static void on_property_changed(GObject *self, GParamSpec *pspec, gpointer user_data)
{
	mark_state_known_later();
}

static gpointer monitor_thread(gpointer user_data)
{
	JavaVM *jvm = user_data;
	JNIEnv *env;
	(*jvm)->AttachCurrentThreadAsDaemon(jvm, (void **)&env, &(JavaVMAttachArgs){.version = JNI_VERSION_1_6, .name = "GNetworkMonitor"});

	GMainContext *context = g_main_context_new();
	g_main_context_push_thread_default(context);

	GNetworkMonitor *m = g_network_monitor_get_default();
	g_signal_connect(m, "network-changed", G_CALLBACK(on_network_changed), jvm);
	g_signal_connect(m, "notify::connectivity", G_CALLBACK(on_property_changed), NULL);
	g_signal_connect(m, "notify::network-available", G_CALLBACK(on_property_changed), NULL);

	g_mutex_lock(&state_mutex);
	monitor = m;
	/* monitors that query synchronously already know: available is only ever FALSE before an async reply */
	if (g_network_monitor_get_network_available(m))
		state_known = TRUE;
	g_cond_broadcast(&state_cond);
	g_mutex_unlock(&state_mutex);

	g_main_loop_run(g_main_loop_new(context, FALSE));
	return NULL;
}

static GNetworkMonitor *get_monitor(JNIEnv *env)
{
	static gsize started = 0;
	if (g_once_init_enter(&started)) {
		JavaVM *jvm;
		(*env)->GetJavaVM(env, &jvm);
		g_thread_unref(g_thread_new("GNetworkMonitor", monitor_thread, jvm));
		g_once_init_leave(&started, 1);
	}

	gint64 deadline = g_get_monotonic_time() + INITIAL_STATE_TIMEOUT_US;
	g_mutex_lock(&state_mutex);
	while (!monitor || !state_known) {
		if (!g_cond_wait_until(&state_cond, &state_mutex, deadline)) {
			/* nothing arrived: take what the monitor says from now on */
			state_known = TRUE;
			break;
		}
	}
	GNetworkMonitor *m = monitor;
	g_mutex_unlock(&state_mutex);
	return m ? m : g_network_monitor_get_default();
}

JNIEXPORT jboolean JNICALL Java_android_net_ConnectivityManager_isActiveNetworkMetered(JNIEnv *env, jobject this)
{
	return g_network_monitor_get_network_metered(get_monitor(env));
}

JNIEXPORT jboolean JNICALL Java_android_net_ConnectivityManager_nativeGetNetworkAvailable(JNIEnv *env, jclass clazz)
{
	return g_network_monitor_get_network_available(get_monitor(env));
}

JNIEXPORT void JNICALL Java_android_net_ConnectivityManager_nativeStartMonitoring(JNIEnv *env, jclass clazz)
{
	get_monitor(env);
}
