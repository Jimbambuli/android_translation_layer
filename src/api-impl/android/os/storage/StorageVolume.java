package android.os.storage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import java.io.File;
import java.util.UUID;

/* AOSP StorageVolume with the fields StorageManagerService fills in for the primary emulated volume,
 * the only volume ATL has (external storage, always mounted) */
public class StorageVolume implements android.os.Parcelable {
	private File mPath = Environment.getExternalStorageDirectory();
	private String mDescription = "Internal shared storage"; // AOSP R.string.storage_internal
	private boolean mPrimary = true;
	private boolean mRemovable = false;
	private boolean mEmulated = true;
	private UUID mUuid = StorageManager.UUID_DEFAULT;
	private String mFsUuid = null;
	private String mState = Environment.MEDIA_MOUNTED;

	/* AOSP StorageVolume.mId, mInternalPath, mExternallyManaged, mAllowMassStorage, mMaxFileSize and mOwner as StorageManagerService
	 * reports them for the primary emulated volume of the one user: id "emulated", the same path inside, no mass storage */
	private String mId = "emulated";
	private File mInternalPath = mPath;
	private boolean mExternallyManaged = false;
	private boolean mAllowMassStorage = false;
	private long mMaxFileSize = 0;
	private android.os.UserHandle mOwner = android.os.Process.myUserHandle();

	/* AOSP StorageVolume(String, File, File, String, boolean, boolean, boolean, boolean, boolean, long, UserHandle, UUID,
	 * String, String) (hidden) */
	public StorageVolume(String id, File path, File internalPath, String description, boolean primary, boolean removable,
	                     boolean emulated, boolean externallyManaged, boolean allowMassStorage, long maxFileSize,
	                     android.os.UserHandle owner, UUID uuid, String fsUuid, String state) {
		mId = java.util.Objects.requireNonNull(id);
		mPath = java.util.Objects.requireNonNull(path);
		mInternalPath = java.util.Objects.requireNonNull(internalPath);
		mDescription = java.util.Objects.requireNonNull(description);
		mPrimary = primary;
		mRemovable = removable;
		mEmulated = emulated;
		mExternallyManaged = externallyManaged;
		mAllowMassStorage = allowMassStorage;
		mMaxFileSize = maxFileSize;
		mOwner = java.util.Objects.requireNonNull(owner);
		mUuid = uuid;
		mFsUuid = fsUuid;
		mState = java.util.Objects.requireNonNull(state);
	}

	StorageVolume() {}

	private StorageVolume(android.os.Parcel in) {
		mId = in.readString8();
		mPath = new File(in.readString8());
		mInternalPath = new File(in.readString8());
		mDescription = in.readString8();
		mPrimary = in.readInt() != 0;
		mRemovable = in.readInt() != 0;
		mEmulated = in.readInt() != 0;
		mExternallyManaged = in.readInt() != 0;
		mAllowMassStorage = in.readInt() != 0;
		mMaxFileSize = in.readLong();
		mOwner = in.readParcelable(null, android.os.UserHandle.class);
		if (in.readInt() != 0) {
			mUuid = StorageManager.convert(in.readString8());
		} else {
			mUuid = null;
		}
		mFsUuid = in.readString8();
		mState = in.readString8();
	}

	public String getId() {
		return mId;
	}

	public static final Creator<StorageVolume> CREATOR = new Creator<StorageVolume>() {
		@Override
		public StorageVolume createFromParcel(android.os.Parcel in) {
			return new StorageVolume(in);
		}

		@Override
		public StorageVolume[] newArray(int size) {
			return new StorageVolume[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel parcel, int flags) {
		parcel.writeString8(mId);
		parcel.writeString8(mPath.toString());
		parcel.writeString8(mInternalPath.toString());
		parcel.writeString8(mDescription);
		parcel.writeInt(mPrimary ? 1 : 0);
		parcel.writeInt(mRemovable ? 1 : 0);
		parcel.writeInt(mEmulated ? 1 : 0);
		parcel.writeInt(mExternallyManaged ? 1 : 0);
		parcel.writeInt(mAllowMassStorage ? 1 : 0);
		parcel.writeLong(mMaxFileSize);
		parcel.writeParcelable(mOwner, flags);
		if (mUuid != null) {
			parcel.writeInt(1);
			parcel.writeString8(StorageManager.convert(mUuid));
		} else {
			parcel.writeInt(0);
		}
		parcel.writeString8(mFsUuid);
		parcel.writeString8(mState);
	}

	public String getPath() {
		return mPath.toString();
	}

	public File getPathFile() {
		return mPath;
	}

	public File getDirectory() {
		switch (mState) {
			case Environment.MEDIA_MOUNTED:
			case Environment.MEDIA_MOUNTED_READ_ONLY:
				return mPath;
			default:
				return null;
		}
	}

	public String getDescription(Context context) {
		return mDescription;
	}

	public boolean isPrimary() {
		return mPrimary;
	}

	public boolean isRemovable() {
		return mRemovable;
	}

	public boolean isEmulated() {
		return mEmulated;
	}

	public UUID getStorageUuid() {
		return mUuid;
	}

	public String getUuid() {
		return mFsUuid;
	}

	public String getState() {
		return mState;
	}

	/* AOSP getMediaStoreVolumeName(): the primary volume is MediaStore's external_primary */
	public String getMediaStoreVolumeName() {
		if (isPrimary())
			return MediaStore.VOLUME_EXTERNAL_PRIMARY;
		return mFsUuid == null ? null : mFsUuid.toLowerCase(java.util.Locale.US);
	}

	public String getUserLabel() {
		return mDescription;
	}

	/* AOSP createOpenDocumentTreeIntent() */
	public Intent createOpenDocumentTreeIntent() {
		final String rootId = isEmulated() ? DocumentsContract.EXTERNAL_STORAGE_PRIMARY_EMULATED_ROOT_ID : mFsUuid;
		final Uri rootUri = DocumentsContract.buildRootUri(DocumentsContract.EXTERNAL_STORAGE_PROVIDER_AUTHORITY, rootId);
		return new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
		    .putExtra(DocumentsContract.EXTRA_INITIAL_URI, rootUri)
		    .putExtra(DocumentsContract.EXTRA_SHOW_ADVANCED, true);
	}
}
