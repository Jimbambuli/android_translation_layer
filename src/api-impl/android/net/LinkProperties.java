package android.net;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes the properties of a network link.
 *
 * The getters of AOSP (android-14). ATL has one network, the computer's: its link is described
 * by the addresses of the interfaces that are up (not loopback), see {@link #forHost()}.
 */
public final class LinkProperties implements android.os.Parcelable {
	private String mIfaceName;
	private final ArrayList<LinkAddress> mLinkAddresses = new ArrayList<>();
	private final ArrayList<RouteInfo> mRoutes = new ArrayList<>();
	private final ArrayList<InetAddress> mDnses = new ArrayList<>();
	private String mDomains;
	private int mMtu;
	private ProxyInfo mHttpProxy;

	public LinkProperties() {
	}

	/* AOSP LinkProperties(LinkProperties): a copy of the fields ATL keeps */
	public LinkProperties(LinkProperties source) {
		if (source != null) {
			mIfaceName = source.mIfaceName;
			mLinkAddresses.addAll(source.mLinkAddresses);
			mRoutes.addAll(source.mRoutes);
			mDnses.addAll(source.mDnses);
			mDomains = source.mDomains;
			mMtu = source.mMtu;
			mHttpProxy = source.mHttpProxy;
		}
	}

	/** ATL: the link of the computer's network (ConnectivityManager.getLinkProperties()) */
	static LinkProperties forHost() {
		LinkProperties lp = new LinkProperties();
		try {
			for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if (!ni.isUp() || ni.isLoopback())
					continue;
				for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
					try {
						lp.mLinkAddresses.add(new LinkAddress(ia.getAddress(), ia.getNetworkPrefixLength()));
					} catch (IllegalArgumentException e) {
						// not a unicast address
					}
				}
				if (lp.mIfaceName == null && !lp.mLinkAddresses.isEmpty()) {
					lp.mIfaceName = ni.getName();
					lp.mMtu = ni.getMTU();
				}
			}
		} catch (SocketException | NullPointerException e) {
			// no interfaces
		}
		return lp;
	}

	/**
	 * Gets the interface name for this link.
	 */
	public String getInterfaceName() {
		return mIfaceName;
	}

	/**
	 * Returns all the {@link LinkAddress} on this link.
	 */
	public List<LinkAddress> getLinkAddresses() {
		return Collections.unmodifiableList(mLinkAddresses);
	}

	/**
	 * Returns all the {@link InetAddress} for DNS servers on this link.
	 */
	/* AOSP LinkProperties.getRoutes(): the unmodifiable route list; ATL does not read the host's routing table */
	public List<RouteInfo> getRoutes() {
		return java.util.Collections.unmodifiableList(mRoutes);
	}

	public List<InetAddress> getDnsServers() {
		return Collections.unmodifiableList(mDnses);
	}

	/**
	 * Get the DNS domains search path set for this link.
	 */
	public String getDomains() {
		return mDomains;
	}

	/**
	 * Gets any non-default MTU size set for this link; 0 if not set.
	 */
	/* AOSP: true only with a private DNS (DNS over TLS) server set up; ATL leaves DNS to the desktop */
	public boolean isPrivateDnsActive() {
		return false;
	}

	public String getPrivateDnsServerName() {
		return null;
	}

	public int getMtu() {
		return mMtu;
	}

	/**
	 * Sets the recommended {@link ProxyInfo} to use on this link, or {@code null} for none.
	 */
	public void setHttpProxy(ProxyInfo proxy) {
		mHttpProxy = proxy;
	}

	/**
	 * Gets the recommended {@link ProxyInfo} (or {@code null}) set on this link.
	 * ATL: the host's link has none set; proxies are left to the desktop like DNS.
	 */
	public ProxyInfo getHttpProxy() {
		return mHttpProxy;
	}

	/* AOSP LinkProperties: a plain field, null until set */
	private java.net.Inet4Address mDhcpServerAddress;

	public void setDhcpServerAddress(java.net.Inet4Address serverAddress) {
		mDhcpServerAddress = serverAddress;
	}

	public java.net.Inet4Address getDhcpServerAddress() {
		return mDhcpServerAddress;
	}

	@Override
	public String toString() {
		return "{InterfaceName: " + mIfaceName + " LinkAddresses: " + mLinkAddresses + " MTU: " + mMtu + "}";
	}

	/* AOSP LinkProperties.describeContents()/writeToParcel()/CREATOR (Connectivity framework), with AOSP's
	 * writeAddress()/readAddress(). ATL keeps no validated private DNS servers, private DNS settings, P-CSCF
	 * servers, TCP buffer sizes, NAT64 prefix, stacked links, wake-on-LAN or captive portal data: written
	 * empty/null/false as an AOSP LinkProperties without them, skipped when read */
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeString(mIfaceName);
		dest.writeInt(mLinkAddresses.size());
		for (LinkAddress linkAddress : mLinkAddresses) {
			dest.writeParcelable(linkAddress, flags);
		}

		writeAddresses(dest, mDnses);
		dest.writeInt(0); // mValidatedPrivateDnses
		dest.writeBoolean(false); // mUsePrivateDns
		dest.writeString(null); // mPrivateDnsServerName
		dest.writeInt(0); // mPcscfs
		dest.writeString(mDomains);
		writeAddress(dest, mDhcpServerAddress);
		dest.writeInt(mMtu);
		dest.writeString(null); // mTcpBufferSizes
		dest.writeInt(mRoutes.size());
		for (RouteInfo route : mRoutes) {
			dest.writeParcelable(route, flags);
		}

		if (mHttpProxy != null) {
			dest.writeByte((byte)1);
			dest.writeParcelable(mHttpProxy, flags);
		} else {
			dest.writeByte((byte)0);
		}
		dest.writeParcelable(null, 0); // mNat64Prefix
		dest.writeList(new ArrayList<LinkProperties>()); // mStackedLinks
		dest.writeBoolean(false); // mWakeOnLanSupported
		dest.writeParcelable(null, 0); // mCaptivePortalApiUrl
		dest.writeParcelable(null, 0); // mCaptivePortalData
	}

	private static void writeAddresses(android.os.Parcel dest, java.util.List<InetAddress> list) {
		dest.writeInt(list.size());
		for (InetAddress d : list) {
			writeAddress(dest, d);
		}
	}

	private static void writeAddress(android.os.Parcel dest, InetAddress addr) {
		byte[] addressBytes = (addr == null ? null : addr.getAddress());
		dest.writeByteArray(addressBytes);
		if (addr instanceof java.net.Inet6Address) {
			final java.net.Inet6Address v6Addr = (java.net.Inet6Address)addr;
			final boolean hasScopeId = v6Addr.getScopeId() != 0;
			dest.writeBoolean(hasScopeId);
			if (hasScopeId)
				dest.writeInt(v6Addr.getScopeId());
		}
	}

	private static InetAddress readAddress(android.os.Parcel p) throws java.net.UnknownHostException {
		final byte[] addr = p.createByteArray();
		if (addr == null)
			return null;

		if (addr.length == 16) {
			final boolean hasScopeId = p.readBoolean();
			final int scopeId = hasScopeId ? p.readInt() : 0;
			return java.net.Inet6Address.getByAddress(null /* host */, addr, scopeId);
		}

		return InetAddress.getByAddress(addr);
	}

	public static final android.os.Parcelable.Creator<LinkProperties> CREATOR = new android.os.Parcelable.Creator<LinkProperties>() {
		public LinkProperties createFromParcel(android.os.Parcel in) {
			LinkProperties netProp = new LinkProperties();

			netProp.mIfaceName = in.readString();
			int addressCount = in.readInt();
			for (int i = 0; i < addressCount; i++) {
				netProp.mLinkAddresses.add((LinkAddress)in.readParcelable(null));
			}
			addressCount = in.readInt();
			for (int i = 0; i < addressCount; i++) {
				try {
					InetAddress dns = readAddress(in);
					if (dns != null)
						netProp.mDnses.add(dns);
				} catch (java.net.UnknownHostException e) {}
			}
			addressCount = in.readInt();
			for (int i = 0; i < addressCount; i++) {
				try {
					readAddress(in); // validated private DNS server
				} catch (java.net.UnknownHostException e) {}
			}
			in.readBoolean(); // usePrivateDns
			in.readString(); // privateDnsServerName
			addressCount = in.readInt();
			for (int i = 0; i < addressCount; i++) {
				try {
					readAddress(in); // P-CSCF server
				} catch (java.net.UnknownHostException e) {}
			}
			netProp.mDomains = in.readString();
			try {
				netProp.mDhcpServerAddress = (java.net.Inet4Address)InetAddress.getByAddress(in.createByteArray());
			} catch (java.net.UnknownHostException e) {}
			netProp.mMtu = in.readInt();
			in.readString(); // tcpBufferSizes
			addressCount = in.readInt();
			for (int i = 0; i < addressCount; i++) {
				netProp.mRoutes.add((RouteInfo)in.readParcelable(null));
			}
			if (in.readByte() == 1) {
				netProp.mHttpProxy = (ProxyInfo)in.readParcelable(null);
			}
			in.readParcelable(null); // nat64Prefix
			try {
				in.readList(new ArrayList<Object>(), LinkProperties.class.getClassLoader()); // stacked links
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			in.readBoolean(); // wakeOnLanSupported
			in.readParcelable(null); // captivePortalApiUrl
			in.readParcelable(null); // captivePortalData
			return netProp;
		}

		public LinkProperties[] newArray(int size) {
			return new LinkProperties[size];
		}
	};
}
