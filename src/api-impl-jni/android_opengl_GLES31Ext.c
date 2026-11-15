/* JNI of android.opengl.GLES31Ext. Generated from AOSP's GLES31Ext.java (its "// C function" comments and native
 * declarations, Android 16) and the Khronos GLES headers, the string and debug functions written by hand. AOSP's
 * own JNI (frameworks/base/core/jni/android_opengl_GLES31Ext.cpp) is generated as well and not in the local sources;
 * the argument checks follow its IllegalArgumentException messages for null arrays/buffers and negative offsets. */
#include "defines.h"
#include "util.h"
#include "android_opengl_GLES3.h"

#include "generated_headers/android_opengl_GLES31Ext.h"

/* void glBlendBarrierKHR ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glBlendBarrierKHR__(JNIEnv *env, jclass clazz)
{
	GL_EXT_CALL(glBlendBarrierKHR, PFNGLBLENDBARRIERKHRPROC)();
}

/* void glDebugMessageControlKHR ( GLenum source, GLenum type, GLenum severity, GLsizei count, const GLuint *ids, GLboolean enabled ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glDebugMessageControlKHR__IIII_3IIZ(JNIEnv *env, jclass clazz, jint source, jint type, jint severity, jint count, jintArray ids_ref, jint offset, jboolean enabled)
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
	if ((*env)->GetArrayLength(env, ids_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	ids = (*env)->GetIntArrayElements(env, ids_ref, NULL);
	GL_EXT_CALL(glDebugMessageControlKHR, PFNGLDEBUGMESSAGECONTROLKHRPROC)((GLenum)source, (GLenum)type, (GLenum)severity, (GLsizei)count, (const GLuint *)(ids + offset), (GLboolean)enabled);
exit:
	if (ids)
		(*env)->ReleaseIntArrayElements(env, ids_ref, ids, JNI_ABORT);
}

/* void glDebugMessageControlKHR ( GLenum source, GLenum type, GLenum severity, GLsizei count, const GLuint *ids, GLboolean enabled ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glDebugMessageControlKHR__IIIILjava_nio_IntBuffer_2Z(JNIEnv *env, jclass clazz, jint source, jint type, jint severity, jint count, jobject ids_ref, jboolean enabled)
{
	jarray ids_array = NULL;
	jbyte *ids_elems = NULL;
	void *ids = NULL;
	ids = get_nio_buffer(env, ids_ref, &ids_array, &ids_elems);
	GL_EXT_CALL(glDebugMessageControlKHR, PFNGLDEBUGMESSAGECONTROLKHRPROC)((GLenum)source, (GLenum)type, (GLenum)severity, (GLsizei)count, (const GLuint *)ids, (GLboolean)enabled);
	release_nio_buffer(env, ids_array, ids_elems);
}

/* GLuint glGetDebugMessageLogKHR ( GLuint count, GLsizei bufSize, GLenum *sources, GLenum *types, GLuint *ids, GLenum *severities, GLsizei *lengths, GLchar *messageLog ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES31Ext_glGetDebugMessageLogKHR__II_3II_3II_3II_3II_3II_3BI(JNIEnv *env, jclass clazz, jint count, jint bufSize, jintArray sources_ref, jint sourcesOffset, jintArray types_ref, jint typesOffset, jintArray ids_ref, jint idsOffset, jintArray severities_ref, jint severitiesOffset, jintArray lengths_ref, jint lengthsOffset, jbyteArray messageLog_ref, jint messageLogOffset)
{
	jint _ret = 0;
	jbyte *messageLog = NULL;
	jint *lengths = NULL;
	jint *severities = NULL;
	jint *ids = NULL;
	jint *types = NULL;
	jint *sources = NULL;
	if (!sources_ref) {
		throw_iae(env, "sources == null");
		goto exit;
	}
	if (sourcesOffset < 0) {
		throw_iae(env, "sourcesOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, sources_ref) - sourcesOffset < 1) {
		throw_iae(env, "length - sourcesOffset < 1 < needed");
		goto exit;
	}
	sources = (*env)->GetIntArrayElements(env, sources_ref, NULL);
	if (!types_ref) {
		throw_iae(env, "types == null");
		goto exit;
	}
	if (typesOffset < 0) {
		throw_iae(env, "typesOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, types_ref) - typesOffset < 1) {
		throw_iae(env, "length - typesOffset < 1 < needed");
		goto exit;
	}
	types = (*env)->GetIntArrayElements(env, types_ref, NULL);
	if (!ids_ref) {
		throw_iae(env, "ids == null");
		goto exit;
	}
	if (idsOffset < 0) {
		throw_iae(env, "idsOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, ids_ref) - idsOffset < 1) {
		throw_iae(env, "length - idsOffset < 1 < needed");
		goto exit;
	}
	ids = (*env)->GetIntArrayElements(env, ids_ref, NULL);
	if (!severities_ref) {
		throw_iae(env, "severities == null");
		goto exit;
	}
	if (severitiesOffset < 0) {
		throw_iae(env, "severitiesOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, severities_ref) - severitiesOffset < 1) {
		throw_iae(env, "length - severitiesOffset < 1 < needed");
		goto exit;
	}
	severities = (*env)->GetIntArrayElements(env, severities_ref, NULL);
	if (!lengths_ref) {
		throw_iae(env, "lengths == null");
		goto exit;
	}
	if (lengthsOffset < 0) {
		throw_iae(env, "lengthsOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, lengths_ref) - lengthsOffset < 1) {
		throw_iae(env, "length - lengthsOffset < 1 < needed");
		goto exit;
	}
	lengths = (*env)->GetIntArrayElements(env, lengths_ref, NULL);
	if (!messageLog_ref) {
		throw_iae(env, "messageLog == null");
		goto exit;
	}
	if (messageLogOffset < 0) {
		throw_iae(env, "messageLogOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, messageLog_ref) - messageLogOffset < 1) {
		throw_iae(env, "length - messageLogOffset < 1 < needed");
		goto exit;
	}
	messageLog = (*env)->GetByteArrayElements(env, messageLog_ref, NULL);
	_ret = (jint)GL_EXT_CALL(glGetDebugMessageLogKHR, PFNGLGETDEBUGMESSAGELOGKHRPROC)((GLuint)count, (GLsizei)bufSize, (GLenum *)(sources + sourcesOffset), (GLenum *)(types + typesOffset), (GLuint *)(ids + idsOffset), (GLenum *)(severities + severitiesOffset), (GLsizei *)(lengths + lengthsOffset), (GLchar *)(messageLog + messageLogOffset));
exit:
	if (sources)
		(*env)->ReleaseIntArrayElements(env, sources_ref, sources, 0);
	if (types)
		(*env)->ReleaseIntArrayElements(env, types_ref, types, 0);
	if (ids)
		(*env)->ReleaseIntArrayElements(env, ids_ref, ids, 0);
	if (severities)
		(*env)->ReleaseIntArrayElements(env, severities_ref, severities, 0);
	if (lengths)
		(*env)->ReleaseIntArrayElements(env, lengths_ref, lengths, 0);
	if (messageLog)
		(*env)->ReleaseByteArrayElements(env, messageLog_ref, messageLog, 0);
	return _ret;
}

/* void glPushDebugGroupKHR ( GLenum source, GLuint id, GLsizei length, const GLchar *message ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glPushDebugGroupKHR__IIILjava_lang_String_2(JNIEnv *env, jclass clazz, jint source, jint id, jint length, jstring message_ref)
{
	const char *message = NULL;
	if (!message_ref) {
		throw_iae(env, "message == null");
		goto exit;
	}
	message = (*env)->GetStringUTFChars(env, message_ref, NULL);
	GL_EXT_CALL(glPushDebugGroupKHR, PFNGLPUSHDEBUGGROUPKHRPROC)((GLenum)source, (GLuint)id, (GLsizei)length, (const GLchar *)message);
exit:
	if (message)
		(*env)->ReleaseStringUTFChars(env, message_ref, message);
}

/* void glPopDebugGroupKHR ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glPopDebugGroupKHR__(JNIEnv *env, jclass clazz)
{
	GL_EXT_CALL(glPopDebugGroupKHR, PFNGLPOPDEBUGGROUPKHRPROC)();
}

/* void glObjectLabelKHR ( GLenum identifier, GLuint name, GLsizei length, const GLchar *label ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glObjectLabelKHR__IIILjava_lang_String_2(JNIEnv *env, jclass clazz, jint identifier, jint name, jint length, jstring label_ref)
{
	const char *label = NULL;
	if (!label_ref) {
		throw_iae(env, "label == null");
		goto exit;
	}
	label = (*env)->GetStringUTFChars(env, label_ref, NULL);
	GL_EXT_CALL(glObjectLabelKHR, PFNGLOBJECTLABELKHRPROC)((GLenum)identifier, (GLuint)name, (GLsizei)length, (const GLchar *)label);
exit:
	if (label)
		(*env)->ReleaseStringUTFChars(env, label_ref, label);
}

/* void glMinSampleShadingOES ( GLfloat value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glMinSampleShadingOES__F(JNIEnv *env, jclass clazz, jfloat value)
{
	GL_EXT_CALL(glMinSampleShadingOES, PFNGLMINSAMPLESHADINGOESPROC)((GLfloat)value);
}

/* void glTexStorage3DMultisampleOES ( GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLboolean fixedsamplelocations ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glTexStorage3DMultisampleOES__IIIIIIZ(JNIEnv *env, jclass clazz, jint target, jint samples, jint internalformat, jint width, jint height, jint depth, jboolean fixedsamplelocations)
{
	GL_EXT_CALL(glTexStorage3DMultisampleOES, PFNGLTEXSTORAGE3DMULTISAMPLEOESPROC)((GLenum)target, (GLsizei)samples, (GLenum)internalformat, (GLsizei)width, (GLsizei)height, (GLsizei)depth, (GLboolean)fixedsamplelocations);
}

/* void glCopyImageSubDataEXT ( GLuint srcName, GLenum srcTarget, GLint srcLevel, GLint srcX, GLint srcY, GLint srcZ, GLuint dstName, GLenum dstTarget, GLint dstLevel, GLint dstX, GLint dstY, GLint dstZ, GLsizei srcWidth, GLsizei srcHeight, GLsizei srcDepth ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glCopyImageSubDataEXT__IIIIIIIIIIIIIII(JNIEnv *env, jclass clazz, jint srcName, jint srcTarget, jint srcLevel, jint srcX, jint srcY, jint srcZ, jint dstName, jint dstTarget, jint dstLevel, jint dstX, jint dstY, jint dstZ, jint srcWidth, jint srcHeight, jint srcDepth)
{
	GL_EXT_CALL(glCopyImageSubDataEXT, PFNGLCOPYIMAGESUBDATAEXTPROC)((GLuint)srcName, (GLenum)srcTarget, (GLint)srcLevel, (GLint)srcX, (GLint)srcY, (GLint)srcZ, (GLuint)dstName, (GLenum)dstTarget, (GLint)dstLevel, (GLint)dstX, (GLint)dstY, (GLint)dstZ, (GLsizei)srcWidth, (GLsizei)srcHeight, (GLsizei)srcDepth);
}

/* void glEnableiEXT ( GLenum target, GLuint index ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glEnableiEXT__II(JNIEnv *env, jclass clazz, jint target, jint index)
{
	GL_EXT_CALL(glEnableiEXT, PFNGLENABLEIEXTPROC)((GLenum)target, (GLuint)index);
}

/* void glDisableiEXT ( GLenum target, GLuint index ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glDisableiEXT__II(JNIEnv *env, jclass clazz, jint target, jint index)
{
	GL_EXT_CALL(glDisableiEXT, PFNGLDISABLEIEXTPROC)((GLenum)target, (GLuint)index);
}

/* void glBlendEquationiEXT ( GLuint buf, GLenum mode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glBlendEquationiEXT__II(JNIEnv *env, jclass clazz, jint buf, jint mode)
{
	GL_EXT_CALL(glBlendEquationiEXT, PFNGLBLENDEQUATIONIEXTPROC)((GLuint)buf, (GLenum)mode);
}

/* void glBlendEquationSeparateiEXT ( GLuint buf, GLenum modeRGB, GLenum modeAlpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glBlendEquationSeparateiEXT__III(JNIEnv *env, jclass clazz, jint buf, jint modeRGB, jint modeAlpha)
{
	GL_EXT_CALL(glBlendEquationSeparateiEXT, PFNGLBLENDEQUATIONSEPARATEIEXTPROC)((GLuint)buf, (GLenum)modeRGB, (GLenum)modeAlpha);
}

/* void glBlendFunciEXT ( GLuint buf, GLenum src, GLenum dst ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glBlendFunciEXT__III(JNIEnv *env, jclass clazz, jint buf, jint src, jint dst)
{
	GL_EXT_CALL(glBlendFunciEXT, PFNGLBLENDFUNCIEXTPROC)((GLuint)buf, (GLenum)src, (GLenum)dst);
}

/* void glBlendFuncSeparateiEXT ( GLuint buf, GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glBlendFuncSeparateiEXT__IIIII(JNIEnv *env, jclass clazz, jint buf, jint srcRGB, jint dstRGB, jint srcAlpha, jint dstAlpha)
{
	GL_EXT_CALL(glBlendFuncSeparateiEXT, PFNGLBLENDFUNCSEPARATEIEXTPROC)((GLuint)buf, (GLenum)srcRGB, (GLenum)dstRGB, (GLenum)srcAlpha, (GLenum)dstAlpha);
}

/* void glColorMaskiEXT ( GLuint index, GLboolean r, GLboolean g, GLboolean b, GLboolean a ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glColorMaskiEXT__IZZZZ(JNIEnv *env, jclass clazz, jint index, jboolean r, jboolean g, jboolean b, jboolean a)
{
	GL_EXT_CALL(glColorMaskiEXT, PFNGLCOLORMASKIEXTPROC)((GLuint)index, (GLboolean)r, (GLboolean)g, (GLboolean)b, (GLboolean)a);
}

/* GLboolean glIsEnablediEXT ( GLenum target, GLuint index ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES31Ext_glIsEnablediEXT__II(JNIEnv *env, jclass clazz, jint target, jint index)
{
	jboolean _ret = 0;
	_ret = (jboolean)GL_EXT_CALL(glIsEnablediEXT, PFNGLISENABLEDIEXTPROC)((GLenum)target, (GLuint)index);
	return _ret;
}

/* void glFramebufferTextureEXT ( GLenum target, GLenum attachment, GLuint texture, GLint level ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glFramebufferTextureEXT__IIII(JNIEnv *env, jclass clazz, jint target, jint attachment, jint texture, jint level)
{
	GL_EXT_CALL(glFramebufferTextureEXT, PFNGLFRAMEBUFFERTEXTUREEXTPROC)((GLenum)target, (GLenum)attachment, (GLuint)texture, (GLint)level);
}

/* void glPrimitiveBoundingBoxEXT ( GLfloat minX, GLfloat minY, GLfloat minZ, GLfloat minW, GLfloat maxX, GLfloat maxY, GLfloat maxZ, GLfloat maxW ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glPrimitiveBoundingBoxEXT__FFFFFFFF(JNIEnv *env, jclass clazz, jfloat minX, jfloat minY, jfloat minZ, jfloat minW, jfloat maxX, jfloat maxY, jfloat maxZ, jfloat maxW)
{
	GL_EXT_CALL(glPrimitiveBoundingBoxEXT, PFNGLPRIMITIVEBOUNDINGBOXEXTPROC)((GLfloat)minX, (GLfloat)minY, (GLfloat)minZ, (GLfloat)minW, (GLfloat)maxX, (GLfloat)maxY, (GLfloat)maxZ, (GLfloat)maxW);
}

/* void glPatchParameteriEXT ( GLenum pname, GLint value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glPatchParameteriEXT__II(JNIEnv *env, jclass clazz, jint pname, jint value)
{
	GL_EXT_CALL(glPatchParameteriEXT, PFNGLPATCHPARAMETERIEXTPROC)((GLenum)pname, (GLint)value);
}

/* void glTexParameterIivEXT ( GLenum target, GLenum pname, const GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glTexParameterIivEXT__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	GL_EXT_CALL(glTexParameterIivEXT, PFNGLTEXPARAMETERIIVEXTPROC)((GLenum)target, (GLenum)pname, (const GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexParameterIivEXT ( GLenum target, GLenum pname, const GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glTexParameterIivEXT__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	GL_EXT_CALL(glTexParameterIivEXT, PFNGLTEXPARAMETERIIVEXTPROC)((GLenum)target, (GLenum)pname, (const GLint *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexParameterIuivEXT ( GLenum target, GLenum pname, const GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glTexParameterIuivEXT__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	GL_EXT_CALL(glTexParameterIuivEXT, PFNGLTEXPARAMETERIUIVEXTPROC)((GLenum)target, (GLenum)pname, (const GLuint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexParameterIuivEXT ( GLenum target, GLenum pname, const GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glTexParameterIuivEXT__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	GL_EXT_CALL(glTexParameterIuivEXT, PFNGLTEXPARAMETERIUIVEXTPROC)((GLenum)target, (GLenum)pname, (const GLuint *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexParameterIivEXT ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glGetTexParameterIivEXT__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	GL_EXT_CALL(glGetTexParameterIivEXT, PFNGLGETTEXPARAMETERIIVEXTPROC)((GLenum)target, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexParameterIivEXT ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glGetTexParameterIivEXT__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	GL_EXT_CALL(glGetTexParameterIivEXT, PFNGLGETTEXPARAMETERIIVEXTPROC)((GLenum)target, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexParameterIuivEXT ( GLenum target, GLenum pname, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glGetTexParameterIuivEXT__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	GL_EXT_CALL(glGetTexParameterIuivEXT, PFNGLGETTEXPARAMETERIUIVEXTPROC)((GLenum)target, (GLenum)pname, (GLuint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexParameterIuivEXT ( GLenum target, GLenum pname, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glGetTexParameterIuivEXT__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	GL_EXT_CALL(glGetTexParameterIuivEXT, PFNGLGETTEXPARAMETERIUIVEXTPROC)((GLenum)target, (GLenum)pname, (GLuint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glSamplerParameterIivEXT ( GLuint sampler, GLenum pname, const GLint *param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glSamplerParameterIivEXT__II_3II(JNIEnv *env, jclass clazz, jint sampler, jint pname, jintArray param_ref, jint offset)
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
	GL_EXT_CALL(glSamplerParameterIivEXT, PFNGLSAMPLERPARAMETERIIVEXTPROC)((GLuint)sampler, (GLenum)pname, (const GLint *)(param + offset));
exit:
	if (param)
		(*env)->ReleaseIntArrayElements(env, param_ref, param, JNI_ABORT);
}

/* void glSamplerParameterIivEXT ( GLuint sampler, GLenum pname, const GLint *param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glSamplerParameterIivEXT__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint sampler, jint pname, jobject param_ref)
{
	jarray param_array = NULL;
	jbyte *param_elems = NULL;
	void *param = NULL;
	param = get_nio_buffer(env, param_ref, &param_array, &param_elems);
	GL_EXT_CALL(glSamplerParameterIivEXT, PFNGLSAMPLERPARAMETERIIVEXTPROC)((GLuint)sampler, (GLenum)pname, (const GLint *)param);
	release_nio_buffer(env, param_array, param_elems);
}

/* void glSamplerParameterIuivEXT ( GLuint sampler, GLenum pname, const GLuint *param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glSamplerParameterIuivEXT__II_3II(JNIEnv *env, jclass clazz, jint sampler, jint pname, jintArray param_ref, jint offset)
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
	GL_EXT_CALL(glSamplerParameterIuivEXT, PFNGLSAMPLERPARAMETERIUIVEXTPROC)((GLuint)sampler, (GLenum)pname, (const GLuint *)(param + offset));
exit:
	if (param)
		(*env)->ReleaseIntArrayElements(env, param_ref, param, JNI_ABORT);
}

/* void glSamplerParameterIuivEXT ( GLuint sampler, GLenum pname, const GLuint *param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glSamplerParameterIuivEXT__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint sampler, jint pname, jobject param_ref)
{
	jarray param_array = NULL;
	jbyte *param_elems = NULL;
	void *param = NULL;
	param = get_nio_buffer(env, param_ref, &param_array, &param_elems);
	GL_EXT_CALL(glSamplerParameterIuivEXT, PFNGLSAMPLERPARAMETERIUIVEXTPROC)((GLuint)sampler, (GLenum)pname, (const GLuint *)param);
	release_nio_buffer(env, param_array, param_elems);
}

/* void glGetSamplerParameterIivEXT ( GLuint sampler, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glGetSamplerParameterIivEXT__II_3II(JNIEnv *env, jclass clazz, jint sampler, jint pname, jintArray params_ref, jint offset)
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
	GL_EXT_CALL(glGetSamplerParameterIivEXT, PFNGLGETSAMPLERPARAMETERIIVEXTPROC)((GLuint)sampler, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetSamplerParameterIivEXT ( GLuint sampler, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glGetSamplerParameterIivEXT__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint sampler, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	GL_EXT_CALL(glGetSamplerParameterIivEXT, PFNGLGETSAMPLERPARAMETERIIVEXTPROC)((GLuint)sampler, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetSamplerParameterIuivEXT ( GLuint sampler, GLenum pname, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glGetSamplerParameterIuivEXT__II_3II(JNIEnv *env, jclass clazz, jint sampler, jint pname, jintArray params_ref, jint offset)
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
	GL_EXT_CALL(glGetSamplerParameterIuivEXT, PFNGLGETSAMPLERPARAMETERIUIVEXTPROC)((GLuint)sampler, (GLenum)pname, (GLuint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetSamplerParameterIuivEXT ( GLuint sampler, GLenum pname, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glGetSamplerParameterIuivEXT__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint sampler, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	GL_EXT_CALL(glGetSamplerParameterIuivEXT, PFNGLGETSAMPLERPARAMETERIUIVEXTPROC)((GLuint)sampler, (GLenum)pname, (GLuint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexBufferEXT ( GLenum target, GLenum internalformat, GLuint buffer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glTexBufferEXT__III(JNIEnv *env, jclass clazz, jint target, jint internalformat, jint buffer)
{
	GL_EXT_CALL(glTexBufferEXT, PFNGLTEXBUFFEREXTPROC)((GLenum)target, (GLenum)internalformat, (GLuint)buffer);
}

/* void glTexBufferRangeEXT ( GLenum target, GLenum internalformat, GLuint buffer, GLintptr offset, GLsizeiptr size ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glTexBufferRangeEXT__IIIII(JNIEnv *env, jclass clazz, jint target, jint internalformat, jint buffer, jint offset, jint size)
{
	GL_EXT_CALL(glTexBufferRangeEXT, PFNGLTEXBUFFERRANGEEXTPROC)((GLenum)target, (GLenum)internalformat, (GLuint)buffer, (GLintptr)offset, (GLsizeiptr)size);
}

/* ---- written by hand: debug output (KHR_debug) ---- */

