/*
 * Android MediaCodec implementation for Android Translation Layer
 *
 * Large part of the video decoding code was taken from https://git.sr.ht/~emersion/vaapi-decoder/tree/wayland/
 */

#include <libavutil/frame.h>
#include <libavutil/hwcontext.h>
#include <libavutil/pixfmt.h>
#include <stdio.h>

#include <gdk/gdk.h>
#include <gtk/gtk.h>

#include <drm_fourcc.h>
#include <libavcodec/avcodec.h>
#include <libavutil/hwcontext_drm.h>
#include <libavutil/pixdesc.h>
#include <libswresample/swresample.h>
#if HAVE_LIBVA
#include <libavutil/hwcontext_vaapi.h>
#include <va/va.h>
#endif
#include <libswscale/swscale.h>

#include <stdlib.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_media_MediaCodec.h"
#include "../../libandroid/native_window.h"
#include "../widgets/android_view_SurfaceView.h"
#include "jni.h"
#include "video_rotate.h"

struct ATL_codec_context {
	AVCodecContext *codec;
	gboolean hardware; // a hardware codec of MediaCodecList: video frames are decoded with VA-API where it can
	union {
		struct {
			SwrContext *swr;
			int sample_rate;
		} audio;
		struct {
			struct SwsContext *sws; // for software decoding
			SurfaceViewWidget *surface_view_widget;
			jobject surface_texture; // global ref: the SurfaceTexture of a Surface(SurfaceTexture)
			int rotation;            // degrees clockwise (the format's rotation-degrees), the frames are turned
			gboolean crop;           // VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
			struct ANativeWindow *remote; // the window of a Surface of another process of the app
			size_t extradata_size;
			uint8_t *extradata;
		} video;
	};
};

/* decoder: FFmpeg's decoder of the codec; 0 if FFmpeg has none (MediaCodec throws then) */
JNIEXPORT jlong JNICALL Java_android_media_MediaCodec_native_1constructor(JNIEnv *env, jobject this, jstring decoder, jboolean hardware)
{
	const char *name = (*env)->GetStringUTFChars(env, decoder, NULL);
	const AVCodec *codec = avcodec_find_decoder_by_name(name);
	if (!codec)
		fprintf(stderr, "MediaCodec: FFmpeg has no decoder %s\n", name);
	(*env)->ReleaseStringUTFChars(env, decoder, name);
	if (!codec)
		return 0;
	AVCodecContext *codec_ctx = avcodec_alloc_context3(codec);

	struct ATL_codec_context *ctx = calloc(1, sizeof(struct ATL_codec_context));
	ctx->codec = codec_ctx;
	ctx->hardware = hardware;
	return _INTPTR(ctx);
}

/* whether FFmpeg's decoder can decode with a VA-API device */
static gboolean decoder_has_vaapi(const char *name)
{
	const AVCodec *codec = avcodec_find_decoder_by_name(name);
	const AVCodecHWConfig *config;
	for (int i = 0; codec && (config = avcodec_get_hw_config(codec, i)); i++) {
		if ((config->methods & AV_CODEC_HW_CONFIG_METHOD_HW_DEVICE_CTX) && config->device_type == AV_HWDEVICE_TYPE_VAAPI)
			return TRUE;
	}
	return FALSE;
}

/* MediaCodecList's hardware codecs (the index of the type in MediaCodecList.HARDWARE_TYPES), the VA-API profile
 * FFmpeg decodes a stream of the Android profile (MediaCodecInfo.CodecProfileLevel) with, and FFmpeg's decoder */
