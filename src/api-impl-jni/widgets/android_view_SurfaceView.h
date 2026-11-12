#include <gtk/gtk.h>
#include <stdint.h>

G_DECLARE_FINAL_TYPE(SurfaceViewWidget, surface_view_widget, SURFACE_VIEW, WIDGET, GtkWidget)

struct _SurfaceViewWidget {
	GtkWidget parent_instance;
	GdkTexture *texture;
	gboolean needs_flip;
	gboolean crop; /* the scaling mode SCALE_CROP: the texture covers the widget, keeping its aspect ratio */
	void (*frame_callback)(SurfaceViewWidget *surface_view_widget);
	gpointer frame_callback_data;
};

struct _SurfaceViewWidgetClass {
	GtkWidgetClass parent_class;
};

void surface_view_widget_set_texture(SurfaceViewWidget *surface_view_widget, GdkTexture *texture, gboolean needs_flip);

/* The buffers of an EGL window on a Surface that came from another process of the app (Surface.Endpoint): that
 * process draws them (libandroid/egl.c, producer), this one shows them in the SurfaceView's widget
 * (android_view_SurfaceView_remote.c, consumer). They talk over a SOCK_SEQPACKET socket, one message each. */
enum atl_surface_msg_type {
	ATL_SURFACE_MSG_BUFFER, /* producer: buffer `index` is the file descriptor sent along (SCM_RIGHTS) */
	ATL_SURFACE_MSG_PRESENT, /* producer: show buffer `index` */
	ATL_SURFACE_MSG_DISCONNECT, /* producer: the window is gone, the last buffer stays shown */
	ATL_SURFACE_MSG_RELEASE, /* consumer: buffer `index` may be drawn into again */
	ATL_SURFACE_MSG_FRAME, /* consumer: the widget drew a frame (the producer's vsync) */
	ATL_SURFACE_MSG_SIZE, /* consumer: the widget's size */
};

#define ATL_SURFACE_MAX_BUFFERS 4

struct atl_surface_msg {
	uint32_t type;
	uint32_t index;
	int32_t width;
	int32_t height;
	int32_t stride;
	uint32_t offset;
	uint32_t fourcc; /* the DRM format of a dmabuf, 0 for memory (memfd) with GDK_MEMORY_R8G8B8A8_PREMULTIPLIED rows */
	uint64_t modifier;
};
