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

import android.telephony.ims.aidl.IFeatureProvisioningCallback;
import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresFeature;
import android.annotation.RequiresPermission;
import android.annotation.StringDef;
import android.annotation.SystemApi;
import android.annotation.WorkerThread;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.telephony.ims.aidl.IImsConfigCallback;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;

import com.android.internal.telephony.ITelephony;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Manages IMS provisioning and configuration parameters, as well as callbacks for apps to listen
 * to changes in these configurations.
 *
 * IMS provisioning keys are defined per carrier or OEM using OMA-DM or other provisioning
 * applications and may vary. It is up to the carrier and OEM applications to ensure that the
 * correct provisioning keys are being used when integrating with a vendor's ImsService.
 *
 * Use {@link android.telephony.ims.ImsManager#getProvisioningManager(int)} to get an instance of
 * this manager.
 */
@RequiresFeature(PackageManager.FEATURE_TELEPHONY_IMS)
public class ProvisioningManager {

    /**@hide*/
    @StringDef(prefix = "STRING_QUERY_RESULT_ERROR_", value = {
            STRING_QUERY_RESULT_ERROR_GENERIC,
            STRING_QUERY_RESULT_ERROR_NOT_READY
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StringResultError {}

    /**
     * The query from {@link #getProvisioningStringValue(int)} has resulted in an unspecified error.
     * @hide
     */
    @SystemApi
    public static final String STRING_QUERY_RESULT_ERROR_GENERIC =
            "STRING_QUERY_RESULT_ERROR_GENERIC";

    /**
     * The query from {@link #getProvisioningStringValue(int)} has resulted in an error because the
     * ImsService implementation was not ready for provisioning queries.
     * @hide
     */
    @SystemApi
    public static final String STRING_QUERY_RESULT_ERROR_NOT_READY =
            "STRING_QUERY_RESULT_ERROR_NOT_READY";

    /**
     * Callback for IMS provisioning changes.
     * @hide
     */
    @SystemApi
    public static class Callback {

        private static class CallbackBinder extends IImsConfigCallback.Stub {

            private final Callback mLocalConfigurationCallback;
            private Executor mExecutor;

            private CallbackBinder(Callback localConfigurationCallback) {
                mLocalConfigurationCallback = localConfigurationCallback;
            }

            @Override
            public final void onIntConfigChanged(int item, int value) {
                final long callingIdentity = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() ->
                            mLocalConfigurationCallback.onProvisioningIntChanged(item, value));
                } finally {
                    restoreCallingIdentity(callingIdentity);
                }
            }

            @Override
            public final void onStringConfigChanged(int item, String value) {
                final long callingIdentity = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() ->
                            mLocalConfigurationCallback.onProvisioningStringChanged(item, value));
                } finally {
                    restoreCallingIdentity(callingIdentity);
                }
            }

            private void setExecutor(Executor executor) {
                mExecutor = executor;
            }
        }

        private final CallbackBinder mBinder = new CallbackBinder(this);

        /**
         * Called when a provisioning item has changed.
         * @param item the IMS provisioning key constant, as defined by the OEM.
         * @param value the new integer value of the IMS provisioning key.
         */
        public void onProvisioningIntChanged(int item, int value) {
            // Base Implementation
        }

        /**
         * Called when a provisioning item has changed.
         * @param item the IMS provisioning key constant, as defined by the OEM.
         * @param value the new String value of the IMS configuration constant.
         */
        public void onProvisioningStringChanged(int item, @NonNull String value) {
            // Base Implementation
        }

        /**@hide*/
        public final IImsConfigCallback getBinder() {
            return mBinder;
        }

        /**@hide*/
        public void setExecutor(Executor executor) {
            mBinder.setExecutor(executor);
        }
    }

    /**
     * Callback for IMS provisioning feature changes.
     */
    public abstract static class FeatureProvisioningCallback {

        /**
         * The IMS MMTEL provisioning has changed for a specific capability and IMS
         * registration technology.
         * @param capability The MMTEL capability that provisioning has changed for.
         * @param tech The IMS registration technology associated with the MMTEL capability that
         * provisioning has changed for.
         * @param isProvisioned {@code true} if the capability is provisioned for the technology
         * specified, or {@code false} if the capability is not provisioned for the technology
         * specified.
         */
        public abstract void onFeatureProvisioningChanged(
                @MmTelFeature.MmTelCapabilities.MmTelCapability int capability,
                @ImsRegistrationImplBase.ImsRegistrationTech int tech,
                boolean isProvisioned);

        /**
         * The IMS RCS provisioning has changed for a specific capability and IMS
         * registration technology.
         * @param capability The RCS capability that provisioning has changed for.
         * @param tech The IMS registration technology associated with the RCS capability that
         * provisioning has changed for.
         * @param isProvisioned {@code true} if the capability is provisioned for the technology
         * specified, or {@code false} if the capability is not provisioned for the technology
         * specified.
         */
        public abstract void onRcsFeatureProvisioningChanged(
                @ImsRcsManager.RcsImsCapabilityFlag int capability,
                @ImsRegistrationImplBase.ImsRegistrationTech int tech,
                boolean isProvisioned);

        private static class CallbackBinder extends IFeatureProvisioningCallback.Stub {

            private final FeatureProvisioningCallback mFeatureProvisioningCallback;
            private Executor mExecutor;

            private CallbackBinder(FeatureProvisioningCallback featureProvisioningCallback) {
                mFeatureProvisioningCallback = featureProvisioningCallback;
            }

            @Override
            public final void onFeatureProvisioningChanged(
                    int capability, int tech, boolean isProvisioned) {
                final long callingIdentity = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() ->
                            mFeatureProvisioningCallback.onFeatureProvisioningChanged(
                                    capability, tech, isProvisioned));
                } finally {
                    restoreCallingIdentity(callingIdentity);
                }
            }

            @Override
            public final void onRcsFeatureProvisioningChanged(
                    int capability, int tech, boolean isProvisioned) {
                final long callingIdentity = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() ->
                            mFeatureProvisioningCallback.onRcsFeatureProvisioningChanged(
                                    capability, tech, isProvisioned));
                } finally {
                    restoreCallingIdentity(callingIdentity);
                }
            }

            private void setExecutor(Executor executor) {
                mExecutor = executor;
            }
        }

        private final CallbackBinder mBinder = new CallbackBinder(this);

        /**@hide*/
        public final IFeatureProvisioningCallback getBinder() {
            return mBinder;
        }

        /**@hide*/
        public void setExecutor(Executor executor) {
            mBinder.setExecutor(executor);
        }
}

    private int mSubId;

    /**
     * Register a new {@link FeatureProvisioningCallback}, which is used to listen for
     * IMS feature provisioning updates.
     * <p>
     * When the subscription associated with this callback is removed (SIM removed,
     * ESIM swap,etc...), this callback will automatically be removed.
     *
     * <p> Requires Permission:
     * <ul>
     *     <li> android.Manifest.permission#READ_PRIVILEGED_PHONE_STATE,</li>
     *     <li>{@link android.Manifest.permission#READ_PRECISE_PHONE_STATE},</li>
     *     <li>or that the caller has carrier privileges (see
     *         {@link TelephonyManager#hasCarrierPrivileges()}).</li>
     * </ul>
     *
     * @param executor The executor that the callback methods will be called on.
     * @param callback The callback instance being registered.
     * @throws ImsException if the subscription associated with this callback is
     * valid, but the service crashed, for example. See
     * {@link ImsException#getCode()} for a more detailed reason.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     */
    @RequiresPermission(Manifest.permission.READ_PRECISE_PHONE_STATE)
    public void registerFeatureProvisioningChangedCallback(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull FeatureProvisioningCallback callback) throws ImsException {
        callback.setExecutor(executor);
        try {
            getITelephony().registerFeatureProvisioningChangedCallback(mSubId,
                    callback.getBinder());
        } catch (ServiceSpecificException e) {
            throw new ImsException(e.getMessage(), e.errorCode);
        } catch (RemoteException | IllegalStateException e) {
            throw new ImsException(e.getMessage(), ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Unregisters a previously registered {@link FeatureProvisioningCallback}
     * instance.  When the subscription associated with this
     * callback is removed (SIM removed, ESIM swap, etc...), this callback will
     * automatically be removed. If this method is called for an inactive
     * subscription, it will result in a no-op.
     *
     * @param callback The existing {@link FeatureProvisioningCallback} to be removed.
     * @see #registerFeatureProvisioningChangedCallback(Executor, FeatureProvisioningCallback)
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     */
    public void unregisterFeatureProvisioningChangedCallback(
            @NonNull FeatureProvisioningCallback callback) {
        try {
            getITelephony().unregisterFeatureProvisioningChangedCallback(mSubId,
                    callback.getBinder());
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Query for the String value associated with the provided key.
     *
     * This operation is blocking and should not be performed on the UI thread.
     *
     * @param key A String that represents the provisioning key, which is defined by the OEM.
     * @return a String value for the provided key, {@code null} if the key doesn't exist, or
     * {@link StringResultError} if there was an error getting the value for the provided key.
     * @throws IllegalArgumentException if the key provided was invalid.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     * @hide
     */
    @SystemApi
    @WorkerThread
    @RequiresPermission(Manifest.permission.READ_PRIVILEGED_PHONE_STATE)
    public @Nullable @StringResultError String getProvisioningStringValue(int key) {
        try {
            return getITelephony().getImsProvisioningString(mSubId, key);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Set the provisioning status for the IMS MmTel capability using the specified subscription.
     *
     * Provisioning may or may not be required, depending on the carrier configuration. If
     * provisioning is not required for the carrier associated with this subscription or the device
     * does not support the capability/technology combination specified, this operation will be a
     * no-op.
     *
     * <p>Requires Permission:
     * <ul>
     *     <li>{@link android.Manifest.permission#MODIFY_PHONE_STATE},</li>
     *     <li>or that the calling app has carrier privileges (see</li>
     *     <li>{@link TelephonyManager#hasCarrierPrivileges}).</li>
     * </ul>
     *
     * @see CarrierConfigManager.Ims#KEY_MMTEL_REQUIRES_PROVISIONING_BUNDLE
     * @param isProvisioned true if the device is provisioned for UT over IMS, false otherwise.
     *
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     */
    @WorkerThread
    @RequiresPermission(Manifest.permission.MODIFY_PHONE_STATE)
    public void setProvisioningStatusForCapability(
            @MmTelFeature.MmTelCapabilities.MmTelCapability int capability,
            @ImsRegistrationImplBase.ImsRegistrationTech int tech,  boolean isProvisioned) {

        try {
            getITelephony().setImsProvisioningStatusForCapability(mSubId, capability, tech,
                        isProvisioned);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Get the provisioning status for the IMS MmTel capability specified.
     *
     * If provisioning is not required for the queried {@code capability} and
     * {@code tech} combination specified, this method will
     * always return {@code true}.
     *
     * <p> Requires Permission:
     * <ul>
     *     <li>android.Manifest.permission#READ_PRIVILEGED_PHONE_STATE,</li>
     *     <li>{@link android.Manifest.permission#READ_PRECISE_PHONE_STATE},</li>
     *     <li>or that the caller has carrier privileges (see
     *         {@link TelephonyManager#hasCarrierPrivileges()}).</li>
     * </ul>
     *
     * @see CarrierConfigManager.Ims#KEY_MMTEL_REQUIRES_PROVISIONING_BUNDLE
     * @return true if the device is provisioned for the capability or does not require
     * provisioning, false if the capability does require provisioning and has not been
     * provisioned yet.
     *
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     */
    @WorkerThread
    @RequiresPermission(Manifest.permission.READ_PRECISE_PHONE_STATE)
    public boolean getProvisioningStatusForCapability(
            @MmTelFeature.MmTelCapabilities.MmTelCapability int capability,
            @ImsRegistrationImplBase.ImsRegistrationTech int tech) {
        try {
            return getITelephony().getImsProvisioningStatusForCapability(mSubId, capability, tech);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Get the provisioning status for the IMS RCS capability specified.
     *
     * If provisioning is not required for the queried
     * {@code capability} or if the device does not support IMS
     * this method will always return {@code true}.
     *
     * @see CarrierConfigManager.Ims#KEY_CARRIER_RCS_PROVISIONING_REQUIRED_BOOL
     * @return true if the device is provisioned for the capability or does not require
     * provisioning, false if the capability does require provisioning and has not been
     * provisioned yet.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     *
     * @deprecated Use {@link #getRcsProvisioningStatusForCapability(int, int)} instead,
     * as this only retrieves provisioning information for
     * {@link ImsRegistrationImplBase#REGISTRATION_TECH_LTE}
     * @hide
     */
    @Deprecated
    @SystemApi
    @WorkerThread
    @RequiresPermission(Manifest.permission.READ_PRIVILEGED_PHONE_STATE)
    public boolean getRcsProvisioningStatusForCapability(
            @ImsRcsManager.RcsImsCapabilityFlag int capability) {
        try {
            return getITelephony().getRcsProvisioningStatusForCapability(mSubId, capability,
            ImsRegistrationImplBase.REGISTRATION_TECH_LTE);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Get the provisioning status for the IMS RCS capability specified.
     *
     * If provisioning is not required for the queried
     * {@code capability} or if the device does not support IMS
     * this method will always return {@code true}.
     *
     * <p> Requires Permission:
     * <ul>
     *     <li>{@link android.Manifest.permission#READ_PRECISE_PHONE_STATE},</li>
     *     <li>or that the caller has carrier privileges (see
     *         {@link TelephonyManager#hasCarrierPrivileges()}).</li>
     * </ul>
     *
     * @see CarrierConfigManager.Ims#KEY_RCS_REQUIRES_PROVISIONING_BUNDLE
     * @return true if the device is provisioned for the capability or does not require
     * provisioning, false if the capability does require provisioning and has not been
     * provisioned yet.
     *
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     */
    @WorkerThread
    @RequiresPermission(Manifest.permission.READ_PRECISE_PHONE_STATE)
    public boolean getRcsProvisioningStatusForCapability(
            @ImsRcsManager.RcsImsCapabilityFlag int capability,
            @ImsRegistrationImplBase.ImsRegistrationTech int tech) {
        try {
            return getITelephony().getRcsProvisioningStatusForCapability(mSubId, capability, tech);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Set the provisioning status for the IMS RCS capability using the specified subscription.
     *
     * <p> Requires Permission:
     * <ul>
     *     <li>{@link android.Manifest.permission#MODIFY_PHONE_STATE}</li>
     *     <li>or that the caller has carrier privileges (see
     *         {@link TelephonyManager#hasCarrierPrivileges()}).</li>
     * </ul>

     * Provisioning may or may not be required, depending on the carrier configuration. If
     * provisioning is not required for the carrier associated with this subscription or the device
     * does not support the capability/technology combination specified, this operation will be a
     * no-op.
     *
     * @see CarrierConfigManager#KEY_CARRIER_RCS_PROVISIONING_REQUIRED_BOOL
     * @param isProvisioned true if the device is provisioned for the RCS capability specified,
     *                      false otherwise.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     *
     * @deprecated Use {@link #setRcsProvisioningStatusForCapability(int, int, boolean)} instead,
     * as this method only sets provisioning information for
     * {@link ImsRegistrationImplBase#REGISTRATION_TECH_LTE}
     * @hide
     */
    @Deprecated
    @SystemApi
    @WorkerThread
    @RequiresPermission(Manifest.permission.MODIFY_PHONE_STATE)
    public void setRcsProvisioningStatusForCapability(
            @ImsRcsManager.RcsImsCapabilityFlag int capability,
            boolean isProvisioned) {
        try {
            getITelephony().setRcsProvisioningStatusForCapability(mSubId, capability,
                    ImsRegistrationImplBase.REGISTRATION_TECH_LTE, isProvisioned);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Set the provisioning status for the IMS RCS capability using the specified subscription.
     *
     * Provisioning may or may not be required, depending on the carrier configuration. If
     * provisioning is not required for the carrier associated with this subscription or the device
     * does not support the capability/technology combination specified, this operation will be a
     * no-op.
     *
     * <p> Requires Permission:
     * <ul>
     *     <li>{@link android.Manifest.permission#MODIFY_PHONE_STATE},</li>
     *     <li>or that the caller has carrier privileges (see
     *         {@link TelephonyManager#hasCarrierPrivileges()}).</li>
     * </ul>
     *
     * @see CarrierConfigManager.Ims#KEY_RCS_REQUIRES_PROVISIONING_BUNDLE
     * @param isProvisioned true if the device is provisioned for the RCS capability specified,
     *                      false otherwise.
     *
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     */
    @WorkerThread
    @RequiresPermission(Manifest.permission.MODIFY_PHONE_STATE)
    public void setRcsProvisioningStatusForCapability(
            @ImsRcsManager.RcsImsCapabilityFlag int capability,
            @ImsRegistrationImplBase.ImsRegistrationTech int tech, boolean isProvisioned) {
        try {
            getITelephony().setRcsProvisioningStatusForCapability(mSubId, capability,
                    tech, isProvisioned);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Indicates whether provisioning for the MMTEL capability and IMS registration technology
     * specified is required or not
     *
     * <p> Requires Permission:
     * <ul>
     *     <li> android.Manifest.permission#READ_PRIVILEGED_PHONE_STATE,</li>
     *     <li>{@link android.Manifest.permission#READ_PRECISE_PHONE_STATE},</li>
     *     <li> or that the caller has carrier privileges (see
     *         {@link TelephonyManager#hasCarrierPrivileges()}).</li>
     * </ul>
     *
     * @return true if provisioning is required for the MMTEL capability and IMS
     * registration technology specified, false if it is not required or if the device does not
     * support IMS.
     *
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     */
    @RequiresPermission(Manifest.permission.READ_PRECISE_PHONE_STATE)
    public boolean isProvisioningRequiredForCapability(
            @MmTelFeature.MmTelCapabilities.MmTelCapability int capability,
            @ImsRegistrationImplBase.ImsRegistrationTech int tech) {
        try {
            return getITelephony().isProvisioningRequiredForCapability(mSubId, capability, tech);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }


    /**
     * Indicates whether provisioning for the RCS capability and IMS registration technology
     * specified is required or not
     *
     * <p> Requires Permission:
     * <ul>
     *     <li> android.Manifest.permission#READ_PRIVILEGED_PHONE_STATE,</li>
     *     <li>{@link android.Manifest.permission#READ_PRECISE_PHONE_STATE},</li>
     *     <li> or that the caller has carrier privileges (see
     *         {@link TelephonyManager#hasCarrierPrivileges()}).</li>
     * </ul>
     *
     * @return true if provisioning is required for the RCS capability and IMS
     * registration technology specified, false if it is not required or if the device does not
     * support IMS.
     *
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     */
    @RequiresPermission(Manifest.permission.READ_PRECISE_PHONE_STATE)
    public boolean isRcsProvisioningRequiredForCapability(
            @ImsRcsManager.RcsImsCapabilityFlag int capability,
            @ImsRegistrationImplBase.ImsRegistrationTech int tech) {
        try {
            return getITelephony().isRcsProvisioningRequiredForCapability(mSubId, capability, tech);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    private static ITelephony getITelephony() {
        throw new RuntimeException("Could not find Telephony Service.");
    
    }

    /**@hide*/
    //@SystemApi
    public ProvisioningManager(int subId) {
        mSubId = subId;
    }
}
