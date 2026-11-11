#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <stdio.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_graphics_SurfaceTexture.h"

/* GLConsumer::updateTexImage() with an empty buffer queue: the SurfaceTexture belongs to the EGL display and
 * context it is first updated in (checkAndUpdateEglStateLocked()), and then binds its external texture */
JNIEXPORT jboolean JNICALL Java_android_graphics_SurfaceTexture_nativeUpdateTexImage(JNIEnv *env, jobject this, jint tex_name)
{
	EGLDisplay dpy = eglGetCurrentDisplay();
	EGLContext ctx = eglGetCurrentContext();
	EGLDisplay own_dpy = _PTR(_GET_LONG_FIELD(this, "eglDisplay"));
	EGLContext own_ctx = _PTR(_GET_LONG_FIELD(this, "eglContext"));
	if ((own_dpy != dpy && own_dpy != EGL_NO_DISPLAY) || dpy == EGL_NO_DISPLAY) {
		fprintf(stderr, "SurfaceTexture: checkAndUpdateEglState: invalid current EGLDisplay\n");
		return false;
	}
	if ((own_ctx != ctx && own_ctx != EGL_NO_CONTEXT) || ctx == EGL_NO_CONTEXT) {
		fprintf(stderr, "SurfaceTexture: checkAndUpdateEglState: invalid current EGLContext\n");
		return false;
	}
	_SET_LONG_FIELD(this, "eglDisplay", _INTPTR(dpy));
	_SET_LONG_FIELD(this, "eglContext", _INTPTR(ctx));
	glBindTexture(GL_TEXTURE_EXTERNAL_OES, tex_name);
	return true;
}
