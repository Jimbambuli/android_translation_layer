#include <errno.h>
#include <inttypes.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_media_MediaMetadataRetriever.h"
#include "av_input.h"

/* MediaMetadataRetriever.OPTION_* */
#define OPTION_PREVIOUS_SYNC 0
#define OPTION_NEXT_SYNC 1
#define OPTION_CLOSEST_SYNC 2
#define OPTION_CLOSEST 3

/* MediaMetadataRetriever.METADATA_KEY_* */
#define METADATA_KEY_DATE         5
#define METADATA_KEY_YEAR         8
#define METADATA_KEY_DURATION 9
#define METADATA_KEY_NUM_TRACKS   10
#define METADATA_KEY_HAS_AUDIO    16
#define METADATA_KEY_HAS_VIDEO    17
#define METADATA_KEY_VIDEO_WIDTH 18
#define METADATA_KEY_VIDEO_HEIGHT 19
#define METADATA_KEY_BITRATE      20

/* decode the first frame at or after the seek target; with OPTION_CLOSEST, the first one at or
 * after time_us, otherwise the sync frame the demuxer seeked to */
static AVFrame *decode_frame(AVFormatContext *format, int stream_index, AVCodecContext *codec, int64_t target, gboolean closest)
{
	AVPacket *packet = av_packet_alloc();
	AVFrame *frame = av_frame_alloc();
	gboolean draining = FALSE;

	while (TRUE) {
		int ret = avcodec_receive_frame(codec, frame);
		if (ret == 0) {
			if (!closest || frame->best_effort_timestamp == AV_NOPTS_VALUE || frame->best_effort_timestamp >= target)
				break;
			av_frame_unref(frame);
			continue;
		}
		if (ret != AVERROR(EAGAIN) || draining) {
			av_frame_free(&frame);
			break;
		}
		if (av_read_frame(format, packet) < 0) {
			draining = TRUE;
			avcodec_send_packet(codec, NULL);
			continue;
		}
		if (packet->stream_index == stream_index)
			avcodec_send_packet(codec, packet);
		av_packet_unref(packet);
	}
	av_packet_free(&packet);
	return frame;
}

/* the frame as ARGB pixels (the int[] of Bitmap.createBitmap()), or NULL if there is no video or
 * it cannot be decoded; the source is the file at path, or the fd region if path is NULL */
static uint32_t *extract_frame(const char *path, struct fd_region *region, int64_t time_us, int option, int dst_width, int dst_height, int *out_width, int *out_height)
{
	AVIOContext *io;
	AVFormatContext *format = open_input(path, region, &io);
	AVCodecContext *codec = NULL;
	AVFrame *frame = NULL;
	uint32_t *pixels = NULL;

	if (!format)
		goto out;

	const AVCodec *decoder = NULL;
	int stream_index = av_find_best_stream(format, AVMEDIA_TYPE_VIDEO, -1, -1, &decoder, 0);
	if (stream_index < 0)
		goto out;
	AVStream *stream = format->streams[stream_index];
	codec = avcodec_alloc_context3(decoder);
	if (avcodec_parameters_to_context(codec, stream->codecpar) < 0 || avcodec_open2(codec, decoder, NULL) < 0)
		goto out;

	/* a negative time means "any frame", AOSP takes one near the start */
	int64_t target = av_rescale_q(MAX(time_us, 0), AV_TIME_BASE_Q, stream->time_base);
	if (stream->start_time != AV_NOPTS_VALUE)
		target += stream->start_time;
	if (time_us > 0)
		av_seek_frame(format, stream_index, target, option == OPTION_NEXT_SYNC ? 0 : AVSEEK_FLAG_BACKWARD);
	frame = decode_frame(format, stream_index, codec, target, option == OPTION_CLOSEST);
	if (!frame)
		goto out;

	int width = frame->width;
	int height = frame->height;
	if (dst_width > 0 && dst_height > 0) {
		/* like getScaledFrameAtTime(): fit into dst_width x dst_height, keeping the aspect ratio */
		if ((int64_t)dst_width * frame->height <= (int64_t)dst_height * frame->width) {
			width = dst_width;
			height = MAX(1, (int)((int64_t)frame->height * dst_width / frame->width));
		} else {
			height = dst_height;
			width = MAX(1, (int)((int64_t)frame->width * dst_height / frame->height));
		}
	}
	struct SwsContext *sws = sws_getContext(frame->width, frame->height, frame->format, width, height, AV_PIX_FMT_BGRA, SWS_BILINEAR, NULL, NULL, NULL);
	if (!sws)
		goto out;
	/* AV_PIX_FMT_BGRA is ARGB in a little endian int */
	pixels = g_new(uint32_t, (size_t)width * height);
	uint8_t *dst_data[4] = {(uint8_t *)pixels};
	int dst_linesize[4] = {width * 4};
	sws_scale(sws, (const uint8_t *const *)frame->data, frame->linesize, 0, frame->height, dst_data, dst_linesize);
	sws_freeContext(sws);
	*out_width = width;
	*out_height = height;

out:
	av_frame_free(&frame);
	avcodec_free_context(&codec);
	close_input(format, io);
	return pixels;
}

