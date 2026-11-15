package android.location;

import android.app.PendingIntent;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.Intent;
import android.location.provider.ProviderProperties;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.io.File;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/*
 * All providers are the desktop's position from the location portal (GeoClue behind it): one portal session
 * while any listener is registered. In a Flatpak the portal asks the user; outside, only with
 * ATL_UGLY_ENABLE_LOCATION.
 */
public class LocationManager {

	public static final String NETWORK_PROVIDER = "network";
	public static final String GPS_PROVIDER = "gps";
	public static final String PASSIVE_PROVIDER = "passive";
	public static final String FUSED_PROVIDER = "fused";
	public static final String KEY_LOCATION_CHANGED = "location";
	public static final String MODE_CHANGED_ACTION = "android.location.MODE_CHANGED";
	public static final String PROVIDERS_CHANGED_ACTION = "android.location.PROVIDERS_CHANGED";

	private static final List<String> PROVIDERS = Arrays.asList(PASSIVE_PROVIDER, NETWORK_PROVIDER, FUSED_PROVIDER, GPS_PROVIDER);

	/* a listener (LocationListener, PendingIntent or one-shot Consumer) with its provider and where it is called */
	private static class Registration {
		final String provider;
		final Executor executor;
		final boolean once;

		Registration(String provider, Executor executor, boolean once) {
			this.provider = provider;
			this.executor = executor;
			this.once = once;
		}
	}

	private static final Map<Object, Registration> registrations = new LinkedHashMap<>();
	private static Location lastLocation;
	private static Context context;

	public LocationManager() {}

	public LocationManager(Context context) {
		LocationManager.context = context;
	}

	/** whether ATL may ask the desktop for the position */
	public static boolean available() {
		if (android.content.pm.PackageManager.deniedByUser("android.permission.ACCESS_FINE_LOCATION")
		    || android.content.pm.PackageManager.deniedByUser("android.permission.ACCESS_COARSE_LOCATION"))
			return false;                   // switched off for this app: no provider, no position
		return System.getenv("ATL_UGLY_ENABLE_LOCATION") != null || new File("/.flatpak-info").exists();
	}

	private static Executor handlerExecutor(Looper looper) {
		if (looper == null)
			looper = Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper();
		final Handler handler = new Handler(looper);
		return new Executor() {
			public void execute(Runnable r) {
				handler.post(r);
			}
		};
	}

	private static void register(Object listener, String provider, Executor executor, boolean once) {
		if (listener == null)
			throw new IllegalArgumentException("invalid null listener");
		synchronized (registrations) {
			boolean first = registrations.isEmpty();
			registrations.put(listener, new Registration(provider == null ? FUSED_PROVIDER : provider, executor, once));
			if (first && available())
				nativeStart();
		}
	}

	private static void unregister(Object listener) {
		synchronized (registrations) {
			if (registrations.remove(listener) != null && registrations.isEmpty() && available())
				nativeStop();
		}
	}

	public String getBestProvider(Criteria criteria, boolean enabledOnly) {
		return enabledOnly && !available() ? null : FUSED_PROVIDER;
	}

	public Location getLastKnownLocation(String provider) {
		synchronized (registrations) {
			return lastLocation == null ? null : withProvider(lastLocation, provider);
		}
	}

