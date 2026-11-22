package android.os;

import java.util.HashMap;
import java.util.Map;

/* AOSP 16 ServiceManager in its single-process form (the $ravenwood variants): the services are kept in
 * this process. ATL's window manager lives in the app's process, so "window" is a local Binder with the
 * descriptor of IWindowManager and no transactions; apps that look it up by reflection get a binder as on a
 * device, not null. */
public final class ServiceManager {
	private static final Map<String, IBinder> sCache = new HashMap<>();

	static {
		sCache.put("window", new Binder("android.view.IWindowManager"));
	}

	public static IBinder getService(String name) {
		synchronized (ServiceManager.class) {
			return sCache.get(name);
		}
	}

	public static IBinder getServiceOrThrow(String name) throws ServiceNotFoundException {
		final IBinder binder = getService(name);
		if (binder != null) {
			return binder;
		} else {
			throw new ServiceNotFoundException(name);
		}
	}

	public static void addService(String name, IBinder service) {
		addService(name, service, false, 0);
	}

	public static void addService(String name, IBinder service, boolean allowIsolated) {
		addService(name, service, allowIsolated, 0);
	}

	public static void addService(String name, IBinder service, boolean allowIsolated, int dumpPriority) {
		synchronized (ServiceManager.class) {
			sCache.put(name, service);
		}
	}

	public static IBinder checkService(String name) {
		return getService(name);
	}

	public static String[] listServices() {
		synchronized (ServiceManager.class) {
			return sCache.keySet().toArray(new String[0]);
		}
	}

	public static class ServiceNotFoundException extends Exception {
		public ServiceNotFoundException(String name) {
			super("No service published for: " + name);
		}
	}
}
