package android.security.keystore;

import android.security.KeyPairGeneratorSpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyPairGeneratorSpi;
import java.security.KeyStoreException;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * KeyPairGenerator of the AndroidKeyStore provider, like AOSP's android.security.keystore2.AndroidKeyStoreKeyPairGeneratorSpi
 * (only RSA so far). AOSP has KeyMint generate the key pair and its certificate; ATL generates the key pair with the
 * default provider and builds the certificate as KeyCreationResult.aidl describes it for a key without attestation:
 * serial number, subject (= issuer), notBefore and notAfter from the spec, a Key Usage extension from the purposes,
 * self-signed if the key may sign, else with an empty signature.
 */
public abstract class AndroidKeyStoreKeyPairGeneratorSpi extends KeyPairGeneratorSpi {

	public static class RSA extends AndroidKeyStoreKeyPairGeneratorSpi {
		public RSA() {
			super("RSA");
		}
	}

	private static final int RSA_DEFAULT_KEY_SIZE = 2048;

	private final String algorithm;
	private KeyGenParameterSpec spec;
	private int keySizeBits;
	private BigInteger publicExponent;
	private SecureRandom random;

	AndroidKeyStoreKeyPairGeneratorSpi(String algorithm) {
		this.algorithm = algorithm;
	}

	@Override
	public void initialize(int keysize, SecureRandom random) {
		throw new IllegalArgumentException(KeyGenParameterSpec.class.getName() + " or " + KeyPairGeneratorSpec.class.getName()
		                                   + " required to initialize this KeyPairGenerator");
	}

	@Override
	public void initialize(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
		spec = null;
		KeyGenParameterSpec spec;
		if (params instanceof KeyGenParameterSpec) {
			spec = (KeyGenParameterSpec)params;
		} else if (params instanceof KeyPairGeneratorSpec) {
			try {
				spec = fromLegacy((KeyPairGeneratorSpec)params);
			} catch (NullPointerException | IllegalArgumentException e) {
				throw new InvalidAlgorithmParameterException(e);
			}
		} else if (params == null) {
			throw new InvalidAlgorithmParameterException("Must supply params of type " + KeyGenParameterSpec.class.getName()
			                                             + " or " + KeyPairGeneratorSpec.class.getName());
		} else {
			throw new InvalidAlgorithmParameterException("Unsupported params class: " + params.getClass().getName()
			                                             + ". Supported: " + KeyGenParameterSpec.class.getName() + ", " + KeyPairGeneratorSpec.class.getName());
		}
		if (spec.getKeystoreAlias() == null)
			throw new InvalidAlgorithmParameterException("KeyStore entry alias not provided");

		// initAlgorithmSpecificParameters() for RSA
		int keySizeBits = spec.getKeySize() > 0 ? spec.getKeySize() : -1;
		BigInteger publicExponent = null;
		AlgorithmParameterSpec algSpecificSpec = spec.getAlgorithmParameterSpec();
		if (algSpecificSpec instanceof RSAKeyGenParameterSpec) {
			RSAKeyGenParameterSpec rsaSpec = (RSAKeyGenParameterSpec)algSpecificSpec;
			if (keySizeBits == -1)
				keySizeBits = rsaSpec.getKeysize();
			else if (keySizeBits != rsaSpec.getKeysize())
				throw new InvalidAlgorithmParameterException("RSA key size must match  between " + spec + " and " + algSpecificSpec
				                                             + ": " + keySizeBits + " vs " + rsaSpec.getKeysize());
			publicExponent = rsaSpec.getPublicExponent();
		} else if (algSpecificSpec != null) {
			throw new InvalidAlgorithmParameterException("RSA may only use RSAKeyGenParameterSpec");
		}
		if (publicExponent == null)
			publicExponent = RSAKeyGenParameterSpec.F4;
		if (publicExponent.compareTo(BigInteger.ZERO) < 1)
			throw new InvalidAlgorithmParameterException("RSA public exponent must be positive: " + publicExponent);
		if (keySizeBits == -1)
			keySizeBits = RSA_DEFAULT_KEY_SIZE;
		// checkValidKeySize() for RSA
		if (keySizeBits < 512 || keySizeBits > 8192)
			throw new InvalidAlgorithmParameterException("RSA key size must be >= 512 and <= 8192");

		this.spec = spec;
		this.keySizeBits = keySizeBits;
		this.publicExponent = publicExponent;
		this.random = random;
	}

	/* buildKeyGenParameterSpecFromLegacy() for RSA */
	private KeyGenParameterSpec fromLegacy(KeyPairGeneratorSpec legacySpec) {
		if (legacySpec.getKeyType() != null && !algorithm.equalsIgnoreCase(legacySpec.getKeyType()))
			throw new IllegalArgumentException("Key type must be " + algorithm + ": " + legacySpec.getKeyType());
		KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(legacySpec.getKeystoreAlias(),
		    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY);
		builder.setDigests(new String[] {KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_MD5, KeyProperties.DIGEST_SHA1,
		                                 KeyProperties.DIGEST_SHA224, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512});
		builder.setEncryptionPaddings(new String[] {KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1,
		                                            KeyProperties.ENCRYPTION_PADDING_RSA_OAEP});
		builder.setSignaturePaddings(new String[] {KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS});
		builder.setRandomizedEncryptionRequired(false);
		if (legacySpec.getKeySize() != -1)
			builder.setKeySize(legacySpec.getKeySize());
		if (legacySpec.getAlgorithmParameterSpec() != null)
			builder.setAlgorithmParameterSpec(legacySpec.getAlgorithmParameterSpec());
		builder.setCertificateSubject(legacySpec.getSubjectDN());
		builder.setCertificateSerialNumber(legacySpec.getSerialNumber());
		builder.setCertificateNotBefore(legacySpec.getStartDate());
		builder.setCertificateNotAfter(legacySpec.getEndDate());
		builder.setUserAuthenticationRequired(false);
		return builder.build();
	}

