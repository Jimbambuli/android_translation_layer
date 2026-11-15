package android.net;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* AOSP frameworks/base core/java/android/net/VpnService.java. The VPN management service (IVpnManager) and
 * VpnConfig do not exist in ATL. Behaviour as for an app that was never granted the VPN consent, which is the
 * state the AOSP client code has without a VPN: prepare() returns VpnConfig.getIntentForConfirmation() (the
 * com.android.vpndialogs ConfirmDialog, config_customVpnConfirmDialogComponent), Builder.establish() returns null
 * ("null if the application is not prepared"); the calls that only act on an established VPN return false.
 * The Builder's checks and its route/address lists are AOSP's, the VpnConfig fields it fills are dropped. */
public class VpnService extends Service {

	public static final String SERVICE_INTERFACE = "android.net.VpnService";
	public static final String SERVICE_META_DATA_SUPPORTS_ALWAYS_ON = "android.net.VpnService.SUPPORTS_ALWAYS_ON";

	public static Intent prepare(Context context) {
		Intent intent = new Intent();
		intent.setClassName("com.android.vpndialogs", "com.android.vpndialogs.ConfirmDialog");
		return intent;
	}

	/* No VPN of ours to protect a socket from: nothing routes into a tunnel. */
	public boolean protect(int socket) {
		return true;
	}

	public boolean protect(Socket socket) {
		return true;
	}

	public boolean protect(DatagramSocket socket) {
		return true;
	}

	public boolean addAddress(InetAddress address, int prefixLength) {
		check(address, prefixLength);
		return false;
	}

	public boolean removeAddress(InetAddress address, int prefixLength) {
		check(address, prefixLength);
		return false;
	}

	public boolean setUnderlyingNetworks(Network[] networks) {
		return false;
	}

	public final boolean isAlwaysOn() {
		return false;
	}

	public final boolean isLockdownEnabled() {
		return false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (intent != null && SERVICE_INTERFACE.equals(intent.getAction())) {
			return new Callback();
		}
		return null;
	}

	public void onRevoke() {
		stopSelf();
	}

	private class Callback extends Binder {
		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
			if (code == IBinder.LAST_CALL_TRANSACTION) {
				onRevoke();
				return true;
			}
			return false;
		}
	}

	private static void check(InetAddress address, int prefixLength) {
		if (address.isLoopbackAddress()) {
			throw new IllegalArgumentException("Bad address");
		}
		if (address instanceof Inet4Address) {
			if (prefixLength < 0 || prefixLength > 32) {
				throw new IllegalArgumentException("Bad prefixLength");
			}
		} else if (address instanceof Inet6Address) {
			if (prefixLength < 0 || prefixLength > 128) {
				throw new IllegalArgumentException("Bad prefixLength");
			}
		} else {
			throw new IllegalArgumentException("Unsupported family");
		}
	}

	private static void checkNonPrefixBytes(InetAddress address, int prefixLength) {
		final IpPrefix prefix = new IpPrefix(address, prefixLength);
		if (!prefix.getAddress().equals(address)) {
			throw new IllegalArgumentException("Bad address");
		}
	}

	public class Builder {
		private final List<LinkAddress> mAddresses = new ArrayList<>();
		private final List<RouteInfo> mRoutes = new ArrayList<>();
		private boolean mHasDisallowed;
		private boolean mHasAllowed;

		public Builder() {}

		public Builder setSession(String session) {
			return this;
		}

		public Builder setConfigureIntent(PendingIntent intent) {
			return this;
		}

		public Builder setMtu(int mtu) {
			if (mtu <= 0) {
				throw new IllegalArgumentException("Bad mtu");
			}
			return this;
		}

		public Builder setHttpProxy(ProxyInfo proxyInfo) {
			return this;
		}

		public Builder addAddress(InetAddress address, int prefixLength) {
			check(address, prefixLength);
			if (address.isAnyLocalAddress()) {
				throw new IllegalArgumentException("Bad address");
			}
			mAddresses.add(new LinkAddress(address, prefixLength));
			return this;
		}

		public Builder addAddress(String address, int prefixLength) {
			return addAddress(InetAddresses.parseNumericAddress(address), prefixLength);
		}

		private Builder addRoute(IpPrefix prefix, int type) {
			check(prefix.getAddress(), prefix.getPrefixLength());
			final RouteInfo newRoute = new RouteInfo(prefix, /* gateway */ null, /* interface */ null, type);
			final int index = findRouteIndexByDestination(newRoute);
			if (index == -1) {
				mRoutes.add(newRoute);
			} else {
				mRoutes.set(index, newRoute);
			}
			return this;
		}

		public Builder addRoute(InetAddress address, int prefixLength) {
			checkNonPrefixBytes(address, prefixLength);
			return addRoute(new IpPrefix(address, prefixLength), RouteInfo.RTN_UNICAST);
		}

		public Builder addRoute(IpPrefix prefix) {
			return addRoute(prefix, RouteInfo.RTN_UNICAST);
		}

		public Builder addRoute(String address, int prefixLength) {
			return addRoute(InetAddresses.parseNumericAddress(address), prefixLength);
		}

		public Builder excludeRoute(IpPrefix prefix) {
			return addRoute(prefix, RouteInfo.RTN_THROW);
		}

		public Builder addDnsServer(InetAddress address) {
			if (address.isLoopbackAddress() || address.isAnyLocalAddress()) {
				throw new IllegalArgumentException("Bad address");
			}
			return this;
		}

		public Builder addDnsServer(String address) {
			return addDnsServer(InetAddresses.parseNumericAddress(address));
		}

		public Builder addSearchDomain(String domain) {
			return this;
		}

		public Builder allowFamily(int family) {
			if (family != AF_INET && family != AF_INET6) {
				throw new IllegalArgumentException(family + " is neither " + AF_INET + " nor " + AF_INET6);
			}
			return this;
		}

		private void verifyApp(String packageName) throws PackageManager.NameNotFoundException {
			VpnService.this.getPackageManager().getApplicationInfo(packageName, 0);
		}

		public Builder addAllowedApplication(String packageName) throws PackageManager.NameNotFoundException {
			if (mHasDisallowed) {
				throw new UnsupportedOperationException("addDisallowedApplication already called");
			}
			verifyApp(packageName);
			mHasAllowed = true;
			return this;
		}

		public Builder addDisallowedApplication(String packageName) throws PackageManager.NameNotFoundException {
			if (mHasAllowed) {
				throw new UnsupportedOperationException("addAllowedApplication already called");
			}
			verifyApp(packageName);
			mHasDisallowed = true;
			return this;
		}

		public Builder allowBypass() {
			return this;
		}

		public Builder setBlocking(boolean blocking) {
			return this;
		}

		public Builder setUnderlyingNetworks(Network[] networks) {
			return this;
		}

		public Builder setMetered(boolean isMetered) {
			return this;
		}

		/* null: the application is not prepared (no consent was given) */
		public ParcelFileDescriptor establish() {
			return null;
		}

		private int findRouteIndexByDestination(RouteInfo route) {
			for (int i = 0; i < mRoutes.size(); i++) {
				if (mRoutes.get(i).getDestination().equals(route.getDestination())) {
					return i;
				}
			}
			return -1;
		}

		public List<RouteInfo> routes() {
			return Collections.unmodifiableList(mRoutes);
		}
	}
}
