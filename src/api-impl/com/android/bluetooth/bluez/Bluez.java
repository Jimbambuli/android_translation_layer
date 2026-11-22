package com.android.bluetooth.bluez;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* ATL: BlueZ's object tree (org.freedesktop.DBus.ObjectManager at /) mirrored in the app, kept current by
 * InterfacesAdded/InterfacesRemoved/PropertiesChanged. This is the state AOSP's Bluetooth app keeps from its native
 * stack's callbacks (AdapterProperties, RemoteDevices); the services in com.android.bluetooth read it from here.
 * Reads may come from any thread; the observers run on the main thread. */
public final class Bluez implements BluezBus.Listener {
	private static final String TAG = "Bluez";

	public static final String ADAPTER = "org.bluez.Adapter1";
	public static final String DEVICE = "org.bluez.Device1";
	public static final String GATT_SERVICE = "org.bluez.GattService1";
	public static final String GATT_CHARACTERISTIC = "org.bluez.GattCharacteristic1";
	public static final String GATT_DESCRIPTOR = "org.bluez.GattDescriptor1";
	public static final String LE_ADVERTISING_MANAGER = "org.bluez.LEAdvertisingManager1";
	public static final String PROFILE_MANAGER = "org.bluez.ProfileManager1";
	public static final String PROPERTIES = "org.freedesktop.DBus.Properties";

	public interface Observer {
		void onInterfaceAdded(String path, String iface, Map<String, Object> properties);
		void onInterfaceRemoved(String path, String iface, Map<String, Object> lastProperties);
		void onPropertiesChanged(String path, String iface, Map<String, Object> changed, String[] invalidated);
		/* BlueZ went away (present false) or came (back) with a new object tree */
		void onReset(boolean present);
	}

	private static Bluez instance;

	private final HashMap<String, HashMap<String, HashMap<String, Object>>> objects = new HashMap<>();
	private final ArrayList<Observer> observers = new ArrayList<>();
	private boolean present;

	private Bluez() {}

	public static synchronized Bluez get() {
		if (instance == null) {
			instance = new Bluez();
			if (BluezBus.init(instance))
				instance.reload();
		}
		return instance;
	}

	public synchronized boolean isPresent() {
		return present;
	}

	public synchronized void addObserver(Observer o) {
		if (!observers.contains(o))
			observers.add(o);
	}

	public synchronized void removeObserver(Observer o) {
		observers.remove(o);
	}

	private synchronized Observer[] observers() {
		return observers.toArray(new Observer[0]);
	}

	@SuppressWarnings("unchecked")
	private boolean reload() {
		Object r = BluezBus.callSync("/", "org.freedesktop.DBus.ObjectManager", "GetManagedObjects", null, null);
		synchronized (this) {
			objects.clear();
			if (!(r instanceof Object[])) {
				Log.i(TAG, "BlueZ is not available: " + r);
				present = false;
				return false;
			}
			present = true;
			Map<String, Map<String, Map<String, Object>>> tree = (Map<String, Map<String, Map<String, Object>>>)((Object[])r)[0];
			for (Map.Entry<String, Map<String, Map<String, Object>>> e : tree.entrySet()) {
				HashMap<String, HashMap<String, Object>> ifaces = new HashMap<>();
				for (Map.Entry<String, Map<String, Object>> i : e.getValue().entrySet())
					ifaces.put(i.getKey(), new HashMap<>(i.getValue()));
				objects.put(e.getKey(), ifaces);
			}
		}
		return true;
	}

	/* ---- reads ---- */

	/* a copy of the properties of path's iface, null if there is none */
	public synchronized Map<String, Object> properties(String path, String iface) {
		HashMap<String, HashMap<String, Object>> ifaces = objects.get(path);
		HashMap<String, Object> props = ifaces == null ? null : ifaces.get(iface);
		return props == null ? null : new HashMap<>(props);
	}

	public synchronized Object property(String path, String iface, String name) {
		HashMap<String, HashMap<String, Object>> ifaces = objects.get(path);
		HashMap<String, Object> props = ifaces == null ? null : ifaces.get(iface);
		return props == null ? null : props.get(name);
	}

	public boolean booleanProperty(String path, String iface, String name) {
		Object o = property(path, iface, name);
		return o instanceof Boolean && (Boolean)o;
	}

	public synchronized boolean has(String path, String iface) {
		HashMap<String, HashMap<String, Object>> ifaces = objects.get(path);
		return ifaces != null && ifaces.containsKey(iface);
	}

