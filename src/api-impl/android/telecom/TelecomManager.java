package android.telecom;

import android.os.OutcomeReceiver;
import java.util.Objects;
import java.util.concurrent.Executor;

/* AOSP telecomm/framework/java/android/telecom/TelecomManager.java: ATL has no telecom service, the answers are
 * those of the branch where getTelecomService() returns null */
public class TelecomManager {
	/* AOSP: null without ITelecomService */
	public String getDefaultDialerPackage() {
		return null;
	}

	/* the methods below are AOSP's with getTelecomService() == null: "if (service != null)" skips the call */
	public void registerPhoneAccount(PhoneAccount account) {}

	public void unregisterPhoneAccount(PhoneAccountHandle accountHandle) {}

	public void addNewIncomingCall(PhoneAccountHandle phoneAccount, android.os.Bundle extras) {}

	/* AOSP placeCall() -> TelecomServiceImpl.placeCall(): CALL_PHONE (or CALL_PRIVILEGED), else SecurityException; then
	 * Telecom places the call. ATL hands the number to the desktop's dialer (tel: link, OpenURI portal), as an
	 * ACTION_CALL intent (Context.startActivity()) */
	public void placeCall(android.net.Uri address, android.os.Bundle extras) {
		android.content.Context context = android.app.ActivityThread.currentApplication();
		if (context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED
		    && context.checkSelfPermission("android.permission.CALL_PRIVILEGED") != android.content.pm.PackageManager.PERMISSION_GRANTED)
			throw new SecurityException("Package " + context.getPackageName() + " is not allowed to place phone calls");
		if (address == null) {
			android.util.Log.w("TelecomManager", "Cannot place call to empty address.");
			return;
		}
		android.app.Activity.nativeOpenURI(android.app.ContextImpl.desktopUri(address));
	}

	/* public extras keys of android.telecom.TelecomManager (values as in the SDK documentation) */
	public static final String EXTRA_INCOMING_CALL_ADDRESS = "android.telecom.extra.INCOMING_CALL_ADDRESS";
	public static final String EXTRA_UNKNOWN_CALL_HANDLE = "android.telecom.extra.UNKNOWN_CALL_HANDLE";

	/* more methods with getTelecomService() == null (defaults written from knowledge of AOSP TelecomManager: the
	 * Telecom sources are not in the local AOSP copy): isInCall/endCall false, no accounts, nothing to cancel */
	public boolean isInCall() {
		return false;
	}

	public boolean endCall() {
		return false;
	}

	public PhoneAccount getPhoneAccount(PhoneAccountHandle account) {
		return null;
	}

	public java.util.List<PhoneAccountHandle> getCallCapablePhoneAccounts() {
		return new java.util.ArrayList<PhoneAccountHandle>();
	}

	public java.util.List<PhoneAccountHandle> getOwnSelfManagedPhoneAccounts() {
		return new java.util.ArrayList<PhoneAccountHandle>();
	}

	public PhoneAccountHandle getDefaultOutgoingPhoneAccount(String uriScheme) {
		return null;
	}

	public void cancelMissedCallsNotification() {}
	public boolean isIncomingCallPermitted(PhoneAccountHandle phoneAccountHandle) {
		return false;
	}

	public boolean isOutgoingCallPermitted(PhoneAccountHandle phoneAccountHandle) {
		return false;
	}

	/* AOSP: null checks, then IllegalStateException when getTelecomService() is null */
	public void addCall(CallAttributes callAttributes, Executor executor, OutcomeReceiver<CallControl, CallException> pendingControl, CallControlCallback handshakes, CallEventCallback events) {
		Objects.requireNonNull(callAttributes);
		Objects.requireNonNull(executor);
		Objects.requireNonNull(pendingControl);
		Objects.requireNonNull(handshakes);
		Objects.requireNonNull(events);
		throw new IllegalStateException("Telecom service is not present");
	}

	/* AOSP TelecomManager.PRESENTATION_UNKNOWN */
	public static final int PRESENTATION_UNKNOWN = 3;
}
