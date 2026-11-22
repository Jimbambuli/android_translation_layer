/*
 * AImageDecoder (android/imagedecoder.h, libjnigraphics) on gdk-pixbuf, following AOSP's
 * frameworks/base/native/graphics/jni/imagedecoder.cpp: header info, output format (RGBA_8888, RGB_565 for opaque
 * images, RGBA_F16), premultiplied or not, target size, sampled size, crop, and the frames of animated images.
 * Differences, because gdk-pixbuf decodes differently than Skia:
 * - colours stay in the image's sRGB; another ADataSpace cannot be converted to and is refused (BAD_PARAMETER);
 * - frames of an animation come composited, so every frame covers the whole image (dispose NONE, blend SRC);
 * - gdk-pixbuf knows no repetition count: animated images report ANDROID_IMAGE_DECODER_INFINITE, and an animation
 *   is finished when it is back at its first frame.
 */

#include <errno.h>
#include <gdk-pixbuf/gdk-pixbuf.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "bitmap.h"

enum {
	ANDROID_IMAGE_DECODER_SUCCESS = 0,
	ANDROID_IMAGE_DECODER_INCOMPLETE = -1,
	ANDROID_IMAGE_DECODER_ERROR = -2,
	ANDROID_IMAGE_DECODER_INVALID_CONVERSION = -3,
	ANDROID_IMAGE_DECODER_INVALID_SCALE = -4,
	ANDROID_IMAGE_DECODER_BAD_PARAMETER = -5,
	ANDROID_IMAGE_DECODER_INVALID_INPUT = -6,
	ANDROID_IMAGE_DECODER_SEEK_ERROR = -7,
	ANDROID_IMAGE_DECODER_INTERNAL_ERROR = -8,
	ANDROID_IMAGE_DECODER_UNSUPPORTED_FORMAT = -9,
	ANDROID_IMAGE_DECODER_FINISHED = -10,
	ANDROID_IMAGE_DECODER_INVALID_STATE = -11,
};
#define ANDROID_IMAGE_DECODER_INFINITE INT32_MAX
#define ANDROID_IMAGE_DECODER_DISPOSE_OP_NONE 1
#define ANDROID_IMAGE_DECODER_BLEND_OP_SRC 1
#define ANDROID_BITMAP_FLAGS_ALPHA_PREMUL 0
#define ANDROID_BITMAP_FLAGS_ALPHA_OPAQUE 1
#define ADATASPACE_SRGB 142671872
#define ADATASPACE_SCRGB 411107328

typedef struct {
	int32_t left, top, right, bottom;
} ARect;

typedef struct AImageDecoder {
	GdkPixbufAnimation *animation;
	GdkPixbufAnimationIter *iter;
	GdkPixbuf *first_frame; /* the composited first frame, to see when an animation starts over */
	GTimeVal time;
	int32_t frame_index;
	bool finished;
	char *mime_type;
	int32_t width, height;
	bool opaque;
	int32_t format;
	bool unpremultiplied;
	int32_t target_width, target_height;
	ARect crop; /* empty: none */
} AImageDecoder;
typedef AImageDecoder AImageDecoderHeaderInfo;

typedef struct AImageDecoderFrameInfo {
	int64_t duration;
	ARect rect;
	bool has_alpha;
} AImageDecoderFrameInfo;

struct AAsset;
int AAsset_read(struct AAsset *asset, void *buf, size_t count);

/* AOSP's getMimeType() names */
static char *android_mime_type(GdkPixbufFormat *format)
{
	const char *name = gdk_pixbuf_format_get_name(format);
	static const char *const names[][2] = {
		{"png", "image/png"}, {"jpeg", "image/jpeg"}, {"gif", "image/gif"}, {"bmp", "image/bmp"},
		{"ico", "image/x-ico"}, {"webp", "image/webp"}, {"heif", "image/heif"}, {"avif", "image/avif"},
	};
	for (size_t i = 0; i < G_N_ELEMENTS(names); i++)
		if (!g_strcmp0(name, names[i][0]))
			return g_strdup(names[i][1]);
	gchar **types = gdk_pixbuf_format_get_mime_types(format);
	char *type = g_strdup(types && types[0] ? types[0] : "");
	g_strfreev(types);
	return type;
}

