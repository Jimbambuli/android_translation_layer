/* JNI of android.opengl.GLES31. Generated from AOSP's GLES31.java (its "// C function" comments and native
 * declarations, Android 16) and the Khronos GLES headers, the string and debug functions written by hand. AOSP's
 * own JNI (frameworks/base/core/jni/android_opengl_GLES31.cpp) is generated as well and not in the local sources;
 * the argument checks follow its IllegalArgumentException messages for null arrays/buffers and negative offsets. */
#include "defines.h"
#include "util.h"
#include "android_opengl_GLES3.h"

#include "generated_headers/android_opengl_GLES31.h"

/* void glDispatchCompute ( GLuint num_groups_x, GLuint num_groups_y, GLuint num_groups_z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glDispatchCompute__III(JNIEnv *env, jclass clazz, jint num_groups_x, jint num_groups_y, jint num_groups_z)
{
	glDispatchCompute((GLuint)num_groups_x, (GLuint)num_groups_y, (GLuint)num_groups_z);
}

/* void glDispatchComputeIndirect ( GLintptr indirect ); */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glDispatchComputeIndirect__J(JNIEnv *env, jclass clazz, jlong indirect)
{
	glDispatchComputeIndirect((GLintptr)indirect);
}

/* void glDrawArraysIndirect ( GLenum mode, const void *indirect ); */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glDrawArraysIndirect__IJ(JNIEnv *env, jclass clazz, jint mode, jlong indirect)
{
	glDrawArraysIndirect((GLenum)mode, (const void *)(intptr_t)indirect);
}

/* glDrawElementsIndirect ( GLenum mode, GLenum type, const void *indirect ); */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glDrawElementsIndirect__IIJ(JNIEnv *env, jclass clazz, jint mode, jint type, jlong indirect)
{
	glDrawElementsIndirect((GLenum)mode, (GLenum)type, (const void *)(intptr_t)indirect);
}

