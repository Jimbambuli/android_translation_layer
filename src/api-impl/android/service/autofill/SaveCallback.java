package android.service.autofill;

import android.content.IntentSender;
import android.util.Log;
import java.util.Objects;

/* AOSP SaveCallback without the ISaveCallback binder: nothing in ATL receives the result, so only the
 * state checks of AOSP SaveCallback remain */
public final class SaveCallback {

	private static final String TAG = "SaveCallback";

	private boolean mCalled;

	SaveCallback() {
	}

	public void onSuccess() {
		onSuccessInternal(null);
	}

	public void onSuccess(IntentSender intentSender) {
		onSuccessInternal(Objects.requireNonNull(intentSender));
	}

	/* AOSP SaveCallback.onSuccessInternal */
	private void onSuccessInternal(IntentSender intentSender) {
		assertNotCalled();
		mCalled = true;
	}

	/* AOSP SaveCallback.onFailure */
	public void onFailure(CharSequence message) {
		Log.w(TAG, "onFailure(): " + message);
		assertNotCalled();
		mCalled = true;
	}

	/* AOSP SaveCallback.assertNotCalled */
	private void assertNotCalled() {
		if (mCalled) {
			throw new IllegalStateException("Already called");
		}
	}
}
