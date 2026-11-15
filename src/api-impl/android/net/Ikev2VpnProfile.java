package android.net;

/* AOSP frameworks/base/net/Ikev2VpnProfile.java (public API surface; no backing network service in ATL, bodies follow the AOSP no-service path) */
public final class Ikev2VpnProfile extends android.net.PlatformVpnProfile {
    public java.util.List<java.lang.String> getAllowedAlgorithms() { return new java.util.ArrayList<>(); }
    public android.net.ipsec.ike.IkeTunnelConnectionParams getIkeTunnelConnectionParams() { return null; }
    public int getMaxMtu() { return 0; }
    public String getPassword() { return null; }
    public byte[] getPresharedKey() { return new byte[0]; }
    public android.net.ProxyInfo getProxyInfo() { return null; }
    public java.security.PrivateKey getRsaPrivateKey() { return null; }
    public String getServerAddr() { return null; }
    public java.security.cert.X509Certificate getServerRootCaCert() { return null; }
    public java.security.cert.X509Certificate getUserCert() { return null; }
    public String getUserIdentity() { return null; }
    public String getUsername() { return null; }
    public boolean isAutomaticIpVersionSelectionEnabled() { return false; }
    public boolean isAutomaticNattKeepaliveTimerEnabled() { return false; }
    public boolean isBypassable() { return false; }
    public boolean isMetered() { return false; }

    public static final class Builder {
        public Builder(String arg0, String arg1)  {}
        public Builder(android.net.ipsec.ike.IkeTunnelConnectionParams arg0)  {}
        public android.net.Ikev2VpnProfile build() { return null; }
        public android.net.Ikev2VpnProfile.Builder setAllowedAlgorithms(java.util.List<java.lang.String> arg0) { return this; }
        public android.net.Ikev2VpnProfile.Builder setAuthDigitalSignature(java.security.cert.X509Certificate arg0, java.security.PrivateKey arg1, java.security.cert.X509Certificate arg2) { return this; }
        public android.net.Ikev2VpnProfile.Builder setAuthPsk(byte[] arg0) { return this; }
        public android.net.Ikev2VpnProfile.Builder setAuthUsernamePassword(String arg0, String arg1, java.security.cert.X509Certificate arg2) { return this; }
        public android.net.Ikev2VpnProfile.Builder setAutomaticIpVersionSelectionEnabled(boolean arg0) { return this; }
        public android.net.Ikev2VpnProfile.Builder setAutomaticNattKeepaliveTimerEnabled(boolean arg0) { return this; }
        public android.net.Ikev2VpnProfile.Builder setBypassable(boolean arg0) { return this; }
        public android.net.Ikev2VpnProfile.Builder setLocalRoutesExcluded(boolean arg0) { return this; }
        public android.net.Ikev2VpnProfile.Builder setMaxMtu(int arg0) { return this; }
        public android.net.Ikev2VpnProfile.Builder setMetered(boolean arg0) { return this; }
        public android.net.Ikev2VpnProfile.Builder setProxy(android.net.ProxyInfo arg0) { return this; }
        public android.net.Ikev2VpnProfile.Builder setRequiresInternetValidation(boolean arg0) { return this; }
    }
}