/* void glDebugMessageInsertKHR ( GLenum source, GLenum type, GLuint id, GLenum severity, GLsizei length, const GLchar *buf ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glDebugMessageInsertKHR__IIIILjava_lang_String_2(JNIEnv *env, jclass clazz, jint source, jint type, jint id, jint severity, jstring buf_ref)
{
	if (!buf_ref) {
		throw_iae(env, "buf == null");
		return;
	}
	const char *buf = (*env)->GetStringUTFChars(env, buf_ref, NULL);
	GL_EXT_CALL(glDebugMessageInsertKHR, PFNGLDEBUGMESSAGEINSERTKHRPROC)((GLenum)source, (GLenum)type, (GLuint)id, (GLenum)severity, -1, buf);
	(*env)->ReleaseStringUTFChars(env, buf_ref, buf);
}

/* DEBUGPROCKHR glGetDebugMessageCallbackKHR (): no callback is ever registered (see glDebugMessageCallbackKHR) */
JNIEXPORT jobject JNICALL Java_android_opengl_GLES31Ext_glGetDebugMessageCallbackKHR__(JNIEnv *env, jclass clazz)
{
	return NULL;
}

/* void glDebugMessageCallbackKHR ( GLDEBUGPROCKHR callback, const void *userParam ): ATL does not call back into
 * Java from GL's debug output; nothing is registered */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glDebugMessageCallbackKHR__Landroid_opengl_GLES31Ext_00024DebugProcKHR_2(JNIEnv *env, jclass clazz, jobject callback)
{
	(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/UnsupportedOperationException"), "not yet implemented");
}

/* GLuint glGetDebugMessageLogKHR ( GLuint count, GLsizei bufSize, GLenum *sources, GLenum *types, GLuint *ids, GLenum *severities, GLsizei *lengths, GLchar *messageLog ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES31Ext_glGetDebugMessageLogKHR__ILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_ByteBuffer_2(JNIEnv *env, jclass clazz, jint count, jobject sources_ref, jobject types_ref, jobject ids_ref, jobject severities_ref, jobject lengths_ref, jobject messageLog_ref)
{
	jarray a[6] = {0};
	jbyte *e[6] = {0};
	GLsizei buf_size = messageLog_ref ? get_nio_buffer_size(env, messageLog_ref) : 0;
	GLenum *sources = get_nio_buffer(env, sources_ref, &a[0], &e[0]);
	GLenum *types = get_nio_buffer(env, types_ref, &a[1], &e[1]);
	GLuint *ids = get_nio_buffer(env, ids_ref, &a[2], &e[2]);
	GLenum *severities = get_nio_buffer(env, severities_ref, &a[3], &e[3]);
	GLsizei *lengths = get_nio_buffer(env, lengths_ref, &a[4], &e[4]);
	GLchar *log = get_nio_buffer(env, messageLog_ref, &a[5], &e[5]);
	GLuint n = GL_EXT_CALL(glGetDebugMessageLogKHR, PFNGLGETDEBUGMESSAGELOGKHRPROC)((GLuint)count, buf_size, sources, types, ids, severities, lengths, log);
	for (int i = 5; i >= 0; i--)
		release_nio_buffer(env, a[i], e[i]);
	return (jint)n;
}

JNIEXPORT jobjectArray JNICALL Java_android_opengl_GLES31Ext_glGetDebugMessageLogKHR__I_3II_3II_3II_3II(JNIEnv *env, jclass clazz, jint count, jintArray sources_ref, jint sourcesOffset, jintArray types_ref, jint typesOffset, jintArray ids_ref, jint idsOffset, jintArray severities_ref, jint severitiesOffset)
{
	jobjectArray result = NULL;
	jint *sources = get_int_array(env, sources_ref, sourcesOffset, count, "sources");
	jint *types = sources ? get_int_array(env, types_ref, typesOffset, count, "types") : NULL;
	jint *ids = types ? get_int_array(env, ids_ref, idsOffset, count, "ids") : NULL;
	jint *severities = ids ? get_int_array(env, severities_ref, severitiesOffset, count, "severities") : NULL;
	if (severities)
		result = debug_message_log_strings(env, GL_EXT_CALL(glGetDebugMessageLogKHR, PFNGLGETDEBUGMESSAGELOGKHRPROC), count, (GLenum *)sources + sourcesOffset, (GLenum *)types + typesOffset,
		                                   (GLuint *)ids + idsOffset, (GLenum *)severities + severitiesOffset);
	if (severities)
		(*env)->ReleaseIntArrayElements(env, severities_ref, severities, 0);
	if (ids)
		(*env)->ReleaseIntArrayElements(env, ids_ref, ids, 0);
	if (types)
		(*env)->ReleaseIntArrayElements(env, types_ref, types, 0);
	if (sources)
		(*env)->ReleaseIntArrayElements(env, sources_ref, sources, 0);
	return result;
}

JNIEXPORT jobjectArray JNICALL Java_android_opengl_GLES31Ext_glGetDebugMessageLogKHR__ILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint count, jobject sources_ref, jobject types_ref, jobject ids_ref, jobject severities_ref)
{
	jarray a[4] = {0};
	jbyte *e[4] = {0};
	if (!sources_ref || !types_ref || !ids_ref || !severities_ref) {
		throw_iae(env, !sources_ref ? "sources == null" : !types_ref ? "types == null" : !ids_ref ? "ids == null" : "severities == null");
		return NULL;
	}
	if (get_nio_buffer_size(env, sources_ref) < count || get_nio_buffer_size(env, types_ref) < count
	    || get_nio_buffer_size(env, ids_ref) < count || get_nio_buffer_size(env, severities_ref) < count) {
		throw_iae(env, "remaining() < count < needed");
		return NULL;
	}
	GLenum *sources = get_nio_buffer(env, sources_ref, &a[0], &e[0]);
	GLenum *types = get_nio_buffer(env, types_ref, &a[1], &e[1]);
	GLuint *ids = get_nio_buffer(env, ids_ref, &a[2], &e[2]);
	GLenum *severities = get_nio_buffer(env, severities_ref, &a[3], &e[3]);
	jobjectArray result = debug_message_log_strings(env, GL_EXT_CALL(glGetDebugMessageLogKHR, PFNGLGETDEBUGMESSAGELOGKHRPROC), count, sources, types, ids, severities);
	for (int i = 3; i >= 0; i--)
		release_nio_buffer(env, a[i], e[i]);
	return result;
}

/* void glGetObjectLabelKHR ( GLenum identifier, GLuint name, GLsizei bufSize, GLsizei *length, GLchar *label ) */
JNIEXPORT jstring JNICALL Java_android_opengl_GLES31Ext_glGetObjectLabelKHR__II(JNIEnv *env, jclass clazz, jint identifier, jint name)
{
	return OBJECT_LABEL(env, GL_EXT_CALL(glGetObjectLabelKHR, PFNGLGETOBJECTLABELKHRPROC), (GLenum)identifier, (GLuint)name);
}

/* void glObjectPtrLabelKHR ( const void *ptr, GLsizei length, const GLchar *label ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31Ext_glObjectPtrLabelKHR__JLjava_lang_String_2(JNIEnv *env, jclass clazz, jlong ptr, jstring label_ref)
{
	const char *label = label_ref ? (*env)->GetStringUTFChars(env, label_ref, NULL) : NULL;
	GL_EXT_CALL(glObjectPtrLabelKHR, PFNGLOBJECTPTRLABELKHRPROC)((const void *)(intptr_t)ptr, -1, label);
	if (label)
		(*env)->ReleaseStringUTFChars(env, label_ref, label);
}

/* void glGetObjectPtrLabelKHR ( const void *ptr, GLsizei bufSize, GLsizei *length, GLchar *label ) */
JNIEXPORT jstring JNICALL Java_android_opengl_GLES31Ext_glGetObjectPtrLabelKHR__J(JNIEnv *env, jclass clazz, jlong ptr)
{
	return OBJECT_LABEL(env, GL_EXT_CALL(glGetObjectPtrLabelKHR, PFNGLGETOBJECTPTRLABELKHRPROC), (const void *)(intptr_t)ptr);
}
