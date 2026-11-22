/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* AParcel: frameworks/native/libs/binder/ndk/parcel.cpp on the kernel binder flavour of libs/binder/Parcel.cpp
 * (android-16.0.0_r4) with its layout, object list and error codes. Without a binder driver the binders in a
 * parcel are always local ones (BINDER_TYPE_BINDER, cookie = the AIBinder); strings and status as Parcel.cpp,
 * Status.cpp and libutils Unicode.cpp. */

#include <fcntl.h>
#include <limits.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <linux/android/binder.h>

#include "binder_ndk.h"

typedef bool (*AParcel_stringAllocator)(void *stringData, int32_t length, char **buffer);
typedef bool (*AParcel_stringArrayAllocator)(void *arrayData, int32_t length);
typedef bool (*AParcel_stringArrayElementAllocator)(void *arrayData, size_t index, int32_t length, char **buffer);
typedef const char *(*AParcel_stringArrayElementGetter)(const void *arrayData, size_t index, int32_t *outLength);
typedef bool (*AParcel_parcelableArrayAllocator)(void *arrayData, int32_t length);
typedef binder_status_t (*AParcel_writeParcelableElement)(AParcel *parcel, const void *arrayData, size_t index);
typedef binder_status_t (*AParcel_readParcelableElement)(const AParcel *parcel, void *arrayData, size_t index);
typedef bool (*AParcel_int32ArrayAllocator)(void *arrayData, int32_t length, int32_t **outBuffer);
typedef bool (*AParcel_uint32ArrayAllocator)(void *arrayData, int32_t length, uint32_t **outBuffer);
typedef bool (*AParcel_int64ArrayAllocator)(void *arrayData, int32_t length, int64_t **outBuffer);
typedef bool (*AParcel_uint64ArrayAllocator)(void *arrayData, int32_t length, uint64_t **outBuffer);
typedef bool (*AParcel_floatArrayAllocator)(void *arrayData, int32_t length, float **outBuffer);
typedef bool (*AParcel_doubleArrayAllocator)(void *arrayData, int32_t length, double **outBuffer);
typedef bool (*AParcel_boolArrayAllocator)(void *arrayData, int32_t length);
typedef bool (*AParcel_boolArrayGetter)(const void *arrayData, size_t index);
typedef void (*AParcel_boolArraySetter)(void *arrayData, size_t index, bool value);
typedef bool (*AParcel_charArrayAllocator)(void *arrayData, int32_t length, char16_t **outBuffer);
typedef bool (*AParcel_byteArrayAllocator)(void *arrayData, int32_t length, int8_t **outBuffer);

void AIBinder_incStrong(AIBinder *binder);
void AIBinder_decStrong(AIBinder *binder);

#define PAD4(s) (((s) + 3) & ~(size_t)3)
#define OBJ_SIZE sizeof(struct flat_binder_object)
#define SCHED_BITS 19        /* schedPolicyMask(SCHED_NORMAL, 19): background scheduling is not disabled */
#define STABILITY_SYSTEM 12  /* Stability::getLocalLevel() of libbinder */
#define P(parcel) ((AParcel *)(parcel))

/* --- libutils Unicode.cpp --- */

static ssize_t utf8_to_utf16_length(const uint8_t *in, size_t len)
{
	const uint8_t *end = in + len;
	size_t n = 0;
	while (in < end) {
		uint8_t c = *in;
		n++;
		if (c < 0xc0) { /* ASCII or an invalid leading byte */
			in++;
		} else if (c < 0xe0) {
			in += 2;
		} else if (c < 0xf0) {
			in += 3;
		} else {
			if (end - in >= 4 && (((c & 0x07) << 18) | ((in[1] & 0x3f) << 12) | ((in[2] & 0x3f) << 6) | (in[3] & 0x3f)) >= 0x10000)
				n++;
			in += 4;
		}
	}
	return in == end ? (ssize_t)n : -1;
}

/* utf8_to_utf16(): u16len counts the terminating NUL */
static void utf8_to_utf16(const uint8_t *in, size_t len, char16_t *out, size_t u16len)
{
	const uint8_t *in_end = in + len;
	char16_t *out_end = out + u16len - 1;
	while (in < in_end && out < out_end) {
		uint8_t c = *in++;
		if (c < 0xc0) {
			*out++ = c;
		} else if (c < 0xe0) {
			if (in + 1 > in_end)
				break;
			*out++ = ((c & 0x1f) << 6) | (in[0] & 0x3f);
			in += 1;
		} else if (c < 0xf0) {
			if (in + 2 > in_end)
				break;
			*out++ = ((c & 0x0f) << 12) | ((in[0] & 0x3f) << 6) | (in[1] & 0x3f);
			in += 2;
		} else {
			if (in + 3 > in_end)
				break;
			uint32_t w = ((c & 0x07) << 18) | ((in[0] & 0x3f) << 12) | ((in[1] & 0x3f) << 6) | (in[2] & 0x3f);
			in += 3;
			if (w < 0x10000) {
				*out++ = w;
			} else {
				if (out + 2 > out_end)
					break;
				*out++ = ((w - 0x10000) >> 10) + 0xd800;
				*out++ = ((w - 0x10000) & 0x3ff) + 0xdc00;
			}
		}
	}
	*out = 0;
}

