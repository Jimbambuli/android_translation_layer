package com.android.server.biometrics;

import android.hardware.biometrics.BiometricConstants;
import android.hardware.biometrics.BiometricFingerprintConstants;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * The fingerprint sensor of ATL, in the place of AOSP's FingerprintService with its HAL: fprintd over the
 * system bus (native part com_android_server_biometrics_FprintdSensor.c). Without fprintd or a reader the
 * device has no fingerprint sensor.
 *
 * The sensor counts as BIOMETRIC_STRONG (the usual strength of AOSP fingerprint sensors, config_biometric_sensors);
 * ATL's keystore does not bind keys to authentication, so the strength only decides which requests it serves.
 *
 * Lockout as AOSP's LockoutFrameworkImpl (services/.../sensors/fingerprint/hidl): 5 failed attempts in a row
 * lock out for 30 s, 20 lock out permanently; a successful attempt resets the count. AOSP keeps the count per
 * user in system_server, ATL per process.
 */
public final class FprintdSensor implements BiometricFingerprintConstants {
	public static final int STATE_NO_HARDWARE = 0;
	public static final int STATE_NOT_ENROLLED = 1;
	public static final int STATE_ENROLLED = 2;

	private static final int MAX_FAILED_ATTEMPTS_LOCKOUT_TIMED = 5;
	private static final int MAX_FAILED_ATTEMPTS_LOCKOUT_PERMANENT = 20;
	private static final long FAIL_LOCKOUT_TIMEOUT_MS = 30 * 1000;

	private static Boolean sPresent;
	private static int sFailedAttempts;
	private static long sLockoutUntil;

	private static native int nativeGetState();
	private static native long nativeStart(Session session);
	private static native void nativeRestart(long session);
	private static native void nativeStop(long session);

	private FprintdSensor() {}

	/* whether the device has the sensor; like AOSP's sensor list it is fixed when first asked */
	public static synchronized boolean isPresent() {
		if (sPresent == null)
			sPresent = nativeGetState() != STATE_NO_HARDWARE;
		return sPresent;
	}

	/* the current state: STATE_NO_HARDWARE when the reader is gone (or the device has none) */
	public static int getState() {
		return isPresent() ? nativeGetState() : STATE_NO_HARDWARE;
	}

	/* AOSP LockoutTracker modes, BiometricConstants.BIOMETRIC_LOCKOUT_* */
	public static synchronized int getLockoutMode() {
		if (sFailedAttempts >= MAX_FAILED_ATTEMPTS_LOCKOUT_PERMANENT)
			return BiometricConstants.BIOMETRIC_LOCKOUT_PERMANENT;
		if (SystemClock.elapsedRealtime() < sLockoutUntil)
			return BiometricConstants.BIOMETRIC_LOCKOUT_TIMED;
		return BiometricConstants.BIOMETRIC_LOCKOUT_NONE;
	}

	/* LockoutFrameworkImpl.addFailedAttemptForUser(): returns the lockout this attempt caused */
	private static synchronized int addFailedAttempt() {
		sFailedAttempts++;
		if (sFailedAttempts >= MAX_FAILED_ATTEMPTS_LOCKOUT_PERMANENT)
			return BiometricConstants.BIOMETRIC_LOCKOUT_PERMANENT;
		if (sFailedAttempts % MAX_FAILED_ATTEMPTS_LOCKOUT_TIMED == 0) {
			sLockoutUntil = SystemClock.elapsedRealtime() + FAIL_LOCKOUT_TIMEOUT_MS;
			return BiometricConstants.BIOMETRIC_LOCKOUT_TIMED;
		}
		return BiometricConstants.BIOMETRIC_LOCKOUT_NONE;
	}

	private static synchronized void resetFailedAttempts() {
		sFailedAttempts = 0;
		sLockoutUntil = 0;
	}

	/** What the sensor reports to its client (the HAL callbacks of AOSP's AuthenticationClient). */
	public interface Client {
		void onAuthenticated();

		/* a finger that is not enrolled */
		void onRejected();

		/* BiometricFingerprintConstants.FINGERPRINT_ACQUIRED_* */
		void onAcquired(int acquiredInfo);

