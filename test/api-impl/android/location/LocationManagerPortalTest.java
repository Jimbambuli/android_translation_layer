/*
 * Host test for LocationManager over the location portal: one desktop position goes to every listener as a
 * Location of the provider it asked for, getLastKnownLocation() keeps it, removeUpdates() ends the updates and
 * all of AOSP's providers are listed. The position is passed in like the portal's signal. Run like
 * test/api-impl/android/os/SystemPropertiesTest.java.
 */
package android.location;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import junit.framework.TestCase;

public class LocationManagerPortalTest extends TestCase {
	private static final Executor DIRECT = new Executor() {
		public void execute(Runnable r) {
			r.run();
		}
	};

	private static class Recorder implements LocationListener {
		final List<Location> seen = new ArrayList<>();

		public void onLocationChanged(Location location) {
			seen.add(location);
		}
	}

	public void testPositionReachesEveryListenerWithItsProvider() {
		LocationManager lm = new LocationManager();
		Recorder gps = new Recorder(), network = new Recorder();
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, DIRECT, gps);
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, DIRECT, network);
		LocationManager.locationUpdated(47.3769, 8.5417, 408, 20, 0, 90, 1762200000123L, 5_000_000_000L);
		assertEquals(1, gps.seen.size());
		assertEquals(LocationManager.GPS_PROVIDER, gps.seen.get(0).getProvider());
		assertEquals(LocationManager.NETWORK_PROVIDER, network.seen.get(0).getProvider());
		Location l = gps.seen.get(0);
		assertEquals(47.3769, l.getLatitude(), 1e-9);
		assertEquals(8.5417, l.getLongitude(), 1e-9);
		assertEquals(20f, l.getAccuracy(), 1e-6);
		assertEquals(1762200000123L, l.getTime());
		assertEquals(5_000_000_000L, l.getElapsedRealtimeNanos());
		assertEquals(408, lm.getLastKnownLocation(LocationManager.FUSED_PROVIDER).getAltitude(), 1e-9);
		assertEquals(LocationManager.FUSED_PROVIDER, lm.getLastKnownLocation(LocationManager.FUSED_PROVIDER).getProvider());
		lm.removeUpdates(gps);
		lm.removeUpdates(network);
		LocationManager.locationUpdated(47.0, 8.0, 0, 20, 0, 0, 1762200001000L, 6_000_000_000L);
		assertEquals(1, gps.seen.size());
	}

	public void testProviders() {
		LocationManager lm = new LocationManager();
		assertTrue(lm.getAllProviders().contains(LocationManager.GPS_PROVIDER));
		assertTrue(lm.getAllProviders().contains(LocationManager.NETWORK_PROVIDER));
		assertTrue(lm.getAllProviders().contains(LocationManager.FUSED_PROVIDER));
		assertTrue(lm.getAllProviders().contains(LocationManager.PASSIVE_PROVIDER));
	}
}
