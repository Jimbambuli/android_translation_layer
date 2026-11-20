package android.security.keystore;

import java.security.PrivateKey;

/**
 * Private key held by AndroidKeyStore, like android.security.keystore2.AndroidKeyStorePrivateKey: it does not export
 * its key material (getEncoded() and getFormat() return null). AOSP's key is a handle to keystore2; ATL keeps the
 * software key inside, for AndroidKeyStoreBCWorkaroundProvider's ciphers (SupportedKeyClasses) to use.
 */
public class AndroidKeyStorePrivateKey implements PrivateKey {

	final PrivateKey key;

	AndroidKeyStorePrivateKey(PrivateKey key) {
		this.key = key;
	}

	@Override
	public String getAlgorithm() {
		return key.getAlgorithm();
	}

	@Override
	public String getFormat() {
		return null;
	}

	@Override
	public byte[] getEncoded() {
		return null;
	}
}
