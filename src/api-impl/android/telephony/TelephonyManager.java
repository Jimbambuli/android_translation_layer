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

	/* PhoneConstants.State.X.toString() */
	public static final String EXTRA_STATE_IDLE = "IDLE";
	public static final String EXTRA_STATE_RINGING = "RINGING";
	public static final String EXTRA_STATE_OFFHOOK = "OFFHOOK";

	/* AOSP TelephonyManager with getITelephony() == null (a device without telephony service) */
	public TelephonyManager createForSubscriptionId(int subId) {
		return new TelephonyManager();
	}

	public int getCallStateForSubscription() {
		return 0; // getCallState(subId): CALL_STATE_IDLE
	}

	public int getDataState() {
		return 0; // DATA_DISCONNECTED
	}

	/* AOSP with getITelephony() == null */
	public SignalStrength getSignalStrength() {
		return null;
	}

	public List<CellInfo> getAllCellInfo() {
		return null;
	}

	public abstract static class CellInfoCallback {
		public static final int ERROR_TIMEOUT = 1;
		public static final int ERROR_MODEM_ERROR = 2;

		public abstract void onCellInfo(List<CellInfo> cellInfo);

		public void onError(int errorCode, Throwable detail) {}
	}

	/* AOSP: with telephony null, ISE for apps targeting after R (NULL_TELEPHONY_THROW_NO_CB), silent return
	 * before. ATL does not keep the caller's target SDK here, so the modern behaviour is used. */
	public void requestCellInfoUpdate(java.util.concurrent.Executor executor, CellInfoCallback callback) {
		throw new IllegalStateException("Telephony is null");
	}

	public String getMmsUserAgent() {
		return null;
	}

	public String getMmsUAProfUrl() {
		return null;
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

	/* AOSP getSimStateIncludingLoaded(): without a SIM slot (getSlotIndex() < 0, getPhoneCount() 0 on a device
	 * that is neither voice, SMS nor data capable) all SIMs count as absent */
	public int getSimState() {
		return 1; // SIM_STATE_ABSENT
	}

	/* AOSP getSimOperatorNameForPhone(): the property icc_operator_alpha, "" when not set (no SIM) */
	public String getSimOperatorName() {
		return "";
	}

	public int getCallState() {
		return 0; // CALL_STATE_IDLE
	}

	/* AOSP TelephonyManager.getDeviceId(): null when there is no ITelephony service */
	public String getDeviceId() {
		return null;
	}
}