static bool is_surrogate_pair(char16_t w1, char16_t w2)
{
	return (w1 & 0xfc00) == 0xd800 && (w2 & 0xfc00) == 0xdc00;
}

static ssize_t utf16_to_utf8_length(const char16_t *in, size_t len)
{
	if (!in || len == 0)
		return -1;
	const char16_t *end = in + len;
	size_t n = 0;
	while (in < end) {
		char16_t w = *in++;
		if (w < 0x80)
			n += 1;
		else if (w < 0x800)
			n += 2;
		else if ((w & 0xf800) != 0xd800)
			n += 3;
		else if (in < end && is_surrogate_pair(w, *in)) {
			n += 4;
			in++;
		} /* lone surrogates are skipped */
	}
	return n;
}

static void utf16_to_utf8(const char16_t *in, size_t len, char *out, size_t out_len)
{
	const char16_t *end = in + len;
	char *out_end = out + out_len;
	while (in < end) {
		char16_t w = *in++;
		if (w < 0x80) {
			if (out + 1 > out_end)
				return;
			*out++ = w;
		} else if (w < 0x800) {
			if (out + 2 > out_end)
				return;
			*out++ = 0xc0 | ((w >> 6) & 0x1f);
			*out++ = 0x80 | (w & 0x3f);
		} else if ((w & 0xf800) != 0xd800) {
			if (out + 3 > out_end)
				return;
			*out++ = 0xe0 | ((w >> 12) & 0xf);
			*out++ = 0x80 | ((w >> 6) & 0x3f);
			*out++ = 0x80 | (w & 0x3f);
		} else if (in < end && is_surrogate_pair(w, *in)) {
			if (out + 4 > out_end)
				return;
			uint32_t dw = 0x10000 + ((w - 0xd800) << 10) + (*in++ - 0xdc00);
			*out++ = 0xf0 | ((dw >> 18) & 0x07);
			*out++ = 0x80 | ((dw >> 12) & 0x3f);
			*out++ = 0x80 | ((dw >> 6) & 0x3f);
			*out++ = 0x80 | (dw & 0x3f);
		}
	}
	*out = 0;
}

/* String16::allocFromUTF8(): invalid UTF-8 gives the empty string */
char16_t *binder_utf16_from_utf8(const char *s, size_t *len)
{
	size_t n8 = strlen(s);
	ssize_t n16 = n8 ? utf8_to_utf16_length((const uint8_t *)s, n8) : 0;
	if (n16 < 0)
		n16 = 0;
	char16_t *r = malloc((n16 + 1) * sizeof(char16_t));
	if (r)
		utf8_to_utf16((const uint8_t *)s, n16 ? n8 : 0, r, n16 + 1);
	*len = n16;
	return r;
}

/* String8::allocFromUTF16() */
char *binder_utf8_from_utf16(const char16_t *s, size_t len)
{
	ssize_t n8 = len ? utf16_to_utf8_length(s, len) : 0;
	char *r = malloc(n8 + 1);
	if (!r)
		return NULL;
	r[0] = 0;
	if (n8 > 0)
		utf16_to_utf8(s, len, r, n8 + 1);
	return r;
}

/* --- android::Parcel --- */

AParcel *binder_parcel_new(const AIBinder *binder)
{
	AParcel *p = calloc(1, sizeof(AParcel));
	if (p)
		p->binder = binder;
	return p;
}

static size_t data_size(const AParcel *p)
{
	return p->size > p->pos ? p->size : p->pos;
}

size_t binder_parcel_avail(const AParcel *p)
{
	return data_size(p) - p->pos;
}

/* validateReadData(): no plain data access over an object */
static int32_t validate(const AParcel *p, size_t upper)
{
	for (size_t i = 0; i < p->nobjs; i++)
		if (p->objs[i] < upper && p->pos < p->objs[i] + OBJ_SIZE)
			return STATUS_PERMISSION_DENIED;
	return STATUS_OK;
}

static void acquire_object(const struct flat_binder_object *obj)
{
	if (obj->hdr.type == BINDER_TYPE_BINDER && obj->binder)
		AIBinder_incStrong((AIBinder *)(uintptr_t)obj->cookie);
}

static void release_object(const struct flat_binder_object *obj)
{
	if (obj->hdr.type == BINDER_TYPE_BINDER && obj->binder)
		AIBinder_decStrong((AIBinder *)(uintptr_t)obj->cookie);
	else if (obj->hdr.type == BINDER_TYPE_FD && obj->cookie) /* owned */
		close(obj->handle);
}

static void free_data(AParcel *p)
{
	while (p->nobjs > 0)
		release_object((struct flat_binder_object *)(p->data + p->objs[--p->nobjs]));
	free(p->data);
	free(p->objs);
	*p = (AParcel){.binder = p->binder};
}

