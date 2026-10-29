package android.security.keystore;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.KeyGeneratorSpi;
import javax.crypto.SecretKey;

public abstract class KeyGenerator extends KeyGeneratorSpi {

	protected javax.crypto.KeyGenerator keyGenerator;
	protected AlgorithmParameterSpec params;

	public static class AES extends KeyGenerator {
		@Override
		protected void engineInit(AlgorithmParameterSpec params, SecureRandom random)
		    throws InvalidAlgorithmParameterException {
			try {
				keyGenerator = javax.crypto.KeyGenerator.getInstance("AES", "BC");
				this.params = params;
				keyGenerator.init(keySize(params, 128), random);
			} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
				e.printStackTrace();
				throw new UnsupportedOperationException("Unimplemented method 'engineInit'");
			}
		}
	}

	public static class HmacSHA512 extends KeyGenerator {
		@Override
		protected void engineInit(AlgorithmParameterSpec params, SecureRandom random)
		    throws InvalidAlgorithmParameterException {
			try {
				keyGenerator = javax.crypto.KeyGenerator.getInstance("HmacSHA512", "BC");
				this.params = params;
				keyGenerator.init(keySize(params, 512), random);
			} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
				e.printStackTrace();
				throw new UnsupportedOperationException("Unimplemented method 'engineInit'");
			}
		}
	}

	/* like AndroidKeyStoreKeyGeneratorSpi.engineInit: the spec's key size, or the algorithm's default
	 * (AES 128 bits, HMAC the digest's output size) */
	private static int keySize(AlgorithmParameterSpec params, int defaultKeySizeBits) {
		int keySize = ((KeyGenParameterSpec)params).getKeySize();
		return keySize > 0 ? keySize : defaultKeySizeBits;
	}

	@Override
	protected SecretKey engineGenerateKey() {
		System.out.println("generating key with alias " + ((KeyGenParameterSpec)params).getKeystoreAlias());
		SecretKey key = keyGenerator.generateKey();
		try {
			new AndroidKeyStore().engineSetKeyEntry(((KeyGenParameterSpec)params).getKeystoreAlias(), key, null, null);
		} catch (KeyStoreException e) {
			throw new ProviderException("Keystore key generation failed", e);
		}
		return new AndroidKeyStoreSecretKey(key.getEncoded(), key.getAlgorithm());
	}

	@Override
	protected void engineInit(SecureRandom random) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'engineInit'");
	}

	@Override
	protected void engineInit(int keysize, SecureRandom random) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'engineInit'");
	}
}
