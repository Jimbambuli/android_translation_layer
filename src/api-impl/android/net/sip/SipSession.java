package android.net.sip;

/* android.net.sip.SipSession (framework-current.txt). ATL has no SIP service: like a session whose service call fails
 * with a RemoteException, the getters answer their "unknown" values and the requests do nothing. */
@Deprecated
public final class SipSession {
	public static class State {
		public static final int READY_TO_CALL = 0;
		public static final int REGISTERING = 1;
		public static final int DEREGISTERING = 2;
		public static final int INCOMING_CALL = 3;
		public static final int INCOMING_CALL_ANSWERING = 4;
		public static final int OUTGOING_CALL = 5;
		public static final int OUTGOING_CALL_RING_BACK = 6;
		public static final int OUTGOING_CALL_CANCELING = 7;
		public static final int IN_CALL = 8;
		public static final int PINGING = 9;
		public static final int NOT_DEFINED = 101;

		public static String toString(int state) {
			switch (state) {
				case READY_TO_CALL: return "READY_TO_CALL";
				case REGISTERING: return "REGISTERING";
				case DEREGISTERING: return "DEREGISTERING";
				case INCOMING_CALL: return "INCOMING_CALL";
				case INCOMING_CALL_ANSWERING: return "INCOMING_CALL_ANSWERING";
				case OUTGOING_CALL: return "OUTGOING_CALL";
				case OUTGOING_CALL_RING_BACK: return "OUTGOING_CALL_RING_BACK";
				case OUTGOING_CALL_CANCELING: return "OUTGOING_CALL_CANCELING";
				case IN_CALL: return "IN_CALL";
				case PINGING: return "PINGING";
				default: return "NOT_DEFINED";
			}
		}

		private State() {
		}
	}

	public static class Listener {
		public void onCalling(SipSession session) {
		}

		public void onRinging(SipSession session, SipProfile caller, String sessionDescription) {
		}

		public void onRingingBack(SipSession session) {
		}

		public void onCallEstablished(SipSession session, String sessionDescription) {
		}

		public void onCallEnded(SipSession session) {
		}

		public void onCallBusy(SipSession session) {
		}

		public void onCallChangeFailed(SipSession session, int errorCode, String errorMessage) {
		}

		public void onError(SipSession session, int errorCode, String errorMessage) {
		}

		public void onRegistering(SipSession session) {
		}

		public void onRegistrationDone(SipSession session, int duration) {
		}

		public void onRegistrationFailed(SipSession session, int errorCode, String errorMessage) {
		}

		public void onRegistrationTimeout(SipSession session) {
		}
	
}

	private final SipProfile mLocalProfile;
	private Listener mListener;

	SipSession(SipProfile localProfile, Listener listener) {
		mLocalProfile = localProfile;
		mListener = listener;
	}

	public String getLocalIp() {
		return "127.0.0.1";
	}

	public SipProfile getLocalProfile() {
		return mLocalProfile;
	}

	public SipProfile getPeerProfile() {
		return null;
	}

	public int getState() {
		return State.NOT_DEFINED;
	}

	public boolean isInCall() {
		return false;
	}

	public String getCallId() {
		return null;
	}

	public void setListener(Listener listener) {
		mListener = listener;
	}

	public void register(int duration) {
	}

	public void unregister() {
	}

	public void makeCall(SipProfile callee, String sessionDescription, int timeout) {
	}

	public void answerCall(String sessionDescription, int timeout) {
	}

	public void endCall() {
	}

	public void changeCall(String sessionDescription, int timeout) {
	}
}
