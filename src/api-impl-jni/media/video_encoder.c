/* Video encoders for MediaCodec and MediaRecorder: FFmpeg's encoders, VA-API ones as the hardware codecs (AOSP lists
 * the vendor's hardware encoders before the platform's software ones). The input is a frame the CPU can read (an
 * app's ByteBuffer, the planes of an ImageReader image, an AHardwareBuffer of the GPU or a decoder), converted into
 * the encoder's format; a VA-API encoder gets it uploaded into its surfaces. */
#include <libavutil/hwcontext.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
#include <stdio.h>
#include <string.h>

#include <glib.h>

#include "../defines.h"
#include "../generated_headers/android_media_MediaCodec.h"
#include "../../libandroid/hardware_buffer.h"
#include "video_encoder.h"

/* android/hardware_buffer.h */
#define AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM 1
#define AHARDWAREBUFFER_FORMAT_R8G8B8X8_UNORM 2
#define AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN 3UL

int AHardwareBuffer_lockPlanes(struct AHardwareBuffer *buffer, uint64_t usage, int32_t fence, const ARect *rect, AHardwareBuffer_Planes *outPlanes);
int AHardwareBuffer_unlock(struct AHardwareBuffer *buffer, int32_t *fence);

struct atl_venc {
	AVCodecContext *ctx;
	enum AVPixelFormat sw_format; /* of the frames given to the encoder, or uploaded for VA-API */
	struct SwsContext *sws;
	bool request_sync;
	int64_t last_pts;
	int64_t frames; /* sent to the encoder */
	bool empty_eof; /* drained without a frame: FFmpeg's encoders are not given that (VA-API's crashes) */
};

/* per type the hardware (VA-API) encoders, then the software ones */
static const struct {
	const char *mime;
	const char *hardware[2];
	const char *software[3];
} ENCODERS[] = {
	{"video/avc", {"h264_vaapi"}, {"libx264", "libopenh264"}},
	{"video/hevc", {"hevc_vaapi"}, {"libx265"}},
	{"video/x-vnd.on2.vp8", {"vp8_vaapi"}, {"libvpx"}},
	{"video/x-vnd.on2.vp9", {"vp9_vaapi"}, {"libvpx-vp9"}},
	{"video/av01", {"av1_vaapi"}, {"libsvtav1", "libaom-av1"}},
	{"video/mp4v-es", {NULL}, {"mpeg4"}},
	{"video/3gpp", {NULL}, {"h263"}},
};

/* AOSP CodecCapabilities: an encoder is listed if it works; tried once per encoder with a small frame */
static bool encoder_works(const char *name)
{
	static GHashTable *works;
	static GMutex lock;
	g_mutex_lock(&lock);
	if (!works)
		works = g_hash_table_new(g_str_hash, g_str_equal);
	gpointer known;
	bool ret;
	if (g_hash_table_lookup_extended(works, name, NULL, &known)) {
		ret = GPOINTER_TO_INT(known);
	} else {
		struct atl_venc *venc = avcodec_find_encoder_by_name(name) ? atl_venc_open(name, 320, 240, 30, 500000, 1, 1, true) : NULL;
		ret = venc != NULL;
		atl_venc_free(venc);
		g_hash_table_insert(works, (gpointer)name, GINT_TO_POINTER(ret));
	}
	g_mutex_unlock(&lock);
	return ret;
}

const char *atl_venc_find(const char *mime, bool hardware)
{
	for (size_t i = 0; i < G_N_ELEMENTS(ENCODERS); i++) {
		if (g_ascii_strcasecmp(ENCODERS[i].mime, mime))
			continue;
		const char *const *names = hardware ? ENCODERS[i].hardware : ENCODERS[i].software;
		for (int j = 0; j < (hardware ? 2 : 3) && names[j]; j++)
			if (encoder_works(names[j]))
				return names[j];
	}
	return NULL;
}

static bool is_vaapi(const AVCodec *codec)
{
	return g_str_has_suffix(codec->name, "_vaapi");
}

