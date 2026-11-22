#define _GNU_SOURCE // memfd_create()

#include <gdk/wayland/gdkwayland.h>
#include <gdk/x11/gdkx.h>
#include <gtk/gtk.h>

#include <wayland-egl.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>

#include <GL/gl.h>
#include <GLES2/gl2.h>

#include <drm/drm_fourcc.h>
#include <stdbool.h>
#include <sys/mman.h>
#include <unistd.h>

// FIXME: put the header in a common place
#include "../api-impl-jni/defines.h"
#include "../api-impl-jni/widgets/android_view_SurfaceView.h"

#include "hardware_buffer.h"
#include "native_window.h"

extern GtkWindow *window; // TODO: how do we get rid of this? the app won't pass anything useful to eglGetDisplay

static GHashTable *egl_surface_hashtable;

/* the ANativeWindow of an EGL window surface ATL made, or NULL (also before the first one: no table yet) */
static struct ANativeWindow *surface_native_window(EGLSurface surface)
{
	return egl_surface_hashtable ? g_hash_table_lookup(egl_surface_hashtable, surface) : NULL;
}

// temporary for debugging
static void PrintConfigAttributes(EGLDisplay display, EGLConfig config)
{
	EGLint value;
	printf("-------------------------------------------------------------------------------\n");
	eglGetConfigAttrib(display, config, EGL_CONFIG_ID, &value);
	printf("EGL_CONFIG_ID %d\n", value);

	eglGetConfigAttrib(display, config, EGL_BUFFER_SIZE, &value);
	printf("EGL_BUFFER_SIZE %d\n", value);
	eglGetConfigAttrib(display, config, EGL_RED_SIZE, &value);
	printf("EGL_RED_SIZE %d\n", value);
	eglGetConfigAttrib(display, config, EGL_GREEN_SIZE, &value);
	printf("EGL_GREEN_SIZE %d\n", value);
	eglGetConfigAttrib(display, config, EGL_BLUE_SIZE, &value);
	printf("EGL_BLUE_SIZE %d\n", value);
	eglGetConfigAttrib(display, config, EGL_ALPHA_SIZE, &value);
	printf("EGL_ALPHA_SIZE %d\n", value);
	eglGetConfigAttrib(display, config, EGL_DEPTH_SIZE, &value);
	printf("EGL_DEPTH_SIZE %d\n", value);
	eglGetConfigAttrib(display, config, EGL_STENCIL_SIZE, &value);
	printf("EGL_STENCIL_SIZE %d\n", value);
	eglGetConfigAttrib(display, config, EGL_SAMPLE_BUFFERS, &value);
	printf("EGL_SAMPLE_BUFFERS %d\n", value);
	eglGetConfigAttrib(display, config, EGL_SAMPLES, &value);
	printf("EGL_SAMPLES %d\n", value);

	eglGetConfigAttrib(display, config, EGL_CONFIG_CAVEAT, &value);
	switch (value) {
		case EGL_NONE:
			printf("EGL_CONFIG_CAVEAT EGL_NONE\n");
			break;
		case EGL_SLOW_CONFIG:
			printf("EGL_CONFIG_CAVEAT EGL_SLOW_CONFIG\n");
			break;
	}

	eglGetConfigAttrib(display, config, EGL_MAX_PBUFFER_WIDTH, &value);
	printf("EGL_MAX_PBUFFER_WIDTH %d\n", value);
	eglGetConfigAttrib(display, config, EGL_MAX_PBUFFER_HEIGHT, &value);
	printf("EGL_MAX_PBUFFER_HEIGHT %d\n", value);
	eglGetConfigAttrib(display, config, EGL_MAX_PBUFFER_PIXELS, &value);
	printf("EGL_MAX_PBUFFER_PIXELS %d\n", value);
	eglGetConfigAttrib(display, config, EGL_NATIVE_RENDERABLE, &value);
	printf("EGL_NATIVE_RENDERABLE %s \n", (value ? "true" : "false"));
	eglGetConfigAttrib(display, config, EGL_NATIVE_VISUAL_ID, &value);
	printf("EGL_NATIVE_VISUAL_ID %d\n", value);
	eglGetConfigAttrib(display, config, EGL_NATIVE_VISUAL_TYPE, &value);
	printf("EGL_NATIVE_VISUAL_TYPE %d\n", value);
	eglGetConfigAttrib(display, config, EGL_RENDERABLE_TYPE, &value);
	printf("EGL_RENDERABLE_TYPE %d\n", value);
	eglGetConfigAttrib(display, config, EGL_SURFACE_TYPE, &value);
	printf("EGL_SURFACE_TYPE %d\n", value);
	eglGetConfigAttrib(display, config, EGL_TRANSPARENT_TYPE, &value);
	printf("EGL_TRANSPARENT_TYPE %d\n", value);
	printf("-------------------------------------------------------------------------------\n");
}

// this is an extension that only android implements, we can hopefully get away with just stubbing it
EGLBoolean bionic_eglPresentationTimeANDROID(EGLDisplay dpy, EGLSurface surface, EGLnsecsANDROID time)
{
	return EGL_TRUE;
}

EGLBoolean bionic_eglSwapBuffers(EGLDisplay display, EGLSurface surface);
EGLImage bionic_eglCreateImage(EGLDisplay dpy, EGLContext ctx, EGLenum target, EGLClientBuffer buffer, const EGLAttrib *attrib_list);
EGLImage bionic_eglCreateImageKHR(EGLDisplay dpy, EGLContext ctx, EGLenum target, EGLClientBuffer buffer, const EGLint *attrib_list);

/* EGL_ANDROID_get_native_client_buffer (eglGetNativeClientBufferANDROIDImpl()): the EGLClientBuffer of an
 * AHardwareBuffer, for eglCreateImageKHR(EGL_NATIVE_BUFFER_ANDROID); AOSP gives NULL for a NULL buffer (with
 * EGL_BAD_PARAMETER, which the host's eglGetError() does not know about). */
EGLClientBuffer bionic_eglGetNativeClientBufferANDROID(const struct AHardwareBuffer *buffer)
{
	struct AHardwareBuffer *ahb = ahb_from_pointer(buffer);
	/* AOSP: the buffer's ANativeWindowBuffer (AHardwareBuffer_to_ANativeWindowBuffer()) */
	return ahb ? (EGLClientBuffer)&ahb->anwb : NULL;
}

