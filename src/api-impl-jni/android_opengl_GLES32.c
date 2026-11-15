/* JNI of android.opengl.GLES32. Generated from AOSP's GLES32.java (its "// C function" comments and native
 * declarations, Android 16) and the Khronos GLES headers, the string and debug functions written by hand. AOSP's
 * own JNI (frameworks/base/core/jni/android_opengl_GLES32.cpp) is generated as well and not in the local sources;
 * the argument checks follow its IllegalArgumentException messages for null arrays/buffers and negative offsets. */
#include "defines.h"
#include "util.h"
#include "android_opengl_GLES3.h"

#include "generated_headers/android_opengl_GLES32.h"

/* void glBlendBarrier ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glBlendBarrier__(JNIEnv *env, jclass clazz)
{
	GL_EXT_CALL(glBlendBarrier, PFNGLBLENDBARRIERPROC)();
}

/* void glCopyImageSubData ( GLuint srcName, GLenum srcTarget, GLint srcLevel, GLint srcX, GLint srcY, GLint srcZ, GLuint dstName, GLenum dstTarget, GLint dstLevel, GLint dstX, GLint dstY, GLint dstZ, GLsizei srcWidth, GLsizei srcHeight, GLsizei srcDepth ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glCopyImageSubData__IIIIIIIIIIIIIII(JNIEnv *env, jclass clazz, jint srcName, jint srcTarget, jint srcLevel, jint srcX, jint srcY, jint srcZ, jint dstName, jint dstTarget, jint dstLevel, jint dstX, jint dstY, jint dstZ, jint srcWidth, jint srcHeight, jint srcDepth)
{
	glCopyImageSubData((GLuint)srcName, (GLenum)srcTarget, (GLint)srcLevel, (GLint)srcX, (GLint)srcY, (GLint)srcZ, (GLuint)dstName, (GLenum)dstTarget, (GLint)dstLevel, (GLint)dstX, (GLint)dstY, (GLint)dstZ, (GLsizei)srcWidth, (GLsizei)srcHeight, (GLsizei)srcDepth);
}

/* void glDebugMessageControl ( GLenum source, GLenum type, GLenum severity, GLsizei count, const GLuint *ids, GLboolean enabled ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glDebugMessageControl__IIII_3IIZ(JNIEnv *env, jclass clazz, jint source, jint type, jint severity, jint count, jintArray ids_ref, jint offset, jboolean enabled)
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
	glDebugMessageControl((GLenum)source, (GLenum)type, (GLenum)severity, (GLsizei)count, (const GLuint *)(ids + offset), (GLboolean)enabled);
exit:
	if (ids)
		(*env)->ReleaseIntArrayElements(env, ids_ref, ids, JNI_ABORT);
}

/* void glDebugMessageControl ( GLenum source, GLenum type, GLenum severity, GLsizei count, const GLuint *ids, GLboolean enabled ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glDebugMessageControl__IIIILjava_nio_IntBuffer_2Z(JNIEnv *env, jclass clazz, jint source, jint type, jint severity, jint count, jobject ids_ref, jboolean enabled)
{
	jarray ids_array = NULL;
	jbyte *ids_elems = NULL;
	void *ids = NULL;
	ids = get_nio_buffer(env, ids_ref, &ids_array, &ids_elems);
	glDebugMessageControl((GLenum)source, (GLenum)type, (GLenum)severity, (GLsizei)count, (const GLuint *)ids, (GLboolean)enabled);
	release_nio_buffer(env, ids_array, ids_elems);
}

/* void glDebugMessageInsert ( GLenum source, GLenum type, GLuint id, GLenum severity, GLsizei length, const GLchar *buf ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glDebugMessageInsert__IIIIILjava_lang_String_2(JNIEnv *env, jclass clazz, jint source, jint type, jint id, jint severity, jint length, jstring buf_ref)
{
	const char *buf = NULL;
	if (!buf_ref) {
		throw_iae(env, "buf == null");
		goto exit;
	}
	buf = (*env)->GetStringUTFChars(env, buf_ref, NULL);
	glDebugMessageInsert((GLenum)source, (GLenum)type, (GLuint)id, (GLenum)severity, (GLsizei)length, (const GLchar *)buf);
exit:
	if (buf)
		(*env)->ReleaseStringUTFChars(env, buf_ref, buf);
}

/* GLuint glGetDebugMessageLog ( GLuint count, GLsizei bufSize, GLenum *sources, GLenum *types, GLuint *ids, GLenum *severities, GLsizei *lengths, GLchar *messageLog ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES32_glGetDebugMessageLog__II_3II_3II_3II_3II_3II_3BI(JNIEnv *env, jclass clazz, jint count, jint bufSize, jintArray sources_ref, jint sourcesOffset, jintArray types_ref, jint typesOffset, jintArray ids_ref, jint idsOffset, jintArray severities_ref, jint severitiesOffset, jintArray lengths_ref, jint lengthsOffset, jbyteArray messageLog_ref, jint messageLogOffset)
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
	_ret = (jint)glGetDebugMessageLog((GLuint)count, (GLsizei)bufSize, (GLenum *)(sources + sourcesOffset), (GLenum *)(types + typesOffset), (GLuint *)(ids + idsOffset), (GLenum *)(severities + severitiesOffset), (GLsizei *)(lengths + lengthsOffset), (GLchar *)(messageLog + messageLogOffset));
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

/* void glPushDebugGroup ( GLenum source, GLuint id, GLsizei length, const GLchar *message ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glPushDebugGroup__IIILjava_lang_String_2(JNIEnv *env, jclass clazz, jint source, jint id, jint length, jstring message_ref)
{
	const char *message = NULL;
	if (!message_ref) {
		throw_iae(env, "message == null");
		goto exit;
	}
	message = (*env)->GetStringUTFChars(env, message_ref, NULL);
	glPushDebugGroup((GLenum)source, (GLuint)id, (GLsizei)length, (const GLchar *)message);
exit:
	if (message)
		(*env)->ReleaseStringUTFChars(env, message_ref, message);
}

/* void glPopDebugGroup ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glPopDebugGroup__(JNIEnv *env, jclass clazz)
{
	glPopDebugGroup();
}

/* void glObjectLabel ( GLenum identifier, GLuint name, GLsizei length, const GLchar *label ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glObjectLabel__IIILjava_lang_String_2(JNIEnv *env, jclass clazz, jint identifier, jint name, jint length, jstring label_ref)
{
	const char *label = NULL;
	if (!label_ref) {
		throw_iae(env, "label == null");
		goto exit;
	}
	label = (*env)->GetStringUTFChars(env, label_ref, NULL);
	glObjectLabel((GLenum)identifier, (GLuint)name, (GLsizei)length, (const GLchar *)label);
exit:
	if (label)
		(*env)->ReleaseStringUTFChars(env, label_ref, label);
}

/* void glEnablei ( GLenum target, GLuint index ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glEnablei__II(JNIEnv *env, jclass clazz, jint target, jint index)
{
	glEnablei((GLenum)target, (GLuint)index);
}

/* void glDisablei ( GLenum target, GLuint index ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glDisablei__II(JNIEnv *env, jclass clazz, jint target, jint index)
{
	glDisablei((GLenum)target, (GLuint)index);
}

/* void glBlendEquationi ( GLuint buf, GLenum mode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glBlendEquationi__II(JNIEnv *env, jclass clazz, jint buf, jint mode)
{
	glBlendEquationi((GLuint)buf, (GLenum)mode);
}

/* void glBlendEquationSeparatei ( GLuint buf, GLenum modeRGB, GLenum modeAlpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glBlendEquationSeparatei__III(JNIEnv *env, jclass clazz, jint buf, jint modeRGB, jint modeAlpha)
{
	glBlendEquationSeparatei((GLuint)buf, (GLenum)modeRGB, (GLenum)modeAlpha);
}

/* void glBlendFunci ( GLuint buf, GLenum src, GLenum dst ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glBlendFunci__III(JNIEnv *env, jclass clazz, jint buf, jint src, jint dst)
{
	glBlendFunci((GLuint)buf, (GLenum)src, (GLenum)dst);
}

/* void glBlendFuncSeparatei ( GLuint buf, GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glBlendFuncSeparatei__IIIII(JNIEnv *env, jclass clazz, jint buf, jint srcRGB, jint dstRGB, jint srcAlpha, jint dstAlpha)
{
	glBlendFuncSeparatei((GLuint)buf, (GLenum)srcRGB, (GLenum)dstRGB, (GLenum)srcAlpha, (GLenum)dstAlpha);
}

/* void glColorMaski ( GLuint index, GLboolean r, GLboolean g, GLboolean b, GLboolean a ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glColorMaski__IZZZZ(JNIEnv *env, jclass clazz, jint index, jboolean r, jboolean g, jboolean b, jboolean a)
{
	glColorMaski((GLuint)index, (GLboolean)r, (GLboolean)g, (GLboolean)b, (GLboolean)a);
}

/* GLboolean glIsEnabledi ( GLenum target, GLuint index ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES32_glIsEnabledi__II(JNIEnv *env, jclass clazz, jint target, jint index)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsEnabledi((GLenum)target, (GLuint)index);
	return _ret;
}

/* void glDrawElementsBaseVertex ( GLenum mode, GLsizei count, GLenum type, const void *indices, GLint basevertex ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glDrawElementsBaseVertex__IIILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint mode, jint count, jint type, jobject indices_ref, jint basevertex)
{
	jarray indices_array = NULL;
	jbyte *indices_elems = NULL;
	void *indices = NULL;
	indices = get_nio_buffer(env, indices_ref, &indices_array, &indices_elems);
	glDrawElementsBaseVertex((GLenum)mode, (GLsizei)count, (GLenum)type, (const void *)indices, (GLint)basevertex);
	release_nio_buffer(env, indices_array, indices_elems);
}

/* void glDrawRangeElementsBaseVertex ( GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const void *indices, GLint basevertex ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glDrawRangeElementsBaseVertex__IIIIILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint mode, jint start, jint end, jint count, jint type, jobject indices_ref, jint basevertex)
{
	jarray indices_array = NULL;
	jbyte *indices_elems = NULL;
	void *indices = NULL;
	indices = get_nio_buffer(env, indices_ref, &indices_array, &indices_elems);
	glDrawRangeElementsBaseVertex((GLenum)mode, (GLuint)start, (GLuint)end, (GLsizei)count, (GLenum)type, (const void *)indices, (GLint)basevertex);
	release_nio_buffer(env, indices_array, indices_elems);
}

/* void glDrawElementsInstancedBaseVertex ( GLenum mode, GLsizei count, GLenum type, const void *indices, GLsizei instanceCount, GLint basevertex ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glDrawElementsInstancedBaseVertex__IIILjava_nio_Buffer_2II(JNIEnv *env, jclass clazz, jint mode, jint count, jint type, jobject indices_ref, jint instanceCount, jint basevertex)
{
	jarray indices_array = NULL;
	jbyte *indices_elems = NULL;
	void *indices = NULL;
	indices = get_nio_buffer(env, indices_ref, &indices_array, &indices_elems);
	glDrawElementsInstancedBaseVertex((GLenum)mode, (GLsizei)count, (GLenum)type, (const void *)indices, (GLsizei)instanceCount, (GLint)basevertex);
	release_nio_buffer(env, indices_array, indices_elems);
}

/* void glDrawElementsInstancedBaseVertex ( GLenum mode, GLsizei count, GLenum type, const void *indices, GLsizei instanceCount, GLint basevertex ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glDrawElementsInstancedBaseVertex__IIIIII(JNIEnv *env, jclass clazz, jint mode, jint count, jint type, jint indicesOffset, jint instanceCount, jint basevertex)
{
	glDrawElementsInstancedBaseVertex((GLenum)mode, (GLsizei)count, (GLenum)type, (const void *)(intptr_t)indicesOffset, (GLsizei)instanceCount, (GLint)basevertex);
}

/* void glFramebufferTexture ( GLenum target, GLenum attachment, GLuint texture, GLint level ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glFramebufferTexture__IIII(JNIEnv *env, jclass clazz, jint target, jint attachment, jint texture, jint level)
{
	glFramebufferTexture((GLenum)target, (GLenum)attachment, (GLuint)texture, (GLint)level);
}

/* void glPrimitiveBoundingBox ( GLfloat minX, GLfloat minY, GLfloat minZ, GLfloat minW, GLfloat maxX, GLfloat maxY, GLfloat maxZ, GLfloat maxW ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glPrimitiveBoundingBox__FFFFFFFF(JNIEnv *env, jclass clazz, jfloat minX, jfloat minY, jfloat minZ, jfloat minW, jfloat maxX, jfloat maxY, jfloat maxZ, jfloat maxW)
{
	GL_EXT_CALL(glPrimitiveBoundingBox, PFNGLPRIMITIVEBOUNDINGBOXPROC)((GLfloat)minX, (GLfloat)minY, (GLfloat)minZ, (GLfloat)minW, (GLfloat)maxX, (GLfloat)maxY, (GLfloat)maxZ, (GLfloat)maxW);
}

/* GLenum glGetGraphicsResetStatus ( void ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES32_glGetGraphicsResetStatus__(JNIEnv *env, jclass clazz)
{
	jint _ret = 0;
	_ret = (jint)glGetGraphicsResetStatus();
	return _ret;
}

/* void glReadnPixels ( GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLsizei bufSize, void *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glReadnPixels__IIIIIIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint x, jint y, jint width, jint height, jint format, jint type, jint bufSize, jobject data_ref)
{
	jarray data_array = NULL;
	jbyte *data_elems = NULL;
	void *data = NULL;
	if (!data_ref) {
		throw_iae(env, "data == null");
		goto exit;
	}
	data = get_nio_buffer(env, data_ref, &data_array, &data_elems);
	glReadnPixels((GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height, (GLenum)format, (GLenum)type, (GLsizei)bufSize, (void *)data);
exit:
	release_nio_buffer(env, data_array, data_elems);
}

/* void glGetnUniformfv ( GLuint program, GLint location, GLsizei bufSize, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetnUniformfv__III_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint bufSize, jfloatArray params_ref, jint offset)
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
	glGetnUniformfv((GLuint)program, (GLint)location, (GLsizei)bufSize, (GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, 0);
}

/* void glGetnUniformfv ( GLuint program, GLint location, GLsizei bufSize, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetnUniformfv__IIILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint bufSize, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetnUniformfv((GLuint)program, (GLint)location, (GLsizei)bufSize, (GLfloat *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetnUniformiv ( GLuint program, GLint location, GLsizei bufSize, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetnUniformiv__III_3II(JNIEnv *env, jclass clazz, jint program, jint location, jint bufSize, jintArray params_ref, jint offset)
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
	glGetnUniformiv((GLuint)program, (GLint)location, (GLsizei)bufSize, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetnUniformiv ( GLuint program, GLint location, GLsizei bufSize, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetnUniformiv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint bufSize, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetnUniformiv((GLuint)program, (GLint)location, (GLsizei)bufSize, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetnUniformuiv ( GLuint program, GLint location, GLsizei bufSize, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetnUniformuiv__III_3II(JNIEnv *env, jclass clazz, jint program, jint location, jint bufSize, jintArray params_ref, jint offset)
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
	glGetnUniformuiv((GLuint)program, (GLint)location, (GLsizei)bufSize, (GLuint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetnUniformuiv ( GLuint program, GLint location, GLsizei bufSize, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetnUniformuiv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint bufSize, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetnUniformuiv((GLuint)program, (GLint)location, (GLsizei)bufSize, (GLuint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glMinSampleShading ( GLfloat value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glMinSampleShading__F(JNIEnv *env, jclass clazz, jfloat value)
{
	glMinSampleShading((GLfloat)value);
}

/* void glPatchParameteri ( GLenum pname, GLint value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glPatchParameteri__II(JNIEnv *env, jclass clazz, jint pname, jint value)
{
	glPatchParameteri((GLenum)pname, (GLint)value);
}

/* void glTexParameterIiv ( GLenum target, GLenum pname, const GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glTexParameterIiv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	glTexParameterIiv((GLenum)target, (GLenum)pname, (const GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexParameterIiv ( GLenum target, GLenum pname, const GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glTexParameterIiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexParameterIiv((GLenum)target, (GLenum)pname, (const GLint *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexParameterIuiv ( GLenum target, GLenum pname, const GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glTexParameterIuiv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	glTexParameterIuiv((GLenum)target, (GLenum)pname, (const GLuint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, JNI_ABORT);
}

/* void glTexParameterIuiv ( GLenum target, GLenum pname, const GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glTexParameterIuiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexParameterIuiv((GLenum)target, (GLenum)pname, (const GLuint *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexParameterIiv ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetTexParameterIiv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	glGetTexParameterIiv((GLenum)target, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexParameterIiv ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetTexParameterIiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexParameterIiv((GLenum)target, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexParameterIuiv ( GLenum target, GLenum pname, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetTexParameterIuiv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	glGetTexParameterIuiv((GLenum)target, (GLenum)pname, (GLuint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexParameterIuiv ( GLenum target, GLenum pname, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetTexParameterIuiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexParameterIuiv((GLenum)target, (GLenum)pname, (GLuint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glSamplerParameterIiv ( GLuint sampler, GLenum pname, const GLint *param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glSamplerParameterIiv__II_3II(JNIEnv *env, jclass clazz, jint sampler, jint pname, jintArray param_ref, jint offset)
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
	glSamplerParameterIiv((GLuint)sampler, (GLenum)pname, (const GLint *)(param + offset));
exit:
	if (param)
		(*env)->ReleaseIntArrayElements(env, param_ref, param, JNI_ABORT);
}

/* void glSamplerParameterIiv ( GLuint sampler, GLenum pname, const GLint *param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glSamplerParameterIiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint sampler, jint pname, jobject param_ref)
{
	jarray param_array = NULL;
	jbyte *param_elems = NULL;
	void *param = NULL;
	param = get_nio_buffer(env, param_ref, &param_array, &param_elems);
	glSamplerParameterIiv((GLuint)sampler, (GLenum)pname, (const GLint *)param);
	release_nio_buffer(env, param_array, param_elems);
}

/* void glSamplerParameterIuiv ( GLuint sampler, GLenum pname, const GLuint *param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glSamplerParameterIuiv__II_3II(JNIEnv *env, jclass clazz, jint sampler, jint pname, jintArray param_ref, jint offset)
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
	glSamplerParameterIuiv((GLuint)sampler, (GLenum)pname, (const GLuint *)(param + offset));
exit:
	if (param)
		(*env)->ReleaseIntArrayElements(env, param_ref, param, JNI_ABORT);
}

/* void glSamplerParameterIuiv ( GLuint sampler, GLenum pname, const GLuint *param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glSamplerParameterIuiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint sampler, jint pname, jobject param_ref)
{
	jarray param_array = NULL;
	jbyte *param_elems = NULL;
	void *param = NULL;
	param = get_nio_buffer(env, param_ref, &param_array, &param_elems);
	glSamplerParameterIuiv((GLuint)sampler, (GLenum)pname, (const GLuint *)param);
	release_nio_buffer(env, param_array, param_elems);
}

/* void glGetSamplerParameterIiv ( GLuint sampler, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetSamplerParameterIiv__II_3II(JNIEnv *env, jclass clazz, jint sampler, jint pname, jintArray params_ref, jint offset)
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
	glGetSamplerParameterIiv((GLuint)sampler, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetSamplerParameterIiv ( GLuint sampler, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetSamplerParameterIiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint sampler, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetSamplerParameterIiv((GLuint)sampler, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetSamplerParameterIuiv ( GLuint sampler, GLenum pname, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetSamplerParameterIuiv__II_3II(JNIEnv *env, jclass clazz, jint sampler, jint pname, jintArray params_ref, jint offset)
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
	glGetSamplerParameterIuiv((GLuint)sampler, (GLenum)pname, (GLuint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetSamplerParameterIuiv ( GLuint sampler, GLenum pname, GLuint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glGetSamplerParameterIuiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint sampler, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetSamplerParameterIuiv((GLuint)sampler, (GLenum)pname, (GLuint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexBuffer ( GLenum target, GLenum internalformat, GLuint buffer ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glTexBuffer__III(JNIEnv *env, jclass clazz, jint target, jint internalformat, jint buffer)
{
	glTexBuffer((GLenum)target, (GLenum)internalformat, (GLuint)buffer);
}

/* void glTexBufferRange ( GLenum target, GLenum internalformat, GLuint buffer, GLintptr offset, GLsizeiptr size ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glTexBufferRange__IIIII(JNIEnv *env, jclass clazz, jint target, jint internalformat, jint buffer, jint offset, jint size)
{
	glTexBufferRange((GLenum)target, (GLenum)internalformat, (GLuint)buffer, (GLintptr)offset, (GLsizeiptr)size);
}

/* void glTexStorage3DMultisample ( GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLboolean fixedsamplelocations ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glTexStorage3DMultisample__IIIIIIZ(JNIEnv *env, jclass clazz, jint target, jint samples, jint internalformat, jint width, jint height, jint depth, jboolean fixedsamplelocations)
{
	glTexStorage3DMultisample((GLenum)target, (GLsizei)samples, (GLenum)internalformat, (GLsizei)width, (GLsizei)height, (GLsizei)depth, (GLboolean)fixedsamplelocations);
}

/* ---- written by hand: debug output (KHR_debug) and glGetPointerv ---- */

