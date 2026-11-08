package android.content;

public abstract class BroadcastReceiver {

	public static class PendingResult {

		private int mResultCode = -1;

		public final void setResultCode(int code) {
			mResultCode = code;
		}

		public final int getResultCode() {
			return mResultCode;
		}

		public void finish() {}
	}

	public abstract void onReceive(Context context, Intent intent);

	public boolean isOrderedBroadcast() {
		return true;
	}

	/* AOSP BroadcastReceiver: the result of an ordered broadcast (code, data, extras) and its abort flag, as set by
	 * onReceive(). ATL delivers a broadcast to each receiver and has no final receiver to hand the result to, so the
	 * result is kept and returned for the receiver that set it. */
	private int mResultCode = -1; // Activity.RESULT_OK
	private String mResultData;
	private android.os.Bundle mResultExtras;
	private boolean mAbortBroadcast;

	public final void setResultCode(int code) {
		mResultCode = code;
	}

	public final int getResultCode() {
		return mResultCode;
	}

	public final void setResultData(String data) {
		mResultData = data;
	}

	public final String getResultData() {
		return mResultData;
	}

	public final void setResultExtras(android.os.Bundle extras) {
		mResultExtras = extras;
	}

	public final android.os.Bundle getResultExtras(boolean makeMap) {
		android.os.Bundle e = mResultExtras;
		if (!makeMap)
			return e;
		if (e == null)
			mResultExtras = e = new android.os.Bundle();
		return e;
	}

	public final void setResult(int code, String data, android.os.Bundle extras) {
		mResultCode = code;
		mResultData = data;
		mResultExtras = extras;
	}

	public final void abortBroadcast() {
		mAbortBroadcast = true;
	}

	public final void clearAbortBroadcast() {
		mAbortBroadcast = false;
	}

	public final boolean getAbortBroadcast() {
		return mAbortBroadcast;
	}

	/* AOSP BroadcastReceiver.isInitialStickyBroadcast(): the intent is the sticky one handed over at registration;
	 * ATL keeps no sticky broadcasts, so none is */
	public final boolean isInitialStickyBroadcast() {
		return false;
	}

	public PendingResult goAsync() {
		return new PendingResult();
	}
}
