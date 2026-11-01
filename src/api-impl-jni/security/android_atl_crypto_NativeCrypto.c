/*
 * JNI part of android.atl.crypto: digests, HMAC, AES and ChaCha20-Poly1305 in OpenSSL's libcrypto, as Conscrypt's
 * NativeCrypto does them in BoringSSL on Android. The Java side checks offsets and lengths like Conscrypt's Java
 * classes; the checks here only keep a wrong length from reaching memory outside the arrays.
 */

#include <errno.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/random.h>

#include <openssl/core_names.h>
#include <openssl/err.h>
#include <openssl/evp.h>
#include <openssl/proverr.h>

#include "../defines.h"

#include "../generated_headers/android_atl_crypto_NativeCrypto.h"

/* the same order as NativeCrypto.MD5 ... SHA512 */
static const char *const digest_names[] = {"MD5", "SHA1", "SHA224", "SHA256", "SHA384", "SHA512"};

static const EVP_MD *get_md(jint md)
{
	switch (md) {
		case 0: return EVP_md5();
		case 1: return EVP_sha1();
		case 2: return EVP_sha224();
		case 3: return EVP_sha256();
		case 4: return EVP_sha384();
		case 5: return EVP_sha512();
		default: return NULL;
	}
}

static void throw(JNIEnv *env, const char *class, const char *message)
{
	jclass exception = (*env)->FindClass(env, class);
	if (exception)
		(*env)->ThrowNew(env, exception, message);
}

/* throws the exception for the first error on OpenSSL's queue (like Conscrypt's throwExceptionFromBoringSSLError) and clears it */
static void throw_openssl(JNIEnv *env, const char *class, const char *what)
{
	char message[256];
	unsigned long error = ERR_peek_error();
	if (error) {
		ERR_error_string_n(error, message, sizeof(message));
	} else {
		strncpy(message, what, sizeof(message) - 1);
		message[sizeof(message) - 1] = '\0';
	}
	ERR_clear_error();
	throw(env, class, message);
}

static jboolean range_ok(JNIEnv *env, jbyteArray array, jint offset, jint len)
{
	jsize size = array ? (*env)->GetArrayLength(env, array) : 0;
	if (offset < 0 || len < 0 || offset > size || len > size - offset) {
		throw(env, "java/lang/ArrayIndexOutOfBoundsException", "offset/length outside the array");
		return JNI_FALSE;
	}
	return JNI_TRUE;
}

/* ---- MessageDigest ---- */

JNIEXPORT jlong JNICALL Java_android_atl_crypto_NativeCrypto_EVP_1MD_1CTX_1create(JNIEnv *env, jclass clazz, jint md)
{
	EVP_MD_CTX *ctx = EVP_MD_CTX_new();
	if (!ctx || !EVP_DigestInit_ex(ctx, get_md(md), NULL)) {
		EVP_MD_CTX_free(ctx);
		throw_openssl(env, "java/lang/RuntimeException", "EVP_DigestInit_ex");
		return 0;
	}
	return _INTPTR(ctx);
}

JNIEXPORT void JNICALL Java_android_atl_crypto_NativeCrypto_EVP_1DigestUpdate(JNIEnv *env, jclass clazz, jlong ctx, jbyteArray input, jint offset, jint len)
{
	if (!range_ok(env, input, offset, len))
		return;
	jbyte *data = (*env)->GetPrimitiveArrayCritical(env, input, NULL);
	EVP_DigestUpdate(_PTR(ctx), data + offset, len);
	(*env)->ReleasePrimitiveArrayCritical(env, input, data, JNI_ABORT);
}

JNIEXPORT jint JNICALL Java_android_atl_crypto_NativeCrypto_EVP_1DigestFinal(JNIEnv *env, jclass clazz, jlong ctx, jbyteArray output, jint offset)
{
	unsigned char digest[EVP_MAX_MD_SIZE];
	unsigned int len = 0;
	EVP_DigestFinal_ex(_PTR(ctx), digest, &len);
	/* MessageDigest starts over after digest() */
	EVP_DigestInit_ex(_PTR(ctx), NULL, NULL);
	if (!range_ok(env, output, offset, len))
		return 0;
	(*env)->SetByteArrayRegion(env, output, offset, len, (jbyte *)digest);
	return len;
}

