/*
 * The camera "HAL" of ATL: the cameras of PipeWire (nodes of media.class Video/Source with media.role Camera,
 * as xdg-desktop-portal defines a camera), what ExternalCameraDevice of AOSP's external camera HAL
 * (hardware/interfaces/camera/device/default) reads from V4L2. Inside a Flatpak the PipeWire remote comes from
 * the camera portal, outside of one ATL connects to the user's PipeWire directly.
 *
 * A stream decodes each frame (raw YUV, or MJPEG through libavcodec), hands it as a GdkTexture to the Surfaces
 * of the current request (the consumers of struct surface_target) and calls ExternalCameraDevice.onFrame(),
 * during which nativeCopyFrame() can read the frame as NV21 or YV12.
 */

#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

#include <gtk/gtk.h>
#include <libavcodec/avcodec.h>
#include <libavutil/imgutils.h>
#include <libportal/portal.h>
#include <libswscale/swscale.h>
#include <pipewire/pipewire.h>
#include <spa/param/format-utils.h>
#include <spa/param/video/format-utils.h>
#include <spa/pod/builder.h>

#include "../defines.h"
#include "../util.h"
#include "../graphics/surface_target.h"
#include "../media/video_rotate.h"
#include "../generated_headers/android_hardware_camera2_impl_ExternalCameraDevice.h"

/* android.graphics.ImageFormat */
#define IMAGE_FORMAT_NV21 0x11
#define IMAGE_FORMAT_YV12 0x32315659
#define IMAGE_FORMAT_YUV_420_888 0x23

struct camera_mode {
	uint32_t subtype; /* SPA_MEDIA_SUBTYPE_raw or SPA_MEDIA_SUBTYPE_mjpg */
	uint32_t format;  /* the SPA_VIDEO_FORMAT of a raw mode */
	int32_t width;
	int32_t height;
	int32_t fps_num;
	int32_t fps_den;
};

struct camera_node {
	uint32_t id;
	char *serial;
	char *name;
	GArray *modes; /* struct camera_mode */
	struct pw_proxy *proxy;
	struct spa_hook listener;
	gboolean removed;
};

struct camera_stream {
	struct camera_node *node;
	jobject java;  /* global ref to the ExternalCameraDevice */
	struct pw_stream *stream;
	struct spa_hook listener;
	uint32_t subtype;
	enum AVPixelFormat pix_fmt; /* of a raw stream */
	int32_t width;
	int32_t height;
	AVCodecContext *decoder;    /* of an MJPEG stream */
	AVFrame *decoded;
	AVPacket *packet;
	struct SwsContext *sws_rgb;
	struct SwsContext *sws_cpu;
	struct SwsContext *sws_jpeg;
	struct surface_target *targets;
	int n_targets;
	AVFrame *current; /* the frame during onFrame() */
	gboolean failed;
	int rotation;      /* the display transform of the targets: horizontal flip first, then turned clockwise */
	gboolean flip_h;
};

static struct pw_thread_loop *loop;
static struct pw_context *context;
static struct pw_core *core;
static struct pw_registry *registry;
static struct spa_hook core_listener;
static struct spa_hook registry_listener;
static GPtrArray *nodes; /* struct camera_node, in the order PipeWire announced them; never shrinks */
static int pending_seq;
static gboolean synced;

/* ATL_CAMERA_DEBUG: the stream's steps on stderr */
static gboolean camera_debug(void)
{
	static int debug = -1;
	if (debug < 0)
		debug = getenv("ATL_CAMERA_DEBUG") != NULL;
	return debug;
}

static enum AVPixelFormat raw_pix_fmt(uint32_t format)
{
	switch (format) {
		case SPA_VIDEO_FORMAT_YUY2: return AV_PIX_FMT_YUYV422;
		case SPA_VIDEO_FORMAT_UYVY: return AV_PIX_FMT_UYVY422;
		case SPA_VIDEO_FORMAT_NV12: return AV_PIX_FMT_NV12;
		case SPA_VIDEO_FORMAT_NV21: return AV_PIX_FMT_NV21;
		case SPA_VIDEO_FORMAT_I420: return AV_PIX_FMT_YUV420P;
		case SPA_VIDEO_FORMAT_RGB: return AV_PIX_FMT_RGB24;
		case SPA_VIDEO_FORMAT_BGR: return AV_PIX_FMT_BGR24;
		case SPA_VIDEO_FORMAT_RGBx: return AV_PIX_FMT_RGB0;
		case SPA_VIDEO_FORMAT_BGRx: return AV_PIX_FMT_BGR0;
		default: return AV_PIX_FMT_NONE;
	}
}

/* --- enumeration --- */

