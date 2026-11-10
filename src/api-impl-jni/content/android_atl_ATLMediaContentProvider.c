#include <glib.h>
#include <string.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_atl_ATLMediaContentProvider.h"

/* the user's folders for Pictures, Music, Movies and Download (in this order), as main.c's link_media_dirs()
 * takes them: null when unset or the home directory */
JNIEXPORT jobjectArray JNICALL Java_android_atl_ATLMediaContentProvider_native_1user_1dirs(JNIEnv *env, jclass class)
{
	static const GUserDirectory dirs[] = {G_USER_DIRECTORY_PICTURES, G_USER_DIRECTORY_MUSIC, G_USER_DIRECTORY_VIDEOS, G_USER_DIRECTORY_DOWNLOAD};
	jobjectArray array = (*env)->NewObjectArray(env, G_N_ELEMENTS(dirs), (*env)->FindClass(env, "java/lang/String"), NULL);
	for (size_t i = 0; i < G_N_ELEMENTS(dirs); i++) {
		const char *dir = g_get_user_special_dir(dirs[i]);
		if (dir && strcmp(dir, g_get_home_dir()))
			(*env)->SetObjectArrayElement(env, array, i, _JSTRING(dir));
	}
	return array;
}
