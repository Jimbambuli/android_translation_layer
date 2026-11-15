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

	public static final int RESULT_SUCCESS = 0;
	public static final int RESULT_ERROR = -1;
	public static final int TYPE_EOF = 0;
	public static final int TYPE_FILE = 1;
	public static final int TYPE_DIRECTORY = 2;
	public static final int TYPE_SYMLINK = 3;
	public static final int FLAG_CLIENT_SIDE_ENCRYPTION_ENABLED = 1;
	public static final int FLAG_DEVICE_TO_DEVICE_TRANSFER = 2;
	public static final int FLAG_SKIP_RESTORE_FOR_LAUNCHED_APPS = 1 << 2;
	public static final int FLAG_CROSS_PLATFORM_DATA_TRANSFER_IOS = 1 << 3;
	public static final int FLAG_FAKE_CLIENT_SIDE_ENCRYPTION_ENABLED = 1 << 31;

	/* AOSP BackupAgent.onRestore(BackupDataInput, long, ParcelFileDescriptor): the int variant with the version
	 * code cut to int */
	public void onRestore(BackupDataInput data, long appVersionCode, android.os.ParcelFileDescriptor newState) throws java.io.IOException {
		onRestore(data, (int) appVersionCode, newState);
	}

	/* AOSP BackupAgent.onFullBackup(): backs up the files the app's backup rules name. ATL has no backup transport,
	 * so there is no FullBackupDataOutput stream to write them to (see fullBackupFile()): nothing is backed up,
	 * which is what AOSP does as well when the full backup is not enabled for the transport. */
	public void onFullBackup(FullBackupDataOutput data) throws java.io.IOException {
	}

	/* AOSP BackupAgent.onEstimateFullBackupBytes(): -1, the agent gives no estimate */
	public long onEstimateFullBackupBytes(long quotaBytes, int transportFlags) throws java.io.IOException {
		return -1;
	}

	/* AOSP BackupAgent.onRestoreFile(FullRestoreDataInput): the six-argument variant with the values of the input */
	public void onRestoreFile(FullRestoreDataInput data) throws java.io.IOException {
		onRestoreFile(data.getData(), data.getSize(), data.getDestination(), data.getType(), data.getMode(),
		              data.getModificationTimeSeconds());
	}

	/* AOSP BackupAgent.onRestoreFile(): FullBackup.restoreFile() (native): copies size bytes from the pipe to the
	 * destination (a file, or a directory for TYPE_DIRECTORY), then sets the mode when it is not negative and the
	 * modification time; the bytes are read and dropped when the destination is null. (AOSP first asks the
	 * app's backup rules whether the file may be restored, ATL has no backup scheme.) */
	public void onRestoreFile(android.os.ParcelFileDescriptor data, long size, java.io.File destination, int type, long mode, long mtime) throws java.io.IOException {
		java.io.InputStream in = new java.io.FileInputStream(data.getFileDescriptor());
		if (type == TYPE_DIRECTORY) {
			if (destination != null) {
				destination.mkdirs();
				applyModeAndTime(destination, mode, mtime);
			}
			return;
		}
		java.io.OutputStream out = null;
		if (destination != null) {
			java.io.File parent = destination.getParentFile();
			if (parent != null)
				parent.mkdirs();
			out = new java.io.FileOutputStream(destination);
		}
		try {
			byte[] buffer = new byte[32 * 1024];
			long remaining = size;
			while (remaining > 0) {
				int n = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
				if (n < 0)
					break;
				if (out != null)
					out.write(buffer, 0, n);
				remaining -= n;
			}
		} finally {
			if (out != null)
				out.close();
		}
		if (destination != null)
			applyModeAndTime(destination, mode, mtime);
	}

	private static void applyModeAndTime(java.io.File f, long mode, long mtime) {
		if (mode >= 0) {
			f.setReadable((mode & 0444) != 0, (mode & 0004) == 0);
			f.setWritable((mode & 0222) != 0, (mode & 0002) == 0);
			f.setExecutable((mode & 0111) != 0, (mode & 0001) == 0);
		}
		if (mtime > 0)
			f.setLastModified(mtime * 1000);
	}

	/* AOSP BackupAgent.onRestoreFinished(): empty */
	public void onRestoreFinished() {}
}
