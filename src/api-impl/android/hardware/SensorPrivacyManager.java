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

package android.hardware;

import android.content.Context;

/* AOSP SensorPrivacyManager. SensorPrivacyService.supportsSensorToggle() answers from the device's
 * config_supports*Toggle resources; ATL has no sensor privacy toggles (no microphone or camera kill switch
 * that ATL could read or set), so no toggle is supported, as on a device without them. */
public final class SensorPrivacyManager {

	/* AOSP SensorPrivacyManager.Sensors (values of SensorPrivacySensorProto) */
	public static class Sensors {

		private Sensors() {}

		public static final int MICROPHONE = 1;

		public static final int CAMERA = 2;
	}

	/* ToggleType (values of SensorPrivacyIndividualEnabledSensorProto) */
	public static final int TOGGLE_TYPE_SOFTWARE = 1;

	public static final int TOGGLE_TYPE_HARDWARE = 2;

	private static SensorPrivacyManager sInstance;

	private SensorPrivacyManager() {}

	/** @hide */
	public static SensorPrivacyManager getInstance(Context context) {
		synchronized (SensorPrivacyManager.class) {
			if (sInstance == null)
				sInstance = new SensorPrivacyManager();
			return sInstance;
		}
	}

	public boolean supportsSensorToggle(int sensor) {
		return supportsSensorToggle(TOGGLE_TYPE_SOFTWARE, sensor);
	}

	public boolean supportsSensorToggle(int toggleType, int sensor) {
		return false;
	}
}
