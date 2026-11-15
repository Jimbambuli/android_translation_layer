package android.content;

import android.os.Parcel;
import android.os.Parcelable;

public class SyncStats implements Parcelable {
	public long numAuthExceptions;
	public long numIoExceptions;
	public long numParseExceptions;
	public long numConflictDetectedExceptions;
	public long numInserts;
	public long numUpdates;
	public long numDeletes;
	public long numEntries;
	public long numSkippedEntries;

	public SyncStats() {
	}

	public SyncStats(Parcel in) {
		numAuthExceptions = in.readLong();
		numIoExceptions = in.readLong();
		numParseExceptions = in.readLong();
		numConflictDetectedExceptions = in.readLong();
		numInserts = in.readLong();
		numUpdates = in.readLong();
		numDeletes = in.readLong();
		numEntries = in.readLong();
		numSkippedEntries = in.readLong();
	}

	public void clear() {
		numAuthExceptions = 0;
		numIoExceptions = 0;
		numParseExceptions = 0;
		numConflictDetectedExceptions = 0;
		numInserts = 0;
		numUpdates = 0;
		numDeletes = 0;
		numEntries = 0;
		numSkippedEntries = 0;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(numAuthExceptions);
		dest.writeLong(numIoExceptions);
		dest.writeLong(numParseExceptions);
		dest.writeLong(numConflictDetectedExceptions);
		dest.writeLong(numInserts);
		dest.writeLong(numUpdates);
		dest.writeLong(numDeletes);
		dest.writeLong(numEntries);
		dest.writeLong(numSkippedEntries);
	}

	public static final Creator<SyncStats> CREATOR = new Creator<SyncStats>() {
		public SyncStats createFromParcel(Parcel in) {
			return new SyncStats(in);
		}

		public SyncStats[] newArray(int size) {
			return new SyncStats[size];
		}
	};

	@Override
	public String toString() {
		return "SyncStats{auth=" + numAuthExceptions + ", io=" + numIoExceptions + ", parse=" + numParseExceptions
		     + ", conflict=" + numConflictDetectedExceptions + ", inserts=" + numInserts + ", updates=" + numUpdates
		     + ", deletes=" + numDeletes + ", entries=" + numEntries + ", skipped=" + numSkippedEntries + "}";
	}
}
