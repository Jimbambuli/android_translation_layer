package android.telephony;

/* The callbacks of AOSP PhoneStateListener whose argument types ATL has; every default body is empty there
 * ("default implementation empty"). TelephonyManager.listen() calls them with ModemManager's modem (ModemManagerPhone). */
public class PhoneStateListener {
	public static final int LISTEN_ACTIVE_DATA_SUBSCRIPTION_ID_CHANGE = 4194304;
	public static final int LISTEN_BARRING_INFO = -2147483648;
	public static final int LISTEN_CALL_DISCONNECT_CAUSES = 33554432;
	public static final int LISTEN_CALL_FORWARDING_INDICATOR = 8;
	public static final int LISTEN_CALL_STATE = 32;
	public static final int LISTEN_CELL_INFO = 1024;
	public static final int LISTEN_CELL_LOCATION = 16;
	public static final int LISTEN_DATA_ACTIVITY = 128;
	public static final int LISTEN_DATA_CONNECTION_STATE = 64;
	public static final int LISTEN_DISPLAY_INFO_CHANGED = 1048576;
	public static final int LISTEN_EMERGENCY_NUMBER_LIST = 16777216;
	public static final int LISTEN_IMS_CALL_DISCONNECT_CAUSES = 134217728;
	public static final int LISTEN_MESSAGE_WAITING_INDICATOR = 4;
	public static final int LISTEN_NONE = 0;
	public static final int LISTEN_PRECISE_DATA_CONNECTION_STATE = 4096;
	public static final int LISTEN_REGISTRATION_FAILURE = 1073741824;
	public static final int LISTEN_SERVICE_STATE = 1;
	public static final int LISTEN_SIGNAL_STRENGTH = 2;
	public static final int LISTEN_SIGNAL_STRENGTHS = 256;
	public static final int LISTEN_USER_MOBILE_DATA_STATE = 524288;

	/** @hide the executor the callbacks run on (AOSP mExecutor) */
	java.util.concurrent.Executor mExecutor;

	/* AOSP: the callbacks run on the looper of the creating thread */
	public PhoneStateListener() {
		this(android.os.Looper.myLooper());
	}

	/** @hide AOSP PhoneStateListener(Looper) */
	public PhoneStateListener(android.os.Looper looper) {
		if (looper != null) {
			final android.os.Handler handler = new android.os.Handler(looper);
			mExecutor = new java.util.concurrent.Executor() {
				public void execute(Runnable r) {
					handler.post(r);
				}
			};
		}
	}

	/* AOSP: the callbacks run on the executor; the executor must not be null (ATL never calls the callbacks) */
	@Deprecated
	public PhoneStateListener(java.util.concurrent.Executor executor) {
		if (executor == null) {
			throw new IllegalArgumentException("PhoneStateListener Executor must be non-null");
		}
		mExecutor = executor;
	}

	@Deprecated
	public void onServiceStateChanged(ServiceState serviceState) {}

	public void onMessageWaitingIndicatorChanged(boolean mwi) {}

	public void onCallForwardingIndicatorChanged(boolean cfi) {}

	public void onCellLocationChanged(CellLocation location) {}

	@Deprecated
	public void onCallStateChanged(int state, String phoneNumber) {}

	public void onDataConnectionStateChanged(int state) {}

	public void onDataConnectionStateChanged(int state, int networkType) {}

	public void onDataActivity(int direction) {}

	@Deprecated
	public void onSignalStrengthChanged(int asu) {}

	public void onUserMobileDataStateChanged(boolean enabled) {}

	public void onActiveDataSubscriptionIdChanged(int subId) {}

	/* AOSP PhoneStateListener: empty default bodies */
	public void onSignalStrengthsChanged(SignalStrength signalStrength) {}

	public void onCellInfoChanged(java.util.List<CellInfo> cellInfo) {}
}
