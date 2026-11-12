#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <gdk/gdk.h>
#include <stdio.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_graphics_SurfaceTexture.h"

/* the GL_TEXTURE_2D the frames are uploaded to, and its EGLImage, which is the storage of the external texture */
struct gl_image {
	EGLDisplay display;
	EGLImage image;
	GLuint texture;
	int width;
	int height;
};

/* the frame into the external texture tex_name: an external texture can only get its storage from an EGLImage,
 * so the pixels go into a GL_TEXTURE_2D of the same context whose EGLImage backs it */
static gboolean upload_frame(JNIEnv *env, jobject this, GLuint tex_name, GdkTexture *frame)
{
	static PFNGLEGLIMAGETARGETTEXTURE2DOESPROC glEGLImageTargetTexture2DOES = NULL;
	struct gl_image *gl = _PTR(_GET_LONG_FIELD(this, "glImage"));
	int width = gdk_texture_get_width(frame);
	int height = gdk_texture_get_height(frame);
	GLint previous_texture, previous_alignment;
	glGetIntegerv(GL_TEXTURE_BINDING_2D, &previous_texture);
	glGetIntegerv(GL_UNPACK_ALIGNMENT, &previous_alignment);

	if (!gl || gl->width != width || gl->height != height) {
		if (!glEGLImageTargetTexture2DOES)
			glEGLImageTargetTexture2DOES = (PFNGLEGLIMAGETARGETTEXTURE2DOESPROC)eglGetProcAddress("glEGLImageTargetTexture2DOES");
		if (gl) {
			eglDestroyImage(gl->display, gl->image);
			glDeleteTextures(1, &gl->texture);
		} else {
			gl = g_new0(struct gl_image, 1);
			_SET_LONG_FIELD(this, "glImage", _INTPTR(gl));
		}
		gl->display = eglGetCurrentDisplay();
		gl->width = width;
		gl->height = height;
		glGenTextures(1, &gl->texture);
		glBindTexture(GL_TEXTURE_2D, gl->texture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
		gl->image = eglCreateImage(gl->display, eglGetCurrentContext(), EGL_GL_TEXTURE_2D, (EGLClientBuffer)(uintptr_t)gl->texture, NULL);
		if (gl->image == EGL_NO_IMAGE) {
			fprintf(stderr, "SurfaceTexture: can't create an EGLImage: 0x%x\n", eglGetError());
			glDeleteTextures(1, &gl->texture);
			g_free(gl);
			_SET_LONG_FIELD(this, "glImage", 0);
			glBindTexture(GL_TEXTURE_2D, previous_texture);
			return false;
		}
		glBindTexture(GL_TEXTURE_EXTERNAL_OES, tex_name);
		glEGLImageTargetTexture2DOES(GL_TEXTURE_EXTERNAL_OES, gl->image);
	}

	GdkTextureDownloader *downloader = gdk_texture_downloader_new(frame);
	gdk_texture_downloader_set_format(downloader, GDK_MEMORY_R8G8B8A8);
	guchar *pixels = g_malloc(width * height * 4);
	gdk_texture_downloader_download_into(downloader, pixels, width * 4);
	gdk_texture_downloader_free(downloader);
	glBindTexture(GL_TEXTURE_2D, gl->texture);
	glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
	glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
	g_free(pixels);
	glPixelStorei(GL_UNPACK_ALIGNMENT, previous_alignment);
	glBindTexture(GL_TEXTURE_2D, previous_texture);
	return true;
}

/* GLConsumer::updateTexImage(): the SurfaceTexture belongs to the EGL display and context it is first updated in
 * (checkAndUpdateEglStateLocked()); the frame latched now (texture, 0 if none was queued) goes into the external
 * texture, which is then bound */
JNIEXPORT jboolean JNICALL Java_android_graphics_SurfaceTexture_nativeUpdateTexImage(JNIEnv *env, jobject this, jint tex_name, jlong texture)
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
	if (texture && !upload_frame(env, this, tex_name, _PTR(texture)))
		return false;
	glBindTexture(GL_TEXTURE_EXTERNAL_OES, tex_name);
	return true;
}

/* the EGLImage goes with the SurfaceTexture; the GL_TEXTURE_2D belongs to the consumer's context, which is not
 * current here, and goes with that context (as AOSP leaves the GL texture to the app) */
JNIEXPORT void JNICALL Java_android_graphics_SurfaceTexture_nativeRelease(JNIEnv *env, jobject this)
{
	struct gl_image *gl = _PTR(_GET_LONG_FIELD(this, "glImage"));
	if (!gl)
		return;
	eglDestroyImage(gl->display, gl->image);
	g_free(gl);
	_SET_LONG_FIELD(this, "glImage", 0);
}
