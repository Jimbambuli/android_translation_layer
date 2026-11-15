package android.app.backup;

import android.os.ParcelFileDescriptor;

/* AOSP android.app.backup.BackupHelper */
public interface BackupHelper {
	public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState);

	public void restoreEntity(BackupDataInputStream data);

	public void writeNewStateDescription(ParcelFileDescriptor newState);
}
