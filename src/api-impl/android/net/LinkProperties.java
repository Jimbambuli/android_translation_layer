package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
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
	private IpPrefix mNat64Prefix;
	private boolean mWakeOnLanSupported;

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
			mNat64Prefix = source.mNat64Prefix;
			mWakeOnLanSupported = source.mWakeOnLanSupported;
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

	/* AOSP LinkProperties.setInterfaceName(): the routes already set take the new interface name */
	public void setInterfaceName(String iface) {
		mIfaceName = iface;
		for (int i = 0; i < mRoutes.size(); i++)
			mRoutes.set(i, routeWithInterface(mRoutes.get(i)));
	}

	/* AOSP LinkProperties.routeWithInterface() */
	private RouteInfo routeWithInterface(RouteInfo route) {
		return new RouteInfo(route.getDestination(), route.getGateway(), mIfaceName, route.getType(), route.getMtu());
	}

	/* AOSP LinkProperties.findRouteIndexByRouteKey() */
	private int findRouteIndexByRouteKey(RouteInfo route) {
		for (int i = 0; i < mRoutes.size(); i++) {
			if (mRoutes.get(i).getRouteKey().equals(route.getRouteKey()))
				return i;
		}
		return -1;
	}

	/* AOSP LinkProperties.addRoute(): added or, for the same route key, updated */
	public boolean addRoute(RouteInfo route) {
		String routeIface = route.getInterface();
		if (routeIface != null && !routeIface.equals(mIfaceName)) {
			throw new IllegalArgumentException("Route added with non-matching interface: " + routeIface + " vs. " + mIfaceName);
		}
		route = routeWithInterface(route);

		int i = findRouteIndexByRouteKey(route);
		if (i == -1) {
			mRoutes.add(route);
			return true;
		} else if (mRoutes.get(i).equals(route)) {
			return false;
		} else {
			mRoutes.set(i, route);
			return true;
		}
	}

	/* AOSP LinkProperties.addLinkAddress(): an address with the same address and prefix is replaced */
	boolean addLinkAddress(LinkAddress address) {
		if (address == null)
			return false;
		int i = -1;
		for (int j = 0; j < mLinkAddresses.size(); j++) {
			LinkAddress l = mLinkAddresses.get(j);
			if (l.getAddress().equals(address.getAddress()) && l.getPrefixLength() == address.getPrefixLength()) {
				i = j;
				break;
			}
		}
		if (i < 0) {
			mLinkAddresses.add(address);
			return true;
		} else if (mLinkAddresses.get(i).equals(address)) {
			return false;
		} else {
			mLinkAddresses.set(i, address);
			return true;
		}
	}

	/* AOSP LinkProperties.setLinkAddresses() */
	public void setLinkAddresses(java.util.Collection<LinkAddress> addresses) {
		mLinkAddresses.clear();
		for (LinkAddress address : addresses)
			addLinkAddress(address);
	}

	/* AOSP LinkProperties.addDnsServer(): not added twice */
	boolean addDnsServer(InetAddress dnsServer) {
		if (dnsServer != null && !mDnses.contains(dnsServer)) {
			mDnses.add(dnsServer);
			return true;
		}
		return false;
	}

	/* AOSP LinkProperties.setDnsServers() */
	public void setDnsServers(java.util.Collection<InetAddress> dnsServers) {
		mDnses.clear();
		for (InetAddress dnsServer : dnsServers)
			addDnsServer(dnsServer);
	}

	public void setDomains(String domains) {
		mDomains = domains;
	}

	public void setMtu(int mtu) {
		mMtu = mtu;
	}

	public IpPrefix getNat64Prefix() {
		return mNat64Prefix;
	}

	/* AOSP LinkProperties.setNat64Prefix(): only 96-bit prefixes */
	public void setNat64Prefix(IpPrefix prefix) {
		if (prefix != null && prefix.getPrefixLength() != 96) {
			throw new IllegalArgumentException("Only 96-bit prefixes are supported: " + prefix);
		}
		mNat64Prefix = prefix;
	}

	/* AOSP: false unless the link sets it; ATL does not ask the interface */
	public boolean isWakeOnLanSupported() {
		return mWakeOnLanSupported;
	}

	/* AOSP LinkProperties.clear(): back to the initial state */
	public void clear() {
		mIfaceName = null;
		mLinkAddresses.clear();
		mDnses.clear();
		mDomains = null;
		mRoutes.clear();
		mHttpProxy = null;
		mMtu = 0;
		mDhcpServerAddress = null;
		mNat64Prefix = null;
		mWakeOnLanSupported = false;
	}

	public int describeContents() {
		return 0;
	}

	/* AOSP LinkProperties.writeToParcel(), field by field in AOSP's order; the fields ATL does not keep (validated
	 * private DNS servers, P-CSCF, TCP buffer sizes, stacked links, captive portal) are written empty */
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mIfaceName);
		dest.writeInt(mLinkAddresses.size());
		for (LinkAddress linkAddress : mLinkAddresses)
			dest.writeParcelable(linkAddress, flags);

		writeAddresses(dest, mDnses);
		dest.writeInt(0);
		dest.writeBoolean(false);
		dest.writeString(null);
		dest.writeInt(0);
		dest.writeString(mDomains);
		writeAddress(dest, mDhcpServerAddress);
		dest.writeInt(mMtu);
		dest.writeString(null);
		dest.writeInt(mRoutes.size());
		for (RouteInfo route : mRoutes)
			dest.writeParcelable(route, flags);

		if (mHttpProxy != null) {
			dest.writeByte((byte)1);
			dest.writeParcelable(mHttpProxy, flags);
		} else {
			dest.writeByte((byte)0);
		}
		dest.writeParcelable(mNat64Prefix, 0);

		dest.writeList(new ArrayList<LinkProperties>());

		dest.writeBoolean(mWakeOnLanSupported);
		dest.writeParcelable(null, 0);
		dest.writeParcelable(null, 0);
	}

	private static void writeAddresses(Parcel dest, List<InetAddress> list) {
		dest.writeInt(list.size());
		for (InetAddress d : list)
			writeAddress(dest, d);
	}

	private static void writeAddress(Parcel dest, InetAddress addr) {
		byte[] addressBytes = (addr == null ? null : addr.getAddress());
		dest.writeByteArray(addressBytes);
		if (addr instanceof Inet6Address) {
			final Inet6Address v6Addr = (Inet6Address)addr;
			final boolean hasScopeId = v6Addr.getScopeId() != 0;
			dest.writeBoolean(hasScopeId);
			if (hasScopeId)
				dest.writeInt(v6Addr.getScopeId());
		}
	}

	private static InetAddress readAddress(Parcel p) throws UnknownHostException {
		final byte[] addr = p.createByteArray();
		if (addr == null)
			return null;

		if (addr.length == 16) {
			final boolean hasScopeId = p.readBoolean();
			final int scopeId = hasScopeId ? p.readInt() : 0;
			return Inet6Address.getByAddress(null, addr, scopeId);
		}

		return InetAddress.getByAddress(addr);
	}

	public static final Creator<LinkProperties> CREATOR = new Creator<LinkProperties>() {
		public LinkProperties createFromParcel(Parcel in) {
			ClassLoader loader = LinkProperties.class.getClassLoader();
			LinkProperties netProp = new LinkProperties();

			String iface = in.readString();
			if (iface != null)
				netProp.setInterfaceName(iface);
			int addressCount = in.readInt();
			for (int i = 0; i < addressCount; i++)
				netProp.addLinkAddress((LinkAddress)in.readParcelable(loader));
			addressCount = in.readInt();
			for (int i = 0; i < addressCount; i++) {
				try {
					netProp.addDnsServer(readAddress(in));
				} catch (UnknownHostException e) {
				}
			}
			addressCount = in.readInt();
			for (int i = 0; i < addressCount; i++) {
				try {
					readAddress(in);
				} catch (UnknownHostException e) {
				}
			}
			in.readBoolean();
			in.readString();
			addressCount = in.readInt();
			for (int i = 0; i < addressCount; i++) {
				try {
					readAddress(in);
				} catch (UnknownHostException e) {
				}
			}
			netProp.setDomains(in.readString());
			try {
				netProp.setDhcpServerAddress((Inet4Address)InetAddress.getByAddress(in.createByteArray()));
			} catch (UnknownHostException e) {
			}
			netProp.setMtu(in.readInt());
			in.readString();
			addressCount = in.readInt();
			for (int i = 0; i < addressCount; i++)
				netProp.addRoute((RouteInfo)in.readParcelable(loader));
			if (in.readByte() == 1)
				netProp.setHttpProxy((ProxyInfo)in.readParcelable(loader));
			netProp.setNat64Prefix((IpPrefix)in.readParcelable(loader));
			try {
				in.readList(new ArrayList<Object>(), loader);
			} catch (ReflectiveOperationException e) {
			}
			netProp.mWakeOnLanSupported = in.readBoolean();
			in.readParcelable(loader);
			in.readParcelable(loader);
			return netProp;
		}

		public LinkProperties[] newArray(int size) {
			return new LinkProperties[size];
		}
	};

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

}
