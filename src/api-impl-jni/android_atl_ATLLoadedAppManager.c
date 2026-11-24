#include <glib.h>

#include "defines.h"
#include "util.h"

#include "generated_headers/android_atl_ATLLoadedAppManager.h"

/* exported by the shim bionic linker: replaces its list of directories with bionic-linked libraries */
void dl_parse_library_path(const char *path, char *delim);
/* main.c: the list given to the linker at start (the app's lib dir and data dir) */
extern char *bionic_ld_library_path;

/* AOSP creates a linker namespace for the class loader of another package's code, with that package's native
 * library dir as search path (LoadedApk.createOrUpdateClassLoaderLocked -> ClassLoaderFactory.createClassloaderNamespace).
 * The shim bionic linker has one list of directories for the whole process, so the dir is added to it. */
JNIEXPORT void JNICALL Java_android_atl_ATLLoadedAppManager_native_1addLibrarySearchPath(JNIEnv *env, jclass clazz, jstring dir)
{
	const char *dir_str = (*env)->GetStringUTFChars(env, dir, NULL);
	char *path = g_strdup_printf("%s:%s", bionic_ld_library_path ? bionic_ld_library_path : "", dir_str);
	(*env)->ReleaseStringUTFChars(env, dir, dir_str);
	dl_parse_library_path(path, ":");
	g_free(bionic_ld_library_path);
	bionic_ld_library_path = path;
}
