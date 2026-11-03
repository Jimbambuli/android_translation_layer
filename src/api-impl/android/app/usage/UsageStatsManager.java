package android.app.usage;

public class UsageStatsManager {
	/* AOSP UsageStatsManager standby buckets */
	public static final int STANDBY_BUCKET_EXEMPTED = 5;
	public static final int STANDBY_BUCKET_ACTIVE = 10;
	public static final int STANDBY_BUCKET_WORKING_SET = 20;
	public static final int STANDBY_BUCKET_FREQUENT = 30;
	public static final int STANDBY_BUCKET_RARE = 40;
	public static final int STANDBY_BUCKET_RESTRICTED = 45;
	public static final int STANDBY_BUCKET_NEVER = 50;

	/* AOSP asks AppStandbyController; an app ATL runs is in use, which is the active bucket */
	public int getAppStandbyBucket() {
		return STANDBY_BUCKET_ACTIVE;
	}
}
