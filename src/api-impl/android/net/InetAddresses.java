package android.net;

import java.net.InetAddress;

/* AOSP InetAddresses (API 29): numeric IPv4 and IPv6 addresses, without a name lookup (libcore's
 * InetAddress.isNumeric() and parseNumericAddress()) */
public class InetAddresses {
	private InetAddresses() {}

	public static boolean isNumericAddress(String address) {
		return InetAddress.isNumeric(address);
	}

	public static InetAddress parseNumericAddress(String address) {
		return InetAddress.parseNumericAddress(address);
	}
}
