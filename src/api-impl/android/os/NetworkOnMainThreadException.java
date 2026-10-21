package android.os;

/* API 11. Thrown when an app does network I/O on its main thread. ATL never enforces the
 * StrictMode network watchdog, so this is never thrown here, but the class has to exist for the
 * code that catches or references it. */
public class NetworkOnMainThreadException extends RuntimeException {

	public NetworkOnMainThreadException() {
	}
}
