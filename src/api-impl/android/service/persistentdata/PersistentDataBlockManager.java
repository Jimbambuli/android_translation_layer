/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.service.persistentdata;

import android.annotation.SystemApi;

/**
 * Interface to the persistent data partition. Provides access to information about the state of
 * factory reset protection.
 *
 * AOSP core/java/android/service/persistentdata/PersistentDataBlockManager.java. AOSP forwards every call
 * to IPersistentDataBlockService (system_server, backed by the persistent data partition). A desktop has
 * no such partition (ATL's Context has no PERSISTENT_DATA_BLOCK_SERVICE), so a directly created
 * one reports what a device without the partition reports: no factory reset protection.
 */
public class PersistentDataBlockManager {

	/** @hide */
	@SystemApi
	public static final int FLASH_LOCK_UNKNOWN = -1;

	/** @hide */
	@SystemApi
	public static final int FLASH_LOCK_UNLOCKED = 0;

	/** @hide */
	@SystemApi
	public static final int FLASH_LOCK_LOCKED = 1;

	/** @hide */
	public PersistentDataBlockManager() {
	}

	/**
	 * Returns whether or not the factory reset protection has been activated.
	 */
	public boolean isFactoryResetProtectionActive() {
		return false;
	}
}
