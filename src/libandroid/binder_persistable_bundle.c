/*
 * Copyright (C) 2023 The Android Open Source Project
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

/* APersistableBundle: frameworks/native/libs/binder/ndk/persistable_bundle.cpp on libs/binder/PersistableBundle.cpp
 * (android-16.0.0_r4). PersistableBundle keeps one std::map<String16, T> per type; here one array ordered by
 * (key, type) holds them all, so each type's keys come out in String16 order, as from the maps. */

#include <stdlib.h>
#include <string.h>

#include "binder_ndk.h"

#define APERSISTABLEBUNDLE_KEY_NOT_FOUND (-1)
#define APERSISTABLEBUNDLE_ALLOCATOR_FAILED (-2)

typedef char *(*APersistableBundle_stringAllocator)(int32_t sizeBytes, void *context);

struct str16 {
	char16_t *s;
	size_t len;
};

/* the maps in the order erase() and writeToParcelInner() walk them, with their Parcel.java VAL_* */
enum { T_BOOL, T_INT, T_LONG, T_DOUBLE, T_STRING, T_BOOLV, T_INTV, T_LONGV, T_DOUBLEV, T_STRINGV, T_BUNDLE, T_COUNT };
static const int32_t val_type[T_COUNT] = {9, 1, 6, 8, 0, 23, 18, 19, 28, 14, 25};
static const size_t elem_size[T_COUNT] = {[T_BOOLV] = sizeof(bool), [T_INTV] = 4, [T_LONGV] = 8, [T_DOUBLEV] = 8,
                                          [T_STRINGV] = sizeof(struct str16)};

#define BUNDLE_MAGIC 0x4C444E42
#define BUNDLE_MAGIC_NATIVE 0x4C444E44

typedef struct APersistableBundle APersistableBundle;

void APersistableBundle_delete(APersistableBundle *pBundle);
APersistableBundle *APersistableBundle_dup(const APersistableBundle *pBundle);

struct entry {
	struct str16 key;
	int type;
	union {
		bool b;
		int32_t i;
		int64_t l;
		double d;
		struct str16 str;
		struct {
			int32_t n;
			void *v; /* bool, int32_t, int64_t, double or struct str16 */
		} vec;
		APersistableBundle *bundle;
	};
};

struct APersistableBundle {
	struct entry *e;
	int32_t n;
};

/* strzcmp16(): String16 operator< */
static int key_cmp(const struct str16 *a, const struct str16 *b)
{
	for (size_t i = 0; i < a->len && i < b->len; i++)
		if (a->s[i] != b->s[i])
			return (int)a->s[i] - (int)b->s[i];
	return a->len < b->len ? -1 : a->len > b->len;
}

static bool str_eq(const struct str16 *a, const struct str16 *b)
{
	return key_cmp(a, b) == 0;
}

static struct str16 str_from_utf8(const char *s)
{
	struct str16 r;
	r.s = binder_utf16_from_utf8(s, &r.len);
	return r;
}

static struct str16 str_dup(const struct str16 *s)
{
	struct str16 r = {malloc((s->len + 1) * sizeof(char16_t)), s->len};
	memcpy(r.s, s->s, (s->len + 1) * sizeof(char16_t));
	return r;
}

APersistableBundle *APersistableBundle_new(void)
{
	return calloc(1, sizeof(APersistableBundle));
}

static void entry_free(struct entry *e)
{
	free(e->key.s);
	if (e->type == T_STRING)
		free(e->str.s);
	else if (e->type == T_STRINGV)
		for (int32_t i = 0; i < e->vec.n; i++)
			free(((struct str16 *)e->vec.v)[i].s);
	else if (e->type == T_BUNDLE)
		APersistableBundle_delete(e->bundle);
	if (e->type >= T_BOOLV && e->type <= T_STRINGV)
		free(e->vec.v);
}

