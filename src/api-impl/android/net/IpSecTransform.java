package android.net;

/* AOSP frameworks/base/net/IpSecTransform.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class IpSecTransform implements java.lang.AutoCloseable {
    public void close() {  }
    public void requestIpSecTransformState(java.util.concurrent.Executor arg0, android.os.OutcomeReceiver<android.net.IpSecTransformState,java.lang.RuntimeException> arg1) {  }

    public static class Builder {
        public Builder(android.content.Context arg0)  {}
        public android.net.IpSecTransform buildTransportModeTransform(java.net.InetAddress arg0, android.net.IpSecManager.SecurityParameterIndex arg1) throws java.io.IOException, android.net.IpSecManager.ResourceUnavailableException, android.net.IpSecManager.SpiUnavailableException { return null; }
        public android.net.IpSecTransform.Builder setAuthenticatedEncryption(android.net.IpSecAlgorithm arg0) { return this; }
        public android.net.IpSecTransform.Builder setAuthentication(android.net.IpSecAlgorithm arg0) { return this; }
        public android.net.IpSecTransform.Builder setEncryption(android.net.IpSecAlgorithm arg0) { return this; }
        public android.net.IpSecTransform.Builder setIpv4Encapsulation(android.net.IpSecManager.UdpEncapsulationSocket arg0, int arg1) { return null; }
    }
}