struct atl_venc *atl_venc_open(const char *encoder, int width, int height, int frame_rate, int bit_rate, int bitrate_mode,
                               float i_frame_interval, bool global_header)
{
	const AVCodec *codec = avcodec_find_encoder_by_name(encoder);
	if (!codec || width <= 0 || height <= 0)
		return NULL;
	struct atl_venc *venc = g_new0(struct atl_venc, 1);
	venc->last_pts = INT64_MIN;
	AVCodecContext *ctx = venc->ctx = avcodec_alloc_context3(codec);
	if (frame_rate <= 0)
		frame_rate = 30;
	ctx->width = width;
	ctx->height = height;
	ctx->time_base = (AVRational){1, 1000000};
	ctx->framerate = (AVRational){frame_rate, 1};
	/* AOSP KEY_I_FRAME_INTERVAL: seconds between key frames, 0 every frame a key frame, negative only the first */
	ctx->gop_size = i_frame_interval < 0 ? INT_MAX : MAX(1, (int)(i_frame_interval * frame_rate + 0.5f));
	ctx->max_b_frames = 0; /* AOSP's encoders give frames in presentation order unless B frames are asked for */
	ctx->bit_rate = bit_rate > 0 ? bit_rate : (int64_t)width * height * frame_rate / 8;
	if (bitrate_mode == 2) /* BITRATE_MODE_CBR */
		ctx->rc_max_rate = ctx->bit_rate;
	ctx->color_range = AVCOL_RANGE_MPEG; /* sws' RGB to YUV conversion: BT.601, limited range */
	ctx->colorspace = AVCOL_SPC_SMPTE170M;
	if (global_header)
		ctx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
	if (is_vaapi(codec)) {
		AVBufferRef *device = NULL;
		AVBufferRef *frames = NULL;
		if (av_hwdevice_ctx_create(&device, AV_HWDEVICE_TYPE_VAAPI, NULL, NULL, 0) < 0 || !(frames = av_hwframe_ctx_alloc(device))) {
			av_buffer_unref(&device);
			goto fail;
		}
		AVHWFramesContext *frames_ctx = (AVHWFramesContext *)frames->data;
		frames_ctx->format = AV_PIX_FMT_VAAPI;
		frames_ctx->sw_format = AV_PIX_FMT_NV12;
		frames_ctx->width = width;
		frames_ctx->height = height;
		frames_ctx->initial_pool_size = 8;
		int ret = av_hwframe_ctx_init(frames);
		av_buffer_unref(&device);
		if (ret < 0) {
			av_buffer_unref(&frames);
			goto fail;
		}
		ctx->hw_frames_ctx = frames;
		ctx->pix_fmt = AV_PIX_FMT_VAAPI;
		venc->sw_format = AV_PIX_FMT_NV12;
	} else {
		const enum AVPixelFormat *formats = NULL;
		int n = 0;
		avcodec_get_supported_config(ctx, codec, AV_CODEC_CONFIG_PIX_FORMAT, 0, (const void **)&formats, &n);
		venc->sw_format = n > 0 ? formats[0] : AV_PIX_FMT_YUV420P;
		for (int i = 0; i < n; i++)
			if (formats[i] == AV_PIX_FMT_YUV420P)
				venc->sw_format = AV_PIX_FMT_YUV420P;
		ctx->pix_fmt = venc->sw_format;
	}
	if (avcodec_open2(ctx, codec, NULL) < 0)
		goto fail;
	return venc;
fail:
	atl_venc_free(venc);
	return NULL;
}

AVCodecContext *atl_venc_context(struct atl_venc *venc)
{
	return venc->ctx;
}

void atl_venc_request_sync_frame(struct atl_venc *venc)
{
	venc->request_sync = true;
}

void atl_venc_free(struct atl_venc *venc)
{
	if (!venc)
		return;
	avcodec_free_context(&venc->ctx);
	sws_freeContext(venc->sws);
	g_free(venc);
}