void APersistableBundle_delete(APersistableBundle *pBundle)
{
	if (!pBundle)
		return;
	for (int32_t i = 0; i < pBundle->n; i++)
		entry_free(&pBundle->e[i]);
	free(pBundle->e);
	free(pBundle);
}

static struct entry entry_dup(const struct entry *e)
{
	struct entry r = *e;
	r.key = str_dup(&e->key);
	if (e->type == T_STRING)
		r.str = str_dup(&e->str);
	else if (e->type == T_BUNDLE)
		r.bundle = APersistableBundle_dup(e->bundle);
	if (e->type >= T_BOOLV && e->type <= T_STRINGV) {
		r.vec.v = malloc(e->vec.n * elem_size[e->type] + 1);
		memcpy(r.vec.v, e->vec.v, e->vec.n * elem_size[e->type]);
		if (e->type == T_STRINGV)
			for (int32_t i = 0; i < e->vec.n; i++)
				((struct str16 *)r.vec.v)[i] = str_dup(&((struct str16 *)e->vec.v)[i]);
	}
	return r;
}

APersistableBundle *APersistableBundle_dup(const APersistableBundle *pBundle)
{
	APersistableBundle *r = APersistableBundle_new();
	if (!r || !pBundle)
		return r;
	r->e = malloc(pBundle->n * sizeof(struct entry) + 1);
	for (r->n = 0; r->n < pBundle->n; r->n++)
		r->e[r->n] = entry_dup(&pBundle->e[r->n]);
	return r;
}

static int find(const APersistableBundle *b, const struct str16 *key, int type)
{
	for (int32_t i = 0; i < b->n; i++)
		if (b->e[i].type == type && str_eq(&b->e[i].key, key))
			return i;
	return -1;
}

/* map[key] = value: replaces the entry of that type or inserts it at its place; takes the entry's memory */
static void set_entry(APersistableBundle *b, struct entry e)
{
	int i = find(b, &e.key, e.type);
	if (i >= 0) {
		entry_free(&b->e[i]);
		b->e[i] = e;
		return;
	}
	struct entry *n = realloc(b->e, (b->n + 1) * sizeof(struct entry));
	if (!n) {
		entry_free(&e);
		return;
	}
	b->e = n;
	for (i = b->n; i > 0; i--) {
		int c = key_cmp(&b->e[i - 1].key, &e.key);
		if (c < 0 || (c == 0 && b->e[i - 1].type < e.type))
			break;
		b->e[i] = b->e[i - 1];
	}
	b->e[i] = e;
	b->n++;
}

/* PersistableBundle::erase(): the first map (in type order) with the key */
static int32_t erase(APersistableBundle *b, const struct str16 *key)
{
	for (int type = 0; type < T_COUNT; type++) {
		int i = find(b, key, type);
		if (i >= 0) {
			entry_free(&b->e[i]);
			memmove(&b->e[i], &b->e[i + 1], (b->n - i - 1) * sizeof(struct entry));
			b->n--;
			return 1;
		}
	}
	return 0;
}

static void put(APersistableBundle *b, const char *key, struct entry e)
{
	e.key = str_from_utf8(key);
	erase(b, &e.key);
	set_entry(b, e);
}

static const struct entry *get(const APersistableBundle *b, const char *key, int type)
{
	struct str16 k = str_from_utf8(key);
	int i = find(b, &k, type);
	free(k.s);
	return i >= 0 ? &b->e[i] : NULL;
}

int32_t APersistableBundle_size(const APersistableBundle *pBundle)
{
	return pBundle->n;
}

int32_t APersistableBundle_erase(APersistableBundle *pBundle, const char *key)
{
	struct str16 k = str_from_utf8(key);
	int32_t r = erase(pBundle, &k);
	free(k.s);
	return r;
}

