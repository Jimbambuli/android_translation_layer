#include <gdk/gdk.h>
#include <jni.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>

// FIXME: put the header in a common place
#include "../api-impl-jni/defines.h"
#include "../api-impl-jni/util.h"
#include "bitmap.h"
#include "ndk_jni.h"

#define ANDROID_BITMAP_RESULT_SUCCESS 0
#define ANDROID_BITMAP_FORMAT_RGB_565 4

/* RGB_565 and ARGB_4444 have no GDK memory format: the pixels are handed out in the Android layout (16 bit, native byte
 * order; 4444 is premultiplied, R in the top nibble) and converted from/to premultiplied R8G8B8A8 on lock/unlock */
static void to_16bit(const uint8_t *rgba, uint8_t *out, int32_t width, int32_t height, int32_t stride, int32_t android_format)
{
	for (int32_t y = 0; y < height; y++) {
		const uint8_t *s = rgba + (size_t)y * width * 4;
		uint16_t *d = (uint16_t *)(out + (size_t)y * stride);
		for (int32_t x = 0; x < width; x++, s += 4)
			d[x] = android_format == ANDROID_BITMAP_FORMAT_RGB_565
			           ? (uint16_t)(((s[0] >> 3) << 11) | ((s[1] >> 2) << 5) | (s[2] >> 3))
			           : (uint16_t)(((s[0] >> 4) << 12) | ((s[1] >> 4) << 8) | ((s[2] >> 4) << 4) | (s[3] >> 4));
	}
}

static GBytes *from_16bit(const uint8_t *in, int32_t width, int32_t height, int32_t stride, int32_t android_format)
{
	uint8_t *rgba = g_malloc((size_t)width * height * 4);
	for (int32_t y = 0; y < height; y++) {
		const uint16_t *s = (const uint16_t *)(in + (size_t)y * stride);
		uint8_t *d = rgba + (size_t)y * width * 4;
		for (int32_t x = 0; x < width; x++, d += 4) {
			uint16_t v = s[x];
			if (android_format == ANDROID_BITMAP_FORMAT_RGB_565) {
				d[0] = (uint8_t)(((v >> 11) & 0x1f) * 255 / 31);
				d[1] = (uint8_t)(((v >> 5) & 0x3f) * 255 / 63);
				d[2] = (uint8_t)((v & 0x1f) * 255 / 31);
				d[3] = 255;
			} else {
				d[0] = (uint8_t)(((v >> 12) & 0xf) * 17);
				d[1] = (uint8_t)(((v >> 8) & 0xf) * 17);
				d[2] = (uint8_t)(((v >> 4) & 0xf) * 17);
				d[3] = (uint8_t)((v & 0xf) * 17);
			}
		}
	}
	return g_bytes_new_take(rgba, (size_t)width * height * 4);
}

/* Bitmap.isPremultiplied() (an RGB_565 bitmap has no gdk_memory_format, it does not get here) */
static bool is_premultiplied(JNIEnv *env, jobject bitmap)
{
	return _GET_BOOL_FIELD(bitmap, "requestPremultiplied") && _GET_BOOL_FIELD(bitmap, "hasAlpha");
}

/* the pixels as AOSP has them in memory, like Bitmap.rawMemoryFormat(): premultiplied for a premultiplied bitmap */
static int raw_format(JNIEnv *env, jobject bitmap, int format)
{
	if (is_premultiplied(env, bitmap)) {
		if (format == GDK_MEMORY_R8G8B8A8)
			return GDK_MEMORY_R8G8B8A8_PREMULTIPLIED;
		if (format == GDK_MEMORY_R16G16B16A16_FLOAT)
			return GDK_MEMORY_R16G16B16A16_FLOAT_PREMULTIPLIED;
	}
	return format;
}

/* NDK bitmap.h AndroidBitmapInfo.flags */
#define ANDROID_BITMAP_FLAGS_ALPHA_PREMUL 0
#define ANDROID_BITMAP_FLAGS_ALPHA_OPAQUE 1
#define ANDROID_BITMAP_FLAGS_ALPHA_UNPREMUL 2

