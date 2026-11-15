package android.net.http;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.X509TrustManager;

public class X509TrustManagerExtensions {

	private final X509TrustManager mTrustManager;

	public X509TrustManagerExtensions(X509TrustManager tm) {
		mTrustManager = tm;
	}

	public List<X509Certificate> checkServerTrusted(X509Certificate[] chain,
	                                                String authType, String host) {
		return Arrays.asList(chain);
	}

	/* AOSP X509TrustManagerExtensions.checkServerTrusted(chain, ocspData, tlsSctData, authType, host), duck-typed
	 * branch: ATL has no TrustManagerImpl, the trust manager must offer the same method itself */
	public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, byte[] ocspData, byte[] tlsSctData,
	                                                String authType, String host) throws CertificateException {
		Method m;
		try {
			m = mTrustManager.getClass().getMethod("checkServerTrusted", X509Certificate[].class, byte[].class,
			                                       byte[].class, String.class, String.class);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Required method"
			                                   + " checkServerTrusted(X509Certificate[], byte[], byte[], String, String)"
			                                   + " missing");
		}
		try {
			@SuppressWarnings("unchecked")
			List<X509Certificate> result =
			    (List<X509Certificate>) m.invoke(mTrustManager, chain, ocspData, tlsSctData, authType, host);
			return result == null ? Collections.<X509Certificate>emptyList() : result;
		} catch (IllegalAccessException e) {
			throw new CertificateException("Failed to call checkServerTrusted", e);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof CertificateException) {
				throw (CertificateException) e.getCause();
			}
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}
			throw new CertificateException("checkServerTrusted failed", e.getCause());
		}
	}
}
