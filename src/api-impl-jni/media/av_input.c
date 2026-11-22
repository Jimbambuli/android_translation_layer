#include <errno.h>
#include <unistd.h>

#include <glib.h>

#include "../defines.h"
#include "../util.h"
#include "av_input.h"

/* MediaDataSource.readAt(): the bytes read, -1 at the end; an exception is an I/O error */
static int data_source_read(struct fd_region *region, uint8_t *buf, int size)
{
	JNIEnv *env = get_jni_env();
	jbyteArray array = (*env)->NewByteArray(env, size);
	jmethodID read_at = _METHOD(_CLASS(region->data_source), "readAt", "(J[BII)I");
	jint ret = (*env)->CallIntMethod(env, region->data_source, read_at, (jlong)region->position, array, 0, size);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
		ret = AVERROR(EIO);
	} else if (ret <= 0) {
		ret = AVERROR_EOF;
	} else {
		ret = MIN(ret, size);
		(*env)->GetByteArrayRegion(env, array, 0, ret, (jbyte *)buf);
		region->position += ret;
	}
	(*env)->DeleteLocalRef(env, array);
	return ret;
}

static int fd_region_read(void *opaque, uint8_t *buf, int size)
{
	struct fd_region *region = opaque;
	if (region->position >= region->length)
		return AVERROR_EOF;
	if (region->data_source)
		return data_source_read(region, buf, size);
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
			if (region->data_source && region->length == INT64_MAX)
				return -1; /* getSize() did not know it */
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

/* opens the file at path, or the fd region if path is NULL; *io is the region's AVIOContext */
AVFormatContext *open_input(const char *path, struct fd_region *region, AVIOContext **io)
{
	return open_input_options(path, region, io, NULL);
}

/* the same with options of avformat_open_input() (e.g. "headers" of http), may be NULL */
AVFormatContext *open_input_options(const char *path, struct fd_region *region, AVIOContext **io, AVDictionary **options)
{
	AVFormatContext *format = avformat_alloc_context();

	*io = NULL;
	if (!path) {
		*io = avio_alloc_context(av_malloc(32768), 32768, 0, region, fd_region_read, NULL, fd_region_seek);
		format->pb = *io;
	}
	/* avformat_open_input() frees the context on failure */
	if (avformat_open_input(&format, path, NULL, options) < 0)
		return NULL;
	if (avformat_find_stream_info(format, NULL) < 0)
		avformat_close_input(&format);
	return format;
}

/* format may be NULL (open failed), the region's io is freed anyway */
void close_input(AVFormatContext *format, AVIOContext *io)
{
	avformat_close_input(&format);
	if (io) {
		av_freep(&io->buffer);
		avio_context_free(&io);
	}
}
