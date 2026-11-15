package android.app.backup;

import java.io.InputStream;

/* AOSP BackupDataInputStream: one entity of a restore, made by BackupHelperDispatcher from a BackupDataInput.
 * ATL never restores (no backup transport), so no instance exists. The class is here for the type. */
public class BackupDataInputStream extends InputStream {
	String key;
	int dataSize;

	BackupDataInputStream() {}

	@Override
	public int read() throws java.io.IOException {
		return -1;
	}

	public String getKey() {
		return key;
	}

	public int size() {
		return dataSize;
	}
}
