/*
 * AHardwareBuffer (android/hardware_buffer.h) after AOSP libs/nativewindow/AHardwareBuffer.cpp (android-16.0.0_r4).
 * Where AOSP's gralloc allocates, ATL allocates a dma-buf with GBM on the render node (linear when the CPU uses
 * it); a buffer only the CPU uses goes to a memfd when GBM cannot allocate it (BLOB, layers, no render node).
 * isSupported() answers from the format table and a trial with GBM; lock() maps the buffer and brackets the CPU
 * access with DMA_BUF_IOCTL_SYNC; send/recvHandleToUnixSocket() pass the description and the fd (SCM_RIGHTS)
 * between ATL processes. EGL and Vulkan import a dma-buf buffer (egl.c, native_window.c).
 */

#define _GNU_SOURCE

#include <dlfcn.h>
#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <poll.h>
#include <pthread.h>
#include <stdatomic.h>
#include <stddef.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <unistd.h>

#include <drm/drm_fourcc.h>
#include <linux/dma-buf.h>

#include <jni.h>

#include "hardware_buffer.h"

/* android::status_t */
#define NO_ERROR 0
#define BAD_VALUE (-EINVAL)
#define NO_MEMORY (-ENOMEM)
#define INVALID_OPERATION (-ENOSYS)
#define UNKNOWN_ERROR (-2147483647 - 1)

/* hardware_buffer.h, vndk/hardware_buffer.h */
enum {
	AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM = 1,
	AHARDWAREBUFFER_FORMAT_R8G8B8X8_UNORM = 2,
	AHARDWAREBUFFER_FORMAT_R8G8B8_UNORM = 3,
	AHARDWAREBUFFER_FORMAT_R5G6B5_UNORM = 4,
	AHARDWAREBUFFER_FORMAT_B8G8R8A8_UNORM = 5,
	AHARDWAREBUFFER_FORMAT_R16G16B16A16_FLOAT = 0x16,
	AHARDWAREBUFFER_FORMAT_BLOB = 0x21,
	AHARDWAREBUFFER_FORMAT_Y8Cb8Cr8_420 = 0x23,
	AHARDWAREBUFFER_FORMAT_R10G10B10A2_UNORM = 0x2b,
	AHARDWAREBUFFER_FORMAT_YCbCr_P010 = 0x36,
	AHARDWAREBUFFER_FORMAT_R8_UNORM = 0x38,
	AHARDWAREBUFFER_FORMAT_R16_UINT = 0x39,
	AHARDWAREBUFFER_FORMAT_R16G16_UINT = 0x3a,
	AHARDWAREBUFFER_FORMAT_YCbCr_P210 = 0x3c,
	AHARDWAREBUFFER_FORMAT_B10G10R10A2_UNORM = 0x43,
	AHARDWAREBUFFER_FORMAT_YV12 = 0x32315659,
	AHARDWAREBUFFER_FORMAT_Y8 = 0x20203859,
	AHARDWAREBUFFER_FORMAT_Y16 = 0x20363159,
	AHARDWAREBUFFER_FORMAT_YCbCr_422_SP = 0x10,
	AHARDWAREBUFFER_FORMAT_YCrCb_420_SP = 0x11,
	AHARDWAREBUFFER_FORMAT_YCbCr_422_I = 0x14,
};

#define USAGE_CPU_READ_MASK 0xFULL
#define USAGE_CPU_WRITE_MASK (0xFULL << 4)
#define USAGE_GPU_SAMPLED_IMAGE (1ULL << 8)
#define USAGE_GPU_FRAMEBUFFER (1ULL << 9)
#define USAGE_COMPOSER_OVERLAY (1ULL << 11)
#define USAGE_PROTECTED_CONTENT (1ULL << 14)
#define USAGE_VIDEO_ENCODE (1ULL << 16)
#define USAGE_SENSOR_DIRECT_DATA (1ULL << 23)
#define USAGE_GPU_DATA_BUFFER (1ULL << 24)
#define USAGE_GPU_CUBE_MAP (1ULL << 25)
#define USAGE_GPU_MIPMAP_COMPLETE (1ULL << 26)
#define USAGE_GPU_ANY (USAGE_GPU_SAMPLED_IMAGE | USAGE_GPU_FRAMEBUFFER | USAGE_GPU_DATA_BUFFER | USAGE_GPU_CUBE_MAP | USAGE_GPU_MIPMAP_COMPLETE | USAGE_COMPOSER_OVERLAY)

