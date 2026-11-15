package android.app;

import android.atl.ATLLoadedApp;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.GestureDetector;

public class PendingIntent implements Parcelable {
	public static final int FLAG_ONE_SHOT = 1 << 30;
	public static final int FLAG_NO_CREATE = 1 << 29;
	public static final int FLAG_CANCEL_CURRENT = 1 << 28;
	public static final int FLAG_UPDATE_CURRENT = 1 << 27;
	public static final int FLAG_IMMUTABLE = 1 << 26;
	public static final int FLAG_MUTABLE = 1 << 25;
	public static final int FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT = 1 << 24;

	private int requestCode;
	Intent intent;
	/* API 31 */
	public boolean isActivity() {
		return type == 0;
	}

	public boolean isBroadcast() {
		return type == 2;
	}

	/* a foreground service start is a service start here; ATL keeps no difference */
	public boolean isForegroundService() {
		return false;
	}

	public boolean isService() {
		return type == 1;
	}

	int type; // 0: activity, 1: service, 2: broadcast, 3: result for an activity (Activity.createPendingResult())
	private Activity resultActivity;

	private PendingIntent(int requestCode, Intent intent, int type) {
		this.requestCode = requestCode;
		this.intent = intent;
		this.type = type;
	}
	public static PendingIntent getBroadcast(Context context, int requestCode, Intent intent, int flags) {
		return new PendingIntent(requestCode, intent, 2);
	}

	/** @hide AOSP asks the system server for it, ATL holds it */
	public Intent getIntent() {
		return intent;
	}

	public IntentSender getIntentSender() {
		return new IntentSender(this);
	}

	/* AOSP PendingIntent.send(Context, int, Intent): the system fills the extras of the given intent into the
	 * stored one (Intent.fillIn() always merges extras) */
	public void send(Context context, int code, Intent intent) throws CanceledException {
		send(context, code, intent, null, null, null, null);
	}

	/* AOSP OnFinished: called by the system once the send is done (FinishedDispatcher, on the handler if given) */
	public interface OnFinished {
		void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode, String resultData, Bundle resultExtras);
	}

	public void send(int code, OnFinished onFinished, android.os.Handler handler) throws CanceledException {
		send(null, code, null, onFinished, handler, null, null);
	}

	public void send(Context context, int code, Intent intent, OnFinished onFinished, android.os.Handler handler) throws CanceledException {
		send(context, code, intent, onFinished, handler, null, null);
	}

	public void send(Context context, int code, Intent intent, OnFinished onFinished, android.os.Handler handler, String requiredPermission) throws CanceledException {
		send(context, code, intent, onFinished, handler, requiredPermission, null);
	}

	/* AOSP: the system's PendingIntentRecord.sendInner() fills the intent in and starts it; afterwards it calls the
	 * finished receiver: for an activity or service with result code 0, for a broadcast (sent ordered with the
	 * code as initial result) with the final result, here the initial code as ATL's broadcasts are not ordered.
	 * ATL grants every permission, so requiredPermission does not filter. */
	public void send(Context context, int code, Intent intent, final OnFinished onFinished, android.os.Handler handler, String requiredPermission, Bundle options) throws CanceledException {
		Intent filled = this.intent;
		if (intent != null) {
			filled = new Intent(this.intent);
			filled.putExtras(intent);
		}
		dispatch(filled, code);
		if (onFinished != null) {
			final Intent finalIntent = new Intent(filled);
			final int resultCode = type == 2 ? code : 0;
			Runnable r = new Runnable() {
				@Override
				public void run() {
					onFinished.onSendFinished(PendingIntent.this, finalIntent, resultCode, null, null);
				}
			};
			if (handler == null)
				r.run();
			else
				handler.post(r);
		}
	}

	public void send() {
		try {
			dispatch(intent, 0);
		} catch (CanceledException e) {
			/* send() of the PendingIntent of an activity that is gone: AOSP throws the CanceledException
			 * only from the variants that declare it */
		}
	}

	public void send(Bundle options) throws CanceledException {
		send();
	}

	/** @hide AOSP: Activity.createPendingResult(); the PendingIntent delivers a result to the activity */
	static PendingIntent getActivityResult(Activity activity, int requestCode, Intent data) {
		PendingIntent pi = new PendingIntent(requestCode, data, 3);
		pi.resultActivity = activity;
		return pi;
	}

	private void dispatch(Intent intent, int resultCode) throws CanceledException {
		if (type == 3) {
			resultActivity.deliverPendingResult(requestCode, resultCode, intent);
			return;
		}
		Context context = ATLLoadedApp.getPrimaryApplication().getApplication();
		if (type == 0) { // type Activity
			context.startActivity(intent);
		} else if (type == 1) { // type Service
			context.startService(intent);
		} else if (type == 2) { // type Broadcast
			context.sendBroadcast(intent);
		}
	}

	public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags) {
		return new PendingIntent(requestCode, intent, 0);
	}

	/* AOSP: the options are kept by the system for the activity start; ATL starts without them */
	public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags, Bundle options) {
		return getActivity(context, requestCode, intent, flags);
	}

	public static PendingIntent getActivities(Context context, int requestCode, Intent[] intents, int flags) {
		return getActivities(context, requestCode, intents, flags, null);
	}

	public static PendingIntent getService(Context context, int requestCode, Intent intent, int flags) {
		return new PendingIntent(requestCode, intent, 1);
	}

	public static PendingIntent getActivities(Context context, int requestCode, Intent[] intents, int flags, Bundle options) {
		return new PendingIntent(requestCode, intents[0], 0);
	}

	public String toString() {
		return "PendingIntent [requestCode=" + requestCode + ", intent=" + intent + ", type="
		     + new String[] {"activity", "service", "broadcast"}[type] + "]";
	}

	public void cancel() {}

	public static class CanceledException extends android.util.AndroidException {
		public CanceledException() {
		}

		public CanceledException(String name) {
			super(name);
		}

		public CanceledException(Exception cause) {
			super(cause);
		}
	}

	/* AOSP: deprecated name of getCreatorPackage() */
	@Deprecated
	public String getTargetPackage() {
		return getCreatorPackage();
	}

	public String getCreatorPackage() {
		return ATLLoadedApp.getPrimaryApplication().pkg.packageName;
	}

	public int getCreatorUid() {
		return ATLLoadedApp.getPrimaryApplication().pkg.applicationInfo.uid;
	}

	/* AOSP writes the binder of the system's PendingIntentRecord, so that every PendingIntent read back is the
	 * same one. ATL has no system server; the binder written here is a token of this very object, and a
	 * parcel is never read in another process (see Parcel.writeStrongBinder()). */
	private static class Token extends Binder {
		final PendingIntent target;

		Token(PendingIntent target) {
			this.target = target;
		}
	}

	private Token token;

	@Override
	public void writeToParcel(Parcel out, int flags) {
		synchronized (this) {
			if (token == null)
				token = new Token(this);
		}
		out.writeStrongBinder(token);
	}

	public static final Parcelable.Creator<PendingIntent> CREATOR = new Parcelable.Creator<PendingIntent>() {
		public PendingIntent createFromParcel(Parcel in) {
			IBinder b = in.readStrongBinder();
			return b instanceof Token ? ((Token)b).target : null;
		}

		public PendingIntent[] newArray(int size) {
			return new PendingIntent[size];
		}
	};

	public static PendingIntent getForegroundService(Context context, int requestCode, Intent intent, int flags) {
		return getService(context, requestCode, intent, flags);
	}
}
