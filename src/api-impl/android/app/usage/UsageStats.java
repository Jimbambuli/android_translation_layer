package android.app.usage;

/* AOSP UsageStats without the Parcelable part, the per-component bookkeeping and merging; what
 * UsageStatsManager.queryUsageStats() returns, which is empty in ATL (no UsageStatsService) */
public final class UsageStats {
	public String mPackageName;
	public long mBeginTimeStamp;
	public long mEndTimeStamp;
	public long mLastTimeUsed;
	public long mLastTimeVisible;
	public long mLastTimeForegroundServiceUsed;
	public long mTotalTimeInForeground;
	public long mTotalTimeVisible;
	public long mTotalTimeForegroundServiceUsed;
	public long mLastTimeComponentUsed;
	public int mAppLaunchCount;

	public UsageStats() {}

	public UsageStats(UsageStats stats) {
		mPackageName = stats.mPackageName;
		mBeginTimeStamp = stats.mBeginTimeStamp;
		mEndTimeStamp = stats.mEndTimeStamp;
		mLastTimeUsed = stats.mLastTimeUsed;
		mLastTimeVisible = stats.mLastTimeVisible;
		mLastTimeForegroundServiceUsed = stats.mLastTimeForegroundServiceUsed;
		mTotalTimeInForeground = stats.mTotalTimeInForeground;
		mTotalTimeVisible = stats.mTotalTimeVisible;
		mTotalTimeForegroundServiceUsed = stats.mTotalTimeForegroundServiceUsed;
		mLastTimeComponentUsed = stats.mLastTimeComponentUsed;
		mAppLaunchCount = stats.mAppLaunchCount;
	}

	public String getPackageName() { return mPackageName; }
	public long getFirstTimeStamp() { return mBeginTimeStamp; }
	public long getLastTimeStamp() { return mEndTimeStamp; }
	public long getLastTimeUsed() { return mLastTimeUsed; }
	public long getLastTimeVisible() { return mLastTimeVisible; }
	public long getTotalTimeInForeground() { return mTotalTimeInForeground; }
	public long getTotalTimeVisible() { return mTotalTimeVisible; }
	public long getLastTimeForegroundServiceUsed() { return mLastTimeForegroundServiceUsed; }
	public long getTotalTimeForegroundServiceUsed() { return mTotalTimeForegroundServiceUsed; }
	public long getLastTimeAnyComponentUsed() { return mLastTimeComponentUsed; }
	public long getLastTimePackageUsed() { return Math.max(mLastTimeUsed, Math.max(mLastTimeVisible, Math.max(mLastTimeForegroundServiceUsed, mLastTimeComponentUsed))); }
	public int getAppLaunchCount() { return mAppLaunchCount; }
}
