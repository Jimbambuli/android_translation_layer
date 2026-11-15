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

/* AOSP HostNfcFService (modules/Nfc) with HostNfcFServiceMsgHandler as inner class. ATL has no NFC service that
 * would send it messages; the service object itself is as in AOSP. */
public abstract class HostNfcFService extends Service {
	public static final String SERVICE_INTERFACE = "android.nfc.cardemulation.action.HOST_NFCF_SERVICE";
	public static final String SERVICE_META_DATA = "android.nfc.cardemulation.host_nfcf_service";
	public static final int DEACTIVATION_LINK_LOSS = 0;

	static final String TAG = "NfcFService";

	public static final int MSG_COMMAND_PACKET = 0;
	public static final int MSG_RESPONSE_PACKET = 1;
	public static final int MSG_DEACTIVATED = 2;
	public static final String KEY_DATA = "data";
	public static final String KEY_MESSENGER = "messenger";

	Messenger mNfcService = null;
	final Messenger mMessenger = new Messenger(new MsgHandler());

	public HostNfcFService() {}

	public Messenger getNfcService() {
		return mNfcService;
	}

	public Messenger getMessenger() {
		return mMessenger;
	}

	public void setNfcService(Messenger nfcService) {
		mNfcService = nfcService;
	}

	final class MsgHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_COMMAND_PACKET:
					Bundle dataBundle = msg.getData();
					if (dataBundle == null) {
						return;
					}
					if (getNfcService() == null) {
						setNfcService(msg.replyTo);
					}
					byte[] packet = dataBundle.getByteArray(KEY_DATA);
					if (packet != null) {
						byte[] responsePacket = processNfcFPacket(packet, null);
						if (responsePacket != null) {
							if (getNfcService() == null) {
								Log.e(TAG, "Response not sent; service was deactivated.");
								return;
							}
							Message responseMsg = Message.obtain(null, MSG_RESPONSE_PACKET);
							Bundle responseBundle = new Bundle();
							responseBundle.putByteArray(KEY_DATA, responsePacket);
							responseMsg.setData(responseBundle);
							responseMsg.replyTo = getMessenger();
							try {
								getNfcService().send(responseMsg);
							} catch (RemoteException e) {
								Log.e("TAG", "Response not sent; RemoteException calling into NfcService.");
							}
						}
					} else {
						Log.e(TAG, "Received MSG_COMMAND_PACKET without data.");
					}
					break;
				case MSG_RESPONSE_PACKET:
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
				default:
					super.handleMessage(msg);
			}
		}
	}

	@Override
	public final IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	public final void sendResponsePacket(byte[] responsePacket) {
		Message responseMsg = Message.obtain(null, MSG_RESPONSE_PACKET);
		Bundle dataBundle = new Bundle();
		dataBundle.putByteArray(KEY_DATA, responsePacket);
		responseMsg.setData(dataBundle);
		try {
			mMessenger.send(responseMsg);
		} catch (RemoteException e) {
			Log.e("TAG", "Local messenger has died.");
		}
	}

	public abstract byte[] processNfcFPacket(byte[] commandPacket, Bundle extras);

	public abstract void onDeactivated(int reason);
}
