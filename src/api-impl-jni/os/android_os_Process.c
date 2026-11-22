#include <grp.h>
#include <pwd.h>
#include <stdlib.h>
#include <string.h>
#include <sys/resource.h>
#include <time.h>

#include "../generated_headers/android_os_Process.h"

JNIEXPORT jboolean JNICALL Java_android_os_Process_is64Bit(JNIEnv *env, jclass this)
{
#ifdef __LP64__
	return 1;
#else
	return 0;
#endif
}

JNIEXPORT jint JNICALL Java_android_os_Process_getThreadPriority(JNIEnv *env, jclass this, jint tid)
{
	return getpriority(PRIO_PROCESS, tid);
}

JNIEXPORT jlong JNICALL Java_android_os_Process_getElapsedCpuTime(JNIEnv *env, jclass this)
{
	struct timespec now;
	if (clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &now) != 0)
		return 0;
	return now.tv_sec * 1000 + now.tv_nsec / 1000000;
}

/* AOSP android_util_Process.cpp: a name of digits only is the id itself, any other name is looked up in the user
 * and group database of the process (on Android bionic's, here the host's); -1 if it has none */
static jint id_for_name(JNIEnv *env, jstring name, jboolean group)
{
	if (name == NULL) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/NullPointerException"), NULL);
		return -1;
	}
	const char *str = (*env)->GetStringUTFChars(env, name, NULL);
	jint id = -1;
	size_t n = strlen(str);
	if (n > 0) {
		size_t i;
		for (i = 0; i < n && str[i] >= '0' && str[i] <= '9'; i++)
			;
		if (i == n) {
			id = atoi(str);
		} else if (group) {
			struct group *grp = getgrnam(str);
			id = grp ? grp->gr_gid : -1;
		} else {
			struct passwd *pwd = getpwnam(str);
			id = pwd ? pwd->pw_uid : -1;
		}
	}
	(*env)->ReleaseStringUTFChars(env, name, str);
	return id;
}

JNIEXPORT jint JNICALL Java_android_os_Process_getUidForName(JNIEnv *env, jclass this, jstring name)
{
	return id_for_name(env, name, JNI_FALSE);
}

JNIEXPORT jint JNICALL Java_android_os_Process_getGidForName(JNIEnv *env, jclass this, jstring name)
{
	return id_for_name(env, name, JNI_TRUE);
}
