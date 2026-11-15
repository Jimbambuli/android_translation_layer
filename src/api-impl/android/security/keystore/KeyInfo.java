package android.security.keystore;

import java.security.spec.KeySpec;

/* AOSP keystore/java/android/security/keystore/KeyInfo.java: metadata of a key. ATL keeps keys in software
 * (AndroidKeyStore.engineSetKeyEntry), so a key is never inside secure hardware. */
public class KeyInfo implements KeySpec {
	private final String mKeystoreAlias;
	private final int mKeySize;

	public KeyInfo(String keystoreAlias, int keySize) {
		mKeystoreAlias = keystoreAlias;
		mKeySize = keySize;
	}

	public String getKeystoreAlias() {
		return mKeystoreAlias;
	}

	public int getKeySize() {
		return mKeySize;
	}

	public boolean isInsideSecureHardware() {
		return false;
	}
}
