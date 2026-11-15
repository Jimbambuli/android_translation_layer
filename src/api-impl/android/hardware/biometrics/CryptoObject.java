package android.hardware.biometrics;

import android.annotation.NonNull;
import android.security.identity.IdentityCredential;
import android.security.identity.PresentationSession;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * AOSP android.hardware.biometrics.CryptoObject (android-14), the base of BiometricPrompt.CryptoObject.
 * Without the hidden getOpId(): it asks the keystore (AndroidKeyStoreProvider) for the operation handle,
 * which only the biometric service consumes.
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

	public Signature getSignature() {
		return mCrypto instanceof Signature ? (Signature)mCrypto : null;
	}

	public Cipher getCipher() {
		return mCrypto instanceof Cipher ? (Cipher)mCrypto : null;
	}

	public Mac getMac() {
		return mCrypto instanceof Mac ? (Mac)mCrypto : null;
	}

	@Deprecated
	public IdentityCredential getIdentityCredential() {
		return mCrypto instanceof IdentityCredential ? (IdentityCredential)mCrypto : null;
	}

	public PresentationSession getPresentationSession() {
		return mCrypto instanceof PresentationSession ? (PresentationSession)mCrypto : null;
	}
}
