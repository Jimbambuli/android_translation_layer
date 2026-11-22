package android.webkit;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

/* Where the Geolocation API of the pages gets its positions: like Chromium's LocationProviderAndroid
 * (services/device/geolocation), the app's LocationManager, asked with a Criteria (ACCURACY_FINE for
 * enableHighAccuracy while the app holds ACCESS_FINE_LOCATION), updates on the main looper. WebKit's geolocation
 * manager calls start()/stop() (android_webkit_WebView.c); without the app's location permission the page gets
 * the error Chromium reports. The APPROXIMATE_GEOLOCATION_PERMISSION feature path of Chromium is not followed
 * (a feature flag, off by default: nicht geprüft). */
final class AtlGeolocationProvider {
	private static final String TAG = "LocationProvider";
	private final Context context;
	private LocationManager locationManager;
	private LocationListener listener;

	AtlGeolocationProvider(Context context) {
		this.context = context;
	}

	void start(final long manager, boolean enableHighAccuracy) {
		stop();
		if (locationManager == null)
			locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		if (locationManager == null) {
			Log.e(TAG, "Could not get location manager.");
			return;
		}
		boolean highAccuracy = enableHighAccuracy && context.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
		listener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				if (listener != this)
					return;
				WebView.native_geolocationUpdate(manager, location.getTime(), location.getLatitude(), location.getLongitude(),
				                                 location.hasAccuracy() ? location.getAccuracy() : 0,
				                                 location.hasAltitude() ? location.getAltitude() : Double.NaN,
				                                 location.hasVerticalAccuracy() ? location.getVerticalAccuracyMeters() : Double.NaN,
				                                 location.hasBearing() ? location.getBearing() : Double.NaN,
				                                 location.hasSpeed() ? location.getSpeed() : Double.NaN);
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {}

			@Override
			public void onProviderEnabled(String provider) {}

			@Override
			public void onProviderDisabled(String provider) {}
		};
		try {
			Criteria criteria = new Criteria();
			if (highAccuracy)
				criteria.setAccuracy(Criteria.ACCURACY_FINE);
			locationManager.requestLocationUpdates(0, 0, criteria, listener, Looper.getMainLooper());
		} catch (SecurityException e) {
			Log.e(TAG, "Caught security exception while registering for location updates from the system. The application does not have sufficient geolocation permissions.");
			stop();
			WebView.native_geolocationFailed(manager, "application does not have sufficient geolocation permissions.");
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Caught IllegalArgumentException registering for location updates.");
			stop();
		}
	}

	void stop() {
		if (listener != null && locationManager != null)
			locationManager.removeUpdates(listener);
		listener = null;
	}
}