static int32_t continue_write(AParcel *p, size_t desired)
{
	if (desired > INT32_MAX)
		return STATUS_BAD_VALUE;
	if (desired < p->size) { /* release the objects past the new end */
		size_t keep = 0;
		for (size_t i = 0; i < p->nobjs; i++) {
			if (desired && p->objs[i] + OBJ_SIZE <= desired)
				p->objs[keep++] = p->objs[i];
			else
				release_object((struct flat_binder_object *)(p->data + p->objs[i]));
		}
		p->nobjs = keep;
	}
	if (p->data) {
		if (desired > p->cap) {
			uint8_t *data = realloc(p->data, desired);
			if (!data)
				return p->err = STATUS_NO_MEMORY;
			p->data = data;
			p->cap = desired;
		} else {
			if (p->size > desired)
				p->size = desired;
			if (p->pos > desired)
				p->pos = desired;
		}
	} else {
		if (!(p->data = malloc(desired)))
			return p->err = STATUS_NO_MEMORY;
		p->size = p->pos = 0;
		p->cap = desired;
	}
	return STATUS_OK;
}

static int32_t grow_data(AParcel *p, size_t len)
{
	if (len > INT32_MAX || p->pos > p->size)
		return STATUS_BAD_VALUE;
	size_t n = ((p->size + len) * 3) / 2;
	return continue_write(p, n > 128 ? n : 128);
}

static void finish_write(AParcel *p, size_t len)
{
	p->pos += len;
	if (p->pos > p->size)
		p->size = p->pos;
}

void *binder_parcel_write_inplace(AParcel *p, size_t len)
{
	if (len > INT32_MAX)
		return NULL;
	size_t padded = PAD4(len);
	if (p->pos + padded > p->cap && grow_data(p, padded) != STATUS_OK)
		return NULL;
	if (validate(p, p->pos + padded) != STATUS_OK)
		return NULL;
	uint8_t *d = p->data + p->pos;
	memset(d + len, 0, padded - len);
	finish_write(p, padded);
	return d;
}

int32_t binder_parcel_write(AParcel *p, const void *data, size_t len)
{
	if (len > INT32_MAX)
		return STATUS_BAD_VALUE;
	void *d = binder_parcel_write_inplace(p, len);
	if (!d)
		return p->err;
	memcpy(d, data, len);
	return STATUS_OK;
}

static int32_t write_aligned(AParcel *p, const void *v, size_t n)
{
	int32_t err;
	if (p->pos + n > p->cap && (err = grow_data(p, n)) != STATUS_OK)
		return err;
	if ((err = validate(p, p->pos + n)) != STATUS_OK)
		return err;
	memcpy(p->data + p->pos, v, n);
	finish_write(p, n);
	return STATUS_OK;
}

static int32_t read_aligned(AParcel *p, void *v, size_t n)
{
	if (p->pos + n > p->size)
		return STATUS_NOT_ENOUGH_DATA;
	int32_t err = validate(p, p->pos + n);
	if (err == STATUS_OK)
		memcpy(v, p->data + p->pos, n);
	p->pos += n;
	return err;
}

const void *binder_parcel_read_inplace(AParcel *p, size_t len)
{
	if (len > INT32_MAX)
		return NULL;
	size_t padded = PAD4(len);
	if (p->pos + padded > p->size)
		return NULL;
	const void *d = validate(p, p->pos + padded) == STATUS_OK ? p->data + p->pos : NULL;
	p->pos += padded;
	return d;
}

int32_t binder_parcel_write_i32(AParcel *p, int32_t v) { return write_aligned(p, &v, sizeof(v)); }
int32_t binder_parcel_write_i64(AParcel *p, int64_t v) { return write_aligned(p, &v, sizeof(v)); }
int32_t binder_parcel_read_i32(AParcel *p, int32_t *v) { return read_aligned(p, v, sizeof(*v)); }
int32_t binder_parcel_read_i64(AParcel *p, int64_t *v) { return read_aligned(p, v, sizeof(*v)); }

int32_t binder_parcel_write_string16(AParcel *p, const char16_t *s, size_t len)
{
	if (!s)
		return binder_parcel_write_i32(p, -1);
	int32_t err = binder_parcel_write_i32(p, len);
	if (err != STATUS_OK)
		return err;
	char16_t *d = binder_parcel_write_inplace(p, (len + 1) * sizeof(char16_t));
	if (!d)
		return p->err;
	memcpy(d, s, len * sizeof(char16_t));
	d[len] = 0;
	return STATUS_OK;
}

const char16_t *binder_parcel_read_string16_inplace(AParcel *p, size_t *len)
{
	int32_t size;
	if (binder_parcel_read_i32(p, &size) != STATUS_OK)
		size = 0; /* readInt32() */
	if (size >= 0 && size < INT32_MAX) {
		*len = size;
		const char16_t *s = binder_parcel_read_inplace(p, (size_t)(size + 1) * sizeof(char16_t));
		if (s && s[size] == 0)
			return s;
	}
	*len = 0;
	return NULL;
}

/* writeObject() */
static int32_t write_object(AParcel *p, const struct flat_binder_object *obj, bool null_meta)
{
	int32_t err;
	if (p->pos + OBJ_SIZE > p->cap && (err = grow_data(p, OBJ_SIZE)) != STATUS_OK)
		return err;
	bool record = null_meta || obj->binder;
	if (record) {
		size_t *objs = realloc(p->objs, (p->nobjs + 1) * sizeof(size_t));
		if (!objs)
			return STATUS_NO_MEMORY;
		p->objs = objs;
	}
	if ((err = validate(p, p->pos + OBJ_SIZE)) != STATUS_OK)
		return err;
	memcpy(p->data + p->pos, obj, OBJ_SIZE);
	if (record) {
		p->objs[p->nobjs++] = p->pos;
		acquire_object(obj);
	}
	finish_write(p, OBJ_SIZE);
	return STATUS_OK;
}

