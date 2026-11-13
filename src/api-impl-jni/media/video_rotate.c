#include <string.h>

#include <glib.h>

#include "video_rotate.h"

/* a plane of width x height pixels of bpp bytes, turned clockwise by degrees, into dst (the size turned, packed); in
 * tiles of 32x32 pixels, so that reading and writing stay in the cache */
void rotate_plane(guchar *dst, const uint8_t *src, int src_stride, int width, int height, int bpp, int degrees)
{
	int dst_width = (degrees == 90 || degrees == 270) ? height : width;
	for (int ty = 0; ty < height; ty += 32)
		for (int tx = 0; tx < width; tx += 32)
			for (int y = ty; y < MIN(ty + 32, height); y++) {
				const uint8_t *row = src + (size_t)y * src_stride;
				for (int x = tx; x < MIN(tx + 32, width); x++) {
					int dx, dy;
					switch (degrees) {
						case 90:
							dx = height - 1 - y;
							dy = x;
							break;
						case 180:
							dx = width - 1 - x;
							dy = height - 1 - y;
							break;
						case 270:
							dx = y;
							dy = width - 1 - x;
							break;
						default:
							dx = x;
							dy = y;
							break;
					}
					guchar *d = dst + ((size_t)dy * dst_width + dx) * bpp;
					if (bpp == 1)
						*d = row[x];
					else
						memcpy(d, row + (size_t)x * bpp, bpp);
				}
			}
}
