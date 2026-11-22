/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
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

package android.bluetooth;

import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;
import static android.bluetooth.BluetoothProfile.CONNECTION_POLICY_ALLOWED;
import static android.bluetooth.BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

import static java.util.Objects.requireNonNull;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SystemApi;
import android.content.Context;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.CloseGuard;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * This class provides the public APIs to control the Bluetooth CSIP set coordinator.
 *
 * <p>BluetoothCsipSetCoordinator is a proxy object for controlling the Bluetooth CSIP set Service
 * via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get the BluetoothCsipSetCoordinator
 * proxy object.
 */
/*
 * ATL: this proxy has no binder service behind it (ATL has no Bluetooth service), so it is never
 * attached. Each method keeps the code path AOSP takes while getService() is null; the service branches,
 * the AIDL types and the binder callback plumbing are left out.
 */
public final class BluetoothCsipSetCoordinator implements BluetoothProfile, AutoCloseable {
    private static final String TAG = BluetoothCsipSetCoordinator.class.getSimpleName();

    private static final boolean DBG = Log.isLoggable("bluetooth", Log.DEBUG);
    private static final boolean VDBG = Log.isLoggable("bluetooth", Log.VERBOSE);

    private final CloseGuard mCloseGuard;

    /** @hide */
    @SystemApi
    public interface ClientLockCallback {
        /** @hide */
        @IntDef(
                value = {
                    BluetoothStatusCodes.SUCCESS,
                    BluetoothStatusCodes.ERROR_DEVICE_NOT_CONNECTED,
                    BluetoothStatusCodes.ERROR_CSIP_INVALID_GROUP_ID,
                    BluetoothStatusCodes.ERROR_CSIP_GROUP_LOCKED_BY_OTHER,
                    BluetoothStatusCodes.ERROR_CSIP_LOCKED_GROUP_MEMBER_LOST,
                    BluetoothStatusCodes.ERROR_UNKNOWN,
                })
        @Retention(RetentionPolicy.SOURCE)
        @interface Status {}

        /**
         * Callback is invoked as a result on {@link BluetoothCsipSetCoordinator#lockGroup()}.
         *
         * @param groupId group identifier
         * @param opStatus status of lock operation
         * @param isLocked indicates if group is locked
         * @hide
         */
        @SystemApi
        void onGroupLockSet(int groupId, @Status int opStatus, boolean isLocked);
    }

    /**
     * Intent used to broadcast the change in connection state of the CSIS Client.
     *
     * <p>This intent will have 3 extras:
     *
     * <ul>
     *   <li>{@link #EXTRA_STATE} - The current state of the profile.
     *   <li>{@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device.
     * </ul>
     *
     * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of {@link
     * #STATE_DISCONNECTED}, {@link #STATE_CONNECTING}, {@link #STATE_CONNECTED}, {@link
     * #STATE_DISCONNECTING}.
     */
    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CSIS_CONNECTION_STATE_CHANGED =
            "android.bluetooth.action.CSIS_CONNECTION_STATE_CHANGED";

    /**
     * Intent used to expose broadcast receiving device.
     *
     * <p>This intent will have 2 extras:
     *
     * <ul>
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote Broadcast receiver device.
     *   <li>{@link #EXTRA_CSIS_GROUP_ID} - Group identifier.
     *   <li>{@link #EXTRA_CSIS_GROUP_SIZE} - Group size.
     *   <li>{@link #EXTRA_CSIS_GROUP_TYPE_UUID} - Group type UUID.
     * </ul>
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(BLUETOOTH_PRIVILEGED)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CSIS_DEVICE_AVAILABLE =
            "android.bluetooth.action.CSIS_DEVICE_AVAILABLE";

    /**
     * Used as an extra field in {@link #ACTION_CSIS_DEVICE_AVAILABLE} intent. Contains the group
     * id.
     *
     * <p>Possible Values: {@link GROUP_ID_INVALID} Invalid group identifier 0x01 - 0xEF Valid group
     * identifier
     *
     * @hide
     */
    @SystemApi
    public static final String EXTRA_CSIS_GROUP_ID = "android.bluetooth.extra.CSIS_GROUP_ID";

    /**
     * Group size as int extra field in {@link #ACTION_CSIS_DEVICE_AVAILABLE} intent.
     *
     * @hide
     */
    public static final String EXTRA_CSIS_GROUP_SIZE = "android.bluetooth.extra.CSIS_GROUP_SIZE";

    /**
     * Group type uuid extra field in {@link #ACTION_CSIS_DEVICE_AVAILABLE} intent.
     *
     * @hide
     */
    public static final String EXTRA_CSIS_GROUP_TYPE_UUID =
            "android.bluetooth.extra.CSIS_GROUP_TYPE_UUID";

    /**
     * Intent used to broadcast information about identified set member ready to connect.
     *
     * <p>This intent will have one extra:
     *
     * <ul>
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device. It can be null if no device
     *       is active.
     *   <li>{@link #EXTRA_CSIS_GROUP_ID} - Group identifier.
     * </ul>
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(BLUETOOTH_PRIVILEGED)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CSIS_SET_MEMBER_AVAILABLE =
            "android.bluetooth.action.CSIS_SET_MEMBER_AVAILABLE";

    /**
     * This represents an invalid group ID.
     *
     * @hide
     */
    @SystemApi
    public static final int GROUP_ID_INVALID = -1;

