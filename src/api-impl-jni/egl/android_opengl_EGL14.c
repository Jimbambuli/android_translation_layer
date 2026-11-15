#include <EGL/egl.h>
#include <stdint.h>
#include <stdlib.h>

#include "../defines.h"

#include "../generated_headers/android_opengl_EGL14.h"

/* EGL14 functions that the EGL10 implementation (EGLImpl) does not cover: plain EGL calls on the native
 * handles. AOSP's android_opengl_EGL14.cpp does the same with its generated code (not in the local sources). */

#define DPY(handle) ((EGLDisplay)(intptr_t)(handle))
#define SURF(handle) ((EGLSurface)(intptr_t)(handle))

JNIEXPORT jlong JNICALL Java_android_opengl_EGL14_native_1eglGetCurrentDisplay(JNIEnv *env, jclass clazz)
{
	return (jlong)(intptr_t)eglGetCurrentDisplay();
}

JNIEXPORT jboolean JNICALL Java_android_opengl_EGL14_native_1eglGetConfigs(JNIEnv *env, jclass clazz, jlong dpy, jlongArray configs_ref, jintArray num_config_ref)
{
	jsize size = (*env)->GetArrayLength(env, configs_ref);
	EGLConfig *configs = calloc(size + 1, sizeof(EGLConfig));
	EGLint num_config = 0;
	EGLBoolean ret = eglGetConfigs(DPY(dpy), configs, size, &num_config);
	if (ret) {
		for (EGLint i = 0; i < num_config && i < size; i++) {
			jlong handle = (jlong)(intptr_t)configs[i];
			(*env)->SetLongArrayRegion(env, configs_ref, i, 1, &handle);
		}
	}
	(*env)->SetIntArrayRegion(env, num_config_ref, 0, 1, &num_config);
	free(configs);
	return ret;
}

JNIEXPORT jint JNICALL Java_android_opengl_EGL14_native_1eglQueryAPI(JNIEnv *env, jclass clazz)
{
	return eglQueryAPI();
}

JNIEXPORT jboolean JNICALL Java_android_opengl_EGL14_native_1eglWaitClient(JNIEnv *env, jclass clazz)
{
	return eglWaitClient();
}

JNIEXPORT jboolean JNICALL Java_android_opengl_EGL14_native_1eglWaitGL(JNIEnv *env, jclass clazz)
{
	return eglWaitGL();
}

JNIEXPORT jboolean JNICALL Java_android_opengl_EGL14_native_1eglWaitNative(JNIEnv *env, jclass clazz, jint engine)
{
	return eglWaitNative(engine);
}

JNIEXPORT jboolean JNICALL Java_android_opengl_EGL14_native_1eglSurfaceAttrib(JNIEnv *env, jclass clazz, jlong dpy, jlong surface, jint attribute, jint value)
{
	return eglSurfaceAttrib(DPY(dpy), SURF(surface), attribute, value);
}

JNIEXPORT jboolean JNICALL Java_android_opengl_EGL14_native_1eglBindTexImage(JNIEnv *env, jclass clazz, jlong dpy, jlong surface, jint buffer)
{
	return eglBindTexImage(DPY(dpy), SURF(surface), buffer);
}

JNIEXPORT jboolean JNICALL Java_android_opengl_EGL14_native_1eglReleaseTexImage(JNIEnv *env, jclass clazz, jlong dpy, jlong surface, jint buffer)
{
	return eglReleaseTexImage(DPY(dpy), SURF(surface), buffer);
}

JNIEXPORT jboolean JNICALL Java_android_opengl_EGL14_native_1eglSwapInterval(JNIEnv *env, jclass clazz, jlong dpy, jint interval)
{
	return eglSwapInterval(DPY(dpy), interval);
}

JNIEXPORT jlong JNICALL Java_android_opengl_EGL14_native_1eglCreatePbufferFromClientBuffer(JNIEnv *env, jclass clazz, jlong dpy, jint buftype, jlong buffer, jlong config, jintArray attrib_list_ref)
{
	jint *attrib_list = attrib_list_ref ? (*env)->GetIntArrayElements(env, attrib_list_ref, NULL) : NULL;
	EGLSurface surface = eglCreatePbufferFromClientBuffer(DPY(dpy), buftype, (EGLClientBuffer)(intptr_t)buffer, (EGLConfig)(intptr_t)config, attrib_list);
	if (attrib_list)
		(*env)->ReleaseIntArrayElements(env, attrib_list_ref, attrib_list, JNI_ABORT);
	return (jlong)(intptr_t)surface;
}
