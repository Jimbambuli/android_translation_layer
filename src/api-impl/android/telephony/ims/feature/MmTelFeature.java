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
 * limitations under the License
 */

package android.telephony.ims.feature;


import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Base implementation for Voice and SMS (IR-92) and Video (IR-94) IMS support.
 *
 * Any class wishing to use MmTelFeature should extend this class and implement all methods that the
 * service supports.
 */
public class MmTelFeature extends ImsFeature {
    private Executor mExecutor;
    /**
     * Creates a new MmTelFeature using the Executor set in {@link ImsService#getExecutor}
     * @hide
     */
    @SystemApi
    public MmTelFeature() {
    }

    /**
     * Create a new MmTelFeature using the Executor specified for methods being called by the
     * framework.
     * @param executor The executor for the framework to use when executing the methods overridden
     * by the implementation of MmTelFeature.
     * @hide
     */
    @SystemApi
    public MmTelFeature(@NonNull Executor executor) {
        super();
        mExecutor = executor;
    }

    /**
     * Contains the capabilities defined and supported by a MmTelFeature in the form of a Bitmask.
     * The capabilities that are used in MmTelFeature are defined as
     * {@link MmTelCapabilities#CAPABILITY_TYPE_VOICE},
     * {@link MmTelCapabilities#CAPABILITY_TYPE_VIDEO},
     * {@link MmTelCapabilities#CAPABILITY_TYPE_UT},
     * {@link MmTelCapabilities#CAPABILITY_TYPE_SMS}, and
     * {@link MmTelCapabilities#CAPABILITY_TYPE_CALL_COMPOSER}.
     *
     * The capabilities of this MmTelFeature will be set by the framework.
     */
    public static class MmTelCapabilities extends Capabilities {

        /** @hide */
        @IntDef(flag = true,
                value = {
                        CAPABILITY_TYPE_VOICE,
                        CAPABILITY_TYPE_VIDEO,
                        CAPABILITY_TYPE_UT,
                        CAPABILITY_TYPE_SMS,
                        CAPABILITY_TYPE_CALL_COMPOSER,
                        CAPABILITY_TYPE_CALL_COMPOSER_BUSINESS_ONLY
                })
        @Retention(RetentionPolicy.SOURCE)
        public @interface MmTelCapability {}

        /**
         * This MmTelFeature supports Voice calling (IR.92)
         */
        public static final int CAPABILITY_TYPE_VOICE = 1 << 0;

        /**
         * This MmTelFeature supports Video (IR.94)
         */
        public static final int CAPABILITY_TYPE_VIDEO = 1 << 1;

        /**
         * This MmTelFeature supports XCAP over Ut for supplementary services. (IR.92)
         */
        public static final int CAPABILITY_TYPE_UT = 1 << 2;

        /**
         * This MmTelFeature supports SMS (IR.92)
         */
        public static final int CAPABILITY_TYPE_SMS = 1 << 3;

        /**
         * This MmTelFeature supports Call Composer (section 2.4 of RC.20). This is the superset
         * Call Composer, meaning that all subset types of Call Composers must be enabled when this
         * capability is enabled
         */
        public static final int CAPABILITY_TYPE_CALL_COMPOSER = 1 << 4;


        /**
         * This MmTelFeature supports Business-only Call Composer. This is a subset of
         * {@code CAPABILITY_TYPE_CALL_COMPOSER} that only supports business related
         * information for calling (e.g. information to signal if the call is a business call) in
         * the SIP header.  When enabling {@code CAPABILITY_TYPE_CALL_COMPOSER}, the
         * {@code CAPABILITY_TYPE_CALL_COMPOSER_BUSINESS_ONLY} capability must also be enabled.
         */
        public static final int CAPABILITY_TYPE_CALL_COMPOSER_BUSINESS_ONLY = 1 << 5;

        /**
         * @param capabilities a bitmask of one or more capabilities.
         *
         * @return true if all queried capabilities are true, otherwise false.
         */
        @Override
        public final boolean isCapable(@MmTelCapability int capabilities) {
            return super.isCapable(capabilities);
        }

        /**
         * Create a new empty {@link MmTelCapabilities} instance.
         * @see #addCapabilities(int)
         * @see #removeCapabilities(int)
         * @hide
         */
        @SystemApi
        public MmTelCapabilities() {
            super();
        }

        /**@deprecated Use {@link MmTelCapabilities} to construct a new instance instead.
         * @hide
         */
        @Deprecated
        @SystemApi
        public MmTelCapabilities(Capabilities c) {
            mCapabilities = c.mCapabilities;
        }

        /**
         * Create a new {link @MmTelCapabilities} instance with the provided capabilities.
         * @param capabilities The capabilities that are supported for MmTel in the form of a
         *                     bitfield.
         * @hide
         */
        @SystemApi
        public MmTelCapabilities(@MmTelCapability int capabilities) {
            super(capabilities);
        }
}

    /**
     * To be returned by {@link #shouldProcessCall(String[])} when the ImsService should process the
     * outgoing call as IMS.
     * @hide
     */
    @SystemApi
    public static final int PROCESS_CALL_IMS = 0;
    /**
     * To be returned by {@link #shouldProcessCall(String[])} when the telephony framework should
     * not process the outgoing call as IMS and should instead use circuit switch.
     * @hide
     */
    @SystemApi
    public static final int PROCESS_CALL_CSFB = 1;