/* the DRM format of each AHardwareBuffer format and its layout for the CPU (memfd buffers, lock) */
struct format_info {
	uint32_t format;
	uint32_t drm_format;
	uint8_t bpp;      /* bytes per pixel of plane 0 */
	uint8_t yuv;      /* 0: one plane; 420: chroma planes of half width and height; 422: half width */
	uint8_t chroma_step; /* bytes between two chroma samples (2: interleaved CbCr) */
	bool cr_first;    /* YV12, NV21 */
};

static const struct format_info formats[] = {
	{AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM, DRM_FORMAT_ABGR8888, 4},
	{AHARDWAREBUFFER_FORMAT_R8G8B8X8_UNORM, DRM_FORMAT_XBGR8888, 4},
	{AHARDWAREBUFFER_FORMAT_R8G8B8_UNORM, DRM_FORMAT_BGR888, 3},
	{AHARDWAREBUFFER_FORMAT_R5G6B5_UNORM, DRM_FORMAT_RGB565, 2},
	{AHARDWAREBUFFER_FORMAT_B8G8R8A8_UNORM, DRM_FORMAT_ARGB8888, 4},
	{AHARDWAREBUFFER_FORMAT_R16G16B16A16_FLOAT, DRM_FORMAT_ABGR16161616F, 8},
	{AHARDWAREBUFFER_FORMAT_R10G10B10A2_UNORM, DRM_FORMAT_ABGR2101010, 4},
	{AHARDWAREBUFFER_FORMAT_B10G10R10A2_UNORM, DRM_FORMAT_ARGB2101010, 4},
	{AHARDWAREBUFFER_FORMAT_R8_UNORM, DRM_FORMAT_R8, 1},
	{AHARDWAREBUFFER_FORMAT_R16_UINT, DRM_FORMAT_R16, 2},
	{AHARDWAREBUFFER_FORMAT_R16G16_UINT, DRM_FORMAT_GR1616, 4},
	{AHARDWAREBUFFER_FORMAT_BLOB, DRM_FORMAT_R8, 1},
	{AHARDWAREBUFFER_FORMAT_Y8, DRM_FORMAT_R8, 1},
	{AHARDWAREBUFFER_FORMAT_Y16, DRM_FORMAT_R16, 2},
	{AHARDWAREBUFFER_FORMAT_Y8Cb8Cr8_420, DRM_FORMAT_NV12, 1, 42, 2, false},
	{AHARDWAREBUFFER_FORMAT_YCrCb_420_SP, DRM_FORMAT_NV21, 1, 42, 2, true},
	{AHARDWAREBUFFER_FORMAT_YCbCr_422_SP, DRM_FORMAT_NV16, 1, 22, 2, false},
	{AHARDWAREBUFFER_FORMAT_YV12, DRM_FORMAT_YVU420, 1, 42, 1, true},
	{AHARDWAREBUFFER_FORMAT_YCbCr_P010, DRM_FORMAT_P010, 2, 42, 4, false},
	{AHARDWAREBUFFER_FORMAT_YCbCr_P210, DRM_FORMAT_P210, 2, 22, 4, false},
	{AHARDWAREBUFFER_FORMAT_YCbCr_422_I, DRM_FORMAT_YUYV, 2, 0, 0, false},
};

static const struct format_info *format_info(uint32_t format)
{
	for (size_t i = 0; i < sizeof(formats) / sizeof(formats[0]); i++)
		if (formats[i].format == format)
			return &formats[i];
	return NULL;
}

/* AHardwareBuffer.cpp AHardwareBuffer_formatIsYuv() */
static bool format_is_yuv(uint32_t format)
{
	switch (format) {
		case AHARDWAREBUFFER_FORMAT_Y8Cb8Cr8_420:
		case AHARDWAREBUFFER_FORMAT_YV12:
		case AHARDWAREBUFFER_FORMAT_Y8:
		case AHARDWAREBUFFER_FORMAT_Y16:
		case AHARDWAREBUFFER_FORMAT_YCbCr_422_SP:
		case AHARDWAREBUFFER_FORMAT_YCrCb_420_SP:
		case AHARDWAREBUFFER_FORMAT_YCbCr_422_I:
		case AHARDWAREBUFFER_FORMAT_YCbCr_P010:
		case AHARDWAREBUFFER_FORMAT_YCbCr_P210:
			return true;
		default:
			return false;
	}
}

