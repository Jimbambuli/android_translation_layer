package android.security.keystore;

import java.security.interfaces.ECKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECParameterSpec;

/** EC private key held by AndroidKeyStore, like android.security.keystore2.AndroidKeyStoreECPrivateKey */
public class AndroidKeyStoreECPrivateKey extends AndroidKeyStorePrivateKey implements ECKey {

	AndroidKeyStoreECPrivateKey(ECPrivateKey key) {
		super(key);
	}

	@Override
	public ECParameterSpec getParams() {
		return ((ECPrivateKey)key).getParams();
	}
}
