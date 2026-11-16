package android.net.ssl;

import javax.net.ssl.SSLException;

/* Conscrypt platform/.../android/net/ssl/InvalidEchDataException (AOSP wraps it from org.conscrypt.InvalidEchDataException):
 * thrown when the ECH (Encrypted Client Hello) data is empty or invalid */
public class InvalidEchDataException extends SSLException {
	public InvalidEchDataException(String message) {
		super(message);
	}
}