#if HAVE_LIBVA
static const struct {
	int type;
	const char *decoder;
	VAProfile va_profile;
	int android_profile;
} hardware_profiles[] = {
	{0, "h264", VAProfileH264ConstrainedBaseline, 0x10000}, // AVCProfileConstrainedBaseline
	{0, "h264", VAProfileH264ConstrainedBaseline, 0x01},    // AVCProfileBaseline: FFmpeg takes it for VA-API
	{0, "h264", VAProfileH264Main, 0x02},                   // AVCProfileMain
	{0, "h264", VAProfileH264High, 0x08},                   // AVCProfileHigh
	{0, "h264", VAProfileH264High, 0x80000},                // AVCProfileConstrainedHigh
	{1, "hevc", VAProfileHEVCMain, 0x01},                   // HEVCProfileMain
	{1, "hevc", VAProfileHEVCMain10, 0x02},                 // HEVCProfileMain10
	{2, "vp8", VAProfileVP8Version0_3, 0x01},               // VP8ProfileMain
	{3, "vp9", VAProfileVP9Profile0, 0x01},                 // VP9Profile0
	{3, "vp9", VAProfileVP9Profile1, 0x02},                 // VP9Profile1
	{3, "vp9", VAProfileVP9Profile2, 0x04},                 // VP9Profile2
	{3, "vp9", VAProfileVP9Profile3, 0x08},                 // VP9Profile3
	{4, "av1", VAProfileAV1Profile0, 0x01},                 // AV1ProfileMain8
	{4, "av1", VAProfileAV1Profile0, 0x02},                 // AV1ProfileMain10
};
#endif

/* the profiles the VA-API device decodes (VAEntrypointVLD), four values each: the type, the Android profile and the
 * largest frame (FFmpeg's frame constraints of that profile, the limit its VA-API decoding checks); empty without
 * a VA-API device */
JNIEXPORT jintArray JNICALL Java_android_media_MediaCodec_native_1hardwareProfiles(JNIEnv *env, jclass klass)
{
	int n = 0;
#if HAVE_LIBVA
	jint values[ARRAY_SIZE(hardware_profiles) * 4];
	AVBufferRef *device = NULL;
	if (av_hwdevice_ctx_create(&device, AV_HWDEVICE_TYPE_VAAPI, NULL, NULL, 0) < 0) {
		fprintf(stderr, "MediaCodec: no VA-API device, the video codecs decode in software\n");
		return (*env)->NewIntArray(env, 0);
	}
	VADisplay display = ((AVVAAPIDeviceContext *)((AVHWDeviceContext *)device->data)->hwctx)->display;
	int nb_profiles = vaMaxNumProfiles(display);
	VAProfile *profiles = calloc(nb_profiles, sizeof(VAProfile));
	if (vaQueryConfigProfiles(display, profiles, &nb_profiles) != VA_STATUS_SUCCESS)
		nb_profiles = 0;
	for (int i = 0; i < ARRAY_SIZE(hardware_profiles); i++) {
		VAProfile profile = hardware_profiles[i].va_profile;
		gboolean listed = FALSE;
		for (int j = 0; j < nb_profiles; j++)
			listed |= profiles[j] == profile;
		VAConfigID config;
		if (!listed || !decoder_has_vaapi(hardware_profiles[i].decoder) ||
		    vaCreateConfig(display, profile, VAEntrypointVLD, NULL, 0, &config) != VA_STATUS_SUCCESS)
			continue;
		AVVAAPIHWConfig *hwconfig = av_hwdevice_hwconfig_alloc(device);
		hwconfig->config_id = config;
		AVHWFramesConstraints *constraints = av_hwdevice_get_hwframe_constraints(device, hwconfig);
		if (constraints && constraints->max_width > 0 && constraints->max_height > 0) {
			values[n++] = hardware_profiles[i].type;
			values[n++] = hardware_profiles[i].android_profile;
			values[n++] = constraints->max_width;
			values[n++] = constraints->max_height;
		}
		av_hwframe_constraints_free(&constraints);
		av_free(hwconfig);
		vaDestroyConfig(display, config);
	}
	free(profiles);
	av_buffer_unref(&device);
	jintArray array = (*env)->NewIntArray(env, n);
	(*env)->SetIntArrayRegion(env, array, 0, n, values);
	return array;
#else
	return (*env)->NewIntArray(env, n);
#endif
}

