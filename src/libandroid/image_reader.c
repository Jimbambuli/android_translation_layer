/*
 * NdkImageReader and NdkImage (libmediandk) without a buffer allocator. AOSP's AImageReader owns a BufferQueue
 * consumer (frameworks/av/media/ndk/NdkImageReader.cpp, AImageReader::init()) and its images are gralloc buffers;
 * ATL has neither (see ImageReader.java and AHardwareBuffer in unsupported_ndk.c). AImageReader_new*() therefore fails
 * the way init() does when the BufferItemConsumer cannot be allocated, AMEDIA_ERROR_UNKNOWN, and as no reader and no
 * image ever exist, every other function gets what AOSP answers for a NULL reader or image: INVALID_PARAMETER.
 */

#include <stdint.h>
#include <stdlib.h>

#include "media_ndk.h"

struct ANativeWindow;
typedef struct AHardwareBuffer AHardwareBuffer;
typedef struct native_handle native_handle_t;
typedef struct AImageReader AImageReader;
typedef struct AImage AImage;

typedef struct AImageCropRect {
	int32_t left;
	int32_t top;
	int32_t right;
	int32_t bottom;
} AImageCropRect;

typedef void (*AImageReader_ImageCallback)(void *context, AImageReader *reader);
typedef struct AImageReader_ImageListener {
	void *context;
	AImageReader_ImageCallback onImageAvailable;
} AImageReader_ImageListener;

typedef void (*AImageReader_BufferRemovedCallback)(void *context, AImageReader *reader, AHardwareBuffer *buffer);
typedef struct AImageReader_BufferRemovedListener {
	void *context;
	AImageReader_BufferRemovedCallback onBufferRemoved;
} AImageReader_BufferRemovedListener;

/* AImageReader::isSupportedFormatAndUsage() with the AIMAGE_FORMAT_* and HAL_PIXEL_FORMAT_YCBCR_P010 values */
static bool supported_format_and_usage(int32_t format, uint64_t usage)
{
	switch (format) {
		case 0x1: case 0x2: case 0x3: case 0x4: case 0x16: case 0x23: case 0x100: case 0x20: case 0x1002: case 0x24:
		case 0x25: case 0x26: case 0x44363159: case 0x101: case 0x20203859: case 0x48454946: case 0x69656963: case 0x1003:
		case 0x36:
			return true;
		case 0x22: /* AIMAGE_FORMAT_PRIVATE: no CPU read usage */
			return !(usage & 0x3);
		default:
			return false;
	}
}

/* AOSP's validateParameters(), BufferQueueDefs::NUM_BUFFER_SLOTS being 64, then init() fails */
media_status_t AImageReader_newWithUsage(int32_t width, int32_t height, int32_t format, uint64_t usage, int32_t maxImages,
                                         AImageReader **reader)
{
	if (!reader || width < 1 || height < 1 || maxImages < 1 || maxImages > 64 || !supported_format_and_usage(format, usage))
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return AMEDIA_ERROR_UNKNOWN;
}

/* usage AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN */
media_status_t AImageReader_new(int32_t width, int32_t height, int32_t format, int32_t maxImages, AImageReader **reader)
{
	return AImageReader_newWithUsage(width, height, format, 0x3, maxImages, reader);
}

/* AOSP maps the HAL format and dataspace to the public format and goes on with newWithUsage(); ATL has no HAL
 * formats, and every public format ends in the same failure. Only the arguments newWithUsage() checks are known: */
media_status_t AImageReader_newWithDataSpace(int32_t width, int32_t height, uint64_t usage, int32_t maxImages,
                                             uint32_t hardwareBufferFormat, int32_t dataSpace, AImageReader **reader)
{
	if (!reader || width < 1 || height < 1 || maxImages < 1 || maxImages > 64)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return AMEDIA_ERROR_UNKNOWN;
}

void AImageReader_delete(AImageReader *reader) {}

media_status_t AImageReader_getWindow(AImageReader *reader, struct ANativeWindow **window) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_getWidth(const AImageReader *reader, int32_t *width) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_getHeight(const AImageReader *reader, int32_t *height) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_getFormat(const AImageReader *reader, int32_t *format) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_getMaxImages(const AImageReader *reader, int32_t *maxImages) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_acquireNextImage(AImageReader *reader, AImage **image) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_acquireLatestImage(AImageReader *reader, AImage **image) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_acquireNextImageAsync(AImageReader *reader, AImage **image, int *acquireFenceFd) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_acquireLatestImageAsync(AImageReader *reader, AImage **image, int *acquireFenceFd) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_setImageListener(AImageReader *reader, AImageReader_ImageListener *listener) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_setBufferRemovedListener(AImageReader *reader, AImageReader_BufferRemovedListener *listener) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_getWindowNativeHandle(AImageReader *reader, native_handle_t **handle) { return AMEDIA_ERROR_INVALID_PARAMETER; }

void AImage_delete(AImage *image) {}
void AImage_deleteAsync(AImage *image, int releaseFenceFd) {}
media_status_t AImage_getWidth(const AImage *image, int32_t *width) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImage_getHeight(const AImage *image, int32_t *height) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImage_getFormat(const AImage *image, int32_t *format) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImage_getCropRect(const AImage *image, AImageCropRect *rect) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImage_getTimestamp(const AImage *image, int64_t *timestampNs) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImage_getNumberOfPlanes(const AImage *image, int32_t *numPlanes) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImage_getPlanePixelStride(const AImage *image, int planeIdx, int32_t *pixelStride) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImage_getPlaneRowStride(const AImage *image, int planeIdx, int32_t *rowStride) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImage_getPlaneData(const AImage *image, int planeIdx, uint8_t **data, int *dataLength) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImage_getHardwareBuffer(const AImage *image, AHardwareBuffer **buffer) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImage_getDataSpace(const AImage *image, int32_t *dataSpace) { return AMEDIA_ERROR_INVALID_PARAMETER; }
