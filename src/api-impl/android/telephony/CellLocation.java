package android.telephony;

import android.os.Bundle;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

/* AOSP frameworks/base telephony CellLocation.java. requestLocationUpdate() asks ITelephony only if the service
 * exists (phone != null); ATL has none, so it does nothing. */
@Deprecated
public abstract class CellLocation {

	@Deprecated
	public static void requestLocationUpdate() {}

	public static CellLocation newFromBundle(Bundle bundle) {
		switch (TelephonyManager.getDefault().getCurrentPhoneType()) {
		case TelephonyManager.PHONE_TYPE_CDMA:
			return new CdmaCellLocation(bundle);
		case TelephonyManager.PHONE_TYPE_GSM:
			return new GsmCellLocation(bundle);
		default:
			return null;
		}
	}

	public abstract void fillInNotifierBundle(Bundle bundle);

	public abstract boolean isEmpty();

	public abstract void setStateInvalid();

	public static CellLocation getEmpty() {
		switch (TelephonyManager.getDefault().getCurrentPhoneType()) {
		case TelephonyManager.PHONE_TYPE_CDMA:
			return new CdmaCellLocation();
		case TelephonyManager.PHONE_TYPE_GSM:
			return new GsmCellLocation();
		default:
			return null;
		}
	}
}
