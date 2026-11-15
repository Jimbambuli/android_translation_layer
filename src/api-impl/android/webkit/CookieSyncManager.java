package android.webkit;

import android.content.Context;

/* AOSP frameworks/base core/java/android/webkit/CookieSyncManager.java (Android 16): a singleton on top of
 * WebSyncManager (Runnable); sync() and syncFromRamToFlash() flush the CookieManager. */
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
	public void resetSync() {
	}

	@Deprecated
	public void startSync() {
	}

	@Deprecated
	public void stopSync() {
	}

	static void setGetInstanceIsAllowed() {
		sGetInstanceAllowed = true;
	}

	private static void checkInstanceIsAllowed() {
		// Prior to Android KK, calling createInstance() or constructing a WebView is
		// a hard pre-condition for calling getInstance(). We retain that contract to aid
		// developers targeting a range of SDK levels.
		if (!sGetInstanceAllowed) {
			throw new IllegalStateException(
					"CookieSyncManager::createInstance() needs to be called "
					+ "before CookieSyncManager::getInstance()");
		}
	}
}
