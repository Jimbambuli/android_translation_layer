package android.app.backup;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import java.io.File;

/* AOSP android.app.backup.FileBackupHelper. The constructor is as in AOSP; the backup and restore go through
 * FileBackupHelperBase, whose state-file handling is native (performBackup_native, writeFile_native) and is not
 * provided. ATL has no backup transport, so the backup system never calls them. */
public class FileBackupHelper implements BackupHelper {
	Context mContext;
	File mFilesDir;
	String[] mFiles;

	public FileBackupHelper(Context context, String... files) {
		mContext = context;
		mFilesDir = context.getFilesDir();
		mFiles = files;
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
