/*
 * AStorageManager (android/storage_manager.h): OBB files are mounted by the storage manager service ("mount"),
 * which ATL does not have. As in AOSP, AStorageManager_new() then gives NULL (initialize() finds no service), so
 * no manager exists for the other functions; they answer like one without a mounted OBB.
 */

#include <stddef.h>
#include <stdint.h>

typedef struct AStorageManager AStorageManager;
typedef void (*AStorageManager_obbCallbackFunc)(const char *filename, const int32_t state, void *data);

#define AOBB_STATE_ERROR_INTERNAL 20
#define AOBB_STATE_ERROR_NOT_MOUNTED 23

AStorageManager *AStorageManager_new(void)
{
	return NULL;
}

void AStorageManager_delete(AStorageManager *mgr)
{
}

void AStorageManager_mountObb(AStorageManager *mgr, const char *filename, const char *key, AStorageManager_obbCallbackFunc cb, void *data)
{
	if (cb)
		cb(filename, AOBB_STATE_ERROR_INTERNAL, data);
}

void AStorageManager_unmountObb(AStorageManager *mgr, const char *filename, const int force, AStorageManager_obbCallbackFunc cb, void *data)
{
	if (cb)
		cb(filename, AOBB_STATE_ERROR_NOT_MOUNTED, data);
}

int AStorageManager_isObbMounted(AStorageManager *mgr, const char *filename)
{
	return 0;
}

const char *AStorageManager_getMountedObbPath(AStorageManager *mgr, const char *filename)
{
	return NULL;
}
