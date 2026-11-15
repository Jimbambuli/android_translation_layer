package android.app.backup;

import android.content.Context;
import android.os.ParcelFileDescriptor;

/* AOSP android.app.backup.SharedPreferencesBackupHelper. The constructor is as in AOSP; the backup and restore go
 * through FileBackupHelperBase, whose state-file handling is native and is not provided (see FileBackupHelper). ATL
 * has no backup transport, so the backup system never calls them. */
public class SharedPreferencesBackupHelper implements BackupHelper {
	private Context mContext;
	private String[] mPrefGroups;

	public SharedPreferencesBackupHelper(Context context, String... prefGroups) {
		mContext = context;
		mPrefGroups = prefGroups;
	}

	public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
		throw new UnsupportedOperationException("FileBackupHelperBase is not provided");
	}

	public void restoreEntity(BackupDataInputStream data) {
		throw new UnsupportedOperationException("FileBackupHelperBase is not provided");
	}

	public void writeNewStateDescription(ParcelFileDescriptor fd) {
		throw new UnsupportedOperationException("FileBackupHelperBase is not provided");
	}
}
