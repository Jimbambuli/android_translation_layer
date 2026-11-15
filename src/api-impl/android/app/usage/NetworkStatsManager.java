package android.app.usage;

import android.net.ConnectivityManager;

/* AOSP NetworkStatsManager (modules/Connectivity framework-t). ATL has no INetworkStatsService and
 * Context.NETWORK_STATS_SERVICE is not registered, so getSystemService() gives null and no instance arises. */
public class NetworkStatsManager {
	/* AOSP querySummary(): createTemplate() accepts TYPE_MOBILE and TYPE_WIFI and throws
	 * IllegalArgumentException otherwise, which querySummary() turns into null; a valid type
	 * yields a NetworkStats from the service, here one without entries */
	public NetworkStats querySummary(int networkType, String subscriberId, long startTime, long endTime)
	    throws SecurityException, android.os.RemoteException {
		if (networkType != ConnectivityManager.TYPE_MOBILE && networkType != ConnectivityManager.TYPE_WIFI) {
			return null;
		}
		return new NetworkStats();
	}
}
