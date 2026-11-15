package android.net.wifi.aware;

/* AOSP android/net/wifi/aware/SubscribeDiscoverySession.java */
import android.annotation.NonNull;
import android.util.Log;

/**
 * A class representing a Aware subscribe session. Created when
 * {@link WifiAwareSession#subscribe(SubscribeConfig,
 * DiscoverySessionCallback, android.os.Handler)}
 * is called and a discovery session is created and returned in
 * {@link DiscoverySessionCallback#onSubscribeStarted(SubscribeDiscoverySession)}.
 * See baseline functionality of all discovery sessions in {@link DiscoverySession}.
 * This object allows updating an existing/running subscribe discovery session using
 * {@link #updateSubscribe(SubscribeConfig)}.
 */
public class SubscribeDiscoverySession extends DiscoverySession {
    private static final String TAG = "SubscribeDiscSession";

    
    /**
     * Re-configure the currently active subscribe session. The
     * {@link DiscoverySessionCallback} is not replaced - the same listener used
     * at creation is still used. The results of the configuration are returned using
     * {@link DiscoverySessionCallback}:
     * <ul>
     *     <li>{@link DiscoverySessionCallback#onSessionConfigUpdated()}: configuration
     *     update succeeded.
     *     <li>{@link DiscoverySessionCallback#onSessionConfigFailed()}: configuration
     *     update failed. The subscribe discovery session is still running using its previous
     *     configuration (i.e. update failure does not terminate the session).
     * </ul>
     *
     * @param subscribeConfig The new discovery subscribe session configuration
     *                        ({@link SubscribeConfig}).
     */
    public void updateSubscribe(@NonNull SubscribeConfig subscribeConfig) {
        if (mTerminated) {
            Log.w(TAG, "updateSubscribe: called on terminated session");
            return;
        } else {
            WifiAwareManager mgr = mMgr.get();
            if (mgr == null) {
                Log.w(TAG, "updateSubscribe: called post GC on WifiAwareManager");
                return;
            }

            mgr.updateSubscribe(mClientId, mSessionId, subscribeConfig);
        }
    }
}
