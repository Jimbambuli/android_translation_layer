/*
 * The camera NDK (libcamera2ndk, camera/NdkCameraManager.h and friends) for a computer without cameras, as ATL's
 * CameraManager reports (getCameraIdList() is empty): the camera id list is empty, a camera id is unknown
 * (ACAMERA_ERROR_INVALID_PARAMETER, as AOSP answers for an id it does not know), and since no device or capture
 * session can be opened, requests on one are invalid. Availability callbacks are accepted and never called.
 */

#include <stdint.h>
#include <stdlib.h>

typedef int32_t camera_status_t;
#define ACAMERA_OK 0
#define ACAMERA_ERROR_INVALID_PARAMETER (-10000 - 1)

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
