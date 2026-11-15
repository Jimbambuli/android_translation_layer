/* JNI of android.opengl.GLES30. Generated from AOSP's GLES30.java (its "// C function" comments and native
 * declarations, Android 16) and the Khronos GLES headers, the string and debug functions written by hand. AOSP's
 * own JNI (frameworks/base/core/jni/android_opengl_GLES30.cpp) is generated as well and not in the local sources;
 * the argument checks follow its IllegalArgumentException messages for null arrays/buffers and negative offsets. */
#include "defines.h"
#include "util.h"
#include "android_opengl_GLES3.h"

#include "generated_headers/android_opengl_GLES30.h"

/* void glReadBuffer ( GLenum mode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glReadBuffer__I(JNIEnv *env, jclass clazz, jint mode)
{
	glReadBuffer((GLenum)mode);
}

/* void glDrawRangeElements ( GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const GLvoid *indices ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDrawRangeElements__IIIIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint mode, jint start, jint end, jint count, jint type, jobject indices_ref)
{
	jarray indices_array = NULL;
	jbyte *indices_elems = NULL;
	void *indices = NULL;
	indices = get_nio_buffer(env, indices_ref, &indices_array, &indices_elems);
	glDrawRangeElements((GLenum)mode, (GLuint)start, (GLuint)end, (GLsizei)count, (GLenum)type, (const void *)indices);
	release_nio_buffer(env, indices_array, indices_elems);
}

/* void glDrawRangeElements ( GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, GLsizei offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDrawRangeElements__IIIIII(JNIEnv *env, jclass clazz, jint mode, jint start, jint end, jint count, jint type, jint offset)
{
	glDrawRangeElements((GLenum)mode, (GLuint)start, (GLuint)end, (GLsizei)count, (GLenum)type, (const void *)(intptr_t)offset);
}

/* void glTexImage3D ( GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLenum format, GLenum type, const GLvoid *pixels ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glTexImage3D__IIIIIIIIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint target, jint level, jint internalformat, jint width, jint height, jint depth, jint border, jint format, jint type, jobject pixels_ref)
{
	jarray pixels_array = NULL;
	jbyte *pixels_elems = NULL;
	void *pixels = NULL;
	pixels = get_nio_buffer(env, pixels_ref, &pixels_array, &pixels_elems);
	glTexImage3D((GLenum)target, (GLint)level, (GLint)internalformat, (GLsizei)width, (GLsizei)height, (GLsizei)depth, (GLint)border, (GLenum)format, (GLenum)type, (const void *)pixels);
	release_nio_buffer(env, pixels_array, pixels_elems);
}

/* void glTexImage3D ( GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLenum format, GLenum type, GLsizei offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glTexImage3D__IIIIIIIIII(JNIEnv *env, jclass clazz, jint target, jint level, jint internalformat, jint width, jint height, jint depth, jint border, jint format, jint type, jint offset)
{
	glTexImage3D((GLenum)target, (GLint)level, (GLint)internalformat, (GLsizei)width, (GLsizei)height, (GLsizei)depth, (GLint)border, (GLenum)format, (GLenum)type, (const void *)(intptr_t)offset);
}

/* void glTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const GLvoid *pixels ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glTexSubImage3D__IIIIIIIIIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint target, jint level, jint xoffset, jint yoffset, jint zoffset, jint width, jint height, jint depth, jint format, jint type, jobject pixels_ref)
{
	jarray pixels_array = NULL;
	jbyte *pixels_elems = NULL;
	void *pixels = NULL;
	pixels = get_nio_buffer(env, pixels_ref, &pixels_array, &pixels_elems);
	glTexSubImage3D((GLenum)target, (GLint)level, (GLint)xoffset, (GLint)yoffset, (GLint)zoffset, (GLsizei)width, (GLsizei)height, (GLsizei)depth, (GLenum)format, (GLenum)type, (const void *)pixels);
	release_nio_buffer(env, pixels_array, pixels_elems);
}

/* void glTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLsizei offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glTexSubImage3D__IIIIIIIIIII(JNIEnv *env, jclass clazz, jint target, jint level, jint xoffset, jint yoffset, jint zoffset, jint width, jint height, jint depth, jint format, jint type, jint offset)
{
	glTexSubImage3D((GLenum)target, (GLint)level, (GLint)xoffset, (GLint)yoffset, (GLint)zoffset, (GLsizei)width, (GLsizei)height, (GLsizei)depth, (GLenum)format, (GLenum)type, (const void *)(intptr_t)offset);
}

/* void glCopyTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLint x, GLint y, GLsizei width, GLsizei height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glCopyTexSubImage3D__IIIIIIIII(JNIEnv *env, jclass clazz, jint target, jint level, jint xoffset, jint yoffset, jint zoffset, jint x, jint y, jint width, jint height)
{
	glCopyTexSubImage3D((GLenum)target, (GLint)level, (GLint)xoffset, (GLint)yoffset, (GLint)zoffset, (GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height);
}

/* void glCompressedTexImage3D ( GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLsizei imageSize, const GLvoid *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glCompressedTexImage3D__IIIIIIIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint target, jint level, jint internalformat, jint width, jint height, jint depth, jint border, jint imageSize, jobject data_ref)
{
	jarray data_array = NULL;
	jbyte *data_elems = NULL;
	void *data = NULL;
	data = get_nio_buffer(env, data_ref, &data_array, &data_elems);
	glCompressedTexImage3D((GLenum)target, (GLint)level, (GLenum)internalformat, (GLsizei)width, (GLsizei)height, (GLsizei)depth, (GLint)border, (GLsizei)imageSize, (const void *)data);
	release_nio_buffer(env, data_array, data_elems);
}

/* void glCompressedTexImage3D ( GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLsizei imageSize, GLsizei offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glCompressedTexImage3D__IIIIIIIII(JNIEnv *env, jclass clazz, jint target, jint level, jint internalformat, jint width, jint height, jint depth, jint border, jint imageSize, jint offset)
{
	glCompressedTexImage3D((GLenum)target, (GLint)level, (GLenum)internalformat, (GLsizei)width, (GLsizei)height, (GLsizei)depth, (GLint)border, (GLsizei)imageSize, (const void *)(intptr_t)offset);
}

/* void glCompressedTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLsizei imageSize, const GLvoid *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glCompressedTexSubImage3D__IIIIIIIIIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint target, jint level, jint xoffset, jint yoffset, jint zoffset, jint width, jint height, jint depth, jint format, jint imageSize, jobject data_ref)
{
	jarray data_array = NULL;
	jbyte *data_elems = NULL;
	void *data = NULL;
	data = get_nio_buffer(env, data_ref, &data_array, &data_elems);
	glCompressedTexSubImage3D((GLenum)target, (GLint)level, (GLint)xoffset, (GLint)yoffset, (GLint)zoffset, (GLsizei)width, (GLsizei)height, (GLsizei)depth, (GLenum)format, (GLsizei)imageSize, (const void *)data);
	release_nio_buffer(env, data_array, data_elems);
}

/* void glCompressedTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLsizei imageSize, GLsizei offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glCompressedTexSubImage3D__IIIIIIIIIII(JNIEnv *env, jclass clazz, jint target, jint level, jint xoffset, jint yoffset, jint zoffset, jint width, jint height, jint depth, jint format, jint imageSize, jint offset)
{
	glCompressedTexSubImage3D((GLenum)target, (GLint)level, (GLint)xoffset, (GLint)yoffset, (GLint)zoffset, (GLsizei)width, (GLsizei)height, (GLsizei)depth, (GLenum)format, (GLsizei)imageSize, (const void *)(intptr_t)offset);
}

/* void glGenQueries ( GLsizei n, GLuint *ids ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGenQueries__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray ids_ref, jint offset)
{
	jint *ids = NULL;
	if (!ids_ref) {
		throw_iae(env, "ids == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, ids_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	ids = (*env)->GetIntArrayElements(env, ids_ref, NULL);
	glGenQueries((GLsizei)n, (GLuint *)(ids + offset));
exit:
	if (ids)
		(*env)->ReleaseIntArrayElements(env, ids_ref, ids, 0);
}

/* void glGenQueries ( GLsizei n, GLuint *ids ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGenQueries__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject ids_ref)
{
	jarray ids_array = NULL;
	jbyte *ids_elems = NULL;
	void *ids = NULL;
	if (!ids_ref) {
		throw_iae(env, "ids == null");
		goto exit;
	}
	ids = get_nio_buffer(env, ids_ref, &ids_array, &ids_elems);
	glGenQueries((GLsizei)n, (GLuint *)ids);
exit:
	release_nio_buffer(env, ids_array, ids_elems);
}

/* void glDeleteQueries ( GLsizei n, const GLuint *ids ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDeleteQueries__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray ids_ref, jint offset)
{
	jint *ids = NULL;
	if (!ids_ref) {
		throw_iae(env, "ids == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, ids_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	ids = (*env)->GetIntArrayElements(env, ids_ref, NULL);
	glDeleteQueries((GLsizei)n, (const GLuint *)(ids + offset));
exit:
	if (ids)
		(*env)->ReleaseIntArrayElements(env, ids_ref, ids, JNI_ABORT);
}

/* void glDeleteQueries ( GLsizei n, const GLuint *ids ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDeleteQueries__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject ids_ref)
{
	jarray ids_array = NULL;
	jbyte *ids_elems = NULL;
	void *ids = NULL;
	ids = get_nio_buffer(env, ids_ref, &ids_array, &ids_elems);
	glDeleteQueries((GLsizei)n, (const GLuint *)ids);
	release_nio_buffer(env, ids_array, ids_elems);
}

/* GLboolean glIsQuery ( GLuint id ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES30_glIsQuery__I(JNIEnv *env, jclass clazz, jint id)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsQuery((GLuint)id);
	return _ret;
}

/* void glBeginQuery ( GLenum target, GLuint id ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glBeginQuery__II(JNIEnv *env, jclass clazz, jint target, jint id)
{
	glBeginQuery((GLenum)target, (GLuint)id);
}

/* void glEndQuery ( GLenum target ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glEndQuery__I(JNIEnv *env, jclass clazz, jint target)
{
	glEndQuery((GLenum)target);
}

/* void glGetQueryiv ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetQueryiv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	glGetQueryiv((GLenum)target, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetQueryiv ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetQueryiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetQueryiv((GLenum)target, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetQueryObjectuiv ( GLuint id, GLenum pname, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetQueryObjectuiv__II_3II(JNIEnv *env, jclass clazz, jint id, jint pname, jintArray params_ref, jint offset)
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
	glGetQueryObjectuiv((GLuint)id, (GLenum)pname, (GLuint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetQueryObjectuiv ( GLuint id, GLenum pname, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetQueryObjectuiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint id, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetQueryObjectuiv((GLuint)id, (GLenum)pname, (GLuint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* GLboolean glUnmapBuffer ( GLenum target ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES30_glUnmapBuffer__I(JNIEnv *env, jclass clazz, jint target)
{
	jboolean _ret = 0;
	_ret = (jboolean)glUnmapBuffer((GLenum)target);
	return _ret;
}

/* void glDrawBuffers ( GLsizei n, const GLenum *bufs ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDrawBuffers__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray bufs_ref, jint offset)
{
	jint *bufs = NULL;
	if (!bufs_ref) {
		throw_iae(env, "bufs == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, bufs_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	bufs = (*env)->GetIntArrayElements(env, bufs_ref, NULL);
	glDrawBuffers((GLsizei)n, (const GLenum *)(bufs + offset));
exit:
	if (bufs)
		(*env)->ReleaseIntArrayElements(env, bufs_ref, bufs, JNI_ABORT);
}

/* void glDrawBuffers ( GLsizei n, const GLenum *bufs ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDrawBuffers__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject bufs_ref)
{
	jarray bufs_array = NULL;
	jbyte *bufs_elems = NULL;
	void *bufs = NULL;
	bufs = get_nio_buffer(env, bufs_ref, &bufs_array, &bufs_elems);
	glDrawBuffers((GLsizei)n, (const GLenum *)bufs);
	release_nio_buffer(env, bufs_array, bufs_elems);
}

/* void glUniformMatrix2x3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformMatrix2x3fv__IIZ_3FI(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
{
	jfloat *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetFloatArrayElements(env, value_ref, NULL);
	glUniformMatrix2x3fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glUniformMatrix2x3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformMatrix2x3fv__IIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniformMatrix2x3fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glUniformMatrix3x2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformMatrix3x2fv__IIZ_3FI(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
{
	jfloat *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetFloatArrayElements(env, value_ref, NULL);
	glUniformMatrix3x2fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glUniformMatrix3x2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformMatrix3x2fv__IIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniformMatrix3x2fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glUniformMatrix2x4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformMatrix2x4fv__IIZ_3FI(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
{
	jfloat *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetFloatArrayElements(env, value_ref, NULL);
	glUniformMatrix2x4fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glUniformMatrix2x4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformMatrix2x4fv__IIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniformMatrix2x4fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glUniformMatrix4x2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformMatrix4x2fv__IIZ_3FI(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
{
	jfloat *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetFloatArrayElements(env, value_ref, NULL);
	glUniformMatrix4x2fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glUniformMatrix4x2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformMatrix4x2fv__IIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniformMatrix4x2fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glUniformMatrix3x4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformMatrix3x4fv__IIZ_3FI(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
{
	jfloat *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetFloatArrayElements(env, value_ref, NULL);
	glUniformMatrix3x4fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glUniformMatrix3x4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformMatrix3x4fv__IIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniformMatrix3x4fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glUniformMatrix4x3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformMatrix4x3fv__IIZ_3FI(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
{
	jfloat *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetFloatArrayElements(env, value_ref, NULL);
	glUniformMatrix4x3fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glUniformMatrix4x3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformMatrix4x3fv__IIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniformMatrix4x3fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glBlitFramebuffer ( GLint srcX0, GLint srcY0, GLint srcX1, GLint srcY1, GLint dstX0, GLint dstY0, GLint dstX1, GLint dstY1, GLbitfield mask, GLenum filter ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glBlitFramebuffer__IIIIIIIIII(JNIEnv *env, jclass clazz, jint srcX0, jint srcY0, jint srcX1, jint srcY1, jint dstX0, jint dstY0, jint dstX1, jint dstY1, jint mask, jint filter)
{
	glBlitFramebuffer((GLint)srcX0, (GLint)srcY0, (GLint)srcX1, (GLint)srcY1, (GLint)dstX0, (GLint)dstY0, (GLint)dstX1, (GLint)dstY1, (GLbitfield)mask, (GLenum)filter);
}

/* void glRenderbufferStorageMultisample ( GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glRenderbufferStorageMultisample__IIIII(JNIEnv *env, jclass clazz, jint target, jint samples, jint internalformat, jint width, jint height)
{
	glRenderbufferStorageMultisample((GLenum)target, (GLsizei)samples, (GLenum)internalformat, (GLsizei)width, (GLsizei)height);
}

/* void glFramebufferTextureLayer ( GLenum target, GLenum attachment, GLuint texture, GLint level, GLint layer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glFramebufferTextureLayer__IIIII(JNIEnv *env, jclass clazz, jint target, jint attachment, jint texture, jint level, jint layer)
{
	glFramebufferTextureLayer((GLenum)target, (GLenum)attachment, (GLuint)texture, (GLint)level, (GLint)layer);
}

/* void glFlushMappedBufferRange ( GLenum target, GLintptr offset, GLsizeiptr length ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glFlushMappedBufferRange__III(JNIEnv *env, jclass clazz, jint target, jint offset, jint length)
{
	glFlushMappedBufferRange((GLenum)target, (GLintptr)offset, (GLsizeiptr)length);
}

/* void glBindVertexArray ( GLuint array ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glBindVertexArray__I(JNIEnv *env, jclass clazz, jint array)
{
	glBindVertexArray((GLuint)array);
}

/* void glDeleteVertexArrays ( GLsizei n, const GLuint *arrays ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDeleteVertexArrays__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray arrays_ref, jint offset)
{
	jint *arrays = NULL;
	if (!arrays_ref) {
		throw_iae(env, "arrays == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, arrays_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	arrays = (*env)->GetIntArrayElements(env, arrays_ref, NULL);
	glDeleteVertexArrays((GLsizei)n, (const GLuint *)(arrays + offset));
exit:
	if (arrays)
		(*env)->ReleaseIntArrayElements(env, arrays_ref, arrays, JNI_ABORT);
}

/* void glDeleteVertexArrays ( GLsizei n, const GLuint *arrays ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDeleteVertexArrays__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject arrays_ref)
{
	jarray arrays_array = NULL;
	jbyte *arrays_elems = NULL;
	void *arrays = NULL;
	arrays = get_nio_buffer(env, arrays_ref, &arrays_array, &arrays_elems);
	glDeleteVertexArrays((GLsizei)n, (const GLuint *)arrays);
	release_nio_buffer(env, arrays_array, arrays_elems);
}

/* void glGenVertexArrays ( GLsizei n, GLuint *arrays ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGenVertexArrays__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray arrays_ref, jint offset)
{
	jint *arrays = NULL;
	if (!arrays_ref) {
		throw_iae(env, "arrays == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, arrays_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	arrays = (*env)->GetIntArrayElements(env, arrays_ref, NULL);
	glGenVertexArrays((GLsizei)n, (GLuint *)(arrays + offset));
exit:
	if (arrays)
		(*env)->ReleaseIntArrayElements(env, arrays_ref, arrays, 0);
}

/* void glGenVertexArrays ( GLsizei n, GLuint *arrays ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGenVertexArrays__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject arrays_ref)
{
	jarray arrays_array = NULL;
	jbyte *arrays_elems = NULL;
	void *arrays = NULL;
	if (!arrays_ref) {
		throw_iae(env, "arrays == null");
		goto exit;
	}
	arrays = get_nio_buffer(env, arrays_ref, &arrays_array, &arrays_elems);
	glGenVertexArrays((GLsizei)n, (GLuint *)arrays);
exit:
	release_nio_buffer(env, arrays_array, arrays_elems);
}

/* GLboolean glIsVertexArray ( GLuint array ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES30_glIsVertexArray__I(JNIEnv *env, jclass clazz, jint array)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsVertexArray((GLuint)array);
	return _ret;
}

/* void glGetIntegeri_v ( GLenum target, GLuint index, GLint *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetIntegeri_1v__II_3II(JNIEnv *env, jclass clazz, jint target, jint index, jintArray data_ref, jint offset)
{
	jint *data = NULL;
	if (!data_ref) {
		throw_iae(env, "data == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, data_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	data = (*env)->GetIntArrayElements(env, data_ref, NULL);
	glGetIntegeri_v((GLenum)target, (GLuint)index, (GLint *)(data + offset));
exit:
	if (data)
		(*env)->ReleaseIntArrayElements(env, data_ref, data, 0);
}

/* void glGetIntegeri_v ( GLenum target, GLuint index, GLint *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetIntegeri_1v__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint index, jobject data_ref)
{
	jarray data_array = NULL;
	jbyte *data_elems = NULL;
	void *data = NULL;
	if (!data_ref) {
		throw_iae(env, "data == null");
		goto exit;
	}
	data = get_nio_buffer(env, data_ref, &data_array, &data_elems);
	glGetIntegeri_v((GLenum)target, (GLuint)index, (GLint *)data);
exit:
	release_nio_buffer(env, data_array, data_elems);
}

/* void glBeginTransformFeedback ( GLenum primitiveMode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glBeginTransformFeedback__I(JNIEnv *env, jclass clazz, jint primitiveMode)
{
	glBeginTransformFeedback((GLenum)primitiveMode);
}

/* void glEndTransformFeedback ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glEndTransformFeedback__(JNIEnv *env, jclass clazz)
{
	glEndTransformFeedback();
}

/* void glBindBufferRange ( GLenum target, GLuint index, GLuint buffer, GLintptr offset, GLsizeiptr size ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glBindBufferRange__IIIII(JNIEnv *env, jclass clazz, jint target, jint index, jint buffer, jint offset, jint size)
{
	glBindBufferRange((GLenum)target, (GLuint)index, (GLuint)buffer, (GLintptr)offset, (GLsizeiptr)size);
}

/* void glBindBufferBase ( GLenum target, GLuint index, GLuint buffer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glBindBufferBase__III(JNIEnv *env, jclass clazz, jint target, jint index, jint buffer)
{
	glBindBufferBase((GLenum)target, (GLuint)index, (GLuint)buffer);
}

/* void glGetTransformFeedbackVarying ( GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size, GLenum *type, GLchar *name ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetTransformFeedbackVarying__III_3II_3II_3II_3BI(JNIEnv *env, jclass clazz, jint program, jint index, jint bufsize, jintArray length_ref, jint lengthOffset, jintArray size_ref, jint sizeOffset, jintArray type_ref, jint typeOffset, jbyteArray name_ref, jint nameOffset)
{
	jbyte *name = NULL;
	jint *type = NULL;
	jint *size = NULL;
	jint *length = NULL;
	if (!length_ref) {
		throw_iae(env, "length == null");
		goto exit;
	}
	if (lengthOffset < 0) {
		throw_iae(env, "lengthOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, length_ref) - lengthOffset < 1) {
		throw_iae(env, "length - lengthOffset < 1 < needed");
		goto exit;
	}
	length = (*env)->GetIntArrayElements(env, length_ref, NULL);
	if (!size_ref) {
		throw_iae(env, "size == null");
		goto exit;
	}
	if (sizeOffset < 0) {
		throw_iae(env, "sizeOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, size_ref) - sizeOffset < 1) {
		throw_iae(env, "length - sizeOffset < 1 < needed");
		goto exit;
	}
	size = (*env)->GetIntArrayElements(env, size_ref, NULL);
	if (!type_ref) {
		throw_iae(env, "type == null");
		goto exit;
	}
	if (typeOffset < 0) {
		throw_iae(env, "typeOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, type_ref) - typeOffset < 1) {
		throw_iae(env, "length - typeOffset < 1 < needed");
		goto exit;
	}
	type = (*env)->GetIntArrayElements(env, type_ref, NULL);
	if (!name_ref) {
		throw_iae(env, "name == null");
		goto exit;
	}
	if (nameOffset < 0) {
		throw_iae(env, "nameOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, name_ref) - nameOffset < 1) {
		throw_iae(env, "length - nameOffset < 1 < needed");
		goto exit;
	}
	name = (*env)->GetByteArrayElements(env, name_ref, NULL);
	glGetTransformFeedbackVarying((GLuint)program, (GLuint)index, (GLsizei)bufsize, (GLsizei *)(length + lengthOffset), (GLsizei *)(size + sizeOffset), (GLenum *)(type + typeOffset), (GLchar *)(name + nameOffset));
exit:
	if (length)
		(*env)->ReleaseIntArrayElements(env, length_ref, length, 0);
	if (size)
		(*env)->ReleaseIntArrayElements(env, size_ref, size, 0);
	if (type)
		(*env)->ReleaseIntArrayElements(env, type_ref, type, 0);
	if (name)
		(*env)->ReleaseByteArrayElements(env, name_ref, name, 0);
}

/* void glGetTransformFeedbackVarying ( GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size, GLenum *type, GLchar *name ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetTransformFeedbackVarying__IIILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_ByteBuffer_2(JNIEnv *env, jclass clazz, jint program, jint index, jint bufsize, jobject length_ref, jobject size_ref, jobject type_ref, jobject name_ref)
{
	jarray name_array = NULL;
	jbyte *name_elems = NULL;
	void *name = NULL;
	jarray type_array = NULL;
	jbyte *type_elems = NULL;
	void *type = NULL;
	jarray size_array = NULL;
	jbyte *size_elems = NULL;
	void *size = NULL;
	jarray length_array = NULL;
	jbyte *length_elems = NULL;
	void *length = NULL;
	if (!length_ref) {
		throw_iae(env, "length == null");
		goto exit;
	}
	length = get_nio_buffer(env, length_ref, &length_array, &length_elems);
	if (!size_ref) {
		throw_iae(env, "size == null");
		goto exit;
	}
	size = get_nio_buffer(env, size_ref, &size_array, &size_elems);
	if (!type_ref) {
		throw_iae(env, "type == null");
		goto exit;
	}
	type = get_nio_buffer(env, type_ref, &type_array, &type_elems);
	if (!name_ref) {
		throw_iae(env, "name == null");
		goto exit;
	}
	name = get_nio_buffer(env, name_ref, &name_array, &name_elems);
	glGetTransformFeedbackVarying((GLuint)program, (GLuint)index, (GLsizei)bufsize, (GLsizei *)length, (GLsizei *)size, (GLenum *)type, (GLchar *)name);
exit:
	release_nio_buffer(env, length_array, length_elems);
	release_nio_buffer(env, size_array, size_elems);
	release_nio_buffer(env, type_array, type_elems);
	release_nio_buffer(env, name_array, name_elems);
}

/* void glVertexAttribIPointer ( GLuint index, GLint size, GLenum type, GLsizei stride, const GLvoid *pointer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glVertexAttribIPointerBounds__IIIILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint index, jint size, jint type, jint stride, jobject pointer_ref, jint remaining)
{
	jarray pointer_array = NULL;
	jbyte *pointer_elems = NULL;
	void *pointer = NULL;
	pointer = get_nio_buffer(env, pointer_ref, &pointer_array, &pointer_elems);
	glVertexAttribIPointer((GLuint)index, (GLint)size, (GLenum)type, (GLsizei)stride, (const void *)pointer);
	release_nio_buffer(env, pointer_array, pointer_elems);
}

/* void glVertexAttribIPointer ( GLuint index, GLint size, GLenum type, GLsizei stride, GLsizei offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glVertexAttribIPointer__IIIII(JNIEnv *env, jclass clazz, jint index, jint size, jint type, jint stride, jint offset)
{
	glVertexAttribIPointer((GLuint)index, (GLint)size, (GLenum)type, (GLsizei)stride, (const void *)(intptr_t)offset);
}

/* void glGetVertexAttribIiv ( GLuint index, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetVertexAttribIiv__II_3II(JNIEnv *env, jclass clazz, jint index, jint pname, jintArray params_ref, jint offset)
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
	glGetVertexAttribIiv((GLuint)index, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetVertexAttribIiv ( GLuint index, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetVertexAttribIiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint index, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetVertexAttribIiv((GLuint)index, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetVertexAttribIuiv ( GLuint index, GLenum pname, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetVertexAttribIuiv__II_3II(JNIEnv *env, jclass clazz, jint index, jint pname, jintArray params_ref, jint offset)
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
	glGetVertexAttribIuiv((GLuint)index, (GLenum)pname, (GLuint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetVertexAttribIuiv ( GLuint index, GLenum pname, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetVertexAttribIuiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint index, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetVertexAttribIuiv((GLuint)index, (GLenum)pname, (GLuint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glVertexAttribI4i ( GLuint index, GLint x, GLint y, GLint z, GLint w ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glVertexAttribI4i__IIIII(JNIEnv *env, jclass clazz, jint index, jint x, jint y, jint z, jint w)
{
	glVertexAttribI4i((GLuint)index, (GLint)x, (GLint)y, (GLint)z, (GLint)w);
}

/* void glVertexAttribI4ui ( GLuint index, GLuint x, GLuint y, GLuint z, GLuint w ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glVertexAttribI4ui__IIIII(JNIEnv *env, jclass clazz, jint index, jint x, jint y, jint z, jint w)
{
	glVertexAttribI4ui((GLuint)index, (GLuint)x, (GLuint)y, (GLuint)z, (GLuint)w);
}

/* void glVertexAttribI4iv ( GLuint index, const GLint *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glVertexAttribI4iv__I_3II(JNIEnv *env, jclass clazz, jint index, jintArray v_ref, jint offset)
{
	jint *v = NULL;
	if (!v_ref) {
		throw_iae(env, "v == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, v_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	v = (*env)->GetIntArrayElements(env, v_ref, NULL);
	glVertexAttribI4iv((GLuint)index, (const GLint *)(v + offset));
exit:
	if (v)
		(*env)->ReleaseIntArrayElements(env, v_ref, v, JNI_ABORT);
}

/* void glVertexAttribI4iv ( GLuint index, const GLint *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glVertexAttribI4iv__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint index, jobject v_ref)
{
	jarray v_array = NULL;
	jbyte *v_elems = NULL;
	void *v = NULL;
	v = get_nio_buffer(env, v_ref, &v_array, &v_elems);
	glVertexAttribI4iv((GLuint)index, (const GLint *)v);
	release_nio_buffer(env, v_array, v_elems);
}

/* void glVertexAttribI4uiv ( GLuint index, const GLuint *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glVertexAttribI4uiv__I_3II(JNIEnv *env, jclass clazz, jint index, jintArray v_ref, jint offset)
{
	jint *v = NULL;
	if (!v_ref) {
		throw_iae(env, "v == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, v_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	v = (*env)->GetIntArrayElements(env, v_ref, NULL);
	glVertexAttribI4uiv((GLuint)index, (const GLuint *)(v + offset));
exit:
	if (v)
		(*env)->ReleaseIntArrayElements(env, v_ref, v, JNI_ABORT);
}

/* void glVertexAttribI4uiv ( GLuint index, const GLuint *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glVertexAttribI4uiv__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint index, jobject v_ref)
{
	jarray v_array = NULL;
	jbyte *v_elems = NULL;
	void *v = NULL;
	v = get_nio_buffer(env, v_ref, &v_array, &v_elems);
	glVertexAttribI4uiv((GLuint)index, (const GLuint *)v);
	release_nio_buffer(env, v_array, v_elems);
}

/* void glGetUniformuiv ( GLuint program, GLint location, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetUniformuiv__II_3II(JNIEnv *env, jclass clazz, jint program, jint location, jintArray params_ref, jint offset)
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
	glGetUniformuiv((GLuint)program, (GLint)location, (GLuint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetUniformuiv ( GLuint program, GLint location, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetUniformuiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetUniformuiv((GLuint)program, (GLint)location, (GLuint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* GLint glGetFragDataLocation ( GLuint program, const GLchar *name ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES30_glGetFragDataLocation__ILjava_lang_String_2(JNIEnv *env, jclass clazz, jint program, jstring name_ref)
{
	jint _ret = 0;
	const char *name = NULL;
	if (!name_ref) {
		throw_iae(env, "name == null");
		goto exit;
	}
	name = (*env)->GetStringUTFChars(env, name_ref, NULL);
	_ret = (jint)glGetFragDataLocation((GLuint)program, (const GLchar *)name);
exit:
	if (name)
		(*env)->ReleaseStringUTFChars(env, name_ref, name);
	return _ret;
}

/* void glUniform1ui ( GLint location, GLuint v0 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniform1ui__II(JNIEnv *env, jclass clazz, jint location, jint v0)
{
	glUniform1ui((GLint)location, (GLuint)v0);
}

/* void glUniform2ui ( GLint location, GLuint v0, GLuint v1 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniform2ui__III(JNIEnv *env, jclass clazz, jint location, jint v0, jint v1)
{
	glUniform2ui((GLint)location, (GLuint)v0, (GLuint)v1);
}

/* void glUniform3ui ( GLint location, GLuint v0, GLuint v1, GLuint v2 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniform3ui__IIII(JNIEnv *env, jclass clazz, jint location, jint v0, jint v1, jint v2)
{
	glUniform3ui((GLint)location, (GLuint)v0, (GLuint)v1, (GLuint)v2);
}

/* void glUniform4ui ( GLint location, GLuint v0, GLuint v1, GLuint v2, GLuint v3 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniform4ui__IIIII(JNIEnv *env, jclass clazz, jint location, jint v0, jint v1, jint v2, jint v3)
{
	glUniform4ui((GLint)location, (GLuint)v0, (GLuint)v1, (GLuint)v2, (GLuint)v3);
}

/* void glUniform1uiv ( GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniform1uiv__II_3II(JNIEnv *env, jclass clazz, jint location, jint count, jintArray value_ref, jint offset)
{
	jint *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetIntArrayElements(env, value_ref, NULL);
	glUniform1uiv((GLint)location, (GLsizei)count, (const GLuint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glUniform1uiv ( GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniform1uiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniform1uiv((GLint)location, (GLsizei)count, (const GLuint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glUniform2uiv ( GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniform2uiv__II_3II(JNIEnv *env, jclass clazz, jint location, jint count, jintArray value_ref, jint offset)
{
	jint *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetIntArrayElements(env, value_ref, NULL);
	glUniform2uiv((GLint)location, (GLsizei)count, (const GLuint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glUniform2uiv ( GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniform2uiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniform2uiv((GLint)location, (GLsizei)count, (const GLuint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glUniform3uiv ( GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniform3uiv__II_3II(JNIEnv *env, jclass clazz, jint location, jint count, jintArray value_ref, jint offset)
{
	jint *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetIntArrayElements(env, value_ref, NULL);
	glUniform3uiv((GLint)location, (GLsizei)count, (const GLuint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glUniform3uiv ( GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniform3uiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniform3uiv((GLint)location, (GLsizei)count, (const GLuint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glUniform4uiv ( GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniform4uiv__II_3II(JNIEnv *env, jclass clazz, jint location, jint count, jintArray value_ref, jint offset)
{
	jint *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetIntArrayElements(env, value_ref, NULL);
	glUniform4uiv((GLint)location, (GLsizei)count, (const GLuint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glUniform4uiv ( GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniform4uiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniform4uiv((GLint)location, (GLsizei)count, (const GLuint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glClearBufferiv ( GLenum buffer, GLint drawbuffer, const GLint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glClearBufferiv__II_3II(JNIEnv *env, jclass clazz, jint buffer, jint drawbuffer, jintArray value_ref, jint offset)
{
	jint *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetIntArrayElements(env, value_ref, NULL);
	glClearBufferiv((GLenum)buffer, (GLint)drawbuffer, (const GLint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glClearBufferiv ( GLenum buffer, GLint drawbuffer, const GLint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glClearBufferiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint buffer, jint drawbuffer, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glClearBufferiv((GLenum)buffer, (GLint)drawbuffer, (const GLint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glClearBufferuiv ( GLenum buffer, GLint drawbuffer, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glClearBufferuiv__II_3II(JNIEnv *env, jclass clazz, jint buffer, jint drawbuffer, jintArray value_ref, jint offset)
{
	jint *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetIntArrayElements(env, value_ref, NULL);
	glClearBufferuiv((GLenum)buffer, (GLint)drawbuffer, (const GLuint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glClearBufferuiv ( GLenum buffer, GLint drawbuffer, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glClearBufferuiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint buffer, jint drawbuffer, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glClearBufferuiv((GLenum)buffer, (GLint)drawbuffer, (const GLuint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glClearBufferfv ( GLenum buffer, GLint drawbuffer, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glClearBufferfv__II_3FI(JNIEnv *env, jclass clazz, jint buffer, jint drawbuffer, jfloatArray value_ref, jint offset)
{
	jfloat *value = NULL;
	if (!value_ref) {
		throw_iae(env, "value == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, value_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	value = (*env)->GetFloatArrayElements(env, value_ref, NULL);
	glClearBufferfv((GLenum)buffer, (GLint)drawbuffer, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glClearBufferfv ( GLenum buffer, GLint drawbuffer, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glClearBufferfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint buffer, jint drawbuffer, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glClearBufferfv((GLenum)buffer, (GLint)drawbuffer, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glClearBufferfi ( GLenum buffer, GLint drawbuffer, GLfloat depth, GLint stencil ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glClearBufferfi__IIFI(JNIEnv *env, jclass clazz, jint buffer, jint drawbuffer, jfloat depth, jint stencil)
{
	glClearBufferfi((GLenum)buffer, (GLint)drawbuffer, (GLfloat)depth, (GLint)stencil);
}

/* void glCopyBufferSubData ( GLenum readTarget, GLenum writeTarget, GLintptr readOffset, GLintptr writeOffset, GLsizeiptr size ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glCopyBufferSubData__IIIII(JNIEnv *env, jclass clazz, jint readTarget, jint writeTarget, jint readOffset, jint writeOffset, jint size)
{
	glCopyBufferSubData((GLenum)readTarget, (GLenum)writeTarget, (GLintptr)readOffset, (GLintptr)writeOffset, (GLsizeiptr)size);
}

/* void glGetActiveUniformsiv ( GLuint program, GLsizei uniformCount, const GLuint *uniformIndices, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetActiveUniformsiv__II_3III_3II(JNIEnv *env, jclass clazz, jint program, jint uniformCount, jintArray uniformIndices_ref, jint uniformIndicesOffset, jint pname, jintArray params_ref, jint paramsOffset)
{
	jint *params = NULL;
	jint *uniformIndices = NULL;
	if (!uniformIndices_ref) {
		throw_iae(env, "uniformIndices == null");
		goto exit;
	}
	if (uniformIndicesOffset < 0) {
		throw_iae(env, "uniformIndicesOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, uniformIndices_ref) - uniformIndicesOffset < 1) {
		throw_iae(env, "length - uniformIndicesOffset < 1 < needed");
		goto exit;
	}
	uniformIndices = (*env)->GetIntArrayElements(env, uniformIndices_ref, NULL);
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	if (paramsOffset < 0) {
		throw_iae(env, "paramsOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, params_ref) - paramsOffset < 1) {
		throw_iae(env, "length - paramsOffset < 1 < needed");
		goto exit;
	}
	params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glGetActiveUniformsiv((GLuint)program, (GLsizei)uniformCount, (const GLuint *)(uniformIndices + uniformIndicesOffset), (GLenum)pname, (GLint *)(params + paramsOffset));
exit:
	if (uniformIndices)
		(*env)->ReleaseIntArrayElements(env, uniformIndices_ref, uniformIndices, JNI_ABORT);
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetActiveUniformsiv ( GLuint program, GLsizei uniformCount, const GLuint *uniformIndices, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetActiveUniformsiv__IILjava_nio_IntBuffer_2ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint uniformCount, jobject uniformIndices_ref, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	jarray uniformIndices_array = NULL;
	jbyte *uniformIndices_elems = NULL;
	void *uniformIndices = NULL;
	uniformIndices = get_nio_buffer(env, uniformIndices_ref, &uniformIndices_array, &uniformIndices_elems);
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetActiveUniformsiv((GLuint)program, (GLsizei)uniformCount, (const GLuint *)uniformIndices, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, uniformIndices_array, uniformIndices_elems);
	release_nio_buffer(env, params_array, params_elems);
}

/* GLuint glGetUniformBlockIndex ( GLuint program, const GLchar *uniformBlockName ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES30_glGetUniformBlockIndex__ILjava_lang_String_2(JNIEnv *env, jclass clazz, jint program, jstring uniformBlockName_ref)
{
	jint _ret = 0;
	const char *uniformBlockName = NULL;
	if (!uniformBlockName_ref) {
		throw_iae(env, "uniformBlockName == null");
		goto exit;
	}
	uniformBlockName = (*env)->GetStringUTFChars(env, uniformBlockName_ref, NULL);
	_ret = (jint)glGetUniformBlockIndex((GLuint)program, (const GLchar *)uniformBlockName);
exit:
	if (uniformBlockName)
		(*env)->ReleaseStringUTFChars(env, uniformBlockName_ref, uniformBlockName);
	return _ret;
}

/* void glGetActiveUniformBlockiv ( GLuint program, GLuint uniformBlockIndex, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetActiveUniformBlockiv__III_3II(JNIEnv *env, jclass clazz, jint program, jint uniformBlockIndex, jint pname, jintArray params_ref, jint offset)
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
	glGetActiveUniformBlockiv((GLuint)program, (GLuint)uniformBlockIndex, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetActiveUniformBlockiv ( GLuint program, GLuint uniformBlockIndex, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetActiveUniformBlockiv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint uniformBlockIndex, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetActiveUniformBlockiv((GLuint)program, (GLuint)uniformBlockIndex, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetActiveUniformBlockName ( GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei *length, GLchar *uniformBlockName ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetActiveUniformBlockName__III_3II_3BI(JNIEnv *env, jclass clazz, jint program, jint uniformBlockIndex, jint bufSize, jintArray length_ref, jint lengthOffset, jbyteArray uniformBlockName_ref, jint uniformBlockNameOffset)
{
	jbyte *uniformBlockName = NULL;
	jint *length = NULL;
	if (!length_ref) {
		throw_iae(env, "length == null");
		goto exit;
	}
	if (lengthOffset < 0) {
		throw_iae(env, "lengthOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, length_ref) - lengthOffset < 1) {
		throw_iae(env, "length - lengthOffset < 1 < needed");
		goto exit;
	}
	length = (*env)->GetIntArrayElements(env, length_ref, NULL);
	if (!uniformBlockName_ref) {
		throw_iae(env, "uniformBlockName == null");
		goto exit;
	}
	if (uniformBlockNameOffset < 0) {
		throw_iae(env, "uniformBlockNameOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, uniformBlockName_ref) - uniformBlockNameOffset < 1) {
		throw_iae(env, "length - uniformBlockNameOffset < 1 < needed");
		goto exit;
	}
	uniformBlockName = (*env)->GetByteArrayElements(env, uniformBlockName_ref, NULL);
	glGetActiveUniformBlockName((GLuint)program, (GLuint)uniformBlockIndex, (GLsizei)bufSize, (GLsizei *)(length + lengthOffset), (GLchar *)(uniformBlockName + uniformBlockNameOffset));
exit:
	if (length)
		(*env)->ReleaseIntArrayElements(env, length_ref, length, 0);
	if (uniformBlockName)
		(*env)->ReleaseByteArrayElements(env, uniformBlockName_ref, uniformBlockName, 0);
}

/* void glUniformBlockBinding ( GLuint program, GLuint uniformBlockIndex, GLuint uniformBlockBinding ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glUniformBlockBinding__III(JNIEnv *env, jclass clazz, jint program, jint uniformBlockIndex, jint uniformBlockBinding)
{
	glUniformBlockBinding((GLuint)program, (GLuint)uniformBlockIndex, (GLuint)uniformBlockBinding);
}

/* void glDrawArraysInstanced ( GLenum mode, GLint first, GLsizei count, GLsizei instanceCount ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDrawArraysInstanced__IIII(JNIEnv *env, jclass clazz, jint mode, jint first, jint count, jint instanceCount)
{
	glDrawArraysInstanced((GLenum)mode, (GLint)first, (GLsizei)count, (GLsizei)instanceCount);
}

/* void glDrawElementsInstanced ( GLenum mode, GLsizei count, GLenum type, const GLvoid *indices, GLsizei instanceCount ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDrawElementsInstanced__IIILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint mode, jint count, jint type, jobject indices_ref, jint instanceCount)
{
	jarray indices_array = NULL;
	jbyte *indices_elems = NULL;
	void *indices = NULL;
	indices = get_nio_buffer(env, indices_ref, &indices_array, &indices_elems);
	glDrawElementsInstanced((GLenum)mode, (GLsizei)count, (GLenum)type, (const void *)indices, (GLsizei)instanceCount);
	release_nio_buffer(env, indices_array, indices_elems);
}

/* void glDrawElementsInstanced ( GLenum mode, GLsizei count, GLenum type, const GLvoid *indices, GLsizei instanceCount ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDrawElementsInstanced__IIIII(JNIEnv *env, jclass clazz, jint mode, jint count, jint type, jint indicesOffset, jint instanceCount)
{
	glDrawElementsInstanced((GLenum)mode, (GLsizei)count, (GLenum)type, (const void *)(intptr_t)indicesOffset, (GLsizei)instanceCount);
}

/* GLsync glFenceSync ( GLenum condition, GLbitfield flags ) */
JNIEXPORT jlong JNICALL Java_android_opengl_GLES30_glFenceSync__II(JNIEnv *env, jclass clazz, jint condition, jint flags)
{
	jlong _ret = 0;
	_ret = (jlong)(intptr_t)glFenceSync((GLenum)condition, (GLbitfield)flags);
	return _ret;
}

