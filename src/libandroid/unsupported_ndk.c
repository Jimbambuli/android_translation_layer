/*
 * NDK functions of features ATL does not have, answering like an Android device without them:
 * no gralloc buffers (AHardwareBuffer), no sync fences (libsync). Firefox's libxul links all of them
 * and checks the results.
 */

#include <errno.h>
#include <stdbool.h>
#include <stdint.h>
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

/* libsync */
int sync_merge(const char *name, int fd1, int fd2)
{
	errno = EINVAL;
	return -1;
}
