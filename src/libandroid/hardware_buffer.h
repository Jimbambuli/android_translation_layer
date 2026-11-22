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

/* system/core/libsystem/include/system/window.h, nativebase/nativebase.h: the ANativeWindowBuffer of a gralloc
 * buffer, which EGL_ANDROID_image_native_buffer takes as EGLClientBuffer */
struct android_native_base {
	int magic;
	int version;
	void *reserved[4];
	void (*incRef)(struct android_native_base *base);
	void (*decRef)(struct android_native_base *base);
};

struct ANativeWindowBuffer_ {
	struct android_native_base common;
	int width;
	int height;
	int stride;
	int format;
	int usage_deprecated;
	uintptr_t layerCount;
	void *reserved[1];
	const struct native_handle *handle;
	uint64_t usage;
	void *reserved_proc[8 - (sizeof(uint64_t) / sizeof(void *))];
};

/* ATL's AHardwareBuffer (hardware_buffer.c): a dma-buf of GBM or a memfd. AOSP's AHardwareBuffer is a GraphicBuffer,
 * whose ANativeWindowBuffer follows two pointers (vtable, RefBase); ANGLE relies on that offset
 * (kAHardwareBufferToANativeWindowBufferOffset), so the ANativeWindowBuffer is at the same place here. */
struct AHardwareBuffer {
	uintptr_t magic;
	uintptr_t unused;
	struct ANativeWindowBuffer_ anwb;
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

/* the AHardwareBuffer behind a pointer an app passes (Vulkan), NULL if it is none */
struct AHardwareBuffer *ahb_from_pointer(const void *pointer);
/* the AHardwareBuffer of an EGLClientBuffer (its ANativeWindowBuffer), NULL if it is none */
struct AHardwareBuffer *ahb_from_client_buffer(const void *client_buffer);

#endif
