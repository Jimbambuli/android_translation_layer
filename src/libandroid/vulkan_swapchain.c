/*
 * VK_KHR_android_surface and VK_KHR_swapchain on the window of a Surface without a SurfaceView widget of this
 * process: the Surface of a SurfaceTexture (TextureView, new Surface(SurfaceTexture)) or of another process
 * (ANativeWindow.remote_fd), whose buffers go over a socket to the consumer (android_view_SurfaceView.h).
 *
 * AOSP: frameworks/native/vulkan/libvulkan/swapchain.cpp builds the swapchain on top of the ANativeWindow; its images
 * are the window's buffers, vkAcquireNextImageKHR() dequeues one, vkQueuePresentKHR() queues it. Here each image is a
 * linear host visible VkImage with a memfd buffer of the window of the same index: vkQueuePresentKHR() waits for the
 * app's semaphores and copies the image into that buffer (like Mesa's WSI for software rendering), which then goes to
 * the consumer; the image can be acquired again once the consumer gave the buffer back (RELEASE).
 */

#define _GNU_SOURCE // memfd_create()

#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

#include <vulkan/vulkan.h>
#include <vulkan/vulkan_android.h>

#include <glib.h>

#include "../api-impl-jni/widgets/android_view_SurfaceView.h"
#include "native_window.h"

/* AOSP: BufferQueue's min_undequeued_buffers (1) + 2; at most the buffers of the socket protocol */
#define MIN_IMAGES 3
#define MAX_IMAGES ATL_SURFACE_MAX_BUFFERS

struct remote_surface {
	struct ANativeWindow *window;
	VkSwapchainKHR swapchain; /* the active one (AOSP Surface::swapchain_handle) */
};

struct remote_image {
	VkImage image;
	VkDeviceMemory memory;
	void *map;
	VkSubresourceLayout layout;
	gboolean coherent;
	/* the window's buffer of the same index */
	int fd;
	void *buffer;
	size_t size;
	gboolean acquired; /* by the app */
	gint busy; /* the consumer has the buffer (until its RELEASE) */
};

struct remote_swapchain {
	VkDevice device;
	struct remote_surface *surface;
	VkExtent2D extent;
	VkFence fence; /* vkQueuePresentKHR() waits for the app's semaphores with it */
	uint32_t num_images;
	struct remote_image images[MAX_IMAGES];
};

static GMutex lock;
static GHashTable *surfaces; /* VkSurfaceKHR -> struct remote_surface */
static GHashTable *swapchains; /* VkSwapchainKHR -> struct remote_swapchain */
static GHashTable *device_physical; /* VkDevice -> VkPhysicalDevice */

static struct remote_surface *find_surface(VkSurfaceKHR surface)
{
	g_mutex_lock(&lock);
	struct remote_surface *ret = surfaces ? g_hash_table_lookup(surfaces, (void *)surface) : NULL;
	g_mutex_unlock(&lock);
	return ret;
}

static struct remote_swapchain *find_swapchain(VkSwapchainKHR swapchain)
{
	g_mutex_lock(&lock);
	struct remote_swapchain *ret = swapchains ? g_hash_table_lookup(swapchains, (void *)swapchain) : NULL;
	g_mutex_unlock(&lock);
	return ret;
}

static void table_set(GHashTable **table, void *key, void *value)
{
	g_mutex_lock(&lock);
	if (!*table)
		*table = g_hash_table_new(NULL, NULL);
	if (value)
		g_hash_table_insert(*table, key, value);
	else
		g_hash_table_remove(*table, key);
	g_mutex_unlock(&lock);
}

/* called from bionic_vkCreateAndroidSurfaceKHR() for a window with remote_fd; holds a reference on the window */
VkResult atl_vk_create_remote_surface(VkInstance instance, struct ANativeWindow *window, VkSurfaceKHR *out_surface)
{
	struct remote_surface *surface = g_new0(struct remote_surface, 1);
	ANativeWindow_acquire(window);
	surface->window = window;
	table_set(&surfaces, surface, surface);
	*out_surface = (VkSurfaceKHR)surface;
	return VK_SUCCESS;
}

/* TRUE if surface was one of atl_vk_create_remote_surface() */
gboolean atl_vk_destroy_remote_surface(VkSurfaceKHR handle)
{
	struct remote_surface *surface = find_surface(handle);
	if (!surface)
		return FALSE;
	table_set(&surfaces, surface, NULL);
	ANativeWindow_release(surface->window);
	g_free(surface);
	return TRUE;
}

static VkResult atl_vkGetPhysicalDeviceSurfaceSupportKHR(VkPhysicalDevice pdev, uint32_t queue_family, VkSurfaceKHR surface, VkBool32 *supported)
{
	if (!find_surface(surface))
		return vkGetPhysicalDeviceSurfaceSupportKHR(pdev, queue_family, surface, supported);
	*supported = VK_TRUE; // AOSP
	return VK_SUCCESS;
}

