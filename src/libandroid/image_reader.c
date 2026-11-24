/*
 * NdkImageReader and NdkImage (libmediandk) after AOSP frameworks/av/media/ndk/NdkImageReader.cpp and NdkImage.cpp
 * (android-16). AOSP's AImageReader and the Java ImageReader are both consumers of a BufferQueue; here the AImageReader
 * is a Java android.media.ImageReader, whose Surface (Surface.atl_imageReader) takes the images of the producers:
 * MediaCodec (its frames are AHardwareBuffers, dma-bufs of the decoder), the camera and the CPU (ANativeWindow_lock()).
 * The callbacks run on a thread of the reader (AOSP: the reader's ALooper). No producer fences: acquire*Async() gives
 * -1, and the release fence of AImage_deleteAsync() is waited for before the buffer goes back.
 */

#include <poll.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <unistd.h>

#include <glib.h>

#include "hardware_buffer.h"
#include "media_ndk.h"
#include "native_window.h"
#include "ndk_jni.h"

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

int AHardwareBuffer_lockPlanes(struct AHardwareBuffer *buffer, uint64_t usage, int32_t fence, const ARect *rect, AHardwareBuffer_Planes *outPlanes);
int AHardwareBuffer_unlock(struct AHardwareBuffer *buffer, int32_t *fence);

enum {
	AIMAGE_FORMAT_RGBA_8888 = 0x1,
	AIMAGE_FORMAT_YUV_420_888 = 0x23,
	AIMAGE_FORMAT_PRIVATE = 0x22,
	AIMAGE_FORMAT_JPEG = 0x100,
	HAL_PIXEL_FORMAT_YCBCR_P010 = 0x36,
	USAGE_CPU_READ_OFTEN = 0x3,
};

struct AImageReader {
	jobject reader; /* the android.media.ImageReader (global reference) */
	struct ANativeWindow *window;
	int32_t width;
	int32_t height;
	int32_t format;
	uint64_t usage;
	int32_t max_images;
	int32_t num_planes;
	pthread_mutex_t lock;
	GList *images; /* the acquired AImages, closed when the reader goes */
};

struct AImage {
	AImageReader *reader;
	jobject image; /* the reader's Image (global reference) */
	bool closed;
	int32_t width;
	int32_t height;
	int64_t timestamp;
	struct AHardwareBuffer *buffer; /* the image's buffer of a hardware producer, else NULL */
	int32_t num_planes;
	bool locked;
	AHardwareBuffer_Planes planes; /* the locked buffer, or the Image's planes (direct ByteBuffers) */
	int32_t data_length[3];
};

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

/* AImageReader::getNumPlanesForFormat() */
static int32_t num_planes_for_format(int32_t format)
{
	switch (format) {
		case AIMAGE_FORMAT_YUV_420_888:
		case HAL_PIXEL_FORMAT_YCBCR_P010:
			return 3;
		case AIMAGE_FORMAT_PRIVATE:
			return 0;
		default:
			return 1;
	}
}

/* a reader of the Java class: through newInstance(), or the constructor of a HardwareBuffer format and data space */
static media_status_t reader_new(int32_t width, int32_t height, int32_t format, uint64_t usage, int32_t maxImages,
                                 jobject (*create)(JNIEnv *env, jclass class, void *data), void *data, AImageReader **out)
{
	JNIEnv *env = ndk_jni_env();
	(*env)->PushLocalFrame(env, 8);
	jclass class = ndk_jni_class(env, "android/media/ImageReader");
	jobject java_reader = create(env, class, data);
	if (ndk_jni_threw(env, "AImageReader_new") || !java_reader) {
		(*env)->PopLocalFrame(env, NULL);
		return AMEDIA_ERROR_UNKNOWN;
	}
	AImageReader *reader = calloc(1, sizeof(*reader));
	reader->reader = (*env)->NewGlobalRef(env, java_reader);
	reader->width = width;
	reader->height = height;
	reader->format = (*env)->CallIntMethod(env, java_reader, (*env)->GetMethodID(env, class, "getImageFormat", "()I"));
	reader->usage = usage;
	reader->max_images = maxImages;
	reader->num_planes = num_planes_for_format(reader->format);
	pthread_mutex_init(&reader->lock, NULL);
	(*env)->CallObjectMethod(env, java_reader, (*env)->GetMethodID(env, class, "atl_nativeExecutor", "(J)Ljava/util/concurrent/Executor;"), (jlong)(intptr_t)reader);
	jobject surface = (*env)->CallObjectMethod(env, java_reader, (*env)->GetMethodID(env, class, "getSurface", "()Landroid/view/Surface;"));
	reader->window = ANativeWindow_fromSurface(env, surface);
	(*env)->PopLocalFrame(env, NULL);
	*out = reader;
	return AMEDIA_OK;
}

