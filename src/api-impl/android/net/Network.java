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

	public void bindSocket(java.net.Socket socket) throws java.io.IOException {}

	public void bindSocket(java.net.DatagramSocket socket) throws java.io.IOException {}

	public void bindSocket(java.io.FileDescriptor fd) throws java.io.IOException {}
}
