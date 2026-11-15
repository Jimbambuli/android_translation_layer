package android.hardware.fingerprint;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.hardware.biometrics.BiometricFingerprintConstants;
import android.os.CancellationSignal;
import android.os.Handler;
import android.security.identity.IdentityCredential;
import android.security.identity.PresentationSession;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * AOSP android.hardware.fingerprint.FingerprintManager (android-14), public API.
 *
 * ATL has no fingerprint service (IFingerprintService), so every method takes AOSP's mService == null
 * branch: no hardware, no enrolled fingerprints, and authenticate() returns without a callback.
 */
public class FingerprintManager implements BiometricFingerprintConstants {

	/* AOSP FingerprintManager.CryptoObject */
	public static final class CryptoObject extends android.hardware.biometrics.CryptoObject {
		public CryptoObject(@NonNull Signature signature) {
			super(signature);
		}

		public CryptoObject(@NonNull Cipher cipher) {
			super(cipher);
		}

		public CryptoObject(@NonNull Mac mac) {
			super(mac);
		}

		@Deprecated
		public CryptoObject(@NonNull IdentityCredential credential) {
			super(credential);
		}

		public CryptoObject(@NonNull PresentationSession session) {
			super(session);
		}

		public Signature getSignature() {
			return super.getSignature();
		}

		public Cipher getCipher() {
			return super.getCipher();
		}

		public Mac getMac() {
			return super.getMac();
		}

		@Deprecated
		public @Nullable IdentityCredential getIdentityCredential() {
			return super.getIdentityCredential();
		}

		public @Nullable PresentationSession getPresentationSession() {
			return super.getPresentationSession();
		}
	}

	/* AOSP FingerprintManager.AuthenticationResult (public part; the Fingerprint is hidden) */
	public static class AuthenticationResult {
		private final CryptoObject mCryptoObject;
		private final int mUserId;
		private final boolean mIsStrongBiometric;

		AuthenticationResult(CryptoObject crypto, int userId, boolean isStrongBiometric) {
			mCryptoObject = crypto;
			mUserId = userId;
			mIsStrongBiometric = isStrongBiometric;
		}

		public CryptoObject getCryptoObject() {
			return mCryptoObject;
		}
	}

	/* AOSP FingerprintManager.AuthenticationCallback */
	public static abstract class AuthenticationCallback {
		public void onAuthenticationError(int errorCode, CharSequence errString) {}

		public void onAuthenticationHelp(int helpCode, CharSequence helpString) {}

		public void onAuthenticationSucceeded(AuthenticationResult result) {}

		public void onAuthenticationFailed() {}
	}

	/* AOSP authenticate(CryptoObject, CancellationSignal, int, AuthenticationCallback, Handler):
	 * the checks, then nothing happens without mService */
	@Deprecated
	public void authenticate(@Nullable CryptoObject crypto, @Nullable CancellationSignal cancel,
	                         int flags, @NonNull AuthenticationCallback callback, @Nullable Handler handler) {
		if (callback == null) {
			throw new IllegalArgumentException("Must supply an authentication callback");
		}
		if (cancel != null && cancel.isCanceled()) {
			android.util.Log.w("FingerprintManager", "authentication already canceled");
			return;
		}
	}

	/* AOSP hasEnrolledFingerprints() -> hasEnrolledFingerprints(userId): false without mService */
	@Deprecated
	public boolean hasEnrolledFingerprints() {
		return false;
	}

	/* AOSP isHardwareDetected(): false without mService */
	@Deprecated
	public boolean isHardwareDetected() {
		return false;
	}
}
