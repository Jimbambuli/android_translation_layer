package android.telephony;

import java.util.ArrayList;
import java.util.List;

/* AOSP frameworks/base/telephony/java/android/telephony/NetworkRegistrationInfo.java: constants and the getters
 * on a default-built instance. Only ServiceState hands these out and ATL's ServiceState has none. */
public final class NetworkRegistrationInfo {
	public static final int DOMAIN_UNKNOWN = 0;
	public static final int DOMAIN_CS = 1;
	public static final int DOMAIN_PS = 2;
	public static final int DOMAIN_CS_PS = 3;

	public static final int REGISTRATION_STATE_NOT_REGISTERED_OR_SEARCHING = 0;
	public static final int REGISTRATION_STATE_HOME = 1;
	public static final int REGISTRATION_STATE_NOT_REGISTERED_SEARCHING = 2;
	public static final int REGISTRATION_STATE_DENIED = 3;
	public static final int REGISTRATION_STATE_UNKNOWN = 4;
	public static final int REGISTRATION_STATE_ROAMING = 5;
	public static final int REGISTRATION_STATE_EMERGENCY = 6;

	private NetworkRegistrationInfo() {}

	public boolean isRegistered() {
		return false;
	}

	public boolean isNetworkRegistered() {
		return false;
	}

	public String getRegisteredPlmn() {
		return null;
	}

	public List<Integer> getAvailableServices() {
		return new ArrayList<>();
	}
}
