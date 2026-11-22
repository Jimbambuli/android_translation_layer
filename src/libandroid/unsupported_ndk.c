/*
 * NDK functions of features ATL does not have, answering like an Android device without them:
 * no DRM scheme (libmediandk NdkMediaCrypto), no parceling of AHardwareBuffers (hardware_buffer_aidl.h).
 * Firefox's libxul and YouTube's libgoogle3 link them and check the results.
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
