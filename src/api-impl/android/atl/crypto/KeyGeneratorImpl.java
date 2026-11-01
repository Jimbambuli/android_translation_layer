package android.atl.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.KeyGeneratorSpi;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Random secret keys, like Conscrypt's KeyGeneratorImpl.
 */
public abstract class KeyGeneratorImpl extends KeyGeneratorSpi {
	private final String algorithm;
	private SecureRandom secureRandom;
	private int keySizeBits;

	private KeyGeneratorImpl(String algorithm, int defaultKeySizeBits) {
		this.algorithm = algorithm;
		this.keySizeBits = defaultKeySizeBits;
	}

	void checkKeySize(int keySize) {
		if (keySize <= 0)
			throw new InvalidParameterException("Key size must be positive");
	}

	@Override
	protected void engineInit(SecureRandom secureRandom) {
		this.secureRandom = secureRandom;
	}

	@Override
	protected void engineInit(AlgorithmParameterSpec params, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
		if (params == null)
			throw new InvalidAlgorithmParameterException("No params provided");
		throw new InvalidAlgorithmParameterException("Unknown param type: " + params.getClass().getName());
	}

	@Override
	protected void engineInit(int keySize, SecureRandom secureRandom) {
		checkKeySize(keySize);
		this.keySizeBits = keySize;
		this.secureRandom = secureRandom;
	}

	@Override
	protected SecretKey engineGenerateKey() {
		byte[] keyData = new byte[(keySizeBits + 7) / 8];
		if (secureRandom != null)
			secureRandom.nextBytes(keyData);
		else
			NativeCrypto.getrandom(keyData);
		return new SecretKeySpec(keyData, algorithm);
	}

	public static final class HmacMD5 extends KeyGeneratorImpl {
		public HmacMD5() { super("HmacMD5", 128); }
	}

	public static final class HmacSHA1 extends KeyGeneratorImpl {
		public HmacSHA1() { super("HmacSHA1", 160); }
	}

	public static final class HmacSHA224 extends KeyGeneratorImpl {
		public HmacSHA224() { super("HmacSHA224", 224); }
	}

	public static final class HmacSHA256 extends KeyGeneratorImpl {
		public HmacSHA256() { super("HmacSHA256", 256); }
	}

	public static final class HmacSHA384 extends KeyGeneratorImpl {
		public HmacSHA384() { super("HmacSHA384", 384); }
	}

	public static final class HmacSHA512 extends KeyGeneratorImpl {
		public HmacSHA512() { super("HmacSHA512", 512); }
	}

	public static final class AES extends KeyGeneratorImpl {
		public AES() { super("AES", 128); }

		@Override
		void checkKeySize(int keySize) {
			if (keySize != 128 && keySize != 192 && keySize != 256)
				throw new InvalidParameterException("Key size must be either 128, 192, or 256 bits");
		}
	}

	public static final class ChaCha20 extends KeyGeneratorImpl {
		public ChaCha20() { super("ChaCha20", 256); }

		@Override
		void checkKeySize(int keySize) {
			if (keySize != 256)
				throw new InvalidParameterException("Key size must be 256 bits");
		}
	}
}
