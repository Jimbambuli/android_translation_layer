/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.os.flagging;

import android.annotation.NonNull;
import android.annotation.SystemApi;

/**
 * Reader of the aconfig flag storage of a package (ConfigInfrastructure,
 * framework/java/android/os/flagging/AconfigPackage.java).
 *
 * ATL has no aconfig storage: AOSP's static initializer maps the *.package.map files of
 * /metadata/aconfig/maps/ and finds none on a device without that directory, so its package
 * cache stays empty and load() reports every package as not found.
 */
public class AconfigPackage {
	private AconfigPackage() {}

	public static @NonNull AconfigPackage load(@NonNull String packageName) {
		// AOSP: no entry in sStorageFilesCache
		throw new AconfigStorageReadException(
		        AconfigStorageReadException.ERROR_PACKAGE_NOT_FOUND,
		        "package " + packageName + " cannot be found on the device");
	}

	// An instance cannot exist (load() always fails); AOSP returns defaultValue for a flag that is not in the table
	public boolean getBooleanFlagValue(@NonNull String flagName, boolean defaultValue) {
		return defaultValue;
	}

	/** @hide */
	@SystemApi
	public boolean getBooleanFlagValueInternal(@NonNull String flagName, boolean defaultValue) {
		return defaultValue;
	}
}
