/* JNI of android.opengl.GLES11. Generated from AOSP's GLES11.java (its "// C function" comments and native
 * declarations, Android 16) and the Khronos GLES 1.x headers, the functions with other return types written by hand.
 * AOSP's own JNI (frameworks/base/core/jni/android_opengl_GLES11.cpp) is generated as well and not in the local sources;
 * the argument checks follow its IllegalArgumentException messages for null arrays/buffers and negative offsets. */
#include "defines.h"
#include "util.h"
#include "android_opengl_GLES1.h"

#include "generated_headers/android_opengl_GLES11.h"

/* void glBindBuffer ( GLenum target, GLuint buffer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glBindBuffer__II(JNIEnv *env, jclass clazz, jint target, jint buffer)
{
	glBindBuffer((GLenum)target, (GLuint)buffer);
}

/* void glBufferData ( GLenum target, GLsizeiptr size, const GLvoid *data, GLenum usage ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glBufferData__IILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint target, jint size, jobject data_ref, jint usage)
{
	jarray data_array = NULL;
	jbyte *data_elems = NULL;
	void *data = NULL;
	data = get_nio_buffer(env, data_ref, &data_array, &data_elems);
	glBufferData((GLenum)target, (GLsizeiptr)size, (const void *)data, (GLenum)usage);
	release_nio_buffer(env, data_array, data_elems);
}

/* void glBufferSubData ( GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glBufferSubData__IIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint target, jint offset, jint size, jobject data_ref)
{
	jarray data_array = NULL;
	jbyte *data_elems = NULL;
	void *data = NULL;
	data = get_nio_buffer(env, data_ref, &data_array, &data_elems);
	glBufferSubData((GLenum)target, (GLintptr)offset, (GLsizeiptr)size, (const void *)data);
	release_nio_buffer(env, data_array, data_elems);
}

/* void glClipPlanef ( GLenum plane, const GLfloat *equation ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glClipPlanef__I_3FI(JNIEnv *env, jclass clazz, jint plane, jfloatArray equation_ref, jint offset)
{
	jfloat *equation = NULL;
	if (!equation_ref) {
		throw_iae(env, "equation == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, equation_ref) - offset < 4) {
		throw_iae(env, "length - offset < 4 < needed");
		goto exit;
	}
	equation = (*env)->GetFloatArrayElements(env, equation_ref, NULL);
	glClipPlanef((GLenum)plane, (const GLfloat *)(equation + offset));
exit:
	if (equation)
		(*env)->ReleaseFloatArrayElements(env, equation_ref, equation, JNI_ABORT);
}

/* void glClipPlanef ( GLenum plane, const GLfloat *equation ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glClipPlanef__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint plane, jobject equation_ref)
{
	jarray equation_array = NULL;
	jbyte *equation_elems = NULL;
	void *equation = NULL;
	equation = get_nio_buffer(env, equation_ref, &equation_array, &equation_elems);
	glClipPlanef((GLenum)plane, (const GLfloat *)equation);
	release_nio_buffer(env, equation_array, equation_elems);
}

/* void glClipPlanex ( GLenum plane, const GLfixed *equation ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glClipPlanex__I_3II(JNIEnv *env, jclass clazz, jint plane, jintArray equation_ref, jint offset)
{
	jint *equation = NULL;
	if (!equation_ref) {
		throw_iae(env, "equation == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, equation_ref) - offset < 4) {
		throw_iae(env, "length - offset < 4 < needed");
		goto exit;
	}
	equation = (*env)->GetIntArrayElements(env, equation_ref, NULL);
	glClipPlanex((GLenum)plane, (const GLfixed *)(equation + offset));
exit:
	if (equation)
		(*env)->ReleaseIntArrayElements(env, equation_ref, equation, JNI_ABORT);
}

/* void glClipPlanex ( GLenum plane, const GLfixed *equation ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glClipPlanex__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint plane, jobject equation_ref)
{
	jarray equation_array = NULL;
	jbyte *equation_elems = NULL;
	void *equation = NULL;
	equation = get_nio_buffer(env, equation_ref, &equation_array, &equation_elems);
	glClipPlanex((GLenum)plane, (const GLfixed *)equation);
	release_nio_buffer(env, equation_array, equation_elems);
}

/* void glColor4ub ( GLubyte red, GLubyte green, GLubyte blue, GLubyte alpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glColor4ub__BBBB(JNIEnv *env, jclass clazz, jbyte red, jbyte green, jbyte blue, jbyte alpha)
{
	glColor4ub((GLubyte)red, (GLubyte)green, (GLubyte)blue, (GLubyte)alpha);
}

/* void glColorPointer ( GLint size, GLenum type, GLsizei stride, GLint offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glColorPointer__IIII(JNIEnv *env, jclass clazz, jint size, jint type, jint stride, jint offset)
{
	glColorPointer((GLint)size, (GLenum)type, (GLsizei)stride, (const void *)(intptr_t)offset);
}

/* void glDeleteBuffers ( GLsizei n, const GLuint *buffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glDeleteBuffers__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray buffers_ref, jint offset)
{
	jint *buffers = NULL;
	if (!buffers_ref) {
		throw_iae(env, "buffers == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, buffers_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	buffers = (*env)->GetIntArrayElements(env, buffers_ref, NULL);
	glDeleteBuffers((GLsizei)n, (const GLuint *)(buffers + offset));
exit:
	if (buffers)
		(*env)->ReleaseIntArrayElements(env, buffers_ref, buffers, JNI_ABORT);
}

/* void glDeleteBuffers ( GLsizei n, const GLuint *buffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glDeleteBuffers__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject buffers_ref)
{
	jarray buffers_array = NULL;
	jbyte *buffers_elems = NULL;
	void *buffers = NULL;
	buffers = get_nio_buffer(env, buffers_ref, &buffers_array, &buffers_elems);
	glDeleteBuffers((GLsizei)n, (const GLuint *)buffers);
	release_nio_buffer(env, buffers_array, buffers_elems);
}

/* void glDrawElements ( GLenum mode, GLsizei count, GLenum type, GLint offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glDrawElements__IIII(JNIEnv *env, jclass clazz, jint mode, jint count, jint type, jint offset)
{
	glDrawElements((GLenum)mode, (GLsizei)count, (GLenum)type, (const void *)(intptr_t)offset);
}

/* void glGenBuffers ( GLsizei n, GLuint *buffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGenBuffers__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray buffers_ref, jint offset)
{
	jint *buffers = NULL;
	if (!buffers_ref) {
		throw_iae(env, "buffers == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, buffers_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	buffers = (*env)->GetIntArrayElements(env, buffers_ref, NULL);
	glGenBuffers((GLsizei)n, (GLuint *)(buffers + offset));
exit:
	if (buffers)
		(*env)->ReleaseIntArrayElements(env, buffers_ref, buffers, 0);
}

/* void glGenBuffers ( GLsizei n, GLuint *buffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGenBuffers__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject buffers_ref)
{
	jarray buffers_array = NULL;
	jbyte *buffers_elems = NULL;
	void *buffers = NULL;
	if (!buffers_ref) {
		throw_iae(env, "buffers == null");
		goto exit;
	}
	buffers = get_nio_buffer(env, buffers_ref, &buffers_array, &buffers_elems);
	glGenBuffers((GLsizei)n, (GLuint *)buffers);
exit:
	release_nio_buffer(env, buffers_array, buffers_elems);
}

/* void glGetBooleanv ( GLenum pname, GLboolean *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetBooleanv__I_3ZI(JNIEnv *env, jclass clazz, jint pname, jbooleanArray params_ref, jint offset)
{
	jboolean *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetBooleanArrayElements(env, params_ref, NULL);
	glGetBooleanv((GLenum)pname, (GLboolean *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseBooleanArrayElements(env, params_ref, params, 0);
}

/* void glGetBooleanv ( GLenum pname, GLboolean *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetBooleanv__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetBooleanv((GLenum)pname, (GLboolean *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetBufferParameteriv ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetBufferParameteriv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
{
	jint *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glGetBufferParameteriv((GLenum)target, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetBufferParameteriv ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetBufferParameteriv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetBufferParameteriv((GLenum)target, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetClipPlanef ( GLenum pname, GLfloat *eqn ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetClipPlanef__I_3FI(JNIEnv *env, jclass clazz, jint pname, jfloatArray eqn_ref, jint offset)
{
	jfloat *eqn = NULL;
	if (!eqn_ref) {
		throw_iae(env, "eqn == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, eqn_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	eqn = (*env)->GetFloatArrayElements(env, eqn_ref, NULL);
	glGetClipPlanef((GLenum)pname, (GLfloat *)(eqn + offset));
exit:
	if (eqn)
		(*env)->ReleaseFloatArrayElements(env, eqn_ref, eqn, 0);
}

/* void glGetClipPlanef ( GLenum pname, GLfloat *eqn ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetClipPlanef__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject eqn_ref)
{
	jarray eqn_array = NULL;
	jbyte *eqn_elems = NULL;
	void *eqn = NULL;
	if (!eqn_ref) {
		throw_iae(env, "eqn == null");
		goto exit;
	}
	eqn = get_nio_buffer(env, eqn_ref, &eqn_array, &eqn_elems);
	glGetClipPlanef((GLenum)pname, (GLfloat *)eqn);
exit:
	release_nio_buffer(env, eqn_array, eqn_elems);
}

/* void glGetClipPlanex ( GLenum pname, GLfixed *eqn ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetClipPlanex__I_3II(JNIEnv *env, jclass clazz, jint pname, jintArray eqn_ref, jint offset)
{
	jint *eqn = NULL;
	if (!eqn_ref) {
		throw_iae(env, "eqn == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, eqn_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	eqn = (*env)->GetIntArrayElements(env, eqn_ref, NULL);
	glGetClipPlanex((GLenum)pname, (GLfixed *)(eqn + offset));
exit:
	if (eqn)
		(*env)->ReleaseIntArrayElements(env, eqn_ref, eqn, 0);
}

/* void glGetClipPlanex ( GLenum pname, GLfixed *eqn ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetClipPlanex__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject eqn_ref)
{
	jarray eqn_array = NULL;
	jbyte *eqn_elems = NULL;
	void *eqn = NULL;
	if (!eqn_ref) {
		throw_iae(env, "eqn == null");
		goto exit;
	}
	eqn = get_nio_buffer(env, eqn_ref, &eqn_array, &eqn_elems);
	glGetClipPlanex((GLenum)pname, (GLfixed *)eqn);
exit:
	release_nio_buffer(env, eqn_array, eqn_elems);
}

/* void glGetFixedv ( GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetFixedv__I_3II(JNIEnv *env, jclass clazz, jint pname, jintArray params_ref, jint offset)
{
	jint *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glGetFixedv((GLenum)pname, (GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetFixedv ( GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetFixedv__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetFixedv((GLenum)pname, (GLfixed *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetFloatv ( GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetFloatv__I_3FI(JNIEnv *env, jclass clazz, jint pname, jfloatArray params_ref, jint offset)
{
	jfloat *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetFloatArrayElements(env, params_ref, NULL);
	glGetFloatv((GLenum)pname, (GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, 0);
}

/* void glGetFloatv ( GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetFloatv__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetFloatv((GLenum)pname, (GLfloat *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetLightfv ( GLenum light, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetLightfv__II_3FI(JNIEnv *env, jclass clazz, jint light, jint pname, jfloatArray params_ref, jint offset)
{
	jfloat *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetFloatArrayElements(env, params_ref, NULL);
	glGetLightfv((GLenum)light, (GLenum)pname, (GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, 0);
}

/* void glGetLightfv ( GLenum light, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetLightfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint light, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetLightfv((GLenum)light, (GLenum)pname, (GLfloat *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetLightxv ( GLenum light, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetLightxv__II_3II(JNIEnv *env, jclass clazz, jint light, jint pname, jintArray params_ref, jint offset)
{
	jint *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glGetLightxv((GLenum)light, (GLenum)pname, (GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetLightxv ( GLenum light, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetLightxv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint light, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetLightxv((GLenum)light, (GLenum)pname, (GLfixed *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetMaterialfv ( GLenum face, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetMaterialfv__II_3FI(JNIEnv *env, jclass clazz, jint face, jint pname, jfloatArray params_ref, jint offset)
{
	jfloat *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetFloatArrayElements(env, params_ref, NULL);
	glGetMaterialfv((GLenum)face, (GLenum)pname, (GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, 0);
}

/* void glGetMaterialfv ( GLenum face, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetMaterialfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint face, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetMaterialfv((GLenum)face, (GLenum)pname, (GLfloat *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetMaterialxv ( GLenum face, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetMaterialxv__II_3II(JNIEnv *env, jclass clazz, jint face, jint pname, jintArray params_ref, jint offset)
{
	jint *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glGetMaterialxv((GLenum)face, (GLenum)pname, (GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetMaterialxv ( GLenum face, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetMaterialxv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint face, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetMaterialxv((GLenum)face, (GLenum)pname, (GLfixed *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexEnvfv ( GLenum env, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetTexEnvfv__II_3FI(JNIEnv *env, jclass clazz, jint env_enum, jint pname, jfloatArray params_ref, jint offset)
{
	jfloat *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetFloatArrayElements(env, params_ref, NULL);
	glGetTexEnvfv((GLenum)env_enum, (GLenum)pname, (GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, 0);
}

/* void glGetTexEnvfv ( GLenum env, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetTexEnvfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint env_enum, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexEnvfv((GLenum)env_enum, (GLenum)pname, (GLfloat *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexEnviv ( GLenum env, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetTexEnviv__II_3II(JNIEnv *env, jclass clazz, jint env_enum, jint pname, jintArray params_ref, jint offset)
{
	jint *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glGetTexEnviv((GLenum)env_enum, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexEnviv ( GLenum env, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetTexEnviv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint env_enum, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexEnviv((GLenum)env_enum, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexEnvxv ( GLenum env, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetTexEnvxv__II_3II(JNIEnv *env, jclass clazz, jint env_enum, jint pname, jintArray params_ref, jint offset)
{
	jint *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glGetTexEnvxv((GLenum)env_enum, (GLenum)pname, (GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexEnvxv ( GLenum env, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetTexEnvxv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint env_enum, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexEnvxv((GLenum)env_enum, (GLenum)pname, (GLfixed *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexParameterfv ( GLenum target, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetTexParameterfv__II_3FI(JNIEnv *env, jclass clazz, jint target, jint pname, jfloatArray params_ref, jint offset)
{
	jfloat *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetFloatArrayElements(env, params_ref, NULL);
	glGetTexParameterfv((GLenum)target, (GLenum)pname, (GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, 0);
}

/* void glGetTexParameterfv ( GLenum target, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetTexParameterfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexParameterfv((GLenum)target, (GLenum)pname, (GLfloat *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexParameteriv ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetTexParameteriv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
{
	jint *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glGetTexParameteriv((GLenum)target, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexParameteriv ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetTexParameteriv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexParameteriv((GLenum)target, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexParameterxv ( GLenum target, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetTexParameterxv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
{
	jint *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glGetTexParameterxv((GLenum)target, (GLenum)pname, (GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexParameterxv ( GLenum target, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glGetTexParameterxv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexParameterxv((GLenum)target, (GLenum)pname, (GLfixed *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* GLboolean glIsBuffer ( GLuint buffer ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES11_glIsBuffer__I(JNIEnv *env, jclass clazz, jint buffer)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsBuffer((GLuint)buffer);
	return _ret;
}

/* GLboolean glIsEnabled ( GLenum cap ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES11_glIsEnabled__I(JNIEnv *env, jclass clazz, jint cap)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsEnabled((GLenum)cap);
	return _ret;
}

/* GLboolean glIsTexture ( GLuint texture ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES11_glIsTexture__I(JNIEnv *env, jclass clazz, jint texture)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsTexture((GLuint)texture);
	return _ret;
}

/* void glNormalPointer ( GLenum type, GLsizei stride, GLint offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glNormalPointer__III(JNIEnv *env, jclass clazz, jint type, jint stride, jint offset)
{
	glNormalPointer((GLenum)type, (GLsizei)stride, (const void *)(intptr_t)offset);
}

/* void glPointParameterf ( GLenum pname, GLfloat param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glPointParameterf__IF(JNIEnv *env, jclass clazz, jint pname, jfloat param)
{
	glPointParameterf((GLenum)pname, (GLfloat)param);
}

/* void glPointParameterfv ( GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glPointParameterfv__I_3FI(JNIEnv *env, jclass clazz, jint pname, jfloatArray params_ref, jint offset)
{
	jfloat *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetFloatArrayElements(env, params_ref, NULL);
	glPointParameterfv((GLenum)pname, (const GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glPointParameterfv ( GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glPointParameterfv__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glPointParameterfv((GLenum)pname, (const GLfloat *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glPointParameterx ( GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glPointParameterx__II(JNIEnv *env, jclass clazz, jint pname, jint param)
{
	glPointParameterx((GLenum)pname, (GLfixed)param);
}

/* void glPointParameterxv ( GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glPointParameterxv__I_3II(JNIEnv *env, jclass clazz, jint pname, jintArray params_ref, jint offset)
{
	jint *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glPointParameterxv((GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glPointParameterxv ( GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glPointParameterxv__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glPointParameterxv((GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glPointSizePointerOES ( GLenum type, GLsizei stride, const GLvoid *pointer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glPointSizePointerOESBounds__IILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint type, jint stride, jobject pointer_ref, jint remaining)
{
	jarray pointer_array = NULL;
	jbyte *pointer_elems = NULL;
	void *pointer = NULL;
	pointer = get_nio_buffer(env, pointer_ref, &pointer_array, &pointer_elems);
	glPointSizePointerOES((GLenum)type, (GLsizei)stride, (const void *)pointer);
	release_nio_buffer(env, pointer_array, pointer_elems);
}

/* void glTexCoordPointer ( GLint size, GLenum type, GLsizei stride, GLint offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glTexCoordPointer__IIII(JNIEnv *env, jclass clazz, jint size, jint type, jint stride, jint offset)
{
	glTexCoordPointer((GLint)size, (GLenum)type, (GLsizei)stride, (const void *)(intptr_t)offset);
}

/* void glTexEnvi ( GLenum target, GLenum pname, GLint param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glTexEnvi__III(JNIEnv *env, jclass clazz, jint target, jint pname, jint param)
{
	glTexEnvi((GLenum)target, (GLenum)pname, (GLint)param);
}

/* void glTexEnviv ( GLenum target, GLenum pname, const GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glTexEnviv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
{
	jint *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glTexEnviv((GLenum)target, (GLenum)pname, (const GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexEnviv ( GLenum target, GLenum pname, const GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glTexEnviv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexEnviv((GLenum)target, (GLenum)pname, (const GLint *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexParameterfv ( GLenum target, GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glTexParameterfv__II_3FI(JNIEnv *env, jclass clazz, jint target, jint pname, jfloatArray params_ref, jint offset)
{
	jfloat *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetFloatArrayElements(env, params_ref, NULL);
	glTexParameterfv((GLenum)target, (GLenum)pname, (const GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexParameterfv ( GLenum target, GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glTexParameterfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexParameterfv((GLenum)target, (GLenum)pname, (const GLfloat *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexParameteri ( GLenum target, GLenum pname, GLint param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glTexParameteri__III(JNIEnv *env, jclass clazz, jint target, jint pname, jint param)
{
	glTexParameteri((GLenum)target, (GLenum)pname, (GLint)param);
}

/* void glTexParameteriv ( GLenum target, GLenum pname, const GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glTexParameteriv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
{
	jint *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glTexParameteriv((GLenum)target, (GLenum)pname, (const GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexParameteriv ( GLenum target, GLenum pname, const GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glTexParameteriv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexParameteriv((GLenum)target, (GLenum)pname, (const GLint *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexParameterxv ( GLenum target, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glTexParameterxv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
{
	jint *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glTexParameterxv((GLenum)target, (GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexParameterxv ( GLenum target, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glTexParameterxv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexParameterxv((GLenum)target, (GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glVertexPointer ( GLint size, GLenum type, GLsizei stride, GLint offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11_glVertexPointer__IIII(JNIEnv *env, jclass clazz, jint size, jint type, jint stride, jint offset)
{
	glVertexPointer((GLint)size, (GLenum)type, (GLsizei)stride, (const void *)(intptr_t)offset);
}

