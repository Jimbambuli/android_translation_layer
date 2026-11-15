package android.app.backup;

/* AOSP android.app.backup.FullBackupDataOutput (android-16.0.0_r4). The ParcelFileDescriptor constructors are
 * @hide and wrap a BackupDataOutput, which ATL does not provide; only the public ones exist. */
public class FullBackupDataOutput {
	private final BackupDataOutput mData;
	private final long mQuota;
	private final int mTransportFlags;
	private long mSize;

	public long getQuota() {
		return mQuota;
	}

	public int getTransportFlags() {
		return mTransportFlags;
	}

	public FullBackupDataOutput(long quota) {
		mData = null;
		mQuota = quota;
		mSize = 0;
		mTransportFlags = 0;
	}

	public FullBackupDataOutput(long quota, int transportFlags) {
		mData = null;
		mQuota = quota;
		mSize = 0;
		mTransportFlags = transportFlags;
	}

	public BackupDataOutput getData() {
		return mData;
	}

	public void addSize(long size) {
		if (size > 0) {
			mSize += size;
		}
	}

	public long getSize() {
		return mSize;
	}
}
