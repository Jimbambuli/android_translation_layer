#ifndef HARDWARE_BUFFER_H
#define HARDWARE_BUFFER_H

#include <pthread.h>
#include <stdbool.h>
#include <stdint.h>
#include <sys/types.h>

/* android/hardware_buffer.h */
typedef struct AHardwareBuffer_Desc {
	uint32_t width;
	uint32_t height;
	uint32_t layers;
	uint32_t format;
	uint64_t usage;
	uint32_t stride;
	uint32_t rfu0;
	uint64_t rfu1;
} AHardwareBuffer_Desc;

typedef struct AHardwareBuffer_Plane {
	void *data;
	uint32_t pixelStride;
	uint32_t rowStride;
} AHardwareBuffer_Plane;

typedef struct AHardwareBuffer_Planes {
	uint32_t planeCount;
	AHardwareBuffer_Plane planes[4];
} AHardwareBuffer_Planes;

#ifndef ARECT_DEFINED
#define ARECT_DEFINED
typedef struct ARect {
	int32_t left;
	int32_t top;
	int32_t right;
	int32_t bottom;
} ARect;
#endif

#define AHB_MAGIC 0x41484221 /* "AHB!" */
#define AHB_MAX_PLANES 4

/* ATL's AHardwareBuffer (hardware_buffer.c): a dma-buf of GBM or a memfd */
struct AHardwareBuffer {
	uint32_t magic;
	_Atomic int refcount;
	AHardwareBuffer_Desc desc;
	int fd;
	bool dmabuf;
	uint32_t drm_format;
	uint64_t modifier;
	uint64_t id;
	size_t size;
	int plane_count;
	uint32_t offsets[AHB_MAX_PLANES];
	uint32_t strides[AHB_MAX_PLANES];
	pthread_mutex_t lock;
	void *map;
	int lock_count;
	uint64_t lock_usage;
	struct native_handle *native_handle;
};

/* the AHardwareBuffer behind a pointer an app passes (EGLClientBuffer, Vulkan), NULL if it is none */
struct AHardwareBuffer *ahb_from_pointer(const void *pointer);

#endif
