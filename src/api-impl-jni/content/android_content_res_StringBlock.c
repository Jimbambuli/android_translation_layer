#include <androidfw/androidfw_c_api.h>

#include "../generated_headers/android_content_res_StringBlock.h"

#include "../defines.h"

/* ported from AOSP's android_content_StringBlock.cpp */

JNIEXPORT jlong JNICALL Java_android_content_res_StringBlock_nativeCreate(JNIEnv *env, jclass clazz, jbyteArray data, jint offset, jint length)
{
	if (data == NULL) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/NullPointerException"), NULL);
		return 0;
	}
	jsize data_length = (*env)->GetArrayLength(env, data);
	if (offset < 0 || offset >= data_length || length < 0 || length > data_length || (offset + length) > data_length) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IndexOutOfBoundsException"), NULL);
		return 0;
	}
	jbyte *bytes = (*env)->GetByteArrayElements(env, data, NULL);
	struct ResStringPool *string_pool = ResStringPool_new(bytes + offset, length, true);
	(*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
	if (string_pool == NULL || ResStringPool_getErrors(string_pool) != 0) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"), NULL);
		if (string_pool)
			ResStringPool_delete(string_pool);
		return 0;
	}
	return _INTPTR(string_pool);
}

JNIEXPORT jint JNICALL Java_android_content_res_StringBlock_nativeGetSize(JNIEnv *env, jclass clazz, jlong token)
{
	const struct ResStringPool *string_pool = _PTR(token);
	if (string_pool == NULL) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/NullPointerException"), NULL);
		return 0;
	}
	return ResStringPool_getSize(string_pool);
}

JNIEXPORT jstring JNICALL Java_android_content_res_StringBlock_nativeGetString(JNIEnv *env, jclass clazz, jlong token, jint idx)
{
	const struct ResStringPool *string_pool = _PTR(token);
	if (string_pool == NULL) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/NullPointerException"), NULL);
		return NULL;
	}
	size_t len;
	const char16_t *string = ResStringPool_stringAt(string_pool, idx, &len);
	if (string == NULL) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IndexOutOfBoundsException"), NULL);
		return NULL;
	}
	return (*env)->NewString(env, string, len);
}

/* the spans of the string as a flat array of <name, firstChar, lastChar>, null if it has none */
JNIEXPORT jintArray JNICALL Java_android_content_res_StringBlock_nativeGetStyle(JNIEnv *env, jclass clazz, jlong token, jint idx)
{
	const struct ResStringPool *string_pool = _PTR(token);
	if (string_pool == NULL) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/NullPointerException"), NULL);
		return NULL;
	}
	const struct ResStringPool_span *spans = ResStringPool_styleAt(string_pool, idx);
	if (spans == NULL)
		return NULL;
	int num = 0;
	while (spans[num].name.index != ResStringPool_span_END)
		num++;
	if (num == 0)
		return NULL;
	const int num_ints = sizeof(struct ResStringPool_span) / sizeof(jint);
	jintArray array = (*env)->NewIntArray(env, num * num_ints);
	if (array == NULL) // NewIntArray already threw OutOfMemoryError
		return NULL;
	(*env)->SetIntArrayRegion(env, array, 0, num * num_ints, (const jint *)spans);
	return array;
}

JNIEXPORT void JNICALL Java_android_content_res_StringBlock_nativeDestroy(JNIEnv *env, jclass clazz, jlong token)
{
	const struct ResStringPool *string_pool = _PTR(token);
	if (string_pool == NULL) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/NullPointerException"), NULL);
		return;
	}
	ResStringPool_delete(string_pool);
}
