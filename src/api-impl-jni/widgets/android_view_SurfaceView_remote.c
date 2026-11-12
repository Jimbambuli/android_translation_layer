/*
 * The consumer side of a Surface drawn in another process of the app (Surface.Endpoint, AOSP: the BufferQueue
 * consumer of SurfaceFlinger): that process sends the buffers of its EGL window over a socket, they are shown
 * in the widget of the SurfaceView the Surface belongs to. See android_view_SurfaceView.h for the messages.
 */

#include <errno.h>
#include <stdio.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <unistd.h>

#include <glib-unix.h>
#include <gtk/gtk.h>

#include "../defines.h"

#include "../generated_headers/android_view_SurfaceView.h"
#include "android_view_SurfaceView.h"

struct remote_buffer {
	int fd;
	struct atl_surface_msg desc;
	void *map; /* memory buffers */
	size_t size;
};

struct remote_consumer {
	gatomicrefcount ref; /* the socket and each texture in GTK's hands */
	SurfaceViewWidget *widget;
	int fd; /* -1 once the producer is gone */
	gulong resize_handler;
	GdkTexture *shown;
	struct remote_buffer buffers[ATL_SURFACE_MAX_BUFFERS];
};

struct texture_data {
	struct remote_consumer *consumer;
	uint32_t index;
};

static void consumer_unref(struct remote_consumer *consumer)
{
	if (!g_atomic_ref_count_dec(&consumer->ref))
		return;
	for (int i = 0; i < ATL_SURFACE_MAX_BUFFERS; i++) {
		if (consumer->buffers[i].map)
			munmap(consumer->buffers[i].map, consumer->buffers[i].size);
		if (consumer->buffers[i].fd >= 0)
			close(consumer->buffers[i].fd);
	}
	g_object_unref(consumer->widget);
	g_free(consumer);
}

static void consumer_send(struct remote_consumer *consumer, enum atl_surface_msg_type type, uint32_t index, int width, int height)
{
	struct atl_surface_msg msg = {.type = type, .index = index, .width = width, .height = height};
	if (consumer->fd >= 0)
		send(consumer->fd, &msg, sizeof(msg), MSG_NOSIGNAL | MSG_DONTWAIT);
}

/* GTK let go of the texture of a buffer: the producer may draw into it again */
static void texture_released(void *data)
{
	struct texture_data *d = data;
	consumer_send(d->consumer, ATL_SURFACE_MSG_RELEASE, d->index, 0, 0);
	consumer_unref(d->consumer);
	g_free(d);
}

static void frame_callback(SurfaceViewWidget *widget)
{
	struct remote_consumer *consumer = widget->frame_callback_data;
	consumer_send(consumer, ATL_SURFACE_MSG_FRAME, 0, 0, 0);
}

static void on_resize(GtkWidget *widget, int width, int height, struct remote_consumer *consumer)
{
	consumer_send(consumer, ATL_SURFACE_MSG_SIZE, 0, width, height);
}

static void present(struct remote_consumer *consumer, uint32_t index)
{
	struct remote_buffer *buffer = &consumer->buffers[index];
	struct texture_data *d = g_new(struct texture_data, 1);
	GdkTexture *texture = NULL;
	GError *error = NULL;

	d->consumer = consumer;
	d->index = index;
	g_atomic_ref_count_inc(&consumer->ref);
	if (buffer->desc.fourcc) {
		GdkDmabufTextureBuilder *builder = gdk_dmabuf_texture_builder_new();
		gdk_dmabuf_texture_builder_set_display(builder, gtk_widget_get_display(GTK_WIDGET(consumer->widget)));
		gdk_dmabuf_texture_builder_set_width(builder, buffer->desc.width);
		gdk_dmabuf_texture_builder_set_height(builder, buffer->desc.height);
		gdk_dmabuf_texture_builder_set_fourcc(builder, buffer->desc.fourcc);
		gdk_dmabuf_texture_builder_set_modifier(builder, buffer->desc.modifier);
		gdk_dmabuf_texture_builder_set_premultiplied(builder, TRUE);
		gdk_dmabuf_texture_builder_set_n_planes(builder, 1);
		gdk_dmabuf_texture_builder_set_fd(builder, 0, buffer->fd);
		gdk_dmabuf_texture_builder_set_stride(builder, 0, buffer->desc.stride);
		gdk_dmabuf_texture_builder_set_offset(builder, 0, buffer->desc.offset);
		texture = gdk_dmabuf_texture_builder_build(builder, texture_released, d, &error);
		g_object_unref(builder);
		if (!texture) {
			fprintf(stderr, "SurfaceView: a dmabuf of another process can't be shown: %s\n", error->message);
			g_error_free(error);
			texture_released(d);
			return;
		}
	} else {
		GBytes *bytes = g_bytes_new_with_free_func(buffer->map, buffer->size, texture_released, d);
		texture = gdk_memory_texture_new(buffer->desc.width, buffer->desc.height, GDK_MEMORY_R8G8B8A8_PREMULTIPLIED, bytes, buffer->desc.stride);
		g_bytes_unref(bytes);
	}
	consumer->shown = texture;
	consumer->widget->frame_callback_data = consumer;
	consumer->widget->frame_callback = frame_callback;
	surface_view_widget_set_texture(consumer->widget, texture, TRUE); // GL rows, bottom first
}

