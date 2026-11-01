package android.atl.crypto;

/**
 * The native calls of this provider, on OpenSSL's libcrypto (android_atl_crypto_NativeCrypto.c). Named after the
 * BoringSSL functions that Conscrypt's NativeCrypto exposes on Android.
 */
final class NativeCrypto {
	private NativeCrypto() {}

	/* digests, in the order of the native digest table */
	static final int MD5 = 0;
	static final int SHA1 = 1;
	static final int SHA224 = 2;
	static final int SHA256 = 3;
	static final int SHA384 = 4;
	static final int SHA512 = 5;

	static native long EVP_MD_CTX_create(int md);
	static native void EVP_DigestUpdate(long ctx, byte[] input, int offset, int len);
	/** writes the digest and starts over; returns its length */
	static native int EVP_DigestFinal(long ctx, byte[] output, int offset);
	static native void EVP_DigestReset(long ctx);
	static native long EVP_MD_CTX_copy(long ctx);
	static native void EVP_MD_CTX_free(long ctx);

	static native long HMAC_create(int md, byte[] key);
	static native void HMAC_Update(long ctx, byte[] input, int offset, int len);
	/** returns the MAC and starts over with the same key */
	static native byte[] HMAC_Final(long ctx);
	static native void HMAC_Reset(long ctx);
	static native void HMAC_free(long ctx);

	/* AES modes */
	static final int ECB = 0;
	static final int CBC = 1;
	static final int CTR = 2;

	static native long EVP_CIPHER_CTX_new();
	static native void EVP_CIPHER_CTX_free(long ctx);
	static native void EVP_CipherInit(long ctx, int mode, byte[] key, byte[] iv, boolean encrypting, boolean padding);
	static native int EVP_CipherUpdate(long ctx, byte[] output, int outputOffset, byte[] input, int inputOffset, int inputLen);
	/** throws BadPaddingException or IllegalBlockSizeException like Conscrypt */
	static native int EVP_CipherFinal(long ctx, byte[] output, int outputOffset)
	    throws javax.crypto.BadPaddingException, javax.crypto.IllegalBlockSizeException;

	/* AEADs */
	static final int AES_GCM = 0;
	static final int CHACHA20_POLY1305 = 1;

	static native int AEAD_seal(int aead, byte[] key, int tagLen, byte[] output, int outputOffset, byte[] iv,
	                            byte[] input, int inputOffset, int inputLen, byte[] aad);
	/** throws AEADBadTagException if the tag does not match */
	static native int AEAD_open(int aead, byte[] key, int tagLen, byte[] output, int outputOffset, byte[] iv,
	                            byte[] input, int inputOffset, int inputLen, byte[] aad)
	    throws javax.crypto.AEADBadTagException;

	static native void getrandom(byte[] output);
}
