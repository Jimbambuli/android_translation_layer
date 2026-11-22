#ifndef APK_ABI_H
#define APK_ABI_H

/* The index in `abis` of the ABI the app's native code is installed for, as PackageManager picks it
 * (NativeLibraryHelper.findSupportedAbi): the first ABI of the device's list, in list order, that any of the APK
 * files (`apk_paths`, separated by ':') has a library for (lib/<abi>/<file>); -1 if none has one, -2 if an
 * APK cannot be read. */
int apk_find_supported_abi(const char *apk_paths, const char *const *abis, int n_abis);

#endif