/* readObject(): an object other than a null binder must be in the object list */
static const struct flat_binder_object *read_object(AParcel *p, bool null_meta)
{
	size_t pos = p->pos;
	if (pos + OBJ_SIZE > p->size)
		return NULL;
	const struct flat_binder_object *obj = (const void *)(p->data + pos);
	p->pos = pos + OBJ_SIZE;
	if (!null_meta && obj->cookie == 0 && obj->binder == 0)
		return obj;
	for (size_t i = 0; i < p->nobjs; i++)
		if (p->objs[i] == pos)
			return obj;
	return NULL;
}

/* --- parcel.cpp --- */

void AParcel_delete(AParcel *parcel)
{
	if (!parcel)
		return;
	free_data(parcel);
	free(parcel);
}

AParcel *AParcel_create(void)
{
	return binder_parcel_new(NULL);
}

binder_status_t AParcel_reset(AParcel *parcel)
{
	free_data(parcel);
	return STATUS_OK;
}

binder_status_t AParcel_setDataPosition(const AParcel *parcel, int32_t position)
{
	if (position < 0)
		return STATUS_BAD_VALUE;
	P(parcel)->pos = position;
	return STATUS_OK;
}

int32_t AParcel_getDataPosition(const AParcel *parcel)
{
	return parcel->pos;
}

int32_t AParcel_getDataSize(const AParcel *parcel)
{
	return data_size(parcel);
}

/* Parcel::appendFrom() */
binder_status_t AParcel_appendFrom(const AParcel *from, AParcel *to, int32_t start, int32_t size)
{
	size_t offset = (size_t)start, len = (size_t)size, start_pos = to->pos, n = from->nobjs;
	int32_t err;
	if (len == 0)
		return STATUS_OK;
	if (len > INT32_MAX || offset > from->size || len > from->size || offset + len > from->size)
		return STATUS_BAD_VALUE;
	if ((err = validate(to, to->pos + len)) != STATUS_OK)
		return binder_prune_status(err);
	if (to->pos + len > to->cap && (err = grow_data(to, len)) != STATUS_OK)
		return binder_prune_status(err);
	memmove(to->data + to->pos, from->data + offset, len);
	finish_write(to, len);
	for (size_t i = 0; i < n; i++) {
		size_t pos = from->objs[i];
		if (pos < offset || pos + OBJ_SIZE > offset + len)
			continue;
		size_t *objs = realloc(to->objs, (to->nobjs + 1) * sizeof(size_t));
		if (!objs)
			return STATUS_NO_MEMORY;
		to->objs = objs;
		size_t off = pos - offset + start_pos;
		to->objs[to->nobjs++] = off;
		struct flat_binder_object *flat = (void *)(to->data + off);
		if (flat->hdr.type == BINDER_TYPE_FD) { /* the new parcel owns its own fd */
			flat->handle = fcntl(flat->handle, F_DUPFD_CLOEXEC, 0);
			flat->cookie = 1;
		}
		acquire_object(flat);
	}
	return STATUS_OK;
}

binder_status_t AParcel_marshal(const AParcel *parcel, uint8_t *buffer, size_t start, size_t len)
{
	if (parcel->nobjs)
		return STATUS_INVALID_OPERATION;
	if (len > parcel->size || start > parcel->size - len)
		return STATUS_BAD_VALUE;
	if (!parcel->data)
		return STATUS_UNEXPECTED_NULL;
	memcpy(buffer, parcel->data + start, len);
	return STATUS_OK;
}

binder_status_t AParcel_unmarshal(AParcel *parcel, const uint8_t *buffer, size_t len)
{
	if (len > INT32_MAX) /* setDataSize() */
		return STATUS_BAD_VALUE;
	int32_t err = continue_write(parcel, len);
	if (err != STATUS_OK)
		return binder_prune_status(err);
	parcel->size = len;
	parcel->pos = 0;
	void *raw = binder_parcel_write_inplace(parcel, len);
	if (!raw)
		return STATUS_NO_MEMORY;
	memcpy(raw, buffer, len);
	return STATUS_OK;
}

/* flattenBinder(): a local binder, its stability (Stability::tryMarkCompilationUnit) follows */
binder_status_t AParcel_writeStrongBinder(AParcel *parcel, AIBinder *binder)
{
	struct flat_binder_object obj = {.hdr.type = BINDER_TYPE_BINDER, .flags = SCHED_BITS};
	if (binder) {
		binder->parceled = true;
		obj.flags |= FLAT_BINDER_FLAG_ACCEPTS_FDS;
		obj.binder = (uintptr_t)binder->refs;
		obj.cookie = (uintptr_t)binder;
	}
	int32_t err = write_object(parcel, &obj, false);
	if (err != STATUS_OK)
		return err;
	if (binder && binder->stability == 0)
		binder->stability = STABILITY_SYSTEM;
	return binder_parcel_write_i32(parcel, binder ? binder->stability : 0);
}