/* AHardwareBuffer.cpp AHardwareBuffer_isValidDescription() */
static bool is_valid_description(const AHardwareBuffer_Desc *desc)
{
	if (desc->width == 0 || desc->height == 0 || desc->layers == 0)
		return false;
	if (desc->rfu0 != 0 || desc->rfu1 != 0)
		return false;
	if (desc->format == AHARDWAREBUFFER_FORMAT_BLOB) {
		if (desc->height != 1 || desc->layers != 1)
			return false;
		if (desc->usage & (USAGE_GPU_SAMPLED_IMAGE | USAGE_GPU_FRAMEBUFFER | USAGE_GPU_MIPMAP_COMPLETE | USAGE_GPU_CUBE_MAP))
			return false;
		if (desc->usage & USAGE_VIDEO_ENCODE)
			return false;
	} else if (format_is_yuv(desc->format)) {
		if (desc->layers != 1)
			return false;
		if (desc->usage & (USAGE_GPU_MIPMAP_COMPLETE | USAGE_GPU_CUBE_MAP))
			return false;
	} else if (desc->usage & USAGE_SENSOR_DIRECT_DATA) {
		return false;
	}
	if ((desc->usage & (USAGE_CPU_READ_MASK | USAGE_CPU_WRITE_MASK)) && (desc->usage & USAGE_PROTECTED_CONTENT))
		return false;
	if (desc->usage & USAGE_GPU_CUBE_MAP) {
		if (desc->width != desc->height)
			return false;
		if (desc->layers % 6 != 0)
			return false;
	}
	return true;
}

/* GBM (libgbm.so.1 of the runtime, loaded when the first GPU buffer is asked for) */
struct gbm_device;
struct gbm_bo;
#define GBM_BO_USE_SCANOUT (1 << 0)
#define GBM_BO_USE_RENDERING (1 << 2)
#define GBM_BO_USE_LINEAR (1 << 4)
static struct {
	struct gbm_device *dev;
	struct gbm_device *(*create_device)(int fd);
	struct gbm_bo *(*bo_create)(struct gbm_device *, uint32_t w, uint32_t h, uint32_t format, uint32_t flags);
	void (*bo_destroy)(struct gbm_bo *);
	int (*bo_get_fd)(struct gbm_bo *);
	int (*bo_get_plane_count)(struct gbm_bo *);
	uint32_t (*bo_get_stride_for_plane)(struct gbm_bo *, int plane);
	uint32_t (*bo_get_offset)(struct gbm_bo *, int plane);
	uint64_t (*bo_get_modifier)(struct gbm_bo *);
	int (*is_format_supported)(struct gbm_device *, uint32_t format, uint32_t flags);
} gbm;
static pthread_once_t gbm_once = PTHREAD_ONCE_INIT;

static void gbm_init(void)
{
	void *lib = dlopen("libgbm.so.1", RTLD_NOW | RTLD_LOCAL);
	if (!lib)
		return;
	gbm.create_device = dlsym(lib, "gbm_create_device");
	gbm.bo_create = dlsym(lib, "gbm_bo_create");
	gbm.bo_destroy = dlsym(lib, "gbm_bo_destroy");
	gbm.bo_get_fd = dlsym(lib, "gbm_bo_get_fd");
	gbm.bo_get_plane_count = dlsym(lib, "gbm_bo_get_plane_count");
	gbm.bo_get_stride_for_plane = dlsym(lib, "gbm_bo_get_stride_for_plane");
	gbm.bo_get_offset = dlsym(lib, "gbm_bo_get_offset");
	gbm.bo_get_modifier = dlsym(lib, "gbm_bo_get_modifier");
	gbm.is_format_supported = dlsym(lib, "gbm_device_is_format_supported");
	if (!gbm.create_device || !gbm.bo_create || !gbm.bo_destroy || !gbm.bo_get_fd || !gbm.bo_get_plane_count ||
	    !gbm.bo_get_stride_for_plane || !gbm.bo_get_offset || !gbm.bo_get_modifier || !gbm.is_format_supported)
		return;
	/* the first render node */
	for (int i = 128; i < 192 && !gbm.dev; i++) {
		char path[32];
		snprintf(path, sizeof(path), "/dev/dri/renderD%d", i);
		int fd = open(path, O_RDWR | O_CLOEXEC);
		if (fd < 0)
			continue;
		gbm.dev = gbm.create_device(fd);
		if (!gbm.dev)
			close(fd);
	}
}

static struct gbm_device *gbm_device(void)
{
	pthread_once(&gbm_once, gbm_init);
	return gbm.dev;
}

