package android.net.sip;

/* android.net.sip.SipException (framework-current.txt) */
@Deprecated
public class SipException extends Exception {
	public SipException() {
	}

	public SipException(String message) {
		super(message);
	}

	public SipException(String message, Throwable cause) {
		super(message, cause);
	}
}
