#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <stdint.h>
#include <stdlib.h>

#include "../defines.h"
#include "../util.h"

#include "../../libandroid/native_window.h"

#include "../generated_headers/android_opengl_EGL15.h"

/* AOSP core/jni/android_opengl_EGL15.cpp (generated from the EGL spec): the EGL 1.5 calls on the handles of the
 * Java objects, with the same argument checks and exceptions. The display is ATL's EGL display, the one of the
 * GTK window (libandroid/egl.c, bionic_eglGetDisplay()), so the calls go to the desktop EGL. */

static jclass egldisplayClass;
static jclass eglsurfaceClass;
static jclass eglconfigClass;
static jclass eglcontextClass;
static jclass eglimageClass;
static jclass eglsyncClass;

static jmethodID egldisplayGetHandleID;
static jmethodID eglconfigGetHandleID;
static jmethodID eglcontextGetHandleID;
static jmethodID eglsurfaceGetHandleID;
static jmethodID eglimageGetHandleID;
static jmethodID eglsyncGetHandleID;

static jmethodID egldisplayConstructor;
static jmethodID eglsurfaceConstructor;
static jmethodID eglimageConstructor;
static jmethodID eglsyncConstructor;

static jclass global_class(JNIEnv *env, const char *name)
{
	jclass local = (*env)->FindClass(env, name);
	jclass global = (*env)->NewGlobalRef(env, local);
	(*env)->DeleteLocalRef(env, local);
	return global;
}

/* AOSP nativeClassInit(): EGL_NO_CONTEXT, EGL_NO_DISPLAY and EGL_NO_SURFACE are objects with the null handle;
 * EGL_NO_IMAGE and EGL_NO_SYNC stay null */
JNIEXPORT void JNICALL Java_android_opengl_EGL15__1nativeClassInit(JNIEnv *env, jclass class)
{
	egldisplayClass = global_class(env, "android/opengl/EGLDisplay");
	eglsurfaceClass = global_class(env, "android/opengl/EGLSurface");
	eglconfigClass = global_class(env, "android/opengl/EGLConfig");
	eglcontextClass = global_class(env, "android/opengl/EGLContext");
	eglimageClass = global_class(env, "android/opengl/EGLImage");
	eglsyncClass = global_class(env, "android/opengl/EGLSync");

	egldisplayGetHandleID = (*env)->GetMethodID(env, egldisplayClass, "getNativeHandle", "()J");
	eglconfigGetHandleID = (*env)->GetMethodID(env, eglconfigClass, "getNativeHandle", "()J");
	eglcontextGetHandleID = (*env)->GetMethodID(env, eglcontextClass, "getNativeHandle", "()J");
	eglsurfaceGetHandleID = (*env)->GetMethodID(env, eglsurfaceClass, "getNativeHandle", "()J");
	eglimageGetHandleID = (*env)->GetMethodID(env, eglimageClass, "getNativeHandle", "()J");
	eglsyncGetHandleID = (*env)->GetMethodID(env, eglsyncClass, "getNativeHandle", "()J");

	egldisplayConstructor = (*env)->GetMethodID(env, egldisplayClass, "<init>", "(J)V");
	eglsurfaceConstructor = (*env)->GetMethodID(env, eglsurfaceClass, "<init>", "(J)V");
	eglimageConstructor = (*env)->GetMethodID(env, eglimageClass, "<init>", "(J)V");
	eglsyncConstructor = (*env)->GetMethodID(env, eglsyncClass, "<init>", "(J)V");
	jmethodID eglcontextConstructor = (*env)->GetMethodID(env, eglcontextClass, "<init>", "(J)V");

	jobject no_context = (*env)->NewObject(env, eglcontextClass, eglcontextConstructor, (jlong)0);
	jobject no_display = (*env)->NewObject(env, egldisplayClass, egldisplayConstructor, (jlong)0);
	jobject no_surface = (*env)->NewObject(env, eglsurfaceClass, eglsurfaceConstructor, (jlong)0);
	(*env)->SetStaticObjectField(env, class, (*env)->GetStaticFieldID(env, class, "EGL_NO_CONTEXT", "Landroid/opengl/EGLContext;"), no_context);
	(*env)->SetStaticObjectField(env, class, (*env)->GetStaticFieldID(env, class, "EGL_NO_DISPLAY", "Landroid/opengl/EGLDisplay;"), no_display);
	(*env)->SetStaticObjectField(env, class, (*env)->GetStaticFieldID(env, class, "EGL_NO_SURFACE", "Landroid/opengl/EGLSurface;"), no_surface);
}

/* AOSP fromEGLHandle(); unlike AOSP, the callers return as soon as it has thrown instead of calling EGL with
 * the null handle (the exception is the result either way) */