bool APersistableBundle_isEqual(const APersistableBundle *lhs, const APersistableBundle *rhs)
{
	if (!lhs || !rhs)
		return lhs == rhs;
	if (lhs->n != rhs->n)
		return false;
	for (int32_t i = 0; i < lhs->n; i++) {
		const struct entry *a = &lhs->e[i], *b = &rhs->e[i];
		if (a->type != b->type || !str_eq(&a->key, &b->key))
			return false;
		switch (a->type) {
		case T_BOOL: if (a->b != b->b) return false; break;
		case T_INT: if (a->i != b->i) return false; break;
		case T_LONG: if (a->l != b->l) return false; break;
		case T_DOUBLE: if (a->d != b->d) return false; break;
		case T_STRING: if (!str_eq(&a->str, &b->str)) return false; break;
		case T_BUNDLE: if (!APersistableBundle_isEqual(a->bundle, b->bundle)) return false; break;
		case T_DOUBLEV:
			if (a->vec.n != b->vec.n)
				return false;
			for (int32_t j = 0; j < a->vec.n; j++)
				if (((double *)a->vec.v)[j] != ((double *)b->vec.v)[j])
					return false;
			break;
		case T_STRINGV:
			if (a->vec.n != b->vec.n)
				return false;
			for (int32_t j = 0; j < a->vec.n; j++)
				if (!str_eq(&((struct str16 *)a->vec.v)[j], &((struct str16 *)b->vec.v)[j]))
					return false;
			break;
		default:
			if (a->vec.n != b->vec.n || memcmp(a->vec.v, b->vec.v, a->vec.n * elem_size[a->type]))
				return false;
		}
	}
	return true;
}

/* --- put --- */

void APersistableBundle_putBoolean(APersistableBundle *pBundle, const char *key, bool val) { put(pBundle, key, (struct entry){.type = T_BOOL, .b = val}); }
void APersistableBundle_putInt(APersistableBundle *pBundle, const char *key, int32_t val) { put(pBundle, key, (struct entry){.type = T_INT, .i = val}); }
void APersistableBundle_putLong(APersistableBundle *pBundle, const char *key, int64_t val) { put(pBundle, key, (struct entry){.type = T_LONG, .l = val}); }
void APersistableBundle_putDouble(APersistableBundle *pBundle, const char *key, double val) { put(pBundle, key, (struct entry){.type = T_DOUBLE, .d = val}); }

void APersistableBundle_putString(APersistableBundle *pBundle, const char *key, const char *val)
{
	put(pBundle, key, (struct entry){.type = T_STRING, .str = str_from_utf8(val)});
}

static void put_vector(APersistableBundle *b, const char *key, int type, const void *vec, int32_t num)
{
	if (num < 0)
		abort(); /* LOG_ALWAYS_FATAL_IF(num < 0) */
	struct entry e = {.type = type, .vec = {num, malloc(num * elem_size[type] + 1)}};
	if (type == T_STRINGV)
		for (int32_t i = 0; i < num; i++)
			((struct str16 *)e.vec.v)[i] = str_from_utf8(((const char *const *)vec)[i]);
	else
		memcpy(e.vec.v, vec, num * elem_size[type]);
	put(b, key, e);
}

void APersistableBundle_putBooleanVector(APersistableBundle *pBundle, const char *key, const bool *vec, int32_t num) { put_vector(pBundle, key, T_BOOLV, vec, num); }
void APersistableBundle_putIntVector(APersistableBundle *pBundle, const char *key, const int32_t *vec, int32_t num) { put_vector(pBundle, key, T_INTV, vec, num); }
void APersistableBundle_putLongVector(APersistableBundle *pBundle, const char *key, const int64_t *vec, int32_t num) { put_vector(pBundle, key, T_LONGV, vec, num); }
void APersistableBundle_putDoubleVector(APersistableBundle *pBundle, const char *key, const double *vec, int32_t num) { put_vector(pBundle, key, T_DOUBLEV, vec, num); }
void APersistableBundle_putStringVector(APersistableBundle *pBundle, const char *key, const char *const *vec, int32_t num) { put_vector(pBundle, key, T_STRINGV, vec, num); }

