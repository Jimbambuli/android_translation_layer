/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package android.telephony.ims.stub;

import android.annotation.IntDef;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Controls IMS registration for this ImsService and notifies the framework when the IMS
 * registration for this ImsService has changed status.
 * <p>
 * Note: There is no guarantee on the thread that the calls from the framework will be called on. It
 * is the implementors responsibility to handle moving the calls to a working thread if required.
 */
public class ImsRegistrationImplBase {
    /**
     * @hide
     */
    // Defines the underlying radio technology type that we have registered for IMS over.
    @IntDef(value = {
                    REGISTRATION_TECH_NONE,
                    REGISTRATION_TECH_LTE,
                    REGISTRATION_TECH_IWLAN,
                    REGISTRATION_TECH_CROSS_SIM,
                    REGISTRATION_TECH_NR,
                    REGISTRATION_TECH_3G
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsRegistrationTech {}
    /**
     * No registration technology specified, used when we are not registered.
     */
    public static final int REGISTRATION_TECH_NONE = -1;
    /**
     * This ImsService is registered to IMS via LTE.
     */
    public static final int REGISTRATION_TECH_LTE = 0;
    /**
     * This ImsService is registered to IMS via IWLAN.
     */
    public static final int REGISTRATION_TECH_IWLAN = 1;

    /**
     * This ImsService is registered to IMS via internet over second subscription.
     */
    public static final int REGISTRATION_TECH_CROSS_SIM = 2;

    /**
     * This ImsService is registered to IMS via NR.
     */
    public static final int REGISTRATION_TECH_NR = 3;

    /**
     * This ImsService is registered to IMS via 3G.
     */
    public static final int REGISTRATION_TECH_3G = 4;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        prefix = {"REASON_"},
        value = {
            REASON_UNKNOWN,
            REASON_SIM_REMOVED,
            REASON_SIM_REFRESH,
            REASON_ALLOWED_NETWORK_TYPES_CHANGED,
            REASON_NON_IMS_CAPABLE_NETWORK,
            REASON_RADIO_POWER_OFF,
            REASON_HANDOVER_FAILED,
            REASON_VOPS_NOT_SUPPORTED,
        })
    public @interface ImsDeregistrationReason{}

    /**
     * Unspecified reason.
     * @hide
     */
    public static final int REASON_UNKNOWN = 0;

    /**
     * Since SIM is removed, the credentials for IMS service is also removed.
     * @hide
     */
    public static final int REASON_SIM_REMOVED = 1;

    /**
     * Detach from the network shall be performed due to the SIM refresh. IMS service should be
     * deregistered before that procedure.
     * @hide
     */
    public static final int REASON_SIM_REFRESH = 2;

    /**
     * The allowed network types have changed, resulting in a network type
     * that does not support IMS.
     * @hide
     */
    public static final int REASON_ALLOWED_NETWORK_TYPES_CHANGED = 3;

   /**
     * The device camped on a network that does not support IMS.
     * @hide
     */
    public static final int REASON_NON_IMS_CAPABLE_NETWORK = 4;

    /**
     * IMS service should be deregistered from the network before turning off the radio.
     * @hide
     */
    public static final int REASON_RADIO_POWER_OFF = 5;

    /**
     * Since the handover is failed or not allowed, the data service for IMS shall be
     * disconnected.
     * @hide
     */
    public static final int REASON_HANDOVER_FAILED = 6;

    /**
     * The network is changed to a network that does not support voice over IMS.
     * @hide
     */
    public static final int REASON_VOPS_NOT_SUPPORTED = 7;
}
