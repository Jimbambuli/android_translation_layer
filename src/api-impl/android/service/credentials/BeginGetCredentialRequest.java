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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Query stage request for getting user's credentials from a given credential provider
 * (AOSP BeginGetCredentialRequest).
 */
public final class BeginGetCredentialRequest implements Parcelable {
	/** Info about the calling app; null if not available. */
	@Nullable private final CallingAppInfo mCallingAppInfo;

	/** List of type specific credential options to get in the query phase. */
	@NonNull private final List<BeginGetCredentialOption> mBeginGetCredentialOptions;

	private BeginGetCredentialRequest(@Nullable CallingAppInfo callingAppInfo,
			@NonNull List<BeginGetCredentialOption> getBeginCredentialOptions) {
		this.mCallingAppInfo = callingAppInfo;
		this.mBeginGetCredentialOptions = getBeginCredentialOptions;
	}

	private BeginGetCredentialRequest(@NonNull Parcel in) {
		mCallingAppInfo = in.readTypedObject(CallingAppInfo.CREATOR);
		List<BeginGetCredentialOption> getBeginCredentialOptions = new ArrayList<>();
		in.readTypedList(getBeginCredentialOptions, BeginGetCredentialOption.CREATOR);
		mBeginGetCredentialOptions = getBeginCredentialOptions;
	}

	@NonNull
	public static final Creator<BeginGetCredentialRequest> CREATOR =
			new Creator<BeginGetCredentialRequest>() {
				@Override
				public BeginGetCredentialRequest createFromParcel(Parcel in) {
					return new BeginGetCredentialRequest(in);
				}

				@Override
				public BeginGetCredentialRequest[] newArray(int size) {
					return new BeginGetCredentialRequest[size];
				}
			};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeTypedObject(mCallingAppInfo, flags);
		dest.writeTypedList(mBeginGetCredentialOptions);
	}

	/** Returns info pertaining to the app requesting credentials. */
	public @Nullable CallingAppInfo getCallingAppInfo() {
		return mCallingAppInfo;
	}

	/** Returns the list of type specific credential options to return credentials for. */
	public @NonNull List<BeginGetCredentialOption> getBeginGetCredentialOptions() {
		return mBeginGetCredentialOptions;
	}

	/** A builder for {@link BeginGetCredentialRequest}. */
	public static final class Builder {
		private CallingAppInfo mCallingAppInfo = null;
		private List<BeginGetCredentialOption> mBeginGetCredentialOptions = new ArrayList<>();

		/** AOSP BeginGetCredentialRequest.Builder.setCallingAppInfo */
		public @NonNull Builder setCallingAppInfo(@Nullable CallingAppInfo callingAppInfo) {
			mCallingAppInfo = callingAppInfo;
			return this;
		}

		/** AOSP BeginGetCredentialRequest.Builder.setBeginGetCredentialOptions */
		public @NonNull Builder setBeginGetCredentialOptions(
				@NonNull List<BeginGetCredentialOption> getBeginCredentialOptions) {
			Preconditions.checkCollectionNotEmpty(getBeginCredentialOptions,
					"getBeginCredentialOptions");
			Preconditions.checkCollectionElementsNotNull(getBeginCredentialOptions,
					"getBeginCredentialOptions");
			mBeginGetCredentialOptions = getBeginCredentialOptions;
			return this;
		}

		/** AOSP BeginGetCredentialRequest.Builder.addBeginGetCredentialOption */
		public @NonNull Builder addBeginGetCredentialOption(
				@NonNull BeginGetCredentialOption beginGetCredentialOption) {
			Objects.requireNonNull(beginGetCredentialOption,
					"beginGetCredentialOption must not be null");
			mBeginGetCredentialOptions.add(beginGetCredentialOption);
			return this;
		}

		/**
		 * AOSP BeginGetCredentialRequest.Builder.build
		 *
		 * @throws NullPointerException If {@code beginGetCredentialOptions} is empty
		 * (AOSP: IllegalArgumentException from Preconditions.checkCollectionNotEmpty).
		 */
		public @NonNull BeginGetCredentialRequest build() {
			Preconditions.checkCollectionNotEmpty(mBeginGetCredentialOptions,
					"beginGetCredentialOptions");
			return new BeginGetCredentialRequest(mCallingAppInfo, mBeginGetCredentialOptions);
		}
	}
}
