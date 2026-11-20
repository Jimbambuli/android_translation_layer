package android.security.keystore;

import java.math.BigInteger;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;

/** RSA private key held by AndroidKeyStore, like android.security.keystore2.AndroidKeyStoreRSAPrivateKey */
public class AndroidKeyStoreRSAPrivateKey extends AndroidKeyStorePrivateKey implements RSAKey {

	AndroidKeyStoreRSAPrivateKey(RSAPrivateKey key) {
		super(key);
	}

	@Override
	public BigInteger getModulus() {
		return ((RSAPrivateKey)key).getModulus();
	}
}
