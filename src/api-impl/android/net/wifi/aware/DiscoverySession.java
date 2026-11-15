package android.net.wifi.aware;

/* AOSP frameworks/base/net/wifi/aware/DiscoverySession.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public class DiscoverySession implements java.lang.AutoCloseable {
    public void acceptPairingRequest(int arg0, android.net.wifi.aware.PeerHandle arg1, String arg2, int arg3, String arg4) {  }
    public void close() {  }
    @Deprecated public android.net.NetworkSpecifier createNetworkSpecifierOpen(android.net.wifi.aware.PeerHandle arg0) { return null; }
    @Deprecated public android.net.NetworkSpecifier createNetworkSpecifierPassphrase(android.net.wifi.aware.PeerHandle arg0, String arg1) { return null; }
    public void initiateBootstrappingRequest(android.net.wifi.aware.PeerHandle arg0, int arg1) {  }
    public void initiatePairingRequest(android.net.wifi.aware.PeerHandle arg0, String arg1, int arg2, String arg3) {  }
    public void rejectPairingRequest(int arg0, android.net.wifi.aware.PeerHandle arg1) {  }
    public void sendMessage(android.net.wifi.aware.PeerHandle arg0, int arg1, byte[] arg2) {  }
}
