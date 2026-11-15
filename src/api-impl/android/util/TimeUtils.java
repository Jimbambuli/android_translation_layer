package android.util;

import android.annotation.NonNull;
import java.time.LocalTime;
import java.time.zone.ZoneRulesProvider;

/**
 * android-16 frameworks/base core/java/android/util/TimeUtils.java, public API. AOSP answers the country based
 * lookups (getTimeZone, getTimeZoneIdsForCountryCode) from the CountryTimeZones data of com.android.i18n.timezone;
 * ATL has no such data, so these two are not implemented rather than faked.
 */
public class TimeUtils {

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
