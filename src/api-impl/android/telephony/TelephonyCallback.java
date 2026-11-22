package android.telephony;

/* AOSP android.telephony.TelephonyCallback with the listener interfaces whose arguments ATL has.
 * TelephonyManager.registerTelephonyCallback() calls ServiceStateListener, CallStateListener and
 * DataConnectionStateListener with ModemManager's modem (ModemManagerPhone); without a modem none is ever called. */
public class TelephonyCallback {

	public TelephonyCallback() {}

	public interface ServiceStateListener {
		void onServiceStateChanged(ServiceState serviceState);
	}

	public interface MessageWaitingIndicatorListener {
		void onMessageWaitingIndicatorChanged(boolean mwi);
	}

	public interface CallForwardingIndicatorListener {
		void onCallForwardingIndicatorChanged(boolean cfi);
	}

	public interface CellLocationListener {
		void onCellLocationChanged(CellLocation location);
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
}
