package com.android.bluetooth.gatt;

import android.bluetooth.IBluetoothAdvertise;
import android.bluetooth.IBluetoothGattServerCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.IAdvertisingSetCallback;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.content.AttributionSource;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import com.android.bluetooth.bluez.Bluez;
import com.android.bluetooth.bluez.BluezBus;
import com.android.bluetooth.btservice.AdapterService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* ATL: AOSP's gatt/AdvertiseManager (android/app, android-16.0.0_r4) as the IBluetoothAdvertise of the app's process.
 * Each advertising set is an org.bluez.LEAdvertisement1 object the app exports and registers with BlueZ's
 * LEAdvertisingManager1; BlueZ picks the advertising instance, legacy or extended PDUs (extended when a secondary
 * channel is asked for) and the address. Changing data or parameters re-registers the object.
 * Not in BlueZ's D-Bus API: periodic advertising (FEATURE_UNSUPPORTED), the own address (reported as unknown),
 * scan response data apart from what the advertisement holds (BlueZ fills the scan response itself with what does
 * not fit). */
public final class AdvertiseManager implements IBluetoothAdvertise {
	private static final String TAG = "AdvertiseManager";

	private static final String XML = "<node><interface name='org.bluez.LEAdvertisement1'>"
	                                  + "<method name='Release'/>"
	                                  + "<property name='Type' type='s' access='read'/>"
	                                  + "<property name='ServiceUUIDs' type='as' access='read'/>"
	                                  + "<property name='SolicitUUIDs' type='as' access='read'/>"
	                                  + "<property name='ManufacturerData' type='a{qv}' access='read'/>"
	                                  + "<property name='ServiceData' type='a{sv}' access='read'/>"
	                                  + "<property name='Includes' type='as' access='read'/>"
	                                  + "<property name='Discoverable' type='b' access='read'/>"
	                                  + "<property name='Timeout' type='q' access='read'/>"
	                                  + "<property name='SecondaryChannel' type='s' access='read'/>"
	                                  + "<property name='MinInterval' type='u' access='read'/>"
	                                  + "<property name='MaxInterval' type='u' access='read'/>"
	                                  + "<property name='TxPower' type='n' access='read'/>"
	                                  + "</interface></node>";

	private static AdvertiseManager instance;

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final AdapterService adapterService;

	private final class Advertiser implements BluezBus.ExportedObject {
		final int id;
		final IAdvertisingSetCallback callback;
		final String path;
		AdvertisingSetParameters parameters;
		AdvertiseData data;
		AdvertiseData scanResponse;
		int timeoutSeconds;
		boolean registered;
		boolean enabled;

		Advertiser(int id, IAdvertisingSetCallback callback) {
			this.id = id;
			this.callback = callback;
			this.path = "/org/atl/bluetooth/advertiser" + id;
		}

		@Override
		public void onMethodCall(long invocation, String method, Object[] args) {
			BluezBus.returnValue(invocation, null);
			if ("Release".equals(method)) {
				/* BlueZ ends it: the timeout ran out (or the adapter went off) */
				registered = false;
				boolean was = enabled;
				enabled = false;
				BluezBus.unexport(path, XML);
				if (was)
					call(new Call() {
						public void run() throws RemoteException {
							callback.onAdvertisingEnabled(id, false, AdvertisingSetCallback.ADVERTISE_SUCCESS);
						}
					});
			}
		}

		@Override
		public Object getProperty(String name) {
			switch (name) {
				case "Type":
					return parameters.isConnectable() ? "peripheral" : "broadcast";
				case "ServiceUUIDs":
					return uuids(merged().serviceUuids);
				case "SolicitUUIDs":
					return uuids(merged().solicitUuids);
				case "ManufacturerData":
					return merged().manufacturer;
				case "ServiceData":
					return merged().serviceData;
				case "Includes": {
					ArrayList<Object> inc = new ArrayList<>();
					if (merged().includeName)
						inc.add("local-name");
					if (merged().includeTxPower || parameters.includeTxPower())
						inc.add("tx-power");
					return inc.toArray();
				}
				case "Discoverable":
					return parameters.isDiscoverable() && parameters.isConnectable();
				case "Timeout":
					return timeoutSeconds;
				case "SecondaryChannel":
					return parameters.isLegacy() ? null : parameters.getSecondaryPhy() == 2 ? "2M" : parameters.getSecondaryPhy() == 3 ? "Coded" : "1M";
				case "MinInterval":
				case "MaxInterval":
					/* AdvertisingSetParameters: interval in 0.625 ms units; BlueZ: ms */
					return (long)Math.max(20, Math.round(parameters.getInterval() * 0.625));
				case "TxPower":
					return parameters.getTxPowerLevel();
				default:
					return null;
			}
		}

