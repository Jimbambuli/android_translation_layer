package com.android.server.location;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GnssCapabilities;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.LocationRequest;
import android.location.provider.ProviderProperties;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import com.android.internal.util.Preconditions;
import com.android.server.location.provider.DesktopLocationProvider;
import com.android.server.location.provider.LocationProviderManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
 * AOSP 16 LocationManagerService (services/core/java/com/android/server/location/LocationManagerService.java) in the
 * app's process, for LocationManager. The providers are those of a device without GNSS hardware with a network
 * location provider (onSystemThirdPartyAppsCanStart): "passive", "network" (the desktop, DesktopLocationProvider) and
 * "fused" (AOSP's FusedLocationProvider over the network provider). Without GnssManagerService every GNSS call is
 * accepted and nothing is ever reported (see LocationManager).
 */
public class LocationManagerService {

	/* LocationPermissions.PERMISSION_* */
	public static final int PERMISSION_NONE = 0;
	public static final int PERMISSION_COARSE = 1;
	public static final int PERMISSION_FINE = 2;

	/* ILocationListener */
	public interface LocationTransport {
		void onLocationChanged(List<Location> locations);
		void onFlushComplete(int requestCode);
		void onProviderEnabledChanged(String provider, boolean enabled);
	}

	/* ILocationCallback */
	public interface LocationCallback {
		void onLocation(Location location);
	}

	/* PassiveLocationProvider.PROPERTIES */
	private static final ProviderProperties PASSIVE_PROPERTIES = new ProviderProperties.Builder()
	        .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
	        .setAccuracy(ProviderProperties.ACCURACY_FINE)
	        .build();
	/* FusedLocationProvider.PROPERTIES (packages/FusedLocation) */
	private static final ProviderProperties FUSED_PROPERTIES = new ProviderProperties.Builder()
	        .setHasAltitudeSupport(true)
	        .setHasSpeedSupport(true)
	        .setHasBearingSupport(true)
	        .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
	        .setAccuracy(ProviderProperties.ACCURACY_FINE)
	        .build();
	/* the desktop as a network location provider: needs the network (WiFi/GeoIP lookups), GeoClue reports altitude,
	 * speed and heading when its source knows them; coarse like the network providers of microG and Play services */
	private static final ProviderProperties NETWORK_PROPERTIES = new ProviderProperties.Builder()
	        .setHasNetworkRequirement(true)
	        .setHasAltitudeSupport(true)
	        .setHasSpeedSupport(true)
	        .setHasBearingSupport(true)
	        .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
	        .setAccuracy(ProviderProperties.ACCURACY_COARSE)
	        .build();

	private static LocationManagerService sInstance;

	public final Object mLock = new Object();
	private final Handler mHandler = new Handler(Looper.getMainLooper()); // AOSP AlarmHelper
	private final LocationProviderManager mPassiveManager;
	private final LocationProviderManager mNetworkManager;
	private final LocationProviderManager mFusedManager;
	private final List<LocationProviderManager> mProviderManagers;
	private Context mContext;

	public static synchronized LocationManagerService get() {
		if (sInstance == null)
			sInstance = new LocationManagerService();
		return sInstance;
	}

	private LocationManagerService() {
		mPassiveManager = new LocationProviderManager(this, LocationManager.PASSIVE_PROVIDER, PASSIVE_PROPERTIES, null);
		mNetworkManager = new LocationProviderManager(this, LocationManager.NETWORK_PROVIDER, NETWORK_PROPERTIES, mPassiveManager);
		mFusedManager = new LocationProviderManager(this, LocationManager.FUSED_PROVIDER, FUSED_PROPERTIES, mPassiveManager);
		mProviderManagers = Arrays.asList(mPassiveManager, mNetworkManager, mFusedManager);
	}

	public void setContext(Context context) {
		if (mContext == null && context != null)
			mContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
	}

	public Context getContext() {
		return mContext;
	}

	public Handler getHandler() {
		return mHandler;
	}

	public int getTargetSdk() {
		return mContext.getApplicationInfo().targetSdkVersion;
	}

	public static void runAll(List<Runnable> ops) {
		for (Runnable op : ops)
			op.run();
	}

	private LocationProviderManager getLocationProviderManager(String provider) {
		if (provider == null)
			return null;
		for (LocationProviderManager manager : mProviderManagers)
			if (provider.equals(manager.getName()))
				return manager;
		return null;
	}

	/* ---- permissions (LocationPermissions) ---- */

	public int getPermissionLevel() {
		if (mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
			return PERMISSION_FINE;
		if (mContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
			return PERMISSION_COARSE;
		return PERMISSION_NONE;
	}

	private static void enforceLocationPermission(int permissionLevel, int requiredPermissionLevel) {
		if (permissionLevel >= requiredPermissionLevel)
			return;
		int uid = Process.myUid();
		if (requiredPermissionLevel == PERMISSION_COARSE) {
			throw new SecurityException("uid " + uid + " does not have " + Manifest.permission.ACCESS_COARSE_LOCATION
			                            + " or " + Manifest.permission.ACCESS_FINE_LOCATION + ".");
		} else if (requiredPermissionLevel == PERMISSION_FINE) {
			throw new SecurityException("uid " + uid + " does not have " + Manifest.permission.ACCESS_FINE_LOCATION + ".");
		}
	}

	/* ---- settings: location is on when the desktop has a location service ---- */

	public boolean isLocationEnabled() {
		return DesktopLocationProvider.isAvailable();
	}

	public boolean isProviderEnabled(String provider) {
		LocationProviderManager manager = getLocationProviderManager(provider);
		return manager != null && manager.isEnabled();
	}

	/* ---- providers ---- */

	public boolean hasProvider(String provider) {
		return getLocationProviderManager(provider) != null;
	}

	public List<String> getAllProviders() {
		ArrayList<String> providers = new ArrayList<>(mProviderManagers.size());
		for (LocationProviderManager manager : mProviderManagers)
			providers.add(manager.getName());
		return providers;
	}

	public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
		if (getPermissionLevel() < PERMISSION_COARSE)
			return Collections.emptyList();
		ArrayList<String> providers = new ArrayList<>(mProviderManagers.size());
		for (LocationProviderManager manager : mProviderManagers) {
			String name = manager.getName();
			if (enabledOnly && !manager.isEnabled())
				continue;
			if (criteria != null && !LocationProvider.propertiesMeetCriteria(name, manager.getProperties(), criteria))
				continue;
			providers.add(name);
		}
		return providers;
	}

	public String getBestProvider(Criteria criteria, boolean enabledOnly) {
		List<String> providers = getProviders(criteria, enabledOnly);
		if (providers.isEmpty())
			providers = getProviders(null, enabledOnly);
		if (!providers.isEmpty()) {
			if (providers.contains(LocationManager.FUSED_PROVIDER)) {
				return LocationManager.FUSED_PROVIDER;
			} else if (providers.contains(LocationManager.GPS_PROVIDER)) {
				return LocationManager.GPS_PROVIDER;
			} else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
				return LocationManager.NETWORK_PROVIDER;
			} else {
				return providers.get(0);
			}
		}
		return null;
	}

	public ProviderProperties getProviderProperties(String provider) {
		LocationProviderManager manager = getLocationProviderManager(provider);
		Preconditions.checkArgument(manager != null, "provider \"%s\" does not exist", provider);
		return manager.getProperties();
	}

	/* ---- locations ---- */

	public Location getLastLocation(String provider) {
		int permissionLevel = getPermissionLevel();
		enforceLocationPermission(permissionLevel, PERMISSION_COARSE);
		LocationProviderManager manager = getLocationProviderManager(provider);
		if (manager == null)
			return null;
		synchronized (mLock) {
			return manager.getLastLocation(permissionLevel);
		}
	}

	private LocationProviderManager checkedManager(String provider, int permissionLevel) {
		enforceLocationPermission(permissionLevel, PERMISSION_COARSE);
		LocationProviderManager manager = getLocationProviderManager(provider);
		Preconditions.checkArgument(manager != null, "provider \"%s\" does not exist", provider);
		return manager;
	}

	/* validateLocationRequest(): without LOCATION_HARDWARE low power is cleared */
	private static LocationRequest sanitize(LocationRequest request) {
		return new LocationRequest.Builder(request).setLowPower(false).build();
	}

	public void registerLocationListener(String provider, LocationRequest request, LocationTransport transport) {
		int permissionLevel = getPermissionLevel();
		LocationProviderManager manager = checkedManager(provider, permissionLevel);
		request = sanitize(request);
		List<Runnable> ops = new ArrayList<>();
		synchronized (mLock) {
			manager.registerLocationRequest(request, permissionLevel, transport, ops);
		}
		runAll(ops);
	}

	public void registerLocationPendingIntent(String provider, LocationRequest request, PendingIntent pendingIntent) {
		int permissionLevel = getPermissionLevel();
		enforceLocationPermission(permissionLevel, PERMISSION_COARSE);
		if (getTargetSdk() > android.os.Build.VERSION_CODES.R) { // BLOCK_PENDING_INTENT_SYSTEM_API_USAGE
			if (request.isLowPower() || request.isHiddenFromAppOps() || request.isLocationSettingsIgnored())
				throw new SecurityException("PendingIntent location requests may not use system APIs: " + request);
		}
		LocationProviderManager manager = checkedManager(provider, permissionLevel);
		request = sanitize(request);
		List<Runnable> ops = new ArrayList<>();
		synchronized (mLock) {
			manager.registerLocationRequest(request, permissionLevel, pendingIntent, ops);
		}
		runAll(ops);
	}

	/* returns the cancellation (ICancellationSignal) */
	public Runnable getCurrentLocation(String provider, LocationRequest request, LocationCallback callback) {
		int permissionLevel = getPermissionLevel();
		final LocationProviderManager manager = checkedManager(provider, permissionLevel);
		request = sanitize(request);
		List<Runnable> ops = new ArrayList<>();
		final Object key;
		synchronized (mLock) {
			key = manager.getCurrentLocation(request, permissionLevel, callback, ops);
		}
		runAll(ops);
		return new Runnable() {
			public void run() {
				synchronized (mLock) {
					manager.unregisterLocationRequest(key);
				}
			}
		};
	}

	public void requestFlush(String provider, Object listenerOrPendingIntent, int requestCode) {
		LocationProviderManager manager = getLocationProviderManager(provider);
		Preconditions.checkArgument(manager != null, "provider \"%s\" does not exist", provider);
		List<Runnable> ops = new ArrayList<>();
		synchronized (mLock) {
			manager.flush(listenerOrPendingIntent, requestCode, ops);
		}
		runAll(ops);
	}

	public void unregisterLocation(Object listenerOrPendingIntent) {
		synchronized (mLock) {
			for (LocationProviderManager manager : mProviderManagers)
				manager.unregisterLocationRequest(listenerOrPendingIntent);
		}
	}

	public void sendExtraCommand(String provider, String command) {
		enforceLocationPermission(getPermissionLevel(), PERMISSION_COARSE);
		mContext.enforceCallingOrSelfPermission(Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS, null);
		// the providers know no extra commands
	}

	/* ---- GNSS: no GnssManagerService ---- */

	public GnssCapabilities getGnssCapabilities() {
		return new GnssCapabilities.Builder().build();
	}

	/* ---- the desktop ---- */

	/* any registration came or went: the desktop session follows the merged request of network and fused (the
	 * fused provider asks the network provider for its own request) */
	public void onRegistrationsChanged() {
		DesktopLocationProvider.setRequest(Math.max(mNetworkManager.getDesktopAccuracy(), mFusedManager.getDesktopAccuracy()));
	}

	public void onDesktopLocation(Location location) {
		List<Runnable> ops = new ArrayList<>();
		synchronized (mLock) {
			mNetworkManager.onReportLocation(location, ops);
			mFusedManager.onReportLocation(location, ops);
		}
		runAll(ops);
	}

	/* the access question was answered with no: the permissions are gone (PackageManager), the registrations stay
	 * but are no longer active, like AOSP when a permission is revoked */
	public void onDesktopDenied() {
		synchronized (mLock) {
			onRegistrationsChanged();
		}
	}
}
