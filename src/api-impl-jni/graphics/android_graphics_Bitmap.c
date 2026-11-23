#include <cairo.h>
#include <gtk/gtk.h>
#include <stdio.h>
#include <webp/encode.h>

#include "../defines.h"
#include "../util.h"
#include "gdk/gdk.h"
#include "glib.h"

#include "../generated_headers/android_graphics_Bitmap.h"

/* The pixels of a Bitmap that is drawn into or has its pixels changed are a cairo image surface (Bitmap.java):
 * ARGB32 (premultiplied, Skia's kPremul N32), RGB24 for the opaque RGB_565, A8 for ALPHA_8. cairo works on such a
 * surface from any thread, one thread at a time (the bitmap's lock); GTK only sees a GdkTexture made from a copy. */

extern GThread *main_thread_id;

/* the GDK memory format with the layout of the surface's pixels */
static GdkMemoryFormat surface_memory_format(cairo_surface_t *surface)
{
	switch (cairo_image_surface_get_format(surface)) {
		case CAIRO_FORMAT_A8:
			return GDK_MEMORY_A8;
		case CAIRO_FORMAT_RGB24:
			return G_BYTE_ORDER == G_LITTLE_ENDIAN ? GDK_MEMORY_B8G8R8X8 : GDK_MEMORY_X8R8G8B8;
		default:
			return G_BYTE_ORDER == G_LITTLE_ENDIAN ? GDK_MEMORY_B8G8R8A8_PREMULTIPLIED : GDK_MEMORY_A8R8G8B8_PREMULTIPLIED;
	}
}

/* the surface's pixels as a GdkMemoryTexture without a copy, for GDK's format conversions (a GdkTexture is
 * immutable and may be made and read in any thread); the caller keeps the bitmap's lock while it uses it */
static GdkTexture *wrap_surface(cairo_surface_t *surface)
{
	cairo_surface_flush(surface);
	int height = cairo_image_surface_get_height(surface);
	int stride = cairo_image_surface_get_stride(surface);
	GBytes *bytes = g_bytes_new_with_free_func(cairo_image_surface_get_data(surface), (gsize)height * stride,
	                                           (GDestroyNotify)cairo_surface_destroy, cairo_surface_reference(surface));
	GdkTexture *texture = gdk_memory_texture_new(cairo_image_surface_get_width(surface), height, surface_memory_format(surface), bytes, stride);
	g_bytes_unref(bytes);
	return texture;
}

/* The surface of a bitmap of the given size: transparent (black for RGB24, as Skia allocates zeroed pixels), or the
 * pixels of the texture (a decoded image). Only a GdkMemoryTexture is downloaded off the main thread: GDK downloads
 * a GL texture in its GL context on the main thread and waits for it (gdk_gl_texture_run()), which can deadlock
 * when the main thread waits for this one; such a bitmap gets transparent pixels there. */
JNIEXPORT jlong JNICALL Java_android_graphics_Bitmap_native_1create_1surface(JNIEnv *env, jclass class, jlong texture_ptr, jint width, jint height, jint format)
{
	cairo_surface_t *surface = cairo_image_surface_create(format, width, height);
	if (cairo_surface_status(surface) != CAIRO_STATUS_SUCCESS)
		return _INTPTR(surface); // the error surface draws nothing
	GdkTexture *texture = texture_ptr ? GDK_TEXTURE(_PTR(texture_ptr)) : NULL;
	if (texture && !GDK_IS_MEMORY_TEXTURE(texture) && main_thread_id && g_thread_self() != main_thread_id) {
		static gint warned;
		if (g_atomic_int_compare_and_exchange(&warned, 0, 1))
			fprintf(stderr, "Bitmap: the pixels of a %s cannot be read outside the main thread\n", G_OBJECT_TYPE_NAME(texture));
		texture = NULL;
	}
	if (texture) {
		GdkTextureDownloader *downloader = gdk_texture_downloader_new(texture);
		gdk_texture_downloader_set_format(downloader, surface_memory_format(surface));
		gdk_texture_downloader_download_into(downloader, cairo_image_surface_get_data(surface), cairo_image_surface_get_stride(surface));
		gdk_texture_downloader_free(downloader);
		cairo_surface_mark_dirty(surface);
	}
	return _INTPTR(surface);
}

/* the texture GTK draws the bitmap with: a copy of the surface's pixels */
JNIEXPORT jlong JNICALL Java_android_graphics_Bitmap_native_1texture_1from_1surface(JNIEnv *env, jclass class, jlong surface_ptr)
{
	cairo_surface_t *surface = _PTR(surface_ptr);
	cairo_surface_flush(surface);
	int height = cairo_image_surface_get_height(surface);
	int stride = cairo_image_surface_get_stride(surface);
	GBytes *bytes = g_bytes_new(cairo_image_surface_get_data(surface), (gsize)height * stride);
	GdkTexture *texture = gdk_memory_texture_new(cairo_image_surface_get_width(surface), height, surface_memory_format(surface), bytes, stride);
	g_bytes_unref(bytes);
	return _INTPTR(texture);
}