	/* paths with iface, sorted; under prefix + "/" if prefix is given */
	public synchronized List<String> paths(String iface, String prefix) {
		ArrayList<String> list = new ArrayList<>();
		for (Map.Entry<String, HashMap<String, HashMap<String, Object>>> e : objects.entrySet()) {
			if (e.getValue().containsKey(iface) && (prefix == null || e.getKey().startsWith(prefix + "/")))
				list.add(e.getKey());
		}
		java.util.Collections.sort(list);
		return list;
	}

	/* ---- the adapter ATL uses: the first powered one, else the first (hci0 on most machines) ---- */

	public synchronized String adapterPath() {
		String first = null;
		for (String p : paths(ADAPTER, null)) {
			if (first == null)
				first = p;
			if (Boolean.TRUE.equals(objects.get(p).get(ADAPTER).get("Powered")))
				return p;
		}
		return first;
	}

	public static String devicePath(String adapter, String address) {
		return adapter + "/dev_" + address.toUpperCase(java.util.Locale.ROOT).replace(':', '_');
	}

	/* ---- BluezBus.Listener (main thread) ---- */

	@Override
	@SuppressWarnings("unchecked")
	public void onSignal(String path, String iface, String member, Object[] args) {
		if ("org.freedesktop.DBus.ObjectManager".equals(iface) && "InterfacesAdded".equals(member)) {
			String objectPath = (String)args[0];
			Map<String, Map<String, Object>> added = (Map<String, Map<String, Object>>)args[1];
			synchronized (this) {
				HashMap<String, HashMap<String, Object>> ifaces = objects.get(objectPath);
				if (ifaces == null)
					objects.put(objectPath, ifaces = new HashMap<>());
				for (Map.Entry<String, Map<String, Object>> e : added.entrySet())
					ifaces.put(e.getKey(), new HashMap<>(e.getValue()));
			}
			for (Map.Entry<String, Map<String, Object>> e : added.entrySet())
				for (Observer o : observers())
					o.onInterfaceAdded(objectPath, e.getKey(), e.getValue());
		} else if ("org.freedesktop.DBus.ObjectManager".equals(iface) && "InterfacesRemoved".equals(member)) {
			String objectPath = (String)args[0];
			Object[] removed = (Object[])args[1];
			HashMap<String, HashMap<String, Object>> last = new HashMap<>();
			synchronized (this) {
				HashMap<String, HashMap<String, Object>> ifaces = objects.get(objectPath);
				if (ifaces != null) {
					for (Object r : removed) {
						HashMap<String, Object> props = ifaces.remove((String)r);
						last.put((String)r, props != null ? props : new HashMap<String, Object>());
					}
					if (ifaces.isEmpty())
						objects.remove(objectPath);
				}
			}
			for (Object r : removed)
				for (Observer o : observers())
					o.onInterfaceRemoved(objectPath, (String)r, last.get((String)r));
		} else if (PROPERTIES.equals(iface) && "PropertiesChanged".equals(member)) {
			String changedIface = (String)args[0];
			Map<String, Object> changed = (Map<String, Object>)args[1];
			Object[] inv = (Object[])args[2];
			String[] invalidated = new String[inv.length];
			for (int i = 0; i < inv.length; i++)
				invalidated[i] = (String)inv[i];
			synchronized (this) {
				HashMap<String, HashMap<String, Object>> ifaces = objects.get(path);
				HashMap<String, Object> props = ifaces == null ? null : ifaces.get(changedIface);
				if (props == null)
					return; /* not (yet) known */
				props.putAll(changed);
				for (String name : invalidated)
					props.remove(name);
			}
			for (Observer o : observers())
				o.onPropertiesChanged(path, changedIface, changed, invalidated);
		}
	}

	@Override
	public void onOwnerChanged(boolean nowPresent) {
		synchronized (this) {
			if (nowPresent == present)
				return;
		}
		if (nowPresent)
			reload();
		else
			synchronized (this) {
				objects.clear();
				present = false;
			}
		for (Observer o : observers())
			o.onReset(nowPresent);
	}

	/* ---- small helpers ---- */

	/* "org.bluez.Error.InProgress" -> "InProgress" */
	public static String shortError(String error) {
		if (error == null)
			return null;
		int dot = error.lastIndexOf('.');
		return dot < 0 ? error : error.substring(dot + 1);
	}

	public static Object[] dict(Object... keysAndValues) {
		return keysAndValues;
	}

	public static Object[] variant(String sig, Object value) {
		return new Object[] {sig, value};
	}
}
