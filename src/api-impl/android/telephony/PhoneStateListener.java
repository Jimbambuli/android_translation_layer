package android.telephony;
import android.Manifest;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.os.Handler;
import android.os.Looper;
import android.telephony.Annotation.DisconnectCauses;
import android.telephony.Annotation.PreciseDisconnectCauses;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.ImsReasonInfo;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

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

    /**
     * Callback invoked when call disconnect cause changes on the registered subscription.
     * Note, the registration subId comes from {@link TelephonyManager} object which registers
     * PhoneStateListener by {@link TelephonyManager#listen(PhoneStateListener, int)}.
     * If this TelephonyManager object was created with
     * {@link TelephonyManager#createForSubscriptionId(int)}, then the callback applies to the
     * subId. Otherwise, this callback applies to
     * {@link SubscriptionManager#getDefaultSubscriptionId()}.
     *
     * @param disconnectCause the disconnect cause
     * @param preciseDisconnectCause the precise disconnect cause
     * @deprecated Use {@link TelephonyCallback.CallDisconnectCauseListener} instead.
     */
    @RequiresPermission(android.Manifest.permission.READ_PRECISE_PHONE_STATE)
    @Deprecated
    public void onCallDisconnectCauseChanged(@DisconnectCauses int disconnectCause,
            @PreciseDisconnectCauses int preciseDisconnectCause) {
        // default implementation empty
    }

    /**
     * Callback invoked when Ims call disconnect cause changes on the registered subscription.
     * Note, the registration subId comes from {@link TelephonyManager} object which registers
     * PhoneStateListener by {@link TelephonyManager#listen(PhoneStateListener, int)}.
     * If this TelephonyManager object was created with
     * {@link TelephonyManager#createForSubscriptionId(int)}, then the callback applies to the
     * subId. Otherwise, this callback applies to
     * {@link SubscriptionManager#getDefaultSubscriptionId()}.
     *
     * @param imsReasonInfo {@link ImsReasonInfo} contains details on why IMS call failed.
     * @deprecated Use {@link TelephonyCallback.ImsCallDisconnectCauseListener} instead.
     */
    @RequiresPermission(android.Manifest.permission.READ_PRECISE_PHONE_STATE)
    @Deprecated
    public void onImsCallDisconnectCauseChanged(@NonNull ImsReasonInfo imsReasonInfo) {
        // default implementation empty
    }

    /**
     * Callback providing update about the default/internet data connection on the registered
     * subscription.
     *
     * Note, the registration subId comes from {@link TelephonyManager} object which registers
     * PhoneStateListener by {@link TelephonyManager#listen(PhoneStateListener, int)}.
     * If this TelephonyManager object was created with
     * {@link TelephonyManager#createForSubscriptionId(int)}, then the callback applies to the
     * subId. Otherwise, this callback applies to
     * {@link SubscriptionManager#getDefaultSubscriptionId()}.
     *
     * <p>Requires permission {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE}
     * or the calling app has carrier privileges
     * (see {@link TelephonyManager#hasCarrierPrivileges}).
     *
     * @param dataConnectionState {@link PreciseDataConnectionState}
     * @deprecated Use {@link TelephonyCallback.PreciseDataConnectionStateListener} instead.
     */
    @RequiresPermission(android.Manifest.permission.READ_PRECISE_PHONE_STATE)
    @Deprecated
    public void onPreciseDataConnectionStateChanged(
            @NonNull PreciseDataConnectionState dataConnectionState) {
        // default implementation empty
    }

    /**
     * Callback invoked when the display info has changed on the registered subscription.
     * <p> The {@link TelephonyDisplayInfo} contains status information shown to the user based on
     * carrier policy.
     *
     * For clients compiled on Android 11 SDK, requires permission:
     * {@link android.Manifest.permission#READ_PHONE_STATE} or that the calling app has carrier
     * privileges (see {@link TelephonyManager#hasCarrierPrivileges}).
     * For clients compiled on Android 12 SDK or newer,
     * {@link android.Manifest.permission#READ_PHONE_STATE} or carrier privileges is not required
     * anymore.
     *
     * @param telephonyDisplayInfo The display information.
     * @deprecated Use {@link TelephonyCallback.DisplayInfoListener} instead.
     */
    @RequiresPermission(android.Manifest.permission.READ_PHONE_STATE)
    @Deprecated
    public void onDisplayInfoChanged(@NonNull TelephonyDisplayInfo telephonyDisplayInfo) {
        // default implementation empty
    }

    /**
     * Callback invoked when the current emergency number list has changed on the registered
     * subscription.
     *
     * Note, the registered subscription is associated with {@link TelephonyManager} object
     * on which {@link TelephonyManager#listen(PhoneStateListener, int)} was called.
     * If this TelephonyManager object was created with
     * {@link TelephonyManager#createForSubscriptionId(int)}, then the callback applies to the
     * given subId. Otherwise, this callback applies to
     * {@link SubscriptionManager#getDefaultSubscriptionId()}.
     *
     * @param emergencyNumberList Map associating all active subscriptions on the device with the
     *                            list of emergency numbers originating from that subscription.
     *                            If there are no active subscriptions, the map will contain a
     *                            single entry with
     *                            {@link SubscriptionManager#INVALID_SUBSCRIPTION_ID} as
     *                            the key and a list of emergency numbers as the value. If no
     *                            emergency number information is available, the value will be null.
     * @deprecated Use {@link TelephonyCallback.EmergencyNumberListListener} instead.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.READ_PHONE_STATE)
    public void onEmergencyNumberListChanged(
            @NonNull Map<Integer, List<EmergencyNumber>> emergencyNumberList) {
        // default implementation empty
    }

    /**
     * Report that Registration or a Location/Routing/Tracking Area update has failed.
     *
     * <p>Indicate whenever a registration procedure, including a location, routing, or tracking
     * area update fails. This includes procedures that do not necessarily result in a change of
     * the modem's registration status. If the modem's registration status changes, that is
     * reflected in the onNetworkStateChanged() and subsequent get{Voice/Data}RegistrationState().
     *
     * <p>Because registration failures are ephemeral, this callback is not sticky.
     * Registrants will not receive the most recent past value when registering.
     *
     * @param cellIdentity the CellIdentity, which must include the globally unique identifier
     *        for the cell (for example, all components of the CGI or ECGI).
     * @param chosenPlmn a 5 or 6 digit alphanumeric PLMN (MCC|MNC) among those broadcast by the
     *         cell that was chosen for the failed registration attempt.
     * @param domain DOMAIN_CS, DOMAIN_PS or both in case of a combined procedure.
     * @param causeCode the primary failure cause code of the procedure.
     *        For GSM/UMTS (MM), values are in TS 24.008 Sec 10.5.95
     *        For GSM/UMTS (GMM), values are in TS 24.008 Sec 10.5.147
     *        For LTE (EMM), cause codes are TS 24.301 Sec 9.9.3.9
     *        For NR (5GMM), cause codes are TS 24.501 Sec 9.11.3.2
     *        Integer.MAX_VALUE if this value is unused.
     * @param additionalCauseCode the cause code of any secondary/combined procedure if appropriate.
     *        For UMTS, if a combined attach succeeds for PS only, then the GMM cause code shall be
     *        included as an additionalCauseCode. For LTE (ESM), cause codes are in
     *        TS 24.301 9.9.4.4. Integer.MAX_VALUE if this value is unused.
     * @deprecated Use {@link TelephonyCallback.RegistrationFailedListener} instead.
     */
    @Deprecated
    @RequiresPermission(allOf = {
            Manifest.permission.READ_PRECISE_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    public void onRegistrationFailed(@NonNull CellIdentity cellIdentity, @NonNull String chosenPlmn,
            int domain, int causeCode, int additionalCauseCode) {
        // default implementation empty
    }

    /**
     * Report updated barring information for the current camped/registered cell.
     *
     * <p>Barring info is provided for all services applicable to the current camped/registered
     * cell, for the registered PLMN and current access class/access category.
     *
     * @param barringInfo for all services on the current cell.
     * @see android.telephony.BarringInfo
     * @deprecated Use {@link TelephonyCallback.BarringInfoListener} instead.
     */
    @Deprecated
    @RequiresPermission(allOf = {
            Manifest.permission.READ_PRECISE_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    public void onBarringInfoChanged(@NonNull BarringInfo barringInfo) {
        // default implementation empty
    }
}
