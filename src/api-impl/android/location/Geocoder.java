package android.location;

import android.content.Context;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/* AOSP Geocoder (location/java/android/location/Geocoder.java) on a device with LocationManagerService but no
 * geocode provider (mGeocodeProvider == null, as isPresent() reports): reverseGeocode()/forwardGeocode() answer
 * callback.onError(null) from the system service. The deprecated blocking forms then get SynchronousGeocoder's
 * defaults: no error message, so an empty list; the listener forms get onError(null) on a binder thread. */
public class Geocoder {

	public interface GeocodeListener {
		void onGeocode(List<Address> addresses);

		default void onError(String errorMessage) {}
	}

	private final Context mContext;
	private final Locale mLocale;

	public static boolean isPresent() {
		return false;
	}

	public Geocoder(Context context) {
		this(context, Locale.getDefault());
	}

	public Geocoder(Context context, Locale locale) {
		mContext = Objects.requireNonNull(context);
		mLocale = Objects.requireNonNull(locale);
	}

	@Deprecated
	public List<Address> getFromLocation(double latitude, double longitude, int maxResults) throws IOException {
		return Collections.emptyList();
	}

	public void getFromLocation(double latitude, double longitude, int maxResults, GeocodeListener listener) {
		noProvider(listener);
	}

	@Deprecated
	public List<Address> getFromLocationName(String locationName, int maxResults) throws IOException {
		return Collections.emptyList();
	}

	public void getFromLocationName(String locationName, int maxResults, GeocodeListener listener) {
		noProvider(listener);
	}

	@Deprecated
	public List<Address> getFromLocationName(String locationName, int maxResults, double lowerLeftLatitude,
	                                         double lowerLeftLongitude, double upperRightLatitude,
	                                         double upperRightLongitude) throws IOException {
		return Collections.emptyList();
	}

	public void getFromLocationName(String locationName, int maxResults, double lowerLeftLatitude,
	                                double lowerLeftLongitude, double upperRightLatitude,
	                                double upperRightLongitude, GeocodeListener listener) {
		noProvider(listener);
	}

	private static void noProvider(GeocodeListener listener) {
		Objects.requireNonNull(listener);
		new Thread(() -> listener.onError(null)).start();
	}
}