/* GLboolean glIsSync ( GLsync sync ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES30_glIsSync__J(JNIEnv *env, jclass clazz, jlong sync)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsSync((GLsync)(intptr_t)sync);
	return _ret;
}

/* void glDeleteSync ( GLsync sync ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDeleteSync__J(JNIEnv *env, jclass clazz, jlong sync)
{
	glDeleteSync((GLsync)(intptr_t)sync);
}

/* GLenum glClientWaitSync ( GLsync sync, GLbitfield flags, GLuint64 timeout ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES30_glClientWaitSync__JIJ(JNIEnv *env, jclass clazz, jlong sync, jint flags, jlong timeout)
{
	jint _ret = 0;
	_ret = (jint)glClientWaitSync((GLsync)(intptr_t)sync, (GLbitfield)flags, (GLuint64)timeout);
	return _ret;
}

/* void glWaitSync ( GLsync sync, GLbitfield flags, GLuint64 timeout ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glWaitSync__JIJ(JNIEnv *env, jclass clazz, jlong sync, jint flags, jlong timeout)
{
	glWaitSync((GLsync)(intptr_t)sync, (GLbitfield)flags, (GLuint64)timeout);
}

/* void glGetInteger64v ( GLenum pname, GLint64 *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetInteger64v__I_3JI(JNIEnv *env, jclass clazz, jint pname, jlongArray params_ref, jint offset)
{
	jlong *params = NULL;
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
	params = (*env)->GetLongArrayElements(env, params_ref, NULL);
	glGetInteger64v((GLenum)pname, (GLint64 *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseLongArrayElements(env, params_ref, params, 0);
}

/* void glGetInteger64v ( GLenum pname, GLint64 *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetInteger64v__ILjava_nio_LongBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetInteger64v((GLenum)pname, (GLint64 *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetSynciv ( GLsync sync, GLenum pname, GLsizei bufSize, GLsizei *length, GLint *values ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetSynciv__JII_3II_3II(JNIEnv *env, jclass clazz, jlong sync, jint pname, jint bufSize, jintArray length_ref, jint lengthOffset, jintArray values_ref, jint valuesOffset)
{
	jint *values = NULL;
	jint *length = NULL;
	if (!length_ref) {
		throw_iae(env, "length == null");
		goto exit;
	}
	if (lengthOffset < 0) {
		throw_iae(env, "lengthOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, length_ref) - lengthOffset < 1) {
		throw_iae(env, "length - lengthOffset < 1 < needed");
		goto exit;
	}
	length = (*env)->GetIntArrayElements(env, length_ref, NULL);
	if (!values_ref) {
		throw_iae(env, "values == null");
		goto exit;
	}
	if (valuesOffset < 0) {
		throw_iae(env, "valuesOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, values_ref) - valuesOffset < 1) {
		throw_iae(env, "length - valuesOffset < 1 < needed");
		goto exit;
	}
	values = (*env)->GetIntArrayElements(env, values_ref, NULL);
	glGetSynciv((GLsync)(intptr_t)sync, (GLenum)pname, (GLsizei)bufSize, (GLsizei *)(length + lengthOffset), (GLint *)(values + valuesOffset));
exit:
	if (length)
		(*env)->ReleaseIntArrayElements(env, length_ref, length, 0);
	if (values)
		(*env)->ReleaseIntArrayElements(env, values_ref, values, 0);
}

/* void glGetSynciv ( GLsync sync, GLenum pname, GLsizei bufSize, GLsizei *length, GLint *values ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetSynciv__JIILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jlong sync, jint pname, jint bufSize, jobject length_ref, jobject values_ref)
{
	jarray values_array = NULL;
	jbyte *values_elems = NULL;
	void *values = NULL;
	jarray length_array = NULL;
	jbyte *length_elems = NULL;
	void *length = NULL;
	if (!length_ref) {
		throw_iae(env, "length == null");
		goto exit;
	}
	length = get_nio_buffer(env, length_ref, &length_array, &length_elems);
	if (!values_ref) {
		throw_iae(env, "values == null");
		goto exit;
	}
	values = get_nio_buffer(env, values_ref, &values_array, &values_elems);
	glGetSynciv((GLsync)(intptr_t)sync, (GLenum)pname, (GLsizei)bufSize, (GLsizei *)length, (GLint *)values);
exit:
	release_nio_buffer(env, length_array, length_elems);
	release_nio_buffer(env, values_array, values_elems);
}

/* void glGetInteger64i_v ( GLenum target, GLuint index, GLint64 *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetInteger64i_1v__II_3JI(JNIEnv *env, jclass clazz, jint target, jint index, jlongArray data_ref, jint offset)
{
	jlong *data = NULL;
	if (!data_ref) {
		throw_iae(env, "data == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, data_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	data = (*env)->GetLongArrayElements(env, data_ref, NULL);
	glGetInteger64i_v((GLenum)target, (GLuint)index, (GLint64 *)(data + offset));
exit:
	if (data)
		(*env)->ReleaseLongArrayElements(env, data_ref, data, 0);
}

/* void glGetInteger64i_v ( GLenum target, GLuint index, GLint64 *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetInteger64i_1v__IILjava_nio_LongBuffer_2(JNIEnv *env, jclass clazz, jint target, jint index, jobject data_ref)
{
	jarray data_array = NULL;
	jbyte *data_elems = NULL;
	void *data = NULL;
	if (!data_ref) {
		throw_iae(env, "data == null");
		goto exit;
	}
	data = get_nio_buffer(env, data_ref, &data_array, &data_elems);
	glGetInteger64i_v((GLenum)target, (GLuint)index, (GLint64 *)data);
exit:
	release_nio_buffer(env, data_array, data_elems);
}

/* void glGetBufferParameteri64v ( GLenum target, GLenum pname, GLint64 *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetBufferParameteri64v__II_3JI(JNIEnv *env, jclass clazz, jint target, jint pname, jlongArray params_ref, jint offset)
{
	jlong *params = NULL;
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
	params = (*env)->GetLongArrayElements(env, params_ref, NULL);
	glGetBufferParameteri64v((GLenum)target, (GLenum)pname, (GLint64 *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseLongArrayElements(env, params_ref, params, 0);
}

/* void glGetBufferParameteri64v ( GLenum target, GLenum pname, GLint64 *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetBufferParameteri64v__IILjava_nio_LongBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetBufferParameteri64v((GLenum)target, (GLenum)pname, (GLint64 *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGenSamplers ( GLsizei count, GLuint *samplers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGenSamplers__I_3II(JNIEnv *env, jclass clazz, jint count, jintArray samplers_ref, jint offset)
{
	jint *samplers = NULL;
	if (!samplers_ref) {
		throw_iae(env, "samplers == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, samplers_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	samplers = (*env)->GetIntArrayElements(env, samplers_ref, NULL);
	glGenSamplers((GLsizei)count, (GLuint *)(samplers + offset));
exit:
	if (samplers)
		(*env)->ReleaseIntArrayElements(env, samplers_ref, samplers, 0);
}

/* void glGenSamplers ( GLsizei count, GLuint *samplers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGenSamplers__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint count, jobject samplers_ref)
{
	jarray samplers_array = NULL;
	jbyte *samplers_elems = NULL;
	void *samplers = NULL;
	if (!samplers_ref) {
		throw_iae(env, "samplers == null");
		goto exit;
	}
	samplers = get_nio_buffer(env, samplers_ref, &samplers_array, &samplers_elems);
	glGenSamplers((GLsizei)count, (GLuint *)samplers);
exit:
	release_nio_buffer(env, samplers_array, samplers_elems);
}

/* void glDeleteSamplers ( GLsizei count, const GLuint *samplers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDeleteSamplers__I_3II(JNIEnv *env, jclass clazz, jint count, jintArray samplers_ref, jint offset)
{
	jint *samplers = NULL;
	if (!samplers_ref) {
		throw_iae(env, "samplers == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, samplers_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	samplers = (*env)->GetIntArrayElements(env, samplers_ref, NULL);
	glDeleteSamplers((GLsizei)count, (const GLuint *)(samplers + offset));
exit:
	if (samplers)
		(*env)->ReleaseIntArrayElements(env, samplers_ref, samplers, JNI_ABORT);
}

/* void glDeleteSamplers ( GLsizei count, const GLuint *samplers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDeleteSamplers__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint count, jobject samplers_ref)
{
	jarray samplers_array = NULL;
	jbyte *samplers_elems = NULL;
	void *samplers = NULL;
	samplers = get_nio_buffer(env, samplers_ref, &samplers_array, &samplers_elems);
	glDeleteSamplers((GLsizei)count, (const GLuint *)samplers);
	release_nio_buffer(env, samplers_array, samplers_elems);
}

/* GLboolean glIsSampler ( GLuint sampler ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES30_glIsSampler__I(JNIEnv *env, jclass clazz, jint sampler)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsSampler((GLuint)sampler);
	return _ret;
}

/* void glBindSampler ( GLuint unit, GLuint sampler ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glBindSampler__II(JNIEnv *env, jclass clazz, jint unit, jint sampler)
{
	glBindSampler((GLuint)unit, (GLuint)sampler);
}

/* void glSamplerParameteri ( GLuint sampler, GLenum pname, GLint param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glSamplerParameteri__III(JNIEnv *env, jclass clazz, jint sampler, jint pname, jint param)
{
	glSamplerParameteri((GLuint)sampler, (GLenum)pname, (GLint)param);
}

/* void glSamplerParameteriv ( GLuint sampler, GLenum pname, const GLint *param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glSamplerParameteriv__II_3II(JNIEnv *env, jclass clazz, jint sampler, jint pname, jintArray param_ref, jint offset)
{
	jint *param = NULL;
	if (!param_ref) {
		throw_iae(env, "param == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, param_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	param = (*env)->GetIntArrayElements(env, param_ref, NULL);
	glSamplerParameteriv((GLuint)sampler, (GLenum)pname, (const GLint *)(param + offset));
exit:
	if (param)
		(*env)->ReleaseIntArrayElements(env, param_ref, param, JNI_ABORT);
}

/* void glSamplerParameteriv ( GLuint sampler, GLenum pname, const GLint *param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glSamplerParameteriv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint sampler, jint pname, jobject param_ref)
{
	jarray param_array = NULL;
	jbyte *param_elems = NULL;
	void *param = NULL;
	param = get_nio_buffer(env, param_ref, &param_array, &param_elems);
	glSamplerParameteriv((GLuint)sampler, (GLenum)pname, (const GLint *)param);
	release_nio_buffer(env, param_array, param_elems);
}

/* void glSamplerParameterf ( GLuint sampler, GLenum pname, GLfloat param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glSamplerParameterf__IIF(JNIEnv *env, jclass clazz, jint sampler, jint pname, jfloat param)
{
	glSamplerParameterf((GLuint)sampler, (GLenum)pname, (GLfloat)param);
}

/* void glSamplerParameterfv ( GLuint sampler, GLenum pname, const GLfloat *param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glSamplerParameterfv__II_3FI(JNIEnv *env, jclass clazz, jint sampler, jint pname, jfloatArray param_ref, jint offset)
{
	jfloat *param = NULL;
	if (!param_ref) {
		throw_iae(env, "param == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, param_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	param = (*env)->GetFloatArrayElements(env, param_ref, NULL);
	glSamplerParameterfv((GLuint)sampler, (GLenum)pname, (const GLfloat *)(param + offset));
exit:
	if (param)
		(*env)->ReleaseFloatArrayElements(env, param_ref, param, JNI_ABORT);
}

/* void glSamplerParameterfv ( GLuint sampler, GLenum pname, const GLfloat *param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glSamplerParameterfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint sampler, jint pname, jobject param_ref)
{
	jarray param_array = NULL;
	jbyte *param_elems = NULL;
	void *param = NULL;
	param = get_nio_buffer(env, param_ref, &param_array, &param_elems);
	glSamplerParameterfv((GLuint)sampler, (GLenum)pname, (const GLfloat *)param);
	release_nio_buffer(env, param_array, param_elems);
}

/* void glGetSamplerParameteriv ( GLuint sampler, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetSamplerParameteriv__II_3II(JNIEnv *env, jclass clazz, jint sampler, jint pname, jintArray params_ref, jint offset)
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
	glGetSamplerParameteriv((GLuint)sampler, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetSamplerParameteriv ( GLuint sampler, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetSamplerParameteriv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint sampler, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetSamplerParameteriv((GLuint)sampler, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetSamplerParameterfv ( GLuint sampler, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetSamplerParameterfv__II_3FI(JNIEnv *env, jclass clazz, jint sampler, jint pname, jfloatArray params_ref, jint offset)
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
	glGetSamplerParameterfv((GLuint)sampler, (GLenum)pname, (GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, 0);
}

/* void glGetSamplerParameterfv ( GLuint sampler, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetSamplerParameterfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint sampler, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetSamplerParameterfv((GLuint)sampler, (GLenum)pname, (GLfloat *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glVertexAttribDivisor ( GLuint index, GLuint divisor ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glVertexAttribDivisor__II(JNIEnv *env, jclass clazz, jint index, jint divisor)
{
	glVertexAttribDivisor((GLuint)index, (GLuint)divisor);
}

/* void glBindTransformFeedback ( GLenum target, GLuint id ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glBindTransformFeedback__II(JNIEnv *env, jclass clazz, jint target, jint id)
{
	glBindTransformFeedback((GLenum)target, (GLuint)id);
}

/* void glDeleteTransformFeedbacks ( GLsizei n, const GLuint *ids ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDeleteTransformFeedbacks__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray ids_ref, jint offset)
{
	jint *ids = NULL;
	if (!ids_ref) {
		throw_iae(env, "ids == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, ids_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	ids = (*env)->GetIntArrayElements(env, ids_ref, NULL);
	glDeleteTransformFeedbacks((GLsizei)n, (const GLuint *)(ids + offset));
exit:
	if (ids)
		(*env)->ReleaseIntArrayElements(env, ids_ref, ids, JNI_ABORT);
}

/* void glDeleteTransformFeedbacks ( GLsizei n, const GLuint *ids ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDeleteTransformFeedbacks__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject ids_ref)
{
	jarray ids_array = NULL;
	jbyte *ids_elems = NULL;
	void *ids = NULL;
	ids = get_nio_buffer(env, ids_ref, &ids_array, &ids_elems);
	glDeleteTransformFeedbacks((GLsizei)n, (const GLuint *)ids);
	release_nio_buffer(env, ids_array, ids_elems);
}

/* void glGenTransformFeedbacks ( GLsizei n, GLuint *ids ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGenTransformFeedbacks__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray ids_ref, jint offset)
{
	jint *ids = NULL;
	if (!ids_ref) {
		throw_iae(env, "ids == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, ids_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	ids = (*env)->GetIntArrayElements(env, ids_ref, NULL);
	glGenTransformFeedbacks((GLsizei)n, (GLuint *)(ids + offset));
exit:
	if (ids)
		(*env)->ReleaseIntArrayElements(env, ids_ref, ids, 0);
}

/* void glGenTransformFeedbacks ( GLsizei n, GLuint *ids ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGenTransformFeedbacks__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject ids_ref)
{
	jarray ids_array = NULL;
	jbyte *ids_elems = NULL;
	void *ids = NULL;
	if (!ids_ref) {
		throw_iae(env, "ids == null");
		goto exit;
	}
	ids = get_nio_buffer(env, ids_ref, &ids_array, &ids_elems);
	glGenTransformFeedbacks((GLsizei)n, (GLuint *)ids);
exit:
	release_nio_buffer(env, ids_array, ids_elems);
}

/* GLboolean glIsTransformFeedback ( GLuint id ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES30_glIsTransformFeedback__I(JNIEnv *env, jclass clazz, jint id)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsTransformFeedback((GLuint)id);
	return _ret;
}

/* void glPauseTransformFeedback ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glPauseTransformFeedback__(JNIEnv *env, jclass clazz)
{
	glPauseTransformFeedback();
}

/* void glResumeTransformFeedback ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glResumeTransformFeedback__(JNIEnv *env, jclass clazz)
{
	glResumeTransformFeedback();
}

/* void glGetProgramBinary ( GLuint program, GLsizei bufSize, GLsizei *length, GLenum *binaryFormat, GLvoid *binary ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetProgramBinary__II_3II_3IILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint program, jint bufSize, jintArray length_ref, jint lengthOffset, jintArray binaryFormat_ref, jint binaryFormatOffset, jobject binary_ref)
{
	jarray binary_array = NULL;
	jbyte *binary_elems = NULL;
	void *binary = NULL;
	jint *binaryFormat = NULL;
	jint *length = NULL;
	if (!length_ref) {
		throw_iae(env, "length == null");
		goto exit;
	}
	if (lengthOffset < 0) {
		throw_iae(env, "lengthOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, length_ref) - lengthOffset < 1) {
		throw_iae(env, "length - lengthOffset < 1 < needed");
		goto exit;
	}
	length = (*env)->GetIntArrayElements(env, length_ref, NULL);
	if (!binaryFormat_ref) {
		throw_iae(env, "binaryFormat == null");
		goto exit;
	}
	if (binaryFormatOffset < 0) {
		throw_iae(env, "binaryFormatOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, binaryFormat_ref) - binaryFormatOffset < 1) {
		throw_iae(env, "length - binaryFormatOffset < 1 < needed");
		goto exit;
	}
	binaryFormat = (*env)->GetIntArrayElements(env, binaryFormat_ref, NULL);
	if (!binary_ref) {
		throw_iae(env, "binary == null");
		goto exit;
	}
	binary = get_nio_buffer(env, binary_ref, &binary_array, &binary_elems);
	glGetProgramBinary((GLuint)program, (GLsizei)bufSize, (GLsizei *)(length + lengthOffset), (GLenum *)(binaryFormat + binaryFormatOffset), (void *)binary);
exit:
	if (length)
		(*env)->ReleaseIntArrayElements(env, length_ref, length, 0);
	if (binaryFormat)
		(*env)->ReleaseIntArrayElements(env, binaryFormat_ref, binaryFormat, 0);
	release_nio_buffer(env, binary_array, binary_elems);
}

/* void glGetProgramBinary ( GLuint program, GLsizei bufSize, GLsizei *length, GLenum *binaryFormat, GLvoid *binary ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetProgramBinary__IILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint program, jint bufSize, jobject length_ref, jobject binaryFormat_ref, jobject binary_ref)
{
	jarray binary_array = NULL;
	jbyte *binary_elems = NULL;
	void *binary = NULL;
	jarray binaryFormat_array = NULL;
	jbyte *binaryFormat_elems = NULL;
	void *binaryFormat = NULL;
	jarray length_array = NULL;
	jbyte *length_elems = NULL;
	void *length = NULL;
	if (!length_ref) {
		throw_iae(env, "length == null");
		goto exit;
	}
	length = get_nio_buffer(env, length_ref, &length_array, &length_elems);
	if (!binaryFormat_ref) {
		throw_iae(env, "binaryFormat == null");
		goto exit;
	}
	binaryFormat = get_nio_buffer(env, binaryFormat_ref, &binaryFormat_array, &binaryFormat_elems);
	if (!binary_ref) {
		throw_iae(env, "binary == null");
		goto exit;
	}
	binary = get_nio_buffer(env, binary_ref, &binary_array, &binary_elems);
	glGetProgramBinary((GLuint)program, (GLsizei)bufSize, (GLsizei *)length, (GLenum *)binaryFormat, (void *)binary);
exit:
	release_nio_buffer(env, length_array, length_elems);
	release_nio_buffer(env, binaryFormat_array, binaryFormat_elems);
	release_nio_buffer(env, binary_array, binary_elems);
}

/* void glProgramBinary ( GLuint program, GLenum binaryFormat, const GLvoid *binary, GLsizei length ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glProgramBinary__IILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint program, jint binaryFormat, jobject binary_ref, jint length)
{
	jarray binary_array = NULL;
	jbyte *binary_elems = NULL;
	void *binary = NULL;
	binary = get_nio_buffer(env, binary_ref, &binary_array, &binary_elems);
	glProgramBinary((GLuint)program, (GLenum)binaryFormat, (const void *)binary, (GLsizei)length);
	release_nio_buffer(env, binary_array, binary_elems);
}

/* void glProgramParameteri ( GLuint program, GLenum pname, GLint value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glProgramParameteri__III(JNIEnv *env, jclass clazz, jint program, jint pname, jint value)
{
	glProgramParameteri((GLuint)program, (GLenum)pname, (GLint)value);
}

/* void glInvalidateFramebuffer ( GLenum target, GLsizei numAttachments, const GLenum *attachments ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glInvalidateFramebuffer__II_3II(JNIEnv *env, jclass clazz, jint target, jint numAttachments, jintArray attachments_ref, jint offset)
{
	jint *attachments = NULL;
	if (!attachments_ref) {
		throw_iae(env, "attachments == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, attachments_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	attachments = (*env)->GetIntArrayElements(env, attachments_ref, NULL);
	glInvalidateFramebuffer((GLenum)target, (GLsizei)numAttachments, (const GLenum *)(attachments + offset));
exit:
	if (attachments)
		(*env)->ReleaseIntArrayElements(env, attachments_ref, attachments, JNI_ABORT);
}

/* void glInvalidateFramebuffer ( GLenum target, GLsizei numAttachments, const GLenum *attachments ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glInvalidateFramebuffer__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint numAttachments, jobject attachments_ref)
{
	jarray attachments_array = NULL;
	jbyte *attachments_elems = NULL;
	void *attachments = NULL;
	attachments = get_nio_buffer(env, attachments_ref, &attachments_array, &attachments_elems);
	glInvalidateFramebuffer((GLenum)target, (GLsizei)numAttachments, (const GLenum *)attachments);
	release_nio_buffer(env, attachments_array, attachments_elems);
}

/* void glInvalidateSubFramebuffer ( GLenum target, GLsizei numAttachments, const GLenum *attachments, GLint x, GLint y, GLsizei width, GLsizei height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glInvalidateSubFramebuffer__II_3IIIIII(JNIEnv *env, jclass clazz, jint target, jint numAttachments, jintArray attachments_ref, jint offset, jint x, jint y, jint width, jint height)
{
	jint *attachments = NULL;
	if (!attachments_ref) {
		throw_iae(env, "attachments == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, attachments_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	attachments = (*env)->GetIntArrayElements(env, attachments_ref, NULL);
	glInvalidateSubFramebuffer((GLenum)target, (GLsizei)numAttachments, (const GLenum *)(attachments + offset), (GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height);
exit:
	if (attachments)
		(*env)->ReleaseIntArrayElements(env, attachments_ref, attachments, JNI_ABORT);
}

/* void glInvalidateSubFramebuffer ( GLenum target, GLsizei numAttachments, const GLenum *attachments, GLint x, GLint y, GLsizei width, GLsizei height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glInvalidateSubFramebuffer__IILjava_nio_IntBuffer_2IIII(JNIEnv *env, jclass clazz, jint target, jint numAttachments, jobject attachments_ref, jint x, jint y, jint width, jint height)
{
	jarray attachments_array = NULL;
	jbyte *attachments_elems = NULL;
	void *attachments = NULL;
	attachments = get_nio_buffer(env, attachments_ref, &attachments_array, &attachments_elems);
	glInvalidateSubFramebuffer((GLenum)target, (GLsizei)numAttachments, (const GLenum *)attachments, (GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height);
	release_nio_buffer(env, attachments_array, attachments_elems);
}

/* void glTexStorage2D ( GLenum target, GLsizei levels, GLenum internalformat, GLsizei width, GLsizei height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glTexStorage2D__IIIII(JNIEnv *env, jclass clazz, jint target, jint levels, jint internalformat, jint width, jint height)
{
	glTexStorage2D((GLenum)target, (GLsizei)levels, (GLenum)internalformat, (GLsizei)width, (GLsizei)height);
}

/* void glTexStorage3D ( GLenum target, GLsizei levels, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glTexStorage3D__IIIIII(JNIEnv *env, jclass clazz, jint target, jint levels, jint internalformat, jint width, jint height, jint depth)
{
	glTexStorage3D((GLenum)target, (GLsizei)levels, (GLenum)internalformat, (GLsizei)width, (GLsizei)height, (GLsizei)depth);
}

/* void glGetInternalformativ ( GLenum target, GLenum internalformat, GLenum pname, GLsizei bufSize, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetInternalformativ__IIII_3II(JNIEnv *env, jclass clazz, jint target, jint internalformat, jint pname, jint bufSize, jintArray params_ref, jint offset)
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
	glGetInternalformativ((GLenum)target, (GLenum)internalformat, (GLenum)pname, (GLsizei)bufSize, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetInternalformativ ( GLenum target, GLenum internalformat, GLenum pname, GLsizei bufSize, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetInternalformativ__IIIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint internalformat, jint pname, jint bufSize, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetInternalformativ((GLenum)target, (GLenum)internalformat, (GLenum)pname, (GLsizei)bufSize, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glReadPixels ( GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glReadPixels__IIIIIII(JNIEnv *env, jclass clazz, jint x, jint y, jint width, jint height, jint format, jint type, jint offset)
{
	glReadPixels((GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height, (GLenum)format, (GLenum)type, (void *)(intptr_t)offset);
}

/* ---- written by hand: strings, string arrays and mapped buffers ---- */

