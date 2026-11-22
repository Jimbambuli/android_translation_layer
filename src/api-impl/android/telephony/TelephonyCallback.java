package android.telephony;

/* AOSP android.telephony.TelephonyCallback with the listener interfaces whose arguments ATL has. ATL has no
 * modem, so like on a device without telephony no listener is ever called (TelephonyManager.registerTelephonyCallback()). */
public class TelephonyCallback {

	public TelephonyCallback() {}

	public interface MessageWaitingIndicatorListener {
		void onMessageWaitingIndicatorChanged(boolean mwi);
	}

	public interface CallForwardingIndicatorListener {
		void onCallForwardingIndicatorChanged(boolean cfi);
	}

	public interface CellLocationListener {
		void onCellLocationChanged(CellLocation location);
	}

	public interface ServiceStateListener {
		void onServiceStateChanged(ServiceState serviceState);
	}

	public interface CallStateListener {
		void onCallStateChanged(int state);
	}

	public interface DataConnectionStateListener {
		void onDataConnectionStateChanged(int state, int networkType);
	}

	public interface DataActivityListener {
		void onDataActivity(int direction);
	}

	public interface UserMobileDataStateListener {
		void onUserMobileDataStateChanged(boolean enabled);
	}

	public interface DisplayInfoListener {
		void onDisplayInfoChanged(TelephonyDisplayInfo telephonyDisplayInfo);
	}

	public interface ActiveDataSubscriptionIdListener {
		void onActiveDataSubscriptionIdChanged(int subId);
	}

	public interface RadioPowerStateListener {
		void onRadioPowerStateChanged(int state);
	}

	public interface CarrierNetworkListener {
		void onCarrierNetworkChange(boolean active);
	}

	public interface SignalStrengthsListener {
		void onSignalStrengthsChanged(SignalStrength signalStrength);
	}

	public interface CellInfoListener {
		void onCellInfoChanged(java.util.List<CellInfo> cellInfo);
	}

	public interface CallDisconnectCauseListener {
		void onCallDisconnectCauseChanged(int disconnectCause, int preciseDisconnectCause);
	}

	public interface DataActivationStateListener {
		void onDataActivationStateChanged(int state);
	}

	public interface EmergencyNumberListListener {
		void onEmergencyNumberListChanged(java.util.Map<Integer, java.util.List<android.telephony.emergency.EmergencyNumber>> emergencyNumberList);
	}

	public interface RegistrationFailedListener {
		void onRegistrationFailed(CellIdentity cellIdentity, String chosenPlmn, int domain, int causeCode, int additionalCauseCode);
	}
}
