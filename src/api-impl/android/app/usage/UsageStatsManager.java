package android.app.usage;

import java.util.Collections;
import java.util.List;

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

	/* AOSP: without events from UsageStatsService (no PACKAGE_USAGE_STATS, or nothing recorded) the empty
	 * results; ATL has no UsageStatsService */
	private static final UsageEvents sEmptyResults = new UsageEvents();

	public static final int INTERVAL_DAILY = 0;
	public static final int INTERVAL_WEEKLY = 1;
	public static final int INTERVAL_MONTHLY = 2;
	public static final int INTERVAL_YEARLY = 3;
	public static final int INTERVAL_BEST = 4;

	public List<UsageStats> queryUsageStats(int intervalType, long beginTime, long endTime) {
		return Collections.emptyList();
	}

	public UsageEvents queryEvents(long beginTime, long endTime) {
		return sEmptyResults;
	}

	/* AOSP: the service's answer, empty results when there is none (ATL has no UsageStatsService) */
	public UsageEvents queryEvents(UsageEventsQuery query) {
		return sEmptyResults;
	}

	public UsageEvents queryEventsForSelf(long beginTime, long endTime) {
		return sEmptyResults;
	}

	/* AOSP: the service's answer, false when it has none (an app ATL runs is in use) */
	public boolean isAppInactive(String packageName) {
		return false;
	}
}
