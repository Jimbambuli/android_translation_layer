package android.atl.crypto;

import java.security.SecureRandomSpi;

/**
 * SecureRandom from the kernel (getrandom()), like Conscrypt's OpenSSLRandom, which Android uses for SHA1PRNG and
 * new SecureRandom(). A seed is ignored, as there: the output never becomes predictable.
 */
public final class OpenSSLRandom extends SecureRandomSpi {
	private static final long serialVersionUID = 8506210602917522861L;

	@Override
	protected void engineSetSeed(byte[] seed) {
		if (seed == null)
			throw new NullPointerException("seed == null");
	}

	@Override
	protected void engineNextBytes(byte[] bytes) {
		NativeCrypto.getrandom(bytes);
	}

	@Override
	protected byte[] engineGenerateSeed(int numBytes) {
		byte[] output = new byte[numBytes];
		NativeCrypto.getrandom(output);
		return output;
	}
}
