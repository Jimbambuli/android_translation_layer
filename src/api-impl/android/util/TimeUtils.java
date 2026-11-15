package android.util;

import android.annotation.NonNull;
import android.annotation.Nullable;
import java.time.LocalTime;
import java.time.zone.ZoneRulesProvider;
import java.util.List;

/**
 * android-16 frameworks/base core/java/android/util/TimeUtils.java, public API. AOSP answers the country based
 * lookups from the CountryTimeZones data of com.android.i18n.timezone (TimeZoneFinder); ATL has no such data
 * (neither the module nor ICU4J), so every country code behaves like one that is not in that data: null.
 */
public class TimeUtils {

	public static java.util.TimeZone getTimeZone(int offset, boolean dst, long when, String country) {
		// AOSP: null country or country without CountryTimeZones -> null
		return null;
	}

	public static @Nullable List<String> getTimeZoneIdsForCountryCode(@NonNull String countryCode) {
		if (countryCode == null) {
			throw new NullPointerException("countryCode == null");
		}
		return null;
	}

	/** AOSP: ZoneInfoDb.getInstance().getVersion(), the tzdata version; here the newest tzdb version of the JDK. */
	public static String getTimeZoneDatabaseVersion() {
		try {
			return ZoneRulesProvider.getVersions("UTC").lastKey();
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static boolean isTimeBetween(@NonNull LocalTime reference, @NonNull LocalTime start, @NonNull LocalTime end) {
		//    ////////E----+-----S////////
		if ((reference.isBefore(start) && reference.isAfter(end)
		     //    -----+----S//////////E------
		     || (reference.isBefore(end) && reference.isBefore(start) && start.isBefore(end))
		     //    ---------S//////////E---+---
		     || (reference.isAfter(end) && reference.isAfter(start)) && start.isBefore(end))) {
			return false;
		} else {
			return true;
		}
	}
}
