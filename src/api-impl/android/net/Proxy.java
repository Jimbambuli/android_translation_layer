package android.net;

/* AOSP frameworks/base/net/Proxy.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class Proxy {
    public Proxy()  {}
    @Deprecated public static String getDefaultHost() { return null; }
    @Deprecated public static int getDefaultPort() { return 0; }
    @Deprecated public static String getHost(android.content.Context arg0) { return null; }
    @Deprecated public static int getPort(android.content.Context arg0) { return 0; }
    public static final String PROXY_CHANGE_ACTION = "android.intent.action.PROXY_CHANGE";
}