JNIEXPORT void JNICALL Java_android_atl_crypto_NativeCrypto_EVP_1DigestReset(JNIEnv *env, jclass clazz, jlong ctx)
{
	EVP_DigestInit_ex(_PTR(ctx), NULL, NULL);
}

JNIEXPORT jlong JNICALL Java_android_atl_crypto_NativeCrypto_EVP_1MD_1CTX_1copy(JNIEnv *env, jclass clazz, jlong ctx)
{
	EVP_MD_CTX *copy = EVP_MD_CTX_new();
	if (!copy || !EVP_MD_CTX_copy_ex(copy, _PTR(ctx))) {
		EVP_MD_CTX_free(copy);
		throw_openssl(env, "java/lang/RuntimeException", "EVP_MD_CTX_copy_ex");
		return 0;
	}
	return _INTPTR(copy);
}

JNIEXPORT void JNICALL Java_android_atl_crypto_NativeCrypto_EVP_1MD_1CTX_1free(JNIEnv *env, jclass clazz, jlong ctx)
{
	EVP_MD_CTX_free(_PTR(ctx));
}

/* ---- HMAC ---- */

JNIEXPORT jlong JNICALL Java_android_atl_crypto_NativeCrypto_HMAC_1create(JNIEnv *env, jclass clazz, jint md, jbyteArray key)
{
	EVP_MAC *mac = EVP_MAC_fetch(NULL, OSSL_MAC_NAME_HMAC, NULL); // cached by libcrypto
	EVP_MAC_CTX *ctx = mac ? EVP_MAC_CTX_new(mac) : NULL;
	EVP_MAC_free(mac); // the context keeps its own reference
	OSSL_PARAM params[] = {
		OSSL_PARAM_construct_utf8_string(OSSL_MAC_PARAM_DIGEST, (char *)digest_names[md], 0),
		OSSL_PARAM_construct_end(),
	};
	jsize key_len = (*env)->GetArrayLength(env, key);
	jbyte *key_bytes = (*env)->GetByteArrayElements(env, key, NULL);
	/* HMAC takes an empty key; OpenSSL wants a non-NULL pointer for it */
	static const unsigned char empty[1];
	int ret = ctx && EVP_MAC_init(ctx, key_len ? (unsigned char *)key_bytes : empty, key_len, params);
	(*env)->ReleaseByteArrayElements(env, key, key_bytes, JNI_ABORT);
	if (!ret) {
		EVP_MAC_CTX_free(ctx);
		throw_openssl(env, "java/lang/RuntimeException", "EVP_MAC_init");
		return 0;
	}
	return _INTPTR(ctx);
}

JNIEXPORT void JNICALL Java_android_atl_crypto_NativeCrypto_HMAC_1Update(JNIEnv *env, jclass clazz, jlong ctx, jbyteArray input, jint offset, jint len)
{
	if (!range_ok(env, input, offset, len))
		return;
	jbyte *data = (*env)->GetPrimitiveArrayCritical(env, input, NULL);
	EVP_MAC_update(_PTR(ctx), (unsigned char *)data + offset, len);
	(*env)->ReleasePrimitiveArrayCritical(env, input, data, JNI_ABORT);
}

JNIEXPORT jbyteArray JNICALL Java_android_atl_crypto_NativeCrypto_HMAC_1Final(JNIEnv *env, jclass clazz, jlong ctx)
{
	unsigned char mac[EVP_MAX_MD_SIZE];
	size_t len = 0;
	EVP_MAC_final(_PTR(ctx), mac, &len, sizeof(mac));
	/* ready for the next message with the same key, as Mac.doFinal() requires */
	EVP_MAC_init(_PTR(ctx), NULL, 0, NULL);
	jbyteArray result = (*env)->NewByteArray(env, len);
	if (result)
		(*env)->SetByteArrayRegion(env, result, 0, len, (jbyte *)mac);
	return result;
}

JNIEXPORT void JNICALL Java_android_atl_crypto_NativeCrypto_HMAC_1Reset(JNIEnv *env, jclass clazz, jlong ctx)
{
	EVP_MAC_init(_PTR(ctx), NULL, 0, NULL);
}

