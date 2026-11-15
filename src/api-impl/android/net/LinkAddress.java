/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Objects;

/**
 * Identifies an IP address on a network link.
 *
 * The public API of AOSP (android-14, packages/modules/Connectivity); ATL fills the flags and
 * scope as AOSP does for an address without kernel flags.
 */
public class LinkAddress implements android.os.Parcelable {
	/* IFA_F_* and RT_SCOPE_* of linux/if_addr.h and linux/rtnetlink.h */
	private static final int RT_SCOPE_UNIVERSE = 0;
	private static final int RT_SCOPE_LINK = 253;
	private static final int RT_SCOPE_HOST = 254;
	private static final int RT_SCOPE_SITE = 200;

	private InetAddress address;
	private int prefixLength;
	private int flags;
	private int scope;

	/**
	 * Utility function to determines the scope of a unicast address. Per RFC 4291 section 2.5 and
	 * RFC 6724 section 3.2.
	 */
	private static int scopeForUnicastAddress(InetAddress addr) {
		if (addr.isAnyLocalAddress()) {
			return RT_SCOPE_HOST;
		}
		if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
			return RT_SCOPE_LINK;
		}
		// isSiteLocalAddress() returns true for private IPv4 addresses, but RFC 6724 section 3.2
		// says that they are assigned global scope.
		if (!(addr instanceof Inet4Address) && addr.isSiteLocalAddress()) {
			return RT_SCOPE_SITE;
		}
		return RT_SCOPE_UNIVERSE;
	}

	/**
	 * Constructs a new {@code LinkAddress} from an {@code InetAddress} and prefix length, with
	 * the specified flags and scope.
	 * @hide
	 */
	public LinkAddress(InetAddress address, int prefixLength, int flags, int scope) {
		if (address == null || address.isMulticastAddress() || prefixLength < 0 ||
		    (address instanceof Inet4Address && prefixLength > 32) ||
		    (prefixLength > 128)) {
			throw new IllegalArgumentException("Bad LinkAddress params " + address +
			                                   "/" + prefixLength);
		}
		this.address = address;
		this.prefixLength = prefixLength;
		this.flags = flags;
		this.scope = scope;
	}

	/**
	 * Constructs a new {@code LinkAddress} from an {@code InetAddress} and a prefix length.
	 * The flags are set to zero and the scope is determined from the address.
	 * @hide
	 */
	public LinkAddress(InetAddress address, int prefixLength) {
		this(address, prefixLength, 0, 0);
		this.scope = scopeForUnicastAddress(address);
	}

	/**
	 * Returns a string representation of this address, such as "192.0.2.1/24" or
	 * "2001:db8::1/64".
	 */
	@Override
	public String toString() {
		return address.getHostAddress() + "/" + prefixLength;
	}

	/**
	 * Compares this {@code LinkAddress} instance against {@code obj}.
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LinkAddress)) {
			return false;
		}
		LinkAddress linkAddress = (LinkAddress)obj;
		return this.address.equals(linkAddress.address) &&
		    this.prefixLength == linkAddress.prefixLength &&
		    this.flags == linkAddress.flags &&
		    this.scope == linkAddress.scope;
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, prefixLength, flags, scope);
	}

	/**
	 * Returns the {@link InetAddress} of this {@code LinkAddress}.
	 */
	public InetAddress getAddress() {
		return address;
	}

	/**
	 * Returns the prefix length of this {@code LinkAddress}.
	 */
	public int getPrefixLength() {
		return prefixLength;
	}

	/**
	 * Returns the flags of this {@code LinkAddress}.
	 */
	public int getFlags() {
		return flags;
	}

	/**
	 * Returns the scope of this {@code LinkAddress}.
	 */
	public int getScope() {
		return scope;
	}

	/**
	 * @return true if the address is IPv4 or is a mapped IPv4 address.
	 * @hide
	 */
	public boolean isIpv4() {
		return address instanceof Inet4Address;
	}

	/**
	 * @return true if the address is IPv6.
	 * @hide
	 */
	public boolean isIpv6() {
		return address instanceof Inet6Address;
	}

	/* AOSP LinkAddress.describeContents()/writeToParcel()/CREATOR (Connectivity framework). ATL keeps no
	 * deprecation/expiration time: written as LIFETIME_UNKNOWN (-1), ignored when read */
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeByteArray(address.getAddress());
		dest.writeInt(prefixLength);
		dest.writeInt(this.flags);
		dest.writeInt(scope);
		dest.writeLong(-1 /* deprecationTime: LIFETIME_UNKNOWN */);
		dest.writeLong(-1 /* expirationTime: LIFETIME_UNKNOWN */);
	}

	public static final android.os.Parcelable.Creator<LinkAddress> CREATOR = new android.os.Parcelable.Creator<LinkAddress>() {
		public LinkAddress createFromParcel(android.os.Parcel in) {
			InetAddress address = null;
			try {
				address = InetAddress.getByAddress(in.createByteArray());
			} catch (java.net.UnknownHostException e) {
				// Nothing we can do here. When we call the constructor, we'll throw an
				// IllegalArgumentException, because a LinkAddress can't have a null
				// InetAddress.
			}
			int prefixLength = in.readInt();
			int flags = in.readInt();
			int scope = in.readInt();
			in.readLong(); // deprecationTime
			in.readLong(); // expirationTime
			return new LinkAddress(address, prefixLength, flags, scope);
		}

		public LinkAddress[] newArray(int size) {
			return new LinkAddress[size];
		}
	};
}