void APersistableBundle_putPersistableBundle(APersistableBundle *pBundle, const char *key, const APersistableBundle *val)
{
	put(pBundle, key, (struct entry){.type = T_BUNDLE, .bundle = APersistableBundle_dup(val)});
}

/* --- get --- */

#define GET_SCALAR(Name, T, type, field)                                                                \
	bool APersistableBundle_get##Name(const APersistableBundle *pBundle, const char *key, T *val)    \
	{                                                                                               \
		const struct entry *e = get(pBundle, key, type);                                        \
		if (e)                                                                                  \
			*val = e->field;                                                                \
		return e != NULL;                                                                       \
	}

GET_SCALAR(Boolean, bool, T_BOOL, b)
GET_SCALAR(Int, int32_t, T_INT, i)
GET_SCALAR(Long, int64_t, T_LONG, l)
GET_SCALAR(Double, double, T_DOUBLE, d)

/* String8(String16) handed to the allocator: the byte length without the NUL */
static int32_t give_string(const struct str16 *s, char **out, APersistableBundle_stringAllocator allocator, void *context)
{
	char *utf8 = binder_utf8_from_utf16(s->s, s->len);
	if (!utf8)
		return APERSISTABLEBUNDLE_ALLOCATOR_FAILED;
	int32_t len = strlen(utf8);
	*out = allocator(len + 1, context);
	if (*out)
		memcpy(*out, utf8, len + 1);
	free(utf8);
	return *out ? len : APERSISTABLEBUNDLE_ALLOCATOR_FAILED;
}

int32_t APersistableBundle_getString(const APersistableBundle *pBundle, const char *key, char **val,
                                     APersistableBundle_stringAllocator stringAllocator, void *context)
{
	const struct entry *e = get(pBundle, key, T_STRING);
	if (!e)
		return APERSISTABLEBUNDLE_KEY_NOT_FOUND;
	return give_string(&e->str, val, stringAllocator, context);
}

/* getVecInternal(): copies what fits, returns the size of all in bytes */
static int32_t get_vector(const APersistableBundle *b, const char *key, int type, void *buffer, int32_t bufferSizeBytes)
{
	const struct entry *e = get(b, key, type);
	if (!e)
		return APERSISTABLEBUNDLE_KEY_NOT_FOUND;
	if (bufferSizeBytes < 0)
		abort();
	int32_t fill = bufferSizeBytes / (int32_t)elem_size[type];
	if (fill > e->vec.n)
		fill = e->vec.n;
	if (fill > 0 && buffer)
		memcpy(buffer, e->vec.v, fill * elem_size[type]);
	return e->vec.n * elem_size[type];
}

int32_t APersistableBundle_getBooleanVector(const APersistableBundle *pBundle, const char *key, bool *buffer, int32_t bufferSizeBytes) { return get_vector(pBundle, key, T_BOOLV, buffer, bufferSizeBytes); }
int32_t APersistableBundle_getIntVector(const APersistableBundle *pBundle, const char *key, int32_t *buffer, int32_t bufferSizeBytes) { return get_vector(pBundle, key, T_INTV, buffer, bufferSizeBytes); }
int32_t APersistableBundle_getLongVector(const APersistableBundle *pBundle, const char *key, int64_t *buffer, int32_t bufferSizeBytes) { return get_vector(pBundle, key, T_LONGV, buffer, bufferSizeBytes); }
int32_t APersistableBundle_getDoubleVector(const APersistableBundle *pBundle, const char *key, double *buffer, int32_t bufferSizeBytes) { return get_vector(pBundle, key, T_DOUBLEV, buffer, bufferSizeBytes); }

