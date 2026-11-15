package android.app.usage;

/* AOSP StorageStats without the Parcelable part */
public final class StorageStats implements android.os.Parcelable {
	/** {@hide} */ public long codeBytes;
	/** {@hide} */ public long dataBytes;
	/** {@hide} */ public long cacheBytes;
	/** {@hide} */ public long externalCacheBytes;

	public long getAppBytes() {
		return codeBytes;
	}

	public long getDataBytes() {
		return dataBytes;
	}

	public long getCacheBytes() {
		return cacheBytes;
	}

	public long getExternalCacheBytes() {
		return externalCacheBytes;
	}

	/** {@hide} */
	public StorageStats() {
	}

	/* AOSP StorageStats.writeToParcel()/StorageStats(Parcel)/CREATOR (Android 16): the fields ATL has, in AOSP order.
	 * Not parcelled (no such field or helper in ATL): packageName, userHandle, uid, dexoptBytes, refProfBytes, curProfBytes, apkBytes, libBytes, dmBytes */
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeLong(codeBytes);
		dest.writeLong(dataBytes);
		dest.writeLong(cacheBytes);
		dest.writeLong(externalCacheBytes);
	}

	private StorageStats(android.os.Parcel in) {
		this.codeBytes = in.readLong();
		this.dataBytes = in.readLong();
		this.cacheBytes = in.readLong();
		this.externalCacheBytes = in.readLong();
	}

	public static final android.os.Parcelable.Creator<StorageStats> CREATOR = new android.os.Parcelable.Creator<StorageStats>() {
		public StorageStats createFromParcel(android.os.Parcel source) {
			return new StorageStats(source);
		}

		public StorageStats[] newArray(int size) {
			return new StorageStats[size];
		}
	};
}
