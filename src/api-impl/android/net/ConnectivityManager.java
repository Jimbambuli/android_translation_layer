package android.net;

import android.app.PendingIntent;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import java.util.ArrayList;

public class ConnectivityManager {
	public static final String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
	public static final String EXTRA_NO_CONNECTIVITY = "noConnectivity";
	public static final String EXTRA_REASON = "reason";
	public static final String EXTRA_IS_FAILOVER = "isFailover";
	public static final String EXTRA_NETWORK_INFO = "networkInfo";
	public static final String EXTRA_EXTRA_INFO = "extraInfo";
	public static final String EXTRA_NETWORK_TYPE = "networkType";
	public static final int TYPE_MOBILE = 0;
	public static final int TYPE_WIFI = 1;
	public static final int TYPE_ETHERNET = 9;
	public static final int TYPE_VPN = 17;
	/* AOSP ConnectivityManager: the remaining deprecated network types, TYPE_NONE and TYPE_TEST (hidden) */
	public static final int TYPE_BLUETOOTH = 7;
	public static final int TYPE_DUMMY = 8;
	public static final int TYPE_MOBILE_DUN = 4;
	public static final int TYPE_MOBILE_HIPRI = 5;
	public static final int TYPE_MOBILE_MMS = 2;
	public static final int TYPE_MOBILE_SUPL = 3;
	public static final int TYPE_WIMAX = 6;
	private static final int TYPE_NONE = -1;
	private static final int TYPE_TEST = 18;
	private static final int MAX_NETWORK_TYPE = TYPE_TEST;
	private static final int MIN_NETWORK_TYPE = TYPE_MOBILE;

	/* AOSP ConnectivityManager: the MULTIPATH_PREFERENCE_* bits of getMultipathPreference() */
	public static final int MULTIPATH_PREFERENCE_HANDOVER = 1 << 0;
	public static final int MULTIPATH_PREFERENCE_RELIABILITY = 1 << 1;
	public static final int MULTIPATH_PREFERENCE_PERFORMANCE = 1 << 2;
	static final int MULTIPATH_PREFERENCE_UNMETERED = MULTIPATH_PREFERENCE_HANDOVER | MULTIPATH_PREFERENCE_RELIABILITY | MULTIPATH_PREFERENCE_PERFORMANCE;

	/* AOSP ConnectivityService.sendGeneralBroadcast(): the sticky CONNECTIVITY_ACTION of the default network.
	 * Built on the app's main thread at registerReceiver() time, so it reads the last known state without
	 * waiting for the initial async network probe; a later network-changed re-sends the sticky (onNetworkChanged). */
	static Intent connectivityIntent() {
		boolean available = nativeGetNetworkAvailableCached();
		Intent intent = new Intent(CONNECTIVITY_ACTION);
		intent.putExtra(EXTRA_NETWORK_TYPE, available ? TYPE_WIFI : TYPE_MOBILE);
		intent.putExtra(EXTRA_NO_CONNECTIVITY, !available);
		intent.putExtra(EXTRA_IS_FAILOVER, false);
		intent.putExtra(EXTRA_NETWORK_INFO, new NetworkInfo(available));
		return intent;
	}

	/* AOSP: a receiver registered for CONNECTIVITY_ACTION gets the sticky broadcast at once */
	public static Intent stickyIntentFor(IntentFilter filter) {
		return filter != null && filter.matchAction(CONNECTIVITY_ACTION) ? connectivityIntent() : null;
	}

	// static like in AOSP: apps call the no-argument constructor of their subclasses
	public static class NetworkCallback {
		/* AOSP: FLAG_NONE (hidden) and FLAG_INCLUDE_LOCATION_INFO; ATL's network has no location info to redact */
		public static final int FLAG_NONE = 0;
		public static final int FLAG_INCLUDE_LOCATION_INFO = 1 << 0;

		public NetworkCallback() {
			this(FLAG_NONE);
		}

		public NetworkCallback(int flags) {}

		public void onAvailable(Network network) {}

		public void onLosing(Network network, int maxMsToLive) {}

		public void onLost(Network network) {}

		public void onUnavailable() {}

		public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {}

		public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {}

		public void onBlockedStatusChanged(Network network, boolean blocked) {}

		/* AOSP: called for a reservation (reserveNetwork) that was made */
		public void onReserved(NetworkCapabilities networkCapabilities) {}
	}

	/* AOSP ConnectivityManager.OnNetworkActiveListener */
	public interface OnNetworkActiveListener {
		void onNetworkActive();
	}