JNIEXPORT void JNICALL Java_android_atl_crypto_NativeCrypto_HMAC_1free(JNIEnv *env, jclass clazz, jlong ctx)
{
	EVP_MAC_CTX_free(_PTR(ctx));
}

/* ---- AES ECB/CBC/CTR ---- */

static const EVP_CIPHER *get_aes(jint mode, jint key_len)
{
	static const EVP_CIPHER *(*const ciphers[3][3])(void) = {
		{EVP_aes_128_ecb, EVP_aes_192_ecb, EVP_aes_256_ecb},
		{EVP_aes_128_cbc, EVP_aes_192_cbc, EVP_aes_256_cbc},
		{EVP_aes_128_ctr, EVP_aes_192_ctr, EVP_aes_256_ctr},
	};
	if (mode < 0 || mode > 2 || (key_len != 16 && key_len != 24 && key_len != 32))
		return NULL;
	return ciphers[mode][(key_len - 16) / 8]();
}

JNIEXPORT jlong JNICALL Java_android_atl_crypto_NativeCrypto_EVP_1CIPHER_1CTX_1new(JNIEnv *env, jclass clazz)
{
	EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
	if (!ctx)
		throw(env, "java/lang/OutOfMemoryError", "EVP_CIPHER_CTX_new");
	return _INTPTR(ctx);
}

JNIEXPORT void JNICALL Java_android_atl_crypto_NativeCrypto_EVP_1CIPHER_1CTX_1free(JNIEnv *env, jclass clazz, jlong ctx)
{
	EVP_CIPHER_CTX_free(_PTR(ctx));
}

JNIEXPORT void JNICALL Java_android_atl_crypto_NativeCrypto_EVP_1CipherInit(JNIEnv *env, jclass clazz, jlong ctx, jint mode, jbyteArray key, jbyteArray iv, jboolean encrypting, jboolean padding)
{
	const EVP_CIPHER *cipher = get_aes(mode, (*env)->GetArrayLength(env, key));
	unsigned char key_bytes[32], iv_bytes[16];
	if (!cipher || (iv && (*env)->GetArrayLength(env, iv) != EVP_CIPHER_get_iv_length(cipher))) {
		throw(env, "java/lang/IllegalArgumentException", "key or IV length");
		return;
	}
	(*env)->GetByteArrayRegion(env, key, 0, EVP_CIPHER_get_key_length(cipher), (jbyte *)key_bytes);
	if (iv)
		(*env)->GetByteArrayRegion(env, iv, 0, sizeof(iv_bytes), (jbyte *)iv_bytes);
	if (!EVP_CipherInit_ex(_PTR(ctx), cipher, NULL, key_bytes, iv ? iv_bytes : NULL, encrypting)) {
		OPENSSL_cleanse(key_bytes, sizeof(key_bytes));
		throw_openssl(env, "java/lang/RuntimeException", "EVP_CipherInit_ex");
		return;
	}
	OPENSSL_cleanse(key_bytes, sizeof(key_bytes));
	EVP_CIPHER_CTX_set_padding(_PTR(ctx), padding);
}

JNIEXPORT jint JNICALL Java_android_atl_crypto_NativeCrypto_EVP_1CipherUpdate(JNIEnv *env, jclass clazz, jlong ctx, jbyteArray output, jint output_offset, jbyteArray input, jint input_offset, jint input_len)
{
	if (!range_ok(env, input, input_offset, input_len) || !range_ok(env, output, output_offset, 0))
		return 0;
	/* OpenSSL writes at most input_len plus one block; the Java side makes room for what it will really write, which
	 * can be less, so go through a buffer when the array could be too short for the worst case */
	jint room = (*env)->GetArrayLength(env, output) - output_offset;
	jint worst = input_len + EVP_CIPHER_CTX_get_block_size(_PTR(ctx));
	unsigned char *buffer = worst > room ? malloc(worst) : NULL;
	if (worst > room && !buffer) {
		throw(env, "java/lang/OutOfMemoryError", "EVP_CipherUpdate");
		return 0;
	}
	jbyte *in = (*env)->GetPrimitiveArrayCritical(env, input, NULL);
	jbyte *out = (*env)->GetPrimitiveArrayCritical(env, output, NULL);
	int written = 0;
	int ret = EVP_CipherUpdate(_PTR(ctx), buffer ? buffer : (unsigned char *)out + output_offset, &written, (unsigned char *)in + input_offset, input_len);
	if (ret && buffer && written <= room)
		memcpy(out + output_offset, buffer, written);
	(*env)->ReleasePrimitiveArrayCritical(env, output, out, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, input, in, JNI_ABORT);
	free(buffer);
	if (!ret) {
		throw_openssl(env, "java/lang/RuntimeException", "EVP_CipherUpdate");
		return 0;
	}
	if (written > room) {
		throw(env, "java/lang/IllegalStateException", "EVP_CipherUpdate wrote more than the output array holds");
		return 0;
	}
	return written;
}

