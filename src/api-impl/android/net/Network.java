package android.net;

public class Network {
	public long getNetworkHandle() {
		return 1L;
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
