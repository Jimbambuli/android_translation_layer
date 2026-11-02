package android.os.storage;

import android.os.Environment;
import android.os.StatFs;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class StorageManager {
	// AOSP: uuid -v5 ad99aa3d-308e-4191-a200-ebcab371c0ad default
	public static final UUID UUID_DEFAULT = UUID.fromString("41217664-9172-527a-b3d5-edabb50a7d69");
	public StorageVolume[] getVolumeList() {
		StorageVolume[] sVolumes = {new StorageVolume()};
		return sVolumes;
	}

	public StorageVolume getStorageVolume(File file) { return null; }

	// there is a single storage volume, so every path is hosted by internal storage
	public UUID getUuidForPath(File path) throws IOException {
		return UUID_DEFAULT;
	}

	// AOSP gets the quota from installd, which shares free space between apps; no installd here
	public long getCacheQuotaBytes(UUID storageUuid) throws IOException {
		return new StatFs(Environment.getDataDirectory().getPath()).getAvailableBytes();
	}

	/* AOSP StorageManager.getAllocatableBytes(UUID): free space plus the cache the system could clear. ATL
	 * clears no caches of other apps, so it is the free space of the single volume */
	public long getAllocatableBytes(UUID storageUuid) throws IOException {
		return new StatFs(Environment.getDataDirectory().getPath()).getAvailableBytes();
	}

	/* AOSP StorageManager.allocateBytes(UUID, long): makes room by clearing caches, or throws IOException */
	public void allocateBytes(UUID storageUuid, long bytes) throws IOException {
		long allocatable = getAllocatableBytes(storageUuid);
		if (bytes > allocatable)
			throw new IOException("Failed to allocate " + bytes + " because only " + allocatable + " allocatable");
	}
}