JNIEXPORT void JNICALL Java_android_media_MediaCodec_native_1configure_1audio(JNIEnv *env, jobject this, jlong codec, jobject extradata, jint sample_rate, jint nb_channels)
{
	struct ATL_codec_context *ctx = _PTR(codec);
	AVCodecContext *codec_ctx = ctx->codec;
	jarray array_ref;
	jbyte *array;
	void *data;

	printf("Java_android_media_MediaCodec_native_1configure_1audio(%s, %d, %d)\n", codec_ctx->codec->name, sample_rate, nb_channels);

	ctx->audio.sample_rate = sample_rate;
	codec_ctx->sample_rate = sample_rate;
	if (nb_channels == 1)
		codec_ctx->ch_layout = (AVChannelLayout)AV_CHANNEL_LAYOUT_MONO;
	else if (nb_channels == 2)
		codec_ctx->ch_layout = (AVChannelLayout)AV_CHANNEL_LAYOUT_STEREO;
	else {
		printf("MediaCodec: Unsupported number of channels %d\n", nb_channels);
		exit(0);
	}

	if (extradata) {
		codec_ctx->extradata_size = get_nio_buffer_size(env, extradata);
		data = get_nio_buffer(env, extradata, &array_ref, &array);
		codec_ctx->extradata = av_mallocz(codec_ctx->extradata_size + AV_INPUT_BUFFER_PADDING_SIZE);
		memcpy(codec_ctx->extradata, data, codec_ctx->extradata_size);
		release_nio_buffer(env, array_ref, array);
	}

	for (int i = 0; i < codec_ctx->extradata_size; i++) {
		printf("params->extradata[%d] = %x\n", i, codec_ctx->extradata[i]);
	}
}

/*
 * Helper functions for hardware accelerated video decoding using Wayland DMA-Buf protocol
 */

static const struct {
	uint32_t format;
	int nb_layers;
	uint32_t layers[AV_DRM_MAX_PLANES];
} drm_format_map[] = {
	{DRM_FORMAT_NV12, 2, {DRM_FORMAT_R8, DRM_FORMAT_GR88}},
};

static uint32_t get_drm_frame_format(const AVDRMFrameDescriptor *drm_frame_desc)
{
	if (drm_frame_desc->nb_layers == 1) {
		return drm_frame_desc->layers[0].format;
	}
	for (size_t i = 0; i < ARRAY_SIZE(drm_format_map); i++) {
		if (drm_format_map[i].nb_layers != drm_frame_desc->nb_layers) {
			continue;
		}
		int match = 1;
		for (int j = 0; j < drm_frame_desc->nb_layers; j++) {
			match &= drm_frame_desc->layers[j].format == drm_format_map[i].layers[j];
		}
		if (match) {
			return drm_format_map[i].format;
		}
	}
	return DRM_FORMAT_INVALID;
}

static enum AVPixelFormat get_hw_format(AVCodecContext *ctx,
                                        const enum AVPixelFormat *pix_fmts)
{
	size_t i;
	for (i = 0; pix_fmts[i] != AV_PIX_FMT_NONE; i++) {
		if (pix_fmts[i] == AV_PIX_FMT_VAAPI || pix_fmts[i] == AV_PIX_FMT_DRM_PRIME) {
			return pix_fmts[i];
		}
	}

	fprintf(stderr, "Failed to find HW pixel format\n");
	if (i > 0) {
		printf("falling back to software decode\n");
		return pix_fmts[i - 1]; // last pixel format should be for software decoding
	}
	return AV_PIX_FMT_NONE;
}

struct render_frame_data {
	AVFrame *frame;
	GdkTexture *texture; // for software decoding
	SurfaceViewWidget *surface_view_widget;
	jobject surface_texture; // global ref, instead of the widget
	struct ANativeWindow *remote; // a reference, instead of the widget
	long release_time_ns;    // wall-clock (monotonic, ns) at which the frame should be shown; 0 = now
	long timestamp_ns;       // the frame's timestamp for SurfaceTexture.getTimestamp()
	gboolean crop;           // the scaling mode SCALE_TO_FIT_WITH_CROPPING, for the SurfaceView
};

/* the texture (taken over) to the consumer: the SurfaceView's widget, or the SurfaceTexture, which queues
 * it as AOSP's BufferQueue does (SurfaceTexture.getTimestamp() is the release time if one was given, else the
 * presentation time) */
static void deliver_texture(struct render_frame_data *d, GdkTexture *texture)
{
	if (d->remote) {
		ANativeWindow_remotePresentTexture(d->remote, texture, d->crop);
		g_object_unref(texture);
		ANativeWindow_release(d->remote);
	} else if (d->surface_texture) {
		JNIEnv *env = get_jni_env();
		jmethodID queue_buffer = _METHOD(_CLASS(d->surface_texture), "queueBuffer", "(JJ)V");
		(*env)->CallVoidMethod(env, d->surface_texture, queue_buffer, _INTPTR(texture), (jlong)d->timestamp_ns);
		(*env)->DeleteGlobalRef(env, d->surface_texture);
	} else {
		d->surface_view_widget->crop = d->crop;
		surface_view_widget_set_texture(d->surface_view_widget, texture, FALSE);
	}
}

