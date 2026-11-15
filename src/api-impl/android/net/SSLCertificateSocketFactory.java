package android.net;

import android.util.Log;
import com.android.org.conscrypt.OpenSSLSocketImpl;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.SocketFactory;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/* AOSP frameworks/base core/java/android/net/SSLCertificateSocketFactory.java (Android 16): an
 * SSLSocketFactory that delegates to an SSLContext socket factory with the given key/trust managers
 * (insecure factories trust every certificate) and verifies the host name of the sockets it connects.
 * Differences, because ATL's JSSE is not AOSP's Conscrypt: the context is SSLContext.getInstance("TLS")
 * without naming the "AndroidOpenSSL" provider; the Conscrypt-only socket settings (NPN/ALPN lists,
 * handshake timeout, channel ID key, persistent session cache) are kept but not applied to the sockets;
 * socket.relaxsslcheck is off, as on a non-debuggable device. getHttpSocketFactory() is left out (ATL has
 * no org.apache.http.conn.ssl). */
@Deprecated
public class SSLCertificateSocketFactory extends SSLSocketFactory {
	private static final String TAG = "SSLCertificateSocketFactory";

	private static final TrustManager[] INSECURE_TRUST_MANAGER = new TrustManager[] {
		new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() { return null; }
			public void checkClientTrusted(X509Certificate[] certs, String authType) {}
			public void checkServerTrusted(X509Certificate[] certs, String authType) {}
		}
	};

	private SSLSocketFactory mInsecureFactory = null;
	private SSLSocketFactory mSecureFactory = null;
	private TrustManager[] mTrustManagers = null;
	private KeyManager[] mKeyManagers = null;
	private byte[] mNpnProtocols = null;
	private byte[] mAlpnProtocols = null;
	private PrivateKey mChannelIdPrivateKey = null;

	private final int mHandshakeTimeoutMillis;
	private final SSLSessionCache mSessionCache;
	private final boolean mSecure;

	/* AOSP SSLCertificateSocketFactory(int handshakeTimeoutMillis) */
	@Deprecated
	public SSLCertificateSocketFactory(int handshakeTimeoutMillis) {
		this(handshakeTimeoutMillis, null, true);
	}

	private SSLCertificateSocketFactory(int handshakeTimeoutMillis, SSLSessionCache cache, boolean secure) {
		mHandshakeTimeoutMillis = handshakeTimeoutMillis;
		mSessionCache = cache;
		mSecure = secure;
	}

	public static SocketFactory getDefault(int handshakeTimeoutMillis) {
		return new SSLCertificateSocketFactory(handshakeTimeoutMillis, null, true);
	}

	public static SSLSocketFactory getDefault(int handshakeTimeoutMillis, SSLSessionCache cache) {
		return new SSLCertificateSocketFactory(handshakeTimeoutMillis, cache, true);
	}

	public static SSLSocketFactory getInsecure(int handshakeTimeoutMillis, SSLSessionCache cache) {
		return new SSLCertificateSocketFactory(handshakeTimeoutMillis, cache, false);
	}

	/** @hide */
	public static void verifyHostname(Socket socket, String hostname) throws IOException {
		if (!(socket instanceof SSLSocket)) {
			throw new IllegalArgumentException("Attempt to verify non-SSL socket");
		}

		SSLSocket ssl = (SSLSocket)socket;
		ssl.startHandshake();

		SSLSession session = ssl.getSession();
		if (session == null) {
			throw new SSLException("Cannot verify SSL socket without session");
		}
		if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)) {
			throw new SSLPeerUnverifiedException("Cannot verify hostname: " + hostname);
		}
	}

	private SSLSocketFactory makeSocketFactory(KeyManager[] keyManagers, TrustManager[] trustManagers) {
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagers, trustManagers, null);
			return sslContext.getSocketFactory();
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			Log.wtf(TAG, e);
			return (SSLSocketFactory)SSLSocketFactory.getDefault(); // Fallback
		}
	}

	private synchronized SSLSocketFactory getDelegate() {
		if (!mSecure) {
			if (mInsecureFactory == null) {
				Log.w(TAG, "Bypassing SSL security checks at caller's request");
				mInsecureFactory = makeSocketFactory(mKeyManagers, INSECURE_TRUST_MANAGER);
			}
			return mInsecureFactory;
		} else {
			if (mSecureFactory == null) {
				mSecureFactory = makeSocketFactory(mKeyManagers, mTrustManagers);
			}
			return mSecureFactory;
		}
	}

	public void setTrustManagers(TrustManager[] trustManager) {
		mTrustManagers = trustManager;

		// Clear out all cached secure factories since configurations have changed.
		mSecureFactory = null;
		// Note - insecure factories only ever use the INSECURE_TRUST_MANAGER so they need not
		// be cleared out here.
	}

	public void setNpnProtocols(byte[][] npnProtocols) {
		this.mNpnProtocols = toLengthPrefixedList(npnProtocols);
	}

	/** @hide */
	public void setAlpnProtocols(byte[][] protocols) {
		this.mAlpnProtocols = toLengthPrefixedList(protocols);
	}

	/** @hide */
	public static byte[] toLengthPrefixedList(byte[]... items) {
		if (items.length == 0) {
			throw new IllegalArgumentException("items.length == 0");
		}
		int totalLength = 0;
		for (byte[] s : items) {
			if (s.length == 0 || s.length > 255) {
				throw new IllegalArgumentException("s.length == 0 || s.length > 255: " + s.length);
			}
			totalLength += 1 + s.length;
		}
		byte[] result = new byte[totalLength];
		int pos = 0;
		for (byte[] s : items) {
			result[pos++] = (byte)s.length;
			for (byte b : s) {
				result[pos++] = b;
			}
		}
		return result;
	}

	/* AOSP: OpenSSLSocketImpl.getNpnSelectedProtocol(); Conscrypt no longer negotiates NPN and returns null */
	public byte[] getNpnSelectedProtocol(Socket socket) {
		castToOpenSSLSocket(socket);
		return null;
	}

	/** @hide */
	public byte[] getAlpnSelectedProtocol(Socket socket) {
		return castToOpenSSLSocket(socket).getAlpnSelectedProtocol();
	}

	public void setKeyManagers(KeyManager[] keyManagers) {
		mKeyManagers = keyManagers;

		// Clear out any existing cached factories since configurations have changed.
		mSecureFactory = null;
		mInsecureFactory = null;
	}

	/** @hide */
	public void setChannelIdPrivateKey(PrivateKey privateKey) {
		mChannelIdPrivateKey = privateKey;
	}

	public void setUseSessionTickets(Socket socket, boolean useSessionTickets) {
		castToOpenSSLSocket(socket).setUseSessionTickets(useSessionTickets);
	}

	public void setHostname(Socket socket, String hostName) {
		castToOpenSSLSocket(socket).setHostname(hostName);
	}

	/** @hide */
	public void setSoWriteTimeout(Socket socket, int writeTimeoutMilliseconds) throws SocketException {
		castToOpenSSLSocket(socket);
	}

	private static OpenSSLSocketImpl castToOpenSSLSocket(Socket socket) {
		if (!(socket instanceof OpenSSLSocketImpl)) {
			throw new IllegalArgumentException("Socket not created by this factory: " + socket);
		}

		return (OpenSSLSocketImpl)socket;
	}

	@Override
	public Socket createSocket(Socket k, String host, int port, boolean close) throws IOException {
		Socket s = getDelegate().createSocket(k, host, port, close);
		if (mSecure) {
			verifyHostname(s, host);
		}
		return s;
	}

	@Override
	public Socket createSocket() throws IOException {
		return getDelegate().createSocket();
	}

	@Override
	public Socket createSocket(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
		return getDelegate().createSocket(addr, port, localAddr, localPort);
	}

	@Override
	public Socket createSocket(InetAddress addr, int port) throws IOException {
		return getDelegate().createSocket(addr, port);
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
		Socket s = getDelegate().createSocket(host, port, localAddr, localPort);
		if (mSecure) {
			verifyHostname(s, host);
		}
		return s;
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException {
		Socket s = getDelegate().createSocket(host, port);
		if (mSecure) {
			verifyHostname(s, host);
		}
		return s;
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return getDelegate().getDefaultCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return getDelegate().getSupportedCipherSuites();
	}
}