/* a copy of the surface in the format of another config (cairo converts: A8 keeps the alpha, RGB24 drops it) */
JNIEXPORT jlong JNICALL Java_android_graphics_Bitmap_native_1copy_1surface(JNIEnv *env, jclass class, jlong surface_ptr, jint format)
{
	cairo_surface_t *src = _PTR(surface_ptr);
	cairo_surface_t *surface = cairo_image_surface_create(format, cairo_image_surface_get_width(src), cairo_image_surface_get_height(src));
	cairo_t *cr = cairo_create(surface);
	cairo_set_operator(cr, CAIRO_OPERATOR_SOURCE);
	cairo_set_source_surface(cr, src, 0, 0);
	cairo_paint(cr);
	cairo_destroy(cr);
	return _INTPTR(surface);
}

JNIEXPORT jint JNICALL Java_android_graphics_Bitmap_native_1get_1width(JNIEnv *env, jclass class, jlong texture_ptr)
{
	return gdk_texture_get_width(GDK_TEXTURE(_PTR(texture_ptr)));
}

JNIEXPORT jint JNICALL Java_android_graphics_Bitmap_native_1get_1height(JNIEnv *env, jclass class, jlong texture_ptr)
{
	return gdk_texture_get_height(GDK_TEXTURE(_PTR(texture_ptr)));
}

/* Skia's SkMulDiv255Round() */
static inline uint32_t premultiply(uint32_t c, uint32_t a)
{
	uint32_t prod = c * a + 128;
	return (prod + (prod >> 8)) >> 8;
}

/* Skia's SkUnPreMultiply (as Bitmap.unpremultiply()) */
static inline uint32_t unpremultiply(uint32_t c, uint32_t a)
{
	return a == 0 ? 0 : a == 255 ? c : MIN(255, (c * 255 + a / 2) / a);
}

/* an unpremultiplied 0xAARRGGBB color as the pixel of the surface's format */
static inline uint32_t color_to_pixel(uint32_t color, cairo_format_t format)
{
	uint32_t a = color >> 24;
	uint32_t pixel = premultiply((color >> 16) & 0xff, a) << 16 | premultiply((color >> 8) & 0xff, a) << 8 | premultiply(color & 0xff, a);
	return format == CAIRO_FORMAT_RGB24 ? 0xff000000 | pixel : a << 24 | pixel; // RGB24: as drawn over black
}

static inline uint32_t pixel_to_color(uint32_t pixel, cairo_format_t format)
{
	if (format == CAIRO_FORMAT_RGB24)
		return 0xff000000 | pixel;
	uint32_t a = pixel >> 24;
	return a << 24 | unpremultiply((pixel >> 16) & 0xff, a) << 16 | unpremultiply((pixel >> 8) & 0xff, a) << 8 | unpremultiply(pixel & 0xff, a);
}

/* SkBitmap::eraseColor(): every pixel set to the color, no blending */
JNIEXPORT void JNICALL Java_android_graphics_Bitmap_native_1erase_1color(JNIEnv *env, jclass class, jlong surface_ptr, jint color)
{
	cairo_surface_t *surface = _PTR(surface_ptr);
	cairo_format_t format = cairo_image_surface_get_format(surface);
	int width = cairo_image_surface_get_width(surface), height = cairo_image_surface_get_height(surface);
	int stride = cairo_image_surface_get_stride(surface);
	unsigned char *data = cairo_image_surface_get_data(surface);
	cairo_surface_flush(surface);
	if (format == CAIRO_FORMAT_A8) {
		memset(data, (uint32_t)color >> 24, (size_t)stride * height);
	} else {
		uint32_t pixel = color_to_pixel(color, format);
		for (int y = 0; y < height; y++) {
			uint32_t *row = (uint32_t *)(data + (size_t)y * stride);
			for (int x = 0; x < width; x++)
				row[x] = pixel;
		}
	}
	cairo_surface_mark_dirty(surface);
}

JNIEXPORT void JNICALL Java_android_graphics_Bitmap_native_1recycle(JNIEnv *env, jclass class, jlong texture_ptr, jlong surface_ptr)
{
	if (texture_ptr)
		g_object_unref(GDK_TEXTURE(_PTR(texture_ptr)));
	if (surface_ptr)
		cairo_surface_destroy(_PTR(surface_ptr));
}

