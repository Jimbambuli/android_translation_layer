package android.nfc.cardemulation;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/* AOSP HostApduService (modules/Nfc). The Flags.nfcHceLatencyEvents() paths (ack messages, trace sections) are
 * left out: ATL has no NFC service that sends or expects them. */
public abstract class HostApduService extends Service {
	public static final String SERVICE_INTERFACE = "android.nfc.cardemulation.action.HOST_APDU_SERVICE";
	public static final String SERVICE_META_DATA = "android.nfc.cardemulation.host_apdu_service";
	public static final int DEACTIVATION_LINK_LOSS = 0;
	public static final int DEACTIVATION_DESELECTED = 1;

	static final String TAG = "ApduService";

	public static final int MSG_COMMAND_APDU = 0;
	public static final int MSG_RESPONSE_APDU = 1;
	public static final int MSG_DEACTIVATED = 2;
	public static final int MSG_UNHANDLED = 3;
	public static final int MSG_POLLING_LOOP = 4;
	public static final int MSG_COMMAND_APDU_ACK = 5;
	public static final int MSG_RESPONSE_APDU_ACK = 6;
	public static final int MSG_POLLING_LOOP_ACK = 7;
	public static final String KEY_DATA = "data";
	public static final String KEY_POLLING_LOOP_FRAMES_BUNDLE = "android.nfc.cardemulation.POLLING_FRAMES";

	Messenger mNfcService = null;
	final Messenger mMessenger = new Messenger(new MsgHandler());

	public HostApduService() {}

	public Messenger getNfcService() {
		return mNfcService;
	}

	public void setNfcService(Messenger nfcService) {
		mNfcService = nfcService;
	}

	public Messenger getMessenger() {
		return mMessenger;
	}

	final class MsgHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_COMMAND_APDU:
					Bundle dataBundle = msg.getData();
					if (dataBundle == null) {
						return;
					}
					if (getNfcService() == null)
						setNfcService(msg.replyTo);

					byte[] apdu = dataBundle.getByteArray(KEY_DATA);
					if (apdu != null) {
						byte[] responseApdu = processCommandApdu(apdu, null);
						if (responseApdu != null) {
							if (getNfcService() == null) {
								Log.e(TAG, "Response not sent; service was deactivated.");
								return;
							}
							Message responseMsg = Message.obtain(null, MSG_RESPONSE_APDU);
							Bundle responseBundle = new Bundle();
							responseBundle.putByteArray(KEY_DATA, responseApdu);
							responseMsg.setData(responseBundle);
							responseMsg.replyTo = getMessenger();
							try {
								getNfcService().send(responseMsg);
							} catch (RemoteException e) {
								Log.e(TAG, "Response not sent; RemoteException calling into NfcService.");
							}
						}
					} else {
						Log.e(TAG, "Received MSG_COMMAND_APDU without data.");
					}
					break;
				case MSG_RESPONSE_APDU:
					if (getNfcService() == null) {
						Log.e(TAG, "Response not sent; service was deactivated.");
						return;
					}
					try {
						msg.replyTo = getMessenger();
						getNfcService().send(msg);
					} catch (RemoteException e) {
						Log.e(TAG, "RemoteException calling into NfcService.");
					}
					break;
				case MSG_DEACTIVATED:
					// Make sure we won't call into NfcService again
					setNfcService(null);
					onDeactivated(msg.arg1);
					break;
				case MSG_UNHANDLED:
					if (getNfcService() == null) {
						Log.e(TAG, "notifyUnhandled not sent; service was deactivated.");
						return;
					}
					try {
						msg.replyTo = getMessenger();
						getNfcService().send(msg);
					} catch (RemoteException e) {
						Log.e(TAG, "RemoteException calling into NfcService.");
					}
					break;
				case MSG_POLLING_LOOP:
					ArrayList<PollingFrame> pollingFrames =
					    msg.getData().getParcelableArrayList(KEY_POLLING_LOOP_FRAMES_BUNDLE, PollingFrame.class);
					processPollingFrames(pollingFrames);
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}

	@Override
	public final IBinder onBind(Intent intent) {
		return getMessenger().getBinder();
	}

	public final void sendResponseApdu(byte[] responseApdu) {
		Message responseMsg = Message.obtain(null, MSG_RESPONSE_APDU);
		Bundle dataBundle = new Bundle();
		dataBundle.putByteArray(KEY_DATA, responseApdu);
		responseMsg.setData(dataBundle);
		try {
			getMessenger().send(responseMsg);
		} catch (RemoteException e) {
			Log.e("TAG", "Local messenger has died.");
		}
	}

	public final void notifyUnhandled() {
		Message unhandledMsg = Message.obtain(null, MSG_UNHANDLED);
		try {
			getMessenger().send(unhandledMsg);
		} catch (RemoteException e) {
			Log.e("TAG", "Local messenger has died.");
		}
	}

	public void processPollingFrames(List<PollingFrame> frames) {}

	public abstract byte[] processCommandApdu(byte[] commandApdu, Bundle extras);

	public abstract void onDeactivated(int reason);
}
