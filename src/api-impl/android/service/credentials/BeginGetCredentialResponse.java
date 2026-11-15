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
import android.content.pm.ParceledListSlice;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Response from a credential provider to {@link BeginGetCredentialRequest}
 * (AOSP BeginGetCredentialResponse).
 *
 * <p>Not Parcelable here: ATL's ParceledListSlice only passes a list within the process.
 * The {@code @RequiresPermission(PROVIDE_REMOTE_CREDENTIALS)} annotation on
 * {@link Builder#setRemoteCredentialEntry} is omitted (no such permission constant in ATL).
 */
public final class BeginGetCredentialResponse {
	private final @NonNull ParceledListSlice<CredentialEntry> mCredentialEntries;
	private final @NonNull ParceledListSlice<Action> mAuthenticationEntries;
	private final @NonNull ParceledListSlice<Action> mActions;
	private final @Nullable RemoteEntry mRemoteCredentialEntry;

	/** AOSP BeginGetCredentialResponse.BeginGetCredentialResponse() */
	public BeginGetCredentialResponse() {
		this(/*credentialEntries=*/new ParceledListSlice<>(new ArrayList<>()),
				/*authenticationEntries=*/new ParceledListSlice<>(new ArrayList<>()),
				/*actions=*/new ParceledListSlice<>(new ArrayList<>()),
				/*remoteCredentialEntry=*/null);
	}

	private BeginGetCredentialResponse(
			@NonNull ParceledListSlice<CredentialEntry> credentialEntries,
			@NonNull ParceledListSlice<Action> authenticationEntries,
			@NonNull ParceledListSlice<Action> actions,
			@Nullable RemoteEntry remoteCredentialEntry) {
		mCredentialEntries = credentialEntries;
		mAuthenticationEntries = authenticationEntries;
		mActions = actions;
		mRemoteCredentialEntry = remoteCredentialEntry;
	}

	/** Returns the list of credential entries to be displayed on the UI. */
	public @NonNull List<CredentialEntry> getCredentialEntries() {
		return mCredentialEntries.getList();
	}

	/** Returns the list of authentication actions to be displayed on the UI. */
	public @NonNull List<Action> getAuthenticationActions() {
		return mAuthenticationEntries.getList();
	}

	/** Returns the list of actions to be displayed on the UI. */
	public @NonNull List<Action> getActions() {
		return mActions.getList();
	}

	/** Returns the remote credential entry to be displayed on the UI. */
	public @Nullable RemoteEntry getRemoteCredentialEntry() {
		return mRemoteCredentialEntry;
	}

	/** Builds a {@link BeginGetCredentialResponse} instance. */
	public static final class Builder {
		private List<CredentialEntry> mCredentialEntries = new ArrayList<>();
		private List<Action> mAuthenticationEntries = new ArrayList<>();
		private List<Action> mActions = new ArrayList<>();
		private RemoteEntry mRemoteCredentialEntry;

		/** AOSP BeginGetCredentialResponse.Builder.setRemoteCredentialEntry */
		public @NonNull Builder setRemoteCredentialEntry(@Nullable RemoteEntry
				remoteCredentialEntry) {
			mRemoteCredentialEntry = remoteCredentialEntry;
			return this;
		}

		/** AOSP BeginGetCredentialResponse.Builder.addCredentialEntry */
		public @NonNull Builder addCredentialEntry(@NonNull CredentialEntry credentialEntry) {
			mCredentialEntries.add(Objects.requireNonNull(credentialEntry));
			return this;
		}

		/** AOSP BeginGetCredentialResponse.Builder.addAuthenticationAction */
		public @NonNull Builder addAuthenticationAction(@NonNull Action authenticationAction) {
			mAuthenticationEntries.add(Objects.requireNonNull(authenticationAction));
			return this;
		}

		/** AOSP BeginGetCredentialResponse.Builder.addAction */
		public @NonNull Builder addAction(@NonNull Action action) {
			mActions.add(Objects.requireNonNull(action, "action must not be null"));
			return this;
		}

		/** AOSP BeginGetCredentialResponse.Builder.setActions */
		public @NonNull Builder setActions(@NonNull List<Action> actions) {
			mActions = Preconditions.checkCollectionElementsNotNull(actions,
					"actions");
			return this;
		}

		/** AOSP BeginGetCredentialResponse.Builder.setCredentialEntries */
		public @NonNull Builder setCredentialEntries(
				@NonNull List<CredentialEntry> credentialEntries) {
			mCredentialEntries = Preconditions.checkCollectionElementsNotNull(
					credentialEntries,
					"credentialEntries");
			return this;
		}

		/** AOSP BeginGetCredentialResponse.Builder.setAuthenticationActions */
		public @NonNull Builder setAuthenticationActions(
				@NonNull List<Action> authenticationActions) {
			mAuthenticationEntries = Preconditions.checkCollectionElementsNotNull(
					authenticationActions,
					"authenticationActions");
			return this;
		}

		/** AOSP BeginGetCredentialResponse.Builder.build */
		public @NonNull BeginGetCredentialResponse build() {
			return new BeginGetCredentialResponse(
					new ParceledListSlice<>(mCredentialEntries),
					new ParceledListSlice<>(mAuthenticationEntries),
					new ParceledListSlice<>(mActions),
					mRemoteCredentialEntry);
		}
	}
}
