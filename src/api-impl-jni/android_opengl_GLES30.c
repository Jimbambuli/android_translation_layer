#include "defines.h"
#include <GLES3/gl3.h>
#include <jni.h>
#include <stdint.h>

#include "generated_headers/android_opengl_GLES30.h"

JNIEXPORT jlong JNICALL Java_android_opengl_GLES30_glFenceSync(JNIEnv *env, jclass class, jint condition, jint flags)
{
	return (jlong)(intptr_t)glFenceSync((GLenum)condition, (GLbitfield)flags);
}

JNIEXPORT jboolean JNICALL Java_android_opengl_GLES30_glIsSync(JNIEnv *env, jclass class, jlong sync)
{
	return glIsSync((GLsync)(intptr_t)sync);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES30_glDeleteSync(JNIEnv *env, jclass class, jlong sync)
{
	glDeleteSync((GLsync)(intptr_t)sync);
}

JNIEXPORT jint JNICALL Java_android_opengl_GLES30_glClientWaitSync(JNIEnv *env, jclass class, jlong sync, jint flags, jlong timeout)
{
	return (jint)glClientWaitSync((GLsync)(intptr_t)sync, (GLbitfield)flags, (GLuint64)timeout);
}

JNIEXPORT void JNICALL Java_android_opengl_GLES30_glWaitSync(JNIEnv *env, jclass class, jlong sync, jint flags, jlong timeout)
{
	glWaitSync((GLsync)(intptr_t)sync, (GLbitfield)flags, (GLuint64)timeout);
}
