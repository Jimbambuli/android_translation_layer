package android.app.usage;

/* AOSP StorageStats without the Parcelable part */
public final class StorageStats {
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
}
