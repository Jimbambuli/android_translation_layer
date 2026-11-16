/* JNI of android.opengl.ETC1, ported from AOSP Android 16 (frameworks/base/core/jni/android/opengl/util.cpp) to C.
 * The codec itself is AOSP's libETC1 (frameworks/native/opengl/libs/ETC1/etc1.cpp), in etc1.c. */
#include <jni.h>
#include <stdint.h>

#include "generated_headers/android_opengl_ETC1.h"
#include "etc1.h"

static void throw_iae(JNIEnv *env, const char *message)
{
	(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"), message);
}

/* like jniGetNioBufferFields: NULL for a buffer without native memory (not direct) */
static void *get_pointer(JNIEnv *env, jobject buffer, jint *remaining)
{
	jclass class = (*env)->GetObjectClass(env, buffer);
	jlong pointer = (*env)->GetLongField(env, buffer, (*env)->GetFieldID(env, class, "address", "J"));
	jint shift = (*env)->GetIntField(env, buffer, (*env)->GetFieldID(env, class, "_elementSizeShift", "I"));
	jint position = (*env)->GetIntField(env, buffer, (*env)->GetFieldID(env, class, "position", "I"));
	jint limit = (*env)->GetIntField(env, buffer, (*env)->GetFieldID(env, class, "limit", "I"));
	if (pointer != 0)
		pointer += (jlong)position << shift;
	*remaining = (limit - position) << shift;
	return (void *)(intptr_t)pointer;
}

typedef struct {
	JNIEnv *env;
	jobject buffer;
	void *data;
	jint remaining;
} BufferHelper;

static int check_pointer(BufferHelper *b, const char *error_message)
{
	if (b->buffer) {
		b->data = get_pointer(b->env, b->buffer, &b->remaining);
		if (b->data == NULL)
			throw_iae(b->env, error_message);
		return b->data != NULL;
	}
	throw_iae(b->env, error_message);
	return 0;
}

#define BUFFER(name, env, obj) BufferHelper name = {env, obj, NULL, 0}

/**
 * Encode a block of pixels.
 *
 * @param in a pointer to a ETC1_DECODED_BLOCK_SIZE array of bytes that represent a
 * 4 x 4 square of 3-byte pixels in form R, G, B. Byte (3 * (x + 4 * y) is the R
 * value of pixel (x, y).
 *
 * @param validPixelMask is a 16-bit mask where bit (1 << (x + y * 4)) indicates whether
 * the corresponding (x,y) pixel is valid. Invalid pixel color values are ignored when compressing.
 *
 * @param out an ETC1 compressed version of the data.
 *
 */
JNIEXPORT void JNICALL Java_android_opengl_ETC1_encodeBlock(JNIEnv *env, jclass clazz,
                                                            jobject in, jint validPixelMask, jobject out)
{
	if (validPixelMask < 0 || validPixelMask > 15) {
		throw_iae(env, "validPixelMask");
		return;
	}
	BUFFER(inB, env, in);
	BUFFER(outB, env, out);
	if (check_pointer(&inB, "in") && check_pointer(&outB, "out")) {
		if (inB.remaining < ETC1_DECODED_BLOCK_SIZE) {
			throw_iae(env, "in's remaining data < DECODED_BLOCK_SIZE");
		} else if (outB.remaining < ETC1_ENCODED_BLOCK_SIZE) {
			throw_iae(env, "out's remaining data < ENCODED_BLOCK_SIZE");
		} else {
			etc1_encode_block((etc1_byte *)inB.data, validPixelMask,
			                  (etc1_byte *)outB.data);
		}
	}
}

/**
 * Decode a block of pixels.
 *
 * @param in an ETC1 compressed version of the data.
 *
 * @param out a pointer to a ETC_DECODED_BLOCK_SIZE array of bytes that represent a
 * 4 x 4 square of 3-byte pixels in form R, G, B. Byte (3 * (x + 4 * y) is the R
 * value of pixel (x, y).
 */
JNIEXPORT void JNICALL Java_android_opengl_ETC1_decodeBlock(JNIEnv *env, jclass clazz,
                                                            jobject in, jobject out)
{
	BUFFER(inB, env, in);
	BUFFER(outB, env, out);
	if (check_pointer(&inB, "in") && check_pointer(&outB, "out")) {
		if (inB.remaining < ETC1_ENCODED_BLOCK_SIZE) {
			throw_iae(env, "in's remaining data < ENCODED_BLOCK_SIZE");
		} else if (outB.remaining < ETC1_DECODED_BLOCK_SIZE) {
			throw_iae(env, "out's remaining data < DECODED_BLOCK_SIZE");
		} else {
			etc1_decode_block((etc1_byte *)inB.data,
			                  (etc1_byte *)outB.data);
		}
	}
}

/**
 * Return the size of the encoded image data (does not include size of PKM header).
 */
JNIEXPORT jint JNICALL Java_android_opengl_ETC1_getEncodedDataSize(JNIEnv *env, jclass clazz,
                                                                   jint width, jint height)
{
	return etc1_get_encoded_data_size(width, height);
}

/**
 * Encode an entire image.
 * @param in pointer to the image data. Formatted such that
 *           pixel (x,y) is at pIn + pixelSize * x + stride * y + redOffset;
 * @param out pointer to encoded data. Must be large enough to store entire encoded image.
 */
JNIEXPORT void JNICALL Java_android_opengl_ETC1_encodeImage(JNIEnv *env, jclass clazz,
                                                            jobject in, jint width, jint height,
                                                            jint pixelSize, jint stride, jobject out)
{
	if (pixelSize < 2 || pixelSize > 3) {
		throw_iae(env, "pixelSize must be 2 or 3");
		return;
	}
	BUFFER(inB, env, in);
	BUFFER(outB, env, out);
	if (check_pointer(&inB, "in") && check_pointer(&outB, "out")) {
		jint imageSize = stride * height;
		jint encodedImageSize = etc1_get_encoded_data_size(width, height);
		if (inB.remaining < imageSize) {
			throw_iae(env, "in's remaining data < image size");
		} else if (outB.remaining < encodedImageSize) {
			throw_iae(env, "out's remaining data < encoded image size");
		} else {
			etc1_encode_image((etc1_byte *)inB.data, width, height, pixelSize, stride,
			                  (etc1_byte *)outB.data);
		}
	}
}

/**
 * Decode an entire image.
 * @param in the encoded data.
 * @param out pointer to the image data. Will be written such that
 *            pixel (x,y) is at pIn + pixelSize * x + stride * y. Must be
 *            large enough to store entire image.
 */
JNIEXPORT void JNICALL Java_android_opengl_ETC1_decodeImage(JNIEnv *env, jclass clazz,
                                                            jobject in, jobject out,
                                                            jint width, jint height,
                                                            jint pixelSize, jint stride)
{
	if (pixelSize < 2 || pixelSize > 3) {
		throw_iae(env, "pixelSize must be 2 or 3");
		return;
	}
	BUFFER(inB, env, in);
	BUFFER(outB, env, out);
	if (check_pointer(&inB, "in") && check_pointer(&outB, "out")) {
		jint imageSize = stride * height;
		jint encodedImageSize = etc1_get_encoded_data_size(width, height);
		if (inB.remaining < encodedImageSize) {
			throw_iae(env, "in's remaining data < encoded image size");
		} else if (outB.remaining < imageSize) {
			throw_iae(env, "out's remaining data < image size");
		} else {
			etc1_decode_image((etc1_byte *)inB.data, (etc1_byte *)outB.data,
			                  width, height, pixelSize, stride);
		}
	}
}

/**
 * Format a PKM header
 */
JNIEXPORT void JNICALL Java_android_opengl_ETC1_formatHeader(JNIEnv *env, jclass clazz,
                                                             jobject header, jint width, jint height)
{
	BUFFER(headerB, env, header);
	if (check_pointer(&headerB, "header")) {
		if (headerB.remaining < ETC_PKM_HEADER_SIZE) {
			throw_iae(env, "header's remaining data < ETC_PKM_HEADER_SIZE");
		} else {
			etc1_pkm_format_header((etc1_byte *)headerB.data, width, height);
		}
	}
}

/**
 * Check if a PKM header is correctly formatted.
 */
JNIEXPORT jboolean JNICALL Java_android_opengl_ETC1_isValid(JNIEnv *env, jclass clazz,
                                                            jobject header)
{
	jboolean result = JNI_FALSE;
	BUFFER(headerB, env, header);
	if (check_pointer(&headerB, "header")) {
		if (headerB.remaining < ETC_PKM_HEADER_SIZE) {
			throw_iae(env, "header's remaining data < ETC_PKM_HEADER_SIZE");
		} else {
			result = etc1_pkm_is_valid((etc1_byte *)headerB.data);
		}
	}
	return result ? JNI_TRUE : JNI_FALSE;
}

/**
 * Read the image width from a PKM header
 */
JNIEXPORT jint JNICALL Java_android_opengl_ETC1_getWidth(JNIEnv *env, jclass clazz,
                                                         jobject header)
{
	jint result = 0;
	BUFFER(headerB, env, header);
	if (check_pointer(&headerB, "header")) {
		if (headerB.remaining < ETC_PKM_HEADER_SIZE) {
			throw_iae(env, "header's remaining data < ETC_PKM_HEADER_SIZE");
		} else {
			result = etc1_pkm_get_width((etc1_byte *)headerB.data);
		}
	}
	return result;
}

/**
 * Read the image height from a PKM header
 */
JNIEXPORT jint JNICALL Java_android_opengl_ETC1_getHeight(JNIEnv *env, jclass clazz,
                                                          jobject header)
{
	jint result = 0;
	BUFFER(headerB, env, header);
	if (check_pointer(&headerB, "header")) {
		if (headerB.remaining < ETC_PKM_HEADER_SIZE) {
			throw_iae(env, "header's remaining data < ETC_PKM_HEADER_SIZE");
		} else {
			result = etc1_pkm_get_height((etc1_byte *)headerB.data);
		}
	}
	return result;
}