static void add_mode(struct camera_node *node, uint32_t subtype, uint32_t format, const struct spa_rectangle *size, const struct spa_fraction *rate)
{
	if (size->width == 0 || size->height == 0)
		return;
	struct camera_mode mode = {subtype, format, size->width, size->height, rate ? rate->num : 0, rate ? rate->denom : 1};
	for (guint i = 0; i < node->modes->len; i++) {
		struct camera_mode *m = &g_array_index(node->modes, struct camera_mode, i);
		if (!memcmp(m, &mode, sizeof(mode)))
			return;
	}
	g_array_append_val(node->modes, mode);
}

/* the values of a property: a fixed value, the alternatives of an enum (the default is listed again among
 * them), or the default of a range */
static const void *prop_values(const struct spa_pod *pod, uint32_t type, uint32_t *n)
{
	uint32_t choice;
	const struct spa_pod *val = spa_pod_get_values(pod, n, &choice);
	if (val->type != type || *n == 0)
		return NULL;
	if (choice == SPA_CHOICE_Enum && *n > 1) { /* skip the default */
		*n -= 1;
		return (const uint8_t *)SPA_POD_BODY_CONST(val) + val->size;
	}
	if (choice == SPA_CHOICE_Range || choice == SPA_CHOICE_Step) /* default, min, max: the default */
		*n = 1;
	return SPA_POD_BODY_CONST(val);
}

static void on_node_param(void *data, int seq, uint32_t id, uint32_t index, uint32_t next, const struct spa_pod *param)
{
	struct camera_node *node = data;
	uint32_t media_type, media_subtype;
	if (id != SPA_PARAM_EnumFormat || spa_format_parse(param, &media_type, &media_subtype) < 0 || media_type != SPA_MEDIA_TYPE_video)
		return;
	if (media_subtype != SPA_MEDIA_SUBTYPE_raw && media_subtype != SPA_MEDIA_SUBTYPE_mjpg)
		return;

	const struct spa_pod_prop *prop;
	uint32_t n_formats = 1, n_sizes = 0, n_rates = 0;
	const uint32_t *formats = (uint32_t[]){0};
	const struct spa_rectangle *sizes = NULL;
	const struct spa_fraction *rates = NULL;
	if (media_subtype == SPA_MEDIA_SUBTYPE_raw) {
		if (!(prop = spa_pod_find_prop(param, NULL, SPA_FORMAT_VIDEO_format)) || !(formats = prop_values(&prop->value, SPA_TYPE_Id, &n_formats)))
			return;
	}
	if (!(prop = spa_pod_find_prop(param, NULL, SPA_FORMAT_VIDEO_size)) || !(sizes = prop_values(&prop->value, SPA_TYPE_Rectangle, &n_sizes)))
		return;
	if ((prop = spa_pod_find_prop(param, NULL, SPA_FORMAT_VIDEO_framerate)))
		rates = prop_values(&prop->value, SPA_TYPE_Fraction, &n_rates);

	for (uint32_t f = 0; f < n_formats; f++) {
		if (media_subtype == SPA_MEDIA_SUBTYPE_raw && raw_pix_fmt(formats[f]) == AV_PIX_FMT_NONE)
			continue;
		for (uint32_t s = 0; s < n_sizes; s++) {
			if (!rates || n_rates == 0)
				add_mode(node, media_subtype, formats[f], &sizes[s], NULL);
			for (uint32_t r = 0; rates && r < n_rates; r++)
				if (rates[r].num && rates[r].denom)
					add_mode(node, media_subtype, formats[f], &sizes[s], &rates[r]);
		}
	}
}

static const struct pw_node_events node_events = {
	PW_VERSION_NODE_EVENTS,
	.param = on_node_param,
};

static void on_registry_global(void *data, uint32_t id, uint32_t permissions, const char *type, uint32_t version, const struct spa_dict *props)
{
	if (strcmp(type, PW_TYPE_INTERFACE_Node) || !props)
		return;
	const char *media_class = spa_dict_lookup(props, PW_KEY_MEDIA_CLASS);
	const char *media_role = spa_dict_lookup(props, PW_KEY_MEDIA_ROLE);
	if (!media_class || strcmp(media_class, "Video/Source") || !media_role || strcmp(media_role, "Camera"))
		return;
	struct camera_node *node = g_new0(struct camera_node, 1);
	const char *serial = spa_dict_lookup(props, PW_KEY_OBJECT_SERIAL);
	const char *name = spa_dict_lookup(props, PW_KEY_NODE_DESCRIPTION);
	node->id = id;
	node->serial = g_strdup(serial ? serial : "");
	node->name = g_strdup(name ? name : spa_dict_lookup(props, PW_KEY_NODE_NAME));
	node->modes = g_array_new(FALSE, FALSE, sizeof(struct camera_mode));
	node->proxy = pw_registry_bind(registry, id, PW_TYPE_INTERFACE_Node, PW_VERSION_NODE, 0);
	pw_proxy_add_object_listener(node->proxy, &node->listener, &node_events, node);
	pw_node_enum_params((struct pw_node *)node->proxy, 0, SPA_PARAM_EnumFormat, 0, -1, NULL);
	g_ptr_array_add(nodes, node);
}

