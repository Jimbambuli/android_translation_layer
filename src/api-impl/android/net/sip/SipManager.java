package android.net.sip;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import java.text.ParseException;

/* android.net.sip.SipManager (framework-current.txt). ATL does not list android.software.sip, so newInstance()
 * answers null as on a device without the SIP API; the instance methods report the missing service with a
 * SipException. */
@Deprecated
public class SipManager {
	public static final String EXTRA_CALL_ID = "android:sipCallID";
	public static final String EXTRA_OFFER_SD = "android:sipOfferSD";
	public static final int INCOMING_CALL_RESULT_CODE = 101;

	private final Context mContext;

	public static SipManager newInstance(Context context) {
		return (isApiSupported(context) ? new SipManager(context) : null);
	}

	public static boolean isApiSupported(Context context) {
		return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SIP);
	}

	public static boolean isVoipSupported(Context context) {
		return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SIP_VOIP) && isApiSupported(context);
	}

	/* AOSP reads the config_sip_wifi_only resource, false in the framework defaults */
	public static boolean isSipWifiOnly(Context context) {
		return false;
	}

	private SipManager(Context context) {
		mContext = context;
	}

	public static boolean isIncomingCallIntent(Intent intent) {
		if (intent == null)
			return false;
		String callId = getCallId(intent);
		String offerSd = getOfferSessionDescription(intent);
		return ((callId != null) && (offerSd != null));
	}

	public static String getCallId(Intent incomingCallIntent) {
		return incomingCallIntent.getStringExtra(EXTRA_CALL_ID);
	}

	public static String getOfferSessionDescription(Intent incomingCallIntent) {
		return incomingCallIntent.getStringExtra(EXTRA_OFFER_SD);
	}

	private static SipException noService() {
		return new SipException("SIP service is not available");
	}

	public void open(SipProfile localProfile) throws SipException {
		throw noService();
	}

	public void open(SipProfile localProfile, PendingIntent incomingCallPendingIntent, SipRegistrationListener listener) throws SipException {
		throw noService();
	}

	public void setRegistrationListener(String localProfileUri, SipRegistrationListener listener) throws SipException {
		throw noService();
	}

	public void close(String localProfileUri) throws SipException {
		throw noService();
	}

	public boolean isOpened(String localProfileUri) throws SipException {
		throw noService();
	}

	public boolean isRegistered(String localProfileUri) throws SipException {
		throw noService();
	}

	public SipAudioCall makeAudioCall(SipProfile localProfile, SipProfile peerProfile, SipAudioCall.Listener listener, int timeout) throws SipException {
		if (!isVoipSupported(mContext))
			throw new SipException("VOIP API is not supported");
		throw noService();
	}

	public SipAudioCall makeAudioCall(String localProfileUri, String peerProfileUri, SipAudioCall.Listener listener, int timeout) throws SipException {
		if (!isVoipSupported(mContext))
			throw new SipException("VOIP API is not supported");
		try {
			SipProfile localProfile = new SipProfile.Builder(localProfileUri).build();
			SipProfile peerProfile = new SipProfile.Builder(peerProfileUri).build();
			return makeAudioCall(localProfile, peerProfile, listener, timeout);
		} catch (ParseException e) {
			throw new SipException("build SipProfile", e);
		}
	}

	public SipAudioCall takeAudioCall(Intent incomingCallIntent, SipAudioCall.Listener listener) throws SipException {
		if (incomingCallIntent == null)
			throw new SipException("Cannot retrieve session with null intent");
		if (!isVoipSupported(mContext))
			throw new SipException("VOIP API is not supported");
		throw noService();
	}

	public void register(SipProfile localProfile, int expiryTime, SipRegistrationListener listener) throws SipException {
		throw noService();
	}

	public void unregister(SipProfile localProfile, SipRegistrationListener listener) throws SipException {
		throw noService();
	}

	public SipSession getSessionFor(Intent incomingCallIntent) throws SipException {
		throw noService();
	}

	public SipSession createSipSession(SipProfile localProfile, SipSession.Listener listener) throws SipException {
		throw noService();
	}
}