    private final BluetoothAdapter mAdapter;
    /**
     * Create a BluetoothCsipSetCoordinator proxy object for interacting with the local Bluetooth
     * CSIS service.
     */
    /*package*/ BluetoothCsipSetCoordinator(Context context, BluetoothAdapter adapter) {
        mAdapter = adapter;
        mCloseGuard = new CloseGuard();
        mCloseGuard.open("close");
    }

    /** @hide */
    @SuppressWarnings("Finalize") // TODO(b/314811467)
    protected void finalize() {
        if (mCloseGuard != null) {
            mCloseGuard.warnIfOpen();
        }
        close();
    }

    /** @hide */
    @Override
    public void close() {
        mAdapter.closeProfileProxy(this);
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceConnected(IBinder service) {
        // no service binder in ATL
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceDisconnected() {
        // no service binder in ATL
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Lock the set.
     *
     * @param groupId group ID to lock,
     * @param executor callback executor,
     * @param callback callback to report lock and unlock events - stays valid until the app unlocks
     *     using the returned lock identifier or the lock timeouts on the remote side, as per CSIS
     *     specification,
     * @return unique lock identifier used for unlocking or null if lock has failed.
     * @throws IllegalArgumentException when executor or callback is null
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {"android.permission.BLUETOOTH_CONNECT", BLUETOOTH_PRIVILEGED})
    public @Nullable UUID lockGroup(
            int groupId,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull ClientLockCallback callback) {
        if (VDBG) log("lockGroup()");
        requireNonNull(executor);
        requireNonNull(callback);
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) log(Log.getStackTraceString(new Throwable()));

        return null;
    }

    /**
     * Unlock the set.
     *
     * @param lockUuid unique lock identifier
     * @return true if unlocked, false on error
     * @throws IllegalArgumentException when lockUuid is null
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {"android.permission.BLUETOOTH_CONNECT", BLUETOOTH_PRIVILEGED})
    public boolean unlockGroup(@NonNull UUID lockUuid) {
        if (VDBG) log("unlockGroup()");
        requireNonNull(lockUuid);
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) log(Log.getStackTraceString(new Throwable()));

        return false;
    }

    /**
     * Get device's groups.
     *
     * @param device the active device
     * @return Map of groups ids and related UUIDs
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {"android.permission.BLUETOOTH_CONNECT", BLUETOOTH_PRIVILEGED})
    @NonNull
    public Map<Integer, ParcelUuid> getGroupUuidMapByDevice(@Nullable BluetoothDevice device) {
        if (VDBG) log("getGroupUuidMapByDevice()");
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) log(Log.getStackTraceString(new Throwable()));

        return new HashMap<>();
    }

    /**
     * Get group id for the given UUID
     *
     * @return list of group IDs
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {"android.permission.BLUETOOTH_CONNECT", BLUETOOTH_PRIVILEGED})
    public @NonNull List<Integer> getAllGroupIds(@Nullable ParcelUuid uuid) {
        if (VDBG) log("getAllGroupIds()");
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) log(Log.getStackTraceString(new Throwable()));

        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    @RequiresPermission(allOf = {"android.permission.BLUETOOTH_CONNECT", BLUETOOTH_PRIVILEGED})
    public @NonNull List<BluetoothDevice> getConnectedDevices() {
        if (VDBG) log("getConnectedDevices()");
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) log(Log.getStackTraceString(new Throwable()));

        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    @RequiresPermission(allOf = {"android.permission.BLUETOOTH_CONNECT", BLUETOOTH_PRIVILEGED})
    @NonNull
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(@NonNull int[] states) {
        if (VDBG) log("getDevicesMatchingStates(states=" + Arrays.toString(states) + ")");
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) log(Log.getStackTraceString(new Throwable()));

        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    @RequiresPermission(allOf = {"android.permission.BLUETOOTH_CONNECT", BLUETOOTH_PRIVILEGED})
    @BluetoothProfile.BtProfileState
    public int getConnectionState(@Nullable BluetoothDevice device) {
        if (VDBG) log("getState(" + device + ")");
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) log(Log.getStackTraceString(new Throwable()));

        return STATE_DISCONNECTED;
    }

    /**
     * Set connection policy of the profile
     *
     * <p>The device should already be paired. Connection policy can be one of {@link
     * #CONNECTION_POLICY_ALLOWED}, {@link #CONNECTION_POLICY_FORBIDDEN}, {@link
     * #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {"android.permission.BLUETOOTH_CONNECT", BLUETOOTH_PRIVILEGED})
    public boolean setConnectionPolicy(
            @Nullable BluetoothDevice device, @ConnectionPolicy int connectionPolicy) {
        if (DBG) log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) log(Log.getStackTraceString(new Throwable()));

        return false;
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p>The connection policy can be any of: {@link #CONNECTION_POLICY_ALLOWED}, {@link
     * #CONNECTION_POLICY_FORBIDDEN}, {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {"android.permission.BLUETOOTH_CONNECT", BLUETOOTH_PRIVILEGED})
    public @ConnectionPolicy int getConnectionPolicy(@Nullable BluetoothDevice device) {
        if (VDBG) log("getConnectionPolicy(" + device + ")");
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) log(Log.getStackTraceString(new Throwable()));

        return CONNECTION_POLICY_FORBIDDEN;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
