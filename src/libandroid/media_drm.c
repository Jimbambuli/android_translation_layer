/*
 * NdkMediaCrypto and NdkMediaDrm (libmediandk) of a device without any DRM plugin. AOSP's functions
 * (frameworks/av/media/ndk/NdkMediaCrypto.cpp, NdkMediaDrm.cpp) go to the DRM HAL through DrmUtils::MakeDrm() and
 * MakeCrypto(); ATL has none, so those return NULL, which is the device AOSP code paths for "no scheme supported":
 * AMediaDrm_createByUUID() hands out an AMediaDrm without a plugin, whose functions all answer
 * AMEDIA_ERROR_INVALID_OBJECT. Firefox's libxul links all of them and checks the results.
 */

#include <stdlib.h>

#include "media_ndk.h"

typedef uint8_t AMediaUUID[16];
typedef struct AMediaCrypto AMediaCrypto;

/* AMediaDrm::mDrm, the plugin, is always NULL: every function with an object check answers INVALID_OBJECT */
typedef struct AMediaDrm {
	void *drm;
} AMediaDrm;

typedef struct {
	const uint8_t *ptr;
	size_t length;
} AMediaDrmByteArray;
typedef AMediaDrmByteArray AMediaDrmSessionId;
typedef AMediaDrmByteArray AMediaDrmScope;
typedef AMediaDrmByteArray AMediaDrmKeySetId;
typedef AMediaDrmByteArray AMediaDrmSecureStop;
typedef AMediaDrmByteArray AMediaDrmKeyId;

typedef enum AMediaDrmEventType {
	EVENT_PROVISION_REQUIRED = 1,
	EVENT_KEY_REQUIRED = 2,
	EVENT_KEY_EXPIRED = 3,
	EVENT_VENDOR_DEFINED = 4,
	EVENT_SESSION_RECLAIMED = 5,
} AMediaDrmEventType;

typedef enum AMediaDrmKeyType {
	KEY_TYPE_STREAMING = 1,
	KEY_TYPE_OFFLINE = 2,
	KEY_TYPE_RELEASE = 3
} AMediaDrmKeyType;

typedef enum AMediaDrmKeyRequestType {
	KEY_REQUEST_TYPE_INITIAL,
	KEY_REQUEST_TYPE_RENEWAL,
	KEY_REQUEST_TYPE_RELEASE,
	KEY_REQUEST_TYPE_NONE,
	KEY_REQUEST_TYPE_UPDATE
} AMediaDrmKeyRequestType;

typedef struct AMediaDrmKeyValuePair {
	const char *mKey;
	const char *mValue;
} AMediaDrmKeyValue;

typedef enum AMediaKeyStatusType {
	KEY_STATUS_TYPE_USABLE,
	KEY_STATUS_TYPE_EXPIRED,
	KEY_STATUS_TYPE_OUTPUTNOTALLOWED,
	KEY_STATUS_TYPE_STATUSPENDING,
	KEY_STATUS_TYPE_INTERNALERROR,
} AMediaDrmKeyStatusType;

typedef struct AMediaDrmKeyStatus {
	AMediaDrmKeyId keyId;
	AMediaDrmKeyStatusType keyType;
} AMediaDrmKeyStatus;

typedef void (*AMediaDrmEventListener)(AMediaDrm *, const AMediaDrmSessionId *sessionId, AMediaDrmEventType eventType, int extra,
                                       const uint8_t *data, size_t dataSize);
typedef void (*AMediaDrmExpirationUpdateListener)(AMediaDrm *, const AMediaDrmSessionId *sessionId, int64_t expiryTimeInMS);
typedef void (*AMediaDrmKeysChangeListener)(AMediaDrm *, const AMediaDrmSessionId *sessionId, const AMediaDrmKeyStatus *keyStatus,
                                            size_t numKeys, bool hasNewUsableKey);

/* NdkMediaCrypto: makeCrypto() is NULL */
bool AMediaCrypto_isCryptoSchemeSupported(const AMediaUUID uuid) { return false; }
bool AMediaCrypto_requiresSecureDecoderComponent(const char *mime) { return false; }
AMediaCrypto *AMediaCrypto_new(const AMediaUUID uuid, const void *initData, size_t initDataSize) { return NULL; }
void AMediaCrypto_delete(AMediaCrypto *crypto) {}

/* NdkMediaDrm: CreateDrm() is NULL */
bool AMediaDrm_isCryptoSchemeSupported(const uint8_t *uuid, const char *mimeType) { return false; }

