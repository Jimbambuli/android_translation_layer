package android.app;

import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;

public class ActivityOptions {

	public static ActivityOptions makeSceneTransitionAnimation(Activity activity, Pair<View, String>... pairs) {
		return new ActivityOptions();
	}

	public static ActivityOptions makeSceneTransitionAnimation(Activity activity, View view, String name) {
		return new ActivityOptions();
	}

	/**
	 * Create an ActivityOptions specifying a custom animation to run when
	 * the activity is displayed.
	 */
	public static ActivityOptions makeCustomAnimation(Context context, int enterResId, int exitResId) {
		return makeCustomAnimation(context, enterResId, exitResId, 0);
	}

	/**
	 * Create an ActivityOptions specifying a custom animation to run when
	 * the activity is displayed.
	 */
	public static ActivityOptions makeCustomAnimation(Context context, int enterResId, int exitResId, int backgroundColor) {
		/* activity transitions are not animated */
		return new ActivityOptions();
	}

	public static ActivityOptions makeBasic() {
		return new ActivityOptions();
	}

	public static ActivityOptions makeScaleUpAnimation(View source, int startX, int startY, int startWidth, int startHeight) {
		return new ActivityOptions();
	}

	public Bundle toBundle() {
		return null;
	}

	/* AOSP (android-14, setAllowPassThroughOnTouchOutside android-15): stored for the getters; ATL starts
	 * activities of the app only, so they change nothing */
	private boolean mShareIdentity;
	private int mPendingIntentBackgroundActivityStartMode;  // MODE_BACKGROUND_ACTIVITY_START_SYSTEM_DEFINED
	private boolean mAllowPassThroughOnTouchOutside;

	/**
	 * Returns whether the launching app has opted-in to sharing its identity with the launched
	 * activity.
	 */
	public boolean isShareIdentityEnabled() {
		return mShareIdentity;
	}

	/**
	 * Sets whether the identity of the launching app should be shared with the activity.
	 */
	public ActivityOptions setShareIdentityEnabled(boolean shareIdentity) {
		mShareIdentity = shareIdentity;
		return this;
	}

	/**
	 * Sets the mode for allowing or denying the senders privileges to start background activities
	 * to the PendingIntent.
	 */
	public ActivityOptions setPendingIntentBackgroundActivityStartMode(int state) {
		mPendingIntentBackgroundActivityStartMode = state;
		return this;
	}

	/**
	 * Get the mode for allowing or denying the senders privileges to start background activities
	 * to the PendingIntent.
	 */
	public int getPendingIntentBackgroundActivityStartMode() {
		return mPendingIntentBackgroundActivityStartMode;
	}

	/**
	 * Sets whether the insecure launch of the activity (touches outside it) passes through.
	 */
	public void setAllowPassThroughOnTouchOutside(boolean allowPassThroughOnTouchOutside) {
		mAllowPassThroughOnTouchOutside = allowPassThroughOnTouchOutside;
	}
}
