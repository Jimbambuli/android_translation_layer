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

package android.app.role;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

/**
 * The public part of AOSP 14 RoleManager (packages/modules/Permission). ATL has no role service and no
 * PermissionController: no role is available on the desktop, so none is held.
 */
public final class RoleManager {
	public static final String ROLE_ASSISTANT = "android.app.role.ASSISTANT";
	public static final String ROLE_BROWSER = "android.app.role.BROWSER";
	public static final String ROLE_DIALER = "android.app.role.DIALER";
	public static final String ROLE_SMS = "android.app.role.SMS";
	public static final String ROLE_EMERGENCY = "android.app.role.EMERGENCY";
	public static final String ROLE_HOME = "android.app.role.HOME";
	public static final String ROLE_CALL_REDIRECTION = "android.app.role.CALL_REDIRECTION";
	public static final String ROLE_CALL_SCREENING = "android.app.role.CALL_SCREENING";
	public static final String ROLE_NOTES = "android.app.role.NOTES";
	public static final String ROLE_WALLET = "android.app.role.WALLET";
	public static final String ACTION_REQUEST_ROLE = "android.app.role.action.REQUEST_ROLE";

	private final Context mContext;

	public RoleManager(Context context) {
		mContext = context;
	}

	public Intent createRequestRoleIntent(String roleName) {
		if (TextUtils.isEmpty(roleName))
			throw new IllegalArgumentException("roleName cannot be null or empty");
		Intent intent = new Intent(ACTION_REQUEST_ROLE);
		intent.putExtra(Intent.EXTRA_ROLE_NAME, roleName);
		return intent;
	}

	public boolean isRoleAvailable(String roleName) {
		if (TextUtils.isEmpty(roleName))
			throw new IllegalArgumentException("roleName cannot be null or empty");
		return false;
	}

	public boolean isRoleHeld(String roleName) {
		if (TextUtils.isEmpty(roleName))
			throw new IllegalArgumentException("roleName cannot be null or empty");
		return false;
	}
}
