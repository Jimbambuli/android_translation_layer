#include <stdbool.h>

#include <glib.h>

#include "defines.h"
#include "util.h"

#include "generated_headers/android_os_Environment.h"

char *get_app_data_dir();

JNIEXPORT jstring JNICALL Java_android_os_Environment_native_1get_1app_1data_1dir(JNIEnv *env, jclass this)
{
	return _JSTRING(get_app_data_dir());
}

bool apply_path_overrides(char **path);

JNIEXPORT jstring JNICALL Java_android_os_Environment_native_1apply_1path_1overrides(JNIEnv *env, jclass this, jstring path_jstr)
{
	const char *orig_path = _CSTRING(path_jstr);
	char *path = (char *)orig_path;
	bool free_path = apply_path_overrides(&path);
	jstring ret = path == orig_path ? path_jstr : _JSTRING(path);
	if (free_path)
		g_free(path);
	(*env)->ReleaseStringUTFChars(env, path_jstr, orig_path);
	return ret;
}
