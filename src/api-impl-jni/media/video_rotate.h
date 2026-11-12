#ifndef VIDEO_ROTATE_H
#define VIDEO_ROTATE_H

#include <stdint.h>

#include <glib.h>

/* a plane of width x height pixels of bpp bytes, turned clockwise by degrees (0, 90, 180, 270), into dst (the size
 * turned, packed) */
void rotate_plane(guchar *dst, const uint8_t *src, int src_stride, int width, int height, int bpp, int degrees);

#endif