	/* the one network GNetworkMonitor reports on */
	private static final Network NETWORK = new Network();

	private static class Registration {
		final NetworkRequest request;
		final NetworkCallback callback;
		final PendingIntent operation;
		final Handler handler;
		boolean reported;
		boolean everReported; // for the timeout of requestNetwork(..., int)

		Registration(NetworkRequest request, NetworkCallback callback, PendingIntent operation, Handler handler) {
			this.request = request;
			this.callback = callback;
			this.operation = operation;
			this.handler = handler;
		}
	}

	private static final ArrayList<Registration> registrations = new ArrayList<>();
	private static Handler connectivityThreadHandler;
	private static boolean lastAvailable;

	/* AOSP ConnectivityManager.getRestrictBackgroundStatus() (API 24): the user's Data Saver; a desktop has none */
	public static final int RESTRICT_BACKGROUND_STATUS_DISABLED = 1;
	public static final int RESTRICT_BACKGROUND_STATUS_WHITELISTED = 2;
	public static final int RESTRICT_BACKGROUND_STATUS_ENABLED = 3;

	public int getRestrictBackgroundStatus() {
		return RESTRICT_BACKGROUND_STATUS_DISABLED;
	}

	public NetworkInfo getNetworkInfo(int networkType) {
		return new NetworkInfo(nativeGetNetworkAvailable());
	}

	public NetworkInfo getNetworkInfo(Network network) {
		// like AOSP: no network, no info
		return network != null ? getActiveNetworkInfo() : null;
	}

	public NetworkInfo getActiveNetworkInfo() {
		// like AOSP: null without a default network
		return nativeGetNetworkAvailable() ? new NetworkInfo(true) : null;
	}

	public native boolean isActiveNetworkMetered();

	protected static native boolean nativeGetNetworkAvailable();

	/* like nativeGetNetworkAvailable() but never waits for the initial async network probe: the sticky built
	 * on the main thread at registration must not block (microG's one-shot isConnected() keeps the blocking one) */
	protected static native boolean nativeGetNetworkAvailableCached();

	/* starts watching GNetworkMonitor; changes arrive in onNetworkChanged() */
	private static native void nativeStartMonitoring();

	public NetworkInfo[] getAllNetworkInfo() {
		NetworkInfo info = getActiveNetworkInfo();
		return info != null ? new NetworkInfo[] {info} : new NetworkInfo[0];
	}

	public Network getActiveNetwork() {
		return nativeGetNetworkAvailable() ? NETWORK : null;
	}

	public Network[] getAllNetworks() {
		Network network = getActiveNetwork();
		return network != null ? new Network[] {network} : new Network[0];
	}

	public NetworkCapabilities getNetworkCapabilities(Network network) {
		// as in getActiveNetworkInfo, a null network means there is none to report on
		if (network == null || !nativeGetNetworkAvailable())
			return null;

		NetworkCapabilities capabilities = new NetworkCapabilities();
		capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
		capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

		if (!isActiveNetworkMetered())
			capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);

		capabilities.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

