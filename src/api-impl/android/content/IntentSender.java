package android.content;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AndroidException;
import java.util.concurrent.Executor;

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
		sendIntent(context, code, intent, requiredPermission, null, handler == null ? null : handler::post, onFinished);
	}

	public void sendIntent(Context context, int code, Intent intent, String requiredPermission, Bundle options, Executor executor, OnFinished onFinished) throws SendIntentException {
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

	/* AOSP writeToParcel(): the strong binder of the target; ATL's target is the PendingIntent, which writes its token */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		if (mPendingIntent != null)
			mPendingIntent.writeToParcel(out, flags);
		else
			out.writeStrongBinder(null);
	}

	/* AOSP CREATOR: an IntentSender around the target the binder stands for, null without one */
	public static final Parcelable.Creator<IntentSender> CREATOR = new Parcelable.Creator<IntentSender>() {
		public IntentSender createFromParcel(Parcel in) {
			PendingIntent target = PendingIntent.CREATOR.createFromParcel(in);
			return target != null ? new IntentSender(target) : null;
		}

		public IntentSender[] newArray(int size) {
			return new IntentSender[size];
		}
	};
}
