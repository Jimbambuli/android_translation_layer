package android.security;

/* API 24. The network security policy decides whether cleartext (non-TLS) traffic is allowed.
 * ATL parses no network security config, so, like a debuggable app without one, it permits
 * cleartext for every host. OkHttp and the AndroidX/Volley stacks query this early on the
 * network start path, so the class has to exist and answer. */
public class NetworkSecurityPolicy {

	private static final NetworkSecurityPolicy INSTANCE = new NetworkSecurityPolicy();

	public static NetworkSecurityPolicy getInstance() {
		return INSTANCE;
	}

	public boolean isCleartextTrafficPermitted() {
		return true;
	}

	public boolean isCleartextTrafficPermitted(String hostname) {
		return true;
	}
}