/* the source of the native_* calls: the file at path, or the region of fd (to its end if length
 * is unknown) */
static const char *get_source(JNIEnv *env, jstring path_jstr, struct fd_region *region)
{
	if (path_jstr)
		return (*env)->GetStringUTFChars(env, path_jstr, NULL);
	if (region->length <= 0 || region->length == INT64_MAX) {
		off_t end = lseek(region->fd, 0, SEEK_END);
		region->length = end > region->offset ? end - region->offset : 0;
	}
	return NULL;
}

JNIEXPORT jintArray JNICALL Java_android_media_MediaMetadataRetriever_native_1getFrameAtTime(JNIEnv *env, jclass class, jstring path_jstr, jint fd, jlong offset, jlong length, jlong time_us, jint option, jint dst_width, jint dst_height, jintArray size)
{
	struct fd_region region = {.fd = fd, .offset = offset, .length = length};
	const char *path = get_source(env, path_jstr, &region);
	int width, height;

	uint32_t *pixels = extract_frame(path, &region, time_us, option, dst_width, dst_height, &width, &height);
	if (path)
		(*env)->ReleaseStringUTFChars(env, path_jstr, path);
	if (!pixels)
		return NULL;

	jintArray array = (*env)->NewIntArray(env, width * height);
	(*env)->SetIntArrayRegion(env, array, 0, width * height, (jint *)pixels);
	(*env)->SetIntArrayRegion(env, size, 0, 2, (jint[]){width, height});
	g_free(pixels);
	return array;
}

/* the attached picture (ID3 APIC, MP4 covr, ...) as stored in the file, like AOSP's
 * getEmbeddedPicture(EMBEDDED_PICTURE_TYPE_ANY); NULL if there is none */
JNIEXPORT jbyteArray JNICALL Java_android_media_MediaMetadataRetriever_native_1getEmbeddedPicture(JNIEnv *env, jclass class, jstring path_jstr, jint fd, jlong offset, jlong length)
{
	struct fd_region region = {.fd = fd, .offset = offset, .length = length};
	const char *path = get_source(env, path_jstr, &region);
	AVIOContext *io;
	AVFormatContext *format = open_input(path, &region, &io);
	jbyteArray array = NULL;

	for (unsigned int i = 0; format && i < format->nb_streams; i++) {
		AVPacket *pic = &format->streams[i]->attached_pic;
		if ((format->streams[i]->disposition & AV_DISPOSITION_ATTACHED_PIC) && pic->size > 0) {
			array = (*env)->NewByteArray(env, pic->size);
			(*env)->SetByteArrayRegion(env, array, 0, pic->size, (jbyte *)pic->data);
			break;
		}
	}
	close_input(format, io);
	if (path)
		(*env)->ReleaseStringUTFChars(env, path_jstr, path);
	return array;
}

/* a tag of the container, else of its first stream that has it (Ogg keeps the Vorbis comments there) */
static const char *get_tag(AVFormatContext *format, const char *key)
{
	AVDictionaryEntry *entry = av_dict_get(format->metadata, key, NULL, 0);
	for (unsigned int i = 0; !entry && i < format->nb_streams; i++)
		entry = av_dict_get(format->streams[i]->metadata, key, NULL, 0);
	return entry ? entry->value : NULL;
}

static void set_value(JNIEnv *env, jobjectArray values, int key, const char *value)
{
	if (value && value[0])
		(*env)->SetObjectArrayElement(env, values, key, _JSTRING(value));
}

