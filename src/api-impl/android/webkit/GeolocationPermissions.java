package android.webkit;

import java.util.Set;

/* The framework class of AOSP frameworks/base core/java/android/webkit/GeolocationPermissions.java: its methods
 * are no-ops there (the permission store lives in the Chromium subclass, which is not part of ATL) */
public class GeolocationPermissions {
	public interface Callback {
		void invoke(String origin, boolean allow, boolean retain);
	}

	public static GeolocationPermissions getInstance() {
		return new GeolocationPermissions();
	}

	public void getOrigins(ValueCallback<Set<String>> callback) {}

	public void getAllowed(String origin, ValueCallback<Boolean> callback) {}

	public void clear(String origin) {}

	public void allow(String origin) {}

	public void clearAll() {}

	public GeolocationPermissions() {}
}
