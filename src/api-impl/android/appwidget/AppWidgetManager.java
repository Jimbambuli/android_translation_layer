package android.appwidget;

import android.content.ComponentName;
import android.content.Context;

public class AppWidgetManager {

	public static AppWidgetManager getInstance(Context context) {
		return new AppWidgetManager();
	}

	public int[] getAppWidgetIds(ComponentName provider) {
		return new int[0];
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
}
