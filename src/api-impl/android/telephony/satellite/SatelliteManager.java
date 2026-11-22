/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.telephony.satellite;

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresFeature;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyRegistryManager;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Manages satellite states such as monitoring enabled state and operations such as provisioning,
 * pointing, messaging, location sharing, etc.
 *
 * <p>To get the object, call {@link Context#getSystemService(String)} with
 * {@link Context#SATELLITE_SERVICE}.
 *
 * <p>SatelliteManager is intended for use on devices with feature
 * {@link PackageManager#FEATURE_TELEPHONY_SATELLITE}. On devices without the feature, the behavior
 * is not reliable.
 */
@SystemService(Context.SATELLITE_SERVICE)
@RequiresFeature(PackageManager.FEATURE_TELEPHONY_SATELLITE)
public final class SatelliteManager {

    private final int mSubId;

    /**
     * Context this SatelliteManager is for.
     */
    @Nullable private final Context mContext;

    private TelephonyRegistryManager mTelephonyRegistryMgr;

    /**
     * Create an instance of the SatelliteManager.
     *
     * @param context The context the SatelliteManager belongs to.
     * @hide
     */
    public SatelliteManager(@Nullable Context context) {
        this(context, SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
    }

    /**
     * Create an instance of the SatelliteManager associated with a particular subscription.
     *
     * @param context The context the SatelliteManager belongs to.
     * @param subId The subscription ID associated with the SatelliteManager.
     */
    private SatelliteManager(@Nullable Context context, int subId) {
        mContext = context;
        mSubId = subId;
    }

    /**
     * The request was successfully processed.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_SUCCESS = 0;

    /**
     * A generic error which should be used only when other specific errors cannot be used.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_ERROR = 1;

    /**
     * Error received from the satellite server.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_SERVER_ERROR = 2;

    /**
     * Error received from the vendor service. This generic error code should be used
     * only when the error cannot be mapped to other specific service error codes.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_SERVICE_ERROR = 3;

    /**
     * Error received from satellite modem. This generic error code should be used only when
     * the error cannot be mapped to other specific modem error codes.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_MODEM_ERROR = 4;

    /**
     * Error received from the satellite network. This generic error code should be used only when
     * the error cannot be mapped to other specific network error codes.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_NETWORK_ERROR = 5;

    /**
     * Telephony is not in a valid state to receive requests from clients.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_INVALID_TELEPHONY_STATE = 6;

    /**
     * Satellite modem is not in a valid state to receive requests from clients.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_INVALID_MODEM_STATE = 7;

    /**
     * Either vendor service, or modem, or Telephony framework has received a request with
     * invalid arguments from its clients.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_INVALID_ARGUMENTS = 8;

    /**
     * Telephony framework failed to send a request or receive a response from the vendor service
     * or satellite modem due to internal error.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_REQUEST_FAILED = 9;

    /**
     * Radio did not start or is resetting.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_RADIO_NOT_AVAILABLE = 10;

    /**
     * The request is not supported by either the satellite modem or the network.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_REQUEST_NOT_SUPPORTED = 11;

    /**
     * Satellite modem or network has no resources available to handle requests from clients.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_NO_RESOURCES = 12;

    /**
     * Satellite service is not provisioned yet.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_SERVICE_NOT_PROVISIONED = 13;

    /**
     * Satellite service provision is already in progress.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_SERVICE_PROVISION_IN_PROGRESS = 14;

    /**
     * The ongoing request was aborted by either the satellite modem or the network.
     * This error is also returned when framework decides to abort current send request as one
     * of the previous send request failed.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_REQUEST_ABORTED = 15;

    /**
     * The device/subscriber is barred from accessing the satellite service.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_ACCESS_BARRED = 16;

    /**
     * Satellite modem timeout to receive ACK or response from the satellite network after
     * sending a request to the network.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_NETWORK_TIMEOUT = 17;

    /**
     * Satellite network is not reachable from the modem.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_NOT_REACHABLE = 18;

    /**
     * The device/subscriber is not authorized to register with the satellite service provider.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_NOT_AUTHORIZED = 19;

    /**
     * The device does not support satellite.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_NOT_SUPPORTED = 20;

    /**
     * The current request is already in-progress.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_REQUEST_IN_PROGRESS = 21;

    /**
     * Satellite modem is currently busy due to which current request cannot be processed.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_MODEM_BUSY = 22;

    /**
     * Telephony process is not currently available or satellite is not supported.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_ILLEGAL_STATE = 23;

    /**
     * Telephony framework timeout to receive ACK or response from the satellite modem after
     * sending a request to the modem.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_MODEM_TIMEOUT = 24;

    /**
     * Telephony framework needs to access the current location of the device to perform the
     * request. However, location in the settings is disabled by users.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_LOCATION_DISABLED = 25;

    /**
     * Telephony framework needs to access the current location of the device to perform the
     * request. However, Telephony fails to fetch the current location from location service.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_LOCATION_NOT_AVAILABLE = 26;

    /**
     * Emergency call is in progress.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_EMERGENCY_CALL_IN_PROGRESS = 27;

    /**
     * Disabling satellite is in progress.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_DISABLE_IN_PROGRESS = 28;

    /**
     * Enabling satellite is in progress.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_ENABLE_IN_PROGRESS = 29;

    /**
     * There is no valid satellite subscription selected.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_RESULT_NO_VALID_SATELLITE_SUBSCRIPTION = 30;

    /** @hide */
    @IntDef(prefix = {"SATELLITE_RESULT_"}, value = {
            SATELLITE_RESULT_SUCCESS,
            SATELLITE_RESULT_ERROR,
            SATELLITE_RESULT_SERVER_ERROR,
            SATELLITE_RESULT_SERVICE_ERROR,
            SATELLITE_RESULT_MODEM_ERROR,
            SATELLITE_RESULT_NETWORK_ERROR,
            SATELLITE_RESULT_INVALID_TELEPHONY_STATE,
            SATELLITE_RESULT_INVALID_MODEM_STATE,
            SATELLITE_RESULT_INVALID_ARGUMENTS,
            SATELLITE_RESULT_REQUEST_FAILED,
            SATELLITE_RESULT_RADIO_NOT_AVAILABLE,
            SATELLITE_RESULT_REQUEST_NOT_SUPPORTED,
            SATELLITE_RESULT_NO_RESOURCES,
            SATELLITE_RESULT_SERVICE_NOT_PROVISIONED,
            SATELLITE_RESULT_SERVICE_PROVISION_IN_PROGRESS,
            SATELLITE_RESULT_REQUEST_ABORTED,
            SATELLITE_RESULT_ACCESS_BARRED,
            SATELLITE_RESULT_NETWORK_TIMEOUT,
            SATELLITE_RESULT_NOT_REACHABLE,
            SATELLITE_RESULT_NOT_AUTHORIZED,
            SATELLITE_RESULT_NOT_SUPPORTED,
            SATELLITE_RESULT_REQUEST_IN_PROGRESS,
            SATELLITE_RESULT_MODEM_BUSY,
            SATELLITE_RESULT_ILLEGAL_STATE,
            SATELLITE_RESULT_MODEM_TIMEOUT,
            SATELLITE_RESULT_LOCATION_DISABLED,
            SATELLITE_RESULT_LOCATION_NOT_AVAILABLE,
            SATELLITE_RESULT_EMERGENCY_CALL_IN_PROGRESS,
            SATELLITE_RESULT_DISABLE_IN_PROGRESS,
            SATELLITE_RESULT_ENABLE_IN_PROGRESS,
            SATELLITE_RESULT_NO_VALID_SATELLITE_SUBSCRIPTION
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SatelliteResult {}

    /**
     * Unknown Non-Terrestrial radio technology. This generic radio technology should be used
     * only when the radio technology cannot be mapped to other specific radio technologies.
     * @hide
     */
    @SystemApi
    public static final int NT_RADIO_TECHNOLOGY_UNKNOWN = 0;

    /**
     * 3GPP NB-IoT (Narrowband Internet of Things) over Non-Terrestrial-Networks technology.
     * @hide
     */
    @SystemApi
    public static final int NT_RADIO_TECHNOLOGY_NB_IOT_NTN = 1;

    /**
     * 3GPP 5G NR over Non-Terrestrial-Networks technology.
     * @hide
     */
    @SystemApi
    public static final int NT_RADIO_TECHNOLOGY_NR_NTN = 2;

    /**
     * 3GPP eMTC (enhanced Machine-Type Communication) over Non-Terrestrial-Networks technology.
     * @hide
     */
    @SystemApi
    public static final int NT_RADIO_TECHNOLOGY_EMTC_NTN = 3;

    /**
     * Proprietary technology.
     * @hide
     */
    @SystemApi
    public static final int NT_RADIO_TECHNOLOGY_PROPRIETARY = 4;

    /** @hide */
    @IntDef(prefix = "NT_RADIO_TECHNOLOGY_", value = {
            NT_RADIO_TECHNOLOGY_UNKNOWN,
            NT_RADIO_TECHNOLOGY_NB_IOT_NTN,
            NT_RADIO_TECHNOLOGY_NR_NTN,
            NT_RADIO_TECHNOLOGY_EMTC_NTN,
            NT_RADIO_TECHNOLOGY_PROPRIETARY
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface NTRadioTechnology {}

    /**
     * Suggested device hold position is unknown.
     * @hide
     */
    @SystemApi
    public static final int DEVICE_HOLD_POSITION_UNKNOWN = 0;

    /**
     * User is suggested to hold the device in portrait mode.
     * @hide
     */
    @SystemApi
    public static final int DEVICE_HOLD_POSITION_PORTRAIT = 1;

    /**
     * User is suggested to hold the device in landscape mode with left hand.
     * @hide
     */
    @SystemApi
    public static final int DEVICE_HOLD_POSITION_LANDSCAPE_LEFT = 2;

    /**
     * User is suggested to hold the device in landscape mode with right hand.
     * @hide
     */
    @SystemApi
    public static final int DEVICE_HOLD_POSITION_LANDSCAPE_RIGHT = 3;

    /** @hide */
    @IntDef(prefix = {"DEVICE_HOLD_POSITION_"}, value = {
            DEVICE_HOLD_POSITION_UNKNOWN,
            DEVICE_HOLD_POSITION_PORTRAIT,
            DEVICE_HOLD_POSITION_LANDSCAPE_LEFT,
            DEVICE_HOLD_POSITION_LANDSCAPE_RIGHT
       })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DeviceHoldPosition {}

    /**
     *  Display mode is unknown.
     * @hide
     */
    @SystemApi
    public static final int DISPLAY_MODE_UNKNOWN = 0;

    /**
     * Display mode of the device used for satellite communication for non-foldable phones.
     * @hide
     */
    @SystemApi
    public static final int DISPLAY_MODE_FIXED = 1;

    /**
     * Display mode of the device used for satellite communication for foldabale phones when the
     * device is opened.
     * @hide
     */
    @SystemApi
    public static final int DISPLAY_MODE_OPENED = 2;

    /**
     * Display mode of the device used for satellite communication for foldabable phones when the
     * device is closed.
     * @hide
     */
    @SystemApi
    public static final int DISPLAY_MODE_CLOSED = 3;

    /** @hide */
    @IntDef(prefix = {"ANTENNA_POSITION_"}, value = {
            DISPLAY_MODE_UNKNOWN,
            DISPLAY_MODE_FIXED,
            DISPLAY_MODE_OPENED,
            DISPLAY_MODE_CLOSED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DisplayMode {}

    /**
     * Unknown or unsupported value for data mode on satellite.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATA_SUPPORT_UNKNOWN = -1;

    /**
     * Support only restricted data usecases like carrier messaging using RCS.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATA_SUPPORT_RESTRICTED = 0;

    /**
     * Support constrained internet which would enable internet only for applications that are
     * modified.
     *
     * <p>
     * To get internet access, applications need to be modified to use the satellite data
     * optimized network. This can be done by setting the {@link #PROPERTY_SATELLITE_DATA_OPTIMIZED}
     * meta-data to <package-name> of the application in the manifest.
     * </p>
     *
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATA_SUPPORT_CONSTRAINED = 1;

    /**
     * Support default internet on satellite without any restrictions on any apps.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATA_SUPPORT_UNCONSTRAINED = 2;

    /** @hide */
    @IntDef(prefix = {"SATELLITE_DATA_SUPPORT_"}, value = {
        SATELLITE_DATA_SUPPORT_UNKNOWN,
        SATELLITE_DATA_SUPPORT_RESTRICTED,
        SATELLITE_DATA_SUPPORT_CONSTRAINED,
        SATELLITE_DATA_SUPPORT_UNCONSTRAINED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SatelliteDataSupportMode {}

    /**
     * The applications that are optimized for low bandwidth satellite data should set this
     * property to package name of the application in the manifest to indicate to platform
     * about the same.
     * {@code
     * <application>
     *   <meta-data
     *     android:name="android.telephony.PROPERTY_SATELLITE_DATA_OPTIMIZED"
     *     android:value=<package-name>/>
     * </application>
     * }
     * <p>
     * When this meta-data is set in application's manifest,
     * satellite data optimized network is available for application to use.
     */
    public static final String PROPERTY_SATELLITE_DATA_OPTIMIZED =
            "android.telephony.PROPERTY_SATELLITE_DATA_OPTIMIZED";

    /**
     * Registers a {@link SatelliteStateChangeListener} to receive callbacks when the satellite
     * state may have changed.
     *
     * <p>The callback method is immediately triggered with latest state on invoking this method if
     * the state change has been notified before.
     *
     * @param executor The {@link Executor} where the {@code listener} will be invoked
     * @param listener The listener to monitor the satellite state change
     *
     * @see SatelliteStateChangeListener
     * @see TelephonyManager#hasCarrierPrivileges()
     */
    @RequiresPermission(anyOf = {android.Manifest.permission.READ_BASIC_PHONE_STATE,
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PHONE_STATE,
            "carrier privileges"})
    public void registerStateChangeListener(@NonNull @CallbackExecutor Executor executor,
            @NonNull SatelliteStateChangeListener listener) {
        if (mContext == null) {
            throw new IllegalStateException("Telephony service is null");
        }

        mTelephonyRegistryMgr = mContext.getSystemService(TelephonyRegistryManager.class);
        if (mTelephonyRegistryMgr == null) {
            throw new IllegalStateException("Telephony registry service is null");
        }
        mTelephonyRegistryMgr.addSatelliteStateChangeListener(executor, listener);
    }

    /**
     * Unregisters the {@link SatelliteStateChangeListener} previously registered with
     * {@link #registerStateChangeListener(Executor, SatelliteStateChangeListener)}.
     *
     * <p>It will be a no-op if the {@code listener} is not currently registered.
     *
     * @param listener The listener to unregister
     *
     * @see SatelliteStateChangeListener
     * @see TelephonyManager#hasCarrierPrivileges()
     */
    @RequiresPermission(anyOf = {android.Manifest.permission.READ_BASIC_PHONE_STATE,
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PHONE_STATE,
            "carrier privileges"})
    public void unregisterStateChangeListener(@NonNull SatelliteStateChangeListener listener) {
        if (mContext == null) {
            throw new IllegalStateException("Telephony service is null");
        }

        mTelephonyRegistryMgr = mContext.getSystemService(TelephonyRegistryManager.class);
        if (mTelephonyRegistryMgr == null) {
            throw new IllegalStateException("Telephony registry service is null");
        }
        mTelephonyRegistryMgr.removeSatelliteStateChangeListener(listener);
    }

    /**
     * The default state indicating that datagram transfer is idle.
     * This should be sent if there are no message transfer activity happening.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE = 0;

    /**
     * A transition state indicating that a datagram is being sent.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING = 1;

    /**
     * An end state indicating that datagram sending completed successfully.
     * After datagram transfer completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE}
     * will be sent if no more messages are pending.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS = 2;

    /**
     * An end state indicating that datagram sending completed with a failure.
     * After datagram transfer completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE}
     * must be sent before reporting any additional datagram transfer state changes. All pending
     * messages will be reported as failed, to the corresponding applications.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED = 3;

    /**
     * A transition state indicating that a datagram is being received.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING = 4;

    /**
     * An end state indicating that datagram receiving completed successfully.
     * After datagram transfer completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE}
     * will be sent if no more messages are pending.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_SUCCESS = 5;

    /**
     * An end state indicating that datagram receive operation found that there are no
     * messages to be retrieved from the satellite.
     * After datagram transfer completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE}
     * will be sent if no more messages are pending.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_NONE = 6;

    /**
     * An end state indicating that datagram receive completed with a failure.
     * After datagram transfer completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE}
     * will be sent if no more messages are pending.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED = 7;

    /**
     * A transition state indicating that Telephony is waiting for satellite modem to connect to a
     * satellite network before sending a datagram or polling for datagrams. If the satellite modem
     * successfully connects to a satellite network, either
     * {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING} or
     * {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING} will be sent. Otherwise,
     * either {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED} or
     * {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED} will be sent.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT = 8;

    /**
     * The datagram transfer state is unknown. This generic datagram transfer state should be used
     * only when the datagram transfer state cannot be mapped to other specific datagram transfer
     * states.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_UNKNOWN = -1;

    /** @hide */
    @IntDef(prefix = {"SATELLITE_DATAGRAM_TRANSFER_STATE_"}, value = {
            SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
            SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING,
            SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS,
            SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
            SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING,
            SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_SUCCESS,
            SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_NONE,
            SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED,
            SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT,
            SATELLITE_DATAGRAM_TRANSFER_STATE_UNKNOWN
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SatelliteDatagramTransferState {}
    // TODO: Split into two enums for sending and receiving states

    /**
     * Satellite modem is in idle state.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_MODEM_STATE_IDLE = 0;

    /**
     * Satellite modem is listening for incoming datagrams.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_MODEM_STATE_LISTENING = 1;

    /**
     * Satellite modem is sending and/or receiving datagrams.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING = 2;

    /**
     * Satellite modem is retrying to send and/or receive datagrams.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_MODEM_STATE_DATAGRAM_RETRYING = 3;

    /**
     * Satellite modem is powered off.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_MODEM_STATE_OFF = 4;

    /**
     * Satellite modem is unavailable.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_MODEM_STATE_UNAVAILABLE = 5;

    /**
     * The satellite modem is powered on but the device is not registered to a satellite cell.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_MODEM_STATE_NOT_CONNECTED = 6;

    /**
     * The satellite modem is powered on and the device is registered to a satellite cell.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_MODEM_STATE_CONNECTED = 7;

    /**
     * The satellite modem is being powered on.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_MODEM_STATE_ENABLING_SATELLITE = 8;

    /**
     * The satellite modem is being powered off.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_MODEM_STATE_DISABLING_SATELLITE = 9;

    /**
     * Satellite modem state is unknown. This generic modem state should be used only when the
     * modem state cannot be mapped to other specific modem states.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_MODEM_STATE_UNKNOWN = -1;

    /** @hide */
    @IntDef(prefix = {"SATELLITE_MODEM_STATE_"}, value = {
            SATELLITE_MODEM_STATE_IDLE,
            SATELLITE_MODEM_STATE_LISTENING,
            SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING,
            SATELLITE_MODEM_STATE_DATAGRAM_RETRYING,
            SATELLITE_MODEM_STATE_OFF,
            SATELLITE_MODEM_STATE_UNAVAILABLE,
            SATELLITE_MODEM_STATE_NOT_CONNECTED,
            SATELLITE_MODEM_STATE_CONNECTED,
            SATELLITE_MODEM_STATE_ENABLING_SATELLITE,
            SATELLITE_MODEM_STATE_DISABLING_SATELLITE,
            SATELLITE_MODEM_STATE_UNKNOWN
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SatelliteModemState {}

    /**
     * Datagram type is unknown. This generic datagram type should be used only when the
     * datagram type cannot be mapped to other specific datagram types.
     * @hide
     */
    @SystemApi
    public static final int DATAGRAM_TYPE_UNKNOWN = 0;

    /**
     * Datagram type indicating that the datagram to be sent or received is of type SOS message.
     * @hide
     */
    @SystemApi
    public static final int DATAGRAM_TYPE_SOS_MESSAGE = 1;

    /**
     * Datagram type indicating that the datagram to be sent or received is of type
     * location sharing.
     * @hide
     */
    @SystemApi
    public static final int DATAGRAM_TYPE_LOCATION_SHARING = 2;

    /**
     * This type of datagram is used to keep the device in satellite connected state or check if
     * there is any incoming message.
     * @hide
     */
    @SystemApi
    public static final int DATAGRAM_TYPE_KEEP_ALIVE = 3;

    /**
     * Datagram type indicating that the datagram to be sent or received is of type SOS message and
     * is the last message to emergency service provider indicating still needs help.
     * @hide
     */
    @SystemApi
    public static final int DATAGRAM_TYPE_LAST_SOS_MESSAGE_STILL_NEED_HELP = 4;

    /**
     * Datagram type indicating that the datagram to be sent or received is of type SOS message and
     * is the last message to emergency service provider indicating no more help is needed.
     * @hide
     */
    @SystemApi
    public static final int DATAGRAM_TYPE_LAST_SOS_MESSAGE_NO_HELP_NEEDED = 5;

    /**
     * Datagram type indicating that the message to be sent or received is of type SMS.
     * @hide
     */
    @SystemApi
    public static final int DATAGRAM_TYPE_SMS = 6;

    /**
     * Datagram type indicating that the message to be sent is an SMS checking
     * for pending incoming SMS.
     * @hide
     */
    @SystemApi
    public static final int DATAGRAM_TYPE_CHECK_PENDING_INCOMING_SMS = 7;

    /** @hide */
    @IntDef(prefix = "DATAGRAM_TYPE_", value = {
            DATAGRAM_TYPE_UNKNOWN,
            DATAGRAM_TYPE_SOS_MESSAGE,
            DATAGRAM_TYPE_LOCATION_SHARING,
            DATAGRAM_TYPE_KEEP_ALIVE,
            DATAGRAM_TYPE_LAST_SOS_MESSAGE_STILL_NEED_HELP,
            DATAGRAM_TYPE_LAST_SOS_MESSAGE_NO_HELP_NEEDED,
            DATAGRAM_TYPE_SMS,
            DATAGRAM_TYPE_CHECK_PENDING_INCOMING_SMS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DatagramType {}

    /**
     * Satellite communication restricted by user.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_COMMUNICATION_RESTRICTION_REASON_USER = 0;

    /**
     * Satellite communication restricted by geolocation. This can be
     * triggered based upon geofence input provided by carrier to enable or disable satellite.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_COMMUNICATION_RESTRICTION_REASON_GEOLOCATION = 1;

    /**
     * Satellite communication restricted by entitlement server. This can be triggered based on
     * the EntitlementStatus value received from the entitlement server to enable or disable
     * satellite.
     * @hide
     */
    @SystemApi
    public static final int SATELLITE_COMMUNICATION_RESTRICTION_REASON_ENTITLEMENT = 2;

    /** @hide */
    @IntDef(prefix = "SATELLITE_COMMUNICATION_RESTRICTION_REASON_", value = {
            SATELLITE_COMMUNICATION_RESTRICTION_REASON_USER,
            SATELLITE_COMMUNICATION_RESTRICTION_REASON_GEOLOCATION,
            SATELLITE_COMMUNICATION_RESTRICTION_REASON_ENTITLEMENT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SatelliteCommunicationRestrictionReason {}

    /**
     * Satellite is disallowed because it is not supported.
     * @hide
     */
    public static final int SATELLITE_DISALLOWED_REASON_NOT_SUPPORTED = 0;

    /**
     * Satellite is disallowed because it has not been provisioned.
     * @hide
     */
    public static final int SATELLITE_DISALLOWED_REASON_NOT_PROVISIONED = 1;

    /**
     * Satellite is disallowed because it is currently outside an allowed region.
     * @hide
     */
    public static final int SATELLITE_DISALLOWED_REASON_NOT_IN_ALLOWED_REGION = 2;

    /**
     * Satellite is disallowed because an unsupported default message application is being used.
     * @hide
     */
    public static final int SATELLITE_DISALLOWED_REASON_UNSUPPORTED_DEFAULT_MSG_APP = 3;

    /**
     * Satellite is disallowed because location settings have been disabled.
     * @hide
     */
    public static final int SATELLITE_DISALLOWED_REASON_LOCATION_DISABLED = 4;

    /** @hide */
    @IntDef(prefix = "SATELLITE_DISALLOWED_REASON_", value = {
            SATELLITE_DISALLOWED_REASON_NOT_SUPPORTED,
            SATELLITE_DISALLOWED_REASON_NOT_PROVISIONED,
            SATELLITE_DISALLOWED_REASON_NOT_IN_ALLOWED_REGION,
            SATELLITE_DISALLOWED_REASON_UNSUPPORTED_DEFAULT_MSG_APP,
            SATELLITE_DISALLOWED_REASON_LOCATION_DISABLED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SatelliteDisallowedReason {}

    /**
     * The emergency call is handed over to carrier-enabled satellite T911 messaging. T911 messages
     * are sent directly to local emergency providers.
     * @hide
     */
    @SystemApi
    public static final int EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_T911 = 2;
}