static uint32_t gbm_flags(uint64_t usage)
{
	uint32_t flags = 0;
	if (usage & USAGE_GPU_FRAMEBUFFER)
		flags |= GBM_BO_USE_RENDERING;
	if (usage & (USAGE_CPU_READ_MASK | USAGE_CPU_WRITE_MASK))
		flags |= GBM_BO_USE_LINEAR;
	return flags;
}

static atomic_uint_fast64_t next_id = 1;

static uint32_t chroma_height(const struct format_info *fi, uint32_t height)
{
	return fi->yuv == 42 ? (height + 1) / 2 : height;
}

static uint32_t chroma_width_bytes(const struct format_info *fi, uint32_t width)
{
	return (width + 1) / 2 * fi->chroma_step;
}

/* the linear layout of a memfd buffer: rows of 64 byte aligned strides, the planes one after the other */
static void cpu_layout(struct AHardwareBuffer *buffer, const struct format_info *fi)
{
	const AHardwareBuffer_Desc *d = &buffer->desc;
	uint32_t stride = (d->width * fi->bpp + 63) & ~63u;
	size_t layer = (size_t)stride * d->height;
	buffer->plane_count = 1;
	buffer->offsets[0] = 0;
	buffer->strides[0] = stride;
	if (fi->yuv) {
		uint32_t cw = chroma_width_bytes(fi, d->width);
		if (fi->chroma_step == 1) { /* YV12: separate Cr and Cb planes, strides aligned to 16 as Android's YV12 */
			stride = (d->width + 15) & ~15u;
			uint32_t cstride = (stride / 2 + 15) & ~15u;
			buffer->strides[0] = stride;
			buffer->strides[1] = buffer->strides[2] = cstride;
			buffer->offsets[1] = (size_t)stride * d->height;
			buffer->offsets[2] = buffer->offsets[1] + (size_t)cstride * chroma_height(fi, d->height);
			buffer->plane_count = 3;
			layer = buffer->offsets[2] + (size_t)cstride * chroma_height(fi, d->height);
		} else {
			/* NV12/NV21/NV16/P010/P210: the interleaved chroma rows are as wide as the luma rows */
			uint32_t cstride = stride;
			(void)cw;
			buffer->strides[1] = cstride;
			buffer->offsets[1] = layer;
			buffer->plane_count = 2;
			layer += (size_t)cstride * chroma_height(fi, d->height);
		}
	}
	buffer->size = layer * d->layers;
	buffer->desc.stride = buffer->strides[0] / fi->bpp;
}

_Static_assert(offsetof(struct AHardwareBuffer, anwb) == 2 * sizeof(void *), "ANativeWindowBuffer offset of GraphicBuffer");

/* the buffer's native_handle_t (cutils/native_handle.h): one fd, the dma-buf or memfd */
struct native_handle {
	int version;
	int numFds;
	int numInts;
	int data[1];
};

void AHardwareBuffer_acquire(struct AHardwareBuffer *buffer);
void AHardwareBuffer_release(struct AHardwareBuffer *buffer);

static void anwb_inc_ref(struct android_native_base *base)
{
	AHardwareBuffer_acquire((struct AHardwareBuffer *)((char *)base - offsetof(struct AHardwareBuffer, anwb)));
}

static void anwb_dec_ref(struct android_native_base *base)
{
	AHardwareBuffer_release((struct AHardwareBuffer *)((char *)base - offsetof(struct AHardwareBuffer, anwb)));
}

/* the ANativeWindowBuffer of the allocated or received buffer, as GraphicBuffer fills it (format and usage are
 * the HAL's, which the AHardwareBuffer values equal for the formats and usage bits here) */
static void fill_anwb(struct AHardwareBuffer *buffer)
{
	struct ANativeWindowBuffer_ *w = &buffer->anwb;
	w->common.magic = ('_' << 24) | ('b' << 16) | ('f' << 8) | 'r'; /* ANDROID_NATIVE_BUFFER_MAGIC */
	w->common.version = sizeof(*w);
	w->common.incRef = anwb_inc_ref;
	w->common.decRef = anwb_dec_ref;
	w->width = buffer->desc.width;
	w->height = buffer->desc.height;
	w->stride = buffer->desc.stride;
	w->format = buffer->desc.format;
	w->usage_deprecated = (int)buffer->desc.usage;
	w->layerCount = buffer->desc.layers;
	buffer->native_handle = calloc(1, sizeof(struct native_handle));
	buffer->native_handle->version = 3 * sizeof(int);
	buffer->native_handle->numFds = 1;
	buffer->native_handle->data[0] = buffer->fd;
	w->handle = buffer->native_handle;
	w->usage = buffer->desc.usage;
}

