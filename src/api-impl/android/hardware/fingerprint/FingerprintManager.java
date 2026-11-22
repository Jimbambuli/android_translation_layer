package android.hardware.fingerprint;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import com.android.internal.R;
import com.android.server.biometrics.FprintdSensor;
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
 * AOSP's FingerprintService is ATL's fprintd sensor (com.android.server.biometrics.FprintdSensor). Without it
 * SystemServiceRegistry hands out no FingerprintManager (apps targeting O and later), or one that takes AOSP's
 * mService == null branch (older apps): no hardware, no enrolled fingerprints, no callback.
 */
public class FingerprintManager implements BiometricFingerprintConstants {

	private final Context mContext;

	/** @hide */
	public FingerprintManager(Context context) {
		mContext = context;
	}

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

	/* AOSP authenticate(CryptoObject, CancellationSignal, int, AuthenticationCallback, Handler): the checks,
	 * then the sensor (FingerprintAuthenticationClient); the callbacks run on the handler, else the main looper */
	@Deprecated
	public void authenticate(@Nullable final CryptoObject crypto, @Nullable CancellationSignal cancel,
	                         int flags, @NonNull final AuthenticationCallback callback, @Nullable Handler handler) {
		if (callback == null) {
			throw new IllegalArgumentException("Must supply an authentication callback");
		}
		if (cancel != null && cancel.isCanceled()) {
			android.util.Log.w("FingerprintManager", "authentication already canceled");
			return;
		}
		if (!FprintdSensor.isPresent())
			return;
		final Handler h = handler != null ? handler : new Handler(mContext.getMainLooper());
		final FprintdSensor.Session session = FprintdSensor.authenticate(new FprintdSensor.Client() {
			@Override
			public void onAuthenticated() {
				h.post(new Runnable() {
					@Override
					public void run() {
						callback.onAuthenticationSucceeded(new AuthenticationResult(crypto, 0, true));
					}
				});
			}

			@Override
			public void onRejected() {
				h.post(new Runnable() {
					@Override
					public void run() {
						callback.onAuthenticationFailed();
					}
				});
			}

			@Override
			public void onAcquired(final int acquiredInfo) {
				final String message = getAcquiredString(mContext, acquiredInfo, 0);
				if (message == null)
					return;
				h.post(new Runnable() {
					@Override
					public void run() {
						callback.onAuthenticationHelp(acquiredInfo, message);
					}
				});
			}

			@Override
			public void onError(final int error) {
				h.post(new Runnable() {
					@Override
					public void run() {
						callback.onAuthenticationError(error, getErrorString(mContext, error, 0));
					}
				});
			}
		});
		if (cancel != null) {
			cancel.setOnCancelListener(new CancellationSignal.OnCancelListener() {
				@Override
				public void onCancel() {
					session.cancel();
				}
			});
		}
	}

	/* AOSP hasEnrolledFingerprints() -> FingerprintService.hasEnrolledFingerprintsDeprecated() */
	@Deprecated
	public boolean hasEnrolledFingerprints() {
		return FprintdSensor.isPresent() && FprintdSensor.getState() == FprintdSensor.STATE_ENROLLED;
	}

	/* AOSP isHardwareDetected() -> FingerprintService.isHardwareDetectedDeprecated() */
	@Deprecated
	public boolean isHardwareDetected() {
		return FprintdSensor.isPresent() && FprintdSensor.getState() != FprintdSensor.STATE_NO_HARDWARE;
	}

	/** AOSP getErrorString() @hide */
	public static String getErrorString(Context context, int errMsg, int vendorCode) {
		switch (errMsg) {
			case FINGERPRINT_ERROR_HW_UNAVAILABLE:
				return context.getString(R.string.fingerprint_error_hw_not_available);
			case FINGERPRINT_ERROR_UNABLE_TO_PROCESS:
				return context.getString(R.string.fingerprint_error_unable_to_process);
			case FINGERPRINT_ERROR_TIMEOUT:
				return context.getString(R.string.fingerprint_error_timeout);
			case FINGERPRINT_ERROR_NO_SPACE:
				return context.getString(R.string.fingerprint_error_no_space);
			case FINGERPRINT_ERROR_CANCELED:
				return context.getString(R.string.fingerprint_error_canceled);
			case FINGERPRINT_ERROR_LOCKOUT:
				return context.getString(R.string.fingerprint_error_lockout);
			case FINGERPRINT_ERROR_LOCKOUT_PERMANENT:
				return context.getString(R.string.fingerprint_error_lockout_permanent);
			case FINGERPRINT_ERROR_USER_CANCELED:
				return context.getString(R.string.fingerprint_error_user_canceled);
			case FINGERPRINT_ERROR_NO_FINGERPRINTS:
				return context.getString(R.string.fingerprint_error_no_fingerprints);
			case FINGERPRINT_ERROR_HW_NOT_PRESENT:
				return context.getString(R.string.fingerprint_error_hw_not_present);
			case android.hardware.biometrics.BiometricConstants.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
				return context.getString(R.string.fingerprint_error_security_update_required);
			case FINGERPRINT_ERROR_BAD_CALIBRATION:
				return context.getString(R.string.fingerprint_error_bad_calibration);
			case android.hardware.biometrics.BiometricConstants.BIOMETRIC_ERROR_POWER_PRESSED:
				return context.getString(R.string.fingerprint_error_power_pressed);
			case FINGERPRINT_ERROR_VENDOR: {
				String[] msgArray = context.getResources().getStringArray(R.array.fingerprint_error_vendor);
				if (vendorCode < msgArray.length) {
					return msgArray[vendorCode];
				}
			}
		}
		return context.getString(R.string.fingerprint_error_vendor_unknown);
	}

	/** AOSP getAcquiredString() @hide */
	public static String getAcquiredString(Context context, int acquireInfo, int vendorCode) {
		switch (acquireInfo) {
			case FINGERPRINT_ACQUIRED_GOOD:
			case FINGERPRINT_ACQUIRED_START:
				return null;
			case FINGERPRINT_ACQUIRED_PARTIAL:
				return context.getString(R.string.fingerprint_acquired_partial);
			case FINGERPRINT_ACQUIRED_INSUFFICIENT:
				return context.getString(R.string.fingerprint_acquired_insufficient);
			case FINGERPRINT_ACQUIRED_IMAGER_DIRTY:
				return context.getString(R.string.fingerprint_acquired_imager_dirty);
			case FINGERPRINT_ACQUIRED_TOO_SLOW:
				return context.getString(R.string.fingerprint_acquired_too_slow);
			case FINGERPRINT_ACQUIRED_TOO_FAST:
				return context.getString(R.string.fingerprint_acquired_too_fast);
			case FINGERPRINT_ACQUIRED_IMMOBILE:
				return context.getString(R.string.fingerprint_acquired_immobile);
			case FINGERPRINT_ACQUIRED_TOO_BRIGHT:
				return context.getString(R.string.fingerprint_acquired_too_bright);
			case FINGERPRINT_ACQUIRED_POWER_PRESSED:
				return context.getString(R.string.fingerprint_acquired_power_press);
			case FINGERPRINT_ACQUIRED_VENDOR: {
				String[] msgArray = context.getResources().getStringArray(R.array.fingerprint_acquired_vendor);
				if (vendorCode < msgArray.length && !msgArray[vendorCode].isEmpty()) {
					return msgArray[vendorCode];
				}
			}
		}
		return null;
	}
}
