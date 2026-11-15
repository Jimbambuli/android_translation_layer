package android.app.backup;

import android.content.ContextWrapper;

public abstract class BackupAgent extends ContextWrapper {
	public BackupAgent() {
		super(null);
	}

	/* AOSP BackupAgent (android-16.0.0_r4): lifecycle hooks that do nothing by default. ATL never runs a backup
	 * agent. */
	public void onCreate() {}

	public void onDestroy() {}

	public void onQuotaExceeded(long backupDataBytes, long quotaBytes) {}

	/* AOSP BackupAgent: the key/value backup and restore every agent implements */
	public abstract void onBackup(android.os.ParcelFileDescriptor oldState, BackupDataOutput data, android.os.ParcelFileDescriptor newState) throws java.io.IOException;

	public abstract void onRestore(BackupDataInput data, int appVersionCode, android.os.ParcelFileDescriptor newState) throws java.io.IOException;

	/* AOSP BackupAgent.fullBackupFile(): writes the file into the tar stream of FullBackupDataOutput.getData()
	 * (FullBackup.backupToTar, native) during onFullBackup(). ATL has no backup transport, never runs a full backup,
	 * and an app's FullBackupDataOutput has no data stream, so nothing is written. */
	public final void fullBackupFile(java.io.File file, FullBackupDataOutput output) {
		android.util.Log.w("BackupAgent", "fullBackupFile(" + file + "): no full backup in progress");
	}
}
