#ifndef SWS_CACHE_H
#define SWS_CACHE_H

#include <libavutil/frame.h>
#include <libswscale/swscale.h>

/* a scaler for one conversion, made again only when the conversion changes. sws_getCachedContext() cannot be used:
 * the scaler stores a full range format (AV_PIX_FMT_YUVJ*, e.g. of a decoded MJPEG frame or a full range H.264
 * stream) as the limited range one plus the range, so the cached scaler never matched and was made anew for every
 * frame (FFmpeg 8 sws_init_context()) */
struct sws_cache {
	struct SwsContext *ctx;
	int src_w, src_h, src_format, dst_w, dst_h, dst_format, flags;
};

struct SwsContext *sws_cached(struct sws_cache *c, const AVFrame *src, int dst_w, int dst_h, enum AVPixelFormat dst_format, int flags);

#endif
