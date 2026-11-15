package android.webkit;

import android.content.Context;

/* AOSP frameworks/base core/java/android/webkit/WebSyncManager.java (Android 16): the deprecated, hidden
 * base of CookieSyncManager; every method does nothing. */
@Deprecated
abstract class WebSyncManager implements Runnable {
	protected static final java.lang.String LOGTAG = "websync";
	protected android.webkit.WebViewDatabase mDataBase;
	protected android.os.Handler mHandler;

	protected WebSyncManager(Context context, String name) {
	}

	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("doesn't implement Cloneable");
	}

	public void run() {
	}

	public void sync() {
	}

	public void resetSync() {
	}

	public void startSync() {
	}

	public void stopSync() {
	}

	protected void onSyncInit() {
	}

	abstract void syncFromRamToFlash();
}