/* unflattenBinder() + finishUnflattenBinder(): Stability::setRepr() accepts only the level already set */
binder_status_t AParcel_readStrongBinder(const AParcel *parcel, AIBinder **binder)
{
	const struct flat_binder_object *flat = read_object(P(parcel), false);
	if (!flat || flat->hdr.type != BINDER_TYPE_BINDER)
		return STATUS_BAD_TYPE;
	AIBinder *b = (AIBinder *)(uintptr_t)flat->cookie;
	int32_t stability;
	int32_t err = binder_parcel_read_i32(P(parcel), &stability);
	if (err != STATUS_OK)
		return binder_prune_status(err);
	if ((int16_t)stability != (b ? b->stability : 0))
		return STATUS_BAD_TYPE;
	AIBinder_incStrong(b);
	*binder = b;
	return STATUS_OK;
}

/* writeDupParcelFileDescriptor() after the non-null flag */
binder_status_t AParcel_writeParcelFileDescriptor(AParcel *parcel, int fd)
{
	if (fd < 0)
		return fd != -1 ? STATUS_UNKNOWN_ERROR : binder_prune_status(binder_parcel_write_i32(parcel, 0));
	int32_t err = binder_parcel_write_i32(parcel, 1);
	if (err != STATUS_OK)
		return binder_prune_status(err);
	int dup_fd = fcntl(fd, F_DUPFD_CLOEXEC, 0);
	if (dup_fd < 0)
		return binder_prune_status(-errno);
	binder_parcel_write_i32(parcel, 0); /* no comm fd */
	struct flat_binder_object obj = {.hdr.type = BINDER_TYPE_FD, .handle = dup_fd, .cookie = 1};
	err = write_object(parcel, &obj, true);
	if (err != STATUS_OK)
		close(dup_fd);
	return binder_prune_status(err);
}

/* readFileDescriptor() */
static int read_fd(AParcel *p)
{
	const struct flat_binder_object *flat = read_object(p, true);
	return flat && flat->hdr.type == BINDER_TYPE_FD ? (int)flat->handle : STATUS_BAD_TYPE;
}

/* readParcelable(std::optional<ParcelFileDescriptor>*) -> readUniqueParcelFileDescriptor() */
binder_status_t AParcel_readParcelFileDescriptor(const AParcel *parcel, int *fd)
{
	AParcel *p = P(parcel);
	size_t start = p->pos;
	int32_t present, has_comm;
	int32_t err = binder_parcel_read_i32(p, &present);
	if (err != STATUS_OK)
		return binder_prune_status(err);
	if (present == 0) {
		*fd = -1;
		return STATUS_OK;
	}
	p->pos = start;
	binder_parcel_read_i32(p, &present);
	if (present != 1)
		return STATUS_UNEXPECTED_NULL;
	if (binder_parcel_read_i32(p, &has_comm) != STATUS_OK)
		has_comm = 0;
	int got = read_fd(p);
	if (has_comm) { /* tell the sender (ParcelFileDescriptor.java) the fd is detached */
		int comm = read_fd(p);
		const int32_t message = __builtin_bswap32(2); /* DETACHED */
		if (write(comm, &message, sizeof(message)) != sizeof(message))
			got = STATUS_BAD_TYPE;
	}
	if (got == STATUS_BAD_TYPE)
		return STATUS_BAD_TYPE;
	int dup_fd = fcntl(got, F_DUPFD_CLOEXEC, 0);
	if (dup_fd < 0)
		return STATUS_BAD_VALUE;
	*fd = dup_fd;
	return STATUS_OK;
}

/* Status::writeToParcel() */
binder_status_t AParcel_writeStatusHeader(AParcel *parcel, const AStatus *status)
{
	if (status->exception == EX_TRANSACTION_FAILED)
		return binder_prune_status(status->error);
	int32_t err = binder_parcel_write_i32(parcel, status->exception);
	if (err != STATUS_OK || status->exception == EX_NONE)
		return binder_prune_status(err);
	size_t len;
	char16_t *msg = binder_utf16_from_utf8(status->message ? status->message : "", &len);
	if (!msg)
		return STATUS_NO_MEMORY;
	err = binder_parcel_write_string16(parcel, msg, len);
	free(msg);
	if (err == STATUS_OK)
		err = binder_parcel_write_i32(parcel, 0); /* empty remote stack trace header */
	if (err == STATUS_OK && status->exception == EX_SERVICE_SPECIFIC)
		err = binder_parcel_write_i32(parcel, status->error);
	else if (err == STATUS_OK && status->exception == EX_PARCELABLE)
		err = binder_parcel_write_i32(parcel, 0);
	return binder_prune_status(err);
}

/* Status::skipUnusedHeader(): the size includes the size field */
static int32_t skip_header(AParcel *p)
{
	size_t start = p->pos, avail = binder_parcel_avail(p);
	int32_t size;
	int32_t err = binder_parcel_read_i32(p, &size);
	if (err != STATUS_OK)
		return err;
	if (size < 0 || (size_t)size > avail)
		return STATUS_UNKNOWN_ERROR;
	p->pos = start + size;
	return STATUS_OK;
}

