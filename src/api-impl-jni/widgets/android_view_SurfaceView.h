#include <gtk/gtk.h>
#include <stdint.h>

/* how a buffer of an ASurfaceControl tree is shown (ATL_SURFACE_MSG_LAYER) */
struct atl_surface_layer {
	float src[4]; /* the part of the buffer shown: x, y, width, height in buffer pixels */
	float dst[4]; /* where it is shown (after the transform) in the window */
	float clip[4]; /* the part of the window it may cover */
	uint32_t transform; /* ANativeWindowTransform of the buffer: flips, then a rotation by 90 degrees */
	float alpha;
};
/* ANativeWindowTransform */
#define ATL_SURFACE_TRANSFORM_FLIP_H 0x01
#define ATL_SURFACE_TRANSFORM_FLIP_V 0x02
#define ATL_SURFACE_TRANSFORM_ROT_90 0x04

G_DECLARE_FINAL_TYPE(SurfaceViewWidget, surface_view_widget, SURFACE_VIEW, WIDGET, GtkWidget)

struct _SurfaceViewWidget {
	GtkWidget parent_instance;
	GdkTexture *texture;
	gboolean needs_flip;
	gboolean crop; /* the scaling mode SCALE_CROP: the texture covers the widget, keeping its aspect ratio */
	void (*frame_callback)(SurfaceViewWidget *surface_view_widget);
	gpointer frame_callback_data;
	/* for the app's render threads (ANativeWindow, MediaCodec, camera), which must not ask GTK: the allocated size
	 * and whether the widget is realized (in a window), kept with g_atomic_int_* on the main thread */
	gint width;
	gint height;
	gint realized;
	/* the layers of an ASurfaceControl tree (struct surface_view_layer) in the window size frame_width x frame_height,
	 * instead of texture */
	GArray *layers;
	int frame_width;
	int frame_height;
};

struct surface_view_layer {
	GdkTexture *texture;
	struct atl_surface_layer geometry;
};

struct _SurfaceViewWidgetClass {
	GtkWidgetClass parent_class;
};

void surface_view_widget_set_texture(SurfaceViewWidget *surface_view_widget, GdkTexture *texture, gboolean needs_flip);
/* takes over layers (an array of struct surface_view_layer whose clear function unrefs the texture, NULL for none) */
void surface_view_widget_set_layers(SurfaceViewWidget *surface_view_widget, GArray *layers, int frame_width, int frame_height);
/* the SurfaceViewWidget of a SurfaceView's widget (Surface.widget), in any thread */
SurfaceViewWidget *surface_view_widget_of(GtkWidget *view_widget);

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
	/* the surfaces of an ASurfaceControl tree (libandroid/surface_control.c): a frame is the LAYER messages since the
	 * last COMMIT, drawn in their order */
	ATL_SURFACE_MSG_LAYER, /* producer: buffer `index` is shown with the geometry of `layer`, `serial` comes back in its RELEASE */
	ATL_SURFACE_MSG_COMMIT, /* producer: show the layers sent since the last COMMIT; width and height are the window's size */
};

#define ATL_SURFACE_MAX_BUFFERS 4
#define ATL_SURFACE_MAX_SLOTS 16 /* buffer indexes the consumer takes (ASurfaceControl shows several buffers at once) */

struct atl_surface_msg {
	uint32_t type;
	uint32_t index;
	int32_t width;
	int32_t height;
	int32_t stride;
	uint32_t offset;
	uint32_t fourcc; /* the DRM format of a dmabuf, 0 for memory (memfd) with GDK_MEMORY_R8G8B8A8_PREMULTIPLIED rows */
	uint32_t top_first; /* the rows start at the top (frames of MediaCodec), else at the bottom (GL) */
	uint32_t crop; /* PRESENT: the scaling mode SCALE_TO_FIT_WITH_CROPPING of MediaCodec (the frame covers the widget) */
	uint64_t modifier;
	/* a YUV dmabuf frame of a hardware decoder (MediaCodec): its second plane, whose file descriptor is sent along
	 * as the second one, and the YUV matrix and range (struct atl_dmabuf); n_planes 0 for the other buffers */
	uint32_t n_planes;
	int32_t stride1;
	uint32_t offset1;
	uint32_t matrix;
	uint32_t full_range;
	/* LAYER: the RELEASE of buffer `index` carries the serial of the last LAYER that showed it */
	uint32_t serial;
	struct atl_surface_layer layer;
};

/* the planes of a frame of a hardware decoder (MediaCodec), on its GdkTexture as "atl-dmabuf" (g_object_set_data):
 * SurfaceTexture.updateTexImage() samples them through an EGLImage, as Android's external texture samples the
 * decoder's buffer, and they go to a Surface of another process as they are */
#define ATL_DMABUF_MAX_PLANES 2
struct atl_dmabuf {
	uint32_t fourcc; /* DRM_FORMAT_NV12 or DRM_FORMAT_P010 */
	uint64_t modifier;
	int32_t width;
	int32_t height;
	uint32_t n_planes;
	int fd[ATL_DMABUF_MAX_PLANES];
	uint32_t offset[ATL_DMABUF_MAX_PLANES];
	uint32_t stride[ATL_DMABUF_MAX_PLANES];
	uint32_t matrix; /* the YUV matrix: 601, 709 or 2020 */
	uint32_t full_range;
};