int atl_venc_send(struct atl_venc *venc, enum AVPixelFormat format, uint8_t *const data[4], const int linesize[4], int width, int height, int64_t pts_us)
{
	AVCodecContext *ctx = venc->ctx;
	if (!data && !venc->frames) {
		venc->empty_eof = true;
		return 0;
	}
	if (!data)
		return avcodec_send_frame(ctx, NULL);
	if (pts_us <= venc->last_pts)
		return 0;
	venc->last_pts = pts_us;
	AVFrame *frame = av_frame_alloc();
	frame->format = venc->sw_format;
	frame->width = ctx->width;
	frame->height = ctx->height;
	int ret = av_frame_get_buffer(frame, 0);
	if (ret >= 0) {
		venc->sws = sws_getCachedContext(venc->sws, width, height, format, ctx->width, ctx->height, venc->sw_format, SWS_BILINEAR, NULL, NULL, NULL);
		ret = venc->sws ? sws_scale(venc->sws, (const uint8_t *const *)data, linesize, 0, height, frame->data, frame->linesize) : AVERROR(EINVAL);
	}
	frame->pts = pts_us;
	if (venc->request_sync) {
		frame->pict_type = AV_PICTURE_TYPE_I;
		venc->request_sync = false;
	}
	if (ret >= 0 && ctx->hw_frames_ctx) {
		AVFrame *hw_frame = av_frame_alloc();
		ret = av_hwframe_get_buffer(ctx->hw_frames_ctx, hw_frame, 0);
		if (ret >= 0)
			ret = av_hwframe_transfer_data(hw_frame, frame, 0);
		if (ret >= 0)
			ret = av_frame_copy_props(hw_frame, frame);
		av_frame_free(&frame);
		frame = hw_frame;
	}
	if (ret >= 0)
		ret = avcodec_send_frame(ctx, frame);
	if (ret >= 0)
		venc->frames++;
	av_frame_free(&frame);
	return ret;
}

int atl_venc_receive(struct atl_venc *venc, AVPacket *pkt)
{
	return venc->empty_eof ? AVERROR_EOF : avcodec_receive_packet(venc->ctx, pkt);
}

int64_t atl_venc_frames(struct atl_venc *venc)
{
	return venc->frames;
}

enum AVPixelFormat atl_venc_planes_format(int n_planes, uint8_t *const data[], const int pixel_stride[])
{
	if (n_planes == 1) {
		switch (pixel_stride[0]) {
			case 4:
				return AV_PIX_FMT_RGBA;
			case 3:
				return AV_PIX_FMT_RGB24;
			case 2:
				return AV_PIX_FMT_RGB565LE;
		}
	} else if (n_planes == 3 && pixel_stride[1] == 1 && pixel_stride[2] == 1) {
		return AV_PIX_FMT_YUV420P;
	} else if (n_planes >= 2 && pixel_stride[1] == 2) {
		/* YUV_420_888 with interleaved chroma: U before V is NV12, else NV21 */
		return n_planes == 3 && data[2] + 1 == data[1] ? AV_PIX_FMT_NV21 : AV_PIX_FMT_NV12;
	}
	return AV_PIX_FMT_NONE;
}

int atl_venc_send_buffer(struct atl_venc *venc, struct AHardwareBuffer *buffer, int64_t pts_us)
{
	AHardwareBuffer_Planes planes;
	if (AHardwareBuffer_lockPlanes(buffer, AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN, -1, NULL, &planes))
		return AVERROR(EINVAL);
	uint8_t *data[4] = {0};
	int linesize[4] = {0};
	int pixel_stride[4] = {0};
	for (uint32_t i = 0; i < planes.planeCount && i < 4; i++) {
		data[i] = planes.planes[i].data;
		linesize[i] = planes.planes[i].rowStride;
		pixel_stride[i] = planes.planes[i].pixelStride;
	}
	enum AVPixelFormat format;
	switch (buffer->desc.format) {
		case AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM:
			format = AV_PIX_FMT_RGBA;
			break;
		case AHARDWAREBUFFER_FORMAT_R8G8B8X8_UNORM:
			format = AV_PIX_FMT_RGB0;
			break;
		default:
			format = atl_venc_planes_format(planes.planeCount, data, pixel_stride);
	}
	/* NV12/NV21 as two planes for swscale */
	if ((format == AV_PIX_FMT_NV12 || format == AV_PIX_FMT_NV21) && planes.planeCount == 3) {
		if (format == AV_PIX_FMT_NV21)
			data[1] = data[2];
		data[2] = NULL;
		linesize[2] = 0;
	}
	int ret = format == AV_PIX_FMT_NONE ? AVERROR(EINVAL) : atl_venc_send(venc, format, data, linesize, buffer->desc.width, buffer->desc.height, pts_us);
	AHardwareBuffer_unlock(buffer, NULL);
	return ret;
}

