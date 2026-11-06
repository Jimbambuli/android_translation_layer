package android.app.backup;

import android.content.Context;

/* AOSP BackupManager: ATL has no backup transport, the requests are accepted and nothing is backed up */
public class BackupManager {
	public BackupManager(Context context) {
	}

	public void dataChanged() {}

	public static void dataChanged(String packageName) {}


	public void cancelBackups() {}
}