	@Override
	public KeyPair generateKeyPair() {
		if (spec == null)
			throw new IllegalStateException("Not initialized");
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm);
			generator.initialize(new RSAKeyGenParameterSpec(keySizeBits, publicExponent), random != null ? random : new SecureRandom());
			KeyPair pair = generator.generateKeyPair();
			X509Certificate certificate = certificate(pair);
			AndroidKeyStorePrivateKey privateKey = new AndroidKeyStoreRSAPrivateKey((RSAPrivateKey)pair.getPrivate());
			new AndroidKeyStore().engineSetKeyEntry(spec.getKeystoreAlias(), privateKey, null, new X509Certificate[] {certificate});
			return new KeyPair(certificate.getPublicKey(), privateKey);
		} catch (GeneralSecurityException e) {
			throw new ProviderException("Failed to generate key pair.", e);
		}
	}

	/* the certificate KeyCreationResult.aidl asks for when there is no attestation */
	private X509Certificate certificate(KeyPair pair) throws GeneralSecurityException {
		byte[] sha256WithRSA = der(0x30, der(0x06, new byte[] {0x2a, (byte)0x86, 0x48, (byte)0x86, (byte)0xf7, 0x0d, 0x01, 0x01, 0x0b}), new byte[] {0x05, 0x00});
		byte[] name = spec.getCertificateSubject().getEncoded();
		int purposes = spec.getPurposes();
		int keyUsage = 0; // KeyUsage bits, bit 0 (digitalSignature) first
		if ((purposes & KeyProperties.PURPOSE_SIGN) != 0)
			keyUsage |= 0x80; // digitalSignature
		if ((purposes & KeyProperties.PURPOSE_WRAP_KEY) != 0)
			keyUsage |= 0x20; // keyEncipherment
		if ((purposes & KeyProperties.PURPOSE_DECRYPT) != 0)
			keyUsage |= 0x10; // dataEncipherment
		if ((purposes & KeyProperties.PURPOSE_AGREE_KEY) != 0)
			keyUsage |= 0x08; // keyAgreement
		if ((purposes & KeyProperties.PURPOSE_ATTEST_KEY) != 0)
			keyUsage |= 0x04; // keyCertSign
		byte[] keyUsageBits = keyUsage == 0 ? new byte[] {0} : new byte[] {(byte)Integer.numberOfTrailingZeros(keyUsage), (byte)keyUsage};
		byte[] extensions = der(0xa3, der(0x30, der(0x30,
		    der(0x06, new byte[] {0x55, 0x1d, 0x0f}), // id-ce-keyUsage
		    new byte[] {0x01, 0x01, (byte)0xff}, // critical
		    der(0x04, der(0x03, keyUsageBits)))));
		byte[] tbs = der(0x30,
		    der(0xa0, der(0x02, new byte[] {2})), // v3
		    der(0x02, spec.getCertificateSerialNumber().toByteArray()),
		    sha256WithRSA, name,
		    der(0x30, time(spec.getCertificateNotBefore()), time(spec.getCertificateNotAfter())),
		    name, pair.getPublic().getEncoded(), extensions);
		byte[] signature = new byte[] {0}; // no unused bits, and no signature unless the key may sign
		if ((purposes & KeyProperties.PURPOSE_SIGN) != 0) {
			Signature signer = Signature.getInstance("SHA256withRSA");
			signer.initSign(pair.getPrivate());
			signer.update(tbs);
			byte[] value = signer.sign();
			signature = new byte[1 + value.length];
			System.arraycopy(value, 0, signature, 1, value.length);
		}
		byte[] encoded = der(0x30, tbs, sha256WithRSA, der(0x03, signature));
		return (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(encoded));
	}

	/* RFC 5280 4.1.2.5: UTCTime through 2049, GeneralizedTime from 2050 */
	private static byte[] time(Date date) {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.setTime(date);
		int year = calendar.get(Calendar.YEAR);
		boolean utc = year >= 1950 && year < 2050;
		SimpleDateFormat format = new SimpleDateFormat(utc ? "yyMMddHHmmss'Z'" : "yyyyMMddHHmmss'Z'", Locale.ROOT);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return der(utc ? 0x17 : 0x18, format.format(date).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
	}

	/* a DER element: tag, definite length, contents */
	private static byte[] der(int tag, byte[]... contents) {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		for (byte[] content : contents)
			body.write(content, 0, content.length);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(tag);
		int length = body.size();
		if (length < 0x80) {
			out.write(length);
		} else {
			int bytes = (32 - Integer.numberOfLeadingZeros(length) + 7) / 8;
			out.write(0x80 | bytes);
			for (int i = bytes - 1; i >= 0; i--)
				out.write(length >> (8 * i));
		}
		byte[] b = body.toByteArray();
		out.write(b, 0, b.length);
		return out.toByteArray();
	}
}
