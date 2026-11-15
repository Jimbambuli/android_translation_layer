package android.net;

public class SSLCertificateSocketFactory {
	private final int mHandshakeTimeoutMillis;

	/* AOSP SSLCertificateSocketFactory(int handshakeTimeoutMillis) */
	public SSLCertificateSocketFactory(int handshakeTimeoutMillis) {
		mHandshakeTimeoutMillis = handshakeTimeoutMillis;
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setHostname(java.net.Socket arg0, String arg1) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.net.Socket createSocket(String arg0, int arg1) throws java.io.IOException { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.net.Socket createSocket(String arg0, int arg1, java.net.InetAddress arg2, int arg3) throws java.io.IOException { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.net.Socket createSocket(java.net.InetAddress arg0, int arg1) throws java.io.IOException { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.net.Socket createSocket(java.net.InetAddress arg0, int arg1, java.net.InetAddress arg2, int arg3) throws java.io.IOException { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.net.Socket createSocket(java.net.Socket arg0, String arg1, int arg2, boolean arg3) throws java.io.IOException { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static javax.net.SocketFactory getDefault(int arg0) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static javax.net.ssl.SSLSocketFactory getDefault(int arg0, android.net.SSLSessionCache arg1) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public String[] getDefaultCipherSuites() { return new String[0]; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static javax.net.ssl.SSLSocketFactory getInsecure(int arg0, android.net.SSLSessionCache arg1) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public byte[] getNpnSelectedProtocol(java.net.Socket arg0) { return new byte[0]; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public String[] getSupportedCipherSuites() { return new String[0]; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setKeyManagers(javax.net.ssl.KeyManager[] arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setNpnProtocols(byte[][] arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setTrustManagers(javax.net.ssl.TrustManager[] arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setUseSessionTickets(java.net.Socket arg0, boolean arg1) {  }
}
