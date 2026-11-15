/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.service.credentials;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Request for clearing a provider's credential state (AOSP ClearCredentialStateRequest).
 */
public final class ClearCredentialStateRequest implements Parcelable {
	/** Returns the calling app info. */
	@NonNull
	private final CallingAppInfo mCallingAppInfo;

	@NonNull
	private final Bundle mData;

	/** Returns the data associated with the request. */
	@NonNull
	public Bundle getData() {
		return mData;
	}

	/** Returns info pertaining to the app requesting the state clear. */
	@NonNull
	public CallingAppInfo getCallingAppInfo() {
		return mCallingAppInfo;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeTypedObject(mCallingAppInfo, flags);
		dest.writeBundle(mData);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public String toString() {
		return "ClearCredentialStateRequest {callingAppInfo="
				+ mCallingAppInfo.toString() + " }, {data= " + mData + "}";
	}

	/**
	 * Constructs a {@link ClearCredentialStateRequest}.
	 *
	 * @throws NullPointerException If {@code callingAppInfo} or {@code data} is null.
	 */
	public ClearCredentialStateRequest(@NonNull CallingAppInfo callingAppInfo,
			@NonNull Bundle data) {
		mCallingAppInfo = requireNonNull(
				callingAppInfo, "callingAppInfo must not be null");
		mData = requireNonNull(data, "data must not be null");
	}

	private ClearCredentialStateRequest(@NonNull Parcel in) {
		mCallingAppInfo = in.readTypedObject(CallingAppInfo.CREATOR);
		mData = requireNonNull(in.readBundle());
	}

	public static final @NonNull Creator<ClearCredentialStateRequest> CREATOR =
			new Creator<ClearCredentialStateRequest>() {
		@Override
		public ClearCredentialStateRequest[] newArray(int size) {
			return new ClearCredentialStateRequest[size];
		}

		@Override
		public ClearCredentialStateRequest createFromParcel(@NonNull Parcel in) {
			return new ClearCredentialStateRequest(in);
		}
	};
}
