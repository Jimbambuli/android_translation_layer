#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <gdk/gdk.h>
#include <stdio.h>
#include <string.h>

#include <drm_fourcc.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_graphics_SurfaceTexture.h"
#include "../widgets/android_view_SurfaceView.h"

/* the GL_TEXTURE_2D the frames are uploaded to, and its EGLImage, which is the storage of the external texture;
 * or the EGLImage of a hardware decoder's frame (dmabuf_image), which then is */
struct gl_image {
	EGLDisplay display;
	EGLImage image;
	GLuint texture;
	int width;
	int height;
	EGLImage dmabuf_image;
};

static PFNGLEGLIMAGETARGETTEXTURE2DOESPROC glEGLImageTargetTexture2DOES = NULL;

static void destroy_images(struct gl_image *gl)
{
	if (gl->image != EGL_NO_IMAGE)
		eglDestroyImage(gl->display, gl->image);
	if (gl->dmabuf_image != EGL_NO_IMAGE)
		eglDestroyImage(gl->display, gl->dmabuf_image);
}

/* the planes of a frame of a hardware decoder (struct atl_dmabuf, "atl-dmabuf" on its GdkTexture) as an EGLImage of
 * the current EGL display (EGL_EXT_image_dma_buf_import): the external texture samples the YUV planes itself, as on
 * Android, where it is backed by the decoder's buffer */
static EGLImage import_dmabuf(EGLDisplay display, const struct atl_dmabuf *dmabuf)
{
	static const EGLint plane_attribs[ATL_DMABUF_MAX_PLANES][5] = {
		{EGL_DMA_BUF_PLANE0_FD_EXT, EGL_DMA_BUF_PLANE0_OFFSET_EXT, EGL_DMA_BUF_PLANE0_PITCH_EXT, EGL_DMA_BUF_PLANE0_MODIFIER_LO_EXT, EGL_DMA_BUF_PLANE0_MODIFIER_HI_EXT},
		{EGL_DMA_BUF_PLANE1_FD_EXT, EGL_DMA_BUF_PLANE1_OFFSET_EXT, EGL_DMA_BUF_PLANE1_PITCH_EXT, EGL_DMA_BUF_PLANE1_MODIFIER_LO_EXT, EGL_DMA_BUF_PLANE1_MODIFIER_HI_EXT},
	};
	const char *extensions = eglQueryString(display, EGL_EXTENSIONS);
	if (!extensions || !strstr(extensions, "EGL_EXT_image_dma_buf_import"))
		return EGL_NO_IMAGE;
	gboolean modifiers = strstr(extensions, "EGL_EXT_image_dma_buf_import_modifiers") != NULL;
	if (!modifiers && dmabuf->modifier != DRM_FORMAT_MOD_INVALID && dmabuf->modifier != DRM_FORMAT_MOD_LINEAR)
		return EGL_NO_IMAGE;

	EGLAttrib attribs[64];
	int n = 0;
	attribs[n++] = EGL_WIDTH;
	attribs[n++] = dmabuf->width;
	attribs[n++] = EGL_HEIGHT;
	attribs[n++] = dmabuf->height;
	attribs[n++] = EGL_LINUX_DRM_FOURCC_EXT;
	attribs[n++] = dmabuf->fourcc;
	for (int i = 0; i < dmabuf->n_planes; i++) {
		attribs[n++] = plane_attribs[i][0];
		attribs[n++] = dmabuf->fd[i];
		attribs[n++] = plane_attribs[i][1];
		attribs[n++] = dmabuf->offset[i];
		attribs[n++] = plane_attribs[i][2];
		attribs[n++] = dmabuf->stride[i];
		if (modifiers && dmabuf->modifier != DRM_FORMAT_MOD_INVALID) {
			attribs[n++] = plane_attribs[i][3];
			attribs[n++] = dmabuf->modifier & 0xffffffff;
			attribs[n++] = plane_attribs[i][4];
			attribs[n++] = dmabuf->modifier >> 32;
		}
	}
	/* the frame's YUV matrix and range (FFmpeg's swscale, the software path, takes BT.601 when it is unset too) */
	attribs[n++] = EGL_YUV_COLOR_SPACE_HINT_EXT;
	attribs[n++] = dmabuf->matrix == 709 ? EGL_ITU_REC709_EXT : dmabuf->matrix == 2020 ? EGL_ITU_REC2020_EXT : EGL_ITU_REC601_EXT;
	attribs[n++] = EGL_SAMPLE_RANGE_HINT_EXT;
	attribs[n++] = dmabuf->full_range ? EGL_YUV_FULL_RANGE_EXT : EGL_YUV_NARROW_RANGE_EXT;
	attribs[n++] = EGL_NONE;
	return eglCreateImage(display, EGL_NO_CONTEXT, EGL_LINUX_DMA_BUF_EXT, NULL, attribs);
}

