/*
 * The camera NDK (libcamera2ndk, camera/NdkCameraManager.h and friends) for a computer without cameras, as ATL's
 * CameraManager reports (getCameraIdList() is empty): the camera id list is empty, a camera id is unknown
 * (ACAMERA_ERROR_INVALID_PARAMETER, as AOSP answers for an id it does not know), and since no device or capture
 * session can be opened, requests on one are invalid. Availability callbacks are accepted and never called.
 * The data objects (ACameraMetadata, ACaptureRequest, output targets and session outputs) are real.
 */

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

typedef int32_t camera_status_t;
#define ACAMERA_OK 0
#define ACAMERA_ERROR_INVALID_PARAMETER (-10000 - 1)
#define ACAMERA_ERROR_METADATA_NOT_FOUND (-10000 - 4)
#define ACAMERA_ERROR_INVALID_OPERATION (-10000 - 8)

typedef struct ACameraManager {
	int unused;
} ACameraManager;

typedef struct ACameraIdList {
	int numCameras;
	const char **cameraIds;
} ACameraIdList;

typedef struct {
	void *context;
	void *onCameraAvailable;
	void *onCameraUnavailable;
} ACameraManager_AvailabilityCallbacks;

typedef struct {
	ACameraManager_AvailabilityCallbacks availabilityCallbacks;
	void *onCameraAccessPrioritiesChanged;
	void *onPhysicalCameraAvailable;
	void *onPhysicalCameraUnavailable;
	void *reserved[4];
} ACameraManager_ExtendedAvailabilityCallbacks;

ACameraManager *ACameraManager_create(void)
{
	return calloc(1, sizeof(ACameraManager));
}

void ACameraManager_delete(ACameraManager *manager)
{
	free(manager);
}

camera_status_t ACameraManager_getCameraIdList(ACameraManager *manager, ACameraIdList **cameraIdList)
{
	if (!manager || !cameraIdList)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	*cameraIdList = calloc(1, sizeof(ACameraIdList));
	return ACAMERA_OK;
}

void ACameraManager_deleteCameraIdList(ACameraIdList *cameraIdList)
{
	free(cameraIdList);
}

camera_status_t ACameraManager_getCameraCharacteristics(ACameraManager *manager, const char *cameraId, void **characteristics)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraManager_registerAvailabilityCallback(ACameraManager *manager, const ACameraManager_AvailabilityCallbacks *callback)
{
	if (!manager || !callback || !callback->onCameraAvailable || !callback->onCameraUnavailable)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return ACAMERA_OK;
}

camera_status_t ACameraManager_unregisterAvailabilityCallback(ACameraManager *manager, const ACameraManager_AvailabilityCallbacks *callback)
{
	if (!manager || !callback)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return ACAMERA_OK;
}

camera_status_t ACameraManager_registerExtendedAvailabilityCallback(ACameraManager *manager, const ACameraManager_ExtendedAvailabilityCallbacks *callback)
{
	if (!manager || !callback || !callback->availabilityCallbacks.onCameraAvailable || !callback->availabilityCallbacks.onCameraUnavailable)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return ACAMERA_OK;
}

camera_status_t ACameraManager_unregisterExtendedAvailabilityCallback(ACameraManager *manager, const ACameraManager_ExtendedAvailabilityCallbacks *callback)
{
	if (!manager || !callback)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return ACAMERA_OK;
}

