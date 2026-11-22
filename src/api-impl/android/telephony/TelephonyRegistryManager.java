/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.telephony;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.content.Context;
import android.telephony.satellite.SatelliteStateChangeListener;
import android.telephony.CarrierConfigManager.CarrierConfigChangeListener;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * ATL: the telephony registry of the process (Android's lives in system_server). It holds the
 * satellite state listeners of SatelliteManager; the satellite state never changes on a device
 * without satellite, notifySatelliteStateChanged() is the AOSP TelephonyRegistry's fan-out.
 *
 * @hide
 */
public class TelephonyRegistryManager {
    private static final Map<SatelliteStateChangeListener, Executor> sSatelliteStateChangeListeners =
            new ConcurrentHashMap<>();

    private static final Map<CarrierConfigChangeListener, Executor> sCarrierConfigChangeListeners =
            new ConcurrentHashMap<>();

    private final Context mContext;

    public TelephonyRegistryManager(@NonNull Context context) {
        mContext = context;
    }

    public void addSatelliteStateChangeListener(@NonNull @CallbackExecutor Executor executor,
            @NonNull SatelliteStateChangeListener listener) {
        if (listener == null || executor == null) {
            throw new IllegalArgumentException("Listener and executor must be non-null");
        }
        sSatelliteStateChangeListeners.putIfAbsent(listener, executor);
    }

    public void removeSatelliteStateChangeListener(@NonNull SatelliteStateChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must be non-null");
        }
        sSatelliteStateChangeListeners.remove(listener);
    }

    public void addCarrierConfigChangedListener(@NonNull Executor executor,
            @NonNull CarrierConfigChangeListener listener) {
        Objects.requireNonNull(executor, "Executor should be non-null.");
        Objects.requireNonNull(listener, "Listener should be non-null.");
        sCarrierConfigChangeListeners.putIfAbsent(listener, executor);
    }

    public void removeCarrierConfigChangedListener(@NonNull CarrierConfigChangeListener listener) {
        Objects.requireNonNull(listener, "Listener should be non-null.");
        sCarrierConfigChangeListeners.remove(listener);
    }

    public void notifyCarrierConfigChanged(int slotIndex, int subId, int carrierId, int specificCarrierId) {
        for (Map.Entry<CarrierConfigChangeListener, Executor> e : sCarrierConfigChangeListeners.entrySet()) {
            CarrierConfigChangeListener listener = e.getKey();
            e.getValue().execute(() -> listener.onCarrierConfigChanged(slotIndex, subId, carrierId,
                    specificCarrierId));
        }
    }

    public void notifySatelliteStateChanged(boolean isEnabled) {
        for (Map.Entry<SatelliteStateChangeListener, Executor> e : sSatelliteStateChangeListeners.entrySet()) {
            SatelliteStateChangeListener listener = e.getKey();
            e.getValue().execute(() -> listener.onEnabledStateChanged(isEnabled));
        }
    }
}
