package android.appwidget;

import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

public class AppWidgetManager {

	public static AppWidgetManager getInstance(Context context) {
		return new AppWidgetManager();
	}

	public static final String ACTION_APPWIDGET_UPDATE = "android.appwidget.action.APPWIDGET_UPDATE";
	public static final String ACTION_APPWIDGET_ENABLE_AND_UPDATE = "android.appwidget.action.APPWIDGET_ENABLE_AND_UPDATE";
	public static final String ACTION_APPWIDGET_OPTIONS_CHANGED = "android.appwidget.action.APPWIDGET_UPDATE_OPTIONS";
	public static final String ACTION_APPWIDGET_DELETED = "android.appwidget.action.APPWIDGET_DELETED";
	public static final String ACTION_APPWIDGET_DISABLED = "android.appwidget.action.APPWIDGET_DISABLED";
	public static final String ACTION_APPWIDGET_ENABLED = "android.appwidget.action.APPWIDGET_ENABLED";
	public static final String ACTION_APPWIDGET_RESTORED = "android.appwidget.action.APPWIDGET_RESTORED";
	public static final String EXTRA_APPWIDGET_ID = "appWidgetId";
	public static final String EXTRA_APPWIDGET_IDS = "appWidgetIds";
	public static final String EXTRA_APPWIDGET_OLD_IDS = "appWidgetOldIds";
	public static final String EXTRA_APPWIDGET_OPTIONS = "appWidgetOptions";
	public static final int INVALID_APPWIDGET_ID = 0;

	/* AOSP answers these without a widget service (mService == null) as follows; the desktop has no widget host */
	public AppWidgetProviderInfo getAppWidgetInfo(int appWidgetId) {
		return null;
	}

	public android.os.Bundle getAppWidgetOptions(int appWidgetId) {
		return android.os.Bundle.EMPTY;
	}

	/* AOSP AppWidgetServiceImpl.updateAppWidgetOptions(): an id the host service does not know is ignored */
	public void updateAppWidgetOptions(int appWidgetId, android.os.Bundle options) {}

	/* AOSP: the providers of the package that the service knows; ATL has no widget host and none registered */
	public java.util.List<AppWidgetProviderInfo> getInstalledProvidersForPackage(String packageName, android.os.UserHandle profile) {
		return new java.util.ArrayList<>();
	}

	public void updateAppWidget(int[] appWidgetIds, RemoteViews views) {}

	public int[] getAppWidgetIds(ComponentName provider) {
		return new int[0];
	}

	/* AOSP: List<AppWidgetProviderInfo> from AppWidgetServiceImpl (ATL has no such class); the desktop has no widget host */
	public java.util.List<AppWidgetProviderInfo> getInstalledProviders() {
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