    /** @hide */
    @IntDef(flag = true,
            value = {
                    PROCESS_CALL_IMS,
                    PROCESS_CALL_CSFB
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProcessCallResult {}

    /** @hide */
    @IntDef(
        prefix = "AUDIO_HANDLER_",
        value = {
            AUDIO_HANDLER_ANDROID,
            AUDIO_HANDLER_BASEBAND
        })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsAudioHandler {}

    /**
    * Audio Handler - Android
    * @hide
    */
    @SystemApi
    public static final int AUDIO_HANDLER_ANDROID = 0;

    /**
    * Audio Handler - Baseband
    * @hide
    */
    @SystemApi
    public static final int AUDIO_HANDLER_BASEBAND = 1;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        prefix = "EPS_FALLBACK_REASON_",
        value = {
            EPS_FALLBACK_REASON_INVALID,
            EPS_FALLBACK_REASON_NO_NETWORK_TRIGGER,
            EPS_FALLBACK_REASON_NO_NETWORK_RESPONSE,
        })
    public @interface EpsFallbackReason {}

    /**
     * Default value. Internal use only.
     * This value should not be used to trigger EPS fallback.
     * @hide
     */
    public static final int EPS_FALLBACK_REASON_INVALID = -1;

    /**
     * If the network only supports the EPS fallback in 5G NR SA for voice calling and the EPS
     * Fallback procedure by the network during the call setup is not triggered, UE initiated
     * fallback will be triggered with this reason. The modem shall locally release the 5G NR
     * SA RRC connection and acquire the LTE network and perform a tracking area update
     * procedure. After the EPS fallback procedure is completed, the call setup for voice will
     * be established if there is no problem.
     *
     * @hide
     */
    @SystemApi
    public static final int EPS_FALLBACK_REASON_NO_NETWORK_TRIGGER = 1;

    /**
     * If the UE doesn't receive any response for SIP INVITE within a certain timeout in 5G NR
     * SA for MO voice calling, the device determines that voice call is not available in 5G and
     * terminates all active SIP dialogs and SIP requests and enters IMS non-registered state.
     * In that case, UE initiated fallback will be triggered with this reason. The modem shall
     * reset modem's data buffer of IMS PDU to prevent the ghost call. After the EPS fallback
     * procedure is completed, VoLTE call could be tried if there is no problem.
     *
     * @hide
     */
    @SystemApi
    public static final int EPS_FALLBACK_REASON_NO_NETWORK_RESPONSE = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        prefix = "IMS_TRAFFIC_TYPE_",
        value = {
            IMS_TRAFFIC_TYPE_NONE,
            IMS_TRAFFIC_TYPE_EMERGENCY,
            IMS_TRAFFIC_TYPE_EMERGENCY_SMS,
            IMS_TRAFFIC_TYPE_VOICE,
            IMS_TRAFFIC_TYPE_VIDEO,
            IMS_TRAFFIC_TYPE_SMS,
            IMS_TRAFFIC_TYPE_REGISTRATION,
            IMS_TRAFFIC_TYPE_UT_XCAP
        })
    public @interface ImsTrafficType {}

    /**
     * Default value for initialization. Internal use only.
     * @hide
     */
    public static final int IMS_TRAFFIC_TYPE_NONE = -1;
    /**
     * Emergency call
     * @hide
     */
    @SystemApi
    public static final int IMS_TRAFFIC_TYPE_EMERGENCY = 0;
    /**
     * Emergency SMS
     * @hide
     */
    @SystemApi
    public static final int IMS_TRAFFIC_TYPE_EMERGENCY_SMS = 1;
    /**
     * Voice call
     * @hide
     */
    @SystemApi
    public static final int IMS_TRAFFIC_TYPE_VOICE = 2;
    /**
     * Video call
     * @hide
     */
    @SystemApi
    public static final int IMS_TRAFFIC_TYPE_VIDEO = 3;
    /**
     * SMS over IMS
     * @hide
     */
    @SystemApi
    public static final int IMS_TRAFFIC_TYPE_SMS = 4;
    /**
     * IMS registration and subscription for reg event package (signaling)
     * @hide
     */
    @SystemApi
    public static final int IMS_TRAFFIC_TYPE_REGISTRATION = 5;
    /**
     * Ut/XCAP (XML Configuration Access Protocol)
     * @hide
     */
    @SystemApi
    public static final int IMS_TRAFFIC_TYPE_UT_XCAP = 6;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = { "IMS_TRAFFIC_DIRECTION_" },
            value = {IMS_TRAFFIC_DIRECTION_INCOMING, IMS_TRAFFIC_DIRECTION_OUTGOING})
    public @interface ImsTrafficDirection {}

    /**
     * Indicates that the traffic is an incoming traffic.
     * @hide
     */
    @SystemApi
    public static final int IMS_TRAFFIC_DIRECTION_INCOMING = 0;
    /**
     * Indicates that the traffic is an outgoing traffic.
     * @hide
     */
    @SystemApi
    public static final int IMS_TRAFFIC_DIRECTION_OUTGOING = 1;

    /**
     * Called by the framework to determine if the outgoing call, designated by the outgoing
     * {@link String}s, should be processed as an IMS call or CSFB call. If this method's
     * functionality is not overridden, the platform will process every call as IMS as long as the
     * MmTelFeature reports that the {@link MmTelCapabilities#CAPABILITY_TYPE_VOICE} capability is
     * available.
     * @param numbers An array of {@link String}s that will be used for placing the call. There can
     *         be multiple {@link String}s listed in the case when we want to place an outgoing
     *         call as a conference.
     * @return a {@link ProcessCallResult} to the framework, which will be used to determine if the
     *        call will be placed over IMS or via CSFB.
     * @hide
     */
    @SystemApi
    public @ProcessCallResult int shouldProcessCall(@NonNull String[] numbers) {
        return PROCESS_CALL_IMS;
    }
}
