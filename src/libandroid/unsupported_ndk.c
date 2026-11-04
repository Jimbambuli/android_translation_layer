/*
 * NDK functions of features ATL does not have, answering like an Android device without them:
 * no DRM scheme (libmediandk NdkMediaDrm/NdkMediaCrypto), no gralloc buffers (AHardwareBuffer),
 * no image readers (NdkImageReader), no sync fences (libsync). Firefox's libxul links all of them
 * and checks the results.
 */

#include <errno.h>
#include <stdbool.h>
#include <stdint.h>
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
void AImage_delete(void *image) {}
void AImage_deleteAsync(void *image, int releaseFenceFd) {}
media_status_t AImage_getHardwareBuffer(const void *image, void **buffer) { return AMEDIA_ERROR_INVALID_PARAMETER; }

/* native_window_jni: only windows of android.view.Surface have one */
jobject ANativeWindow_toSurface(JNIEnv *env, void *window) { return NULL; }

/* libsync */
int sync_merge(const char *name, int fd1, int fd2)
{
	errno = EINVAL;
	return -1;
}