struct format_usage {
	int32_t width, height, format, max_images;
	uint64_t usage;
	uint32_t hardware_buffer_format;
	int32_t data_space;
};

static jobject create_with_format(JNIEnv *env, jclass class, void *data)
{
	struct format_usage *a = data;
	jmethodID new_instance = (*env)->GetStaticMethodID(env, class, "newInstance", "(IIIIJ)Landroid/media/ImageReader;");
	return (*env)->CallStaticObjectMethod(env, class, new_instance, a->width, a->height, a->format, a->max_images, (jlong)a->usage);
}

static jobject create_with_data_space(JNIEnv *env, jclass class, void *data)
{
	struct format_usage *a = data;
	jmethodID init = (*env)->GetMethodID(env, class, "<init>", "(IIIJII)V");
	return (*env)->NewObject(env, class, init, a->width, a->height, a->max_images, (jlong)a->usage, (jint)a->hardware_buffer_format, a->data_space);
}

/* AOSP validateParameters(): BufferQueueDefs::NUM_BUFFER_SLOTS being 64 */
media_status_t AImageReader_newWithUsage(int32_t width, int32_t height, int32_t format, uint64_t usage, int32_t maxImages,
                                         AImageReader **reader)
{
	if (!reader || width < 1 || height < 1 || maxImages < 1 || maxImages > 64 || !supported_format_and_usage(format, usage))
		return AMEDIA_ERROR_INVALID_PARAMETER;
	struct format_usage a = {.width = width, .height = height, .format = format, .max_images = maxImages, .usage = usage};
	return reader_new(width, height, format, usage, maxImages, create_with_format, &a, reader);
}

/* usage AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN */
media_status_t AImageReader_new(int32_t width, int32_t height, int32_t format, int32_t maxImages, AImageReader **reader)
{
	return AImageReader_newWithUsage(width, height, format, USAGE_CPU_READ_OFTEN, maxImages, reader);
}

/* AOSP maps the HAL format and data space to the public format (the Java reader's constructor does it) */
media_status_t AImageReader_newWithDataSpace(int32_t width, int32_t height, uint64_t usage, int32_t maxImages,
                                             uint32_t hardwareBufferFormat, int32_t dataSpace, AImageReader **reader)
{
	if (!reader || width < 1 || height < 1 || maxImages < 1 || maxImages > 64)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	struct format_usage a = {.width = width, .height = height, .max_images = maxImages, .usage = usage,
	                         .hardware_buffer_format = hardwareBufferFormat, .data_space = dataSpace};
	return reader_new(width, height, 0, usage, maxImages, create_with_data_space, &a, reader);
}

static void image_release(JNIEnv *env, AImage *image)
{
	if (image->closed)
		return;
	image->closed = true;
	if (image->locked && image->buffer)
		AHardwareBuffer_unlock(image->buffer, NULL);
	image->locked = false;
	(*env)->CallVoidMethod(env, image->image, (*env)->GetMethodID(env, (*env)->GetObjectClass(env, image->image), "close", "()V"));
	ndk_jni_threw(env, "AImage close");
}

