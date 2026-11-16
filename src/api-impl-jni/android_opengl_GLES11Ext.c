/* JNI of android.opengl.GLES11Ext. Generated from AOSP's GLES11Ext.java (its "// C function" comments and native
 * declarations, Android 16) and the Khronos GLES 1.x headers, the functions with other return types written by hand.
 * AOSP's own JNI (frameworks/base/core/jni/android_opengl_GLES11Ext.cpp) is generated as well and not in the local sources;
 * the argument checks follow its IllegalArgumentException messages for null arrays/buffers and negative offsets. */
#include "defines.h"
#include "util.h"
#include "android_opengl_GLES1.h"

#include "generated_headers/android_opengl_GLES11Ext.h"

/* void glBlendEquationSeparateOES ( GLenum modeRGB, GLenum modeAlpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glBlendEquationSeparateOES__II(JNIEnv *env, jclass clazz, jint modeRGB, jint modeAlpha)
{
	glBlendEquationSeparateOES((GLenum)modeRGB, (GLenum)modeAlpha);
}

/* void glBlendFuncSeparateOES ( GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glBlendFuncSeparateOES__IIII(JNIEnv *env, jclass clazz, jint srcRGB, jint dstRGB, jint srcAlpha, jint dstAlpha)
{
	glBlendFuncSeparateOES((GLenum)srcRGB, (GLenum)dstRGB, (GLenum)srcAlpha, (GLenum)dstAlpha);
}

/* void glBlendEquationOES ( GLenum mode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glBlendEquationOES__I(JNIEnv *env, jclass clazz, jint mode)
{
	glBlendEquationOES((GLenum)mode);
}

/* void glDrawTexsOES ( GLshort x, GLshort y, GLshort z, GLshort width, GLshort height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDrawTexsOES__SSSSS(JNIEnv *env, jclass clazz, jshort x, jshort y, jshort z, jshort width, jshort height)
{
	glDrawTexsOES((GLshort)x, (GLshort)y, (GLshort)z, (GLshort)width, (GLshort)height);
}

/* void glDrawTexiOES ( GLint x, GLint y, GLint z, GLint width, GLint height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDrawTexiOES__IIIII(JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint width, jint height)
{
	glDrawTexiOES((GLint)x, (GLint)y, (GLint)z, (GLint)width, (GLint)height);
}

/* void glDrawTexxOES ( GLfixed x, GLfixed y, GLfixed z, GLfixed width, GLfixed height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDrawTexxOES__IIIII(JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint width, jint height)
{
	glDrawTexxOES((GLfixed)x, (GLfixed)y, (GLfixed)z, (GLfixed)width, (GLfixed)height);
}

/* void glDrawTexsvOES ( const GLshort *coords ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDrawTexsvOES___3SI(JNIEnv *env, jclass clazz, jshortArray coords_ref, jint offset)
{
	jshort *coords = NULL;
	if (!coords_ref) {
		throw_iae(env, "coords == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, coords_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	coords = (*env)->GetShortArrayElements(env, coords_ref, NULL);
	glDrawTexsvOES((const GLshort *)(coords + offset));
exit:
	if (coords)
		(*env)->ReleaseShortArrayElements(env, coords_ref, coords, JNI_ABORT);
}

/* void glDrawTexsvOES ( const GLshort *coords ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDrawTexsvOES__Ljava_nio_ShortBuffer_2(JNIEnv *env, jclass clazz, jobject coords_ref)
{
	jarray coords_array = NULL;
	jbyte *coords_elems = NULL;
	void *coords = NULL;
	coords = get_nio_buffer(env, coords_ref, &coords_array, &coords_elems);
	glDrawTexsvOES((const GLshort *)coords);
	release_nio_buffer(env, coords_array, coords_elems);
}

/* void glDrawTexivOES ( const GLint *coords ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDrawTexivOES___3II(JNIEnv *env, jclass clazz, jintArray coords_ref, jint offset)
{
	jint *coords = NULL;
	if (!coords_ref) {
		throw_iae(env, "coords == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, coords_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	coords = (*env)->GetIntArrayElements(env, coords_ref, NULL);
	glDrawTexivOES((const GLint *)(coords + offset));
exit:
	if (coords)
		(*env)->ReleaseIntArrayElements(env, coords_ref, coords, JNI_ABORT);
}

/* void glDrawTexivOES ( const GLint *coords ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDrawTexivOES__Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jobject coords_ref)
{
	jarray coords_array = NULL;
	jbyte *coords_elems = NULL;
	void *coords = NULL;
	coords = get_nio_buffer(env, coords_ref, &coords_array, &coords_elems);
	glDrawTexivOES((const GLint *)coords);
	release_nio_buffer(env, coords_array, coords_elems);
}

/* void glDrawTexxvOES ( const GLfixed *coords ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDrawTexxvOES___3II(JNIEnv *env, jclass clazz, jintArray coords_ref, jint offset)
{
	jint *coords = NULL;
	if (!coords_ref) {
		throw_iae(env, "coords == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, coords_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	coords = (*env)->GetIntArrayElements(env, coords_ref, NULL);
	glDrawTexxvOES((const GLfixed *)(coords + offset));
exit:
	if (coords)
		(*env)->ReleaseIntArrayElements(env, coords_ref, coords, JNI_ABORT);
}

/* void glDrawTexxvOES ( const GLfixed *coords ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDrawTexxvOES__Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jobject coords_ref)
{
	jarray coords_array = NULL;
	jbyte *coords_elems = NULL;
	void *coords = NULL;
	coords = get_nio_buffer(env, coords_ref, &coords_array, &coords_elems);
	glDrawTexxvOES((const GLfixed *)coords);
	release_nio_buffer(env, coords_array, coords_elems);
}

/* void glDrawTexfOES ( GLfloat x, GLfloat y, GLfloat z, GLfloat width, GLfloat height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDrawTexfOES__FFFFF(JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat z, jfloat width, jfloat height)
{
	glDrawTexfOES((GLfloat)x, (GLfloat)y, (GLfloat)z, (GLfloat)width, (GLfloat)height);
}

/* void glDrawTexfvOES ( const GLfloat *coords ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDrawTexfvOES___3FI(JNIEnv *env, jclass clazz, jfloatArray coords_ref, jint offset)
{
	jfloat *coords = NULL;
	if (!coords_ref) {
		throw_iae(env, "coords == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, coords_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	coords = (*env)->GetFloatArrayElements(env, coords_ref, NULL);
	glDrawTexfvOES((const GLfloat *)(coords + offset));
exit:
	if (coords)
		(*env)->ReleaseFloatArrayElements(env, coords_ref, coords, JNI_ABORT);
}

/* void glDrawTexfvOES ( const GLfloat *coords ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDrawTexfvOES__Ljava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jobject coords_ref)
{
	jarray coords_array = NULL;
	jbyte *coords_elems = NULL;
	void *coords = NULL;
	coords = get_nio_buffer(env, coords_ref, &coords_array, &coords_elems);
	glDrawTexfvOES((const GLfloat *)coords);
	release_nio_buffer(env, coords_array, coords_elems);
}

/* void glAlphaFuncxOES ( GLenum func, GLclampx ref ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glAlphaFuncxOES__II(JNIEnv *env, jclass clazz, jint func, jint ref)
{
	glAlphaFuncxOES((GLenum)func, (GLfixed)ref);
}

/* void glClearColorxOES ( GLclampx red, GLclampx green, GLclampx blue, GLclampx alpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glClearColorxOES__IIII(JNIEnv *env, jclass clazz, jint red, jint green, jint blue, jint alpha)
{
	glClearColorxOES((GLfixed)red, (GLfixed)green, (GLfixed)blue, (GLfixed)alpha);
}

/* void glClearDepthxOES ( GLclampx depth ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glClearDepthxOES__I(JNIEnv *env, jclass clazz, jint depth)
{
	glClearDepthxOES((GLfixed)depth);
}

/* void glClipPlanexOES ( GLenum plane, const GLfixed *equation ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glClipPlanexOES__I_3II(JNIEnv *env, jclass clazz, jint plane, jintArray equation_ref, jint offset)
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
	glClipPlanexOES((GLenum)plane, (const GLfixed *)(equation + offset));
exit:
	if (equation)
		(*env)->ReleaseIntArrayElements(env, equation_ref, equation, JNI_ABORT);
}

/* void glClipPlanexOES ( GLenum plane, const GLfixed *equation ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glClipPlanexOES__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint plane, jobject equation_ref)
{
	jarray equation_array = NULL;
	jbyte *equation_elems = NULL;
	void *equation = NULL;
	equation = get_nio_buffer(env, equation_ref, &equation_array, &equation_elems);
	glClipPlanexOES((GLenum)plane, (const GLfixed *)equation);
	release_nio_buffer(env, equation_array, equation_elems);
}

/* void glColor4xOES ( GLfixed red, GLfixed green, GLfixed blue, GLfixed alpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glColor4xOES__IIII(JNIEnv *env, jclass clazz, jint red, jint green, jint blue, jint alpha)
{
	glColor4xOES((GLfixed)red, (GLfixed)green, (GLfixed)blue, (GLfixed)alpha);
}

/* void glDepthRangexOES ( GLclampx zNear, GLclampx zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDepthRangexOES__II(JNIEnv *env, jclass clazz, jint zNear, jint zFar)
{
	glDepthRangexOES((GLfixed)zNear, (GLfixed)zFar);
}

/* void glFogxOES ( GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glFogxOES__II(JNIEnv *env, jclass clazz, jint pname, jint param)
{
	glFogxOES((GLenum)pname, (GLfixed)param);
}

/* void glFogxvOES ( GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glFogxvOES__I_3II(JNIEnv *env, jclass clazz, jint pname, jintArray params_ref, jint offset)
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
	glFogxvOES((GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glFogxvOES ( GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glFogxvOES__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glFogxvOES((GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glFrustumxOES ( GLfixed left, GLfixed right, GLfixed bottom, GLfixed top, GLfixed zNear, GLfixed zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glFrustumxOES__IIIIII(JNIEnv *env, jclass clazz, jint left, jint right, jint bottom, jint top, jint zNear, jint zFar)
{
	glFrustumxOES((GLfixed)left, (GLfixed)right, (GLfixed)bottom, (GLfixed)top, (GLfixed)zNear, (GLfixed)zFar);
}

/* void glGetClipPlanexOES ( GLenum pname, GLfixed *eqn ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetClipPlanexOES__I_3II(JNIEnv *env, jclass clazz, jint pname, jintArray eqn_ref, jint offset)
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
	glGetClipPlanexOES((GLenum)pname, (GLfixed *)(eqn + offset));
exit:
	if (eqn)
		(*env)->ReleaseIntArrayElements(env, eqn_ref, eqn, 0);
}

/* void glGetClipPlanexOES ( GLenum pname, GLfixed *eqn ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetClipPlanexOES__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject eqn_ref)
{
	jarray eqn_array = NULL;
	jbyte *eqn_elems = NULL;
	void *eqn = NULL;
	if (!eqn_ref) {
		throw_iae(env, "eqn == null");
		goto exit;
	}
	eqn = get_nio_buffer(env, eqn_ref, &eqn_array, &eqn_elems);
	glGetClipPlanexOES((GLenum)pname, (GLfixed *)eqn);
exit:
	release_nio_buffer(env, eqn_array, eqn_elems);
}

/* void glGetFixedvOES ( GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetFixedvOES__I_3II(JNIEnv *env, jclass clazz, jint pname, jintArray params_ref, jint offset)
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
	glGetFixedvOES((GLenum)pname, (GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetFixedvOES ( GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetFixedvOES__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetFixedvOES((GLenum)pname, (GLfixed *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetLightxvOES ( GLenum light, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetLightxvOES__II_3II(JNIEnv *env, jclass clazz, jint light, jint pname, jintArray params_ref, jint offset)
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
	glGetLightxvOES((GLenum)light, (GLenum)pname, (GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetLightxvOES ( GLenum light, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetLightxvOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint light, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetLightxvOES((GLenum)light, (GLenum)pname, (GLfixed *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetMaterialxvOES ( GLenum face, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetMaterialxvOES__II_3II(JNIEnv *env, jclass clazz, jint face, jint pname, jintArray params_ref, jint offset)
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
	glGetMaterialxvOES((GLenum)face, (GLenum)pname, (GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetMaterialxvOES ( GLenum face, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetMaterialxvOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint face, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetMaterialxvOES((GLenum)face, (GLenum)pname, (GLfixed *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexEnvxvOES ( GLenum env, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetTexEnvxvOES__II_3II(JNIEnv *env, jclass clazz, jint env_enum, jint pname, jintArray params_ref, jint offset)
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
	glGetTexEnvxvOES((GLenum)env_enum, (GLenum)pname, (GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexEnvxvOES ( GLenum env, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetTexEnvxvOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint env_enum, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexEnvxvOES((GLenum)env_enum, (GLenum)pname, (GLfixed *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexParameterxvOES ( GLenum target, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetTexParameterxvOES__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	glGetTexParameterxvOES((GLenum)target, (GLenum)pname, (GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexParameterxvOES ( GLenum target, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetTexParameterxvOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexParameterxvOES((GLenum)target, (GLenum)pname, (GLfixed *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glLightModelxOES ( GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glLightModelxOES__II(JNIEnv *env, jclass clazz, jint pname, jint param)
{
	glLightModelxOES((GLenum)pname, (GLfixed)param);
}

/* void glLightModelxvOES ( GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glLightModelxvOES__I_3II(JNIEnv *env, jclass clazz, jint pname, jintArray params_ref, jint offset)
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
	glLightModelxvOES((GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glLightModelxvOES ( GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glLightModelxvOES__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glLightModelxvOES((GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glLightxOES ( GLenum light, GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glLightxOES__III(JNIEnv *env, jclass clazz, jint light, jint pname, jint param)
{
	glLightxOES((GLenum)light, (GLenum)pname, (GLfixed)param);
}

/* void glLightxvOES ( GLenum light, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glLightxvOES__II_3II(JNIEnv *env, jclass clazz, jint light, jint pname, jintArray params_ref, jint offset)
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
	glLightxvOES((GLenum)light, (GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glLightxvOES ( GLenum light, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glLightxvOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint light, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glLightxvOES((GLenum)light, (GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glLineWidthxOES ( GLfixed width ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glLineWidthxOES__I(JNIEnv *env, jclass clazz, jint width)
{
	glLineWidthxOES((GLfixed)width);
}

/* void glLoadMatrixxOES ( const GLfixed *m ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glLoadMatrixxOES___3II(JNIEnv *env, jclass clazz, jintArray m_ref, jint offset)
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
	glLoadMatrixxOES((const GLfixed *)(m + offset));
exit:
	if (m)
		(*env)->ReleaseIntArrayElements(env, m_ref, m, JNI_ABORT);
}

/* void glLoadMatrixxOES ( const GLfixed *m ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glLoadMatrixxOES__Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jobject m_ref)
{
	jarray m_array = NULL;
	jbyte *m_elems = NULL;
	void *m = NULL;
	m = get_nio_buffer(env, m_ref, &m_array, &m_elems);
	glLoadMatrixxOES((const GLfixed *)m);
	release_nio_buffer(env, m_array, m_elems);
}

/* void glMaterialxOES ( GLenum face, GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glMaterialxOES__III(JNIEnv *env, jclass clazz, jint face, jint pname, jint param)
{
	glMaterialxOES((GLenum)face, (GLenum)pname, (GLfixed)param);
}

/* void glMaterialxvOES ( GLenum face, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glMaterialxvOES__II_3II(JNIEnv *env, jclass clazz, jint face, jint pname, jintArray params_ref, jint offset)
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
	glMaterialxvOES((GLenum)face, (GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glMaterialxvOES ( GLenum face, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glMaterialxvOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint face, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glMaterialxvOES((GLenum)face, (GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glMultMatrixxOES ( const GLfixed *m ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glMultMatrixxOES___3II(JNIEnv *env, jclass clazz, jintArray m_ref, jint offset)
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
	glMultMatrixxOES((const GLfixed *)(m + offset));
exit:
	if (m)
		(*env)->ReleaseIntArrayElements(env, m_ref, m, JNI_ABORT);
}

/* void glMultMatrixxOES ( const GLfixed *m ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glMultMatrixxOES__Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jobject m_ref)
{
	jarray m_array = NULL;
	jbyte *m_elems = NULL;
	void *m = NULL;
	m = get_nio_buffer(env, m_ref, &m_array, &m_elems);
	glMultMatrixxOES((const GLfixed *)m);
	release_nio_buffer(env, m_array, m_elems);
}

/* void glMultiTexCoord4xOES ( GLenum target, GLfixed s, GLfixed t, GLfixed r, GLfixed q ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glMultiTexCoord4xOES__IIIII(JNIEnv *env, jclass clazz, jint target, jint s, jint t, jint r, jint q)
{
	glMultiTexCoord4xOES((GLenum)target, (GLfixed)s, (GLfixed)t, (GLfixed)r, (GLfixed)q);
}

/* void glNormal3xOES ( GLfixed nx, GLfixed ny, GLfixed nz ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glNormal3xOES__III(JNIEnv *env, jclass clazz, jint nx, jint ny, jint nz)
{
	glNormal3xOES((GLfixed)nx, (GLfixed)ny, (GLfixed)nz);
}

/* void glOrthoxOES ( GLfixed left, GLfixed right, GLfixed bottom, GLfixed top, GLfixed zNear, GLfixed zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glOrthoxOES__IIIIII(JNIEnv *env, jclass clazz, jint left, jint right, jint bottom, jint top, jint zNear, jint zFar)
{
	glOrthoxOES((GLfixed)left, (GLfixed)right, (GLfixed)bottom, (GLfixed)top, (GLfixed)zNear, (GLfixed)zFar);
}

/* void glPointParameterxOES ( GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glPointParameterxOES__II(JNIEnv *env, jclass clazz, jint pname, jint param)
{
	glPointParameterxOES((GLenum)pname, (GLfixed)param);
}

/* void glPointParameterxvOES ( GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glPointParameterxvOES__I_3II(JNIEnv *env, jclass clazz, jint pname, jintArray params_ref, jint offset)
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
	glPointParameterxvOES((GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glPointParameterxvOES ( GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glPointParameterxvOES__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glPointParameterxvOES((GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glPointSizexOES ( GLfixed size ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glPointSizexOES__I(JNIEnv *env, jclass clazz, jint size)
{
	glPointSizexOES((GLfixed)size);
}

/* void glPolygonOffsetxOES ( GLfixed factor, GLfixed units ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glPolygonOffsetxOES__II(JNIEnv *env, jclass clazz, jint factor, jint units)
{
	glPolygonOffsetxOES((GLfixed)factor, (GLfixed)units);
}

/* void glRotatexOES ( GLfixed angle, GLfixed x, GLfixed y, GLfixed z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glRotatexOES__IIII(JNIEnv *env, jclass clazz, jint angle, jint x, jint y, jint z)
{
	glRotatexOES((GLfixed)angle, (GLfixed)x, (GLfixed)y, (GLfixed)z);
}

/* void glSampleCoveragexOES ( GLclampx value, GLboolean invert ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glSampleCoveragexOES__IZ(JNIEnv *env, jclass clazz, jint value, jboolean invert)
{
	glSampleCoveragexOES((GLclampx)value, (GLboolean)invert);
}

/* void glScalexOES ( GLfixed x, GLfixed y, GLfixed z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glScalexOES__III(JNIEnv *env, jclass clazz, jint x, jint y, jint z)
{
	glScalexOES((GLfixed)x, (GLfixed)y, (GLfixed)z);
}

/* void glTexEnvxOES ( GLenum target, GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexEnvxOES__III(JNIEnv *env, jclass clazz, jint target, jint pname, jint param)
{
	glTexEnvxOES((GLenum)target, (GLenum)pname, (GLfixed)param);
}

/* void glTexEnvxvOES ( GLenum target, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexEnvxvOES__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	glTexEnvxvOES((GLenum)target, (GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexEnvxvOES ( GLenum target, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexEnvxvOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexEnvxvOES((GLenum)target, (GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexParameterxOES ( GLenum target, GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexParameterxOES__III(JNIEnv *env, jclass clazz, jint target, jint pname, jint param)
{
	glTexParameterxOES((GLenum)target, (GLenum)pname, (GLfixed)param);
}

/* void glTexParameterxvOES ( GLenum target, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexParameterxvOES__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	glTexParameterxvOES((GLenum)target, (GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexParameterxvOES ( GLenum target, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexParameterxvOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexParameterxvOES((GLenum)target, (GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTranslatexOES ( GLfixed x, GLfixed y, GLfixed z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTranslatexOES__III(JNIEnv *env, jclass clazz, jint x, jint y, jint z)
{
	glTranslatexOES((GLfixed)x, (GLfixed)y, (GLfixed)z);
}

/* GLboolean glIsRenderbufferOES ( GLuint renderbuffer ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES11Ext_glIsRenderbufferOES__I(JNIEnv *env, jclass clazz, jint renderbuffer)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsRenderbufferOES((GLuint)renderbuffer);
	return _ret;
}

/* void glBindRenderbufferOES ( GLenum target, GLuint renderbuffer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glBindRenderbufferOES__II(JNIEnv *env, jclass clazz, jint target, jint renderbuffer)
{
	glBindRenderbufferOES((GLenum)target, (GLuint)renderbuffer);
}

/* void glDeleteRenderbuffersOES ( GLsizei n, const GLuint *renderbuffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDeleteRenderbuffersOES__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray renderbuffers_ref, jint offset)
{
	jint *renderbuffers = NULL;
	if (!renderbuffers_ref) {
		throw_iae(env, "renderbuffers == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, renderbuffers_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	renderbuffers = (*env)->GetIntArrayElements(env, renderbuffers_ref, NULL);
	glDeleteRenderbuffersOES((GLsizei)n, (const GLuint *)(renderbuffers + offset));
exit:
	if (renderbuffers)
		(*env)->ReleaseIntArrayElements(env, renderbuffers_ref, renderbuffers, JNI_ABORT);
}

/* void glDeleteRenderbuffersOES ( GLsizei n, const GLuint *renderbuffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDeleteRenderbuffersOES__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject renderbuffers_ref)
{
	jarray renderbuffers_array = NULL;
	jbyte *renderbuffers_elems = NULL;
	void *renderbuffers = NULL;
	renderbuffers = get_nio_buffer(env, renderbuffers_ref, &renderbuffers_array, &renderbuffers_elems);
	glDeleteRenderbuffersOES((GLsizei)n, (const GLuint *)renderbuffers);
	release_nio_buffer(env, renderbuffers_array, renderbuffers_elems);
}

/* void glGenRenderbuffersOES ( GLsizei n, GLuint *renderbuffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGenRenderbuffersOES__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray renderbuffers_ref, jint offset)
{
	jint *renderbuffers = NULL;
	if (!renderbuffers_ref) {
		throw_iae(env, "renderbuffers == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, renderbuffers_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	renderbuffers = (*env)->GetIntArrayElements(env, renderbuffers_ref, NULL);
	glGenRenderbuffersOES((GLsizei)n, (GLuint *)(renderbuffers + offset));
exit:
	if (renderbuffers)
		(*env)->ReleaseIntArrayElements(env, renderbuffers_ref, renderbuffers, 0);
}

/* void glGenRenderbuffersOES ( GLsizei n, GLuint *renderbuffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGenRenderbuffersOES__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject renderbuffers_ref)
{
	jarray renderbuffers_array = NULL;
	jbyte *renderbuffers_elems = NULL;
	void *renderbuffers = NULL;
	if (!renderbuffers_ref) {
		throw_iae(env, "renderbuffers == null");
		goto exit;
	}
	renderbuffers = get_nio_buffer(env, renderbuffers_ref, &renderbuffers_array, &renderbuffers_elems);
	glGenRenderbuffersOES((GLsizei)n, (GLuint *)renderbuffers);
exit:
	release_nio_buffer(env, renderbuffers_array, renderbuffers_elems);
}

/* void glRenderbufferStorageOES ( GLenum target, GLenum internalformat, GLsizei width, GLsizei height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glRenderbufferStorageOES__IIII(JNIEnv *env, jclass clazz, jint target, jint internalformat, jint width, jint height)
{
	glRenderbufferStorageOES((GLenum)target, (GLenum)internalformat, (GLsizei)width, (GLsizei)height);
}

/* void glGetRenderbufferParameterivOES ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetRenderbufferParameterivOES__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	glGetRenderbufferParameterivOES((GLenum)target, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetRenderbufferParameterivOES ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetRenderbufferParameterivOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetRenderbufferParameterivOES((GLenum)target, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* GLboolean glIsFramebufferOES ( GLuint framebuffer ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES11Ext_glIsFramebufferOES__I(JNIEnv *env, jclass clazz, jint framebuffer)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsFramebufferOES((GLuint)framebuffer);
	return _ret;
}

/* void glBindFramebufferOES ( GLenum target, GLuint framebuffer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glBindFramebufferOES__II(JNIEnv *env, jclass clazz, jint target, jint framebuffer)
{
	glBindFramebufferOES((GLenum)target, (GLuint)framebuffer);
}

/* void glDeleteFramebuffersOES ( GLsizei n, const GLuint *framebuffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDeleteFramebuffersOES__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray framebuffers_ref, jint offset)
{
	jint *framebuffers = NULL;
	if (!framebuffers_ref) {
		throw_iae(env, "framebuffers == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, framebuffers_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	framebuffers = (*env)->GetIntArrayElements(env, framebuffers_ref, NULL);
	glDeleteFramebuffersOES((GLsizei)n, (const GLuint *)(framebuffers + offset));
exit:
	if (framebuffers)
		(*env)->ReleaseIntArrayElements(env, framebuffers_ref, framebuffers, JNI_ABORT);
}

/* void glDeleteFramebuffersOES ( GLsizei n, const GLuint *framebuffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDeleteFramebuffersOES__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject framebuffers_ref)
{
	jarray framebuffers_array = NULL;
	jbyte *framebuffers_elems = NULL;
	void *framebuffers = NULL;
	framebuffers = get_nio_buffer(env, framebuffers_ref, &framebuffers_array, &framebuffers_elems);
	glDeleteFramebuffersOES((GLsizei)n, (const GLuint *)framebuffers);
	release_nio_buffer(env, framebuffers_array, framebuffers_elems);
}

/* void glGenFramebuffersOES ( GLsizei n, GLuint *framebuffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGenFramebuffersOES__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray framebuffers_ref, jint offset)
{
	jint *framebuffers = NULL;
	if (!framebuffers_ref) {
		throw_iae(env, "framebuffers == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, framebuffers_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	framebuffers = (*env)->GetIntArrayElements(env, framebuffers_ref, NULL);
	glGenFramebuffersOES((GLsizei)n, (GLuint *)(framebuffers + offset));
exit:
	if (framebuffers)
		(*env)->ReleaseIntArrayElements(env, framebuffers_ref, framebuffers, 0);
}

/* void glGenFramebuffersOES ( GLsizei n, GLuint *framebuffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGenFramebuffersOES__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject framebuffers_ref)
{
	jarray framebuffers_array = NULL;
	jbyte *framebuffers_elems = NULL;
	void *framebuffers = NULL;
	if (!framebuffers_ref) {
		throw_iae(env, "framebuffers == null");
		goto exit;
	}
	framebuffers = get_nio_buffer(env, framebuffers_ref, &framebuffers_array, &framebuffers_elems);
	glGenFramebuffersOES((GLsizei)n, (GLuint *)framebuffers);
exit:
	release_nio_buffer(env, framebuffers_array, framebuffers_elems);
}

/* GLenum glCheckFramebufferStatusOES ( GLenum target ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES11Ext_glCheckFramebufferStatusOES__I(JNIEnv *env, jclass clazz, jint target)
{
	jint _ret = 0;
	_ret = (jint)glCheckFramebufferStatusOES((GLenum)target);
	return _ret;
}

/* void glFramebufferRenderbufferOES ( GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glFramebufferRenderbufferOES__IIII(JNIEnv *env, jclass clazz, jint target, jint attachment, jint renderbuffertarget, jint renderbuffer)
{
	glFramebufferRenderbufferOES((GLenum)target, (GLenum)attachment, (GLenum)renderbuffertarget, (GLuint)renderbuffer);
}

/* void glFramebufferTexture2DOES ( GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glFramebufferTexture2DOES__IIIII(JNIEnv *env, jclass clazz, jint target, jint attachment, jint textarget, jint texture, jint level)
{
	glFramebufferTexture2DOES((GLenum)target, (GLenum)attachment, (GLenum)textarget, (GLuint)texture, (GLint)level);
}

/* void glGetFramebufferAttachmentParameterivOES ( GLenum target, GLenum attachment, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetFramebufferAttachmentParameterivOES__III_3II(JNIEnv *env, jclass clazz, jint target, jint attachment, jint pname, jintArray params_ref, jint offset)
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
	glGetFramebufferAttachmentParameterivOES((GLenum)target, (GLenum)attachment, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetFramebufferAttachmentParameterivOES ( GLenum target, GLenum attachment, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetFramebufferAttachmentParameterivOES__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint attachment, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetFramebufferAttachmentParameterivOES((GLenum)target, (GLenum)attachment, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGenerateMipmapOES ( GLenum target ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGenerateMipmapOES__I(JNIEnv *env, jclass clazz, jint target)
{
	glGenerateMipmapOES((GLenum)target);
}

/* void glCurrentPaletteMatrixOES ( GLuint matrixpaletteindex ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glCurrentPaletteMatrixOES__I(JNIEnv *env, jclass clazz, jint matrixpaletteindex)
{
	glCurrentPaletteMatrixOES((GLuint)matrixpaletteindex);
}

/* void glLoadPaletteFromModelViewMatrixOES ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glLoadPaletteFromModelViewMatrixOES__(JNIEnv *env, jclass clazz)
{
	glLoadPaletteFromModelViewMatrixOES();
}

/* void glMatrixIndexPointerOES ( GLint size, GLenum type, GLsizei stride, const GLvoid *pointer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glMatrixIndexPointerOESBounds__IIILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint size, jint type, jint stride, jobject pointer_ref, jint remaining)
{
	jarray pointer_array = NULL;
	jbyte *pointer_elems = NULL;
	void *pointer = NULL;
	pointer = get_nio_buffer(env, pointer_ref, &pointer_array, &pointer_elems);
	glMatrixIndexPointerOES((GLint)size, (GLenum)type, (GLsizei)stride, (const void *)pointer);
	release_nio_buffer(env, pointer_array, pointer_elems);
}

/* void glWeightPointerOES ( GLint size, GLenum type, GLsizei stride, const GLvoid *pointer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glWeightPointerOESBounds__IIILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint size, jint type, jint stride, jobject pointer_ref, jint remaining)
{
	jarray pointer_array = NULL;
	jbyte *pointer_elems = NULL;
	void *pointer = NULL;
	pointer = get_nio_buffer(env, pointer_ref, &pointer_array, &pointer_elems);
	glWeightPointerOES((GLint)size, (GLenum)type, (GLsizei)stride, (const void *)pointer);
	release_nio_buffer(env, pointer_array, pointer_elems);
}

/* void glDepthRangefOES ( GLclampf zNear, GLclampf zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glDepthRangefOES__FF(JNIEnv *env, jclass clazz, jfloat zNear, jfloat zFar)
{
	glDepthRangefOES((GLclampf)zNear, (GLclampf)zFar);
}

/* void glFrustumfOES ( GLfloat left, GLfloat right, GLfloat bottom, GLfloat top, GLfloat zNear, GLfloat zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glFrustumfOES__FFFFFF(JNIEnv *env, jclass clazz, jfloat left, jfloat right, jfloat bottom, jfloat top, jfloat zNear, jfloat zFar)
{
	glFrustumfOES((GLfloat)left, (GLfloat)right, (GLfloat)bottom, (GLfloat)top, (GLfloat)zNear, (GLfloat)zFar);
}

/* void glOrthofOES ( GLfloat left, GLfloat right, GLfloat bottom, GLfloat top, GLfloat zNear, GLfloat zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glOrthofOES__FFFFFF(JNIEnv *env, jclass clazz, jfloat left, jfloat right, jfloat bottom, jfloat top, jfloat zNear, jfloat zFar)
{
	glOrthofOES((GLfloat)left, (GLfloat)right, (GLfloat)bottom, (GLfloat)top, (GLfloat)zNear, (GLfloat)zFar);
}

/* void glClipPlanefOES ( GLenum plane, const GLfloat *equation ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glClipPlanefOES__I_3FI(JNIEnv *env, jclass clazz, jint plane, jfloatArray equation_ref, jint offset)
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
	glClipPlanefOES((GLenum)plane, (const GLfloat *)(equation + offset));
exit:
	if (equation)
		(*env)->ReleaseFloatArrayElements(env, equation_ref, equation, JNI_ABORT);
}

/* void glClipPlanefOES ( GLenum plane, const GLfloat *equation ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glClipPlanefOES__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint plane, jobject equation_ref)
{
	jarray equation_array = NULL;
	jbyte *equation_elems = NULL;
	void *equation = NULL;
	equation = get_nio_buffer(env, equation_ref, &equation_array, &equation_elems);
	glClipPlanefOES((GLenum)plane, (const GLfloat *)equation);
	release_nio_buffer(env, equation_array, equation_elems);
}

/* void glGetClipPlanefOES ( GLenum pname, GLfloat *eqn ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetClipPlanefOES__I_3FI(JNIEnv *env, jclass clazz, jint pname, jfloatArray eqn_ref, jint offset)
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
	glGetClipPlanefOES((GLenum)pname, (GLfloat *)(eqn + offset));
exit:
	if (eqn)
		(*env)->ReleaseFloatArrayElements(env, eqn_ref, eqn, 0);
}

/* void glGetClipPlanefOES ( GLenum pname, GLfloat *eqn ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetClipPlanefOES__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject eqn_ref)
{
	jarray eqn_array = NULL;
	jbyte *eqn_elems = NULL;
	void *eqn = NULL;
	if (!eqn_ref) {
		throw_iae(env, "eqn == null");
		goto exit;
	}
	eqn = get_nio_buffer(env, eqn_ref, &eqn_array, &eqn_elems);
	glGetClipPlanefOES((GLenum)pname, (GLfloat *)eqn);
exit:
	release_nio_buffer(env, eqn_array, eqn_elems);
}

/* void glClearDepthfOES ( GLclampf depth ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glClearDepthfOES__F(JNIEnv *env, jclass clazz, jfloat depth)
{
	glClearDepthfOES((GLclampf)depth);
}

/* void glTexGenfOES ( GLenum coord, GLenum pname, GLfloat param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexGenfOES__IIF(JNIEnv *env, jclass clazz, jint coord, jint pname, jfloat param)
{
	glTexGenfOES((GLenum)coord, (GLenum)pname, (GLfloat)param);
}

/* void glTexGenfvOES ( GLenum coord, GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexGenfvOES__II_3FI(JNIEnv *env, jclass clazz, jint coord, jint pname, jfloatArray params_ref, jint offset)
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
	glTexGenfvOES((GLenum)coord, (GLenum)pname, (const GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexGenfvOES ( GLenum coord, GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexGenfvOES__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint coord, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexGenfvOES((GLenum)coord, (GLenum)pname, (const GLfloat *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexGeniOES ( GLenum coord, GLenum pname, GLint param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexGeniOES__III(JNIEnv *env, jclass clazz, jint coord, jint pname, jint param)
{
	glTexGeniOES((GLenum)coord, (GLenum)pname, (GLint)param);
}

/* void glTexGenivOES ( GLenum coord, GLenum pname, const GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexGenivOES__II_3II(JNIEnv *env, jclass clazz, jint coord, jint pname, jintArray params_ref, jint offset)
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
	glTexGenivOES((GLenum)coord, (GLenum)pname, (const GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexGenivOES ( GLenum coord, GLenum pname, const GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexGenivOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint coord, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexGenivOES((GLenum)coord, (GLenum)pname, (const GLint *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexGenxOES ( GLenum coord, GLenum pname, GLfixed param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexGenxOES__III(JNIEnv *env, jclass clazz, jint coord, jint pname, jint param)
{
	glTexGenxOES((GLenum)coord, (GLenum)pname, (GLfixed)param);
}

/* void glTexGenxvOES ( GLenum coord, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexGenxvOES__II_3II(JNIEnv *env, jclass clazz, jint coord, jint pname, jintArray params_ref, jint offset)
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
	glTexGenxvOES((GLenum)coord, (GLenum)pname, (const GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexGenxvOES ( GLenum coord, GLenum pname, const GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glTexGenxvOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint coord, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexGenxvOES((GLenum)coord, (GLenum)pname, (const GLfixed *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexGenfvOES ( GLenum coord, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetTexGenfvOES__II_3FI(JNIEnv *env, jclass clazz, jint coord, jint pname, jfloatArray params_ref, jint offset)
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
	glGetTexGenfvOES((GLenum)coord, (GLenum)pname, (GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, 0);
}

/* void glGetTexGenfvOES ( GLenum coord, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetTexGenfvOES__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint coord, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexGenfvOES((GLenum)coord, (GLenum)pname, (GLfloat *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexGenivOES ( GLenum coord, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetTexGenivOES__II_3II(JNIEnv *env, jclass clazz, jint coord, jint pname, jintArray params_ref, jint offset)
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
	glGetTexGenivOES((GLenum)coord, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexGenivOES ( GLenum coord, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetTexGenivOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint coord, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexGenivOES((GLenum)coord, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexGenxvOES ( GLenum coord, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetTexGenxvOES__II_3II(JNIEnv *env, jclass clazz, jint coord, jint pname, jintArray params_ref, jint offset)
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
	glGetTexGenxvOES((GLenum)coord, (GLenum)pname, (GLfixed *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexGenxvOES ( GLenum coord, GLenum pname, GLfixed *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glGetTexGenxvOES__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint coord, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexGenxvOES((GLenum)coord, (GLenum)pname, (GLfixed *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glEGLImageTargetTexture2DOES ( GLenum target, GLeglImageOES image ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glEGLImageTargetTexture2DOES__ILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint target, jobject image_ref)
{
	jarray image_array = NULL;
	jbyte *image_elems = NULL;
	void *image = get_nio_buffer(env, image_ref, &image_array, &image_elems);
	glEGLImageTargetTexture2DOES((GLenum)target, (GLeglImageOES)image);
	release_nio_buffer(env, image_array, image_elems);
}
/* void glEGLImageTargetRenderbufferStorageOES ( GLenum target, GLeglImageOES image ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES11Ext_glEGLImageTargetRenderbufferStorageOES__ILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint target, jobject image_ref)
{
	jarray image_array = NULL;
	jbyte *image_elems = NULL;
	void *image = get_nio_buffer(env, image_ref, &image_array, &image_elems);
	glEGLImageTargetRenderbufferStorageOES((GLenum)target, (GLeglImageOES)image);
	release_nio_buffer(env, image_array, image_elems);
}