static int create(GBytes *bytes, AImageDecoder **out)
{
	if (!out)
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	*out = NULL;
	GdkPixbufLoader *loader = gdk_pixbuf_loader_new();
	gsize length;
	const guchar *data = g_bytes_get_data(bytes, &length);
	bool ok = length && gdk_pixbuf_loader_write(loader, data, length, NULL);
	ok = gdk_pixbuf_loader_close(loader, NULL) && ok;
	g_bytes_unref(bytes);
	GdkPixbufAnimation *animation = ok ? gdk_pixbuf_loader_get_animation(loader) : NULL;
	if (!animation) {
		g_object_unref(loader);
		return length ? ANDROID_IMAGE_DECODER_UNSUPPORTED_FORMAT : ANDROID_IMAGE_DECODER_INVALID_INPUT;
	}
	AImageDecoder *decoder = g_new0(AImageDecoder, 1);
	decoder->animation = g_object_ref(animation);
	decoder->mime_type = android_mime_type(gdk_pixbuf_loader_get_format(loader));
	g_object_unref(loader);
	decoder->width = gdk_pixbuf_animation_get_width(animation);
	decoder->height = gdk_pixbuf_animation_get_height(animation);
	decoder->time.tv_sec = 0;
	decoder->time.tv_usec = 0;
	G_GNUC_BEGIN_IGNORE_DEPRECATIONS
	decoder->iter = gdk_pixbuf_animation_get_iter(animation, &decoder->time);
	G_GNUC_END_IGNORE_DEPRECATIONS
	decoder->first_frame = gdk_pixbuf_animation_iter_get_pixbuf(decoder->iter);
	decoder->opaque = !gdk_pixbuf_get_has_alpha(decoder->first_frame);
	decoder->format = ANDROID_BITMAP_FORMAT_RGBA_8888;
	decoder->target_width = decoder->width;
	decoder->target_height = decoder->height;
	*out = decoder;
	return ANDROID_IMAGE_DECODER_SUCCESS;
}

/* like AOSP, the encoded data is read from the current position of the asset or file */
int AImageDecoder_createFromAAsset(struct AAsset *asset, AImageDecoder **outDecoder)
{
	if (!asset)
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	GByteArray *data = g_byte_array_new();
	guint8 chunk[65536];
	int n;
	while ((n = AAsset_read(asset, chunk, sizeof(chunk))) > 0)
		g_byte_array_append(data, chunk, n);
	return create(g_byte_array_free_to_bytes(data), outDecoder);
}

int AImageDecoder_createFromFd(int fd, AImageDecoder **outDecoder)
{
	if (fd <= 0)
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	GByteArray *data = g_byte_array_new();
	guint8 chunk[65536];
	ssize_t n;
	while ((n = read(fd, chunk, sizeof(chunk))) > 0 || (n < 0 && errno == EINTR))
		if (n > 0)
			g_byte_array_append(data, chunk, n);
	return create(g_byte_array_free_to_bytes(data), outDecoder);
}

int AImageDecoder_createFromBuffer(const void *buffer, size_t length, AImageDecoder **outDecoder)
{
	if (!buffer || !length)
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	return create(g_bytes_new(buffer, length), outDecoder);
}

void AImageDecoder_delete(AImageDecoder *decoder)
{
	if (!decoder)
		return;
	g_object_unref(decoder->iter);
	g_object_unref(decoder->animation);
	g_free(decoder->mime_type);
	g_free(decoder);
}

