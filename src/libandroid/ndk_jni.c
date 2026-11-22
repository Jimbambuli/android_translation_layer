#include <pthread.h>
#include <stdio.h>
#include <string.h>

#include <glib.h>

#include "ndk_jni.h"

extern JavaVM *jvm;

static pthread_key_t attached_key;
static pthread_once_t attached_once = PTHREAD_ONCE_INIT;

static void detach_thread(void *unused)
{
	(*jvm)->DetachCurrentThread(jvm);
}

static void create_key(void)
{
	pthread_key_create(&attached_key, detach_thread);
}

JNIEnv *ndk_jni_env(void)
{
	JNIEnv *env = NULL;
	if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6) == JNI_OK)
		return env;
	pthread_once(&attached_once, create_key);
	if ((*jvm)->AttachCurrentThreadAsDaemon(jvm, (void **)&env, &(JavaVMAttachArgs){.version = JNI_VERSION_1_6, .name = "NdkMedia"}) != JNI_OK)
		return NULL;
	pthread_setspecific(attached_key, env);
	return env;
}

bool ndk_jni_threw(JNIEnv *env, const char *what)
{
	if (!(*env)->ExceptionCheck(env))
		return false;
	fprintf(stderr, "NdkMedia: %s threw:\n", what);
	(*env)->ExceptionDescribe(env);
	(*env)->ExceptionClear(env);
	return true;
}

bool ndk_jni_threw_class(JNIEnv *env, const char *class_name, const char *what)
{
	if (!(*env)->ExceptionCheck(env))
		return false;
	jthrowable e = (*env)->ExceptionOccurred(env);
	(*env)->ExceptionClear(env);
	bool match = (*env)->IsInstanceOf(env, e, ndk_jni_class(env, class_name));
	if (!match) {
		fprintf(stderr, "NdkMedia: %s threw:\n", what);
		(*env)->Throw(env, e);
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, e);
	return true;
}

jclass ndk_jni_class(JNIEnv *env, const char *name)
{
	static GHashTable *classes;
	static GMutex lock;
	g_mutex_lock(&lock);
	if (!classes)
		classes = g_hash_table_new(g_str_hash, g_str_equal);
	jclass class = g_hash_table_lookup(classes, name);
	if (!class) {
		jclass local = (*env)->FindClass(env, name);
		if (local) {
			class = (*env)->NewGlobalRef(env, local);
			(*env)->DeleteLocalRef(env, local);
			g_hash_table_insert(classes, g_strdup(name), class);
		} else {
			ndk_jni_threw(env, name);
		}
	}
	g_mutex_unlock(&lock);
	return class;
}
