#include <glib.h>
#include <stdint.h>

#include "../defines.h"

#include "../generated_headers/android_atl_NativeDigest.h"

JNIEXPORT jlong JNICALL Java_android_atl_NativeDigest_native_1new(JNIEnv *env, jclass clazz, jint type)
{
	return _INTPTR(g_checksum_new(type));
}

JNIEXPORT void JNICALL Java_android_atl_NativeDigest_native_1update(JNIEnv *env, jclass clazz, jlong checksum, jbyteArray input, jint offset, jint len)
{
	/* MessageDigest.update() has checked offset and len; g_checksum_update() does not call back into Java */
	jbyte *data = (*env)->GetPrimitiveArrayCritical(env, input, NULL);
	g_checksum_update(_PTR(checksum), (const guchar *)data + offset, len);
	(*env)->ReleasePrimitiveArrayCritical(env, input, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_android_atl_NativeDigest_native_1digest(JNIEnv *env, jclass clazz, jlong checksum, jbyteArray digest)
{
	guint8 buffer[64]; // SHA-512 is the longest
	gsize len = sizeof(buffer);
	g_checksum_get_digest(_PTR(checksum), buffer, &len);
	(*env)->SetByteArrayRegion(env, digest, 0, len, (jbyte *)buffer);
	/* after g_checksum_get_digest() the checksum is closed; MessageDigest starts over after digest() */
	g_checksum_reset(_PTR(checksum));
}

JNIEXPORT void JNICALL Java_android_atl_NativeDigest_native_1reset(JNIEnv *env, jclass clazz, jlong checksum)
{
	g_checksum_reset(_PTR(checksum));
}

JNIEXPORT jlong JNICALL Java_android_atl_NativeDigest_native_1copy(JNIEnv *env, jclass clazz, jlong checksum)
{
	return _INTPTR(g_checksum_copy(_PTR(checksum)));
}

JNIEXPORT void JNICALL Java_android_atl_NativeDigest_native_1free(JNIEnv *env, jclass clazz, jlong checksum)
{
	if (checksum)
		g_checksum_free(_PTR(checksum));
}