static void on_registry_global_remove(void *data, uint32_t id)
{
	for (guint i = 0; i < nodes->len; i++) {
		struct camera_node *node = g_ptr_array_index(nodes, i);
		if (node->id == id && !node->removed) {
			node->removed = TRUE;
			spa_hook_remove(&node->listener);
			pw_proxy_destroy(node->proxy);
			node->proxy = NULL;
		}
	}
}

static const struct pw_registry_events registry_events = {
	PW_VERSION_REGISTRY_EVENTS,
	.global = on_registry_global,
	.global_remove = on_registry_global_remove,
};

static void on_core_done(void *data, uint32_t id, int seq)
{
	if (id == PW_ID_CORE && seq == pending_seq) {
		synced = TRUE;
		pw_thread_loop_signal(loop, false);
	}
}

static void on_core_error(void *data, uint32_t id, int seq, int res, const char *message)
{
	fprintf(stderr, "camera: PipeWire error %d on %u: %s\n", res, id, message);
	if (id == PW_ID_CORE) { /* the connection is gone: stop waiting */
		synced = TRUE;
		pw_thread_loop_signal(loop, false);
	}
}

static const struct pw_core_events core_events = {
	PW_VERSION_CORE_EVENTS,
	.done = on_core_done,
	.error = on_core_error,
};

/* with the loop locked */
static void roundtrip(void)
{
	synced = FALSE;
	pending_seq = pw_core_sync(core, PW_ID_CORE, pending_seq);
	while (!synced)
		pw_thread_loop_wait(loop);
}

static void camera_access_done(GObject *source, GAsyncResult *result, gpointer data)
{
	int *state = data;
	g_autoptr(GError) error = NULL;
	if (xdp_portal_access_camera_finish(XDP_PORTAL(source), result, &error))
		*state = 1;
	else {
		fprintf(stderr, "camera: the camera portal gives no access: %s\n", error ? error->message : "");
		*state = 2;
	}
}

/* the PipeWire remote of the camera portal (org.freedesktop.portal.Camera). OpenPipeWireRemote succeeds without a
 * dialog once the user has allowed the app (the portal's permission store says yes) and fails otherwise; only
 * with request_access, ATL asks with AccessCamera first, which shows the desktop's dialog if the user has not
 * answered yet. -1 without a remote */
static int portal_camera_remote(gboolean request_access, gboolean *denied)
{
	GMainContext *ctx = g_main_context_new();
	g_main_context_push_thread_default(ctx);
	XdpPortal *portal = xdp_portal_new();
	int fd = -1;
	if (xdp_portal_is_camera_present(portal)) {
		fd = xdp_portal_open_pipewire_remote_for_camera(portal);
		if (fd < 0 && request_access) {
			int state = 0;
			xdp_portal_access_camera(portal, NULL, XDP_CAMERA_FLAG_NONE, NULL, camera_access_done, &state);
			while (!state)
				g_main_context_iteration(ctx, TRUE);
			if (state == 1)
				fd = xdp_portal_open_pipewire_remote_for_camera(portal);
			else
				*denied = TRUE;
		}
	}
	g_object_unref(portal);
	g_main_context_pop_thread_default(ctx);
	g_main_context_unref(ctx);
	return fd;
}

/* the portal's IsCameraPresent property: whether there is a camera, without asking the user */
JNIEXPORT jboolean JNICALL Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeIsCameraPresent(JNIEnv *env, jclass klass)
{
	GMainContext *ctx = g_main_context_new();
	g_main_context_push_thread_default(ctx);
	XdpPortal *portal = xdp_portal_new();
	gboolean present = xdp_portal_is_camera_present(portal);
	g_object_unref(portal);
	g_main_context_pop_thread_default(ctx);
	g_main_context_unref(ctx);
	return present;
}

/* connects to PipeWire (outside a Flatpak directly; inside one through the camera portal, asking the user only with
 * request_access) and reads the cameras; once connected, later calls only report what is known (cameras PipeWire
 * announced since are appended). The number of cameras, or -1 without a connection, -2 if the user refused */