/* EGL_KHR_partial_update: the damage region is a hint; a window surface of an ANativeWindow is ATL's own back
 * buffer, which the host EGL does not know, so it is only accepted there */
static EGLBoolean bionic_eglSetDamageRegionKHR(EGLDisplay display, EGLSurface surface, EGLint *rects, EGLint n_rects)
{
	if (surface_native_window(surface))
		return EGL_TRUE;
	EGLBoolean (*host)(EGLDisplay, EGLSurface, EGLint *, EGLint) = (void *)eglGetProcAddress("eglSetDamageRegionKHR");
	return host ? host(display, surface, rects, n_rects) : EGL_FALSE;
}

/* EGL_KHR/EXT_swap_buffers_with_damage: swaps the whole buffer, as eglSwapBuffers() (the damage is a hint) */
static EGLBoolean bionic_eglSwapBuffersWithDamage(EGLDisplay display, EGLSurface surface, const EGLint *rects, EGLint n_rects)
{
	return bionic_eglSwapBuffers(display, surface);
}

/* GL_KHR_debug entry points that OpenGL ES drivers export under their KHR names; Mesa exports only the core
 * names, so they are taken from the driver by name */
typedef void (*GLDEBUGPROCKHR_)(GLenum source, GLenum type, GLuint id, GLenum severity, GLsizei length, const GLchar *message, const void *userParam);

void bionic_glDebugMessageCallbackKHR(GLDEBUGPROCKHR_ callback, const void *userParam)
{
	void (*func)(GLDEBUGPROCKHR_, const void *) = (void *)eglGetProcAddress("glDebugMessageCallbackKHR");
	if (!func)
		func = (void *)eglGetProcAddress("glDebugMessageCallback");
	if (func)
		func(callback, userParam);
}

void bionic_glGetPointervKHR(GLenum pname, void **params)
{
	void (*func)(GLenum, void **) = (void *)eglGetProcAddress("glGetPointervKHR");
	if (!func)
		func = (void *)eglGetProcAddress("glGetPointerv");
	if (func)
		func(pname, params);
}

void (*bionic_eglGetProcAddress(char const *procname))(void)
{
	if (__unlikely__(!strcmp(procname, "eglPresentationTimeANDROID")))
		return (void (*)(void))bionic_eglPresentationTimeANDROID;
	if (!strcmp(procname, "eglGetNativeClientBufferANDROID"))
		return (void (*)(void))bionic_eglGetNativeClientBufferANDROID;
	if (!strcmp(procname, "eglCreateImageKHR"))
		return (void (*)(void))bionic_eglCreateImageKHR;
	if (!strcmp(procname, "eglCreateImage"))
		return (void (*)(void))bionic_eglCreateImage;
	if (!getenv("ATL_DIRECT_EGL")) {
		if (!strcmp(procname, "eglSetDamageRegionKHR"))
			return (void (*)(void))bionic_eglSetDamageRegionKHR;
		if (!strcmp(procname, "eglSwapBuffersWithDamageKHR") || !strcmp(procname, "eglSwapBuffersWithDamageEXT"))
			return (void (*)(void))bionic_eglSwapBuffersWithDamage;
		/* EGL_KHR_get_all_proc_addresses: the same function as the exported one, which binds the back buffer of
		 * an ANativeWindow surface for framebuffer 0 (Flutter's Impeller loads all GL functions this way) */
		if (!strcmp(procname, "glBindFramebuffer"))
			return (void (*)(void))bionic_glBindFramebuffer;
	}

	return eglGetProcAddress(procname);
}

EGLDisplay bionic_eglGetDisplay(EGLNativeDisplayType native_display)
{
	/*
	 * On android, at least SDL passes 0 (EGL_DISPLAY_DEFAULT) to eglGetDisplay and uses the resulting display.
	 * We obviously want to make the app use the correct display, which may happen to be a different one
	 * than the "default" display (especially on Wayland)
	 */
	GdkDisplay *display = gtk_root_get_display(GTK_ROOT(window));

	if (GDK_IS_WAYLAND_DISPLAY(display)) {
		struct wl_display *wl_display = gdk_wayland_display_get_wl_display(display);
		return eglGetPlatformDisplay(EGL_PLATFORM_WAYLAND_KHR, wl_display, NULL);
	} else if (GDK_IS_X11_DISPLAY(display)) {
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
		Display *x11_display = gdk_x11_display_get_xdisplay(display);
#pragma GCC diagnostic pop
		return eglGetPlatformDisplay(EGL_PLATFORM_X11_KHR, x11_display, NULL);
	} else {
		return NULL;
	}
}

/* The display apps get is GTK's own (see above). On Android, eglTerminate() of a display only ends it when
 * every eglInitialize() is matched (egl_display_t::terminate() counts them). Here an app that terminates it
 * would take GTK's contexts with it, and the window would not be drawn any more (Firefox does that at its
 * first start). GTK keeps the display until the end, so it stays initialized. */
EGLBoolean bionic_eglTerminate(EGLDisplay display)
{
	if (display == bionic_eglGetDisplay(EGL_DEFAULT_DISPLAY))
		return EGL_TRUE;
	return eglTerminate(display);
}