/* whether the texture has no alpha channel, as Skia's kOpaque_SkAlphaType of an encoded image */
JNIEXPORT jboolean JNICALL Java_android_graphics_Bitmap_native_1is_1opaque(JNIEnv *env, jclass class, jlong texture_ptr)
{
	switch (gdk_texture_get_format(GDK_TEXTURE(_PTR(texture_ptr)))) {
		case GDK_MEMORY_R8G8B8:
		case GDK_MEMORY_B8G8R8:
		case GDK_MEMORY_R16G16B16:
		case GDK_MEMORY_R16G16B16_FLOAT:
		case GDK_MEMORY_R32G32B32_FLOAT:
		case GDK_MEMORY_G8:
		case GDK_MEMORY_G16:
		case GDK_MEMORY_B8G8R8X8:
		case GDK_MEMORY_X8R8G8B8:
		case GDK_MEMORY_R8G8B8X8:
		case GDK_MEMORY_X8B8G8R8:
			return JNI_TRUE;
		default:
			return JNI_FALSE;
	}
}

/* hwui Bitmap_getPixels(): the area as unpremultiplied 0xAARRGGBB colors, row by row with the stride (which may be
 * negative) from pixels[offset] on; Bitmap.java has checked the bounds */
JNIEXPORT void JNICALL Java_android_graphics_Bitmap_native_1get_1pixels(JNIEnv *env, jclass class, jlong surface_ptr, jintArray pixels, jint offset, jint stride, jint x, jint y, jint width, jint height)
{
	cairo_surface_t *surface = _PTR(surface_ptr);
	cairo_format_t format = cairo_image_surface_get_format(surface);
	int surface_stride = cairo_image_surface_get_stride(surface);
	const unsigned char *data = cairo_image_surface_get_data(surface);
	cairo_surface_flush(surface);
	jint *array = (*env)->GetIntArrayElements(env, pixels, NULL);
	for (int row = 0; row < height; row++) {
		const unsigned char *src = data + (size_t)(y + row) * surface_stride;
		jint *dst = array + offset + row * stride;
		for (int col = 0; col < width; col++)
			dst[col] = format == CAIRO_FORMAT_A8 ? (uint32_t)src[x + col] << 24 : pixel_to_color(((const uint32_t *)src)[x + col], format);
	}
	(*env)->ReleaseIntArrayElements(env, pixels, array, 0);
}

/* hwui Bitmap_setPixels(): the unpremultiplied colors written into the area, premultiplied (no blending) */
JNIEXPORT void JNICALL Java_android_graphics_Bitmap_native_1set_1pixels(JNIEnv *env, jclass class, jlong surface_ptr, jintArray pixels, jint offset, jint stride, jint x, jint y, jint width, jint height)
{
	cairo_surface_t *surface = _PTR(surface_ptr);
	cairo_format_t format = cairo_image_surface_get_format(surface);
	int surface_stride = cairo_image_surface_get_stride(surface);
	unsigned char *data = cairo_image_surface_get_data(surface);
	cairo_surface_flush(surface);
	jint *array = (*env)->GetIntArrayElements(env, pixels, NULL);
	for (int row = 0; row < height; row++) {
		unsigned char *dst = data + (size_t)(y + row) * surface_stride;
		const jint *src = array + offset + row * stride;
		for (int col = 0; col < width; col++) {
			if (format == CAIRO_FORMAT_A8)
				dst[x + col] = (uint32_t)src[col] >> 24;
			else
				((uint32_t *)dst)[x + col] = color_to_pixel(src[col], format);
		}
	}
	(*env)->ReleaseIntArrayElements(env, pixels, array, JNI_ABORT);
	cairo_surface_mark_dirty(surface);
}

JNIEXPORT void JNICALL Java_android_graphics_Bitmap_native_1copy_1to_1buffer(JNIEnv *env, jclass class, jlong surface_ptr, jobject buffer, jint memory_format, jint stride)
{
	GdkTexture *texture = wrap_surface(_PTR(surface_ptr));
	GdkTextureDownloader *downloader = gdk_texture_downloader_new(texture);
	gdk_texture_downloader_set_format(downloader, memory_format);
	jarray array_ref;
	jbyte *array;
	guchar *data = get_nio_buffer(env, buffer, &array_ref, &array);
	gdk_texture_downloader_download_into(downloader, data, stride);
	release_nio_buffer(env, array_ref, array);
	gdk_texture_downloader_free(downloader);
	g_object_unref(texture);
}

static GBytes *download(GdkTexture *texture, GdkMemoryFormat format, gsize *stride)
{
	GdkTextureDownloader *downloader = gdk_texture_downloader_new(texture);
	gdk_texture_downloader_set_format(downloader, format);
	GBytes *pixels = gdk_texture_downloader_download_bytes(downloader, stride);
	gdk_texture_downloader_free(downloader);
	return pixels;
}