/* Status::readFromParcel() */
static int32_t read_status(AParcel *p, AStatus *st)
{
	int32_t err;
	if ((err = binder_parcel_read_i32(p, &st->exception)) != STATUS_OK)
		return err;
	if (st->exception == EX_HAS_NOTED_APPOPS_REPLY_HEADER) {
		if ((err = skip_header(p)) != STATUS_OK || (err = binder_parcel_read_i32(p, &st->exception)) != STATUS_OK)
			return err;
	}
	if (st->exception == EX_HAS_REPLY_HEADER) {
		if ((err = skip_header(p)) != STATUS_OK)
			return err;
		st->exception = EX_NONE;
	}
	if (st->exception == EX_NONE)
		return STATUS_OK;
	size_t start = p->pos, len;
	int32_t peek;
	if ((err = binder_parcel_read_i32(p, &peek)) != STATUS_OK)
		return err;
	if (peek != -1) {
		p->pos = start;
		const char16_t *s = binder_parcel_read_string16_inplace(p, &len);
		if (!s)
			return STATUS_UNEXPECTED_NULL;
		if (!(st->message = binder_utf8_from_utf16(s, len)))
			return STATUS_NO_MEMORY;
	}
	size_t remote_start = p->pos, remote_avail = binder_parcel_avail(p);
	int32_t remote_size;
	if ((err = binder_parcel_read_i32(p, &remote_size)) != STATUS_OK)
		return err;
	if (remote_size < 0 || (size_t)remote_size > remote_avail)
		return STATUS_UNKNOWN_ERROR;
	if (remote_size != 0)
		p->pos = remote_start + remote_size;
	if (st->exception == EX_SERVICE_SPECIFIC)
		return binder_parcel_read_i32(p, &st->error);
	if (st->exception == EX_PARCELABLE)
		return skip_header(p);
	return STATUS_OK;
}

binder_status_t AParcel_readStatusHeader(const AParcel *parcel, AStatus **status)
{
	AStatus *st = calloc(1, sizeof(AStatus));
	if (!st)
		return STATUS_NO_MEMORY;
	int32_t err = read_status(P(parcel), st);
	if (err != STATUS_OK) {
		free(st->message);
		free(st);
		return binder_prune_status(err);
	}
	*status = st;
	return STATUS_OK;
}

binder_status_t AParcel_writeString(AParcel *parcel, const char *string, int32_t length)
{
	if (!string)
		return length != -1 ? STATUS_BAD_VALUE : binder_prune_status(binder_parcel_write_i32(parcel, -1));
	if (length < 0)
		return STATUS_BAD_VALUE;
	ssize_t len16 = utf8_to_utf16_length((const uint8_t *)string, length);
	if (len16 < 0 || len16 >= INT32_MAX)
		return STATUS_BAD_VALUE;
	int32_t err = binder_parcel_write_i32(parcel, len16);
	if (err != STATUS_OK)
		return binder_prune_status(err);
	char16_t *str16 = binder_parcel_write_inplace(parcel, (len16 + 1) * sizeof(char16_t));
	if (!str16)
		return STATUS_NO_MEMORY;
	utf8_to_utf16((const uint8_t *)string, length, str16, len16 + 1);
	return STATUS_OK;
}

binder_status_t AParcel_readString(const AParcel *parcel, void *stringData, AParcel_stringAllocator allocator)
{
	size_t len16;
	const char16_t *str16 = binder_parcel_read_string16_inplace(P(parcel), &len16);
	if (!str16)
		return allocator(stringData, -1, NULL) ? STATUS_OK : STATUS_UNEXPECTED_NULL;
	ssize_t len8 = len16 == 0 ? 1 : utf16_to_utf8_length(str16, len16) + 1;
	if (len8 <= 0 || len8 > INT32_MAX)
		return STATUS_BAD_VALUE;
	char *str8;
	if (!allocator(stringData, len8, &str8) || !str8)
		return STATUS_NO_MEMORY;
	str8[0] = 0;
	if (len16)
		utf16_to_utf8(str16, len16, str8, len8);
	return STATUS_OK;
}

/* WriteAndValidateArraySize() */
static binder_status_t write_array_size(AParcel *p, bool is_null, int32_t length)
{
	if (length < -1 || (!is_null && length < 0) || (is_null && length > 0))
		return STATUS_BAD_VALUE;
	return binder_prune_status(binder_parcel_write_i32(p, length));
}

/* ReadAndValidateArraySize() */
static binder_status_t read_array_size(AParcel *p, int32_t *length)
{
	int32_t err = binder_parcel_read_i32(p, length);
	if (err != STATUS_OK)
		return binder_prune_status(err);
	if (*length < -1)
		return STATUS_BAD_VALUE;
	if (*length > 0 && (size_t)*length > binder_parcel_avail(p))
		return STATUS_NO_MEMORY;
	return STATUS_OK;
}

binder_status_t AParcel_writeStringArray(AParcel *parcel, const void *arrayData, int32_t length, AParcel_stringArrayElementGetter getter)
{
	binder_status_t st = write_array_size(parcel, length < 0, length);
	for (int32_t i = 0; st == STATUS_OK && i < length; i++) {
		int32_t element_length = 0;
		const char *str = getter(arrayData, i, &element_length);
		if (!str && element_length != -1)
			return STATUS_BAD_VALUE;
		st = AParcel_writeString(parcel, str, element_length);
	}
	return st;
}

/* StringArrayElementAllocationAdapter */
struct element_adapter {
	void *array_data;
	int32_t index;
	AParcel_stringArrayElementAllocator element_allocator;
};

