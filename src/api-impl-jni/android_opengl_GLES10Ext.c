/* JNI of android.opengl.GLES10Ext. Generated from AOSP's GLES10Ext.java (its "// C function" comments and native
 * declarations, Android 16) and the Khronos GLES 1.x headers, the functions with other return types written by hand.
 * AOSP's own JNI (frameworks/base/core/jni/android_opengl_GLES10Ext.cpp) is generated as well and not in the local sources;
 * the argument checks follow its IllegalArgumentException messages for null arrays/buffers and negative offsets. */
#include "defines.h"
#include "util.h"
#include "android_opengl_GLES1.h"

#include "generated_headers/android_opengl_GLES10Ext.h"

/* GLbitfield glQueryMatrixxOES ( GLfixed *mantissa, GLint *exponent ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES10Ext_glQueryMatrixxOES___3II_3II(JNIEnv *env, jclass clazz, jintArray mantissa_ref, jint mantissaOffset, jintArray exponent_ref, jint exponentOffset)
{
	jint _ret = 0;
	jint *exponent = NULL;
	jint *mantissa = NULL;
	if (!mantissa_ref) {
		throw_iae(env, "mantissa == null");
		goto exit;
	}
	if (mantissaOffset < 0) {
		throw_iae(env, "mantissaOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, mantissa_ref) - mantissaOffset < 16) {
		throw_iae(env, "length - mantissaOffset < 16 < needed");
		goto exit;
	}
	mantissa = (*env)->GetIntArrayElements(env, mantissa_ref, NULL);
	if (!exponent_ref) {
		throw_iae(env, "exponent == null");
		goto exit;
	}
	if (exponentOffset < 0) {
		throw_iae(env, "exponentOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, exponent_ref) - exponentOffset < 16) {
		throw_iae(env, "length - exponentOffset < 16 < needed");
		goto exit;
	}
	exponent = (*env)->GetIntArrayElements(env, exponent_ref, NULL);
	_ret = (jint)glQueryMatrixxOES((GLfixed *)(mantissa + mantissaOffset), (GLint *)(exponent + exponentOffset));
exit:
	if (mantissa)
		(*env)->ReleaseIntArrayElements(env, mantissa_ref, mantissa, 0);
	if (exponent)
		(*env)->ReleaseIntArrayElements(env, exponent_ref, exponent, 0);
	return _ret;
}

/* GLbitfield glQueryMatrixxOES ( GLfixed *mantissa, GLint *exponent ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES10Ext_glQueryMatrixxOES__Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jobject mantissa_ref, jobject exponent_ref)
{
	jint _ret = 0;
	jarray exponent_array = NULL;
	jbyte *exponent_elems = NULL;
	void *exponent = NULL;
	jarray mantissa_array = NULL;
	jbyte *mantissa_elems = NULL;
	void *mantissa = NULL;
	if (!mantissa_ref) {
		throw_iae(env, "mantissa == null");
		goto exit;
	}
	mantissa = get_nio_buffer(env, mantissa_ref, &mantissa_array, &mantissa_elems);
	if (!exponent_ref) {
		throw_iae(env, "exponent == null");
		goto exit;
	}
	exponent = get_nio_buffer(env, exponent_ref, &exponent_array, &exponent_elems);
	_ret = (jint)glQueryMatrixxOES((GLfixed *)mantissa, (GLint *)exponent);
exit:
	release_nio_buffer(env, mantissa_array, mantissa_elems);
	release_nio_buffer(env, exponent_array, exponent_elems);
	return _ret;
}

