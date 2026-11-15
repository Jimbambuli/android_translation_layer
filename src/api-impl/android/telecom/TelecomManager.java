package android.telecom;

/* AOSP telecomm/java/android/telecom/TelecomManager.java: ATL has no telecom service, the answers are those of
 * the branch where getTelecomService() returns null */
public class TelecomManager {
	/* AOSP: null without ITelecomService */
	public String getDefaultDialerPackage() {
		return null;
	}

	/* the methods below are AOSP's with getTelecomService() == null: "if (service != null)" skips the call */
	public void registerPhoneAccount(PhoneAccount account) {}

	public void unregisterPhoneAccount(PhoneAccountHandle accountHandle) {}

	public void addNewIncomingCall(PhoneAccountHandle phoneAccount, android.os.Bundle extras) {}

	public void placeCall(android.net.Uri address, android.os.Bundle extras) {}
}