/* AOSP GetPhysicalDeviceSurfaceCapabilities2KHR() for a window without a transform hint */
static void remote_capabilities(struct remote_surface *surface, VkSurfaceCapabilitiesKHR *capabilities)
{
	uint32_t width = ANativeWindow_getWidth(surface->window);
	uint32_t height = ANativeWindow_getHeight(surface->window);
	capabilities->minImageCount = MIN_IMAGES;
	capabilities->maxImageCount = MAX_IMAGES;
	capabilities->currentExtent = (VkExtent2D){width, height};
	capabilities->minImageExtent = (VkExtent2D){1, 1};
	capabilities->maxImageExtent = (VkExtent2D){MAX(4096, width), MAX(4096, height)};
	capabilities->maxImageArrayLayers = 1;
	/* AOSP kSupportedTransforms (MapLibre treats a surface with only IDENTITY as not set up yet) */
	capabilities->supportedTransforms = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR | VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR |
	                                    VK_SURFACE_TRANSFORM_ROTATE_180_BIT_KHR | VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR |
	                                    VK_SURFACE_TRANSFORM_HORIZONTAL_MIRROR_BIT_KHR | VK_SURFACE_TRANSFORM_HORIZONTAL_MIRROR_ROTATE_90_BIT_KHR |
	                                    VK_SURFACE_TRANSFORM_HORIZONTAL_MIRROR_ROTATE_180_BIT_KHR | VK_SURFACE_TRANSFORM_HORIZONTAL_MIRROR_ROTATE_270_BIT_KHR |
	                                    VK_SURFACE_TRANSFORM_INHERIT_BIT_KHR;
	capabilities->currentTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
	capabilities->supportedCompositeAlpha = VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR;
	capabilities->supportedUsageFlags = VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT |
	                                    VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT |
	                                    VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT;
}

/* TRUE if surface was one of atl_vk_create_remote_surface() */
gboolean atl_vk_remote_surface_capabilities(VkSurfaceKHR handle, VkSurfaceCapabilitiesKHR *capabilities)
{
	struct remote_surface *surface = find_surface(handle);
	if (surface)
		remote_capabilities(surface, capabilities);
	return surface != NULL;
}

static VkResult atl_vkGetPhysicalDeviceSurfaceCapabilities2KHR(VkPhysicalDevice pdev, const VkPhysicalDeviceSurfaceInfo2KHR *info, VkSurfaceCapabilities2KHR *capabilities)
{
	struct remote_surface *surface = find_surface(info->surface);
	if (!surface)
		return vkGetPhysicalDeviceSurfaceCapabilities2KHR(pdev, info, capabilities);
	remote_capabilities(surface, &capabilities->surfaceCapabilities);
	return VK_SUCCESS;
}

static VkResult atl_vkGetPhysicalDeviceSurfaceFormatsKHR(VkPhysicalDevice pdev, VkSurfaceKHR surface, uint32_t *count, VkSurfaceFormatKHR *formats)
{
	if (!find_surface(surface))
		return vkGetPhysicalDeviceSurfaceFormatsKHR(pdev, surface, count, formats);
	/* AOSP: R8G8B8A8 always, the other formats only with an AHardwareBuffer of that format (none here) */
	static const VkSurfaceFormatKHR all[] = {
		{VK_FORMAT_R8G8B8A8_UNORM, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR},
		{VK_FORMAT_R8G8B8A8_SRGB, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR},
	};
	if (!formats) {
		*count = G_N_ELEMENTS(all);
		return VK_SUCCESS;
	}
	uint32_t n = MIN(*count, G_N_ELEMENTS(all));
	memcpy(formats, all, n * sizeof(*formats));
	*count = n;
	return n < G_N_ELEMENTS(all) ? VK_INCOMPLETE : VK_SUCCESS;
}

static VkResult atl_vkGetPhysicalDeviceSurfaceFormats2KHR(VkPhysicalDevice pdev, const VkPhysicalDeviceSurfaceInfo2KHR *info, uint32_t *count, VkSurfaceFormat2KHR *formats)
{
	if (!find_surface(info->surface))
		return vkGetPhysicalDeviceSurfaceFormats2KHR(pdev, info, count, formats);
	if (!formats)
		return atl_vkGetPhysicalDeviceSurfaceFormatsKHR(pdev, info->surface, count, NULL);
	VkSurfaceFormatKHR plain[2];
	uint32_t n = MIN(*count, G_N_ELEMENTS(plain));
	VkResult ret = atl_vkGetPhysicalDeviceSurfaceFormatsKHR(pdev, info->surface, &n, plain);
	for (uint32_t i = 0; i < n; i++)
		formats[i].surfaceFormat = plain[i];
	*count = n;
	return ret;
}

