package android.app;

public class KeyguardManager {
	public boolean inKeyguardRestrictedInputMode() {
		return false;
	}

	public boolean isKeyguardLocked() {
		return false;
	}

	public boolean isKeyguardSecure() {
		return true;
	}

	public KeyguardLock newKeyguardLock(String tag) {
		return new KeyguardLock();
	}

	public boolean isDeviceSecure() {
		return false;
	}

	/* AOSP isDeviceLocked(): the trust manager's answer for the user; ATL shows no lock screen, as isKeyguardLocked() */
	public boolean isDeviceLocked() {
		return false;
	}

	public static final String ACTION_CONFIRM_DEVICE_CREDENTIAL = "android.app.action.CONFIRM_DEVICE_CREDENTIAL";
	public static final String EXTRA_TITLE = "android.app.extra.TITLE";
	public static final String EXTRA_DESCRIPTION = "android.app.extra.DESCRIPTION";

	/* AOSP: null when the device is not secure (isDeviceSecure()), else the settings' confirm credential intent */
	@Deprecated
	public android.content.Intent createConfirmDeviceCredentialIntent(CharSequence title, CharSequence description) {
		if (!isDeviceSecure())
			return null;
		android.content.Intent intent = new android.content.Intent(ACTION_CONFIRM_DEVICE_CREDENTIAL);
		intent.putExtra(EXTRA_TITLE, title);
		intent.putExtra(EXTRA_DESCRIPTION, description);
		return intent;
	}

	public static abstract class KeyguardDismissCallback {
		public void onDismissError() {}

		public void onDismissSucceeded() {}

		public void onDismissCancelled() {}
	}

	public void requestDismissKeyguard(Activity activity, KeyguardDismissCallback callback) {
		requestDismissKeyguard(activity, null, callback);
	}

	/* AOSP requestDismissKeyguard() -> PhoneWindowManager.dismissKeyguardLw(): no keyguard is
	 * showing (isKeyguardLocked() is false), so the callback gets onDismissError(), posted on the
	 * activity's handler unless the activity is destroyed */
	public void requestDismissKeyguard(final Activity activity, CharSequence message, final KeyguardDismissCallback callback) {
		if (callback != null && !activity.isDestroyed()) {
			activity.mHandler.post(new Runnable() {
				@Override
				public void run() {
					callback.onDismissError();
				}
			});
		}
	}

	public class KeyguardLock {
		public void disableKeyguard() {}
		public void reenableKeyguard() {}
	}
}
