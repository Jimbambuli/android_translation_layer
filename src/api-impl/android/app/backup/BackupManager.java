package android.app.backup;

import android.content.Context;

/* AOSP BackupManager: ATL has no backup transport, the requests are accepted and nothing is backed up */
public class BackupManager {
	public BackupManager(Context context) {
	}

	public void dataChanged() {}

	public static void dataChanged(String packageName) {}


	public void cancelBackups() {}

	/* AOSP BackupManager.requestRestore(): without the backup service (checkServiceBinder() leaves sService null)
	 * -1, the restore was not started */
	public int requestRestore(RestoreObserver observer) {
		return -1;
	}

	/* AOSP BackupManager.getUserForAncestralSerialNumber(): without the backup service null */
	public android.os.UserHandle getUserForAncestralSerialNumber(long ancestralSerialNumber) {
		return null;
	}
}
