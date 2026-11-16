#include <errno.h>
#include <poll.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include <linux/sync_file.h>

#include "defines.h"
#include "generated_headers/android_hardware_SyncFence.h"

/* AOSP core/jni/android_hardware_SyncFence.cpp: a SyncFence holds a reference to a ui/Fence (frameworks/native
 * libs/ui/Fence.cpp), which owns a sync_file descriptor. Here the Fence is this counted descriptor, and its
 * methods call the kernel's sync_file interface as libsync (system/core/libsync/sync.c) does. */
struct fence {
	int fd;
	uint32_t refs;
};

static void fence_dec_ref(struct fence *fence)
{
	if (__atomic_sub_fetch(&fence->refs, 1, __ATOMIC_ACQ_REL) == 0) {
		if (fence->fd != -1)
			close(fence->fd);
		free(fence);
	}
}

JNIEXPORT jlong JNICALL Java_android_hardware_SyncFence_nGetDestructor(JNIEnv *env, jclass class)
{
	return _INTPTR(&fence_dec_ref);
}

JNIEXPORT jlong JNICALL Java_android_hardware_SyncFence_nCreate(JNIEnv *env, jclass class, jint fd)
{
	struct fence *fence = malloc(sizeof(struct fence));
	fence->fd = fd;
	fence->refs = 1;
	return _INTPTR(fence);
}

/* Fence::isValid() */
JNIEXPORT jboolean JNICALL Java_android_hardware_SyncFence_nIsValid(JNIEnv *env, jclass class, jlong ptr)
{
	return ((struct fence *)_PTR(ptr))->fd != -1;
}

JNIEXPORT jint JNICALL Java_android_hardware_SyncFence_nGetFd(JNIEnv *env, jclass class, jlong ptr)
{
	return ((struct fence *)_PTR(ptr))->fd;
}

/* Fence::wait(int timeout) with libsync's sync_wait(). Like AOSP, the nanoseconds of SyncFence.await() go into
 * the int milliseconds parameter as they are (-1 = forever). */
JNIEXPORT jboolean JNICALL Java_android_hardware_SyncFence_nWait(JNIEnv *env, jclass class, jlong ptr, jlong timeout_nanos)
{
	int fd = ((struct fence *)_PTR(ptr))->fd;
	int timeout = (int)timeout_nanos;
	if (fd == -1)
		return JNI_TRUE;
	struct pollfd fds = {.fd = fd, .events = POLLIN};
	int ret;
	do {
		ret = poll(&fds, 1, timeout);
		if (ret > 0)
			return !(fds.revents & (POLLERR | POLLNVAL));
		else if (ret == 0)
			return JNI_FALSE;
	} while (ret == -1 && (errno == EINTR || errno == EAGAIN));
	return JNI_FALSE;
}

/* Fence::getSignalTime() with libsync's sync_file_info(): the latest timestamp of the fences in the sync_file,
 * SIGNAL_TIME_PENDING while it is not signaled, SIGNAL_TIME_INVALID on error */
JNIEXPORT jlong JNICALL Java_android_hardware_SyncFence_nGetSignalTime(JNIEnv *env, jclass class, jlong ptr)
{
	int fd = ((struct fence *)_PTR(ptr))->fd;
	if (fd == -1)
		return android_hardware_SyncFence_SIGNAL_TIME_INVALID;
	struct sync_file_info info = {0};
	if (ioctl(fd, SYNC_IOC_FILE_INFO, &info) < 0 || info.num_fences == 0)
		return android_hardware_SyncFence_SIGNAL_TIME_INVALID;
	if (info.status != 1)
		return info.status < 0 ? android_hardware_SyncFence_SIGNAL_TIME_INVALID : android_hardware_SyncFence_SIGNAL_TIME_PENDING;
	struct sync_fence_info *fences = calloc(info.num_fences, sizeof(struct sync_fence_info));
	info.sync_fence_info = (uint64_t)(uintptr_t)fences;
	jlong timestamp = android_hardware_SyncFence_SIGNAL_TIME_INVALID;
	if (ioctl(fd, SYNC_IOC_FILE_INFO, &info) >= 0) {
		uint64_t latest = 0;
		for (uint32_t i = 0; i < info.num_fences; i++) {
			if (fences[i].timestamp_ns > latest)
				latest = fences[i].timestamp_ns;
		}
		timestamp = latest;
	}
	free(fences);
	return timestamp;
}

JNIEXPORT void JNICALL Java_android_hardware_SyncFence_nIncRef(JNIEnv *env, jclass class, jlong ptr)
{
	__atomic_add_fetch(&((struct fence *)_PTR(ptr))->refs, 1, __ATOMIC_ACQ_REL);
}
