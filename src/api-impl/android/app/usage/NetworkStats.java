package android.app.usage;

/* AOSP NetworkStats (modules/Connectivity framework-t). ATL has no INetworkStatsService, so the session is
 * empty: a summary with no entries. The enumeration methods follow AOSP's summary enumeration over it. */
public final class NetworkStats implements AutoCloseable {
	public static class Bucket {
		public static final int STATE_ALL = -1;
		public static final int STATE_DEFAULT = 0x1;
		public static final int STATE_FOREGROUND = 0x2;

		private int mState;
		private long mRxBytes;
		private long mTxBytes;

		public Bucket() {
		}

		public int getState() {
			return mState;
		}

		public long getRxBytes() {
			return mRxBytes;
		}

		public long getTxBytes() {
			return mTxBytes;
		}
	}

	/* AOSP: NetworkStats(...) opens a session and startSummaryEnumeration() fetches the summary; here it has no entries */
	NetworkStats() {
	}

	/* AOSP getNextBucket() -> getNextSummaryBucket(): false when the summary is exhausted (and for a null bucket) */
	public boolean getNextBucket(Bucket bucketOut) {
		return false;
	}

	/* AOSP hasNextBucket(): mEnumerationIndex < mSummary.size(), i.e. 0 < 0 */
	public boolean hasNextBucket() {
		return false;
	}

	/* AOSP close(): closes the session; there is none */
	@Override
	public void close() {
	}
}