const char *AImageDecoder_resultToString(int result)
{
	switch (result) {
		case ANDROID_IMAGE_DECODER_SUCCESS: return "ANDROID_IMAGE_DECODER_SUCCESS";
		case ANDROID_IMAGE_DECODER_INCOMPLETE: return "ANDROID_IMAGE_DECODER_INCOMPLETE";
		case ANDROID_IMAGE_DECODER_ERROR: return "ANDROID_IMAGE_DECODER_ERROR";
		case ANDROID_IMAGE_DECODER_INVALID_CONVERSION: return "ANDROID_IMAGE_DECODER_INVALID_CONVERSION";
		case ANDROID_IMAGE_DECODER_INVALID_SCALE: return "ANDROID_IMAGE_DECODER_INVALID_SCALE";
		case ANDROID_IMAGE_DECODER_BAD_PARAMETER: return "ANDROID_IMAGE_DECODER_BAD_PARAMETER";
		case ANDROID_IMAGE_DECODER_INVALID_INPUT: return "ANDROID_IMAGE_DECODER_INVALID_INPUT";
		case ANDROID_IMAGE_DECODER_SEEK_ERROR: return "ANDROID_IMAGE_DECODER_SEEK_ERROR";
		case ANDROID_IMAGE_DECODER_INTERNAL_ERROR: return "ANDROID_IMAGE_DECODER_INTERNAL_ERROR";
		case ANDROID_IMAGE_DECODER_UNSUPPORTED_FORMAT: return "ANDROID_IMAGE_DECODER_UNSUPPORTED_FORMAT";
		case ANDROID_IMAGE_DECODER_FINISHED: return "ANDROID_IMAGE_DECODER_FINISHED";
		case ANDROID_IMAGE_DECODER_INVALID_STATE: return "ANDROID_IMAGE_DECODER_INVALID_STATE";
		default: return NULL;
	}
}

/* settings are only allowed before the first frame is passed */
#define CHECK_SETTABLE(decoder)                                \
	do {                                                       \
		if (!(decoder))                                        \
			return ANDROID_IMAGE_DECODER_BAD_PARAMETER;        \
		if ((decoder)->frame_index != 0)                       \
			return ANDROID_IMAGE_DECODER_INVALID_STATE;        \
	} while (0)

int AImageDecoder_setAndroidBitmapFormat(AImageDecoder *decoder, int32_t format)
{
	CHECK_SETTABLE(decoder);
	switch (format) {
		case ANDROID_BITMAP_FORMAT_RGBA_8888:
		case ANDROID_BITMAP_FORMAT_RGBA_F16:
			break;
		case ANDROID_BITMAP_FORMAT_RGB_565: /* only for opaque images, as with Skia */
			if (!decoder->opaque)
				return ANDROID_IMAGE_DECODER_INVALID_CONVERSION;
			break;
		default:
			return ANDROID_IMAGE_DECODER_INVALID_CONVERSION;
	}
	decoder->format = format;
	return ANDROID_IMAGE_DECODER_SUCCESS;
}

int AImageDecoder_setUnpremultipliedRequired(AImageDecoder *decoder, bool required)
{
	CHECK_SETTABLE(decoder);
	decoder->unpremultiplied = required;
	return ANDROID_IMAGE_DECODER_SUCCESS;
}

int AImageDecoder_setDataSpace(AImageDecoder *decoder, int32_t dataspace)
{
	if (!decoder || (dataspace != ADATASPACE_SRGB && dataspace != ADATASPACE_SCRGB))
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	CHECK_SETTABLE(decoder);
	return ANDROID_IMAGE_DECODER_SUCCESS;
}

static bool crop_is_empty(ARect r)
{
	return !r.left && !r.top && !r.right && !r.bottom;
}

int AImageDecoder_setTargetSize(AImageDecoder *decoder, int32_t width, int32_t height)
{
	CHECK_SETTABLE(decoder);
	if (width <= 0 || height <= 0)
		return ANDROID_IMAGE_DECODER_INVALID_SCALE;
	/* a crop must stay within the target size */
	if (!crop_is_empty(decoder->crop) && (decoder->crop.right > width || decoder->crop.bottom > height))
		return ANDROID_IMAGE_DECODER_INVALID_SCALE;
	decoder->target_width = width;
	decoder->target_height = height;
	return ANDROID_IMAGE_DECODER_SUCCESS;
}

