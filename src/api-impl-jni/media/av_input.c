#include <errno.h>
#include <unistd.h>

#include <glib.h>

#include "av_input.h"

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

/* opens the file at path, or the fd region if path is NULL; *io is the region's AVIOContext */
AVFormatContext *open_input(const char *path, struct fd_region *region, AVIOContext **io)
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
void close_input(AVFormatContext *format, AVIOContext *io)
{
	avformat_close_input(&format);
	if (io) {
		av_freep(&io->buffer);
		avio_context_free(&io);
	}
}
