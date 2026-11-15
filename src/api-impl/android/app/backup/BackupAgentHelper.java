package android.app.backup;

import android.os.ParcelFileDescriptor;
import java.io.IOException;
import java.util.TreeMap;

/* AOSP android.app.backup.BackupAgentHelper: the helpers by key prefix (BackupHelperDispatcher.mHelpers, a TreeMap).
 * The dispatching of a backup or restore (BackupHelperDispatcher, native headers in the state file) is not
 * provided; ATL has no backup transport, so the backup system never calls onBackup()/onRestore(). */
public class BackupAgentHelper extends BackupAgent {
	private final TreeMap<String, BackupHelper> mHelpers = new TreeMap<>();

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
		throw new UnsupportedOperationException("BackupHelperDispatcher is not provided");
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
		throw new UnsupportedOperationException("BackupHelperDispatcher is not provided");
	}

	/* AOSP BackupHelperDispatcher.addHelper(): mHelpers.put(keyPrefix, helper) */
	public void addHelper(String keyPrefix, BackupHelper helper) {
		mHelpers.put(keyPrefix, helper);
	}
}
