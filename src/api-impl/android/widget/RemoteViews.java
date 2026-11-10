package android.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;

public class RemoteViews {

	private final String mPackage;
	private final int mLayoutId;

	public RemoteViews(String packageName, int layoutId) {
		mPackage = packageName;
		mLayoutId = layoutId;
	}

	public int getLayoutId() {
		return mLayoutId;
	}

	public View apply(Context context, ViewGroup parent) {
		return null;
	}

	public void setProgressBar(int viewId, int max, int progress, boolean indeterminate) {}

	public void setTextViewText(int viewId, CharSequence text) {}

	public void setTextColor(int viewId, int color) {}

	public void setInt(int viewId, String methodName, int value) {}

	public void setPendingIntentTemplate(int viewId, PendingIntent pendingIntentTemplate) {}

	public void setOnClickFillInIntent(int viewId, Intent fillInIntent) {}

	public void setRemoteAdapter(int viewId, Intent intent) {}

	public void setImageViewResource(int viewId, int resId) {}

	public void setContentDescription(int viewId, CharSequence text) {}

	public void setOnClickPendingIntent(int viewId, PendingIntent pendingIntent) {}

	public void setViewVisibility(int viewId, int visibility) {}

	public void setImageViewBitmap(int viewId, Bitmap bitmap) {}

	public void removeAllViews(int viewId) {}

	public RemoteViews clone() {
		return new RemoteViews(mPackage, mLayoutId);
	}

	public void addView(int viewId, RemoteViews child) {}

	public void setViewPadding(int viewId, int left, int top, int right, int bottom) {}
}
