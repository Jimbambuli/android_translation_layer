package android.content;

public final class SyncResult implements android.os.Parcelable {
	public final SyncStats stats;

	public SyncResult() {
		stats = new SyncStats();
	}
	public boolean syncAlreadyInProgress;
	public boolean tooManyDeletions;
	public boolean tooManyRetries;
	public boolean databaseError;
	public boolean fullSyncRequested;
	public boolean partialSyncUnavailable;
	public boolean moreRecordsToGet;
	public long delayUntil;

	public boolean hasHardError() {
		return stats.numParseExceptions > 0 || stats.numConflictDetectedExceptions > 0 || stats.numAuthExceptions > 0
		    || tooManyDeletions || tooManyRetries || databaseError;
	}

	public boolean hasSoftError() {
		return syncAlreadyInProgress || stats.numIoExceptions > 0;
	}

	public boolean hasError() {
		return hasSoftError() || hasHardError();
	}

	@Override
	public String toString() {
		return "SyncResult{" + stats + ", tooManyDeletions=" + tooManyDeletions + ", tooManyRetries=" + tooManyRetries
		     + ", databaseError=" + databaseError + ", fullSyncRequested=" + fullSyncRequested
		     + ", moreRecordsToGet=" + moreRecordsToGet + ", delayUntil=" + delayUntil + "}";
	}

	/* AOSP SyncResult(Parcel)/writeToParcel()/describeContents()/CREATOR */
	private SyncResult(android.os.Parcel parcel) {
		syncAlreadyInProgress = parcel.readInt() != 0;
		tooManyDeletions = parcel.readInt() != 0;
		tooManyRetries = parcel.readInt() != 0;
		databaseError = parcel.readInt() != 0;
		fullSyncRequested = parcel.readInt() != 0;
		partialSyncUnavailable = parcel.readInt() != 0;
		moreRecordsToGet = parcel.readInt() != 0;
		delayUntil = parcel.readLong();
		stats = new SyncStats(parcel);
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel parcel, int flags) {
		parcel.writeInt(syncAlreadyInProgress ? 1 : 0);
		parcel.writeInt(tooManyDeletions ? 1 : 0);
		parcel.writeInt(tooManyRetries ? 1 : 0);
		parcel.writeInt(databaseError ? 1 : 0);
		parcel.writeInt(fullSyncRequested ? 1 : 0);
		parcel.writeInt(partialSyncUnavailable ? 1 : 0);
		parcel.writeInt(moreRecordsToGet ? 1 : 0);
		parcel.writeLong(delayUntil);
		stats.writeToParcel(parcel, flags);
	}

	public static final android.os.Parcelable.Creator<SyncResult> CREATOR = new android.os.Parcelable.Creator<SyncResult>() {
		public SyncResult createFromParcel(android.os.Parcel in) {
			return new SyncResult(in);
		}

		public SyncResult[] newArray(int size) {
			return new SyncResult[size];
		}
	};
}