/* -- MediaCodec's video encoders (MediaCodec.java) -- */

/* the encoders MediaCodecList lists: name, type and "hardware" or "software", per type the first that works */
JNIEXPORT jobjectArray JNICALL Java_android_media_MediaCodec_native_1encoders(JNIEnv *env, jclass clazz)
{
	const char *list[G_N_ELEMENTS(ENCODERS) * 2 * 3];
	int n = 0;
	for (int hardware = 1; hardware >= 0; hardware--) {
		for (size_t i = 0; i < G_N_ELEMENTS(ENCODERS); i++) {
			const char *name = atl_venc_find(ENCODERS[i].mime, hardware);
			if (!name)
				continue;
			list[n++] = name;
			list[n++] = ENCODERS[i].mime;
			list[n++] = hardware ? "hardware" : "software";
		}
	}
	jobjectArray array = (*env)->NewObjectArray(env, n, (*env)->FindClass(env, "java/lang/String"), NULL);
	for (int i = 0; i < n; i++)
		(*env)->SetObjectArrayElement(env, array, i, _JSTRING(list[i]));
	return array;
}

JNIEXPORT jlong JNICALL Java_android_media_MediaCodec_native_1encoderOpen(JNIEnv *env, jclass clazz, jstring name, jint width, jint height, jint frame_rate, jint bit_rate, jint bitrate_mode, jfloat i_frame_interval)
{
	const char *encoder = (*env)->GetStringUTFChars(env, name, NULL);
	/* the codec-specific data goes into the output format (csd-0/csd-1) and the first output buffer, as AOSP's encoders */
	struct atl_venc *venc = atl_venc_open(encoder, width, height, frame_rate, bit_rate, bitrate_mode, i_frame_interval, true);
	if (!venc)
		fprintf(stderr, "MediaCodec: can't open the encoder %s for %dx%d\n", encoder, width, height);
	(*env)->ReleaseStringUTFChars(env, name, encoder);
	return _INTPTR(venc);
}

JNIEXPORT jbyteArray JNICALL Java_android_media_MediaCodec_native_1encoderExtradata(JNIEnv *env, jclass clazz, jlong ptr)
{
	AVCodecContext *ctx = atl_venc_context(_PTR(ptr));
	if (!ctx->extradata_size)
		return NULL;
	jbyteArray array = (*env)->NewByteArray(env, ctx->extradata_size);
	(*env)->SetByteArrayRegion(env, array, 0, ctx->extradata_size, (jbyte *)ctx->extradata);
	return array;
}

/* a ByteBuffer of the app: COLOR_FormatYUV420Planar (and Flexible, whose ByteBuffer ATL lays out the same way) or
 * COLOR_FormatYUV420SemiPlanar, with the stride and slice height of the input format; NULL ends the stream */
JNIEXPORT jint JNICALL Java_android_media_MediaCodec_native_1encoderSendBuffer(JNIEnv *env, jclass clazz, jlong ptr, jobject buffer, jint offset, jint size, jint color_format, jint stride, jint slice_height, jlong pts_us)
{
	struct atl_venc *venc = _PTR(ptr);
	if (!buffer)
		return atl_venc_send(venc, AV_PIX_FMT_NONE, NULL, NULL, 0, 0, 0);
	AVCodecContext *ctx = atl_venc_context(venc);
	int width = ctx->width, height = ctx->height;
	stride = stride > 0 ? stride : width;
	slice_height = slice_height > 0 ? slice_height : height;
	uint8_t *base = (uint8_t *)(*env)->GetDirectBufferAddress(env, buffer) + offset;
	bool semi_planar = color_format == 21; /* COLOR_FormatYUV420SemiPlanar */
	if (size < width * height * 3 / 2)
		return AVERROR(EINVAL);
	uint8_t *data[4] = {base, base + (size_t)stride * slice_height};
	int linesize[4] = {stride, semi_planar ? stride : stride / 2};
	if (!semi_planar) {
		data[2] = data[1] + (size_t)(stride / 2) * ((slice_height + 1) / 2);
		linesize[2] = stride / 2;
	}
	return atl_venc_send(venc, semi_planar ? AV_PIX_FMT_NV12 : AV_PIX_FMT_YUV420P, data, linesize, width, height, pts_us);
}

