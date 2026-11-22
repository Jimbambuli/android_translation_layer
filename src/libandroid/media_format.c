/*
 * NdkMediaFormat (libmediandk) as in AOSP frameworks/av/media/ndk/NdkMediaFormat.cpp: a native map of typed
 * entries like AOSP's AMessage (entries keep their order; setting a name again replaces its value and type;
 * a getter finds only an entry of its own type).
 */

#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <glib.h>

#include "media_ndk.h"
#include "ndk_jni.h"

enum entry_type { /* AMessage::Type */
	TYPE_INT32,
	TYPE_INT64,
	TYPE_SIZE,
	TYPE_FLOAT,
	TYPE_DOUBLE,
	TYPE_STRING,
	TYPE_BUFFER,
	TYPE_RECT,
};

struct entry {
	char *name;
	enum entry_type type;
	union {
		int32_t i32;
		int64_t i64;
		size_t size;
		float f;
		double d;
		char *string;
		struct {
			void *data;
			size_t size;
		} buffer;
		struct {
			int32_t left, top, right, bottom;
		} rect;
	} u;
};

struct AMediaFormat {
	GArray *entries;
	char *debug;
};

#include "media_format_keys.inc"

static void entry_clear(struct entry *e)
{
	g_free(e->name);
	if (e->type == TYPE_STRING)
		g_free(e->u.string);
	else if (e->type == TYPE_BUFFER)
		g_free(e->u.buffer.data);
}

static struct entry *find(const AMediaFormat *format, const char *name, enum entry_type type)
{
	if (!format || !name)
		return NULL;
	for (guint i = 0; i < format->entries->len; i++) {
		struct entry *e = &g_array_index(format->entries, struct entry, i);
		if (!strcmp(e->name, name))
			return e->type == type ? e : NULL;
	}
	return NULL;
}

/* the entry of this name with its old value freed, appended if new */
static struct entry *set(AMediaFormat *format, const char *name, enum entry_type type)
{
	if (!format || !name)
		return NULL;
	struct entry *e = NULL;
	for (guint i = 0; i < format->entries->len && !e; i++) {
		if (!strcmp(g_array_index(format->entries, struct entry, i).name, name))
			e = &g_array_index(format->entries, struct entry, i);
	}
	if (e) {
		entry_clear(e);
	} else {
		g_array_set_size(format->entries, format->entries->len + 1);
		e = &g_array_index(format->entries, struct entry, format->entries->len - 1);
	}
	e->name = g_strdup(name);
	e->type = type;
	return e;
}

AMediaFormat *AMediaFormat_new(void)
{
	AMediaFormat *format = calloc(1, sizeof(AMediaFormat));
	format->entries = g_array_new(FALSE, TRUE, sizeof(struct entry));
	g_array_set_clear_func(format->entries, (GDestroyNotify)entry_clear);
	return format;
}

media_status_t AMediaFormat_delete(AMediaFormat *format)
{
	if (format) {
		g_array_unref(format->entries);
		g_free(format->debug);
		free(format);
	}
	return AMEDIA_OK;
}

void AMediaFormat_clear(AMediaFormat *format)
{
	g_array_set_size(format->entries, 0);
}

media_status_t AMediaFormat_copy(AMediaFormat *to, AMediaFormat *from)
{
	if (!to || !from)
		return AMEDIA_ERROR_INVALID_PARAMETER;
	if (to == from)
		return AMEDIA_OK;
	g_array_set_size(to->entries, 0);
	for (guint i = 0; i < from->entries->len; i++) {
		struct entry e = g_array_index(from->entries, struct entry, i);
		e.name = g_strdup(e.name);
		if (e.type == TYPE_STRING)
			e.u.string = g_strdup(e.u.string);
		else if (e.type == TYPE_BUFFER)
			e.u.buffer.data = g_memdup2(e.u.buffer.data, e.u.buffer.size);
		g_array_append_val(to->entries, e);
	}
	return AMEDIA_OK;
}

