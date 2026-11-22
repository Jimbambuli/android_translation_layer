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

import android.Manifest;
import android.annotation.RequiresFeature;
import android.annotation.RequiresPermission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Manages RCS User Capability Exchange for the subscription specified.
 *
 * @see ImsRcsManager#getUceAdapter() for information on creating an instance of this class.
 */
@RequiresFeature(PackageManager.FEATURE_TELEPHONY_IMS)
public class RcsUceAdapter {
    private static final String TAG = "RcsUceAdapter";

    private final Context mContext;
    private final int mSubId;

    /**
     * Not to be instantiated directly, use
     * {@link ImsRcsManager#getUceAdapter()} to instantiate this manager class.
     * @hide
     */
    RcsUceAdapter(Context context, int subId) {
        mContext = context;
        mSubId = subId;
    }

    /**
     * The setting for whether or not the user has opted in to the automatic refresh of the RCS
     * capabilities associated with the contacts in the user's contact address book. By default,
     * this setting is disabled and must be enabled after the user has seen the opt-in dialog shown
     * by {@link ImsRcsManager#ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN}.
     * <p>
     * If this feature is enabled, the device will periodically share the phone numbers of all of
     * the contacts in the user's address book with the carrier to refresh the RCS capabilities
     * cache associated with those contacts as the local cache becomes stale.
     * <p>
     * This setting will only enable this feature if
     * {@link android.telephony.CarrierConfigManager.Ims#KEY_RCS_BULK_CAPABILITY_EXCHANGE_BOOL} is
     * also enabled.
     * <p>
     * Note: This setting does not affect whether or not the device publishes its service
     * capabilities if the subscription supports presence publication.
     *
     * @return true if the user has opted in for automatic refresh of the RCS capabilities of their
     * contacts, false otherwise.
     * @throws ImsException if the subscription associated with this instance of
     * {@link RcsUceAdapter} is valid, but the ImsService associated with the subscription is not
     * available. This can happen if the ImsService has crashed, for example, or if the subscription
     * becomes inactive. See {@link ImsException#getCode()} for more information on the error codes.
     * @throws UnsupportedOperationException If the device does not have
     *          {@link PackageManager#FEATURE_TELEPHONY_IMS}.
     */
        @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public boolean isUceSettingEnabled() throws ImsException {
        // ATL: the IMS RCS controller service of the phone process does not exist (AOSP's null branch)
        Log.e(TAG, "isUceSettingEnabled: IImsRcsController is null");
        throw new ImsException("Can not find remote IMS service",
                ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
    }
}
