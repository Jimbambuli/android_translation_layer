package android.os.storage;

import android.os.Environment;
import android.os.StatFs;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

public class StorageManager {
	// AOSP: uuid -v5 ad99aa3d-308e-4191-a200-ebcab371c0ad default
	public static final UUID UUID_DEFAULT = UUID.fromString("41217664-9172-527a-b3d5-edabb50a7d69");
	public StorageVolume[] getVolumeList() {
		StorageVolume[] sVolumes = {new StorageVolume()};
		return sVolumes;
	}

	/* AOSP StorageManager.getStorageVolume(File) -> getStorageVolume(StorageVolume[], File): the volume whose
	 * directory holds the file (canonical paths); the deprecated /data path branch is left out */
	public StorageVolume getStorageVolume(File file) {
		return getStorageVolume(getVolumeList(), file);
	}

	private static StorageVolume getStorageVolume(StorageVolume[] volumes, File file) {
		if (file == null) {
			return null;
		}
		try {
			file = file.getCanonicalFile();
		} catch (IOException ignored) {
			return null;
		}
		for (StorageVolume volume : volumes) {
			File volumeFile = volume.getPathFile();
			try {
				volumeFile = volumeFile.getCanonicalFile();
			} catch (IOException ignored) {
				continue;
			}
			if (android.os.FileUtils.contains(volumeFile, file)) {
				return volume;
			}
		}
		return null;
	}

	/* AOSP StorageManager.getStorageVolumes() */
	public List<StorageVolume> getStorageVolumes() {
		final ArrayList<StorageVolume> res = new ArrayList<>();
		Collections.addAll(res, getVolumeList());
		return res;
	}

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

	/* AOSP StorageManager.StorageVolumeCallback */
	public static class StorageVolumeCallback {
		public void onStateChanged(StorageVolume volume) {}
	}

	/* AOSP registers the callback with the storage service; ATL has no volume state changes to report */
	public void registerStorageVolumeCallback(Executor executor, StorageVolumeCallback callback) {}

	public void unregisterStorageVolumeCallback(StorageVolumeCallback callback) {}
}
