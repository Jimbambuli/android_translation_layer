package android.net.sip;

import android.content.Context;
import android.os.Message;

/* android.net.sip.SipAudioCall (framework-current.txt). ATL has no SIP service and no RTP audio: a call object keeps
 * its profile and listener, never leaves READY_TO_CALL, and every request that would need the service throws. */
@Deprecated
public class SipAudioCall {
	public static class Listener {
		public void onCalling(SipAudioCall call) {
			onChanged(call);
		}

		public void onRinging(SipAudioCall call, SipProfile caller) {
			onChanged(call);
		}

		public void onRingingBack(SipAudioCall call) {
			onChanged(call);
		}

		public void onCallEstablished(SipAudioCall call) {
			onChanged(call);
		}

		public void onCallEnded(SipAudioCall call) {
			onChanged(call);
		}

		public void onCallBusy(SipAudioCall call) {
			onChanged(call);
		}

		public void onCallHeld(SipAudioCall call) {
			onChanged(call);
		}

		public void onError(SipAudioCall call, int errorCode, String errorMessage) {
		}

		public void onReadyToCall(SipAudioCall call) {
			onChanged(call);
		}

		public void onChanged(SipAudioCall call) {
		}
	}

	private final SipProfile mLocalProfile;
	private Listener mListener;
	private boolean mMuted = false;
	private boolean mHold = false;

	public SipAudioCall(Context context, SipProfile localProfile) {
		mLocalProfile = localProfile;
	}

	public void setListener(Listener listener) {
		setListener(listener, false);
	}

	public void setListener(Listener listener, boolean callbackImmediately) {
		mListener = listener;
		if (listener == null || !callbackImmediately)
			return;
		listener.onReadyToCall(this);
	}

	public SipProfile getLocalProfile() {
		return mLocalProfile;
	}

	public SipProfile getPeerProfile() {
		return null;
	}

	public int getState() {
		return SipSession.State.READY_TO_CALL;
	}

	public boolean isInCall() {
		return false;
	}

	public boolean isOnHold() {
		return mHold;
	}

	public void close() {
	}

	public void attachCall(SipSession session, String sessionDescription) throws SipException {
		throw new SipException("SIP service is not available");
	}

	public void makeCall(SipProfile peerProfile, SipSession sipSession, int timeout) throws SipException {
		throw new SipException("SIP service is not available");
	}

	public void endCall() throws SipException {
	}

	public void holdCall(int timeout) throws SipException {
		throw new SipException("SIP service is not available");
	}

	public void answerCall(int timeout) throws SipException {
		throw new SipException("SIP service is not available");
	}

	public void continueCall(int timeout) throws SipException {
		throw new SipException("SIP service is not available");
	}

	public void toggleMute() {
		mMuted = !mMuted;
	}

	public boolean isMuted() {
		return mMuted;
	}

	public void setSpeakerMode(boolean speakerMode) {
	}

	public void sendDtmf(int code) {
		sendDtmf(code, null);
	}

	public void sendDtmf(int code, Message result) {
	}

	public void startAudio() {
	}
}