/* the frame into the external texture tex_name: an external texture can only get its storage from an EGLImage, that
 * of a hardware decoder's frame, or else of a GL_TEXTURE_2D of the same context the pixels are uploaded to */
static gboolean upload_frame(JNIEnv *env, jobject this, GLuint tex_name, GdkTexture *frame)
{
	struct gl_image *gl = _PTR(_GET_LONG_FIELD(this, "glImage"));
	int width = gdk_texture_get_width(frame);
	int height = gdk_texture_get_height(frame);
	GLint previous_texture, previous_alignment;
	if (!glEGLImageTargetTexture2DOES)
		glEGLImageTargetTexture2DOES = (PFNGLEGLIMAGETARGETTEXTURE2DOESPROC)eglGetProcAddress("glEGLImageTargetTexture2DOES");
	if (!gl) {
		gl = g_new0(struct gl_image, 1);
		gl->image = EGL_NO_IMAGE;
		gl->dmabuf_image = EGL_NO_IMAGE;
		_SET_LONG_FIELD(this, "glImage", _INTPTR(gl));
	}

	const struct atl_dmabuf *dmabuf = g_object_get_data(G_OBJECT(frame), "atl-dmabuf");
	EGLImage dmabuf_image = dmabuf ? import_dmabuf(eglGetCurrentDisplay(), dmabuf) : EGL_NO_IMAGE;
	if (gl->dmabuf_image != EGL_NO_IMAGE)
		eglDestroyImage(gl->display, gl->dmabuf_image);
	gl->dmabuf_image = dmabuf_image;
	if (dmabuf_image != EGL_NO_IMAGE) {
		gl->display = eglGetCurrentDisplay();
		glBindTexture(GL_TEXTURE_EXTERNAL_OES, tex_name);
		glEGLImageTargetTexture2DOES(GL_TEXTURE_EXTERNAL_OES, dmabuf_image);
		gl->width = 0; // the memory path sets the GL_TEXTURE_2D's EGLImage again
		return true;
	}

	glGetIntegerv(GL_TEXTURE_BINDING_2D, &previous_texture);
	glGetIntegerv(GL_UNPACK_ALIGNMENT, &previous_alignment);

	if (gl->image == EGL_NO_IMAGE || gl->width != width || gl->height != height) {
		if (gl->image != EGL_NO_IMAGE) {
			eglDestroyImage(gl->display, gl->image);
			glDeleteTextures(1, &gl->texture);
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
			gl->texture = 0;
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

/* GLConsumer::detachFromContext(): only in the SurfaceTexture's own EGL display and context, if it has them; the
 * GL objects of that context (the texture tex_name and the one of the EGLImage) are deleted if it is current */
JNIEXPORT jboolean JNICALL Java_android_graphics_SurfaceTexture_nativeDetachFromGLContext(JNIEnv *env, jobject this, jint tex_name)
{
	EGLDisplay dpy = eglGetCurrentDisplay();
	EGLContext ctx = eglGetCurrentContext();
	EGLDisplay own_dpy = _PTR(_GET_LONG_FIELD(this, "eglDisplay"));
	EGLContext own_ctx = _PTR(_GET_LONG_FIELD(this, "eglContext"));
	if (own_dpy != EGL_NO_DISPLAY && own_dpy != dpy) {
		fprintf(stderr, "SurfaceTexture: detachFromContext: invalid current EGLDisplay\n");
		return false;
	}
	if (own_ctx != EGL_NO_CONTEXT && own_ctx != ctx) {
		fprintf(stderr, "SurfaceTexture: detachFromContext: invalid current EGLContext\n");
		return false;
	}
	gboolean current = dpy != EGL_NO_DISPLAY && ctx != EGL_NO_CONTEXT;
	struct gl_image *gl = _PTR(_GET_LONG_FIELD(this, "glImage"));
	if (gl) {
		destroy_images(gl);
		if (current && gl->texture)
			glDeleteTextures(1, &gl->texture);
		g_free(gl);
		_SET_LONG_FIELD(this, "glImage", 0);
	}
	if (current) {
		GLuint texture = tex_name;
		glDeleteTextures(1, &texture);
	}
	_SET_LONG_FIELD(this, "eglDisplay", 0);
	_SET_LONG_FIELD(this, "eglContext", 0);
	return true;
}

/* the EGLImage goes with the SurfaceTexture; the GL_TEXTURE_2D belongs to the consumer's context, which is not
 * current here, and goes with that context (as AOSP leaves the GL texture to the app) */
JNIEXPORT void JNICALL Java_android_graphics_SurfaceTexture_nativeRelease(JNIEnv *env, jobject this)
{
	struct gl_image *gl = _PTR(_GET_LONG_FIELD(this, "glImage"));
	if (!gl)
		return;
	destroy_images(gl);
	g_free(gl);
	_SET_LONG_FIELD(this, "glImage", 0);
}