EGLBoolean bionic_eglChooseConfig(EGLDisplay display, EGLint *attrib_list, EGLConfig *configs, EGLint config_size, EGLint *num_config)
{
	GdkDisplay *gdk_display = gtk_root_get_display(GTK_ROOT(window));

	if (GDK_IS_X11_DISPLAY(gdk_display)) {
		/* X11 supports pbuffers just fine */
		return eglChooseConfig(display, attrib_list, configs, config_size, num_config);
	} else {
		bool has_pbuffer_bit = false;
		int attrib_list_size = 0;
		for (EGLint *attr = attrib_list; *attr != EGL_NONE; attr += 2) {
			if (*attr == EGL_SURFACE_TYPE && (*(attr + 1) & EGL_PBUFFER_BIT) && *(attr + 1) != EGL_DONT_CARE) {
				has_pbuffer_bit = true;
			}
			attrib_list_size += 2;
		}
		attrib_list_size += 1; // for EGL_NONE
		if (has_pbuffer_bit) {
			/* copy the list in case it's mapped read-only */
			EGLint *new_attrib_list = malloc(sizeof(EGLint) * attrib_list_size);
			memcpy(new_attrib_list, attrib_list, sizeof(EGLint) * attrib_list_size);
			for (EGLint *attr = new_attrib_list; *attr != EGL_NONE; attr += 2) {
				if (*attr == EGL_SURFACE_TYPE && *(attr + 1) != EGL_DONT_CARE) {
					*(attr + 1) &= ~EGL_PBUFFER_BIT;
					*(attr + 1) |= EGL_WINDOW_BIT;
				}
			}
			EGLBoolean ret = eglChooseConfig(display, new_attrib_list, configs, config_size, num_config);
			free(new_attrib_list);
			return ret;
		} else {
			return eglChooseConfig(display, attrib_list, configs, config_size, num_config);
		}
	}
}

/* the pbuffers that are wl_egl_window surfaces of an unmapped wl_surface (Wayland has none) */
static GHashTable *wayland_pbuffers = NULL;

EGLSurface bionic_eglCreatePbufferSurface(EGLDisplay display, EGLConfig config, EGLint const *attrib_list)
{
	GdkDisplay *gdk_display = gtk_root_get_display(GTK_ROOT(window));

	if (GDK_IS_X11_DISPLAY(gdk_display)) {
		/* X11 supports pbuffers just fine */
		return eglCreatePbufferSurface(display, config, attrib_list);
	} else {
		struct wl_compositor *wl_compositor = gdk_wayland_display_get_wl_compositor(gdk_display);
		struct wl_surface *wayland_surface = wl_compositor_create_surface(wl_compositor);
		EGLint width = 0;
		EGLint height = 0;
		EGLint *new_attrib_list = NULL;
		if (attrib_list) {
			size_t attrib_list_len = 0;
			for (EGLint *attr = (EGLint *)attrib_list; *attr != EGL_NONE; attr++)
				attrib_list_len++;
			new_attrib_list = malloc(sizeof(EGLint) * (attrib_list_len + 1));
			EGLint *new_attr_pos = new_attrib_list;
			for (EGLint *attr = (EGLint *)attrib_list; *attr != EGL_NONE; attr += 2) {
				if (*attr == EGL_WIDTH) {
					width = *(attr + 1);
				} else if (*attr == EGL_HEIGHT) {
					height = *(attr + 1);
				} else {
					*new_attr_pos = *attr;
					*(new_attr_pos + 1) = *(attr + 1);
					new_attr_pos += 2;
				}
			}
			*new_attr_pos = EGL_NONE;
		}
		struct wl_egl_window *egl_window = wl_egl_window_create(wayland_surface, width, height);
		EGLSurface surface = eglCreateWindowSurface(display, config, (EGLNativeWindowType)egl_window, new_attrib_list);
		if (surface) {
			if (!wayland_pbuffers)
				wayland_pbuffers = g_hash_table_new(NULL, NULL);
			g_hash_table_add(wayland_pbuffers, surface);
		}
		return surface;
	}
}

#define NUM_BUFFERS 3

struct _ATLSurface {
	GObject parent;
	int width;
	int height;
	SurfaceViewWidget *surface_view_widget;
	int32_t framebuffer_format;
	int32_t renderbuffer_format;
	unsigned int renderbuffer_attachment;
	uint32_t renderbuffer;
	struct atl_surface_buffer {
		struct _ATLSurface *surface;
		EGLImage egl_image;
		GdkGLTextureBuilder *texture_builder;
		uint32_t gl_texture;
		uint32_t gl_framebuffer;
		int width; /* the size the buffer was made in */
		int height;
		int fd; /* of a remote window: the dmabuf or memfd the other process shows */
		void *map; /* of a memfd */
		size_t size;
	} buffers[NUM_BUFFERS];
	struct atl_surface_buffer *back_buffer;
	struct atl_surface_buffer *front_buffer;
	GAsyncQueue *vsync;
	GAsyncQueue *unused_buffers;
	gboolean destroyed;
	/* the window of a Surface of another process (ANativeWindow.remote_fd): the buffers go there instead of to a
	 * widget of this process */
	struct ANativeWindow *remote;
};
G_DECLARE_FINAL_TYPE(ATLSurface, atl_surface, ATL, SURFACE, GObject);
static void atl_surface_dispose(GObject *object)
{
	ATLSurface *atl_surface = ATL_SURFACE(object);
	if (atl_surface->remote) {
		for (int i = 0; i < NUM_BUFFERS; i++) {
			if (atl_surface->buffers[i].map)
				munmap(atl_surface->buffers[i].map, atl_surface->buffers[i].size);
			if (atl_surface->buffers[i].fd >= 0)
				close(atl_surface->buffers[i].fd);
		}
		if (atl_surface->unused_buffers)
			g_async_queue_unref(atl_surface->unused_buffers);
		if (atl_surface->vsync)
			g_async_queue_unref(atl_surface->vsync);
		return;
	}
	GdkGLContext *gtk_gl_context = gdk_surface_create_gl_context(gtk_native_get_surface(gtk_widget_get_native(GTK_WIDGET(atl_surface->surface_view_widget))), NULL);
	gdk_gl_context_make_current(gtk_gl_context);
	for (int i = 0; i < NUM_BUFFERS; i++) {
		if (atl_surface->buffers[i].texture_builder) {
			GLuint texture_id = gdk_gl_texture_builder_get_id(atl_surface->buffers[i].texture_builder);
			glDeleteTextures(1, &texture_id);
			g_object_unref(atl_surface->buffers[i].texture_builder);
		}
	}
	g_async_queue_unref(atl_surface->unused_buffers);
	g_async_queue_unref(atl_surface->vsync);
	g_object_unref(atl_surface->surface_view_widget);
}
static void atl_surface_class_init(ATLSurfaceClass *class)
{
	G_OBJECT_CLASS(class)->dispose = atl_surface_dispose;
}
static void atl_surface_init(ATLSurface *self) {}
G_DEFINE_TYPE(ATLSurface, atl_surface, G_TYPE_OBJECT)