static struct AHardwareBuffer *buffer_new(const AHardwareBuffer_Desc *desc)
{
	struct AHardwareBuffer *buffer = calloc(1, sizeof(*buffer));
	buffer->magic = AHB_MAGIC;
	buffer->refcount = 1;
	buffer->desc = *desc;
	buffer->fd = -1;
	buffer->modifier = DRM_FORMAT_MOD_INVALID;
	buffer->id = atomic_fetch_add(&next_id, 1) | ((uint64_t)getpid() << 32);
	return buffer;
}

static int allocate(const AHardwareBuffer_Desc *desc, struct AHardwareBuffer **out)
{
	const struct format_info *fi = format_info(desc->format);
	if (!fi)
		return BAD_VALUE;
	struct AHardwareBuffer *buffer = buffer_new(desc);
	buffer->drm_format = fi->drm_format;
	/* as gralloc's buffers, which every user (CPU, GPU, codecs) can import, a buffer is a dma-buf of GBM when the
	 * render node can allocate it (linear for the CPU); GBM knows no layers, data buffers or protected memory */
	bool gpu = desc->usage & USAGE_GPU_ANY;
	bool gbm_possible = desc->layers == 1 && !(desc->usage & (USAGE_GPU_DATA_BUFFER | USAGE_PROTECTED_CONTENT | USAGE_GPU_CUBE_MAP)) &&
	                    desc->format != AHARDWAREBUFFER_FORMAT_BLOB && gbm_device();
	struct gbm_bo *bo = gbm_possible ? gbm.bo_create(gbm.dev, desc->width, desc->height, fi->drm_format, gbm_flags(desc->usage)) : NULL;
	if (bo) {
		buffer->fd = gbm.bo_get_fd(bo);
		buffer->plane_count = gbm.bo_get_plane_count(bo);
		for (int i = 0; i < buffer->plane_count && i < AHB_MAX_PLANES; i++) {
			buffer->offsets[i] = gbm.bo_get_offset(bo, i);
			buffer->strides[i] = gbm.bo_get_stride_for_plane(bo, i);
		}
		buffer->modifier = gbm.bo_get_modifier(bo);
		gbm.bo_destroy(bo); /* the dma-buf fd keeps the memory */
		if (buffer->fd < 0) {
			free(buffer);
			return NO_MEMORY;
		}
		buffer->dmabuf = true;
		buffer->desc.stride = buffer->strides[0] / fi->bpp;
		buffer->size = lseek(buffer->fd, 0, SEEK_END);
	} else if (gpu) {
		free(buffer);
		return gbm_possible ? NO_MEMORY : INVALID_OPERATION;
	} else {
		cpu_layout(buffer, fi);
		buffer->fd = memfd_create("AHardwareBuffer", MFD_CLOEXEC | MFD_ALLOW_SEALING);
		if (buffer->fd < 0 || ftruncate(buffer->fd, buffer->size) < 0) {
			if (buffer->fd >= 0)
				close(buffer->fd);
			free(buffer);
			return NO_MEMORY;
		}
	}
	fill_anwb(buffer);
	*out = buffer;
	return NO_ERROR;
}

struct AHardwareBuffer *ahb_from_pointer(const void *pointer)
{
	struct AHardwareBuffer *buffer = (struct AHardwareBuffer *)pointer;
	return buffer && buffer->magic == AHB_MAGIC ? buffer : NULL;
}

struct AHardwareBuffer *ahb_from_client_buffer(const void *client_buffer)
{
	return client_buffer ? ahb_from_pointer((const char *)client_buffer - offsetof(struct AHardwareBuffer, anwb)) : NULL;
}

int AHardwareBuffer_allocate(const AHardwareBuffer_Desc *desc, struct AHardwareBuffer **outBuffer)
{
	if (!outBuffer || !desc)
		return BAD_VALUE;
	if (!is_valid_description(desc))
		return BAD_VALUE;
	return allocate(desc, outBuffer);
}

/* AOSP: a trial allocation of the gralloc allocator (IAllocator::isSupported()); here the format table and, for
 * GPU usage, GBM's answer for the DRM format */
