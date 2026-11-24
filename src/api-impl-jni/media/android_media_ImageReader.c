#include <stdint.h>

#include "../defines.h"
#include "../generated_headers/android_media_ImageReader.h"
#include "../../libandroid/hardware_buffer.h"

/* the AHardwareBuffers of ImageReader images of a hardware producer (ImageReader.queueHardwareBuffer()) */

int AHardwareBuffer_lockPlanes(struct AHardwareBuffer *buffer, uint64_t usage, int32_t fence, const ARect *rect, AHardwareBuffer_Planes *outPlanes);
int AHardwareBuffer_unlock(struct AHardwareBuffer *buffer, int32_t *fence);
void AHardwareBuffer_release(struct AHardwareBuffer *buffer);

/* the reader lets go of the buffer: the producer has it back (a decoder's surface), as AOSP's BufferQueue slot */
JNIEXPORT void JNICALL Java_android_media_ImageReader_nativeReleaseBuffer(JNIEnv *env, jclass class, jlong buffer)
{
	ahb_return_to_producer(_PTR(buffer));
	AHardwareBuffer_release(_PTR(buffer));
}

/* AImageReader_ImageListener.onImageAvailable(context, reader) */
JNIEXPORT void JNICALL Java_android_media_ImageReader_nativeImageAvailable(JNIEnv *env, jclass class, jlong callback, jlong context, jlong reader)
{
	((void (*)(void *, void *))_PTR(callback))(_PTR(context), _PTR(reader));
}

/* AImageReader_BufferRemovedListener.onBufferRemoved(context, reader, buffer) */
JNIEXPORT void JNICALL Java_android_media_ImageReader_nativeBufferRemoved(JNIEnv *env, jclass class, jlong callback, jlong context, jlong reader, jlong buffer)
{
	((void (*)(void *, void *, struct AHardwareBuffer *))_PTR(callback))(_PTR(context), _PTR(reader), _PTR(buffer));
}

/* AOSP Image_createSurfacePlanes(): the planes of the buffer locked for reading (AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN),
 * each as a direct ByteBuffer up to its last pixel; the number of planes, -1 if the buffer cannot be locked */
JNIEXPORT jint JNICALL Java_android_media_ImageReader_nativeLockPlanes(JNIEnv *env, jclass class, jlong buffer_ptr, jobjectArray buffers, jintArray row_strides, jintArray pixel_strides)
{
	struct AHardwareBuffer *buffer = _PTR(buffer_ptr);
	AHardwareBuffer_Planes planes;
	if (AHardwareBuffer_lockPlanes(buffer, 3, -1, NULL, &planes))
		return -1;
	uint32_t width = buffer->desc.width;
	uint32_t height = buffer->desc.height;
	for (uint32_t i = 0; i < planes.planeCount && i < 3; i++) {
		AHardwareBuffer_Plane *p = &planes.planes[i];
		uint32_t w = i ? width / 2 : width;
		uint32_t h = i ? height / 2 : height;
		jlong size = (jlong)p->rowStride * (h - 1) + (jlong)p->pixelStride * (w - 1) + (i ? 1 : p->pixelStride); /* AOSP: to the last sample */
		(*env)->SetObjectArrayElement(env, buffers, i, (*env)->NewDirectByteBuffer(env, p->data, size));
		(*env)->SetIntArrayRegion(env, row_strides, i, 1, (jint *)&p->rowStride);
		(*env)->SetIntArrayRegion(env, pixel_strides, i, 1, (jint *)&p->pixelStride);
	}
	return planes.planeCount;
}

JNIEXPORT void JNICALL Java_android_media_ImageReader_nativeUnlock(JNIEnv *env, jclass class, jlong buffer)
{
	AHardwareBuffer_unlock(_PTR(buffer), NULL);
}
