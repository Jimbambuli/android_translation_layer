/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.net;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Objects;

/**
 * A {@link NetworkSpecifier} used to identify ethernet interfaces.
 *
 * AOSP modules_Connectivity framework-t/src/android/net/EthernetNetworkSpecifier.java without the hidden
 * MAC address (constructor and getter are not in the public API).
 */
public final class EthernetNetworkSpecifier extends NetworkSpecifier implements Parcelable {
	@Nullable private final String mInterfaceName;

	public EthernetNetworkSpecifier(@NonNull String interfaceName) {
		if (TextUtils.isEmpty(interfaceName)) {
			throw new IllegalArgumentException("Interface name cannot be null");
		}
		mInterfaceName = interfaceName;
	}

	@Nullable
	public String getInterfaceName() {
		return mInterfaceName;
	}

	@Override
	public boolean canBeSatisfiedBy(@Nullable NetworkSpecifier other) {
		if (!(other instanceof EthernetNetworkSpecifier))
			return false;
		final EthernetNetworkSpecifier rhs = (EthernetNetworkSpecifier)other;
		// If interface name is specified, match interface name.
		return mInterfaceName == null || mInterfaceName.equals(rhs.mInterfaceName);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof EthernetNetworkSpecifier))
			return false;
		EthernetNetworkSpecifier rhs = (EthernetNetworkSpecifier)o;
		return TextUtils.equals(mInterfaceName, rhs.mInterfaceName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(null, mInterfaceName);
	}

	@Override
	public String toString() {
		return "EthernetNetworkSpecifier ( interface name: " + mInterfaceName + ", MAC address: " + Objects.toString(null) + ")";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeString(mInterfaceName);
		dest.writeParcelable(null, flags);
	}

	public static final @NonNull Parcelable.Creator<EthernetNetworkSpecifier> CREATOR = new Parcelable.Creator<EthernetNetworkSpecifier>() {
		public EthernetNetworkSpecifier createFromParcel(Parcel in) {
			final String ifname = in.readString();
			in.readString(); // class name of the (absent) MAC address
			return new EthernetNetworkSpecifier(ifname);
		}

		public EthernetNetworkSpecifier[] newArray(int size) {
			return new EthernetNetworkSpecifier[size];
		}
	};
}
