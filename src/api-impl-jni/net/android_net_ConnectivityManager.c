#include <gio/gio.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_net_ConnectivityManager.h"

JNIEXPORT jboolean JNICALL Java_android_net_ConnectivityManager_nativeNetworkMetered(JNIEnv *env, jclass clazz)
{
	return g_network_monitor_get_network_metered(g_network_monitor_get_default());
}

JNIEXPORT jboolean JNICALL Java_android_net_ConnectivityManager_nativeNetworkAvailable(JNIEnv *env, jclass clazz)
{
	return g_network_monitor_get_network_available(g_network_monitor_get_default());
}

static void on_network_changed(GNetworkMonitor *self, gboolean network_available, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	J__ConnectivityManager__networkChanged(env);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

/* ConnectivityManager sends the callbacks itself; GNetworkMonitor tells it when the network changed */
JNIEXPORT void JNICALL Java_android_net_ConnectivityManager_nativeWatchNetwork(JNIEnv *env, jclass clazz)
{
	g_signal_connect(g_network_monitor_get_default(), "network-changed", G_CALLBACK(on_network_changed), NULL);
}