/* a direct ByteBuffer over a mapping of the bound buffer (AOSP returns one of the mapped length) */
static jobject mapped_buffer(JNIEnv *env, void *pointer, GLint64 length)
{
	return pointer ? (*env)->NewDirectByteBuffer(env, pointer, length) : NULL;
}

/* void glGetBufferPointerv ( GLenum target, GLenum pname, GLvoid** params ) */
JNIEXPORT jobject JNICALL Java_android_opengl_GLES30_glGetBufferPointerv__II(JNIEnv *env, jclass clazz, jint target, jint pname)
{
	GLint64 length = 0;
	void *pointer = NULL;
	glGetBufferParameteri64v((GLenum)target, GL_BUFFER_MAP_LENGTH, &length);
	glGetBufferPointerv((GLenum)target, (GLenum)pname, &pointer);
	return mapped_buffer(env, pointer, length);
}

/* GLvoid * glMapBufferRange ( GLenum target, GLintptr offset, GLsizeiptr length, GLbitfield access ) */
JNIEXPORT jobject JNICALL Java_android_opengl_GLES30_glMapBufferRange__IIII(JNIEnv *env, jclass clazz, jint target, jint offset, jint length, jint access)
{
	void *pointer = glMapBufferRange((GLenum)target, (GLintptr)offset, (GLsizeiptr)length, (GLbitfield)access);
	return mapped_buffer(env, pointer, length);
}

