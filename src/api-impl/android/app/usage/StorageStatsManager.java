package android.app.usage;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

/* AOSP StorageStatsManager.queryStatsForPackage(). AOSP's StorageStatsService asks installd for the sizes;
 * ATL has no installd, so the package's files are measured here as installd's getAppSize() does: code = the
 * APKs, data = the data directory (the cache included), cache = cache and code_cache */
public class StorageStatsManager {
	private final Context mContext;

	/** {@hide} */
	public StorageStatsManager(Context context) {
		mContext = context;
	}

	public StorageStats queryStatsForPackage(UUID storageUuid, String packageName, UserHandle user)
	    throws PackageManager.NameNotFoundException, IOException {
		final ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(packageName, 0);
		final StorageStats stats = new StorageStats();
		stats.codeBytes = sizeOf(appInfo.sourceDir);
		if (appInfo.splitSourceDirs != null) {
			for (String split : appInfo.splitSourceDirs)
				stats.codeBytes += sizeOf(split);
		}
		if (appInfo.dataDir != null) {
			stats.dataBytes = sizeOf(new File(appInfo.dataDir));
			stats.cacheBytes = sizeOf(new File(appInfo.dataDir, "cache")) + sizeOf(new File(appInfo.dataDir, "code_cache"));
		}
		return stats;
	}

	/* AOSP StorageStatsService.getTotalBytes()/getFreeBytes() for the internal volume: the size of /data and
	 * what is free there (AOSP also rounds the total up to a storage size and adds the clearable cache to
	 * the free bytes; ATL does neither). */
	public long getTotalBytes(UUID storageUuid) throws IOException {
		return android.os.Environment.getDataDirectory().getTotalSpace();
	}

	public long getFreeBytes(UUID storageUuid) throws IOException {
		return android.os.Environment.getDataDirectory().getUsableSpace();
	}

	/* AOSP queryStatsForUid(): the sum of the packages of the uid; ATL's only package is the app's own */
	public StorageStats queryStatsForUid(UUID storageUuid, int uid) throws IOException {
		if (uid != android.os.Process.myUid())
			return new StorageStats();
		try {
			return queryStatsForPackage(storageUuid, mContext.getPackageName(), android.os.Process.myUserHandle());
		} catch (PackageManager.NameNotFoundException e) {
			throw new IOException(e);
		}
	}

	/* AOSP queryStatsForUser(): the sum over the packages of the user; ATL's only package is the app's own */
	public StorageStats queryStatsForUser(UUID storageUuid, UserHandle user) throws IOException {
		return queryStatsForUid(storageUuid, android.os.Process.myUid());
	}

	private static long sizeOf(String path) {
		return path != null ? sizeOf(new File(path)) : 0;
	}

	/* installd calculate_tree_size(): the files below the directory, symlinks not followed */
	private static long sizeOf(File file) {
		if (Files.isSymbolicLink(file.toPath()))
			return 0;
		if (!file.isDirectory())
			return file.length();
		long size = 0;
		File[] children = file.listFiles();
		if (children != null) {
			for (File child : children)
				size += sizeOf(child);
		}
		return size;
	}
}