int AndroidBitmap_getInfo(JNIEnv *env, jobject bitmap, struct AndroidBitmapInfo *info)
{
	info->width = _GET_INT_FIELD(bitmap, "width");
	info->height = _GET_INT_FIELD(bitmap, "height");
	info->stride = _GET_INT_FIELD(bitmap, "stride");
	info->format = _GET_INT_FIELD(_GET_OBJ_FIELD(bitmap, "config", "Landroid/graphics/Bitmap$Config;"), "android_memory_format");
	/* like AOSP's ABitmap_getInfo: the alpha type of the bitmap */
	if (info->format == ANDROID_BITMAP_FORMAT_RGB_565 || !_GET_BOOL_FIELD(bitmap, "hasAlpha"))
		info->flags = ANDROID_BITMAP_FLAGS_ALPHA_OPAQUE;
	else if (_GET_BOOL_FIELD(bitmap, "requestPremultiplied"))
		info->flags = ANDROID_BITMAP_FLAGS_ALPHA_PREMUL;
	else
		info->flags = ANDROID_BITMAP_FLAGS_ALPHA_UNPREMUL;
	return ANDROID_BITMAP_RESULT_SUCCESS;
}

int AndroidBitmap_lockPixels(JNIEnv *env, jobject bitmap, void **pixels)
{
	printf("AndroidBitmap_lockPixels\n");
	GdkTexture *texture = _PTR(J__Bitmap__getTexture(env, bitmap));
	int stride = _GET_INT_FIELD(bitmap, "stride");
	int format = _GET_INT_FIELD(_GET_OBJ_FIELD(bitmap, "config", "Landroid/graphics/Bitmap$Config;"), "gdk_memory_format");
	if (format != -1)
		format = raw_format(env, bitmap, format);
	if (format == -1) {
		int32_t width = gdk_texture_get_width(texture), height = gdk_texture_get_height(texture);
		uint8_t *rgba = g_malloc((size_t)width * height * 4);
		GdkTextureDownloader *downloader = gdk_texture_downloader_new(texture);
		gdk_texture_downloader_set_format(downloader, GDK_MEMORY_R8G8B8A8_PREMULTIPLIED);
		gdk_texture_downloader_download_into(downloader, rgba, (gsize)width * 4);
		gdk_texture_downloader_free(downloader);
		uint8_t *data = g_malloc0((size_t)stride * height);
		to_16bit(rgba, data, width, height, stride, _GET_INT_FIELD(_GET_OBJ_FIELD(bitmap, "config", "Landroid/graphics/Bitmap$Config;"), "android_memory_format"));
		g_free(rgba);
		GBytes *bytes = g_bytes_new_take(data, (size_t)stride * height);
		_SET_LONG_FIELD(bitmap, "bytes", _INTPTR(bytes));
		*pixels = data;
		return ANDROID_BITMAP_RESULT_SUCCESS;
	}
	GdkTextureDownloader *downloader = gdk_texture_downloader_new(texture);
	gdk_texture_downloader_set_format(downloader, format);
	GBytes *bytes = NULL;
	if (GDK_IS_MEMORY_TEXTURE(texture)) { // try to get the bytes non-copying
		gsize texture_stride;
		bytes = gdk_texture_downloader_download_bytes(downloader, &texture_stride);
		if (texture_stride != stride) { // texture was not created by us, fall back to copy
			g_bytes_unref(bytes);
			bytes = NULL;
		}
	}
	if (bytes == NULL) {
		guchar *data = g_malloc(stride * gdk_texture_get_height(texture));
		gdk_texture_downloader_download_into(downloader, data, stride);
		bytes = g_bytes_new_take(data, stride * gdk_texture_get_height(texture));
	}
	gdk_texture_downloader_free(downloader);
	_SET_LONG_FIELD(bitmap, "bytes", _INTPTR(bytes));
	*pixels = (void *)g_bytes_get_data(bytes, NULL);
	return ANDROID_BITMAP_RESULT_SUCCESS;
}

