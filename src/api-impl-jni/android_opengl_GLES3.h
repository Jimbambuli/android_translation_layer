#ifndef ANDROID_OPENGL_GLES3_H
#define ANDROID_OPENGL_GLES3_H
/* shared by the JNI of android.opengl.GLES30, GLES31, GLES31Ext and GLES32 */
#include <EGL/egl.h>
#include <GLES3/gl32.h>
#include <GLES2/gl2ext.h>
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

/* the IllegalArgumentException AOSP's generated GL bindings throw for bad arrays and buffers */
static inline void throw_iae(JNIEnv *env, const char *message)
{
	(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"), message);
}

/* extension functions are not exported by libGLESv2, they are looked up once with eglGetProcAddress */
#define GL_EXT_CALL(name, type) ({ static type _proc; if (!_proc) _proc = (type)eglGetProcAddress(#name); _proc; })

/* the strings of a String[] as UTF-8 (released with release_strings) */
static inline const GLchar **get_strings(JNIEnv *env, jobjectArray array, jsize *count)
{
	*count = (*env)->GetArrayLength(env, array);
	const GLchar **strings = calloc(*count + 1, sizeof(GLchar *));
	for (jsize i = 0; i < *count; i++) {
		jstring s = (*env)->GetObjectArrayElement(env, array, i);
		strings[i] = s ? (*env)->GetStringUTFChars(env, s, NULL) : NULL;
		(*env)->DeleteLocalRef(env, s);
	}
	return strings;
}

static inline void release_strings(JNIEnv *env, jobjectArray array, const GLchar **strings, jsize count)
{
	for (jsize i = 0; i < count; i++) {
		if (strings[i]) {
			jstring s = (*env)->GetObjectArrayElement(env, array, i);
			(*env)->ReleaseStringUTFChars(env, s, strings[i]);
			(*env)->DeleteLocalRef(env, s);
		}
	}
	free(strings);
}


typedef GLuint (GL_APIENTRYP atl_get_debug_message_log)(GLuint count, GLsizei bufSize, GLenum *sources, GLenum *types, GLuint *ids, GLenum *severities, GLsizei *lengths, GLchar *messageLog);

/* glGetDebugMessageLog/glGetDebugMessageLogKHR returning the messages as String[]: up to count messages, the
 * sources, types, ids and severities written to the given arrays/buffers */
static inline jobjectArray debug_message_log_strings(JNIEnv *env, atl_get_debug_message_log get_log, jint count, GLenum *sources, GLenum *types, GLuint *ids, GLenum *severities)
{
	GLint max_length = 0;
	glGetIntegerv(GL_MAX_DEBUG_MESSAGE_LENGTH, &max_length);
	if (count < 0)
		count = 0;
	GLsizei *lengths = calloc(count + 1, sizeof(GLsizei));
	GLchar *log = calloc((size_t)count * max_length + 1, 1);
	GLuint n = get_log(count, count * max_length, sources, types, ids, severities, lengths, log);
	jobjectArray result = (*env)->NewObjectArray(env, n, (*env)->FindClass(env, "java/lang/String"), NULL);
	GLchar *message = log;
	for (GLuint i = 0; i < n; i++) {
		jstring s = (*env)->NewStringUTF(env, message); /* each message is NUL terminated, lengths include it */
		(*env)->SetObjectArrayElement(env, result, i, s);
		(*env)->DeleteLocalRef(env, s);
		message += lengths[i];
	}
	free(log);
	free(lengths);
	return result;
}

/* the int[] + offset of an output array, NULL with the exception thrown if it is too small for count */
static inline jint *get_int_array(JNIEnv *env, jintArray ref, jint offset, jint count, const char *name)
{
	char message[96];
	if (!ref) {
		snprintf(message, sizeof(message), "%s == null", name);
	} else if (offset < 0) {
		snprintf(message, sizeof(message), "%sOffset < 0", name);
	} else if ((*env)->GetArrayLength(env, ref) - offset < count) {
		snprintf(message, sizeof(message), "length - %sOffset < count < needed", name);
	} else {
		return (*env)->GetIntArrayElements(env, ref, NULL);
	}
	throw_iae(env, message);
	return NULL;
}

/* glGetObjectLabel/glGetObjectPtrLabel and the KHR versions: ask for the length, then the label */
#define OBJECT_LABEL(env, get, ...) ({ \
	GLsizei _length = 0; \
	get(__VA_ARGS__, 0, &_length, NULL); \
	GLchar *_label = calloc(_length + 1, 1); \
	get(__VA_ARGS__, _length + 1, NULL, _label); \
	jstring _result = (*env)->NewStringUTF(env, _label); \
	free(_label); \
	_result; })

#endif
