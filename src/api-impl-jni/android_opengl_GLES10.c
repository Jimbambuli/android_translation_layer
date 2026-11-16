/* JNI of android.opengl.GLES10. Generated from AOSP's GLES10.java (its "// C function" comments and native
 * declarations, Android 16) and the Khronos GLES 1.x headers, the functions with other return types written by hand.
 * AOSP's own JNI (frameworks/base/core/jni/android_opengl_GLES10.cpp) is generated as well and not in the local sources;
 * the argument checks follow its IllegalArgumentException messages for null arrays/buffers and negative offsets. */
#include "defines.h"
#include "util.h"
#include "android_opengl_GLES1.h"

#include "generated_headers/android_opengl_GLES10.h"

/* void glActiveTexture ( GLenum texture ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glActiveTexture__I(JNIEnv *env, jclass clazz, jint texture)
{
	glActiveTexture((GLenum)texture);
}

/* void glAlphaFunc ( GLenum func, GLclampf ref ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glAlphaFunc__IF(JNIEnv *env, jclass clazz, jint func, jfloat ref)
{
	glAlphaFunc((GLenum)func, (GLfloat)ref);
}

/* void glAlphaFuncx ( GLenum func, GLclampx ref ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glAlphaFuncx__II(JNIEnv *env, jclass clazz, jint func, jint ref)
{
	glAlphaFuncx((GLenum)func, (GLfixed)ref);
}

/* void glBindTexture ( GLenum target, GLuint texture ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glBindTexture__II(JNIEnv *env, jclass clazz, jint target, jint texture)
{
	glBindTexture((GLenum)target, (GLuint)texture);
}

/* void glBlendFunc ( GLenum sfactor, GLenum dfactor ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glBlendFunc__II(JNIEnv *env, jclass clazz, jint sfactor, jint dfactor)
{
	glBlendFunc((GLenum)sfactor, (GLenum)dfactor);
}

/* void glClear ( GLbitfield mask ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glClear__I(JNIEnv *env, jclass clazz, jint mask)
{
	glClear((GLbitfield)mask);
}

/* void glClearColor ( GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glClearColor__FFFF(JNIEnv *env, jclass clazz, jfloat red, jfloat green, jfloat blue, jfloat alpha)
{
	glClearColor((GLfloat)red, (GLfloat)green, (GLfloat)blue, (GLfloat)alpha);
}

/* void glClearColorx ( GLclampx red, GLclampx green, GLclampx blue, GLclampx alpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glClearColorx__IIII(JNIEnv *env, jclass clazz, jint red, jint green, jint blue, jint alpha)
{
	glClearColorx((GLfixed)red, (GLfixed)green, (GLfixed)blue, (GLfixed)alpha);
}

/* void glClearDepthf ( GLclampf depth ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glClearDepthf__F(JNIEnv *env, jclass clazz, jfloat depth)
{
	glClearDepthf((GLfloat)depth);
}

/* void glClearDepthx ( GLclampx depth ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glClearDepthx__I(JNIEnv *env, jclass clazz, jint depth)
{
	glClearDepthx((GLfixed)depth);
}

/* void glClearStencil ( GLint s ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glClearStencil__I(JNIEnv *env, jclass clazz, jint s)
{
	glClearStencil((GLint)s);
}

/* void glClientActiveTexture ( GLenum texture ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glClientActiveTexture__I(JNIEnv *env, jclass clazz, jint texture)
{
	glClientActiveTexture((GLenum)texture);
}

/* void glColor4f ( GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glColor4f__FFFF(JNIEnv *env, jclass clazz, jfloat red, jfloat green, jfloat blue, jfloat alpha)
{
	glColor4f((GLfloat)red, (GLfloat)green, (GLfloat)blue, (GLfloat)alpha);
}

/* void glColor4x ( GLfixed red, GLfixed green, GLfixed blue, GLfixed alpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glColor4x__IIII(JNIEnv *env, jclass clazz, jint red, jint green, jint blue, jint alpha)
{
	glColor4x((GLfixed)red, (GLfixed)green, (GLfixed)blue, (GLfixed)alpha);
}

/* void glColorMask ( GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glColorMask__ZZZZ(JNIEnv *env, jclass clazz, jboolean red, jboolean green, jboolean blue, jboolean alpha)
{
	glColorMask((GLboolean)red, (GLboolean)green, (GLboolean)blue, (GLboolean)alpha);
}

/* void glColorPointer ( GLint size, GLenum type, GLsizei stride, const GLvoid *pointer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glColorPointerBounds__IIILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint size, jint type, jint stride, jobject pointer_ref, jint remaining)
{
	jarray pointer_array = NULL;
	jbyte *pointer_elems = NULL;
	void *pointer = NULL;
	pointer = get_nio_buffer(env, pointer_ref, &pointer_array, &pointer_elems);
	glColorPointer((GLint)size, (GLenum)type, (GLsizei)stride, (const void *)pointer);
	release_nio_buffer(env, pointer_array, pointer_elems);
}

/* void glCompressedTexImage2D ( GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glCompressedTexImage2D__IIIIIIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint target, jint level, jint internalformat, jint width, jint height, jint border, jint imageSize, jobject data_ref)
{
	jarray data_array = NULL;
	jbyte *data_elems = NULL;
	void *data = NULL;
	data = get_nio_buffer(env, data_ref, &data_array, &data_elems);
	glCompressedTexImage2D((GLenum)target, (GLint)level, (GLenum)internalformat, (GLsizei)width, (GLsizei)height, (GLint)border, (GLsizei)imageSize, (const void *)data);
	release_nio_buffer(env, data_array, data_elems);
}

/* void glCompressedTexSubImage2D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glCompressedTexSubImage2D__IIIIIIIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint target, jint level, jint xoffset, jint yoffset, jint width, jint height, jint format, jint imageSize, jobject data_ref)
{
	jarray data_array = NULL;
	jbyte *data_elems = NULL;
	void *data = NULL;
	data = get_nio_buffer(env, data_ref, &data_array, &data_elems);
	glCompressedTexSubImage2D((GLenum)target, (GLint)level, (GLint)xoffset, (GLint)yoffset, (GLsizei)width, (GLsizei)height, (GLenum)format, (GLsizei)imageSize, (const void *)data);
	release_nio_buffer(env, data_array, data_elems);
}

/* void glCopyTexImage2D ( GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glCopyTexImage2D__IIIIIIII(JNIEnv *env, jclass clazz, jint target, jint level, jint internalformat, jint x, jint y, jint width, jint height, jint border)
{
	glCopyTexImage2D((GLenum)target, (GLint)level, (GLenum)internalformat, (GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height, (GLint)border);
}

/* void glCopyTexSubImage2D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glCopyTexSubImage2D__IIIIIIII(JNIEnv *env, jclass clazz, jint target, jint level, jint xoffset, jint yoffset, jint x, jint y, jint width, jint height)
{
	glCopyTexSubImage2D((GLenum)target, (GLint)level, (GLint)xoffset, (GLint)yoffset, (GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height);
}

/* void glCullFace ( GLenum mode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glCullFace__I(JNIEnv *env, jclass clazz, jint mode)
{
	glCullFace((GLenum)mode);
}

/* void glDeleteTextures ( GLsizei n, const GLuint *textures ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glDeleteTextures__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray textures_ref, jint offset)
{
	jint *textures = NULL;
	if (!textures_ref) {
		throw_iae(env, "textures == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, textures_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	textures = (*env)->GetIntArrayElements(env, textures_ref, NULL);
	glDeleteTextures((GLsizei)n, (const GLuint *)(textures + offset));
exit:
	if (textures)
		(*env)->ReleaseIntArrayElements(env, textures_ref, textures, JNI_ABORT);
}

/* void glDeleteTextures ( GLsizei n, const GLuint *textures ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glDeleteTextures__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject textures_ref)
{
	jarray textures_array = NULL;
	jbyte *textures_elems = NULL;
	void *textures = NULL;
	textures = get_nio_buffer(env, textures_ref, &textures_array, &textures_elems);
	glDeleteTextures((GLsizei)n, (const GLuint *)textures);
	release_nio_buffer(env, textures_array, textures_elems);
}

/* void glDepthFunc ( GLenum func ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glDepthFunc__I(JNIEnv *env, jclass clazz, jint func)
{
	glDepthFunc((GLenum)func);
}

/* void glDepthMask ( GLboolean flag ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glDepthMask__Z(JNIEnv *env, jclass clazz, jboolean flag)
{
	glDepthMask((GLboolean)flag);
}

/* void glDepthRangef ( GLclampf zNear, GLclampf zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glDepthRangef__FF(JNIEnv *env, jclass clazz, jfloat zNear, jfloat zFar)
{
	glDepthRangef((GLfloat)zNear, (GLfloat)zFar);
}

/* void glDepthRangex ( GLclampx zNear, GLclampx zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glDepthRangex__II(JNIEnv *env, jclass clazz, jint zNear, jint zFar)
{
	glDepthRangex((GLfixed)zNear, (GLfixed)zFar);
}

/* void glDisable ( GLenum cap ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glDisable__I(JNIEnv *env, jclass clazz, jint cap)
{
	glDisable((GLenum)cap);
}

/* void glDisableClientState ( GLenum array ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glDisableClientState__I(JNIEnv *env, jclass clazz, jint array)
{
	glDisableClientState((GLenum)array);
}

/* void glDrawArrays ( GLenum mode, GLint first, GLsizei count ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glDrawArrays__III(JNIEnv *env, jclass clazz, jint mode, jint first, jint count)
{
	glDrawArrays((GLenum)mode, (GLint)first, (GLsizei)count);
}

/* void glDrawElements ( GLenum mode, GLsizei count, GLenum type, const GLvoid *indices ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glDrawElements__IIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint mode, jint count, jint type, jobject indices_ref)
{
	jarray indices_array = NULL;
	jbyte *indices_elems = NULL;
	void *indices = NULL;
	indices = get_nio_buffer(env, indices_ref, &indices_array, &indices_elems);
	glDrawElements((GLenum)mode, (GLsizei)count, (GLenum)type, (const void *)indices);
	release_nio_buffer(env, indices_array, indices_elems);
}

/* void glEnable ( GLenum cap ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glEnable__I(JNIEnv *env, jclass clazz, jint cap)
{
	glEnable((GLenum)cap);
}

/* void glEnableClientState ( GLenum array ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glEnableClientState__I(JNIEnv *env, jclass clazz, jint array)
{
	glEnableClientState((GLenum)array);
}

/* void glFinish ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glFinish__(JNIEnv *env, jclass clazz)
{
	glFinish();
}

/* void glFlush ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glFlush__(JNIEnv *env, jclass clazz)
{
	glFlush();
}

/* void glFogf ( GLenum pname, GLfloat param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glFogf__IF(JNIEnv *env, jclass clazz, jint pname, jfloat param)
{
	glFogf((GLenum)pname, (GLfloat)param);
}

/* void glFogfv ( GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glFogfv__I_3FI(JNIEnv *env, jclass clazz, jint pname, jfloatArray params_ref, jint offset)
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
	glFogfv((GLenum)pname, (const GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glFogfv ( GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glFogfv__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glFogfv((GLenum)pname, (const GLfloat *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glFogx ( GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glFogx__II(JNIEnv *env, jclass clazz, jint pname, jint param)
{
	glFogx((GLenum)pname, (GLfixed)param);
}

/* void glFogxv ( GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glFogxv__I_3II(JNIEnv *env, jclass clazz, jint pname, jintArray params_ref, jint offset)
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
	glFogxv((GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glFogxv ( GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glFogxv__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glFogxv((GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glFrontFace ( GLenum mode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glFrontFace__I(JNIEnv *env, jclass clazz, jint mode)
{
	glFrontFace((GLenum)mode);
}

/* void glFrustumf ( GLfloat left, GLfloat right, GLfloat bottom, GLfloat top, GLfloat zNear, GLfloat zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glFrustumf__FFFFFF(JNIEnv *env, jclass clazz, jfloat left, jfloat right, jfloat bottom, jfloat top, jfloat zNear, jfloat zFar)
{
	glFrustumf((GLfloat)left, (GLfloat)right, (GLfloat)bottom, (GLfloat)top, (GLfloat)zNear, (GLfloat)zFar);
}

/* void glFrustumx ( GLfixed left, GLfixed right, GLfixed bottom, GLfixed top, GLfixed zNear, GLfixed zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glFrustumx__IIIIII(JNIEnv *env, jclass clazz, jint left, jint right, jint bottom, jint top, jint zNear, jint zFar)
{
	glFrustumx((GLfixed)left, (GLfixed)right, (GLfixed)bottom, (GLfixed)top, (GLfixed)zNear, (GLfixed)zFar);
}

/* void glGenTextures ( GLsizei n, GLuint *textures ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glGenTextures__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray textures_ref, jint offset)
{
	jint *textures = NULL;
	if (!textures_ref) {
		throw_iae(env, "textures == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, textures_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	textures = (*env)->GetIntArrayElements(env, textures_ref, NULL);
	glGenTextures((GLsizei)n, (GLuint *)(textures + offset));
exit:
	if (textures)
		(*env)->ReleaseIntArrayElements(env, textures_ref, textures, 0);
}

/* void glGenTextures ( GLsizei n, GLuint *textures ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glGenTextures__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject textures_ref)
{
	jarray textures_array = NULL;
	jbyte *textures_elems = NULL;
	void *textures = NULL;
	if (!textures_ref) {
		throw_iae(env, "textures == null");
		goto exit;
	}
	textures = get_nio_buffer(env, textures_ref, &textures_array, &textures_elems);
	glGenTextures((GLsizei)n, (GLuint *)textures);
exit:
	release_nio_buffer(env, textures_array, textures_elems);
}

/* GLenum glGetError ( void ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES10_glGetError__(JNIEnv *env, jclass clazz)
{
	jint _ret = 0;
	_ret = (jint)glGetError();
	return _ret;
}

/* void glGetIntegerv ( GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glGetIntegerv__I_3II(JNIEnv *env, jclass clazz, jint pname, jintArray params_ref, jint offset)
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
	glGetIntegerv((GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetIntegerv ( GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glGetIntegerv__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetIntegerv((GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glHint ( GLenum target, GLenum mode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glHint__II(JNIEnv *env, jclass clazz, jint target, jint mode)
{
	glHint((GLenum)target, (GLenum)mode);
}

/* void glLightModelf ( GLenum pname, GLfloat param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLightModelf__IF(JNIEnv *env, jclass clazz, jint pname, jfloat param)
{
	glLightModelf((GLenum)pname, (GLfloat)param);
}

/* void glLightModelfv ( GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLightModelfv__I_3FI(JNIEnv *env, jclass clazz, jint pname, jfloatArray params_ref, jint offset)
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
	glLightModelfv((GLenum)pname, (const GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glLightModelfv ( GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLightModelfv__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glLightModelfv((GLenum)pname, (const GLfloat *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glLightModelx ( GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLightModelx__II(JNIEnv *env, jclass clazz, jint pname, jint param)
{
	glLightModelx((GLenum)pname, (GLfixed)param);
}

/* void glLightModelxv ( GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLightModelxv__I_3II(JNIEnv *env, jclass clazz, jint pname, jintArray params_ref, jint offset)
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
	glLightModelxv((GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glLightModelxv ( GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLightModelxv__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glLightModelxv((GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glLightf ( GLenum light, GLenum pname, GLfloat param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLightf__IIF(JNIEnv *env, jclass clazz, jint light, jint pname, jfloat param)
{
	glLightf((GLenum)light, (GLenum)pname, (GLfloat)param);
}

/* void glLightfv ( GLenum light, GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLightfv__II_3FI(JNIEnv *env, jclass clazz, jint light, jint pname, jfloatArray params_ref, jint offset)
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
	glLightfv((GLenum)light, (GLenum)pname, (const GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glLightfv ( GLenum light, GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLightfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint light, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glLightfv((GLenum)light, (GLenum)pname, (const GLfloat *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glLightx ( GLenum light, GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLightx__III(JNIEnv *env, jclass clazz, jint light, jint pname, jint param)
{
	glLightx((GLenum)light, (GLenum)pname, (GLfixed)param);
}

/* void glLightxv ( GLenum light, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLightxv__II_3II(JNIEnv *env, jclass clazz, jint light, jint pname, jintArray params_ref, jint offset)
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
	glLightxv((GLenum)light, (GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glLightxv ( GLenum light, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLightxv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint light, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glLightxv((GLenum)light, (GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glLineWidth ( GLfloat width ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLineWidth__F(JNIEnv *env, jclass clazz, jfloat width)
{
	glLineWidth((GLfloat)width);
}

/* void glLineWidthx ( GLfixed width ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLineWidthx__I(JNIEnv *env, jclass clazz, jint width)
{
	glLineWidthx((GLfixed)width);
}

/* void glLoadIdentity ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLoadIdentity__(JNIEnv *env, jclass clazz)
{
	glLoadIdentity();
}

/* void glLoadMatrixf ( const GLfloat *m ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLoadMatrixf___3FI(JNIEnv *env, jclass clazz, jfloatArray m_ref, jint offset)
{
	jfloat *m = NULL;
	if (!m_ref) {
		throw_iae(env, "m == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, m_ref) - offset < 16) {
		throw_iae(env, "length - offset < 16 < needed");
		goto exit;
	}
	m = (*env)->GetFloatArrayElements(env, m_ref, NULL);
	glLoadMatrixf((const GLfloat *)(m + offset));
exit:
	if (m)
		(*env)->ReleaseFloatArrayElements(env, m_ref, m, JNI_ABORT);
}

/* void glLoadMatrixf ( const GLfloat *m ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLoadMatrixf__Ljava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jobject m_ref)
{
	jarray m_array = NULL;
	jbyte *m_elems = NULL;
	void *m = NULL;
	m = get_nio_buffer(env, m_ref, &m_array, &m_elems);
	glLoadMatrixf((const GLfloat *)m);
	release_nio_buffer(env, m_array, m_elems);
}

/* void glLoadMatrixx ( const GLfixed *m ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLoadMatrixx___3II(JNIEnv *env, jclass clazz, jintArray m_ref, jint offset)
{
	jint *m = NULL;
	if (!m_ref) {
		throw_iae(env, "m == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, m_ref) - offset < 16) {
		throw_iae(env, "length - offset < 16 < needed");
		goto exit;
	}
	m = (*env)->GetIntArrayElements(env, m_ref, NULL);
	glLoadMatrixx((const GLfixed *)(m + offset));
exit:
	if (m)
		(*env)->ReleaseIntArrayElements(env, m_ref, m, JNI_ABORT);
}

/* void glLoadMatrixx ( const GLfixed *m ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLoadMatrixx__Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jobject m_ref)
{
	jarray m_array = NULL;
	jbyte *m_elems = NULL;
	void *m = NULL;
	m = get_nio_buffer(env, m_ref, &m_array, &m_elems);
	glLoadMatrixx((const GLfixed *)m);
	release_nio_buffer(env, m_array, m_elems);
}

/* void glLogicOp ( GLenum opcode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glLogicOp__I(JNIEnv *env, jclass clazz, jint opcode)
{
	glLogicOp((GLenum)opcode);
}

/* void glMaterialf ( GLenum face, GLenum pname, GLfloat param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMaterialf__IIF(JNIEnv *env, jclass clazz, jint face, jint pname, jfloat param)
{
	glMaterialf((GLenum)face, (GLenum)pname, (GLfloat)param);
}

/* void glMaterialfv ( GLenum face, GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMaterialfv__II_3FI(JNIEnv *env, jclass clazz, jint face, jint pname, jfloatArray params_ref, jint offset)
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
	glMaterialfv((GLenum)face, (GLenum)pname, (const GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glMaterialfv ( GLenum face, GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMaterialfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint face, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glMaterialfv((GLenum)face, (GLenum)pname, (const GLfloat *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glMaterialx ( GLenum face, GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMaterialx__III(JNIEnv *env, jclass clazz, jint face, jint pname, jint param)
{
	glMaterialx((GLenum)face, (GLenum)pname, (GLfixed)param);
}

/* void glMaterialxv ( GLenum face, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMaterialxv__II_3II(JNIEnv *env, jclass clazz, jint face, jint pname, jintArray params_ref, jint offset)
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
	glMaterialxv((GLenum)face, (GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glMaterialxv ( GLenum face, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMaterialxv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint face, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glMaterialxv((GLenum)face, (GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glMatrixMode ( GLenum mode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMatrixMode__I(JNIEnv *env, jclass clazz, jint mode)
{
	glMatrixMode((GLenum)mode);
}

/* void glMultMatrixf ( const GLfloat *m ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMultMatrixf___3FI(JNIEnv *env, jclass clazz, jfloatArray m_ref, jint offset)
{
	jfloat *m = NULL;
	if (!m_ref) {
		throw_iae(env, "m == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, m_ref) - offset < 16) {
		throw_iae(env, "length - offset < 16 < needed");
		goto exit;
	}
	m = (*env)->GetFloatArrayElements(env, m_ref, NULL);
	glMultMatrixf((const GLfloat *)(m + offset));
exit:
	if (m)
		(*env)->ReleaseFloatArrayElements(env, m_ref, m, JNI_ABORT);
}

/* void glMultMatrixf ( const GLfloat *m ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMultMatrixf__Ljava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jobject m_ref)
{
	jarray m_array = NULL;
	jbyte *m_elems = NULL;
	void *m = NULL;
	m = get_nio_buffer(env, m_ref, &m_array, &m_elems);
	glMultMatrixf((const GLfloat *)m);
	release_nio_buffer(env, m_array, m_elems);
}

/* void glMultMatrixx ( const GLfixed *m ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMultMatrixx___3II(JNIEnv *env, jclass clazz, jintArray m_ref, jint offset)
{
	jint *m = NULL;
	if (!m_ref) {
		throw_iae(env, "m == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, m_ref) - offset < 16) {
		throw_iae(env, "length - offset < 16 < needed");
		goto exit;
	}
	m = (*env)->GetIntArrayElements(env, m_ref, NULL);
	glMultMatrixx((const GLfixed *)(m + offset));
exit:
	if (m)
		(*env)->ReleaseIntArrayElements(env, m_ref, m, JNI_ABORT);
}

/* void glMultMatrixx ( const GLfixed *m ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMultMatrixx__Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jobject m_ref)
{
	jarray m_array = NULL;
	jbyte *m_elems = NULL;
	void *m = NULL;
	m = get_nio_buffer(env, m_ref, &m_array, &m_elems);
	glMultMatrixx((const GLfixed *)m);
	release_nio_buffer(env, m_array, m_elems);
}

/* void glMultiTexCoord4f ( GLenum target, GLfloat s, GLfloat t, GLfloat r, GLfloat q ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMultiTexCoord4f__IFFFF(JNIEnv *env, jclass clazz, jint target, jfloat s, jfloat t, jfloat r, jfloat q)
{
	glMultiTexCoord4f((GLenum)target, (GLfloat)s, (GLfloat)t, (GLfloat)r, (GLfloat)q);
}

/* void glMultiTexCoord4x ( GLenum target, GLfixed s, GLfixed t, GLfixed r, GLfixed q ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glMultiTexCoord4x__IIIII(JNIEnv *env, jclass clazz, jint target, jint s, jint t, jint r, jint q)
{
	glMultiTexCoord4x((GLenum)target, (GLfixed)s, (GLfixed)t, (GLfixed)r, (GLfixed)q);
}

/* void glNormal3f ( GLfloat nx, GLfloat ny, GLfloat nz ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glNormal3f__FFF(JNIEnv *env, jclass clazz, jfloat nx, jfloat ny, jfloat nz)
{
	glNormal3f((GLfloat)nx, (GLfloat)ny, (GLfloat)nz);
}

/* void glNormal3x ( GLfixed nx, GLfixed ny, GLfixed nz ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glNormal3x__III(JNIEnv *env, jclass clazz, jint nx, jint ny, jint nz)
{
	glNormal3x((GLfixed)nx, (GLfixed)ny, (GLfixed)nz);
}

/* void glNormalPointer ( GLenum type, GLsizei stride, const GLvoid *pointer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glNormalPointerBounds__IILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint type, jint stride, jobject pointer_ref, jint remaining)
{
	jarray pointer_array = NULL;
	jbyte *pointer_elems = NULL;
	void *pointer = NULL;
	pointer = get_nio_buffer(env, pointer_ref, &pointer_array, &pointer_elems);
	glNormalPointer((GLenum)type, (GLsizei)stride, (const void *)pointer);
	release_nio_buffer(env, pointer_array, pointer_elems);
}

/* void glOrthof ( GLfloat left, GLfloat right, GLfloat bottom, GLfloat top, GLfloat zNear, GLfloat zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glOrthof__FFFFFF(JNIEnv *env, jclass clazz, jfloat left, jfloat right, jfloat bottom, jfloat top, jfloat zNear, jfloat zFar)
{
	glOrthof((GLfloat)left, (GLfloat)right, (GLfloat)bottom, (GLfloat)top, (GLfloat)zNear, (GLfloat)zFar);
}

/* void glOrthox ( GLfixed left, GLfixed right, GLfixed bottom, GLfixed top, GLfixed zNear, GLfixed zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glOrthox__IIIIII(JNIEnv *env, jclass clazz, jint left, jint right, jint bottom, jint top, jint zNear, jint zFar)
{
	glOrthox((GLfixed)left, (GLfixed)right, (GLfixed)bottom, (GLfixed)top, (GLfixed)zNear, (GLfixed)zFar);
}

/* void glPixelStorei ( GLenum pname, GLint param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glPixelStorei__II(JNIEnv *env, jclass clazz, jint pname, jint param)
{
	glPixelStorei((GLenum)pname, (GLint)param);
}

/* void glPointSize ( GLfloat size ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glPointSize__F(JNIEnv *env, jclass clazz, jfloat size)
{
	glPointSize((GLfloat)size);
}

/* void glPointSizex ( GLfixed size ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glPointSizex__I(JNIEnv *env, jclass clazz, jint size)
{
	glPointSizex((GLfixed)size);
}

/* void glPolygonOffset ( GLfloat factor, GLfloat units ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glPolygonOffset__FF(JNIEnv *env, jclass clazz, jfloat factor, jfloat units)
{
	glPolygonOffset((GLfloat)factor, (GLfloat)units);
}

/* void glPolygonOffsetx ( GLfixed factor, GLfixed units ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glPolygonOffsetx__II(JNIEnv *env, jclass clazz, jint factor, jint units)
{
	glPolygonOffsetx((GLfixed)factor, (GLfixed)units);
}

/* void glPopMatrix ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glPopMatrix__(JNIEnv *env, jclass clazz)
{
	glPopMatrix();
}

/* void glPushMatrix ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glPushMatrix__(JNIEnv *env, jclass clazz)
{
	glPushMatrix();
}

/* void glReadPixels ( GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid *pixels ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glReadPixels__IIIIIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint x, jint y, jint width, jint height, jint format, jint type, jobject pixels_ref)
{
	jarray pixels_array = NULL;
	jbyte *pixels_elems = NULL;
	void *pixels = NULL;
	if (!pixels_ref) {
		throw_iae(env, "pixels == null");
		goto exit;
	}
	pixels = get_nio_buffer(env, pixels_ref, &pixels_array, &pixels_elems);
	glReadPixels((GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height, (GLenum)format, (GLenum)type, (void *)pixels);
exit:
	release_nio_buffer(env, pixels_array, pixels_elems);
}

/* void glRotatef ( GLfloat angle, GLfloat x, GLfloat y, GLfloat z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glRotatef__FFFF(JNIEnv *env, jclass clazz, jfloat angle, jfloat x, jfloat y, jfloat z)
{
	glRotatef((GLfloat)angle, (GLfloat)x, (GLfloat)y, (GLfloat)z);
}

/* void glRotatex ( GLfixed angle, GLfixed x, GLfixed y, GLfixed z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glRotatex__IIII(JNIEnv *env, jclass clazz, jint angle, jint x, jint y, jint z)
{
	glRotatex((GLfixed)angle, (GLfixed)x, (GLfixed)y, (GLfixed)z);
}

/* void glSampleCoverage ( GLclampf value, GLboolean invert ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glSampleCoverage__FZ(JNIEnv *env, jclass clazz, jfloat value, jboolean invert)
{
	glSampleCoverage((GLfloat)value, (GLboolean)invert);
}

/* void glSampleCoveragex ( GLclampx value, GLboolean invert ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glSampleCoveragex__IZ(JNIEnv *env, jclass clazz, jint value, jboolean invert)
{
	glSampleCoveragex((GLclampx)value, (GLboolean)invert);
}

/* void glScalef ( GLfloat x, GLfloat y, GLfloat z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glScalef__FFF(JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat z)
{
	glScalef((GLfloat)x, (GLfloat)y, (GLfloat)z);
}

/* void glScalex ( GLfixed x, GLfixed y, GLfixed z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glScalex__III(JNIEnv *env, jclass clazz, jint x, jint y, jint z)
{
	glScalex((GLfixed)x, (GLfixed)y, (GLfixed)z);
}

/* void glScissor ( GLint x, GLint y, GLsizei width, GLsizei height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glScissor__IIII(JNIEnv *env, jclass clazz, jint x, jint y, jint width, jint height)
{
	glScissor((GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height);
}

/* void glShadeModel ( GLenum mode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glShadeModel__I(JNIEnv *env, jclass clazz, jint mode)
{
	glShadeModel((GLenum)mode);
}

/* void glStencilFunc ( GLenum func, GLint ref, GLuint mask ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glStencilFunc__III(JNIEnv *env, jclass clazz, jint func, jint ref, jint mask)
{
	glStencilFunc((GLenum)func, (GLint)ref, (GLuint)mask);
}

/* void glStencilMask ( GLuint mask ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glStencilMask__I(JNIEnv *env, jclass clazz, jint mask)
{
	glStencilMask((GLuint)mask);
}

/* void glStencilOp ( GLenum fail, GLenum zfail, GLenum zpass ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glStencilOp__III(JNIEnv *env, jclass clazz, jint fail, jint zfail, jint zpass)
{
	glStencilOp((GLenum)fail, (GLenum)zfail, (GLenum)zpass);
}

/* void glTexCoordPointer ( GLint size, GLenum type, GLsizei stride, const GLvoid *pointer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTexCoordPointerBounds__IIILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint size, jint type, jint stride, jobject pointer_ref, jint remaining)
{
	jarray pointer_array = NULL;
	jbyte *pointer_elems = NULL;
	void *pointer = NULL;
	pointer = get_nio_buffer(env, pointer_ref, &pointer_array, &pointer_elems);
	glTexCoordPointer((GLint)size, (GLenum)type, (GLsizei)stride, (const void *)pointer);
	release_nio_buffer(env, pointer_array, pointer_elems);
}

/* void glTexEnvf ( GLenum target, GLenum pname, GLfloat param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTexEnvf__IIF(JNIEnv *env, jclass clazz, jint target, jint pname, jfloat param)
{
	glTexEnvf((GLenum)target, (GLenum)pname, (GLfloat)param);
}

/* void glTexEnvfv ( GLenum target, GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTexEnvfv__II_3FI(JNIEnv *env, jclass clazz, jint target, jint pname, jfloatArray params_ref, jint offset)
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
	glTexEnvfv((GLenum)target, (GLenum)pname, (const GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexEnvfv ( GLenum target, GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTexEnvfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexEnvfv((GLenum)target, (GLenum)pname, (const GLfloat *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexEnvx ( GLenum target, GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTexEnvx__III(JNIEnv *env, jclass clazz, jint target, jint pname, jint param)
{
	glTexEnvx((GLenum)target, (GLenum)pname, (GLfixed)param);
}

/* void glTexEnvxv ( GLenum target, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTexEnvxv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	glTexEnvxv((GLenum)target, (GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexEnvxv ( GLenum target, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTexEnvxv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexEnvxv((GLenum)target, (GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexImage2D ( GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid *pixels ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTexImage2D__IIIIIIIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint target, jint level, jint internalformat, jint width, jint height, jint border, jint format, jint type, jobject pixels_ref)
{
	jarray pixels_array = NULL;
	jbyte *pixels_elems = NULL;
	void *pixels = NULL;
	pixels = get_nio_buffer(env, pixels_ref, &pixels_array, &pixels_elems);
	glTexImage2D((GLenum)target, (GLint)level, (GLint)internalformat, (GLsizei)width, (GLsizei)height, (GLint)border, (GLenum)format, (GLenum)type, (const void *)pixels);
	release_nio_buffer(env, pixels_array, pixels_elems);
}

/* void glTexParameterf ( GLenum target, GLenum pname, GLfloat param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTexParameterf__IIF(JNIEnv *env, jclass clazz, jint target, jint pname, jfloat param)
{
	glTexParameterf((GLenum)target, (GLenum)pname, (GLfloat)param);
}

/* void glTexParameterx ( GLenum target, GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTexParameterx__III(JNIEnv *env, jclass clazz, jint target, jint pname, jint param)
{
	glTexParameterx((GLenum)target, (GLenum)pname, (GLfixed)param);
}

/* void glTexSubImage2D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid *pixels ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTexSubImage2D__IIIIIIIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint target, jint level, jint xoffset, jint yoffset, jint width, jint height, jint format, jint type, jobject pixels_ref)
{
	jarray pixels_array = NULL;
	jbyte *pixels_elems = NULL;
	void *pixels = NULL;
	pixels = get_nio_buffer(env, pixels_ref, &pixels_array, &pixels_elems);
	glTexSubImage2D((GLenum)target, (GLint)level, (GLint)xoffset, (GLint)yoffset, (GLsizei)width, (GLsizei)height, (GLenum)format, (GLenum)type, (const void *)pixels);
	release_nio_buffer(env, pixels_array, pixels_elems);
}

/* void glTranslatef ( GLfloat x, GLfloat y, GLfloat z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTranslatef__FFF(JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat z)
{
	glTranslatef((GLfloat)x, (GLfloat)y, (GLfloat)z);
}

/* void glTranslatex ( GLfixed x, GLfixed y, GLfixed z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glTranslatex__III(JNIEnv *env, jclass clazz, jint x, jint y, jint z)
{
	glTranslatex((GLfixed)x, (GLfixed)y, (GLfixed)z);
}

/* void glVertexPointer ( GLint size, GLenum type, GLsizei stride, const GLvoid *pointer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glVertexPointerBounds__IIILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint size, jint type, jint stride, jobject pointer_ref, jint remaining)
{
	jarray pointer_array = NULL;
	jbyte *pointer_elems = NULL;
	void *pointer = NULL;
	pointer = get_nio_buffer(env, pointer_ref, &pointer_array, &pointer_elems);
	glVertexPointer((GLint)size, (GLenum)type, (GLsizei)stride, (const void *)pointer);
	release_nio_buffer(env, pointer_array, pointer_elems);
}

/* void glViewport ( GLint x, GLint y, GLsizei width, GLsizei height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES10_glViewport__IIII(JNIEnv *env, jclass clazz, jint x, jint y, jint width, jint height)
{
	glViewport((GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height);
}

/* const GLubyte * glGetString ( GLenum name ) */
JNIEXPORT jstring JNICALL Java_android_opengl_GLES10_glGetString__I(JNIEnv *env, jclass clazz, jint name)
{
	const char *chars = (const char *)glGetString((GLenum)name);
	return chars ? (*env)->NewStringUTF(env, chars) : NULL;
}