static VkResult atl_vkGetPhysicalDeviceSurfacePresentModesKHR(VkPhysicalDevice pdev, VkSurfaceKHR surface, uint32_t *count, VkPresentModeKHR *modes)
{
	if (!find_surface(surface))
		return vkGetPhysicalDeviceSurfacePresentModesKHR(pdev, surface, count, modes);
	/* AOSP: MAILBOX when min_undequeued_buffers + 1 < max_buffer_count, then FIFO */
	static const VkPresentModeKHR all[] = {VK_PRESENT_MODE_MAILBOX_KHR, VK_PRESENT_MODE_FIFO_KHR};
	if (!modes) {
		*count = G_N_ELEMENTS(all);
		return VK_SUCCESS;
	}
	uint32_t n = MIN(*count, G_N_ELEMENTS(all));
	memcpy(modes, all, n * sizeof(*modes));
	*count = n;
	return n < G_N_ELEMENTS(all) ? VK_INCOMPLETE : VK_SUCCESS;
}

static VkResult atl_vkGetPhysicalDevicePresentRectanglesKHR(VkPhysicalDevice pdev, VkSurfaceKHR handle, uint32_t *count, VkRect2D *rects)
{
	struct remote_surface *surface = find_surface(handle);
	if (!surface)
		return vkGetPhysicalDevicePresentRectanglesKHR(pdev, handle, count, rects);
	if (!rects) {
		*count = 1;
		return VK_SUCCESS;
	}
	if (*count < 1)
		return VK_INCOMPLETE;
	*count = 1;
	rects[0] = (VkRect2D){{0, 0}, {ANativeWindow_getWidth(surface->window), ANativeWindow_getHeight(surface->window)}};
	return VK_SUCCESS;
}

static VkResult atl_vkGetDeviceGroupSurfacePresentModesKHR(VkDevice device, VkSurfaceKHR surface, VkDeviceGroupPresentModeFlagsKHR *modes)
{
	if (!find_surface(surface))
		return vkGetDeviceGroupSurfacePresentModesKHR(device, surface, modes);
	*modes = VK_DEVICE_GROUP_PRESENT_MODE_LOCAL_BIT_KHR; // AOSP
	return VK_SUCCESS;
}

/* vkAcquireNextImageKHR() signals the app's semaphore and fence at once (the image is free on the CPU), as Mesa's
 * AcquireImageANDROID() does with the fence fd -1 of an idle buffer: a temporary import of an already signaled
 * sync file; the device gets these extensions in vkCreateDevice() */
static const char *const sync_extensions[] = {
	"VK_KHR_external_semaphore", "VK_KHR_external_semaphore_fd", "VK_KHR_external_fence", "VK_KHR_external_fence_fd",
};

static VkResult atl_vkCreateDevice(VkPhysicalDevice pdev, const VkDeviceCreateInfo *create_info, const VkAllocationCallbacks *allocator, VkDevice *device)
{
	uint32_t available_count = 0;
	vkEnumerateDeviceExtensionProperties(pdev, NULL, &available_count, NULL);
	VkExtensionProperties *available = g_new(VkExtensionProperties, available_count);
	vkEnumerateDeviceExtensionProperties(pdev, NULL, &available_count, available);
	const char **names = g_new(const char *, create_info->enabledExtensionCount + G_N_ELEMENTS(sync_extensions));
	uint32_t count = create_info->enabledExtensionCount;
	memcpy(names, create_info->ppEnabledExtensionNames, count * sizeof(*names));
	for (int i = 0; i < G_N_ELEMENTS(sync_extensions); i++) {
		gboolean supported = FALSE, enabled = FALSE;
		for (uint32_t k = 0; k < available_count; k++)
			supported |= !strcmp(available[k].extensionName, sync_extensions[i]);
		for (uint32_t k = 0; k < count; k++)
			enabled |= !strcmp(names[k], sync_extensions[i]);
		if (supported && !enabled)
			names[count++] = sync_extensions[i];
	}
	VkDeviceCreateInfo info = *create_info;
	info.enabledExtensionCount = count;
	info.ppEnabledExtensionNames = names;
	VkResult ret = vkCreateDevice(pdev, &info, allocator, device);
	g_free(names);
	g_free(available);
	if (ret == VK_SUCCESS)
		table_set(&device_physical, *device, pdev);
	return ret;
}

static void atl_vkDestroyDevice(VkDevice device, const VkAllocationCallbacks *allocator)
{
	table_set(&device_physical, device, NULL);
	vkDestroyDevice(device, allocator);
}

/* ANativeWindow_remoteRead(), with remote_lock held: the consumer gave a buffer back */
static void remote_message(void *data, const struct atl_surface_msg *msg)
{
	struct remote_swapchain *swapchain = data;
	if (msg->type == ATL_SURFACE_MSG_RELEASE && msg->index < swapchain->num_images)
		g_atomic_int_set(&swapchain->images[msg->index].busy, FALSE);
}

