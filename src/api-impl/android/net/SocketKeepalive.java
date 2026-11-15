package android.net;

/* AOSP frameworks/base/net/SocketKeepalive.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public abstract class SocketKeepalive implements java.lang.AutoCloseable {
    public final void close() {  }
    public final void start(int arg0) {  }
    public final void stop() {  }
    public static final int ERROR_HARDWARE_ERROR = -31; // 0xffffffe1;
    public static final int ERROR_INSUFFICIENT_RESOURCES = -32; // 0xffffffe0;
    public static final int ERROR_INVALID_INTERVAL = -24; // 0xffffffe8;
    public static final int ERROR_INVALID_IP_ADDRESS = -21; // 0xffffffeb;
    public static final int ERROR_INVALID_LENGTH = -23; // 0xffffffe9;
    public static final int ERROR_INVALID_NETWORK = -20; // 0xffffffec;
    public static final int ERROR_INVALID_PORT = -22; // 0xffffffea;
    public static final int ERROR_INVALID_SOCKET = -25; // 0xffffffe7;
    public static final int ERROR_SOCKET_NOT_IDLE = -26; // 0xffffffe6;
    public static final int ERROR_UNSUPPORTED = -30; // 0xffffffe2;

    public static class Callback {
        public Callback()  {}
        public void onDataReceived() {  }
        public void onError(int arg0) {  }
        public void onStarted() {  }
        public void onStopped() {  }
    }
}
