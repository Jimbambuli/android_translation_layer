package android.webkit;

public class WebStorage {

	/**
	 * Interface to receive notifications when the quota of a web database is exceeded (android-14 WebStorage.QuotaUpdater).
	 * @deprecated This class is no longer used.
	 */
	@Deprecated
	public interface QuotaUpdater {
		/**
		 * Provides a new quota, specified in bytes.
		 */
		public void updateQuota(long newQuota);
	};

	public static WebStorage getInstance() {
		return new WebStorage();
	}

	public void deleteAllData() {}

	/* AOSP WebStorage: the base class does nothing (the Chromium adapter keeps no WebSQL/AppCache origins) */
	public void getOrigins(ValueCallback<java.util.Map> callback) {}
	public void getUsageForOrigin(String origin, ValueCallback<Long> callback) {}
	public void getQuotaForOrigin(String origin, ValueCallback<Long> callback) {}
	@Deprecated
	public void setQuotaForOrigin(String origin, long quota) {}
	public void deleteOrigin(String origin) {}
}
