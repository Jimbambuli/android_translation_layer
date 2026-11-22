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
 * limitations under the License.
 */

package android.telephony.ims.feature;

import android.annotation.IntDef;
import android.annotation.SystemApi;
import android.annotation.TestApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all IMS features that are supported by the framework. ATL: only the constants and the
 * Capabilities holder of AOSP's ImsFeature, which are what MmTelFeature and RcsFeature's public API
 * is built on; the IMS service binding of the system (IImsFeature) does not exist in ATL.
 *
 * @hide
 */
@SystemApi
public abstract class ImsFeature {

    /**
     * Invalid feature value
     * @hide
     */
    public static final int FEATURE_INVALID = -1;

    // ImsFeatures that are defined in the Manifests. Ensure that these values match the previously
    // defined values in ImsServiceClass for compatibility purposes.
    /**
     * This feature supports emergency calling over MMTEL. If defined, the framework will try to
     * place an emergency call over IMS first. If it is not defined, the framework will only use
     * CSFB for emergency calling.
     * @hide
     */
    @SystemApi
    public static final int FEATURE_EMERGENCY_MMTEL = 0;

    /**
     * This feature supports the MMTEL feature.
     * @hide
     */
    @SystemApi
    public static final int FEATURE_MMTEL = 1;

    /**
     * This feature supports the RCS feature.
     * @hide
     */
    @SystemApi
    public static final int FEATURE_RCS = 2;

    /**
     * Total number of features defined
     * @hide
     */
    public static final int FEATURE_MAX = 3;

    /**
     * Used for logging purposes.
     * @hide
     */
    public static final Map<Integer, String> FEATURE_LOG_MAP = Map.of(
            FEATURE_EMERGENCY_MMTEL, "EMERGENCY_MMTEL",
            FEATURE_MMTEL, "MMTEL",
            FEATURE_RCS, "RCS");

    /**
     * Integer values defining IMS features that are supported in ImsFeature.
     * @hide
     */
    @IntDef(flag = true,
            value = {
                    FEATURE_EMERGENCY_MMTEL,
                    FEATURE_MMTEL,
                    FEATURE_RCS
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FeatureType {}

    /**
     * Integer values defining the state of the ImsFeature at any time.
     * @hide
     */
    @IntDef(flag = true,
            value = {
                    STATE_UNAVAILABLE,
                    STATE_INITIALIZING,
                    STATE_READY,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsState {}

    /**
     * This {@link ImsFeature}'s state is unavailable and should not be communicated with. This will
     * remove all bindings back to the framework. Any attempt to communicate with the framework
     * during this time will result in an {@link IllegalStateException}.
     * @hide
     */
    @SystemApi
    public static final int STATE_UNAVAILABLE = 0;

    /**
     * This {@link ImsFeature} state is initializing and should not be communicated with. This will
     * remove all bindings back to the framework. Any attempt to communicate with the framework
     * during this time will result in an {@link IllegalStateException}.
     * @hide
     */
    @SystemApi
    public static final int STATE_INITIALIZING = 1;

    /**
     * This {@link ImsFeature} is ready for communication. Do not attempt to call framework methods
     * until {@see #onFeatureReady()} is called.
     * @hide
     */
    @SystemApi
    public static final int STATE_READY = 2;

    /**
     * Used for logging purposes.
     * @hide
     */
    public static final Map<Integer, String> STATE_LOG_MAP = Map.of(
            STATE_UNAVAILABLE, "UNAVAILABLE",
            STATE_INITIALIZING, "INITIALIZING",
            STATE_READY, "READY");

    /**
     * Integer values defining the result codes that should be returned from
     * {@link #changeEnabledCapabilities} when the framework tries to set a feature's capability.
     * @hide
     */
    @IntDef(flag = true,
            value = {
                    CAPABILITY_ERROR_GENERIC,
                    CAPABILITY_SUCCESS
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsCapabilityError {}

    /**
     * The capability was unable to be changed.
     * @hide
     */
    @SystemApi
    public static final int CAPABILITY_ERROR_GENERIC = -1;

    /**
     * The capability was able to be changed.
     * @hide
     */
    @SystemApi
    public static final int CAPABILITY_SUCCESS = 0;

    /**
     * Contains the IMS capabilities defined and supported by an ImsFeature in the form of a
     * bit-mask.
     *
     * @deprecated This class is not used directly, but rather extended in subclasses of
     * {@link ImsFeature} to provide service specific capabilities.
     * @see MmTelFeature.MmTelCapabilities
     * @hide
     */
    // Not Actually deprecated, but we need to remove it from the @SystemApi surface.
    @Deprecated
    @SystemApi // SystemApi only because it was leaked through type usage in a previous release.
    @TestApi
    public static class Capabilities {
        /** @deprecated Use getters and accessors instead. */
        // Not actually deprecated, but we need to remove it from the @SystemApi surface eventually.
        protected int mCapabilities = 0;

        /**
         * @hide
         */
        public Capabilities() {
        }

        /**
         * @hide
         */
        protected Capabilities(int capabilities) {
            mCapabilities = capabilities;
        }

        /**
         * @param capabilities Capabilities to be added to the configuration in the form of a
         *     bit mask.
         * @hide
         */
        public void addCapabilities(int capabilities) {
            mCapabilities |= capabilities;
        }

        /**
         * @param capabilities Capabilities to be removed to the configuration in the form of a
         *     bit mask.
         * @hide
         */
        public void removeCapabilities(int capabilities) {
            mCapabilities &= ~capabilities;
        }

        /**
         * @return true if all of the capabilities specified are capable.
         * @hide
         */
        public boolean isCapable(int capabilities) {
            return (mCapabilities & capabilities) == capabilities;
        }

        /**
         * @return a deep copy of the Capabilites.
         * @hide
         */
        public Capabilities copy() {
            return new Capabilities(mCapabilities);
        }

        /**
         * @return a bitmask containing the capability flags directly.
         * @hide
         */
        public int getMask() {
            return mCapabilities;
        }

        /**
         * @hide
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Capabilities)) return false;

            Capabilities that = (Capabilities) o;

            return mCapabilities == that.mCapabilities;
        }

        /**
         * @hide
         */
        @Override
        public int hashCode() {
            return mCapabilities;
        }

        /**
         * @hide
         */
        @Override
        public String toString() {
            return "Capabilities: " + Integer.toBinaryString(mCapabilities);
        }
    }
}
