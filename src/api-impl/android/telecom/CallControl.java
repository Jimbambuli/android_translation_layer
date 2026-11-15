package android.telecom;

import android.os.Bundle;
import android.os.OutcomeReceiver;
import android.os.ParcelUuid;
import java.util.Objects;
import java.util.concurrent.Executor;

/* AOSP telecomm/framework/java/android/telecom/CallControl.java: the instance is handed out by
 * TelecomManager.addCall() through an ICallControl binder. ATL has no telecom service, addCall() throws, so there
 * is never an instance and the operations below fail like addCall() does. */
public final class CallControl {
	private final String mCallId;

	private CallControl(String callId) {
		mCallId = callId;
	}

	public ParcelUuid getCallId() {
		return ParcelUuid.fromString(mCallId);
	}

	private static void fail() {
		throw new IllegalStateException("Telecom service is not present");
	}

	public void setActive(Executor executor, OutcomeReceiver<Void, CallException> callback) {
		Objects.requireNonNull(executor);
		Objects.requireNonNull(callback);
		fail();
	}

	public void answer(int videoState, Executor executor, OutcomeReceiver<Void, CallException> callback) {
		Objects.requireNonNull(executor);
		Objects.requireNonNull(callback);
		fail();
	}

	public void setInactive(Executor executor, OutcomeReceiver<Void, CallException> callback) {
		Objects.requireNonNull(executor);
		Objects.requireNonNull(callback);
		fail();
	}

	public void disconnect(DisconnectCause disconnectCause, Executor executor, OutcomeReceiver<Void, CallException> callback) {
		Objects.requireNonNull(disconnectCause);
		Objects.requireNonNull(executor);
		Objects.requireNonNull(callback);
		fail();
	}

	public void requestCallEndpointChange(CallEndpoint callEndpoint, Executor executor, OutcomeReceiver<Void, CallEndpointException> callback) {
		Objects.requireNonNull(callEndpoint);
		Objects.requireNonNull(executor);
		Objects.requireNonNull(callback);
		fail();
	}

	public void requestMuteState(boolean isMuted, Executor executor, OutcomeReceiver<Void, CallException> callback) {
		Objects.requireNonNull(executor);
		Objects.requireNonNull(callback);
		fail();
	}

	public void requestVideoState(int videoState, Executor executor, OutcomeReceiver<Void, CallException> callback) {
		Objects.requireNonNull(executor);
		Objects.requireNonNull(callback);
		fail();
	}

	public void sendEvent(String event, Bundle extras) {
		Objects.requireNonNull(event);
		Objects.requireNonNull(extras);
		fail();
	}
}