JNIEXPORT jint JNICALL Java_android_atl_crypto_NativeCrypto_EVP_1CipherFinal(JNIEnv *env, jclass clazz, jlong ctx, jbyteArray output, jint output_offset)
{
	unsigned char block[EVP_MAX_BLOCK_LENGTH];
	int written = 0;
	if (!EVP_CipherFinal_ex(_PTR(ctx), block, &written)) {
		/* as Conscrypt maps BoringSSL's CIPHER_R_* reasons */
		unsigned long error = ERR_peek_error();
		int lib = ERR_GET_LIB(error), reason = ERR_GET_REASON(error);
		if ((lib == ERR_LIB_PROV && reason == PROV_R_WRONG_FINAL_BLOCK_LENGTH) ||
		    (lib == ERR_LIB_EVP && (reason == EVP_R_WRONG_FINAL_BLOCK_LENGTH || reason == EVP_R_DATA_NOT_MULTIPLE_OF_BLOCK_LENGTH)))
			throw_openssl(env, "javax/crypto/IllegalBlockSizeException", "EVP_CipherFinal_ex");
		else
			throw_openssl(env, "javax/crypto/BadPaddingException", "EVP_CipherFinal_ex");
		return 0;
	}
	if (!range_ok(env, output, output_offset, written))
		return 0;
	(*env)->SetByteArrayRegion(env, output, output_offset, written, (jbyte *)block);
	return written;
}

/* ---- AEAD (one shot, like BoringSSL's EVP_AEAD that Conscrypt uses) ---- */

#define AEAD_AES_GCM 0
#define AEAD_CHACHA20_POLY1305 1

