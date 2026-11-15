package android.os;

/* AOSP PowerWhitelistManager (apex/jobscheduler, deprecated). Context.getSystemService(POWER_WHITELIST_MANAGER)
 * is not registered in ATL (hidden service name), so no instance arises here either. */
public class PowerWhitelistManager {
	/* AOSP isWhitelisted() -> PowerExemptionManager.isAllowListed() -> PowerManager service; ATL has none and
	 * its PowerManager.isIgnoringBatteryOptimizations() (the same query) reports true, so this does too. */
	public boolean isWhitelisted(String packageName, boolean includingIdle) {
		return true;
	}
}
