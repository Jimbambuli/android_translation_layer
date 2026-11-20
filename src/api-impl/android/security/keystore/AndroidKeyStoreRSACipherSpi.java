package android.security.keystore;

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * RSA ciphers of AndroidKeyStoreBCWorkaroundProvider, like AOSP's android.security.keystore2.AndroidKeyStoreRSACipherSpi
 * with AndroidKeyStoreCipherSpiBase: a key that is not an AndroidKeyStore private key (e.g. the public key from the
 * entry's certificate) is diverted to the default provider's cipher of the same transformation; an AndroidKeyStore
 * private key is used as keystore2 uses it. AOSP lets keystore2 do that work; ATL has the key material and lets the
 * default provider do it with the parameters keystore2 would use (OAEP: MGF1 with SHA-1 unless the spec says else).
 */
public abstract class AndroidKeyStoreRSACipherSpi extends CipherSpi {

	private static final String DEFAULT_MGF1_DIGEST = "SHA-1";

	private final String transformation;
	private final String oaepDigest; // null: no OAEP padding
	private Cipher cipher;

	AndroidKeyStoreRSACipherSpi(String transformation, String oaepDigest) {
		this.transformation = transformation;
		this.oaepDigest = oaepDigest;
	}

	public static final class NoPadding extends AndroidKeyStoreRSACipherSpi {
		public NoPadding() { super("RSA/ECB/NoPadding", null); }
	}

	public static final class PKCS1Padding extends AndroidKeyStoreRSACipherSpi {
		public PKCS1Padding() { super("RSA/ECB/PKCS1Padding", null); }
	}

	public static final class OAEPWithSHA1AndMGF1Padding extends AndroidKeyStoreRSACipherSpi {
		public OAEPWithSHA1AndMGF1Padding() { super("RSA/ECB/OAEPPadding", "SHA-1"); }
	}

	public static final class OAEPWithSHA224AndMGF1Padding extends AndroidKeyStoreRSACipherSpi {
		public OAEPWithSHA224AndMGF1Padding() { super("RSA/ECB/OAEPWithSHA-224AndMGF1Padding", "SHA-224"); }
	}

	public static final class OAEPWithSHA256AndMGF1Padding extends AndroidKeyStoreRSACipherSpi {
		public OAEPWithSHA256AndMGF1Padding() { super("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "SHA-256"); }
	}

	public static final class OAEPWithSHA384AndMGF1Padding extends AndroidKeyStoreRSACipherSpi {
		public OAEPWithSHA384AndMGF1Padding() { super("RSA/ECB/OAEPWithSHA-384AndMGF1Padding", "SHA-384"); }
	}

	public static final class OAEPWithSHA512AndMGF1Padding extends AndroidKeyStoreRSACipherSpi {
		public OAEPWithSHA512AndMGF1Padding() { super("RSA/ECB/OAEPWithSHA-512AndMGF1Padding", "SHA-512"); }
	}

	/* AndroidKeyStoreRSACipherSpi.OAEPWithMGF1Padding.initAlgorithmSpecificParameters() and, for the other
	 * paddings, its "No parameters supported" */
	private OAEPParameterSpec keystoreParameters(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
		if (oaepDigest == null) {
			if (params != null)
				throw new InvalidAlgorithmParameterException("Unexpected parameters: " + params + ". No parameters supported");
			return null;
		}
		if (params == null)
			return new OAEPParameterSpec(oaepDigest, "MGF1", new MGF1ParameterSpec(DEFAULT_MGF1_DIGEST), PSource.PSpecified.DEFAULT);
		if (!(params instanceof OAEPParameterSpec))
			throw new InvalidAlgorithmParameterException("Unsupported parameter spec: " + params + ". Only OAEPParameterSpec supported");
		OAEPParameterSpec spec = (OAEPParameterSpec)params;
		if (!"MGF1".equalsIgnoreCase(spec.getMGFAlgorithm()))
			throw new InvalidAlgorithmParameterException("Unsupported MGF: " + spec.getMGFAlgorithm() + ". Only MGF1 supported");
		if (!Arrays.asList("SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512").contains(spec.getDigestAlgorithm().toUpperCase()))
			throw new InvalidAlgorithmParameterException("Unsupported digest: " + spec.getDigestAlgorithm());
		if (spec.getMGFParameters() == null)
			throw new InvalidAlgorithmParameterException("MGF parameters must be provided");
		if (!(spec.getMGFParameters() instanceof MGF1ParameterSpec))
			throw new InvalidAlgorithmParameterException("Unsupported MGF parameters: " + spec.getMGFParameters() + ". Only MGF1ParameterSpec supported");
		PSource pSource = spec.getPSource();
		if (!(pSource instanceof PSource.PSpecified) || ((PSource.PSpecified)pSource).getValue().length > 0)
			throw new InvalidAlgorithmParameterException("Unsupported source of encoding input P: " + pSource + ". Only pSpecifiedEmpty (PSource.PSpecified.DEFAULT) supported");
		return spec;
	}

	private void init(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {
		cipher = null;
		Key cipherKey;
		if (key instanceof AndroidKeyStorePrivateKey) {
			params = keystoreParameters(params);
			cipherKey = ((AndroidKeyStorePrivateKey)key).key;
		} else if (key instanceof PrivateKey || key instanceof PublicKey) {
			// AndroidKeyStoreCipherSpiBase: public key operations get diverted to the default provider
			if (params == null && oaepDigest != null)
				params = new OAEPParameterSpec(oaepDigest, "MGF1", new MGF1ParameterSpec(DEFAULT_MGF1_DIGEST), PSource.PSpecified.DEFAULT);
			cipherKey = key;
		} else {
			throw new InvalidKeyException("Unsupported key: " + (key != null ? key.getClass().getName() : "null"));
		}
		Cipher c;
		try {
			// the default provider's cipher: this provider takes only AndroidKeyStore private keys (SupportedKeyClasses)
			c = Cipher.getInstance(transformation);
		} catch (GeneralSecurityException e) {
			throw new InvalidKeyException(e);
		}
		if (params != null)
			c.init(opmode, cipherKey, params, random);
		else
			c.init(opmode, cipherKey, random);
		cipher = c;
	}

	@Override
	protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
		try {
			init(opmode, key, null, random);
		} catch (InvalidAlgorithmParameterException e) {
			throw new InvalidKeyException(e);
		}
	}

	@Override
	protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {
		init(opmode, key, params, random);
	}

	@Override
	protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {
		AlgorithmParameterSpec spec = null;
		if (params != null) {
			if (oaepDigest == null)
				throw new InvalidAlgorithmParameterException("Unexpected parameters: " + params + ". No parameters supported");
			try {
				spec = params.getParameterSpec(OAEPParameterSpec.class);
			} catch (InvalidParameterSpecException e) {
				throw new InvalidAlgorithmParameterException("OAEP parameters required, but not found in parameters: " + params, e);
			}
		}
		init(opmode, key, spec, random);
	}

	@Override
	protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
		// all transformations registered for this class name their block mode
		throw new UnsupportedOperationException();
	}

	@Override
	protected void engineSetPadding(String padding) {
		// all transformations registered for this class name their padding
		throw new UnsupportedOperationException();
	}

	@Override
	protected int engineGetBlockSize() {
		return 0; // not a block cipher, according to the RI
	}

	@Override
	protected byte[] engineGetIV() {
		return null; // IV never used
	}

	@Override
	protected int engineGetKeySize(Key key) throws InvalidKeyException {
		if (key instanceof RSAKey)
			return ((RSAKey)key).getModulus().bitLength();
		throw new InvalidKeyException("Unsupported key: " + key);
	}

	private Cipher cipher() {
		if (cipher == null)
			throw new IllegalStateException("Not initialized");
		return cipher;
	}

	@Override
	protected int engineGetOutputSize(int inputLen) {
		return cipher().getOutputSize(inputLen);
	}

	@Override
	protected AlgorithmParameters engineGetParameters() {
		return cipher != null && oaepDigest != null ? cipher.getParameters() : null;
	}

	@Override
	protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
		return cipher().update(input, inputOffset, inputLen);
	}

	@Override
	protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset)
	    throws ShortBufferException {
		return cipher().update(input, inputOffset, inputLen, output, outputOffset);
	}

	@Override
	protected int engineUpdate(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
		return cipher().update(input, output);
	}

	@Override
	protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen)
	    throws IllegalBlockSizeException, BadPaddingException {
		return cipher().doFinal(input, inputOffset, inputLen);
	}

	@Override
	protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset)
	    throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		return cipher().doFinal(input, inputOffset, inputLen, output, outputOffset);
	}

	@Override
	protected int engineDoFinal(ByteBuffer input, ByteBuffer output)
	    throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		return cipher().doFinal(input, output);
	}

	@Override
	protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
		return cipher().wrap(key);
	}

	@Override
	protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType)
	    throws InvalidKeyException, NoSuchAlgorithmException {
		return cipher().unwrap(wrappedKey, wrappedKeyAlgorithm, wrappedKeyType);
	}
}
