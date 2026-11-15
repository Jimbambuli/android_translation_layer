#include "defines.h"
#include "util.h"
#include <GLES2/gl2.h>
#include <jni.h>
#include <stdint.h>

#include "../libandroid/native_window.h"
#include "android_opengl_GLES3.h"

#include "generated_headers/android_opengl_GLES20.h"

JNIEXPORT jstring JNICALL Java_android_opengl_GLES20_glGetString(JNIEnv *env, jclass, jint name)
{
	const char *chars = (const char *)glGetString((GLenum)name);
	return _JSTRING(chars);
}

JNIEXPORT jint JNICALL Java_android_opengl_GLES20_glGetError(JNIEnv *env, jclass)
{
	return (jint)glGetError();
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetIntegerv__I_3II(JNIEnv *env, jclass, jint pname, jintArray params_ref, jint offset)
{
	jint *params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glGetIntegerv((GLenum)pname, params + offset);
	(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glEnableVertexAttribArray(JNIEnv *env, jclass, jint index)
{
	glEnableVertexAttribArray((GLuint)index);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttribPointerBounds(JNIEnv *env, jclass, jint index, jint size, jint type, jboolean normalized, jint stride, jobject pointer, jint count)
{
	jarray array_ref;
	jbyte *array;
	GLvoid *pixels = get_nio_buffer(env, pointer, &array_ref, &array);

	glVertexAttribPointer(index, size, type, normalized, stride, pixels);
	release_nio_buffer(env, array_ref, array);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDisable(JNIEnv *env, jclass, jint cap)
{
	glDisable((GLenum)cap);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glActiveTexture(JNIEnv *env, jclass, jint texture)
{
	glActiveTexture((GLenum)texture);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glEnable(JNIEnv *env, jclass, jint cap)
{
	glEnable((GLenum)cap);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glFrontFace(JNIEnv *env, jclass, jint mode)
{
	glFrontFace((GLenum)mode);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glViewport(JNIEnv *env, jclass, jint x, jint y, jint width, jint height)
{
	glViewport((GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGenTextures__I_3II(JNIEnv *env, jclass, jint n, jintArray textures_ref, jint offset)
{
	jint *textures = (*env)->GetIntArrayElements(env, textures_ref, NULL);
	glGenTextures((GLsizei)n, (GLuint *)textures + offset);
	(*env)->ReleaseIntArrayElements(env, textures_ref, textures, 0);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glBindTexture(JNIEnv *env, jclass, jint target, jint texture)
{
	glBindTexture((GLenum)target, (GLuint)texture);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glTexImage2D(JNIEnv *env, jclass, jint target, jint level, jint internalformat, jint width, jint height, jint border, jint format, jint type, jobject pixels_buf)
{
	jarray array_ref;
	jbyte *array;
	GLvoid *pixels = get_nio_buffer(env, pixels_buf, &array_ref, &array);
	glTexImage2D((GLenum)target, (GLint)level, (GLint)internalformat, (GLsizei)width, (GLsizei)height, (GLint)border, (GLenum)format, (GLenum)type, pixels);
	release_nio_buffer(env, array_ref, array);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glTexSubImage2D(JNIEnv *env, jclass, jint target, jint level, jint xoffset, jint yoffset, jint width, jint height, jint format, jint type, jobject pixels_buf)
{
	jarray array_ref;
	jbyte *array;
	GLvoid *pixels = get_nio_buffer(env, pixels_buf, &array_ref, &array);
	glTexSubImage2D((GLenum)target, (GLint)level, (GLint)xoffset, (GLint)yoffset, (GLsizei)width, (GLsizei)height, (GLenum)format, (GLenum)type, pixels);
	release_nio_buffer(env, array_ref, array);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glTexParameterf(JNIEnv *env, jclass, jint target, jint pname, jfloat param)
{
	glTexParameterf((GLenum)target, (GLenum)pname, (GLfloat)param);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGenBuffers__I_3II(JNIEnv *env, jclass, jint n, jintArray buffers_ref, jint offset)
{
	jint *buffers = (*env)->GetIntArrayElements(env, buffers_ref, NULL);
	glGenBuffers((GLsizei)n, (GLuint *)buffers + offset);
	(*env)->ReleaseIntArrayElements(env, buffers_ref, buffers, 0);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glBindBuffer(JNIEnv *env, jclass, jint target, jint buffer)
{
	glBindBuffer((GLenum)target, (GLuint)buffer);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glBufferData(JNIEnv *env, jclass, jint target, jint size, jobject data_buf, jint usage)
{
	jarray array_ref;
	jbyte *array;
	GLvoid *data = get_nio_buffer(env, data_buf, &array_ref, &array);
	glBufferData((GLenum)target, (GLsizeiptr)size, data, (GLenum)usage);
	release_nio_buffer(env, array_ref, array);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDisableVertexAttribArray(JNIEnv *env, jclass, jint index)
{
	glDisableVertexAttribArray((GLuint)index);
}

JNIEXPORT jint JNICALL Java_android_opengl_GLES20_glCreateShader(JNIEnv *env, jclass, jint type)
{
	return (jint)glCreateShader((GLenum)type);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glShaderSource(JNIEnv *env, jclass, jint shader, jstring string)
{
	const char *nativeString = (*env)->GetStringUTFChars(env, string, NULL);
	const char *strings[] = {nativeString};
	glShaderSource(shader, 1, strings, 0);
	(*env)->ReleaseStringUTFChars(env, string, nativeString);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glCompileShader(JNIEnv *env, jclass, jint shader)
{
	glCompileShader((GLuint)shader);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetShaderiv__II_3II(JNIEnv *env, jclass, jint shader, jint pname, jintArray params_ref, jint offset)
{
	jint *params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glGetShaderiv((GLuint)shader, (GLenum)pname, params + offset);
	(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

JNIEXPORT jint JNICALL Java_android_opengl_GLES20_glCreateProgram(JNIEnv *env, jclass)
{
	return (jint)glCreateProgram();
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glAttachShader(JNIEnv *env, jclass, jint program, jint shader)
{
	glAttachShader((GLuint)program, (GLuint)shader);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glBindAttribLocation(JNIEnv *env, jclass, jint program, jint index, jstring name)
{
	const char *nativeName = (*env)->GetStringUTFChars(env, name, NULL);
	glBindAttribLocation((GLuint)program, (GLuint)index, nativeName);
	(*env)->ReleaseStringUTFChars(env, name, nativeName);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glLinkProgram(JNIEnv *env, jclass, jint program)
{
	glLinkProgram((GLuint)program);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetProgramiv__II_3II(JNIEnv *env, jclass, jint program, jint pname, jintArray params_ref, jint offset)
{
	jint *params = (*env)->GetIntArrayElements(env, params_ref, NULL);
	glGetProgramiv((GLuint)program, (GLenum)pname, params + offset);
	(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetActiveAttrib__III_3II_3II_3II_3BI(JNIEnv *env, jclass, jint program, jint index, jint bufsize, jintArray length_ref, jint lengthOffset, jintArray size_ref, jint sizeOffset, jintArray type_ref, jint typeOffset, jbyteArray name_ref, jint nameOffset)
{
	jint *length = (*env)->GetIntArrayElements(env, length_ref, NULL);
	jint *size = (*env)->GetIntArrayElements(env, size_ref, NULL);
	jint *type = (*env)->GetIntArrayElements(env, type_ref, NULL);
	jbyte *name = (*env)->GetByteArrayElements(env, name_ref, NULL);
	glGetActiveAttrib((GLuint)program, (GLuint)index, (GLsizei)bufsize, (GLsizei *)length + lengthOffset, (GLint *)size + sizeOffset, (GLenum *)type + typeOffset, (char *)name + nameOffset);
	(*env)->ReleaseByteArrayElements(env, name_ref, name, 0);
	(*env)->ReleaseIntArrayElements(env, type_ref, type, 0);
	(*env)->ReleaseIntArrayElements(env, size_ref, size, 0);
	(*env)->ReleaseIntArrayElements(env, length_ref, length, 0);
}

JNIEXPORT jint JNICALL Java_android_opengl_GLES20_glGetAttribLocation(JNIEnv *env, jclass, jint program, jstring name)
{
	const char *nativeName = (*env)->GetStringUTFChars(env, name, NULL);
	jint ret = glGetAttribLocation((GLuint)program, nativeName);
	(*env)->ReleaseStringUTFChars(env, name, nativeName);
	return ret;
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetActiveUniform__III_3II_3II_3II_3BI(JNIEnv *env, jclass, jint program, jint index, jint bufsize, jintArray length_ref, jint lengthOffset, jintArray size_ref, jint sizeOffset, jintArray type_ref, jint typeOffset, jbyteArray name_ref, jint nameOffset)
{
	jint *length = (*env)->GetIntArrayElements(env, length_ref, NULL);
	jint *size = (*env)->GetIntArrayElements(env, size_ref, NULL);
	jint *type = (*env)->GetIntArrayElements(env, type_ref, NULL);
	jbyte *name = (*env)->GetByteArrayElements(env, name_ref, NULL);
	glGetActiveUniform((GLuint)program, (GLuint)index, (GLsizei)bufsize, (GLsizei *)length + lengthOffset, (GLint *)size + sizeOffset, (GLenum *)type + typeOffset, (char *)name + nameOffset);
	(*env)->ReleaseByteArrayElements(env, name_ref, name, 0);
	(*env)->ReleaseIntArrayElements(env, type_ref, type, 0);
	(*env)->ReleaseIntArrayElements(env, size_ref, size, 0);
	(*env)->ReleaseIntArrayElements(env, length_ref, length, 0);
}

JNIEXPORT jint JNICALL Java_android_opengl_GLES20_glGetUniformLocation(JNIEnv *env, jclass, jint program, jstring name)
{
	const char *nativeName = (*env)->GetStringUTFChars(env, name, NULL);
	jint ret = glGetUniformLocation((GLuint)program, nativeName);
	(*env)->ReleaseStringUTFChars(env, name, nativeName);
	return ret;
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDeleteShader(JNIEnv *env, jclass, jint shader)
{
	glDeleteShader((GLuint)shader);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUseProgram(JNIEnv *env, jclass, jint program)
{
	glUseProgram((GLuint)program);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttribPointer(JNIEnv *env, jclass, jint indx, jint size, jint type, jboolean normalized, jint stride, jint offset)
{
	glVertexAttribPointer((GLuint)indx, (GLint)size, (GLenum)type, (GLboolean)normalized, (GLsizei)stride, (GLvoid *)(intptr_t)offset);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniformMatrix4fv__IIZ_3FI(JNIEnv *env, jclass, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
{
	jfloat *value = (*env)->GetFloatArrayElements(env, value_ref, NULL);
	glUniformMatrix4fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (GLfloat *)value);
	(*env)->ReleaseFloatArrayElements(env, value_ref, value, 0);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform1i(JNIEnv *env, jclass, jint location, jint x)
{
	glUniform1i((GLint)location, (GLint)x);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform4f(JNIEnv *env, jclass, jint location, jfloat x, jfloat y, jfloat z, jfloat w)
{
	glUniform4f((GLint)location, (GLfloat)x, (GLfloat)y, (GLfloat)z, (GLfloat)w);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDrawArrays(JNIEnv *env, jclass, jint mode, jint first, jint count)
{
	glDrawArrays((GLenum)mode, (GLint)first, (GLsizei)count);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glClearColor(JNIEnv *env, jclass, jfloat red, jfloat green, jfloat blue, jfloat alpha)
{
	glClearColor((GLclampf)red, (GLclampf)green, (GLclampf)blue, (GLclampf)alpha);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glClear(JNIEnv *env, jclass, jint mask)
{
	glClear((GLbitfield)mask);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glBlendFunc(JNIEnv *env, jclass, jint sfactor, jint dfactor)
{
	glBlendFunc((GLenum)sfactor, (GLenum)dfactor);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetFloatv__I_3FI(JNIEnv *env, jclass this, jint pname, jfloatArray params_ref, jint offset)
{
	GLfloat *params_base = (GLfloat *)(*env)->GetPrimitiveArrayCritical(env, params_ref, 0);
	GLfloat *params = params_base + offset;

	glGetFloatv((GLenum)pname, params);

	(*env)->ReleasePrimitiveArrayCritical(env, params_ref, params_base, 0);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glFlush(JNIEnv *env, jclass this)
{
	glFlush();
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glReadPixels(JNIEnv *env, jclass this, jint x, jint y, jint width, jint height, jint format, jint type, jobject pixels_buf)
{
	jarray array_ref;
	jbyte *array;
	GLvoid *pixels = get_nio_buffer(env, pixels_buf, &array_ref, &array);
	glReadPixels(x, y, width, height, format, type, pixels);
	release_nio_buffer(env, array_ref, array);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glPixelStorei(JNIEnv *env, jclass this, jint pname, jint param)
{
	glPixelStorei((GLenum)pname, (GLint)param);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glTexParameteri(JNIEnv *env, jclass this, jint target, jint pname, jint param)
{
	glTexParameteri((GLenum)target, (GLenum)pname, (GLint)param);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetShaderiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass this, jint shader, jint pname, jobject params_buf)
{
	jarray array_ref;
	jbyte *array;
	GLvoid *params = get_nio_buffer(env, params_buf, &array_ref, &array);
	glGetShaderiv((GLuint)shader, (GLenum)pname, (GLint *)params);
	release_nio_buffer(env, array_ref, array);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetProgramiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass this, jint program, jint pname, jobject params_buf)
{
	jarray array_ref;
	jbyte *array;
	GLvoid *params = get_nio_buffer(env, params_buf, &array_ref, &array);
	glGetProgramiv((GLuint)program, (GLenum)pname, (GLint *)params);
	release_nio_buffer(env, array_ref, array);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDepthMask(JNIEnv *env, jclass this, jboolean flag)
{
	glDepthMask((GLboolean)flag);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glBlendFuncSeparate(JNIEnv *env, jclass this, jint srcRGB, jint dstRGB, jint srcAlpha, jint dstAlpha)
{
	glBlendFuncSeparate((GLenum)srcRGB, (GLenum)dstRGB, (GLenum)srcAlpha, (GLenum)dstAlpha);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGenFramebuffers__I_3II(JNIEnv *env, jclass this, jint n, jintArray framebuffers_ref, jint offset)
{
	GLuint *framebuffers = (*env)->GetPrimitiveArrayCritical(env, framebuffers_ref, 0);
	glGenFramebuffers((GLsizei)n, framebuffers + offset);
	(*env)->ReleasePrimitiveArrayCritical(env, framebuffers_ref, framebuffers, 0);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glBindFramebuffer(JNIEnv *env, jclass this, jint target, jint framebuffer)
{
	bionic_glBindFramebuffer((GLenum)target, (GLuint)framebuffer);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glFramebufferTexture2D(JNIEnv *env, jclass this, jint target, jint attachment, jint textarget, jint texture, jint level)
{
	glFramebufferTexture2D((GLenum)target, (GLenum)attachment, (GLenum)textarget, (GLuint)texture, (GLint)level);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glBindRenderbuffer(JNIEnv *env, jclass this, jint target, jint renderbuffer)
{
	glBindRenderbuffer((GLenum)target, (GLuint)renderbuffer);
}

JNIEXPORT jint JNICALL Java_android_opengl_GLES20_glCheckFramebufferStatus(JNIEnv *env, jclass this, jint target)
{
	return (jint)glCheckFramebufferStatus((GLenum)target);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDeleteFramebuffers__I_3II(JNIEnv *env, jclass this, jint n, jintArray framebuffers_ref, jint offset)
{
	GLuint *framebuffers = (*env)->GetPrimitiveArrayCritical(env, framebuffers_ref, 0);
	glDeleteFramebuffers((GLsizei)n, framebuffers + offset);
	(*env)->ReleasePrimitiveArrayCritical(env, framebuffers_ref, framebuffers, 0);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDeleteProgram(JNIEnv *env, jclass this, jint program)
{
	glDeleteProgram((GLuint)program);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetFloatv__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass this, jint pname, jobject params_buf)
{
	jarray array_ref;
	jbyte *array;
	GLvoid *params = get_nio_buffer(env, params_buf, &array_ref, &array);
	glGetFloatv((GLenum)pname, (GLfloat *)params);
	release_nio_buffer(env, array_ref, array);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGenerateMipmap(JNIEnv *env, jclass this, jint pname)
{
	glGenerateMipmap((GLenum)pname);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glLineWidth(JNIEnv *env, jclass this, jfloat width)
{
	glLineWidth((GLfloat)width);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glColorMask(JNIEnv *env, jclass this, jboolean red, jboolean green, jboolean blue, jboolean alpha)
{
	glColorMask((GLboolean)red, (GLboolean)green, (GLboolean)blue, (GLboolean)alpha);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glBufferSubData(JNIEnv *env, jclass this, jint target, jint offset, jint size, jobject data)
{
	glBufferSubData((GLenum)target, (GLintptr)offset, (GLsizeiptr)size, (void *)data);
}

JNIEXPORT jstring JNICALL Java_android_opengl_GLES20_glGetShaderInfoLog(JNIEnv *env, jclass this, jint shader)
{
	GLsizei bufSize;
	glGetShaderiv((GLuint)shader, GL_INFO_LOG_LENGTH, &bufSize);

	jstring output;
	if (bufSize == 0) {
		char cstring = 0;
		output = _JSTRING(&cstring);
	} else {
		GLchar *infoLog = malloc(sizeof(GLchar) * bufSize + 1);
		GLsizei length;
		glGetShaderInfoLog((GLuint)shader, bufSize, &length, infoLog);
		output = _JSTRING(infoLog);
		free(infoLog);
	}
	return output;
}

JNIEXPORT jstring JNICALL Java_android_opengl_GLES20_glGetActiveUniform__IILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2(JNIEnv *env, jclass this, jint program, jint index, jobject buffer_1, jobject buffer_2)
{
	char *name = malloc(512);
	glGetActiveUniform((GLuint)program, (GLuint)index, 512, NULL, NULL, NULL, name);
	jstring output = _JSTRING(name);
	free(name);
	return output;
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetIntegerv__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass this, jint pname, jobject params_buf)
{
	jarray array_ref;
	jbyte *array;
	GLvoid *params = get_nio_buffer(env, params_buf, &array_ref, &array);
	glGetIntegerv((GLenum)pname, (GLint *)params);
	release_nio_buffer(env, array_ref, array);
}

JNIEXPORT jstring JNICALL Java_android_opengl_GLES20_glGetProgramInfoLog(JNIEnv *env, jclass this, jint program)
{
	GLint bufSize = 0;
	glGetProgramiv((GLuint)program, GL_INFO_LOG_LENGTH, &bufSize);
	GLchar *infoLog = calloc(bufSize + 1, sizeof(GLchar));
	if (bufSize > 0)
		glGetProgramInfoLog((GLuint)program, bufSize, NULL, infoLog);
	jstring output = _JSTRING(infoLog);
	free(infoLog);
	return output;
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glScissor(JNIEnv *env, jclass this, jint x, jint y, jint width, jint height)
{
	glScissor((GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform1f(JNIEnv *env, jclass this, jint location, jfloat x)
{
	glUniform1f((GLint)location, (GLfloat)x);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDeleteBuffers__I_3II(JNIEnv *env, jclass this, jint n, jintArray buffers_ref, jint offset)
{
	jint *buffers = (*env)->GetIntArrayElements(env, buffers_ref, NULL);
	glDeleteBuffers((GLsizei)n, (GLuint *)buffers + offset);
	(*env)->ReleaseIntArrayElements(env, buffers_ref, buffers, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGenRenderbuffers__I_3II(JNIEnv *env, jclass this, jint n, jintArray renderbuffers_ref, jint offset)
{
	jint *renderbuffers = (*env)->GetIntArrayElements(env, renderbuffers_ref, NULL);
	glGenRenderbuffers((GLsizei)n, (GLuint *)renderbuffers + offset);
	(*env)->ReleaseIntArrayElements(env, renderbuffers_ref, renderbuffers, 0);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDeleteRenderbuffers__I_3II(JNIEnv *env, jclass this, jint n, jintArray renderbuffers_ref, jint offset)
{
	jint *renderbuffers = (*env)->GetIntArrayElements(env, renderbuffers_ref, NULL);
	glDeleteRenderbuffers((GLsizei)n, (GLuint *)renderbuffers + offset);
	(*env)->ReleaseIntArrayElements(env, renderbuffers_ref, renderbuffers, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glRenderbufferStorage(JNIEnv *env, jclass this, jint target, jint internalformat, jint width, jint height)
{
	glRenderbufferStorage((GLenum)target, (GLenum)internalformat, (GLsizei)width, (GLsizei)height);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glFramebufferRenderbuffer(JNIEnv *env, jclass this, jint target, jint attachment, jint renderbuffertarget, jint renderbuffer)
{
	glFramebufferRenderbuffer((GLenum)target, (GLenum)attachment, (GLenum)renderbuffertarget, (GLuint)renderbuffer);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES20_glCompressedTexImage2D(JNIEnv *env, jclass this, jint target, jint level, jint internalformat, jint width, jint height, jint border, jint imageSize, jobject data_buf)
{
	jarray array_ref;
	jbyte *array;
	GLvoid *data = get_nio_buffer(env, data_buf, &array_ref, &array);
	glCompressedTexImage2D((GLenum)target, (GLint)level, (GLenum)internalformat, (GLsizei)width, (GLsizei)height, (GLint)border, (GLsizei)imageSize, data);
	release_nio_buffer(env, array_ref, array);
}

/* ---- the remaining GLES20 natives, generated like android_opengl_GLES30.c (AOSP "// C function" comments,
 * Khronos prototypes); glDeleteTextures and glDrawElements replaced: their short JNI names bound both overloads
 * to one implementation, which also scaled the offset by 4 and passed the type as count ---- */

/* void glBlendColor ( GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glBlendColor__FFFF(JNIEnv *env, jclass clazz, jfloat red, jfloat green, jfloat blue, jfloat alpha)
{
	glBlendColor((GLfloat)red, (GLfloat)green, (GLfloat)blue, (GLfloat)alpha);
}

/* void glBlendEquation ( GLenum mode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glBlendEquation__I(JNIEnv *env, jclass clazz, jint mode)
{
	glBlendEquation((GLenum)mode);
}

/* void glBlendEquationSeparate ( GLenum modeRGB, GLenum modeAlpha ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glBlendEquationSeparate__II(JNIEnv *env, jclass clazz, jint modeRGB, jint modeAlpha)
{
	glBlendEquationSeparate((GLenum)modeRGB, (GLenum)modeAlpha);
}

/* void glClearDepthf ( GLclampf depth ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glClearDepthf__F(JNIEnv *env, jclass clazz, jfloat depth)
{
	glClearDepthf((GLfloat)depth);
}

/* void glClearStencil ( GLint s ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glClearStencil__I(JNIEnv *env, jclass clazz, jint s)
{
	glClearStencil((GLint)s);
}

/* void glCompressedTexSubImage2D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid *data ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glCompressedTexSubImage2D__IIIIIIIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint target, jint level, jint xoffset, jint yoffset, jint width, jint height, jint format, jint imageSize, jobject data_ref)
{
	jarray data_array = NULL;
	jbyte *data_elems = NULL;
	void *data = NULL;
	data = get_nio_buffer(env, data_ref, &data_array, &data_elems);
	glCompressedTexSubImage2D((GLenum)target, (GLint)level, (GLint)xoffset, (GLint)yoffset, (GLsizei)width, (GLsizei)height, (GLenum)format, (GLsizei)imageSize, (const void *)data);
	release_nio_buffer(env, data_array, data_elems);
}

/* void glCopyTexImage2D ( GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glCopyTexImage2D__IIIIIIII(JNIEnv *env, jclass clazz, jint target, jint level, jint internalformat, jint x, jint y, jint width, jint height, jint border)
{
	glCopyTexImage2D((GLenum)target, (GLint)level, (GLenum)internalformat, (GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height, (GLint)border);
}

/* void glCopyTexSubImage2D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glCopyTexSubImage2D__IIIIIIII(JNIEnv *env, jclass clazz, jint target, jint level, jint xoffset, jint yoffset, jint x, jint y, jint width, jint height)
{
	glCopyTexSubImage2D((GLenum)target, (GLint)level, (GLint)xoffset, (GLint)yoffset, (GLint)x, (GLint)y, (GLsizei)width, (GLsizei)height);
}

/* void glCullFace ( GLenum mode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glCullFace__I(JNIEnv *env, jclass clazz, jint mode)
{
	glCullFace((GLenum)mode);
}

/* void glDeleteBuffers ( GLsizei n, const GLuint *buffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDeleteBuffers__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject buffers_ref)
{
	jarray buffers_array = NULL;
	jbyte *buffers_elems = NULL;
	void *buffers = NULL;
	buffers = get_nio_buffer(env, buffers_ref, &buffers_array, &buffers_elems);
	glDeleteBuffers((GLsizei)n, (const GLuint *)buffers);
	release_nio_buffer(env, buffers_array, buffers_elems);
}

/* void glDeleteFramebuffers ( GLsizei n, const GLuint *framebuffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDeleteFramebuffers__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject framebuffers_ref)
{
	jarray framebuffers_array = NULL;
	jbyte *framebuffers_elems = NULL;
	void *framebuffers = NULL;
	framebuffers = get_nio_buffer(env, framebuffers_ref, &framebuffers_array, &framebuffers_elems);
	glDeleteFramebuffers((GLsizei)n, (const GLuint *)framebuffers);
	release_nio_buffer(env, framebuffers_array, framebuffers_elems);
}

/* void glDeleteRenderbuffers ( GLsizei n, const GLuint *renderbuffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDeleteRenderbuffers__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject renderbuffers_ref)
{
	jarray renderbuffers_array = NULL;
	jbyte *renderbuffers_elems = NULL;
	void *renderbuffers = NULL;
	renderbuffers = get_nio_buffer(env, renderbuffers_ref, &renderbuffers_array, &renderbuffers_elems);
	glDeleteRenderbuffers((GLsizei)n, (const GLuint *)renderbuffers);
	release_nio_buffer(env, renderbuffers_array, renderbuffers_elems);
}

/* void glDeleteTextures ( GLsizei n, const GLuint *textures ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDeleteTextures__I_3II(JNIEnv *env, jclass clazz, jint n, jintArray textures_ref, jint offset)
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
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDeleteTextures__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject textures_ref)
{
	jarray textures_array = NULL;
	jbyte *textures_elems = NULL;
	void *textures = NULL;
	textures = get_nio_buffer(env, textures_ref, &textures_array, &textures_elems);
	glDeleteTextures((GLsizei)n, (const GLuint *)textures);
	release_nio_buffer(env, textures_array, textures_elems);
}

/* void glDepthFunc ( GLenum func ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDepthFunc__I(JNIEnv *env, jclass clazz, jint func)
{
	glDepthFunc((GLenum)func);
}

/* void glDepthRangef ( GLclampf zNear, GLclampf zFar ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDepthRangef__FF(JNIEnv *env, jclass clazz, jfloat zNear, jfloat zFar)
{
	glDepthRangef((GLfloat)zNear, (GLfloat)zFar);
}

/* void glDetachShader ( GLuint program, GLuint shader ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDetachShader__II(JNIEnv *env, jclass clazz, jint program, jint shader)
{
	glDetachShader((GLuint)program, (GLuint)shader);
}

/* void glDrawElements ( GLenum mode, GLsizei count, GLenum type, GLint offset ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDrawElements__IIII(JNIEnv *env, jclass clazz, jint mode, jint count, jint type, jint offset)
{
	glDrawElements((GLenum)mode, (GLsizei)count, (GLenum)type, (const void *)(intptr_t)offset);
}

/* void glDrawElements ( GLenum mode, GLsizei count, GLenum type, const GLvoid *indices ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glDrawElements__IIILjava_nio_Buffer_2(JNIEnv *env, jclass clazz, jint mode, jint count, jint type, jobject indices_ref)
{
	jarray indices_array = NULL;
	jbyte *indices_elems = NULL;
	void *indices = NULL;
	indices = get_nio_buffer(env, indices_ref, &indices_array, &indices_elems);
	glDrawElements((GLenum)mode, (GLsizei)count, (GLenum)type, (const void *)indices);
	release_nio_buffer(env, indices_array, indices_elems);
}

/* void glFinish ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glFinish__(JNIEnv *env, jclass clazz)
{
	glFinish();
}

/* void glGenBuffers ( GLsizei n, GLuint *buffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGenBuffers__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject buffers_ref)
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

/* void glGenFramebuffers ( GLsizei n, GLuint *framebuffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGenFramebuffers__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject framebuffers_ref)
{
	jarray framebuffers_array = NULL;
	jbyte *framebuffers_elems = NULL;
	void *framebuffers = NULL;
	if (!framebuffers_ref) {
		throw_iae(env, "framebuffers == null");
		goto exit;
	}
	framebuffers = get_nio_buffer(env, framebuffers_ref, &framebuffers_array, &framebuffers_elems);
	glGenFramebuffers((GLsizei)n, (GLuint *)framebuffers);
exit:
	release_nio_buffer(env, framebuffers_array, framebuffers_elems);
}

/* void glGenRenderbuffers ( GLsizei n, GLuint *renderbuffers ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGenRenderbuffers__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject renderbuffers_ref)
{
	jarray renderbuffers_array = NULL;
	jbyte *renderbuffers_elems = NULL;
	void *renderbuffers = NULL;
	if (!renderbuffers_ref) {
		throw_iae(env, "renderbuffers == null");
		goto exit;
	}
	renderbuffers = get_nio_buffer(env, renderbuffers_ref, &renderbuffers_array, &renderbuffers_elems);
	glGenRenderbuffers((GLsizei)n, (GLuint *)renderbuffers);
exit:
	release_nio_buffer(env, renderbuffers_array, renderbuffers_elems);
}

/* void glGenTextures ( GLsizei n, GLuint *textures ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGenTextures__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint n, jobject textures_ref)
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

/* void glGetAttachedShaders ( GLuint program, GLsizei maxcount, GLsizei *count, GLuint *shaders ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetAttachedShaders__II_3II_3II(JNIEnv *env, jclass clazz, jint program, jint maxcount, jintArray count_ref, jint countOffset, jintArray shaders_ref, jint shadersOffset)
{
	jint *shaders = NULL;
	jint *count = NULL;
	if (!count_ref) {
		throw_iae(env, "count == null");
		goto exit;
	}
	if (countOffset < 0) {
		throw_iae(env, "countOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, count_ref) - countOffset < 1) {
		throw_iae(env, "length - countOffset < 1 < needed");
		goto exit;
	}
	count = (*env)->GetIntArrayElements(env, count_ref, NULL);
	if (!shaders_ref) {
		throw_iae(env, "shaders == null");
		goto exit;
	}
	if (shadersOffset < 0) {
		throw_iae(env, "shadersOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, shaders_ref) - shadersOffset < 1) {
		throw_iae(env, "length - shadersOffset < 1 < needed");
		goto exit;
	}
	shaders = (*env)->GetIntArrayElements(env, shaders_ref, NULL);
	glGetAttachedShaders((GLuint)program, (GLsizei)maxcount, (GLsizei *)(count + countOffset), (GLuint *)(shaders + shadersOffset));
exit:
	if (count)
		(*env)->ReleaseIntArrayElements(env, count_ref, count, 0);
	if (shaders)
		(*env)->ReleaseIntArrayElements(env, shaders_ref, shaders, 0);
}

/* void glGetAttachedShaders ( GLuint program, GLsizei maxcount, GLsizei *count, GLuint *shaders ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetAttachedShaders__IILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint maxcount, jobject count_ref, jobject shaders_ref)
{
	jarray shaders_array = NULL;
	jbyte *shaders_elems = NULL;
	void *shaders = NULL;
	jarray count_array = NULL;
	jbyte *count_elems = NULL;
	void *count = NULL;
	if (!count_ref) {
		throw_iae(env, "count == null");
		goto exit;
	}
	count = get_nio_buffer(env, count_ref, &count_array, &count_elems);
	if (!shaders_ref) {
		throw_iae(env, "shaders == null");
		goto exit;
	}
	shaders = get_nio_buffer(env, shaders_ref, &shaders_array, &shaders_elems);
	glGetAttachedShaders((GLuint)program, (GLsizei)maxcount, (GLsizei *)count, (GLuint *)shaders);
exit:
	release_nio_buffer(env, count_array, count_elems);
	release_nio_buffer(env, shaders_array, shaders_elems);
}

/* void glGetBooleanv ( GLenum pname, GLboolean *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetBooleanv__I_3ZI(JNIEnv *env, jclass clazz, jint pname, jbooleanArray params_ref, jint offset)
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
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetBooleanv__ILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint pname, jobject params_ref)
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
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetBufferParameteriv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetBufferParameteriv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
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

/* void glGetFramebufferAttachmentParameteriv ( GLenum target, GLenum attachment, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetFramebufferAttachmentParameteriv__III_3II(JNIEnv *env, jclass clazz, jint target, jint attachment, jint pname, jintArray params_ref, jint offset)
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
	glGetFramebufferAttachmentParameteriv((GLenum)target, (GLenum)attachment, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetFramebufferAttachmentParameteriv ( GLenum target, GLenum attachment, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetFramebufferAttachmentParameteriv__IIILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint attachment, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetFramebufferAttachmentParameteriv((GLenum)target, (GLenum)attachment, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetRenderbufferParameteriv ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetRenderbufferParameteriv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
	glGetRenderbufferParameteriv((GLenum)target, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetRenderbufferParameteriv ( GLenum target, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetRenderbufferParameteriv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetRenderbufferParameteriv((GLenum)target, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetShaderPrecisionFormat ( GLenum shadertype, GLenum precisiontype, GLint *range, GLint *precision ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetShaderPrecisionFormat__II_3II_3II(JNIEnv *env, jclass clazz, jint shadertype, jint precisiontype, jintArray range_ref, jint rangeOffset, jintArray precision_ref, jint precisionOffset)
{
	jint *precision = NULL;
	jint *range = NULL;
	if (!range_ref) {
		throw_iae(env, "range == null");
		goto exit;
	}
	if (rangeOffset < 0) {
		throw_iae(env, "rangeOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, range_ref) - rangeOffset < 1) {
		throw_iae(env, "length - rangeOffset < 1 < needed");
		goto exit;
	}
	range = (*env)->GetIntArrayElements(env, range_ref, NULL);
	if (!precision_ref) {
		throw_iae(env, "precision == null");
		goto exit;
	}
	if (precisionOffset < 0) {
		throw_iae(env, "precisionOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, precision_ref) - precisionOffset < 1) {
		throw_iae(env, "length - precisionOffset < 1 < needed");
		goto exit;
	}
	precision = (*env)->GetIntArrayElements(env, precision_ref, NULL);
	glGetShaderPrecisionFormat((GLenum)shadertype, (GLenum)precisiontype, (GLint *)(range + rangeOffset), (GLint *)(precision + precisionOffset));
exit:
	if (range)
		(*env)->ReleaseIntArrayElements(env, range_ref, range, 0);
	if (precision)
		(*env)->ReleaseIntArrayElements(env, precision_ref, precision, 0);
}

/* void glGetShaderPrecisionFormat ( GLenum shadertype, GLenum precisiontype, GLint *range, GLint *precision ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetShaderPrecisionFormat__IILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint shadertype, jint precisiontype, jobject range_ref, jobject precision_ref)
{
	jarray precision_array = NULL;
	jbyte *precision_elems = NULL;
	void *precision = NULL;
	jarray range_array = NULL;
	jbyte *range_elems = NULL;
	void *range = NULL;
	if (!range_ref) {
		throw_iae(env, "range == null");
		goto exit;
	}
	range = get_nio_buffer(env, range_ref, &range_array, &range_elems);
	if (!precision_ref) {
		throw_iae(env, "precision == null");
		goto exit;
	}
	precision = get_nio_buffer(env, precision_ref, &precision_array, &precision_elems);
	glGetShaderPrecisionFormat((GLenum)shadertype, (GLenum)precisiontype, (GLint *)range, (GLint *)precision);
exit:
	release_nio_buffer(env, range_array, range_elems);
	release_nio_buffer(env, precision_array, precision_elems);
}

/* void glGetShaderSource ( GLuint shader, GLsizei bufsize, GLsizei *length, char *source ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetShaderSource__II_3II_3BI(JNIEnv *env, jclass clazz, jint shader, jint bufsize, jintArray length_ref, jint lengthOffset, jbyteArray source_ref, jint sourceOffset)
{
	jbyte *source = NULL;
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
	if (!source_ref) {
		throw_iae(env, "source == null");
		goto exit;
	}
	if (sourceOffset < 0) {
		throw_iae(env, "sourceOffset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, source_ref) - sourceOffset < 1) {
		throw_iae(env, "length - sourceOffset < 1 < needed");
		goto exit;
	}
	source = (*env)->GetByteArrayElements(env, source_ref, NULL);
	glGetShaderSource((GLuint)shader, (GLsizei)bufsize, (GLsizei *)(length + lengthOffset), (GLchar *)(source + sourceOffset));
exit:
	if (length)
		(*env)->ReleaseIntArrayElements(env, length_ref, length, 0);
	if (source)
		(*env)->ReleaseByteArrayElements(env, source_ref, source, 0);
}

/* void glGetTexParameterfv ( GLenum target, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetTexParameterfv__II_3FI(JNIEnv *env, jclass clazz, jint target, jint pname, jfloatArray params_ref, jint offset)
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
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetTexParameterfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
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
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetTexParameteriv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetTexParameteriv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
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

/* void glGetUniformfv ( GLuint program, GLint location, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetUniformfv__II_3FI(JNIEnv *env, jclass clazz, jint program, jint location, jfloatArray params_ref, jint offset)
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
	glGetUniformfv((GLuint)program, (GLint)location, (GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, 0);
}

/* void glGetUniformfv ( GLuint program, GLint location, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetUniformfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetUniformfv((GLuint)program, (GLint)location, (GLfloat *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetUniformiv ( GLuint program, GLint location, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetUniformiv__II_3II(JNIEnv *env, jclass clazz, jint program, jint location, jintArray params_ref, jint offset)
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
	glGetUniformiv((GLuint)program, (GLint)location, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetUniformiv ( GLuint program, GLint location, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetUniformiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint location, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetUniformiv((GLuint)program, (GLint)location, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetVertexAttribfv ( GLuint index, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetVertexAttribfv__II_3FI(JNIEnv *env, jclass clazz, jint index, jint pname, jfloatArray params_ref, jint offset)
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
	glGetVertexAttribfv((GLuint)index, (GLenum)pname, (GLfloat *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseFloatArrayElements(env, params_ref, params, 0);
}

/* void glGetVertexAttribfv ( GLuint index, GLenum pname, GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetVertexAttribfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint index, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetVertexAttribfv((GLuint)index, (GLenum)pname, (GLfloat *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glGetVertexAttribiv ( GLuint index, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetVertexAttribiv__II_3II(JNIEnv *env, jclass clazz, jint index, jint pname, jintArray params_ref, jint offset)
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
	glGetVertexAttribiv((GLuint)index, (GLenum)pname, (GLint *)(params + offset));
exit:
	if (params)
		(*env)->ReleaseIntArrayElements(env, params_ref, params, 0);
}

/* void glGetVertexAttribiv ( GLuint index, GLenum pname, GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetVertexAttribiv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint index, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	if (!params_ref) {
		throw_iae(env, "params == null");
		goto exit;
	}
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glGetVertexAttribiv((GLuint)index, (GLenum)pname, (GLint *)params);
exit:
	release_nio_buffer(env, params_array, params_elems);
}

/* void glHint ( GLenum target, GLenum mode ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glHint__II(JNIEnv *env, jclass clazz, jint target, jint mode)
{
	glHint((GLenum)target, (GLenum)mode);
}

/* GLboolean glIsBuffer ( GLuint buffer ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES20_glIsBuffer__I(JNIEnv *env, jclass clazz, jint buffer)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsBuffer((GLuint)buffer);
	return _ret;
}

/* GLboolean glIsEnabled ( GLenum cap ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES20_glIsEnabled__I(JNIEnv *env, jclass clazz, jint cap)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsEnabled((GLenum)cap);
	return _ret;
}

/* GLboolean glIsFramebuffer ( GLuint framebuffer ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES20_glIsFramebuffer__I(JNIEnv *env, jclass clazz, jint framebuffer)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsFramebuffer((GLuint)framebuffer);
	return _ret;
}

/* GLboolean glIsProgram ( GLuint program ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES20_glIsProgram__I(JNIEnv *env, jclass clazz, jint program)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsProgram((GLuint)program);
	return _ret;
}

/* GLboolean glIsRenderbuffer ( GLuint renderbuffer ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES20_glIsRenderbuffer__I(JNIEnv *env, jclass clazz, jint renderbuffer)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsRenderbuffer((GLuint)renderbuffer);
	return _ret;
}

/* GLboolean glIsShader ( GLuint shader ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES20_glIsShader__I(JNIEnv *env, jclass clazz, jint shader)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsShader((GLuint)shader);
	return _ret;
}

/* GLboolean glIsTexture ( GLuint texture ) */
JNIEXPORT jboolean JNICALL Java_android_opengl_GLES20_glIsTexture__I(JNIEnv *env, jclass clazz, jint texture)
{
	jboolean _ret = 0;
	_ret = (jboolean)glIsTexture((GLuint)texture);
	return _ret;
}

/* void glPolygonOffset ( GLfloat factor, GLfloat units ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glPolygonOffset__FF(JNIEnv *env, jclass clazz, jfloat factor, jfloat units)
{
	glPolygonOffset((GLfloat)factor, (GLfloat)units);
}

/* void glReleaseShaderCompiler ( void ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glReleaseShaderCompiler__(JNIEnv *env, jclass clazz)
{
	glReleaseShaderCompiler();
}

/* void glSampleCoverage ( GLclampf value, GLboolean invert ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glSampleCoverage__FZ(JNIEnv *env, jclass clazz, jfloat value, jboolean invert)
{
	glSampleCoverage((GLfloat)value, (GLboolean)invert);
}

/* void glShaderBinary ( GLsizei n, const GLuint *shaders, GLenum binaryformat, const GLvoid *binary, GLsizei length ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glShaderBinary__I_3IIILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint n, jintArray shaders_ref, jint offset, jint binaryformat, jobject binary_ref, jint length)
{
	jarray binary_array = NULL;
	jbyte *binary_elems = NULL;
	void *binary = NULL;
	jint *shaders = NULL;
	if (!shaders_ref) {
		throw_iae(env, "shaders == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, shaders_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	shaders = (*env)->GetIntArrayElements(env, shaders_ref, NULL);
	binary = get_nio_buffer(env, binary_ref, &binary_array, &binary_elems);
	glShaderBinary((GLsizei)n, (const GLuint *)(shaders + offset), (GLenum)binaryformat, (const void *)binary, (GLsizei)length);
exit:
	if (shaders)
		(*env)->ReleaseIntArrayElements(env, shaders_ref, shaders, JNI_ABORT);
	release_nio_buffer(env, binary_array, binary_elems);
}

/* void glShaderBinary ( GLsizei n, const GLuint *shaders, GLenum binaryformat, const GLvoid *binary, GLsizei length ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glShaderBinary__ILjava_nio_IntBuffer_2ILjava_nio_Buffer_2I(JNIEnv *env, jclass clazz, jint n, jobject shaders_ref, jint binaryformat, jobject binary_ref, jint length)
{
	jarray binary_array = NULL;
	jbyte *binary_elems = NULL;
	void *binary = NULL;
	jarray shaders_array = NULL;
	jbyte *shaders_elems = NULL;
	void *shaders = NULL;
	shaders = get_nio_buffer(env, shaders_ref, &shaders_array, &shaders_elems);
	binary = get_nio_buffer(env, binary_ref, &binary_array, &binary_elems);
	glShaderBinary((GLsizei)n, (const GLuint *)shaders, (GLenum)binaryformat, (const void *)binary, (GLsizei)length);
	release_nio_buffer(env, shaders_array, shaders_elems);
	release_nio_buffer(env, binary_array, binary_elems);
}

/* void glStencilFunc ( GLenum func, GLint ref, GLuint mask ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glStencilFunc__III(JNIEnv *env, jclass clazz, jint func, jint ref, jint mask)
{
	glStencilFunc((GLenum)func, (GLint)ref, (GLuint)mask);
}

/* void glStencilFuncSeparate ( GLenum face, GLenum func, GLint ref, GLuint mask ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glStencilFuncSeparate__IIII(JNIEnv *env, jclass clazz, jint face, jint func, jint ref, jint mask)
{
	glStencilFuncSeparate((GLenum)face, (GLenum)func, (GLint)ref, (GLuint)mask);
}

/* void glStencilMask ( GLuint mask ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glStencilMask__I(JNIEnv *env, jclass clazz, jint mask)
{
	glStencilMask((GLuint)mask);
}

/* void glStencilMaskSeparate ( GLenum face, GLuint mask ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glStencilMaskSeparate__II(JNIEnv *env, jclass clazz, jint face, jint mask)
{
	glStencilMaskSeparate((GLenum)face, (GLuint)mask);
}

/* void glStencilOp ( GLenum fail, GLenum zfail, GLenum zpass ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glStencilOp__III(JNIEnv *env, jclass clazz, jint fail, jint zfail, jint zpass)
{
	glStencilOp((GLenum)fail, (GLenum)zfail, (GLenum)zpass);
}

/* void glStencilOpSeparate ( GLenum face, GLenum fail, GLenum zfail, GLenum zpass ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glStencilOpSeparate__IIII(JNIEnv *env, jclass clazz, jint face, jint fail, jint zfail, jint zpass)
{
	glStencilOpSeparate((GLenum)face, (GLenum)fail, (GLenum)zfail, (GLenum)zpass);
}

/* void glTexParameterfv ( GLenum target, GLenum pname, const GLfloat *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glTexParameterfv__II_3FI(JNIEnv *env, jclass clazz, jint target, jint pname, jfloatArray params_ref, jint offset)
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
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glTexParameterfv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexParameterfv((GLenum)target, (GLenum)pname, (const GLfloat *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glTexParameteriv ( GLenum target, GLenum pname, const GLint *params ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glTexParameteriv__II_3II(JNIEnv *env, jclass clazz, jint target, jint pname, jintArray params_ref, jint offset)
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
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glTexParameteriv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint target, jint pname, jobject params_ref)
{
	jarray params_array = NULL;
	jbyte *params_elems = NULL;
	void *params = NULL;
	params = get_nio_buffer(env, params_ref, &params_array, &params_elems);
	glTexParameteriv((GLenum)target, (GLenum)pname, (const GLint *)params);
	release_nio_buffer(env, params_array, params_elems);
}

/* void glUniform1fv ( GLint location, GLsizei count, const GLfloat *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform1fv__II_3FI(JNIEnv *env, jclass clazz, jint location, jint count, jfloatArray v_ref, jint offset)
{
	jfloat *v = NULL;
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
	v = (*env)->GetFloatArrayElements(env, v_ref, NULL);
	glUniform1fv((GLint)location, (GLsizei)count, (const GLfloat *)(v + offset));
exit:
	if (v)
		(*env)->ReleaseFloatArrayElements(env, v_ref, v, JNI_ABORT);
}

/* void glUniform1fv ( GLint location, GLsizei count, const GLfloat *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform1fv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jobject v_ref)
{
	jarray v_array = NULL;
	jbyte *v_elems = NULL;
	void *v = NULL;
	v = get_nio_buffer(env, v_ref, &v_array, &v_elems);
	glUniform1fv((GLint)location, (GLsizei)count, (const GLfloat *)v);
	release_nio_buffer(env, v_array, v_elems);
}

/* void glUniform1iv ( GLint location, GLsizei count, const GLint *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform1iv__II_3II(JNIEnv *env, jclass clazz, jint location, jint count, jintArray v_ref, jint offset)
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
	glUniform1iv((GLint)location, (GLsizei)count, (const GLint *)(v + offset));
exit:
	if (v)
		(*env)->ReleaseIntArrayElements(env, v_ref, v, JNI_ABORT);
}

/* void glUniform1iv ( GLint location, GLsizei count, const GLint *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform1iv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jobject v_ref)
{
	jarray v_array = NULL;
	jbyte *v_elems = NULL;
	void *v = NULL;
	v = get_nio_buffer(env, v_ref, &v_array, &v_elems);
	glUniform1iv((GLint)location, (GLsizei)count, (const GLint *)v);
	release_nio_buffer(env, v_array, v_elems);
}

/* void glUniform2f ( GLint location, GLfloat x, GLfloat y ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform2f__IFF(JNIEnv *env, jclass clazz, jint location, jfloat x, jfloat y)
{
	glUniform2f((GLint)location, (GLfloat)x, (GLfloat)y);
}

/* void glUniform2fv ( GLint location, GLsizei count, const GLfloat *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform2fv__II_3FI(JNIEnv *env, jclass clazz, jint location, jint count, jfloatArray v_ref, jint offset)
{
	jfloat *v = NULL;
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
	v = (*env)->GetFloatArrayElements(env, v_ref, NULL);
	glUniform2fv((GLint)location, (GLsizei)count, (const GLfloat *)(v + offset));
exit:
	if (v)
		(*env)->ReleaseFloatArrayElements(env, v_ref, v, JNI_ABORT);
}

/* void glUniform2fv ( GLint location, GLsizei count, const GLfloat *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform2fv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jobject v_ref)
{
	jarray v_array = NULL;
	jbyte *v_elems = NULL;
	void *v = NULL;
	v = get_nio_buffer(env, v_ref, &v_array, &v_elems);
	glUniform2fv((GLint)location, (GLsizei)count, (const GLfloat *)v);
	release_nio_buffer(env, v_array, v_elems);
}

/* void glUniform2i ( GLint location, GLint x, GLint y ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform2i__III(JNIEnv *env, jclass clazz, jint location, jint x, jint y)
{
	glUniform2i((GLint)location, (GLint)x, (GLint)y);
}

/* void glUniform2iv ( GLint location, GLsizei count, const GLint *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform2iv__II_3II(JNIEnv *env, jclass clazz, jint location, jint count, jintArray v_ref, jint offset)
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
	glUniform2iv((GLint)location, (GLsizei)count, (const GLint *)(v + offset));
exit:
	if (v)
		(*env)->ReleaseIntArrayElements(env, v_ref, v, JNI_ABORT);
}

/* void glUniform2iv ( GLint location, GLsizei count, const GLint *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform2iv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jobject v_ref)
{
	jarray v_array = NULL;
	jbyte *v_elems = NULL;
	void *v = NULL;
	v = get_nio_buffer(env, v_ref, &v_array, &v_elems);
	glUniform2iv((GLint)location, (GLsizei)count, (const GLint *)v);
	release_nio_buffer(env, v_array, v_elems);
}

/* void glUniform3f ( GLint location, GLfloat x, GLfloat y, GLfloat z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform3f__IFFF(JNIEnv *env, jclass clazz, jint location, jfloat x, jfloat y, jfloat z)
{
	glUniform3f((GLint)location, (GLfloat)x, (GLfloat)y, (GLfloat)z);
}

/* void glUniform3fv ( GLint location, GLsizei count, const GLfloat *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform3fv__II_3FI(JNIEnv *env, jclass clazz, jint location, jint count, jfloatArray v_ref, jint offset)
{
	jfloat *v = NULL;
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
	v = (*env)->GetFloatArrayElements(env, v_ref, NULL);
	glUniform3fv((GLint)location, (GLsizei)count, (const GLfloat *)(v + offset));
exit:
	if (v)
		(*env)->ReleaseFloatArrayElements(env, v_ref, v, JNI_ABORT);
}

/* void glUniform3fv ( GLint location, GLsizei count, const GLfloat *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform3fv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jobject v_ref)
{
	jarray v_array = NULL;
	jbyte *v_elems = NULL;
	void *v = NULL;
	v = get_nio_buffer(env, v_ref, &v_array, &v_elems);
	glUniform3fv((GLint)location, (GLsizei)count, (const GLfloat *)v);
	release_nio_buffer(env, v_array, v_elems);
}

/* void glUniform3i ( GLint location, GLint x, GLint y, GLint z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform3i__IIII(JNIEnv *env, jclass clazz, jint location, jint x, jint y, jint z)
{
	glUniform3i((GLint)location, (GLint)x, (GLint)y, (GLint)z);
}

/* void glUniform3iv ( GLint location, GLsizei count, const GLint *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform3iv__II_3II(JNIEnv *env, jclass clazz, jint location, jint count, jintArray v_ref, jint offset)
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
	glUniform3iv((GLint)location, (GLsizei)count, (const GLint *)(v + offset));
exit:
	if (v)
		(*env)->ReleaseIntArrayElements(env, v_ref, v, JNI_ABORT);
}

/* void glUniform3iv ( GLint location, GLsizei count, const GLint *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform3iv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jobject v_ref)
{
	jarray v_array = NULL;
	jbyte *v_elems = NULL;
	void *v = NULL;
	v = get_nio_buffer(env, v_ref, &v_array, &v_elems);
	glUniform3iv((GLint)location, (GLsizei)count, (const GLint *)v);
	release_nio_buffer(env, v_array, v_elems);
}

/* void glUniform4fv ( GLint location, GLsizei count, const GLfloat *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform4fv__II_3FI(JNIEnv *env, jclass clazz, jint location, jint count, jfloatArray v_ref, jint offset)
{
	jfloat *v = NULL;
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
	v = (*env)->GetFloatArrayElements(env, v_ref, NULL);
	glUniform4fv((GLint)location, (GLsizei)count, (const GLfloat *)(v + offset));
exit:
	if (v)
		(*env)->ReleaseFloatArrayElements(env, v_ref, v, JNI_ABORT);
}

/* void glUniform4fv ( GLint location, GLsizei count, const GLfloat *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform4fv__IILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jobject v_ref)
{
	jarray v_array = NULL;
	jbyte *v_elems = NULL;
	void *v = NULL;
	v = get_nio_buffer(env, v_ref, &v_array, &v_elems);
	glUniform4fv((GLint)location, (GLsizei)count, (const GLfloat *)v);
	release_nio_buffer(env, v_array, v_elems);
}

/* void glUniform4i ( GLint location, GLint x, GLint y, GLint z, GLint w ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform4i__IIIII(JNIEnv *env, jclass clazz, jint location, jint x, jint y, jint z, jint w)
{
	glUniform4i((GLint)location, (GLint)x, (GLint)y, (GLint)z, (GLint)w);
}

/* void glUniform4iv ( GLint location, GLsizei count, const GLint *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform4iv__II_3II(JNIEnv *env, jclass clazz, jint location, jint count, jintArray v_ref, jint offset)
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
	glUniform4iv((GLint)location, (GLsizei)count, (const GLint *)(v + offset));
exit:
	if (v)
		(*env)->ReleaseIntArrayElements(env, v_ref, v, JNI_ABORT);
}

/* void glUniform4iv ( GLint location, GLsizei count, const GLint *v ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniform4iv__IILjava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jobject v_ref)
{
	jarray v_array = NULL;
	jbyte *v_elems = NULL;
	void *v = NULL;
	v = get_nio_buffer(env, v_ref, &v_array, &v_elems);
	glUniform4iv((GLint)location, (GLsizei)count, (const GLint *)v);
	release_nio_buffer(env, v_array, v_elems);
}

/* void glUniformMatrix2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniformMatrix2fv__IIZ_3FI(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
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
	glUniformMatrix2fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glUniformMatrix2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniformMatrix2fv__IIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniformMatrix2fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glUniformMatrix3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniformMatrix3fv__IIZ_3FI(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jfloatArray value_ref, jint offset)
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
	glUniformMatrix3fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)(value + offset));
exit:
	if (value)
		(*env)->ReleaseFloatArrayElements(env, value_ref, value, JNI_ABORT);
}

/* void glUniformMatrix3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniformMatrix3fv__IIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniformMatrix3fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glUniformMatrix4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glUniformMatrix4fv__IIZLjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint location, jint count, jboolean transpose, jobject value_ref)
{
	jarray value_array = NULL;
	jbyte *value_elems = NULL;
	void *value = NULL;
	value = get_nio_buffer(env, value_ref, &value_array, &value_elems);
	glUniformMatrix4fv((GLint)location, (GLsizei)count, (GLboolean)transpose, (const GLfloat *)value);
	release_nio_buffer(env, value_array, value_elems);
}

/* void glValidateProgram ( GLuint program ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glValidateProgram__I(JNIEnv *env, jclass clazz, jint program)
{
	glValidateProgram((GLuint)program);
}

/* void glVertexAttrib1f ( GLuint indx, GLfloat x ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttrib1f__IF(JNIEnv *env, jclass clazz, jint indx, jfloat x)
{
	glVertexAttrib1f((GLuint)indx, (GLfloat)x);
}

/* void glVertexAttrib1fv ( GLuint indx, const GLfloat *values ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttrib1fv__I_3FI(JNIEnv *env, jclass clazz, jint indx, jfloatArray values_ref, jint offset)
{
	jfloat *values = NULL;
	if (!values_ref) {
		throw_iae(env, "values == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, values_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	values = (*env)->GetFloatArrayElements(env, values_ref, NULL);
	glVertexAttrib1fv((GLuint)indx, (const GLfloat *)(values + offset));
exit:
	if (values)
		(*env)->ReleaseFloatArrayElements(env, values_ref, values, JNI_ABORT);
}

/* void glVertexAttrib1fv ( GLuint indx, const GLfloat *values ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttrib1fv__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint indx, jobject values_ref)
{
	jarray values_array = NULL;
	jbyte *values_elems = NULL;
	void *values = NULL;
	values = get_nio_buffer(env, values_ref, &values_array, &values_elems);
	glVertexAttrib1fv((GLuint)indx, (const GLfloat *)values);
	release_nio_buffer(env, values_array, values_elems);
}

/* void glVertexAttrib2f ( GLuint indx, GLfloat x, GLfloat y ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttrib2f__IFF(JNIEnv *env, jclass clazz, jint indx, jfloat x, jfloat y)
{
	glVertexAttrib2f((GLuint)indx, (GLfloat)x, (GLfloat)y);
}

/* void glVertexAttrib2fv ( GLuint indx, const GLfloat *values ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttrib2fv__I_3FI(JNIEnv *env, jclass clazz, jint indx, jfloatArray values_ref, jint offset)
{
	jfloat *values = NULL;
	if (!values_ref) {
		throw_iae(env, "values == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, values_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	values = (*env)->GetFloatArrayElements(env, values_ref, NULL);
	glVertexAttrib2fv((GLuint)indx, (const GLfloat *)(values + offset));
exit:
	if (values)
		(*env)->ReleaseFloatArrayElements(env, values_ref, values, JNI_ABORT);
}

/* void glVertexAttrib2fv ( GLuint indx, const GLfloat *values ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttrib2fv__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint indx, jobject values_ref)
{
	jarray values_array = NULL;
	jbyte *values_elems = NULL;
	void *values = NULL;
	values = get_nio_buffer(env, values_ref, &values_array, &values_elems);
	glVertexAttrib2fv((GLuint)indx, (const GLfloat *)values);
	release_nio_buffer(env, values_array, values_elems);
}

/* void glVertexAttrib3f ( GLuint indx, GLfloat x, GLfloat y, GLfloat z ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttrib3f__IFFF(JNIEnv *env, jclass clazz, jint indx, jfloat x, jfloat y, jfloat z)
{
	glVertexAttrib3f((GLuint)indx, (GLfloat)x, (GLfloat)y, (GLfloat)z);
}

/* void glVertexAttrib3fv ( GLuint indx, const GLfloat *values ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttrib3fv__I_3FI(JNIEnv *env, jclass clazz, jint indx, jfloatArray values_ref, jint offset)
{
	jfloat *values = NULL;
	if (!values_ref) {
		throw_iae(env, "values == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, values_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	values = (*env)->GetFloatArrayElements(env, values_ref, NULL);
	glVertexAttrib3fv((GLuint)indx, (const GLfloat *)(values + offset));
exit:
	if (values)
		(*env)->ReleaseFloatArrayElements(env, values_ref, values, JNI_ABORT);
}

/* void glVertexAttrib3fv ( GLuint indx, const GLfloat *values ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttrib3fv__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint indx, jobject values_ref)
{
	jarray values_array = NULL;
	jbyte *values_elems = NULL;
	void *values = NULL;
	values = get_nio_buffer(env, values_ref, &values_array, &values_elems);
	glVertexAttrib3fv((GLuint)indx, (const GLfloat *)values);
	release_nio_buffer(env, values_array, values_elems);
}

/* void glVertexAttrib4f ( GLuint indx, GLfloat x, GLfloat y, GLfloat z, GLfloat w ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttrib4f__IFFFF(JNIEnv *env, jclass clazz, jint indx, jfloat x, jfloat y, jfloat z, jfloat w)
{
	glVertexAttrib4f((GLuint)indx, (GLfloat)x, (GLfloat)y, (GLfloat)z, (GLfloat)w);
}

/* void glVertexAttrib4fv ( GLuint indx, const GLfloat *values ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttrib4fv__I_3FI(JNIEnv *env, jclass clazz, jint indx, jfloatArray values_ref, jint offset)
{
	jfloat *values = NULL;
	if (!values_ref) {
		throw_iae(env, "values == null");
		goto exit;
	}
	if (offset < 0) {
		throw_iae(env, "offset < 0");
		goto exit;
	}
	if ((*env)->GetArrayLength(env, values_ref) - offset < 1) {
		throw_iae(env, "length - offset < 1 < needed");
		goto exit;
	}
	values = (*env)->GetFloatArrayElements(env, values_ref, NULL);
	glVertexAttrib4fv((GLuint)indx, (const GLfloat *)(values + offset));
exit:
	if (values)
		(*env)->ReleaseFloatArrayElements(env, values_ref, values, JNI_ABORT);
}

/* void glVertexAttrib4fv ( GLuint indx, const GLfloat *values ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glVertexAttrib4fv__ILjava_nio_FloatBuffer_2(JNIEnv *env, jclass clazz, jint indx, jobject values_ref)
{
	jarray values_array = NULL;
	jbyte *values_elems = NULL;
	void *values = NULL;
	values = get_nio_buffer(env, values_ref, &values_array, &values_elems);
	glVertexAttrib4fv((GLuint)indx, (const GLfloat *)values);
	release_nio_buffer(env, values_array, values_elems);
}

/* ---- written by hand: names returned as strings ---- */

/* glGetActiveAttrib/glGetActiveUniform: the name of the attribute/uniform at index with its size and type */
static jstring active_name(JNIEnv *env, PFNGLGETACTIVEATTRIBPROC get, GLenum max_length_pname, jint program, jint index, GLint *size, GLenum *type)
{
	GLint max_length = 0;
	glGetProgramiv((GLuint)program, max_length_pname, &max_length);
	GLchar *name = calloc(max_length + 1, 1);
	get((GLuint)program, (GLuint)index, max_length + 1, NULL, size, type, name);
	jstring result = (*env)->NewStringUTF(env, name);
	free(name);
	return result;
}

static jstring active_name_array(JNIEnv *env, PFNGLGETACTIVEATTRIBPROC get, GLenum max_length_pname, jint program, jint index, jintArray size_ref, jint sizeOffset, jintArray type_ref, jint typeOffset)
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
	jstring result = active_name(env, get, max_length_pname, program, index, &size, &type);
	(*env)->SetIntArrayRegion(env, size_ref, sizeOffset, 1, &size);
	(*env)->SetIntArrayRegion(env, type_ref, typeOffset, 1, (jint *)&type);
	return result;
}

static jstring active_name_buffer(JNIEnv *env, PFNGLGETACTIVEATTRIBPROC get, GLenum max_length_pname, jint program, jint index, jobject size_ref, jobject type_ref)
{
	jarray a1 = NULL, a2 = NULL;
	jbyte *e1 = NULL, *e2 = NULL;
	if (!size_ref || !type_ref) {
		throw_iae(env, !size_ref ? "size == null" : "type == null");
		return NULL;
	}
	GLint *size = get_nio_buffer(env, size_ref, &a1, &e1);
	GLenum *type = get_nio_buffer(env, type_ref, &a2, &e2);
	jstring result = active_name(env, get, max_length_pname, program, index, size, type);
	release_nio_buffer(env, a2, e2);
	release_nio_buffer(env, a1, e1);
	return result;
}

/* deprecated variants: name is a single byte passed by value, there is no room for the name; only length,
 * size and type are returned */
static void active_name_deprecated(JNIEnv *env, PFNGLGETACTIVEATTRIBPROC get, jint program, jint index, jobject length_ref, jobject size_ref, jobject type_ref)
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
	get((GLuint)program, (GLuint)index, 0, length, size, type, NULL);
	release_nio_buffer(env, a3, e3);
	release_nio_buffer(env, a2, e2);
	release_nio_buffer(env, a1, e1);
}

/* void glGetActiveAttrib ( GLuint program, GLuint index, GLsizei bufsize, GLsizei *length, GLint *size, GLenum *type, char *name ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetActiveAttrib__IIILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2B(JNIEnv *env, jclass clazz, jint program, jint index, jint bufsize, jobject length_ref, jobject size_ref, jobject type_ref, jbyte name)
{
	active_name_deprecated(env, glGetActiveAttrib, program, index, length_ref, size_ref, type_ref);
}

JNIEXPORT jstring JNICALL Java_android_opengl_GLES20_glGetActiveAttrib__II_3II_3II(JNIEnv *env, jclass clazz, jint program, jint index, jintArray size_ref, jint sizeOffset, jintArray type_ref, jint typeOffset)
{
	return active_name_array(env, glGetActiveAttrib, GL_ACTIVE_ATTRIBUTE_MAX_LENGTH, program, index, size_ref, sizeOffset, type_ref, typeOffset);
}

JNIEXPORT jstring JNICALL Java_android_opengl_GLES20_glGetActiveAttrib__IILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2(JNIEnv *env, jclass clazz, jint program, jint index, jobject size_ref, jobject type_ref)
{
	return active_name_buffer(env, glGetActiveAttrib, GL_ACTIVE_ATTRIBUTE_MAX_LENGTH, program, index, size_ref, type_ref);
}

/* void glGetActiveUniform ( GLuint program, GLuint index, GLsizei bufsize, GLsizei *length, GLint *size, GLenum *type, char *name ) */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetActiveUniform__IIILjava_nio_IntBuffer_2Ljava_nio_IntBuffer_2Ljava_nio_IntBuffer_2B(JNIEnv *env, jclass clazz, jint program, jint index, jint bufsize, jobject length_ref, jobject size_ref, jobject type_ref, jbyte name)
{
	active_name_deprecated(env, glGetActiveUniform, program, index, length_ref, size_ref, type_ref);
}

JNIEXPORT jstring JNICALL Java_android_opengl_GLES20_glGetActiveUniform__II_3II_3II(JNIEnv *env, jclass clazz, jint program, jint index, jintArray size_ref, jint sizeOffset, jintArray type_ref, jint typeOffset)
{
	return active_name_array(env, glGetActiveUniform, GL_ACTIVE_UNIFORM_MAX_LENGTH, program, index, size_ref, sizeOffset, type_ref, typeOffset);
}

/* void glGetShaderSource ( GLuint shader, GLsizei bufsize, GLsizei *length, char *source )
 * deprecated variant: source is a single byte passed by value; only the length is returned */
JNIEXPORT void JNICALL Java_android_opengl_GLES20_glGetShaderSource__IILjava_nio_IntBuffer_2B(JNIEnv *env, jclass clazz, jint shader, jint bufsize, jobject length_ref, jbyte source)
{
	jarray a = NULL;
	jbyte *e = NULL;
	GLsizei *length = get_nio_buffer(env, length_ref, &a, &e);
	glGetShaderSource((GLuint)shader, 0, length, NULL);
	release_nio_buffer(env, a, e);
}

JNIEXPORT jstring JNICALL Java_android_opengl_GLES20_glGetShaderSource__I(JNIEnv *env, jclass clazz, jint shader)
{
	GLint length = 0;
	glGetShaderiv((GLuint)shader, GL_SHADER_SOURCE_LENGTH, &length);
	GLchar *source = calloc(length + 1, 1);
	glGetShaderSource((GLuint)shader, length + 1, NULL, source);
	jstring result = (*env)->NewStringUTF(env, source);
	free(source);
	return result;
}