		return capabilities;
	}

	public void registerNetworkCallback(NetworkRequest request, NetworkCallback callback) {
		register(request, callback, null, null);
	}

	public void registerNetworkCallback(NetworkRequest request, NetworkCallback callback, Handler handler) {
		register(request, callback, null, handler);
	}

	/* AOSP: a listen request (LISTEN_FOR_BEST) on the request's capabilities; ATL has one network, so it behaves
	 * like registerNetworkCallback() */
	public void registerBestMatchingNetworkCallback(NetworkRequest request, NetworkCallback callback, Handler handler) {
		register(request, callback, null, handler);
	}

	public void registerNetworkCallback(NetworkRequest request, PendingIntent operation) {
		register(request, null, operation, null);
	}

	/* AOSP ConnectivityManager.requestNetwork(): the callback follows the network that satisfies the
	 * request; ATL has only the host's network, so it is the network the request matches */
	public void requestNetwork(NetworkRequest request, NetworkCallback callback) {
		register(request, callback, null, null);
	}

	public void requestNetwork(NetworkRequest request, NetworkCallback callback, Handler handler) {
		register(request, callback, null, handler);
	}

	/* AOSP requestNetwork(NetworkRequest, PendingIntent): the intent is sent when a matching network is up */
	public void requestNetwork(NetworkRequest request, PendingIntent operation) {
		if (operation == null)
			throw new NullPointerException("PendingIntent cannot be null.");
		register(request, null, operation, null);
	}

	/* AOSP requestNetwork(NetworkRequest, NetworkCallback, int): checkTimeout(), then the request; when no matching
	 * network was found within the time, onUnavailable() follows and the request is released */
	public void requestNetwork(NetworkRequest request, NetworkCallback callback, int timeoutMs) {
		requestNetwork(request, callback, null, timeoutMs);
	}

	public void requestNetwork(NetworkRequest request, NetworkCallback callback, Handler handler, int timeoutMs) {
		if (timeoutMs <= 0) {
			throw new IllegalArgumentException("timeoutMs must be strictly positive.");
		}
		final Registration r = register(request, callback, null, handler);
		r.handler.postDelayed(new Runnable() {
			public void run() {
				synchronized (registrations) {
					if (r.everReported || !registrations.remove(r))
						return;
				}
				r.callback.onUnavailable();
			}
		}, timeoutMs);
	}

	/* AOSP reserveNetwork() (RESERVATION request): only an Ethernet provider can reserve a network, and ATL has
	 * none, so the system decides that the reservation can never be fulfilled: onUnavailable() */
	public void reserveNetwork(NetworkRequest request, Handler handler, final NetworkCallback networkCallback) {
		if (request == null || networkCallback == null)
			throw new NullPointerException();
		(handler != null ? handler : getConnectivityThreadHandler()).post(new Runnable() {
			public void run() {
				networkCallback.onUnavailable();
			}
		});
	}

	public void registerDefaultNetworkCallback(NetworkCallback callback) {
		register(null, callback, null, null);
	}

	public void registerDefaultNetworkCallback(NetworkCallback callback, Handler handler) {
		register(null, callback, null, handler);
	}

	public void unregisterNetworkCallback(NetworkCallback callback) {
		synchronized (registrations) {
			for (int i = registrations.size() - 1; i >= 0; i--)
				if (registrations.get(i).callback == callback)
					registrations.remove(i);
		}
	}

	public void unregisterNetworkCallback(PendingIntent operation) {
		synchronized (registrations) {
			for (int i = registrations.size() - 1; i >= 0; i--)
				if (registrations.get(i).operation != null && registrations.get(i).operation.equals(operation))
					registrations.remove(i);
		}
	}

	/* request null: the default network, which is the one network ATL knows */
	private Registration register(NetworkRequest request, NetworkCallback callback, PendingIntent operation, Handler handler) {
		if (callback == null && operation == null)
			throw new IllegalArgumentException("null NetworkCallback");
		nativeStartMonitoring();
		Registration r = new Registration(request, callback, operation, handler != null ? handler : getConnectivityThreadHandler());
		synchronized (registrations) {
			registrations.add(r);
			lastAvailable = nativeGetNetworkAvailable();
			// like AOSP: a matching network that is already up is reported right away
			if (lastAvailable)
				notify(r, true);
		}
		return r;
	}

	private static Handler getConnectivityThreadHandler() {
		synchronized (registrations) {
			if (connectivityThreadHandler == null) {
				HandlerThread thread = new HandlerThread("ConnectivityThread");
				thread.start();
				connectivityThreadHandler = new Handler(thread.getLooper());
			}
			return connectivityThreadHandler;
		}
	}

	private static void notify(final Registration r, final boolean available) {
		// like AOSP: only a network that satisfies the request is reported, and lost only after that
		if (available) {
			if (r.request != null && !r.request.canBeSatisfiedBy(new ConnectivityManager().getNetworkCapabilities(NETWORK)))
				return;
		} else if (!r.reported) {
			return;
		}
		r.reported = available;
		r.everReported |= available;
		if (r.operation != null) {
			if (available) {
				r.handler.post(new Runnable() {
					public void run() {
						r.operation.send();
					}
				});
			}
			return;
		}
		final NetworkCallback cb = r.callback;
		r.handler.post(new Runnable() {
			public void run() {
				if (!available) {
					cb.onLost(NETWORK);
					return;
				}
				// same order as AOSP's CallbackHandler for a new network
				cb.onAvailable(NETWORK);
				NetworkCapabilities caps = new ConnectivityManager().getNetworkCapabilities(NETWORK);
				if (caps != null)
					cb.onCapabilitiesChanged(NETWORK, caps);
				cb.onLinkPropertiesChanged(NETWORK, LinkProperties.forHost());
				cb.onBlockedStatusChanged(NETWORK, false);
			}
		});
	}

	@Export
	private static void onNetworkChanged(boolean available) {
		synchronized (registrations) {
			if (available == lastAvailable)
				return;
			lastAvailable = available;
			for (Registration r : registrations)
				notify(r, available);
		}
		Context.sendBroadcastToReceivers(connectivityIntent());
	}

	/* AOSP releaseNetworkRequest(PendingIntent): the same as unregisterNetworkCallback(PendingIntent), null refused */
	public void releaseNetworkRequest(PendingIntent operation) {
		if (operation == null)
			throw new NullPointerException("PendingIntent cannot be null.");
		unregisterNetworkCallback(operation);
	}

	public ProxyInfo getDefaultProxy() { return null; }

	/* AOSP: the link of the given network; ATL has one, the computer's */
	public LinkProperties getLinkProperties(Network network) {
		return network != null ? LinkProperties.forHost() : null;
	}

	/* AOSP: the network the process is bound to, null when none is; ATL has one network, so binding changes
	 * nothing but is remembered for getBoundNetworkForProcess() */
	private static volatile Network boundNetwork;

	public boolean bindProcessToNetwork(Network network) {
		boundNetwork = network;
		return true;
	}

	@Deprecated
	public static boolean setProcessDefaultNetwork(Network network) {
		boundNetwork = network;
		return true;
	}

	public Network getBoundNetworkForProcess() {
		return getProcessDefaultNetwork();
	}

	@Deprecated
	public static Network getProcessDefaultNetwork() {
		return boundNetwork;
	}

	/* the connectivity service evaluates the report; nothing to do without one */
	public void reportNetworkConnectivity(Network network, boolean hasConnectivity) {}

	/* AOSP reportBadNetwork(): reportNetworkConnectivity(network, true) and then (network, false) */
	@Deprecated
	public void reportBadNetwork(Network network) {
		reportNetworkConnectivity(network, true);
		reportNetworkConnectivity(network, false);
	}

	/* AOSP: "assume that background data is allowed" */
	@Deprecated
	public boolean getBackgroundDataSetting() {
		return true;
	}

	@Deprecated
	public void setBackgroundDataSetting(boolean allowBackgroundData) {}

	/* ConnectivityService looks the socket up in the kernel; no owner found is Process.INVALID_UID */
	public int getConnectionOwnerUid(int protocol, java.net.InetSocketAddress local, java.net.InetSocketAddress remote) {
		return -1;
	}

	/* AOSP ConnectivityManager: removed functionality, TYPE_NONE and a no-op setter */
	@Deprecated
	public void setNetworkPreference(int preference) {}

	@Deprecated
	public int getNetworkPreference() {
		return TYPE_NONE;
	}

	@Deprecated
	public static boolean isNetworkTypeValid(int networkType) {
		return MIN_NETWORK_TYPE <= networkType && networkType <= MAX_NETWORK_TYPE;
	}

	/* ConnectivityService.requestBandwidthUpdate(): true when the network is up (the service polls its
	 * bandwidth), false for a network that is no longer valid */
	public boolean requestBandwidthUpdate(Network network) {
		return network != null && nativeGetNetworkAvailable();
	}

	/* ConnectivityService.getMultipathPreference(): an unmetered network gets all bits; for a metered one
	 * NetworkPolicyManager has no preference and config_networkMeteredMultipathPreference is 0 */
	public int getMultipathPreference(Network network) {
		if (network != null && nativeGetNetworkAvailable() && !isActiveNetworkMetered())
			return MULTIPATH_PREFERENCE_UNMETERED;
		return 0;
	}

	/* ConnectivityService.getNetworkWatchlistConfigHash(): null, there is no network watchlist config */
	public byte[] getNetworkWatchlistConfigHash() {
		return null;
	}

	/* AOSP: the data network of a desktop is not a radio with a power state, so it is always active
	 * (NetworkActivityTracker.isDefaultNetworkActive()); the listeners are never called */
	private final java.util.ArrayList<OnNetworkActiveListener> networkActiveListeners = new java.util.ArrayList<>();

	public void addDefaultNetworkActiveListener(OnNetworkActiveListener l) {
		synchronized (networkActiveListeners) {
			networkActiveListeners.add(l);
		}
	}

	public void removeDefaultNetworkActiveListener(OnNetworkActiveListener l) {
		synchronized (networkActiveListeners) {
			if (!networkActiveListeners.remove(l))
				throw new IllegalArgumentException("Listener was not registered.");
		}
	}

	public boolean isDefaultNetworkActive() {
		return true;
	}
}