		private Merged merged() {
			return new Merged(data, scanResponse);
		}
	}

	/* the advertisement and its scan response as one LEAdvertisement1 */
	private static final class Merged {
		final ArrayList<ParcelUuid> serviceUuids = new ArrayList<>();
		final ArrayList<ParcelUuid> solicitUuids = new ArrayList<>();
		Object[] manufacturer = new Object[0];
		Object[] serviceData = new Object[0];
		boolean includeName, includeTxPower;

		Merged(AdvertiseData... parts) {
			ArrayList<Object> m = new ArrayList<>(), s = new ArrayList<>();
			for (AdvertiseData d : parts) {
				if (d == null)
					continue;
				serviceUuids.addAll(d.getServiceUuids());
				solicitUuids.addAll(d.getServiceSolicitationUuids());
				for (int i = 0; i < d.getManufacturerSpecificData().size(); i++) {
					m.add(d.getManufacturerSpecificData().keyAt(i));
					m.add(Bluez.variant("ay", d.getManufacturerSpecificData().valueAt(i)));
				}
				for (Map.Entry<ParcelUuid, byte[]> e : d.getServiceData().entrySet()) {
					s.add(e.getKey().toString());
					s.add(Bluez.variant("ay", e.getValue()));
				}
				includeName |= d.getIncludeDeviceName();
				includeTxPower |= d.getIncludeTxPowerLevel();
			}
			manufacturer = m.toArray();
			serviceData = s.toArray();
		}
	}

	private static Object[] uuids(List<ParcelUuid> list) {
		Object[] out = new Object[list.size()];
		for (int i = 0; i < out.length; i++)
			out[i] = list.get(i).toString();
		return out;
	}

	private final SparseArray<Advertiser> advertisers = new SparseArray<>();
	private int nextId = 1;

	private AdvertiseManager() {
		adapterService = AdapterService.get();
	}

	public static synchronized AdvertiseManager get() {
		if (instance == null)
			instance = new AdvertiseManager();
		return instance;
	}

	/* ---- capabilities (LEAdvertisingManager1) ---- */

	private Object managerProperty(String name) {
		String adapter = adapterService.adapterPath();
		return adapter == null ? null : adapterService.bluez().property(adapter, Bluez.LE_ADVERTISING_MANAGER, name);
	}

	public int numOfAdvertisementInstances() {
		Object supported = managerProperty("SupportedInstances");
		Object active = managerProperty("ActiveInstances");
		int n = 0;
		if (supported instanceof Number)
			n += ((Number)supported).intValue();
		if (active instanceof Number)
			n += ((Number)active).intValue();
		return n;
	}

	public List<String> secondaryChannels() {
		Object channels = managerProperty("SupportedSecondaryChannels");
		ArrayList<String> list = new ArrayList<>();
		if (channels instanceof Object[])
			for (Object o : (Object[])channels)
				list.add((String)o);
		return list;
	}

	@SuppressWarnings("unchecked")
	public int maxAdvertisingDataLength() {
		Object caps = managerProperty("SupportedCapabilities");
		if (caps instanceof Map) {
			Object max = ((Map<String, Object>)caps).get("MaxAdvLen");
			if (max instanceof Number)
				return ((Number)max).intValue();
		}
		return 31; /* legacy advertising PDU */
	}

	/* ---- IBluetoothAdvertise ---- */

	private interface Call {
		void run() throws RemoteException;
	}

	private void call(final Call c) {
		handler.post(new Runnable() {
			public void run() {
				try {
					c.run();
				} catch (RemoteException e) {
				}
			}
		});
	}

	private Advertiser byCallback(IAdvertisingSetCallback callback) {
		synchronized (advertisers) {
			for (int i = 0; i < advertisers.size(); i++)
				if (advertisers.valueAt(i).callback == callback)
					return advertisers.valueAt(i);
		}
		return null;
	}

	private Advertiser byId(int id) {
		synchronized (advertisers) {
			return advertisers.get(id);
		}
	}

	/* BlueZ's errors of RegisterAdvertisement (src/advertising.c) as AdvertisingSetCallback status */
	private static int status(String error, String message) {
		if (error == null)
			return AdvertisingSetCallback.ADVERTISE_SUCCESS;
		switch (Bluez.shortError(error)) {
			case "InvalidLength":
				return AdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE;
			case "AlreadyExists":
				return AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED;
			case "NotSupported":
				return AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED;
			case "NotPermitted":
				/* "Maximum advertisements reached" */
				return AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS;
			default:
				return message != null && message.contains("Maximum") ? AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
				                                                      : AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR;
		}
	}

	private interface Done {
		void done(int status);
	}