/* void glTransformFeedbackVaryings ( GLuint program, GLsizei count, const GLchar *varyings, GLenum bufferMode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glTransformFeedbackVaryings__I_3Ljava_lang_String_2I(JNIEnv *env, jclass clazz, jint program, jobjectArray varyings_ref, jint bufferMode)
{
	if (!varyings_ref) {
		throw_iae(env, "varyings == null");
		return;
	}
	jsize count;
	const GLchar **varyings = get_strings(env, varyings_ref, &count);
	glTransformFeedbackVaryings((GLuint)program, count, varyings, (GLenum)bufferMode);
	release_strings(env, varyings_ref, varyings, count);
}

/* void glGetTransformFeedbackVarying ( GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size, GLenum *type, GLchar *name )
 * deprecated variant: name is a single byte passed by value, so there is no room for the name; only length,
 * size and type are returned */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetTransformFeedbackVarying__IIILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2B(JNIEnv *env, jclass clazz, jint program, jint index, jint bufsize, jobject length_ref, jobject size_ref, jobject type_ref, jbyte name)
{
	jarray a1 = NULL, a2 = NULL, a3 = NULL;
	jbyte *e1 = NULL, *e2 = NULL, *e3 = NULL;
	if (!size_ref || !type_ref) {
		throw_iae(env, !size_ref ? "size == null" : "type == null");
		return;
	}
	GLsizei *length = get_nio_buffer(env, length_ref, &a1, &e1);
	GLint *size = get_nio_buffer(env, size_ref, &a2, &e2);
	GLenum *type = get_nio_buffer(env, type_ref, &a3, &e3);
	glGetTransformFeedbackVarying((GLuint)program, (GLuint)index, 0, length, size, type, NULL);
	release_nio_buffer(env, a3, e3);
	release_nio_buffer(env, a2, e2);
	release_nio_buffer(env, a1, e1);
}

