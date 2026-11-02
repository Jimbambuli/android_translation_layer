package android.net;

import android.app.PendingIntent;
import android.atl.annotation.Export;
import android.os.Handler;
import android.os.HandlerThread;
import java.util.ArrayList;

class ProxyInfo {}

public class ConnectivityManager {

	// static like in AOSP: apps call the no-argument constructor of their subclasses
	public static class NetworkCallback {
		public void onAvailable(Network network) {}

		public void onLosing(Network network, int maxMsToLive) {}

		public void onLost(Network network) {}

		public void onUnavailable() {}

		public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {}

		public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {}

		public void onBlockedStatusChanged(Network network, boolean blocked) {}
	}

	/* the one network GNetworkMonitor reports on */
	private static final Network NETWORK = new Network();

	private static class Registration {
		final NetworkCallback callback;
		final PendingIntent operation;
		final Handler handler;

		Registration(NetworkCallback callback, PendingIntent operation, Handler handler) {
			this.callback = callback;
			this.operation = operation;
			this.handler = handler;
		}
	}

	private static final ArrayList<Registration> registrations = new ArrayList<>();
	private static Handler connectivityThreadHandler;
	private static boolean lastAvailable;

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
		register(callback, null, null);
	}

	public void registerNetworkCallback(NetworkRequest request, NetworkCallback callback, Handler handler) {
		register(callback, null, handler);
	}

	public void registerNetworkCallback(NetworkRequest request, PendingIntent operation) {
		register(null, operation, null);
	}

	public void registerDefaultNetworkCallback(NetworkCallback callback) {
		register(callback, null, null);
	}

	public void registerDefaultNetworkCallback(NetworkCallback callback, Handler handler) {
		register(callback, null, handler);
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

	private void register(NetworkCallback callback, PendingIntent operation, Handler handler) {
		if (callback == null && operation == null)
			throw new IllegalArgumentException("null NetworkCallback");
		nativeStartMonitoring();
		Registration r = new Registration(callback, operation, handler != null ? handler : getConnectivityThreadHandler());
		synchronized (registrations) {
			registrations.add(r);
			lastAvailable = nativeGetNetworkAvailable();
			// like AOSP: a matching network that is already up is reported right away
			if (lastAvailable)
				notify(r, true);
		}
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
				cb.onLinkPropertiesChanged(NETWORK, new LinkProperties());
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
	}

	public ProxyInfo getDefaultProxy() { return null; }

	public LinkProperties getLinkProperties(Network network) { return null; }
}