/* getStringsInternal(): the strings of a vector (vec) or the keys of one type (keys_type) */
static int32_t get_strings(const APersistableBundle *b, const struct entry *vec, int keys_type, char **buffer, int32_t bufferSizeBytes,
                           APersistableBundle_stringAllocator allocator, void *context)
{
	if (bufferSizeBytes < 0)
		abort();
	int32_t num = 0;
	if (vec)
		num = vec->vec.n;
	else
		for (int32_t i = 0; i < b->n; i++)
			num += b->e[i].type == keys_type;
	int32_t fill = bufferSizeBytes / (int32_t)sizeof(char *);
	if (fill > num)
		fill = num;
	if (!allocator)
		return APERSISTABLEBUNDLE_ALLOCATOR_FAILED;
	if (fill > 0 && buffer) {
		int32_t n = 0;
		for (int32_t i = 0; n < fill; i++) {
			const struct str16 *s = vec ? &((struct str16 *)vec->vec.v)[i] : b->e[i].type == keys_type ? &b->e[i].key : NULL;
			if (s && give_string(s, &buffer[n++], allocator, context) < 0)
				return APERSISTABLEBUNDLE_ALLOCATOR_FAILED;
		}
	}
	return num * sizeof(char *);
}

int32_t APersistableBundle_getStringVector(const APersistableBundle *pBundle, const char *key, char **vec, int32_t bufferSizeBytes,
                                           APersistableBundle_stringAllocator stringAllocator, void *context)
{
	const struct entry *e = get(pBundle, key, T_STRINGV);
	if (!e)
		return APERSISTABLEBUNDLE_KEY_NOT_FOUND;
	return get_strings(pBundle, e, 0, vec, bufferSizeBytes, stringAllocator, context);
}

bool APersistableBundle_getPersistableBundle(const APersistableBundle *pBundle, const char *key, APersistableBundle **outBundle)
{
	const struct entry *e = get(pBundle, key, T_BUNDLE);
	if (e)
		*outBundle = APersistableBundle_dup(e->bundle);
	return e != NULL;
}

#define GET_KEYS(Name, type)                                                                                                     \
	int32_t APersistableBundle_get##Name##Keys(const APersistableBundle *pBundle, char **outKeys, int32_t bufferSizeBytes,   \
	                                           APersistableBundle_stringAllocator stringAllocator, void *context)            \
	{                                                                                                                        \
		return get_strings(pBundle, NULL, type, outKeys, bufferSizeBytes, stringAllocator, context);                     \
	}

GET_KEYS(Boolean, T_BOOL)
GET_KEYS(Int, T_INT)
GET_KEYS(Long, T_LONG)
GET_KEYS(Double, T_DOUBLE)
GET_KEYS(String, T_STRING)
GET_KEYS(BooleanVector, T_BOOLV)
GET_KEYS(IntVector, T_INTV)
GET_KEYS(LongVector, T_LONGV)
GET_KEYS(DoubleVector, T_DOUBLEV)
GET_KEYS(StringVector, T_STRINGV)
GET_KEYS(PersistableBundle, T_BUNDLE)

/* --- parcel: PersistableBundle::writeToParcel()/readFromParcel(), in sync with BaseBundle.java --- */

#define RETURN_IF_FAILED(call)                    \
	do {                                      \
		int32_t status_ = (call);         \
		if (status_ != STATUS_OK)         \
			return status_;           \
	} while (0)

static int32_t write_bundle(const APersistableBundle *b, AParcel *p);