static jstring transform_feedback_varying(JNIEnv *env, jint program, jint index, GLint *size, GLenum *type)
{
	GLint max_length = 0;
	glGetProgramiv((GLuint)program, GL_TRANSFORM_FEEDBACK_VARYING_MAX_LENGTH, &max_length);
	GLchar *name = calloc(max_length + 1, 1);
	glGetTransformFeedbackVarying((GLuint)program, (GLuint)index, max_length + 1, NULL, size, type, name);
	jstring result = (*env)->NewStringUTF(env, name);
	free(name);
	return result;
}

JNIEXPORT jstring JNICALL Java_android_opengl_GLES30_glGetTransformFeedbackVarying__II_3II_3II(JNIEnv *env, jclass clazz, jint program, jint index, jintArray size_ref, jint sizeOffset, jintArray type_ref, jint typeOffset)
{
	if (!size_ref || sizeOffset < 0 || (*env)->GetArrayLength(env, size_ref) - sizeOffset < 1) {
		throw_iae(env, !size_ref ? "size == null" : sizeOffset < 0 ? "sizeOffset < 0" : "length - sizeOffset < 1 < needed");
		return NULL;
	}
	if (!type_ref || typeOffset < 0 || (*env)->GetArrayLength(env, type_ref) - typeOffset < 1) {
		throw_iae(env, !type_ref ? "type == null" : typeOffset < 0 ? "typeOffset < 0" : "length - typeOffset < 1 < needed");
		return NULL;
	}
	GLint size;
	GLenum type;
	jstring result = transform_feedback_varying(env, program, index, &size, &type);
	(*env)->SetIntArrayRegion(env, size_ref, sizeOffset, 1, &size);
	(*env)->SetIntArrayRegion(env, type_ref, typeOffset, 1, (jint *)&type);
	return result;
}

