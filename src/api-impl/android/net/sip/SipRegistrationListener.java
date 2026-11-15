package android.net.sip;

/* android.net.sip.SipRegistrationListener (framework-current.txt) */
@Deprecated
public interface SipRegistrationListener {
	void onRegistering(String localProfileUri);

	void onRegistrationDone(String localProfileUri, long expiryTime);

	void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage);
}