/* Skia's get_scaled_dimension() */
static int32_t sampled(int32_t dimension, int sample_size)
{
	return sample_size > dimension ? 1 : dimension / sample_size;
}

int AImageDecoder_computeSampledSize(const AImageDecoder *decoder, int sampleSize, int32_t *width, int32_t *height)
{
	if (!decoder || !width || !height || sampleSize < 1)
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	*width = sampled(decoder->width, sampleSize);
	*height = sampled(decoder->height, sampleSize);
	return ANDROID_IMAGE_DECODER_SUCCESS;
}

int AImageDecoder_setCrop(AImageDecoder *decoder, ARect crop)
{
	CHECK_SETTABLE(decoder);
	if (!crop_is_empty(crop) &&
	    (crop.left < 0 || crop.top < 0 || crop.left >= crop.right || crop.top >= crop.bottom ||
	     crop.right > decoder->target_width || crop.bottom > decoder->target_height))
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	decoder->crop = crop;
	return ANDROID_IMAGE_DECODER_SUCCESS;
}

const AImageDecoderHeaderInfo *AImageDecoder_getHeaderInfo(const AImageDecoder *decoder)
{
	return decoder;
}

int32_t AImageDecoderHeaderInfo_getWidth(const AImageDecoderHeaderInfo *info)
{
	return info ? info->width : 0;
}

int32_t AImageDecoderHeaderInfo_getHeight(const AImageDecoderHeaderInfo *info)
{
	return info ? info->height : 0;
}

const char *AImageDecoderHeaderInfo_getMimeType(const AImageDecoderHeaderInfo *info)
{
	return info ? info->mime_type : NULL;
}

int32_t AImageDecoderHeaderInfo_getAndroidBitmapFormat(const AImageDecoderHeaderInfo *info)
{
	return info ? ANDROID_BITMAP_FORMAT_RGBA_8888 : ANDROID_BITMAP_FORMAT_NONE;
}

int AImageDecoderHeaderInfo_getAlphaFlags(const AImageDecoderHeaderInfo *info)
{
	if (!info)
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	return info->opaque ? ANDROID_BITMAP_FLAGS_ALPHA_OPAQUE : ANDROID_BITMAP_FLAGS_ALPHA_PREMUL;
}

int32_t AImageDecoderHeaderInfo_getDataSpace(const AImageDecoderHeaderInfo *info)
{
	return info ? ADATASPACE_SRGB : ANDROID_IMAGE_DECODER_BAD_PARAMETER;
}

static int32_t bytes_per_pixel(int32_t format)
{
	return format == ANDROID_BITMAP_FORMAT_RGB_565 ? 2 : format == ANDROID_BITMAP_FORMAT_RGBA_F16 ? 8 : 4;
}

static void output_size(const AImageDecoder *decoder, int32_t *width, int32_t *height)
{
	*width = crop_is_empty(decoder->crop) ? decoder->target_width : decoder->crop.right - decoder->crop.left;
	*height = crop_is_empty(decoder->crop) ? decoder->target_height : decoder->crop.bottom - decoder->crop.top;
}

size_t AImageDecoder_getMinimumStride(AImageDecoder *decoder)
{
	if (!decoder)
		return 0;
	int32_t width, height;
	output_size(decoder, &width, &height);
	return (size_t)width * bytes_per_pixel(decoder->format);
}

static uint16_t to_half(float f)
{
	union { float f; uint32_t u; } v = { f };
	uint32_t sign = (v.u >> 16) & 0x8000, exponent = (v.u >> 23) & 0xff, mantissa = v.u & 0x7fffff;
	if (exponent < 113) /* zero or too small for a normal half (values here are 0..1) */
		return sign;
	return sign | ((exponent - 112) << 10) | (mantissa >> 13);
}

