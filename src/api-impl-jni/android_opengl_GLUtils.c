/* JNI of android.opengl.GLUtils, ported from AOSP Android 16 (frameworks/base/core/jni/android/opengl/util.cpp).
 * The pixels of a Bitmap are the "bytes" pointer of ATL's Bitmap, AOSP's graphics::Bitmap::getPixels(). */
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <jni.h>
#include <stdbool.h>

#include "../api-impl-jni/defines.h"
#include "generated_headers/android_opengl_GLUtils.h"
#include "../libandroid/bitmap.h"

#ifndef GL_PALETTE8_RGBA8_OES
	#define GL_PALETTE8_RGBA8_OES 0x8B96
#endif

/* Locking and unlocking a Bitmap goes through GDK, which can leave its own GL context current on this thread. AOSP's
 * GLUtils works on the caller's context, so remember it and make it current again. */
typedef struct {
	EGLDisplay display;
	EGLContext context;
	EGLSurface draw, read;
} GlState;

static void save_gl_state(GlState *st)
{
	st->display = eglGetCurrentDisplay();
	st->context = eglGetCurrentContext();
	st->draw = eglGetCurrentSurface(EGL_DRAW);
	st->read = eglGetCurrentSurface(EGL_READ);
}

static void restore_gl_state(const GlState *st)
{
	if (st->context != EGL_NO_CONTEXT && eglGetCurrentContext() != st->context)
		eglMakeCurrent(st->display, st->draw, st->read, st->context);
}

/* AOSP: bitmap.isValid() && bitmap.getPixels() != nullptr; getPixels() is AndroidBitmap_lockPixels(), the pixels are only
 * there between lock and unlock (the "bytes" field is the GBytes of the lock, 0 otherwise) */
static void *lock_pixels(JNIEnv *env, jobject bitmap, const GlState *st)
{
	void *pixels = NULL;
	if (!bitmap || _GET_BOOL_FIELD(bitmap, "recycled"))
		return NULL;
	if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != 0)
		pixels = NULL;
	restore_gl_state(st);
	return pixels;
}

static void unlock_pixels(JNIEnv *env, jobject bitmap, const GlState *st)
{
	AndroidBitmap_unlockPixels(env, bitmap);
	restore_gl_state(st);
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
	GlState st;
	save_gl_state(&st);
	void *pixels = lock_pixels(env, bitmapObj, &st);
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
			unlock_pixels(env, bitmapObj, &st);
			return 0;
		}
		unlock_pixels(env, bitmapObj, &st);
	}
	return -1;
}

JNIEXPORT jint JNICALL Java_android_opengl_GLUtils_native_1texSubImage2D(JNIEnv *env, jclass clazz, jint target, jint level,
                                                                         jint xoffset, jint yoffset, jobject bitmapObj, jint format, jint type)
{
	GlState st;
	save_gl_state(&st);
	void *pixels = lock_pixels(env, bitmapObj, &st);
	if (pixels != NULL) {
		struct AndroidBitmapInfo bitmapInfo;
		AndroidBitmap_getInfo(env, bitmapObj, &bitmapInfo);

		int internalFormat = getInternalFormat(bitmapInfo.format);
		if (format < 0) {
			format = getPixelFormatFromInternalFormat(internalFormat);
			if (format == GL_PALETTE8_RGBA8_OES) {
				unlock_pixels(env, bitmapObj, &st);
				return -1; // glCompressedTexSubImage2D() not supported
			}
		}

		if (checkInternalFormat(bitmapInfo.format, internalFormat, type)) {
			glTexSubImage2D(target, level, xoffset, yoffset, bitmapInfo.width, bitmapInfo.height,
			                format, type, pixels);
			unlock_pixels(env, bitmapObj, &st);
			return 0;
		}
		unlock_pixels(env, bitmapObj, &st);
	}
	return -1;
}
