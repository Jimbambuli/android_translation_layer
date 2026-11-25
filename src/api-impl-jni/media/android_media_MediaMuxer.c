/* MediaMuxer (AOSP media/jni/android_media_MediaMuxer.cpp on libstagefright's MediaMuxer) on FFmpeg's muxers: the
 * tracks come from the MediaFormats of addTrack() (type, size, sample rate, channels, csd-0/csd-1), the samples are
 * written interleaved, start() writes the header and stop() the index. */
#include <libavformat/avformat.h>
#include <libavutil/display.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include <glib.h>

#include "../defines.h"
#include "../generated_headers/android_media_MediaMuxer.h"

struct muxer {
	AVFormatContext *oc;
	int fd; /* own copy of the app's descriptor */
	int rotation;
	gboolean started;
};

static int fd_write(void *opaque, const uint8_t *buf, int size)
{
	struct muxer *mx = opaque;
	int done = 0;
	while (done < size) {
		ssize_t n = write(mx->fd, buf + done, size - done);
		if (n < 0)
			return AVERROR(errno);
		done += n;
	}
	return done;
}

static int64_t fd_seek(void *opaque, int64_t offset, int whence)
{
	struct muxer *mx = opaque;
	if (whence == AVSEEK_SIZE)
		return -1;
	return lseek(mx->fd, offset, whence & ~AVSEEK_FORCE);
}

static void throw(JNIEnv *env, const char *class, const char *message)
{
	(*env)->ThrowNew(env, (*env)->FindClass(env, class), message);
}

/* MediaMuxer.OutputFormat */
static const char *muxer_name(int format)
{
	switch (format) {
		case 1:
			return "webm";
		case 2:
			return "3gp";
		case 3:
			return "heif"; /* FFmpeg has no HEIF muxer: setup fails as AOSP's for an unsupported format */
		case 4:
			return "ogg";
		default:
			return "mp4";
	}
}

JNIEXPORT jlong JNICALL Java_android_media_MediaMuxer_nativeSetup(JNIEnv *env, jclass clazz, jobject fd_object, jint format)
{
	int fd = (*env)->GetIntField(env, fd_object, (*env)->GetFieldID(env, (*env)->GetObjectClass(env, fd_object), "descriptor", "I"));
	struct muxer *mx = g_new0(struct muxer, 1);
	mx->fd = dup(fd);
	if (mx->fd < 0 || avformat_alloc_output_context2(&mx->oc, NULL, muxer_name(format), NULL) < 0) {
		if (mx->fd >= 0)
			close(mx->fd);
		g_free(mx);
		throw(env, "java/io/IOException", "Failed to instantiate the muxer");
		return 0;
	}
	mx->oc->pb = avio_alloc_context(av_malloc(65536), 65536, 1, mx, NULL, fd_write, fd_seek);
	return _INTPTR(mx);
}

static const struct {
	const char *mime;
	enum AVCodecID id;
} CODECS[] = {
	{"video/avc", AV_CODEC_ID_H264},
	{"video/hevc", AV_CODEC_ID_HEVC},
	{"video/x-vnd.on2.vp8", AV_CODEC_ID_VP8},
	{"video/x-vnd.on2.vp9", AV_CODEC_ID_VP9},
	{"video/av01", AV_CODEC_ID_AV1},
	{"video/mp4v-es", AV_CODEC_ID_MPEG4},
	{"video/3gpp", AV_CODEC_ID_H263},
	{"audio/mp4a-latm", AV_CODEC_ID_AAC},
	{"audio/opus", AV_CODEC_ID_OPUS},
	{"audio/vorbis", AV_CODEC_ID_VORBIS},
	{"audio/3gpp", AV_CODEC_ID_AMR_NB},
	{"audio/amr-wb", AV_CODEC_ID_AMR_WB},
	{"audio/flac", AV_CODEC_ID_FLAC},
	{"audio/raw", AV_CODEC_ID_PCM_S16LE},
};

static int int_value(JNIEnv *env, jobject value)
{
	jclass number = (*env)->FindClass(env, "java/lang/Number");
	if (!value || !(*env)->IsInstanceOf(env, value, number))
		return 0;
	return (*env)->CallIntMethod(env, value, (*env)->GetMethodID(env, number, "intValue", "()I"));
}

