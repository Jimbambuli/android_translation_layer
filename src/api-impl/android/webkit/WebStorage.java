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
}