JNIEXPORT jint JNICALL Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeInit(JNIEnv *env, jclass klass, jboolean request_access)
{
	static gsize initialized = 0;
	static GMutex connect_lock;
	if (g_once_init_enter(&initialized)) {
		pw_init(NULL, NULL);
		nodes = g_ptr_array_new();
		loop = pw_thread_loop_new("atl-camera", NULL);
		context = pw_context_new(pw_thread_loop_get_loop(loop), NULL, 0);
		pw_thread_loop_start(loop);
		g_once_init_leave(&initialized, 1);
	}
	gboolean denied = FALSE;
	g_mutex_lock(&connect_lock);
	if (!core) {
		/* the portal is asked without the loop's lock: the dialog may take long */
		int fd = -1;
		gboolean flatpak = g_file_test("/.flatpak-info", G_FILE_TEST_EXISTS);
		if (flatpak)
			fd = portal_camera_remote(request_access, &denied);
		pw_thread_loop_lock(loop);
		if (!flatpak)
			core = pw_context_connect(context, NULL, 0);
		else if (fd >= 0)
			core = pw_context_connect_fd(context, fd, NULL, 0);
		if (core) {
			pw_core_add_listener(core, &core_listener, &core_events, NULL);
			registry = pw_core_get_registry(core, PW_VERSION_REGISTRY, 0);
			pw_registry_add_listener(registry, &registry_listener, &registry_events, NULL);
			roundtrip(); /* the globals, and with them the EnumFormat requests */
			roundtrip(); /* the formats */
			/* through the portal the session manager grants the camera nodes after the connection (WirePlumber's
			 * access-portal): the portal reported a camera, so wait for it, for up to two seconds */
			for (int i = 0; flatpak && nodes->len == 0 && i < 20; i++) {
				pw_thread_loop_unlock(loop);
				g_usleep(100000);
				pw_thread_loop_lock(loop);
				roundtrip();
			}
			if (flatpak && nodes->len)
				roundtrip(); /* the formats of the cameras that appeared */
			if (camera_debug())
				fprintf(stderr, "camera: connected, %u cameras\n", nodes->len);
		} else if (!flatpak || request_access) {
			fprintf(stderr, "camera: no PipeWire connection\n");
		}
		pw_thread_loop_unlock(loop);
	}
	g_mutex_unlock(&connect_lock);
	if (!core)
		return denied ? -2 : -1;
	pw_thread_loop_lock(loop);
	jint count = nodes->len;
	pw_thread_loop_unlock(loop);
	return count;
}

static struct camera_node *get_node(jint index)
{
	return index >= 0 && index < nodes->len ? g_ptr_array_index(nodes, index) : NULL;
}

JNIEXPORT jstring JNICALL Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeGetName(JNIEnv *env, jclass klass, jint index)
{
	pw_thread_loop_lock(loop);
	struct camera_node *node = get_node(index);
	jstring name = node && node->name ? _JSTRING(node->name) : NULL;
	pw_thread_loop_unlock(loop);
	return name;
}

/* the sizes and frame rates of all formats ATL can read, as {width, height, fps numerator, fps denominator};
 * none for a camera that is gone */
JNIEXPORT jintArray JNICALL Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeGetModes(JNIEnv *env, jclass klass, jint index)
{
	pw_thread_loop_lock(loop);
	struct camera_node *node = get_node(index);
	int n = node && !node->removed ? node->modes->len : 0;
	jintArray array = (*env)->NewIntArray(env, n * 4);
	for (int i = 0; i < n; i++) {
		struct camera_mode *m = &g_array_index(node->modes, struct camera_mode, i);
		jint v[4] = {m->width, m->height, m->fps_num, m->fps_den};
		(*env)->SetIntArrayRegion(env, array, i * 4, 4, v);
	}
	pw_thread_loop_unlock(loop);
	return array;
}

/* --- streaming --- */

static void stream_error(struct camera_stream *s, const char *message)
{
	if (s->failed)
		return;
	s->failed = TRUE;
	JNIEnv *env;
	(*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);
	jstring msg = _JSTRING(message ? message : "");
	(*env)->CallVoidMethod(env, s->java, _METHOD(_CLASS(s->java), "onStreamError", "(Ljava/lang/String;)V"), msg);
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionDescribe(env);
	(*env)->DeleteLocalRef(env, msg);
}

static void on_stream_state_changed(void *data, enum pw_stream_state old, enum pw_stream_state state, const char *error)
{
	struct camera_stream *s = data;
	if (camera_debug())
		fprintf(stderr, "camera: stream %s -> %s %s\n", pw_stream_state_as_string(old), pw_stream_state_as_string(state), error ? error : "");
	if (state == PW_STREAM_STATE_ERROR || (state == PW_STREAM_STATE_UNCONNECTED && old >= PW_STREAM_STATE_PAUSED))
		stream_error(s, error);
}

