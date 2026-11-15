package android.app;

import android.content.Context;
import android.graphics.Rect;
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

	/* AOSP keys and animation types (ActivityOptions, ComponentOptions) for toBundle(); ATL does not animate
	 * activity starts, the values are kept for toBundle(), update() and the getters only */
	public static final String KEY_LAUNCH_BOUNDS = "android:activity.launchBounds";
	public static final String KEY_ANIM_TYPE = "android:activity.animType";
	public static final String KEY_ANIM_START_X = "android:activity.animStartX";
	public static final String KEY_ANIM_START_Y = "android:activity.animStartY";
	public static final String KEY_ANIM_WIDTH = "android:activity.animWidth";
	public static final String KEY_ANIM_HEIGHT = "android:activity.animHeight";
	private static final String KEY_LAUNCH_DISPLAY_ID = "android.activity.launchDisplayId";
	private static final String KEY_USAGE_TIME_REPORT = "android:activity.usageTimeReport";
	private static final String KEY_PENDING_INTENT_BACKGROUND_ACTIVITY_ALLOWED = "android.pendingIntent.backgroundActivityAllowed";
	public static final int ANIM_UNDEFINED = -1;
	public static final int ANIM_SCALE_UP = 2;
	public static final int ANIM_THUMBNAIL_SCALE_UP = 3;
	public static final int ANIM_LAUNCH_TASK_BEHIND = 7;
	public static final int ANIM_CLIP_REVEAL = 11;

	private int mAnimationType = ANIM_UNDEFINED;
	private int mStartX;
	private int mStartY;
	private int mWidth;
	private int mHeight;
	private PendingIntent mUsageTimeReport;
	private int mLaunchDisplayId = -1;  // Display.INVALID_DISPLAY
	private Boolean mPendingIntentBalAllowed;

	/* AOSP makeScaleUpAnimation/makeClipRevealAnimation: the start rectangle relative to the source view on screen */
	private static ActivityOptions makeAnimation(int type, View source, int startX, int startY, int width, int height) {
		ActivityOptions opts = new ActivityOptions();
		opts.mAnimationType = type;
		int[] pts = new int[2];
		source.getLocationOnScreen(pts);
		opts.mStartX = pts[0] + startX;
		opts.mStartY = pts[1] + startY;
		opts.mWidth = width;
		opts.mHeight = height;
		return opts;
	}

	public static ActivityOptions makeScaleUpAnimation(View source, int startX, int startY, int startWidth, int startHeight) {
		return makeAnimation(ANIM_SCALE_UP, source, startX, startY, startWidth, startHeight);
	}

	public static ActivityOptions makeClipRevealAnimation(View source, int startX, int startY, int width, int height) {
		return makeAnimation(ANIM_CLIP_REVEAL, source, startX, startY, width, height);
	}

	/* AOSP keeps the thumbnail for the window manager; ATL draws no start animation */
	public static ActivityOptions makeThumbnailScaleUpAnimation(View source, android.graphics.Bitmap thumbnail, int startX, int startY) {
		return makeAnimation(ANIM_THUMBNAIL_SCALE_UP, source, startX, startY, 0, 0);
	}

	public static ActivityOptions makeTaskLaunchBehind() {
		ActivityOptions opts = new ActivityOptions();
		opts.mAnimationType = ANIM_LAUNCH_TASK_BEHIND;
		return opts;
	}

	/* AOSP: the system sends the PendingIntent with the usage time when the activity leaves; ATL keeps no usage stats */
	public void requestUsageTimeReport(PendingIntent receiver) {
		mUsageTimeReport = receiver;
	}

	public ActivityOptions setLaunchDisplayId(int launchDisplayId) {
		mLaunchDisplayId = launchDisplayId;
		return this;
	}

	public int getLaunchDisplayId() {
		return mLaunchDisplayId;
	}

	/* AOSP ComponentOptions */
	@Deprecated
	public void setPendingIntentBackgroundActivityLaunchAllowed(boolean allowed) {
		mPendingIntentBalAllowed = allowed;
	}

	@Deprecated
	public boolean isPendingIntentBackgroundActivityLaunchAllowed() {
		return mPendingIntentBalAllowed == null ? true : mPendingIntentBalAllowed;
	}

	/* AOSP update(): the other options' usage time report and animation replace these */
	public void update(ActivityOptions otherOptions) {
		mUsageTimeReport = otherOptions.mUsageTimeReport;
		mAnimationType = otherOptions.mAnimationType;
		switch (otherOptions.mAnimationType) {
			case ANIM_SCALE_UP:
			case ANIM_CLIP_REVEAL:
			case ANIM_THUMBNAIL_SCALE_UP:
				mStartX = otherOptions.mStartX;
				mStartY = otherOptions.mStartY;
				mWidth = otherOptions.mWidth;
				mHeight = otherOptions.mHeight;
				break;
		}
	}

	/* AOSP toBundle(): the set values under their keys (the subset ATL keeps) */
	public Bundle toBundle() {
		Bundle b = new Bundle();
		if (mPendingIntentBalAllowed != null)
			b.putBoolean(KEY_PENDING_INTENT_BACKGROUND_ACTIVITY_ALLOWED, mPendingIntentBalAllowed);
		if (mLaunchBounds != null)
			b.putParcelable(KEY_LAUNCH_BOUNDS, mLaunchBounds);
		if (mAnimationType != ANIM_UNDEFINED)
			b.putInt(KEY_ANIM_TYPE, mAnimationType);
		if (mUsageTimeReport != null)
			b.putParcelable(KEY_USAGE_TIME_REPORT, mUsageTimeReport);
		switch (mAnimationType) {
			case ANIM_SCALE_UP:
			case ANIM_CLIP_REVEAL:
				b.putInt(KEY_ANIM_START_X, mStartX);
				b.putInt(KEY_ANIM_START_Y, mStartY);
				b.putInt(KEY_ANIM_WIDTH, mWidth);
				b.putInt(KEY_ANIM_HEIGHT, mHeight);
				break;
			case ANIM_THUMBNAIL_SCALE_UP:
				b.putInt(KEY_ANIM_START_X, mStartX);
				b.putInt(KEY_ANIM_START_Y, mStartY);
				break;
		}
		if (mLaunchDisplayId != -1)
			b.putInt(KEY_LAUNCH_DISPLAY_ID, mLaunchDisplayId);
		return b;
	}

	/* AOSP (android-14, setAllowPassThroughOnTouchOutside android-15): stored for the getters; ATL starts
	 * activities of the app only, so they change nothing */
	private boolean mShareIdentity;
	private int mPendingIntentCreatorBackgroundActivityStartMode; // MODE_BACKGROUND_ACTIVITY_START_SYSTEM_DEFINED
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

	/* AOSP: the mode the creator of the PendingIntent grants for starting background activities */
	public ActivityOptions setPendingIntentCreatorBackgroundActivityStartMode(int mode) {
		mPendingIntentCreatorBackgroundActivityStartMode = mode;
		return this;
	}

	public int getPendingIntentCreatorBackgroundActivityStartMode() {
		return mPendingIntentCreatorBackgroundActivityStartMode;
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

	/* AOSP: where the activity's window should go (VLC asks for the full screen); ATL's windows are placed by
	 * the desktop, so it is kept for the getter only */
	private Rect mLaunchBounds;

	/**
	 * Sets the bounds (window size and position) that the activity should be launched in.
	 * Rect position should be provided in pixels and in screen coordinates.
	 * Set to {@code null} to explicitly launch fullscreen.
	 */
	public ActivityOptions setLaunchBounds(Rect screenSpacePixelRect) {
		mLaunchBounds = screenSpacePixelRect != null ? new Rect(screenSpacePixelRect) : null;
		return this;
	}

	/**
	 * Returns the bounds that should be used to launch the activity.
	 */
	public Rect getLaunchBounds() {
		return mLaunchBounds;
	}
}
