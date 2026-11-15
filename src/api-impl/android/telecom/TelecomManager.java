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

	/* AOSP: false without ITelecomService */
	public boolean isInCall() {
		return false;
	}

	public boolean endCall() {
		return false;
	}

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
}
