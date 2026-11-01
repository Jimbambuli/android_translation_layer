package android.atl.crypto;

import java.security.Provider;

/**
 * The first security provider, as Conscrypt's ("AndroidOpenSSL") is on Android: digests, HMAC, AES (ECB, CBC, CTR,
 * GCM), ChaCha20-Poly1305, SecureRandom and secret key generation in OpenSSL's libcrypto, with Conscrypt's algorithm
 * names, aliases and key constraints. Without it these come from Bouncy Castle, which computes them in Java; that is
 * much slower whenever Java runs slowly (interpreter, method trace, debugger), and an app that hashes or decrypts on
 * one thread then takes much longer for it than on Android. Signatures, key agreement, key pairs and the rest of
 * what Conscrypt has stay with Bouncy Castle.
 */
public final class OpenSSLProvider extends Provider {
	private static final String PREFIX = OpenSSLProvider.class.getPackage().getName() + ".";

	public OpenSSLProvider() {
		super("AndroidOpenSSL", 1.0, "Android's OpenSSL-backed security provider, on libcrypto");

		/* === AlgorithmParameters === */
		put("AlgorithmParameters.AES", PREFIX + "IvParameters$AES");
		putAliases("AlgorithmParameters", "AES", "2.16.840.1.101.3.4.1.2", "2.16.840.1.101.3.4.1.22", "2.16.840.1.101.3.4.1.42");
		put("AlgorithmParameters.ChaCha20", PREFIX + "IvParameters$ChaCha20");
		put("AlgorithmParameters.GCM", PREFIX + "IvParameters$GCM");
		putAliases("AlgorithmParameters", "GCM", "2.16.840.1.101.3.4.1.6", "2.16.840.1.101.3.4.1.26", "2.16.840.1.101.3.4.1.46");

		/* === MessageDigest === */
		put("MessageDigest.SHA-1", PREFIX + "OpenSSLMessageDigest$SHA1");
		put("Alg.Alias.MessageDigest.SHA1", "SHA-1");
		put("Alg.Alias.MessageDigest.SHA", "SHA-1");
		put("Alg.Alias.MessageDigest.1.3.14.3.2.26", "SHA-1");
		put("MessageDigest.SHA-224", PREFIX + "OpenSSLMessageDigest$SHA224");
		put("Alg.Alias.MessageDigest.SHA224", "SHA-224");
		put("Alg.Alias.MessageDigest.2.16.840.1.101.3.4.2.4", "SHA-224");
		put("MessageDigest.SHA-256", PREFIX + "OpenSSLMessageDigest$SHA256");
		put("Alg.Alias.MessageDigest.SHA256", "SHA-256");
		put("Alg.Alias.MessageDigest.2.16.840.1.101.3.4.2.1", "SHA-256");
		put("MessageDigest.SHA-384", PREFIX + "OpenSSLMessageDigest$SHA384");
		put("Alg.Alias.MessageDigest.SHA384", "SHA-384");
		put("Alg.Alias.MessageDigest.2.16.840.1.101.3.4.2.2", "SHA-384");
		put("MessageDigest.SHA-512", PREFIX + "OpenSSLMessageDigest$SHA512");
		put("Alg.Alias.MessageDigest.SHA512", "SHA-512");
		put("Alg.Alias.MessageDigest.2.16.840.1.101.3.4.2.3", "SHA-512");
		put("MessageDigest.MD5", PREFIX + "OpenSSLMessageDigest$MD5");
		put("Alg.Alias.MessageDigest.1.2.840.113549.2.5", "MD5");

		/* === KeyGenerator === */
		put("KeyGenerator.AES", PREFIX + "KeyGeneratorImpl$AES");
		put("KeyGenerator.ChaCha20", PREFIX + "KeyGeneratorImpl$ChaCha20");
		put("KeyGenerator.HmacMD5", PREFIX + "KeyGeneratorImpl$HmacMD5");
		putAliases("KeyGenerator", "HmacMD5", "1.3.6.1.5.5.8.1.1", "HMAC-MD5", "HMAC/MD5");
		put("KeyGenerator.HmacSHA1", PREFIX + "KeyGeneratorImpl$HmacSHA1");
		putAliases("KeyGenerator", "HmacSHA1", "1.2.840.113549.2.7", "1.3.6.1.5.5.8.1.2", "HMAC-SHA1", "HMAC/SHA1");
		put("KeyGenerator.HmacSHA224", PREFIX + "KeyGeneratorImpl$HmacSHA224");
		putAliases("KeyGenerator", "HmacSHA224", "1.2.840.113549.2.8", "HMAC-SHA224", "HMAC/SHA224");
		put("KeyGenerator.HmacSHA256", PREFIX + "KeyGeneratorImpl$HmacSHA256");
		putAliases("KeyGenerator", "HmacSHA256", "1.2.840.113549.2.9", "2.16.840.1.101.3.4.2.1", "HMAC-SHA256", "HMAC/SHA256");
		put("KeyGenerator.HmacSHA384", PREFIX + "KeyGeneratorImpl$HmacSHA384");
		putAliases("KeyGenerator", "HmacSHA384", "1.2.840.113549.2.10", "HMAC-SHA384", "HMAC/SHA384");
		put("KeyGenerator.HmacSHA512", PREFIX + "KeyGeneratorImpl$HmacSHA512");
		putAliases("KeyGenerator", "HmacSHA512", "1.2.840.113549.2.11", "HMAC-SHA512", "HMAC/SHA512");

		/* === SecureRandom === (the provider's only one, hence also what new SecureRandom() gets) */
		put("SecureRandom.SHA1PRNG", PREFIX + "OpenSSLRandom");
		put("SecureRandom.SHA1PRNG ImplementedIn", "Software");
		put("SecureRandom.SHA1PRNG ThreadSafe", "true");

		/* === Cipher === */
		final String aes = "OpenSSLEvpCipherAES$";
		putCipher("AES/ECB/NoPadding", aes + "AES$ECB_NoPadding");
		putCipher("AES/ECB/PKCS5Padding", aes + "AES$ECB_PKCS5Padding");
		put("Alg.Alias.Cipher.AES/ECB/PKCS7Padding", "AES/ECB/PKCS5Padding");
		putCipher("AES/CBC/NoPadding", aes + "AES$CBC_NoPadding");
		putCipher("AES/CBC/PKCS5Padding", aes + "AES$CBC_PKCS5Padding");
		put("Alg.Alias.Cipher.AES/CBC/PKCS7Padding", "AES/CBC/PKCS5Padding");
		putCipher("AES/CTR/NoPadding", aes + "AES$CTR");
		for (String size : new String[] {"128", "256"}) {
			String name = "AES_" + size;
			putCipher(name + "/ECB/NoPadding", aes + name + "$ECB_NoPadding");
			putCipher(name + "/ECB/PKCS5Padding", aes + name + "$ECB_PKCS5Padding");
			put("Alg.Alias.Cipher." + name + "/ECB/PKCS7Padding", name + "/ECB/PKCS5Padding");
			putCipher(name + "/CBC/NoPadding", aes + name + "$CBC_NoPadding");
			putCipher(name + "/CBC/PKCS5Padding", aes + name + "$CBC_PKCS5Padding");
			put("Alg.Alias.Cipher." + name + "/CBC/PKCS7Padding", name + "/CBC/PKCS5Padding");
			for (String mac : new String[] {"SHA1", "SHA224", "SHA256", "SHA384", "SHA512"})
				put("Alg.Alias.Cipher.PBEWithHmac" + mac + "And" + name, name + "/CBC/PKCS5PADDING");
		}
		putCipher("AES/GCM/NoPadding", "OpenSSLAeadCipher$AES_GCM");
		putAliases("Cipher", "AES/GCM/NoPadding", "GCM", "2.16.840.1.101.3.4.1.6", "2.16.840.1.101.3.4.1.26", "2.16.840.1.101.3.4.1.46");
		putCipher("AES_128/GCM/NoPadding", "OpenSSLAeadCipher$AES_GCM$AES_128");
		putCipher("AES_256/GCM/NoPadding", "OpenSSLAeadCipher$AES_GCM$AES_256");
		putCipher("ChaCha20/Poly1305/NoPadding", "OpenSSLAeadCipher$ChaCha20Poly1305");
		put("Alg.Alias.Cipher.ChaCha20-Poly1305", "ChaCha20/Poly1305/NoPadding");

		/* === Mac === */
		putMac("HmacMD5", "1.3.6.1.5.5.8.1.1", "HMAC-MD5", "HMAC/MD5");
		putMac("HmacSHA1", "1.2.840.113549.2.7", "1.3.6.1.5.5.8.1.2", "HMAC-SHA1", "HMAC/SHA1");
		putMac("HmacSHA224", "1.2.840.113549.2.8", "HMAC-SHA224", "HMAC/SHA224", "PBEWITHHMACSHA224");
		putMac("HmacSHA256", "1.2.840.113549.2.9", "2.16.840.1.101.3.4.2.1", "HMAC-SHA256", "HMAC/SHA256", "PBEWITHHMACSHA256");
		putMac("HmacSHA384", "1.2.840.113549.2.10", "HMAC-SHA384", "HMAC/SHA384", "PBEWITHHMACSHA384");
		putMac("HmacSHA512", "1.2.840.113549.2.11", "HMAC-SHA512", "HMAC/SHA512", "PBEWITHHMACSHA512");
	}

	private void putAliases(String type, String algorithm, String... aliases) {
		for (String alias : aliases)
			put("Alg.Alias." + type + "." + alias, algorithm);
	}

	/* like Conscrypt: only keys that give their key material as RAW */
	private void putCipher(String transformation, String className) {
		put("Cipher." + transformation, PREFIX + className);
		put("Cipher." + transformation + " SupportedKeyFormats", "RAW");
	}

	private void putMac(String algorithm, String... aliases) {
		put("Mac." + algorithm, PREFIX + "OpenSSLMac$" + algorithm);
		put("Mac." + algorithm + " SupportedKeyFormats", "RAW");
		putAliases("Mac", algorithm, aliases);
	}
}
