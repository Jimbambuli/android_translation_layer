package android.webkit;

import android.atl.ATLLoadedApp;
import android.content.Context;
import android.webkit.WebView;

public class CookieManager {
	/* AwCookieManager / AwSettings: cookies are accepted, third-party cookies only where a WebView allows them
	 * (default off for apps since Android 5). WebKit has one policy for all views: third-party cookies are
	 * accepted while one WebView allows them. */
	private static boolean acceptCookie = true;
	private static final java.util.Map<WebView, Boolean> acceptThirdParty = new java.util.WeakHashMap<>();


	public static CookieManager getInstance() {
		// HACK: disable NewPipe's WebView based PoToken generator for now
		if (ATLLoadedApp.getPrimaryApplication().pkg.packageName.equals("org.schabi.newpipe")) {
			throw new RuntimeException("CookieManager not yet fully implemented");
		}
		try { // also handle NewPipe forks which can have a different packagename
			ATLLoadedApp.getPrimaryApplication().loadClass(
			    "org.schabi.newpipe.util.potoken.PoTokenWebView");
			throw new RuntimeException("CookieManager not yet fully implemented");
		} catch (ClassNotFoundException e) {
		}
		return new CookieManager();
	}

	public void removeAllCookies(ValueCallback<Boolean> callback) {
		native_removeAllCookies();
		if (callback != null)
			callback.onReceiveValue(true);
	}

	public void removeSessionCookies(ValueCallback<Boolean> callback) {
		if (callback != null)
			callback.onReceiveValue(true);
	}

	public void removeExpiredCookie() {}

	public void removeAllCookie() {
		native_removeAllCookies();
	}

	public void removeSessionCookie() {}

	public void flush() {}

	public String getCookie(String url) {
		return native_getCookie(url);
	}

	/* AOSP CookieManager.setCookie(): value is a Set-Cookie header, an invalid one is ignored */
	public void setCookie(String url, String value) {
		native_setCookie(url, value);
	}

	public void setCookie(String url, String value, ValueCallback<Boolean> callback) {
		boolean set = native_setCookie(url, value);
		if (callback != null)
			callback.onReceiveValue(set);
	}

	public void setAcceptCookie(boolean accept) {
		synchronized (CookieManager.class) {
			acceptCookie = accept;
			updateAcceptPolicy();
		}
	}

	public boolean acceptCookie() {
		synchronized (CookieManager.class) {
			return acceptCookie;
		}
	}

	public boolean acceptThirdPartyCookies(WebView webview) {
		synchronized (CookieManager.class) {
			Boolean accept = acceptThirdParty.get(webview);
			return accept != null && accept;
		}
	}

	public void setAcceptThirdPartyCookies(WebView webView, boolean accept) {
		synchronized (CookieManager.class) {
			acceptThirdParty.put(webView, accept);
			updateAcceptPolicy();
		}
	}

	private static void updateAcceptPolicy() {
		/* WebKitCookieAcceptPolicy: ALWAYS 0, NEVER 1, NO_THIRD_PARTY 2 */
		native_setAcceptPolicy(!acceptCookie ? 1 : acceptThirdParty.containsValue(Boolean.TRUE) ? 0 : 2);
	}

	public static void setAcceptFileSchemeCookies(boolean accept) {}

	private native String native_getCookie(String url);
	private static native boolean native_setCookie(String url, String value);
	private static native void native_removeAllCookies();
	private static native void native_setAcceptPolicy(int policy);
}
