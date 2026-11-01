package android.security.keystore;

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES/GCM/NoPadding for AndroidKeyStore keys, like AOSP's AndroidKeyStoreAuthenticatedAESCipherSpi.GCM.NoPadding:
 * encrypting without an IV uses a random 12-byte IV (DEFAULT_IV_LENGTH_BYTES) and a 128-bit tag, decrypting needs
 * the IV. AOSP lets keystore2 do the work; ATL has the key material and lets Bouncy Castle do it, with the IV
 * chosen here (Bouncy Castle's own default IV for GCM is a whole block, 16 bytes).
 */
public class AndroidKeyStoreAuthenticatedAESCipherSpi {

	public static class GCM extends CipherSpi {

		private static final int DEFAULT_IV_LENGTH_BYTES = 12;
		private static final int DEFAULT_TAG_LENGTH_BITS = 128;

		private Cipher cipher;

		private void init(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random)
		    throws InvalidKeyException, InvalidAlgorithmParameterException {
			// The provider is only chosen for AndroidKeyStore keys; a Cipher that picked it before init (e.g. through
			// getBlockSize()) may still get other raw AES keys, handled the same way.
			if (key == null || key.getEncoded() == null)
				throw new InvalidKeyException("Unsupported key: " + key);
			try {
				// the first provider that takes the plain key (not this one): AndroidOpenSSL, else Bouncy Castle
				cipher = Cipher.getInstance("AES/GCM/NoPadding");
			} catch (GeneralSecurityException e) {
				throw new ProviderException(e);
			}
			cipher.init(opmode, new SecretKeySpec(key.getEncoded(), "AES"), params, random);
		}

		@Override
		protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
			if (opmode != Cipher.ENCRYPT_MODE && opmode != Cipher.WRAP_MODE)
				throw new InvalidKeyException("IV required when decrypting. Use IvParameterSpec or AlgorithmParameters to provide it.");
			byte[] iv = new byte[DEFAULT_IV_LENGTH_BYTES];
			(random != null ? random : new SecureRandom()).nextBytes(iv);
			try {
				init(opmode, key, new GCMParameterSpec(DEFAULT_TAG_LENGTH_BITS, iv), random);
			} catch (InvalidAlgorithmParameterException e) {
				throw new InvalidKeyException(e);
			}
		}

		@Override
		protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random)
		    throws InvalidKeyException, InvalidAlgorithmParameterException {
			if (params == null) {
				engineInit(opmode, key, random);
				return;
			}
			init(opmode, key, params, random);
		}

		@Override
		protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random)
		    throws InvalidKeyException, InvalidAlgorithmParameterException {
			if (params == null) {
				engineInit(opmode, key, random);
				return;
			}
			try {
				init(opmode, key, params.getParameterSpec(GCMParameterSpec.class), random);
			} catch (InvalidParameterSpecException e) {
				throw new InvalidAlgorithmParameterException(e);
			}
		}

		@Override
		protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
			if (!"GCM".equalsIgnoreCase(mode))
				throw new NoSuchAlgorithmException("Unsupported block mode: " + mode);
		}

		@Override
		protected void engineSetPadding(String padding) throws NoSuchPaddingException {
			if (!"NoPadding".equalsIgnoreCase(padding))
				throw new NoSuchPaddingException("Unsupported padding scheme: " + padding);
		}

		@Override
		protected int engineGetBlockSize() {
			return 16;
		}

		@Override
		protected int engineGetKeySize(Key key) {
			return key.getEncoded().length * 8;
		}

		@Override
		protected int engineGetOutputSize(int inputLen) {
			return cipher.getOutputSize(inputLen);
		}

		@Override
		protected byte[] engineGetIV() {
			return cipher != null ? cipher.getIV() : null;
		}

		@Override
		protected AlgorithmParameters engineGetParameters() {
			return cipher != null ? cipher.getParameters() : null;
		}

		@Override
		protected void engineUpdateAAD(byte[] src, int offset, int len) {
			cipher.updateAAD(src, offset, len);
		}

		@Override
		protected void engineUpdateAAD(ByteBuffer src) {
			cipher.updateAAD(src);
		}

		@Override
		protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
			return cipher.update(input, inputOffset, inputLen);
		}

		@Override
		protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset)
		    throws ShortBufferException {
			return cipher.update(input, inputOffset, inputLen, output, outputOffset);
		}

		@Override
		protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen)
		    throws IllegalBlockSizeException, BadPaddingException {
			return cipher.doFinal(input, inputOffset, inputLen);
		}

		@Override
		protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset)
		    throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
			return cipher.doFinal(input, inputOffset, inputLen, output, outputOffset);
		}
	}
}
