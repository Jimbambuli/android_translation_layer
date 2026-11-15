package android.os.health;

/* AOSP SystemHealthManager. ATL has no IBatteryStats service (Context.SYSTEM_HEALTH_SERVICE is not registered),
 * so no instance arises from getSystemService(); the type keeps the no-data result. */
public class SystemHealthManager {
	/* AOSP takeMyUidSnapshot() -> takeUidSnapshot(Process.myUid()) -> takeUidSnapshots(): without snapshot data
	 * (RESULT_OK, null bundle) the result array holds null, and takeUidSnapshot() returns that null */
	public HealthStats takeMyUidSnapshot() {
		return null;
	}
}