/* the Surface's consumer: a SurfaceView's widget, the SurfaceTexture of a Surface(SurfaceTexture), or the window of
 * a Surface of another process (its frames go there as memory buffers); none for null or a released Surface */
static void set_output_surface(JNIEnv *env, struct ATL_codec_context *ctx, jobject surface)
{
	if (ctx->video.surface_texture)
		(*env)->DeleteGlobalRef(env, ctx->video.surface_texture);
	if (ctx->video.remote)
		ANativeWindow_release(ctx->video.remote);
	ctx->video.surface_texture = NULL;
	ctx->video.surface_view_widget = NULL;
	ctx->video.remote = NULL;
	if (!surface)
		return;
	if (!_GET_LONG_FIELD(surface, "widget") && _GET_OBJ_FIELD(surface, "atl_endpoint", "Landroid/os/IBinder;")) {
		ctx->video.remote = ANativeWindow_fromSurface(env, surface);
		return;
	}
	GtkWidget *wrapper = _PTR(_GET_LONG_FIELD(surface, "widget"));
	jobject surface_texture = _GET_OBJ_FIELD(surface, "surfaceTexture", "Landroid/graphics/SurfaceTexture;");
	if (wrapper)
		ctx->video.surface_view_widget = SURFACE_VIEW_WIDGET(gtk_widget_get_first_child(wrapper));
	else if (surface_texture)
		ctx->video.surface_texture = (*env)->NewGlobalRef(env, surface_texture);
}

// 5ms lead time (~0.5 vsync) to correct possible GTK + the Wayland compositor latency
#define RENDER_LEAD_US (5000)

// schedule a render so the frame reaches the screen by (or fractionally before) its release time
static guint schedule_render(GSourceFunc render_fn, struct render_frame_data *data, GdkTexture *texture)
{
	gint64 delay_us = 0;
	if (data->release_time_ns > 0)
		delay_us = (data->release_time_ns - RENDER_LEAD_US * 1000 - g_get_monotonic_time() * 1000) / 1000; // all in ns
	if (delay_us > 0)
		return g_timeout_add_full(G_PRIORITY_DEFAULT_IDLE, MAX(delay_us / 1000, 1), render_fn, data, NULL);
	return g_idle_add(render_fn, data);
}

static void handle_dmabuftexture_destroy(void *data)
{
	AVFrame *drm_frame = data;
	av_frame_free(&drm_frame);
}

static GdkTexture *import_drm_frame_desc_as_texture(const AVDRMFrameDescriptor *drm_frame_desc, int width, int height, AVFrame *drm_frame)
{
	// VA-API drivers may use separate layers with one plane each, or a single
	// layer with multiple planes. We need to handle both.
	uint32_t drm_format = get_drm_frame_format(drm_frame_desc);
	if (drm_format == DRM_FORMAT_INVALID) {
		fprintf(stderr, "Failed to get DRM frame format\n");
		return NULL;
	}
	GdkDmabufTextureBuilder *builder = gdk_dmabuf_texture_builder_new();
	gdk_dmabuf_texture_builder_set_display(builder, gdk_display_get_default());
	int k = 0;
	for (int i = 0; i < drm_frame_desc->nb_layers; i++) {
		const AVDRMLayerDescriptor *drm_layer = &drm_frame_desc->layers[i];

		for (int j = 0; j < drm_layer->nb_planes; j++) {
			const AVDRMPlaneDescriptor *drm_plane = &drm_layer->planes[j];
			const AVDRMObjectDescriptor *drm_object =
			    &drm_frame_desc->objects[drm_plane->object_index];

			gdk_dmabuf_texture_builder_set_modifier(builder, drm_object->format_modifier);
			gdk_dmabuf_texture_builder_set_offset(builder, k, drm_plane->offset);
			gdk_dmabuf_texture_builder_set_stride(builder, k, drm_plane->pitch);
			gdk_dmabuf_texture_builder_set_fd(builder, k, drm_object->fd);
			k++;
		}
	}
	gdk_dmabuf_texture_builder_set_n_planes(builder, k);
	gdk_dmabuf_texture_builder_set_width(builder, width);
	gdk_dmabuf_texture_builder_set_height(builder, height);
	gdk_dmabuf_texture_builder_set_fourcc(builder, drm_format);
	GError *error = NULL;
	GdkTexture *texture = gdk_dmabuf_texture_builder_build(builder, handle_dmabuftexture_destroy, drm_frame, &error);
	if (error) {
		fprintf(stderr, "Failed to build texture: %s\n", error->message);
		exit(1);
	}
	g_object_unref(builder);
	return texture;
}