/*
 * Android exports eglCreateImageKHR (with EGLAttrib) as a direct libEGL.so symbol,
 * while Mesa exports the core EGL 1.5 function eglCreateImage as its linkable entry
 * point instead. The wrappers bridge this ABI naming difference.
 */
/* EGL_NATIVE_BUFFER_ANDROID (an AHardwareBuffer, EGL_ANDROID_image_native_buffer) becomes the EGL_LINUX_DMA_BUF_EXT
 * image of its dma-buf; EGL_IMAGE_PRESERVED_KHR (the only attribute of a native buffer image) is passed on.
 * eglCreateImageKHR() takes EGLint attributes, eglCreateImage() EGLAttrib ones. */
#ifndef EGL_NATIVE_BUFFER_ANDROID
#define EGL_NATIVE_BUFFER_ANDROID 0x3140
#endif
EGLImage bionic_eglCreateImage(EGLDisplay dpy, EGLContext ctx, EGLenum target, EGLClientBuffer buffer, const EGLAttrib *attrib_list)
{
	EGLAttrib attribs[64];
	int n = 0;
	bool native = target == EGL_NATIVE_BUFFER_ANDROID;
	if (native) {
		struct AHardwareBuffer *ahb = ahb_from_client_buffer(buffer);
		if (!ahb || !ahb->dmabuf)
			return EGL_NO_IMAGE;
		static const EGLAttrib plane_attribs[3][5] = {
			{EGL_DMA_BUF_PLANE0_FD_EXT, EGL_DMA_BUF_PLANE0_OFFSET_EXT, EGL_DMA_BUF_PLANE0_PITCH_EXT, EGL_DMA_BUF_PLANE0_MODIFIER_LO_EXT, EGL_DMA_BUF_PLANE0_MODIFIER_HI_EXT},
			{EGL_DMA_BUF_PLANE1_FD_EXT, EGL_DMA_BUF_PLANE1_OFFSET_EXT, EGL_DMA_BUF_PLANE1_PITCH_EXT, EGL_DMA_BUF_PLANE1_MODIFIER_LO_EXT, EGL_DMA_BUF_PLANE1_MODIFIER_HI_EXT},
			{EGL_DMA_BUF_PLANE2_FD_EXT, EGL_DMA_BUF_PLANE2_OFFSET_EXT, EGL_DMA_BUF_PLANE2_PITCH_EXT, EGL_DMA_BUF_PLANE2_MODIFIER_LO_EXT, EGL_DMA_BUF_PLANE2_MODIFIER_HI_EXT},
		};
		attribs[n++] = EGL_WIDTH;
		attribs[n++] = ahb->desc.width;
		attribs[n++] = EGL_HEIGHT;
		attribs[n++] = ahb->desc.height;
		attribs[n++] = EGL_LINUX_DRM_FOURCC_EXT;
		attribs[n++] = ahb->drm_format;
		for (int i = 0; i < ahb->plane_count && i < 3; i++) {
			attribs[n++] = plane_attribs[i][0];
			attribs[n++] = ahb->fd;
			attribs[n++] = plane_attribs[i][1];
			attribs[n++] = ahb->offsets[i];
			attribs[n++] = plane_attribs[i][2];
			attribs[n++] = ahb->strides[i];
			if (ahb->modifier != DRM_FORMAT_MOD_INVALID) {
				attribs[n++] = plane_attribs[i][3];
				attribs[n++] = (EGLAttrib)(ahb->modifier & 0xffffffff);
				attribs[n++] = plane_attribs[i][4];
				attribs[n++] = (EGLAttrib)(ahb->modifier >> 32);
			}
		}
		target = EGL_LINUX_DMA_BUF_EXT;
		buffer = NULL;
		ctx = EGL_NO_CONTEXT;
	}
	for (; attrib_list && attrib_list[0] != EGL_NONE && n < 62; attrib_list += 2) {
		if (native && attrib_list[0] != EGL_IMAGE_PRESERVED_KHR)
			continue;
		attribs[n++] = attrib_list[0];
		attribs[n++] = attrib_list[1];
	}
	attribs[n] = EGL_NONE;
	return eglCreateImage(dpy, ctx, target, buffer, attribs);
}

EGLImage bionic_eglCreateImageKHR(EGLDisplay dpy, EGLContext ctx, EGLenum target, EGLClientBuffer buffer, const EGLint *attrib_list)
{
	EGLAttrib attribs[64];
	int n = 0;
	for (; attrib_list && attrib_list[0] != EGL_NONE && n < 62; attrib_list += 2) {
		attribs[n++] = attrib_list[0];
		attribs[n++] = attrib_list[1];
	}
	attribs[n] = EGL_NONE;
	return bionic_eglCreateImage(dpy, ctx, target, buffer, attribs);
}

EGLBoolean bionic_eglDestroyImageKHR(EGLDisplay dpy, EGLImage image)
{
	return eglDestroyImage(dpy, image);
}

