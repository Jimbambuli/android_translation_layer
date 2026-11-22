/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hardware.biometrics;

import com.android.server.biometrics.FprintdSensor;

/**
 * A class that contains biometric utilities. For authentication, see {@link BiometricPrompt}.
 *
 * The public API of AOSP (android-14). AOSP asks the AuthService, whose BiometricService builds a PreAuthInfo
 * from the sensors and the device credential; ATL does the same in the app process (preAuthenticateStatus())
 * with its only sensor, the fprintd fingerprint sensor (com.android.server.biometrics.FprintdSensor), and
 * without a device credential (KeyguardManager.isDeviceSecure() is false). The strings stay null.
 */
public class BiometricManager {
	/**
	 * No error detected.
	 */
	public static final int BIOMETRIC_SUCCESS = 0;

	/**
	 * The hardware is unavailable. Try again later.
	 */
	public static final int BIOMETRIC_ERROR_HW_UNAVAILABLE = 1;

	/**
	 * The user does not have any biometrics enrolled.
	 */
	public static final int BIOMETRIC_ERROR_NONE_ENROLLED = 11;

	/**
	 * There is no biometric hardware.
	 */
	public static final int BIOMETRIC_ERROR_NO_HARDWARE = 12;

	/**
	 * A security vulnerability has been discovered and the sensor is unavailable until a
	 * security update has addressed this issue.
	 */
	public static final int BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED = 15;

	/**
	 * Identity Check is currently not active.
	 */
	public static final int BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE = BiometricConstants.BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE;

	/**
	 * Biometrics is not allowed to verify the user in apps.
	 */
	public static final int BIOMETRIC_ERROR_NOT_ENABLED_FOR_APPS = BiometricConstants.BIOMETRIC_ERROR_NOT_ENABLED_FOR_APPS;

	/**
	 * Returned from {@link #getLastAuthenticationTime(int)} when no matching successful authentication has
	 * been performed since boot.
	 */
	public static final long BIOMETRIC_NO_AUTHENTICATION = BiometricConstants.BIOMETRIC_NO_AUTHENTICATION;

	private static final int GET_LAST_AUTH_TIME_ALLOWED_AUTHENTICATORS =
		Authenticators.DEVICE_CREDENTIAL | Authenticators.BIOMETRIC_STRONG;

	/* icon types of a BiometricPrompt fallback option (FallbackOption) */
	public static final int ICON_TYPE_PASSWORD = 0;
	public static final int ICON_TYPE_QR_CODE = 1;
	public static final int ICON_TYPE_ACCOUNT = 2;
	public static final int ICON_TYPE_GENERIC = 3;
	public static final int ICON_TYPE_SETTING = 4;

	/**
	 * Types of authenticators, defined at a level of granularity supported by
	 * {@link BiometricManager} and {@link BiometricPrompt}.
	 */
	public interface Authenticators {
		int BIOMETRIC_STRONG = 0x000F;
		int BIOMETRIC_WEAK = 0x00FF;
		int DEVICE_CREDENTIAL = 1 << 15;
		int IDENTITY_CHECK = 1 << 16;
	}

	/**
	 * Provides localized strings for an application that uses {@link BiometricPrompt} to
	 * authenticate the user.
	 */
	public static class Strings {
		Strings() {
		}

		/**
		 * Provides a localized string that can be used as the label for a button that invokes
		 * {@link BiometricPrompt}. May return null if none of the requested authenticator
		 * types are available.
		 */
		public CharSequence getButtonLabel() {
			return null;
		}

		/**
		 * Provides a localized string that can be shown while the user is authenticating with
		 * {@link BiometricPrompt}. May return null.
		 */
		public CharSequence getPromptMessage() {
			return null;
		}

		/**
		 * Provides a localized string that can be shown as the title for an app setting that
		 * enables authentication with {@link BiometricPrompt}. May return null.
		 */
		public CharSequence getSettingName() {
			return null;
		}
	}

	/**
	 * Determine if biometrics can be used.
	 */
	@Deprecated
	public int canAuthenticate() {
		return canAuthenticate(Authenticators.BIOMETRIC_WEAK);
	}

	/**
	 * Determine if any of the provided authenticators can be used.
	 */
	public int canAuthenticate(int authenticators) {
		/* BiometricService.canAuthenticate() */
		if (!isValidAuthenticatorConfig(authenticators)) {
			throw new SecurityException("Invalid authenticator configuration");
		}
		/* PreAuthInfo.getCanAuthenticateResult() */
		return biometricConstantsToBiometricManager(preAuthenticateStatus(authenticators));
	}

	/* AOSP Utils.isValidAuthenticatorConfig() for the public authenticators */
	static boolean isValidAuthenticatorConfig(int authenticators) {
		if (authenticators == 0)
			return true;
		if ((authenticators & ~(Authenticators.DEVICE_CREDENTIAL | Authenticators.BIOMETRIC_WEAK | Authenticators.IDENTITY_CHECK)) != 0)
			return false;
		final int biometricBits = authenticators & Authenticators.BIOMETRIC_WEAK;
		return (biometricBits == 0 && (authenticators & Authenticators.DEVICE_CREDENTIAL) != 0)
		       || biometricBits == Authenticators.BIOMETRIC_STRONG || biometricBits == Authenticators.BIOMETRIC_WEAK;
	}