int AHardwareBuffer_isSupported(const AHardwareBuffer_Desc *desc)
{
	if (!desc || !is_valid_description(desc))
		return 0;
	const struct format_info *fi = format_info(desc->format);
	if (!fi)
		return 0;
	if (!(desc->usage & USAGE_GPU_ANY))
		return 1;
	if (desc->layers != 1 || (desc->usage & (USAGE_GPU_DATA_BUFFER | USAGE_PROTECTED_CONTENT | USAGE_GPU_CUBE_MAP)) || !gbm_device())
		return 0;
	return gbm.is_format_supported(gbm.dev, fi->drm_format, gbm_flags(desc->usage)) ? 1 : 0;
}

void AHardwareBuffer_acquire(struct AHardwareBuffer *buffer)
{
	if (buffer)
		atomic_fetch_add(&buffer->refcount, 1);
}

void AHardwareBuffer_release(struct AHardwareBuffer *buffer)
{
	if (!buffer || atomic_fetch_sub(&buffer->refcount, 1) != 1)
		return;
	if (buffer->map)
		munmap(buffer->map, buffer->size);
	if (buffer->fd >= 0)
		close(buffer->fd);
	free(buffer->native_handle);
	buffer->magic = 0;
	free(buffer);
}

void AHardwareBuffer_describe(const struct AHardwareBuffer *buffer, AHardwareBuffer_Desc *outDesc)
{
	if (!buffer || !outDesc)
		return;
	*outDesc = buffer->desc;
	outDesc->rfu0 = 0;
	outDesc->rfu1 = 0;
}

/* gralloc's lockAsync() waits for the acquire fence and takes it over */
static void wait_fence(int32_t fence)
{
	if (fence < 0)
		return;
	struct pollfd pfd = {.fd = fence, .events = POLLIN};
	while (poll(&pfd, 1, -1) < 0 && errno == EINTR)
		;
	close(fence);
}

static int map(struct AHardwareBuffer *buffer, uint64_t usage, int32_t fence)
{
	if (usage & ~(USAGE_CPU_READ_MASK | USAGE_CPU_WRITE_MASK) || usage == 0)
		return BAD_VALUE;
	/* gralloc refuses to lock a buffer for a CPU access it was not allocated for */
	if (((usage & USAGE_CPU_READ_MASK) && !(buffer->desc.usage & USAGE_CPU_READ_MASK)) ||
	    ((usage & USAGE_CPU_WRITE_MASK) && !(buffer->desc.usage & USAGE_CPU_WRITE_MASK)))
		return BAD_VALUE;
	wait_fence(fence);
	pthread_mutex_lock(&buffer->lock);
	if (!buffer->map) {
		void *map = mmap(NULL, buffer->size, PROT_READ | PROT_WRITE, MAP_SHARED, buffer->fd, 0);
		if (map == MAP_FAILED) {
			pthread_mutex_unlock(&buffer->lock);
			return NO_MEMORY;
		}
		buffer->map = map;
	}
	if (buffer->dmabuf) {
		struct dma_buf_sync sync = {.flags = DMA_BUF_SYNC_START};
		sync.flags |= (usage & USAGE_CPU_READ_MASK) ? DMA_BUF_SYNC_READ : 0;
		sync.flags |= (usage & USAGE_CPU_WRITE_MASK) ? DMA_BUF_SYNC_WRITE : 0;
		ioctl(buffer->fd, DMA_BUF_IOCTL_SYNC, &sync);
	}
	buffer->lock_usage = usage;
	buffer->lock_count++;
	pthread_mutex_unlock(&buffer->lock);
	return NO_ERROR;
}

static void *rect_address(struct AHardwareBuffer *buffer, int plane, const ARect *rect, uint32_t bpp)
{
	uint8_t *base = (uint8_t *)buffer->map + buffer->offsets[plane];
	if (rect)
		base += (size_t)rect->top * buffer->strides[plane] + (size_t)rect->left * bpp;
	return base;
}