static void on_stream_param_changed(void *data, uint32_t id, const struct spa_pod *param)
{
	struct camera_stream *s = data;
	uint32_t media_type, media_subtype;
	if (id != SPA_PARAM_Format || !param || spa_format_parse(param, &media_type, &media_subtype) < 0)
		return;
	s->subtype = media_subtype;
	if (camera_debug())
		fprintf(stderr, "camera: format negotiated, subtype %u\n", media_subtype);
	if (media_subtype == SPA_MEDIA_SUBTYPE_raw) {
		struct spa_video_info_raw info;
		if (spa_format_video_raw_parse(param, &info) < 0)
			return;
		s->pix_fmt = raw_pix_fmt(info.format);
		s->width = info.size.width;
		s->height = info.size.height;
	} else if (media_subtype == SPA_MEDIA_SUBTYPE_mjpg) {
		struct spa_video_info_mjpg info;
		if (spa_format_video_mjpg_parse(param, &info) < 0)
			return;
		s->width = info.size.width;
		s->height = info.size.height;
		if (!s->decoder) {
			s->decoder = avcodec_alloc_context3(avcodec_find_decoder(AV_CODEC_ID_MJPEG));
			avcodec_open2(s->decoder, s->decoder->codec, NULL);
			s->decoded = av_frame_alloc();
			s->packet = av_packet_alloc();
		}
	}
}

struct deliver_data {
	struct surface_target target;
	GdkTexture *texture;
	gint64 timestamp_ns;
};

static gboolean deliver(void *data)
{
	struct deliver_data *d = data;
	static unsigned delivered;
	if (camera_debug() && delivered++ % 60 == 0)
		fprintf(stderr, "camera: delivered %u: widget %p texture-target %p remote %p\n", delivered, (void *)d->target.surface_view_widget, (void *)d->target.surface_texture, (void *)d->target.remote);
	surface_target_deliver(&d->target, d->texture, d->timestamp_ns, FALSE);
	g_free(d);
	return G_SOURCE_REMOVE;
}

/* the buffer as a frame: raw planes in place, or the decoded MJPEG picture */
static AVFrame *buffer_frame(struct camera_stream *s, struct spa_buffer *buf, AVFrame *raw)
{
	struct spa_data *d = &buf->datas[0];
	if (!d->data || !d->chunk || d->chunk->size == 0)
		return NULL;
	uint8_t *data = (uint8_t *)d->data + d->chunk->offset;
	if (s->subtype == SPA_MEDIA_SUBTYPE_mjpg) {
		if (!s->decoder || av_new_packet(s->packet, d->chunk->size) < 0)
			return NULL;
		memcpy(s->packet->data, data, d->chunk->size);
		int ret = avcodec_send_packet(s->decoder, s->packet);
		av_packet_unref(s->packet);
		if (ret < 0 || avcodec_receive_frame(s->decoder, s->decoded) < 0)
			return NULL;
		return s->decoded;
	}
	if (s->pix_fmt == AV_PIX_FMT_NONE)
		return NULL;
	raw->format = s->pix_fmt;
	raw->width = s->width;
	raw->height = s->height;
	int linesizes[4];
	if (av_image_fill_linesizes(linesizes, s->pix_fmt, s->width) < 0)
		return NULL;
	if (d->chunk->stride > 0) { /* the planes of a planar format scale with the first one's stride */
		int scale = d->chunk->stride / MAX(linesizes[0], 1);
		for (int i = 0; i < 4; i++)
			linesizes[i] = i == 0 ? d->chunk->stride : linesizes[i] * scale;
	}
	if (buf->n_datas > 1) { /* one plane per data */
		for (uint32_t i = 0; i < buf->n_datas && i < 4; i++) {
			struct spa_data *p = &buf->datas[i];
			if (!p->data || !p->chunk)
				return NULL;
			raw->data[i] = (uint8_t *)p->data + p->chunk->offset;
			raw->linesize[i] = p->chunk->stride > 0 ? p->chunk->stride : linesizes[i];
		}
		return raw;
	}
	if (av_image_fill_pointers(raw->data, s->pix_fmt, s->height, data, linesizes) < 0)
		return NULL;
	if (av_image_get_buffer_size(s->pix_fmt, s->width, s->height, 1) > (int)d->chunk->size && d->chunk->stride <= 0)
		return NULL;
	memcpy(raw->linesize, linesizes, sizeof(linesizes));
	return raw;
}

