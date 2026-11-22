/* The windows that com.android.server.input.InputDispatcher hands the events of game controllers to: the
 * focused window of the app (see activity_dispatch_device_key()). The result: 1 handled, 0 not handled,
 * -1 no window of the app has the focus (the event is dropped, as InputDispatcher drops it without a
 * focused window) */

#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"
#include "../app/android_app_Activity.h"
#include "../generated_headers/com_android_server_input_InputDispatcher.h"

JNIEXPORT jint JNICALL Java_com_android_server_input_InputDispatcher_nativeDispatchKey(JNIEnv *env, jclass class, jobject event)
{
	return activity_dispatch_device_key(env, event);
}

JNIEXPORT jint JNICALL Java_com_android_server_input_InputDispatcher_nativeDispatchMotion(JNIEnv *env, jclass class, jobject event)
{
	return activity_dispatch_device_motion(env, event);
}
