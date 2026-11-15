package android.webkit;

import android.content.Context;

/* AOSP CookieSyncManager (deprecated): createInstance() must be called before getInstance() */
@Deprecated
public final class CookieSyncManager extends WebSyncManager {
	private static CookieSyncManager sRef;
	private static boolean sGetInstanceAllowed = false;
	private static final Object sLock = new Object();

	private CookieSyncManager() {
		super(null, null);
	}

	public static CookieSyncManager getInstance() {
		synchronized (sLock) {
			checkInstanceIsAllowed();
			if (sRef == null) {
				sRef = new CookieSyncManager();
			}
			return sRef;
		}
	}

	public static CookieSyncManager createInstance(Context context) {
		synchronized (sLock) {
			if (context == null) {
				throw new IllegalArgumentException("Invalid context argument");
			}
			setGetInstanceIsAllowed();
			return getInstance();
		}
	}

	@Deprecated
	public void sync() {
		CookieManager.getInstance().flush();
	}

	@Deprecated
	protected void syncFromRamToFlash() {
		CookieManager.getInstance().flush();
	}

	@Deprecated
	public void resetSync() {}

	@Deprecated
	public void startSync() {}

	@Deprecated
	public void stopSync() {}

	static void setGetInstanceIsAllowed() {
		sGetInstanceAllowed = true;
	}

	private static void checkInstanceIsAllowed() {
		if (!sGetInstanceAllowed) {
			throw new IllegalStateException("CookieSyncManager::createInstance() needs to be called before CookieSyncManager::getInstance()");
		}
	}
}