/* AOSP ~AImageReader(): the acquired images are closed (the app still deletes them), the listeners go */
void AImageReader_delete(AImageReader *reader)
{
	if (!reader)
		return;
	JNIEnv *env = ndk_jni_env();
	(*env)->PushLocalFrame(env, 8);
	pthread_mutex_lock(&reader->lock);
	for (GList *l = reader->images; l; l = l->next) {
		AImage *image = l->data;
		image_release(env, image);
		image->reader = NULL;
	}
	g_list_free(reader->images);
	reader->images = NULL;
	pthread_mutex_unlock(&reader->lock);
	jclass class = (*env)->GetObjectClass(env, reader->reader);
	(*env)->CallVoidMethod(env, reader->reader, (*env)->GetMethodID(env, class, "atl_setBufferRemovedListener", "(JJ)V"), (jlong)0, (jlong)0);
	(*env)->CallVoidMethod(env, reader->reader, (*env)->GetMethodID(env, class, "close", "()V"));
	ndk_jni_threw(env, "AImageReader_delete");
	(*env)->PopLocalFrame(env, NULL);
	if (reader->window)
		ANativeWindow_release(reader->window);
	(*env)->DeleteGlobalRef(env, reader->reader);
	pthread_mutex_destroy(&reader->lock);
	free(reader);
}

/* AOSP: the window stays the reader's (no reference for the caller) */
media_status_t AImageReader_getWindow(AImageReader *reader, struct ANativeWindow **window)
{
	if (!reader || !window)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*window = reader->window;
	return reader->window ? AMEDIA_OK : AMEDIA_ERROR_UNKNOWN;
}

media_status_t AImageReader_getWidth(const AImageReader *reader, int32_t *width)
{
	if (!reader || !width)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*width = reader->width;
	return AMEDIA_OK;
}

media_status_t AImageReader_getHeight(const AImageReader *reader, int32_t *height)
{
	if (!reader || !height)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*height = reader->height;
	return AMEDIA_OK;
}

media_status_t AImageReader_getFormat(const AImageReader *reader, int32_t *format)
{
	if (!reader || !format)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*format = reader->format;
	return AMEDIA_OK;
}

media_status_t AImageReader_getMaxImages(const AImageReader *reader, int32_t *maxImages)
{
	if (!reader || !maxImages)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*maxImages = reader->max_images;
	return AMEDIA_OK;
}

/* AOSP acquireImageLocked(): the oldest queued image; the image's size is its buffer's */
static media_status_t acquire_image(JNIEnv *env, AImageReader *reader, AImage **out)
{
	jclass class = (*env)->GetObjectClass(env, reader->reader);
	jobjectArray result = (*env)->NewObjectArray(env, 1, ndk_jni_class(env, "android/media/Image"), NULL);
	jint status = (*env)->CallIntMethod(env, reader->reader, (*env)->GetMethodID(env, class, "atl_acquireNextImage", "([Landroid/media/Image;)I"), result);
	if (ndk_jni_threw(env, "AImageReader_acquireNextImage"))
		return AMEDIA_ERROR_UNKNOWN;
	if (status == 1)
		return AMEDIA_IMGREADER_NO_BUFFER_AVAILABLE;
	if (status == 2)
		return AMEDIA_IMGREADER_MAX_IMAGES_ACQUIRED;
	jobject java_image = (*env)->GetObjectArrayElement(env, result, 0);
	jclass image_class = (*env)->GetObjectClass(env, java_image);
	AImage *image = calloc(1, sizeof(*image));
	image->reader = reader;
	image->image = (*env)->NewGlobalRef(env, java_image);
	image->width = (*env)->CallIntMethod(env, java_image, (*env)->GetMethodID(env, image_class, "getWidth", "()I"));
	image->height = (*env)->CallIntMethod(env, java_image, (*env)->GetMethodID(env, image_class, "getHeight", "()I"));
	image->timestamp = (*env)->CallLongMethod(env, java_image, (*env)->GetMethodID(env, image_class, "getTimestamp", "()J"));
	image->buffer = (struct AHardwareBuffer *)(intptr_t)(*env)->CallLongMethod(env, java_image, (*env)->GetMethodID(env, image_class, "atl_getHardwareBuffer", "()J"));
	image->num_planes = reader->num_planes;
	reader->images = g_list_prepend(reader->images, image);
	*out = image;
	return AMEDIA_OK;
}

