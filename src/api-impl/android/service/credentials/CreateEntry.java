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
import android.app.slice.Slice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * An entry to be shown on the UI; represents a place where a credential can be created (AOSP CreateEntry).
 */
public final class CreateEntry implements Parcelable {
	private final @NonNull Slice mSlice;

	private CreateEntry(@NonNull Parcel in) {
		mSlice = in.readTypedObject(Slice.CREATOR);
	}

	@NonNull
	public static final Creator<CreateEntry> CREATOR = new Creator<CreateEntry>() {
		@Override
		public CreateEntry createFromParcel(@NonNull Parcel in) {
			return new CreateEntry(in);
		}

		@Override
		public CreateEntry[] newArray(int size) {
			return new CreateEntry[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeTypedObject(mSlice, flags);
	}

	/**
	 * Constructs a {@link CreateEntry}.
	 *
	 * @throws NullPointerException If {@code slice} is null (AOSP: AnnotationValidations).
	 */
	public CreateEntry(
			@NonNull Slice slice) {
		this.mSlice = java.util.Objects.requireNonNull(slice);
	}

	/** Returns the content to be displayed with this entry on the UI. */
	@NonNull
	public Slice getSlice() {
		return mSlice;
	}
}
