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

/**
 * A class that contains biometric utilities. For authentication, see {@link BiometricPrompt}.
 *
 * The public API of AOSP (android-14). ATL has no biometric hardware: like AOSP on such a device,
 * canAuthenticate() answers BIOMETRIC_ERROR_NO_HARDWARE and the strings are null.
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
	 * Types of authenticators, defined at a level of granularity supported by
	 * {@link BiometricManager} and {@link BiometricPrompt}.
	 */
	public interface Authenticators {
		int BIOMETRIC_STRONG = 0x000F;
		int BIOMETRIC_WEAK = 0x00FF;
		int DEVICE_CREDENTIAL = 1 << 15;
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
		return BIOMETRIC_ERROR_NO_HARDWARE;
	}

	/**
	 * Produces an instance of the {@link Strings} class.
	 */
	public Strings getStrings(int authenticators) {
		return new Strings();
	}
}