static void on_stream_process(void *data)
{
	struct camera_stream *s = data;
	struct pw_buffer *b = pw_stream_dequeue_buffer(s->stream);
	if (!b)
		return;
	struct timespec now;
	clock_gettime(CLOCK_MONOTONIC, &now); /* AOSP SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN: the time of arrival */
	gint64 timestamp_ns = now.tv_sec * 1000000000LL + now.tv_nsec;
	AVFrame raw = {0};
	AVFrame *frame = buffer_frame(s, b->buffer, &raw);
	static unsigned frames;
	if (camera_debug() && frames++ % 30 == 0)
		fprintf(stderr, "camera: buffer %u: %s %dx%d, %d targets, data %p size %u\n", frames, frame ? "frame" : "no frame",
		        frame ? frame->width : 0, frame ? frame->height : 0, s->n_targets, b->buffer->datas[0].data, b->buffer->datas[0].chunk ? b->buffer->datas[0].chunk->size : 0);
	if (frame) {
		JNIEnv *env;
		(*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);
		if (s->n_targets) {
			int stride = frame->width * 3;
			s->sws_rgb = sws_getCachedContext(s->sws_rgb, frame->width, frame->height, frame->format,
			                                  frame->width, frame->height, AV_PIX_FMT_RGB24, SWS_BILINEAR, NULL, NULL, NULL);
			guchar *rgb = g_malloc(frame->height * stride);
			sws_scale(s->sws_rgb, (const uint8_t *const *)frame->data, frame->linesize, 0, frame->height, (uint8_t *[1]){rgb}, (int[1]){stride});
			int width = frame->width, height = frame->height;
			if (s->flip_h) {
				for (int y = 0; y < height; y++) {
					guchar *row = rgb + y * stride;
					for (int l = 0, r = width - 1; l < r; l++, r--)
						for (int c = 0; c < 3; c++) {
							guchar t = row[l * 3 + c];
							row[l * 3 + c] = row[r * 3 + c];
							row[r * 3 + c] = t;
						}
				}
			}
			if (s->rotation) {
				guchar *turned = g_malloc(height * stride);
				rotate_plane(turned, rgb, stride, width, height, 3, s->rotation);
				g_free(rgb);
				rgb = turned;
				if (s->rotation != 180) {
					width = frame->height;
					height = frame->width;
					stride = width * 3;
				}
			}
			GBytes *bytes = g_bytes_new_take(rgb, height * stride);
			GdkTexture *texture = gdk_memory_texture_new(width, height, GDK_MEMORY_R8G8B8, bytes, stride);
			g_bytes_unref(bytes);
			for (int i = 0; i < s->n_targets; i++) {
				struct deliver_data *d = g_new(struct deliver_data, 1);
				surface_target_copy(env, &d->target, &s->targets[i]);
				d->texture = g_object_ref(texture);
				d->timestamp_ns = timestamp_ns;
				g_idle_add(deliver, d);
			}
			g_object_unref(texture);
		}
		s->current = frame;
		(*env)->CallVoidMethod(env, s->java, _METHOD(_CLASS(s->java), "onFrame", "(J)V"), (jlong)timestamp_ns);
		if ((*env)->ExceptionCheck(env))
			(*env)->ExceptionDescribe(env);
		s->current = NULL;
	}
	pw_stream_queue_buffer(s->stream, b);
}

static const struct pw_stream_events stream_events = {
	PW_VERSION_STREAM_EVENTS,
	.state_changed = on_stream_state_changed,
	.param_changed = on_stream_param_changed,
	.process = on_stream_process,
};

JNIEXPORT jlong JNICALL Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeOpen(JNIEnv *env, jobject this, jint index)
{
	pw_thread_loop_lock(loop);
	struct camera_node *node = get_node(index);
	struct camera_stream *s = NULL;
	if (node && !node->removed) {
		s = g_new0(struct camera_stream, 1);
		s->node = node;
		s->java = (*env)->NewGlobalRef(env, this);
		s->pix_fmt = AV_PIX_FMT_NONE;
	}
	pw_thread_loop_unlock(loop);
	return _INTPTR(s);
}

/* the mode to ask for: a raw format of that size and rate if there is one (nothing to decode), else MJPEG */
static struct camera_mode *pick_mode(struct camera_node *node, int width, int height, int fps_num, int fps_den)
{
	struct camera_mode *best = NULL;
	for (guint i = 0; i < node->modes->len; i++) {
		struct camera_mode *m = &g_array_index(node->modes, struct camera_mode, i);
		if (m->width != width || m->height != height || (gint64)m->fps_num * fps_den != (gint64)fps_num * m->fps_den)
			continue;
		if (!best || (best->subtype != SPA_MEDIA_SUBTYPE_raw && m->subtype == SPA_MEDIA_SUBTYPE_raw))
			best = m;
	}
	return best;
}

