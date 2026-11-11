package android.telephony;

import java.util.ArrayList;
import java.util.List;

public class TelephonyManager {
	/* AOSP TelephonyManager.NETWORK_TYPE_* and getNetworkTypeName() */
	public static final int NETWORK_TYPE_UNKNOWN = 0;
	public static final int NETWORK_TYPE_GPRS = 1;
	public static final int NETWORK_TYPE_EDGE = 2;
	public static final int NETWORK_TYPE_UMTS = 3;
	public static final int NETWORK_TYPE_CDMA = 4;
	public static final int NETWORK_TYPE_EVDO_0 = 5;
	public static final int NETWORK_TYPE_EVDO_A = 6;
	public static final int NETWORK_TYPE_1xRTT = 7;
	public static final int NETWORK_TYPE_HSDPA = 8;
	public static final int NETWORK_TYPE_HSUPA = 9;
	public static final int NETWORK_TYPE_HSPA = 10;
	public static final int NETWORK_TYPE_IDEN = 11;
	public static final int NETWORK_TYPE_EVDO_B = 12;
	public static final int NETWORK_TYPE_LTE = 13;
	public static final int NETWORK_TYPE_EHRPD = 14;
	public static final int NETWORK_TYPE_HSPAP = 15;
	public static final int NETWORK_TYPE_GSM = 16;
	public static final int NETWORK_TYPE_TD_SCDMA = 17;
	public static final int NETWORK_TYPE_IWLAN = 18;
	public static final int NETWORK_TYPE_LTE_CA = 19;
	public static final int NETWORK_TYPE_NR = 20;

	public static String getNetworkTypeName(int type) {
		switch (type) {
			case NETWORK_TYPE_GPRS:
				return "GPRS";
			case NETWORK_TYPE_EDGE:
				return "EDGE";
			case NETWORK_TYPE_UMTS:
				return "UMTS";
			case NETWORK_TYPE_HSDPA:
				return "HSDPA";
			case NETWORK_TYPE_HSUPA:
				return "HSUPA";
			case NETWORK_TYPE_HSPA:
				return "HSPA";
			case NETWORK_TYPE_CDMA:
				return "CDMA";
			case NETWORK_TYPE_EVDO_0:
				return "CDMA - EvDo rev. 0";
			case NETWORK_TYPE_EVDO_A:
				return "CDMA - EvDo rev. A";
			case NETWORK_TYPE_EVDO_B:
				return "CDMA - EvDo rev. B";
			case NETWORK_TYPE_1xRTT:
				return "CDMA - 1xRTT";
			case NETWORK_TYPE_LTE:
				return "LTE";
			case NETWORK_TYPE_EHRPD:
				return "CDMA - eHRPD";
			case NETWORK_TYPE_IDEN:
				return "iDEN";
			case NETWORK_TYPE_HSPAP:
				return "HSPA+";
			case NETWORK_TYPE_GSM:
				return "GSM";
			case NETWORK_TYPE_TD_SCDMA:
				return "TD_SCDMA";
			case NETWORK_TYPE_IWLAN:
				return "IWLAN";
			case NETWORK_TYPE_LTE_CA:
				return "LTE_CA";
			case NETWORK_TYPE_NR:
				return "NR";
			case NETWORK_TYPE_UNKNOWN:
				return "UNKNOWN";
			default:
				return "UNKNOWN(" + type + ")";
		}
	}

	public static TelephonyManager getDefault() {
		return new TelephonyManager();
	}

	// FIXME: can we return null instead of ""?
	public String getNetworkOperator() {
		return "";
	}

	public String getNetworkOperatorName() {
		return "";
	}

	public String getSimOperator() {
		return "";
	}

	public String getSubscriberId() {
		return "";
	}

	public int getPhoneType() {
		return 0; // PHONE_TYPE_NONE
	}

	public String getNetworkCountryIso() { return ""; }
	public String getSimCountryIso() { return ""; }

	public List getNeighboringCellInfo() {
		return new ArrayList(0);
	}

	public CellLocation getCellLocation() {
		return new CellLocation();
	}

	public boolean isNetworkRoaming() {
		return false;
	}

	public void listen(PhoneStateListener listener, int events) {}

	/* AOSP TelephonyManager.registerTelephonyCallback(): ATL has no modem, so as on a device without telephony
	 * the callback never hears of a change */
	public void registerTelephonyCallback(java.util.concurrent.Executor executor, TelephonyCallback callback) {
		if (executor == null || callback == null) {
			throw new IllegalArgumentException("TelephonyCallback and executor must be non-null");
		}
	}

	public void unregisterTelephonyCallback(TelephonyCallback callback) {}

	public int getNetworkType() {
		return 0; // NETWORK_TYPE_UNKNOWN
	}

	/* AOSP TelephonyManager.getDataNetworkType(): no mobile data network */
	public int getDataNetworkType() {
		return 0; // NETWORK_TYPE_UNKNOWN
	}

	public String getLine1Number() {
		return null;
	}

	public int getSimState() {
		return 0; // SIM_STATE_UNKNOWN
	}

	public String getSimOperatorName() {
		return null;
	}

	public int getCallState() {
		return 0; // CALL_STATE_IDLE
	}

	public String getDeviceId() {
		return "";
	}
}