/* the bytes of a ByteBuffer value (csd-N) from its position to its limit */
static void append_buffer(JNIEnv *env, jobject value, uint8_t **data, int *size)
{
	jclass byte_buffer = (*env)->FindClass(env, "java/nio/ByteBuffer");
	if (!value || !(*env)->IsInstanceOf(env, value, byte_buffer))
		return;
	int position = (*env)->CallIntMethod(env, value, (*env)->GetMethodID(env, byte_buffer, "position", "()I"));
	int limit = (*env)->CallIntMethod(env, value, (*env)->GetMethodID(env, byte_buffer, "limit", "()I"));
	if (limit <= position)
		return;
	jbyteArray bytes = (*env)->NewByteArray(env, limit - position);
	jobject dup = (*env)->CallObjectMethod(env, value, (*env)->GetMethodID(env, byte_buffer, "duplicate", "()Ljava/nio/ByteBuffer;"));
	(*env)->CallObjectMethod(env, dup, (*env)->GetMethodID(env, byte_buffer, "get", "([B)Ljava/nio/ByteBuffer;"), bytes);
	*data = av_realloc(*data, *size + (limit - position) + AV_INPUT_BUFFER_PADDING_SIZE);
	(*env)->GetByteArrayRegion(env, bytes, 0, limit - position, (jbyte *)*data + *size);
	*size += limit - position;
	memset(*data + *size, 0, AV_INPUT_BUFFER_PADDING_SIZE);
}

/* AOSP: the index of the new track; IllegalArgumentException for a format the muxer can't take */
JNIEXPORT jint JNICALL Java_android_media_MediaMuxer_nativeAddTrack(JNIEnv *env, jclass clazz, jlong ptr, jobjectArray keys, jobjectArray values)
{
	struct muxer *mx = _PTR(ptr);
	AVCodecParameters *par = avcodec_parameters_alloc();
	jobject csd[3] = {0};
	int n = (*env)->GetArrayLength(env, keys);
	for (int i = 0; i < n; i++) {
		jstring jkey = (*env)->GetObjectArrayElement(env, keys, i);
		jobject value = (*env)->GetObjectArrayElement(env, values, i);
		const char *key = (*env)->GetStringUTFChars(env, jkey, NULL);
		if (!strcmp(key, "mime")) {
			const char *mime = (*env)->GetStringUTFChars(env, value, NULL);
			for (size_t j = 0; j < G_N_ELEMENTS(CODECS); j++)
				if (!g_ascii_strcasecmp(mime, CODECS[j].mime))
					par->codec_id = CODECS[j].id;
			par->codec_type = g_str_has_prefix(mime, "video/") ? AVMEDIA_TYPE_VIDEO : AVMEDIA_TYPE_AUDIO;
			(*env)->ReleaseStringUTFChars(env, value, mime);
		} else if (!strcmp(key, "width")) {
			par->width = int_value(env, value);
		} else if (!strcmp(key, "height")) {
			par->height = int_value(env, value);
		} else if (!strcmp(key, "sample-rate")) {
			par->sample_rate = int_value(env, value);
		} else if (!strcmp(key, "channel-count")) {
			av_channel_layout_default(&par->ch_layout, int_value(env, value));
		} else if (!strcmp(key, "bitrate")) {
			par->bit_rate = int_value(env, value);
		} else if (!strncmp(key, "csd-", 4) && key[4] >= '0' && key[4] <= '2' && !key[5]) {
			csd[key[4] - '0'] = value;
			value = NULL;
		}
		(*env)->ReleaseStringUTFChars(env, jkey, key);
		(*env)->DeleteLocalRef(env, jkey);
		if (value)
			(*env)->DeleteLocalRef(env, value);
	}
	/* the codec-specific data as FFmpeg's extradata: AVC/HEVC Annex B parameter sets (csd-0 + csd-1), which the mp4
	 * muxer turns into avcC/hvcC; AAC's AudioSpecificConfig and Opus' OpusHead as they are */
	int csd_count = par->codec_id == AV_CODEC_ID_H264 ? 2 : 1;
	for (int i = 0; i < csd_count; i++)
		append_buffer(env, csd[i], &par->extradata, &par->extradata_size);
	for (int i = 0; i < 3; i++)
		if (csd[i])
			(*env)->DeleteLocalRef(env, csd[i]);
	if (par->codec_id == AV_CODEC_ID_PCM_S16LE)
		par->format = AV_SAMPLE_FMT_S16;
	if (mx->started || par->codec_id == AV_CODEC_ID_NONE ||
	    avformat_query_codec(mx->oc->oformat, par->codec_id, FF_COMPLIANCE_NORMAL) != 1) {
		avcodec_parameters_free(&par);
		throw(env, "java/lang/IllegalArgumentException", "Failed to add the track to the muxer");
		return -1;
	}
	AVStream *st = avformat_new_stream(mx->oc, NULL);
	avcodec_parameters_copy(st->codecpar, par);
	avcodec_parameters_free(&par);
	st->time_base = (AVRational){1, 1000000};
	return st->index;
}

