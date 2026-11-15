package android.net;

/* AOSP frameworks/base/net/IpSecManager.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public class IpSecManager {
    public android.net.IpSecManager.SecurityParameterIndex allocateSecurityParameterIndex(java.net.InetAddress arg0) throws android.net.IpSecManager.ResourceUnavailableException { return null; }
    public android.net.IpSecManager.SecurityParameterIndex allocateSecurityParameterIndex(java.net.InetAddress arg0, int arg1) throws android.net.IpSecManager.ResourceUnavailableException, android.net.IpSecManager.SpiUnavailableException { return null; }
    public void applyTransportModeTransform(java.net.Socket arg0, int arg1, android.net.IpSecTransform arg2) throws java.io.IOException {  }
    public void applyTransportModeTransform(java.net.DatagramSocket arg0, int arg1, android.net.IpSecTransform arg2) throws java.io.IOException {  }
    public void applyTransportModeTransform(java.io.FileDescriptor arg0, int arg1, android.net.IpSecTransform arg2) throws java.io.IOException {  }
    public android.net.IpSecManager.UdpEncapsulationSocket openUdpEncapsulationSocket(int arg0) throws java.io.IOException, android.net.IpSecManager.ResourceUnavailableException { return null; }
    public android.net.IpSecManager.UdpEncapsulationSocket openUdpEncapsulationSocket() throws java.io.IOException, android.net.IpSecManager.ResourceUnavailableException { return null; }
    public void removeTransportModeTransforms(java.net.Socket arg0) throws java.io.IOException {  }
    public void removeTransportModeTransforms(java.net.DatagramSocket arg0) throws java.io.IOException {  }
    public void removeTransportModeTransforms(java.io.FileDescriptor arg0) throws java.io.IOException {  }
    public static final int DIRECTION_IN = 0; // 0x0;
    public static final int DIRECTION_OUT = 1; // 0x1;

    public static final class ResourceUnavailableException extends android.util.AndroidException {
    }

    public static final class SecurityParameterIndex implements java.lang.AutoCloseable {
        public void close() {  }
        public int getSpi() { return 0; }
    }

    public static final class SpiUnavailableException extends android.util.AndroidException {
        public int getSpi() { return 0; }
    }

    public static final class UdpEncapsulationSocket implements java.lang.AutoCloseable {
        public void close() throws java.io.IOException {  }
        public java.io.FileDescriptor getFileDescriptor() { return null; }
        public int getPort() { return 0; }
    }
}
