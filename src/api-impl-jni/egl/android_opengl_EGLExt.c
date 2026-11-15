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
