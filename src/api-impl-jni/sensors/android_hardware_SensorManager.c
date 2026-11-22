#include <gio/gio.h>
#include <math.h>
#include <string.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_hardware_SensorManager.h"

/* iio-sensor-proxy (net.hadess.SensorProxy on the system bus): the ambient light and proximity sensors of
 * SensorManager's list. A sensor is claimed while it has listeners (ClaimLight/ClaimProximity, released with
 * Release*), the changes of its property are its events. */

#define PROXY_LIGHT 1
#define PROXY_PROXIMITY 2

static GDBusProxy *sensor_proxy = NULL;
static gboolean sensor_proxy_tried = FALSE;

static void proxy_properties_changed_cb(GDBusProxy *proxy, GVariant *changed, GStrv invalidated, gpointer user_data);

static GDBusProxy *get_sensor_proxy(void)
{
	if (sensor_proxy_tried)
		return sensor_proxy;
	sensor_proxy_tried = TRUE;
	GError *error = NULL;
	/* the property changes are signals of the main loop, whichever thread asked first */
	g_main_context_push_thread_default(g_main_context_default());
	sensor_proxy = g_dbus_proxy_new_for_bus_sync(G_BUS_TYPE_SYSTEM, G_DBUS_PROXY_FLAGS_DO_NOT_AUTO_START, NULL,
	                                             "net.hadess.SensorProxy", "/net/hadess/SensorProxy", "net.hadess.SensorProxy",
	                                             NULL, &error);
	g_main_context_pop_thread_default(g_main_context_default());
	if (!sensor_proxy) {
		g_error_free(error);
		return NULL;
	}
	gchar *owner = g_dbus_proxy_get_name_owner(sensor_proxy);
	if (!owner) { // iio-sensor-proxy does not run (it quits on a machine without sensors)
		g_clear_object(&sensor_proxy);
		return NULL;
	}
	g_free(owner);
	g_signal_connect(sensor_proxy, "g-properties-changed", G_CALLBACK(proxy_properties_changed_cb), NULL);
	return sensor_proxy;
}

static gboolean proxy_get_boolean(const char *property)
{
	GVariant *v = g_dbus_proxy_get_cached_property(sensor_proxy, property);
	gboolean ret = v && g_variant_is_of_type(v, G_VARIANT_TYPE_BOOLEAN) && g_variant_get_boolean(v);
	if (v)
		g_variant_unref(v);
	return ret;
}

/* light: LightLevel (lux when LightLevelUnit is "lux", otherwise the vendor's unit), proximity: ProximityNear */
static float proxy_value(int which)
{
	GVariant *v = g_dbus_proxy_get_cached_property(sensor_proxy, which == PROXY_LIGHT ? "LightLevel" : "ProximityNear");
	float ret = NAN;
	if (v && g_variant_is_of_type(v, G_VARIANT_TYPE_DOUBLE))
		ret = g_variant_get_double(v);
	else if (v && g_variant_is_of_type(v, G_VARIANT_TYPE_BOOLEAN))
		ret = g_variant_get_boolean(v) ? 1 : 0;
	if (v)
		g_variant_unref(v);
	return ret;
}

static void proxy_properties_changed_cb(GDBusProxy *proxy, GVariant *changed, GStrv invalidated, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	GVariant *v;
	if ((v = g_variant_lookup_value(changed, "LightLevel", G_VARIANT_TYPE_DOUBLE))) {
		J__SensorManager__onProxyValue(env, PROXY_LIGHT, g_variant_get_double(v));
		g_variant_unref(v);
	}
	if ((v = g_variant_lookup_value(changed, "ProximityNear", G_VARIANT_TYPE_BOOLEAN))) {
		J__SensorManager__onProxyValue(env, PROXY_PROXIMITY, g_variant_get_boolean(v) ? 1 : 0);
		g_variant_unref(v);
	}
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

JNIEXPORT jint JNICALL Java_android_hardware_SensorManager_nativeProxySensors(JNIEnv *env, jclass this)
{
	if (!get_sensor_proxy())
		return 0;
	return (proxy_get_boolean("HasAmbientLight") ? PROXY_LIGHT : 0) | (proxy_get_boolean("HasProximity") ? PROXY_PROXIMITY : 0);
}

JNIEXPORT jboolean JNICALL Java_android_hardware_SensorManager_nativeProxyClaim(JNIEnv *env, jclass this, jint which, jboolean claim)
{
	if (!get_sensor_proxy())
		return JNI_FALSE;
	const char *method = which == PROXY_LIGHT ? (claim ? "ClaimLight" : "ReleaseLight") : (claim ? "ClaimProximity" : "ReleaseProximity");
	GError *error = NULL;
	GVariant *ret = g_dbus_proxy_call_sync(sensor_proxy, method, NULL, G_DBUS_CALL_FLAGS_NONE, 2000, NULL, &error);
	if (!ret) {
		g_warning("iio-sensor-proxy %s failed: %s", method, error->message);
		g_error_free(error);
		return JNI_FALSE;
	}
	g_variant_unref(ret);
	return JNI_TRUE;
}

JNIEXPORT jfloat JNICALL Java_android_hardware_SensorManager_nativeProxyValue(JNIEnv *env, jclass this, jint which)
{
	if (!get_sensor_proxy())
		return NAN;
	return proxy_value(which);
}
