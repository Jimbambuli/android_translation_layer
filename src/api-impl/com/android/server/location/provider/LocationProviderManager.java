package com.android.server.location.provider;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationRequest;
import android.location.provider.ProviderProperties;
import android.os.SystemClock;

import com.android.server.location.LocationManagerService;
import com.android.server.location.LocationManagerService.LocationCallback;
import com.android.server.location.LocationManagerService.LocationTransport;
import com.android.server.location.fudger.LocationFudger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/*
 * One provider's registrations, after AOSP 16 services/.../location/provider/LocationProviderManager.java (ATL runs
 * the service in the app's process): the request a registration gets (coarse permission: low power, at most every
 * 10 min), the delivery filter (interval with jitter, distance, max updates, expiry), getCurrentLocation (last
 * location of at most 30 s, else the next one, null after at most 30 s), the last fine and coarse locations, flush
 * and the provider enabled callbacks. Left out, because ATL has nothing they depend on: wake locks, app ops,
 * background throttling (the app's window is always the foreground), power save modes, users, bypass requests and
 * mock providers (no app can be the mock location app, see LocationManagerService.addTestProvider()).
 *
 * All state is guarded by the service's lock; deliveries are collected under it and run after it is released.
 */
public class LocationProviderManager {

	private static final long MIN_COARSE_INTERVAL_MS = 10 * 60 * 1000;
	private static final long MAX_CURRENT_LOCATION_AGE_MS = 30 * 1000;
	private static final long MAX_GET_CURRENT_LOCATION_TIMEOUT_MS = 30 * 1000;
	private static final float FASTEST_INTERVAL_JITTER_PERCENTAGE = .10f;
	private static final int MAX_FASTEST_INTERVAL_JITTER_MS = 30 * 1000;
	private static final long MIN_REQUEST_DELAY_MS = 30 * 1000;
	/* SystemSettingsHelper.DEFAULT_COARSE_LOCATION_ACCURACY_M */
	private static final float DEFAULT_COARSE_LOCATION_ACCURACY_M = 2000.0f;

	protected final LocationManagerService mService;
	protected final String mName;
	private final ProviderProperties mProperties;
	private final LocationProviderManager mPassiveManager;
	private final LocationFudger mLocationFudger = new LocationFudger(DEFAULT_COARSE_LOCATION_ACCURACY_M);
	private final ArrayList<Registration> mRegistrations = new ArrayList<>();

	private Location mLastFine;
	private Location mLastCoarse;

	public LocationProviderManager(LocationManagerService service, String name, ProviderProperties properties,
	                               LocationProviderManager passiveManager) {
		mService = service;
		mName = name;
		mProperties = properties;
		mPassiveManager = passiveManager;
	}

	public String getName() {
		return mName;
	}

	public ProviderProperties getProperties() {
		return mProperties;
	}

	public boolean isEnabled() {
		return mService.isLocationEnabled();
	}

	/* ---- registrations ---- */

	protected abstract class Registration implements Runnable {
		final Object mKey;
		final LocationRequest mBaseRequest;
		final int mPermissionLevel;
		LocationRequest mRequest;
		long mExpirationRealtimeMs = Long.MAX_VALUE;
		Location mLastDelivered;
		int mNumLocationsDelivered;
		boolean mRemoved;

		Registration(Object key, LocationRequest request, int permissionLevel) {
			mKey = key;
			mBaseRequest = request;
			mPermissionLevel = permissionLevel;
			mRequest = calculateProviderLocationRequest();
		}

		/* AOSP Registration.calculateProviderLocationRequest() */
		private LocationRequest calculateProviderLocationRequest() {
			LocationRequest.Builder builder = new LocationRequest.Builder(mBaseRequest);
			if (mPermissionLevel < LocationManagerService.PERMISSION_FINE) {
				builder.setQuality(LocationRequest.QUALITY_LOW_POWER);
				if (mBaseRequest.getIntervalMillis() < MIN_COARSE_INTERVAL_MS) {
					builder.setIntervalMillis(MIN_COARSE_INTERVAL_MS);
				}
				if (mBaseRequest.getMinUpdateIntervalMillis() < MIN_COARSE_INTERVAL_MS) {
					builder.setMinUpdateIntervalMillis(MIN_COARSE_INTERVAL_MS);
				}
			}
			return builder.build();
		}

		LocationRequest getRequest() {
			return mRequest;
		}

