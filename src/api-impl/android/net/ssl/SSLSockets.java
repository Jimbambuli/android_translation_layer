package android.net.ssl;

import javax.net.ssl.SSLSocket;

public class SSLSockets {

	/* ATL's sockets are wolfJSSE's, not Conscrypt's, which AOSP's SSLSockets works on */
	public static boolean isSupportedSocket(SSLSocket socket) {
		return false;
	}

	/* AOSP: Conscrypt.setUseSessionTickets(), which turns a socket that is not Conscrypt's down */
	public static void setUseSessionTickets(SSLSocket socket, boolean useSessionTickets) {
		if (!isSupportedSocket(socket))
			throw new IllegalArgumentException("Not a conscrypt socket: " + socket.getClass().getName());
	}

	/* AOSP: Conscrypt.setEchConfigList(socket, echConfigList.toBytes()), which turns a socket that is not Conscrypt's down */
	public static void setEchConfigList(SSLSocket socket, EchConfigList echConfigList) {
		if (!isSupportedSocket(socket))
			throw new IllegalArgumentException("Not a conscrypt socket: " + socket.getClass().getName());
	}
}
