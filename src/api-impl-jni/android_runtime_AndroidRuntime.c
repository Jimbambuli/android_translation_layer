/* libandroid_runtime's C++ API for native code of the platform (AOSP frameworks/base/core/jni/AndroidRuntime.cpp).
 * libtranslation_layer_main is ATL's counterpart of libandroid_runtime (bionic_translation maps the name to it),
 * so native code that calls into libandroid_runtime, e.g. the guest side of a native bridge through its
 * libandroid_runtime proxy, finds these here. */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

/* static int android::AndroidRuntime::registerNativeMethods(JNIEnv*, const char*, const JNINativeMethod*, int) */
__attribute__((visibility("default"))) int AndroidRuntime_registerNativeMethods(JNIEnv *env, const char *className, const JNINativeMethod *gMethods, int numMethods)
	__asm__("_ZN7android14AndroidRuntime21registerNativeMethodsEP7_JNIEnvPKcPK15JNINativeMethodi");

/* AOSP: jniRegisterNativeMethods(), which aborts when the class or a method is missing */
int AndroidRuntime_registerNativeMethods(JNIEnv *env, const char *className, const JNINativeMethod *gMethods, int numMethods)
{
	jclass clazz = (*env)->FindClass(env, className);
	if (!clazz) {
		fprintf(stderr, "Native registration unable to find class '%s'; aborting...\n", className);
		abort();
	}
	if ((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {
		fprintf(stderr, "RegisterNatives failed for '%s'; aborting...\n", className);
		abort();
	}
	(*env)->DeleteLocalRef(env, clazz);
	return 0;
}