camera_status_t ACameraManager_openCamera(ACameraManager *manager, const char *cameraId, void *callback, void **device)
{
	if (device)
		*device = NULL;
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraManager_openSharedCamera(ACameraManager *manager, const char *cameraId, void *callback, void **device, int *primaryClient)
{
	if (device)
		*device = NULL;
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

/* no device can be opened, so there is no device or session these could act on */
camera_status_t ACameraDevice_createCaptureSession(void *device, const void *outputs, const void *callbacks, void **session)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraDevice_createCaptureSessionWithSessionParameters(void *device, const void *outputs, const void *sessionParameters,
                                                                        const void *callbacks, void **session)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_capture(void *session, void *callbacks, int numRequests, void **requests, int *captureSequenceId)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_captureV2(void *session, void *callbacks, int numRequests, void **requests, int *captureSequenceId)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_setRepeatingRequest(void *session, void *callbacks, int numRequests, void **requests, int *captureSequenceId)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_setRepeatingRequestV2(void *session, void *callbacks, int numRequests, void **requests, int *captureSequenceId)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_logicalCamera_capture(void *session, void *callbacks, int numRequests, void **requests, int *captureSequenceId)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_logicalCamera_captureV2(void *session, void *callbacks, int numRequests, void **requests, int *captureSequenceId)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_logicalCamera_setRepeatingRequest(void *session, void *callbacks, int numRequests, void **requests, int *captureSequenceId)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_logicalCamera_setRepeatingRequestV2(void *session, void *callbacks, int numRequests, void **requests, int *captureSequenceId)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

/* AOSP NdkCameraDevice.cpp / NdkCameraCaptureSession.cpp: the device and session calls; as above, none can exist */
camera_status_t ACameraDevice_close(void *device)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

const char *ACameraDevice_getId(const void *device)
{
	return NULL;
}

camera_status_t ACameraDevice_createCaptureRequest(const void *device, int templateId, void **request)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraDevice_createCaptureRequest_withPhysicalIds(const void *device, int templateId, const ACameraIdList *physicalIdList, void **request)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraDevice_isSessionConfigurationSupported(const void *device, const void *sessionOutputContainer)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

void ACameraCaptureSession_close(void *session)
{
}

camera_status_t ACameraCaptureSession_getDevice(void *session, void **device)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_stopRepeating(void *session)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_abortCaptures(void *session)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_updateSharedOutput(void *session, void *output)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_setWindowPreparedCallback(void *session, void *context, void *callback)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACameraCaptureSession_prepareWindow(void *session, void *window)
{
	return ACAMERA_ERROR_INVALID_PARAMETER;
}

/* a set of window pointers, as AOSP's std::set<ANativeWindow*> and the sets of outputs/targets ordered by window */
struct window_set {
	void **windows;
	uint32_t count;
};

static bool window_set_add(struct window_set *set, void *window)
{
	for (uint32_t i = 0; i < set->count; i++)
		if (set->windows[i] == window)
			return false;
	set->windows = realloc(set->windows, (set->count + 1) * sizeof(void *));
	set->windows[set->count++] = window;
	return true;
}

static bool window_set_remove(struct window_set *set, void *window)
{
	for (uint32_t i = 0; i < set->count; i++)
		if (set->windows[i] == window) {
			set->windows[i] = set->windows[--set->count];
			return true;
		}
	return false;
}

static void window_set_copy(struct window_set *dst, const struct window_set *src)
{
	dst->count = src->count;
	dst->windows = malloc(src->count * sizeof(void *));
	memcpy(dst->windows, src->windows, src->count * sizeof(void *));
}

/* AOSP impl/ACameraCaptureSession.h: an output and the container of outputs (a set by window). The physical
 * camera id of ACaptureSessionPhysicalOutput_create() is not kept: only a capture session would read it. */
typedef struct ACaptureSessionOutput {
	void *window;
	bool shared;
	struct window_set shared_windows;
} ACaptureSessionOutput;

typedef struct ACaptureSessionOutputContainer {
	struct window_set outputs;
} ACaptureSessionOutputContainer;

/* AOSP NdkCameraDevice.cpp */
camera_status_t ACaptureSessionOutputContainer_create(ACaptureSessionOutputContainer **out)
{
	if (!out)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	*out = calloc(1, sizeof(ACaptureSessionOutputContainer));
	return ACAMERA_OK;
}

void ACaptureSessionOutputContainer_free(ACaptureSessionOutputContainer *container)
{
	if (container)
		free(container->outputs.windows);
	free(container);
}

static camera_status_t session_output_create(void *window, bool shared, ACaptureSessionOutput **out)
{
	*out = calloc(1, sizeof(ACaptureSessionOutput));
	(*out)->window = window;
	(*out)->shared = shared;
	return ACAMERA_OK;
}

camera_status_t ACaptureSessionOutput_create(void *window, ACaptureSessionOutput **out)
{
	if (!window || !out)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return session_output_create(window, false, out);
}

camera_status_t ACaptureSessionSharedOutput_create(void *window, ACaptureSessionOutput **out)
{
	if (!window || !out)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return session_output_create(window, true, out);
}

camera_status_t ACaptureSessionPhysicalOutput_create(void *window, const char *physicalId, ACaptureSessionOutput **out)
{
	if (!window || !physicalId || !out)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return session_output_create(window, false, out);
}

camera_status_t ACaptureSessionSharedOutput_add(ACaptureSessionOutput *out, void *window)
{
	if (!window || !out)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	if (!out->shared)
		return ACAMERA_ERROR_INVALID_OPERATION;
	if (out->window == window)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return window_set_add(&out->shared_windows, window) ? ACAMERA_OK : ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t ACaptureSessionSharedOutput_remove(ACaptureSessionOutput *out, void *window)
{
	if (!window || !out)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	if (!out->shared)
		return ACAMERA_ERROR_INVALID_OPERATION;
	if (out->window == window)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return window_set_remove(&out->shared_windows, window) ? ACAMERA_OK : ACAMERA_ERROR_INVALID_PARAMETER;
}

void ACaptureSessionOutput_free(ACaptureSessionOutput *output)
{
	if (output)
		free(output->shared_windows.windows);
	free(output);
}

camera_status_t ACaptureSessionOutputContainer_add(ACaptureSessionOutputContainer *container, const ACaptureSessionOutput *output)
{
	if (!container || !output)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	window_set_add(&container->outputs, output->window);
	return ACAMERA_OK;
}

camera_status_t ACaptureSessionOutputContainer_remove(ACaptureSessionOutputContainer *container, const ACaptureSessionOutput *output)
{
	if (!container || !output)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	window_set_remove(&container->outputs, output->window);
	return ACAMERA_OK;
}

/* AOSP system/media camera_metadata.h: the entry types */
enum { TYPE_BYTE, TYPE_INT32, TYPE_FLOAT, TYPE_INT64, TYPE_DOUBLE, TYPE_RATIONAL };
static const uint8_t camera_type_size[] = { 1, 4, 4, 8, 8, 8 };

static const struct camera_tag_info {
	const char *section;
	const char *name;
	uint32_t tag;
	uint8_t type;
	bool request;
} camera_tags[] = {
#include "camera_metadata_tags.inc"
};

typedef struct ACameraMetadata_rational {
	int32_t numerator;
	int32_t denominator;
} ACameraMetadata_rational;

typedef struct ACameraMetadata_const_entry {
	uint32_t tag;
	uint8_t type;
	uint32_t count;
	union {
		const uint8_t *u8;
		const int32_t *i32;
		const float *f;
		const int64_t *i64;
		const double *d;
		const ACameraMetadata_rational *r;
	} data;
} ACameraMetadata_const_entry;

/* AOSP impl/ACameraMetadata.cpp over CameraMetadata: entries in insertion order. ATL creates metadata only as the
 * settings of a capture request (ACM_REQUEST); there are no characteristics or results. */
struct camera_entry {
	uint32_t tag;
	uint8_t type;
	uint32_t count;
	void *data;
};

typedef struct ACameraMetadata {
	struct camera_entry *entries;
	uint32_t count;
	uint32_t *tags; /* getTags(): built on demand, dropped on update */
} ACameraMetadata;

static struct camera_entry *metadata_find(const ACameraMetadata *md, uint32_t tag)
{
	for (uint32_t i = 0; i < md->count; i++)
		if (md->entries[i].tag == tag)
			return &md->entries[i];
	return NULL;
}

static ACameraMetadata *metadata_copy(const ACameraMetadata *src)
{
	ACameraMetadata *md = calloc(1, sizeof(ACameraMetadata));
	md->count = src->count;
	md->entries = malloc(src->count * sizeof(struct camera_entry));
	for (uint32_t i = 0; i < src->count; i++) {
		size_t size = src->entries[i].count * camera_type_size[src->entries[i].type];
		md->entries[i] = src->entries[i];
		md->entries[i].data = malloc(size);
		memcpy(md->entries[i].data, src->entries[i].data, size);
	}
	return md;
}

static void metadata_free(ACameraMetadata *md)
{
	if (!md)
		return;
	for (uint32_t i = 0; i < md->count; i++)
		free(md->entries[i].data);
	free(md->entries);
	free(md->tags);
	free(md);
}

static camera_status_t metadata_get(const ACameraMetadata *md, uint32_t tag, ACameraMetadata_const_entry *entry)
{
	const struct camera_entry *e = metadata_find(md, tag);
	if (!e || !e->count)
		return ACAMERA_ERROR_METADATA_NOT_FOUND;
	entry->tag = tag;
	entry->type = e->type;
	entry->count = e->count;
	entry->data.u8 = e->data;
	return ACAMERA_OK;
}

static camera_status_t metadata_get_tags(const ACameraMetadata *md, int32_t *numTags, const uint32_t **tags)
{
	ACameraMetadata *m = (ACameraMetadata *)md;
	if (!m->tags) {
		m->tags = malloc((m->count + 1) * sizeof(uint32_t));
		for (uint32_t i = 0; i < m->count; i++)
			m->tags[i] = m->entries[i].tag;
	}
	*numTags = m->count;
	*tags = m->tags;
	return ACAMERA_OK;
}

/* ACameraMetadata.h updateImpl(): capture request tags only, of the tag's own type (CameraMetadata::checkType());
 * no data and no count erases the entry. ATL has no vendor tags. */
static camera_status_t metadata_update(ACameraMetadata *md, uint32_t tag, uint8_t type, uint32_t count, const void *data)
{
	const struct camera_tag_info *info = NULL;
	for (size_t i = 0; i < sizeof(camera_tags) / sizeof(camera_tags[0]); i++)
		if (camera_tags[i].tag == tag)
			info = &camera_tags[i];
	if (!info || !info->request || info->type != type)
		return ACAMERA_ERROR_INVALID_PARAMETER;

	struct camera_entry *e = metadata_find(md, tag);
	if (count == 0 && !data) {
		if (e) {
			free(e->data);
			memmove(e, e + 1, (md->count - (e - md->entries) - 1) * sizeof(struct camera_entry));
			md->count--;
		}
	} else {
		void *copy = malloc(count * camera_type_size[type]);
		memcpy(copy, data, count * camera_type_size[type]);
		if (!e) {
			md->entries = realloc(md->entries, (md->count + 1) * sizeof(struct camera_entry));
			e = &md->entries[md->count++];
			e->tag = tag;
			e->type = type;
		} else {
			free(e->data);
		}
		e->count = count;
		e->data = copy;
	}
	free(md->tags);
	md->tags = NULL;
	return ACAMERA_OK;
}

/* AOSP NdkCameraMetadata.cpp */
camera_status_t ACameraMetadata_getConstEntry(const ACameraMetadata *acm, uint32_t tag, ACameraMetadata_const_entry *entry)
{
	if (!acm || !entry)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return metadata_get(acm, tag, entry);
}

camera_status_t ACameraMetadata_getAllTags(const ACameraMetadata *acm, int32_t *numTags, const uint32_t **tags)
{
	if (!acm || !numTags || !tags)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return metadata_get_tags(acm, numTags, tags);
}

/* CameraMetadata::getTagFromName(): the longest section name the name begins with, then the tag name after it */
camera_status_t ACameraMetadata_getTagFromName(const ACameraMetadata *acm, const char *name, uint32_t *tag)
{
	if (!acm || !name || !tag)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	const char *section = NULL;
	size_t length = 0;
	for (size_t i = 0; i < sizeof(camera_tags) / sizeof(camera_tags[0]); i++) {
		size_t l = strlen(camera_tags[i].section);
		if (l > length && !strncmp(name, camera_tags[i].section, l)) {
			section = camera_tags[i].section;
			length = l;
		}
	}
	if (!section || length + 1 >= strlen(name))
		return ACAMERA_ERROR_METADATA_NOT_FOUND;
	for (size_t i = 0; i < sizeof(camera_tags) / sizeof(camera_tags[0]); i++)
		if (!strcmp(camera_tags[i].section, section) && !strcmp(camera_tags[i].name, name + length + 1)) {
			*tag = camera_tags[i].tag;
			return ACAMERA_OK;
		}
	return ACAMERA_ERROR_METADATA_NOT_FOUND;
}

ACameraMetadata *ACameraMetadata_copy(const ACameraMetadata *src)
{
	return src ? metadata_copy(src) : NULL;
}

void ACameraMetadata_free(ACameraMetadata *metadata)
{
	metadata_free(metadata);
}

/* only static metadata (ACM_CHARACTERISTICS) can describe a logical multi-camera, and ATL has none */
bool ACameraMetadata_isLogicalMultiCamera(const ACameraMetadata *staticMetadata, size_t *numPhysicalCameras, const char *const **physicalCameraIds)
{
	return false;
}

/* AOSP wraps the native metadata of a CameraCharacteristics or CaptureResult and returns NULL for anything else;
 * ATL's CameraCharacteristics and CaptureResult hold their values in Java (no getNativeMetadataPtr()), so there is
 * no native metadata to wrap */
ACameraMetadata *ACameraMetadata_fromCameraMetadata(void *env, void *cameraMetadata)
{
	return NULL;
}

/* AOSP impl/ACaptureRequest.h */
typedef struct ACameraOutputTarget {
	void *window;
} ACameraOutputTarget;

struct physical_settings {
	char *id;
	ACameraMetadata *settings;
};

typedef struct ACaptureRequest {
	ACameraMetadata *settings;
	struct physical_settings *physical;
	uint32_t physical_count;
	struct window_set targets;
	void *context;
} ACaptureRequest;

static ACameraMetadata *request_physical(const ACaptureRequest *req, const char *physicalId)
{
	for (uint32_t i = 0; i < req->physical_count; i++)
		if (!strcmp(req->physical[i].id, physicalId))
			return req->physical[i].settings;
	return NULL;
}

/* AOSP NdkCaptureRequest.cpp */
camera_status_t ACameraOutputTarget_create(void *window, ACameraOutputTarget **out)
{
	if (!window)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	*out = malloc(sizeof(ACameraOutputTarget));
	(*out)->window = window;
	return ACAMERA_OK;
}

void ACameraOutputTarget_free(ACameraOutputTarget *target)
{
	free(target);
}

camera_status_t ACaptureRequest_addTarget(ACaptureRequest *req, const ACameraOutputTarget *target)
{
	if (!req || !target)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	window_set_add(&req->targets, target->window);
	return ACAMERA_OK;
}

camera_status_t ACaptureRequest_removeTarget(ACaptureRequest *req, const ACameraOutputTarget *target)
{
	if (!req || !target)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	window_set_remove(&req->targets, target->window);
	return ACAMERA_OK;
}

camera_status_t ACaptureRequest_getConstEntry(const ACaptureRequest *req, uint32_t tag, ACameraMetadata_const_entry *entry)
{
	if (!req || !entry)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return metadata_get(req->settings, tag, entry);
}

camera_status_t ACaptureRequest_getConstEntry_physicalCamera(const ACaptureRequest *req, const char *physicalId, uint32_t tag, ACameraMetadata_const_entry *entry)
{
	if (!req || !entry || !physicalId)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	ACameraMetadata *settings = request_physical(req, physicalId);
	if (!settings)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return metadata_get(settings, tag, entry);
}

camera_status_t ACaptureRequest_getAllTags(const ACaptureRequest *req, int32_t *numTags, const uint32_t **tags)
{
	if (!req || !numTags || !tags)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	return metadata_get_tags(req->settings, numTags, tags);
}

#define SET_ENTRY(NAME, NDK_TYPE, TYPE)                                                                                  \
	camera_status_t ACaptureRequest_setEntry_##NAME(ACaptureRequest *req, uint32_t tag, uint32_t count, const NDK_TYPE *data) \
	{                                                                                                                \
		if (!req || (count > 0 && !data))                                                                        \
			return ACAMERA_ERROR_INVALID_PARAMETER;                                                          \
		return metadata_update(req->settings, tag, TYPE, count, data);                                           \
	}                                                                                                                \
	camera_status_t ACaptureRequest_setEntry_physicalCamera_##NAME(ACaptureRequest *req, const char *physicalId,     \
	                                                               uint32_t tag, uint32_t count, const NDK_TYPE *data) \
	{                                                                                                                \
		if (!req || (count > 0 && !data) || !physicalId)                                                         \
			return ACAMERA_ERROR_INVALID_PARAMETER;                                                          \
		ACameraMetadata *settings = request_physical(req, physicalId);                                           \
		if (!settings)                                                                                           \
			return ACAMERA_ERROR_INVALID_PARAMETER;                                                          \
		return metadata_update(settings, tag, TYPE, count, data);                                                \
	}

SET_ENTRY(u8, uint8_t, TYPE_BYTE)
SET_ENTRY(i32, int32_t, TYPE_INT32)
SET_ENTRY(float, float, TYPE_FLOAT)
SET_ENTRY(double, double, TYPE_DOUBLE)
SET_ENTRY(i64, int64_t, TYPE_INT64)
SET_ENTRY(rational, ACameraMetadata_rational, TYPE_RATIONAL)
#undef SET_ENTRY

void ACaptureRequest_free(ACaptureRequest *request)
{
	if (!request)
		return;
	metadata_free(request->settings);
	for (uint32_t i = 0; i < request->physical_count; i++) {
		free(request->physical[i].id);
		metadata_free(request->physical[i].settings);
	}
	free(request->physical);
	free(request->targets.windows);
	free(request);
}

camera_status_t ACaptureRequest_setUserContext(ACaptureRequest *request, void *context)
{
	if (!request)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	request->context = context;
	return ACAMERA_OK;
}

camera_status_t ACaptureRequest_getUserContext(const ACaptureRequest *request, void **context)
{
	if (!request || !context)
		return ACAMERA_ERROR_INVALID_PARAMETER;
	*context = request->context;
	return ACAMERA_OK;
}

ACaptureRequest *ACaptureRequest_copy(const ACaptureRequest *src)
{
	if (!src)
		return NULL;
	ACaptureRequest *req = calloc(1, sizeof(ACaptureRequest));
	req->settings = metadata_copy(src->settings);
	req->physical_count = src->physical_count;
	req->physical = malloc(src->physical_count * sizeof(struct physical_settings));
	for (uint32_t i = 0; i < src->physical_count; i++) {
		req->physical[i].id = strdup(src->physical[i].id);
		req->physical[i].settings = metadata_copy(src->physical[i].settings);
	}
	window_set_copy(&req->targets, &src->targets);
	req->context = src->context;
	return req;
}