/* AOSP: "name: type(value), ..." closed by "}" (there is no opening brace) */
const char *AMediaFormat_toString(AMediaFormat *format)
{
	GString *ret = g_string_new(NULL);
	for (guint i = 0; i < format->entries->len; i++) {
		struct entry *e = &g_array_index(format->entries, struct entry, i);
		if (i != 0)
			g_string_append(ret, ", ");
		g_string_append_printf(ret, "%s: ", e->name);
		switch (e->type) {
			case TYPE_INT32:
				g_string_append_printf(ret, "int32(%" PRId32 ")", e->u.i32);
				break;
			case TYPE_INT64:
				g_string_append_printf(ret, "int64(%" PRId64 ")", e->u.i64);
				break;
			case TYPE_SIZE:
				g_string_append_printf(ret, "size_t(%zu)", e->u.size);
				break;
			case TYPE_FLOAT:
				g_string_append_printf(ret, "float(%f)", e->u.f);
				break;
			case TYPE_DOUBLE:
				g_string_append_printf(ret, "double(%f)", e->u.d);
				break;
			case TYPE_RECT:
				g_string_append_printf(ret, "Rect(%" PRId32 ", %" PRId32 ", %" PRId32 ", %" PRId32 ")",
				                       e->u.rect.left, e->u.rect.top, e->u.rect.right, e->u.rect.bottom);
				break;
			case TYPE_STRING:
				g_string_append_printf(ret, "string(%s)", e->u.string);
				break;
			case TYPE_BUFFER:
				g_string_append(ret, "data");
				break;
		}
	}
	g_string_append(ret, "}");
	g_free(format->debug);
	format->debug = g_string_free(ret, FALSE);
	return format->debug;
}

#define GETTER(name, ctype, TYPE, field)                                       \
	bool AMediaFormat_get##name(AMediaFormat *format, const char *key, ctype *out) \
	{                                                                      \
		struct entry *e = find(format, key, TYPE);                     \
		if (!e)                                                        \
			return false;                                          \
		*out = e->u.field;                                             \
		return true;                                                   \
	}
#define SETTER(name, ctype, TYPE, field)                                            \
	void AMediaFormat_set##name(AMediaFormat *format, const char *key, ctype value) \
	{                                                                           \
		struct entry *e = set(format, key, TYPE);                           \
		if (e)                                                              \
			e->u.field = value;                                         \
	}

GETTER(Int32, int32_t, TYPE_INT32, i32)
GETTER(Int64, int64_t, TYPE_INT64, i64)
GETTER(Float, float, TYPE_FLOAT, f)
GETTER(Double, double, TYPE_DOUBLE, d)
GETTER(Size, size_t, TYPE_SIZE, size)
SETTER(Int32, int32_t, TYPE_INT32, i32)
SETTER(Int64, int64_t, TYPE_INT64, i64)
SETTER(Float, float, TYPE_FLOAT, f)
SETTER(Double, double, TYPE_DOUBLE, d)
SETTER(Size, size_t, TYPE_SIZE, size)

bool AMediaFormat_getRect(AMediaFormat *format, const char *name, int32_t *left, int32_t *top, int32_t *right, int32_t *bottom)
{
	struct entry *e = find(format, name, TYPE_RECT);
	if (!e)
		return false;
	*left = e->u.rect.left;
	*top = e->u.rect.top;
	*right = e->u.rect.right;
	*bottom = e->u.rect.bottom;
	return true;
}

void AMediaFormat_setRect(AMediaFormat *format, const char *name, int32_t left, int32_t top, int32_t right, int32_t bottom)
{
	struct entry *e = set(format, name, TYPE_RECT);
	if (e) {
		e->u.rect.left = left;
		e->u.rect.top = top;
		e->u.rect.right = right;
		e->u.rect.bottom = bottom;
	}
}

/* the data belongs to the format and stays valid while the entry exists */
bool AMediaFormat_getBuffer(AMediaFormat *format, const char *name, void **data, size_t *size)
{
	struct entry *e = find(format, name, TYPE_BUFFER);
	if (!e)
		return false;
	*data = e->u.buffer.data;
	*size = e->u.buffer.size;
	return true;
}

/* the data is copied */
void AMediaFormat_setBuffer(AMediaFormat *format, const char *name, const void *data, size_t size)
{
	void *copy = g_memdup2(data, size);
	struct entry *e = set(format, name, TYPE_BUFFER);
	if (!e) {
		g_free(copy);
		return;
	}
	e->u.buffer.data = copy;
	e->u.buffer.size = size;
}

/* AOSP keeps a copy per name until the next getString() of that name or the deletion of the format; the string
 * of the entry itself lives at least as long */
