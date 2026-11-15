package android.telecom;

/* AOSP telecomm/java/android/telecom/TelecomManager.java: ATL has no telecom service, the answers are those of
 * the branch where getTelecomService() returns null */
public class TelecomManager {
	/* AOSP: null without ITelecomService */
	public String getDefaultDialerPackage() {
		return null;
	}
}
