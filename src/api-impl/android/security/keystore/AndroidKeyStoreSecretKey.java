package android.security.keystore;

import javax.crypto.spec.SecretKeySpec;

/**
 * Secret key held by AndroidKeyStore, like android.security.keystore2.AndroidKeyStoreSecretKey. AOSP's key is only a
 * handle to keystore2; ATL holds the key material, so the key stays encodable and providers such as Bouncy Castle
 * can still use it (e.g. for HMAC). Its class lets AndroidKeyStoreBCWorkaroundProvider pick it for the ciphers that
 * must behave as on Android (SupportedKeyClasses).
 */
public class AndroidKeyStoreSecretKey extends SecretKeySpec {

	public AndroidKeyStoreSecretKey(byte[] key, String algorithm) {
		super(key, algorithm);
	}
}