/* void glGetPointerv ( GLenum pname, void **params ) */
JNIEXPORT jlong JNICALL Java_android_opengl_GLES32_glGetPointerv__I(JNIEnv *env, jclass clazz, jint pname)
{
	void *pointer = NULL;
	glGetPointerv((GLenum)pname, &pointer);
	return (jlong)(intptr_t)pointer;
}

/* void glDebugMessageInsert ( GLenum source, GLenum type, GLuint id, GLenum severity, GLsizei length, const GLchar *buf ) */
/* (generated above) */

/* void glDebugMessageCallback ( GLDEBUGPROC callback, const void *userParam ): ATL does not call back into
 * Java from GL's debug output; nothing is registered */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glDebugMessageCallback__Landroid_opengl_GLES32_00024DebugProc_2(JNIEnv *env, jclass clazz, jobject callback)
{
	(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/UnsupportedOperationException"), "not yet implemented");
}

/* GLuint glGetDebugMessageLog ( GLuint count, GLsizei bufSize, GLenum *sources, GLenum *types, GLuint *ids, GLenum *severities, GLsizei *lengths, GLchar *messageLog ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES32_glGetDebugMessageLog__ILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_ByteBuffer_2(JNIEnv *env, jclass clazz, jint count, jobject sources_ref, jobject types_ref, jobject ids_ref, jobject severities_ref, jobject lengths_ref, jobject messageLog_ref)
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
	GLuint n = glGetDebugMessageLog((GLuint)count, buf_size, sources, types, ids, severities, lengths, log);
	for (int i = 5; i >= 0; i--)
		release_nio_buffer(env, a[i], e[i]);
	return (jint)n;
}

JNIEXPORT jobjectArray JNICALL Java_android_opengl_GLES32_glGetDebugMessageLog__I_3II_3II_3II_3II(JNIEnv *env, jclass clazz, jint count, jintArray sources_ref, jint sourcesOffset, jintArray types_ref, jint typesOffset, jintArray ids_ref, jint idsOffset, jintArray severities_ref, jint severitiesOffset)
{
	jobjectArray result = NULL;
	jint *sources = get_int_array(env, sources_ref, sourcesOffset, count, "sources");
	jint *types = sources ? get_int_array(env, types_ref, typesOffset, count, "types") : NULL;
	jint *ids = types ? get_int_array(env, ids_ref, idsOffset, count, "ids") : NULL;
	jint *severities = ids ? get_int_array(env, severities_ref, severitiesOffset, count, "severities") : NULL;
	if (severities)
		result = debug_message_log_strings(env, glGetDebugMessageLog, count, (GLenum *)sources + sourcesOffset, (GLenum *)types + typesOffset,
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

JNIEXPORT jobjectArray JNICALL Java_android_opengl_GLES32_glGetDebugMessageLog__ILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint count, jobject sources_ref, jobject types_ref, jobject ids_ref, jobject severities_ref)
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
	jobjectArray result = debug_message_log_strings(env, glGetDebugMessageLog, count, sources, types, ids, severities);
	for (int i = 3; i >= 0; i--)
		release_nio_buffer(env, a[i], e[i]);
	return result;
}

/* void glGetObjectLabel ( GLenum identifier, GLuint name, GLsizei bufSize, GLsizei *length, GLchar *label ) */
JNIEXPORT jstring JNICALL Java_android_opengl_GLES32_glGetObjectLabel__II(JNIEnv *env, jclass clazz, jint identifier, jint name)
{
	return OBJECT_LABEL(env, glGetObjectLabel, (GLenum)identifier, (GLuint)name);
}

/* void glObjectPtrLabel ( const void *ptr, GLsizei length, const GLchar *label ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES32_glObjectPtrLabel__JLjava_lang_String_2(JNIEnv *env, jclass clazz, jlong ptr, jstring label_ref)
{
	const char *label = label_ref ? (*env)->GetStringUTFChars(env, label_ref, NULL) : NULL;
	glObjectPtrLabel((const void *)(intptr_t)ptr, -1, label);
	if (label)
		(*env)->ReleaseStringUTFChars(env, label_ref, label);
}

/* void glGetObjectPtrLabel ( const void *ptr, GLsizei bufSize, GLsizei *length, GLchar *label ) */
JNIEXPORT jstring JNICALL Java_android_opengl_GLES32_glGetObjectPtrLabel__J(JNIEnv *env, jclass clazz, jlong ptr)
{
	return OBJECT_LABEL(env, glGetObjectPtrLabel, (const void *)(intptr_t)ptr);
}
