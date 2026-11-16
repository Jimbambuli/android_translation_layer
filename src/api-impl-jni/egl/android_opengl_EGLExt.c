#include <EGL/egl.h>
#include <EGL/eglext.h>

#include "../defines.h"

#include "../generated_headers/android_opengl_EGLExt.h"

/* AOSP EGLExt.eglPresentationTimeANDROID() (android_opengl_EGLExt.cpp) calls the EGL_ANDROID_presentation_time
 * extension. Mesa does not have it; ATL's EGL layer for native apps (libandroid/egl.c,
 * bionic_eglPresentationTimeANDROID()) accepts the call and ignores the time, which is only a hint, and so does
 * this. */
JNIEXPORT jboolean JNICALL Java_android_opengl_EGLExt_eglPresentationTimeANDROID(JNIEnv *env, jclass class, jobject dpy, jobject sur, jlong time)
{
	return EGL_TRUE;
}

/* AOSP android_eglDupNativeFenceFDANDROID(): the EGL_ANDROID_native_fence_sync call of the desktop EGL. Apps see
 * the extension in eglQueryString(EGL_EXTENSIONS) exactly when the desktop EGL has it (Mesa on GPU drivers, not
 * llvmpipe); without it, the call fails and returns EGL_NO_NATIVE_FENCE_FD_ANDROID as on Android. */
JNIEXPORT jint JNICALL Java_android_opengl_EGLExt_eglDupNativeFenceFDANDROIDImpl(JNIEnv *env, jclass class, jobject dpy, jobject sync)
{
	static PFNEGLDUPNATIVEFENCEFDANDROIDPROC dup_native_fence_fd;
	if (dpy == NULL || sync == NULL) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"), "Object is set to null.");
		return EGL_NO_NATIVE_FENCE_FD_ANDROID;
	}
	EGLDisplay dpy_native = _PTR((*env)->CallLongMethod(env, dpy, _METHOD(_CLASS(dpy), "getNativeHandle", "()J")));
	EGLSync sync_native = _PTR((*env)->CallLongMethod(env, sync, _METHOD(_CLASS(sync), "getNativeHandle", "()J")));
	if (!dup_native_fence_fd)
		dup_native_fence_fd = (PFNEGLDUPNATIVEFENCEFDANDROIDPROC)eglGetProcAddress("eglDupNativeFenceFDANDROID");
	if (!dup_native_fence_fd)
		return EGL_NO_NATIVE_FENCE_FD_ANDROID;
	return dup_native_fence_fd(dpy_native, sync_native);
}