		/* permitted (the permission was not withdrawn, e.g. by the portal's access dialog) and enabled */
		boolean isActive() {
			return mService.getPermissionLevel() >= LocationManagerService.PERMISSION_COARSE && isEnabled();
		}

		void onRegister(List<Runnable> ops) {
			long registerTimeMs = SystemClock.elapsedRealtime();
			mExpirationRealtimeMs = getRequest().getExpirationRealtimeMs(registerTimeMs);
			if (mExpirationRealtimeMs <= registerTimeMs) {
				onExpired(ops);
			} else if (mExpirationRealtimeMs < Long.MAX_VALUE) {
				mService.getHandler().postDelayed(this, mExpirationRealtimeMs - registerTimeMs);
			}
		}

		void onUnregister() {
			mRemoved = true;
			mService.getHandler().removeCallbacks(this);
		}

		/* the expiry alarm (AOSP onAlarm) */
		@Override
		public void run() {
			List<Runnable> ops = new ArrayList<>();
			synchronized (mService.mLock) {
				if (mRemoved)
					return;
				mExpirationRealtimeMs = Long.MAX_VALUE;
				onExpired(ops);
			}
			LocationManagerService.runAll(ops);
		}

		abstract void onExpired(List<Runnable> ops);

		/* returns the delivery or null */
		abstract Runnable acceptLocationChange(Location fineLocation);

		void flush(int requestCode, List<Runnable> ops) {}

		void onProviderEnabledChanged(boolean enabled, List<Runnable> ops) {}

		void remove() {
			if (mRegistrations.remove(this)) {
				onUnregister();
				mService.onRegistrationsChanged();
			}
		}
	}

	protected abstract class LocationRegistration extends Registration {
		LocationRegistration(Object key, LocationRequest request, int permissionLevel) {
			super(key, request, permissionLevel);
		}

		@Override
		void onRegister(List<Runnable> ops) {
			super.onRegister(ops);
			if (mRemoved)
				return;
			if (!isEnabled())
				onProviderEnabledChanged(false, ops);
			if (isActive())
				onActive(ops);
		}

		/* DELIVER_HISTORICAL_LOCATIONS (enabled after R): a last location young enough for the interval */
		void onActive(List<Runnable> ops) {
			if (mService.getTargetSdk() <= android.os.Build.VERSION_CODES.R)
				return;
			long maxLocationAgeMs = getRequest().getIntervalMillis();
			if (mLastDelivered != null)
				maxLocationAgeMs = Math.min(maxLocationAgeMs, mLastDelivered.getElapsedRealtimeAgeMillis() - 1);
			if (maxLocationAgeMs > MIN_REQUEST_DELAY_MS) {
				Location lastLocation = getLastLocationUnsafe(LocationManagerService.PERMISSION_FINE, maxLocationAgeMs);
				if (lastLocation != null) {
					Runnable op = acceptLocationChange(lastLocation);
					if (op != null)
						ops.add(op);
				}
			}
		}

		@Override
		void onExpired(List<Runnable> ops) {
			remove();
		}

		@Override
		Runnable acceptLocationChange(Location fineLocation) {
			if (SystemClock.elapsedRealtime() >= mExpirationRealtimeMs) {
				remove();
				return null;
			}
			final Location location = getPermittedLocation(fineLocation, mPermissionLevel);
			if (Double.isNaN(location.getLatitude()) || location.getLatitude() < -90 || location.getLatitude() > 90
			    || Double.isNaN(location.getLongitude()) || location.getLongitude() < -180 || location.getLongitude() > 180)
				return null;
			if (mLastDelivered != null) {
				long deltaMs = location.getElapsedRealtimeMillis() - mLastDelivered.getElapsedRealtimeMillis();
				long maxJitterMs = Math.min((long)(FASTEST_INTERVAL_JITTER_PERCENTAGE * getRequest().getIntervalMillis()),
				                            MAX_FASTEST_INTERVAL_JITTER_MS);
				if (deltaMs < getRequest().getMinUpdateIntervalMillis() - maxJitterMs)
					return null;
				double smallestDisplacementM = getRequest().getMinUpdateDistanceMeters();
				if (smallestDisplacementM > 0.0 && location.distanceTo(mLastDelivered) <= smallestDisplacementM)
					return null;
			}
			mLastDelivered = location;
			if (++mNumLocationsDelivered >= getRequest().getMaxUpdates())
				remove();
			final Location copy = new Location(location);
			return new Runnable() {
				public void run() {
					deliverOnLocationChanged(copy);
				}
			};
		}