/* an image of the input surface (its ImageReader): an AHardwareBuffer, else the planes of its ByteBuffers */
JNIEXPORT jint JNICALL Java_android_media_MediaCodec_native_1encoderSendImage(JNIEnv *env, jclass clazz, jlong ptr, jlong buffer, jobjectArray buffers, jintArray row_strides, jintArray pixel_strides, jint width, jint height, jlong pts_us)
{
	struct atl_venc *venc = _PTR(ptr);
	if (buffer)
		return atl_venc_send_buffer(venc, _PTR(buffer), pts_us);
	int n = buffers ? MIN((*env)->GetArrayLength(env, buffers), 4) : 0;
	uint8_t *data[4] = {0};
	int linesize[4] = {0};
	int pixel_stride[4] = {0};
	(*env)->GetIntArrayRegion(env, row_strides, 0, n, linesize);
	(*env)->GetIntArrayRegion(env, pixel_strides, 0, n, pixel_stride);
	for (int i = 0; i < n; i++) {
		jobject plane = (*env)->GetObjectArrayElement(env, buffers, i);
		data[i] = (*env)->GetDirectBufferAddress(env, plane);
		(*env)->DeleteLocalRef(env, plane);
	}
	enum AVPixelFormat format = atl_venc_planes_format(n, data, pixel_stride);
	if (format == AV_PIX_FMT_NV12 || format == AV_PIX_FMT_NV21) {
		if (format == AV_PIX_FMT_NV21 && n == 3)
			data[1] = data[2];
		data[2] = NULL;
		linesize[2] = 0;
	}
	if (format == AV_PIX_FMT_NONE)
		return AVERROR(EINVAL);
	return atl_venc_send(venc, format, data, linesize, width, height, pts_us);
}

/* the next packet into the output buffer: 0 with info set, AVERROR(EAGAIN) without one; the end of the stream is an
 * empty buffer with BUFFER_FLAG_END_OF_STREAM */
JNIEXPORT jint JNICALL Java_android_media_MediaCodec_native_1encoderReceive(JNIEnv *env, jclass clazz, jlong ptr, jobject buffer, jobject info)
{
	AVPacket *pkt = av_packet_alloc();
	int ret = atl_venc_receive(_PTR(ptr), pkt);
	int size = 0, flags = 0;
	int64_t pts = 0;
	if (ret == AVERROR_EOF) {
		flags = 4; /* BUFFER_FLAG_END_OF_STREAM */
		ret = 0;
	} else if (ret == 0) {
		size = MIN(pkt->size, (*env)->GetDirectBufferCapacity(env, buffer));
		if (size < pkt->size)
			fprintf(stderr, "MediaCodec: encoded frame of %d bytes cut to %d\n", pkt->size, size);
		memcpy((*env)->GetDirectBufferAddress(env, buffer), pkt->data, size);
		pts = pkt->pts;
		flags = (pkt->flags & AV_PKT_FLAG_KEY) ? 1 /* BUFFER_FLAG_KEY_FRAME */ : 0;
	}
	av_packet_free(&pkt);
	if (ret == 0) {
		jclass info_class = (*env)->GetObjectClass(env, info);
		(*env)->CallVoidMethod(env, info, (*env)->GetMethodID(env, info_class, "set", "(IIJI)V"), 0, size, (jlong)pts, flags);
	}
	return ret;
}

JNIEXPORT void JNICALL Java_android_media_MediaCodec_native_1encoderRequestSyncFrame(JNIEnv *env, jclass clazz, jlong ptr)
{
	atl_venc_request_sync_frame(_PTR(ptr));
}

JNIEXPORT void JNICALL Java_android_media_MediaCodec_native_1encoderFree(JNIEnv *env, jclass clazz, jlong ptr)
{
	atl_venc_free(_PTR(ptr));
}
