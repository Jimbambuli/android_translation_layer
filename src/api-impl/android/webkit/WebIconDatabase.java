package android.webkit;

import android.content.ContentResolver;
import android.graphics.Bitmap;

/* AOSP frameworks/base core/java/android/webkit/WebIconDatabase.java (Android 16). getInstance() is the provider's
 * database; Chromium's WebIconDatabaseAdapter (android_webview/glue) does nothing but let open() switch on favicon
 * downloads, which ATL's WebView always does (WebChromeClient.onReceivedIcon()). */
@Deprecated
public abstract class WebIconDatabase {
	@Deprecated
	public interface IconListener {
		public void onReceivedIcon(String url, Bitmap icon);
	}

	public abstract void open(String path);

	public abstract void close();

	public abstract void removeAllIcons();

	public abstract void requestIconForPageUrl(String url, IconListener listener);

	/** @hide */
	public abstract void bulkRequestIconForPageUrl(ContentResolver cr, String where, IconListener listener);

	public abstract void retainIconForPageUrl(String url);

	public abstract void releaseIconForPageUrl(String url);

	private static WebIconDatabase instance;

	public static WebIconDatabase getInstance() {
		synchronized (WebIconDatabase.class) {
			if (instance == null)
				instance = new Adapter();
			return instance;
		}
	}

	/* WebIconDatabaseAdapter: "big old no-op" */
	private static final class Adapter extends WebIconDatabase {
		public void open(String path) {}
		public void close() {}
		public void removeAllIcons() {}
		public void requestIconForPageUrl(String url, IconListener listener) {}
		public void bulkRequestIconForPageUrl(ContentResolver cr, String where, IconListener listener) {}
		public void retainIconForPageUrl(String url) {}
		public void releaseIconForPageUrl(String url) {}
	}
}