	private void register(final Advertiser a, final Done done) {
		String adapter = adapterService.adapterPath();
		if (adapter == null || !adapterService.bluez().has(adapter, Bluez.LE_ADVERTISING_MANAGER)) {
			done.done(AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED);
			return;
		}
		if (!BluezBus.export(a.path, XML, a)) {
			done.done(AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR);
			return;
		}
		BluezBus.call(adapter, Bluez.LE_ADVERTISING_MANAGER, "RegisterAdvertisement", "(oa{sv})", new Object[] {a.path, new Object[0]}, new BluezBus.Reply() {
			public void onReply(Object[] result, String error, String message) {
				if (error != null) {
					Log.w(TAG, "RegisterAdvertisement: " + error + " " + message);
					BluezBus.unexport(a.path, XML);
				} else {
					a.registered = true;
				}
				done.done(status(error, message));
			}
		});
	}

	private void unregister(final Advertiser a, final Done done) {
		String adapter = adapterService.adapterPath();
		if (!a.registered || adapter == null) {
			BluezBus.unexport(a.path, XML);
			a.registered = false;
			if (done != null)
				done.done(AdvertisingSetCallback.ADVERTISE_SUCCESS);
			return;
		}
		a.registered = false;
		BluezBus.call(adapter, Bluez.LE_ADVERTISING_MANAGER, "UnregisterAdvertisement", "(o)", new Object[] {a.path}, new BluezBus.Reply() {
			public void onReply(Object[] result, String error, String message) {
				BluezBus.unexport(a.path, XML);
				if (done != null)
					done.done(AdvertisingSetCallback.ADVERTISE_SUCCESS);
			}
		});
	}