int AHardwareBuffer_lockAndGetInfo(struct AHardwareBuffer *buffer, uint64_t usage, int32_t fence, const ARect *rect,
                                   void **outVirtualAddress, int32_t *outBytesPerPixel, int32_t *outBytesPerStride)
{
	if (outBytesPerPixel)
		*outBytesPerPixel = -1;
	if (outBytesPerStride)
		*outBytesPerStride = -1;
	if (!buffer)
		return BAD_VALUE;
	if (usage & ~(USAGE_CPU_READ_MASK | USAGE_CPU_WRITE_MASK) || usage == 0)
		return BAD_VALUE;
	if (buffer->desc.layers > 1)
		return INVALID_OPERATION;
	const struct format_info *fi = format_info(buffer->desc.format);
	/* gralloc 4's bytes per pixel/stride are -1 for a format with more than one plane */
	if (fi->yuv)
		return INVALID_OPERATION;
	int ret = map(buffer, usage, fence);
	if (ret)
		return ret;
	*outVirtualAddress = rect_address(buffer, 0, rect, fi->bpp);
	if (outBytesPerPixel)
		*outBytesPerPixel = fi->bpp;
	if (outBytesPerStride)
		*outBytesPerStride = buffer->strides[0];
	return NO_ERROR;
}

int AHardwareBuffer_lock(struct AHardwareBuffer *buffer, uint64_t usage, int32_t fence, const ARect *rect, void **outVirtualAddress)
{
	if (!buffer)
		return BAD_VALUE;
	if (usage & ~(USAGE_CPU_READ_MASK | USAGE_CPU_WRITE_MASK) || usage == 0)
		return BAD_VALUE;
	if (buffer->desc.layers > 1)
		return INVALID_OPERATION;
	const struct format_info *fi = format_info(buffer->desc.format);
	int ret = map(buffer, usage, fence);
	if (ret)
		return ret;
	*outVirtualAddress = rect_address(buffer, 0, rect, fi->bpp);
	return NO_ERROR;
}

int AHardwareBuffer_lockPlanes(struct AHardwareBuffer *buffer, uint64_t usage, int32_t fence, const ARect *rect, AHardwareBuffer_Planes *outPlanes)
{
	if (!buffer || !outPlanes)
		return BAD_VALUE;
	if (usage & ~(USAGE_CPU_READ_MASK | USAGE_CPU_WRITE_MASK) || usage == 0)
		return BAD_VALUE;
	const struct format_info *fi = format_info(buffer->desc.format);
	memset(outPlanes->planes, 0, sizeof(outPlanes->planes));
	int ret = map(buffer, usage, fence);
	if (ret) {
		outPlanes->planeCount = 0;
		return ret;
	}
	if (!fi->yuv) {
		outPlanes->planeCount = 1;
		outPlanes->planes[0].data = rect_address(buffer, 0, rect, fi->bpp);
		outPlanes->planes[0].pixelStride = fi->bpp;
		outPlanes->planes[0].rowStride = buffer->strides[0];
		return NO_ERROR;
	}
	/* android_ycbcr of gralloc: Y, Cb, Cr; interleaved chroma shares one plane with chroma_step 2 (4 for P010) */
	ARect crect = rect ? (ARect){rect->left / 2, fi->yuv == 42 ? rect->top / 2 : rect->top, 0, 0} : (ARect){0};
	outPlanes->planeCount = 3;
	outPlanes->planes[0].data = rect_address(buffer, 0, rect, fi->bpp);
	outPlanes->planes[0].pixelStride = fi->bpp;
	outPlanes->planes[0].rowStride = buffer->strides[0];
	uint32_t step = fi->chroma_step;
	for (int i = 1; i <= 2; i++) {
		int plane;
		size_t extra;
		if (fi->chroma_step == 1) { /* YV12: plane 1 is Cr, plane 2 Cb */
			plane = i == 1 ? 2 : 1;
			extra = 0;
		} else {
			plane = 1;
			/* NV12/P010: Cb first; NV21: Cr first */
			extra = ((i == 2) != fi->cr_first) ? step / 2 : 0;
		}
		outPlanes->planes[i].data = (uint8_t *)rect_address(buffer, plane, rect ? &crect : NULL, step) + extra;
		outPlanes->planes[i].pixelStride = step;
		outPlanes->planes[i].rowStride = buffer->strides[plane];
	}
	return NO_ERROR;
}

int AHardwareBuffer_unlock(struct AHardwareBuffer *buffer, int32_t *fence)
{
	if (!buffer)
		return BAD_VALUE;
	pthread_mutex_lock(&buffer->lock);
	if (buffer->lock_count == 0) {
		pthread_mutex_unlock(&buffer->lock);
		return BAD_VALUE;
	}
	if (buffer->dmabuf) {
		struct dma_buf_sync sync = {.flags = DMA_BUF_SYNC_END};
		sync.flags |= (buffer->lock_usage & USAGE_CPU_READ_MASK) ? DMA_BUF_SYNC_READ : 0;
		sync.flags |= (buffer->lock_usage & USAGE_CPU_WRITE_MASK) ? DMA_BUF_SYNC_WRITE : 0;
		ioctl(buffer->fd, DMA_BUF_IOCTL_SYNC, &sync);
	}
	buffer->lock_count--;
	pthread_mutex_unlock(&buffer->lock);
	/* the CPU access is over when unlock() returns: no release fence */
	if (fence)
		*fence = -1;
	return NO_ERROR;
}

