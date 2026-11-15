package android.telephony;

import java.util.ArrayList;
import java.util.List;

/* AOSP frameworks/base/telephony/java/android/telephony/ServiceState.java: only the state after init() (no modem).
 * TelephonyManager.getServiceState() returns null without ITelephony, so apps normally never get an instance. */
public class ServiceState {
	public static final int STATE_IN_SERVICE = 0;
	public static final int STATE_OUT_OF_SERVICE = 1;
	public static final int STATE_EMERGENCY_ONLY = 2;
	public static final int STATE_POWER_OFF = 3;

	/* init(): mVoiceRegState = STATE_OUT_OF_SERVICE */
	public int getState() {
		return STATE_OUT_OF_SERVICE;
	}

	public int getVoiceRegState() {
		return STATE_OUT_OF_SERVICE;
	}

	public int getDataRegState() {
		return STATE_OUT_OF_SERVICE;
	}

	public boolean isEmergencyOnly() {
		return false;
	}

	public boolean getRoaming() {
		return false;
	}

	public String getOperatorAlphaLong() {
		return null;
	}

	public String getOperatorNumeric() {
		return null;
	}

	/* AOSP builds three UNKNOWN-state entries here; NetworkRegistrationInfo is not ported, so the list is empty */
	public List<NetworkRegistrationInfo> getNetworkRegistrationInfoList() {
		return new ArrayList<>();
	}

	public boolean isUsingNonTerrestrialNetwork() {
		return false;
	}
}