static void *from_egl_handle(JNIEnv *env, jmethodID mid, jobject obj)
{
	if (obj == NULL) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"), "Object is set to null.");
		return NULL;
	}
	return _PTR((*env)->CallLongMethod(env, obj, mid));
}

/* AOSP toEGLHandle(): EGL_NO_IMAGE becomes null, every other handle (also a null EGLSync, EGLDisplay or
 * EGLSurface) an object */
static jobject to_egl_handle(JNIEnv *env, jclass cls, jmethodID con, void *handle)
{
	if (cls == eglimageClass && handle == EGL_NO_IMAGE)
		return NULL;
	return (*env)->NewObject(env, cls, con, _INTPTR(handle));
}

/* AOSP's checks of an EGLAttrib list argument; returns the elements from offset on, or NULL after throwing */
static jlong *get_attrib_list(JNIEnv *env, jlongArray attrib_list_ref, jint offset, const char *null_message)
{
	if (!attrib_list_ref) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"), null_message);
		return NULL;
	}
	if (offset < 0) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"), "offset < 0");
		return NULL;
	}
	return (*env)->GetLongArrayElements(env, attrib_list_ref, NULL);
}

JNIEXPORT jobject JNICALL Java_android_opengl_EGL15_eglCreateSync(JNIEnv *env, jclass class, jobject dpy, jint type, jlongArray attrib_list_ref, jint offset)
{
	EGLDisplay dpy_native = from_egl_handle(env, egldisplayGetHandleID, dpy);
	if ((*env)->ExceptionCheck(env))
		return NULL;
	jlong *attrib_list = get_attrib_list(env, attrib_list_ref, offset, "attrib_list == null");
	if (!attrib_list)
		return NULL;
	EGLSync ret = eglCreateSync(dpy_native, type, (EGLAttrib *)(attrib_list + offset));
	(*env)->ReleaseLongArrayElements(env, attrib_list_ref, attrib_list, JNI_ABORT);
	return to_egl_handle(env, eglsyncClass, eglsyncConstructor, ret);
}

JNIEXPORT jboolean JNICALL Java_android_opengl_EGL15_eglGetSyncAttrib(JNIEnv *env, jclass class, jobject dpy, jobject sync, jint attribute, jlongArray value_ref, jint offset)
{
	EGLDisplay dpy_native = from_egl_handle(env, egldisplayGetHandleID, dpy);
	EGLSync sync_native = from_egl_handle(env, eglsyncGetHandleID, sync);
	if ((*env)->ExceptionCheck(env))
		return JNI_FALSE;
	jlong *value_base = get_attrib_list(env, value_ref, offset, "value == null");
	if (!value_base)
		return JNI_FALSE;
	EGLAttrib value;
	EGLBoolean ret = eglGetSyncAttrib(dpy_native, sync_native, attribute, &value);
	if (ret == EGL_TRUE)
		value_base[offset] = value;
	(*env)->ReleaseLongArrayElements(env, value_ref, value_base, 0);
	return ret;
}

JNIEXPORT jboolean JNICALL Java_android_opengl_EGL15_eglDestroySync(JNIEnv *env, jclass class, jobject dpy, jobject sync)
{
	EGLDisplay dpy_native = from_egl_handle(env, egldisplayGetHandleID, dpy);
	EGLSync sync_native = from_egl_handle(env, eglsyncGetHandleID, sync);
	if ((*env)->ExceptionCheck(env))
		return JNI_FALSE;
	return eglDestroySync(dpy_native, sync_native);
}

JNIEXPORT jint JNICALL Java_android_opengl_EGL15_eglClientWaitSync(JNIEnv *env, jclass class, jobject dpy, jobject sync, jint flags, jlong timeout)
{
	EGLDisplay dpy_native = from_egl_handle(env, egldisplayGetHandleID, dpy);
	EGLSync sync_native = from_egl_handle(env, eglsyncGetHandleID, sync);
	if ((*env)->ExceptionCheck(env))
		return 0;
	return eglClientWaitSync(dpy_native, sync_native, flags, timeout);
}

/* On Android the platform is EGL_PLATFORM_ANDROID_KHR with the default display; that is ATL's display (the
 * GTK one), like eglGetDisplay(). Other platforms are not Android's: EGL_NO_DISPLAY. */
