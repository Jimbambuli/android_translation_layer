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
package com.android.internal.telephony;

import android.annotation.NonNull;
import android.content.Context;
import android.os.Binder;
import android.os.Process;
import android.os.UserHandle;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/** Utility class for Telephony permission enforcement. */
/* ATL: only the methods the telephony providers call (isSystemOrPhone, checkSubscriptionAssociatedWithUser);
 * the rest of AOSP's TelephonyPermissions needs LegacyPermissionManager, carrier privileges and statsd, which ATL does not have */
public final class TelephonyPermissions {
    private static final String LOG_TAG = "TelephonyPermissions";

    /**
     * Check if calling user is associated with the given subscription.
     * Subscription-user association check is skipped if destination address is an emergency number.
     *
     * @param context Context
     * @param subId subscription ID
     * @param callerUserHandle caller user handle
     * @param destAddr destination address of the message
     * @return  true if destAddr is an emergency number
     * and return false if user is not associated with the subscription.
     */
    public static boolean checkSubscriptionAssociatedWithUser(@NonNull Context context, int subId,
            @NonNull UserHandle callerUserHandle, @NonNull String destAddr) {
        // Skip subscription-user association check for emergency numbers
        TelephonyManager tm = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        final long token = Binder.clearCallingIdentity();
        try {
            if (tm != null && tm.isEmergencyNumber(destAddr)) {
                Log.d(LOG_TAG, "checkSubscriptionAssociatedWithUser:"
                        + " destAddr is emergency number");
                return true;
            }
        } catch(Exception e) {
            Log.e(LOG_TAG, "Cannot verify if destAddr is an emergency number: " + e);
        } finally {
            Binder.restoreCallingIdentity(token);
        }

        return checkSubscriptionAssociatedWithUser(context, subId, callerUserHandle);
    }

    /**
     * Check if calling user is associated with the given subscription.
     * @param context Context
     * @param subId subscription ID
     * @param callerUserHandle caller user handle
     * @return  false if user is not associated with the subscription, or no record found of this
     * subscription.
     */
    public static boolean checkSubscriptionAssociatedWithUser(@NonNull Context context, int subId,
            @NonNull UserHandle callerUserHandle) {
        SubscriptionManager subManager = (SubscriptionManager) context.getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        final long token = Binder.clearCallingIdentity();
        try {
            if ((subManager != null) &&
                    (!subManager.isSubscriptionAssociatedWithUser(subId, callerUserHandle))) {
                // If subId is not associated with calling user, return false.
                Log.e(LOG_TAG, "User[User ID:" + callerUserHandle.getIdentifier()
                        + "] is not associated with Subscription ID:" + subId);
                return false;
            }
        } catch (IllegalArgumentException e) {
            // Found no record of this sub Id.
            Log.e(LOG_TAG, "Subscription[Subscription ID:" + subId + "] has no records on device");
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
        return true;
    }

    /**
     * @return true if the specified {@code uid} is for a system or phone process, no matter if runs
     * as system user or not.
     */
    public static boolean isSystemOrPhone(int uid) {
        return UserHandle.isSameApp(uid, Process.SYSTEM_UID) || UserHandle.isSameApp(uid,
                Process.PHONE_UID);
    }
}
