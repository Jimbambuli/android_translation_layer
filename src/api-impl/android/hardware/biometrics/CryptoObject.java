package android.hardware.biometrics;

import android.annotation.NonNull;
import android.security.identity.IdentityCredential;
import android.security.identity.PresentationSession;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;

/**
 * AOSP android.hardware.biometrics.CryptoObject (android-14), the base of BiometricPrompt.CryptoObject.
 * getOpId() and the operation-handle constructor from android-16 (BiometricPrompt.CryptoObject.getOperationHandle()).
 */
public class CryptoObject {
	private final Object mCrypto;

	public CryptoObject(@NonNull Signature signature) {
		mCrypto = signature;
	}

	public CryptoObject(@NonNull Cipher cipher) {
		mCrypto = cipher;
	}

	public CryptoObject(@NonNull Mac mac) {
		mCrypto = mac;
	}

	@Deprecated
	public CryptoObject(@NonNull IdentityCredential credential) {
		mCrypto = credential;
	}

	public CryptoObject(@NonNull PresentationSession session) {
		mCrypto = session;
	}

	public CryptoObject(@NonNull KeyAgreement keyAgreement) {
		mCrypto = keyAgreement;
	}

	public CryptoObject(long operationHandle) {
		mCrypto = operationHandle;
	}

	public Signature getSignature() {
		return mCrypto instanceof Signature ? (Signature)mCrypto : null;
	}

	public Cipher getCipher() {
		return mCrypto instanceof Cipher ? (Cipher)mCrypto : null;
	}

	public Mac getMac() {
		return mCrypto instanceof Mac ? (Mac)mCrypto : null;
	}

	public KeyAgreement getKeyAgreement() {
		return mCrypto instanceof KeyAgreement ? (KeyAgreement)mCrypto : null;
	}

	@Deprecated
	public IdentityCredential getIdentityCredential() {
		return mCrypto instanceof IdentityCredential ? (IdentityCredential)mCrypto : null;
	}

	public PresentationSession getPresentationSession() {
		return mCrypto instanceof PresentationSession ? (PresentationSession)mCrypto : null;
	}

	/* AOSP CryptoObject.getOpId(). Signature/Mac/Cipher go to AndroidKeyStoreProvider.getKeyStoreOperationHandle()
	 * there, which needs a KeyStoreCryptoOperation SPI; ATL's keystore has none, so they end in its
	 * "not backed by AndroidKeyStore" case (AOSP throws IllegalStateException first for an uninitialized one) */
	public long getOpId() {
		if (mCrypto == null) {
			return 0;
		} else if (mCrypto instanceof Long) {
			return (long) mCrypto;
		} else if (mCrypto instanceof IdentityCredential) {
			return ((IdentityCredential) mCrypto).getCredstoreOperationHandle();
		} else if (mCrypto instanceof PresentationSession) {
			return ((PresentationSession) mCrypto).getCredstoreOperationHandle();
		}
		throw new IllegalArgumentException("Crypto primitive not backed by AndroidKeyStore provider: " + mCrypto);
	}
}