/* AOSP setOrientationHint(): the video's display matrix (mp4 'tkhd' matrix) */
JNIEXPORT void JNICALL Java_android_media_MediaMuxer_nativeSetOrientationHint(JNIEnv *env, jclass clazz, jlong ptr, jint degrees)
{
	((struct muxer *)_PTR(ptr))->rotation = degrees;
}

/* AOSP writes the location as mp4 'udta' ©xyz; FFmpeg's mp4 muxer takes it as the "location" metadata */
JNIEXPORT void JNICALL Java_android_media_MediaMuxer_nativeSetLocation(JNIEnv *env, jclass clazz, jlong ptr, jint latitude, jint longitude)
{
	char location[64];
	snprintf(location, sizeof(location), "%+.4f%+.4f/", latitude / 10000.0, longitude / 10000.0);
	av_dict_set(&((struct muxer *)_PTR(ptr))->oc->metadata, "location", location, 0);
}

JNIEXPORT void JNICALL Java_android_media_MediaMuxer_nativeStart(JNIEnv *env, jclass clazz, jlong ptr)
{
	struct muxer *mx = _PTR(ptr);
	for (unsigned int i = 0; i < mx->oc->nb_streams; i++) {
		AVStream *st = mx->oc->streams[i];
		if (mx->rotation && st->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
			AVPacketSideData *sd = av_packet_side_data_new(&st->codecpar->coded_side_data, &st->codecpar->nb_coded_side_data,
			                                               AV_PKT_DATA_DISPLAYMATRIX, sizeof(int32_t) * 9, 0);
			if (sd)
				av_display_rotation_set((int32_t *)sd->data, -mx->rotation);
		}
	}
	if (avformat_write_header(mx->oc, NULL) < 0) {
		throw(env, "java/lang/IllegalStateException", "Failed to start the muxer");
		return;
	}
	mx->started = TRUE;
}

/* AOSP: a codec-config buffer is the track's codec-specific data, given with the format already: not a sample */
JNIEXPORT void JNICALL Java_android_media_MediaMuxer_nativeWriteSampleData(JNIEnv *env, jclass clazz, jlong ptr, jint track, jobject buffer, jint offset, jint size, jlong pts_us, jint flags)
{
	struct muxer *mx = _PTR(ptr);
	if ((flags & 2 /* BUFFER_FLAG_CODEC_CONFIG */) || size <= 0 || track < 0 || track >= (int)mx->oc->nb_streams)
		return;
	uint8_t *data = (*env)->GetDirectBufferAddress(env, buffer);
	if (!data) {
		throw(env, "java/lang/IllegalArgumentException", "the buffer must be a direct ByteBuffer");
		return;
	}
	AVPacket *pkt = av_packet_alloc();
	if (av_new_packet(pkt, size) == 0) {
		memcpy(pkt->data, data + offset, size);
		AVStream *st = mx->oc->streams[track];
		pkt->stream_index = track;
		pkt->pts = pkt->dts = av_rescale_q(pts_us, (AVRational){1, 1000000}, st->time_base);
		if (flags & 1 /* BUFFER_FLAG_KEY_FRAME */)
			pkt->flags |= AV_PKT_FLAG_KEY;
		if (av_interleaved_write_frame(mx->oc, pkt) < 0)
			fprintf(stderr, "MediaMuxer: sample of track %d at %lld us not written\n", track, (long long)pts_us);
	}
	av_packet_free(&pkt);
}

JNIEXPORT void JNICALL Java_android_media_MediaMuxer_nativeStop(JNIEnv *env, jclass clazz, jlong ptr)
{
	struct muxer *mx = _PTR(ptr);
	if (!mx->started)
		return;
	mx->started = FALSE;
	if (av_write_trailer(mx->oc) < 0)
		throw(env, "java/lang/IllegalStateException", "Failed to stop the muxer");
	avio_flush(mx->oc->pb);
}

JNIEXPORT void JNICALL Java_android_media_MediaMuxer_nativeRelease(JNIEnv *env, jclass clazz, jlong ptr)
{
	struct muxer *mx = _PTR(ptr);
	if (mx->oc->pb) {
		av_freep(&mx->oc->pb->buffer);
		avio_context_free(&mx->oc->pb);
	}
	avformat_free_context(mx->oc);
	close(mx->fd);
	g_free(mx);
}