static media_status_t acquire(AImageReader *reader, AImage **image, int *acquireFenceFd, bool latest)
{
	if (!reader || !image)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*image = NULL;
	if (acquireFenceFd)
		*acquireFenceFd = -1;
	JNIEnv *env = ndk_jni_env();
	(*env)->PushLocalFrame(env, 16);
	pthread_mutex_lock(&reader->lock);
	AImage *prev = NULL;
	media_status_t ret = acquire_image(env, reader, &prev);
	/* AOSP acquireLatestImage(): the newer ones replace it while there are */
	while (latest && prev) {
		AImage *next = NULL;
		acquire_image(env, reader, &next);
		if (!next)
			break;
		image_release(env, prev);
		reader->images = g_list_remove(reader->images, prev);
		(*env)->DeleteGlobalRef(env, prev->image);
		free(prev);
		prev = next;
	}
	pthread_mutex_unlock(&reader->lock);
	(*env)->PopLocalFrame(env, NULL);
	*image = prev;
	return prev ? AMEDIA_OK : ret;
}

media_status_t AImageReader_acquireNextImage(AImageReader *reader, AImage **image)
{
	return acquire(reader, image, NULL, false);
}

media_status_t AImageReader_acquireLatestImage(AImageReader *reader, AImage **image)
{
	return acquire(reader, image, NULL, true);
}

media_status_t AImageReader_acquireNextImageAsync(AImageReader *reader, AImage **image, int *acquireFenceFd)
{
	if (!acquireFenceFd)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return acquire(reader, image, acquireFenceFd, false);
}

media_status_t AImageReader_acquireLatestImageAsync(AImageReader *reader, AImage **image, int *acquireFenceFd)
{
	if (!acquireFenceFd)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return acquire(reader, image, acquireFenceFd, true);
}

media_status_t AImageReader_setImageListener(AImageReader *reader, AImageReader_ImageListener *listener)
{
	if (!reader)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	JNIEnv *env = ndk_jni_env();
	(*env)->PushLocalFrame(env, 4);
	jmethodID set = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, reader->reader), "atl_setImageListener", "(JJ)V");
	(*env)->CallVoidMethod(env, reader->reader, set, (jlong)(intptr_t)(listener ? listener->onImageAvailable : NULL), (jlong)(intptr_t)(listener ? listener->context : NULL));
	(*env)->PopLocalFrame(env, NULL);
	return AMEDIA_OK;
}

media_status_t AImageReader_setBufferRemovedListener(AImageReader *reader, AImageReader_BufferRemovedListener *listener)
{
	if (!reader)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	JNIEnv *env = ndk_jni_env();
	(*env)->PushLocalFrame(env, 4);
	jmethodID set = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, reader->reader), "atl_setBufferRemovedListener", "(JJ)V");
	(*env)->CallVoidMethod(env, reader->reader, set, (jlong)(intptr_t)(listener ? listener->onBufferRemoved : NULL), (jlong)(intptr_t)(listener ? listener->context : NULL));
	(*env)->PopLocalFrame(env, NULL);
	return AMEDIA_OK;
}

/* AOSP: the native handle of the window's IGraphicBufferProducer for another process; ATL has none */
media_status_t AImageReader_getWindowNativeHandle(AImageReader *reader, native_handle_t **handle)
{
	if (!reader || !handle)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	return AMEDIA_ERROR_UNKNOWN;
}

/* AOSP AImage::close() and free(): the buffer goes back to the reader after the release fence */
void AImage_deleteAsync(AImage *image, int releaseFenceFd)
{
	if (!image) {
		if (releaseFenceFd >= 0)
			close(releaseFenceFd);
		return;
	}
	if (releaseFenceFd >= 0) {
		poll(&(struct pollfd){.fd = releaseFenceFd, .events = POLLIN}, 1, -1);
		close(releaseFenceFd);
	}
	JNIEnv *env = ndk_jni_env();
	(*env)->PushLocalFrame(env, 4);
	AImageReader *reader = image->reader;
	if (reader)
		pthread_mutex_lock(&reader->lock);
	image_release(env, image);
	if (reader) {
		reader->images = g_list_remove(reader->images, image);
		pthread_mutex_unlock(&reader->lock);
	}
	(*env)->PopLocalFrame(env, NULL);
	(*env)->DeleteGlobalRef(env, image->image);
	free(image);
}

