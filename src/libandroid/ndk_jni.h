#ifndef NDK_JNI_H
#define NDK_JNI_H

#include <jni.h>
#include <stdbool.h>

/* The NDK libraries of ATL (libmediandk on the Java media classes) are called from threads of the app that the VM
 * does not know. ndk_jni_env() attaches such a thread, which is detached again when it ends. */
JNIEnv *ndk_jni_env(void);

/* true and the pending exception cleared if the last call threw */
bool ndk_jni_threw(JNIEnv *env, const char *what);

/* like ndk_jni_threw(); an exception of another class than class_name is also reported */
bool ndk_jni_threw_class(JNIEnv *env, const char *class_name, const char *what);

jclass ndk_jni_class(JNIEnv *env, const char *name); /* a global reference, looked up once per name */

#endif
