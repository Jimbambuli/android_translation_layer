package android.net;

import android.atl.annotation.Export;
import android.os.Handler;

class ProxyInfo {}

public class ConnectivityManager {

	/* AOSP ConnectivityManager.NetworkCallback (static, as apps subclass it without an outer instance) */
	public static class NetworkCallback {
		public void onPreCheck(Network network) {}

		public void onAvailable(Network network, NetworkCapabilities networkCapabilities, LinkProperties linkProperties, boolean blocked) {
			onAvailable(network);
			onCapabilitiesChanged(network, networkCapabilities);
			onLinkPropertiesChanged(network, linkProperties);
			onBlockedStatusChanged(network, blocked);
		}

		public void onAvailable(Network network) {}

		public void onLosing(Network network, int maxMsToLive) {}

		public void onLost(Network network) {}

		public void onUnavailable() {}

		public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {}

		public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {}

		public void onBlockedStatusChanged(Network network, boolean blocked) {}
	}

	/* the registered callbacks with their request (null: the default network) and handler. On AOSP the
	 * ConnectivityService calls a new callback at once with the current network (onAvailable,
	 * onCapabilitiesChanged, ...) if one satisfies the request, and again on each change; ATL has the one
	 * network GNetworkMonitor reports */
	private static final class Registration {
		final NetworkRequest request;
		final NetworkCallback callback;
		final Handler handler;
		boolean available;

		Registration(NetworkRequest request, NetworkCallback callback, Handler handler) {
			this.request = request;
			this.callback = callback;
			this.handler = handler;
		}
	}

	private static final java.util.List<Registration> registrations = new java.util.ArrayList<>();
	private static boolean watching;

	private static native void nativeWatchNetwork();
	private static native boolean nativeNetworkAvailable();
	private static native boolean nativeNetworkMetered();

	private static NetworkCapabilities currentCapabilities() {
		if (!nativeNetworkAvailable())
			return null;
		NetworkCapabilities capabilities = new NetworkCapabilities();
		capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
		capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
		capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING);
		capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED);
		capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED);
		capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND);
		if (!nativeNetworkMetered())
			capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
		capabilities.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
		return capabilities;
	}

	private static void update(final Registration r) {
		final NetworkCapabilities capabilities = currentCapabilities();
		final boolean satisfied = capabilities != null && (r.request == null || r.request.canBeSatisfiedBy(capabilities));
		final Network network = new Network();
		r.handler.post(new Runnable() {
			@Override
			public void run() {
				if (satisfied) {
					if (!r.available)
						r.callback.onAvailable(network, capabilities, null, false);
					else
						r.callback.onCapabilitiesChanged(network, capabilities);
				} else if (r.available) {
					r.callback.onLost(network);
				}
				r.available = satisfied;
			}
		});
	}

	@Export
	private static void networkChanged() {
		synchronized (registrations) {
			for (Registration r : registrations)
				update(r);
		}
	}

	private void register(NetworkRequest request, NetworkCallback callback, Handler handler) {
		Registration r = new Registration(request, callback, handler != null ? handler : new Handler(android.os.Looper.getMainLooper()));
		synchronized (registrations) {
			if (!watching) {
				nativeWatchNetwork();
				watching = true;
			}
			registrations.add(r);
		}
		update(r);
	}

	public NetworkInfo getNetworkInfo(int networkType) {
		return new NetworkInfo(nativeGetNetworkAvailable());
	}

	public NetworkInfo getNetworkInfo(Network network) {
		// like AOSP: no network, no info
		return network != null ? getActiveNetworkInfo() : null;
	}

	public NetworkInfo getActiveNetworkInfo() {
		return new NetworkInfo(nativeGetNetworkAvailable());
	}

	public void registerNetworkCallback(NetworkRequest request, NetworkCallback callback) {
		register(request, callback, null);
	}

	public void registerNetworkCallback(NetworkRequest request, NetworkCallback callback, Handler handler) {
		register(request, callback, handler);
	}

	public void requestNetwork(NetworkRequest request, NetworkCallback callback) {
		register(request, callback, null);
	}

	public void requestNetwork(NetworkRequest request, NetworkCallback callback, Handler handler) {
		register(request, callback, handler);
	}

	public void unregisterNetworkCallback(NetworkCallback callback) {
		synchronized (registrations) {
			for (int i = registrations.size() - 1; i >= 0; i--)
				if (registrations.get(i).callback == callback)
					registrations.remove(i);
		}
	}

	public boolean isActiveNetworkMetered() {
		return nativeNetworkMetered();
	}

	protected boolean nativeGetNetworkAvailable() {
		return nativeNetworkAvailable();
	}

	public NetworkInfo[] getAllNetworkInfo() {
		return new NetworkInfo[] {getActiveNetworkInfo()};
	}

	public Network getActiveNetwork() {
		return new Network();
	}

	public Network[] getAllNetworks() {
		return new Network[] {getActiveNetwork()};
	}

	public NetworkCapabilities getNetworkCapabilities(Network network) {
		// as in getActiveNetworkInfo, a null network means there is none to report on
		if (network == null || !nativeGetNetworkAvailable())
			return null;

		NetworkCapabilities capabilities = new NetworkCapabilities();
		capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

		if (!isActiveNetworkMetered())
			capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);

		capabilities.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

		return capabilities;
	}

	public void registerDefaultNetworkCallback(NetworkCallback cb, Handler hdl) {
		register(null, cb, hdl);
	}

	public void registerDefaultNetworkCallback(NetworkCallback cb) {
		register(null, cb, null);
	}

	public ProxyInfo getDefaultProxy() { return null; }

	public LinkProperties getLinkProperties(Network network) { return null; }
}
