#include <libportal/portal.h>
#include <time.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_location_LocationManager.h"

static XdpPortal *portal = NULL;

static void location_updated(
    XdpPortal *self,
    gdouble latitude,
    gdouble longitude,
    gdouble altitude,
    gdouble accuracy,
    gdouble speed,
    gdouble heading,
    gchar *description,
    gint64 timestamp_s,
    gint64 timestamp_ms,
    JavaVM *jvm)
{
	JNIEnv *env;
	(*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6);
	/* the portal sends the microseconds of the second as its second timestamp value */
	jlong timestamp = timestamp_s * 1000 + timestamp_ms / 1000;
	struct timespec now; /* SystemClock.elapsedRealtimeNanos() */
	clock_gettime(CLOCK_BOOTTIME, &now);
	J__LocationManager__locationUpdated(env, latitude, longitude, altitude, accuracy, speed, heading, timestamp,
	                                    now.tv_sec * 1000000000LL + now.tv_nsec);
}

static void monitor_started(GObject *source, GAsyncResult *result, gpointer data)
{
	g_autoptr(GError) error = NULL;
	if (!xdp_portal_location_monitor_start_finish(XDP_PORTAL(source), result, &error))
		printf("LocationManager: the location portal gives no position: %s\n", error->message);
}

/* Java only calls this when location is allowed (LocationManager.available()) */
JNIEXPORT void JNICALL Java_android_location_LocationManager_nativeStart(JNIEnv *env, jclass)
{
	if (!portal) {
		portal = xdp_portal_new();
		JavaVM *jvm;
		(*env)->GetJavaVM(env, &jvm);
		g_signal_connect(portal, "location-updated", G_CALLBACK(location_updated), jvm);
	}
	xdp_portal_location_monitor_start(portal, NULL, 0, 0, XDP_LOCATION_ACCURACY_EXACT, XDP_LOCATION_MONITOR_FLAG_NONE, NULL, monitor_started, NULL);
}

JNIEXPORT void JNICALL Java_android_location_LocationManager_nativeStop(JNIEnv *env, jclass)
{
	if (portal)
		xdp_portal_location_monitor_stop(portal);
}
