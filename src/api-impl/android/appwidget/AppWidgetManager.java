package android.appwidget;

import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

public class AppWidgetManager {

	public static AppWidgetManager getInstance(Context context) {
		return new AppWidgetManager();
	}

	public int[] getAppWidgetIds(ComponentName provider) {
		return new int[0];
	}

	/* AOSP: List<AppWidgetProviderInfo> from AppWidgetServiceImpl (ATL has no such class); the desktop has no widget host */
	public java.util.List<?> getInstalledProviders() {
		return new java.util.ArrayList<>();
	}

	public void notifyAppWidgetViewDataChanged(int appWidgetId, int viewId) {}

	/* AOSP asks the default launcher (AppWidgetServiceImpl); the desktop has none that pins widgets */
	public boolean isRequestPinAppWidgetSupported() {
		return false;
	}

	public boolean requestPinAppWidget(ComponentName provider, android.os.Bundle extras, android.app.PendingIntent successCallback) {
		return false;
	}

	public void notifyAppWidgetViewDataChanged(int[] appWidgetIds, int viewId) {}

	/* AOSP returns early when mService == null; the desktop has no widget host */
	public void updateAppWidget(int appWidgetId, RemoteViews views) {}

	/* no widget host: there are no widgets of this provider to update */
	public void updateAppWidget(ComponentName provider, RemoteViews views) {}

	public void partiallyUpdateAppWidget(int appWidgetId, RemoteViews views) {}
}
