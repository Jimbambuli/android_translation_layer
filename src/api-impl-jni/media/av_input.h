#ifndef AV_INPUT_H
#define AV_INPUT_H

#include <stdint.h>

#include <libavformat/avformat.h>

/* a region of a file descriptor, like setDataSource(FileDescriptor, long, long) */
struct fd_region {
	int fd;
	int64_t offset;
	int64_t length;
	int64_t position;
};

/* opens the file at path, or the fd region if path is NULL; *io is the region's AVIOContext */
AVFormatContext *open_input(const char *path, struct fd_region *region, AVIOContext **io);
/* format may be NULL (open failed), the region's io is freed anyway */
void close_input(AVFormatContext *format, AVIOContext *io);

#endif