static jint aead(JNIEnv *env, jboolean seal, jint aead, jbyteArray key, jint tag_len, jbyteArray output, jint output_offset,
                 jbyteArray iv, jbyteArray input, jint input_offset, jint input_len, jbyteArray aad)
{
	jsize key_len = (*env)->GetArrayLength(env, key);
	const EVP_CIPHER *cipher = aead == AEAD_CHACHA20_POLY1305 ? EVP_chacha20_poly1305() : key_len == 16 ? EVP_aes_128_gcm()
	                                                                                    : key_len == 32 ? EVP_aes_256_gcm()
	                                                                                                    : NULL;
	jint text_len = seal ? input_len : input_len - tag_len;
	jint output_len = seal ? input_len + tag_len : text_len;
	if (!cipher || key_len != EVP_CIPHER_get_key_length(cipher) || (*env)->GetArrayLength(env, iv) != 12 || tag_len < 1 || tag_len > 16) {
		throw(env, "java/lang/IllegalArgumentException", "key, IV or tag length");
		return 0;
	}
	if (text_len < 0) {
		throw(env, "javax/crypto/AEADBadTagException", "input shorter than the tag");
		return 0;
	}
	if (!range_ok(env, input, input_offset, input_len) || !range_ok(env, output, output_offset, output_len))
		return 0;

	unsigned char key_bytes[32], iv_bytes[12], tag[16];
	(*env)->GetByteArrayRegion(env, key, 0, key_len, (jbyte *)key_bytes);
	(*env)->GetByteArrayRegion(env, iv, 0, sizeof(iv_bytes), (jbyte *)iv_bytes);
	jsize aad_len = aad ? (*env)->GetArrayLength(env, aad) : 0;
	jbyte *aad_bytes = aad_len ? (*env)->GetByteArrayElements(env, aad, NULL) : NULL;
	if (!seal)
		(*env)->GetByteArrayRegion(env, input, input_offset + text_len, tag_len, (jbyte *)tag);

	EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
	int len = 0, ok = ctx &&
	                  EVP_CipherInit_ex(ctx, cipher, NULL, key_bytes, iv_bytes, seal) &&
	                  (!aad_len || EVP_CipherUpdate(ctx, NULL, &len, (unsigned char *)aad_bytes, aad_len)) &&
	                  (seal || EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_AEAD_SET_TAG, tag_len, tag));
	OPENSSL_cleanse(key_bytes, sizeof(key_bytes));
	if (aad_bytes)
		(*env)->ReleaseByteArrayElements(env, aad, aad_bytes, JNI_ABORT);

	int written = 0, final_len = 0;
	if (ok) {
		/* input and output may be the same array at the same offset (in place); the Java side copies overlapping input */
		jbyte *in = (*env)->GetPrimitiveArrayCritical(env, input, NULL);
		jbyte *out = (*env)->GetPrimitiveArrayCritical(env, output, NULL);
		ok = (!text_len || EVP_CipherUpdate(ctx, (unsigned char *)out + output_offset, &written, (unsigned char *)in + input_offset, text_len)) &&
		     EVP_CipherFinal_ex(ctx, (unsigned char *)out + output_offset + written, &final_len);
		if (!ok && !seal) // no plaintext from a message that failed authentication
			memset(out + output_offset, 0, text_len);
		(*env)->ReleasePrimitiveArrayCritical(env, output, out, 0);
		(*env)->ReleasePrimitiveArrayCritical(env, input, in, JNI_ABORT);
		written += final_len;
	}
	if (ok && seal) {
		ok = EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_AEAD_GET_TAG, tag_len, tag);
		if (ok)
			(*env)->SetByteArrayRegion(env, output, output_offset + written, tag_len, (jbyte *)tag);
		written += tag_len;
	}
	EVP_CIPHER_CTX_free(ctx);
	if (!ok) {
		if (seal)
			throw_openssl(env, "java/lang/RuntimeException", "AEAD seal");
		else
			throw_openssl(env, "javax/crypto/AEADBadTagException", "AEAD open");
		return 0;
	}
	return written;
}

JNIEXPORT jint JNICALL Java_android_atl_crypto_NativeCrypto_AEAD_1seal(JNIEnv *env, jclass clazz, jint aead_type, jbyteArray key, jint tag_len, jbyteArray output, jint output_offset, jbyteArray iv, jbyteArray input, jint input_offset, jint input_len, jbyteArray aad)
{
	return aead(env, JNI_TRUE, aead_type, key, tag_len, output, output_offset, iv, input, input_offset, input_len, aad);
}

JNIEXPORT jint JNICALL Java_android_atl_crypto_NativeCrypto_AEAD_1open(JNIEnv *env, jclass clazz, jint aead_type, jbyteArray key, jint tag_len, jbyteArray output, jint output_offset, jbyteArray iv, jbyteArray input, jint input_offset, jint input_len, jbyteArray aad)
{
	return aead(env, JNI_FALSE, aead_type, key, tag_len, output, output_offset, iv, input, input_offset, input_len, aad);
}

/* ---- SecureRandom ---- */

JNIEXPORT void JNICALL Java_android_atl_crypto_NativeCrypto_getrandom(JNIEnv *env, jclass clazz, jbyteArray output)
{
	jsize len = (*env)->GetArrayLength(env, output);
	jbyte *bytes = (*env)->GetByteArrayElements(env, output, NULL);
	jsize done = 0;
	while (done < len) {
		ssize_t ret = getrandom(bytes + done, len - done, 0);
		if (ret < 0) {
			if (errno == EINTR)
				continue;
			(*env)->ReleaseByteArrayElements(env, output, bytes, JNI_ABORT);
			throw(env, "java/lang/RuntimeException", strerror(errno));
			return;
		}
		done += ret;
	}
	(*env)->ReleaseByteArrayElements(env, output, bytes, 0);
}