JNIEXPORT jobject JNICALL Java_android_opengl_EGL15_eglGetPlatformDisplay(JNIEnv *env, jclass class, jint platform, jlong native_display, jlongArray attrib_list_ref, jint offset)
{
	jlong *attrib_list = get_attrib_list(env, attrib_list_ref, offset, "attrib_list == null");
	if (!attrib_list)
		return NULL;
	(*env)->ReleaseLongArrayElements(env, attrib_list_ref, attrib_list, JNI_ABORT);
	EGLDisplay ret = EGL_NO_DISPLAY;
	if (platform == EGL_PLATFORM_ANDROID_KHR)
		ret = bionic_eglGetDisplay(_PTR(native_display));
	return to_egl_handle(env, egldisplayClass, egldisplayConstructor, ret);
}

/* AOSP passes the address of the buffer's content as the native window (an ANativeWindow); the window goes
 * through ATL's eglCreateWindowSurface() for ANativeWindows, which takes EGLint attributes */
JNIEXPORT jobject JNICALL Java_android_opengl_EGL15_eglCreatePlatformWindowSurface(JNIEnv *env, jclass class, jobject dpy, jobject config, jobject native_window_buf, jlongArray attrib_list_ref, jint offset)
{
	EGLDisplay dpy_native = from_egl_handle(env, egldisplayGetHandleID, dpy);
	EGLConfig config_native = from_egl_handle(env, eglconfigGetHandleID, config);
	if ((*env)->ExceptionCheck(env))
		return NULL;
	if (!native_window_buf) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"), "native_window == null");
		return NULL;
	}
	jlong *attrib_list = get_attrib_list(env, attrib_list_ref, offset, "attrib_list == null");
	if (!attrib_list)
		return NULL;
	jsize length = (*env)->GetArrayLength(env, attrib_list_ref) - offset;
	EGLint *int_attrib_list = calloc(length > 0 ? length : 1, sizeof(EGLint));
	for (jsize i = 0; i < length; i++)
		int_attrib_list[i] = attrib_list[offset + i];
	(*env)->ReleaseLongArrayElements(env, attrib_list_ref, attrib_list, JNI_ABORT);
	jarray array_ref;
	jbyte *array;
	void *native_window = get_nio_buffer(env, native_window_buf, &array_ref, &array);
	EGLSurface ret = bionic_eglCreateWindowSurface(dpy_native, config_native, native_window, length > 0 ? int_attrib_list : NULL);
	release_nio_buffer(env, array_ref, array);
	free(int_attrib_list);
	return to_egl_handle(env, eglsurfaceClass, eglsurfaceConstructor, ret);
}

JNIEXPORT jobject JNICALL Java_android_opengl_EGL15_eglCreatePlatformPixmapSurface(JNIEnv *env, jclass class, jobject dpy, jobject config, jobject native_pixmap_buf, jlongArray attrib_list_ref, jint offset)
{
	(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/UnsupportedOperationException"), "eglCreatePlatformPixmapSurface");
	return NULL;
}

JNIEXPORT jboolean JNICALL Java_android_opengl_EGL15_eglWaitSync(JNIEnv *env, jclass class, jobject dpy, jobject sync, jint flags)
{
	EGLDisplay dpy_native = from_egl_handle(env, egldisplayGetHandleID, dpy);
	EGLSync sync_native = from_egl_handle(env, eglsyncGetHandleID, sync);
	if ((*env)->ExceptionCheck(env))
		return JNI_FALSE;
	return eglWaitSync(dpy_native, sync_native, flags);
}

JNIEXPORT jobject JNICALL Java_android_opengl_EGL15_eglCreateImage(JNIEnv *env, jclass class, jobject dpy, jobject context, jint target, jlong buffer, jlongArray attrib_list_ref, jint offset)
{
	EGLDisplay dpy_native = from_egl_handle(env, egldisplayGetHandleID, dpy);
	EGLContext context_native = from_egl_handle(env, eglcontextGetHandleID, context);
	if ((*env)->ExceptionCheck(env))
		return NULL;
	jlong *attrib_list = get_attrib_list(env, attrib_list_ref, offset, "attrib_list == null");
	if (!attrib_list)
		return NULL;
	EGLImage ret = eglCreateImage(dpy_native, context_native, target, _PTR(buffer), (EGLAttrib *)(attrib_list + offset));
	(*env)->ReleaseLongArrayElements(env, attrib_list_ref, attrib_list, JNI_ABORT);
	return to_egl_handle(env, eglimageClass, eglimageConstructor, ret);
}

JNIEXPORT jboolean JNICALL Java_android_opengl_EGL15_eglDestroyImage(JNIEnv *env, jclass class, jobject dpy, jobject image)
{
	EGLDisplay dpy_native = from_egl_handle(env, egldisplayGetHandleID, dpy);
	EGLImage image_native = from_egl_handle(env, eglimageGetHandleID, image);
	if ((*env)->ExceptionCheck(env))
		return JNI_FALSE;
	return eglDestroyImage(dpy_native, image_native);
}