static int32_t write_value(const struct entry *e, AParcel *p)
{
	switch (e->type) {
	case T_BOOL: return binder_parcel_write_i32(p, e->b);
	case T_INT: return binder_parcel_write_i32(p, e->i);
	case T_LONG: return binder_parcel_write_i64(p, e->l);
	case T_DOUBLE: { /* writeDouble(): an aligned 8 byte value */
		int64_t bits;
		memcpy(&bits, &e->d, sizeof(bits));
		return binder_parcel_write_i64(p, bits);
	}
	case T_STRING: return binder_parcel_write_string16(p, e->str.s, e->str.len);
	case T_BUNDLE: return write_bundle(e->bundle, p);
	case T_BOOLV: {
		binder_parcel_write_i32(p, e->vec.n);
		int32_t *d = binder_parcel_write_inplace(p, e->vec.n * sizeof(int32_t));
		if (!d)
			return STATUS_BAD_VALUE;
		for (int32_t i = 0; i < e->vec.n; i++)
			d[i] = ((bool *)e->vec.v)[i];
		return STATUS_OK;
	}
	case T_STRINGV:
		RETURN_IF_FAILED(binder_parcel_write_i32(p, e->vec.n));
		for (int32_t i = 0; i < e->vec.n; i++) {
			struct str16 *s = &((struct str16 *)e->vec.v)[i];
			RETURN_IF_FAILED(binder_parcel_write_string16(p, s->s, s->len));
		}
		return STATUS_OK;
	default: /* int32, int64, double vectors: written in one piece */
		binder_parcel_write_i32(p, e->vec.n);
		return binder_parcel_write(p, e->vec.v, e->vec.n * elem_size[e->type]);
	}
}

static int32_t write_bundle(const APersistableBundle *b, AParcel *p)
{
	if (b->n == 0)
		return binder_parcel_write_i32(p, 0);
	size_t length_pos = p->pos;
	RETURN_IF_FAILED(binder_parcel_write_i32(p, 1)); /* length, patched below */
	RETURN_IF_FAILED(binder_parcel_write_i32(p, BUNDLE_MAGIC_NATIVE));
	size_t start_pos = p->pos;
	RETURN_IF_FAILED(binder_parcel_write_i32(p, b->n));
	for (int type = 0; type < T_COUNT; type++) {
		for (int32_t i = 0; i < b->n; i++) {
			if (b->e[i].type != type)
				continue;
			RETURN_IF_FAILED(binder_parcel_write_string16(p, b->e[i].key.s, b->e[i].key.len));
			RETURN_IF_FAILED(binder_parcel_write_i32(p, val_type[type]));
			RETURN_IF_FAILED(write_value(&b->e[i], p));
		}
	}
	size_t end_pos = p->pos;
	if (end_pos - start_pos > INT32_MAX)
		return STATUS_BAD_VALUE;
	p->pos = length_pos;
	RETURN_IF_FAILED(binder_parcel_write_i32(p, end_pos - start_pos));
	p->pos = end_pos;
	return binder_parcel_write_i32(p, 0); /* mHasIntent: false */
}

/* Parcel::readString16(String16 *) */
static int32_t read_str(AParcel *p, struct str16 *out)
{
	const char16_t *s = binder_parcel_read_string16_inplace(p, &out->len);
	out->s = malloc((out->len + 1) * sizeof(char16_t));
	if (!out->s)
		return STATUS_NO_MEMORY;
	if (s)
		memcpy(out->s, s, out->len * sizeof(char16_t));
	out->s[out->len] = 0;
	return s ? STATUS_OK : STATUS_UNEXPECTED_NULL;
}

/* Parcel::readData(std::vector<T> *) */
static int32_t read_vector(AParcel *p, struct entry *e)
{
	int32_t size;
	RETURN_IF_FAILED(binder_parcel_read_i32(p, &size));
	if (size < 0)
		return STATUS_UNEXPECTED_NULL;
	if ((size_t)size > binder_parcel_avail(p))
		return STATUS_BAD_VALUE;
	e->vec.n = 0;
	if (!(e->vec.v = calloc(size + 1, elem_size[e->type])))
		return STATUS_NO_MEMORY;
	if (e->type == T_STRINGV) {
		for (; e->vec.n < size; e->vec.n++)
			RETURN_IF_FAILED(read_str(p, &((struct str16 *)e->vec.v)[e->vec.n]));
		return STATUS_OK;
	}
	size_t wire = e->type == T_BOOLV ? sizeof(int32_t) : elem_size[e->type];
	const void *d = binder_parcel_read_inplace(p, size * wire);
	if (!d)
		return STATUS_BAD_VALUE;
	e->vec.n = size;
	if (e->type == T_BOOLV)
		for (int32_t i = 0; i < size; i++)
			((bool *)e->vec.v)[i] = ((const int32_t *)d)[i] != 0;
	else
		memcpy(e->vec.v, d, size * wire);
	return STATUS_OK;
}