static void add_buffer(struct remote_consumer *consumer, const struct atl_surface_msg *msg, int fd)
{
	struct remote_buffer *buffer = &consumer->buffers[msg->index];
	/* a buffer replaced by the producer stays mapped by the textures still made from it */
	if (buffer->map)
		munmap(buffer->map, buffer->size);
	if (buffer->fd >= 0)
		close(buffer->fd);
	buffer->fd = fd;
	buffer->desc = *msg;
	buffer->map = NULL;
	buffer->size = 0;
	if (!msg->fourcc) {
		buffer->size = (size_t)msg->stride * msg->height;
		buffer->map = mmap(NULL, buffer->size, PROT_READ, MAP_SHARED, fd, 0);
		if (buffer->map == MAP_FAILED) {
			fprintf(stderr, "SurfaceView: can't map a buffer of another process: %m\n");
			buffer->map = NULL;
		}
	}
}

/* the producer is gone: an EGL window destroyed leaves its last frame, a process that ended does not */
static void disconnect(struct remote_consumer *consumer, gboolean keep_frame)
{
	SurfaceViewWidget *widget = consumer->widget;
	g_clear_signal_handler(&consumer->resize_handler, widget);
	if (widget->frame_callback_data == consumer) {
		widget->frame_callback = NULL;
		widget->frame_callback_data = NULL;
	}
	if (!keep_frame && consumer->shown && widget->texture == consumer->shown)
		surface_view_widget_set_texture(widget, NULL, FALSE);
	consumer->shown = NULL;
	close(consumer->fd);
	consumer->fd = -1;
	consumer_unref(consumer);
}

static gboolean on_message(int fd, GIOCondition condition, struct remote_consumer *consumer)
{
	struct atl_surface_msg msg;
	char control[CMSG_SPACE(sizeof(int))];
	struct iovec iov = {.iov_base = &msg, .iov_len = sizeof(msg)};
	struct msghdr hdr = {.msg_iov = &iov, .msg_iovlen = 1, .msg_control = control, .msg_controllen = sizeof(control)};
	int buffer_fd = -1;

	ssize_t len = recvmsg(fd, &hdr, MSG_CMSG_CLOEXEC | MSG_DONTWAIT);
	if (len < 0 && (errno == EAGAIN || errno == EINTR))
		return G_SOURCE_CONTINUE;
	for (struct cmsghdr *cmsg = CMSG_FIRSTHDR(&hdr); cmsg; cmsg = CMSG_NXTHDR(&hdr, cmsg))
		if (cmsg->cmsg_level == SOL_SOCKET && cmsg->cmsg_type == SCM_RIGHTS)
			memcpy(&buffer_fd, CMSG_DATA(cmsg), sizeof(int));
	if (len != sizeof(msg) || msg.index >= ATL_SURFACE_MAX_BUFFERS || msg.type == ATL_SURFACE_MSG_DISCONNECT) {
		if (buffer_fd >= 0)
			close(buffer_fd);
		if (len > 0 && len != sizeof(msg))
			fprintf(stderr, "SurfaceView: broken message from the process drawing the surface (%zd bytes)\n", len);
		/* EOF without DISCONNECT: the process drawing the surface ended */
		disconnect(consumer, len == sizeof(msg) && msg.type == ATL_SURFACE_MSG_DISCONNECT);
		return G_SOURCE_REMOVE;
	}
	if (msg.type == ATL_SURFACE_MSG_BUFFER && buffer_fd >= 0)
		add_buffer(consumer, &msg, buffer_fd);
	else if (buffer_fd >= 0)
		close(buffer_fd);
	if (msg.type == ATL_SURFACE_MSG_PRESENT) {
		if (consumer->buffers[msg.index].fd >= 0 && (consumer->buffers[msg.index].map || consumer->buffers[msg.index].desc.fourcc))
			present(consumer, msg.index);
		else
			consumer_send(consumer, ATL_SURFACE_MSG_RELEASE, msg.index, 0, 0);
	}
	return G_SOURCE_CONTINUE;
}

static gboolean start(struct remote_consumer *consumer)
{
	consumer->resize_handler = g_signal_connect(consumer->widget, "resize", G_CALLBACK(on_resize), consumer);
	consumer_send(consumer, ATL_SURFACE_MSG_SIZE, 0, gtk_widget_get_width(GTK_WIDGET(consumer->widget)), gtk_widget_get_height(GTK_WIDGET(consumer->widget)));
	g_unix_fd_add(consumer->fd, G_IO_IN | G_IO_HUP | G_IO_ERR, (GUnixFDSourceFunc)on_message, consumer);
	return G_SOURCE_REMOVE;
}

/* Surface.Endpoint CONNECT, on a binder thread: the socket for the producer, the consumer starts on the main thread */
JNIEXPORT jint JNICALL Java_android_view_SurfaceView_native_1connectRemote(JNIEnv *env, jclass class, jlong surface_view)
{
	int fds[2];
	if (socketpair(AF_UNIX, SOCK_SEQPACKET | SOCK_CLOEXEC, 0, fds) < 0)
		return -1;
	struct remote_consumer *consumer = g_new0(struct remote_consumer, 1);
	g_atomic_ref_count_init(&consumer->ref);
	consumer->widget = SURFACE_VIEW_WIDGET(g_object_ref(gtk_widget_get_first_child(GTK_WIDGET(_PTR(surface_view)))));
	consumer->fd = fds[0];
	for (int i = 0; i < ATL_SURFACE_MAX_BUFFERS; i++)
		consumer->buffers[i].fd = -1;
	/* the producer may be waiting for its size inside a call the main thread waits for: read here, not there */
	consumer_send(consumer, ATL_SURFACE_MSG_SIZE, 0, gtk_widget_get_width(GTK_WIDGET(consumer->widget)), gtk_widget_get_height(GTK_WIDGET(consumer->widget)));
	g_main_context_invoke(NULL, G_SOURCE_FUNC(start), consumer);
	return fds[1];
}