/* the values of the METADATA_KEY_* (the array index) that the container has, read once like AOSP's retriever
 * does in setDataSource(): durations in ms, tags as text, "yes" for HAS_AUDIO/HAS_VIDEO, DATE as
 * yyyyMMdd'T'HHmmss.SSS'Z'; null where the source does not have it */
JNIEXPORT jobjectArray JNICALL Java_android_media_MediaMetadataRetriever_native_1extractMetadata(JNIEnv *env, jclass class, jstring path_jstr, jint fd, jlong offset, jlong length, jint count)
{
	/* METADATA_KEY_CD_TRACK_NUMBER, ALBUM, ARTIST, AUTHOR, COMPOSER, GENRE, TITLE, WRITER, ALBUMARTIST,
	 * DISC_NUMBER, COMPILATION */
	static const struct {
		int key;
		const char *tag;
	} tags[] = {
		{ 0,        "track"},
		{ 1,        "album"},
		{ 2,       "artist"},
		{ 3,       "author"},
		{ 4,     "composer"},
		{ 6,        "genre"},
		{ 7,        "title"},
		{11,     "lyricist"},
		{13, "album_artist"},
		{14,         "disc"},
		{15,  "compilation"},
	};
	struct fd_region region = {.fd = fd, .offset = offset, .length = length};
	const char *path = get_source(env, path_jstr, &region);
	AVIOContext *io;
	AVFormatContext *format = open_input(path, &region, &io);
	jobjectArray values = (*env)->NewObjectArray(env, count, (*env)->FindClass(env, "java/lang/String"), NULL);
	char value[64];

	if (format) {
		for (size_t i = 0; i < G_N_ELEMENTS(tags); i++)
			set_value(env, values, tags[i].key, get_tag(format, tags[i].tag));
		const char *date = get_tag(format, "date");
		if (!date)
			date = get_tag(format, "year");
		if (date && strlen(date) >= 4 && g_ascii_isdigit(date[0]) && g_ascii_isdigit(date[1]) && g_ascii_isdigit(date[2]) && g_ascii_isdigit(date[3])) {
			snprintf(value, sizeof(value), "%.4s", date);
			set_value(env, values, METADATA_KEY_YEAR, value);
		}
		int year, month, day, hour, minute, second;
		const char *creation = get_tag(format, "creation_time");
		if (creation && sscanf(creation, "%4d-%2d-%2dT%2d:%2d:%2d", &year, &month, &day, &hour, &minute, &second) == 6) {
			snprintf(value, sizeof(value), "%04d%02d%02dT%02d%02d%02d.000Z", year, month, day, hour, minute, second);
			set_value(env, values, METADATA_KEY_DATE, value);
		}
		if (format->duration != AV_NOPTS_VALUE) {
			snprintf(value, sizeof(value), "%" PRId64, format->duration / 1000);
			set_value(env, values, METADATA_KEY_DURATION, value);
		}
		snprintf(value, sizeof(value), "%u", format->nb_streams);
		set_value(env, values, METADATA_KEY_NUM_TRACKS, value);
		if (format->bit_rate > 0) {
			snprintf(value, sizeof(value), "%" PRId64, format->bit_rate);
			set_value(env, values, METADATA_KEY_BITRATE, value);
		}
		if (av_find_best_stream(format, AVMEDIA_TYPE_AUDIO, -1, -1, NULL, 0) >= 0)
			set_value(env, values, METADATA_KEY_HAS_AUDIO, "yes");
		/* a cover picture is a video stream to FFmpeg, but not video */
		int stream_index = av_find_best_stream(format, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
		if (stream_index >= 0 && !(format->streams[stream_index]->disposition & AV_DISPOSITION_ATTACHED_PIC)) {
			AVCodecParameters *par = format->streams[stream_index]->codecpar;
			set_value(env, values, METADATA_KEY_HAS_VIDEO, "yes");
			snprintf(value, sizeof(value), "%d", par->width);
			set_value(env, values, METADATA_KEY_VIDEO_WIDTH, value);
			snprintf(value, sizeof(value), "%d", par->height);
			set_value(env, values, METADATA_KEY_VIDEO_HEIGHT, value);
		}
	}
	close_input(format, io);
	if (path)
		(*env)->ReleaseStringUTFChars(env, path_jstr, path);
	return values;
}