static int32_t read_bundle(APersistableBundle *b, AParcel *p);

static int32_t read_value(AParcel *p, struct entry *e)
{
	int32_t v, err;
	int64_t bits;
	switch (e->type) {
	case T_STRING: return read_str(p, &e->str);
	case T_INT: return binder_parcel_read_i32(p, &e->i);
	case T_LONG: return binder_parcel_read_i64(p, &e->l);
	case T_DOUBLE:
		if ((err = binder_parcel_read_i64(p, &bits)) == STATUS_OK)
			memcpy(&e->d, &bits, sizeof(bits));
		return err;
	case T_BOOL:
		err = binder_parcel_read_i32(p, &v);
		e->b = err == STATUS_OK && v != 0;
		return err;
	case T_BUNDLE: return (e->bundle = APersistableBundle_new()) ? read_bundle(e->bundle, p) : STATUS_NO_MEMORY;
	default: return read_vector(p, e);
	}
}

static int32_t read_bundle(APersistableBundle *b, AParcel *p)
{
	int32_t length, magic, num;
	if (binder_parcel_read_i32(p, &length) != STATUS_OK)
		length = 0; /* readInt32() */
	if (length < 0)
		return STATUS_UNEXPECTED_NULL;
	if (length == 0)
		return STATUS_OK;
	RETURN_IF_FAILED(binder_parcel_read_i32(p, &magic));
	if (magic != BUNDLE_MAGIC && magic != BUNDLE_MAGIC_NATIVE)
		return STATUS_BAD_VALUE;
	RETURN_IF_FAILED(binder_parcel_read_i32(p, &num));
	for (; num > 0; --num) {
		struct entry e = {0};
		int32_t value_type, err;
		if ((err = read_str(p, &e.key)) != STATUS_OK || (err = binder_parcel_read_i32(p, &value_type)) != STATUS_OK) {
			free(e.key.s);
			return err;
		}
		for (e.type = 0; e.type < T_COUNT && val_type[e.type] != value_type; e.type++)
			;
		if (e.type == T_COUNT) {
			free(e.key.s);
			return STATUS_BAD_TYPE;
		}
		err = read_value(p, &e);
		set_entry(b, e);
		if (err != STATUS_OK)
			return err;
	}
	/* RETURN_IF_FAILED(parcel->readBool()): mHasIntent, a true value is returned as status 1 */
	int32_t has_intent;
	if (binder_parcel_read_i32(p, &has_intent) != STATUS_OK)
		has_intent = 0;
	return has_intent != 0;
}

binder_status_t APersistableBundle_readFromParcel(const AParcel *parcel, APersistableBundle **outPBundle)
{
	if (!parcel || !outPBundle)
		return STATUS_BAD_VALUE;
	APersistableBundle *b = APersistableBundle_new();
	if (!b)
		return STATUS_NO_MEMORY;
	binder_status_t status = read_bundle(b, (AParcel *)parcel);
	if (status == STATUS_OK)
		*outPBundle = b;
	else
		APersistableBundle_delete(b);
	return status;
}

binder_status_t APersistableBundle_writeToParcel(const APersistableBundle *pBundle, AParcel *parcel)
{
	if (!parcel || !pBundle)
		return STATUS_BAD_VALUE;
	return write_bundle(pBundle, parcel);
}
