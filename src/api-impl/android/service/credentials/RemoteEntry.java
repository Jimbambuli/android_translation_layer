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
 * An entry to be shown on the UI; represents a credential held on a remote device (AOSP RemoteEntry).
 */
public final class RemoteEntry implements Parcelable {
	private final @NonNull Slice mSlice;

	private RemoteEntry(@NonNull Parcel in) {
		mSlice = in.readTypedObject(Slice.CREATOR);
	}

	@NonNull
	public static final Creator<RemoteEntry> CREATOR = new Creator<RemoteEntry>() {
		@Override
		public RemoteEntry createFromParcel(@NonNull Parcel in) {
			return new RemoteEntry(in);
		}

		@Override
		public RemoteEntry[] newArray(int size) {
			return new RemoteEntry[size];
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
	 * Constructs a {@link RemoteEntry}.
	 *
	 * @throws NullPointerException If {@code slice} is null (AOSP: AnnotationValidations).
	 */
	public RemoteEntry(
			@NonNull Slice slice) {
		this.mSlice = java.util.Objects.requireNonNull(slice);
	}

	/** Returns the content to be displayed with this entry on the UI. */
	@NonNull
	public Slice getSlice() {
		return mSlice;
	}
}
