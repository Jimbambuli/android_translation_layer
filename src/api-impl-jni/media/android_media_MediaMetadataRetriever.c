#include <errno.h>
#include <inttypes.h>
#include <unistd.h>

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_media_MediaMetadataRetriever.h"

/* MediaMetadataRetriever.OPTION_* */
#define OPTION_PREVIOUS_SYNC 0
#define OPTION_NEXT_SYNC 1
#define OPTION_CLOSEST_SYNC 2
#define OPTION_CLOSEST 3

/* MediaMetadataRetriever.METADATA_KEY_* */
#define METADATA_KEY_DURATION 9
#define METADATA_KEY_VIDEO_WIDTH 18
#define METADATA_KEY_VIDEO_HEIGHT 19

/* a region of a file descriptor, like setDataSource(FileDescriptor, long, long) */
struct fd_region {
	int fd;
	int64_t offset;
	int64_t length;
	int64_t position;
};

static int fd_region_read(void *opaque, uint8_t *buf, int size)
{
	struct fd_region *region = opaque;
	if (region->position >= region->length)
		return AVERROR_EOF;
	ssize_t ret = pread(region->fd, buf, MIN((int64_t)size, region->length - region->position), region->offset + region->position);
	if (ret < 0)
		return AVERROR(errno);
	if (ret == 0)
		return AVERROR_EOF;
	region->position += ret;
	return ret;
}

static int64_t fd_region_seek(void *opaque, int64_t offset, int whence)
{
	struct fd_region *region = opaque;
	switch (whence & ~AVSEEK_FORCE) {
		case AVSEEK_SIZE:
			return region->length;
		case SEEK_SET:
			region->position = offset;
			break;
		case SEEK_CUR:
			region->position += offset;
			break;
		case SEEK_END:
			region->position = region->length + offset;
			break;
		default:
			return AVERROR(EINVAL);
	}
	return region->position;
}

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
/* opens the file at path, or the fd region if path is NULL; *io is the region's AVIOContext */
static AVFormatContext *open_input(const char *path, struct fd_region *region, AVIOContext **io)
{
	AVFormatContext *format = avformat_alloc_context();

	*io = NULL;
	if (!path) {
		*io = avio_alloc_context(av_malloc(32768), 32768, 0, region, fd_region_read, NULL, fd_region_seek);
		format->pb = *io;
	}
	/* avformat_open_input() frees the context on failure */
	if (avformat_open_input(&format, path, NULL, NULL) < 0)
		return NULL;
	if (avformat_find_stream_info(format, NULL) < 0)
		avformat_close_input(&format);
	return format;
}

/* format may be NULL (open failed), the region's io is freed anyway */
static void close_input(AVFormatContext *format, AVIOContext *io)
{
	avformat_close_input(&format);
	if (io) {
		av_freep(&io->buffer);
		avio_context_free(&io);
	}
}

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

/* METADATA_KEY_DURATION (ms), METADATA_KEY_VIDEO_WIDTH/HEIGHT, read from the container like AOSP's
 * retriever does without preparing a player; NULL if the source does not have it */
JNIEXPORT jstring JNICALL Java_android_media_MediaMetadataRetriever_native_1extractMetadata(JNIEnv *env, jclass class, jstring path_jstr, jint fd, jlong offset, jlong length, jint key)
{
	struct fd_region region = {.fd = fd, .offset = offset, .length = length};
	const char *path = get_source(env, path_jstr, &region);
	AVIOContext *io;
	AVFormatContext *format = open_input(path, &region, &io);
	char value[32] = "";

	if (format && key == METADATA_KEY_DURATION && format->duration != AV_NOPTS_VALUE) {
		snprintf(value, sizeof(value), "%" PRId64, format->duration / 1000);
	} else if (format && (key == METADATA_KEY_VIDEO_WIDTH || key == METADATA_KEY_VIDEO_HEIGHT)) {
		int stream_index = av_find_best_stream(format, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
		if (stream_index >= 0) {
			AVCodecParameters *par = format->streams[stream_index]->codecpar;
			snprintf(value, sizeof(value), "%d", key == METADATA_KEY_VIDEO_WIDTH ? par->width : par->height);
		}
	}
	close_input(format, io);
	if (path)
		(*env)->ReleaseStringUTFChars(env, path_jstr, path);
	return value[0] ? _JSTRING(value) : NULL;
}
