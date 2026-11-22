package android.location;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.location.provider.ProviderProperties;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.Process;

import com.android.internal.util.Preconditions;
import com.android.server.location.LocationManagerService;
import com.android.server.location.provider.DesktopLocationProvider;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/*
 * AOSP 16 location/java/android/location/LocationManager.java (the public API), with LocationManagerService running in
 * the app's process instead of the binder interface. The providers are the desktop's position, see
 * com.android.server.location.provider.DesktopLocationProvider: in a Flatpak the location portal, which asks the
 * user, outside GeoClue2. The device has no GNSS hardware: no "gps" provider, the GNSS listeners are accepted and
 * never called (LocationManagerService without GnssManagerService).
 */
public class LocationManager {

	/* compat changes (CompatChanges, @EnabledAfter: on for apps targeting a later SDK) */
	private static final int BLOCK_IMMUTABLE_PENDING_INTENTS_AFTER = Build.VERSION_CODES.R;
	private static final int GET_PROVIDER_SECURITY_EXCEPTIONS_AFTER = Build.VERSION_CODES.Q;
	private static final int BLOCK_UNTARGETED_PENDING_INTENTS_AFTER = Build.VERSION_CODES.JELLY_BEAN;
	private static final int BLOCK_INCOMPLETE_LOCATIONS_AFTER = Build.VERSION_CODES.JELLY_BEAN;
	private static final int BLOCK_GPS_STATUS_USAGE_AFTER = Build.VERSION_CODES.R;

	public static final String NETWORK_PROVIDER = "network";
	public static final String GPS_PROVIDER = "gps";
	public static final String PASSIVE_PROVIDER = "passive";
	public static final String FUSED_PROVIDER = "fused";
	public static final String KEY_PROXIMITY_ENTERING = "entering";
	public static final String KEY_STATUS_CHANGED = "status";
	public static final String KEY_PROVIDER_ENABLED = "providerEnabled";
	public static final String KEY_LOCATION_CHANGED = "location";
	public static final String KEY_LOCATIONS = "locations";
	public static final String KEY_FLUSH_COMPLETE = "flushComplete";
	public static final String PROVIDERS_CHANGED_ACTION = "android.location.PROVIDERS_CHANGED";
	public static final String EXTRA_PROVIDER_NAME = "android.location.extra.PROVIDER_NAME";
	public static final String EXTRA_PROVIDER_ENABLED = "android.location.extra.PROVIDER_ENABLED";
	public static final String MODE_CHANGED_ACTION = "android.location.MODE_CHANGED";
	public static final String EXTRA_LOCATION_ENABLED = "android.location.extra.LOCATION_ENABLED";
	public static final String ACTION_GNSS_CAPABILITIES_CHANGED = "android.location.action.GNSS_CAPABILITIES_CHANGED";
	public static final String EXTRA_GNSS_CAPABILITIES = "android.location.extra.GNSS_CAPABILITIES";

	private static final long MAX_SINGLE_LOCATION_TIMEOUT_MS = 30 * 1000;

	private static final WeakHashMap<LocationListener, WeakReference<LocationListenerTransport>> sLocationListeners = new WeakHashMap<>();

	private final Context mContext;
	private final LocationManagerService mService = LocationManagerService.get();

	public LocationManager(Context context) {
		mContext = context;
		mService.setContext(context);
	}

	/** @hide whether ATL can give positions at all (the desktop has a location service and the user did not refuse
	 * it); PackageManager answers the location permissions with it */
	public static boolean available() {
		return DesktopLocationProvider.isAvailable() && !DesktopLocationProvider.isDenied();
	}

	private boolean isChangeEnabledAfter(int sdk) {
		return mContext.getApplicationInfo().targetSdkVersion > sdk;
	}

	public boolean isLocationEnabled() {
		return mService.isLocationEnabled();
	}

