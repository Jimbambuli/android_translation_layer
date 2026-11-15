package android.hardware.biometrics;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.DialogInterface;
import android.os.CancellationSignal;
import android.security.identity.IdentityCredential;
import android.security.identity.PresentationSession;
import android.text.TextUtils;
import java.security.Signature;
import java.util.concurrent.Executor;
import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * AOSP android.hardware.biometrics.BiometricPrompt (android-14), public API.
 *
 * AOSP hands the prompt to the AuthService (IAuthService.authenticate), which checks the sensors and the
 * device credential before it shows a dialog (the same check as BiometricManager.canAuthenticate()).
 * ATL has no AuthService and no biometric sensors: the check is BiometricManager.canAuthenticate() of
 * this context, and a failed check reaches the callback as the service reports it,
 * BiometricServiceReceiver.onError(TYPE_NONE, error, 0) -> onAuthenticationError(error, message) on the executor.
 */
public class BiometricPrompt implements BiometricConstants {

	public static final int DISMISSED_REASON_BIOMETRIC_CONFIRMED = 1;
	public static final int DISMISSED_REASON_NEGATIVE = 2;
	public static final int DISMISSED_REASON_USER_CANCEL = 3;
	public static final int DISMISSED_REASON_BIOMETRIC_CONFIRM_NOT_REQUIRED = 4;
	public static final int DISMISSED_REASON_ERROR = 5;
	public static final int DISMISSED_REASON_SERVER_REQUESTED = 6;
	public static final int DISMISSED_REASON_CREDENTIAL_CONFIRMED = 7;

	public static final int AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL = 1;
	public static final int AUTHENTICATION_RESULT_TYPE_BIOMETRIC = 2;

	/* AOSP PromptInfo, the fields the public API sets and reads */
	private static final class PromptInfo {
		CharSequence title;
		boolean useDefaultTitle;
		CharSequence subtitle;
		boolean useDefaultSubtitle;
		CharSequence description;
		CharSequence negativeButtonText;
		boolean confirmationRequested = true;
		boolean deviceCredentialAllowed;
		int authenticators;
		boolean allowBackgroundAuthentication;
	}

	public static class Builder {
		private final PromptInfo mPromptInfo;
		private final Context mContext;

		public Builder(Context context) {
			mPromptInfo = new PromptInfo();
			mContext = context;
		}

		@NonNull
		public Builder setTitle(@NonNull CharSequence title) {
			mPromptInfo.title = title;
			return this;
		}

		@NonNull
		public Builder setUseDefaultTitle() {
			mPromptInfo.useDefaultTitle = true;
			return this;
		}

		@NonNull
		public Builder setSubtitle(@NonNull CharSequence subtitle) {
			mPromptInfo.subtitle = subtitle;
			return this;
		}

		@NonNull
		public Builder setUseDefaultSubtitle() {
			mPromptInfo.useDefaultSubtitle = true;
			return this;
		}

		@NonNull
		public Builder setDescription(@NonNull CharSequence description) {
			mPromptInfo.description = description;
			return this;
		}

		@NonNull
		public Builder setNegativeButton(@NonNull CharSequence text, @NonNull Executor executor,
		                                 @NonNull DialogInterface.OnClickListener listener) {
			if (TextUtils.isEmpty(text)) {
				throw new IllegalArgumentException("Text must be set and non-empty");
			}
			if (executor == null) {
				throw new IllegalArgumentException("Executor must not be null");
			}
			if (listener == null) {
				throw new IllegalArgumentException("Listener must not be null");
			}
			mPromptInfo.negativeButtonText = text;
			return this;
		}

		@NonNull
		public Builder setConfirmationRequired(boolean requireConfirmation) {
			mPromptInfo.confirmationRequested = requireConfirmation;
			return this;
		}

		@Deprecated
		@NonNull
		public Builder setDeviceCredentialAllowed(boolean allowed) {
			mPromptInfo.deviceCredentialAllowed = allowed;
			return this;
		}

		@NonNull
		public Builder setAllowedAuthenticators(int authenticators) {
			mPromptInfo.authenticators = authenticators;
			return this;
		}

		@NonNull
		public Builder setAllowBackgroundAuthentication(boolean allow) {
			mPromptInfo.allowBackgroundAuthentication = allow;
			return this;
		}

		/* AOSP Builder.build(): title is required; a negative button only without device credential */
		@NonNull
		public BiometricPrompt build() {
			final CharSequence title = mPromptInfo.title;
			final boolean useDefaultTitle = mPromptInfo.useDefaultTitle;
			final CharSequence negative = mPromptInfo.negativeButtonText;
			final boolean isCredentialAllowed = mPromptInfo.deviceCredentialAllowed
			                                    || (mPromptInfo.authenticators & BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0;
			if (TextUtils.isEmpty(title) && !useDefaultTitle) {
				throw new IllegalArgumentException("Title must be set and non-empty");
			} else if (TextUtils.isEmpty(negative) && !isCredentialAllowed) {
				throw new IllegalArgumentException("Negative text must be set and non-empty");
			} else if (!TextUtils.isEmpty(negative) && isCredentialAllowed) {
				throw new IllegalArgumentException("Can't have both negative button behavior"
				                                   + " and device credential enabled");
			}
			return new BiometricPrompt(mContext, mPromptInfo);
		}
	}

	private final Context mContext;
	private final PromptInfo mPromptInfo;

	private BiometricPrompt(Context context, PromptInfo promptInfo) {
		mContext = context;
		mPromptInfo = promptInfo;
	}

