/* JNI of android.opengl.GLUtils, ported from AOSP Android 16 (frameworks/base/core/jni/android/opengl/util.cpp).
 * The pixels of a Bitmap are the "bytes" pointer of ATL's Bitmap, AOSP's graphics::Bitmap::getPixels(). */
#include <GLES3/gl3.h>
#include <jni.h>
#include <stdbool.h>

#include "../api-impl-jni/defines.h"
#include "generated_headers/android_opengl_GLUtils.h"
#include "../libandroid/bitmap.h"

#ifndef GL_PALETTE8_RGBA8_OES
	#define GL_PALETTE8_RGBA8_OES 0x8B96
#endif

/* AOSP: bitmap.isValid() && bitmap.getPixels() != nullptr */
static void *get_pixels(JNIEnv *env, jobject bitmap)
{
	if (!bitmap || _GET_BOOL_FIELD(bitmap, "mRecycled"))
		return NULL;
	return _PTR(_GET_LONG_FIELD(bitmap, "bytes"));
}

// The internal format is no longer the same as pixel format, per Table 2 in
// https://www.khronos.org/registry/OpenGL-Refpages/es3.1/html/glTexImage2D.xhtml
static bool checkInternalFormat(int32_t bitmapFormat, int internalformat, int type)
{
	if (internalformat == GL_PALETTE8_RGBA8_OES) {
		return false;
	}
	switch (bitmapFormat) {
		case ANDROID_BITMAP_FORMAT_RGBA_8888:
			return (type == GL_UNSIGNED_BYTE && internalformat == GL_RGBA) || (type == GL_UNSIGNED_BYTE && internalformat == GL_SRGB8_ALPHA8);
		case ANDROID_BITMAP_FORMAT_A_8:
			return (type == GL_UNSIGNED_BYTE && internalformat == GL_ALPHA);
		case ANDROID_BITMAP_FORMAT_RGBA_4444:
			return (type == GL_UNSIGNED_SHORT_4_4_4_4 && internalformat == GL_RGBA);
		case ANDROID_BITMAP_FORMAT_RGB_565:
			return (type == GL_UNSIGNED_SHORT_5_6_5 && internalformat == GL_RGB);
		case ANDROID_BITMAP_FORMAT_RGBA_F16:
			return (type == GL_HALF_FLOAT && internalformat == GL_RGBA16F);
		case ANDROID_BITMAP_FORMAT_RGBA_1010102:
			return (type == GL_UNSIGNED_INT_2_10_10_10_REV && internalformat == GL_RGB10_A2);
		default:
			break;
	}
	return false;
}

// The internal format is no longer the same as pixel format, per Table 2 in
// https://www.khronos.org/registry/OpenGL-Refpages/es3.1/html/glTexImage2D.xhtml
static int getPixelFormatFromInternalFormat(uint32_t internalFormat)
{
	switch (internalFormat) {
		// For sized internal format.
		case GL_RGBA16F:
		case GL_SRGB8_ALPHA8:
			return GL_RGBA;
		// Base internal formats and pixel formats are still the same, see Table 1 in
		// https://www.khronos.org/registry/OpenGL-Refpages/es3.1/html/glTexImage2D.xhtml
		default:
			return internalFormat;
	}
}

static int getInternalFormat(int32_t bitmapFormat)
{
	switch (bitmapFormat) {
		case ANDROID_BITMAP_FORMAT_A_8:
			return GL_ALPHA;
		case ANDROID_BITMAP_FORMAT_RGBA_4444:
			return GL_RGBA;
		case ANDROID_BITMAP_FORMAT_RGBA_8888:
			return GL_RGBA;
		case ANDROID_BITMAP_FORMAT_RGB_565:
			return GL_RGB;
		case ANDROID_BITMAP_FORMAT_RGBA_F16:
			return GL_RGBA16F;
		case ANDROID_BITMAP_FORMAT_RGBA_1010102:
			return GL_RGB10_A2;
		default:
			return -1;
	}
}

static int getType(int32_t bitmapFormat)
{
	switch (bitmapFormat) {
		case ANDROID_BITMAP_FORMAT_A_8:
			return GL_UNSIGNED_BYTE;
		case ANDROID_BITMAP_FORMAT_RGBA_4444:
			return GL_UNSIGNED_SHORT_4_4_4_4;
		case ANDROID_BITMAP_FORMAT_RGBA_8888:
			return GL_UNSIGNED_BYTE;
		case ANDROID_BITMAP_FORMAT_RGB_565:
			return GL_UNSIGNED_SHORT_5_6_5;
		case ANDROID_BITMAP_FORMAT_RGBA_F16:
			return GL_HALF_FLOAT;
		case ANDROID_BITMAP_FORMAT_RGBA_1010102:
			return GL_UNSIGNED_INT_2_10_10_10_REV;
		default:
			return -1;
	}
}

JNIEXPORT jint JNICALL Java_android_opengl_GLUtils_native_1getInternalFormat(JNIEnv *env, jclass clazz, jobject bitmapObj)
{
	struct AndroidBitmapInfo info;
	AndroidBitmap_getInfo(env, bitmapObj, &info);
	return getInternalFormat(info.format);
}

JNIEXPORT jint JNICALL Java_android_opengl_GLUtils_native_1getType(JNIEnv *env, jclass clazz, jobject bitmapObj)
{
	struct AndroidBitmapInfo info;
	AndroidBitmap_getInfo(env, bitmapObj, &info);
	return getType(info.format);
}

JNIEXPORT jint JNICALL Java_android_opengl_GLUtils_native_1texImage2D(JNIEnv *env, jclass clazz, jint target, jint level,
                                                                      jint internalformat, jobject bitmapObj, jint type, jint border)
{
	void *pixels = get_pixels(env, bitmapObj);
	if (pixels != NULL) {
		struct AndroidBitmapInfo bitmapInfo;
		AndroidBitmap_getInfo(env, bitmapObj, &bitmapInfo);

		if (internalformat < 0) {
			internalformat = getInternalFormat(bitmapInfo.format);
		}
		if (type < 0) {
			type = getType(bitmapInfo.format);
		}

		if (checkInternalFormat(bitmapInfo.format, internalformat, type)) {
			glTexImage2D(target, level, internalformat, bitmapInfo.width, bitmapInfo.height, border,
			             getPixelFormatFromInternalFormat(internalformat), type, pixels);
			return 0;
		}
	}
	return -1;
}

JNIEXPORT jint JNICALL Java_android_opengl_GLUtils_native_1texSubImage2D(JNIEnv *env, jclass clazz, jint target, jint level,
                                                                         jint xoffset, jint yoffset, jobject bitmapObj, jint format, jint type)
{
	void *pixels = get_pixels(env, bitmapObj);
	if (pixels != NULL) {
		struct AndroidBitmapInfo bitmapInfo;
		AndroidBitmap_getInfo(env, bitmapObj, &bitmapInfo);

		int internalFormat = getInternalFormat(bitmapInfo.format);
		if (format < 0) {
			format = getPixelFormatFromInternalFormat(internalFormat);
			if (format == GL_PALETTE8_RGBA8_OES)
				return -1; // glCompressedTexSubImage2D() not supported
		}

		if (checkInternalFormat(bitmapInfo.format, internalFormat, type)) {
			glTexSubImage2D(target, level, xoffset, yoffset, bitmapInfo.width, bitmapInfo.height,
			                format, type, pixels);
			return 0;
		}
	}
	return -1;
}