EGLSurface bionic_eglCreateWindowSurface(EGLDisplay display, EGLConfig config, struct ANativeWindow *native_window, EGLint const *attrib_list)
{
	// better than crashing (TODO: check if apps try to use the NULL value anyway)
	if (!native_window)
		return NULL;

	if (!egl_surface_hashtable)
		egl_surface_hashtable = g_hash_table_new(NULL, NULL);

	ANativeWindow_acquire(native_window);

	PrintConfigAttributes(display, config);
	EGLSurface surface;
	if (getenv("ATL_DIRECT_EGL")) {
		surface = eglCreateWindowSurface(display, config, native_window->egl_window, attrib_list);
	} else {
		ATLSurface *atl_surface = g_object_new(atl_surface_get_type(), NULL);
		atl_surface->width = native_window->width;
		atl_surface->height = native_window->height;
		if (native_window->remote_fd >= 0) {
			atl_surface->remote = native_window;
			for (int i = 0; i < NUM_BUFFERS; i++)
				atl_surface->buffers[i].fd = -1;
		} else
			atl_surface->surface_view_widget = SURFACE_VIEW_WIDGET(g_object_ref(native_window->surface_view_widget));
		EGLint alpha_size = 0;
		EGLint depth_size = 0;
		EGLint stencil_size = 0;
		eglGetConfigAttrib(display, config, EGL_ALPHA_SIZE, &alpha_size);
		eglGetConfigAttrib(display, config, EGL_DEPTH_SIZE, &depth_size);
		eglGetConfigAttrib(display, config, EGL_STENCIL_SIZE, &stencil_size);
		if (alpha_size > 0) {
			atl_surface->framebuffer_format = GL_RGBA;
		} else {
			atl_surface->framebuffer_format = GL_RGB;
		}
		if (depth_size > 0 && stencil_size > 0) {
			atl_surface->renderbuffer_format = GL_DEPTH24_STENCIL8;
			atl_surface->renderbuffer_attachment = GL_DEPTH_STENCIL_ATTACHMENT;
		} else if (depth_size > 0) {
			atl_surface->renderbuffer_format = GL_DEPTH_COMPONENT16;
			atl_surface->renderbuffer_attachment = GL_DEPTH_ATTACHMENT;
		} else if (stencil_size > 0) {
			atl_surface->renderbuffer_format = GL_STENCIL_INDEX8;
			atl_surface->renderbuffer_attachment = GL_STENCIL_ATTACHMENT;
		}
		surface = atl_surface;
	}

	printf("EGL::: native_window->egl_window: %p\n", native_window->egl_window);
	printf("EGL::: eglGetError: %d\n", eglGetError());

	printf("EGL::: ret: %p\n", surface);

	g_hash_table_insert(egl_surface_hashtable, surface, native_window);

	if (!surface)
		ANativeWindow_release(native_window);

	return surface;
}

EGLBoolean bionic_eglDestroySurface(EGLDisplay display, EGLSurface surface)
{
	if (wayland_pbuffers)
		g_hash_table_remove(wayland_pbuffers, surface);
	struct ANativeWindow *native_window = surface_native_window(surface);

	if (!native_window)
		return eglDestroySurface(display, surface);

	EGLBoolean ret = EGL_TRUE;
	if (getenv("ATL_DIRECT_EGL")) {
		ret = eglDestroySurface(display, surface);
	} else {
		ATLSurface *atl_surface = surface;
		atl_surface->destroyed = TRUE;
		if (atl_surface->remote)
			ANativeWindow_remoteSetHandler(atl_surface->remote, NULL, NULL);
		for (int i = 0; i < NUM_BUFFERS; i++) {
			if (atl_surface->buffers[i].egl_image)
				eglDestroyImage(display, atl_surface->buffers[i].egl_image);
			atl_surface->buffers[i].egl_image = 0;
			if (atl_surface->buffers[i].gl_framebuffer)
				glDeleteFramebuffers(1, &atl_surface->buffers[i].gl_framebuffer);
			if (atl_surface->buffers[i].gl_texture)
				glDeleteTextures(1, &atl_surface->buffers[i].gl_texture);
		}
		g_object_unref(atl_surface);
	}
	if (ret) {
		g_hash_table_remove(egl_surface_hashtable, surface);
		ANativeWindow_release(native_window);
	}

	return ret;
}

static GHashTable *draw_surface_hashtable = NULL;
static GHashTable *read_surface_hashtable = NULL;

/* -- the window of a Surface of another process: a BufferQueue over a socket (android_view_SurfaceView.h) -- */

/* hands buffer i to the process showing the surface: as dmabuf if the driver exports one
 * (EGL_MESA_image_dma_buf_export), else as memfd that eglSwapBuffers() copies the frame into */
static void remote_buffer_share(ATLSurface *atl_surface, int i)
{
	struct atl_surface_buffer *buffer = &atl_surface->buffers[i];
	EGLDisplay display = eglGetCurrentDisplay();
	struct atl_surface_msg msg = {.type = ATL_SURFACE_MSG_BUFFER, .index = i, .width = atl_surface->width, .height = atl_surface->height};
	static PFNEGLEXPORTDMABUFIMAGEQUERYMESAPROC eglExportDMABUFImageQueryMESA;
	static PFNEGLEXPORTDMABUFIMAGEMESAPROC eglExportDMABUFImageMESA;
	static int has_export = -1;
	if (has_export < 0) {
		const char *extensions = eglQueryString(display, EGL_EXTENSIONS);
		has_export = extensions && strstr(extensions, "EGL_MESA_image_dma_buf_export");
		eglExportDMABUFImageQueryMESA = (PFNEGLEXPORTDMABUFIMAGEQUERYMESAPROC)eglGetProcAddress("eglExportDMABUFImageQueryMESA");
		eglExportDMABUFImageMESA = (PFNEGLEXPORTDMABUFIMAGEMESAPROC)eglGetProcAddress("eglExportDMABUFImageMESA");
	}
	int fourcc = 0;
	int n_planes = 0;
	EGLuint64KHR modifier = 0;
	EGLint stride = 0;
	EGLint offset = 0;
	if (has_export > 0 && buffer->egl_image && !getenv("ATL_SURFACE_NO_DMABUF") &&
	    eglExportDMABUFImageQueryMESA(display, buffer->egl_image, &fourcc, &n_planes, &modifier) && n_planes == 1 &&
	    eglExportDMABUFImageMESA(display, buffer->egl_image, &buffer->fd, &stride, &offset)) {
		msg.fourcc = fourcc;
		msg.modifier = modifier;
		msg.stride = stride;
		msg.offset = offset;
	} else {
		msg.stride = atl_surface->width * 4;
		buffer->size = (size_t)msg.stride * atl_surface->height;
		buffer->fd = memfd_create("atl-surface", MFD_CLOEXEC);
		if (buffer->fd < 0 || ftruncate(buffer->fd, buffer->size) < 0 ||
		    (buffer->map = mmap(NULL, buffer->size, PROT_READ | PROT_WRITE, MAP_SHARED, buffer->fd, 0)) == MAP_FAILED) {
			fprintf(stderr, "eglCreateWindowSurface: no buffer for a surface of another process: %m\n");
			buffer->map = NULL;
			return;
		}
	}
	fprintf(stderr, "EGL::: buffer %d of a surface of another process: %s, %dx%d\n", i, msg.fourcc ? "dmabuf" : "memfd", msg.width, msg.height);
	if (buffer->egl_image)
		eglDestroyImage(display, buffer->egl_image);
	buffer->egl_image = NULL;
	ANativeWindow_remoteSend(atl_surface->remote, &msg, buffer->fd);
}