AMediaDrm *AMediaDrm_createByUUID(const uint8_t *uuid)
{
	return calloc(1, sizeof(AMediaDrm));
}

void AMediaDrm_release(AMediaDrm *drm)
{
	free(drm);
}

media_status_t AMediaDrm_setOnEventListener(AMediaDrm *drm, AMediaDrmEventListener listener)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_setOnExpirationUpdateListener(AMediaDrm *drm, AMediaDrmExpirationUpdateListener listener)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_setOnKeysChangeListener(AMediaDrm *drm, AMediaDrmKeysChangeListener listener)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_openSession(AMediaDrm *drm, AMediaDrmSessionId *sessionId)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_closeSession(AMediaDrm *drm, const AMediaDrmSessionId *sessionId)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_getKeyRequest(AMediaDrm *drm, const AMediaDrmScope *scope, const uint8_t *init, size_t initSize,
                                       const char *mimeType, AMediaDrmKeyType keyType, const AMediaDrmKeyValue *optionalParameters,
                                       size_t numOptionalParameters, const uint8_t **keyRequest, size_t *keyRequestSize)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_getKeyRequestWithDefaultUrlAndType(AMediaDrm *drm, const AMediaDrmScope *scope, const uint8_t *init,
                                                            size_t initSize, const char *mimeType, AMediaDrmKeyType keyType,
                                                            const AMediaDrmKeyValue *optionalParameters, size_t numOptionalParameters,
                                                            const uint8_t **keyRequest, size_t *keyRequestSize, const char **defaultUrl,
                                                            AMediaDrmKeyRequestType *keyRequestType)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_provideKeyResponse(AMediaDrm *drm, const AMediaDrmScope *scope, const uint8_t *response, size_t responseSize,
                                            AMediaDrmKeySetId *keySetId)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_restoreKeys(AMediaDrm *drm, const AMediaDrmSessionId *sessionId, const AMediaDrmKeySetId *keySetId)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_removeKeys(AMediaDrm *drm, const AMediaDrmSessionId *keySetId)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_queryKeyStatus(AMediaDrm *drm, const AMediaDrmSessionId *sessionId, AMediaDrmKeyValue *keyValuePairs,
                                        size_t *numPairs)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_getProvisionRequest(AMediaDrm *drm, const uint8_t **provisionRequest, size_t *provisionRequestSize,
                                             const char **serverUrl)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_provideProvisionResponse(AMediaDrm *drm, const uint8_t *response, size_t responseSize)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_getSecureStops(AMediaDrm *drm, AMediaDrmSecureStop *secureStops, size_t *numSecureStops)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_releaseSecureStops(AMediaDrm *drm, const AMediaDrmSecureStop *ssRelease)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_getPropertyString(AMediaDrm *drm, const char *propertyName, const char **propertyValue)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_getPropertyByteArray(AMediaDrm *drm, const char *propertyName, AMediaDrmByteArray *propertyValue)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_setPropertyString(AMediaDrm *drm, const char *propertyName, const char *value)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_setPropertyByteArray(AMediaDrm *drm, const char *propertyName, const uint8_t *value, size_t valueSize)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

/* encrypt and decrypt check the session id before the object */
media_status_t AMediaDrm_encrypt(AMediaDrm *drm, const AMediaDrmSessionId *sessionId, const char *cipherAlgorithm, uint8_t *keyId,
                                 uint8_t *iv, const uint8_t *input, uint8_t *output, size_t dataSize)
{
	return sessionId ? AMEDIA_ERROR_INVALID_OBJECT : AMEDIA_ERROR_INVALID_PARAMETER;
}

media_status_t AMediaDrm_decrypt(AMediaDrm *drm, const AMediaDrmSessionId *sessionId, const char *cipherAlgorithm, uint8_t *keyId,
                                 uint8_t *iv, const uint8_t *input, uint8_t *output, size_t dataSize)
{
	return sessionId ? AMEDIA_ERROR_INVALID_OBJECT : AMEDIA_ERROR_INVALID_PARAMETER;
}

media_status_t AMediaDrm_sign(AMediaDrm *drm, const AMediaDrmSessionId *sessionId, const char *macAlgorithm, uint8_t *keyId,
                              uint8_t *message, size_t messageSize, uint8_t *signature, size_t *signatureSize)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}

media_status_t AMediaDrm_verify(AMediaDrm *drm, const AMediaDrmSessionId *sessionId, const char *macAlgorithm, uint8_t *keyId,
                                const uint8_t *message, size_t messageSize, const uint8_t *signature, size_t signatureSize)
{
	return AMEDIA_ERROR_INVALID_OBJECT;
}