static GBytes *encode_jpeg(GdkTexture *texture, int quality)
{
	gsize stride;
	/* JPEG has no alpha channel */
	GBytes *pixels = download(texture, GDK_MEMORY_R8G8B8, &stride);
	GdkPixbuf *pixbuf = gdk_pixbuf_new_from_bytes(pixels, GDK_COLORSPACE_RGB, FALSE, 8, gdk_texture_get_width(texture), gdk_texture_get_height(texture), stride);
	g_bytes_unref(pixels);

	char quality_str[4];
	snprintf(quality_str, sizeof(quality_str), "%d", quality);
	gchar *buffer = NULL;
	gsize size = 0;
	GBytes *result = NULL;
	if (gdk_pixbuf_save_to_buffer(pixbuf, &buffer, &size, "jpeg", NULL, "quality", quality_str, NULL))
		result = g_bytes_new_take(buffer, size);
	g_object_unref(pixbuf);
	return result;
}

/* like Skia's SkWebpEncoder::Encode(): unpremultiplied RGBA, method 3 */
static GBytes *encode_webp(GdkTexture *texture, int quality, bool lossless)
{
	WebPConfig config;
	WebPPicture picture;
	WebPMemoryWriter writer;
	GBytes *result = NULL;

	if (!WebPConfigInit(&config) || !WebPPictureInit(&picture))
		return NULL;
	config.lossless = lossless;
	config.quality = quality;
	config.method = 3;
	picture.use_argb = lossless;
	picture.width = gdk_texture_get_width(texture);
	picture.height = gdk_texture_get_height(texture);

	gsize stride;
	GBytes *pixels = download(texture, GDK_MEMORY_R8G8B8A8, &stride);
	WebPMemoryWriterInit(&writer);
	picture.writer = WebPMemoryWrite;
	picture.custom_ptr = &writer;
	if (WebPPictureImportRGBA(&picture, g_bytes_get_data(pixels, NULL), stride) && WebPEncode(&config, &picture))
		result = g_bytes_new_with_free_func(writer.mem, writer.size, WebPFree, writer.mem);
	else
		WebPMemoryWriterClear(&writer);
	WebPPictureFree(&picture);
	g_bytes_unref(pixels);
	return result;
}

/* the formats of Bitmap.CompressFormat, as in hwui's Bitmap::compress() */
enum {
	COMPRESS_JPEG = 0,
	COMPRESS_PNG = 1,
	COMPRESS_WEBP = 2,
	COMPRESS_WEBP_LOSSY = 3,
	COMPRESS_WEBP_LOSSLESS = 4,
};

static jbyteArray compress(JNIEnv *env, GdkTexture *texture, jint format, jint quality)
{
	GBytes *bytes = NULL;
	switch (format) {
		case COMPRESS_JPEG:
			bytes = encode_jpeg(texture, quality);
			break;
		case COMPRESS_PNG:
			bytes = gdk_texture_save_to_png_bytes(texture);
			break;
		case COMPRESS_WEBP:
			/* SkEncodeImage(): quality 100 is lossless (with quality 75), anything else lossy */
			bytes = quality == 100 ? encode_webp(texture, 75, true) : encode_webp(texture, quality, false);
			break;
		case COMPRESS_WEBP_LOSSY:
		case COMPRESS_WEBP_LOSSLESS:
			bytes = encode_webp(texture, quality, format == COMPRESS_WEBP_LOSSLESS);
			break;
	}
	if (!bytes)
		return NULL;
	gsize size;
	gconstpointer data = g_bytes_get_data(bytes, &size);
	jbyteArray result = (*env)->NewByteArray(env, size);
	(*env)->SetByteArrayRegion(env, result, 0, size, data);
	g_bytes_unref(bytes);
	return result;
}

/* for the NDK's AndroidBitmap_compress(), which has its pixels as a texture */
JNIEXPORT jbyteArray JNICALL Java_android_graphics_Bitmap_native_1compress(JNIEnv *env, jclass class, jlong texture_ptr, jint format, jint quality)
{
	return compress(env, GDK_TEXTURE(_PTR(texture_ptr)), format, quality);
}

JNIEXPORT jbyteArray JNICALL Java_android_graphics_Bitmap_native_1compress_1surface(JNIEnv *env, jclass class, jlong surface_ptr, jint format, jint quality)
{
	GdkTexture *texture = wrap_surface(_PTR(surface_ptr));
	jbyteArray result = compress(env, texture, format, quality);
	g_object_unref(texture);
	return result;
}
