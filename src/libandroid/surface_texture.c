/*
 * ASurfaceTexture (NDK surface_texture.h, AOSP frameworks/native libs/nativedisplay/surfacetexture/surface_texture.cpp):
 * the native handle of a Java SurfaceTexture. AOSP's handle holds the SurfaceTexture's consumer and producer; ATL's
 * consumer is the Java SurfaceTexture itself, so the handle holds a global reference to it and calls its methods.
 * Where the Java method throws, the native one returns the status_t the AOSP consumer has for that case.
 */

#include <errno.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdlib.h>

#include <jni.h>

#include "native_window.h"

#define NO_INIT (-ENODEV)
#define INVALID_OPERATION (-ENOSYS)

typedef struct ASurfaceTexture {
	jobject surface_texture; // global reference
} ASurfaceTexture;

static JNIEnv *jni_env(void)
{
	JavaVM *vm;
	jsize count;
	JNIEnv *env;
	if (JNI_GetCreatedJavaVMs(&vm, 1, &count) != JNI_OK || count < 1)
		return NULL;
	if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK && (*vm)->AttachCurrentThread(vm, (void **)&env, NULL) != JNI_OK)
		return NULL;
	return env;
}

// the status of the Java call: 0, or NO_INIT once the SurfaceTexture is released, else INVALID_OPERATION
static int status(JNIEnv *env, ASurfaceTexture *st)
{
	if (!(*env)->ExceptionCheck(env))
		return 0;
	(*env)->ExceptionClear(env);
	jclass class = (*env)->GetObjectClass(env, st->surface_texture);
	jboolean released = (*env)->CallBooleanMethod(env, st->surface_texture, (*env)->GetMethodID(env, class, "isReleased", "()Z"));
	(*env)->DeleteLocalRef(env, class);
	return released ? NO_INIT : INVALID_OPERATION;
}

static void call_void(JNIEnv *env, ASurfaceTexture *st, const char *name, const char *signature, ...)
{
	jclass class = (*env)->GetObjectClass(env, st->surface_texture);
	jmethodID method = (*env)->GetMethodID(env, class, name, signature);
	(*env)->DeleteLocalRef(env, class);
	va_list args;
	va_start(args, signature);
	(*env)->CallVoidMethodV(env, st->surface_texture, method, args);
	va_end(args);
}

ASurfaceTexture *ASurfaceTexture_fromSurfaceTexture(JNIEnv *env, jobject surfacetexture)
{
	jclass class = (*env)->FindClass(env, "android/graphics/SurfaceTexture");
	jboolean is_instance = surfacetexture && (*env)->IsInstanceOf(env, surfacetexture, class);
	(*env)->DeleteLocalRef(env, class);
	if (!is_instance)
		return NULL;
	ASurfaceTexture *st = malloc(sizeof(ASurfaceTexture));
	st->surface_texture = (*env)->NewGlobalRef(env, surfacetexture);
	return st;
}

// a new Surface for the SurfaceTexture, as AOSP makes one for its producer
struct ANativeWindow *ASurfaceTexture_acquireANativeWindow(ASurfaceTexture *st)
{
	JNIEnv *env = jni_env();
	jclass class = (*env)->FindClass(env, "android/view/Surface");
	jobject surface = (*env)->NewObject(env, class, (*env)->GetMethodID(env, class, "<init>", "(Landroid/graphics/SurfaceTexture;)V"), st->surface_texture);
	(*env)->DeleteLocalRef(env, class);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		return NULL;
	}
	struct ANativeWindow *window = ANativeWindow_fromSurface(env, surface); // returned with one reference, which is the caller's
	(*env)->DeleteLocalRef(env, surface);
	return window;
}

void ASurfaceTexture_release(ASurfaceTexture *st)
{
	JNIEnv *env = jni_env();
	(*env)->DeleteGlobalRef(env, st->surface_texture);
	free(st);
}

int ASurfaceTexture_attachToGLContext(ASurfaceTexture *st, uint32_t texName)
{
	JNIEnv *env = jni_env();
	call_void(env, st, "attachToGLContext", "(I)V", (jint)texName);
	return status(env, st);
}

int ASurfaceTexture_detachFromGLContext(ASurfaceTexture *st)
{
	JNIEnv *env = jni_env();
	call_void(env, st, "detachFromGLContext", "()V");
	return status(env, st);
}

int ASurfaceTexture_updateTexImage(ASurfaceTexture *st)
{
	JNIEnv *env = jni_env();
	call_void(env, st, "updateTexImage", "()V");
	return status(env, st);
}

void ASurfaceTexture_getTransformMatrix(ASurfaceTexture *st, float mtx[16])
{
	JNIEnv *env = jni_env();
	jfloatArray array = (*env)->NewFloatArray(env, 16);
	call_void(env, st, "getTransformMatrix", "([F)V", array);
	(*env)->GetFloatArrayRegion(env, array, 0, 16, mtx);
	(*env)->DeleteLocalRef(env, array);
}

int64_t ASurfaceTexture_getTimestamp(ASurfaceTexture *st)
{
	JNIEnv *env = jni_env();
	jclass class = (*env)->GetObjectClass(env, st->surface_texture);
	jlong timestamp = (*env)->CallLongMethod(env, st->surface_texture, (*env)->GetMethodID(env, class, "getTimestamp", "()J"));
	(*env)->DeleteLocalRef(env, class);
	return timestamp;
}