int AndroidBitmap_unlockPixels(JNIEnv *env, jobject bitmap)
{
	printf("AndroidBitmap_unlockPixels\n");
	GBytes *bytes = _PTR(_GET_LONG_FIELD(bitmap, "bytes"));
	if (!bytes) {
		printf("AndroidBitmap_unlockPixels: no bytes! Was AndroidBitmap_lockPixels called?\n");
		exit(1);
	}
	int width = _GET_INT_FIELD(bitmap, "width");
	int height = _GET_INT_FIELD(bitmap, "height");
	int stride = _GET_INT_FIELD(bitmap, "stride");
	int format = _GET_INT_FIELD(_GET_OBJ_FIELD(bitmap, "config", "Landroid/graphics/Bitmap$Config;"), "gdk_memory_format");
	if (format != -1)
		format = raw_format(env, bitmap, format);
	GdkTexture *texture;
	if (format == -1) {
		GBytes *rgba = from_16bit(g_bytes_get_data(bytes, NULL), width, height, stride, _GET_INT_FIELD(_GET_OBJ_FIELD(bitmap, "config", "Landroid/graphics/Bitmap$Config;"), "android_memory_format"));
		texture = gdk_memory_texture_new(width, height, GDK_MEMORY_R8G8B8A8_PREMULTIPLIED, rgba, (gsize)width * 4);
		g_bytes_unref(rgba);
	} else {
		texture = gdk_memory_texture_new(width, height, format, bytes, stride);
	}
	g_bytes_unref(bytes);
	J__Bitmap__recycle(env, bitmap);
	_SET_BOOL_FIELD(bitmap, "recycled", false); // recycle() only drops the old texture here, the bitmap stays usable
	_SET_LONG_FIELD(bitmap, "texture", _INTPTR(texture));
	_SET_LONG_FIELD(bitmap, "bytes", 0);
	return ANDROID_BITMAP_RESULT_SUCCESS;
}

#define ANDROID_BITMAP_RESULT_BAD_PARAMETER (-1)
#define ANDROID_BITMAP_RESULT_JNI_EXCEPTION (-2)
/* NDK data_space.h */
#define ADATASPACE_UNKNOWN 0
#define ADATASPACE_SRGB 142671872

/* bitmap.cpp: ATL's bitmaps are sRGB (Bitmap.getColorSpace()); an A_8 bitmap has no color space
 * (ColorSpaceToADataSpace() of kAlpha_8 without one is ADATASPACE_UNKNOWN) */
int32_t AndroidBitmap_getDataSpace(JNIEnv *env, jobject jbitmap)
{
	struct AndroidBitmapInfo info;
	if (!env || !jbitmap)
		return ADATASPACE_UNKNOWN;
	AndroidBitmap_getInfo(env, jbitmap, &info);
	return info.format == ANDROID_BITMAP_FORMAT_A_8 ? ADATASPACE_UNKNOWN : ADATASPACE_SRGB;
}

/* bitmap.cpp: a bitmap without a hardware buffer (all of ATL's) answers BAD_PARAMETER with NULL */
int AndroidBitmap_getHardwareBuffer(JNIEnv *env, jobject jbitmap, void **outBuffer)
{
	if (!env || !jbitmap || !outBuffer)
		return ANDROID_BITMAP_RESULT_BAD_PARAMETER;
	*outBuffer = NULL;
	return ANDROID_BITMAP_RESULT_BAD_PARAMETER;
}

/* RGBA_1010102 (no GDK memory format) as R16G16B16A16 */
static GBytes *from_1010102(const uint8_t *in, int32_t width, int32_t height, int32_t stride)
{
	uint16_t *out = g_malloc((size_t)width * height * 8);
	for (int32_t y = 0; y < height; y++) {
		const uint32_t *s = (const uint32_t *)(in + (size_t)y * stride);
		uint16_t *d = out + (size_t)y * width * 4;
		for (int32_t x = 0; x < width; x++, d += 4) {
			uint32_t v = s[x];
			d[0] = (v & 0x3ff) * 65535 / 1023;
			d[1] = ((v >> 10) & 0x3ff) * 65535 / 1023;
			d[2] = ((v >> 20) & 0x3ff) * 65535 / 1023;
			d[3] = (v >> 30) * 65535 / 3;
		}
	}
	return g_bytes_new_take(out, (size_t)width * height * 8);
}

/* android_bitmap.cpp ABitmap_compress(): the pixels are encoded as Bitmap.compress() does
 * (Bitmap.native_compress()); the data space is taken as sRGB, the only one ATL's bitmaps have */
