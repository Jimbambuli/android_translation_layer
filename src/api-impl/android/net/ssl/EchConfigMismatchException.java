package android.net.ssl;

import javax.net.ssl.SSLException;

/* Conscrypt EchConfigMismatchException (org.conscrypt, repackaged into android.net.ssl by the platform build):
 * thrown when the server rejects the ECH config the client offered. */
public class EchConfigMismatchException extends SSLException {
	private final String publicName;
	private final EchConfigList echRetryConfigList;

	/* The EchConfigList the server provided for retrying the connection, or null if the server set none.
	 * The client must verify that the certificate is valid for getPublicHostname() before reading this. */
	public EchConfigList getRetryConfigList() {
		if (publicName == null) {
			return null;
		}
		return echRetryConfigList;
	}

	/* The hostname that should be used for verification (draft-ietf-tls-esni-25, 6.1.7 "Authenticating for the
	 * Public Name"); must be called before interpreting getRetryConfigList(). */
	public String getPublicHostname() {
		return publicName;
	}

	/* True if a retry EchConfigList is available. */
	public boolean hasRetryConfigList() {
		return echRetryConfigList != null;
	}

	public EchConfigMismatchException(String message) {
		super(message);
		this.publicName = null;
		this.echRetryConfigList = null;
	}

	public EchConfigMismatchException(String message, String publicName, EchConfigList echRetryConfigList) {
		super(message);
		this.publicName = publicName;
		this.echRetryConfigList = echRetryConfigList;
	}
}