JNIEXPORT void JNICALL Java_android_media_MediaCodec_native_1configure_1video(JNIEnv *env, jobject this, jlong codec, jobject csd0, jobject csd1, jobject surface_obj, jint rotation)
{
	struct ATL_codec_context *ctx = _PTR(codec);
	AVCodecContext *codec_ctx = ctx->codec;
	jarray array_ref;
	jbyte *array;
	int sps_size = 0;
	int pps_size = 0;

	printf("Java_android_media_MediaCodec_native_1configure_video(%s)\n", codec_ctx->codec->name);

	if (csd0)
		sps_size = get_nio_buffer_size(env, csd0);
	if (csd1)
		pps_size = get_nio_buffer_size(env, csd1);

	size_t extradata_size = sps_size + pps_size;
	uint8_t *extradata = av_mallocz(extradata_size + AV_INPUT_BUFFER_PADDING_SIZE);
	if (csd0) {
		memcpy(extradata, get_nio_buffer(env, csd0, &array_ref, &array), extradata_size);
		release_nio_buffer(env, array_ref, array);
	}
	if (csd1) {
		memcpy(extradata + sps_size, get_nio_buffer(env, csd1, &array_ref, &array), extradata_size);
		release_nio_buffer(env, array_ref, array);
	}

	for (int i = 0; i < extradata_size; i++) {
		printf("extradata[%d] = %x\n", i, extradata[i]);
	}

	/* For some reason, using AVCodecContext.extradata doesn't work with livestreams.
	   As a workaround, we inject the extradata into the first frame. */
	ctx->video.extradata = extradata;
	ctx->video.extradata_size = extradata_size;

	int i = 0;
	while (ctx->hardware) {
		const AVCodecHWConfig *config = avcodec_get_hw_config(codec_ctx->codec, i);
		if (!config) {
			fprintf(stderr, "Decoder %s doesn't support pixel format VAAPI or DRM_PRIME\n",
			        codec_ctx->codec->name);
			break;
		}

		if ((config->methods & AV_CODEC_HW_CONFIG_METHOD_HW_DEVICE_CTX) && (config->pix_fmt == AV_PIX_FMT_VAAPI || config->pix_fmt == AV_PIX_FMT_DRM_PRIME)) {
			fprintf(stderr, "Selected pixel format %s\n", av_get_pix_fmt_name(config->pix_fmt));
			codec_ctx->get_format = get_hw_format;

			AVBufferRef *hw_device_ctx = NULL;
			int ret = av_hwdevice_ctx_create(&hw_device_ctx, config->device_type, NULL, NULL, 0);
			if (ret >= 0) {
				codec_ctx->hw_device_ctx = hw_device_ctx;
				break;
			}
		}

		i++;
	}

	/* AOSP: the codec turns the frames by rotation-degrees (0, 90, 180, 270) when it renders to the Surface */
	ctx->video.rotation = (rotation % 360 + 360) % 360 / 90 * 90;
	set_output_surface(env, ctx, surface_obj);
}

JNIEXPORT void JNICALL Java_android_media_MediaCodec_native_1setVideoScalingMode(JNIEnv *env, jobject this, jlong codec, jboolean crop)
{
	((struct ATL_codec_context *)_PTR(codec))->video.crop = crop;
}

JNIEXPORT void JNICALL Java_android_media_MediaCodec_native_1setOutputSurface(JNIEnv *env, jobject this, jlong codec, jobject surface)
{
	set_output_surface(env, _PTR(codec), surface);
}

JNIEXPORT void JNICALL Java_android_media_MediaCodec_native_1start(JNIEnv *env, jobject this, jlong codec)
{
	struct ATL_codec_context *ctx = _PTR(codec);
	AVCodecContext *codec_ctx = ctx->codec;

	if (avcodec_open2(codec_ctx, codec_ctx->codec, NULL) < 0) {
		printf("Codec cannot be found");
	}
}

