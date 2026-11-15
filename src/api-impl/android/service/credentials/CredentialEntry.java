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
import android.annotation.Nullable;
import android.app.slice.Slice;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;

/**
 * An entry to be shown on the UI. This entry represents a credential that can be used to
 * complete a get flow (AOSP CredentialEntry).
 */
public final class CredentialEntry implements Parcelable {
	/** The id of the {@link BeginGetCredentialOption} this entry is for. */
	private final @Nullable String mBeginGetCredentialOptionId;

	/** The type of the credential entry to be shown on the UI. */
	private final @NonNull String mType;

	/** The content to be displayed with this credential entry on the UI. */
	private final @NonNull Slice mSlice;

	/**
	 * Constructs a CredentialEntry to be displayed on the UI.
	 *
	 * @throws IllegalArgumentException If {@code beginGetCredentialOptionId} or {@code type} is
	 * null or empty.
	 * @throws NullPointerException If {@code slice} is null.
	 */
	public CredentialEntry(@NonNull String beginGetCredentialOptionId, @NonNull String type,
			@NonNull Slice slice) {
		mBeginGetCredentialOptionId = Preconditions.checkStringNotEmpty(
				beginGetCredentialOptionId, "beginGetCredentialOptionId must not be "
						+ "null, or empty");
		mType = Preconditions.checkStringNotEmpty(type, "type must not be null, or "
				+ "empty");
		mSlice = requireNonNull(slice, "slice must not be null");
	}

	/**
	 * Constructs a CredentialEntry to be displayed on the UI.
	 *
	 * @throws NullPointerException If {@code beginGetCredentialOption} or {@code slice} is null.
	 */
	public CredentialEntry(@NonNull BeginGetCredentialOption beginGetCredentialOption,
			@NonNull Slice slice) {
		requireNonNull(beginGetCredentialOption, "beginGetCredentialOption must not"
				+ " be null");
		mBeginGetCredentialOptionId = Preconditions.checkStringNotEmpty(
				beginGetCredentialOption.getId(), "Id in beginGetCredentialOption "
						+ "must not be null");
		mType = Preconditions.checkStringNotEmpty(beginGetCredentialOption.getType(),
				"type in beginGetCredentialOption must not be null");
		mSlice = requireNonNull(slice, "slice must not be null");
	}

	/** AOSP CredentialEntry.CredentialEntry(String, Slice): entry without an option id. */
	public CredentialEntry(@NonNull String type, @NonNull Slice slice) {
		mBeginGetCredentialOptionId = null;
		mType = requireNonNull(type, "type must not be null");
		mSlice = requireNonNull(slice, "slice must not be null");
	}

	private CredentialEntry(@NonNull Parcel in) {
		requireNonNull(in, "parcel must not be null");
		mType = in.readString8();
		mSlice = in.readTypedObject(Slice.CREATOR);
		mBeginGetCredentialOptionId = in.readString8();
	}

	@NonNull
	public static final Creator<CredentialEntry> CREATOR =
			new Creator<CredentialEntry>() {
				@Override
				public CredentialEntry createFromParcel(@NonNull Parcel in) {
					return new CredentialEntry(in);
				}

				@Override
				public CredentialEntry[] newArray(int size) {
					return new CredentialEntry[size];
				}
			};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeString8(mType);
		dest.writeTypedObject(mSlice, flags);
		dest.writeString8(mBeginGetCredentialOptionId);
	}

	/** Returns the id of the {@link BeginGetCredentialOption} this entry is for. */
	@NonNull
	public String getBeginGetCredentialOptionId() {
		return mBeginGetCredentialOptionId;
	}

	/** Returns the specific credential type of the entry. */
	@NonNull
	public String getType() {
		return mType;
	}

	/** Returns the {@link Slice} to be displayed on the UI. */
	@NonNull
	public Slice getSlice() {
		return mSlice;
	}
}
