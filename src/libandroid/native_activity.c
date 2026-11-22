/*
 * ANativeActivity requests (android/native_activity.h). As in AOSP (android_app_NativeActivity.cpp), they may come
 * from any thread and are carried out on the main thread by the Java NativeActivity: finish(),
 * setWindowFormat(), setWindowFlags(), showIme(), hideIme().
 */

#include <glib.h>
#include <jni.h>
#include <stdint.h>

#include "../api-impl-jni/native_activity.h"

enum { CMD_FINISH, CMD_SET_WINDOW_FORMAT, CMD_SET_WINDOW_FLAGS, CMD_SHOW_SOFT_INPUT, CMD_HIDE_SOFT_INPUT };

struct activity_work {
	JavaVM *vm;
	jobject activity; /* global reference to the Java NativeActivity */
	int32_t cmd;
	int32_t arg1;
	int32_t arg2;
};

static gboolean run_work(gpointer data)
{
	struct activity_work *work = data;
	JNIEnv *env;
	if ((*work->vm)->GetEnv(work->vm, (void **)&env, JNI_VERSION_1_6) == JNI_OK) {
		jclass clazz = (*env)->GetObjectClass(env, work->activity);
		switch (work->cmd) {
			case CMD_FINISH:
				(*env)->CallVoidMethod(env, work->activity, (*env)->GetMethodID(env, clazz, "finish", "()V"));
				break;
			case CMD_SET_WINDOW_FORMAT:
				(*env)->CallVoidMethod(env, work->activity, (*env)->GetMethodID(env, clazz, "setWindowFormat", "(I)V"), work->arg1);
				break;
			case CMD_SET_WINDOW_FLAGS:
				(*env)->CallVoidMethod(env, work->activity, (*env)->GetMethodID(env, clazz, "setWindowFlags", "(II)V"), work->arg1, work->arg2);
				break;
			case CMD_SHOW_SOFT_INPUT:
				(*env)->CallVoidMethod(env, work->activity, (*env)->GetMethodID(env, clazz, "showIme", "(I)V"), work->arg1);
				break;
			case CMD_HIDE_SOFT_INPUT:
				(*env)->CallVoidMethod(env, work->activity, (*env)->GetMethodID(env, clazz, "hideIme", "(I)V"), work->arg1);
				break;
		}
		if ((*env)->ExceptionCheck(env)) { /* AOSP: raiseAndClearException */
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
		(*env)->DeleteLocalRef(env, clazz);
		(*env)->DeleteGlobalRef(env, work->activity);
	}
	g_free(work);
	return G_SOURCE_REMOVE;
}

static void post_work(ANativeActivity *activity, int32_t cmd, int32_t arg1, int32_t arg2)
{
	JNIEnv *env;
	int attached = 0;
	if ((*activity->vm)->GetEnv(activity->vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
		if ((*activity->vm)->AttachCurrentThread(activity->vm, (void **)&env, NULL) != JNI_OK)
			return;
		attached = 1;
	}
	struct activity_work *work = g_new(struct activity_work, 1);
	*work = (struct activity_work){ activity->vm, (*env)->NewGlobalRef(env, activity->clazz), cmd, arg1, arg2 };
	if (attached)
		(*activity->vm)->DetachCurrentThread(activity->vm);
	g_idle_add(run_work, work);
}

void ANativeActivity_finish(ANativeActivity *activity)
{
	post_work(activity, CMD_FINISH, 0, 0);
}

void ANativeActivity_setWindowFormat(ANativeActivity *activity, int32_t format)
{
	post_work(activity, CMD_SET_WINDOW_FORMAT, format, 0);
}

void ANativeActivity_setWindowFlags(ANativeActivity *activity, uint32_t addFlags, uint32_t removeFlags)
{
	post_work(activity, CMD_SET_WINDOW_FLAGS, addFlags, addFlags | removeFlags);
}

void ANativeActivity_showSoftInput(ANativeActivity *activity, uint32_t flags)
{
	post_work(activity, CMD_SHOW_SOFT_INPUT, flags, 0);
}

void ANativeActivity_hideSoftInput(ANativeActivity *activity, uint32_t flags)
{
	post_work(activity, CMD_HIDE_SOFT_INPUT, flags, 0);
}
