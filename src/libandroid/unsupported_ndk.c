/*
 * NDK functions of features ATL does not have, answering like an Android device without them:
 * no DRM scheme (libmediandk NdkMediaCrypto), no gralloc buffers (AHardwareBuffer), no sync fences
 * (libsync), no SurfaceFlinger (ASurfaceControl). Firefox's libxul and YouTube's
 * libgoogle3 link them and check the results.
 */

#include <errno.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>

#include <jni.h>

/* android::status_t */
#define BAD_VALUE (-EINVAL)
#define INVALID_OPERATION (-ENOSYS)

/* AHardwareBuffer: no allocator, so no buffer ever exists */
int AHardwareBuffer_allocate(const void *desc, void **outBuffer)
{
	if (outBuffer)
		*outBuffer = NULL;
	return INVALID_OPERATION;
}
void AHardwareBuffer_release(void *buffer) {}
void AHardwareBuffer_describe(const void *buffer, void *outDesc) {}
int AHardwareBuffer_lock(void *buffer, uint64_t usage, int32_t fence, const void *rect, void **outVirtualAddress) { return BAD_VALUE; }
int AHardwareBuffer_unlock(void *buffer, int32_t *fence) { return BAD_VALUE; }
int AHardwareBuffer_sendHandleToUnixSocket(const void *buffer, int socketFd) { return BAD_VALUE; }
int AHardwareBuffer_recvHandleFromUnixSocket(int socketFd, void **outBuffer) { return INVALID_OPERATION; }
/* AOSP AHardwareBuffer.cpp: isSupported() asks the allocator; a trial allocation fails here */
int AHardwareBuffer_isSupported(const void *desc) { return 0; }
void AHardwareBuffer_acquire(void *buffer) {}
int AHardwareBuffer_lockPlanes(void *buffer, uint64_t usage, int32_t fence, const void *rect, void *outPlanes) { return BAD_VALUE; }
/* hardware_buffer_jni.cpp: an android.hardware.HardwareBuffer in ATL never holds a native buffer */
void *AHardwareBuffer_fromHardwareBuffer(JNIEnv *env, jobject hardwareBufferObj) { return NULL; }
jobject AHardwareBuffer_toHardwareBuffer(JNIEnv *env, void *hardwareBuffer) { return NULL; }

/* surface_control.cpp: createFromWindow() returns NULL when there is no composer client
 * (SurfaceComposerClient::initCheck()). So no ASurfaceControl exists, and a transaction has nothing to
 * apply and never completes. */
void *ASurfaceControl_createFromWindow(void *window, const char *debug_name) { return NULL; }
void ASurfaceControl_release(void *aSurfaceControl) {}
struct ASurfaceTransaction {
	char unused;
};
void *ASurfaceTransaction_create(void) { return calloc(1, sizeof(struct ASurfaceTransaction)); }
void ASurfaceTransaction_delete(void *aSurfaceTransaction) { free(aSurfaceTransaction); }
void ASurfaceTransaction_apply(void *aSurfaceTransaction) {}
void ASurfaceTransaction_setOnComplete(void *aSurfaceTransaction, void *context, void (*func)(void *context, void *stats)) {}
void ASurfaceTransaction_reparent(void *aSurfaceTransaction, void *aSurfaceControl, void *newParentASurfaceControl) {}
void ASurfaceTransaction_setBuffer(void *aSurfaceTransaction, void *aSurfaceControl, void *buffer, int acquire_fence_fd) {}
void ASurfaceTransaction_setBufferDataSpace(void *aSurfaceTransaction, void *aSurfaceControl, int32_t aDataSpace) {}
void ASurfaceTransaction_setGeometry(void *aSurfaceTransaction, void *aSurfaceControl, const void *source, const void *destination, int32_t transform) {}
int ASurfaceTransactionStats_getPreviousReleaseFenceFd(void *aSurfaceTransactionStats, void *aSurfaceControl) { return -1; }

/* libsync */
int sync_merge(const char *name, int fd1, int fd2)
{
	errno = EINVAL;
	return -1;
}
