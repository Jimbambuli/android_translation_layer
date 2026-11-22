package android.hardware.biometrics;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.DialogInterface;
import android.os.CancellationSignal;
import android.security.identity.IdentityCredential;
import android.security.identity.PresentationSession;
import android.text.TextUtils;
import android.hardware.fingerprint.FingerprintManager;
import com.android.server.biometrics.FprintdSensor;
import java.security.Signature;
import java.util.concurrent.Executor;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;

/**
 * AOSP android.hardware.biometrics.BiometricPrompt (android-14), public API.
 *
 * AOSP hands the prompt to the AuthService (IAuthService.authenticate), which checks the sensors and the
 * device credential before it shows a dialog (the same check as BiometricManager.canAuthenticate()).
 * ATL makes that check in the app process (BiometricManager.preAuthenticateStatus()); a failed check reaches
 * the callback as the service reports it, BiometricServiceReceiver.onError(modality, error, 0) ->
 * onAuthenticationError(error, message) on the executor. Otherwise ATL shows the dialog of SystemUI's
 * AuthContainerView as an AlertDialog of the app and runs the fprintd sensor (FprintdSensor) behind it.
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
		int logoRes;
		android.graphics.Bitmap logoBitmap;
		String logoDescription;
		PromptContentView contentView;
	}

	/* AOSP BiometricPrompt.ButtonInfo */
	static class ButtonInfo {
		Executor executor;
		DialogInterface.OnClickListener listener;

		ButtonInfo(Executor ex, DialogInterface.OnClickListener l) {
			executor = ex;
			listener = l;
		}
	}

	static final int MAX_LOGO_DESCRIPTION_CHARACTER_NUMBER = 30;

	public static class Builder {
		private final PromptInfo mPromptInfo;
		private final Context mContext;
		private ButtonInfo mNegativeButtonInfo;

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

		/* AOSP Builder.setLogoRes()/setLogoBitmap()/setLogoDescription()/setContentView() (android-16): kept in
		 * the PromptInfo; the logo resource is not turned into a bitmap, ATL's prompt shows no dialog */
		@NonNull
		public Builder setLogoRes(int logoRes) {
			if (mPromptInfo.logoBitmap != null) {
				throw new IllegalStateException("Exclusively one of logo resource or logo bitmap can be set");
			}
			if (logoRes != 0) {
				mPromptInfo.logoRes = logoRes;
			}
			return this;
		}

		@NonNull
		public Builder setLogoBitmap(@NonNull android.graphics.Bitmap logoBitmap) {
			if (mPromptInfo.logoRes != 0) {
				throw new IllegalStateException("Exclusively one of logo resource or logo bitmap can be set");
			}
			mPromptInfo.logoBitmap = logoBitmap;
			return this;
		}

		@NonNull
		public Builder setLogoDescription(@NonNull String logoDescription) {
			if (logoDescription == null || logoDescription.isEmpty()) {
				throw new IllegalArgumentException("Logo description passed in can not be null");
			}
			if (logoDescription.length() > MAX_LOGO_DESCRIPTION_CHARACTER_NUMBER) {
				android.util.Log.w("BiometricPrompt", "Logo description passed in exceeds" + MAX_LOGO_DESCRIPTION_CHARACTER_NUMBER
						+ " character number and may be truncated.");
			}
			mPromptInfo.logoDescription = logoDescription;
			return this;
		}

		@NonNull
		public Builder setContentView(@NonNull PromptContentView view) {
			mPromptInfo.contentView = view;
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
			mNegativeButtonInfo = new ButtonInfo(executor, listener);
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
			return new BiometricPrompt(mContext, mPromptInfo, mNegativeButtonInfo);
		}
	}

	private final Context mContext;
	private final PromptInfo mPromptInfo;
	private final ButtonInfo mNegativeButtonInfo;

	private BiometricPrompt(Context context, PromptInfo promptInfo, ButtonInfo negativeButtonInfo) {
		mContext = context;
		mPromptInfo = promptInfo;
		mNegativeButtonInfo = negativeButtonInfo;
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

		public CryptoObject(@NonNull KeyAgreement keyAgreement) {
			super(keyAgreement);
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

		public @Nullable KeyAgreement getKeyAgreement() {
			return super.getKeyAgreement();
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
		authenticateInternal(crypto, authenticators, cancel, executor, callback);
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
		authenticateInternal(null, mPromptInfo.authenticators, cancel, executor, callback);
	}

	/* AOSP authenticateInternal() and BiometricService's handleAuthenticate(): the precondition check
	 * (PreAuthInfo.getPreAuthenticateStatus()), then the dialog with the sensor */
	private void authenticateInternal(final CryptoObject crypto, int authenticators, CancellationSignal cancel,
	                                  final Executor executor, final AuthenticationCallback callback) {
		if (cancel.isCanceled()) {
			android.util.Log.w("BiometricPrompt", "Authentication already canceled");
			return;
		}
		if (authenticators == 0) {
			authenticators = mPromptInfo.deviceCredentialAllowed
			                     ? BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL
			                     : BiometricManager.Authenticators.BIOMETRIC_WEAK;
		}
		final int error = BiometricManager.preAuthenticateStatus(authenticators);
		if (error != BIOMETRIC_SUCCESS) {
			/* the modality of a sensor error is the fingerprint (FingerprintManager.getErrorString()); without
			 * a sensor it is TYPE_NONE with the generic strings of BiometricServiceReceiver.onError() */
			final String message;
			switch (error) {
				case BIOMETRIC_ERROR_HW_NOT_PRESENT:
					message = mContext.getString(com.android.internal.R.string.biometric_error_hw_unavailable);
					break;
				case BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL:
					message = mContext.getString(com.android.internal.R.string.biometric_error_device_not_secured);
					break;
				default:
					message = FingerprintManager.getErrorString(mContext, error, 0);
					break;
			}
			executor.execute(new Runnable() {
				@Override
				public void run() {
					callback.onAuthenticationError(error, message);
				}
			});
			return;
		}
		new PromptSession(crypto, executor, callback).start(cancel);
	}

	/* the dialog (SystemUI AuthContainerView/AuthBiometricView) and the sensor session of one authenticate() */
	private final class PromptSession implements FprintdSensor.Client {
		private final CryptoObject mCrypto;
		private final Executor mExecutor;
		private final AuthenticationCallback mCallback;
		private android.app.AlertDialog mDialog;
		private FprintdSensor.Session mSensor;
		private boolean mDone;

		PromptSession(CryptoObject crypto, Executor executor, AuthenticationCallback callback) {
			mCrypto = crypto;
			mExecutor = executor;
			mCallback = callback;
		}

		/* SystemUI's fingerprint_dialog_touch_sensor */
		private static final String TOUCH_SENSOR = "Touch the fingerprint sensor";

		private String text(String status) {
			StringBuilder text = new StringBuilder();
			CharSequence subtitle = mPromptInfo.useDefaultSubtitle
			                            ? mContext.getString(com.android.internal.R.string.fingerprint_dialog_default_subtitle)
			                            : mPromptInfo.subtitle;
			for (CharSequence part : new CharSequence[] {subtitle, mPromptInfo.description}) {
				if (!TextUtils.isEmpty(part))
					text.append(part).append("\n\n");
			}
			return text.append(status).toString();
		}

		void start(CancellationSignal cancel) {
			mDialog = new android.app.AlertDialog(mContext);
			mDialog.setTitle(mPromptInfo.useDefaultTitle
			                     ? mContext.getString(com.android.internal.R.string.biometric_dialog_default_title)
			                     : mPromptInfo.title);
			mDialog.setMessage(text(TOUCH_SENSOR));
			mDialog.setCanceledOnTouchOutside(false);
			if (mNegativeButtonInfo != null) {
				/* DISMISSED_REASON_NEGATIVE: the app's listener, no callback */
				mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mPromptInfo.negativeButtonText, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (end())
							mNegativeButtonInfo.executor.execute(new Runnable() {
								@Override
								public void run() {
									mNegativeButtonInfo.listener.onClick(null, DialogInterface.BUTTON_NEGATIVE);
								}
							});
					}
				});
			} else {
				/* device credential allowed but not set up: the dialog can only be canceled */
				mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						onUserCancel();
					}
				});
			}
			/* DISMISSED_REASON_USER_CANCEL (back, Escape, close button) */
			mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					onUserCancel();
				}
			});
			mDialog.show();
			mSensor = FprintdSensor.authenticate(this);
			cancel.setOnCancelListener(new CancellationSignal.OnCancelListener() {
				@Override
				public void onCancel() {
					if (!mDone)
						mSensor.cancel();
				}
			});
		}

		/* ends the session; false if it had ended */
		private boolean end() {
			if (mDone)
				return false;
			mDone = true;
			mSensor.stop();
			mDialog.dismiss();
			return true;
		}

		private void onUserCancel() {
			if (end())
				post(new Runnable() {
					@Override
					public void run() {
						mCallback.onAuthenticationError(BIOMETRIC_ERROR_USER_CANCELED,
						                                FingerprintManager.getErrorString(mContext, BIOMETRIC_ERROR_USER_CANCELED, 0));
					}
				});
		}

		private void post(Runnable runnable) {
			mExecutor.execute(runnable);
		}

		@Override
		public void onAuthenticated() {
			if (end())
				post(new Runnable() {
					@Override
					public void run() {
						mCallback.onAuthenticationSucceeded(new AuthenticationResult(mCrypto, AUTHENTICATION_RESULT_TYPE_BIOMETRIC));
					}
				});
		}

		@Override
		public void onRejected() {
			if (mDone)
				return;
			mDialog.setMessage(text(mContext.getString(com.android.internal.R.string.biometric_not_recognized)));
			post(new Runnable() {
				@Override
				public void run() {
					mCallback.onAuthenticationFailed();
				}
			});
		}

		@Override
		public void onAcquired(final int acquiredInfo) {
			if (mDone)
				return;
			final String message = FingerprintManager.getAcquiredString(mContext, acquiredInfo, 0);
			if (message == null)
				return;
			mDialog.setMessage(text(message));
			post(new Runnable() {
				@Override
				public void run() {
					mCallback.onAuthenticationHelp(acquiredInfo, message);
				}
			});
		}

		@Override
		public void onError(final int error) {
			if (end())
				post(new Runnable() {
					@Override
					public void run() {
						mCallback.onAuthenticationError(error, FingerprintManager.getErrorString(mContext, error, 0));
					}
				});
		}
	}
}