#define INFO_TRY_AGAIN_LATER -1

JNIEXPORT jint JNICALL Java_android_media_MediaCodec_native_1queueInputBuffer(JNIEnv *env, jobject this, jlong codec, jobject buffer, jlong presentationTimeUs)
{
	jarray array_ref = NULL;
	jbyte *array = NULL;
	int ret;
	struct ATL_codec_context *ctx = _PTR(codec);
	AVCodecContext *codec_ctx = ctx->codec;
	AVPacket *pkt = NULL;
	if (buffer) { // buffer can be null if we're sending EOF
		pkt = av_packet_alloc();
		pkt->size = get_nio_buffer_size(env, buffer);
		pkt->data = get_nio_buffer(env, buffer, &array_ref, &array);
		if (codec_ctx->codec_type == AVMEDIA_TYPE_VIDEO && ctx->video.extradata_size) {
			uint8_t *data = pkt->data;
			pkt->data = av_malloc(pkt->size + ctx->video.extradata_size + AV_INPUT_BUFFER_PADDING_SIZE);
			memcpy(pkt->data, ctx->video.extradata, ctx->video.extradata_size);
			memcpy(pkt->data + ctx->video.extradata_size, data, pkt->size);
			pkt->size += ctx->video.extradata_size;
			ctx->video.extradata_size = 0;
		}
		pkt->pts = presentationTimeUs;
	}
	ret = avcodec_send_packet(codec_ctx, pkt);
	if (ret < 0 && ret != AVERROR(EAGAIN)) {
		fprintf(stderr, "Error while sending packet: %d = %s\n", ret, av_err2str(ret));
	}
	if (buffer) {
		release_nio_buffer(env, array_ref, array);
		av_packet_free(&pkt);
	}
	return ret;
}

JNIEXPORT jint JNICALL Java_android_media_MediaCodec_native_1dequeueOutputBuffer(JNIEnv *env, jobject this, jlong codec, jobject buffer, jobject buffer_info)
{
	struct ATL_codec_context *ctx = _PTR(codec);
	AVCodecContext *codec_ctx = ctx->codec;
	AVFrame *frame = av_frame_alloc();
	int ret;
	jarray array_ref;
	jbyte *array;

	ret = avcodec_receive_frame(codec_ctx, frame);
	if (ret < 0) {
		if (ret == AVERROR_EOF) {
			_SET_INT_FIELD(buffer_info, "flags", android_media_MediaCodec_BUFFER_FLAG_END_OF_STREAM);
			_SET_INT_FIELD(buffer_info, "offset", 0);
			_SET_INT_FIELD(buffer_info, "size", 0);
			_SET_LONG_FIELD(buffer_info, "presentationTimeUs", 0);
			av_frame_free(&frame);
			// set the buffer to NULL, so we don't try to render it
			uint8_t *raw_buffer = get_nio_buffer(env, buffer, &array_ref, &array);
			*((AVFrame **)raw_buffer) = NULL;
			release_nio_buffer(env, array_ref, array);
			return 0;
		}
		if (ret != AVERROR(EAGAIN)) {
			printf("avcodec_receive_frame returned %d\n", ret);
			printf("frame->data = %p frame->nb_samples = %d\n", frame->data[0], frame->nb_samples);
		}
		av_frame_free(&frame);
		return INFO_TRY_AGAIN_LATER;
	}
	_SET_INT_FIELD(buffer_info, "flags", 0);
	_SET_LONG_FIELD(buffer_info, "presentationTimeUs", frame->pts);

	if (codec_ctx->codec_type == AVMEDIA_TYPE_AUDIO) {
		if (!ctx->audio.swr) {
			printf("ctx->sample_rate = %d\n", codec_ctx->sample_rate);
			printf("ctx->ch_layout.nb_channels = %d\n", codec_ctx->ch_layout.nb_channels);
			printf("ctx->sample_fmt = %d\n", codec_ctx->sample_fmt);

			int ret = swr_alloc_set_opts2(&ctx->audio.swr,
			                              &codec_ctx->ch_layout,
			                              AV_SAMPLE_FMT_S16,
			                              ctx->audio.sample_rate,
			                              &codec_ctx->ch_layout,
			                              codec_ctx->sample_fmt,
			                              codec_ctx->sample_rate,
			                              0,
			                              NULL);
			if (ret != 0) {
				fprintf(stderr, "FFmpegDecoder error: Swresampler alloc fail\n");
			}
			swr_init(ctx->audio.swr);
		}
		uint8_t *raw_buffer = get_nio_buffer(env, buffer, &array_ref, &array);
		int outSamples = swr_convert(ctx->audio.swr, &raw_buffer, frame->nb_samples, (uint8_t const **)(frame->data), frame->nb_samples);
		release_nio_buffer(env, array_ref, array);
		_SET_INT_FIELD(buffer_info, "offset", 0);
		_SET_INT_FIELD(buffer_info, "size", outSamples * 2 * codec_ctx->ch_layout.nb_channels);

		av_frame_free(&frame);
	} else if (codec_ctx->codec_type == AVMEDIA_TYPE_VIDEO) {
		// copy frame pointer into data buffer to be read by releaseOutputBuffer function
		uint8_t *raw_buffer = get_nio_buffer(env, buffer, &array_ref, &array);
		*((AVFrame **)raw_buffer) = frame;
		release_nio_buffer(env, array_ref, array);
		_SET_INT_FIELD(buffer_info, "offset", 0);
		_SET_INT_FIELD(buffer_info, "size", sizeof(AVFrame *));
	}
	return 0;
}