bool AMediaFormat_getString(AMediaFormat *format, const char *name, const char **out)
{
	struct entry *e = find(format, name, TYPE_STRING);
	if (!e)
		return false;
	*out = e->u.string;
	return true;
}

/* the string is copied */
void AMediaFormat_setString(AMediaFormat *format, const char *name, const char *value)
{
	char *copy = g_strdup(value);
	struct entry *e = set(format, name, TYPE_STRING);
	if (!e) {
		g_free(copy);
		return;
	}
	e->u.string = copy;
}

/* AOSP media/jni android_media_Utils ConvertMessageToMap: the entries as Integer, Long, Float, String and ByteBuffer
 * values of a MediaFormat (MediaFormat has no setter for the other types; a size goes as Long, like AOSP's
 * kTypeSize) */
jobject AMediaFormat_toJava(JNIEnv *env, const AMediaFormat *format)
{
	jclass class = ndk_jni_class(env, "android/media/MediaFormat");
	static jmethodID ctor, set_int, set_long, set_float, set_string, set_buffer;
	if (!ctor) {
		set_int = (*env)->GetMethodID(env, class, "setInteger", "(Ljava/lang/String;I)V");
		set_long = (*env)->GetMethodID(env, class, "setLong", "(Ljava/lang/String;J)V");
		set_float = (*env)->GetMethodID(env, class, "setFloat", "(Ljava/lang/String;F)V");
		set_string = (*env)->GetMethodID(env, class, "setString", "(Ljava/lang/String;Ljava/lang/String;)V");
		set_buffer = (*env)->GetMethodID(env, class, "setByteBuffer", "(Ljava/lang/String;Ljava/nio/ByteBuffer;)V");
		ctor = (*env)->GetMethodID(env, class, "<init>", "()V");
	}
	jobject jformat = (*env)->NewObject(env, class, ctor);
	if (!format)
		return jformat;
	for (guint i = 0; i < format->entries->len; i++) {
		struct entry *e = &g_array_index(format->entries, struct entry, i);
		jstring name = (*env)->NewStringUTF(env, e->name);
		switch (e->type) {
			case TYPE_INT32:
				(*env)->CallVoidMethod(env, jformat, set_int, name, e->u.i32);
				break;
			case TYPE_INT64:
				(*env)->CallVoidMethod(env, jformat, set_long, name, e->u.i64);
				break;
			case TYPE_SIZE:
				(*env)->CallVoidMethod(env, jformat, set_long, name, (jlong)e->u.size);
				break;
			case TYPE_FLOAT:
				(*env)->CallVoidMethod(env, jformat, set_float, name, e->u.f);
				break;
			case TYPE_STRING: {
				jstring value = (*env)->NewStringUTF(env, e->u.string);
				(*env)->CallVoidMethod(env, jformat, set_string, name, value);
				(*env)->DeleteLocalRef(env, value);
				break;
			}
			case TYPE_BUFFER: {
				/* a direct buffer of its own, as the format may outlive the AMediaFormat */
				jclass byte_buffer = ndk_jni_class(env, "java/nio/ByteBuffer");
				static jmethodID allocate_direct;
				if (!allocate_direct)
					allocate_direct = (*env)->GetStaticMethodID(env, byte_buffer, "allocateDirect", "(I)Ljava/nio/ByteBuffer;");
				jobject buffer = (*env)->CallStaticObjectMethod(env, byte_buffer, allocate_direct, (jint)e->u.buffer.size);
				memcpy((*env)->GetDirectBufferAddress(env, buffer), e->u.buffer.data, e->u.buffer.size);
				(*env)->CallVoidMethod(env, jformat, set_buffer, name, buffer);
				(*env)->DeleteLocalRef(env, buffer);
				break;
			}
			default:
				break;
		}
		(*env)->DeleteLocalRef(env, name);
		ndk_jni_threw(env, "AMediaFormat_toJava");
	}
	return jformat;
}

/* AOSP ConvertKeyValueArraysToMessage: Integer, Long, Float, String and the bytes of a ByteBuffer from its
 * position to its limit */
