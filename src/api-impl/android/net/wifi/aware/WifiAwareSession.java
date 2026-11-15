package android.net.wifi.aware;

/* AOSP frameworks/base/net/wifi/aware/WifiAwareSession.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public class WifiAwareSession implements java.lang.AutoCloseable {
    public void close() {  }
    @Deprecated public android.net.NetworkSpecifier createNetworkSpecifierOpen(int arg0, byte[] arg1) { return null; }
    @Deprecated public android.net.NetworkSpecifier createNetworkSpecifierPassphrase(int arg0, byte[] arg1, String arg2) { return null; }
    public void publish(android.net.wifi.aware.PublishConfig arg0, android.net.wifi.aware.DiscoverySessionCallback arg1, android.os.Handler arg2) {  }
    public void subscribe(android.net.wifi.aware.SubscribeConfig arg0, android.net.wifi.aware.DiscoverySessionCallback arg1, android.os.Handler arg2) {  }
}
