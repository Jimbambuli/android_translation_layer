package android.webkit;

import android.annotation.Nullable;
import android.content.Context;
import java.util.HashMap;
import java.util.Map;

/* AOSP: getInstance() = WebViewFactory.getProvider().getWebViewDatabase(context). ATL has no WebView provider;
 * the HTTP-auth credentials are kept in memory for the process (Chromium's WebViewDatabase stores them in its
 * own database file, which ATL does not have), the deprecated username/password and form data stores are empty. */
public abstract class WebViewDatabase {
	protected static final String LOGTAG = "webviewdatabase";

	private static WebViewDatabase instance;

	@Deprecated
	public WebViewDatabase() {}

	public static synchronized WebViewDatabase getInstance(Context context) {
		if (instance == null) {
			instance = new WebViewDatabase() {
				private final Map<String, String[]> httpAuth = new HashMap<>();

				@Override
				public boolean hasUsernamePassword() {
					return false;
				}

				@Override
				public void clearUsernamePassword() {}

				@Override
				public synchronized boolean hasHttpAuthUsernamePassword() {
					return !httpAuth.isEmpty();
				}

				@Override
				public synchronized void clearHttpAuthUsernamePassword() {
					httpAuth.clear();
				}

				@Override
				public synchronized void setHttpAuthUsernamePassword(String host, String realm, String username, String password) {
					if (host == null || realm == null)
						return;
					httpAuth.put(host + '\0' + realm, new String[] {username, password});
				}

				@Override
				@Nullable
				public synchronized String[] getHttpAuthUsernamePassword(String host, String realm) {
					if (host == null || realm == null)
						return null;
					String[] up = httpAuth.get(host + '\0' + realm);
					return up == null ? null : up.clone();
				}

				@Override
				public boolean hasFormData() {
					return false;
				}

				@Override
				public void clearFormData() {}
			};
		}
		return instance;
	}

	@Deprecated
	public abstract boolean hasUsernamePassword();

	@Deprecated
	public abstract void clearUsernamePassword();

	public abstract boolean hasHttpAuthUsernamePassword();

	public abstract void clearHttpAuthUsernamePassword();

	public abstract void setHttpAuthUsernamePassword(String host, String realm, String username, String password);

	@Nullable
	public abstract String[] getHttpAuthUsernamePassword(String host, String realm);

	@Deprecated
	public abstract boolean hasFormData();

	@Deprecated
	public abstract void clearFormData();
}