	public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, LocationListener listener) {
		register(listener, provider, handlerExecutor(null), false);
	}

	public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, LocationListener listener, Looper looper) {
		register(listener, provider, handlerExecutor(looper), false);
	}

	public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, Executor executor, LocationListener listener) {
		register(listener, provider, executor, false);
	}

	public void requestLocationUpdates(long minTimeMs, float minDistanceM, Criteria criteria, LocationListener listener, Looper looper) {
		register(listener, FUSED_PROVIDER, handlerExecutor(looper), false);
	}

	public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, PendingIntent pendingIntent) {
		register(pendingIntent, provider, handlerExecutor(Looper.getMainLooper()), false);
	}

	/* API 31: the request's interval and quality do not change the desktop's position */
	public void requestLocationUpdates(String provider, LocationRequest request, Executor executor, LocationListener listener) {
		register(listener, provider, executor, false);
	}

	public void requestLocationUpdates(String provider, LocationRequest request, PendingIntent pendingIntent) {
		register(pendingIntent, provider, handlerExecutor(Looper.getMainLooper()), false);
	}

	public void getCurrentLocation(String provider, LocationRequest request, CancellationSignal cancellationSignal, Executor executor, Consumer<Location> consumer) {
		getCurrentLocation(provider, cancellationSignal, executor, consumer);
	}

	public void requestSingleUpdate(String provider, LocationListener listener, Looper looper) {
		register(listener, provider, handlerExecutor(looper), true);
	}

	public void getCurrentLocation(String provider, CancellationSignal cancellationSignal, Executor executor, final Consumer<Location> consumer) {
		synchronized (registrations) {
			// like AOSP, a recent last location answers at once
			if (lastLocation != null && SystemClock.elapsedRealtimeNanos() - lastLocation.getElapsedRealtimeNanos() < 30_000_000_000L) {
				final Location location = withProvider(lastLocation, provider);
				executor.execute(new Runnable() {
					public void run() {
						consumer.accept(location);
					}
				});
				return;
			}
		}
		register(consumer, provider, executor, true);
		if (cancellationSignal != null) {
			cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
				public void onCancel() {
					unregister(consumer);
				}
			});
		}
	}

	public void removeUpdates(LocationListener listener) {
		unregister(listener);
	}

	public void removeUpdates(PendingIntent pendingIntent) {
		unregister(pendingIntent);
	}

	private static Location withProvider(Location location, String provider) {
		Location copy = new Location(location);
		copy.setProvider(provider == null ? FUSED_PROVIDER : provider);
		return copy;
	}

	private static native void nativeStart();
	private static native void nativeStop();

	/* the portal's LocationUpdated: timestamp in ms since the epoch, elapsedRealtimeNanos when it came */
	@Export
	static void locationUpdated(double latitude, double longitude, double altitude, double accuracy,
	                            double speed, double bearing, long timestamp, long elapsedRealtimeNanos) {
		Location location = new Location(FUSED_PROVIDER);
		location.setLatitude(latitude);
		location.setLongitude(longitude);
		if (altitude != -Double.MAX_VALUE) // the portal's "unknown"
			location.setAltitude(altitude);
		location.setAccuracy((float)accuracy);
		if (speed >= 0)
			location.setSpeed((float)speed);
		if (bearing >= 0)
			location.setBearing((float)bearing);
		location.setTime(timestamp);
		location.setElapsedRealtimeNanos(elapsedRealtimeNanos);
		List<Map.Entry<Object, Registration>> targets;
		synchronized (registrations) {
			lastLocation = location;
			targets = new ArrayList<>(registrations.entrySet());
			for (Map.Entry<Object, Registration> e : targets)
				if (e.getValue().once)
					registrations.remove(e.getKey());
			if (registrations.isEmpty() && !targets.isEmpty() && available())
				nativeStop();
		}
		for (Map.Entry<Object, Registration> e : targets) {
			final Object listener = e.getKey();
			final Location copy = withProvider(location, e.getValue().provider);
			e.getValue().executor.execute(new Runnable() {
				@SuppressWarnings("unchecked")
				public void run() {
					if (listener instanceof LocationListener) {
						((LocationListener)listener).onLocationChanged(copy);
					} else if (listener instanceof Consumer) {
						((Consumer<Location>)listener).accept(copy);
					} else if (listener instanceof PendingIntent) {
						try {
							((PendingIntent)listener).send(context, 0, new Intent().putExtra(KEY_LOCATION_CHANGED, copy));
						} catch (PendingIntent.CanceledException ignored) {
							unregister(listener);
						}
					}
				}
			});
		}
	}

	public boolean sendExtraCommand(String provider, String command, Bundle extras) {
		return true;
	}

	public GpsStatus getGpsStatus(GpsStatus status) {
		if (status == null) {
			status = new GpsStatus();
		}
		return status;
	}

	public List<String> getAllProviders() {
		return new ArrayList<>(PROVIDERS);
	}

	public List<String> getProviders(boolean enabledOnly) {
		return enabledOnly && !available() ? new ArrayList<String>() : getAllProviders();
	}

	public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
		return getProviders(enabledOnly);
	}

	/* the desktop's position: fine, like AOSP's GPS provider, which alone needs satellites */
	public ProviderProperties getProviderProperties(String provider) {
		if (!PROVIDERS.contains(provider))
			return null;
		return new ProviderProperties.Builder()
		    .setHasSatelliteRequirement(GPS_PROVIDER.equals(provider))
		    .setHasAltitudeSupport(true)
		    .setHasSpeedSupport(true)
		    .setHasBearingSupport(true)
		    .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
		    .setAccuracy(ProviderProperties.ACCURACY_FINE)
		    .build();
	}

	public LocationProvider getProvider(String provider) {
		ProviderProperties properties = getProviderProperties(provider);
		return properties == null ? null : new LocationProvider(provider, properties);
	}

	public boolean hasProvider(String provider) {
		return PROVIDERS.contains(provider);
	}

	public boolean registerGnssStatusCallback(GnssStatus.Callback callback, Handler handler) {
		return registerGnssStatusCallback(handlerExecutor(handler != null ? handler.getLooper() : null), callback);
	}

	public boolean registerGnssStatusCallback(GnssStatus.Callback callback) {
		return registerGnssStatusCallback(callback, null);
	}

	/* the desktop's position comes without satellites: one status with none */
	public boolean registerGnssStatusCallback(Executor executor, final GnssStatus.Callback callback) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				callback.onSatelliteStatusChanged(new GnssStatus.Builder().build());
			}
		});
		return true;
	}

	public void unregisterGnssStatusCallback(GnssStatus.Callback callback) {
	}

	/* AOSP registers the listener and returns true; without a GNSS HAL nothing ever calls onGnssMeasurementsReceived
	 * or onStatusChanged (GnssNative: no measurement provider, status only set by the HAL) */
	public boolean registerGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback) {
		return true;
	}

	public boolean registerGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback, Handler handler) {
		return true;
	}

	public boolean registerGnssMeasurementsCallback(Executor executor, GnssMeasurementsEvent.Callback callback) {
		return true;
	}

	public void unregisterGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback) {}

	/* LocationManagerService.getGnssYearOfHardware/getGnssHardwareModelName: GnssNative defaults 0 / null */
	public int getGnssYearOfHardware() {
		return 0;
	}

	public String getGnssHardwareModelName() {
		return null;
	}

	/* the geofence manager stores the fence; without a location source it never fires */
	public void addProximityAlert(double latitude, double longitude, float radius, long expiration,
	                              android.app.PendingIntent pendingIntent) {
		if (pendingIntent == null)
			throw new IllegalArgumentException("invalid null pending intent");
	}

	public void removeProximityAlert(android.app.PendingIntent intent) {
		if (intent == null)
			throw new IllegalArgumentException("invalid null pending intent");
	}

	/* the desktop's position comes without satellites: the listener is accepted and never called */
	public boolean addGpsStatusListener(GpsStatus.Listener listener) {
		return true;
	}

	public void removeGpsStatusListener(GpsStatus.Listener listener) {}

	/* the desktop's position comes without NMEA sentences: the listeners are accepted and never called */
	public boolean addNmeaListener(OnNmeaMessageListener listener) {
		return true;
	}

	public boolean addNmeaListener(OnNmeaMessageListener listener, Handler handler) {
		return true;
	}

	public boolean addNmeaListener(Executor executor, OnNmeaMessageListener listener) {
		return true;
	}

	public void removeNmeaListener(OnNmeaMessageListener listener) {}

	public boolean addNmeaListener(GpsStatus.NmeaListener listener) {
		return true;
	}

	public void removeNmeaListener(GpsStatus.NmeaListener listener) {}

	public boolean isProviderEnabled(String provider) {
		return available() && PROVIDERS.contains(provider);
	}

	public boolean isLocationEnabled() {
		return available();
	}
}
