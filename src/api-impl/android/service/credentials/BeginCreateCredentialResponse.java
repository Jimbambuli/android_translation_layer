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
 * Response to a {@link BeginCreateCredentialRequest} (AOSP BeginCreateCredentialResponse).
 *
 * <p>Not Parcelable here: ATL's ParceledListSlice only passes a list within the process.
 * The {@code @RequiresPermission(PROVIDE_REMOTE_CREDENTIALS)} annotation on
 * {@link Builder#setRemoteCreateEntry} is omitted (no such permission constant in ATL).
 */
public final class BeginCreateCredentialResponse {
	private final @NonNull ParceledListSlice<CreateEntry> mCreateEntries;
	private final @Nullable RemoteEntry mRemoteCreateEntry;

	/** AOSP BeginCreateCredentialResponse.BeginCreateCredentialResponse() */
	public BeginCreateCredentialResponse() {
		this(/*createEntries=*/new ParceledListSlice<>(new ArrayList<>()),
				/*remoteCreateEntry=*/null);
	}

	/* package-private */ BeginCreateCredentialResponse(
			@NonNull ParceledListSlice<CreateEntry> createEntries,
			@Nullable RemoteEntry remoteCreateEntry) {
		this.mCreateEntries = Objects.requireNonNull(createEntries);
		this.mRemoteCreateEntry = remoteCreateEntry;
	}

	/** Returns the list of create entries to be displayed on the UI. */
	public @NonNull List<CreateEntry> getCreateEntries() {
		return mCreateEntries.getList();
	}

	/** Returns the remote create entry to be displayed on the UI. */
	public @Nullable RemoteEntry getRemoteCreateEntry() {
		return mRemoteCreateEntry;
	}

	/** A builder for {@link BeginCreateCredentialResponse}. */
	public static final class Builder {
		private @NonNull List<CreateEntry> mCreateEntries = new ArrayList<>();
		private @Nullable RemoteEntry mRemoteCreateEntry;

		/** AOSP BeginCreateCredentialResponse.Builder.setCreateEntries */
		public @NonNull Builder setCreateEntries(@NonNull List<CreateEntry> createEntries) {
			Preconditions.checkCollectionNotEmpty(createEntries, "createEntries");
			mCreateEntries = Preconditions.checkCollectionElementsNotNull(
					createEntries, "createEntries");
			return this;
		}

		/** AOSP BeginCreateCredentialResponse.Builder.addCreateEntry */
		public @NonNull Builder addCreateEntry(@NonNull CreateEntry createEntry) {
			mCreateEntries.add(Objects.requireNonNull(createEntry));
			return this;
		}

		/** AOSP BeginCreateCredentialResponse.Builder.setRemoteCreateEntry */
		public @NonNull Builder setRemoteCreateEntry(@Nullable RemoteEntry remoteCreateEntry) {
			mRemoteCreateEntry = remoteCreateEntry;
			return this;
		}

		/** AOSP BeginCreateCredentialResponse.Builder.build */
		public @NonNull BeginCreateCredentialResponse build() {
			return new BeginCreateCredentialResponse(
					new ParceledListSlice<>(mCreateEntries),
					mRemoteCreateEntry);
		}
	}
}