void AImage_delete(AImage *image)
{
	AImage_deleteAsync(image, -1);
}

media_status_t AImage_getWidth(const AImage *image, int32_t *width)
{
	if (!image || !width)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	if (image->closed)
		return AMEDIA_ERROR_INVALID_OBJECT;
	*width = image->width;
	return AMEDIA_OK;
}

media_status_t AImage_getHeight(const AImage *image, int32_t *height)
{
	if (!image || !height)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	if (image->closed)
		return AMEDIA_ERROR_INVALID_OBJECT;
	*height = image->height;
	return AMEDIA_OK;
}

media_status_t AImage_getFormat(const AImage *image, int32_t *format)
{
	if (!image || !format)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	if (image->closed)
		return AMEDIA_ERROR_INVALID_OBJECT;
	*format = image->reader->format;
	return AMEDIA_OK;
}

/* AOSP: the whole image */
media_status_t AImage_getCropRect(const AImage *image, AImageCropRect *rect)
{
	if (!image || !rect)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	if (image->closed)
		return AMEDIA_ERROR_INVALID_OBJECT;
	*rect = (AImageCropRect){0, 0, image->width, image->height};
	return AMEDIA_OK;
}

media_status_t AImage_getTimestamp(const AImage *image, int64_t *timestampNs)
{
	if (!image || !timestampNs)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	if (image->closed)
		return AMEDIA_ERROR_INVALID_OBJECT;
	*timestampNs = image->timestamp;
	return AMEDIA_OK;
}

media_status_t AImage_getNumberOfPlanes(const AImage *image, int32_t *numPlanes)
{
	if (!image || !numPlanes)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	if (image->closed)
		return AMEDIA_ERROR_INVALID_OBJECT;
	*numPlanes = image->num_planes;
	return AMEDIA_OK;
}

/* AOSP AImage::lockImage(): CPU read usage needed; a hardware buffer is locked, the planes of a producer that queued
 * memory (camera, CPU) are its direct ByteBuffers */
static media_status_t lock_image(AImage *cimage)
{
	AImage *image = (AImage *)cimage;
	if (image->closed)
		return AMEDIA_ERROR_INVALID_OBJECT;
	if (!(image->reader->usage & USAGE_CPU_READ_OFTEN))
		return AMEDIA_IMGREADER_CANNOT_LOCK_IMAGE;
	if (image->locked)
		return AMEDIA_OK;
	if (image->buffer) {
		if (AHardwareBuffer_lockPlanes(image->buffer, USAGE_CPU_READ_OFTEN, -1, NULL, &image->planes))
			return AMEDIA_IMGREADER_CANNOT_LOCK_IMAGE;
		for (int i = 0; i < 3 && i < (int)image->planes.planeCount; i++) {
			AHardwareBuffer_Plane *p = &image->planes.planes[i];
			int32_t w = i ? image->width / 2 : image->width;
			int32_t h = i ? image->height / 2 : image->height;
			/* AOSP getPlaneData(): YUV to the last sample, else all rows */
			image->data_length[i] = image->num_planes == 3 ? p->rowStride * (h - 1) + p->pixelStride * (w - 1) + (i ? 1 : p->pixelStride) : p->rowStride * h;
		}
	} else {
		JNIEnv *env = ndk_jni_env();
		(*env)->PushLocalFrame(env, 16);
		jobjectArray planes = (*env)->CallObjectMethod(env, image->image, (*env)->GetMethodID(env, (*env)->GetObjectClass(env, image->image), "getPlanes", "()[Landroid/media/Image$Plane;"));
		if (ndk_jni_threw(env, "AImage lock") || !planes) {
			(*env)->PopLocalFrame(env, NULL);
			return AMEDIA_IMGREADER_CANNOT_LOCK_IMAGE;
		}
		jclass plane_class = ndk_jni_class(env, "android/media/Image$Plane");
		jmethodID get_buffer = (*env)->GetMethodID(env, plane_class, "getBuffer", "()Ljava/nio/ByteBuffer;");
		jmethodID get_row_stride = (*env)->GetMethodID(env, plane_class, "getRowStride", "()I");
		jmethodID get_pixel_stride = (*env)->GetMethodID(env, plane_class, "getPixelStride", "()I");
		int n = (*env)->GetArrayLength(env, planes);
		image->planes.planeCount = n < 3 ? n : 3;
		for (int i = 0; i < (int)image->planes.planeCount; i++) {
			jobject plane = (*env)->GetObjectArrayElement(env, planes, i);
			jobject buffer = (*env)->CallObjectMethod(env, plane, get_buffer);
			image->planes.planes[i].data = (*env)->GetDirectBufferAddress(env, buffer);
			image->planes.planes[i].rowStride = (*env)->CallIntMethod(env, plane, get_row_stride);
			image->planes.planes[i].pixelStride = (*env)->CallIntMethod(env, plane, get_pixel_stride);
			image->data_length[i] = (*env)->GetDirectBufferCapacity(env, buffer);
		}
		(*env)->PopLocalFrame(env, NULL);
	}
	image->locked = true;
	return AMEDIA_OK;
}

