package android.net.wifi.aware;

/* AOSP android/net/wifi/aware/DiscoverySession.java */
import static android.net.wifi.aware.Characteristics.WIFI_AWARE_CIPHER_SUITE_NCS_PK_PASN_128;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.NetworkSpecifier;
import android.os.Build;
import android.util.CloseGuard;
import android.util.Log;


import com.android.internal.annotations.VisibleForTesting;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * A class representing a single publish or subscribe Aware session. This object
 * will not be created directly - only its child classes are available:
 * {@link PublishDiscoverySession} and {@link SubscribeDiscoverySession}. This
 * class provides functionality common to both publish and subscribe discovery sessions:
 * <ul>
 *      <li>Sending messages: {@link #sendMessage(PeerHandle, int, byte[])} method.
 *      <li>Creating a network-specifier when requesting a Aware connection using
 *      {@link WifiAwareNetworkSpecifier.Builder}.
 * </ul>
 * <p>
 * The {@link #close()} method must be called to destroy discovery sessions once they are
 * no longer needed.
 */
public class DiscoverySession implements AutoCloseable {
    private static final String TAG = "DiscoverySession";
    private static final boolean DBG = false;
    private static final boolean VDBG = false; // STOPSHIP if true

    private static final int MAX_SEND_RETRY_COUNT = 5;

                
    private final CloseGuard mCloseGuard = new CloseGuard();

    
    
    /**
     * Destroy the publish or subscribe session - free any resources, and stop
     * transmitting packets on-air (for an active session) or listening for
     * matches (for a passive session). The session may not be used for any
     * additional operations after its destruction.
     * <p>
     *     This operation must be done on a session which is no longer needed. Otherwise system
     *     resources will continue to be utilized until the application exits. The only
     *     exception is a session for which we received a termination callback,
     *     {@link DiscoverySessionCallback#onSessionTerminated()}.
     */
    @Override
    public void close() {
        WifiAwareManager mgr = mMgr.get();
        if (mgr == null) {
            Log.w(TAG, "destroy: called post GC on WifiAwareManager");
            return;
        }
        mgr.terminateSession(mClientId, mSessionId);
        mTerminated = true;
        mMgr.clear();
        mCloseGuard.close();
        Reference.reachabilityFence(this);
    }

    
    
    
    
    
    /**
     * Sends a message to the specified destination. Aware messages are transmitted in the context
     * of a discovery session - executed subsequent to a publish/subscribe
     * {@link DiscoverySessionCallback#onServiceDiscovered(PeerHandle,
     * byte[], java.util.List)} event.
     * <p>
     *     Aware messages are not guaranteed delivery. Callbacks on
     *     {@link DiscoverySessionCallback} indicate message was transmitted successfully,
     *     {@link DiscoverySessionCallback#onMessageSendSucceeded(int)}, or transmission
     *     failed (possibly after several retries) -
     *     {@link DiscoverySessionCallback#onMessageSendFailed(int)}.
     * <p>
     * The peer will get a callback indicating a message was received using
     * {@link DiscoverySessionCallback#onMessageReceived(PeerHandle,
     * byte[])}.
     *
     * @param peerHandle The peer's handle for the message. Must be a result of an
     * {@link DiscoverySessionCallback#onServiceDiscovered(PeerHandle,
     * byte[], java.util.List)} or
     * {@link DiscoverySessionCallback#onMessageReceived(PeerHandle,
     * byte[])} events.
     * @param messageId An arbitrary integer used by the caller to identify the message. The same
     *            integer ID will be returned in the callbacks indicating message send success or
     *            failure. The {@code messageId} is not used internally by the Aware service - it
     *                  can be arbitrary and non-unique.
     * @param message The message to be transmitted.
     */
    public void sendMessage(@NonNull PeerHandle peerHandle, int messageId,
            @Nullable byte[] message) {
        sendMessage(peerHandle, messageId, message, 0);
    }

    /**
     * Initiate a Wi-Fi Aware Pairing setup request to create a pairing with the target peer.
     * The Aware pairing request should be done in the context of a discovery session -
     * after a publish/subscribe
     * {@link DiscoverySessionCallback#onServiceDiscovered(ServiceDiscoveryInfo)} event is received.
     * The peer will get a callback indicating a message was received using
     * {@link DiscoverySessionCallback#onPairingSetupRequestReceived(PeerHandle, int)}.
     * When the Aware Pairing setup is finished, both sides will receive
     * {@link DiscoverySessionCallback#onPairingSetupSucceeded(PeerHandle, String)}
     *
     * @param peerHandle      The peer's handle for the pairing request. Must be a result of a
     *                        {@link
     *                        DiscoverySessionCallback#onServiceDiscovered(ServiceDiscoveryInfo)}
     *                        or
     *                        {@link DiscoverySessionCallback#onMessageReceived(PeerHandle, byte[])}
     *                        events.
     * @param peerDeviceAlias The alias of paired device set by caller, will help caller to identify
     *                        the paired device.
     * @param cipherSuite     The cipher suite to be used to encrypt the link.
     * @param password        The password used for the pairing setup. If set to empty or null,
     *                        opportunistic pairing will be used.
     */
    public void initiatePairingRequest(@NonNull PeerHandle peerHandle,
            @NonNull String peerDeviceAlias,
            int cipherSuite,
            @Nullable String password) {
        if (!true) {
            throw new UnsupportedOperationException();
        }
        WifiAwareManager mgr = mMgr.get();
        if (mgr == null) {
            Log.w(TAG, "initiatePairingRequest: called post GC on WifiAwareManager");
            return;
        }
        mgr.initiateNanPairingSetupRequest(mClientId, mSessionId, peerHandle, password,
                peerDeviceAlias, cipherSuite);
    }

    /**
     * Accept and respond to a Wi-Fi Aware Pairing setup request received from peer. This is the
     * response to the
     * {@link DiscoverySessionCallback#onPairingSetupRequestReceived(PeerHandle, int)}
     * When the Aware Pairing setup is finished, both sides will receive
     * {@link DiscoverySessionCallback#onPairingSetupSucceeded(PeerHandle, String)}
     *
     * @param requestId       Id to identify the received pairing session, obtained by
     *                        {@link
     *                        DiscoverySessionCallback#onPairingSetupRequestReceived(PeerHandle,
     *                        int)}
     * @param peerHandle      The peer's handle for the pairing request. Must be a result of a
     *                        {@link
     *                        DiscoverySessionCallback#onServiceDiscovered(ServiceDiscoveryInfo)}
     *                        or
     *                        {@link DiscoverySessionCallback#onMessageReceived(PeerHandle, byte[])}
     *                        events.
     * @param peerDeviceAlias The alias of paired device set by caller, will help caller to identify
     *                        the paired device.
     * @param cipherSuite     The cipher suite to be used to encrypt the link.
     * @param password        The password is used for the pairing setup. If set to empty or null,
     *                        opportunistic pairing will be used.
     */
    public void acceptPairingRequest(int requestId, @NonNull PeerHandle peerHandle,
            @NonNull String peerDeviceAlias,
            int cipherSuite,
            @Nullable String password) {
        if (!true) {
            throw new UnsupportedOperationException();
        }
        WifiAwareManager mgr = mMgr.get();
        if (mgr == null) {
            Log.w(TAG, "initiatePairingRequest: called post GC on WifiAwareManager");
            return;
        }
        mgr.responseNanPairingSetupRequest(mClientId, mSessionId, peerHandle, requestId, password,
                peerDeviceAlias, true, cipherSuite);
    }

    /**
     * Reject a Wi-Fi Aware Pairing setup request received from peer. This is the
     * response to the
     * {@link DiscoverySessionCallback#onPairingSetupRequestReceived(PeerHandle, int)}
     *
     * @param requestId       Id to identify the received pairing session, get by
     *                        {@link
     *                        DiscoverySessionCallback#onPairingSetupRequestReceived(PeerHandle,
     *                        int)}
     * @param peerHandle      The peer's handle for the pairing request. Must be a result of a
     *                        {@link
     *                        DiscoverySessionCallback#onServiceDiscovered(ServiceDiscoveryInfo)}
     *                        or
     *                        {@link DiscoverySessionCallback#onMessageReceived(PeerHandle, byte[])}
     *                        events.
     */
    public void rejectPairingRequest(int requestId, @NonNull PeerHandle peerHandle) {
        if (!true) {
            throw new UnsupportedOperationException();
        }
        WifiAwareManager mgr = mMgr.get();
        if (mgr == null) {
            Log.w(TAG, "initiatePairingRequest: called post GC on WifiAwareManager");
            return;
        }
        mgr.responseNanPairingSetupRequest(mClientId, mSessionId, peerHandle, requestId, null,
                null, false, WIFI_AWARE_CIPHER_SUITE_NCS_PK_PASN_128);
    }

    /**
     * Initiate a Wi-Fi Aware bootstrapping setup request to create a pairing with the target peer.
     * The Aware bootstrapping request should be done in the context of a discovery session -
     * after a publish/subscribe
     * {@link DiscoverySessionCallback#onServiceDiscovered(ServiceDiscoveryInfo)} event is received.
     * The peer will check if the method can be fulfilled by
     * {@link AwarePairingConfig.Builder#setBootstrappingMethods(int)}
     * When the Aware Bootstrapping setup finished, both side will receive
     * {@link DiscoverySessionCallback#onBootstrappingSucceeded(PeerHandle, int)}
     * @param peerHandle The peer's handle for the pairing request. Must be a result of an
     * {@link DiscoverySessionCallback#onServiceDiscovered(ServiceDiscoveryInfo)} or
     * {@link DiscoverySessionCallback#onMessageReceived(PeerHandle, byte[])} events.
     * @param method one of the AwarePairingConfig#PAIRING_BOOTSTRAPPING_ values, should match one
     *               of the methods received from {@link ServiceDiscoveryInfo#getPairingConfig()}
     *               {@link AwarePairingConfig#getBootstrappingMethods()}
     */
    public void initiateBootstrappingRequest(@NonNull PeerHandle peerHandle,
            int method) {
        if (!true) {
            throw new UnsupportedOperationException();
        }
        WifiAwareManager mgr = mMgr.get();
        if (mgr == null) {
            Log.w(TAG, "initiatePairingRequest: called post GC on WifiAwareManager");
            return;
        }
        mgr.initiateBootStrappingSetupRequest(mClientId, mSessionId, peerHandle, method);
    }

    
    
    /**
     * Create a {@link android.net.NetworkRequest.Builder#setNetworkSpecifier(NetworkSpecifier)} for
     * an unencrypted WiFi Aware connection (link) to the specified peer. The
     * {@link android.net.NetworkRequest.Builder#addTransportType(int)} should be set to
     * {@link android.net.NetworkCapabilities#TRANSPORT_WIFI_AWARE}.
     * <p>
     * This method should be used when setting up a connection with a peer discovered through Aware
     * discovery or communication (in such scenarios the MAC address of the peer is shielded by
     * an opaque peer ID handle). If an Aware connection is needed to a peer discovered using other
     * OOB (out-of-band) mechanism then use the alternative
     * {@link WifiAwareSession#createNetworkSpecifierOpen(int, byte[])} method - which uses the
     * peer's MAC address.
     * <p>
     * Note: per the Wi-Fi Aware specification the roles are fixed - a Subscriber is an INITIATOR
     * and a Publisher is a RESPONDER.
     * <p>
     * To set up an encrypted link use the
     * {@link #createNetworkSpecifierPassphrase(PeerHandle, String)} API.
     * @deprecated Use the replacement {@link WifiAwareNetworkSpecifier.Builder}.
     *
     * @param peerHandle The peer's handle obtained through
     * {@link DiscoverySessionCallback#onServiceDiscovered(PeerHandle, byte[], java.util.List)}
     *                   or
     *                   {@link DiscoverySessionCallback#onMessageReceived(PeerHandle, byte[])}.
     *                   On a RESPONDER this value is used to gate the acceptance of a connection
     *                   request from only that peer.
     *
     * @return A {@link NetworkSpecifier} to be used to construct
     * {@link android.net.NetworkRequest.Builder#setNetworkSpecifier(NetworkSpecifier)} to pass to
     * {@link android.net.ConnectivityManager#requestNetwork(android.net.NetworkRequest,
     * android.net.ConnectivityManager.NetworkCallback)}
     * [or other varieties of that API].
     */
    @Deprecated
    public NetworkSpecifier createNetworkSpecifierOpen(@NonNull PeerHandle peerHandle) {
        if (mTerminated) {
            Log.w(TAG, "createNetworkSpecifierOpen: called on terminated session");
            return null;
        }

        WifiAwareManager mgr = mMgr.get();
        if (mgr == null) {
            Log.w(TAG, "createNetworkSpecifierOpen: called post GC on WifiAwareManager");
            return null;
        }

        int role = this instanceof SubscribeDiscoverySession
                ? WifiAwareManager.WIFI_AWARE_DATA_PATH_ROLE_INITIATOR
                : WifiAwareManager.WIFI_AWARE_DATA_PATH_ROLE_RESPONDER;

        return mgr.createNetworkSpecifier(mClientId, role, mSessionId, peerHandle, null, null);
    }

    /**
     * Create a {@link android.net.NetworkRequest.Builder#setNetworkSpecifier(NetworkSpecifier)} for
     * an encrypted WiFi Aware connection (link) to the specified peer. The
     * {@link android.net.NetworkRequest.Builder#addTransportType(int)} should be set to
     * {@link android.net.NetworkCapabilities#TRANSPORT_WIFI_AWARE}.
     * <p>
     * This method should be used when setting up a connection with a peer discovered through Aware
     * discovery or communication (in such scenarios the MAC address of the peer is shielded by
     * an opaque peer ID handle). If an Aware connection is needed to a peer discovered using other
     * OOB (out-of-band) mechanism then use the alternative
     * {@link WifiAwareSession#createNetworkSpecifierPassphrase(int, byte[], String)} method -
     * which uses the peer's MAC address.
     * <p>
     * Note: per the Wi-Fi Aware specification the roles are fixed - a Subscriber is an INITIATOR
     * and a Publisher is a RESPONDER.
     * @deprecated Use the replacement {@link WifiAwareNetworkSpecifier.Builder}.
     *
     * @param peerHandle The peer's handle obtained through
     * {@link DiscoverySessionCallback#onServiceDiscovered(PeerHandle,
     * byte[], java.util.List)} or
     * {@link DiscoverySessionCallback#onMessageReceived(PeerHandle,
     * byte[])}. On a RESPONDER this value is used to gate the acceptance of a connection request
     *                   from only that peer.
     * @param passphrase The passphrase to be used to encrypt the link. The PMK is generated from
     *                   the passphrase. Use the
     *                   {@link #createNetworkSpecifierOpen(PeerHandle)} API to
     *                   specify an open (unencrypted) link.
     *
     * @return A {@link NetworkSpecifier} to be used to construct
     * {@link android.net.NetworkRequest.Builder#setNetworkSpecifier(NetworkSpecifier)} to pass to
     * {@link android.net.ConnectivityManager#requestNetwork(android.net.NetworkRequest,
     * android.net.ConnectivityManager.NetworkCallback)}
     * [or other varieties of that API].
     */
    @Deprecated
    public NetworkSpecifier createNetworkSpecifierPassphrase(
            @NonNull PeerHandle peerHandle, @NonNull String passphrase) {
        if (!WifiAwareUtils.validatePassphrase(passphrase)) {
            throw new IllegalArgumentException("Passphrase must meet length requirements");
        }

        if (mTerminated) {
            Log.w(TAG, "createNetworkSpecifierPassphrase: called on terminated session");
            return null;
        }

        WifiAwareManager mgr = mMgr.get();
        if (mgr == null) {
            Log.w(TAG, "createNetworkSpecifierPassphrase: called post GC on WifiAwareManager");
            return null;
        }

        int role = this instanceof SubscribeDiscoverySession
                ? WifiAwareManager.WIFI_AWARE_DATA_PATH_ROLE_INITIATOR
                : WifiAwareManager.WIFI_AWARE_DATA_PATH_ROLE_RESPONDER;

        return mgr.createNetworkSpecifier(mClientId, role, mSessionId, peerHandle, null,
                passphrase);
    }

    }