JNIEXPORT jboolean JNICALL Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeStart(JNIEnv *env, jobject this, jlong handle, jint width, jint height, jint fps_num, jint fps_den)
{
	struct camera_stream *s = _PTR(handle);
	pw_thread_loop_lock(loop);
	struct camera_mode *mode = s->node->removed ? NULL : pick_mode(s->node, width, height, fps_num, fps_den);
	if (!mode || s->stream) {
		pw_thread_loop_unlock(loop);
		return mode != NULL;
	}
	s->failed = FALSE;
	struct pw_properties *props = pw_properties_new(PW_KEY_MEDIA_TYPE, "Video", PW_KEY_MEDIA_CATEGORY, "Capture",
	                                                PW_KEY_MEDIA_ROLE, "Camera", PW_KEY_TARGET_OBJECT, s->node->serial, NULL);
	s->stream = pw_stream_new(core, "atl-camera", props);
	pw_stream_add_listener(s->stream, &s->listener, &stream_events, s);

	uint8_t buffer[1024];
	struct spa_pod_builder b = SPA_POD_BUILDER_INIT(buffer, sizeof(buffer));
	struct spa_pod_frame f;
	spa_pod_builder_push_object(&b, &f, SPA_TYPE_OBJECT_Format, SPA_PARAM_EnumFormat);
	spa_pod_builder_add(&b, SPA_FORMAT_mediaType, SPA_POD_Id(SPA_MEDIA_TYPE_video),
	                    SPA_FORMAT_mediaSubtype, SPA_POD_Id(mode->subtype), 0);
	if (mode->subtype == SPA_MEDIA_SUBTYPE_raw)
		spa_pod_builder_add(&b, SPA_FORMAT_VIDEO_format, SPA_POD_Id(mode->format), 0);
	spa_pod_builder_add(&b, SPA_FORMAT_VIDEO_size, SPA_POD_Rectangle(&SPA_RECTANGLE(mode->width, mode->height)), 0);
	if (mode->fps_num)
		spa_pod_builder_add(&b, SPA_FORMAT_VIDEO_framerate, SPA_POD_Fraction(&SPA_FRACTION(mode->fps_num, mode->fps_den)), 0);
	const struct spa_pod *params[1] = {spa_pod_builder_pop(&b, &f)};

	int ret = pw_stream_connect(s->stream, PW_DIRECTION_INPUT, PW_ID_ANY,
	                            PW_STREAM_FLAG_AUTOCONNECT | PW_STREAM_FLAG_MAP_BUFFERS, params, 1);
	if (camera_debug())
		fprintf(stderr, "camera: connect %s %dx%d %d/%d (subtype %u): %d\n", s->node->serial, mode->width, mode->height, mode->fps_num, mode->fps_den, mode->subtype, ret);
	pw_thread_loop_unlock(loop);
	return ret >= 0;
}

/* the consumers of the frames (with the loop locked, the stream does not read them meanwhile) */
JNIEXPORT void JNICALL Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeSetTargets(JNIEnv *env, jclass klass, jlong handle, jobjectArray surfaces)
{
	struct camera_stream *s = _PTR(handle);
	int n = surfaces ? (*env)->GetArrayLength(env, surfaces) : 0;
	struct surface_target *targets = g_new0(struct surface_target, MAX(n, 1));
	int count = 0;
	for (int i = 0; i < n; i++) {
		jobject surface = (*env)->GetObjectArrayElement(env, surfaces, i);
		surface_target_set(env, &targets[count], surface);
		if (surface_target_is_set(&targets[count]))
			count++;
		(*env)->DeleteLocalRef(env, surface);
	}
	if (camera_debug())
		fprintf(stderr, "camera: %d of %d surfaces are targets\n", count, n);
	pw_thread_loop_lock(loop);
	struct surface_target *old = s->targets;
	int n_old = s->n_targets;
	s->targets = targets;
	s->n_targets = count;
	pw_thread_loop_unlock(loop);
	for (int i = 0; i < n_old; i++)
		surface_target_clear(env, &old[i]);
	g_free(old);
}

JNIEXPORT void JNICALL Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeStop(JNIEnv *env, jclass klass, jlong handle)
{
	struct camera_stream *s = _PTR(handle);
	pw_thread_loop_lock(loop);
	if (s->stream) {
		spa_hook_remove(&s->listener);
		pw_stream_destroy(s->stream);
		s->stream = NULL;
	}
	if (s->decoder) {
		avcodec_free_context(&s->decoder);
		av_frame_free(&s->decoded);
		av_packet_free(&s->packet);
	}
	pw_thread_loop_unlock(loop);
}

JNIEXPORT void JNICALL Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeClose(JNIEnv *env, jclass klass, jlong handle)
{
	struct camera_stream *s = _PTR(handle);
	Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeSetTargets(env, klass, handle, NULL);
	Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeStop(env, klass, handle);
	sws_freeContext(s->sws_rgb);
	sws_freeContext(s->sws_cpu);
	sws_freeContext(s->sws_jpeg);
	g_free(s->targets);
	(*env)->DeleteGlobalRef(env, s->java);
	g_free(s);
}

/* the display transform of the stream's Surfaces (AOSP's preview transform of API 1: HAL_TRANSFORM_FLIP_H, then
 * turned clockwise by 0/90/180/270 degrees) */
JNIEXPORT void JNICALL Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeSetDisplayTransform(JNIEnv *env, jclass klass, jlong handle, jint rotation, jboolean flip_h)
{
	struct camera_stream *s = _PTR(handle);
	pw_thread_loop_lock(loop);
	s->rotation = rotation;
	s->flip_h = flip_h;
	pw_thread_loop_unlock(loop);
}

/* during onFrame(): the frame scaled to width x height in the layout of an android.hardware.Camera preview callback
 * (NV21, or YV12 with its 16-byte aligned strides) or of an ImageReader's YUV_420_888 image (I420, packed) */