// callback to perform wayland stuff on main thread
static gboolean render_frame(void *data)
{
	struct render_frame_data *d = (struct render_frame_data *)data;
	AVFrame *frame = d->frame;
	int ret;

	AVFrame *drm_frame;
	if (frame->format != AV_PIX_FMT_DRM_PRIME) {
		drm_frame = av_frame_alloc();
		drm_frame->format = AV_PIX_FMT_DRM_PRIME;
		drm_frame->hw_frames_ctx = av_buffer_ref(frame->hw_frames_ctx);

		// Convert the VA-API frame into a DMA-BUF frame
		ret = av_hwframe_map(drm_frame, frame, 0);
		if (ret < 0) {
			fprintf(stderr, "Failed to map frame: %s\n", av_err2str(ret));
			exit(1);
		}
		av_frame_free(&frame);
	} else {
		drm_frame = frame;
	}

	AVDRMFrameDescriptor *drm_frame_desc = (void *)drm_frame->data[0];

	GdkTexture *texture = import_drm_frame_desc_as_texture(drm_frame_desc, drm_frame->width, drm_frame->height, drm_frame);
	deliver_texture(d, texture);
	free(d);

	return G_SOURCE_REMOVE;
}

static gboolean render_texture(void *data)
{
	struct render_frame_data *d = (struct render_frame_data *)data;

	deliver_texture(d, d->texture);

	free(d);
	return G_SOURCE_REMOVE;
}

