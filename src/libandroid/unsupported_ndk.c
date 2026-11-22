/*
 * NDK functions of features ATL does not have, answering like an Android device without them:
 * no DRM scheme (libmediandk NdkMediaDrm/NdkMediaCrypto), no gralloc buffers (AHardwareBuffer),
 * no image readers (NdkImageReader), no sync fences (libsync), no SurfaceFlinger (ASurfaceControl,
 * AChoreographer). Firefox's libxul and YouTube's libgoogle3 link them and check the results.
 */

#include <errno.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>

#include <jni.h>

typedef int32_t media_status_t;
#define AMEDIA_ERROR_UNSUPPORTED (-10000 - 2)
#define AMEDIA_ERROR_INVALID_OBJECT (-10000 - 3)
#define AMEDIA_ERROR_INVALID_PARAMETER (-10000 - 4)
/* android::status_t */
#define BAD_VALUE (-EINVAL)
#define INVALID_OPERATION (-ENOSYS)

/* NdkMediaDrm / NdkMediaCrypto: MediaDrm.isCryptoSchemeSupported() is false for every scheme */
bool AMediaDrm_isCryptoSchemeSupported(const uint8_t *uuid, const char *mimeType) { return false; }
void *AMediaDrm_createByUUID(const uint8_t *uuid) { return NULL; }
void AMediaDrm_release(void *drm) {}
media_status_t AMediaDrm_setOnEventListener(void *drm, void *listener) { return AMEDIA_ERROR_INVALID_OBJECT; }
media_status_t AMediaDrm_openSession(void *drm, void *sessionId) { return AMEDIA_ERROR_INVALID_OBJECT; }
media_status_t AMediaDrm_closeSession(void *drm, const void *sessionId) { return AMEDIA_ERROR_INVALID_OBJECT; }
media_status_t AMediaDrm_getKeyRequest(void *drm, const void *scope, const uint8_t *init, size_t initSize,
                                       const char *mimeType, int keyType, const void *optionalParameters,
                                       size_t numOptionalParameters, const uint8_t **keyRequest, size_t *keyRequestSize) { return AMEDIA_ERROR_INVALID_OBJECT; }
media_status_t AMediaDrm_provideKeyResponse(void *drm, const void *scope, const uint8_t *response, size_t responseSize, void *keySetId) { return AMEDIA_ERROR_INVALID_OBJECT; }
media_status_t AMediaDrm_getProvisionRequest(void *drm, const uint8_t **provisionRequest, size_t *provisionRequestSize, const char **serverUrl) { return AMEDIA_ERROR_INVALID_OBJECT; }
media_status_t AMediaDrm_provideProvisionResponse(void *drm, const uint8_t *response, size_t responseSize) { return AMEDIA_ERROR_INVALID_OBJECT; }
media_status_t AMediaDrm_setPropertyString(void *drm, const char *propertyName, const char *value) { return AMEDIA_ERROR_INVALID_OBJECT; }
media_status_t AMediaDrm_setPropertyByteArray(void *drm, const char *propertyName, const uint8_t *value, size_t valueSize) { return AMEDIA_ERROR_INVALID_OBJECT; }
bool AMediaCrypto_isCryptoSchemeSupported(const uint8_t *uuid) { return false; }
void *AMediaCrypto_new(const uint8_t *uuid, const void *initData, size_t initDataSize) { return NULL; }
void AMediaCrypto_delete(void *crypto) {}

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

/* NdkImageReader / NdkImage */
media_status_t AImageReader_newWithUsage(int32_t width, int32_t height, int32_t format, uint64_t usage, int32_t maxImages, void **reader)
{
	if (reader)
		*reader = NULL;
	return AMEDIA_ERROR_UNSUPPORTED;
}
void AImageReader_delete(void *reader) {}
media_status_t AImageReader_getWindow(void *reader, void **window) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_setImageListener(void *reader, void *listener) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_acquireNextImageAsync(void *reader, void **image, int *acquireFenceFd) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_acquireNextImage(void *reader, void **image) { return AImageReader_acquireNextImageAsync(reader, image, NULL); }
media_status_t AImageReader_acquireLatestImageAsync(void *reader, void **image, int *acquireFenceFd) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImageReader_acquireLatestImage(void *reader, void **image) { return AImageReader_acquireLatestImageAsync(reader, image, NULL); }
void AImage_delete(void *image) {}
void AImage_deleteAsync(void *image, int releaseFenceFd) {}
media_status_t AImage_getHardwareBuffer(const void *image, void **buffer) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImage_getCropRect(const void *image, void *rect) { return AMEDIA_ERROR_INVALID_PARAMETER; }
media_status_t AImage_getTimestamp(const void *image, int64_t *timestampNs) { return AMEDIA_ERROR_INVALID_PARAMETER; }

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

/* AChoreographer.cpp: getInstance() is Choreographer::getForThread(), which returns NULL when it cannot
 * connect to SurfaceFlinger's display events ("Failed to initialize") */
void *AChoreographer_getInstance(void) { return NULL; }
void AChoreographer_postFrameCallback(void *choreographer, void (*callback)(long frameTimeNanos, void *data), void *data) {}
void AChoreographer_postFrameCallbackDelayed(void *choreographer, void (*callback)(long frameTimeNanos, void *data), void *data, long delayMillis) {}
void AChoreographer_postFrameCallback64(void *choreographer, void (*callback)(int64_t frameTimeNanos, void *data), void *data) {}
void AChoreographer_postFrameCallbackDelayed64(void *choreographer, void (*callback)(int64_t frameTimeNanos, void *data), void *data, uint32_t delayMillis) {}

/* native_window_jni: only windows of android.view.Surface have one */
jobject ANativeWindow_toSurface(JNIEnv *env, void *window) { return NULL; }

/* libsync */
int sync_merge(const char *name, int fd1, int fd2)
{
	errno = EINVAL;
	return -1;
}
