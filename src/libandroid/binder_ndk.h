#ifndef BINDER_NDK_H
#define BINDER_NDK_H

/* Internals shared by binder_ndk.c, binder_parcel.c and binder_persistable_bundle.c (libbinder_ndk of
 * frameworks/native/libs/binder, android-16.0.0_r4). ATL has no binder driver: every AIBinder is a local
 * binder of this process, a parcel lives in memory only. */

#include <errno.h>
#include <stdatomic.h>
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <sys/types.h>
#include <uchar.h>

/* binder_status.h, same values as utils/Errors.h */
#define STATUS_OK 0
#define STATUS_UNKNOWN_ERROR INT32_MIN
#define STATUS_NO_MEMORY (-ENOMEM)
#define STATUS_INVALID_OPERATION (-ENOSYS)
#define STATUS_BAD_VALUE (-EINVAL)
#define STATUS_BAD_TYPE (STATUS_UNKNOWN_ERROR + 1)
#define STATUS_NAME_NOT_FOUND (-ENOENT)
#define STATUS_PERMISSION_DENIED (-EPERM)
#define STATUS_NO_INIT (-ENODEV)
#define STATUS_ALREADY_EXISTS (-EEXIST)
#define STATUS_DEAD_OBJECT (-EPIPE)
#define STATUS_FAILED_TRANSACTION (STATUS_UNKNOWN_ERROR + 2)
#define STATUS_BAD_INDEX (-EOVERFLOW)
#define STATUS_NOT_ENOUGH_DATA (-ENODATA)
#define STATUS_WOULD_BLOCK (-EWOULDBLOCK)
#define STATUS_TIMED_OUT (-ETIMEDOUT)
#define STATUS_UNKNOWN_TRANSACTION (-EBADMSG)
#define STATUS_FDS_NOT_ALLOWED (STATUS_UNKNOWN_ERROR + 7)
#define STATUS_UNEXPECTED_NULL (STATUS_UNKNOWN_ERROR + 8)

/* binder_status.h / binder/Status.h */
#define EX_NONE 0
#define EX_SERVICE_SPECIFIC (-8)
#define EX_PARCELABLE (-9)
#define EX_HAS_NOTED_APPOPS_REPLY_HEADER (-127)
#define EX_HAS_REPLY_HEADER (-128)
#define EX_TRANSACTION_FAILED (-129)

typedef int32_t binder_status_t;
typedef int32_t binder_exception_t;
typedef uint32_t transaction_code_t;
typedef uint32_t binder_flags_t;

typedef struct AIBinder AIBinder;
typedef struct AIBinder_Class AIBinder_Class;
typedef struct AParcel AParcel;
typedef struct AStatus AStatus;

/* RefBase weakref_impl: outlives the object while AIBinder_Weak references it */
struct binder_refs {
	atomic_int strong;
	atomic_int weak;
};

/* ABBinder */
struct AIBinder {
	struct binder_refs *refs;
	const AIBinder_Class *clazz;
	void *user_data;
	AIBinder *ext;     /* BBinder extension */
	int16_t stability; /* BBinder::mStability, Stability::Level */
	bool parceled;     /* BBinder::mParceled */
};

/* binder/Status */
struct AStatus {
	int32_t exception;
	int32_t error;
	char *message; /* NULL: empty */
};

/* android::Parcel (kernel binder flavour) and the AIBinder it belongs to (AParcel::mBinder) */
struct AParcel {
	const AIBinder *binder;
	uint8_t *data;
	size_t size, pos, cap; /* mDataSize, mDataPos, mDataCapacity */
	size_t *objs;          /* offsets of the flat_binder_objects */
	size_t nobjs;
	int32_t err;           /* mError */
};

binder_status_t binder_prune_status(int32_t status);

AParcel *binder_parcel_new(const AIBinder *binder);
int32_t binder_parcel_write_i32(AParcel *p, int32_t v);
int32_t binder_parcel_write_i64(AParcel *p, int64_t v);
int32_t binder_parcel_read_i32(AParcel *p, int32_t *v);
int32_t binder_parcel_read_i64(AParcel *p, int64_t *v);
void *binder_parcel_write_inplace(AParcel *p, size_t len);
const void *binder_parcel_read_inplace(AParcel *p, size_t len);
int32_t binder_parcel_write(AParcel *p, const void *data, size_t len);
int32_t binder_parcel_write_string16(AParcel *p, const char16_t *s, size_t len);
const char16_t *binder_parcel_read_string16_inplace(AParcel *p, size_t *len);
size_t binder_parcel_avail(const AParcel *p);

/* String16(const char *) and String8(const String16 &) of libutils (malloc, NUL terminated) */
char16_t *binder_utf16_from_utf8(const char *s, size_t *len);
char *binder_utf8_from_utf16(const char16_t *s, size_t len);

#endif