/* void glFramebufferParameteri ( GLenum target, GLenum pname, GLint param ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glFramebufferParameteri__III(JNIEnv *env, jclass clazz, jint target, jint pname, jint param)
{
	glFramebufferParameteri((GLenum)target, (GLenum)pname, (GLint)param);
}

/* void glGetFramebufferParameteriv ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetFramebufferParameteriv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	glGetFramebufferParameteriv((GLenum)target, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetFramebufferParameteriv ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetFramebufferParameteriv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetFramebufferParameteriv((GLenum)target, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetProgramInterfaceiv ( GLuint program, GLenum programInterface, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetProgramInterfaceiv__III_3II(JNIEnv *env, jclass clazz, jint program, jint programInterface, jint pname, jintArray params_ref, jint offset)
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
	glGetProgramInterfaceiv((GLuint)program, (GLenum)programInterface, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetProgramInterfaceiv ( GLuint program, GLenum programInterface, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetProgramInterfaceiv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint programInterface, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetProgramInterfaceiv((GLuint)program, (GLenum)programInterface, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* GLuint glGetProgramResourceIndex ( GLuint program, GLenum programInterface, const GLchar *name ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES31_glGetProgramResourceIndex__IILjava_lang_String_2(JNIEnv *env, jclass clazz, jint program, jint programInterface, jstring name_ref)
{
	jint _ret = 0;
	const char *name = NULL;
	if (!name_ref) {
		throw_iae(env, "name == null");
		goto exit;
	}
	name = (*env)->GetStringUTFChars(env, name_ref, NULL);
	_ret = (jint)glGetProgramResourceIndex((GLuint)program, (GLenum)programInterface, (const GLchar *)name);
exit:
	if (name)
		(*env)->ReleaseStringUTFChars(env, name_ref, name);
	return _ret;
}

/* void glGetProgramResourceiv ( GLuint program, GLenum programInterface, GLuint index, GLsizei propCount, const GLenum *props, GLsizei bufSize, GLsizei *length, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetProgramResourceiv__IIII_3III_3II_3II(JNIEnv *env, jclass clazz, jint program, jint programInterface, jint index, jint propCount, jintArray props_ref, jint propsOffset, jint bufSize, jintArray length_ref, jint lengthOffset, jintArray params_ref, jint paramsOffset)
{
	jint *params = NULL;
	jint *length = NULL;
	jint *props = NULL;
	if (!props_ref) {
		throw_iae(env, "props == null");
		goto exit;
	}
	if (propsOffset < 0) {
		throw_iae(env, "propsOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, props_ref) - propsOffset < 1) {
		throw_iae(env, "length - propsOffset < 1 < needed");
		goto exit;
	}
	props = (*env)->GetIntArrayElements(env, props_ref, NULL);
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
	glGetProgramResourceiv((GLuint)program, (GLenum)programInterface, (GLuint)index, (GLsizei)propCount, (const GLenum *)(props + propsOffset), (GLsizei)bufSize, (GLsizei *)(length + lengthOffset), (GLint *)(params + paramsOffset));
exit:
	if (props)
		(*env)->ReleaseIntArrayElements(env, props_ref, props, JNI_ABORT);
	if (length)
		(*env)->ReleaseIntArrayElements(env, length_ref, length, 0);
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetProgramResourceiv ( GLuint program, GLenum programInterface, GLuint index, GLsizei propCount, const GLenum *props, GLsizei bufSize, GLsizei *length, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetProgramResourceiv__IIIILjava_nio_IntBuffer_2ILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint programInterface, jint index, jint propCount, jobject props_ref, jint bufSize, jobject length_ref, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	jarray length_array = NULL;
	jbyte *length_elems = NULL;
	void *length = NULL;
	jarray props_array = NULL;
	jbyte *props_elems = NULL;
	void *props = NULL;
	props = get_nio_buffer(env, props_ref, &props_array, &props_elems);
	if (!length_ref) {
		throw_iae(env, "length == null");
		goto exit;
	}
	length = get_nio_buffer(env, length_ref, &length_array, &length_elems);
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetProgramResourceiv((GLuint)program, (GLenum)programInterface, (GLuint)index, (GLsizei)propCount, (const GLenum *)props, (GLsizei)bufSize, (GLsizei *)length, (GLint *)params);
exit:
	release_nio_buffer(env, props_array, props_elems);
	release_nio_buffer(env, length_array, length_elems);
	release_nio_buffer(env, params_array, params_elems);
}

/* GLint glGetProgramResourceLocation ( GLuint program, GLenum programInterface, const GLchar *name ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES31_glGetProgramResourceLocation__IILjava_lang_String_2(JNIEnv *env, jclass clazz, jint program, jint programInterface, jstring name_ref)
{
	jint _ret = 0;
	const char *name = NULL;
	if (!name_ref) {
		throw_iae(env, "name == null");
		goto exit;
	}
	name = (*env)->GetStringUTFChars(env, name_ref, NULL);
	_ret = (jint)glGetProgramResourceLocation((GLuint)program, (GLenum)programInterface, (const GLchar *)name);
exit:
	if (name)
		(*env)->ReleaseStringUTFChars(env, name_ref, name);
	return _ret;
}

/* void glUseProgramStages ( GLuint pipeline, GLbitfield stages, GLuint program ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glUseProgramStages__III(JNIEnv *env, jclass clazz, jint pipeline, jint stages, jint program)
{
	glUseProgramStages((GLuint)pipeline, (GLbitfield)stages, (GLuint)program);
}

/* void glActiveShaderProgram ( GLuint pipeline, GLuint program ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glActiveShaderProgram__II(JNIEnv *env, jclass clazz, jint pipeline, jint program)
{
	glActiveShaderProgram((GLuint)pipeline, (GLuint)program);
}

/* void glBindProgramPipeline ( GLuint pipeline ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glBindProgramPipeline__I(JNIEnv *env, jclass clazz, jint pipeline)
{
	glBindProgramPipeline((GLuint)pipeline);
}

/* void glDeleteProgramPipelines ( GLsizei n, const GLuint *pipelines ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glDeleteProgramPipelines__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray pipelines_ref, jint offset)
{
	jint *pipelines = NULL;
	if (!pipelines_ref) {
		throw_iae(env, "pipelines == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, pipelines_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	pipelines = (*env)->GetIntArrayElements(env, pipelines_ref, NULL);
	glDeleteProgramPipelines((GLsizei)n, (const GLuint *)(pipelines + offset));
exit:
	if (pipelines)
		(*env)->ReleaseIntArrayElements(env, pipelines_ref, pipelines, JNI_ABORT);
}

/* void glDeleteProgramPipelines ( GLsizei n, const GLuint *pipelines ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glDeleteProgramPipelines__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject pipelines_ref)
{
	jarray pipelines_array = NULL;
	jbyte *pipelines_elems = NULL;
	void *pipelines = NULL;
	pipelines = get_nio_buffer(env, pipelines_ref, &pipelines_array, &pipelines_elems);
	glDeleteProgramPipelines((GLsizei)n, (const GLuint *)pipelines);
	release_nio_buffer(env, pipelines_array, pipelines_elems);
}

/* void glGenProgramPipelines ( GLsizei n, GLuint *pipelines ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGenProgramPipelines__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray pipelines_ref, jint offset)
{
	jint *pipelines = NULL;
	if (!pipelines_ref) {
		throw_iae(env, "pipelines == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, pipelines_ref) - offset < n) {
		throw_iae(env, "length - offset < n < needed");
		goto exit;
	}
	pipelines = (*env)->GetIntArrayElements(env, pipelines_ref, NULL);
	glGenProgramPipelines((GLsizei)n, (GLuint *)(pipelines + offset));
exit:
	if (pipelines)
		(*env)->ReleaseIntArrayElements(env, pipelines_ref, pipelines, 0);
}

/* void glGenProgramPipelines ( GLsizei n, GLuint *pipelines ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGenProgramPipelines__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject pipelines_ref)
{
	jarray pipelines_array = NULL;
	jbyte *pipelines_elems = NULL;
	void *pipelines = NULL;
	if (!pipelines_ref) {
		throw_iae(env, "pipelines == null");
		goto exit;
	}
	pipelines = get_nio_buffer(env, pipelines_ref, &pipelines_array, &pipelines_elems);
	glGenProgramPipelines((GLsizei)n, (GLuint *)pipelines);
exit:
	release_nio_buffer(env, pipelines_array, pipelines_elems);
}

/* GLboolean glIsProgramPipeline ( GLuint pipeline ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES31_glIsProgramPipeline__I(JNIEnv *env, jclass clazz, jint pipeline)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsProgramPipeline((GLuint)pipeline);
	return _ret;
}

/* void glGetProgramPipelineiv ( GLuint pipeline, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetProgramPipelineiv__II_3II(JNIEnv *env, jclass clazz, jint pipeline, jint pname, jintArray params_ref, jint offset)
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
	glGetProgramPipelineiv((GLuint)pipeline, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetProgramPipelineiv ( GLuint pipeline, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetProgramPipelineiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pipeline, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetProgramPipelineiv((GLuint)pipeline, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glProgramUniform1i ( GLuint program, GLint location, GLint v0 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform1i__III(JNIEnv *env, jclass clazz, jint program, jint location, jint v0)
{
	glProgramUniform1i((GLuint)program, (GLint)location, (GLint)v0);
}

/* void glProgramUniform2i ( GLuint program, GLint location, GLint v0, GLint v1 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform2i__IIII(JNIEnv *env, jclass clazz, jint program, jint location, jint v0, jint v1)
{
	glProgramUniform2i((GLuint)program, (GLint)location, (GLint)v0, (GLint)v1);
}

/* void glProgramUniform3i ( GLuint program, GLint location, GLint v0, GLint v1, GLint v2 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform3i__IIIII(JNIEnv *env, jclass clazz, jint program, jint location, jint v0, jint v1, jint v2)
{
	glProgramUniform3i((GLuint)program, (GLint)location, (GLint)v0, (GLint)v1, (GLint)v2);
}

/* void glProgramUniform4i ( GLuint program, GLint location, GLint v0, GLint v1, GLint v2, GLint v3 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform4i__IIIIII(JNIEnv *env, jclass clazz, jint program, jint location, jint v0, jint v1, jint v2, jint v3)
{
	glProgramUniform4i((GLuint)program, (GLint)location, (GLint)v0, (GLint)v1, (GLint)v2, (GLint)v3);
}

/* void glProgramUniform1ui ( GLuint program, GLint location, GLuint v0 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform1ui__III(JNIEnv *env, jclass clazz, jint program, jint location, jint v0)
{
	glProgramUniform1ui((GLuint)program, (GLint)location, (GLuint)v0);
}

/* void glProgramUniform2ui ( GLuint program, GLint location, GLuint v0, GLuint v1 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform2ui__IIII(JNIEnv *env, jclass clazz, jint program, jint location, jint v0, jint v1)
{
	glProgramUniform2ui((GLuint)program, (GLint)location, (GLuint)v0, (GLuint)v1);
}

/* void glProgramUniform3ui ( GLuint program, GLint location, GLuint v0, GLuint v1, GLuint v2 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform3ui__IIIII(JNIEnv *env, jclass clazz, jint program, jint location, jint v0, jint v1, jint v2)
{
	glProgramUniform3ui((GLuint)program, (GLint)location, (GLuint)v0, (GLuint)v1, (GLuint)v2);
}

/* void glProgramUniform4ui ( GLuint program, GLint location, GLuint v0, GLuint v1, GLuint v2, GLuint v3 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform4ui__IIIIII(JNIEnv *env, jclass clazz, jint program, jint location, jint v0, jint v1, jint v2, jint v3)
{
	glProgramUniform4ui((GLuint)program, (GLint)location, (GLuint)v0, (GLuint)v1, (GLuint)v2, (GLuint)v3);
}

/* void glProgramUniform1f ( GLuint program, GLint location, GLfloat v0 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform1f__IIF(JNIEnv *env, jclass clazz, jint program, jint location, jfloat v0)
{
	glProgramUniform1f((GLuint)program, (GLint)location, (GLfloat)v0);
}

/* void glProgramUniform2f ( GLuint program, GLint location, GLfloat v0, GLfloat v1 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform2f__IIFF(JNIEnv *env, jclass clazz, jint program, jint location, jfloat v0, jfloat v1)
{
	glProgramUniform2f((GLuint)program, (GLint)location, (GLfloat)v0, (GLfloat)v1);
}

/* void glProgramUniform3f ( GLuint program, GLint location, GLfloat v0, GLfloat v1, GLfloat v2 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform3f__IIFFF(JNIEnv *env, jclass clazz, jint program, jint location, jfloat v0, jfloat v1, jfloat v2)
{
	glProgramUniform3f((GLuint)program, (GLint)location, (GLfloat)v0, (GLfloat)v1, (GLfloat)v2);
}

/* void glProgramUniform4f ( GLuint program, GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3 ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform4f__IIFFFF(JNIEnv *env, jclass clazz, jint program, jint location, jfloat v0, jfloat v1, jfloat v2, jfloat v3)
{
	glProgramUniform4f((GLuint)program, (GLint)location, (GLfloat)v0, (GLfloat)v1, (GLfloat)v2, (GLfloat)v3);
}

/* void glProgramUniform1iv ( GLuint program, GLint location, GLsizei count, const GLint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform1iv__III_3II(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jintArray value_ref, jint offset)
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
	glProgramUniform1iv((GLuint)program, (GLint)location, (GLsizei)count, (const GLint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniform1iv ( GLuint program, GLint location, GLsizei count, const GLint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform1iv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniform1iv((GLuint)program, (GLint)location, (GLsizei)count, (const GLint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniform2iv ( GLuint program, GLint location, GLsizei count, const GLint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform2iv__III_3II(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jintArray value_ref, jint offset)
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
	glProgramUniform2iv((GLuint)program, (GLint)location, (GLsizei)count, (const GLint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniform2iv ( GLuint program, GLint location, GLsizei count, const GLint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform2iv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniform2iv((GLuint)program, (GLint)location, (GLsizei)count, (const GLint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniform3iv ( GLuint program, GLint location, GLsizei count, const GLint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform3iv__III_3II(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jintArray value_ref, jint offset)
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
	glProgramUniform3iv((GLuint)program, (GLint)location, (GLsizei)count, (const GLint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniform3iv ( GLuint program, GLint location, GLsizei count, const GLint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform3iv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniform3iv((GLuint)program, (GLint)location, (GLsizei)count, (const GLint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniform4iv ( GLuint program, GLint location, GLsizei count, const GLint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform4iv__III_3II(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jintArray value_ref, jint offset)
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
	glProgramUniform4iv((GLuint)program, (GLint)location, (GLsizei)count, (const GLint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniform4iv ( GLuint program, GLint location, GLsizei count, const GLint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform4iv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniform4iv((GLuint)program, (GLint)location, (GLsizei)count, (const GLint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniform1uiv ( GLuint program, GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform1uiv__III_3II(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jintArray value_ref, jint offset)
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
	glProgramUniform1uiv((GLuint)program, (GLint)location, (GLsizei)count, (const GLuint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniform1uiv ( GLuint program, GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform1uiv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniform1uiv((GLuint)program, (GLint)location, (GLsizei)count, (const GLuint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniform2uiv ( GLuint program, GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform2uiv__III_3II(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jintArray value_ref, jint offset)
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
	glProgramUniform2uiv((GLuint)program, (GLint)location, (GLsizei)count, (const GLuint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniform2uiv ( GLuint program, GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform2uiv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniform2uiv((GLuint)program, (GLint)location, (GLsizei)count, (const GLuint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniform3uiv ( GLuint program, GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform3uiv__III_3II(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jintArray value_ref, jint offset)
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
	glProgramUniform3uiv((GLuint)program, (GLint)location, (GLsizei)count, (const GLuint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniform3uiv ( GLuint program, GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform3uiv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniform3uiv((GLuint)program, (GLint)location, (GLsizei)count, (const GLuint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniform4uiv ( GLuint program, GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform4uiv__III_3II(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jintArray value_ref, jint offset)
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
	glProgramUniform4uiv((GLuint)program, (GLint)location, (GLsizei)count, (const GLuint *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseIntArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniform4uiv ( GLuint program, GLint location, GLsizei count, const GLuint *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform4uiv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniform4uiv((GLuint)program, (GLint)location, (GLsizei)count, (const GLuint *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniform1fv ( GLuint program, GLint location, GLsizei count, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform1fv__III_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jfloatArray value_ref, jint offset)
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
	glProgramUniform1fv((GLuint)program, (GLint)location, (GLsizei)count, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniform1fv ( GLuint program, GLint location, GLsizei count, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform1fv__IIILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniform1fv((GLuint)program, (GLint)location, (GLsizei)count, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniform2fv ( GLuint program, GLint location, GLsizei count, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform2fv__III_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jfloatArray value_ref, jint offset)
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
	glProgramUniform2fv((GLuint)program, (GLint)location, (GLsizei)count, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniform2fv ( GLuint program, GLint location, GLsizei count, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform2fv__IIILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniform2fv((GLuint)program, (GLint)location, (GLsizei)count, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniform3fv ( GLuint program, GLint location, GLsizei count, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform3fv__III_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jfloatArray value_ref, jint offset)
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
	glProgramUniform3fv((GLuint)program, (GLint)location, (GLsizei)count, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniform3fv ( GLuint program, GLint location, GLsizei count, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform3fv__IIILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniform3fv((GLuint)program, (GLint)location, (GLsizei)count, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniform4fv ( GLuint program, GLint location, GLsizei count, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform4fv__III_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jfloatArray value_ref, jint offset)
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
	glProgramUniform4fv((GLuint)program, (GLint)location, (GLsizei)count, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniform4fv ( GLuint program, GLint location, GLsizei count, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniform4fv__IIILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniform4fv((GLuint)program, (GLint)location, (GLsizei)count, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniformMatrix2fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix2fv__IIIZ_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
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
	glProgramUniformMatrix2fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniformMatrix2fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix2fv__IIIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniformMatrix2fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniformMatrix3fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix3fv__IIIZ_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
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
	glProgramUniformMatrix3fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniformMatrix3fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix3fv__IIIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniformMatrix3fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniformMatrix4fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix4fv__IIIZ_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
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
	glProgramUniformMatrix4fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniformMatrix4fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix4fv__IIIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniformMatrix4fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniformMatrix2x3fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix2x3fv__IIIZ_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
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
	glProgramUniformMatrix2x3fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniformMatrix2x3fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix2x3fv__IIIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniformMatrix2x3fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniformMatrix3x2fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix3x2fv__IIIZ_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
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
	glProgramUniformMatrix3x2fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniformMatrix3x2fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix3x2fv__IIIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniformMatrix3x2fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniformMatrix2x4fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix2x4fv__IIIZ_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
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
	glProgramUniformMatrix2x4fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniformMatrix2x4fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix2x4fv__IIIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniformMatrix2x4fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniformMatrix4x2fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix4x2fv__IIIZ_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
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
	glProgramUniformMatrix4x2fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniformMatrix4x2fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix4x2fv__IIIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniformMatrix4x2fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniformMatrix3x4fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix3x4fv__IIIZ_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
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
	glProgramUniformMatrix3x4fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniformMatrix3x4fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix3x4fv__IIIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniformMatrix3x4fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glProgramUniformMatrix4x3fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix4x3fv__IIIZ_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
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
	glProgramUniformMatrix4x3fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glProgramUniformMatrix4x3fv ( GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glProgramUniformMatrix4x3fv__IIIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glProgramUniformMatrix4x3fv((GLuint)program, (GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glValidateProgramPipeline ( GLuint pipeline ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glValidateProgramPipeline__I(JNIEnv *env, jclass clazz, jint pipeline)
{
	glValidateProgramPipeline((GLuint)pipeline);
}

/* void glBindImageTexture ( GLuint unit, GLuint texture, GLint level, GLboolean layered, GLint layer, GLenum access, GLenum format ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glBindImageTexture__IIIZIII(JNIEnv *env, jclass clazz, jint unit, jint texture, jint level, jboolean layered, jint layer, jint access, jint format)
{
	glBindImageTexture((GLuint)unit, (GLuint)texture, (GLint)level, (GLboolean)layered, (GLint)layer, (GLenum)access, (GLenum)format);
}

/* void glGetBooleani_v ( GLenum target, GLuint index, GLboolean *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetBooleani_1v__II_3ZI(JNIEnv *env, jclass clazz, jint target, jint index, jbooleanArray data_ref, jint offset)
{
	jboolean *data = NULL;
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
	data = (*env)->GetBooleanArrayElements(env, data_ref, NULL);
	glGetBooleani_v((GLenum)target, (GLuint)index, (GLboolean *)(data + offset));
exit:
	if (data)
		(*env)->ReleaseBooleanArrayElements(env, data_ref, data, 0);
}

/* void glGetBooleani_v ( GLenum target, GLuint index, GLboolean *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetBooleani_1v__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint index, jobject data_ref)
{
	jarray data_array = NULL;
	jbyte *data_elems = NULL;
	void *data = NULL;
	if (!data_ref) {
		throw_iae(env, "data == null");
		goto exit;
	}
	data = get_nio_buffer(env, data_ref, &data_array, &data_elems);
	glGetBooleani_v((GLenum)target, (GLuint)index, (GLboolean *)data);
exit:
	release_nio_buffer(env, data_array, data_elems);
}

/* void glMemoryBarrier ( GLbitfield barriers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glMemoryBarrier__I(JNIEnv *env, jclass clazz, jint barriers)
{
	glMemoryBarrier((GLbitfield)barriers);
}

/* void glMemoryBarrierByRegion ( GLbitfield barriers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glMemoryBarrierByRegion__I(JNIEnv *env, jclass clazz, jint barriers)
{
	glMemoryBarrierByRegion((GLbitfield)barriers);
}

/* void glTexStorage2DMultisample ( GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height, GLboolean fixedsamplelocations ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glTexStorage2DMultisample__IIIIIZ(JNIEnv *env, jclass clazz, jint target, jint samples, jint internalformat, jint width, jint height, jboolean fixedsamplelocations)
{
	glTexStorage2DMultisample((GLenum)target, (GLsizei)samples, (GLenum)internalformat, (GLsizei)width, (GLsizei)height, (GLboolean)fixedsamplelocations);
}

/* void glGetMultisamplefv ( GLenum pname, GLuint index, GLfloat *val ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetMultisamplefv__II_3FI(JNIEnv *env, jclass clazz, jint pname, jint index, jfloatArray val_ref, jint offset)
{
	jfloat *val = NULL;
	if (!val_ref) {
		throw_iae(env, "val == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, val_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	val = (*env)->GetFloatArrayElements(env, val_ref, NULL);
	glGetMultisamplefv((GLenum)pname, (GLuint)index, (GLfloat *)(val + offset));
exit:
	if (val)
		(*env)->ReleaseFloatArrayElements(env, val_ref, val, 0);
}

/* void glGetMultisamplefv ( GLenum pname, GLuint index, GLfloat *val ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetMultisamplefv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint pname, jint index, jobject val_ref)
{
	jarray val_array = NULL;
	jbyte *val_elems = NULL;
	void *val = NULL;
	if (!val_ref) {
		throw_iae(env, "val == null");
		goto exit;
	}
	val = get_nio_buffer(env, val_ref, &val_array, &val_elems);
	glGetMultisamplefv((GLenum)pname, (GLuint)index, (GLfloat *)val);
exit:
	release_nio_buffer(env, val_array, val_elems);
}

/* void glSampleMaski ( GLuint maskNumber, GLbitfield mask ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glSampleMaski__II(JNIEnv *env, jclass clazz, jint maskNumber, jint mask)
{
	glSampleMaski((GLuint)maskNumber, (GLbitfield)mask);
}

/* void glGetTexLevelParameteriv ( GLenum target, GLint level, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetTexLevelParameteriv__III_3II(JNIEnv *env, jclass clazz, jint target, jint level, jint pname, jintArray params_ref, jint offset)
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
	glGetTexLevelParameteriv((GLenum)target, (GLint)level, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetTexLevelParameteriv ( GLenum target, GLint level, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetTexLevelParameteriv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint level, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexLevelParameteriv((GLenum)target, (GLint)level, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetTexLevelParameterfv ( GLenum target, GLint level, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetTexLevelParameterfv__III_3FI(JNIEnv *env, jclass clazz, jint target, jint level, jint pname, jfloatArray params_ref, jint offset)
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
	glGetTexLevelParameterfv((GLenum)target, (GLint)level, (GLenum)pname, (GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, 0);
}

/* void glGetTexLevelParameterfv ( GLenum target, GLint level, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glGetTexLevelParameterfv__IIILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint target, jint level, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetTexLevelParameterfv((GLenum)target, (GLint)level, (GLenum)pname, (GLfloat *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glBindVertexBuffer ( GLuint bindingindex, GLuint buffer, GLintptr offset, GLsizei stride ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glBindVertexBuffer__IIJI(JNIEnv *env, jclass clazz, jint bindingindex, jint buffer, jlong offset, jint stride)
{
	glBindVertexBuffer((GLuint)bindingindex, (GLuint)buffer, (GLintptr)offset, (GLsizei)stride);
}

/* void glVertexAttribFormat ( GLuint attribindex, GLint size, GLenum type, GLboolean normalized, GLuint relativeoffset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glVertexAttribFormat__IIIZI(JNIEnv *env, jclass clazz, jint attribindex, jint size, jint type, jboolean normalized, jint relativeoffset)
{
	glVertexAttribFormat((GLuint)attribindex, (GLint)size, (GLenum)type, (GLboolean)normalized, (GLuint)relativeoffset);
}

/* void glVertexAttribIFormat ( GLuint attribindex, GLint size, GLenum type, GLuint relativeoffset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glVertexAttribIFormat__IIII(JNIEnv *env, jclass clazz, jint attribindex, jint size, jint type, jint relativeoffset)
{
	glVertexAttribIFormat((GLuint)attribindex, (GLint)size, (GLenum)type, (GLuint)relativeoffset);
}

/* void glVertexAttribBinding ( GLuint attribindex, GLuint bindingindex ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glVertexAttribBinding__II(JNIEnv *env, jclass clazz, jint attribindex, jint bindingindex)
{
	glVertexAttribBinding((GLuint)attribindex, (GLuint)bindingindex);
}

/* void glVertexBindingDivisor ( GLuint bindingindex, GLuint divisor ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES31_glVertexBindingDivisor__II(JNIEnv *env, jclass clazz, jint bindingindex, jint divisor)
{
	glVertexBindingDivisor((GLuint)bindingindex, (GLuint)divisor);
}

/* ---- written by hand: strings and string arrays ---- */