JNIEXPORT jstring JNICALL Java_android_opengl_GLES30_glGetTransformFeedbackVarying__IILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint index, jobject size_ref, jobject type_ref)
{
	jarray a1 = NULL, a2 = NULL;
	jbyte *e1 = NULL, *e2 = NULL;
	if (!size_ref || !type_ref) {
		throw_iae(env, !size_ref ? "size == null" : "type == null");
		return NULL;
	}
	GLint *size = get_nio_buffer(env, size_ref, &a1, &e1);
	GLenum *type = get_nio_buffer(env, type_ref, &a2, &e2);
	jstring result = transform_feedback_varying(env, program, index, size, type);
	release_nio_buffer(env, a2, e2);
	release_nio_buffer(env, a1, e1);
	return result;
}

/* const GLubyte * glGetStringi ( GLenum name, GLuint index ) */
JNIEXPORT jstring JNICALL Java_android_opengl_GLES30_glGetStringi__II(JNIEnv *env, jclass clazz, jint name, jint index)
{
	const GLubyte *chars = glGetStringi((GLenum)name, (GLuint)index);
	return chars ? (*env)->NewStringUTF(env, (const char *)chars) : NULL;
}

/* void glGetUniformIndices ( GLuint program, GLsizei uniformCount, const GLchar *const *uniformNames, GLuint *uniformIndices ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetUniformIndices__I_3Ljava_lang_String_2_3II(JNIEnv *env, jclass clazz, jint program, jobjectArray uniformNames_ref, jintArray uniformIndices_ref, jint uniformIndicesOffset)
{
	if (!uniformNames_ref) {
		throw_iae(env, "uniformNames == null");
		return;
	}
	jsize count = (*env)->GetArrayLength(env, uniformNames_ref);
	if (!uniformIndices_ref || uniformIndicesOffset < 0 || (*env)->GetArrayLength(env, uniformIndices_ref) - uniformIndicesOffset < count) {
		throw_iae(env, !uniformIndices_ref ? "uniformIndices == null" : uniformIndicesOffset < 0 ? "uniformIndicesOffset < 0" : "length - uniformIndicesOffset < count < needed");
		return;
	}
	const GLchar **names = get_strings(env, uniformNames_ref, &count);
	jint *indices = (*env)->GetIntArrayElements(env, uniformIndices_ref, NULL);
	glGetUniformIndices((GLuint)program, count, names, (GLuint *)(indices + uniformIndicesOffset));
	(*env)->ReleaseIntArrayElements(env, uniformIndices_ref, indices, 0);
	release_strings(env, uniformNames_ref, names, count);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetUniformIndices__I_3Ljava_lang_String_2Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jobjectArray uniformNames_ref, jobject uniformIndices_ref)
{
	if (!uniformNames_ref || !uniformIndices_ref) {
		throw_iae(env, !uniformNames_ref ? "uniformNames == null" : "uniformIndices == null");
		return;
	}
	jsize count = (*env)->GetArrayLength(env, uniformNames_ref);
	if (get_nio_buffer_size(env, uniformIndices_ref) < count) {
		throw_iae(env, "remaining() < count < needed");
		return;
	}
	const GLchar **names = get_strings(env, uniformNames_ref, &count);
	jarray array = NULL;
	jbyte *elems = NULL;
	GLuint *indices = get_nio_buffer(env, uniformIndices_ref, &array, &elems);
	glGetUniformIndices((GLuint)program, count, names, indices);
	release_nio_buffer(env, array, elems);
	release_strings(env, uniformNames_ref, names, count);
}

/* void glGetActiveUniformBlockName ( GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei *length, GLchar *uniformBlockName ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES30_glGetActiveUniformBlockName__IILjava_nio_Buffer_2Ljava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint program, jint uniformBlockIndex, jobject length_ref, jobject uniformBlockName_ref)
{
	if (!uniformBlockName_ref) {
		throw_iae(env, "uniformBlockName == null");
		return;
	}
	jarray a1 = NULL, a2 = NULL;
	jbyte *e1 = NULL, *e2 = NULL;
	GLsizei buf_size = get_nio_buffer_size(env, uniformBlockName_ref);
	GLsizei *length = get_nio_buffer(env, length_ref, &a1, &e1);
	GLchar *name = get_nio_buffer(env, uniformBlockName_ref, &a2, &e2);
	glGetActiveUniformBlockName((GLuint)program, (GLuint)uniformBlockIndex, buf_size, length, name);
	release_nio_buffer(env, a2, e2);
	release_nio_buffer(env, a1, e1);
}

JNIEXPORT jstring JNICALL Java_android_opengl_GLES30_glGetActiveUniformBlockName__II(JNIEnv *env, jclass clazz, jint program, jint uniformBlockIndex)
{
	GLint length = 0;
	glGetActiveUniformBlockiv((GLuint)program, (GLuint)uniformBlockIndex, GL_UNIFORM_BLOCK_NAME_LENGTH, &length);
	GLchar *name = calloc(length + 1, 1);
	glGetActiveUniformBlockName((GLuint)program, (GLuint)uniformBlockIndex, length + 1, NULL, name);
	jstring result = (*env)->NewStringUTF(env, name);
	free(name);
	return result;
}