static void free_swapchain(struct remote_swapchain *swapchain, const VkAllocationCallbacks *allocator)
{
	VkDevice device = swapchain->device;
	for (uint32_t i = 0; i < MAX_IMAGES; i++) {
		struct remote_image *image = &swapchain->images[i];
		if (image->image)
			vkDestroyImage(device, image->image, allocator);
		if (image->memory)
			vkFreeMemory(device, image->memory, allocator); // unmaps it
		if (image->buffer)
			munmap(image->buffer, image->size);
		if (image->fd >= 0)
			close(image->fd);
	}
	if (swapchain->fence)
		vkDestroyFence(device, swapchain->fence, allocator);
	g_free(swapchain);
}

static VkResult create_image(struct remote_swapchain *swapchain, VkPhysicalDevice pdev, const VkSwapchainCreateInfoKHR *create_info, const VkAllocationCallbacks *allocator, uint32_t index)
{
	VkDevice device = swapchain->device;
	struct remote_image *image = &swapchain->images[index];
	VkImageCreateInfo image_info = {
		.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO,
		.flags = (create_info->flags & VK_SWAPCHAIN_CREATE_MUTABLE_FORMAT_BIT_KHR) ? VK_IMAGE_CREATE_MUTABLE_FORMAT_BIT | VK_IMAGE_CREATE_EXTENDED_USAGE_BIT : 0,
		.imageType = VK_IMAGE_TYPE_2D,
		.format = create_info->imageFormat,
		.extent = {create_info->imageExtent.width, create_info->imageExtent.height, 1},
		.mipLevels = 1,
		.arrayLayers = create_info->imageArrayLayers,
		.samples = VK_SAMPLE_COUNT_1_BIT,
		.tiling = VK_IMAGE_TILING_LINEAR,
		.usage = create_info->imageUsage,
		.sharingMode = create_info->imageSharingMode,
		.queueFamilyIndexCount = create_info->queueFamilyIndexCount,
		.pQueueFamilyIndices = create_info->pQueueFamilyIndices,
		.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
	};
	VkResult ret = vkCreateImage(device, &image_info, allocator, &image->image);
	if (ret != VK_SUCCESS)
		return ret;
	VkMemoryRequirements requirements;
	vkGetImageMemoryRequirements(device, image->image, &requirements);
	VkPhysicalDeviceMemoryProperties properties;
	vkGetPhysicalDeviceMemoryProperties(pdev, &properties);
	int type = -1;
	for (uint32_t i = 0; i < properties.memoryTypeCount; i++) {
		VkMemoryPropertyFlags flags = properties.memoryTypes[i].propertyFlags;
		if (!(requirements.memoryTypeBits & (1u << i)) || !(flags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT))
			continue;
		/* the CPU reads it: cached memory if there is any */
		if (type < 0 || ((flags & VK_MEMORY_PROPERTY_HOST_CACHED_BIT) && !(properties.memoryTypes[type].propertyFlags & VK_MEMORY_PROPERTY_HOST_CACHED_BIT)))
			type = i;
	}
	if (type < 0) {
		fprintf(stderr, "vkCreateSwapchainKHR: no host visible memory for a linear image\n");
		return VK_ERROR_OUT_OF_DEVICE_MEMORY;
	}
	image->coherent = !!(properties.memoryTypes[type].propertyFlags & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
	VkMemoryAllocateInfo allocate_info = {.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO, .allocationSize = requirements.size, .memoryTypeIndex = type};
	if ((ret = vkAllocateMemory(device, &allocate_info, allocator, &image->memory)) != VK_SUCCESS ||
	    (ret = vkBindImageMemory(device, image->image, image->memory, 0)) != VK_SUCCESS ||
	    (ret = vkMapMemory(device, image->memory, 0, VK_WHOLE_SIZE, 0, &image->map)) != VK_SUCCESS)
		return ret;
	vkGetImageSubresourceLayout(device, image->image, &(VkImageSubresource){.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT}, &image->layout);

	/* the window's buffer: rows of 4 bytes per pixel, top first, as those of ANativeWindow_remotePresentTexture() */
	uint32_t width = create_info->imageExtent.width;
	uint32_t height = create_info->imageExtent.height;
	image->size = (size_t)width * 4 * height;
	image->fd = memfd_create("atl-surface", MFD_CLOEXEC);
	if (image->fd < 0 || ftruncate(image->fd, image->size) < 0 ||
	    (image->buffer = mmap(NULL, image->size, PROT_READ | PROT_WRITE, MAP_SHARED, image->fd, 0)) == MAP_FAILED) {
		fprintf(stderr, "vkCreateSwapchainKHR: no buffer for the window: %m\n");
		image->buffer = NULL;
		return VK_ERROR_OUT_OF_HOST_MEMORY;
	}
	ANativeWindow_remoteSend(swapchain->surface->window, &(struct atl_surface_msg){.type = ATL_SURFACE_MSG_BUFFER, .index = index, .width = width, .height = height, .stride = width * 4, .top_first = 1}, image->fd);
	return VK_SUCCESS;
}

static VkResult atl_vkCreateSwapchainKHR(VkDevice device, const VkSwapchainCreateInfoKHR *create_info, const VkAllocationCallbacks *allocator, VkSwapchainKHR *out_swapchain)
{
	struct remote_surface *surface = find_surface(create_info->surface);
	if (!surface)
		return vkCreateSwapchainKHR(device, create_info, allocator, out_swapchain);

	g_mutex_lock(&lock);
	VkPhysicalDevice pdev = device_physical ? g_hash_table_lookup(device_physical, device) : NULL;
	g_mutex_unlock(&lock);
	if (!pdev) {
		fprintf(stderr, "vkCreateSwapchainKHR: device %p was not created through vkCreateDevice()\n", device);
		return VK_ERROR_INITIALIZATION_FAILED;
	}
	/* AOSP: only formats with a native pixel format and the sRGB color space */
	if ((create_info->imageFormat != VK_FORMAT_R8G8B8A8_UNORM && create_info->imageFormat != VK_FORMAT_R8G8B8A8_SRGB) ||
	    create_info->imageColorSpace != VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
		fprintf(stderr, "vkCreateSwapchainKHR: format %d / color space %d not supported\n", create_info->imageFormat, create_info->imageColorSpace);
		return VK_ERROR_INITIALIZATION_FAILED;
	}
	if (create_info->preTransform != VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR && create_info->preTransform != VK_SURFACE_TRANSFORM_INHERIT_BIT_KHR)
		fprintf(stderr, "vkCreateSwapchainKHR: preTransform %#x is not applied\n", create_info->preTransform);
	/* AOSP: a surface has one swapchain, a new one replaces it only as its oldSwapchain */
	if (surface->swapchain != create_info->oldSwapchain)
		return VK_ERROR_NATIVE_WINDOW_IN_USE_KHR;
	struct remote_swapchain *old = find_swapchain(create_info->oldSwapchain);
	if (old)
		old->surface = NULL; // orphaned (AOSP OrphanSwapchain())
	surface->swapchain = VK_NULL_HANDLE;
	ANativeWindow_remoteSetHandler(surface->window, NULL, NULL);

	struct remote_swapchain *swapchain = g_new0(struct remote_swapchain, 1);
	swapchain->device = device;
	swapchain->surface = surface;
	swapchain->extent = create_info->imageExtent;
	swapchain->num_images = CLAMP(create_info->minImageCount, 2, MAX_IMAGES);
	for (uint32_t i = 0; i < MAX_IMAGES; i++)
		swapchain->images[i].fd = -1;
	VkResult ret = vkCreateFence(device, &(VkFenceCreateInfo){.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO}, allocator, &swapchain->fence);
	for (uint32_t i = 0; i < swapchain->num_images && ret == VK_SUCCESS; i++)
		ret = create_image(swapchain, pdev, create_info, allocator, i);
	if (ret != VK_SUCCESS) {
		free_swapchain(swapchain, allocator);
		return ret;
	}
	ANativeWindow_remoteSetHandler(surface->window, remote_message, swapchain);
	table_set(&swapchains, swapchain, swapchain);
	surface->swapchain = (VkSwapchainKHR)swapchain;
	*out_swapchain = (VkSwapchainKHR)swapchain;
	fprintf(stderr, "vkCreateSwapchainKHR: %u images %ux%u on the window of a SurfaceTexture or of another process\n", swapchain->num_images, swapchain->extent.width, swapchain->extent.height);
	return VK_SUCCESS;
}

static void atl_vkDestroySwapchainKHR(VkDevice device, VkSwapchainKHR handle, const VkAllocationCallbacks *allocator)
{
	struct remote_swapchain *swapchain = find_swapchain(handle);
	if (!swapchain) {
		vkDestroySwapchainKHR(device, handle, allocator);
		return;
	}
	table_set(&swapchains, swapchain, NULL);
	if (swapchain->surface && swapchain->surface->swapchain == handle) {
		swapchain->surface->swapchain = VK_NULL_HANDLE;
		ANativeWindow_remoteSetHandler(swapchain->surface->window, NULL, NULL);
	}
	free_swapchain(swapchain, allocator);
}

static VkResult atl_vkGetSwapchainImagesKHR(VkDevice device, VkSwapchainKHR handle, uint32_t *count, VkImage *images)
{
	struct remote_swapchain *swapchain = find_swapchain(handle);
	if (!swapchain)
		return vkGetSwapchainImagesKHR(device, handle, count, images);
	if (!images) {
		*count = swapchain->num_images;
		return VK_SUCCESS;
	}
	uint32_t n = MIN(*count, swapchain->num_images);
	for (uint32_t i = 0; i < n; i++)
		images[i] = swapchain->images[i].image;
	*count = n;
	return n < swapchain->num_images ? VK_INCOMPLETE : VK_SUCCESS;
}

static VkResult signal_acquired(VkDevice device, VkSemaphore semaphore, VkFence fence)
{
	PFN_vkImportSemaphoreFdKHR import_semaphore = (PFN_vkImportSemaphoreFdKHR)vkGetDeviceProcAddr(device, "vkImportSemaphoreFdKHR");
	PFN_vkImportFenceFdKHR import_fence = (PFN_vkImportFenceFdKHR)vkGetDeviceProcAddr(device, "vkImportFenceFdKHR");
	VkResult ret = VK_SUCCESS;
	if (semaphore != VK_NULL_HANDLE)
		ret = import_semaphore ? import_semaphore(device, &(VkImportSemaphoreFdInfoKHR){.sType = VK_STRUCTURE_TYPE_IMPORT_SEMAPHORE_FD_INFO_KHR, .semaphore = semaphore, .flags = VK_SEMAPHORE_IMPORT_TEMPORARY_BIT, .handleType = VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_SYNC_FD_BIT, .fd = -1})
		                       : VK_ERROR_FEATURE_NOT_PRESENT;
	if (fence != VK_NULL_HANDLE && ret == VK_SUCCESS)
		ret = import_fence ? import_fence(device, &(VkImportFenceFdInfoKHR){.sType = VK_STRUCTURE_TYPE_IMPORT_FENCE_FD_INFO_KHR, .fence = fence, .flags = VK_FENCE_IMPORT_TEMPORARY_BIT, .handleType = VK_EXTERNAL_FENCE_HANDLE_TYPE_SYNC_FD_BIT, .fd = -1})
		                   : VK_ERROR_FEATURE_NOT_PRESENT;
	if (ret != VK_SUCCESS)
		fprintf(stderr, "vkAcquireNextImageKHR: can't signal the semaphore/fence (sync fd import): %d\n", ret);
	return ret;
}

static VkResult atl_vkAcquireNextImageKHR(VkDevice device, VkSwapchainKHR handle, uint64_t timeout, VkSemaphore semaphore, VkFence fence, uint32_t *index)
{
	struct remote_swapchain *swapchain = find_swapchain(handle);
	if (!swapchain)
		return vkAcquireNextImageKHR(device, handle, timeout, semaphore, fence, index);
	if (!swapchain->surface || swapchain->surface->swapchain != handle)
		return VK_ERROR_OUT_OF_DATE_KHR; // AOSP
	struct ANativeWindow *window = swapchain->surface->window;
	gint64 start = g_get_monotonic_time();
	gint64 end = timeout >= G_MAXINT64 / 1000 ? G_MAXINT64 : start + (gint64)(timeout / 1000);
	int i = -1;
	for (;;) {
		gboolean alive = ANativeWindow_remoteRead(window, 0);
		for (uint32_t k = 0; k < swapchain->num_images && i < 0; k++)
			if (!swapchain->images[k].acquired && !g_atomic_int_get(&swapchain->images[k].busy))
				i = k;
		if (i >= 0)
			break;
		if (!alive)
			return VK_ERROR_SURFACE_LOST_KHR; // AOSP: dequeueBuffer() failed
		if (timeout == 0)
			return VK_NOT_READY;
		gint64 now = g_get_monotonic_time();
		/* the buffers come back on the main thread: a render thread waited for by the main thread (MapLibre's when
		 * its TextureView goes) must not wait for that forever; like eglSwapBuffers() on such a window, it then draws
		 * into an image whose frame is dropped (vkQueuePresentKHR() of an image the consumer still has) */
		if (end == G_MAXINT64 && now - start >= 100000) {
			for (uint32_t k = 0; k < swapchain->num_images && i < 0; k++)
				if (!swapchain->images[k].acquired)
					i = k;
			if (i < 0)
				return VK_NOT_READY;
			break;
		}
		if (now >= end)
			return VK_TIMEOUT;
		ANativeWindow_remoteRead(window, MAX(1, (MIN(end, start + 100000) - now) / 1000));
	}
	VkResult ret = signal_acquired(device, semaphore, fence);
	if (ret != VK_SUCCESS)
		return ret;
	swapchain->images[i].acquired = TRUE;
	*index = i;
	return VK_SUCCESS;
}

static VkResult atl_vkAcquireNextImage2KHR(VkDevice device, const VkAcquireNextImageInfoKHR *info, uint32_t *index)
{
	if (!find_swapchain(info->swapchain))
		return vkAcquireNextImage2KHR(device, info, index);
	return atl_vkAcquireNextImageKHR(device, info->swapchain, info->timeout, info->semaphore, info->fence, index);
}

/* the image goes into the window's buffer of the same index, which goes to the consumer */
static VkResult present_one(struct remote_swapchain *swapchain, VkSwapchainKHR handle, uint32_t index)
{
	struct remote_image *image = &swapchain->images[index];
	image->acquired = FALSE;
	if (!swapchain->surface || swapchain->surface->swapchain != handle)
		return VK_ERROR_OUT_OF_DATE_KHR; // AOSP
	struct ANativeWindow *window = swapchain->surface->window;
	if (!g_atomic_int_get(&image->busy)) {
		if (!image->coherent)
			vkInvalidateMappedMemoryRanges(swapchain->device, 1, &(VkMappedMemoryRange){.sType = VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE, .memory = image->memory, .size = VK_WHOLE_SIZE});
		size_t row = (size_t)swapchain->extent.width * 4;
		for (uint32_t y = 0; y < swapchain->extent.height; y++)
			memcpy((char *)image->buffer + y * row, (char *)image->map + image->layout.offset + y * image->layout.rowPitch, row);
		g_atomic_int_set(&image->busy, TRUE);
		ANativeWindow_remoteSend(window, &(struct atl_surface_msg){.type = ATL_SURFACE_MSG_PRESENT, .index = index}, -1);
	}
	return window->remote_gone ? VK_ERROR_SURFACE_LOST_KHR : VK_SUCCESS;
}

/* AOSP WorstPresentResult(): the ranking of the spec */
static VkResult worst_present_result(VkResult a, VkResult b)
{
	static const VkResult worst_to_best[] = {
		VK_ERROR_DEVICE_LOST, VK_ERROR_SURFACE_LOST_KHR, VK_ERROR_OUT_OF_DATE_KHR,
		VK_ERROR_OUT_OF_DEVICE_MEMORY, VK_ERROR_OUT_OF_HOST_MEMORY, VK_SUBOPTIMAL_KHR,
	};
	for (int i = 0; i < G_N_ELEMENTS(worst_to_best); i++)
		if (a == worst_to_best[i] || b == worst_to_best[i])
			return worst_to_best[i];
	return a != VK_SUCCESS ? a : b;
}

static VkResult atl_vkQueuePresentKHR(VkQueue queue, const VkPresentInfoKHR *info)
{
	struct remote_swapchain *first = NULL;
	for (uint32_t i = 0; i < info->swapchainCount && !first; i++)
		first = find_swapchain(info->pSwapchains[i]);
	if (!first)
		return vkQueuePresentKHR(queue, info);

	/* AOSP QueueSignalReleaseImageANDROID(): the image is ready once the app's semaphores are */
	VkPipelineStageFlags *stages = g_new(VkPipelineStageFlags, MAX(1, info->waitSemaphoreCount));
	for (uint32_t i = 0; i < info->waitSemaphoreCount; i++)
		stages[i] = VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
	VkSubmitInfo submit = {
		.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO,
		.waitSemaphoreCount = info->waitSemaphoreCount,
		.pWaitSemaphores = info->pWaitSemaphores,
		.pWaitDstStageMask = stages,
	};
	VkResult ret = vkQueueSubmit(queue, 1, &submit, first->fence);
	g_free(stages);
	if (ret == VK_SUCCESS)
		ret = vkWaitForFences(first->device, 1, &first->fence, VK_TRUE, UINT64_MAX);
	vkResetFences(first->device, 1, &first->fence);
	if (ret != VK_SUCCESS)
		return ret;

	/* swapchains of the host's WSI in the same call: without the semaphores, already waited for */
	VkSwapchainKHR *host_swapchains = g_new(VkSwapchainKHR, info->swapchainCount);
	uint32_t *host_indices = g_new(uint32_t, info->swapchainCount);
	VkResult *host_results = g_new(VkResult, info->swapchainCount);
	uint32_t host_count = 0;
	VkResult final = VK_SUCCESS;
	for (uint32_t i = 0; i < info->swapchainCount; i++) {
		struct remote_swapchain *swapchain = find_swapchain(info->pSwapchains[i]);
		if (!swapchain) {
			host_swapchains[host_count] = info->pSwapchains[i];
			host_indices[host_count++] = info->pImageIndices[i];
			continue;
		}
		VkResult result = present_one(swapchain, info->pSwapchains[i], info->pImageIndices[i]);
		if (info->pResults)
			info->pResults[i] = result;
		final = worst_present_result(final, result);
	}
	if (host_count) {
		VkPresentInfoKHR host_info = {.sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR, .swapchainCount = host_count, .pSwapchains = host_swapchains, .pImageIndices = host_indices, .pResults = host_results};
		VkResult result = vkQueuePresentKHR(queue, &host_info);
		for (uint32_t i = 0, k = 0; i < info->swapchainCount && info->pResults; i++)
			if (!find_swapchain(info->pSwapchains[i]))
				info->pResults[i] = host_results[k++];
		final = worst_present_result(final, result);
	}
	g_free(host_swapchains);
	g_free(host_indices);
	g_free(host_results);
	return final;
}

static PFN_vkVoidFunction atl_vkGetDeviceProcAddr(VkDevice device, const char *name);

static PFN_vkVoidFunction device_proc(const char *name)
{
#define PROC(fn)                    \
	if (!strcmp(name, #fn))     \
		return (PFN_vkVoidFunction)atl_##fn;
	PROC(vkGetDeviceProcAddr) // Vulkan-Hpp's init(device) asks for it again
	PROC(vkDestroyDevice)
	PROC(vkCreateSwapchainKHR)
	PROC(vkDestroySwapchainKHR)
	PROC(vkGetSwapchainImagesKHR)
	PROC(vkAcquireNextImageKHR)
	PROC(vkAcquireNextImage2KHR)
	PROC(vkQueuePresentKHR)
	PROC(vkGetDeviceGroupSurfacePresentModesKHR)
	return NULL;
}

static PFN_vkVoidFunction atl_vkGetDeviceProcAddr(VkDevice device, const char *name)
{
	PFN_vkVoidFunction host = vkGetDeviceProcAddr(device, name);
	PFN_vkVoidFunction own = host ? device_proc(name) : NULL; // only what the device has
	return own ? own : host;
}

/* for bionic_vkGetInstanceProcAddr(): the functions above where the host has them, NULL for the others */
PFN_vkVoidFunction atl_vk_remote_proc(VkInstance instance, const char *name)
{
	if (!instance || !vkGetInstanceProcAddr(instance, name))
		return NULL;
	PROC(vkCreateDevice)
	PROC(vkGetPhysicalDeviceSurfaceSupportKHR)
	PROC(vkGetPhysicalDeviceSurfaceCapabilities2KHR)
	PROC(vkGetPhysicalDeviceSurfaceFormatsKHR)
	PROC(vkGetPhysicalDeviceSurfaceFormats2KHR)
	PROC(vkGetPhysicalDeviceSurfacePresentModesKHR)
	PROC(vkGetPhysicalDevicePresentRectanglesKHR)
#undef PROC
	return device_proc(name);
}

/* the same for apps linking libvulkan.so directly */
#define EXPORT(ret, fn, params, args) \
	ret bionic_##fn params { return atl_##fn args; }
EXPORT(VkResult, vkCreateDevice, (VkPhysicalDevice p, const VkDeviceCreateInfo *c, const VkAllocationCallbacks *a, VkDevice *d), (p, c, a, d))
EXPORT(void, vkDestroyDevice, (VkDevice d, const VkAllocationCallbacks *a), (d, a))
EXPORT(PFN_vkVoidFunction, vkGetDeviceProcAddr, (VkDevice d, const char *n), (d, n))
EXPORT(VkResult, vkGetPhysicalDeviceSurfaceSupportKHR, (VkPhysicalDevice p, uint32_t q, VkSurfaceKHR s, VkBool32 *r), (p, q, s, r))
EXPORT(VkResult, vkGetPhysicalDeviceSurfaceCapabilities2KHR, (VkPhysicalDevice p, const VkPhysicalDeviceSurfaceInfo2KHR *i, VkSurfaceCapabilities2KHR *c), (p, i, c))
EXPORT(VkResult, vkGetPhysicalDeviceSurfaceFormatsKHR, (VkPhysicalDevice p, VkSurfaceKHR s, uint32_t *n, VkSurfaceFormatKHR *f), (p, s, n, f))
EXPORT(VkResult, vkGetPhysicalDeviceSurfaceFormats2KHR, (VkPhysicalDevice p, const VkPhysicalDeviceSurfaceInfo2KHR *i, uint32_t *n, VkSurfaceFormat2KHR *f), (p, i, n, f))
EXPORT(VkResult, vkGetPhysicalDeviceSurfacePresentModesKHR, (VkPhysicalDevice p, VkSurfaceKHR s, uint32_t *n, VkPresentModeKHR *m), (p, s, n, m))
EXPORT(VkResult, vkGetPhysicalDevicePresentRectanglesKHR, (VkPhysicalDevice p, VkSurfaceKHR s, uint32_t *n, VkRect2D *r), (p, s, n, r))
EXPORT(VkResult, vkGetDeviceGroupSurfacePresentModesKHR, (VkDevice d, VkSurfaceKHR s, VkDeviceGroupPresentModeFlagsKHR *m), (d, s, m))
EXPORT(VkResult, vkCreateSwapchainKHR, (VkDevice d, const VkSwapchainCreateInfoKHR *c, const VkAllocationCallbacks *a, VkSwapchainKHR *s), (d, c, a, s))
EXPORT(void, vkDestroySwapchainKHR, (VkDevice d, VkSwapchainKHR s, const VkAllocationCallbacks *a), (d, s, a))
EXPORT(VkResult, vkGetSwapchainImagesKHR, (VkDevice d, VkSwapchainKHR s, uint32_t *n, VkImage *i), (d, s, n, i))
EXPORT(VkResult, vkAcquireNextImageKHR, (VkDevice d, VkSwapchainKHR s, uint64_t t, VkSemaphore m, VkFence f, uint32_t *i), (d, s, t, m, f, i))
EXPORT(VkResult, vkAcquireNextImage2KHR, (VkDevice d, const VkAcquireNextImageInfoKHR *i, uint32_t *n), (d, i, n))
EXPORT(VkResult, vkQueuePresentKHR, (VkQueue q, const VkPresentInfoKHR *i), (q, i))
