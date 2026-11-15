package android.net.rtp;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketException;

/* android.net.rtp.RtpStream (framework-current.txt). AOSP opens a UDP socket on the local address in native code
 * (the local port is the one it chose); here that socket is a DatagramSocket. */
@Deprecated
public class RtpStream {
	public static final int MODE_NORMAL = 0;
	public static final int MODE_SEND_ONLY = 1;
	public static final int MODE_RECEIVE_ONLY = 2;

	private final InetAddress mLocalAddress;
	private final int mLocalPort;

	private InetAddress mRemoteAddress;
	private int mRemotePort = -1;
	private int mMode = MODE_NORMAL;
	private DatagramSocket mSocket;

	RtpStream(InetAddress address) throws SocketException {
		mSocket = new DatagramSocket(0, address);
		mLocalPort = mSocket.getLocalPort();
		mLocalAddress = address;
	}

	public InetAddress getLocalAddress() {
		return mLocalAddress;
	}

	public int getLocalPort() {
		return mLocalPort;
	}

	public InetAddress getRemoteAddress() {
		return mRemoteAddress;
	}

	public int getRemotePort() {
		return mRemotePort;
	}

	public boolean isBusy() {
		return false;
	}

	public int getMode() {
		return mMode;
	}

	public void setMode(int mode) {
		if (isBusy()) {
			throw new IllegalStateException("Busy");
		}
		if (mode != MODE_NORMAL && mode != MODE_SEND_ONLY && mode != MODE_RECEIVE_ONLY) {
			throw new IllegalArgumentException("Invalid mode");
		}
		mMode = mode;
	}

	public void associate(InetAddress address, int port) {
		if (isBusy()) {
			throw new IllegalStateException("Busy");
		}
		if (!(address instanceof Inet4Address && mLocalAddress instanceof Inet4Address) &&
			!(address instanceof Inet6Address && mLocalAddress instanceof Inet6Address)) {
			throw new IllegalArgumentException("Unsupported address");
		}
		mRemoteAddress = address;
		mRemotePort = port;
	}

	public void release() {
		synchronized (this) {
			if (isBusy()) {
				throw new IllegalStateException("Busy");
			}
			if (mSocket != null) {
				mSocket.close();
				mSocket = null;
			}
		}
	}
}
