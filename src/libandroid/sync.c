/* libsync's NDK functions (android/sync.h) on the kernel's sync_file interface, as AOSP
 * system/core/libsync/sync.c does with the modern (4.7+) uapi */

#include <errno.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>

#include <linux/sync_file.h>

int sync_merge(const char *name, int fd1, int fd2)
{
	struct sync_merge_data data = {.fd2 = fd2};
	strncpy(data.name, name, sizeof(data.name) - 1);
	if (ioctl(fd1, SYNC_IOC_MERGE, &data) < 0)
		return -1;
	return data.fence;
}

/* the fence infos follow the struct, sync_file_info.sync_fence_info points to them (sync_get_fence_info()) */
struct sync_file_info *sync_file_info(int32_t fd)
{
	struct sync_file_info count = {0};
	if (ioctl(fd, SYNC_IOC_FILE_INFO, &count) < 0)
		return NULL;
	struct sync_file_info *info = calloc(1, sizeof(*info) + count.num_fences * sizeof(struct sync_fence_info));
	if (!info)
		return NULL;
	info->num_fences = count.num_fences;
	info->sync_fence_info = (uintptr_t)(info + 1);
	if (ioctl(fd, SYNC_IOC_FILE_INFO, info) < 0) {
		free(info);
		return NULL;
	}
	return info;
}

void sync_file_info_free(struct sync_file_info *info)
{
	free(info);
}