JNIEXPORT void JNICALL Java_android_media_MediaCodec_native_1releaseOutputBuffer(JNIEnv *env, jobject this, jlong codec, jobject buffer, jboolean render, jlong presentationTimeNs)
{
	struct ATL_codec_context *ctx = _PTR(codec);
	jarray array_ref;
	jbyte *array;
	AVFrame *frame;

	if (ctx->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
		AVFrame **raw_buffer = get_nio_buffer(env, buffer, &array_ref, &array);
		frame = *raw_buffer;
		*raw_buffer = NULL;
		release_nio_buffer(env, array_ref, array);

		if (!frame)
			return;

		if (!render || (!ctx->video.surface_view_widget && !ctx->video.surface_texture && !ctx->video.remote)) {
			fprintf(stderr, "skipping %dx%d frame!\n", frame->width, frame->height);
			av_frame_free(&frame);
			return;
		}

		/* a SurfaceTexture's consumer may read the pixels (updateTexImage()), and a frame to be turned is turned
		 * in memory: a hardware frame is copied to memory first */
		if (frame->hw_frames_ctx && (ctx->video.surface_texture || ctx->video.rotation)) {
			AVFrame *sw_frame = av_frame_alloc();
			if (av_hwframe_transfer_data(sw_frame, frame, 0) < 0 || av_frame_copy_props(sw_frame, frame) < 0) {
				fprintf(stderr, "skipping %dx%d frame: can't copy it from the hardware\n", frame->width, frame->height);
				av_frame_free(&sw_frame);
				av_frame_free(&frame);
				return;
			}
			av_frame_free(&frame);
			frame = sw_frame;
		}
		long timestamp_ns = presentationTimeNs ? presentationTimeNs : frame->pts * 1000;

		if (!frame->hw_frames_ctx) {
			const AVPixFmtDescriptor *desc = av_pix_fmt_desc_get(frame->format);
			enum AVPixelFormat gdk_pix_fmt;
			GdkMemoryFormat gdk_mem_fmt;
			int stride;
			if (desc != NULL && (desc->flags & AV_PIX_FMT_FLAG_ALPHA)) {
				gdk_pix_fmt = AV_PIX_FMT_RGBA;
				gdk_mem_fmt = GDK_MEMORY_R8G8B8A8;
				stride = frame->width * 4;
			} else {
				gdk_pix_fmt = AV_PIX_FMT_RGB24;
				gdk_mem_fmt = GDK_MEMORY_R8G8B8;
				stride = frame->width * 3;
			}

			// use swscale to convert YUV to RGB
			ctx->video.sws = sws_getCachedContext(ctx->video.sws, frame->width, frame->height, frame->format,
			                                      frame->width, frame->height, gdk_pix_fmt, 0, NULL, NULL, NULL);
			guchar *data_rgb = g_try_malloc0(frame->height * stride);
			sws_scale(ctx->video.sws, (const uint8_t *const *)frame->data, frame->linesize, 0,
			          frame->height, (uint8_t *[1]){data_rgb}, (int[1]){stride});

			int width = frame->width, height = frame->height;
			if (ctx->video.rotation) {
				int bpp = stride / frame->width;
				guchar *turned = g_malloc(frame->height * stride);
				rotate_plane(turned, data_rgb, stride, width, height, bpp, ctx->video.rotation);
				g_free(data_rgb);
				data_rgb = turned;
				if (ctx->video.rotation != 180) {
					width = frame->height;
					height = frame->width;
					stride = width * bpp;
				}
			}

			GBytes *bytes = g_bytes_new_take(data_rgb, height * stride);
			GdkTexture *texture = gdk_memory_texture_new(width, height, gdk_mem_fmt, bytes, stride);
			struct render_frame_data *data = malloc(sizeof(struct render_frame_data));
			data->texture = texture;
			data->surface_view_widget = ctx->video.surface_view_widget;
			data->surface_texture = ctx->video.surface_texture ? (*env)->NewGlobalRef(env, ctx->video.surface_texture) : NULL;
			data->remote = ctx->video.remote;
			if (data->remote)
				ANativeWindow_acquire(data->remote);
			data->release_time_ns = presentationTimeNs;
			data->timestamp_ns = timestamp_ns;
			data->crop = ctx->video.crop;
			schedule_render(render_texture, data, texture);
			g_bytes_unref(bytes);
			av_frame_free(&frame);
			return;
		}

		struct render_frame_data *data = malloc(sizeof(struct render_frame_data));
		data->frame = frame;
		data->release_time_ns = presentationTimeNs;
		data->surface_view_widget = ctx->video.surface_view_widget;
		data->surface_texture = NULL;
		data->crop = ctx->video.crop;
		data->remote = ctx->video.remote;
		if (data->remote)
			ANativeWindow_acquire(data->remote);
		schedule_render(render_frame, data, NULL);
	}
}

JNIEXPORT void JNICALL Java_android_media_MediaCodec_native_1flush(JNIEnv *env, jobject this, jlong codec)
{
	struct ATL_codec_context *ctx = _PTR(codec);
	avcodec_flush_buffers(ctx->codec);
}

JNIEXPORT void JNICALL Java_android_media_MediaCodec_native_1release(JNIEnv *env, jobject this, jlong codec)
{
	struct ATL_codec_context *ctx = _PTR(codec);
	if (ctx->codec->codec_type == AVMEDIA_TYPE_VIDEO && ctx->video.sws) {
		sws_freeContext(ctx->video.sws);
	}
	if (ctx->codec->codec_type == AVMEDIA_TYPE_VIDEO && ctx->video.surface_texture)
		(*env)->DeleteGlobalRef(env, ctx->video.surface_texture);
	if (ctx->codec->codec_type == AVMEDIA_TYPE_VIDEO && ctx->video.remote)
		ANativeWindow_release(ctx->video.remote);

	avcodec_free_context(&ctx->codec);
	free(ctx);
}