/* void glGetProgramResourceName ( GLuint program, GLenum programInterface, GLuint index, GLsizei bufSize, GLsizei *length, GLchar *name ) */
JNIEXPORT jstring JNICALL Java_android_opengl_GLES31_glGetProgramResourceName__III(JNIEnv *env, jclass clazz, jint program, jint programInterface, jint index)
{
	GLenum prop = GL_NAME_LENGTH;
	GLint length = 0;
	glGetProgramResourceiv((GLuint)program, (GLenum)programInterface, (GLuint)index, 1, &prop, 1, NULL, &length);
	GLchar *name = calloc(length + 1, 1);
	glGetProgramResourceName((GLuint)program, (GLenum)programInterface, (GLuint)index, length + 1, NULL, name);
	jstring result = (*env)->NewStringUTF(env, name);
	free(name);
	return result;
}

/* GLuint glCreateShaderProgramv ( GLenum type, GLsizei count, const GLchar *const *strings ) */
JNIEXPORT jint JNICALL Java_android_opengl_GLES31_glCreateShaderProgramv__I_3Ljava_lang_String_2(JNIEnv *env, jclass clazz, jint type, jobjectArray strings_ref)
{
	if (!strings_ref) {
		throw_iae(env, "strings == null");
		return 0;
	}
	jsize count;
	const GLchar **strings = get_strings(env, strings_ref, &count);
	GLuint program = glCreateShaderProgramv((GLenum)type, count, strings);
	release_strings(env, strings_ref, strings, count);
	return (jint)program;
}

/* void glGetProgramPipelineInfoLog ( GLuint pipeline, GLsizei bufSize, GLsizei *length, GLchar *infoLog ) */
JNIEXPORT jstring JNICALL Java_android_opengl_GLES31_glGetProgramPipelineInfoLog__I(JNIEnv *env, jclass clazz, jint pipeline)
{
	GLint length = 0;
	glGetProgramPipelineiv((GLuint)pipeline, GL_INFO_LOG_LENGTH, &length);
	GLchar *log = calloc(length + 1, 1);
	glGetProgramPipelineInfoLog((GLuint)pipeline, length + 1, NULL, log);
	jstring result = (*env)->NewStringUTF(env, log);
	free(log);
	return result;
}
