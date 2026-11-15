package android.service.autofill;

import android.util.Log;

/* AOSP FillCallback without the IFillCallback binder: nothing in ATL receives the result, so only the
 * state checks of AOSP FillCallback.onSuccess / onFailure / assertNotCalled remain */
public final class FillCallback {

	private static final String TAG = "FillCallback";

	private final int mRequestId;
	private boolean mCalled;

	FillCallback(int requestId) {
		mRequestId = requestId;
	}

	/* AOSP FillCallback.onSuccess */
	public void onSuccess(FillResponse response) {
		assertNotCalled();
		mCalled = true;
		if (response != null) {
			response.setRequestId(mRequestId);
		}
	}

	/* AOSP FillCallback.onFailure */
	public void onFailure(CharSequence message) {
		Log.w(TAG, "onFailure(): " + message);
		assertNotCalled();
		mCalled = true;
	}

	/* AOSP FillCallback.assertNotCalled */
	private void assertNotCalled() {
		if (mCalled) {
			throw new IllegalStateException("Already called");
		}
	}
}
