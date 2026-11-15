package android.net.wifi.rtt;

/* AOSP android/net/wifi/rtt/RangingResultCallback.java */
import android.annotation.IntDef;
import android.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Base class for ranging result callbacks. Should be extended by applications and set when calling
 * {@link WifiRttManager#startRanging(RangingRequest, java.util.concurrent.Executor, RangingResultCallback)}.
 * If the ranging operation fails in whole (not attempted) then {@link #onRangingFailure(int)}
 * will be called with a failure code. If the ranging operation is performed for each of the
 * requested peers then the {@link #onRangingResults(List)} will be called with the set of
 * results (@link {@link RangingResult}, each of which has its own success/failure code
 * {@link RangingResult#getStatus()}.
 */
public abstract class RangingResultCallback {
    
    /**
     * A failure code for the whole ranging request operation. Indicates a failure.
     */
    public static final int STATUS_CODE_FAIL = 1;

    /**
     * A failure code for the whole ranging request operation. Indicates that the request failed due
     * to RTT not being available - e.g. Wi-Fi was disabled. Use the
     * {@link WifiRttManager#isAvailable()} and {@link WifiRttManager#ACTION_WIFI_RTT_STATE_CHANGED}
     * to track RTT availability.
     */
    public static final int STATUS_CODE_FAIL_RTT_NOT_AVAILABLE = 2;

    /**
     * Called when a ranging operation failed in whole - i.e. no ranging operation to any of the
     * devices specified in the request was attempted.
     *
     * @param code A status code indicating the type of failure.
     */
    public abstract void onRangingFailure(@RangingOperationStatus int code);

    /**
     * Called when a ranging operation was executed. The list of results corresponds to devices
     * specified in the ranging request.
     *
     * @param results List of range measurements, one per requested device.
     */
    public abstract void onRangingResults(@NonNull List<RangingResult> results);
}