/* ANativeWindow_remoteRead(), with remote_lock held */
static void remote_message(void *data, const struct atl_surface_msg *msg)
{
	ATLSurface *atl_surface = data;
	if (msg->type == ATL_SURFACE_MSG_RELEASE && msg->index < NUM_BUFFERS)
		g_async_queue_push(atl_surface->unused_buffers, &atl_surface->buffers[msg->index]);
	else if (msg->type == ATL_SURFACE_MSG_FRAME)
		g_async_queue_push(atl_surface->vsync, GINT_TO_POINTER(1));
}

/* g_async_queue_timeout_pop() for the queues filled by the messages of the other process */
static gpointer remote_pop(ATLSurface *atl_surface, GAsyncQueue *queue, int timeout_ms)
{
	gint64 end = g_get_monotonic_time() + timeout_ms * 1000;
	gpointer item;
	while (!(item = g_async_queue_try_pop(queue))) {
		int left = (end - g_get_monotonic_time()) / 1000;
		if (left <= 0 || !ANativeWindow_remoteRead(atl_surface->remote, left))
			return g_async_queue_try_pop(queue);
	}
	return item;
}

static void fit_back_buffer(ATLSurface *atl_surface, struct ANativeWindow *native_window);

/* eglSwapBuffers(): the back buffer goes to the other process (a memfd gets the frame copied in), like the frames
 * of a widget of this process paced by its frame callbacks */
static EGLBoolean remote_swap_buffers(ATLSurface *atl_surface)
{
	struct atl_surface_buffer *buffer = atl_surface->back_buffer;
	if (buffer && buffer->map) {
		/* the app's pack state must not change where the pixels go */
		GLint pack_buffer = 0, row_length = 0, skip_rows = 0, skip_pixels = 0, alignment = 4;
		const char *version = (const char *)glGetString(GL_VERSION);
		gboolean es3 = version && strncmp(version, "OpenGL ES ", 10) == 0 && version[10] >= '3';
		if (es3) {
			glGetIntegerv(0x88ED /* GL_PIXEL_PACK_BUFFER_BINDING */, &pack_buffer);
			glGetIntegerv(0x0D02 /* GL_PACK_ROW_LENGTH */, &row_length);
			glGetIntegerv(0x0D03 /* GL_PACK_SKIP_ROWS */, &skip_rows);
			glGetIntegerv(0x0D04 /* GL_PACK_SKIP_PIXELS */, &skip_pixels);
			glBindBuffer(0x88EB /* GL_PIXEL_PACK_BUFFER */, 0);
			glPixelStorei(0x0D02, 0);
			glPixelStorei(0x0D03, 0);
			glPixelStorei(0x0D04, 0);
		}
		glGetIntegerv(GL_PACK_ALIGNMENT, &alignment);
		glPixelStorei(GL_PACK_ALIGNMENT, 4);
		glBindFramebuffer(GL_FRAMEBUFFER, buffer->gl_framebuffer);
		glReadPixels(0, 0, buffer->width, buffer->height, GL_RGBA, GL_UNSIGNED_BYTE, buffer->map);
		glPixelStorei(GL_PACK_ALIGNMENT, alignment);
		if (es3) {
			glBindBuffer(0x88EB, pack_buffer);
			glPixelStorei(0x0D02, row_length);
			glPixelStorei(0x0D03, skip_rows);
			glPixelStorei(0x0D04, skip_pixels);
		}
	} else if (buffer) {
		glFinish(); // the dmabuf is read by the other process' GPU context: no fence crosses the processes here
	}
	if (buffer && buffer->fd >= 0) {
		remote_pop(atl_surface, atl_surface->vsync, 50);
		ANativeWindow_remoteSend(atl_surface->remote, &(struct atl_surface_msg){.type = ATL_SURFACE_MSG_PRESENT, .index = buffer - atl_surface->buffers}, -1);
		atl_surface->back_buffer = NULL;
	}
	/* a process that is gone gives no buffer back: draw into the same one again */
	if (!atl_surface->back_buffer)
		atl_surface->back_buffer = atl_surface->remote->remote_gone ? buffer : remote_pop(atl_surface, atl_surface->unused_buffers, 100);
	fit_back_buffer(atl_surface, atl_surface->remote);
	glBindFramebuffer(GL_FRAMEBUFFER, atl_surface->back_buffer ? atl_surface->back_buffer->gl_framebuffer : 0);
	return EGL_TRUE;
}

/* a buffer of the surface's current size: a GL_TEXTURE_2D with an EGLImage for GTK, as colour attachment of a
 * framebuffer (with the surface's depth/stencil renderbuffer) */
static void make_buffer(ATLSurface *atl_surface, struct atl_surface_buffer *buffer)
{
	glGenTextures(1, &buffer->gl_texture);
	glBindTexture(GL_TEXTURE_2D, buffer->gl_texture);
	glTexImage2D(GL_TEXTURE_2D, 0, atl_surface->framebuffer_format, atl_surface->width, atl_surface->height, 0, atl_surface->framebuffer_format, GL_UNSIGNED_BYTE, NULL);
	buffer->egl_image = eglCreateImage(eglGetCurrentDisplay(), eglGetCurrentContext(), EGL_GL_TEXTURE_2D,
	                                   (EGLClientBuffer)(uintptr_t)buffer->gl_texture, NULL);
	glGenFramebuffers(1, &buffer->gl_framebuffer);
	glBindFramebuffer(GL_FRAMEBUFFER, buffer->gl_framebuffer);
	glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, buffer->gl_texture, 0);
	if (atl_surface->renderbuffer)
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, atl_surface->renderbuffer_attachment, GL_RENDERBUFFER, atl_surface->renderbuffer);
	buffer->width = atl_surface->width;
	buffer->height = atl_surface->height;
}

/* runs on the main thread: GTK's texture of a buffer that was made again in another size */
static gboolean delete_gtk_texture(gpointer data)
{
	GdkGLTextureBuilder *builder = data;
	gdk_gl_context_make_current(gdk_gl_texture_builder_get_context(builder));
	GLuint texture_id = gdk_gl_texture_builder_get_id(builder);
	glDeleteTextures(1, &texture_id);
	gdk_gl_context_clear_current();
	g_object_unref(builder);
	return G_SOURCE_REMOVE;
}

