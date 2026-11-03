package android.os.storage;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.util.UUID;

/* AOSP StorageVolume with the fields StorageManagerService fills in for the primary emulated volume,
 * the only volume ATL has (external storage, always mounted) */
public class StorageVolume {
	private final File mPath = Environment.getExternalStorageDirectory();
	private final String mDescription = "Internal shared storage"; // AOSP R.string.storage_internal
	private final boolean mPrimary = true;
	private final boolean mRemovable = false;
	private final boolean mEmulated = true;
	private final UUID mUuid = StorageManager.UUID_DEFAULT;
	private final String mFsUuid = null;
	private final String mState = Environment.MEDIA_MOUNTED;

	public String getPath() {
		return mPath.toString();
	}

	public File getPathFile() {
		return mPath;
	}

	public File getDirectory() {
		switch (mState) {
			case Environment.MEDIA_MOUNTED:
			case Environment.MEDIA_MOUNTED_READ_ONLY:
				return mPath;
			default:
				return null;
		}
	}

	public String getDescription(Context context) {
		return mDescription;
	}

	public boolean isPrimary() {
		return mPrimary;
	}

	public boolean isRemovable() {
		return mRemovable;
	}

	public boolean isEmulated() {
		return mEmulated;
	}

	public UUID getStorageUuid() {
		return mUuid;
	}

	public String getUuid() {
		return mFsUuid;
	}

	public String getState() {
		return mState;
	}
}