static media_status_t locked_plane(const AImage *image, int planeIdx, AHardwareBuffer_Plane **plane)
{
	if (!image)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	media_status_t ret = lock_image((AImage *)image);
	if (ret != AMEDIA_OK)
		return ret;
	if (planeIdx < 0 || planeIdx >= (int)image->planes.planeCount)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	*plane = (AHardwareBuffer_Plane *)&image->planes.planes[planeIdx];
	return AMEDIA_OK;
}

media_status_t AImage_getPlanePixelStride(const AImage *image, int planeIdx, int32_t *pixelStride)
{
	AHardwareBuffer_Plane *plane;
	if (!pixelStride)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	media_status_t ret = locked_plane(image, planeIdx, &plane);
	if (ret == AMEDIA_OK)
		*pixelStride = plane->pixelStride;
	return ret;
}

media_status_t AImage_getPlaneRowStride(const AImage *image, int planeIdx, int32_t *rowStride)
{
	AHardwareBuffer_Plane *plane;
	if (!rowStride)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	media_status_t ret = locked_plane(image, planeIdx, &plane);
	if (ret == AMEDIA_OK)
		*rowStride = plane->rowStride;
	return ret;
}

media_status_t AImage_getPlaneData(const AImage *image, int planeIdx, uint8_t **data, int *dataLength)
{
	AHardwareBuffer_Plane *plane;
	if (!data || !dataLength)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	media_status_t ret = locked_plane(image, planeIdx, &plane);
	if (ret == AMEDIA_OK) {
		*data = plane->data;
		*dataLength = image->data_length[planeIdx];
	}
	return ret;
}

/* AOSP: the image's GraphicBuffer, valid while the image is (no reference for the caller); the images of a producer
 * that queued memory (camera, CPU) have none */
media_status_t AImage_getHardwareBuffer(const AImage *image, AHardwareBuffer **buffer)
{
	if (!image || !buffer)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	if (image->closed || !image->buffer)
		return AMEDIA_ERROR_INVALID_OBJECT;
	*buffer = image->buffer;
	return AMEDIA_OK;
}

media_status_t AImage_getDataSpace(const AImage *image, int32_t *dataSpace)
{
	if (!image || !dataSpace)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	if (image->closed)
		return AMEDIA_ERROR_INVALID_OBJECT;
	JNIEnv *env = ndk_jni_env();
	*dataSpace = (*env)->CallIntMethod(env, image->image, (*env)->GetMethodID(env, ndk_jni_class(env, "android/media/Image"), "getDataSpace", "()I"));
	return ndk_jni_threw(env, "AImage_getDataSpace") ? AMEDIA_ERROR_UNKNOWN : AMEDIA_OK;
}
