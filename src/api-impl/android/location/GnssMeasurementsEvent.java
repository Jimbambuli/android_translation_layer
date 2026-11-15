package android.location;

/* AOSP location/java/android/location/GnssMeasurementsEvent.java (android16-release): the callback type apps
 * extend. ATL's positions come without GNSS measurements (see LocationManager.registerGnssStatusCallback()),
 * so no event is made; the event's data (GnssMeasurement) are not part of it. */
public final class GnssMeasurementsEvent {
	private GnssMeasurementsEvent() {}

	public static abstract class Callback {
		public static final int STATUS_NOT_SUPPORTED = 0;
		public static final int STATUS_READY = 1;
		public static final int STATUS_LOCATION_DISABLED = 2;
		public static final int STATUS_NOT_ALLOWED = 3;

		public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {}

		public void onStatusChanged(int status) {}
	}
}