		abstract void deliverOnLocationChanged(Location location);
	}

	protected final class LocationListenerRegistration extends LocationRegistration {
		private final LocationTransport mTransport;

		LocationListenerRegistration(LocationTransport transport, LocationRequest request, int permissionLevel) {
			super(transport, request, permissionLevel);
			mTransport = transport;
		}

		@Override
		void deliverOnLocationChanged(Location location) {
			mTransport.onLocationChanged(Collections.singletonList(location));
		}

		@Override
		void flush(final int requestCode, List<Runnable> ops) {
			ops.add(new Runnable() {
				public void run() {
					mTransport.onFlushComplete(requestCode);
				}
			});
		}

		@Override
		void onProviderEnabledChanged(final boolean enabled, List<Runnable> ops) {
			ops.add(new Runnable() {
				public void run() {
					mTransport.onProviderEnabledChanged(mName, enabled);
				}
			});
		}
	}

	protected final class LocationPendingIntentRegistration extends LocationRegistration {
		private final PendingIntent mPendingIntent;

		LocationPendingIntentRegistration(PendingIntent pendingIntent, LocationRequest request, int permissionLevel) {
			super(pendingIntent, request, permissionLevel);
			mPendingIntent = pendingIntent;
		}

		private void send(Intent intent) {
			try {
				mPendingIntent.send(mService.getContext(), 0, intent);
			} catch (PendingIntent.CanceledException e) {
				// AOSP onOperationFailure: a cancelled pending intent ends the registration
				synchronized (mService.mLock) {
					remove();
				}
			}
		}

		@Override
		void deliverOnLocationChanged(Location location) {
			send(new Intent().putExtra(android.location.LocationManager.KEY_LOCATION_CHANGED, location));
		}

		@Override
		void flush(final int requestCode, List<Runnable> ops) {
			ops.add(new Runnable() {
				public void run() {
					send(new Intent().putExtra(android.location.LocationManager.KEY_FLUSH_COMPLETE, requestCode));
				}
			});
		}

		@Override
		void onProviderEnabledChanged(final boolean enabled, List<Runnable> ops) {
			ops.add(new Runnable() {
				public void run() {
					send(new Intent().putExtra(android.location.LocationManager.KEY_PROVIDER_ENABLED, enabled));
				}
			});
		}
	}

	protected final class GetCurrentLocationListenerRegistration extends Registration {
		private final LocationCallback mCallback;

		GetCurrentLocationListenerRegistration(LocationCallback callback, LocationRequest request, int permissionLevel) {
			super(callback, request, permissionLevel);
			mCallback = callback;
		}

		@Override
		void onRegister(List<Runnable> ops) {
			super.onRegister(ops);
			if (mRemoved)
				return;
			if (!isActive()) {
				ops.add(deliver(null)); // AOSP: registerAndHandleIdentity -> deliverNull() when not active
				return;
			}
			Location lastLocation = getLastLocationUnsafe(LocationManagerService.PERMISSION_FINE, MAX_CURRENT_LOCATION_AGE_MS);
			if (lastLocation != null)
				ops.add(acceptLocationChange(lastLocation));
		}

		@Override
		void onExpired(List<Runnable> ops) {
			ops.add(deliver(null));
		}

		@Override
		Runnable acceptLocationChange(Location fineLocation) {
			if (SystemClock.elapsedRealtime() >= mExpirationRealtimeMs)
				fineLocation = null;
			return deliver(fineLocation != null ? getPermittedLocation(fineLocation, mPermissionLevel) : null);
		}

		private Runnable deliver(Location location) {
			remove();
			final Location copy = location != null ? new Location(location) : null;
			return new Runnable() {
				public void run() {
					mCallback.onLocation(copy);
				}
			};
		}
	}

	/* ---- the service's calls (under mService.mLock) ---- */

	private Registration findRegistration(Object key) {
		for (Registration r : mRegistrations)
			if (r.mKey.equals(key))
				return r;
		return null;
	}

	private void putRegistration(Registration registration, List<Runnable> ops) {
		Registration old = findRegistration(registration.mKey);
		if (old != null) {
			mRegistrations.remove(old);
			old.onUnregister();
		}
		mRegistrations.add(registration);
		registration.onRegister(ops);
		mService.onRegistrationsChanged();
	}

