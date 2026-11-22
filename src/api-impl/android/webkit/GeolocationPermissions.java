package android.webkit;

import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/* AOSP frameworks/base core/java/android/webkit/GeolocationPermissions.java, whose getInstance() is the provider's
 * store: Chromium's GeolocationPermissionsAdapter over AwGeolocationPermissions, the decisions an app retained
 * (GeolocationPermissions.Callback.invoke(origin, allow, true)), kept in SharedPreferences under
 * "AwGeolocationPermissions%" + origin ("scheme://host[:port]/", GURLUtils.getOrigin()). The preferences file is
 * ATL's own (WebViewGeolocationPermissions). Callbacks are posted to the main looper, as AwThreadUtils does. */
public class GeolocationPermissions {
	private static final String PREF_PREFIX = "AwGeolocationPermissions%";

	public interface Callback {
		void invoke(String origin, boolean allow, boolean retain);
	}

	private static GeolocationPermissions instance;

	public static GeolocationPermissions getInstance() {
		synchronized (GeolocationPermissions.class) {
			if (instance == null)
				instance = new GeolocationPermissions();
			return instance;
		}
	}

	public GeolocationPermissions() {}

	private static SharedPreferences prefs() {
		return android.app.ActivityThread.currentApplication().getSharedPreferences("WebViewGeolocationPermissions", 0);
	}

	/* GURLUtils.getOrigin(): "scheme://host[:port]/", empty if the URL has none */
	private static String getOriginKey(String url) {
		if (url == null)
			return null;
		android.net.Uri uri = android.net.Uri.parse(url);
		String scheme = uri.getScheme(), host = uri.getHost();
		if (scheme == null || host == null || host.isEmpty())
			return null;
		scheme = scheme.toLowerCase(java.util.Locale.ROOT);
		int port = uri.getPort();
		boolean defaultPort = port == -1 || (port == 80 && scheme.equals("http")) || (port == 443 && scheme.equals("https"));
		return PREF_PREFIX + scheme + "://" + host.toLowerCase(java.util.Locale.ROOT) + (defaultPort ? "" : ":" + port) + "/";
	}

	private static void post(final ValueCallback callback, final Object value) {
		if (callback == null)
			return;
		new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				callback.onReceiveValue(value);
			}
		});
	}

	public void getOrigins(ValueCallback<Set<String>> callback) {
		Set<String> origins = new HashSet<String>();
		for (String name : prefs().getAll().keySet()) {
			if (name.startsWith(PREF_PREFIX))
				origins.add(name.substring(PREF_PREFIX.length()));
		}
		post(callback, origins);
	}

	public void getAllowed(String origin, ValueCallback<Boolean> callback) {
		String key = getOriginKey(origin);
		post(callback, key != null && prefs().getBoolean(key, false));
	}

	public void clear(String origin) {
		String key = getOriginKey(origin);
		if (key != null)
			prefs().edit().remove(key).apply();
	}

	public void allow(String origin) {
		String key = getOriginKey(origin);
		if (key != null)
			prefs().edit().putBoolean(key, true).apply();
	}

	/* AwGeolocationPermissions.deny(): a retained "no" */
	void deny(String origin) {
		String key = getOriginKey(origin);
		if (key != null)
			prefs().edit().putBoolean(key, false).apply();
	}

	/* hasOrigin() ? isOriginAllowed() : null */
	Boolean retained(String origin) {
		String key = getOriginKey(origin);
		if (key == null || !prefs().contains(key))
			return null;
		return prefs().getBoolean(key, false);
	}

	public void clearAll() {
		SharedPreferences.Editor editor = null;
		for (String name : prefs().getAll().keySet()) {
			if (name.startsWith(PREF_PREFIX)) {
				if (editor == null)
					editor = prefs().edit();
				editor.remove(name);
			}
		}
		if (editor != null)
			editor.apply();
	}
}
