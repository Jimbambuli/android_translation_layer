package android.net;

public class Network {
	public long getNetworkHandle() {
		return 1L;
	}

	/* AOSP refuses handle 0 (NETID_UNSET); ATL has one network, so any other handle is that network */
	public static Network fromNetworkHandle(long networkHandle) {
		if (networkHandle == 0)
			throw new IllegalArgumentException("Network.fromNetworkHandle refusing to instantiate NETID_UNSET Network.");
		return new Network();
	}

	/* AOSP Network.java binds name lookups and sockets to this network's netId. ATL has one network
	 * (the host's), so each of them is the host's own resolver and socket factory */
	public java.net.InetAddress[] getAllByName(String host) throws java.net.UnknownHostException {
		return java.net.InetAddress.getAllByName(host);
	}

	public java.net.InetAddress getByName(String host) throws java.net.UnknownHostException {
		return java.net.InetAddress.getByName(host);
	}

	public javax.net.SocketFactory getSocketFactory() {
		return javax.net.SocketFactory.getDefault();
	}

	/* AOSP Network.openConnection(URL): with the network's proxy (ConnectivityManager.getProxyForNetwork(), none on
	 * ATL: getDefaultProxy() is null) -> openConnection(url, Proxy.NO_PROXY) */
	public java.net.URLConnection openConnection(java.net.URL url) throws java.io.IOException {
		return openConnection(url, java.net.Proxy.NO_PROXY);
	}

	/* AOSP Network.openConnection(URL, Proxy): an HTTP or HTTPS connection whose names and sockets go through this
	 * network; on ATL's one network that is the platform's own URLConnection */
	public java.net.URLConnection openConnection(java.net.URL url, java.net.Proxy proxy) throws java.io.IOException {
		if (proxy == null)
			throw new IllegalArgumentException("proxy is null");
		return url.openConnection(proxy);
	}

	public void bindSocket(java.net.Socket socket) throws java.io.IOException {}

	public void bindSocket(java.net.DatagramSocket socket) throws java.io.IOException {}

	public void bindSocket(java.io.FileDescriptor fd) throws java.io.IOException {}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int describeContents() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
}