	public boolean isProviderEnabled(String provider) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		return mService.isProviderEnabled(provider);
	}

	public Location getLastKnownLocation(String provider) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		return mService.getLastLocation(provider);
	}

	public void getCurrentLocation(String provider, CancellationSignal cancellationSignal, Executor executor,
	                               Consumer<Location> consumer) {
		getCurrentLocation(provider, new LocationRequest.Builder(0).build(), cancellationSignal, executor, consumer);
	}

	public void getCurrentLocation(String provider, LocationRequest locationRequest,
	                               CancellationSignal cancellationSignal, Executor executor,
	                               Consumer<Location> consumer) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		Preconditions.checkArgument(locationRequest != null, "invalid null location request");
		if (cancellationSignal != null)
			cancellationSignal.throwIfCanceled();
		GetCurrentLocationTransport transport = new GetCurrentLocationTransport(executor, consumer, cancellationSignal);
		final Runnable cancelRemote = mService.getCurrentLocation(provider, locationRequest, transport);
		if (cancellationSignal != null) {
			// CancellationSignal.setRemote(): the service's registration ends with the cancellation
			cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
				public void onCancel() {
					transport.onCancel();
					cancelRemote.run();
				}
			});
		}
	}

	public void requestSingleUpdate(String provider, LocationListener listener, Looper looper) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		Handler handler = looper == null ? new Handler() : new Handler(looper);
		requestLocationUpdates(provider, new LocationRequest.Builder(0)
		                                     .setMaxUpdates(1)
		                                     .setDurationMillis(MAX_SINGLE_LOCATION_TIMEOUT_MS)
		                                     .build(),
		                       new HandlerExecutor(handler), listener);
	}

	public void requestSingleUpdate(Criteria criteria, LocationListener listener, Looper looper) {
		Preconditions.checkArgument(criteria != null, "invalid null criteria");
		Handler handler = looper == null ? new Handler() : new Handler(looper);
		requestLocationUpdates(FUSED_PROVIDER, new LocationRequest.Builder(0)
		                                           .setQuality(criteria)
		                                           .setMaxUpdates(1)
		                                           .setDurationMillis(MAX_SINGLE_LOCATION_TIMEOUT_MS)
		                                           .build(),
		                       new HandlerExecutor(handler), listener);
	}

	public void requestSingleUpdate(String provider, PendingIntent pendingIntent) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		requestLocationUpdates(provider, new LocationRequest.Builder(0)
		                                     .setMaxUpdates(1)
		                                     .setDurationMillis(MAX_SINGLE_LOCATION_TIMEOUT_MS)
		                                     .build(),
		                       pendingIntent);
	}

	public void requestSingleUpdate(Criteria criteria, PendingIntent pendingIntent) {
		Preconditions.checkArgument(criteria != null, "invalid null criteria");
		requestLocationUpdates(FUSED_PROVIDER, new LocationRequest.Builder(0)
		                                           .setQuality(criteria)
		                                           .setMaxUpdates(1)
		                                           .setDurationMillis(MAX_SINGLE_LOCATION_TIMEOUT_MS)
		                                           .build(),
		                       pendingIntent);
	}

	public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, LocationListener listener) {
		requestLocationUpdates(provider, minTimeMs, minDistanceM, listener, null);
	}

	public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, LocationListener listener,
	                                   Looper looper) {
		Handler handler = looper == null ? new Handler() : new Handler(looper);
		requestLocationUpdates(provider, minTimeMs, minDistanceM, new HandlerExecutor(handler), listener);
	}

	public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, Executor executor,
	                                   LocationListener listener) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		requestLocationUpdates(provider,
		                       LocationRequest.createFromDeprecatedProvider(provider, minTimeMs, minDistanceM, false),
		                       executor, listener);
	}

	public void requestLocationUpdates(long minTimeMs, float minDistanceM, Criteria criteria, LocationListener listener,
	                                   Looper looper) {
		Handler handler = looper == null ? new Handler() : new Handler(looper);
		requestLocationUpdates(minTimeMs, minDistanceM, criteria, new HandlerExecutor(handler), listener);
	}

	public void requestLocationUpdates(long minTimeMs, float minDistanceM, Criteria criteria, Executor executor,
	                                   LocationListener listener) {
		Preconditions.checkArgument(criteria != null, "invalid null criteria");
		requestLocationUpdates(FUSED_PROVIDER,
		                       LocationRequest.createFromDeprecatedCriteria(criteria, minTimeMs, minDistanceM, false),
		                       executor, listener);
	}

	public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, PendingIntent pendingIntent) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		requestLocationUpdates(provider,
		                       LocationRequest.createFromDeprecatedProvider(provider, minTimeMs, minDistanceM, false),
		                       pendingIntent);
	}

	public void requestLocationUpdates(long minTimeMs, float minDistanceM, Criteria criteria, PendingIntent pendingIntent) {
		Preconditions.checkArgument(criteria != null, "invalid null criteria");
		requestLocationUpdates(FUSED_PROVIDER,
		                       LocationRequest.createFromDeprecatedCriteria(criteria, minTimeMs, minDistanceM, false),
		                       pendingIntent);
	}

	public void requestLocationUpdates(String provider, LocationRequest locationRequest, Executor executor,
	                                   LocationListener listener) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		Preconditions.checkArgument(locationRequest != null, "invalid null location request");
		synchronized (sLocationListeners) {
			WeakReference<LocationListenerTransport> reference = sLocationListeners.get(listener);
			LocationListenerTransport transport = reference != null ? reference.get() : null;
			if (transport == null) {
				transport = new LocationListenerTransport(listener, executor);
			} else {
				Preconditions.checkState(transport.isRegistered());
				transport.setExecutor(executor);
			}
			mService.registerLocationListener(provider, locationRequest, transport);
			sLocationListeners.put(listener, new WeakReference<>(transport));
		}
	}

	public void requestLocationUpdates(String provider, LocationRequest locationRequest, PendingIntent pendingIntent) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		Preconditions.checkArgument(locationRequest != null, "invalid null location request");
		Preconditions.checkArgument(pendingIntent != null, "invalid null pending intent");
		if (isChangeEnabledAfter(BLOCK_UNTARGETED_PENDING_INTENTS_AFTER))
			Preconditions.checkArgument(pendingIntent.isTargetedToPackage(), "pending intent must be targeted to a package");
		if (isChangeEnabledAfter(BLOCK_IMMUTABLE_PENDING_INTENTS_AFTER))
			Preconditions.checkArgument(!pendingIntent.isImmutable(), "pending intent must be mutable");
		mService.registerLocationPendingIntent(provider, locationRequest, pendingIntent);
	}

	public void requestFlush(String provider, LocationListener listener, int requestCode) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		Preconditions.checkArgument(listener != null, "invalid null listener");
		synchronized (sLocationListeners) {
			WeakReference<LocationListenerTransport> ref = sLocationListeners.get(listener);
			LocationListenerTransport transport = ref != null ? ref.get() : null;
			Preconditions.checkArgument(transport != null, "unregistered listener cannot be flushed");
			mService.requestFlush(provider, transport, requestCode);
		}
	}

	public void requestFlush(String provider, PendingIntent pendingIntent, int requestCode) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		Preconditions.checkArgument(pendingIntent != null, "invalid null pending intent");
		mService.requestFlush(provider, pendingIntent, requestCode);
	}

	public void removeUpdates(LocationListener listener) {
		Preconditions.checkArgument(listener != null, "invalid null listener");
		synchronized (sLocationListeners) {
			WeakReference<LocationListenerTransport> ref = sLocationListeners.remove(listener);
			LocationListenerTransport transport = ref != null ? ref.get() : null;
			if (transport != null) {
				transport.unregister();
				mService.unregisterLocation(transport);
			}
		}
	}

	public void removeUpdates(PendingIntent pendingIntent) {
		Preconditions.checkArgument(pendingIntent != null, "invalid null pending intent");
		mService.unregisterLocation(pendingIntent);
	}

	public boolean hasProvider(String provider) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		return mService.hasProvider(provider);
	}

	public List<String> getAllProviders() {
		return mService.getAllProviders();
	}

	public List<String> getProviders(boolean enabledOnly) {
		return mService.getProviders(null, enabledOnly);
	}

	public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
		Preconditions.checkArgument(criteria != null, "invalid null criteria");
		return mService.getProviders(criteria, enabledOnly);
	}

	public String getBestProvider(Criteria criteria, boolean enabledOnly) {
		Preconditions.checkArgument(criteria != null, "invalid null criteria");
		return mService.getBestProvider(criteria, enabledOnly);
	}

	public LocationProvider getProvider(String provider) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		if (!isChangeEnabledAfter(GET_PROVIDER_SECURITY_EXCEPTIONS_AFTER)) {
			if (NETWORK_PROVIDER.equals(provider) || FUSED_PROVIDER.equals(provider)) {
				try {
					mContext.enforcePermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(), Process.myUid(), null);
				} catch (SecurityException e) {
					mContext.enforcePermission(Manifest.permission.ACCESS_COARSE_LOCATION, Process.myPid(), Process.myUid(), null);
				}
			} else {
				mContext.enforcePermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(), Process.myUid(), null);
			}
		}
		try {
			ProviderProperties properties = mService.getProviderProperties(provider);
			return new LocationProvider(provider, properties);
		} catch (IllegalArgumentException e) {
			// provider does not exist
			return null;
		}
	}

	public ProviderProperties getProviderProperties(String provider) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		return mService.getProviderProperties(provider);
	}

	public boolean sendExtraCommand(String provider, String command, Bundle extras) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		Preconditions.checkArgument(command != null, "invalid null command");
		mService.sendExtraCommand(provider, command);
		return true;
	}

	/* the test provider calls need the app op MOCK_LOCATION (the developer option «mock location app»); no app can be
	 * that app here, so LocationManagerService.addTestProvider() and the others return at once */
	public void addTestProvider(String provider, boolean requiresNetwork, boolean requiresSatellite,
	                            boolean requiresCell, boolean hasMonetaryCost, boolean supportsAltitude,
	                            boolean supportsSpeed, boolean supportsBearing, int powerUsage, int accuracy) {
		addTestProvider(provider, new ProviderProperties.Builder()
		                              .setHasNetworkRequirement(requiresNetwork)
		                              .setHasSatelliteRequirement(requiresSatellite)
		                              .setHasCellRequirement(requiresCell)
		                              .setHasMonetaryCost(hasMonetaryCost)
		                              .setHasAltitudeSupport(supportsAltitude)
		                              .setHasSpeedSupport(supportsSpeed)
		                              .setHasBearingSupport(supportsBearing)
		                              .setPowerUsage(powerUsage)
		                              .setAccuracy(accuracy)
		                              .build());
	}

	public void addTestProvider(String provider, ProviderProperties properties) {
		addTestProvider(provider, properties, Collections.<String>emptySet());
	}

	public void addTestProvider(String provider, ProviderProperties properties, Set<String> extraAttributionTags) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		Preconditions.checkArgument(properties != null, "invalid null properties");
		Preconditions.checkArgument(extraAttributionTags != null, "invalid null extra attribution tags");
	}

	public void removeTestProvider(String provider) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
	}

	public void setTestProviderLocation(String provider, Location location) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
		Preconditions.checkArgument(location != null, "invalid null location");
		if (isChangeEnabledAfter(BLOCK_INCOMPLETE_LOCATIONS_AFTER)) {
			Preconditions.checkArgument(location.isComplete(), "incomplete location object, missing timestamp or accuracy?");
		} else {
			location.makeComplete();
		}
	}

	public void clearTestProviderLocation(String provider) {}

	public void setTestProviderEnabled(String provider, boolean enabled) {
		Preconditions.checkArgument(provider != null, "invalid null provider");
	}

	public void clearTestProviderEnabled(String provider) {
		setTestProviderEnabled(provider, false);
	}

	public void setTestProviderStatus(String provider, int status, Bundle extras, long updateTime) {}

	public void clearTestProviderStatus(String provider) {}

	/* ATL has no GeofenceManager: the fence is accepted after AOSP's checks and never fires */
	public void addProximityAlert(double latitude, double longitude, float radius, long expiration,
	                              PendingIntent pendingIntent) {
		Preconditions.checkArgument(pendingIntent != null, "invalid null pending intent");
		if (isChangeEnabledAfter(BLOCK_UNTARGETED_PENDING_INTENTS_AFTER))
			Preconditions.checkArgument(pendingIntent.isTargetedToPackage(), "pending intent must be targeted to a package");
		if (isChangeEnabledAfter(BLOCK_IMMUTABLE_PENDING_INTENTS_AFTER))
			Preconditions.checkArgument(!pendingIntent.isImmutable(), "pending intent must be mutable");
	}

	public void removeProximityAlert(PendingIntent intent) {
		Preconditions.checkArgument(intent != null, "invalid null pending intent");
	}

	// ================= GNSS APIs (no GnssManagerService: accepted, never called) =================

	public GnssCapabilities getGnssCapabilities() {
		return mService.getGnssCapabilities();
	}

	/* LocationManagerService.getGnssYearOfHardware/getGnssHardwareModelName without GNSS: 0 / null */
	public int getGnssYearOfHardware() {
		return 0;
	}

	public String getGnssHardwareModelName() {
		return null;
	}

	public List<GnssAntennaInfo> getGnssAntennaInfos() {
		return null;
	}

	public GpsStatus getGpsStatus(GpsStatus status) {
		if (isChangeEnabledAfter(BLOCK_GPS_STATUS_USAGE_AFTER))
			throw new UnsupportedOperationException("GpsStatus APIs not supported, please use GnssStatus APIs instead");
		// no satellite status ever came (GpsStatusTransport.sGnssStatus stays null); legacy behaviour: never null
		if (status == null)
			status = GpsStatus.createEmpty();
		return status;
	}

	public boolean addGpsStatusListener(GpsStatus.Listener listener) {
		if (isChangeEnabledAfter(BLOCK_GPS_STATUS_USAGE_AFTER))
			throw new UnsupportedOperationException("GpsStatus APIs not supported, please use GnssStatus APIs instead");
		new Handler(); // GpsStatusTransport(new HandlerExecutor(new Handler()), ...)
		return true;
	}

	public void removeGpsStatusListener(GpsStatus.Listener listener) {
		if (isChangeEnabledAfter(BLOCK_GPS_STATUS_USAGE_AFTER))
			throw new UnsupportedOperationException("GpsStatus APIs not supported, please use GnssStatus APIs instead");
	}

	public boolean registerGnssStatusCallback(GnssStatus.Callback callback) {
		return registerGnssStatusCallback(callback, null);
	}

	public boolean registerGnssStatusCallback(GnssStatus.Callback callback, Handler handler) {
		if (handler == null)
			handler = new Handler();
		return registerGnssStatusCallback(new HandlerExecutor(handler), callback);
	}

	public boolean registerGnssStatusCallback(Executor executor, GnssStatus.Callback callback) {
		checkTransport(executor, callback, "invalid null callback");
		return true;
	}

	public void unregisterGnssStatusCallback(GnssStatus.Callback callback) {}

	public boolean addNmeaListener(GpsStatus.NmeaListener listener) {
		return false;
	}

	public void removeNmeaListener(GpsStatus.NmeaListener listener) {}

	public boolean addNmeaListener(OnNmeaMessageListener listener) {
		return addNmeaListener(listener, null);
	}

	public boolean addNmeaListener(OnNmeaMessageListener listener, Handler handler) {
		if (handler == null)
			handler = new Handler();
		return addNmeaListener(new HandlerExecutor(handler), listener);
	}

	public boolean addNmeaListener(Executor executor, OnNmeaMessageListener listener) {
		checkTransport(executor, listener, "invalid null listener");
		return true;
	}

	public void removeNmeaListener(OnNmeaMessageListener listener) {}

	public boolean registerGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback) {
		return registerGnssMeasurementsCallback(Runnable::run, callback); // DIRECT_EXECUTOR
	}

	public boolean registerGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback, Handler handler) {
		if (handler == null)
			handler = new Handler();
		return registerGnssMeasurementsCallback(new GnssMeasurementRequest.Builder().build(), new HandlerExecutor(handler), callback);
	}

	public boolean registerGnssMeasurementsCallback(Executor executor, GnssMeasurementsEvent.Callback callback) {
		return registerGnssMeasurementsCallback(new GnssMeasurementRequest.Builder().build(), executor, callback);
	}

	public boolean registerGnssMeasurementsCallback(GnssMeasurementRequest request, Executor executor,
	                                                GnssMeasurementsEvent.Callback callback) {
		checkTransport(executor, callback, "invalid null callback");
		Preconditions.checkArgument(request != null, "invalid null request");
		return true;
	}

	public void unregisterGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback) {}

	public boolean registerAntennaInfoListener(Executor executor, GnssAntennaInfo.Listener listener) {
		checkTransport(executor, listener, "invalid null listener");
		return true;
	}

	public void unregisterAntennaInfoListener(GnssAntennaInfo.Listener listener) {}

	public boolean registerGnssNavigationMessageCallback(GnssNavigationMessage.Callback callback) {
		return registerGnssNavigationMessageCallback(Runnable::run, callback); // DIRECT_EXECUTOR
	}

	public boolean registerGnssNavigationMessageCallback(GnssNavigationMessage.Callback callback, Handler handler) {
		if (handler == null)
			handler = new Handler();
		return registerGnssNavigationMessageCallback(new HandlerExecutor(handler), callback);
	}

	public boolean registerGnssNavigationMessageCallback(Executor executor, GnssNavigationMessage.Callback callback) {
		checkTransport(executor, callback, "invalid null callback");
		return true;
	}

	public void unregisterGnssNavigationMessageCallback(GnssNavigationMessage.Callback callback) {}

	/* the checks of the GNSS transports' constructors */
	private static void checkTransport(Executor executor, Object listener, String nullListenerMessage) {
		Preconditions.checkArgument(executor != null, "invalid null executor");
		Preconditions.checkArgument(listener != null, nullListenerMessage);
	}

	/* ================= transports ================= */

	private static class GetCurrentLocationTransport implements LocationManagerService.LocationCallback {
		private final Executor mExecutor;
		volatile Consumer<Location> mConsumer;

		GetCurrentLocationTransport(Executor executor, Consumer<Location> consumer, CancellationSignal cancellationSignal) {
			Preconditions.checkArgument(executor != null, "illegal null executor");
			Preconditions.checkArgument(consumer != null, "illegal null consumer");
			mExecutor = executor;
			mConsumer = consumer;
		}

		void onCancel() {
			mConsumer = null;
		}

		@Override
		public void onLocation(final Location location) {
			if (mConsumer == null)
				return;
			mExecutor.execute(new Runnable() {
				public void run() {
					Consumer<Location> consumer = mConsumer;
					if (consumer == null)
						return;
					mConsumer = null;
					consumer.accept(location);
				}
			});
		}
	}

	private static class LocationListenerTransport implements LocationManagerService.LocationTransport {
		private Executor mExecutor;
		private volatile LocationListener mListener;

		LocationListenerTransport(LocationListener listener, Executor executor) {
			Preconditions.checkArgument(listener != null, "invalid null listener");
			mListener = listener;
			setExecutor(executor);
		}

		void setExecutor(Executor executor) {
			Preconditions.checkArgument(executor != null, "invalid null executor");
			mExecutor = executor;
		}

		boolean isRegistered() {
			return mListener != null;
		}

		void unregister() {
			mListener = null;
		}

		/* ListenerExecutor.executeSafely(): run on the executor if the listener is still registered then */
		private void execute(final Consumer<LocationListener> operation) {
			if (mListener == null)
				return;
			mExecutor.execute(new Runnable() {
				public void run() {
					LocationListener listener = mListener;
					if (listener != null)
						operation.accept(listener);
				}
			});
		}

		@Override
		public void onLocationChanged(final List<Location> locations) {
			execute(listener -> listener.onLocationChanged(locations));
		}

		@Override
		public void onFlushComplete(final int requestCode) {
			execute(listener -> listener.onFlushComplete(requestCode));
		}

		@Override
		public void onProviderEnabledChanged(final String provider, final boolean enabled) {
			execute(listener -> {
				if (enabled) {
					listener.onProviderEnabled(provider);
				} else {
					listener.onProviderDisabled(provider);
				}
			});
		}
	}
}
