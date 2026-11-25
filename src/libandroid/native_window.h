
#include <EGL/egl.h>
#include <GL/gl.h>
#include <X11/Xlib.h>
#include <gtk/gtk.h>
#include <jni.h>

struct atl_surface_msg;

struct ANativeWindow {
	EGLNativeWindowType egl_window;
	GtkWidget *surface_view_widget;
	struct wl_display *wayland_display;
	struct wl_surface *wayland_surface;
	Display *x11_display;
	gulong resize_handler;
	int refcount;
	jobject surface; /* the android.view.Surface of this window (a global reference) */
	jobject image_reader; /* the android.media.ImageReader whose Surface this is (a global reference), else NULL */
	uint64_t image_reader_usage; /* its ImageReader.getUsage() (EGL can't ask: the JNI of EGL14 holds arrays critical) */
	struct { /* ANativeWindow_setBuffersGeometry() and the buffer of ANativeWindow_lock() */
		int width;
		int height;
		int format;
		void *bits;
		size_t size;
		gboolean locked;
		int locked_width;
		int locked_height;
		int locked_format;
	} cpu;
	int width;
	int height;
	int32_t dataspace; // ANativeWindow_setBuffersDataSpace(), ADATASPACE_UNKNOWN (0) until set
	/* a Surface of another process of the app (Surface.Endpoint): the socket to its widget, else -1 */
	int remote_fd;
	gboolean remote_gone;
	GMutex remote_lock;
	void (*remote_handler)(void *data, const struct atl_surface_msg *msg);
	void *remote_data;
	struct { /* ANativeWindow_remotePresentTexture() */
		int fd;
		void *map;
		size_t size;
		int width;
		int height;
		gboolean busy;
		GdkTexture *held; /* ANativeWindow_remotePresentDmabuf(): the frame the other process shows from this buffer */
	} remote_memory[3];
};

gboolean ANativeWindow_remoteRead(struct ANativeWindow *native_window, int timeout_ms);
void ANativeWindow_remoteSend(struct ANativeWindow *native_window, const struct atl_surface_msg *msg, int fd);
void ANativeWindow_remoteSendFds(struct ANativeWindow *native_window, const struct atl_surface_msg *msg, const int *fds, int n_fds);
gboolean ANativeWindow_remotePresentTexture(struct ANativeWindow *native_window, GdkTexture *texture, gboolean crop);
gboolean ANativeWindow_remotePresentDmabuf(struct ANativeWindow *native_window, GdkTexture *texture, gboolean crop);
void ANativeWindow_localPresentTexture(struct ANativeWindow *native_window, GdkTexture *texture);
void ANativeWindow_remoteSetHandler(struct ANativeWindow *native_window, void (*handler)(void *data, const struct atl_surface_msg *msg), void *data);

struct ANativeWindow *ANativeWindow_fromSurface(JNIEnv *env, jobject surface);
struct ANativeWindow *ANativeWindow_fromConsumerSurface(JNIEnv *env, jobject surface);
void ANativeWindow_setImageReader(struct ANativeWindow *native_window, JNIEnv *env, jobject image_reader);

#ifdef VULKAN_CORE_H_
/* vulkan_swapchain.c: Vulkan on a window with remote_fd or without a Wayland/X11 surface of its own */
VkResult atl_vk_create_remote_surface(VkInstance instance, struct ANativeWindow *window, VkSurfaceKHR *out_surface);
gboolean atl_vk_destroy_remote_surface(VkSurfaceKHR surface);
gboolean atl_vk_remote_surface_capabilities(VkSurfaceKHR surface, VkSurfaceCapabilitiesKHR *capabilities);
PFN_vkVoidFunction atl_vk_remote_proc(VkInstance instance, const char *name);
#endif
EGLSurface bionic_eglCreateWindowSurface(EGLDisplay display, EGLConfig config, struct ANativeWindow *native_window, EGLint const *attrib_list);
EGLBoolean bionic_eglDestroySurface(EGLDisplay display, EGLSurface surface);
EGLDisplay bionic_eglGetDisplay(NativeDisplayType native_display);
EGLBoolean bionic_eglMakeCurrent(EGLDisplay display, EGLSurface draw, EGLSurface read, EGLContext context);
EGLBoolean bionic_eglSwapBuffers(EGLDisplay display, EGLSurface surface);
EGLBoolean bionic_eglQuerySurface(EGLDisplay display, EGLSurface surface, EGLint attribute, EGLint *value);
EGLBoolean bionic_eglPresentationTimeANDROID(EGLDisplay dpy, EGLSurface surface, int64_t time);
EGLBoolean bionic_eglChooseConfig(EGLDisplay display, EGLint *attrib_list, EGLConfig *configs, EGLint config_size, EGLint *num_config);
EGLBoolean bionic_eglGetConfigAttrib(EGLDisplay display, EGLConfig config, EGLint attribute, EGLint *value);
EGLSurface bionic_eglCreatePbufferSurface(EGLDisplay display, EGLConfig config, EGLint const *attrib_list);
void bionic_glBindFramebuffer(GLenum target, GLuint framebuffer);
void ANativeWindow_acquire(struct ANativeWindow *native_window);
void ANativeWindow_release(struct ANativeWindow *native_window);
int32_t ANativeWindow_getWidth(struct ANativeWindow *native_window);
int32_t ANativeWindow_getHeight(struct ANativeWindow *native_window);
