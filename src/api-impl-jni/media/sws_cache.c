#include "sws_cache.h"

struct SwsContext *sws_cached(struct sws_cache *c, const AVFrame *src, int dst_w, int dst_h, enum AVPixelFormat dst_format, int flags)
{
	if (c->ctx && c->src_w == src->width && c->src_h == src->height && c->src_format == src->format &&
	    c->dst_w == dst_w && c->dst_h == dst_h && c->dst_format == dst_format && c->flags == flags)
		return c->ctx;
	sws_freeContext(c->ctx);
	c->ctx = sws_getContext(src->width, src->height, src->format, dst_w, dst_h, dst_format, flags, NULL, NULL, NULL);
	*c = (struct sws_cache){c->ctx, src->width, src->height, src->format, dst_w, dst_h, dst_format, flags};
	return c->ctx;
}
