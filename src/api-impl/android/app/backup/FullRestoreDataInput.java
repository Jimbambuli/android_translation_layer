package android.app.backup;

import android.os.ParcelFileDescriptor;
import java.io.File;

/* AOSP android.app.backup.FullRestoreDataInput (android-16.0.0_r4, FLAG_ENABLE_CROSS_PLATFORM_TRANSFER) */
public class FullRestoreDataInput {
	private final ParcelFileDescriptor mData;
	private final long mSize;
	private final File mDestination;
	private final int mType;
	private final long mMode;
	private final long mModificationTime;
	private final long mAppVersionCode;
	private final int mTransportFlags;
	private final String mContentVersion;

	public FullRestoreDataInput(ParcelFileDescriptor data, long size, File destination, int type, long mode,
	                            long mtime, long appVersionCode, int transportFlags, String contentVersion) {
		this.mData = data;
		this.mSize = size;
		this.mDestination = destination;
		this.mType = type;
		this.mMode = mode;
		this.mModificationTime = mtime;
		this.mAppVersionCode = appVersionCode;
		this.mTransportFlags = transportFlags;
		this.mContentVersion = contentVersion;
	}

	public ParcelFileDescriptor getData() {
		return mData;
	}

	public long getSize() {
		return mSize;
	}

	public File getDestination() {
		return mDestination;
	}

	public int getType() {
		return mType;
	}

	public long getMode() {
		return mMode;
	}

	public long getModificationTimeSeconds() {
		return mModificationTime;
	}

	public long getAppVersionCode() {
		return mAppVersionCode;
	}

	public int getTransportFlags() {
		return mTransportFlags;
	}

	public String getContentVersion() {
		return mContentVersion;
	}
}