	@Override
	public void startAdvertisingSet(final AdvertisingSetParameters parameters, final AdvertiseData advertiseData, final AdvertiseData scanResponse,
	                                PeriodicAdvertisingParameters periodicParameters, AdvertiseData periodicData, final int duration, int maxExtAdvEvents,
	                                IBluetoothGattServerCallback gattServerCallback, final IAdvertisingSetCallback callback, AttributionSource source) {
		AdapterService.checkAdvertisePermission("AdvertiseManager startAdvertisingSet()");
		if (parameters.getOwnAddressType() != AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT || parameters.isDirected()) {
			if (!AdapterService.hasPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED))
				throw new SecurityException("Need BLUETOOTH_PRIVILEGED permission");
		}
		handler.post(new Runnable() {
			public void run() {
				final int id;
				synchronized (advertisers) {
					id = nextId++;
				}
				final Advertiser a = new Advertiser(id, callback);
				a.parameters = parameters;
				a.data = advertiseData;
				a.scanResponse = scanResponse;
				/* duration: 10 ms units, 0 = until stopped; BlueZ's Timeout: seconds */
				a.timeoutSeconds = duration <= 0 ? 0 : Math.max(1, (duration + 99) / 100);
				if (periodicParameters != null || periodicData != null) {
					try {
						callback.onAdvertisingSetStarted(null, 0, 0, AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED);
					} catch (RemoteException e) {
					}
					return;
				}
				synchronized (advertisers) {
					advertisers.put(id, a);
				}
				register(a, new Done() {
					public void done(int status) {
						if (status != AdvertisingSetCallback.ADVERTISE_SUCCESS) {
							synchronized (advertisers) {
								advertisers.remove(id);
							}
						} else {
							a.enabled = true;
						}
						try {
							callback.onAdvertisingSetStarted(null, id, parameters.getTxPowerLevel(), status);
						} catch (RemoteException e) {
						}
					}
				});
			}
		});
	}

	@Override
	public void stopAdvertisingSet(final IAdvertisingSetCallback callback, AttributionSource source) {
		AdapterService.checkAdvertisePermission("AdvertiseManager stopAdvertisingSet()");
		handler.post(new Runnable() {
			public void run() {
				final Advertiser a = byCallback(callback);
				if (a == null)
					return;
				synchronized (advertisers) {
					advertisers.remove(a.id);
				}
				unregister(a, new Done() {
					public void done(int status) {
						try {
							callback.onAdvertisingSetStopped(a.id);
						} catch (RemoteException e) {
						}
					}
				});
			}
		});
	}

	/* BlueZ chooses the address and does not say which one */
	@Override
	public void getOwnAddress(final int advertiserId, AttributionSource source) {
		AdapterService.checkAdvertisePermission("AdvertiseManager getOwnAddress()");
		if (!AdapterService.hasPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED))
			throw new SecurityException("Need BLUETOOTH_PRIVILEGED permission");
		final Advertiser a = byId(advertiserId);
		if (a == null)
			return;
		call(new Call() {
			public void run() throws RemoteException {
				a.callback.onOwnAddressRead(advertiserId, -1, "00:00:00:00:00:00");
			}
		});
	}

	@Override
	public void enableAdvertisingSet(final int advertiserId, final boolean enable, final int duration, int maxExtAdvEvents, AttributionSource source) {
		AdapterService.checkAdvertisePermission("AdvertiseManager enableAdvertisingSet()");
		handler.post(new Runnable() {
			public void run() {
				final Advertiser a = byId(advertiserId);
				if (a == null)
					return;
				Done done = new Done() {
					public void done(int status) {
						if (status == AdvertisingSetCallback.ADVERTISE_SUCCESS)
							a.enabled = enable;
						try {
							a.callback.onAdvertisingEnabled(advertiserId, enable, status);
						} catch (RemoteException e) {
						}
					}
				};
				if (enable) {
					a.timeoutSeconds = duration <= 0 ? 0 : Math.max(1, (duration + 99) / 100);
					if (a.registered)
						done.done(AdvertisingSetCallback.ADVERTISE_SUCCESS);
					else
						register(a, done);
				} else {
					unregister(a, done);
				}
			}
		});
	}

	private interface Change {
		void apply(Advertiser a);
		void report(Advertiser a, int status) throws RemoteException;
	}

	/* BlueZ reads the properties when the advertisement is registered: a change registers it again */
	private void change(final int advertiserId, final Change change) {
		handler.post(new Runnable() {
			public void run() {
				final Advertiser a = byId(advertiserId);
				if (a == null)
					return;
				change.apply(a);
				final Done report = new Done() {
					public void done(int status) {
						try {
							change.report(a, status);
						} catch (RemoteException e) {
						}
					}
				};
				if (!a.registered) {
					report.done(AdvertisingSetCallback.ADVERTISE_SUCCESS);
					return;
				}
				unregister(a, new Done() {
					public void done(int status) {
						register(a, report);
					}
				});
			}
		});
	}

	@Override
	public void setAdvertisingData(int advertiserId, final AdvertiseData data, AttributionSource source) {
		AdapterService.checkAdvertisePermission("AdvertiseManager setAdvertisingData()");
		change(advertiserId, new Change() {
			public void apply(Advertiser a) {
				a.data = data;
			}

			public void report(Advertiser a, int status) throws RemoteException {
				a.callback.onAdvertisingDataSet(a.id, status);
			}
		});
	}

	@Override
	public void setScanResponseData(int advertiserId, final AdvertiseData data, AttributionSource source) {
		AdapterService.checkAdvertisePermission("AdvertiseManager setScanResponseData()");
		change(advertiserId, new Change() {
			public void apply(Advertiser a) {
				a.scanResponse = data;
			}

			public void report(Advertiser a, int status) throws RemoteException {
				a.callback.onScanResponseDataSet(a.id, status);
			}
		});
	}

	@Override
	public void setAdvertisingParameters(int advertiserId, final AdvertisingSetParameters parameters, AttributionSource source) {
		AdapterService.checkAdvertisePermission("AdvertiseManager setAdvertisingParameters()");
		change(advertiserId, new Change() {
			public void apply(Advertiser a) {
				a.parameters = parameters;
			}

			public void report(Advertiser a, int status) throws RemoteException {
				a.callback.onAdvertisingParametersUpdated(a.id, parameters.getTxPowerLevel(), status);
			}
		});
	}

	@Override
	public void setPeriodicAdvertisingParameters(final int advertiserId, PeriodicAdvertisingParameters parameters, AttributionSource source) {
		AdapterService.checkAdvertisePermission("AdvertiseManager setPeriodicAdvertisingParameters()");
		final Advertiser a = byId(advertiserId);
		if (a != null)
			call(new Call() {
				public void run() throws RemoteException {
					a.callback.onPeriodicAdvertisingParametersUpdated(advertiserId, AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED);
				}
			});
	}

	@Override
	public void setPeriodicAdvertisingData(final int advertiserId, AdvertiseData data, AttributionSource source) {
		AdapterService.checkAdvertisePermission("AdvertiseManager setPeriodicAdvertisingData()");
		final Advertiser a = byId(advertiserId);
		if (a != null)
			call(new Call() {
				public void run() throws RemoteException {
					a.callback.onPeriodicAdvertisingDataSet(advertiserId, AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED);
				}
			});
	}

	@Override
	public void setPeriodicAdvertisingEnable(final int advertiserId, final boolean enable, AttributionSource source) {
		AdapterService.checkAdvertisePermission("AdvertiseManager setPeriodicAdvertisingEnable()");
		final Advertiser a = byId(advertiserId);
		if (a != null)
			call(new Call() {
				public void run() throws RemoteException {
					a.callback.onPeriodicAdvertisingEnabled(advertiserId, enable, AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED);
				}
			});
	}
}
