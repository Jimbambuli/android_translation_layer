package android.widget;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.view.View;

public class Toast {
	private final Context mContext;
	private String text;
	private int mDuration;
	private View mNextView;
	private int mGravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.BOTTOM;
	private int mX, mY;
	private float mHorizontalMargin, mVerticalMargin;

	public static final int LENGTH_SHORT = 0;
	public static final int LENGTH_LONG = 1;

	public Toast(Context context) {
		mContext = context;
	}

	public static Toast makeText(Context context, int resId, int duration) {
		return makeText(context, context.getString(resId), duration);
	}

	public static Toast makeText(Context context, CharSequence text, int duration) {
		Toast toast = new Toast(context);
		toast.text = String.valueOf(text);
		toast.mDuration = duration;
		return toast;
	}

	public void show() {
		System.out.println("showing toast: " + text);
		Notification notification = new Notification.Builder(mContext).setContentText(text).build();
		NotificationManager manager = (NotificationManager)mContext.getSystemService("notification");
		int id = hashCode();
		manager.notify(id, notification);
		new Handler().postDelayed(new Runnable() {
			public void run() {
				manager.cancel(id);
			}
		}, 2000);
	}

	/* AOSP Toast: the accessors keep what the app sets; show() posts the text as a notification */
	public void setView(View view) { mNextView = view; }
	public View getView() { return mNextView; }
	public void setDuration(int duration) { mDuration = duration; }
	public int getDuration() { return mDuration; }
	public void setText(int resId) { setText(mContext.getText(resId)); }
	public void setText(CharSequence s) { text = String.valueOf(s); }
	public void setMargin(float horizontalMargin, float verticalMargin) { mHorizontalMargin = horizontalMargin; mVerticalMargin = verticalMargin; }
	public float getHorizontalMargin() { return mHorizontalMargin; }
	public float getVerticalMargin() { return mVerticalMargin; }
	public void setGravity(int gravity, int xOffset, int yOffset) { mGravity = gravity; mX = xOffset; mY = yOffset; }
	public int getGravity() { return mGravity; }
	public int getXOffset() { return mX; }
	public int getYOffset() { return mY; }

	public void cancel() {}
}
