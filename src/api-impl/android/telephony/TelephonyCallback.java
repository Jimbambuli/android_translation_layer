package android.telephony;
import android.Manifest;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.satellite.NtnSignalStrength;
import java.util.List;
import java.util.Map;

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

    /**
     * Interface for call disconnect cause listener.
     */
    public interface CallDisconnectCauseListener {
        /**
         * Callback invoked when call disconnect cause changes on the registered subscription.
         * Note, the registration subscription ID comes from {@link TelephonyManager} object
         * which registers TelephonyCallback by
         * {@link TelephonyManager#registerTelephonyCallback(Executor, TelephonyCallback)}.
         * If this TelephonyManager object was created with
         * {@link TelephonyManager#createForSubscriptionId(int)}, then the callback applies to the
         * subscription ID. Otherwise, this callback applies to
         * {@link SubscriptionManager#getDefaultSubscriptionId()}.
         *
         * @param disconnectCause        the disconnect cause
         * @param preciseDisconnectCause the precise disconnect cause
         */
        @RequiresPermission(android.Manifest.permission.READ_PRECISE_PHONE_STATE)
        void onCallDisconnectCauseChanged(@Annotation.DisconnectCauses int disconnectCause,
                @Annotation.PreciseDisconnectCauses int preciseDisconnectCause);
    }

    /**
     * Interface for IMS call disconnect cause listener.
     */
    public interface ImsCallDisconnectCauseListener {
        /**
         * Callback invoked when IMS call disconnect cause changes on the registered subscription.
         * Note, the registration subscription ID comes from {@link TelephonyManager} object
         * which registers TelephonyCallback by
         * {@link TelephonyManager#registerTelephonyCallback(Executor, TelephonyCallback)}.
         * If this TelephonyManager object was created with
         * {@link TelephonyManager#createForSubscriptionId(int)}, then the callback applies to the
         * subscription ID. Otherwise, this callback applies to
         * {@link SubscriptionManager#getDefaultSubscriptionId()}.
         *
         * The calling app should have carrier privileges
         * (see {@link TelephonyManager#hasCarrierPrivileges}) if it does not have the
         * {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE}.
         *
         * @param imsReasonInfo {@link ImsReasonInfo} contains details on why IMS call failed.
         */
        @RequiresPermission(android.Manifest.permission.READ_PRECISE_PHONE_STATE)
        void onImsCallDisconnectCauseChanged(@NonNull ImsReasonInfo imsReasonInfo);
    }

    /**
     * Interface for precise data connection state listener.
     */
    public interface PreciseDataConnectionStateListener {
        /**
         * Callback providing update about the default/internet data connection on the registered
         * subscription.
         * <p>
         * Note, the registration subscription ID comes from {@link TelephonyManager} object
         * which registers TelephonyCallback by
         * {@link TelephonyManager#registerTelephonyCallback(Executor, TelephonyCallback)}.
         * If this TelephonyManager object was created with
         * {@link TelephonyManager#createForSubscriptionId(int)}, then the callback applies to the
         * subscription ID. Otherwise, this callback applies to
         * {@link SubscriptionManager#getDefaultSubscriptionId()}.
         *
         * The calling app should have carrier privileges
         * (see {@link TelephonyManager#hasCarrierPrivileges}) if it does not have the
         * {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE}.
         *
         * @param dataConnectionState {@link PreciseDataConnectionState}
         */
        @RequiresPermission(android.Manifest.permission.READ_PRECISE_PHONE_STATE)
        void onPreciseDataConnectionStateChanged(
            @NonNull PreciseDataConnectionState dataConnectionState);
    }

    /**
     * Interface for SIM data activation state listener.
     */
    public interface DataActivationStateListener {
        /**
         * Callback invoked when the SIM data activation state has changed on the registered
         * subscription.
         * Note, the registration subscription ID comes from {@link TelephonyManager} object
         * which registers TelephonyCallback by
         * {@link TelephonyManager#registerTelephonyCallback(Executor, TelephonyCallback)}.
         * If this TelephonyManager object was created with
         * {@link TelephonyManager#createForSubscriptionId(int)}, then the callback applies to the
         * subscription ID. Otherwise, this callback applies to
         * {@link SubscriptionManager#getDefaultSubscriptionId()}.
         *
         * @param state is the current SIM data activation state
         */
        void onDataActivationStateChanged(@Annotation.SimActivationState int state);
    }

    /**
     * Interface for the current emergency number list listener.
     */
    public interface EmergencyNumberListListener {
        /**
         * Callback invoked when the current emergency number list has changed on the registered
         * subscription.
         * <p>
         * Note, the registered subscription is associated with {@link TelephonyManager} object
         * on which
         * {@link TelephonyManager#registerTelephonyCallback(Executor, TelephonyCallback)}
         * was called.
         * If this TelephonyManager object was created with
         * {@link TelephonyManager#createForSubscriptionId(int)}, then the callback applies to the
         * given subscription ID. Otherwise, this callback applies to
         * {@link SubscriptionManager#getDefaultSubscriptionId()}.
         *
         * The calling app should have carrier privileges
         * (see {@link TelephonyManager#hasCarrierPrivileges}) if it does not have the
         * {@link android.Manifest.permission#READ_PHONE_STATE}.
         *
         * @param emergencyNumberList Map associating all active subscriptions on the device with
         *                            the list of emergency numbers originating from that
         *                            subscription.
         *                            If there are no active subscriptions, the map will contain a
         *                            single entry with
         *                            {@link SubscriptionManager#INVALID_SUBSCRIPTION_ID} as
         *                            the key and a list of emergency numbers as the value. If no
         *                            emergency number information is available, the value will be
         *                            empty.
         */
        @RequiresPermission(android.Manifest.permission.READ_PHONE_STATE)
        void onEmergencyNumberListChanged(@NonNull Map<Integer,
                List<EmergencyNumber>> emergencyNumberList);
    }

    /**
     * Interface for registration failures listener.
     */
    public interface RegistrationFailedListener {
        /**
         * Report that Registration or a Location/Routing/Tracking Area update has failed.
         *
         * <p>Indicate whenever a registration procedure, including a location, routing, or tracking
         * area update fails. This includes procedures that do not necessarily result in a change of
         * the modem's registration status. If the modem's registration status changes, that is
         * reflected in the onNetworkStateChanged() and subsequent
         * get{Voice/Data}RegistrationState().
         *
         * <p>Because registration failures are ephemeral, this callback is not sticky.
         * Registrants will not receive the most recent past value when registering.
         *
         * The calling app should have carrier privileges
         * (see {@link TelephonyManager#hasCarrierPrivileges}) if it does not have the
         * {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE} and
         * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
         *
         * If the calling app doesn't have {@link android.Manifest.permission#ACCESS_FINE_LOCATION},
         * it will receive {@link CellIdentity} without location-sensitive information included.
         *
         * @param cellIdentity        the CellIdentity, which must include the globally unique
         *                            identifier
         *                            for the cell (for example, all components of the CGI or ECGI).
         * @param chosenPlmn          a 5 or 6 digit alphanumeric PLMN (MCC|MNC) among those
         *                            broadcast by the
         *                            cell that was chosen for the failed registration attempt.
         * @param domain              DOMAIN_CS, DOMAIN_PS or both in case of a combined procedure.
         * @param causeCode           the primary failure cause code of the procedure.
         *                            For GSM/UMTS (MM), values are in TS 24.008 Sec 10.5.95
         *                            For GSM/UMTS (GMM), values are in TS 24.008 Sec 10.5.147
         *                            For LTE (EMM), cause codes are TS 24.301 Sec 9.9.3.9
         *                            For NR (5GMM), cause codes are TS 24.501 Sec 9.11.3.2
         *                            Integer.MAX_VALUE if this value is unused.
         * @param additionalCauseCode the cause code of any secondary/combined procedure
         *                            if appropriate. For UMTS, if a combined attach succeeds for
         *                            PS only, then the GMM cause code shall be included as an
         *                            additionalCauseCode. For LTE (ESM), cause codes are in
         *                            TS 24.301 9.9.4.4. Integer.MAX_VALUE if this value is unused.
         */
        @RequiresPermission(allOf = {
                Manifest.permission.READ_PRECISE_PHONE_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION
        })
        void onRegistrationFailed(@NonNull CellIdentity cellIdentity, @NonNull String chosenPlmn,
                @NetworkRegistrationInfo.Domain int domain, int causeCode, int additionalCauseCode);
    }

    /**
     * Interface for barring information listener.
     */
    public interface BarringInfoListener {
        /**
         * Report updated barring information for the current camped/registered cell.
         *
         * <p>Barring info is provided for all services applicable to the current camped/registered
         * cell, for the registered PLMN and current access class/access category.
         *
         * The calling app should have carrier privileges
         * (see {@link TelephonyManager#hasCarrierPrivileges}) if it does not have the
         * {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE} and
         * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
         *
         * If the calling app doesn't have {@link android.Manifest.permission#ACCESS_FINE_LOCATION},
         * it will receive {@link BarringInfo} including {@link CellIdentity} without
         * location-sensitive information included.
         *
         * @param barringInfo for all services on the current cell.
         * @see android.telephony.BarringInfo
         */
        @RequiresPermission(allOf = {
                Manifest.permission.READ_PRECISE_PHONE_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION
        })
        void onBarringInfoChanged(@NonNull BarringInfo barringInfo);
    }

    /**
     * Interface for current physical channel configuration listener.
     */
    public interface PhysicalChannelConfigListener {
        /**
         * Callback invoked when the current physical channel configuration has changed
         *
         * The calling app should have carrier privileges
         * (see {@link TelephonyManager#hasCarrierPrivileges}) if it does not have the
         * {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE}.
         *
         * @param configs List of the current {@link PhysicalChannelConfig}s
         */
        @RequiresPermission(Manifest.permission.READ_PRECISE_PHONE_STATE)
        void onPhysicalChannelConfigChanged(@NonNull List<PhysicalChannelConfig> configs);
    }

    /**
     * Interface for carrier roaming non-terrestrial network listener.
     */

    public interface CarrierRoamingNtnListener {
        /**
         * Callback invoked when carrier roaming non-terrestrial network mode changes.
         *
         * @param active {@code true} If the device is connected to carrier roaming
         *                           non-terrestrial network or was connected within the
         *                           {CarrierConfigManager
         *                           #KEY_SATELLITE_CONNECTION_HYSTERESIS_SEC_INT} duration,
         *                           {code false} otherwise.
         */
        void onCarrierRoamingNtnModeChanged(boolean active);

        /**
         * Callback invoked when carrier roaming non-terrestrial network eligibility changes.
         *
         * @param eligible {@code true} when the device is eligible for satellite
         * communication if all the following conditions are met:
         * <ul>
         * <li>Any subscription on the device supports P2P satellite messaging which is defined by
         * {@link CarrierConfigManager#KEY_SATELLITE_ATTACH_SUPPORTED_BOOL} </li>
         * <li>{@link CarrierConfigManager#KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT} set to
         * {@link CarrierConfigManager#CARRIER_ROAMING_NTN_CONNECT_MANUAL} </li>
         * <li>The device is in {@link ServiceState#STATE_OUT_OF_SERVICE}, not connected to Wi-Fi,
         * and the hysteresis timer defined by {@link CarrierConfigManager
         * #KEY_CARRIER_SUPPORTED_SATELLITE_NOTIFICATION_HYSTERESIS_SEC_INT} is expired. </li>
         * </ul>
         */
        default void onCarrierRoamingNtnEligibleStateChanged(boolean eligible) {}

        /**
         * Callback invoked when carrier roaming non-terrestrial network available
         * service changes.
         *
         * @param availableServices array of supported services.
         */
        default void onCarrierRoamingNtnAvailableServicesChanged(
                @NonNull @NetworkRegistrationInfo.ServiceType int[] availableServices) {}

        /**
         * Callback invoked when carrier roaming non-terrestrial network signal strength changes.
         *
         * @param ntnSignalStrength non-terrestrial network signal strength.
         */
        default void onCarrierRoamingNtnSignalStrengthChanged(
                @NonNull NtnSignalStrength ntnSignalStrength) {}
    }
}