		/* BiometricConstants.BIOMETRIC_ERROR_*; the session has ended */
		void onError(int error);
	}

	/**
	 * Starts an authentication; the client hears from it on the main looper until it gets onAuthenticated()
	 * or onError(), or until cancel().
	 */
	public static Session authenticate(Client client) {
		Session session = new Session(client);
		switch (getLockoutMode()) {
			case BiometricConstants.BIOMETRIC_LOCKOUT_TIMED:
				session.finishWithError(BiometricConstants.BIOMETRIC_ERROR_LOCKOUT);
				return session;
			case BiometricConstants.BIOMETRIC_LOCKOUT_PERMANENT:
				session.finishWithError(BiometricConstants.BIOMETRIC_ERROR_LOCKOUT_PERMANENT);
				return session;
		}
		session.mPtr = nativeStart(session);
		if (session.mPtr == 0)
			session.finishWithError(BiometricConstants.BIOMETRIC_ERROR_HW_UNAVAILABLE);
		return session;
	}

	public static final class Session {
		private final Client mClient;
		private final Handler mHandler = new Handler(Looper.getMainLooper());
		private long mPtr;
		private boolean mFinished;

		private Session(Client client) {
			mClient = client;
		}

		/* AOSP cancelAuthentication(): the client hears BIOMETRIC_ERROR_CANCELED */
		public void cancel() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					finishWithError(BiometricConstants.BIOMETRIC_ERROR_CANCELED);
				}
			});
		}

		/* ends the session without telling the client (the dialog went away) */
		public void stop() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					finish();
				}
			});
		}

		private boolean finish() {
			if (mFinished)
				return false;
			mFinished = true;
			if (mPtr != 0)
				nativeStop(mPtr);
			return true;
		}

		private void finishWithError(final int error) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (finish())
						mClient.onError(error);
				}
			});
		}

		/* called by the native part on the main looper with fprintd's VerifyStatus (or "error:<D-Bus error>") */
		private void onStatus(String result, boolean done) {
			if (mFinished)
				return;
			android.util.Log.d("FprintdSensor", "VerifyStatus " + result + " done=" + done);
			switch (result) {
				case "verify-match":
					resetFailedAttempts();
					finish();
					mClient.onAuthenticated();
					return;
				case "verify-no-match": {
					mClient.onRejected();
					int lockout = addFailedAttempt();
					if (lockout == BiometricConstants.BIOMETRIC_LOCKOUT_TIMED)
						finishWithError(BiometricConstants.BIOMETRIC_ERROR_LOCKOUT);
					else if (lockout == BiometricConstants.BIOMETRIC_LOCKOUT_PERMANENT)
						finishWithError(BiometricConstants.BIOMETRIC_ERROR_LOCKOUT_PERMANENT);
					else
						nativeRestart(mPtr);
					return;
				}
				/* fprintd's retry results; the FINGERPRINT_ACQUIRED_* that describes each one */
				case "verify-retry-scan":
				case "verify-remove-and-retry":
					mClient.onAcquired(FINGERPRINT_ACQUIRED_INSUFFICIENT);
					break;
				case "verify-swipe-too-short":
					mClient.onAcquired(FINGERPRINT_ACQUIRED_TOO_FAST);
					break;
				case "verify-finger-not-centered":
					mClient.onAcquired(FINGERPRINT_ACQUIRED_PARTIAL);
					break;
				case "verify-disconnected":
					finishWithError(BiometricConstants.BIOMETRIC_ERROR_HW_UNAVAILABLE);
					return;
				case "error:net.reactivated.Fprint.Error.NoEnrolledPrints":
					finishWithError(BiometricConstants.BIOMETRIC_ERROR_NO_BIOMETRICS);
					return;
				default:
					if (result.startsWith("error:")) {
						/* no device, device in use by another client, no permission */
						finishWithError(BiometricConstants.BIOMETRIC_ERROR_HW_UNAVAILABLE);
						return;
					}
					/* verify-unknown-error */
					if (done) {
						finishWithError(BiometricConstants.BIOMETRIC_ERROR_UNABLE_TO_PROCESS);
						return;
					}
			}
			if (done)
				nativeRestart(mPtr);
		}
	}
}