	/* AOSP Utils.biometricConstantsToBiometricManager() (flag bp_fallback_options off) */
	private static int biometricConstantsToBiometricManager(int code) {
		switch (code) {
			case BiometricConstants.BIOMETRIC_SUCCESS:
				return BIOMETRIC_SUCCESS;
			case BiometricConstants.BIOMETRIC_ERROR_NO_BIOMETRICS:
			case BiometricConstants.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL:
				return BIOMETRIC_ERROR_NONE_ENROLLED;
			case BiometricConstants.BIOMETRIC_ERROR_HW_NOT_PRESENT:
				return BIOMETRIC_ERROR_NO_HARDWARE;
			case BiometricConstants.BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE:
				return BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE;
			case BiometricConstants.BIOMETRIC_ERROR_LOCKOUT:
			case BiometricConstants.BIOMETRIC_ERROR_LOCKOUT_PERMANENT:
				return BiometricConstants.BIOMETRIC_ERROR_LOCKOUT; // BiometricManager.BIOMETRIC_ERROR_LOCKOUT (@hide)
			default:
				return BIOMETRIC_ERROR_HW_UNAVAILABLE;
		}
	}

	/**
	 * AOSP PreAuthInfo.getInternalStatus() and Utils.authenticatorStatusToBiometricConstant() for ATL's sensors
	 * (the fprintd fingerprint sensor, if present) without a device credential: the BiometricConstants code.
	 * @hide
	 */
	static int preAuthenticateStatus(int authenticators) {
		/* Identity Check needs a settings toggle and a trusted-places signal; ATL has neither, so it is never active */
		if ((authenticators & Authenticators.IDENTITY_CHECK) != 0)
			return BiometricConstants.BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE;
		final boolean biometricRequested = (authenticators & Authenticators.BIOMETRIC_WEAK) != 0;
		final boolean credentialRequested = (authenticators & Authenticators.DEVICE_CREDENTIAL) != 0;
		final boolean credentialAvailable = false; // TrustManager.isDeviceSecure(): ATL has no lock screen
		if (biometricRequested) {
			/* the sensor is STRONG, so it is strong enough for any request; then getStatusForBiometricAuthenticator() */
			final int sensorStatus;
			if (!FprintdSensor.isPresent()) {
				sensorStatus = -1; // no sensor at all
			} else {
				final int state = FprintdSensor.getState();
				if (state == FprintdSensor.STATE_NO_HARDWARE)
					sensorStatus = BiometricConstants.BIOMETRIC_ERROR_HW_UNAVAILABLE; // BIOMETRIC_HARDWARE_NOT_DETECTED
				else if (state == FprintdSensor.STATE_NOT_ENROLLED)
					sensorStatus = BiometricConstants.BIOMETRIC_ERROR_NO_BIOMETRICS; // BIOMETRIC_NOT_ENROLLED
				else if (FprintdSensor.getLockoutMode() == BiometricConstants.BIOMETRIC_LOCKOUT_TIMED)
					sensorStatus = BiometricConstants.BIOMETRIC_ERROR_LOCKOUT;
				else if (FprintdSensor.getLockoutMode() == BiometricConstants.BIOMETRIC_LOCKOUT_PERMANENT)
					sensorStatus = BiometricConstants.BIOMETRIC_ERROR_LOCKOUT_PERMANENT;
				else
					sensorStatus = BiometricConstants.BIOMETRIC_SUCCESS;
			}
			if (sensorStatus == BiometricConstants.BIOMETRIC_SUCCESS || (credentialRequested && credentialAvailable))
				return BiometricConstants.BIOMETRIC_SUCCESS;
			if (sensorStatus != -1)
				return sensorStatus;
			return credentialRequested ? BiometricConstants.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL // CREDENTIAL_NOT_ENROLLED
			                           : BiometricConstants.BIOMETRIC_ERROR_HW_NOT_PRESENT;    // BIOMETRIC_NO_HARDWARE
		}
		if (credentialRequested)
			return credentialAvailable ? BiometricConstants.BIOMETRIC_SUCCESS : BiometricConstants.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL;
		return BiometricConstants.BIOMETRIC_ERROR_HW_NOT_PRESENT; // "No authenticators requested"
	}

	/**
	 * Gets the last time (SystemClock.elapsedRealtime()) the user successfully authenticated using one of the
	 * given authenticators. AOSP returns BIOMETRIC_NO_AUTHENTICATION when the device has no secure lock screen,
	 * whatever was authenticated before; ATL has no lock screen (see preAuthenticateStatus()).
	 */
	public long getLastAuthenticationTime(int authenticators) {
		if (authenticators == 0
		    || (GET_LAST_AUTH_TIME_ALLOWED_AUTHENTICATORS & authenticators) != authenticators) {
			throw new IllegalArgumentException(
				"Only BIOMETRIC_STRONG and DEVICE_CREDENTIAL authenticators may be used.");
		}
		return BIOMETRIC_NO_AUTHENTICATION;
	}

	/**
	 * Produces an instance of the {@link Strings} class.
	 */
	public Strings getStrings(int authenticators) {
		return new Strings();
	}
}