AMediaFormat *AMediaFormat_fromJava(JNIEnv *env, jobject jformat)
{
	if (!jformat)
		return NULL;
	jclass class = ndk_jni_class(env, "android/media/MediaFormat");
	static jmethodID get_keys, get_type, get_int, get_long, get_float, get_string, get_buffer, to_array, b_position, b_limit, b_get;
	if (!get_keys) {
		get_type = (*env)->GetMethodID(env, class, "getValueTypeForKey", "(Ljava/lang/String;)I");
		get_int = (*env)->GetMethodID(env, class, "getInteger", "(Ljava/lang/String;)I");
		get_long = (*env)->GetMethodID(env, class, "getLong", "(Ljava/lang/String;)J");
		get_float = (*env)->GetMethodID(env, class, "getFloat", "(Ljava/lang/String;)F");
		get_string = (*env)->GetMethodID(env, class, "getString", "(Ljava/lang/String;)Ljava/lang/String;");
		get_buffer = (*env)->GetMethodID(env, class, "getByteBuffer", "(Ljava/lang/String;)Ljava/nio/ByteBuffer;");
		to_array = (*env)->GetMethodID(env, ndk_jni_class(env, "java/util/Set"), "toArray", "()[Ljava/lang/Object;");
		b_position = (*env)->GetMethodID(env, ndk_jni_class(env, "java/nio/Buffer"), "position", "()I");
		b_limit = (*env)->GetMethodID(env, ndk_jni_class(env, "java/nio/Buffer"), "limit", "()I");
		b_get = (*env)->GetMethodID(env, ndk_jni_class(env, "java/nio/ByteBuffer"), "get", "(I)B");
		get_keys = (*env)->GetMethodID(env, class, "getKeys", "()Ljava/util/Set;");
	}
	AMediaFormat *format = AMediaFormat_new();
	jobject keys = (*env)->CallObjectMethod(env, jformat, get_keys);
	jobjectArray names = (*env)->CallObjectMethod(env, keys, to_array);
	jsize n = (*env)->GetArrayLength(env, names);
	for (jsize i = 0; i < n; i++) {
		jstring jname = (*env)->GetObjectArrayElement(env, names, i);
		const char *name = (*env)->GetStringUTFChars(env, jname, NULL);
		switch ((*env)->CallIntMethod(env, jformat, get_type, jname)) {
			case 1: /* MediaFormat.TYPE_INTEGER */
				AMediaFormat_setInt32(format, name, (*env)->CallIntMethod(env, jformat, get_int, jname));
				break;
			case 2: /* TYPE_LONG */
				AMediaFormat_setInt64(format, name, (*env)->CallLongMethod(env, jformat, get_long, jname));
				break;
			case 3: /* TYPE_FLOAT */
				AMediaFormat_setFloat(format, name, (*env)->CallFloatMethod(env, jformat, get_float, jname));
				break;
			case 4: { /* TYPE_STRING */
				jstring value = (*env)->CallObjectMethod(env, jformat, get_string, jname);
				const char *s = (*env)->GetStringUTFChars(env, value, NULL);
				AMediaFormat_setString(format, name, s);
				(*env)->ReleaseStringUTFChars(env, value, s);
				(*env)->DeleteLocalRef(env, value);
				break;
			}
			case 5: { /* TYPE_BYTE_BUFFER */
				jobject buffer = (*env)->CallObjectMethod(env, jformat, get_buffer, jname);
				jint position = (*env)->CallIntMethod(env, buffer, b_position);
				jint limit = (*env)->CallIntMethod(env, buffer, b_limit);
				uint8_t *data = (*env)->GetDirectBufferAddress(env, buffer);
				if (data) {
					AMediaFormat_setBuffer(format, name, data + position, limit - position);
				} else { /* a heap buffer */
					uint8_t *copy = g_malloc(limit - position);
					for (jint j = position; j < limit; j++)
						copy[j - position] = (*env)->CallByteMethod(env, buffer, b_get, j);
					AMediaFormat_setBuffer(format, name, copy, limit - position);
					g_free(copy);
				}
				(*env)->DeleteLocalRef(env, buffer);
				break;
			}
			default:
				break;
		}
		(*env)->ReleaseStringUTFChars(env, jname, name);
		(*env)->DeleteLocalRef(env, jname);
		ndk_jni_threw(env, "AMediaFormat_fromJava");
	}
	(*env)->DeleteLocalRef(env, names);
	(*env)->DeleteLocalRef(env, keys);
	return format;
}
