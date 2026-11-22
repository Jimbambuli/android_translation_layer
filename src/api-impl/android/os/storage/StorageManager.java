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
	/* ATL: AOSP isFileEncrypted() (@hide) reads ro.crypto.type; ATL has no file-based encryption */
	public static boolean isFileEncrypted() {
		return false;
	}


	/* AOSP StorageManager.openProxyFileDescriptor(int, ProxyFileDescriptorCallback, Handler): checkNotNull(handler)
	 * and the callback, then the AppFuse bridge of the StorageManagerService (mountProxyFileDescriptorBridge()).
	 * ATL has no such service, so the bridge cannot be mounted: IOException("Failed to mount proxy bridge"). */
	public android.os.ParcelFileDescriptor openProxyFileDescriptor(int mode, android.os.ProxyFileDescriptorCallback callback, android.os.Handler handler) throws IOException {
		java.util.Objects.requireNonNull(handler);
		java.util.Objects.requireNonNull(callback);
		throw new IOException("Failed to mount proxy bridge");
	}

	// AOSP: uuid -v5 ad99aa3d-308e-4191-a200-ebcab371c0ad default
	public static final UUID UUID_DEFAULT = UUID.fromString("41217664-9172-527a-b3d5-edabb50a7d69");
	public static final String UUID_PRIVATE_INTERNAL = null;
	public static final String UUID_PRIMARY_PHYSICAL = "primary_physical";
	public static final String UUID_SYSTEM = "system";
	private static final String FAT_UUID_PREFIX = "fafafafa-fafa-5afa-8afa-fafa";
	public static final UUID UUID_PRIMARY_PHYSICAL_ = UUID.fromString("0f95a519-dae7-5abf-9519-fbd6209e05fd");
	public static final UUID UUID_SYSTEM_ = UUID.fromString("5d258386-e60d-59e3-826d-0089cdd42cc0");
	public static final int FLAG_FOR_WRITE = 1 << 8;
	public static final int FLAG_REAL_STATE = 1 << 9;
	public static final int FLAG_INCLUDE_INVISIBLE = 1 << 10;

	/* AOSP StorageManager.convert(String): the UUID for a volume's fsUuid; FAT identifiers XXXX-YYYY become
	 * fafafafa-fafa-5afa-8afa-fafaXXXXYYYY */
	public static UUID convert(String uuid) {
		if (java.util.Objects.equals(uuid, UUID_PRIVATE_INTERNAL)) {
			return UUID_DEFAULT;
		} else if (java.util.Objects.equals(uuid, UUID_PRIMARY_PHYSICAL)) {
			return UUID_PRIMARY_PHYSICAL_;
		} else if (java.util.Objects.equals(uuid, UUID_SYSTEM)) {
			return UUID_SYSTEM_;
		} else if (uuid.length() == 9 && uuid.charAt(4) == '-') {
			return UUID.fromString(FAT_UUID_PREFIX + uuid.replace("-", ""));
		} else {
			return UUID.fromString(uuid);
		}
	}

	/* AOSP StorageManager.convert(UUID) */
	public static String convert(UUID storageUuid) {
		if (UUID_DEFAULT.equals(storageUuid)) {
			return UUID_PRIVATE_INTERNAL;
		} else if (UUID_PRIMARY_PHYSICAL_.equals(storageUuid)) {
			return UUID_PRIMARY_PHYSICAL;
		} else if (UUID_SYSTEM_.equals(storageUuid)) {
			return UUID_SYSTEM;
		}
		String uuidString = storageUuid.toString();
		if (uuidString.startsWith(FAT_UUID_PREFIX)) {
			String fatStr = uuidString.substring(FAT_UUID_PREFIX.length()).toUpperCase(java.util.Locale.US);
			return fatStr.substring(0, 4) + "-" + fatStr.substring(4);
		}
		return uuidString;
	}

	/* AOSP StorageManager.getPrimaryStorageVolume(): the first volume of the list is the primary one */
	public StorageVolume getPrimaryStorageVolume() {
		return getVolumeList()[0];
	}

	/* AOSP static getVolumeList(int, int): the volumes StorageManagerService reports to the app; ATL has one */
	public static StorageVolume[] getVolumeList(int userId, int flags) {
		return new StorageVolume[] {new StorageVolume()};
	}

	/* AOSP isObbMounted(): ATL never mounts an OBB image, so none is mounted */
	public boolean isObbMounted(String rawPath) {
		if (rawPath == null)
			throw new NullPointerException("rawPath cannot be null");
		return false;
	}

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

	/* AOSP getStorageVolume(Uri): the volume of a MediaStore item; ATL has one volume, MediaStore's external_primary
	 * (a synthetic "external" name resolves to it), any other name finds no volume as in AOSP's loop */
	public StorageVolume getStorageVolume(android.net.Uri uri) {
		String volumeName = android.provider.MediaStore.getVolumeName(uri);
		if (android.provider.MediaStore.VOLUME_EXTERNAL.equals(volumeName)) {
			volumeName = android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY;
		}
		if (android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY.equals(volumeName)) {
			return getPrimaryStorageVolume();
		}
		for (StorageVolume vol : getStorageVolumes()) {
			if (java.util.Objects.equals(vol.getMediaStoreVolumeName(), volumeName)) {
				return vol;
			}
		}
		throw new IllegalStateException("Unknown volume for " + uri);
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

	/* AOSP allocateBytes(FileDescriptor, long): AOSP asks for the missing space, then posix_fallocate()s and falls back
	 * to ftruncate(); ATL has no libcore Os, so the file is only grown to the size (the ftruncate() fallback) */
	public void allocateBytes(java.io.FileDescriptor fd, long bytes) throws IOException {
		java.nio.channels.FileChannel channel = new java.io.FileOutputStream(fd).getChannel();
		long have = channel.size();
		long need = bytes - have;
		if (need > 0) {
			allocateBytes(UUID_DEFAULT, need);
			channel.write(java.nio.ByteBuffer.allocate(1), bytes - 1);
		}
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
