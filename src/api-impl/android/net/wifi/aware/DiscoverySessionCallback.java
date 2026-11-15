package android.net.wifi.aware;

import android.annotation.NonNull;
import java.util.List;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/aware/DiscoverySessionCallback.java */
public class DiscoverySessionCallback {
	public void onPublishStarted(@NonNull PublishDiscoverySession session) {
	}

	public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
	}

	public void onSessionConfigUpdated() {
	}

	public void onSessionConfigFailed() {
	}

	public void onSessionTerminated() {
	}

	public void onServiceDiscovered(PeerHandle peerHandle, byte[] serviceSpecificInfo,
			List<byte[]> matchFilter) {
	}

	public void onServiceDiscovered(@NonNull ServiceDiscoveryInfo info) {
	}

	public void onServiceDiscoveredWithinRange(PeerHandle peerHandle, byte[] serviceSpecificInfo,
			List<byte[]> matchFilter, int distanceMm) {
	}

	public void onServiceDiscoveredWithinRange(@NonNull ServiceDiscoveryInfo info, int distanceMm) {
	}

	public void onMessageSendSucceeded(int messageId) {
	}

	public void onMessageSendFailed(int messageId) {
	}

	public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
	}

	public void onServiceLost(@NonNull PeerHandle peerHandle, int reason) {
	}

	public void onPairingSetupRequestReceived(@NonNull PeerHandle peerHandle, int requestId) {
	}

	public void onPairingSetupSucceeded(@NonNull PeerHandle peerHandle, @NonNull String alias) {
	}

	public void onPairingSetupFailed(@NonNull PeerHandle peerHandle) {
	}

	public void onPairingVerificationSucceed(@NonNull PeerHandle peerHandle, @NonNull String alias) {
	}

	public void onPairingVerificationFailed(@NonNull PeerHandle peerHandle) {
	}

	public void onBootstrappingSucceeded(@NonNull PeerHandle peerHandle, int method) {
	}

	public void onBootstrappingFailed(@NonNull PeerHandle peerHandle) {
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public DiscoverySessionCallback() {}
}
