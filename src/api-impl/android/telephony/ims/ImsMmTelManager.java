/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.telephony.ims;


import android.os.Binder;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.feature.ImsFeature;
import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresFeature;
import android.annotation.RequiresPermission;
import android.annotation.SuppressAutoDoc;
import android.annotation.SystemApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.AccessNetworkConstants;
import android.telephony.ims.feature.MmTelFeature;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * A manager for the MmTel (Multimedia Telephony) feature of an IMS network, given an associated
 * subscription.
 *
 * Allows a user to query the IMS MmTel feature information for a subscription, register for
 * registration and MmTel capability status callbacks, as well as query/modify user settings for the
 * associated subscription.
 *
 * Use {@link android.telephony.ims.ImsManager#getImsMmTelManager(int)} to get an instance of this
 * manager.
 */
@RequiresFeature(PackageManager.FEATURE_TELEPHONY_IMS)
public class ImsMmTelManager implements RegistrationManager {

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "WIFI_MODE_", value = {
            WIFI_MODE_UNKNOWN,
            WIFI_MODE_WIFI_ONLY,
            WIFI_MODE_CELLULAR_PREFERRED,
            WIFI_MODE_WIFI_PREFERRED
            })
    public @interface WiFiCallingMode {}

    /**
     * Wifi calling mode is unknown. This is for initialization only.
     * @hide
     */
    public static final int WIFI_MODE_UNKNOWN = -1;

    /**
     * Register for IMS over IWLAN if WiFi signal quality is high enough. Do not hand over to LTE
     * registration if signal quality degrades.
     */
    public static final int WIFI_MODE_WIFI_ONLY = 0;

    /**
     * Prefer registering for IMS over LTE if LTE signal quality is high enough.
     */
    public static final int WIFI_MODE_CELLULAR_PREFERRED = 1;

    /**
     * Prefer registering for IMS over IWLAN if possible if WiFi signal quality is high enough.
     */
    public static final int WIFI_MODE_WIFI_PREFERRED = 2;

    /**
     * Callback class for receiving IMS network Registration callback events.
     * @see #registerImsRegistrationCallback(Executor, RegistrationCallback) (RegistrationCallback)
     * @see #unregisterImsRegistrationCallback(RegistrationCallback)
     * @deprecated Use {@link RegistrationManager.RegistrationCallback} instead.
     * @hide
     */
    // Do not add to this class, add to RegistrationManager.RegistrationCallback instead.
    @Deprecated
    @SystemApi
    public static class RegistrationCallback extends RegistrationManager.RegistrationCallback {

        /**
         * Notifies the framework when the IMS Provider is registered to the IMS network.
         *
         * @param imsTransportType the radio access technology.
         */
        @Override
        public void onRegistered(@AccessNetworkConstants.TransportType int imsTransportType) {
        }

        /**
         * Notifies the framework when the IMS Provider is trying to register the IMS network.
         *
         * @param imsTransportType the radio access technology.
         */
        @Override
        public void onRegistering(@AccessNetworkConstants.TransportType int imsTransportType) {
        }

        /**
         * Notifies the framework when the IMS Provider is deregistered from the IMS network.
         *
         * @param info the {@link ImsReasonInfo} associated with why registration was disconnected.
         */
        @Override
        public void onUnregistered(@NonNull ImsReasonInfo info) {
        }

        /**
         * A failure has occurred when trying to handover registration to another technology type.
         *
         * @param imsTransportType The transport type that has failed to handover registration to.
         * @param info A {@link ImsReasonInfo} that identifies the reason for failure.
         */
        @Override
        public void onTechnologyChangeFailed(
                @AccessNetworkConstants.TransportType int imsTransportType,
                @NonNull ImsReasonInfo info) {
        }
    }

    /**
     * Receives IMS capability status updates from the ImsService.
     *
     * @see #registerMmTelCapabilityCallback(Executor, CapabilityCallback) (CapabilityCallback)
     * @see #unregisterMmTelCapabilityCallback(CapabilityCallback)
     */
    public static class CapabilityCallback {

        /**
         * The status of the feature's capabilities has changed to either available or unavailable.
         * If unavailable, the feature is not able to support the unavailable capability at this
         * time.
         *
         * @param capabilities The new availability of the capabilities.
         */
        public void onCapabilitiesStatusChanged(
                @NonNull MmTelFeature.MmTelCapabilities capabilities) {
        }

        private static class CapabilityBinder extends IImsCapabilityCallback.Stub {

            private final CapabilityCallback mLocalCallback;
            private Executor mExecutor;

            CapabilityBinder(CapabilityCallback c) {
                mLocalCallback = c;
            }

            @Override
            public void onCapabilitiesStatusChanged(int config) {
                if (mLocalCallback == null) return;

                final long callingIdentity = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() -> mLocalCallback.onCapabilitiesStatusChanged(
                            new MmTelFeature.MmTelCapabilities(config)));
                } finally {
                    restoreCallingIdentity(callingIdentity);
                }
            }

            @Override
            public void onQueryCapabilityConfiguration(int capability, int radioTech,
                    boolean isEnabled) {
                // This is not used for public interfaces.
            }

            @Override
            public void onChangeCapabilityConfigurationError(int capability, int radioTech,
                    @ImsFeature.ImsCapabilityError int reason) {
                // This is not used for public interfaces
            }

            private void setExecutor(Executor executor) {
                mExecutor = executor;
            }
        }

        private final CapabilityBinder mBinder = new CapabilityBinder(this);

        /**@hide*/
        public final IImsCapabilityCallback getBinder() {
            return mBinder;
        }

        /**@hide*/
        // Only exposed as public method for compatibility with deprecated ImsManager APIs.
        // TODO: clean up dependencies and change back to private visibility.
        public final void setExecutor(Executor executor) {
            mBinder.setExecutor(executor);
        }
}

    private final Context mContext;
    private final int mSubId;

    /**
     * Only visible for testing, use {@link ImsManager#getImsMmTelManager(int)} instead.
     * @hide
     */
    @VisibleForTesting
    public ImsMmTelManager(int subId) {
        this(null, subId);
    }

    /**
     * Only visible for testing, use {@link ImsManager#getImsMmTelManager(int)} instead.
     * @hide
     */
    @VisibleForTesting
    public ImsMmTelManager(Context context, int subId) {
        mContext = context;
        mSubId = subId;
    }

    /**
     * Registers a {@link RegistrationCallback} with the system, which will provide registration
     * updates for the subscription specified in {@link ImsManager#getImsMmTelManager(int)}. Use
     * {@link SubscriptionManager.OnSubscriptionsChangedListener} to listen to Subscription changed
     * events and call {@link #unregisterImsRegistrationCallback(RegistrationCallback)} to clean up.
     *
     * When the callback is registered, it will initiate the callback c to be called with the
     * current registration state.
     *
     * @param executor The executor the callback events should be run on.
     * @param c The {@link RegistrationCallback} to be added.
     * @see #unregisterImsRegistrationCallback(RegistrationCallback)
     * @throws IllegalArgumentException if the subscription associated with this callback is not
     * active (SIM is not inserted, ESIM inactive) or invalid, or a null {@link Executor} or
     * {@link CapabilityCallback} callback.
     * @throws ImsException if the subscription associated with this callback is valid, but
     * the {@link ImsService} associated with the subscription is not available. This can happen if
     * the service crashed, for example. See {@link ImsException#getCode()} for a more detailed
     * reason.
     * @deprecated Use {@link RegistrationManager#registerImsRegistrationCallback(Executor,
     * RegistrationManager.RegistrationCallback)} instead.
     * @hide
     */
    @Deprecated
    @SystemApi
    @RequiresPermission(Manifest.permission.READ_PRIVILEGED_PHONE_STATE)
    public void registerImsRegistrationCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull RegistrationCallback c) throws ImsException {
        if (c == null) {
            throw new IllegalArgumentException("Must include a non-null RegistrationCallback.");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Must include a non-null Executor.");
        }
        c.setExecutor(executor);

        throw new ImsException("Could not find Telephony Service.",
                ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
    
    }

     /**
     *
     * <p>Requires Permission: {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE
     * READ_PRECISE_PHONE_STATE} or that the calling app has carrier privileges
     * (see {@link android.telephony.TelephonyManager#hasCarrierPrivileges}).
     *
     * {@inheritDoc}
     *
     */
    @Override
    @SuppressAutoDoc // No support for device / profile owner or carrier privileges (b/72967236).
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PRECISE_PHONE_STATE})
    public void registerImsRegistrationCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull RegistrationManager.RegistrationCallback c) throws ImsException {
        if (c == null) {
            throw new IllegalArgumentException("Must include a non-null RegistrationCallback.");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Must include a non-null Executor.");
        }
        c.setExecutor(executor);

        throw new ImsException("Could not find Telephony Service.",
                ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
    
    }

    /**
     * Removes an existing {@link RegistrationCallback}.
     *
     * When the subscription associated with this callback is removed (SIM removed, ESIM swap,
     * etc...), this callback will automatically be removed. If this method is called for an
     * inactive subscription, it will result in a no-op.
     *
     * @param c The {@link RegistrationCallback} to be removed.
     * @see SubscriptionManager.OnSubscriptionsChangedListener
     * @see #registerImsRegistrationCallback(Executor, RegistrationCallback)
     * @deprecated Use {@link #unregisterImsRegistrationCallback(
     * RegistrationManager.RegistrationCallback)}.
     * @hide
     */
    @Deprecated
    @SystemApi
    @RequiresPermission(Manifest.permission.READ_PRIVILEGED_PHONE_STATE)
    public void unregisterImsRegistrationCallback(@NonNull RegistrationCallback c) {
        if (c == null) {
            throw new IllegalArgumentException("Must include a non-null RegistrationCallback.");
        }

        throw new RuntimeException("Could not find Telephony Service.");
    
    }

     /**
     *
     * <p>Requires Permission: {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE
     * READ_PRECISE_PHONE_STATE} or that the calling app has carrier privileges
     * (see {@link android.telephony.TelephonyManager#hasCarrierPrivileges}).
     * Access by profile owners is deprecated and will be removed in a future release.
     *
     *{@inheritDoc}
     */
    @Override
    @SuppressAutoDoc // No support for device / profile owner or carrier privileges (b/72967236).
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PRECISE_PHONE_STATE})
    public void unregisterImsRegistrationCallback(
            @NonNull RegistrationManager.RegistrationCallback c) {
        if (c == null) {
            throw new IllegalArgumentException("Must include a non-null RegistrationCallback.");
        }

        throw new RuntimeException("Could not find Telephony Service.");
    
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @Override
    @SystemApi
    @RequiresPermission(android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE)
    public void getRegistrationState(@NonNull @CallbackExecutor Executor executor,
            @NonNull @ImsRegistrationState Consumer<Integer> stateCallback) {
        if (stateCallback == null) {
            throw new IllegalArgumentException("Must include a non-null callback.");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Must include a non-null Executor.");
        }

        throw new RuntimeException("Could not find Telephony Service.");
    
    }

    /**
     * <p>Requires Permission: {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE
     * READ_PRECISE_PHONE_STATE} or that the calling app has carrier privileges
     * (see {@link android.telephony.TelephonyManager#hasCarrierPrivileges}).
     * Access by profile owners is deprecated and will be removed in a future release.
     *
     *{@inheritDoc}
     */
    @Override
    @SuppressAutoDoc // No support for device / profile owner or carrier privileges (b/72967236).
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PRECISE_PHONE_STATE})
    public void getRegistrationTransportType(@NonNull @CallbackExecutor Executor executor,
            @NonNull @AccessNetworkConstants.TransportType
                    Consumer<Integer> transportTypeCallback) {
        if (transportTypeCallback == null) {
            throw new IllegalArgumentException("Must include a non-null callback.");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Must include a non-null Executor.");
        }

        throw new RuntimeException("Could not find Telephony Service.");
    
    }

    /**
     * Registers a {@link CapabilityCallback} with the system, which will provide MmTel service
     * availability updates for the subscription specified in
     * {@link ImsManager#getImsMmTelManager(int)}.
     *
     * Use {@link SubscriptionManager.OnSubscriptionsChangedListener} to listen to
     * subscription changed events and call
     * {@link #unregisterMmTelCapabilityCallback(CapabilityCallback)} to clean up.
     * <p>This API requires one of the following:
     * <ul>
     *     <li>The caller holds the READ_PRECISE_PHONE_STATE permission.</li>
     *     <li>If the caller is the device or profile owner, the caller holds the
     *     {@link Manifest.permission#READ_PRECISE_PHONE_STATE} permission.</li>
     *     <li>The caller has carrier privileges (see
     *     {@link android.telephony.TelephonyManager#hasCarrierPrivileges}) on any
     *     active subscription.</li>
     * </ul>
     * <p>The profile owner is an app that owns a managed profile on the device; for more details
     * see <a href="https://developer.android.com/work/managed-profiles">Work profiles</a>.
     * Access by profile owners is deprecated and will be removed in a future release.
     *
     * When the callback is registered, it will initiate the callback c to be called with the
     * current capabilities.
     *
     * @param executor The executor the callback events should be run on.
     * @param c The MmTel {@link CapabilityCallback} to be registered.
     * @see #unregisterMmTelCapabilityCallback(CapabilityCallback)
     * @throws ImsException if the subscription associated with this callback is valid, but
     * the {@code ImsService} associated with the subscription is not available. This can happen if
     * the service crashed, for example. See {@link ImsException#getCode()} for a more detailed
     * reason.
     */
    @SuppressAutoDoc // No support for device / profile owner or carrier privileges (b/72967236).
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PRECISE_PHONE_STATE})
    public void registerMmTelCapabilityCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull CapabilityCallback c) throws ImsException {
        if (c == null) {
            throw new IllegalArgumentException("Must include a non-null RegistrationCallback.");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Must include a non-null Executor.");
        }
        c.setExecutor(executor);

        throw new ImsException("Could not find Telephony Service.",
                ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
    
    }

    /**
     * Removes an existing MmTel {@link CapabilityCallback}.
     *
     * When the subscription associated with this callback is removed (SIM removed, ESIM swap,
     * etc...), this callback will automatically be removed. If this method is called for an
     * inactive subscription, it will result in a no-op.
     * <p>This API requires one of the following:
     * <ul>
     *     <li>The caller holds the READ_PRECISE_PHONE_STATE permission.</li>
     *     <li>If the caller is the device or profile owner, the caller holds the
     *     {@link Manifest.permission#READ_PRECISE_PHONE_STATE} permission.</li>
     *     <li>The caller has carrier privileges (see
     *     {@link android.telephony.TelephonyManager#hasCarrierPrivileges}) on any
     *     active subscription.</li>
     * </ul>
     * <p>The profile owner is an app that owns a managed profile on the device; for more details
     * see <a href="https://developer.android.com/work/managed-profiles">Work profiles</a>.
     * Access by profile owners is deprecated and will be removed in a future release.
     *
     * @param c The MmTel {@link CapabilityCallback} to be removed.
     * @see #registerMmTelCapabilityCallback(Executor, CapabilityCallback)
     */
    @SuppressAutoDoc // No support for device / profile owner or carrier privileges (b/72967236).
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PRECISE_PHONE_STATE})
    public void unregisterMmTelCapabilityCallback(@NonNull CapabilityCallback c) {
        if (c == null) {
            throw new IllegalArgumentException("Must include a non-null RegistrationCallback.");
        }

        Log.w("ImsMmTelManager", "Could not find Telephony Service.");
        return;
    
    }

    /**
     * Query the user’s setting for “Advanced Calling” or "Enhanced 4G LTE", which is used to
     * enable MmTel IMS features, depending on the carrier configuration for the current
     * subscription. If this setting is enabled, IMS voice and video telephony over IWLAN/LTE will
     * be enabled as long as the carrier has provisioned these services for the specified
     * subscription. Other IMS services (SMS/UT) are not affected by this user setting and depend on
     * carrier requirements.
     * <p>
     * Note: If the carrier configuration for advanced calling is not editable or hidden, this
     * method will always return the default value.
     * <p>This API requires one of the following:
     * <ul>
     *     <li>The caller holds the READ_PRECISE_PHONE_STATE permission.</li>
     *     <li>If the caller is the device or profile owner, the caller holds the
     *     {@link Manifest.permission#READ_PRECISE_PHONE_STATE} permission.</li>
     *     <li>The caller has carrier privileges (see
     *     {@link android.telephony.TelephonyManager#hasCarrierPrivileges}) on any
     *     active subscription.</li>
     * </ul>
     * <p>The profile owner is an app that owns a managed profile on the device; for more details
     * see <a href="https://developer.android.com/work/managed-profiles">Work profiles</a>.
     * Access by profile owners is deprecated and will be removed in a future release.
     *
     * @see android.telephony.CarrierConfigManager#KEY_CARRIER_VOLTE_PROVISIONING_REQUIRED_BOOL
     * @see android.telephony.CarrierConfigManager#KEY_EDITABLE_ENHANCED_4G_LTE_BOOL
     * @see android.telephony.CarrierConfigManager#KEY_HIDE_ENHANCED_4G_LTE_BOOL
     * @see android.telephony.CarrierConfigManager#KEY_ENHANCED_4G_LTE_ON_BY_DEFAULT_BOOL
     * @see android.telephony.CarrierConfigManager#KEY_CARRIER_VOLTE_AVAILABLE_BOOL
     * @throws IllegalArgumentException if the subscription associated with this operation is not
     * active (SIM is not inserted, ESIM inactive) or invalid.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     * @return true if the user's setting for advanced calling is enabled, false otherwise.
     */
    @SuppressAutoDoc // No support for device / profile owner or carrier privileges (b/72967236).
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PRECISE_PHONE_STATE})
    public boolean isAdvancedCallingSettingEnabled() {
        throw new RuntimeException("Could not find Telephony Service.");
    
    }

    /**
     * The user's setting for whether or not they have enabled the "Video Calling" setting.
     *
     * <p>
     * Note: If the carrier configuration for advanced calling is not editable or hidden, this
     * method will always return the default value.
     * <p>This API requires one of the following:
     * <ul>
     *     <li>The caller holds the READ_PRECISE_PHONE_STATE permission.</li>
     *     <li>If the caller is the device or profile owner, the caller holds the
     *     {@link Manifest.permission#READ_PRECISE_PHONE_STATE} permission.</li>
     *     <li>The caller has carrier privileges (see
     *     {@link android.telephony.TelephonyManager#hasCarrierPrivileges}) on any
     *     active subscription.</li>
     * </ul>
     * <p>The profile owner is an app that owns a managed profile on the device; for more details
     * see <a href="https://developer.android.com/work/managed-profiles">Work profiles</a>.
     * Access by profile owners is deprecated and will be removed in a future release.
     *
     * @throws IllegalArgumentException if the subscription associated with this operation is not
     * active (SIM is not inserted, ESIM inactive) or invalid.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     * @return true if the user’s “Video Calling” setting is currently enabled.
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PRECISE_PHONE_STATE})
    @SuppressAutoDoc // No support for device / profile owner or carrier privileges (b/72967236).
    public boolean isVtSettingEnabled() {
        throw new RuntimeException("Could not find Telephony Service.");
    
    }

    /**
     * @return true if the user's setting for Voice over WiFi is enabled and false if it is not.
     *
     * <p>This API requires one of the following:
     * <ul>
     *     <li>The caller holds the READ_PRECISE_PHONE_STATE permission.</li>
     *     <li>If the caller is the device or profile owner, the caller holds the
     *     {@link Manifest.permission#READ_PRECISE_PHONE_STATE} permission.</li>
     *     <li>The caller has carrier privileges (see
     *     {@link android.telephony.TelephonyManager#hasCarrierPrivileges}) on any
     *     active subscription.</li>
     * </ul>
     * <p>The profile owner is an app that owns a managed profile on the device; for more details
     * see <a href="https://developer.android.com/work/managed-profiles">Work profiles</a>.
     * Access by profile owners is deprecated and will be removed in a future release.
     *
     * @throws IllegalArgumentException if the subscription associated with this operation is not
     * active (SIM is not inserted, ESIM inactive) or invalid.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     */
    @SuppressAutoDoc // No support for device / profile owner or carrier privileges (b/72967236).
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PRECISE_PHONE_STATE})
    public boolean isVoWiFiSettingEnabled() {
        throw new RuntimeException("Could not find Telephony Service.");
    
    }

    /**
     * This configuration is meaningful only on dual sim device.
     * If enabled, this will result in the device setting up IMS of all other
     * active subscriptions over the INTERNET APN of the primary default data subscription
     * when any of those subscriptions are roaming or out of service and if wifi is not available
     * for VoWifi. This feature will be disabled if
     * {@link CarrierConfigManager#KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL} is set to false.
     * <p>Following are the conditions in which system will try to register IMS over
     * cross sim
     * <ul>
     *     <li>Wifi is not available, one SIM is roaming and the default data
     *     SIM is in home network. Then roaming SIM IMS will be registered over INTERNET APN of the
     *     default data subscription </li>
     *     <li>Wifi is not available, one SIM is out of service and the default data
     *     SIM is in home network. Then out of service SIM IMS will be registered over INTERNET
     *     APN of the default data subscription </li>
     * </ul>
     * <p>This API requires one of the following:
     * <ul>
     *     <li>The caller holds the READ_PRECISE_PHONE_STATE permission.</li>
     *     <li>If the caller is the device or profile owner, the caller holds the
     *     {@link Manifest.permission#READ_PRECISE_PHONE_STATE} permission.</li>
     *     <li>The caller has carrier privileges (see
     *     {@link android.telephony.TelephonyManager#hasCarrierPrivileges}) on any
     *     active subscription.</li>
     * </ul>
     * <p>The profile owner is an app that owns a managed profile on the device; for more details
     * see <a href="https://developer.android.com/work/managed-profiles">Work profiles</a>.
     * Access by profile owners is deprecated and will be removed in a future release.
     *
     * @throws ImsException if the IMS service associated with this subscription is not available or
     * the IMS service is not available.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     * @return true if the user's setting for Voice over Cross SIM is enabled and false if it is not
     */
    @SuppressAutoDoc // No support for device / profile owner or carrier privileges (b/72967236).
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PRECISE_PHONE_STATE})
    public boolean isCrossSimCallingEnabled() throws ImsException {
        throw new ImsException("Could not find Telephony Service.",
                ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
    
    }

    /**
     * Returns the user's voice over WiFi roaming setting associated with the current subscription.
     *
     * <p>This API requires one of the following:
     * <ul>
     *     <li>The caller holds the READ_PRECISE_PHONE_STATE permission.</li>
     *     <li>If the caller is the device or profile owner, the caller holds the
     *     {@link Manifest.permission#READ_PRECISE_PHONE_STATE} permission.</li>
     *     <li>The caller has carrier privileges (see
     *     {@link android.telephony.TelephonyManager#hasCarrierPrivileges}) on any
     *     active subscription.</li>
     * </ul>
     * <p>The profile owner is an app that owns a managed profile on the device; for more details
     * see <a href="https://developer.android.com/work/managed-profiles">Work profiles</a>.
     * Access by profile owners is deprecated and will be removed in a future release.
     *
     * @throws IllegalArgumentException if the subscription associated with this operation is not
     * active (SIM is not inserted, ESIM inactive) or invalid.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     * @return true if the user's setting for Voice over WiFi while roaming is enabled, false
     * if disabled.
     */
    @SuppressAutoDoc // No support for device / profile owner or carrier privileges (b/72967236).
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PRECISE_PHONE_STATE})
    public boolean isVoWiFiRoamingSettingEnabled() {
        throw new RuntimeException("Could not find Telephony Service.");
    
    }

    /**
     * Returns the user's voice over WiFi Roaming mode setting associated with the device.
     *
     * <p>This API requires one of the following:
     * <ul>
     *     <li>The caller holds the READ_PRECISE_PHONE_STATE permission.</li>
     *     <li>If the caller is the device or profile owner, the caller holds the
     *     {@link Manifest.permission#READ_PRECISE_PHONE_STATE} permission.</li>
     *     <li>The caller has carrier privileges (see
     *     {@link android.telephony.TelephonyManager#hasCarrierPrivileges}) on any
     *     active subscription.</li>
     * </ul>
     * <p>The profile owner is an app that owns a managed profile on the device; for more details
     * see <a href="https://developer.android.com/work/managed-profiles">Work profiles</a>.
     * Access by profile owners is deprecated and will be removed in a future release.
     *
     * @throws IllegalArgumentException if the subscription associated with this operation is not
     * active (SIM is not inserted, ESIM inactive) or invalid.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     * @return The Voice over WiFi Mode preference set by the user, which can be one of the
     * following:
     * - {@link #WIFI_MODE_WIFI_ONLY}
     * - {@link #WIFI_MODE_CELLULAR_PREFERRED}
     * - {@link #WIFI_MODE_WIFI_PREFERRED}
     */
    @SuppressAutoDoc // No support for device / profile owner or carrier privileges (b/72967236).
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PRECISE_PHONE_STATE})
    public @WiFiCallingMode int getVoWiFiModeSetting() {
        throw new RuntimeException("Could not find Telephony Service.");
    
    }

    /**
     * @return true if TTY over VoLTE is supported
     *
     * <p>This API requires one of the following:
     * <ul>
     *     <li>The caller holds the READ_PRECISE_PHONE_STATE permission.</li>
     *     <li>If the caller is the device or profile owner, the caller holds the
     *     {@link Manifest.permission#READ_PRECISE_PHONE_STATE} permission.</li>
     *     <li>The caller has carrier privileges (see
     *     {@link android.telephony.TelephonyManager#hasCarrierPrivileges}) on any
     *     active subscription.</li>
     * </ul>
     * <p>The profile owner is an app that owns a managed profile on the device; for more details
     * see <a href="https://developer.android.com/work/managed-profiles">Work profiles</a>.
     * Access by profile owners is deprecated and will be removed in a future release.
     *
     * @throws IllegalArgumentException if the subscription associated with this operation is not
     * active (SIM is not inserted, ESIM inactive) or invalid.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     * @see android.telephony.CarrierConfigManager#KEY_CARRIER_VOLTE_TTY_SUPPORTED_BOOL
     */
    @SuppressAutoDoc // No support for device / profile owner or carrier privileges (b/72967236).
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
            android.Manifest.permission.READ_PRECISE_PHONE_STATE})
    public boolean isTtyOverVolteEnabled() {
        throw new RuntimeException("Could not find Telephony Service.");
    
    }

    /**
     * Register a new callback, which is used to notify the registrant of changes to
     * the state of the underlying IMS service that is attached to telephony to
     * implement IMS functionality. If the manager is created for
     * the {@link SubscriptionManager#DEFAULT_SUBSCRIPTION_ID},
     * this throws an {@link ImsException}.
     *
     * <p>Requires Permission:
     * {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE READ_PRECISE_PHONE_STATE}
     * or that the calling app has carrier privileges
     * (see {@link android.telephony.TelephonyManager#hasCarrierPrivileges}).
     *
     * @param executor the Executor that will be used to call the {@link ImsStateCallback}.
     * @param callback The callback instance being registered.
     * @throws ImsException in the case that the callback can not be registered.
     * See {@link ImsException#getCode} for more information on when this is called.
     */
    @RequiresPermission(anyOf = {Manifest.permission.READ_PRECISE_PHONE_STATE,
            Manifest.permission.READ_PRIVILEGED_PHONE_STATE})
    public void registerImsStateCallback(@NonNull Executor executor,
            @NonNull ImsStateCallback callback) throws ImsException {
        Objects.requireNonNull(callback, "Must include a non-null ImsStateCallback.");
        Objects.requireNonNull(executor, "Must include a non-null Executor.");

        callback.init(executor);
        throw new ImsException("Telephony server is down",
                ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
    
    }

    /**
     * Unregisters a previously registered callback.
     *
     * @param callback The callback instance to be unregistered.
     */
    public void unregisterImsStateCallback(@NonNull ImsStateCallback callback) {
        Objects.requireNonNull(callback, "Must include a non-null ImsStateCallback.");

        // ATL: registerImsStateCallback() never registered the callback (no telephony service),
        // AOSP's binder cache has no telephony for it either and does nothing
    }

    private String getOpPackageName() {
        if (mContext != null) {
            return mContext.getOpPackageName();
        } else {
            return null;
        }
    }
}
