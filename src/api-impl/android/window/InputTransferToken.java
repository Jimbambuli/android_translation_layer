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

package android.window;

import android.annotation.NonNull;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/*
 * AOSP android16 frameworks/base/core/java/android/window/InputTransferToken.java. AOSP keeps the token in a
 * native object; ATL has no input transfer, so the binder token is held directly (same equals/hashCode meaning).
 */
public final class InputTransferToken implements Parcelable {
	private final IBinder mToken;

	public InputTransferToken(@NonNull IBinder token) {
		mToken = Objects.requireNonNull(token);
	}

	public InputTransferToken() {
		this(new Binder());
	}

	public IBinder getToken() {
		return mToken;
	}

	private InputTransferToken(Parcel in) {
		this(in.readStrongBinder());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeStrongBinder(mToken);
	}

	public static final @NonNull Creator<InputTransferToken> CREATOR = new Creator<>() {
		public InputTransferToken createFromParcel(Parcel in) {
			return new InputTransferToken(in);
		}

		public InputTransferToken[] newArray(int size) {
			return new InputTransferToken[size];
		}
	};

	@Override
	public int hashCode() {
		return Objects.hashCode(mToken);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		return mToken.equals(((InputTransferToken) obj).mToken);
	}
}
