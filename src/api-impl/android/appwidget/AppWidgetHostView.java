package android.appwidget;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

/* AOSP AppWidgetHostView (android-16.0.0_r4, AppWidgetHostView.java): hosts the view of one widget. Without the
 * asynchronous inflation, the view recycling, the size and light-background handling and the interaction handler.
 * AOSP shows an "error" layout (appwidget_error) when RemoteViews cannot be inflated, ATL does not have that layout,
 * the host then shows nothing. */
public class AppWidgetHostView extends FrameLayout {
	static final String TAG = "AppWidgetHostView";

	int mAppWidgetId;
	AppWidgetProviderInfo mInfo;
	View mView;

	public AppWidgetHostView(Context context) {
		super(context);
	}

	public AppWidgetHostView(Context context, int animationIn, int animationOut) {
		super(context);
	}

	public void setAppWidget(int appWidgetId, AppWidgetProviderInfo info) {
		mAppWidgetId = appWidgetId;
		mInfo = info;
	}

	public int getAppWidgetId() {
		return mAppWidgetId;
	}

	public AppWidgetProviderInfo getAppWidgetInfo() {
		return mInfo;
	}

	/* AOSP updateAppWidget(): null means the default view, which the host shows by inflating nothing new */
	public void updateAppWidget(RemoteViews remoteViews) {
		if (remoteViews == null) {
			return;
		}
		View content = null;
		try {
			content = remoteViews.apply(getContext(), this);
		} catch (RuntimeException e) {
			Log.w(TAG, "Error inflating RemoteViews", e);
		}
		if (content != null) {
			prepareView(content);
			addView(content);
		}
		if (mView != content) {
			if (mView != null) {
				removeView(mView);
			}
			mView = content;
		}
	}

	/* AOSP prepareView(): the requested size of the child, with the default gravity */
	protected void prepareView(View view) {
		FrameLayout.LayoutParams requested = (FrameLayout.LayoutParams) view.getLayoutParams();
		if (requested == null) {
			requested = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}
		requested.gravity = android.view.Gravity.CENTER;
		view.setLayoutParams(requested);
	}
}
