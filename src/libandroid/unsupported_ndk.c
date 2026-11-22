/*
 * NDK functions of features ATL does not have, answering like an Android device without them:
 * no DRM scheme (libmediandk NdkMediaCrypto), no SurfaceFlinger (ASurfaceControl), no parceling of
 * AHardwareBuffers (hardware_buffer_aidl.h). Firefox's libxul and YouTube's
 * libgoogle3 link them and check the results.
 */

#include <errno.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>

#include <jni.h>

/* android::status_t */
#define BAD_VALUE (-EINVAL)
#define INVALID_OPERATION (-ENOSYS)
/* binder_status_t */
#define STATUS_BAD_VALUE (-EINVAL)
#define STATUS_INVALID_OPERATION (-ENOSYS)

/* LOG_ALWAYS_FATAL in AOSP */
static void fatal(const char *msg)
{
	fprintf(stderr, "%s\n", msg);
	abort();
}
#define CHECK_NOT_NULL(name) \
	if (!(name))         \
		fatal("nullptr passed as " #name " argument");

/* AHardwareBuffer: hardware_buffer.c */
/* hardware_buffer_aidl.h; AHardwareBuffer.cpp: writing needs a buffer. Reading imports the parceled handle
 * into gralloc (GraphicBuffer::unflatten()), which does not exist here */
int AHardwareBuffer_readFromParcel(const void *parcel, void **outBuffer)
{
	if (!parcel || !outBuffer)
		return STATUS_BAD_VALUE;
	return STATUS_INVALID_OPERATION;
}
int AHardwareBuffer_writeToParcel(const void *buffer, void *parcel) { return STATUS_BAD_VALUE; }
/* hardware_buffer_jni.cpp: an android.hardware.HardwareBuffer in ATL never holds a native buffer */
void *AHardwareBuffer_fromHardwareBuffer(JNIEnv *env, jobject hardwareBufferObj) { return NULL; }
jobject AHardwareBuffer_toHardwareBuffer(JNIEnv *env, void *hardwareBuffer) { return NULL; }

/* surface_control.cpp: createFromWindow() returns NULL when there is no composer client
 * (SurfaceComposerClient::initCheck()). So no ASurfaceControl exists, and a transaction has nothing to
 * apply and never completes. */
void *ASurfaceControl_createFromWindow(void *window, const char *debug_name) { return NULL; }
/* create() gets a NULL parent (none exists) or not one of ours; createSurface() would fail */
void *ASurfaceControl_create(void *parent, const char *debug_name)
{
	CHECK_NOT_NULL(parent);
	CHECK_NOT_NULL(debug_name);
	return NULL;
}
void ASurfaceControl_acquire(void *aSurfaceControl) {}
void ASurfaceControl_release(void *aSurfaceControl) {}
/* surface_control.cpp: a Java SurfaceControl/Transaction without a native object is fatal; ATL's Java
 * classes (api-impl android/view/SurfaceControl.java) have none */
void *ASurfaceControl_fromJava(JNIEnv *env, jobject surfaceControlObj)
{
	CHECK_NOT_NULL(env);
	CHECK_NOT_NULL(surfaceControlObj);
	fatal("surfaceControlObj passed to ASurfaceControl_fromJava is not valid");
	return NULL;
}
void *ASurfaceTransaction_fromJava(JNIEnv *env, jobject transactionObj)
{
	CHECK_NOT_NULL(env);
	CHECK_NOT_NULL(transactionObj);
	fatal("surfaceControlObj passed to ASurfaceTransaction_fromJava is not valid");
	return NULL;
}
struct ASurfaceTransaction {
	char unused;
};
void *ASurfaceTransaction_create(void) { return calloc(1, sizeof(struct ASurfaceTransaction)); }
void ASurfaceTransaction_delete(void *aSurfaceTransaction) { free(aSurfaceTransaction); }
void ASurfaceTransaction_apply(void *aSurfaceTransaction) {}
void ASurfaceTransaction_setOnComplete(void *aSurfaceTransaction, void *context, void (*func)(void *context, void *stats)) {}
void ASurfaceTransaction_reparent(void *aSurfaceTransaction, void *aSurfaceControl, void *newParentASurfaceControl) {}
void ASurfaceTransaction_setBuffer(void *aSurfaceTransaction, void *aSurfaceControl, void *buffer, int acquire_fence_fd) {}
void ASurfaceTransaction_setBufferDataSpace(void *aSurfaceTransaction, void *aSurfaceControl, int32_t aDataSpace) {}
void ASurfaceTransaction_setGeometry(void *aSurfaceTransaction, void *aSurfaceControl, const void *source, const void *destination, int32_t transform) {}
int ASurfaceTransactionStats_getPreviousReleaseFenceFd(void *aSurfaceTransactionStats, void *aSurfaceControl) { return -1; }

/* setters on a surface: AOSP needs both; since no ASurfaceControl exists, they have nothing to set */
#define CHECK_SURFACE()                     \
	CHECK_NOT_NULL(aSurfaceTransaction); \
	CHECK_NOT_NULL(aSurfaceControl);
void ASurfaceTransaction_setVisibility(void *aSurfaceTransaction, void *aSurfaceControl, int8_t visibility) { CHECK_SURFACE(); }
void ASurfaceTransaction_setZOrder(void *aSurfaceTransaction, void *aSurfaceControl, int32_t z_order) { CHECK_SURFACE(); }
void ASurfaceTransaction_setBufferWithRelease(void *aSurfaceTransaction, void *aSurfaceControl, void *buffer, int acquire_fence_fd,
                                              void *context, void (*aReleaseCallback)(void *context, int release_fence_fd))
{
	CHECK_SURFACE();
	CHECK_NOT_NULL(aReleaseCallback);
}
void ASurfaceTransaction_setColor(void *aSurfaceTransaction, void *aSurfaceControl, float r, float g, float b, float alpha, int32_t dataspace) { CHECK_SURFACE(); }
void ASurfaceTransaction_setCrop(void *aSurfaceTransaction, void *aSurfaceControl, const void *crop) { CHECK_SURFACE(); }
void ASurfaceTransaction_setPosition(void *aSurfaceTransaction, void *aSurfaceControl, int32_t x, int32_t y) { CHECK_SURFACE(); }
void ASurfaceTransaction_setBufferTransform(void *aSurfaceTransaction, void *aSurfaceControl, int32_t transform) { CHECK_SURFACE(); }
void ASurfaceTransaction_setScale(void *aSurfaceTransaction, void *aSurfaceControl, float xScale, float yScale) { CHECK_SURFACE(); }
void ASurfaceTransaction_setBufferTransparency(void *aSurfaceTransaction, void *aSurfaceControl, int8_t transparency) { CHECK_SURFACE(); }
void ASurfaceTransaction_setDamageRegion(void *aSurfaceTransaction, void *aSurfaceControl, const void *rects, uint32_t count) { CHECK_SURFACE(); }
void ASurfaceTransaction_setBufferAlpha(void *aSurfaceTransaction, void *aSurfaceControl, float alpha) { CHECK_SURFACE(); }
void ASurfaceTransaction_setHdrMetadata_smpte2086(void *aSurfaceTransaction, void *aSurfaceControl, void *metadata) { CHECK_SURFACE(); }
void ASurfaceTransaction_setHdrMetadata_cta861_3(void *aSurfaceTransaction, void *aSurfaceControl, void *metadata) { CHECK_SURFACE(); }
void ASurfaceTransaction_setExtendedRangeBrightness(void *aSurfaceTransaction, void *aSurfaceControl, float currentBufferRatio, float desiredRatio) { CHECK_SURFACE(); }
void ASurfaceTransaction_setDesiredHdrHeadroom(void *aSurfaceTransaction, void *aSurfaceControl, float desiredRatio) { CHECK_SURFACE(); }
void ASurfaceTransaction_setLuts(void *aSurfaceTransaction, void *aSurfaceControl, const void *luts) { CHECK_SURFACE(); }
void ASurfaceTransaction_setFrameRate(void *aSurfaceTransaction, void *aSurfaceControl, float frameRate, int8_t compatibility) { CHECK_SURFACE(); }
void ASurfaceTransaction_setFrameRateWithChangeStrategy(void *aSurfaceTransaction, void *aSurfaceControl, float frameRate,
                                                        int8_t compatibility, int8_t changeFrameRateStrategy) { CHECK_SURFACE(); }
void ASurfaceTransaction_clearFrameRate(void *aSurfaceTransaction, void *aSurfaceControl) { CHECK_SURFACE(); }
void ASurfaceTransaction_setEnableBackPressure(void *aSurfaceTransaction, void *aSurfaceControl, bool enableBackPressure) { CHECK_SURFACE(); }
void ASurfaceTransaction_setDesiredPresentTime(void *aSurfaceTransaction, int64_t desiredPresentTime) { CHECK_NOT_NULL(aSurfaceTransaction); }
void ASurfaceTransaction_setFrameTimeline(void *aSurfaceTransaction, int64_t vsyncId) { CHECK_NOT_NULL(aSurfaceTransaction); }
/* never committed, like setOnComplete() never completes */
void ASurfaceTransaction_setOnCommit(void *aSurfaceTransaction, void *context, void (*func)(void *context, void *stats))
{
	CHECK_NOT_NULL(aSurfaceTransaction);
	CHECK_NOT_NULL(func);
}

/* ASurfaceTransactionStats are only handed to the callbacks above, which never run */
int64_t ASurfaceTransactionStats_getLatchTime(void *aSurfaceTransactionStats)
{
	CHECK_NOT_NULL(aSurfaceTransactionStats);
	return -1;
}
int ASurfaceTransactionStats_getPresentFenceFd(void *aSurfaceTransactionStats)
{
	CHECK_NOT_NULL(aSurfaceTransactionStats);
	return -1;
}
/* the stats of no surface: an empty array, freed by releaseASurfaceControls() */
void ASurfaceTransactionStats_getASurfaceControls(void *aSurfaceTransactionStats, void ***outASurfaceControls, size_t *outASurfaceControlsSize)
{
	CHECK_NOT_NULL(aSurfaceTransactionStats);
	CHECK_NOT_NULL(outASurfaceControls);
	CHECK_NOT_NULL(outASurfaceControlsSize);
	*outASurfaceControls = calloc(1, sizeof(void *));
	*outASurfaceControlsSize = 0;
}
void ASurfaceTransactionStats_releaseASurfaceControls(void **aSurfaceControls)
{
	CHECK_NOT_NULL(aSurfaceControls);
	free(aSurfaceControls);
}
int64_t ASurfaceTransactionStats_getAcquireTime(void *aSurfaceTransactionStats, void *aSurfaceControl)
{
	CHECK_NOT_NULL(aSurfaceTransactionStats);
	CHECK_NOT_NULL(aSurfaceControl);
	fatal("ASurfaceControl not found");
	return -1;
}

/* display_luts.cpp: ADisplayLuts(Entry) are plain data, refcounted like the AOSP RefBase objects */
#define ADISPLAYLUTS_ONE_DIMENSION 1
#define ADISPLAYLUTS_THREE_DIMENSION 3
#define ADISPLAYLUTS_BUFFER_LENGTH_LIMIT 100000
struct ADisplayLutsEntry {
	int32_t refs;
	int32_t dimension;
	int32_t size;
	int32_t samplingKey;
	float buffer[];
};
struct ADisplayLuts {
	int32_t count;
	struct ADisplayLutsEntry *entries[2];
};

struct ADisplayLutsEntry *ADisplayLutsEntry_createEntry(float *buffer, int32_t length, int32_t dimension, int32_t key)
{
	CHECK_NOT_NULL(buffer);
	if (length >= ADISPLAYLUTS_BUFFER_LENGTH_LIMIT)
		fatal("the lut raw buffer length is too big to handle");
	if (dimension != ADISPLAYLUTS_ONE_DIMENSION && dimension != ADISPLAYLUTS_THREE_DIMENSION)
		fatal("the lut dimension is be either 1 or 3");
	int32_t size = length;
	if (dimension == ADISPLAYLUTS_THREE_DIMENSION) {
		if (length % 3 != 0)
			fatal("the 3d lut raw buffer is not divisible by 3");
		int32_t lengthPerChannel = length / 3;
		/* AOSP: cbrt() of it must be an integer */
		for (size = 0; size * size * size < lengthPerChannel; size++)
			;
		if (size * size * size != lengthPerChannel)
			fatal("the 3d lut buffer length is incorrect");
	}
	if (size < 2)
		fatal("the lut size for each dimension is too small");

	struct ADisplayLutsEntry *entry = malloc(sizeof(*entry) + length * sizeof(float));
	memcpy(entry->buffer, buffer, length * sizeof(float));
	entry->refs = 1;
	entry->dimension = dimension;
	entry->size = size;
	entry->samplingKey = key;
	return entry;
}
void ADisplayLutsEntry_destroy(struct ADisplayLutsEntry *entry)
{
	if (entry && __atomic_sub_fetch(&entry->refs, 1, __ATOMIC_ACQ_REL) == 0)
		free(entry);
}
int32_t ADisplayLutsEntry_getDimension(const struct ADisplayLutsEntry *entry)
{
	CHECK_NOT_NULL(entry);
	return entry->dimension;
}
int32_t ADisplayLutsEntry_getSize(const struct ADisplayLutsEntry *entry)
{
	CHECK_NOT_NULL(entry);
	return entry->size;
}
int32_t ADisplayLutsEntry_getSamplingKey(const struct ADisplayLutsEntry *entry)
{
	CHECK_NOT_NULL(entry);
	return entry->samplingKey;
}
const float *ADisplayLutsEntry_getBuffer(const struct ADisplayLutsEntry *entry)
{
	CHECK_NOT_NULL(entry);
	return entry->buffer;
}
struct ADisplayLuts *ADisplayLuts_create(void) { return calloc(1, sizeof(struct ADisplayLuts)); }
static void luts_clear(struct ADisplayLuts *luts)
{
	for (int32_t i = 0; i < luts->count; i++)
		ADisplayLutsEntry_destroy(luts->entries[i]);
	luts->count = 0;
}
void ADisplayLuts_destroy(struct ADisplayLuts *luts)
{
	if (luts) {
		luts_clear(luts);
		free(luts);
	}
}
void ADisplayLuts_setEntries(struct ADisplayLuts *luts, struct ADisplayLutsEntry **entries, int32_t numEntries)
{
	CHECK_NOT_NULL(luts);
	luts_clear(luts);
	if (!entries || numEntries == 0)
		return;
	if (numEntries > 2)
		fatal("The number of entries should be not over 2!");
	if (numEntries == 2 && entries[0]->dimension != ADISPLAYLUTS_ONE_DIMENSION &&
	    entries[1]->dimension != ADISPLAYLUTS_THREE_DIMENSION)
		fatal("The entries should be 1D and 3D in order!");
	for (int32_t i = 0; i < numEntries; i++) {
		__atomic_add_fetch(&entries[i]->refs, 1, __ATOMIC_RELAXED);
		luts->entries[luts->count++] = entries[i];
	}
}
