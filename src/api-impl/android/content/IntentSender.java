package android.content;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AndroidException;

/* AOSP core/java/android/content/IntentSender.java: a PendingIntent's sender. ATL keeps the PendingIntent itself
 * (AOSP: an IIntentSender in the system server). */
public class IntentSender implements Parcelable {
	private final PendingIntent mPendingIntent;

	public static class SendIntentException extends AndroidException {
		public SendIntentException() {
		}

		public SendIntentException(String name) {
			super(name);
		}

		public SendIntentException(Exception cause) {
			super(cause);
		}
	}

	public interface OnFinished {
		void onSendFinished(IntentSender IntentSender, Intent intent, int resultCode, String resultData, Bundle resultExtras);
	}

	/** @hide */
	public IntentSender(PendingIntent pendingIntent) {
		mPendingIntent = pendingIntent;
	}

	public void sendIntent(Context context, int code, Intent intent, OnFinished onFinished, Handler handler) throws SendIntentException {
		sendIntent(context, code, intent, onFinished, handler, null);
	}

	public void sendIntent(Context context, int code, Intent intent, OnFinished onFinished, Handler handler, String requiredPermission) throws SendIntentException {
		if (mPendingIntent == null)
			throw new SendIntentException("no PendingIntent");
		try {
			mPendingIntent.send(context, code, intent);
		} catch (PendingIntent.CanceledException e) {
			throw new SendIntentException(e);
		}
	}

	/** @hide AOSP: getTarget() gives the IIntentSender, ATL the PendingIntent it stands for */
	public PendingIntent getPendingIntent() {
		return mPendingIntent;
	}

	public String getCreatorPackage() {
		return mPendingIntent != null ? mPendingIntent.getCreatorPackage() : null;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {}
}
