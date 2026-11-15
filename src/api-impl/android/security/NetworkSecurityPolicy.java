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

	/* libcore NetworkSecurityPolicy domain encryption (ECH) modes. */
	public static final int DOMAIN_ENCRYPTION_SETTING_UNKNOWN = 0;
	public static final int DOMAIN_ENCRYPTION_DISABLED = 1;
	public static final int DOMAIN_ENCRYPTION_OPPORTUNISTIC = 2;
	public static final int DOMAIN_ENCRYPTION_STRICT = 3;
	public static final int DOMAIN_ENCRYPTION_FAIL_CLOSED = 4;

	/* libcore.net.NetworkSecurityPolicy.getDomainEncryptionMode(): the default, as ATL has no network security
	 * config, is SETTING_UNKNOWN (plain TLS handshake). */
	public int getDomainEncryptionMode(String hostname) {
		return DOMAIN_ENCRYPTION_SETTING_UNKNOWN;
	}
}
