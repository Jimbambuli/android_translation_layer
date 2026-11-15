package android.security;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;

/* AOSP keystore/java/android/security/KeyChain.java (android16-release). ATL has no KeyChainService and no
 * installed keys: the answers are those of a KeyChain whose store is empty (requestPrivateKey() and
 * getCertificate() of the service return null, the chooser has nothing to offer and reports no alias). The
 * argument checks and the main-thread check of bind() are kept. */
public final class KeyChain {
	public static final String LOG = "KeyChain";
	public static final String ACCOUNT_TYPE = "com.android.keychain";
	private static final String KEYCHAIN_PACKAGE = "com.android.keychain";
	private static final String ACTION_CHOOSER = "com.android.keychain.CHOOSER";
	private static final String CERT_INSTALLER_PACKAGE = "com.android.certinstaller";
	public static final String EXTRA_RESPONSE = "response";
	public static final String EXTRA_URI = "uri";
	public static final String EXTRA_ALIAS = "alias";
	public static final String EXTRA_SENDER = "sender";
	public static final String EXTRA_KEY_TYPES = "key_types";
	public static final String EXTRA_ISSUERS = "issuers";
	private static final String ACTION_INSTALL = "android.credentials.INSTALL";
	public static final String EXTRA_NAME = "name";
	public static final String EXTRA_CERTIFICATE = "CERT";
	public static final String EXTRA_PKCS12 = "PKCS12";
	public static final String ACTION_STORAGE_CHANGED = "android.security.STORAGE_CHANGED";
	public static final String ACTION_KEYCHAIN_CHANGED = "android.security.action.KEYCHAIN_CHANGED";
	public static final String ACTION_TRUST_STORE_CHANGED = "android.security.action.TRUST_STORE_CHANGED";
	public static final String ACTION_KEY_ACCESS_CHANGED = "android.security.action.KEY_ACCESS_CHANGED";
	public static final String EXTRA_KEY_ALIAS = "android.security.extra.KEY_ALIAS";
	public static final String EXTRA_KEY_ACCESSIBLE = "android.security.extra.KEY_ACCESSIBLE";

	public static Intent createInstallIntent() {
		Intent intent = new Intent(ACTION_INSTALL);
		intent.setClassName(CERT_INSTALLER_PACKAGE, "com.android.certinstaller.CertInstallerMain");
		return intent;
	}

	public static void choosePrivateKeyAlias(Activity activity, KeyChainAliasCallback response, String[] keyTypes,
	                                         Principal[] issuers, String host, int port, String alias) {
		Uri uri = null;
		if (host != null) {
			uri = new Uri.Builder()
			          .authority(host + (port != -1 ? ":" + port : ""))
			          .build();
		}
		choosePrivateKeyAlias(activity, response, keyTypes, issuers, uri, alias);
	}

	/* the chooser of an empty store: AliasResponse.alias(null), which AOSP calls on a binder thread */
	public static void choosePrivateKeyAlias(Activity activity, final KeyChainAliasCallback response, String[] keyTypes,
	                                         Principal[] issuers, Uri uri, String alias) {
		if (activity == null)
			throw new NullPointerException("activity == null");
		if (response == null)
			throw new NullPointerException("response == null");
		if (issuers != null) {
			for (Principal issuer : issuers) {
				if (!(issuer instanceof X500Principal))
					throw new IllegalArgumentException(String.format("Issuer %s is of type %s, not X500Principal",
					                                                 issuer.toString(), issuer.getClass()));
			}
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				response.alias(null);
			}
		}, "KeyChain").start();
	}

	public static PrivateKey getPrivateKey(Context context, String alias) throws KeyChainException, InterruptedException {
		if (alias == null)
			throw new NullPointerException("alias == null");
		if (context == null)
			throw new NullPointerException("context == null");
		ensureNotOnMainThread(context);
		return null;
	}

	public static X509Certificate[] getCertificateChain(Context context, String alias) throws KeyChainException, InterruptedException {
		if (alias == null)
			throw new NullPointerException("alias == null");
		ensureNotOnMainThread(context);
		return null;
	}

	/* AOSP KeyChain.isKeyAlgorithmSupported(): "RSA" and "EC" */
	public static boolean isKeyAlgorithmSupported(String algorithm) {
		final String algUpper = algorithm.toUpperCase(java.util.Locale.US);
		return "EC".equals(algUpper) || "RSA".equals(algUpper);
	}

	private static void ensureNotOnMainThread(Context context) {
		Looper looper = Looper.myLooper();
		if (looper != null && looper == context.getMainLooper())
			throw new IllegalStateException("calling this from your main thread can lead to deadlock");
	}
}