/* AOSP: the buffers of a window surface have the window's current size (Surface::dequeueBuffer() takes the size
 * of the window, eglSwapBuffers()/eglQuerySurface() follow it); the SurfaceView widget or the SurfaceTexture's
 * default size may have changed since the surface was made, so the back buffer is made again in the window's size
 * (for another process: and handed over again). Only on this context's thread; a buffer comes back only when its
 * consumer shows it no more */
static void fit_back_buffer(ATLSurface *atl_surface, struct ANativeWindow *native_window)
{
	struct atl_surface_buffer *buffer = atl_surface->back_buffer;
	if (!buffer || (atl_surface->remote && atl_surface->remote->remote_gone))
		return;
	int width = ANativeWindow_getWidth(native_window);
	int height = ANativeWindow_getHeight(native_window);
	if (width <= 0 || height <= 0 || (width == buffer->width && height == buffer->height))
		return;
	GLint previous_texture, previous_renderbuffer;
	glGetIntegerv(GL_TEXTURE_BINDING_2D, &previous_texture);
	if (width != atl_surface->width || height != atl_surface->height) {
		atl_surface->width = width;
		atl_surface->height = height;
		if (atl_surface->renderbuffer) {
			glGetIntegerv(GL_RENDERBUFFER_BINDING, &previous_renderbuffer);
			glBindRenderbuffer(GL_RENDERBUFFER, atl_surface->renderbuffer);
			glRenderbufferStorage(GL_RENDERBUFFER, atl_surface->renderbuffer_format, width, height);
			glBindRenderbuffer(GL_RENDERBUFFER, previous_renderbuffer);
		}
	}
	if (buffer->egl_image)
		eglDestroyImage(eglGetCurrentDisplay(), buffer->egl_image);
	buffer->egl_image = NULL;
	glDeleteFramebuffers(1, &buffer->gl_framebuffer);
	glDeleteTextures(1, &buffer->gl_texture);
	if (buffer->texture_builder) {
		g_idle_add(delete_gtk_texture, buffer->texture_builder);
		buffer->texture_builder = NULL;
	}
	if (buffer->map)
		munmap(buffer->map, buffer->size);
	buffer->map = NULL;
	if (buffer->fd >= 0)
		close(buffer->fd);
	buffer->fd = -1;
	make_buffer(atl_surface, buffer);
	if (atl_surface->remote)
		remote_buffer_share(atl_surface, buffer - atl_surface->buffers);
	glBindTexture(GL_TEXTURE_2D, previous_texture);
}

EGLBoolean bionic_eglMakeCurrent(EGLDisplay display, EGLSurface draw, EGLSurface read, EGLContext context)
{
	if (getenv("ATL_DIRECT_EGL"))
		return eglMakeCurrent(display, draw, read, context);
	struct ANativeWindow *native_window = surface_native_window(draw);
	if (!native_window) {
		return eglMakeCurrent(display, draw, read, context);
	}
	if (!draw_surface_hashtable)
		draw_surface_hashtable = g_hash_table_new(NULL, NULL);
	g_hash_table_insert(draw_surface_hashtable, context, draw);
	if (!read_surface_hashtable)
		read_surface_hashtable = g_hash_table_new(NULL, NULL);
	g_hash_table_insert(read_surface_hashtable, context, read);
	EGLBoolean ret = eglMakeCurrent(display, NULL, NULL, context);
	ATLSurface *atl_surface = draw;
	if (!atl_surface->unused_buffers) {
		atl_surface->vsync = g_async_queue_new();
		g_async_queue_push(atl_surface->vsync, GINT_TO_POINTER(1));
		atl_surface->unused_buffers = g_async_queue_new();
		GLint previousTexture;
		glGetIntegerv(GL_TEXTURE_BINDING_2D, &previousTexture); // Save current binding
		if (atl_surface->renderbuffer_format) {
			GLint previous_renderbuffer;
			glGetIntegerv(GL_RENDERBUFFER_BINDING, &previous_renderbuffer);
			glGenRenderbuffers(1, &atl_surface->renderbuffer);
			glBindRenderbuffer(GL_RENDERBUFFER, atl_surface->renderbuffer);
			glRenderbufferStorage(GL_RENDERBUFFER, atl_surface->renderbuffer_format, atl_surface->width, atl_surface->height);
			glBindRenderbuffer(GL_RENDERBUFFER, previous_renderbuffer);
		}

		for (int i = 0; i < NUM_BUFFERS; i++) {
			atl_surface->buffers[i].surface = atl_surface;
			make_buffer(atl_surface, &atl_surface->buffers[i]);
			if (atl_surface->remote)
				remote_buffer_share(atl_surface, i);
			g_async_queue_push(atl_surface->unused_buffers, &atl_surface->buffers[i]);
		}
		glBindTexture(GL_TEXTURE_2D, previousTexture);
		if (atl_surface->remote)
			ANativeWindow_remoteSetHandler(atl_surface->remote, remote_message, atl_surface);
	}
	/* the buffers come back when GTK lets go of their textures, on the main thread; a thread drawing for an app
	 * whose main thread waits for it (GeckoView pausing its compositor) must not wait for that forever: like in
	 * eglSwapBuffers(), without a free buffer it draws into nothing until the next swap */
	if (!atl_surface->back_buffer && atl_surface->remote)
		atl_surface->back_buffer = remote_pop(atl_surface, atl_surface->unused_buffers, 100);
	else if (!atl_surface->back_buffer)
		atl_surface->back_buffer = g_async_queue_timeout_pop(atl_surface->unused_buffers, 100000); // 100ms
	fit_back_buffer(atl_surface, native_window);
	glBindFramebuffer(GL_FRAMEBUFFER, atl_surface->back_buffer ? atl_surface->back_buffer->gl_framebuffer : 0);
	return ret;
}

/* runs on main thread */
static void destroy_texture(void *data)
{
	struct atl_surface_buffer *buffer = data;
	g_async_queue_push(buffer->surface->unused_buffers, buffer);
	g_object_unref(buffer->surface);
}

