package android.security.keystore;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignatureSpi;

/**
 * ECDSA signatures of AndroidKeyStoreBCWorkaroundProvider, like AOSP's android.security.keystore2.AndroidKeyStoreECDSASignatureSpi
 * with AndroidKeyStoreSignatureSpiBase: signing takes only an AndroidKeyStore EC private key, which keystore2 uses; verifying
 * goes to the default provider's signature of the same algorithm. ATL has the key material and lets the default provider
 * sign with it (NONEwithECDSA truncates the input to the group size there as keystore2 does).
 */
public abstract class AndroidKeyStoreECDSASignatureSpi extends SignatureSpi {

	public static final class NONE extends AndroidKeyStoreECDSASignatureSpi {
		public NONE() { super("NONEwithECDSA"); }
	}

	public static final class SHA1 extends AndroidKeyStoreECDSASignatureSpi {
		public SHA1() { super("SHA1withECDSA"); }
	}

	public static final class SHA224 extends AndroidKeyStoreECDSASignatureSpi {
		public SHA224() { super("SHA224withECDSA"); }
	}

	public static final class SHA256 extends AndroidKeyStoreECDSASignatureSpi {
		public SHA256() { super("SHA256withECDSA"); }
	}

	public static final class SHA384 extends AndroidKeyStoreECDSASignatureSpi {
		public SHA384() { super("SHA384withECDSA"); }
	}

	public static final class SHA512 extends AndroidKeyStoreECDSASignatureSpi {
		public SHA512() { super("SHA512withECDSA"); }
	}

	private final String algorithm;
	private Signature signature;
	private boolean signing;
	private SignatureException cachedException;

	AndroidKeyStoreECDSASignatureSpi(String algorithm) {
		this.algorithm = algorithm;
	}

	private Signature defaultProviderSignature() throws InvalidKeyException {
		try {
			// the default provider's: this provider takes only AndroidKeyStore private keys (SupportedKeyClasses)
			return Signature.getInstance(algorithm);
		} catch (GeneralSecurityException e) {
			throw new InvalidKeyException(e);
		}
	}

	@Override
	protected final void engineInitSign(PrivateKey key) throws InvalidKeyException {
		engineInitSign(key, null);
	}

	@Override
	protected final void engineInitSign(PrivateKey privateKey, SecureRandom random) throws InvalidKeyException {
		signature = null;
		signing = false;
		cachedException = null;
		if (privateKey == null)
			throw new InvalidKeyException("Unsupported key: null");
		if (!(privateKey instanceof AndroidKeyStorePrivateKey))
			throw new InvalidKeyException("Unsupported private key type: " + privateKey);
		// AndroidKeyStoreECDSASignatureSpi.initKey()
		if (!"EC".equalsIgnoreCase(privateKey.getAlgorithm()))
			throw new InvalidKeyException("Unsupported key algorithm: " + privateKey.getAlgorithm() + ". Only [ec, ed25519, eddsa] supported");
		Signature s = defaultProviderSignature();
		if (random != null)
			s.initSign(((AndroidKeyStorePrivateKey)privateKey).key, random);
		else
			s.initSign(((AndroidKeyStorePrivateKey)privateKey).key);
		signature = s;
		signing = true;
	}

	@Override
	protected final void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
		signature = null;
		signing = false;
		cachedException = null;
		Signature s = defaultProviderSignature();
		s.initVerify(publicKey);
		signature = s;
	}

	private Signature initialized() throws SignatureException {
		if (cachedException != null)
			throw new SignatureException(cachedException);
		if (signature == null)
			throw new SignatureException("object not initialized for signing or verification");
		return signature;
	}

	@Override
	protected final void engineUpdate(byte b) throws SignatureException {
		initialized().update(b);
	}

	@Override
	protected final void engineUpdate(byte[] b, int off, int len) throws SignatureException {
		initialized().update(b, off, len);
	}

	@Override
	protected final void engineUpdate(ByteBuffer input) {
		try {
			initialized().update(input);
		} catch (SignatureException e) {
			cachedException = e;
		}
	}

	@Override
	protected final byte[] engineSign() throws SignatureException {
		Signature s = initialized();
		if (!signing)
			throw new SignatureException("object not initialized for signing");
		return s.sign();
	}

	@Override
	protected final boolean engineVerify(byte[] sigBytes) throws SignatureException {
		if (signature == null || signing)
			throw new IllegalStateException("Not initialised.");
		return initialized().verify(sigBytes);
	}

	@Override
	protected final boolean engineVerify(byte[] sigBytes, int offset, int len) throws SignatureException {
		return engineVerify(ArrayUtils.subarray(sigBytes, offset, len));
	}

	@Deprecated
	@Override
	protected final Object engineGetParameter(String param) throws InvalidParameterException {
		throw new InvalidParameterException();
	}

	@Deprecated
	@Override
	protected final void engineSetParameter(String param, Object value) throws InvalidParameterException {
		throw new InvalidParameterException();
	}
}