/* what goes over the socket besides the fd (both ends are ATL's libandroid) */
struct wire {
	uint32_t magic;
	AHardwareBuffer_Desc desc;
	uint32_t drm_format;
	uint64_t modifier;
	uint64_t id;
	uint64_t size;
	int32_t plane_count;
	uint32_t offsets[AHB_MAX_PLANES];
	uint32_t strides[AHB_MAX_PLANES];
	uint8_t dmabuf;
};

int AHardwareBuffer_sendHandleToUnixSocket(const struct AHardwareBuffer *buffer, int socketFd)
{
	if (!buffer)
		return BAD_VALUE;
	struct wire w = {.magic = AHB_MAGIC, .desc = buffer->desc, .drm_format = buffer->drm_format, .modifier = buffer->modifier,
	                 .id = buffer->id, .size = buffer->size, .plane_count = buffer->plane_count, .dmabuf = buffer->dmabuf};
	memcpy(w.offsets, buffer->offsets, sizeof(w.offsets));
	memcpy(w.strides, buffer->strides, sizeof(w.strides));
	struct iovec iov = {&w, sizeof(w)};
	char control[CMSG_SPACE(sizeof(int))] = {0};
	struct msghdr msg = {.msg_iov = &iov, .msg_iovlen = 1, .msg_control = control, .msg_controllen = sizeof(control)};
	struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
	cmsg->cmsg_level = SOL_SOCKET;
	cmsg->cmsg_type = SCM_RIGHTS;
	cmsg->cmsg_len = CMSG_LEN(sizeof(int));
	memcpy(CMSG_DATA(cmsg), &buffer->fd, sizeof(int));
	ssize_t n;
	do {
		n = sendmsg(socketFd, &msg, MSG_NOSIGNAL);
	} while (n < 0 && errno == EINTR);
	return n < 0 ? -errno : NO_ERROR;
}

int AHardwareBuffer_recvHandleFromUnixSocket(int socketFd, struct AHardwareBuffer **outBuffer)
{
	if (!outBuffer)
		return BAD_VALUE;
	struct wire w;
	struct iovec iov = {&w, sizeof(w)};
	char control[CMSG_SPACE(sizeof(int))];
	struct msghdr msg = {.msg_iov = &iov, .msg_iovlen = 1, .msg_control = control, .msg_controllen = sizeof(control)};
	ssize_t n;
	do {
		n = recvmsg(socketFd, &msg, MSG_CMSG_CLOEXEC);
	} while (n < 0 && errno == EINTR);
	if (n < 0)
		return -errno;
	struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
	int fd = -1;
	if (cmsg && cmsg->cmsg_level == SOL_SOCKET && cmsg->cmsg_type == SCM_RIGHTS)
		memcpy(&fd, CMSG_DATA(cmsg), sizeof(int));
	if (n != sizeof(w) || w.magic != AHB_MAGIC || fd < 0) {
		if (fd >= 0)
			close(fd);
		return INVALID_OPERATION;
	}
	struct AHardwareBuffer *buffer = buffer_new(&w.desc);
	buffer->fd = fd;
	buffer->drm_format = w.drm_format;
	buffer->modifier = w.modifier;
	buffer->id = w.id; /* the same buffer: getId() is equal in both processes */
	buffer->size = w.size;
	buffer->plane_count = w.plane_count;
	buffer->dmabuf = w.dmabuf;
	memcpy(buffer->offsets, w.offsets, sizeof(w.offsets));
	memcpy(buffer->strides, w.strides, sizeof(w.strides));
	fill_anwb(buffer);
	*outBuffer = buffer;
	return NO_ERROR;
}

int AHardwareBuffer_getId(const struct AHardwareBuffer *buffer, uint64_t *outId)
{
	if (!buffer || !outId)
		return BAD_VALUE;
	*outId = buffer->id;
	return NO_ERROR;
}

const struct native_handle *AHardwareBuffer_getNativeHandle(const struct AHardwareBuffer *buffer)
{
	return buffer ? buffer->native_handle : NULL;
}
