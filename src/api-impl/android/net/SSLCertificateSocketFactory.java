package android.net;

public class SSLCertificateSocketFactory {
	private final int mHandshakeTimeoutMillis;

	/* AOSP SSLCertificateSocketFactory(int handshakeTimeoutMillis) */
	public SSLCertificateSocketFactory(int handshakeTimeoutMillis) {
		mHandshakeTimeoutMillis = handshakeTimeoutMillis;
	}
}
