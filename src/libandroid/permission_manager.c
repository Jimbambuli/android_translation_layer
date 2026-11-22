/* NDK permission manager (android/permission_manager.h) after AOSP frameworks/base/native/android/permission_manager.cpp:
 * ActivityManager.checkPermission(), which is the application's Context.checkPermission() in ATL */

#include <stdint.h>
#include <sys/types.h>

#include "ndk_jni.h"

#define PERMISSION_MANAGER_STATUS_OK 0
#define PERMISSION_MANAGER_STATUS_ERROR_UNKNOWN (-1)
#define PERMISSION_MANAGER_STATUS_SERVICE_UNAVAILABLE (-2)

int32_t APermissionManager_checkPermission(const char *permission, pid_t pid, uid_t uid, int32_t *outResult)
{
	JNIEnv *env = ndk_jni_env();
	if (!env)
		return PERMISSION_MANAGER_STATUS_SERVICE_UNAVAILABLE;
	jclass activity_thread = ndk_jni_class(env, "android/app/ActivityThread");
	jobject app = (*env)->CallStaticObjectMethod(env, activity_thread,
	        (*env)->GetStaticMethodID(env, activity_thread, "currentApplication", "()Landroid/app/Application;"));
	if (ndk_jni_threw(env, "ActivityThread.currentApplication") || !app)
		return PERMISSION_MANAGER_STATUS_SERVICE_UNAVAILABLE;
	jclass context = ndk_jni_class(env, "android/content/Context");
	jstring name = (*env)->NewStringUTF(env, permission);
	jint result = (*env)->CallIntMethod(env, app, (*env)->GetMethodID(env, context, "checkPermission", "(Ljava/lang/String;II)I"), name, (jint)pid, (jint)uid);
	(*env)->DeleteLocalRef(env, name);
	(*env)->DeleteLocalRef(env, app);
	if (ndk_jni_threw(env, "Context.checkPermission"))
		return PERMISSION_MANAGER_STATUS_ERROR_UNKNOWN;
	*outResult = result;
	return PERMISSION_MANAGER_STATUS_OK;
}
