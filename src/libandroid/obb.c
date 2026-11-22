/* NDK OBB scanner (android/obb.h) after AOSP frameworks/base/native/android/obb.cpp: androidfw's ObbFile */

#include <stdint.h>

#include <androidfw/androidfw_c_api.h>

typedef struct ObbFile AObbInfo;

AObbInfo *AObbScanner_getObbInfo(const char *filename)
{
	return ObbFile_readFrom(filename);
}

void AObbInfo_delete(AObbInfo *obbInfo)
{
	ObbFile_delete(obbInfo);
}

const char *AObbInfo_getPackageName(AObbInfo *obbInfo)
{
	return ObbFile_getPackageName(obbInfo);
}

int32_t AObbInfo_getVersion(AObbInfo *obbInfo)
{
	return ObbFile_getVersion(obbInfo);
}

int32_t AObbInfo_getFlags(AObbInfo *obbInfo)
{
	return ObbFile_getFlags(obbInfo);
}
