package android.net.wifi.aware;

/* AOSP android/net/wifi/aware/PublishDiscoverySession.java */
import android.annotation.NonNull;
import android.util.Log;

/**
 * A class representing a Aware publish session. Created when
 * {@link WifiAwareSession#publish(PublishConfig, DiscoverySessionCallback,
 * android.os.Handler)} is called and a discovery session is created and returned in
 * {@link DiscoverySessionCallback#onPublishStarted(PublishDiscoverySession)}. See
 * baseline functionality of all discovery sessions in {@link DiscoverySession}. This
 * object allows updating an existing/running publish discovery session using
 * {@link #updatePublish(PublishConfig)}.
 */
public class PublishDiscoverySession extends DiscoverySession {
    private static final String TAG = "PublishDiscoverySession";

    
    /**
     * Re-configure the currently active publish session. The
     * {@link DiscoverySessionCallback} is not replaced - the same listener used
     * at creation is still used. The results of the configuration are returned using
     * {@link DiscoverySessionCallback}:
     * <ul>
     *     <li>{@link DiscoverySessionCallback#onSessionConfigUpdated()}: configuration
     *     update succeeded.
     *     <li>{@link DiscoverySessionCallback#onSessionConfigFailed()}: configuration
     *     update failed. The publish discovery session is still running using its previous
     *     configuration (i.e. update failure does not terminate the session).
     * </ul>
     *
     * @param publishConfig The new discovery publish session configuration ({@link PublishConfig}).
     */
    public void updatePublish(@NonNull PublishConfig publishConfig) {
        if (mTerminated) {
            Log.w(TAG, "updatePublish: called on terminated session");
            return;
        } else {
            WifiAwareManager mgr = mMgr.get();
            if (mgr == null) {
                Log.w(TAG, "updatePublish: called post GC on WifiAwareManager");
                return;
            }

            mgr.updatePublish(mClientId, mSessionId, publishConfig);
        }
    }
}
