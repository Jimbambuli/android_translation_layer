package android.app.backup;

import android.content.ContextWrapper;

public abstract class BackupAgent extends ContextWrapper {
	public BackupAgent() {
		super(null);
	}

	/* AOSP BackupAgent (android-16.0.0_r4): lifecycle hooks that do nothing by default. ATL never runs a backup
	 * agent; onBackup/onRestore/onFullBackup need BackupDataInput/Output, which are native in AOSP. */
	public void onCreate() {}

	public void onDestroy() {}

	public void onQuotaExceeded(long backupDataBytes, long quotaBytes) {}
}
