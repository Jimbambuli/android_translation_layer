package com.android.server.location.provider;

import android.atl.annotation.Export;
import android.location.Location;
import android.location.LocationRequest;

import com.android.server.location.LocationManagerService;

/*
 * ATL's one location source, the desktop's position: in a Flatpak the location portal (org.freedesktop.portal.Location,
 * GeoClue behind it), outside GeoClue2 on the system bus (the camera's pattern: portal in the sandbox, direct outside).
 * It takes the place of AOSP's network location provider (a ProxyLocationProvider bound to the network location app,
 * the fused provider builds on it). The device has no GNSS hardware (GnssNative.isSupported() false), so there is no
 * "gps" provider and no GNSS data.
 *
 * One session runs while a network or fused registration is active; its accuracy follows the merged request
 * (LocationProviderManager.mergeRegistrations: the best quality asked for).
 */
public final class DesktopLocationProvider {

	/* the portal's accuracy levels (org.freedesktop.portal.Location "accuracy", libportal XdpLocationAccuracy) */
	public static final int ACCURACY_CITY = 2;
	public static final int ACCURACY_STREET = 4;
	public static final int ACCURACY_EXACT = 5;

	private static Boolean available;
	private static boolean denied;
	private static int sessionAccuracy = -1; // the running session's accuracy, -1 none

	private DesktopLocationProvider() {}

	/* LocationRequest quality -> accuracy: GeoClue's levels by source (EXACT GPS, STREET WiFi, CITY GeoIP) match the
	 * qualities' documented ranges (high accuracy, block level, city level) */
	public static int accuracyForQuality(int quality) {
		switch (quality) {
			case LocationRequest.QUALITY_HIGH_ACCURACY:
				return ACCURACY_EXACT;
			case LocationRequest.QUALITY_BALANCED_POWER_ACCURACY:
				return ACCURACY_STREET;
			default:
				return ACCURACY_CITY;
		}
	}

	/** whether the desktop has a location service: the portal in a Flatpak (it answers the access question
	 * itself), else GeoClue2 on the system bus */
	public static synchronized boolean isAvailable() {
		if (available == null)
			available = nativeIsAvailable();
		return available;
	}

	/** the user refused this app the location (portal access dialog or GeoClue's agent) */
	public static synchronized boolean isDenied() {
		return denied;
	}

	/** start, change or (accuracy -1) stop the desktop's session */
	public static synchronized void setRequest(int accuracy) {
		if (denied || !isAvailable())
			accuracy = -1;
		if (accuracy == sessionAccuracy)
			return;
		if (sessionAccuracy >= 0)
			nativeStop();
		sessionAccuracy = accuracy;
		if (accuracy >= 0)
			nativeStart(accuracy);
	}

	private static native boolean nativeIsAvailable();
	private static native void nativeStart(int accuracy);
	private static native void nativeStop();

	/* the portal's LocationUpdated / GeoClue's Location: timestamp in ms since the epoch, elapsedRealtimeNanos when it
	 * came; GeoClue marks unknown altitude with -DBL_MAX, unknown speed and heading with -1 */
	@Export
	static void locationUpdated(double latitude, double longitude, double altitude, double accuracy,
	                            double speed, double bearing, long timestamp, long elapsedRealtimeNanos) {
		Location location = new Location((String)null);
		location.setLatitude(latitude);
		location.setLongitude(longitude);
		if (altitude != -Double.MAX_VALUE)
			location.setAltitude(altitude);
		location.setAccuracy((float)accuracy);
		if (speed >= 0)
			location.setSpeed((float)speed);
		if (bearing >= 0)
			location.setBearing((float)bearing);
		location.setTime(timestamp);
		location.setElapsedRealtimeNanos(elapsedRealtimeNanos);
		LocationManagerService.get().onDesktopLocation(location);
	}

	@Export
	static void locationDenied() {
		synchronized (DesktopLocationProvider.class) {
			denied = true;
			sessionAccuracy = -1;
		}
		LocationManagerService.get().onDesktopDenied();
	}
}