int AndroidBitmap_compress(const struct AndroidBitmapInfo *info, int32_t dataSpace, const void *pixels, int32_t format,
                           int32_t quality, void *userContext, bool (*fn)(void *userContext, const void *data, size_t size))
{
	if (!info || !pixels || !fn || quality < 0 || quality > 100)
		return ANDROID_BITMAP_RESULT_BAD_PARAMETER;
	/* AndroidBitmapCompressFormat: JPEG 0, PNG 1, WEBP_LOSSY 3, WEBP_LOSSLESS 4 = Bitmap.CompressFormat.nativeInt */
	if (format != 0 && format != 1 && format != 3 && format != 4)
		return ANDROID_BITMAP_RESULT_BAD_PARAMETER;
	/* getAlphaType(): the alpha bits must be one of the three types; the data space must be known, except for A_8 */
	if ((info->flags & 0x3) > ANDROID_BITMAP_FLAGS_ALPHA_UNPREMUL)
		return ANDROID_BITMAP_RESULT_BAD_PARAMETER;
	if (info->format != ANDROID_BITMAP_FORMAT_A_8 && dataSpace == ADATASPACE_UNKNOWN)
		return ANDROID_BITMAP_RESULT_BAD_PARAMETER;
	bool premul = (info->flags & 0x3) == ANDROID_BITMAP_FLAGS_ALPHA_PREMUL;
	GdkMemoryFormat memory_format;
	GBytes *bytes;
	size_t stride = info->stride;
	switch (info->format) {
		case ANDROID_BITMAP_FORMAT_RGBA_8888:
			memory_format = premul ? GDK_MEMORY_R8G8B8A8_PREMULTIPLIED : GDK_MEMORY_R8G8B8A8;
			bytes = g_bytes_new(pixels, (size_t)info->stride * info->height);
			break;
		case ANDROID_BITMAP_FORMAT_RGB_565:
			memory_format = GDK_MEMORY_R8G8B8A8_PREMULTIPLIED;
			bytes = from_16bit(pixels, info->width, info->height, info->stride, ANDROID_BITMAP_FORMAT_RGB_565);
			stride = (size_t)info->width * 4;
			break;
		case ANDROID_BITMAP_FORMAT_A_8:
			memory_format = GDK_MEMORY_A8;
			bytes = g_bytes_new(pixels, (size_t)info->stride * info->height);
			break;
		case ANDROID_BITMAP_FORMAT_RGBA_F16:
			memory_format = premul ? GDK_MEMORY_R16G16B16A16_FLOAT_PREMULTIPLIED : GDK_MEMORY_R16G16B16A16_FLOAT;
			bytes = g_bytes_new(pixels, (size_t)info->stride * info->height);
			break;
		case ANDROID_BITMAP_FORMAT_RGBA_1010102:
			memory_format = premul ? GDK_MEMORY_R16G16B16A16_PREMULTIPLIED : GDK_MEMORY_R16G16B16A16;
			bytes = from_1010102(pixels, info->width, info->height, info->stride);
			stride = (size_t)info->width * 8;
			break;
		default:
			return ANDROID_BITMAP_RESULT_BAD_PARAMETER;
	}
	GdkTexture *texture = gdk_memory_texture_new(info->width, info->height, memory_format, bytes, stride);
	g_bytes_unref(bytes);
	JNIEnv *env = ndk_jni_env();
	jclass bitmap_class = ndk_jni_class(env, "android/graphics/Bitmap");
	jmethodID compress = (*env)->GetStaticMethodID(env, bitmap_class, "native_compress", "(JII)[B");
	jbyteArray data = (*env)->CallStaticObjectMethod(env, bitmap_class, compress, _INTPTR(texture), format, quality);
	g_object_unref(texture);
	if (ndk_jni_threw(env, "Bitmap.native_compress"))
		return ANDROID_BITMAP_RESULT_JNI_EXCEPTION;
	/* SkEncodeImage() failed */
	if (!data)
		return ANDROID_BITMAP_RESULT_JNI_EXCEPTION;
	jsize size = (*env)->GetArrayLength(env, data);
	jbyte *buf = (*env)->GetByteArrayElements(env, data, NULL);
	bool ok = fn(userContext, buf, size);
	(*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);
	(*env)->DeleteLocalRef(env, data);
	return ok ? ANDROID_BITMAP_RESULT_SUCCESS : ANDROID_BITMAP_RESULT_JNI_EXCEPTION;
}