int AImageDecoder_decodeImage(AImageDecoder *decoder, void *pixels, size_t stride, size_t size)
{
	if (!decoder || !pixels || !stride)
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	int32_t width, height;
	output_size(decoder, &width, &height);
	size_t min_stride = AImageDecoder_getMinimumStride(decoder);
	if (stride < min_stride || stride % bytes_per_pixel(decoder->format) || size < stride * (height - 1) + min_stride)
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	if (decoder->finished)
		return ANDROID_IMAGE_DECODER_FINISHED;

	GdkPixbuf *frame = gdk_pixbuf_animation_iter_get_pixbuf(decoder->iter);
	GdkPixbuf *scaled = (decoder->target_width == decoder->width && decoder->target_height == decoder->height)
	                        ? g_object_ref(frame)
	                        : gdk_pixbuf_scale_simple(frame, decoder->target_width, decoder->target_height, GDK_INTERP_BILINEAR);
	if (!scaled)
		return ANDROID_IMAGE_DECODER_INTERNAL_ERROR;
	int32_t x0 = crop_is_empty(decoder->crop) ? 0 : decoder->crop.left;
	int32_t y0 = crop_is_empty(decoder->crop) ? 0 : decoder->crop.top;
	int channels = gdk_pixbuf_get_n_channels(scaled);
	int src_stride = gdk_pixbuf_get_rowstride(scaled);
	const guchar *src = gdk_pixbuf_read_pixels(scaled);
	bool has_alpha = gdk_pixbuf_get_has_alpha(scaled);

	for (int32_t y = 0; y < height; y++) {
		const guchar *s = src + (size_t)(y0 + y) * src_stride + (size_t)x0 * channels;
		uint8_t *d = (uint8_t *)pixels + (size_t)y * stride;
		for (int32_t x = 0; x < width; x++, s += channels) {
			uint32_t a = has_alpha ? s[3] : 255;
			uint32_t r = s[0], g = s[1], b = s[2];
			if (!decoder->unpremultiplied && a != 255) {
				r = (r * a + 127) / 255;
				g = (g * a + 127) / 255;
				b = (b * a + 127) / 255;
			}
			switch (decoder->format) {
				case ANDROID_BITMAP_FORMAT_RGB_565:
					((uint16_t *)d)[x] = (uint16_t)(((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3));
					break;
				case ANDROID_BITMAP_FORMAT_RGBA_F16: {
					uint16_t *h = (uint16_t *)d + (size_t)x * 4;
					h[0] = to_half(r / 255.0f);
					h[1] = to_half(g / 255.0f);
					h[2] = to_half(b / 255.0f);
					h[3] = to_half(a / 255.0f);
					break;
				}
				default:
					d[x * 4] = r;
					d[x * 4 + 1] = g;
					d[x * 4 + 2] = b;
					d[x * 4 + 3] = a;
			}
		}
	}
	g_object_unref(scaled);
	return ANDROID_IMAGE_DECODER_SUCCESS;
}

bool AImageDecoder_isAnimated(AImageDecoder *decoder)
{
	return decoder && !gdk_pixbuf_animation_is_static_image(decoder->animation);
}

int32_t AImageDecoder_getRepeatCount(AImageDecoder *decoder)
{
	if (!decoder)
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	return AImageDecoder_isAnimated(decoder) ? ANDROID_IMAGE_DECODER_INFINITE : 0;
}

int AImageDecoder_advanceFrame(AImageDecoder *decoder)
{
	if (!decoder)
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	if (!AImageDecoder_isAnimated(decoder))
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	if (decoder->format != ANDROID_BITMAP_FORMAT_RGBA_8888 && decoder->format != ANDROID_BITMAP_FORMAT_RGBA_F16)
		return ANDROID_IMAGE_DECODER_INVALID_STATE;
	if (decoder->finished)
		return ANDROID_IMAGE_DECODER_FINISHED;
	int delay = gdk_pixbuf_animation_iter_get_delay_time(decoder->iter);
	if (delay < 0) { /* the last frame shows forever */
		decoder->finished = true;
		return ANDROID_IMAGE_DECODER_FINISHED;
	}
	g_time_val_add(&decoder->time, (glong)delay * 1000);
	G_GNUC_BEGIN_IGNORE_DEPRECATIONS
	gdk_pixbuf_animation_iter_advance(decoder->iter, &decoder->time);
	G_GNUC_END_IGNORE_DEPRECATIONS
	if (gdk_pixbuf_animation_iter_get_pixbuf(decoder->iter) == decoder->first_frame) {
		decoder->finished = true;
		return ANDROID_IMAGE_DECODER_FINISHED;
	}
	decoder->frame_index++;
	return ANDROID_IMAGE_DECODER_SUCCESS;
}

int AImageDecoder_rewind(AImageDecoder *decoder)
{
	if (!decoder)
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	if (!AImageDecoder_isAnimated(decoder))
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	g_object_unref(decoder->iter);
	decoder->time.tv_sec = 0;
	decoder->time.tv_usec = 0;
	G_GNUC_BEGIN_IGNORE_DEPRECATIONS
	decoder->iter = gdk_pixbuf_animation_get_iter(decoder->animation, &decoder->time);
	G_GNUC_END_IGNORE_DEPRECATIONS
	decoder->first_frame = gdk_pixbuf_animation_iter_get_pixbuf(decoder->iter);
	decoder->frame_index = 0;
	decoder->finished = false;
	return ANDROID_IMAGE_DECODER_SUCCESS;
}

void AImageDecoder_setInternallyHandleDisposePrevious(AImageDecoder *decoder, bool handleInternally)
{
	/* frames come composited from gdk-pixbuf, there is no DISPOSE_OP_PREVIOUS to handle */
}

AImageDecoderFrameInfo *AImageDecoderFrameInfo_create(void)
{
	return g_new0(AImageDecoderFrameInfo, 1);
}

void AImageDecoderFrameInfo_delete(AImageDecoderFrameInfo *info)
{
	g_free(info);
}

int AImageDecoder_getFrameInfo(AImageDecoder *decoder, AImageDecoderFrameInfo *info)
{
	if (!decoder || !info)
		return ANDROID_IMAGE_DECODER_BAD_PARAMETER;
	if (decoder->finished)
		return ANDROID_IMAGE_DECODER_FINISHED;
	int delay = gdk_pixbuf_animation_iter_get_delay_time(decoder->iter);
	info->duration = AImageDecoder_isAnimated(decoder) && delay > 0 ? (int64_t)delay * 1000000 : 0;
	info->rect = (ARect){ 0, 0, decoder->width, decoder->height };
	info->has_alpha = gdk_pixbuf_get_has_alpha(gdk_pixbuf_animation_iter_get_pixbuf(decoder->iter));
	return ANDROID_IMAGE_DECODER_SUCCESS;
}

int64_t AImageDecoderFrameInfo_getDuration(const AImageDecoderFrameInfo *info)
{
	return info ? info->duration : ANDROID_IMAGE_DECODER_BAD_PARAMETER;
}

ARect AImageDecoderFrameInfo_getFrameRect(const AImageDecoderFrameInfo *info)
{
	return info ? info->rect : (ARect){ 0, 0, 0, 0 };
}

bool AImageDecoderFrameInfo_hasAlphaWithinBounds(const AImageDecoderFrameInfo *info)
{
	return info && info->has_alpha;
}

int32_t AImageDecoderFrameInfo_getDisposeOp(const AImageDecoderFrameInfo *info)
{
	return info ? ANDROID_IMAGE_DECODER_DISPOSE_OP_NONE : ANDROID_IMAGE_DECODER_BAD_PARAMETER;
}

int32_t AImageDecoderFrameInfo_getBlendOp(const AImageDecoderFrameInfo *info)
{
	return info ? ANDROID_IMAGE_DECODER_BLEND_OP_SRC : ANDROID_IMAGE_DECODER_BAD_PARAMETER;
}