static bool element_allocator(void *string_data, int32_t length, char **buffer)
{
	struct element_adapter *a = string_data;
	return a->element_allocator(a->array_data, a->index, length, buffer);
}

binder_status_t AParcel_readStringArray(const AParcel *parcel, void *arrayData, AParcel_stringArrayAllocator allocator,
                                        AParcel_stringArrayElementAllocator elementAllocator)
{
	int32_t length;
	binder_status_t st = read_array_size(P(parcel), &length);
	if (st != STATUS_OK)
		return st;
	if (!allocator(arrayData, length))
		return length < 0 ? STATUS_UNEXPECTED_NULL : STATUS_NO_MEMORY;
	struct element_adapter a = {arrayData, 0, elementAllocator};
	for (; st == STATUS_OK && a.index < length; a.index++)
		st = AParcel_readString(parcel, &a, element_allocator);
	return st;
}

binder_status_t AParcel_writeParcelableArray(AParcel *parcel, const void *arrayData, int32_t length,
                                             AParcel_writeParcelableElement elementWriter)
{
	binder_status_t st = write_array_size(parcel, length < 0, length);
	for (int32_t i = 0; st == STATUS_OK && i < length; i++)
		st = elementWriter(parcel, arrayData, i);
	return st;
}

binder_status_t AParcel_readParcelableArray(const AParcel *parcel, void *arrayData, AParcel_parcelableArrayAllocator allocator,
                                            AParcel_readParcelableElement elementReader)
{
	int32_t length;
	binder_status_t st = read_array_size(P(parcel), &length);
	if (st != STATUS_OK)
		return st;
	if (!allocator(arrayData, length))
		return length < 0 ? STATUS_UNEXPECTED_NULL : STATUS_NO_MEMORY;
	for (int32_t i = 0; st == STATUS_OK && i < length; i++)
		st = elementReader(parcel, arrayData, i);
	return st;
}

/* writeBool/writeChar/writeByte write an int32 */
binder_status_t AParcel_writeInt32(AParcel *parcel, int32_t value) { return binder_prune_status(write_aligned(parcel, &value, sizeof(value))); }
binder_status_t AParcel_writeUint32(AParcel *parcel, uint32_t value) { return binder_prune_status(write_aligned(parcel, &value, sizeof(value))); }
binder_status_t AParcel_writeInt64(AParcel *parcel, int64_t value) { return binder_prune_status(write_aligned(parcel, &value, sizeof(value))); }
binder_status_t AParcel_writeUint64(AParcel *parcel, uint64_t value) { return binder_prune_status(write_aligned(parcel, &value, sizeof(value))); }
binder_status_t AParcel_writeFloat(AParcel *parcel, float value) { return binder_prune_status(write_aligned(parcel, &value, sizeof(value))); }
binder_status_t AParcel_writeDouble(AParcel *parcel, double value) { return binder_prune_status(write_aligned(parcel, &value, sizeof(value))); }
binder_status_t AParcel_writeBool(AParcel *parcel, bool value) { return AParcel_writeInt32(parcel, value); }
binder_status_t AParcel_writeChar(AParcel *parcel, char16_t value) { return AParcel_writeInt32(parcel, value); }
binder_status_t AParcel_writeByte(AParcel *parcel, int8_t value) { return AParcel_writeInt32(parcel, value); }

binder_status_t AParcel_readInt32(const AParcel *parcel, int32_t *value) { return binder_prune_status(read_aligned(P(parcel), value, sizeof(*value))); }
binder_status_t AParcel_readUint32(const AParcel *parcel, uint32_t *value) { return binder_prune_status(read_aligned(P(parcel), value, sizeof(*value))); }
binder_status_t AParcel_readInt64(const AParcel *parcel, int64_t *value) { return binder_prune_status(read_aligned(P(parcel), value, sizeof(*value))); }
binder_status_t AParcel_readUint64(const AParcel *parcel, uint64_t *value) { return binder_prune_status(read_aligned(P(parcel), value, sizeof(*value))); }
binder_status_t AParcel_readFloat(const AParcel *parcel, float *value) { return binder_prune_status(read_aligned(P(parcel), value, sizeof(*value))); }
binder_status_t AParcel_readDouble(const AParcel *parcel, double *value) { return binder_prune_status(read_aligned(P(parcel), value, sizeof(*value))); }

/* Parcel::readBool/readChar/readByte: the value is set (0 on failure) together with the status */
static binder_status_t read_int32_as(const AParcel *parcel, int32_t *tmp)
{
	*tmp = 0;
	return binder_prune_status(read_aligned(P(parcel), tmp, sizeof(*tmp)));
}

binder_status_t AParcel_readBool(const AParcel *parcel, bool *value)
{
	int32_t tmp;
	binder_status_t st = read_int32_as(parcel, &tmp);
	*value = tmp != 0;
	return st;
}

binder_status_t AParcel_readChar(const AParcel *parcel, char16_t *value)
{
	int32_t tmp;
	binder_status_t st = read_int32_as(parcel, &tmp);
	*value = tmp;
	return st;
}

binder_status_t AParcel_readByte(const AParcel *parcel, int8_t *value)
{
	int32_t tmp;
	binder_status_t st = read_int32_as(parcel, &tmp);
	*value = tmp;
	return st;
}

