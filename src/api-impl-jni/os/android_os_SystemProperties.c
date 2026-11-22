#include <stdbool.h>
#include <stdio.h>

#include "../generated_headers/android_os_SystemProperties.h"

/* in libc_bio (bionic_translation): the table __system_property_get() and android_get_device_api_level() read */
extern bool btl_system_property_set(const char *name, const char *value);

JNIEXPORT void JNICALL Java_android_os_SystemProperties_publishToNative(JNIEnv *env, jclass this, jstring name, jstring value)
{
	const char *name_chars = (*env)->GetStringUTFChars(env, name, NULL);
	const char *value_chars = (*env)->GetStringUTFChars(env, value, NULL);

	if (!btl_system_property_set(name_chars, value_chars))
		fprintf(stderr, "SystemProperties: native property table full, %s not published\n", name_chars);

	(*env)->ReleaseStringUTFChars(env, name, name_chars);
	(*env)->ReleaseStringUTFChars(env, value, value_chars);
}