	@NonNull
	public CharSequence getTitle() {
		return mPromptInfo.title;
	}

	public boolean shouldUseDefaultTitle() {
		return mPromptInfo.useDefaultTitle;
	}

	@Nullable
	public CharSequence getSubtitle() {
		return mPromptInfo.subtitle;
	}

	public boolean shouldUseDefaultSubtitle() {
		return mPromptInfo.useDefaultSubtitle;
	}

	@Nullable
	public CharSequence getDescription() {
		return mPromptInfo.description;
	}

	@Nullable
	public CharSequence getNegativeButtonText() {
		return mPromptInfo.negativeButtonText;
	}

	public boolean isConfirmationRequired() {
		return mPromptInfo.confirmationRequested;
	}

	public int getAllowedAuthenticators() {
		return mPromptInfo.authenticators;
	}

	public boolean isAllowBackgroundAuthentication() {
		return mPromptInfo.allowBackgroundAuthentication;
	}

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

		public CryptoObject(long operationHandle) {
			super(operationHandle);
		}

		public long getOperationHandle() {
			return super.getOpId();
		}
	}

	/* AOSP BiometricPrompt.AuthenticationResult (fields of BiometricAuthenticator.AuthenticationResult) */
	public static class AuthenticationResult {
		private final CryptoObject mCryptoObject;
		private final int mAuthenticationType;

		public AuthenticationResult(CryptoObject crypto, int authenticationType) {
			mCryptoObject = crypto;
			mAuthenticationType = authenticationType;
		}

		public CryptoObject getCryptoObject() {
			return mCryptoObject;
		}

		public int getAuthenticationType() {
			return mAuthenticationType;
		}
	}

	public abstract static class AuthenticationCallback {
		public void onAuthenticationError(int errorCode, CharSequence errString) {}

		public void onAuthenticationHelp(int helpCode, CharSequence helpString) {}

		public void onAuthenticationSucceeded(AuthenticationResult result) {}

		public void onAuthenticationFailed() {}

		public void onAuthenticationAcquired(int acquireInfo) {}

		public void onSystemEvent(int event) {}
	}

	/* AOSP BiometricPrompt.authenticate(CryptoObject, ...) */
	public void authenticate(@NonNull CryptoObject crypto, @NonNull CancellationSignal cancel,
	                         @NonNull Executor executor, @NonNull AuthenticationCallback callback) {
		if (crypto == null) {
			throw new IllegalArgumentException("Must supply a crypto object");
		}
		if (cancel == null) {
			throw new IllegalArgumentException("Must supply a cancellation signal");
		}
		if (executor == null) {
			throw new IllegalArgumentException("Must supply an executor");
		}
		if (callback == null) {
			throw new IllegalArgumentException("Must supply a callback");
		}
		// Disallow explicitly setting any non-Strong biometric authenticator types.
		int authenticators = mPromptInfo.authenticators;
		if (authenticators == 0) {
			authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;
		}
		final int biometricStrength = authenticators & BiometricManager.Authenticators.BIOMETRIC_WEAK;
		if ((biometricStrength & ~BiometricManager.Authenticators.BIOMETRIC_STRONG) != 0) {
			throw new IllegalArgumentException("Only Strong biometrics supported with crypto");
		}
		authenticateInternal(authenticators, cancel, executor, callback);
	}

	/* AOSP BiometricPrompt.authenticate(CancellationSignal, ...) */
	public void authenticate(@NonNull CancellationSignal cancel, @NonNull Executor executor,
	                         @NonNull AuthenticationCallback callback) {
		if (cancel == null) {
			throw new IllegalArgumentException("Must supply a cancellation signal");
		}
		if (executor == null) {
			throw new IllegalArgumentException("Must supply an executor");
		}
		if (callback == null) {
			throw new IllegalArgumentException("Must supply a callback");
		}
		authenticateInternal(mPromptInfo.authenticators, cancel, executor, callback);
	}

	/* AOSP authenticateInternal(); the service's precondition check (canAuthenticate) fails on ATL,
	 * so the callback gets the error the way BiometricServiceReceiver.onError() delivers it */
	private void authenticateInternal(int authenticators, CancellationSignal cancel, Executor executor,
	                                  AuthenticationCallback callback) {
		if (cancel.isCanceled()) {
			android.util.Log.w("BiometricPrompt", "Authentication already canceled");
			return;
		}
		if (authenticators == 0) {
			authenticators = mPromptInfo.deviceCredentialAllowed
			                     ? BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL
			                     : BiometricManager.Authenticators.BIOMETRIC_WEAK;
		}
		/* BiometricManager is not registered as a system service in ATL (SystemServiceRegistry), so ask
		 * it directly; its answer is the one ATL gives for every app */
		final int error = new BiometricManager().canAuthenticate(authenticators);
		if (error == BiometricManager.BIOMETRIC_SUCCESS)
			return;
		final String message;
		switch (error) {
			/* AOSP strings (core/res/res/values/strings.xml) */
			case BIOMETRIC_ERROR_HW_NOT_PRESENT:
				message = "Biometric hardware unavailable"; // biometric_error_hw_unavailable
				break;
			case BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL:
				message = "No pin, pattern, or password set"; // biometric_error_device_not_secured
				break;
			default:
				message = "Error authenticating"; // biometric_error_generic
				break;
		}
		executor.execute(new Runnable() {
			@Override
			public void run() {
				callback.onAuthenticationError(error, message);
			}
		});
	}
}