static void frame_callback(SurfaceViewWidget *surface_view_widget)
{
	g_async_queue_push(surface_view_widget->frame_callback_data, GINT_TO_POINTER(1));
}

static gboolean queue_texture(gpointer data)
{
	static PFNGLEGLIMAGETARGETTEXTURE2DOESPROC glEGLImageTargetTexture2DOES = NULL;
	ATLSurface *atl_surface = data;
	if (atl_surface->destroyed) {
		g_object_unref(atl_surface);
		return G_SOURCE_REMOVE;
	}
	struct atl_surface_buffer *buffer = atl_surface->front_buffer;
	if (!buffer->texture_builder) {
		GLuint texture_id;
		GdkGLContext *gtk_gl_context = gdk_surface_create_gl_context(gtk_native_get_surface(gtk_widget_get_native(GTK_WIDGET(atl_surface->surface_view_widget))), NULL);
		gdk_gl_context_make_current(gtk_gl_context);
		glGenTextures(1, &texture_id);
		glBindTexture(GL_TEXTURE_2D, texture_id);
		if (!glEGLImageTargetTexture2DOES)
			glEGLImageTargetTexture2DOES = (PFNGLEGLIMAGETARGETTEXTURE2DOESPROC)eglGetProcAddress("glEGLImageTargetTexture2DOES");
		glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, buffer->egl_image);
		glBindTexture(GL_TEXTURE_2D, 0);
		eglDestroyImage(eglGetCurrentDisplay(), buffer->egl_image);
		buffer->egl_image = NULL;
		buffer->texture_builder = gdk_gl_texture_builder_new();
		gdk_gl_texture_builder_set_context(buffer->texture_builder, gtk_gl_context);
		gdk_gl_texture_builder_set_id(buffer->texture_builder, texture_id);
		gdk_gl_texture_builder_set_format(buffer->texture_builder, GDK_MEMORY_R8G8B8A8_PREMULTIPLIED);
		gdk_gl_texture_builder_set_width(buffer->texture_builder, buffer->width);
		gdk_gl_texture_builder_set_height(buffer->texture_builder, buffer->height);
		gdk_gl_context_clear_current();
	}
	atl_surface->surface_view_widget->frame_callback_data = atl_surface->vsync;
	atl_surface->surface_view_widget->frame_callback = frame_callback;
	surface_view_widget_set_texture(atl_surface->surface_view_widget, gdk_gl_texture_builder_build(buffer->texture_builder, destroy_texture, buffer), TRUE);
	// atl_surface reference will be dropped in destroy_texture
	return G_SOURCE_REMOVE;
}

EGLBoolean bionic_eglSwapBuffers(EGLDisplay display, EGLSurface surface)
{
	/* EGL: on a pbuffer eglSwapBuffers has no effect. The window surface standing in for it would wait
	 * forever for a frame callback of its never shown wl_surface (OsmAnd's GLThread drew one frame) */
	if (wayland_pbuffers && g_hash_table_contains(wayland_pbuffers, surface))
		return EGL_TRUE;
	if (getenv("ATL_DIRECT_EGL"))
		return eglSwapBuffers(display, surface);
	struct ANativeWindow *native_window = surface_native_window(surface);
	if (!native_window) {
		return eglSwapBuffers(display, surface);
	}
	ATLSurface *atl_surface = surface;

	if (atl_surface->remote)
		return remote_swap_buffers(atl_surface);

	glFlush();
	if (atl_surface->back_buffer) {
		g_async_queue_timeout_pop(atl_surface->vsync, 50000); // 50ms
		atl_surface->front_buffer = atl_surface->back_buffer;
		atl_surface->back_buffer = NULL;
		g_idle_add_full(G_PRIORITY_HIGH_IDLE + 20, queue_texture, g_object_ref(atl_surface), NULL);
	}
	atl_surface->back_buffer = g_async_queue_timeout_pop(atl_surface->unused_buffers, 100000); // 100ms
	fit_back_buffer(atl_surface, native_window);
	if (atl_surface->back_buffer)
		glBindFramebuffer(GL_FRAMEBUFFER, atl_surface->back_buffer->gl_framebuffer);
	else
		glBindFramebuffer(GL_FRAMEBUFFER, 0);

	return EGL_TRUE;
}

EGLBoolean bionic_eglQuerySurface(EGLDisplay display, EGLSurface surface, EGLint attribute, EGLint *value)
{
	if (getenv("ATL_DIRECT_EGL"))
		return eglQuerySurface(display, surface, attribute, value);
	struct ANativeWindow *native_window = surface_native_window(surface);
	if (!native_window)
		return eglQuerySurface(display, surface, attribute, value);
	if (attribute == EGL_WIDTH) {
		*value = ((ATLSurface *)surface)->width; // the size of its buffers, as on the window of another process
	} else if (attribute == EGL_HEIGHT) {
		*value = ((ATLSurface *)surface)->height;
	} else if (attribute == 0x313D /* EGL_BUFFER_AGE_EXT */) {
		/* the back buffer's contents are not kept for the app: it redraws everything */
		*value = 0;
	} else {
		printf("bionic_eglQuerySurface(%p, %p, %d, %p): attribute not implemented\n", display, surface, attribute, value);
		return eglQuerySurface(display, surface, attribute, value);
	}
	return EGL_TRUE;
}

EGLSurface bionic_eglGetCurrentSurface(EGLint readdraw)
{
	if (getenv("ATL_DIRECT_EGL"))
		return eglGetCurrentSurface(readdraw);
	EGLContext current_context = eglGetCurrentContext();
	if (readdraw == EGL_READ)
		return g_hash_table_lookup(read_surface_hashtable, current_context);
	else if (readdraw == EGL_DRAW)
		return g_hash_table_lookup(draw_surface_hashtable, current_context);

	return NULL;
}

void bionic_glBindFramebuffer(GLenum target, GLuint framebuffer)
{
	if (getenv("ATL_DIRECT_EGL") || framebuffer != 0)
		return glBindFramebuffer(target, framebuffer);
	ATLSurface *atl_surface = g_hash_table_lookup(draw_surface_hashtable, eglGetCurrentContext());
	return glBindFramebuffer(target, atl_surface && atl_surface->back_buffer ? atl_surface->back_buffer->gl_framebuffer : 0);
}
