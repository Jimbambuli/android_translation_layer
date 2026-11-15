package android.os;

/* AOSP core/java/android/os/ResultReceiver.java: the receiver that crosses a Parcel is a binder
 * (IResultReceiver) that calls back into the original object. ATL's Parcel keeps binders as objects,
 * so the binder is a plain Binder subclass instead of an AIDL stub. */
public class ResultReceiver implements Parcelable {
	final boolean mLocal;
	final Handler mHandler;

	MyResultReceiver mReceiver;

	class MyRunnable implements Runnable {
		final int mResultCode;
		final Bundle mResultData;

		MyRunnable(int resultCode, Bundle resultData) {
			mResultCode = resultCode;
			mResultData = resultData;
		}

		public void run() {
			onReceiveResult(mResultCode, mResultData);
		}
	}

	class MyResultReceiver extends Binder {
		public void send(int resultCode, Bundle resultData) {
			if (mHandler != null) {
				mHandler.post(new MyRunnable(resultCode, resultData));
			} else {
				onReceiveResult(resultCode, resultData);
			}
		}
	}

	public ResultReceiver(Handler handler) {
		mLocal = true;
		mHandler = handler;
	}

	public void send(int resultCode, Bundle resultData) {
		if (mLocal) {
			if (mHandler != null) {
				mHandler.post(new MyRunnable(resultCode, resultData));
			} else {
				onReceiveResult(resultCode, resultData);
			}
			return;
		}

		if (mReceiver != null) {
			mReceiver.send(resultCode, resultData);
		}
	}

	protected void onReceiveResult(int resultCode, Bundle resultData) {
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		synchronized (this) {
			if (mReceiver == null) {
				mReceiver = new MyResultReceiver();
			}
			out.writeStrongBinder(mReceiver);
		}
	}

	ResultReceiver(Parcel in) {
		mLocal = false;
		mHandler = null;
		IBinder binder = in.readStrongBinder();
		mReceiver = binder instanceof MyResultReceiver ? (MyResultReceiver)binder : null;
	}

	public static final Parcelable.Creator<ResultReceiver> CREATOR = new Parcelable.Creator<ResultReceiver>() {
		public ResultReceiver createFromParcel(Parcel in) {
			return new ResultReceiver(in);
		}

		public ResultReceiver[] newArray(int size) {
			return new ResultReceiver[size];
		}
	};
}