JNIEXPORT jboolean JNICALL Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeCopyFrame(JNIEnv *env, jclass klass, jlong handle, jbyteArray dst, jint format, jint w, jint h)
{
	struct camera_stream *s = _PTR(handle);
	AVFrame *frame = s->current;
	if (!frame || !dst || w <= 0 || h <= 0)
		return JNI_FALSE;
	int y_stride = w, uv_stride = w;
	size_t size = (size_t)w * h * 3 / 2;
	enum AVPixelFormat dst_fmt = AV_PIX_FMT_NV21;
	if (format == IMAGE_FORMAT_YV12) {
		y_stride = (w + 15) & ~15;
		uv_stride = (y_stride / 2 + 15) & ~15;
		size = (size_t)y_stride * h + (size_t)uv_stride * (h / 2) * 2;
		dst_fmt = AV_PIX_FMT_YUV420P;
	} else if (format == IMAGE_FORMAT_YUV_420_888) {
		uv_stride = w / 2;
		size = (size_t)w * h + (size_t)uv_stride * (h / 2) * 2;
		dst_fmt = AV_PIX_FMT_YUV420P;
	} else if (format != IMAGE_FORMAT_NV21) {
		return JNI_FALSE;
	}
	if ((size_t)(*env)->GetArrayLength(env, dst) < size)
		return JNI_FALSE;
	s->sws_cpu = sws_getCachedContext(s->sws_cpu, frame->width, frame->height, frame->format, w, h, dst_fmt, SWS_BILINEAR, NULL, NULL, NULL);
	uint8_t *out = (*env)->GetPrimitiveArrayCritical(env, dst, NULL);
	uint8_t *planes[3];
	int strides[3];
	planes[0] = out;
	strides[0] = y_stride;
	if (dst_fmt == AV_PIX_FMT_NV21) {
		planes[1] = out + (size_t)w * h;
		strides[1] = w;
		planes[2] = NULL;
		strides[2] = 0;
	} else if (format == IMAGE_FORMAT_YV12) { /* Y, then Cr (V), then Cb (U) */
		planes[2] = out + (size_t)y_stride * h;
		planes[1] = planes[2] + (size_t)uv_stride * (h / 2);
		strides[1] = strides[2] = uv_stride;
	} else { /* I420: Y, then Cb (U), then Cr (V) */
		planes[1] = out + (size_t)y_stride * h;
		planes[2] = planes[1] + (size_t)uv_stride * (h / 2);
		strides[1] = strides[2] = uv_stride;
	}
	sws_scale(s->sws_cpu, (const uint8_t *const *)frame->data, frame->linesize, 0, frame->height, planes, strides);
	(*env)->ReleasePrimitiveArrayCritical(env, dst, out, 0);
	return JNI_TRUE;
}

/* during onFrame(): the frame scaled to width x height as a JPEG of that quality (gdk-pixbuf, as Bitmap.compress());
 * null if it cannot be encoded */
JNIEXPORT jbyteArray JNICALL Java_android_hardware_camera2_impl_ExternalCameraDevice_nativeEncodeJpeg(JNIEnv *env, jclass klass, jlong handle, jint w, jint h, jint quality)
{
	struct camera_stream *s = _PTR(handle);
	AVFrame *frame = s->current;
	if (!frame || w <= 0 || h <= 0)
		return NULL;
	int stride = w * 3;
	guchar *rgb = g_malloc((size_t)h * stride);
	s->sws_jpeg = sws_getCachedContext(s->sws_jpeg, frame->width, frame->height, frame->format, w, h, AV_PIX_FMT_RGB24, SWS_BICUBIC, NULL, NULL, NULL);
	sws_scale(s->sws_jpeg, (const uint8_t *const *)frame->data, frame->linesize, 0, frame->height, (uint8_t *[1]){rgb}, (int[1]){stride});
	GdkPixbuf *pixbuf = gdk_pixbuf_new_from_data(rgb, GDK_COLORSPACE_RGB, FALSE, 8, w, h, stride, NULL, NULL);
	gchar *buffer = NULL;
	gsize size = 0;
	char quality_str[8];
	snprintf(quality_str, sizeof(quality_str), "%d", CLAMP(quality, 1, 100));
	g_autoptr(GError) error = NULL;
	jbyteArray jpeg = NULL;
	if (gdk_pixbuf_save_to_buffer(pixbuf, &buffer, &size, "jpeg", &error, "quality", quality_str, NULL)) {
		jpeg = (*env)->NewByteArray(env, size);
		(*env)->SetByteArrayRegion(env, jpeg, 0, size, (jbyte *)buffer);
	} else {
		fprintf(stderr, "camera: JPEG encoding failed: %s\n", error ? error->message : "");
	}
	g_free(buffer);
	g_object_unref(pixbuf);
	g_free(rgb);
	return jpeg;
}
