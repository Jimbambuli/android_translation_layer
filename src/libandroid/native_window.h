
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
	int width;
	int height;
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
void ANativeWindow_remoteSetHandler(struct ANativeWindow *native_window, void (*handler)(void *data, const struct atl_surface_msg *msg), void *data);

struct ANativeWindow *ANativeWindow_fromSurface(JNIEnv *env, jobject surface);
EGLSurface bionic_eglCreateWindowSurface(EGLDisplay display, EGLConfig config, struct ANativeWindow *native_window, EGLint const *attrib_list);
EGLBoolean bionic_eglDestroySurface(EGLDisplay display, EGLSurface surface);
EGLDisplay bionic_eglGetDisplay(NativeDisplayType native_display);
EGLBoolean bionic_eglMakeCurrent(EGLDisplay display, EGLSurface draw, EGLSurface read, EGLContext context);
EGLBoolean bionic_eglSwapBuffers(EGLDisplay display, EGLSurface surface);
EGLBoolean bionic_eglChooseConfig(EGLDisplay display, EGLint *attrib_list, EGLConfig *configs, EGLint config_size, EGLint *num_config);
EGLSurface bionic_eglCreatePbufferSurface(EGLDisplay display, EGLConfig config, EGLint const *attrib_list);
void bionic_glBindFramebuffer(GLenum target, GLuint framebuffer);
void ANativeWindow_acquire(struct ANativeWindow *native_window);
void ANativeWindow_release(struct ANativeWindow *native_window);