/* WriteArray<T>(): length, then the elements in place */
static binder_status_t write_array(AParcel *p, const void *array, int32_t length, size_t elsize)
{
	binder_status_t st = write_array_size(p, !array, length);
	if (st != STATUS_OK || length <= 0)
		return st;
	int32_t size;
	if (__builtin_mul_overflow(elsize, length, &size))
		return STATUS_NO_MEMORY;
	void *d = binder_parcel_write_inplace(p, size);
	if (!d)
		return STATUS_NO_MEMORY;
	memcpy(d, array, size);
	return STATUS_OK;
}

/* ReadArray<T>() after the allocator */
static binder_status_t read_array(AParcel *p, int32_t length, void *array, size_t elsize)
{
	if (length <= 0)
		return STATUS_OK;
	if (!array)
		return STATUS_NO_MEMORY;
	int32_t size;
	if (__builtin_mul_overflow(elsize, length, &size))
		return STATUS_NO_MEMORY;
	const void *d = binder_parcel_read_inplace(p, size);
	if (!d)
		return STATUS_NO_MEMORY;
	memcpy(array, d, size);
	return STATUS_OK;
}

#define ARRAY_FUNCS(Name, T, Alloc)                                                                                  \
	binder_status_t AParcel_write##Name##Array(AParcel *parcel, const T *arrayData, int32_t length)          \
	{                                                                                                       \
		return write_array(parcel, arrayData, length, sizeof(T));                                       \
	}                                                                                                       \
	binder_status_t AParcel_read##Name##Array(const AParcel *parcel, void *arrayData, Alloc allocator)                   \
	{                                                                                                       \
		int32_t length;                                                                                 \
		T *array;                                                                                       \
		binder_status_t st = read_array_size(P(parcel), &length);                                       \
		if (st != STATUS_OK)                                                                            \
			return st;                                                                              \
		if (!allocator(arrayData, length, &array))                                                      \
			return length < 0 ? STATUS_UNEXPECTED_NULL : STATUS_NO_MEMORY;                          \
		return read_array(P(parcel), length, array, sizeof(T));                                         \
	}


ARRAY_FUNCS(Int32, int32_t, AParcel_int32ArrayAllocator)
ARRAY_FUNCS(Uint32, uint32_t, AParcel_uint32ArrayAllocator)
ARRAY_FUNCS(Int64, int64_t, AParcel_int64ArrayAllocator)
ARRAY_FUNCS(Uint64, uint64_t, AParcel_uint64ArrayAllocator)
ARRAY_FUNCS(Float, float, AParcel_floatArrayAllocator)
ARRAY_FUNCS(Double, double, AParcel_doubleArrayAllocator)
ARRAY_FUNCS(Byte, int8_t, AParcel_byteArrayAllocator)

/* WriteArray<char16_t>/ReadArray<char16_t>: one int32 per char */
binder_status_t AParcel_writeCharArray(AParcel *parcel, const char16_t *arrayData, int32_t length)
{
	binder_status_t st = write_array_size(parcel, !arrayData, length);
	if (st == STATUS_OK && length > 0 && length > INT32_MAX / 2)
		return STATUS_NO_MEMORY;
	for (int32_t i = 0; st == STATUS_OK && i < length; i++)
		st = AParcel_writeChar(parcel, arrayData[i]);
	return st;
}

binder_status_t AParcel_readCharArray(const AParcel *parcel, void *arrayData, AParcel_charArrayAllocator allocator)
{
	int32_t length;
	char16_t *array;
	binder_status_t st = read_array_size(P(parcel), &length);
	if (st != STATUS_OK)
		return st;
	if (!allocator(arrayData, length, &array))
		return length < 0 ? STATUS_UNEXPECTED_NULL : STATUS_NO_MEMORY;
	if (length <= 0)
		return STATUS_OK;
	if (!array || length > INT32_MAX / 2)
		return STATUS_NO_MEMORY;
	for (int32_t i = 0; st == STATUS_OK && i < length; i++)
		st = AParcel_readChar(parcel, array + i);
	return st;
}

/* WriteArray/ReadArray with getter and setter: one int32 per bool */
binder_status_t AParcel_writeBoolArray(AParcel *parcel, const void *arrayData, int32_t length, AParcel_boolArrayGetter getter)
{
	binder_status_t st = write_array_size(parcel, length < 0, length);
	for (int32_t i = 0; st == STATUS_OK && i < length; i++)
		st = AParcel_writeBool(parcel, getter(arrayData, i));
	return st;
}

binder_status_t AParcel_readBoolArray(const AParcel *parcel, void *arrayData, AParcel_boolArrayAllocator allocator,
                                      AParcel_boolArraySetter setter)
{
	int32_t length;
	binder_status_t st = read_array_size(P(parcel), &length);
	if (st != STATUS_OK)
		return st;
	if (!allocator(arrayData, length))
		return length < 0 ? STATUS_UNEXPECTED_NULL : STATUS_NO_MEMORY;
	for (int32_t i = 0; i < length; i++) {
		bool value;
		if ((st = AParcel_readBool(parcel, &value)) != STATUS_OK)
			return st;
		setter(arrayData, i, value);
	}
	return STATUS_OK;
}