	public void registerLocationRequest(LocationRequest request, int permissionLevel, LocationTransport transport,
	                                    List<Runnable> ops) {
		putRegistration(new LocationListenerRegistration(transport, request, permissionLevel), ops);
	}

	public void registerLocationRequest(LocationRequest request, int permissionLevel, PendingIntent pendingIntent,
	                                    List<Runnable> ops) {
		putRegistration(new LocationPendingIntentRegistration(pendingIntent, request, permissionLevel), ops);
	}

	/* returns the registration's key for the cancellation */
	public Object getCurrentLocation(LocationRequest request, int permissionLevel, LocationCallback callback,
	                                 List<Runnable> ops) {
		if (request.getDurationMillis() > MAX_GET_CURRENT_LOCATION_TIMEOUT_MS) {
			request = new LocationRequest.Builder(request)
			              .setDurationMillis(MAX_GET_CURRENT_LOCATION_TIMEOUT_MS)
			              .build();
		}
		putRegistration(new GetCurrentLocationListenerRegistration(callback, request, permissionLevel), ops);
		return callback;
	}

	public void unregisterLocationRequest(Object key) {
		Registration r = findRegistration(key);
		if (r != null)
			r.remove();
	}

	/* AOSP flush(): IllegalArgumentException for an unknown listener */
	public void flush(Object key, int requestCode, List<Runnable> ops) {
		Registration r = findRegistration(key);
		if (r == null)
			throw new IllegalArgumentException(key instanceof PendingIntent
			                                       ? "unregistered pending intent cannot be flushed"
			                                       : "unregistered listener cannot be flushed");
		r.flush(requestCode, ops);
	}

	public Location getLastLocation(int permissionLevel) {
		if (!isEnabled())
			return null;
		Location location = getPermittedLocation(getLastLocationUnsafe(permissionLevel, Long.MAX_VALUE), permissionLevel);
		return location != null ? new Location(location) : null;
	}

	/* fine even for a coarse level: the caller coarsens (AOSP getLastLocationUnsafe) */
	private Location getLastLocationUnsafe(int permissionLevel, long maximumAgeMs) {
		Location location = permissionLevel == LocationManagerService.PERMISSION_FINE ? mLastFine : mLastCoarse;
		if (location == null || location.getElapsedRealtimeAgeMillis() > maximumAgeMs)
			return null;
		return location;
	}

	private Location getPermittedLocation(Location fineLocation, int permissionLevel) {
		if (fineLocation == null || permissionLevel == LocationManagerService.PERMISSION_FINE)
			return fineLocation;
		return mLocationFudger.createCoarse(fineLocation);
	}

	/* AOSP LastLocation.set(): the coarse one moves on at most every 10 min */
	private void setLastLocation(Location location) {
		if (mLastFine == null || location.getElapsedRealtimeNanos() > mLastFine.getElapsedRealtimeNanos())
			mLastFine = location;
		if (mLastCoarse == null
		    || location.getElapsedRealtimeMillis() - MIN_COARSE_INTERVAL_MS > mLastCoarse.getElapsedRealtimeMillis())
			mLastCoarse = location;
	}

	/* a new location of this provider (AOSP onReportLocation): last location, the active registrations, then the
	 * passive provider */
	public void onReportLocation(Location fix, List<Runnable> ops) {
		Location location = new Location(fix);
		location.setProvider(mName);
		if (isEnabled())
			setLastLocation(location);
		for (Registration r : new ArrayList<>(mRegistrations)) {
			if (r.mRemoved || !r.isActive())
				continue;
			Runnable op = r.acceptLocationChange(location);
			if (op != null)
				ops.add(op);
		}
		if (mPassiveManager != null)
			mPassiveManager.onReportLocation(location, ops);
	}

	/* the merged request's accuracy for the desktop, -1 without an active non-passive registration
	 * (AOSP mergeRegistrations: passive requests do not contribute) */
	public int getDesktopAccuracy() {
		int accuracy = -1;
		for (Registration r : mRegistrations) {
			if (!r.isActive() || r.getRequest().getIntervalMillis() == LocationRequest.PASSIVE_INTERVAL)
				continue;
			accuracy = Math.max(accuracy, DesktopLocationProvider.accuracyForQuality(r.getRequest().getQuality()));
		}
		return accuracy;
	}
}
