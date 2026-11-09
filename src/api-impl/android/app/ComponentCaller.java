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

package android.app;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import java.util.Objects;

/**
 * Represents the app that launched an activity, or that sent it a new intent or a result
 * (Android 15, taken from the AOSP android-14 QPR source that carries it).
 * ATL: there is no ActivityTaskManager to ask. The caller is known when it is an activity of the same
 * app (its package and uid); for any other launcher the package is null and the uid -1 (Process.INVALID_UID),
 * as ActivityClient answers when it cannot tell.
 */
public final class ComponentCaller {
	private final IBinder mActivityToken;
	private final IBinder mCallerToken;
	private final String mPackage;
	private final int mUid;

	/** Not for apps: the tokens identify the activity and its caller to the system. */
	public ComponentCaller(@Nullable IBinder activityToken, @Nullable IBinder callerToken) {
		this(activityToken, callerToken, null, -1);
	}

	ComponentCaller(@Nullable IBinder activityToken, @Nullable IBinder callerToken, @Nullable String callerPackage, int callerUid) {
		mActivityToken = activityToken;
		mCallerToken = callerToken;
		mPackage = callerPackage;
		mUid = callerUid;
	}

	/** The uid of the caller, -1 if it is not known. */
	public int getUid() {
		return mUid;
	}

	/** The package name of the caller, null if it is not known. */
	@Nullable
	public String getPackage() {
		return mPackage;
	}

	/**
	 * Checks whether the caller has permission to access a content URI. ATL: a caller of the same app
	 * (same uid) may access what the app itself may, any other caller is denied.
	 */
	public int checkContentUriPermission(@NonNull Uri uri, int modeFlags) {
		Objects.requireNonNull(uri);
		return mUid != -1 && mUid == android.system.Os.getuid() ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == null || !(obj instanceof ComponentCaller)) {
			return false;
		}
		ComponentCaller other = (ComponentCaller)obj;
		return this.mActivityToken == other.mActivityToken
		    && this.mCallerToken == other.mCallerToken;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + Objects.hashCode(mActivityToken);
		result = 31 * result + Objects.hashCode(mCallerToken);
		return result;
	}
}
