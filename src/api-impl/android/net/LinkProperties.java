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
public class LinkProperties {
	private String mIfaceName;
	private final ArrayList<LinkAddress> mLinkAddresses = new ArrayList<>();
	private final ArrayList<InetAddress> mDnses = new ArrayList<>();
	private String mDomains;
	private int mMtu;

	public LinkProperties() {
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

	@Override
	public String toString() {
		return "{InterfaceName: " + mIfaceName + " LinkAddresses: " + mLinkAddresses + " MTU: " + mMtu + "}";
	}
}
